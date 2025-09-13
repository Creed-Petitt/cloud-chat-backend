package com.creedpetitt.aiservicesbackend.services;

import com.creedpetitt.aiservicesbackend.models.User;
import com.creedpetitt.aiservicesbackend.repositories.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getOrCreateUser(String uid, String email) {
        return userRepository.findByFirebaseUid(uid)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setFirebaseUid(uid);
                    newUser.setEmail(email);
                    return userRepository.save(newUser);
                });
    }
}
