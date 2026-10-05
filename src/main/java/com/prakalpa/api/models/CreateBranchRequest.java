package com.prakalpa.api.models;

public class CreateBranchRequest {
    private String branchName;
    private String gitToken;
    private String hostUrl;
    private String provider;
    private String repoPath;
    private String sourceBranch;
    private String userId;

    // Getters and Setters
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getGitToken() { return gitToken; }
    public void setGitToken(String gitToken) { this.gitToken = gitToken; }

    public String getHostUrl() { return hostUrl; }
    public void setHostUrl(String hostUrl) { this.hostUrl = hostUrl; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getRepoPath() { return repoPath; }
    public void setRepoPath(String repoPath) { this.repoPath = repoPath; }

    public String getSourceBranch() { return sourceBranch; }
    public void setSourceBranch(String sourceBranch) { this.sourceBranch = sourceBranch; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}