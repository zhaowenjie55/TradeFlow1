package com.globalvibe.arbitrage.domain.report.dto;

public record ReportDownloadDocumentVO(
        String fileName,
        String mimeType,
        String content
) {
}
