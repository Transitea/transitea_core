# Journal des versions

Format inspire de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/),
versionnage semantique ([SemVer](https://semver.org/lang/fr/)).

## [Non publie]

## [1.0.0] - 2026-07-27

Premiere version taguee du backend Transitea.

### Ajoute

- Mise en coherence du backend avec le cahier des charges v2.2 (refonte
  multi-agences)
- Synchronisation hors-ligne des colis (upload/download, `SyncLog`)
- Gestion des comptes utilisateurs par l'administrateur
- Quota mensuel par enseigne, documentation OpenAPI
- Agences publiques (`GET /v1/agences` accessible sans authentification,
  necessaire au formulaire d'inscription)
- Endpoints clients, notifications et rapports
- Integration WhatsApp reelle (envoi du QR code en image), recapitulatif
  quotidien par email, correction du retrait de colis par scan QR
- Limitation de debit (rate limiting) et verrouillage de compte apres
  echecs de connexion repetes
- Synthese de conformite OWASP
- Cahier de recettes, plan de correction des bogues, manuels de
  deploiement et de mise a jour
- Integration continue (GitHub Actions : build, tests, image Docker)
- Mises a jour de dependances automatisees via Dependabot (Maven et
  GitHub Actions)

### Corrige

- `mvnw` non executable, provoquant un echec (exit 126) sur les runners
  CI Linux
- Le QR code / lien de suivi pointait vers une route API au lieu de la
  page `/suivi` du frontend
- Risque de classement en spam des emails de notification (nom
  d'expediteur et version texte brut manquants)
- Domaine de production non autorise dans la configuration CORS

[Non publie]: https://github.com/Transitea/transitea_core/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Transitea/transitea_core/releases/tag/v1.0.0
