package az.techvibeds.specialservice.dtos.roleTemplate;

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
public class RoleTemplatePermissionDto {

    @NotNull(message = "Module ID cannot be null")
    private Long moduleId;

    private String moduleName;

    @NotNull(message = "Access level cannot be null")
    private ModuleRole accessLevel;
}
