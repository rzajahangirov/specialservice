package az.techvibeds.specialservice.dtos.moduleRole;

import az.techvibeds.specialservice.enums.ModuleRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserModuleRoleResponse {
    private Long id;
    private Long userId;
    private String userFullName;
    private Long companyModuleId;
    private String moduleName;
    private ModuleRole role;
}
