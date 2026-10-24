package com.ipt.siga_backend.repository;

import com.ipt.siga_backend.entity.AcademicPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Long> {

    boolean existsByYearAndSemester(Integer year, Integer semester);   // RN-5.1
}