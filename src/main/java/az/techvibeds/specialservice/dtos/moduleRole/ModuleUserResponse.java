package az.techvibeds.specialservice.dtos.moduleRole;

import az.techvibeds.specialservice.enums.ModuleRole;
import lombok.*;

@Data
@Builder
public class ModuleUserResponse {
    private Long userId;
    private String fullName;
    private String email;
    private ModuleRole role;
    private Boolean active;
}
