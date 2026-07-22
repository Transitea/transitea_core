# Synthèse des mesures de sécurité — OWASP Top 10 (2021)

Ce document présente les mesures de sécurité mises en œuvre dans le backend Transitea (`transitea_core`) au regard des 10 failles de sécurité principales décrites par l'OWASP. Il sert de pièce justificative pour le critère C2.2.3 du Bloc 2 (« Les mesures prises permettent de couvrir les 10 failles de sécurité principales décrites par l'OWASP »).

## A01:2021 — Broken Access Control (contrôle d'accès défaillant)

**Mesures en place :**
- Toutes les routes sont protégées par défaut (`anyRequest().authenticated()`), seule une liste explicite d'endpoints publics est autorisée (inscription, connexion, refresh, logout, suivi de colis en lecture, health check, documentation Swagger).
- Contrôle d'accès basé sur les rôles via `@PreAuthorize("hasRole('ADMIN')")` sur les endpoints d'administration (`AgenceController`, `EnseigneController`, `UtilisateurController`).
- `@EnableMethodSecurity` actif, ce qui permet un contrôle au niveau méthode en plus du contrôle au niveau route.

*Fichiers concernés : `ConfigurationSecurite.java`, contrôleurs annotés `@PreAuthorize`.*

## A02:2021 — Cryptographic Failures (défaillances cryptographiques)

**Mesures en place :**
- Mots de passe hachés avec BCrypt (`BCryptPasswordEncoder`), jamais stockés ni journalisés en clair.
- Jetons JWT signés (access + refresh) avec des clés distinctes, chargées depuis des variables d'environnement (`JWT_SECRET_ACCESS`, `JWT_SECRET_REFRESH`), jamais codées en dur en production.
- Durées de vie courtes pour l'access token (1h) et plus longues pour le refresh token (7j), limitant la fenêtre d'exploitation en cas de vol de jeton.

**Point de vigilance :** les valeurs par défaut de secret JWT présentes dans `application.yml` (`dev-secret-...`) sont uniquement des garde-fous de développement ; il faut s'assurer que les variables d'environnement de production sont toujours renseignées (à documenter dans le manuel de déploiement).

## A03:2021 — Injection

