package util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import model.Receipt;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Professional PDF Receipt Generator
 * Generates branded, colored, and styled fee receipts
 */
public class PDFReceiptGenerator {

    // Color Theme
    private static final BaseColor PRIMARY_BLUE = new BaseColor(13, 27, 42);      // #0D1B2A
    private static final BaseColor SECONDARY_BLUE = new BaseColor(27, 38, 59);    // #1B263B
    private static final BaseColor ACCENT_GREEN = new BaseColor(76, 175, 80);     // #4CAF50
    private static final BaseColor ACCENT_ORANGE = new BaseColor(255, 152, 0);    // #FF9800
    private static final BaseColor TEXT_DARK = new BaseColor(26, 35, 64);         // #1A2340
    private static final BaseColor TEXT_LIGHT = new BaseColor(107, 122, 153);     // #6B7A99
    private static final BaseColor BORDER_LIGHT = new BaseColor(225, 230, 240);   // #E1E6F0
    private static final BaseColor ROW_ALT = new BaseColor(248, 250, 253);        // #F8FAFD

    // Fonts
    private Font headerBold;
    private Font headerRegular;
    private Font titleBold;
    private Font sectionBold;
    private Font labelBold;
    private Font bodyRegular;
    private Font bodySmall;
    private Font badgeFont;

