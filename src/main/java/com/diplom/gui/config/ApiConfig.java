package com.diplom.gui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiConfig {

    private final String baseUrl;

    public ApiConfig(@Value("${app.api.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}