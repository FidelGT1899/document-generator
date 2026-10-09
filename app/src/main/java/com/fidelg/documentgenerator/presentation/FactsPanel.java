package com.fidelg.documentgenerator.presentation;

import com.fidelg.documentgenerator.domain.demand.DemandNumbering;
import com.fidelg.documentgenerator.domain.demand.FactNode;
import com.fidelg.documentgenerator.domain.demand.FactNodeKind;
import com.fidelg.documentgenerator.domain.demand.FactTreeOperations;
import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.type.DocumentType;
import com.fidelg.documentgenerator.domain.type.FactTemplate;
import com.fidelg.documentgenerator.domain.type.InputSpec;
import com.fidelg.documentgenerator.domain.type.SectionSpec;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Editor de la sección jerárquica de fundamentos fácticos: árbol de subtítulos,
 * hechos y subhechos, con su cierre opcional. La numeración que se muestra se
 * deriva de la posición en el árbol, igual que en el documento final.
 */
public class FactsPanel extends SectionPanel {

    private JTree tree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode treeRoot;
    private JPanel editorPanel;
    private JTextArea factContentArea;
    private JTextArea closingArea;
    private JComboBox<String> templateCombo;
    private String selectedNodeId;
    private boolean treeInitialized;

    public FactsPanel(SectionSpec spec, Section section, DocumentType type, MainViewListener listener) {
        super(spec, section, type, listener);
    }

