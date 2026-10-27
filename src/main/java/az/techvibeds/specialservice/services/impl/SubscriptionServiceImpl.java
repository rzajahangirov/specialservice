package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.installment.SubscriptionStatusDto;
import az.techvibeds.specialservice.dtos.subscription.SubscribeRequestDto;
import az.techvibeds.specialservice.enums.SubscriptionStatus;
import az.techvibeds.specialservice.models.CompanyModule;
import az.techvibeds.specialservice.models.Subscription;
import az.techvibeds.specialservice.models.SubscriptionPlan;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.repositories.CompanyModuleRepository;
import az.techvibeds.specialservice.repositories.SubscriptionPlanRepository;
import az.techvibeds.specialservice.repositories.SubscriptionRepository;
import az.techvibeds.specialservice.repositories.UserRepository;
import az.techvibeds.specialservice.services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UserRepository userRepository;
    private final CompanyModuleRepository companyModuleRepository;

    @Override
    @Transactional
    public SubscriptionStatusDto subscribe(SubscribeRequestDto dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SubscriptionPlan plan = planRepository.findById(dto.getPlanId())
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        // Cari aktiv abunəliyi bağlayaq (əgər varsa)
        subscriptionRepository.findFirstByCompanyIdOrderByEndDateDesc(user.getCompany().getId())
                .ifPresent(s -> {
                    s.setStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(s);
                });

        Subscription sub = new Subscription();
        sub.setCompany(user.getCompany());
        sub.setPlan(plan);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartDate(LocalDate.now());
        sub.setBillingCycle(dto.getBillingCycle() != null ? dto.getBillingCycle() : az.techvibeds.specialservice.enums.BillingCycle.YEARLY);

        // BillingCycle-a görə endDate hesabla
        if (sub.getBillingCycle() == az.techvibeds.specialservice.enums.BillingCycle.MONTHLY) {
            sub.setEndDate(LocalDate.now().plusMonths(1));
        } else {
            sub.setEndDate(LocalDate.now().plusYears(1));
        }

        sub.setPricePaid(plan.getPrice());
        sub.setFrozen(false);

        Subscription saved = subscriptionRepository.save(sub);

        // PlanModuleConfig → CompanyModule sinxronizasiyası
        syncCompanyModulesFromPlan(user.getCompany(), plan);

        log.info("Subscription created for company: {}. Plan: {}. Modules synced from plan config.",
                user.getCompany().getName(), plan.getName());

        return convertToDto(saved);
    }

    /**
     * Planın module konfiqurasiyasına əsasən şirkətin company_modules cədvəlini yeniləyir.
     * - Planda olan modullar → aktiv edilir (əgər yoxdursa yaradılır)
     * - Planda olmayan köhnə modullar → deaktiv edilir
     */
    private void syncCompanyModulesFromPlan(
            az.techvibeds.specialservice.models.Company company,
            SubscriptionPlan plan) {

        List<Long> planModuleIds = plan.getModuleConfigs().stream()
                .filter(config -> Boolean.TRUE.equals(config.getActive()))
                .map(config -> config.getModule().getId())
                .toList();

        List<CompanyModule> existingModules = companyModuleRepository.findAllByCompanyId(company.getId());

        // Mövcud modulların ID map-i
        java.util.Map<Long, CompanyModule> existingMap = existingModules.stream()
                .collect(java.util.stream.Collectors.toMap(
                        cm -> cm.getModule().getId(),
                        cm -> cm
                ));

        // Plan modullarını aktiv et / yarat
        for (az.techvibeds.specialservice.models.PlanModuleConfig config : plan.getModuleConfigs()) {
            if (!Boolean.TRUE.equals(config.getActive())) continue;
            Long moduleId = config.getModule().getId();
            if (existingMap.containsKey(moduleId)) {
                // Mövcuddur → aktivləşdir
                CompanyModule cm = existingMap.get(moduleId);
                cm.setActive(true);
                companyModuleRepository.save(cm);
            } else {
                // Yoxdur → yarat
                CompanyModule newCm = new CompanyModule();
                newCm.setCompany(company);
                newCm.setModule(config.getModule());
                newCm.setActive(true);
                newCm.setStartDate(new java.util.Date());
                companyModuleRepository.save(newCm);
                log.info("New CompanyModule created for company {} - module {}",
                        company.getName(), config.getModule().getName());
            }
        }

        // Planda olmayan mövcud modulları deaktiv et
        for (CompanyModule cm : existingModules) {
            if (!planModuleIds.contains(cm.getModule().getId())) {
                cm.setActive(false);
                companyModuleRepository.save(cm);
                log.info("CompanyModule deactivated (not in new plan) for company {} - module {}",
                        company.getName(), cm.getModule().getName());
            }
        }
    }


    @Override
    public SubscriptionStatusDto getStatus(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return subscriptionRepository.findFirstByCompanyIdOrderByEndDateDesc(user.getCompany().getId())
                .map(this::convertToDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public void freezeExpiredSubscriptions() {
        List<Subscription> expiredSubs = subscriptionRepository.findActiveExpiredSubscriptions(LocalDate.now());

        for (Subscription sub : expiredSubs) {
            log.info("Freezing subscription for company: {} (subscription ID: {})",
                    sub.getCompany().getName(), sub.getId());

            // Subscription-u FROZEN statusuna keçir
            sub.setStatus(SubscriptionStatus.FROZEN);
            sub.setFrozen(true);
            sub.setFrozenAt(LocalDate.now());
            subscriptionRepository.save(sub);

            // Şirkətin modullarını deaktiv et (amma user_module_roles saxlanılır!)
            List<CompanyModule> companyModules = companyModuleRepository.findAllByCompanyId(sub.getCompany().getId());
            for (CompanyModule cm : companyModules) {
                cm.setActive(false);
                companyModuleRepository.save(cm);
            }

            log.info("Subscription frozen. {} modules deactivated for company: {}",
                    companyModules.size(), sub.getCompany().getName());
        }

        if (!expiredSubs.isEmpty()) {
            log.info("Total {} subscriptions frozen.", expiredSubs.size());
        }
    }

    @Override
    @Transactional
    public void unfreezeSubscription(Long subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (sub.getStatus() != SubscriptionStatus.FROZEN) {
            log.warn("Subscription {} is not frozen, skipping unfreeze.", subscriptionId);
            return;
        }

        log.info("Unfreezing subscription {} for company: {}", subscriptionId, sub.getCompany().getName());

        // Subscription-u yenidən aktiv et
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setFrozen(false);
        sub.setFrozenAt(null);
        sub.setEndDate(LocalDate.now().plusYears(1)); // Yeni müddət
        subscriptionRepository.save(sub);

        // Şirkətin modullarını yenidən aktiv et
        List<CompanyModule> companyModules = companyModuleRepository.findAllByCompanyId(sub.getCompany().getId());
        for (CompanyModule cm : companyModules) {
            cm.setActive(true);
            companyModuleRepository.save(cm);
        }

        log.info("Subscription unfrozen. {} modules reactivated for company: {}",
                companyModules.size(), sub.getCompany().getName());
    }

    @Override
    @Transactional
    public void freezeCompany(Long companyId) {
        Optional<Subscription> subOpt = subscriptionRepository.findFirstByCompanyIdOrderByEndDateDesc(companyId);

        if (subOpt.isPresent()) {
            Subscription sub = subOpt.get();
            if (sub.getStatus() == SubscriptionStatus.FROZEN) {
                log.warn("Company {} subscription is already frozen.", companyId);
                return;
            }
            sub.setStatus(SubscriptionStatus.FROZEN);
            sub.setFrozen(true);
            sub.setFrozenAt(LocalDate.now());
            subscriptionRepository.save(sub);
        }

        // Deactivate all company modules (roles are preserved)
        List<CompanyModule> companyModules = companyModuleRepository.findAllByCompanyId(companyId);
        for (CompanyModule cm : companyModules) {
            cm.setActive(false);
            companyModuleRepository.save(cm);
        }

        log.info("Company {} manually frozen by GLOBAL_ADMIN. {} modules deactivated.",
                companyId, companyModules.size());
    }

    @Override
    @Transactional
    public void unfreezeCompany(Long companyId) {
        Optional<Subscription> subOpt = subscriptionRepository.findFirstByCompanyIdOrderByEndDateDesc(companyId);

        if (subOpt.isPresent()) {
            Subscription sub = subOpt.get();
            if (sub.getStatus() != SubscriptionStatus.FROZEN) {
                log.warn("Company {} subscription is not frozen, skipping unfreeze.", companyId);
                return;
            }
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setFrozen(false);
            sub.setFrozenAt(null);
            subscriptionRepository.save(sub);
        }

        // Reactivate all company modules
        List<CompanyModule> companyModules = companyModuleRepository.findAllByCompanyId(companyId);
        for (CompanyModule cm : companyModules) {
            cm.setActive(true);
            companyModuleRepository.save(cm);
        }

        log.info("Company {} manually unfrozen by GLOBAL_ADMIN. {} modules reactivated.",
                companyId, companyModules.size());
    }

    private SubscriptionStatusDto convertToDto(Subscription s) {
        return SubscriptionStatusDto.builder()
                .planName(s.getPlan().getName())
                .endDate(s.getEndDate())
                .nextPaymentDate(s.getEndDate().minusDays(5))
                .status(s.getStatus().name())
                .build();
    }
}

