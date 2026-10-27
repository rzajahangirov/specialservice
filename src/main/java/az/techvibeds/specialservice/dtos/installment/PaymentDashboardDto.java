package az.techvibeds.specialservice.dtos.installment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDashboardDto {
    private SubscriptionStatusDto subscriptionStatus;
    private List<MonthlyPaymentDto> upcomingPayments;
    private List<NotificationLogDto> recentNotifications;
}
