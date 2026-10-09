package com.fidelg.documentgenerator.preview;

import com.fidelg.documentgenerator.domain.DocumentBlock;
import com.fidelg.documentgenerator.domain.DocumentRun;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reparte los párrafos del documento en líneas de pantalla, respetando runs con
 * estilos mixtos, sangrías y espaciado entre párrafos.
 */
public final class DocumentLayout {

    public static final int PAGE_WIDTH = 794;
    public static final int PAGE_HEIGHT = 1123;
    public static final int MARGIN = 96;

    private static final int POINTS_PER_INCH = 72;
    private static final int PIXELS_PER_INCH = 96;
    private static final double PIXELS_PER_TWIP = 96.0 / 1440.0;
    private static final Map<String, String> FONT_CACHE = new ConcurrentHashMap<>();

    /** Un fragmento de línea con su propia fuente y posición. */
    public record Segment(Font font, String text, int x) {
    }

    public record Line(List<Segment> segments, int x, int baselineY) {

        public String text() {
            StringBuilder builder = new StringBuilder();
            for (Segment segment : segments) {
                builder.append(segment.text());
            }
            return builder.toString();
        }

        public Font font() {
            return segments.isEmpty() ? defaultFont() : segments.get(0).font();
        }

        private static Font defaultFont() {
            return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
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
            int indent = (int) Math.round(block.leftIndent() * PIXELS_PER_TWIP);
            Font fallback = fontOf(block.runs().isEmpty() ? null : block.runs().get(0), block);
            for (List<PendingSegment> pendingLine : wrap(block, textWidth - indent, graphics, fallback)) {
                int height = lineHeight(pendingLine, fallback, graphics);
                if (cursor + height > PAGE_HEIGHT - MARGIN) {
                    pages.add(new Page(List.copyOf(lines)));
                    lines = new ArrayList<>();
                    cursor = MARGIN;
                }
                lines.add(buildLine(pendingLine, block, indent, textWidth, cursor, graphics));
                cursor += height;
            }
            cursor += (int) Math.round(block.spaceAfter() * PIXELS_PER_TWIP);
        }
        pages.add(new Page(List.copyOf(lines)));
        return pages;
    }

    private static List<List<PendingSegment>> wrap(DocumentBlock block, int maxWidth, Graphics2D graphics,
                                                   Font fallback) {
        List<List<PendingSegment>> result = new ArrayList<>();
        Accumulator accumulator = new Accumulator(maxWidth, result);
        for (DocumentRun run : block.runs()) {
            Font font = fontOf(run, block);
            FontMetrics metrics = graphics.getFontMetrics(font);
            String[] fragments = run.text().split("\n", -1);
            for (int i = 0; i < fragments.length; i++) {
                if (i > 0) {
                    accumulator.newline();
                }
                appendWords(accumulator, font, metrics, fragments[i]);
            }
        }
        accumulator.finish();
        return result;
    }

    private static void appendWords(Accumulator accumulator, Font font, FontMetrics metrics, String fragment) {
        int index = 0;
        while (index < fragment.length()) {
            while (index < fragment.length() && fragment.charAt(index) == ' ') {
                index++;
            }
            if (index >= fragment.length()) {
                return;
            }
            int end = index;
            while (end < fragment.length() && fragment.charAt(end) != ' ') {
                end++;
            }
            accumulator.append(font, metrics, fragment.substring(index, end));
            index = end;
        }
    }

    private static Line buildLine(List<PendingSegment> pending, DocumentBlock block, int indent,
                                  int textWidth, int cursorY, Graphics2D graphics) {
        int width = 0;
        for (PendingSegment segment : pending) {
            width += segment.metrics.stringWidth(segment.text());
        }
        int baseX = MARGIN + indent;
        if (block.alignment() == DocumentBlock.Alignment.CENTER) {
            baseX = MARGIN + Math.max(0, (textWidth - width) / 2);
        }
        List<Segment> segments = new ArrayList<>();
        int offset = 0;
        for (PendingSegment segment : pending) {
            String text = segment.text();
            segments.add(new Segment(segment.font, text, baseX + offset));
            offset += segment.metrics.stringWidth(text);
        }
        int ascent = graphics.getFontMetrics(
                block.runs().isEmpty() ? fallbackFont(block) : fontOf(block.runs().get(0), block)).getAscent();
        return new Line(segments, baseX, cursorY + ascent);
    }

