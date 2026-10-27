package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.currency.CurrencyDto;
import az.techvibeds.specialservice.dtos.foreignPurchase.ForeignPurchaseCreateDto;
import az.techvibeds.specialservice.dtos.foreignPurchase.ForeignPurchaseItemDto;
import az.techvibeds.specialservice.dtos.foreignPurchase.ForeignPurchaseReadDto;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.repositories.*;
import az.techvibeds.specialservice.services.ForeignPurchaseService;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForeignPurchaseServiceImpl implements ForeignPurchaseService {

    private final ForeignPurchaseRepository purchaseRepository;
    private final ForeignPurchaseSequenceRepository sequenceRepository;
    private final CurrencyRepository currencyRepository;
    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ForeignPurchaseReadDto createPurchase(ForeignPurchaseCreateDto dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Currency currency = currencyRepository.findByCode(dto.getCurrencyCode())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "code", dto.getCurrencyCode()));

        ForeignPurchase purchase = new ForeignPurchase();
        purchase.setInvoiceDate(dto.getInvoiceDate());
        purchase.setDeliveryDate(dto.getDeliveryDate());
        purchase.setPaymentMethod(dto.getPaymentMethod());
        purchase.setCurrency(currency);
        purchase.setExchangeRate(dto.getExchangeRate());
        purchase.setNotes(dto.getNotes());
        purchase.setCompany(user.getCompany());

        if (dto.getSupplierId() != null) {
            Partner supplier = partnerRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", dto.getSupplierId()));
            purchase.setSupplier(supplier);
        }

        if (dto.getCustomerId() != null) {
            Partner customer = partnerRepository.findById(dto.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", dto.getCustomerId()));
            purchase.setCustomer(customer);
        }

        purchase.setInvoiceNumber(generateInvoiceNumber(user.getCompany()));

        if (dto.getItems() != null) {
            for (ForeignPurchaseItemDto itemDto : dto.getItems()) {
                ForeignPurchaseItem item = new ForeignPurchaseItem();
                item.setName(itemDto.getName());
                item.setUnit(itemDto.getUnit());
                item.setUnitPrice(itemDto.getUnitPrice());
                item.setQuantity(itemDto.getQuantity());
                item.setBarcode(itemDto.getBarcode());
                
                if (itemDto.getProductId() != null) {
                    Product product = productRepository.findById(itemDto.getProductId()).orElse(null);
                    item.setProduct(product);
                }
                
                purchase.addItem(item);
            }
        }

        purchase.recalculateTotals();

        return convertToReadDto(purchaseRepository.save(purchase));
    }

    private String generateInvoiceNumber(Company company) {
        int year = LocalDate.now().getYear();
        ForeignPurchaseSequence sequence = sequenceRepository.findByCompanyIdAndYear(company.getId(), year)
                .orElseGet(() -> {
                    ForeignPurchaseSequence s = new ForeignPurchaseSequence();
                    s.setCompany(company);
                    s.setYear(year);
                    s.setLastNumber(0L);
                    return s;
                });
        
        String nextNumber = sequence.generateNextNumber();
        sequenceRepository.save(sequence);
        return nextNumber;
    }

    @Override
    public Page<ForeignPurchaseReadDto> getPurchases(Pageable pageable, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        return purchaseRepository.findAllByCompanyId(user.getCompany().getId(), pageable)
                .map(this::convertToReadDto);
    }

    @Override
    public ForeignPurchaseReadDto getPurchaseById(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        ForeignPurchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "id", id));

        if (!purchase.getCompany().getId().equals(user.getCompany().getId())) {
            throw new AccessDeniedException("Bu satınalma sizin şirkətə aid deyil");
        }

        return convertToReadDto(purchase);
    }

    @Override
    @Transactional
    public void deletePurchase(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        ForeignPurchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "id", id));

        if (!purchase.getCompany().getId().equals(user.getCompany().getId())) {
            throw new AccessDeniedException("Bu satınalma sizin şirkətə aid deyil");
        }

        purchaseRepository.delete(purchase);
    }

    @Override
    @Transactional
    public ForeignPurchaseReadDto importItemsFromExcel(Long purchaseId, MultipartFile file, String userEmail) throws Exception {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        ForeignPurchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "id", purchaseId));

        if (!purchase.getCompany().getId().equals(user.getCompany().getId())) {
            throw new AccessDeniedException("Bu satınalma sizin şirkətə aid deyil");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header

                String name = getCellValueAsString(row.getCell(0));
                if (name == null || name.isBlank()) continue;

                String unit = getCellValueAsString(row.getCell(1));
                BigDecimal unitPrice = getCellValueAsBigDecimal(row.getCell(2));
                BigDecimal quantity = getCellValueAsBigDecimal(row.getCell(3));
                String barcode = getCellValueAsString(row.getCell(4));

                if (unitPrice == null || quantity == null) continue;

                ForeignPurchaseItem item = new ForeignPurchaseItem();
                item.setName(name);
                item.setUnit(unit);
                item.setUnitPrice(unitPrice);
                item.setQuantity(quantity);
                item.setBarcode(barcode);
                purchase.addItem(item);
            }
        }

        purchase.recalculateTotals();
        return convertToReadDto(purchaseRepository.save(purchase));
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        return null;
    }

    private ForeignPurchaseReadDto convertToReadDto(ForeignPurchase entity) {
        ForeignPurchaseReadDto dto = new ForeignPurchaseReadDto();
        dto.setId(entity.getId());
        dto.setInvoiceNumber(entity.getInvoiceNumber());
        dto.setInvoiceDate(entity.getInvoiceDate());
        dto.setDeliveryDate(entity.getDeliveryDate());
        dto.setCustomerName(entity.getCustomer() != null ? entity.getCustomer().getName() : null);
        dto.setSupplierName(entity.getSupplier() != null ? entity.getSupplier().getName() : null);
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setExchangeRate(entity.getExchangeRate());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setTotalAmountAzn(entity.getTotalAmountAzn());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());

        CurrencyDto currencyDto = new CurrencyDto();
        currencyDto.setId(entity.getCurrency().getId());
        currencyDto.setCode(entity.getCurrency().getCode());
        currencyDto.setName(entity.getCurrency().getName());
        currencyDto.setSymbol(entity.getCurrency().getSymbol());
        currencyDto.setCurrentRate(entity.getCurrency().getCurrentRate());
        dto.setCurrency(currencyDto);

        if (entity.getItems() != null) {
            dto.setItems(entity.getItems().stream().map(item -> {
                ForeignPurchaseItemDto itemDto = new ForeignPurchaseItemDto();
                itemDto.setId(item.getId());
                itemDto.setName(item.getName());
                itemDto.setUnit(item.getUnit());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setBarcode(item.getBarcode());
                itemDto.setLineTotal(item.getLineTotal());
                itemDto.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
                return itemDto;
            }).collect(Collectors.toList()));
        }

        return dto;
    }
}
