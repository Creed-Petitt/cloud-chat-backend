package com.creedpetitt.aiservicesbackend.services;

import com.creedpetitt.aiservicesbackend.models.AppUser;
import org.springframework.stereotype.Service;

@Service
public class RateLimitingService {

    private final int MAX_AUTHENTICATED_REQUESTS = 50;
    private final int MAX_IMAGES_PER_USER = 5;

    public boolean isUserAllowed(AppUser user) {
        if (user == null) return false;
        Integer messageCount = user.getMessageCount();
        return (messageCount == null ? 0 : messageCount) < MAX_AUTHENTICATED_REQUESTS;
    }

    public int getRemainingRequests(AppUser user) {
        if (user == null) return 0;
        Integer messageCount = user.getMessageCount();
        return Math.max(0, MAX_AUTHENTICATED_REQUESTS - (messageCount == null ? 0 : messageCount));
    }

    public boolean isUserImageAllowed(AppUser user) {
        if (user == null) return false;
        Integer imageCount = user.getImageCount();
        return (imageCount == null ? 0 : imageCount) < MAX_IMAGES_PER_USER;
    }

    public int getRemainingImages(AppUser user) {
        if (user == null) return 0;
        Integer imageCount = user.getImageCount();
        return Math.max(0, MAX_IMAGES_PER_USER - (imageCount == null ? 0 : imageCount));
    }
}
