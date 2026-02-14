package com.saccos_system.service;



import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.saccos_system.dto.StatementResponseDTO;
import com.saccos_system.dto.StatementTransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PDFService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public byte[] generateStatementPDF(StatementResponseDTO statement) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Initialize PDF writer
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(20, 20, 20, 20);

            // Add content
            addHeader(document, statement);
            addMemberInfo(document, statement);
            addSummary(document, statement);
            addTransactionsTable(document, statement);
            addFooter(document, statement);

            document.close();

            log.info("PDF statement generated successfully for: {}", statement.getStatementNumber());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF statement: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private void addHeader(Document document, StatementResponseDTO statement) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont();

        // SACCO Name
        Paragraph saccoName = new Paragraph()
                .add(new Text("SACCO SYSTEM\n").setFont(boldFont).setFontSize(20))
                .setTextAlignment(TextAlignment.CENTER);
        document.add(saccoName);

        // Statement Title
        Paragraph title = new Paragraph()
                .add(new Text("MONTHLY STATEMENT\n").setFontSize(16))
                .add(new Text(statement.getPeriod()).setFontSize(14))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // Horizontal line
        Table line = new Table(1);
        line.addCell(new Cell().add(new Paragraph(""))
                .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER));
        document.add(line);
    }

    private void addMemberInfo(Document document, StatementResponseDTO statement) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginTop(20);
        table.setMarginBottom(20);

        // Member Name
        table.addCell(createLabelCell("Member Name:"));
        table.addCell(createValueCell(statement.getMemberName()));

        // Member Number
        table.addCell(createLabelCell("Member No:"));
        table.addCell(createValueCell(statement.getMemberNumber()));

        // Account Number
        table.addCell(createLabelCell("Account No:"));
        table.addCell(createValueCell(statement.getAccountNumber()));

        // Statement Number
        table.addCell(createLabelCell("Statement No:"));
        table.addCell(createValueCell(statement.getStatementNumber()));

        document.add(table);
    }

    private void addSummary(Document document, StatementResponseDTO statement) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        // Headers
        table.addCell(createSummaryHeaderCell("Opening"));
        table.addCell(createSummaryHeaderCell("Deposits"));
        table.addCell(createSummaryHeaderCell("Withdrawals"));
        table.addCell(createSummaryHeaderCell("Interest"));
        table.addCell(createSummaryHeaderCell("Closing"));

        // Values
        table.addCell(createSummaryValueCell(statement.getOpeningBalance(), false));
        table.addCell(createSummaryValueCell(statement.getTotalDeposits(), true));
        table.addCell(createSummaryValueCell(statement.getTotalWithdrawals(), false));
        table.addCell(createSummaryValueCell(statement.getTotalInterest(), true));
        table.addCell(createSummaryValueCell(statement.getClosingBalance(), false, true));

        document.add(table);
    }

    private void addTransactionsTable(Document document, StatementResponseDTO statement) {
        Paragraph title = new Paragraph("TRANSACTION HISTORY")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2.5f, 1.5f, 1, 1, 1}));
        table.setWidth(UnitValue.createPercentValue(100));

        // Table headers
        String[] headers = {"Date", "Description", "Reference", "Deposit", "Withdrawal", "Balance"};
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8));
        }

        // Transactions
        for (StatementTransactionDTO tx : statement.getTransactions()) {
            table.addCell(createCell(tx.getDate(), TextAlignment.LEFT));
            table.addCell(createCell(tx.getDescription(), TextAlignment.LEFT));
            table.addCell(createCell(tx.getReference(), TextAlignment.LEFT));
            table.addCell(createCell(
                    tx.getDeposit().compareTo(BigDecimal.ZERO) > 0 ?
                            String.format("%,.2f", tx.getDeposit()) : "-",
                    TextAlignment.RIGHT));
            table.addCell(createCell(
                    tx.getWithdrawal().compareTo(BigDecimal.ZERO) > 0 ?
                            String.format("%,.2f", tx.getWithdrawal()) : "-",
                    TextAlignment.RIGHT));
            table.addCell(createCell(
                    String.format("%,.2f", tx.getBalance()),
                    TextAlignment.RIGHT));
        }

        document.add(table);
    }

    private void addFooter(Document document, StatementResponseDTO statement) {
        Paragraph generatedDate = new Paragraph()
                .add(new Text("Generated on: "))
                .add(new Text(statement.getGeneratedDate().format(DATE_TIME_FORMATTER)))
                .setMarginTop(30)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(generatedDate);

        Paragraph note = new Paragraph()
                .add(new Text("This is an electronically generated statement and does not require a physical signature."))
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(note);

        Paragraph copyright = new Paragraph()
                .add(new Text("© 2026 SACCO System. All rights reserved."))
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(5);
        document.add(copyright);
    }

    private Cell createLabelCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setBorder(Border.NO_BORDER)
                .setPadding(4)
                .setTextAlignment(TextAlignment.RIGHT);
    }

    private Cell createValueCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setBorder(Border.NO_BORDER)
                .setPadding(4)
                .setTextAlignment(TextAlignment.LEFT);
    }

    private Cell createSummaryHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8);
    }

    private Cell createSummaryValueCell(BigDecimal value, boolean isPositive) {
        return createSummaryValueCell(value, isPositive, false);
    }

    private Cell createSummaryValueCell(BigDecimal value, boolean isPositive, boolean isBold) {
        Paragraph p = new Paragraph(String.format("%,.2f", value));
        if (isBold) {
            p.setBold();
        }
        if (isPositive && value.compareTo(BigDecimal.ZERO) > 0) {
            p.setFontColor(ColorConstants.GREEN);
        } else if (!isPositive && value.compareTo(BigDecimal.ZERO) > 0) {
            p.setFontColor(ColorConstants.RED);
        }
        return new Cell()
                .add(p)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8);
    }

    private Cell createCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text != null ? text : ""))
                .setTextAlignment(alignment)
                .setPadding(6);
    }
}