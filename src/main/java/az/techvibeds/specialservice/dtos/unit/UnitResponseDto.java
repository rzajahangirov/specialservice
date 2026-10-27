package az.techvibeds.specialservice.dtos.unit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitResponseDto {
    private Long id;
    private String name;
    private String category;
}
