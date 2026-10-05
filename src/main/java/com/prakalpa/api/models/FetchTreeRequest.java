package com.prakalpa.api.models;

public class FetchTreeRequest {
    private String branch;
    private String gitToken;
    private String hostUrl;
    private String path;
    private String provider;
    private boolean recursive;
    private String repoPath;
    private String userId;

    // Getters and Setters
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getGitToken() { return gitToken; }
    public void setGitToken(String gitToken) { this.gitToken = gitToken; }

    public String getHostUrl() { return hostUrl; }
    public void setHostUrl(String hostUrl) { this.hostUrl = hostUrl; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public boolean isRecursive() { return recursive; }
    public void setRecursive(boolean recursive) { this.recursive = recursive; }

    public String getRepoPath() { return repoPath; }
    public void setRepoPath(String repoPath) { this.repoPath = repoPath; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}