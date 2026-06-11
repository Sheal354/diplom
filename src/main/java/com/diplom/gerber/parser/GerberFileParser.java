package com.diplom.gerber.parser;

import com.deltaproto.deltagerber.model.drill.DrillDocument;
import com.deltaproto.deltagerber.model.gerber.BoundingBox;
import com.deltaproto.deltagerber.model.gerber.GerberDocument;
import com.deltaproto.deltagerber.parser.ExcellonParser;
import com.deltaproto.deltagerber.parser.GerberParser;
import com.diplom.gerber.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Основной сервис парсинга Gerber-файлов и файлов сверловки.
 * <p>
 * Координирует извлечение всех данных из ZIP-архива или папки, содержащих Gerber- и Excellon-файлы,
 * и формирует сводную модель {@link BoardParameters}.
 */
@Service
@RequiredArgsConstructor
public class GerberFileParser {

    private final LayerClassifier layerClassifier;
    private final GeometryAnalyzer geometryAnalyzer;
    private final DrillAnalyzer drillAnalyzer;

    /**
     * Парсит Gerber-файлы и файлы сверловки из ZIP-потока.
     *
     * @param zipStream входной поток ZIP-архива
     * @return результат парсинга с параметрами платы и документами слоёв
     * @throws IOException при ошибках чтения архива
     */
    public ParseResult parseFromZip(InputStream zipStream) throws IOException {
        Map<String, byte[]> files = extractZipEntries(zipStream);
        return parseFiles(files);
    }

