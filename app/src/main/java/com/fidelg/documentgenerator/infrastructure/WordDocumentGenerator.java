package com.fidelg.documentgenerator.infrastructure;

import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.domain.DocumentRun;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Escribe el documento final en formato .docx. Consume la lista de párrafos ya
 * ensamblada: no conoce el modelo de la demanda ni vuelve a generar textos.
 */
public class WordDocumentGenerator {

    public void generate(List<DocumentBlock> blocks, Path outputFile) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            for (DocumentBlock block : blocks) {
                addBlock(document, block);
            }

            try (OutputStream out = Files.newOutputStream(outputFile)) {
                document.write(out);
            }
        }
    }

    private void addBlock(XWPFDocument document, DocumentBlock block) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignmentOf(block.alignment()));
        if (block.leftIndent() > 0) {
            paragraph.setIndentationLeft(block.leftIndent());
        }
        if (block.spaceAfter() > 0) {
            paragraph.setSpacingAfter(block.spaceAfter() * 20);
        }

        for (DocumentRun run : block.runs()) {
            addRun(paragraph, run, block);
        }
    }

    private void addRun(XWPFParagraph paragraph, DocumentRun run, DocumentBlock block) {
        XWPFRun poiRun = null;
        for (String line : run.text().split("\n", -1)) {
            if (poiRun != null) {
                poiRun.addBreak();
            }
            poiRun = paragraph.createRun();
            poiRun.setFontFamily(block.fontName());
            poiRun.setFontSize(block.fontSize());
            poiRun.setBold(run.bold());
            poiRun.setItalic(run.italic());
            poiRun.setUnderline(run.underline() ? UnderlinePatterns.SINGLE : UnderlinePatterns.NONE);
            poiRun.setText(line, 0);
        }
    }

    private ParagraphAlignment alignmentOf(DocumentBlock.Alignment alignment) {
        return switch (alignment) {
            case CENTER -> ParagraphAlignment.CENTER;
            case JUSTIFY -> ParagraphAlignment.BOTH;
            case LEFT -> ParagraphAlignment.LEFT;
        };
    }
}
