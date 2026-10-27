package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.product.*;
import az.techvibeds.specialservice.enums.ActivityTypeWarehouse;
import az.techvibeds.specialservice.enums.ProductStatus;
import az.techvibeds.specialservice.enums.ProductType;
import az.techvibeds.specialservice.enums.VatStatus;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.repositories.ProductRepository;
import az.techvibeds.specialservice.repositories.UnitRepository;
import az.techvibeds.specialservice.repositories.WarehouseActivityRepository;
import az.techvibeds.specialservice.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final WarehouseService warehouseService;
    private final CategoryService categoryService;
    private final WarehouseProductService warehouseProductService;
    private final CompanyService companyService;
    private final UnitService unitService;
    private final UnitRepository unitRepository;
    private final NotificationService notificationService;
    private final ExternalBarcodeService externalBarcodeService;
    private final WarehouseActivityRepository warehouseActivityRepository;

    @Override
    @Transactional(readOnly = true)
    public Long calculateCompanyStock(Long companyId) {
        return productRepository.sumStockByCompanyId(companyId).orElse(0L);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getProductCountByCompany(Long companyId) {
        return productRepository.countByCompanyId(companyId).intValue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductInventoryDto> getProductsByCompany(String email, ProductType productType) {
        Company company = companyService.findByUserEmail(email);
        if (company == null) {
            return new ArrayList<>();
        }
        List<WarehouseProduct> wpList = warehouseProductService.findWarehouseProductByCompany_Id(
                warehouseService.findAllByCompany_Id(company.getId())
        );

        return wpList.stream()
                .filter(wp -> wp.getProduct() != null) // Mal (SemiProduct) olan qeydləri ötürürük
                .filter(wp -> productType == null || wp.getProduct().getProductType() == productType)
                .map(this::mapToProductInventoryDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void uploadProductsFromExcel(MultipartFile file, Company company) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Log columns and first data row
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                log.info("--- Excel Upload Debug Info ---");
                log.info("Headers count: {}", headerRow.getLastCellNum());
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    log.info("Header Index {}: '{}'", i, getCellValueAsString(headerRow.getCell(i)));
                }
            }
            if (sheet.getPhysicalNumberOfRows() > 1) {
                Row firstDataRow = sheet.getRow(1);
                if (firstDataRow != null) {
                    log.info("First Data Row Values:");
                    for (int i = 0; i < firstDataRow.getLastCellNum(); i++) {
                        log.info("Cell Index {}: '{}'", i, getCellValueAsString(firstDataRow.getCell(i)));
                    }
                }
            }
            log.info("--------------------------------");

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                // Frontend Excel şablonu sütun sırası:
                // 0=Məhsul Kodu, 1=Məhsul Adı, 2=Anbar, 3=Kateqoriya,
                // 4=Vahid, 5=Mövcud Stok, 6=Qiymət, 7=Status
                String productCode = getCellValueAsString(row.getCell(0));
                if (productCode == null || productCode.trim().isEmpty()) {
                    continue; // Boş sətirləri keçirik
                }

                String warehouseName = getCellValueAsString(row.getCell(2));
                double quantity = getCellValueAsDouble(row.getCell(5));

                Product product = productRepository.findByProductCodeAndCompanyId(productCode, company.getId())
                        .orElseGet(() -> createProductFromRow(row, company));

                if (product.getId() != null) {
                    long currentStock = product.getStock() != null ? product.getStock() : 0L;
                    product.setStock(currentStock + (long) Math.max(0, quantity));

                    // Əgər ölçü vahidi yoxdursa və Excel-də qeyd olunubsa, onu da təyin edirik
                    if (product.getUnit() == null) {
                        String unitName = getCellValueAsString(row.getCell(4));
                        if (unitName != null && !unitName.trim().isEmpty()) {
                            unitRepository.findByName(unitName.trim()).ifPresent(product::setUnit);
                        }
                    }
                }

                productRepository.save(product);

                if (warehouseName != null && !warehouseName.trim().isEmpty()) {
                    warehouseProductService.createWarehouseProductFromExcel(product, warehouseName, quantity);
                }
            }
        } catch (Exception e) {
            log.error("Excel import error: {}", e.getMessage());
            throw new BusinessException("Error processing Excel file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ProductReadDto createProductAndInventoryRecord(ProductCreateDto dto, Company company) {
        Unit unit = unitService.findUnitById(dto.getUnitId());
        if (unit == null) {
            throw new IllegalArgumentException("Seçilmiş ölçü vahidi tapılmadı!");
        }

        // Barkod (productCode) əgər boşdursa avtomatik generasiya olunur
        String productCode = dto.getProductCode();
        if (productCode == null || productCode.trim().isEmpty()) {
            productCode = generateUniqueBarcode(company.getId());
        }

        final String finalProductCode = productCode;
        String cleanedName = cleanProductName(dto.getName());

        Product product = productRepository.findByProductCodeAndCompanyId(finalProductCode, company.getId())
                .orElseGet(() -> {
                    Product newProd = new Product();
                    newProd.setCompany(company);
                    newProd.setName(cleanedName);
                    newProd.setProductCode(finalProductCode);
                    newProd.setCategory(categoryService.findById(dto.getCategoryId()));
                    newProd.setUnit(unit);
                    newProd.setProductStatus(ProductStatus.fromString(dto.getProductStatus()));

                    // Məhsul Tipi (PRODUCT / GOODS)
                    if (dto.getProductType() != null) {
                        try {
                            newProd.setProductType(ProductType.valueOf(dto.getProductType().toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            newProd.setProductType(ProductType.GOODS);
                        }
                    } else {
                        newProd.setProductType(ProductType.GOODS);
                    }

                    newProd.setPrice(dto.getPrice());
                    newProd.setPurchasePrice(dto.getPurchasePrice());
                    newProd.setSalePrice(dto.getSalePrice());
                    newProd.setMarginPercentage(dto.getMarginPercentage());
                    newProd.setDiscount(dto.getDiscount());
                    newProd.setVatStatus(dto.getVatStatus() != null ? VatStatus.fromString(dto.getVatStatus()) : VatStatus.WITHOUT_VAT);
                    newProd.setVatRate(dto.getVatRate());
                    newProd.calculatePriceWithVat();
                    newProd.setStock(null);
                    newProd.setMinStockLimit(dto.getMinStockLimit() != null ? dto.getMinStockLimit() : 0L);

                    return newProd;
                });

        product.setUnit(unit);
        product.setName(cleanedName);

        product.setPrice(dto.getPrice());
        product.setPurchasePrice(dto.getPurchasePrice());
        product.setSalePrice(dto.getSalePrice());
        product.setMarginPercentage(dto.getMarginPercentage());
        product.setDiscount(dto.getDiscount());
        product.setVatStatus(dto.getVatStatus() != null ? VatStatus.fromString(dto.getVatStatus()) : VatStatus.WITHOUT_VAT);
        product.setVatRate(dto.getVatRate());
        product.calculatePriceWithVat();

        // Məhsul yaradılanda stock həmişə null olur
        product.setStock(null);
        productRepository.save(product);

        // Anbarda miqdar ilkin olaraq 0.0 (və ya GOODS üçün null) qeyd olunur
        WarehouseProduct wp = warehouseProductService.createWarehouseProduct(product, dto.getWarehouseId(), 0.0);
        return mapToProductReadDto(product, wp);
    }

    @Override
    @Transactional
    public ProductReadDto updateInventorProduct(ProductInventoryUpdateDto dto, String userEmail) {
        WarehouseProduct wp = warehouseProductService.findWarehouseProductById(dto.getWarehouseProductId());
        Company company = companyService.findByUserEmail(userEmail);

        if (!wp.getProduct().getCompany().getId().equals(company.getId())) {
            throw new BusinessException("Unauthorized access to this product", HttpStatus.FORBIDDEN);
        }

        Product product = wp.getProduct();
        product.setName(cleanProductName(dto.getName()));
        product.setProductCode(dto.getProductCode());
        product.setCategory(categoryService.findById(dto.getCategoryId()));
        product.setProductStatus(ProductStatus.fromString(dto.getProductStatus()));

        if (dto.getUnitId() != null) {
            Unit unit = unitService.findUnitById(dto.getUnitId());
            if (unit != null) {
                product.setUnit(unit);
            }
        }
        
        // Məhsul Tipi (PRODUCT / GOODS)
        if (dto.getProductType() != null) {
            try {
                product.setProductType(ProductType.valueOf(dto.getProductType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // ignore invalid types
            }
        }
        
        product.setPrice(dto.getPrice());
        product.setPurchasePrice(dto.getPurchasePrice());
        product.setSalePrice(dto.getSalePrice());
        product.setMarginPercentage(dto.getMarginPercentage());
        product.setDiscount(dto.getDiscount());
        product.setVatStatus(dto.getVatStatus() != null ? VatStatus.fromString(dto.getVatStatus()) : VatStatus.WITHOUT_VAT);
        product.setVatRate(dto.getVatRate());
        product.calculatePriceWithVat();
        if (dto.getMinStockLimit() != null) {
            product.setMinStockLimit(dto.getMinStockLimit());
        }

        // Anbar yenilənməsi: əvvəlcə yeni anbarı tapırıq
        Warehouse newWarehouse = warehouseService.getWarehouseById(dto.getWarehouseId());

        if (product.getProductType() == ProductType.GOODS) {
            wp.setQuantity(null);
            product.setStock(null);
        } else {
            Integer oldQuantity = wp.getQuantity() != null ? wp.getQuantity() : 0;
            Integer newQuantity = dto.getQuantity() != null ? dto.getQuantity() : 0;
            wp.setQuantity(Math.max(0, newQuantity));

            long currentStock = product.getStock() != null ? product.getStock() : 0L;
            long newStock = currentStock - oldQuantity + newQuantity;
            product.setStock(newStock);
            
            Long limit = product.getMinStockLimit() != null ? product.getMinStockLimit() : 0L;
            if (newStock < limit && currentStock >= limit) {
                notificationService.createEmailNotification(
                    String.format("Məhsul stoku limitdən aşağı düşdü: %s (Cari stok: %d, Limit: %d)", product.getName(), newStock, limit),
                    company
                );
            }
        }

        wp.setWarehouse(newWarehouse);

        // Əvvəlcə product-u, sonra wp-ni saxlayırıq — Hibernate flush sırasını düzgün edir
        productRepository.save(product);
        warehouseProductService.saveWarehouseProduct(wp);
        return mapToProductReadDto(product, wp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductInventoryDto> getFilteredProducts(Long warehouseId, Long categoryId, String status, String email) {
        Company company = companyService.findByUserEmail(email);
        if (company == null) return new ArrayList<>();
        Long companyId = company.getId();

        if (warehouseId != null) {
            Warehouse warehouse = warehouseService.findById(warehouseId);
            if (!warehouse.getCompany().getId().equals(companyId)) {
                throw new BusinessException("Warehouse does not belong to your company", HttpStatus.FORBIDDEN);
            }
        }

        return warehouseProductService.findFiltered(companyId, warehouseId)
                .stream()
                .filter(wp -> wp.getProduct() != null)
                .filter(wp -> categoryId == null || (wp.getProduct().getCategory() != null && wp.getProduct().getCategory().getId().equals(categoryId)))
                .filter(wp -> status == null || wp.getProduct().getProductStatus().name().equalsIgnoreCase(status))
                .map(this::mapToProductInventoryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductInventoryDto> getSearchedProducts(String productName, Principal principal) {
        Long companyId = companyService.findByUserEmail(principal.getName()).getId();
        return productRepository.searchProductsByCompanyAndKeyword(companyId, productName)
                .stream()
                .flatMap(p -> p.getWarehouseProducts().stream())
                .map(this::mapToProductInventoryDto)
                .toList();
    }

    @Override
    @Transactional
    public void addQuantityToProduct(String productCode, Long warehouseId, Integer quantity, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);
        Product product = productRepository.findByProductCodeAndCompanyId(productCode, company.getId())
                .orElseThrow(() -> new BusinessException("Product not found", HttpStatus.NOT_FOUND));

        // WarehouseProduct-a miqdar əlavə et
        warehouseProductService.createWarehouseProduct(product, warehouseId, quantity);

        // Product-un ümumi stock-unu yenilə
        long currentStock = product.getStock() != null ? product.getStock() : 0L;
        product.setStock(currentStock + quantity);
        productRepository.save(product);

        // Anbar hərəkəti loqu — Manual əlavə
        Warehouse warehouse = warehouseService.getWarehouseById(warehouseId);
        WarehouseProduct wp = warehouseProductService.findByProductAndWarehouse(product.getId(), warehouseId);
        WarehouseActivity activity = new WarehouseActivity();
        activity.setType(ActivityTypeWarehouse.INCOMING);
        activity.setProduct(wp);
        activity.setQuantity(quantity);
        activity.setDestinationWarehouse(warehouse);
        activity.setSourceName("Manual Artırılma");
        activity.setDestinationName(warehouse != null ? warehouse.getName() : null);
        activity.setReferenceNumber("MANUAL");
        activity.setPurchasePrice(product.getPurchasePrice() != null ? product.getPurchasePrice() : product.getPrice());
        activity.setSalePrice(product.getSalePrice() != null ? product.getSalePrice() : product.getPrice());
        activity.setDate(LocalDateTime.now());
        activity.setCompany(company);
        warehouseActivityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportProductsToExcel(String userEmail) throws Exception {
        List<ProductInventoryDto> products = getProductsByCompany(userEmail, null);

        try (org.apache.poi.xssf.streaming.SXSSFWorkbook workbook = new org.apache.poi.xssf.streaming.SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Inventar");

            // Başlıq stili
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Başlıq sətri
            String[] columns = {"Məhsul Kodu", "Məhsul Adı", "Anbar", "Kateqoriya", "Vahid", "Mövcud Stok", "Qiymət (ƏDV-siz)", "Status"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Məlumat sətirləri
            int rowIdx = 1;
            for (ProductInventoryDto p : products) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getProductCode() != null ? p.getProductCode() : "");
                row.createCell(1).setCellValue(p.getName() != null ? p.getName() : "");
                row.createCell(2).setCellValue(p.getWarehouse() != null ? p.getWarehouse() : "");
                row.createCell(3).setCellValue(p.getCategory() != null ? p.getCategory() : "");
                row.createCell(4).setCellValue(p.getUnitName() != null ? p.getUnitName() : "");
                row.createCell(5).setCellValue(p.getStock() != null ? p.getStock() : 0);
                row.createCell(6).setCellValue(p.getPrice() != null ? p.getPrice().doubleValue() : 0);
                String statusDesc = "";
                if (p.getProductStatus() != null) {
                    try {
                        statusDesc = ProductStatus.valueOf(p.getProductStatus()).getDescription();
                    } catch (IllegalArgumentException e) {
                        statusDesc = p.getProductStatus();
                    }
                }
                row.createCell(7).setCellValue(statusDesc);
            }

            workbook.write(out);
            workbook.dispose();
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private Product createProductFromRow(Row row, Company company) {
        // Frontend Excel şablonu sütun sırası:
        // 0=Məhsul Kodu, 1=Məhsul Adı, 2=Anbar, 3=Kateqoriya,
        // 4=Vahid, 5=Mövcud Stok, 6=Qiymət, 7=Status
        Product product = new Product();
        product.setCompany(company);

        String productCode = getCellValueAsString(row.getCell(0));
        String name = getCellValueAsString(row.getCell(1));
        // cell(2) = Anbar - warehouseProductService-də istifadə olunur
        String categoryName = getCellValueAsString(row.getCell(3));
        String unitName = getCellValueAsString(row.getCell(4));
        double quantity = getCellValueAsDouble(row.getCell(5));
        double price = getCellValueAsDouble(row.getCell(6));
        String statusStr = getCellValueAsString(row.getCell(7));

        product.setName(name);
        product.setProductCode(productCode);
        product.setPrice(BigDecimal.valueOf(price));

        Category category = categoryService.findByName(categoryName);
        product.setCategory(category);

        product.setProductStatus(ProductStatus.fromString(statusStr));

        // Ölçü Vahidi təyini
        if (unitName != null && !unitName.trim().isEmpty()) {
            Optional<Unit> unitOpt = unitRepository.findByName(unitName.trim());
            if (unitOpt.isPresent()) {
                product.setUnit(unitOpt.get());
            } else {
                unitRepository.findByName("Ədəd").ifPresent(product::setUnit);
            }
        } else {
            unitRepository.findByName("Ədəd").ifPresent(product::setUnit);
        }

        product.setStock((long) Math.max(0, quantity));
        return product;
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

    private double getCellValueAsDouble(Cell cell) {
        if (cell == null) return 0.0;
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                String val = cell.getStringCellValue().trim();
                if (val.isEmpty() || val.equals("-")) return 0.0;
                try {
                    return Double.parseDouble(val);
                } catch (Exception e) {
                    return 0.0;
                }
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    try {
                        String valFormula = cell.getStringCellValue().trim();
                        if (valFormula.isEmpty() || valFormula.equals("-")) return 0.0;
                        return Double.parseDouble(valFormula);
                    } catch (Exception ex) {
                        return 0.0;
                    }
                }
            default:
                return 0.0;
        }
    }

    private ProductReadDto mapToProductReadDto(Product p, WarehouseProduct wp) {
        return ProductReadDto.builder()
                .id(wp.getId())
                .warehouseProductId(wp.getId())
                .productId(p.getId())
                .name(p.getName())
                .productCode(p.getProductCode())
                .price(p.getPrice())
                .purchasePrice(p.getPurchasePrice())
                .salePrice(p.getSalePrice())
                .marginPercentage(p.getMarginPercentage())
                .discount(p.getDiscount())
                .totalStock(p.getProductType() == ProductType.GOODS ? null : p.getStock())
                .quantity(p.getProductType() == ProductType.GOODS ? null : wp.getQuantity())
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .unitName(p.getUnit() != null ? p.getUnit().getName() : null)
                .warehouseName(wp.getWarehouse() != null ? wp.getWarehouse().getName() : null)
                .productStatus(p.getProductStatus() != null ? p.getProductStatus().name() : null)
                .vatStatus(p.getVatStatus() != null ? p.getVatStatus().name() : "WITHOUT_VAT")
                .vatRate(p.getVatRate())
                .priceWithVat(p.getPriceWithVat())
                .productType(p.getProductType() != null ? p.getProductType().name() : "GOODS")
                .minStockLimit(p.getMinStockLimit())
                .build();
    }

    private ProductInventoryDto mapToProductInventoryDto(WarehouseProduct wp) {
        Product p = wp.getProduct();
        return ProductInventoryDto.builder()
                .id(wp.getId())
                .productId(p.getId())
                .productCode(p.getProductCode())
                .name(p.getName())
                .price(p.getPrice())
                .purchasePrice(p.getPurchasePrice())
                .salePrice(p.getSalePrice())
                .marginPercentage(p.getMarginPercentage())
                .discount(p.getDiscount())
                .stock(p.getProductType() == ProductType.GOODS ? null : wp.getQuantity())
                .category(p.getCategory() != null ? p.getCategory().getName() : null)
                .unitName(p.getUnit() != null ? p.getUnit().getName() : null)
                .warehouse(wp.getWarehouse() != null ? wp.getWarehouse().getName() : null)
                .productStatus(p.getProductStatus() != null ? p.getProductStatus().name() : null)
                .vatStatus(p.getVatStatus() != null ? p.getVatStatus().name() : "WITHOUT_VAT")
                .vatRate(p.getVatRate())
                .priceWithVat(p.getPriceWithVat())
                .productType(p.getProductType() != null ? p.getProductType().name() : "GOODS")
                .minStockLimit(p.getMinStockLimit())
                .build();
    }

    private String generateUniqueBarcode(Long companyId) {
        String barcode;
        do {
            barcode = az.techvibeds.specialservice.utils.BarcodeGenerator.generateEAN13(companyId);
        } while (productRepository.findByProductCodeAndCompanyId(barcode, companyId).isPresent());
        return barcode;
    }

    private String cleanProductName(String name) {
        if (name == null) return null;
        return name.replaceAll("(?i)\\bstock\\b", "")
                   .replaceAll("(?i)\\bstok\\b", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    @Override
    @Transactional
    public ProductReadDto quickScanToInventory(String barcode, Long warehouseId, Long categoryId, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);

        // Skanerlərdən gələ biləcək artıq simvolları təmizlə
        String cleanedBarcode = barcode.trim().replaceAll("[\\r\\n]", "");
        if (cleanedBarcode.isEmpty()) {
            throw new BusinessException("Barkod boş ola bilməz!");
        }

        // 1. Əvvəlcə bizim bazada axtarırıq
        Optional<Product> existingProduct = productRepository.findByProductCodeAndCompanyId(cleanedBarcode, company.getId());

        if (existingProduct.isPresent()) {
            // Məhsul artıq var → stokunu 1 artır
            Product product = existingProduct.get();
            warehouseProductService.createWarehouseProduct(product, warehouseId, 1);

            long currentStock = product.getStock() != null ? product.getStock() : 0L;
            product.setStock(currentStock + 1);
            product.setProductStatus(ProductStatus.IN_STOCK);
            productRepository.save(product);

            log.info("Mövcud məhsulun stoku artırıldı: {} ({}), yeni stok: {}", product.getName(), cleanedBarcode, product.getStock());

            WarehouseProduct wp = warehouseProductService.findByProductAndWarehouse(product.getId(), warehouseId);
            return mapToProductReadDto(product, wp);
        }

        // 2. Bazamızda yoxdur — xarici barkod bazasından adını axtarırıq
        String productName = externalBarcodeService.lookupProductName(cleanedBarcode);
        if (productName == null || productName.isBlank()) {
            productName = "Yeni Məhsul (" + cleanedBarcode + ")";
        }

        // 3. Default ölçü vahidi tapırıq (ədəd)
        Unit defaultUnit = unitRepository.findAll().stream()
                .filter(u -> u.getName() != null && (u.getName().equalsIgnoreCase("ədəd") || u.getName().equalsIgnoreCase("Eded") || u.getName().equalsIgnoreCase("pcs")))
                .findFirst()
                .orElse(unitRepository.findAll().stream().findFirst().orElse(null));

        if (defaultUnit == null) {
            throw new BusinessException("Sistemdə heç bir ölçü vahidi tapılmadı. Əvvəlcə bir vahid yaradın.");
        }

        // 4. Yeni məhsul yaradırıq
        Product newProduct = new Product();
        newProduct.setCompany(company);
        newProduct.setName(productName);
        newProduct.setProductCode(cleanedBarcode);
        newProduct.setUnit(defaultUnit);
        newProduct.setProductType(ProductType.PRODUCT);
        newProduct.setProductStatus(ProductStatus.IN_STOCK);
        newProduct.setVatStatus(VatStatus.WITHOUT_VAT);
        newProduct.setStock(1L);
        newProduct.setMinStockLimit(0L);

        if (categoryId != null) {
            newProduct.setCategory(categoryService.findById(categoryId));
        }

        productRepository.save(newProduct);

        // 5. Anbara əlavə edirik (1 ədəd)
        WarehouseProduct wp = warehouseProductService.createWarehouseProduct(newProduct, warehouseId, 1);

        log.info("Yeni məhsul yaradıldı və anbara əlavə edildi: {} ({})", productName, cleanedBarcode);

        return mapToProductReadDto(newProduct, wp);
    }
}