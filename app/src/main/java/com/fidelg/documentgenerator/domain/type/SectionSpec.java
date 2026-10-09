package com.fidelg.documentgenerator.domain.type;

import com.fidelg.documentgenerator.domain.demand.SectionKind;

import java.util.List;

/**
 * Descripción estructural de una sección de un tipo de documento: qué inputs
 * tiene, si produce texto editable y cómo se numera/renderiza.
 *
 * <p>La numeración romana no se guarda en la spec: solo se declara si la
 * sección participa en ella y el ensamblador deriva el número de su posición.</p>
 */
public record SectionSpec(String id,
                          SectionKind kind,
                          String title,
                          boolean renderTitle,
                          boolean romanNumbered,
                          boolean hasSectionContent,
                          CollectionFormat collectionFormat,
                          List<InputSpec> inputs,
                          List<FactTemplate> factTemplates) {

    public SectionSpec {
        inputs = List.copyOf(inputs);
        factTemplates = List.copyOf(factTemplates);
    }

    public static SectionSpec of(String id, SectionKind kind, String title, List<InputSpec> inputs) {
        return new SectionSpec(id, kind, title, false, false, false, null, inputs, List.of());
    }

    public SectionSpec withRomanNumbering() {
        return new SectionSpec(id, kind, title, true, true, hasSectionContent,
                collectionFormat, inputs, factTemplates);
    }

    public SectionSpec withVisibleTitle() {
        return new SectionSpec(id, kind, title, true, romanNumbered, hasSectionContent,
                collectionFormat, inputs, factTemplates);
    }

    public SectionSpec withCollection(CollectionFormat format, boolean sectionContent) {
        return new SectionSpec(id, kind, title, renderTitle, romanNumbered, sectionContent,
                format, inputs, factTemplates);
    }

    public SectionSpec withFacts(List<FactTemplate> templates) {
        return new SectionSpec(id, kind, title, renderTitle, romanNumbered, hasSectionContent,
                collectionFormat, inputs, templates);
    }

    public InputSpec input(String key) {
        for (InputSpec input : inputs) {
            if (input.key().equals(key)) {
                return input;
            }
        }
        throw new IllegalArgumentException("Input desconocido en la sección " + id + ": " + key);
    }

    public FactTemplate factTemplate(String templateId) {
        for (FactTemplate template : factTemplates) {
            if (template.id().equals(templateId)) {
                return template;
            }
        }
        throw new IllegalArgumentException("Plantilla de hecho desconocida: " + templateId);
    }
}
