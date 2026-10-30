package com.ipt.siga_backend.repository;

import com.ipt.siga_backend.entity.PeriodStatus;
import com.ipt.siga_backend.entity.Section;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {

    // RN-6.9: locks the section row (SELECT ... FOR UPDATE) so two replicas cannot take the last seat at once
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Section> findWithLockById(Long id);// academic offer
    List<Section> findByPeriodId(Long periodId);
    boolean existsBySubjectIdAndPeriodId(Long subjectId, Long periodId);           // RN-5.4
    boolean existsByTeacherId(Long teacherId);                                     // RN-4.6
    boolean existsByTeacherIdAndPeriodStatus(Long teacherId, PeriodStatus status); // RN-4.7
    boolean existsBySubjectId(Long subjectId);
    List<Section> findByTeacherIdAndPeriodId(Long teacherId, Long periodId);   // RN-P.2: teacher's sections
    // subject delete guard
}