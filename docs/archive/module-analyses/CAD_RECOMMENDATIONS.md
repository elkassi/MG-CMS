# CAD (Cutting Plan) - Improvement Recommendations

## Architecture & Code Quality

### 1. Extract CuttingPlanForm.js into Multiple Components
**Priority: High**

`CuttingPlanForm.js` is ~5100 lines, making it very difficult to maintain, test, and debug. Split into smaller focused components:

- `CuttingPlanHeader.js` - Form fields (project, version, definition, dates, type)
- `CuttingPlanPartNumbers.js` - Part number table with BOM loading
- `CuttingPlanMaterials.js` - Materials table with placement rows
- `CuttingPlanPlacementRow.js` - Individual placement row with editing
- `CuttingPlanVerification.js` - Verification panel (verification1-7)
- `CuttingPlanVerificationModal.js` - The "Vérification du plan de coupe" modal
- `CuttingPlanCMSSync.js` - CMS search/sync functionality
- `CuttingPlanSimilar.js` - Similar plans section
- `CuttingPlanDetailView.js` - Read-only detail view

Each component would receive relevant state via props and emit changes through callbacks.

### 2. Move Business Logic to Custom Hooks or Service Layer
**Priority: High**

Extract verification logic, data loading, and computation functions into separate modules:

```
services/
  cuttingPlanLoader.js       - Data loading (BOM, material configs, correspondances)
  cuttingPlanVerification.js - verification1() through verification7()
  cuttingPlanCmsSync.js      - CMS data sync and search
  marginCalculator.js        - getMarge() and related calculations
  pliesConfigParser.js       - PliesConfig parsing utilities
```

### 3. State Management
**Priority: Medium**

Consider using React Context or a state management solution (Redux/Zustand) for this complex form. Currently, state is deeply nested and mutations are spread across many methods.

Key state that could be centralized:
- `modalObj` (the cutting plan data)
- `partNumberMaterialConfigs` (material configurations)
- `optionsList` (dropdown options)
- Verification results

---

## Performance

### 4. Lazy Load Material Configurations
**Priority: Medium**

Currently, all `partNumberMaterialConfig` data is loaded in `componentDidMount` for all materials at once. For plans with many materials (10+), this creates a large initial load. Consider:
- Loading configs on demand when a material section is expanded
- Caching configs in sessionStorage to avoid re-fetching

### 5. Optimize Re-renders
**Priority: Medium**

Many `setState` calls trigger full component re-renders of the 5000+ line component. With the component split (recommendation #1), use `React.memo` or `shouldComponentUpdate` on child components to prevent unnecessary re-renders when only a single placement row changes.

### 6. Debounce Input Fields
**Priority: Low**

Input fields (nbrCouche, drill, longueur) currently trigger `setState` on every keystroke. Debounce these inputs to reduce re-render frequency, especially for the materials table which can have 20+ rows.

---

## Data Integrity

### 7. Server-Side Verification
**Priority: High**

All 7 verification checks currently run client-side only. Critical checks should be duplicated on the backend:
- **verification4** (placement validation) before CMS submission
- **verification1** (PN vs placements) before save
- Material mismatch check with correspondance support on the backend

This prevents invalid data from being saved if the frontend checks are bypassed.

### 8. CMS ID Collision Prevention
**Priority: High**

The current `max(id)+1` strategy for CMS IDs (even with ID reuse) can cause collisions in concurrent scenarios. Consider:
- Using database sequences in the CMS database
- Implementing optimistic locking on the parent `ItemPlanCoupe`
- Using a separate ID allocation service

### 9. Transaction Management for saveToCms
**Priority: Medium**

The `saveToCms` method deletes and recreates many child entities. If any step fails mid-way, the CMS data can be left in an inconsistent state. Ensure the entire operation is wrapped in a single `@Transactional` boundary that rolls back on any failure.

---

## User Experience

### 10. Progressive Loading with Skeleton UI
**Priority: Medium**

When opening a cutting plan, show skeleton/placeholder UI immediately while data loads in the background. Currently, users see a blank or partially loaded form.

### 11. Undo/Redo for Placement Changes
**Priority: Low**

Track placement edits (machine changes, nbrCouche adjustments, activation/deactivation) and allow undo/redo. This is particularly useful when swapping optional placements.

### 12. Batch Verification
**Priority: Medium**

Add a "Run All Verifications" button that executes verification1-7 sequentially and presents a consolidated results panel, instead of requiring users to click each verification individually.

### 13. Keyboard Shortcuts
**Priority: Low**

Add keyboard shortcuts for common actions:
- `Ctrl+S` - Save/Enregistrer
- `Ctrl+Enter` - Run all verifications
- `Tab` / `Shift+Tab` - Navigate between placement fields
- `Esc` - Cancel/Go back

---

## Feature Enhancements

### 14. Correspondance Management UI
**Priority: Medium**

Add a UI to manage `PartNumberCorrespendance` records directly from the cutting plan form. When a material mismatch is detected, offer a button to create a new correspondance rule instead of just flagging it as an error.

### 15. Machine-Specific Margin Visualization
**Priority: Low**

Show a visual chart/graph of the margin intervals (from `reftissuMargins`) for the selected machine, helping users understand the relationship between longueur, nbrCouche, and marge.

### 16. Placement Diff View
**Priority: Medium**

When viewing a copy or updated plan, show a diff view highlighting what changed between versions:
- Added/removed placements
- Changed nbrCouche values
- Changed machine assignments
- Changed drill values

### 17. Smart Placement Suggestions
**Priority: Low**

Based on historical data and material configs, automatically suggest optimal placement configurations:
- Recommended machine type based on longueur and nbrCouche
- Optimal nbrCouche based on consumption and maxPlie
- Category/laize selection based on material properties

---

## Testing

### 18. Unit Tests for Verification Logic
**Priority: High**

Add unit tests for each verification function, covering:
- Edge cases (empty materials, null placements, zero quantities)
- Correspondance matching logic
- LASER-DXF consumption threshold
- Optional placement digit/quantity matching
- Drill validation

### 19. Integration Tests for saveToCms
**Priority: High**

Test the CMS sync flow end-to-end:
- ID reuse with various numbers of existing/new entities
- maxPliePlan computation for different pliesConfig formats
- Machine filter (null machine margins only)
- Concurrent sync operations

### 20. E2E Tests for View Mode
**Priority: Medium**

Test the detail-only view → edit mode transition:
- Verify all data is displayed correctly in read-only mode
- Verify "Modifier" button activates edit mode
- Verify no data is lost during the transition
