package com.prakalpa.api.repository;

import com.prakalpa.api.models.ProjectTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectTypeRepository extends JpaRepository<ProjectTypes , Long> {
    Optional<ProjectTypes> findById(Long id);
}
