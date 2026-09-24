package com.prakalpa.api.interfacedto;

import com.prakalpa.api.models.Roles;

import java.util.List;
import java.util.Optional;

public interface RoleInfo {
    void Create(Roles roles);

    Roles findById(Long Id);

    List<Roles> roles();


}
