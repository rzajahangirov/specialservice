package az.techvibeds.specialservice.dtos.installment;

import lombok.Data;

@Data
public class NotificationSettingDto {
    private boolean smsNotifications;
    private boolean emailNotifications;
    private boolean pushNotifications;
}
