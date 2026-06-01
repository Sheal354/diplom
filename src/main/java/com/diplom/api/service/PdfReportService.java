package com.diplom.api.service;

import com.diplom.api.dto.CalculationRequest;
import com.diplom.api.dto.CalculationResponse;
import com.diplom.persistence.entity.*;
import com.diplom.persistence.repository.TariffRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Сервис генерации PDF-сметы на изготовление печатной платы.
 * <p>
 * Формирует документ в стиле технологической ведомости (ЕСТД).
 * Шрифт (Times New Roman) должен быть доступен в classpath (в папке {@code fonts}).
 */
@Service
public class PdfReportService {

    private final CostCalculationService calculationService;
    private final TariffRepository tariffRepository;

    /**
     * @param calculationService сервис расчёта стоимости
     * @param tariffRepository  репозиторий тарифов
     */
    public PdfReportService(CostCalculationService calculationService,
                            TariffRepository tariffRepository) {
        this.calculationService = calculationService;
        this.tariffRepository = tariffRepository;
    }

    /**
     * Генерирует PDF-отчёт со сметой на изготовление печатной платы.
     * <p>
     * Включает расчёт стоимости, информацию о выбранном тарифе и производителе,
     * детализацию по статьям затрат и результаты DFM-контроля.
     *
     * @param request параметры платы и выбранного тарифа
     * @return массив байтов, содержащий готовый PDF-документ
     */
    public byte[] generateReport(CalculationRequest request) {
        // 1. Расчёт стоимости
        CalculationResponse calcResult = calculationService.calculate(request);

        // 2. Загрузка тарифа и производителя
        TariffEntity tariff = tariffRepository.findById(request.getTariffId())
                .orElseThrow(() -> new RuntimeException("Tariff not found"));
        ProducerEntity producer = tariff.getProducer();
        LimitationEntity limits = tariff.getLimitation();

        // 3. PDF
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 15, 15, 15, 15);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();

        // Шрифты (Times New Roman, встроенный)
        BaseFont baseFont;
        try {
            baseFont = BaseFont.createFont("fonts/times.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            try {
                baseFont = BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        Font titleFont = new Font(baseFont, 13, Font.BOLD);
        Font headerFont = new Font(baseFont, 10, Font.BOLD);
        Font normalFont = new Font(baseFont, 9, Font.NORMAL);
        Font smallFont = new Font(baseFont, 7, Font.NORMAL);
        Font boldFont = new Font(baseFont, 9, Font.BOLD);

        // --- Заголовок отчёта ---
        Paragraph title = new Paragraph("СМЕТА НА ИЗГОТОВЛЕНИЕ ПЕЧАТНОЙ ПЛАТЫ", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        addEmptyLine(document, 6);

        // --- Основная надпись (штамп) ---
        PdfPTable stampTable = new PdfPTable(2);
        stampTable.setWidthPercentage(100);
        stampTable.setWidths(new float[]{2, 3});
        addStampRow(stampTable, "Номер документа:", "РТ-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")), headerFont, normalFont);
        addStampRow(stampTable, "Наименование изделия:", "__________________________", headerFont, normalFont);
        addStampRow(stampTable, "Разработчик (инженер-технолог):", "__________________________", headerFont, normalFont);
        addStampRow(stampTable, "Дата расчёта:", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), headerFont, normalFont);
        document.add(stampTable);
        addEmptyLine(document, 6);

        // --- Технологические параметры ---
        Paragraph techTitle = new Paragraph("ТЕХНОЛОГИЧЕСКИЕ ПАРАМЕТРЫ", headerFont);
        techTitle.setSpacingAfter(6);
        document.add(techTitle);
        PdfPTable paramTable = new PdfPTable(2);
        paramTable.setWidthPercentage(100);
        paramTable.setWidths(new float[]{2, 1});
        addParamRow(paramTable, "Габариты платы (Ш x В), мм", String.format("%.2f x %.2f", request.getWidth(), request.getHeight()), normalFont);
        addParamRow(paramTable, "Площадь платы, мм²", String.format("%.2f", request.getWidth() * request.getHeight()), normalFont);
        addParamRow(paramTable, "Количество медных слоёв", String.valueOf(request.getCopperLayers()), normalFont);
        addParamRow(paramTable, "Количество отверстий", String.valueOf(request.getTotalHoles()), normalFont);
        addParamRow(paramTable, "Минимальная ширина проводника, мм", String.format("%.3f", request.getMinTrackWidth()), normalFont);
        addParamRow(paramTable, "Минимальный зазор, мм", String.format("%.3f", request.getMinClearance()), normalFont);
        addParamRow(paramTable, "Минимальный диаметр отверстия, мм", String.format("%.3f", request.getMinHoleDiameter()), normalFont);
        addParamRow(paramTable, "Материал основания", request.getBoardMaterial(), normalFont);
        addParamRow(paramTable, "Финишное покрытие", request.getSurfaceFinish(), normalFont);
        document.add(paramTable);
        addEmptyLine(document, 6);

