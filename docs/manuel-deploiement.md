# Manuel de déploiement — Transitea

Ce manuel décrit comment déployer l'application Transitea (backend `transitea_core` + frontend `transitea_front`), pour le critère C2.4.1 du Bloc 2.

## Architecture de déploiement

Transitea est composé de **trois briques déployées indépendamment** :

| Brique | Technologie | Exposition en production |
|---|---|---|
| Base de données | PostgreSQL 15 (conteneur Docker) | interne, non exposée publiquement |
| Backend (API) | Spring Boot 3.4 / Java 21, packagé en jar puis en image Docker | `api.transitea.fr` (HTTP simple derrière le reverse-proxy de la plateforme) |
| Frontend (SPA) | React 19 / Vite, buildé en fichiers statiques servis par nginx | `transitea.fr` |

En production, le frontend et le backend sont déployés **séparément** (actuellement sur la plateforme Dokploy) : le conteneur nginx du frontend proxifie les appels `/api/*` vers `api.transitea.fr`, qui répond en HTTP simple en interne (TLS terminé en amont par la plateforme).

## Prérequis

- Docker et Docker Compose
- Un nom de domaine pour le frontend (ex. `transitea.fr`) et un pour l'API (ex. `api.transitea.fr`)
- Un compte Gmail dédié avec un mot de passe d'application (pour l'envoi d'emails)
- Un token WhatsApp Business API (Meta for Developers) si les notifications WhatsApp sont activées

## Déploiement du backend

### 1. Variables d'environnement

Copier `.env.example` en `.env` et renseigner (voir le fichier pour le détail complet) :

| Variable | Rôle | Remarque |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Connexion PostgreSQL | En Docker Compose, `DB_HOST=postgres` (nom du service) |
| `JWT_SECRET_ACCESS`, `JWT_SECRET_REFRESH` | Clés de signature des jetons JWT | **Obligatoire en production** — sans cela, les valeurs par défaut de développement (`dev-secret-...`) resteraient actives, ce qui est un risque de sécurité (voir `docs/securite-owasp.md`, A02). Générer avec `openssl rand -base64 64` |
| `APP_BASE_URL` | Domaine encodé dans les QR codes et les liens de suivi envoyés par email | **Doit pointer vers le domaine du FRONTEND** (ex. `https://transitea.fr`), pas celui du backend — erreur déjà rencontrée en production (voir `docs/plan-correction-bogues.md`, BUG-02) |
| `JPA_DDL_AUTO` | Stratégie de schéma Hibernate | `update` en développement, `validate` en production (le profil Spring `prod` l'impose de toute façon, voir plus bas) |
| `GMAIL_USERNAME`, `GMAIL_APP_PASSWORD` | Envoi d'email (notifications, récapitulatif quotidien) | Mot de passe d'application à créer sur `myaccount.google.com/apppasswords`, pas le mot de passe du compte |
| `WHATSAPP_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID` | Notifications WhatsApp | Depuis `developers.facebook.com` |
| `QRCODE_CACHE_PATH` | Répertoire de cache des QR codes générés | Monté sur un volume Docker persistant (`qrcode_cache`) |
| `SERVER_PORT`, `LOG_LEVEL` | Port d'écoute et verbosité des logs | Défauts : `8080`, `INFO` |

### 2. Profil Spring

Activer le profil `prod` (`SPRING_PROFILES_ACTIVE=prod`) en production : il force `ddl-auto: validate` (le schéma n'est jamais modifié automatiquement, seulement vérifié) et réduit la verbosité des logs. **Le schéma doit donc déjà exister** avant le premier démarrage en profil `prod` — le faire créer une première fois avec `ddl-auto: update` (profil par défaut) ou par un script SQL, puis basculer sur `prod` pour les démarrages suivants.

### 3. Build et lancement

```bash
# Développement local : uniquement PostgreSQL en conteneur, l'app tourne depuis l'IDE
docker-compose -f docker-compose.dev.yml up -d

# Production : build de l'image + lancement complet (Postgres + app)
docker-compose up -d --build
```

Le `Dockerfile` réalise un build multi-étapes : compilation Maven dans une image `eclipse-temurin:21-jdk-alpine`, puis exécution du jar dans une image `eclipse-temurin:21-jre-alpine` allégée, sous un utilisateur non-root dédié (`transitea`) pour réduire la surface d'attaque du conteneur.

### 4. Vérification post-déploiement

- `GET /actuator/health` doit répondre `{"status":"UP"}` (seul endpoint Actuator exposé publiquement).
- Consulter les logs (`docker logs transitea-app`) pour confirmer l'absence d'erreur au démarrage (connexion base de données, Quartz scheduler, etc.).

## Déploiement du frontend

### 1. Build

```bash
npm ci
npm run build
```

Génère les fichiers statiques dans `dist/` (TypeScript est vérifié avant le build via `tsc -b`).

### 2. Image Docker

Le `Dockerfile` du frontend construit l'application (Node 22) puis sert les fichiers statiques via nginx (`nginx:1.27-alpine`), avec la configuration `default.conf` :

- `location /api/` : proxifie vers `http://api.transitea.fr/`, en retirant le préfixe `/api` (le frontend appelle toujours `/api/v1/...`, jamais directement l'URL du backend — voir `src/services/api.ts`).
- `location /` : fallback SPA (`try_files ... /index.html`) pour que React Router gère les routes côté client.

### 3. Configuration CORS côté backend

Toute nouvelle origine frontend (nouveau domaine, environnement de recette, etc.) doit être ajoutée explicitement à la liste `allowedOrigins` de `ConfigurationSecurite.corsConfigurationSource()` — Spring Security rejette silencieusement (403) toute origine absente de cette liste, y compris pour les endpoints publics (voir `docs/plan-correction-bogues.md`, BUG-01).

## Ports et volumes

| Service | Port exposé | Volume |
|---|---|---|
| PostgreSQL | 5432 | `postgres_data` (données) |
| Backend | 8080 | `qrcode_cache` (cache des QR codes générés) |
| Frontend (nginx) | 80 | — (fichiers statiques dans l'image) |

## Points de vigilance

- Les secrets JWT et le mot de passe de base de données ne doivent **jamais** être committés — `.env` est dans `.gitignore`, seul `.env.example` (sans valeur réelle) est versionné.
- La limitation de débit (`FiltreLimitationDebit`) et le verrouillage de compte sont en mémoire locale à l'instance : un déploiement multi-instances (plusieurs réplicas de l'API derrière un load-balancer) nécessiterait un état partagé (ex. Redis) — non nécessaire au volume actuel de trafic.
