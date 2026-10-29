package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.EnrollmentResponse;
import com.ipt.siga_backend.dto.OfferItemResponse;
import com.ipt.siga_backend.entity.*;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class EnrollmentService {

    private static final int MIN_SUBJECTS = 3;
    private static final int MAX_SUBJECTS = 6;

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final AcademicPeriodRepository periodRepository;
    private final SectionRepository sectionRepository;
    private final PeriodRegistrationRepository registrationRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final SubjectRepository subjectRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, StudentRepository studentRepository,
                             AcademicPeriodRepository periodRepository, SectionRepository sectionRepository,
                             PeriodRegistrationRepository registrationRepository,
                             AcademicRecordRepository academicRecordRepository,
                             SubjectRepository subjectRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.periodRepository = periodRepository;
        this.sectionRepository = sectionRepository;
        this.registrationRepository = registrationRepository;
        this.academicRecordRepository = academicRecordRepository;
        this.subjectRepository = subjectRepository;
    }

    // ==================== enroll ====================

    @Transactional
    public List<EnrollmentResponse> enroll(Long studentId, Long periodId, List<Long> sectionIds) {
        validateRequest(periodId, sectionIds);                                     // 400 / RN-6.6
        Student student = findStudentOrThrow(studentId);                           // 404
        AcademicPeriod period = findPeriodOrThrow(periodId);                       // 404
        validatePeriodIsOpen(period);                                              // RN-6.3 -> 409
        validateStudentIsRegular(student);                                         // RN-6.1 -> 409
        validateStudentIsRegistered(student, period);                              // RN-6.1 -> 409

        List<Enrollment> currentEnrollments =
                enrollmentRepository.findByStudentIdAndSectionPeriodId(studentId, periodId);

        // Checks each chosen section on its own
        List<Section> newSections = new ArrayList<>();
        for (Long sectionId : sectionIds) {
            Section section = findSectionWithLockOrThrow(sectionId);              // 404 + lock (RN-6.9)
            Subject subject = section.getSubject();
            validateSectionBelongsToPeriod(section, periodId);                     // RN-6.2 -> 400
            validateSubjectBelongsToStudentPlan(subject, student);                 // RN-6.2 -> 400
            validateNotAlreadyEnrolledInPeriod(subject, currentEnrollments);       // RN-6.6 -> 409
            validateSubjectNotPassed(studentId, subject);                          // RN-6.4 -> 409
            validatePrerequisitesPassed(studentId, subject);                       // RN-6.5 -> 409
            validateSectionHasSeats(section);                                      // RN-6.9 -> 409
            newSections.add(section);
        }

        // Checks the whole set: what the student already had + what is being added
        List<Section> allSections = new ArrayList<>();
        for (Enrollment enrollment : currentEnrollments) {
            allSections.add(enrollment.getSection());
        }
        allSections.addAll(newSections);

        validateSubjectCount(student, allSections.size());                        // RN-6.7 -> 400
        validateLowerSemestersFirst(student, periodId, newSections, allSections);  // RN-6.8 -> 409
        validateNoScheduleConflicts(allSections);                                  // RN-6.10 -> 409

        // RN-6.11: each enrollment is linked to student + section (+ subject and period through the section)
        List<EnrollmentResponse> result = new ArrayList<>();
        for (Section section : newSections) {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setSection(section);
            enrollment.setEnrollmentDate(LocalDate.now());
            result.add(toResponse(enrollmentRepository.save(enrollment)));
        }
        return result;
    }

    // ==================== cancel ====================

    @Transactional
    public void cancelEnrollment(Long studentId, Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment " + enrollmentId + " not found"));
        // A student can only cancel HIS OWN enrollments
        if (!enrollment.getStudent().getId().equals(studentId)) {
            throw new NotFoundException("Enrollment " + enrollmentId + " not found");
        }
        validatePeriodIsOpen(enrollment.getSection().getPeriod());                 // RN-6.3 -> 409

        // Decision: an enrollment that already has a final grade cannot be cancelled
        if (enrollment.getFinalGrade() != null) {
            throw new ConflictException("Enrollment already has a final grade and cannot be cancelled");
        }

        // RN-6.7: the student cannot end up below the minimum
        Long periodId = enrollment.getSection().getPeriod().getId();
        int remaining = enrollmentRepository.findByStudentIdAndSectionPeriodId(studentId, periodId).size() - 1;
        long minimum = getMinimumSubjects(enrollment.getStudent());
        if (remaining < minimum) {
            throw new ConflictException("Cannot cancel: the student must keep at least " + minimum + " subjects");
        }

        enrollmentRepository.delete(enrollment);   // RN-6.9: seats are counted, so the seat becomes free
    }

    // ==================== queries ====================

    // RN-6.2: sections of the period whose subject belongs to the student's plan
    @Transactional
    public List<OfferItemResponse> getAvailableOffer(Long studentId, Long periodId) {
        Student student = findStudentOrThrow(studentId);
        findPeriodOrThrow(periodId);
        return sectionRepository.findByPeriodId(periodId).stream()
                .filter(section -> belongsToStudentPlan(section.getSubject(), student))
                .map(section -> toOfferItem(section, studentId))
                .sorted((a, b) -> a.getSemester() - b.getSemester())
                .toList();
    }

    @Transactional
    public List<EnrollmentResponse> getStudentEnrollments(Long studentId, Long periodId) {
        findStudentOrThrow(studentId);
        findPeriodOrThrow(periodId);
        return enrollmentRepository.findByStudentIdAndSectionPeriodId(studentId, periodId).stream()
                .map(enrollment -> toResponse(enrollment))
                .toList();
    }

    // Admin view (section 4.3): all enrollments of a period
    @Transactional
    public List<EnrollmentResponse> getPeriodEnrollments(Long periodId) {
        findPeriodOrThrow(periodId);
        return enrollmentRepository.findBySectionPeriodId(periodId).stream()
                .map(enrollment -> toResponse(enrollment))
                .toList();
    }

    // ==================== validations ====================

    // RN-6.6: no empty list and no section chosen twice
    private void validateRequest(Long periodId, List<Long> sectionIds) {
        if (periodId == null) {
            throw new BadRequestException("Period is required");
        }
        if (sectionIds == null || sectionIds.isEmpty()) {
            throw new BadRequestException("Select at least one section");
        }
        if (sectionIds.stream().distinct().count() != sectionIds.size()) {
            throw new BadRequestException("The same section was selected twice");
        }
    }

    // RN-6.3: enrollments and cancellations only while the period is OPEN
    private void validatePeriodIsOpen(AcademicPeriod period) {
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new ConflictException("Period " + period.getCode() + " is CLOSED");
        }
    }

    // RN-6.1: the student must be REGULAR
    private void validateStudentIsRegular(Student student) {
        if (student.getStatus() != StudentStatus.REGULAR) {
            throw new ConflictException("Student status is " + student.getStatus() + "; only REGULAR students can enroll");
        }
    }

    // RN-6.1: the student must be registered ("matriculado") in the period
    private void validateStudentIsRegistered(Student student, AcademicPeriod period) {
        if (!registrationRepository.existsByStudentIdAndPeriodId(student.getId(), period.getId())) {
            throw new ConflictException("Student is not registered in period " + period.getCode());
        }
    }

    // RN-6.2: the section must be offered in THIS period
    private void validateSectionBelongsToPeriod(Section section, Long periodId) {
        if (!section.getPeriod().getId().equals(periodId)) {
            throw new BadRequestException("Section " + section.getId() + " does not belong to this period");
        }
    }

    // RN-6.2: the subject must belong to the student's study plan
    private void validateSubjectBelongsToStudentPlan(Subject subject, Student student) {
        if (!belongsToStudentPlan(subject, student)) {
            throw new BadRequestException("Subject " + subject.getCode() + " is not part of the student's study plan");
        }
    }

    // RN-6.6: the same subject cannot be enrolled twice in the same period
    private void validateNotAlreadyEnrolledInPeriod(Subject subject, List<Enrollment> currentEnrollments) {
        for (Enrollment enrollment : currentEnrollments) {
            if (enrollment.getSection().getSubject().getId().equals(subject.getId())) {
                throw new ConflictException("Subject " + subject.getCode() + " is already enrolled in this period");
            }
        }
    }

    // RN-6.4: a passed subject cannot be enrolled again (a failed one can)
    private void validateSubjectNotPassed(Long studentId, Subject subject) {
        if (hasPassed(studentId, subject.getId())) {
            throw new ConflictException("Subject " + subject.getCode() + " was already passed");
        }
    }

    // RN-6.5: all prerequisites must be passed
    private void validatePrerequisitesPassed(Long studentId, Subject subject) {
        for (Subject prerequisite : subject.getPrerequisites()) {
            if (!hasPassed(studentId, prerequisite.getId())) {
                throw new ConflictException("Subject " + subject.getCode() + " requires " + prerequisite.getCode()
                        + " to be passed first");
            }
        }
    }

    // RN-6.9: the section must have free seats
    private void validateSectionHasSeats(Section section) {
        if (!hasSeats(section)) {
            throw new ConflictException("Section of " + section.getSubject().getCode() + " has no available seats");
        }
    }

    // RN-6.7: between 3 and 6 subjects (fewer than 3 only if fewer are pending in the plan)
    private void validateSubjectCount(Student student, int totalSubjects) {
        long minimum = getMinimumSubjects(student);
        if (totalSubjects < minimum || totalSubjects > MAX_SUBJECTS) {
            throw new BadRequestException("Student must enroll between " + minimum + " and " + MAX_SUBJECTS
                    + " subjects per period (would have " + totalSubjects + ")");
        }
    }

    // RN-6.8: no higher-semester subject while a lower-semester one is pending AND available
    private void validateLowerSemestersFirst(Student student, Long periodId,
                                             List<Section> newSections, List<Section> allSections) {
        int highestNewSemester = 0;
        for (Section section : newSections) {
            highestNewSemester = Math.max(highestNewSemester, section.getSubject().getSemester());
        }

        List<Long> chosenSubjectIds = new ArrayList<>();
        for (Section section : allSections) {
            chosenSubjectIds.add(section.getSubject().getId());
        }

        for (Section offered : sectionRepository.findByPeriodId(periodId)) {
            Subject subject = offered.getSubject();
            boolean isAvailablePending = belongsToStudentPlan(subject, student)
                    && subject.getSemester() < highestNewSemester
                    && !chosenSubjectIds.contains(subject.getId())
                    && !hasPassed(student.getId(), subject.getId())
                    && allPrerequisitesPassed(student.getId(), subject)
                    && hasSeats(offered);
            if (isAvailablePending) {
                throw new ConflictException("Subject " + subject.getCode() + " (semester " + subject.getSemester()
                        + ") is pending and available; lower-semester subjects must be enrolled first");
            }
        }
    }

    // RN-6.10: the chosen sections cannot share a day+module
    private void validateNoScheduleConflicts(List<Section> sections) {
        List<String> usedBlocks = new ArrayList<>();
        for (Section section : sections) {
            for (ScheduleBlock block : section.getBlocks()) {
                if (usedBlocks.contains(block.getCode())) {
                    throw new ConflictException("Schedule conflict at " + block.getCode()
                            + " (" + section.getSubject().getCode() + ")");
                }
                usedBlocks.add(block.getCode());
            }
        }
    }

    // ==================== helpers ====================

    // RN-6.7: minimum = 3, or fewer if the student has fewer than 3 subjects left in the plan
    private long getMinimumSubjects(Student student) {
        long planSubjects = subjectRepository.countByStudyPlanId(student.getStudyPlan().getId());
        long passedSubjects = academicRecordRepository.countByStudentIdAndResult(student.getId(), SubjectResult.PASSED);
        return Math.min(MIN_SUBJECTS, planSubjects - passedSubjects);
    }

    private boolean belongsToStudentPlan(Subject subject, Student student) {
        return subject.getStudyPlan().getId().equals(student.getStudyPlan().getId());
    }

    private boolean hasPassed(Long studentId, Long subjectId) {
        return academicRecordRepository.existsByStudentIdAndSubjectIdAndResult(studentId, subjectId, SubjectResult.PASSED);
    }

    private boolean allPrerequisitesPassed(Long studentId, Subject subject) {
        for (Subject prerequisite : subject.getPrerequisites()) {
            if (!hasPassed(studentId, prerequisite.getId())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSeats(Section section) {
        return enrollmentRepository.countBySectionId(section.getId()) < section.getCapacity();
    }

    private Student findStudentOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student " + id + " not found"));
    }

    private AcademicPeriod findPeriodOrThrow(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Academic period " + id + " not found"));
    }

    private Section findSectionWithLockOrThrow(Long id) {
        return sectionRepository.findWithLockById(id)
                .orElseThrow(() -> new NotFoundException("Section " + id + " not found"));
    }

    // ==================== mapping ====================

    private List<String> blockCodes(Section section) {
        return section.getBlocks().stream()
                .map(block -> block.getCode())
                .toList();
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        Section section = enrollment.getSection();
        Student student = enrollment.getStudent();
        return new EnrollmentResponse(
                enrollment.getId(),
                student.getId(), student.getRun(), student.getFullName(),
                section.getPeriod().getId(), section.getPeriod().getCode(),
                section.getId(), section.getSubject().getCode(), section.getSubject().getName(),
                section.getSubject().getSemester(), section.getTeacher().getFullName(),
                blockCodes(section), enrollment.getEnrollmentDate(), enrollment.getFinalGrade());
    }

    private OfferItemResponse toOfferItem(Section section, Long studentId) {
        Subject subject = section.getSubject();
        List<String> prerequisiteCodes = subject.getPrerequisites().stream()
                .map(prerequisite -> prerequisite.getCode())
                .toList();
        long availableSeats = section.getCapacity() - enrollmentRepository.countBySectionId(section.getId());
        return new OfferItemResponse(
                section.getId(), subject.getCode(), subject.getName(), subject.getSemester(),
                subject.getSctCredits(), section.getTeacher().getFullName(), blockCodes(section),
                prerequisiteCodes, availableSeats,
                hasPassed(studentId, subject.getId()), allPrerequisitesPassed(studentId, subject));
    }
}