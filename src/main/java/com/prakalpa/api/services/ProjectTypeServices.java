package com.prakalpa.api.services;

import com.prakalpa.api.interfacedto.ProjectTypeInfo;
import com.prakalpa.api.models.ProjectTypes;
import com.prakalpa.api.repository.ProjectTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectTypeServices implements ProjectTypeInfo {
    @Autowired
    private ProjectTypeRepository projectTypeRepository;

    @Autowired
    private AuthUserService authUserService;

    @Override
    public void create(ProjectTypes projectTypes) {
        projectTypes.setCreatedBy(authUserService.getLoggedInUser());
        projectTypeRepository.save(projectTypes);
    }

    @Override
    public List<ProjectTypes> findAll() {
        return projectTypeRepository.findAll();
    }

    @Override
    public Optional<ProjectTypes> findOne(Long id) {
        return projectTypeRepository.findById(id);
    }
}
