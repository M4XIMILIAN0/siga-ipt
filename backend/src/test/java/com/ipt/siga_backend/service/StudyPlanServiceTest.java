package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.StudyPlanRequest;
import com.ipt.siga_backend.entity.Career;
import com.ipt.siga_backend.entity.StudyPlan;
import com.ipt.siga_backend.entity.StudyPlanStatus;
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
class StudyPlanServiceTest {

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @Mock
    private CareerRepository careerRepository;

    @InjectMocks
    private StudyPlanService studyPlanService;

    // Helper: builds a sample career
    private Career buildCareer(Long id) {
        Career career = new Career();
        career.setId(id);
        career.setCode("10450");
        return career;
    }

    // ---------- createStudyPlan ----------

    // RN-2.10: new plan becomes CURRENT and the old one becomes NOT_CURRENT
    @Test
    void createStudyPlan_withCurrentPlan_marksOldPlanAsNotCurrent() {
        // Arrange
        Career career = buildCareer(1L);

        StudyPlan oldPlan = new StudyPlan();
        oldPlan.setIdentifier("2021.3");
        oldPlan.setStatus(StudyPlanStatus.CURRENT);
        oldPlan.setCareer(career);

        when(careerRepository.findById(1L)).thenReturn(Optional.of(career));
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2025.1")).thenReturn(false);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT)).thenReturn(Optional.of(oldPlan));
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        StudyPlan result = studyPlanService.createStudyPlan(1L, new StudyPlanRequest("2025.1"));

        // Assert
        assertEquals(StudyPlanStatus.NOT_CURRENT, oldPlan.getStatus());   // old plan retired
        assertEquals(StudyPlanStatus.CURRENT, result.getStatus());        // new plan is current
        verify(studyPlanRepository, times(2)).save(any(StudyPlan.class)); // UPDATE old + INSERT new
    }

    // RN-2.10: first plan of a career -> created as CURRENT, nothing to retire
    @Test
    void createStudyPlan_withoutCurrentPlan_createsFirstPlan() {
        // Arrange
        Career career = buildCareer(1L);
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career));
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2021.3")).thenReturn(false);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT)).thenReturn(Optional.empty());
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        StudyPlan result = studyPlanService.createStudyPlan(1L, new StudyPlanRequest("2021.3"));

        // Assert
        assertEquals("2021.3", result.getIdentifier());
        assertEquals(StudyPlanStatus.CURRENT, result.getStatus());
        assertEquals(career, result.getCareer());                         // linked to its career
        verify(studyPlanRepository, times(1)).save(any(StudyPlan.class)); // only the new plan
    }

    // Career does not exist -> 404, nothing saved
    @Test
    void createStudyPlan_careerNotFound_throwsNotFound() {
        // Arrange
        when(careerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(NotFoundException.class,
                () -> studyPlanService.createStudyPlan(99L, new StudyPlanRequest("2021.3")));
        verify(studyPlanRepository, never()).save(any());
    }

    // Identifier is required -> 400, nothing saved
    @Test
    void createStudyPlan_blankIdentifier_throwsBadRequest() {
        // Arrange (findById runs first in the service, so we must stub it)
        when(careerRepository.findById(1L)).thenReturn(Optional.of(buildCareer(1L)));

        // Act + Assert
        assertThrows(BadRequestException.class,
                () -> studyPlanService.createStudyPlan(1L, new StudyPlanRequest("")));
        verify(studyPlanRepository, never()).save(any());
    }

    // RN-2.6: duplicate identifier in the same career -> 409, old plan untouched
    @Test
    void createStudyPlan_duplicateIdentifier_throwsConflict() {
        // Arrange
        when(careerRepository.findById(1L)).thenReturn(Optional.of(buildCareer(1L)));
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2021.3")).thenReturn(true);

        // Act + Assert
        assertThrows(ConflictException.class,
                () -> studyPlanService.createStudyPlan(1L, new StudyPlanRequest("2021.3")));
        verify(studyPlanRepository, never()).findByCareerIdAndStatus(any(), any()); // never reached RN-2.10
        verify(studyPlanRepository, never()).save(any());
    }

    // ---------- getPlansByCareer ----------

    @Test
    void getPlansByCareer_existingCareer_returnsPlans() {
        // Arrange
        StudyPlan plan = new StudyPlan();
        plan.setIdentifier("2021.3");
        when(careerRepository.findById(1L)).thenReturn(Optional.of(buildCareer(1L)));
        when(studyPlanRepository.findByCareerId(1L)).thenReturn(List.of(plan));

        // Act
        List<StudyPlan> result = studyPlanService.getPlansByCareer(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals("2021.3", result.get(0).getIdentifier());
    }

    @Test
    void getPlansByCareer_careerNotFound_throwsNotFound() {
        // Arrange
        when(careerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(NotFoundException.class, () -> studyPlanService.getPlansByCareer(99L));
        verify(studyPlanRepository, never()).findByCareerId(any());
    }
}