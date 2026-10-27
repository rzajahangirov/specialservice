package az.techvibeds.specialservice.dtos.constructionProject;

import az.techvibeds.specialservice.enums.ProjectStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConstructionProjectUpdateDto {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private ProjectStatus status;
    private BigDecimal budget;
    private String projectManager;
    private Long projectContractorId;
}
