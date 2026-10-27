package az.techvibeds.specialservice.dtos.partner;

import az.techvibeds.specialservice.enums.PartnerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartnerTypeDto {
    private List<PartnerType> partnerTypes;
}
