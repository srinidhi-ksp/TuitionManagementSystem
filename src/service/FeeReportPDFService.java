package service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import db.DBConnection;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import java.awt.*;
import java.awt.Font;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * iText 5 implementation of Fee Report PDF Service
 */
public class FeeReportPDFService {

    private static final BaseColor HEADER_BG  = new BaseColor(2, 6, 23);
    private static final BaseColor ACCENT     = new BaseColor(59, 130, 246);
    private static final BaseColor SUCCESS    = new BaseColor(34, 197, 94);
    private static final BaseColor WARNING    = new BaseColor(245, 158, 11);
    private static final BaseColor ERROR      = new BaseColor(239, 68, 68);
    private static final BaseColor ROW_ALT    = new BaseColor(248, 250, 252);
    private static final BaseColor DIVIDER    = new BaseColor(226, 232, 240);
    private static final BaseColor TEXT_DARK  = new BaseColor(15, 23, 42);

    public String generateFeeReport(String filePath) {
        com.itextpdf.text.Document document = null;
        try {
            MongoDatabase db = DBConnection.getDatabase();
            MongoCollection<Document> paymentsCol   = db.getCollection("payments");
            MongoCollection<Document> studentsCol   = db.getCollection("students");
            MongoCollection<Document> batchesCol    = db.getCollection("batches");

            // ── COLLECT DATA ─────────────────────────────────────────────────
            java.util.List<Document> payments = new ArrayList<>();
            for (Document p : paymentsCol.find()) payments.add(p);

            double totalPaid = 0, totalPending = 0, totalPartial = 0;
            int countPaid = 0, countPending = 0, countPartial = 0;

            for (Document p : payments) {
                String status = p.getString("status");
                Object amtObj = p.get("amount");
                double amt = (amtObj instanceof Number) ? ((Number) amtObj).doubleValue() : 0;
                
                if ("PAID".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) { 
                    totalPaid += amt; countPaid++; 
                } else if ("PENDING".equalsIgnoreCase(status) || "UNPAID".equalsIgnoreCase(status) || "REQUESTED".equalsIgnoreCase(status)) { 
                    totalPending += amt; countPending++; 
                } else if ("PARTIAL".equalsIgnoreCase(status)) { 
                    totalPartial += amt; countPartial++; 
                }
            }
            double grandTotal = totalPaid + totalPending + totalPartial;

            document = new com.itextpdf.text.Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Fonts
            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BaseColor.WHITE);
            com.itextpdf.text.Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 13, new BaseColor(148, 163, 184));
            com.itextpdf.text.Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
            com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new BaseColor(30, 41, 59));

            // HEADER BANNER
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(HEADER_BG);
            headerCell.setPadding(20);
            headerCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
            
            Paragraph p1 = new Paragraph("MRK TUITION CENTER", titleFont);
            p1.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(p1);
            
            Paragraph p2 = new Paragraph("Student Fee Payment Report", subTitleFont);
            p2.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(p2);
            
            headerTable.addCell(headerCell);
            document.add(headerTable);

            String today = new SimpleDateFormat("dd MMMM yyyy").format(new Date());
            Paragraph datePara = new Paragraph("Generated: " + today, FontFactory.getFont(FontFactory.HELVETICA, 9, new BaseColor(100, 116, 139)));
            datePara.setAlignment(Element.ALIGN_RIGHT);
            datePara.setSpacingBefore(8);
            datePara.setSpacingAfter(16);
            document.add(datePara);

            // ── SUMMARY CARDS ────────────────────────────────────────────────
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20);
            
            addSummaryCard(summaryTable, "Total Collected", "₹" + String.format("%,.0f", totalPaid), SUCCESS);
            addSummaryCard(summaryTable, "Pending Amount", "₹" + String.format("%,.0f", totalPending), ERROR);
            addSummaryCard(summaryTable, "Partial Payments", "₹" + String.format("%,.0f", totalPartial), WARNING);
            addSummaryCard(summaryTable, "Total Fee Amount", "₹" + String.format("%,.0f", grandTotal), ACCENT);
            document.add(summaryTable);

            // ── PIE CHART ──────────────────────────────────────────────────
            byte[] pieChartBytes = generatePieChartImage(countPaid, countPending, countPartial);
            if (pieChartBytes != null) {
                document.add(new Paragraph("Payment Status Distribution", 
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, TEXT_DARK)));
                
                PdfPTable chartLayout = new PdfPTable(new float[]{45, 55});
                chartLayout.setWidthPercentage(100);
                chartLayout.setSpacingBefore(10);
                chartLayout.setSpacingAfter(24);

                com.itextpdf.text.Image pieImg = com.itextpdf.text.Image.getInstance(pieChartBytes);
                pieImg.scaleToFit(200, 200);
                PdfPCell imgCell = new PdfPCell(pieImg);
                imgCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                chartLayout.addCell(imgCell);

                PdfPCell legendCell = new PdfPCell();
                legendCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                legendCell.setPaddingLeft(20);
                legendCell.setPaddingTop(30);
                
                legendCell.addElement(new Paragraph("● PAID: " + countPaid + " payments", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, SUCCESS)));
                legendCell.addElement(new Paragraph("● PENDING: " + countPending + " payments", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, ERROR)));
                legendCell.addElement(new Paragraph("● PARTIAL: " + countPartial + " payments", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, WARNING)));
                
                chartLayout.addCell(legendCell);
                document.add(chartLayout);
            }

            // ── DETAILED PAYMENTS TABLE ──────────────────────────────────────
            document.add(new Paragraph("Detailed Payment Logs", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, TEXT_DARK)));
            
            float[] colWidths = {4f, 8f, 13f, 13f, 8f, 9f, 10f, 11f};
            PdfPTable detailTable = new PdfPTable(colWidths);
            detailTable.setWidthPercentage(100);
            detailTable.setSpacingBefore(10);

            String[] headers = {"#", "Batch ID", "Student Name", "Batch Name", "Amount", "Status", "Mode", "Date"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
                cell.setBackgroundColor(ACCENT);
                cell.setPadding(7);
                cell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                detailTable.addCell(cell);
            }

            int rn = 1;
            for (Document p : payments) {
                if (p == null) continue;

                // ── Guard: amount must be > 0 ──────────────────────────────────
                double amt = p.get("amount") instanceof Number
                        ? ((Number) p.get("amount")).doubleValue() : 0;
                if (amt <= 0) continue;

                // ── Guard: student_id must exist ───────────────────────────────
                Object studentIdObj = p.get("student_id");
                if (studentIdObj == null) continue;
                String studentId = studentIdObj.toString();

                // ── Guard: batch_id must exist ─────────────────────────────────
                Object bidObj = p.get("batch_id");
                if (bidObj == null) continue;

                // ── Student Name – triple-key lookup ───────────────────────────
                Document stu = studentsCol.find(Filters.or(
                    Filters.eq("_id",        studentId),
                    Filters.eq("student_id", studentId),
                    Filters.eq("user_id",    studentId)
                )).first();
                if (stu == null) continue;   // skip unresolvable row

                String studentName = stu.getString("full_name") != null
                        ? stu.getString("full_name")
                        : stu.getString("name") != null
                            ? stu.getString("name") : "Unknown";

                // ── Batch Name – Number or String _id ──────────────────────────
                Document batch;
                if (bidObj instanceof Number) {
                    int bid = ((Number) bidObj).intValue();
                    batch = batchesCol.find(Filters.or(
                        Filters.eq("_id",      bid),
                        Filters.eq("batch_id", bid)
                    )).first();
                } else {
                    String bidStr = bidObj.toString();
                    batch = batchesCol.find(Filters.or(
                        Filters.eq("_id",      bidStr),
                        Filters.eq("batch_id", bidStr),
                        Filters.eq("batch_id", String.valueOf(bidObj))
                    )).first();
                }
                if (batch == null) continue;  // skip unresolvable row

                String batchInfo = batch.getString("batch_name") != null
                        ? batch.getString("batch_name")
                        : batch.getString("name") != null
                            ? batch.getString("name") : "Unknown";

                // ── Safe field extraction ──────────────────────────────────────
                String mode = p.getString("method") != null
                        ? p.getString("method") : p.getString("payment_mode");
                if (mode == null) mode = "—";

                String status = p.getString("status");
                if ("SUCCESS".equalsIgnoreCase(status)) status = "PAID";
                if (status == null) status = "—";

                java.util.Date pDate = p.getDate("date") != null
                        ? p.getDate("date") : p.getDate("payment_date");
                String dateStr = pDate != null
                        ? new SimpleDateFormat("dd-MM-yyyy").format(pDate) : "—";

                // ── Row colour ─────────────────────────────────────────────────
                BaseColor rowBg = (rn % 2 == 0) ? ROW_ALT : BaseColor.WHITE;
                BaseColor sColor = ("PAID".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) ? SUCCESS
                                 : ("PENDING".equalsIgnoreCase(status) || "UNPAID".equalsIgnoreCase(status))  ? ERROR
                                 : WARNING;

                String[] rowData = {
                    String.valueOf(rn),
                    String.valueOf(bidObj),
                    studentName,
                    batchInfo,
                    "₹" + String.format("%,.0f", amt),
                    status,
                    mode,
                    dateStr
                };

                for (int i = 0; i < rowData.length; i++) {
                    com.itextpdf.text.Font f = (i == 5)
                            ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, sColor) : dataFont;
                    PdfPCell cell = new PdfPCell(new Phrase(rowData[i] != null ? rowData[i] : "—", f));
                    cell.setBackgroundColor(rowBg);
                    cell.setBorder(com.itextpdf.text.Rectangle.BOTTOM);
                    cell.setBorderColor(DIVIDER);
                    cell.setPadding(6);
                    cell.setHorizontalAlignment(i == 0 ? Element.ALIGN_CENTER : Element.ALIGN_LEFT);
                    detailTable.addCell(cell);
                }
                rn++;
            }
            document.add(detailTable);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (document != null && document.isOpen()) {
                try { document.close(); } catch (Exception ignored) {}
            }
        }
        return filePath;
    }

    private byte[] generatePieChartImage(int paid, int pending, int partial) {
        try {
            int total = paid + pending + partial;
            if (total == 0) return null;

            int size = 320;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(java.awt.Color.WHITE);
            g2.fillRect(0, 0, size, size);

            int margin = 20;
            int diameter = size - 2 * margin;

            double[] values = {paid, pending, partial};
            java.awt.Color[] colors  = {
                new java.awt.Color(34, 197, 94),
                new java.awt.Color(239, 68, 68),
                new java.awt.Color(245, 158, 11)
            };

            double startAngle = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == 0) continue;
                double arcAngle = (values[i] / total) * 360.0;
                g2.setColor(colors[i]);
                g2.fill(new Arc2D.Double(margin, margin, diameter, diameter, startAngle, arcAngle, Arc2D.PIE));
                startAngle += arcAngle;
            }

            g2.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private void addSummaryCard(PdfPTable table, String label, String value, BaseColor accent) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(ROW_ALT);
        cell.setBorder(com.itextpdf.text.Rectangle.BOX);
        cell.setBorderColor(DIVIDER);
        cell.setPadding(12);
        
        Paragraph l = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA, 9, new BaseColor(100, 116, 139)));
        l.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(l);
        
        Paragraph v = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, accent));
        v.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(v);
        
        table.addCell(cell);
    }
}
