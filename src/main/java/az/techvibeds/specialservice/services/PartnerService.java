package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.partner.*;
import az.techvibeds.specialservice.enums.PartnerType;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface PartnerService {
    PartnerResponseDto createCustomer(PartnerCreateDto partnerCreateDto, String name);

    PartnerResponseDto createSupplier(PartnerCreateDto partnerCreateDto, String name);

//    List<PartnerReadDto> getAllPartnersByCompany(String name);

    void importFromExcel(MultipartFile file, String name) throws Exception;

    PartnerResponseDto updatePartner(PartnerUpdateDto partnerUpdateDto, String userEmail) throws Exception;

    void delete(Long id, String userEmail);

    ByteArrayInputStream exportToExcel(String name) throws Exception;

    PartnerTypeDto getPartnerTypes();

    List<PartnerReadDto> findByPartnerTypeAndCompany_Id(PartnerType partnerType, String email) throws Exception;

    void toggleActive(Long partnerId, String userEmail) throws Exception;

    List<PartnerReadDto> getPartnersByActiveStatus(Boolean isActive, String userEmail) throws Exception;
}
