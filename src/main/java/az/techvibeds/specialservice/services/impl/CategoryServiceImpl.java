package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.category.CategoryCreateDto;
import az.techvibeds.specialservice.dtos.category.CategoryReadDto;
import az.techvibeds.specialservice.dtos.category.CategoryUpdateDto;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.models.Category;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.repositories.CategoryRepository;
import az.techvibeds.specialservice.services.CategoryService;
import az.techvibeds.specialservice.services.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CompanyService companyService;

    private Company getCurrentCompany() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || (auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"))) {
            throw new BusinessException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        return companyService.findByUserEmail(auth.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public Category findByName(String name) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"))) {
            try {
                Company company = getCurrentCompany();
                return categoryRepository.findByNameAndCompanyId(name, company.getId())
                        .orElseGet(() -> categoryRepository.findByName(name));
            } catch (Exception e) {
                // Fallback to global category if user company fails (e.g. background threads or custom runners)
                return categoryRepository.findByName(name);
            }
        }
        Category category = categoryRepository.findByName(name);
        if (category == null) {
            log.warn("Category lookup failed for name: {}", name);
            throw new BusinessException("Category not found with name: " + name, HttpStatus.NOT_FOUND);
        }
        return category;
    }

    @Override
    @Transactional(readOnly = true)
    public Category findById(Long categoryId) {
        return findEntityById(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryReadDto getById(Long id) {
        Category category = findEntityById(id);
        Company company = getCurrentCompany();
        if (category.getCompany() != null && !category.getCompany().getId().equals(company.getId())) {
            throw new BusinessException("Unauthorized access to this category", HttpStatus.FORBIDDEN);
        }
        return mapToReadDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryReadDto> getAll() {
        Company company = getCurrentCompany();
        return categoryRepository.findAllByCompanyId(company.getId())
                .stream()
                .map(this::mapToReadDto)
                .toList();
    }

    @Override
    @Transactional
    public CategoryReadDto create(CategoryCreateDto dto) {
        Company company = getCurrentCompany();
        if (categoryRepository.existsByNameAndCompanyId(dto.getName(), company.getId())) {
            throw new BusinessException("Category with this name already exists", HttpStatus.CONFLICT);
        }

        Category category = new Category();
        category.setName(dto.getName());
        category.setCompany(company);

        categoryRepository.save(category);
        log.info("Category created successfully: {} for company id: {}", category.getName(), company.getId());
        return mapToReadDto(category);
    }

    @Override
    @Transactional
    public CategoryReadDto update(CategoryUpdateDto dto) {
        Category category = findEntityById(dto.getId());
        Company company = getCurrentCompany();

        if (category.getCompany() != null && !category.getCompany().getId().equals(company.getId())) {
            throw new BusinessException("Unauthorized access to this category", HttpStatus.FORBIDDEN);
        }

        if (!category.getName().equals(dto.getName()) && categoryRepository.existsByNameAndCompanyId(dto.getName(), company.getId())) {
            throw new BusinessException("Category name already in use", HttpStatus.CONFLICT);
        }

        category.setName(dto.getName());
        categoryRepository.save(category);
        log.info("Category updated: id={}, newName={}", category.getId(), category.getName());
        return mapToReadDto(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = findEntityById(id);
        Company company = getCurrentCompany();

        if (category.getCompany() != null && !category.getCompany().getId().equals(company.getId())) {
            throw new BusinessException("Unauthorized access to this category", HttpStatus.FORBIDDEN);
        }

        if (!category.getProducts().isEmpty()) {
            throw new BusinessException("Cannot delete category with associated products", HttpStatus.PRECONDITION_FAILED);
        }

        categoryRepository.delete(category);
        log.warn("Category deleted: id={}", id);
    }

    private Category findEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Category not found with id: " + id, HttpStatus.NOT_FOUND));
    }

    private CategoryReadDto mapToReadDto(Category category) {
        return CategoryReadDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}