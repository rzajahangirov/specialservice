package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.balace.CreateBalanceDto;
import az.techvibeds.specialservice.models.Balance;
import az.techvibeds.specialservice.repositories.BalanceRepository;
import az.techvibeds.specialservice.services.BalanceService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BalanceServiceImpl implements BalanceService {
    private final ModelMapper modelMapper;
    private final BalanceRepository balanceRepository;

    @Override
    public Balance createBalance(CreateBalanceDto balanceDto) {
        try{
            Balance balance = modelMapper.map(balanceDto, Balance.class);
            balanceRepository.save(balance);
            return balance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
