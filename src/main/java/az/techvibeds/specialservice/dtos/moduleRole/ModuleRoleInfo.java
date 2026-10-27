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
public class ModuleRoleInfo {
    private Long userModuleRoleId;
    private ModuleRole role;
}
