package com.fidelg.documentgenerator.infrastructure;

import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.domain.DocumentRun;
import com.fidelg.documentgenerator.domain.DemandStyles;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
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
    void generatesDocxWithBlockContent() throws Exception {
        Path output = tempDir.resolve("documento.docx");

        new WordDocumentGenerator().generate(List.of(
                DocumentBlock.plain("Título de prueba", DocumentBlock.Alignment.CENTER, DemandStyles.FONT),
                DocumentBlock.plain("Cuerpo del documento.")), output);

        assertTrue(Files.exists(output));
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(output))) {
            String text = document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .reduce("", (a, b) -> a + b);
            assertTrue(text.contains("Título de prueba"));
            assertTrue(text.contains("Cuerpo del documento."));
        }
    }

    @Test
    void appliesRunAndParagraphStyles() throws Exception {
        Path output = tempDir.resolve("documento.docx");

        new WordDocumentGenerator().generate(List.of(
                new DocumentBlock(List.of(DocumentRun.bold("TÍTULO")), 22,
                        DocumentBlock.Alignment.CENTER, DemandStyles.FONT,
                        DemandStyles.HEADER_INDENT, 6),
                new DocumentBlock(List.of(new DocumentRun("Cuerpo", false, true, false)), 12,
                        DocumentBlock.Alignment.JUSTIFY, DemandStyles.FONT, 0, 4)), output);

        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(output))) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            assertEquals(2, paragraphs.size());

            XWPFParagraph title = paragraphs.get(0);
            XWPFRun titleRun = title.getRuns().get(0);
            assertTrue(titleRun.isBold());
            assertFalse(titleRun.isItalic());
            assertEquals(22.0, titleRun.getFontSizeAsDouble(), 0.001);
            assertEquals(DemandStyles.FONT, titleRun.getFontFamily());
            assertEquals(ParagraphAlignment.CENTER, title.getAlignment());
            assertEquals(DemandStyles.HEADER_INDENT, title.getIndentationLeft());
            assertEquals(6 * 20, title.getSpacingAfter());

            XWPFParagraph body = paragraphs.get(1);
            XWPFRun bodyRun = body.getRuns().get(0);
            assertFalse(bodyRun.isBold());
            assertTrue(bodyRun.isItalic());
            assertEquals(ParagraphAlignment.BOTH, body.getAlignment());
            assertEquals(4 * 20, body.getSpacingAfter());
        }
    }

    @Test
    void rendersUnderlinedRun() throws Exception {
        Path output = tempDir.resolve("documento.docx");

        new WordDocumentGenerator().generate(List.of(
                new DocumentBlock(List.of(DocumentRun.boldUnderline("SUBLÍNEADO"), DocumentRun.plain(" normal")),
                        12, DocumentBlock.Alignment.LEFT, DemandStyles.FONT, 0, 0)), output);

        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(output))) {
            XWPFParagraph paragraph = document.getParagraphs().get(0);
            assertEquals(UnderlinePatterns.SINGLE, paragraph.getRuns().get(0).getUnderline());
            assertEquals(UnderlinePatterns.NONE, paragraph.getRuns().get(1).getUnderline());
        }
    }

    @Test
    void keepsExplicitLineBreaksOfTheContent() throws Exception {
        Path output = tempDir.resolve("documento.docx");

        new WordDocumentGenerator().generate(List.of(
                DocumentBlock.plain("primera línea\nsegunda línea")), output);

        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(output))) {
            XWPFParagraph paragraph = document.getParagraphs().get(0);
            assertEquals(2, paragraph.getRuns().size());
            String text = paragraph.getText();
            assertTrue(text.contains("primera línea"));
            assertTrue(text.contains("segunda línea"));
        }
    }
}
