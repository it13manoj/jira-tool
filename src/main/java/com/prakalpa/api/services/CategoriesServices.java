package com.prakalpa.api.services;

import com.prakalpa.api.interfacedto.CategoriesInfo;
import com.prakalpa.api.models.Categories;
import com.prakalpa.api.repository.CategoriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriesServices implements CategoriesInfo {

    @Autowired
    private CategoriesRepository categoriesRepository;

    @Autowired
    private AuthUserService authUserService;

    @Override
    public void create(Categories categories) {
        categories.setCreatedBy(authUserService.getLoggedInUser());
        categoriesRepository.save(categories);
    }

    @Override
    public List<Categories> findAll() {
        return categoriesRepository.findAll();
    }

    @Override
    public Optional<Categories> findOne(Long id) {
        return categoriesRepository.findById(id);
    }
}
