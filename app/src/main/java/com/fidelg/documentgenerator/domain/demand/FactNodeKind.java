package com.fidelg.documentgenerator.domain.demand;

/**
 * Naturaleza de un nodo de la sección de fundamentos fácticos.
 */
public enum FactNodeKind {
    /** Rótulo agrupador de hechos (ej. "Necesidades del Acreedor Alimentario"). */
    SUBTITLE,
    /** Hecho numerado (3.1, 3.12, 3.12.1...) con sus propios datos estructurados. */
    FACT
}
