package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.backup.BackupSettingResponse;

public interface BackupSettingService {

    BackupSettingResponse getSettings(String adminEmail);

    BackupSettingResponse toggleAutoBackup(Boolean enabled, String adminEmail);

    BackupSettingResponse triggerBackupNow(String adminEmail);
}
