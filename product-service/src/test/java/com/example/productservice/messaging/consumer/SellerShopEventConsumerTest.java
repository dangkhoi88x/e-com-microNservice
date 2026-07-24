package com.example.productservice.messaging.consumer;

import com.example.productservice.service.ProductService;
import event.SellerShopStatusChangedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SellerShopEventConsumerTest {
    private final ProductService productService = mock(ProductService.class);
    private final SellerShopEventConsumer consumer = new SellerShopEventConsumer(productService);

    @Test
    void inactivatesProductsOnlyWhenShopIsSuspended() {
        consumer.sellerShopStatusChanged(SellerShopStatusChangedEvent.builder()
                .shopId("shop-1").status("SUSPENDED").build());

        verify(productService).inactivateProductsForSuspendedShop("shop-1");
    }

    @Test
    void ignoresOtherShopStatusChanges() {
        consumer.sellerShopStatusChanged(SellerShopStatusChangedEvent.builder()
                .shopId("shop-1").status("APPROVED").build());

        verifyNoInteractions(productService);
    }
}
