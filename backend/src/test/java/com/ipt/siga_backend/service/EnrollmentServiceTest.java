package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.EnrollmentResponse;
import com.ipt.siga_backend.dto.OfferItemResponse;
import com.ipt.siga_backend.entity.*;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AcademicPeriodRepository periodRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private PeriodRegistrationRepository registrationRepository;

    @Mock
    private AcademicRecordRepository academicRecordRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    // ==================== builders ====================

    private StudyPlan buildPlan(Long id) {
        StudyPlan plan = new StudyPlan();
        plan.setId(id);
        return plan;
    }

    private AcademicPeriod buildPeriod(PeriodStatus status) {
        AcademicPeriod period = new AcademicPeriod();
        period.setId(1L);
        period.setYear(2027);
        period.setSemester(1);
        period.setStatus(status);
        return period;
    }

    // Pedro Soto (id 3), plan 1
    private Student buildStudent(StudentStatus status) {
        Student student = new Student();
        student.setId(3L);
        student.setRun("12345678-9");
        student.setFullName("Pedro Soto");
        student.setStatus(status);
        student.setStudyPlan(buildPlan(1L));
        return student;
    }

    // A subject of plan 1, without prerequisites
    private Subject buildSubject(Long id, String code, int semester) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setCode(code);
        subject.setName("Subject " + code);
        subject.setSemester(semester);
        subject.setSctCredits(6);
        subject.setStudyPlan(buildPlan(1L));
        return subject;
    }

    // blockCodes like "L2" -> day L, module 2
    private Section buildSection(Long id, Subject subject, AcademicPeriod period, int capacity, String... blockCodes) {
        Teacher teacher = new Teacher();
        teacher.setFullName("María González");

        Section section = new Section();
        section.setId(id);
        section.setCapacity(capacity);
        section.setSubject(subject);
        section.setPeriod(period);
        section.setTeacher(teacher);
        for (String code : blockCodes) {
            ScheduleBlock block = new ScheduleBlock();
            block.setDay(ScheduleDay.valueOf(code.substring(0, 1)));
            block.setModuleNumber(Integer.parseInt(code.substring(1)));
            block.setSection(section);
            section.getBlocks().add(block);
        }
        return section;
    }

    private Enrollment buildEnrollment(Long id, Student student, Section section, Double finalGrade) {
        return new Enrollment(id, student, section, LocalDate.now(), finalGrade);
    }

    // Three valid semester-1 sections of period 2027-1 (OPEN), without schedule conflicts
    private Section tap101() {
        return buildSection(1L, buildSubject(101L, "TAP101", 1), buildPeriod(PeriodStatus.OPEN), 40, "L2");
    }

    private Section tap102() {
        return buildSection(2L, buildSubject(102L, "TAP102", 1), buildPeriod(PeriodStatus.OPEN), 40, "M1");
    }

    private Section tap103() {
        return buildSection(3L, buildSubject(103L, "TAP103", 1), buildPeriod(PeriodStatus.OPEN), 40, "W1");
    }

    private List<Enrollment> enrollmentsOfSize(int size) {
        List<Enrollment> enrollments = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            enrollments.add(new Enrollment());
        }
        return enrollments;
    }

    // ==================== givens ====================

    // REGULAR student, OPEN period, student registered in the period (RN-6.1 / RN-6.3 pass)
    private void givenValidContext() {
        when(studentRepository.findById(3L)).thenReturn(Optional.of(buildStudent(StudentStatus.REGULAR)));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        when(registrationRepository.existsByStudentIdAndPeriodId(3L, 1L)).thenReturn(true);
    }

    private void givenSections(Section... sections) {
        for (Section section : sections) {
            when(sectionRepository.findWithLockById(section.getId())).thenReturn(Optional.of(section));
        }
    }

    // RN-6.7: the plan has N subjects and the student passed M of them
    private void givenPlanProgress(long planSubjects, long passedSubjects) {
        when(subjectRepository.countByStudyPlanId(1L)).thenReturn(planSubjects);
        when(academicRecordRepository.countByStudentIdAndResult(3L, SubjectResult.PASSED)).thenReturn(passedSubjects);
    }

    private void givenSaveReturnsArgument() {
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void assertBadRequestOnEnroll(List<Long> sectionIds) {
        assertThrows(BadRequestException.class, () -> enrollmentService.enroll(3L, 1L, sectionIds));
        verify(enrollmentRepository, never()).save(any());
    }

    private void assertConflictOnEnroll(List<Long> sectionIds) {
        assertThrows(ConflictException.class, () -> enrollmentService.enroll(3L, 1L, sectionIds));
        verify(enrollmentRepository, never()).save(any());
    }

    // ==================== enroll: success ====================

    // RN-6.1..6.11: 3 valid subjects -> 3 enrollments with today's date
    @Test
    void enroll_threeValidSubjects_createsEnrollments() {
        givenValidContext();
        givenSections(tap101(), tap102(), tap103());
        givenPlanProgress(24L, 0L);
        givenSaveReturnsArgument();

        List<EnrollmentResponse> result = enrollmentService.enroll(3L, 1L, List.of(1L, 2L, 3L));

        assertEquals(3, result.size());
        assertEquals(LocalDate.now(), result.get(0).getEnrollmentDate());   // RN-6.11
        verify(enrollmentRepository, times(3)).save(any(Enrollment.class));
    }

    // RN-6.7 exception: only 2 subjects left in the plan -> 2 are allowed
    @Test
    void enroll_fewPendingSubjects_allowsLessThanThree() {
        givenValidContext();
        givenSections(tap101(), tap102());
        givenPlanProgress(24L, 22L);
        givenSaveReturnsArgument();

        List<EnrollmentResponse> result = enrollmentService.enroll(3L, 1L, List.of(1L, 2L));

        assertEquals(2, result.size());
    }

    // ==================== enroll: 400 / 404 on the request ====================

    @Test
    void enroll_nullPeriod_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> enrollmentService.enroll(3L, null, List.of(1L)));
    }

    @Test
    void enroll_noSections_throwsBadRequest() {
        assertBadRequestOnEnroll(List.of());
    }

    // RN-6.6: the same section twice in one request
    @Test
    void enroll_duplicatedSectionIds_throwsBadRequest() {
        assertBadRequestOnEnroll(List.of(1L, 1L, 2L));
    }

    @Test
    void enroll_studentNotFound_throwsNotFound() {
        when(studentRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> enrollmentService.enroll(3L, 1L, List.of(1L)));
    }

    @Test
    void enroll_sectionNotFound_throwsNotFound() {
        givenValidContext();
        when(sectionRepository.findWithLockById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> enrollmentService.enroll(3L, 1L, List.of(99L)));
    }

    // ==================== enroll: student and period ====================

    // RN-6.3: period must be OPEN
    @Test
    void enroll_closedPeriod_throwsConflict() {
        when(studentRepository.findById(3L)).thenReturn(Optional.of(buildStudent(StudentStatus.REGULAR)));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.CLOSED)));
        assertConflictOnEnroll(List.of(1L, 2L, 3L));
    }

    // RN-6.1: student must be REGULAR
    @Test
    void enroll_studentNotRegular_throwsConflict() {
        when(studentRepository.findById(3L)).thenReturn(Optional.of(buildStudent(StudentStatus.POSTPONED)));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        assertConflictOnEnroll(List.of(1L, 2L, 3L));
    }

    // RN-6.1: student must be registered in the period (the mock answers false by default)
    @Test
    void enroll_studentNotRegistered_throwsConflict() {
        when(studentRepository.findById(3L)).thenReturn(Optional.of(buildStudent(StudentStatus.REGULAR)));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        assertConflictOnEnroll(List.of(1L, 2L, 3L));
    }

    // ==================== enroll: rules per section ====================

    // RN-6.2: section of another period
    @Test
    void enroll_sectionFromOtherPeriod_throwsBadRequest() {
        givenValidContext();
        AcademicPeriod otherPeriod = buildPeriod(PeriodStatus.OPEN);
        otherPeriod.setId(2L);
        givenSections(buildSection(1L, buildSubject(101L, "TAP101", 1), otherPeriod, 40, "L2"));
        assertBadRequestOnEnroll(List.of(1L));
    }

    // RN-6.2: subject of another study plan
    @Test
    void enroll_subjectFromOtherPlan_throwsBadRequest() {
        givenValidContext();
        Subject otherPlanSubject = buildSubject(301L, "TCS101", 1);
        otherPlanSubject.setStudyPlan(buildPlan(2L));
        givenSections(buildSection(9L, otherPlanSubject, buildPeriod(PeriodStatus.OPEN), 40, "L2"));
        assertBadRequestOnEnroll(List.of(9L));
    }

    // RN-6.6: the subject is already enrolled in this period
    @Test
    void enroll_subjectAlreadyEnrolledInPeriod_throwsConflict() {
        givenValidContext();
        when(enrollmentRepository.findByStudentIdAndSectionPeriodId(3L, 1L))
                .thenReturn(List.of(buildEnrollment(50L, buildStudent(StudentStatus.REGULAR), tap101(), null)));
        givenSections(tap101());
        assertConflictOnEnroll(List.of(1L));
    }

    // RN-6.4: an already passed subject cannot be enrolled again
    @Test
    void enroll_subjectAlreadyPassed_throwsConflict() {
        givenValidContext();
        givenSections(tap101());
        when(academicRecordRepository.existsByStudentIdAndSubjectIdAndResult(3L, 101L, SubjectResult.PASSED))
                .thenReturn(true);
        assertConflictOnEnroll(List.of(1L));
    }

    // RN-6.5: TAP201 requires TAP101, which is not passed
    @Test
    void enroll_prerequisiteNotPassed_throwsConflict() {
        givenValidContext();
        Subject tap201 = buildSubject(201L, "TAP201", 2);
        tap201.getPrerequisites().add(buildSubject(101L, "TAP101", 1));
        givenSections(buildSection(5L, tap201, buildPeriod(PeriodStatus.OPEN), 40, "J1"));
        assertConflictOnEnroll(List.of(5L));
    }

    // RN-6.9: section is full
    @Test
    void enroll_sectionWithoutSeats_throwsConflict() {
        givenValidContext();
        givenSections(buildSection(1L, buildSubject(101L, "TAP101", 1), buildPeriod(PeriodStatus.OPEN), 1, "L2"));
        when(enrollmentRepository.countBySectionId(1L)).thenReturn(1L);
        assertConflictOnEnroll(List.of(1L));
    }

    // ==================== enroll: rules on the whole set ====================

    // RN-6.7: fewer than 3 subjects
    @Test
    void enroll_lessThanThreeSubjects_throwsBadRequest() {
        givenValidContext();
        givenSections(tap101(), tap102());
        givenPlanProgress(24L, 0L);
        assertBadRequestOnEnroll(List.of(1L, 2L));
    }

    // RN-6.7: already 5 enrolled + 2 new = 7 > 6
    @Test
    void enroll_moreThanSixSubjects_throwsBadRequest() {
        givenValidContext();
        List<Enrollment> current = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            Section section = buildSection(10L + i, buildSubject(200L + i, "TAP20" + i, 2),
                    buildPeriod(PeriodStatus.OPEN), 40, "J" + i);
            current.add(buildEnrollment(i, buildStudent(StudentStatus.REGULAR), section, null));
        }
        when(enrollmentRepository.findByStudentIdAndSectionPeriodId(3L, 1L)).thenReturn(current);
        givenSections(tap101(), tap102());
        givenPlanProgress(24L, 0L);
        assertBadRequestOnEnroll(List.of(1L, 2L));
    }

    // RN-6.8: enrolls 3 semester-2 subjects while TAP104 (semester 1) is pending and available
    @Test
    void enroll_lowerSemesterPendingAndAvailable_throwsConflict() {
        givenValidContext();
        AcademicPeriod open = buildPeriod(PeriodStatus.OPEN);
        Section tap201 = buildSection(5L, buildSubject(201L, "TAP201", 2), open, 40, "J1");
        Section tap202 = buildSection(6L, buildSubject(202L, "TAP202", 2), open, 40, "J2");
        Section tap203 = buildSection(7L, buildSubject(203L, "TAP203", 2), open, 40, "J3");
        Section tap104 = buildSection(8L, buildSubject(104L, "TAP104", 1), open, 40, "V1");
        givenSections(tap201, tap202, tap203);
        givenPlanProgress(24L, 6L);
        when(sectionRepository.findByPeriodId(1L)).thenReturn(List.of(tap201, tap202, tap203, tap104));
        assertConflictOnEnroll(List.of(5L, 6L, 7L));
    }

    // RN-6.10: TAP101 and TAP104 are both at L2
    @Test
    void enroll_scheduleConflict_throwsConflict() {
        givenValidContext();
        Section tap104AtL2 = buildSection(4L, buildSubject(104L, "TAP104", 1), buildPeriod(PeriodStatus.OPEN), 40, "L2");
        givenSections(tap101(), tap102(), tap104AtL2);
        givenPlanProgress(24L, 0L);
        assertConflictOnEnroll(List.of(1L, 2L, 4L));
    }

    // ==================== cancelEnrollment ====================

    private Enrollment enrollmentOfPedro(PeriodStatus periodStatus, Double finalGrade) {
        Section section = buildSection(1L, buildSubject(101L, "TAP101", 1), buildPeriod(periodStatus), 40, "L2");
        return buildEnrollment(50L, buildStudent(StudentStatus.REGULAR), section, finalGrade);
    }

    // RN-6.9: cancelling deletes the enrollment, so the seat is free again
    @Test
    void cancelEnrollment_valid_deletesEnrollment() {
        Enrollment enrollment = enrollmentOfPedro(PeriodStatus.OPEN, null);
        when(enrollmentRepository.findById(50L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.findByStudentIdAndSectionPeriodId(3L, 1L)).thenReturn(enrollmentsOfSize(4));
        givenPlanProgress(24L, 0L);

        enrollmentService.cancelEnrollment(3L, 50L);

        verify(enrollmentRepository).delete(enrollment);
    }

    @Test
    void cancelEnrollment_notFound_throwsNotFound() {
        when(enrollmentRepository.findById(50L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> enrollmentService.cancelEnrollment(3L, 50L));
    }

    // A student cannot cancel someone else's enrollment
    @Test
    void cancelEnrollment_otherStudent_throwsNotFound() {
        when(enrollmentRepository.findById(50L)).thenReturn(Optional.of(enrollmentOfPedro(PeriodStatus.OPEN, null)));

        assertThrows(NotFoundException.class, () -> enrollmentService.cancelEnrollment(9L, 50L));
        verify(enrollmentRepository, never()).delete(any());
    }

    // RN-6.3: no cancellations in a CLOSED period
    @Test
    void cancelEnrollment_closedPeriod_throwsConflict() {
        when(enrollmentRepository.findById(50L)).thenReturn(Optional.of(enrollmentOfPedro(PeriodStatus.CLOSED, null)));

        assertThrows(ConflictException.class, () -> enrollmentService.cancelEnrollment(3L, 50L));
        verify(enrollmentRepository, never()).delete(any());
    }

    // Decision: an enrollment with a final grade cannot be cancelled
    @Test
    void cancelEnrollment_withFinalGrade_throwsConflict() {
        when(enrollmentRepository.findById(50L)).thenReturn(Optional.of(enrollmentOfPedro(PeriodStatus.OPEN, 5.5)));

        assertThrows(ConflictException.class, () -> enrollmentService.cancelEnrollment(3L, 50L));
        verify(enrollmentRepository, never()).delete(any());
    }

    // RN-6.7: with 3 enrolled, cancelling one would leave 2
    @Test
    void cancelEnrollment_belowMinimum_throwsConflict() {
        when(enrollmentRepository.findById(50L)).thenReturn(Optional.of(enrollmentOfPedro(PeriodStatus.OPEN, null)));
        when(enrollmentRepository.findByStudentIdAndSectionPeriodId(3L, 1L)).thenReturn(enrollmentsOfSize(3));
        givenPlanProgress(24L, 0L);

        assertThrows(ConflictException.class, () -> enrollmentService.cancelEnrollment(3L, 50L));
        verify(enrollmentRepository, never()).delete(any());
    }

    // ==================== queries ====================

    // RN-6.2: the offer only shows sections of the student's plan
    @Test
    void getAvailableOffer_onlyShowsStudentPlan() {
        Subject otherPlanSubject = buildSubject(301L, "TCS101", 1);
        otherPlanSubject.setStudyPlan(buildPlan(2L));
        Section otherPlanSection = buildSection(9L, otherPlanSubject, buildPeriod(PeriodStatus.OPEN), 40, "L2");
        when(studentRepository.findById(3L)).thenReturn(Optional.of(buildStudent(StudentStatus.REGULAR)));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        when(sectionRepository.findByPeriodId(1L)).thenReturn(List.of(tap101(), otherPlanSection));

        List<OfferItemResponse> result = enrollmentService.getAvailableOffer(3L, 1L);

        assertEquals(1, result.size());
        assertEquals("TAP101", result.get(0).getSubjectCode());
        assertEquals(40, result.get(0).getAvailableSeats());
        assertFalse(result.get(0).isPassed());
        assertTrue(result.get(0).isPrerequisitesPassed());
    }

    @Test
    void getStudentEnrollments_returnsList() {
        when(studentRepository.findById(3L)).thenReturn(Optional.of(buildStudent(StudentStatus.REGULAR)));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        when(enrollmentRepository.findByStudentIdAndSectionPeriodId(3L, 1L))
                .thenReturn(List.of(buildEnrollment(50L, buildStudent(StudentStatus.REGULAR), tap101(), null)));

        List<EnrollmentResponse> result = enrollmentService.getStudentEnrollments(3L, 1L);

        assertEquals(1, result.size());
        assertEquals("TAP101", result.get(0).getSubjectCode());
        assertEquals(List.of("L2"), result.get(0).getBlocks());
    }

    @Test
    void getPeriodEnrollments_returnsList() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        when(enrollmentRepository.findBySectionPeriodId(1L))
                .thenReturn(List.of(buildEnrollment(50L, buildStudent(StudentStatus.REGULAR), tap101(), null)));

        assertEquals(1, enrollmentService.getPeriodEnrollments(1L).size());
    }

    @Test
    void getPeriodEnrollments_periodNotFound_throwsNotFound() {
        when(periodRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> enrollmentService.getPeriodEnrollments(99L));
    }
}