package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.service.*;
import az.techvibeds.specialservice.enums.ServiceStatus;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.enums.TransactionType;
import az.techvibeds.specialservice.repositories.*;
import az.techvibeds.specialservice.services.AssigneeService;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.ServiceService;
import az.techvibeds.specialservice.services.UnitService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final ModelMapper modelMapper;
    private final ServiceRepository serviceRepository;
    private final CompanyService companyService;
    private final AssigneeService assigneeService;
    private final UnitService unitService;
    private final PartnerRepository partnerRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BalanceRepository balanceRepository;

    @Override
    public List<ServiceCount> getCountOfServicesByCompany(String email) {
        log.info("Fetching service counts for company with email: {}", email);

        Long companyId = findCompanyByEmail(email).getId();

        List<ServiceStatusCount> statusCounts = serviceRepository.countServicesByStatusAndCompanyId(companyId);

        Map<ServiceStatus, Long> countsMap = statusCounts.stream()
                .collect(Collectors.toMap(ServiceStatusCount::getStatus, ServiceStatusCount::getCount));

        long inProgress = countsMap.getOrDefault(ServiceStatus.IN_PROGRESS, 0L);
        long completed = countsMap.getOrDefault(ServiceStatus.COMPLETED, 0L);
        long pending = countsMap.getOrDefault(ServiceStatus.PENDING, 0L);
        long all = inProgress + completed + pending;

        return List.of(
                new ServiceCount("In Progress", (int) inProgress),
                new ServiceCount("Completed", (int) completed),
                new ServiceCount("Pending", (int) pending),
                new ServiceCount("All", (int) all)
        );
    }

    @Override
    public List<ServiceDescriptionDto> getServiceDescriptions(String email) {
        log.debug("Fetching service descriptions for email: {}", email);
        return serviceRepository.findAllByCompany(findCompanyByEmail(email)).stream()
                .map(service -> {
                    ServiceDescriptionDto dto = modelMapper.map(service, ServiceDescriptionDto.class);
                    dto.setDtoUnit(service.getUnit() != null ? service.getUnit().getName() : null);
                    dto.setPartnerId(service.getPartner() != null ? service.getPartner().getId() : null);
                    dto.setPartnerName(service.getPartner() != null ? service.getPartner().getName() : null);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceExecutionStatusDto> getServiceExecutionStatus(String email) {
        log.debug("Fetching service execution statuses for email: {}", email);
        return serviceRepository.findAllByCompany(findCompanyByEmail(email)).stream()
                .map(service -> {
                    ServiceExecutionStatusDto dto = modelMapper.map(service, ServiceExecutionStatusDto.class);
                    dto.setDtoAssignee(service.getAssignee() != null ? service.getAssignee().getName() : null);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ServiceReadDto createService(String email, ServiceCreateDto dto) {
        log.info("Creating new service for user: {}, Service Name: {}", email, dto.getName());
        Assignee assignee = assigneeService.findAssigneeById(dto.getAssigneeId());
        validateAssigneeCapacity(assignee);

        Service service = new Service();
        service.setName(dto.getName());
        service.setDescription(dto.getDescription());
        service.setAmount(dto.getAmount());
        service.setDeadline(dto.getDeadline());
        service.setStatus(dto.getStatus());
        service.setAssignee(assignee);
        service.setCompany(findCompanyByEmail(email));
        service.setUnit(unitService.findUnitById(dto.getUnitId()));
        if (dto.getPartnerId() != null) {
            Partner partner = partnerRepository.findById(dto.getPartnerId())
                    .orElseThrow(() -> new EntityNotFoundException("Partner not found with id: " + dto.getPartnerId()));
            service.setPartner(partner);
        }

        Service savedService = serviceRepository.save(service);
        log.info("Service created successfully with ID: {}", savedService.getId());

        return mapToReadDto(savedService);
    }

    @Override
    @Transactional
    public ServiceReadDto updateService(ServiceUpdateDto dto, String userEmail) {
        log.info("Updating service ID: {} for user: {}", dto.getId(), userEmail);
        Service service = serviceRepository.findById(dto.getId())
                .orElseThrow(() -> {
                    log.error("Update failed: Service not found with id {}", dto.getId());
                    return new EntityNotFoundException("Service not found with id: " + dto.getId());
                });

        Company company = findCompanyByEmail(userEmail);
        if (!service.getCompany().getId().equals(company.getId())) {
            log.warn("Unauthorized update attempt for service ID: {} by user: {}", dto.getId(), userEmail);
            throw new AccessDeniedException("You don't have permission to update this service");
        }

        Optional.ofNullable(dto.getName()).ifPresent(service::setName);
        Optional.ofNullable(dto.getDescription()).ifPresent(service::setDescription);
        Optional.ofNullable(dto.getAmount()).ifPresent(service::setAmount);
        Optional.ofNullable(dto.getDeadline()).ifPresent(service::setDeadline);
        Optional.ofNullable(dto.getStatus()).ifPresent(service::setStatus);

        if (dto.getUnitId() != null) service.setUnit(unitService.findUnitById(dto.getUnitId()));
        if (dto.getPartnerId() != null) {
            Partner partner = partnerRepository.findById(dto.getPartnerId())
                    .orElseThrow(() -> new EntityNotFoundException("Partner not found with id: " + dto.getPartnerId()));
            service.setPartner(partner);
        }
        if (dto.getAssigneeId() != null) {
            Assignee assignee = assigneeService.findAssigneeById(dto.getAssigneeId());
            validateAssigneeCapacity(assignee);
            service.setAssignee(assignee);
        }

        log.info("Service ID: {} updated successfully", dto.getId());
        return mapToReadDto(service);
    }

    @Override
    @Transactional
    public void delete(Long id, String userEmail) {
        log.info("Attempting to delete service ID: {} by user: {}", id, userEmail);
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service not found"));

        Company company = findCompanyByEmail(userEmail);

        if (!service.getCompany().getId().equals(company.getId())) {
            log.warn("Unauthorized delete attempt for service ID: {} by user: {}", id, userEmail);
            throw new AccessDeniedException("You don't have permission to delete this service");
        }

        serviceRepository.delete(service);
        log.info("Service ID: {} deleted successfully", id);
    }

    @Override
    public List<ServiceExecutionStatusDto> getServiceByFiltered(String email, ServiceStatus status, String keyword) {
        log.info("Filtering services for user: {}, status: {}, keyword: {}", email, status, keyword);
        Long companyId = findCompanyByEmail(email).getId();

        String processedKeyword = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.toLowerCase() + "%"
                : null;

        return serviceRepository.filterServices(companyId, status, processedKeyword).stream()
                .map(service -> {
                    ServiceExecutionStatusDto dto = modelMapper.map(service, ServiceExecutionStatusDto.class);
                    dto.setDtoAssignee(service.getAssignee() != null ? service.getAssignee().getName() : null);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getServiceStatuses() {
        return Arrays.stream(ServiceStatus.values())
                .map(Enum::name)
                .toList();
    }


    @Override
    @Transactional
    public ServiceReadDto payService(Long serviceId, ServicePaymentDto dto, String userEmail) {
        log.info("Processing payment for service ID: {} by user: {}", serviceId, userEmail);
        
        Company company = findCompanyByEmail(userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + userEmail));

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException("Service not found with id: " + serviceId));

        if (!service.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("You don't have permission to pay for this service");
        }

        if (service.isPaid()) {
            throw new BusinessException("This service is already fully paid");
        }

        BigDecimal amount = dto.getAmount();
        BigDecimal currentPaid = service.getPaidAmount() != null ? service.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = service.getAmount() != null ? service.getAmount() : BigDecimal.ZERO;
        BigDecimal remainingAmount = totalAmount.subtract(currentPaid);

        if (amount.compareTo(remainingAmount) > 0) {
            throw new BusinessException("Payment amount cannot exceed the remaining balance. Remaining balance: " + remainingAmount);
        }

        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + dto.getAccountId()));

        if (!account.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("This account does not belong to your company");
        }

        // Add amount to account balance (Income)
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        // Find or create category for Service Payment
        TransactionCategory category = categoryRepository
                .findByCompanyIdAndTypeAndName(company.getId(), TransactionType.INCOME, "Xidmət Ödənişi")
                .orElseGet(() -> {
                    TransactionCategory newCat = new TransactionCategory();
                    newCat.setName("Xidmət Ödənişi");
                    newCat.setType(TransactionType.INCOME);
                    newCat.setCompany(company);
                    return categoryRepository.save(newCat);
                });

        // Create Financial Transaction
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setAmount(amount);
        transaction.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDateTime.now());
        transaction.setType(TransactionType.INCOME);
        transaction.setDescription(dto.getDescription() != null ? dto.getDescription() : (service.getName() + " xidməti üzrə ödəniş"));
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setCompany(company);
        transaction.setCreatedBy(user);
        transaction.setService(service);
        if (service.getPartner() != null) {
            transaction.setPartner(service.getPartner());
            
            // Update partner balance
            Partner partner = service.getPartner();
            Balance balance = partner.getBalance();
            if (balance == null) {
                balance = new Balance();
                balance.setAmount(BigDecimal.ZERO);
                balance.setCurrencyType("manat");
                balance.setPartner(partner);
                partner.setBalance(balance);
            }
            // For service payment, it's an income, so customer balance decreases (they owe us less)
            balance.setAmount(balance.getAmount().subtract(amount));
            balanceRepository.save(balance);
            partnerRepository.save(partner);
        }
        transactionRepository.save(transaction);

        // Update service payment status
        BigDecimal newPaidAmount = currentPaid.add(amount);
        service.setPaidAmount(newPaidAmount);
        if (newPaidAmount.compareTo(totalAmount) >= 0) {
            service.setPaid(true);
        }
        serviceRepository.save(service);

        return mapToReadDto(service);
    }

    private Company findCompanyByEmail(String email) {
        return companyService.findByUserEmail(email);
    }

    private void validateAssigneeCapacity(Assignee assignee) {
        if (assignee.getTotalCapacity() == null) {
            return; // No capacity limit set
        }
        int activeCount = assignee.getActiveServiceCount() != null ? assignee.getActiveServiceCount() : 0;
        if (activeCount >= assignee.getTotalCapacity()) {
            log.warn("Assignee capacity exceeded for assignee ID: {}", assignee.getId());
            throw new BusinessException("Assignee capacity exceeded! Limit: " + assignee.getTotalCapacity());
        }
    }

    private ServiceReadDto mapToReadDto(Service service) {
        ServiceReadDto dto = modelMapper.map(service, ServiceReadDto.class);
        dto.setAssignee(service.getAssignee() != null ? service.getAssignee().getName() : null);
        dto.setUnit(service.getUnit() != null ? service.getUnit().getName() : null);
        dto.setPartnerId(service.getPartner() != null ? service.getPartner().getId() : null);
        dto.setPartnerName(service.getPartner() != null ? service.getPartner().getName() : null);
        dto.setCompanyName(service.getCompany() != null ? service.getCompany().getName() : null);
        return dto;
    }
}