package service;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import model.Student;
import model.TestMark;
import model.Payment;

/**
 * PDF Report Service - Generates Report Cards and Payment Receipts using iText
 */
public class PDFReportService {

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BaseColor.DARK_GRAY);
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.BLACK);
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    /**
     * Generate Student Report Card
     */
    public String generateReportCard(Student s, List<TestMark> marks, String outputPath) {
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            // Header
            Paragraph header = new Paragraph("MRK TUITION CENTER", FONT_TITLE);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
            
            Paragraph subHeader = new Paragraph("STUDENT REPORT CARD", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.GRAY));
            subHeader.setAlignment(Element.ALIGN_CENTER);
            subHeader.setSpacingAfter(20);
            document.add(subHeader);

            // Student Info
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(20);
            
            addInfoRow(infoTable, "Student Name:", s.getName());
            addInfoRow(infoTable, "Student ID:", s.getUserId());
            addInfoRow(infoTable, "Standard:", s.getCurrentStd());
            addInfoRow(infoTable, "Board:", s.getBoard());
            addInfoRow(infoTable, "Generated On:", DATE_FMT.format(new Date()));
            
            document.add(infoTable);

            // Marks Table
            PdfPTable marksTable = new PdfPTable(5);
            marksTable.setWidthPercentage(100);
            marksTable.setWidths(new float[]{3f, 2f, 1.5f, 1.5f, 1f});
            
            String[] headers = {"Subject", "Exam Name", "Marks", "Max", "Grade"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FONT_HEADER));
                cell.setBackgroundColor(new BaseColor(10, 27, 63)); // NAV_BG
                cell.setPadding(8);
                marksTable.addCell(cell);
            }

            double totalObtained = 0, totalMax = 0;
            for (TestMark m : marks) {
                marksTable.addCell(new Phrase(m.getSubjectName(), FONT_BODY));
                marksTable.addCell(new Phrase(m.getTestName(), FONT_BODY));
                marksTable.addCell(new Phrase(String.valueOf(m.getMarksObtained()), FONT_BODY));
                marksTable.addCell(new Phrase(String.valueOf(m.getMaxMarks()), FONT_BODY));
                marksTable.addCell(new Phrase(calculateGrade(m.getMarksObtained(), m.getMaxMarks()), FONT_BODY));
                
                totalObtained += m.getMarksObtained();
                totalMax += m.getMaxMarks();
            }
            document.add(marksTable);

            // Summary
            Paragraph summary = new Paragraph("\nSUMMARY", FONT_LABEL);
            summary.setSpacingBefore(10);
            document.add(summary);
            
            double percentage = (totalMax > 0) ? (totalObtained / totalMax * 100) : 0;
            document.add(new Paragraph(String.format("Total Marks: %.1f / %.1f", totalObtained, totalMax), FONT_BODY));
            document.add(new Paragraph(String.format("Percentage: %.2f%%", percentage), FONT_BODY));
            document.add(new Paragraph("Overall Grade: " + calculateGrade((int)totalObtained, (int)totalMax), FONT_BODY));

            // Footer
            Paragraph footer = new Paragraph("\n\nTeacher Signature: ____________________          Principal Signature: ____________________", FONT_BODY);
            footer.setSpacingBefore(50);
            document.add(footer);

            document.close();
            return outputPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate Payment Receipt
     */
    public String generateReceipt(Student s, Payment p, String outputPath) {
        Document document = new Document(PageSize.A5);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            // Header
            Paragraph header = new Paragraph("MRK TUITION CENTER", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY));
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
            
            Paragraph subHeader = new Paragraph("PAYMENT RECEIPT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.GRAY));
            subHeader.setAlignment(Element.ALIGN_CENTER);
            subHeader.setSpacingAfter(20);
            document.add(subHeader);

            // Info Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            
            addInfoRow(table, "Receipt No:", "RCT-" + System.currentTimeMillis() / 10000);
            addInfoRow(table, "Date:", DATE_FMT.format(p.getPaymentDate()));
            addInfoRow(table, "Student Name:", s.getName());
            addInfoRow(table, "Student ID:", s.getUserId());
            addInfoRow(table, "Subject:", p.getSubjectId()); // Assuming subject name is handled elsewhere or just ID
            addInfoRow(table, "Amount Paid:", "INR " + p.getAmountPaid());
            addInfoRow(table, "Payment Mode:", p.getPaymentMode());
            addInfoRow(table, "Status:", "SUCCESS");
            
            document.add(table);

            Paragraph footer = new Paragraph("\nThis is a system-generated receipt.", FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return outputPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell keyCell = new PdfPCell(new Phrase(label, FONT_LABEL));
        keyCell.setBorder(Rectangle.NO_BORDER);
        keyCell.setPadding(5);
        table.addCell(keyCell);
        
        PdfPCell valCell = new PdfPCell(new Phrase(value != null ? value : "—", FONT_BODY));
        valCell.setBorder(Rectangle.NO_BORDER);
        valCell.setPadding(5);
        table.addCell(valCell);
    }

    private String calculateGrade(int obtained, int max) {
        if (max == 0) return "N/A";
        double p = (double) obtained / max * 100;
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B";
        if (p >= 60) return "C";
        if (p >= 50) return "D";
        return "F";
    }
}
