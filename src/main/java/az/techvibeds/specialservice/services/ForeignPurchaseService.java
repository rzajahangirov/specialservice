package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.foreignPurchase.ForeignPurchaseCreateDto;
import az.techvibeds.specialservice.dtos.foreignPurchase.ForeignPurchaseReadDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ForeignPurchaseService {
    ForeignPurchaseReadDto createPurchase(ForeignPurchaseCreateDto dto, String userEmail);
    Page<ForeignPurchaseReadDto> getPurchases(Pageable pageable, String userEmail);
    ForeignPurchaseReadDto getPurchaseById(Long id, String userEmail);
    void deletePurchase(Long id, String userEmail);
    ForeignPurchaseReadDto importItemsFromExcel(Long purchaseId, org.springframework.web.multipart.MultipartFile file, String userEmail) throws Exception;
}
