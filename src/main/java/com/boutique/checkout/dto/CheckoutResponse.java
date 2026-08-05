package com.boutique.checkout.dto;
import java.math.BigDecimal;
import java.util.UUID;
public record CheckoutResponse(UUID orderId, UUID paymentId, String status, BigDecimal total, String currency) {}
