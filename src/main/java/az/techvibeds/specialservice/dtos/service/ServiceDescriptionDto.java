package az.techvibeds.specialservice.dtos.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceDescriptionDto {
    private Long id;
    private String name;
    private String description;
    private java.math.BigDecimal amount;
    private String dtoUnit;
    private Long partnerId;
    private String partnerName;
}