    @Override
    protected JComponent buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 6));
        body.add(buildToolbar(), BorderLayout.NORTH);
        body.add(buildTree(), BorderLayout.CENTER);
        body.add(buildSouth(), BorderLayout.SOUTH);
        rebuild();
        return body;
    }

    private JComponent buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton addTitle = new JButton("+ Título / subtítulo");
        addTitle.addActionListener(e ->
                listener.factAction(section.getId(), null, FactAction.ADD_TITLE, null, null));
        toolbar.add(addTitle);

        if (spec.factTemplates().size() > 1) {
            String[] names = spec.factTemplates().stream().map(FactTemplate::name).toArray(String[]::new);
            templateCombo = new JComboBox<>(names);
            toolbar.add(templateCombo);
        }

        JButton addFact = new JButton("+ Agregar hecho");
        addFact.addActionListener(e ->
                listener.factAction(section.getId(), null, FactAction.ADD_FACT, null, selectedTemplateId()));
        toolbar.add(addFact);
        return toolbar;
    }

    private JComponent buildTree() {
        tree = new JTree();
        tree.addTreeSelectionListener(this::onSelection);
        tree.setCellRenderer(new javax.swing.tree.DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree t, Object value, boolean selected,
                                                          boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(t, value, selected, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof FactNode fact) {
                    setText(labelOf(fact));
                }
                return this;
            }
        });
        JScrollPane scroll = new JScrollPane(tree);
        scroll.setPreferredSize(new Dimension(100, 160));
        return scroll;
    }

    private String labelOf(FactNode node) {
        if (node.getKind() == FactNodeKind.SUBTITLE) {
            return "▍ " + abbreviate(node.getContent());
        }
        String number = DemandNumbering.factNumbers(section.getFacts()).getOrDefault(node.getId(), "?");
        return sectionPrefix() + "." + number + ". " + abbreviate(node.getContent());
    }

    /** Número romano de esta sección, igual que en el documento final. */
    private String sectionPrefix() {
        int counter = 0;
        for (SectionSpec candidate : type.sections()) {
            if (candidate.romanNumbered()) {
                counter++;
            }
            if (candidate.id().equals(spec.id())) {
                break;
            }
        }
        return DemandNumbering.roman(counter);
    }

    private String abbreviate(String text) {
        String single = text == null ? "" : text.replace('\n', ' ').trim();
        return single.isEmpty() ? "(sin contenido)" : single;
    }

    private JComponent buildSouth() {
        JPanel south = new JPanel(new BorderLayout(0, 6));

        JPanel nodeButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton addSubfact = new JButton("+ Agregar subhecho");
        addSubfact.addActionListener(e ->
                listener.factAction(section.getId(), selectedNodeId, FactAction.ADD_SUBFACT, null, selectedTemplateId()));
        JButton up = new JButton("↑");
        up.addActionListener(e ->
                listener.factAction(section.getId(), parentOfSelection(), FactAction.MOVE_UP, selectedNodeId, null));
        JButton down = new JButton("↓");
        down.addActionListener(e ->
                listener.factAction(section.getId(), parentOfSelection(), FactAction.MOVE_DOWN, selectedNodeId, null));
        JButton remove = new JButton("Eliminar");
        remove.addActionListener(e ->
                listener.factAction(section.getId(), parentOfSelection(), FactAction.REMOVE, selectedNodeId, null));
        nodeButtons.add(addSubfact);
        nodeButtons.add(up);
        nodeButtons.add(down);
        nodeButtons.add(remove);
        south.add(nodeButtons, BorderLayout.NORTH);

        editorPanel = new JPanel(new BorderLayout(0, 4));
        south.add(editorPanel, BorderLayout.CENTER);
        south.add(buildClosing(), BorderLayout.SOUTH);
        return south;
    }

    private JComponent buildClosing() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Cierre / conclusión (opcional, sin número)"));
        closingArea = editableArea(section.getClosingContent());
        closingArea.setRows(3);
        closingArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyClosing();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyClosing();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyClosing();
            }
        });
        JButton regenerate = new JButton("Regenerar cierre desde los hechos");
        regenerate.addActionListener(e -> listener.regenerateClosing(section.getId()));
        JPanel top = new JPanel(new BorderLayout());
        top.add(regenerate, BorderLayout.WEST);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(closingArea), BorderLayout.CENTER);
        return panel;
    }

    private void notifyClosing() {
        if (applying) {
            return;
        }
        listener.closingEdited(section.getId(), closingArea.getText());
    }

    private void onSelection(TreeSelectionEvent event) {
        Object selected = tree.getLastSelectedPathComponent();
        if (selected instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof FactNode fact) {
            selectedNodeId = fact.getId();
        } else {
            selectedNodeId = null;
        }
        rebuildEditor();
    }

    private String selectedTemplateId() {
        if (spec.factTemplates().isEmpty()) {
            return null;
        }
        if (templateCombo == null) {
            return spec.factTemplates().get(0).id();
        }
        int index = templateCombo.getSelectedIndex();
        return spec.factTemplates().get(Math.max(0, index)).id();
    }

    private String parentOfSelection() {
        return selectedNodeId;
    }

    @Override
    public void rebuild() {
        rebuildTree();
        rebuildEditor();
    }

    private void rebuildTree() {
        Map<String, String> numbers = DemandNumbering.factNumbers(section.getFacts());
        treeRoot = new DefaultMutableTreeNode("Fundamentos fácticos");
        for (FactNode node : section.getFacts()) {
            treeRoot.add(toTreeNode(node, numbers));
        }
        treeModel = new DefaultTreeModel(treeRoot);
        tree.setModel(treeModel);
        if (!treeInitialized) {
            tree.expandRow(0);
            treeInitialized = true;
        }
        if (selectedNodeId != null) {
            DefaultMutableTreeNode found = findTreeNode(treeRoot, selectedNodeId);
            if (found != null) {
                tree.setSelectionPath(new TreePath(found.getPath()));
            } else {
                selectedNodeId = null;
            }
        }
    }

    private DefaultMutableTreeNode toTreeNode(FactNode node, Map<String, String> numbers) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
        for (FactNode child : node.getChildren()) {
            treeNode.add(toTreeNode(child, numbers));
        }
        return treeNode;
    }

    private DefaultMutableTreeNode findTreeNode(DefaultMutableTreeNode parent, String nodeId) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (child.getUserObject() instanceof FactNode node && node.getId().equals(nodeId)) {
                return child;
            }
            DefaultMutableTreeNode found = findTreeNode(child, nodeId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void rebuildEditor() {
        editorPanel.removeAll();
        factContentArea = null;
        FactNode node = selectedNodeId == null ? null : FactTreeOperations.find(section.getFacts(), selectedNodeId);
        if (node == null) {
            editorPanel.add(new JLabel("Seleccione un título, hecho o subhecho para editarlo."));
        } else if (node.getKind() == FactNodeKind.SUBTITLE) {
            editorPanel.add(buildSubtitleEditor(node));
        } else {
            editorPanel.add(buildFactEditor(node));
        }
        editorPanel.revalidate();
        editorPanel.repaint();
    }

    private JComponent buildSubtitleEditor(FactNode node) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(new JLabel("Título / subtítulo:"), BorderLayout.NORTH);
        JTextArea area = editableArea(node.getContent());
        area.setRows(2);
        addFactListener(node, area);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildFactEditor(FactNode node) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        FactTemplate template = spec.factTemplate(node.getTemplateId());

        if (!template.inputs().isEmpty()) {
            JPanel inputs = new JPanel(new java.awt.GridBagLayout());
            java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
            c.insets = new java.awt.Insets(2, 4, 2, 4);
            c.anchor = java.awt.GridBagConstraints.NORTHWEST;
            c.fill = java.awt.GridBagConstraints.HORIZONTAL;
            for (InputSpec input : template.inputs()) {
                c.gridx = 0;
                c.weightx = 0;
                inputs.add(new JLabel(input.label() + ":"), c);
                c.gridx = 1;
                c.weightx = 1;
                inputs.add(factInputField(node, input), c);
            }
            panel.add(inputs, BorderLayout.NORTH);
        }

        JPanel contentPanel = new JPanel(new BorderLayout(0, 4));
        JButton regenerate = new JButton("Regenerar desde datos");
        regenerate.addActionListener(e -> listener.regenerateFact(section.getId(), node.getId()));
        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("Contenido final del hecho:"), BorderLayout.WEST);
        header.add(regenerate, BorderLayout.EAST);

        factContentArea = editableArea(node.getContent());
        factContentArea.setRows(4);
        addFactListener(node, factContentArea);

        contentPanel.add(header, BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(factContentArea), BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    private JComponent factInputField(FactNode node, InputSpec input) {
        if (input.multiline()) {
            JTextArea area = new JTextArea(2, 24);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setText(node.getInputs().getOrDefault(input.key(), ""));
            area.setBorder(BorderFactory.createEtchedBorder());
            area.getDocument().addDocumentListener(factInputListener(node, input, area::getText));
            return new JScrollPane(area);
        }
        JTextField field = new JTextField(24);
        field.setText(node.getInputs().getOrDefault(input.key(), ""));
        field.getDocument().addDocumentListener(factInputListener(node, input, field::getText));
        return field;
    }

    private DocumentListener factInputListener(FactNode node, InputSpec input, java.util.function.Supplier<String> value) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyFactInput(node, input, value.get());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyFactInput(node, input, value.get());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyFactInput(node, input, value.get());
            }
        };
    }

    private void notifyFactInput(FactNode node, InputSpec input, String value) {
        if (applying) {
            return;
        }
        listener.factInputChanged(section.getId(), node.getId(), input.key(), value);
    }

    private void addFactListener(FactNode node, JTextArea area) {
        area.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyFactContent(node, area.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyFactContent(node, area.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyFactContent(node, area.getText());
            }
        });
    }

    private void notifyFactContent(FactNode node, String text) {
        if (applying) {
            return;
        }
        listener.factContentEdited(section.getId(), node.getId(), text);
    }

    @Override
    public void setClosingText(String text) {
        if (closingArea == null || applying) {
            return;
        }
        applying = true;
        closingArea.setText(text);
        closingArea.setCaretPosition(0);
        applying = false;
    }

    @Override
    public void setFactContent(String nodeId, String text) {
        if (factContentArea == null || applying || !nodeId.equals(selectedNodeId)) {
            return;
        }
        applying = true;
        factContentArea.setText(text);
        factContentArea.setCaretPosition(0);
        applying = false;
    }
}
