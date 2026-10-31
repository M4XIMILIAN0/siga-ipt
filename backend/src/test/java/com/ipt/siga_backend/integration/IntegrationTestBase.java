package com.ipt.siga_backend.integration;

import com.ipt.siga_backend.dto.ScheduleBlockRequest;
import com.ipt.siga_backend.dto.SectionRequest;
import com.ipt.siga_backend.entity.*;
import com.ipt.siga_backend.repository.*;
import com.ipt.siga_backend.service.EnrollmentService;
import com.ipt.siga_backend.service.SectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

// Full Spring context + MockMvc + H2. Each test is rolled back at the end (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestBase {

    // Demo people created by DataLoader (same emails as the Keycloak users)
    protected static final String ADMIN = "admin@ipt.cl";
    protected static final String MARIA = "maria.gonzalez@ipt.cl";
    protected static final String JUAN = "juan.perez@ipt.cl";
    protected static final String PEDRO = "pedro.soto@ipt.cl";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected AcademicPeriodRepository periodRepository;

    @Autowired
    protected CareerRepository careerRepository;

    @Autowired
    protected StudyPlanRepository studyPlanRepository;

    @Autowired
    protected SubjectRepository subjectRepository;

    @Autowired
    protected TeacherRepository teacherRepository;

    @Autowired
    protected StudentRepository studentRepository;

    @Autowired
    protected PeriodRegistrationRepository periodRegistrationRepository;

    @Autowired
    protected SectionService sectionService;

    @Autowired
    protected EnrollmentService enrollmentService;

    // ---------- simulated logged users (a JWT already validated, with email + role) ----------

    protected RequestPostProcessor as(String email, String role) {
        return jwt().jwt(token -> token.claim("email", email))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    protected RequestPostProcessor asAdmin() {
        return as(ADMIN, "ADMIN");
    }

    protected RequestPostProcessor asTeacher(String email) {
        return as(email, "TEACHER");
    }

    protected RequestPostProcessor asStudent() {
        return as(PEDRO, "STUDENT");
    }

    // ---------- test data ----------

    protected Long createOpenPeriod() {
        AcademicPeriod period = new AcademicPeriod();
        period.setYear(2027);
        period.setSemester(1);
        period.setStatus(PeriodStatus.OPEN);
        return periodRepository.save(period).getId();
    }

    // Id of a subject of the CURRENT plan of career 10450 (Técnico Analista Programador)
    protected Long subjectId(String code) {
        Long careerId = careerRepository.findByCode("10450").orElseThrow().getId();
        Long planId = studyPlanRepository.findByCareerIdAndStatus(careerId, StudyPlanStatus.CURRENT)
                .orElseThrow().getId();
        return subjectRepository.findByStudyPlanIdAndCode(planId, code).orElseThrow().getId();
    }

    protected Long teacherId(String email) {
        return teacherRepository.findByEmail(email).orElseThrow().getId();
    }

    protected Long studentId(String email) {
        return studentRepository.findByEmail(email).orElseThrow().getId();
    }

    // Creates a section through the real service (blocks like "L2", "W3")
    protected Long createSection(Long periodId, String subjectCode, String teacherEmail, String... blockCodes) {
        List<ScheduleBlockRequest> blocks = new ArrayList<>();
        for (String code : blockCodes) {
            blocks.add(new ScheduleBlockRequest(ScheduleDay.valueOf(code.substring(0, 1)),
                    Integer.parseInt(code.substring(1))));
        }
        SectionRequest request = new SectionRequest(subjectId(subjectCode), teacherId(teacherEmail), 40, blocks);
        return sectionService.createSection(periodId, request).getId();
    }

    // RN-6.1: registers ("matricula") a student in a period
    protected void registerStudent(Long periodId, String email) {
        PeriodRegistration registration = new PeriodRegistration();
        registration.setStudent(studentRepository.findByEmail(email).orElseThrow());
        registration.setPeriod(periodRepository.findById(periodId).orElseThrow());
        periodRegistrationRepository.save(registration);
    }

    // ---------- small JSON helpers ----------

    // "L2", "W3" -> [{"day": "L", "moduleNumber": 2}, {"day": "W", "moduleNumber": 3}]
    protected String blocksJson(String... blockCodes) {
        List<String> items = new ArrayList<>();
        for (String code : blockCodes) {
            items.add("{\"day\": \"" + code.substring(0, 1) + "\", \"moduleNumber\": " + code.substring(1) + "}");
        }
        return "[" + String.join(", ", items) + "]";
    }

    // 1, 2, 3 -> [1, 2, 3]
    protected String idsJson(Long... ids) {
        List<String> items = new ArrayList<>();
        for (Long id : ids) {
            items.add(String.valueOf(id));
        }
        return "[" + String.join(", ", items) + "]";
    }
}