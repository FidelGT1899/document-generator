package com.fidelg.documentgenerator.presentation;

import java.nio.file.Path;

public interface MainViewListener {
    void formChanged(DocumentFormData form);

    void generateDocument(DocumentFormData form, Path outputFile);
}
