package com.prakalpa.api.controllers;


import com.prakalpa.api.models.Categories;
import com.prakalpa.api.models.ProjectTypes;
import com.prakalpa.api.services.CategoriesServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/categories")
public class CategoriesController {

    @Autowired
    private CategoriesServices categoriesServices;

    @PostMapping(path = "/create")
    public ResponseEntity<?> create(@RequestBody Categories categories){
        categoriesServices.create(categories);
        return ResponseEntity.ok("Registered successfully!");
    }

    @GetMapping(path = "/find")
    public  ResponseEntity<?> find(){
        List<Categories> categories = categoriesServices.findAll();
        return ResponseEntity.ok(categories);
    }

}
