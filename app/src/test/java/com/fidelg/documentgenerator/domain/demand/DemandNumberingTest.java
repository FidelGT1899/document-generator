package com.fidelg.documentgenerator.domain.demand;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemandNumberingTest {

    @Test
    void convertsNumbersToRomanNumerals() {
        assertEquals("I", DemandNumbering.roman(1));
        assertEquals("IV", DemandNumbering.roman(4));
        assertEquals("IX", DemandNumbering.roman(9));
        assertEquals("XIV", DemandNumbering.roman(14));
        assertEquals("XL", DemandNumbering.roman(40));
        assertEquals("XXXIX", DemandNumbering.roman(39));
    }

    @Test
    void rejectsRomanNumbersOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> DemandNumbering.roman(0));
        assertThrows(IllegalArgumentException.class, () -> DemandNumbering.roman(4000));
    }

    @Test
    void numbersFactsByPositionAndHierarchyIgnoringSubtitles() {
        Section section = new Section("hechos");
        FactTreeOperations.addSubtitle(section, null, "Necesidades");
        FactNode first = FactTreeOperations.addFact(section, null, "hecho");
        FactNode second = FactTreeOperations.addFact(section, null, "hecho");
        FactNode child = FactTreeOperations.addFact(section, second.getId(), "hecho");
        FactTreeOperations.addSubtitle(section, second.getId(), "Detalle");
        FactNode grandChild = FactTreeOperations.addFact(section, child.getId(), "hecho");

        Map<String, String> numbers = DemandNumbering.factNumbers(section.getFacts());

        assertEquals("1", numbers.get(first.getId()));
        assertEquals("2", numbers.get(second.getId()));
        assertEquals("2.1", numbers.get(child.getId()));
        assertEquals("2.1.1", numbers.get(grandChild.getId()));
        assertEquals(4, numbers.size());
    }

    @Test
    void renumbersWhenStructureChanges() {
        Section section = new Section("hechos");
        FactNode first = FactTreeOperations.addFact(section, null, "hecho");
        FactNode second = FactTreeOperations.addFact(section, null, "hecho");

        FactTreeOperations.remove(section, first.getId());

        Map<String, String> numbers = DemandNumbering.factNumbers(section.getFacts());
        assertEquals("1", numbers.get(second.getId()));
        assertFalse(numbers.containsKey(first.getId()));
    }

    @Test
    void labelsEvidenceWithTwentySevenLetterGroups() {
        assertEquals("1-A", DemandNumbering.evidenceLabel(0));
        assertEquals("1-N", DemandNumbering.evidenceLabel(13));
        assertEquals("1-Ñ", DemandNumbering.evidenceLabel(14));
        assertEquals("1-Z", DemandNumbering.evidenceLabel(26));
        assertEquals("2-A", DemandNumbering.evidenceLabel(27));
        assertEquals("2-Z", DemandNumbering.evidenceLabel(53));
        assertEquals("3-A", DemandNumbering.evidenceLabel(54));
        assertThrows(IllegalArgumentException.class, () -> DemandNumbering.evidenceLabel(-1));
    }

    @Test
    void namesOthersiesWithOrdinals() {
        assertEquals("PRIMER", DemandNumbering.otrosiOrdinal(0));
        assertEquals("SEGUNDO", DemandNumbering.otrosiOrdinal(1));
        assertEquals("DÉCIMO", DemandNumbering.otrosiOrdinal(9));
        assertEquals("OTROSÍ 11", DemandNumbering.otrosiOrdinal(10));
        assertThrows(IllegalArgumentException.class, () -> DemandNumbering.otrosiOrdinal(-1));
    }

    @Test
    void keepsSubtitleFromConsumingANumber() {
        Section section = new Section("hechos");
        FactNode subtitle = FactTreeOperations.addSubtitle(section, null, "Rótulo");
        FactNode fact = FactTreeOperations.addFact(section, null, "hecho");

        Map<String, String> numbers = DemandNumbering.factNumbers(section.getFacts());

        assertEquals("1", numbers.get(fact.getId()));
        assertTrue(!numbers.containsKey(subtitle.getId()));
    }
}
