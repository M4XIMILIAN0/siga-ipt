package com.ipt.siga_backend.repository;

import com.ipt.siga_backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // Enrollments of one student in one period (RN-6.6, RN-6.7, RN-6.10)
    List<Enrollment> findByStudentIdAndSectionPeriodId(Long studentId, Long periodId);

    // All enrollments of a period (admin view, closing in Epic 7)
    List<Enrollment> findBySectionPeriodId(Long periodId);

    // Students enrolled in a section (teacher view in Epic 7)
    List<Enrollment> findBySectionId(Long sectionId);

    long countBySectionId(Long sectionId);            // RN-6.9: used seats
    boolean existsByStudentId(Long studentId);        // RN-1.6 / RN-1.8
    boolean existsBySectionId(Long sectionId);        // RN-5.12
}