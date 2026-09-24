package com.prakalpa.api.services;

import com.prakalpa.api.repository.RoleRepository;
import com.prakalpa.api.interfacedto.RoleInfo;
import com.prakalpa.api.models.Roles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleServices implements RoleInfo {
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void Create(Roles roles) {
        roleRepository.save(roles);
    }

    @Override
    public Roles findById(Long Id) {
        return roleRepository.findById(Id).orElse(null);
    }

    @Override
    public List<Roles> roles() {
        return roleRepository.findAll();
    }
}
