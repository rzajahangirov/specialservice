package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.service.*;
import az.techvibeds.specialservice.enums.ServiceStatus;

import java.util.List;

public interface ServiceService {
    List<ServiceCount> getCountOfServicesByCompany(String email);

    List<ServiceDescriptionDto> getServiceDescriptions(String email);

    List<ServiceExecutionStatusDto> getServiceExecutionStatus(String email);

    ServiceReadDto createService(String email, ServiceCreateDto serviceCreateDto);

    ServiceReadDto updateService(ServiceUpdateDto dto, String userEmail);

    void delete(Long id, String userEmail);

    List<ServiceExecutionStatusDto> getServiceByFiltered(String email, ServiceStatus status, String keyword);

    ServiceReadDto payService(Long serviceId, ServicePaymentDto dto, String userEmail);

    List<String> getServiceStatuses();
}
