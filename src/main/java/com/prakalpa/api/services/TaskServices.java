package com.prakalpa.api.services;

import com.prakalpa.api.interfacedto.TasksInfo;
import com.prakalpa.api.models.*;
import com.prakalpa.api.repository.TaskReposigory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class TaskServices implements TasksInfo {
    @Autowired
    private TaskReposigory taskReposigory;

    @Autowired
    private AuthUserService authUserService;

    @Autowired
    private StageServices stageServices;

    @Autowired
    private PriorityServices priorityServices;

    @Autowired
    private PermissionServices permissionServices;

    @Autowired
    private ProjectsServices projectsServices;

    @Autowired
    private UserServices userServices;

    @Override
    @Transactional
    public Tasks create(Tasks tasks) {
        if (tasks.getStages() != null && tasks.getStages().getId() != null) {
            stageServices.findOne(tasks.getStages().getId()).ifPresent(tasks::setStages);
        }
        if (tasks.getPriority() != null && tasks.getPriority().getId() != null) {
            priorityServices.findOne(tasks.getPriority().getId()).ifPresent(tasks::setPriority);
        }
        if (tasks.getPermissions() != null && tasks.getPermissions().getId() != null) {
            permissionServices.findOne(tasks.getPermissions().getId()).ifPresent(tasks::setPermissions);
        }
        if (tasks.getProjects() != null && tasks.getProjects().getId() != null) {
            projectsServices.findOne(tasks.getProjects().getId()).ifPresent(tasks::setProjects);
        }
        if (tasks.getAssignTo() != null && tasks.getAssignTo().getId() != null) {
            userServices.findOne(tasks.getAssignTo().getId()).ifPresent(tasks::setAssignTo);
        }
        tasks.setCreatedBy(authUserService.getLoggedInUser());

        return taskReposigory.save(tasks); // Return the saved Tasks object
    }

    @Override
    public List<Tasks> findAll() {
        return taskReposigory.findAll();
    }

    @Override
    public Optional<Tasks> findOne(Long Id) {
        return Optional.empty();
    }

    @Override
    public Tasks findById(Long Id) {
        return null;
    }

    public List<Tasks> AssignTask(){
        Users CreatedBy = authUserService.getLoggedInUser();
        Users assignTo = authUserService.getLoggedInUser();
        return taskReposigory.findByCreatedByOrAssignTo(CreatedBy, assignTo);
    }

    public void changeState(Stages Stage, Long Id){
        Optional<Stages> stages = stageServices.findOne(Stage.getId());
        Optional<Tasks> tasks = taskReposigory.findById(Id);
        if(tasks.isPresent()) {
            Tasks tasks1 = tasks.get();
            if(stages.isPresent()) tasks1.setStages(stages.get());
            taskReposigory.save(tasks1);
        }
    }
}
