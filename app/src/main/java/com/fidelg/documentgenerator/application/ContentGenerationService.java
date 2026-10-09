package com.fidelg.documentgenerator.application;

import com.fidelg.documentgenerator.domain.demand.FactNode;
import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.type.DocumentType;
import com.fidelg.documentgenerator.domain.type.FactTemplate;
import com.fidelg.documentgenerator.domain.type.SectionSpec;

/**
 * Genera contenido inicial (o lo regenera) a partir de los datos estructurados.
 *
 * <p>Regla central: si el usuario ya modificó el contenido generado, la
 * regeneración automática no lo sobrescribe. Solo un regenerado explícito,
 * confirmado por el usuario, lo reemplaza.</p>
 */
public class ContentGenerationService {

    /**
     * Regenera solo si el contenido no fue editado manualmente y no está vacío
     * por obra del usuario. Devuelve true si el contenido cambió.
     */
    public boolean regenerateIfClean(Section section, SectionSpec spec, DocumentType type) {
        if (section.isDirty()) {
            return false;
        }
        if (!hasAnyInput(section)) {
            if (section.getContent().isEmpty()) {
                return false;
            }
            section.applyGenerated("");
            return true;
        }
        String generated = type.generateContent(spec, section);
        if (generated.equals(section.getContent())) {
            return false;
        }
        section.applyGenerated(generated);
        return true;
    }

    /** Regeneración explícita pedida por el usuario: pisa la edición. */
    public void regenerateForced(Section section, SectionSpec spec, DocumentType type) {
        section.applyGenerated(type.generateContent(spec, section));
    }

    public boolean regenerateFactIfClean(FactNode node, Section section, FactTemplate template, DocumentType type) {
        if (node.isDirty() || !hasAnyInput(node.getInputs())) {
            return false;
        }
        String generated = type.generateFact(template, node, section);
        if (generated.equals(node.getContent())) {
            return false;
        }
        node.applyGenerated(generated);
        return true;
    }

    public void regenerateFactForced(FactNode node, Section section, FactTemplate template, DocumentType type) {
        node.applyGenerated(type.generateFact(template, node, section));
    }

    public boolean regenerateClosingIfClean(Section section, DocumentType type) {
        if (section.isClosingDirty()) {
            return false;
        }
        String generated = type.generateClosing(section);
        if (generated.equals(section.getClosingContent())) {
            return false;
        }
        section.applyClosingGenerated(generated);
        return true;
    }

    public void regenerateClosingForced(Section section, DocumentType type) {
        section.applyClosingGenerated(type.generateClosing(section));
    }

    private boolean hasAnyInput(Section section) {
        return hasAnyInput(section.getInputs());
    }

    private boolean hasAnyInput(java.util.Map<String, String> inputs) {
        return inputs.values().stream().anyMatch(value -> value != null && !value.isBlank());
    }
}
