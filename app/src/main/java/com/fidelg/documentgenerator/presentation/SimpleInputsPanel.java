package com.fidelg.documentgenerator.presentation;

import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.type.DocumentType;
import com.fidelg.documentgenerator.domain.type.SectionSpec;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Sección de solo datos estructurados (sin contenido editable), como la
 * cabecera del cuaderno principal.
 */
public class SimpleInputsPanel extends SectionPanel {

    public SimpleInputsPanel(SectionSpec spec, Section section, DocumentType type, MainViewListener listener) {
        super(spec, section, type, listener);
    }

    @Override
    protected JComponent buildBody() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(buildInputRows(), BorderLayout.NORTH);
        return wrapper;
    }
}
