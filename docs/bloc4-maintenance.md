# Bloc 4 — A4.3 Assurer la maintenance du logiciel

Ce document couvre les critères C4.3.1 (axes d'amélioration), C4.3.2
(journal des versions) et C4.3.3 (collaboration avec le support
client) du référentiel RNCP39583, Bloc 4.

## C4.3.1 — Axes d'amélioration proposés

Recommandations issues directement de l'analyse menée pour les
sections précédentes (supervision, traitement des anomalies), classées
par gain attendu :

| Recommandation | Gain attendu | Coût de mise en œuvre |
|---|---|---|
| Étendre Dependabot au frontend (`transitea_front`, écosystème npm) | Réduit le risque de dépendances JS non maintenues/vulnérables, actuellement hors du processus automatisé décrit en C4.1.1 | Faible — ajout d'un bloc `package-ecosystem: "npm"` dans `dependabot.yml` |
| Sécuriser `/actuator/prometheus` avec une authentification dédiée (Basic Auth) | Réduit la surface d'exposition publique identifiée comme limite assumée en C4.1.2 | Modéré — nécessite une règle Spring Security dédiée + configuration `basic_auth` côté Prometheus |
| Passer le lien nginx → backend en HTTPS en production (`api.transitea.fr` actuellement en HTTP simple) | Ferme la faille de transit en clair déjà identifiée dans `docs/securite-owasp.md` (A02) | Modéré — dépend de la configuration du certificat côté Dokploy |
| Ajuster les seuils d'alerte Grafana une fois un volume de trafic réel observé | Réduit le risque de faux positifs/négatifs sur les 4 règles actuelles (valeurs de départ arbitraires) | Faible — modification de `monitoring/grafana/provisioning/alerting/rules.yaml` |
| Fiabiliser la connexion réseau Grafana → PostgreSQL (dashboard métier) en rattachant les ressources Dokploy au même réseau Docker | Rend le dashboard métier robuste indépendamment de la topologie réseau Dokploy | Faible à modéré — dépend des options réseau exposées par Dokploy |
| Mettre en place une suite de tests automatisés frontend (déjà identifié dans `docs/plan-correction-bogues.md`) | Deux bogues bloquants (BUG-05, BUG-06) se sont produits sur des pages sans aucun test | Élevé — mise en place complète (Vitest + Testing Library) |

## C4.3.2 — Journal des versions

Le projet dispose d'un système de versionnage complet, mis en place au
Bloc 4 :

- **Versionnage sémantique** ([SemVer](https://semver.org/lang/fr/)) :
  `MAJOR.MINOR.PATCH`.
- **`CHANGELOG.md`** (racine du dépôt `transitea_core`) — au format
  [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/), une entrée
  par version publiée, classée par type de changement (Ajouté /
  Corrigé / Modifié / Supprimé).
- **Tags Git annotés** (`vX.Y.Z`), posés sur le commit `main`
  correspondant à chaque version publiée.
- **`docs/guide-release.md`** — procédure documentée pour publier une
  nouvelle version (mise à jour du changelog, tag, release GitHub
  optionnelle, déploiement).

**Version actuelle : `v1.0.0`**, taguée sur le commit `main` de
référence et documentée rétroactivement à partir de l'historique
complet des 60 commits du projet (fonctionnalités, corrections,
sécurité, documentation, dépendances). Voir `CHANGELOG.md` pour le
détail intégral.

Ce système répond directement à l'exigence du référentiel : chaque
version dispose d'un journal reprenant les anomalies corrigées et les
nouvelles fonctionnalités, et les correctifs déployés sont documentés
(voir aussi `docs/bloc4-traitement-anomalies.md`, C4.2.2).

## C4.3.3 — Collaboration avec le support client

### Contexte du retour

Le porteur de projet (agissant côté client/exploitant sur
l'environnement dev) a signalé une impossibilité de connexion :
`https://dev.transitea.fr/login` retournait une erreur 403, empêchant
tout accès à l'application.

### Résolution en collaboration

L'investigation a nécessité plusieurs allers-retours entre expertise
technique et informations d'exploitation, aucune des deux parties ne
disposant seule de tous les éléments :

1. **Hypothèse initiale** (expertise technique) : configuration CORS
   backend incomplète. Vérification faite sur le code source déployé.
2. **Éléments fournis par le porteur de projet** : configuration
   exacte de l'environnement (variables Dokploy, structure des
   domaines configurés — notamment la découverte que le backend dev
   est exposé sous `api-dev.transitea.fr`, un domaine séparé du
   frontend `dev.transitea.fr`), captures d'écran de la configuration
   Dokploy.
3. **Diagnostic technique approfondi** : reproduction de l'anomalie
   par appel direct (`curl`) au backend, qui a permis d'écarter
   l'hypothèse initiale et d'isoler la cause réelle côté frontend
   (proxy nginx pointant en dur vers le backend de production — voir
   `docs/bloc4-traitement-anomalies.md`, C4.2.1/C4.2.2).
4. **Correctif déployé**, avec configuration complémentaire côté
   Dokploy (variable d'environnement) réalisée par le porteur de
   projet suite aux instructions techniques fournies.
5. **Validation croisée** : test technique (`curl`) puis test réel en
   conditions d'usage par le porteur de projet, confirmant la
   résolution complète.

### Contribution des différentes parties prenantes

| Partie | Contribution |
|---|---|
| Porteur de projet / exploitant | Signalement initial, informations d'environnement (config Dokploy, domaines), exécution des changements de configuration côté plateforme, validation finale en conditions réelles |
| Expertise technique | Diagnostic méthodique (élimination d'hypothèses par tests reproductibles), identification de la cause racine, développement et déploiement du correctif |

Ce cas illustre une résolution de problème complexe nécessitant une
collaboration active entre expertise technique et connaissance
opérationnelle de l'environnement — aucune des deux parties n'aurait pu
isoler la cause racine seule, la configuration réseau/domaine
n'apparaissant pas dans le code source.
