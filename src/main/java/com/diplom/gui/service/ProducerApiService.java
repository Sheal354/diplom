package com.diplom.gui.service;

import com.diplom.api.dto.ProducerDto;
import com.diplom.api.dto.ProducerProfileDto;
import com.diplom.api.dto.UpdateProducerRequest;
import com.diplom.gui.config.ApiConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class ProducerApiService {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProducerApiService(ApiConfig apiConfig) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = apiConfig.getBaseUrl();
    }

    public List<ProducerDto> fetchProducers() {
        try {
            ResponseEntity<List<ProducerDto>> response = restTemplate.exchange(
                    baseUrl + "/producers",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ProducerDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public ProducerProfileDto getMyProfile(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<ProducerProfileDto> response = restTemplate.exchange(
                    baseUrl + "/producer/profile",
                    HttpMethod.GET,
                    entity,
                    ProducerProfileDto.class
            );
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new RuntimeException("Ошибка загрузки профиля: " + ex.getMessage());
        } catch (RestClientException ex) {
            throw new RuntimeException("Ошибка соединения с сервером.");
        }
    }

    public ProducerProfileDto updateMyProfile(String token, UpdateProducerRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateProducerRequest> entity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<ProducerProfileDto> response = restTemplate.exchange(
                    baseUrl + "/producer/profile",
                    HttpMethod.PUT,
                    entity,
                    ProducerProfileDto.class
            );
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new RuntimeException("Ошибка обновления профиля: " + ex.getMessage());
        } catch (RestClientException ex) {
            throw new RuntimeException("Ошибка соединения с сервером.");
        }
    }
}