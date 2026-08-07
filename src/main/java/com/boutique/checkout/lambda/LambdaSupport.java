package com.boutique.checkout.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.boutique.checkout.CheckoutserviceApplication;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class LambdaSupport {
    private static final ConfigurableApplicationContext SPRING =
            new SpringApplicationBuilder(CheckoutserviceApplication.class)
                    .web(WebApplicationType.NONE)
                    .properties(
                            "spring.main.banner-mode=off",
                            "spring.jmx.enabled=false"
                    )
                    .run();

    static final ObjectMapper JSON = SPRING.getBean(ObjectMapper.class);
    private static final Validator VALIDATOR = SPRING.getBean(Validator.class);

    private LambdaSupport() {}

    static <T> T bean(Class<T> type) {
        return SPRING.getBean(type);
    }

    static <T> T validate(T value) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(value);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return value;
    }

    static JsonNode readEvent(InputStream input) throws Exception {
        return JSON.readTree(input);
    }

    static String method(JsonNode event) {
        String value = event.path("requestContext").path("http").path("method").asText("");
        if (value.isBlank()) value = event.path("httpMethod").asText("");
        return value.toUpperCase();
    }

    static String path(JsonNode event) {
        String value = event.path("rawPath").asText("");
        if (value.isBlank()) value = event.path("path").asText("");
        return value;
    }

    static String body(JsonNode event) {
        String value = event.path("body").asText("");
        if (event.path("isBase64Encoded").asBoolean(false) && !value.isBlank()) {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        }
        return value;
    }

    static String pathParameter(JsonNode event, String name) {
        return event.path("pathParameters").path(name).asText("");
    }

    static String queryParameter(JsonNode event, String name) {
        return event.path("queryStringParameters").path(name).asText("");
    }

    static void respond(OutputStream output, int statusCode, Object body) throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("statusCode", statusCode);
        response.put("headers", Map.of(
                "content-type", "application/json",
                "cache-control", "no-store"
        ));
        response.put("isBase64Encoded", false);
        response.put("body", body instanceof String ? body : JSON.writeValueAsString(body));
        JSON.writeValue(output, response);
    }

    static void fail(OutputStream output, Throwable failure, Context context) throws Exception {
        Throwable root = rootCause(failure);
        int status = status(root);

        if (context != null && context.getLogger() != null) {
            context.getLogger().log(
                    "Lambda request failed: " + root.getClass().getName() + ": " + safeMessage(root)
            );
        }

        respond(output, status, Map.of(
                "status", status,
                "error", errorName(status),
                "message", safeMessage(root)
        ));
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static int status(Throwable failure) {
        if (failure instanceof ResponseStatusException response) {
            return response.getStatusCode().value();
        }

        String name = failure.getClass().getSimpleName().toLowerCase();
        if (name.contains("notfound")) return 404;
        if (name.contains("duplicate") || name.contains("conflict")) return 409;
        if (failure instanceof ConstraintViolationException
                || failure instanceof IllegalArgumentException
                || failure instanceof IllegalStateException) {
            return 400;
        }
        return 500;
    }

    private static String errorName(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 402 -> "Payment Required";
            default -> "Internal Server Error";
        };
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message;
    }
}
