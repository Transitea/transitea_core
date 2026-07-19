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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    private static final Logger journal = LoggerFactory.getLogger(QrCodeServiceImpl.class);

    private static final int TAILLE_QR_CODE = 300;
    private static final String FORMAT_IMAGE = "PNG";

    @Value("${application.qrcode.cache-repertoire:./cache/qrcodes}")
    private String cacheRepertoire;

    @Value("${application.qrcode.cache-duree-jours:90}")
    private int cacheDureeJours;

    @Override
    public byte[] generer(String contenu, String cleCache) {
        Path fichierCache = cheminCache(cleCache);

        if (fichierCache != null) {
            byte[] depuisCache = lireDepuisCache(fichierCache);
            if (depuisCache != null) {
                return depuisCache;
            }
        }

        byte[] image = genererImage(contenu);

        if (fichierCache != null) {
            ecrireDansCache(fichierCache, image);
        }

        return image;
    }

    private byte[] genererImage(String contenu) {
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

    private Path cheminCache(String cleCache) {
        if (cleCache == null || cleCache.isBlank()) {
            return null;
        }
        String nomFichier = cleCache.replaceAll("[^a-zA-Z0-9_-]", "_") + ".png";
        return Path.of(cacheRepertoire, nomFichier);
    }

    private byte[] lireDepuisCache(Path fichierCache) {
        try {
            if (!Files.exists(fichierCache)) {
                return null;
            }

            Instant dateModification = Files.getLastModifiedTime(fichierCache).toInstant();
            if (Duration.between(dateModification, Instant.now()).toDays() > cacheDureeJours) {
                Files.deleteIfExists(fichierCache);
                return null;
            }

            return Files.readAllBytes(fichierCache);

        } catch (IOException e) {
            journal.warn("Lecture du cache QR code impossible ({}) : {}", fichierCache, e.getMessage());
            return null;
        }
    }

    private void ecrireDansCache(Path fichierCache, byte[] image) {
        try {
            Files.createDirectories(fichierCache.getParent());
            Files.write(fichierCache, image);
        } catch (IOException e) {
            journal.warn("Ecriture du cache QR code impossible ({}) : {}", fichierCache, e.getMessage());
        }
    }
}
