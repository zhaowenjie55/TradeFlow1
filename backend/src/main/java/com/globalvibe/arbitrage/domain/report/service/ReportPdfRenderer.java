package com.globalvibe.arbitrage.domain.report.service;

import com.globalvibe.arbitrage.domain.report.dto.ReportDownloadDocumentVO;
import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class ReportPdfRenderer {

    private static final float FONT_SIZE = 11f;
    private static final float LEADING = 16f;
    private static final float PAGE_MARGIN = 42f;
    private static final List<Path> FONT_CANDIDATES = List.of(
            Path.of("/Library/Fonts/Arial Unicode.ttf"),
            Path.of("/System/Library/Fonts/Supplemental/Arial Unicode.ttf"),
            Path.of("/System/Library/Fonts/Supplemental/NISC18030.ttf"),
            Path.of("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"),
            Path.of("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc")
    );

    public ReportDownloadDocumentVO render(ArbitrageReport report, String reportMarkdown) {
        byte[] bytes = renderPdfBytes(reportMarkdown == null || reportMarkdown.isBlank() ? buildFallbackMarkdown(report) : reportMarkdown);
        return new ReportDownloadDocumentVO(
                sanitizeFileName(report.title()) + "-agent-report.pdf",
                "application/pdf",
                Base64.getEncoder().encodeToString(bytes)
        );
    }

    private byte[] renderPdfBytes(String markdown) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFont font = loadFont(document);
            PageWriter writer = new PageWriter(document, font);
            for (String line : normalizeMarkdown(markdown)) {
                writer.writeParagraph(line);
            }
            writer.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to render PDF report", exception);
        }
    }

    private PDFont loadFont(PDDocument document) throws IOException {
        for (Path candidate : FONT_CANDIDATES) {
            if (Files.exists(candidate)) {
                try {
                    return PDType0Font.load(document, Files.newInputStream(candidate));
                } catch (IOException ignored) {
                    // Some system Chinese fonts are TTC/bitmap hybrids that PDFBox cannot embed directly.
                    // Continue searching until we find a valid embeddable font on the host.
                }
            }
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private List<String> normalizeMarkdown(String markdown) {
        List<String> lines = new ArrayList<>();
        for (String rawLine : markdown.replace("\r\n", "\n").split("\n")) {
            String line = rawLine
                    .replaceAll("^#{1,6}\\s*", "")
                    .replaceAll("^[-*]\\s*", "• ")
                    .replaceAll("\\|", " ")
                    .trim();
            lines.add(line);
        }
        return lines;
    }

    private String buildFallbackMarkdown(ArbitrageReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(report.title()).append("\n\n");
        builder.append("决策: ").append(report.decision()).append("\n");
        builder.append("风险等级: ").append(report.riskLevel()).append("\n");
        builder.append("预计利润率: ").append(report.expectedMargin()).append("%\n");
        if (report.costBreakdown() != null) {
            builder.append("预计利润: ").append(currency(report.costBreakdown().estimatedProfit())).append("\n");
        }
        if (report.recommendations() != null && !report.recommendations().isEmpty()) {
            builder.append("\n建议:\n");
            report.recommendations().forEach(item -> builder.append("- ").append(item).append("\n"));
        }
        return builder.toString();
    }

    private String sanitizeFileName(String value) {
        return value == null
                ? "tradeflow-report"
                : value.trim()
                .replaceAll("[<>:\"/\\\\|?*\\x00-\\x1F]+", "-")
                .replaceAll("\\s+", "-")
                .toLowerCase();
    }

    private String currency(BigDecimal value) {
        if (value == null) {
            return "¥0.00";
        }
        return "¥" + value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static final class PageWriter {
        private final PDDocument document;
        private final PDFont font;
        private PDPage page;
        private PDPageContentStream stream;
        private float cursorY;

        private PageWriter(PDDocument document, PDFont font) throws IOException {
            this.document = document;
            this.font = font;
            startNewPage();
        }

        private void writeParagraph(String paragraph) throws IOException {
            if (paragraph == null || paragraph.isBlank()) {
                ensureSpace(LEADING);
                stream.newLineAtOffset(0, -LEADING);
                cursorY -= LEADING;
                return;
            }
            for (String line : wrap(paragraph)) {
                ensureSpace(LEADING);
                stream.showText(sanitize(line));
                stream.newLineAtOffset(0, -LEADING);
                cursorY -= LEADING;
            }
        }

        private List<String> wrap(String text) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            float maxWidth = PDRectangle.A4.getWidth() - (PAGE_MARGIN * 2);
            for (char ch : text.toCharArray()) {
                String next = current.toString() + ch;
                float width = font.getStringWidth(next) / 1000 * FONT_SIZE;
                if (width > maxWidth && current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder().append(ch);
                } else {
                    current.append(ch);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines;
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (cursorY - requiredHeight < PAGE_MARGIN) {
                startNewPage();
            }
        }

        private void startNewPage() throws IOException {
            if (stream != null) {
                stream.endText();
                stream.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            stream.beginText();
            stream.setFont(font, FONT_SIZE);
            cursorY = page.getMediaBox().getHeight() - PAGE_MARGIN;
            stream.newLineAtOffset(PAGE_MARGIN, cursorY);
        }

        private void close() throws IOException {
            if (stream != null) {
                stream.endText();
                stream.close();
            }
        }

        private String sanitize(String value) {
            return value.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
        }
    }
}
