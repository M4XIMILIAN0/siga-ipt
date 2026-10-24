package com.ipt.siga_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "sections",
        // RN-5.4: only one section per subject in the same period (DB-level guard)
        uniqueConstraints = @UniqueConstraint(columnNames = {"subject_id", "period_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RN-5.6: 0 < capacity <= 60 (validated in SectionService)
    @Column(nullable = false)
    private Integer capacity;

    // RN-5.3: section belongs to an existing subject and period
    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "period_id", nullable = false)
    private AcademicPeriod period;

    // RN-5.5: responsible teacher
    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    // The only bidirectional relation (decision D2): blocks are created and deleted with their section
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleBlock> blocks = new ArrayList<>();
}