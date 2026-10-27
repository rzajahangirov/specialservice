package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.hrContract.ContractCreateDto;
import az.techvibeds.specialservice.dtos.hrContract.ContractResponseDto;
import az.techvibeds.specialservice.dtos.hrContract.ContractUpdateDto;
import az.techvibeds.specialservice.payloads.EnumResponseDto;
import az.techvibeds.specialservice.payloads.ResponseDto;
import jakarta.validation.Valid;

import java.util.List;

public interface ContractService {
    ContractResponseDto createContract(Long id, @Valid ContractCreateDto request);

    ResponseDto<ContractResponseDto> getEmployeeContract(Long id, String name);

    ContractResponseDto updateContract(Long id, @Valid ContractUpdateDto updateDTO, String name);

    ResponseDto<List<EnumResponseDto>> getContractTypes();
}
