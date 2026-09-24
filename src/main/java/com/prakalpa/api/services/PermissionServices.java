package com.prakalpa.api.services;

import com.prakalpa.api.interfacedto.PermissionInfo;
import com.prakalpa.api.models.Permissions;
import com.prakalpa.api.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PermissionServices implements PermissionInfo {
    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private AuthUserService authUserService;


    @Override
    public void create(Permissions permissions) {
            permissions.setCreatedBy(authUserService.getLoggedInUser());
            permissionRepository.save(permissions);
    }

    @Override
    public List<Permissions> findAll() {
        return permissionRepository.findAll();
    }

    @Override
    public Optional<Permissions> findOne(Long Id) {
        return Optional.empty();
    }

    @Override
    public Permissions findByid(Long Id) {
        return null;
    }
}
