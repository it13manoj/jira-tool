package com.prakalpa.api.services;

import com.prakalpa.api.interfacedto.ProjectsInfo;
import com.prakalpa.api.models.*;
import com.prakalpa.api.repository.ProjectRepository;
import com.prakalpa.api.utils.CustomException;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectsServices implements ProjectsInfo {
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AuthUserService authUserService;

    @Autowired
    private CategoriesServices categoriesServices;

    @Autowired
    private ProjectTypeServices projectTypeServices;

    @Autowired
    private TemplateServices templateServices;

    @Autowired
    private UserServices userServices;


    @Override
    public void create(Projects projects) {
        Optional<Template> template = templateServices.findOne(projects.getTemplate().getId());
        if(template.isPresent()){
            projects.setTemplate(template.get());
        }else{
            throw new CustomException("Template not found");
        }
        Optional<ProjectTypes> projectTypes = projectTypeServices.findOne(projects.getProjectTypes().getId());
        if(projectTypes.isPresent()){
            projects.setProjectTypes(projectTypes.get());
        }else{
            throw new CustomException("Project Type not found");
        }

        Optional<Categories> categories = categoriesServices.findOne(projects.getCategories().getId());
        if(categories.isPresent()){
            projects.setCategories(categories.get());
        }else{
            throw new CustomException("Categories not found");
        }

        Optional<Users> users = userServices.findOne(projects.getLeads().getId());
        if(users.isPresent()){
            projects.setLeads(users.get());
        }else{
            throw new CustomException("Lead not found");
        }
        projects.setCreatedBy(authUserService.getLoggedInUser());
        projectRepository.save(projects);
    }

    @Override
    public List<Projects> findAll() {
        return projectRepository.findAll();
    }

    @Override
    public Optional<Projects> findOne(Long id) {
        return projectRepository.findById(id);
    }
}
