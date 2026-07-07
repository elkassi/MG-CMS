import React, { useState, useRef, useEffect } from 'react';
import moment from 'moment';

// Helper to generate color for a sequence
function getSequenceColor(sequence) {
    // Simple hash to color
    let hash = 0;
    for (let i = 0; i < sequence.length; i++) {
        hash = sequence.charCodeAt(i) + ((hash << 5) - hash);
    }
    const c = (hash & 0x00FFFFFF)
        .toString(16)
        .toUpperCase();
    return '#' + '00000'.substring(0, 6 - c.length) + c;
}
 
const CalendarCoupe = ({ series, machines, onSerieClick, machineIntervals = [], scrollToNowTrigger }) => {
    const machineNames = machines.map(m => m.nom);
    const [viewWindowMinutes, setViewWindowMinutes] = useState(120); // default 2 hours
    const blocks = series
        .filter(s => (s.dateDebutCoupe || s.EDdateDebutCoupe) && (s.dateFinCoupe || s.EDdateFinCoupe) && (s.machineAssigned || s.tableCoupe || s.tableMatelassage))
        .map(s => {
            const start = moment(s.dateDebutCoupe || s.EDdateDebutCoupe);
            const end = moment(s.dateFinCoupe || s.EDdateFinCoupe);
            return {
                serie: s,
                machine: s.machineAssigned || s.tableCoupe || s.tableMatelassage,
                start,
                end,
                sequence: s.sequence 
            };
        });
    // Find min and max date
    let minDate = null, maxDate = null;
    if (blocks.length) {
        minDate = moment.min(blocks.map(b => b.start));
        maxDate = moment.max(blocks.map(b => b.end));
    } else {
        minDate = moment().startOf('day');
        maxDate = moment(minDate).add(8, 'hours');
    }
    // If minDate and maxDate are on different days, show all days in between
    // Remove unused totalMinutes
    // For vertical position: 0% = minDate, 100% = maxDate
    // We'll show only a 2-hour window at a time, with scroll
    // Remove old timeLabels and t for view window
    // For time labels, show every 15 minutes, bigger font and spacing, for the full range
    const totalMinutes = maxDate.diff(minDate, 'minutes');
    const timeLabels = [];
    let t = minDate.clone();
    while (t.isBefore(maxDate)) {
        timeLabels.push(
            <div key={t.format()} style={{ height: (600 * 15) / viewWindowMinutes, fontSize: 16, color: '#555', textAlign: 'right', paddingRight: 4, fontWeight: 500 }}>
                {t.format('DD/MM HH:mm')}
            </div>
        );
        t.add(15, 'minutes');
    }
    // For vertical position: 0% = minDate, 100% = maxDate
    function getTop(start) {
        return ((start.diff(minDate, 'minutes')) / totalMinutes) * 100;
    }
    function getHeight(start, end) {
        return ((end.diff(start, 'minutes')) / totalMinutes) * 100;
    }
    // Define showNowLine before useEffect
    const now = moment();
    const showNowLine = now.isBetween(minDate, maxDate, null, '[)');
    // Ref for scrolling to now
    const scrollRef = React.useRef();
    // Ref to track last scrollToNowTrigger
    const lastScrollTrigger = useRef();
    useEffect(() => {
        if (scrollToNowTrigger !== undefined && scrollToNowTrigger !== lastScrollTrigger.current) {
            lastScrollTrigger.current = scrollToNowTrigger;
            if (scrollRef.current && showNowLine) {
                // Scroll so that the red line (now) is at the top, showing the next 2 hours
                const container = scrollRef.current;
                const nowMinutes = now.diff(minDate, 'minutes');
                const totalHeight = 600 * (totalMinutes / viewWindowMinutes);
                const scrollTop = (nowMinutes / totalMinutes) * totalHeight;
                container.scrollTop = Math.max(0, scrollTop);
            }
        }
    }, [scrollToNowTrigger, minDate, maxDate, showNowLine, now, totalMinutes, viewWindowMinutes]);
    // For each machine, get blocks sorted by start
    const blocksByMachine = {};
    machineNames.forEach(m => {
        blocksByMachine[m] = blocks.filter(b => b.machine === m).sort((a, b) => a.start - b.start);
    });
    // Prepare interval blocks for each machine
    const intervalBlocksByMachine = {};
    machineNames.forEach(m => {
        intervalBlocksByMachine[m] = (machineIntervals || [])
            .filter(iv => !iv.machine || iv.machine === m)
            .map(iv => {
                const start = moment(iv.start);
                const end = moment(iv.end);
                return {
                    start,
                    end,
                    type: iv.type,
                    machine: iv.machine || null
                };
            });
    });
    // Find all day changes between minDate and maxDate
    let dayLines = [];
    let d = minDate.clone().add(1, 'day').startOf('day');
    while (d.isBefore(maxDate)) {
        dayLines.push(d.clone());
        d.add(1, 'day');
    }
    const [modalSerie, setModalSerie] = useState(null);
    return (
        <>
        <div style={{ marginBottom: 12, display: 'flex', alignItems: 'center', gap: 12 }}>
            <span style={{ fontWeight: 600, color: '#2a3a5a' }}>Fenêtre visible:</span>
            {[30, 60, 120, 240, 480].map(mins => (
                <button
                    key={mins}
                    className={viewWindowMinutes === mins ? 'btn btn-primary btn-sm' : 'btn btn-outline-primary btn-sm'}
                    style={{ minWidth: 48, fontWeight: 600, borderRadius: 6 }}
                    onClick={() => setViewWindowMinutes(mins)}
                >
                    {mins < 60 ? `${mins} min` : `${mins / 60}h`}
                </button>
            ))}
        </div>
        <div style={{ display: 'flex', border: '1px solid #ccc', borderRadius: 8, overflow: 'hidden', minHeight: 600, flexDirection: 'column', background: '#f7f7f7' }}>
            {/* Sticky header for machine names */}
            <div style={{ display: 'flex', position: 'sticky', top: 0, zIndex: 20, background: '#f0f4fa', borderBottom: '2px solid #bbb' }}>
                <div style={{ width: 90, background: '#f7f7f7', borderRight: '1px solid #ddd', padding: 8, fontWeight: 600, textAlign: 'center' }}>Heure</div>
                {machineNames.map(m => {
                    const machineObj = machines.find(mac => mac.nom === m);
                    const machineType = machineObj ? machineObj.machineType?.name : '';
                    const zoneName = machineObj && machineObj.zone ? machineObj.zone.nom : '';
                    return (
                        <div key={m} style={{ flex: 1, minWidth: 120, fontWeight: 600, textAlign: 'center', padding: 6, borderRight: '1px solid #eee' }}>
                            {m}{machineType ? ` : ${machineType}` : ''}{zoneName ? ` (${zoneName})` : ''}
                        </div>
                    );
                })}
            </div>
            <div ref={scrollRef} style={{ display: 'flex', flex: 1, minHeight: 600, maxHeight: 600, overflowY: 'auto', background: '#fff' }}>
                {/* Time labels */}
                <div style={{ width: 90, background: '#f7f7f7', borderRight: '1px solid #ddd', padding: 8 }}>
                    {timeLabels}
                </div>
                {machineNames.map(m => {
                    return (
                        <div key={m} style={{ flex: 1, borderRight: '1px solid #eee', position: 'relative', minWidth: 120, height: 600 * (totalMinutes / viewWindowMinutes), background: '#fff' }}>
                            {/* Day change lines */}
                            {dayLines.map((line, idx) => (
                                <div key={idx} style={{
                                    position: 'absolute',
                                    left: 0,
                                    right: 0,
                                    top: getTop(line) + '%',
                                    height: 2,
                                    background: '#ffb',
                                    zIndex: 1
                                }} title={line.format('DD/MM/YYYY')}></div>
                            ))}
                            {/* Red line for current moment */}
                            {showNowLine && (
                                <div style={{
                                    position: 'absolute',
                                    left: 0,
                                    right: 0,
                                    top: getTop(now) + '%',
                                    height: 2,
                                    background: 'red',
                                    zIndex: 10
                                }} title="Moment actuel"></div>
                            )}
                            {/* Interval overlays */}
                            {intervalBlocksByMachine[m].map((iv, idx) => (
                                <div key={idx} style={{
                                    position: 'absolute',
                                    left: 2,
                                    right: 2,
                                    top: getTop(iv.start) + '%',
                                    height: getHeight(iv.start, iv.end) + '%',
                                    background: iv.type === 'strict' ? 'rgba(255,0,0,0.25)' : 'rgba(120,120,120,0.18)',
                                    border: iv.type === 'strict' ? '2px solid #c00' : '1px dashed #888',
                                    borderRadius: 6,
                                    zIndex: 5,
                                    pointerEvents: 'none',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    fontSize: 13,
                                    color: iv.type === 'strict' ? '#c00' : '#444',
                                    fontWeight: 600
                                }} title={iv.type === 'strict' ? 'Arrêt strict' : 'Pause'}>
                                    {iv.type === 'strict' ? 'Arrêt strict' : 'Pause'}
                                </div>
                            ))}
                            {/* All blocks */}
                            {blocksByMachine[m].map((b, i) => {
                                // Get dueDate and dueShift from sequence object if available
                                const sequenceObj = b.serie.sequenceObj || b.serie.sequence || {};
                                const dueDate = sequenceObj.dueDate || b.serie.dueDate;
                                const dueShift = sequenceObj.dueShift || b.serie.dueShift;
                                return (
                                    <div key={i} style={{
                                        position: 'absolute',
                                        left: 8,
                                        right: 8,
                                        top: getTop(b.start) + '%',
                                        height: getHeight(b.start, b.end) + '%',
                                        background: getSequenceColor(b.sequence),
                                        color: '#fff',
                                        borderRadius: 6,
                                        boxShadow: '0 2px 8px #0002',
                                        padding: 4,
                                        fontSize: 14,
                                        overflow: 'hidden',
                                        border: '1px solid #fff',
                                        zIndex: 2,
                                        cursor: 'pointer'
                                    }} title={`Série: ${b.serie.serie}\nSéquence: ${b.sequence}\n${b.start.format('DD/MM HH:mm')} - ${b.end.format('DD/MM HH:mm')}`}
                                    onClick={() => onSerieClick ? onSerieClick(b.serie) : setModalSerie(b.serie)}
                                    >
                                        {/* Status indicators + dueDate/dueShift */}
                                        <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 2 }}>
                                            <span title={`Matelassage: ${b.serie.statusMatelassage || ''}`} style={{
                                                display: 'inline-block',
                                                width: 12,
                                                height: 12,
                                                borderRadius: '50%',
                                                background: b.serie.statusMatelassage === 'Waiting' ? '#ffafaf' : b.serie.statusMatelassage === 'In progress' ? '#f6ff6b' : b.serie.statusMatelassage === 'Complete' ? '#7bff6b' : b.serie.statusMatelassage === 'Incomplete' ? '#ffc46b' : '#ccc',
                                                border: '1px solid #888',
                                            }} />
                                            <span title={`Coupe: ${b.serie.statusCoupe || ''}`} style={{
                                                display: 'inline-block',
                                                width: 12,
                                                height: 12,
                                                borderRadius: '50%',
                                                background: b.serie.statusCoupe === 'Waiting' ? '#ffafaf' : b.serie.statusCoupe === 'In progress' ? '#f6ff6b' : b.serie.statusCoupe === 'Complete' ? '#7bff6b' : b.serie.statusCoupe === 'Incomplete' ? '#ffc46b' : '#ccc',
                                                border: '1px solid #888',
                                            }} />
                                            {/* Due date and shift from sequence */}
                                            {dueDate && (
                                                <span style={{ fontSize: 12, color: '#fff', background: '#2a3a5a', borderRadius: 4, padding: '0 6px', marginLeft: 4 }} title="Due date">{moment(dueDate).format('DD/MM')}</span>
                                            )}
                                            {dueShift && (
                                                <span style={{ fontSize: 12, color: '#fff', background: '#007bff', borderRadius: 4, padding: '0 6px', marginLeft: 2 }} title="Shift">{dueShift}</span>
                                            )}
                                        </div>
                                        <div><b>{b.serie.serie}</b></div>
                                        <div style={{ fontSize: 12 }}>{b.start.format('DD/MM HH:mm')} - {b.end.format('DD/MM HH:mm')}</div>
                                        <div style={{ fontSize: 11, opacity: 0.8 }}>Seq: {b.sequence}</div>
                                        <div style={{ fontSize: 11, opacity: 0.8 }}>Placement: {b.serie.placement}</div>
                                        <div style={{ fontSize: 11, opacity: 0.8 }}>Part#: {b.serie.partNumberMaterial}</div>
                                        <div style={{ fontSize: 11, opacity: 0.8 }}>Desc: {b.serie.description}</div>
                                        <div style={{ fontSize: 11, opacity: 0.8 }}>Config: {b.serie.config}</div>
                                    </div>
                                );
                            })}
                        </div>
                    );
                })}
            </div>
        </div>
        {/* Modal for serie details */}
        {modalSerie && (
            <div style={{
                position: 'fixed', left: 0, top: 0, width: '100vw', height: '100vh', background: '#0008', zIndex: 1000,
                display: 'flex', alignItems: 'center', justifyContent: 'center'
            }} onClick={() => setModalSerie(null)}>
                <div style={{ background: '#fff', borderRadius: 10, padding: 32, minWidth: 400, maxWidth: 600, boxShadow: '0 4px 32px #0004', position: 'relative', maxHeight: '90%', overflowY: 'auto' }} onClick={e => e.stopPropagation()}>
                    <button onClick={() => setModalSerie(null)} style={{ position: 'absolute', top: 10, right: 10, background: 'none', border: 'none', fontSize: 22, cursor: 'pointer' }}>&times;</button>
                    <h3>Détails de la série</h3>
                    <table style={{ width: '100%', fontSize: 15 }}>
                        <tbody>
                            {Object.entries(modalSerie).map(([k, v]) => (
                                <tr key={k}>
                                    <td style={{ fontWeight: 600, padding: 4 }}>{k}</td>
                                    <td style={{ padding: 4 }}>{v !== null && v !== undefined ? v.toString() : ''}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        )}
        </>
    );
};

export default CalendarCoupe;
