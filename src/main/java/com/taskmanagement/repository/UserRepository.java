package com.taskmanagement.repository;

import com.taskmanagement.model.entity.User;
import com.taskmanagement.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByCognitoSub(String cognitoSub);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}