package com.prakalpa.api.controllers;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.kohsuke.github.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.*;

@RestController
@RequestMapping("/api/v1/users/git")
@CrossOrigin(origins = "*")
public class GitPortalController {

    // 1. Connect & Verify Git Token
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> request) {
        String gitToken = request.get("gitToken");
        try {
            GitHub github = new GitHubBuilder().withOAuthToken(gitToken).build();
            GHMyself user = github.getMyself();
            return ResponseEntity.ok(Map.of("success", true, "user", user.getLogin()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Invalid Git Token"));
        }
    }

    // 2. Fetch Open PRs
    @PostMapping(path = "/pull-requests")
    public ResponseEntity<Map<String, Object>> getPullRequests(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("gitToken");
            String rawRepoPath = request.get("repoPath");

            // 1. Clean path to ensure 'owner/repo' format
            String repoPath = sanitizeRepoPath(rawRepoPath);
            if (!repoPath.contains("/")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Please provide owner/repository format (e.g. 'it13manoj/lms_frontend')"
                ));
            }

            // Split owner and repo so Spring doesn't encode the slash '/'
            String[] parts = repoPath.split("/");
            String owner = parts[0];
            String repo = parts[1];

            // 2. Format Auth Header
            String authHeader = (token != null && token.startsWith("github_pat_"))
                    ? "Bearer " + token
                    : "token " + token;

            // 3. Make Request with separate {owner} and {repo} variables
            RestClient restClient = RestClient.create();

            List<Map<String, Object>> rawPrs = restClient.get()
                    .uri("https://api.github.com/repos/{owner}/{repo}/pulls?state=open", owner, repo)
                    .header("Authorization", authHeader)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            // 4. Transform response
            List<Map<String, Object>> prList = new ArrayList<>();
            if (rawPrs != null) {
                for (Map<String, Object> pr : rawPrs) {
                    Map<String, Object> user = (Map<String, Object>) pr.get("user");
                    prList.add(Map.of(
                            "number", pr.get("number"),
                            "title", pr.get("title"),
                            "author", user != null ? user.get("login") : "unknown"
                    ));
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "pullRequests", prList));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
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

    // 3. Gemini Code Review & Merge Action
//    @PostMapping("/review-and-merge")
//    public ResponseEntity<Map<String, Object>> reviewAndMerge(@RequestBody Map<String, Object> request) {
//        String gitToken = (String) request.get("gitToken");
//        String geminiKey = (String) request.get("geminiKey");
//        String repoPath = (String) request.get("repoPath");
//        int pullNumber = (Integer) request.get("pullNumber");
//        String action = (String) request.get("action"); // "review" or "merge"
//
//        try {
//            // A. Fetch PR Diffs
//            GitHub github = new GitHubBuilder().withOAuthToken(gitToken).build();
//            GHRepository repository = github.getRepository(repoPath);
//            GHPullRequest pr = repository.getPullRequest(pullNumber);
//
//            StringBuilder diffContent = new StringBuilder();
//            for (GHPullRequestFileDetail file : pr.listFiles()) {
//                diffContent.append("File: ").append(file.getFilename()).append("\n");
//                diffContent.append("Patch:\n").append(file.getPatch()).append("\n\n");
//            }
//
//            // B. Analyze Diffs with Gemini
//            Client client = Client.builder().apiKey(geminiKey).build();
//            String prompt = "You are an expert code reviewer. Review this PR patch and provide concise bug findings and actionable feedback:\n\n" + diffContent.toString();
//
//            GenerateContentResponse response = client.models.generateContent("gemini-3.6-flash", prompt, null);
//            String aiReview = response.text();
//
//            // C. Post Review Comment on Git PR
//            pr.comment("### 🤖 Gemini AI Code Review Session\n\n" + aiReview);
//
//            // D. Merge PR if requested
//            boolean isMerged = false;
//            if ("merge".equalsIgnoreCase(action)) {
//                pr.merge("Merged via Portal after Gemini AI Review");
//                isMerged = true;
//            }
//
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "reviewFeedback", aiReview,
//                    "merged", isMerged
//            ));
//
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
//        }
//    }
}