package com.transitea.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class FiltreLimitationDebitTest {

    private final FiltreLimitationDebit filtre = new FiltreLimitationDebit();

    @Test
    void doit_laisser_passer_les_requetes_sous_la_limite_globale() throws Exception {
        FilterChain chaine = mock(FilterChain.class);

        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/v1/agences");
            requete.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse reponse = new MockHttpServletResponse();

            filtre.doFilter(requete, reponse, chaine);

            assertThat(reponse.getStatus()).isEqualTo(200);
        }

        verify(chaine, times(100)).doFilter(any(), any());
    }

    @Test
    void doit_bloquer_la_101e_requete_globale_dans_la_meme_minute() throws Exception {
        FilterChain chaine = mock(FilterChain.class);

        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/v1/agences");
            requete.setRemoteAddr("10.0.0.2");
            filtre.doFilter(requete, new MockHttpServletResponse(), chaine);
        }

        MockHttpServletRequest requete101 = new MockHttpServletRequest("GET", "/v1/agences");
        requete101.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse reponse101 = new MockHttpServletResponse();

        filtre.doFilter(requete101, reponse101, chaine);

        assertThat(reponse101.getStatus()).isEqualTo(429);
        verify(chaine, times(100)).doFilter(any(), any());
    }

    @Test
    void doit_bloquer_la_11e_tentative_de_connexion_dans_la_meme_minute() throws Exception {
        FilterChain chaine = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest requete = new MockHttpServletRequest("POST", "/v1/auth/login");
            requete.setRemoteAddr("10.0.0.3");
            filtre.doFilter(requete, new MockHttpServletResponse(), chaine);
        }

        MockHttpServletRequest requete11 = new MockHttpServletRequest("POST", "/v1/auth/login");
        requete11.setRemoteAddr("10.0.0.3");
        MockHttpServletResponse reponse11 = new MockHttpServletResponse();

        filtre.doFilter(requete11, reponse11, chaine);

        assertThat(reponse11.getStatus()).isEqualTo(429);
        verify(chaine, times(10)).doFilter(any(), any());
    }

    @Test
    void doit_isoler_les_compteurs_par_adresse_ip() throws Exception {
        FilterChain chaine = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest requete = new MockHttpServletRequest("POST", "/v1/auth/login");
            requete.setRemoteAddr("10.0.0.4");
            filtre.doFilter(requete, new MockHttpServletResponse(), chaine);
        }

        MockHttpServletRequest requeteAutreIp = new MockHttpServletRequest("POST", "/v1/auth/login");
        requeteAutreIp.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse reponse = new MockHttpServletResponse();

        filtre.doFilter(requeteAutreIp, reponse, chaine);

        assertThat(reponse.getStatus()).isEqualTo(200);
    }
}