        // --- Информация о тарифе ---
        Paragraph tariffTitle = new Paragraph("ИНФОРМАЦИЯ О ТАРИФЕ", headerFont);
        tariffTitle.setSpacingAfter(6);
        document.add(tariffTitle);
        PdfPTable tariffTable = new PdfPTable(2);
        tariffTable.setWidthPercentage(100);
        tariffTable.setWidths(new float[]{2, 1});
        addParamRow(tariffTable, "Производитель", producer.getName(), normalFont);
        addParamRow(tariffTable, "Тариф", tariff.getName(), normalFont);
        addParamRow(tariffTable, "Базовая цена за см², руб.", tariff.getBasePricePerCm2().toString(), normalFont);
        addParamRow(tariffTable, "Надбавка за дополнительный слой, руб.", tariff.getExtraLayerPrice().toString(), normalFont);
        addParamRow(tariffTable, "Стоимость подготовки производства, руб.", tariff.getSetupCost().toString(), normalFont);
        if (request.isExpedited()) {
            addParamRow(tariffTable, "Надбавка за срочность, руб.", tariff.getExpeditedFee().toString(), normalFont);
        }
        document.add(tariffTable);
        addEmptyLine(document, 6);

        // --- Сводная ведомость затрат ---
        Paragraph costTitle = new Paragraph("СВОДНАЯ ВЕДОМОСТЬ ЗАТРАТ", headerFont);
        costTitle.setSpacingAfter(6);
        document.add(costTitle);
        PdfPTable costTable = new PdfPTable(6);
        costTable.setWidthPercentage(100);
        costTable.setWidths(new float[]{0.5f, 2.5f, 1.5f, 1.5f, 1.5f, 1.5f});
        addCell(costTable, "№", boldFont);
        addCell(costTable, "Статья затрат", boldFont);
        addCell(costTable, "Ед. изм.", boldFont);
        addCell(costTable, "Количество", boldFont);
        addCell(costTable, "Цена за ед., руб.", boldFont);
        addCell(costTable, "Сумма, руб.", boldFont);

        Map<String, Double> breakdown = calcResult.getBreakdown();
        double base = breakdown.getOrDefault("Base", 0.0);
        double layers = breakdown.getOrDefault("Layers", 0.0);
        double setup = breakdown.getOrDefault("Setup", 0.0);
        double expedited = breakdown.getOrDefault("Expedited", 0.0);
        double total = breakdown.getOrDefault("Total", 0.0);
        int quantity = request.getQuantity();

        addCostRow(costTable, 1, "Базовая стоимость (площадь)", "см²", 1, base, base * quantity, normalFont);
        addCostRow(costTable, 2, "Надбавка за слои", "шт.", 1, layers, layers * quantity, normalFont);
        addCostRow(costTable, 3, "Подготовка производства", "шт.", 1, setup, setup * quantity, normalFont);
        if (request.isExpedited()) {
            addCostRow(costTable, 4, "Надбавка за срочность", "шт.", 1, expedited, expedited * quantity, normalFont);
        }
        addCostRow(costTable, 5, "ОБЩАЯ СТОИМОСТЬ", "—", quantity, total, total, boldFont);

        document.add(costTable);
        addEmptyLine(document, 6);

        String totalText = String.format("%.2f руб.", total);
        document.add(new Paragraph("ИТОГО: " + totalText, boldFont));
        addEmptyLine(document, 6);

        // --- DFM-контроль ---
        Paragraph dfmTitle = new Paragraph("РЕЗУЛЬТАТЫ DFM-КОНТРОЛЯ", headerFont);
        dfmTitle.setSpacingAfter(6);
        document.add(dfmTitle);
        PdfPTable dfmTable = new PdfPTable(4);
        dfmTable.setWidthPercentage(100);
        dfmTable.setWidths(new float[]{2, 1, 1, 1});
        addCell(dfmTable, "Параметр", boldFont);
        addCell(dfmTable, "Факт", boldFont);
        addCell(dfmTable, "Лимит", boldFont);
        addCell(dfmTable, "Соответствие", boldFont);

