package com.fidelg.documentgenerator.presentation;

import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.domain.demand.DemandDocument;
import com.fidelg.documentgenerator.domain.type.DocumentType;
import com.fidelg.documentgenerator.preview.PreviewPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Ventana principal: editor de la demanda a la izquierda, vista previa del
 * documento final a la derecha. Solo captura interacción y delega en el
 * presentador.
 */
public class MainView extends JFrame {

    private final DemandEditorPanel editorPanel = new DemandEditorPanel();
    private final PreviewPanel previewPanel = new PreviewPanel();
    private final JButton newButton = new JButton("Nuevo");
    private final JButton saveButton = new JButton("Guardar borrador");
    private final JButton loadButton = new JButton("Cargar borrador");
    private final JButton generateButton = new JButton("Generar DOCX");

    private MainViewListener listener;

    public MainView() {
        super("Generador de demandas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        add(buildContent(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
        wireButtons();
        setSize(1400, 900);
        setLocationRelativeTo(null);
    }

    public void setListener(MainViewListener listener) {
        this.listener = listener;
        editorPanel.setListener(listener);
    }

    public void showDocument(DocumentType type, DemandDocument document) {
        editorPanel.showDocument(type, document);
    }

    public void updateContent(String sectionId, String text) {
        editorPanel.updateContent(sectionId, text);
    }

    public void updateClosing(String sectionId, String text) {
        editorPanel.updateClosing(sectionId, text);
    }

    public void updateFactContent(String sectionId, String nodeId, String text) {
        editorPanel.updateFactContent(sectionId, nodeId, text);
    }

    public void refreshSection(String sectionId) {
        editorPanel.refreshSection(sectionId);
    }

    public void showPreview(List<DocumentBlock> blocks) {
        previewPanel.setDocument(blocks);
    }

    public void setBusy(boolean busy) {
        generateButton.setEnabled(!busy);
        generateButton.setText(busy ? "Generando..." : "Generar DOCX");
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showSuccess(Path outputFile) {
        JOptionPane.showMessageDialog(this, "Documento generado en: " + outputFile,
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void wireButtons() {
        newButton.addActionListener(e -> {
            if (listener != null) {
                listener.newDocument();
            }
        });
        saveButton.addActionListener(e -> {
            Path file = chooseFile("Guardar borrador", "Borrador de demanda", "json", true);
            if (file != null && listener != null) {
                listener.saveDraft(file);
            }
        });
        loadButton.addActionListener(e -> {
            Path file = chooseFile("Cargar borrador", "Borrador de demanda", "json", false);
            if (file != null && listener != null) {
                listener.loadDraft(file);
            }
        });
        generateButton.addActionListener(e -> {
            Path file = chooseFile("Guardar documento", "Documento Word", "docx", true);
            if (file != null && listener != null) {
                listener.generateDocument(file);
            }
        });
    }

    private Path chooseFile(String title, String description, String extension, boolean save) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter(description + " (*." + extension + ")", extension));
        if (save) {
            chooser.setSelectedFile(new File("demanda." + extension));
        }
        int result = save ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        Path path = chooser.getSelectedFile().toPath();
        if (save && !path.getFileName().toString().toLowerCase().endsWith("." + extension)) {
            path = path.resolveSibling(path.getFileName() + "." + extension);
        }
        return path;
    }

    private JSplitPane buildContent() {
        JScrollPane editor = new JScrollPane(editorPanel);
        editor.setBorder(BorderFactory.createTitledBorder("Datos de la demanda"));
        editor.getVerticalScrollBar().setUnitIncrement(16);
        editor.setPreferredSize(new Dimension(560, 800));

        JScrollPane preview = new JScrollPane(previewPanel);
        preview.setBorder(BorderFactory.createTitledBorder("Vista previa del documento final"));
        preview.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editor, preview);
        split.setResizeWeight(0.45);
        split.setContinuousLayout(true);
        split.setDividerLocation(600);
        return split;
    }

    private javax.swing.JPanel buildButtonPanel() {
        javax.swing.JPanel panel = new javax.swing.JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        panel.add(newButton);
        panel.add(loadButton);
        panel.add(saveButton);
        panel.add(generateButton);
        return panel;
    }
}
