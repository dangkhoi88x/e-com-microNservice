package com.example.productservice.service.implement;

import com.example.productservice.client.InventoryClient;
import com.example.productservice.client.SellerClient;
import com.example.productservice.common.ProductStatus;
import com.example.productservice.entity.Category;
import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductVariant;
import com.example.productservice.mapper.ProductMapper;
import com.example.productservice.repository.CategoryRepository;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.repository.ProductVariantRepository;
import event.ProductUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplShopSuspensionTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private SellerClient sellerClient;
    @Spy
    private ProductMapper productMapper = new ProductMapper();

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void suspensionInactivatesOnlyActiveProductsAndVariants() {
        Category category = Category.builder().id("category-1").name("Electronics").slug("electronics").build();
        ProductVariant variant = ProductVariant.builder().status(ProductStatus.ACTIVE).build();
        Product product = Product.builder()
                .id("product-1")
                .shopId("shop-1")
                .name("Laptop")
                .description("Description")
                .price(BigDecimal.TEN)
                .quantity(2)
                .sellerId("seller-1")
                .slug("laptop")
                .status(ProductStatus.ACTIVE)
                .category(category)
                .variants(new java.util.ArrayList<>(List.of(variant)))
                .build();
        when(productRepository.findAllByShopIdAndStatus("shop-1", ProductStatus.ACTIVE)).thenReturn(List.of(product));
        when(productRepository.saveAll(List.of(product))).thenReturn(List.of(product));
        when(kafkaTemplate.send(eq("product-updated"), any(ProductUpdatedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        productService.inactivateProductsForSuspendedShop("shop-1");

        assertEquals(ProductStatus.INACTIVE, product.getStatus());
        assertEquals(ProductStatus.INACTIVE, variant.getStatus());
    }
}
