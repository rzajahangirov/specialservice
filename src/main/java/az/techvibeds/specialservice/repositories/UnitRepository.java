package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.UnitCategory;
import az.techvibeds.specialservice.models.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    Optional<Unit> findByName(String name);

    @Query("SELECT u FROM Unit u WHERE u.category = :category OR u.category = az.techvibeds.specialservice.enums.UnitCategory.BOTH")
    List<Unit> findAllByCategoryOrBoth(@Param("category") UnitCategory category);
}
