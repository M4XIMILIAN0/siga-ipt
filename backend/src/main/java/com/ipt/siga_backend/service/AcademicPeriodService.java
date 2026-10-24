package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.AcademicPeriodRequest;
import com.ipt.siga_backend.entity.AcademicPeriod;
import com.ipt.siga_backend.entity.PeriodStatus;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.AcademicPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AcademicPeriodService {

    private static final int FIRST_YEAR = 2000;   // the institute started in 2000 (section 4.1)

    private final AcademicPeriodRepository periodRepository;

    public AcademicPeriodService(AcademicPeriodRepository periodRepository) {
        this.periodRepository = periodRepository;
    }

    @Transactional
    public AcademicPeriod createPeriod(AcademicPeriodRequest request) {
        validateYearAndSemester(request);                               // RN-5.1 -> 400
        validatePeriodIsUnique(request.getYear(), request.getSemester()); // RN-5.1 -> 409

        AcademicPeriod period = new AcademicPeriod();
        period.setYear(request.getYear());
        period.setSemester(request.getSemester());
        period.setStatus(PeriodStatus.OPEN);                            // RN-5.2: always OPEN at creation
        return periodRepository.save(period);
    }

    public List<AcademicPeriod> getAllPeriods() {
        return periodRepository.findAll();
    }

    public AcademicPeriod getPeriodById(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Academic period " + id + " not found"));
    }

    // RN-5.1: identified by year and semester (1 or 2), like 2027-1
    private void validateYearAndSemester(AcademicPeriodRequest request) {
        if (request.getYear() == null || request.getSemester() == null) {
            throw new BadRequestException("Year and semester are required");
        }
        if (request.getYear() < FIRST_YEAR) {
            throw new BadRequestException("Year must be " + FIRST_YEAR + " or later");
        }
        if (request.getSemester() != 1 && request.getSemester() != 2) {
            throw new BadRequestException("Semester must be 1 or 2");
        }
    }

    // RN-5.1: the same period cannot be created twice
    private void validatePeriodIsUnique(Integer year, Integer semester) {
        if (periodRepository.existsByYearAndSemester(year, semester)) {
            throw new ConflictException("Academic period " + year + "-" + semester + " already exists");
        }
    }
}