# Guide Chef d'Équipe — Rectification des Séquences

> Page : **Rectification Séquences** (`/sequenceRectification`) — visible pour les rôles
> CHEF_EQUIPE, CHEF_DE_ZONE et ADMIN.
> Des actions rapides existent aussi sur **Vue Production (Floor)** (`/productionFloor`).

## 1. Pourquoi cette page existe

Le nombre de **boîtes par zone** affiché dans *Préparation / Release* (`/logisticsRelease`)
et sur le *Floor* est calculé à partir des séquences **en production**
(statuts `Released`, `En cours`, `Matière manquante`). Quand un opérateur ne clôture pas
toutes les séries d'une séquence, la séquence reste **« En cours »** alors qu'elle est
physiquement terminée → ses boîtes continuent d'être comptées et les zones paraissent
saturées.

Cette page permet au chef de **corriger le statut et la zone** de n'importe quelle
séquence récente, et la correction **tient dans le temps** (voir §6).

## 2. Les statuts et leur effet

| Statut (badge) | Signification | Compté dans les boîtes / charge zone ? |
|---|---|---|
| **Importée** (`IMPORTED`) | Importée, pas encore released par la logistique | Non |
| **Released** (`RELEASED`) | Picklist confirmée, zone de release fixée | **Oui** |
| **En cours** (`STARTED`) | Au moins une série en matelassage/coupe | **Oui** |
| **Matière manquante** (`MATERIAL_MISSING`) | Bloquée par un manque de tissu | **Oui** |
| **Terminée** (`COMPLETED`) | Toutes les séries coupées (ou clôture manuelle) | Non — libère les boîtes |
| **Incomplète** (`INCOMPLETE`) | Retirée de la production par un chef (infinissable) | Non |

**Règle simple : pour vider une zone qui paraît pleine à tort, passez les séquences
réellement finies en « Terminée ».**

## 3. La page Rectification Séquences

- **Fenêtre** : séquences planifiées sur le **1 dernier jour par défaut** (options
  1 / 2 / 3 / 7 / 14 / 30 jours).
- **Zone** : filtrer sur votre zone (ou « Sans zone » pour les importées non released).
- **Pastilles de statut** : cliquables — elles filtrent le tableau et donnent le compte
  par statut pour la zone sélectionnée.
- **Pastille « À clôturer »** : le cas exact du nettoyage — séquences **En cours dont
  toutes les séries sont déjà coupées** (colonne Séries en vert, ex. `12/12`).
  Ces lignes sont surlignées en jaune dans le tableau.
- **Colonne Séries** : `coupées/total`. `9/12` en orange = il reste vraiment 3 séries ;
  `12/12` en vert = la séquence est finie, seul le statut est faux.
- **Colonne Boîtes** : boîtes de la séquence comptées dans l'occupation zone.

### Corriger une séquence (ligne par ligne)

1. Repérez la ligne (filtres ou recherche par n° de séquence / projet).
2. Colonne **Rectifier** :
   - liste **Statut…** → choisir le bon statut → confirmer ;
   - liste **Zone…** → choisir la bonne zone → confirmer (« pas ma zone »).
3. La ligne se met à jour ; les compteurs de boîtes de `/logisticsRelease`,
   du Floor et du dispatcher suivent immédiatement.

### Clôture en masse (le grand nettoyage)

