package com.fidelg.documentgenerator.infrastructure;

import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.domain.DocumentBlocks;
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
            for (DocumentBlock block : DocumentBlocks.from(request.title(), request.author(), request.body())) {
                addBlock(document, block);
            }

            try (OutputStream out = Files.newOutputStream(outputFile)) {
                document.write(out);
            }
        }
    }

    private void addBlock(XWPFDocument document, DocumentBlock block) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(block.alignment() == DocumentBlock.Alignment.CENTER
                ? ParagraphAlignment.CENTER
                : ParagraphAlignment.LEFT);

        String[] lines = block.text().split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                paragraph.createRun().addBreak();
            }
            XWPFRun run = paragraph.createRun();
            run.setBold(block.bold());
            run.setItalic(block.italic());
            run.setFontSize(block.fontSize());
            run.setText(lines[i]);
        }
    }
}
