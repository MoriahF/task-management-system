package com.taskmanagement.service;

import com.taskmanagement.dto.request.ProjectRequest;
import com.taskmanagement.dto.response.PageResponse;
import com.taskmanagement.dto.response.ProjectResponse;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.exception.UnauthorizedException;
import com.taskmanagement.model.entity.Project;
import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.enums.UserRole;
import com.taskmanagement.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Tests")
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProjectService projectService;

    private User testUser;
    private Project testProject;
    private ProjectRequest projectRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .cognitoSub("test-cognito-sub")
                .email("test@example.com")
                .name("Test User")
                .role(UserRole.USER)
                .build();

        testProject = Project.builder()
                .id(1L)
                .name("Test Project")
                .description("Test Description")
                .owner(testUser)
                .tasks(new ArrayList<>())
                .build();

        projectRequest = ProjectRequest.builder()
                .name("New Project")
                .description("New Description")
                .build();
    }

    @Test
    @DisplayName("Should create project successfully")
    void shouldCreateProjectSuccessfully() {
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        ProjectResponse response = projectService.createProject(projectRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(testProject.getName());
        assertThat(response.getOwnerId()).isEqualTo(testUser.getId());

        verify(userService).getOrCreateCurrentUser();
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("Should get current user projects with pagination")
    void shouldGetCurrentUserProjectsWithPagination() {
        List<Project> projects = Collections.singletonList(testProject);
        Page<Project> projectPage = new PageImpl<>(projects, PageRequest.of(0, 20), projects.size());
        Pageable pageable = PageRequest.of(0, 20);

        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(projectRepository.findByOwnerId(eq(testUser.getId()), any(Pageable.class))).thenReturn(projectPage);

        PageResponse<ProjectResponse> response = projectService.getCurrentUserProjects(pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo(testProject.getName());

        verify(userService).getOrCreateCurrentUser();
        verify(projectRepository).findByOwnerId(eq(testUser.getId()), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get user projects by ID for admin")
    void shouldGetUserProjectsByIdForAdmin() {
        User adminUser = User.builder()
                .id(99L)
                .role(UserRole.ADMIN)
                .build();

        Page<Project> projectPage = new PageImpl<>(Collections.singletonList(testProject));

        when(userService.getOrCreateCurrentUser()).thenReturn(adminUser);
        when(projectRepository.findByOwnerId(eq(testUser.getId()), any(Pageable.class))).thenReturn(projectPage);

        PageResponse<ProjectResponse> response = projectService.getUserProjectsById(testUser.getId(), 0, 20);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);

        verify(userService).getOrCreateCurrentUser();
        verify(projectRepository).findByOwnerId(eq(testUser.getId()), any(Pageable.class));
    }

    @Test
    @DisplayName("Should throw exception when non-admin tries to get other user projects")
    void shouldThrowExceptionWhenNonAdminGetsOtherUserProjects() {
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);

        assertThatThrownBy(() -> projectService.getUserProjectsById(999L, 0, 20))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("admin");

        verify(userService).getOrCreateCurrentUser();
        verifyNoInteractions(projectRepository);
    }

    @Test
    @DisplayName("Should search projects successfully")
    void shouldSearchProjectsSuccessfully() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Project> projectPage = new PageImpl<>(Collections.singletonList(testProject));

        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(projectRepository.findByOwnerIdAndNameContainingIgnoreCase(testUser.getId(), "Test", pageable))
                .thenReturn(projectPage);

        PageResponse<ProjectResponse> response = projectService.searchProjects("Test", pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);

        verify(userService).getOrCreateCurrentUser();
        verify(projectRepository).findByOwnerIdAndNameContainingIgnoreCase(testUser.getId(), "Test", pageable);
    }

    @Test
    @DisplayName("Should get project by ID when user is owner")
    void shouldGetProjectByIdWhenUserIsOwner() {
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);

        ProjectResponse response = projectService.getProjectById(testProject.getId());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testProject.getId());
        assertThat(response.getName()).isEqualTo(testProject.getName());

        verify(projectRepository).findById(testProject.getId());
        verify(userService).getOrCreateCurrentUser();
    }

    @Test
    @DisplayName("Should throw exception when project not found")
    void shouldThrowExceptionWhenProjectNotFound() {
        Long nonExistentId = 2L;
        when(projectRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");

        verify(projectRepository).findById(nonExistentId);
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Should throw exception when user doesn't own project")
    void shouldThrowExceptionWhenUserDoesntOwnProject() {
        User differentUser = User.builder()
                .id(2L)
                .cognitoSub("different-sub")
                .email("different@example.com")
                .role(UserRole.USER)
                .build();

        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(userService.getOrCreateCurrentUser()).thenReturn(differentUser);

        assertThatThrownBy(() -> projectService.getProjectById(testProject.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("access");

        verify(projectRepository).findById(testProject.getId());
        verify(userService).getOrCreateCurrentUser();
    }

    @Test
    @DisplayName("Should update project successfully")
    void shouldUpdateProjectSuccessfully() {
        ProjectRequest updateRequest = ProjectRequest.builder()
                .name("Updated Project")
                .description("Updated Description")
                .build();

        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        ProjectResponse response = projectService.updateProject(testProject.getId(), updateRequest);

        assertThat(response).isNotNull();
        verify(projectRepository).findById(testProject.getId());
        verify(userService).getOrCreateCurrentUser();
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("Should delete project successfully")
    void shouldDeleteProjectSuccessfully() {
        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);

        projectService.deleteProject(testProject.getId());

        verify(projectRepository).findById(testProject.getId());
        verify(userService).getOrCreateCurrentUser();
        verify(projectRepository).delete(testProject);
    }

    @Test
    @DisplayName("Admin should access any project")
    void adminShouldAccessAnyProject() {
        User adminUser = User.builder()
                .id(99L)
                .cognitoSub("admin-sub")
                .email("admin@example.com")
                .role(UserRole.ADMIN)
                .build();

        when(projectRepository.findById(testProject.getId())).thenReturn(Optional.of(testProject));
        when(userService.getOrCreateCurrentUser()).thenReturn(adminUser);

        ProjectResponse response = projectService.getProjectById(testProject.getId());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testProject.getId());

        verify(projectRepository).findById(testProject.getId());
        verify(userService).getOrCreateCurrentUser();
    }
}