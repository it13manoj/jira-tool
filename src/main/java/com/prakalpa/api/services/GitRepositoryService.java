package com.prakalpa.api.services;

import com.prakalpa.api.models.FetchRepositoriesRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GitRepositoryService {

    private final RestClient restClient;

    public GitRepositoryService() {
        this.restClient = RestClient.create();
    }

    public List<?> fetchRepositories(FetchRepositoriesRequest request) {
        String rawBaseUrl = request.getHostUrl() != null ? request.getHostUrl().replaceAll("/+$", "") : "https://github.com";
        String apiBaseUrl = rawBaseUrl.contains("github.com") ? "https://api.github.com" : rawBaseUrl + "/api/v3";

        String token = request.getGitToken();

        // Endpoint for fetching all repositories accessible to the authenticated user
        String url = String.format("%s/user/repos?type=all&sort=updated&per_page=100", apiBaseUrl);

        try {
            return restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "SpringBoot-GitRepositoryService") // GitHub API requires User-Agent
                    .retrieve()
                    .body(List.class);

        } catch (HttpClientErrorException e) {
            String githubErrorDetails = e.getResponseBodyAsString();
            System.err.println("GitHub API Error Status: " + e.getStatusCode());
            System.err.println("GitHub API Error Body: " + githubErrorDetails);

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new RuntimeException("GitHub Authentication Error (" + e.getStatusCode() + "): " + githubErrorDetails, e);
            }
            throw new RuntimeException("GitHub API Error [" + e.getStatusCode() + "]: " + githubErrorDetails, e);
        }
    }
}