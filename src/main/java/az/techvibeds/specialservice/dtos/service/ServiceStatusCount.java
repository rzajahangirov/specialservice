package az.techvibeds.specialservice.dtos.service;

import az.techvibeds.specialservice.enums.ServiceStatus;

public interface ServiceStatusCount {
    ServiceStatus getStatus();
    Long getCount();
}
