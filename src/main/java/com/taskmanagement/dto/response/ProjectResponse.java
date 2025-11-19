package com.taskmanagement.dto.response;

import com.taskmanagement.model.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;
    private int taskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse fromEntity(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwner().getId())
                .ownerName(project.getOwner().getName())
                .ownerEmail(project.getOwner().getEmail())
                .taskCount(project.getTaskCount())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}