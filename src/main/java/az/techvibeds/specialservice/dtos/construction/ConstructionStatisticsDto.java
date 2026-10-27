package az.techvibeds.specialservice.dtos.construction;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConstructionStatisticsDto {
    private Integer countOfProjects;
    private Integer countOfActiveProjects;
    private Integer countOfCompletedProjects;
    private Double budgetUsagePercentage;
}
