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
import com.taskmanagement.model.enums.UserRole;
import com.taskmanagement.repository.TaskRepository;
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
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private Project testProject;
    private Task testTask;
    private TaskRequest taskRequest;

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
                .id(13L)
                .name("Test Project")
                .description("Test Description")
                .owner(testUser)
                .tasks(new ArrayList<>())
                .build();

        testTask = Task.builder()
                .id(14L)
                .title("Test Task")
                .description("Test Task Description")
                .status(TaskStatus.TODO)
                .project(testProject)
                .build();

        taskRequest = TaskRequest.builder()
                .title("New Task")
                .description("New Task Description")
                .status(TaskStatus.TODO)
                .build();
    }

    @Test
    @DisplayName("Should create task successfully")
    void shouldCreateTaskSuccessfully() {
        when(projectService.findProjectById(testProject.getId())).thenReturn(testProject);
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(taskRepository.existsByTitleAndProjectId("New Task", testProject.getId())).thenReturn(false);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        TaskResponse response = taskService.createTask(testProject.getId(), taskRequest);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo(testTask.getTitle());
        assertThat(response.getProjectId()).isEqualTo(testProject.getId());

        verify(projectService).findProjectById(testProject.getId());
        verify(userService).getOrCreateCurrentUser();
        verify(taskRepository).existsByTitleAndProjectId("New Task", testProject.getId());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw exception when task title already exists in project")
    void shouldThrowExceptionWhenTaskTitleExists() {
        when(projectService.findProjectById(testProject.getId())).thenReturn(testProject);
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(taskRepository.existsByTitleAndProjectId("New Task", testProject.getId())).thenReturn(true);

        assertThatThrownBy(() -> taskService.createTask(testProject.getId(), taskRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists in this project");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw exception when creating task in non-existent project")
    void shouldThrowExceptionWhenCreatingTaskInNonExistentProject() {
        Long nonExistentProjectId = 999L;
        when(projectService.findProjectById(nonExistentProjectId))
                .thenThrow(new ResourceNotFoundException("Project not found"));

        assertThatThrownBy(() -> taskService.createTask(nonExistentProjectId, taskRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");

        verify(projectService).findProjectById(nonExistentProjectId);
        verifyNoInteractions(taskRepository);
    }

    @Test
    @DisplayName("Should throw exception when creating task in project not owned by user")
    void shouldThrowExceptionWhenCreatingTaskInProjectNotOwnedByUser() {
        User differentUser = User.builder()
                .id(2L)
                .cognitoSub("different-sub")
                .email("different@example.com")
                .role(UserRole.USER)
                .build();

        when(projectService.findProjectById(testProject.getId())).thenReturn(testProject);
        when(userService.getOrCreateCurrentUser()).thenReturn(differentUser);

        assertThatThrownBy(() -> taskService.createTask(testProject.getId(), taskRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("own");

        verify(projectService).findProjectById(testProject.getId());
        verify(userService).getOrCreateCurrentUser();
        verifyNoInteractions(taskRepository);
    }

    @Test
    @DisplayName("Should get task by ID successfully")
    void shouldGetTaskByIdSuccessfully() {
        when(projectService.findProjectById(testProject.getId())).thenReturn(testProject);
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByIdAndProjectId(testTask.getId(), testProject.getId()))
                .thenReturn(Optional.of(testTask));

        TaskResponse response = taskService.getTaskById(testProject.getId(), testTask.getId());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testTask.getId());
        assertThat(response.getTitle()).isEqualTo(testTask.getTitle());

        verify(projectService).findProjectById(testProject.getId());
        verify(userService).getOrCreateCurrentUser();
        verify(taskRepository).findByIdAndProjectId(testTask.getId(), testProject.getId());
    }

    @Test
    @DisplayName("Should get tasks by project")
    void shouldGetTasksByProject() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> taskPage = new PageImpl<>(Collections.singletonList(testTask), pageable, 1);

        when(projectService.findProjectById(testProject.getId())).thenReturn(testProject);
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByProjectId(testProject.getId(), pageable)).thenReturn(taskPage);

        PageResponse<TaskResponse> response = taskService.getTasksByProject(testProject.getId(), pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);

        verify(projectService).findProjectById(testProject.getId());
        verify(userService).getOrCreateCurrentUser();
        verify(taskRepository).findByProjectId(testProject.getId(), pageable);
    }

    @Test
    @DisplayName("Should get tasks by project and status")
    void shouldGetTasksByProjectAndStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> taskPage = new PageImpl<>(Collections.singletonList(testTask), pageable, 1);

        when(projectService.findProjectById(testProject.getId())).thenReturn(testProject);
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByProjectIdAndStatus(testProject.getId(), TaskStatus.TODO, pageable))
                .thenReturn(taskPage);

        PageResponse<TaskResponse> response = taskService.getTasksByProjectAndStatus(
                testProject.getId(), TaskStatus.TODO, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);

        verify(taskRepository).findByProjectIdAndStatus(testProject.getId(), TaskStatus.TODO, pageable);
    }

    @Test
    @DisplayName("Should get current user tasks")
    void shouldGetCurrentUserTasks() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> taskPage = new PageImpl<>(Collections.singletonList(testTask), pageable, 1);

        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByProjectOwnerId(testUser.getId(), pageable)).thenReturn(taskPage);

        PageResponse<TaskResponse> response = taskService.getCurrentUserTasks(pageable, null);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);

        verify(userService).getOrCreateCurrentUser();
        verify(taskRepository).findByProjectOwnerId(testUser.getId(), pageable);
    }

    @Test
    @DisplayName("Should get current user tasks with status filter")
    void shouldGetCurrentUserTasksWithStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> taskPage = new PageImpl<>(Collections.singletonList(testTask), pageable, 1);

        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByProjectOwnerIdAndStatus(testUser.getId(), TaskStatus.TODO, pageable))
                .thenReturn(taskPage);

        PageResponse<TaskResponse> response = taskService.getCurrentUserTasks(pageable, TaskStatus.TODO);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);

        verify(taskRepository).findByProjectOwnerIdAndStatus(testUser.getId(), TaskStatus.TODO, pageable);
    }

    @Test
    @DisplayName("Should get user tasks by ID for admin")
    void shouldGetUserTasksByIdForAdmin() {
        User adminUser = User.builder()
                .id(99L)
                .role(UserRole.ADMIN)
                .build();

        Page<Task> taskPage = new PageImpl<>(Collections.singletonList(testTask));

        when(userService.getOrCreateCurrentUser()).thenReturn(adminUser);
        when(taskRepository.findByProjectOwnerId(eq(testUser.getId()), any(Pageable.class)))
                .thenReturn(taskPage);

        PageResponse<TaskResponse> response = taskService.getUserTasksById(testUser.getId(), 0, 20, null);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);

        verify(userService).getOrCreateCurrentUser();
        verify(taskRepository).findByProjectOwnerId(eq(testUser.getId()), any(Pageable.class));
    }

    @Test
    @DisplayName("Should throw exception when non-admin tries to get other user tasks")
    void shouldThrowExceptionWhenNonAdminGetsOtherUserTasks() {
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);

        assertThatThrownBy(() -> taskService.getUserTasksById(999L, 0, 20, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("admin");

        verify(userService).getOrCreateCurrentUser();
        verifyNoInteractions(taskRepository);
    }

    @Test
    @DisplayName("Should update task successfully")
    void shouldUpdateTaskSuccessfully() {
        TaskRequest updateRequest = TaskRequest.builder()
                .title("Updated Task")
                .description("Updated Description")
                .status(TaskStatus.IN_PROGRESS)
                .build();

        when(projectService.findProjectById(testProject.getId())).thenReturn(testProject);
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByIdAndProjectId(testTask.getId(), testProject.getId()))
                .thenReturn(Optional.of(testTask));
        when(taskRepository.existsByTitleAndProjectId("Updated Task", testProject.getId())).thenReturn(false);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        TaskResponse response = taskService.updateTask(testProject.getId(), testTask.getId(), updateRequest);

        assertThat(response).isNotNull();
        verify(projectService).findProjectById(testProject.getId());
        verify(userService).getOrCreateCurrentUser();
        verify(taskRepository).findByIdAndProjectId(testTask.getId(), testProject.getId());
        verify(taskRepository).existsByTitleAndProjectId("Updated Task", testProject.getId());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw exception when updating to duplicate title")
    void shouldThrowExceptionWhenUpdatingToDuplicateTitle() {
        TaskRequest updateRequest = TaskRequest.builder()
                .title("Duplicate Title")
                .description("Description")
                .status(TaskStatus.TODO)
                .build();

        when(projectService.findProjectById(testProject.getId())).thenReturn(testProject);
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByIdAndProjectId(testTask.getId(), testProject.getId()))
                .thenReturn(Optional.of(testTask));
        when(taskRepository.existsByTitleAndProjectId("Duplicate Title", testProject.getId())).thenReturn(true);

        assertThatThrownBy(() -> taskService.updateTask(testProject.getId(), testTask.getId(), updateRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists in this project");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should update task status successfully")
    void shouldUpdateTaskStatusSuccessfully() {
        UpdateTaskStatusRequest statusRequest = UpdateTaskStatusRequest.builder()
                .status(TaskStatus.DONE)
                .build();

        when(projectService.findProjectById(testProject.getId())).thenReturn(testProject);
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByIdAndProjectId(testTask.getId(), testProject.getId()))
                .thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        TaskResponse response = taskService.updateTaskStatus(testProject.getId(), testTask.getId(), statusRequest);

        assertThat(response).isNotNull();
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should delete task successfully")
    void shouldDeleteTaskSuccessfully() {
        when(projectService.findProjectById(testProject.getId())).thenReturn(testProject);
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByIdAndProjectId(testTask.getId(), testProject.getId()))
                .thenReturn(Optional.of(testTask));

        taskService.deleteTask(testProject.getId(), testTask.getId());

        verify(projectService).findProjectById(testProject.getId());
        verify(userService).getOrCreateCurrentUser();
        verify(taskRepository).findByIdAndProjectId(testTask.getId(), testProject.getId());
        verify(taskRepository).delete(testTask);
    }

    @Test
    @DisplayName("Should throw exception when task not found")
    void shouldThrowExceptionWhenTaskNotFound() {
        Long nonExistentTaskId = 999L;
        when(projectService.findProjectById(testProject.getId())).thenReturn(testProject);
        when(userService.getOrCreateCurrentUser()).thenReturn(testUser);
        when(taskRepository.findByIdAndProjectId(nonExistentTaskId, testProject.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(testProject.getId(), nonExistentTaskId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found");

        verify(projectService).findProjectById(testProject.getId());
        verify(userService).getOrCreateCurrentUser();
        verify(taskRepository).findByIdAndProjectId(nonExistentTaskId, testProject.getId());
    }
}