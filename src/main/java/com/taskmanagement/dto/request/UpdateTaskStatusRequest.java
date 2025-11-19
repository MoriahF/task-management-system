package com.taskmanagement.dto.request;

import com.taskmanagement.model.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTaskStatusRequest {

    @NotNull(message = "Task status is required")
    private TaskStatus status;
}