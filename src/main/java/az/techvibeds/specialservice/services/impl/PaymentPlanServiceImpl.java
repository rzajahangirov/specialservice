package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.installment.*;
import az.techvibeds.specialservice.enums.PlanStatus;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.repositories.*;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.services.PaymentPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentPlanServiceImpl implements PaymentPlanService {

    private final PaymentPlanRepository planRepository;
    private final InstallmentRepository installmentRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PaymentPlanReadDto createPlan(PaymentPlanCreateDto dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (user.getCompany() == null) {
            throw new BusinessException("User does not have an associated company to create a payment plan.");
        }

        PaymentPlan plan = new PaymentPlan();
        plan.setName(dto.getName());
        plan.setTotalAmount(dto.getTotalAmount());
        plan.setCompany(user.getCompany());
        plan.setStatus(PlanStatus.ACTIVE);
        
        PaymentPlan savedPlan = planRepository.save(plan);

        if (dto.getInstallments() != null) {
            List<Installment> installments = dto.getInstallments().stream().map(iDto -> {
                Installment i = new Installment();
                i.setAmount(iDto.getAmount());
                i.setDueDate(iDto.getDueDate());
                i.setPaymentPlan(savedPlan);
                return i;
            }).collect(Collectors.toList());
            installmentRepository.saveAll(installments);
            savedPlan.setInstallments(installments);
        }

        // Bildiriş tarixçəsinə qeyd ataq
        logNotification("Email", "Yeni ödəniş planı yaradıldı: " + plan.getName(), user.getCompany());

        return convertToReadDto(savedPlan);
    }

    @Override
    public Page<PaymentPlanReadDto> getPlans(Pageable pageable, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        if (user.getCompany() == null) {
            return Page.empty();
        }
        
        return planRepository.findAllByCompanyId(user.getCompany().getId(), pageable)
                .map(this::convertToReadDto);
    }

    @Override
    public PaymentPlanReadDto getPlanById(Long id, String userEmail) {
        PaymentPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentPlan", "id", id));
        return convertToReadDto(plan);
    }

    @Override
    @Transactional
    public PaymentPlanReadDto payInstallment(Long planId, Long installmentId, String userEmail) {
        Installment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Installment", "id", installmentId));

        if (installment.isPaid()) {
            throw new RuntimeException("Installment already paid");
        }

        installment.setPaid(true);
        installment.setPaymentDate(LocalDate.now());
        installmentRepository.save(installment);

        PaymentPlan plan = installment.getPaymentPlan();
        plan.setPaidAmount(plan.getPaidAmount().add(installment.getAmount()));
        plan.setLastPaymentDate(LocalDate.now());
        plan.recalculateRemaining();

        if (plan.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            plan.setStatus(PlanStatus.COMPLETED);
            logNotification("SMS", plan.getName() + " ödənişi tamamlandı.", plan.getCompany());
        } else {
            logNotification("Push", "Hissəli ödəniş uğurla qəbul edildi: " + installment.getAmount() + " AZN", plan.getCompany());
        }

        return convertToReadDto(planRepository.save(plan));
    }

    @Override
    public PaymentDashboardDto getDashboard(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
                
        if (user.getCompany() == null) {
            return PaymentDashboardDto.builder()
                    .upcomingPayments(new ArrayList<>())
                    .recentNotifications(new ArrayList<>())
                    .build();
        }

        Long companyId = user.getCompany().getId();

        // 1. Abunəlik məlumatları
        SubscriptionStatusDto subStatus = subscriptionRepository.findFirstByCompanyIdOrderByEndDateDesc(companyId)
                .map(s -> SubscriptionStatusDto.builder()
                        .planName(s.getPlan().getName())
                        .endDate(s.getEndDate())
                        .nextPaymentDate(s.getEndDate().minusDays(5)) // Təxmini
                        .status(s.getStatus().name())
                        .build())
                .orElse(null);

        // 2. Yaxınlaşan ödənişlər (Aylara görə qruplaşdırma)
        Map<String, BigDecimal> monthlyTotals = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 6; i++) {
            String monthName = now.plusMonths(i).getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("az"));
            monthlyTotals.put(monthName, BigDecimal.ZERO);
        }

        List<PaymentPlan> activePlans = planRepository.findAllByCompanyId(companyId, Pageable.unpaged()).getContent();
        for (PaymentPlan p : activePlans) {
            for (Installment i : p.getInstallments()) {
                if (!i.isPaid() && i.getDueDate().isAfter(now.minusDays(1)) && i.getDueDate().isBefore(now.plusMonths(6))) {
                    String mName = i.getDueDate().getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("az"));
                    if (monthlyTotals.containsKey(mName)) {
                        monthlyTotals.put(mName, monthlyTotals.get(mName).add(i.getAmount()));
                    }
                }
            }
        }

        List<MonthlyPaymentDto> chartData = monthlyTotals.entrySet().stream()
                .map(e -> MonthlyPaymentDto.builder().month(e.getKey()).amount(e.getValue()).build())
                .collect(Collectors.toList());

        // 3. Son bildirişlər
        List<NotificationLogDto> recentLogs = notificationLogRepository.findTop5ByCompanyIdOrderBySentAtDesc(companyId)
                .stream()
                .map(l -> NotificationLogDto.builder()
                        .type(l.getType())
                        .message(l.getMessage())
                        .sentAt(l.getSentAt().toString())
                        .build())
                .collect(Collectors.toList());

        return PaymentDashboardDto.builder()
                .subscriptionStatus(subStatus)
                .upcomingPayments(chartData)
                .recentNotifications(recentLogs)
                .build();
    }

    @Override
    public NotificationSettingDto getNotificationSettings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        if (user.getCompany() == null) {
            return new NotificationSettingDto();
        }

        NotificationSetting settings = notificationSettingRepository.findByCompanyId(user.getCompany().getId())
                .orElseGet(() -> {
                    NotificationSetting ns = new NotificationSetting();
                    ns.setCompany(user.getCompany());
                    return notificationSettingRepository.save(ns);
                });

        NotificationSettingDto dto = new NotificationSettingDto();
        dto.setSmsNotifications(settings.isSmsNotifications());
        dto.setEmailNotifications(settings.isEmailNotifications());
        dto.setPushNotifications(settings.isPushNotifications());
        return dto;
    }

    @Override
    @Transactional
    public NotificationSettingDto updateNotificationSettings(NotificationSettingDto dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (user.getCompany() == null) {
            throw new BusinessException("User does not have an associated company to update notification settings.");
        }

        NotificationSetting settings = notificationSettingRepository.findByCompanyId(user.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("NotificationSetting", "companyId", user.getCompany().getId()));

        settings.setSmsNotifications(dto.isSmsNotifications());
        settings.setEmailNotifications(dto.isEmailNotifications());
        settings.setPushNotifications(dto.isPushNotifications());
        
        notificationSettingRepository.save(settings);
        return dto;
    }

    private void logNotification(String type, String message, Company company) {
        NotificationLog log = new NotificationLog();
        log.setType(type);
        log.setMessage(message);
        log.setCompany(company);
        notificationLogRepository.save(log);
    }

    private PaymentPlanReadDto convertToReadDto(PaymentPlan entity) {
        PaymentPlanReadDto dto = new PaymentPlanReadDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setPaidAmount(entity.getPaidAmount());
        dto.setRemainingAmount(entity.getRemainingAmount());
        dto.setLastPaymentDate(entity.getLastPaymentDate());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        
        if (entity.getInstallments() != null) {
            dto.setInstallments(entity.getInstallments().stream().map(i -> {
                InstallmentDto iDto = new InstallmentDto();
                iDto.setId(i.getId());
                iDto.setAmount(i.getAmount());
                iDto.setDueDate(i.getDueDate());
                iDto.setPaymentDate(i.getPaymentDate());
                iDto.setPaid(i.isPaid());
                return iDto;
            }).collect(Collectors.toList()));
        }
        
        return dto;
    }
}
