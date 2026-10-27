package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.assignee.*;
import az.techvibeds.specialservice.models.Assignee;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.repositories.AssigneeRepository;
import az.techvibeds.specialservice.services.AssigneeService;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.FileService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssigneeServiceImpl implements AssigneeService {
    private final ModelMapper modelMapper;
    private final AssigneeRepository assigneeRepository;
    private final CompanyService companyService;
    private final FileService fileService;

    @Value("${project.image.path:uploads/assignees}")
    private String path;

    @Override
    public List<AssigneeServiceDto> getAssigneeByCompany(String email) {
        log.info("Fetching all assignees for company email: {}", email);
        Company company = findCompanyByEmail(email);

        return assigneeRepository.findAllByCompany(company).stream()
                .map(assignee -> modelMapper.map(assignee, AssigneeServiceDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public Assignee findAssigneeById(Long assigneeId) {
        return assigneeRepository.findById(assigneeId)
                .orElseThrow(() -> {
                    log.error("Assignee not found with ID: {}", assigneeId);
                    return new EntityNotFoundException("Assignee cannot be found");
                });
    }

    @Override
    public AssigneeDetailDto findAssigneeByIdDetailDto(Long id, String userEmail) {
        Assignee assignee = findAssigneeById(id);
        validateOwnership(assignee, userEmail);

        return modelMapper.map(assignee, AssigneeDetailDto.class);
    }

    @Override
    @Transactional
    public AssigneeReadDto create(AssigneeCreateDto dto, String email) {
        log.info("Creating new assignee: {} for company: {}", dto.getName(), email);
        Company company = findCompanyByEmail(email);

        Assignee assignee = new Assignee();
        assignee.setName(dto.getName());
        assignee.setTotalCapacity(dto.getTotalCapacity());
        assignee.setActiveServiceCount(0);
        assignee.setCompany(company);

        Assignee savedAssignee = assigneeRepository.save(assignee);
        return modelMapper.map(savedAssignee, AssigneeReadDto.class);
    }

    @Override
    @Transactional
    public AssigneeReadDto update(AssigneeUpdateDto dto, String userEmail) {
        log.info("Updating assignee ID: {}", dto.getId());
        Assignee assignee = findAssigneeById(dto.getId());
        validateOwnership(assignee, userEmail);

        assignee.setName(dto.getName());
        assignee.setTotalCapacity(dto.getTotalCapacity());
        assignee.setActiveServiceCount(dto.getActiveServiceCount());

        Assignee savedAssignee = assigneeRepository.save(assignee);
        return modelMapper.map(savedAssignee, AssigneeReadDto.class);
    }

    @Override
    @Transactional
    public AssigneeReadDto uploadImage(Long id, MultipartFile image, String userEmail) throws IOException {
        log.info("Uploading image for assignee ID: {}", id);
        Assignee assignee = findAssigneeById(id);
        validateOwnership(assignee, userEmail);

        if (image != null && !image.isEmpty()) {
            // Köhnə şəkil varsa sil
            if (assignee.getImageUrl() != null) {
                fileService.deleteFile(path, assignee.getImageUrl());
            }
            String fileName = fileService.uploadImage(path, image);
            assignee.setImageUrl(fileName);
        }

        Assignee savedAssignee = assigneeRepository.save(assignee);
        return modelMapper.map(savedAssignee, AssigneeReadDto.class);
    }

    @Override
    @Transactional
    public void delete(Long id, String userEmail) {
        log.info("Deleting assignee ID: {} by user: {}", id, userEmail);
        Assignee assignee = findAssigneeById(id);
        validateOwnership(assignee, userEmail);

        // Şəkli də sil
        if (assignee.getImageUrl() != null) {
            fileService.deleteFile(path, assignee.getImageUrl());
        }

        assigneeRepository.delete(assignee);
    }

    private void validateOwnership(Assignee assignee, String email) {
        Company company = findCompanyByEmail(email);
        if (!assignee.getCompany().getId().equals(company.getId())) {
            log.warn("Security alert! User {} tried to access assignee ID {}", email, assignee.getId());
            throw new AccessDeniedException("You do not have permission for this assignee");
        }
    }

    private Company findCompanyByEmail(String email) {
        return companyService.findByUserEmail(email);
    }
}