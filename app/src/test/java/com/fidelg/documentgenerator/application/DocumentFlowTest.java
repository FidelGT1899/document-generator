package com.fidelg.documentgenerator.application;

import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.domain.demand.CollectionItem;
import com.fidelg.documentgenerator.domain.demand.DemandDocument;
import com.fidelg.documentgenerator.domain.demand.DemandDocumentAssembler;
import com.fidelg.documentgenerator.domain.demand.FactNode;
import com.fidelg.documentgenerator.domain.demand.FactTreeOperations;
import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.type.AlimentosDemandType;
import com.fidelg.documentgenerator.domain.type.DocumentType;
import com.fidelg.documentgenerator.domain.type.DocumentTypeRegistry;
import com.fidelg.documentgenerator.domain.type.SectionSpec;
import com.fidelg.documentgenerator.infrastructure.JsonDraftStore;
import com.fidelg.documentgenerator.infrastructure.WordDocumentGenerator;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flujo completo sin interfaz: datos estructurados → generación → edición →
 * reglas de sobrescritura → borrador JSON → ensamblado → DOCX legible.
 */
class DocumentFlowTest {

    @TempDir
    Path tempDir;

    private final DocumentType type = new AlimentosDemandType();
    private final ContentGenerationService generation = new ContentGenerationService();
    private final DemandDocumentAssembler assembler = new DemandDocumentAssembler();
    private DocumentTypeRegistry registry;
    private DemandDocument document;

    @BeforeEach
    void setUp() {
        registry = new DocumentTypeRegistry();
        registry.register(new AlimentosDemandType());
        document = new CreateDemandService(registry).execute(AlimentosDemandType.ID);
    }

    @Test
    void runsTheWholeFlowAndWritesALegibleDocx() throws Exception {
        fillStructuredData();
        generateInitialContent();
        editContentManually();
        renumberAfterStructureChanges();
        saveAndReloadDraft();
        List<DocumentBlock> blocks = assembler.assemble(document, type);
        verifyDerivedNumbering(blocks);
        Path output = verifyGeneratedDocx(blocks);
        assertFalse(Files.size(output) == 0);
    }

    private void fillStructuredData() {
        Section cabecera = document.section("cabecera");
        cabecera.setInput("expediente", "001-2026");
        cabecera.setInput("especialista", "ANA TORRES");
        cabecera.setInput("escrito", "PRIMERA");

        document.section("titulo").setInput("titulo", "DEMANDA DE ALIMENTOS");
        document.section("sumilla").setInput("sumilla", "Sumilla de la demanda de alimentos.");

        Section encabezamiento = document.section("encabezamiento");
        encabezamiento.setInput("juzgado", "DE LO CIVIL DE LIMA");
        encabezamiento.setInput("demandante", "MARÍA PÉREZ");
        encabezamiento.setInput("documento", "45678912");
        encabezamiento.setInput("domicilio", "Av. Los Olivos 123");
        encabezamiento.setInput("notificaciones", "Jr. Comercio 45");

        Section demandado = document.section("demandado");
        demandado.setInput("demandado", "JUAN PÉREZ");
        demandado.setInput("domicilioReal", "Av. Los Álamos 456");

        Section petitorio = document.section("petitorio");
        petitorio.setInput("proceso", "ordenaria");
        petitorio.setInput("tipoDemanda", "DEMANDA DE ALIMENTOS");
        petitorio.setInput("demandado", "JUAN PÉREZ");
        petitorio.setInput("monto", "S/ 1,500.00");
        petitorio.setInput("profesion", "COMERCIANTE");
        petitorio.setInput("alimentistas", "ANA PÉREZ\nLUIS PÉREZ");

        Section juridicos = document.section("juridicos");
        juridicos.getItems().add(new CollectionItem("Código Civil",
                "Artículo 476.- Contenido de la obligación"));

        Section medios = document.section("medios");
        medios.getItems().add(new CollectionItem(null, "Partida de nacimiento"));
        medios.getItems().add(new CollectionItem(null, "Constancia de trabajo"));

        Section otrosies = document.section("otrosies");
        otrosies.getItems().add(new CollectionItem("Nulidad", "Solicito la nulidad de lo actuado."));

        Section cierre = document.section("cierre");
        cierre.setInput("ciudad", "Morropón");
        cierre.setInput("fecha", "27 de diciembre del 2017");
        cierre.setInput("demandante", "MARÍA PÉREZ");
        cierre.setInput("documento", "45678912");
    }

