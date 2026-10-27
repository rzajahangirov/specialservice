package az.techvibeds.specialservice.dtos.inventory;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStatusDto {
    private List<String> productStatus;
}
