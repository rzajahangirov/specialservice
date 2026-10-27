package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.category.CategoryCreateDto;
import az.techvibeds.specialservice.dtos.category.CategoryReadDto;
import az.techvibeds.specialservice.dtos.category.CategoryUpdateDto;
import az.techvibeds.specialservice.models.Category;

import java.util.List;

public interface CategoryService {
    Category findByName(String name);

    Category findById(Long categoryId);

    CategoryReadDto create(CategoryCreateDto dto);

    CategoryReadDto update(CategoryUpdateDto dto);

    void delete(Long id);

    CategoryReadDto getById(Long id);

    List<CategoryReadDto> getAll();
}
