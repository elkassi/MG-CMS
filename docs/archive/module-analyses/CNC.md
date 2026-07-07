# CNC – Module Complet (CNC PS + Contrôle + Qualité + KPI)

## Aperçu

Le module CNC couvre l'intégralité du flux CNC : consommation cuir (CNC PS), contrôle de production (CNC Control), gestion qualité, suivi par shift, et KPI. Il s'adresse à 4 rôles distincts et s'articule autour de sessions (boîtes scannées) enrichies de consommations cuir, puis de contrôles de production CNC.

---

## Rôles

| Rôle | Description |
|---|---|
| `ROLE_CNC_PS` | Opérateur CNC PS – scan des boîtes et saisie des consommations cuir |
| `ROLE_CNC_CONTROL` | Opérateur CNC Control – démarrage de production, contrôles qualité sur machines |
| `ROLE_QUALITE` | Qualité – réouverture / suppression de sessions, supervision |
| `ROLE_PROCESS` | Process – accès aux Programmes CNC (CRUD + Export/Import) |
| `ROLE_ENGINEERING` | Engineering – accès aux Programmes CNC (CRUD + Export/Import) |
| `ROLE_ADMIN` | Administrateur – accès complet + gestion Programmes CNC + Machines CNC |

---

## Flux de travail (Workflow)

### Étape 1 – Consommation Cuir (ROLE_CNC_PS)

#### 1.1 Scan de la boîte

L'opérateur scanne le boxId (ex : `S12345`). Le système cherche dans `GammeTechniqueImprimer` via `nSerieGammeImp = <partie numérique>`.

**Si une session existe déjà**, elle est retournée avec ses consommations et patterns. Sinon, les détails de la boîte sont affichés pour création.

**Informations affichées :**

| Champ | Description |
|---|---|
| `partNumberImp` | Part Number de la pièce |
| `code1Imp` | Code 1 (cuir) |
| `code3Imp` | Code 3 |
| `quantiteImp` | Quantité boîte |

**Patterns CNC :** Recherchés depuis la table `Files` (base CTC) où `type = 'CNC'` et `partNumberCover` = PN boîte. Pour chaque pattern, le `ProgramCNC` associé fournit : version, row, set, programme, cassette, Fil Couture CNC, Fil blind, profil, type (Insert/Upper).

#### 1.2 Création de session

Session créée avec les infos de la gamme + opérateur connecté. BoxId = UNIQUE (409 CONFLICT si dupliqué).

#### 1.3 Consommation cuir

Pour chaque consommation :
- **PN Cuir** : Part Number du cuir (alphanumeric only)
- **Serial** : Numéro de série (alphanumeric only)
- **Lot** : doit commencer par `"H"`
- **Quantité Initiale** : quantité disponible (Double)

**Navigation clavier** : chaque champ Enter → focus champ suivant (via React refs). Seuls les caractères `a-z A-Z 0-9` sont autorisés (handleAlphanumericChange).

**Calcul automatique :**
- `quantiteConsumed` = min(restante boîte, quantiteInitial)
- `quantiteRetour` = quantiteInitial - quantiteConsumed

**Prérequis cuir** : Si `code1Imp` existe (la boîte a du cuir), la consommation doit être totalement complétée avant que le contrôle CNC puisse démarrer. Si pas de code1Imp, la session peut être complétée directement sans consommation.

#### 1.4 Fin de consommation

Validation : somme des `quantiteConsumed` = `quantiteImp`. Si code1Imp absent, la complétion est autorisée sans consommation.

#### 1.5 Impression Zebra (fallback réseau + local)

**Détection imprimante :**
1. **Réseau** : TCP/IP sur l'IP utilisateur (`ipPrinter`), port 9100
2. **Local ZEBRA** : Si réseau non disponible, javax.print.PrintServiceLookup recherche une imprimante locale dont le nom contient "ZEBRA"

L'envoi ZPL utilise `sendZpl()` qui essaie d'abord le réseau, puis le fallback local ZEBRA.

**Badge imprimante** : 
- `Réseau (IP)` vert = connectée via réseau
- `Local ZEBRA (nom)` bleu = connectée via imprimante locale
- `Déconnectée` rouge = aucune imprimante

**Étiquettes :**
- **Retour cuir** : pour les consommations avec quantiteRetour > 0 (codes-barres Code 128)
- **Étiquette boîte** : détails boîte + **code-barres ID boîte** + patterns + version/row/set/Fil Couture CNC/Fil blind ; chaque programme dans un grand rectangle avec séparation tabulaire

