package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.roleTemplate.*;
import az.techvibeds.specialservice.enums.RoleStatus;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.models.Module;
import az.techvibeds.specialservice.repositories.*;
import az.techvibeds.specialservice.services.AuditLogService;
import az.techvibeds.specialservice.services.RoleTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleTemplateServiceImpl implements RoleTemplateService {

    private final RoleTemplateRepository roleTemplateRepository;
    private final RoleTemplatePermissionRepository roleTemplatePermissionRepository;
    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;
    private final CompanyModuleRepository companyModuleRepository;
    private final UserModuleRoleRepository userModuleRoleRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public RoleTemplateDetailResponse createRoleTemplate(RoleTemplateRequest request, String adminEmail) {
        log.info("Creating role template: {} by {}", request.getName(), adminEmail);

        User admin = findUserByEmail(adminEmail);
        Company company = admin.getCompany();

        if (company == null) {
            throw new BusinessException("Qlobal Admin birbaşa rol şablonu yarada bilməz. Şirkət məlumatı mütləqdir.");
        }

        if (roleTemplateRepository.existsByNameAndCompanyId(request.getName(), company.getId())) {
            throw new BusinessException("Bu adda rol şablonu artıq mövcuddur: " + request.getName());
        }

        RoleTemplate template = new RoleTemplate();
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setCompany(company);
        template.setStatus(RoleStatus.ACTIVE);

        // Save template first to get ID
        template = roleTemplateRepository.save(template);

        // Create permissions
        if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
            List<RoleTemplatePermission> permissions = new ArrayList<>();
            for (RoleTemplatePermissionDto permDto : request.getPermissions()) {
                Module module = moduleRepository.findById(permDto.getModuleId())
                        .orElseThrow(() -> new ResourceNotFoundException("Module", "id", permDto.getModuleId()));

                RoleTemplatePermission perm = new RoleTemplatePermission();
                perm.setRoleTemplate(template);
                perm.setModule(module);
                perm.setAccessLevel(permDto.getAccessLevel());
                permissions.add(perm);
            }
            roleTemplatePermissionRepository.saveAll(permissions);
            template.setPermissions(permissions);
        }

        auditLogService.logAction(adminEmail, "ROLE_TEMPLATE_CREATED",
                "RoleTemplate:" + template.getId(),
                "Rol şablonu yaradıldı: " + template.getName());

        log.info("Role template created successfully: id={}", template.getId());
        return mapToDetailResponse(template);
    }

    @Override
    @Transactional
    public RoleTemplateDetailResponse updateRoleTemplate(Long id, RoleTemplateRequest request, String adminEmail) {
        log.info("Updating role template: {} by {}", id, adminEmail);

        User admin = findUserByEmail(adminEmail);
        if (admin.getCompany() == null) {
            throw new BusinessException("Qlobal Admin rol şablonunu birbaşa yeniləyə bilməz.");
        }
        
        RoleTemplate template = findTemplateAndValidateCompany(id, admin);

        // Check duplicate name (excluding current)
        roleTemplateRepository.findByNameAndCompanyId(request.getName(), admin.getCompany().getId())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BusinessException("Bu adda rol şablonu artıq mövcuddur: " + request.getName());
                    }
                });

        template.setName(request.getName());
        template.setDescription(request.getDescription());

        // Replace permissions
        roleTemplatePermissionRepository.deleteAllByRoleTemplateId(id);

        List<RoleTemplatePermission> newPermissions = new ArrayList<>();
        if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
            for (RoleTemplatePermissionDto permDto : request.getPermissions()) {
                Module module = moduleRepository.findById(permDto.getModuleId())
                        .orElseThrow(() -> new ResourceNotFoundException("Module", "id", permDto.getModuleId()));

                RoleTemplatePermission perm = new RoleTemplatePermission();
                perm.setRoleTemplate(template);
                perm.setModule(module);
                perm.setAccessLevel(permDto.getAccessLevel());
                newPermissions.add(perm);
            }
            roleTemplatePermissionRepository.saveAll(newPermissions);
        }
        template.setPermissions(newPermissions);

        roleTemplateRepository.save(template);

        auditLogService.logAction(adminEmail, "ROLE_TEMPLATE_UPDATED",
                "RoleTemplate:" + template.getId(),
                "Rol şablonu yeniləndi: " + template.getName());

        log.info("Role template updated: id={}", id);
        return mapToDetailResponse(template);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleTemplateResponse> getAllRoleTemplates(String adminEmail) {
        User admin = findUserByEmail(adminEmail);
        List<RoleTemplate> templates;
        
        if (admin.getCompany() == null) {
            templates = roleTemplateRepository.findAll();
        } else {
            templates = roleTemplateRepository.findAllByCompanyId(admin.getCompany().getId());
        }

        return templates.stream().map(this::mapToListResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleTemplateDetailResponse getRoleTemplateById(Long id, String adminEmail) {
        User admin = findUserByEmail(adminEmail);
        RoleTemplate template = findTemplateAndValidateCompany(id, admin);
        return mapToDetailResponse(template);
    }

    @Override
    @Transactional
    public void toggleRoleTemplateStatus(Long id, String adminEmail) {
        log.info("Toggling role template status: id={}", id);

        User admin = findUserByEmail(adminEmail);
        RoleTemplate template = findTemplateAndValidateCompany(id, admin);

        RoleStatus newStatus = template.getStatus() == RoleStatus.ACTIVE
                ? RoleStatus.INACTIVE
                : RoleStatus.ACTIVE;
        template.setStatus(newStatus);
        roleTemplateRepository.save(template);

        auditLogService.logAction(adminEmail, "ROLE_TEMPLATE_STATUS_CHANGED",
                "RoleTemplate:" + id,
                "Rol şablonu statusu dəyişdirildi: " + newStatus);

        log.info("Role template status toggled: id={}, newStatus={}", id, newStatus);
    }

    @Override
    @Transactional
    public void deleteRoleTemplate(Long id, String adminEmail) {
        log.info("Deleting role template: id={}", id);

        User admin = findUserByEmail(adminEmail);
        RoleTemplate template = findTemplateAndValidateCompany(id, admin);

        roleTemplateRepository.delete(template);

        auditLogService.logAction(adminEmail, "ROLE_TEMPLATE_DELETED",
                "RoleTemplate:" + id,
                "Rol şablonu silindi: " + template.getName());

        log.info("Role template deleted: id={}", id);
    }

    @Override
    @Transactional
    public void applyTemplateToUser(Long templateId, Long userId, String adminEmail) {
        log.info("Applying template {} to user {} by {}", templateId, userId, adminEmail);

        User admin = findUserByEmail(adminEmail);
        if (admin.getCompany() == null) {
            throw new BusinessException("Qlobal Admin rol şablonunu birbaşa tətbiq edə bilməz.");
        }

        RoleTemplate template = findTemplateAndValidateCompany(templateId, admin);

        if (template.getStatus() == RoleStatus.INACTIVE) {
            throw new BusinessException("Deaktiv rol şablonu istifadəçiyə tətbiq edilə bilməz.");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!targetUser.getCompany().getId().equals(admin.getCompany().getId())) {
            throw new AccessDeniedException("Bu istifadəçi sizin şirkətə aid deyil.");
        }

        // Clear existing module roles for this user
        userModuleRoleRepository.deleteByUserId(userId);

        // Apply template permissions as UserModuleRoles
        List<UserModuleRole> newRoles = new ArrayList<>();
        for (RoleTemplatePermission perm : template.getPermissions()) {
            // Find CompanyModule for this module in user's company
            companyModuleRepository.findByCompanyIdAndModuleId(
                    admin.getCompany().getId(), perm.getModule().getId()
            ).ifPresent(companyModule -> {
                UserModuleRole umr = new UserModuleRole();
                umr.setUser(targetUser);
                umr.setCompanyModule(companyModule);
                umr.setRole(perm.getAccessLevel());
                newRoles.add(umr);
            });
        }

        userModuleRoleRepository.saveAll(newRoles);

        auditLogService.logAction(adminEmail, "ROLE_TEMPLATE_APPLIED",
                "User:" + userId,
                "'" + template.getName() + "' şablonu istifadəçiyə tətbiq edildi: " + targetUser.getEmail());

        log.info("Template {} applied to user {}. {} module roles created.", templateId, userId, newRoles.size());
    }

    // ==================== Helper Methods ====================

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private RoleTemplate findTemplateAndValidateCompany(Long templateId, User admin) {
        RoleTemplate template = roleTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("RoleTemplate", "id", templateId));

        if (admin.getCompany() != null && !template.getCompany().getId().equals(admin.getCompany().getId())) {
            throw new AccessDeniedException("Bu rol şablonuna giriş icazəniz yoxdur.");
        }
        return template;
    }

    private RoleTemplateResponse mapToListResponse(RoleTemplate template) {
        // Count users who have module roles matching this template
        int userCount = countUsersWithTemplate(template);

        return RoleTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .status(template.getStatus())
                .userCount(userCount)
                .build();
    }

    private RoleTemplateDetailResponse mapToDetailResponse(RoleTemplate template) {
        List<RoleTemplatePermissionDto> permDtos = template.getPermissions().stream()
                .map(perm -> RoleTemplatePermissionDto.builder()
                        .moduleId(perm.getModule().getId())
                        .moduleName(perm.getModule().getName())
                        .accessLevel(perm.getAccessLevel())
                        .build())
                .toList();

        int userCount = countUsersWithTemplate(template);

        return RoleTemplateDetailResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .status(template.getStatus())
                .userCount(userCount)
                .createdAt(template.getCreatedAt())
                .permissions(permDtos)
                .build();
    }

    private int countUsersWithTemplate(RoleTemplate template) {
        // Count distinct users who have any module role in this company
        // This is an approximation - counts users with roles in the same modules
        if (template.getPermissions() == null || template.getPermissions().isEmpty()) {
            return 0;
        }

        Long companyId = template.getCompany().getId();
        List<CompanyModule> companyModules = companyModuleRepository.findAllByCompanyId(companyId);

        if (companyModules.isEmpty()) return 0;

        // Get first company module to count users
        return companyModules.stream()
                .mapToInt(cm -> userModuleRoleRepository.countByCompanyModuleId(cm.getId()))
                .max()
                .orElse(0);
    }
}
