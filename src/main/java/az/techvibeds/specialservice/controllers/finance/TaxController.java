package az.techvibeds.specialservice.controllers.finance;

import az.techvibeds.specialservice.dtos.finance.TaxCalculateRequestDto;
import az.techvibeds.specialservice.dtos.finance.TaxCalculateResponseDto;
import az.techvibeds.specialservice.dtos.finance.TaxPaymentRequestDto;
import az.techvibeds.specialservice.dtos.finance.TaxPeriodType;
import az.techvibeds.specialservice.dtos.finance.TaxXmlExportRequestDto;
import az.techvibeds.specialservice.services.TaxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/finance/tax")
public class TaxController {

    private final TaxService taxService;

    @PostMapping("/calculate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaxCalculateResponseDto> calculateTax(@RequestBody TaxCalculateRequestDto request,
                                                                Principal principal) {
        return ResponseEntity.ok(taxService.calculateTax(request, principal.getName()));
    }

    @PostMapping("/pay")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaxCalculateResponseDto> payTax(@RequestBody TaxPaymentRequestDto request,
                                                          Principal principal) {
        return ResponseEntity.ok(taxService.payTax(request, principal.getName()));
    }

    @GetMapping("/export/xml")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportXml(
            Principal principal,
            @RequestParam(defaultValue = "MONTHLY") TaxPeriodType periodType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal taxRate) {
        TaxXmlExportRequestDto request = new TaxXmlExportRequestDto();
        request.setPeriodType(periodType);
        request.setReferenceDate(referenceDate);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setTaxRate(taxRate);
        byte[] xmlBytes = taxService.exportXml(request, principal.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tax-report.xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xmlBytes);
    }
}
