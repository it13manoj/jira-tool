package com.prakalpa.api.controllers;


import com.prakalpa.api.models.Images;
import com.prakalpa.api.services.ImagesServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/images")
public class ImagesController {

    @Autowired
    private ImagesServices imagesServices;

    @PostMapping(path = "/create")
    public ResponseEntity<?> create(@RequestBody Images images) {
        imagesServices.create(images);
        return ResponseEntity.ok("Registered successfully!");
    }

    @GetMapping(path = "/find")
    public ResponseEntity<?> find() {
        List<Images> iamges = imagesServices.findAll();
        return ResponseEntity.ok(iamges);
    }
}

