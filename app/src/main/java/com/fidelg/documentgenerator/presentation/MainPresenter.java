package com.fidelg.documentgenerator.presentation;

import com.fidelg.documentgenerator.application.GenerateDocumentService;
import com.fidelg.documentgenerator.domain.DocumentBlocks;
import com.fidelg.documentgenerator.domain.DocumentRequest;

import javax.swing.SwingWorker;
import java.nio.file.Path;

public class MainPresenter implements MainViewListener {
    private final MainView view;
    private final GenerateDocumentService service;

    public MainPresenter(MainView view, GenerateDocumentService service) {
        this.view = view;
        this.service = service;
        this.view.setListener(this);
    }

    @Override
    public void formChanged(DocumentFormData form) {
        view.showPreview(DocumentBlocks.from(form.title(), form.author(), form.body()));
    }

    @Override
    public void generateDocument(DocumentFormData form, Path outputFile) {
        DocumentRequest request;
        try {
            request = new DocumentRequest(form.title(), form.author(), form.body());
        } catch (IllegalArgumentException e) {
            view.showError(e.getMessage());
            return;
        }

        view.setBusy(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.execute(request, outputFile);
                return null;
            }

            @Override
            protected void done() {
                view.setBusy(false);
                try {
                    get();
                    view.showSuccess(outputFile);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    view.showError("No se pudo generar el documento: " + cause.getMessage());
                }
            }
        }.execute();
    }
}