package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.indirectExpense.IndirectExpenseCreateDto;
import az.techvibeds.specialservice.dtos.indirectExpense.IndirectExpenseReadDto;
import az.techvibeds.specialservice.dtos.indirectExpense.IndirectExpenseUpdateDto;
import az.techvibeds.specialservice.enums.ExpenseCategory;

import java.util.List;

public interface IndirectExpenseService {

    IndirectExpenseReadDto create(IndirectExpenseCreateDto dto, String userEmail);

    IndirectExpenseReadDto update(Long id, IndirectExpenseUpdateDto dto, String userEmail);

    void delete(Long id, String userEmail);

    List<IndirectExpenseReadDto> getExpenses(String keyword, ExpenseCategory category, String name);
}
