package az.techvibeds.specialservice.schedulers;

import az.techvibeds.specialservice.services.FinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringExpenseScheduler {

    private final FinanceService financeService;

    // Her gun saat 03:00-da ishleyir
    @Scheduled(cron = "0 0 3 * * ?")
    public void executeRecurringExpenses() {
        log.info("Starting Recurring Expense execution scheduler...");
        try {
            financeService.executeRecurringExpenses();
            log.info("Recurring Expense execution completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred while executing recurring expenses: ", e);
        }
    }
}
