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
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.InventoryReservationRepository;
import com.example.inventoryservice.service.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new InventoryServiceException(ErrorCode.INVENTORY_ALREADY_EXISTS);
        }

        Instant now = Instant.now();

        Inventory inventory = Inventory.builder()
                .productId(request.productId())
                .availableQuantity(request.availableQuantity())
                .reservedQuantity(0)
                .soldQuantity(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);

        return inventoryMapper.toResponse(savedInventory);
    }

    @Override
    public InventoryResponse getInventoryByProductId(String productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryServiceException(ErrorCode.INVENTORY_NOT_FOUND));

        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional
    public void reserveInventory(ReserveInventoryRequest request) {
        if (inventoryReservationRepository.existsByOrderId(request.orderId())) {
            throw new InventoryServiceException(ErrorCode.INVENTORY_ALREADY_RESERVED);
        }

        Instant now = Instant.now();

        for (ReserveInventoryItemRequest item : request.items()) {
            Inventory inventory = inventoryRepository.findByProductId(item.productId())
                    .orElseThrow(() -> new InventoryServiceException(ErrorCode.INVENTORY_NOT_FOUND));

            if (inventory.getAvailableQuantity() < item.quantity()) {
                throw new InventoryServiceException(ErrorCode.INSUFFICIENT_STOCK);
            }

            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - item.quantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.quantity());
            validateNonNegativeQuantities(inventory);

            InventoryReservation reservation = InventoryReservation.builder()
                    .orderId(request.orderId())
                    .productId(item.productId())
                    .quantity(item.quantity())
                    .status(ReservationStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            inventoryRepository.save(inventory);
            inventoryReservationRepository.save(reservation);
        }
    }

    @Override
    @Transactional
    public void confirmInventory(InventoryOrderRequest request) {
        List<InventoryReservation> reservations = getReservationsOrThrow(request.orderId());
        List<InventoryReservation> pendingReservations = filterPendingReservations(reservations);

        if (pendingReservations.isEmpty()) {
            return;
        }

        for (InventoryReservation reservation : pendingReservations) {
            Inventory inventory = inventoryRepository.findByProductId(reservation.getProductId())
                    .orElseThrow(() -> new InventoryServiceException(ErrorCode.INVENTORY_NOT_FOUND));

            ensureReservedQuantityEnough(inventory, reservation);
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventory.setSoldQuantity(inventory.getSoldQuantity() + reservation.getQuantity());
            validateNonNegativeQuantities(inventory);
            reservation.setStatus(ReservationStatus.CONFIRMED);

            inventoryRepository.save(inventory);
            inventoryReservationRepository.save(reservation);
        }
    }

    @Override
    @Transactional
    public void releaseInventory(InventoryOrderRequest request) {
        List<InventoryReservation> reservations = getReservationsOrThrow(request.orderId());
        List<InventoryReservation> pendingReservations = filterPendingReservations(reservations);

        if (pendingReservations.isEmpty()) {
            return;
        }

        for (InventoryReservation reservation : pendingReservations) {
            Inventory inventory = inventoryRepository.findByProductId(reservation.getProductId())
                    .orElseThrow(() -> new InventoryServiceException(ErrorCode.INVENTORY_NOT_FOUND));

            ensureReservedQuantityEnough(inventory, reservation);
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
            validateNonNegativeQuantities(inventory);
            reservation.setStatus(ReservationStatus.RELEASED);

            inventoryRepository.save(inventory);
            inventoryReservationRepository.save(reservation);
        }
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
}
