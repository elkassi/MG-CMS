import React, { useState, useEffect, useRef, useCallback } from 'react';
import Dashboard from '../Dashboard';
import '../styles/PerforationAnalyzer.scss';

// Load external scripts dynamically
const loadScript = (src) => {
    return new Promise((resolve, reject) => {
        // Check if already loaded
        if (document.querySelector(`script[src="${src}"]`)) {
            resolve();
            return;
        }
        const script = document.createElement('script');
        script.src = src;
        script.async = true;
        script.onload = resolve;
        script.onerror = reject;
        document.head.appendChild(script);
    });
};

const PerforationDistanceAnalyzer = () => {
    // State management
    const [imageFile, setImageFile] = useState(null);
    const [imageSrc, setImageSrc] = useState(null);
    const [isProcessing, setIsProcessing] = useState(false);
    const [opencvReady, setOpencvReady] = useState(false);
    const [chartReady, setChartReady] = useState(false);
    const [error, setError] = useState(null);
    const [results, setResults] = useState(null);
    const [scale, setScale] = useState(1); // pixels per mm
    const [useScale, setUseScale] = useState(false);
    
    // Distance thresholds for filtering
    const [useThreshold, setUseThreshold] = useState(false);
    const [minThreshold, setMinThreshold] = useState(0);
    const [maxThreshold, setMaxThreshold] = useState(100);
    
    // Manual measurement mode
    const [measureMode, setMeasureMode] = useState(false);
    const [measurePoint1, setMeasurePoint1] = useState(null);
    const [measurePoint2, setMeasurePoint2] = useState(null);
    const [manualMeasurement, setManualMeasurement] = useState(null);
    
    // OpenCV parameters (user adjustable)
    const [params, setParams] = useState({
        minDist: 15,
        param1: 50,
        param2: 25,
        minRadius: 3,
        maxRadius: 15,
        blurSize: 5,
        cannyThreshold1: 50,
        cannyThreshold2: 150,
        lineThreshold: 50,
        minLineLength: 30,
        maxLineGap: 20
    });

    // Refs
    const canvasRef = useRef(null);
    const chartCanvasRef = useRef(null);
    const chartInstanceRef = useRef(null);
    const imageRef = useRef(null);

    // Load OpenCV.js and Chart.js
    useEffect(() => {
        const loadLibraries = async () => {
            try {
                // Load Chart.js
                await loadScript('https://cdn.jsdelivr.net/npm/chart.js');
                setChartReady(true);

                // Load OpenCV.js
                await loadScript('https://docs.opencv.org/4.x/opencv.js');
                
                // Wait for OpenCV to be ready
                const waitForOpenCV = () => {
                    return new Promise((resolve) => {
                        const checkCV = setInterval(() => {
                            if (window.cv && window.cv.Mat) {
                                clearInterval(checkCV);
                                resolve();
                            }
                        }, 100);
                        // Timeout after 30 seconds
                        setTimeout(() => {
                            clearInterval(checkCV);
                            resolve();
                        }, 30000);
                    });
                };
                
                await waitForOpenCV();
                
                if (window.cv && window.cv.Mat) {
                    setOpencvReady(true);
                } else {
                    setError('OpenCV.js failed to load properly. Please refresh the page.');
                }
            } catch (err) {
                setError('Failed to load required libraries: ' + err.message);
            }
        };

        loadLibraries();
    }, []);

    // Handle file upload
    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file && (file.type === 'image/jpeg' || file.type === 'image/png')) {
            setImageFile(file);
            setError(null);
            setResults(null);
            
            const reader = new FileReader();
            reader.onload = (event) => {
                setImageSrc(event.target.result);
            };
            reader.readAsDataURL(file);
        } else {
            setError('Please upload a valid JPG or PNG image.');
        }
    };

    // Calculate point-to-line segment distance
    const pointToLineDistance = (px, py, x1, y1, x2, y2) => {
        const A = px - x1;
        const B = py - y1;
        const C = x2 - x1;
        const D = y2 - y1;

        const dot = A * C + B * D;
        const lenSq = C * C + D * D;
        let param = lenSq !== 0 ? dot / lenSq : -1;

        let xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        const dx = px - xx;
        const dy = py - yy;
        return { distance: Math.sqrt(dx * dx + dy * dy), closestX: xx, closestY: yy };
    };

    // Find closest point on polyline
    const findClosestPointOnPolyline = (px, py, lineSegments) => {
        let minDist = Infinity;
        let closestPoint = { x: px, y: py };

        for (const seg of lineSegments) {
            const result = pointToLineDistance(px, py, seg.x1, seg.y1, seg.x2, seg.y2);
            if (result.distance < minDist) {
                minDist = result.distance;
                closestPoint = { x: result.closestX, y: result.closestY };
            }
        }

        return { distance: minDist, closestX: closestPoint.x, closestY: closestPoint.y };
    };

    // Process image with OpenCV
    const processImage = useCallback(() => {
        if (!opencvReady || !imageSrc) {
            setError('OpenCV not ready or no image loaded');
            return;
        }

        setIsProcessing(true);
        setError(null);

        try {
            const cv = window.cv;
            const img = imageRef.current;
            
            if (!img) {
                setError('Image not loaded');
                setIsProcessing(false);
                return;
            }

            // Read image from canvas
            const canvas = canvasRef.current;
            const ctx = canvas.getContext('2d');
            
            canvas.width = img.naturalWidth;
            canvas.height = img.naturalHeight;
            ctx.drawImage(img, 0, 0);

            // Read into OpenCV Mat
            const src = cv.imread(canvas);
            const gray = new cv.Mat();
            const blurred = new cv.Mat();
            const binary = new cv.Mat();
            const edges = new cv.Mat();

            // Convert to grayscale
            cv.cvtColor(src, gray, cv.COLOR_RGBA2GRAY);

            // Apply Gaussian blur
            const ksize = new cv.Size(params.blurSize, params.blurSize);
            cv.GaussianBlur(gray, blurred, ksize, 0);

            // Binary threshold for dot detection
            cv.threshold(blurred, binary, 0, 255, cv.THRESH_BINARY_INV + cv.THRESH_OTSU);

            // Detect circles (perforation dots) using Hough Circle Transform
            const circles = new cv.Mat();
            cv.HoughCircles(
                blurred,
                circles,
                cv.HOUGH_GRADIENT,
                1,
                params.minDist,
                params.param1,
                params.param2,
                params.minRadius,
                params.maxRadius
            );

            // Canny edge detection for thread line
            cv.Canny(blurred, edges, params.cannyThreshold1, params.cannyThreshold2);

            // Detect lines using Probabilistic Hough Transform
            const lines = new cv.Mat();
            cv.HoughLinesP(
                edges,
                lines,
                1,
                Math.PI / 180,
                params.lineThreshold,
                params.minLineLength,
                params.maxLineGap
            );

            // Extract detected circles
            const detectedDots = [];
            for (let i = 0; i < circles.cols; i++) {
                const x = circles.data32F[i * 3];
                const y = circles.data32F[i * 3 + 1];
                const r = circles.data32F[i * 3 + 2];
                detectedDots.push({ x, y, radius: r, index: i + 1 });
            }

            // Extract detected line segments
            const lineSegments = [];
            for (let i = 0; i < lines.rows; i++) {
                const x1 = lines.data32S[i * 4];
                const y1 = lines.data32S[i * 4 + 1];
                const x2 = lines.data32S[i * 4 + 2];
                const y2 = lines.data32S[i * 4 + 3];
                lineSegments.push({ x1, y1, x2, y2 });
            }

            // Check if we have detections
            if (detectedDots.length === 0) {
                setError('No perforation dots detected. Try adjusting the parameters.');
                // Clean up
                src.delete(); gray.delete(); blurred.delete(); binary.delete();
                edges.delete(); circles.delete(); lines.delete();
                setIsProcessing(false);
                return;
            }

            if (lineSegments.length === 0) {
                setError('No thread line detected. Try adjusting the line detection parameters.');
                // Clean up
                src.delete(); gray.delete(); blurred.delete(); binary.delete();
                edges.delete(); circles.delete(); lines.delete();
                setIsProcessing(false);
                return;
            }

            // Calculate distances from each dot to the thread line
            const allDistances = [];
            for (const dot of detectedDots) {
                const result = findClosestPointOnPolyline(dot.x, dot.y, lineSegments);
                allDistances.push({
                    dotIndex: dot.index,
                    dotX: dot.x,
                    dotY: dot.y,
                    dotRadius: dot.radius,
                    closestX: result.closestX,
                    closestY: result.closestY,
                    distancePixels: result.distance,
                    distanceMM: useScale ? result.distance / scale : null
                });
            }

            // Sort by dot index
            allDistances.sort((a, b) => a.dotIndex - b.dotIndex);

            // Filter by threshold if enabled
            let distances = allDistances;
            let skippedCount = 0;
            if (useThreshold) {
                const minPx = useScale ? minThreshold * scale : minThreshold;
                const maxPx = useScale ? maxThreshold * scale : maxThreshold;
                distances = allDistances.filter(d => {
                    const inRange = d.distancePixels >= minPx && d.distancePixels <= maxPx;
                    if (!inRange) skippedCount++;
                    return inRange;
                });
            }

            // Calculate statistics
            const distanceValues = distances.map(d => d.distancePixels);
            const avgDistance = distanceValues.length > 0 
                ? distanceValues.reduce((a, b) => a + b, 0) / distanceValues.length 
                : 0;
            const minDistance = distanceValues.length > 0 ? Math.min(...distanceValues) : 0;
            const maxDistance = distanceValues.length > 0 ? Math.max(...distanceValues) : 0;
            const stdDev = distanceValues.length > 0 
                ? Math.sqrt(distanceValues.reduce((acc, val) => acc + Math.pow(val - avgDistance, 2), 0) / distanceValues.length)
                : 0;

            // Draw visualization
            drawVisualization(ctx, detectedDots, lineSegments, distances, img.naturalWidth, img.naturalHeight);

            // Set results
            setResults({
                dots: detectedDots,
                lines: lineSegments,
                distances,
                allDistances,
                stats: {
                    count: distances.length,
                    totalDetected: allDistances.length,
                    skipped: skippedCount,
                    average: avgDistance,
                    min: minDistance,
                    max: maxDistance,
                    stdDev
                }
            });

            // Clean up OpenCV Mats
            src.delete();
            gray.delete();
            blurred.delete();
            binary.delete();
            edges.delete();
            circles.delete();
            lines.delete();

            setIsProcessing(false);
        } catch (err) {
            console.error('OpenCV processing error:', err);
            setError('Error processing image: ' + err.message);
            setIsProcessing(false);
        }
    }, [opencvReady, imageSrc, params, scale, useScale, useThreshold, minThreshold, maxThreshold]);

    // Draw visualization on canvas
    const drawVisualization = (ctx, dots, lines, distances, width, height) => {
        // Redraw original image
        ctx.drawImage(imageRef.current, 0, 0);

        // Draw detected thread lines (blue)
        ctx.strokeStyle = '#00BFFF';
        ctx.lineWidth = 3;
        for (const line of lines) {
            ctx.beginPath();
            ctx.moveTo(line.x1, line.y1);
            ctx.lineTo(line.x2, line.y2);
            ctx.stroke();
        }

        // Draw detected dots (green circles)
        ctx.strokeStyle = '#00FF00';
        ctx.lineWidth = 2;
        for (const dot of dots) {
            ctx.beginPath();
            ctx.arc(dot.x, dot.y, dot.radius + 3, 0, 2 * Math.PI);
            ctx.stroke();
        }

        // Draw distance lines and labels (red)
        ctx.strokeStyle = '#FF0000';
        ctx.fillStyle = '#FF0000';
        ctx.lineWidth = 1.5;
        ctx.font = 'bold 12px Arial';

        for (const d of distances) {
            // Draw red line from dot to closest point on thread
            ctx.beginPath();
            ctx.moveTo(d.dotX, d.dotY);
            ctx.lineTo(d.closestX, d.closestY);
            ctx.stroke();

            // Draw small circle at closest point
            ctx.beginPath();
            ctx.arc(d.closestX, d.closestY, 3, 0, 2 * Math.PI);
            ctx.fill();

            // Draw distance label
            const midX = (d.dotX + d.closestX) / 2;
            const midY = (d.dotY + d.closestY) / 2;
            const label = useScale 
                ? `${d.distanceMM.toFixed(2)} mm`
                : `${d.distancePixels.toFixed(1)} px`;
            
            // Background for text
            ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
            const textMetrics = ctx.measureText(label);
            ctx.fillRect(midX - 2, midY - 12, textMetrics.width + 4, 16);
            
            // Text
            ctx.fillStyle = '#FF0000';
            ctx.fillText(label, midX, midY);
        }
    };

    // Update chart when results change
    useEffect(() => {
        if (!results || !chartReady || !chartCanvasRef.current) return;

        // Destroy existing chart
        if (chartInstanceRef.current) {
            chartInstanceRef.current.destroy();
        }

        const Chart = window.Chart;
        const ctx = chartCanvasRef.current.getContext('2d');

        const labels = results.distances.map(d => `Dot ${d.dotIndex}`);
        const data = results.distances.map(d => 
            useScale ? d.distancePixels / scale : d.distancePixels
        );
        const unit = useScale ? 'mm' : 'px';

        chartInstanceRef.current = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: `Distance to Thread (${unit})`,
                    data,
                    backgroundColor: 'rgba(54, 162, 235, 0.6)',
                    borderColor: 'rgba(54, 162, 235, 1)',
                    borderWidth: 1
                }, {
                    label: `Average (${results.stats.average.toFixed(2)} ${unit})`,
                    data: Array(data.length).fill(useScale ? results.stats.average / scale : results.stats.average),
                    type: 'line',
                    borderColor: 'rgba(255, 99, 132, 1)',
                    borderWidth: 2,
                    borderDash: [5, 5],
                    fill: false,
                    pointRadius: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: `Distance (${unit})`
                        }
                    },
                    x: {
                        title: {
                            display: true,
                            text: 'Perforation Dot'
                        }
                    }
                },
                plugins: {
                    legend: {
                        position: 'top'
                    },
                    title: {
                        display: true,
                        text: 'Perforation Distance Analysis'
                    }
                }
            }
        });
    }, [results, chartReady, useScale, scale]);

    // Handle image load
    const handleImageLoad = () => {
        if (canvasRef.current && imageRef.current) {
            const canvas = canvasRef.current;
            const img = imageRef.current;
            canvas.width = img.naturalWidth;
            canvas.height = img.naturalHeight;
            const ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0);
        }
    };

    // Get mouse position relative to canvas
    const getCanvasMousePos = (e) => {
        const canvas = canvasRef.current;
        if (!canvas) return null;
        const rect = canvas.getBoundingClientRect();
        const scaleX = canvas.width / rect.width;
        const scaleY = canvas.height / rect.height;
        return {
            x: (e.clientX - rect.left) * scaleX,
            y: (e.clientY - rect.top) * scaleY
        };
    };

    // Handle canvas click for manual measurement
    const handleCanvasClick = (e) => {
        if (!measureMode || !imageSrc) return;
        
        const pos = getCanvasMousePos(e);
        if (!pos) return;

        if (!measurePoint1) {
            setMeasurePoint1(pos);
            setMeasurePoint2(null);
            setManualMeasurement(null);
            drawMeasurePoint(pos, null);
        } else if (!measurePoint2) {
            setMeasurePoint2(pos);
            const distPx = Math.sqrt(
                Math.pow(pos.x - measurePoint1.x, 2) + 
                Math.pow(pos.y - measurePoint1.y, 2)
            );
            const distMm = useScale ? distPx / scale : null;
            setManualMeasurement({ distPx, distMm });
            drawMeasurePoint(measurePoint1, pos, distPx, distMm);
        } else {
            // Reset and start new measurement
            setMeasurePoint1(pos);
            setMeasurePoint2(null);
            setManualMeasurement(null);
            // Redraw image then draw new point
            const canvas = canvasRef.current;
            const ctx = canvas.getContext('2d');
            ctx.drawImage(imageRef.current, 0, 0);
            drawMeasurePoint(pos, null);
        }
    };

    // Draw measurement points and line
    const drawMeasurePoint = (p1, p2, distPx = null, distMm = null) => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ctx = canvas.getContext('2d');

        // Draw first point (yellow)
        ctx.fillStyle = '#FFD700';
        ctx.strokeStyle = '#000';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(p1.x, p1.y, 8, 0, 2 * Math.PI);
        ctx.fill();
        ctx.stroke();

        // Label for point 1
        ctx.fillStyle = '#000';
        ctx.font = 'bold 14px Arial';
        ctx.fillText('P1', p1.x + 12, p1.y + 5);

        if (p2) {
            // Draw second point (yellow)
            ctx.fillStyle = '#FFD700';
            ctx.beginPath();
            ctx.arc(p2.x, p2.y, 8, 0, 2 * Math.PI);
            ctx.fill();
            ctx.stroke();

            // Label for point 2
            ctx.fillStyle = '#000';
            ctx.fillText('P2', p2.x + 12, p2.y + 5);

            // Draw line between points (magenta dashed)
            ctx.strokeStyle = '#FF00FF';
            ctx.lineWidth = 3;
            ctx.setLineDash([8, 4]);
            ctx.beginPath();
            ctx.moveTo(p1.x, p1.y);
            ctx.lineTo(p2.x, p2.y);
            ctx.stroke();
            ctx.setLineDash([]);

            // Draw distance label at midpoint
            const midX = (p1.x + p2.x) / 2;
            const midY = (p1.y + p2.y) / 2;
            let label = `${distPx.toFixed(1)} px`;
            if (distMm !== null) {
                label += ` (${distMm.toFixed(2)} mm)`;
            }

            // Background
            ctx.fillStyle = 'rgba(255, 255, 0, 0.9)';
            const textMetrics = ctx.measureText(label);
            ctx.fillRect(midX - textMetrics.width / 2 - 5, midY - 20, textMetrics.width + 10, 24);

            // Text
            ctx.fillStyle = '#000';
            ctx.font = 'bold 14px Arial';
            ctx.textAlign = 'center';
            ctx.fillText(label, midX, midY - 3);
            ctx.textAlign = 'left';
        }
    };

    // Reset manual measurement
    const resetMeasurement = () => {
        setMeasurePoint1(null);
        setMeasurePoint2(null);
        setManualMeasurement(null);
        if (canvasRef.current && imageRef.current) {
            const ctx = canvasRef.current.getContext('2d');
            ctx.drawImage(imageRef.current, 0, 0);
        }
    };

    return (
        <>
            <Dashboard toggleMenu={() => {}} />
            <div className="perforation-analyzer" style={{ marginLeft: '250px', padding: '20px' }}>
                <h2>
                    <i className="fas fa-circle-dot mr-2"></i>
                    Analyseur de Distance de Perforation
                </h2>
                <p className="text-muted">
                    Analysez les distances entre les perforations et le fil de couture sur des images de matériaux perforés.
                </p>

                {/* Status indicators */}
                <div className="status-bar mb-3">
                    <span className={`badge ${opencvReady ? 'badge-success' : 'badge-warning'} mr-2`}>
                        {opencvReady ? '✓ OpenCV Ready' : '⏳ Loading OpenCV...'}
                    </span>
                    <span className={`badge ${chartReady ? 'badge-success' : 'badge-warning'}`}>
                        {chartReady ? '✓ Chart.js Ready' : '⏳ Loading Chart.js...'}
                    </span>
                </div>

                {/* Error display */}
                {error && (
                    <div className="alert alert-danger" role="alert">
                        <strong>Erreur:</strong> {error}
                    </div>
                )}

                <div className="row">
                    {/* Control Panel */}
                    <div className="col-md-4">
                        <div className="card mb-3">
                            <div className="card-header bg-primary text-white">
                                <strong>Paramètres</strong>
                            </div>
                            <div className="card-body">
                                {/* File Upload */}
                                <div className="form-group">
                                    <label><strong>Image (JPG/PNG)</strong></label>
                                    <input
                                        type="file"
                                        className="form-control-file"
                                        accept="image/jpeg,image/png"
                                        onChange={handleFileChange}
                                        disabled={!opencvReady}
                                    />
                                </div>

                                {/* Scale settings */}
                                <div className="form-group">
                                    <div className="custom-control custom-checkbox">
                                        <input
                                            type="checkbox"
                                            className="custom-control-input"
                                            id="useScale"
                                            checked={useScale}
                                            onChange={(e) => setUseScale(e.target.checked)}
                                        />
                                        <label className="custom-control-label" htmlFor="useScale">
                                            Utiliser échelle (pixels/mm)
                                        </label>
                                    </div>
                                    {useScale && (
                                        <input
                                            type="number"
                                            className="form-control mt-2"
                                            placeholder="Pixels par mm"
                                            value={scale}
                                            onChange={(e) => setScale(parseFloat(e.target.value) || 1)}
                                            min="0.1"
                                            step="0.1"
                                        />
                                    )}
                                </div>

                                {/* Distance threshold filtering */}
                                <div className="form-group">
                                    <div className="custom-control custom-checkbox">
                                        <input
                                            type="checkbox"
                                            className="custom-control-input"
                                            id="useThreshold"
                                            checked={useThreshold}
                                            onChange={(e) => setUseThreshold(e.target.checked)}
                                        />
                                        <label className="custom-control-label" htmlFor="useThreshold">
                                            Filtrer par distance (min/max)
                                        </label>
                                    </div>
                                    {useThreshold && (
                                        <div className="row mt-2">
                                            <div className="col-6">
                                                <label className="small">Min ({useScale ? 'mm' : 'px'})</label>
                                                <input
                                                    type="number"
                                                    className="form-control form-control-sm"
                                                    placeholder="Min"
                                                    value={minThreshold}
                                                    onChange={(e) => setMinThreshold(parseFloat(e.target.value) || 0)}
                                                    min="0"
                                                    step="0.1"
                                                />
                                            </div>
                                            <div className="col-6">
                                                <label className="small">Max ({useScale ? 'mm' : 'px'})</label>
                                                <input
                                                    type="number"
                                                    className="form-control form-control-sm"
                                                    placeholder="Max"
                                                    value={maxThreshold}
                                                    onChange={(e) => setMaxThreshold(parseFloat(e.target.value) || 100)}
                                                    min="0"
                                                    step="0.1"
                                                />
                                            </div>
                                        </div>
                                    )}
                                </div>

                                <hr />

                                {/* Circle detection parameters */}
                                <h6>Détection des Perforations</h6>
                                <div className="form-group">
                                    <label>Min Distance: {params.minDist}</label>
                                    <input
                                        type="range"
                                        className="form-control-range"
                                        min="5"
                                        max="50"
                                        value={params.minDist}
                                        onChange={(e) => setParams({ ...params, minDist: parseInt(e.target.value) })}
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Min Radius: {params.minRadius}</label>
                                    <input
                                        type="range"
                                        className="form-control-range"
                                        min="1"
                                        max="20"
                                        value={params.minRadius}
                                        onChange={(e) => setParams({ ...params, minRadius: parseInt(e.target.value) })}
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Max Radius: {params.maxRadius}</label>
                                    <input
                                        type="range"
                                        className="form-control-range"
                                        min="5"
                                        max="50"
                                        value={params.maxRadius}
                                        onChange={(e) => setParams({ ...params, maxRadius: parseInt(e.target.value) })}
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Param1 (Canny): {params.param1}</label>
                                    <input
                                        type="range"
                                        className="form-control-range"
                                        min="10"
                                        max="200"
                                        value={params.param1}
                                        onChange={(e) => setParams({ ...params, param1: parseInt(e.target.value) })}
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Param2 (Accumulator): {params.param2}</label>
                                    <input
                                        type="range"
                                        className="form-control-range"
                                        min="5"
                                        max="100"
                                        value={params.param2}
                                        onChange={(e) => setParams({ ...params, param2: parseInt(e.target.value) })}
                                    />
                                </div>

                                <hr />

                                {/* Line detection parameters */}
                                <h6>Détection du Fil</h6>
                                <div className="form-group">
                                    <label>Blur Size: {params.blurSize}</label>
                                    <input
                                        type="range"
                                        className="form-control-range"
                                        min="1"
                                        max="15"
                                        step="2"
                                        value={params.blurSize}
                                        onChange={(e) => setParams({ ...params, blurSize: parseInt(e.target.value) })}
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Canny Threshold 1: {params.cannyThreshold1}</label>
                                    <input
                                        type="range"
                                        className="form-control-range"
                                        min="10"
                                        max="200"
                                        value={params.cannyThreshold1}
                                        onChange={(e) => setParams({ ...params, cannyThreshold1: parseInt(e.target.value) })}
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Canny Threshold 2: {params.cannyThreshold2}</label>
                                    <input
                                        type="range"
                                        className="form-control-range"
                                        min="50"
                                        max="300"
                                        value={params.cannyThreshold2}
                                        onChange={(e) => setParams({ ...params, cannyThreshold2: parseInt(e.target.value) })}
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Line Threshold: {params.lineThreshold}</label>
                                    <input
                                        type="range"
                                        className="form-control-range"
                                        min="10"
                                        max="150"
                                        value={params.lineThreshold}
                                        onChange={(e) => setParams({ ...params, lineThreshold: parseInt(e.target.value) })}
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Min Line Length: {params.minLineLength}</label>
                                    <input
                                        type="range"
                                        className="form-control-range"
                                        min="10"
                                        max="100"
                                        value={params.minLineLength}
                                        onChange={(e) => setParams({ ...params, minLineLength: parseInt(e.target.value) })}
                                    />
                                </div>

                                <hr />

                                <button
                                    className="btn btn-success btn-block"
                                    onClick={processImage}
                                    disabled={!imageSrc || !opencvReady || isProcessing}
                                >
                                    {isProcessing ? (
                                        <>
                                            <span className="spinner-border spinner-border-sm mr-2"></span>
                                            Analyse en cours...
                                        </>
                                    ) : (
                                        <>
                                            <i className="fas fa-play mr-2"></i>
                                            Analyser l'image
                                        </>
                                    )}
                                </button>
                            </div>
                        </div>

                        {/* Statistics Summary */}
                        {results && (
                            <div className="card">
                                <div className="card-header bg-info text-white">
                                    <strong>Statistiques</strong>
                                </div>
                                <div className="card-body">
                                    <table className="table table-sm table-bordered">
                                        <tbody>
                                            <tr>
                                                <td><strong>Perforations détectées</strong></td>
                                                <td>{results.stats.totalDetected || results.stats.count}</td>
                                            </tr>
                                            {results.stats.skipped > 0 && (
                                                <tr className="table-warning">
                                                    <td><strong>Hors seuil (ignorées)</strong></td>
                                                    <td>{results.stats.skipped}</td>
                                                </tr>
                                            )}
                                            <tr>
                                                <td><strong>Perforations analysées</strong></td>
                                                <td>{results.stats.count}</td>
                                            </tr>
                                            <tr>
                                                <td><strong>Segments de ligne</strong></td>
                                                <td>{results.lines.length}</td>
                                            </tr>
                                            <tr>
                                                <td><strong>Distance moyenne</strong></td>
                                                <td>
                                                    {useScale 
                                                        ? `${(results.stats.average / scale).toFixed(2)} mm`
                                                        : `${results.stats.average.toFixed(2)} px`
                                                    }
                                                </td>
                                            </tr>
                                            <tr>
                                                <td><strong>Distance minimum</strong></td>
                                                <td>
                                                    {useScale 
                                                        ? `${(results.stats.min / scale).toFixed(2)} mm`
                                                        : `${results.stats.min.toFixed(2)} px`
                                                    }
                                                </td>
                                            </tr>
                                            <tr>
                                                <td><strong>Distance maximum</strong></td>
                                                <td>
                                                    {useScale 
                                                        ? `${(results.stats.max / scale).toFixed(2)} mm`
                                                        : `${results.stats.max.toFixed(2)} px`
                                                    }
                                                </td>
                                            </tr>
                                            <tr>
                                                <td><strong>Écart type</strong></td>
                                                <td>
                                                    {useScale 
                                                        ? `${(results.stats.stdDev / scale).toFixed(2)} mm`
                                                        : `${results.stats.stdDev.toFixed(2)} px`
                                                    }
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Image and Chart Area */}
                    <div className="col-md-8">
                        {/* Hidden image for loading */}
                        {imageSrc && (
                            <img
                                ref={imageRef}
                                src={imageSrc}
                                alt="Uploaded"
                                style={{ display: 'none' }}
                                onLoad={handleImageLoad}
                            />
                        )}

                        {/* Canvas for visualization */}
                        <div className="card mb-3">
                            <div className="card-header bg-dark text-white d-flex justify-content-between align-items-center">
                                <strong>Visualisation</strong>
                                <div>
                                    <button
                                        className={`btn btn-sm ${measureMode ? 'btn-warning' : 'btn-outline-light'} mr-2`}
                                        onClick={() => {
                                            setMeasureMode(!measureMode);
                                            if (measureMode) resetMeasurement();
                                        }}
                                    >
                                        <i className="fas fa-ruler mr-1"></i>
                                        {measureMode ? 'Mode Mesure ON' : 'Mesure Manuelle'}
                                    </button>
                                    {measureMode && (
                                        <button
                                            className="btn btn-sm btn-outline-light"
                                            onClick={resetMeasurement}
                                        >
                                            <i className="fas fa-undo mr-1"></i>
                                            Reset
                                        </button>
                                    )}
                                </div>
                            </div>
                            {measureMode && (
                                <div className="alert alert-warning mb-0 rounded-0">
                                    <small>
                                        <strong>Mode Mesure:</strong> Cliquez sur deux points de l'image pour mesurer la distance.
                                        {manualMeasurement && (
                                            <span className="ml-3 badge badge-dark">
                                                Distance: {manualMeasurement.distPx.toFixed(1)} px
                                                {manualMeasurement.distMm !== null && ` (${manualMeasurement.distMm.toFixed(2)} mm)`}
                                            </span>
                                        )}
                                    </small>
                                </div>
                            )}
                            <div className="card-body canvas-container">
                                {imageSrc ? (
                                    <canvas
                                        ref={canvasRef}
                                        className="analysis-canvas"
                                        onClick={handleCanvasClick}
                                        style={{ cursor: measureMode ? 'crosshair' : 'default' }}
                                    />
                                ) : (
                                    <div className="no-image-placeholder">
                                        <i className="fas fa-image fa-4x mb-3"></i>
                                        <p>Téléchargez une image pour commencer l'analyse</p>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Chart */}
                        {results && (
                            <div className="card">
                                <div className="card-header bg-secondary text-white">
                                    <strong>Graphique des Distances</strong>
                                </div>
                                <div className="card-body">
                                    <div className="chart-container">
                                        <canvas ref={chartCanvasRef}></canvas>
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* Results Table */}
                        {results && (
                            <div className="card mt-3">
                                <div className="card-header bg-secondary text-white">
                                    <strong>Détails par Perforation</strong>
                                </div>
                                <div className="card-body" style={{ maxHeight: '300px', overflowY: 'auto' }}>
                                    <table className="table table-sm table-striped table-hover">
                                        <thead className="thead-light">
                                            <tr>
                                                <th>#</th>
                                                <th>Position X</th>
                                                <th>Position Y</th>
                                                <th>Rayon</th>
                                                <th>Distance</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {results.distances.map((d, i) => (
                                                <tr key={i}>
                                                    <td>{d.dotIndex}</td>
                                                    <td>{d.dotX.toFixed(1)}</td>
                                                    <td>{d.dotY.toFixed(1)}</td>
                                                    <td>{d.dotRadius.toFixed(1)} px</td>
                                                    <td>
                                                        {useScale 
                                                            ? `${(d.distancePixels / scale).toFixed(2)} mm`
                                                            : `${d.distancePixels.toFixed(2)} px`
                                                        }
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </>
    );
};

export default PerforationDistanceAnalyzer;
