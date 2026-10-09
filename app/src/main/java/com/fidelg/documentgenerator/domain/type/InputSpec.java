package com.fidelg.documentgenerator.domain.type;

/**
 * Dato estructurado simple de una sección o de un hecho.
 *
 * @param key       clave del valor dentro del mapa de inputs
 * @param label     texto visible en el formulario
 * @param multiline true para campos de varias líneas
 */
public record InputSpec(String key, String label, boolean multiline) {

    public InputSpec(String key, String label) {
        this(key, label, false);
    }
}
