package com.prakalpa.api.repository;


import com.prakalpa.api.models.Images;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImagesRepository extends JpaRepository<Images, Long> {
    @Override
    Optional<Images> findById(Long Id);
}
