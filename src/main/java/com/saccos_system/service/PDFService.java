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
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.saccos_system.dto.StatementDTO.StatementResponseDTO;
import com.saccos_system.dto.StatementDTO.StatementTransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PDFService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public byte[] generateStatementPDF(StatementResponseDTO statement) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(20, 20, 20, 20);

            addHeader(document, statement);
            addMemberInfo(document, statement);
            addSummary(document, statement);
            addTransactionsTable(document, statement);
            addFooter(document, statement);

            document.close();

            log.info("PDF generated for statement: {}", statement.getStatementNumber());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private void addHeader(Document document, StatementResponseDTO statement) throws Exception {
        PdfFont boldFont = PdfFontFactory.createFont();

        Paragraph saccoName = new Paragraph()
                .add(new Text("UHAMIAJI SACCOS STATEMENT\n").setFont(boldFont).setFontSize(20))
                .setTextAlignment(TextAlignment.CENTER);
        document.add(saccoName);

        Paragraph title = new Paragraph()
                .add(new Text("MONTHLY STATEMENT\n").setFontSize(16))
                .add(new Text(statement.getPeriod()).setFontSize(14))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

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

        table.addCell(createLabelCell("Member Name:"));
        table.addCell(createValueCell(statement.getMemberName()));
        table.addCell(createLabelCell("Member No:"));
        table.addCell(createValueCell(statement.getMemberNumber()));
        table.addCell(createLabelCell("Account No:"));
        table.addCell(createValueCell(statement.getAccountNumber()));
        table.addCell(createLabelCell("Statement No:"));
        table.addCell(createValueCell(statement.getStatementNumber()));

        document.add(table);
    }

    private void addSummary(Document document, StatementResponseDTO statement) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        table.addCell(createSummaryHeaderCell("Opening"));
        table.addCell(createSummaryHeaderCell("Deposits"));
        table.addCell(createSummaryHeaderCell("Withdrawals"));
        table.addCell(createSummaryHeaderCell("Interest"));
        table.addCell(createSummaryHeaderCell("Closing"));

        table.addCell(createSummaryValueCell(statement.getOpeningBalance()));
        table.addCell(createSummaryValueCell(statement.getTotalDeposits()));
        table.addCell(createSummaryValueCell(statement.getTotalWithdrawals()));
        table.addCell(createSummaryValueCell(statement.getTotalInterest()));
        table.addCell(createSummaryValueCell(statement.getClosingBalance(), true));

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

        String[] headers = {"Date", "Description", "Reference", "Deposit", "Withdrawal", "Balance"};
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8));
        }

        for (StatementTransactionDTO tx : statement.getTransactions()) {
            table.addCell(createCell(tx.getDate(), TextAlignment.LEFT));
            table.addCell(createCell(truncate(tx.getDescription(), 26), TextAlignment.LEFT));
            table.addCell(createCell(tx.getReference(), TextAlignment.LEFT));
            table.addCell(createCell(formatAmount(tx.getDeposit()), TextAlignment.RIGHT));
            table.addCell(createCell(formatAmount(tx.getWithdrawal()), TextAlignment.RIGHT));
            table.addCell(createCell(formatAmount(tx.getBalance()), TextAlignment.RIGHT));
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
                .add(new Paragraph(text != null ? text : ""))
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

    private Cell createSummaryValueCell(BigDecimal value) {
        return createSummaryValueCell(value, false);
    }

    private Cell createSummaryValueCell(BigDecimal value, boolean isBold) {
        Paragraph p = new Paragraph(formatAmount(value));
        if (isBold) {
            p.setBold();
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

    private String formatAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) != 0
                ? String.format("%,.2f", amount) : "-";
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }
}