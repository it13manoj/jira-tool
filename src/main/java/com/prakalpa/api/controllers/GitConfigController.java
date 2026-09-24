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
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users/git")
public class GitConfigController {
    private static final String SECRET_KEY = "Your32ByteLongSecretKeyHere!!!!!";

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
            throw new RuntimeException("Error decrypting value: " + e.getMessage(), e);
        }
    }


    @Autowired
    private UserGitConfigRepository configRepository;

    @Autowired
    private AuthUserService authUserService;

    @PostMapping("/save-config")
    public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody Map<String, String> request) {
        try {

            String repoPath = sanitizeRepoPath(request.get("repoPath"));
            String gitToken = request.get("gitToken");
            String geminiKey = request.get("geminiApiKey");

            // 1. Save or Update Configuration in Database
            System.out.println(authUserService.getLoggedInUserId());

            Optional<UserGitConfig> config = configRepository.findByUserId(authUserService.getLoggedInUserId());
            if(config.isPresent()){
                UserGitConfig config1 = config.get();
                config1.setUserId(authUserService.getLoggedInUserId());
                config1.setRepoPath(repoPath);
                config1.setEncryptedGitToken(encrypt(gitToken));
                config1.setEncryptedGeminiApiKey(encrypt(geminiKey));
                configRepository.save(config1);
            }else{
                UserGitConfig config1 = new UserGitConfig();
                config1.setUserId(authUserService.getLoggedInUserId());
                config1.setRepoPath(repoPath);
                config1.setEncryptedGitToken(encrypt(gitToken));
                config1.setEncryptedGeminiApiKey(encrypt(geminiKey));
                configRepository.save(config1);
            }


            // 2. Programmatically create GitHub Webhook on user's repo automatically
            boolean webhookCreated = registerGithubWebhook(repoPath, gitToken);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuration saved! Webhook status: " + (webhookCreated ? "Connected" : "Manual review required")
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Programmatically registers your platform's webhook endpoint on the user's repository using the GitHub API
     */
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
            // Webhook might already exist or require admin access; fail gracefully
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
        // Implement Encryption Algorithm (e.g. AES-256)
        return value;
    }

    @PostMapping("/review-pr")
    public ResponseEntity<Map<String, Object>> reviewPullRequest(@RequestBody Map<String, String> request) {
        try {
            Long userId = Long.parseLong(request.get("userId"));
            int prNumber = Integer.parseInt(request.get("prNumber"));

            // 1. Retrieve saved user credentials securely from DB
            Optional<UserGitConfig> config = configRepository.findByUserId(userId);

            String repoPath = config.get().getRepoPath();
            String gitToken = decrypt(config.get().getEncryptedGitToken());
            String geminiApiKey = decrypt(config.get().getEncryptedGeminiApiKey());

            String[] parts = repoPath.split("/");
            String owner = parts[0];
            String repo = parts[1];

            // 2. Fetch Diff using User's Git Token
            RestClient restClient = RestClient.create();
            String authHeader = gitToken.startsWith("github_pat_") ? "Bearer " + gitToken : "token " + gitToken;

            List<Map<String, Object>> files = restClient.get()
                    .uri("https://api.github.com/repos/{owner}/{repo}/pulls/{number}/files", owner, repo, prNumber)
                    .header("Authorization", authHeader)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            StringBuilder diffContent = new StringBuilder();
            if (files != null) {
                for (Map<String, Object> file : files) {
                    diffContent.append("File: ").append(file.get("filename")).append("\n");
                    diffContent.append("Patch:\n").append(file.get("patch")).append("\n\n");
                }
            }

            // 3. Perform AI Review using User's Gemini API Key
            String prompt = "Perform a thorough code review on this patch:\n\n" + diffContent;
            String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

            Map<String, Object> geminiResponse = restClient.post()
                    .uri(geminiUrl)
                    .body(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))))
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) geminiResponse.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> partsList = (List<Map<String, Object>>) content.get("parts");
            String aiReview = (String) partsList.get(0).get("text");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "review", aiReview
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}