package com.boutique.checkout.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class CheckoutClients {
 private final RestClient cart;
 private final RestClient order;
 private final RestClient payment;

 public CheckoutClients(RestClient.Builder builder,
  @Value("${clients.cart.base-url}") String cartUrl,
  @Value("${clients.order.base-url}") String orderUrl,
  @Value("${clients.payment.base-url}") String paymentUrl) {
   this.cart=builder.clone().baseUrl(cartUrl).build();
   this.order=builder.clone().baseUrl(orderUrl).build();
   this.payment=builder.clone().baseUrl(paymentUrl).build();
 }

 public CartResponse getCart(UUID userId){
   return cart.get().uri("/api/v1/carts/{userId}",userId).retrieve().body(CartResponse.class);
 }

 public void clearCart(UUID userId){
   cart.delete().uri("/api/v1/carts/{userId}",userId).retrieve().toBodilessEntity();
 }

 public OrderResponse createOrder(OrderCreate request){
   return order.post().uri("/api/v1/orders").body(request).retrieve().body(OrderResponse.class);
 }

 public OrderResponse confirmOrder(UUID orderId, UUID paymentId){
   return order.post().uri(uri -> uri.path("/api/v1/orders/{id}/confirm").queryParam("paymentId",paymentId).build(orderId))
    .retrieve().body(OrderResponse.class);
 }

 public void failOrder(UUID orderId){
   order.post().uri("/api/v1/orders/{id}/payment-failed",orderId).retrieve().toBodilessEntity();
 }

 public PaymentResponse authorize(PaymentRequest request){
   return payment.post().uri("/api/v1/payments/authorize").body(request)
    .exchange((req,res) -> res.bodyTo(PaymentResponse.class));
 }

 public record CartResponse(UUID userId,List<Item> items,Integer totalItems,BigDecimal subtotal,String currency){
   public record Item(UUID productId,String sku,String name,String imageUrl,BigDecimal unitPrice,String currency,
    Integer quantity,Integer sellableQuantity,boolean available,BigDecimal lineTotal){}
 }
 public record OrderCreate(UUID userId,String idempotencyKey,List<OrderItem> items,BigDecimal total,String currency){}
 public record OrderItem(UUID productId,String sku,String name,BigDecimal unitPrice,int quantity,BigDecimal lineTotal){}
 public record OrderResponse(UUID id,String status,BigDecimal total,String currency,UUID paymentId){}
 public record PaymentRequest(UUID orderId,String idempotencyKey,BigDecimal amount,String currency,String cardLast4){}
 public record PaymentResponse(UUID id,UUID orderId,BigDecimal amount,String currency,String status,String providerReference){}
}
