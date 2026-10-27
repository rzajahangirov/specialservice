package az.techvibeds.specialservice.dtos.moduleRole;

import lombok.*;

@Data
@Builder
public class CompanyModuleSummaryResponse {
    private Long companyModuleId;
    private String moduleName;
    private Integer userCount;
    private Boolean active;
}
