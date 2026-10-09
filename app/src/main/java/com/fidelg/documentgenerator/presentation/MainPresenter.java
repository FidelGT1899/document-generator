package com.fidelg.documentgenerator.presentation;

import com.fidelg.documentgenerator.application.ContentGenerationService;
import com.fidelg.documentgenerator.application.CreateDemandService;
import com.fidelg.documentgenerator.application.DraftService;
import com.fidelg.documentgenerator.application.GenerateDocumentService;
import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.domain.demand.CollectionItem;
import com.fidelg.documentgenerator.domain.demand.DemandDocument;
import com.fidelg.documentgenerator.domain.demand.DemandDocumentAssembler;
import com.fidelg.documentgenerator.domain.demand.FactNode;
import com.fidelg.documentgenerator.domain.demand.FactTreeOperations;
import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.demand.SectionKind;
import com.fidelg.documentgenerator.domain.type.DocumentType;
import com.fidelg.documentgenerator.domain.type.DocumentTypeRegistry;
import com.fidelg.documentgenerator.domain.type.FactTemplate;
import com.fidelg.documentgenerator.domain.type.SectionSpec;

import javax.swing.SwingWorker;
import java.nio.file.Path;
import java.util.List;

/**
 * Presentador de la demanda: posee el modelo, aplica las reglas de generación
 * (nunca sobrescribe contenido editado), mantiene la numeración derivada al
 * refrescar la vista previa y delega en los casos de uso.
 */
public class MainPresenter implements MainViewListener {

    private final MainView view;
    private final DocumentTypeRegistry registry;
    private final CreateDemandService createService;
    private final ContentGenerationService generationService;
    private final DraftService draftService;
    private final GenerateDocumentService generateService;
    private final DemandDocumentAssembler assembler = new DemandDocumentAssembler();

    private DocumentType type;
    private DemandDocument document;

    public MainPresenter(MainView view,
                         DocumentTypeRegistry registry,
                         CreateDemandService createService,
                         ContentGenerationService generationService,
                         DraftService draftService,
                         GenerateDocumentService generateService,
                         String initialTypeId) {
        this.view = view;
        this.registry = registry;
        this.createService = createService;
        this.generationService = generationService;
        this.draftService = draftService;
        this.generateService = generateService;
        view.setListener(this);
        startNew(initialTypeId);
    }

    private void startNew(String typeId) {
        type = registry.get(typeId);
        document = createService.execute(typeId);
        view.showDocument(type, document);
        refreshPreview();
    }

    private void refreshPreview() {
        view.showPreview(assembler.assemble(document, type));
    }

    @Override
    public void newDocument() {
        if (!confirmDiscard()) {
            return;
        }
        startNew(type.id());
    }

