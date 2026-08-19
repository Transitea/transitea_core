# Bloc 4 — A4.2 Traitement des anomalies détectées en production

Ce document couvre les critères C4.2.1 (consignation des anomalies) et
C4.2.2 (création et déploiement d'un correctif) du référentiel
RNCP39583, Bloc 4.

## C4.2.1 — Processus de collecte et de consignation des anomalies

### Sources de détection

- **Automatique** : alerting Grafana (voir `docs/bloc4-supervision.md`,
  C4.1.2) — détection d'une indisponibilité ou d'une dégradation de
  service.
- **Manuelle** : remontée directe par un utilisateur/porteur de projet
  lors de l'usage d'un environnement (cas illustré ci-dessous),
  vérification systématique après chaque déploiement.

### Outil de collecte

Le volume du projet ne justifie pas d'outil de ticketing dédié : chaque
anomalie est **consignée directement dans la documentation projet**, au
format fiche structurée reproductible ci-dessous, avec engagement
systématique sur trois points : reproduction, analyse causale,
correctif vérifié. Cette approche est déjà en place pour les anomalies
fonctionnelles/techniques et d'accessibilité (voir
`docs/plan-correction-bogues.md`, Bloc 2) ; le même format est repris
ici pour une anomalie de production détectée depuis.

### Fiche de consignation — Exemple réel

**ID :** ANOM-2026-07-27-01
**Environnement concerné :** dev (`https://dev.transitea.fr`)
**Détecté par :** porteur de projet, lors d'un test de connexion manuel
**Gravité :** 🔴 bloquant (authentification totalement impossible)

**Symptôme observé**
```
POST https://dev.transitea.fr/api/v1/auth/login → 403 (Forbidden)
```
Impossible de se connecter sur l'environnement dev, alors que la même
opération fonctionne en production.

**Étapes de reproduction**
```bash
curl -X POST https://dev.transitea.fr/api/v1/auth/login \
  -H "Origin: https://dev.transitea.fr" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","motDePasse":"test"}'
# → 403 Forbidden, corps de reponse : "Invalid CORS request"
```

**Analyse (déroulé réel de l'investigation)**

1. Première hypothèse : configuration CORS backend n'autorisant pas
   `https://dev.transitea.fr`. Vérifiée sur le code source
   (`ConfigurationSecurite`) — l'origine était bien présente dans la
   configuration par défaut sur la branche déployée. Hypothèse écartée
   après confirmation que le commit déployé sur Dokploy contenait bien
   ce correctif.
2. Le test `curl` direct vers le backend continuant à échouer avec le
   message `"Invalid CORS request"` (message natif de Spring Security
   lors d'un rejet CORS), l'anomalie a été isolée au niveau du
   **frontend** : sa configuration nginx (`default.conf`) proxyait
   `/api/` vers `http://api.transitea.fr` **en dur**, quel que soit
   l'environnement de déploiement — donc le frontend dev envoyait ses
   requêtes vers le backend de **production**, dont la liste
   d'origines CORS n'autorise pas `dev.transitea.fr`.
3. Cause racine confirmée : absence de paramétrage de l'URL du backend
   par environnement dans la configuration nginx du frontend.

**Correctif** : voir C4.2.2 ci-dessous.

**Vérification**
```bash
curl -i https://dev.transitea.fr/api/v1/auth/login \
  -H "Origin: https://dev.transitea.fr" ...
# → réponse applicative normale (plus de rejet CORS)
```
puis test de connexion réel depuis le navigateur, confirmé fonctionnel
par le porteur de projet.

## C4.2.2 — Exemple de correctif créé et déployé via CI/CD

### Contexte

Anomalie ANOM-2026-07-27-01 ci-dessus : le frontend Transitea
(`transitea_front`) proxyait systématiquement ses appels `/api/` vers
le backend de production, y compris lorsqu'il était lui-même déployé
sur l'environnement dev.

### Correctif

Le fichier `default.conf` (configuration nginx statique, compilée dans
l'image Docker au build) a été transformé en `default.conf.template`,
avec l'URL du backend paramétrée par variable d'environnement :

```nginx
location /api/ {
    proxy_pass ${API_BACKEND_URL}/;
    proxy_set_header Host $proxy_host;
    ...
}
```

Le `Dockerfile` copie ce template dans `/etc/nginx/templates/` —
mécanisme natif de l'image nginx officielle qui substitue les
variables d'environnement au démarrage du conteneur (`envsubst`),
sans script personnalisé à maintenir. Une valeur par défaut
(`http://api.transitea.fr`) est conservée pour ne rien casser en
production si la variable n'est pas fournie.

### Processus d'intégration et de déploiement

1. Correctif développé sur une branche dédiée
   (`fix/api-backend-url-configurable`), à partir d'un `main`
   à jour.
2. Pull request ouverte, revue, mergée.
3. Déploiement Dokploy configuré en mode **« On Push »** : chaque
   merge sur la branche suivie déclenche automatiquement un nouveau
   build et déploiement du conteneur frontend concerné.
4. Ajout de la variable d'environnement `API_BACKEND_URL` sur la
   ressource Dokploy de l'environnement dev
   (`https://api-dev.transitea.fr`), sans impact sur la ressource de
   production (qui conserve la valeur par défaut).
5. Redéploiement automatique déclenché, correctif vérifié en
   conditions réelles (étape « Vérification » ci-dessus).

### Résolution de l'anomalie annexe rencontrée pendant le déploiement

Le premier déploiement de la stack de supervision (voir
`docs/bloc4-supervision.md`) a également révélé une anomalie
opérationnelle distincte : le conteneur Grafana ne démarrait pas
(`port is already allocated` sur le port 3000, déjà utilisé par un
autre service du serveur). Correctif : suppression de la publication
de ports sur l'hôte (`ports` → `expose` dans
`docker-compose.monitoring.yml`), Dokploy routant déjà vers les
conteneurs via son propre réseau/proxy interne sans nécessiter de
publication sur l'hôte. Corrigé, commité et redéployé avec succès dans
la foulée — illustration supplémentaire du cycle
détection → analyse → correctif → déploiement → vérification sur un
cas réel.
