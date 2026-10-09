package com.fidelg.documentgenerator.presentation;

import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.demand.SectionKind;
import com.fidelg.documentgenerator.domain.type.DocumentType;
import com.fidelg.documentgenerator.domain.type.SectionSpec;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Sección con datos estructurados y su contenido final editable: el textarea es
 * el texto que se incorpora al documento y puede modificarse libremente.
 */
public class TextContentPanel extends SectionPanel {

    private JTextArea contentArea;

    public TextContentPanel(SectionSpec spec, Section section, DocumentType type, MainViewListener listener) {
        super(spec, section, type, listener);
    }

    @Override
    protected JComponent buildBody() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(3, 4, 3, 4);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1;

        if (!spec.inputs().isEmpty()) {
            panel.add(buildInputRows(), constraints);
            constraints.gridy++;
        }

        if (canGenerate()) {
            JButton regenerate = new JButton("Regenerar desde datos");
            regenerate.addActionListener(e -> listener.regenerateSection(section.getId()));
            constraints.gridx = 1;
            constraints.gridwidth = 1;
            constraints.weightx = 0;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            panel.add(regenerate, constraints);
            constraints.gridx = 0;
            constraints.gridwidth = 2;
            constraints.weightx = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.gridy++;
        }

        contentArea = editableArea(section.getContent());
        contentArea.setRows(7);
        contentArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyContent();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyContent();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyContent();
            }
        });

        JScrollPane scroll = new JScrollPane(contentArea);
        scroll.setPreferredSize(new Dimension(100, 150));
        constraints.gridy++;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;
        panel.add(scroll, constraints);

        constraints.gridy++;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("El texto de arriba es el contenido final del documento."), constraints);

        return panel;
    }

    private boolean canGenerate() {
        return spec.kind() == SectionKind.GENERATED_TEXT
                || (spec.kind() == SectionKind.COLLECTION && spec.hasSectionContent());
    }

    private void notifyContent() {
        if (applying) {
            return;
        }
        listener.contentEdited(section.getId(), contentArea.getText());
    }

    @Override
    public void setContentText(String text) {
        if (contentArea == null || applying) {
            return;
        }
        applying = true;
        contentArea.setText(text);
        contentArea.setCaretPosition(0);
        applying = false;
    }
}
