package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.AcademicPeriodRequest;
import com.ipt.siga_backend.entity.AcademicPeriod;
import com.ipt.siga_backend.entity.PeriodStatus;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.AcademicPeriodRepository;
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
class AcademicPeriodServiceTest {

    @Mock
    private AcademicPeriodRepository periodRepository;

    @InjectMocks
    private AcademicPeriodService periodService;

    private void assertBadRequestOnCreate(AcademicPeriodRequest request) {
        assertThrows(BadRequestException.class, () -> periodService.createPeriod(request));
        verify(periodRepository, never()).save(any());
    }

    // RN-5.1 / RN-5.2: valid period -> OPEN, code "2027-1"
    @Test
    void createPeriod_valid_savesAsOpen() {
        when(periodRepository.existsByYearAndSemester(2027, 1)).thenReturn(false);
        when(periodRepository.save(any(AcademicPeriod.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AcademicPeriod result = periodService.createPeriod(new AcademicPeriodRequest(2027, 1));

        assertEquals(PeriodStatus.OPEN, result.getStatus());
        assertEquals("2027-1", result.getCode());
    }

    @Test
    void createPeriod_nullYear_throwsBadRequest() {
        assertBadRequestOnCreate(new AcademicPeriodRequest(null, 1));
    }

    @Test
    void createPeriod_yearBefore2000_throwsBadRequest() {
        assertBadRequestOnCreate(new AcademicPeriodRequest(1999, 1));
    }

    // RN-5.1: semester must be 1 or 2
    @Test
    void createPeriod_semesterThree_throwsBadRequest() {
        assertBadRequestOnCreate(new AcademicPeriodRequest(2027, 3));
    }

    // RN-5.1: duplicate period -> 409
    @Test
    void createPeriod_duplicate_throwsConflict() {
        when(periodRepository.existsByYearAndSemester(2027, 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> periodService.createPeriod(new AcademicPeriodRequest(2027, 1)));
        verify(periodRepository, never()).save(any());
    }

    @Test
    void getAllPeriods_returnsList() {
        when(periodRepository.findAll()).thenReturn(List.of(new AcademicPeriod()));

        assertEquals(1, periodService.getAllPeriods().size());
    }

    @Test
    void getPeriodById_notFound_throwsNotFound() {
        when(periodRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> periodService.getPeriodById(99L));
    }
}