    public PDFReceiptGenerator() {
        // Initialize fonts
        this.headerBold = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, BaseColor.WHITE);
        this.headerRegular = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.WHITE);
        this.titleBold = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, TEXT_DARK);
        this.sectionBold = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, TEXT_DARK);
        this.labelBold = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, TEXT_LIGHT);
        this.bodyRegular = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_DARK);
        this.bodySmall = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_LIGHT);
        this.badgeFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
    }

    /**
     * Generate professional PDF receipt
     */
    public String generateReceipt(Receipt receipt) {
        String fileName = generateFileName(receipt);
        
        try {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            // 1. Add Header (Letterhead)
            addHeader(document);

            // 2. Add Title
            addTitle(document);

            // 3. Add Receipt Metadata (Receipt No, Date)
            addReceiptMetadata(document, receipt);

            // 4. Add Student Details Section
            addStudentDetailsSection(document, receipt);

            // 5. Add Payment Details Section
            addPaymentDetailsSection(document, receipt);

            // 6. Add Status Badge
            addStatusBadge(document, receipt);

            // 7. Add Footer
            addFooter(document);

            document.close();
            System.out.println("✅ Receipt generated successfully: " + fileName);
            return fileName;

        } catch (Exception e) {
            System.err.println("❌ Error generating PDF receipt: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Add professional header/letterhead
     */
    private void addHeader(Document document) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);

        // Left side: Company details
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBackgroundColor(PRIMARY_BLUE);
        leftCell.setPadding(15);
        leftCell.setBorder(PdfPCell.NO_BORDER);

        // Company name
        Paragraph companyName = new Paragraph("MRK TUITION CENTER", headerBold);
        leftCell.addElement(companyName);

        // Company details
        Paragraph address = new Paragraph("33/3, Krishnamachari Road, Virudhunagar", headerRegular);
        leftCell.addElement(address);

        Paragraph phone = new Paragraph("Phone: 9486613520", headerRegular);
        leftCell.addElement(phone);

        Paragraph email = new Paragraph("Email: mrkphysics1966@gmail.com", headerRegular);
        leftCell.addElement(email);

        // Right side: Logo placeholder
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBackgroundColor(PRIMARY_BLUE);
        rightCell.setPadding(15);
        rightCell.setBorder(PdfPCell.NO_BORDER);
        rightCell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);

        Paragraph logoPlaceholder = new Paragraph("[LOGO]", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, SECONDARY_BLUE));
        logoPlaceholder.setAlignment(Element.ALIGN_CENTER);
        rightCell.addElement(logoPlaceholder);

        headerTable.addCell(leftCell);
        headerTable.addCell(rightCell);
        headerTable.setTotalWidth(document.getPageSize().getWidth() - 80);

        document.add(headerTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Add receipt title
     */
    private void addTitle(Document document) throws DocumentException {
        Paragraph title = new Paragraph("FEE RECEIPT", titleBold);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
    }

    /**
     * Add receipt metadata (receipt number and date)
     */
    private void addReceiptMetadata(Document document, Receipt receipt) throws DocumentException {
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);

        // Receipt Number
        PdfPCell receiptNoLabel = createLabelCell("Receipt No:", labelBold);
        receiptNoLabel.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        
        String receiptNo = generateReceiptNumber(receipt);
        PdfPCell receiptNoValue = createValueCell(receiptNo, bodyRegular);
        receiptNoValue.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);

        // Date
        PdfPCell dateLabel = createLabelCell("Date:", labelBold);
        dateLabel.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
        
        String date = formatDate(new Date());
        PdfPCell dateValue = createValueCell(date, bodyRegular);
        dateValue.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);

        metaTable.addCell(receiptNoLabel);
        metaTable.addCell(receiptNoValue);
        metaTable.addCell(dateLabel);
        metaTable.addCell(dateValue);

        document.add(metaTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Add student details section
     */
    private void addStudentDetailsSection(Document document, Receipt receipt) throws DocumentException {
        // Section title
        Paragraph sectionTitle = new Paragraph("STUDENT DETAILS", sectionBold);
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        // Student details table
        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100);

        addDetailRow(detailsTable, "Student Name:", receipt.getStudentName());
        addDetailRow(detailsTable, "Student ID:", receipt.getStudentId());
        addDetailRow(detailsTable, "Class/Standard:", receipt.getClassName());
        addDetailRow(detailsTable, "Batch Name:", receipt.getBatchName());
        addDetailRow(detailsTable, "Subject:", receipt.getSubjectName());

        document.add(detailsTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Add payment details section with table
     */
    private void addPaymentDetailsSection(Document document, Receipt receipt) throws DocumentException {
        // Section title
        Paragraph sectionTitle = new Paragraph("PAYMENT DETAILS", sectionBold);
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        // Payment details table
        PdfPTable paymentTable = new PdfPTable(2);
        paymentTable.setWidthPercentage(100);

        addDetailRow(paymentTable, "Amount Paid:", "₹" + String.format("%.2f", receipt.getAmount()));
        addDetailRow(paymentTable, "Payment Date:", receipt.getPaymentDate());
        addDetailRow(paymentTable, "Payment Mode:", receipt.getPaymentMode());

        document.add(paymentTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Add status badge
     */
    private void addStatusBadge(Document document, Receipt receipt) throws DocumentException {
        PdfPTable statusTable = new PdfPTable(1);
        statusTable.setWidthPercentage(40);

        PdfPCell statusCell = new PdfPCell();
        
        // Determine color based on status
        String status = receipt.getStatus() != null ? receipt.getStatus() : "PAID";
        BaseColor badgeColor = status.equalsIgnoreCase("PAID") ? ACCENT_GREEN : ACCENT_ORANGE;
        
        statusCell.setBackgroundColor(badgeColor);
        statusCell.setPadding(10);
        statusCell.setBorder(PdfPCell.NO_BORDER);
        statusCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);

        Font badgeTextFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
        Paragraph statusText = new Paragraph("✓ " + status, badgeTextFont);
        statusCell.addElement(statusText);

        statusTable.addCell(statusCell);
        statusTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        document.add(statusTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Add footer
     */
    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph("\n"));
        
        Paragraph footerText = new Paragraph("Thank you for your payment!", 
            new Font(Font.FontFamily.HELVETICA, 11, Font.ITALIC, TEXT_LIGHT));
        footerText.setAlignment(Element.ALIGN_CENTER);
        document.add(footerText);
    }


    /**
     * Add a detail row to a table
     */
    private void addDetailRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = createLabelCell(label, labelBold);
        PdfPCell valueCell = createValueCell(value, bodyRegular);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    /**
     * Create a styled label cell
     */
    private PdfPCell createLabelCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBackgroundColor(ROW_ALT);
        cell.setPadding(8);
        cell.setBorderColor(BORDER_LIGHT);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        return cell;
    }

    /**
     * Create a styled value cell
     */
    private PdfPCell createValueCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBackgroundColor(BaseColor.WHITE);
        cell.setPadding(8);
        cell.setBorderColor(BORDER_LIGHT);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
        return cell;
    }

    /**
     * Generate professional receipt number
     */
    private String generateReceiptNumber(Receipt receipt) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy");
        String year = dateFormat.format(new Date());
        
        // Generate sequence number based on student ID hash
        int sequence = Math.abs(receipt.getStudentId().hashCode()) % 10000;
        
        return String.format("RCP-%s-%05d", year, sequence);
    }

    /**
     * Format date in professional format
     */
    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");
        return sdf.format(date);
    }

    /**
     * Generate professional filename
     */
    private String generateFileName(Receipt receipt) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = dateFormat.format(new Date());
        String sanitizedId = receipt.getStudentId().replaceAll("[^a-zA-Z0-9]", "");
        return String.format("Receipt_%s_%s.pdf", sanitizedId, dateStr);
    }
}
