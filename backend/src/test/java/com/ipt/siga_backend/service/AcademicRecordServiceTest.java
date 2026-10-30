package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.AcademicRecordResponse;
import com.ipt.siga_backend.dto.ClosingResponse;
import com.ipt.siga_backend.entity.*;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicRecordServiceTest {

    @Mock
    private AcademicPeriodRepository periodRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AcademicRecordRepository academicRecordRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private AcademicRecordService academicRecordService;

    // ========== builders ==========

    private AcademicPeriod buildPeriod(PeriodStatus status) {
        AcademicPeriod period = new AcademicPeriod();
        period.setId(1L);
        period.setYear(2027);
        period.setSemester(1);
        period.setStatus(status);
        return period;
    }

    // Pedro (id 3), REGULAR, plan 1
    private Student buildStudent() {
        StudyPlan plan = new StudyPlan();
        plan.setId(1L);
        Student student = new Student();
        student.setId(3L);
        student.setFullName("Pedro Soto");
        student.setStatus(StudentStatus.REGULAR);
        student.setStudyPlan(plan);
        return student;
    }

    private Enrollment buildEnrollment(Student student, Long subjectId, String code, Double finalGrade,
                                       AcademicPeriod period) {
        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setCode(code);
        subject.setName("Subject " + code);
        subject.setSemester(1);
        subject.setSctCredits(6);

        Section section = new Section();
        section.setSubject(subject);
        section.setPeriod(period);
        return new Enrollment(subjectId, student, section, LocalDate.now(), finalGrade);
    }

    private void givenPeriodWithEnrollments(AcademicPeriod period, List<Enrollment> enrollments) {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(enrollmentRepository.findBySectionPeriodId(1L)).thenReturn(enrollments);
    }

    // RN-7.10: the plan has N subjects and the student passed M
    private void givenPlanProgress(long planSubjects, long passedSubjects) {
        when(subjectRepository.countByStudyPlanId(1L)).thenReturn(planSubjects);
        when(academicRecordRepository.countByStudentIdAndResult(3L, SubjectResult.PASSED)).thenReturn(passedSubjects);
    }

    // ========== closePeriod: success ==========

    // RN-7.8 / RN-7.11: all graded -> records created and period CLOSED
    @Test
    void closePeriod_allGraded_createsRecordsAndClosesPeriod() {
        AcademicPeriod period = buildPeriod(PeriodStatus.OPEN);
        Student pedro = buildStudent();
        givenPeriodWithEnrollments(period, List.of(
                buildEnrollment(pedro, 101L, "TAP101", 5.5, period),
                buildEnrollment(pedro, 102L, "TAP102", 6.0, period)));
        givenPlanProgress(24L, 2L);

        ClosingResponse result = academicRecordService.closePeriod(1L);

        assertEquals(2, result.getRecordsCreated());
        assertEquals(0, result.getGraduatedStudents());
        assertEquals(0, result.getEliminatedStudents());
        assertEquals(PeriodStatus.CLOSED, period.getStatus());                     // RN-7.11
        verify(academicRecordRepository, times(2)).save(any(AcademicRecord.class));  // RN-7.8
        verify(periodRepository).save(period);
        verify(studentRepository, never()).save(any());                            // stays REGULAR
    }

    // A period without enrollments can also be closed
    @Test
    void closePeriod_withoutEnrollments_closesPeriod() {
        AcademicPeriod period = buildPeriod(PeriodStatus.OPEN);
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));

        ClosingResponse result = academicRecordService.closePeriod(1L);

        assertEquals(0, result.getRecordsCreated());
        assertEquals(PeriodStatus.CLOSED, period.getStatus());
    }

    // RN-7.9: failed TAP101 for the SECOND time -> ELIMINATED
    @Test
    void closePeriod_failedSameSubjectTwice_studentEliminated() {
        AcademicPeriod period = buildPeriod(PeriodStatus.OPEN);
        Student pedro = buildStudent();
        givenPeriodWithEnrollments(period, List.of(buildEnrollment(pedro, 101L, "TAP101", 3.0, period)));
        when(academicRecordRepository.countByStudentIdAndSubjectIdAndResult(3L, 101L, SubjectResult.FAILED))
                .thenReturn(2L);

        ClosingResponse result = academicRecordService.closePeriod(1L);

        assertEquals(1, result.getEliminatedStudents());
        assertEquals(StudentStatus.ELIMINATED, pedro.getStatus());
        verify(studentRepository).save(pedro);
    }

    // RN-7.9: failing for the FIRST time does not eliminate (RN-6.4: he can retake it)
    @Test
    void closePeriod_failedFirstTime_studentStaysRegular() {
        AcademicPeriod period = buildPeriod(PeriodStatus.OPEN);
        Student pedro = buildStudent();
        givenPeriodWithEnrollments(period, List.of(buildEnrollment(pedro, 101L, "TAP101", 3.0, period)));
        when(academicRecordRepository.countByStudentIdAndSubjectIdAndResult(3L, 101L, SubjectResult.FAILED))
                .thenReturn(1L);
        givenPlanProgress(24L, 0L);

        academicRecordService.closePeriod(1L);

        assertEquals(StudentStatus.REGULAR, pedro.getStatus());
        verify(studentRepository, never()).save(any());
    }

    // RN-7.10: passed every subject of the plan -> GRADUATED
    @Test
    void closePeriod_passedWholePlan_studentGraduated() {
        AcademicPeriod period = buildPeriod(PeriodStatus.OPEN);
        Student pedro = buildStudent();
        givenPeriodWithEnrollments(period, List.of(buildEnrollment(pedro, 406L, "TAP406", 6.0, period)));
        givenPlanProgress(24L, 24L);

        ClosingResponse result = academicRecordService.closePeriod(1L);

        assertEquals(1, result.getGraduatedStudents());
        assertEquals(StudentStatus.GRADUATED, pedro.getStatus());
        verify(studentRepository).save(pedro);
    }

    // ========== closePeriod: rejections ==========

    // RN-7.7: pending grades -> rejected, and the incomplete sections are reported
    @Test
    void closePeriod_pendingGrades_throwsConflictWithSections() {
        AcademicPeriod period = buildPeriod(PeriodStatus.OPEN);
        Student pedro = buildStudent();
        givenPeriodWithEnrollments(period, List.of(
                buildEnrollment(pedro, 101L, "TAP101", 5.5, period),
                buildEnrollment(pedro, 102L, "TAP102", null, period)));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> academicRecordService.closePeriod(1L));

        assertTrue(exception.getMessage().contains("TAP102"));
        assertEquals(PeriodStatus.OPEN, period.getStatus());
        verify(academicRecordRepository, never()).save(any());
    }

    // RN-7.6 / RN-7.12: a CLOSED period cannot be closed again
    @Test
    void closePeriod_alreadyClosed_throwsConflict() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.CLOSED)));

        assertThrows(ConflictException.class, () -> academicRecordService.closePeriod(1L));
        verify(academicRecordRepository, never()).save(any());
    }

    @Test
    void closePeriod_periodNotFound_throwsNotFound() {
        when(periodRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> academicRecordService.closePeriod(99L));
    }

    // ========== getStudentRecord ==========

    // Records come back ordered by semester
    @Test
    void getStudentRecord_returnsRecordsOrderedBySemester() {
        AcademicPeriod period = buildPeriod(PeriodStatus.CLOSED);
        AcademicRecord semesterTwo = new AcademicRecord(1L, buildStudent(), null, period,
                "TAP201", "Programación Orientada a Objetos", 2, 6, 5.0, SubjectResult.PASSED);
        AcademicRecord semesterOne = new AcademicRecord(2L, buildStudent(), null, period,
                "TAP101", "Fundamentos de Programación", 1, 6, 6.0, SubjectResult.PASSED);
        when(studentRepository.existsById(3L)).thenReturn(true);
        when(academicRecordRepository.findByStudentId(3L)).thenReturn(List.of(semesterTwo, semesterOne));

        List<AcademicRecordResponse> result = academicRecordService.getStudentRecord(3L);

        assertEquals("TAP101", result.get(0).getSubjectCode());
        assertEquals("2027-1", result.get(0).getPeriodCode());
    }

    @Test
    void getStudentRecord_studentNotFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> academicRecordService.getStudentRecord(99L));
    }
}