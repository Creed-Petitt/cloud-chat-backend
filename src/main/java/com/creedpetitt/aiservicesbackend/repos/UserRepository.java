package com.creedpetitt.aiservicesbackend.repos;

import com.creedpetitt.aiservicesbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findById(String id);
}