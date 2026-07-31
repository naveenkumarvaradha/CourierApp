package com.courierapp.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;

public final class BarcodeGenerator {

    private BarcodeGenerator() {
    }

    /**
     * Generates a Code128 barcode PNG for the given content.
     */
    public static byte[] code128Png(String content, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 2);
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.CODE_128, width, height, hints);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate barcode for: " + content, e);
        }
    }
}
