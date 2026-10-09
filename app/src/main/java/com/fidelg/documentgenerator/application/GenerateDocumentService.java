package com.fidelg.documentgenerator.application;

import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.infrastructure.WordDocumentGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Caso de uso: escribir el documento final en .docx.
 * Recibe los párrafos ya ensamblados a partir del contenido editable.
 */
public class GenerateDocumentService {
    private final WordDocumentGenerator generator;

    public GenerateDocumentService(WordDocumentGenerator generator) {
        this.generator = generator;
    }

    public void execute(List<DocumentBlock> blocks, Path outputFile) throws IOException {
        generator.generate(blocks, outputFile);
    }
}