**Mesures en place :**
- Accès aux données exclusivement via Spring Data JPA / Hibernate avec requêtes paramétrées (aucune requête SQL native concaténée dans le code).
- Validation systématique des entrées via Bean Validation (`@NotBlank`, `@NotNull`, `@Size`, `@Email`, etc.) sur les DTOs de requête, avec retour d'erreurs structuré (`GestionnaireExceptionsGlobal`).
- Frontend React : échappement automatique du contenu affiché (pas d'usage de `dangerouslySetInnerHTML`), ce qui limite le risque de XSS stocké/réfléchi.

## A04:2021 — Insecure Design (conception non sécurisée)

**Mesures en place :**
- Machine à états stricte pour les transitions de statut des colis (`ValidateurTransitionStatut`), empêchant les transitions métier invalides.
- Politique de mot de passe minimale imposée (8 à 100 caractères) à la création de compte et à l'inscription.
- Limitation de débit (`FiltreLimitationDebit`) : 100 requêtes/minute par IP en global, 10 tentatives/minute par IP sur `/v1/auth/login`, avec réponse HTTP 429 au-delà.
- Verrouillage de compte après 5 échecs de connexion consécutifs (`AuthServiceImpl.enregistrerEchecConnexion`), pour une durée de 15 minutes, y compris si le mot de passe correct est ensuite fourni pendant la fenêtre de verrouillage (contrôlé par `Utilisateur.isAccountNonLocked()`, vérifié par Spring Security avant même la comparaison du mot de passe).

**Point de vigilance :** la limitation de débit est en mémoire locale, adaptée à un déploiement mono-instance actuel. Une montée en charge horizontale (plusieurs instances applicatives) nécessiterait un compteur partagé (ex : Bucket4j + Redis).

## A05:2021 — Security Misconfiguration (mauvaise configuration)

**Mesures en place :**
- CORS restreint à une liste explicite d'origines autorisées (pas de wildcard `*`), avec `allowCredentials` maîtrisé.
- Gestionnaire d'exceptions global qui ne renvoie jamais de stack trace ni de détail interne au client (message générique + log complet côté serveur uniquement).
- Exposition Actuator limitée à `/actuator/health` (pas de `/actuator/env`, `/actuator/beans`, etc. exposés publiquement).
- CSRF désactivé de manière justifiée : l'API est strictement stateless (jetons Bearer en en-tête `Authorization`, aucune authentification par cookie de session), donc non exposée aux attaques CSRF classiques.

**Point de vigilance :** l'exposition de Swagger UI (`/swagger-ui/**`, `/v3/api-docs/**`) est utile en développement mais mérite d'être restreinte ou désactivée en production (profil Spring dédié).

## A06:2021 — Vulnerable and Outdated Components (composants vulnérables)

**Mesures en place :**
- Stack à jour : Spring Boot 3.4.12, Java 21 (LTS), dépendances gérées uniquement via Maven Central.
- La CI (une fois mergée sur `main`) exécute le build complet à chaque push/PR, ce qui garantit qu'aucune dépendance cassée n'est fusionnée silencieusement.
- Dependabot activé (`.github/dependabot.yml`) sur les écosystèmes Maven, Docker et GitHub Actions (vérification hebdomadaire), avec ouverture automatique de pull requests en cas de dépendance vulnérable ou obsolète.

## A07:2021 — Identification and Authentication Failures (défaillances d'authentification)

**Mesures en place :**
- Authentification par JWT avec séparation access/refresh token et révocation du refresh token à la déconnexion.
- Mots de passe jamais transmis ni stockés en clair, hachage BCrypt avec sel intégré.
- Protection anti brute-force : limitation à 10 tentatives/minute par IP sur la connexion, et verrouillage de compte après 5 échecs consécutifs (voir A04).

## A08:2021 — Software and Data Integrity Failures (défaillances d'intégrité)

**Mesures en place :**
- Gestion de versions Git avec branches de fonctionnalités et revue via pull requests avant fusion sur `main`.
- Build Docker multi-étapes (`Dockerfile`) : l'image finale ne contient que le jar compilé, exécuté par un utilisateur non-root dédié (`transitea`), réduisant la surface d'attaque du conteneur.
- Dépendances récupérées uniquement depuis les dépôts Maven officiels (pas de source tierce non vérifiée).

## A09:2021 — Security Logging and Monitoring Failures (défaillances de journalisation)

**Mesures en place :**
- Journalisation structurée (SLF4J) des événements d'authentification (connexion, déconnexion, échec) et des erreurs internes, sans jamais journaliser de mot de passe ou de jeton en clair.
- Les erreurs non gérées sont systématiquement tracées côté serveur avec la pile d'exécution complète (`journal.error(..., ex)`), même si le client ne reçoit qu'un message générique.

**Point de vigilance :** la supervision active (alerting, tableau de bord de monitoring) relève du Bloc 4 (maintien en condition opérationnelle) et sera traitée à ce stade — voir le futur système de supervision et d'alerte (C4.1.2).

## A10:2021 — Server-Side Request Forgery (SSRF)

**Mesures en place :**
- Aucune fonctionnalité de l'application n'effectue de requête serveur vers une URL fournie par l'utilisateur : les seuls appels sortants (API WhatsApp, envoi d'email) pointent vers des endpoints fixes définis en configuration, jamais construits à partir d'une entrée utilisateur.
- Risque jugé non applicable dans la conception actuelle.

---

## Synthèse

| Catégorie OWASP | Statut |
|---|---|
| A01 Broken Access Control | ✅ Couvert |
| A02 Cryptographic Failures | ✅ Couvert |
| A03 Injection | ✅ Couvert |
| A04 Insecure Design | ✅ Couvert (rate limiting + verrouillage de compte) |
| A05 Security Misconfiguration | ✅ Couvert (Swagger à restreindre en prod) |
| A06 Vulnerable Components | ✅ Couvert (Dependabot activé) |
| A07 Authentication Failures | ✅ Couvert (rate limiting + verrouillage de compte) |
| A08 Software/Data Integrity | ✅ Couvert |
| A09 Logging & Monitoring | ✅ Couvert (volet supervision approfondi en Bloc 4) |
| A10 SSRF | ✅ Non applicable / couvert par conception |
