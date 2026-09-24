package com.prakalpa.api.services;


import com.prakalpa.api.interfacedto.AssignInfo;
import com.prakalpa.api.models.Assigns;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssignServices implements AssignInfo {
    @Override
    public void create(Assigns assigns) {

    }

    @Override
    public List<Assigns> findAll() {
        return List.of();
    }

    @Override
    public Optional<Assigns> findOne(Long Id) {
        return Optional.empty();
    }

    @Override
    public Assigns findById(Long Id) {
        return null;
    }
}
