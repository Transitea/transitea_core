# Guide de release

Comment publier une nouvelle version du backend Transitea. Le
versionnage suit [SemVer](https://semver.org/lang/fr/) (`MAJOR.MINOR.PATCH`)
et s'appuie sur des tags Git + `CHANGELOG.md` (voir ce fichier pour le
detail des versions deja publiees).

## Quand incrementer quoi

| Type de changement | Exemple | Numero incremente |
|---|---|---|
| Correctif sans impact sur le comportement attendu | fix CORS, fix bug d'affichage | PATCH (`1.0.0` -> `1.0.1`) |
| Nouvelle fonctionnalite retro-compatible | nouvel endpoint, nouvelle page | MINOR (`1.0.0` -> `1.1.0`) |
| Changement non retro-compatible | suppression/renommage d'un endpoint, changement de format de reponse | MAJOR (`1.0.0` -> `2.0.0`) |

## Etapes

1. **Verifier que `main` contient tout ce qui doit etre publie.**
   Toutes les PR prevues pour cette version doivent etre mergees sur
   `main`.

2. **Mettre a jour `CHANGELOG.md`** :
   - Renommer la section `[Non publie]` en `[X.Y.Z] - AAAA-MM-JJ` (date
     du jour), et lister les changements sous les sous-titres
     `### Ajoute` / `### Corrige` / `### Modifie` / `### Supprime`
     selon ce qui s'applique (`git log` depuis le tag precedent aide a
     ne rien oublier : `git log vPRECEDENT..HEAD --oneline`).
   - Recreer une section `[Non publie]` vide au-dessus, prete a
     accueillir les prochains changements.
   - Mettre a jour les liens de comparaison tout en bas du fichier.

3. **Committer ce changement** (directement sur `main` ou via une
   branche `docs/changelog-vX.Y.Z` + PR selon la taille du changement).

4. **Poser le tag Git** sur le commit de `main` qui correspond a cette
   version :
   ```bash
   git checkout main
   git pull
   git tag -a vX.Y.Z -m "Version X.Y.Z"
   git push origin vX.Y.Z
   ```

5. **(Optionnel) Creer une Release GitHub** a partir du tag, pour que
   le changelog de cette version soit visible depuis l'onglet
   "Releases" du repo :
   ```bash
   gh release create vX.Y.Z --title "vX.Y.Z" --notes-from-tag
   ```
   Ou depuis l'interface GitHub : "Releases" -> "Draft a new release"
   -> choisir le tag `vX.Y.Z`.

6. **Deployer** : redeployer l'environnement de production sur Dokploy
   depuis ce commit de `main` (voir `docs/manuel-deploiement.md`).

## Exemple

Passage de `1.0.0` a `1.1.0` apres ajout du monitoring Prometheus/Grafana :

```bash
# 1. CHANGELOG.md mis a jour et commite sur main

# 2. Tag
git checkout main
git pull
git tag -a v1.1.0 -m "Version 1.1.0 - monitoring Prometheus/Grafana"
git push origin v1.1.0

# 3. Release GitHub (optionnel)
gh release create v1.1.0 --title "v1.1.0" --notes-from-tag

# 4. Redeploiement sur Dokploy
```
