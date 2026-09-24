package com.prakalpa.api.repository;

import com.prakalpa.api.models.Assigns;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssignRepository extends JpaRepository<Assigns,Long> {
    @Override
    Optional<Assigns> findById(Long aLong);
}
