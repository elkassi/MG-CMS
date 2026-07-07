import React from 'react';
import moment from 'moment';

/**
 * SequenceSummaryTable - Side table showing sequence summaries
 * 
 * Features:
 * - Display min start date, max end date for each sequence
 * - Show duration in hours
 * - Color-coded status indicators
 * - Sortable columns
 * - Double-click to view sequence details
 */
const SequenceSummaryTable = ({ summaries, onSequenceDoubleClick }) => {
    if (!summaries || summaries.length === 0) {
        return (
            <div style={{
                background: '#fff',
                borderRadius: 8,
                padding: 16,
                textAlign: 'center',
                color: '#888'
            }}>
                Aucun résumé de séquence disponible
            </div>
        );
    }

    const formatDuration = (minutes) => {
        if (!minutes && minutes !== 0) return '-';
        const hours = (minutes / 60).toFixed(2);
        return `${hours}h`;
    };

    const formatDateTime = (dateStr) => {
        if (!dateStr) return '-';
        return moment(dateStr).format('DD/MM HH:mm');
    };

    // Calculate total duration
    const totalMinutes = summaries.reduce((sum, s) => sum + (s.durationMinutes || 0), 0);

    return (
        <div style={{
            background: '#fff',
            borderRadius: 8,
            padding: 16,
            height: '100%'
        }}>
            <h5 style={{ 
                margin: '0 0 16px 0', 
                fontWeight: 600,
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
            }}>
                <span>Résumé des Séquences</span>
                <span style={{ 
                    fontSize: 13, 
                    fontWeight: 400,
                    color: '#666'
                }}>
                    Total: {formatDuration(totalMinutes)}
                </span>
            </h5>

            <table 
                className="table table-bordered table-sm" 
                style={{ 
                    fontSize: 12,
                    marginBottom: 0
                }}
            >
                <thead style={{ background: '#f0f4fa' }}>
                    <tr>
                        <th style={{ 
                            fontWeight: 600, 
                            padding: '8px 6px',
                            whiteSpace: 'nowrap'
                        }}>
                            Séquence
                        </th>
                        <th style={{ 
                            fontWeight: 600, 
                            padding: '8px 6px',
                            whiteSpace: 'nowrap'
                        }}>
                            Début Min
                        </th>
                        <th style={{ 
                            fontWeight: 600, 
                            padding: '8px 6px',
                            whiteSpace: 'nowrap'
                        }}>
                            Fin Max
                        </th>
                        <th style={{ 
                            fontWeight: 600, 
                            padding: '8px 6px',
                            whiteSpace: 'nowrap'
                        }}>
                            Durée (h)
                        </th>
                        <th style={{ 
                            fontWeight: 600, 
                            padding: '8px 6px',
                            whiteSpace: 'nowrap',
                            background: '#d4edda'
                        }}>
                            ⏱️ Min/Box
                        </th>
                        <th style={{ 
                            fontWeight: 600, 
                            padding: '8px 6px',
                            whiteSpace: 'nowrap'
                        }}>
                            Boxes
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {summaries.map((summary, index) => {
                        // Calculate background color based on duration per box
                        let bgColor = '#fff';
                        const durationPerBox = summary.durationPerBox || 0;
                        if (durationPerBox > 30) bgColor = '#fff3cd'; // Warning: slow
                        if (durationPerBox > 60) bgColor = '#f8d7da'; // Danger: very slow

                        return (
                            <tr 
                                key={summary.sequenceId || index} 
                                style={{ background: bgColor, cursor: 'pointer' }}
                                onDoubleClick={() => {
                                    if (onSequenceDoubleClick && summary.sequenceId) {
                                        onSequenceDoubleClick(summary.sequenceId);
                                    }
                                }}
                                title="Double-clic pour voir les détails"
                            >
                                <td style={{ 
                                    fontWeight: 600,
                                    padding: '6px',
                                    whiteSpace: 'nowrap'
                                }}>
                                    {summary.sequenceId || '-'}
                                </td>
                                <td style={{ 
                                    padding: '6px',
                                    whiteSpace: 'nowrap',
                                    color: '#4a5568'
                                }}>
                                    {formatDateTime(summary.minStartDate)}
                                </td>
                                <td style={{ 
                                    padding: '6px',
                                    whiteSpace: 'nowrap',
                                    color: '#4a5568'
                                }}>
                                    {formatDateTime(summary.maxEndDate)}
                                </td>
                                <td style={{ 
                                    padding: '6px',
                                    textAlign: 'right',
                                    fontWeight: 500
                                }}>
                                    {formatDuration(summary.durationMinutes)}
                                </td>
                                <td style={{ 
                                    padding: '6px',
                                    textAlign: 'right',
                                    fontWeight: 600,
                                    background: durationPerBox > 0 ? '#e8f5e9' : undefined,
                                    color: durationPerBox > 60 ? '#dc3545' : durationPerBox > 30 ? '#856404' : '#28a745'
                                }}>
                                    {summary.durationPerBox ? `${summary.durationPerBox.toFixed(1)}m` : '-'}
                                </td>
                                <td style={{ 
                                    padding: '6px',
                                    textAlign: 'center',
                                    fontWeight: 500
                                }}>
                                    {summary.boxCount || summary.seriesCount || '-'}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>

            {/* Summary footer */}
            <div style={{
                marginTop: 12,
                padding: '10px 12px',
                background: '#e3e9f7',
                borderRadius: 6,
                fontSize: 12
            }}>
                <div style={{ 
                    display: 'flex', 
                    justifyContent: 'space-between',
                    marginBottom: 4
                }}>
                    <span style={{ fontWeight: 500 }}>Séquences:</span>
                    <span>{summaries.length}</span>
                </div>
                <div style={{ 
                    display: 'flex', 
                    justifyContent: 'space-between',
                    marginBottom: 4
                }}>
                    <span style={{ fontWeight: 500 }}>Durée totale:</span>
                    <span style={{ fontWeight: 600 }}>{formatDuration(totalMinutes)}</span>
                </div>
                <div style={{ 
                    display: 'flex', 
                    justifyContent: 'space-between',
                    marginBottom: 4
                }}>
                    <span style={{ fontWeight: 500 }}>Total boxes:</span>
                    <span>
                        {summaries.reduce((sum, s) => sum + (s.boxCount || s.seriesCount || 0), 0)}
                    </span>
                </div>
                <div style={{ 
                    display: 'flex', 
                    justifyContent: 'space-between',
                    padding: '4px 0',
                    borderTop: '1px solid #c3d0e8',
                    marginTop: 4
                }}>
                    <span style={{ fontWeight: 600, color: '#155724' }}>⏱️ Max Min/Box:</span>
                    <span style={{ fontWeight: 700, color: '#155724' }}>
                        {summaries.length > 0 
                            ? `${Math.max(...summaries.map(s => s.durationPerBox || 0)).toFixed(1)}m`
                            : '-'}
                    </span>
                </div>
            </div>

            {/* Legend */}
            <div style={{
                marginTop: 12,
                fontSize: 11,
                color: '#666'
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                    <div style={{ 
                        width: 12, 
                        height: 12, 
                        background: '#fff3cd', 
                        borderRadius: 2,
                        border: '1px solid #ffc107'
                    }} />
                    <span>Min/Box &gt; 30m (attention)</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <div style={{ 
                        width: 12, 
                        height: 12, 
                        background: '#f8d7da', 
                        borderRadius: 2,
                        border: '1px solid #dc3545'
                    }} />
                    <span>Min/Box &gt; 60m (critique)</span>
                </div>
            </div>
        </div>
    );
};

export default SequenceSummaryTable;
