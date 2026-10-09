package com.fidelg.documentgenerator.application;

import com.fidelg.documentgenerator.domain.demand.DemandDocument;
import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.type.DocumentType;
import com.fidelg.documentgenerator.domain.type.DocumentTypeRegistry;
import com.fidelg.documentgenerator.domain.type.SectionSpec;

/**
 * Crea una demanda vacía a partir de la estructura de su tipo de documento.
 */
public class CreateDemandService {

    private final DocumentTypeRegistry registry;

    public CreateDemandService(DocumentTypeRegistry registry) {
        this.registry = registry;
    }

    public DemandDocument execute(String typeId) {
        DocumentType type = registry.get(typeId);
        DemandDocument document = new DemandDocument(typeId);
        for (SectionSpec spec : type.sections()) {
            document.getSections().add(new Section(spec.id()));
        }
        return document;
    }
}
