package com.fidelg.documentgenerator.domain.demand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sección de una demanda: agrupa los datos estructurados, el contenido generado
 * y el contenido final editable.
 *
 * <p>El texto final que se incorpora al documento es siempre {@code content}
 * (o {@code closingContent} en la sección de hechos). {@code inputs} y
 * {@code items} solo alimentan la generación inicial.</p>
 */
public class Section {

    private String id;
    private Map<String, String> inputs = new LinkedHashMap<>();
    private List<CollectionItem> items = new ArrayList<>();
    private List<FactNode> facts = new ArrayList<>();
    private String generatedContent = "";
    private String content = "";
    private boolean dirty;
    private String closingGeneratedContent = "";
    private String closingContent = "";
    private boolean closingDirty;

    public Section() {
    }

    public Section(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String input(String key) {
        return inputs.getOrDefault(key, "");
    }

    public List<CollectionItem> getItems() {
        return items;
    }

    public void setItems(List<CollectionItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public List<FactNode> getFacts() {
        return facts;
    }

    public void setFacts(List<FactNode> facts) {
        this.facts = facts == null ? new ArrayList<>() : facts;
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

    /** El usuario modificó el contenido final de la sección. */
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

    public String getClosingGeneratedContent() {
        return closingGeneratedContent;
    }

    public void setClosingGeneratedContent(String closingGeneratedContent) {
        this.closingGeneratedContent = valueOrEmpty(closingGeneratedContent);
    }

    public String getClosingContent() {
        return closingContent;
    }

    public boolean isClosingDirty() {
        return closingDirty;
    }

    public void setClosingDirty(boolean closingDirty) {
        this.closingDirty = closingDirty;
    }

    /** El usuario modificó el cierre/conclusión de los fundamentos fácticos. */
    public void markClosingContent(String text) {
        this.closingContent = valueOrEmpty(text);
        this.closingDirty = !this.closingContent.equals(closingGeneratedContent);
    }

    /** El cierre se (re)generó desde los hechos anteriores. */
    public void applyClosingGenerated(String text) {
        this.closingGeneratedContent = valueOrEmpty(text);
        this.closingContent = this.closingGeneratedContent;
        this.closingDirty = false;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
