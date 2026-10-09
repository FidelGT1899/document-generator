package com.fidelg.documentgenerator.domain.demand;

import java.util.ArrayList;
import java.util.List;

/**
 * Documento de demanda en curso (borrador o finalizado). Es la fuente de verdad
 * del estado: la vista previa y el DOCX se derivan de este modelo.
 */
public class DemandDocument {

    private String typeId;
    private List<Section> sections = new ArrayList<>();

    public DemandDocument() {
    }

    public DemandDocument(String typeId) {
        this.typeId = typeId;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public List<Section> getSections() {
        return sections;
    }

    public void setSections(List<Section> sections) {
        this.sections = sections == null ? new ArrayList<>() : sections;
    }

    public Section section(String id) {
        for (Section section : sections) {
            if (section.getId().equals(id)) {
                return section;
            }
        }
        throw new IllegalArgumentException("Sección desconocida: " + id);
    }
}
