package com.taskmanagement.service;

import com.taskmanagement.dto.request.ProjectRequest;
import com.taskmanagement.dto.response.PageResponse;
import com.taskmanagement.dto.response.ProjectResponse;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.exception.UnauthorizedException;
import com.taskmanagement.model.entity.Project;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.repository.ProjectRepository;
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
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        log.debug("Creating new project: {}", request.getName());

        User currentUser = userService.getOrCreateCurrentUser();

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(currentUser)
                .build();

        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully with ID: {}", savedProject.getId());

        return ProjectResponse.fromEntity(savedProject);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getCurrentUserProjects(Pageable pageable) {
        log.debug("Fetching projects for current user with pagination");

        User currentUser = userService.getOrCreateCurrentUser();
        Page<Project> projectPage = projectRepository.findByOwnerId(currentUser.getId(), pageable);

        List<ProjectResponse> projectResponses = projectPage.getContent().stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<ProjectResponse>builder()
                .content(projectResponses)
                .pageNumber(projectPage.getNumber())
                .pageSize(projectPage.getSize())
                .totalElements(projectPage.getTotalElements())
                .totalPages(projectPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getUserProjectsById(Long userId, int page, int size) {
        log.debug("Fetching projects for user: {}", userId);

        User currentUser = userService.getOrCreateCurrentUser();
        if (!currentUser.isAdmin()) {
            throw new UnauthorizedException("Only admins can view other users' projects");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Project> projectPage = projectRepository.findByOwnerId(userId, pageable);

        List<ProjectResponse> projectResponses = projectPage.getContent().stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<ProjectResponse>builder()
                .content(projectResponses)
                .pageNumber(projectPage.getNumber())
                .pageSize(projectPage.getSize())
                .totalElements(projectPage.getTotalElements())
                .totalPages(projectPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> searchProjects(String searchTerm, Pageable pageable) {
        log.debug("Searching projects with term: {}", searchTerm);

        User currentUser = userService.getOrCreateCurrentUser();
        Page<Project> projectPage = projectRepository.findByOwnerIdAndNameContainingIgnoreCase(
                currentUser.getId(), searchTerm, pageable);

        List<ProjectResponse> projectResponses = projectPage.getContent().stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.<ProjectResponse>builder()
                .content(projectResponses)
                .pageNumber(projectPage.getNumber())
                .pageSize(projectPage.getSize())
                .totalElements(projectPage.getTotalElements())
                .totalPages(projectPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        log.debug("Fetching project with ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));

        User currentUser = userService.getOrCreateCurrentUser();

        if (!project.isOwnedBy(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedException("You don't have access to this project");
        }

        return ProjectResponse.fromEntity(project);
    }

    @Transactional(readOnly = true)
    public Project findProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        log.debug("Updating project with ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));

        User currentUser = userService.getOrCreateCurrentUser();

        if (!project.isOwnedBy(currentUser.getId())) {
            throw new UnauthorizedException("You don't own this project");
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        Project updatedProject = projectRepository.save(project);
        log.info("Project updated successfully with ID: {}", updatedProject.getId());

        return ProjectResponse.fromEntity(updatedProject);
    }

    @Transactional
    public void deleteProject(Long id) {
        log.debug("Deleting project with ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));

        User currentUser = userService.getOrCreateCurrentUser();

        if (!project.isOwnedBy(currentUser.getId())) {
            throw new UnauthorizedException("You don't own this project");
        }

        projectRepository.delete(project);
        log.info("Project deleted successfully with ID: {}", id);
    }
}