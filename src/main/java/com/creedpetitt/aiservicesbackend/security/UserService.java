package com.creedpetitt.aiservicesbackend.security;

import com.creedpetitt.aiservicesbackend.model.User;
import com.creedpetitt.aiservicesbackend.repos.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getOrCreateUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        String userId = jwt.getSubject();
        Optional<User> existingUser = userRepository.findById(userId);

        if (existingUser.isPresent()) {
            return existingUser.get();
        } else {
            User newUser = new User();
            newUser.setId(userId);
            newUser.setName(jwt.getClaimAsString("name"));
            newUser.setEmail(jwt.getClaimAsString("email"));
            return userRepository.save(newUser);
        }
    }
}