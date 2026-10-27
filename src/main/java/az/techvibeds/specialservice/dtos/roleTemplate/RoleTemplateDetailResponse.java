package az.techvibeds.specialservice.dtos.roleTemplate;

import az.techvibeds.specialservice.enums.RoleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleTemplateDetailResponse {
    private Long id;
    private String name;
    private String description;
    private RoleStatus status;
    private Integer userCount;
    private LocalDateTime createdAt;
    private List<RoleTemplatePermissionDto> permissions;
}
