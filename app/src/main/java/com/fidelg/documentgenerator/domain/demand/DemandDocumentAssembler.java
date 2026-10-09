package com.fidelg.documentgenerator.domain.demand;

import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.domain.DocumentRun;
import com.fidelg.documentgenerator.domain.DemandStyles;
import com.fidelg.documentgenerator.domain.type.CollectionFormat;
import com.fidelg.documentgenerator.domain.type.DocumentType;
import com.fidelg.documentgenerator.domain.type.SectionSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Construye el documento final (lista de párrafos) a partir del modelo de la
 * demanda. Respeta el orden de las secciones, la jerarquía de hechos y toda la
 * numeración derivada. Usa siempre el contenido final editable: nunca vuelve a
 * generar textos desde los inputs.
 */
public final class DemandDocumentAssembler {

    public List<DocumentBlock> assemble(DemandDocument document, DocumentType type) {
        List<DocumentBlock> blocks = new ArrayList<>();
        int romanCounter = 0;
        for (SectionSpec spec : type.sections()) {
            Section section = document.section(spec.id());
            int sectionNumber = 0;
            if (spec.romanNumbered()) {
                romanCounter++;
                sectionNumber = romanCounter;
            }
            renderSection(blocks, spec, section, sectionNumber);
        }
        return blocks;
    }

    private void renderSection(List<DocumentBlock> blocks, SectionSpec spec, Section section, int sectionNumber) {
        switch (spec.kind()) {
            case SIMPLE_INPUT -> renderCabecera(blocks, section);
            case GENERATED_TEXT, EDITABLE_TEXT -> renderTextSection(blocks, spec, section, sectionNumber);
            case COLLECTION -> renderCollectionSection(blocks, spec, section, sectionNumber);
            case FACTS -> renderFactsSection(blocks, spec, section, sectionNumber);
        }
    }

    private void renderCabecera(List<DocumentBlock> blocks, Section section) {
        if (allBlank(section.getInputs())) {
            return;
        }
        blocks.add(headerLine("EXPEDIENTE: " + section.input("expediente")));
        blocks.add(headerLine("ESPECIALISTA LEGAL: " + section.input("especialista")));
        blocks.add(headerLine("ESCRITO: " + section.input("escrito")));
        blocks.add(block("Cuaderno Principal", false, false, DemandStyles.HEADER_INDENT, 0));
    }

    private void renderTextSection(List<DocumentBlock> blocks, SectionSpec spec, Section section, int sectionNumber) {
        if (section.getContent().isBlank()) {
            return;
        }
        addSectionTitle(blocks, spec, sectionNumber);
        blocks.add(contentBlock(section.getContent(), 0, DemandStyles.SECTION_SPACE_AFTER));
    }

    private void renderCollectionSection(List<DocumentBlock> blocks, SectionSpec spec, Section section, int sectionNumber) {
        boolean hasItems = section.getItems().stream().anyMatch(this::hasRenderableItem);
        boolean hasContent = spec.hasSectionContent() && !section.getContent().isBlank();
        if (!hasItems && !hasContent) {
            return;
        }
        addSectionTitle(blocks, spec, sectionNumber);
        if (spec.collectionFormat() == CollectionFormat.LEGAL_GROUP) {
            if (hasContent) {
                blocks.add(contentBlock(section.getContent(), DemandStyles.ARTICLE_INDENT,
                        DemandStyles.SECTION_SPACE_AFTER));
            }
            return;
        }
        int index = 0;
        for (CollectionItem item : section.getItems()) {
            if (!hasRenderableItem(item)) {
                continue;
            }
            if (spec.collectionFormat() == CollectionFormat.EVIDENCE) {
                blocks.add(evidenceBlock(item, index));
            } else {
                blocks.add(otrosiBlock(item, index));
            }
            index++;
        }
        addSpace(blocks, DemandStyles.SECTION_SPACE_AFTER);
    }

    private void renderFactsSection(List<DocumentBlock> blocks, SectionSpec spec, Section section, int sectionNumber) {
        boolean hasContent = !section.getFacts().isEmpty() || !section.getClosingContent().isBlank();
        if (!hasContent) {
            return;
        }
        addSectionTitle(blocks, spec, sectionNumber);
        Map<String, String> numbers = DemandNumbering.factNumbers(section.getFacts());
        renderFacts(blocks, section.getFacts(), numbers,
                String.valueOf(sectionNumber), 0);
        if (!section.getClosingContent().isBlank()) {
            blocks.add(contentBlock(section.getClosingContent(), 0, DemandStyles.SECTION_SPACE_AFTER));
        }
    }

    private void renderFacts(List<DocumentBlock> blocks, List<FactNode> nodes, Map<String, String> numbers,
                             String sectionPrefix, int depth) {
        for (FactNode node : nodes) {
            if (node.getKind() == FactNodeKind.SUBTITLE) {
                if (!node.getContent().isBlank()) {
                    blocks.add(subtitleBlock(node.getContent(), depth));
                }
                continue;
            }
            String number = numbers.get(node.getId());
            if (number == null) {
                continue;
            }
            if (!node.getContent().isBlank()) {
                blocks.add(factBlock(sectionPrefix + "." + number, node, depth));
            }
            renderFacts(blocks, node.getChildren(), numbers, sectionPrefix, depth + 1);
        }
    }

