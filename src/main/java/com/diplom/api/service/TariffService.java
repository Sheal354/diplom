package com.diplom.api.service;

import com.diplom.api.dto.TariffCreateRequest;
import com.diplom.api.dto.TariffResponseDto;
import com.diplom.api.dto.TariffUpdateRequest;
import com.diplom.persistence.entity.*;
import com.diplom.persistence.repository.ProducerRepository;
import com.diplom.persistence.repository.TariffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис управления тарифами производителя.
 * <p>
 * Предоставляет CRUD-операции для тарифов и связанных с ними технологических ограничений.
 * Все операции в рамках одного производителя.
 */
@Service
public class TariffService {

    private final TariffRepository tariffRepository;
    private final ProducerRepository producerRepository;

    /**
     * @param tariffRepository   репозиторий тарифов
     * @param producerRepository репозиторий производителей
     */
    public TariffService(TariffRepository tariffRepository, ProducerRepository producerRepository) {
        this.tariffRepository = tariffRepository;
        this.producerRepository = producerRepository;
    }

    /**
     * Создаёт новый тариф вместе с технологическими ограничениями.
     *
     * @param producerId идентификатор производителя-владельца
     * @param request    данные тарифа и ограничений
     * @return созданный тариф в виде {@link TariffResponseDto}
     * @throws RuntimeException если производитель не найден
     */
    @Transactional
    public TariffResponseDto createTariff(Integer producerId, TariffCreateRequest request) {
        ProducerEntity producer = producerRepository.findById(producerId)
                .orElseThrow(() -> new RuntimeException("Producer not found"));

        LimitationEntity limitation = LimitationEntity.builder()
                .minTrackWidthMm(request.getMinTrackWidthMm())
                .minClearanceMm(request.getMinClearanceMm())
                .minHoleDiameterMm(request.getMinHoleDiameterMm())
                .maxLayers(request.getMaxLayers())
                .supportedMaterials(request.getSupportedMaterials())
                .supportedFinishes(request.getSupportedFinishes())
                .supportsBlindVia(request.getSupportsBlindVia())
                .supportsBuriedVia(request.getSupportsBuriedVia())
                .build();

        // isActive можно взять из запроса, если передано, иначе true
        Boolean active = request.getIsActive() != null ? request.getIsActive() : true;

        TariffEntity tariff = TariffEntity.builder()
                .producer(producer)
                .name(request.getName())
                .basePricePerCm2(request.getBasePricePerCm2())
                .extraLayerPrice(request.getExtraLayerPrice())
                .expeditedFee(request.getExpeditedFee())
                .setupCost(request.getSetupCost())
                .minQuantity(request.getMinQuantity())
                .isActive(active)
                .limitation(limitation)
                .build();
        limitation.setTariff(tariff);  // двусторонняя связь

        tariff = tariffRepository.save(tariff);

        // Обновляем дату изменения у производителя
        producer.setUpdatedAt(OffsetDateTime.now());
        producerRepository.save(producer);

        return buildDto(tariff);
    }

    /**
     * Возвращает все тарифы указанного производителя.
     *
     * @param producerId идентификатор производителя
     * @return список {@link TariffResponseDto} (может быть пустым)
     */
    public List<TariffResponseDto> getMyTariffs(Integer producerId) {
        return tariffRepository.findByProducerId(producerId).stream()
                .map(this::buildDto)
                .collect(Collectors.toList());
    }

