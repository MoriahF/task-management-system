package com.taskmanagement.service;

import com.taskmanagement.dto.response.PageResponse;
import com.taskmanagement.dto.response.UserResponse;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.exception.UnauthorizedException;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.enums.UserRole;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.SecurityContextHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .cognitoSub("test-cognito-sub")
                .email("test@example.com")
                .name("Test User")
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("Should get existing user from database")
    void shouldGetExistingUser() {
        when(securityContextHelper.getCurrentUserCognitoSub()).thenReturn("test-cognito-sub");
        when(userRepository.findByCognitoSub("test-cognito-sub")).thenReturn(Optional.of(testUser));

        User result = userService.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUser.getId());
        assertThat(result.getEmail()).isEqualTo(testUser.getEmail());

        verify(securityContextHelper).getCurrentUserCognitoSub();
        verify(userRepository).findByCognitoSub("test-cognito-sub");
    }

    @Test
    @DisplayName("Should create new user if not exists")
    void shouldCreateNewUserIfNotExists() {
        when(securityContextHelper.getCurrentUserCognitoSub()).thenReturn("new-cognito-sub");
        when(securityContextHelper.getCurrentUserEmail()).thenReturn("new@example.com");
        when(securityContextHelper.getCurrentUserName()).thenReturn("New User");
        when(securityContextHelper.getCurrentUserRole()).thenReturn(UserRole.USER);
        when(userRepository.findByCognitoSub("new-cognito-sub")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.getOrCreateCurrentUser();

        assertThat(result).isNotNull();

        verify(securityContextHelper).getCurrentUserCognitoSub();
        verify(securityContextHelper).getCurrentUserEmail();
        verify(securityContextHelper).getCurrentUserName();
        verify(securityContextHelper, atLeastOnce()).getCurrentUserRole();
        verify(userRepository).findByCognitoSub("new-cognito-sub");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should get current user profile")
    void shouldGetCurrentUserProfile() {
        when(securityContextHelper.getCurrentUserCognitoSub()).thenReturn("test-cognito-sub");
        when(userRepository.findByCognitoSub("test-cognito-sub")).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getCurrentUserProfile();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testUser.getId());
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());

        verify(userRepository).findByCognitoSub("test-cognito-sub");
    }

    @Test
    @DisplayName("Should get user profile by ID for admin")
    void shouldGetUserProfileByIdForAdmin() {
        User adminUser = User.builder()
                .id(99L)
                .role(UserRole.ADMIN)
                .build();

        User targetUser = User.builder()
                .id(2L)
                .email("target@example.com")
                .name("Target User")
                .role(UserRole.USER)
                .build();

        when(securityContextHelper.getCurrentUserCognitoSub()).thenReturn("admin-sub");
        when(userRepository.findByCognitoSub("admin-sub")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        UserResponse response = userService.getUserProfileById(2L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);

        verify(userRepository).findById(2L);
    }

    @Test
    @DisplayName("Should throw exception when non-admin tries to get other user profile")
    void shouldThrowExceptionWhenNonAdminGetsOtherUserProfile() {
        when(securityContextHelper.getCurrentUserCognitoSub()).thenReturn("test-cognito-sub");
        when(userRepository.findByCognitoSub("test-cognito-sub")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.getUserProfileById(999L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("admin");

        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should get all users for admin")
    void shouldGetAllUsersForAdmin() {
        User adminUser = User.builder()
                .id(99L)
                .role(UserRole.ADMIN)
                .build();

        Page<User> userPage = new PageImpl<>(Collections.singletonList(testUser));

        when(securityContextHelper.getCurrentUserCognitoSub()).thenReturn("admin-sub");
        when(userRepository.findByCognitoSub("admin-sub")).thenReturn(Optional.of(adminUser));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(userPage);

        PageResponse<UserResponse> response = userService.getAllUsers(0, 20);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);

        verify(userRepository).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when non-admin tries to get all users")
    void shouldThrowExceptionWhenNonAdminGetsAllUsers() {
        when(securityContextHelper.getCurrentUserCognitoSub()).thenReturn("test-cognito-sub");
        when(userRepository.findByCognitoSub("test-cognito-sub")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> userService.getAllUsers(0, 20))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("admin");

        verify(userRepository, never()).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("Should find user by cognito sub")
    void shouldFindUserByCognitoSub() {
        when(userRepository.findByCognitoSub("test-cognito-sub")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByCognitoSub("test-cognito-sub");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(testUser.getId());

        verify(userRepository).findByCognitoSub("test-cognito-sub");
    }

    @Test
    @DisplayName("Should return empty when user not found by cognito sub")
    void shouldReturnEmptyWhenUserNotFoundByCognitoSub() {
        when(userRepository.findByCognitoSub("non-existent-sub")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByCognitoSub("non-existent-sub");

        assertThat(result).isEmpty();

        verify(userRepository).findByCognitoSub("non-existent-sub");
    }
}