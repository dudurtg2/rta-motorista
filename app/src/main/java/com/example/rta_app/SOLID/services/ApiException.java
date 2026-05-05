package com.example.rta_app.SOLID.services;

import java.io.IOException;

public class ApiException extends IOException {

    private static final int MAX_BODY_LENGTH = 500;

    private final int statusCode;
    private final String responseBody;

    public ApiException(int statusCode, String message, String responseBody) {
        super(buildMessage(statusCode, message, responseBody));
        this.statusCode = statusCode;
        this.responseBody = responseBody == null ? "" : responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    private static String buildMessage(int statusCode, String message, String responseBody) {
        StringBuilder builder = new StringBuilder("HTTP ").append(statusCode);
        if (message != null && !message.trim().isEmpty()) {
            builder.append(" - ").append(message.trim());
        }

        String safeBody = responseBody == null ? "" : responseBody.trim();
        if (!safeBody.isEmpty()) {
            if (safeBody.length() > MAX_BODY_LENGTH) {
                safeBody = safeBody.substring(0, MAX_BODY_LENGTH) + "...";
            }
            builder.append(": ").append(safeBody);
        }
        return builder.toString();
    }
}
