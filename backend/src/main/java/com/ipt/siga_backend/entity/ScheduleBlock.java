package com.ipt.siga_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "schedule_blocks",
        // RN-5.10: a section cannot have the same block twice (DB-level guard)
        uniqueConstraints = @UniqueConstraint(columnNames = {"section_id", "day", "module_number"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RN-5.8: L, M, W, J or V
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private ScheduleDay day;

    // RN-5.8: module between 1 and 6 (validated in SectionService)
    @Column(name = "module_number", nullable = false)
    private Integer moduleNumber;

    // The FK lives here: schedule_blocks.section_id
    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    // "L2" = Monday, module 2 (notation from the statement)
    public String getCode() {
        return day.name() + moduleNumber;
    }
}