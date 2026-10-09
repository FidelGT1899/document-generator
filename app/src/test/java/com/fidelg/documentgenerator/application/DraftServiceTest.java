package com.fidelg.documentgenerator.application;

import com.fidelg.documentgenerator.domain.demand.DemandDocument;
import com.fidelg.documentgenerator.domain.demand.FactNode;
import com.fidelg.documentgenerator.domain.demand.FactTreeOperations;
import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.type.AlimentosDemandType;
import com.fidelg.documentgenerator.domain.type.DocumentTypeRegistry;
import com.fidelg.documentgenerator.infrastructure.JsonDraftStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DraftServiceTest {

    @TempDir
    Path tempDir;

    private DocumentTypeRegistry registry;
    private DraftService service;
    private CreateDemandService createService;

    @BeforeEach
    void setUp() {
        registry = new DocumentTypeRegistry();
        registry.register(new AlimentosDemandType());
        service = new DraftService(new JsonDraftStore(), registry);
        createService = new CreateDemandService(registry);
    }

    @Test
    void roundTripPreservesInputsContentAndDirtyState() throws Exception {
        DemandDocument document = createService.execute(AlimentosDemandType.ID);

        Section cabecera = document.section("cabecera");
        cabecera.setInput("expediente", "001-2026");

        Section petitorio = document.section("petitorio");
        petitorio.setInput("proceso", "ordenaria");
        petitorio.applyGenerated("Generado original.");
        petitorio.markContent("Editado por el usuario.");

        Section hechos = document.section("hechos");
        FactNode fact = FactTreeOperations.addFact(hechos, null, "hecho");
        fact.setInput("rotulo", "PRIMERO");
        fact.applyGenerated("Hecho generado.");
        fact.markContent("Hecho editado.");

        Section medios = document.section("medios");
        medios.getItems().add(new com.fidelg.documentgenerator.domain.demand.CollectionItem(
                "Título", "Contenido del anexo"));

        Path file = tempDir.resolve("borrador.json");
        service.save(document, file);

        DemandDocument loaded = service.load(file);

        assertEquals(AlimentosDemandType.ID, loaded.getTypeId());
        assertEquals(document.getSections().size(), loaded.getSections().size());
        assertEquals("001-2026", loaded.section("cabecera").input("expediente"));
        assertEquals("ordenaria", loaded.section("petitorio").input("proceso"));
        assertEquals("Editado por el usuario.", loaded.section("petitorio").getContent());
        assertTrue(loaded.section("petitorio").isDirty());

        Section loadedHechos = loaded.section("hechos");
        assertEquals(1, loadedHechos.getFacts().size());
        FactNode loadedFact = loadedHechos.getFacts().get(0);
        assertEquals("PRIMERO", loadedFact.getInputs().get("rotulo"));
        assertEquals("Hecho editado.", loadedFact.getContent());
        assertTrue(loadedFact.isDirty());

        Section loadedMedios = loaded.section("medios");
        assertEquals(1, loadedMedios.getItems().size());
        assertEquals("Título", loadedMedios.getItems().get(0).getTitle());
        assertEquals("Contenido del anexo", loadedMedios.getItems().get(0).getContent());
    }

    @Test
    void roundTripPreservesCleanGeneratedContent() throws Exception {
        DemandDocument document = createService.execute(AlimentosDemandType.ID);
        document.section("sumilla").applyGenerated("Sumilla original.");
        document.section("sumilla").setInput("sumilla", "Sumilla original.");

        Path file = tempDir.resolve("borrador.json");
        service.save(document, file);
        DemandDocument loaded = service.load(file);

        Section sumilla = loaded.section("sumilla");
        assertEquals("Sumilla original.", sumilla.getContent());
        assertFalse(sumilla.isDirty());
    }

    @Test
    void rejectsADraftThatDoesNotMatchTheTypeStructure() throws Exception {
        DemandDocument broken = new DemandDocument(AlimentosDemandType.ID);
        Path file = tempDir.resolve("incompleto.json");
        service.save(broken, file);

        assertThrows(IOException.class, () -> service.load(file));
    }

    @Test
    void rejectsADraftOfAnUnknownDocumentType() throws Exception {
        DemandDocument unknown = new DemandDocument("INEXISTENTE");
        Path file = tempDir.resolve("desconocido.json");
        service.save(unknown, file);

        assertThrows(IllegalArgumentException.class, () -> service.load(file));
    }
}
