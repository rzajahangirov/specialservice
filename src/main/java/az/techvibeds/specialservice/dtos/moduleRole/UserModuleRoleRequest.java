package az.techvibeds.specialservice.dtos.moduleRole;

import az.techvibeds.specialservice.enums.ModuleRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserModuleRoleRequest {

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotNull(message = "CompanyModule ID cannot be null")
    private Long companyModuleId;

    @NotNull(message = "Module role cannot be null")
    private ModuleRole role;
}
