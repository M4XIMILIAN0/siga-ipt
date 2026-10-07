package com.ipt.siga_backend.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "study_plans",
        // RN-2.6: plan identifier must be unique
        uniqueConstraints = @UniqueConstraint(columnNames = {"career_id", "identifier"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class StudyPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyPlanStatus status;

    // Many plans belong to one career
    @ManyToOne
    @JoinColumn(name = "career_id", nullable = false)
    private Career career;
}
