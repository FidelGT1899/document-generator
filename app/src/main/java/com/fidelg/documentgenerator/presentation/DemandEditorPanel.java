package com.fidelg.documentgenerator.presentation;

import com.fidelg.documentgenerator.domain.demand.DemandDocument;
import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.demand.SectionKind;
import com.fidelg.documentgenerator.domain.type.DocumentType;
import com.fidelg.documentgenerator.domain.type.SectionSpec;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contenedor de los paneles de cada sección de la demanda, en el orden de la
 * estructura del tipo de documento.
 */
public class DemandEditorPanel extends JPanel {

    private MainViewListener listener;
    private final Map<String, SectionPanel> panels = new LinkedHashMap<>();
    private DocumentType type;
    private DemandDocument document;

    public DemandEditorPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void setListener(MainViewListener listener) {
        this.listener = listener;
    }

    public void showDocument(DocumentType type, DemandDocument document) {
        this.type = type;
        this.document = document;
        removeAll();
        panels.clear();
        for (SectionSpec spec : type.sections()) {
            Section section = document.section(spec.id());
            SectionPanel panel = createPanel(spec, section);
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panels.put(spec.id(), panel);
            add(panel);
        }
        add(Box.createVerticalGlue());
        revalidate();
        repaint();
    }

    private SectionPanel createPanel(SectionSpec spec, Section section) {
        if (spec.kind() == SectionKind.FACTS) {
            return new FactsPanel(spec, section, type, listener);
        }
        if (spec.kind() == SectionKind.COLLECTION) {
            return new CollectionPanel(spec, section, type, listener);
        }
        if (spec.kind() == SectionKind.SIMPLE_INPUT) {
            return new SimpleInputsPanel(spec, section, type, listener);
        }
        return new TextContentPanel(spec, section, type, listener);
    }

    public void updateContent(String sectionId, String text) {
        SectionPanel panel = panels.get(sectionId);
        if (panel != null) {
            panel.setContentText(text);
        }
    }

    public void updateClosing(String sectionId, String text) {
        SectionPanel panel = panels.get(sectionId);
        if (panel != null) {
            panel.setClosingText(text);
        }
    }

    public void updateFactContent(String sectionId, String nodeId, String text) {
        SectionPanel panel = panels.get(sectionId);
        if (panel != null) {
            panel.setFactContent(nodeId, text);
        }
    }

    public void refreshSection(String sectionId) {
        SectionPanel panel = panels.get(sectionId);
        if (panel != null) {
            panel.rebuild();
            panel.revalidate();
            panel.repaint();
        }
    }
}
