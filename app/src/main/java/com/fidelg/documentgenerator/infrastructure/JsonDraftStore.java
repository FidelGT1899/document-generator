package com.fidelg.documentgenerator.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fidelg.documentgenerator.domain.demand.DemandDocument;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Persistencia de borradores de demanda en JSON (un archivo por demanda).
 * Guarda el modelo completo —incluidos inputs, contenido generado, contenido
 * editable y estado sucio— para poder continuar sin perder ediciones manuales.
 */
public class JsonDraftStore {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public void save(DemandDocument document, Path file) throws IOException {
        mapper.writeValue(file.toFile(), document);
    }

    public DemandDocument load(Path file) throws IOException {
        return mapper.readValue(file.toFile(), DemandDocument.class);
    }
}
