package com.ipt.siga_backend.repository;

import com.ipt.siga_backend.entity.PeriodStatus;
import com.ipt.siga_backend.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByPeriodId(Long periodId);                                   // academic offer
    boolean existsBySubjectIdAndPeriodId(Long subjectId, Long periodId);           // RN-5.4
    boolean existsByTeacherId(Long teacherId);                                     // RN-4.6
    boolean existsByTeacherIdAndPeriodStatus(Long teacherId, PeriodStatus status); // RN-4.7
    boolean existsBySubjectId(Long subjectId);                                     // subject delete guard
}