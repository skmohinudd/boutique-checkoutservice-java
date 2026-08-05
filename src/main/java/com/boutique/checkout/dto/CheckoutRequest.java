package com.boutique.checkout.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record CheckoutRequest(
 @NotNull UUID userId,
 @NotBlank @Size(max=100) String idempotencyKey,
 @NotBlank @Size(min=4,max=4) String cardLast4
) {}
