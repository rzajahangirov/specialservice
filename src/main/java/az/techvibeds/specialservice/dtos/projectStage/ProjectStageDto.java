package az.techvibeds.specialservice.dtos.projectStage;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectStageDto {
    private String projectName;
    private String name;
    private Integer stageNumber;
    private Double progressPercentage;
}
