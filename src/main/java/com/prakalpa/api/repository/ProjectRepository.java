package com.prakalpa.api.repository;

import com.prakalpa.api.models.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Projects, Long> {
    Optional<Projects> findById(Long id);

}
