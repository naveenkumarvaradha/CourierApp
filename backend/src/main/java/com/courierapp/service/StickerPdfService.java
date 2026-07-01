package com.courierapp.service;

import com.courierapp.entity.Booking;
import com.courierapp.entity.Party;
import com.courierapp.util.BarcodeGenerator;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Generates a 4x6 inch shipping-label PDF for a booking.
 */
@Service
public class StickerPdfService {

    // 4 x 6 inch in points (72 pt = 1 inch)
    private static final float WIDTH = 4 * 72f;
    private static final float HEIGHT = 6 * 72f;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    private static final Font TITLE = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font LABEL = new Font(Font.HELVETICA, 7, Font.BOLD);
    private static final Font VALUE = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font BIG = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font SMALL = new Font(Font.HELVETICA, 8, Font.NORMAL);

    public byte[] generate(Booking booking) {
        try {
            Rectangle pageSize = new Rectangle(WIDTH, HEIGHT);
            Document document = new Document(pageSize, 12, 12, 12, 12);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            // Header
            Paragraph header = new Paragraph("COURIER SHIPPING LABEL", TITLE);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            String wayLabel = booking.getCourierWay() != null ? "  via " + booking.getCourierWay().getName() : "";
            Paragraph mode = new Paragraph(booking.getCourierMode().name() + wayLabel + "  |  "
                    + booking.getPaymentMode().name(), BIG);
            mode.setAlignment(Element.ALIGN_CENTER);
            mode.setSpacingAfter(6);
            document.add(mode);

            document.add(divider());

            // Sender / Receiver blocks
            document.add(addressBlock("FROM (SENDER)", booking.getSender()));
            document.add(Chunk.NEWLINE);
            document.add(addressBlock("TO (RECEIVER)", booking.getReceiver()));

            document.add(divider());

            // Shipment details table
            PdfPTable details = new PdfPTable(2);
            details.setWidthPercentage(100);
            details.setSpacingBefore(4);
            addDetail(details, "Booking Date", booking.getBookingDate().format(DATE_FMT));
            addDetail(details, "Weight (kg)", String.valueOf(booking.getWeightKg()));
            addDetail(details, "Packages", String.valueOf(booking.getNoOfPackages()));
            addDetail(details, "Mode", booking.getCourierMode().name());
            addDetail(details, "Item", booking.getItemDescription());
            addDetail(details, "Total Charges", String.valueOf(booking.getTotalCharges()));
            document.add(details);

            document.add(divider());

            // AWB number — large, prominent
            Paragraph awbLabel = new Paragraph("AWB NO.", LABEL);
            awbLabel.setAlignment(Element.ALIGN_CENTER);
            document.add(awbLabel);

            Font awbFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Paragraph awbValue = new Paragraph(booking.getAwbNumber(), awbFont);
            awbValue.setAlignment(Element.ALIGN_CENTER);
            awbValue.setSpacingAfter(4);
            document.add(awbValue);

            document.add(divider());

            // Barcode of AWB number + booking reference below
            byte[] barcodePng = BarcodeGenerator.code128Png(booking.getAwbNumber(), 480, 120);
            Image barcode = Image.getInstance(barcodePng);
            barcode.scaleToFit(WIDTH - 40, 80);
            barcode.setAlignment(Element.ALIGN_CENTER);
            document.add(barcode);

            Paragraph number = new Paragraph("Booking Ref: " + booking.getBookingNumber(), SMALL);
            number.setAlignment(Element.ALIGN_CENTER);
            document.add(number);

            document.close();
            return baos.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("Failed to generate sticker PDF", e);
        }
    }

    private Paragraph addressBlock(String title, Party party) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(title + "\n", LABEL));
        p.add(new Chunk(party.getPartyName() + "\n", BIG));
        StringBuilder addr = new StringBuilder(party.getAddressLine1());
        if (party.getAddressLine2() != null && !party.getAddressLine2().isBlank()) {
            addr.append(", ").append(party.getAddressLine2());
        }
        p.add(new Chunk(addr + "\n", VALUE));
        p.add(new Chunk(party.getCity() + ", " + party.getState() + " - "
                + party.getPincode() + "\n", VALUE));
        p.add(new Chunk(party.getCountry() + "\n", VALUE));
        if (party.getPhone() != null && !party.getPhone().isBlank()) {
            p.add(new Chunk("Ph: " + party.getPhone(), SMALL));
        }
        return p;
    }

    private void addDetail(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(2);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, VALUE));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(2);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private Paragraph divider() {
        Paragraph p = new Paragraph();
        p.add(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(0.8f, 100, null, Element.ALIGN_CENTER, -2)));
        p.setSpacingBefore(4);
        p.setSpacingAfter(4);
        return p;
    }
}
