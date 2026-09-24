package com.prakalpa.api.services;

import com.prakalpa.api.interfacedto.TemplateInfo;
import com.prakalpa.api.models.Template;
import com.prakalpa.api.repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TemplateServices implements TemplateInfo {

    @Autowired
    private TemplateRepository templateRepository;



    @Override
    public void create(Template template) {
            templateRepository.save(template);
    }

    @Override
    public List<Template> findAll() {
        return templateRepository.findAll();
    }

    @Override
    public Optional<Template> findOne(Long id) {
        return templateRepository.findById(id);
    }
}
