package com.example.sellerservice.service.implement;

import com.example.sellerservice.dto.request.CreateSellerShopRequest;
import com.example.sellerservice.client.IdentityRoleClient;
import com.example.sellerservice.dto.request.ReviewSellerShopRequest;
import com.example.sellerservice.dto.request.UpdateSellerShopRequest;
import com.example.sellerservice.dto.response.SellerEligibilityResponse;
import com.example.sellerservice.dto.response.SellerShopResponse;
import com.example.sellerservice.entity.SellerReviewAction;
import com.example.sellerservice.entity.SellerShop;
import com.example.sellerservice.entity.SellerStatus;
import com.example.sellerservice.exception.ErrorCode;
import com.example.sellerservice.exception.SellerServiceException;
import com.example.sellerservice.repository.SellerShopRepository;
import com.example.sellerservice.messaging.SellerShopEventPublisher;
import com.example.sellerservice.service.SellerShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import event.SellerShopStatusChangedEvent;

@Service
@RequiredArgsConstructor
public class SellerShopServiceImpl implements SellerShopService {
    private final SellerShopRepository sellerShopRepository;
    private final IdentityRoleClient identityRoleClient;
    private final SellerShopEventPublisher sellerShopEventPublisher;

    @Override
    @Transactional
    public SellerShopResponse createMyShop(String ownerUserId, CreateSellerShopRequest request) {
        if (sellerShopRepository.findByOwnerUserId(ownerUserId).isPresent()) {
            throw new SellerServiceException(ErrorCode.SELLER_SHOP_ALREADY_EXISTS);
        }

        SellerShop shop = SellerShop.builder()
                .ownerUserId(ownerUserId)
                .slug(uniqueSlug(request.shopName()))
                .shopName(request.shopName().trim())
                .description(trimToNull(request.description()))
                .phone(request.phone().trim())
                .address(request.address().trim())
                .city(request.city().trim())
                .status(SellerStatus.PENDING)
                .build();
        return toResponse(sellerShopRepository.save(shop));
    }

    @Override
    @Transactional(readOnly = true)
    public SellerShopResponse getMyShop(String ownerUserId) {
        return toResponse(findByOwnerUserId(ownerUserId));
    }

    @Override
    @Transactional
    public SellerShopResponse updateMyShop(String ownerUserId, UpdateSellerShopRequest request) {
        SellerShop shop = findByOwnerUserId(ownerUserId);
        shop.setShopName(request.shopName().trim());
        shop.setDescription(trimToNull(request.description()));
        shop.setPhone(request.phone().trim());
        shop.setAddress(request.address().trim());
        shop.setCity(request.city().trim());
        return toResponse(sellerShopRepository.save(shop));
    }

    @Override
    @Transactional
    public SellerShopResponse resubmitMyShop(String ownerUserId) {
        SellerShop shop = findByOwnerUserId(ownerUserId);
        if (shop.getStatus() != SellerStatus.REJECTED) {
            throw new SellerServiceException(ErrorCode.INVALID_SELLER_TRANSITION);
        }
        shop.setStatus(SellerStatus.PENDING);
        shop.setReviewNote(null);
        shop.setReviewedBy(null);
        shop.setReviewedAt(null);
        return toResponse(sellerShopRepository.save(shop));
    }

