package com.example.inventoryservice.service.implement;


import com.example.inventoryservice.dto.request.CreateInventoryRequest;
import com.example.inventoryservice.dto.request.InventoryOrderRequest;
import com.example.inventoryservice.dto.request.ReserveInventoryItemRequest;
import com.example.inventoryservice.dto.request.ReserveInventoryRequest;
import com.example.inventoryservice.dto.response.InventoryResponse;
import com.example.inventoryservice.dto.response.ReservationResponse;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.enums.ReservationStatus;
import com.example.inventoryservice.exception.ErrorCode;
import com.example.inventoryservice.exception.InventoryServiceException;
import com.example.inventoryservice.mapper.InventoryMapper;
import com.example.inventoryservice.messaging.OutboxPublisher;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.InventoryReservationRepository;
import com.example.inventoryservice.service.InventoryService;
import event.InventoryUpdatedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "INVENTORY-SERVICE")
public class InventoryServiceImpl implements InventoryService {

    private static final String INVENTORY_UPDATED_TOPIC = "inventory-updated";

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryMapper inventoryMapper;
    private final OutboxPublisher outboxPublisher;

    @Override
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        if (request.variantId() != null && !request.variantId().isBlank()
                && inventoryRepository.existsByVariantId(request.variantId())) {
            throw new InventoryServiceException(ErrorCode.INVENTORY_ALREADY_EXISTS);
        }

        if ((request.variantId() == null || request.variantId().isBlank())
                && inventoryRepository.existsByProductIdAndVariantIdIsNull(request.productId())) {
            throw new InventoryServiceException(ErrorCode.INVENTORY_ALREADY_EXISTS);
        }

        Instant now = Instant.now();

        Inventory inventory = Inventory.builder()
                .productId(request.productId())
                .variantId(normalizeVariantId(request.variantId()))
                .availableQuantity(request.availableQuantity())
                .reservedQuantity(0)
                .soldQuantity(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);
        publishInventoryUpdatedEvent(savedInventory);

