package com.prakalpa.api.services;

import com.prakalpa.api.models.FetchTreeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GitTreeService {

    private final RestClient restClient;

    public GitTreeService() {
        this.restClient = RestClient.create();
    }

    public Object fetchRepositoryTree(FetchTreeRequest request) {
        String rawBaseUrl = request.getHostUrl() != null ? request.getHostUrl().replaceAll("/+$", "") : "https://github.com";
        String apiBaseUrl = rawBaseUrl.contains("github.com") ? "https://api.github.com" : rawBaseUrl + "/api/v3";

        String repoPath = request.getRepoPath(); // e.g., "it13manoj/jira-tool"
        String branch = request.getBranch();     // e.g., "main"
        String token = request.getGitToken();
        String subPath = request.getPath();      // e.g., "src" or ""

        try {
            // Case 1: Subdirectory requested (e.g., path = "src")
            if (subPath != null && !subPath.trim().isEmpty()) {
                String contentsUrl = String.format("%s/repos/%s/contents/%s?ref=%s",
                        apiBaseUrl, repoPath, subPath, branch);

                return restClient.get()
                        .uri(contentsUrl)
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "SpringBoot-GitTreeService")
                        .retrieve()
                        .body(List.class); // Returns list of files/folders in that subdirectory
            }

            // Case 2: Root level tree request (path is empty "")
            String treeUrl = String.format("%s/repos/%s/git/trees/%s", apiBaseUrl, repoPath, branch);
            if (request.isRecursive()) {
                treeUrl += "?recursive=1";
            }

            return restClient.get()
                    .uri(treeUrl)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "SpringBoot-GitTreeService")
                    .retrieve()
                    .body(Map.class);

        } catch (HttpClientErrorException e) {
            String githubErrorDetails = e.getResponseBodyAsString();
            System.err.println("GitHub API Error Status: " + e.getStatusCode());
            System.err.println("GitHub API Error Body: " + githubErrorDetails);

            if (e.getStatusCode() == HttpStatus.FORBIDDEN || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new RuntimeException("GitHub Authentication Error (" + e.getStatusCode() + "): " + githubErrorDetails, e);
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new RuntimeException("Directory, Repository, or Branch not found: " + subPath, e);
            }
            throw new RuntimeException("GitHub API Error [" + e.getStatusCode() + "]: " + githubErrorDetails, e);
        }
    }
}