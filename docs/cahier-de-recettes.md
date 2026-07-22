# Cahier de recettes — Transitea

Ce document reprend l'ensemble des fonctionnalités attendues du logiciel Transitea (backend `transitea_core` + frontend `transitea_front`) sous forme de scénarios de tests et de résultats attendus, pour le critère C2.3.1 du Bloc 2 (« Élaborer le cahier de recettes en rédigeant les scénarios de tests et les résultats attendus afin de détecter les anomalies de fonctionnement et les régressions éventuelles »).

**Légende des colonnes :**
- **Type** : Fonc. (fonctionnel), Struct. (structurel / technique), Sécu. (sécurité)
- **Couverture automatisée** : classe de test qui exécute ce scénario (quand applicable). L'absence d'entrée signifie un test manuel à réaliser.

## 1. Authentification et sécurité des comptes

| ID | Scénario | Préconditions | Étapes | Résultat attendu | Type | Couverture automatisée |
|---|---|---|---|---|---|---|
| AUTH-01 | Inscription d'un nouvel agent | Email et téléphone non utilisés, agence existante | POST `/v1/auth/register` avec des données valides | 201, compte créé avec le rôle AGENT, tokens access/refresh retournés | Fonc. | `AuthControllerTest`, `AuthServiceImplTest` |
| AUTH-02 | Refus d'inscription si email déjà utilisé | Un compte existe déjà avec cet email | POST `/v1/auth/register` avec cet email | 400, message explicite, aucun compte créé | Fonc. | `AuthServiceImplTest` |
| AUTH-03 | Connexion avec identifiants valides | Compte actif existant | POST `/v1/auth/login` | 200, nouveaux tokens émis, anciens refresh tokens révoqués | Fonc. | `AuthControllerTest`, `AuthServiceImplTest` |
| AUTH-04 | Connexion refusée avec mauvais mot de passe | Compte actif existant | POST `/v1/auth/login` avec mauvais mot de passe | 400, message générique « Email ou mot de passe incorrect » (pas d'énumération de compte) | Sécu. | `AuthServiceImplTest` |
| AUTH-05 | Verrouillage du compte après 5 échecs | Compte actif | 5 tentatives de connexion consécutives avec mauvais mot de passe | Le compte est verrouillé 15 minutes ; une 6ᵉ tentative, même avec le bon mot de passe, est refusée | Sécu. | `AuthServiceImplTest` |
| AUTH-06 | Déverrouillage automatique après expiration | Compte verrouillé depuis plus de 15 minutes | Connexion avec le bon mot de passe | 200, connexion acceptée, compteur d'échecs réinitialisé | Sécu. | `AuthServiceImplTest` |
| AUTH-07 | Limitation de débit sur la connexion | — | Plus de 10 requêtes POST `/v1/auth/login` en moins d'une minute depuis la même IP | Réponse 429 au-delà de la 10ᵉ requête | Sécu. | `FiltreLimitationDebitTest` |
| AUTH-08 | Limitation de débit globale | — | Plus de 100 requêtes en moins d'une minute depuis la même IP, tous endpoints confondus | Réponse 429 au-delà de la 100ᵉ requête | Sécu. | `FiltreLimitationDebitTest` |
| AUTH-09 | Rafraîchissement de token | Refresh token valide et non expiré | POST `/v1/auth/refresh` | 200, nouveaux access/refresh tokens, ancien refresh token révoqué | Fonc. | `AuthControllerTest`, `AuthServiceImplTest` |
| AUTH-10 | Refus de rafraîchissement si token invalide/expiré/révoqué | Refresh token invalide | POST `/v1/auth/refresh` | 401, `TokenInvalideException` | Fonc. | `AuthServiceImplTest` |
| AUTH-11 | Déconnexion | Refresh token valide | POST `/v1/auth/logout` | Refresh token révoqué, connexions suivantes avec ce token refusées | Fonc. | `AuthControllerTest`, `AuthServiceImplTest` |
| AUTH-12 | Consultation du profil connecté | Utilisateur authentifié | GET `/v1/auth/me` | 200, informations de l'utilisateur courant | Fonc. | `AuthControllerTest` |
| AUTH-13 | Accès refusé sans jeton | — | Appel à un endpoint protégé sans en-tête `Authorization` | 401 Unauthorized | Sécu. | `AuthControllerTest` |
| AUTH-14 | Accès refusé à un endpoint ADMIN pour un AGENT | Utilisateur connecté avec le rôle AGENT | GET `/v1/utilisateurs` (réservé ADMIN) | 403 Forbidden | Sécu. | `UtilisateurControllerTest` |

## 2. Gestion des colis

| ID | Scénario | Préconditions | Étapes | Résultat attendu | Type | Couverture automatisée |
|---|---|---|---|---|---|---|
| COLIS-01 | Création d'un colis | Utilisateur authentifié, quota mensuel de l'enseigne non atteint | POST `/v1/colis` avec des données valides | 201, colis créé avec un code de tracking unique (`TRA-AAAA-XXXXXX`), statut initial `ENREGISTRE` | Fonc. | `ColisControllerTest`, `ColisServiceImplTest` |
| COLIS-02 | Refus de création si quota mensuel atteint | Quota mensuel de l'enseigne à 100% | POST `/v1/colis` | Erreur métier explicite, colis non créé | Fonc. | `QuotaServiceImplTest`, `ColisServiceImplTest` |
| COLIS-03 | Liste paginée des colis, avec filtre par statut | Colis existants | GET `/v1/colis?statut=EN_TRANSIT&page=0&taille=20` | 200, page de résultats filtrés, triés par date de création décroissante | Fonc. | `ColisControllerTest` |
| COLIS-04 | Recherche de colis (code, ville, client) | Colis existants | GET `/v1/colis/recherche?q=...` | 200, résultats correspondant à la recherche | Fonc. | `ColisControllerTest` |
| COLIS-05 | Détail d'un colis | Colis existant | GET `/v1/colis/{id}` | 200, détail complet + historique des statuts | Fonc. | `ColisControllerTest` |
| COLIS-06 | Statistiques du tableau de bord | Colis existants | GET `/v1/colis/statistiques` | 200, compteurs par statut cohérents avec les données | Fonc. | `ColisControllerTest` |
| COLIS-07 | Volume quotidien sur une période | Colis existants | GET `/v1/colis/volume-quotidien?debut=...&fin=...` | 200, une entrée par jour de la période, volumes exacts | Fonc. | `ColisControllerTest` |
| COLIS-08 | Génération du QR code d'un colis | Colis existant | GET `/v1/colis/{id}/qrcode` | 200, image PNG valide encodant le code de tracking | Fonc. | `ColisControllerTest`, `QrCodeServiceImplTest` |
| COLIS-09 | Transition de statut valide | Colis au statut `ENREGISTRE` | PATCH `/v1/colis/{id}/statut` vers `EN_TRANSIT` | 200, statut mis à jour, entrée ajoutée à l'historique, notification déclenchée | Fonc. | `ColisControllerTest`, `ValidateurTransitionStatutTest` |
| COLIS-10 | Transition de statut invalide refusée | Colis au statut `RETIRE` (terminal) | PATCH `/v1/colis/{id}/statut` vers `EN_TRANSIT` | 400, `TransitionStatutInvalideException`, statut inchangé | Fonc. | `ValidateurTransitionStatutTest` |
| COLIS-11 | Retrait d'un colis par scan QR | Colis au statut `ARRIVE_AGENCE` | POST `/v1/colis/retrait?codeTracking=...` | 200, statut passe à `RETIRE`, horodatage de retrait enregistré | Fonc. | `ColisControllerTest`, `ColisServiceImplTest` |
| COLIS-12 | Suppression (soft delete) d'un colis | Colis existant | DELETE `/v1/colis/{id}` | 204, colis marqué supprimé, absent des listes suivantes mais conservé en base | Fonc. | `ColisControllerTest`, `ColisServiceImplTest` |
| COLIS-13 | Export CSV des colis | Colis existants | GET `/v1/export/colis` | 200, fichier CSV avec en-têtes et lignes correspondant aux colis visibles par l'utilisateur | Fonc. | `ExportControllerTest`, `ExportServiceImplTest` |

## 3. Suivi public d'un colis (sans authentification)

| ID | Scénario | Préconditions | Étapes | Résultat attendu | Type | Couverture automatisée |
|---|---|---|---|---|---|---|
| SUIVI-01 | Suivi d'un colis avec un code valide | Colis existant | GET `/v1/tracking/{codeTracking}` (public) | 200, statut actuel + historique, sans données sensibles (pas d'email/téléphone du destinataire) | Fonc. | `TrackingControllerTest`, `TrackingServiceImplTest` |
| SUIVI-02 | Code de tracking inconnu | — | GET `/v1/tracking/CODE-INCONNU` | 404, message clair | Fonc. | `TrackingControllerTest` |
| SUIVI-03 | Accès à la page `/suivi` sans être connecté | — | Navigation vers `/suivi` puis saisie d'un code | Page accessible sans authentification, résultat affiché | Fonc. | Test manuel (parcours navigateur) |

