package com.transitea.service.impl;

import com.transitea.config.ProprietesWhatsApp;
import com.transitea.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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

    private String normaliserTelephone(String telephone) {
        return telephone.replaceAll("[^0-9]", "");
    }
}
