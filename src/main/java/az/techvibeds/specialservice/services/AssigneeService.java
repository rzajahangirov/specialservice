package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.assignee.*;
import az.techvibeds.specialservice.models.Assignee;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface AssigneeService {
    List<AssigneeServiceDto> getAssigneeByCompany(String email);

    Assignee findAssigneeById(Long assigneeId);


    AssigneeDetailDto findAssigneeByIdDetailDto(Long id, String userEmail);

    AssigneeReadDto create(AssigneeCreateDto dto, String email);

    AssigneeReadDto update(AssigneeUpdateDto dto, String userEmail);

    AssigneeReadDto uploadImage(Long id, MultipartFile image, String userEmail) throws IOException;

    void delete(Long id, String userEmail);

}
