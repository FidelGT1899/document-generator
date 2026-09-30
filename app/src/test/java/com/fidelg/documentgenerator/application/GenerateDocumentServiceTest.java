package com.fidelg.documentgenerator.application;

import com.fidelg.documentgenerator.domain.DocumentRequest;
import com.fidelg.documentgenerator.infrastructure.WordDocumentGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateDocumentServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesDocumentFile() throws Exception {
        GenerateDocumentService service = new GenerateDocumentService(new WordDocumentGenerator());
        Path output = tempDir.resolve("documento.docx");

        service.execute(new DocumentRequest("Título", "Autor", "Cuerpo del documento"), output);

        assertTrue(Files.exists(output));
    }
}