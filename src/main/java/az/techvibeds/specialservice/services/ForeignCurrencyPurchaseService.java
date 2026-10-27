package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.foreignCurrency.ForeignCurrencyPurchaseCreateDto;
import az.techvibeds.specialservice.dtos.foreignCurrency.ForeignCurrencyPurchaseReadDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ForeignCurrencyPurchaseService {
    ForeignCurrencyPurchaseReadDto createPurchase(ForeignCurrencyPurchaseCreateDto dto, String userEmail);
    Page<ForeignCurrencyPurchaseReadDto> getPurchases(Pageable pageable, String userEmail);
    ForeignCurrencyPurchaseReadDto getPurchaseById(Long id, String userEmail);
    void deletePurchase(Long id, String userEmail);
}
