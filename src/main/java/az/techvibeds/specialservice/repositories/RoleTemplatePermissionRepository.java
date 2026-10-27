package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.RoleTemplatePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleTemplatePermissionRepository extends JpaRepository<RoleTemplatePermission, Long> {

    List<RoleTemplatePermission> findAllByRoleTemplateId(Long roleTemplateId);

    void deleteAllByRoleTemplateId(Long roleTemplateId);
}
