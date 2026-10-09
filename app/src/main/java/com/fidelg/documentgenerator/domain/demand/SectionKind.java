package com.fidelg.documentgenerator.domain.demand;

/**
 * Tipo estructural de una sección. Determina qué componentes de {@link Section}
 * están activos y qué panel debe mostrar la vista.
 */
public enum SectionKind {
    /** Solo inputs simples; no produce texto (ej. cabecera del cuaderno). */
    SIMPLE_INPUT,
    /** Inputs → contenido inicial → textarea editable. */
    GENERATED_TEXT,
    /** Solo textarea editable, sin datos estructurados. */
    EDITABLE_TEXT,
    /** Lista ordenada de elementos (medios probatorios, otrosíes, grupos normativos). */
    COLLECTION,
    /** Árbol de hechos con subhechos y cierre opcional. */
    FACTS
}
