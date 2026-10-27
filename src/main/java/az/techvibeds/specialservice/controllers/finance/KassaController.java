package az.techvibeds.specialservice.controllers.finance;

import az.techvibeds.specialservice.dtos.finance.TransactionReadDto;
import az.techvibeds.specialservice.dtos.kassa.KassaIncomeDto;
import az.techvibeds.specialservice.dtos.kassa.KassaExpenseDto;
import az.techvibeds.specialservice.dtos.kassa.KassaTransferDto;
import az.techvibeds.specialservice.services.KassaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/kassa")
public class KassaController {

    private final KassaService kassaService;

    @PostMapping("/income")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR', 'MODULE:FINANCE:SUPER_ADMIN', 'MODULE:FINANCE:ADMIN', 'MODULE:FINANCE:CREATOR')")
    public ResponseEntity<TransactionReadDto> income(@RequestBody KassaIncomeDto dto, Principal principal) {
        return ResponseEntity.ok(kassaService.processIncome(dto, principal.getName()));
    }

    @PostMapping("/expense")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR', 'MODULE:FINANCE:SUPER_ADMIN', 'MODULE:FINANCE:ADMIN', 'MODULE:FINANCE:CREATOR')")
    public ResponseEntity<TransactionReadDto> expense(@RequestBody KassaExpenseDto dto, Principal principal) {
        return ResponseEntity.ok(kassaService.processExpense(dto, principal.getName()));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR', 'MODULE:FINANCE:SUPER_ADMIN', 'MODULE:FINANCE:ADMIN', 'MODULE:FINANCE:CREATOR')")
    public ResponseEntity<TransactionReadDto> transfer(@RequestBody KassaTransferDto dto, Principal principal) {
        return ResponseEntity.ok(kassaService.processTransfer(dto, principal.getName()));
    }

    @GetMapping("/history/{accountId}")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR', 'MODULE:FINANCE:SUPER_ADMIN', 'MODULE:FINANCE:ADMIN', 'MODULE:FINANCE:CREATOR')")
    public ResponseEntity<List<TransactionReadDto>> getHistory(@PathVariable Long accountId, Principal principal) {
        return ResponseEntity.ok(kassaService.getKassaHistory(principal.getName(), accountId));
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:FINANCE:SUPER_ADMIN', 'MODULE:FINANCE:ADMIN')")
    public ResponseEntity<TransactionReadDto> approveTransaction(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(kassaService.approveTransaction(id, principal.getName()));
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:FINANCE:SUPER_ADMIN', 'MODULE:FINANCE:ADMIN')")
    public ResponseEntity<TransactionReadDto> rejectTransaction(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(kassaService.rejectTransaction(id, principal.getName()));
    }
}
