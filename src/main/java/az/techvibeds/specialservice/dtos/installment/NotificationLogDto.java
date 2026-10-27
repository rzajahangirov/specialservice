package az.techvibeds.specialservice.dtos.installment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogDto {
    private String type;
    private String message;
    private String sentAt;
}
