package com.smockin.mockserver.service;

public record StatefulResponse(int httpResponseCode, String responseBody) {
    public StatefulResponse(int httpResponseCode) {
        this(httpResponseCode, null);
    }
}
