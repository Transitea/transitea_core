# Manuel de mise à jour — Transitea

Ce manuel décrit le processus de mise à jour du logiciel Transitea (dépendances, code applicatif, base de données), pour le critère C2.4.1 du Bloc 2.

## Suivi des versions

Le projet ne pratique pas encore de versionnage sémantique formel (tags Git, `CHANGELOG.md`) : le suivi des évolutions se fait actuellement par l'historique Git (branches `feat/xxx` / `fix/xxx`, pull requests mergées sur `main`) et par les messages de commit, structurés par type (`feat`, `fix`, `docs`, `test`...). C'est un axe d'amélioration identifié pour une montée en maturité du projet (voir aussi C4.3.2 du référentiel, relatif au Bloc 4).

## Mise à jour des dépendances

Le projet a activé **Dependabot** (`.github/dependabot.yml`) sur trois écosystèmes, avec une vérification hebdomadaire :

- **Maven** (backend) : Spring Boot, Spring Security, dépendances Java.
- **Docker** : images de base (`eclipse-temurin`, `postgres`, `nginx`, `node`).
- **GitHub Actions** : actions utilisées dans les workflows CI.

Dependabot ouvre automatiquement une pull request par dépendance obsolète ou vulnérable détectée. Chaque PR doit passer la CI (build + tests complets) avant fusion — aucune mise à jour de dépendance n'est appliquée sans validation automatisée.

Côté frontend (`transitea_front`), les dépendances npm sont mises à jour manuellement (`npm outdated`, `npm update`) en l'absence de CI dédiée à ce jour sur ce dépôt.

## Processus de déploiement d'une nouvelle version

1. Développement sur une branche dédiée (`feat/xxx` ou `fix/xxx`), partant d'un `main` à jour.
2. Pull request vers `main`, validée par la CI (`.github/workflows/ci.yml` sur `transitea_core` : build Maven + suite de tests complète + vérification que l'image Docker se construit).
3. Fusion sur `main`.
4. Reconstruction et redéploiement des images Docker (backend et/ou frontend selon ce qui a changé) :
   ```bash
   docker-compose up -d --build
   ```
5. Vérification post-déploiement : `GET /actuator/health`, contrôle des logs applicatifs.

## Mise à jour du schéma de base de données

Le projet n'utilise pas d'outil de migration versionnée (type Flyway ou Liquibase) : le schéma est géré par Hibernate via `ddl-auto`.

- En développement (`ddl-auto: update`) : le schéma est mis à jour automatiquement à chaque démarrage en fonction des entités JPA.
- En production (profil Spring `prod`, `ddl-auto: validate`) : Hibernate **vérifie** la cohérence du schéma mais ne le modifie jamais automatiquement.

**Conséquence pour toute mise à jour qui modifie une entité JPA (nouveau champ, nouvelle table, etc.)** : le schéma de production doit être mis à jour manuellement (script SQL ou démarrage temporaire en `ddl-auto: update` sur un environnement contrôlé) *avant* de déployer la nouvelle version en profil `prod`, sous peine d'échec au démarrage (`SchemaManagementException`). C'est le cas par exemple des colonnes `tentatives_echouees` et `verrouille_jusqua` ajoutées à la table `utilisateur` par la fonctionnalité de verrouillage de compte.

**Point d'amélioration recommandé** : introduire Flyway pour tracer et automatiser ces évolutions de schéma de façon fiable et reproductible, plutôt que de dépendre d'une procédure manuelle.

## Rollback

En cas d'anomalie détectée après déploiement :
- **Backend/Frontend** : redéployer l'image Docker précédente (conservée localement ou reconstruite depuis le commit précédent sur `main`).
- **Base de données** : en l'absence de migrations versionnées, un rollback de schéma n'est pas automatisé — à anticiper par une sauvegarde (`pg_dump`) avant toute mise à jour touchant la structure des données.

## Communication des évolutions

À ce jour, les évolutions ne sont pas communiquées via un journal des versions formel. Pour une mise en production à plus grande échelle, il est recommandé de tenir un `CHANGELOG.md` par dépôt, alimenté à chaque fusion sur `main`, reprenant les nouveautés, corrections et éventuelles ruptures de compatibilité (voir aussi le plan de correction des bogues, `docs/plan-correction-bogues.md`, pour l'historique des anomalies déjà traitées).
