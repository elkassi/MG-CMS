import React, { Component } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faSpinner, faChevronDown, faChevronRight,
} from '@fortawesome/free-solid-svg-icons';

/**
 * Active-run suggestions panel rendered under the engine bar on ProcessDispatcher.
 * Derived directly from liveCharge (engine-driven).
 * The engine's bestAssignment is already reflected in
 * liveCharge.pendingSequences[].effectiveZone. No manual /suggestions call.
 */
export default class DispatchEngineRunsPanel extends Component {
    deriveActiveSuggestions(liveCharge) {
        const suggestions = [];
        if (!liveCharge || !liveCharge.zones) return suggestions;
        liveCharge.zones.forEach(zone => {
            (zone.pendingSequences || []).forEach(seq => {
                if (seq.dispatchedZone && seq.dispatchedZone !== seq.effectiveZone) {
                    suggestions.push({
                        sequence: seq.sequence,
                        previousZone: seq.dispatchedZone,
                        suggestedZone: seq.effectiveZone,
                        remainingMinutes: seq.totalRemainingMinutes || 0,
                        machineTypes: [...new Set((seq.series || []).map(s => s.machine).filter(Boolean))],
                        pinned: seq.pinnedByChef,
                    });
                }
            });
        });
        (liveCharge.unassigned || []).forEach(seq => {
            if (seq.dispatchedZone && seq.dispatchedZone !== seq.effectiveZone) {
                suggestions.push({
                    sequence: seq.sequence,
                    previousZone: seq.dispatchedZone,
                    suggestedZone: seq.effectiveZone,
                    remainingMinutes: seq.totalRemainingMinutes || 0,
                    machineTypes: [...new Set((seq.series || []).map(s => s.machine).filter(Boolean))],
                    pinned: seq.pinnedByChef,
                });
            }
        });
        return suggestions;
    }

    render() {
        const { currentRunId, engineState, liveCharge } = this.props;
        const activeSuggestions = this.deriveActiveSuggestions(liveCharge);
        const hasActive = currentRunId
                && engineState !== 'IDLE'
                && engineState !== 'STOPPED'
                && activeSuggestions.length > 0;

        return (
            <div style={{ marginBottom: 20 }}>
                {/* Active-run suggestions — engine-driven from liveCharge */}
                {hasActive && (
                    <div style={{
                        background: '#fff', borderRadius: 8, padding: '14px 18px',
                        boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
                        border: '1px solid #eee', marginBottom: 12,
                    }}>
                        <h5 style={{
                            margin: '0 0 10px', fontSize: '0.95rem', fontWeight: 600,
                            color: '#222',
                        }}>
                            Suggestions en cours — run #{currentRunId} ({activeSuggestions.length})
                        </h5>
                        <div style={{ maxHeight: 200, overflowY: 'auto' }}>
                            <table style={{ width: '100%', fontSize: '0.85rem', borderCollapse: 'collapse' }}>
                                <thead>
                                    <tr style={{ background: '#fafafa' }}>
                                        <th style={tdStyle()}>Séquence</th>
                                        <th style={tdStyle()}>Machines</th>
                                        <th style={tdStyle()}>Reste</th>
                                        <th style={tdStyle()}>Zone précédente</th>
                                        <th style={tdStyle()}>Zone suggérée</th>
                                        <th style={tdStyle()}>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {activeSuggestions.map(s => (
                                        <tr key={s.sequence}>
                                            <td style={tdStyle({ fontFamily: "'Courier New', monospace", fontWeight: 600 })}>
                                                {s.sequence}
                                                {s.pinned && <span title="Épinglée par le chef" style={{ marginLeft: 4 }}>📌</span>}
                                            </td>
                                            <td style={tdStyle()}>
                                                {s.machineTypes.length > 0 ? s.machineTypes.join(', ') : '—'}
                                            </td>
                                            <td style={tdStyle()}>
                                                {s.remainingMinutes.toFixed(0)} min
                                            </td>
                                            <td style={tdStyle({ color: '#888' })}>
                                                {s.previousZone || '—'}
                                            </td>
                                            <td style={tdStyle({ fontWeight: 600, color: '#28a745' })}>
                                                {s.suggestedZone}
                                            </td>
                                            <td style={tdStyle()}>
                                                <span style={{
                                                    fontSize: '0.72rem', padding: '2px 8px',
                                                    background: '#e8f5e9', color: '#1b5e20',
                                                    borderRadius: 4, fontWeight: 500,
                                                }}>
                                                    À publier
                                                </span>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}


            </div>
        );
    }
}

function tdStyle(extra) {
    return {
        padding: '6px 10px', textAlign: 'left', borderBottom: '1px solid #f0f0f0',
        ...extra,
    };
}


