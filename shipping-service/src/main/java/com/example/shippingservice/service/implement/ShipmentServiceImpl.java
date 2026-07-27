package com.example.shippingservice.service.implement;

import com.example.event.ShipmentStatusUpdatedEvent;
import com.example.shippingservice.dto.request.AssignCarrierRequest;
import com.example.shippingservice.dto.request.CreateShipmentRequest;
import com.example.shippingservice.dto.request.UpdateShipmentStatusRequest;
import com.example.shippingservice.dto.response.ShipmentResponse;
import com.example.shippingservice.entity.Shipment;
import com.example.shippingservice.entity.ShipmentHistory;
import com.example.shippingservice.entity.ShipmentStatus;
import com.example.shippingservice.exception.ErrorCode;
import com.example.shippingservice.exception.ShippingServiceException;
import com.example.shippingservice.mapper.ShipmentMapper;
import com.example.shippingservice.repository.ShipmentHistoryRepository;
import com.example.shippingservice.repository.ShipmentRepository;
import com.example.shippingservice.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "SHIPPING-SERVICE")
public class ShipmentServiceImpl implements ShipmentService {

    private static final String SHIPMENT_STATUS_UPDATED_TOPIC = "shipment-status-updated";
    private static final Set<ShipmentStatus> CANCELLABLE_STATUSES = Set.of(
            ShipmentStatus.CREATED,
            ShipmentStatus.PACKING,
            ShipmentStatus.READY_TO_SHIP
    );

