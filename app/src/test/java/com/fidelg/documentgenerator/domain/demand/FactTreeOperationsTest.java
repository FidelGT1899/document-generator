package com.fidelg.documentgenerator.domain.demand;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactTreeOperationsTest {

    private final Section section = new Section("hechos");

    @Test
    void addsFactsAtRootAndBelowOtherFacts() {
        FactNode parent = FactTreeOperations.addFact(section, null, "hecho");
        FactNode child = FactTreeOperations.addFact(section, parent.getId(), "hecho");

        assertEquals(1, section.getFacts().size());
        assertEquals(child, parent.getChildren().get(0));
        assertEquals(child, FactTreeOperations.find(section.getFacts(), child.getId()));
    }

    @Test
    void addsSubtitlesAtRoot() {
        FactNode subtitle = FactTreeOperations.addSubtitle(section, null, "Rótulo");

        assertEquals(1, section.getFacts().size());
        assertEquals(FactNodeKind.SUBTITLE, subtitle.getKind());
    }

    @Test
    void refusesChildrenUnderASubtitle() {
        FactNode subtitle = FactTreeOperations.addSubtitle(section, null, "Rótulo");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> FactTreeOperations.addFact(section, subtitle.getId(), "hecho"));
        assertEquals("Solo los hechos pueden contener subhechos.", error.getMessage());
        assertTrue(subtitle.getChildren().isEmpty());
    }

    @Test
    void rejectsUnknownParent() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> FactTreeOperations.addFact(section, "no-existe", "hecho"));
        assertEquals("Hecho no encontrado: no-existe", error.getMessage());
    }

    @Test
    void removesANodeAndItsSubtree() {
        FactNode parent = FactTreeOperations.addFact(section, null, "hecho");
        FactNode child = FactTreeOperations.addFact(section, parent.getId(), "hecho");

        assertTrue(FactTreeOperations.remove(section, parent.getId()));

        assertTrue(section.getFacts().isEmpty());
        assertNull(FactTreeOperations.find(section.getFacts(), child.getId()));
    }

    @Test
    void returnsFalseWhenRemovingAnUnknownNode() {
        assertFalse(FactTreeOperations.remove(section, "no-existe"));
    }

    @Test
    void movesNodesAmongTheirSiblings() {
        FactNode first = FactTreeOperations.addFact(section, null, "hecho");
        FactNode second = FactTreeOperations.addFact(section, null, "hecho");
        FactNode third = FactTreeOperations.addFact(section, null, "hecho");

        assertTrue(FactTreeOperations.move(section, second.getId(), -1));
        assertEquals(second, section.getFacts().get(0));

        assertTrue(FactTreeOperations.move(section, second.getId(), 1));
        assertEquals(second, section.getFacts().get(1));

        assertFalse(FactTreeOperations.move(section, third.getId(), 1));
        assertFalse(FactTreeOperations.move(section, first.getId(), -1));
        assertEquals(3, section.getFacts().size());
    }

    @Test
    void movesSubFactsWithinTheirParent() {
        FactNode parent = FactTreeOperations.addFact(section, null, "hecho");
        FactNode childA = FactTreeOperations.addFact(section, parent.getId(), "hecho");
        FactNode childB = FactTreeOperations.addFact(section, parent.getId(), "hecho");

        assertTrue(FactTreeOperations.move(section, childB.getId(), -1));

        assertEquals(childB, parent.getChildren().get(0));
        assertEquals(childA, parent.getChildren().get(1));
    }

    @Test
    void returnsFalseWhenMovingAnUnknownNode() {
        assertFalse(FactTreeOperations.move(section, "no-existe", 1));
    }
}
