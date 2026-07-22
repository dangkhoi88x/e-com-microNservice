package com.example.shippingservice.mapper;

import com.example.shippingservice.dto.request.CreateShipmentRequest;
import com.example.shippingservice.dto.response.ShipmentHistoryResponse;
import com.example.shippingservice.dto.response.ShipmentResponse;
import com.example.shippingservice.entity.Shipment;
import com.example.shippingservice.entity.ShipmentHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "carrier", ignore = true)
    @Mapping(target = "trackingNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "estimatedDeliveryAt", ignore = true)
    @Mapping(target = "shippedAt", ignore = true)
    @Mapping(target = "deliveredAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Shipment toEntity(CreateShipmentRequest request);

    @Mapping(target = "timeline", source = "histories")
    ShipmentResponse toResponse(Shipment shipment, List<ShipmentHistory> histories);

    @Mapping(target = "shipmentId", source = "shipment.id")
    ShipmentHistoryResponse toHistoryResponse(ShipmentHistory history);

    List<ShipmentHistoryResponse> toHistoryResponses(List<ShipmentHistory> histories);
}
