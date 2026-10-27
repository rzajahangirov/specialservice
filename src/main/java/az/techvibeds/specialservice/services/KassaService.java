package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.finance.TransactionReadDto;
import az.techvibeds.specialservice.dtos.kassa.KassaIncomeDto;
import az.techvibeds.specialservice.dtos.kassa.KassaExpenseDto;
import az.techvibeds.specialservice.dtos.kassa.KassaTransferDto;

import java.util.List;

public interface KassaService {
    TransactionReadDto processIncome(KassaIncomeDto incomeDto, String userEmail);
    TransactionReadDto processExpense(KassaExpenseDto expenseDto, String userEmail);
    List<TransactionReadDto> getKassaHistory(String userEmail, Long accountId);
    TransactionReadDto processTransfer(KassaTransferDto transferDto, String userEmail);
    TransactionReadDto approveTransaction(Long id, String userEmail);
    TransactionReadDto rejectTransaction(Long id, String userEmail);
}
