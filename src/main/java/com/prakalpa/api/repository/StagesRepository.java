package com.prakalpa.api.repository;

import com.prakalpa.api.models.Stages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StagesRepository extends JpaRepository<Stages, Long> {
    Optional<Stages> findByIdAndTypesFalse(Long Id);

    Optional<Stages> findByIdAndTypesTrue(Long Id);
    List<Stages> findByTypesTrue();
}
