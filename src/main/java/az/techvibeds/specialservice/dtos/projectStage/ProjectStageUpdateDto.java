package az.techvibeds.specialservice.dtos.projectStage;


import az.techvibeds.specialservice.enums.StageStatus;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectStageUpdateDto {
    @NotBlank(message = "Stage name is required")
    private String name;

    @Min(1)
    private Integer stageNumber;

    @Min(0) @Max(100)
    private Double progressPercentage;

    private StageStatus status;
}
