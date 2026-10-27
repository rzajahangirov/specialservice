package az.techvibeds.specialservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "production_order_items")
public class ProductionOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "manufactured_product_id", referencedColumnName = "id", nullable = false)
    private ManufacturedProduct manufacturedProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_product_id")
    private Product materialProduct;

    @Column(nullable = false)
    private Integer quantity;

    private Integer materialQuantity;

    private Boolean stockApplied = false;

    @Column(precision = 19, scale = 2)
    private java.math.BigDecimal primeCost;

    @OneToMany(mappedBy = "productionOrderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ProductionOrderIngredient> ingredients = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "productionOrderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ProductionOrderExpense> expenses = new java.util.ArrayList<>();

    public void addIngredient(ProductionOrderIngredient ingredient) {
        ingredients.add(ingredient);
        ingredient.setProductionOrderItem(this);
    }

    public void removeIngredient(ProductionOrderIngredient ingredient) {
        ingredients.remove(ingredient);
        ingredient.setProductionOrderItem(null);
    }

    public void addExpense(ProductionOrderExpense expense) {
        expenses.add(expense);
        expense.setProductionOrderItem(this);
    }

    public void removeExpense(ProductionOrderExpense expense) {
        expenses.remove(expense);
        expense.setProductionOrderItem(null);
    }
}

