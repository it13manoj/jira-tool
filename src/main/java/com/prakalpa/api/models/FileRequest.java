package com.prakalpa.api.models;

public class FileRequest {
    private String branch;
    private String commitMessage;
    private String content;
    private String gitToken;
    private String hostUrl;
    private boolean isNewFile;
    private String operation;   // e.g., "create"
    private String path;
    private String provider;
    private String repoPath;
    private String userId;
    private String sha;         // Null/optional for new file creation

    // Getters and Setters
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getCommitMessage() { return commitMessage; }
    public void setCommitMessage(String commitMessage) { this.commitMessage = commitMessage; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getGitToken() { return gitToken; }
    public void setGitToken(String gitToken) { this.gitToken = gitToken; }

    public String getHostUrl() { return hostUrl; }
    public void setHostUrl(String hostUrl) { this.hostUrl = hostUrl; }

    public boolean isNewFile() { return isNewFile; }
    public void setNewFile(boolean newFile) { isNewFile = newFile; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getRepoPath() { return repoPath; }
    public void setRepoPath(String repoPath) { this.repoPath = repoPath; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }
}