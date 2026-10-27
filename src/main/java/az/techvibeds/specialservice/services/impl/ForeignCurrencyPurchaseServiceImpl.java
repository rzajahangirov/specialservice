package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.currency.CurrencyDto;
import az.techvibeds.specialservice.dtos.foreignCurrency.ForeignCurrencyPurchaseCreateDto;
import az.techvibeds.specialservice.dtos.foreignCurrency.ForeignCurrencyPurchaseReadDto;
import az.techvibeds.specialservice.models.Currency;
import az.techvibeds.specialservice.models.ForeignCurrencyPurchase;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.repositories.CurrencyRepository;
import az.techvibeds.specialservice.repositories.ForeignCurrencyPurchaseRepository;
import az.techvibeds.specialservice.repositories.UserRepository;
import az.techvibeds.specialservice.services.ForeignCurrencyPurchaseService;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ForeignCurrencyPurchaseServiceImpl implements ForeignCurrencyPurchaseService {

    private final ForeignCurrencyPurchaseRepository purchaseRepository;
    private final CurrencyRepository currencyRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ForeignCurrencyPurchaseReadDto createPurchase(ForeignCurrencyPurchaseCreateDto dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Currency currency = currencyRepository.findByCode(dto.getCurrencyCode())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "code", dto.getCurrencyCode()));

        ForeignCurrencyPurchase purchase = new ForeignCurrencyPurchase();
        purchase.setSupplierName(dto.getSupplierName());
        purchase.setAmount(dto.getAmount());
        purchase.setCurrency(currency);
        purchase.setExchangeRate(dto.getExchangeRate());
        purchase.setPurchaseDate(dto.getPurchaseDate());
        purchase.setExpenseCategory(dto.getExpenseCategory());
        purchase.setNotes(dto.getNotes());
        purchase.setCompany(user.getCompany());

        // Hesablamalar:
        // 1. Yerli məbləğ (AZN) = Alış məbləği * Alış məzənnəsi
        BigDecimal convertedAmount = dto.getAmount().multiply(dto.getExchangeRate())
                .setScale(2, RoundingMode.HALF_UP);
        purchase.setConvertedAmount(convertedAmount);

        // 2. Məzənnə fərqi = (Cari məzənnə - Alış məzənnəsi) * Alış məbləği
        BigDecimal currentRate = currency.getCurrentRate() != null ? currency.getCurrentRate() : dto.getExchangeRate();
        BigDecimal rateDiff = currentRate.subtract(dto.getExchangeRate());
        BigDecimal totalDiff = rateDiff.multiply(dto.getAmount())
                .setScale(2, RoundingMode.HALF_UP);
        purchase.setExchangeRateDifference(totalDiff);

        return convertToReadDto(purchaseRepository.save(purchase));
    }

    @Override
    public Page<ForeignCurrencyPurchaseReadDto> getPurchases(Pageable pageable, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        return purchaseRepository.findAllByCompanyId(user.getCompany().getId(), pageable)
                .map(this::convertToReadDto);
    }

    @Override
    public ForeignCurrencyPurchaseReadDto getPurchaseById(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        ForeignCurrencyPurchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "id", id));

        if (!purchase.getCompany().getId().equals(user.getCompany().getId())) {
            throw new AccessDeniedException("Bu valyuta alışı sizin şirkətə aid deyil");
        }

        return convertToReadDto(purchase);
    }

    @Override
    @Transactional
    public void deletePurchase(Long id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        ForeignCurrencyPurchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "id", id));

        if (!purchase.getCompany().getId().equals(user.getCompany().getId())) {
            throw new AccessDeniedException("Bu valyuta alışı sizin şirkətə aid deyil");
        }

        purchaseRepository.delete(purchase);
    }

    private ForeignCurrencyPurchaseReadDto convertToReadDto(ForeignCurrencyPurchase entity) {
        ForeignCurrencyPurchaseReadDto dto = new ForeignCurrencyPurchaseReadDto();
        dto.setId(entity.getId());
        dto.setSupplierName(entity.getSupplierName());
        dto.setAmount(entity.getAmount());
        dto.setExchangeRate(entity.getExchangeRate());
        dto.setConvertedAmount(entity.getConvertedAmount());
        dto.setExchangeRateDifference(entity.getExchangeRateDifference());
        dto.setPurchaseDate(entity.getPurchaseDate());
        dto.setExpenseCategory(entity.getExpenseCategory());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());

        CurrencyDto currencyDto = new CurrencyDto();
        currencyDto.setId(entity.getCurrency().getId());
        currencyDto.setCode(entity.getCurrency().getCode());
        currencyDto.setName(entity.getCurrency().getName());
        currencyDto.setSymbol(entity.getCurrency().getSymbol());
        currencyDto.setCurrentRate(entity.getCurrency().getCurrentRate());
        dto.setCurrency(currencyDto);

        return dto;
    }
}
