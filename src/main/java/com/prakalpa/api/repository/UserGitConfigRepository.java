package com.prakalpa.api.repository;

import com.prakalpa.api.models.UserGitConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserGitConfigRepository extends JpaRepository<UserGitConfig, Long> {
//    UserGitConfig findByUserId(Long id);

    Optional<UserGitConfig> findByUserId(Long id);
    Optional<UserGitConfig> findByRepoPath(String repoPath);

}
