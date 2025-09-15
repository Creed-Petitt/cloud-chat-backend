package com.creedpetitt.aiservicesbackend.security;

import com.creedpetitt.aiservicesbackend.services.FirebaseUserDetailsService;
import com.creedpetitt.aiservicesbackend.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FirebaseFilter extends OncePerRequestFilter {

    private final FirebaseAuth firebaseAuth;
    private final UserService userService;
    private final FirebaseUserDetailsService userDetailsService;

    public FirebaseFilter(FirebaseAuth firebaseAuth, UserService userService, FirebaseUserDetailsService userDetailsService) {
        this.firebaseAuth = firebaseAuth;
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        
        // Check if token is empty or null after extracting
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);

            userService.getOrCreateUser(decodedToken.getUid(), decodedToken.getEmail());

            UserDetails userDetails = userDetailsService.loadUserByUsername(decodedToken.getUid());

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            logger.error("Firebase Authentication failed", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
