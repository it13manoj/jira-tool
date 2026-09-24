package com.prakalpa.api.services;

import com.prakalpa.api.interfacedto.StageInfo;
import com.prakalpa.api.models.Stages;
import com.prakalpa.api.repository.StagesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StageServices implements StageInfo {
    @Autowired
    private StagesRepository stagesRepository;

    @Autowired
    private AuthUserService authUserService;


    @Override
    public void Create(Stages stages) {
            stages.setCreatedBy(authUserService.getLoggedInUser());
            stages.setStatus(true);
            stagesRepository.save(stages);
    }

    @Override
    public Stages findById(Long Id) {
        return null;
    }

    @Override
    public List<Stages> findAll() {
        return stagesRepository.findAll();
    }

    public List<Stages> getStages() {
        return stagesRepository.findByTypesTrue();
    }


    @Override
    public Optional<Stages> findOne(Long Id) {
        return stagesRepository.findByIdAndTypesTrue(Id);
    }

    @Override
    public List<Stages> findIssueType() {
        return stagesRepository.findByTypesTrue();
    }
}
