package com.prakalpa.api.interfacedto;

import com.prakalpa.api.models.Comments;

import java.util.List;
import java.util.Optional;

public interface CommentsInfo {
    void create(Comments comments);

    List<Comments> findAll();

    Optional<Comments> findOne(Long Id);

    Comments findById(Long Id);
}
