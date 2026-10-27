package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.payroll.PayrollCreateDto;
import az.techvibeds.specialservice.dtos.payroll.PayrollResponseDto;
import az.techvibeds.specialservice.dtos.payroll.PayrollUpdateDto;
import az.techvibeds.specialservice.dtos.payroll.PayrollStatsDto;
import jakarta.validation.Valid;

import java.time.YearMonth;
import java.util.List;

public interface PayrollService {
    PayrollResponseDto generatePayroll(@Valid PayrollCreateDto request, String name);

    PayrollResponseDto updatePayroll(Long id, PayrollUpdateDto request, String callerEmail);

    void togglePaymentStatus(Long id, az.techvibeds.specialservice.dtos.payroll.PayrollPaymentDto request, String callerEmail);

    PayrollResponseDto payAdvance(Long id, az.techvibeds.specialservice.dtos.payroll.AdvancePaymentDto request, String callerEmail);

    List<PayrollResponseDto> getEmployeePayrollHistory(Long employeeId, String name);

    List<PayrollResponseDto> getPayrollsByFilters(YearMonth period, Boolean isPaid, String name);

    PayrollStatsDto getPayrollStats(YearMonth period, String callerEmail);
}
