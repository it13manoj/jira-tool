package com.prakalpa.api.interfacedto;

import com.prakalpa.api.models.Images;

import java.util.List;
import java.util.Optional;


public interface ImagesInfo {
    void create(Images iamges);

    List<Images> findAll();

    Optional<Images> findOne(Long Id);

    Images findByid(Long Id);

}
