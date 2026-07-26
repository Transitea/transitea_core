package com.transitea.config;

import com.transitea.security.FiltreAuthentificationJwt;
import com.transitea.security.FiltreLimitationDebit;
import com.transitea.security.GestionnaireAccesRefuse;
import com.transitea.security.PointEntreeNonAutorise;
import com.transitea.security.ServiceDetailsUtilisateur;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ConfigurationSecurite {

    private final ServiceDetailsUtilisateur serviceDetailsUtilisateur;
    private final FiltreAuthentificationJwt filtreAuthentificationJwt;
    private final FiltreLimitationDebit filtreLimitationDebit;
    private final PointEntreeNonAutorise pointEntreeNonAutorise;
    private final GestionnaireAccesRefuse gestionnaireAccesRefuse;

    public ConfigurationSecurite(
            ServiceDetailsUtilisateur serviceDetailsUtilisateur,
            FiltreAuthentificationJwt filtreAuthentificationJwt,
            FiltreLimitationDebit filtreLimitationDebit,
            PointEntreeNonAutorise pointEntreeNonAutorise,
            GestionnaireAccesRefuse gestionnaireAccesRefuse) {
        this.serviceDetailsUtilisateur = serviceDetailsUtilisateur;
        this.filtreAuthentificationJwt = filtreAuthentificationJwt;
        this.filtreLimitationDebit = filtreLimitationDebit;
        this.pointEntreeNonAutorise = pointEntreeNonAutorise;
        this.gestionnaireAccesRefuse = gestionnaireAccesRefuse;
    }

    @Bean
    public SecurityFilterChain chaineFiltres(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/v1/auth/register",
                                "/v1/auth/login",
                                "/v1/auth/refresh",
                                "/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/tracking/**").permitAll()
                        // Public : necessaire pour peupler le choix d'agence sur la page d'inscription
                        .requestMatchers(HttpMethod.GET, "/v1/agences").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(pointEntreeNonAutorise)
                        .accessDeniedHandler(gestionnaireAccesRefuse)
                )
                .authenticationProvider(fournisseurAuthentification())
                .addFilterBefore(filtreAuthentificationJwt, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(filtreLimitationDebit, FiltreAuthentificationJwt.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173", "http://localhost:4173",
                "https://transitea.fr", "http://transitea.fr",
                "https://dev.transitea.fr"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider fournisseurAuthentification() {
        DaoAuthenticationProvider fournisseur = new DaoAuthenticationProvider();
        fournisseur.setUserDetailsService(serviceDetailsUtilisateur);
        fournisseur.setPasswordEncoder(encodeurMotDePasse());
        return fournisseur;
    }

    @Bean
    public AuthenticationManager gestionnaireAuthentification(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder encodeurMotDePasse() {
        return new BCryptPasswordEncoder();
    }
}