    private static int lineHeight(List<PendingSegment> line, Font fallback, Graphics2D graphics) {
        int height = graphics.getFontMetrics(fallback).getHeight();
        for (PendingSegment segment : line) {
            height = Math.max(height, segment.metrics.getHeight());
        }
        return height;
    }

    private static Font fontOf(DocumentRun run, DocumentBlock block) {
        int style = Font.PLAIN;
        if (run != null && run.bold()) {
            style |= Font.BOLD;
        }
        if (run != null && run.italic()) {
            style |= Font.ITALIC;
        }
        int size = (int) Math.round(block.fontSize() * (double) PIXELS_PER_INCH / POINTS_PER_INCH);
        return new Font(resolvedFont(block.fontName()), style, size);
    }

    private static Font fallbackFont(DocumentBlock block) {
        return fontOf(null, block);
    }

    /**
     * Bookman Old Style no siempre está instalada en el sistema: se resuelve una
     * sola vez y se cachea, porque consultar las familias disponibles es caro.
     */
    private static String resolvedFont(String name) {
        Map<String, String> cache = FONT_CACHE;
        String resolved = cache.get(name);
        if (resolved == null) {
            resolved = List.of(GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames()).contains(name) ? name : Font.SERIF;
            cache.put(name, resolved);
        }
        return resolved;
    }

    /** Segmento en construcción: fuente, métricas y texto acumulado. */
    private static final class PendingSegment {
        private final Font font;
        private final FontMetrics metrics;
        private final StringBuilder text = new StringBuilder();

        private PendingSegment(Font font, FontMetrics metrics) {
            this.font = font;
            this.metrics = metrics;
        }

        private String text() {
            return text.toString();
        }
    }

    /** Acumula los segmentos de un párrafo agrupándolos en líneas. */
    private static final class Accumulator {
        private final int maxWidth;
        private final List<List<PendingSegment>> lines;
        private final List<PendingSegment> current = new ArrayList<>();
        private int width;

        private Accumulator(int maxWidth, List<List<PendingSegment>> lines) {
            this.maxWidth = maxWidth;
            this.lines = lines;
        }

        private void append(Font font, FontMetrics metrics, String word) {
            if (word.isEmpty()) {
                return;
            }
            String prefix = needsSeparator() ? " " : "";
            int wordWidth = metrics.stringWidth(prefix + word);
            if (width + wordWidth <= maxWidth) {
                addText(font, metrics, prefix + word, wordWidth);
                return;
            }
            if (current.isEmpty()) {
                // Palabra más larga que la línea: se parte como en el original.
                int end = 1;
                while (end < word.length() && metrics.stringWidth(word.substring(0, end + 1)) <= maxWidth) {
                    end++;
                }
                addText(font, metrics, word.substring(0, end), metrics.stringWidth(word.substring(0, end)));
                newline();
                append(font, metrics, word.substring(end));
                return;
            }
            newline();
            append(font, metrics, word);
        }

        private void addText(Font font, FontMetrics metrics, String text, int textWidth) {
            PendingSegment segment = current.isEmpty() ? null : current.get(current.size() - 1);
            if (segment == null || segment.font != font) {
                segment = new PendingSegment(font, metrics);
                current.add(segment);
            }
            segment.text.append(text);
            width += textWidth;
        }

        private boolean needsSeparator() {
            return !current.isEmpty() && !current.get(current.size() - 1).text().isEmpty();
        }

        private void newline() {
            lines.add(List.copyOf(current));
            current.clear();
            width = 0;
        }

        private void finish() {
            newline();
        }
    }
}
