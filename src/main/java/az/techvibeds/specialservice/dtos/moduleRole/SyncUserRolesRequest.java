package az.techvibeds.specialservice.dtos.moduleRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyncUserRolesRequest {
    private Long userId;
    private List<ModuleAssignmentDto> assignments;
}
