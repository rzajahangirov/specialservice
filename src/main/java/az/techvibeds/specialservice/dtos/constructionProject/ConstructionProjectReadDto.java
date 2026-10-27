package az.techvibeds.specialservice.dtos.constructionProject;

import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseReadDto;
import az.techvibeds.specialservice.dtos.projectStage.ProjectStageReadDto;
import az.techvibeds.specialservice.dtos.constructionMaterial.ConstructionProjectMaterialReadDto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConstructionProjectReadDto {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String companyName;
    private String status;
    private BigDecimal budget;
    private String projectManager;
    private String projectContractorName;
    private List<ProjectExpenseReadDto> expensesList;
    private List<ProjectStageReadDto> stagesList;
    private List<ConstructionProjectMaterialReadDto> materialsList;

}
