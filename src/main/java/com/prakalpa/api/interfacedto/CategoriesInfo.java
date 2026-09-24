package com.prakalpa.api.interfacedto;

import com.prakalpa.api.models.Categories;

import java.util.List;
import java.util.Optional;

public interface CategoriesInfo {
    void create(Categories categories);
    List<Categories> findAll();
    Optional<Categories> findOne(Long id);

}
