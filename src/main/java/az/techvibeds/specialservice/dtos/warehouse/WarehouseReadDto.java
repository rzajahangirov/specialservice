package az.techvibeds.specialservice.dtos.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WarehouseReadDto {
    private Long id;
    private String name;
    private String address;
    private String companyName;
    private Boolean active;
}
