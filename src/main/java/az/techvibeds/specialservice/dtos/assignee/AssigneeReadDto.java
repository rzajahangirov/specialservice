package az.techvibeds.specialservice.dtos.assignee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AssigneeReadDto {
    private Long id;
    private String name;
    private Integer activeServiceCount;
    private Integer totalCapacity;
    private String imageUrl;
    private Long companyId;
}
