# Gamme Technique - Technical Documentation

## Overview

The **Gamme Technique** (Technical Sheet) feature generates visual work instructions showing all piece shapes for a given part number. It's used in production to help operators identify and verify pieces during the manufacturing process.

**Main Component**: `GammePnPage.js` (~1354 lines)  
**Path**: `src/main/js/components/Layout/GammePnPage.js`

---

## Core Concept

The Gamme Technique displays:
1. **Part Number Information** - PN, description, ECN, project, site
2. **Barcodes** - For scanning (Cut Kit, Sequence, Quantity, Work Order)
3. **Visual Piece Layout** - SVG drawings of each piece based on PLT file coordinates
4. **Material Grouping** - Pieces grouped by `partNumberMaterial` (Reftissu)

---

## Data Flow Architecture

### 1. API Endpoint Call
```javascript
axios.get(`/api/cuttingPlan/ctc-info/${pn}/${type}`)
```

**Parameters:**
- `pn`: Part Number (e.g., "A8W6P-000000-XXX")
- `type`: One of:
  - `"fabric"` - Fabric pieces
  - `"supplier kit leather"` - Leather supplier kit pieces
  - `"supplier kit fabric"` - Fabric supplier kit pieces

### 2. Response Data Structure

The API returns an array of CTC file objects:

```javascript
[
  {
    file: {
      partNumber: "PN123",
      partNumberCover: "COVER-PN",
      partNumberCoverDescription: "Cover Description",
      partNumberMaterial: "REFTISSU-001",  // Material reference
      partNumberMaterialDescription: "Material Description",
      panelNumber: "A1",                   // Panel identifier
      pattern: "PATTERN-001",              // Pattern name
      type: "fabric",                      // fabric, supplier kit leather, etc.
      semiFinishedGoodPartNumber: "WL...", // Item code
      quantity: "1",                       // Pieces per panel
      ecnNumber: "ECN-001",
      projet: "P33B"
    },
    graphContent: [                        // PLT file content (coordinates)
      "PU100,200;PD300,400;PD500,600;",
      "PU700,800;PD900,1000;",
      // ... more coordinate strings
    ]
  },
  // ... more pieces
]
```

### 3. PLT Coordinate Parsing

PLT (HP-GL plotter) format uses pen commands:
- **PU** (Pen Up): Move to position without drawing
- **PD** (Pen Down): Draw line to position
- **SI**: Scale instruction (ignored)

**Parsing Logic:**
```javascript
elem.graphContent.map(e => {
  if (!e.includes("SI")) {  // Skip scale instructions
    let arrString = e.split(";")
    arrString.map(contentElem => {
      if (contentElem.startsWith("PU")) {
        // Start new path segment
        if(pointXY.length > 0) {
          arrPointsXY.push(pointXY)
          pointXY = ""
        }
        pointXY = contentElem.replace("PU", "")
      }
      if (contentElem.startsWith("PD")) {
        // Continue drawing
        if (pointXY.length > 0) {
          pointXY += "," + contentElem.replace("PD", "")
        }
      }
    })
  }
})
```

### 4. Coordinate Transformation

PLT coordinates are transformed for SVG display:

```javascript
// Find bounding box
let minX = 9999999999, minY = 9999999999, maxX = 0, maxY = 0
arrPointsXY.map(elemXY => {
  let arrCoords = elemXY.split(",").filter(e => !isNaN(e)).map(e => parseInt(e))
  for (let i = 0; i < (arrCoords.length - 1) / 2; i++) {
    // X coordinates at odd indices, Y at even indices
    if (arrCoords[2 * i + 1] < minX) minX = arrCoords[2 * i + 1]
    if (arrCoords[2 * i + 1] > maxX) maxX = arrCoords[2 * i + 1]
    if (arrCoords[2 * i] < minY) minY = arrCoords[2 * i]
    if (arrCoords[2 * i] > maxY) maxY = arrCoords[2 * i]
  }
})

// Normalize to 0,0 origin and flip orientation
elem.minX = 0
elem.minY = 0
elem.maxX = maxX - minX
elem.maxY = maxY - minY

// Transform points (flip X and Y for display)
elem.pointsXY = arrPointsXY.map(e => {
  let pointsFliped = []
  let points = e.split(",").filter(e => !isNaN(e)).map(num => parseInt(num))
  for (let i = 0; i < (points.length - 1) / 2; i++) {
    pointsFliped.push((maxX - minX) - (points[2 * i + 1] - minX))  // Flip X
    pointsFliped.push((maxY - minY) - (points[2 * i] - minY))      // Flip Y
  }
  return pointsFliped.join(",")
})
```

