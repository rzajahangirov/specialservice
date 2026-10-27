package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.invoice.*;
import az.techvibeds.specialservice.enums.ActivityTypeWarehouse;
import az.techvibeds.specialservice.enums.InvoiceStatus;
import az.techvibeds.specialservice.enums.InvoiceType;
import az.techvibeds.specialservice.enums.PartnerType;
import az.techvibeds.specialservice.enums.ProductStatus;
import az.techvibeds.specialservice.enums.ProductType;
import az.techvibeds.specialservice.enums.VatStatus;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.repositories.InvoiceRepository;
import az.techvibeds.specialservice.repositories.InvoiceSequenceRepository;
import az.techvibeds.specialservice.repositories.PartnerRepository;
import az.techvibeds.specialservice.repositories.ProductRepository;
import az.techvibeds.specialservice.repositories.WarehouseActivityRepository;
import az.techvibeds.specialservice.repositories.WarehouseProductRepository;
import az.techvibeds.specialservice.repositories.*;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.InvoiceService;
import az.techvibeds.specialservice.services.NotificationService;
import az.techvibeds.specialservice.enums.TransactionType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceSequenceRepository invoiceSequenceRepository;
    private final PartnerRepository partnerRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseProductRepository warehouseProductRepository;
    private final WarehouseActivityRepository warehouseActivityRepository;
    private final CompanyService companyService;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BalanceRepository balanceRepository;
    private final NotificationService notificationService;
    private final SequenceGeneratorService sequenceGeneratorService;

    // --- PURCHASE INVOICE SECURITY CHECK ---
    private void checkPurchaseInvoiceAccess(InvoiceType type) {
        if (type == InvoiceType.PURCHASE) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                boolean hasPurchasingRole = auth.getAuthorities().stream()
                        .anyMatch(a -> {
                            String role = a.getAuthority();
                            return role.startsWith("MODULE:PURCHASING:") ||
                                   role.equals("ROLE_COMPANY_ADMIN") ||
                                   role.equals("ROLE_GLOBAL_ADMIN");
                        });
                if (!hasPurchasingRole) {
                    throw new AccessDeniedException("Alış qaimələrinə (PURCHASE) baxmaq və ya yaratmaq üçün PURCHASING modul hüququnuz olmalıdır.");
                }
            }
        }
    }
    
    private boolean hasPurchaseInvoiceAccess() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> {
                    String role = a.getAuthority();
                    return role.startsWith("MODULE:PURCHASING:") ||
                           role.equals("ROLE_COMPANY_ADMIN") ||
                           role.equals("ROLE_GLOBAL_ADMIN");
                });
    }
    // ---------------------------------------

    // ═══════════════════════════════════════════════════════════

    //  YARATMA
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public InvoiceReadDto createInvoice(InvoiceCreateDto dto, String userEmail) {
        // Alış qaiməsi üçün xüsusi rol yoxlaması
        checkPurchaseInvoiceAccess(dto.getInvoiceType());

        Company company = companyService.findByUserEmail(userEmail);

        // Partner-i tap və şirkətə aidliyini yoxla
        Partner partner = partnerRepository.findById(dto.getPartnerId())
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("Partner", "id", dto.getPartnerId()));

        if (!partner.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Bu partner sizin şirkətə aid deyil");
        }

        // Qaimə növü ilə partnyor növünün uyğunluğunu yoxla
        if (dto.getInvoiceType() == InvoiceType.SALES && partner.getPartnerType() != PartnerType.CUSTOMER) {
            throw new BusinessException("Satış qaiməsi yalnız müştərilər (CUSTOMER) üçün yaradıla bilər.");
        }
        if (dto.getInvoiceType() == InvoiceType.PURCHASE && partner.getPartnerType() != PartnerType.SUPPLIER) {
            throw new BusinessException("Alış qaiməsi yalnız təchizatçılar (SUPPLIER) üçün yaradıla bilər.");
        }

        // Faktura nömrəsini avtomatik generasiya et
        String invoiceNumber = generateInvoiceNumber(company);

        // Invoice entity-ni qur
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceType(dto.getInvoiceType());
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setInvoiceDate(dto.getInvoiceDate());
        invoice.setDeliveryDate(dto.getDeliveryDate());
        invoice.setPaymentMethod(dto.getPaymentMethod());
        invoice.setCurrency(dto.getCurrency());
        invoice.setNotes(dto.getNotes());
        invoice.setAdditionalExpense(dto.getAdditionalExpense() != null ? dto.getAdditionalExpense() : BigDecimal.ZERO);
        invoice.setAdditionalExpenseType(dto.getAdditionalExpenseType() != null ? dto.getAdditionalExpenseType() : az.techvibeds.specialservice.enums.AdditionalExpenseType.AMOUNT);
        invoice.setDiscount(dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO);
        invoice.setDiscountType(dto.getDiscountType() != null ? dto.getDiscountType() : az.techvibeds.specialservice.enums.AdditionalExpenseType.AMOUNT);
        invoice.setPartner(partner);
        invoice.setCompany(company);

        // Sətirləri əlavə et (eyni barkodlu olanları qruplaşdırıb miqdarını toplayırıq)
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            java.util.List<InvoiceItemDto> mergedItems = mergeDuplicateItemDtos(dto.getItems());
            for (InvoiceItemDto itemDto : mergedItems) {
                InvoiceItem item = mapToInvoiceItem(itemDto, company);
                invoice.addItem(item);
            }
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return mapToReadDto(savedInvoice);
    }

    @Override
    @Transactional
    public InvoiceReadDto createInvoiceFromScan(ScanInvoiceCreateDto dto, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);

        // 1. Resolve InvoiceType (default to SALES)
        InvoiceType invoiceType = dto.getInvoiceType() != null ? dto.getInvoiceType() : InvoiceType.SALES;

        // 2. Resolve PartnerId
        Long partnerId = dto.getPartnerId();
        if (partnerId == null) {
            PartnerType partnerType = (invoiceType == InvoiceType.SALES) ? PartnerType.CUSTOMER : PartnerType.SUPPLIER;
            List<Partner> partners = partnerRepository.findAllByCompanyIdAndPartnerType(company.getId(), partnerType);
            if (partners.isEmpty()) {
                throw new BusinessException("Skan edilmiş qaimə yaratmaq üçün şirkətinizə aid heç bir aktiv partnyor (müştəri/təchizatçı) tapılmadı.");
            }
            partnerId = partners.get(0).getId();
        }

        // 3. Resolve WarehouseId
        Long warehouseId = dto.getWarehouseId();
        if (warehouseId == null) {
            List<Warehouse> warehouses = warehouseRepository.findAllByCompany_Id(company.getId());
            if (warehouses.isEmpty()) {
                throw new BusinessException("Skan edilmiş qaimə yaratmaq üçün şirkətinizə aid heç bir anbar tapılmadı.");
            }
            warehouseId = warehouses.get(0).getId();
        }

        // 4. Transform ScanBarcodeItemDto into InvoiceItemDto
        java.util.ArrayList<InvoiceItemDto> itemDtos = new java.util.ArrayList<>();
        for (ScanBarcodeItemDto scanItem : dto.getItems()) {
            String cleanedBarcode = scanItem.getBarcode() != null ? scanItem.getBarcode().trim().replaceAll("[\\r\\n]", "") : "";
            if (cleanedBarcode.isEmpty()) {
                throw new BusinessException("Barkod boş ola bilməz!");
            }

            Product product = productRepository.findByProductCodeAndCompanyId(cleanedBarcode, company.getId())
                    .orElseThrow(() -> new BusinessException("Bu barkoda uyğun məhsul tapılmadı: " + cleanedBarcode, HttpStatus.NOT_FOUND));

            // Determine which warehouse this product is actually in (the one with the most stock)
            Long itemWarehouseId = warehouseId; // default fallback
            Optional<WarehouseProduct> warehouseProduct = warehouseProductRepository
                    .findFirstByProduct_IdAndWarehouse_Company_IdAndQuantityGreaterThanOrderByQuantityDesc(
                            product.getId(), company.getId(), 0);
            if (warehouseProduct.isPresent()) {
                itemWarehouseId = warehouseProduct.get().getWarehouse().getId();
            }

            InvoiceItemDto itemDto = new InvoiceItemDto();
            itemDto.setProductId(product.getId());
            itemDto.setBarcode(cleanedBarcode);
            itemDto.setQuantity(scanItem.getQuantity() != null ? scanItem.getQuantity() : 1);
            itemDto.setWarehouseId(itemWarehouseId);
            itemDto.setUnitPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
            itemDto.setName(product.getName());
            itemDto.setVatStatus(product.getVatStatus() != null ? product.getVatStatus().name() : "WITHOUT_VAT");
            itemDto.setVatRate(product.getVatRate());

            itemDtos.add(itemDto);
        }

        // 5. Build InvoiceCreateDto to delegate to createInvoice
        InvoiceCreateDto invoiceCreateDto = new InvoiceCreateDto();
        invoiceCreateDto.setInvoiceType(invoiceType);
        invoiceCreateDto.setInvoiceDate(LocalDate.now());
        invoiceCreateDto.setPartnerId(partnerId);
        invoiceCreateDto.setCurrency("AZN");
        invoiceCreateDto.setNotes(dto.getNotes());
        invoiceCreateDto.setItems(itemDtos);

        return createInvoice(invoiceCreateDto, userEmail);
    }

    // ═══════════════════════════════════════════════════════════
    //  YENİLƏMƏ
    // ═══════════════════════════════════════════════════════════


    @Override
    @Transactional
    public InvoiceReadDto updateInvoice(InvoiceUpdateDto dto, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        Invoice invoice = findInvoiceAndCheckAccess(dto.getId(), company);

        // Yalnız DRAFT statusundakı fakturalar yenilənə bilər
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new RuntimeException("Yalnız DRAFT statusundakı fakturalar yenilənə bilər. Hazırki status: "
                    + invoice.getStatus());
        }

        // Sahələri yenilə (null olmayanları)
        if (dto.getInvoiceType() != null) {
            invoice.setInvoiceType(dto.getInvoiceType());
        }
        if (dto.getInvoiceDate() != null) {
            invoice.setInvoiceDate(dto.getInvoiceDate());
        }
        if (dto.getDeliveryDate() != null) {
            invoice.setDeliveryDate(dto.getDeliveryDate());
        }
        if (dto.getPaymentMethod() != null) {
            invoice.setPaymentMethod(dto.getPaymentMethod());
        }
        if (dto.getCurrency() != null) {
            invoice.setCurrency(dto.getCurrency());
        }
        if (dto.getNotes() != null) {
            invoice.setNotes(dto.getNotes());
        }
        if (dto.getAdditionalExpense() != null) {
            invoice.setAdditionalExpense(dto.getAdditionalExpense());
        }
        if (dto.getAdditionalExpenseType() != null) {
            invoice.setAdditionalExpenseType(dto.getAdditionalExpenseType());
        }
        if (dto.getDiscount() != null) {
            invoice.setDiscount(dto.getDiscount());
        }
        if (dto.getDiscountType() != null) {
            invoice.setDiscountType(dto.getDiscountType());
        }

        // Partner dəyişdirilibsə
        if (dto.getPartnerId() != null) {
            Partner partner = partnerRepository.findById(dto.getPartnerId())
                    .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("Partner", "id", dto.getPartnerId()));
            if (!partner.getCompany().getId().equals(company.getId())) {
                throw new AccessDeniedException("Bu partner sizin şirkətə aid deyil");
            }
            invoice.setPartner(partner);
        }

        // Qaimə növü ilə partnyor növünün uyğunluğunu yoxla
        InvoiceType targetType = invoice.getInvoiceType();
        Partner targetPartner = invoice.getPartner();
        if (targetType != null && targetPartner != null) {
            if (targetType == InvoiceType.SALES && targetPartner.getPartnerType() != PartnerType.CUSTOMER) {
                throw new BusinessException("Satış qaiməsi yalnız müştərilər (CUSTOMER) üçün yaradıla bilər.");
            }
            if (targetType == InvoiceType.PURCHASE && targetPartner.getPartnerType() != PartnerType.SUPPLIER) {
                throw new BusinessException("Alış qaiməsi yalnız təchizatçılar (SUPPLIER) üçün yaradıla bilər.");
            }
        }

        // Sətirləri yenilə (əgər göndərilibsə — köhnələri silir, qruplaşdırıb yenilərini əlavə edir)
        if (dto.getItems() != null) {
            invoice.getItems().clear();
            java.util.List<InvoiceItemDto> mergedItems = mergeDuplicateItemDtos(dto.getItems());
            for (InvoiceItemDto itemDto : mergedItems) {
                InvoiceItem item = mapToInvoiceItem(itemDto, company);
                invoice.addItem(item);
            }
        }

        invoice.recalculateTotal();
        Invoice savedInvoice = invoiceRepository.save(invoice);
        return mapToReadDto(savedInvoice);
    }

    // ═══════════════════════════════════════════════════════════
    //  OXUMA
    // ═══════════════════════════════════════════════════════════

    @Override
    public InvoiceReadDto getInvoiceById(Long id, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        Invoice invoice = findInvoiceAndCheckAccess(id, company);
        checkPurchaseInvoiceAccess(invoice.getInvoiceType());
        return mapToReadDto(invoice);
    }

    @Override
    public Page<InvoiceReadDto> getInvoices(InvoiceType type, InvoiceStatus status,
                                            String keyword, Pageable pageable, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        
        if (type == InvoiceType.PURCHASE) {
            checkPurchaseInvoiceAccess(type);
        } else if (type == null && !hasPurchaseInvoiceAccess()) {
            // Əgər növ verilməyibsə və istifadəçinin Alış qaiməsini görməyə yetkisi yoxdursa,
            // onda yalnız Satış qaimələrini gətiririk (qarışıq gəlməsin deyə).
            type = InvoiceType.SALES;
        }

        Page<Invoice> invoices;

        if (company == null) {
            invoices = Page.empty();
        } else if (type != null && status != null) {
            invoices = invoiceRepository.findAllByCompanyAndInvoiceTypeAndStatus(company, type, status, pageable);
        } else if (type != null) {
            invoices = invoiceRepository.findAllByCompanyAndInvoiceType(company, type, pageable);
        } else if (status != null) {
            invoices = invoiceRepository.findAllByCompanyAndStatus(company, status, pageable);
        } else {
            invoices = invoiceRepository.findAllByCompany(company, pageable);
        }

        return invoices.map(this::mapToReadDto);
    }

    // ═══════════════════════════════════════════════════════════
    //  STATUS DƏYİŞMƏ
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public InvoiceReadDto changeStatus(Long invoiceId, InvoiceStatus newStatus, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        Invoice invoice = findInvoiceAndCheckAccess(invoiceId, company);

        validateStatusTransition(invoice.getStatus(), newStatus);

        if (newStatus == InvoiceStatus.CANCELLED) {
            if (invoice.getPaidAmount() != null && invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                throw new RuntimeException("Ödəniş edilmiş faktura ləğv edilə bilməz. Əvvəlcə ödənişləri silin.");
            }
            if (Boolean.TRUE.equals(invoice.getStockApplied())) {
                reverseInvoiceStock(invoice, company);
            }
        } else if (shouldApplyStock(newStatus) && !Boolean.TRUE.equals(invoice.getStockApplied())) {
            applyInvoiceStock(invoice, company);
        }

        invoice.setStatus(newStatus);
        Invoice savedInvoice = invoiceRepository.save(invoice);
        
        if (newStatus == InvoiceStatus.SENT) {
            notificationService.createEmailNotification(
                String.format("Qaimə (sifariş) tamamlandı/təsdiqləndi. Qaimə nömrəsi: %s", savedInvoice.getInvoiceNumber()),
                company
            );
        }
        
        return mapToReadDto(savedInvoice);
    }

    // ═══════════════════════════════════════════════════════════
    //  SİLMƏ
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void deleteInvoice(Long id, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        Invoice invoice = findInvoiceAndCheckAccess(id, company);

        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID) {
            throw new BusinessException("Ödənilmiş və ya qismən ödənilmiş qaimə silinə bilməz");
        }

        if (invoice.getPaidAmount() != null && invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Ödəniş edilmiş qaimə silinə bilməz. Əvvəlcə ödənişləri silin.");
        }

        if (Boolean.TRUE.equals(invoice.getStockApplied())) {
            throw new BusinessException("Stock is already applied. Cancel the invoice before deleting it.");
        }

        invoiceRepository.delete(invoice);
    }

    // ═══════════════════════════════════════════════════════════
    //  SƏTİR ƏMƏLİYYATLARI
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public InvoiceReadDto addItem(Long invoiceId, InvoiceItemDto itemDto, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        Invoice invoice = findInvoiceAndCheckAccess(invoiceId, company);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new RuntimeException("Yalnız DRAFT statusundakı fakturalara sətir əlavə oluna bilər");
        }

        InvoiceItem item = mapToInvoiceItem(itemDto, company);
        
        // Eyni barkodlu/məhsullu sətirin olub-olmadığını yoxla
        InvoiceItem existingItem = null;
        for (InvoiceItem ii : invoice.getItems()) {
            if (item.getProduct() != null && ii.getProduct() != null && ii.getProduct().getId().equals(item.getProduct().getId())) {
                existingItem = ii;
                break;
            } else if (item.getBarcode() != null && !item.getBarcode().isBlank() && item.getBarcode().trim().equals(ii.getBarcode())) {
                existingItem = ii;
                break;
            } else if (item.getName() != null && item.getName().trim().equals(ii.getName())) {
                existingItem = ii;
                break;
            }
        }

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
            existingItem.calculateLineTotal();
            invoice.recalculateTotal();
        } else {
            invoice.addItem(item);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return mapToReadDto(savedInvoice);
    }

    @Override
    @Transactional
    public InvoiceReadDto scanItem(Long invoiceId, String barcode, Integer quantity, Long warehouseId, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        
        // Skanerlərdən gələ biləcək boşluqları və sətir sonu simvollarını təmizləyirik
        String cleanedBarcode = barcode != null ? barcode.trim().replaceAll("[\\r\\n]", "") : "";
        if (cleanedBarcode.isEmpty()) {
            throw new BusinessException("Barkod boş ola bilməz!");
        }

        Product product = productRepository.findByProductCodeAndCompanyId(cleanedBarcode, company.getId())
                .orElseThrow(() -> new BusinessException("Bu barkoda uyğun məhsul tapılmadı: " + cleanedBarcode, HttpStatus.NOT_FOUND));

        InvoiceItemDto itemDto = new InvoiceItemDto();
        itemDto.setProductId(product.getId());
        itemDto.setBarcode(cleanedBarcode);
        itemDto.setQuantity(quantity != null ? quantity : 1);
        itemDto.setWarehouseId(warehouseId);
        itemDto.setUnitPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
        itemDto.setName(product.getName());
        itemDto.setVatStatus(product.getVatStatus() != null ? product.getVatStatus().name() : "WITHOUT_VAT");
        itemDto.setVatRate(product.getVatRate());

        return addItem(invoiceId, itemDto, userEmail);
    }

    @Override
    @Transactional
    public InvoiceReadDto removeItem(Long invoiceId, Long itemId, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        Invoice invoice = findInvoiceAndCheckAccess(invoiceId, company);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new RuntimeException("Yalnız DRAFT statusundakı fakturalardan sətir silinə bilər");
        }

        InvoiceItem itemToRemove = invoice.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("InvoiceItem", "id", itemId));

        invoice.removeItem(itemToRemove);
        Invoice savedInvoice = invoiceRepository.save(invoice);
        return mapToReadDto(savedInvoice);
    }

    // ═══════════════════════════════════════════════════════════
    //  EXCEL EXPORT
    // ═══════════════════════════════════════════════════════════

    @Override
    public ByteArrayInputStream exportToExcel(InvoiceType type, String userEmail) throws Exception {
        Company company = companyService.findByUserEmail(userEmail);

        try (org.apache.poi.xssf.streaming.SXSSFWorkbook workbook = new org.apache.poi.xssf.streaming.SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Fakturalar");

            // Başlıq sətri
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Faktura Nömrəsi", "Növ", "Status", "Tarix",
                    "Çatdırılma Tarixi", "Partner", "Valyuta",
                    "Ümumi Məbləğ", "Ödənilmiş Məbləğ", "Son Qalıq", "Ödəniş Metodu"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowIdx = 1;
            int page = 0;
            int size = 500;
            org.springframework.data.domain.Page<Invoice> invoicePage;

            do {
                if (type != null) {
                    invoicePage = invoiceRepository.findAllByCompanyAndInvoiceType(company, type, org.springframework.data.domain.PageRequest.of(page, size));
                } else {
                    invoicePage = invoiceRepository.findAllByCompany(company, org.springframework.data.domain.PageRequest.of(page, size));
                }

                for (Invoice invoice : invoicePage.getContent()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(invoice.getInvoiceNumber());
                    row.createCell(1).setCellValue(invoice.getInvoiceType().toValue());
                    row.createCell(2).setCellValue(invoice.getStatus().toValue());
                    row.createCell(3).setCellValue(invoice.getInvoiceDate().toString());
                    row.createCell(4).setCellValue(
                            invoice.getDeliveryDate() != null ? invoice.getDeliveryDate().toString() : "");
                    row.createCell(5).setCellValue(invoice.getPartner().getName());
                    row.createCell(6).setCellValue(invoice.getCurrency());
                    row.createCell(7).setCellValue(invoice.getTotalAmount().doubleValue());
                    row.createCell(8).setCellValue(
                            invoice.getPaidAmount() != null ? invoice.getPaidAmount().doubleValue() : 0.0);
                    row.createCell(9).setCellValue(
                            invoice.getRemainingAmount() != null ? invoice.getRemainingAmount().doubleValue() : 0.0);
                    row.createCell(10).setCellValue(
                            invoice.getPaymentMethod() != null ? invoice.getPaymentMethod().toValue() : "");
                }
                page++;
            } while (invoicePage.hasNext());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.dispose();
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  EXCEL IMPORT (faktura sətirləri)
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public InvoiceReadDto importItemsFromExcel(Long invoiceId, MultipartFile file, String userEmail) throws Exception {
        Company company = companyService.findByUserEmail(userEmail);
        Invoice invoice = findInvoiceAndCheckAccess(invoiceId, company);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessException("Yalnız DRAFT statusundakı fakturalara Excel-dən idxal oluna bilər");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Başlıq sətirini keç

                String name = getCellValueAsString(row.getCell(0));
                if (name.isBlank()) continue; // Boş sətirləri keç

                String unit = getCellValueAsString(row.getCell(1));
                BigDecimal unitPrice = getCellValueAsBigDecimal(row.getCell(2));
                Integer quantity = getCellValueAsInteger(row.getCell(3));
                String barcode = getCellValueAsString(row.getCell(4));

                if (unitPrice == null || quantity == null) {
                    throw new Exception("Sətir " + (row.getRowNum() + 1)
                            + ": Qiymət və Miqdar mütləqdir!");
                }

                InvoiceItem item = new InvoiceItem();
                item.setName(name);
                item.setUnit(unit);
                item.setUnitPrice(unitPrice);
                item.setQuantity(quantity);
                item.setBarcode(barcode);
                item.calculateLineTotal();
                invoice.addItem(item);
            }
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return mapToReadDto(savedInvoice);
    }

    @Override
    @Transactional
    public InvoiceReadDto payInvoice(Long invoiceId, InvoicePaymentDto paymentDto, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("User", "email", userEmail));

        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("Invoice", "id", invoiceId));
        if (!invoice.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Bu faktura sizin şirkətə aid deyil");
        }

        if (invoice.getStatus() == InvoiceStatus.DRAFT || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException("Draft və ya ləğv edilmiş fakturaya ödəniş edilə bilməz");
        }

        if (paymentDto.getAmount() == null && Boolean.TRUE.equals(paymentDto.getFullPayment())) {
            paymentDto.setAmount(invoice.getRemainingAmount());
        }

        BigDecimal amount = paymentDto.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Ödəniş məbləği sıfırdan böyük olmalıdır");
        }

        if (amount.compareTo(invoice.getRemainingAmount()) > 0) {
            throw new BusinessException("Ödəniş məbləği qalıq borcdan çox ola bilməz. Qalıq borc: " + invoice.getRemainingAmount());
        }

        Account account = accountRepository.findByIdForUpdate(paymentDto.getAccountId())
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("Account", "id", paymentDto.getAccountId()));
        if (!account.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Bu hesab sizin şirkətə aid deyil");
        }

        TransactionType expectedType = (invoice.getInvoiceType() == InvoiceType.SALES)
                ? TransactionType.INCOME
                : TransactionType.EXPENSE;

        // Kateqoriyanı avtomatik olaraq qaimə tipinə görə tap, yoxsa yarat
        TransactionCategory category = categoryRepository
            .findByCompanyIdAndTypeAndName(company.getId(), expectedType, "Faktura Ödənişi")
            .orElseGet(() -> {
                TransactionCategory newCat = new TransactionCategory();
                newCat.setName("Faktura Ödənişi");
                newCat.setType(expectedType);
                newCat.setCompany(company);
                return categoryRepository.save(newCat);
            });

        if (expectedType == TransactionType.EXPENSE && account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Kassada kifayət qədər vəsait yoxdur. Mövcud balans: " + account.getBalance());
        }

        // ——— Hesab balansını yenilə ———
        if (expectedType == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            account.setBalance(account.getBalance().subtract(amount));
        }
        accountRepository.save(account);

        // ——— Tranzaksiyanı yarat ———
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setAmount(amount);
        transaction.setTransactionDate(paymentDto.getTransactionDate() != null ? paymentDto.getTransactionDate() : LocalDateTime.now());
        transaction.setType(expectedType);
        transaction.setDescription(paymentDto.getDescription() != null ? paymentDto.getDescription() : (invoice.getInvoiceNumber() + " nömrəli qaimə üzrə ödəniş"));
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setCompany(company);
        transaction.setCreatedBy(user);
        transaction.setPartner(invoice.getPartner());
        transaction.setInvoice(invoice);
        transactionRepository.save(transaction);

        // ——— Partnyor balansını yenilə ———
        if (invoice.getPartner() != null) {
            Partner partner = invoice.getPartner();
            Balance balance = partner.getBalance();
            if (balance == null) {
                balance = new Balance();
                balance.setAmount(BigDecimal.ZERO);
                balance.setCurrencyType(invoice.getCurrency() != null ? invoice.getCurrency() : "manat");
                balance.setPartner(partner);
                partner.setBalance(balance);
            }
            if (invoice.getInvoiceType() == InvoiceType.SALES) {
                balance.setAmount(balance.getAmount().subtract(amount));
            } else if (invoice.getInvoiceType() == InvoiceType.PURCHASE) {
                balance.setAmount(balance.getAmount().add(amount));
            }
            balanceRepository.save(balance);
            partnerRepository.save(partner);
        }

        // ——— Qaimənin ödənilən/qalıq borc və statusunu yenilə ———
        BigDecimal newPaidAmount = (invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO).add(amount);
        invoice.setPaidAmount(newPaidAmount);

        // Recalculate remaining amount and save
        invoice.recalculateTotal();

        if (invoice.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return mapToReadDto(savedInvoice);
    }

    @Override
    @Transactional
    public InvoiceReadDto refundPayment(Long invoiceId, Long transactionId, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("User", "email", userEmail));

        Invoice invoice = findInvoiceAndCheckAccess(invoiceId, company);

        FinancialTransaction originalTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("Transaction", "id", transactionId));

        if (originalTransaction.getInvoice() == null || !originalTransaction.getInvoice().getId().equals(invoice.getId())) {
            throw new BusinessException("Bu tranzaksiya bu fakturaya aid deyil");
        }

        BigDecimal amount = originalTransaction.getAmount();

        Account account = originalTransaction.getAccount();
        TransactionType reverseType = (originalTransaction.getType() == TransactionType.INCOME) ? TransactionType.EXPENSE : TransactionType.INCOME;

        if (reverseType == TransactionType.EXPENSE && account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Refund etmək üçün kassada kifayət qədər vəsait yoxdur. Mövcud balans: " + account.getBalance());
        }

        // Hesab balansını geri qaytar
        if (reverseType == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            account.setBalance(account.getBalance().subtract(amount));
        }
        accountRepository.save(account);

        // Yeni əks-yönlü tranzaksiya yarat
        FinancialTransaction reverseTransaction = new FinancialTransaction();
        reverseTransaction.setAmount(amount);
        reverseTransaction.setTransactionDate(LocalDateTime.now());
        reverseTransaction.setType(reverseType);
        reverseTransaction.setDescription("Ləğv/Refund: " + originalTransaction.getDescription());
        reverseTransaction.setAccount(account);
        reverseTransaction.setCategory(originalTransaction.getCategory());
        reverseTransaction.setCompany(company);
        reverseTransaction.setCreatedBy(user);
        reverseTransaction.setPartner(originalTransaction.getPartner());
        reverseTransaction.setInvoice(invoice); 
        transactionRepository.save(reverseTransaction);

        // Partnyor balansını geri qaytar
        if (invoice.getPartner() != null) {
            Partner partner = invoice.getPartner();
            Balance balance = partner.getBalance();
            if (balance != null) {
                if (invoice.getInvoiceType() == InvoiceType.SALES) {
                    balance.setAmount(balance.getAmount().add(amount));
                } else if (invoice.getInvoiceType() == InvoiceType.PURCHASE) {
                    balance.setAmount(balance.getAmount().subtract(amount));
                }
                balanceRepository.save(balance);
                partnerRepository.save(partner);
            }
        }

        // Qaimənin paidAmount və remainingAmount yenilə
        invoice.setPaidAmount(invoice.getPaidAmount().subtract(amount));
        invoice.recalculateTotal();

        if (invoice.getPaidAmount().compareTo(BigDecimal.ZERO) == 0) {
            if (Boolean.TRUE.equals(invoice.getStockApplied())) {
                invoice.setStatus(InvoiceStatus.SENT);
            } else {
                invoice.setStatus(InvoiceStatus.DRAFT);
            }
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return mapToReadDto(savedInvoice);
    }

    // ═══════════════════════════════════════════════════════════
    //  YARDIMÇI METODLAR
    // ═══════════════════════════════════════════════════════════

    /**
     * Fakturanı tapır və şirkətə aidliyini yoxlayır.
     */
    private Invoice findInvoiceAndCheckAccess(Long invoiceId, Company company) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("Invoice", "id", invoiceId));

        if (!invoice.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Bu faktura sizin şirkətə aid deyil");
        }

        return invoice;
    }

    private String generateInvoiceNumber(Company company) {
        int currentYear = LocalDate.now().getYear();

        // 1. Şirkət və il üçün ardıcıllığın mövcud olduğuna əmin ol (ayrıca tranzaksiyada yaradılır)
        try {
            sequenceGeneratorService.ensureSequenceExists(company, currentYear);
        } catch (Exception e) {
            // Başqa bir thread eyni anda yaratdısa və ya bazada unikallıq xətası verdisə, 
            // inner transaction (REQUIRES_NEW) rollback olacaq, lakin outer transaction davam edəcək.
        }

        // 2. Ardıcıllığı PESSIMISTIC_WRITE ilə kilidləyərək götür
        InvoiceSequence sequence = invoiceSequenceRepository
                .findByCompanyAndYear(company, currentYear)
                .orElseThrow(() -> new IllegalStateException("Faktura ardıcıllığı yaradıla bilmədi"));

        String invoiceNumber;
        do {
            invoiceNumber = sequence.generateNextNumber();
        } while (invoiceRepository.findByInvoiceNumberAndCompany(invoiceNumber, company).isPresent());

        invoiceSequenceRepository.save(sequence);

        return invoiceNumber;
    }

    private void validateStatusTransition(InvoiceStatus current, InvoiceStatus target) {
        if (target == InvoiceStatus.PAID || target == InvoiceStatus.PARTIALLY_PAID) {
            throw new BusinessException("PAID/PARTIALLY_PAID statusuna keçid yalnız ödəniş endpoint-i vasitəsilə mümkündür. POST /api/v1/invoices/{id}/pay istifadə edin.");
        }

        // CANCELLED statusundan heç yerə keçid yoxdur
        if (current == InvoiceStatus.CANCELLED) {
            throw new RuntimeException("Ləğv edilmiş fakturanın statusu dəyişdirilə bilməz");
        }

        // PAID statusundan yalnız CANCELLED-ə keçid mümkündür
        if (current == InvoiceStatus.PAID && target != InvoiceStatus.CANCELLED) {
            throw new BusinessException("Ödənilmiş faktura yalnız ləğv edilə bilər");
        }

        if (current == InvoiceStatus.PARTIALLY_PAID && target != InvoiceStatus.PAID && target != InvoiceStatus.OVERDUE && target != InvoiceStatus.CANCELLED) {
            throw new BusinessException("Qismən ödənilmiş faktura yalnız tam ödənilə, vaxtı keçmiş kimi qeyd edilə və ya ləğv edilə bilər");
        }
    }

    /**
     * InvoiceItemDto-dan InvoiceItem entity-yə çevirir.
     */
    private InvoiceItem mapToInvoiceItem(InvoiceItemDto dto, Company company) {
        InvoiceItem item = new InvoiceItem();
        item.setUnitPrice(dto.getUnitPrice());
        item.setQuantity(dto.getQuantity());

        // Əgər product ID göstərilibsə — əlaqələndir
        if (dto.getProductId() != null) {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("Product", "id", dto.getProductId()));
            if (product.getCompany() == null || !product.getCompany().getId().equals(company.getId())) {
                throw new AccessDeniedException("This product does not belong to your company");
            }
            item.setProduct(product);
        }

        if (item.getProduct() == null && dto.getBarcode() != null && !dto.getBarcode().isBlank()) {
            Product product = productRepository.findByProductCodeAndCompanyId(dto.getBarcode(), company.getId()).orElse(null);
            if (product != null) {
                item.setProduct(product);
            }
        }

        // Avtomatik olaraq məhsulun vahidini, adını və barkodunu təyin edirik
        if (item.getProduct() != null) {
            Product p = item.getProduct();
            item.setName(p.getName());
            item.setBarcode(p.getProductCode());
            if (p.getUnit() != null) {
                item.setUnit(p.getUnit().getName());
            } else {
                item.setUnit(dto.getUnit());
            }
        } else {
            // Əgər məhsul tapılmayıbsa, əllə daxil edilən ad, vahid və barkoddan istifadə edirik
            if (dto.getName() == null || dto.getName().isBlank()) {
                throw new IllegalArgumentException("Məhsul adı və ya Məhsul seçimi mütləqdir!");
            }
            item.setName(dto.getName());
            item.setUnit(dto.getUnit());
            item.setBarcode(dto.getBarcode());
        }

        if (dto.getVatStatus() != null) {
            VatStatus status = VatStatus.fromString(dto.getVatStatus());
            item.setVatStatus(status);
            if (status == VatStatus.WITH_VAT) {
                item.setVatRate(dto.getVatRate() != null ? dto.getVatRate() : BigDecimal.valueOf(18));
            } else {
                item.setVatRate(null);
            }
        } else if (item.getProduct() != null) {
            Product p = item.getProduct();
            item.setVatStatus(p.getVatStatus());
            item.setVatRate(p.getVatRate());
        } else {
            item.setVatStatus(VatStatus.WITHOUT_VAT);
            item.setVatRate(null);
        }

        // Sətir üzrə endirim
        if (dto.getDiscount() != null) {
            item.setDiscount(dto.getDiscount());
        }
        if (dto.getDiscountType() != null && !dto.getDiscountType().isBlank()) {
            item.setDiscountType(az.techvibeds.specialservice.enums.AdditionalExpenseType.fromValue(dto.getDiscountType()));
        }

        // Maya dəyəri / alış qiyməti — alış qaiməsindən gəlir, addStock() oxuyur
        if (dto.getPrimeCost() != null) {
            item.setPrimeCost(dto.getPrimeCost());
        }

        item.calculateLineTotal();


        if (dto.getWarehouseId() != null) {
            Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                    .orElseThrow(() -> new BusinessException("Warehouse not found", HttpStatus.NOT_FOUND));
            if (!warehouse.getCompany().getId().equals(company.getId())) {
                throw new AccessDeniedException("This warehouse does not belong to your company");
            }
            item.setWarehouse(warehouse);
        }

        return item;
    }

    /**
     * Invoice entity-dən InvoiceReadDto-ya çevirir.
     */
    private InvoiceReadDto mapToReadDto(Invoice invoice) {
        InvoiceReadDto dto = new InvoiceReadDto();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setInvoiceType(invoice.getInvoiceType());
        dto.setStatus(invoice.getStatus());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setDeliveryDate(invoice.getDeliveryDate());
        dto.setPaymentMethod(invoice.getPaymentMethod());
        dto.setCurrency(invoice.getCurrency());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setPaidAmount(invoice.getPaidAmount());
        dto.setRemainingAmount(invoice.getRemainingAmount());
        dto.setAdditionalExpense(invoice.getAdditionalExpense());
        dto.setAdditionalExpenseType(invoice.getAdditionalExpenseType());
        dto.setDiscount(invoice.getDiscount());
        dto.setDiscountType(invoice.getDiscountType());
        dto.setTotalVatAmount(invoice.getTotalVatAmount());
        dto.setStockApplied(invoice.getStockApplied());
        dto.setNotes(invoice.getNotes());
        dto.setCreatedAt(invoice.getCreatedAt());

        BigDecimal gross = invoice.getItems().stream()
                .map(InvoiceItem::getLineTotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setGrossAmount(gross);

        // Partner məlumatları
        if (invoice.getPartner() != null) {
            dto.setPartnerId(invoice.getPartner().getId());
            dto.setPartnerName(invoice.getPartner().getName());
            dto.setPartnerType(invoice.getPartner().getPartnerType().toValue());
        }

        // Faktura sətirləri
        if (invoice.getItems() != null) {
            List<InvoiceItemReadDto> itemDtos = invoice.getItems().stream()
                    .map(this::mapToItemReadDto)
                    .collect(Collectors.toList());
            dto.setItems(itemDtos);
        }

        // Ödəniş tarixçəsi
        if (invoice.getId() != null) {
            List<FinancialTransaction> transactions = transactionRepository.findByInvoiceIdOrderByTransactionDateDesc(invoice.getId());
            if (transactions != null && !transactions.isEmpty()) {
                List<InvoicePaymentReadDto> paymentDtos = transactions.stream().map(t -> {
                    InvoicePaymentReadDto pDto = new InvoicePaymentReadDto();
                    pDto.setTransactionId(t.getId());
                    pDto.setAmount(t.getAmount());
                    pDto.setTransactionDate(t.getTransactionDate());
                    if (t.getAccount() != null) {
                        pDto.setAccountId(t.getAccount().getId());
                        pDto.setAccountName(t.getAccount().getName());
                    }
                    pDto.setDescription(t.getDescription());
                    if (t.getCreatedBy() != null) {
                        pDto.setCreatedByName(t.getCreatedBy().getName() + " " + t.getCreatedBy().getSurname());
                    }
                    return pDto;
                }).collect(Collectors.toList());
                dto.setPayments(paymentDtos);
            }
        }

        return dto;
    }

    /**
     * InvoiceItem entity-dən InvoiceItemReadDto-ya çevirir.
     */
    private InvoiceItemReadDto mapToItemReadDto(InvoiceItem item) {
        InvoiceItemReadDto dto = new InvoiceItemReadDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setUnit(item.getUnit());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setQuantity(item.getQuantity());
        dto.setBarcode(item.getBarcode());
        dto.setLineTotal(item.getLineTotal());
        dto.setVatAmount(item.getVatAmount());
        dto.setVatStatus(item.getVatStatus() != null ? item.getVatStatus().name() : "WITHOUT_VAT");
        dto.setVatRate(item.getVatRate());
        dto.setPrimeCost(item.getPrimeCost());
        dto.setDiscount(item.getDiscount());
        dto.setDiscountType(item.getDiscountType() != null ? item.getDiscountType().name() : null);
        dto.setDiscountAmount(item.getDiscountAmount());

        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getId());
        }
        if (item.getWarehouse() != null) {
            dto.setWarehouseId(item.getWarehouse().getId());
            dto.setWarehouseName(item.getWarehouse().getName());
        }

        return dto;
    }

    // ——— Excel yardımçı metodlar ———

    private boolean shouldApplyStock(InvoiceStatus status) {
        return status == InvoiceStatus.SENT
                || status == InvoiceStatus.PAID
                || status == InvoiceStatus.OVERDUE;
    }

    private void applyInvoiceStock(Invoice invoice, Company company) {
        boolean applied = false;
        for (InvoiceItem item : invoice.getItems()) {
            if (item.getProduct() == null) {
                continue;
            }
            if (item.getWarehouse() == null) {
                throw new BusinessException("Warehouse is required for stock item: " + item.getName());
            }

            validateStockItemOwnership(item, company);
            if (invoice.getInvoiceType() == InvoiceType.SALES) {
                removeStock(item, company);
            } else if (invoice.getInvoiceType() == InvoiceType.PURCHASE) {
                addStock(item, company);
            }
            applied = true;
        }
        invoice.setStockApplied(true);

        // Partnyorun balansını yenilə
        if (invoice.getPartner() != null && invoice.getTotalAmount() != null) {
            Partner partner = invoice.getPartner();
            Balance balance = partner.getBalance();
            if (balance == null) {
                balance = new Balance();
                balance.setAmount(BigDecimal.ZERO);
                balance.setCurrencyType(invoice.getCurrency() != null ? invoice.getCurrency() : "manat");
                balance.setPartner(partner);
                partner.setBalance(balance);
            }
            if (invoice.getInvoiceType() == InvoiceType.SALES) {
                // Müştərinin borcu artır (müsbətə doğru gedir)
                balance.setAmount(balance.getAmount().add(invoice.getTotalAmount()));
            } else if (invoice.getInvoiceType() == InvoiceType.PURCHASE) {
                // Bizim təchizatçıya borcumuz artır (mənfiyə doğru gedir)
                balance.setAmount(balance.getAmount().subtract(invoice.getTotalAmount()));
            }
            balanceRepository.save(balance);
            partnerRepository.save(partner);
        }
    }

    private void reverseInvoiceStock(Invoice invoice, Company company) {
        for (InvoiceItem item : invoice.getItems()) {
            if (item.getProduct() == null) {
                continue;
            }
            if (item.getWarehouse() == null) {
                throw new BusinessException("Warehouse is required for stock item: " + item.getName());
            }


            validateStockItemOwnership(item, company);
            if (invoice.getInvoiceType() == InvoiceType.SALES) {
                addStock(item, company);
            } else if (invoice.getInvoiceType() == InvoiceType.PURCHASE) {
                removeStock(item, company);
            }
        }
        invoice.setStockApplied(false);

        // Partnyorun balansını geri qaytar
        if (invoice.getPartner() != null && invoice.getTotalAmount() != null) {
            Partner partner = invoice.getPartner();
            Balance balance = partner.getBalance();
            if (balance != null) {
                if (invoice.getInvoiceType() == InvoiceType.SALES) {
                    // Müştərinin borcunu azaldırıq
                    balance.setAmount(balance.getAmount().subtract(invoice.getTotalAmount()));
                } else if (invoice.getInvoiceType() == InvoiceType.PURCHASE) {
                    // Bizim təchizatçıya borcumuzu azaldırıq
                    balance.setAmount(balance.getAmount().add(invoice.getTotalAmount()));
                }
                partnerRepository.save(partner);
            }
        }
    }

    private void validateStockItemOwnership(InvoiceItem item, Company company) {
        Product product = item.getProduct();
        Warehouse warehouse = item.getWarehouse();

        if (product.getCompany() == null || !product.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("This product does not belong to your company");
        }
        if (warehouse.getCompany() == null || !warehouse.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("This warehouse does not belong to your company");
        }
    }

    private void addStock(InvoiceItem item, Company company) {
        WarehouseProduct warehouseProduct = warehouseProductRepository
                .findByProductAndWarehouse(item.getProduct(), item.getWarehouse());
        if (warehouseProduct == null) {
            warehouseProduct = new WarehouseProduct();
            warehouseProduct.setProduct(item.getProduct());
            warehouseProduct.setWarehouse(item.getWarehouse());
            warehouseProduct.setQuantity(0);
        }

        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
        int currentQuantity = warehouseProduct.getQuantity() != null ? warehouseProduct.getQuantity() : 0;
        warehouseProduct.setQuantity(currentQuantity + quantity);

        Product product = item.getProduct();
        long currentStock = product.getStock() != null ? product.getStock() : 0L;
        product.setStock(currentStock + quantity);
        product.setProductStatus(ProductStatus.IN_STOCK);
        
        if (item.getInvoice() != null && az.techvibeds.specialservice.enums.InvoiceType.PURCHASE.equals(item.getInvoice().getInvoiceType())) {
            java.math.BigDecimal primeCost = item.getPrimeCost() != null ? item.getPrimeCost() : item.getUnitPrice();
            product.setPurchasePrice(primeCost);
        }

        warehouseProductRepository.save(warehouseProduct);
        productRepository.save(product);

        Invoice invoice = item.getInvoice();
        String refNum = invoice != null ? invoice.getInvoiceNumber() : null;
        String partnerName = (invoice != null && invoice.getPartner() != null) ? invoice.getPartner().getName() : "N/A";
        BigDecimal purPrice = item.getUnitPrice();
        BigDecimal salPrice = item.getProduct() != null ? item.getProduct().getPrice() : null;

        saveWarehouseActivity(ActivityTypeWarehouse.INCOMING, warehouseProduct, quantity,
                null, item.getWarehouse(), purPrice, salPrice, refNum, partnerName, item.getWarehouse().getName(), company);
    }

    private void removeStock(InvoiceItem item, Company company) {
        WarehouseProduct warehouseProduct = warehouseProductRepository
                .findByProductAndWarehouse(item.getProduct(), item.getWarehouse());
        if (warehouseProduct == null) {
            throw new BusinessException("Product is not present in selected warehouse");
        }

        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
        int available = warehouseProduct.getQuantity() != null ? warehouseProduct.getQuantity() : 0;
        if (available < quantity) {
            throw new BusinessException("Not enough stock for " + item.getName() + ". Available: " + available);
        }

        warehouseProduct.setQuantity(available - quantity);

        Product product = item.getProduct();
        long currentStock = product.getStock() != null ? product.getStock() : 0L;
        long newStock = Math.max(0L, currentStock - quantity);
        product.setStock(newStock);
        
        Long limit = product.getMinStockLimit() != null ? product.getMinStockLimit() : 0L;
        if (newStock <= 0) {
            product.setProductStatus(ProductStatus.LOW_STOCK);
        }
        
        if (newStock < limit && currentStock >= limit) {
            notificationService.createEmailNotification(
                String.format("Məhsul stoku limitdən aşağı düşdü: %s (Cari stok: %d, Limit: %d)", product.getName(), newStock, limit),
                company
            );
        }

        warehouseProductRepository.save(warehouseProduct);
        productRepository.save(product);

        Invoice invoice = item.getInvoice();
        String refNum = invoice != null ? invoice.getInvoiceNumber() : null;
        String partnerName = (invoice != null && invoice.getPartner() != null) ? invoice.getPartner().getName() : "N/A";
        BigDecimal purPrice = item.getPrimeCost();
        BigDecimal salPrice = item.getUnitPrice();

        saveWarehouseActivity(ActivityTypeWarehouse.OUTGOING, warehouseProduct, quantity,
                item.getWarehouse(), null, purPrice, salPrice, refNum, item.getWarehouse().getName(), partnerName, company);
    }

    private void saveWarehouseActivity(
            ActivityTypeWarehouse type,
            WarehouseProduct warehouseProduct,
            int quantity,
            Warehouse sourceWarehouse,
            Warehouse destinationWarehouse,
            BigDecimal purchasePrice,
            BigDecimal salePrice,
            String referenceNumber,
            String sourceName,
            String destinationName,
            Company company) {
        WarehouseActivity activity = new WarehouseActivity();
        activity.setType(type);
        activity.setProduct(warehouseProduct);
        activity.setQuantity(quantity);
        activity.setSourceWarehouse(sourceWarehouse);
        activity.setDestinationWarehouse(destinationWarehouse);
        activity.setPurchasePrice(purchasePrice);
        activity.setSalePrice(salePrice);
        activity.setReferenceNumber(referenceNumber);
        activity.setSourceName(sourceName);
        activity.setDestinationName(destinationName);
        activity.setDate(LocalDateTime.now());
        activity.setCompany(company);
        warehouseActivityRepository.save(activity);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        return null;
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        return null;
    }

    @Override
    public Page<InvoiceReadDto> getPayableInvoices(InvoiceType type, Pageable pageable, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        if (company == null) {
            return Page.empty();
        }
        return invoiceRepository.findPayableInvoices(company, type, pageable)
                .map(this::mapToReadDto);
    }

    @Override
    public Page<InvoiceReadDto> getSalesInvoices(InvoiceStatus status, String keyword, Pageable pageable, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        if (company == null) {
            return Page.empty();
        }

        Page<Invoice> invoices;
        if (status != null) {
            invoices = invoiceRepository.findAllByCompanyAndInvoiceTypeAndStatus(company, InvoiceType.SALES, status, pageable);
        } else {
            invoices = invoiceRepository.findAllByCompanyAndInvoiceType(company, InvoiceType.SALES, pageable);
        }
        return invoices.map(this::mapToReadDto);
    }

    @Override
    public Page<InvoiceReadDto> getPurchaseInvoices(InvoiceStatus status, String keyword, Pageable pageable, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        if (company == null) {
            return Page.empty();
        }

        Page<Invoice> invoices;
        if (status != null) {
            invoices = invoiceRepository.findAllByCompanyAndInvoiceTypeAndStatus(company, InvoiceType.PURCHASE, status, pageable);
        } else {
            invoices = invoiceRepository.findAllByCompanyAndInvoiceType(company, InvoiceType.PURCHASE, pageable);
        }
        return invoices.map(this::mapToReadDto);
    }

    // ═══════════════════════════════════════════════════════════
    //  QAİMƏ LƏĞVİ (CANCEL WITH REFUND)
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public InvoiceReadDto cancelInvoice(Long invoiceId, InvoiceCancelDto dto, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("User", "email", userEmail));

        Invoice invoice = findInvoiceAndCheckAccess(invoiceId, company);

        // Artıq ləğv edilmiş və ya qaralama statusundakı qaimə ləğv edilə bilməz
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException("Bu qaimə artıq ləğv edilib");
        }
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new BusinessException("Qaralama statusundakı qaimə üçün ləğv əvəzinə silin");
        }

        // Kassanı yoxla
        Account account = accountRepository.findByIdForUpdate(dto.getAccountId())
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("Account", "id", dto.getAccountId()));
        if (!account.getCompany().getId().equals(company.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bu hesab sizin şirkətə aid deyil");
        }

        BigDecimal paidAmount = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;

        // Əgər ödəniş edilmişdirsə, geri qaytarılma tranzaksiyası yarat
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            // SALES qaiməsi üçün: əvvəlcə INCOME gəlmişdi, indi EXPENSE olaraq geri qaytarırıq
            // PURCHASE qaiməsi üçün: əvvəlcə EXPENSE getmişdi, indi INCOME olaraq geri alırıq
            TransactionType refundType;
            if (invoice.getInvoiceType() == InvoiceType.SALES) {
                refundType = TransactionType.EXPENSE;
                // Kassadan pul çıxır — balans yoxlaması
                if (account.getBalance().compareTo(paidAmount) < 0) {
                    throw new BusinessException("Kassada kifayət qədər vəsait yoxdur. Mövcud balans: " + account.getBalance());
                }
                account.setBalance(account.getBalance().subtract(paidAmount));
            } else {
                refundType = TransactionType.INCOME;
                account.setBalance(account.getBalance().add(paidAmount));
            }
            accountRepository.save(account);

            // Kateqoriyanı tap və ya yarat
            TransactionCategory category = categoryRepository
                    .findByCompanyIdAndTypeAndName(company.getId(), refundType, "Qaimə Ləğvi")
                    .orElseGet(() -> {
                        TransactionCategory newCat = new TransactionCategory();
                        newCat.setName("Qaimə Ləğvi");
                        newCat.setType(refundType);
                        newCat.setCompany(company);
                        return categoryRepository.save(newCat);
                    });

            // Refund tranzaksiyasını yarat
            FinancialTransaction refundTransaction = new FinancialTransaction();
            refundTransaction.setAmount(paidAmount);
            refundTransaction.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDateTime.now());
            refundTransaction.setType(refundType);
            refundTransaction.setDescription(dto.getDescription() != null
                    ? dto.getDescription()
                    : invoice.getInvoiceNumber() + " nömrəli qaimənin ləğvi — " + paidAmount + " " + (invoice.getCurrency() != null ? invoice.getCurrency() : "AZN") + " geri qaytarıldı (" + dto.getPaymentMethod().toValue() + ")");
            refundTransaction.setAccount(account);
            refundTransaction.setCategory(category);
            refundTransaction.setCompany(company);
            refundTransaction.setCreatedBy(user);
            refundTransaction.setPartner(invoice.getPartner());
            refundTransaction.setInvoice(invoice);
            transactionRepository.save(refundTransaction);

            // Partnyor balansından ödəniş hissəsini geri al
            if (invoice.getPartner() != null) {
                Partner partner = invoice.getPartner();
                Balance balance = partner.getBalance();
                if (balance != null) {
                    if (invoice.getInvoiceType() == InvoiceType.SALES) {
                        // Müştəridən geri ödəniş — onun borcu artır (və ya alacağı artır)
                        balance.setAmount(balance.getAmount().add(paidAmount));
                    } else if (invoice.getInvoiceType() == InvoiceType.PURCHASE) {
                        // Təchizatçıdan geri alma — bizim borcumuz artır (müsbətə doğru)
                        balance.setAmount(balance.getAmount().subtract(paidAmount));
                    }
                    balanceRepository.save(balance);
                    partnerRepository.save(partner);
                }
            }
        }

        // Anbar hərəkətlərini geri al
        if (Boolean.TRUE.equals(invoice.getStockApplied())) {
            reverseInvoiceStock(invoice, company);
        }

        // Qaimənin statusunu yenilə
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.recalculateTotal();

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return mapToReadDto(savedInvoice);
    }

    // ═══════════════════════════════════════════════════════════
    //  QİSMƏN QAYTARILMA (PARTIAL RETURN)
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public InvoiceReadDto partialReturn(Long invoiceId, InvoicePartialReturnDto dto, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("User", "email", userEmail));

        Invoice invoice = findInvoiceAndCheckAccess(invoiceId, company);

        // Yalnız SENT, PAID, PARTIALLY_PAID statuslarında qaytarma ola bilər
        if (invoice.getStatus() != InvoiceStatus.SENT
                && invoice.getStatus() != InvoiceStatus.PAID
                && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID
                && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new BusinessException("Bu statusda qaimədən qaytarma edilə bilməz. Status: " + invoice.getStatus());
        }

        // Qaytarılan sətiri tap
        InvoiceItem item = invoice.getItems().stream()
                .filter(i -> i.getId().equals(dto.getInvoiceItemId()))
                .findFirst()
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("InvoiceItem", "id", dto.getInvoiceItemId()));

        int currentQty = item.getQuantity() != null ? item.getQuantity() : 0;
        int returnQty = dto.getQuantity();

        if (returnQty > currentQty) {
            throw new BusinessException("Qaytarılan miqdar (" + returnQty + ") qaimədəki miqdardan (" + currentQty + ") çox ola bilməz");
        }

        // Qaytarılan məbləğ
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal returnAmount = unitPrice.multiply(BigDecimal.valueOf(returnQty));

        // Kassanı yoxla
        Account account = accountRepository.findByIdForUpdate(dto.getAccountId())
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("Account", "id", dto.getAccountId()));
        if (!account.getCompany().getId().equals(company.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bu hesab sizin şirkətə aid deyil");
        }

        BigDecimal paidAmount = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;

        // Əgər bu sətir üçün ödəniş edilmişdirsə, geri qaytarılma tranzaksiyası yarat
        // Geri qaytarılacaq pul = min(returnAmount, paidAmount - yeni totalAmount)
        // Sadə yanaşma: əgər ödənilmişdirsə, qaytarılan məhsulun məbləğini geri ödə
        BigDecimal cashRefund = BigDecimal.ZERO;
        BigDecimal newTotalAmount = invoice.getTotalAmount().subtract(returnAmount);
        if (newTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            newTotalAmount = BigDecimal.ZERO;
        }

        if (paidAmount.compareTo(newTotalAmount) > 0) {
            // Artıq ödəniş var, fərqi geri qaytar
            cashRefund = paidAmount.subtract(newTotalAmount);
        }

        if (cashRefund.compareTo(BigDecimal.ZERO) > 0) {
            TransactionType refundType;
            if (invoice.getInvoiceType() == InvoiceType.SALES) {
                refundType = TransactionType.EXPENSE;
                if (account.getBalance().compareTo(cashRefund) < 0) {
                    throw new BusinessException("Kassada kifayət qədər vəsait yoxdur. Mövcud balans: " + account.getBalance());
                }
                account.setBalance(account.getBalance().subtract(cashRefund));
            } else {
                refundType = TransactionType.INCOME;
                account.setBalance(account.getBalance().add(cashRefund));
            }
            accountRepository.save(account);

            // Kateqoriyanı tap və ya yarat
            TransactionCategory category = categoryRepository
                    .findByCompanyIdAndTypeAndName(company.getId(), refundType, "Qismən Qaytarma")
                    .orElseGet(() -> {
                        TransactionCategory newCat = new TransactionCategory();
                        newCat.setName("Qismən Qaytarma");
                        newCat.setType(refundType);
                        newCat.setCompany(company);
                        return categoryRepository.save(newCat);
                    });

            FinancialTransaction refundTransaction = new FinancialTransaction();
            refundTransaction.setAmount(cashRefund);
            refundTransaction.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDateTime.now());
            refundTransaction.setType(refundType);
            refundTransaction.setDescription(dto.getDescription() != null
                    ? dto.getDescription()
                    : invoice.getInvoiceNumber() + " — " + item.getName() + " x" + returnQty + " qaytarıldı, " + cashRefund + " " + (invoice.getCurrency() != null ? invoice.getCurrency() : "AZN") + " geri ödənildi (" + dto.getPaymentMethod().toValue() + ")");
            refundTransaction.setAccount(account);
            refundTransaction.setCategory(category);
            refundTransaction.setCompany(company);
            refundTransaction.setCreatedBy(user);
            refundTransaction.setPartner(invoice.getPartner());
            refundTransaction.setInvoice(invoice);
            transactionRepository.save(refundTransaction);

            // paidAmount yenilə
            invoice.setPaidAmount(paidAmount.subtract(cashRefund));
        }

        // Partnyor balansını yenilə — qaytarılan məhsulun məbləği qədər
        if (invoice.getPartner() != null) {
            Partner partner = invoice.getPartner();
            Balance balance = partner.getBalance();
            if (balance != null) {
                if (invoice.getInvoiceType() == InvoiceType.SALES) {
                    // Müştərinin borcu returnAmount qədər azalır
                    balance.setAmount(balance.getAmount().subtract(returnAmount));
                } else if (invoice.getInvoiceType() == InvoiceType.PURCHASE) {
                    // Bizim borcumuz returnAmount qədər azalır (müsbətə doğru)
                    balance.setAmount(balance.getAmount().add(returnAmount));
                }
                balanceRepository.save(balance);
                partnerRepository.save(partner);
            }
        }

        // Anbar hərəkətini geri al (qaytarılan miqdar qədər)
        if (Boolean.TRUE.equals(invoice.getStockApplied()) && item.getProduct() != null && item.getWarehouse() != null) {
            if (invoice.getInvoiceType() == InvoiceType.SALES) {
                // Satışdan qaytarma — məhsul anbara geri daxil olur
                WarehouseProduct warehouseProduct = warehouseProductRepository
                        .findByProductAndWarehouse(item.getProduct(), item.getWarehouse());
                if (warehouseProduct == null) {
                    warehouseProduct = new WarehouseProduct();
                    warehouseProduct.setProduct(item.getProduct());
                    warehouseProduct.setWarehouse(item.getWarehouse());
                    warehouseProduct.setQuantity(0);
                }
                int currentWpQty = warehouseProduct.getQuantity() != null ? warehouseProduct.getQuantity() : 0;
                warehouseProduct.setQuantity(currentWpQty + returnQty);

                Product product = item.getProduct();
                long currentStock = product.getStock() != null ? product.getStock() : 0L;
                product.setStock(currentStock + returnQty);
                product.setProductStatus(ProductStatus.IN_STOCK);

                warehouseProductRepository.save(warehouseProduct);
                productRepository.save(product);

                String partnerName = (invoice.getPartner() != null) ? invoice.getPartner().getName() : "N/A";
                saveWarehouseActivity(ActivityTypeWarehouse.INCOMING, warehouseProduct, returnQty,
                        null, item.getWarehouse(), item.getPrimeCost(), item.getUnitPrice(),
                        invoice.getInvoiceNumber() + "-RET", partnerName, item.getWarehouse().getName(), company);
            } else if (invoice.getInvoiceType() == InvoiceType.PURCHASE) {
                // Alışdan qaytarma — məhsul anbardan çıxır
                WarehouseProduct warehouseProduct = warehouseProductRepository
                        .findByProductAndWarehouse(item.getProduct(), item.getWarehouse());
                if (warehouseProduct == null) {
                    throw new BusinessException("Anbarda bu məhsul tapılmadı");
                }
                int available = warehouseProduct.getQuantity() != null ? warehouseProduct.getQuantity() : 0;
                if (available < returnQty) {
                    throw new BusinessException("Anbarda kifayət qədər məhsul yoxdur. Mövcud: " + available);
                }
                warehouseProduct.setQuantity(available - returnQty);

                Product product = item.getProduct();
                long currentStock = product.getStock() != null ? product.getStock() : 0L;
                product.setStock(Math.max(0L, currentStock - returnQty));
                if (product.getStock() <= 0) {
                    product.setProductStatus(ProductStatus.LOW_STOCK);
                }

                warehouseProductRepository.save(warehouseProduct);
                productRepository.save(product);

                String partnerName = (invoice.getPartner() != null) ? invoice.getPartner().getName() : "N/A";
                BigDecimal salPrice = item.getProduct() != null ? item.getProduct().getPrice() : null;
                saveWarehouseActivity(ActivityTypeWarehouse.OUTGOING, warehouseProduct, returnQty,
                        item.getWarehouse(), null, item.getUnitPrice(), salPrice,
                        invoice.getInvoiceNumber() + "-RET", item.getWarehouse().getName(), partnerName, company);
            }
        }

        // Sətirdəki miqdarı yenilə
        int newQty = currentQty - returnQty;
        if (newQty <= 0) {
            // Miqdar sıfıra düşdüsə sətiri sil
            invoice.removeItem(item);
        } else {
            item.setQuantity(newQty);
            item.calculateLineTotal();
        }

        // Qaimə məbləğini yenidən hesabla
        invoice.recalculateTotal();

        // Statusu yenilə
        BigDecimal currentPaid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal currentRemaining = invoice.getRemainingAmount() != null ? invoice.getRemainingAmount() : BigDecimal.ZERO;

        if (invoice.getItems().isEmpty()) {
            // Bütün məhsullar qaytarılıb
            invoice.setStatus(InvoiceStatus.CANCELLED);
        } else if (currentRemaining.compareTo(BigDecimal.ZERO) <= 0 && currentPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else if (currentPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return mapToReadDto(savedInvoice);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public byte[] generateReceiptPdf(Long invoiceId, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Qaimə tapılmadı!", HttpStatus.NOT_FOUND));

        if (!invoice.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Bu qaimə sizin şirkətə aid deyil");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException("İstifadəçi tapılmadı!", HttpStatus.NOT_FOUND));

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // 80mm eni 226 point-dir. Hündürlük məhsulların sayına görə dinamik uzana bilər deyə 600 təyin edirik
            com.lowagie.text.Rectangle pageSize = new com.lowagie.text.Rectangle(226, 600);
            // Çekin sağ kənarının kəsilməməsi üçün sağ kənar boşluğunu (right margin) 28 point edirik (sola sürüşdürürük)
            com.lowagie.text.Document document = new com.lowagie.text.Document(pageSize, 12, 28, 8, 8);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, outputStream);

            document.open();

            // Fontlar — Azərbaycan hərflərini (ə, ş, ç, ö, ü, ğ, ı) dəstəkləyən Unicode font
            byte[] regularFontBytes = org.springframework.util.StreamUtils.copyToByteArray(
                    getClass().getResourceAsStream("/fonts/arial.ttf"));
            com.lowagie.text.pdf.BaseFont baseFont = com.lowagie.text.pdf.BaseFont.createFont(
                    "arial.ttf", com.lowagie.text.pdf.BaseFont.IDENTITY_H, com.lowagie.text.pdf.BaseFont.EMBEDDED,
                    true, regularFontBytes, null);

            byte[] boldFontBytes = org.springframework.util.StreamUtils.copyToByteArray(
                    getClass().getResourceAsStream("/fonts/arialbd.ttf"));
            com.lowagie.text.pdf.BaseFont baseFontBold = com.lowagie.text.pdf.BaseFont.createFont(
                    "arialbd.ttf", com.lowagie.text.pdf.BaseFont.IDENTITY_H, com.lowagie.text.pdf.BaseFont.EMBEDDED,
                    true, boldFontBytes, null);

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(baseFontBold, 12);
            com.lowagie.text.Font sectionFont = new com.lowagie.text.Font(baseFontBold, 9);
            com.lowagie.text.Font textFont = new com.lowagie.text.Font(baseFont, 8);
            com.lowagie.text.Font boldTextFont = new com.lowagie.text.Font(baseFontBold, 8);

            // Şirkət adı (Dinamik olaraq gəlir)
            com.lowagie.text.Paragraph compName = new com.lowagie.text.Paragraph(invoice.getCompany().getName().toUpperCase(), titleFont);
            compName.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(compName);
            document.add(new com.lowagie.text.Paragraph(" "));

            // Satış nömrəsi, Kassir və Zaman məlumatları
            java.time.LocalDateTime createdAt = invoice.getCreatedAt() != null ? invoice.getCreatedAt() : java.time.LocalDateTime.now();
            String dateStr = createdAt.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String timeStr = createdAt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

            // 2 Sütunlu Üst Məlumat Cədvəli
            com.lowagie.text.pdf.PdfPTable metaTable = new com.lowagie.text.pdf.PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{60, 40});

            addCellToReceipt(metaTable, "Satış № " + invoice.getId(), textFont, com.lowagie.text.Element.ALIGN_LEFT);
            addCellToReceipt(metaTable, "Tarix: " + dateStr, textFont, com.lowagie.text.Element.ALIGN_RIGHT);
            addCellToReceipt(metaTable, "Kassir: " + user.getName() + " " + (user.getSurname() != null ? user.getSurname() : ""), textFont, com.lowagie.text.Element.ALIGN_LEFT);
            addCellToReceipt(metaTable, "Saat: " + timeStr, textFont, com.lowagie.text.Element.ALIGN_RIGHT);

            document.add(metaTable);
            document.add(new com.lowagie.text.Paragraph("----------------------------------", textFont));

            // Sətir başlıqları (Qiymət ilə Toplam arasındakı boşluğu artırmaq üçün sütun ölçülərini tənzimləyirik)
            com.lowagie.text.pdf.PdfPTable headerTable = new com.lowagie.text.pdf.PdfPTable(4);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{40, 16, 24, 20});

            addCellToReceipt(headerTable, "Malın adı", boldTextFont, com.lowagie.text.Element.ALIGN_LEFT);
            addCellToReceipt(headerTable, "Miqdar", boldTextFont, com.lowagie.text.Element.ALIGN_CENTER);
            addCellToReceipt(headerTable, "Qiymət", boldTextFont, com.lowagie.text.Element.ALIGN_RIGHT);
            addCellToReceipt(headerTable, "Toplam", boldTextFont, com.lowagie.text.Element.ALIGN_RIGHT);
            document.add(headerTable);

            document.add(new com.lowagie.text.Paragraph(" ", textFont));

            // Sətirlər
            com.lowagie.text.pdf.PdfPTable itemsTable = new com.lowagie.text.pdf.PdfPTable(4);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{40, 16, 24, 20});

            for (InvoiceItem item : invoice.getItems()) {
                String barcode = item.getBarcode() != null ? item.getBarcode() : "";
                String quantityStr = item.getQuantity() + "əd";
                String priceStr = String.format(java.util.Locale.US, "%.2f", item.getUnitPrice());
                String totalStr = String.format(java.util.Locale.US, "%.2f", item.getLineTotal());

                addCellToReceipt(itemsTable, barcode, textFont, com.lowagie.text.Element.ALIGN_LEFT);
                addCellToReceipt(itemsTable, quantityStr, textFont, com.lowagie.text.Element.ALIGN_CENTER);
                addCellToReceipt(itemsTable, priceStr, textFont, com.lowagie.text.Element.ALIGN_RIGHT);
                addCellToReceipt(itemsTable, totalStr, textFont, com.lowagie.text.Element.ALIGN_RIGHT);

                // Məhsulun adı növbəti sətirdə bütün sütunları əhatə edir
                com.lowagie.text.pdf.PdfPCell nameCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(item.getName(), textFont));
                nameCell.setColspan(4);
                nameCell.setBorder(com.lowagie.text.pdf.PdfPCell.NO_BORDER);
                nameCell.setPaddingTop(0);
                nameCell.setPaddingBottom(4);
                itemsTable.addCell(nameCell);
            }
            document.add(itemsTable);
            document.add(new com.lowagie.text.Paragraph("----------------------------------", textFont));

            // Yekunlar Cədvəli
            com.lowagie.text.pdf.PdfPTable summaryTable = new com.lowagie.text.pdf.PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{70, 30});

            String totalAmountStr = String.format(java.util.Locale.US, "%.2f", invoice.getTotalAmount());

            addCellToReceipt(summaryTable, "CƏMİ MƏBLƏĞ", boldTextFont, com.lowagie.text.Element.ALIGN_LEFT);
            addCellToReceipt(summaryTable, totalAmountStr, boldTextFont, com.lowagie.text.Element.ALIGN_RIGHT);

            addCellToReceipt(summaryTable, "ÜMUMİ ENDİRİM", textFont, com.lowagie.text.Element.ALIGN_LEFT);
            addCellToReceipt(summaryTable, "0.00", textFont, com.lowagie.text.Element.ALIGN_RIGHT);

            addCellToReceipt(summaryTable, "FƏRDİ ENDİRİM", textFont, com.lowagie.text.Element.ALIGN_LEFT);
            addCellToReceipt(summaryTable, "0.00", textFont, com.lowagie.text.Element.ALIGN_RIGHT);

            addCellToReceipt(summaryTable, "YEKUN MƏBLƏĞ", boldTextFont, com.lowagie.text.Element.ALIGN_LEFT);
            addCellToReceipt(summaryTable, totalAmountStr, boldTextFont, com.lowagie.text.Element.ALIGN_RIGHT);

            document.add(summaryTable);
            document.add(new com.lowagie.text.Paragraph("----------------------------------", textFont));

            // Ödəniş üsulu cədvəli
            com.lowagie.text.Paragraph payTitle = new com.lowagie.text.Paragraph("ÖDƏNİŞ ÜSULU", sectionFont);
            payTitle.setSpacingAfter(4);
            document.add(payTitle);

            com.lowagie.text.pdf.PdfPTable paymentTable = new com.lowagie.text.pdf.PdfPTable(2);
            paymentTable.setWidthPercentage(100);
            paymentTable.setWidths(new float[]{70, 30});

            BigDecimal cashVal = BigDecimal.ZERO;
            BigDecimal cardVal = BigDecimal.ZERO;

            if (invoice.getPaymentMethod() == az.techvibeds.specialservice.enums.PaymentMethod.CASH) {
                cashVal = invoice.getTotalAmount();
            } else if (invoice.getPaymentMethod() == az.techvibeds.specialservice.enums.PaymentMethod.CARD ||
                       invoice.getPaymentMethod() == az.techvibeds.specialservice.enums.PaymentMethod.BANK_TRANSFER) {
                cardVal = invoice.getTotalAmount();
            }

            addCellToReceipt(paymentTable, "Nəğd", textFont, com.lowagie.text.Element.ALIGN_LEFT);
            addCellToReceipt(paymentTable, String.format(java.util.Locale.US, "%.2f", cashVal), textFont, com.lowagie.text.Element.ALIGN_RIGHT);

            addCellToReceipt(paymentTable, "Nəğdsiz", textFont, com.lowagie.text.Element.ALIGN_LEFT);
            addCellToReceipt(paymentTable, String.format(java.util.Locale.US, "%.2f", cardVal), textFont, com.lowagie.text.Element.ALIGN_RIGHT);

            addCellToReceipt(paymentTable, "Bonus", textFont, com.lowagie.text.Element.ALIGN_LEFT);
            addCellToReceipt(paymentTable, "0.00", textFont, com.lowagie.text.Element.ALIGN_RIGHT);

            document.add(paymentTable);
            document.add(new com.lowagie.text.Paragraph("----------------------------------", textFont));

            // Qaimə nömrəsi üçün barkod yaradılması (Code 128)
            try {
                com.google.zxing.oned.Code128Writer barcodeWriter = new com.google.zxing.oned.Code128Writer();
                String barcodeValue = invoice.getInvoiceNumber();
                com.google.zxing.common.BitMatrix bitMatrix = barcodeWriter.encode(barcodeValue, com.google.zxing.BarcodeFormat.CODE_128, 150, 30);
                java.awt.image.BufferedImage barcodeImage = com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(bitMatrix);
                
                java.io.ByteArrayOutputStream imageStream = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(barcodeImage, "PNG", imageStream);
                
                com.lowagie.text.Image pdfImage = com.lowagie.text.Image.getInstance(imageStream.toByteArray());
                pdfImage.setAlignment(com.lowagie.text.Image.MIDDLE);
                document.add(pdfImage);
            } catch (Exception e) {
                log.error("Receipt barcode generation error: {}", e.getMessage());
            }

            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Receipt PDF generation error: {}", e.getMessage());
            throw new RuntimeException("Çek PDF formatında yaradılarkən xəta baş verdi: " + e.getMessage(), e);
        }
    }

    private void addCellToReceipt(com.lowagie.text.pdf.PdfPTable table, String text, com.lowagie.text.Font font, int alignment) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(text, font));
        cell.setBorder(com.lowagie.text.pdf.PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(1);
        table.addCell(cell);
    }

    private java.util.List<InvoiceItemDto> mergeDuplicateItemDtos(java.util.List<InvoiceItemDto> items) {
        java.util.Map<String, InvoiceItemDto> grouped = new java.util.LinkedHashMap<>();
        for (InvoiceItemDto item : items) {
            String key = "";
            if (item.getProductId() != null) {
                key = "ID_" + item.getProductId();
            } else if (item.getBarcode() != null && !item.getBarcode().isBlank()) {
                key = "BC_" + item.getBarcode().trim();
            } else if (item.getName() != null && !item.getName().isBlank()) {
                key = "NM_" + item.getName().trim();
            } else {
                key = "UNKNOWN_" + java.util.UUID.randomUUID();
            }

            // Endirimi fərqli olan sətirləri birləşdirmə
            if (item.getDiscount() != null && item.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                key += "_D_" + item.getDiscount() + "_" + (item.getDiscountType() != null ? item.getDiscountType() : "AMOUNT");
            }

            if (grouped.containsKey(key)) {
                InvoiceItemDto existing = grouped.get(key);
                int newQty = (existing.getQuantity() != null ? existing.getQuantity() : 0) + 
                             (item.getQuantity() != null ? item.getQuantity() : 0);
                existing.setQuantity(newQty);
            } else {
                InvoiceItemDto clone = new InvoiceItemDto();
                clone.setProductId(item.getProductId());
                clone.setBarcode(item.getBarcode());
                clone.setName(item.getName());
                clone.setUnit(item.getUnit());
                clone.setUnitPrice(item.getUnitPrice());
                clone.setQuantity(item.getQuantity() != null ? item.getQuantity() : 1);
                clone.setWarehouseId(item.getWarehouseId());
                clone.setVatStatus(item.getVatStatus());
                clone.setVatRate(item.getVatRate());
                clone.setDiscount(item.getDiscount());
                clone.setDiscountType(item.getDiscountType());
                grouped.put(key, clone);
            }
        }
        return new java.util.ArrayList<>(grouped.values());
    }
}
