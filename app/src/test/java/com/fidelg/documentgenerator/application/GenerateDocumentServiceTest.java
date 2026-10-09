package com.fidelg.documentgenerator.application;

import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.domain.DemandStyles;
import com.fidelg.documentgenerator.infrastructure.WordDocumentGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateDocumentServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesDocumentFile() throws Exception {
        GenerateDocumentService service = new GenerateDocumentService(new WordDocumentGenerator());
        Path output = tempDir.resolve("documento.docx");

        service.execute(List.of(DocumentBlock.plain("Título",
                DocumentBlock.Alignment.CENTER, DemandStyles.FONT),
                DocumentBlock.plain("Cuerpo del documento")), output);

        assertTrue(Files.exists(output));
    }
}
