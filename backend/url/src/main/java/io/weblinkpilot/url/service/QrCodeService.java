package io.weblinkpilot.url.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class QrCodeService {

  private static final int SIZE = 320;

  public byte[] generatePng(String content) {
    try {
      BitMatrix matrix =
          new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, SIZE, SIZE, hints());
      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
        return outputStream.toByteArray();
      }
    } catch (WriterException | IOException exception) {
      throw new IllegalStateException("Failed to generate QR code", exception);
    }
  }

  private Map<EncodeHintType, Object> hints() {
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.MARGIN, 2);
    return hints;
  }
}
