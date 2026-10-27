package az.techvibeds.specialservice.services.impl;


import az.techvibeds.specialservice.dtos.moduleRole.*;
import az.techvibeds.specialservice.enums.ModuleRole;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.CompanyModule;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.models.UserModuleRole;
import az.techvibeds.specialservice.repositories.CompanyModuleRepository;
import az.techvibeds.specialservice.repositories.UserModuleRoleRepository;
import az.techvibeds.specialservice.repositories.UserRepository;
import az.techvibeds.specialservice.services.UserModuleRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserModuleRoleServiceImpl implements UserModuleRoleService {

    private final UserRepository userRepository;
    private final CompanyModuleRepository companyModuleRepository;
    private final UserModuleRoleRepository userModuleRoleRepository;

    @Override
    @Transactional
    public UserModuleRoleResponse assignOrUpdateRole(UserModuleRoleRequest request) {
        log.info("Assigning module role. userId={}, companyModuleId={}, role={}",
                request.getUserId(), request.getCompanyModuleId(), request.getRole());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> {
                    log.error("User not found. userId={}", request.getUserId());
                    return new ResourceNotFoundException("User", "id", request.getUserId());
                });

        CompanyModule companyModule = companyModuleRepository.findById(request.getCompanyModuleId())
                .orElseThrow(() -> {
                    log.error("CompanyModule not found. companyModuleId={}", request.getCompanyModuleId());
                    return new ResourceNotFoundException("CompanyModule", "id", request.getCompanyModuleId());
                });


        if (user.getCompany() != null && companyModule.getCompany() != null && 
            !user.getCompany().getId().equals(companyModule.getCompany().getId())) {
            log.error("User does not belong to the company of this module. userId={}, companyModuleId={}",
                    user.getId(), companyModule.getId());
            throw new BusinessException("User does not belong to the company associated with this module.");
        } else if (user.getCompany() == null && !user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_GLOBAL_ADMIN"))) {
             // If they are not global admin and have no company, they shouldn't be here anyway, but for safety:
             throw new BusinessException("User has no company association.");
        }


        if (Boolean.FALSE.equals(companyModule.getActive())) {
            log.error("CompanyModule is not active. companyModuleId={}", companyModule.getId());
            throw new BusinessException("Cannot assign role to an inactive module.");
        }


        UserModuleRole userModuleRole = userModuleRoleRepository
                .findByUserIdAndCompanyModuleId(user.getId(), companyModule.getId())
                .orElse(new UserModuleRole());

        userModuleRole.setUser(user);
        userModuleRole.setCompanyModule(companyModule);
        userModuleRole.setRole(request.getRole());

        UserModuleRole saved = userModuleRoleRepository.save(userModuleRole);
        log.info("Module role saved successfully. userModuleRoleId={}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyModuleResponse> getCompanyModulesWithUserRoles(Long userId) {
        log.info("Fetching company modules with user roles. userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found. userId={}", userId);
                    return new ResourceNotFoundException("User", "id", userId);
                });

        List<CompanyModule> companyModules;
        if (user.getCompany() != null) {
            companyModules = companyModuleRepository.findAllByCompanyId(user.getCompany().getId());
        } else {
            // Super Admin might not have company modules, return empty or all? 
            // Usually Super Admin is managed separately, let's return empty to avoid crashes.
            companyModules = List.of();
        }


        Map<Long, UserModuleRole> userRoleMap = userModuleRoleRepository
                .findAllByUserIdWithDetails(userId)
                .stream()
                .collect(Collectors.toMap(
                        umr -> umr.getCompanyModule().getId(),
                        umr -> umr
                ));

        List<CompanyModuleResponse> result = companyModules.stream()
                .map(cm -> {
                    UserModuleRole umr = userRoleMap.get(cm.getId());
                    ModuleRoleInfo roleInfo = umr != null
                            ? ModuleRoleInfo.builder()
                            .userModuleRoleId(umr.getId())
                            .role(umr.getRole())
                            .build()
                            : null;

                    return CompanyModuleResponse.builder()
                            .companyModuleId(cm.getId())
                            .moduleId(cm.getModule().getId())
                            .moduleName(cm.getModule().getName())
                            .active(cm.getActive())
                            .startDate(cm.getStartDate())
                            .endDate(cm.getEndDate())
                            .userRole(roleInfo)
                            .build();
                })
                .collect(Collectors.toList());

        log.info("Returning {} company modules for userId={}", result.size(), userId);
        return result;
    }

    @Override
    @Transactional
    public void assignAllCompanyModulesToUser(User user, ModuleRole role) {
        log.info("Starting automatic module assignment for user: {} (ID: {}). Target Role: {}",
                user.getEmail(), user.getId(), role);

        if (user.getCompany() == null) {
            log.warn("User ID: {} has no company. Automatic assignment skipped.", user.getId());
            return;
        }
        List<CompanyModule> companyModules = companyModuleRepository
                .findAllByCompanyIdAndActiveTrue(user.getCompany().getId());

        if (companyModules.isEmpty()) {
            log.warn("No active modules found for company ID: {}. No roles assigned.", user.getCompany().getId());
            return;
        }

        List<UserModuleRole> userModuleRoles = companyModules.stream().map(cm -> {
            UserModuleRole umr = new UserModuleRole();
            umr.setUser(user);
            umr.setCompanyModule(cm);
            umr.setRole(role);
            return umr;
        }).toList();

        userModuleRoleRepository.saveAll(userModuleRoles);
        log.info("Successfully assigned {} modules to user ID: {}", userModuleRoles.size(), user.getId());
    }

    @Override
    @Transactional
    public void syncUserModuleRoles(SyncUserRolesRequest request) {
        log.info("Syncing module roles for user ID: {}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> {
                    log.error("Sync failed: User not found with ID: {}", request.getUserId());
                    return new ResourceNotFoundException("User", "id", request.getUserId());
                });

        userModuleRoleRepository.deleteByUserId(user.getId());
        log.debug("Old permissions cleared for user ID: {}", user.getId());

        if (request.getAssignments() != null && !request.getAssignments().isEmpty()) {
            List<UserModuleRole> newRoles = request.getAssignments().stream()
                    .map(dto -> {
                        CompanyModule cm = companyModuleRepository.findById(dto.getCompanyModuleId())
                                .orElseThrow(() -> {
                                    log.error("Module ID {} not found during sync for user {}",
                                            dto.getCompanyModuleId(), user.getId());
                                    return new ResourceNotFoundException("Module", "id", dto.getCompanyModuleId());
                                });

                        UserModuleRole umr = new UserModuleRole();
                        umr.setUser(user);
                        umr.setCompanyModule(cm);
                        umr.setRole(dto.getRole());
                        return umr;
                    }).toList();

            userModuleRoleRepository.saveAll(newRoles);
            log.info("Successfully synced {} new roles for user ID: {}", newRoles.size(), user.getId());
        } else {
            log.info("No assignments provided. User ID: {} now has zero module permissions.", user.getId());
        }
    }

    @Override
    public List<CompanyModuleSummaryResponse> getCompanyModulesSummary(String adminEmail) {
        User admin = findUserByEmail(adminEmail);
        if (admin.getCompany() == null) {
            return List.of();
        }
        Long companyId = admin.getCompany().getId();

        List<CompanyModule> companyModules = companyModuleRepository.findAllByCompanyId(companyId);

        return companyModules.stream().map(cm -> {
            Integer count = userModuleRoleRepository.countByCompanyModuleId(cm.getId());

            return CompanyModuleSummaryResponse.builder()
                    .companyModuleId(cm.getId())
                    .moduleName(cm.getModule().getName())
                    .userCount(count)
                    .active(cm.getActive())
                    .build();
        }).toList();
    }

    @Override
    public List<ModuleUserResponse> getUsersByModule(Long companyModuleId, String email) {
        log.info("Fetching users for company module ID: {}", companyModuleId);

        User admin = findUserByEmail(email);
        CompanyModule cm = companyModuleRepository.findById(companyModuleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", companyModuleId));

        if (admin.getCompany() != null && !cm.getCompany().getId().equals(admin.getCompany().getId())) {
            throw new AccessDeniedException("Access denied to this module's user list.");
        } else if (admin.getCompany() == null && !admin.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_GLOBAL_ADMIN"))) {
             throw new AccessDeniedException("Access denied to this module's user list.");
        }

        List<UserModuleRole> userRoles = userModuleRoleRepository.findByCompanyModuleId(companyModuleId);

        return userRoles.stream().map(umr -> ModuleUserResponse.builder()
                .userId(umr.getUser().getId())
                .fullName(umr.getUser().getName() + " " + umr.getUser().getSurname())
                .email(umr.getUser().getEmail())
                .role(umr.getRole())
                .active(umr.getUser().getActive())
                .build()).toList();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(()->new ResourceNotFoundException("User", "email", email));
    }


    private UserModuleRoleResponse mapToResponse(UserModuleRole umr) {
        return UserModuleRoleResponse.builder()
                .id(umr.getId())
                .userId(umr.getUser().getId())
                .userFullName(umr.getUser().getName() + " " + umr.getUser().getSurname())
                .companyModuleId(umr.getCompanyModule().getId())
                .moduleName(umr.getCompanyModule().getModule().getName())
                .role(umr.getRole())
                .build();
    }
}
