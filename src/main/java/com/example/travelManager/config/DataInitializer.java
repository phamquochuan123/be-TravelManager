package com.example.travelManager.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.example.travelManager.domain.Role;
import com.example.travelManager.repository.RoleRepository;

@Component
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedRoles();
    }

    private void seedRoles() {
        createRoleIfNotExists("ADMIN", "Quản trị viên");
        createRoleIfNotExists("STAFF", "Nhân viên");
        createRoleIfNotExists("USER", "Người dùng thông thường");
    }

    private void createRoleIfNotExists(String name, String description) {
        if (!roleRepository.existsByName(name)) {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            role.setActive(true);
            roleRepository.save(role);
        }
    }
}
