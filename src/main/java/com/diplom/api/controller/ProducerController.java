package com.diplom.api.controller;

import com.diplom.api.dto.ProducerProfileDto;
import com.diplom.api.dto.UpdateProducerRequest;
import com.diplom.api.service.AuthService;
import com.diplom.api.service.ProducerService;
import com.diplom.persistence.entity.ProducerEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер личного кабинета производителя.
 * <p>
 * Предоставляет защищённые эндпоинты для получения и редактирования профиля производителя.
 * Доступ разрешён только авторизованному производителю.
 */
@RestController
@RequestMapping("/api/producer")
public class ProducerController {

    private final AuthService authService;
    private final ProducerService producerService;

    /**
     * @param authService      сервис аутентификации и валидации токенов
     * @param producerService  сервис работы с профилем производителя
     */
    public ProducerController(AuthService authService, ProducerService producerService) {
        this.authService = authService;
        this.producerService = producerService;
    }

    /**
     * Возвращает профиль текущего авторизованного производителя.
     *
     * @param authHeader заголовок {@code Authorization} с Bearer-токеном
     * @return заполненный {@link ProducerProfileDto}
     */
    @GetMapping("/profile")
    public ProducerProfileDto getProfile(@RequestHeader("Authorization") String authHeader) {
        Integer producerId = validateAndGetProducerId(authHeader);
        ProducerEntity producer = producerService.getProfile(producerId);
        return buildDto(producer);
    }

    /**
     * Обновление профиля текущего авторизованного производителя.
     * <p>
     * Принимает только те поля, которые необходимо изменить.
     *
     * @param authHeader заголовок {@code Authorization} с Bearer-токеном
     * @param request    новые значения полей (необязательные)
     * @return обновлённый {@link ProducerProfileDto}
     */
    @PutMapping("/profile")
    public ProducerProfileDto updateProfile(@RequestHeader("Authorization") String authHeader,
                                            @RequestBody UpdateProducerRequest request) {
        Integer producerId = validateAndGetProducerId(authHeader);
        ProducerEntity updated = producerService.updateProfile(producerId, request);
        return buildDto(updated);
    }

    private Integer validateAndGetProducerId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        return authService.validateToken(token);
    }

    private ProducerProfileDto buildDto(ProducerEntity producer) {
        return ProducerProfileDto.builder()
                .id(producer.getId())
                .name(producer.getName())
                .contactPerson(producer.getContactPerson())
                .email(producer.getEmail())
                .phone(producer.getPhone())
                .isActive(producer.getIsActive())
                .build();
    }
}