    private void addSectionTitle(List<DocumentBlock> blocks, SectionSpec spec, int sectionNumber) {
        if (!spec.renderTitle()) {
            return;
        }
        String prefix = spec.romanNumbered()
                ? DemandNumbering.roman(sectionNumber) + ". "
                : "";
        blocks.add(new DocumentBlock(
                List.of(DocumentRun.bold(prefix + spec.title())),
                DemandStyles.BODY_FONT_SIZE,
                DocumentBlock.Alignment.JUSTIFY,
                DemandStyles.FONT,
                0,
                DemandStyles.TITLE_SPACE_AFTER));
    }

    private DocumentBlock factBlock(String number, FactNode node, int depth) {
        List<DocumentRun> runs = new ArrayList<>();
        runs.add(DocumentRun.bold(number + ".- "));
        String label = node.getInputs().getOrDefault("rotulo", "").trim();
        String content = node.getContent();
        String marker = label + ".- ";
        if (!label.isEmpty() && content.startsWith(marker)) {
            runs.add(DocumentRun.bold(marker));
            runs.add(DocumentRun.plain(content.substring(marker.length())));
        } else if (!label.isEmpty() && content.equals(label)) {
            runs.add(DocumentRun.bold(label));
        } else {
            runs.add(DocumentRun.plain(content));
        }
        return new DocumentBlock(runs, DemandStyles.BODY_FONT_SIZE, DocumentBlock.Alignment.JUSTIFY,
                DemandStyles.FONT, DemandStyles.factIndent(depth), DemandStyles.PARAGRAPH_SPACE_AFTER);
    }

    private DocumentBlock subtitleBlock(String text, int depth) {
        String label = text.trim();
        if (!label.endsWith(":")) {
            label += ":";
        }
        return new DocumentBlock(List.of(DocumentRun.bold(label)), DemandStyles.BODY_FONT_SIZE,
                DocumentBlock.Alignment.JUSTIFY, DemandStyles.FONT,
                DemandStyles.factIndent(depth), DemandStyles.PARAGRAPH_SPACE_AFTER);
    }

    private DocumentBlock evidenceBlock(CollectionItem item, int index) {
        return new DocumentBlock(
                List.of(DocumentRun.bold(DemandNumbering.evidenceLabel(index) + ". "),
                        DocumentRun.plain(item.getContent().trim())),
                DemandStyles.BODY_FONT_SIZE, DocumentBlock.Alignment.JUSTIFY, DemandStyles.FONT,
                DemandStyles.FACT_INDENT, DemandStyles.PARAGRAPH_SPACE_AFTER);
    }

    private DocumentBlock otrosiBlock(CollectionItem item, int index) {
        String ordinal = DemandNumbering.otrosiOrdinal(index) + " OTROSÍ DIGO:";
        List<DocumentRun> runs = new ArrayList<>();
        runs.add(DocumentRun.bold(ordinal + " "));
        String title = item.getTitle() == null ? "" : item.getTitle().trim();
        if (!title.isEmpty()) {
            runs.add(DocumentRun.bold(title + ".- "));
        }
        runs.add(DocumentRun.plain(item.getContent().trim()));
        return new DocumentBlock(runs, DemandStyles.BODY_FONT_SIZE, DocumentBlock.Alignment.JUSTIFY,
                DemandStyles.FONT, 0, DemandStyles.SECTION_SPACE_AFTER);
    }

    private DocumentBlock contentBlock(String content, int indent, int spaceAfter) {
        return new DocumentBlock(List.of(DocumentRun.plain(content)), DemandStyles.BODY_FONT_SIZE,
                DocumentBlock.Alignment.JUSTIFY, DemandStyles.FONT, indent, spaceAfter);
    }

    private DocumentBlock headerLine(String text) {
        return block(text, true, false, DemandStyles.HEADER_INDENT, 0);
    }

    private DocumentBlock block(String text, boolean bold, boolean underline, int indent, int spaceAfter) {
        return new DocumentBlock(List.of(new DocumentRun(text, bold, false, underline)),
                DemandStyles.BODY_FONT_SIZE, DocumentBlock.Alignment.JUSTIFY, DemandStyles.FONT,
                indent, spaceAfter);
    }

    private void addSpace(List<DocumentBlock> blocks, int spaceAfter) {
        if (!blocks.isEmpty()) {
            DocumentBlock last = blocks.get(blocks.size() - 1);
            blocks.set(blocks.size() - 1, new DocumentBlock(last.runs(), last.fontSize(), last.alignment(),
                    last.fontName(), last.leftIndent(), spaceAfter));
        }
    }

    private boolean hasRenderableItem(CollectionItem item) {
        boolean hasTitle = item.getTitle() != null && !item.getTitle().isBlank();
        return hasTitle || !item.getContent().isBlank();
    }

    private boolean allBlank(Map<String, String> inputs) {
        return inputs.values().stream().allMatch(value -> value == null || value.isBlank());
    }
}