---

## CNC Panel Detection

For fabric type, the system detects **CNC panels** (panels ending with "CNC"):

```javascript
// Check if any panel ends with "CNC"
hasCNCPanel = arr1.some(elem => 
  elem.file.panelNumber && 
  elem.file.panelNumber.toUpperCase().endsWith("CNC")
);

if (hasCNCPanel) {
  // Merge leather supplier kit into fabric display
  arr1 = [...arr1, ...arr2];
  
  // Collect CNC panel numbers without the suffix
  cncPanels = [...new Set([
    ...arr1.filter(e => e.file.panelNumber?.endsWith("CNC"))
           .map(e => e.file.panelNumber.replace(/CNC$/i, '')),
    ...arr2.filter(e => e.file.panelNumber?.endsWith("CNC"))
           .map(e => e.file.panelNumber.replace(/CNC$/i, ''))
  ])];
}

// Include leather pieces for CNC gammes
let typesToInclude = this.state.type === "fabric" 
  ? (hasCNCPanel 
      ? ["fabric", "supplier kit fabric", "supplier kit leather"]  // CNC: include leather
      : ["fabric", "supplier kit fabric"])                         // Normal: no leather
  : [this.state.type];
```

---

## Layout Algorithm (Bin Packing)

Pieces are arranged using a **bar-based bin packing** algorithm:

### Algorithm Steps:

1. **Sort pieces** by height (largest first)
2. **Initialize bars** - Available vertical spaces
3. **Place each piece** in first fitting bar
4. **Update bars** - Split remaining space after placement
5. **Create new bar** - Add bar at piece's right edge

```javascript
// Initial full-height bar
let bars = [{ x: 0, y1: 0, y2: height * zoom }]

subarr.map(elem => {
  // Find first bar where piece fits
  for (let i = 0; i < bars.length; i++) {
    if (parseInt(elem.maxY) <= parseInt(bars[i].y2 - bars[i].y1) + 1) {
      elem.x = bars[i].x
      elem.y = bars[i].y1
      break;
    }
  }
  
  // Update width tracking
  if (pnInfo[pn].width < elem.x + elem.maxX) {
    pnInfo[pn].width = elem.x + elem.maxX
  }
  
  // Split bars affected by placement
  let newBars = []
  for (let k = 0; k < bars.length; k++) {
    if (bars[k].x < (elem.x + elem.maxX) && 
        !(bars[k].y1 >= (elem.y + elem.maxY) || bars[k].y2 <= elem.y)) {
      // Bar overlaps - split into upper/lower portions
      if (bars[k].y1 < elem.y) {
        newBars.push({ x: bars[k].x, y1: bars[k].y1, y2: elem.y - 5 * zoom })
      }
      if (bars[k].y2 > (elem.y + elem.maxY)) {
        newBars.push({ x: bars[k].x, y1: (elem.y + elem.maxY) + 5 * zoom, y2: bars[k].y2 })
      }
    } else {
      newBars.push(bars[k])
    }
  }
  
  // Add new bar at right edge
  newBars.push({ x: (elem.x + elem.maxX) + 5 * zoom, y1: 0, y2: height * zoom })
  bars = [...newBars.sort((a, b) => a.x - b.x)]
})
```

---

## SVG Rendering

Each piece is rendered as an SVG polygon:

```javascript
<svg height={height} width={width}>
  {elem.pointsXY.map((points, i) => (
    <polygon
      key={`${panelNumber}-${i}`}
      points={points.split(",").map((p, j) => 
        j % 2 === 0 
          ? parseInt(elem.x + parseInt(p)) / zoom 
          : parseInt(elem.y + parseInt(p)) / zoom
      ).join(" ")}
      fill="none"
      stroke="black"
      strokeWidth="1"
    />
  ))}
  
  {/* Panel label */}
  <text
    x={(elem.x + elem.labelX) / zoom}
    y={(elem.y + elem.labelY) / zoom}
    fill="red"
    style={{ fontSize: elem.labelSize, fontWeight: 'bold' }}
  >
    {elem.file.panelNumber}
    {elem.file.quantity !== "1" && `X${elem.file.quantity}`}
  </text>
</svg>
```

