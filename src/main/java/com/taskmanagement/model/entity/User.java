package com.taskmanagement.model.entity;

import com.taskmanagement.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_cognito_sub", columnList = "cognito_sub"),
        @Index(name = "idx_email", columnList = "email")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cognito_sub", nullable = false, unique = true, length = 255)
    private String cognitoSub;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Project> projects = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    public void addProject(Project project) {
        projects.add(project);
        project.setOwner(this);
    }

    public void removeProject(Project project) {
        projects.remove(project);
        project.setOwner(null);
    }
}