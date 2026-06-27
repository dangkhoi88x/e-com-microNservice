package com.example.productservice.service.implement;

import com.example.productservice.dto.request.CreateCategoryRequest;
import com.example.productservice.dto.request.UpdateCategoryRequest;
import com.example.productservice.dto.response.CategoryDetailResponse;
import com.example.productservice.dto.response.CreateCategoryResponse;
import com.example.productservice.dto.response.UpdateCategoryResponse;
import com.example.productservice.entity.Category;
import com.example.productservice.exception.ErrorCode;
import com.example.productservice.exception.ProductServiceException;
import com.example.productservice.repository.CategoryRepository;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.service.CategoryService;
import com.example.productservice.utils.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j(topic = "CATEGORY-SERVICE")
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    //    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public CreateCategoryResponse createCategory(CreateCategoryRequest request) {
       String name = request.name().trim();
       if(categoryRepository.existsByNameIgnoreCase(name))
           throw new ProductServiceException(ErrorCode.CATEGORY_EXISTED);

       String slug = generateUniqueSlug(name);

       Category category = Category.builder()
               .name(name)
               .slug(slug)
               .description(request.description())
               .build();
        categoryRepository.save(category);
        return CreateCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }

    @Override
    public List<CategoryDetailResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toCategoryDetailResponse)
                .toList();
    }

    @Override
    public CategoryDetailResponse getCategoryDetail(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.CATEGORY_NOT_FOUND));

        return toCategoryDetailResponse(category);
    }

    @Override
    public CategoryDetailResponse getCategoryDetailBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.CATEGORY_NOT_FOUND));

        return toCategoryDetailResponse(category);
    }

    //    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public UpdateCategoryResponse updateCategory(String id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.CATEGORY_NOT_FOUND));

        Optional.ofNullable(request.name()).ifPresent(name -> {
            String trimmedName = name.trim();
            if (!category.getName().equalsIgnoreCase(trimmedName)) {
                if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
                    throw new ProductServiceException(ErrorCode.CATEGORY_EXISTED);
                }
                category.setSlug(generateUniqueSlug(trimmedName));
            }
            category.setName(trimmedName);
        });
        Optional.ofNullable(request.description()).ifPresent(category::setDescription);
        categoryRepository.save(category);
        return UpdateCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt()).build();

    }


    //    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public void deleteCategory(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.CATEGORY_NOT_FOUND));

        if (productRepository.existsByCategoryId(id)) {
            throw new ProductServiceException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully");

    }

    private String generateUniqueSlug(String name) {
        String baseSlug = SlugUtils.toSlug(name);
        if (baseSlug.isBlank()) {
            throw new ProductServiceException(ErrorCode.INVALID_CATEGORY_NAME);
        }

        String slug = baseSlug;
        int suffix = 1;

        while (categoryRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }

        return slug;
    }

    private CategoryDetailResponse toCategoryDetailResponse(Category category) {
        return CategoryDetailResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
