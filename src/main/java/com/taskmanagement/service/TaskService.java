package com.taskmanagement.service;

import com.taskmanagement.dto.request.TaskRequest;
import com.taskmanagement.dto.request.UpdateTaskStatusRequest;
import com.taskmanagement.dto.response.PageResponse;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.exception.UnauthorizedException;
import com.taskmanagement.exception.ValidationException;
import com.taskmanagement.model.entity.Project;
import com.taskmanagement.model.entity.Task;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.enums.TaskStatus;
import com.taskmanagement.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final UserService userService;

    @Transactional
    public TaskResponse createTask(Long projectId, TaskRequest request) {
        log.debug("Creating task in project with ID: {}", projectId);

        Project project = projectService.findProjectById(projectId);
        User currentUser = userService.getOrCreateCurrentUser();

        if (!project.isOwnedBy(currentUser.getId())) {
            throw new UnauthorizedException("You don't own this project");
        }

        if (taskRepository.existsByTitleAndProjectId(request.getTitle(), projectId)) {
            throw new ValidationException("Task with title '" + request.getTitle() + "' already exists in this project");
        }

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .project(project)
                .build();

        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully with ID: {}", savedTask.getId());

        return TaskResponse.fromEntity(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long projectId, Long taskId) {
        log.debug("Fetching task with ID: {} from project: {}", taskId, projectId);

        Project project = projectService.findProjectById(projectId);
        User currentUser = userService.getOrCreateCurrentUser();

        if (!project.isOwnedBy(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedException("You don't have access to this project");
        }

        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        return TaskResponse.fromEntity(task);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getTasksByProject(Long projectId, Pageable pageable) {
        log.debug("Fetching all tasks for project: {}", projectId);

        Project project = projectService.findProjectById(projectId);
        User currentUser = userService.getOrCreateCurrentUser();

        if (!project.isOwnedBy(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedException("You don't have access to this project");
        }

        Page<Task> taskPage = taskRepository.findByProjectId(projectId, pageable);

        List<TaskResponse> taskResponses = taskPage.getContent().stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<TaskResponse>builder()
                .content(taskResponses)
                .pageNumber(taskPage.getNumber())
                .pageSize(taskPage.getSize())
                .totalElements(taskPage.getTotalElements())
                .totalPages(taskPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getTasksByProjectAndStatus(Long projectId, TaskStatus status, Pageable pageable) {
        log.debug("Fetching tasks for project: {} with status: {}", projectId, status);

        Project project = projectService.findProjectById(projectId);
        User currentUser = userService.getOrCreateCurrentUser();

        if (!project.isOwnedBy(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedException("You don't have access to this project");
        }

        Page<Task> taskPage = taskRepository.findByProjectIdAndStatus(projectId, status, pageable);

        List<TaskResponse> taskResponses = taskPage.getContent().stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<TaskResponse>builder()
                .content(taskResponses)
                .pageNumber(taskPage.getNumber())
                .pageSize(taskPage.getSize())
                .totalElements(taskPage.getTotalElements())
                .totalPages(taskPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getCurrentUserTasks(Pageable pageable, TaskStatus status) {
        log.debug("Fetching tasks for current user");

        User currentUser = userService.getOrCreateCurrentUser();

        Page<Task> taskPage;
        if (status != null) {
            taskPage = taskRepository.findByProjectOwnerIdAndStatus(currentUser.getId(), status, pageable);
        } else {
            taskPage = taskRepository.findByProjectOwnerId(currentUser.getId(), pageable);
        }

        List<TaskResponse> taskResponses = taskPage.getContent().stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<TaskResponse>builder()
                .content(taskResponses)
                .pageNumber(taskPage.getNumber())
                .pageSize(taskPage.getSize())
                .totalElements(taskPage.getTotalElements())
                .totalPages(taskPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getUserTasksById(Long userId, int page, int size, TaskStatus status) {
        log.debug("Fetching tasks for user: {}", userId);

        User currentUser = userService.getOrCreateCurrentUser();
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedException("Only admins can view other users' tasks");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Task> taskPage;
        if (status != null) {
            taskPage = taskRepository.findByProjectOwnerIdAndStatus(userId, status, pageable);
        } else {
            taskPage = taskRepository.findByProjectOwnerId(userId, pageable);
        }

        List<TaskResponse> taskResponses = taskPage.getContent().stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<TaskResponse>builder()
                .content(taskResponses)
                .pageNumber(taskPage.getNumber())
                .pageSize(taskPage.getSize())
                .totalElements(taskPage.getTotalElements())
                .totalPages(taskPage.getTotalPages())
                .build();
    }

    @Transactional
    public TaskResponse updateTask(Long projectId, Long taskId, TaskRequest request) {
        log.debug("Updating task with ID: {} in project: {}", taskId, projectId);

        Project project = projectService.findProjectById(projectId);
        User currentUser = userService.getOrCreateCurrentUser();

        if (!project.isOwnedBy(currentUser.getId())) {
            throw new UnauthorizedException("You don't own this project");
        }

        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        if (!task.getTitle().equals(request.getTitle()) &&
                taskRepository.existsByTitleAndProjectId(request.getTitle(), projectId)) {
            throw new ValidationException("Task with title '" + request.getTitle() + "' already exists in this project");
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());

        Task updatedTask = taskRepository.save(task);
        log.info("Task updated successfully with ID: {}", updatedTask.getId());

        return TaskResponse.fromEntity(updatedTask);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long projectId, Long taskId, UpdateTaskStatusRequest request) {
        log.debug("Updating task status for task: {} in project: {}", taskId, projectId);

        Project project = projectService.findProjectById(projectId);
        User currentUser = userService.getOrCreateCurrentUser();

        if (!project.isOwnedBy(currentUser.getId())) {
            throw new UnauthorizedException("You don't own this project");
        }

        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        task.setStatus(request.getStatus());

        Task updatedTask = taskRepository.save(task);
        log.info("Task status updated successfully for ID: {}", updatedTask.getId());

        return TaskResponse.fromEntity(updatedTask);
    }

    @Transactional
    public void deleteTask(Long projectId, Long taskId) {
        log.debug("Deleting task with ID: {} from project: {}", taskId, projectId);

        Project project = projectService.findProjectById(projectId);
        User currentUser = userService.getOrCreateCurrentUser();

        if (!project.isOwnedBy(currentUser.getId())) {
            throw new UnauthorizedException("You don't own this project");
        }

        Task task = taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + taskId));

        taskRepository.delete(task);
        log.info("Task deleted successfully with ID: {}", taskId);
    }
}