package com.fidelg.documentgenerator.domain.type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resuelve el tipo de documento por su id. Es el único punto donde se concretan
 * los tipos existentes: añadir un tipo nuevo no exige tocar el resto del código.
 */
public final class DocumentTypeRegistry {

    private final Map<String, DocumentType> types = new LinkedHashMap<>();

    public void register(DocumentType type) {
        if (types.putIfAbsent(type.id(), type) != null) {
            throw new IllegalArgumentException("Tipo de documento duplicado: " + type.id());
        }
    }

    public DocumentType get(String typeId) {
        DocumentType type = types.get(typeId);
        if (type == null) {
            throw new IllegalArgumentException("Tipo de documento desconocido: " + typeId);
        }
        return type;
    }

    public List<DocumentType> all() {
        return new ArrayList<>(types.values());
    }
}
