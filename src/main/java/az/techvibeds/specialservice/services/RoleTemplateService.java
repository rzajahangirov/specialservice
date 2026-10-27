package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.roleTemplate.*;

import java.util.List;

public interface RoleTemplateService {

    RoleTemplateDetailResponse createRoleTemplate(RoleTemplateRequest request, String adminEmail);

    RoleTemplateDetailResponse updateRoleTemplate(Long id, RoleTemplateRequest request, String adminEmail);

    List<RoleTemplateResponse> getAllRoleTemplates(String adminEmail);

    RoleTemplateDetailResponse getRoleTemplateById(Long id, String adminEmail);

    void toggleRoleTemplateStatus(Long id, String adminEmail);

    void deleteRoleTemplate(Long id, String adminEmail);

    void applyTemplateToUser(Long templateId, Long userId, String adminEmail);
}
