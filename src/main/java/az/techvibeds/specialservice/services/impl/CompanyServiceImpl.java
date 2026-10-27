package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.company.CompanyCreateDto;
import az.techvibeds.specialservice.dtos.company.CompanyReadDto;
import az.techvibeds.specialservice.dtos.company.CompanyUpdateDto;
import az.techvibeds.specialservice.dtos.module.ModuleReadDto;
import az.techvibeds.specialservice.dtos.user.UserReadDto;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.CompanyModule;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.models.Module;
import az.techvibeds.specialservice.repositories.CompanyModuleRepository;
import az.techvibeds.specialservice.repositories.CompanyRepository;
import az.techvibeds.specialservice.repositories.ModuleRepository;
import az.techvibeds.specialservice.repositories.SubscriptionRepository;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final ModelMapper modelMapper;
    private final CompanyRepository companyRepository;
    private final UserService userService;
    private final ModuleRepository moduleRepository;
    private final CompanyModuleRepository companyModuleRepository;
    private final az.techvibeds.specialservice.repositories.UserModuleRoleRepository userModuleRoleRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public Company findByUserEmail(String name) {
        User findUser = userService.findByEmail(name);
        // Birbaşa User-in company field-ini istifadə edirik
        // findByUsers_Id bəzən sinkron olmayan əlaqə səbəbi ilə null qaytara bilir
        Company company = findUser.getCompany();
        return company;
    }
    @Override
    @Transactional
    public CompanyReadDto create(CompanyCreateDto createDto) {
        Company company = new Company();
        company.setName(createDto.getName());
        company.setExpressDate(createDto.getExpressDate());

        Company savedCompany = companyRepository.save(company);

        if (createDto.getModuleIds() != null && !createDto.getModuleIds().isEmpty()) {
            List<Module> modules = createDto.getModuleIds().stream()
                    .map(id -> moduleRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Module", "id", id)))
                    .toList();

            List<CompanyModule> companyModules = modules.stream()
                    .map(module -> {
                        CompanyModule cm = new CompanyModule();
                        cm.setCompany(savedCompany);
                        cm.setModule(module);
                        return cm;
                    })
                    .toList();

            companyModuleRepository.saveAll(companyModules);
            savedCompany.setCompanyModules(companyModules);
        }

        return mapToReadDto(savedCompany);
    }

    @Override
    @Transactional
    public CompanyReadDto update(CompanyUpdateDto updateDto) {
        Company company = companyRepository.findById(updateDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", updateDto.getId()));
        if (updateDto.getName() != null) company.setName(updateDto.getName());
        if (updateDto.getExpressDate() != null) company.setExpressDate(updateDto.getExpressDate());
        if (updateDto.getModuleIds() != null) {
            List<CompanyModule> existingModules = companyModuleRepository.findAllByCompanyId(company.getId());
            java.util.Set<Long> existingModuleIds = existingModules.stream()
                    .map(cm -> cm.getModule().getId())
                    .collect(Collectors.toSet());
            java.util.Set<Long> newModuleIds = new java.util.HashSet<>(updateDto.getModuleIds());
            
            // Silinməli olan modullar
            List<CompanyModule> toDelete = existingModules.stream()
                    .filter(cm -> !newModuleIds.contains(cm.getModule().getId()))
                    .collect(Collectors.toList());
                    
            if (!toDelete.isEmpty()) {
                for (CompanyModule cm : toDelete) {
                    List<az.techvibeds.specialservice.models.UserModuleRole> umrs = userModuleRoleRepository.findByCompanyModuleId(cm.getId());
                    userModuleRoleRepository.deleteAll(umrs);
                    companyModuleRepository.delete(cm);
                }
            }
            
            // Əlavə edilməli olanlar
            List<Long> toAddIds = newModuleIds.stream()
                    .filter(id -> !existingModuleIds.contains(id))
                    .collect(Collectors.toList());
                    
            List<CompanyModule> newCompanyModules = toAddIds.stream()
                    .map(id -> moduleRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Module", "id", id)))
                    .map(module -> {
                        CompanyModule cm = new CompanyModule();
                        cm.setCompany(company);
                        cm.setModule(module);
                        cm.setActive(true);
                        cm.setStartDate(new java.util.Date());
                        return cm;
                    })
                    .collect(Collectors.toList());
            companyModuleRepository.saveAll(newCompanyModules);
            
            // Assign SUPER_ADMIN role for the NEW modules to company admins automatically
            if (!newCompanyModules.isEmpty() && company.getUsers() != null) {
                List<User> companyAdmins = company.getUsers().stream()
                        .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("USER_ADMIN")))
                        .collect(Collectors.toList());

                List<az.techvibeds.specialservice.models.UserModuleRole> newAdminRoles = new ArrayList<>();
                for (User adminUser : companyAdmins) {
                    for (CompanyModule cm : newCompanyModules) {
                        az.techvibeds.specialservice.models.UserModuleRole umr = new az.techvibeds.specialservice.models.UserModuleRole();
                        umr.setUser(adminUser);
                        umr.setCompanyModule(cm);
                        umr.setRole(az.techvibeds.specialservice.enums.ModuleRole.SUPER_ADMIN);
                        newAdminRoles.add(umr);
                    }
                }
                if (!newAdminRoles.isEmpty()) {
                    userModuleRoleRepository.saveAll(newAdminRoles);
                }
            }
            
            List<CompanyModule> finalModules = new ArrayList<>(existingModules);
            finalModules.removeAll(toDelete);
            finalModules.addAll(newCompanyModules);
            company.setCompanyModules(finalModules);
        }
        Company saved = companyRepository.save(company);
        return mapToReadDto(saved);
    }


    @Override
    public void delete(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", id));
        companyRepository.delete(company);
    }

    @Override
    public CompanyReadDto getById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", id));
        return mapToReadDto(company);
    }

    @Override
    public List<CompanyReadDto> getAll(String email) {
        User user = userService.findByEmail(email);
        boolean isGlobalAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_GLOBAL_ADMIN"));

        if (isGlobalAdmin) {
            return companyRepository.findAll().stream()
                    .map(this::mapToReadDto)
                    .collect(Collectors.toList());
        } else {
            Company company = user.getCompany();
            if (company != null) {
                return List.of(mapToReadDto(company));
            } else {
                return List.of();
            }
        }
    }

    @Override
    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    private CompanyReadDto mapToReadDto(Company company) {
        CompanyReadDto dto = new CompanyReadDto();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setExpressDate(company.getExpressDate());
        if (company.getCompanyModules() != null) {
            dto.setModules(company.getCompanyModules().stream()
                    .map(m -> ModuleReadDto.builder().id(m.getModule().getId()).name(m.getModule().getName()).build())
                    .collect(Collectors.toSet()));
        }
        if (company.getUsers() != null) {
            List<UserReadDto> userReadDtoList = new ArrayList<>();
            for (User user : company.getUsers()) {
                UserReadDto userReadDto = modelMapper.map(user, UserReadDto.class);
                userReadDto.setUsername(user.getName() + " " + user.getSurname());
                userReadDtoList.add(userReadDto);
            }
            dto.setUsers(userReadDtoList);
        }
        else {
            dto.setUsers(new ArrayList<>());
        }
        
        subscriptionRepository.findFirstByCompanyIdOrderByEndDateDesc(company.getId())
                .ifPresentOrElse(
                        sub -> dto.setSubscriptionFrozen(sub.getFrozen()),
                        () -> dto.setSubscriptionFrozen(false)
                );

        // Users and Products intentionally omitted for brevity/not required for CRUD basics
        return dto;
    }
}
