package com.fidelg.documentgenerator.domain;

public record DocumentRequest(String title, String author, String body) {
    public DocumentRequest {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio.");
        }
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("El autor es obligatorio.");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("El cuerpo del documento es obligatorio.");
        }
    }
}