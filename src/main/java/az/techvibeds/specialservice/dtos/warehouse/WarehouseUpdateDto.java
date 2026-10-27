package az.techvibeds.specialservice.dtos.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WarehouseUpdateDto {
    private Long id;
    private String name;
    private String address;
}
