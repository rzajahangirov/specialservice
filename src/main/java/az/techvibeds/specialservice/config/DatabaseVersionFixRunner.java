package az.techvibeds.specialservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseVersionFixRunner {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void fixNullVersionsOnStartup() {
        log.info("Checking and fixing NULL versions in database for optimistic locking...");
        try {
            int productsFixed = jdbcTemplate.update("UPDATE products SET version = 0 WHERE version IS NULL");
            if (productsFixed > 0) {
                log.info("Fixed {} NULL version(s) in products table.", productsFixed);
            }

            int wpFixed = jdbcTemplate.update("UPDATE warehouse_products SET version = 0 WHERE version IS NULL");
            if (wpFixed > 0) {
                log.info("Fixed {} NULL version(s) in warehouse_products table.", wpFixed);
            }

            int invoicesFixed = jdbcTemplate.update("UPDATE invoices SET version = 0 WHERE version IS NULL");
            if (invoicesFixed > 0) {
                log.info("Fixed {} NULL version(s) in invoices table.", invoicesFixed);
            }
        } catch (Exception e) {
            log.error("Failed to execute version fix script: {}", e.getMessage());
        }
    }
}
