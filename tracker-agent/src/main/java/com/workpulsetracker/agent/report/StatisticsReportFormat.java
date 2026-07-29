package com.workpulsetracker.agent.report;

/**
 * Формат файла отчёта статистики.
 */
public enum StatisticsReportFormat {

    EXCEL("xlsx"),
    PDF("pdf");

    private final String fileExtension;

    StatisticsReportFormat(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
