package com.prakalpa.api.controllers;

import com.prakalpa.api.models.UserGitConfig;
import com.prakalpa.api.repository.UserGitConfigRepository;
import com.prakalpa.api.services.AuthUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/users/git")
public class GitConfigController {

    private static final String SECRET_KEY = "Your32ByteLongSecretKeyHere!!!!!"; // Ensure 32 bytes for AES-256 or 16 bytes for AES-128
    private static final String WEBHOOK_ENDPOINT_URL = "https://api.wdpcare.com/api/v1/users/git/webhook";

    @Autowired
    private UserGitConfigRepository configRepository;

    @Autowired
    private AuthUserService authUserService;

    // ==========================================
    // 1. SAVE OR UPDATE CONFIGURATION
    // ==========================================
    @PostMapping("/save-config")
    public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody Map<String, String> request) {
        try {
            Long userId = authUserService.getLoggedInUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized user session"));
            }

            String repoPath = sanitizeRepoPath(request.get("repoPath"));
            String gitToken = request.get("gitToken");
            String geminiKey = request.get("geminiApiKey");

            if (repoPath.isBlank() || gitToken == null || geminiKey == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid or missing parameter values"));
            }

            UserGitConfig config = configRepository.findByUserId(userId)
                    .orElseGet(UserGitConfig::new);

            config.setUserId(userId);
            config.setRepoPath(repoPath);
            config.setEncryptedGitToken(encrypt(gitToken));
            config.setEncryptedGeminiApiKey(encrypt(geminiKey));

            configRepository.save(config);

            // Automatically register webhook on user's GitHub repository
            boolean webhookCreated = registerGithubWebhook(repoPath, gitToken);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuration saved successfully. Webhook connection: " + (webhookCreated ? "Active" : "Manual review required")
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==========================================
    // 2. MANUAL OR ON-DEMAND PR REVIEW & MERGE
    // ==========================================
    @PostMapping("/review-and-merge")
    public ResponseEntity<Map<String, Object>> reviewAndMergePullRequest(@RequestBody Map<String, String> request) {
        try {
            if (!request.containsKey("userId") || !request.containsKey("pullNumber")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing required parameters: userId or prNumber"));
            }

            Long userId = authUserService.getLoggedInUserId();
            int prNumber = Integer.parseInt(request.get("pullNumber"));

            Map<String, Object> result = processPullRequestReview(userId, prNumber);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "PR processing failed: " + e.getMessage()));
        }
    }

    // ==========================================
    // 3. GITHUB WEBHOOK RECEIVER
    // ==========================================
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleGithubWebhook(@RequestBody Map<String, Object> payload) {
        try {
            // Validate pull request event action
            String action = (String) payload.get("action");
            if (action == null || (!action.equals("opened") && !action.equals("reopened") && !action.equals("synchronize"))) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Ignored non-targeted webhook event action: " + action));
            }

            Map<String, Object> repository = (Map<String, Object>) payload.get("repository");
            Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");

            if (repository == null || pullRequest == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Malformed webhook payload"));
            }

            String fullRepoName = (String) repository.get("full_name"); // e.g., "owner/repo"
            int prNumber = (Integer) pullRequest.get("number");

            // Locate user config associated with this repository path
            Optional<UserGitConfig> configOpt = configRepository.findByRepoPath(fullRepoName);
            if (configOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "No configured user found for repository: " + fullRepoName));
            }

            Long userId = configOpt.get().getUserId();

            // Run code review asynchronously to respond immediately to GitHub's webhook timer
            CompletableFuture.runAsync(() -> processPullRequestReview(userId, prNumber));

            return ResponseEntity.ok(Map.of("success", true, "message", "Webhook received. AI Review process initiated for PR #" + prNumber));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Webhook processing failed: " + e.getMessage()));
        }
    }

    // ==========================================
    // CORE PR REVIEW AND MERGE EXECUTION
    // ==========================================
    private Map<String, Object> processPullRequestReview(Long userId, int prNumber) {
        Optional<UserGitConfig> configOpt = configRepository.findByUserId(userId);
        if (configOpt.isEmpty()) {
            return Map.of("success", false, "error", "Configuration not found for user ID: " + userId);
        }

        UserGitConfig config = configOpt.get();
        String repoPath = config.getRepoPath();
        String gitToken = decrypt(config.getEncryptedGitToken());
        String geminiApiKey = decrypt(config.getEncryptedGeminiApiKey());

        String[] parts = repoPath.split("/");
        if (parts.length < 2) {
            return Map.of("success", false, "error", "Invalid repository path pattern in configuration");
        }
        String owner = parts[0];
        String repo = parts[1];

        RestClient restClient = RestClient.create();
        String authHeader = gitToken.startsWith("github_pat_") ? "Bearer " + gitToken : "token " + gitToken;

        // Fetch PR Files/Diffs
        List<Map<String, Object>> files = restClient.get()
                .uri("https://api.github.com/repos/{owner}/{repo}/pulls/{number}/files", owner, repo, prNumber)
                .header("Authorization", authHeader)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        StringBuilder diffContent = new StringBuilder();
        if (files != null && !files.isEmpty()) {
            for (Map<String, Object> file : files) {
                diffContent.append("File: ").append(file.get("filename")).append("\n");
                Object patch = file.get("patch");
                if (patch != null) {
                    diffContent.append("Patch:\n").append(patch).append("\n\n");
                }
            }
        } else {
            return Map.of("success", true, "message", "No visible diff patches found for this PR.");
        }

        // Generate AI Review via Gemini API
        String prompt = "You are an automated senior code reviewer. Perform a thorough code review on this Git pull request diff. " +
                "Structure your response clearly and conclude with either 'RECOMMENDATION: APPROVE' or 'RECOMMENDATION: REJECT'.\n\n" + diffContent;

        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=" + geminiApiKey;

        Map<String, Object> geminiResponse = restClient.post()
                .uri(geminiUrl)
                .body(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))))
                .retrieve()
                .body(Map.class);

        String aiReview = parseGeminiResponse(geminiResponse);

        // Comment AI Review back on GitHub PR
        postGithubPrComment(owner, repo, prNumber, authHeader, aiReview);

        // Conditionally Squash-Merge PR if Approved
        boolean merged = false;
        String mergeMessage;

        if (aiReview.contains("RECOMMENDATION: APPROVE") || !aiReview.contains("RECOMMENDATION: REJECT")) {
            merged = mergeGithubPr(owner, repo, prNumber, authHeader);
            mergeMessage = merged ? "PR reviewed and auto-merged successfully!" : "PR reviewed and commented, but automated merge failed.";
        } else {
            mergeMessage = "PR reviewed with requested changes. Automated merge aborted due to findings.";
        }

        return Map.of(
                "success", true,
                "merged", merged,
                "review", aiReview,
                "message", mergeMessage
        );
    }

    // ==========================================
    // HELPER METHODS: GITHUB INTEGRATION
    // ==========================================
    private void postGithubPrComment(String owner, String repo, int prNumber, String authHeader, String commentBody) {
        try {
            RestClient restClient = RestClient.create();
            restClient.post()
                    .uri("https://api.github.com/repos/{owner}/{repo}/issues/{number}/comments", owner, repo, prNumber)
                    .header("Authorization", authHeader)
                    .header("Accept", "application/vnd.github+json")
                    .body(Map.of("body", "### 🤖 AI Automated Code Review\n\n" + commentBody))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.err.println("Failed to post comment on GitHub: " + e.getMessage());
        }
    }

    private boolean mergeGithubPr(String owner, String repo, int prNumber, String authHeader) {
        try {
            RestClient restClient = RestClient.create();
            restClient.put()
                    .uri("https://api.github.com/repos/{owner}/{repo}/pulls/{number}/merge", owner, repo, prNumber)
                    .header("Authorization", authHeader)
                    .header("Accept", "application/vnd.github+json")
                    .body(Map.of(
                            "commit_title", "Auto-merged by AI Reviewer (PR #" + prNumber + ")",
                            "merge_method", "squash"
                    ))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            System.err.println("Failed to execute GitHub PR merge: " + e.getMessage());
            return false;
        }
    }

    private boolean registerGithubWebhook(String repoPath, String gitToken) {
        try {
            String[] parts = repoPath.split("/");
            String owner = parts[0];
            String repo = parts[1];

            RestClient restClient = RestClient.create();
            String authHeader = gitToken.startsWith("github_pat_") ? "Bearer " + gitToken : "token " + gitToken;

            Map<String, Object> webhookPayload = Map.of(
                    "name", "web",
                    "active", true,
                    "events", List.of("pull_request"),
                    "config", Map.of(
                            "url", WEBHOOK_ENDPOINT_URL,
                            "content_type", "json",
                            "insecure_ssl", "0"
                    )
            );

            restClient.post()
                    .uri("https://api.github.com/repos/{owner}/{repo}/hooks", owner, repo)
                    .header("Authorization", authHeader)
                    .header("Accept", "application/vnd.github+json")
                    .body(webhookPayload)
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String sanitizeRepoPath(String input) {
        if (input == null || input.isBlank()) return "";
        String path = input.trim();
        if (path.startsWith("https://github.com/")) path = path.replace("https://github.com/", "");
        if (path.startsWith("github.com/")) path = path.replace("github.com/", "");
        if (path.contains("/tree/")) path = path.substring(0, path.indexOf("/tree/"));
        if (path.contains("/blob/")) path = path.substring(0, path.indexOf("/blob/"));
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path;
    }

    @SuppressWarnings("unchecked")
    private String parseGeminiResponse(Map<String, Object> geminiResponse) {
        if (geminiResponse == null || !geminiResponse.containsKey("candidates")) {
            return "Unable to retrieve response from Gemini API.";
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) geminiResponse.get("candidates");
        if (candidates == null || candidates.isEmpty()) return "No content candidates generated.";

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) return "Empty content generated.";

        List<Map<String, Object>> partsList = (List<Map<String, Object>>) content.get("parts");
        if (partsList == null || partsList.isEmpty()) return "Empty parts response.";

        return (String) partsList.get(0).get("text");
    }

    // ==========================================
    // HELPER METHODS: AES ENCRYPTION & DECRYPTION
    // ==========================================
    private String encrypt(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting sensitive key", e);
        }
    }

    private String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) return "";
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedValue);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fallback for unencrypted legacy plain-text database values
            return encryptedValue;
        }
    }
}