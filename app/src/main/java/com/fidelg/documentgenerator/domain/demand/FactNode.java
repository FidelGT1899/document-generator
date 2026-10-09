package com.fidelg.documentgenerator.domain.demand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Nodo del árbol de fundamentos fácticos. Un hecho puede contener subhechos sin
 * profundidad máxima. La numeración (3.12.1) se deriva de la posición en el
 * árbol al momento de ensamblar: nunca se almacena en el nodo.
 *
 * <p>Los datos estructurados solo generan el contenido inicial. Una vez que el
 * usuario modifica {@code content}, el nodo queda sucio ({@code dirty}) y los
 * cambios posteriores de los inputs no lo sobrescriben.</p>
 */
public class FactNode {

    private String id;
    private FactNodeKind kind;
    private String templateId;
    private Map<String, String> inputs = new LinkedHashMap<>();
    private String generatedContent = "";
    private String content = "";
    private boolean dirty;
    private List<FactNode> children = new ArrayList<>();

    public FactNode() {
    }

    public static FactNode subtitle(String text) {
        FactNode node = new FactNode();
        node.id = UUID.randomUUID().toString();
        node.kind = FactNodeKind.SUBTITLE;
        node.content = text == null ? "" : text;
        return node;
    }

    public static FactNode fact(String templateId) {
        FactNode node = new FactNode();
        node.id = UUID.randomUUID().toString();
        node.kind = FactNodeKind.FACT;
        node.templateId = templateId;
        return node;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public FactNodeKind getKind() {
        return kind;
    }

    public void setKind(FactNodeKind kind) {
        this.kind = kind;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public Map<String, String> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, String> inputs) {
        this.inputs = inputs == null ? new LinkedHashMap<>() : inputs;
    }

    public void setInput(String key, String value) {
        inputs.put(key, value);
    }

    public String getGeneratedContent() {
        return generatedContent;
    }

    public void setGeneratedContent(String generatedContent) {
        this.generatedContent = valueOrEmpty(generatedContent);
    }

    public String getContent() {
        return content;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /** El usuario modificó el contenido final. */
    public void markContent(String text) {
        this.content = valueOrEmpty(text);
        this.dirty = !this.content.equals(generatedContent);
    }

    /** El contenido se (re)generó desde los inputs: deja de estar sucio. */
    public void applyGenerated(String text) {
        this.generatedContent = valueOrEmpty(text);
        this.content = this.generatedContent;
        this.dirty = false;
    }

    public List<FactNode> getChildren() {
        return children;
    }

    public void setChildren(List<FactNode> children) {
        this.children = children == null ? new ArrayList<>() : children;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FactNode node)) {
            return false;
        }
        return Objects.equals(id, node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
