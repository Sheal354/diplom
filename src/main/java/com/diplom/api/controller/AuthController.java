package com.diplom.api.controller;

import com.diplom.api.dto.*;
import com.diplom.api.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер аутентификации производителей.
 * <p>
 * Предоставляет конечные точки для регистрации нового производителя и входа в систему.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * @param authService сервис, отвечающий за регистрацию и аутентификацию
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Регистрация нового производителя.
     *
     * @param request данные для регистрации
     * @return ответ с токеном, идентификатором и названием организации
     */
    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Вход производителя в систему.
     *
     * @param request учётные данные (email и пароль)
     * @return ответ с токеном, идентификатором и названием организации
     */
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}