package com.example.travelManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.travelManager.domain.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByModuleAndApiPathAndMethod(String module, String apiPath, String method);

    List<Permission> findByModule(String module);
}
