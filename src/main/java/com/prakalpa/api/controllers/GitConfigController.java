package com.prakalpa.api.controllers;

import com.prakalpa.api.models.UserGitConfig;
import com.prakalpa.api.repository.UserGitConfigRepository;
import com.prakalpa.api.services.AuthUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/users/git")
public class GitConfigController {

    private static final String SECRET_KEY = "Your32ByteLongSecretKeyHere!!!!!"; // 32 bytes for AES-256
    private static final String WEBHOOK_ENDPOINT_URL = "https://api.wdpcare.com/api/v1/users/git/webhook";
    private static final Random RANDOM = new Random();

    @Autowired
    private UserGitConfigRepository configRepository;

    @Autowired
    private AuthUserService authUserService;

    // ==========================================
    // 1. SAVE OR UPDATE CONFIGURATION
    // ==========================================

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getUserConfig() {
        try {
            Long userId = authUserService.getLoggedInUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
            }

            Optional<UserGitConfig> configOpt = configRepository.findByUserId(userId);
            if (configOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Configuration not found"));
            }

            UserGitConfig entity = configOpt.get();

            Map<String, Object> configMap = Map.of(
                    "id", entity.getId(),
                    "userId", entity.getUserId(),
                    "repoPath", entity.getRepoPath() != null ? entity.getRepoPath() : "",
                    "gitToken", decrypt(entity.getEncryptedGitToken()),
                    "geminiApiKey", decrypt(entity.getEncryptedGeminiApiKey())
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "config", configMap
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to retrieve git config: " + e.getMessage()
            ));
        }
    }

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
    public ResponseEntity<Map<String, Object>> reviewAndMergePullRequest(@RequestBody Map<String, Object> request) {
        try {
            Long userId = authUserService.getLoggedInUserId();

            if (userId == null && request.containsKey("userId")) {
                userId = parseUserIdSafely(String.valueOf(request.get("userId")));
            }

            if (userId == null || !request.containsKey("pullNumber")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing required parameters: userId or pullNumber"));
            }

            int prNumber = Integer.parseInt(String.valueOf(request.get("pullNumber")));

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
            String action = (String) payload.get("action");
            if (action == null || (!action.equals("opened") && !action.equals("reopened") && !action.equals("synchronize"))) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Ignored event action: " + action));
            }

            Map<String, Object> repository = (Map<String, Object>) payload.get("repository");
            Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");

            if (repository == null || pullRequest == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Malformed webhook payload"));
            }

            String fullRepoName = (String) repository.get("full_name");
            int prNumber = (Integer) pullRequest.get("number");

            Optional<UserGitConfig> configOpt = configRepository.findByRepoPath(fullRepoName);
            if (configOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "No configured user found for repository: " + fullRepoName));
            }

            Long userId = configOpt.get().getUserId();

            CompletableFuture.runAsync(() -> processPullRequestReview(userId, prNumber));

            return ResponseEntity.ok(Map.of("success", true, "message", "Webhook received. AI Review initiated for PR #" + prNumber));

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
        List<Map<String, Object>> files;
        try {
            files = restClient.get()
                    .uri("https://api.github.com/repos/{owner}/{repo}/pulls/{number}/files", owner, repo, prNumber)
                    .header("Authorization", authHeader)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return Map.of("success", false, "error", "Failed to fetch files from GitHub: " + e.getMessage());
        }

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

        // Safety Cap: Truncate oversized diffs (Max 30,000 characters) to prevent HTTP 429/503 limits
        String truncatedDiff = diffContent.toString();
        if (truncatedDiff.length() > 30000) {
            truncatedDiff = truncatedDiff.substring(0, 30000) + "\n\n...[Diff truncated due to size limits]...";
        }

        String prompt = "You are an automated senior code reviewer. Perform a thorough code review on this Git pull request diff. " +
                "Structure your response clearly with summary, findings, and conclude explicitly with either 'RECOMMENDATION: APPROVE' or 'RECOMMENDATION: REJECT'.\n\n" + truncatedDiff;

        String aiReview = callGeminiWithRetryAndFallback(prompt, geminiApiKey);

        // Soft Failure Handling: Inform user on GitHub PR instead of breaking pipeline
        if (aiReview.startsWith("ERROR:")) {
            postGithubPrComment(owner, repo, prNumber, authHeader,
                    "⚠️ **AI Review Delayed**: Gemini models are currently experiencing heavy traffic. Please re-trigger review manually shortly.");

            return Map.of(
                    "success", false,
                    "error", "Gemini API unavailable after multiple retries across all active models."
            );
        }

        // Post review comment back to GitHub PR
        postGithubPrComment(owner, repo, prNumber, authHeader, aiReview);

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
// RESILIENT GEMINI CALL (ACTIVE 2026 MODELS)
// ==========================================
    private String callGeminiWithRetryAndFallback(String prompt, String apiKey) {
        // Current Active Models List
        String[] models = {"gemini-3.5-flash", "gemini-3.5-flash-lite", "gemini-2.5-flash"};
        RestClient restClient = RestClient.create();
        Random random = new Random();

        for (String model : models) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    Map<String, Object> requestBody = Map.of(
                            "contents", List.of(
                                    Map.of("parts", List.of(Map.of("text", prompt)))
                            )
                    );

                    Map<String, Object> geminiResponse = restClient.post()
                            .uri(url)
                            .body(requestBody)
                            .retrieve()
                            .body(Map.class);

                    String parsedResponse = parseGeminiResponse(geminiResponse);
                    if (parsedResponse != null && !parsedResponse.isBlank()) {
                        return parsedResponse;
                    }

                } catch (HttpStatusCodeException ex) {
                    int statusCode = ex.getStatusCode().value();
                    System.err.println("[" + model + "] Attempt " + attempt + " returned HTTP " + statusCode);

                    if (statusCode == 503 || statusCode == 429 || statusCode == 500) {
                        if (attempt < 3) {
                            // Exponential backoff with jitter: ~2s, ~4s, ~8s
                            long sleepTime = (long) (Math.pow(2, attempt) * 1000 + random.nextInt(1000));
                            try {
                                Thread.sleep(sleepTime);
                            } catch (InterruptedException ignored) {}
                            continue; // Retry current model
                        }
                    }
                    break; // Switch to next model in list on invalid status or exhausted retries

                } catch (Exception e) {
                    System.err.println("[" + model + "] Unexpected exception: " + e.getMessage());
                    break;
                }
            }
        }

        return "ERROR: All Gemini models are currently experiencing high demand. Please try again in 1 minute.";
    }

    // ==========================================
    // RESILIENT GEMINI ENGINE (BACKOFF + FALLBACK)
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

    private Long parseUserIdSafely(String rawUserId) {
        if (rawUserId == null || rawUserId.isBlank()) return null;
        String cleaned = rawUserId.replaceAll("[^0-9]", "");
        return cleaned.isEmpty() ? null : Long.parseLong(cleaned);
    }

    @SuppressWarnings("unchecked")
    private String parseGeminiResponse(Map<String, Object> geminiResponse) {
        if (geminiResponse == null || !geminiResponse.containsKey("candidates")) {
            return null;
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) geminiResponse.get("candidates");
        if (candidates == null || candidates.isEmpty()) return null;

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) return null;

        List<Map<String, Object>> partsList = (List<Map<String, Object>>) content.get("parts");
        if (partsList == null || partsList.isEmpty()) return null;

        return (String) partsList.get(0).get("text");
    }

    // ==========================================
    // AES ENCRYPTION & DECRYPTION
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
            return encryptedValue;
        }
    }



    // ==========================================
    // 4. REJECT AND CLOSE PULL REQUEST
    // ==========================================
    @PostMapping("/reject-pr")
    public ResponseEntity<Map<String, Object>> rejectPullRequest(@RequestBody Map<String, Object> request) {
        try {
            // Parse payload values

            Long userId = authUserService.getLoggedInUserId();

            // Fallback: If userId is not supplied in body, fetch from active Auth session
            if (userId == null) {
                userId = authUserService.getLoggedInUserId();
            }

            if (!request.containsKey("pullNumber") || !request.containsKey("repoPath")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Missing required fields: pullNumber or repoPath"
                ));
            }

            int prNumber = Integer.parseInt(String.valueOf(request.get("pullNumber")));
            String repoPath = sanitizeRepoPath(String.valueOf(request.get("repoPath")));
            String rejectReason = request.get("rejectReason") != null ? String.valueOf(request.get("rejectReason")) : "No reason provided.";

            // Prefer gitToken provided in request body; fall back to encrypted DB token if omitted
            String gitToken = request.get("gitToken") != null ? String.valueOf(request.get("gitToken")) : null;

            if ((gitToken == null || gitToken.isBlank()) && userId != null) {
                Optional<UserGitConfig> configOpt = configRepository.findByUserId(userId);
                if (configOpt.isPresent()) {
                    gitToken = decrypt(configOpt.get().getEncryptedGitToken());
                }
            }

            if (gitToken == null || gitToken.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "GitHub access token is required to execute this operation."
                ));
            }

            String[] parts = repoPath.split("/");
            if (parts.length < 2) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Invalid repository path pattern. Expected 'owner/repo'."
                ));
            }
            String owner = parts[0];
            String repo = parts[1];

            String authHeader = gitToken.startsWith("github_pat_") ? "Bearer " + gitToken : "token " + gitToken;

            // Step 1: Post rejection feedback comment on the PR
            String commentMarkdown = "### ❌ Pull Request Rejected\n\n**Reason:** " + rejectReason;
            postGithubPrComment(owner, repo, prNumber, authHeader, commentMarkdown);

            // Step 2: Close the Pull Request via GitHub REST API
            boolean closed = closeGithubPr(owner, repo, prNumber, authHeader);

            if (closed) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Pull Request #" + prNumber + " has been rejected and closed successfully.",
                        "repoPath", repoPath,
                        "pullNumber", prNumber,
                        "rejectReason", rejectReason
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error", "Posted rejection comment, but failed to close Pull Request #" + prNumber + " on GitHub."
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to reject PR: " + e.getMessage()
            ));
        }
    }

    // ==========================================
    // HELPER: CLOSE GITHUB PULL REQUEST
    // ==========================================
    // ==========================================
    // HELPER: CLOSE GITHUB PULL REQUEST (FIXED)
    // ==========================================
    private boolean closeGithubPr(String owner, String repo, int prNumber, String authHeader) {
        try {
            RestClient restClient = RestClient.create();

            // GitHub PATCH endpoint payload to close PR
            Map<String, Object> patchBody = Map.of("state", "closed");

            ResponseEntity<Map<String, Object>> response = restClient.patch()
                    .uri("https://api.github.com/repos/{owner}/{repo}/pulls/{number}", owner, repo, prNumber)
                    .header("Authorization", authHeader)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Spring-Boot-Git-Integration")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .body(patchBody)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Successfully closed PR #" + prNumber + " on GitHub.");
                return true;
            } else {
                System.err.println("GitHub returned status " + response.getStatusCode() + " while closing PR #" + prNumber);
                return false;
            }

        } catch (HttpStatusCodeException e) {
            System.err.println("GitHub API error closing PR #" + prNumber + " [HTTP " + e.getStatusCode() + "]: " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected exception while closing PR #" + prNumber + ": " + e.getMessage());
            return false;
        }
    }
}