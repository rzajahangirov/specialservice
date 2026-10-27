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
public class UpdateUserModuleRoleRequest {

    @NotNull(message = "Role cannot be null")
    private ModuleRole role;
}
