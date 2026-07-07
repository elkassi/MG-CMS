# Changes Log — March 2026 Update

## New Features

### 1. CAD Piece Weight Calculation
- **CSV Import**: Import piece details from CAD software export (`/pieceDetailImport`)
- **Weight Calculation**: Calculate real weight per part number cover based on patterns, areas, and material weight/m² (`/weightCalculation`)
- **Cutting Time per PN**: Calculate cutting time attributed to each part number based on perimeter proportion (`/cuttingTimePerPartNumber`)
- **New Entity**: `PieceDetail` — stores imported CSV piece data
- **Entity Changes**:
  - `PartNumberMaterialConfig` — added `weightUnit` (kg/m²)
  - `PartNumberInfo` — added `weight`, `totalPerimetre`, `tempsDeCoupe`

### 2. Work Order SPLIT
- Automatic split when imported quantity < total WO quantity
- Creates new WO in both databases (MG_CMS + qualite) with remaining quantity
- Links back to original WO via `Marker_Group_ID_D`
- Tracks operation in `Remarque_Demande`

### 3. Work Order FUSE
- Auto-detects duplicate part numbers in work orders for same date/shift
- Displays modal dialog with duplicate groups
- Merges quantities into the last WO (highest ID), zeroes out sources
- Updates both databases with tracking info

### 4. Duplicate Detection
- Automatic detection on work order loading
- Visual dialog with per-partNumber fusion option
- User can ignore or fuse selectively

---

## API Endpoints Added

| Endpoint | Method | Feature |
|----------|--------|---------|
| `/api/pieceDetail/import` | POST | CSV import |
| `/api/pieceDetail/all` | GET | Piece list |
| `/api/pieceDetail/{pieceName}` | GET/DELETE | Piece CRUD |
| `/api/pieceDetail/calculateWeight` | POST | Weight calc |
| `/api/partNumberCuttingTime/calculate` | POST | Cutting time calc |
| `/api/workOrder/duplicates` | GET | Detect duplicates |
| `/api/workOrder/fuse` | POST | Fuse WOs |
| `/api/workOrder/split` | POST | Split WO |

---

## Database Changes

```sql
-- New table (auto-created by JPA)
-- PieceDetail with pieceName as PK

-- Alter existing tables
ALTER TABLE PartNumberMaterialConfig ADD weight_unit FLOAT NULL;
ALTER TABLE PartNumberInfo ADD weight FLOAT NULL;
ALTER TABLE PartNumberInfo ADD totalPerimetre FLOAT NULL;
ALTER TABLE PartNumberInfo ADD tempsDeCoupe FLOAT NULL;
```

---

## Frontend Changes

### New Pages
- `/pieceDetailImport` — CSV upload page
- `/weightCalculation` — Weight calculation with breakdown
- `/cuttingTimePerPartNumber` — Cutting time per PN (process)
- `/pieceDetail` — Entity list for piece details

### Modified Pages
- `ImportationNew.js` — Added duplicate dialog, fuse handler, auto-detection
- `Dashboard.js` — Added 4 new menu items under CAD section
- `App.js` — Added 3 new routes
- `metadata.js` — Added `pieceDetail` entity, `weightUnit` to `partNumberMaterialConfig`
