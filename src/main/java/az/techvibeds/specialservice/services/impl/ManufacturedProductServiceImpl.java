package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.manufacturedProduct.ManufacturedProductCreateDto;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.ManufacturedProduct;
import az.techvibeds.specialservice.repositories.ManufacturedProductRepository;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.ManufacturedProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManufacturedProductServiceImpl implements ManufacturedProductService {

    private final ManufacturedProductRepository manufacturedProductRepository;
    private final ModelMapper modelMapper;
    private final CompanyService companyService;

    @Override
    @Transactional
    public ManufacturedProduct createProduct(ManufacturedProductCreateDto dto, String userEmail) {
        log.info("Creating manufactured product: {} for user: {}", dto.getName(), userEmail);

        Company company = companyService.findByUserEmail(userEmail);

        ManufacturedProduct manufacturedProduct = modelMapper.map(dto, ManufacturedProduct.class);
        manufacturedProduct.setCompany(company);

        ManufacturedProduct savedProduct = manufacturedProductRepository.save(manufacturedProduct);

        log.info("Manufactured product created successfully with ID: {}", savedProduct.getId());
        return savedProduct;
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportToExcel(String userEmail) throws Exception {
        Company company = companyService.findByUserEmail(userEmail);
        List<ManufacturedProduct> products = manufacturedProductRepository.findAllByCompany_Id(company.getId());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Məhsullar");

            // Başlıq stili
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Başlıq sətri
            String[] columns = {"Məhsul Adı", "Təsvir"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Məlumat sətirləri
            int rowIdx = 1;
            for (ManufacturedProduct product : products) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(product.getName() != null ? product.getName() : "");
                row.createCell(1).setCellValue(product.getDescription() != null ? product.getDescription() : "");
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
                if (row.getRowNum() == 0) continue; // Başlıq sətirini keçirik

                try {
                    String name = getCellValueAsString(row.getCell(0));
                    String description = getCellValueAsString(row.getCell(1));

                    if (name == null || name.trim().isEmpty()) {
                        continue; // Boş sətirləri keçirik
                    }

                    Optional<ManufacturedProduct> existingProduct =
                            manufacturedProductRepository.findByNameAndCompany_Id(name.trim(), company.getId());

                    if (existingProduct.isPresent()) {
                        ManufacturedProduct product = existingProduct.get();
                        product.setDescription(description);
                        manufacturedProductRepository.save(product);
                        log.info("Updated manufactured product: {}", name);
                    } else {
                        ManufacturedProduct newProduct = new ManufacturedProduct();
                        newProduct.setName(name.trim());
                        newProduct.setDescription(description);
                        newProduct.setCompany(company);
                        manufacturedProductRepository.save(newProduct);
                        log.info("Created new manufactured product: {}", name);
                    }
                } catch (Exception e) {
                    log.error("Error processing row {}: {}", row.getRowNum(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Excel import error: {}", e.getMessage());
            throw new BusinessException("Error processing Excel file: " + e.getMessage());
        }
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