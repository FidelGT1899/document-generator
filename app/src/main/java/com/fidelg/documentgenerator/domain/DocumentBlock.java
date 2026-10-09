package com.fidelg.documentgenerator.domain;

import java.util.List;

/**
 * Un párrafo del documento final. Los estilos de carácter viven en los runs;
 * el bloque aporta tamaño, fuente, alineación y espaciado de párrafo.
 * La numeración no se almacena aquí: quien ensambla la deriva de la estructura.
 */
public record DocumentBlock(List<DocumentRun> runs,
                            int fontSize,
                            Alignment alignment,
                            String fontName,
                            int leftIndent,
                            int spaceAfter) {

    public enum Alignment {
        LEFT,
        CENTER,
        JUSTIFY
    }

    public DocumentBlock {
        runs = List.copyOf(runs);
        if (fontName == null || fontName.isBlank()) {
            throw new IllegalArgumentException("La fuente del bloque es obligatoria.");
        }
    }

    public static DocumentBlock plain(String text, Alignment alignment, String fontName) {
        return new DocumentBlock(List.of(DocumentRun.plain(text)), 12, alignment, fontName, 0, 0);
    }

    public static DocumentBlock plain(String text) {
        return plain(text, Alignment.JUSTIFY, DemandStyles.FONT);
    }

    public String text() {
        StringBuilder builder = new StringBuilder();
        for (DocumentRun run : runs) {
            builder.append(run.text());
        }
        return builder.toString();
    }

    public boolean bold() {
        return !runs.isEmpty() && runs.stream().allMatch(DocumentRun::bold);
    }

    public boolean italic() {
        return !runs.isEmpty() && runs.stream().allMatch(DocumentRun::italic);
    }
}
