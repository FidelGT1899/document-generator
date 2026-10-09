package com.fidelg.documentgenerator.presentation;

import com.fidelg.documentgenerator.domain.demand.CollectionItem;
import com.fidelg.documentgenerator.domain.demand.DemandNumbering;
import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.type.CollectionFormat;
import com.fidelg.documentgenerator.domain.type.DocumentType;
import com.fidelg.documentgenerator.domain.type.SectionSpec;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Sección colección: lista ordenada de elementos (medios probatorios, otrosíes,
 * grupos normativos). La numeración mostrada en cada elemento se deriva de su
 * posición y se reconstruye al reordenar.
 */
public class CollectionPanel extends SectionPanel {

    private JPanel itemsPanel;
    private JTextArea sectionContentArea;

    public CollectionPanel(SectionSpec spec, Section section, DocumentType type, MainViewListener listener) {
        super(spec, section, type, listener);
    }

    @Override
    protected JComponent buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 6));

        JButton add = new JButton("+ Agregar");
        add.addActionListener(e -> listener.itemAction(section.getId(), ItemAction.ADD, null));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        toolbar.add(add);
        body.add(toolbar, BorderLayout.NORTH);

        itemsPanel = new JPanel();
        itemsPanel.setLayout(new javax.swing.BoxLayout(itemsPanel, javax.swing.BoxLayout.Y_AXIS));
        rebuildItems();
        JScrollPane scroll = new JScrollPane(itemsPanel);
        scroll.setPreferredSize(new Dimension(100, 220));
        body.add(scroll, BorderLayout.CENTER);

        if (spec.hasSectionContent()) {
            body.add(buildSectionContent(), BorderLayout.SOUTH);
        }
        return body;
    }

    private JComponent buildSectionContent() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        sectionContentArea = editableArea(section.getContent());
        sectionContentArea.setRows(6);
        sectionContentArea.getDocument().addDocumentListener(new DocumentListener() {
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
        JButton regenerate = new JButton("Regenerar desde los grupos");
        regenerate.addActionListener(e -> listener.regenerateSection(section.getId()));

        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("Contenido final:"), BorderLayout.WEST);
        top.add(regenerate, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(sectionContentArea), BorderLayout.CENTER);
        return panel;
    }

    private void notifyContent() {
        if (applying || sectionContentArea == null) {
            return;
        }
        listener.contentEdited(section.getId(), sectionContentArea.getText());
    }

    @Override
    public void rebuild() {
        rebuildItems();
    }

    private void rebuildItems() {
        if (itemsPanel == null) {
            return;
        }
        applying = true;
        itemsPanel.removeAll();
        List<CollectionItem> items = section.getItems();
        int renderableIndex = 0;
        for (CollectionItem item : items) {
            boolean renderable = isRenderable(item);
            int labelIndex = renderable ? renderableIndex : -1;
            if (renderable) {
                renderableIndex++;
            }
            itemsPanel.add(buildItemPanel(item, labelIndex));
        }
        if (items.isEmpty()) {
            itemsPanel.add(new JLabel("Sin elementos. Use el botón \"+ Agregar\"."));
        }
        applying = false;
        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    private boolean isRenderable(CollectionItem item) {
        return (item.getTitle() != null && !item.getTitle().isBlank()) || !item.getContent().isBlank();
    }

    private JPanel buildItemPanel(CollectionItem item, int index) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder(itemLabel(item, index)));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        if (spec.collectionFormat() != CollectionFormat.EVIDENCE) {
            String titleCaption = spec.collectionFormat() == CollectionFormat.OTROSIE
                    ? "Título / asunto:" : "Título del grupo:";
            JTextField titleField = new JTextField(item.getTitle() == null ? "" : item.getTitle());
            titleField.getDocument().addDocumentListener(itemListener(item, titleField, null));
            JPanel titleRow = new JPanel(new BorderLayout(4, 0));
            titleRow.add(new JLabel(titleCaption), BorderLayout.WEST);
            titleRow.add(titleField, BorderLayout.CENTER);
            panel.add(titleRow, BorderLayout.NORTH);
        }

        JTextArea content = editableArea(item.getContent());
        content.setRows(spec.collectionFormat() == CollectionFormat.EVIDENCE ? 2 : 4);
        content.getDocument().addDocumentListener(itemListener(item, null, content));
        JScrollPane scroll = new JScrollPane(content);
        scroll.setPreferredSize(new Dimension(100, 60));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.add(button("↑", ItemAction.MOVE_UP, item));
        buttons.add(button("↓", ItemAction.MOVE_DOWN, item));
        buttons.add(button("Eliminar", ItemAction.REMOVE, item));
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private String itemLabel(CollectionItem item, int index) {
        if (index < 0) {
            return "(sin contenido)";
        }
        return switch (spec.collectionFormat()) {
            case EVIDENCE -> DemandNumbering.evidenceLabel(index);
            case OTROSIE -> DemandNumbering.otrosiOrdinal(index) + " OTROSÍ";
            case LEGAL_GROUP -> "Grupo " + (index + 1);
        };
    }

    private JButton button(String text, ItemAction action, CollectionItem item) {
        JButton button = new JButton(text);
        button.addActionListener(e -> listener.itemAction(section.getId(), action, item.getId()));
        return button;
    }

    private DocumentListener itemListener(CollectionItem item, JTextField titleField, JTextArea contentArea) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyItem(item, titleField, contentArea);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyItem(item, titleField, contentArea);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyItem(item, titleField, contentArea);
            }
        };
    }

    private void notifyItem(CollectionItem item, JTextField titleField, JTextArea contentArea) {
        if (applying) {
            return;
        }
        String title = titleField != null ? titleField.getText() : item.getTitle();
        String content = contentArea != null ? contentArea.getText() : item.getContent();
        listener.itemChanged(section.getId(), item.getId(), title, content);
    }

    @Override
    public void setContentText(String text) {
        if (sectionContentArea == null || applying) {
            return;
        }
        applying = true;
        sectionContentArea.setText(text);
        sectionContentArea.setCaretPosition(0);
        applying = false;
    }
}
