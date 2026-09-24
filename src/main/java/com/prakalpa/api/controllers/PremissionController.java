package com.prakalpa.api.controllers;


import com.prakalpa.api.models.Permissions;
import com.prakalpa.api.services.PermissionServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/permission")
public class PremissionController {
    @Autowired
    private PermissionServices permissionServices;

    @PostMapping(path = "/create")
    public ResponseEntity<?> create(@RequestBody Permissions permissions){
        permissionServices.create(permissions);
        return ResponseEntity.ok("Registered successfully!");
    }
    @GetMapping(path = "/find")
    public ResponseEntity<?> find(){
        List<Permissions> stages = permissionServices.findAll();
        return ResponseEntity.ok(stages);
    }
}
