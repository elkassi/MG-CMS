import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faPlay, faPause, faStop, faForward, faSpinner, faGaugeHigh,
} from '@fortawesome/free-solid-svg-icons';

const STATE_CONFIG = {
    IDLE: { label: 'IDLE', color: '#6c757d', bg: '#f5f5f5' },
    WARMING: { label: 'WARMING', color: '#fd7e14', bg: '#fff3cd' },
    IMPROVING: { label: 'IMPROVING', color: '#28a745', bg: '#d4edda' },
    PAUSED: { label: 'PAUSED', color: '#ffc107', bg: '#fff3cd' },
    STOPPED: { label: 'STOPPED', color: '#c62828', bg: '#ffebee' },
};

export default function DispatchEngineControl({
    canConfigure,
    engineState,
    engineMode,
    enginePhase,
    engineDuration,
    engineBusy,
    engineStarting,
    iteration,
    currentSpread,
    stdDev,
    median,
    lastImprovement,
    onModeChange,
    onDurationChange,
    onStart,
    onPause,
    onResume,
    onStop,
}) {
    const cfg = engineStarting
        ? { label: 'Démarrage...', color: '#fd7e14', bg: '#fff3cd' }
        : (STATE_CONFIG[engineState] || STATE_CONFIG.IDLE);

    const startEnabled = (engineState === 'IDLE' || engineState === 'STOPPED') && !engineBusy && !engineStarting;
    const pauseEnabled = engineState === 'IMPROVING' && !engineBusy;
    const resumeEnabled = engineState === 'PAUSED' && !engineBusy;
    const stopEnabled = (engineState === 'IMPROVING' || engineState === 'PAUSED') && !engineBusy;

    return (
        <div className="disp-engine-control" style={{ marginBottom: 20 }}>
            <div style={{
                background: '#f5f5f5', borderRadius: 8, padding: '15px 20px',
                boxShadow: '0 2px 8px rgba(0,0,0,0.08)', marginBottom: 12,
            }}>
                <div style={{
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    flexWrap: 'wrap', gap: 12,
                }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
                        <span style={{
                            display: 'inline-flex', alignItems: 'center', gap: 6,
                            padding: '6px 14px', borderRadius: 20, fontWeight: 600,
                            fontSize: '0.9rem', background: cfg.bg, color: cfg.color,
                            border: `1px solid ${cfg.color}`,
                        }}>
                            <FontAwesomeIcon icon={faGaugeHigh} />
                            {cfg.label}
                        </span>
                        <span style={{ fontSize: '0.85rem', color: '#555' }}>
                            Itération: <strong>{iteration}</strong>
                        </span>
                        <span style={{ fontSize: '0.85rem', color: '#555' }}>
                            Écart: <strong>{currentSpread != null ? currentSpread.toFixed(2) : '—'}%</strong>
                        </span>
                        <span style={{ fontSize: '0.85rem', color: '#555' }}>
                            Médiane: <strong>{median != null ? median.toFixed(2) : '—'}%</strong>
                        </span>
                        <span style={{ fontSize: '0.85rem', color: '#555' }}>
                            σ: <strong>{stdDev != null ? stdDev.toFixed(2) : '—'}%</strong>
                        </span>
                        <span style={{ fontSize: '0.85rem', color: '#555' }}>
                            Dernière amélioration: <strong>
                                {lastImprovement != null
                                    ? `-${Math.abs(lastImprovement).toFixed(2)}%`
                                    : '—'}
                            </strong>
                        </span>
                        {enginePhase && (
                            <span style={{
                                fontSize: '0.8rem', fontWeight: 600,
                                padding: '3px 10px', borderRadius: 12,
                                background: enginePhase === 'DISPATCH' ? '#e3f2fd' : '#f3e5f5',
                                color: enginePhase === 'DISPATCH' ? '#1565c0' : '#6a1b9a',
                                border: `1px solid ${enginePhase === 'DISPATCH' ? '#1565c0' : '#6a1b9a'}`,
                            }}>
                                {enginePhase === 'DISPATCH' ? '📦 Dispatch' : '📅 Ordonnancement'}
                            </span>
                        )}
                    </div>
                </div>

                {canConfigure && (
                    <div style={{
                        marginTop: 14, display: 'flex', alignItems: 'flex-end',
                        gap: 14, flexWrap: 'wrap',
                    }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                            <span style={{ fontSize: '0.78rem', color: '#555', fontWeight: 500 }}>Mode:</span>
                            <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>Alterné</span>
                        </div>

                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                            <button
                                type="button"
                                className="disp-btn"
                                style={{ background: '#28a745', color: '#fff' }}
                                onClick={onStart}
                                disabled={!startEnabled}
                                title="Démarrer"
                            >
                                <FontAwesomeIcon icon={engineBusy || engineStarting ? faSpinner : faPlay} spin={engineBusy || engineStarting} />
                                Démarrer
                            </button>
                            <button
                                type="button"
                                className="disp-btn"
                                style={{ background: '#ffc107', color: '#222' }}
                                onClick={onPause}
                                disabled={!pauseEnabled}
                                title="Pause"
                            >
                                <FontAwesomeIcon icon={faPause} />
                                Pause
                            </button>
                            <button
                                type="button"
                                className="disp-btn"
                                style={{ background: '#28a745', color: '#fff' }}
                                onClick={onResume}
                                disabled={!resumeEnabled}
                                title="Reprendre"
                            >
                                <FontAwesomeIcon icon={faForward} />
                                Reprendre
                            </button>
                            <button
                                type="button"
                                className="disp-btn"
                                style={{ background: '#c62828', color: '#fff' }}
                                onClick={onStop}
                                disabled={!stopEnabled}
                                title="Arrêter"
                            >
                                <FontAwesomeIcon icon={faStop} />
                                Arrêter
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
