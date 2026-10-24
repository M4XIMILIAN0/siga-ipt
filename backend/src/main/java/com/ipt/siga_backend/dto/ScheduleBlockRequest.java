package com.ipt.siga_backend.dto;

import com.ipt.siga_backend.entity.ScheduleDay;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleBlockRequest {
    private ScheduleDay day;          // L, M, W, J, V
    private Integer moduleNumber;     // 1..6
}