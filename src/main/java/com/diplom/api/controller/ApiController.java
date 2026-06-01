package com.diplom.api.controller;

import com.diplom.api.dto.*;
import com.diplom.api.service.CostCalculationService;
import com.diplom.api.service.PdfReportService;
import com.diplom.persistence.entity.*;
import com.diplom.persistence.repository.ProducerRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Контроллер публичного API.
 * <p>
 * Предоставляет доступ к списку активных производителей с тарифами,
 * выполняет расчёт стоимости изготовления печатной платы и формирует смету в формате PDF.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final ProducerRepository producerRepository;
    private final CostCalculationService calculationService;
    private final PdfReportService pdfReportService;

    /**
     * @param producerRepository репозиторий производителей
     * @param calculationService сервис расчёта стоимости
     * @param pdfReportService   сервис генерации PDF-отчёта
     */
    public ApiController(ProducerRepository producerRepository,
                         CostCalculationService calculationService, PdfReportService pdfReportService) {
        this.producerRepository = producerRepository;
        this.calculationService = calculationService;
        this.pdfReportService = pdfReportService;
    }

    /**
     * Возвращает список всех активных производителей, отсортированных
     * по дате последнего обновления (сначала свежие).
     * <p>
     * В карточку каждого производителя включаются только активные тарифы
     * с их технологическими ограничениями.
     *
     * @return список {@link ProducerDto}
     */
    @GetMapping("/producers")
    public List<ProducerDto> getProducers() {
        return producerRepository.findAllByIsActiveTrueOrderByUpdatedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Выполняет расчёт стоимости изготовления печатной платы
     * по заданным параметрам и выбранному тарифу.
     * <p>
     * Возвращает смету с разбивкой по статьям затрат и результатами DFM-контроля.
     *
     * @param request параметры платы и идентификатор тарифа
     * @return {@link CalculationResponse} с итоговой ценой и предупреждениями
     */
    @PostMapping("/calculate")
    public CalculationResponse calculate(@RequestBody CalculationRequest request) {
        return calculationService.calculate(request);
    }

    /**
     * Формирует смету на изготовление платы в формате PDF и возвращает её в виде вложения для скачивания.
     * <p>
     * Использует данные расчёта стоимости и информацию о выбранном тарифе.
     *
     * @param request параметры платы и идентификатор тарифа
     * @return HTTP-ответ с телом {@code byte[]} (PDF-файл) и заголовками
     */
    @PostMapping("/calculate/report")
    public ResponseEntity<byte[]> getReport(@RequestBody CalculationRequest request) {
        byte[] pdf = pdfReportService.generateReport(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename("smeta.pdf").build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private ProducerDto toDto(ProducerEntity producer) {
        List<TariffDto> tariffDtos = producer.getTariffs().stream()
                .filter(t -> t.getIsActive() != null && t.getIsActive())   // только активные тарифы
                .map(t -> {
                    LimitationEntity lim = t.getLimitation();
                    return TariffDto.builder()
                            .id(t.getId())
                            .name(t.getName())
                            .basePricePerCm2(t.getBasePricePerCm2())
                            .extraLayerPrice(t.getExtraLayerPrice())
                            .expeditedFee(t.getExpeditedFee())
                            .setupCost(t.getSetupCost())
                            .minQuantity(t.getMinQuantity())
                            .minTrackWidthMm(lim != null ? lim.getMinTrackWidthMm() : null)
                            .minClearanceMm(lim != null ? lim.getMinClearanceMm() : null)
                            .minHoleDiameterMm(lim != null ? lim.getMinHoleDiameterMm() : null)
                            .maxLayers(lim != null ? lim.getMaxLayers() : null)
                            .supportedMaterials(lim != null ? lim.getSupportedMaterials() : null)
                            .supportedFinishes(lim != null ? lim.getSupportedFinishes() : null)
                            .supportsBlindVia(lim != null ? lim.getSupportsBlindVia() : null)
                            .supportsBuriedVia(lim != null ? lim.getSupportsBuriedVia() : null)
                            .build();
                }).collect(Collectors.toList());

        return ProducerDto.builder()
                .id(producer.getId())
                .name(producer.getName())
                .contactPerson(producer.getContactPerson())
                .email(producer.getEmail())
                .phone(producer.getPhone())
                .tariffs(tariffDtos)
                .build();
    }
}