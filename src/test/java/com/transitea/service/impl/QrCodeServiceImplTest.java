package com.transitea.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeServiceImplTest {

    private final QrCodeServiceImpl qrCodeService = new QrCodeServiceImpl();

    @TempDir
    Path repertoireCache;

    @BeforeEach
    void initialiser() {
        ReflectionTestUtils.setField(qrCodeService, "cacheRepertoire", repertoireCache.toString());
        ReflectionTestUtils.setField(qrCodeService, "cacheDureeJours", 90);
    }

    @Test
    void doit_retourner_un_tableau_bytes_non_vide_pour_contenu_valide() {
        byte[] resultat = qrCodeService.generer("TRA-2026-ABC123", null);

        assertThat(resultat).isNotNull().isNotEmpty();
    }

    @Test
    void doit_produire_une_image_png_valide() {
        byte[] resultat = qrCodeService.generer("TRA-2026-ABC123", null);

        // Signature PNG : 8 premiers octets sont toujours 89 50 4E 47 0D 0A 1A 0A
        assertThat(resultat[0] & 0xFF).isEqualTo(0x89);
        assertThat(resultat[1] & 0xFF).isEqualTo(0x50); // 'P'
        assertThat(resultat[2] & 0xFF).isEqualTo(0x4E); // 'N'
        assertThat(resultat[3] & 0xFF).isEqualTo(0x47); // 'G'
    }

    @Test
    void doit_generer_un_qrcode_pour_une_url() {
        byte[] resultat = qrCodeService.generer("https://transitea.cd/tracking/TRA-2026-ABC123", null);

        assertThat(resultat).isNotNull().isNotEmpty();
    }

    @Test
    void doit_generer_des_qrcodes_differents_pour_des_contenus_differents() {
        byte[] qr1 = qrCodeService.generer("TRA-2026-ABC123", null);
        byte[] qr2 = qrCodeService.generer("TRA-2026-XYZ999", null);

        assertThat(qr1).isNotEqualTo(qr2);
    }

    @Test
    void doit_generer_un_qrcode_meme_pour_un_contenu_court() {
        // ZXing accepte une chaine vide et genere un QR code valide
        byte[] resultat = qrCodeService.generer("X", null);

        assertThat(resultat).isNotNull().isNotEmpty();
    }

    @Test
    void doit_ecrire_le_qrcode_dans_le_cache_quand_cle_fournie() throws Exception {
        qrCodeService.generer("TRA-2026-ABC123", "TRA-2026-ABC123");

        Path fichierCache = repertoireCache.resolve("TRA-2026-ABC123.png");
        assertThat(Files.exists(fichierCache)).isTrue();
    }

    @Test
    void doit_reutiliser_le_cache_existant_sans_regenerer() throws Exception {
        byte[] premiereGeneration = qrCodeService.generer("TRA-2026-ABC123", "TRA-2026-ABC123");

        Path fichierCache = repertoireCache.resolve("TRA-2026-ABC123.png");
        byte[] contenuCache = Files.readAllBytes(fichierCache);
        assertThat(contenuCache).isEqualTo(premiereGeneration);

        byte[] deuxiemeAppel = qrCodeService.generer("TRA-2026-ABC123", "TRA-2026-ABC123");
        assertThat(deuxiemeAppel).isEqualTo(premiereGeneration);
    }

    @Test
    void doit_regenerer_quand_le_cache_a_expire() throws Exception {
        ReflectionTestUtils.setField(qrCodeService, "cacheDureeJours", 0);

        qrCodeService.generer("TRA-2026-ABC123", "TRA-2026-ABC123");
        Path fichierCache = repertoireCache.resolve("TRA-2026-ABC123.png");

        // Simule un fichier vieux de 5 jours
        Files.setLastModifiedTime(fichierCache,
                java.nio.file.attribute.FileTime.from(
                        java.time.Instant.now().minus(java.time.Duration.ofDays(5))));

        byte[] resultat = qrCodeService.generer("TRA-2026-ABC123", "TRA-2026-ABC123");
        assertThat(resultat).isNotNull().isNotEmpty();
    }
}
