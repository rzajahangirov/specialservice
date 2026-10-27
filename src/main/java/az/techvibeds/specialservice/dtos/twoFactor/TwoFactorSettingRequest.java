package az.techvibeds.specialservice.dtos.twoFactor;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TwoFactorSettingRequest {

    @NotNull(message = "Enabled status cannot be null")
    private Boolean enabled;
}
