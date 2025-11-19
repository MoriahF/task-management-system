package com.taskmanagement.controller;

import com.taskmanagement.dto.response.PageResponse;
import com.taskmanagement.dto.response.ProjectResponse;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.dto.response.UserResponse;
import com.taskmanagement.model.enums.TaskStatus;
import com.taskmanagement.service.ProjectService;
import com.taskmanagement.service.TaskService;
import com.taskmanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    private final UserService userService;
    private final ProjectService projectService;
    private final TaskService taskService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns the profile of the currently authenticated user")
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
        log.info("Fetching current user profile");
        UserResponse response = userService.getCurrentUserProfile();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/projects")
    @Operation(summary = "Get current user's projects", description = "Returns all projects owned by the authenticated user")
    public ResponseEntity<PageResponse<ProjectResponse>> getCurrentUserProjects(@RequestParam(defaultValue = "0") int page,
                                                                                @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching projects for current user - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ProjectResponse> response = projectService.getCurrentUserProjects(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/tasks")
    @Operation(summary = "Get current user's tasks", description = "Returns all tasks from projects owned by the authenticated user")
    public ResponseEntity<PageResponse<TaskResponse>> getCurrentUserTasks(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size,
                                                                          @RequestParam(required = false) TaskStatus status) {

        log.info("Fetching tasks for current user - page: {}, size: {}, status: {}", page, size, status);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<TaskResponse> response = taskService.getCurrentUserTasks(pageable, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (Admin only)", description = "Returns a paginated list of all users")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {

        log.info("Admin fetching all users - page: {}, size: {}", page, size);
        PageResponse<UserResponse> response = userService.getAllUsers(page, size);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID (Admin only)", description = "Returns user details by their ID")
    public ResponseEntity<UserResponse> getUserById(@Parameter(description = "User ID", required = true) @PathVariable Long userId) {
        log.info("Getting user by ID: {}", userId);
        UserResponse response = userService.getUserProfileById(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/projects")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user's projects (Admin only)", description = "Returns all projects owned by a specific user")
    public ResponseEntity<PageResponse<ProjectResponse>> getUserProjectsById(@Parameter(description = "User  ID", required = true)
                                                                             @PathVariable Long userId,
                                                                             @RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "20") int size) {

        log.info("Admin fetching projects for user {} - page: {}, size: {}", userId, page, size);
        PageResponse<ProjectResponse> response = projectService.getUserProjectsById(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/tasks")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user's tasks (Admin only)", description = "Returns all tasks from projects owned by a specific user")
    public ResponseEntity<PageResponse<TaskResponse>> getUserTasksById(@Parameter(description = "User ID", required = true)
                                                                       @PathVariable Long userId, @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size,
                                                                       @RequestParam(required = false) TaskStatus status) {

        log.info("Admin fetching tasks for user {} - page: {}, size: {}, status: {}", userId, page, size, status);
        PageResponse<TaskResponse> response = taskService.getUserTasksById(userId, page, size, status);
        return ResponseEntity.ok(response);
    }
}