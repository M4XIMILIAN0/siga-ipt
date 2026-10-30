package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.ScheduleBlockRequest;
import com.ipt.siga_backend.dto.SectionRequest;
import com.ipt.siga_backend.dto.SectionResponse;
import com.ipt.siga_backend.entity.*;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SectionService {

    private static final int MAX_CAPACITY = 60;
    private static final int MIN_MODULE = 1;
    private static final int MAX_MODULE = 6;
    private static final int HOURS_PER_BLOCK = 2;   // each block = 2 pedagogical hours (RN-5.9)

    private final SectionRepository sectionRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final AcademicPeriodRepository periodRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository; // <-- Agregado

    // <-- Constructor actualizado
    public SectionService(SectionRepository sectionRepository, ScheduleBlockRepository scheduleBlockRepository,
                          AcademicPeriodRepository periodRepository, SubjectRepository subjectRepository,
                          TeacherRepository teacherRepository, EnrollmentRepository enrollmentRepository) {
        this.sectionRepository = sectionRepository;
        this.scheduleBlockRepository = scheduleBlockRepository;
        this.periodRepository = periodRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public SectionResponse createSection(Long periodId, SectionRequest request) {
        AcademicPeriod period = findPeriodOrThrow(periodId);                       // RN-5.3 -> 404
        validatePeriodIsOpen(period);                                              // decision -> 409
        validateRequiredFields(request);                                           // 400
        Subject subject = findSubjectOrThrow(request.getSubjectId());              // RN-5.3 -> 404
        Teacher teacher = findTeacherOrThrow(request.getTeacherId());              // 404
        validateTeacherIsActive(teacher);                                          // RN-5.5 / RN-4.5 -> 409
        validateCapacity(request.getCapacity());                                   // RN-5.6 -> 400
        validateBlocksAreValid(request.getBlocks());                               // RN-5.8 -> 400
        validateNoDuplicatedBlocks(request.getBlocks());                           // RN-5.10 -> 400
        validateBlockCountMatchesTel(subject, request.getBlocks().size());         // RN-5.9 -> 400
        validateOneSectionPerSubject(subject.getId(), periodId);                   // RN-5.4 -> 409
        validateNoTeacherScheduleConflict(teacher.getId(), periodId, request.getBlocks()); // RN-5.11 -> 409

        Section section = new Section();
        section.setCapacity(request.getCapacity());
        section.setSubject(subject);
        section.setPeriod(period);
        section.setTeacher(teacher);
        for (ScheduleBlockRequest blockRequest : request.getBlocks()) {
            ScheduleBlock block = new ScheduleBlock();
            block.setDay(blockRequest.getDay());
            block.setModuleNumber(blockRequest.getModuleNumber());
            block.setSection(section);          // bidirectional: each block must know its section (fills the FK)
            section.getBlocks().add(block);
        }
        return toResponse(sectionRepository.save(section));   // ONE save: cascade also saves the blocks
    }

    @Transactional
    public List<SectionResponse> getSectionsByPeriod(Long periodId) {
        findPeriodOrThrow(periodId);
        return sectionRepository.findByPeriodId(periodId).stream()
                .map(section -> toResponse(section))
                .toList();
    }

    @Transactional
    public SectionResponse getSectionById(Long id) {
        return toResponse(findSectionOrThrow(id));
    }

    // RN-5.12: a section with enrolled students cannot be physically deleted
    @Transactional
    public void deleteSection(Long id) {
        Section section = findSectionOrThrow(id);
        if (hasEnrollments(id)) {
            throw new ConflictException("Section has enrolled students and cannot be deleted");
        }
        sectionRepository.delete(section);    // cascade also deletes its blocks
    }

    // ---------- validations ----------

    // Decision: sections can only be added to an OPEN period
    private void validatePeriodIsOpen(AcademicPeriod period) {
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new ConflictException("Period " + period.getCode() + " is CLOSED");
        }
    }

    private void validateRequiredFields(SectionRequest request) {
        if (request.getSubjectId() == null || request.getTeacherId() == null
                || request.getCapacity() == null || request.getBlocks() == null) {
            throw new BadRequestException("Subject, teacher, capacity and schedule blocks are required");
        }
    }

    // RN-5.5 / RN-4.5: only ACTIVE teachers can be assigned to new sections
    private void validateTeacherIsActive(Teacher teacher) {
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new ConflictException("Teacher " + teacher.getFullName() + " is INACTIVE");
        }
    }

    // RN-5.6: 0 < capacity <= 60
    private void validateCapacity(Integer capacity) {
        if (capacity <= 0 || capacity > MAX_CAPACITY) {
            throw new BadRequestException("Capacity must be greater than 0 and at most 60");
        }
    }

    // RN-5.8: at least one block, each with a valid day and a module between 1 and 6
    private void validateBlocksAreValid(List<ScheduleBlockRequest> blocks) {
        if (blocks.isEmpty()) {
            throw new BadRequestException("At least one schedule block is required");
        }
        for (ScheduleBlockRequest block : blocks) {
            if (block.getDay() == null || block.getModuleNumber() == null) {
                throw new BadRequestException("Each schedule block needs a day and a module");
            }
            if (block.getModuleNumber() < MIN_MODULE || block.getModuleNumber() > MAX_MODULE) {
                throw new BadRequestException("Module must be between 1 and 6");
            }
        }
    }

    // RN-5.10: no duplicated blocks (e.g. L2 twice)
    private void validateNoDuplicatedBlocks(List<ScheduleBlockRequest> blocks) {
        List<String> codes = blocks.stream()
                .map(block -> block.getDay().name() + block.getModuleNumber())
                .toList();
        if (codes.stream().distinct().count() != codes.size()) {
            throw new BadRequestException("Section has duplicated schedule blocks");
        }
    }

    // RN-5.9: number of blocks = TEL total / 2  (TAP101: 2-2-2 = 6 hours -> 3 blocks)
    private void validateBlockCountMatchesTel(Subject subject, int blockCount) {
        int requiredBlocks = subject.getTelTotal() / HOURS_PER_BLOCK;
        if (blockCount != requiredBlocks) {
            throw new BadRequestException("Subject " + subject.getCode() + " has " + subject.getTelTotal()
                    + " weekly hours, so it needs exactly " + requiredBlocks + " schedule blocks");
        }
    }

    // RN-5.4: one section per subject in the same period
    private void validateOneSectionPerSubject(Long subjectId, Long periodId) {
        if (sectionRepository.existsBySubjectIdAndPeriodId(subjectId, periodId)) {
            throw new ConflictException("This subject already has a section in this period");
        }
    }

    // RN-5.11: the teacher cannot have two sections on the same day+module in the same period
    private void validateNoTeacherScheduleConflict(Long teacherId, Long periodId, List<ScheduleBlockRequest> blocks) {
        for (ScheduleBlockRequest block : blocks) {
            boolean teacherIsBusy = scheduleBlockRepository.existsBySectionTeacherIdAndSectionPeriodIdAndDayAndModuleNumber(
                    teacherId, periodId, block.getDay(), block.getModuleNumber());
            if (teacherIsBusy) {
                throw new ConflictException("Teacher already has a section at " + block.getDay() + block.getModuleNumber()
                        + " in this period");
            }
        }
    }

    // RN-5.12: does the section have enrollments?
    private boolean hasEnrollments(Long sectionId) {
        return enrollmentRepository.existsBySectionId(sectionId);
    }

    // ---------- finders ----------

    private AcademicPeriod findPeriodOrThrow(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Academic period " + id + " not found"));
    }

    private Subject findSubjectOrThrow(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subject " + id + " not found"));
    }

    private Teacher findTeacherOrThrow(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Teacher " + id + " not found"));
    }

    private Section findSectionOrThrow(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Section " + id + " not found"));
    }

    // ---------- mapping ----------

    private SectionResponse toResponse(Section section) {
        List<String> blockCodes = section.getBlocks().stream()
                .map(block -> block.getCode())
                .toList();
        return new SectionResponse(
                section.getId(),
                section.getPeriod().getId(), section.getPeriod().getCode(),
                section.getSubject().getId(), section.getSubject().getCode(),
                section.getSubject().getName(), section.getSubject().getSemester(),
                section.getTeacher().getId(), section.getTeacher().getFullName(),
                section.getCapacity(), blockCodes);
    }

    // RN-P.2: the teacher sees HIS sections of a period
    @Transactional
    public List<SectionResponse> getSectionsByTeacher(Long teacherId, Long periodId) {
        findPeriodOrThrow(periodId);
        return sectionRepository.findByTeacherIdAndPeriodId(teacherId, periodId).stream()
                .map(section -> toResponse(section))
                .toList();
    }
}