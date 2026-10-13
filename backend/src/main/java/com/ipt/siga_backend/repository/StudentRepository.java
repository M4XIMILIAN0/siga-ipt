package com.ipt.siga_backend.repository;

import com.ipt.siga_backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByRun(String run);                     // RN-1.2
    boolean existsByEmail(String email);                 // RN-1.3
    boolean existsByStudyPlanId(Long studyPlanId);       // RN-2.8 (next block)
    boolean existsByStudyPlanCareerId(Long careerId);    // RN-2.9 (next block)
}