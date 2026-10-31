package com.ipt.siga_backend.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Process 1 (Epic 5): ADMIN creates a section in the academic offer
class SectionCreationIntegrationTest extends IntegrationTestBase {

    private Long periodId;

    @BeforeEach
    void setUp() {
        periodId = createOpenPeriod();
    }

    private String sectionBody(String subjectCode, String teacherEmail, String... blockCodes) {
        return "{\"subjectId\": %d, \"teacherId\": %d, \"capacity\": 40, \"blocks\": %s}"
                .formatted(subjectId(subjectCode), teacherId(teacherEmail), blocksJson(blockCodes));
    }

    private ResultActions postSection(String body, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(post("/api/periods/" + periodId + "/sections")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // ===== 3 success scenarios =====

    // RN-5.9: TAP101 (2-2-2 = 6 h) with exactly 3 blocks
    @Test
    void createSection_validTap101_returns201() throws Exception {
        postSection(sectionBody("TAP101", MARIA, "L2", "W3", "V1"), asAdmin())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectCode").value("TAP101"))
                .andExpect(jsonPath("$.blocks.length()").value(3));
    }

    // RN-5.11: the same teacher can have another section at different blocks
    @Test
    void createSection_sameTeacherDifferentBlocks_returns201() throws Exception {
        createSection(periodId, "TAP101", MARIA, "L2", "W3", "V1");

        postSection(sectionBody("TAP102", MARIA, "M1", "J4"), asAdmin())
                .andExpect(status().isCreated());
    }

    // RN-5.11: two DIFFERENT teachers can use the same block (L2)
    @Test
    void createSection_otherTeacherSameBlock_returns201() throws Exception {
        createSection(periodId, "TAP101", MARIA, "L2", "W3", "V1");

        postSection(sectionBody("TAP102", JUAN, "L2", "J4"), asAdmin())
                .andExpect(status().isCreated());
    }

    // ===== 3 rejection scenarios =====

    // RN-5.9: TAP101 needs 3 blocks, 2 are rejected
    @Test
    void createSection_wrongBlockCount_returns400() throws Exception {
        postSection(sectionBody("TAP101", MARIA, "L2", "W3"), asAdmin())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("exactly 3")));
    }

    // RN-5.11: María is already busy at L2 in this period
    @Test
    void createSection_teacherScheduleConflict_returns409() throws Exception {
        createSection(periodId, "TAP101", MARIA, "L2", "W3", "V1");

        postSection(sectionBody("TAP102", MARIA, "L2", "J4"), asAdmin())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("L2")));
    }

    // RN-5.4: only one section per subject in the same period
    @Test
    void createSection_subjectAlreadyOffered_returns409() throws Exception {
        createSection(periodId, "TAP101", MARIA, "L2", "W3", "V1");

        postSection(sectionBody("TAP101", JUAN, "M1", "M2", "M3"), asAdmin())
                .andExpect(status().isConflict());
    }

    // ===== security =====

    // RN-P.1: only ADMIN manages the offer
    @Test
    void createSection_asStudent_returns403() throws Exception {
        postSection(sectionBody("TAP101", MARIA, "L2", "W3", "V1"), asStudent())
                .andExpect(status().isForbidden());
    }
}