    /**
     * Парсит Gerber-файлы и файлы сверловки из указанной папки.
     * Все файлы в папке будут прочитаны; неподходящие файлы будут проигнорированы парсером.
     *
     * @param directory папка с Gerber- и Excellon-файлами
     * @return результат парсинга
     * @throws IOException если папка не существует или произошла ошибка чтения файлов
     */
    public ParseResult parseFromDirectory(File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Указанный путь не является папкой: " + directory.getAbsolutePath());
        }
        Map<String, byte[]> files = new LinkedHashMap<>();
        File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile()) {
                    files.put(file.getName(), Files.readAllBytes(file.toPath()));
                }
            }
        }
        return parseFiles(files);
    }

    /**
     * Общий метод обработки набора файлов (имя -> содержимое).
     * Выполняет классификацию, геометрический анализ и извлечение отверстий.
     */
    private ParseResult parseFiles(Map<String, byte[]> files) throws IOException {
        List<BoardLayer> layers = new ArrayList<>();
        Map<BoardLayer, Object> layerDocuments = new HashMap<>();
        List<DrillHole> allDrillHoles = new ArrayList<>();
        boolean hasBlindVia = false;
        boolean hasBuriedVia = false;

        GerberParser gerberParser = new GerberParser();
        ExcellonParser excellonParser = new ExcellonParser();

        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String fileName = entry.getKey();
            byte[] content = entry.getValue();

            ParseLayerResult layerResult = parseGerberLayer(fileName, content, gerberParser);
            if (layerResult != null) {
                layers.add(layerResult.layer);
                layerDocuments.put(layerResult.layer, layerResult.document);
                continue;
            }

            if (drillAnalyzer.isExcellonByExtension(fileName)) {
                try {
                    String drillContent = new String(content, StandardCharsets.UTF_8);
                    DrillDocument drillDoc = excellonParser.parse(drillContent);
                    if (drillDoc != null && !drillDoc.getOperations().isEmpty()) {
                        allDrillHoles.addAll(drillAnalyzer.extractDrillHoles(drillDoc, fileName));

                        BoardLayer drillLayer = new BoardLayer();
                        drillLayer.setName(fileName);
                        drillLayer.setType(LayerType.DRILL);
                        drillLayer.setSide(null);
                        layers.add(drillLayer);
                        layerDocuments.put(drillLayer, drillDoc);

                        boolean[] viaInfo = drillAnalyzer.determineViaType(drillDoc, fileName);
                        if (viaInfo[0]) hasBlindVia = true;
                        if (viaInfo[1]) hasBuriedVia = true;
                    }
                } catch (Exception e) {
                    System.out.println("Ошибка парсинга сверловки " + fileName + ": " + e.getMessage());
                }
            }
        }

        BoardParameters params = buildBoardParameters(layers, allDrillHoles);
        params.setHasBlindVia(hasBlindVia);
        params.setHasBuriedVia(hasBuriedVia);
        return new ParseResult(params, layerDocuments);
    }

    /**
     * Извлекает все файлы из ZIP-потока и возвращает отображение «имя файла - содержимое».
     * Имена файлов очищаются от пути, остаётся только короткое имя.
     */
    private Map<String, byte[]> extractZipEntries(InputStream zipStream) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String name = entry.getName();
                    if (name.contains("/")) {
                        name = name.substring(name.lastIndexOf('/') + 1);
                    }
                    files.put(name, zis.readAllBytes());
                }
                zis.closeEntry();
            }
        }
        return files;
    }

    /**
     * Пытается разобрать один Gerber-файл и создать объект {@link BoardLayer}.
     */
    private ParseLayerResult parseGerberLayer(String fileName, byte[] content,
                                              GerberParser gerberParser) throws IOException {
        GerberDocument doc;
        try {
            String gerberContent = new String(content, StandardCharsets.UTF_8);
            doc = gerberParser.parse(gerberContent);
        } catch (Exception e) {
            System.out.println("Ошибка парсинга Gerber " + fileName + ": " + e.getMessage());
            return null;
        }
        if (doc == null) return null;

        BoardLayer layer = new BoardLayer();
        if (!layerClassifier.classifyLayer(doc, fileName, layer)) {
            return null;
        }

        BoundingBox bbox = doc.getBoundingBox();
        if (bbox != null) {
            layer.setBounds(new RectBounds(
                    bbox.getMinX(), bbox.getMaxX(),
                    bbox.getMinY(), bbox.getMaxY()));
        }

        if (layer.getType() == LayerType.COPPER) {
            layer.setMinTrackWidth(geometryAnalyzer.computeMinTrackWidth(doc));
            layer.setMinClearance(geometryAnalyzer.computeMinClearance(doc));
        }
        return new ParseLayerResult(layer, doc);
    }

    /**
     * Собирает итоговую модель {@link BoardParameters} из списка слоёв и отверстий.
     */
    private BoardParameters buildBoardParameters(List<BoardLayer> layers, List<DrillHole> holes) {
        BoardParameters params = new BoardParameters();
        params.setLayers(layers);

        RectBounds outline = findOutlineBounds(layers);
        if (outline != null) {
            params.setWidth(outline.getWidth());
            params.setHeight(outline.getHeight());
            params.setArea(outline.getArea());
        } else {
            RectBounds union = computeUnionBounds(layers);
            params.setWidth(union.getWidth());
            params.setHeight(union.getHeight());
            params.setArea(union.getArea());
        }

        params.setCopperLayersCount(countCopperLayers(layers));

        params.setSolderMaskLayersCount((int) layers.stream().filter(l -> l.getType() == LayerType.SOLDER_MASK).count());
        params.setSilkscreenLayersCount((int) layers.stream().filter(l -> l.getType() == LayerType.SILKSCREEN).count());

        double minTrack = Double.MAX_VALUE;
        for (BoardLayer l : layers) {
            if (l.getType() == LayerType.COPPER && l.getMinTrackWidth() > 0) {
                minTrack = Math.min(minTrack, l.getMinTrackWidth());
            }
        }
        params.setMinTrackWidth(minTrack == Double.MAX_VALUE ? -1 : minTrack);

        double minClear = Double.MAX_VALUE;
        for (BoardLayer l : layers) {
            if (l.getType() == LayerType.COPPER && l.getMinClearance() > 0) {
                minClear = Math.min(minClear, l.getMinClearance());
            }
        }
        params.setMinClearance(minClear == Double.MAX_VALUE ? -1 : minClear);

        params.setTotalHoles(holes.size());
        params.setPlatedHoles((int) holes.stream().filter(DrillHole::isPlated).count());
        params.setNonPlatedHoles(params.getTotalHoles() - params.getPlatedHoles());

        double minHoleDiam = holes.stream().mapToDouble(DrillHole::getDiameter).min().orElse(-1);
        params.setMinHoleDiameter(minHoleDiam);

        params.setBoardMaterial(null);
        params.setSurfaceFinish(null);

        return params;
    }

    private RectBounds findOutlineBounds(List<BoardLayer> layers) {
        for (BoardLayer l : layers) {
            if (l.getType() == LayerType.OUTLINE && l.getBounds() != null) return l.getBounds();
        }
        return null;
    }

    private RectBounds computeUnionBounds(List<BoardLayer> layers) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (BoardLayer l : layers) {
            RectBounds b = l.getBounds();
            if (b != null) {
                minX = Math.min(minX, b.getMinX());
                maxX = Math.max(maxX, b.getMaxX());
                minY = Math.min(minY, b.getMinY());
                maxY = Math.max(maxY, b.getMaxY());
            }
        }
        if (minX == Double.MAX_VALUE) return new RectBounds(0,0,0,0);
        return new RectBounds(minX, maxX, minY, maxY);
    }

    private int countCopperLayers(List<BoardLayer> layers) {
        return (int) layers.stream().filter(l -> l.getType() == LayerType.COPPER).count();
    }

    private static class ParseLayerResult {
        final BoardLayer layer;
        final GerberDocument document;
        ParseLayerResult(BoardLayer layer, GerberDocument document) {
            this.layer = layer;
            this.document = document;
        }
    }
}