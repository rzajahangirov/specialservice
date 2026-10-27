package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.repositories.CompanyModuleRepository;
import az.techvibeds.specialservice.repositories.UserRepository;
import az.techvibeds.specialservice.services.ModuleAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

/**
 * Modul icazəsi yoxlama service-inin implementasiyası.
 * CompanyModule cədvəlindən aktiv modulların backendKey-lərini çəkir
 * və hesabatlıq/dashboard kimi modullarda data filtrasiyası üçün istifadə olunur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModuleAccessServiceImpl implements ModuleAccessService {

    private final CompanyModuleRepository companyModuleRepository;
    private final UserRepository userRepository;

    @Override
    public boolean hasModuleAccess(Long companyId, String backendKey) {
        Set<String> activeKeys = getActiveModuleKeys(companyId);
        boolean hasAccess = activeKeys.contains(backendKey);
        if (!hasAccess) {
            log.debug("Company {} has no access to module: {}", companyId, backendKey);
        }
        return hasAccess;
    }

    @Override
    public Set<String> getActiveModuleKeys(Long companyId) {
        if (companyId == null) {
            return Collections.emptySet();
        }
        return companyModuleRepository.findActiveModuleKeysByCompanyId(companyId);
    }

    @Override
    public Set<String> getActiveModuleKeys(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı: " + userEmail));
        if (user.getCompany() == null) {
            return Collections.emptySet();
        }
        return getActiveModuleKeys(user.getCompany().getId());
    }
}
