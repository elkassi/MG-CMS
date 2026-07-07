import React, { Component } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faRotate, faSpinner, faCalendarDay, faFilter,
} from '@fortawesome/free-solid-svg-icons';
import { WorkbenchContext } from './WorkbenchContext';

class WorkbenchHeader extends Component {
    static contextType = WorkbenchContext;

    render() {
        const { data, loading, error, date, shift, setDate, setShift, refresh } = this.context;

        const zones = data?.liveCharge?.zones || [];
        const zoneNames = ['All', ...zones.map(z => z.zoneNom)];
        const { zoneFilter, onZoneFilterChange } = this.props;

        // Data freshness: chefs must trust the live recommendations are current.
        const cachedAt = data && data._cachedAt ? new Date(data._cachedAt) : null;
        const ageSec = cachedAt ? Math.max(0, Math.round((Date.now() - cachedAt.getTime()) / 1000)) : null;
        const stale = ageSec != null && ageSec > 120;

        return (
            <div className="wb-header">
                <div className="wb-header-left">
                    <h2 className="wb-header-title">
                        <FontAwesomeIcon icon={faCalendarDay} style={{ marginRight: 8 }} />
                        Process Workbench
                    </h2>
                </div>

                <div className="wb-header-controls">
                    <div className="wb-control-group">
                        <label>Date</label>
                        <input
                            type="date"
                            value={date}
                            onChange={e => setDate(e.target.value)}
                            className="wb-input"
                        />
                    </div>

                    <div className="wb-control-group">
                        <label>Shift</label>
                        <select
                            value={shift}
                            onChange={e => setShift(e.target.value)}
                            className="wb-select"
                        >
                            <option value={1}>1 (Nuit)</option>
                            <option value={2}>2 (Matin)</option>
                            <option value={3}>3 (Après-midi)</option>
                        </select>
                    </div>

                    <div className="wb-control-group">
                        <label>Zone</label>
                        <div className="wb-zone-filter">
                            <FontAwesomeIcon icon={faFilter} size="sm" />
                            <select
                                value={zoneFilter || 'All'}
                                onChange={e => onZoneFilterChange && onZoneFilterChange(e.target.value)}
                                className="wb-select"
                            >
                                {zoneNames.map(z => (
                                    <option key={z} value={z}>{z}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    {cachedAt && (
                        <div
                            className={`wb-freshness ${stale ? 'wb-freshness-stale' : ''}`}
                            title={stale ? 'Données possiblement périmées' : 'Données à jour'}
                        >
                            <span className="wb-freshness-dot" />
                            MAJ {cachedAt.toLocaleTimeString('fr-FR')}
                        </div>
                    )}

                    <button
                        className="wb-refresh-btn"
                        onClick={refresh}
                        disabled={loading}
                        title="Rafraîchir"
                    >
                        <FontAwesomeIcon icon={loading ? faSpinner : faRotate} spin={loading} />
                    </button>
                </div>

                {error && (
                    <div className="wb-header-error">
                        {error}
                    </div>
                )}
            </div>
        );
    }
}

export default WorkbenchHeader;