        return inventoryMapper.toResponse(savedInventory);
    }

    @Override
    @Transactional
    public InventoryResponse setAvailableQuantity(String productId, String variantId, Integer availableQuantity) {
        String normalizedVariantId = normalizeVariantId(variantId);
        Inventory inventory = normalizedVariantId == null
                ? inventoryRepository.findByProductIdAndVariantIdIsNull(productId).orElse(null)
                : inventoryRepository.findByVariantId(normalizedVariantId).orElse(null);

        if (inventory == null) {
            inventory = Inventory.builder()
                    .productId(productId)
                    .variantId(normalizedVariantId)
                    .availableQuantity(availableQuantity)
                    .reservedQuantity(0)
                    .soldQuantity(0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        } else {
            inventory.setAvailableQuantity(availableQuantity);
            inventory.setUpdatedAt(Instant.now());
        }

        Inventory savedInventory = inventoryRepository.save(inventory);
        publishInventoryUpdatedEvent(savedInventory);
        return inventoryMapper.toResponse(savedInventory);
    }

    @Override
    public InventoryResponse getInventoryByProductId(String productId) {
        Inventory inventory = inventoryRepository.findByProductIdAndVariantIdIsNull(productId)
                .orElseThrow(() -> new InventoryServiceException(ErrorCode.INVENTORY_NOT_FOUND));

        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public List<InventoryResponse> getInventoriesByProductIds(List<String> productIds) {
        List<String> distinctProductIds = productIds.stream()
                .distinct()
                .toList();

        return inventoryRepository.findByProductIdInAndVariantIdIsNull(distinctProductIds)
                .stream()
                .map(inventoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void reserveInventory(ReserveInventoryRequest request) {
        if (inventoryReservationRepository.existsByOrderId(request.orderId())) {
            throw new InventoryServiceException(ErrorCode.INVENTORY_ALREADY_RESERVED);
        }

        Instant now = Instant.now();

        for (ReserveInventoryItemRequest item : request.items()) {
            Inventory inventory = findInventoryForUpdate(item.productId(), item.variantId());
            if (inventory.getAvailableQuantity() < item.quantity()) {
                throw new InventoryServiceException(ErrorCode.INSUFFICIENT_STOCK);
            }

            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - item.quantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.quantity());
            validateNonNegativeQuantities(inventory);

            InventoryReservation reservation = InventoryReservation.builder()
                    .orderId(request.orderId())
                    .productId(item.productId())
                    .variantId(normalizeVariantId(item.variantId()))
                    .quantity(item.quantity())
                    .status(ReservationStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            inventoryRepository.save(inventory);
            inventoryReservationRepository.save(reservation);
            publishInventoryUpdatedEvent(inventory);
        }
    }

    @Override
    @Transactional
    public void confirmInventory(InventoryOrderRequest request) {
        List<InventoryReservation> reservations = getReservationsForUpdateOrThrow(request.orderId());
        List<InventoryReservation> pendingReservations = filterPendingReservations(reservations);

        if (pendingReservations.isEmpty()) {
            return;
        }

        for (InventoryReservation reservation : pendingReservations) {
            Inventory inventory = findInventoryForUpdate(reservation.getProductId(), reservation.getVariantId());

            ensureReservedQuantityEnough(inventory, reservation);
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventory.setSoldQuantity(inventory.getSoldQuantity() + reservation.getQuantity());
            validateNonNegativeQuantities(inventory);
            reservation.setStatus(ReservationStatus.CONFIRMED);

            inventoryRepository.save(inventory);
            inventoryReservationRepository.save(reservation);
            publishInventoryUpdatedEvent(inventory);
        }
    }

    @Override
    @Transactional
    public void releaseInventory(InventoryOrderRequest request) {
        List<InventoryReservation> reservations = getReservationsForUpdateOrThrow(request.orderId());
        List<InventoryReservation> pendingReservations = filterPendingReservations(reservations);

        if (pendingReservations.isEmpty()) {
            return;
        }

        for (InventoryReservation reservation : pendingReservations) {
            Inventory inventory = findInventoryForUpdate(reservation.getProductId(), reservation.getVariantId());

            ensureReservedQuantityEnough(inventory, reservation);
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
            validateNonNegativeQuantities(inventory);
            reservation.setStatus(ReservationStatus.RELEASED);

            inventoryRepository.save(inventory);
            inventoryReservationRepository.save(reservation);
            publishInventoryUpdatedEvent(inventory);
        }
    }

    @Override
    @Transactional
    public void confirmReturnedInventory(String orderId) {
        List<InventoryReservation> reservations = inventoryReservationRepository.findByOrderIdForUpdate(orderId);
        if (reservations.isEmpty()) {
            throw new InventoryServiceException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        for (InventoryReservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.RETURNED
                    || reservation.getStatus() == ReservationStatus.RELEASED) {
                continue;
            }

            Inventory inventory = findInventoryForUpdate(
                    reservation.getProductId(),
                    reservation.getVariantId()
            );

            if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
                if (inventory.getSoldQuantity() < reservation.getQuantity()) {
                    throw new InventoryServiceException(ErrorCode.INVALID_INVENTORY_REQUEST);
                }
                inventory.setSoldQuantity(inventory.getSoldQuantity() - reservation.getQuantity());
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
            } else if (reservation.getStatus() == ReservationStatus.PENDING) {
                ensureReservedQuantityEnough(inventory, reservation);
                inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
            } else {
                throw new InventoryServiceException(ErrorCode.INVALID_INVENTORY_REQUEST);
            }

            validateNonNegativeQuantities(inventory);
            reservation.setStatus(ReservationStatus.RETURNED);
            inventoryRepository.save(inventory);
            inventoryReservationRepository.save(reservation);
            publishInventoryUpdatedEvent(inventory);
        }

        log.info("Confirmed returned inventory: orderId={}, reservationCount={}",
                orderId, reservations.size());
    }

    @Override
    public List<ReservationResponse> getReservationsByOrderId(String orderId) {
        return getReservationsOrThrow(orderId)
                .stream()
                .map(inventoryMapper::toResponse)
                .toList();
    }

    private List<InventoryReservation> getReservationsOrThrow(String orderId) {
        List<InventoryReservation> reservations = inventoryReservationRepository.findByOrderId(orderId);

        if (reservations.isEmpty()) {
            throw new InventoryServiceException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        return reservations;
    }

    private List<InventoryReservation> getReservationsForUpdateOrThrow(String orderId) {
        List<InventoryReservation> reservations = inventoryReservationRepository.findByOrderIdForUpdate(orderId);

        if (reservations.isEmpty()) {
            throw new InventoryServiceException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        return reservations;
    }

    private List<InventoryReservation> filterPendingReservations(List<InventoryReservation> reservations) {
        return reservations.stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
                .toList();
    }

    private void ensureReservedQuantityEnough(Inventory inventory, InventoryReservation reservation) {
        if (inventory.getReservedQuantity() < reservation.getQuantity()) {
            throw new InventoryServiceException(ErrorCode.INVALID_INVENTORY_REQUEST);
        }
    }

    private void validateNonNegativeQuantities(Inventory inventory) {
        if (inventory.getAvailableQuantity() < 0
                || inventory.getReservedQuantity() < 0
                || inventory.getSoldQuantity() < 0) {
            throw new InventoryServiceException(ErrorCode.INVALID_INVENTORY_REQUEST);
        }
    }

    private Inventory findInventoryForUpdate(String productId, String variantId) {
        String normalizedVariantId = normalizeVariantId(variantId);
        if (normalizedVariantId != null) {
            return inventoryRepository.findByVariantIdForUpdate(normalizedVariantId)
                    .orElseThrow(() -> new InventoryServiceException(ErrorCode.INVENTORY_NOT_FOUND));
        }

        return inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new InventoryServiceException(ErrorCode.INVENTORY_NOT_FOUND));
    }

    private Inventory findInventory(String productId, String variantId) {
        String normalizedVariantId = normalizeVariantId(variantId);
        if (normalizedVariantId != null) {
            return inventoryRepository.findByVariantId(normalizedVariantId)
                    .orElseThrow(() -> new InventoryServiceException(ErrorCode.INVENTORY_NOT_FOUND));
        }

        return inventoryRepository.findByProductIdAndVariantIdIsNull(productId)
                .orElseThrow(() -> new InventoryServiceException(ErrorCode.INVENTORY_NOT_FOUND));
    }

    private String normalizeVariantId(String variantId) {
        if (variantId == null || variantId.isBlank()) {
            return null;
        }

        return variantId.trim();
    }

    private void publishInventoryUpdatedEvent(Inventory inventory) {
        InventoryUpdatedEvent event = InventoryUpdatedEvent.builder()
                .productId(inventory.getProductId())
                .variantId(inventory.getVariantId())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .soldQuantity(inventory.getSoldQuantity())
                .inStock(inventory.getAvailableQuantity() != null && inventory.getAvailableQuantity() > 0)
                .updatedAt(Instant.now())
                .build();

        outboxPublisher.enqueue(INVENTORY_UPDATED_TOPIC, inventory.getProductId(), event);
    }
}
