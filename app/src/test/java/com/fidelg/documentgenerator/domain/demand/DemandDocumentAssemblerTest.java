package com.fidelg.documentgenerator.domain.demand;

import com.fidelg.documentgenerator.application.CreateDemandService;
import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.domain.DemandStyles;
import com.fidelg.documentgenerator.domain.type.AlimentosDemandType;
import com.fidelg.documentgenerator.domain.type.DocumentTypeRegistry;
import com.fidelg.documentgenerator.domain.type.SectionSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemandDocumentAssemblerTest {

    private final AlimentosDemandType type = new AlimentosDemandType();
    private final DemandDocumentAssembler assembler = new DemandDocumentAssembler();
    private DemandDocument document;

    @BeforeEach
    void setUp() {
        DocumentTypeRegistry registry = new DocumentTypeRegistry();
        registry.register(type);
        document = new CreateDemandService(registry).execute(AlimentosDemandType.ID);
    }

    @Test
    void producesNoBlocksForAnEmptyDocument() {
        assertTrue(assembler.assemble(document, type).isEmpty());
    }

    @Test
    void rendersTheHeaderWithItsIndent() {
        Section cabecera = document.section("cabecera");
        cabecera.setInput("expediente", "001-2026");
        cabecera.setInput("especialista", "ANA TORRES");
        cabecera.setInput("escrito", "PRIMERA");

        List<DocumentBlock> blocks = assembler.assemble(document, type);

        assertEquals(4, blocks.size());
        assertEquals("EXPEDIENTE: 001-2026", blocks.get(0).text());
        assertEquals("ESPECIALISTA LEGAL: ANA TORRES", blocks.get(1).text());
        assertEquals("ESCRITO: PRIMERA", blocks.get(2).text());
        assertEquals("Cuaderno Principal", blocks.get(3).text());
        assertEquals(DemandStyles.HEADER_INDENT, blocks.get(3).leftIndent());
    }

    @Test
    void numbersRomanSectionsByTheirPosition() {
        document.section("demandado").applyGenerated("Contenido del demandado.");
        document.section("petitorio").applyGenerated("Contenido del petitorio.");
        document.section("hechos").applyClosingGenerated("Hecho único.");

        List<DocumentBlock> blocks = assembler.assemble(document, type);

        assertEquals("I. DEMANDADO:", blocks.get(0).text());
        assertEquals("Contenido del demandado.", blocks.get(1).text());
        assertEquals("II. PETITORIO", blocks.get(2).text());
        assertEquals("III. FUNDAMENTOS FÁCTICOS:", blocks.get(4).text());
        assertEquals("Hecho único.", blocks.get(5).text());
    }

    @Test
    void skipsSectionsWithoutContentButKeepsTheRomanPosition() {
        document.section("demandado").applyGenerated("   ");
        document.section("petitorio").applyGenerated("Contenido del petitorio.");

        List<DocumentBlock> blocks = assembler.assemble(document, type);

        assertEquals("II. PETITORIO", blocks.get(0).text());
        assertEquals("Contenido del petitorio.", blocks.get(1).text());
        assertEquals(2, blocks.size());
    }

    @Test
    void numbersFactsRelativeToTheirSectionIncludingSubFacts() {
        Section hechos = document.section("hechos");
        FactNode first = FactTreeOperations.addFact(hechos, null, "hecho");
        first.applyGenerated("Primer hecho.");
        FactNode second = FactTreeOperations.addFact(hechos, null, "hecho");
        second.applyGenerated("Segundo hecho.");
        FactNode child = FactTreeOperations.addFact(hechos, second.getId(), "hecho");
        child.applyGenerated("Subhecho.");
        hechos.applyClosingGenerated("Cierre de los hechos.");

        List<DocumentBlock> blocks = assembler.assemble(document, type);

        assertEquals("III. FUNDAMENTOS FÁCTICOS:", blocks.get(0).text());
        assertEquals("3.1.- Primer hecho.", blocks.get(1).text());
        assertEquals("3.2.- Segundo hecho.", blocks.get(2).text());
        assertEquals("3.2.1.- Subhecho.", blocks.get(3).text());
        assertEquals("Cierre de los hechos.", blocks.get(4).text());
        assertEquals(DemandStyles.factIndent(0), blocks.get(1).leftIndent());
        assertEquals(DemandStyles.factIndent(1), blocks.get(3).leftIndent());
    }

    @Test
    void rendersSubtitlesBoldAndWithoutANumber() {
        Section hechos = document.section("hechos");
        FactNode subtitle = FactTreeOperations.addSubtitle(hechos, null, "Necesidades de la menor");
        subtitle.applyGenerated("Necesidades de la menor");
        FactNode fact = FactTreeOperations.addFact(hechos, null, "hecho");
        fact.applyGenerated("Hecho numerado.");

        List<DocumentBlock> blocks = assembler.assemble(document, type);

        assertEquals("III. FUNDAMENTOS FÁCTICOS:", blocks.get(0).text());
        assertEquals("Necesidades de la menor:", blocks.get(1).text());
        assertEquals("3.1.- Hecho numerado.", blocks.get(2).text());
        assertTrue(blocks.get(1).bold());
    }

    @Test
    void labelsEvidenceItemsWithDerivedLetters() {
        Section medios = document.section("medios");
        medios.getItems().add(new CollectionItem(null, "Partida de nacimiento"));
        medios.getItems().add(new CollectionItem(null, "Constancia de trabajo"));

        List<DocumentBlock> blocks = assembler.assemble(document, type);

        assertEquals("VIII. MEDIOS PROBATORIOS Y ANEXOS:", blocks.get(0).text());
        assertEquals("1-A. Partida de nacimiento", blocks.get(1).text());
        assertEquals("1-B. Constancia de trabajo", blocks.get(2).text());
    }

    @Test
    void labelsOthersiesWithDerivedOrdinals() {
        Section otrosies = document.section("otrosies");
        otrosies.getItems().add(new CollectionItem("Nulidad", "Solicito la nulidad de lo actuado."));
        otrosies.getItems().add(new CollectionItem(null, "Segundo otrosí sin título."));

        List<DocumentBlock> blocks = assembler.assemble(document, type);

        assertEquals("PRIMER OTROSÍ DIGO: Nulidad.- Solicito la nulidad de lo actuado.",
                blocks.get(0).text());
        assertEquals("SEGUNDO OTROSÍ DIGO: Segundo otrosí sin título.", blocks.get(1).text());
    }

    @Test
    void rendersLegalGroupsWithArticleIndent() {
        Section juridicos = document.section("juridicos");
        juridicos.applyGenerated("❖ Artículo 476.- Alimentos.");

        List<DocumentBlock> blocks = assembler.assemble(document, type);

        assertEquals("IV. FUNDAMENTOS JURÍDICOS:", blocks.get(0).text());
        assertEquals("❖ Artículo 476.- Alimentos.", blocks.get(1).text());
        assertEquals(DemandStyles.ARTICLE_INDENT, blocks.get(1).leftIndent());
    }

    @Test
    void rendersTheEditableContentOfTextSectionsAfterTheirTitle() {
        document.section("representacion").markContent("Yo, MARÍA PÉREZ, en mi calidad de madre.");
        document.section("monto").markContent("S/ 1,500.00 mensuales.");

        List<DocumentBlock> blocks = assembler.assemble(document, type);

        assertEquals("V. REPRESENTACIÓN PROCESAL, LEGITIMIDAD E INTERÉS PARA OBRAR:", blocks.get(0).text());
        assertEquals("Yo, MARÍA PÉREZ, en mi calidad de madre.", blocks.get(1).text());
        assertEquals("VI. MONTO DEL PETITORIO:", blocks.get(2).text());
        assertEquals("S/ 1,500.00 mensuales.", blocks.get(3).text());
    }

    @Test
    void keepsSectionTitlesBoldAndJustified() {
        document.section("demandado").applyGenerated("Contenido.");

        DocumentBlock title = assembler.assemble(document, type).get(0);

        assertTrue(title.bold());
        assertEquals(DocumentBlock.Alignment.JUSTIFY, title.alignment());
        assertEquals(DemandStyles.FONT, title.fontName());
        assertEquals(DemandStyles.BODY_FONT_SIZE, title.fontSize());
    }

    @Test
    void exposesTheTypeIdentityAndSpecDetails() {
        SectionSpec spec = type.spec("otrosies");
        assertEquals(0, spec.inputs().size());
        assertEquals("Otrosíes", spec.title());
        assertEquals("Demanda de Alimentos", type.displayName());
        assertEquals(AlimentosDemandType.ID, type.id());
    }
}
