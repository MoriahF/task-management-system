package com.taskmanagement.repository;

import com.taskmanagement.model.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Page<Project> findByOwnerId(Long ownerId, Pageable pageable);
    Optional<Project> findByIdAndOwnerId(Long id, Long ownerId);
    long countByOwnerId(Long ownerId);
    boolean existsByNameAndOwnerId(String name, Long ownerId);
    Optional<Project> findById(Long id);

    @Query("SELECT p FROM Project p WHERE p.owner.id = :ownerId AND " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Project> searchByName(@Param("ownerId") Long ownerId, @Param("searchTerm") String searchTerm, Pageable pageable);

    Page<Project> findByOwnerIdAndNameContainingIgnoreCase(Long id, String searchTerm, Pageable pageable);
}
