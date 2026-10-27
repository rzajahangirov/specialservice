package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.subscription.SubscribeRequestDto;
import az.techvibeds.specialservice.dtos.installment.SubscriptionStatusDto;

public interface SubscriptionService {
    SubscriptionStatusDto subscribe(SubscribeRequestDto dto, String userEmail);
    SubscriptionStatusDto getStatus(String userEmail);

    /**
     * Müddəti bitmiş subscription-ları tapır və FROZEN statusuna keçirir.
     * Şirkətin modulları deaktiv olur, amma istifadəçi rolları saxlanılır.
     */
    void freezeExpiredSubscriptions();

    /**
     * Subscription yeniləndikdə, dondurulmuş modulları bərpa edir.
     */
    void unfreezeSubscription(Long subscriptionId);

    /**
     * GLOBAL_ADMIN manually freezes a company's subscription.
     * All modules are deactivated, user roles are preserved.
     */
    void freezeCompany(Long companyId);

    /**
     * GLOBAL_ADMIN manually unfreezes a company's subscription.
     * All modules are reactivated.
     */
    void unfreezeCompany(Long companyId);
}

