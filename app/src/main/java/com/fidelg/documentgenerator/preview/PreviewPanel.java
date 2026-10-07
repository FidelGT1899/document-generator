package com.fidelg.documentgenerator.preview;

import com.fidelg.documentgenerator.domain.DocumentBlock;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

public final class PreviewPanel extends JPanel {

    private static final int PAGE_GAP = 24;
    private static final int PADDING = 12;
    private static final Color BACKGROUND = new Color(0xE4, 0xE6, 0xE9);
    private static final Color PAGE_FILL = Color.WHITE;
    private static final Color PAGE_BORDER = new Color(0xC4, 0xC7, 0xCB);
    private static final Color PAGE_SHADOW = new Color(0, 0, 0, 40);
    private static final Color HINT = new Color(0x9A, 0x9E, 0xA3);

    private DocumentLayout layout = DocumentLayout.of(List.of());

    public PreviewPanel() {
        setBackground(BACKGROUND);
    }

    public void setDocument(List<DocumentBlock> blocks) {
        layout = DocumentLayout.of(blocks);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int pages = Math.max(1, layout.pages().size());
        return new Dimension(
                DocumentLayout.PAGE_WIDTH + 2 * PADDING,
                pages * DocumentLayout.PAGE_HEIGHT + (pages - 1) * PAGE_GAP + 2 * PADDING);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics = (Graphics2D) g.create();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int x = Math.max(PADDING, (getWidth() - DocumentLayout.PAGE_WIDTH) / 2);
            int y = PADDING;
            for (DocumentLayout.Page page : layout.pages()) {
                paintPage(graphics, page, x, y);
                y += DocumentLayout.PAGE_HEIGHT + PAGE_GAP;
            }
            if (isEmptyDocument()) {
                paintHint(graphics, x, PADDING);
            }
        } finally {
            graphics.dispose();
        }
    }

    private void paintPage(Graphics2D graphics, DocumentLayout.Page page, int x, int y) {
        graphics.setColor(PAGE_SHADOW);
        graphics.fillRect(x + 5, y + 5, DocumentLayout.PAGE_WIDTH, DocumentLayout.PAGE_HEIGHT);
        graphics.setColor(PAGE_FILL);
        graphics.fillRect(x, y, DocumentLayout.PAGE_WIDTH, DocumentLayout.PAGE_HEIGHT);
        graphics.setColor(PAGE_BORDER);
        graphics.drawRect(x, y, DocumentLayout.PAGE_WIDTH, DocumentLayout.PAGE_HEIGHT);
        for (DocumentLayout.Line line : page.lines()) {
            graphics.setFont(line.font());
            graphics.setColor(Color.BLACK);
            graphics.drawString(line.text(), x + line.x(), y + line.baselineY());
        }
    }

    private void paintHint(Graphics2D graphics, int pageX, int pageY) {
        Font font = new Font(Font.SANS_SERIF, Font.ITALIC, 16);
        graphics.setFont(font);
        graphics.setColor(HINT);
        String hint = "La vista previa se actualizará a medida que escribas.";
        FontMetrics metrics = graphics.getFontMetrics(font);
        int x = pageX + (DocumentLayout.PAGE_WIDTH - metrics.stringWidth(hint)) / 2;
        int y = pageY + DocumentLayout.PAGE_HEIGHT / 2;
        graphics.drawString(hint, x, y);
    }

    private boolean isEmptyDocument() {
        for (DocumentLayout.Page page : layout.pages()) {
            for (DocumentLayout.Line line : page.lines()) {
                if (!line.text().isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }
}
