package com.example.productservice.controller;

import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/products")
public class InternalProductOwnershipController {
    private final ProductRepository productRepository;

    @GetMapping("/ownership")
    public ProductOwnershipResponse ownsAll(@RequestParam String sellerId, @RequestParam List<String> productIds) {
        List<String> distinctIds = productIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        boolean owned = !distinctIds.isEmpty() && productRepository.countByIdInAndSellerId(distinctIds, sellerId) == distinctIds.size();
        return new ProductOwnershipResponse(owned);
    }

    public record ProductOwnershipResponse(boolean owned) { }
}