### Étape 2 – Contrôle CNC (ROLE_CNC_CONTROL)

#### 2.1 Scanner la boîte

L'opérateur CNC Control scanne le boxId. Le système retrouve la session existante.

#### 2.2 Démarrer la production (2 étapes)

**Étape A – Démarrer :**
L'opérateur sélectionne :
- **Machine CNC** (dropdown depuis GET /api/cncPs/machines) – **obligatoire**
- **Date début production** (datetime-local) – **optionnel**, défaut = maintenant

Champs envoyés : `machineCncId`, `startProductionDate` (optionnel).
Le statut passe à **"In progress"**. L'opérateur de production est enregistré automatiquement.

**Étape B – Compléter :**
L'opérateur renseigne :
- **Date fin production** (datetime-local) – **obligatoire** pour compléter

**Validation dates** : Les dates doivent être entre `maintenant - 2 jours` et `maintenant`. startDate < endDate obligatoire.

**Format dates** : Le "T" du format `datetime-local` HTML est remplacé par un espace avant envoi au backend (`2026-03-04T13:50` → `2026-03-04 13:50`).

**Auto-complétion** : Quand la somme des contrôles atteint la quantité boîte, le système demande automatiquement une date de fin pour compléter.

#### 2.3 Ajouter des contrôles

Chaque contrôle contient :
- **Quantité** : nombre de pièces contrôlées (ne doit pas dépasser la qté restante)
- **Résultat** : OK ou NOK
- **Code Défaut** (si NOK) : sélection depuis les codes commençant par "CNC"
- **Code Scrap** (si NOK) : sélection depuis les codes commençant par "CNC"

**Règle NOK** : Si résultat = NOK, au moins un code défaut ou code scrap est obligatoire.

**Auto-complétion** : Quand la somme des quantités contrôlées atteint la quantité boîte ET que la production a une date de fin, la production est automatiquement marquée "Complete".

**Restriction suppression** : Un opérateur ROLE_CNC_CONTROL ne peut supprimer un contrôle que s'il a été créé il y a moins de 24h. Les rôles QUALITE et ADMIN peuvent supprimer sans restriction.

### Étape 3 – Gestion Qualité (ROLE_QUALITE / ROLE_ADMIN)

- **Réouverture** : Réouvrir une session complétée (POST /session/{id}/reopen) → remet `completed=false`, `productionStatus=null`, `machineCnc=null`, `startProductionDate=null`, `endProductionDate=null`, `productionOperator=null`
- **Suppression** : Supprimer une session avec toutes ses consommations et contrôles (DELETE /session/{id})
- **Recherche** : Par boxId, par statut, par date
- **Pagination** : Les sessions sont récupérées paginées (GET `/api/cncPs/all?size=500`), résultat `Page<CncPsSession>` → utiliser `res.data.content`

### Étape 4 – Statut Shift

Affichage temps réel des indicateurs par shift :

| Shift | Horaires |
|---|---|
| Shift 1 | 21:50 (veille) → 05:50 |
| Shift 2 | 05:50 → 13:50 |
| Shift 3 | 13:50 → 21:50 |

**Indicateurs** : sessions totales, complétées, en cours, en attente, qté boîtes (`totalBoxQuantity`), OK/NOK (`totalControlOK`/`totalControlNOK`), par opérateur (`byOperator` – Map<String,Long>), par PN (`byPartNumber` – Map<String,Long>).

**Note waitingSessions** : Les sessions sans `productionStatus` (null) sont comptées comme "en attente" (Waiting).

### Étape 5 – KPI

Analyse sur plage de dates :
- **Taux qualité** : OK / (OK + NOK) × 100
- Ventilation par jour (`byDay`), PN (`byPartNumber`), opérateur (`byOperator`), machine (`byMachine`) – tous `Map<String,Long>` (session count)
- Défauts (`defautBreakdown`) et scraps (`scrapBreakdown`) – `Map<String,Integer>` (quantité NOK)
- Compteurs : `totalSessions`, `totalBoxQuantity`, `totalControlOK`, `totalControlNOK`

---

## Entités

### `CncPsSession`

