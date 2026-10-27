package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.installment.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentPlanService {
    PaymentPlanReadDto createPlan(PaymentPlanCreateDto dto, String userEmail);
    Page<PaymentPlanReadDto> getPlans(Pageable pageable, String userEmail);
    PaymentPlanReadDto getPlanById(Long id, String userEmail);
    PaymentPlanReadDto payInstallment(Long planId, Long installmentId, String userEmail);
    
    PaymentDashboardDto getDashboard(String userEmail);
    
    NotificationSettingDto getNotificationSettings(String userEmail);
    NotificationSettingDto updateNotificationSettings(NotificationSettingDto dto, String userEmail);
}
