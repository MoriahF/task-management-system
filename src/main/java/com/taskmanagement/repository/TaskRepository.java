package com.taskmanagement.repository;

import com.taskmanagement.model.entity.Task;
import com.taskmanagement.model.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByProjectId(Long projectId, Pageable pageable);
    Page<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status, Pageable pageable);
    Optional<Task> findByIdAndProjectId(Long id, Long projectId);
    long countByProjectId(Long projectId);
    boolean existsByTitleAndProjectId(String title, Long projectId);
    @Query("SELECT t FROM Task t WHERE t.project.owner = :ownerId AND t.status = :status")
    Page<Task> findByProjectOwnerIdAndStatus(
            @Param("ownerId") Long ownerId,
            @Param("status") TaskStatus status,
            Pageable pageable
    );
    @Query("SELECT t FROM Task t WHERE t.project.owner.id = :ownerId")
    Page<Task> findByProjectOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);


}
