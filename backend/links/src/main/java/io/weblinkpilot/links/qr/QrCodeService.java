package io.weblinkpilot.links.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import io.weblinkpilot.links.config.ShortLinkProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class QrCodeService {

  private final int size;
  private final int margin;
  private final String imageFormat;

  public QrCodeService(ShortLinkProperties properties) {
    this.size = properties.getQr().getSize();
    this.margin = properties.getQr().getMargin();
    this.imageFormat = properties.getQr().getImageFormat();
  }

  public byte[] generatePng(String content) {
    try {
      BitMatrix matrix =
          new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints());
      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        MatrixToImageWriter.writeToStream(matrix, imageFormat, outputStream);
        return outputStream.toByteArray();
      }
    } catch (WriterException | IOException exception) {
      throw new IllegalStateException("Failed to generate QR code", exception);
    }
  }

  private Map<EncodeHintType, Object> hints() {
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.MARGIN, margin);
    return hints;
  }
}
