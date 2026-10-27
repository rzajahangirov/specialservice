package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.semiProduct.SemiProductCreateDto;
import az.techvibeds.specialservice.dtos.semiProduct.SemiProductReadDto;
import az.techvibeds.specialservice.dtos.semiProduct.SemiProductUpdateDto;
import az.techvibeds.specialservice.enums.SemiProductStatus;
import az.techvibeds.specialservice.enums.UnitType;
import az.techvibeds.specialservice.enums.VatStatus;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.models.Category;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.SemiProduct;
import az.techvibeds.specialservice.models.WarehouseProduct;
import az.techvibeds.specialservice.repositories.SemiProductRepository;
import az.techvibeds.specialservice.services.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemiProductServiceImpl implements SemiProductService {
    private final SemiProductRepository semiProductRepository;
    private final CompanyService companyService;
    private final CategoryService categoryService;
    private final WarehouseProductService warehouseProductService;

    @Override
    @Transactional
    public SemiProductReadDto create(SemiProductCreateDto dto, String email) {
        log.info("Creating semi-product: {} for user: {}", dto.getName(), email);
        Company company = companyService.findByUserEmail(email);

        // Barkod (productCode) əgər boşdursa avtomatik generasiya olunur
        String productCode = dto.getProductCode();
        if (productCode == null || productCode.trim().isEmpty()) {
            productCode = generateUniqueBarcode(company.getId());
        }

        final String finalProductCode = productCode;

        SemiProduct sp = semiProductRepository.findByProductCodeAndCompany_Id(finalProductCode, company.getId())
                .orElseGet(() -> {
                    SemiProduct newSp = new SemiProduct();
                    newSp.setCompany(company);
                    newSp.setName(dto.getName());
                    newSp.setProductCode(finalProductCode);
                    newSp.setDescription(dto.getDescription());
                    newSp.setPrice(dto.getPrice());
                    newSp.setUnit(dto.getUnitType());
                    newSp.setStatus(dto.getProductStatus());
                    newSp.setCategory(categoryService.findById(dto.getCategoryId()));
                    newSp.setStock(0L);

                    // ƏDV Statusu
                    newSp.setVatStatus(dto.getVatStatus() != null ? VatStatus.fromString(dto.getVatStatus()) : VatStatus.WITHOUT_VAT);
                    newSp.setVatRate(dto.getVatRate());
                    newSp.calculatePriceWithVat();

                    return newSp;
                });

        sp.setName(dto.getName());

        sp.setPrice(dto.getPrice());
        sp.setVatStatus(dto.getVatStatus() != null ? VatStatus.fromString(dto.getVatStatus()) : VatStatus.WITHOUT_VAT);
        sp.setVatRate(dto.getVatRate());
        sp.calculatePriceWithVat();

        // Məhsul yaradılanda stock həmişə 0 olur
        if (sp.getId() == null) {
            sp.setStock(0L);
        }
        semiProductRepository.save(sp);

        // Anbarda miqdar ilkin olaraq 0.0 qeyd olunur
        WarehouseProduct wp = warehouseProductService.createWarehouseSemiProduct(
                sp, dto.getWarehouseId(), 0.0, email);

        return mapToReadDto(sp, wp);
    }

    @Override
    @Transactional
    public SemiProductReadDto update(Long id, SemiProductUpdateDto dto, String email) {
        SemiProduct sp = findAndValidateOwner(id, email);

        sp.setName(dto.getName());
        sp.setUnit(dto.getUnitType());
        sp.setStatus(dto.getProductStatus());
        sp.setDescription(dto.getDescription());
        sp.setPrice(dto.getPrice());

        if (dto.getProductCode() != null) {
            sp.setProductCode(dto.getProductCode());
        }

        if (dto.getCategoryId() != null) {
            sp.setCategory(categoryService.findById(dto.getCategoryId()));
        }

        sp.setVatStatus(dto.getVatStatus() != null ? VatStatus.fromString(dto.getVatStatus()) : VatStatus.WITHOUT_VAT);
        sp.setVatRate(dto.getVatRate());
        sp.calculatePriceWithVat();

        log.info("Semi-product ID: {} updated", id);
        semiProductRepository.save(sp);

        // Anbar qeydini yenilə (əgər warehouseId göndərilibsə)
        WarehouseProduct wp = null;
        if (dto.getWarehouseId() != null && dto.getQuantity() != null) {
            List<WarehouseProduct> warehouseProducts = sp.getWarehouseProducts();
            Integer newQuantity = dto.getQuantity();

            if (warehouseProducts != null && !warehouseProducts.isEmpty()) {
                wp = warehouseProducts.get(0);
                Integer oldQuantity = wp.getQuantity() != null ? wp.getQuantity() : 0;
                warehouseProductService.updateInventoryWarehouseProduct(wp, dto.getWarehouseId(), newQuantity);

                long currentStock = sp.getStock() != null ? sp.getStock() : 0L;
                sp.setStock(currentStock - oldQuantity + newQuantity);
                semiProductRepository.save(sp);
            } else {
                wp = warehouseProductService.createWarehouseSemiProduct(sp, dto.getWarehouseId(), (double) newQuantity, email);
                long currentStock = sp.getStock() != null ? sp.getStock() : 0L;
                sp.setStock(currentStock + newQuantity);
                semiProductRepository.save(sp);
            }
        }

        if (wp == null && sp.getWarehouseProducts() != null && !sp.getWarehouseProducts().isEmpty()) {
            wp = sp.getWarehouseProducts().get(0);
        }

        return mapToReadDto(sp, wp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SemiProductReadDto> getSemiProducts(String keyword, SemiProductStatus status, String email) {
        log.info("Fetching SemiProducts for user: {} | Keyword: {}, Status: {}", email, keyword, status);

        Long companyId = companyService.findByUserEmail(email).getId();
        List<SemiProduct> semiProducts;

        if (keyword == null || keyword.trim().isEmpty()) {
            semiProducts = semiProductRepository.findByCompanyAndStatus(companyId, status);
        } else {
            semiProducts = semiProductRepository.findByCompanyAndStatusAndKeyword(companyId, status, keyword);
        }

        return semiProducts.stream()
                .map(sp -> {
                    WarehouseProduct wp = (sp.getWarehouseProducts() != null && !sp.getWarehouseProducts().isEmpty())
                            ? sp.getWarehouseProducts().get(0) : null;
                    return mapToReadDto(sp, wp);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id, String email) {
        SemiProduct sp = findAndValidateOwner(id, email);
        semiProductRepository.delete(sp);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportToExcel(String userEmail) throws Exception {
        Company company = companyService.findByUserEmail(userEmail);
        List<SemiProduct> semiProducts = semiProductRepository.findAllByCompany_Id(company.getId());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Mallar");

            // Başlıq stili
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Başlıq sətri
            String[] columns = {"Ad", "Məhsul Kodu", "Təsvir", "Ölçü Vahidi", "Stok",
                    "Kateqoriya", "Status", "Qiymət", "ƏDV Statusu", "ƏDV Faizi", "ƏDV-li Qiymət"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Məlumat sətirləri
            int rowIdx = 1;
            for (SemiProduct sp : semiProducts) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sp.getName() != null ? sp.getName() : "");
                row.createCell(1).setCellValue(sp.getProductCode() != null ? sp.getProductCode() : "");
                row.createCell(2).setCellValue(sp.getDescription() != null ? sp.getDescription() : "");
                row.createCell(3).setCellValue(sp.getUnit() != null ? sp.getUnit().name() : "");
                row.createCell(4).setCellValue(sp.getStock() != null ? sp.getStock() : 0);
                row.createCell(5).setCellValue(sp.getCategory() != null ? sp.getCategory().getName() : "");
                row.createCell(6).setCellValue(sp.getStatus() != null ? sp.getStatus().name() : "");
                row.createCell(7).setCellValue(sp.getPrice() != null ? sp.getPrice().doubleValue() : 0);
                row.createCell(8).setCellValue(sp.getVatStatus() != null ? sp.getVatStatus().name() : "WITHOUT_VAT");
                row.createCell(9).setCellValue(sp.getVatRate() != null ? sp.getVatRate().doubleValue() : 0);
                row.createCell(10).setCellValue(sp.getPriceWithVat() != null ? sp.getPriceWithVat().doubleValue() : 0);
            }

            // Sütunları auto-resize et
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Override
    @Transactional
    public void importFromExcel(MultipartFile file, String userEmail) throws Exception {
        Company company = companyService.findByUserEmail(userEmail);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                try {
                    String name = getCellValueAsString(row.getCell(0));
                    String productCode = getCellValueAsString(row.getCell(1));
                    String description = getCellValueAsString(row.getCell(2));
                    String unitStr = getCellValueAsString(row.getCell(3));
                    String statusStr = getCellValueAsString(row.getCell(6));

                    if (name == null || name.trim().isEmpty()) {
                        continue;
                    }

                    SemiProduct sp;
                    if (productCode != null && !productCode.trim().isEmpty()) {
                        sp = semiProductRepository.findByProductCodeAndCompany_Id(productCode.trim(), company.getId())
                                .orElse(new SemiProduct());
                    } else {
                        sp = new SemiProduct();
                        productCode = generateUniqueBarcode(company.getId());
                    }

                    sp.setName(name.trim());
                    sp.setProductCode(productCode.trim());
                    sp.setDescription(description);
                    sp.setCompany(company);

                    if (unitStr != null && !unitStr.trim().isEmpty()) {
                        try {
                            sp.setUnit(UnitType.valueOf(unitStr.trim()));
                        } catch (IllegalArgumentException e) {
                            sp.setUnit(UnitType.PIECE);
                        }
                    } else {
                        sp.setUnit(UnitType.PIECE);
                    }

                    if (statusStr != null && !statusStr.trim().isEmpty()) {
                        try {
                            sp.setStatus(SemiProductStatus.valueOf(statusStr.trim()));
                        } catch (IllegalArgumentException e) {
                            sp.setStatus(SemiProductStatus.AVAILABLE);
                        }
                    } else {
                        sp.setStatus(SemiProductStatus.AVAILABLE);
                    }

                    if (sp.getStock() == null) {
                        sp.setStock(0L);
                    }

                    Cell priceCell = row.getCell(7);
                    if (priceCell != null && priceCell.getCellType() == CellType.NUMERIC) {
                        sp.setPrice(BigDecimal.valueOf(priceCell.getNumericCellValue()));
                    }

                    semiProductRepository.save(sp);
                    log.info("Imported semi-product: {}", name);
                } catch (Exception e) {
                    log.error("Error processing row {}: {}", row.getRowNum(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Excel import error: {}", e.getMessage());
            throw new BusinessException("Error processing Excel file: " + e.getMessage());
        }
    }

    private SemiProduct findAndValidateOwner(Long id, String email) {
        SemiProduct sp = semiProductRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Semi-product not found"));

        Long userCompanyId = companyService.findByUserEmail(email).getId();
        if (!sp.getCompany().getId().equals(userCompanyId)) {
            throw new AccessDeniedException("Access denied to this semi-product");
        }
        return sp;
    }

    private SemiProductReadDto mapToReadDto(SemiProduct sp, WarehouseProduct wp) {
        SemiProductReadDto.SemiProductReadDtoBuilder builder = SemiProductReadDto.builder()
                .semiProductId(sp.getId())
                .name(sp.getName())
                .productCode(sp.getProductCode())
                .description(sp.getDescription())
                .price(sp.getPrice())
                .totalStock(sp.getStock())
                .categoryName(sp.getCategory() != null ? sp.getCategory().getName() : null)
                .unitType(sp.getUnit() != null ? sp.getUnit().name() : null)
                .productStatus(sp.getStatus() != null ? sp.getStatus().name() : null)
                .vatStatus(sp.getVatStatus() != null ? sp.getVatStatus().name() : "WITHOUT_VAT")
                .vatRate(sp.getVatRate())
                .priceWithVat(sp.getPriceWithVat());

        if (wp != null) {
            builder.id(wp.getId())
                    .warehouseProductId(wp.getId())
                    .warehouseName(wp.getWarehouse() != null ? wp.getWarehouse().getName() : null)
                    .quantity(wp.getQuantity());
        } else {
            builder.id(sp.getId());
        }

        return builder.build();
    }

    private String generateUniqueBarcode(Long companyId) {
        String barcode;
        do {
            barcode = az.techvibeds.specialservice.utils.BarcodeGenerator.generateEAN13(companyId);
        } while (semiProductRepository.findByProductCodeAndCompany_Id(barcode, companyId).isPresent());
        return barcode;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double val = cell.getNumericCellValue();
                if (val == (long) val) {
                    return String.valueOf((long) val);
                }
                return String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        double valFormula = cell.getNumericCellValue();
                        if (valFormula == (long) valFormula) {
                            return String.valueOf((long) valFormula);
                        }
                        return String.valueOf(valFormula);
                    } catch (Exception ex) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }
}
