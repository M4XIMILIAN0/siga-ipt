package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.PeriodRegistrationResponse;
import com.ipt.siga_backend.entity.AcademicPeriod;
import com.ipt.siga_backend.entity.PeriodRegistration;
import com.ipt.siga_backend.entity.PeriodStatus;
import com.ipt.siga_backend.entity.Student;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.AcademicPeriodRepository;
import com.ipt.siga_backend.repository.PeriodRegistrationRepository;
import com.ipt.siga_backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Period registration ("matrícula"): loaded by the admin, used by RN-6.1
@Service
public class PeriodRegistrationService {

    private final PeriodRegistrationRepository registrationRepository;
    private final AcademicPeriodRepository periodRepository;
    private final StudentRepository studentRepository;

    public PeriodRegistrationService(PeriodRegistrationRepository registrationRepository,
                                     AcademicPeriodRepository periodRepository,
                                     StudentRepository studentRepository) {
        this.registrationRepository = registrationRepository;
        this.periodRepository = periodRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public PeriodRegistrationResponse registerStudent(Long periodId, Long studentId) {
        if (studentId == null) {
            throw new BadRequestException("Student is required");
        }
        AcademicPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Academic period " + periodId + " not found"));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student " + studentId + " not found"));

        // Decision: registrations only in an OPEN period
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new ConflictException("Period " + period.getCode() + " is CLOSED");
        }
        if (registrationRepository.existsByStudentIdAndPeriodId(studentId, periodId)) {
            throw new ConflictException("Student is already registered in period " + period.getCode());
        }

        PeriodRegistration registration = new PeriodRegistration();
        registration.setStudent(student);
        registration.setPeriod(period);
        return toResponse(registrationRepository.save(registration));
    }

    @Transactional
    public List<PeriodRegistrationResponse> getRegistrationsByPeriod(Long periodId) {
        if (!periodRepository.existsById(periodId)) {
            throw new NotFoundException("Academic period " + periodId + " not found");
        }
        return registrationRepository.findByPeriodId(periodId).stream()
                .map(registration -> toResponse(registration))
                .toList();
    }

    private PeriodRegistrationResponse toResponse(PeriodRegistration registration) {
        return new PeriodRegistrationResponse(
                registration.getId(),
                registration.getPeriod().getId(), registration.getPeriod().getCode(),
                registration.getStudent().getId(), registration.getStudent().getRun(),
                registration.getStudent().getFullName());
    }
}