1. Filtrez votre zone, cliquez la pastille **« À clôturer »**.
2. Vérifiez que la colonne Séries est bien à `total/total` pour chaque ligne.
3. Cochez les lignes (ou la case d'entête = tout cocher).
4. Bouton bleu **« Marquer Terminée(s) »** → confirmer.
5. Les échecs éventuels sont listés séquence par séquence.

## 4. Cas d'usage typiques

| Situation constatée | Correction |
|---|---|
| Séquence finie mais encore « En cours » (opérateur n'a pas clôturé les séries) | Statut → **Terminée** |
| Séquence marquée « Terminée » par erreur, du travail reste | Statut → **En cours** (ou **Released** si rien n'a commencé) |
| Séquence released vers la mauvaise zone | **Zone…** → bonne zone |
| Séquence released par erreur (ne doit pas être en production) | Statut → **Importée** (elle redevient candidate au release ; sa zone est effacée) |
| Tissu manquant découvert en production | Statut → **Matière manquante** |
| Séquence infinissable à sortir de la production | Statut → **Incomplète** |

**« Importée » avec une zone est refusé** : une séquence pré-release n'a pas de zone ;
elle sera re-released normalement par la logistique.
**« Released » exige une zone** : choisissez d'abord la zone si la séquence n'en a pas.

## 4 bis. Zones automatiques (tant que la picklist n'est pas utilisée)

Tant que la release logistique (`/logisticsRelease`) n'est pas le passage obligé, la
plupart des séquences arrivent en production **sans zone fiable**. Un job corrige donc
automatiquement (toutes les **15 minutes**, fenêtre 14 jours) la zone des séquences
**En cours** et **Terminées** : la zone retenue est celle de la **table qui a travaillé
la dernière série** (date de début de coupe la plus récente), **uniquement si cette
table est dans une zone stricte** — les tables des zones partagées (C/CAP, SPARTEL
presse…) ne donnent aucun signal et la zone reste inchangée.

Le bouton **« Auto-zones »** de la page lance ce recalcul immédiatement et affiche le
bilan (corrigées / verrouillées / sans signal).

Chaque zone affichée porte une étiquette indiquant **qui l'a fixée** :

| Étiquette | Origine | Le job peut-il la modifier ? |
|---|---|---|
| `auto` | Déduite de la table de coupe | Oui (recalculée à chaque passage) |
| `log` | Release logistique (`/logisticsRelease`) | **Non — verrouillée** |
| `chef` | Fixée manuellement (rectification ou « pas ma zone ») | **Non — verrouillée** |
| *(aucune)* | Ancienne donnée, origine inconnue | Oui (traitée comme `auto`) |

Donc : dès que vous utiliserez réellement la picklist, ses zones (`log`) deviennent
définitives et le job cesse d'y toucher ; une correction manuelle de chef (`chef`)
n'est jamais écrasée non plus. Repasser une séquence en « Importée » efface zone et
étiquette.

## 5. Sur la Vue Production (Floor)

Chaque carte de zone garde ses actions rapides sur les séquences en production :
**Terminée**, **Incomplète**, et **« Pas ma zone »** (déplacement). Ces boutons passent
désormais par la rectification : la correction est durable (voir §6). Pour les statuts
`Importée` / `Released` / retours arrière, utilisez la page Rectification Séquences.

## 6. Pourquoi la correction « tient » (technique)

Un job synchronise toutes les **20 minutes** `suiviplanning.Statu` (écrit par CMS desktop,
scanCoupe, CMS-Prod) vers le statut MG-CMS. Avant, une clôture manuelle pouvait être
**annulée au sync suivant** si `suiviplanning` disait encore « En cours ».

La rectification écrit maintenant **aussi dans `suiviplanning`** :

| Statut rectifié | Écrit dans suiviplanning.Statu |
|---|---|
| Importée | `Non demarre` |
| Released | `Released` |
| En cours | `En cours` |
| Terminée | `Complet` |
| Matière manquante / Incomplète | *(rien — le sync ne les écrase jamais)* |

Le sync retombe donc d'accord avec la correction au lieu de la défaire. Chaque
rectification est tracée dans les logs serveur (utilisateur, ancien → nouveau statut,
zone, lignes suiviplanning touchées).

## 7. Désactiver la flexibilité après le nettoyage

Cette liberté totale (n'importe quel statut, retours arrière compris) est prévue pour la
**phase de nettoyage**. Une fois les données saines :

```properties
# application.properties
mgcms.sequence.rectify.enabled=false
```

- la page passe en **lecture seule** (bandeau « rectification désactivée »),
- les endpoints `/api/sequence/*/rectify` et `/rectify-bulk` refusent toute écriture,
- le cycle de vie normal (release logistique, démarrage CMS-Prod, complétion auto)
  continue de fonctionner — seul l'override chef est coupé.

Le job de zones automatiques a son propre interrupteur (à couper quand la picklist
sera le seul attributeur de zones) :

```properties
mgcms.sequence.zoneAutofix.enabled=false
# fenêtre d'analyse du job (jours, défaut 14)
mgcms.sequence.zoneAutofix.days=14
```

## 8. Précautions

- **« Terminée » libère les boîtes** dans tous les écrans — ne clôturez que ce qui est
  physiquement fini (fiez-vous à la colonne Séries `total/total`).
- Une séquence remise « En cours » réapparaît dans la charge de la zone et chez le
  dispatcher.
- Le passage à « Importée » efface la zone de release — la logistique devra la
  re-releaser.
- Les corrections sont visibles par toutes les applications qui lisent `suiviplanning`
  (CMS desktop inclus).
