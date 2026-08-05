package com.boutique.checkout.service;

import com.boutique.checkout.client.CheckoutClients;
import com.boutique.checkout.dto.CheckoutRequest;
import com.boutique.checkout.dto.CheckoutResponse;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final CheckoutClients clients;

    public CheckoutService(
            CheckoutClients clients
    ) {
        this.clients = clients;
    }

    public CheckoutResponse checkout(
            CheckoutRequest request
    ) {
        var cart =
                clients.getCart(request.userId());

        if (cart == null
                || cart.items() == null
                || cart.items().isEmpty()) {
            throw new IllegalStateException(
                    "Cart is empty."
            );
        }

        if (cart.items()
                .stream()
                .anyMatch(item -> !item.available())) {
            throw new IllegalStateException(
                    "Cart contains unavailable items."
            );
        }

        var order =
                clients.createOrder(
                        new CheckoutClients.OrderCreate(
                                request.userId(),
                                request.idempotencyKey(),
                                cart.items()
                                        .stream()
                                        .map(
                                                item ->
                                                        new CheckoutClients.OrderItem(
                                                                item.productId(),
                                                                item.sku(),
                                                                item.name(),
                                                                item.unitPrice(),
                                                                item.quantity(),
                                                                item.lineTotal()
                                                        )
                                        )
                                        .toList(),
                                cart.subtotal(),
                                cart.currency()
                        )
                );

        var payment =
                clients.authorize(
                        new CheckoutClients.PaymentRequest(
                                order.id(),
                                request.idempotencyKey()
                                        + ":payment",
                                cart.subtotal(),
                                cart.currency(),
                                request.cardLast4()
                        )
                );

        if (payment == null
                || !"AUTHORIZED".equals(
                        payment.status()
                )) {
            clients.failOrder(order.id());

            return new CheckoutResponse(
                    order.id(),
                    payment == null
                            ? null
                            : payment.id(),
                    "PAYMENT_FAILED",
                    cart.subtotal(),
                    cart.currency()
            );
        }

        // Order Service writes the order status and Kafka
        // outbox event atomically.
        clients.confirmOrder(
                order.id(),
                payment.id()
        );

        clients.clearCart(request.userId());

        return new CheckoutResponse(
                order.id(),
                payment.id(),
                "CONFIRMED",
                cart.subtotal(),
                cart.currency()
        );
    }
}
