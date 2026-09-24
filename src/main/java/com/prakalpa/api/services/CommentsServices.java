package com.prakalpa.api.services;

import com.prakalpa.api.interfacedto.CommentsInfo;
import com.prakalpa.api.models.Comments;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommentsServices implements CommentsInfo {
    @Override
    public void create(Comments comments) {

    }

    @Override
    public List<Comments> findAll() {
        return List.of();
    }

    @Override
    public Optional<Comments> findOne(Long Id) {
        return Optional.empty();
    }

    @Override
    public Comments findById(Long Id) {
        return null;
    }
}
