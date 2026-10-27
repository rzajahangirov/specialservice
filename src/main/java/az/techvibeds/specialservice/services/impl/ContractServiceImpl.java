package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.hrContract.ContractCreateDto;
import az.techvibeds.specialservice.dtos.hrContract.ContractResponseDto;
import az.techvibeds.specialservice.dtos.hrContract.ContractUpdateDto;
import az.techvibeds.specialservice.payloads.EnumResponseDto;
import az.techvibeds.specialservice.enums.ContractType;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.Contract;
import az.techvibeds.specialservice.models.HREmployee;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.repositories.ContractRepository;
import az.techvibeds.specialservice.repositories.HREmployeeRepository;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.ContractService;
import az.techvibeds.specialservice.services.HREmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractServiceImpl implements ContractService {
    private final ContractRepository contractRepository;
    private final HREmployeeRepository hrEmployeeRepository;
    private final ModelMapper modelMapper;
    private final HREmployeeService employeeService;
    private final CompanyService companyService;

    @Override
    @Transactional
    public ContractResponseDto createContract(Long employeeId, ContractCreateDto request) {
        log.info("Creating new contract for employee ID: {}", employeeId);

        HREmployee employee = hrEmployeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        contractRepository.findByEmployeeIdAndIsActiveTrue(employeeId)
                .ifPresent(existingContract -> {
                    log.info("Deactivating old contract for employee ID: {}", employeeId);
                    existingContract.setActive(false);
                    contractRepository.save(existingContract);
                });

        Contract contract = modelMapper.map(request, Contract.class);


        contract.setEmployee(employee);
        contract.setActive(true);
        contract.setSalary(request.getSalary());

        Contract savedContract = contractRepository.save(contract);
        log.info("New contract successfully created with ID: {}", savedContract.getId());

        return mapToResponseDto(savedContract);
    }

    @Override
    public ResponseDto<ContractResponseDto> getEmployeeContract(Long employeeId, String email) {
        HREmployee employee = employeeService.getEmployeeIfAuthorized(employeeId, email);

        Contract contract = contractRepository.findByEmployeeIdAndIsActiveTrue(employee.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "employeeId", employeeId));

        ContractResponseDto contractDTO = modelMapper.map(contract, ContractResponseDto.class);
        contractDTO.setEmployeeId(employeeId);
        return new ResponseDto<>(contractDTO, "Active contract retrieved successfully");
    }

    @Override
    @Transactional
    public ContractResponseDto updateContract(Long contractId, ContractUpdateDto updateDTO, String currentUserEmail) {
        log.info("User {} is attempting to update contract ID: {}", currentUserEmail, contractId);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", contractId));

        authenticationCheck(currentUserEmail, contract.getEmployee().getCompany().getId());

        modelMapper.map(updateDTO, contract);

        Contract updatedContract = contractRepository.save(contract);
        log.info("Contract ID: {} successfully updated", contractId);

        return modelMapper.map(updatedContract, ContractResponseDto.class);
    }

    @Override
    public ResponseDto<List<EnumResponseDto>> getContractTypes() {
        List<EnumResponseDto> types = Arrays.stream(ContractType.values())
                .map(type -> new EnumResponseDto(
                        type.name(),
                        capitalizeFirstLetter(type.name().replace("_", " ").toLowerCase())
                ))
                .collect(Collectors.toList());

        return new ResponseDto<>(types, "Contract types retrieved successfully");
    }

    private String capitalizeFirstLetter(String original) {
        if (original == null || original.isEmpty()) return original;
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }

    private void authenticationCheck(String email, Long companyId) {
        Company company = companyService.findByUserEmail(email);

        if (!companyId.equals(company.getId())) {
            log.error("Unauthorized contract update attempt by user {}", email);
            throw new AccessDeniedException("You do not have permission to update this contract");
        }
    }


    private ContractResponseDto mapToResponseDto(Contract entity) {
        return ContractResponseDto.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee().getId())
                .contractType(entity.getContractType())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .salary(entity.getSalary())
                .documentUrl(entity.getDocumentUrl())
                .isActive(entity.isActive())
                .build();
    }
}
