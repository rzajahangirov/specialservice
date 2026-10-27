package az.techvibeds.specialservice.dtos.projectStage;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectStageReadDto {
    private Long id;
    private String name;
    private Integer stageNumber;
    private Double progressPercentage;
    private String stagesStatus;
}
