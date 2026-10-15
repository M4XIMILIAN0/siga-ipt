package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.TeacherRequest;
import com.ipt.siga_backend.dto.TeacherUpdateRequest;
import com.ipt.siga_backend.entity.Teacher;
import com.ipt.siga_backend.entity.TeacherStatus;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.TeacherRepository;
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
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private TeacherService teacherService;

    // ========== helpers ==========

    // A VALID create request. Each test breaks only ONE field.
    private TeacherRequest buildRequest() {
        return new TeacherRequest("15432876-5", "María González", "maria.gonzalez@ipt.cl",
                "Ingeniera Civil en Informática", "Magíster en Ingeniería de Software");
    }

    // A VALID update request (same email as the saved teacher)
    private TeacherUpdateRequest buildUpdateRequest() {
        return new TeacherUpdateRequest("María González Rojas", "maria.gonzalez@ipt.cl",
                "Ingeniera Civil en Informática", "Doctora en Ciencias de la Computación");
    }

    // A saved teacher
    private Teacher buildTeacher(Long id) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setRun("15432876-5");
        teacher.setFullName("María González");
        teacher.setEmail("maria.gonzalez@ipt.cl");
        teacher.setProfessionalTitle("Ingeniera Civil en Informática");
        teacher.setAcademicDegree("Magíster en Ingeniería de Software");
        teacher.setStatus(TeacherStatus.ACTIVE);
        return teacher;
    }

    private void assertBadRequestOnCreate(TeacherRequest request) {
        assertThrows(BadRequestException.class, () -> teacherService.createTeacher(request));
        verify(teacherRepository, never()).save(any());
    }

    private void assertBadRequestOnUpdate(TeacherUpdateRequest request) {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(buildTeacher(1L)));
        assertThrows(BadRequestException.class, () -> teacherService.updateTeacher(1L, request));
        verify(teacherRepository, never()).save(any());
    }

    // ========== createTeacher ==========

    // RN-4.4: valid data -> teacher saved as ACTIVE
    @Test
    void createTeacher_valid_savesAsActive() {
        when(teacherRepository.existsByRun("15432876-5")).thenReturn(false);
        when(teacherRepository.existsByEmail("maria.gonzalez@ipt.cl")).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Teacher result = teacherService.createTeacher(buildRequest());

        assertEquals(TeacherStatus.ACTIVE, result.getStatus());
        assertEquals("Magíster en Ingeniería de Software", result.getAcademicDegree());
    }

    // RN-4.3: each required field
    @Test
    void createTeacher_blankRun_throwsBadRequest() {
        TeacherRequest request = buildRequest();
        request.setRun("");
        assertBadRequestOnCreate(request);
    }

    @Test
    void createTeacher_blankFullName_throwsBadRequest() {
        TeacherRequest request = buildRequest();
        request.setFullName("");
        assertBadRequestOnCreate(request);
    }

    @Test
    void createTeacher_blankEmail_throwsBadRequest() {
        TeacherRequest request = buildRequest();
        request.setEmail("");
        assertBadRequestOnCreate(request);
    }

    @Test
    void createTeacher_blankProfessionalTitle_throwsBadRequest() {
        TeacherRequest request = buildRequest();
        request.setProfessionalTitle("");
        assertBadRequestOnCreate(request);
    }

    @Test
    void createTeacher_blankAcademicDegree_throwsBadRequest() {
        TeacherRequest request = buildRequest();
        request.setAcademicDegree("");
        assertBadRequestOnCreate(request);
    }

    // RN-4.1: RUN must be unique
    @Test
    void createTeacher_duplicateRun_throwsConflict() {
        when(teacherRepository.existsByRun("15432876-5")).thenReturn(true);

        assertThrows(ConflictException.class, () -> teacherService.createTeacher(buildRequest()));
        verify(teacherRepository, never()).save(any());
    }

    // RN-4.2: email must be unique
    @Test
    void createTeacher_duplicateEmail_throwsConflict() {
        when(teacherRepository.existsByRun("15432876-5")).thenReturn(false);
        when(teacherRepository.existsByEmail("maria.gonzalez@ipt.cl")).thenReturn(true);

        assertThrows(ConflictException.class, () -> teacherService.createTeacher(buildRequest()));
        verify(teacherRepository, never()).save(any());
    }

    // ========== queries ==========

    @Test
    void getAllTeachers_returnsList() {
        when(teacherRepository.findAll()).thenReturn(List.of(buildTeacher(1L)));

        assertEquals(1, teacherService.getAllTeachers().size());
    }

    @Test
    void getTeacherById_existing_returnsTeacher() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(buildTeacher(1L)));

        assertEquals("María González", teacherService.getTeacherById(1L).getFullName());
    }

    @Test
    void getTeacherById_notFound_throwsNotFound() {
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> teacherService.getTeacherById(99L));
    }

    // ========== updateTeacher ==========

    // RN-4.2 trap: same email -> must NOT check uniqueness
    @Test
    void updateTeacher_sameEmail_updatesWithoutCheckingEmail() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(buildTeacher(1L)));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Teacher result = teacherService.updateTeacher(1L, buildUpdateRequest());

        assertEquals("Doctora en Ciencias de la Computación", result.getAcademicDegree());
        verify(teacherRepository, never()).existsByEmail(any());
    }

    // RN-4.2: changing to an email already used -> 409
    @Test
    void updateTeacher_newDuplicateEmail_throwsConflict() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(buildTeacher(1L)));
        when(teacherRepository.existsByEmail("pedro@ipt.cl")).thenReturn(true);
        TeacherUpdateRequest request = buildUpdateRequest();
        request.setEmail("pedro@ipt.cl");

        assertThrows(ConflictException.class, () -> teacherService.updateTeacher(1L, request));
        verify(teacherRepository, never()).save(any());
    }

    // RN-4.3 on update: each required field
    @Test
    void updateTeacher_blankFullName_throwsBadRequest() {
        TeacherUpdateRequest request = buildUpdateRequest();
        request.setFullName("");
        assertBadRequestOnUpdate(request);
    }

    @Test
    void updateTeacher_blankEmail_throwsBadRequest() {
        TeacherUpdateRequest request = buildUpdateRequest();
        request.setEmail("");
        assertBadRequestOnUpdate(request);
    }

    @Test
    void updateTeacher_blankProfessionalTitle_throwsBadRequest() {
        TeacherUpdateRequest request = buildUpdateRequest();
        request.setProfessionalTitle("");
        assertBadRequestOnUpdate(request);
    }

    @Test
    void updateTeacher_blankAcademicDegree_throwsBadRequest() {
        TeacherUpdateRequest request = buildUpdateRequest();
        request.setAcademicDegree("");
        assertBadRequestOnUpdate(request);
    }

    // ========== changeStatus ==========

    // RN-4.7 / RN-4.8: without open sections -> can become INACTIVE
    @Test
    void changeStatus_toInactive_updatesStatus() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(buildTeacher(1L)));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Teacher result = teacherService.changeStatus(1L, TeacherStatus.INACTIVE);

        assertEquals(TeacherStatus.INACTIVE, result.getStatus());
    }

    @Test
    void changeStatus_nullStatus_throwsBadRequest() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(buildTeacher(1L)));

        assertThrows(BadRequestException.class, () -> teacherService.changeStatus(1L, null));
        verify(teacherRepository, never()).save(any());
    }

    // ========== deleteTeacher ==========

    // RN-4.6: teacher without sections -> deleted
    @Test
    void deleteTeacher_withoutSections_deletesTeacher() {
        Teacher teacher = buildTeacher(1L);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher));

        teacherService.deleteTeacher(1L);

        verify(teacherRepository).delete(teacher);
    }
}