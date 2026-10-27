package az.techvibeds.specialservice.controllers.auth;

import az.techvibeds.specialservice.dtos.moduleRole.*;
import az.techvibeds.specialservice.dtos.user.CompanyUserResponseDto;
import az.techvibeds.specialservice.services.UserModuleRoleService;
import az.techvibeds.specialservice.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/user-module-roles")
@RequiredArgsConstructor
public class UserModuleRoleController {

    private final UserModuleRoleService userModuleRoleService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<UserModuleRoleResponse> assignOrUpdateRole(
            @Valid @RequestBody UserModuleRoleRequest request) {
        log.info("POST /api/user-module-roles - userId={}, companyModuleId={}",
                request.getUserId(), request.getCompanyModuleId());
        UserModuleRoleResponse response = userModuleRoleService.assignOrUpdateRole(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/users/{userId}/company-modules")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<List<CompanyModuleResponse>> getCompanyModulesWithUserRoles(
            @PathVariable Long userId) {
        log.info("GET /api/user-module-roles/users/{}/company-modules", userId);
        List<CompanyModuleResponse> response = userModuleRoleService.getCompanyModulesWithUserRoles(userId);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/sync-permissions")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<Void> syncPermissions(@RequestBody SyncUserRolesRequest request) {
        log.info("REST request to sync permissions for userId: {}. Assignments count: {}",
                request.getUserId(),
                request.getAssignments() != null ? request.getAssignments().size() : 0);

        userModuleRoleService.syncUserModuleRoles(request);

        log.info("Permissions successfully synced for userId: {}", request.getUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<List<CompanyUserResponseDto>> getMyCompanyUsers(Principal principal) {
        return ResponseEntity.ok(userService.getCompanyUsers(principal.getName()));
    }
}
