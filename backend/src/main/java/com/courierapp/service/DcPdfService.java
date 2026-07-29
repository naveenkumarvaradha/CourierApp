package com.courierapp.service;

import com.courierapp.entity.CompanySettings;
import com.courierapp.entity.DeliveryChallan;
import com.courierapp.entity.Party;
import com.courierapp.entity.Unit;
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
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class DcPdfService {

    private static final float MARGIN = 30f;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    private static final Font F_TITLE  = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font F_CO     = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font F_LBL    = new Font(Font.HELVETICA, 8,  Font.BOLD, new Color(80, 80, 80));
    private static final Font F_VAL    = new Font(Font.HELVETICA, 9,  Font.NORMAL);
    private static final Font F_VAL_B  = new Font(Font.HELVETICA, 9,  Font.BOLD);
    private static final Font F_SECT   = new Font(Font.HELVETICA, 9,  Font.BOLD, new Color(60, 60, 60));

    /**
     * A4-portrait Delivery Challan document.
     *  ┌──────────────────────────────────────────────┐
     *  │ [logo]         DELIVERY CHALLAN               │  ← HEADER
     *  │                Company Name                   │
     *  ├────────────────────┬──────────────────────────┤
     *  │ DC No: …            │ Date: …                 │
     *  │ Booking No: …       │ Status: …                │
     *  ├────────────────────┬──────────────────────────┤
     *  │ FROM (UNIT)         │ TO (RECEIVER)            │
     *  ├──────────────────────────────────────────────┤
     *  │ Item / Qty / Weight / Package Type / Mode      │
     *  ├──────────────────────────────────────────────┤
     *  │ Vehicle No / Driver Name                       │
     *  ├──────────────────────────────────────────────┤
     *  │ Remarks                                        │
     *  └──────────────────────────────────────────────┘
     */
    public byte[] generate(DeliveryChallan dc, CompanySettings company) {
        try {
            Document doc = new Document(com.lowagie.text.PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ── HEADER ──────────────────────────────────────────────────────
            boolean hasLogo = company != null && company.getLogoData() != null && company.getLogoData().length > 0;
            PdfPTable hdrTable = hasLogo ? new PdfPTable(new float[]{20, 80}) : new PdfPTable(1);
            hdrTable.setWidthPercentage(100);
            hdrTable.setSpacingAfter(10);

            if (hasLogo) {
                PdfPCell logoCell = new PdfPCell();
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                try {
                    Image logo = Image.getInstance(company.getLogoData());
                    logo.scaleToFit(60, 45);
                    logoCell.addElement(logo);
                } catch (Exception ignored) {}
                hdrTable.addCell(logoCell);
            }
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph titlePara = new Paragraph();
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.add(new Chunk("DELIVERY CHALLAN\n", F_TITLE));
            if (company != null && company.getCompanyName() != null) {
                titlePara.add(new Chunk(company.getCompanyName(), F_CO));
            }
            titleCell.addElement(titlePara);
            hdrTable.addCell(titleCell);
            doc.add(hdrTable);

            // ── DC NO / DATE / BOOKING NO / STATUS ───────────────────────────
            PdfPTable metaRow = new PdfPTable(new float[]{50, 50});
            metaRow.setWidthPercentage(100);
            metaRow.setSpacingAfter(10);
            metaRow.addCell(metaCell("DC No.", dc.getDcNumber()));
            metaRow.addCell(metaCell("Date", dc.getDcDate().format(DATE_FMT)));
            metaRow.addCell(metaCell("Booking No.", dc.getBooking().getBookingNumber()));
            metaRow.addCell(metaCell("Status", dc.getStatus().name()));
            doc.add(metaRow);

            // ── FROM | TO ─────────────────────────────────────────────────
            PdfPTable fromTo = new PdfPTable(new float[]{50, 50});
            fromTo.setWidthPercentage(100);
            fromTo.setSpacingAfter(10);

            Unit unit = dc.getUnit();
            PdfPCell fromCell = new PdfPCell();
            fromCell.setPadding(6);
            Paragraph fromPara = new Paragraph();
            fromPara.add(new Chunk("FROM (UNIT)\n", F_SECT));
            if (unit != null) {
                fromPara.add(new Chunk(unit.getUnitName() + "\n", F_VAL_B));
                fromPara.add(new Chunk(unit.getAddressLine1(), F_VAL));
                if (unit.getAddressLine2() != null && !unit.getAddressLine2().isBlank()) {
                    fromPara.add(new Chunk(", " + unit.getAddressLine2(), F_VAL));
                }
                fromPara.add(new Chunk("\n" + unit.getCity() + " - " + unit.getPincode()
                        + ", " + unit.getState() + "\n" + unit.getCountry(), F_VAL));
                if (unit.getPhone() != null && !unit.getPhone().isBlank()) {
                    fromPara.add(new Chunk("\nPh: " + unit.getPhone(), F_VAL));
                }
                if (unit.getGstin() != null && !unit.getGstin().isBlank()) {
                    fromPara.add(new Chunk("\nGSTIN: " + unit.getGstin(), F_VAL));
                }
            }
            fromCell.addElement(fromPara);
            fromTo.addCell(fromCell);

            Party receiver = dc.getBooking().getReceiver();
            PdfPCell toCell = new PdfPCell();
            toCell.setPadding(6);
            Paragraph toPara = new Paragraph();
            toPara.add(new Chunk("TO (RECEIVER)\n", F_SECT));
            if (receiver.getCompanyName() != null && !receiver.getCompanyName().isBlank()) {
                toPara.add(new Chunk(receiver.getCompanyName() + "\n", F_VAL));
            }
            toPara.add(new Chunk(receiver.getPartyName() + "\n", F_VAL_B));
            toPara.add(new Chunk(receiver.getAddressLine1(), F_VAL));
            if (receiver.getAddressLine2() != null && !receiver.getAddressLine2().isBlank()) {
                toPara.add(new Chunk(", " + receiver.getAddressLine2(), F_VAL));
            }
            toPara.add(new Chunk("\n" + receiver.getCity() + " - " + receiver.getPincode()
                    + ", " + receiver.getState() + "\n" + receiver.getCountry(), F_VAL));
            if (receiver.getPhone() != null && !receiver.getPhone().isBlank()) {
                toPara.add(new Chunk("\nPh: " + receiver.getPhone(), F_VAL));
            }
            if (receiver.getGstin() != null && !receiver.getGstin().isBlank()) {
                toPara.add(new Chunk("\nGSTIN: " + receiver.getGstin(), F_VAL));
            }
            toCell.addElement(toPara);
            fromTo.addCell(toCell);
            doc.add(fromTo);

            // ── ITEM / SHIPMENT DETAILS ───────────────────────────────────
            PdfPTable itemTable = new PdfPTable(new float[]{40, 20, 20, 20});
            itemTable.setWidthPercentage(100);
            itemTable.setSpacingAfter(10);
            itemTable.addCell(headerCell("Item Description"));
            itemTable.addCell(headerCell("Packages"));
            itemTable.addCell(headerCell("Weight (kg)"));
            itemTable.addCell(headerCell("Mode"));
            itemTable.addCell(valueCell(dc.getBooking().getItemDescription()));
            itemTable.addCell(valueCell(String.valueOf(dc.getBooking().getNoOfPackages())));
            itemTable.addCell(valueCell(String.valueOf(dc.getBooking().getWeightKg())));
            String mode = dc.getBooking().getCourierMode().name()
                    + (dc.getBooking().getCourierWay() != null ? " / " + dc.getBooking().getCourierWay().getName() : "");
            itemTable.addCell(valueCell(mode));
            doc.add(itemTable);

            // ── TRANSPORT DETAILS ─────────────────────────────────────────
            PdfPTable transportRow = new PdfPTable(new float[]{50, 50});
            transportRow.setWidthPercentage(100);
            transportRow.setSpacingAfter(10);
            transportRow.addCell(metaCell("Vehicle No.", dc.getVehicleNumber() != null ? dc.getVehicleNumber() : "-"));
            transportRow.addCell(metaCell("Driver Name", dc.getDriverName() != null ? dc.getDriverName() : "-"));
            doc.add(transportRow);

            // ── REMARKS ────────────────────────────────────────────────────
            if (dc.getRemarks() != null && !dc.getRemarks().isBlank()) {
                PdfPTable remarksRow = new PdfPTable(1);
                remarksRow.setWidthPercentage(100);
                remarksRow.addCell(metaCell("Remarks", dc.getRemarks()));
                doc.add(remarksRow);
            }

            doc.close();
            return baos.toByteArray();

        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate delivery challan PDF", e);
        }
    }

    private PdfPCell metaCell(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", F_LBL));
        p.add(new Chunk(value != null ? value : "-", F_VAL_B));
        cell.addElement(p);
        return cell;
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, F_LBL));
        cell.setPadding(5);
        cell.setBackgroundColor(new Color(240, 240, 240));
        return cell;
    }

    private PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text != null ? text : "-", F_VAL));
        cell.setPadding(5);
        return cell;
    }
}
