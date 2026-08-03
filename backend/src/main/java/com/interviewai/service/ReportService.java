package com.interviewai.service;

import com.interviewai.common.enums.ReportType;
import com.interviewai.common.response.PageResponse;
import com.interviewai.common.util.PageableUtils;
import com.interviewai.domain.Report;
import com.interviewai.domain.User;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.exception.ResourceNotFoundException;
import com.interviewai.repository.ReportRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    @Transactional
    public Report generate(ReportType type, String title, String scopeJson, String dataJson,
                           User generatedBy, User subjectUser, String format) {
        Report report = new Report();
        report.setReportType(type);
        report.setTitle(title);
        report.setScopeJson(scopeJson);
        report.setDataJson(dataJson);
        report.setGeneratedBy(generatedBy);
        report.setSubjectUser(subjectUser);
        report.setFormat(format);
        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public PageResponse<com.interviewai.dto.response.ReportMetaResponse> list(Long generatedById, int page, int size, String sort) {
        Pageable pageable = PageableUtils.build(page, size, sort);
        Page<Report> reports = generatedById != null
                ? reportRepository.findByGeneratedById(generatedById, pageable)
                : reportRepository.findAll(pageable);
        return PageResponse.from(reports, reports.stream()
                .map(com.interviewai.dto.response.ReportMetaResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public Report getByUuid(UUID uuid) {
        return reportRepository.findByUuid(uuid)
                .orElseThrow(() -> ResourceNotFoundException.of("Report", uuid));
    }

    public Resource renderPdf(Report report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            document.add(new Paragraph(report.getTitle(), titleFont));
            document.add(new Paragraph("InterView AI — " + report.getReportType().name() + " Report", bodyFont));
            document.add(new Paragraph("Generated: " + Instant.now(), bodyFont));
            if (report.getDataJson() != null) {
                document.add(new Paragraph(report.getDataJson(), bodyFont));
            }
            document.close();
        } catch (DocumentException ex) {
            log.error("PDF generation failed", ex);
            throw new IllegalStateException("PDF generation failed", ex);
        }
        return new ByteArrayResource(out.toByteArray());
    }

    public Resource renderCsv(Report report) {
        StringBuilder csv = new StringBuilder();
        csv.append("Report Type,Title,UUID,Generated At\n");
        csv.append(report.getReportType()).append(',')
                .append(sanitizeCsv(report.getTitle())).append(',')
                .append(report.getUuid()).append(',')
                .append(report.getCreatedAt()).append('\n');
        if (report.getDataJson() != null) {
            csv.append("Data\n").append(sanitizeCsv(report.getDataJson())).append('\n');
        }
        return new ByteArrayResource(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    public MessageResponse delete(UUID uuid) {
        Report report = getByUuid(uuid);
        reportRepository.delete(report);
        return MessageResponse.of("Report deleted");
    }

    private String sanitizeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
