package com.prakalpa.api.controllers;

import com.prakalpa.api.models.Template;
import com.prakalpa.api.services.AuthUserService;
import com.prakalpa.api.services.TemplateServices;
import com.prakalpa.api.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class TemplateController {
    @Autowired
    private TemplateServices templateServices;

    @Autowired
    private AuthUserService authUserService;

    @PostMapping(path = "/template/create")
    public ResponseEntity<?> createTemplate(@RequestBody Template template){
        template.setCreatedBy(authUserService.getLoggedInUser());
        templateServices.create(template);
        return ResponseEntity.ok("Registered successfully!");
    }

    @GetMapping(path = "/template/find")
    public ResponseEntity<?> findAll(){
        List<Template> templateList = templateServices.findAll();
        return ResponseEntity.ok(templateList);
    }

}
