package az.techvibeds.specialservice.dtos.backup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BackupSettingResponse {
    private Boolean autoBackupEnabled;
    private LocalDateTime lastBackupDate;
}
