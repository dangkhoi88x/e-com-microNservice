package com.example.paymentservice.service;

import com.example.paymentservice.dto.request.CreatePaymentRequest;
import com.example.paymentservice.dto.response.PageResponse;
import com.example.paymentservice.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse createPayment(String userId, String token, CreatePaymentRequest request);

    PageResponse<PaymentResponse> getMyPayments(String userId, int page, int size);

    PageResponse<PaymentResponse> getAllPayments(int page, int size);

    PaymentResponse getPaymentDetail(String userId, String paymentId);

    PaymentResponse markPaymentSuccess(String userId, String token, String paymentId);

    PaymentResponse markPaymentFailed(String userId, String paymentId);

    PaymentResponse cancelPayment(String userId, String paymentId);
}
