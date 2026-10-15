package com.ipt.siga_backend.repository;


import com.ipt.siga_backend.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    boolean existsByRun(String run);
    boolean existsByEmail(String email);

}