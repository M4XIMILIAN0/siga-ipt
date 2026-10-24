package com.ipt.siga_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SectionRequest {
    private Long subjectId;
    private Long teacherId;
    private Integer capacity;
    private List<ScheduleBlockRequest> blocks;
}