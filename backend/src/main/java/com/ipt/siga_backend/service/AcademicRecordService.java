package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.AcademicRecordResponse;
import com.ipt.siga_backend.dto.ClosingResponse;
import com.ipt.siga_backend.entity.*;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AcademicRecordService {

    private static final int TIMES_FAILED_TO_BE_ELIMINATED = 2;

    private final AcademicPeriodRepository periodRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    public AcademicRecordService(AcademicPeriodRepository periodRepository, EnrollmentRepository enrollmentRepository,
                                 AcademicRecordRepository academicRecordRepository,
                                 StudentRepository studentRepository, SubjectRepository subjectRepository) {
        this.periodRepository = periodRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.academicRecordRepository = academicRecordRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
    }

    // ==================== period closing ====================

    @Transactional
    public ClosingResponse closePeriod(Long periodId) {
        AcademicPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Academic period " + periodId + " not found"));
        validatePeriodIsOpen(period);                                              // RN-7.6 / RN-7.12 -> 409

        List<Enrollment> enrollments = enrollmentRepository.findBySectionPeriodId(periodId);
        validateAllEnrollmentsHaveGrade(enrollments);                              // RN-7.7 -> 409

        // RN-7.8: grades and results are consolidated into the academic record
        for (Enrollment enrollment : enrollments) {
            academicRecordRepository.save(buildRecord(enrollment, period));
        }

        // RN-7.9 / RN-7.10: automatic status of every student of the period
        int eliminated = 0;
        int graduated = 0;
        for (Student student : getDistinctStudents(enrollments)) {
            if (failedSomeSubjectTwice(student, enrollments)) {
                student.setStatus(StudentStatus.ELIMINATED);                       // RN-7.9 (all data is kept)
                studentRepository.save(student);
                eliminated++;
            } else if (passedWholePlan(student)) {
                student.setStatus(StudentStatus.GRADUATED);                        // RN-7.10
                studentRepository.save(student);
                graduated++;
            }
        }

        period.setStatus(PeriodStatus.CLOSED);                                     // RN-7.11
        periodRepository.save(period);
        return new ClosingResponse(period.getCode(), enrollments.size(), graduated, eliminated);
    }

    // ==================== student's academic record ====================

    @Transactional
    public List<AcademicRecordResponse> getStudentRecord(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new NotFoundException("Student " + studentId + " not found");
        }
        return academicRecordRepository.findByStudentId(studentId).stream()
                .map(record -> toResponse(record))
                .sorted((a, b) -> a.getSemester() - b.getSemester())
                .toList();
    }

    // ==================== validations ====================

    // RN-7.6 / RN-7.12: only an OPEN period can be closed (and only once)
    private void validatePeriodIsOpen(AcademicPeriod period) {
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new ConflictException("Period " + period.getCode() + " is already CLOSED");
        }
    }

    // RN-7.7: every enrollment needs a final grade; otherwise report the incomplete sections
    private void validateAllEnrollmentsHaveGrade(List<Enrollment> enrollments) {
        List<String> incompleteSections = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            String subjectCode = enrollment.getSection().getSubject().getCode();
            if (enrollment.getFinalGrade() == null && !incompleteSections.contains(subjectCode)) {
                incompleteSections.add(subjectCode);
            }
        }
        if (!incompleteSections.isEmpty()) {
            throw new ConflictException("Cannot close the period. Sections with pending grades: "
                    + String.join(", ", incompleteSections));
        }
    }

    // ==================== helpers ====================

    // RN-7.8 + RN-3.11: a COPY of the subject data, so later edits do not change history
    private AcademicRecord buildRecord(Enrollment enrollment, AcademicPeriod period) {
        Subject subject = enrollment.getSection().getSubject();
        AcademicRecord record = new AcademicRecord();
        record.setStudent(enrollment.getStudent());
        record.setSubject(subject);
        record.setPeriod(period);
        record.setSubjectCode(subject.getCode());
        record.setSubjectName(subject.getName());
        record.setSemester(subject.getSemester());
        record.setSctCredits(subject.getSctCredits());
        record.setFinalGrade(enrollment.getFinalGrade());
        record.setResult(enrollment.getResult());                                  // RN-7.5
        return record;
    }

    private List<Student> getDistinctStudents(List<Enrollment> enrollments) {
        List<Student> students = new ArrayList<>();
        List<Long> seenIds = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Student student = enrollment.getStudent();
            if (!seenIds.contains(student.getId())) {
                seenIds.add(student.getId());
                students.add(student);
            }
        }
        return students;
    }

    // RN-7.9: did the student fail, in this period, a subject that he had already failed before?
    private boolean failedSomeSubjectTwice(Student student, List<Enrollment> enrollments) {
        for (Enrollment enrollment : enrollments) {
            boolean isThisStudent = enrollment.getStudent().getId().equals(student.getId());
            if (isThisStudent && enrollment.getResult() == SubjectResult.FAILED) {
                Long subjectId = enrollment.getSection().getSubject().getId();
                long timesFailed = academicRecordRepository.countByStudentIdAndSubjectIdAndResult(
                        student.getId(), subjectId, SubjectResult.FAILED);
                if (timesFailed >= TIMES_FAILED_TO_BE_ELIMINATED) {
                    return true;
                }
            }
        }
        return false;
    }

    // RN-7.10: passed every subject of his study plan
    private boolean passedWholePlan(Student student) {
        long planSubjects = subjectRepository.countByStudyPlanId(student.getStudyPlan().getId());
        long passedSubjects = academicRecordRepository.countByStudentIdAndResult(student.getId(), SubjectResult.PASSED);
        return passedSubjects >= planSubjects;
    }

    private AcademicRecordResponse toResponse(AcademicRecord record) {
        return new AcademicRecordResponse(
                record.getPeriod().getCode(), record.getSubjectCode(), record.getSubjectName(),
                record.getSemester(), record.getSctCredits(), record.getFinalGrade(), record.getResult());
    }
}