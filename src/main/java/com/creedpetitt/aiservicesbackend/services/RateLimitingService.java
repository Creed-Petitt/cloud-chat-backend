package com.creedpetitt.aiservicesbackend.services;

import com.creedpetitt.aiservicesbackend.models.AppUser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private final int MAX_REQUESTS = 10;

    private final Map<String, Integer> anonymousRequestCounts = new ConcurrentHashMap<>();

    public boolean isUserAllowed(AppUser user) {
        if (user == null) return false;
        Integer messageCount = user.getMessageCount();
        return (messageCount == null ? 0 : messageCount) < MAX_REQUESTS;
    }

    public boolean isAnonymousAllowed(String ipAddress) {
        int count = anonymousRequestCounts.getOrDefault(ipAddress, 0);
        return count < MAX_REQUESTS;
    }

    public void incrementAnonymousCount(String ipAddress) {
        int count = anonymousRequestCounts.getOrDefault(ipAddress, 0);
        anonymousRequestCounts.put(ipAddress, count + 1);
    }

    public int getRemainingRequests(AppUser user) {
        if (user == null) return 0;
        Integer messageCount = user.getMessageCount();
        return Math.max(0, MAX_REQUESTS - (messageCount == null ? 0 : messageCount));
    }

    public int getRemainingAnonymousRequests(String ipAddress) {
        int count = anonymousRequestCounts.getOrDefault(ipAddress, 0);
        return Math.max(0, MAX_REQUESTS - count);
    }
}
