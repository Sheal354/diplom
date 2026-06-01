package com.diplom.gerber.parser;

import com.deltaproto.deltagerber.model.drill.DrillDocument;
import com.deltaproto.deltagerber.model.drill.DrillOperation;
import com.deltaproto.deltagerber.model.drill.Tool;
import com.deltaproto.deltagerber.model.gerber.BoundingBox;
import com.diplom.gerber.model.DrillHole;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/** Обработчик файлов сверловки**/
@Component
public class DrillAnalyzer {

    private static final Set<String> EXCELLON_EXTENSIONS = Set.of(
            ".DRL", ".TXT", ".XLN", ".EXC", ".DNC"
    );

    /**
     * Проверяет, относится ли файл к формату Excellon по расширению.
     *
     * @param fileName имя файла
     * @return {@code true}, если расширение входит в список {@link #EXCELLON_EXTENSIONS}
     */
    public boolean isExcellonByExtension(String fileName) {
        String upper = fileName.toUpperCase();
        for (String ext : EXCELLON_EXTENSIONS) {
            if (upper.endsWith(ext)) return true;
        }
        return false;
    }

    /**
     * Извлекает список отверстий из документа сверловки.
     *
     * @param drillDoc распарсенный {@link DrillDocument}
     * @param fileName имя файла
     * @return список {@link DrillHole} с координатами, диаметром и типом металлизации
     */
    public List<DrillHole> extractDrillHoles(DrillDocument drillDoc, String fileName) {
        List<DrillHole> holes = new ArrayList<>();
        boolean plated = determinePlatingType(drillDoc, fileName);

        for (DrillOperation op : drillDoc.getOperations()) {
            BoundingBox bb = op.getBoundingBox();
            double x = (bb.getMinX() + bb.getMaxX()) / 2.0;
            double y = (bb.getMinY() + bb.getMaxY()) / 2.0;

            Tool tool = op.getTool();
            double diameter = tool != null ? tool.getDiameter() : 0.0;

            holes.add(new DrillHole(x, y, diameter, plated));
        }
        return holes;
    }

    /** Определяет тип металлизации отверстий. */
    private boolean determinePlatingType(DrillDocument drillDoc, String fileName) {
        for (String comment : drillDoc.getComments()) {
            String upper = comment.toUpperCase();
            if (upper.contains("TYPE=PLATED") || upper.contains("PTH")) return true;
            if (upper.contains("TYPE=NON_PLATED") || upper.contains("NPTH")) return false;
        }
        String upperName = fileName.toUpperCase();
        if (upperName.contains("PTH") || upperName.contains("PLATED")) return true;
        if (upperName.contains("NPTH") || upperName.contains("NON_PLATED")) return false;
        return true; // по умолчанию металлизированные
    }

    /**
     * Определяет наличие глухих и скрытых переходных отверстий.
     * <p>
     * Анализирует комментарии и имя файла на ключевые слова:
     * {@code BLIND}, {@code BURIED}, {@code BURIEDVIA} или паттерн слоёв {@code L1-L2}.
     *
     * @param drillDoc документ сверловки
     * @param fileName имя файла
     * @return массив из двух элементов: [hasBlind, hasBuried]
     */
    public boolean[] determineViaType(DrillDocument drillDoc, String fileName) {
        boolean hasBlind = false;
        boolean hasBuried = false;

        StringBuilder sb = new StringBuilder();
        for (String comment : drillDoc.getComments()) {
            sb.append(comment.toUpperCase()).append(" ");
        }
        sb.append(fileName.toUpperCase());
        String combined = sb.toString();

        if (combined.contains("BLIND") || combined.matches(".*L\\d+-L\\d+.*")) {
            hasBlind = true;
        }
        if (combined.contains("BURIED") || combined.contains("BURIEDVIA")) {
            hasBuried = true;
        }
        return new boolean[]{hasBlind, hasBuried};
    }
}