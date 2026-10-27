package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Module;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModuleRepository extends JpaRepository<Module, Long> {
    Optional<Module> findByName(String name);
    Optional<Module> findByBackendKey(String backendKey);
    boolean existsByBackendKey(String backendKey);
}