| Colonne | Type | Contrainte | Description |
|---|---|---|---|
| `id` | Long | PK, auto-gen | Identifiant |
| `boxId` | String | **UNIQUE** | ID de la boîte scannée |
| `nSequenceImp` | String | | Séquence |
| `partNumberImp` | String | | Part Number |
| `code1Imp` | String | | Code1 (cuir) |
| `code3Imp` | String | | Code3 |
| `quantiteImp` | String | | Quantité boîte |
| `operator` | String | | Opérateur PS |
| `createdAt` | LocalDateTime | | Date/heure création |
| `completed` | Boolean | | Session cuir terminée |
| `labelPrinted` | Boolean | | Étiquette imprimée |
| `machineCnc` | MachineCnc | FK @ManyToOne | Machine CNC assignée |
| `startProductionDate` | LocalDateTime | | Début production |
| `endProductionDate` | LocalDateTime | | Fin production |
| `productionStatus` | String | | Waiting / In progress / Complete |
| `productionOperator` | String | | Opérateur production |

Relations :
- `@OneToMany` → `CncPsLeatherConsumption` (cascade ALL, orphanRemoval) – `@JsonManagedReference("session-consumptions")` – **Set** (pas List, pour éviter MultipleBagFetchException Hibernate)
- `@OneToMany` → `CncControl` (cascade ALL, orphanRemoval) – `@JsonManagedReference("session-controls")` – **Set** (pas List)

### `CncPsLeatherConsumption`

| Colonne | Type | Description |
|---|---|---|
| `id` | Long | Identifiant auto-généré |
| `session` | CncPsSession | FK @ManyToOne – `@JsonBackReference("session-consumptions")` |
| `leatherPartNumber` | String | PN cuir |
| `serial` | String | Numéro de série |
| `lot` | String | Lot (commence par "H") |
| `quantiteInitial` | Double | Quantité initiale |
| `quantiteConsumed` | Double | Quantité consommée (calculée) |
| `quantiteRetour` | Double | Quantité retour (calculée) |
| `createdAt` | LocalDateTime | Date/heure |

### `CncControl`

| Colonne | Type | Description |
|---|---|---|
| `id` | Long | Identifiant auto-généré |
| `session` | CncPsSession | FK @ManyToOne – `@JsonBackReference("session-controls")` |
| `quantite` | Integer | Nombre de pièces contrôlées |
| `result` | String | OK ou NOK |
| `codeDefaut` | String | Code défaut (si NOK) |
| `codeScrap` | String | Code scrap (si NOK) |
| `createdAt` | LocalDateTime | Date/heure création |

### `ProgramCNC`

| Colonne | Type | Description |
|---|---|---|
| `id` | Long | Identifiant auto-généré |
| `partNumber` | String | Part Number |
| `profil` | String | Profil |
| `type` | String | Type de pièce : `Insert` ou `Upper` |
| `pattern` | String | Pattern CNC |
| `programNumber` | String | Numéro de programme |
| `casette` | String | Cassette |
| `version` | String | Version |
| `row` | String | Row (colonne SQL: `row_num`) |
| `set` | String | Set (colonne SQL: `set_num`) |
| `coutureDecorativeCnc` | String | Fil Couture CNC |
| `blindStitch` | String | Fil blind |

CRUD via le composant générique EntityList/EntityForm (ADMIN, PROCESS, ENGINEERING). Operations: Add, Edit, Delete, Export, Import.

### `MachineCnc`

| Colonne | Type | Description |
|---|---|---|
| `id` | Long | PK, auto-gen |
| `name` | String | Nom de la machine |

CRUD via `/api/machineCnc` et menu Dashboard Machines CNC (admin).

---

## API Endpoints

### CncPs Controller (`/api/cncPs`)

