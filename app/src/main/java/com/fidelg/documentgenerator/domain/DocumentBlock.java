package com.fidelg.documentgenerator.domain;

public record DocumentBlock(String text, boolean bold, boolean italic, int fontSize, Alignment alignment) {
    public enum Alignment {
        LEFT,
        CENTER
    }
}
