package com.example.sellerservice.service.implement;

import com.example.sellerservice.dto.request.CreateSellerShopRequest;
import com.example.sellerservice.dto.request.ReviewSellerShopRequest;
import com.example.sellerservice.client.IdentityRoleClient;
import com.example.sellerservice.entity.SellerReviewAction;
import com.example.sellerservice.entity.SellerShop;
import com.example.sellerservice.entity.SellerStatus;
import com.example.sellerservice.exception.ErrorCode;
import com.example.sellerservice.exception.SellerServiceException;
import com.example.sellerservice.repository.SellerShopRepository;
import com.example.sellerservice.messaging.SellerShopEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SellerShopServiceImplTest {
    @Mock
    private SellerShopRepository sellerShopRepository;

    @Mock
    private IdentityRoleClient identityRoleClient;

    @Mock
    private SellerShopEventPublisher sellerShopEventPublisher;

    @InjectMocks
    private SellerShopServiceImpl sellerShopService;

    @Test
    void createMyShopAlwaysStartsPending() {
        when(sellerShopRepository.findByOwnerUserId("seller-1")).thenReturn(Optional.empty());
        when(sellerShopRepository.save(any(SellerShop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = sellerShopService.createMyShop("seller-1", new CreateSellerShopRequest(
                "Nova Store", "Shop đồ công nghệ", "0900000000", "1 Nguyen Hue", "Ho Chi Minh"
        ));

        ArgumentCaptor<SellerShop> savedShop = ArgumentCaptor.forClass(SellerShop.class);
        verify(sellerShopRepository).save(savedShop.capture());
        assertEquals("seller-1", savedShop.getValue().getOwnerUserId());
        assertEquals(SellerStatus.PENDING, savedShop.getValue().getStatus());
        assertEquals(SellerStatus.PENDING, response.status());
    }

    @Test
    void rejectRequiresReason() {
        SellerShop shop = SellerShop.builder()
                .id(UUID.randomUUID())
                .ownerUserId("seller-1")
                .slug("nova-store")
                .shopName("Nova Store")
                .status(SellerStatus.PENDING)
                .build();
        when(sellerShopRepository.findById(shop.getId())).thenReturn(Optional.of(shop));

        SellerServiceException exception = assertThrows(SellerServiceException.class,
                () -> sellerShopService.reviewShop(shop.getId(), "admin-1",
                        "Bearer admin-access-token",
                        new ReviewSellerShopRequest(SellerReviewAction.REJECT, " ")));

        assertEquals(ErrorCode.REVIEW_NOTE_REQUIRED, exception.getErrorCode());
        verify(sellerShopRepository).findById(eq(shop.getId()));
        verifyNoInteractions(identityRoleClient, sellerShopEventPublisher);
    }

    @Test
    void approvingShopGrantsSellerRoleAndPublishesStatusChange() {
        SellerShop shop = SellerShop.builder()
                .id(UUID.randomUUID())
                .ownerUserId("seller-1")
                .slug("nova-store")
                .shopName("Nova Store")
                .status(SellerStatus.PENDING)
                .build();
        when(sellerShopRepository.findById(shop.getId())).thenReturn(Optional.of(shop));
        when(sellerShopRepository.save(any(SellerShop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = sellerShopService.reviewShop(shop.getId(), "admin-1", "Bearer admin-access-token",
                new ReviewSellerShopRequest(SellerReviewAction.APPROVE, null));

        assertEquals(SellerStatus.APPROVED, response.status());
        verify(identityRoleClient).grantSellerRole("seller-1", "Bearer admin-access-token");
        verify(sellerShopEventPublisher).publishStatusChanged(any());
    }
}
