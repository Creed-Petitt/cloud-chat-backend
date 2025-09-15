package com.creedpetitt.aiservicesbackend.services;

import com.creedpetitt.aiservicesbackend.models.AppUser;
import com.creedpetitt.aiservicesbackend.repositories.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser getOrCreateUser(String uid, String email) {
        return userRepository.findByFirebaseUid(uid)
                .orElseGet(() -> {
                    AppUser newUser = new AppUser();
                    newUser.setFirebaseUid(uid);
                    newUser.setEmail(email);
                    newUser.setMessageCount(0);
                    return userRepository.save(newUser);
                });
    }

    public void incrementMessageCount(AppUser user) {
        user.incrementMessageCount();
        userRepository.save(user);
    }

    public int getCurrentMessageCount(AppUser user) {
        return user.getMessageCount();
    }
}