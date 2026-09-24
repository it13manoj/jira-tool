package com.prakalpa.api.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_git_configs")
@Data
public class UserGitConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId; // Links to logged-in platform user

    @Column(nullable = false)
    private String repoPath; // e.g., "it13manoj/lms_frontend"

    @Column(nullable = false)
    private String encryptedGitToken;

    @Column(nullable = false)
    private String encryptedGeminiApiKey;


}