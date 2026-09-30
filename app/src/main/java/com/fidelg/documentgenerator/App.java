package com.fidelg.documentgenerator;

import com.fidelg.documentgenerator.application.GenerateDocumentService;
import com.fidelg.documentgenerator.infrastructure.WordDocumentGenerator;
import com.fidelg.documentgenerator.presentation.MainPresenter;
import com.fidelg.documentgenerator.presentation.MainView;

import javax.swing.SwingUtilities;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::start);
    }

    private static void start() {
        MainView view = new MainView();
        new MainPresenter(view, new GenerateDocumentService(new WordDocumentGenerator()));
        view.setVisible(true);
    }
}