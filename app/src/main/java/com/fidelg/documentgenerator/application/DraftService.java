package com.fidelg.documentgenerator.application;

import com.fidelg.documentgenerator.domain.demand.DemandDocument;
import com.fidelg.documentgenerator.domain.type.DocumentType;
import com.fidelg.documentgenerator.domain.type.DocumentTypeRegistry;
import com.fidelg.documentgenerator.infrastructure.JsonDraftStore;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Guarda y carga borradores de demanda. Al cargar valida que el tipo de
 * documento siga existiendo en el registro.
 */
public class DraftService {

    private final JsonDraftStore store;
    private final DocumentTypeRegistry registry;

    public DraftService(JsonDraftStore store, DocumentTypeRegistry registry) {
        this.store = store;
        this.registry = registry;
    }

    public void save(DemandDocument document, Path file) throws IOException {
        store.save(document, file);
    }

    public DemandDocument load(Path file) throws IOException {
        DemandDocument document = store.load(file);
        DocumentType type = registry.get(document.getTypeId());
        if (document.getSections().size() != type.sections().size()) {
            throw new IOException("El borrador no coincide con la estructura del tipo " + type.id());
        }
        return document;
    }
}
