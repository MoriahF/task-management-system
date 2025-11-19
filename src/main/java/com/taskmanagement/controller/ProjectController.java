package com.taskmanagement.controller;

import com.taskmanagement.dto.request.ProjectRequest;
import com.taskmanagement.dto.response.PageResponse;
import com.taskmanagement.dto.response.ProjectResponse;
import com.taskmanagement.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Projects", description = "Project management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new project", description = "Creates a new project for the authenticated user")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        log.info("Creating new project: {}", request.getName());
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all projects", description = "Returns all projects for the authenticated user with pagination")
    public ResponseEntity<PageResponse<ProjectResponse>> getAllProjects(@Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
                                                                        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
                                                                        @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
                                                                        @Parameter(description = "Sort direction (ASC or DESC)") @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        log.info("Fetching projects - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        PageResponse<ProjectResponse> response = projectService.getCurrentUserProjects(pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get project by ID", description = "Returns a specific project by ID if owned by the user")
    public ResponseEntity<ProjectResponse> getProjectById(@Parameter(description = "Project ID") @PathVariable Long projectId) {

        log.info("Fetching project with ID: {}", projectId);
        ProjectResponse response = projectService.getProjectById(projectId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Update project", description = "Updates an existing project")
    public ResponseEntity<ProjectResponse> updateProject(@Parameter(description = "Project ID") @PathVariable Long projectId, @Valid @RequestBody ProjectRequest request) {

        log.info("Updating project with ID: {}", projectId);
        ProjectResponse response = projectService.updateProject(projectId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Delete project", description = "Deletes a project and all its tasks")
    public ResponseEntity<Void> deleteProject(@Parameter(description = "Project ID") @PathVariable Long projectId) {

        log.info("Deleting project with ID: {}", projectId);
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search projects", description = "Search projects by name for the authenticated user")
    public ResponseEntity<PageResponse<ProjectResponse>> searchProjects(@Parameter(description = "Search term") @RequestParam String searchTerm,
                                                                        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
                                                                        @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        log.info("Searching projects with term: {}", searchTerm);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<ProjectResponse> response = projectService.searchProjects(searchTerm, pageable);

        return ResponseEntity.ok(response);
    }
}
