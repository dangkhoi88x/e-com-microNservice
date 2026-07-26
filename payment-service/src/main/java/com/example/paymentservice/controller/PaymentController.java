package com.example.paymentservice.controller;

import com.example.paymentservice.dto.request.CreatePaymentRequest;
import com.example.paymentservice.dto.response.ApiResponse;
import com.example.paymentservice.dto.response.PageResponse;
import com.example.paymentservice.dto.response.PaymentResponse;
import com.example.paymentservice.dto.response.StripeCheckoutResponse;
import com.example.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreatePaymentRequest request
    ) {
        PaymentResponse data = paymentService.createPayment(jwt.getSubject(), jwt.getTokenValue(), request);

        return ApiResponse.<PaymentResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Payment created successfully")
                .data(data)
                .build();
    }

    @GetMapping("/my-payments")
    public ApiResponse<PageResponse<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        PageResponse<PaymentResponse> data = paymentService.getMyPayments(jwt.getSubject(), page, size);

        return ApiResponse.<PageResponse<PaymentResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Payments retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<PaymentResponse>> getAllPayments(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        PageResponse<PaymentResponse> data = paymentService.getAllPayments(page, size);

        return ApiResponse.<PageResponse<PaymentResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Payments retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> getPaymentDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) {
        PaymentResponse data = paymentService.getPaymentDetail(jwt.getSubject(), id);

        return ApiResponse.<PaymentResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Payment retrieved successfully")
                .data(data)
                .build();
    }

    @PostMapping("/{id}/stripe-checkout")
    public ApiResponse<StripeCheckoutResponse> createStripeCheckout(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) {
        StripeCheckoutResponse data = paymentService.createStripeCheckout(jwt.getSubject(), id);

        return ApiResponse.<StripeCheckoutResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Stripe checkout session created successfully")
                .data(data)
                .build();
    }

    @PostMapping("/{id}/stripe-reconcile")
    public ApiResponse<PaymentResponse> reconcileStripePayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) {
        PaymentResponse data = paymentService.reconcileStripePayment(jwt.getSubject(), id);

        return ApiResponse.<PaymentResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Stripe payment reconciled successfully")
                .data(data)
                .build();
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestHeader("Stripe-Signature") String signature,
            @RequestBody String payload
    ) {
        paymentService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/success")
    public ApiResponse<PaymentResponse> markPaymentSuccess(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Authorization") String authorization,
            @PathVariable String id
    ) {
        PaymentResponse data = paymentService.markPaymentSuccess(
                jwt.getSubject(),
                authorization.replace("Bearer ", ""),
                id
        );

        return ApiResponse.<PaymentResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Payment marked success")
                .data(data)
                .build();
    }

    @PutMapping("/{id}/failed")
    public ApiResponse<PaymentResponse> markPaymentFailed(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) {
        PaymentResponse data = paymentService.markPaymentFailed(jwt.getSubject(), id);

        return ApiResponse.<PaymentResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Payment marked failed")
                .data(data)
                .build();
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<PaymentResponse> cancelPayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) {
        PaymentResponse data = paymentService.cancelPayment(jwt.getSubject(), id);

        return ApiResponse.<PaymentResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Payment cancelled successfully")
                .data(data)
                .build();
    }
}