## 4. Synchronisation hors-ligne

| ID | Scénario | Préconditions | Étapes | Résultat attendu | Type | Couverture automatisée |
|---|---|---|---|---|---|---|
| SYNC-01 | Upload de colis créés hors-ligne | Colis en attente côté client (IndexedDB) | POST `/v1/sync/upload` avec un lot de colis locaux | 200, colis créés côté serveur, identifiants locaux mappés aux identifiants serveur | Fonc. | `SyncControllerTest`, `SyncServiceImplTest` |
| SYNC-02 | Échec partiel d'un upload (conflit ou donnée invalide) | Un des colis du lot est invalide | POST `/v1/sync/upload` | 200, rapport détaillant les éléments synchronisés avec succès et ceux en échec (avec raison) | Fonc. | `SyncServiceImplTest` |
| SYNC-03 | Téléchargement des changements depuis une date | Des colis/statuts ont changé depuis `since` | GET `/v1/sync/download?since=...` | 200, uniquement les éléments modifiés après cette date | Fonc. | `SyncControllerTest`, `SyncServiceImplTest` |
| SYNC-04 | Reprise de la synchronisation après perte de connexion | Application hors-ligne puis reconnexion | Perte réseau simulée pendant la saisie, puis reconnexion | Les données saisies hors-ligne sont conservées localement puis synchronisées automatiquement au retour du réseau | Fonc. | Test manuel (voir roadmap offline-first en cours) |

