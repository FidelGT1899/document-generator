package com.fidelg.documentgenerator.domain.demand;

import java.util.List;

/**
 * Operaciones sobre el árbol de fundamentos fácticos. Cualquier hecho puede
 * contener subhechos y la numeración se reconstruye sola al ensamblar: aquí
 * solo se manipula la estructura.
 */
public final class FactTreeOperations {

    private FactTreeOperations() {
    }

    /** Agrega un rótulo agrupador. {@code parentId} nulo = raíz. */
    public static FactNode addSubtitle(Section section, String parentId, String text) {
        FactNode node = FactNode.subtitle(text);
        insertInto(section, parentId, node);
        return node;
    }

    public static FactNode addFact(Section section, String parentId, String templateId) {
        FactNode node = FactNode.fact(templateId);
        insertInto(section, parentId, node);
        return node;
    }

    /** Elimina un nodo y todo su subárbol. */
    public static boolean remove(Section section, String nodeId) {
        return removeFrom(section.getFacts(), nodeId);
    }

    /** Mueve un nodo entre sus hermanos (-1 arriba, +1 abajo). */
    public static boolean move(Section section, String nodeId, int delta) {
        return moveTo(section.getFacts(), nodeId, delta);
    }

    public static FactNode find(List<FactNode> nodes, String nodeId) {
        for (FactNode node : nodes) {
            if (node.getId().equals(nodeId)) {
                return node;
            }
            FactNode found = find(node.getChildren(), nodeId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static void insertInto(Section section, String parentId, FactNode node) {
        if (parentId == null) {
            section.getFacts().add(node);
            return;
        }
        FactNode parent = find(section.getFacts(), parentId);
        if (parent == null) {
            throw new IllegalArgumentException("Hecho no encontrado: " + parentId);
        }
        if (parent.getKind() != FactNodeKind.FACT) {
            throw new IllegalArgumentException("Solo los hechos pueden contener subhechos.");
        }
        parent.getChildren().add(node);
    }

    private static boolean removeFrom(List<FactNode> nodes, String nodeId) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().equals(nodeId)) {
                nodes.remove(i);
                return true;
            }
            if (removeFrom(nodes.get(i).getChildren(), nodeId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean moveTo(List<FactNode> nodes, String nodeId, int delta) {
        for (int i = 0; i < nodes.size(); i++) {
            FactNode node = nodes.get(i);
            if (node.getId().equals(nodeId)) {
                int target = i + delta;
                if (target < 0 || target >= nodes.size()) {
                    return false;
                }
                nodes.remove(i);
                nodes.add(target, node);
                return true;
            }
            if (moveTo(node.getChildren(), nodeId, delta)) {
                return true;
            }
        }
        return false;
    }
}