    private void generateInitialContent() {
        for (SectionSpec spec : type.sections()) {
            if (spec.kind() == com.fidelg.documentgenerator.domain.demand.SectionKind.GENERATED_TEXT) {
                assertTrue(generation.regenerateIfClean(document.section(spec.id()), spec, type),
                        "debe generar contenido: " + spec.id());
            }
        }

        Section hechos = document.section("hechos");
        FactNode fact = FactTreeOperations.addFact(hechos, null, "hecho");
        fact.setInput("rotulo", "PRIMERO");
        fact.setInput("narrativa", "El demandado es padre de la menor.");
        assertTrue(generation.regenerateFactIfClean(fact, hechos, type.spec("hechos").factTemplate("hecho"), type));
        assertEquals("PRIMERO.- El demandado es padre de la menor.", fact.getContent());
        assertTrue(generation.regenerateClosingIfClean(hechos, type));

        assertFalse(document.section("encabezamiento").isDirty());
        assertTrue(document.section("encabezamiento").getContent().startsWith("SEÑORA JUEZ DEL"));
    }

    private void editContentManually() {
        Section petitorio = document.section("petitorio");
        petitorio.markContent("Petitorio reescrito por el usuario.");
        assertTrue(petitorio.isDirty());

        assertFalse(generation.regenerateIfClean(petitorio, type.spec("petitorio"), type));
        assertEquals("Petitorio reescrito por el usuario.", petitorio.getContent());

        generation.regenerateForced(petitorio, type.spec("petitorio"), type);
        assertFalse(petitorio.isDirty());
        assertTrue(petitorio.getContent().startsWith("Recurro a su despacho"));
    }

    private void renumberAfterStructureChanges() {
        Section hechos = document.section("hechos");
        FactNode second = FactTreeOperations.addFact(hechos, null, "hecho");
        second.applyGenerated("Segundo hecho.");
        assertTrue(generation.regenerateClosingIfClean(hechos, type));

        assertEquals("PRIMERO.- El demandado es padre de la menor.\nSegundo hecho.",
                hechos.getClosingContent());

        FactTreeOperations.remove(hechos, hechos.getFacts().get(0).getId());
        assertTrue(generation.regenerateClosingIfClean(hechos, type));
        assertEquals("Segundo hecho.", hechos.getClosingContent());
    }

    private void saveAndReloadDraft() throws Exception {
        DraftService drafts = new DraftService(new JsonDraftStore(), registry);
        Path file = tempDir.resolve("demanda.json");
        drafts.save(document, file);

        DemandDocument loaded = drafts.load(file);
        assertEquals(AlimentosDemandType.ID, loaded.getTypeId());
        assertEquals("001-2026", loaded.section("cabecera").input("expediente"));
        assertEquals("Segundo hecho.", loaded.section("hechos").getFacts().get(0).getContent());
        document = loaded;
    }

    private void verifyDerivedNumbering(List<DocumentBlock> blocks) {
        String text = blocks.stream().map(DocumentBlock::text).toList().toString();
        assertTrue(text.contains("EXPEDIENTE: 001-2026"));
        assertTrue(text.contains("I. DEMANDADO:"));
        assertTrue(text.contains("II. PETITORIO"));
        assertTrue(text.contains("III. FUNDAMENTOS FÁCTICOS:"));
        assertTrue(text.contains("VIII. MEDIOS PROBATORIOS Y ANEXOS:"));
        assertTrue(text.contains("3.1.- Segundo hecho."), text);
        assertTrue(text.contains("1-A. Partida de nacimiento"), text);
        assertTrue(text.contains("1-B. Constancia de trabajo"), text);
        assertTrue(text.contains("PRIMER OTROSÍ DIGO: Nulidad.- Solicito la nulidad de lo actuado."));
        assertTrue(text.contains("IV. FUNDAMENTOS JURÍDICOS:"));
        assertTrue(text.contains("Morropón, 27 de diciembre del 2017\nMARÍA PÉREZ\nDNI Nº 45678912"));
    }

    private Path verifyGeneratedDocx(List<DocumentBlock> blocks) throws Exception {
        Path output = tempDir.resolve("demanda.docx");
        new GenerateDocumentService(new WordDocumentGenerator()).execute(blocks, output);

        assertTrue(Files.exists(output));
        try (XWPFDocument docx = new XWPFDocument(Files.newInputStream(output))) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : docx.getParagraphs()) {
                text.append(paragraph.getText()).append('\n');
            }
            String all = text.toString();
            assertTrue(all.contains("II. PETITORIO"));
            assertTrue(all.contains("3.1.- Segundo hecho."));
            assertTrue(all.contains("1-A. Partida de nacimiento"));
        }
        return output;
    }
}
