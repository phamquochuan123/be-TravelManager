package com.example.travelManager.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.travelManager.domain.Permission;
import com.example.travelManager.exception.DuplicateResourceException;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.PermissionRepository;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public Permission getPermissionById(long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission không tồn tại"));
    }

    public Permission createPermission(Permission permission) {
        if (permissionRepository.existsByModuleAndApiPathAndMethod(
                permission.getModule(), permission.getApiPath(), permission.getMethod())) {
            throw new DuplicateResourceException("Permission đã tồn tại");
        }
        return permissionRepository.save(permission);
    }

    public Permission updatePermission(long id, Permission request) {
        Permission existing = getPermissionById(id);
        existing.setName(request.getName());
        existing.setApiPath(request.getApiPath());
        existing.setMethod(request.getMethod());
        existing.setModule(request.getModule());
        return permissionRepository.save(existing);
    }

    public void deletePermission(long id) {
        getPermissionById(id);
        permissionRepository.deleteById(id);
    }
}
