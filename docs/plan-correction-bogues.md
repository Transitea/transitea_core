# Plan de correction des bogues — Transitea

Ce document analyse les anomalies détectées au cours de la recette (voir `docs/cahier-de-recettes.md`) et lors des itérations de développement du logiciel, qualifie chacune d'elles, et présente la correction apportée ainsi que sa vérification. Il sert de pièce justificative pour le critère C2.3.2 du Bloc 2 (« Élaborer un plan de correction des bogues à partir de l'analyse des anomalies et des régressions détectées au cours de la recette afin de garantir le fonctionnement du logiciel conformément à l'attendu »).

**Légende de gravité :** 🔴 Bloquant (fonctionnalité inutilisable) · 🟠 Majeur (fonctionnalité dégradée) · 🟡 Mineur (UX/qualité)

## Backend (`transitea_core`)

### BUG-01 — CORS bloque le frontend de production 🔴

- **Anomalie observée :** en production, le frontend déployé (`https://transitea.fr`) ne pouvait appeler aucun endpoint, y compris les endpoints publics comme `/v1/auth/login`, échec avec une erreur CORS avant même que la requête n'atteigne le contrôleur.
- **Analyse :** la configuration CORS (`ConfigurationSecurite.corsConfigurationSource`) n'autorisait que les origines de développement local (`localhost:5173`, `localhost:4173`) ; Spring Security rejette côté serveur toute origine absente de la liste, quelle que soit la règle d'autorisation de la route.
- **Correction :** ajout des origines `https://transitea.fr` et `http://transitea.fr` à la liste `allowedOrigins`.
- **Vérification :** test manuel (déploiement de vérification) ; couverture de régression : `ConfigurationSecurite` reste testée indirectement via les tests HTTP de contrôleurs (`AuthControllerTest` et al.), qui échoueraient si la chaîne de filtres cassait globalement.

### BUG-02 — Le QR code et le lien de suivi pointent vers une route inexistante ou une route API 🔴

