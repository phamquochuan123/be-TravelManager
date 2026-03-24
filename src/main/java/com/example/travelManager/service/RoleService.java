package com.example.travelManager.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.travelManager.domain.Role;
import com.example.travelManager.exception.DuplicateResourceException;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.RoleRepository;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Role getRoleById(long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại"));
    }

    public Role createRole(Role role) {
        if (roleRepository.existsByName(role.getName())) {
            throw new DuplicateResourceException("Role đã tồn tại: " + role.getName());
        }
        return roleRepository.save(role);
    }

    public Role updateRole(long id, Role request) {
        Role existing = getRoleById(id);
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setActive(request.isActive());
        if (request.getPermissions() != null) {
            existing.setPermissions(request.getPermissions());
        }
        return roleRepository.save(existing);
    }

    public void deleteRole(long id) {
        getRoleById(id);
        roleRepository.deleteById(id);
    }
}