    /** Hay datos cargados que se perderían al crear uno nuevo o al cargar otro borrador. */
    private boolean hasWorkInProgress() {
        for (Section section : document.getSections()) {
            if (section.getInputs().values().stream().anyMatch(value -> value != null && !value.isBlank())) {
                return true;
            }
            if (!section.getContent().isBlank() || !section.getClosingContent().isBlank()) {
                return true;
            }
            if (!section.getItems().isEmpty() || !section.getFacts().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean confirmDiscard() {
        return !hasWorkInProgress() || view.confirm(
                "Hay datos sin guardar que se perderán.\n¿Descartarlos?");
    }

    @Override
    public void inputChanged(String sectionId, String key, String value) {
        Section section = document.section(sectionId);
        section.setInput(key, value);
        SectionSpec spec = type.spec(sectionId);
        if (spec.kind() == SectionKind.GENERATED_TEXT
                && generationService.regenerateIfClean(section, spec, type)) {
            view.updateContent(sectionId, section.getContent());
        }
        if (spec.kind() == SectionKind.SIMPLE_INPUT || spec.kind() == SectionKind.GENERATED_TEXT) {
            // La cabecera y el texto generado alimentan directamente el documento.
            refreshPreview();
        }
    }

    @Override
    public void contentEdited(String sectionId, String text) {
        document.section(sectionId).markContent(text);
        refreshPreview();
    }

    @Override
    public void regenerateSection(String sectionId) {
        Section section = document.section(sectionId);
        SectionSpec spec = type.spec(sectionId);
        if (!canGenerate(spec)) {
            return;
        }
        if (section.isDirty() && !view.confirm(
                "El contenido de esta sección fue modificado a mano.\n"
                        + "¿Regenerarlo desde los datos y descartar esos cambios?")) {
            return;
        }
        generationService.regenerateForced(section, spec, type);
        view.updateContent(sectionId, section.getContent());
        refreshPreview();
    }

    @Override
    public void closingEdited(String sectionId, String text) {
        document.section(sectionId).markClosingContent(text);
        refreshPreview();
    }

    @Override
    public void regenerateClosing(String sectionId) {
        Section section = document.section(sectionId);
        if (section.isClosingDirty() && !view.confirm(
                "El cierre fue modificado a mano.\n¿Regenerarlo desde los hechos y descartar esos cambios?")) {
            return;
        }
        generationService.regenerateClosingForced(section, type);
        view.updateClosing(sectionId, section.getClosingContent());
        refreshPreview();
    }

    @Override
    public void factAction(String sectionId, String parentId, FactAction action, String nodeId, String templateId) {
        Section section = document.section(sectionId);
        try {
            boolean changed = applyFactAction(section, type.spec(sectionId), parentId, action, nodeId, templateId);
            if (!changed) {
                return;
            }
        } catch (IllegalArgumentException e) {
            view.showError(e.getMessage());
            return;
        }
        generationService.regenerateClosingIfClean(section, type);
        view.refreshSection(sectionId);
        view.updateClosing(sectionId, section.getClosingContent());
        refreshPreview();
    }

    private boolean applyFactAction(Section section, SectionSpec spec, String parentId, FactAction action,
                                    String nodeId, String templateId) {
        return switch (action) {
            case ADD_TITLE -> {
                FactTreeOperations.addSubtitle(section, parentId, "");
                yield true;
            }
            case ADD_FACT, ADD_SUBFACT -> {
                if (action == FactAction.ADD_SUBFACT && parentId == null) {
                    throw new IllegalArgumentException("Seleccione el hecho al que desea agregar el subhecho.");
                }
                FactTreeOperations.addFact(section, parentId, resolveTemplateId(spec, templateId));
                yield true;
            }
            case REMOVE -> nodeId != null && FactTreeOperations.remove(section, nodeId);
            case MOVE_UP -> nodeId != null && FactTreeOperations.move(section, nodeId, -1);
            case MOVE_DOWN -> nodeId != null && FactTreeOperations.move(section, nodeId, 1);
        };
    }

    private String resolveTemplateId(SectionSpec spec, String templateId) {
        if (templateId != null) {
            return templateId;
        }
        if (spec.factTemplates().isEmpty()) {
            throw new IllegalArgumentException("Este tipo de documento no define plantillas de hechos.");
        }
        return spec.factTemplates().get(0).id();
    }

    @Override
    public void factInputChanged(String sectionId, String nodeId, String key, String value) {
        Section section = document.section(sectionId);
        FactNode node = FactTreeOperations.find(section.getFacts(), nodeId);
        if (node == null) {
            return;
        }
        node.setInput(key, value);
        FactTemplate template = type.spec(sectionId).factTemplate(node.getTemplateId());
        if (generationService.regenerateFactIfClean(node, section, template, type)) {
            view.updateFactContent(sectionId, nodeId, node.getContent());
            regenerateClosingIfClean(sectionId, section);
            refreshPreview();
        }
    }

    @Override
    public void factContentEdited(String sectionId, String nodeId, String text) {
        Section section = document.section(sectionId);
        FactNode node = FactTreeOperations.find(section.getFacts(), nodeId);
        if (node == null) {
            return;
        }
        node.markContent(text);
        regenerateClosingIfClean(sectionId, section);
        refreshPreview();
    }

    @Override
    public void regenerateFact(String sectionId, String nodeId) {
        Section section = document.section(sectionId);
        FactNode node = FactTreeOperations.find(section.getFacts(), nodeId);
        if (node == null) {
            return;
        }
        FactTemplate template = type.spec(sectionId).factTemplate(node.getTemplateId());
        if (node.isDirty() && !view.confirm(
                "El hecho fue modificado a mano.\n¿Regenerarlo desde los datos y descartar esos cambios?")) {
            return;
        }
        generationService.regenerateFactForced(node, section, template, type);
        view.updateFactContent(sectionId, nodeId, node.getContent());
        regenerateClosingIfClean(sectionId, section);
        refreshPreview();
    }

    private void regenerateClosingIfClean(String sectionId, Section section) {
        if (generationService.regenerateClosingIfClean(section, type)) {
            view.updateClosing(sectionId, section.getClosingContent());
        }
    }

    @Override
    public void itemAction(String sectionId, ItemAction action, String itemId) {
        Section section = document.section(sectionId);
        List<CollectionItem> items = section.getItems();
        boolean changed = switch (action) {
            case ADD -> {
                items.add(new CollectionItem(null, ""));
                yield true;
            }
            case REMOVE -> removeItem(items, itemId);
            case MOVE_UP -> moveItem(items, itemId, -1);
            case MOVE_DOWN -> moveItem(items, itemId, 1);
        };
        if (!changed) {
            return;
        }
        SectionSpec spec = type.spec(sectionId);
        if (generationService.regenerateIfClean(section, spec, type)) {
            view.updateContent(sectionId, section.getContent());
        }
        view.refreshSection(sectionId);
        refreshPreview();
    }

    @Override
    public void itemChanged(String sectionId, String itemId, String title, String content) {
        Section section = document.section(sectionId);
        CollectionItem item = findItem(section, itemId);
        if (item == null) {
            return;
        }
        item.setTitle(title);
        item.setContent(content);
        SectionSpec spec = type.spec(sectionId);
        if (generationService.regenerateIfClean(section, spec, type)) {
            view.updateContent(sectionId, section.getContent());
        }
        refreshPreview();
    }

    private boolean removeItem(List<CollectionItem> items, String itemId) {
        return itemId != null && items.removeIf(item -> item.getId().equals(itemId));
    }

    private boolean moveItem(List<CollectionItem> items, String itemId, int delta) {
        if (itemId == null) {
            return false;
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(itemId)) {
                int target = i + delta;
                if (target < 0 || target >= items.size()) {
                    return false;
                }
                CollectionItem item = items.remove(i);
                items.add(target, item);
                return true;
            }
        }
        return false;
    }

    private CollectionItem findItem(Section section, String itemId) {
        if (itemId == null) {
            return null;
        }
        for (CollectionItem item : section.getItems()) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    @Override
    public void saveDraft(Path file) {
        try {
            draftService.save(document, file);
        } catch (Exception e) {
            view.showError("No se pudo guardar el borrador: " + e.getMessage());
        }
    }

    @Override
    public void loadDraft(Path file) {
        if (!confirmDiscard()) {
            return;
        }
        try {
            document = draftService.load(file);
            type = registry.get(document.getTypeId());
            view.showDocument(type, document);
            refreshPreview();
        } catch (Exception e) {
            view.showError("No se pudo cargar el borrador: " + e.getMessage());
        }
    }

    @Override
    public void generateDocument(Path outputFile) {
        List<DocumentBlock> blocks = assembler.assemble(document, type);
        view.setBusy(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                generateService.execute(blocks, outputFile);
                return null;
            }

            @Override
            protected void done() {
                view.setBusy(false);
                try {
                    get();
                    view.showSuccess(outputFile);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    view.showError("No se pudo generar el documento: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private boolean canGenerate(SectionSpec spec) {
        return spec.kind() == SectionKind.GENERATED_TEXT
                || (spec.kind() == SectionKind.COLLECTION && spec.hasSectionContent());
    }
}
