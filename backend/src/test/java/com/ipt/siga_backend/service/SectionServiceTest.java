package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.ScheduleBlockRequest;
import com.ipt.siga_backend.dto.SectionRequest;
import com.ipt.siga_backend.dto.SectionResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private ScheduleBlockRepository scheduleBlockRepository;

    @Mock
    private AcademicPeriodRepository periodRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private SectionService sectionService;

    // ========== helpers ==========

    private AcademicPeriod buildPeriod(PeriodStatus status) {
        AcademicPeriod period = new AcademicPeriod();
        period.setId(1L);
        period.setYear(2027);
        period.setSemester(1);
        period.setStatus(status);
        return period;
    }

    // TAP101: TEL 2-2-2 = 6 hours -> needs 3 blocks
    private Subject buildSubject() {
        Subject subject = new Subject();
        subject.setId(1L);
        subject.setCode("TAP101");
        subject.setName("Fundamentos de Programación");
        subject.setSemester(1);
        subject.setTheoryHours(2);
        subject.setExerciseHours(2);
        subject.setLabHours(2);
        return subject;
    }

    private Teacher buildTeacher(TeacherStatus status) {
        Teacher teacher = new Teacher();
        teacher.setId(7L);
        teacher.setFullName("María González");
        teacher.setStatus(status);
        return teacher;
    }

    private ScheduleBlockRequest block(ScheduleDay day, Integer moduleNumber) {
        return new ScheduleBlockRequest(day, moduleNumber);
    }

    // A VALID request for TAP101: 3 blocks L2, W3, V1. Each test breaks only ONE thing.
    private SectionRequest buildRequest() {
        List<ScheduleBlockRequest> blocks = new ArrayList<>();
        blocks.add(block(ScheduleDay.L, 2));
        blocks.add(block(ScheduleDay.W, 3));
        blocks.add(block(ScheduleDay.V, 1));
        return new SectionRequest(1L, 7L, 40, blocks);
    }

    // OPEN period, TAP101 exists, María exists and is ACTIVE
    private void givenValidSetup() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(buildSubject()));
        when(teacherRepository.findById(7L)).thenReturn(Optional.of(buildTeacher(TeacherStatus.ACTIVE)));
    }

    // A saved section (for queries and delete)
    private Section buildSection() {
        Section section = new Section();
        section.setId(5L);
        section.setCapacity(40);
        section.setPeriod(buildPeriod(PeriodStatus.OPEN));
        section.setSubject(buildSubject());
        section.setTeacher(buildTeacher(TeacherStatus.ACTIVE));
        ScheduleBlock scheduleBlock = new ScheduleBlock();
        scheduleBlock.setDay(ScheduleDay.L);
        scheduleBlock.setModuleNumber(2);
        scheduleBlock.setSection(section);
        section.getBlocks().add(scheduleBlock);
        return section;
    }

    private void assertBadRequestOnCreate(SectionRequest request) {
        assertThrows(BadRequestException.class, () -> sectionService.createSection(1L, request));
        verify(sectionRepository, never()).save(any());
    }

    private void assertConflictOnCreate(SectionRequest request) {
        assertThrows(ConflictException.class, () -> sectionService.createSection(1L, request));
        verify(sectionRepository, never()).save(any());
    }

    // ========== createSection: success ==========

    // RN-5.3 / RN-5.8 / RN-5.9: valid section with its 3 blocks, saved with ONE save
    @Test
    void createSection_valid_savesSectionWithBlocks() {
        givenValidSetup();
        when(sectionRepository.existsBySubjectIdAndPeriodId(1L, 1L)).thenReturn(false);
        when(scheduleBlockRepository.existsBySectionTeacherIdAndSectionPeriodIdAndDayAndModuleNumber(
                any(), any(), any(), any())).thenReturn(false);
        when(sectionRepository.save(any(Section.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SectionResponse result = sectionService.createSection(1L, buildRequest());

        assertEquals(List.of("L2", "W3", "V1"), result.getBlocks());
        assertEquals("2027-1", result.getPeriodCode());
        assertEquals("TAP101", result.getSubjectCode());
        verify(sectionRepository, times(1)).save(any(Section.class));   // cascade saves the blocks
    }

    // ========== createSection: 404 ==========

    @Test
    void createSection_periodNotFound_throwsNotFound() {
        when(periodRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sectionService.createSection(1L, buildRequest()));
    }

    // RN-5.3: subject must exist
    @Test
    void createSection_subjectNotFound_throwsNotFound() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        when(subjectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sectionService.createSection(1L, buildRequest()));
    }

    @Test
    void createSection_teacherNotFound_throwsNotFound() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(buildSubject()));
        when(teacherRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sectionService.createSection(1L, buildRequest()));
    }

    // ========== createSection: 409 ==========

    // Decision: no new sections in a CLOSED period
    @Test
    void createSection_closedPeriod_throwsConflict() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.CLOSED)));
        assertConflictOnCreate(buildRequest());
    }

    // RN-5.5: teacher must be ACTIVE
    @Test
    void createSection_inactiveTeacher_throwsConflict() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(buildSubject()));
        when(teacherRepository.findById(7L)).thenReturn(Optional.of(buildTeacher(TeacherStatus.INACTIVE)));
        assertConflictOnCreate(buildRequest());
    }

    // RN-5.4: only one section per subject in the same period
    @Test
    void createSection_subjectAlreadyHasSection_throwsConflict() {
        givenValidSetup();
        when(sectionRepository.existsBySubjectIdAndPeriodId(1L, 1L)).thenReturn(true);
        assertConflictOnCreate(buildRequest());
    }

    // RN-5.11: the teacher is already busy on L2 in this period
    @Test
    void createSection_teacherScheduleConflict_throwsConflict() {
        givenValidSetup();
        when(sectionRepository.existsBySubjectIdAndPeriodId(1L, 1L)).thenReturn(false);
        when(scheduleBlockRepository.existsBySectionTeacherIdAndSectionPeriodIdAndDayAndModuleNumber(
                7L, 1L, ScheduleDay.L, 2)).thenReturn(true);
        assertConflictOnCreate(buildRequest());
    }

    // ========== createSection: 400 ==========

    @Test
    void createSection_nullCapacity_throwsBadRequest() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        SectionRequest request = buildRequest();
        request.setCapacity(null);
        assertBadRequestOnCreate(request);
    }

    // RN-5.6: capacity must be greater than 0
    @Test
    void createSection_capacityZero_throwsBadRequest() {
        givenValidSetup();
        SectionRequest request = buildRequest();
        request.setCapacity(0);
        assertBadRequestOnCreate(request);
    }

    // RN-5.6: capacity must be at most 60
    @Test
    void createSection_capacityAbove60_throwsBadRequest() {
        givenValidSetup();
        SectionRequest request = buildRequest();
        request.setCapacity(61);
        assertBadRequestOnCreate(request);
    }

    // RN-5.8: at least one block
    @Test
    void createSection_noBlocks_throwsBadRequest() {
        givenValidSetup();
        SectionRequest request = buildRequest();
        request.setBlocks(new ArrayList<>());
        assertBadRequestOnCreate(request);
    }

    // RN-5.8: module between 1 and 6
    @Test
    void createSection_moduleSeven_throwsBadRequest() {
        givenValidSetup();
        SectionRequest request = buildRequest();
        request.getBlocks().set(0, block(ScheduleDay.L, 7));
        assertBadRequestOnCreate(request);
    }

    // RN-5.8: each block needs a day
    @Test
    void createSection_blockWithoutDay_throwsBadRequest() {
        givenValidSetup();
        SectionRequest request = buildRequest();
        request.getBlocks().set(0, block(null, 2));
        assertBadRequestOnCreate(request);
    }

    // RN-5.10: no duplicated blocks (L2 twice)
    @Test
    void createSection_duplicatedBlocks_throwsBadRequest() {
        givenValidSetup();
        SectionRequest request = buildRequest();
        request.getBlocks().set(1, block(ScheduleDay.L, 2));
        assertBadRequestOnCreate(request);
    }

    // RN-5.9: TAP101 has 6 hours -> needs exactly 3 blocks, 2 are rejected
    @Test
    void createSection_wrongBlockCount_throwsBadRequest() {
        givenValidSetup();
        SectionRequest request = buildRequest();
        request.getBlocks().remove(2);
        assertBadRequestOnCreate(request);
    }

    // ========== queries ==========

    @Test
    void getSectionsByPeriod_returnsOffer() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(buildPeriod(PeriodStatus.OPEN)));
        when(sectionRepository.findByPeriodId(1L)).thenReturn(List.of(buildSection()));

        List<SectionResponse> result = sectionService.getSectionsByPeriod(1L);

        assertEquals(1, result.size());
        assertEquals(List.of("L2"), result.get(0).getBlocks());
    }

    @Test
    void getSectionById_existing_returnsSection() {
        when(sectionRepository.findById(5L)).thenReturn(Optional.of(buildSection()));

        assertEquals("María González", sectionService.getSectionById(5L).getTeacherName());
    }

    @Test
    void getSectionById_notFound_throwsNotFound() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> sectionService.getSectionById(99L));
    }

    // ========== deleteSection ==========

    // RN-5.12: section without enrollments -> deleted
    @Test
    void deleteSection_withoutEnrollments_deletesSection() {
        Section section = buildSection();
        when(sectionRepository.findById(5L)).thenReturn(Optional.of(section));

        sectionService.deleteSection(5L);

        verify(sectionRepository).delete(section);
    }
}