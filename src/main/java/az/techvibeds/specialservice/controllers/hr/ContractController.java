package az.techvibeds.specialservice.controllers.hr;

import az.techvibeds.specialservice.dtos.hrContract.ContractCreateDto;
import az.techvibeds.specialservice.dtos.hrContract.ContractResponseDto;
import az.techvibeds.specialservice.dtos.hrContract.ContractUpdateDto;
import az.techvibeds.specialservice.payloads.EnumResponseDto;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.services.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/hr/contract")
public class ContractController {
    private final ContractService contractService;

    @PostMapping("/{id}/contracts")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<ContractResponseDto> createContract(
            @PathVariable Long id,
            @Valid @RequestBody ContractCreateDto request) {

        ContractResponseDto response = contractService.createContract(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/active-contract")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<ResponseDto<ContractResponseDto>> getContract(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(contractService.getEmployeeContract(id, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN')")
    public ResponseEntity<ResponseDto<ContractResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody ContractUpdateDto updateDTO,
            Principal principal) {

        ContractResponseDto response = contractService.updateContract(id, updateDTO, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(response, "Contract updated successfully"));
    }

    @GetMapping("/types")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<ResponseDto<List<EnumResponseDto>>> getContractTypes() {
        return ResponseEntity.ok(contractService.getContractTypes());
    }
}
