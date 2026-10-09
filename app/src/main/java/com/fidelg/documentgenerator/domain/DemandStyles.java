package com.fidelg.documentgenerator.domain;

/**
 * Constantes de formato deducidas de los documentos DOCX de ejemplo.
 * Están en el dominio porque el ensamblador decide el formato y la
 * infraestructura (POI) y la vista previa solo lo aplican.
 */
public final class DemandStyles {

    public static final String FONT = "Bookman Old Style";
    public static final int BODY_FONT_SIZE = 12;

    /** Sangría de la cabecera y del título (twips), según los ejemplos. */
    public static final int HEADER_INDENT = 3119;

    /** Sangría de los hechos de primer nivel y su incremento por subnivel (twips). */
    public static final int FACT_INDENT = 709;
    public static final int FACT_INDENT_STEP = 1134;

    /** Sangría de los artículos de los fundamentos jurídicos (twips). */
    public static final int ARTICLE_INDENT = 720;

    public static final int TITLE_SPACE_AFTER = 6;
    public static final int SECTION_SPACE_AFTER = 8;
    public static final int PARAGRAPH_SPACE_AFTER = 4;

    private DemandStyles() {
    }

    public static int factIndent(int depth) {
        return FACT_INDENT + Math.max(0, depth) * FACT_INDENT_STEP;
    }
}