        addDfmRow(dfmTable, "Мин. ширина проводника, мм", request.getMinTrackWidth(),
                limits.getMinTrackWidthMm().doubleValue(), true, normalFont);
        addDfmRow(dfmTable, "Мин. зазор, мм", request.getMinClearance(),
                limits.getMinClearanceMm().doubleValue(), true, normalFont);
        addDfmRow(dfmTable, "Мин. диаметр отверстия, мм", request.getMinHoleDiameter(),
                limits.getMinHoleDiameterMm().doubleValue(), true, normalFont);
        addDfmRow(dfmTable, "Количество слоёв (макс.)", request.getCopperLayers(),
                limits.getMaxLayers(), false, normalFont);

        // Глухие отверстия
        String blindStatus = !request.isHasBlindVia() || limits.getSupportsBlindVia() ? "Соответствует" : "Не соответствует";
        Font blindFont = new Font(normalFont);
        if ("Не соответствует".equals(blindStatus)) blindFont.setColor(java.awt.Color.RED);
        addCell(dfmTable, "Глухие отверстия", normalFont);
        addCell(dfmTable, request.isHasBlindVia() ? "Да" : "Нет", normalFont);
        addCell(dfmTable, limits.getSupportsBlindVia() ? "Да" : "Нет", normalFont);
        addCell(dfmTable, blindStatus, blindFont);

        // Скрытые отверстия
        String buriedStatus = !request.isHasBuriedVia() || limits.getSupportsBuriedVia() ? "Соответствует" : "Не соответствует";
        Font buriedFont = new Font(normalFont);
        if ("Не соответствует".equals(buriedStatus)) buriedFont.setColor(java.awt.Color.RED);
        addCell(dfmTable, "Скрытые отверстия", normalFont);
        addCell(dfmTable, request.isHasBuriedVia() ? "Да" : "Нет", normalFont);
        addCell(dfmTable, limits.getSupportsBuriedVia() ? "Да" : "Нет", normalFont);
        addCell(dfmTable, buriedStatus, buriedFont);

        document.add(dfmTable);
        addEmptyLine(document, 6);

        // --- Конец ---
        Paragraph footer1 = new Paragraph("Контактная информация производителя: " +
                producer.getName() + ", " + producer.getEmail() + ", " + producer.getPhone(), smallFont);
        Paragraph footer2 = new Paragraph("Смета носит ознакомительный характер. Окончательная стоимость согласовывается при размещении заказа.", smallFont);
        footer1.setSpacingAfter(2);
        footer2.setSpacingAfter(2);
        document.add(footer1);
        document.add(footer2);

        document.close();
        return baos.toByteArray();
    }

    private void addEmptyLine(Document document, int fontSize) {
        Paragraph empty = new Paragraph(" ", new Font(Font.HELVETICA, fontSize));
        empty.setSpacingAfter(0);
        try {
            document.add(empty);
        } catch (DocumentException e) {
            // ignore
        }
    }

    private void addStampRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.BOTTOM);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addParamRow(PdfPTable table, String param, String value, Font font) {
        PdfPCell cell1 = new PdfPCell(new Phrase(param, font));
        cell1.setBorder(Rectangle.NO_BORDER);
        PdfPCell cell2 = new PdfPCell(new Phrase(value, font));
        cell2.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell1);
        table.addCell(cell2);
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2);
        table.addCell(cell);
    }

    private void addCostRow(PdfPTable table, int num, String item, String unit, int qty,
                            double price, double amount, Font font) {
        addCell(table, String.valueOf(num), font);
        addCell(table, item, font);
        addCell(table, unit, font);
        addCell(table, String.valueOf(qty), font);
        addCell(table, String.format("%.2f", price), font);
        addCell(table, String.format("%.2f", amount), font);
    }

    private void addDfmRow(PdfPTable table, String param, double actual, double limit,
                           boolean greaterIsBetter, Font font) {
        boolean ok = greaterIsBetter ? actual >= limit : actual <= limit;
        String status = ok ? "Соответствует" : "Не соответствует";
        Font statusFont = new Font(font);
        if (!ok) {
            statusFont.setColor(java.awt.Color.RED);
        }
        addCell(table, param, font);
        addCell(table, String.format("%.3f", actual), font);
        addCell(table, String.format("%.3f", limit), font);
        addCell(table, status, statusFont);
    }
}