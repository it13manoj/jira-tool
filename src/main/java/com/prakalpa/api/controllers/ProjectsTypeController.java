package com.prakalpa.api.controllers;


import com.prakalpa.api.models.ProjectTypes;
import com.prakalpa.api.services.ProjectTypeServices;
import com.prakalpa.api.services.ProjectsServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/project-type")
public class ProjectsTypeController {
    @Autowired
    private ProjectTypeServices projectTypeServices;

    @PostMapping(path = "/create")
    public ResponseEntity<?> create(@RequestBody ProjectTypes projectTypes){
        projectTypeServices.create(projectTypes);
        return ResponseEntity.ok("Registered successfully!");
    }

    @GetMapping(path = "/find")
    public  ResponseEntity<?> find(){
        List<ProjectTypes> projectTypes = projectTypeServices.findAll();
        return ResponseEntity.ok(projectTypes);
    }
}
