package com.prakalpa.api.services;

import com.prakalpa.api.models.CreateBranchRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class GitBranchService {

    private final RestClient restClient;

    public GitBranchService() {
        this.restClient = RestClient.create();
    }

    public Map<String, Object> createBranch(CreateBranchRequest request) {
        String rawBaseUrl = request.getHostUrl() != null ? request.getHostUrl().replaceAll("/+$", "") : "https://github.com";
        // Standardize base endpoint: use api.github.com for github.com, or enterprise base API path
        String apiBaseUrl = rawBaseUrl.contains("github.com") ? "https://api.github.com" : rawBaseUrl + "/api/v3";

        String repoPath = request.getRepoPath(); // e.g., "it13manoj/batohi_frontend"
        String token = request.getGitToken();

        try {
            // Step 1: Get the latest commit SHA of source branch (e.g., 'main')
            String getRefUrl = String.format("%s/repos/%s/git/ref/heads/%s",
                    apiBaseUrl,
                    repoPath,
                    request.getSourceBranch());

            Map<?, ?> refResponse = restClient.get()
                    .uri(getRefUrl)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "SpringBoot-GitBranchService") // GitHub API requires User-Agent
                    .retrieve()
                    .body(Map.class);

            if (refResponse == null || !refResponse.containsKey("object")) {
                throw new RuntimeException("Could not fetch SHA for source branch: " + request.getSourceBranch());
            }

            Map<?, ?> objectMap = (Map<?, ?>) refResponse.get("object");
            String sha = (String) objectMap.get("sha");

            // Step 2: Create reference for the new branch
            String createRefUrl = String.format("%s/repos/%s/git/refs",
                    apiBaseUrl,
                    repoPath);

            Map<String, String> body = new HashMap<>();
            body.put("ref", "refs/heads/" + request.getBranchName());
            body.put("sha", sha);

            return restClient.post()
                    .uri(createRefUrl)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "SpringBoot-GitBranchService")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

        } catch (HttpClientErrorException e) {
            // Unpack exact GitHub 403 error body (e.g., SAML SSO enforcement, protected branch rule, missing permissions)
            String githubErrorDetails = e.getResponseBodyAsString();
            System.err.println("GitHub API Error Status: " + e.getStatusCode());
            System.err.println("GitHub API Response Body: " + githubErrorDetails);

            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new RuntimeException("GitHub Branch Creation Denied (403): " + githubErrorDetails, e);
            }
            throw new RuntimeException("GitHub API Error [" + e.getStatusCode() + "]: " + githubErrorDetails, e);
        }
    }
}