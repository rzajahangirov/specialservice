package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.balace.CreateBalanceDto;
import az.techvibeds.specialservice.models.Balance;

public interface BalanceService {
    Balance createBalance(CreateBalanceDto balanceDto);

}
