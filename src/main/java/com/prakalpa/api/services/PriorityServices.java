package com.prakalpa.api.services;

import com.prakalpa.api.interfacedto.PriorityInfo;
import com.prakalpa.api.models.Priority;
import com.prakalpa.api.repository.PriorityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PriorityServices implements PriorityInfo {
    @Autowired
    private PriorityRepository repository;

    @Autowired
    private AuthUserService authUserService;

    @Override
    public void create(Priority priority) {
        priority.setCreatedBy(authUserService.getLoggedInUser());
        repository.save(priority);
    }

    @Override
    public List<Priority> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Priority> findOne(Long Id) {
        return Optional.empty();
    }

    @Override
    public Priority findById(Long Id) {
        return null;
    }
}
