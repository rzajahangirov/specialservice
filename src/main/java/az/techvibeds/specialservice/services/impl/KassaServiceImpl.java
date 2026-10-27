package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.finance.TransactionReadDto;
import az.techvibeds.specialservice.dtos.kassa.KassaIncomeDto;
import az.techvibeds.specialservice.dtos.kassa.KassaExpenseDto;
import az.techvibeds.specialservice.dtos.kassa.KassaTransferDto;
import az.techvibeds.specialservice.enums.TransactionType;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.repositories.*;
import az.techvibeds.specialservice.services.KassaService;
import az.techvibeds.specialservice.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KassaServiceImpl implements KassaService {

    private final FinancialTransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public TransactionReadDto processIncome(KassaIncomeDto dto, String userEmail) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Məbləğ 0-dan böyük olmalıdır!");
        }
        User user = findUserByEmail(userEmail);
        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new RuntimeException("Hesab tapılmadı"));
        
        TransactionCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Kateqoriya tapılmadı"));

        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setAmount(dto.getAmount());
        transaction.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : java.time.LocalDateTime.now());
        transaction.setType(TransactionType.INCOME);
        transaction.setDescription(dto.getDescription());
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setCompany(user.getCompany());
        transaction.setCreatedBy(user);

        // Partner məlumatını doldur və balansını yenilə
        if (dto.getPartnerId() != null) {
            Partner partner = partnerRepository.findById(dto.getPartnerId())
                    .orElseThrow(() -> new RuntimeException("Partnyor tapılmadı"));
            transaction.setPartner(partner);

            // Mədaxil: Partnyor pul ödəyir -> onun bizə olan borcu azalır
            Balance balance = partner.getBalance();
            if (balance == null) {
                balance = new Balance();
                balance.setAmount(java.math.BigDecimal.ZERO);
                balance.setCurrencyType("manat");
                balance.setPartner(partner);
                partner.setBalance(balance);
            }
            balance.setAmount(balance.getAmount().subtract(dto.getAmount()));
            partnerRepository.save(partner);
        }

        // Balansı artır
        account.setBalance(account.getBalance().add(dto.getAmount()));
        accountRepository.save(account);

        FinancialTransaction saved = transactionRepository.save(transaction);
        
        notificationService.createEmailNotification(
            String.format("Kassaya yeni ödəniş (Mədaxil) daxil oldu: %.2f AZN", dto.getAmount()),
            user.getCompany()
        );
        
        return mapToReadDto(saved);
    }

    @Override
    @Transactional
    public TransactionReadDto processExpense(KassaExpenseDto dto, String userEmail) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Məbləğ 0-dan böyük olmalıdır!");
        }
        User user = findUserByEmail(userEmail);
        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new RuntimeException("Hesab tapılmadı"));

        if (account.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new RuntimeException("Kassada kifayət qədər vəsait yoxdur!");
        }
        
        TransactionCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Kateqoriya tapılmadı"));

        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setAmount(dto.getAmount());
        transaction.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : java.time.LocalDateTime.now());
        transaction.setType(TransactionType.EXPENSE);
        transaction.setDescription(dto.getDescription());
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setCompany(user.getCompany());
        transaction.setCreatedBy(user);

        if (dto.getPartnerId() != null) {
            Partner partner = partnerRepository.findById(dto.getPartnerId())
                    .orElseThrow(() -> new RuntimeException("Partnyor tapılmadı"));
            transaction.setPartner(partner);
        }

        boolean manager = isManager();
        if (manager) {
            transaction.setStatus("Tamamlandı");
            
            // Partner məlumatını doldur və balansını yenilə
            if (transaction.getPartner() != null) {
                Partner partner = transaction.getPartner();
                // Məxaric: Biz partnyora ödəniş edirik -> bizim ona olan borcumuz azalır (və ya onun bizə borcu artır)
                Balance balance = partner.getBalance();
                if (balance == null) {
                    balance = new Balance();
                    balance.setAmount(java.math.BigDecimal.ZERO);
                    balance.setCurrencyType("manat");
                    balance.setPartner(partner);
                    partner.setBalance(balance);
                }
                balance.setAmount(balance.getAmount().add(dto.getAmount()));
                partnerRepository.save(partner);
            }

            // Balansı azalt
            account.setBalance(account.getBalance().subtract(dto.getAmount()));
            accountRepository.save(account);
        } else {
            transaction.setStatus("Gözləmədə");
        }

        FinancialTransaction saved = transactionRepository.save(transaction);
        return mapToReadDto(saved);
    }

    @Override
    public List<TransactionReadDto> getKassaHistory(String userEmail, Long accountId) {
        User user = findUserByEmail(userEmail);
        return transactionRepository.findByCompanyIdOrderByTransactionDateDesc(user.getCompany().getId())
                .stream()
                .filter(t -> t.getAccount().getId().equals(accountId))
                .map(this::mapToReadDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TransactionReadDto processTransfer(KassaTransferDto dto, String userEmail) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Məbləğ 0-dan böyük olmalıdır!");
        }
        User user = findUserByEmail(userEmail);
        Account fromAccount = accountRepository.findById(dto.getFromAccountId())
                .orElseThrow(() -> new RuntimeException("Mənbə hesab tapılmadı"));

        Account toAccount = accountRepository.findById(dto.getToAccountId())
                .orElseThrow(() -> new RuntimeException("Hədəf hesab tapılmadı"));

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new RuntimeException("Mənbə və hədəf hesabları eyni ola bilməz!");
        }

        if (fromAccount.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new RuntimeException("Mənbə hesabda kifayət qədər vəsait yoxdur!");
        }

        Company company = user.getCompany();

        // Kateqoriyaları tap və ya yarat
        TransactionCategory expenseCategory = categoryRepository.findByCompanyIdAndTypeAndName(
                company.getId(), TransactionType.EXPENSE, "Kassa transferi (Məxaric)"
        ).orElseGet(() -> {
            TransactionCategory cat = new TransactionCategory();
            cat.setName("Kassa transferi (Məxaric)");
            cat.setType(TransactionType.EXPENSE);
            cat.setCompany(company);
            return categoryRepository.save(cat);
        });

        TransactionCategory incomeCategory = categoryRepository.findByCompanyIdAndTypeAndName(
                company.getId(), TransactionType.INCOME, "Kassa transferi (Mədaxil)"
        ).orElseGet(() -> {
            TransactionCategory cat = new TransactionCategory();
            cat.setName("Kassa transferi (Mədaxil)");
            cat.setType(TransactionType.INCOME);
            cat.setCompany(company);
            return categoryRepository.save(cat);
        });

        String desc = dto.getDescription();
        if (desc == null || desc.trim().isEmpty()) {
            desc = "Transfer: " + fromAccount.getName() + " -> " + toAccount.getName();
        }

        java.time.LocalDateTime txDate = dto.getTransactionDate() != null ? dto.getTransactionDate() : java.time.LocalDateTime.now();

        // Məxaric tranzaksiyası
        FinancialTransaction expenseTx = new FinancialTransaction();
        expenseTx.setAmount(dto.getAmount());
        expenseTx.setTransactionDate(txDate);
        expenseTx.setType(TransactionType.EXPENSE);
        expenseTx.setDescription(desc);
        expenseTx.setAccount(fromAccount);
        expenseTx.setCategory(expenseCategory);
        expenseTx.setCompany(company);
        expenseTx.setCreatedBy(user);

        // Mədaxil tranzaksiyası
        FinancialTransaction incomeTx = new FinancialTransaction();
        incomeTx.setAmount(dto.getAmount());
        incomeTx.setTransactionDate(txDate);
        incomeTx.setType(TransactionType.INCOME);
        incomeTx.setDescription(desc);
        incomeTx.setAccount(toAccount);
        incomeTx.setCategory(incomeCategory);
        incomeTx.setCompany(company);
        incomeTx.setCreatedBy(user);

        boolean manager = isManager();
        if (manager) {
            // Balansları yenilə
            fromAccount.setBalance(fromAccount.getBalance().subtract(dto.getAmount()));
            toAccount.setBalance(toAccount.getBalance().add(dto.getAmount()));
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            expenseTx.setStatus("Tamamlandı");
            incomeTx.setStatus("Tamamlandı");
        } else {
            expenseTx.setStatus("Gözləmədə");
            incomeTx.setStatus("Gözləmədə");
        }

        // Save expense first to get its ID, then link
        FinancialTransaction savedExpense = transactionRepository.save(expenseTx);
        incomeTx.setRelatedTransaction(savedExpense);
        FinancialTransaction savedIncome = transactionRepository.save(incomeTx);

        savedExpense.setRelatedTransaction(savedIncome);
        transactionRepository.save(savedExpense);

        return mapToReadDto(savedExpense);
    }

    @Override
    @Transactional
    public TransactionReadDto approveTransaction(Long id, String userEmail) {
        if (!isManager()) {
            throw new RuntimeException("Təsdiqləmək üçün kifayət qədər yetkiniz yoxdur!");
        }

        FinancialTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tranzaksiya tapılmadı"));

        if (!"Gözləmədə".equals(tx.getStatus())) {
            throw new RuntimeException("Bu tranzaksiya artıq təsdiqlənib və ya rədd edilib!");
        }

        if (tx.getType() == TransactionType.EXPENSE) {
            Account account = tx.getAccount();
            if (account.getBalance().compareTo(tx.getAmount()) < 0) {
                throw new RuntimeException("Kassada kifayət qədər vəsait yoxdur!");
            }

            // Balansı azalt
            account.setBalance(account.getBalance().subtract(tx.getAmount()));
            accountRepository.save(account);

            // Partner balansını yenilə
            if (tx.getPartner() != null) {
                Partner partner = tx.getPartner();
                Balance balance = partner.getBalance();
                if (balance == null) {
                    balance = new Balance();
                    balance.setAmount(java.math.BigDecimal.ZERO);
                    balance.setCurrencyType("manat");
                    balance.setPartner(partner);
                    partner.setBalance(balance);
                }
                balance.setAmount(balance.getAmount().add(tx.getAmount()));
                partnerRepository.save(partner);
            }

            tx.setStatus("Tamamlandı");
            transactionRepository.save(tx);

            // Əgər bu bir Transfer-in EXPENSE tərəfidirsə, digər INCOME tərəfini də təsdiqlə
            FinancialTransaction related = tx.getRelatedTransaction();
            if (related != null && "Gözləmədə".equals(related.getStatus())) {
                Account toAccount = related.getAccount();
                toAccount.setBalance(toAccount.getBalance().add(related.getAmount()));
                accountRepository.save(toAccount);

                related.setStatus("Tamamlandı");
                transactionRepository.save(related);
            }
        } else if (tx.getType() == TransactionType.INCOME) {
            FinancialTransaction related = tx.getRelatedTransaction();
            if (related != null && related.getType() == TransactionType.EXPENSE && "Gözləmədə".equals(related.getStatus())) {
                Account fromAccount = related.getAccount();
                if (fromAccount.getBalance().compareTo(related.getAmount()) < 0) {
                    throw new RuntimeException("Mənbə kassada kifayət qədər vəsait yoxdur!");
                }
                // Mənbədən çıx, hədəfə əlavə et
                fromAccount.setBalance(fromAccount.getBalance().subtract(related.getAmount()));
                accountRepository.save(fromAccount);

                Account toAccount = tx.getAccount();
                toAccount.setBalance(toAccount.getBalance().add(tx.getAmount()));
                accountRepository.save(toAccount);

                tx.setStatus("Tamamlandı");
                related.setStatus("Tamamlandı");
                transactionRepository.save(tx);
                transactionRepository.save(related);
            } else {
                Account account = tx.getAccount();
                account.setBalance(account.getBalance().add(tx.getAmount()));
                accountRepository.save(account);

                tx.setStatus("Tamamlandı");
                transactionRepository.save(tx);
            }
        }

        return mapToReadDto(tx);
    }

    @Override
    @Transactional
    public TransactionReadDto rejectTransaction(Long id, String userEmail) {
        if (!isManager()) {
            throw new RuntimeException("Rədd etmək üçün kifayət qədər yetkiniz yoxdur!");
        }

        FinancialTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tranzaksiya tapılmadı"));

        if (!"Gözləmədə".equals(tx.getStatus())) {
            throw new RuntimeException("Bu tranzaksiya artıq təsdiqlənib və ya rədd edilib!");
        }

        tx.setStatus("Rədd edildi");
        transactionRepository.save(tx);

        FinancialTransaction related = tx.getRelatedTransaction();
        if (related != null && "Gözləmədə".equals(related.getStatus())) {
            related.setStatus("Rədd edildi");
            transactionRepository.save(related);
        }

        return mapToReadDto(tx);
    }

    private boolean isManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_GLOBAL_ADMIN") ||
                               a.equals("ROLE_COMPANY_ADMIN") ||
                               a.equals("MODULE:SALES:SUPER_ADMIN") ||
                               a.equals("MODULE:SALES:ADMIN") ||
                               a.equals("MODULE:PURCHASING:SUPER_ADMIN") ||
                               a.equals("MODULE:PURCHASING:ADMIN") ||
                               a.equals("MODULE:FINANCE:SUPER_ADMIN") ||
                               a.equals("MODULE:FINANCE:ADMIN"));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
    }

    private TransactionReadDto mapToReadDto(FinancialTransaction t) {
        TransactionReadDto dto = new TransactionReadDto();
        dto.setId(t.getId());
        dto.setAmount(t.getAmount());
        dto.setTransactionDate(t.getTransactionDate());
        dto.setType(t.getType().name());
        dto.setDescription(t.getDescription());
        dto.setAccountName(t.getAccount().getName());
        dto.setCategoryName(t.getCategory().getName());
        dto.setStatus(t.getStatus() != null ? t.getStatus() : "Tamamlandı");
        if (t.getPartner() != null) {
            dto.setPartnerId(t.getPartner().getId());
            dto.setPartnerName(t.getPartner().getName());
        }
        return dto;
    }
}
