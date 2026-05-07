package com.transitea.security;

import com.transitea.entity.Utilisateur;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Service
public class ServiceJwt {

    private final SecretKey cleAccess;
    private final SecretKey cleRefresh;
    private final long expirationAccessMs;
    private final long expirationRefreshMs;

    public ServiceJwt(ProprietesJwt proprietes) {
        this.cleAccess = construireCle(proprietes.secretAccess());
        this.cleRefresh = construireCle(proprietes.secretRefresh());
        this.expirationAccessMs = proprietes.expirationAccessMs();
        this.expirationRefreshMs = proprietes.expirationRefreshMs();
    }

    public String genererAccessToken(Utilisateur utilisateur) {
        return Jwts.builder()
                .subject(utilisateur.getEmail())
                .claim("id", utilisateur.getId())
                .claim("role", utilisateur.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationAccessMs))
                .signWith(cleAccess)
                .compact();
    }

    public String genererRefreshToken(Utilisateur utilisateur) {
        return Jwts.builder()
                .subject(utilisateur.getEmail())
                .claim("id", utilisateur.getId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationRefreshMs))
                .signWith(cleRefresh)
                .compact();
    }

    public String extraireEmailAccessToken(String token) {
        return extraireClaims(token, cleAccess).getSubject();
    }

    public String extraireEmailRefreshToken(String token) {
        return extraireClaims(token, cleRefresh).getSubject();
    }

    public boolean validerAccessToken(String token) {
        try {
            extraireClaims(token, cleAccess);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validerRefreshToken(String token) {
        try {
            extraireClaims(token, cleRefresh);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims extraireClaims(String token, SecretKey cle) {
        return Jwts.parser()
                .verifyWith(cle)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static SecretKey construireCle(String secret) {
        try {
            byte[] octets = Base64.getDecoder().decode(secret);
            return Keys.hmacShaKeyFor(octets);
        } catch (IllegalArgumentException e) {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }
}
