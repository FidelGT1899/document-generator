package com.fidelg.documentgenerator.preview;

import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.domain.DocumentRun;
import com.fidelg.documentgenerator.domain.DemandStyles;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentLayoutTest {

    @Test
    void keepsASingleEmptyPageWhenThereAreNoBlocks() {
        DocumentLayout layout = DocumentLayout.of(List.of());

        assertEquals(1, layout.pages().size());
        assertTrue(layout.pages().get(0).lines().isEmpty());
    }

    @Test
    void keepsOneLinePerEmptyBlock() {
        DocumentLayout layout = DocumentLayout.of(List.of(DocumentBlock.plain(""),
                DocumentBlock.plain("")));

        List<DocumentLayout.Line> lines = layout.pages().get(0).lines();
        assertEquals(2, lines.size());
        assertTrue(lines.stream().allMatch(line -> line.text().isEmpty()));
    }

    @Test
    void wrapsLongParagraphIntoSeveralLinesWithoutLosingWords() {
        String body = "palabra ".repeat(300).trim();

        DocumentLayout layout = DocumentLayout.of(List.of(DocumentBlock.plain(body)));

        List<String> texts = allLines(layout).stream()
                .map(DocumentLayout.Line::text).filter(text -> !text.isBlank()).toList();
        assertTrue(texts.size() > 3);
        long words = texts.stream().mapToLong(text -> text.trim().split("\\s+").length).sum();
        assertEquals(300, words);
    }

    @Test
    void startsNewPageWhenContentDoesNotFit() {
        String body = "linea de texto ".repeat(600).trim();

        DocumentLayout layout = DocumentLayout.of(List.of(DocumentBlock.plain(body)));

        assertTrue(layout.pages().size() >= 2);
        for (int i = 0; i < layout.pages().size() - 1; i++) {
            assertFalse(layout.pages().get(i).lines().isEmpty());
        }
    }

    @Test
    void keepsEveryLineInsideThePageMargins() {
        String body = "contenido de prueba ".repeat(400).trim();

        DocumentLayout layout = DocumentLayout.of(List.of(DocumentBlock.plain(body)));

        for (DocumentLayout.Page page : layout.pages()) {
            for (DocumentLayout.Line line : page.lines()) {
                assertTrue(line.x() >= DocumentLayout.MARGIN);
                assertTrue(line.baselineY() <= DocumentLayout.PAGE_HEIGHT - DocumentLayout.MARGIN);
            }
        }
    }

    @Test
    void centersCenteredBlocksAndKeepsLeftAlignedOnesAtTheMargin() {
        DocumentLayout layout = DocumentLayout.of(List.of(
                DocumentBlock.plain("Cuerpo", DocumentBlock.Alignment.CENTER, DemandStyles.FONT),
                DocumentBlock.plain("Cuerpo", DocumentBlock.Alignment.LEFT, DemandStyles.FONT)));

        List<DocumentLayout.Line> lines = layout.pages().get(0).lines();
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).x() > DocumentLayout.MARGIN);
        assertEquals(DocumentLayout.MARGIN, lines.get(1).x());
    }

    @Test
    void keepsExplicitLineBreaksOfTheContent() {
        DocumentLayout layout = DocumentLayout.of(List.of(
                DocumentBlock.plain("primera\nsegunda", DocumentBlock.Alignment.LEFT, DemandStyles.FONT)));

        List<DocumentLayout.Line> lines = layout.pages().get(0).lines();
        assertEquals(2, lines.size());
        assertEquals("primera", lines.get(0).text());
        assertEquals("segunda", lines.get(1).text());
    }

    @Test
    void appliesLeftIndentMeasuredInTwips() {
        DocumentBlock block = new DocumentBlock(List.of(DocumentRun.plain("Sangrado")),
                DemandStyles.BODY_FONT_SIZE, DocumentBlock.Alignment.LEFT,
                DemandStyles.FONT, 1440, 0);

        DocumentLayout layout = DocumentLayout.of(List.of(block));

        assertEquals(DocumentLayout.MARGIN + 96, layout.pages().get(0).lines().get(0).x());
    }

    @Test
    void keepsMixedStyleRunsInASingleLine() {
        DocumentBlock block = new DocumentBlock(
                List.of(DocumentRun.bold("3.1.- "), DocumentRun.plain("el hecho")),
                DemandStyles.BODY_FONT_SIZE, DocumentBlock.Alignment.LEFT,
                DemandStyles.FONT, 0, 0);

        DocumentLayout layout = DocumentLayout.of(List.of(block));
        DocumentLayout.Line line = layout.pages().get(0).lines().get(0);

        assertEquals("3.1.- el hecho", line.text());
        assertTrue(line.segments().get(0).font().isBold());
        assertFalse(line.segments().get(line.segments().size() - 1).font().isBold());
    }

    private List<DocumentLayout.Line> allLines(DocumentLayout layout) {
        return layout.pages().stream().flatMap(page -> page.lines().stream()).toList();
    }
}
