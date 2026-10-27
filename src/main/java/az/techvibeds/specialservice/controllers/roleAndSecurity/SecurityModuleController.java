package az.techvibeds.specialservice.controllers.roleAndSecurity;

import az.techvibeds.specialservice.dtos.auth.AdminPasswordChangeDto;
import az.techvibeds.specialservice.dtos.auth.UserResponseDto;
import az.techvibeds.specialservice.dtos.backup.BackupSettingResponse;
import az.techvibeds.specialservice.dtos.moduleRole.CompanyModuleSummaryResponse;
import az.techvibeds.specialservice.dtos.moduleRole.ModuleUserResponse;
import az.techvibeds.specialservice.dtos.roleTemplate.*;
import az.techvibeds.specialservice.dtos.twoFactor.TwoFactorSettingRequest;
import az.techvibeds.specialservice.dtos.twoFactor.TwoFactorSettingResponse;
import az.techvibeds.specialservice.dtos.user.UserDetailResponse;
import az.techvibeds.specialservice.dtos.user.UserListResponse;
import az.techvibeds.specialservice.dtos.user.UserStatusRequest;
import az.techvibeds.specialservice.dtos.user.UserUpdateDto;
import az.techvibeds.specialservice.services.BackupSettingService;
import az.techvibeds.specialservice.services.RoleTemplateService;
import az.techvibeds.specialservice.services.TwoFactorSettingService;
import az.techvibeds.specialservice.services.UserModuleRoleService;
import az.techvibeds.specialservice.services.UserService;
import jakarta.validation.Valid;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
@Slf4j
public class SecurityModuleController {

    private final UserService userService;
    private final UserModuleRoleService userModuleRoleService;
    private final RoleTemplateService roleTemplateService;
    private final TwoFactorSettingService twoFactorSettingService;
    private final BackupSettingService backupSettingService;

    // ==================== User Management ====================

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDto updateDto,
            Principal principal) {
        log.info("REST request to update User : {}", id);
        return ResponseEntity.ok(userService.updateUser(id, updateDto, principal.getName()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<Void> toggleStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest statusRequest,
            Principal principal) {
        log.info("REST request to change User status : {} to {}", id, statusRequest.getActive());
        userService.toggleUserStatus(id, statusRequest.getActive(), principal.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Principal principal) {
        log.info("REST request to delete User : {}", id);
        userService.deleteUser(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<Page<UserListResponse>> getAllUsers(
            Principal principal,
            @org.springdoc.core.annotations.ParameterObject 
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(userService.getCompanyUsersForDashboard(principal.getName(), pageable));
    }

    @GetMapping("/{id}/details")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<UserDetailResponse> getUserDetails(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(userService.getUserDetails(id, principal.getName()));
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<List<UserListResponse>> getDeletedUsers(Principal principal) {
        log.info("REST request to get all deleted users");
        return ResponseEntity.ok(userService.getDeletedCompanyUsers(principal.getName()));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<Void> restoreUser(@PathVariable Long id) {
        log.info("REST request to restore User : {}", id);
        userService.restoreUser(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminPasswordChangeDto passwordDto,
            Principal principal) {

        log.info("REST request to reset password for user ID: {}", id);
        userService.resetUserPassword(id, passwordDto.getNewPassword(), principal.getName());

        return ResponseEntity.noContent().build();
    }

    // ==================== Module Access ====================

    @GetMapping("/my-modules")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<List<CompanyModuleSummaryResponse>> getMyModules(Principal principal) {
        return ResponseEntity.ok(userModuleRoleService.getCompanyModulesSummary(principal.getName()));
    }

    @GetMapping("/modules/{companyModuleId}/users")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<List<ModuleUserResponse>> getModuleUsers(@PathVariable Long companyModuleId, Principal principal) {
        return ResponseEntity.ok(userModuleRoleService.getUsersByModule(companyModuleId, principal.getName()));
    }

    // ==================== Role Templates ====================

    @PostMapping("/role-templates")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<RoleTemplateDetailResponse> createRoleTemplate(
            @Valid @RequestBody RoleTemplateRequest request,
            Principal principal) {
        log.info("REST request to create Role Template: {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleTemplateService.createRoleTemplate(request, principal.getName()));
    }

    @PutMapping("/role-templates/{id}")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<RoleTemplateDetailResponse> updateRoleTemplate(
            @PathVariable Long id,
            @Valid @RequestBody RoleTemplateRequest request,
            Principal principal) {
        log.info("REST request to update Role Template: {}", id);
        return ResponseEntity.ok(roleTemplateService.updateRoleTemplate(id, request, principal.getName()));
    }

    @GetMapping("/role-templates")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<List<RoleTemplateResponse>> getAllRoleTemplates(Principal principal) {
        return ResponseEntity.ok(roleTemplateService.getAllRoleTemplates(principal.getName()));
    }

    @GetMapping("/role-templates/{id}")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<RoleTemplateDetailResponse> getRoleTemplate(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(roleTemplateService.getRoleTemplateById(id, principal.getName()));
    }

    @PatchMapping("/role-templates/{id}/toggle-status")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<Void> toggleRoleTemplateStatus(@PathVariable Long id, Principal principal) {
        log.info("REST request to toggle Role Template status: {}", id);
        roleTemplateService.toggleRoleTemplateStatus(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/role-templates/{id}")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<Void> deleteRoleTemplate(@PathVariable Long id, Principal principal) {
        log.info("REST request to delete Role Template: {}", id);
        roleTemplateService.deleteRoleTemplate(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/role-templates/{templateId}/apply/{userId}")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<Void> applyTemplateToUser(
            @PathVariable Long templateId,
            @PathVariable Long userId,
            Principal principal) {
        log.info("REST request to apply template {} to user {}", templateId, userId);
        roleTemplateService.applyTemplateToUser(templateId, userId, principal.getName());
        return ResponseEntity.ok().build();
    }

    // ==================== Two-Factor Authentication ====================

    @GetMapping("/2fa")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<TwoFactorSettingResponse> getTwoFactorSettings(Principal principal) {
        return ResponseEntity.ok(twoFactorSettingService.getSettings(principal.getName()));
    }

    @PatchMapping("/2fa")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<TwoFactorSettingResponse> toggleTwoFactor(
            @Valid @RequestBody TwoFactorSettingRequest request,
            Principal principal) {
        log.info("REST request to toggle 2FA: {}", request.getEnabled());
        return ResponseEntity.ok(twoFactorSettingService.toggleTwoFactor(request, principal.getName()));
    }

    // ==================== Backup Management ====================

    @GetMapping("/backup")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<BackupSettingResponse> getBackupSettings(Principal principal) {
        return ResponseEntity.ok(backupSettingService.getSettings(principal.getName()));
    }

    @PatchMapping("/backup/auto")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<BackupSettingResponse> toggleAutoBackup(
            @RequestParam Boolean enabled,
            Principal principal) {
        log.info("REST request to toggle auto backup: {}", enabled);
        return ResponseEntity.ok(backupSettingService.toggleAutoBackup(enabled, principal.getName()));
    }

    @PostMapping("/backup/trigger")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<BackupSettingResponse> triggerBackupNow(Principal principal) {
        log.info("REST request to trigger manual backup");
        return ResponseEntity.ok(backupSettingService.triggerBackupNow(principal.getName()));
    }
}
