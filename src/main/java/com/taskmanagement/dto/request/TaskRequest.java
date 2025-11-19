package com.taskmanagement.dto.request;

import com.taskmanagement.model.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequest {

    @NotBlank(message = "Task title is required")
    @Size(min = 3, max = 255, message = "Task title must be between 3 and 255 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @NotNull(message = "Task status is required")
    private TaskStatus status;
}