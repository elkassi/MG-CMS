import React, { useState, useMemo } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faSpinner, faFire, faSortAmountDown,
    faBalanceScale, faArrowRight, faArrowLeft,
} from '@fortawesome/free-solid-svg-icons';

function cellStyle(loadPct) {
    if (loadPct <= 80) return { bg: '#d4edda', text: '#155724', border: '#c3e6cb' };
    if (loadPct <= 100) return { bg: '#fff3cd', text: '#856404', border: '#ffeeba' };
    return { bg: '#ffebee', text: '#c62828', border: '#f5c6cb' };
}

function computeMachineTypeStats(machineTypes, cells) {
    const stats = {};
    machineTypes.forEach(mt => {
        const mtCells = cells.filter(c => c.machineType === mt && c.machinePresent);
        if (mtCells.length === 0) {
            stats[mt] = null;
            return;
        }
        const loads = mtCells.map(c => c.loadPct);
        const avg = loads.reduce((a, b) => a + b, 0) / loads.length;
        const min = Math.min(...loads);
        const max = Math.max(...loads);
        const deviations = loads.map(l => Math.abs(l - avg));
        const avgDeviation = deviations.reduce((a, b) => a + b, 0) / deviations.length;
        stats[mt] = { avg, min, max, avgDeviation, count: loads.length };
    });
    return stats;
}

function computeCellImbalance(cell, mtStats) {
    const s = mtStats[cell.machineType];
    if (!s) return { deviation: 0, isWorst: false, target: 0 };
    const deviation = Math.abs(cell.loadPct - s.avg);
    // A cell is "worst" if its deviation is > 1.5x the average deviation for that MT
    const isWorst = s.avgDeviation > 0 && deviation > s.avgDeviation * 1.5;
    return { deviation, isWorst, target: s.avg };
}

function computeZoneImbalanceScore(zoneNom, machineTypes, cellMap, mtStats) {
    let score = 0;
    let count = 0;
    machineTypes.forEach(mt => {
        const cell = cellMap[`${zoneNom}|${mt}`];
        if (cell && cell.machinePresent) {
            const { deviation } = computeCellImbalance(cell, mtStats);
            score += deviation;
            count++;
        }
    });
    return count > 0 ? score / count : 0;
}

function findTopPendingSequenceForCell(zoneNom, machineType, liveCharge) {
    if (!liveCharge || !liveCharge.zones) return null;
    const zone = liveCharge.zones.find(z => z.zoneNom === zoneNom);
    if (!zone || !zone.pendingSequences) return null;
    const candidates = zone.pendingSequences.filter(seq =>
        seq.series && seq.series.some(s => s.machine === machineType)
    );
    if (candidates.length === 0) return null;
    return candidates.reduce((best, seq) =>
        (seq.totalRemainingMinutes || 0) > (best.totalRemainingMinutes || 0) ? seq : best
    );
}

function findTopPendingSequenceToMoveIn(targetZoneNom, machineType, liveCharge) {
    if (!liveCharge || !liveCharge.zones) return null;
    let best = null;
    liveCharge.zones.forEach(zone => {
        if (zone.zoneNom === targetZoneNom) return;
        if (!zone.pendingSequences) return;
        const candidates = zone.pendingSequences.filter(seq =>
            seq.series && seq.series.some(s => s.machine === machineType)
        );
        candidates.forEach(seq => {
            if (!best || (seq.totalRemainingMinutes || 0) > (best.seq.totalRemainingMinutes || 0)) {
                best = { seq, sourceZone: zone.zoneNom };
            }
        });
    });
    return best;
}

function fmtPct(p) {
    if (p == null || isNaN(p)) return '—';
    return `${p.toFixed(1)}%`;
}

