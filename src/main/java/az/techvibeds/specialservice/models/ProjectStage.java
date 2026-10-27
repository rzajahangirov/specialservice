package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.StageStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "project_stages")
public class ProjectStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Integer stageNumber;
    private Double progressPercentage;

    @Enumerated(EnumType.STRING)
    private StageStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ConstructionProject project;
}
