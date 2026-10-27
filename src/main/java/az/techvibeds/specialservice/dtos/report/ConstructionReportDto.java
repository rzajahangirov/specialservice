package az.techvibeds.specialservice.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConstructionReportDto {
    private Long projectId;
    private String projectName;
    private String projectManager;
    private BigDecimal budget;
    private BigDecimal usedBudget;
    private BigDecimal remainingBudget;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer stageCount;
    private Integer completedStageCount;
}
