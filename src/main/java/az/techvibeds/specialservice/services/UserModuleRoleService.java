package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.moduleRole.*;
import az.techvibeds.specialservice.enums.ModuleRole;
import az.techvibeds.specialservice.models.User;

import java.util.List;

public interface UserModuleRoleService {

    UserModuleRoleResponse assignOrUpdateRole(UserModuleRoleRequest request);

    List<CompanyModuleResponse> getCompanyModulesWithUserRoles(Long userId);

    void assignAllCompanyModulesToUser(User savedUser, ModuleRole moduleRole);

    void syncUserModuleRoles(SyncUserRolesRequest request);

    List<CompanyModuleSummaryResponse> getCompanyModulesSummary(String name);

    List<ModuleUserResponse> getUsersByModule(Long companyModuleId, String email);
}
