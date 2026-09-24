package com.prakalpa.api.repository;

import com.prakalpa.api.models.Tasks;
import com.prakalpa.api.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskReposigory extends JpaRepository<Tasks, Long> {
    @Override
    Optional<Tasks> findById(Long aLong);

    List<Tasks> findByCreatedByOrAssignTo(Users createdBy, Users assignTo);
}
