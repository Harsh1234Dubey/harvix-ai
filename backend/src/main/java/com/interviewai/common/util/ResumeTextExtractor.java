package com.interviewai.common.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Extracts plain text from stored resume files. Supports PDF and plain text;
 * returns an empty string for formats that cannot be read so callers can fall
 * back gracefully.
 */
public final class ResumeTextExtractor {

    private ResumeTextExtractor() {
    }

    public static String extract(Path path) throws IOException {
        if (path == null) {
            return "";
        }
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".pdf")) {
            return extractPdf(path);
        }
        if (name.endsWith(".txt") || name.endsWith(".md")) {
            return Files.readString(path);
        }
        return "";
    }

    private static String extractPdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }
}
