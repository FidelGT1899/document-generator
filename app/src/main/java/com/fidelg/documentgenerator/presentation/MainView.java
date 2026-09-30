package com.fidelg.documentgenerator.presentation;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Path;

public class MainView extends JFrame {
    private final JTextField titleField = new JTextField(30);
    private final JTextField authorField = new JTextField(30);
    private final JTextArea bodyArea = new JTextArea(10, 30);
    private final JButton generateButton = new JButton("Generar documento");

    private MainViewListener listener;

    public MainView() {
        super("Generador de documentos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        add(buildForm(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
        setSize(520, 420);
        setLocationRelativeTo(null);
    }

    public void setListener(MainViewListener listener) {
        this.listener = listener;
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

    private void generateDocument() {
        if (listener == null) {
            return;
        }
        Path outputFile = chooseOutputFile();
        if (outputFile != null) {
            listener.generateDocument(new DocumentFormData(
                    titleField.getText(), authorField.getText(), bodyArea.getText()), outputFile);
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