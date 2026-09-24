package com.prakalpa.api.interfacedto;

import com.prakalpa.api.models.Projects;

import java.util.List;
import java.util.Optional;

public interface ProjectsInfo {
    void create(Projects projects);

    List<Projects> findAll();

    Optional<Projects> findOne(Long id);
}
