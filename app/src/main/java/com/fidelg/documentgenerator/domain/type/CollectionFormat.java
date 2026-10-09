package com.fidelg.documentgenerator.domain.type;

/**
 * Forma en que el ensamblador renderiza una colección.
 */
public enum CollectionFormat {
    /** Anexos numerados 1-A, 1-B... + descripción. */
    EVIDENCE,
    /** Otrosíes: ordinal derivado + título + contenido. */
    OTROSIE,
    /** Grupo normativo: título + artículos con viñeta. */
    LEGAL_GROUP
}