| Méthode | URL | Description | Rôle requis |
|---|---|---|---|
| GET | `/api/cncPs/boxDetails/{boxId}` | Détails boîte + session + patterns | CNC_PS, CNC_CONTROL, QUALITE, ADMIN |
| POST | `/api/cncPs/session` | Créer session (409 si dupliqué) | CNC_PS, ADMIN |
| POST | `/api/cncPs/session/{id}/consume` | Ajouter consommation cuir | CNC_PS, ADMIN |
| POST | `/api/cncPs/session/{id}/complete` | Terminer session (prérequis cuir) | CNC_PS, ADMIN |
| POST | `/api/cncPs/session/{id}/reopen` | Réouvrir session complétée | QUALITE, ADMIN |
| DELETE | `/api/cncPs/session/{id}` | Supprimer session + données | QUALITE, ADMIN |
| POST | `/api/cncPs/session/{id}/startProduction` | Démarrer production (machine obligatoire, startDate optionnel=now) | CNC_CONTROL, QUALITE, ADMIN |
| POST | `/api/cncPs/session/{id}/control` | Ajouter contrôle (qté, résultat, codes) | CNC_CONTROL, QUALITE, ADMIN |
| DELETE | `/api/cncPs/control/{id}` | Supprimer contrôle (24h pour CNC_CONTROL) | CNC_CONTROL, QUALITE, ADMIN |
| GET | `/api/cncPs/session/{id}/controls` | Liste contrôles d'une session | CNC_CONTROL, QUALITE, ADMIN |
| POST | `/api/cncPs/session/{id}/completeProduction` | Compléter production (body: endProductionDate obligatoire) | CNC_CONTROL, QUALITE, ADMIN |
| GET | `/api/cncPs/codesDefautCNC` | Codes défaut commençant par CNC | CNC_CONTROL, QUALITE, ADMIN |
| GET | `/api/cncPs/codesScrapCNC` | Codes scrap commençant par CNC | CNC_CONTROL, QUALITE, ADMIN |
| GET | `/api/cncPs/machines` | Liste machines CNC | Tous rôles CNC |
| POST | `/api/cncPs/machines` | Sauvegarder machine | ADMIN |
| GET | `/api/cncPs/printerStatus` | Statut imprimante (réseau + local ZEBRA) | CNC_PS, ADMIN |
| POST | `/api/cncPs/session/{id}/printLeatherReturn` | Imprimer étiquettes retour cuir | CNC_PS, ADMIN |
| POST | `/api/cncPs/session/{id}/printBoxLabel` | Imprimer étiquette boîte | CNC_PS, ADMIN |
| GET | `/api/cncPs/session/{id}` | Récupérer session | Tous rôles CNC |
| GET | `/api/cncPs/myRecent` | Mes sessions récentes (8h) | CNC_PS, ADMIN |
| GET | `/api/cncPs/all` | Toutes les sessions (paginé: Page<CncPsSession>, params: page, size, sort, dir) | Public |
| GET | `/api/cncPs/shiftStatus` | Statut shift (date, shift) | Tous rôles CNC |
| GET | `/api/cncPs/kpi` | KPI (startDate, endDate) | Tous rôles CNC |

### MachineCnc Controller (`/api/machineCnc`)

| Méthode | URL | Description | Rôle requis |
|---|---|---|---|
| GET | `/api/machineCnc/list` | Liste machines | CNC_CONTROL, QUALITE, ADMIN |
| GET | `/api/machineCnc/all` | Liste machines (paginée) | CNC_CONTROL, QUALITE, ADMIN |
| GET | `/api/machineCnc/{id}` | Détail machine | CNC_CONTROL, QUALITE, ADMIN |
| POST | `/api/machineCnc` | Créer/modifier machine | ADMIN |
| POST | `/api/machineCnc/delete` | Supprimer machine | ADMIN |

### ProgramCNC Controller (`/api/programCNC`)

| Méthode | URL | Description | Rôle requis |
|---|---|---|---|
| GET | `/api/programCNC/all` | Liste programmes (paginée) | ADMIN, PROCESS, ENGINEERING |
| GET | `/api/programCNC/{id}` | Détail programme | ADMIN, PROCESS, ENGINEERING |
| POST | `/api/programCNC` | Créer/modifier programme | ADMIN, PROCESS, ENGINEERING |
| POST | `/api/programCNC/delete` | Supprimer programme | ADMIN, PROCESS, ENGINEERING |
| GET | `/api/programCNC/download/programCNC.xlsx` | Export Excel | ADMIN, PROCESS, ENGINEERING |
| POST | `/api/programCNC/import` | Import Excel (multipart) | ADMIN, PROCESS, ENGINEERING |

---

## Configuration imprimante

**Réseau** : IP via `user.ipPrinter`, port 9100, timeout 2s.  
**Local (fallback)** : javax.print.PrintServiceLookup recherche imprimante dont le nom contient "ZEBRA". Envoi via SimpleDoc (autosense).

Priorité : réseau d'abord, puis local ZEBRA si réseau échoue.

### Étiquette boîte (ZPL)

