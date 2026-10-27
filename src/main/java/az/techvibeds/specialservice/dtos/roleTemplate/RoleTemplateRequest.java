package az.techvibeds.specialservice.dtos.roleTemplate;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleTemplateRequest {

    @NotBlank(message = "Role template name cannot be empty")
    private String name;

    private String description;

    private List<RoleTemplatePermissionDto> permissions;
}
