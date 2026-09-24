package com.prakalpa.api.interfacedto;

import com.prakalpa.api.models.ProjectTypes;

import java.util.List;
import java.util.Optional;

public interface ProjectTypeInfo {
    void create(ProjectTypes projectTypes);
    List<ProjectTypes> findAll();
    Optional<ProjectTypes> findOne(Long id);

}
