package com.ipt.siga_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OfferItemResponse {
    private Long sectionId;
    private String subjectCode;
    private String subjectName;
    private Integer semester;
    private Integer sctCredits;
    private String teacherName;
    private List<String> blocks;
    private List<String> prerequisiteCodes;
    private long availableSeats;
    private boolean passed;                 // already passed -> cannot enroll (RN-6.4)
    private boolean prerequisitesPassed;    // false -> cannot enroll (RN-6.5)
}