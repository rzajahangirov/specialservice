package az.techvibeds.specialservice.controllers;

import az.techvibeds.specialservice.dtos.foreignCurrency.ForeignCurrencyPurchaseCreateDto;
import az.techvibeds.specialservice.dtos.foreignCurrency.ForeignCurrencyPurchaseReadDto;
import az.techvibeds.specialservice.services.ForeignCurrencyPurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/foreign-currency")
@RequiredArgsConstructor
public class ForeignCurrencyPurchaseController {

    private final ForeignCurrencyPurchaseService purchaseService;

    @PostMapping("/purchase")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ForeignCurrencyPurchaseReadDto> createPurchase(
            @RequestBody ForeignCurrencyPurchaseCreateDto dto, Principal principal) {
        return ResponseEntity.ok(purchaseService.createPurchase(dto, principal.getName()));
    }

    @GetMapping("/purchases")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ForeignCurrencyPurchaseReadDto>> getPurchases(
            @org.springdoc.core.annotations.ParameterObject Pageable pageable, 
            Principal principal) {
        return ResponseEntity.ok(purchaseService.getPurchases(pageable, principal.getName()));
    }

    @GetMapping("/purchase/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ForeignCurrencyPurchaseReadDto> getPurchaseById(
            @PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(purchaseService.getPurchaseById(id, principal.getName()));
    }

    @DeleteMapping("/purchase/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePurchase(@PathVariable Long id, Principal principal) {
        purchaseService.deletePurchase(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
