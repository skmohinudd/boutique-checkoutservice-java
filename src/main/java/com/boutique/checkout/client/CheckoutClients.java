package com.boutique.checkout.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class CheckoutClients {

    private final RestClient cart;
    private final RestClient order;
    private final RestClient payment;
    private final RestClient inventory;

    public CheckoutClients(
            RestClient.Builder builder,
            @Value("${clients.cart.base-url}") String cartUrl,
            @Value("${clients.order.base-url}") String orderUrl,
            @Value("${clients.payment.base-url}") String paymentUrl,
            @Value("${clients.inventory.base-url}") String inventoryUrl
    ) {
        this.cart = builder.clone().baseUrl(cartUrl).build();
        this.order = builder.clone().baseUrl(orderUrl).build();
        this.payment = builder.clone().baseUrl(paymentUrl).build();
        this.inventory = builder.clone().baseUrl(inventoryUrl).build();
    }

    public CartResponse getCart(UUID userId) {
        RestClient.RequestHeadersSpec<?> request = cart.get()
                .uri("/api/v1/carts/{userId}", userId);
        relayInboundAuthorization(request);
        return request.retrieve().body(CartResponse.class);
    }

    public void clearCart(UUID userId) {
        RestClient.RequestHeadersSpec<?> request = cart.delete()
                .uri("/api/v1/carts/{userId}", userId);
        relayInboundAuthorization(request);
        request.retrieve().toBodilessEntity();
    }

    public OrderResponse createOrder(OrderCreate request) {
        return order.post()
                .uri("/api/v1/orders")
                .body(request)
                .retrieve()
                .body(OrderResponse.class);
    }

    public OrderResponse confirmOrder(UUID id, UUID paymentId) {
        return order.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/orders/{id}/confirm")
                        .queryParam("paymentId", paymentId)
                        .build(id))
                .retrieve()
                .body(OrderResponse.class);
    }

    public void failOrder(UUID id) {
        order.post()
                .uri("/api/v1/orders/{id}/payment-failed", id)
                .retrieve()
                .toBodilessEntity();
    }

    public PaymentResponse authorize(PaymentRequest request) {
        return payment.post()
                .uri("/api/v1/payments/authorize")
                .body(request)
                .exchange((httpRequest, response) -> response.bodyTo(PaymentResponse.class));
    }

    public void refund(UUID id, String reason) {
        payment.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/payments/{id}/refund")
                        .queryParam("reason", reason)
                        .build(id))
                .retrieve()
                .toBodilessEntity();
    }

    public void reserve(ReservationRequest request) {
        inventory.post()
                .uri("/api/v1/inventory/reservations")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void commitReservations(UUID id) {
        inventory.post()
                .uri("/api/v1/inventory/reservations/orders/{id}/commit", id)
                .retrieve()
                .toBodilessEntity();
    }

    public void releaseReservations(UUID id) {
        inventory.post()
                .uri("/api/v1/inventory/reservations/orders/{id}/release", id)
                .retrieve()
                .toBodilessEntity();
    }

    public void compensateReservations(UUID id) {
        inventory.post()
                .uri("/api/v1/inventory/reservations/orders/{id}/compensate", id)
                .retrieve()
                .toBodilessEntity();
    }

    private void relayInboundAuthorization(RestClient.RequestHeadersSpec<?> request) {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) {
            return;
        }

        String authorization = attributes.getRequest()
                .getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization != null
                && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    public record CartResponse(
            UUID userId,
            List<Item> items,
            Integer totalItems,
            BigDecimal subtotal,
            String currency
    ) {
        public record Item(
                UUID productId,
                String sku,
                String name,
                String imageUrl,
                BigDecimal unitPrice,
                String currency,
                Integer quantity,
                Integer sellableQuantity,
                boolean available,
                BigDecimal lineTotal
        ) {
        }
    }

    public record OrderCreate(
            UUID userId,
            String idempotencyKey,
            List<OrderItem> items,
            BigDecimal total,
            String currency
    ) {
    }

    public record OrderItem(
            UUID productId,
            String sku,
            String name,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal
    ) {
    }

    public record OrderResponse(
            UUID id,
            String status,
            BigDecimal total,
            String currency,
            UUID paymentId
    ) {
    }

    public record PaymentRequest(
            UUID orderId,
            String idempotencyKey,
            BigDecimal amount,
            String currency,
            String cardLast4
    ) {
    }

    public record PaymentResponse(
            UUID id,
            UUID orderId,
            BigDecimal amount,
            String currency,
            String status,
            String providerReference
    ) {
    }

    public record ReservationRequest(
            UUID orderId,
            UUID productId,
            int quantity
    ) {
    }
}
