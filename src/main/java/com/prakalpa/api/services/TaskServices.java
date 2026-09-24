package com.prakalpa.api.services;

import com.prakalpa.api.interfacedto.TasksInfo;
import com.prakalpa.api.models.*;
import com.prakalpa.api.repository.TaskReposigory;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public void create(Tasks tasks) {
        Optional<Stages> stages = stageServices.findOne(tasks.getStages().getId());
        if(stages.isPresent()) tasks.setStages(stages.get());
        Optional<Priority> priority = priorityServices.findOne(tasks.getPriority().getId());
        if(priority.isPresent()) tasks.setPriority(priority.get());
        Optional<Permissions> permissions = permissionServices.findOne(tasks.getPermissions().getId());
        if(permissions.isPresent()) tasks.setPermissions(permissions.get());
        Optional<Projects> projects = projectsServices.findOne(tasks.getProjects().getId());
        if(projects.isPresent()) tasks.setProjects(projects.get());
        Optional<Users> assignTo = userServices.findOne(tasks.getAssignTo().getId());
        if(assignTo.isPresent()) tasks.setAssignTo(assignTo.get());
        tasks.setCreatedBy(authUserService.getLoggedInUser());

        taskReposigory.save(tasks);
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
