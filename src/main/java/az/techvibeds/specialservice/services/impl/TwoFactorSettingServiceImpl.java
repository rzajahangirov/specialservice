package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.twoFactor.TwoFactorSettingRequest;
import az.techvibeds.specialservice.dtos.twoFactor.TwoFactorSettingResponse;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.TwoFactorSetting;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.repositories.TwoFactorSettingRepository;
import az.techvibeds.specialservice.repositories.UserRepository;
import az.techvibeds.specialservice.services.AuditLogService;
import az.techvibeds.specialservice.services.TwoFactorSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorSettingServiceImpl implements TwoFactorSettingService {

    private final TwoFactorSettingRepository twoFactorSettingRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public TwoFactorSettingResponse getSettings(String adminEmail) {
        User admin = findUserByEmail(adminEmail);
        TwoFactorSetting settings = getOrCreateSettings(admin.getCompany());

        return TwoFactorSettingResponse.builder()
                .enabled(settings.getEnabled())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public TwoFactorSettingResponse toggleTwoFactor(TwoFactorSettingRequest request, String adminEmail) {
        log.info("Toggling 2FA setting to {} by {}", request.getEnabled(), adminEmail);

        User admin = findUserByEmail(adminEmail);
        TwoFactorSetting settings = getOrCreateSettings(admin.getCompany());

        settings.setEnabled(request.getEnabled());
        twoFactorSettingRepository.save(settings);

        String action = request.getEnabled() ? "2FA_ENABLED" : "2FA_DISABLED";
        auditLogService.logAction(adminEmail, action, "TwoFactorSetting",
                "2FA " + (request.getEnabled() ? "aktivləşdirildi" : "deaktiv edildi"));

        log.info("2FA setting updated: enabled={}", request.getEnabled());

        return TwoFactorSettingResponse.builder()
                .enabled(settings.getEnabled())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    private TwoFactorSetting getOrCreateSettings(Company company) {
        return twoFactorSettingRepository.findByCompanyId(company.getId())
                .orElseGet(() -> {
                    TwoFactorSetting newSettings = new TwoFactorSetting();
                    newSettings.setCompany(company);
                    newSettings.setEnabled(false);
                    return twoFactorSettingRepository.save(newSettings);
                });
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
