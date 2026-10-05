package com.prakalpa.api.controllers;

import com.prakalpa.api.models.CreateBranchRequest;
import com.prakalpa.api.models.FetchRepositoriesRequest;
import com.prakalpa.api.models.FetchTreeRequest;
import com.prakalpa.api.models.FileRequest;
import com.prakalpa.api.services.GitBranchService;
import com.prakalpa.api.services.GitFileService;
import com.prakalpa.api.services.GitRepositoryService;
import com.prakalpa.api.services.GitTreeService;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.*;

@RestController
@RequestMapping("/api/v1/users/git")
@CrossOrigin(origins = "*")
public class GitPortalController {
    @Autowired
    private GitBranchService gitBranchService;

    @Autowired
    private GitRepositoryService gitRepositoryService;

    @Autowired
    private GitTreeService gitTreeService;

    @Autowired
    private GitFileService gitFileService;

    // Single constructor injecting both dependencies
    public GitPortalController(
            GitBranchService gitBranchService,
            GitRepositoryService gitRepositoryService,
            GitTreeService gitTreeService, GitFileService gitFileService) {
        this.gitBranchService = gitBranchService;
        this.gitRepositoryService = gitRepositoryService;
        this.gitTreeService = gitTreeService;
        this.gitFileService= gitFileService;
    }

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

    @PostMapping("/create-branch")
    public ResponseEntity<Map<String, Object>> createBranch(@RequestBody CreateBranchRequest request) {
        try {
            // Branch creation service logic
            Map<String, Object> result = gitBranchService.createBranch(request);
            return ResponseEntity.ok(result);
        } catch (org.springframework.web.client.HttpClientErrorException.Forbidden e) {
            // Print exact error returned by GitHub API to stdout/logs
            System.err.println("GitHub API Error Details: " + e.getResponseBodyAsString());
            throw e;
        }
    }

    @PostMapping("/repositories")
    public ResponseEntity<List<?>> getRepositories(@RequestBody FetchRepositoriesRequest request) {
        List<?> repositories = gitRepositoryService.fetchRepositories(request);
        return ResponseEntity.ok(repositories);
    }


    @PostMapping("/repository/tree")
    public ResponseEntity<Object> getRepositoryTree(@RequestBody FetchTreeRequest request) {
        Object treeData = gitTreeService.fetchRepositoryTree(request);
        return ResponseEntity.ok(treeData);
    }


    @PostMapping("/repository/file")
    public ResponseEntity<Map<String, Object>> readFile(@RequestBody FileRequest request) {
        Map<String, Object> fileData = gitFileService.readFile(request);
        return ResponseEntity.ok(fileData);
    }

    @PutMapping("/repository/file/commit")
    public ResponseEntity<Map<String, Object>> commitFile(@RequestBody FileRequest request) {
        Map<String, Object> commitResult = gitFileService.commitFile(request);
        return ResponseEntity.ok(commitResult);
    }

    @PostMapping("/repository/commit-push")
    public ResponseEntity<Map<String, Object>> commitAndPush(@RequestBody FileRequest request) {
        Map<String, Object> result = gitFileService.commitAndPushFile(request);
        return ResponseEntity.ok(result);
    }

}