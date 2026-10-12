package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.CareerRequest;
import com.ipt.siga_backend.entity.Career;
import com.ipt.siga_backend.entity.CareerStatus;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.CareerRepository;
import com.ipt.siga_backend.repository.StudyPlanRepository;
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
class CareerServiceTest {

    @Mock
    private CareerRepository careerRepository;

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @InjectMocks
    private CareerService careerService;

    // Helper: builds a sample career to use in tests
    private Career buildCareer(Long id, String code) {
        Career career = new Career();
        career.setId(id);
        career.setCode(code);
        career.setName("Técnico Analista Programador");
        career.setDescription("Two-year software career");
        career.setStatus(CareerStatus.ACTIVE);
        return career;
    }

    // ---------- createCareer ----------

    // RN-2.1 / RN-2.2: valid data -> career saved as ACTIVE
    @Test
    void createCareer_validData_savesWithActiveStatus() {
        CareerRequest request = new CareerRequest("10450", "Técnico Analista Programador", "Two-year software career");
        when(careerRepository.existsByCode("10450")).thenReturn(false);
        when(careerRepository.save(any(Career.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Career result = careerService.createCareer(request);

        assertEquals("10450", result.getCode());
        assertEquals(CareerStatus.ACTIVE, result.getStatus());
        verify(careerRepository).save(any(Career.class));
    }

    // RN-2.1: duplicate code -> 409, nothing saved
    @Test
    void createCareer_duplicateCode_throwsConflict() {
        CareerRequest request = new CareerRequest("10450", "Técnico Analista Programador", "Two-year software career");
        when(careerRepository.existsByCode("10450")).thenReturn(true);

        assertThrows(ConflictException.class, () -> careerService.createCareer(request));
        verify(careerRepository, never()).save(any());
    }

    // RN-2.2: blank code -> 400
    @Test
    void createCareer_blankCode_throwsBadRequest() {
        CareerRequest request = new CareerRequest("", "Técnico Analista Programador", "Two-year software career");

        assertThrows(BadRequestException.class, () -> careerService.createCareer(request));
        verify(careerRepository, never()).save(any());
    }

    // RN-2.2: blank name -> 400
    @Test
    void createCareer_blankName_throwsBadRequest() {
        CareerRequest request = new CareerRequest("10450", "", "Two-year software career");

        assertThrows(BadRequestException.class, () -> careerService.createCareer(request));
        verify(careerRepository, never()).save(any());
    }

    // RN-2.2: blank description -> 400
    @Test
    void createCareer_blankDescription_throwsBadRequest() {
        CareerRequest request = new CareerRequest("10450", "Técnico Analista Programador", "");

        assertThrows(BadRequestException.class, () -> careerService.createCareer(request));
        verify(careerRepository, never()).save(any());
    }

    // ---------- getAllCareers / getCareerById ----------

    @Test
    void getAllCareers_returnsList() {
        when(careerRepository.findAll()).thenReturn(List.of(buildCareer(1L, "10450")));

        List<Career> result = careerService.getAllCareers();

        assertEquals(1, result.size());
    }

    @Test
    void getCareerById_existing_returnsCareer() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(buildCareer(1L, "10450")));

        Career result = careerService.getCareerById(1L);

        assertEquals("10450", result.getCode());
    }

    @Test
    void getCareerById_notFound_throwsNotFound() {
        when(careerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> careerService.getCareerById(99L));
    }

    // ---------- updateCareer ----------

    // RN-2.1 trap: same code -> must NOT check uniqueness (it would find itself)
    @Test
    void updateCareer_sameCode_updatesWithoutCheckingCode() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(buildCareer(1L, "10450")));
        when(careerRepository.save(any(Career.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CareerRequest request = new CareerRequest("10450", "Técnico Analista Programador (nuevo)", "Updated description");

        Career result = careerService.updateCareer(1L, request);

        assertEquals("Técnico Analista Programador (nuevo)", result.getName());
        verify(careerRepository, never()).existsByCode(any());
    }

    // RN-2.1: changing to a code that already exists -> 409
    @Test
    void updateCareer_newDuplicateCode_throwsConflict() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(buildCareer(1L, "10450")));
        when(careerRepository.existsByCode("10451")).thenReturn(true);
        CareerRequest request = new CareerRequest("10451", "Técnico Analista Programador", "Two-year software career");

        assertThrows(ConflictException.class, () -> careerService.updateCareer(1L, request));
        verify(careerRepository, never()).save(any());
    }

    // ---------- changeStatus ----------

    // RN-2.3: ACTIVE -> INACTIVE
    @Test
    void changeStatus_valid_updatesStatus() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(buildCareer(1L, "10450")));
        when(careerRepository.save(any(Career.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Career result = careerService.changeStatus(1L, CareerStatus.INACTIVE);

        assertEquals(CareerStatus.INACTIVE, result.getStatus());
    }

    // RN-2.3: status is required -> 400
    @Test
    void changeStatus_nullStatus_throwsBadRequest() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(buildCareer(1L, "10450")));

        assertThrows(BadRequestException.class, () -> careerService.changeStatus(1L, null));
        verify(careerRepository, never()).save(any());
    }

    // ---------- deleteCareer ----------

    // RN-2.9: career without plans -> deleted
    @Test
    void deleteCareer_withoutPlans_deletesCareer() {
        Career career = buildCareer(1L, "10450");
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career));
        when(studyPlanRepository.existsByCareerId(1L)).thenReturn(false);

        careerService.deleteCareer(1L);

        verify(careerRepository).delete(career);
    }

    // RN-2.9: career with study plans -> 409, never deleted
    @Test
    void deleteCareer_withPlans_throwsConflict() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(buildCareer(1L, "10450")));
        when(studyPlanRepository.existsByCareerId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> careerService.deleteCareer(1L));
        verify(careerRepository, never()).delete(any());
    }
}