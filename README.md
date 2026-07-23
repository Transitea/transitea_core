# Transitea — Backend (API)

API REST de gestion et de suivi de colis. Spring Boot 3.4 / Java 21 / PostgreSQL.

> Pour lancer l'application complète (backend + frontend + BDD) en une seule commande, voir le [README à la racine du dépôt](../../README.md).

## Prérequis

- Java 21 (JDK)
- Docker + Docker Compose (pour PostgreSQL)
- Le wrapper Maven `./mvnw` est fourni : pas besoin d'installer Maven

## Lancer en local sans tout dockeriser (recommandé en dev)

1. Démarrer uniquement PostgreSQL en conteneur :

   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

2. Lancer l'application (les valeurs par défaut de `application.yml` fonctionnent sans fichier `.env`) :

   ```bash
   ./mvnw spring-boot:run
   ```

   Ou depuis l'IDE : lancer la classe `TransiteaApplication`.

3. Vérifier que ça fonctionne :

   - http://localhost:8080/actuator/health → `{"status":"UP"}`
   - http://localhost:8080/swagger-ui.html → documentation interactive de l'API

Arrêter PostgreSQL :

```bash
docker-compose -f docker-compose.dev.yml down
```

## Lancer entièrement en Docker (API + PostgreSQL)

1. Copier `.env.example` en `.env` et renseigner au minimum `JWT_SECRET_ACCESS` / `JWT_SECRET_REFRESH` (commande de génération dans le fichier).

2. Construire et lancer :

   ```bash
   docker-compose up -d --build
   ```

3. Arrêter :

   ```bash
   docker-compose down       # conserve les données
   docker-compose down -v    # supprime aussi le volume postgres (reset complet de la BDD)
   ```

## Tests

```bash
./mvnw test
```

## Variables d'environnement principales

| Variable | Rôle | Défaut (dev) |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | Connexion PostgreSQL | `localhost` / `5432` / `transitea_db` / `transitea` / `transitea` |
| `JWT_SECRET_ACCESS` / `JWT_SECRET_REFRESH` | Signature des jetons JWT | valeur de dev non sécurisée — **à changer en production** |
| `APP_BASE_URL` | Domaine encodé dans les QR codes et les liens de suivi | `http://localhost:8080` — doit pointer vers le **frontend** en production |
| `CORS_ALLOWED_ORIGINS` | Origines frontend autorisées | `http://localhost:5173,http://localhost:4173,...` |
| `JPA_DDL_AUTO` | Stratégie de schéma Hibernate | `update` (`validate` imposé par le profil `prod`) |
| `GMAIL_USERNAME` / `GMAIL_APP_PASSWORD` | Envoi d'emails (notifications) | vide (fonctionnalité désactivée) |
| `WHATSAPP_TOKEN` / `WHATSAPP_PHONE_NUMBER_ID` | Notifications WhatsApp | vide (fonctionnalité désactivée) |
| `SERVER_PORT` / `LOG_LEVEL` | Port d'écoute et verbosité des logs | `8080` / `INFO` |

Voir `.env.example` pour la liste complète et les instructions de génération des secrets.

## Documentation

- [Manuel de déploiement (production)](docs/manuel-deploiement.md)
- [Sécurité OWASP](docs/securite-owasp.md)
- [Cahier de recettes](docs/cahier-de-recettes.md)
- [Manuel de mise à jour](docs/manuel-mise-a-jour.md)
- [Plan de correction des bogues](docs/plan-correction-bogues.md)
- Collection Bruno (`Transitea API/`) pour tester les endpoints manuellement
