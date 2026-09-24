package com.prakalpa.api.controllers;

import com.prakalpa.api.models.Priority;
import com.prakalpa.api.services.PriorityServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/priority")
public class PriorityController {
    @Autowired
    private PriorityServices priorityServices;

    @PostMapping(path = "/create")
    public ResponseEntity<?> create(@RequestBody Priority priority){
        priorityServices.create(priority);
        return ResponseEntity.ok("Registered successfully!");
    }
    @GetMapping(path = "/find")
    public ResponseEntity<?> find(){
        List<Priority> stages = priorityServices.findAll();
        return ResponseEntity.ok(stages);
    }
}
