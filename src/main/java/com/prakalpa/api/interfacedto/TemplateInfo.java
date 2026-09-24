package com.prakalpa.api.interfacedto;

import com.prakalpa.api.models.Template;

import java.util.List;
import java.util.Optional;

public interface TemplateInfo {
    void create(Template template);
    List<Template> findAll();
    Optional<Template> findOne(Long id);



}
