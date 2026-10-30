package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.GradeResponse;
import com.ipt.siga_backend.entity.*;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.ForbiddenException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.EnrollmentRepository;
import com.ipt.siga_backend.repository.SectionRepository;
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
class GradeServiceTest {

    private static final Long MARIA_ID = 7L;
    private static final Long JUAN_ID = 8L;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private GradeService gradeService;

    // ========== builders ==========

    // Section of TAP101 taught by María (id 7)
    private Section buildSection(PeriodStatus periodStatus) {
        Teacher maria = new Teacher();
        maria.setId(MARIA_ID);
        maria.setFullName("María González");

        Subject subject = new Subject();
        subject.setId(101L);
        subject.setCode("TAP101");

        AcademicPeriod period = new AcademicPeriod();
        period.setId(1L);
        period.setYear(2027);
        period.setSemester(1);
        period.setStatus(periodStatus);

        Section section = new Section();
        section.setId(1L);
        section.setTeacher(maria);
        section.setSubject(subject);
        section.setPeriod(period);
        return section;
    }

    // Pedro's enrollment (id 50) in María's section
    private Enrollment buildEnrollment(PeriodStatus periodStatus, Double finalGrade) {
        Student pedro = new Student();
        pedro.setId(3L);
        pedro.setRun("12345678-9");
        pedro.setFullName("Pedro Soto");
        return new Enrollment(50L, pedro, buildSection(periodStatus), LocalDate.now(), finalGrade);
    }

    private Enrollment givenEnrollment(PeriodStatus periodStatus, Double finalGrade) {
        Enrollment enrollment = buildEnrollment(periodStatus, finalGrade);
        when(enrollmentRepository.findById(50L)).thenReturn(Optional.of(enrollment));
        return enrollment;
    }

    private void givenSaveReturnsArgument() {
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void assertBadRequestOnGrade(Double finalGrade) {
        givenEnrollment(PeriodStatus.OPEN, null);
        assertThrows(BadRequestException.class, () -> gradeService.registerFinalGrade(MARIA_ID, 50L, finalGrade));
        verify(enrollmentRepository, never()).save(any());
    }

    // ========== getSectionGrades ==========

    // RN-P.2: the teacher sees the students of HIS section
    @Test
    void getSectionGrades_ownSection_returnsStudents() {
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(buildSection(PeriodStatus.OPEN)));
        when(enrollmentRepository.findBySectionId(1L)).thenReturn(List.of(buildEnrollment(PeriodStatus.OPEN, null)));

        List<GradeResponse> result = gradeService.getSectionGrades(MARIA_ID, 1L);

        assertEquals(1, result.size());
        assertEquals("Pedro Soto", result.get(0).getStudentName());
        assertNull(result.get(0).getFinalGrade());      // pending
        assertNull(result.get(0).getResult());
    }

    // RN-7.1: Juan cannot see María's section
    @Test
    void getSectionGrades_otherTeacher_throwsForbidden() {
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(buildSection(PeriodStatus.OPEN)));

        assertThrows(ForbiddenException.class, () -> gradeService.getSectionGrades(JUAN_ID, 1L));
    }

    @Test
    void getSectionGrades_sectionNotFound_throwsNotFound() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> gradeService.getSectionGrades(MARIA_ID, 99L));
    }

    // ========== registerFinalGrade: success ==========

    // RN-7.5: 5.5 -> PASSED
    @Test
    void registerFinalGrade_fivePointFive_isPassed() {
        givenEnrollment(PeriodStatus.OPEN, null);
        givenSaveReturnsArgument();

        GradeResponse result = gradeService.registerFinalGrade(MARIA_ID, 50L, 5.5);

        assertEquals(5.5, result.getFinalGrade());
        assertEquals(SubjectResult.PASSED, result.getResult());
    }

    // RN-7.5: 3.9 -> FAILED
    @Test
    void registerFinalGrade_threePointNine_isFailed() {
        givenEnrollment(PeriodStatus.OPEN, null);
        givenSaveReturnsArgument();

        GradeResponse result = gradeService.registerFinalGrade(MARIA_ID, 50L, 3.9);

        assertEquals(SubjectResult.FAILED, result.getResult());
    }

    // RN-7.5 boundary: exactly 4.0 -> PASSED
    @Test
    void registerFinalGrade_exactlyFour_isPassed() {
        givenEnrollment(PeriodStatus.OPEN, null);
        givenSaveReturnsArgument();

        GradeResponse result = gradeService.registerFinalGrade(MARIA_ID, 50L, 4.0);

        assertEquals(SubjectResult.PASSED, result.getResult());
    }

    // RN-7.3 / RN-7.4: while OPEN, registering again REPLACES the only grade
    @Test
    void registerFinalGrade_existingGrade_isReplaced() {
        Enrollment enrollment = givenEnrollment(PeriodStatus.OPEN, 5.0);
        givenSaveReturnsArgument();

        gradeService.registerFinalGrade(MARIA_ID, 50L, 6.0);

        assertEquals(6.0, enrollment.getFinalGrade());
    }

    // ========== registerFinalGrade: rejections ==========

    @Test
    void registerFinalGrade_enrollmentNotFound_throwsNotFound() {
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> gradeService.registerFinalGrade(MARIA_ID, 99L, 5.0));
    }

    // RN-7.1: only the responsible teacher
    @Test
    void registerFinalGrade_otherTeacher_throwsForbidden() {
        givenEnrollment(PeriodStatus.OPEN, null);

        assertThrows(ForbiddenException.class, () -> gradeService.registerFinalGrade(JUAN_ID, 50L, 5.0));
        verify(enrollmentRepository, never()).save(any());
    }

    // RN-7.4 / RN-7.12: no grades in a CLOSED period
    @Test
    void registerFinalGrade_closedPeriod_throwsConflict() {
        givenEnrollment(PeriodStatus.CLOSED, 5.0);

        assertThrows(ConflictException.class, () -> gradeService.registerFinalGrade(MARIA_ID, 50L, 6.0));
        verify(enrollmentRepository, never()).save(any());
    }

    // RN-7.3: range 1.0 .. 7.0
    @Test
    void registerFinalGrade_aboveSeven_throwsBadRequest() {
        assertBadRequestOnGrade(7.1);
    }

    @Test
    void registerFinalGrade_belowOne_throwsBadRequest() {
        assertBadRequestOnGrade(0.9);
    }

    @Test
    void registerFinalGrade_nullGrade_throwsBadRequest() {
        assertBadRequestOnGrade(null);
    }
}