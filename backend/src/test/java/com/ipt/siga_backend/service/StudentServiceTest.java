package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.StudentRequest;
import com.ipt.siga_backend.dto.StudentResponse;
import com.ipt.siga_backend.dto.StudentUpdateRequest;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CareerRepository careerRepository;

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AcademicRecordRepository academicRecordRepository;

    @InjectMocks
    private StudentService studentService;

    // ========== helpers ==========

    private Career buildCareer(Long id, CareerStatus status) {
        Career career = new Career();
        career.setId(id);
        career.setCode("1045" + id);
        career.setName("Career " + id);
        career.setStatus(status);
        return career;
    }

    private StudyPlan buildPlan(Long id, Career career, String identifier) {
        StudyPlan plan = new StudyPlan();
        plan.setId(id);
        plan.setIdentifier(identifier);
        plan.setStatus(StudyPlanStatus.CURRENT);
        plan.setCareer(career);
        return plan;
    }

    // Saved student in career 1, plan 2021.3
    private Student buildStudent(Long id, StudentStatus status) {
        Student student = new Student();
        student.setId(id);
        student.setRun("12345678-9");
        student.setFullName("Juan Pérez");
        student.setEmail("juan.perez@ipt.cl");
        student.setStatus(status);
        student.setStudyPlan(buildPlan(1L, buildCareer(1L, CareerStatus.ACTIVE), "2021.3"));
        return student;
    }

    // A VALID request. Each test breaks only ONE field.
    private StudentRequest buildRequest() {
        return new StudentRequest("12345678-9", "Juan Pérez", "juan.perez@ipt.cl", 1L);
    }

    // Active career 1, RUN and email free, current plan 2021.3
    private void givenValidCareerSetup() {
        Career career = buildCareer(1L, CareerStatus.ACTIVE);
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career));
        when(studentRepository.existsByRun("12345678-9")).thenReturn(false);
        when(studentRepository.existsByEmail("juan.perez@ipt.cl")).thenReturn(false);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT))
                .thenReturn(Optional.of(buildPlan(1L, career, "2021.3")));
    }

    private void assertBadRequestOnCreate(StudentRequest request) {
        assertThrows(BadRequestException.class, () -> studentService.createStudent(request));
        verify(studentRepository, never()).save(any());
    }

    private void assertConflictOnCreate(StudentRequest request) {
        assertThrows(ConflictException.class, () -> studentService.createStudent(request));
        verify(studentRepository, never()).save(any());
    }

    // ========== createStudent ==========

    // RN-1.1 / RN-1.5: valid data -> REGULAR + current plan of the career
    @Test
    void createStudent_valid_assignsCurrentPlanAndRegularStatus() {
        givenValidCareerSetup();
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponse result = studentService.createStudent(buildRequest());

        assertEquals(StudentStatus.REGULAR, result.getStatus());
        assertEquals("2021.3", result.getStudyPlanIdentifier());
        assertEquals(1L, result.getCareerId());
    }

    // RN-1.1: RUN is required
    @Test
    void createStudent_blankRun_throwsBadRequest() {
        StudentRequest request = buildRequest();
        request.setRun("");
        assertBadRequestOnCreate(request);
    }

    // RN-1.1: full name is required
    @Test
    void createStudent_blankFullName_throwsBadRequest() {
        StudentRequest request = buildRequest();
        request.setFullName("");
        assertBadRequestOnCreate(request);
    }

    // RN-1.1: email is required
    @Test
    void createStudent_blankEmail_throwsBadRequest() {
        StudentRequest request = buildRequest();
        request.setEmail("");
        assertBadRequestOnCreate(request);
    }

    // RN-1.1: career is required
    @Test
    void createStudent_nullCareer_throwsBadRequest() {
        StudentRequest request = buildRequest();
        request.setCareerId(null);
        assertBadRequestOnCreate(request);
    }

    // RN-1.4: career must exist -> 404
    @Test
    void createStudent_careerNotFound_throwsNotFound() {
        when(careerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> studentService.createStudent(buildRequest()));
        verify(studentRepository, never()).save(any());
    }

    // RN-2.5: INACTIVE career cannot receive new students
    @Test
    void createStudent_inactiveCareer_throwsConflict() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(buildCareer(1L, CareerStatus.INACTIVE)));
        assertConflictOnCreate(buildRequest());
    }

    // RN-1.2: RUN must be unique
    @Test
    void createStudent_duplicateRun_throwsConflict() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(buildCareer(1L, CareerStatus.ACTIVE)));
        when(studentRepository.existsByRun("12345678-9")).thenReturn(true);
        assertConflictOnCreate(buildRequest());
    }

    // RN-1.3: email must be unique
    @Test
    void createStudent_duplicateEmail_throwsConflict() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(buildCareer(1L, CareerStatus.ACTIVE)));
        when(studentRepository.existsByRun("12345678-9")).thenReturn(false);
        when(studentRepository.existsByEmail("juan.perez@ipt.cl")).thenReturn(true);
        assertConflictOnCreate(buildRequest());
    }

    // RN-1.1: career without a current plan -> 409
    @Test
    void createStudent_careerWithoutCurrentPlan_throwsConflict() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(buildCareer(1L, CareerStatus.ACTIVE)));
        when(studentRepository.existsByRun("12345678-9")).thenReturn(false);
        when(studentRepository.existsByEmail("juan.perez@ipt.cl")).thenReturn(false);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT)).thenReturn(Optional.empty());
        assertConflictOnCreate(buildRequest());
    }

    // ========== queries ==========

    @Test
    void getAllStudents_returnsList() {
        when(studentRepository.findAll()).thenReturn(List.of(buildStudent(1L, StudentStatus.REGULAR)));

        List<StudentResponse> result = studentService.getAllStudents();

        assertEquals(1, result.size());
        assertEquals("12345678-9", result.get(0).getRun());
    }

    @Test
    void getStudentById_existing_returnsStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));

        StudentResponse result = studentService.getStudentById(1L);

        assertEquals("Juan Pérez", result.getFullName());
    }

    @Test
    void getStudentById_notFound_throwsNotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> studentService.getStudentById(99L));
    }

    // ========== updateStudent ==========

    // RN-1.3 trap: same email -> must NOT check uniqueness
    @Test
    void updateStudent_sameEmail_updatesWithoutCheckingEmail() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponse result = studentService.updateStudent(1L,
                new StudentUpdateRequest("Juan Pérez Soto", "juan.perez@ipt.cl"));

        assertEquals("Juan Pérez Soto", result.getFullName());
        verify(studentRepository, never()).existsByEmail(any());
    }

    // RN-1.3: changing to an email already used -> 409
    @Test
    void updateStudent_newDuplicateEmail_throwsConflict() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));
        when(studentRepository.existsByEmail("ana@ipt.cl")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> studentService.updateStudent(1L, new StudentUpdateRequest("Juan Pérez", "ana@ipt.cl")));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateStudent_blankFullName_throwsBadRequest() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));

        assertThrows(BadRequestException.class,
                () -> studentService.updateStudent(1L, new StudentUpdateRequest("", "juan.perez@ipt.cl")));
    }

    @Test
    void updateStudent_blankEmail_throwsBadRequest() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));

        assertThrows(BadRequestException.class,
                () -> studentService.updateStudent(1L, new StudentUpdateRequest("Juan Pérez", "")));
    }

    // ========== changeStatus ==========

    // RN-1.9: admin can assign POSTPONED
    @Test
    void changeStatus_toPostponed_updatesStatus() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponse result = studentService.changeStatus(1L, StudentStatus.POSTPONED);

        assertEquals(StudentStatus.POSTPONED, result.getStatus());
    }

    @Test
    void changeStatus_nullStatus_throwsBadRequest() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));

        assertThrows(BadRequestException.class, () -> studentService.changeStatus(1L, null));
    }

    // RN-1.7: GRADUATED cannot be assigned manually -> 400
    @Test
    void changeStatus_toGraduated_throwsBadRequest() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));

        assertThrows(BadRequestException.class, () -> studentService.changeStatus(1L, StudentStatus.GRADUATED));
        verify(studentRepository, never()).save(any());
    }

    // RN-1.7: ELIMINATED cannot be assigned manually -> 400
    @Test
    void changeStatus_toEliminated_throwsBadRequest() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));

        assertThrows(BadRequestException.class, () -> studentService.changeStatus(1L, StudentStatus.ELIMINATED));
        verify(studentRepository, never()).save(any());
    }

    // RN-1.7: an ELIMINATED student is in a final status -> 409
    @Test
    void changeStatus_studentAlreadyEliminated_throwsConflict() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.ELIMINATED)));

        assertThrows(ConflictException.class, () -> studentService.changeStatus(1L, StudentStatus.REGULAR));
        verify(studentRepository, never()).save(any());
    }

    // ========== changeCareer ==========

    // RN-1.8 / RN-1.1: student without activity moves to the CURRENT plan of the new career
    @Test
    void changeCareer_valid_assignsCurrentPlanOfNewCareer() {
        Career newCareer = buildCareer(2L, CareerStatus.ACTIVE);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));
        when(careerRepository.findById(2L)).thenReturn(Optional.of(newCareer));
        when(studyPlanRepository.findByCareerIdAndStatus(2L, StudyPlanStatus.CURRENT))
                .thenReturn(Optional.of(buildPlan(2L, newCareer, "2022.3")));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponse result = studentService.changeCareer(1L, 2L);

        assertEquals(2L, result.getCareerId());
        assertEquals("2022.3", result.getStudyPlanIdentifier());
    }

    @Test
    void changeCareer_nullCareer_throwsBadRequest() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));

        assertThrows(BadRequestException.class, () -> studentService.changeCareer(1L, null));
    }

    // RN-2.5: cannot move a student into an INACTIVE career
    @Test
    void changeCareer_inactiveCareer_throwsConflict() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));
        when(careerRepository.findById(2L)).thenReturn(Optional.of(buildCareer(2L, CareerStatus.INACTIVE)));

        assertThrows(ConflictException.class, () -> studentService.changeCareer(1L, 2L));
        verify(studentRepository, never()).save(any());
    }

    // ========== deleteStudent ==========

    // RN-1.6: student without enrollments or record -> deleted
    @Test
    void deleteStudent_withoutActivity_deletesStudent() {
        Student student = buildStudent(1L, StudentStatus.REGULAR);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        studentService.deleteStudent(1L);

        verify(studentRepository).delete(student);
    }

    // RN-1.6: a student with enrollments cannot be deleted
    @Test
    void deleteStudent_withEnrollments_throwsConflict() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> studentService.deleteStudent(1L));
        verify(studentRepository, never()).delete(any());
    }

    // RN-1.8: a student with academic record cannot change career
    @Test
    void changeCareer_withAcademicRecord_throwsConflict() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));
        when(academicRecordRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> studentService.changeCareer(1L, 2L));
        verify(studentRepository, never()).save(any());
    }

    // Link between Keycloak (JWT email) and our Student
    @Test
    void getStudentIdByEmail_existing_returnsId() {
        when(studentRepository.findByEmail("juan.perez@ipt.cl")).thenReturn(Optional.of(buildStudent(1L, StudentStatus.REGULAR)));

        assertEquals(1L, studentService.getStudentIdByEmail("juan.perez@ipt.cl"));
    }

    @Test
    void getStudentIdByEmail_unknown_throwsNotFound() {
        when(studentRepository.findByEmail("nobody@ipt.cl")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> studentService.getStudentIdByEmail("nobody@ipt.cl"));
    }
}