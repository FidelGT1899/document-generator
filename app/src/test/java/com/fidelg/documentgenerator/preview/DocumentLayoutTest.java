package com.fidelg.documentgenerator.preview;

import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.domain.DocumentBlocks;
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
    void keepsTheDocumentStructureWhenTheFormIsEmpty() {
        DocumentLayout layout = DocumentLayout.of(DocumentBlocks.from("", "", ""));

        List<DocumentLayout.Line> lines = layout.pages().get(0).lines();
        assertEquals(3, lines.size());
        assertTrue(lines.stream().allMatch(line -> line.text().isEmpty()));
    }

    @Test
    void wrapsLongParagraphIntoSeveralLinesWithoutLosingWords() {
        String body = "palabra ".repeat(300).trim();

        DocumentLayout layout = DocumentLayout.of(DocumentBlocks.from("Título", "Autor", body));

        List<String> texts = allLines(layout).stream().map(DocumentLayout.Line::text).filter(text -> !text.isBlank()).toList();
        assertTrue(texts.size() > 3);
        long words = texts.stream().mapToLong(text -> text.trim().split("\\s+").length).sum();
        assertEquals(302, words);
    }

    @Test
    void startsNewPageWhenContentDoesNotFit() {
        String body = "linea de texto ".repeat(600).trim();

        DocumentLayout layout = DocumentLayout.of(DocumentBlocks.from("Título", "Autor", body));

        assertTrue(layout.pages().size() >= 2);
        for (int i = 0; i < layout.pages().size() - 1; i++) {
            assertFalse(layout.pages().get(i).lines().isEmpty());
        }
    }

    @Test
    void keepsEveryLineInsideThePageMargins() {
        String body = "contenido de prueba ".repeat(400).trim();

        DocumentLayout layout = DocumentLayout.of(DocumentBlocks.from("Título", "Autor", body));

        for (DocumentLayout.Page page : layout.pages()) {
            for (DocumentLayout.Line line : page.lines()) {
                assertTrue(line.x() >= DocumentLayout.MARGIN);
                assertTrue(line.baselineY() <= DocumentLayout.PAGE_HEIGHT - DocumentLayout.MARGIN);
            }
        }
    }

    @Test
    void centersTitleAndKeepsBodyLeftAligned() {
        DocumentLayout layout = DocumentLayout.of(DocumentBlocks.from("T", "A", "Cuerpo"));

        List<DocumentLayout.Line> lines = layout.pages().get(0).lines();
        assertTrue(lines.get(0).x() > DocumentLayout.MARGIN);
        assertTrue(lines.get(1).x() > DocumentLayout.MARGIN);
        assertEquals(DocumentLayout.MARGIN, lines.get(2).x());
    }

    @Test
    void keepsExplicitLineBreaksOfTheBody() {
        DocumentLayout layout = DocumentLayout.of(DocumentBlocks.from("T", "A", "primera\nsegunda"));

        List<DocumentLayout.Line> lines = layout.pages().get(0).lines();
        assertEquals(4, lines.size());
        assertEquals("primera", lines.get(2).text());
        assertEquals("segunda", lines.get(3).text());
    }

    private List<DocumentLayout.Line> allLines(DocumentLayout layout) {
        return layout.pages().stream().flatMap(page -> page.lines().stream()).toList();
    }
}
