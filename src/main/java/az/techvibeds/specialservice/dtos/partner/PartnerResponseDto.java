package az.techvibeds.specialservice.dtos.partner;

import az.techvibeds.specialservice.enums.PartnerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartnerResponseDto {
    private Long id;
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String voen;
    private String contractNumber;
    private BigDecimal balance;
    private String currency;
    private PartnerType partnerType;
}
