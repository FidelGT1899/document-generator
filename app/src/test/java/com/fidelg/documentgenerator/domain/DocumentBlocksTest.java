package com.fidelg.documentgenerator.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentBlocksTest {

    @Test
    void buildsTitleAuthorAndBodyInOrder() {
        List<DocumentBlock> blocks = DocumentBlocks.from("Título", "Autor", "Cuerpo");

        assertEquals(3, blocks.size());
        assertEquals("Título", blocks.get(0).text());
        assertEquals("Autor", blocks.get(1).text());
        assertEquals("Cuerpo", blocks.get(2).text());
    }

    @Test
    void appliesDocumentStyles() {
        List<DocumentBlock> blocks = DocumentBlocks.from("Título", "Autor", "Cuerpo");

        DocumentBlock title = blocks.get(0);
        assertTrue(title.bold());
        assertFalse(title.italic());
        assertEquals(22, title.fontSize());
        assertEquals(DocumentBlock.Alignment.CENTER, title.alignment());

        DocumentBlock author = blocks.get(1);
        assertFalse(author.bold());
        assertTrue(author.italic());
        assertEquals(12, author.fontSize());
        assertEquals(DocumentBlock.Alignment.CENTER, author.alignment());

        DocumentBlock body = blocks.get(2);
        assertFalse(body.bold());
        assertFalse(body.italic());
        assertEquals(12, body.fontSize());
        assertEquals(DocumentBlock.Alignment.LEFT, body.alignment());
    }

    @Test
    void keepsBlankAndNullValuesAsEmptyText() {
        List<DocumentBlock> blocks = DocumentBlocks.from("", "  ", null);

        assertEquals(3, blocks.size());
        assertEquals("", blocks.get(0).text());
        assertEquals("  ", blocks.get(1).text());
        assertEquals("", blocks.get(2).text());
    }
}
