package com.taskmanagement.service;

import com.taskmanagement.dto.response.PageResponse;
import com.taskmanagement.dto.response.UserResponse;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.exception.UnauthorizedException;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.enums.UserRole;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        String cognitoSub = securityContextHelper.getCurrentUserCognitoSub();
        return userRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public User getOrCreateCurrentUser() {
        String cognitoSub = securityContextHelper.getCurrentUserCognitoSub();

        return userRepository.findByCognitoSub(cognitoSub)
                .orElseGet(() -> {
                    log.info("Creating new user with Cognito sub: {}", cognitoSub);

                    User newUser = User.builder()
                            .cognitoSub(cognitoSub)
                            .email(securityContextHelper.getCurrentUserEmail())
                            .name(securityContextHelper.getCurrentUserName())
                            .role(securityContextHelper.getCurrentUserRole() != null ?
                                    securityContextHelper.getCurrentUserRole() : UserRole.USER)
                            .build();

                    return userRepository.save(newUser);
                });
    }

    @Transactional
    public UserResponse getCurrentUserProfile() {
        log.debug("Fetching current user profile");
        User user = getOrCreateCurrentUser();
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse getUserProfileById(Long userId) {
        log.debug("Fetching user profile for user ID: {}", userId);

        User currentUser = getOrCreateCurrentUser();
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedException("Only admins can view other users' profiles");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return UserResponse.fromEntity(user);
    }

    @Transactional
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        log.debug("Fetching all users - page: {}, size: {}", page, size);

        User currentUser = getOrCreateCurrentUser();
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedException("Only admins can view all users");
        }

        PageRequest pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);

        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .content(userResponses)
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<User> findByCognitoSub(String cognitoSub) {
        return userRepository.findByCognitoSub(cognitoSub);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}