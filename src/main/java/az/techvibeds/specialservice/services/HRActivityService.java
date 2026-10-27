package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.hrActivity.HRActivityCreateDto;
import az.techvibeds.specialservice.dtos.hrActivity.HRActivityResponseDto;
import jakarta.validation.Valid;

import java.util.List;

public interface HRActivityService {
    HRActivityResponseDto createActivity(@Valid HRActivityCreateDto request, String name);

    List<HRActivityResponseDto> getEmployeeActivities(Long employeeId, String name);

    List<HRActivityResponseDto> getCompanyActivityFeed(String name);
}
