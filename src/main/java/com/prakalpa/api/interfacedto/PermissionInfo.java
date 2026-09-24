package com.prakalpa.api.interfacedto;

import com.prakalpa.api.models.Permissions;

import java.util.List;
import java.util.Optional;

public interface PermissionInfo {
    void create(Permissions permissions);

    List<Permissions> findAll();

    Optional<Permissions> findOne(Long Id);

    Permissions findByid(Long Id);

}
