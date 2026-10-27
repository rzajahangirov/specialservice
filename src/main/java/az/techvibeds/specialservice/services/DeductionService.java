package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.deduction.DeductionCreateDto;
import az.techvibeds.specialservice.dtos.deduction.DeductionResponseDto;
import az.techvibeds.specialservice.dtos.deduction.DeductionUpdateDto;
import az.techvibeds.specialservice.payloads.EnumResponseDto;

import java.util.List;

public interface DeductionService {
    DeductionResponseDto createDeduction(DeductionCreateDto request, String name);

    List<EnumResponseDto> getDeductionTypes();
    List<EnumResponseDto> getDeductionDurationTypes();

    DeductionResponseDto updateDeduction(Long id, DeductionUpdateDto request, String callerEmail);

    void toggleDeductionStatus(Long id, boolean status, String callerEmail);

    void deleteDeduction(Long id, String name);

    List<DeductionResponseDto> getDeletedDeductions(String name);

    DeductionResponseDto restoreDeduction(Long id, String name);

    List<DeductionResponseDto> getEmployeeDeductions(Long employeeId, String name);
}
