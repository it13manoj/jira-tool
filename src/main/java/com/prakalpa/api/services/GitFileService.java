package com.prakalpa.api.services;

import com.prakalpa.api.models.FileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class GitFileService {

    private final RestClient restClient;

    public GitFileService() {
        this.restClient = RestClient.create();
    }

    /**
     * Reads a file and decodes its Base64 content into plain text along with its SHA.
     */
    public Map<String, Object> readFile(FileRequest request) {
        String apiBaseUrl = getApiBaseUrl(request.getHostUrl());
        String url = String.format("%s/repos/%s/contents/%s?ref=%s",
                apiBaseUrl, request.getRepoPath(), request.getPath(), request.getBranch());

        try {
            Map<?, ?> response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + request.getGitToken())
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "SpringBoot-GitFileService")
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("content")) {
                throw new RuntimeException("Unable to read file content from response.");
            }

            // Decode Base64 content from GitHub
            String rawBase64 = ((String) response.get("content")).replaceAll("\\s+", "");
            byte[] decodedBytes = Base64.getDecoder().decode(rawBase64);
            String plainContent = new String(decodedBytes, StandardCharsets.UTF_8);

            Map<String, Object> result = new HashMap<>();
            result.put("name", response.get("name"));
            result.put("path", response.get("path"));
            result.put("sha", response.get("sha")); // Essential for committing updates later
            result.put("content", plainContent);
            result.put("encoding", "utf-8");

            return result;

        } catch (HttpClientErrorException e) {
            handleError("Read File", e);
            return null;
        }
    }

    /**
     * Updates an existing file in GitHub by committing changes.
     */
    public Map<String, Object> commitFile(FileRequest request) {
        String apiBaseUrl = getApiBaseUrl(request.getHostUrl());
        String url = String.format("%s/repos/%s/contents/%s",
                apiBaseUrl, request.getRepoPath(), request.getPath());

        // Base64 encode the updated plain text content
        String encodedContent = Base64.getEncoder().encodeToString(
                request.getContent().getBytes(StandardCharsets.UTF_8)
        );

        Map<String, Object> body = new HashMap<>();
        body.put("message", request.getCommitMessage() != null ? request.getCommitMessage() : "Update " + request.getPath());
        body.put("content", encodedContent);
        body.put("branch", request.getBranch());
        body.put("sha", request.getSha()); // Mandatory for updating existing files

        try {
            return restClient.put()
                    .uri(url)
                    .header("Authorization", "Bearer " + request.getGitToken())
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "SpringBoot-GitFileService")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

        } catch (HttpClientErrorException e) {
            handleError("Commit File", e);
            return null;
        }
    }

    private String getApiBaseUrl(String hostUrl) {
        String rawBaseUrl = hostUrl != null ? hostUrl.replaceAll("/+$", "") : "https://github.com";
        return rawBaseUrl.contains("github.com") ? "https://api.github.com" : rawBaseUrl + "/api/v3";
    }

    private void handleError(String operation, HttpClientErrorException e) {
        String githubErrorDetails = e.getResponseBodyAsString();
        System.err.println("GitHub API Error (" + operation + "): " + e.getStatusCode());
        System.err.println("Body: " + githubErrorDetails);

        if (e.getStatusCode() == HttpStatus.FORBIDDEN || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            throw new RuntimeException("GitHub Authentication/Permission Error (" + e.getStatusCode() + "): " + githubErrorDetails, e);
        } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new RuntimeException("File or Repository path not found.", e);
        }
        throw new RuntimeException("GitHub API Error [" + e.getStatusCode() + "]: " + githubErrorDetails, e);
    }

    public Map<String, Object> commitAndPushFile(FileRequest request) {
        String rawBaseUrl = request.getHostUrl() != null ? request.getHostUrl().replaceAll("/+$", "") : "https://github.com";
        String apiBaseUrl = rawBaseUrl.contains("github.com") ? "https://api.github.com" : rawBaseUrl + "/api/v3";

        String url = String.format("%s/repos/%s/contents/%s",
                apiBaseUrl, request.getRepoPath(), request.getPath());

        // Base64 encode the content (e.g., Vue template or Java code)
        String encodedContent = Base64.getEncoder().encodeToString(
                request.getContent().getBytes(StandardCharsets.UTF_8)
        );

        Map<String, Object> body = new HashMap<>();
        body.put("message", request.getCommitMessage() != null ? request.getCommitMessage() : "Create " + request.getPath());
        body.put("content", encodedContent);
        body.put("branch", request.getBranch());

        // Include SHA only if updating an existing file
        if (!request.isNewFile() && request.getSha() != null) {
            body.put("sha", request.getSha());
        }

        try {
            return restClient.put()
                    .uri(url)
                    .header("Authorization", "Bearer " + request.getGitToken())
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "SpringBoot-GitFileService")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

        } catch (HttpClientErrorException e) {
            String githubErrorDetails = e.getResponseBodyAsString();
            System.err.println("GitHub API Error (Commit File): " + e.getStatusCode());
            System.err.println("Response Body: " + githubErrorDetails);

            if (e.getStatusCode() == HttpStatus.FORBIDDEN || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new RuntimeException("GitHub Authentication/Permission Error (" + e.getStatusCode() + "): " + githubErrorDetails, e);
            } else if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                throw new RuntimeException("File creation failed. Check if file already exists or branch is locked: " + githubErrorDetails, e);
            }
            throw new RuntimeException("GitHub API Error [" + e.getStatusCode() + "]: " + githubErrorDetails, e);
        }
    }
}