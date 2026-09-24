package com.prakalpa.api.controllers;

import com.prakalpa.api.models.Stages;
import com.prakalpa.api.services.StageServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/stage")
public class StageController {

    @Autowired
    private StageServices services;

    @PostMapping(path = "/create")
    public ResponseEntity<?> create(@RequestBody Stages stages){
        services.Create(stages);
        return ResponseEntity.ok("Registered successfully!");
    }
    @GetMapping(path = "/find")
    public ResponseEntity<?> find(){
        List<Stages> stages = services.findAll();
        return ResponseEntity.ok(stages);
    }

    @GetMapping(path = "/get-stages")
    public ResponseEntity<?> getStages(){
        List<Stages> stages = services.getStages();
        return ResponseEntity.ok(stages);
    }

    @GetMapping(path = "/issue-types")
    public ResponseEntity<?> issueType(){
        List<Stages> stages = services.findIssueType();
        return ResponseEntity.ok(stages);
    }
}
