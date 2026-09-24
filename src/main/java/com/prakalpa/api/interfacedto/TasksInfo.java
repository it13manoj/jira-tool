package com.prakalpa.api.interfacedto;

import com.prakalpa.api.models.Tasks;

import java.util.List;
import java.util.Optional;


public interface TasksInfo {
    void create(Tasks tasks);

    List<Tasks> findAll();

    Optional<Tasks> findOne(Long Id);

    Tasks findById(Long Id);

}