---

## Rotation & Inverse Transformations

Pieces can be rotated (0°, 90°, 180°, 270°) and inversed (mirrored):

```javascript
// Apply rotation and inverse transformations
if (elem.rotation != 0 || elem.inverse === true) {
  let { maxX, maxY, minY, minX } = elem
  
  elem.pointsXY = elem.pointsXY.map(pointsString => {
    let pointsFliped = []
    let points = pointsString.split(",").map(num => parseInt(num))
    
    for (let i = 0; i < (points.length - 1) / 2; i++) {
      if (elem.inverse) {
        // Mirror transformations
        if ((elem.rotation + 360) % 360 == 0) {
          pointsFliped.push((maxX + minX) - points[2 * i])
          pointsFliped.push(points[2 * i + 1])
        }
        // ... other rotation + inverse cases
      } else {
        // Rotation only
        if ((elem.rotation + 360) % 360 == 180) {
          pointsFliped.push((maxX + minX) - points[2 * i])
          pointsFliped.push((maxY + minY) - points[2 * i + 1])
        }
        // ... other rotation cases
      }
    }
    return pointsFliped.join(",")
  })
  
  // Swap dimensions for 90°/270° rotations
  if (elem.rotation % 180 == 90) {
    elem.maxX = maxY
    elem.maxY = maxX
  }
}
```

---

## Saved Configuration (GammeTechnique Entity)

Customizations are saved to the database:

### API: Load Config
```javascript
axios.get(`/api/gammeTechnique/${pn}`)
```

### Saved Properties:

| Property | Description |
|----------|-------------|
| `heightRow` | Row height for layout |
| `image` | Custom image path |
| `imageHeight` | Custom image height |
| `gammeTechniqueEmps[]` | Per-piece customizations |
| `gammeTechniquePartNumberMaterials[]` | Per-material customizations |

### Per-Piece Config (`gammeTechniqueEmps`):
```javascript
{
  panelNumber: "A1",
  labelX: 100,       // Label X position
  labelY: 50,        // Label Y position
  labelSize: 20,     // Font size
  rotation: 0,       // 0, 90, 180, 270
  inverse: false     // Mirror flip
}
```

### Per-Material Config (`gammeTechniquePartNumberMaterials`):
```javascript
{
  partNumberMaterial: "REFTISSU-001",
  zoom: 25           // Zoom factor for this material
}
```

---

## Component State

```javascript
state = {
  type: "fabric",          // Current filter type
  pn: "",                  // Part number
  data: [],                // Processed piece data
  arrPn: [],               // List of materials
  pnInfo: {},              // Per-material layout info
  heightRow: 209,          // Default row height
  nbrLigne: 3,             // Number of rows
  modalRotate: false,      // Print rotation
  patternActive: false,    // Show pattern names
  
  // Display info
  pndesc: "",              // PN description
  item: "",                // Item code (WL...)
  itemcode5: "",           // Item code 5 (YL...)
  leatherKit: "",          // Leather kit PN
  supplierKit: "",         // Supplier kit PN
  ecnNumber: "",           // ECN number
  projet: "",              // Project name
  site: "",                // Site name
  
  // CNC handling
  cncPanels: [],           // CNC panel identifiers
  
  // UI
  image: null,             // Custom image
  imageHeight: 258,        // Image height
  arrPltFound: [],         // Panels with missing PLT
  error: null,
  message: null
}
```

---

## API Endpoints Summary

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/cuttingPlan/ctc-info/{pn}/{type}` | GET | Get CTC data with PLT coordinates |
| `/api/gammeTechnique/{pn}` | GET | Load saved configuration |
| `/api/gammeTechnique` | POST | Save configuration |
| `/api/projet/search?nom={projet}` | GET | Get project info (site) |

---

## Printing

Uses `react-to-print` library:

```javascript
<ReactToPrint
  trigger={() => <button><FontAwesomeIcon icon={faPrint} /></button>}
  content={() => this.componentRef}
