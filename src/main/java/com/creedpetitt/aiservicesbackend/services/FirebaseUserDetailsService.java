package com.creedpetitt.aiservicesbackend.services;

import com.creedpetitt.aiservicesbackend.models.AppUser;
import com.creedpetitt.aiservicesbackend.repositories.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class FirebaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public FirebaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = userRepository.findByFirebaseUid(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with UID: " + username));

        return new User(appUser.getEmail(), "", new ArrayList<>());
    }
}
