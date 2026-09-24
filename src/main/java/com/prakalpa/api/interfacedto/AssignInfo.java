package com.prakalpa.api.interfacedto;

import com.prakalpa.api.models.Assigns;

import java.util.List;
import java.util.Optional;

public interface AssignInfo {
    void create(Assigns assigns);

    List<Assigns> findAll();

    Optional<Assigns> findOne(Long Id);

    Assigns findById(Long Id);
}
