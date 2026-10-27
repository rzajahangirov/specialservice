package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.hrActivity.HRActivityCreateDto;
import az.techvibeds.specialservice.dtos.hrActivity.HRActivityResponseDto;
import az.techvibeds.specialservice.exceptions.EmployeeNotFoundException;
import az.techvibeds.specialservice.models.HRActivity;
import az.techvibeds.specialservice.models.HREmployee;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.repositories.HRActivityRepository;
import az.techvibeds.specialservice.repositories.HREmployeeRepository;
import az.techvibeds.specialservice.services.HRActivityService;
import az.techvibeds.specialservice.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HRActivityServiceImpl implements HRActivityService {
    private final HRActivityRepository activityRepository;
    private final HREmployeeRepository employeeRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public HRActivityResponseDto createActivity(HRActivityCreateDto request, String callerEmail) {
        log.info("Recording new HR activity: {} for employeeId={}", request.getActivityType(), request.getEmployeeId());

        HREmployee employee = employeeRepository.findByIdAndDeletedFalse(request.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(request.getEmployeeId()));

        validateCompanyAccess(callerEmail, employee);

        HRActivity activity = new HRActivity();
        activity.setEmployee(employee);
        activity.setActivityType(request.getActivityType());
        activity.setStatus(request.getStatus());

        HRActivity saved = activityRepository.save(activity);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HRActivityResponseDto> getEmployeeActivities(Long employeeId, String callerEmail) {
        log.info("Fetching activity logs for employeeId={}", employeeId);

        HREmployee employee = employeeRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        validateCompanyAccess(callerEmail, employee);

        return activityRepository.findAllByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HRActivityResponseDto> getCompanyActivityFeed(String callerEmail) {
        User caller = userService.findByEmail(callerEmail);
        Long companyId = caller.getCompany().getId();

        log.info("Fetching company-wide activity feed for companyId={}", companyId);

        return activityRepository.findAllByEmployeeCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .limit(50)
                .map(this::mapToResponse)
                .toList();
    }


    private void validateCompanyAccess(String email, HREmployee target) {
        User caller = userService.findByEmail(email);
        if (!caller.getCompany().getId().equals(target.getCompany().getId())) {
            throw new AccessDeniedException("Unauthorized: This employee belongs to another company.");
        }
    }

    private HRActivityResponseDto mapToResponse(HRActivity activity) {
        HRActivityResponseDto dto = modelMapper.map(activity, HRActivityResponseDto.class);
        dto.setEmployeeId(activity.getEmployee().getId());
        dto.setEmployeeFullName(activity.getEmployee().getFirstName() + " " + activity.getEmployee().getLastName());
        return dto;
    }
}
