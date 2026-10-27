package az.techvibeds.specialservice.controllers.invoice;

import az.techvibeds.specialservice.dtos.invoice.*;
import az.techvibeds.specialservice.enums.InvoiceStatus;
import az.techvibeds.specialservice.enums.InvoiceType;
import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.services.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.util.Map;

/**
 * Faktura (Invoice) Controller.
 * Satış & Alış İdarəetmənin "Fakturalar" tab-ı üçün bütün endpoint-lər.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    // ═══════════════════════════════════════════════════════════
    //  YARATMA
    // ═══════════════════════════════════════════════════════════

    /**
     * Yeni faktura yaradır.
     * POST /api/v1/invoices
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR')")
    public ResponseEntity<InvoiceReadDto> createInvoice(
            @Valid @RequestBody InvoiceCreateDto dto,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        InvoiceReadDto created = invoiceService.createInvoice(dto, email);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Skan edilmiş barkod siyahısı ilə birbaşa yeni qaimə yaradır (kassa sistemi kimi).
     * POST /api/v1/invoices/scan-create
     */
    @PostMapping("/scan-create")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR')")
    public ResponseEntity<InvoiceReadDto> createInvoiceFromScan(
            @Valid @RequestBody ScanInvoiceCreateDto dto,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        InvoiceReadDto created = invoiceService.createInvoiceFromScan(dto, email);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // ═══════════════════════════════════════════════════════════
    //  OXUMA
    // ═══════════════════════════════════════════════════════════

    /**
     * Satış faturalarını gətirir (filtr + pagination).
     * GET /api/v1/invoices/sales?status=DRAFT&page=0&size=10
     */
    @GetMapping("/sales")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:VIEWER', 'MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR')")
    public ResponseEntity<Page<InvoiceReadDto>> getSalesInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Principal principal) {

        String email = (principal != null) ? principal.getName() : "test@example.com";

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<InvoiceReadDto> invoices = invoiceService.getSalesInvoices(status, keyword, pageable, email);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Alış faturalarını gətirir (filtr + pagination).
     * GET /api/v1/invoices/purchases?status=DRAFT&page=0&size=10
     */
    @GetMapping("/purchases")
    @PreAuthorize("hasAnyAuthority('MODULE:PURCHASING:VIEWER', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR')")
    public ResponseEntity<Page<InvoiceReadDto>> getPurchaseInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Principal principal) {

        String email = (principal != null) ? principal.getName() : "test@example.com";

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<InvoiceReadDto> invoices = invoiceService.getPurchaseInvoices(status, keyword, pageable, email);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Kassa üçün ödənilməmiş (payable) qaimələri gətirir (SALES və ya PURCHASE).
     * GET /api/v1/invoices/payable?type=SALES&page=0&size=10
     */
    @GetMapping("/payable")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:VIEWER', 'MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:VIEWER', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR')")
    public ResponseEntity<Page<InvoiceReadDto>> getPayableInvoices(
            @RequestParam InvoiceType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<InvoiceReadDto> invoices = invoiceService.getPayableInvoices(type, pageable, email);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Fakturanı ID-yə görə gətirir (detallar + sətirləri ilə).
     * GET /api/v1/invoices/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:VIEWER', 'MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:VIEWER', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR')")
    public ResponseEntity<InvoiceReadDto> getInvoiceById(
            @PathVariable Long id,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        InvoiceReadDto invoice = invoiceService.getInvoiceById(id, email);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Fakturanın 80mm PDF formatında qəbzini (çekini) generasiya edir.
     * GET /api/v1/invoices/{id}/receipt
     */
    @GetMapping("/{id}/receipt")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:VIEWER', 'MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:VIEWER', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR')")
    public ResponseEntity<byte[]> getReceiptPdf(
            @PathVariable Long id,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        byte[] pdfBytes = invoiceService.generateReceiptPdf(id, email);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add("Content-Disposition", "attachment; filename=receipt_" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // ═══════════════════════════════════════════════════════════
    //  YENİLƏMƏ
    // ═══════════════════════════════════════════════════════════

    /**
     * Mövcud fakturanı yeniləyir (yalnız DRAFT statusunda).
     * PUT /api/v1/invoices
     */
    @PutMapping
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN')")
    public ResponseEntity<InvoiceReadDto> updateInvoice(
            @Valid @RequestBody InvoiceUpdateDto dto,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        InvoiceReadDto updated = invoiceService.updateInvoice(dto, email);
        return ResponseEntity.ok(updated);
    }

    // ═══════════════════════════════════════════════════════════
    //  STATUS DƏYİŞMƏ
    // ═══════════════════════════════════════════════════════════

    /**
     * Fakturanın statusunu dəyişir.
     * PATCH /api/v1/invoices/{id}/status?newStatus=SENT
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN')")
    public ResponseEntity<?> changeStatus(
            @PathVariable Long id,
            @RequestParam InvoiceStatus newStatus,
            Principal principal) {
        try {
            String email = (principal != null) ? principal.getName() : "test@example.com";
            InvoiceReadDto updated = invoiceService.changeStatus(id, newStatus, email);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SİLMƏ
    // ═══════════════════════════════════════════════════════════

    /**
     * Fakturanı silir (ödənilmiş faktura silinə bilməz).
     * DELETE /api/v1/invoices/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:PURCHASING:SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> deleteInvoice(
            @PathVariable Long id,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        invoiceService.deleteInvoice(id, email);
        return ResponseEntity.ok(new ApiResponse("Faktura silindi", true));
    }

    // ═══════════════════════════════════════════════════════════
    //  SƏTİR ƏMƏLİYYATLARI
    // ═══════════════════════════════════════════════════════════

    /**
     * Fakturaya yeni sətir (məhsul/xidmət) əlavə edir.
     * POST /api/v1/invoices/{id}/items
     */
    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR')")
    public ResponseEntity<InvoiceReadDto> addItem(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceItemDto itemDto,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        InvoiceReadDto updated = invoiceService.addItem(id, itemDto, email);
        return ResponseEntity.ok(updated);
    }

    /**
     * Sürətli barkod skan edilməsi üçün endpoint (JSON tələb etmir).
     * POST /api/v1/invoices/{id}/items/scan
     */
    @PostMapping("/{id}/items/scan")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR')")
    public ResponseEntity<InvoiceReadDto> scanItem(
            @PathVariable Long id,
            @RequestParam String barcode,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam Long warehouseId,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        InvoiceReadDto updated = invoiceService.scanItem(id, barcode, quantity, warehouseId, email);
        return ResponseEntity.ok(updated);
    }

    /**
     * Fakturadan sətir silir.
     * DELETE /api/v1/invoices/{invoiceId}/items/{itemId}
     */
    @DeleteMapping("/{invoiceId}/items/{itemId}")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN')")
    public ResponseEntity<InvoiceReadDto> removeItem(
            @PathVariable Long invoiceId,
            @PathVariable Long itemId,
            Principal principal) {
        String email = (principal != null) ? principal.getName() : "test@example.com";
        InvoiceReadDto updated = invoiceService.removeItem(invoiceId, itemId, email);
        return ResponseEntity.ok(updated);
    }

    // ═══════════════════════════════════════════════════════════
    //  EXCEL İDXAL / İXRAC
    // ═══════════════════════════════════════════════════════════

    /**
     * Faturaları Excel-ə export edir.
     * GET /api/v1/invoices/export?type=SALES
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:VIEWER', 'MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:VIEWER', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR')")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) InvoiceType type,
            Principal principal) throws Exception {

        String email = (principal != null) ? principal.getName() : "test@example.com";
        ByteArrayInputStream in = invoiceService.exportToExcel(type, email);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=invoices.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(in.readAllBytes());
    }

    /**
     * Excel-dən faktura sətirləri idxal edir.
     * POST /api/v1/invoices/{id}/import-items
     */
    @PostMapping(value = "/{id}/import-items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR')")
    public ResponseEntity<?> importItemsFromExcel(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        try {
            String email = (principal != null) ? principal.getName() : "test@example.com";
            InvoiceReadDto updated = invoiceService.importItemsFromExcel(id, file, email);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Fakturaya ödəniş əlavə edir.
     * POST /api/v1/invoices/{id}/pay
     */
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:SALES:CREATOR', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN', 'MODULE:PURCHASING:CREATOR')")
    public ResponseEntity<?> payInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoicePaymentDto dto,
            Principal principal) {
        try {
            String email = (principal != null) ? principal.getName() : "test@example.com";
            InvoiceReadDto updated = invoiceService.payInvoice(id, dto, email);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Faktura ödənişini geri alır (refund).
     * POST /api/v1/invoices/{id}/refund/{transactionId}
     */
    @PostMapping("/{id}/refund/{transactionId}")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN')")
    public ResponseEntity<?> refundPayment(
            @PathVariable Long id,
            @PathVariable Long transactionId,
            Principal principal) {
        try {
            String email = (principal != null) ? principal.getName() : "test@example.com";
            InvoiceReadDto updated = invoiceService.refundPayment(id, transactionId, email);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  QAİMƏ LƏĞVİ (CANCEL WITH REFUND)
    // ═══════════════════════════════════════════════════════════

    /**
     * Qaiməni ləğv edir və ödənilmiş məbləği kassadan geri qaytarır.
     * POST /api/v1/invoices/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN')")
    public ResponseEntity<?> cancelInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceCancelDto dto,
            Principal principal) {
        try {
            String email = (principal != null) ? principal.getName() : "test@example.com";
            InvoiceReadDto cancelled = invoiceService.cancelInvoice(id, dto, email);
            return ResponseEntity.ok(cancelled);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  QİSMƏN QAYTARILMA (PARTIAL RETURN)
    // ═══════════════════════════════════════════════════════════

    /**
     * Qaimədəki müəyyən miqdarda məhsulu geri qaytarır, pulu kassadan geri ödəyir.
     * POST /api/v1/invoices/{id}/partial-return
     */
    @PostMapping("/{id}/partial-return")
    @PreAuthorize("hasAnyAuthority('MODULE:SALES:SUPER_ADMIN', 'MODULE:SALES:ADMIN', 'MODULE:PURCHASING:SUPER_ADMIN', 'MODULE:PURCHASING:ADMIN')")
    public ResponseEntity<?> partialReturn(
            @PathVariable Long id,
            @Valid @RequestBody InvoicePartialReturnDto dto,
            Principal principal) {
        try {
            String email = (principal != null) ? principal.getName() : "test@example.com";
            InvoiceReadDto updated = invoiceService.partialReturn(id, dto, email);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage()));
        }
    }
}

