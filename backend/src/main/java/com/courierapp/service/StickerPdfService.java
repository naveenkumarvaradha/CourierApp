package com.courierapp.service;

import com.courierapp.dto.admin.StickerFieldDto;
import com.courierapp.entity.Booking;
import com.courierapp.entity.CompanySettings;
import com.courierapp.entity.Party;
import com.courierapp.entity.User;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StickerPdfService {

    // 152 mm × 101 mm (6" × 4" landscape sticker)
    private static final float W      = 152 * 2.8346f;
    private static final float H      = 101 * 2.8346f;
    private static final float MARGIN = 6f;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font F_SHIP    = new Font(Font.HELVETICA, 9,  Font.BOLD);
    private static final Font F_CO      = new Font(Font.HELVETICA, 9,  Font.BOLD);
    private static final Font F_LBL     = new Font(Font.HELVETICA, 6,  Font.BOLD, new Color(80, 80, 80));
    private static final Font F_BNUM    = new Font(Font.HELVETICA, 7,  Font.BOLD);
    private static final Font F_MODE    = new Font(Font.HELVETICA, 7,  Font.NORMAL, new Color(60, 60, 60));
    private static final Font F_DVAL    = new Font(Font.HELVETICA, 8,  Font.BOLD);
    private static final Font F_DLBL    = new Font(Font.HELVETICA, 6,  Font.BOLD, new Color(100, 100, 100));
    private static final Font F_FROM_N  = new Font(Font.HELVETICA, 8,  Font.BOLD);
    private static final Font F_FROM_PH = new Font(Font.HELVETICA, 7,  Font.BOLD, new Color(50, 50, 50));
    private static final Font F_FROM_CO = new Font(Font.HELVETICA, 7,  Font.NORMAL);
    private static final Font F_FROM_B  = new Font(Font.HELVETICA, 7,  Font.NORMAL);
    private static final Font F_TO_CO   = new Font(Font.HELVETICA, 7,  Font.NORMAL, new Color(60, 60, 60));
    private static final Font F_TO_N    = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font F_TO_B    = new Font(Font.HELVETICA, 7,  Font.NORMAL);
    private static final Font F_AWB_L   = new Font(Font.HELVETICA, 7,  Font.BOLD, new Color(50, 50, 50));
    private static final Font F_AWB     = new Font(Font.HELVETICA, 16, Font.BOLD);

    /**
     * Sticker layout (top → bottom):
     *  ┌──────────────────────────────────────────────┐
     *  │ [logo] │  COURIER SHIPPING LABEL (center)    │  ← HEADER
     *  │        │  **Company Name** (bold, centered)  │
     *  ├────────────────────┬─────────────────────────┤
     *  │ Booking No: …      │ Date: 02-Jul-2026        │  ← DETAILS (left=booking | right=date/weight)
     *  │ SURFACE via DHL    │ Weight: 1.000 kg         │
     *  ├────────────────────────────────── divider ───┤
     *  │ FROM (SENDER)      │ TO (RECEIVER)           │  ← FROM | TO
     *  │ Creator Name (bold)│ Company / Name(big)     │  FROM order: name → mobile → company → address
     *  │ Mob: 9876543210    │ Address / Phone         │
     *  │ CTL India Pvt Ltd  │                         │
     *  │ Address...         │                         │
     *  ├────────────────────────────────── divider ───┤
     *  │ AWB NO.   3546463845241457687 (large)        │  ← BOTTOM (AWB full width)
     *  └──────────────────────────────────────────────┘
     */
    public byte[] generate(Booking booking, User creator, CompanySettings company,
                           List<StickerFieldDto> fields) {
        Set<String> visible = (fields == null || fields.isEmpty())
                ? null
                : fields.stream().filter(StickerFieldDto::visible)
                        .map(StickerFieldDto::fieldKey).collect(Collectors.toSet());

        try {
            Document doc = new Document(new Rectangle(W, H), MARGIN, MARGIN, MARGIN, MARGIN);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ── 1. HEADER ROW ─────────────────────────────────────────────────
            boolean hasLogo = company != null && company.getLogoData() != null
                    && company.getLogoData().length > 0;

            PdfPTable hdrTable = hasLogo
                    ? new PdfPTable(new float[]{18, 82})
                    : new PdfPTable(1);
            hdrTable.setWidthPercentage(100);
            hdrTable.setSpacingAfter(0);

            if (hasLogo) {
                PdfPCell logoCell = new PdfPCell();
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setBorderWidthBottom(0.4f);
                logoCell.setBorderColorBottom(Color.LIGHT_GRAY);
                logoCell.setPadding(1);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                try {
                    Image logo = Image.getInstance(company.getLogoData());
                    logo.scaleToFit(42, 32);
                    logoCell.addElement(logo);
                } catch (Exception ignored) {}
                hdrTable.addCell(logoCell);
            }
            hdrTable.addCell(headerTextCell(visible, company));
            doc.add(hdrTable);

            // ── 2. DETAILS ROW (left: booking/mode | right: date/weight) ─────
            PdfPTable detailRow = new PdfPTable(new float[]{52, 48});
            detailRow.setWidthPercentage(100);
            detailRow.setSpacingAfter(0);

            // Left: Booking No + Courier Mode
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setBorderWidthBottom(0.4f);
            leftCell.setBorderColorBottom(Color.LIGHT_GRAY);
            leftCell.setPadding(2);
            leftCell.setPaddingTop(1);
            Paragraph leftPara = new Paragraph();
            if (show(visible, "BOOKING_NUMBER")) {
                leftPara.add(new Chunk("Booking No: " + booking.getBookingNumber() + "\n", F_BNUM));
            }
            if (show(visible, "COURIER_MODE")) {
                String mode = booking.getCourierMode().name()
                        + (booking.getCourierWay() != null ? " via " + booking.getCourierWay().getName() : "")
                        + (booking.getPaymentMode() != null ? "  | " + booking.getPaymentMode().name() : "");
                leftPara.add(new Chunk(mode + "\n", F_MODE));
            }
            leftCell.addElement(leftPara);
            detailRow.addCell(leftCell);

            // Right: Date + Weight
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setBorderWidthBottom(0.4f);
            rightCell.setBorderColorBottom(Color.LIGHT_GRAY);
            rightCell.setPadding(2);
            rightCell.setPaddingTop(1);
            Paragraph rightPara = new Paragraph();
            if (show(visible, "DETAIL_DATE")) {
                rightPara.add(new Chunk("Date: ", F_DLBL));
                rightPara.add(new Chunk(booking.getBookingDate().format(DATE_FMT) + "\n", F_DVAL));
            }
            if (show(visible, "DETAIL_WEIGHT")) {
                rightPara.add(new Chunk("Weight: ", F_DLBL));
                rightPara.add(new Chunk(booking.getWeightKg() + " kg\n", F_DVAL));
            }
            if (show(visible, "DETAIL_PACKAGES")) {
                rightPara.add(new Chunk("Pkgs: ", F_DLBL));
                rightPara.add(new Chunk(String.valueOf(booking.getNoOfPackages()) + "\n", F_DVAL));
            }
            if (show(visible, "DETAIL_PKG_TYPE") && booking.getPackageType() != null) {
                rightPara.add(new Chunk("Type: ", F_DLBL));
                rightPara.add(new Chunk(booking.getPackageType().getName() + "\n", F_DVAL));
            }
            rightCell.addElement(rightPara);
            detailRow.addCell(rightCell);
            doc.add(detailRow);

            // ── 3. FROM | TO ──────────────────────────────────────────────────
            PdfPTable fromTo = new PdfPTable(new float[]{42, 58});
            fromTo.setWidthPercentage(100);
            fromTo.setSpacingAfter(0);

            // FROM cell — order: creator name → mobile → company → address
            PdfPCell fromCell = new PdfPCell();
            fromCell.setBorder(Rectangle.RIGHT);
            fromCell.setBorderColor(Color.LIGHT_GRAY);
            fromCell.setBorderWidthBottom(0.4f);
            fromCell.setBorderColorBottom(Color.LIGHT_GRAY);
            fromCell.setPadding(3);
            fromCell.setPaddingTop(2);
            Paragraph fromPara = new Paragraph();
            fromPara.add(new Chunk("FROM (SENDER)\n", F_LBL));
            if (show(visible, "FROM_NAME")) {
                String name = creator != null ? creator.getFullName()
                        : (booking.getCreatedBy() != null ? booking.getCreatedBy() : null);
                if (name != null) fromPara.add(new Chunk(name + "\n", F_FROM_N));
            }
            if (show(visible, "FROM_PHONE")) {
                String phone = (creator != null && creator.getPhone() != null && !creator.getPhone().isBlank())
                        ? creator.getPhone()
                        : (company != null && company.getPhone() != null ? company.getPhone() : null);
                if (phone != null && !phone.isBlank()) {
                    fromPara.add(new Chunk("Mob: " + phone + "\n", F_FROM_PH));
                }
            }
            if (show(visible, "SENDER_COMPANY") && company != null && company.getCompanyName() != null) {
                fromPara.add(new Chunk(company.getCompanyName() + "\n", F_FROM_CO));
            }
            if (show(visible, "FROM_ADDRESS") && company != null && company.getAddressLine1() != null) {
                fromPara.add(new Chunk(company.getAddressLine1(), F_FROM_B));
                if (company.getAddressLine2() != null && !company.getAddressLine2().isBlank()) {
                    fromPara.add(new Chunk(", " + company.getAddressLine2(), F_FROM_B));
                }
                fromPara.add(new Chunk("\n" + company.getCity() + " - " + company.getPincode()
                        + ", " + company.getState(), F_FROM_B));
            }
            fromCell.addElement(fromPara);
            fromTo.addCell(fromCell);

            // TO cell
            Party r = booking.getReceiver();
            PdfPCell toCell = new PdfPCell();
            toCell.setBorder(Rectangle.NO_BORDER);
            toCell.setBorderWidthBottom(0.4f);
            toCell.setBorderColorBottom(Color.LIGHT_GRAY);
            toCell.setPadding(3);
            toCell.setPaddingTop(2);
            Paragraph toPara = new Paragraph();
            toPara.add(new Chunk("TO (RECEIVER)\n", F_LBL));
            if (show(visible, "TO_COMPANY") && r.getCompanyName() != null && !r.getCompanyName().isBlank()) {
                toPara.add(new Chunk(r.getCompanyName() + "\n", F_TO_CO));
            }
            if (show(visible, "TO_NAME")) {
                toPara.add(new Chunk(r.getPartyName() + "\n", F_TO_N));
            }
            if (show(visible, "TO_ADDRESS")) {
                toPara.add(new Chunk(r.getAddressLine1(), F_TO_B));
                if (r.getAddressLine2() != null && !r.getAddressLine2().isBlank()) {
                    toPara.add(new Chunk(", " + r.getAddressLine2(), F_TO_B));
                }
                toPara.add(new Chunk("\n" + r.getCity() + " - " + r.getPincode()
                        + ", " + r.getState() + "\n" + r.getCountry(), F_TO_B));
            }
            if (show(visible, "TO_PHONE") && r.getPhone() != null && !r.getPhone().isBlank()) {
                toPara.add(new Chunk("\nPh: " + r.getPhone(), F_TO_B));
            }
            if (show(visible, "TO_GSTIN") && r.getGstin() != null && !r.getGstin().isBlank()) {
                toPara.add(new Chunk("  GSTIN: " + r.getGstin(), F_TO_B));
            }
            toCell.addElement(toPara);
            fromTo.addCell(toCell);
            doc.add(fromTo);

            // ── 4. BOTTOM: AWB Number (full width, large) ────────────────────
            if (show(visible, "AWB_NUMBER")) {
                PdfPTable awbRow = new PdfPTable(1);
                awbRow.setWidthPercentage(100);
                PdfPCell awbCell = new PdfPCell();
                awbCell.setBorder(Rectangle.NO_BORDER);
                awbCell.setPadding(2);
                awbCell.setPaddingTop(2);
                Paragraph awbPara = new Paragraph();
                awbPara.add(new Chunk("AWB NO.  ", F_AWB_L));
                awbPara.add(new Chunk(
                        booking.getAwbNumber() != null ? booking.getAwbNumber() : "—", F_AWB));
                awbCell.addElement(awbPara);
                awbRow.addCell(awbCell);
                doc.add(awbRow);
            }

            doc.close();
            return baos.toByteArray();

        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate sticker PDF", e);
        }
    }

    /** Backward-compat: no field config → show all fields */
    public byte[] generate(Booking booking, User creator, CompanySettings company) {
        return generate(booking, creator, company, null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PdfPCell headerTextCell(Set<String> visible, CompanySettings company) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderWidthBottom(0.4f);
        cell.setBorderColorBottom(Color.LIGHT_GRAY);
        cell.setPaddingTop(2);
        cell.setPaddingBottom(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        if (show(visible, "SHIPPING_LABEL")) {
            p.add(new Chunk("COURIER SHIPPING LABEL\n", F_SHIP));
        }
        if (show(visible, "COMPANY_NAME") && company != null && company.getCompanyName() != null) {
            // Extra spacing line before company name for visual breathing room
            p.add(new Chunk(company.getCompanyName(), F_CO));
        }
        cell.addElement(p);
        return cell;
    }

    private boolean show(Set<String> visible, String key) {
        return visible == null || visible.contains(key);
    }
}
