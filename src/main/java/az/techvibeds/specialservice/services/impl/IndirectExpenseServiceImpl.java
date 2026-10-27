package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.indirectExpense.IndirectExpenseCreateDto;
import az.techvibeds.specialservice.dtos.indirectExpense.IndirectExpenseReadDto;
import az.techvibeds.specialservice.dtos.indirectExpense.IndirectExpenseUpdateDto;
import az.techvibeds.specialservice.enums.ExpenseCategory;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.IndirectExpense;
import az.techvibeds.specialservice.repositories.IndirectExpenseRepository;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.IndirectExpenseService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndirectExpenseServiceImpl implements IndirectExpenseService {
    private final IndirectExpenseRepository expenseRepository;
    private final CompanyService companyService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public IndirectExpenseReadDto create(IndirectExpenseCreateDto dto, String email) {
        log.info("Creating indirect expense: {} for user: {}", dto.getName(), email);
        Company company = companyService.findByUserEmail(email);

        IndirectExpense expense = modelMapper.map(dto, IndirectExpense.class);
        expense.setCompany(company);

        return mapToRead(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public IndirectExpenseReadDto update(Long id, IndirectExpenseUpdateDto dto, String email) {
        IndirectExpense expense = findAndValidateOwner(id, email);

        // ModelMapper ilə sahələri yeniləyirik
        modelMapper.map(dto, expense);

        log.info("Indirect expense ID: {} updated", id);
        return mapToRead(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public void delete(Long id, String email) {
        IndirectExpense expense = findAndValidateOwner(id, email);
        expenseRepository.delete(expense);
        log.info("Indirect expense ID: {} deleted", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndirectExpenseReadDto> getExpenses(String keyword, ExpenseCategory category, String email) {
        log.info("Fetching expenses with filter - keyword: {}, category: {}", keyword, category);

        Long companyId = companyService.findByUserEmail(email).getId();
        List<IndirectExpense> expenses;

        if (keyword == null || keyword.trim().isEmpty()) {
            expenses = expenseRepository.findByCompanyAndCategory(companyId, category);
        } else {
            expenses = expenseRepository.findByCompanyAndCategoryAndKeyword(companyId, category, keyword);
        }

        return expenses.stream()
                .map(this::mapToRead)
                .collect(Collectors.toList());
    }

    private IndirectExpense findAndValidateOwner(Long id, String email) {
        IndirectExpense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with ID: " + id));

        Long userCompanyId = companyService.findByUserEmail(email).getId();
        if (!expense.getCompany().getId().equals(userCompanyId)) {
            throw new AccessDeniedException("Access denied to this expense record");
        }
        return expense;
    }

    private IndirectExpenseReadDto mapToRead(IndirectExpense expense) {
        IndirectExpenseReadDto dto = modelMapper.map(expense, IndirectExpenseReadDto.class);
        dto.setCurrency(expense.getCurrency().name());
        dto.setCategory(expense.getCategory().name());
        return dto;
    }
}
