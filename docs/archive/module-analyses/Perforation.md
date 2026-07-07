# Perforation Distance Analyzer

## Overview

The Perforation Distance Analyzer is a browser-based computer vision tool that analyzes images of perforated materials with sewing thread. It uses OpenCV.js to detect perforation holes (small dark circular dots) and the sewing thread line, then calculates and visualizes the perpendicular distance from each perforation to the thread.

## Features

### Image Processing
- **Circle Detection**: Uses Hough Circle Transform to detect perforation holes
- **Line Detection**: Uses Canny edge detection + Probabilistic Hough Line Transform to detect the thread
- **Distance Calculation**: Calculates perpendicular distance from each dot center to the nearest point on the thread polyline

### Visualization
- **Green circles**: Detected perforation holes
- **Blue lines**: Detected thread segments
- **Red lines**: Distance measurement lines from each hole to the thread
- **Distance labels**: Shows distance value (in pixels or mm) near each measurement line

### Statistics
- Number of detected perforations
- Number of detected line segments
- Average distance
- Minimum distance
- Maximum distance
- Standard deviation

### Chart
- Bar chart showing distance for each detected perforation
- Average line overlay for reference
- Interactive Chart.js visualization

## Technical Implementation

### Dependencies (loaded from CDN)
- **OpenCV.js 4.x**: `https://docs.opencv.org/4.x/opencv.js`
- **Chart.js**: `https://cdn.jsdelivr.net/npm/chart.js`

### Detection Parameters (User Adjustable)

#### Circle Detection (HoughCircles)
| Parameter | Default | Description |
|-----------|---------|-------------|
| minDist | 15 | Minimum distance between circle centers |
| param1 | 50 | Higher threshold for Canny edge detector |
| param2 | 25 | Accumulator threshold for circle detection |
| minRadius | 3 | Minimum circle radius |
| maxRadius | 15 | Maximum circle radius |

#### Line Detection (HoughLinesP)
| Parameter | Default | Description |
|-----------|---------|-------------|
| blurSize | 5 | Gaussian blur kernel size |
| cannyThreshold1 | 50 | First threshold for Canny edge detector |
| cannyThreshold2 | 150 | Second threshold for Canny edge detector |
| lineThreshold | 50 | Accumulator threshold for line detection |
| minLineLength | 30 | Minimum length of detected line segments |
| maxLineGap | 20 | Maximum gap between line segments to merge |

## Usage

### Step 1: Load Image
1. Click "Choose File" and select a JPG or PNG image
2. The image should show perforated material with:
   - Dark circular perforation holes
   - A visible thread line (preferably lighter colored)

### Step 2: Adjust Parameters (if needed)
- If perforations are not detected, try:
  - Decreasing `param2` (lower accumulator threshold)
  - Adjusting `minRadius` and `maxRadius` for your hole size
  - Increasing `minDist` if too many false detections

- If thread is not detected, try:
  - Adjusting `cannyThreshold1` and `cannyThreshold2`
  - Decreasing `lineThreshold`
  - Decreasing `minLineLength`

### Step 3: Scale Conversion (optional)
- Check "Utiliser échelle (pixels/mm)" checkbox
- Enter the pixels per mm ratio
- This converts all measurements to millimeters

### Step 4: Analyze
- Click "Analyser l'image" button
- Wait for processing to complete
- View results on canvas, chart, and statistics panel

## File Structure

```
src/main/js/
├── components/
│   ├── Layout/
│   │   └── PerforationDistanceAnalyzer.js   # Main component
│   └── styles/
│       └── PerforationAnalyzer.scss          # Styles
└── App.js                                    # Route configuration
```

## Route Configuration

- **Path**: `/perforationAnalyzer`
- **Menu**: Qualité → Analyse Perforation
- **Access**: Requires ROLE_QUALITE or ROLE_QUALITE_READER

## Algorithm Details

### Distance Calculation
For each detected perforation center point (px, py), the algorithm:

1. Iterates through all detected line segments
2. For each segment (x1, y1) to (x2, y2):
   - Calculates the perpendicular projection of the point onto the line
   - If projection is outside segment, uses the nearest endpoint
   - Computes Euclidean distance to closest point
3. Returns the minimum distance across all segments

### Formula
```javascript
// Point-to-line-segment distance
const A = px - x1;
const B = py - y1;
const C = x2 - x1;
const D = y2 - y1;

const dot = A * C + B * D;
const lenSq = C * C + D * D;
const param = lenSq !== 0 ? dot / lenSq : -1;

// Clamp to segment
if (param < 0) { xx = x1; yy = y1; }
else if (param > 1) { xx = x2; yy = y2; }
else { xx = x1 + param * C; yy = y1 + param * D; }

distance = sqrt((px - xx)² + (py - yy)²)
```

## Troubleshooting

### "No perforation dots detected"
- Image may be too low contrast
- Adjust circle detection parameters
- Ensure perforations appear as dark circular spots

### "No thread line detected"
- Thread may not have enough contrast
- Adjust Canny thresholds
- Lower the line threshold
- Reduce minimum line length

### "OpenCV.js failed to load"
- Check internet connection (CDN required)
- Try refreshing the page
- Check browser console for errors

### Slow performance
- Large images take longer to process
- Consider resizing images before upload
- OpenCV.js loading takes ~5-10 seconds initially

## Browser Compatibility
- Chrome (recommended)
- Firefox
- Edge
- Safari (WebAssembly support required)

## Future Improvements
- [ ] Support for curved thread detection (Bezier fitting)
- [ ] Batch image processing
- [ ] Export results to CSV/Excel
- [ ] Save annotated image
- [ ] Tolerance thresholds and pass/fail indication
- [ ] Integration with quality database
