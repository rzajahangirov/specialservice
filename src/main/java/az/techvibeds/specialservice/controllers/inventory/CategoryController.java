package az.techvibeds.specialservice.controllers.inventory;

import az.techvibeds.specialservice.dtos.category.CategoryCreateDto;
import az.techvibeds.specialservice.dtos.category.CategoryReadDto;
import az.techvibeds.specialservice.dtos.category.CategoryUpdateDto;
import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/category")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<CategoryReadDto> create(@RequestBody CategoryCreateDto dto){
        CategoryReadDto created = categoryService.create(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN')")
    public ResponseEntity<CategoryReadDto> update(@RequestBody CategoryUpdateDto dto){
        CategoryReadDto updated = categoryService.update(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:INVENTORY:SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id){
        categoryService.delete(id);
        return ResponseEntity.ok(new ApiResponse("Category deleted", true));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<CategoryReadDto> getById(@PathVariable Long id){
        return ResponseEntity.ok(categoryService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:VIEWER', 'MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<List<CategoryReadDto>> getAll(){
        return ResponseEntity.ok(categoryService.getAll());
    }
}