export default function DispatchHeatmap({ matrix, loading, liveCharge }) {
    const [filterMt, setFilterMt] = useState('ALL');
    const [sortByImbalance, setSortByImbalance] = useState(false);
    const [hoveredCell, setHoveredCell] = useState(null);
    const [selectedCell, setSelectedCell] = useState(null);

    // Extract data safely for hook computations
    const machineTypes = matrix?.machineTypes || [];
    const rows = matrix?.rows || [];
    const cells = matrix?.cells || [];
    const equilibre = matrix?.equilibre;

    const cellMap = useMemo(() => {
        const map = {};
        cells.forEach(c => {
            map[`${c.zoneNom}|${c.machineType}`] = c;
        });
        return map;
    }, [cells]);

    const mtStats = useMemo(() => computeMachineTypeStats(machineTypes, cells), [machineTypes, cells]);

    const visibleMachineTypes = filterMt === 'ALL' ? machineTypes : machineTypes.filter(mt => mt === filterMt);

    const sortedRows = useMemo(() => {
        let result = [...rows].sort((a, b) => {
            if (a.zoneCategory !== b.zoneCategory) return a.zoneCategory === 'SHARED' ? 1 : -1;
            return (a.zoneNom || '').localeCompare(b.zoneNom || '');
        });
        if (sortByImbalance) {
            result = [...result].sort((a, b) => {
                const scoreA = computeZoneImbalanceScore(a.zoneNom, machineTypes, cellMap, mtStats);
                const scoreB = computeZoneImbalanceScore(b.zoneNom, machineTypes, cellMap, mtStats);
                return scoreB - scoreA;
            });
        }
        return result;
    }, [rows, sortByImbalance, machineTypes, cellMap, mtStats]);

    if (loading && !matrix) {
        return (
            <div style={{ padding: 30, textAlign: 'center', color: '#888' }}>
                <FontAwesomeIcon icon={faSpinner} spin /> Chargement de la heatmap…
            </div>
        );
    }
    if (!matrix) return null;

    if (machineTypes.length === 0 || rows.length === 0) {
        return (
            <div style={{ padding: 20, textAlign: 'center', color: '#888' }}>
                Aucune donnée de charge disponible.
            </div>
        );
    }

    return (
        <div style={{ marginBottom: 20 }}>
            {/* Header */}
            <div style={{
                display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12,
                flexWrap: 'wrap',
            }}>
                <h5 className="disp-section-title" style={{ margin: 0 }}>
                    <FontAwesomeIcon icon={faFire} style={{ color: '#EE3124' }} />
                    Charge Zone × Type Machine
                </h5>

                {/* Controls */}
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginLeft: 'auto', alignItems: 'center' }}>
                    <select
                        value={filterMt}
                        onChange={e => setFilterMt(e.target.value)}
                        style={{
                            padding: '4px 8px', fontSize: '0.78rem', borderRadius: 4,
                            border: '1px solid #ccc', background: '#fff',
                        }}
                        title="Filtrer par type de machine"
                    >
                        <option value="ALL">Tous les types</option>
                        {machineTypes.map(mt => (
                            <option key={mt} value={mt}>{mt}</option>
                        ))}
                    </select>

                    <button
                        type="button"
                        onClick={() => setSortByImbalance(v => !v)}
                        style={{
                            padding: '4px 10px', fontSize: '0.78rem', borderRadius: 4,
                            border: '1px solid #ccc', background: sortByImbalance ? '#EE3124' : '#fff',
                            color: sortByImbalance ? '#fff' : '#222', cursor: 'pointer',
                            display: 'inline-flex', alignItems: 'center', gap: 4,
                        }}
                        title="Trier par déséquilibre"
                    >
                        <FontAwesomeIcon icon={sortByImbalance ? faSortAmountDown : faBalanceScale} />
                        {sortByImbalance ? 'Tri déséquilibre' : 'Trier'}
                    </button>

                    {equilibre && (
                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                            <span style={{
                                fontSize: '0.78rem', padding: '3px 10px', borderRadius: 10,
                                fontWeight: 600,
                                background: equilibre.interStatus === 'GREEN' ? '#d4edda'
                                    : equilibre.interStatus === 'AMBER' ? '#fff3cd' : '#ffebee',
                                color: equilibre.interStatus === 'GREEN' ? '#155724'
                                    : equilibre.interStatus === 'AMBER' ? '#856404' : '#c62828',
                            }}>
                                Écart max: {equilibre.interZoneSpreadPct?.toFixed(1)}%
                            </span>
                            {equilibre.hottestZone && (
                                <span style={{
                                    fontSize: '0.78rem', padding: '3px 10px', borderRadius: 10,
                                    fontWeight: 600, background: '#ffebee', color: '#c62828',
                                }} title="Zone la plus chargée">
                                    🔥 {equilibre.hottestZone}
                                </span>
                            )}
                            {equilibre.coolestZone && (
                                <span style={{
                                    fontSize: '0.78rem', padding: '3px 10px', borderRadius: 10,
                                    fontWeight: 600, background: '#d4edda', color: '#155724',
                                }} title="Zone la moins chargée">
                                    ❄️ {equilibre.coolestZone}
                                </span>
                            )}
                        </div>
                    )}
                </div>
            </div>

            {/* Per-machine-type balance bars */}
            <div style={{
                display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 16,
                padding: 12, background: '#fafafa', borderRadius: 8, border: '1px solid #eee',
            }}>
                <div style={{ fontSize: '0.78rem', fontWeight: 600, color: '#555', width: '100%', marginBottom: 4 }}>
                    <FontAwesomeIcon icon={faBalanceScale} /> Équilibre par type de machine
                </div>
                {machineTypes.map(mt => {
                    const s = mtStats[mt];
                    if (!s) return null;
                    const range = s.max - s.min;
                    const isBalanced = range <= 15;
                    return (
                        <div key={mt} style={{ flex: '1 1 160px', minWidth: 140 }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', marginBottom: 2 }}>
                                <span style={{ fontWeight: 600 }}>{mt}</span>
                                <span style={{ color: isBalanced ? '#28a745' : '#c62828', fontWeight: 600 }}>
                                    {fmtPct(s.min)} – {fmtPct(s.max)} (moy. {fmtPct(s.avg)})
                                </span>
                            </div>
                            <div style={{
                                height: 8, background: '#e0e0e0', borderRadius: 4,
                                position: 'relative', overflow: 'hidden',
                            }}>
                                <div style={{
                                    position: 'absolute', left: `${Math.max(0, Math.min(100, s.min))}%`,
                                    width: `${Math.max(1, Math.min(100, range))}%`,
                                    height: '100%', background: isBalanced ? '#28a745' : '#EE3124',
                                    borderRadius: 4, opacity: 0.7,
                                }} />
                                <div style={{
                                    position: 'absolute', left: `${Math.max(0, Math.min(100, s.avg))}%`,
                                    width: 2, height: '100%', background: '#222', transform: 'translateX(-50%)',
                                }} />
                            </div>
                        </div>
                    );
                })}
            </div>

            {/* Heatmap table */}
            <div style={{ overflowX: 'auto' }}>
                <table style={{
                    borderCollapse: 'collapse', width: '100%', fontSize: '0.82rem',
                    minWidth: 600,
                }}>
                    <thead>
                        <tr style={{ background: '#f5f5f5' }}>
                            <th style={{
                                padding: '8px 10px', textAlign: 'left',
                                borderBottom: '2px solid #ddd', position: 'sticky',
                                left: 0, background: '#f5f5f5', zIndex: 1,
                            }}>Zone</th>
                            {visibleMachineTypes.map(mt => (
                                <th key={mt} style={{
                                    padding: '8px 10px', textAlign: 'center',
                                    borderBottom: '2px solid #ddd', whiteSpace: 'nowrap',
                                }}>
                                    {mt}
                                    <div style={{ fontSize: '0.65rem', color: '#888', fontWeight: 400 }}>
                                        cible {fmtPct(mtStats[mt]?.avg)}
                                    </div>
                                </th>
                            ))}
                            <th style={{
                                padding: '8px 10px', textAlign: 'center',
                                borderBottom: '2px solid #ddd',
                            }}>Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        {sortedRows.map(row => {
                            const isShared = row.zoneCategory === 'SHARED';
                            const zoneScore = computeZoneImbalanceScore(row.zoneNom, machineTypes, cellMap, mtStats);
                            return (
                                <tr
                                    key={row.zoneNom}
                                    style={{ borderBottom: '1px solid #eee', cursor: 'pointer' }}
                                    onClick={() => setSelectedCell({ zoneNom: row.zoneNom })}
                                    title="Cliquer pour voir toutes les séquences de cette zone"
                                >
                                    <td style={{
                                        padding: '8px 10px', fontWeight: 600,
                                        whiteSpace: 'nowrap', position: 'sticky',
                                        left: 0, background: '#fff', zIndex: 1,
                                    }}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                                            {sortByImbalance && zoneScore > 0 && (
                                                <span style={{
                                                    fontSize: '0.65rem', background: '#ffebee', color: '#c62828',
                                                    padding: '1px 4px', borderRadius: 4, fontWeight: 700,
                                                }} title={`Déséquilibre moyen: ${zoneScore.toFixed(1)}%`}>
                                                    Δ{zoneScore.toFixed(0)}
                                                </span>
                                            )}
                                            {row.zoneNom}
                                        </div>
                                        <span style={{
                                            fontSize: '0.65rem', marginLeft: 0, marginTop: 2, display: 'inline-block',
                                            padding: '1px 6px', borderRadius: 8,
                                            background: isShared ? '#d1ecf1' : '#fff3cd',
                                            color: isShared ? '#0c5460' : '#856404',
                                            fontWeight: 600, textTransform: 'uppercase',
                                        }}>
                                            {row.zoneCategory}
                                        </span>
                                    </td>
                                    {visibleMachineTypes.map(mt => {
                                        const cell = cellMap[`${row.zoneNom}|${mt}`];
                                        if (!cell || !cell.machinePresent) {
                                            return (
                                                <td key={mt} style={{
                                                    padding: '8px 10px', textAlign: 'center',
                                                    background: '#fafafa', color: '#ccc',
                                                }}>—</td>
                                            );
                                        }
                                        const color = cellStyle(cell.loadPct);
                                        const hasPending = cell.plannedMinutes > 0;
                                        const { isWorst, target } = computeCellImbalance(cell, mtStats);
                                        const isHovered = hoveredCell && hoveredCell.zoneNom === row.zoneNom && hoveredCell.machineType === mt;

                                        // Compute "what if" preview
                                        let whatIf = null;
                                        if (isHovered && liveCharge) {
                                            const localTop = findTopPendingSequenceForCell(row.zoneNom, mt, liveCharge);
                                            if (localTop) {
                                                const mins = localTop.totalRemainingMinutes || 0;
                                                const newLoad = cell.capacityMinutes > 0
                                                    ? ((cell.plannedMinutes - mins) / cell.capacityMinutes) * 100
                                                    : cell.loadPct;
                                                whatIf = {
                                                    type: 'out',
                                                    seq: localTop.sequence,
                                                    mins,
                                                    newLoad: Math.max(0, newLoad),
                                                };
                                            } else {
                                                const inbound = findTopPendingSequenceToMoveIn(row.zoneNom, mt, liveCharge);
                                                if (inbound) {
                                                    const mins = inbound.seq.totalRemainingMinutes || 0;
                                                    const newLoad = cell.capacityMinutes > 0
                                                        ? ((cell.plannedMinutes + mins) / cell.capacityMinutes) * 100
                                                        : cell.loadPct;
                                                    whatIf = {
                                                        type: 'in',
                                                        seq: inbound.seq.sequence,
                                                        from: inbound.sourceZone,
                                                        mins,
                                                        newLoad,
                                                    };
                                                }
                                            }
                                        }

                                        return (
                                            <td
                                                key={mt}
                                                style={{
                                                    padding: '8px 10px', textAlign: 'center',
                                                    background: color.bg, color: color.text,
                                                    fontWeight: 600, position: 'relative',
                                                    border: isWorst
                                                        ? '2px solid #f44336'
                                                        : `1px solid ${color.border}`,
                                                    animation: isWorst ? 'pulse-red 2s infinite' : 'none',
                                                    cursor: 'pointer',
                                                }}
                                                onMouseEnter={() => setHoveredCell({ zoneNom: row.zoneNom, machineType: mt })}
                                                onMouseLeave={() => setHoveredCell(null)}
                                            >
                                                <div>{cell.loadPct.toFixed(0)}%</div>
                                                <div style={{
                                                    fontSize: '0.7rem', fontWeight: 400,
                                                    opacity: 0.85,
                                                }}>
                                                    {cell.plannedMinutes.toFixed(0)} min
                                                </div>
                                                <div style={{
                                                    fontSize: '0.65rem', fontWeight: 400,
                                                    opacity: 0.7, marginTop: 2,
                                                }}>
                                                    cible {fmtPct(target)}
                                                </div>
                                                {cell.activeMachines > 0 && (
                                                    <span style={{
                                                        position: 'absolute', top: 2, right: 2,
                                                        fontSize: '0.6rem',
                                                        background: 'rgba(0,0,0,0.15)',
                                                        color: '#fff', padding: '1px 4px',
                                                        borderRadius: 4, fontWeight: 700,
                                                    }}>
                                                        {cell.activeMachines}
                                                    </span>
                                                )}
                                                {hasPending && (
                                                    <span style={{
                                                        position: 'absolute', bottom: 2, left: 2,
                                                        fontSize: '0.6rem', background: '#EE3124',
                                                        color: '#fff', padding: '1px 4px',
                                                        borderRadius: 4, fontWeight: 700,
                                                    }}>
                                                        P
                                                    </span>
                                                )}

                                                {/* What-if tooltip */}
                                                {isHovered && whatIf && (
                                                    <div style={{
                                                        position: 'absolute', bottom: '105%', left: '50%',
                                                        transform: 'translateX(-50%)',
                                                        background: '#222', color: '#fff',
                                                        padding: '6px 10px', borderRadius: 6,
                                                        fontSize: '0.72rem', whiteSpace: 'nowrap',
                                                        zIndex: 10, boxShadow: '0 4px 12px rgba(0,0,0,0.2)',
                                                        pointerEvents: 'none',
                                                    }}>
                                                        {whatIf.type === 'out' ? (
                                                            <div>
                                                                <div style={{ fontWeight: 700, marginBottom: 2 }}>
                                                                    <FontAwesomeIcon icon={faArrowRight} /> Déplacer {whatIf.seq}
                                                                </div>
                                                                <div>→ charge passerait à {fmtPct(whatIf.newLoad)}</div>
                                                                <div style={{ opacity: 0.7, fontSize: '0.65rem' }}>
                                                                    ({whatIf.mins.toFixed(0)} min)
                                                                </div>
                                                            </div>
                                                        ) : (
                                                            <div>
                                                                <div style={{ fontWeight: 700, marginBottom: 2 }}>
                                                                    <FontAwesomeIcon icon={faArrowLeft} /> Recevoir {whatIf.seq}
                                                                </div>
                                                                <div>depuis {whatIf.from}</div>
                                                                <div>→ charge passerait à {fmtPct(whatIf.newLoad)}</div>
                                                                <div style={{ opacity: 0.7, fontSize: '0.65rem' }}>
                                                                    (+{whatIf.mins.toFixed(0)} min)
                                                                </div>
                                                            </div>
                                                        )}
                                                        <div style={{
                                                            position: 'absolute', top: '100%', left: '50%',
                                                            transform: 'translateX(-50%)',
                                                            borderWidth: 5, borderStyle: 'solid',
                                                            borderColor: '#222 transparent transparent transparent',
                                                        }} />
                                                    </div>
                                                )}
                                            </td>
                                        );
                                    })}
                                    <td style={{
                                        padding: '8px 10px', textAlign: 'center',
                                        fontWeight: 600,
                                    }}>
                                        <div>{row.loadPct.toFixed(0)}%</div>
                                        <div style={{ fontSize: '0.7rem', color: '#888' }}>
                                            {row.sequencesCount} seq
                                        </div>
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
            {selectedCell && (
                <ZoneSequencesModal
                    zoneNom={selectedCell.zoneNom}
                    liveCharge={liveCharge}
                    onClose={() => setSelectedCell(null)}
                />
            )}
        </div>
    );
}

function ZoneSequencesModal({ zoneNom, liveCharge, onClose }) {
    React.useEffect(() => {
        const onKey = (e) => { if (e.key === 'Escape') onClose(); };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [onClose]);

    // Backend groups sequences by their "home" zone (effectiveZone) — and
    // splits them into lockedSequences / pendingSequences on each ZoneChargeDto
    // (LiveChargeDto.java:85). A sequence's individual series can route to a
    // SHARED zone (DIE / Gerber / LASER-DXF spillover). Walk every zone's
    // BOTH lists and pick sequences with at least one serie whose targetZoneNom
    // matches the clicked zone. Per-sequence contribution = sum of remaining
    // minutes for just those routed series (not totalRemainingMinutes).
    const allSequences = (liveCharge?.zones || []).flatMap(z => {
        const merged = [
            ...(z.lockedSequences || []),
            ...(z.pendingSequences || []),
            ...(z.sequences || []), // future-proofing if backend ever exposes a unified list
        ];
        return merged.map(seq => ({ ...seq, hostZoneNom: z.zoneNom }));
    });
    // Also include unassigned (no home zone) — they have series with targetZoneNom too.
    (liveCharge?.unassigned || []).forEach(seq => {
        allSequences.push({ ...seq, hostZoneNom: null });
    });

    const sequences = allSequences
        .map(seq => {
            const seriesHere = (seq.series || []).filter(s => {
                if (!s) return false;
                // Prefer routed zone; for very old payloads without
                // targetZoneNom, fall back to the sequence's host zone so the
                // home-zone case still works.
                const routed = s.targetZoneNom || seq.hostZoneNom;
                return routed === zoneNom;
            });
            if (seriesHere.length === 0) return null;
            const minutesHere = seriesHere.reduce(
                (acc, s) => acc + (s.remainingMinutes || 0), 0);
            const byMt = {};
            seriesHere.forEach(s => {
                if (!s.machine) return;
                byMt[s.machine] = (byMt[s.machine] || 0) + (s.remainingMinutes || 0);
            });
            return { ...seq, seriesHere, minutesHere, byMt };
        })
        .filter(Boolean)
        .sort((a, b) => {
            const ad = a.dueDate, bd = b.dueDate;
            if (ad && bd) {
                if (ad < bd) return -1;
                if (ad > bd) return 1;
            } else if (ad && !bd) return -1;
            else if (!ad && bd) return 1;
            return (b.minutesHere || 0) - (a.minutesHere || 0);
        });

    const lockedCount = sequences.filter(s => s.locked).length;
    const waitingCount = sequences.length - lockedCount;
    const totalMinutesHere = sequences.reduce((a, s) => a + s.minutesHere, 0);

    return (
        <div
            onClick={onClose}
            style={{
                position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)',
                zIndex: 1000, display: 'flex', alignItems: 'center',
                justifyContent: 'center', padding: 20,
            }}
        >
            <div
                onClick={(e) => e.stopPropagation()}
                style={{
                    background: '#fff', borderRadius: 10, width: '100%',
                    maxWidth: 1000, maxHeight: '85vh', display: 'flex',
                    flexDirection: 'column', boxShadow: '0 10px 30px rgba(0,0,0,0.2)',
                    overflow: 'hidden',
                }}
            >
                <div style={{
                    display: 'flex', alignItems: 'center', gap: 12,
                    padding: '14px 18px', borderBottom: '1px solid #eee',
                    background: '#f8f9fa',
                }}>
                    <strong style={{ fontSize: '1.05rem' }}>Séquences — zone {zoneNom}</strong>
                    <span style={{ color: '#6c757d', fontSize: '0.85rem' }}>
                        {sequences.length} séquence(s) · {Math.round(totalMinutesHere)} min routées ici
                    </span>
                    {lockedCount > 0 && (
                        <span style={{
                            fontSize: '0.75rem', padding: '2px 8px',
                            background: '#f8d7da', color: '#721c24',
                            borderRadius: 10, fontWeight: 600,
                        }}>
                            {lockedCount} verrouillée(s)
                        </span>
                    )}
                    {waitingCount > 0 && (
                        <span style={{
                            fontSize: '0.75rem', padding: '2px 8px',
                            background: '#d4edda', color: '#155724',
                            borderRadius: 10, fontWeight: 600,
                        }}>
                            {waitingCount} libre(s)
                        </span>
                    )}
                    <button
                        onClick={onClose}
                        style={{
                            marginLeft: 'auto', background: 'transparent',
                            border: 'none', cursor: 'pointer', fontSize: '1.4rem',
                            color: '#6c757d', lineHeight: 1,
                        }}
                        title="Fermer (Échap)"
                    >×</button>
                </div>
                <div style={{ overflowY: 'auto', padding: '4px 0' }}>
                    {sequences.length === 0 && (
                        <div style={{ padding: '18px 22px', color: '#6c757d' }}>
                            Aucune séquence sur cette zone.
                        </div>
                    )}
                    {sequences.map(seq => {
                        const lockColor = seq.locked
                            ? (seq.lockReason === 'EXPLICIT_ACCEPTED' ? '#c62828' : '#d84315')
                            : '#2e7d32';
                        const lockBg = seq.locked
                            ? (seq.lockReason === 'EXPLICIT_ACCEPTED' ? '#ffebee' : '#fff3e0')
                            : '#e8f5e9';
                        const lockLabel = seq.locked
                            ? (seq.lockReason === 'EXPLICIT_ACCEPTED' ? 'ACCEPTÉE' : 'VERROUILLÉE')
                            : 'LIBRE';
                        const lockTip = seq.locked
                            ? (seq.lockReason === 'IMPLICIT_TABLE_STRICT'
                                ? `Série ${seq.lockingSerieId || '?'} démarrée sur ${seq.lockingTableNom || '?'} — toute la séquence est fixée sur ${zoneNom}.`
                                : `Acceptée par le chef de zone — placement fixé sur ${zoneNom}.`)
                            : 'Pas de série démarrée : la séquence peut être déplacée librement.';
                        return (
                            <div key={seq.sequence} style={{
                                padding: '10px 18px', borderBottom: '1px solid #f4f4f4',
                            }}>
                                <div style={{
                                    display: 'flex', gap: 10, alignItems: 'baseline',
                                    flexWrap: 'wrap',
                                }}>
                                    <strong style={{ fontSize: '0.95rem' }}>{seq.sequence}</strong>
                                    <span
                                        style={{
                                            fontSize: '0.7rem', padding: '2px 8px',
                                            background: lockBg, color: lockColor,
                                            borderRadius: 10, fontWeight: 700,
                                            letterSpacing: 0.3,
                                        }}
                                        title={lockTip}
                                    >
                                        {lockLabel}
                                    </span>
                                    {seq.pinnedByChef && (
                                        <span style={{
                                            fontSize: '0.7rem', padding: '2px 6px',
                                            background: '#e3f2fd', color: '#1565c0',
                                            borderRadius: 10, fontWeight: 600,
                                        }} title="Épinglée par le chef de zone">
                                            ÉPINGLÉE
                                        </span>
                                    )}
                                    {seq.hostZoneNom && seq.hostZoneNom !== zoneNom && (
                                        <span style={{
                                            fontSize: '0.7rem', padding: '2px 6px',
                                            background: '#fff3cd', color: '#856404',
                                            borderRadius: 10, fontWeight: 600,
                                        }} title={`Séquence rattachée à ${seq.hostZoneNom}; les minutes ci-dessous routent vers ${zoneNom} (SHARED).`}>
                                            via {seq.hostZoneNom}
                                        </span>
                                    )}
                                    {!seq.hostZoneNom && (
                                        <span style={{
                                            fontSize: '0.7rem', padding: '2px 6px',
                                            background: '#f8d7da', color: '#721c24',
                                            borderRadius: 10, fontWeight: 600,
                                        }} title="Séquence non assignée — aucune zone STRICT ne peut l'héberger.">
                                            NON ASSIGNÉE
                                        </span>
                                    )}
                                    {seq.dueDate && (
                                        <span style={{ fontSize: '0.8rem', color: '#495057' }}>
                                            Due {seq.dueDate}
                                        </span>
                                    )}
                                    <span style={{
                                        fontSize: '0.8rem', color: '#0d6efd',
                                        marginLeft: 'auto', fontWeight: 700,
                                    }} title="Minutes que cette séquence apporte à la zone cliquée">
                                        {Math.round(seq.minutesHere)} min ici
                                        {(seq.totalRemainingMinutes || 0) > seq.minutesHere + 0.5 && (
                                            <span style={{
                                                color: '#6c757d', fontWeight: 400, marginLeft: 6,
                                            }}>
                                                / {Math.round(seq.totalRemainingMinutes || 0)} total
                                            </span>
                                        )}
                                    </span>
                                </div>
                                {Object.keys(seq.byMt).length > 0 && (
                                    <div style={{ marginTop: 4, display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                                        {Object.entries(seq.byMt)
                                            .sort((a, b) => b[1] - a[1])
                                            .map(([mt, mins]) => (
                                                <span key={mt} style={{
                                                    fontSize: '0.7rem', padding: '1px 8px',
                                                    background: '#f1f3f5', color: '#495057',
                                                    borderRadius: 8, fontWeight: 500,
                                                }}>
                                                    {mt}: {Math.round(mins)} min
                                                </span>
                                            ))}
                                    </div>
                                )}
                                {seq.locked && seq.lockingSerieId && (
                                    <div style={{
                                        marginTop: 4, fontSize: '0.75rem', color: '#6c757d',
                                    }}>
                                        Verrou : série <strong>{seq.lockingSerieId}</strong>
                                        {seq.lockingTableNom && <> sur table <strong>{seq.lockingTableNom}</strong></>}
                                        {seq.lockingStatusCoupe && <> ({seq.lockingStatusCoupe})</>}
                                    </div>
                                )}
                                {seq.seriesHere.length > 0 && (
                                    <table style={{ width: '100%', marginTop: 8, fontSize: '0.78rem' }}>
                                        <thead>
                                            <tr style={{ color: '#6c757d', textAlign: 'left' }}>
                                                <th style={{ padding: '2px 6px' }}>Série</th>
                                                <th style={{ padding: '2px 6px' }}>Type</th>
                                                <th style={{ padding: '2px 6px' }}>Statut coupe</th>
                                                <th style={{ padding: '2px 6px' }}>Table coupe</th>
                                                <th style={{ padding: '2px 6px', textAlign: 'right' }}>Restant ici</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {seq.seriesHere.map(s => (
                                                <tr key={s.serie}>
                                                    <td style={{ padding: '2px 6px' }}>{s.serie}</td>
                                                    <td style={{ padding: '2px 6px' }}>{s.machine || '—'}</td>
                                                    <td style={{ padding: '2px 6px' }}>{s.statusCoupe || '—'}</td>
                                                    <td style={{ padding: '2px 6px' }}>{s.tableCoupe || '—'}</td>
                                                    <td style={{ padding: '2px 6px', textAlign: 'right' }}>
                                                        {s.remainingMinutes != null ? Math.round(s.remainingMinutes) : '—'} min
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>
        </div>
    );
}
