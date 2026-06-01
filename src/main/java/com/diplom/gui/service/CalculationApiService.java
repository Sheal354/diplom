package com.diplom.gui.service;

import com.diplom.api.dto.CalculationRequest;
import com.diplom.gui.config.ApiConfig;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CalculationApiService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;

    public CalculationApiService(ApiConfig apiConfig) {
        this.baseUrl = apiConfig.getBaseUrl();
    }

    public byte[] getReport(CalculationRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CalculationRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(
                baseUrl + "/calculate/report",
                HttpMethod.POST,
                entity,
                byte[].class
        );
        return response.getBody();
    }
}