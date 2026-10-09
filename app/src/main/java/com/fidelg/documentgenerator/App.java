package com.fidelg.documentgenerator;

import com.fidelg.documentgenerator.application.ContentGenerationService;
import com.fidelg.documentgenerator.application.CreateDemandService;
import com.fidelg.documentgenerator.application.DraftService;
import com.fidelg.documentgenerator.application.GenerateDocumentService;
import com.fidelg.documentgenerator.domain.type.AlimentosDemandType;
import com.fidelg.documentgenerator.domain.type.DocumentTypeRegistry;
import com.fidelg.documentgenerator.infrastructure.JsonDraftStore;
import com.fidelg.documentgenerator.infrastructure.WordDocumentGenerator;
import com.fidelg.documentgenerator.presentation.MainPresenter;
import com.fidelg.documentgenerator.presentation.MainView;

import javax.swing.SwingUtilities;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::start);
    }

    private static void start() {
        DocumentTypeRegistry registry = new DocumentTypeRegistry();
        registry.register(new AlimentosDemandType());

        MainView view = new MainView();
        new MainPresenter(
                view,
                registry,
                new CreateDemandService(registry),
                new ContentGenerationService(),
                new DraftService(new JsonDraftStore(), registry),
                new GenerateDocumentService(new WordDocumentGenerator()),
                AlimentosDemandType.ID);
        view.setVisible(true);
    }
}
