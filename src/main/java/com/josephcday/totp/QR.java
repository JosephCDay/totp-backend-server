package com.josephcday.totp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import io.vertx.core.buffer.Buffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * QR generator utility
 */
public class QR {
    private static final QRCodeWriter WRITER = new QRCodeWriter();

    /**
     * Generate a QR Code image
     * 
     * @param text text to embed into a QR Code image
     * @return buffer containing a PNG for writing to a vertx HTTP routing context
     *         response.
     * @throws WriterException exception
     * @throws IOException     exception
     */
    public static Buffer generateQRCodeImage(String text) throws WriterException, IOException {
        return generateQRCodeImage(text, 128);
    }

    /**
     * Generate a QR Code image
     * 
     * @param text text to embed into a QR Code image
     * @param size edge dimension in pixels for the generated QR code.
     * @return buffer containing a PNG of a QRCode for writing to a vertx HTTP
     *         routing context response.
     * @throws WriterException exception
     * @throws IOException     exception
     */
    public static Buffer generateQRCodeImage(String text, int size) throws WriterException, IOException {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BitMatrix bitMatrix = WRITER.encode(text, BarcodeFormat.QR_CODE, size, size);
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", stream);
        Buffer buffer = Buffer.buffer(stream.toByteArray());
        return buffer;
    }
}
