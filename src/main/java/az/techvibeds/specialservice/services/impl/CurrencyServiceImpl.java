package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.currency.CurrencyDto;
import az.techvibeds.specialservice.dtos.currency.CurrencyRateHistoryDto;
import az.techvibeds.specialservice.models.Currency;
import az.techvibeds.specialservice.models.CurrencyRateHistory;
import az.techvibeds.specialservice.repositories.CurrencyRateHistoryRepository;
import az.techvibeds.specialservice.repositories.CurrencyRepository;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.services.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyRateHistoryRepository historyRepository;

    @Override
    public List<CurrencyDto> getAllCurrencies() {
        return currencyRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public CurrencyDto getCurrencyByCode(String code) {
        return currencyRepository.findByCode(code)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "code", code));
    }

    @Override
    @Transactional
    public CurrencyDto createOrUpdateCurrency(CurrencyDto dto) {
        Currency currency = currencyRepository.findByCode(dto.getCode())
                .orElse(new Currency());
        
        currency.setCode(dto.getCode());
        currency.setName(dto.getName());
        currency.setSymbol(dto.getSymbol());
        
        // Əgər məzənnə dəyişibsə, tarixçəyə yazırıq
        if (currency.getCurrentRate() == null || !currency.getCurrentRate().equals(dto.getCurrentRate())) {
            currency.setCurrentRate(dto.getCurrentRate());
            Currency savedCurrency = currencyRepository.save(currency);
            
            CurrencyRateHistory history = new CurrencyRateHistory();
            history.setCurrency(savedCurrency);
            history.setRate(dto.getCurrentRate());
            historyRepository.save(history);
            
            return convertToDto(savedCurrency);
        }
        
        return convertToDto(currencyRepository.save(currency));
    }

    @Override
    public List<CurrencyRateHistoryDto> getRateHistory(String currencyCode) {
        return historyRepository.findAllByCurrencyCodeOrderByRateDateDesc(currencyCode)
                .stream()
                .map(h -> {
                    CurrencyRateHistoryDto dto = new CurrencyRateHistoryDto();
                    dto.setRate(h.getRate());
                    dto.setRateDate(h.getRateDate());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private CurrencyDto convertToDto(Currency entity) {
        CurrencyDto dto = new CurrencyDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setSymbol(entity.getSymbol());
        dto.setCurrentRate(entity.getCurrentRate());
        return dto;
    }
}
