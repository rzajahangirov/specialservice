package az.techvibeds.specialservice.dtos.moduleRole;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserModulePermissionDto {
    private String moduleName;
    private String role;
}
