package com.creedpetitt.aiservicesbackend.services;

import com.creedpetitt.aiservicesbackend.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class FirebaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public FirebaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByFirebaseUid(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with UID: " + username));
    }
}
