package com.creedpetitt.aiservicesbackend.repositories;

import com.creedpetitt.aiservicesbackend.models.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByFirebaseUid(String firebaseUid);
}
