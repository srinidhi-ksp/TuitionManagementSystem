package service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import db.DBConnection;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * iText 5 implementation of Salary PDF Report
 */
public class SalaryPDFService {

    private static final BaseColor HEADER_BG = new BaseColor(2, 6, 23);
    private static final BaseColor ACCENT    = new BaseColor(59, 130, 246);
    private static final BaseColor ROW_ALT   = new BaseColor(248, 250, 252);
    private static final BaseColor DIVIDER   = new BaseColor(226, 232, 240);
    private static final BaseColor TEXT_DARK = new BaseColor(15, 23, 42);

    public String generateSalaryReport(String filePath) {
        com.itextpdf.text.Document document = null;
        try {
            MongoDatabase db = DBConnection.getDatabase();
            MongoCollection<Document> teachersCol = db.getCollection("teachers");
            java.util.List<Document> teachers = new ArrayList<>();
            for (Document d : teachersCol.find()) teachers.add(d);

            document = new com.itextpdf.text.Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BaseColor.WHITE);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 13, new BaseColor(148, 163, 184));
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_DARK);
            Font summaryLabelFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new BaseColor(100, 116, 139));
            Font summaryValFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, TEXT_DARK);

            // ── HEADER SECTION ──────────────────────────────────────────────
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(HEADER_BG);
            headerCell.setPadding(20);
            headerCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
            
            Paragraph p1 = new Paragraph("MRK TUITION CENTER", titleFont);
            p1.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(p1);
            
            Paragraph p2 = new Paragraph("Teacher Salary Report", subTitleFont);
            p2.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(p2);
            
            headerTable.addCell(headerCell);
            document.add(headerTable);

            // Generated date + report period
            String today = new SimpleDateFormat("dd MMMM yyyy").format(new Date());
            Paragraph datePara = new Paragraph("Generated on: " + today + "   |   Report Period: " +
                new SimpleDateFormat("MMMM yyyy").format(new Date()), 
                FontFactory.getFont(FontFactory.HELVETICA, 10, new BaseColor(100, 116, 139)));
            datePara.setAlignment(Element.ALIGN_RIGHT);
            datePara.setSpacingBefore(8);
            datePara.setSpacingAfter(16);
            document.add(datePara);

            // ── SUMMARY STATISTICS ROW ───────────────────────────────────────
            double totalSalaryBill = 0;
            double maxSalary = 0;
            double minSalary = Double.MAX_VALUE;

            for (Document t : teachers) {
                Object salObj = t.get("salary");
                double base = 0;
                if (salObj instanceof Number) {
                    base = ((Number) salObj).doubleValue();
                } else if (salObj instanceof Document) {
                    Document salDoc = (Document) salObj;
                    base = (salDoc.get("base_salary") instanceof Number) ? ((Number) salDoc.get("base_salary")).doubleValue() : 0;
                }
                totalSalaryBill += base;
                if (base > maxSalary) maxSalary = base;
                if (base < minSalary) minSalary = base;
            }
            if (teachers.isEmpty()) minSalary = 0;

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20);
            
            addSummaryCard(summaryTable, "Total Teachers", String.valueOf(teachers.size()), summaryLabelFont, summaryValFont);
            addSummaryCard(summaryTable, "Total Salary Bill", "₹" + String.format("%,.0f", totalSalaryBill), summaryLabelFont, summaryValFont);
            addSummaryCard(summaryTable, "Highest Salary", "₹" + String.format("%,.0f", maxSalary), summaryLabelFont, summaryValFont);
            addSummaryCard(summaryTable, "Lowest Salary", "₹" + String.format("%,.0f", minSalary), summaryLabelFont, summaryValFont);
            document.add(summaryTable);

            // ── SALARY TABLE ───────────────────────────────────────────────
            document.add(new Paragraph("Detailed Salary Breakdown", 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, TEXT_DARK)));
            
            float[] colWidths = {3f, 6f, 14f, 13f, 12f, 10f, 10f, 10f, 11f};
            PdfPTable salaryTable = new PdfPTable(colWidths);
            salaryTable.setWidthPercentage(100);
            salaryTable.setSpacingBefore(10);

            String[] headers = {"#", "Teacher ID", "Name", "Specialization", "Qualifications",
                                 "Base Salary", "Working Days", "Per Day (₹)", "Net Payable"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
                cell.setBackgroundColor(ACCENT);
                cell.setPadding(8);
                cell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                salaryTable.addCell(cell);
            }

            int rowNum = 1;
            for (Document t : teachers) {
                BaseColor rowBg = (rowNum % 2 == 0) ? ROW_ALT : BaseColor.WHITE;

                Object salObj = t.get("salary");
                double baseSal = 0;
                int workDays = 26;
                if (salObj instanceof Number) {
                    baseSal = ((Number) salObj).doubleValue();
                } else if (salObj instanceof Document) {
                    Document salDoc = (Document) salObj;
                    baseSal = (salDoc.get("base_salary") instanceof Number) ? ((Number) salDoc.get("base_salary")).doubleValue() : 0;
                    workDays = (salDoc.get("working_days") instanceof Number) ? ((Number) salDoc.get("working_days")).intValue() : 26;
                }
                double perDay    = (workDays > 0) ? baseSal / workDays : 0;
                
                java.util.List<String> quals = t.getList("qualifications", String.class);
                String qualStr = (quals != null) ? String.join(", ", quals) : "N/A";

                String[] rowData = {
                    String.valueOf(rowNum),
                    t.getString("_id") != null ? t.getString("_id") : "N/A",
                    t.getString("full_name") != null ? t.getString("full_name") : "N/A",
                    t.getString("specialization") != null ? t.getString("specialization") : "N/A",
                    qualStr,
                    "₹" + String.format("%,.0f", baseSal),
                    String.valueOf(workDays),
                    "₹" + String.format("%.0f", perDay),
                    "₹" + String.format("%,.0f", baseSal)
                };

                for (int i = 0; i < rowData.length; i++) {
                    PdfPCell cell = new PdfPCell(new Phrase(rowData[i], dataFont));
                    cell.setBackgroundColor(rowBg);
                    cell.setPadding(7);
                    cell.setBorder(com.itextpdf.text.Rectangle.BOTTOM);
                    cell.setBorderColor(DIVIDER);
                    cell.setHorizontalAlignment(i == 0 ? Element.ALIGN_CENTER : Element.ALIGN_LEFT);
                    salaryTable.addCell(cell);
                }
                rowNum++;
            }
            document.add(salaryTable);

            // Totals
            Paragraph totalPara = new Paragraph("Total Salary Payable This Month: ₹" + String.format("%,.0f", totalSalaryBill),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, ACCENT));
            totalPara.setAlignment(Element.ALIGN_RIGHT);
            totalPara.setSpacingBefore(12);
            document.add(totalPara);

            // Footer
            Paragraph footer = new Paragraph("\n\nThis report is system-generated by MRK Tuition Admin Dashboard. For queries contact admin@mrktuition.com",
                FontFactory.getFont(FontFactory.HELVETICA, 8, new BaseColor(148, 163, 184)));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            return filePath;

        } catch (Exception e) {
            System.err.println("[SalaryPDFService] Error generating PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (document != null && document.isOpen()) {
                try { document.close(); } catch (Exception e) {}
            }
        }
    }

    private void addSummaryCard(PdfPTable table, String label, String value, Font lFont, Font vFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(ROW_ALT);
        cell.setBorder(com.itextpdf.text.Rectangle.BOX);
        cell.setBorderColor(DIVIDER);
        cell.setPadding(14);
        
        Paragraph l = new Paragraph(label, lFont);
        l.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(l);
        
        Paragraph v = new Paragraph(value, vFont);
        v.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(v);
        
        table.addCell(cell);
    }

    /**
     * Generate individual Payslip PDF for a teacher directly from profile data
     */
    public String generateTeacherPayslip(model.Teacher t, String filePath) {
        try {
            if (t == null) return null;

            com.itextpdf.text.Document document = new com.itextpdf.text.Document(PageSize.A5);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, HEADER_BG);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new BaseColor(100, 116, 139));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEXT_DARK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_DARK);

            // Header
            Paragraph header = new Paragraph("MRK TUITION CENTER", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
            
            Paragraph subHeader = new Paragraph("MONTHLY SALARY SLIP", subTitleFont);
            subHeader.setAlignment(Element.ALIGN_CENTER);
            subHeader.setSpacingAfter(20);
            document.add(subHeader);

            // Teacher Info
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.addCell(createNoBorderCell("Teacher: " + t.getName(), boldFont));
            String monthYear = new SimpleDateFormat("MMMM yyyy").format(new Date());
            infoTable.addCell(createNoBorderCell("Month/Year: " + monthYear, normalFont));
            infoTable.addCell(createNoBorderCell("ID: " + t.getUserId(), normalFont));
            infoTable.addCell(createNoBorderCell("Status: PAID (PROCESSED)", SUCCESS_COLOR()));
            infoTable.setSpacingAfter(20);
            document.add(infoTable);

            // Earnings Breakdown
            PdfPTable breakdown = new PdfPTable(2);
            breakdown.setWidthPercentage(100);
            breakdown.setSpacingAfter(15);
            
            // Header for breakdown
            PdfPCell h1 = new PdfPCell(new Phrase("Description", boldFont));
            h1.setBackgroundColor(DIVIDER);
            h1.setPadding(8);
            PdfPCell h2 = new PdfPCell(new Phrase("Amount (₹)", boldFont));
            h2.setBackgroundColor(DIVIDER);
            h2.setPadding(8);
            h2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            breakdown.addCell(h1);
            breakdown.addCell(h2);

            // Calculation rules: Base (50%), HRA (30%), DA (20%)
            double total = t.getSalary();
            double base = total * 0.5;
            double hra = total * 0.3;
            double da = total * 0.2;

            addRow(breakdown, "Basic Salary", base, normalFont);
            addRow(breakdown, "House Rent Allowance (HRA)", hra, normalFont);
            addRow(breakdown, "Dearness Allowance (DA)", da, normalFont);
            
            document.add(breakdown);

            // Total
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            PdfPCell t1 = new PdfPCell(new Phrase("NET PAYABLE", boldFont));
            t1.setBorder(Rectangle.TOP);
            t1.setPadding(10);
            PdfPCell t2 = new PdfPCell(new Phrase("₹" + String.format("%,.0f", total), boldFont));
            t2.setBorder(Rectangle.TOP);
            t2.setPadding(10);
            t2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.addCell(t1);
            totalTable.addCell(t2);
            document.add(totalTable);

            Paragraph foot = new Paragraph("\n\n*This is a computer generated document and does not require a signature.", 
                FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY));
            foot.setAlignment(Element.ALIGN_CENTER);
            document.add(foot);

            document.close();
            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    private PdfPCell createNoBorderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private void addRow(PdfPTable table, String desc, double amt, Font font) {
        table.addCell(new PdfPCell(new Phrase(desc, font)));
        PdfPCell cell = new PdfPCell(new Phrase(String.format("%,.2f", amt), font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private Font SUCCESS_COLOR() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new BaseColor(34, 197, 94));
    }
}
