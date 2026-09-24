package com.prakalpa.api.controllers;

import com.prakalpa.api.models.Assigns;
import com.prakalpa.api.services.AssignServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/assign")
public class AssignController {
    @Autowired
    private AssignServices assignServices;

    @PostMapping(path = "/create")
    public ResponseEntity<?> create(@RequestBody Assigns assigns) {
        assignServices.create(assigns);
        return ResponseEntity.ok("Registered successfully!");
    }

    @GetMapping(path = "/find")
    public ResponseEntity<?> find() {
        List<Assigns> assigns = assignServices.findAll();
        return ResponseEntity.ok(assigns);
    }
}
