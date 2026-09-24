package com.prakalpa.api.controllers;


import com.prakalpa.api.models.Stages;
import com.prakalpa.api.models.Tasks;
import com.prakalpa.api.services.TaskServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/tasks")
public class TaskController {

    @Autowired
    private TaskServices taskServices;

    @PostMapping(path = "/create")
    public ResponseEntity<?> create(@RequestBody Tasks tasks) {
        System.out.println("Hello Word");
        System.out.println(tasks);
        taskServices.create(tasks);
        return ResponseEntity.ok("Tasks successfully!");
    }

    @GetMapping(path = "/find")
    public ResponseEntity<?> find() {
        List<Tasks> tasks = taskServices.findAll();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping(path = "/assign-to")
    public ResponseEntity<?> assignTo() {
        List<Tasks> tasks = taskServices.AssignTask();
        return ResponseEntity.ok(tasks);
    }

    @PatchMapping(path = "/change-stage/{id}")
    public ResponseEntity<?> changeStage(@RequestBody Stages stage, @PathVariable("id") Long id) {
        taskServices.changeState(stage, id);
        return ResponseEntity.ok("Successfully Updated Stage!");
    }
}
