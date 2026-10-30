package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.GradeResponse;
import com.ipt.siga_backend.entity.AcademicPeriod;
import com.ipt.siga_backend.entity.Enrollment;
import com.ipt.siga_backend.entity.PeriodStatus;
import com.ipt.siga_backend.entity.Section;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.ForbiddenException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.EnrollmentRepository;
import com.ipt.siga_backend.repository.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GradeService {

    private static final double MIN_GRADE = 1.0;
    private static final double MAX_GRADE = 7.0;

    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;

    public GradeService(EnrollmentRepository enrollmentRepository, SectionRepository sectionRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.sectionRepository = sectionRepository;
    }

    // RN-P.2: the teacher sees the students enrolled in HIS section, with their grades
    @Transactional
    public List<GradeResponse> getSectionGrades(Long teacherId, Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Section " + sectionId + " not found"));
        validateTeacherIsResponsible(section, teacherId);                          // RN-7.1 -> 403
        return enrollmentRepository.findBySectionId(sectionId).stream()
                .map(enrollment -> toResponse(enrollment))
                .toList();
    }

    @Transactional
    public GradeResponse registerFinalGrade(Long teacherId, Long enrollmentId, Double finalGrade) {
        // RN-7.2: the grade belongs to an enrollment, so the student is always enrolled in that section
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment " + enrollmentId + " not found"));
        validateTeacherIsResponsible(enrollment.getSection(), teacherId);          // RN-7.1 -> 403
        validatePeriodIsOpen(enrollment.getSection().getPeriod());                 // RN-7.4 / RN-7.12 -> 409
        validateGradeRange(finalGrade);                                            // RN-7.3 -> 400

        // RN-7.3: one grade per enrollment; registering again replaces it (allowed while OPEN, RN-7.4)
        enrollment.setFinalGrade(finalGrade);
        return toResponse(enrollmentRepository.save(enrollment));
    }

    // RN-7.1: only the responsible teacher of the section can grade it
    private void validateTeacherIsResponsible(Section section, Long teacherId) {
        if (!section.getTeacher().getId().equals(teacherId)) {
            throw new ForbiddenException("Only the responsible teacher of " + section.getSubject().getCode()
                    + " can manage its grades");
        }
    }

    // RN-7.4 / RN-7.12: grades can only be registered or changed while the period is OPEN
    private void validatePeriodIsOpen(AcademicPeriod period) {
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new ConflictException("Period " + period.getCode() + " is CLOSED; grades cannot be changed");
        }
    }

    // RN-7.3: final grade between 1.0 and 7.0
    private void validateGradeRange(Double finalGrade) {
        if (finalGrade == null) {
            throw new BadRequestException("Final grade is required");
        }
        if (finalGrade < MIN_GRADE || finalGrade > MAX_GRADE) {
            throw new BadRequestException("Final grade must be between 1.0 and 7.0");
        }
    }

    private GradeResponse toResponse(Enrollment enrollment) {
        return new GradeResponse(
                enrollment.getId(),
                enrollment.getStudent().getRun(), enrollment.getStudent().getFullName(),
                enrollment.getSection().getSubject().getCode(),
                enrollment.getFinalGrade(), enrollment.getResult());   // RN-7.5: calculated
    }
}