package com.taskmanagement.model.enums;

import lombok.Getter;

@Getter
public enum UserRole {

    ADMIN("ROLE_ADMIN"),
    USER("ROLE_USER");

    private final String authority;

    UserRole(String authority) {
        this.authority = authority;
    }

    public static UserRole fromString(String role) {
        if (role == null) {
            return USER;
        }
        String cleanRole = role.replace("ROLE_", "");

        try {
            return UserRole.valueOf(cleanRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}