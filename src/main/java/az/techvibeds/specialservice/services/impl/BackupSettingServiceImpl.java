package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.backup.BackupSettingResponse;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.BackupSetting;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.repositories.BackupSettingRepository;
import az.techvibeds.specialservice.repositories.UserRepository;
import az.techvibeds.specialservice.services.AuditLogService;
import az.techvibeds.specialservice.services.BackupSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupSettingServiceImpl implements BackupSettingService {

    private final BackupSettingRepository backupSettingRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public BackupSettingResponse getSettings(String adminEmail) {
        User admin = findUserByEmail(adminEmail);
        BackupSetting settings = getOrCreateSettings(admin.getCompany());

        return mapToResponse(settings);
    }

    @Override
    @Transactional
    public BackupSettingResponse toggleAutoBackup(Boolean enabled, String adminEmail) {
        log.info("Toggling auto backup to {} by {}", enabled, adminEmail);

        User admin = findUserByEmail(adminEmail);
        BackupSetting settings = getOrCreateSettings(admin.getCompany());

        settings.setAutoBackupEnabled(enabled);
        backupSettingRepository.save(settings);

        auditLogService.logAction(adminEmail, "BACKUP_AUTO_TOGGLED", "BackupSetting",
                "Avtomatik nüsxə " + (enabled ? "aktivləşdirildi" : "deaktiv edildi"));

        return mapToResponse(settings);
    }

    @Override
    @Transactional
    public BackupSettingResponse triggerBackupNow(String adminEmail) {
        log.info("Manual backup triggered by {}", adminEmail);

        User admin = findUserByEmail(adminEmail);
        BackupSetting settings = getOrCreateSettings(admin.getCompany());

        settings.setLastBackupDate(LocalDateTime.now());
        backupSettingRepository.save(settings);

        auditLogService.logAction(adminEmail, "BACKUP_TRIGGERED", "BackupSetting",
                "Manual ehtiyat nüsxəsi yaradıldı");

        return mapToResponse(settings);
    }

    private BackupSetting getOrCreateSettings(Company company) {
        return backupSettingRepository.findByCompanyId(company.getId())
                .orElseGet(() -> {
                    BackupSetting newSettings = new BackupSetting();
                    newSettings.setCompany(company);
                    newSettings.setAutoBackupEnabled(true);
                    return backupSettingRepository.save(newSettings);
                });
    }

    private BackupSettingResponse mapToResponse(BackupSetting settings) {
        return BackupSettingResponse.builder()
                .autoBackupEnabled(settings.getAutoBackupEnabled())
                .lastBackupDate(settings.getLastBackupDate())
                .build();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