/>
```

**Print Dimensions:**
- Width: 1545px
- Height: 1100px
- Rotation: Can be rotated 90° for landscape printing

---

## Related Files

| File | Purpose |
|------|---------|
| `GammePnPage.js` | Standalone page component (full editing) |
| `GammePn.js` | Embedded component (used in VerificationQualite) |
| `GammePn.scss` | Styling |
| `GammeTechnique.java` | Backend entity |
| `GammeTechniqueController.java` | REST controller |
| `GammeTechniqueService.java` | Business logic |

---

## Visual Layout Structure

```
┌─────────────────────────────────────────────────────────────────┐
│  LOGO  │    Gamme Technique FR PE 04    │  SITE  │   PROJECT   │
├────────┴────────────────────────────────┼────────┴─────────────┤
│ Part Number: PN123                      │ ECN: ECN-001         │
│ Description: Cover Assembly             │ Date: 2026-01-30     │
├─────────────────────────────────────────┴──────────────────────┤
│ Cut Kit 2 (P): [BARCODE] YL123456                              │
├────────────────────────────────────────────────────────────────┤
│ Sequence: [BARCODE] │ Supplier Kit: [BARCODE] │ Leather Kit:   │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ REFTISSU-001: [zoom: 25]                                │   │
│  │ Material Description                                     │   │
│  │ ┌──────┐ ┌───────────┐ ┌────┐ ┌──────────┐              │   │
│  │ │  A1  │ │    A2     │ │ A3 │ │    A4    │              │   │
│  │ │      │ │           │ │    │ │          │              │   │
│  │ └──────┘ └───────────┘ └────┘ └──────────┘              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ LEATHER-001: [zoom: 30]                (grey background)│   │
│  │ Leather Material                                         │   │
│  │ ┌────────┐ ┌──────┐                                     │   │
│  │ │   B1   │ │  B2  │                                     │   │
│  │ └────────┘ └──────┘                                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
├────────────────────────────────────────────────────────────────┤
│ Cut Kit: [BARCODE] │ Qty: [BARCODE] │ Label: [BARCODE] │ WO:   │
│          WL123456  │      10        │        72135648  │ 112312│
└────────────────────────────────────────────────────────────────┘
```

---

## Known Issue: GammePn.js vs GammePnPage.js

**Problem**: `GammePn.js` (used in `VerificationQualite.js`) doesn't show "supplier kit leather" pieces for CNC gammes.

**Root Cause**: The filtering logic in `GammePn.js` is missing the CNC panel handling that exists in `GammePnPage.js`.

**GammePnPage.js (Correct):**
```javascript
// Line 105-109
let typesToInclude = this.state.type === "fabric" 
  ? (hasCNCPanel 
      ? ["fabric", "supplier kit fabric", "supplier kit leather"]  // ✓ Includes leather
      : ["fabric", "supplier kit fabric"])
  : [this.state.type];

arr = res.data.filter(elem => typesToInclude.includes(elem.file.type.toLowerCase().trim()) ...
```

**GammePn.js (Missing CNC handling):**
```javascript
// Line 104
arr = res.data.filter(elem => (this.state.type === "fabric" 
  ? ["fabric", "supplier kit fabric"].includes(elem.file.type.toLowerCase().trim())  // ✗ Missing leather
  : elem.file.type.toLowerCase().trim() === this.state.type) ...
```

**Fix Required**: Update `GammePn.js` to use the same `typesToInclude` logic with CNC panel detection.

---

## Updates (February 2026)

### GammeTechniqueImprimer - Serie Lookup Endpoint

Added a new endpoint to `GammeTechniqueImprimerController` for looking up gamme technique by série number:

```
GET /api/gammeTechniqueImprimer/serie/{serie}
```

#### Purpose
Used by `BoxWeightFilling.js` to auto-populate box details when a Box ID (série) is scanned.

#### Implementation
- **Controller**: `GammeTechniqueImprimerController.java` - Added `@GetMapping("/serie/{serie}")`
- **Service**: `GammeTechniqueImprimerService.java` - Added `findByNSerieGammeImp(String nSerieGammeImp)` calling `repo.findFirstByNSerieGammeImp(nSerieGammeImp)`
- **Response**: Returns the first matching `GammeTechniqueImprimer` record, or HTTP 400 with message if not found
- **Fields used by BoxWeightFilling**: `partNumberImp` (auto-fills part number), `quantiteImp` (auto-fills quantity)
