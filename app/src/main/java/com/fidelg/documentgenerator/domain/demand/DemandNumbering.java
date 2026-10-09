package com.fidelg.documentgenerator.domain.demand;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Numeración derivada de la estructura. Ningún número se almacena: se recalcula
 * siempre a partir de la posición/jerarquía del elemento, de modo que agregar,
 * eliminar o reordenar reconstruye la serie completa.
 */
public final class DemandNumbering {

    /** Alfabeto con Ñ: 27 letras por grupo de anexos (regla deducida de los ejemplos). */
    static final String LETTERS = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ";

    private static final String[] ORDINALS = {
            "PRIMER", "SEGUNDO", "TERCER", "CUARTO", "QUINTO",
            "SEXTO", "SÉPTIMO", "OCTAVO", "NOVENO", "DÉCIMO"
    };

    private DemandNumbering() {
    }

    public static String roman(int number) {
        if (number < 1 || number > 3999) {
            throw new IllegalArgumentException("Número romano fuera de rango: " + number);
        }
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder builder = new StringBuilder();
        int remaining = number;
        for (int i = 0; i < values.length; i++) {
            while (remaining >= values[i]) {
                builder.append(symbols[i]);
                remaining -= values[i];
            }
        }
        return builder.toString();
    }

    /**
     * Numeración de los hechos por posición en el árbol. Los subtítulos no
     * consumen número; cualquier hecho puede tener subhechos a cualquier
     * profundidad. Devuelve id de nodo → número relativo a la sección
     * (ej. "12", "12.1"); el ensamblador antepone el número de sección.
     */
    public static Map<String, String> factNumbers(List<FactNode> roots) {
        Map<String, String> numbers = new LinkedHashMap<>();
        numberSiblings(roots, null, numbers);
        return numbers;
    }

    private static void numberSiblings(List<FactNode> nodes, String parentNumber, Map<String, String> numbers) {
        int siblingCounter = 0;
        for (FactNode node : nodes) {
            if (node.getKind() != FactNodeKind.FACT) {
                continue;
            }
            siblingCounter++;
            String number = parentNumber == null
                    ? String.valueOf(siblingCounter)
                    : parentNumber + "." + siblingCounter;
            numbers.put(node.getId(), number);
            numberSiblings(node.getChildren(), number, numbers);
        }
    }

    /**
     * Numeración de anexos: 1-A, 1-B, ... 1-Ñ, 1-O ... 1-Z, 2-A, 2-B...
     * El prefijo cambia cada {@link #LETTERS}.length() elementos.
     */
    public static String evidenceLabel(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Índice de anexo negativo: " + index);
        }
        int group = index / LETTERS.length() + 1;
        char letter = LETTERS.charAt(index % LETTERS.length());
        return group + "-" + letter;
    }

    /** Ordinal de otrosí: PRIMER OTROSÍ, SEGUNDO OTROSÍ, ... */
    public static String otrosiOrdinal(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Índice de otrosí negativo: " + index);
        }
        if (index < ORDINALS.length) {
            return ORDINALS[index];
        }
        return "OTROSÍ " + (index + 1);
    }
}
