package com.prakalpa.api.controllers;

import com.prakalpa.api.models.Roles;
import com.prakalpa.api.services.RoleServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path="/api/v1")
public class RoleController {
    @Autowired
    private RoleServices roleServices;

    @PostMapping(path = "/admin/role")
    public void create(@RequestBody Roles roles){
        roleServices.Create(roles);
    }

    @GetMapping(path = "/admin/role")
    public List<Roles> getRole(){
        return roleServices.roles();
    }

    @GetMapping(path = "/admin/roles/{id}")
    public Roles findId(@PathVariable("id") Long id){
        return  roleServices.findById(id);
    }
}
