package com.fidelg.documentgenerator.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentRequestTest {

    @Test
    void acceptsValidData() {
        assertDoesNotThrow(() -> new DocumentRequest("Título", "Autor", "Cuerpo"));
    }

    @Test
    void requiresTitle() {
        assertThrows(IllegalArgumentException.class, () -> new DocumentRequest("   ", "Autor", "Cuerpo"));
    }

    @Test
    void requiresAuthor() {
        assertThrows(IllegalArgumentException.class, () -> new DocumentRequest("Título", null, "Cuerpo"));
    }

    @Test
    void requiresBody() {
        assertThrows(IllegalArgumentException.class, () -> new DocumentRequest("Título", "Autor", ""));
    }
}