package com.example.productservice.service.implement;

import com.example.productservice.common.ProductStatus;
import com.example.productservice.dto.request.CreateProductRequest;
import com.example.productservice.dto.request.SearchRequest;
import com.example.productservice.dto.request.UpdateProductRequest;
import com.example.productservice.dto.response.CreateProductResponse;
import com.example.productservice.dto.response.PageResponse;
import com.example.productservice.dto.response.ProductDetailResponse;
import com.example.productservice.entity.Category;
import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductImage;
import com.example.productservice.exception.ErrorCode;
import com.example.productservice.exception.ProductServiceException;
import com.example.productservice.repository.CategoryRepository;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.service.ProductService;
import com.example.productservice.repository.specification.ProductSpecification;
import com.example.productservice.utils.SlugUtils;
import event.ProductCreatedEvent;
import event.ProductDeletedEvent;
import event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PRODUCT-SERVICE")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    @Override
    public CreateProductResponse createProduct(String sellerId, CreateProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ProductServiceException(ErrorCode.CATEGORY_NOT_FOUND));

        Product product = Product.builder()
                .sellerId(sellerId)
                .name(request.name().trim())
                .slug(generateUniqueSlug(request.name()))
                .description(request.description())
                .price(request.price())
                .quantity(request.quantity())
                .images(request.images())
                .status(request.status())
                .category(category)
                .build();

        productRepository.save(product);
        log.info("Product created successfully: id={}", product.getId());

        ProductCreatedEvent productCreatedEvent = ProductCreatedEvent.builder()
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice().doubleValue())
                .status(product.getStatus().name())
                .createdAt(product.getCreatedAt())
                .categoryId(category.getId())
                .categoryName(category.getName())
                .thumbnailUrl(getThumbnailUrl(product))
                .inStock(product.getQuantity() != null && product.getQuantity() > 0)
                .build();

        sendProductCreatedEvent(productCreatedEvent);
        return CreateProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .images(product.getImages())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .build();
    }

    @Override
    public PageResponse<ProductDetailResponse> getAllProducts(int page, int size, SearchRequest request) {
        int currentPage = Math.max(page, 1);
        int pageSize = Math.max(size, 1);

        // JPA bắt đầu từ 0, còn API dùng page bắt đầu từ 1.
        Pageable pageable = PageRequest.of(currentPage - 1, pageSize, Sort.by(Sort.Direction.ASC, "name"));

        // 2. Tạo Specification để filter (kết hợp các điều kiện)
        Specification<Product> specification = Specification.allOf(
                ProductSpecification.hasName(request.name()),
                ProductSpecification.hasPrice(request.minPrice(), request.maxPrice()),
                ProductSpecification.hasStatus(request.status()),
                ProductSpecification.inStock(request.inStock()),
                ProductSpecification.hasCategory(request.categoryId())
        );

        // 3. Query với Specification + Pageable
        // JPA tự động: filter, paginate, sort, và count total
        Page<Product> productPage = productRepository.findAll(specification, pageable);

        // 4. Lấy content (danh sách products của trang hiện tại)
        List<Product> products = productPage.getContent();

        // 5. Map Entity sang DTO
        List<ProductDetailResponse> responses = products.stream()
                .map(product -> ProductDetailResponse.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .slug(product.getSlug())
                        .description(product.getDescription())
                        .price(product.getPrice())
                        .quantity(product.getQuantity())
                        .images(product.getImages())
                        .status(product.getStatus())
                        .createdAt(product.getCreatedAt())
                .build())
                .toList();

        return PageResponse.<ProductDetailResponse>builder()
                .currentPage(currentPage)
                .pageSize(pageable.getPageSize())
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .content(responses)
                .build();
    }
    @Override
    public ProductDetailResponse getProductById(String id) {
        return productRepository.findById(id)
                .map(this::toProductDetailResponse)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Override
    public ProductDetailResponse getProductBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .map(this::toProductDetailResponse)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    @Override
    public ProductDetailResponse updateProduct(String id, String userId, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_NOT_FOUND));

        checkProductAccess(product, userId);

        if (request.categoryId() != null) {
            if (request.categoryId().isBlank()) {
                throw new ProductServiceException(ErrorCode.INVALID_CATEGORY_ID);
            }
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ProductServiceException(ErrorCode.CATEGORY_NOT_FOUND));
            product.setCategory(category);
        }

        if (request.name() != null) {
            String name = request.name().trim();
            product.setSlug(generateUniqueSlugForUpdate(name, product.getId(), product.getSlug()));
            product.setName(name);
        }

        if (request.description() != null) {
            product.setDescription(request.description());
        }

        if (request.price() != null) {
            product.setPrice(request.price());
        }

        if (request.quantity() != null) {
            product.setQuantity(request.quantity());
        }

        if (request.images() != null) {
            product.setImages(request.images());
        }

        if (request.status() != null) {
            product.setStatus(request.status());
        }

        productRepository.save(product);
        sendProductUpdatedEvent(toProductUpdatedEvent(product));
        log.info("Product updated successfully: id={}", product.getId());

        return toProductDetailResponse(product);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    @Override
    public void deleteProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_NOT_FOUND));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ProductServiceException(ErrorCode.UNAUTHORIZED);
        }

        checkProductAccess(product, authentication.getName());

        productRepository.delete(product);

        ProductDeletedEvent productDeletedEvent = ProductDeletedEvent.builder()
                .productId(product.getId())
                .build();

        kafkaTemplate.send("product-deleted", productDeletedEvent)
                .whenComplete((res, throwable) -> {
                    if (throwable != null) {
                        log.error("Error while sending product deleted event", throwable);
                        return;
                    }
                    log.info("Successfully sent product deleted event");
                });
        log.info("Product deleted successfully: id={}", product.getId());
    }

    private String generateUniqueSlug(String name) {
        String baseSlug = SlugUtils.toSlug(name);
        if (baseSlug.isBlank()) {
            throw new ProductServiceException(ErrorCode.INVALID_PRODUCT_NAME);
        }

        String slug = baseSlug;
        int suffix = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }

        return slug;
    }

    private String generateUniqueSlugForUpdate(String name, String productId, String currentSlug) {
        String baseSlug = SlugUtils.toSlug(name);
        if (baseSlug.isBlank()) {
            throw new ProductServiceException(ErrorCode.INVALID_PRODUCT_NAME);
        }

        if (baseSlug.equals(currentSlug)) {
            return currentSlug;
        }

        String slug = baseSlug;
        int suffix = 1;
        while (productRepository.existsBySlugAndIdNot(slug, productId)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }

        return slug;
    }

    private void checkProductAccess(Product product, String userId) {
        if (product.getSellerId().equals(userId)) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ProductServiceException(ErrorCode.UNAUTHORIZED);
        }

        Set<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (!authorities.contains("ROLE_ADMIN")) {
            throw new ProductServiceException(ErrorCode.PRODUCT_ACCESS_DENIED);
        }
    }

    private ProductUpdatedEvent toProductUpdatedEvent(Product product) {
        Category category = product.getCategory();
        return ProductUpdatedEvent.builder()
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice().doubleValue())
                .status(product.getStatus().name())
                .createdAt(product.getCreatedAt())
                .categoryId(category.getId())
                .categoryName(category.getName())
                .thumbnailUrl(getThumbnailUrl(product))
                .inStock(product.getQuantity() != null && product.getQuantity() > 0)
                .build();
    }

    private void sendProductCreatedEvent(ProductCreatedEvent event) {
        kafkaTemplate.send("product-created", event)
                .whenComplete((res, throwable) -> {
                    if (throwable != null) {
                        log.error("Error while sending product created event", throwable);
                        return;
                    }
                    log.info("Successfully sent product created event");
                });
    }

    private void sendProductUpdatedEvent(ProductUpdatedEvent event) {
        kafkaTemplate.send("product-updated", event)
                .whenComplete((res, throwable) -> {
                    if (throwable != null) {
                        log.error("Error while sending product updated event", throwable);
                        return;
                    }
                    log.info("Successfully sent product updated event");
                });
    }

    private ProductDetailResponse toProductDetailResponse(Product product) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .images(product.getImages())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .build();
    }

    private String getThumbnailUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }

        return product.getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .findFirst()
                .orElse(product.getImages().getFirst())
                .getUrl();
    }

    public List<Product> searchProducts(String categoryId, ProductStatus status,
                                        BigDecimal minPrice, BigDecimal maxPrice) {
        // Chỉ cần 1 method duy nhất - tự động kết hợp các điều kiện
        Specification<Product> spec = Specification
                .where(ProductSpecification.hasCategory(categoryId))
                .and(ProductSpecification.hasStatus(status))
                .and(ProductSpecification.hasPriceBetween(minPrice, maxPrice));
        return productRepository.findAll(spec);
    }
}