- **ID Boîte** : affiché en texte + code-barres Code 128 (`^BCN,70`)
- **Programmes CNC** : chaque programme est encadré dans un grand rectangle avec séparation tabulaire interne (lignes horizontales)
- **Fil Couture CNC** et **Fil blind** : police agrandie (34×34 pts) pour une meilleure lisibilité
- **Profil** et **Type** (Insert/Upper) : inclus dans le bloc de chaque programme

---

## Menu Dashboard

Section CNC visible pour `ROLE_CNC_PS`, `ROLE_CNC_CONTROL`, `ROLE_QUALITE`, `ROLE_ADMIN` :

| Lien | Route | Rôles |
|---|---|---|
| Consommation Cuir | `/cncPs` | CNC_PS, ADMIN |
| Contrôle CNC | `/cncControl` | CNC_CONTROL, QUALITE, ADMIN |
| Gestion Qualité | `/cncQualite` | QUALITE, ADMIN |
| Statut Shift | `/cncShiftStatus` | Tous rôles CNC |
| KPI CNC | `/cncKpi` | Tous rôles CNC |
| Historique Sessions | `/cncPsSession` | ADMIN |
| Historique Consommations | `/cncPsLeatherConsumption` | ADMIN |
| Programmes CNC | `/programCNC` | ADMIN, PROCESS, ENGINEERING |
| Machines CNC | `/machineCnc` | ADMIN |

---

## Pages Frontend

### CncPs.js – Consommation Cuir
- Scan boxId, détails boîte, patterns (version/row/set/Fil Couture CNC/Fil blind/profil/type)
- Formulaire consommation : inputs alphanumeric + Enter → next via React refs
- Barre de progression, aperçu calcul, historique sessions 8h
- Badge imprimante (Réseau/Local ZEBRA/Déconnectée)
- Prérequis cuir : si code1Imp absent, complétion sans consommation

### CncControl.js – Contrôle CNC
- Scan boxId pour retrouver session
- **Démarrage production (2 étapes)** :
  - Étape A : Sélection machine CNC (obligatoire) + date début (optionnel, défaut = now) → statut "In progress"
  - Étape B : Date fin production (obligatoire) → statut "Complete"
- Formulaire plus grand : inputs `form-control-lg`, boutons larges, mise en page claire
- Session info en barre horizontale (pas colonne étroite)
- Patterns dans une carte dédiée avec colonne cassette
- Formulaire contrôle : quantité, résultat OK/NOK (select coloré), codes défaut/scrap si NOK
- Barre de progression contrôle vs quantité boîte
- Liste contrôles avec totaux OK/NOK dans l'en-tête, suppression (24h pour CNC_CONTROL)
- Avertissement si prérequis cuir non complété
- Format dates : "T" remplacé par espace pour compatibilité backend

### CncQualite.js – Gestion Qualité
- Recherche par boxId, filtre par statut (complétée/en cours), filtre par date
- Tableau des sessions avec actions : réouvrir (bouton jaune), supprimer (bouton rouge)
- Affichage statut PS + statut production + machine + opérateurs
- Réouverture reset complet : completed, productionStatus, machineCnc, dates, productionOperator
- Données paginées (Page<CncPsSession>) → utilise `res.data.content`

### CncShiftStatus.js – Statut Shift
- Sélection date + shift (auto-détection du shift courant)
- Cartes KPI : sessions totales, complétées, en cours, en attente, qté boîtes (`totalBoxQuantity`), OK/NOK (`totalControlOK`/`totalControlNOK`)
- Tableaux par opérateur et par PN (session count simple – `Map<String,Long>`)
- Liste détaillée des sessions du shift

### CncKpi.js – KPI CNC
- Plage de dates (défaut : derniers 7 jours)
- Taux qualité (coloré selon seuils : ≥95% vert, ≥85% jaune, <85% rouge)
- Ventilations : par jour (`byDay`), PN (`byPartNumber`), opérateur (`byOperator`), machine (`byMachine`) – session count
- Défauts (`defautBreakdown`) et scraps (`scrapBreakdown`) – quantité NOK
- Compteurs : `totalSessions`, `totalBoxQuantity`, `totalControlOK`, `totalControlNOK`

---

## Import / Export CNC (Sync MG-CMS ↔ CMS-CNC)

### Vue d'ensemble
Le module de synchronisation permet l'échange de données bidirectionnel entre MG-CMS et CMS-CNC via des fichiers JSON téléchargés manuellement :

