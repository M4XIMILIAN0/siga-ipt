package com.ipt.siga_backend.repository;


import com.ipt.siga_backend.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    boolean existsByRun(String run);
    boolean existsByEmail(String email);
    Optional<Teacher> findByEmail(String email);   // links the Keycloak user (JWT email) with our Teacher

}