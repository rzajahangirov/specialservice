package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.deduction.DeductionCreateDto;
import az.techvibeds.specialservice.dtos.deduction.DeductionResponseDto;
import az.techvibeds.specialservice.dtos.deduction.DeductionUpdateDto;
import az.techvibeds.specialservice.enums.DeductionDurationType;
import az.techvibeds.specialservice.enums.DeductionType;
import az.techvibeds.specialservice.exceptions.EmployeeNotFoundException;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.Deduction;
import az.techvibeds.specialservice.models.HREmployee;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.payloads.EnumResponseDto;
import az.techvibeds.specialservice.repositories.DeductionRepository;
import az.techvibeds.specialservice.repositories.HREmployeeRepository;
import az.techvibeds.specialservice.services.DeductionService;
import az.techvibeds.specialservice.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeductionServiceImpl implements DeductionService {

    private final DeductionRepository deductionRepository;
    private final HREmployeeRepository employeeRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;

    @Override
    @Transactional
    public DeductionResponseDto createDeduction(DeductionCreateDto request, String callerEmail) {
        log.info("Creating deduction for employeeId={} by caller={}", request.getEmployeeId(), callerEmail);

        HREmployee target = findEmployeeOrThrow(request.getEmployeeId());
        validateCompanyAccess(callerEmail, target);

        Deduction deduction = modelMapper.map(request, Deduction.class);
        deduction.setEmployee(target);
        deduction.setActive(true);

        Deduction saved = deductionRepository.save(deduction);
        return mapToResponse(saved);
    }

    @Override
    public List<EnumResponseDto> getDeductionTypes() {
        return Arrays.stream(DeductionType.values())
                .map(type -> EnumResponseDto.builder()
                        .key(type.name())
                        .value(formatEnumValue(type.name()))
                        .build())
                .toList();
    }

    @Override
    public List<EnumResponseDto> getDeductionDurationTypes() {
        return Arrays.stream(DeductionDurationType.values())
                .map(type -> EnumResponseDto.builder()
                        .key(type.name())
                        .value(formatEnumValue(type.name()))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public DeductionResponseDto updateDeduction(Long id, DeductionUpdateDto request, String callerEmail) {
        log.info("Updating deductionId={} by caller={}", id, callerEmail);

        Deduction deduction = findDeductionOrThrow(id, false);
        validateCompanyAccess(callerEmail, deduction.getEmployee());

        deduction.setType(request.getType());
        deduction.setAmount(request.getAmount());
        deduction.setReason(request.getReason());
        deduction.setStartDate(request.getStartDate());
        deduction.setDuration(request.getDuration());
        deduction.setDurationType(request.getDurationType());

        return mapToResponse(deductionRepository.save(deduction));
    }

    @Override
    @Transactional
    public void toggleDeductionStatus(Long id, boolean status, String callerEmail) {
        Deduction deduction = findDeductionOrThrow(id, false);
        validateCompanyAccess(callerEmail, deduction.getEmployee());

        deduction.setActive(status);
        deductionRepository.save(deduction);
        log.info("DeductionId={} status toggled to {}", id, status);
    }

    @Override
    @Transactional
    public void deleteDeduction(Long id, String callerEmail) {
        Deduction deduction = findDeductionOrThrow(id, false);
        validateCompanyAccess(callerEmail, deduction.getEmployee());

        deduction.setDeleted(true);
        deduction.setActive(false);
        deductionRepository.save(deduction);
        log.info("DeductionId={} soft-deleted", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeductionResponseDto> getDeletedDeductions(String callerEmail) {
        User caller = userService.findByEmail(callerEmail);
        return deductionRepository.findAllByEmployeeCompanyIdAndDeletedTrue(caller.getCompany().getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public DeductionResponseDto restoreDeduction(Long id, String callerEmail) {
        Deduction deduction = findDeductionOrThrow(id, true);
        validateCompanyAccess(callerEmail, deduction.getEmployee());

        deduction.setDeleted(false);
        deduction.setActive(true);
        return mapToResponse(deductionRepository.save(deduction));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeductionResponseDto> getEmployeeDeductions(Long employeeId, String callerEmail) {
        HREmployee target = findEmployeeOrThrow(employeeId);
        validateCompanyAccess(callerEmail, target);

        return deductionRepository.findAllByEmployeeIdAndDeletedFalseOrderByStartDateDesc(employeeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    private void validateCompanyAccess(String callerEmail, HREmployee targetEmployee) {
        User caller = userService.findByEmail(callerEmail);

        if (caller.getCompany() == null || targetEmployee.getCompany() == null ||
                !caller.getCompany().getId().equals(targetEmployee.getCompany().getId())) {

            log.warn("SECURITY ALERT: User {} unauthorized access attempt to Employee ID {}", callerEmail, targetEmployee.getId());
            throw new AccessDeniedException("Access Denied: You cannot manage employees outside your company.");
        }
    }

    private HREmployee findEmployeeOrThrow(Long id) {
        return employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private Deduction findDeductionOrThrow(Long id, boolean isDeletedSearch) {
        if (isDeletedSearch) {
            return deductionRepository.findByIdAndDeletedTrue(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Deleted Deduction", "id", id));
        }
        return deductionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deduction", "id", id));
    }

    private DeductionResponseDto mapToResponse(Deduction deduction) {
        DeductionResponseDto dto = modelMapper.map(deduction, DeductionResponseDto.class);
        dto.setEmployeeId(deduction.getEmployee().getId());
        return dto;
    }

    private String formatEnumValue(String name) {
        if (name == null || name.isEmpty()) return "";
        String replaced = name.replace("_", " ").toLowerCase();
        return replaced.substring(0, 1).toUpperCase() + replaced.substring(1);
    }
}