package az.techvibeds.specialservice.controllers;

import az.techvibeds.specialservice.dtos.foreignPurchase.ForeignPurchaseCreateDto;
import az.techvibeds.specialservice.dtos.foreignPurchase.ForeignPurchaseReadDto;
import az.techvibeds.specialservice.services.ForeignPurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/foreign-purchase")
@RequiredArgsConstructor
public class ForeignPurchaseController {

    private final ForeignPurchaseService purchaseService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ForeignPurchaseReadDto> createPurchase(
            @RequestBody ForeignPurchaseCreateDto dto, Principal principal) {
        return ResponseEntity.ok(purchaseService.createPurchase(dto, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ForeignPurchaseReadDto>> getPurchases(
            @org.springdoc.core.annotations.ParameterObject Pageable pageable, 
            Principal principal) {
        return ResponseEntity.ok(purchaseService.getPurchases(pageable, principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ForeignPurchaseReadDto> getPurchaseById(
            @PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(purchaseService.getPurchaseById(id, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePurchase(@PathVariable Long id, Principal principal) {
        purchaseService.deletePurchase(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ForeignPurchaseReadDto> importExcel(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal) throws Exception {
        return ResponseEntity.ok(purchaseService.importItemsFromExcel(id, file, principal.getName()));
    }
}