    private final ShipmentRepository shipmentRepository;
    private final ShipmentHistoryRepository shipmentHistoryRepository;
    private final ShipmentMapper shipmentMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public ShipmentResponse create(CreateShipmentRequest request) {
        return shipmentRepository.findByOrderId(request.orderId())
                .map(this::toResponse)
                .orElseGet(() -> createNewShipment(request));
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getByOrderId(String orderId) {
        return toResponse(findByOrderId(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getByOrderIdForUser(String orderId, String userId) {
        Shipment shipment = shipmentRepository.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ShippingServiceException(ErrorCode.SHIPMENT_NOT_FOUND));
        return toResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentResponse> getMyShipments(String userId, Pageable pageable) {
        return shipmentRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentResponse> getAll(ShipmentStatus status, Pageable pageable) {
        Page<Shipment> shipments = status == null
                ? shipmentRepository.findAllByOrderByCreatedAtDesc(pageable)
                : shipmentRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
        return shipments.map(this::toResponse);
    }

    @Override
    @Transactional
    public ShipmentResponse assignCarrier(UUID shipmentId, AssignCarrierRequest request) {
        Shipment shipment = findById(shipmentId);
        if (!CANCELLABLE_STATUSES.contains(shipment.getStatus())) {
            throw new ShippingServiceException(ErrorCode.INVALID_SHIPMENT_TRANSITION);
        }

        String trackingNumber = request.trackingNumber().trim();
        if (shipmentRepository.existsByTrackingNumberAndIdNot(trackingNumber, shipmentId)) {
            throw new ShippingServiceException(ErrorCode.TRACKING_NUMBER_EXISTS);
        }

        shipment.setCarrier(request.carrier().trim());
        shipment.setTrackingNumber(trackingNumber);
        shipment.setEstimatedDeliveryAt(request.estimatedDeliveryAt());
        Shipment saved = shipmentRepository.save(shipment);
        addHistory(saved, saved.getStatus(), "Carrier assigned: " + saved.getCarrier(), null);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ShipmentResponse startPacking(UUID shipmentId, UpdateShipmentStatusRequest request) {
        return transition(shipmentId, ShipmentStatus.PACKING, Set.of(ShipmentStatus.CREATED), request);
    }

    @Override
    @Transactional
    public ShipmentResponse readyToShip(UUID shipmentId, UpdateShipmentStatusRequest request) {
        return transition(shipmentId, ShipmentStatus.READY_TO_SHIP, Set.of(ShipmentStatus.PACKING), request);
    }

    @Override
    @Transactional
    public ShipmentResponse ship(UUID shipmentId, UpdateShipmentStatusRequest request) {
        Shipment shipment = findById(shipmentId);
        if (shipment.getStatus() == ShipmentStatus.IN_TRANSIT) {
            return toResponse(shipment);
        }
        if (shipment.getStatus() != ShipmentStatus.READY_TO_SHIP
                && shipment.getStatus() != ShipmentStatus.DELIVERY_FAILED) {
            throw new ShippingServiceException(ErrorCode.INVALID_SHIPMENT_TRANSITION);
        }
        if (isBlank(shipment.getCarrier()) || isBlank(shipment.getTrackingNumber())) {
            throw new ShippingServiceException(ErrorCode.INVALID_REQUEST);
        }

        ShipmentStatus oldStatus = shipment.getStatus();
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        if (shipment.getShippedAt() == null) {
            shipment.setShippedAt(Instant.now());
        }
        Shipment saved = shipmentRepository.save(shipment);
        addHistory(saved, ShipmentStatus.IN_TRANSIT, description(request, "Shipment is in transit"), request.location());
        publishStatusUpdated(saved, oldStatus, request);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ShipmentResponse deliver(UUID shipmentId, UpdateShipmentStatusRequest request) {
        Shipment shipment = findById(shipmentId);
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            return toResponse(shipment);
        }
        // The current workflow does not persist an assignee. A shipper can
        // therefore confirm a delivery directly from the operational queue
        // without first recording packing/carrier hand-off transitions.
        if (!Set.of(
                ShipmentStatus.CREATED,
                ShipmentStatus.PACKING,
                ShipmentStatus.READY_TO_SHIP,
                ShipmentStatus.IN_TRANSIT
        ).contains(shipment.getStatus())) {
            throw new ShippingServiceException(ErrorCode.INVALID_SHIPMENT_TRANSITION);
        }

        ShipmentStatus oldStatus = shipment.getStatus();
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(Instant.now());
        Shipment saved = shipmentRepository.save(shipment);
        addHistory(saved, ShipmentStatus.DELIVERED, description(request, "Shipment delivered"), request.location());
        publishStatusUpdated(saved, oldStatus, request);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ShipmentResponse markDeliveryFailed(UUID shipmentId, UpdateShipmentStatusRequest request) {
        return transition(shipmentId, ShipmentStatus.DELIVERY_FAILED, Set.of(ShipmentStatus.IN_TRANSIT), request);
    }

    @Override
    @Transactional
    public ShipmentResponse startReturning(UUID shipmentId, UpdateShipmentStatusRequest request) {
        return transition(shipmentId, ShipmentStatus.RETURNING, Set.of(ShipmentStatus.DELIVERY_FAILED), request);
    }

    @Override
    @Transactional
    public ShipmentResponse markReturned(UUID shipmentId, UpdateShipmentStatusRequest request) {
        return transition(shipmentId, ShipmentStatus.RETURNED, Set.of(ShipmentStatus.RETURNING), request);
    }

    @Override
    @Transactional
    public ShipmentResponse cancel(UUID shipmentId, UpdateShipmentStatusRequest request) {
        return transition(shipmentId, ShipmentStatus.CANCELLED, CANCELLABLE_STATUSES, request);
    }

    @Override
    @Transactional
    public void cancelByOrderId(String orderId) {
        shipmentRepository.findByOrderId(orderId).ifPresentOrElse(shipment -> {
            if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
                log.info("Skip duplicate order cancellation: shipmentId={}, orderId={}",
                        shipment.getId(), orderId);
                return;
            }
            if (!CANCELLABLE_STATUSES.contains(shipment.getStatus())) {
                log.warn("Shipment cannot be cancelled after delivery started: shipmentId={}, orderId={}, status={}",
                        shipment.getId(), orderId, shipment.getStatus());
                return;
            }
            transition(
                    shipment.getId(),
                    ShipmentStatus.CANCELLED,
                    CANCELLABLE_STATUSES,
                    new UpdateShipmentStatusRequest("Order was cancelled", null)
            );
            log.info("Cancelled shipment from OrderCancelledEvent: shipmentId={}, orderId={}",
                    shipment.getId(), orderId);
        }, () -> log.info("No shipment to cancel for orderId={}", orderId));
    }

    private ShipmentResponse createNewShipment(CreateShipmentRequest request) {
        Shipment shipment = shipmentMapper.toEntity(request);
        shipment.setStatus(ShipmentStatus.CREATED);
        Shipment saved = shipmentRepository.save(shipment);
        addHistory(saved, ShipmentStatus.CREATED, "Shipment created", null);
        log.info("Created shipment: shipmentId={}, orderId={}", saved.getId(), saved.getOrderId());
        return toResponse(saved);
    }

    private ShipmentResponse transition(
            UUID shipmentId,
            ShipmentStatus targetStatus,
            Set<ShipmentStatus> allowedCurrentStatuses,
            UpdateShipmentStatusRequest request
    ) {
        Shipment shipment = findById(shipmentId);
        if (shipment.getStatus() == targetStatus) {
            return toResponse(shipment);
        }
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new ShippingServiceException(ErrorCode.SHIPMENT_ALREADY_DELIVERED);
        }
        if (!allowedCurrentStatuses.contains(shipment.getStatus())) {
            throw new ShippingServiceException(ErrorCode.INVALID_SHIPMENT_TRANSITION);
        }

        ShipmentStatus oldStatus = shipment.getStatus();
        shipment.setStatus(targetStatus);
        Shipment saved = shipmentRepository.save(shipment);
        addHistory(saved, targetStatus, description(request, defaultDescription(targetStatus)), request.location());
        publishStatusUpdated(saved, oldStatus, request);
        return toResponse(saved);
    }

    private void addHistory(
            Shipment shipment,
            ShipmentStatus status,
            String description,
            String location
    ) {
        shipmentHistoryRepository.save(ShipmentHistory.builder()
                .shipment(shipment)
                .status(status)
                .description(description)
                .location(location)
                .occurredAt(Instant.now())
                .build());
    }

    private void publishStatusUpdated(
            Shipment shipment,
            ShipmentStatus oldStatus,
            UpdateShipmentStatusRequest request
    ) {
        ShipmentStatusUpdatedEvent event = ShipmentStatusUpdatedEvent.builder()
                .shipmentId(shipment.getId())
                .orderId(shipment.getOrderId())
                .userId(shipment.getUserId())
                .oldStatus(oldStatus.name())
                .newStatus(shipment.getStatus().name())
                .carrier(shipment.getCarrier())
                .trackingNumber(shipment.getTrackingNumber())
                .description(request.description())
                .location(request.location())
                .updatedAt(Instant.now())
                .build();

        kafkaTemplate.send(SHIPMENT_STATUS_UPDATED_TOPIC, shipment.getOrderId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish shipment status: shipmentId={}, status={}",
                                shipment.getId(), shipment.getStatus(), throwable);
                    } else {
                        log.info("Published shipment status: shipmentId={}, oldStatus={}, newStatus={}",
                                shipment.getId(), oldStatus, shipment.getStatus());
                    }
                });
    }

    private Shipment findById(UUID shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShippingServiceException(ErrorCode.SHIPMENT_NOT_FOUND));
    }

    private Shipment findByOrderId(String orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ShippingServiceException(ErrorCode.SHIPMENT_NOT_FOUND));
    }

    private ShipmentResponse toResponse(Shipment shipment) {
        return shipmentMapper.toResponse(
                shipment,
                shipmentHistoryRepository.findAllByShipmentIdOrderByOccurredAtAsc(shipment.getId())
        );
    }

    private String description(UpdateShipmentStatusRequest request, String fallback) {
        return isBlank(request.description()) ? fallback : request.description().trim();
    }

    private String defaultDescription(ShipmentStatus status) {
        return switch (status) {
            case PACKING -> "Order is being packed";
            case READY_TO_SHIP -> "Shipment is ready for carrier pickup";
            case DELIVERY_FAILED -> "Delivery attempt failed";
            case RETURNING -> "Shipment is returning to sender";
            case RETURNED -> "Shipment returned to sender";
            case CANCELLED -> "Shipment cancelled";
            default -> "Shipment status updated to " + status;
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
