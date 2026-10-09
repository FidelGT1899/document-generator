package com.fidelg.documentgenerator.application;

import com.fidelg.documentgenerator.domain.demand.FactNode;
import com.fidelg.documentgenerator.domain.demand.FactTreeOperations;
import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.type.AlimentosDemandType;
import com.fidelg.documentgenerator.domain.type.FactTemplate;
import com.fidelg.documentgenerator.domain.type.SectionSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentGenerationServiceTest {

    private final AlimentosDemandType type = new AlimentosDemandType();
    private final ContentGenerationService service = new ContentGenerationService();

    private Section encabezamiento() {
        Section section = new Section("encabezamiento");
        section.setInput("juzgado", "DE LO CIVIL");
        section.setInput("demandante", "MARÍA PÉREZ");
        section.setInput("documento", "45678912");
        section.setInput("domicilio", "Av. Los Olivos 123");
        section.setInput("notificaciones", "Jr. Comercio 45");
        return section;
    }

    @Test
    void generatesContentFromStructuredInputsWhenTheSectionIsClean() {
        Section section = encabezamiento();
        SectionSpec spec = type.spec("encabezamiento");

        assertTrue(service.regenerateIfClean(section, spec, type));

        assertEquals(type.generateContent(spec, section), section.getContent());
        assertFalse(section.isDirty());
        assertEquals(section.getGeneratedContent(), section.getContent());
    }

    @Test
    void neverOverwritesContentEditedByTheUser() {
        Section section = encabezamiento();
        SectionSpec spec = type.spec("encabezamiento");
        service.regenerateIfClean(section, spec, type);
        section.markContent("Redacción manual del usuario.");

        assertFalse(service.regenerateIfClean(section, spec, type));

        assertEquals("Redacción manual del usuario.", section.getContent());
        assertTrue(section.isDirty());
    }

    @Test
    void forcedRegenerationOverwritesEditedContent() {
        Section section = encabezamiento();
        SectionSpec spec = type.spec("encabezamiento");
        service.regenerateIfClean(section, spec, type);
        section.markContent("Redacción manual del usuario.");

        service.regenerateForced(section, spec, type);

        assertEquals(type.generateContent(spec, section), section.getContent());
        assertFalse(section.isDirty());
    }

    @Test
    void clearsContentWhenEveryInputIsRemoved() {
        Section section = encabezamiento();
        SectionSpec spec = type.spec("encabezamiento");
        service.regenerateIfClean(section, spec, type);
        section.getInputs().clear();

        assertTrue(service.regenerateIfClean(section, spec, type));
        assertEquals("", section.getContent());
        assertFalse(section.isDirty());
    }

    @Test
    void leavesAnUntouchedSectionAlone() {
        Section section = new Section("encabezamiento");

        assertFalse(service.regenerateIfClean(section, type.spec("encabezamiento"), type));
        assertEquals("", section.getContent());
        assertFalse(section.isDirty());
    }

    @Test
    void regeneratesCleanFactsButKeepsEditedOnes() {
        Section section = new Section("hechos");
        FactTemplate template = type.spec("hechos").factTemplate("hecho");

        FactNode clean = FactTreeOperations.addFact(section, null, "hecho");
        clean.setInput("rotulo", "PRIMERO");
        clean.setInput("narrativa", "Narrativa.");
        assertTrue(service.regenerateFactIfClean(clean, section, template, type));
        assertEquals("PRIMERO.- Narrativa.", clean.getContent());
        assertFalse(clean.isDirty());

        clean.markContent("Hecho reescrito.");
        assertFalse(service.regenerateFactIfClean(clean, section, template, type));
        assertEquals("Hecho reescrito.", clean.getContent());
        assertTrue(clean.isDirty());
    }

    @Test
    void doesNotRegenerateFactsWithoutInputs() {
        Section section = new Section("hechos");
        FactTemplate template = type.spec("hechos").factTemplate("hecho");
        FactNode node = FactTreeOperations.addFact(section, null, "hecho");

        assertFalse(service.regenerateFactIfClean(node, section, template, type));
        assertEquals("", node.getContent());
    }

    @Test
    void regeneratesTheClosingWhileItIsCleanAndKeepsItOnceEdited() {
        Section section = new Section("hechos");
        FactNode node = FactTreeOperations.addFact(section, null, "hecho");
        node.applyGenerated("Primer hecho.");

        assertTrue(service.regenerateClosingIfClean(section, type));
        assertEquals("Primer hecho.", section.getClosingContent());
        assertFalse(section.isClosingDirty());

        section.markClosingContent("Cierre reescrito.");
        node.applyGenerated("Hecho alterado.");

        assertFalse(service.regenerateClosingIfClean(section, type));
        assertEquals("Cierre reescrito.", section.getClosingContent());
        assertTrue(section.isClosingDirty());
    }

    @Test
    void forcedClosingRegenerationOverwritesTheEditedClosing() {
        Section section = new Section("hechos");
        FactNode node = FactTreeOperations.addFact(section, null, "hecho");
        node.applyGenerated("Primer hecho.");
        section.markClosingContent("Cierre reescrito.");

        service.regenerateClosingForced(section, type);

        assertEquals("Primer hecho.", section.getClosingContent());
        assertFalse(section.isClosingDirty());
    }
}
