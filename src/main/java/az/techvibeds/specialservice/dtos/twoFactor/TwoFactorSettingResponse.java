package az.techvibeds.specialservice.dtos.twoFactor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TwoFactorSettingResponse {
    private Boolean enabled;
    private LocalDateTime updatedAt;
}
