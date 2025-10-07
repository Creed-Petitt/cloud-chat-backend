package com.creedpetitt.aiservicesbackend.controllers;

import com.creedpetitt.aiservicesbackend.models.AppUser;
import com.creedpetitt.aiservicesbackend.repositories.UserRepository;
import com.creedpetitt.aiservicesbackend.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public abstract class BaseController {

    protected final UserService userService;
    protected final UserRepository userRepository;

    protected BaseController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    protected AppUser getAuthenticatedUser(Authentication authentication) {
        // Get the detached user from authentication
        AppUser detachedUser = (AppUser) authentication.getPrincipal();
        // Fetch fresh, managed entity from current Hibernate session
        return userRepository.findById(detachedUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    protected AppUser requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return getAuthenticatedUser(authentication);
    }
}
