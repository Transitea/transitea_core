package com.transitea.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.transitea.exception.ErreurMetier;
import com.transitea.service.QrCodeService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    private static final int TAILLE_QR_CODE = 300;
    private static final String FORMAT_IMAGE = "PNG";

    @Override
    public byte[] generer(String contenu) {
        QRCodeWriter writer = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 2
        );

        try {
            BitMatrix bitMatrix = writer.encode(
                    contenu, BarcodeFormat.QR_CODE, TAILLE_QR_CODE, TAILLE_QR_CODE, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, FORMAT_IMAGE, outputStream);
            return outputStream.toByteArray();

        } catch (WriterException | IOException e) {
            throw new ErreurMetier("Erreur lors de la generation du QR code : " + e.getMessage());
        }
    }
}
