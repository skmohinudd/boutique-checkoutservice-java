package com.boutique.checkout.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.boutique.checkout.dto.CheckoutRequest;
import com.boutique.checkout.service.CheckoutService;
import tools.jackson.databind.JsonNode;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public final class CheckoutLambdaHandler implements RequestStreamHandler {
    private final CheckoutService service = LambdaSupport.bean(CheckoutService.class);

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        try {
            JsonNode event = LambdaSupport.readEvent(input);
            String method = LambdaSupport.method(event);
            String path = LambdaSupport.path(event);

            if ("POST".equals(method) && "/api/v1/checkouts".equals(path)) {
                CheckoutRequest request = LambdaSupport.validate(
                        LambdaSupport.JSON.readValue(
                                LambdaSupport.body(event),
                                CheckoutRequest.class
                        )
                );
                LambdaSupport.respond(output, 200, service.checkout(request));
                return;
            }

            LambdaSupport.respond(output, 404, Map.of("message", "Checkout route not found"));
        } catch (Throwable failure) {
            try {
                LambdaSupport.fail(output, failure, context);
            } catch (Exception responseFailure) {
                throw new RuntimeException(responseFailure);
            }
        }
    }
}