    @Override
    @Transactional(readOnly = true)
    public SellerEligibilityResponse getMyEligibility(String ownerUserId) {
        return sellerShopRepository.findByOwnerUserId(ownerUserId)
                .map(shop -> new SellerEligibilityResponse(
                        shop.getStatus() == SellerStatus.APPROVED,
                        shop.getId(),
                        shop.getStatus()
                ))
                .orElse(new SellerEligibilityResponse(false, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SellerShopResponse> getAll(SellerStatus status, Pageable pageable) {
        Page<SellerShop> shops = status == null
                ? sellerShopRepository.findAll(pageable)
                : sellerShopRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
        return shops.map(this::toResponse);
    }

    @Override
    @Transactional
    public SellerShopResponse reviewShop(
            UUID shopId,
            String adminUserId,
            String authorization,
            ReviewSellerShopRequest request
    ) {
        SellerShop shop = sellerShopRepository.findById(shopId)
                .orElseThrow(() -> new SellerServiceException(ErrorCode.SELLER_SHOP_NOT_FOUND));
        SellerStatus targetStatus = targetStatus(shop.getStatus(), request.action());
        String note = trimToNull(request.note());
        if ((request.action() == SellerReviewAction.REJECT || request.action() == SellerReviewAction.SUSPEND)
                && note == null) {
            throw new SellerServiceException(ErrorCode.REVIEW_NOTE_REQUIRED);
        }

        if (request.action() == SellerReviewAction.APPROVE) {
            grantSellerRole(shop.getOwnerUserId(), authorization);
        }

        SellerStatus previousStatus = shop.getStatus();
        shop.setStatus(targetStatus);
        shop.setReviewNote(note);
        shop.setReviewedBy(adminUserId);
        shop.setReviewedAt(Instant.now());
        SellerShop savedShop = sellerShopRepository.save(shop);
        sellerShopEventPublisher.publishStatusChanged(SellerShopStatusChangedEvent.builder()
                .shopId(savedShop.getId().toString())
                .ownerUserId(savedShop.getOwnerUserId())
                .previousStatus(previousStatus.name())
                .status(savedShop.getStatus().name())
                .occurredAt(Instant.now())
                .build());
        return toResponse(savedShop);
    }

    private SellerStatus targetStatus(SellerStatus currentStatus, SellerReviewAction action) {
        return switch (action) {
            case APPROVE -> {
                if (currentStatus != SellerStatus.PENDING) {
                    throw new SellerServiceException(ErrorCode.INVALID_SELLER_TRANSITION);
                }
                yield SellerStatus.APPROVED;
            }
            case REJECT -> {
                if (currentStatus != SellerStatus.PENDING) {
                    throw new SellerServiceException(ErrorCode.INVALID_SELLER_TRANSITION);
                }
                yield SellerStatus.REJECTED;
            }
            case SUSPEND -> {
                if (currentStatus != SellerStatus.APPROVED) {
                    throw new SellerServiceException(ErrorCode.INVALID_SELLER_TRANSITION);
                }
                yield SellerStatus.SUSPENDED;
            }
        };
    }

    private void grantSellerRole(String userId, String authorization) {
        try {
            identityRoleClient.grantSellerRole(userId, authorization);
        } catch (IdentityRoleClient.IdentityRoleClientException exception) {
            ErrorCode errorCode = exception.getFailure() == IdentityRoleClient.IdentityRoleClientFailure.UNAVAILABLE
                    ? ErrorCode.IDENTITY_SERVICE_UNAVAILABLE
                    : ErrorCode.IDENTITY_ROLE_GRANT_REJECTED;
            throw new SellerServiceException(errorCode);
        }
    }

    private SellerShop findByOwnerUserId(String ownerUserId) {
        return sellerShopRepository.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new SellerServiceException(ErrorCode.SELLER_SHOP_NOT_FOUND));
    }

    private String uniqueSlug(String shopName) {
        String base = Normalizer.normalize(shopName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String prefix = base.isBlank() ? "shop" : base;
        String slug;
        do {
            slug = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        } while (sellerShopRepository.existsBySlug(slug));
        return slug;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SellerShopResponse toResponse(SellerShop shop) {
        return new SellerShopResponse(
                shop.getId(), shop.getOwnerUserId(), shop.getSlug(), shop.getShopName(),
                shop.getDescription(), shop.getPhone(), shop.getAddress(), shop.getCity(),
                shop.getStatus(), shop.getReviewNote(), shop.getReviewedBy(), shop.getReviewedAt(),
                shop.getCreatedAt(), shop.getUpdatedAt()
        );
    }
}
