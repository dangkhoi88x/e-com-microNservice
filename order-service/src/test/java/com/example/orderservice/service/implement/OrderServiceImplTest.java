package com.example.orderservice.service.implement;

import com.example.orderservice.client.CartClient;
import com.example.orderservice.client.InventoryClient;
import com.example.orderservice.client.ProductClient;
import com.example.orderservice.client.PromotionClient;
import com.example.orderservice.client.ShipmentClient;
import com.example.orderservice.common.OrderStatus;
import com.example.orderservice.dto.request.CheckoutOrderRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.ProductDetailResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.exception.ErrorCode;
import com.example.orderservice.exception.OrderServiceException;
import com.example.orderservice.repository.OrderItemRepository;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the checkout saga: what the order ends up as, and whether the steps that already
 * succeeded are compensated when a later step fails.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplTest {

    private static final String USER_ID = "user-1";
    private static final String TOKEN = "token-1";
    private static final String PRODUCT_ID = "product-1";
    private static final String CART_ITEM_ID = "cart-item-1";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private CartClient cartClient;

    @Mock
    private PromotionClient promotionClient;

    @Mock
    private ShipmentClient shipmentClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        when(cartClient.checkoutItems(USER_ID))
                .thenReturn(List.of(new CartClient.CartItem(CART_ITEM_ID, PRODUCT_ID, null, 2)));
        when(productClient.getProductById(PRODUCT_ID)).thenReturn(new ProductDetailResponse(
                PRODUCT_ID,
                "shop-1",
                "seller-1",
                "Nova Runner",
                "Running shoes",
                BigDecimal.valueOf(500_000),
                10,
                List.of(),
                "ACTIVE"
        ));
        // Stand in for JPA assigning the generated id, so the saga has an orderId to correlate on.
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) order.setId(UUID.randomUUID().toString());
            return order;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(anyString(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void checkoutReachesPendingPaymentAndLocksTheCheckedOutCartItems() {
        when(promotionClient.reserveFlashDeals(anyString(), any())).thenReturn(List.of());

        OrderResponse order = orderService.checkout(USER_ID, new CheckoutOrderRequest("12 Nguyen Hue", null), TOKEN);

        assertEquals(OrderStatus.PENDING_PAYMENT.name(), order.status());
        assertEquals(BigDecimal.valueOf(1_000_000), order.subtotalAmount());
        verify(inventoryClient).reserveInventory(any(), eq(TOKEN));
        verify(cartClient).mark(USER_ID, order.id(), List.of(CART_ITEM_ID));
        verify(inventoryClient, never()).releaseInventory(any(), anyString());
    }

    @Test
    void checkoutFailsAsInventoryFailedWhenStockCannotBeReserved() {
        doThrow(new OrderServiceException(ErrorCode.INVENTORY_RESERVATION_FAILED))
                .when(inventoryClient).reserveInventory(any(), anyString());

        OrderResponse order = orderService.checkout(USER_ID, new CheckoutOrderRequest("12 Nguyen Hue", null), TOKEN);

        assertEquals(OrderStatus.INVENTORY_FAILED.name(), order.status());
        // Nothing was reserved, so nothing may be released, and the cart stays unlocked.
        verify(inventoryClient, never()).releaseInventory(any(), anyString());
        verify(cartClient, never()).mark(anyString(), anyString(), any());
    }

    @Test
    void checkoutFailsAsPromotionFailedAndReleasesInventoryWhenFlashDealReservationFails() {
        when(promotionClient.reserveFlashDeals(anyString(), any()))
                .thenThrow(new OrderServiceException(ErrorCode.PROMOTION_SERVICE_UNAVAILABLE));

        OrderResponse order = orderService.checkout(USER_ID, new CheckoutOrderRequest("12 Nguyen Hue", null), TOKEN);

        // Inventory succeeded, so the failure came later: reporting INVENTORY_FAILED here would
        // tell the customer an in-stock item had sold out.
        assertEquals(OrderStatus.PROMOTION_FAILED.name(), order.status());
        verify(inventoryClient).releaseInventory(any(), eq(TOKEN));
        verify(cartClient, never()).mark(anyString(), anyString(), any());
    }

    @Test
    void checkoutRejectsAnEmptyCartBeforeCreatingAnyOrder() {
        when(cartClient.checkoutItems(USER_ID)).thenReturn(List.of());

        OrderServiceException exception = org.junit.jupiter.api.Assertions.assertThrows(
                OrderServiceException.class,
                () -> orderService.checkout(USER_ID, new CheckoutOrderRequest("12 Nguyen Hue", null), TOKEN));

        assertEquals(ErrorCode.CART_CHECKOUT_EMPTY, exception.getErrorCode());
        verify(orderRepository, never()).saveAndFlush(any());
    }
}
