package com.prakalpa.api.models;

public class FetchRepositoriesRequest {
    private String gitToken;
    private String hostUrl;
    private String provider;
    private String userId;

    // Getters and Setters
    public String getGitToken() { return gitToken; }
    public void setGitToken(String gitToken) { this.gitToken = gitToken; }

    public String getHostUrl() { return hostUrl; }
    public void setHostUrl(String hostUrl) { this.hostUrl = hostUrl; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
