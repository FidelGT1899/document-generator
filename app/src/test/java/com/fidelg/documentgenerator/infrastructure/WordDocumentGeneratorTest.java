package com.fidelg.documentgenerator.infrastructure;

import com.fidelg.documentgenerator.domain.DocumentRequest;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordDocumentGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesDocxWithRequestContent() throws Exception {
        Path output = tempDir.resolve("documento.docx");

        new WordDocumentGenerator().generate(
                new DocumentRequest("Título de prueba", "Autor de prueba", "Cuerpo del documento."),
                output);

        assertTrue(Files.exists(output));
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(output))) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            String text = paragraphs.stream()
                    .map(XWPFParagraph::getText)
                    .reduce("", (a, b) -> a + b);
            assertTrue(text.contains("Título de prueba"));
            assertTrue(text.contains("Autor de prueba"));
            assertTrue(text.contains("Cuerpo del documento."));
        }
    }

    @Test
    void appliesTheBlockStyles() throws Exception {
        Path output = tempDir.resolve("documento.docx");

        new WordDocumentGenerator().generate(
                new DocumentRequest("Título de prueba", "Autor de prueba", "Cuerpo del documento."),
                output);

        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(output))) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            assertEquals(3, paragraphs.size());

            XWPFRun title = paragraphs.get(0).getRuns().get(0);
            assertTrue(title.isBold());
            assertFalse(title.isItalic());
            assertEquals(22.0, title.getFontSizeAsDouble(), 0.001);
            assertEquals(ParagraphAlignment.CENTER, paragraphs.get(0).getAlignment());

            XWPFRun author = paragraphs.get(1).getRuns().get(0);
            assertFalse(author.isBold());
            assertTrue(author.isItalic());
            assertEquals(12.0, author.getFontSizeAsDouble(), 0.001);
            assertEquals(ParagraphAlignment.CENTER, paragraphs.get(1).getAlignment());

            XWPFRun body = paragraphs.get(2).getRuns().get(0);
            assertFalse(body.isBold());
            assertFalse(body.isItalic());
            assertEquals(12.0, body.getFontSizeAsDouble(), 0.001);
            assertEquals(ParagraphAlignment.LEFT, paragraphs.get(2).getAlignment());
        }
    }

    @Test
    void keepsExplicitLineBreaksOfTheBody() throws Exception {
        Path output = tempDir.resolve("documento.docx");

        new WordDocumentGenerator().generate(
                new DocumentRequest("Título", "Autor", "primera línea\nsegunda línea"),
                output);

        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(output))) {
            String text = document.getParagraphs().get(2).getText();
            assertTrue(text.contains("primera línea"));
            assertTrue(text.contains("segunda línea"));
        }
    }
}