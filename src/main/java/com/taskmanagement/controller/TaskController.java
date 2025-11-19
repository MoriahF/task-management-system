package com.taskmanagement.controller;

import com.taskmanagement.dto.request.TaskRequest;
import com.taskmanagement.dto.request.UpdateTaskStatusRequest;
import com.taskmanagement.dto.response.PageResponse;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.model.enums.TaskStatus;
import com.taskmanagement.service.TaskService;
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
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tasks", description = "Task management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a new task", description = "Creates a new task in the specified project")
    public ResponseEntity<TaskResponse> createTask(@Parameter(description = "Project ID") @PathVariable Long projectId,
                                                   @Valid @RequestBody TaskRequest request) {
        log.info("Creating new task in project: {}", projectId);
        TaskResponse response = taskService.createTask(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all tasks in project", description = "Returns all tasks in the specified project with pagination")
    public ResponseEntity<PageResponse<TaskResponse>> getTasksByProject(@Parameter(description = "Project ID") @PathVariable Long projectId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)") @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @Parameter(description = "Filter by status (optional)") @RequestParam(required = false) TaskStatus status) {

        log.info("Fetching tasks for project: {} - page: {}, size: {}", projectId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        PageResponse<TaskResponse> response;

        if (status != null) {
            response = taskService.getTasksByProjectAndStatus(projectId, status, pageable);
        } else {
            response = taskService.getTasksByProject(projectId, pageable);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by ID", description = "Returns a specific task by ID within a project")
    public ResponseEntity<TaskResponse> getTaskById(@Parameter(description = "Project ID") @PathVariable Long projectId,
            @Parameter(description = "Task ID") @PathVariable Long taskId) {

        log.info("Fetching task {} in project {}", taskId, projectId);
        TaskResponse response = taskService.getTaskById(projectId, taskId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Update task", description = "Updates an existing task")
    public ResponseEntity<TaskResponse> updateTask(@Parameter(description = "Project ID") @PathVariable Long projectId,
           @Parameter(description = "Task ID") @PathVariable Long taskId, @Valid @RequestBody TaskRequest request) {

        log.info("Updating task {} in project {}", taskId, projectId);
        TaskResponse response = taskService.updateTask(projectId, taskId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Update task status", description = "Updates only the status of a task")
    public ResponseEntity<TaskResponse> updateTaskStatus(@Parameter(description = "Project ID") @PathVariable Long projectId,
                                                        @Parameter(description = "Task ID") @PathVariable Long taskId,
                                                        @Valid @RequestBody UpdateTaskStatusRequest request) {

        log.info("Updating task {} status to {}", taskId, request.getStatus());
        TaskResponse response = taskService.updateTaskStatus(projectId, taskId, request);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete task", description = "Deletes a task from a project")
    public ResponseEntity<Void> deleteTask(@Parameter(description = "Project ID") @PathVariable Long projectId,
                                           @Parameter(description = "Task ID") @PathVariable Long taskId) {

        log.info("Deleting task {} from project {}", taskId, projectId);
        taskService.deleteTask(projectId, taskId);
        return ResponseEntity.noContent().build();
    }
}