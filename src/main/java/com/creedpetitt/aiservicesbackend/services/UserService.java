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

    public AppUser getOrCreateUser(String uid, String email, String provider) {
        return userRepository.findByFirebaseUid(uid)
                .orElseGet(() -> {
                    try {
                        AppUser newUser = new AppUser();
                        newUser.setFirebaseUid(uid);
                        newUser.setEmail(email != null ? email : uid + "@anonymous.user");
                        newUser.setGuest("anonymous".equals(provider));
                        newUser.setMessageCount(0);
                        newUser.setImageCount(0);
                        return userRepository.save(newUser);
                    } catch (Exception e) {
                        // Race condition: another thread created user, fetch it
                        return userRepository.findByFirebaseUid(uid)
                                .orElseThrow(() -> new RuntimeException("Failed to create or find user", e));
                    }
                });
    }

    public void incrementMessageCount(AppUser user) {
        user.incrementMessageCount();
        userRepository.save(user);
    }

    public int getCurrentMessageCount(AppUser user) {
        return user.getMessageCount();
    }

    public void incrementImageCount(AppUser user) {
        user.incrementImageCount();
        userRepository.save(user);
    }

    public int getCurrentImageCount(AppUser user) {
        return user.getImageCount();
    }
}