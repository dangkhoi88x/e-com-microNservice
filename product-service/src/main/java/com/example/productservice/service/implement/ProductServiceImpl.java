package com.example.productservice.service.implement;

import com.example.productservice.client.InventoryClient;
import com.example.productservice.client.SellerClient;
import com.example.productservice.common.ProductModerationAction;
import com.example.productservice.common.ProductStatus;
import com.example.productservice.dto.request.CreateProductRequest;
import com.example.productservice.dto.request.CreateSellerProductRequest;
import com.example.productservice.dto.request.CreateSellerProductVariantRequest;
import com.example.productservice.dto.request.ModerateProductRequest;
import com.example.productservice.dto.request.ProductVariantRequest;
import com.example.productservice.dto.request.ProductOptionRequest;
import com.example.productservice.dto.request.ProductOptionValueRequest;
import com.example.productservice.dto.request.SearchRequest;
import com.example.productservice.dto.request.UpdateProductRequest;
import com.example.productservice.dto.request.UpdateSellerProductQuantityRequest;
import com.example.productservice.dto.request.UpdateSellerProductStatusRequest;
import com.example.productservice.dto.request.UpdateSellerProductRequest;
import com.example.productservice.dto.response.CreateProductResponse;
import com.example.productservice.dto.response.PageResponse;
import com.example.productservice.dto.response.ProductDetailResponse;
import com.example.productservice.entity.Category;
import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductVariant;
import com.example.productservice.entity.ProductOption;
import com.example.productservice.entity.ProductOptionValue;
import com.example.productservice.exception.ErrorCode;
import com.example.productservice.exception.ProductServiceException;
import com.example.productservice.mapper.ProductMapper;
import com.example.productservice.repository.CategoryRepository;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.repository.ProductVariantRepository;
import com.example.productservice.service.ProductService;
import com.example.productservice.repository.specification.ProductSpecification;
import com.example.productservice.utils.SlugUtils;
import event.InventoryUpdatedEvent;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Instant;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PRODUCT-SERVICE")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final InventoryClient inventoryClient;
    private final SellerClient sellerClient;
    private final ProductMapper productMapper;

  //  @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
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

        replaceOptions(product, request.options());
        replaceVariants(product, request.variants());
        productRepository.save(product);
        log.info("Product created successfully: id={}", product.getId());

        sendProductCreatedEvent(productMapper.toCreatedEvent(product));
        return productMapper.toCreateResponse(product);
    }

    @Override
    @Transactional
    public CreateProductResponse createSellerProduct(
            String sellerId,
            String authorization,
            CreateSellerProductRequest request
    ) {
        String shopId = requireApprovedShop(authorization);
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ProductServiceException(ErrorCode.CATEGORY_NOT_FOUND));

        Product product = Product.builder()
                .sellerId(sellerId)
                .shopId(shopId)
                .name(request.name().trim())
                .slug(generateUniqueSlug(request.name()))
                .description(request.description())
                .price(request.price())
                .quantity(request.quantity())
                .images(request.images())
                .status(ProductStatus.DRAFT)
                .category(category)
                .build();
        replaceOptions(product, request.options());
        replaceVariants(product, request.variants());

        //variant bị ép về DRAFT
        product.getVariants().forEach(variant -> variant.setStatus(ProductStatus.DRAFT));
        Product savedProduct = productRepository.save(product);
        //Đồng bộ tồn kho
        savedProduct.setQuantity(inventoryClient.setAvailableQuantity(savedProduct.getId(), request.quantity()));
        //Phát event
        sendProductCreatedEvent(productMapper.toCreatedEvent(savedProduct));
        return productMapper.toCreateResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductDetailResponse> getMySellerProducts(String sellerId, int page, int size) {
        int currentPage = Math.max(page, 1);
        Pageable pageable = PageRequest.of(currentPage - 1, Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> products = productRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId, pageable);
        return productMapper.toPageResponse(
                products,
                products.getContent().stream().map(productMapper::toDetailResponse).toList()
        );
    }

    @Override
    public PageResponse<ProductDetailResponse> getAllProducts(int page, int size, SearchRequest request) {
        int currentPage = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), 100);

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
        Map<String, Integer> availableQuantities;
        try {
            availableQuantities = inventoryClient.getAvailableQuantities(
                    products.stream()
                            .map(Product::getId)
                            .toList()
            );
        } catch (InventoryClient.InventoryClientException exception) {
            throw new ProductServiceException(ErrorCode.INVENTORY_SERVICE_UNAVAILABLE);
        }

        // 5. Map Entity sang DTO
        List<ProductDetailResponse> responses = products.stream()
                .map(product -> productMapper.toDetailResponse(
                        product,
                        availableQuantities.getOrDefault(product.getId(), product.getQuantity())
                ))
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
                .filter(product -> product.getStatus() == ProductStatus.ACTIVE)
                .map(this::toProductDetailResponseWithFreshInventory)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Override
    public ProductDetailResponse getSellerProductById(String id, String sellerId) {
        return productRepository.findByIdAndSellerId(id, sellerId)
                .map(this::toProductDetailResponseWithFreshInventory)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_ACCESS_DENIED));
    }

    @Override
    public ProductDetailResponse getProductBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .filter(product -> product.getStatus() == ProductStatus.ACTIVE)
                .map(this::toProductDetailResponseWithFreshInventory)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
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

        if (request.options() != null) {
            replaceOptions(product, request.options());
        }

        if (request.variants() != null) {
            replaceVariants(product, request.variants());
        } else if (request.options() != null) {
            validateVariantAttributes(product.getVariants(), product);
        }

        if (request.status() != null) {
            product.setStatus(request.status());
        }

        productRepository.save(product);
        sendProductUpdatedEvent(productMapper.toUpdatedEvent(product));
        log.info("Product updated successfully: id={}", product.getId());

        return productMapper.toDetailResponse(product);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateSellerProduct(String id, String sellerId, String authorization, UpdateSellerProductRequest request) {
        Product product = productRepository.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_ACCESS_DENIED));
        requireApprovedShopForProduct(authorization, product);
        if (product.getStatus() != ProductStatus.DRAFT && product.getStatus() != ProductStatus.REJECTED) {
            throw new ProductServiceException(ErrorCode.INVALID_PRODUCT_TRANSITION);
        }

        applySellerUpdate(product, request);
        product.getVariants().forEach(variant -> variant.setStatus(product.getStatus()));
        Product savedProduct = productRepository.save(product);
        sendProductUpdatedEvent(productMapper.toUpdatedEvent(savedProduct));
        return productMapper.toDetailResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductDetailResponse addSellerProductVariant(
            String id,
            String sellerId,
            String authorization,
            CreateSellerProductVariantRequest request
    ) {
        Product product = productRepository.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_ACCESS_DENIED));
        requireApprovedShopForProduct(authorization, product);
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ProductServiceException(ErrorCode.INVALID_PRODUCT_TRANSITION);
        }

        validateVariantAttributes(request.attributes(), product);
        ProductVariant variant = toVariant(product, new ProductVariantRequest(
                null,
                request.sku(),
                request.attributes(),
                request.price(),
                request.quantity(),
                request.imageUrl(),
                ProductStatus.ACTIVE
        ), product.getVariants().stream().map(ProductVariant::getSku).collect(Collectors.toSet()));
        product.getVariants().add(variant);

        ProductVariant savedVariant = productVariantRepository.saveAndFlush(variant);
        savedVariant.setQuantity(inventoryClient.setVariantAvailableQuantity(product.getId(), savedVariant.getId(), request.quantity()));
        productVariantRepository.save(savedVariant);
        sendProductUpdatedEvent(productMapper.toUpdatedEvent(product));
        return productMapper.toDetailResponse(product);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateSellerProductQuantity(
            String id,
            String sellerId,
            String authorization,
            UpdateSellerProductQuantityRequest request
    ) {
        Product product = productRepository.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_ACCESS_DENIED));
        requireApprovedShopForProduct(authorization, product);
        if (product.getStatus() == ProductStatus.PENDING_APPROVAL) {
            throw new ProductServiceException(ErrorCode.INVALID_PRODUCT_TRANSITION);
        }

        product.setQuantity(inventoryClient.setAvailableQuantity(product.getId(), request.quantity()));
        Product savedProduct = productRepository.save(product);
        sendProductUpdatedEvent(productMapper.toUpdatedEvent(savedProduct));
        return productMapper.toDetailResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateSellerProductStatus(
            String id,
            String sellerId,
            String authorization,
            UpdateSellerProductStatusRequest request
    ) {
        Product product = productRepository.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_ACCESS_DENIED));
        requireApprovedShopForProduct(authorization, product);
        ProductStatus currentStatus = product.getStatus();
        ProductStatus targetStatus = request.status();
        boolean currentCanBeChanged = currentStatus == ProductStatus.ACTIVE || currentStatus == ProductStatus.INACTIVE;
        boolean targetCanBeSet = targetStatus == ProductStatus.ACTIVE || targetStatus == ProductStatus.INACTIVE;
        if (!currentCanBeChanged || !targetCanBeSet) {
            throw new ProductServiceException(ErrorCode.INVALID_PRODUCT_TRANSITION);
        }

        product.setStatus(targetStatus);
        product.getVariants().forEach(variant -> variant.setStatus(targetStatus));
        Product savedProduct = productRepository.save(product);
        sendProductUpdatedEvent(productMapper.toUpdatedEvent(savedProduct));
        return productMapper.toDetailResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductDetailResponse submitSellerProduct(String id, String sellerId, String authorization) {
        Product product = productRepository.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_ACCESS_DENIED));
        String approvedShopId = requireApprovedShop(authorization);
        if (!approvedShopId.equals(product.getShopId())
                || (product.getStatus() != ProductStatus.DRAFT && product.getStatus() != ProductStatus.REJECTED)) {
            throw new ProductServiceException(ErrorCode.INVALID_PRODUCT_TRANSITION);
        }

        product.setStatus(ProductStatus.PENDING_APPROVAL);
        product.getVariants().forEach(variant -> variant.setStatus(ProductStatus.PENDING_APPROVAL));
        product.setModerationNote(null);
        product.setModeratedBy(null);
        product.setModeratedAt(null);
        Product savedProduct = productRepository.save(product);
        sendProductUpdatedEvent(productMapper.toUpdatedEvent(savedProduct));
        return productMapper.toDetailResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductDetailResponse moderateProduct(String id, String adminUserId, ModerateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_NOT_FOUND));
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        ProductStatus targetStatus = targetModerationStatus(product.getStatus(), request.action());
        if ((request.action() == ProductModerationAction.REJECT || request.action() == ProductModerationAction.HIDE)
                && note == null) {
            throw new ProductServiceException(ErrorCode.MODERATION_NOTE_REQUIRED);
        }

        product.setStatus(targetStatus);
        product.getVariants().forEach(variant -> variant.setStatus(targetStatus));
        product.setModerationNote(note);
        product.setModeratedBy(adminUserId);
        product.setModeratedAt(Instant.now());
        Product savedProduct = productRepository.save(product);
        sendProductUpdatedEvent(productMapper.toUpdatedEvent(savedProduct));
        return productMapper.toDetailResponse(savedProduct);
    }

   // @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    @Override
    public void deleteProduct(String id, String sellerId, String authorization) {
        Product product = productRepository.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ProductServiceException(ErrorCode.PRODUCT_ACCESS_DENIED));
        requireApprovedShopForProduct(authorization, product);

        if (product.getStatus() != ProductStatus.DRAFT && product.getStatus() != ProductStatus.REJECTED) {
            throw new ProductServiceException(ErrorCode.INVALID_PRODUCT_TRANSITION);
        }

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

    @Override
    @Transactional
    public void syncStockFromInventoryEvent(InventoryUpdatedEvent event) {
        if (event.getVariantId() != null && !event.getVariantId().isBlank()) {
            ProductVariant variant = productVariantRepository.findById(event.getVariantId())
                    .orElse(null);

            if (variant == null) {
                log.warn("Skip inventory sync because product variant does not exist: variantId={}", event.getVariantId());
                return;
            }

            variant.setQuantity(event.getAvailableQuantity());
            productVariantRepository.save(variant);
            sendProductUpdatedEvent(productMapper.toUpdatedEvent(variant.getProduct()));

            log.info("Synced variant stock from inventory: productId={}, variantId={}, quantity={}",
                    variant.getProduct().getId(),
                    variant.getId(),
                    variant.getQuantity());
            return;
        }

        Product product = productRepository.findById(event.getProductId())
                .orElse(null);

        if (product == null) {
            log.warn("Skip inventory sync because product does not exist: productId={}", event.getProductId());
            return;
        }

        product.setQuantity(event.getAvailableQuantity());
        Product savedProduct = productRepository.save(product);
        sendProductUpdatedEvent(productMapper.toUpdatedEvent(savedProduct));

        log.info("Synced product stock from inventory: productId={}, quantity={}",
                savedProduct.getId(),
                savedProduct.getQuantity());
    }

    @Override
    @Transactional
    public void inactivateProductsForSuspendedShop(String shopId) {
        List<Product> activeProducts = productRepository.findAllByShopIdAndStatus(shopId, ProductStatus.ACTIVE);
        if (activeProducts.isEmpty()) {
            log.info("No active products to inactivate for suspended shop: shopId={}", shopId);
            return;
        }

        activeProducts.forEach(product -> {
            product.setStatus(ProductStatus.INACTIVE);
            product.getVariants().forEach(variant -> variant.setStatus(ProductStatus.INACTIVE));
        });
        List<Product> savedProducts = productRepository.saveAll(activeProducts);
        savedProducts.forEach(product -> sendProductUpdatedEvent(productMapper.toUpdatedEvent(product)));
        log.info("Inactivated {} active products for suspended shop: shopId={}", savedProducts.size(), shopId);
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

    private void applySellerUpdate(Product product, UpdateSellerProductRequest request) {
        if (request.categoryId() != null) {
            if (request.categoryId().isBlank()) {
                throw new ProductServiceException(ErrorCode.INVALID_CATEGORY_ID);
            }
            product.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ProductServiceException(ErrorCode.CATEGORY_NOT_FOUND)));
        }
        if (request.name() != null) {
            String name = request.name().trim();
            product.setSlug(generateUniqueSlugForUpdate(name, product.getId(), product.getSlug()));
            product.setName(name);
        }
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null) product.setPrice(request.price());
        if (request.quantity() != null) product.setQuantity(request.quantity());
        if (request.images() != null) product.setImages(request.images());
        if (request.options() != null) replaceOptions(product, request.options());
        if (request.variants() != null) {
            replaceVariants(product, request.variants());
        } else if (request.options() != null) {
            validateVariantAttributes(product.getVariants(), product);
        }
    }

    private String requireApprovedShop(String authorization) {
        try {
            return sellerClient.requireApprovedShop(authorization).shopId();
        } catch (SellerClient.SellerClientException exception) {
            if (exception.getFailure() == SellerClient.SellerClientFailure.NOT_ELIGIBLE) {
                throw new ProductServiceException(ErrorCode.SELLER_SHOP_NOT_APPROVED);
            }
            throw new ProductServiceException(ErrorCode.SELLER_SERVICE_UNAVAILABLE);
        }
    }

    private ProductStatus targetModerationStatus(ProductStatus currentStatus, ProductModerationAction action) {
        return switch (action) {
            case APPROVE -> {
                if (currentStatus != ProductStatus.PENDING_APPROVAL) {
                    throw new ProductServiceException(ErrorCode.INVALID_PRODUCT_TRANSITION);
                }
                yield ProductStatus.ACTIVE;
            }
            case REJECT -> {
                if (currentStatus != ProductStatus.PENDING_APPROVAL) {
                    throw new ProductServiceException(ErrorCode.INVALID_PRODUCT_TRANSITION);
                }
                yield ProductStatus.REJECTED;
            }
            case HIDE -> {
                if (currentStatus != ProductStatus.ACTIVE) {
                    throw new ProductServiceException(ErrorCode.INVALID_PRODUCT_TRANSITION);
                }
                yield ProductStatus.INACTIVE;
            }
        };
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

    private void requireApprovedShopForProduct(String authorization, Product product) {
        String approvedShopId = requireApprovedShop(authorization);
        if (!approvedShopId.equals(product.getShopId())) {
            throw new ProductServiceException(ErrorCode.PRODUCT_ACCESS_DENIED);
        }
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

    private ProductDetailResponse toProductDetailResponseWithFreshInventory(Product product) {
        Integer availableQuantity;
        try {
            availableQuantity = inventoryClient.getAvailableQuantity(product.getId())
                    .orElse(product.getQuantity());
        } catch (InventoryClient.InventoryClientException exception) {
            throw new ProductServiceException(ErrorCode.INVENTORY_SERVICE_UNAVAILABLE);
        }

        return productMapper.toDetailResponse(product, availableQuantity);
    }

    private void replaceVariants(Product product, List<ProductVariantRequest> variantRequests) {
        product.getVariants().clear();

        if (variantRequests == null || variantRequests.isEmpty()) {
            return;
        }

        Set<String> usedSkus = new HashSet<>();
        for (ProductVariantRequest request : variantRequests) {
            validateVariantAttributes(request.attributes(), product);
            ProductVariant variant = toVariant(product, request, usedSkus);
            product.getVariants().add(variant);
            usedSkus.add(variant.getSku());
        }
    }

    private void replaceOptions(Product product, List<ProductOptionRequest> optionRequests) {
        product.getOptions().clear();
        if (optionRequests == null) return;
        for (ProductOptionRequest request : optionRequests) {
            ProductOption option = ProductOption.builder().product(product).name(request.name().trim())
                    .displayName(request.displayName()).displayType(request.displayType())
                    .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                    .required(request.required() == null || request.required()).build();
            if (request.values() != null) for (ProductOptionValueRequest value : request.values()) {
                option.getValues().add(ProductOptionValue.builder().option(option).value(value.value().trim())
                        .displayValue(value.displayValue()).colorHex(value.colorHex()).imageUrl(value.imageUrl())
                        .displayOrder(value.displayOrder() == null ? 0 : value.displayOrder())
                        .active(value.active() == null || value.active()).build());
            }
            product.getOptions().add(option);
        }
    }

    private void validateVariantAttributes(Map<String, String> attributes, Product product) {
        if (attributes == null || attributes.isEmpty()) return;
        for (var attribute : attributes.entrySet()) {
            ProductOption option = product.getOptions().stream().filter(item -> item.getName().equals(attribute.getKey())).findFirst()
                    .orElseThrow(() -> new ProductServiceException(ErrorCode.INVALID_PRODUCT_VARIANT_ATTRIBUTE));
            boolean valid = option.getValues().stream().anyMatch(value -> Boolean.TRUE.equals(value.getActive()) && value.getValue().equals(attribute.getValue()));
            if (!valid) throw new ProductServiceException(ErrorCode.INVALID_PRODUCT_VARIANT_ATTRIBUTE);
        }
    }

    private void validateVariantAttributes(List<ProductVariant> variants, Product product) {
        variants.forEach(variant -> validateVariantAttributes(variant.getAttributes(), product));
    }

    private ProductVariant toVariant(Product product, ProductVariantRequest request, Set<String> usedSkus) {
        String sku = normalizeSku(request.sku());
        if (sku == null) {
            sku = generateUniqueSku(product.getSlug(), usedSkus);
        } else if (usedSkus.contains(sku)) {
            throw new ProductServiceException(ErrorCode.DUPLICATE_PRODUCT_VARIANT_SKU);
        } else if (variantSkuExists(request, sku)) {
            throw new ProductServiceException(ErrorCode.DUPLICATE_PRODUCT_VARIANT_SKU);
        }

        ProductStatus status = request.status() != null ? request.status() : product.getStatus();
        BigDecimal price = request.price() != null ? request.price() : product.getPrice();
        Integer quantity = request.quantity() != null ? request.quantity() : 0;

        return ProductVariant.builder()
                .id(request.id())
                .product(product)
                .sku(sku)
                .attributes(request.attributes())
                .price(price)
                .quantity(quantity)
                .imageUrl(request.imageUrl())
                .status(status)
                .build();
    }

    private boolean variantSkuExists(ProductVariantRequest request, String sku) {
        if (request.id() == null || request.id().isBlank()) {
            return productVariantRepository.existsBySku(sku);
        }

        return productVariantRepository.existsBySkuAndIdNot(sku, request.id());
    }

    private String normalizeSku(String sku) {
        if (sku == null || sku.isBlank()) {
            return null;
        }

        return sku.trim().toUpperCase();
    }

    private String generateUniqueSku(String productSlug, Set<String> usedSkus) {
        String baseSku = (productSlug == null || productSlug.isBlank())
                ? "SKU"
                : productSlug.toUpperCase().replace("-", "-");
        String sku = baseSku;
        int suffix = 1;
        while (productVariantRepository.existsBySku(sku) || usedSkus.contains(sku)) {
            sku = baseSku + "-" + suffix;
            suffix++;
        }

        return sku;
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
