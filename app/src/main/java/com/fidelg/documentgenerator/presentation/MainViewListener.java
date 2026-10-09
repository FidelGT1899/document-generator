package com.fidelg.documentgenerator.presentation;

import java.nio.file.Path;

/**
 * Eventos que la vista delega al presentador. La vista no contiene lógica de
 * negocio: solo captura la interacción y la traduce a estos eventos.
 */
public interface MainViewListener {

    void newDocument();

    void inputChanged(String sectionId, String key, String value);

    void contentEdited(String sectionId, String text);

    void regenerateSection(String sectionId);

    void closingEdited(String sectionId, String text);

    void regenerateClosing(String sectionId);

    void factAction(String sectionId, String parentId, FactAction action, String nodeId, String templateId);

    void factInputChanged(String sectionId, String nodeId, String key, String value);

    void factContentEdited(String sectionId, String nodeId, String text);

    void regenerateFact(String sectionId, String nodeId);

    void itemAction(String sectionId, ItemAction action, String itemId);

    void itemChanged(String sectionId, String itemId, String title, String content);

    void saveDraft(Path file);

    void loadDraft(Path file);

    void generateDocument(Path file);
}
