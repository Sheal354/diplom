package com.diplom.gerber.parser;

import com.diplom.gerber.model.BoardParameters;
import com.diplom.gerber.model.BoardLayer;
import java.util.Map;

/** Результат парсинга **/
public class ParseResult {
    private final BoardParameters boardParameters;
    private final Map<BoardLayer, Object> layerDocuments;   // GerberDocument или DrillDocument

    public ParseResult(BoardParameters boardParameters, Map<BoardLayer, Object> layerDocuments) {
        this.boardParameters = boardParameters;
        this.layerDocuments = layerDocuments;
    }

    public BoardParameters getBoardParameters() { return boardParameters; }
    public Map<BoardLayer, Object> getLayerDocuments() { return layerDocuments; }
}