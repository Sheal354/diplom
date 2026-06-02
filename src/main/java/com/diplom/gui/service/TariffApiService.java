package com.diplom.gui.service;

import com.diplom.api.dto.*;
import com.diplom.gui.config.ApiConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class TariffApiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;

    public TariffApiService(ApiConfig apiConfig) {
        this.baseUrl = apiConfig.getBaseUrl() + "/producer/tariffs";
    }

    public List<TariffResponseDto> getMyTariffs(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<List<TariffResponseDto>> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<TariffResponseDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public TariffResponseDto createTariff(String token, TariffCreateRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TariffCreateRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<TariffResponseDto> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.POST,
                entity,
                TariffResponseDto.class
        );
        return response.getBody();
    }

    public TariffResponseDto updateTariff(String token, Integer tariffId, TariffUpdateRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TariffUpdateRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<TariffResponseDto> response = restTemplate.exchange(
                baseUrl + "/" + tariffId,
                HttpMethod.PUT,
                entity,
                TariffResponseDto.class
        );
        return response.getBody();
    }

    public void deleteTariff(String token, Integer tariffId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.exchange(
                baseUrl + "/" + tariffId,
                HttpMethod.DELETE,
                entity,
                Void.class
        );
    }
}