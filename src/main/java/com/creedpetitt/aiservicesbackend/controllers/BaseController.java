package com.creedpetitt.aiservicesbackend.controllers;

import com.creedpetitt.aiservicesbackend.models.AppUser;
import com.creedpetitt.aiservicesbackend.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public abstract class BaseController {

    protected final UserService userService;

    protected BaseController(UserService userService) {
        this.userService = userService;
    }

    protected AppUser getAuthenticatedUser(Authentication authentication) {
        String uid = authentication.getName();
        String email = (String) authentication.getDetails();
        return userService.getOrCreateUser(uid, email != null ? email : uid + "@firebase.user", "unknown");
    }

    protected AppUser requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return getAuthenticatedUser(authentication);
    }
}
