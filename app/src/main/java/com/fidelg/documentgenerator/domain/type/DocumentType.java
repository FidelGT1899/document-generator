package com.fidelg.documentgenerator.domain.type;

import com.fidelg.documentgenerator.domain.demand.FactNode;
import com.fidelg.documentgenerator.domain.demand.Section;

import java.util.List;

/**
 * Definición de un tipo de documento judicial (demanda de alimentos, futuras
 * demandas, etc.). Concentra aquí la estructura de secciones y la generación de
 * contenido: el resto del sistema es genérico y resuelve el tipo por id, sin
 * ramificaciones por tipo concreto.
 */
public interface DocumentType {

    String id();

    String displayName();

    List<SectionSpec> sections();

    default SectionSpec spec(String sectionId) {
        for (SectionSpec spec : sections()) {
            if (spec.id().equals(sectionId)) {
                return spec;
            }
        }
        throw new IllegalArgumentException("Sección desconocida en el tipo " + id() + ": " + sectionId);
    }

    /**
     * Contenido inicial de una sección a partir de sus datos estructurados.
     * Solo se invoca para secciones con generación ({@code GENERATED_TEXT}).
     */
    String generateContent(SectionSpec spec, Section section);

    /** Contenido inicial de un hecho a partir de los datos de su plantilla. */
    String generateFact(FactTemplate template, FactNode node, Section section);

    /** Cierre/conclusión de los fundamentos fácticos a partir de los hechos anteriores. */
    String generateClosing(Section section);
}
