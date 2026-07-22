package com.transitea.service.impl;

import com.transitea.config.ProprietesWhatsApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    private WhatsAppServiceImpl whatsAppService;

    @BeforeEach
    void initialiser() {
        ProprietesWhatsApp proprietes = new ProprietesWhatsApp(
                "token-test", "123456789", "https://graph.facebook.com/v18.0");
        whatsAppService = new WhatsAppServiceImpl(restTemplate, proprietes);
    }

    @Test
    void doit_retourner_true_quand_api_repond_200() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));

        boolean resultat = whatsAppService.envoyerMessage("+243900000011", "Votre colis est arrive");

        assertThat(resultat).isTrue();
    }

    @Test
    void doit_retourner_false_quand_api_repond_en_erreur() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST));

        boolean resultat = whatsAppService.envoyerMessage("+243900000011", "Votre colis est arrive");

        assertThat(resultat).isFalse();
    }

    @Test
    void doit_retourner_false_quand_exception_reseau() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Timeout"));

        boolean resultat = whatsAppService.envoyerMessage("+243900000011", "Votre colis est arrive");

        assertThat(resultat).isFalse();
    }

    @Test
    void doit_retourner_false_quand_telephone_absent() {
        boolean resultat = whatsAppService.envoyerMessage(null, "Votre colis est arrive");

        assertThat(resultat).isFalse();
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void doit_retourner_false_quand_whatsapp_non_configure() {
        ProprietesWhatsApp proprietesVides = new ProprietesWhatsApp("", "", "https://graph.facebook.com/v18.0");
        WhatsAppServiceImpl serviceNonConfigure = new WhatsAppServiceImpl(restTemplate, proprietesVides);

        boolean resultat = serviceNonConfigure.envoyerMessage("+243900000011", "Message");

        assertThat(resultat).isFalse();
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    // --- envoyerImage (QR code) ---

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void doit_retourner_true_quand_upload_et_envoi_image_reussissent() {
        ResponseEntity<WhatsAppServiceImpl.MediaUploadReponse> uploadReponse =
                new ResponseEntity<>(new WhatsAppServiceImpl.MediaUploadReponse("media-123"), HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class)))
                .thenReturn((ResponseEntity) uploadReponse)
                .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));

        boolean resultat = whatsAppService.envoyerImage("+243900000011", new byte[]{1, 2, 3}, "Legende");

        assertThat(resultat).isTrue();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void doit_retourner_false_quand_upload_media_echoue() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class)))
                .thenReturn((ResponseEntity) new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        boolean resultat = whatsAppService.envoyerImage("+243900000011", new byte[]{1, 2, 3}, "Legende");

        assertThat(resultat).isFalse();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void doit_retourner_false_quand_envoi_du_message_image_echoue_apres_upload_reussi() {
        ResponseEntity<WhatsAppServiceImpl.MediaUploadReponse> uploadReponse =
                new ResponseEntity<>(new WhatsAppServiceImpl.MediaUploadReponse("media-123"), HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any(Class.class)))
                .thenReturn((ResponseEntity) uploadReponse)
                .thenReturn(new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST));

        boolean resultat = whatsAppService.envoyerImage("+243900000011", new byte[]{1, 2, 3}, "Legende");

        assertThat(resultat).isFalse();
    }

    @Test
    void doit_retourner_false_quand_image_absente() {
        boolean resultat = whatsAppService.envoyerImage("+243900000011", null, "Legende");

        assertThat(resultat).isFalse();
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), any(Class.class));
    }

    @Test
    void doit_retourner_false_quand_envoi_image_et_whatsapp_non_configure() {
        ProprietesWhatsApp proprietesVides = new ProprietesWhatsApp("", "", "https://graph.facebook.com/v18.0");
        WhatsAppServiceImpl serviceNonConfigure = new WhatsAppServiceImpl(restTemplate, proprietesVides);

        boolean resultat = serviceNonConfigure.envoyerImage("+243900000011", new byte[]{1, 2, 3}, "Legende");

        assertThat(resultat).isFalse();
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), any(Class.class));
    }
}
