package com.fidelg.documentgenerator.domain;

/**
 * Fragmento de un párrafo con su propio estilo. Los títulos de sección y los
 * rótulos numerados necesitan negrita/subrayado dentro del mismo párrafo.
 */
public record DocumentRun(String text, boolean bold, boolean italic, boolean underline) {

    public static DocumentRun plain(String text) {
        return new DocumentRun(text, false, false, false);
    }

    public static DocumentRun bold(String text) {
        return new DocumentRun(text, true, false, false);
    }

    public static DocumentRun boldUnderline(String text) {
        return new DocumentRun(text, true, false, true);
    }
}
