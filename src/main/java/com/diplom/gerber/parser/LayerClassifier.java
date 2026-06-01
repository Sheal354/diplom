package com.diplom.gerber.parser;

import com.deltaproto.deltagerber.model.gerber.GerberDocument;
import com.diplom.gerber.model.BoardLayer;
import com.diplom.gerber.model.LayerType;
import org.springframework.stereotype.Component;

import java.util.Set;

/**Класс классификации слоя**/
@Component
public class LayerClassifier {

    // Расширения, по которым мы распознаём Gerber‑слои (если нет метаданных)
    private static final Set<String> GERBER_EXTENSIONS = Set.of(
            ".GBR", ".GERBER", ".GTL", ".GBL", ".GTS", ".GBS",
            ".GTO", ".GBO", ".GKO", ".GM1", ".GPT", ".GPB",
            ".GDD", ".G1", ".G2", ".G3"
    );

    /**
     * Классифицирует слой по цепочке:
     * 1. Метаданные Gerber X2 (FileFunction)
     * 2. Расширение файла (строгий список GERBER_EXTENSIONS)
     * 3. Ключевые слова в имени файла
     *
     * @return true, если слой успешно классифицирован, иначе false
     */
    public boolean classifyLayer(GerberDocument doc, String fileName, BoardLayer layer) {
        String fileFunction = doc.getFileFunction();
        if (fileFunction != null && !fileFunction.isEmpty()) {
            parseFileFunction(fileFunction, layer);
            return true;
        }

        // Метаданных нет – пробуем по расширению
        if (classifyByExtension(fileName, layer)) {
            return true;
        }

        // Расширение не подошло – пробуем ключевые слова в имени
        if (classifyByFileName(fileName, layer)) {
            return true;
        }

        return false;   // не удалось классифицировать
    }

    /** Разбирает строку FileFunction (Gerber X2). */
    private void parseFileFunction(String fileFunction, BoardLayer layer) {
        String[] parts = fileFunction.split(",");
        if (parts.length == 0) return;
        String function = parts[0].trim();
        layer.setName(fileFunction);

        switch (function) {
            case "Copper":
                layer.setType(LayerType.COPPER);
                if (parts.length >= 3) layer.setSide(parts[2].trim());
                if (parts.length >= 2) {
                    String num = parts[1].trim();
                    if (num.startsWith("L")) num = num.substring(1);
                    try {
                        layer.setLayerNumber(Integer.parseInt(num));
                    } catch (NumberFormatException ignored) {}
                }
                break;
            case "SolderMask":
                layer.setType(LayerType.SOLDER_MASK);
                if (parts.length >= 2) layer.setSide(parts[1].trim());
                break;
            case "Silkscreen":
                layer.setType(LayerType.SILKSCREEN);
                if (parts.length >= 2) layer.setSide(parts[1].trim());
                break;
            case "Paste":
                layer.setType(LayerType.PASTE);
                if (parts.length >= 2) layer.setSide(parts[1].trim());
                break;
            case "Profile":
                layer.setType(LayerType.OUTLINE);
                break;
            case "DrillDrawing":
                layer.setType(LayerType.DRILL_DRAWING);
                break;
            default:
                layer.setType(LayerType.UNKNOWN);
        }
    }

