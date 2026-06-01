package com.diplom.gui.service;

import com.diplom.api.dto.AuthResponse;
import com.diplom.api.dto.LoginRequest;
import com.diplom.api.dto.RegisterRequest;
import com.diplom.gui.config.ApiConfig;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthApiService {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AuthApiService(ApiConfig apiConfig) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = apiConfig.getBaseUrl() + "/auth";
    }

    public AuthResponse login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    baseUrl + "/login",
                    HttpMethod.POST,
                    entity,
                    AuthResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new RuntimeException("Ошибка авторизации! Неверный логин или пароль.");
        } catch (RestClientException ex) {
            throw new RuntimeException("Ошибка соединения с сервером.");
        }
    }

    public AuthResponse register(String name, String contactPerson, String email,
                                 String phone, String password) {
        RegisterRequest request = new RegisterRequest(name, contactPerson, email, phone, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    baseUrl + "/register",
                    HttpMethod.POST,
                    entity,
                    AuthResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new RuntimeException("Ошибка регистрации! Возможно, email уже занят.");
        } catch (RestClientException ex) {
            throw new RuntimeException("Ошибка соединения с сервером.");
        }
    }
}