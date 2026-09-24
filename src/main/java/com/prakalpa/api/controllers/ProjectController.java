package com.prakalpa.api.controllers;

import com.prakalpa.api.models.ProjectTypes;
import com.prakalpa.api.models.Projects;
import com.prakalpa.api.services.ProjectsServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/projects")
public class ProjectController {
    @Autowired
    private ProjectsServices projectsServices;

    @PostMapping(path = "/create")
    public ResponseEntity<?> create(@RequestBody Projects projects){
        projectsServices.create(projects);
        return ResponseEntity.ok("Registered successfully!");
    }

    @GetMapping(path = "/find")
    public  ResponseEntity<?> find(){
        List<Projects> projects = projectsServices.findAll();
        return ResponseEntity.ok(projects);
    }
}
