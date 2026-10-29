package com.ipt.siga_backend.repository;

import com.ipt.siga_backend.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    // List all subjects of a study plan
    List<Subject> findByStudyPlanId(Long studyPlanId);

    // RN-3.2: is this code already used in this plan?
    boolean existsByStudyPlanIdAndCode(Long studyPlanId, String code);

    // RN-3.8: find a prerequisite by code INSIDE the same plan
    Optional<Subject> findByStudyPlanIdAndCode(Long studyPlanId, String code);

    // Is this subject a prerequisite of ANY other subject? (JOIN with subject_prerequisites)
    boolean existsByPrerequisitesId(Long subjectId);

    boolean existsByStudyPlanId(Long studyPlanId);

    long countByStudyPlanId(Long studyPlanId);   // RN-6.7 / RN-7.10: total subjects of the plan
}