    /**
     * Классификация по строгому списку расширений.
     * @return true, если расширение известно
     */
    private boolean classifyByExtension(String fileName, BoardLayer layer) {
        String upper = fileName.toUpperCase();

        // Ищем точное совпадение с одним из известных расширений
        String matchedExt = null;
        for (String ext : GERBER_EXTENSIONS) {
            if (upper.endsWith(ext)) {
                matchedExt = ext;
                break;
            }
        }
        if (matchedExt == null) return false;

        switch (matchedExt) {
            case ".GTL":
                layer.setType(LayerType.COPPER); layer.setSide("TOP"); break;
            case ".GBL":
                layer.setType(LayerType.COPPER); layer.setSide("BOTTOM"); break;
            case ".G1": case ".G2": case ".G3":
                layer.setType(LayerType.COPPER); break;
            case ".GTS":
                layer.setType(LayerType.SOLDER_MASK); layer.setSide("TOP"); break;
            case ".GBS":
                layer.setType(LayerType.SOLDER_MASK); layer.setSide("BOTTOM"); break;
            case ".GTO":
                layer.setType(LayerType.SILKSCREEN); layer.setSide("TOP"); break;
            case ".GBO":
                layer.setType(LayerType.SILKSCREEN); layer.setSide("BOTTOM"); break;
            case ".GKO": case ".GM1":
                layer.setType(LayerType.OUTLINE); break;
            case ".GPT":
                layer.setType(LayerType.PASTE); layer.setSide("TOP"); break;
            case ".GPB":
                layer.setType(LayerType.PASTE); layer.setSide("BOTTOM"); break;
            case ".GDD":
                layer.setType(LayerType.DRILL_DRAWING); break;
            // Для расширений .GBR, .GERBER, .BOT, .TOP, .SMB, .SMT, .SST, .SSB, .SMD
            // не задаём жёсткую классификацию – они будут обработаны на 3-м этапе
            default:
                return false;
        }
        layer.setName(fileName);
        return true;
    }

    /**
     * Классификация по ключевым словам в имени файла.
     * Применяется только для файлов с нестандартными расширениями (.gbr, .gerber и т.п.).
     * @return true, если удалось определить тип по имени
     */
    private boolean classifyByFileName(String fileName, BoardLayer layer) {
        String upper = fileName.toUpperCase();

        // Медные слои
        if (upper.contains("COPPER_BOTTOM") || upper.contains("COPPER.BOT") ||
                (upper.contains("BOTTOM") && upper.contains("COPPER"))) {
            layer.setType(LayerType.COPPER);
            layer.setSide("BOTTOM");
        } else if (upper.contains("COPPER_TOP") || upper.contains("COPPER.TOP") ||
                (upper.contains("TOP") && upper.contains("COPPER"))) {
            layer.setType(LayerType.COPPER);
            layer.setSide("TOP");
        }
        // Паяльная маска
        else if (upper.contains("SOLDERMASK_BOTTOM") || upper.contains("SOLDERMASK.BOT") ||
                (upper.contains("BOTTOM") && upper.contains("SOLDERMASK"))) {
            layer.setType(LayerType.SOLDER_MASK);
            layer.setSide("BOTTOM");
        } else if (upper.contains("SOLDERMASK_TOP") || upper.contains("SOLDERMASK.TOP") ||
                (upper.contains("TOP") && upper.contains("SOLDERMASK"))) {
            layer.setType(LayerType.SOLDER_MASK);
            layer.setSide("TOP");
        }
        // Шелкография
        else if (upper.contains("SILKSCREEN_BOTTOM") || upper.contains("SILKSCREEN.BOT") ||
                (upper.contains("BOTTOM") && upper.contains("SILKSCREEN"))) {
            layer.setType(LayerType.SILKSCREEN);
            layer.setSide("BOTTOM");
        } else if (upper.contains("SILKSCREEN_TOP") || upper.contains("SILKSCREEN.TOP") ||
                (upper.contains("TOP") && upper.contains("SILKSCREEN"))) {
            layer.setType(LayerType.SILKSCREEN);
            layer.setSide("TOP");
        }
        // Контур
        else if (upper.contains("OUTLINE")) {
            layer.setType(LayerType.OUTLINE);
        }
        // Паяльная паста
        else if (upper.contains("SMDMASK") || upper.contains("PASTE")) {
            layer.setType(LayerType.PASTE);
            if (upper.contains("TOP")) layer.setSide("TOP");
            else if (upper.contains("BOTTOM")) layer.setSide("BOTTOM");
        }
        // Чертёж отверстий
        else if (upper.contains("DRILLDRAWING") || upper.contains("DRILL_DRAWING")) {
            layer.setType(LayerType.DRILL_DRAWING);
        }
        // Внутренние медные слои (если имя содержит, например, "COPPER_INNER")
        else if (upper.contains("COPPER_INNER") || upper.contains("COPPER.L2") ||
                upper.contains("COPPER.L3")) {
            layer.setType(LayerType.COPPER);
        }
        else {
            return false;
        }

        layer.setName(fileName);
        return true;
    }
}