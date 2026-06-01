package com.diplom.api.controller;

import com.diplom.api.dto.*;
import com.diplom.api.service.AuthService;
import com.diplom.api.service.TariffService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Контроллер управления тарифами производителя.
 * <p>
 * Предоставляет защищённые эндпоинты для CRUD-операций тарифов.
 * Все операции выполняются от имени авторизованного производителя.
 * Производитель может управлять только своими тарифами.
 */
@RestController
@RequestMapping("/api/producer/tariffs")
public class TariffController {

    private final AuthService authService;
    private final TariffService tariffService;

    /**
     * @param authService   сервис аутентификации и валидации токенов
     * @param tariffService сервис управления тарифами
     */
    public TariffController(AuthService authService, TariffService tariffService) {
        this.authService = authService;
        this.tariffService = tariffService;
    }

    /**
     * Создание нового тарифа для текущего производителя.
     *
     * @param authHeader заголовок {@code Authorization} с Bearer-токеном
     * @param request    данные нового тарифа и ограничений
     * @return созданный {@link TariffResponseDto}
     */
    @PostMapping
    public TariffResponseDto create(@RequestHeader("Authorization") String authHeader,
                                    @RequestBody TariffCreateRequest request) {
        Integer producerId = validateAndGetProducerId(authHeader);
        return tariffService.createTariff(producerId, request);
    }

    /**
     * Возвращает список всех тарифов текущего производителя.
     *
     * @param authHeader заголовок {@code Authorization} с Bearer-токеном
     * @return список {@link TariffResponseDto}, может быть пустым
     */
    @GetMapping
    public List<TariffResponseDto> getMyTariffs(@RequestHeader("Authorization") String authHeader) {
        Integer producerId = validateAndGetProducerId(authHeader);
        return tariffService.getMyTariffs(producerId);
    }

    /**
     * Обновление существующего тарифа и его технологических ограничений.
     * <p>
     * Принимаются только те поля, которые необходимо изменить.
     * Проверяется принадлежность тарифа текущему производителю.
     *
     * @param authHeader заголовок {@code Authorization} с Bearer-токеном
     * @param tariffId   идентификатор редактируемого тарифа
     * @param request    новые значения полей (необязательные)
     * @return обновлённый {@link TariffResponseDto}
     */
    @PutMapping("/{tariffId}")
    public TariffResponseDto update(@RequestHeader("Authorization") String authHeader,
                                    @PathVariable Integer tariffId,
                                    @RequestBody TariffUpdateRequest request) {
        Integer producerId = validateAndGetProducerId(authHeader);
        return tariffService.updateTariff(producerId, tariffId, request);
    }

    /**
     * Удаление тарифа текущего производителя по его идентификатору.
     * <p>
     * Предварительно проверяется, что тариф принадлежит данному производителю.
     *
     * @param authHeader заголовок {@code Authorization} с Bearer-токеном
     * @param tariffId   идентификатор удаляемого тарифа
     */
    @DeleteMapping("/{tariffId}")
    public void delete(@RequestHeader("Authorization") String authHeader,
                       @PathVariable Integer tariffId) {
        Integer producerId = validateAndGetProducerId(authHeader);
        tariffService.deleteTariff(producerId, tariffId);
    }

    private Integer validateAndGetProducerId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        return authService.validateToken(token);
    }
}