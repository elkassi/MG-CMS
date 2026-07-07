import React, { useState } from 'react';
import moment from 'moment';

/**
 * SchedulingGanttChart - Visualize machine scheduling with time on vertical axis
 * 
 * Features:
 * - Time on vertical axis (hours/half-hours)
 * - Machines on horizontal axis
 * - Series displayed as colored blocks
 * - Tooltips showing series details
 * - Color-coded by sequence or material
 * - Zoom controls for time interval
 * - Double-click to view sequence details
 */
const SchedulingGanttChart = ({ assignments, machines, onSerieDoubleClick }) => {
    const [pixelsPerMinute, setPixelsPerMinute] = useState(2);

    const handleZoomIn = () => {
        setPixelsPerMinute(prev => Math.min(prev + 0.5, 6));
    };

    const handleZoomOut = () => {
        setPixelsPerMinute(prev => Math.max(prev - 0.5, 0.5));
    };

    const handleZoomReset = () => {
        setPixelsPerMinute(2);
    };
    if (!assignments || assignments.length === 0) {
        return (
            <div style={{
                background: '#fff',
                borderRadius: 8,
                padding: 24,
                textAlign: 'center',
                color: '#888'
            }}>
                Aucune donnée à afficher
            </div>
        );
    }

    // Calculate time bounds
    let minTime = null;
    let maxTime = null;

    assignments.forEach(a => {
        if (a.scheduledStart) {
            const start = moment(a.scheduledStart);
            if (!minTime || start.isBefore(minTime)) minTime = start.clone();
        }
        if (a.scheduledEnd) {
            const end = moment(a.scheduledEnd);
            if (!maxTime || end.isAfter(maxTime)) maxTime = end.clone();
        }
    });

    if (!minTime || !maxTime) {
        return (
            <div style={{
                background: '#fff',
                borderRadius: 8,
                padding: 24,
                textAlign: 'center',
                color: '#888'
            }}>
                Données de planification incomplètes
            </div>
        );
    }

    // Round to nearest hour
    minTime = minTime.startOf('hour');
    maxTime = maxTime.endOf('hour').add(1, 'hour');

    const totalMinutes = maxTime.diff(minTime, 'minutes');
    const chartHeight = totalMinutes * pixelsPerMinute;
    const machineWidth = 150;
    const timeAxisWidth = 70;
    const headerHeight = 40;

    // Get unique machines from assignments or props
    const machineNames = machines && machines.length > 0
        ? machines.map(m => m.nom)
        : [...new Set(assignments.map(a => a.machineName))].sort();

    // Generate time labels (every 30 minutes)
    const timeLabels = [];
    const current = minTime.clone();
    while (current.isBefore(maxTime)) {
        timeLabels.push({
            label: current.format('HH:mm'),
            offset: current.diff(minTime, 'minutes') * pixelsPerMinute
        });
        current.add(30, 'minutes');
    }

    // Generate color for each sequence
    const sequenceColors = {};
    const colors = [
        '#4299e1', '#48bb78', '#ed8936', '#9f7aea',
        '#ed64a6', '#38b2ac', '#e53e3e', '#dd6b20',
        '#3182ce', '#38a169', '#d69e2e', '#805ad5',
        '#d53f8c', '#319795', '#c53030', '#c05621'
    ];
    let colorIndex = 0;

    assignments.forEach(a => {
        if (!sequenceColors[a.sequenceId]) {
            sequenceColors[a.sequenceId] = colors[colorIndex % colors.length];
            colorIndex++;
        }
    });

    // Group assignments by sequence to find first/last serie
    const bySequence = {};
    assignments.forEach(a => {
        if (!bySequence[a.sequenceId]) {
            bySequence[a.sequenceId] = [];
        }
        bySequence[a.sequenceId].push(a);
    });
    
    // Sort each sequence by scheduledStart and mark first/last
    const serieInfo = {};
    Object.keys(bySequence).forEach(seqId => {
        const sorted = bySequence[seqId].sort((a, b) => 
            moment(a.scheduledStart).diff(moment(b.scheduledStart))
        );
        if (sorted.length > 0) {
            serieInfo[sorted[0].serieId] = { ...serieInfo[sorted[0].serieId], isFirst: true };
            serieInfo[sorted[sorted.length - 1].serieId] = { 
                ...serieInfo[sorted[sorted.length - 1].serieId], 
                isLast: true 
            };
        }
    });

    // Group assignments by machine
    const byMachine = {};
    machineNames.forEach(m => byMachine[m] = []);
    assignments.forEach(a => {
        if (byMachine[a.machineName]) {
            byMachine[a.machineName].push(a);
        }
    });

    const formatDuration = (minutes) => {
        if (!minutes) return '-';
        const hours = Math.floor(minutes / 60);
        const mins = Math.floor(minutes % 60);
        if (hours > 0) {
            return `${hours}h${mins > 0 ? mins + 'm' : ''}`;
        }
        return `${mins}m`;
    };

    return (
        <div style={{
            background: '#fff',
            borderRadius: 8,
            padding: 16,
            overflowX: 'auto',
            overflowY: 'auto',
            maxHeight: 700
        }}>
            <div style={{ 
                margin: '0 0 16px 0', 
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
            }}>
                <h5 style={{ margin: 0, fontWeight: 600 }}>
                    Diagramme de Gantt
                </h5>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 12, color: '#666' }}>Zoom:</span>
                    <button 
                        onClick={handleZoomOut}
                        disabled={pixelsPerMinute <= 0.5}
                        style={{
                            padding: '4px 10px',
                            border: '1px solid #ccc',
                            borderRadius: 4,
                            background: pixelsPerMinute <= 0.5 ? '#f0f0f0' : '#fff',
                            cursor: pixelsPerMinute <= 0.5 ? 'not-allowed' : 'pointer',
                            fontSize: 14,
                            fontWeight: 'bold'
                        }}
                        title="Réduire"
                    >
                        −
                    </button>
                    <button 
                        onClick={handleZoomReset}
                        style={{
                            padding: '4px 10px',
                            border: '1px solid #ccc',
                            borderRadius: 4,
                            background: '#fff',
                            cursor: 'pointer',
                            fontSize: 12
                        }}
                        title="Réinitialiser"
                    >
                        {Math.round(pixelsPerMinute * 50)}%
                    </button>
                    <button 
                        onClick={handleZoomIn}
                        disabled={pixelsPerMinute >= 6}
                        style={{
                            padding: '4px 10px',
                            border: '1px solid #ccc',
                            borderRadius: 4,
                            background: pixelsPerMinute >= 6 ? '#f0f0f0' : '#fff',
                            cursor: pixelsPerMinute >= 6 ? 'not-allowed' : 'pointer',
                            fontSize: 14,
                            fontWeight: 'bold'
                        }}
                        title="Agrandir"
                    >
                        +
                    </button>
                </div>
            </div>
            <div style={{ position: 'relative', minWidth: timeAxisWidth + machineNames.length * machineWidth }}>
                {/* Header with machine names */}
                <div style={{
                    display: 'flex',
                    position: 'sticky',
                    top: 0,
                    zIndex: 10,
                    background: '#fff'
                }}>
                    <div style={{
                        width: timeAxisWidth,
                        minWidth: timeAxisWidth,
                        height: headerHeight,
                        borderRight: '1px solid #e0e6ef',
                        borderBottom: '2px solid #e0e6ef',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontWeight: 600,
                        fontSize: 12,
                        color: '#666'
                    }}>
                        Heure
                    </div>
                    {machineNames.map(machine => (
                        <div
                            key={machine}
                            style={{
                                width: machineWidth,
                                minWidth: machineWidth,
                                height: headerHeight,
                                borderRight: '1px solid #e0e6ef',
                                borderBottom: '2px solid #e0e6ef',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                fontWeight: 600,
                                fontSize: 13,
                                background: '#f8fafc'
                            }}
                        >
                            {machine}
                        </div>
                    ))}
                </div>

                {/* Chart body */}
                <div style={{ display: 'flex' }}>
                    {/* Time axis */}
                    <div style={{
                        width: timeAxisWidth,
                        minWidth: timeAxisWidth,
                        height: chartHeight,
                        borderRight: '1px solid #e0e6ef',
                        position: 'relative',
                        background: '#fafbfc'
                    }}>
                        {timeLabels.map((t, i) => (
                            <div
                                key={i}
                                style={{
                                    position: 'absolute',
                                    top: t.offset,
                                    left: 0,
                                    right: 0,
                                    fontSize: 11,
                                    color: '#666',
                                    textAlign: 'right',
                                    paddingRight: 8,
                                    transform: 'translateY(-50%)'
                                }}
                            >
                                {t.label}
                            </div>
                        ))}
                    </div>

                    {/* Machine columns */}
                    {machineNames.map(machine => (
                        <div
                            key={machine}
                            style={{
                                width: machineWidth,
                                minWidth: machineWidth,
                                height: chartHeight,
                                borderRight: '1px solid #e0e6ef',
                                position: 'relative',
                                background: '#fff'
                            }}
                        >
                            {/* Grid lines */}
                            {timeLabels.map((t, i) => (
                                <div
                                    key={i}
                                    style={{
                                        position: 'absolute',
                                        top: t.offset,
                                        left: 0,
                                        right: 0,
                                        height: 1,
                                        background: t.label.endsWith(':00') ? '#e0e6ef' : '#f0f4fa'
                                    }}
                                />
                            ))}

                            {/* Assignment blocks */}
                            {byMachine[machine].map((a, i) => {
                                const start = moment(a.scheduledStart);
                                const end = moment(a.scheduledEnd);
                                const top = start.diff(minTime, 'minutes') * pixelsPerMinute;
                                const height = end.diff(start, 'minutes') * pixelsPerMinute;
                                const info = serieInfo[a.serieId] || {};
                                const isFirst = info.isFirst;
                                const isLast = info.isLast;

                                // Base border style
                                let borderStyle = a.isLocked ? '2px dashed #333' : '1px solid rgba(0,0,0,0.1)';
                                let borderTop = borderStyle;
                                let borderBottom = borderStyle;
                                
                                // Add indicators for first/last serie in sequence
                                if (isFirst) {
                                    borderTop = '4px solid #28a745'; // Green for start
                                }
                                if (isLast) {
                                    borderBottom = '4px solid #dc3545'; // Red for end
                                }

                                return (
                                    <div
                                        key={`${a.serieId}-${i}`}
                                        title={`Seq: ${a.sequenceId}\nSerie: ${a.serieId}${isFirst ? ' (Début)' : ''}${isLast ? ' (Fin)' : ''}\nDurée: ${formatDuration(a.cuttingDurationMinutes)}\n${start.format('HH:mm')} - ${end.format('HH:mm')}\n(Double-clic pour détails)`}
                                        onDoubleClick={() => {
                                            if (onSerieDoubleClick) {
                                                onSerieDoubleClick(a.sequenceId, a.serieId);
                                            }
                                        }}
                                        style={{
                                            position: 'absolute',
                                            top: top,
                                            left: 4,
                                            right: 4,
                                            height: Math.max(height - 2, 18),
                                            background: sequenceColors[a.sequenceId],
                                            borderRadius: 4,
                                            opacity: a.isLocked ? 0.6 : 1,
                                            borderTop,
                                            borderBottom,
                                            borderLeft: a.isLocked ? '2px dashed #333' : '1px solid rgba(0,0,0,0.1)',
                                            borderRight: a.isLocked ? '2px dashed #333' : '1px solid rgba(0,0,0,0.1)',
                                            cursor: 'pointer',
                                            overflow: 'hidden',
                                            fontSize: 10,
                                            color: '#fff',
                                            padding: 2,
                                            display: 'flex',
                                            flexDirection: 'column',
                                            justifyContent: 'center',
                                            alignItems: 'center',
                                            textShadow: '0 1px 2px rgba(0,0,0,0.3)'
                                        }}
                                    >
                                        {height > 25 && (
                                            <span style={{ fontSize: 8, opacity: 0.9 }}>
                                                {a.serieId}
                                            </span>
                                        )}
                                        {height > 40 && (
                                            <span style={{ fontWeight: 600 }}>
                                                {a.sequenceId}
                                            </span>
                                        )}
                                        {height > 60 && (
                                            <span style={{ fontSize: 9 }}>
                                                {formatDuration(a.cuttingDurationMinutes)}
                                            </span>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    ))}
                </div>
            </div>

            {/* Legend */}
            <div style={{
                marginTop: 16,
                display: 'flex',
                flexWrap: 'wrap',
                gap: 12,
                borderTop: '1px solid #e0e6ef',
                paddingTop: 12,
                alignItems: 'center'
            }}>
                {Object.entries(sequenceColors).map(([seqId, color]) => (
                    <div key={seqId} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <div style={{
                            width: 16,
                            height: 16,
                            borderRadius: 4,
                            background: color
                        }} />
                        <span style={{ fontSize: 12 }}>Seq {seqId}</span>
                    </div>
                ))}
                <div style={{ borderLeft: '1px solid #ccc', paddingLeft: 12, marginLeft: 8, display: 'flex', gap: 12 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <div style={{
                            width: 16,
                            height: 10,
                            borderRadius: 2,
                            background: '#ddd',
                            borderTop: '3px solid #28a745'
                        }} />
                        <span style={{ fontSize: 11, color: '#28a745' }}>Début séquence</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <div style={{
                            width: 16,
                            height: 10,
                            borderRadius: 2,
                            background: '#ddd',
                            borderBottom: '3px solid #dc3545'
                        }} />
                        <span style={{ fontSize: 11, color: '#dc3545' }}>Fin séquence</span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SchedulingGanttChart;