- **Export MG-CMS → CMS-CNC** : Données de référence (machines, programmes, distributions)
- **Import CMS-CNC → MG-CMS** : Rapports de sessions machines (travail effectué sur chaque machine CNC)

### Backend

#### CncSyncController.java (`/api/cncSync`)
| Endpoint | Méthode | Description |
|---|---|---|
| `/api/cncSync/export` | GET | Télécharge un JSON avec machines, programmes, distributions |
| `/api/cncSync/import` | POST | Upload d'un fichier JSON exporté de CMS-CNC (sessions) |
| `/api/cncSync/reports/all` | GET | Liste paginée des rapports importés (filtrable) |
| `/api/cncSync/reports/{id}/pieces` | GET | Pièces d'un rapport spécifique |

- Roles requis : `ROLE_ADMIN`, `ROLE_CNC_PS`, `ROLE_CNC_CONTROL`, `ROLE_PROCESS`, `ROLE_ENGINEERING`
- Import limité à : `ROLE_ADMIN`, `ROLE_PROCESS`, `ROLE_ENGINEERING`

#### CncSyncService.java
- `exportForCnc()` : Sérialise toutes les machines (`MachineCnc`), programmes (`ProgramCNC`), distributions (`ProgrammeDistribution`) en JSON au format attendu par CMS-CNC
- `importFromCnc(file, username)` : Parse le JSON CMS-CNC (format `{sessions: [...]}` avec `workPieces`), crée `CncMachineReport` + `CncMachineReportPiece`, skip les doublons via `sourceSessionId`
- `findAllReports(filters, page, size, sort)` : Recherche paginée avec `JpaSpecificationExecutor`

#### Entités SQL

**CncMachineReport** - Table des rapports machine importés
| Champ | Type | Description |
|---|---|---|
| id | Long (PK) | Auto-incrémenté |
| machineName | String | Nom de la machine CNC |
| boxId | String | ID de la boîte |
| programNumber | String | N° programme |
| partNumber | String | Part Number |
| operator / productionOperator | String | Opérateurs |
| productionStatus | String | Statut production |
| totalPieces / okPieces / defautPieces / scrapPieces | Integer | Compteurs pièces |
| shiftNumber / shiftDate | String | Shift |
| importedAt | LocalDateTime | Date d'import |
| importedBy | String | Utilisateur ayant importé |
| sourceSessionId | Long | ID session CMS-CNC (pour dédoublonnage) |

**CncMachineReportPiece** - Pièces individuelles d'un rapport
| Champ | Type | Description |
|---|---|---|
| id | Long (PK) | Auto-incrémenté |
| report | FK → CncMachineReport | Relation parent |
| programNumber | String | N° programme |
| status / qualityStatus | String | Statut pièce / qualité |
| codeDefaut / codeScrap | String | Codes défaut/scrap |
| qualityComment | String | Commentaire qualité |
| operatorUsername | String | Opérateur |
| startDate / endDate | LocalDateTime | Dates début/fin |
| sourcePieceId | Long | ID pièce CMS-CNC |

### Frontend

#### CncSync.js – Page Import / Export
- **Export** : Bouton "Exporter les Données" → télécharge `mgcms-cnc-export.json`
- **Import** : Sélection fichier JSON + bouton "Importer" → affiche résultat (sessions importées, ignorées, total pièces)
- Route : `/cncSync`

#### Métadonnées (metadata.js)
- `cncMachineReport` : Consultation des rapports importés (search only), triés par date d'import desc
- `cncMachineReportPiece` : Consultation des pièces (search only), triés par ID desc

### Workflow complet
1. Dans MG-CMS : aller sur "Import / Export CNC" → cliquer "Exporter les Données" → fichier `mgcms-cnc-export.json` téléchargé
2. Dans CMS-CNC : aller sur "Import / Export" → importer le fichier → machines, programmes et distributions synchronisés
3. Dans CMS-CNC : travailler normalement (sessions, pièces, contrôle qualité)
4. Dans CMS-CNC : aller sur "Import / Export" → cliquer "Exporter les Sessions" → fichier `cnc-export.json` téléchargé
5. Dans MG-CMS : aller sur "Import / Export CNC" → importer le fichier → rapports de sessions créés
6. Consulter les rapports via "Rapports Machine CNC" et "Pièces Rapport CNC" dans le menu CNC PS
