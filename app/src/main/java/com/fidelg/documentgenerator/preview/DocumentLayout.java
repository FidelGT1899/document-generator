package com.fidelg.documentgenerator.preview;

import com.fidelg.documentgenerator.domain.DocumentBlock;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class DocumentLayout {

    public static final int PAGE_WIDTH = 794;
    public static final int PAGE_HEIGHT = 1123;
    public static final int MARGIN = 96;

    private static final int POINTS_PER_INCH = 72;
    private static final int PIXELS_PER_INCH = 96;

    public record Line(Font font, String text, int x, int baselineY) {
    }

    public record Page(List<Line> lines) {
    }

    private final List<Page> pages;

    private DocumentLayout(List<Page> pages) {
        this.pages = List.copyOf(pages);
    }

    public static DocumentLayout of(List<DocumentBlock> blocks) {
        BufferedImage canvas = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            return new DocumentLayout(layout(blocks, graphics));
        } finally {
            graphics.dispose();
        }
    }

    public List<Page> pages() {
        return pages;
    }

    private static List<Page> layout(List<DocumentBlock> blocks, Graphics2D graphics) {
        List<Page> pages = new ArrayList<>();
        List<Line> lines = new ArrayList<>();
        int textWidth = PAGE_WIDTH - 2 * MARGIN;
        int cursor = MARGIN;

        for (DocumentBlock block : blocks) {
            Font font = fontOf(block);
            FontMetrics metrics = graphics.getFontMetrics(font);
            for (String text : wrap(block.text(), metrics, textWidth)) {
                if (cursor + metrics.getHeight() > PAGE_HEIGHT - MARGIN) {
                    pages.add(new Page(List.copyOf(lines)));
                    lines = new ArrayList<>();
                    cursor = MARGIN;
                }
                lines.add(new Line(font, text, alignX(block, metrics, text, textWidth), cursor + metrics.getAscent()));
                cursor += metrics.getHeight();
            }
        }
        pages.add(new Page(List.copyOf(lines)));
        return pages;
    }

    private static List<String> wrap(String text, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n", -1)) {
            wrapLine(line, metrics, maxWidth, lines);
        }
        return lines;
    }

    private static void wrapLine(String line, FontMetrics metrics, int maxWidth, List<String> lines) {
        if (line.isEmpty()) {
            lines.add("");
            return;
        }
        int start = 0;
        while (start < line.length()) {
            int end = start + 1;
            while (end < line.length() && metrics.stringWidth(line.substring(start, end + 1)) <= maxWidth) {
                end++;
            }
            if (end >= line.length()) {
                lines.add(line.substring(start));
                return;
            }
            int breakAt = line.lastIndexOf(' ', end);
            if (breakAt <= start) {
                lines.add(line.substring(start, end));
                start = end;
            } else {
                lines.add(line.substring(start, breakAt));
                start = breakAt + 1;
                while (start < line.length() && line.charAt(start) == ' ') {
                    start++;
                }
            }
        }
    }

    private static int alignX(DocumentBlock block, FontMetrics metrics, String text, int textWidth) {
        if (block.alignment() == DocumentBlock.Alignment.CENTER) {
            return MARGIN + Math.max(0, (textWidth - metrics.stringWidth(text)) / 2);
        }
        return MARGIN;
    }

    private static Font fontOf(DocumentBlock block) {
        int style = Font.PLAIN;
        if (block.bold()) {
            style |= Font.BOLD;
        }
        if (block.italic()) {
            style |= Font.ITALIC;
        }
        int size = (int) Math.round(block.fontSize() * (double) PIXELS_PER_INCH / POINTS_PER_INCH);
        return new Font(Font.SANS_SERIF, style, size);
    }
}
