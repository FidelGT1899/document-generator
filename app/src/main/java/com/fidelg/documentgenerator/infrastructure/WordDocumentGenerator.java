package com.fidelg.documentgenerator.infrastructure;

import com.fidelg.documentgenerator.domain.DocumentRequest;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class WordDocumentGenerator {

    public void generate(DocumentRequest request, Path outputFile) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            addTitle(document, request.title());
            addAuthor(document, request.author());
            addBody(document, request.body());

            try (OutputStream out = Files.newOutputStream(outputFile)) {
                document.write(out);
            }
        }
    }

    private void addTitle(XWPFDocument document, String title) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setText(title);
        run.setBold(true);
        run.setFontSize(22);
    }

    private void addAuthor(XWPFDocument document, String author) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setText(author);
        run.setItalic(true);
        run.setFontSize(12);
    }

    private void addBody(XWPFDocument document, String body) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(body);
        run.setFontSize(12);
    }
}