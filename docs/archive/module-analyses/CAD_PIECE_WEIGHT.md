# CAD Piece Weight Calculation — Feature Documentation

## Overview

This feature enables the CAD team to import CSV files containing piece details from CAD software export, store them in a database table (`PieceDetail`), and calculate the **real weight** of each part number cover based on:
- The patterns (pieces) composing each part number and their areas (from CSV)
- The fabric material used for each pattern (from `Files` entity in CTC)
- The weight per m² of each material (`weightUnit` field on `PartNumberMaterialConfig`)

---

## New Components

### Backend

| Component | Location | Description |
|-----------|----------|-------------|
| `PieceDetail.java` | `domain/PieceDetail.java` | Entity for CSV piece data (PK: `pieceName`) |
| `PieceDetailRepository.java` | `repositories/PieceDetailRepository.java` | JPA repository for PieceDetail |
| `PieceDetailService.java` | `services/PieceDetailService.java` | CSV import + CRUD operations |
| `PartNumberWeightCalculationService.java` | `services/PartNumberWeightCalculationService.java` | Weight calculation logic |
| `PartNumberCuttingTimeService.java` | `services/PartNumberCuttingTimeService.java` | Cutting time per PN calculation |
| `PieceDetailController.java` | `controller/PieceDetailController.java` | REST endpoints for CSV import & weight calc |
| `PartNumberCuttingTimeController.java` | `controller/PartNumberCuttingTimeController.java` | REST endpoints for cutting time |

### Frontend

| Component | Location | Description |
|-----------|----------|-------------|
| `PieceDetailImport.js` | `components/Layout/PieceDetailImport.js` | CSV upload UI |
| `WeightCalculation.js` | `components/Layout/WeightCalculation.js` | Weight calculation UI with breakdown |
| `CuttingTimePerPartNumber.js` | `components/Layout/CuttingTimePerPartNumber.js` | Process: cutting time per PN UI |

---

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/pieceDetail/import` | POST | Upload CSV file, parse and store |
| `/api/pieceDetail/all` | GET | List all pieces (paginated) |
| `/api/pieceDetail/list` | GET | List all pieces |
| `/api/pieceDetail/{pieceName}` | GET | Get single piece |
| `/api/pieceDetail/search?descrip=` | GET | Search by description |
| `/api/pieceDetail/{pieceName}` | DELETE | Delete a piece |
| `/api/pieceDetail/calculateWeight` | POST | Calculate weight for partNumberCovers |
| `/api/partNumberCuttingTime/calculate` | POST | Calculate cutting time per PN |

---

## Weight Calculation Formula

For a given **partNumberCover**:

```
Weight(PN) = Σ (Area(pattern) × quantity(pattern) × weightUnit(material))
```

Where:
- **Area** comes from `PieceDetail` table (imported CSV), in cm² → converted to m² (÷ 10000)
- **quantity** comes from `Files` entity (CTC), for each pattern of type `fabric`
- **weightUnit** comes from `PartNumberMaterialConfig`, weight in kg/m²

---

## Entity Changes

### PartNumberMaterialConfig — New Field
- `weightUnit` (Double) — kg per m² of this material

### PartNumberInfo — New Fields
- `weight` (Double) — Calculated total weight (kg)
- `totalPerimetre` (Double) — Sum of perimeters (cm)
- `tempsDeCoupe` (Double) — Estimated cutting time (minutes)

---

## Database Migration

```sql
ALTER TABLE PartNumberMaterialConfig ADD weight_unit FLOAT NULL;
ALTER TABLE PartNumberInfo ADD weight FLOAT NULL;
ALTER TABLE PartNumberInfo ADD totalPerimetre FLOAT NULL;
ALTER TABLE PartNumberInfo ADD tempsDeCoupe FLOAT NULL;
```

Note: The `PieceDetail` table is auto-created by JPA/Hibernate.

---

## Dashboard Menu

New entries added under **CAD** section:
- **Import Pièces CSV** → `/pieceDetailImport`
- **Calcul Poids** → `/weightCalculation`
- **Détails Pièces** → `/pieceDetail`
- **Temps de Coupe / PN** → `/cuttingTimePerPartNumber`
