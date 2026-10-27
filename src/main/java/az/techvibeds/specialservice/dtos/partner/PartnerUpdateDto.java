package az.techvibeds.specialservice.dtos.partner;

import az.techvibeds.specialservice.enums.PartnerType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartnerUpdateDto {

    @NotNull(message = "Partner ID to be updated is required")
    private Long id;

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    private String contactPerson;

    @Email(message = "Email address is not in a valid format")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number format is invalid")
    private String phone;
    private String voen;
    private String contractNumber;

    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    private BigDecimal balance;

    private String currency;

    private PartnerType partnerType;
}