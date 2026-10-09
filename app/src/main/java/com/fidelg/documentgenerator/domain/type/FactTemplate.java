package com.fidelg.documentgenerator.domain.type;

import java.util.List;

/**
 * Plantilla de un hecho: define qué datos estructurados propios tiene cada
 * hecho de este tipo de documento. No hay un formulario universal: cada tipo de
 * documento declara sus plantillas y cada plantilla sus campos.
 */
public record FactTemplate(String id, String name, List<InputSpec> inputs) {

    public FactTemplate {
        inputs = List.copyOf(inputs);
    }
}
