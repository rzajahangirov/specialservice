package az.techvibeds.specialservice.dtos.assignee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssigneeServiceDto {
    private Long id;
    private String name;
    private Integer activeServiceCount;
    private Integer totalCapacity;
    private String imageUrl;
}
