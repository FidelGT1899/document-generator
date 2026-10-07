package com.fidelg.documentgenerator.presentation;

import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.preview.PreviewPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class MainView extends JFrame {
    private final JTextField titleField = new JTextField(30);
    private final JTextField authorField = new JTextField(30);
    private final JTextArea bodyArea = new JTextArea(10, 30);
    private final JButton generateButton = new JButton("Generar documento");
    private final PreviewPanel previewPanel = new PreviewPanel();

    private MainViewListener listener;

    public MainView() {
        super("Generador de documentos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        add(buildContent(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
        listenForFormChanges();
        setSize(1360, 850);
        setLocationRelativeTo(null);
    }

    public void setListener(MainViewListener listener) {
        this.listener = listener;
    }

    public void showPreview(List<DocumentBlock> blocks) {
        previewPanel.setDocument(blocks);
    }

    public void setBusy(boolean busy) {
        generateButton.setEnabled(!busy);
        generateButton.setText(busy ? "Generando..." : "Generar documento");
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showSuccess(Path outputFile) {
        JOptionPane.showMessageDialog(this, "Documento generado en: " + outputFile,
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
    }

    private JSplitPane buildContent() {
        JPanel form = buildForm();
        JScrollPane preview = new JScrollPane(previewPanel);
        preview.setBorder(BorderFactory.createTitledBorder("Vista previa"));
        preview.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, form, preview);
        split.setResizeWeight(0.35);
        split.setContinuousLayout(true);
        split.setDividerLocation(form.getPreferredSize().width + 12);
        return split;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel("Título:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        form.add(titleField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        form.add(new JLabel("Autor:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        form.add(authorField, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.0;
        form.add(new JLabel("Cuerpo:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        form.add(new JScrollPane(bodyArea), c);

        return form;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        generateButton.addActionListener(e -> generateDocument());
        panel.add(generateButton);
        return panel;
    }

    private void listenForFormChanges() {
        DocumentListener changes = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyFormChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyFormChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyFormChanged();
            }
        };
        titleField.getDocument().addDocumentListener(changes);
        authorField.getDocument().addDocumentListener(changes);
        bodyArea.getDocument().addDocumentListener(changes);
    }

    private void notifyFormChanged() {
        if (listener != null) {
            listener.formChanged(currentForm());
        }
    }

    private DocumentFormData currentForm() {
        return new DocumentFormData(titleField.getText(), authorField.getText(), bodyArea.getText());
    }

    private void generateDocument() {
        if (listener == null) {
            return;
        }
        Path outputFile = chooseOutputFile();
        if (outputFile != null) {
            listener.generateDocument(currentForm(), outputFile);
        }
    }

    private Path chooseOutputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar documento");
        chooser.setSelectedFile(new File("documento.docx"));
        chooser.setFileFilter(new FileNameExtensionFilter("Documento Word (*.docx)", "docx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        Path path = chooser.getSelectedFile().toPath();
        if (!path.getFileName().toString().toLowerCase().endsWith(".docx")) {
            path = path.resolveSibling(path.getFileName() + ".docx");
        }
        return path;
    }
}
