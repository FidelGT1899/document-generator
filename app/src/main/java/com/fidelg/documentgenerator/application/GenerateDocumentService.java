package com.fidelg.documentgenerator.application;

import com.fidelg.documentgenerator.domain.DocumentRequest;
import com.fidelg.documentgenerator.infrastructure.WordDocumentGenerator;

import java.io.IOException;
import java.nio.file.Path;

public class GenerateDocumentService {
    private final WordDocumentGenerator generator;

    public GenerateDocumentService(WordDocumentGenerator generator) {
        this.generator = generator;
    }

    public void execute(DocumentRequest request, Path outputFile) throws IOException {
        generator.generate(request, outputFile);
    }
}