package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.models.Company;

public interface NotificationService {
    void createEmailNotification(String message, Company company);
}
