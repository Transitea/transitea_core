package com.transitea.service.impl;

import com.transitea.config.ProprietesWhatsApp;
import com.transitea.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    private static final Logger journal = LoggerFactory.getLogger(WhatsAppServiceImpl.class);

    private final RestTemplate restTemplate;
    private final ProprietesWhatsApp proprietesWhatsApp;

    public WhatsAppServiceImpl(RestTemplate restTemplate, ProprietesWhatsApp proprietesWhatsApp) {
        this.restTemplate = restTemplate;
        this.proprietesWhatsApp = proprietesWhatsApp;
    }

    @Override
    public boolean envoyerMessage(String telephoneDestinataire, String message) {
        if (!proprietesWhatsApp.estConfigure()) {
            journal.debug("WhatsApp non configure (token/phoneNumberId absent), envoi ignore");
            return false;
        }

        if (telephoneDestinataire == null || telephoneDestinataire.isBlank()) {
            return false;
        }

        String telephoneNormalise = normaliserTelephone(telephoneDestinataire);
        String url = proprietesWhatsApp.apiUrl() + "/" + proprietesWhatsApp.phoneNumberId() + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(proprietesWhatsApp.token());

        Map<String, Object> corps = Map.of(
                "messaging_product", "whatsapp",
                "to", telephoneNormalise,
                "type", "text",
                "text", Map.of("body", message)
        );

        try {
            ResponseEntity<String> reponse = restTemplate.postForEntity(
                    url, new HttpEntity<>(corps, headers), String.class);

            boolean succes = reponse.getStatusCode() == HttpStatus.OK;
            if (succes) {
                journal.info("Message WhatsApp envoye a {}", telephoneNormalise);
            } else {
                journal.warn("Reponse inattendue de l'API WhatsApp pour {} : {}",
                        telephoneNormalise, reponse.getStatusCode());
            }
            return succes;

        } catch (RestClientException e) {
            journal.error("Echec envoi WhatsApp a {} : {}", telephoneNormalise, e.getMessage());
            return false;
        }
    }

    /**
     * Envoie une image WhatsApp (QR code de retrait, cf. CDC 9.2 : "Support
     * media... ideal pour transmettre le QR code"). L'API Meta n'accepte pas
     * de bytes bruts dans le message : il faut d'abord televerser le media
     * (endpoint /media) pour obtenir un media_id, puis envoyer un message de
     * type "image" le referencant.
     */
    @Override
    public boolean envoyerImage(String telephoneDestinataire, byte[] image, String legende) {
        if (!proprietesWhatsApp.estConfigure()) {
            journal.debug("WhatsApp non configure (token/phoneNumberId absent), envoi image ignore");
            return false;
        }

        if (telephoneDestinataire == null || telephoneDestinataire.isBlank()
                || image == null || image.length == 0) {
            return false;
        }

        String telephoneNormalise = normaliserTelephone(telephoneDestinataire);

        try {
            String mediaId = televerserMedia(image);
            if (mediaId == null) {
                return false;
            }
            return envoyerMessageImage(telephoneNormalise, mediaId, legende);

        } catch (RestClientException e) {
            journal.error("Echec envoi image WhatsApp a {} : {}", telephoneNormalise, e.getMessage());
            return false;
        }
    }

    private String televerserMedia(byte[] image) {
        String url = proprietesWhatsApp.apiUrl() + "/" + proprietesWhatsApp.phoneNumberId() + "/media";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(proprietesWhatsApp.token());

        MultiValueMap<String, Object> corps = new LinkedMultiValueMap<>();
        corps.add("messaging_product", "whatsapp");
        corps.add("type", "image/png");
        corps.add("file", new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                return "qrcode.png";
            }
        });

        ResponseEntity<MediaUploadReponse> reponse = restTemplate.postForEntity(
                url, new HttpEntity<>(corps, headers), MediaUploadReponse.class);

        if (reponse.getStatusCode() != HttpStatus.OK
                || reponse.getBody() == null || reponse.getBody().id() == null) {
            journal.warn("Echec upload media WhatsApp : {}", reponse.getStatusCode());
            return null;
        }

        return reponse.getBody().id();
    }

    private boolean envoyerMessageImage(String telephoneNormalise, String mediaId, String legende) {
        String url = proprietesWhatsApp.apiUrl() + "/" + proprietesWhatsApp.phoneNumberId() + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(proprietesWhatsApp.token());

        Map<String, Object> image = (legende != null && !legende.isBlank())
                ? Map.of("id", mediaId, "caption", legende)
                : Map.of("id", mediaId);

        Map<String, Object> corps = Map.of(
                "messaging_product", "whatsapp",
                "to", telephoneNormalise,
                "type", "image",
                "image", image
        );

        ResponseEntity<String> reponse = restTemplate.postForEntity(
                url, new HttpEntity<>(corps, headers), String.class);

        boolean succes = reponse.getStatusCode() == HttpStatus.OK;
        if (succes) {
            journal.info("Image WhatsApp envoyee a {}", telephoneNormalise);
        } else {
            journal.warn("Reponse inattendue de l'API WhatsApp (image) pour {} : {}",
                    telephoneNormalise, reponse.getStatusCode());
        }
        return succes;
    }

    private String normaliserTelephone(String telephone) {
        return telephone.replaceAll("[^0-9]", "");
    }

    // Package-private (pas private) pour rester instanciable depuis les tests unitaires.
    record MediaUploadReponse(String id) {
    }
}
