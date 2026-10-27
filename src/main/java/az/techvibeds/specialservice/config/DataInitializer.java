package az.techvibeds.specialservice.config;

import az.techvibeds.specialservice.enums.*;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.models.Module;
import az.techvibeds.specialservice.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import az.techvibeds.specialservice.models.Currency;
import org.springframework.jdbc.core.JdbcTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final CompanyRepository companyRepository;
    private final CompanyModuleRepository companyModuleRepository;
    private final UserModuleRoleRepository userModuleRoleRepository;
    private final CurrencyRepository currencyRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CategoryRepository categoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final WarehouseProductRepository warehouseProductRepository;
    private final PartnerRepository partnerRepository;
    private final ServiceRepository serviceRepository;
    private final UnitRepository unitRepository;
    private final AssigneeRepository assigneeRepository;
    private final StockCountRepository stockCountRepository;
    private final StockCountItemRepository stockCountItemRepository;
    private final ForeignPurchaseRepository foreignPurchaseRepository;
    private final SemiProductRepository semiProductRepository;
    private final ConstructionProjectRepository constructionProjectRepository;
    private final ProjectStageRepository projectStageRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectExpenseRepository projectExpenseRepository;
    private final ProjectContractorRepository projectContractorRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final ManufacturedProductRepository manufacturedProductRepository;
    private final PaymentPlanRepository paymentPlanRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final RoleTemplateRepository roleTemplateRepository;
    private final RoleTemplatePermissionRepository roleTemplatePermissionRepository;
    private final TwoFactorSettingRepository twoFactorSettingRepository;
    private final BackupSettingRepository backupSettingRepository;
    private final AccountRepository accountRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final HREmployeeRepository hrEmployeeRepository;
    private final ContractRepository contractRepository;
    private final PayrollRepository payrollRepository;
    private final DeductionRepository deductionRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final HRActivityRepository hrActivityRepository;
    private final InvoiceRepository invoiceRepository;
    private final InstallmentRepository installmentRepository;
    private final IndirectExpenseRepository indirectExpenseRepository;
    private final ForeignCurrencyPurchaseRepository foreignCurrencyPurchaseRepository;
    private final AuditLogRepository auditLogRepository;
    private final ConstructionProjectMaterialRepository constructionProjectMaterialRepository;
    private final CompanyStockRepository companyStockRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    private static final String MAIN_WAREHOUSE_NAME = "Əsas Anbar";
    private static final String SECOND_WAREHOUSE_NAME = "İkinci Anbar";
    private static final String RESERVE_WAREHOUSE_NAME = "Ehtiyat Anbar";

    @Override
    @Transactional
    public void run(String... args) {
        try {
            log.info("Starting data initialization...");

            // ——— Data fix: NULL product_type dəyərlərini GOODS ilə doldur ———
            log.info("Fixing NULL product_type values in products table...");
            try {
                jdbcTemplate.execute(
                    "DO $$ BEGIN " +
                    "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'products' AND column_name = 'product_type') THEN " +
                    "    UPDATE products SET product_type = 'GOODS' WHERE product_type IS NULL; " +
                    "END IF; " +
                    "END $$;"
                );
                log.info("Successfully fixed NULL product_type values.");
            } catch (Exception e) {
                log.warn("Could not fix product_type nulls: {}", e.getMessage());
            }

            // ——— Data fix: services.amount sütununun tipini düzəlt ———
            log.info("Fixing services.amount column type if needed...");
            try {
                jdbcTemplate.execute(
                    "DO $$ BEGIN " +
                    "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'services' AND column_name = 'amount' AND data_type != 'numeric') THEN " +
                    "    ALTER TABLE services ALTER COLUMN amount TYPE numeric(38,2) USING amount::numeric(38,2); " +
                    "END IF; " +
                    "END $$;"
                );
                log.info("Successfully fixed services.amount column type.");
            } catch (Exception e) {
                log.warn("Could not fix services.amount type: {}", e.getMessage());
            }


            log.info("Dropping obsolete invoices status check constraint if exists...");
            try {
                jdbcTemplate.execute("ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_status_check");
                log.info("Successfully dropped constraint invoices_status_check.");
            } catch (Exception e) {
                log.warn("Could not drop constraint invoices_status_check: {}", e.getMessage());
            }

            log.info("Dropping obsolete single-product columns from production_orders if they exist...");
            try {
                jdbcTemplate.execute("ALTER TABLE production_orders DROP COLUMN IF EXISTS quantity");
                jdbcTemplate.execute("ALTER TABLE production_orders DROP COLUMN IF EXISTS material_quantity");
                jdbcTemplate.execute("ALTER TABLE production_orders DROP COLUMN IF EXISTS manufactured_product_id");
                jdbcTemplate.execute("ALTER TABLE production_orders DROP COLUMN IF EXISTS material_product_id");
                log.info("Successfully dropped obsolete columns from production_orders.");
            } catch (Exception e) {
                log.warn("Could not drop obsolete columns from production_orders: {}", e.getMessage());
            }

            // Standard units
            initializeStandardUnits();

            // Roles
            Role globalAdminRole = createRoleIfNotExist("ROLE_GLOBAL_ADMIN");
            Role companyAdminRole = createRoleIfNotExist("ROLE_COMPANY_ADMIN");
            Role userAdminRole = createRoleIfNotExist("USER_ADMIN");
            Role userRole = createRoleIfNotExist("USER");

            // Super Admin
            createSuperAdmin(globalAdminRole);

            log.info("Renaming TRANSACTION module to FINANCE if it exists...");
            try {
                jdbcTemplate.execute("UPDATE modules SET name = 'FINANCE', backend_key = 'FINANCE' WHERE name = 'TRANSACTION'");
                log.info("Successfully renamed TRANSACTION to FINANCE.");
            } catch (Exception e) {
                log.warn("Could not rename TRANSACTION to FINANCE: {}", e.getMessage());
            }

            log.info("Fixing duplicate modules caused by casing changes...");
            try {
                jdbcTemplate.execute(
                    "DO $$ " +
                    "DECLARE " +
                    "    dup RECORD; " +
                    "BEGIN " +
                    "    FOR dup IN (SELECT UPPER(name) as uname, MIN(id) as min_id, MAX(id) as max_id FROM modules GROUP BY UPPER(name) HAVING COUNT(*) > 1) LOOP " +
                    "        UPDATE company_modules SET module_id = dup.min_id WHERE module_id = dup.max_id AND NOT EXISTS (SELECT 1 FROM company_modules cm2 WHERE cm2.module_id = dup.min_id AND cm2.company_id = company_modules.company_id); " +
                    "        UPDATE role_template_permissions SET module_id = dup.min_id WHERE module_id = dup.max_id AND NOT EXISTS (SELECT 1 FROM role_template_permissions rtp2 WHERE rtp2.module_id = dup.min_id AND rtp2.role_template_id = role_template_permissions.role_template_id); " +
                    "        UPDATE user_module_roles umr SET company_module_id = new_cm.id FROM company_modules old_cm JOIN company_modules new_cm ON new_cm.company_id = old_cm.company_id AND new_cm.module_id = dup.min_id WHERE umr.company_module_id = old_cm.id AND old_cm.module_id = dup.max_id; " +
                    "        DELETE FROM user_module_roles WHERE company_module_id IN (SELECT id FROM company_modules WHERE module_id = dup.max_id); " +
                    "        DELETE FROM company_modules WHERE module_id = dup.max_id; " +
                    "        DELETE FROM role_template_permissions WHERE module_id = dup.max_id; " +
                    "        DELETE FROM modules WHERE id = dup.max_id; " +
                    "        UPDATE modules SET name = dup.uname, backend_key = dup.uname WHERE id = dup.min_id; " +
                    "    END LOOP; " +
                    "    UPDATE modules SET name = UPPER(name), backend_key = UPPER(name) WHERE name != UPPER(name) OR backend_key IS NULL; " +
                    "END $$;"
                );
                log.info("Successfully fixed duplicate modules.");
            } catch (Exception e) {
                log.warn("Could not fix duplicate modules: {}", e.getMessage());
            }

            // Modules
            Map<String, Module> modules = initializeModules();

            // Test Company
            Company testCompany = createCompanyIfNotExist("Test Company LLC");

            // Connect Modules to Company
            Map<String, CompanyModule> companyModules = initializeCompanyModules(testCompany, modules);

            // Company Admin
            User fuadAdmin = createCompanyAdminIfNotExist("fuad@test.com", testCompany, companyAdminRole);
            ensureUserHasRole(fuadAdmin, userAdminRole);

            // Assign Module Roles to Fuad
            assignSuperAdminRoleToAllModules(fuadAdmin, companyModules);

            // Default Currencies
            initializeCurrencies();

            // Default Subscription for Testing
            initializeTestSubscription(testCompany);

            // Warehouse, Category, Products
            initializeTestWarehouses(testCompany);
            initializeTestInventory(testCompany);

            // Stock Count and Entities
            initializeTestEntities(testCompany, fuadAdmin);

            // Test Foreign Purchases (Complex)
            initializeComplexForeignPurchases(testCompany);

            // Transaction module seed data (Customer / Supplier)
            initializeTestPartners(testCompany);

            // Service module seed data
            initializeTestServices(testCompany);

            // Semi-product module seed data
            initializeTestSemiProducts(testCompany);

            // Construction module seed data
            initializeTestConstruction(testCompany);

            // Manufacturing module seed data
            initializeTestManufacturing(testCompany);

            // Payment Plan module seed data
            initializeTestPaymentPlans(testCompany);

            // HR module seed data
            initializeTestEmployees(testCompany);

            // Security module seed data
            initializeSecurityUsers(testCompany, userRole, userAdminRole, companyModules);

            // Role Templates
            initializeRoleTemplates(testCompany, modules);

            // 2FA Settings
            initializeTwoFactorSettings(testCompany);

            // Backup Settings
            initializeBackupSettings(testCompany);

            // HR module advanced seed data
            initializeTestHRDetails(testCompany);

            // Kassa module seed data
            initializeTestKassa(testCompany, fuadAdmin);

            // Advanced Manufacturing (Indirect Expenses)
            initializeAdvancedManufacturing(testCompany);

            // Local Invoices
            initializeTestInvoices(testCompany);

            // Foreign Currency Purchases
            initializeTestForeignCurrencyPurchases(testCompany);

            // HR Activities
            initializeTestHRActivities(testCompany);

            // Construction Project Materials
            initializeTestConstructionMaterials(testCompany);

            // Finance extra (expense category + expense transaction)
            initializeTestFinanceExtra(testCompany, fuadAdmin);

            // Installments for PaymentPlan
            initializeTestInstallments(testCompany);

            // Notification Settings
            initializeNotificationSettings(testCompany);

            // Company Stock history
            initializeTestCompanyStock(testCompany);

            // Audit Logs
            initializeTestAuditLogs(fuadAdmin);

            syncCompanyAdminsPermissions();
            log.info("Data initialization completed.");
        } catch (Exception e) {
            log.error("CRITICAL ERROR IN DATA INITIALIZER: ", e);
        }
    }

    private void syncCompanyAdminsPermissions() {
        log.info("Syncing missing SUPER_ADMIN permissions for all USER_ADMINs...");
        List<User> companyAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "USER_ADMIN".equals(r.getName())))
                .toList();

        for (User admin : companyAdmins) {
            if (admin.getCompany() != null) {
                List<CompanyModule> companyModules = companyModuleRepository.findAllByCompanyId(admin.getCompany().getId());
                for (CompanyModule cm : companyModules) {
                    if (Boolean.TRUE.equals(cm.getActive())) {
                        assignModuleRoleIfNotExist(admin, cm, az.techvibeds.specialservice.enums.ModuleRole.SUPER_ADMIN);
                    }
                }
            }
        }
    }

    private Role createRoleIfNotExist(String name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            log.info("Creating role: {}...", name);
            Role role = new Role();
            role.setName(name);
            return roleRepository.save(role);
        });
    }

    private void createSuperAdmin(Role globalAdminRole) {
        String adminEmail = "superadmin@specialservice.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            log.info("Creating default Super Admin user...");
            User superAdmin = new User();
            superAdmin.setName("Super");
            superAdmin.setSurname("Admin");
            superAdmin.setEmail(adminEmail);
            superAdmin.setPassword(passwordEncoder.encode("superadmin123"));
            superAdmin.setActive(true);
            superAdmin.setDeleted(false);
            superAdmin.setRoles(Collections.singletonList(globalAdminRole));
            userRepository.save(superAdmin);
        }
    }

    private Module createModuleIfNotExist(String name, String description) {
        Module existing = moduleRepository.findByName(name).orElse(null);
        if (existing == null) {
            log.info("Creating module: {}...", name);
            Module module = new Module();
            module.setName(name);
            module.setDescription(description);
            module.setBackendKey(name);
            return moduleRepository.save(module);
        } else if (existing.getBackendKey() == null) {
            log.info("Fixing missing backendKey for module: {}", name);
            existing.setBackendKey(name);
            return moduleRepository.save(existing);
        }
        return existing;
    }

    private Map<String, Module> initializeModules() {
        Map<String, String> moduleDefinitions = new LinkedHashMap<>();
        moduleDefinitions.put("INVOICE", "Invoice management module");
        moduleDefinitions.put("PARTNER", "Partner management module");
        moduleDefinitions.put("INVENTORY", "Inventory management module");
        moduleDefinitions.put("HR", "Human Resources management module");
        moduleDefinitions.put("REPORT", "Reports management module");
        moduleDefinitions.put("MANUFACTURING", "Manufacturing management module");
        moduleDefinitions.put("SERVICE", "Service management module");
        moduleDefinitions.put("FINANCE", "Finance management module");
        moduleDefinitions.put("CONSTRUCTION", "Construction management module");
        moduleDefinitions.put("SECURITY", "Security and user management module");
        moduleDefinitions.put("DASHBOARD", "Dashboard and analytics module");
        moduleDefinitions.put("FOREIGN_PURCHASE", "Foreign purchase management module");
        moduleDefinitions.put("NOTIFICATION", "Notification management module");
        moduleDefinitions.put("SALES", "Sales management module");
        moduleDefinitions.put("PURCHASING", "Purchasing management module");

        Map<String, Module> modules = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : moduleDefinitions.entrySet()) {
            modules.put(entry.getKey(), createModuleIfNotExist(entry.getKey(), entry.getValue()));
        }
        return modules;
    }

    private Map<String, CompanyModule> initializeCompanyModules(Company company, Map<String, Module> modules) {
        Map<String, CompanyModule> companyModules = new LinkedHashMap<>();
        for (Map.Entry<String, Module> entry : modules.entrySet()) {
            companyModules.put(entry.getKey(), createCompanyModuleIfNotExist(company, entry.getValue()));
        }
        return companyModules;
    }

    private void assignSuperAdminRoleToAllModules(User user, Map<String, CompanyModule> companyModules) {
        for (CompanyModule companyModule : companyModules.values()) {
            assignModuleRoleIfNotExist(user, companyModule, ModuleRole.SUPER_ADMIN);
        }
    }

    private Company createCompanyIfNotExist(String name) {
        return companyRepository.findByName(name).orElseGet(() -> {
            log.info("Creating company: {}...", name);
            Company company = new Company();
            company.setName(name);
            company.setActive(true);
            company.setExpressDate(new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000));
            return companyRepository.save(company);
        });
    }

    private CompanyModule createCompanyModuleIfNotExist(Company company, Module module) {
        return companyModuleRepository.findByCompanyIdAndModuleId(company.getId(), module.getId())
                .orElseGet(() -> {
                    log.info("Connecting module {} to company {}...", module.getName(), company.getName());
                    CompanyModule companyModule = new CompanyModule();
                    companyModule.setCompany(company);
                    companyModule.setModule(module);
                    companyModule.setActive(true);
                    companyModule.setStartDate(new Date());
                    return companyModuleRepository.save(companyModule);
                });
    }

    private User createCompanyAdminIfNotExist(String email, Company company, Role adminRole) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Creating company admin: {}...", email);
            User user = new User();
            user.setName("Company");
            user.setSurname("Admin");
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("password123"));
            user.setActive(true);
            user.setDeleted(false);
            user.setCompany(company);
            user.setRoles(new ArrayList<>(Collections.singletonList(adminRole)));
            return userRepository.save(user);
        });
    }

    private void ensureUserHasRole(User user, Role role) {
        if (user.getRoles().stream().noneMatch(existingRole -> existingRole.getName().equals(role.getName()))) {
            user.getRoles().add(role);
            userRepository.save(user);
        }
    }

    private void initializeCurrencies() {
        if (currencyRepository.count() == 0) {
            createCurrency("AZN", "Azerbaijani Manat", "₼", BigDecimal.ONE);
            createCurrency("USD", "US Dollar", "$", new BigDecimal("1.70"));
            createCurrency("EUR", "Euro", "€", new BigDecimal("1.82"));
            createCurrency("TRY", "Turkish Lira", "₺", new BigDecimal("0.052"));
        }
    }

    private void createCurrency(String code, String name, String symbol, BigDecimal rate) {
        Currency c = new Currency();
        c.setCode(code);
        c.setName(name);
        c.setSymbol(symbol);
        c.setCurrentRate(rate);
        currencyRepository.save(c);
    }

    private void initializeTestSubscription(Company company) {
        if (subscriptionRepository.findByCompanyId(company.getId()).isEmpty()) {
            SubscriptionPlan premiumPlan = subscriptionPlanRepository.findByName("Premium")
                    .orElseGet(() -> {
                        SubscriptionPlan sp = new SubscriptionPlan();
                        sp.setName("Premium");
                        sp.setPrice(new BigDecimal("99.00"));
                        return subscriptionPlanRepository.save(sp);
                    });

            Subscription sub = new Subscription();
            sub.setCompany(company);
            sub.setPlan(premiumPlan);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setStartDate(LocalDate.now());
            sub.setEndDate(LocalDate.now().plusYears(1));
            subscriptionRepository.save(sub);
            log.info("Created test subscription for company {}", company.getName());
        }
    }

    private void initializeTestWarehouses(Company company) {
        createWarehouseIfNotExist(MAIN_WAREHOUSE_NAME, "Baku, Azerbaijan", company);
        createWarehouseIfNotExist(SECOND_WAREHOUSE_NAME, "Sumqayit, Azerbaijan", company);
        createWarehouseIfNotExist(RESERVE_WAREHOUSE_NAME, "Ganja, Azerbaijan", company);
    }

    private void initializeTestInventory(Company company) {
        if (productRepository.countByCompanyId(company.getId()) > 0) {
            return;
        }

        Category electronicsCategory = createCategoryIfNotExist("Elektronika", company);
        Category foodCategory = createCategoryIfNotExist("Ərzaq", company);
        Category rawMaterialCategory = createCategoryIfNotExist("Xammal", company);

        Warehouse mainWarehouse = createWarehouseIfNotExist(MAIN_WAREHOUSE_NAME, "Baku, Azerbaijan", company);
        Warehouse secondWarehouse = createWarehouseIfNotExist(SECOND_WAREHOUSE_NAME, "Sumqayit, Azerbaijan", company);

        Product laptop = createProduct("Laptop HP ProBook", "PRD-001", new BigDecimal("1500.00"),
                50L, electronicsCategory, ProductStatus.IN_STOCK,
                VatStatus.WITHOUT_VAT, null, company);
        createWarehouseProductRecord(laptop, mainWarehouse, 30);
        createWarehouseProductRecord(laptop, secondWarehouse, 20);

        Product monitor = createProduct("Monitor Dell 27\"", "PRD-002", null,
                null, electronicsCategory, ProductStatus.INACTIVE,
                VatStatus.WITHOUT_VAT, null, company);
        createWarehouseProductRecord(monitor, mainWarehouse, 0);

        Product sugar = createProduct("Şəkər 1kg", "GDS-001", new BigDecimal("2.50"),
                200L, foodCategory, ProductStatus.IN_STOCK,
                VatStatus.WITH_VAT, new BigDecimal("18"), company);
        createWarehouseProductRecord(sugar, mainWarehouse, 200);

        Product flour = createProduct("Un 2kg", "GDS-002", new BigDecimal("3.00"),
                150L, foodCategory, ProductStatus.IN_STOCK,
                VatStatus.WITHOUT_VAT, BigDecimal.ZERO, company);
        createWarehouseProductRecord(flour, mainWarehouse, 100);
        createWarehouseProductRecord(flour, secondWarehouse, 50);

        Product steel = createProduct("Polad lövhə 2mm", "GDS-003", null,
                80L, rawMaterialCategory, ProductStatus.IN_STOCK,
                VatStatus.WITH_VAT, new BigDecimal("18"), company);
        createWarehouseProductRecord(steel, secondWarehouse, 80);
    }

    private Category createCategoryIfNotExist(String name, Company company) {
        return categoryRepository.findByNameAndCompanyId(name, company.getId())
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName(name);
                    category.setCompany(company);
                    return categoryRepository.save(category);
                });
    }

    private Warehouse createWarehouseIfNotExist(String name, String address, Company company) {
        return warehouseRepository.findByNameAndCompanyId(name, company.getId()).orElseGet(() -> {
            Warehouse warehouse = new Warehouse();
            warehouse.setName(name);
            warehouse.setAddress(address);
            warehouse.setActive(true);
            warehouse.setCompany(company);
            return warehouseRepository.save(warehouse);
        });
    }

    private Product createProduct(String name, String code, BigDecimal price, Long stock,
            Category category, ProductStatus status,
            VatStatus vatStatus, BigDecimal vatRate, Company company) {
        Product product = new Product();
        product.setName(name);
        product.setProductCode(code);
        product.setPrice(price);
        product.setStock(stock);
        product.setCategory(category);
        product.setProductStatus(status);
        product.setVatStatus(vatStatus);
        product.setVatRate(vatRate);
        product.setCompany(company);
        return productRepository.save(product);
    }

    private void createWarehouseProductRecord(Product product, Warehouse warehouse, int quantity) {
        WarehouseProduct wp = new WarehouseProduct();
        wp.setProduct(product);
        wp.setWarehouse(warehouse);
        wp.setQuantity(quantity);
        warehouseProductRepository.save(wp);
    }

    private void initializeTestEntities(Company company, User admin) {
        // Partners
        Partner customer = null;
        if (partnerRepository.countByCompanyId(company.getId()) == 0) {
            customer = new Partner();
            customer.setName("Baku Electronics - Special");
            customer.setContactPerson("Əli Məmmədov");
            customer.setEmail("special.baku@example.com");
            customer.setPartnerType(PartnerType.CUSTOMER);
            customer.setCompany(company);
            customer.setIsActive(true);
            customer = partnerRepository.save(customer);
        } else {
            customer = partnerRepository.findAllByCompanyId(company.getId()).stream()
                    .filter(p -> p.getPartnerType() == PartnerType.CUSTOMER)
                    .findFirst()
                    .orElse(null);
        }

        // Services
        if (serviceRepository.countByCompanyId(company.getId()) == 0) {
            Unit unit = new Unit();
            unit.setName("Xidmət Saatı");
            unit = unitRepository.save(unit);

            Assignee assignee = new Assignee();
            assignee.setName("İlqar Vəliyev");
            assignee.setCompany(company);
            assignee = assigneeRepository.save(assignee);

            Service service = new Service();
            service.setName("Kondisioner Təmiri");
            service.setAmount(new java.math.BigDecimal("50.00"));
            service.setDeadline(LocalDate.now().plusDays(7));
            service.setUnit(unit);
            service.setStatus(ServiceStatus.IN_PROGRESS);
            service.setAssignee(assignee);
            service.setCompany(company);
            service.setPartner(customer);
            serviceRepository.save(service);
        }

        // Stock Count
        if (stockCountRepository.findAllByCompanyId(company.getId()).isEmpty()) {
            Warehouse mainWarehouse = warehouseRepository.findByNameAndCompanyId(MAIN_WAREHOUSE_NAME, company.getId())
                    .orElse(null);
            if (mainWarehouse != null) {
                StockCount count = StockCount.builder()
                        .countNumber("SC-TEST-001")
                        .warehouse(mainWarehouse)
                        .countedBy(admin)
                        .countDate(LocalDateTime.now())
                        .status(StockCountStatus.DRAFT)
                        .company(company)
                        .items(new ArrayList<>())
                        .build();

                StockCount savedCount = stockCountRepository.save(count);
                productRepository.findAllByCompany_Id(company.getId()).stream().limit(2).forEach(product -> {
                    WarehouseProduct wp = warehouseProductRepository.findByProductAndWarehouse(product, mainWarehouse);
                    int expected = (wp != null) ? (wp.getQuantity() != null ? wp.getQuantity() : 0) : 0;
                    StockCountItem item = StockCountItem.builder()
                            .product(product)
                            .expectedQuantity(expected)
                            .countedQuantity(expected + 5)
                            .difference(5)
                            .stockCount(savedCount)
                            .build();
                    stockCountItemRepository.save(item);
                });
            }
        }
    }

    private void assignModuleRoleIfNotExist(User user, CompanyModule companyModule, ModuleRole role) {
        if (!userModuleRoleRepository.existsByUserIdAndCompanyModuleId(user.getId(), companyModule.getId())) {
            UserModuleRole umr = new UserModuleRole();
            umr.setUser(user);
            umr.setCompanyModule(companyModule);
            umr.setRole(role);
            userModuleRoleRepository.save(umr);
        }
    }

    private void initializeComplexForeignPurchases(Company company) {
        if (foreignPurchaseRepository.findAllByCompanyId(company.getId()).isEmpty()) {
            Currency usd = currencyRepository.findByCode("USD").orElse(null);
            Partner supplier = partnerRepository.findAllByCompanyId(company.getId()).stream().findFirst().orElse(null);

            if (usd != null && supplier != null) {
                ForeignPurchase purchase = new ForeignPurchase();
                purchase.setInvoiceNumber("F-PUR-2024-001");
                purchase.setInvoiceDate(LocalDate.now());
                purchase.setSupplier(supplier);
                purchase.setCurrency(usd);
                purchase.setExchangeRate(usd.getCurrentRate());
                purchase.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
                purchase.setCompany(company);

                ForeignPurchaseItem item1 = new ForeignPurchaseItem();
                item1.setName("Məhsul 1");
                item1.setUnit("ədəd");
                item1.setUnitPrice(new BigDecimal("15.00"));
                item1.setQuantity(new BigDecimal("2"));
                item1.setForeignPurchase(purchase);

                ForeignPurchaseItem item2 = new ForeignPurchaseItem();
                item2.setName("Xidmət C");
                item2.setUnit("saat");
                item2.setUnitPrice(new BigDecimal("50.00"));
                item2.setQuantity(new BigDecimal("3"));
                item2.setForeignPurchase(purchase);

                purchase.addItem(item1);
                purchase.addItem(item2);
                purchase.recalculateTotals();

                foreignPurchaseRepository.save(purchase);
            }
        }
    }

    private void initializeTestPartners(Company company) {
        if (!partnerRepository.findAllByCompanyId(company.getId()).isEmpty()) {
            return;
        }

        createPartner(company, "Araz Market", "Rauf Hasanov", "araz.market@test.com", "+994501112233",
                PartnerType.CUSTOMER, new BigDecimal("1250.00"), "AZN", true);
        createPartner(company, "Global Supplies MMC", "Kamal Aliyev", "global.supplies@test.com", "+994705556677",
                PartnerType.SUPPLIER, new BigDecimal("3000.00"), "AZN", true);
    }

    private void createPartner(Company company, String name, String contactPerson, String email, String phone,
            PartnerType partnerType, BigDecimal balanceAmount, String currencyType, boolean isActive) {
        Partner partner = new Partner();
        partner.setCompany(company);
        partner.setName(name);
        partner.setContactPerson(contactPerson);
        partner.setEmail(email);
        partner.setPhone(phone);
        partner.setPartnerType(partnerType);
        partner.setIsActive(isActive);

        Balance balance = new Balance();
        balance.setAmount(balanceAmount);
        balance.setCurrencyType(currencyType);
        balance.setPartner(partner);
        partner.setBalance(balance);
        partnerRepository.save(partner);
    }

    private void initializeTestServices(Company company) {
        if (!serviceRepository.findAllByCompany(company).isEmpty()) {
            return;
        }
        Unit pieceUnit = unitRepository.findByName("Ədəd").orElseGet(() -> createUnit("Ədəd", UnitCategory.BOTH));
        Assignee assignee = assigneeRepository.findAllByCompany(company).stream().findFirst()
                .orElse(createAssignee(company, "Nicat Məmmədov", 0, 10));

        Partner partner = partnerRepository.findAllByCompanyId(company.getId()).stream().findFirst().orElse(null);

        createService(company, "Printer Maintenance", "Office printer service", new BigDecimal("20.00"), LocalDate.now().plusDays(7),
                ServiceStatus.PENDING, pieceUnit, assignee, partner);
    }

    private Unit createUnit(String name, UnitCategory category) {
        Unit unit = new Unit();
        unit.setName(name);
        unit.setCategory(category);
        return unitRepository.save(unit);
    }

    private void initializeStandardUnits() {
        // 1. Migrate existing units that have a null category
        List<Unit> existingUnits = unitRepository.findAll();
        for (Unit u : existingUnits) {
            if (u.getCategory() == null) {
                String name = u.getName();
                if (name.equals("Saat") || name.equals("Gün") || name.equals("Həftə") || 
                    name.equals("Ay") || name.equals("İl") || name.equals("Dəfə") || 
                    name.equals("Xidmət Saatı")) {
                    u.setCategory(UnitCategory.SERVICE);
                } else if (name.equals("Ədəd")) {
                    u.setCategory(UnitCategory.BOTH);
                } else if (name.equals("Kilometr")) {
                    u.setCategory(UnitCategory.PRODUCT);
                } else {
                    u.setCategory(UnitCategory.PRODUCT);
                }
                unitRepository.save(u);
            }
        }

        // 2. Initialize service units
        String[] serviceUnits = {"Saat", "Gün", "Həftə", "Ay", "İl", "Dəfə"};
        for (String unitName : serviceUnits) {
            if (unitRepository.findByName(unitName).isEmpty()) {
                createUnit(unitName, UnitCategory.SERVICE);
            }
        }

        // 3. Initialize product units
        String[] productUnits = {"Kiloqram", "Qram", "Metr", "Litr"};
        for (String unitName : productUnits) {
            if (unitRepository.findByName(unitName).isEmpty()) {
                createUnit(unitName, UnitCategory.PRODUCT);
            }
        }

        // 4. Initialize both
        if (unitRepository.findByName("Ədəd").isEmpty()) {
            createUnit("Ədəd", UnitCategory.BOTH);
        }
    }

    private Assignee createAssignee(Company company, String name, Integer activeServiceCount, Integer totalCapacity) {
        Assignee assignee = new Assignee();
        assignee.setCompany(company);
        assignee.setName(name);
        assignee.setActiveServiceCount(activeServiceCount);
        assignee.setTotalCapacity(totalCapacity);
        return assigneeRepository.save(assignee);
    }

    private void createService(Company company, String name, String description, BigDecimal amount, LocalDate deadline,
            ServiceStatus status, Unit unit, Assignee assignee, Partner partner) {
        Service service = new Service();
        service.setCompany(company);
        service.setName(name);
        service.setDescription(description);
        service.setAmount(amount);
        service.setDeadline(deadline);
        service.setStatus(status);
        service.setUnit(unit);
        service.setAssignee(assignee);
        service.setPartner(partner);
        serviceRepository.save(service);
    }

    private void initializeTestSemiProducts(Company company) {
        if (!semiProductRepository.findAllByCompany(company).isEmpty()) {
            return;
        }
        createSemiProduct(company, "Yarım Hazır Korpus", "SP-001", "Şkaf istehsalı üçün korpus", UnitType.PIECE, 120,
                SemiProductStatus.AVAILABLE, new BigDecimal("45.00"), VatStatus.WITHOUT_VAT, null);
        createSemiProduct(company, "Dəmir Ayaqaltı", "SP-002", "Masa üçün metal ayaqaltı", UnitType.PIECE, 50,
                SemiProductStatus.AVAILABLE, new BigDecimal("12.50"), VatStatus.WITH_VAT, new BigDecimal("18"));
        createSemiProduct(company, "Plastik Qapaq", "SP-003", "Konteyner qapağı", UnitType.PIECE, 300,
                SemiProductStatus.AVAILABLE, new BigDecimal("0.80"), VatStatus.WITH_VAT, new BigDecimal("18"));
    }

    private void createSemiProduct(Company company, String name, String code, String description, UnitType unit,
            Integer stockQuantity, SemiProductStatus status, BigDecimal price, VatStatus vatStatus,
            BigDecimal vatRate) {
        SemiProduct semiProduct = new SemiProduct();
        semiProduct.setCompany(company);
        semiProduct.setName(name);
        semiProduct.setProductCode(code);
        semiProduct.setDescription(description);
        semiProduct.setUnit(unit);
        semiProduct.setStock(stockQuantity != null ? stockQuantity.longValue() : 0L);
        semiProduct.setStatus(status);
        semiProduct.setPrice(price);
        semiProduct.setVatStatus(vatStatus);
        semiProduct.setVatRate(vatRate);
        semiProductRepository.save(semiProduct);
    }

    private void initializeTestConstruction(Company company) {
        if (!constructionProjectRepository.findAllByCompany_Id(company.getId()).isEmpty()) {
            return;
        }
        ProjectContractor contractor = projectContractorRepository.findAllByCompany_Id(company.getId())
                .stream().filter(c -> c.getName().equals("Premium Construction MMC")).findFirst()
                .orElseGet(() -> {
                    ProjectContractor pc = new ProjectContractor();
                    pc.setName("Premium Construction MMC");
                    pc.setCompany(company);
                    return projectContractorRepository.save(pc);
                });

        ConstructionProject project = new ConstructionProject();
        project.setCompany(company);
        project.setName("Residential Complex A");
        project.setStartDate(LocalDate.now());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setBudget(new BigDecimal("500000.00"));
        project.setProjectContractor(contractor);
        project = constructionProjectRepository.save(project);

        // Project Stages
        createProjectStage(project, "Bünövrə işləri", 1, 100.0, StageStatus.COMPLETED);
        createProjectStage(project, "Birinci mərtəbə", 2, 60.0, StageStatus.ACTIVE);
        createProjectStage(project, "Dam örtüyü", 3, 0.0, StageStatus.PENDING);

        // Project Expenses
        createProjectExpense(project, "Sement alışı", new BigDecimal("2500.00"), LocalDate.now().minusDays(10));
        createProjectExpense(project, "Dəmir dirəklər", new BigDecimal("4800.00"), LocalDate.now().minusDays(5));
        createProjectExpense(project, "Usta ödənişi", new BigDecimal("1200.00"), LocalDate.now().minusDays(1));
    }

    private void createProjectStage(ConstructionProject project, String name, Integer number, Double progress,
            StageStatus status) {
        ProjectStage stage = new ProjectStage();
        stage.setProject(project);
        stage.setName(name);
        stage.setStageNumber(number);
        stage.setProgressPercentage(progress);
        stage.setStatus(status);
        projectStageRepository.save(stage);
    }

    private void createProjectExpense(ConstructionProject project, String name, BigDecimal amount, LocalDate date) {
        ProjectExpense expense = new ProjectExpense();
        expense.setProject(project);
        expense.setName(name);
        expense.setAmount(amount);
        expense.setDate(date);
        projectExpenseRepository.save(expense);
    }

    private void initializeTestEmployees(Company company) {
        if (!employeeRepository.findAllByCompanyId(company.getId()).isEmpty()) {
            return;
        }
        createEmployee(company, "Taleh", "Həsənov", "taleh.h@test.com", "+994501234567", "Senior Developer", "IT",
                LocalDate.now().minusYears(2), new BigDecimal("3500.00"));
    }

    private void createEmployee(Company company, String firstName, String lastName, String email, String phone,
            String position, String department, LocalDate hireDate, BigDecimal salary) {
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setPhoneNumber(phone);
        employee.setPosition(position);
        employee.setDepartment(department);
        employee.setHireDate(hireDate);
        employee.setSalary(salary);
        employeeRepository.save(employee);
    }

    private void initializeTestManufacturing(Company company) {
        if (productionOrderRepository.countByCompanyId(company.getId()) > 0) {
            return;
        }

        ManufacturedProduct mp = new ManufacturedProduct();
        mp.setName("Metal Door Frame");
        mp.setCompany(company);
        mp = manufacturedProductRepository.save(mp);

        // İstehsalat üçün material olaraq normal Product istifadə edirik
        List<Product> companyProducts = productRepository.findAllByCompany_Id(company.getId());
        Product materialProduct = companyProducts.stream()
                .filter(p -> p.getName().contains("HP ProBook"))
                .findFirst().orElse(null);

        Product inventoryProduct = companyProducts.stream()
                .filter(p -> !p.equals(materialProduct))
                .findFirst().orElse(materialProduct);

        Warehouse mainWarehouse = warehouseRepository.findByNameAndCompanyId(MAIN_WAREHOUSE_NAME, company.getId())
                .orElse(null);

        if (materialProduct != null && mainWarehouse != null) {
            ProductionOrder order = new ProductionOrder();
            order.setStatus(ProductionOrderStatus.IN_PRODUCTION);
            order.setStartDate(new Date());
            order.setProgressPercentage(45);
            order.setWarehouse(mainWarehouse);
            order.setCompany(company);

            ProductionOrderItem item = new ProductionOrderItem();
            item.setManufacturedProduct(mp);
            item.setProduct(inventoryProduct);
            item.setMaterialProduct(materialProduct);
            item.setQuantity(10);
            item.setMaterialQuantity(20);
            item.setStockApplied(false);

            order.addItem(item);

            productionOrderRepository.save(order);
        }
    }

    private void initializeTestPaymentPlans(Company company) {
        if (paymentPlanRepository.findAllByCompanyId(company.getId(), Pageable.unpaged()).isEmpty()) {
            PaymentPlan plan = new PaymentPlan();
            plan.setName("Equipment Purchase Plan");
            plan.setTotalAmount(new BigDecimal("12000.00"));
            plan.setStatus(PlanStatus.ACTIVE);
            plan.setCompany(company);
            paymentPlanRepository.save(plan);
        }
    }

    private void initializeSecurityUsers(Company company, Role userRole, Role userAdminRole,
            Map<String, CompanyModule> companyModules) {
        createSecurityUserIfNotExist(company, "security.admin@test.com", "Security", "Admin", "password123", true,
                false, userAdminRole);
        createSecurityUserIfNotExist(company, "employee@test.com", "Sıravi", "İşçi", "password123", true,
                false, userRole);

        User cashier = createSecurityUserIfNotExist(company, "kassir@test.com", "Kassir", "Test", "password123", true,
                false, userRole);

        CompanyModule salesModule = companyModules.get("SALES");
        if (salesModule != null) {
            assignModuleRoleIfNotExist(cashier, salesModule, ModuleRole.CREATOR);
        }
        CompanyModule financeModule = companyModules.get("FINANCE");
        if (financeModule != null) {
            assignModuleRoleIfNotExist(cashier, financeModule, ModuleRole.CREATOR);
        }
    }

    private User createSecurityUserIfNotExist(Company company, String email, String name, String surname,
            String rawPassword, boolean active, boolean deleted, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setCompany(company);
            user.setName(name);
            user.setSurname(surname);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setActive(active);
            user.setDeleted(deleted);
            user.setRoles(new ArrayList<>(Collections.singletonList(role)));
            return userRepository.save(user);
        });
    }

    private void initializeRoleTemplates(Company company, Map<String, Module> modules) {
        if (roleTemplateRepository.countByCompanyId(company.getId()) > 0) {
            log.info("Role templates already exist for company {}, skipping...", company.getName());
            return;
        }

        // Administrator — Bütün sistemə tam giriş
        createRoleTemplate(company, "Administrator", "Bütün sistemə tam giriş", RoleStatus.ACTIVE, modules,
                Map.ofEntries(
                        Map.entry("INVOICE", ModuleRole.SUPER_ADMIN),
                        Map.entry("PARTNER", ModuleRole.SUPER_ADMIN),
                        Map.entry("INVENTORY", ModuleRole.SUPER_ADMIN),
                        Map.entry("HR", ModuleRole.SUPER_ADMIN),
                        Map.entry("REPORT", ModuleRole.SUPER_ADMIN),
                        Map.entry("MANUFACTURING", ModuleRole.SUPER_ADMIN),
                        Map.entry("SERVICE", ModuleRole.SUPER_ADMIN),
                        Map.entry("FINANCE", ModuleRole.SUPER_ADMIN),
                        Map.entry("CONSTRUCTION", ModuleRole.SUPER_ADMIN),
                        Map.entry("SECURITY", ModuleRole.SUPER_ADMIN),
                        Map.entry("DASHBOARD", ModuleRole.SUPER_ADMIN),
                        Map.entry("FOREIGN_PURCHASE", ModuleRole.SUPER_ADMIN),
                        Map.entry("NOTIFICATION", ModuleRole.SUPER_ADMIN),
                        Map.entry("SALES", ModuleRole.SUPER_ADMIN),
                        Map.entry("PURCHASING", ModuleRole.SUPER_ADMIN)));

        // Maliyyəçi — Maliyyə modullarına giriş
        createRoleTemplate(company, "Maliyyəçi", "Maliyyə modullarına giriş", RoleStatus.ACTIVE, modules,
                Map.of(
                        "FINANCE", ModuleRole.ADMIN,
                        "INVOICE", ModuleRole.ADMIN,
                        "FOREIGN_PURCHASE", ModuleRole.VIEWER,
                        "REPORT", ModuleRole.VIEWER,
                        "DASHBOARD", ModuleRole.VIEWER,
                        "SALES", ModuleRole.VIEWER,
                        "PURCHASING", ModuleRole.VIEWER));

        log.info("Role templates initialized: Administrator and Maliyyəçi for company {}", company.getName());
    }

    private void createRoleTemplate(Company company, String name, String description,
            RoleStatus status, Map<String, Module> modules,
            Map<String, ModuleRole> permissions) {
        RoleTemplate template = new RoleTemplate();
        template.setName(name);
        template.setDescription(description);
        template.setStatus(status);
        template.setCompany(company);
        template = roleTemplateRepository.save(template);

        List<RoleTemplatePermission> permList = new ArrayList<>();
        for (Map.Entry<String, ModuleRole> entry : permissions.entrySet()) {
            Module module = modules.get(entry.getKey());
            if (module != null) {
                RoleTemplatePermission perm = new RoleTemplatePermission();
                perm.setRoleTemplate(template);
                perm.setModule(module);
                perm.setAccessLevel(entry.getValue());
                permList.add(perm);
            }
        }
        roleTemplatePermissionRepository.saveAll(permList);
    }

    private void initializeTwoFactorSettings(Company company) {
        if (twoFactorSettingRepository.findByCompanyId(company.getId()).isPresent())
            return;
        TwoFactorSetting settings = new TwoFactorSetting();
        settings.setCompany(company);
        settings.setEnabled(false);
        twoFactorSettingRepository.save(settings);
    }

    private void initializeBackupSettings(Company company) {
        if (backupSettingRepository.findByCompanyId(company.getId()).isPresent())
            return;
        BackupSetting settings = new BackupSetting();
        settings.setCompany(company);
        settings.setAutoBackupEnabled(true);
        backupSettingRepository.save(settings);
    }

    private void initializeTestHRDetails(Company company) {
        if (hrEmployeeRepository.countByCompanyIdAndDeletedFalse(company.getId()) > 0)
            return;

        HREmployee hrEmp = new HREmployee();
        hrEmp.setFirstName("Elvin");
        hrEmp.setLastName("Qasımov");
        hrEmp.setEmail("elvin.q@test.com");
        hrEmp.setPosition("İstehsalat Müdiri");
        hrEmp.setStatus(EmploymentStatus.ACTIVE);
        hrEmp.setHireDate(LocalDate.now().minusMonths(6));
        hrEmp.setCompany(company);
        hrEmp = hrEmployeeRepository.save(hrEmp);

        Contract contract = new Contract();
        contract.setEmployee(hrEmp);
        contract.setContractType(ContractType.FULL_TIME);
        contract.setSalary(new BigDecimal("2800.00"));
        contract.setStartDate(hrEmp.getHireDate());
        contract.setActive(true);
        contractRepository.save(contract);

        Payroll payroll = new Payroll();
        payroll.setEmployee(hrEmp);
        payroll.setPeriod(YearMonth.now().minusMonths(1));
        payroll.setBaseSalary(contract.getSalary());
        payroll.setBonusAmount(new BigDecimal("200.00"));
        payroll.setTaxAmount(new BigDecimal("420.00"));
        payroll.setTotalDeductions(BigDecimal.ZERO);
        payroll.setPaid(true);
        payrollRepository.save(payroll);

        Deduction deduction = new Deduction();
        deduction.setEmployee(hrEmp);
        deduction.setType(DeductionType.ADVANCE_PAYMENT);
        deduction.setAmount(new BigDecimal("100.00"));
        deduction.setReason("Avans ödənişi");
        deduction.setStartDate(LocalDate.now());
        deduction.setActive(true);
        deductionRepository.save(deduction);

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(hrEmp);
        leave.setStartDate(LocalDate.now().plusDays(10));
        leave.setEndDate(LocalDate.now().plusDays(15));
        leave.setLeaveType(LeaveType.ANNUAL);
        leave.setStatus(ActivityStatus.PENDING);
        leaveRequestRepository.save(leave);
    }

    private void initializeTestKassa(Company company, User admin) {
        if (!accountRepository.findByCompanyId(company.getId()).isEmpty())
            return;

        Account mainKassa = new Account();
        mainKassa.setName("Əsas Kassa (AZN)");
        mainKassa.setAccountNumber("K-001");
        mainKassa.setBalance(new BigDecimal("5000.00"));
        mainKassa.setCompany(company);
        mainKassa = accountRepository.save(mainKassa);

        TransactionCategory salesCat = new TransactionCategory();
        salesCat.setName("Satış Gəliri");
        salesCat.setType(TransactionType.INCOME);
        salesCat.setCompany(company);
        salesCat = transactionCategoryRepository.save(salesCat);

        Partner partner = partnerRepository.findAllByCompanyId(company.getId()).stream()
                .filter(p -> p.getPartnerType() == PartnerType.CUSTOMER)
                .findFirst().orElse(null);

        FinancialTransaction tx = new FinancialTransaction();
        tx.setAccount(mainKassa);
        tx.setCategory(salesCat);
        tx.setAmount(new BigDecimal("1200.00"));
        tx.setType(TransactionType.INCOME);
        tx.setDescription("Nəqd satışdan gələn məbləğ");
        tx.setTransactionDate(LocalDateTime.now());
        tx.setCompany(company);
        tx.setCreatedBy(admin);
        if (partner != null) {
            tx.setPartner(partner);
            Balance balance = partner.getBalance();
            if (balance != null) {
                balance.setAmount(balance.getAmount().subtract(tx.getAmount()));
                partnerRepository.save(partner);
            }
        }
        financialTransactionRepository.save(tx);
    }

    private void initializeAdvancedManufacturing(Company company) {
        if (indirectExpenseRepository.findByCompanyAndCategory(company.getId(), null).isEmpty()) {
            IndirectExpense expense = new IndirectExpense();
            expense.setName("Elektrik xərci (İstehsalat)");
            expense.setAmount(new BigDecimal("350.00"));
            expense.setCurrency(CurrencyType.AZN);
            expense.setCategory(ExpenseCategory.UTILITIES);
            expense.setExpenseDate(new Date());
            expense.setCompany(company);
            indirectExpenseRepository.save(expense);
        }
    }

    private void initializeTestInvoices(Company company) {
        if (!invoiceRepository.findAllByCompany(company).isEmpty())
            return;

        Partner customer = partnerRepository.findAllByCompanyId(company.getId()).stream()
                .filter(p -> p.getPartnerType() == PartnerType.CUSTOMER)
                .findFirst().orElse(null);

        Product laptop = productRepository.findAllByCompany_Id(company.getId()).stream().findFirst().orElse(null);

        if (customer != null && laptop != null) {
            Invoice inv = new Invoice();
            inv.setInvoiceNumber("INV-LOCAL-001");
            inv.setInvoiceType(InvoiceType.SALES);
            inv.setStatus(InvoiceStatus.PAID);
            inv.setInvoiceDate(LocalDate.now());
            inv.setCurrency("AZN");
            inv.setPartner(customer);
            inv.setCompany(company);

            InvoiceItem item = new InvoiceItem();
            item.setName(laptop.getName());
            item.setUnitPrice(laptop.getPrice() != null ? laptop.getPrice() : new BigDecimal("1000.00"));
            item.setQuantity(2);
            item.setProduct(laptop);
            item.setInvoice(inv);
            item.calculateLineTotal();

            inv.addItem(item);
            invoiceRepository.save(inv);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ƏLAVƏ SEED DATA - Bütün endpoint-lər üçün
    // ═══════════════════════════════════════════════════════════

    private void initializeTestForeignCurrencyPurchases(Company company) {
        if (!foreignCurrencyPurchaseRepository.findAllByCompanyId(company.getId()).isEmpty())
            return;

        Currency usd = currencyRepository.findByCode("USD").orElse(null);
        Currency eur = currencyRepository.findByCode("EUR").orElse(null);

        if (usd != null) {
            ForeignCurrencyPurchase purchase1 = new ForeignCurrencyPurchase();
            purchase1.setSupplierName("Global Tech Supplies");
            purchase1.setAmount(new BigDecimal("5000.00"));
            purchase1.setCurrency(usd);
            purchase1.setExchangeRate(usd.getCurrentRate());
            purchase1.setConvertedAmount(new BigDecimal("5000.00").multiply(usd.getCurrentRate()));
            purchase1.setExchangeRateDifference(BigDecimal.ZERO);
            purchase1.setPurchaseDate(LocalDate.now().minusDays(15));
            purchase1.setExpenseCategory("Avadanlıq alışı");
            purchase1.setCompany(company);
            purchase1.setNotes("Server avadanlığı alışı - USD hesablaşma");
            foreignCurrencyPurchaseRepository.save(purchase1);
        }

        if (eur != null) {
            ForeignCurrencyPurchase purchase2 = new ForeignCurrencyPurchase();
            purchase2.setSupplierName("Euro Parts GmbH");
            purchase2.setAmount(new BigDecimal("3200.00"));
            purchase2.setCurrency(eur);
            purchase2.setExchangeRate(eur.getCurrentRate());
            purchase2.setConvertedAmount(new BigDecimal("3200.00").multiply(eur.getCurrentRate()));
            purchase2.setExchangeRateDifference(new BigDecimal("12.50"));
            purchase2.setPurchaseDate(LocalDate.now().minusDays(5));
            purchase2.setExpenseCategory("Xammal idxalı");
            purchase2.setCompany(company);
            purchase2.setNotes("Ehtiyat hissələri idxalı - EUR hesablaşma");
            foreignCurrencyPurchaseRepository.save(purchase2);
        }

        log.info("Foreign currency purchases initialized for company {}", company.getName());
    }

    private void initializeTestHRActivities(Company company) {
        if (!hrActivityRepository.findAll().isEmpty())
            return;

        HREmployee hrEmp = hrEmployeeRepository.findAll().stream()
                .filter(e -> e.getCompany() != null && e.getCompany().getId().equals(company.getId()))
                .findFirst().orElse(null);

        if (hrEmp != null) {
            HRActivity activity1 = new HRActivity();
            activity1.setEmployee(hrEmp);
            activity1.setActivityType("HIRE");
            activity1.setStatus(ActivityStatus.COMPLETED);
            hrActivityRepository.save(activity1);

            HRActivity activity2 = new HRActivity();
            activity2.setEmployee(hrEmp);
            activity2.setActivityType("PROMOTION");
            activity2.setStatus(ActivityStatus.COMPLETED);
            hrActivityRepository.save(activity2);

            HRActivity activity3 = new HRActivity();
            activity3.setEmployee(hrEmp);
            activity3.setActivityType("TRAINING");
            activity3.setStatus(ActivityStatus.PENDING);
            hrActivityRepository.save(activity3);

            log.info("HR activities initialized for employee {}", hrEmp.getFirstName());
        }
    }

    private void initializeTestConstructionMaterials(Company company) {
        if (!constructionProjectMaterialRepository.findAll().isEmpty())
            return;

        ConstructionProject project = constructionProjectRepository.findAllByCompany_Id(company.getId())
                .stream().findFirst().orElse(null);
        Warehouse mainWarehouse = warehouseRepository.findByNameAndCompanyId(MAIN_WAREHOUSE_NAME, company.getId())
                .orElse(null);

        if (project != null && mainWarehouse != null) {
            List<Product> products = productRepository.findAllByCompany_Id(company.getId());
            for (Product product : products) {
                WarehouseProduct wp = warehouseProductRepository.findByProductAndWarehouse(product, mainWarehouse);
                if (wp != null && wp.getQuantity() != null && wp.getQuantity() > 0) {
                    ConstructionProjectMaterial material = new ConstructionProjectMaterial();
                    material.setProject(project);
                    material.setWarehouseProduct(wp);
                    material.setProduct(product);
                    material.setWarehouse(mainWarehouse);
                    material.setCompany(company);
                    material.setRequiredQuantity(10);
                    material.setUsedQuantity(3);
                    material.setLastUsedDate(LocalDate.now().minusDays(2));
                    material.setNote("Tikinti layihəsi üçün ayrılmış material");
                    constructionProjectMaterialRepository.save(material);
                    break; // Yalnız 1 material əlavə edirik
                }
            }
            log.info("Construction project materials initialized for project {}", project.getName());
        }
    }

    private void initializeTestFinanceExtra(Company company, User admin) {
        // Əgər artıq 2+ kateqoriya varsa, skip
        if (transactionCategoryRepository.findAll().stream()
                .filter(c -> c.getCompany() != null && c.getCompany().getId().equals(company.getId()))
                .count() >= 2)
            return;

        TransactionCategory expenseCat = new TransactionCategory();
        expenseCat.setName("Ofis Xərcləri");
        expenseCat.setType(TransactionType.EXPENSE);
        expenseCat.setCompany(company);
        expenseCat = transactionCategoryRepository.save(expenseCat);

        Account mainKassa = accountRepository.findByCompanyId(company.getId()).stream().findFirst().orElse(null);
        if (mainKassa != null) {
            FinancialTransaction expenseTx = new FinancialTransaction();
            expenseTx.setAccount(mainKassa);
            expenseTx.setCategory(expenseCat);
            expenseTx.setAmount(new BigDecimal("450.00"));
            expenseTx.setType(TransactionType.EXPENSE);
            expenseTx.setDescription("Ofis ləvazimatları alışı");
            expenseTx.setTransactionDate(LocalDateTime.now().minusDays(3));
            expenseTx.setCompany(company);
            expenseTx.setCreatedBy(admin);
            financialTransactionRepository.save(expenseTx);

            FinancialTransaction incomeTx2 = new FinancialTransaction();
            incomeTx2.setAccount(mainKassa);
            incomeTx2.setCategory(transactionCategoryRepository.findAll().stream()
                    .filter(c -> c.getType() == TransactionType.INCOME && c.getCompany() != null
                            && c.getCompany().getId().equals(company.getId()))
                    .findFirst().orElse(expenseCat));
            incomeTx2.setAmount(new BigDecimal("2500.00"));
            incomeTx2.setType(TransactionType.INCOME);
            incomeTx2.setDescription("Xidmət göstərişindən gəlir");
            incomeTx2.setTransactionDate(LocalDateTime.now().minusDays(1));
            incomeTx2.setCompany(company);
            incomeTx2.setCreatedBy(admin);
            financialTransactionRepository.save(incomeTx2);
        }

        log.info("Finance extra data initialized for company {}", company.getName());
    }

    private void initializeTestInstallments(Company company) {
        if (!installmentRepository.findAll().isEmpty())
            return;

        paymentPlanRepository.findAllByCompanyId(company.getId(), org.springframework.data.domain.Pageable.unpaged())
                .stream().findFirst().ifPresent(plan -> {
                    BigDecimal totalAmount = plan.getTotalAmount();
                    int numberOfInstallments = 4;
                    BigDecimal installmentAmount = totalAmount.divide(BigDecimal.valueOf(numberOfInstallments), 2,
                            java.math.RoundingMode.HALF_UP);

                    for (int i = 0; i < numberOfInstallments; i++) {
                        Installment inst = new Installment();
                        inst.setAmount(installmentAmount);
                        inst.setDueDate(LocalDate.now().plusMonths(i + 1));
                        inst.setPaymentPlan(plan);

                        if (i == 0) {
                            inst.setPaid(true);
                            inst.setPaymentDate(LocalDate.now().minusDays(5));
                        } else {
                            inst.setPaid(false);
                        }

                        installmentRepository.save(inst);
                    }
                    log.info("Installments initialized for payment plan: {}", plan.getName());
                });
    }

    private void initializeNotificationSettings(Company company) {
        if (notificationSettingRepository.findAll().stream()
                .anyMatch(ns -> ns.getCompany() != null && ns.getCompany().getId().equals(company.getId())))
            return;

        NotificationSetting settings = new NotificationSetting();
        settings.setCompany(company);
        settings.setSmsNotifications(true);
        settings.setEmailNotifications(true);
        settings.setPushNotifications(false);
        notificationSettingRepository.save(settings);
        log.info("Notification settings initialized for company {}", company.getName());
    }

    private void initializeTestCompanyStock(Company company) {
        if (!companyStockRepository.findAll().isEmpty())
            return;

        for (int i = 6; i >= 0; i--) {
            CompanyStock stock = new CompanyStock();
            stock.setDate(LocalDate.now().minusDays(i));
            stock.setStockCount(500L + (long) (Math.random() * 200));
            stock.setCompany(company);
            companyStockRepository.save(stock);
        }
        log.info("Company stock history initialized for company {}", company.getName());
    }

    private void initializeTestAuditLogs(User admin) {
        if (auditLogRepository.count() > 0)
            return;

        AuditLog log1 = AuditLog.builder()
                .actorEmail(admin.getEmail())
                .action("LOGIN")
                .target("Auth System")
                .ipAddress("192.168.1.10")
                .details("İstifadəçi sisteme daxil oldu")
                .timestamp(LocalDateTime.now().minusHours(5))
                .build();
        auditLogRepository.save(log1);

        AuditLog log2 = AuditLog.builder()
                .actorEmail(admin.getEmail())
                .action("CREATE")
                .target("Partner: Araz Market")
                .ipAddress("192.168.1.10")
                .details("Yeni müştəri yaradıldı")
                .timestamp(LocalDateTime.now().minusHours(4))
                .build();
        auditLogRepository.save(log2);

        AuditLog log3 = AuditLog.builder()
                .actorEmail(admin.getEmail())
                .action("UPDATE")
                .target("Product: Laptop HP ProBook")
                .ipAddress("192.168.1.10")
                .details("Məhsul qiyməti yeniləndi: 1500.00 AZN")
                .timestamp(LocalDateTime.now().minusHours(3))
                .build();
        auditLogRepository.save(log3);

        AuditLog log4 = AuditLog.builder()
                .actorEmail(admin.getEmail())
                .action("CREATE")
                .target("Invoice: INV-LOCAL-001")
                .ipAddress("192.168.1.10")
                .details("Yeni faktura yaradıldı - 3000.00 AZN")
                .timestamp(LocalDateTime.now().minusHours(2))
                .build();
        auditLogRepository.save(log4);

        AuditLog log5 = AuditLog.builder()
                .actorEmail("superadmin@specialservice.com")
                .action("SYSTEM_CONFIG")
                .target("Backup Settings")
                .ipAddress("127.0.0.1")
                .details("Avtomatik backup aktivləşdirildi")
                .timestamp(LocalDateTime.now().minusHours(1))
                .build();
        auditLogRepository.save(log5);

        log.info("Audit logs initialized");
    }
}
