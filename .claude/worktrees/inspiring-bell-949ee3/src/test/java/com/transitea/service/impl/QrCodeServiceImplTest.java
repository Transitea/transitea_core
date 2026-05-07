package com.transitea.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrCodeServiceImplTest {

    private final QrCodeServiceImpl qrCodeService = new QrCodeServiceImpl();

    @Test
    void doit_retourner_un_tableau_bytes_non_vide_pour_contenu_valide() {
        byte[] resultat = qrCodeService.generer("TRA-2026-ABC123");

        assertThat(resultat).isNotNull().isNotEmpty();
    }

    @Test
    void doit_produire_une_image_png_valide() {
        byte[] resultat = qrCodeService.generer("TRA-2026-ABC123");

        // Signature PNG : 8 premiers octets sont toujours 89 50 4E 47 0D 0A 1A 0A
        assertThat(resultat[0] & 0xFF).isEqualTo(0x89);
        assertThat(resultat[1] & 0xFF).isEqualTo(0x50); // 'P'
        assertThat(resultat[2] & 0xFF).isEqualTo(0x4E); // 'N'
        assertThat(resultat[3] & 0xFF).isEqualTo(0x47); // 'G'
    }

    @Test
    void doit_generer_un_qrcode_pour_une_url() {
        byte[] resultat = qrCodeService.generer("https://transitea.cd/tracking/TRA-2026-ABC123");

        assertThat(resultat).isNotNull().isNotEmpty();
    }

    @Test
    void doit_generer_des_qrcodes_differents_pour_des_contenus_differents() {
        byte[] qr1 = qrCodeService.generer("TRA-2026-ABC123");
        byte[] qr2 = qrCodeService.generer("TRA-2026-XYZ999");

        assertThat(qr1).isNotEqualTo(qr2);
    }

    @Test
    void doit_generer_un_qrcode_meme_pour_un_contenu_court() {
        // ZXing accepte une chaine vide et genere un QR code valide
        byte[] resultat = qrCodeService.generer("X");

        assertThat(resultat).isNotNull().isNotEmpty();
    }
}
