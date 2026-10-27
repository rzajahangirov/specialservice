package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.partner.*;
import az.techvibeds.specialservice.enums.PartnerType;
import az.techvibeds.specialservice.models.Balance;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.Partner;
import az.techvibeds.specialservice.repositories.PartnerRepository;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.PartnerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PartnerServiceImpl implements PartnerService {

    private final PartnerRepository partnerRepository;
    private final CompanyService companyService;
    private final ModelMapper modelMapper;



    @Override
    @Transactional
    public PartnerResponseDto createCustomer(PartnerCreateDto dto, String userEmail) {
        return createPartner(dto, PartnerType.CUSTOMER, userEmail);
    }

    @Override
    @Transactional
    public PartnerResponseDto createSupplier(PartnerCreateDto dto, String userEmail) {
        return createPartner(dto, PartnerType.SUPPLIER, userEmail);
    }

    private PartnerResponseDto createPartner(PartnerCreateDto dto, PartnerType type, String userEmail) {
        Company company = companyService.findByUserEmail(userEmail);

        Partner partner = modelMapper.map(dto, Partner.class);
        partner.setPartnerType(type);
        partner.setCompany(company);


        Balance balance = new Balance();
        balance.setAmount(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO);
        balance.setCurrencyType(dto.getCurrency());
        balance.setPartner(partner);

        partner.setBalance(balance);


        Partner savedPartner = partnerRepository.save(partner);
        return mapToReadDto(savedPartner);
    }

//    @Override
//    public List<PartnerReadDto> getAllPartnersByCompany(String userEmail) {
//        Company company = companyService.findByUserEmail(userEmail);
//        List<Partner> partners = partnerRepository.findAllByCompanyId(company.getId());
//        return mapToDtoList(partners);
//    }

    @Transactional
    @Override
    public void importFromExcel(MultipartFile file, String userEmail) throws Exception {
        Company company = companyService.findByUserEmail(userEmail);

        Map<String, String> currencyMap = Map.of(
                "₼", "manat",
                "$", "USD",
                "€", "EUR"
        );

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Partner> partnersToSave = new ArrayList<>();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                String name = getCellValueAsString(row.getCell(0));
                String contactPerson = getCellValueAsString(row.getCell(1));
                String email = getCellValueAsString(row.getCell(2));
                String phone = getCellValueAsString(row.getCell(3));
                String typeStr = getCellValueAsString(row.getCell(6));
                String isActiveStr = getCellValueAsString(row.getCell(7));
                String voen = getCellValueAsString(row.getCell(8));
                if (voen.isBlank()) voen = null;
                
                String contractNumber = getCellValueAsString(row.getCell(9));
                if (contractNumber.isBlank()) contractNumber = null;

                validateRow(row.getRowNum() + 1, name, email, typeStr);

                Partner partner = new Partner();
                partner.setName(name);
                partner.setContactPerson(contactPerson);
                partner.setEmail(email);
                partner.setPhone(phone);
                partner.setVoen(voen);
                partner.setContractNumber(contractNumber);
                partner.setPartnerType(parsePartnerTypeFromRow(typeStr));
                partner.setCompany(company);


                partner.setIsActive(parseIsActiveFromRow(isActiveStr));

                Balance balance = parseBalanceFromRow(row, partner, currencyMap);
                partner.setBalance(balance);

                partnersToSave.add(partner);
            }

            partnerRepository.saveAll(partnersToSave);
        }
    }
    private Boolean parseIsActiveFromRow(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }

        value = value.trim().toLowerCase();

        return switch (value) {
            case "true", "1", "yes", "active", "aktiv" -> true;
            case "false", "0", "no", "passive", "inactive", "deaktiv" -> false;
            default -> true;
        };
    }


    private void validateRow(int rowNum, String name, String email, String type) throws Exception {
        if (name.isBlank()) {
            throw new Exception("Sətir " + rowNum + ": Partnyor adı boş ola bilməz!");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new Exception("Sətir " + rowNum + ": Email formatı düzgün deyil (" + email + ")");
        }
        if (type.isBlank()) {
            throw new Exception("Sətir " + rowNum + ": Partner tipi (CUSTOMER/SUPPLIER) mütləqdir!");
        }
    }


    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    private Balance parseBalanceFromRow(Row row, Partner partner, Map<String, String> currencyMap) throws Exception {
        Cell balanceCell = row.getCell(4);
        BigDecimal amount = BigDecimal.ZERO;

        if (balanceCell != null && balanceCell.getCellType() == CellType.NUMERIC) {
            amount = BigDecimal.valueOf(balanceCell.getNumericCellValue());
        } else if (balanceCell != null && balanceCell.getCellType() != CellType.BLANK) {
            throw new Exception("Sətir " + (row.getRowNum() + 1) + ": Balans rəqəm olmalıdır!");
        }

        Cell currencyCell = row.getCell(5);
        String currencySymbol = getCellValueAsString(currencyCell);
        String currencyType = currencyMap.getOrDefault(currencySymbol, "manat");

        Balance balance = new Balance();
        balance.setAmount(amount);
        balance.setCurrencyType(currencyType);
        balance.setPartner(partner);

        return balance;
    }

    private PartnerType parsePartnerTypeFromRow(String type) throws Exception {
        if ("CUSTOMER".equalsIgnoreCase(type)) {
            return PartnerType.CUSTOMER;
        } else if ("SUPPLIER".equalsIgnoreCase(type)) {
            return PartnerType.SUPPLIER;
        } else {
            throw new Exception("Unknown partner type: " + type);
        }
    }



    @Override
    @Transactional
    public PartnerResponseDto updatePartner(PartnerUpdateDto dto, String userEmail){
        Partner partner = partnerRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Partner not found"));
        Company company = companyService.findByUserEmail(userEmail);

        if (!partner.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Access denied: Company mismatch");
        }
        if (dto.getName() != null) partner.setName(dto.getName());
        if (dto.getContactPerson() != null) partner.setContactPerson(dto.getContactPerson());
        if (dto.getEmail() != null) partner.setEmail(dto.getEmail());
        if (dto.getPhone() != null) partner.setPhone(dto.getPhone());
        if (dto.getVoen() != null) partner.setVoen(dto.getVoen().isBlank() ? null : dto.getVoen());
        if (dto.getContractNumber() != null) partner.setContractNumber(dto.getContractNumber().isBlank() ? null : dto.getContractNumber());

        if (dto.getBalance() != null || dto.getCurrency() != null) {
            Balance balance = new Balance();
            if (dto.getBalance() != null) balance.setAmount(dto.getBalance());
            if (dto.getCurrency() != null) balance.setCurrencyType(dto.getCurrency());
            partner.setBalance(balance);
        }

        if (dto.getPartnerType() != null) {
            partner.setPartnerType(dto.getPartnerType());
        }

        partnerRepository.save(partner);

        return mapToReadDto(partner);

    }


    @Override
    public void delete(Long id, String userEmail) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partner not found"));
        Company company = companyService.findByUserEmail(userEmail);
        if (company.getId().equals(partner.getCompany().getId())) {
            partnerRepository.delete(partner);
        }else {
            throw new RuntimeException("Company does not match");
        }
    }

    @Override
    public ByteArrayInputStream exportToExcel(String userEmail) throws Exception {
        Company company = companyService.findByUserEmail(userEmail);
        List<Partner> partners;
        if (company != null) {
            partners = partnerRepository.findAllByCompany(company);
        } else {
            partners = partnerRepository.findAll(); // Super Admin sees all
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Partners");

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Name",
                    "Contact Person",
                    "Email",
                    "Phone",
                    "Balance",
                    "Currency",
                    "Partner Type",
                    "Is Active",
                    "VOEN",
                    "Contract Number"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (Partner partner : partners) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(partner.getName());
                row.createCell(1).setCellValue(partner.getContactPerson());
                row.createCell(2).setCellValue(partner.getEmail());
                row.createCell(3).setCellValue(partner.getPhone());

                // Balance məlumatı varsa
                Balance balance = partner.getBalance();
                if (balance != null) {
                    row.createCell(4).setCellValue(balance.getAmount().doubleValue());
                    row.createCell(5).setCellValue(balance.getCurrencyType());
                } else {
                    row.createCell(4).setCellValue(0);
                    row.createCell(5).setCellValue("UNKNOWN");
                }

                row.createCell(6).setCellValue(partner.getPartnerType().toString());

                Boolean isActive = partner.getIsActive();
                row.createCell(7).setCellValue(isActive != null && isActive);

                row.createCell(8).setCellValue(partner.getVoen() != null ? partner.getVoen() : "");
                row.createCell(9).setCellValue(partner.getContractNumber() != null ? partner.getContractNumber() : "");
            }

            // Autosize
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Override
    public PartnerTypeDto getPartnerTypes() {
        PartnerTypeDto partnerTypeDto = new PartnerTypeDto();
        List<PartnerType> partnerTypes = List.of(PartnerType.values());
        partnerTypeDto.setPartnerTypes(partnerTypes);
        return partnerTypeDto;
    }

    @Override
    public List<PartnerReadDto> findByPartnerTypeAndCompany_Id(PartnerType type, String email){
        Company company = companyService.findByUserEmail(email);
        List<Partner> partners;
        if (company != null) {
            partners = partnerRepository.findAllByCompanyIdAndPartnerType(company.getId(), type);
        } else {
            partners = partnerRepository.findAllByPartnerType(type); // Super Admin sees all
        }
        return mapToDtoList(partners);
    }

    @Override
    @Transactional
    public void toggleActive(Long partnerId, String userEmail) throws Exception {

        Company company = companyService.findByUserEmail(userEmail);

        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new Exception("Partner can not find"));

        if (!partner.getCompany().getId().equals(company.getId())) {
            throw new Exception("You can not change this partner");
        }

        Boolean current = partner.getIsActive();

        partner.setIsActive(current == null || !current);

        partnerRepository.save(partner);
    }

    @Override
    public List<PartnerReadDto> getPartnersByActiveStatus(Boolean isActive, String userEmail) {

        Company company = companyService.findByUserEmail(userEmail);

        List<Partner> partners;
        if (company == null) {
            // Super Admin sees all partners
            if (isActive == null) {
                partners = partnerRepository.findAll();
            } else {
                partners = partnerRepository.findAll().stream()
                        .filter(p -> p.getIsActive() != null && p.getIsActive().equals(isActive))
                        .toList();
            }
        } else {
            if (isActive == null) {
                partners = partnerRepository.findAllByCompany(company);
            } else {
                partners = partnerRepository.findAllByCompanyAndIsActive(company, isActive);
            }
        }

        return mapToDtoList(partners);
    }

    private PartnerResponseDto mapToReadDto(Partner partner) {
        PartnerResponseDto readDto = new PartnerResponseDto();
        readDto.setId(partner.getId());
        readDto.setName(partner.getName());
        readDto.setContactPerson(partner.getContactPerson());
        readDto.setEmail(partner.getEmail());
        readDto.setPhone(partner.getPhone());
        readDto.setVoen(partner.getVoen());
        readDto.setContractNumber(partner.getContractNumber());
        if (partner.getBalance() != null) {
            readDto.setBalance(partner.getBalance().getAmount());
            readDto.setCurrency(partner.getBalance().getCurrencyType());
        } else {
            readDto.setBalance(BigDecimal.ZERO);
            readDto.setCurrency("manat");
        }
        readDto.setPartnerType(partner.getPartnerType());
        return readDto;
    }
    private List<PartnerReadDto> mapToDtoList(List<Partner> partners) {
        List<PartnerReadDto> partnerDtos = new ArrayList<>();

        for (Partner partner : partners) {
            PartnerReadDto dto = new PartnerReadDto();
            dto.setId(partner.getId());
            dto.setName(partner.getName());
            dto.setContactPerson(partner.getContactPerson());
            dto.setEmail(partner.getEmail());
            dto.setPhone(partner.getPhone());
            dto.setVoen(partner.getVoen());
            dto.setContractNumber(partner.getContractNumber());
            if (partner.getBalance() != null) {
                dto.setBalance(partner.getBalance().getAmount());
                dto.setCurrency(partner.getBalance().getCurrencyType());
            } else {
                dto.setBalance(BigDecimal.ZERO);
                dto.setCurrency("manat");
            }
            dto.setIsActive(partner.getIsActive());

            if (partner.getPartnerType() != null) {
                String partnerType = partner.getPartnerType().toString();
                dto.setPartnerType(partnerType.substring(0, 1).toUpperCase() + partnerType.substring(1).toLowerCase());
            } else {
                dto.setPartnerType("Unknown");
            }

            partnerDtos.add(dto);
        }
        return partnerDtos;
    }
}
