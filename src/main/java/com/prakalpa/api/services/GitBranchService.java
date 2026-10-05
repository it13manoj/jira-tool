package com.prakalpa.api.services;

import com.prakalpa.api.models.CreateBranchRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import java.util.HashMap;
import java.util.Map;

@Service
public class GitBranchService {

    private final RestClient restClient;

    public GitBranchService() {
        this.restClient = RestClient.create();
    }

    public Map<String, Object> createBranch(CreateBranchRequest request) {
        String baseUrl = request.getHostUrl().replaceAll("/+$", ""); // Strip trailing slashes
        String repoPath = request.getRepoPath();                     // e.g., "it13manoj/batohi_frontend"
        String token = request.getGitToken();

        // Step 1: Get the latest commit SHA of source branch (e.g., 'main')
        String getRefUrl = String.format("%s/api/v3/repos/%s/git/ref/heads/%s",
                baseUrl.contains("github.com") ? "https://api.github.com" : baseUrl,
                repoPath,
                request.getSourceBranch());

        Map<?, ?> refResponse = restClient.get()
                .uri(getRefUrl)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(Map.class);

        if (refResponse == null || !refResponse.containsKey("object")) {
            throw new RuntimeException("Could not fetch SHA for source branch: " + request.getSourceBranch());
        }

        Map<?, ?> objectMap = (Map<?, ?>) refResponse.get("object");
        String sha = (String) objectMap.get("sha");

        // Step 2: Create reference for the new branch
        String createRefUrl = String.format("%s/api/v3/repos/%s/git/refs",
                baseUrl.contains("github.com") ? "https://api.github.com" : baseUrl,
                repoPath);

        Map<String, String> body = new HashMap<>();
        body.put("ref", "refs/heads/" + request.getBranchName());
        body.put("sha", sha);

        return restClient.post()
                .uri(createRefUrl)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
    }
}
