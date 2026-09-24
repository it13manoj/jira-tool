package com.prakalpa.api.repository;

import com.prakalpa.api.models.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Roles, Long> {

    @Override
    Optional<Roles> findById(Long id);


}
