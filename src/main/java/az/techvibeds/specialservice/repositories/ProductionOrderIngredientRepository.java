package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.ProductionOrderIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionOrderIngredientRepository extends JpaRepository<ProductionOrderIngredient, Long> {
}