    /**
     * Обновляет существующий тариф и его ограничения.
     * <p>
     * Принимает только те поля, которые переданы (не {@code null}).
     *
     * @param producerId идентификатор производителя-владельца
     * @param tariffId   идентификатор редактируемого тарифа
     * @param request    новые значения полей (опциональные)
     * @return обновлённый {@link TariffResponseDto}
     * @throws RuntimeException если тариф не найден или не принадлежит производителю
     */
    @Transactional
    public TariffResponseDto updateTariff(Integer producerId, Integer tariffId, TariffUpdateRequest request) {
        TariffEntity tariff = tariffRepository.findById(tariffId)
                .orElseThrow(() -> new RuntimeException("Tariff not found"));
        if (!tariff.getProducer().getId().equals(producerId)) {
            throw new RuntimeException("Access denied");
        }

        // Обновляем поля тарифа
        if (request.getName() != null) tariff.setName(request.getName());
        if (request.getBasePricePerCm2() != null) tariff.setBasePricePerCm2(request.getBasePricePerCm2());
        if (request.getExtraLayerPrice() != null) tariff.setExtraLayerPrice(request.getExtraLayerPrice());
        if (request.getExpeditedFee() != null) tariff.setExpeditedFee(request.getExpeditedFee());
        if (request.getSetupCost() != null) tariff.setSetupCost(request.getSetupCost());
        if (request.getMinQuantity() != null) tariff.setMinQuantity(request.getMinQuantity());
        if (request.getIsActive() != null) tariff.setIsActive(request.getIsActive());

        // Обновляем ограничения, если переданы
        LimitationEntity lim = tariff.getLimitation();
        if (lim != null) {
            if (request.getMinTrackWidthMm() != null) lim.setMinTrackWidthMm(request.getMinTrackWidthMm());
            if (request.getMinClearanceMm() != null) lim.setMinClearanceMm(request.getMinClearanceMm());
            if (request.getMinHoleDiameterMm() != null) lim.setMinHoleDiameterMm(request.getMinHoleDiameterMm());
            if (request.getMaxLayers() != null) lim.setMaxLayers(request.getMaxLayers());
            if (request.getSupportedMaterials() != null) lim.setSupportedMaterials(request.getSupportedMaterials());
            if (request.getSupportedFinishes() != null) lim.setSupportedFinishes(request.getSupportedFinishes());
            if (request.getSupportsBlindVia() != null) lim.setSupportsBlindVia(request.getSupportsBlindVia());
            if (request.getSupportsBuriedVia() != null) lim.setSupportsBuriedVia(request.getSupportsBuriedVia());
        }

        TariffEntity saved = tariffRepository.save(tariff);

        // Обновляем дату изменения у производителя
        ProducerEntity producer = saved.getProducer();
        producer.setUpdatedAt(OffsetDateTime.now());
        producerRepository.save(producer);

        return buildDto(saved);
    }

    /**
     * Удаляет тариф по идентификатору.
     *
     * @param producerId идентификатор производителя-владельца
     * @param tariffId   идентификатор удаляемого тарифа
     * @throws RuntimeException если тариф не найден или не принадлежит производителю
     */
    @Transactional
    public void deleteTariff(Integer producerId, Integer tariffId) {
        TariffEntity tariff = tariffRepository.findById(tariffId)
                .orElseThrow(() -> new RuntimeException("Tariff not found"));
        if (!tariff.getProducer().getId().equals(producerId)) {
            throw new RuntimeException("Access denied");
        }

        ProducerEntity producer = tariff.getProducer();
        tariffRepository.delete(tariff);

        // Обновляем дату изменения у производителя
        producer.setUpdatedAt(OffsetDateTime.now());
        producerRepository.save(producer);
    }

    private TariffResponseDto buildDto(TariffEntity tariff) {
        LimitationEntity lim = tariff.getLimitation();
        return TariffResponseDto.builder()
                .id(tariff.getId())
                .name(tariff.getName())
                .basePricePerCm2(tariff.getBasePricePerCm2())
                .extraLayerPrice(tariff.getExtraLayerPrice())
                .expeditedFee(tariff.getExpeditedFee())
                .setupCost(tariff.getSetupCost())
                .minQuantity(tariff.getMinQuantity())
                .isActive(tariff.getIsActive())
                .minTrackWidthMm(lim != null ? lim.getMinTrackWidthMm() : null)
                .minClearanceMm(lim != null ? lim.getMinClearanceMm() : null)
                .minHoleDiameterMm(lim != null ? lim.getMinHoleDiameterMm() : null)
                .maxLayers(lim != null ? lim.getMaxLayers() : null)
                .supportedMaterials(lim != null ? lim.getSupportedMaterials() : null)
                .supportedFinishes(lim != null ? lim.getSupportedFinishes() : null)
                .supportsBlindVia(lim != null ? lim.getSupportsBlindVia() : null)
                .supportsBuriedVia(lim != null ? lim.getSupportsBuriedVia() : null)
                .expeditedFee(tariff.getExpeditedFee())
                .build();
    }
}