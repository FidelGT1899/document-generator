package com.fidelg.documentgenerator.infrastructure;

import com.fidelg.documentgenerator.domain.DocumentRequest;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
}