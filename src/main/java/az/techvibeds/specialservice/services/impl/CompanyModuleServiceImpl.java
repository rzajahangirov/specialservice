package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.module.ModuleReadDto;
import az.techvibeds.specialservice.models.CompanyModule;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.repositories.CompanyModuleRepository;
import az.techvibeds.specialservice.repositories.UserRepository;
import az.techvibeds.specialservice.services.CompanyModuleService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CompanyModuleServiceImpl implements CompanyModuleService {
    private final CompanyModuleRepository companyModuleRepository;
    private final UserRepository userRepository;

    @Override
    public List<ModuleReadDto> getCompanysModules(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        List<CompanyModule> companyModules = user.getCompany().getCompanyModules();
        if (companyModules.isEmpty()) {
            return Collections.emptyList();
        }
        return companyModules.stream()
                .filter(cm -> Boolean.TRUE.equals(cm.getActive()))
                .filter(cm -> cm.getModule() != null)
                .map(cm -> {
                    ModuleReadDto dto = new ModuleReadDto();
                    dto.setId(cm.getModule().getId());
                    dto.setName(cm.getModule().getName());
                    dto.setBackendKey(cm.getModule().getBackendKey());
                    dto.setIcon(cm.getModule().getIcon());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deactivateExpiredCompanyModules() {
        Date now = new Date();

        List<CompanyModule> expiredModules =
                companyModuleRepository.findByActiveTrueAndEndDateBefore(now);

        for (CompanyModule companyModule : expiredModules) {
            companyModule.setActive(false);
        }

        companyModuleRepository.saveAll(expiredModules);
    }
}
