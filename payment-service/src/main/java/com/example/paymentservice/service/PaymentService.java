package com.example.paymentservice.service;

import com.example.paymentservice.dto.request.CreatePaymentRequest;
import com.example.paymentservice.dto.response.PageResponse;
import com.example.paymentservice.dto.response.PaymentResponse;
import com.example.paymentservice.dto.response.StripeCheckoutResponse;

public interface PaymentService {
    PaymentResponse createPayment(String userId, String token, CreatePaymentRequest request);

    PageResponse<PaymentResponse> getMyPayments(String userId, int page, int size);

    PageResponse<PaymentResponse> getAllPayments(int page, int size);

    PaymentResponse getPaymentDetail(String userId, String paymentId);

    PaymentResponse markPaymentSuccess(String userId, String token, String paymentId);

    PaymentResponse markPaymentFailed(String userId, String paymentId);

    PaymentResponse cancelPayment(String userId, String paymentId);

    StripeCheckoutResponse createStripeCheckout(String userId, String paymentId);

    PaymentResponse reconcileStripePayment(String userId, String paymentId);

    void handleStripeWebhook(String payload, String signature);
}
