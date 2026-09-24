package com.prakalpa.api.interfacedto;

import com.prakalpa.api.models.Stages;

import java.util.List;
import java.util.Optional;

public interface StageInfo {
    void Create(Stages stages);

    Stages findById(Long Id);

    List<Stages> findAll();

    Optional<Stages> findOne(Long Id);

    List<Stages> findIssueType();

}
