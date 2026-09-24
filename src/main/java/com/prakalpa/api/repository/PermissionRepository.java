package com.prakalpa.api.repository;

import com.prakalpa.api.models.Permissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permissions, Long> {

    Optional<Permissions> findById(Long Id);
}
