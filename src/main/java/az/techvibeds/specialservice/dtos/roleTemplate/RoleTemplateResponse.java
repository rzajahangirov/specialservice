package az.techvibeds.specialservice.dtos.roleTemplate;

import az.techvibeds.specialservice.enums.RoleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleTemplateResponse {
    private Long id;
    private String name;
    private String description;
    private RoleStatus status;
    private Integer userCount;
}
