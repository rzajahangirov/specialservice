package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.NotificationLog;
import az.techvibeds.specialservice.repositories.NotificationLogRepository;
import az.techvibeds.specialservice.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    @Override
    @Transactional
    public void createEmailNotification(String message, Company company) {
        NotificationLog logEntry = new NotificationLog();
        logEntry.setType("EMAIL");
        logEntry.setMessage(message);
        logEntry.setCompany(company);
        notificationLogRepository.save(logEntry);
        
        log.info("Saved EMAIL notification for company {}: {}", company.getId(), message);
    }
}
