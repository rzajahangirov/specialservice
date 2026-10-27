package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.semiProduct.SemiProductCreateDto;
import az.techvibeds.specialservice.dtos.semiProduct.SemiProductReadDto;
import az.techvibeds.specialservice.dtos.semiProduct.SemiProductUpdateDto;
import az.techvibeds.specialservice.enums.SemiProductStatus;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface SemiProductService {

    SemiProductReadDto create(SemiProductCreateDto dto, String userEmail);

    SemiProductReadDto update(Long id, SemiProductUpdateDto dto, String userEmail);

    void delete(Long id, String userEmail);

    List<SemiProductReadDto> getSemiProducts(String keyword, SemiProductStatus status, String name);

    ByteArrayInputStream exportToExcel(String userEmail) throws Exception;

    void importFromExcel(org.springframework.web.multipart.MultipartFile file, String userEmail) throws Exception;
}
