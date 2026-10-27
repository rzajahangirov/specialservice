package az.techvibeds.specialservice.schedulers;

import az.techvibeds.specialservice.services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hər gün gecə 02:00-da müddəti bitmiş subscription-ları yoxlayır
 * və FROZEN statusuna keçirir. İstifadəçi rolları silinmir, dondurulur.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionFreezeScheduler {

    private final SubscriptionService subscriptionService;

    @Scheduled(cron = "0 0 2 * * ?") // Hər gün saat 02:00
    public void checkAndFreezeExpiredSubscriptions() {
        log.info("Running subscription freeze check...");
        subscriptionService.freezeExpiredSubscriptions();
        log.info("Subscription freeze check completed.");
    }
}
