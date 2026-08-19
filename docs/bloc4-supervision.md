# Bloc 4 — A4.1 Monitorer l'application logicielle

Ce document couvre les critères C4.1.1 (mise à jour des dépendances) et
C4.1.2 (système de supervision) du référentiel RNCP39583, Bloc 4
« Maintenir l'application logicielle en condition opérationnelle ».

## C4.1.1 — Gestion des mises à jour des dépendances et bibliothèques tierces

### Processus

Les mises à jour de dépendances du backend sont gérées par **Dependabot**
(`.github/dependabot.yml`), sur trois périmètres :

| Périmètre | Écosystème | Fréquence |
|---|---|---|
| Dépendances Maven (`pom.xml`) | `maven` | hebdomadaire |
| Image Docker de base (`Dockerfile`) | `docker` | hebdomadaire |
| Workflows GitHub Actions (`.github/workflows/`) | `github-actions` | hebdomadaire |

**Type de mise à jour :** semi-automatique. Dependabot surveille en
continu les nouvelles versions disponibles et ouvre automatiquement une
pull request par mise à jour détectée (jusqu'à 5 PR ouvertes
simultanément pour l'écosystème Maven). La mise à jour effective reste
**manuelle** : chaque PR doit passer la pipeline d'intégration continue
(build + suite de tests complète, voir `.github/workflows/ci.yml`) avant
d'être relue et mergée. Aucune mise à jour n'est appliquée sans
validation humaine ni sans que les tests automatisés confirment
l'absence de régression.

### Exemples réels de mises à jour traitées

| Dépendance | Ancienne version | Nouvelle version | Validation |
|---|---|---|---|
| `zxing` (génération QR code) | 3.5.2 | 3.5.4 | CI verte, mergée |
| `mapstruct` | 1.5.5.Final | 1.6.3 | CI verte, mergée |
| `jjwt` (JWT) | 0.12.6 | 0.13.0 | CI verte, mergée |
| `jacoco-maven-plugin` | — | 0.8.15 | CI verte, mergée |
| `eclipse-temurin` (image Docker) | 21-jre-alpine | 25-jre-alpine | build Docker CI vérifié, mergée |
| `actions/checkout` | v4 | v7 | mergée |
| `actions/setup-java` | v4 | v5 | mergée |
| `actions/upload-artifact` | v4 | v7 | mergée |

### Limite connue

Le frontend (`transitea_front`, dépendances npm) n'est **pas** couvert
par Dependabot à ce jour — voir la recommandation correspondante dans
`docs/bloc4-maintenance.md` (C4.3.1).

## C4.1.2 — Système de supervision et d'alerte

### Architecture

```
[Backend Spring Boot] --/actuator/prometheus (Micrometer)--> [Prometheus] --> [Grafana] --> alertes email
        ^                                                                          |
        |                                                                          v
   [PostgreSQL] <----------------------------- requêtes SQL directes (dashboard métier)
```

Le backend et la base de données sont des ressources Dokploy
indépendantes (Applications/Database managées séparément, pas
orchestrées par un unique Docker Compose). La stack de supervision
(Prometheus + Grafana) est déployée comme une ressource Dokploy
distincte, de type Compose
(`docker-compose.monitoring.yml`) :

- **Prometheus** scrape le backend via son domaine public HTTPS
  (`https://api-dev.transitea.fr/actuator/prometheus`) toutes les 15
  secondes — choix dicté par l'isolation réseau des ressources Dokploy,
  qui empêche un adressage par nom de service Docker interne entre deux
  ressources distinctes.
- **Grafana** lit les métriques Prometheus, et interroge directement
  PostgreSQL pour les métriques métier.

### Sondes mises en place

**Techniques** (exposées automatiquement par Spring Boot Actuator /
Micrometer, sans instrumentation de code supplémentaire) :
- Mémoire JVM (heap, non-heap), garbage collector, threads
- Requêtes HTTP par endpoint : nombre, latence, taux d'erreur
- Pool de connexions HikariCP (connexions actives/inactives)

**Métier** (requêtes SQL directes sur `PostgreSQL`, via la datasource
Grafana dédiée) :
- Nombre de colis créés par jour
- Répartition des colis par statut (`ENREGISTRE`, `EN_TRANSIT`,
  `ARRIVE_AGENCE`, `RETIRE`, `REFUSE`, `RETOUR_EXPEDITEUR`)

### Dashboards

Deux dashboards Grafana, provisionnés automatiquement au démarrage
(`monitoring/grafana/provisioning/`, aucune configuration manuelle
requise) :
1. **Spring Boot Statistics** (dashboard communautaire standard) — vue
   technique complète (JVM, HTTP, pool de connexions).
2. **Transitea — Métriques métier** (dashboard construit
   spécifiquement pour ce projet) — colis créés par jour, répartition
   par statut, latence p95 de l'endpoint de suivi public
   (`/v1/tracking/{codeTracking}`), taux d'erreurs HTTP 5xx par
   endpoint.

### Seuils d'alerte et modalité de signalement

Quatre règles d'alerte provisionnées
(`monitoring/grafana/provisioning/alerting/rules.yaml`), évaluées en
continu par Grafana :

| Règle | Seuil | Durée de confirmation | Sévérité |
|---|---|---|---|
| Backend injoignable | `up == 0` (échec de scrape) | 2 min | critique |
| Taux d'erreurs HTTP 5xx élevé | > 0,5 requête/s | 5 min | warning |
| Latence p95 du suivi public élevée | > 2 secondes | 5 min | warning |
| Mémoire heap JVM proche de la limite | > 90 % du maximum configuré | 5 min | warning |

**Modalité de signalement :** notification par **email**, via un
contact point Grafana configuré sur le SMTP Gmail déjà utilisé par
l'application pour ses propres notifications. Une politique de
notification unique route toutes les alertes vers ce canal
(regroupement par nom d'alerte, ré-envoi toutes les 4h tant que
l'alerte reste active).

Les seuils ci-dessus sont des valeurs de départ raisonnables mais
arbitraires, à ajuster une fois un volume de trafic réel observé (voir
recommandation C4.3.1).

### Justification des choix techniques

Prometheus + Grafana a été préféré à des alternatives plus lourdes :
- **ArgoCD** (GitOps Kubernetes) écarté : l'infrastructure Transitea
  est déployée en conteneurs Docker simples via Dokploy, sans cluster
  Kubernetes — ArgoCD n'aurait aucune utilité dans ce contexte.
- **Stack ELK** (Elasticsearch/Logstash/Kibana) écartée : dimensionnée
  pour de gros volumes de logs, disproportionnée pour le volume de
  trafic actuel du projet.
- Alternative plus légère envisagée (**Uptime Kuma**, simple sonde de
  disponibilité) écartée au profit de Prometheus/Grafana pour disposer
  de métriques applicatives détaillées (JVM, latence par endpoint) en
  plus de la simple disponibilité, nécessaires pour répondre aux
  « critères de qualité et de performance » attendus.

**Limite assumée :** l'endpoint `/actuator/prometheus` est exposé sans
authentification (`permitAll` côté Spring Security), condition
nécessaire pour que Prometheus puisse le scraper via le domaine public
sans partager de secret entre les deux ressources Dokploy isolées. Il
n'expose aucune donnée sensible (uniquement des métriques techniques
et compteurs applicatifs, jamais de données métier ou d'identifiants).
Ce compromis est documenté comme tel plutôt que passé sous silence —
voir la recommandation de durcissement en C4.3.1.
