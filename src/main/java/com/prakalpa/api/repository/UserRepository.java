package com.prakalpa.api.repository;

import com.prakalpa.api.models.Users;
import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);

    // Optional: Useful for registration validation checks
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    Optional<Users> findById(Long id);

}
