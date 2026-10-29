package com.ipt.siga_backend.repository;

import com.ipt.siga_backend.entity.PeriodRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeriodRegistrationRepository extends JpaRepository<PeriodRegistration, Long> {

    boolean existsByStudentIdAndPeriodId(Long studentId, Long periodId);   // RN-6.1
    List<PeriodRegistration> findByPeriodId(Long periodId);
}