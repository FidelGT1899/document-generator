package com.fidelg.documentgenerator.domain.type;

import com.fidelg.documentgenerator.domain.demand.FactNode;
import com.fidelg.documentgenerator.domain.demand.FactTreeOperations;
import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.demand.SectionKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlimentosDemandTypeTest {

    private final AlimentosDemandType type = new AlimentosDemandType();

    @Test
    void exposesTheSectionsOfTheApprovedStructure() {
        List<String> ids = type.sections().stream().map(SectionSpec::id).toList();

        assertEquals(List.of("cabecera", "titulo", "sumilla", "encabezamiento", "demandado",
                "petitorio", "hechos", "juridicos", "representacion", "monto", "via",
                "medios", "otrosies", "cierre"), ids);
    }

    @Test
    void declaresRomanNumberingOnlyForTheNumberedSections() {
        assertTrue(type.spec("demandado").romanNumbered());
        assertTrue(type.spec("petitorio").romanNumbered());
        assertTrue(type.spec("hechos").romanNumbered());
        assertTrue(type.spec("juridicos").romanNumbered());
        assertTrue(type.spec("medios").romanNumbered());

        assertFalse(type.spec("cabecera").romanNumbered());
        assertFalse(type.spec("titulo").romanNumbered());
        assertFalse(type.spec("sumilla").romanNumbered());
        assertFalse(type.spec("encabezamiento").romanNumbered());
        assertFalse(type.spec("otrosies").romanNumbered());
        assertFalse(type.spec("cierre").romanNumbered());
    }

    @Test
    void mapsEachSectionToItsEditorKind() {
        assertEquals(SectionKind.SIMPLE_INPUT, type.spec("cabecera").kind());
        assertEquals(SectionKind.GENERATED_TEXT, type.spec("encabezamiento").kind());
        assertEquals(SectionKind.EDITABLE_TEXT, type.spec("representacion").kind());
        assertEquals(SectionKind.COLLECTION, type.spec("medios").kind());
        assertEquals(SectionKind.FACTS, type.spec("hechos").kind());
    }

    @Test
    void generatesEncabezamientoFromStructuredInputs() {
        Section section = new Section("encabezamiento");
        section.setInput("juzgado", "DE LO CIVIL DE LIMA NORTE");
        section.setInput("demandante", "MARÍA PÉREZ");
        section.setInput("documento", "45678912");
        section.setInput("domicilio", "Av. Los Olivos 123");
        section.setInput("notificaciones", "Jr. Comercio 45");

        String content = type.generateContent(type.spec("encabezamiento"), section);

        assertTrue(content.startsWith("SEÑORA JUEZ DEL DE LO CIVIL DE LIMA NORTE:"));
        assertTrue(content.contains("MARÍA PÉREZ"));
        assertTrue(content.contains("Documento Nacional de Identidad Nº 45678912"));
        assertTrue(content.contains("domiciliada en Av. Los Olivos 123"));
        assertTrue(content.contains("notificaciones en Jr. Comercio 45"));
        assertTrue(content.endsWith("a usted atentamente digo:"));
    }

    @Test
    void generatesTheClosingInTheExampleFormat() {
        Section section = new Section("cierre");
        section.setInput("ciudad", "Morropón");
        section.setInput("fecha", "27 de diciembre del 2017");
        section.setInput("demandante", "FEDELISA GARCÍA DOMÍNGUEZ");
        section.setInput("documento", "40860823");

        String content = type.generateContent(type.spec("cierre"), section);

        assertEquals("Morropón, 27 de diciembre del 2017\n"
                + "FEDELISA GARCÍA DOMÍNGUEZ\n"
                + "DNI Nº 40860823", content);
    }

    @Test
    void generatesAClosingFromWhateverPartIsKnown() {
        Section section = new Section("cierre");
        section.setInput("fecha", "Enero del 2016");

        assertEquals("Enero del 2016", type.generateContent(type.spec("cierre"), section));
    }

    @Test
    void generatesEmptyContentWhenEveryInputIsBlank() {
        assertEquals("", type.generateContent(type.spec("encabezamiento"), new Section("encabezamiento")));
        assertEquals("", type.generateContent(type.spec("petitorio"), new Section("petitorio")));
        assertEquals("", type.generateContent(type.spec("demandado"), new Section("demandado")));
        assertEquals("", type.generateContent(type.spec("cierre"), new Section("cierre")));
    }

    @Test
    void joinsAlimentistasAsASentence() {
        Section section = new Section("petitorio");
        section.setInput("proceso", "ordenaria");
        section.setInput("tipoDemanda", "DEMANDA DE ALIMENTOS");
        section.setInput("demandado", "JUAN PÉREZ");
        section.setInput("monto", "S/ 1,500.00");
        section.setInput("profesion", "COMERCIANTE");
        section.setInput("alimentistas", "ANA PÉREZ\nLUIS PÉREZ\nBETO PÉREZ");

        String content = type.generateContent(type.spec("petitorio"), section);

        assertTrue(content.contains("a favor de sus menores hijos: ANA PÉREZ, LUIS PÉREZ y BETO PÉREZ"));
        assertTrue(content.contains("en vía de ordenaria"));
    }

    @Test
    void composesAFactFromLabelAndNarrative() {
        Section section = new Section("hechos");
        FactTemplate template = type.spec("hechos").factTemplate("hecho");
        FactNode node = FactNode.fact("hecho");
        node.setInput("rotulo", "PRIMERO");
        node.setInput("narrativa", "El demandado es padre de la menor.");

        String content = type.generateFact(template, node, section);

        assertEquals("PRIMERO.- El demandado es padre de la menor.", content);
    }

    @Test
    void usesNarrativeAloneWhenTheLabelIsMissing() {
        Section section = new Section("hechos");
        FactTemplate template = type.spec("hechos").factTemplate("hecho");
        FactNode node = FactNode.fact("hecho");
        node.setInput("narrativa", "Sin rótulo.");

        assertEquals("Sin rótulo.", type.generateFact(template, node, section));
    }

    @Test
    void generatesClosingFromEveryFactIncludingSubFactsButNotSubtitles() {
        Section section = new Section("hechos");
        FactNode first = FactTreeOperations.addFact(section, null, "hecho");
        first.applyGenerated("Primer hecho.");
        FactNode second = FactTreeOperations.addFact(section, null, "hecho");
        second.applyGenerated("Segundo hecho.");
        FactNode child = FactTreeOperations.addFact(section, second.getId(), "hecho");
        child.applyGenerated("Subhecho del segundo.");
        FactNode subtitle = FactTreeOperations.addSubtitle(section, null, "Rótulo");
        subtitle.applyGenerated("Rótulo ignorado");

        assertEquals("Primer hecho.\nSegundo hecho.\nSubhecho del segundo.",
                type.generateClosing(section));
    }

    @Test
    void formatsLegalGroupsWithBulletArticles() {
        Section section = new Section("juridicos");
        section.getItems().add(new com.fidelg.documentgenerator.domain.demand.CollectionItem(
                "Código Civil", "Artículo 476.- Contenido de la obligación de alimentos"));
        section.getItems().add(new com.fidelg.documentgenerator.domain.demand.CollectionItem(null,
                "Artículo 480.- Medidas cautelares"));

        String content = type.generateContent(type.spec("juridicos"), section);

        assertTrue(content.contains("CÓDIGO CIVIL"));
        assertTrue(content.contains("❖ Artículo 476.- Contenido de la obligación de alimentos"));
        assertTrue(content.contains("❖ Artículo 480.- Medidas cautelares"));
        assertFalse(content.contains("null"));
    }
}
