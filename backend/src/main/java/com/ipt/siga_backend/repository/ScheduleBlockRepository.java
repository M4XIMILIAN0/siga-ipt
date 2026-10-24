package com.ipt.siga_backend.repository;

import com.ipt.siga_backend.entity.ScheduleBlock;
import com.ipt.siga_backend.entity.ScheduleDay;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, Long> {

    // RN-5.11: does this teacher already have a block on this day+module in this period?
    boolean existsBySectionTeacherIdAndSectionPeriodIdAndDayAndModuleNumber(
            Long teacherId, Long periodId, ScheduleDay day, Integer moduleNumber);
}