package com.prakalpa.api.interfacedto;

import com.prakalpa.api.models.Priority;

import java.util.List;
import java.util.Optional;

public interface PriorityInfo {

    void create(Priority priority);

    List<Priority> findAll();

    Optional<Priority> findOne(Long Id);

    Priority findById(Long Id);
}
