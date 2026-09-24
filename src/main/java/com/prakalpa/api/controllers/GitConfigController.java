package com.prakalpa.api.controllers;

import com.prakalpa.api.models.UserGitConfig;
import com.prakalpa.api.repository.UserGitConfigRepository;
import com.prakalpa.api.services.AuthUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users/git")
public class GitConfigController {

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;
    // Hardcoded 32-byte key fallback to ensure Spring Boot starts up cleanly
    private static final String SECRET_KEY = "PrakalpaAES256SecretKeyString32!";

    @Autowired
    private UserGitConfigRepository configRepository;

    @Autowired
    private AuthUserService authUserService;

    @PostMapping("/save-config")
    public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody Map<String, String> request) {
        try {
            Long userId = authUserService.getLoggedInUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized access"));
            }

            String repoPath = sanitizeRepoPath(request.get("repoPath"));
            String gitToken = request.get("gitToken");
            String geminiKey = request.get("geminiApiKey");

            if (repoPath.isBlank() || gitToken == null || geminiKey == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid or missing parameters"));
            }

            UserGitConfig gitConfig = configRepository.findByUserId(userId)
                    .orElseGet(UserGitConfig::new);

            gitConfig.setUserId(userId);
            gitConfig.setRepoPath(repoPath);
            gitConfig.setEncryptedGitToken(encrypt(gitToken));
            gitConfig.setEncryptedGeminiApiKey(encrypt(geminiKey));

            configRepository.save(gitConfig);

            boolean webhookCreated = registerGithubWebhook(repoPath, gitToken);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuration saved! Webhook status: " + (webhookCreated ? "Connected" : "Manual review required")
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/review-and-merge")
    public ResponseEntity<Map<String, Object>> reviewAndMergePullRequest(@RequestBody Map<String, String> request) {
        try {
            if (!request.containsKey("userId") || !request.containsKey("prNumber")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing userId or prNumber"));
            }

            Long userId = Long.parseLong(request.get("userId"));
            int prNumber = Integer.parseInt(request.get("prNumber"));

            Optional<UserGitConfig> configOpt = configRepository.findByUserId(userId);
            if (configOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Configuration not found for user"));
            }

            UserGitConfig config = configOpt.get();
            String repoPath = config.getRepoPath();
            String gitToken = decrypt(config.getEncryptedGitToken());
            String geminiApiKey = decrypt(config.getEncryptedGeminiApiKey());

            String[] parts = repoPath.split("/");
            if (parts.length < 2) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid repository path pattern"));
            }
            String owner = parts[0];
            String repo = parts[1];

            RestClient restClient = RestClient.create();
            String authHeader = gitToken.startsWith("github_pat_") ? "Bearer " + gitToken : "token " + gitToken;

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
                return ResponseEntity.ok(Map.of("success", true, "message", "No visible diffs found for this PR."));
            }

            String prompt = "You are an automated senior code reviewer. Perform a thorough code review on this Git pull request diff. " +
                    "Structure your review clearly with a decision at the end starting with 'RECOMMENDATION: APPROVE' or 'RECOMMENDATION: REJECT'.\n\n" + diffContent;

            String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

            Map<String, Object> responseBody = restClient.post()
                    .uri(geminiUrl)
                    .body(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))))
                    .retrieve()
                    .body(Map.class);

            String aiReview = parseGeminiResponse(responseBody);

            postGithubPrComment(owner, repo, prNumber, authHeader, aiReview);

            boolean merged = false;
            String mergeMessage = "PR reviewed successfully.";

            if (aiReview.contains("RECOMMENDATION: APPROVE") || !aiReview.contains("RECOMMENDATION: REJECT")) {
                merged = mergeGithubPr(owner, repo, prNumber, authHeader);
                mergeMessage = merged ? "PR reviewed and merged successfully!" : "PR reviewed and commented, but automated merge failed on GitHub.";
            } else {
                mergeMessage = "PR reviewed with requested changes. Merge aborted due to review findings.";
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "merged", merged,
                    "review", aiReview,
                    "message", mergeMessage
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "Failed PR Review & Merge: " + e.getMessage()));
        }
    }

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
            System.err.println("Failed to post comment to GitHub: " + e.getMessage());
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
            System.err.println("Failed to merge PR on GitHub: " + e.getMessage());
            return false;
        }
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
                            "url", "https://api.wdpcare.com/api/v1/users/git/webhook",
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

    private String encrypt(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting value", e);
        }
    }

    private String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) return "";
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedValue);

            byte[] iv = new byte[IV_SIZE];
            byte[] cipherText = new byte[combined.length - IV_SIZE];

            System.arraycopy(combined, 0, iv, 0, IV_SIZE);
            System.arraycopy(combined, IV_SIZE, cipherText, 0, cipherText.length);

            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));

            byte[] decryptedBytes = cipher.doFinal(cipherText);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting value", e);
        }
    }
}