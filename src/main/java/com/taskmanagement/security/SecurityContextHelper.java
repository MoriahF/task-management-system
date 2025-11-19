package com.taskmanagement.security;

import com.taskmanagement.model.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class SecurityContextHelper {

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Map) {
            return (Map<String, Object>) authentication.getPrincipal();
        }

        throw new IllegalStateException("No authenticated user found in security context");
    }

    public String getCurrentUserCognitoSub() {
        Map<String, Object> userDetails = getCurrentUserDetails();
        return (String) userDetails.get("cognitoSub");
    }

    public String getCurrentUserEmail() {
        Map<String, Object> userDetails = getCurrentUserDetails();
        return (String) userDetails.get("email");
    }

    public UserRole getCurrentUserRole() {
        Map<String, Object> userDetails = getCurrentUserDetails();
        return (UserRole) userDetails.get("role");
    }

    public String getCurrentUserName() {
        Map<String, Object> userDetails = getCurrentUserDetails();
        return (String) userDetails.get("name");
    }

    public boolean isCurrentUserAdmin() {
        try {
            return getCurrentUserRole() == UserRole.ADMIN;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof Map;
    }
}