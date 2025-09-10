package com.creedpetitt.aiservicesbackend.repos;

import com.creedpetitt.aiservicesbackend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRespository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String role);
}
