package com.fidelg.documentgenerator.presentation;

import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.type.DocumentType;
import com.fidelg.documentgenerator.domain.type.InputSpec;
import com.fidelg.documentgenerator.domain.type.SectionSpec;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Panel base de una sección: cabecera con el título y utilidades comunes para
 * campos de datos estructurados. El flag {@code applying} evita que las
 * actualizaciones programáticas disparen eventos hacia el presentador.
 */
public abstract class SectionPanel extends JPanel {

    protected final SectionSpec spec;
    protected final Section section;
    protected final DocumentType type;
    protected final MainViewListener listener;
    protected boolean applying;

    protected SectionPanel(SectionSpec spec, Section section, DocumentType type, MainViewListener listener) {
        super(new BorderLayout(0, 6));
        this.spec = spec;
        this.section = section;
        this.type = type;
        this.listener = listener;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(6, 6, 6, 6),
                BorderFactory.createTitledBorder(spec.title())));
        add(buildBody(), BorderLayout.CENTER);
    }

    protected abstract JComponent buildBody();

    /** Reconstruye la parte estructural (listas, árbol) desde el modelo. */
    public void rebuild() {
    }

    public void setContentText(String text) {
    }

    public void setClosingText(String text) {
    }

    public void setFactContent(String nodeId, String text) {
    }

    protected JPanel buildInputRows() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(3, 4, 3, 4);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        for (InputSpec input : spec.inputs()) {
            panel.add(new JLabel(input.label() + ":"), constraints);
            constraints.gridx = 1;
            constraints.weightx = 1;
            panel.add(inputField(input), constraints);
            constraints.gridx = 0;
            constraints.weightx = 0;
            constraints.gridy++;
        }
        return panel;
    }

    protected JComponent inputField(InputSpec input) {
        if (input.multiline()) {
            JTextArea area = new JTextArea(3, 28);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setText(section.input(input.key()));
            area.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3, 4, 3, 4)));
            area.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    notifyInput(input, area.getText());
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    notifyInput(input, area.getText());
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    notifyInput(input, area.getText());
                }
            });
            return new JScrollPane(area);
        }
        JTextField field = new JTextField(28);
        field.setText(section.input(input.key()));
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyInput(input, field.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyInput(input, field.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyInput(input, field.getText());
            }
        });
        return field;
    }

    private void notifyInput(InputSpec input, String value) {
        if (applying) {
            return;
        }
        listener.inputChanged(section.getId(), input.key(), value);
    }

    protected static JTextArea editableArea(String text) {
        JTextArea area = new JTextArea(text, 5, 30);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        return area;
    }
}
