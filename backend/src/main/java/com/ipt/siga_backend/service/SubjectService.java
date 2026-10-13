package com.ipt.siga_backend.service;

import com.ipt.siga_backend.dto.SubjectRequest;
import com.ipt.siga_backend.dto.SubjectResponse;
import com.ipt.siga_backend.entity.StudyPlan;
import com.ipt.siga_backend.entity.Subject;
import com.ipt.siga_backend.exception.BadRequestException;
import com.ipt.siga_backend.exception.ConflictException;
import com.ipt.siga_backend.exception.NotFoundException;
import com.ipt.siga_backend.repository.StudyPlanRepository;
import com.ipt.siga_backend.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SubjectService {

    private static final int MIN_SEMESTER = 1;
    private static final int MAX_SEMESTER = 4;        // careers last 4 semesters (RN-2.2)
    private static final int MAX_SCT = 7;
    private static final int MAX_WEEKLY_HOURS = 8;    // T+E+L must be LESS than this
    private static final int MAX_PREREQUISITES = 3;

    private final SubjectRepository subjectRepository;
    private final StudyPlanRepository studyPlanRepository;

    public SubjectService(SubjectRepository subjectRepository, StudyPlanRepository studyPlanRepository) {
        this.subjectRepository = subjectRepository;
        this.studyPlanRepository = studyPlanRepository;
    }

    @Transactional
    public SubjectResponse createSubject(Long studyPlanId, SubjectRequest request) {
        StudyPlan studyPlan = findStudyPlanOrThrow(studyPlanId);        // RN-3.1 -> 404
        validateSubjectData(request);                                   // RN-3.5 / RN-3.6 -> 400
        validateCodeIsUniqueInPlan(studyPlanId, request.getCode());      // RN-3.2 -> 409
        List<Subject> prerequisites = resolvePrerequisites(studyPlanId, request); // RN-3.7/3.8/3.9

        Subject subject = new Subject();
        subject.setCode(request.getCode());
        subject.setName(request.getName());
        subject.setSemester(request.getSemester());
        subject.setTheoryHours(request.getTheoryHours());
        subject.setExerciseHours(request.getExerciseHours());
        subject.setLabHours(request.getLabHours());
        subject.setSctCredits(request.getSctCredits());
        subject.setStudyPlan(studyPlan);
        subject.setPrerequisites(prerequisites);
        return toResponse(subjectRepository.save(subject));
    }

    // @Transactional keeps the DB session open while we read the (lazy) prerequisites
    @Transactional
    public List<SubjectResponse> getSubjectsByPlan(Long studyPlanId) {
        findStudyPlanOrThrow(studyPlanId);
        return subjectRepository.findByStudyPlanId(studyPlanId).stream()
                .map(subject -> toResponse(subject))
                .toList();
    }

    @Transactional
    public SubjectResponse getSubjectById(Long id) {
        return toResponse(findSubjectOrThrow(id));
    }

    // RN-3.11: editing a subject does NOT change history (AcademicRecord keeps its own copy)
    @Transactional
    public SubjectResponse updateSubject(Long id, SubjectRequest request) {
        Subject subject = findSubjectOrThrow(id);                            // 404
        Long studyPlanId = subject.getStudyPlan().getId();
        validateSubjectData(request);                                         // 400

        // RN-3.2: only check uniqueness if the code changed (same trap as careers)
        if (!subject.getCode().equals(request.getCode())) {
            validateCodeIsUniqueInPlan(studyPlanId, request.getCode());      // 409
        }
        List<Subject> prerequisites = resolvePrerequisites(studyPlanId, request); // RN-3.7/3.8/3.9

        subject.setCode(request.getCode());
        subject.setName(request.getName());
        subject.setSemester(request.getSemester());
        subject.setTheoryHours(request.getTheoryHours());
        subject.setExerciseHours(request.getExerciseHours());
        subject.setLabHours(request.getLabHours());
        subject.setSctCredits(request.getSctCredits());
        subject.setPrerequisites(prerequisites);    // replaces the list; JPA updates subject_prerequisites
        return toResponse(subjectRepository.save(subject));
    }

    @Transactional
    public void deleteSubject(Long id) {
        Subject subject = findSubjectOrThrow(id);                            // 404
        validateIsNotPrerequisiteOfOthers(id);                               // 409
        // TODO RN-3.10 (Epic 7): reject if the subject appears in any academic record
        // TODO (Epic 5): reject if the subject has sections
        subjectRepository.delete(subject);
    }


    // ---------- validations ----------

    private StudyPlan findStudyPlanOrThrow(Long studyPlanId) {
        return studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new NotFoundException("Study plan " + studyPlanId + " not found"));
    }

    // RN-3.1: code and name are required (and the numeric fields too)
    private void validateRequiredFields(SubjectRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BadRequestException("Subject code is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Subject name is required");
        }
        if (request.getSemester() == null || request.getSctCredits() == null
                || request.getTheoryHours() == null || request.getExerciseHours() == null
                || request.getLabHours() == null) {
            throw new BadRequestException("Semester, hours and SCT are required");
        }
    }

    // RN-3.3: semester must be between 1 and 4
    private void validateSemester(Integer semester) {
        if (semester < MIN_SEMESTER || semester > MAX_SEMESTER) {
            throw new BadRequestException("Semester must be between 1 and 4");
        }
    }

    // RN-3.4: SCT must be greater than 0 and at most 7
    private void validateSct(Integer sctCredits) {
        if (sctCredits <= 0 || sctCredits > MAX_SCT) {
            throw new BadRequestException("SCT must be greater than 0 and at most 7");
        }
    }

    // RN-3.5: no negative hours / RN-3.6: total weekly hours < 8
    private void validateWeeklyHours(SubjectRequest request) {
        if (request.getTheoryHours() < 0 || request.getExerciseHours() < 0 || request.getLabHours() < 0) {
            throw new BadRequestException("Weekly hours cannot be negative");
        }
        int total = request.getTheoryHours() + request.getExerciseHours() + request.getLabHours();
        if (total >= MAX_WEEKLY_HOURS) {
            throw new BadRequestException("Total weekly hours (T+E+L) must be less than 8");
        }
    }

    // RN-3.2: code must be unique within the study plan
    private void validateCodeIsUniqueInPlan(Long studyPlanId, String code) {
        if (subjectRepository.existsByStudyPlanIdAndCode(studyPlanId, code)) {
            throw new ConflictException("Subject code " + code + " already exists in this study plan");
        }
    }

    // Turns ["TAP201", "TAP204"] into the real Subject objects, checking RN-3.7 / 3.8 / 3.9
    private List<Subject> resolvePrerequisites(Long studyPlanId, SubjectRequest request) {
        List<Subject> prerequisites = new ArrayList<>();
        List<String> codes = request.getPrerequisiteCodes();
        if (codes == null || codes.isEmpty()) {
            return prerequisites;                              // 0 prerequisites is valid
        }

        validatePrerequisiteCount(codes);                      // RN-3.7
        for (String code : codes) {
            Subject prerequisite = findPrerequisiteInPlan(studyPlanId, code);           // RN-3.8
            validatePrerequisiteIsFromEarlierSemester(prerequisite, request.getSemester()); // RN-3.9
            prerequisites.add(prerequisite);
        }
        return prerequisites;
    }

    // RN-3.7: between 0 and 3 prerequisites, no duplicates
    private void validatePrerequisiteCount(List<String> codes) {
        if (codes.size() > MAX_PREREQUISITES) {
            throw new BadRequestException("A subject can have at most 3 prerequisites");
        }
        if (codes.stream().distinct().count() != codes.size()) {
            throw new BadRequestException("Prerequisite list contains duplicates");
        }
    }

    // RN-3.8: prerequisite must exist in the SAME study plan
    private Subject findPrerequisiteInPlan(Long studyPlanId, String code) {
        return subjectRepository.findByStudyPlanIdAndCode(studyPlanId, code)
                .orElseThrow(() -> new BadRequestException("Prerequisite " + code + " does not exist in this study plan"));
    }

    // RN-3.9: prerequisite must belong to an earlier semester
    private void validatePrerequisiteIsFromEarlierSemester(Subject prerequisite, Integer semester) {
        if (prerequisite.getSemester() >= semester) {
            throw new BadRequestException("Prerequisite " + prerequisite.getCode() + " must belong to an earlier semester");
        }
    }

    private Subject findSubjectOrThrow(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subject " + id + " not found"));
    }

    // A subject that other subjects require cannot be deleted
    private void validateIsNotPrerequisiteOfOthers(Long subjectId) {
        if (subjectRepository.existsByPrerequisitesId(subjectId)) {
            throw new ConflictException("Subject is a prerequisite of other subjects and cannot be deleted");
        }
    }

    // ---------- mapping ----------

    // Entity -> DTO (prerequisites as codes, not nested objects)
    private SubjectResponse toResponse(Subject subject) {
        List<String> prerequisiteCodes = subject.getPrerequisites().stream()
                .map(prerequisite -> prerequisite.getCode())
                .toList();
        return new SubjectResponse(
                subject.getId(), subject.getCode(), subject.getName(), subject.getSemester(),
                subject.getTheoryHours(), subject.getExerciseHours(), subject.getLabHours(),
                subject.getTelTotal(), subject.getSctCredits(),
                subject.getStudyPlan().getId(), prerequisiteCodes);
    }


    // RN-3.1 / 3.3 / 3.4 / 3.5 / 3.6: data checks shared by create and update
    private void validateSubjectData(SubjectRequest request) {
        validateRequiredFields(request);
        validateSemester(request.getSemester());
        validateSct(request.getSctCredits());
        validateWeeklyHours(request);
    }



}