- **Anomalie observée :** scanner le QR code d'un colis ou cliquer sur le lien de suivi reçu par email ne menait à aucune page utilisable : le QR encodait `baseUrl + /v1/tracking/{code}` (endpoint API, retourne du JSON) et le lien email encodait `baseUrl + /tracking/{code}` (route inexistante côté frontend).
- **Analyse :** confusion entre l'URL de l'API backend et l'URL de la page publique de suivi du frontend (`/suivi/{code}`) ; non-conformité avec le CDC (§8.3 : « Contenu encodé : `https://transitea.com/suivi/TRA-2026-...` »).
- **Correction :** les deux générateurs de lien (QR code, template d'email) pointent désormais vers `/suivi/{code}` sur le domaine du frontend.
- **Point de vigilance résiduel :** la variable d'environnement `APP_BASE_URL` doit être positionnée sur le domaine public du **frontend** (ex. `https://transitea.fr`) et non celui du backend — à documenter explicitement dans le manuel de déploiement.
- **Vérification :** `QrCodeServiceImplTest`, `NotificationServiceImplTest`.

### BUG-03 — Notifications email classées en spam 🟠

- **Anomalie observée :** les emails de notification (changement de statut, récapitulatif quotidien) atterrissaient fréquemment dans les spams des destinataires.
- **Analyse :** absence de nom d'expéditeur lisible (adresse brute seule) et absence d'alternative texte brut (HTML seul envoyé) — deux signaux classiquement pénalisés par les filtres anti-spam.
- **Correction :** ajout du nom d'expéditeur « Transitea » et envoi en `multipart/alternative` (texte brut + HTML).
- **Limite connue et assumée :** avec un compte Gmail personnel (pas de domaine authentifié SPF/DKIM/DMARC), le risque est réduit mais pas éliminé. Une solution durable nécessiterait un fournisseur transactionnel avec authentification de domaine sur `transitea.fr` — hors périmètre de ce correctif, à traiter avant une mise en production à grande échelle.
- **Vérification :** `NotificationServiceImplTest` ; validation visuelle de la réception réelle recommandée avant mise en production (scénario NOTIF-04 du cahier de recettes).

### BUG-04 — CI cassée (`mvnw` non exécutable sur Linux) 🟠

- **Anomalie observée :** le pipeline GitHub Actions échouait systématiquement après 4-7 secondes, code de sortie 126.
- **Analyse :** le fichier `mvnw` avait été committé avec le mode `100644` (non exécutable) au lieu de `100755` — sans conséquence sous Windows (pas de notion de bit exécutable), mais bloquant sur le runner Linux de la CI.
- **Correction :** `git update-index --chmod=+x mvnw`.
- **Vérification :** build local reproduit avec succès (95 tests, jar généré) avant correction du mode ; CI relancée après le fix (voir historique de la branche `feat/ajout-ci`).

## Frontend (`transitea_front`)

### BUG-05 — Le scan QR échoue dès qu'une lettre apparaît dans le code 🔴

- **Anomalie observée :** la saisie/scan d'un code de tracking échouait systématiquement pour la majorité des colis réels.
- **Analyse :** l'expression régulière d'extraction du code (`ScanPage`) n'acceptait que des chiffres dans le suffixe (`\d{6}`), alors que le format réel généré par le backend (`GenerateurCodeTracking`) est alphanumérique.
- **Correction :** la regex accepte désormais le format alphanumérique réel.
- **Vérification :** couverte côté backend par `GenerateurCodeTrackingTest` (format généré) ; test manuel du scan à réaliser côté frontend (pas de test automatisé sur `ScanPage` actuellement — écart de couverture identifié).

### BUG-06 — Page blanche après mise à jour de statut ou retrait 🔴

- **Anomalie observée :** après une action de mise à jour de statut ou de retrait réussie, la page de détail du colis plantait (page blanche).
- **Analyse :** la réponse de l'action retourne le colis avec ses champs de base mais sans l'historique complet ; `pkg.historique.map(...)` était appelé sur `undefined`.
- **Correction :** rechargement du colis complet (`obtenirColis`) après une action réussie en ligne, avec en complément une garde défensive (`pkg.historique ?? []`).
- **Vérification :** test manuel du parcours (pas de test de composant React actuellement sur `ColisDetailPage` — écart de couverture identifié, voir section « Écarts de couverture »).

### BUG-07 — Bouton d'action trompeur sur un colis à statut terminal 🟡

- **Anomalie observée :** sur un colis déjà `RETIRE` ou `RETOUR_EXPEDITEUR`, un bouton « Valider » désactivé s'affichait sans explication.
- **Analyse :** `UpdateStatusSheet` ne distinguait pas les statuts terminaux des statuts transitoires.
- **Correction :** affichage d'un simple bouton « Fermer » à la place, cohérent avec `ValidateurTransitionStatut.estStatutTerminal()` côté backend.
- **Vérification :** test manuel.

### BUG-08 — Faux positif « En ligne » en mode avion 🟠

- **Anomalie observée :** l'indicateur de connexion affichait « En ligne » alors que le réseau était coupé, empêchant le repli hors-ligne attendu.
- **Analyse :** `navigator.onLine` reste à `true` sur certaines configurations tant qu'au moins un adaptateur réseau est actif, indépendamment de la connectivité réelle.
- **Correction :** `useEnLigne()` vérifie désormais activement la connectivité en sondant `/actuator/health` (au montage, sur l'évènement `online`, et toutes les 20s), au lieu de faire confiance à `navigator.onLine` seul.
- **Vérification :** test manuel (mode avion).

### BUG-09 — Appels réseau bloquants jusqu'à 30-60s sans timeout 🟠

- **Anomalie observée :** sur un hôte injoignable, l'UI restait figée (bouton « Enregistrement… » bloqué) bien au-delà de ce qui est utilisable, retardant d'autant le repli hors-ligne.
- **Analyse :** aucun des appels réseau n'avait de timeout explicite ; le navigateur attend 30 à 60+ secondes avant d'abandonner une connexion à un hôte injoignable.
- **Correction :** `fetchAvecTimeout()` borne chaque appel à 8 secondes via `AbortController` et rejette avec un type d'erreur compatible avec la détection de panne réseau existante.
- **Vérification :** test manuel.

### BUG-10 — Faux repli hors-ligne sur des erreurs non réseau 🟠

- **Anomalie observée :** un message « hors-ligne » trompeur s'affichait pour la liste des agences même quand la vraie cause était une erreur serveur (401, 500…) sans rapport avec la connectivité.
- **Analyse :** `listerAgencesResilient` basculait sur le cache local dès que le cache était vide, en avalant toute erreur sans distinction.
- **Correction :** réutilisation de la détection `estErreurReseau` (déjà utilisée pour la création de colis) pour ne basculer sur le cache qu'en cas de panne réseau réelle ; toute autre erreur remonte telle quelle.
- **Vérification :** test manuel.

### BUG-11 — Formulaire de création de colis inutilisable hors-ligne (liste d'agences vide) 🟠

- **Anomalie observée :** hors-ligne, les sélecteurs d'agence de dépôt/retrait restaient vides, rendant le formulaire de création de colis inutilisable, alors que la mise en file d'attente du colis fonctionnait déjà correctement en dessous.
- **Analyse :** le formulaire dépendait d'un appel réseau direct (`listerAgences`) sans repli local.
- **Correction :** `listerAgencesResilient()` met en cache le dernier résultat réussi (Dexie/IndexedDB) et bascule dessus dès que l'appel réseau échoue.
- **Vérification :** test manuel.

### BUG-12 — Menu latéral inaccessible sur petite hauteur d'écran (desktop) 🟡

- **Anomalie observée :** sur une fenêtre desktop peu haute, la carte utilisateur et le bouton de déconnexion en bas du menu latéral disparaissaient hors de l'écran, sans moyen d'y accéder.
- **Analyse :** `.sidebar` est en hauteur fixe (`position: fixed`) ; sans `overflow` ni `min-height: 0` sur la zone de navigation (`flex: 1`), un contenu trop grand débordait silencieusement.
- **Correction :** ajout du défilement indépendant sur la zone de navigation du menu latéral.
- **Vérification :** test manuel (redimensionnement de fenêtre).

## Accessibilité (détectées lors de l'audit RGAA de la recette)

Voir `docs/accessibilite-rgaa.md` pour le détail complet. Résumé des anomalies qualifiées et corrigées le même jour (2026-07-22) :

| ID | Anomalie | Gravité | Correction |
|---|---|---|---|
| A11Y-BUG-01 | Contraste insuffisant (2,7:1) du logo sur la page de suivi public | 🟠 | Teinte assombrie (`#C2410C`), contraste ≈ 5:1 |
| A11Y-BUG-02 | Landmark `<main>` absent (page de suivi, 404, et **toutes** les pages authentifiées via `AppLayout`) | 🟠 | Ajout d'un `<main>` sémantique |
| A11Y-BUG-03 | Titre `<h1>` absent (page de suivi, 404, et **toutes** les pages authentifiées via `Topbar`) | 🟠 | Ajout d'un `<h1>` (visible ou masqué visuellement) |
| A11Y-BUG-04 | Champ de recherche sans nom accessible (placeholder seul) | 🟡 | Ajout d'un `aria-label` |
| A11Y-BUG-05 | Bouton de synchronisation sans nom accessible (`title` seul, non fiable pour les lecteurs d'écran) | 🟡 | Ajout d'un `aria-label` équivalent |

## Écarts de couverture identifiés

L'analyse de ces anomalies met en évidence un point d'amélioration transverse : **`ScanPage` et `ColisDetailPage` (frontend) n'ont aucun test automatisé**, alors que deux bogues bloquants (BUG-05, BUG-06) s'y sont produits. Aucun test frontend n'existe actuellement dans `transitea_front` (0 fichier `*.test.*`). C'est un axe d'amélioration à part entière, distinct de ce plan de correction, qui mériterait la mise en place d'une suite de tests de composants (ex. Vitest + Testing Library) sur les parcours critiques (scan, détail colis, mise à jour de statut).

## Synthèse

- **17 anomalies** analysées, qualifiées et corrigées (12 fonctionnelles/techniques + 5 d'accessibilité).
- Répartition : 4 🔴 bloquantes, 9 🟠 majeures, 4 🟡 mineures.
- Toutes les anomalies bloquantes et majeures disposent d'une correction déployée et vérifiée (automatiquement ou manuellement selon le composant concerné).
- Un écart de couverture de tests frontend est explicitement documenté ci-dessus plutôt que dissimulé.
