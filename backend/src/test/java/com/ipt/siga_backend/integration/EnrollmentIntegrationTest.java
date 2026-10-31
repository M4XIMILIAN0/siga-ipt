package com.ipt.siga_backend.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Process 2 (Epic 6): the logged STUDENT enrolls subjects
class EnrollmentIntegrationTest extends IntegrationTestBase {

    private Long periodId;
    private Long tap101;
    private Long tap102;
    private Long tap103;
    private Long tap104AtL2;
    private Long tap105;
    private Long tap201;

    // Offer of 2027-1 (no teacher conflicts) + Pedro registered in the period
    @BeforeEach
    void setUp() {
        periodId = createOpenPeriod();
        tap101 = createSection(periodId, "TAP101", MARIA, "L2", "W3", "V1");
        tap102 = createSection(periodId, "TAP102", JUAN, "M1", "J4");
        tap103 = createSection(periodId, "TAP103", JUAN, "M2", "J3");
        tap104AtL2 = createSection(periodId, "TAP104", JUAN, "L2", "M3");   // clashes with TAP101 at L2
        tap105 = createSection(periodId, "TAP105", JUAN, "W4", "V4");
        tap201 = createSection(periodId, "TAP201", JUAN, "J1", "J2", "V2"); // requires TAP101
        registerStudent(periodId, PEDRO);
    }

    private ResultActions postEnrollment(RequestPostProcessor user, Long... sectionIds) throws Exception {
        String body = "{\"periodId\": %d, \"sectionIds\": %s}".formatted(periodId, idsJson(sectionIds));
        return mockMvc.perform(post("/api/me/enrollments")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // ===== 3 success scenarios =====

    // RN-6.7 minimum: 3 subjects
    @Test
    void enroll_threeSubjects_returns201() throws Exception {
        postEnrollment(asStudent(), tap101, tap102, tap103)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].studentName").value("Pedro Soto"));
    }

    // RN-6.7: 4 subjects in one request
    @Test
    void enroll_fourSubjects_returns201() throws Exception {
        postEnrollment(asStudent(), tap101, tap102, tap103, tap105)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(4));
    }

    // RN-6.7: already has 3 and adds a 4th one later
    @Test
    void enroll_addSubjectToExistingEnrollments_returns201() throws Exception {
        enrollmentService.enroll(studentId(PEDRO), periodId, List.of(tap101, tap102, tap103));

        postEnrollment(asStudent(), tap105)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ===== 3 rejection scenarios =====

    // RN-6.7: fewer than 3 subjects
    @Test
    void enroll_onlyTwoSubjects_returns400() throws Exception {
        postEnrollment(asStudent(), tap101, tap102)
                .andExpect(status().isBadRequest());
    }

    // RN-6.10: TAP101 and TAP104 are both at L2
    @Test
    void enroll_scheduleConflict_returns409() throws Exception {
        postEnrollment(asStudent(), tap101, tap102, tap104AtL2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("L2")));
    }

    // RN-6.5: TAP201 requires TAP101, which Pedro has not passed
    @Test
    void enroll_prerequisiteNotPassed_returns409() throws Exception {
        postEnrollment(asStudent(), tap102, tap103, tap201)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("TAP101")));
    }

    // ===== security =====

    // RN-P.3: only STUDENT can enroll
    @Test
    void enroll_asAdmin_returns403() throws Exception {
        postEnrollment(asAdmin(), tap101, tap102, tap103)
                .andExpect(status().isForbidden());
    }
}