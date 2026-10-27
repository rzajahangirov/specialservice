package az.techvibeds.specialservice.dtos.partner;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartnerReadDto {
    private Long id;
    private String name;
    private String ContactPerson;
    private String email;
    private String phone;
    private String voen;
    private String contractNumber;
    private BigDecimal balance;
    private String currency;
    private String partnerType;
    private Boolean isActive;
}
