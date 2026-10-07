package com.ipt.siga_backend.repository;

import com.ipt.siga_backend.entity.StudyPlan;
import com.ipt.siga_backend.entity.StudyPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

    // List all plans of a career
    List<StudyPlan> findByCareerId(Long careerId);

    // RN-2.6: identifier must be unique within the career
    boolean existsByCareerIdAndIdentifier(Long careerId, String identifier);

    // RN-2.10 / RN-1.1: get the CURRENT plan of a career
    Optional<StudyPlan> findByCareerIdAndStatus(Long careerId, StudyPlanStatus status);

    // RN-2.9: does the career have any plans?
    boolean existsByCareerId(Long careerId);
}