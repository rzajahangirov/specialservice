package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.invoice.*;
import az.techvibeds.specialservice.enums.InvoiceStatus;
import az.techvibeds.specialservice.enums.InvoiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

public interface InvoiceService {

    /** Yeni faktura yaradır (avtomatik nömrə ilə) */
    InvoiceReadDto createInvoice(InvoiceCreateDto dto, String userEmail);

    /** Skan edilmiş barkod siyahısı ilə birbaşa yeni faktura yaradır */
    InvoiceReadDto createInvoiceFromScan(ScanInvoiceCreateDto dto, String userEmail);

    /** Fakturanı yeniləyir */
    InvoiceReadDto updateInvoice(InvoiceUpdateDto dto, String userEmail);

    /** Fakturanı ID-yə görə gətirir */
    InvoiceReadDto getInvoiceById(Long id, String userEmail);

    /** Şirkətin bütün fakturalarını gətirir (filtrlərlə) */
    Page<InvoiceReadDto> getInvoices(InvoiceType type, InvoiceStatus status,
                                     String keyword, Pageable pageable, String userEmail);

    /** Yalnız satış (SALES) qaimələrini gətirir */
    Page<InvoiceReadDto> getSalesInvoices(InvoiceStatus status, String keyword, Pageable pageable, String userEmail);

    /** Yalnız alış (PURCHASE) qaimələrini gətirir */
    Page<InvoiceReadDto> getPurchaseInvoices(InvoiceStatus status, String keyword, Pageable pageable, String userEmail);

    /** Faktura statusunu dəyişir */
    InvoiceReadDto changeStatus(Long invoiceId, InvoiceStatus newStatus, String userEmail);

    /** Fakturanı silir */
    void deleteInvoice(Long id, String userEmail);

    /** Fakturaya yeni sətir əlavə edir */
    InvoiceReadDto addItem(Long invoiceId, InvoiceItemDto itemDto, String userEmail);

    /** Fakturaya barkod oxutmaqla məhsul əlavə edir (sürətli skan) */
    InvoiceReadDto scanItem(Long invoiceId, String barcode, Integer quantity, Long warehouseId, String userEmail);

    /** Fakturadan sətir silir */
    InvoiceReadDto removeItem(Long invoiceId, Long itemId, String userEmail);

    /** Faturaları Excel-ə export edir */
    ByteArrayInputStream exportToExcel(InvoiceType type, String userEmail) throws Exception;

    /** Excel-dən faktura sətirləri idxal edir */
    InvoiceReadDto importItemsFromExcel(Long invoiceId, MultipartFile file, String userEmail) throws Exception;

    /** Fakturaya ödəniş daxil edir */
    InvoiceReadDto payInvoice(Long invoiceId, InvoicePaymentDto paymentDto, String userEmail);

    /** Fakturadakı ödənişi geri alır (refund) */
    InvoiceReadDto refundPayment(Long invoiceId, Long transactionId, String userEmail);

    /** Şirkət və növ üzrə ödənilməmiş (payable) fakturaları gətirir */
    Page<InvoiceReadDto> getPayableInvoices(InvoiceType type, Pageable pageable, String userEmail);

    /** Qaiməni refund ilə ləğv edir */
    InvoiceReadDto cancelInvoice(Long invoiceId, InvoiceCancelDto dto, String userEmail);

    /** Qaimədən müəyyən miqdarda məhsulu geri qaytarır */
    InvoiceReadDto partialReturn(Long invoiceId, InvoicePartialReturnDto dto, String userEmail);

    /** Qaimənin 80mm PDF çek formatında qəbzini generasiya edir */
    byte[] generateReceiptPdf(Long invoiceId, String userEmail);
}
