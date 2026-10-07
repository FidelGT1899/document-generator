package com.fidelg.documentgenerator.domain;

import java.util.List;

public final class DocumentBlocks {
    public static final int TITLE_FONT_SIZE = 22;
    public static final int BODY_FONT_SIZE = 12;

    private DocumentBlocks() {
    }

    public static List<DocumentBlock> from(String title, String author, String body) {
        return List.of(
                new DocumentBlock(valueOf(title), true, false, TITLE_FONT_SIZE, DocumentBlock.Alignment.CENTER),
                new DocumentBlock(valueOf(author), false, true, BODY_FONT_SIZE, DocumentBlock.Alignment.CENTER),
                new DocumentBlock(valueOf(body), false, false, BODY_FONT_SIZE, DocumentBlock.Alignment.LEFT));
    }

    private static String valueOf(String text) {
        return text == null ? "" : text;
    }
}
