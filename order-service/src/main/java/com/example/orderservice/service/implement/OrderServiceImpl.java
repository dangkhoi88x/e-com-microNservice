package com.example.orderservice.service.implement;


import com.example.event.OrderCancelledEvent;
import com.example.event.OrderCreatedEvent;
import com.example.event.OrderItemEvent;
import com.example.event.OrderStatusUpdatedEvent;
import com.example.event.CodPaymentCreatedEvent;
import com.example.event.PaymentCancelledEvent;
import com.example.event.PaymentFailedEvent;
import com.example.event.PaymentSuccessEvent;
import com.example.event.ShipmentRequestedEvent;
import com.example.event.ShipmentStatusUpdatedEvent;
import com.example.orderservice.client.InventoryClient;
import com.example.orderservice.client.CartClient;
import com.example.orderservice.client.ProductClient;
import com.example.orderservice.client.PromotionClient;
import com.example.orderservice.client.ShipmentClient;
import com.example.orderservice.common.OrderStatus;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.CheckoutOrderRequest;
import com.example.orderservice.dto.request.InventoryOrderRequest;
import com.example.orderservice.dto.request.ReserveInventoryItemRequest;
import com.example.orderservice.dto.request.ReserveInventoryRequest;
import com.example.orderservice.dto.response.FlashDealPriceResponse;
import com.example.orderservice.dto.response.OrderItemResponse;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.PageResponse;
import com.example.orderservice.dto.response.ProductDetailResponse;
import com.example.orderservice.dto.response.ProductVariantResponse;
import com.example.orderservice.dto.response.PromotionCalculationResponse;
import com.example.orderservice.dto.response.ReviewEligibilityResponse;
import com.example.orderservice.dto.response.SellerOrderDetailResponse;
import com.example.orderservice.dto.response.SellerAnalyticsResponse;
import com.example.orderservice.dto.response.AdminAnalyticsResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.exception.ErrorCode;
import com.example.orderservice.exception.OrderServiceException;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.OrderItemRepository;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-SERVICE")
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String ORDER_CANCELLED_TOPIC = "order-cancelled";
    private static final String ORDER_STATUS_UPDATED_TOPIC = "order-status-updated";
    private static final String SHIPMENT_REQUESTED_TOPIC = "shipment-requested";
    private static final String COD_METHOD = "COD";
    private static final DateTimeFormatter ORDER_CODE_DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyyMMdd")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PENDING,
            OrderStatus.PENDING_PAYMENT,
            OrderStatus.CONFIRMED
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final CartClient cartClient;
    private final PromotionClient promotionClient;
    private final ShipmentClient shipmentClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional(readOnly = true)
    public ReviewEligibilityResponse checkReviewEligibility(String userId, String orderItemId) {
        OrderItem item = orderItemRepository.findByIdWithOrder(orderItemId)
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));
        Order order = item.getOrder();

        if (!order.getUserId().equals(userId)) {
            throw new OrderServiceException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        return new ReviewEligibilityResponse(
                order.getStatus() == OrderStatus.COMPLETED,
                order.getId(),
                item.getId(),
                item.getProductId(),
                item.getVariantId(),
                item.getProductName(),
                order.getStatus().name(),
                order.getSellerId()
        );
    }

    @Override
    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request, String token) {
        return createOrder(userId, request, token, null);
    }

    private OrderResponse createOrder(String userId, CreateOrderRequest request, String token, String campaignCode) {
        //Nhận danh sách sản phẩm
        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .userId(userId)
                .shippingAddress(request.shippingAddress())
                .status(OrderStatus.PENDING)
                .subtotalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        //Lấy thông tin từ Product Service
        for (var itemRequest : request.items()) {
            ProductDetailResponse product = productClient.getProductById(itemRequest.productId());

            if (product == null) {
                throw new OrderServiceException(ErrorCode.PRODUCT_NOT_FOUND);
            }

            if (!"ACTIVE".equals(product.status())) {
                throw new OrderServiceException(ErrorCode.PRODUCT_NOT_ACTIVE);
            }
        //xác định seller
            if (order.getSellerId() == null) {
                order.setSellerId(product.sellerId());
                order.setShopId(product.shopId());
            } else if (!order.getSellerId().equals(product.sellerId())) {
                throw new OrderServiceException(ErrorCode.MULTI_SHOP_CHECKOUT_NOT_SUPPORTED);
            }
        //Tìm variant và xác định giá
            ProductVariantResponse variant = findVariant(product, itemRequest.variantId());
            BigDecimal itemPrice = variant != null ? variant.price() : product.price();
            String itemName = variant != null
                    ? product.name() + " - " + formatVariantAttributes(variant)
                    : product.name();
    //Tính tiền từng item
            BigDecimal subtotal = itemPrice
                    .multiply(BigDecimal.valueOf(itemRequest.quantity()));
    //snapshot
            OrderItem orderItem = OrderItem.builder()
                    .productId(product.id())
                    .variantId(variant != null ? variant.id() : null)
                    .productName(itemName)
                    .price(itemPrice)
                    .originalPrice(itemPrice)
                    .quantity(itemRequest.quantity())
                    .subtotal(subtotal)
                    .build();
    //Thêm item vào Order
            order.addItem(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }
        //Lưu Order lần đầu
        order.setSubtotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.saveAndFlush(order);
        OrderStatus oldStatus = savedOrder.getStatus();
        //Giữ tồn kho và khuyến mãi
        boolean inventoryReserved = false;
        boolean flashDealsReserved = false;

        try {
            //Giữ tồn kho
            reserveInventory(savedOrder, token);
            inventoryReserved = true;
    //Giữ flash deal
            List<PromotionClient.FlashDealItemRequest> flashItems = savedOrder.getItems().stream()
                    .map(item -> new PromotionClient.FlashDealItemRequest(item.getProductId(), item.getVariantId(), item.getQuantity()))
                    .toList();

            List<FlashDealPriceResponse> flashPrices = promotionClient.reserveFlashDeals(savedOrder.getId(), flashItems);
            flashDealsReserved = !flashPrices.isEmpty();
            applyFlashDealPrices(savedOrder, flashPrices);
            //Áp dụng campaign code
            if (hasPromotion(campaignCode)) {
                PromotionCalculationResponse promotion = promotionClient.validate(campaignCode, savedOrder.getSubtotalAmount());
                savedOrder.setPromotionCode(promotion.campaignCode());
                savedOrder.setDiscountAmount(promotion.discountAmount());
                savedOrder.setTotalAmount(promotion.finalAmount());
            }
            if (hasPromotion(savedOrder.getPromotionCode())) {
                promotionClient.reserve(savedOrder.getPromotionCode(), userId, savedOrder.getId(), savedOrder.getSubtotalAmount());
            }
            //Chuyển sang chờ thanh toán
            savedOrder.setStatus(OrderStatus.PENDING_PAYMENT);
            Order reservedOrder = orderRepository.save(savedOrder);
            log.info("Order created: orderId={}, orderCode={}, userId={}, sellerId={}, itemCount={}, totalAmount={}, status={}",
                    reservedOrder.getId(),
                    reservedOrder.getOrderCode(),
                    reservedOrder.getUserId(),
                    reservedOrder.getSellerId(),
                    reservedOrder.getItems().size(),
                    reservedOrder.getTotalAmount(),
                    reservedOrder.getStatus());
            //event
            publishOrderStatusUpdatedEvent(reservedOrder, oldStatus);
            publishOrderCreatedEvent(reservedOrder);
            return toOrderResponse(reservedOrder);
            //tạo Order thất bại
        } catch (RuntimeException exception) {
            log.error("Failed to prepare checkout resources: orderId={}", savedOrder.getId(), exception);
            if (inventoryReserved) {
                safeReleaseInventory(savedOrder, token);
                safeReleasePromotion(savedOrder);
            }
            if (flashDealsReserved) safeReleaseFlashDeals(savedOrder);
            savedOrder.setStatus(inventoryReserved
                    ? OrderStatus.PROMOTION_FAILED
                    : OrderStatus.INVENTORY_FAILED);
            Order failedOrder = orderRepository.save(savedOrder);
            publishOrderStatusUpdatedEvent(failedOrder, oldStatus);

            return toOrderResponse(failedOrder);
        }
    }
    //lấy danh sách đơn
    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(String userId, int page, int size) {
        Pageable pageable = createOrderPageable(page, size);
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);

        List<OrderResponse> content = orderPage.getContent()
                .stream()
                .map(this::toOrderResponse)
                .toList();

        return toPageResponse(orderPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getSellerOrders(String sellerId, int page, int size) {
        Pageable pageable = createOrderPageable(page, size);
        Page<Order> orderPage = orderRepository.findBySellerId(sellerId, pageable);
        return toPageResponse(orderPage, orderPage.getContent().stream().map(this::toOrderResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SellerOrderDetailResponse getSellerOrderDetail(String sellerId, String orderId) {
        Order order = orderRepository.findById(orderId).filter(value -> sellerId.equals(value.getSellerId()))
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));
        return new SellerOrderDetailResponse(toOrderResponse(order), shipmentClient.getByOrderId(orderId));
    }
    //tính trực tiếp bằng query aggregate
    @Override
    @Transactional(readOnly = true)
    public SellerAnalyticsResponse getSellerAnalytics(String sellerId, Instant from, Instant to) {
        OrderRepository.AnalyticsSummary summary = orderRepository.getSellerAnalyticsSummary(sellerId, from, to);
        BigDecimal revenue = summary.getRevenue();
        long completedOrders = summary.getCompletedOrders();
        Map<String, Long> statuses = orderRepository.countSellerByStatus(sellerId, from, to).stream()
                .collect(Collectors.toMap(OrderRepository.StatusCount::getStatus, OrderRepository.StatusCount::getTotal));
        List<SellerAnalyticsResponse.TopProduct> topProducts = orderRepository.getSellerTopProducts(sellerId, from, to).stream()
                .map(product -> new SellerAnalyticsResponse.TopProduct(product.getProductId(), product.getName(), product.getQuantitySold(), product.getRevenue()))
                .toList();
        BigDecimal averageOrderValue = completedOrders == 0 ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(completedOrders), 0, RoundingMode.HALF_UP);
        return new SellerAnalyticsResponse(revenue, completedOrders, summary.getTotalOrders(), averageOrderValue, statuses, topProducts);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAdminAnalytics(Instant from, Instant to) {
        OrderRepository.AnalyticsSummary summary = orderRepository.getAnalyticsSummary(from, to);
        BigDecimal revenue = summary.getRevenue();
        long completedOrders = summary.getCompletedOrders();
        Map<String, Long> statuses = orderRepository.countByStatus(from, to).stream()
                .collect(Collectors.toMap(OrderRepository.StatusCount::getStatus, OrderRepository.StatusCount::getTotal));
        List<AdminAnalyticsResponse.RevenuePoint> daily = orderRepository.getDailyRevenue(from, to).stream()
                .map(point -> new AdminAnalyticsResponse.RevenuePoint(point.getDate(), point.getRevenue(), point.getTotal()))
                .toList();
        List<AdminAnalyticsResponse.TopProduct> topProducts = orderRepository.getTopProducts(from, to).stream()
                .map(product -> new AdminAnalyticsResponse.TopProduct(product.getProductId(), product.getName(), product.getQuantitySold(), product.getRevenue()))
                .toList();
        BigDecimal averageOrderValue = completedOrders == 0 ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(completedOrders), 0, RoundingMode.HALF_UP);
        return new AdminAnalyticsResponse(revenue, summary.getTotalOrders(), completedOrders, averageOrderValue, statuses, daily, topProducts);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = createOrderPageable(page, size);
        Page<Order> orderPage = orderRepository.findAll(pageable);

        List<OrderResponse> content = orderPage.getContent()
                .stream()
                .map(this::toOrderResponse)
                .toList();

        return toPageResponse(orderPage, content);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByPromotionCode(String promotionCode, int page, int size) {
        if (promotionCode == null || promotionCode.isBlank()) {
            throw new OrderServiceException(ErrorCode.PROMOTION_NOT_APPLICABLE);
        }
        Pageable pageable = createOrderPageable(page, size);
        Page<Order> orderPage = orderRepository.findByPromotionCodeIgnoreCase(promotionCode.trim(), pageable);
        List<OrderResponse> content = orderPage.getContent()
                .stream()
                .map(this::toOrderResponse)
                .toList();
        return toPageResponse(orderPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        return toOrderResponse(order);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetailForAdmin(String orderId) {
        return getOrderForPaymentValidation(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderForPaymentValidation(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        return toOrderResponse(order);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public OrderResponse searchOrderForAdmin(String query) {
        String value = query == null ? "" : query.trim();
        Order order = orderRepository.findById(value)
                .or(() -> orderRepository.findByOrderCodeIgnoreCase(value))
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));
        return toOrderResponse(order);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public OrderResponse updateOrderStatus(String orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(status);
        Order savedOrder = orderRepository.save(order);
        publishOrderStatusUpdatedEvent(savedOrder, oldStatus);
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateSellerOrderStatus(String sellerId, String orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .filter(value -> sellerId.equals(value.getSellerId()))
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.CONFIRMED || status != OrderStatus.SHIPPING) {
            throw new OrderServiceException(ErrorCode.SELLER_ORDER_TRANSITION_INVALID);
        }
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.SHIPPING);
        Order savedOrder = orderRepository.save(order);
        publishOrderStatusUpdatedEvent(savedOrder, oldStatus);
        publishShipmentRequestedEvent(savedOrder);
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String userId, String orderId, String token) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new OrderServiceException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            releaseInventory(order, token);
            safeReleasePromotion(order);
            safeReleaseFlashDeals(order);
            cartClient.release(order.getId());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        publishOrderCancelledEvent(savedOrder);
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public void startShippingFromCodPayment(CodPaymentCreatedEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.SHIPPING) {
            // COD checkout is complete from the buyer's perspective once the
            // order has entered shipping. This operation is idempotent, so a
            // replayed Kafka event also repairs an earlier failed cart cleanup.
            cartClient.finalize(order.getId());
            publishShipmentRequestedEvent(order);
            log.info("Shipment already requested for COD order: orderId={}, paymentId={}",
                    event.getOrderId(), event.getPaymentId());
            return;
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.info("Skip COD shipping transition because order is already progressed: orderId={}, paymentId={}, status={}",
                    event.getOrderId(), event.getPaymentId(), order.getStatus());
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("Skip COD shipping transition because order is not pending payment: orderId={}, paymentId={}, status={}",
                    event.getOrderId(), event.getPaymentId(), order.getStatus());
            return;
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.SHIPPING);
        Order savedOrder = orderRepository.save(order);
        cartClient.finalize(savedOrder.getId());
        publishOrderStatusUpdatedEvent(savedOrder, oldStatus);
        publishShipmentRequestedEvent(savedOrder);

        log.info("Order is shipping for COD payment: orderId={}, paymentId={}",
                event.getOrderId(), event.getPaymentId());
    }

    @Override
    @Transactional
    public void confirmOrderFromPaymentSuccess(PaymentSuccessEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus targetStatus = COD_METHOD.equals(event.getMethod())
                ? OrderStatus.COMPLETED
                : OrderStatus.CONFIRMED;

        if (order.getStatus() == targetStatus) {
            finalizePaymentSuccess(order);
            publishShipmentRequestedEvent(order);
            log.info("Skip payment success because order is already {}: orderId={}, paymentId={}",
                    targetStatus,
                    event.getOrderId(),
                    event.getPaymentId());
            return;
        }

        boolean codOrderReady = COD_METHOD.equals(event.getMethod())
                && (order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.SHIPPING);
        boolean onlineOrderReady = !COD_METHOD.equals(event.getMethod())
                && order.getStatus() == OrderStatus.PENDING_PAYMENT;
        if (!codOrderReady && !onlineOrderReady) {
            log.warn("Skip payment success because order is not ready for completion: orderId={}, paymentId={}, status={}",
                    event.getOrderId(),
                    event.getPaymentId(),
                    order.getStatus());
            return;
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(targetStatus);
        Order savedOrder = orderRepository.save(order);
        publishOrderStatusUpdatedEvent(savedOrder, oldStatus);
        finalizePaymentSuccess(savedOrder);
        publishShipmentRequestedEvent(savedOrder);

        log.info("Order {} from payment success: orderId={}, paymentId={}", targetStatus,
                event.getOrderId(),
                event.getPaymentId());
    }

    @Override
    @Transactional
    public void cancelOrderFromPaymentFailed(PaymentFailedEvent event) {
        cancelOrderFromPaymentEvent(event.getOrderId(), event.getPaymentId(), "failed");
    }

    @Override
    @Transactional
    public void cancelOrderFromPaymentCancelled(PaymentCancelledEvent event) {
        cancelOrderFromPaymentEvent(event.getOrderId(), event.getPaymentId(), "cancelled");
    }

    @Override
    @Transactional
    public void updateOrderFromShipment(ShipmentStatusUpdatedEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus targetStatus = switch (event.getNewStatus() == null
                ? ""
                : event.getNewStatus().toUpperCase()) {
            case "IN_TRANSIT" -> OrderStatus.SHIPPING;
            case "DELIVERY_FAILED" -> OrderStatus.DELIVERY_FAILED;
            case "RETURNING" -> OrderStatus.RETURNING;
            case "RETURNED" -> OrderStatus.RETURNED;
            case "DELIVERED" -> OrderStatus.COMPLETED;
            default -> null;
        };

        if (targetStatus == null) {
            log.debug("Shipment status does not change order status: orderId={}, shipmentStatus={}",
                    event.getOrderId(), event.getNewStatus());
            return;
        }

        if (order.getStatus() == targetStatus) {
            if (targetStatus == OrderStatus.RETURNED) {
                inventoryClient.confirmReturnedInventory(order.getId());
            }
            log.info("Skip duplicate shipment transition: orderId={}, orderStatus={}, shipmentStatus={}",
                    order.getId(), order.getStatus(), event.getNewStatus());
            return;
        }

        boolean allowed = switch (targetStatus) {
            case SHIPPING -> order.getStatus() == OrderStatus.CONFIRMED
                    || order.getStatus() == OrderStatus.DELIVERY_FAILED;
            case DELIVERY_FAILED -> order.getStatus() == OrderStatus.SHIPPING;
            case RETURNING -> order.getStatus() == OrderStatus.DELIVERY_FAILED;
            case RETURNED -> order.getStatus() == OrderStatus.RETURNING;
            case COMPLETED -> order.getStatus() == OrderStatus.SHIPPING;
            default -> false;
        };
        if (!allowed) {
            log.warn("Skip invalid shipment transition: orderId={}, orderStatus={}, shipmentStatus={}",
                    order.getId(), order.getStatus(), event.getNewStatus());
            return;
        }

        if (targetStatus == OrderStatus.RETURNED) {
            inventoryClient.confirmReturnedInventory(order.getId());
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(targetStatus);
        Order savedOrder = orderRepository.save(order);
        publishOrderStatusUpdatedEvent(savedOrder, oldStatus);
        log.info("Order status synchronized from shipment: orderId={}, oldStatus={}, newStatus={}",
                savedOrder.getId(), oldStatus, targetStatus);
    }

    private void cancelOrderFromPaymentEvent(String orderId, String paymentId, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            releasePaymentReservations(order);
            log.info("Skip payment {} because order is already cancelled: orderId={}, paymentId={}",
                    paymentStatus,
                    orderId,
                    paymentId);
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("Skip payment {} because order is not pending payment: orderId={}, paymentId={}, status={}",
                    paymentStatus,
                    orderId,
                    paymentId,
                    order.getStatus());
            return;
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        publishOrderStatusUpdatedEvent(savedOrder, oldStatus);
        publishOrderCancelledEvent(savedOrder);
        releasePaymentReservations(savedOrder);

        log.info("Order cancelled from payment {}: orderId={}, paymentId={}",
                paymentStatus,
                orderId,
                paymentId);
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getVariantId(),
                        item.getProductName(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getUserId(),
                order.getSellerId(),
                order.getShopId(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getPromotionCode(),
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getShippingAddress(),
                items,
                order.getCreatedAt()
        );
    }

    private Pageable createOrderPageable(int page, int size) {
        int currentPage = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(currentPage - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PageResponse<OrderResponse> toPageResponse(Page<Order> orderPage, List<OrderResponse> content) {
        return PageResponse.<OrderResponse>builder()
                .currentPage(orderPage.getNumber() + 1)
                .pageSize(orderPage.getSize())
                .totalPages(orderPage.getTotalPages())
                .totalElements(orderPage.getTotalElements())
                .content(content)
                .build();
    }

    private void publishOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .items(order.getItems()
                        .stream()
                        .map(item -> OrderItemEvent.builder()
                                .productId(item.getProductId())
                                .variantId(item.getVariantId())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .build();

        kafkaTemplate.send(ORDER_CREATED_TOPIC, order.getId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish OrderCreatedEvent: orderId={}", order.getId(), throwable);
                        return;
                    }

                    log.info("Published OrderCreatedEvent: orderId={}", order.getId());
                });
    }

    private void reserveInventory(Order order, String token) {
        ReserveInventoryRequest reserveRequest = new ReserveInventoryRequest(
                order.getId(),
                order.getItems()
                        .stream()
                        .map(item -> new ReserveInventoryItemRequest(
                                item.getProductId(),
                                item.getVariantId(),
                                item.getQuantity()
                        ))
                        .toList()
        );

        inventoryClient.reserveInventory(reserveRequest, token);
    }

    private void releaseInventory(Order order, String token) {
        inventoryClient.releaseInventory(new InventoryOrderRequest(order.getId()), token);
    }

    private boolean hasPromotion(String promotionCode) {
        return promotionCode != null && !promotionCode.isBlank();
    }

    private void safeReleaseInventory(Order order, String token) {
        try {
            releaseInventory(order, token);
        } catch (RuntimeException releaseException) {
            log.error("Failed to compensate inventory reservation: orderId={}", order.getId(), releaseException);
        }
    }

    private void safeReleasePromotion(Order order) {
        if (!hasPromotion(order.getPromotionCode())) return;
        try {
            promotionClient.release(order.getId());
        } catch (RuntimeException releaseException) {
            log.error("Failed to release promotion reservation: orderId={}", order.getId(), releaseException);
        }
    }

    /**
     * All downstream operations are idempotent by orderId. Re-running a Kafka
     * payment-success record therefore completes any previously interrupted step.
     */
    private void finalizePaymentSuccess(Order order) {
        if (hasFlashDeal(order)) promotionClient.confirmFlashDeals(order.getId());
        if (hasPromotion(order.getPromotionCode())) {
            promotionClient.confirm(order.getId());
        }
        inventoryClient.confirmInventory(new InventoryOrderRequest(order.getId()));
        cartClient.finalize(order.getId());
    }

    /**
     * Failed/cancelled payment returns every reservation and unlocks only the
     * cart items that belong to this order's checkout session.
     */
    private void releasePaymentReservations(Order order) {
        if (hasFlashDeal(order)) promotionClient.releaseFlashDeals(order.getId());
        if (hasPromotion(order.getPromotionCode())) {
            promotionClient.release(order.getId());
        }
        inventoryClient.releaseInventory(new InventoryOrderRequest(order.getId()));
        cartClient.release(order.getId());
    }

    private boolean hasFlashDeal(Order order) { return order.getItems().stream().anyMatch(item -> item.getPrice() != null && item.getOriginalPrice() != null && item.getPrice().compareTo(item.getOriginalPrice()) < 0); }

    private void applyFlashDealPrices(Order order, List<FlashDealPriceResponse> prices) {
        for (OrderItem item : order.getItems()) {
            prices.stream().filter(price -> price.productId().equals(item.getProductId()) && Objects.equals(price.variantId(), item.getVariantId())).findFirst().ifPresent(price -> {
                item.setOriginalPrice(item.getPrice());
                item.setPrice(price.salePrice());
                item.setSubtotal(price.salePrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            });
        }
        BigDecimal subtotal = order.getItems().stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotalAmount(subtotal);
        order.setTotalAmount(subtotal.subtract(order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount()).max(BigDecimal.ZERO));
    }

    private void safeReleaseFlashDeals(Order order) {
        try { promotionClient.releaseFlashDeals(order.getId()); }
        catch (RuntimeException exception) { log.error("Failed to release flash deal reservation: orderId={}", order.getId(), exception); }
    }

    @Override
    public OrderResponse checkout(String userId, CheckoutOrderRequest request, String token) {
        List<CartClient.CartItem> items = cartClient.checkoutItems(userId);
        if (items.isEmpty()) throw new OrderServiceException(ErrorCode.CART_CHECKOUT_EMPTY);
        OrderResponse order = createOrder(userId, new CreateOrderRequest(request.shippingAddress(), items.stream()
                .map(item -> new com.example.orderservice.dto.request.OrderItemRequest(item.productId(), item.variantId(), item.quantity())).toList()), token, request.campaignCode());
        if (OrderStatus.PENDING_PAYMENT.name().equals(order.status())) {
            cartClient.mark(userId, order.id(), items.stream().map(CartClient.CartItem::id).toList());
        }
        return order;
    }

    private ProductVariantResponse findVariant(ProductDetailResponse product, String variantId) {
        if (variantId == null || variantId.isBlank()) {
            return null;
        }

        if (product.variants() == null) {
            throw new OrderServiceException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        ProductVariantResponse variant = product.variants()
                .stream()
                .filter(item -> variantId.equals(item.id()))
                .findFirst()
                .orElseThrow(() -> new OrderServiceException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!"ACTIVE".equals(variant.status())) {
            throw new OrderServiceException(ErrorCode.PRODUCT_NOT_ACTIVE);
        }

        return variant;
    }

    private String formatVariantAttributes(ProductVariantResponse variant) {
        if (variant.attributes() == null || variant.attributes().isEmpty()) {
            return variant.sku();
        }

        return String.join(", ", variant.attributes().values());
    }

    private void publishOrderCancelledEvent(Order order) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .cancelledAt(Instant.now())
                .build();

        kafkaTemplate.send(ORDER_CANCELLED_TOPIC, order.getId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish OrderCancelledEvent: orderId={}", order.getId(), throwable);
                        return;
                    }

                    log.info("Published OrderCancelledEvent: orderId={}", order.getId());
                });
    }

    private void publishOrderStatusUpdatedEvent(Order order, OrderStatus oldStatus) {
        OrderStatus newStatus = order.getStatus();
        OrderStatusUpdatedEvent event = OrderStatusUpdatedEvent.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .oldStatus(oldStatus.name())
                .newStatus(newStatus.name())
                .updatedAt(Instant.now())
                .build();

        kafkaTemplate.send(ORDER_STATUS_UPDATED_TOPIC, order.getId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish OrderStatusUpdatedEvent: orderId={}, oldStatus={}, newStatus={}",
                                order.getId(),
                                oldStatus,
                                newStatus,
                                throwable);
                        return;
                    }

                    log.info("Published OrderStatusUpdatedEvent: orderId={}, oldStatus={}, newStatus={}",
                            order.getId(),
                            oldStatus,
                            newStatus);
                });
    }

    private void publishShipmentRequestedEvent(Order order) {
        ShipmentRequestedEvent event = ShipmentRequestedEvent.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .shippingAddress(order.getShippingAddress())
                .requestedAt(Instant.now())
                .build();

        kafkaTemplate.send(SHIPMENT_REQUESTED_TOPIC, order.getId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish ShipmentRequestedEvent: orderId={}", order.getId(), throwable);
                    } else {
                        log.info("Published ShipmentRequestedEvent: orderId={}", order.getId());
                    }
                });
    }

    private String generateOrderCode() {
        String orderCode;
        do {
            String date = ORDER_CODE_DATE_FORMAT.format(Instant.now());
            String suffix = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();
            orderCode = "ORD-" + date + "-" + suffix;
        } while (orderRepository.existsByOrderCode(orderCode));
        return orderCode;
    }
}
