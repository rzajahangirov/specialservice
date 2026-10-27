package az.techvibeds.specialservice.schedulers;

import az.techvibeds.specialservice.enums.DeductionDurationType;
import az.techvibeds.specialservice.models.Deduction;
import az.techvibeds.specialservice.repositories.DeductionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeductionCleanupScheduler {

    private final DeductionRepository deductionRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deactivateExpiredDeductions() {
        log.info("Deduction Cleanup Scheduler started at {}", LocalDate.now());

        List<Deduction> allActive = deductionRepository.findAll().stream()
                .filter(Deduction::isActive)
                .toList();

        int deactivatedCount = 0;
        LocalDate today = LocalDate.now();

        for (Deduction d : allActive) {
            LocalDate expiryDate = d.getDurationType() == DeductionDurationType.MONTHS
                    ? d.getStartDate().plusMonths(d.getDuration())
                    : d.getStartDate().plusDays(d.getDuration());

            if (today.isAfter(expiryDate) || today.isEqual(expiryDate)) {
                d.setActive(false);
                deactivatedCount++;
                log.info("Deduction ID {} deactivated. Reason: Expired on {}", d.getId(), expiryDate);
            }
        }

        if (deactivatedCount > 0) {
            deductionRepository.saveAll(allActive);
            log.info("Cleanup finished. Total deactivated: {}", deactivatedCount);
        } else {
            log.info("Cleanup finished. No expired deductions found.");
        }
    }
}