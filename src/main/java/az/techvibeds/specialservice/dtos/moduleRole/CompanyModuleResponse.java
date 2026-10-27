package az.techvibeds.specialservice.dtos.moduleRole;

import lombok.*;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompanyModuleResponse {
    private Long companyModuleId;
    private Long moduleId;
    private String moduleName;
    private Boolean active;
    private Date startDate;
    private Date endDate;
    private ModuleRoleInfo userRole; // bu userin həmin modulda rolu (nullable ola bilər)
}