## 5. Agences

| ID | Scénario | Préconditions | Étapes | Résultat attendu | Type | Couverture automatisée |
|---|---|---|---|---|---|---|
| AGENCE-01 | Liste des agences (publique, pour le formulaire d'inscription) | Agences existantes | GET `/v1/agences` sans authentification | 200, liste des agences actives | Fonc. | `AgenceControllerTest` |
| AGENCE-02 | Création d'une agence par un administrateur | Utilisateur ADMIN connecté | POST `/v1/agences` | 201, agence créée | Fonc. | `AgenceControllerTest` |
| AGENCE-03 | Refus de création d'agence pour un non-administrateur | Utilisateur AGENT connecté | POST `/v1/agences` | 403 Forbidden | Sécu. | `AgenceControllerTest` |
| AGENCE-04 | Détail d'une agence | Agence existante | GET `/v1/agences/{id}` | 200, détail de l'agence | Fonc. | `AgenceControllerTest` |

## 6. Gestion des utilisateurs (administration)

| ID | Scénario | Préconditions | Étapes | Résultat attendu | Type | Couverture automatisée |
|---|---|---|---|---|---|---|
| UTIL-01 | Liste des utilisateurs | Utilisateur ADMIN connecté | GET `/v1/utilisateurs` | 200, liste des comptes | Fonc. | `UtilisateurControllerTest` |
| UTIL-02 | Création d'un compte par l'administrateur | Utilisateur ADMIN connecté | POST `/v1/utilisateurs` | 201, compte créé avec le rôle demandé | Fonc. | `UtilisateurControllerTest`, `UtilisateurServiceImplTest` |
| UTIL-03 | Activation / désactivation d'un compte | Compte existant | PATCH `/v1/utilisateurs/{id}/statut` | 200, statut mis à jour ; un compte `INACTIF` ne peut plus se connecter | Fonc. + Sécu. | `UtilisateurControllerTest`, `UtilisateurServiceImplTest` |

## 7. Notifications

| ID | Scénario | Préconditions | Étapes | Résultat attendu | Type | Couverture automatisée |
|---|---|---|---|---|---|---|
| NOTIF-01 | Notification automatique lors d'un changement de statut critique | Colis passant à un statut notifiable (ex : `ARRIVE_AGENCE`) | Mise à jour du statut du colis | Une notification (email et/ou WhatsApp) est envoyée au destinataire avec le lien de suivi | Fonc. | `NotificationServiceImplTest` |
| NOTIF-02 | Reprise sur échec d'envoi (SMTP indisponible) | Service mail indisponible | Déclenchement d'une notification pendant la panne | L'échec est journalisé, l'application ne plante pas, la notification suivante est retentée normalement | Struct. | `NotificationServiceImplTest` |
| NOTIF-03 | Historique des notifications | Notifications déjà envoyées | GET `/v1/notifications` | 200, liste des notifications envoyées pour les colis de l'utilisateur | Fonc. | `NotificationControllerTest` |
| NOTIF-04 | QR code joint en pièce jointe de l'email | Notification par email envoyée | Réception de l'email | Le QR code du colis est joint en inline dans l'email | Fonc. | Test manuel (vérification boîte mail réelle) |

## 8. Enseigne et quota mensuel

| ID | Scénario | Préconditions | Étapes | Résultat attendu | Type | Couverture automatisée |
|---|---|---|---|---|---|---|
| ENS-01 | Consultation du quota mensuel de l'enseigne | Enseigne existante | GET `/v1/enseigne` | 200, quota utilisé / quota maximum | Fonc. | `EnseigneControllerTest` |
| ENS-02 | Alerte à 80% du quota | Enseigne à 80 colis / 100 | Création d'un colis supplémentaire | Avertissement journalisé (log), création acceptée | Struct. | `QuotaServiceImplTest` |
| ENS-03 | Blocage à 100% du quota | Enseigne à 100 colis / 100 | Tentative de création d'un colis | Création refusée avec message explicite | Fonc. | `QuotaServiceImplTest` |

## 9. Clients

| ID | Scénario | Préconditions | Étapes | Résultat attendu | Type | Couverture automatisée |
|---|---|---|---|---|---|---|
| CLI-01 | Liste des clients (destinataires/expéditeurs connus) | Colis existants | GET `/v1/clients` | 200, liste dédupliquée des clients associés aux colis de l'utilisateur | Fonc. | `ClientControllerTest` |

## 10. Accessibilité (transverse)

| ID | Scénario | Préconditions | Étapes | Résultat attendu | Type | Couverture automatisée |
|---|---|---|---|---|---|---|
| A11Y-01 | Navigation par lecteur d'écran sur les pages publiques | — | Audit automatisé (axe-core) sur Connexion, Inscription, Suivi, 404 | 0 violation d'accessibilité détectée | Struct. | Voir `docs/accessibilite-rgaa.md` (frontend) |
| A11Y-02 | Structure de landmarks/titres sur les pages authentifiées | Utilisateur connecté | Navigation sur Tableau de bord, Colis, Utilisateurs, etc. | Un `<main>` et un `<h1>` uniques par page (héritage `AppLayout`/`Topbar`) | Struct. | Revue de code, à confirmer par audit live (voir doc RGAA) |

## 11. Sécurité applicative (transverse)

Voir `docs/securite-owasp.md` pour la synthèse complète des mesures de sécurité mises en œuvre au regard des 10 failles OWASP ; les scénarios AUTH-04 à AUTH-08 et AGENCE-03/UTIL-03/AUTH-14 ci-dessus en couvrent la part fonctionnelle testable.

---

## Synthèse de couverture

- **56 scénarios** répertoriés sur l'ensemble des fonctionnalités du logiciel.
- La grande majorité dispose déjà d'un test automatisé associé (tests unitaires de service + tests HTTP de contrôleur), exécuté à chaque build via Maven Surefire/Failsafe et vérifié par la CI.
- Les scénarios sans couverture automatisée sont explicitement marqués « Test manuel » : ils concernent des parcours navigateur de bout en bout (offline-first, réception d'email réelle) qui nécessitent un environnement complet (réseau, boîte mail) et restent à exécuter manuellement avant la mise en production.
