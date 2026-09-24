package com.prakalpa.api.services;

import com.prakalpa.api.interfacedto.ImagesInfo;
import com.prakalpa.api.models.Images;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ImagesServices implements ImagesInfo {
    @Override
    public void create(Images images) {

    }

    @Override
    public List<Images> findAll() {
        return List.of();
    }

    @Override
    public Optional<Images> findOne(Long Id) {
        return Optional.empty();
    }

    @Override
    public Images findByid(Long Id) {
        return null;
    }
}
