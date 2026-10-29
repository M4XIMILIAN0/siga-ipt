package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.SubjectRequest;
import com.ipt.siga_backend.dto.SubjectResponse;
import com.ipt.siga_backend.entity.StudyPlan;
import com.ipt.siga_backend.entity.Subject;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.AcademicRecordRepository;
import com.ipt.siga_backend.repository.SectionRepository;
import com.ipt.siga_backend.repository.StudyPlanRepository;
import com.ipt.siga_backend.repository.SubjectRepository;
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
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private AcademicRecordRepository academicRecordRepository;

    @InjectMocks
    private SubjectService subjectService;

    // ========== helpers ==========

    private StudyPlan buildPlan() {
        StudyPlan plan = new StudyPlan();
        plan.setId(1L);
        plan.setIdentifier("2021.3");
        return plan;
    }

    // A saved subject with TEL 2-2-2 and SCT 6
    private Subject buildSubject(Long id, String code, int semester) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setCode(code);
        subject.setName("Subject " + code);
        subject.setSemester(semester);
        subject.setTheoryHours(2);
        subject.setExerciseHours(2);
        subject.setLabHours(2);
        subject.setSctCredits(6);
        subject.setStudyPlan(buildPlan());
        return subject;
    }

    // A VALID request (TEL 2-2-2, SCT 6). Each test breaks only ONE field.
    private SubjectRequest buildRequest(String code, int semester, List<String> prerequisiteCodes) {
        return new SubjectRequest(code, "Subject " + code, semester, 2, 2, 2, 6, prerequisiteCodes);
    }

    private void givenPlanExists() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(buildPlan()));
    }

    // Shared assertion for all "400" tests on create
    private void assertBadRequestOnCreate(SubjectRequest request) {
        assertThrows(BadRequestException.class, () -> subjectService.createSubject(1L, request));
        verify(subjectRepository, never()).save(any());
    }

    // ========== createSubject: success ==========

    // RN-3.1: valid subject without prerequisites -> created
    @Test
    void createSubject_validWithoutPrerequisites_returnsResponse() {
        givenPlanExists();
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP101")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubjectResponse result = subjectService.createSubject(1L, buildRequest("TAP101", 1, List.of()));

        assertEquals("TAP101", result.getCode());
        assertEquals(6, result.getTelTotal());
        assertTrue(result.getPrerequisiteCodes().isEmpty());
    }

    // RN-3.8 / RN-3.9: prerequisite from the same plan and an earlier semester -> linked
    @Test
    void createSubject_withValidPrerequisite_linksPrerequisite() {
        givenPlanExists();
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP201")).thenReturn(false);
        when(subjectRepository.findByStudyPlanIdAndCode(1L, "TAP101"))
                .thenReturn(Optional.of(buildSubject(10L, "TAP101", 1)));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubjectResponse result = subjectService.createSubject(1L, buildRequest("TAP201", 2, List.of("TAP101")));

        assertEquals(List.of("TAP101"), result.getPrerequisiteCodes());
    }

    // ========== createSubject: rejections ==========

    // RN-3.1: study plan must exist -> 404
    @Test
    void createSubject_studyPlanNotFound_throwsNotFound() {
        when(studyPlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> subjectService.createSubject(99L, buildRequest("TAP101", 1, List.of())));
        verify(subjectRepository, never()).save(any());
    }

    // RN-3.1: code is required
    @Test
    void createSubject_blankCode_throwsBadRequest() {
        givenPlanExists();
        assertBadRequestOnCreate(buildRequest("", 1, List.of()));
    }

    // RN-3.1: name is required
    @Test
    void createSubject_blankName_throwsBadRequest() {
        givenPlanExists();
        SubjectRequest request = buildRequest("TAP101", 1, List.of());
        request.setName("");
        assertBadRequestOnCreate(request);
    }

    // RN-3.1: numeric fields are required
    @Test
    void createSubject_missingSct_throwsBadRequest() {
        givenPlanExists();
        SubjectRequest request = buildRequest("TAP101", 1, List.of());
        request.setSctCredits(null);
        assertBadRequestOnCreate(request);
    }

    // RN-3.3: semester must be between 1 and 4
    @Test
    void createSubject_semesterOutOfRange_throwsBadRequest() {
        givenPlanExists();
        assertBadRequestOnCreate(buildRequest("TAP101", 5, List.of()));
    }

    // RN-3.4: SCT must be at most 7
    @Test
    void createSubject_sctTooHigh_throwsBadRequest() {
        givenPlanExists();
        SubjectRequest request = buildRequest("TAP101", 1, List.of());
        request.setSctCredits(8);
        assertBadRequestOnCreate(request);
    }

    // RN-3.5: hours cannot be negative
    @Test
    void createSubject_negativeHours_throwsBadRequest() {
        givenPlanExists();
        SubjectRequest request = buildRequest("TAP101", 1, List.of());
        request.setLabHours(-1);
        assertBadRequestOnCreate(request);
    }

    // RN-3.6: total weekly hours must be LESS than 8 (4+2+2 = 8 is rejected)
    @Test
    void createSubject_totalHoursEight_throwsBadRequest() {
        givenPlanExists();
        SubjectRequest request = buildRequest("TAP101", 1, List.of());
        request.setTheoryHours(4);
        assertBadRequestOnCreate(request);
    }

    // RN-3.2: code must be unique within the plan -> 409
    @Test
    void createSubject_duplicateCode_throwsConflict() {
        givenPlanExists();
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP101")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> subjectService.createSubject(1L, buildRequest("TAP101", 1, List.of())));
        verify(subjectRepository, never()).save(any());
    }

    // RN-3.7: at most 3 prerequisites
    @Test
    void createSubject_moreThanThreePrerequisites_throwsBadRequest() {
        givenPlanExists();
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP401")).thenReturn(false);
        assertBadRequestOnCreate(buildRequest("TAP401", 4, List.of("TAP301", "TAP302", "TAP303", "TAP304")));
    }

    // RN-3.7: no duplicated prerequisites
    @Test
    void createSubject_duplicatedPrerequisites_throwsBadRequest() {
        givenPlanExists();
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP201")).thenReturn(false);
        assertBadRequestOnCreate(buildRequest("TAP201", 2, List.of("TAP101", "TAP101")));
    }

    // RN-3.8: prerequisite must exist in the same plan
    @Test
    void createSubject_prerequisiteNotInPlan_throwsBadRequest() {
        givenPlanExists();
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP201")).thenReturn(false);
        when(subjectRepository.findByStudyPlanIdAndCode(1L, "TCS101")).thenReturn(Optional.empty());
        assertBadRequestOnCreate(buildRequest("TAP201", 2, List.of("TCS101")));
    }

    // RN-3.9: prerequisite must be from an EARLIER semester (same semester is rejected)
    @Test
    void createSubject_prerequisiteSameSemester_throwsBadRequest() {
        givenPlanExists();
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP102")).thenReturn(false);
        when(subjectRepository.findByStudyPlanIdAndCode(1L, "TAP101"))
                .thenReturn(Optional.of(buildSubject(10L, "TAP101", 1)));
        assertBadRequestOnCreate(buildRequest("TAP102", 1, List.of("TAP101")));
    }

    // ========== queries ==========

    @Test
    void getSubjectsByPlan_returnsResponses() {
        givenPlanExists();
        when(subjectRepository.findByStudyPlanId(1L)).thenReturn(List.of(buildSubject(10L, "TAP101", 1)));

        List<SubjectResponse> result = subjectService.getSubjectsByPlan(1L);

        assertEquals(1, result.size());
        assertEquals("TAP101", result.get(0).getCode());
    }

    @Test
    void getSubjectById_existing_returnsResponse() {
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(buildSubject(10L, "TAP101", 1)));

        SubjectResponse result = subjectService.getSubjectById(10L);

        assertEquals("TAP101", result.getCode());
    }

    @Test
    void getSubjectById_notFound_throwsNotFound() {
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> subjectService.getSubjectById(99L));
    }

    // ========== updateSubject ==========

    // RN-3.2 trap: same code -> must NOT check uniqueness
    @Test
    void updateSubject_sameCode_updatesWithoutCheckingCode() {
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(buildSubject(10L, "TAP101", 1)));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SubjectRequest request = buildRequest("TAP101", 1, List.of());
        request.setName("Programming Fundamentals");

        SubjectResponse result = subjectService.updateSubject(10L, request);

        assertEquals("Programming Fundamentals", result.getName());
        verify(subjectRepository, never()).existsByStudyPlanIdAndCode(any(), any());
    }

    // RN-3.2: changing to a code already used in the plan -> 409
    @Test
    void updateSubject_newDuplicateCode_throwsConflict() {
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(buildSubject(10L, "TAP101", 1)));
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP102")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> subjectService.updateSubject(10L, buildRequest("TAP102", 1, List.of())));
        verify(subjectRepository, never()).save(any());
    }

    // ========== deleteSubject ==========

    @Test
    void deleteSubject_notPrerequisite_deletesSubject() {
        Subject subject = buildSubject(10L, "TAP101", 1);
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject));
        when(subjectRepository.existsByPrerequisitesId(10L)).thenReturn(false);

        subjectService.deleteSubject(10L);

        verify(subjectRepository).delete(subject);
    }

    // A subject required by others cannot be deleted -> 409
    @Test
    void deleteSubject_isPrerequisiteOfOthers_throwsConflict() {
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(buildSubject(10L, "TAP101", 1)));
        when(subjectRepository.existsByPrerequisitesId(10L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> subjectService.deleteSubject(10L));
        verify(subjectRepository, never()).delete(any());
    }

    // A subject with sections cannot be deleted -> 409
    @Test
    void deleteSubject_withSections_throwsConflict() {
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(buildSubject(10L, "TAP101", 1)));
        when(subjectRepository.existsByPrerequisitesId(10L)).thenReturn(false);
        when(sectionRepository.existsBySubjectId(10L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> subjectService.deleteSubject(10L));
        verify(subjectRepository, never()).delete(any());
    }

    // RN-3.10: subject in an academic record cannot be deleted
    @Test
    void deleteSubject_inAcademicRecord_throwsConflict() {
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(buildSubject(10L, "TAP101", 1)));
        when(academicRecordRepository.existsBySubjectId(10L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> subjectService.deleteSubject(10L));
        verify(subjectRepository, never()).delete(any());
    }
}