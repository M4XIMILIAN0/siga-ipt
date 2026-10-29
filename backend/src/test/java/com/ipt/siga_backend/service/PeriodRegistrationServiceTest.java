package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.PeriodRegistrationResponse;
import com.ipt.siga_backend.entity.*;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.AcademicPeriodRepository;
import com.ipt.siga_backend.repository.PeriodRegistrationRepository;
import com.ipt.siga_backend.repository.StudentRepository;
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
class PeriodRegistrationServiceTest {

    @Mock
    private PeriodRegistrationRepository registrationRepository;

    @Mock
    private AcademicPeriodRepository periodRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private PeriodRegistrationService registrationService;

    private AcademicPeriod buildPeriod(PeriodStatus status) {
        AcademicPeriod period = new AcademicPeriod();
        period.setId(1L);
        period.setYear(2027);
        period.setSemester(1);
        period.setStatus(status);
        return period;
    }

    private Student buildStudent() {
        Student student = new Student();
        student.setId(3L);
        student.setRun("12345678-9");
        student.setFullName("Pedro Soto");
        return student;
    }

    @Test
    void registerStudent_valid_savesRegistration() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        when(studentRepository.findById(3L)).thenReturn(Optional.of(buildStudent()));
        when(registrationRepository.existsByStudentIdAndPeriodId(3L, 1L)).thenReturn(false);
        when(registrationRepository.save(any(PeriodRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PeriodRegistrationResponse result = registrationService.registerStudent(1L, 3L);

        assertEquals("2027-1", result.getPeriodCode());
        assertEquals("Pedro Soto", result.getStudentName());
    }

    @Test
    void registerStudent_nullStudent_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> registrationService.registerStudent(1L, null));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerStudent_periodNotFound_throwsNotFound() {
        when(periodRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> registrationService.registerStudent(1L, 3L));
    }

    @Test
    void registerStudent_studentNotFound_throwsNotFound() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        when(studentRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> registrationService.registerStudent(1L, 3L));
    }

    @Test
    void registerStudent_closedPeriod_throwsConflict() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.CLOSED)));
        when(studentRepository.findById(3L)).thenReturn(Optional.of(buildStudent()));

        assertThrows(ConflictException.class, () -> registrationService.registerStudent(1L, 3L));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerStudent_alreadyRegistered_throwsConflict() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        when(studentRepository.findById(3L)).thenReturn(Optional.of(buildStudent()));
        when(registrationRepository.existsByStudentIdAndPeriodId(3L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> registrationService.registerStudent(1L, 3L));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void getRegistrationsByPeriod_returnsList() {
        PeriodRegistration registration = new PeriodRegistration(5L, buildStudent(), buildPeriod(PeriodStatus.OPEN));
        when(periodRepository.existsById(1L)).thenReturn(true);
        when(registrationRepository.findByPeriodId(1L)).thenReturn(List.of(registration));

        assertEquals(1, registrationService.getRegistrationsByPeriod(1L).size());
    }

    @Test
    void getRegistrationsByPeriod_periodNotFound_throwsNotFound() {
        when(periodRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> registrationService.getRegistrationsByPeriod(99L));
    }
}