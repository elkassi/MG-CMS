import React, { Component } from 'react';
import moment from 'moment';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faBullseye, faTruck, faBoxesPacking, faCheckCircle,
    faExclamationTriangle, faArrowRight, faRecycle, faSitemap,
    faPlayCircle, faLayerGroup, faSpinner,
} from '@fortawesome/free-solid-svg-icons';

class SequenceFocusView extends Component {
    fmtMin(value) {
        if (value == null || isNaN(value)) return '—';
        if (value < 60) return `${Number(value).toFixed(0)} min`;
        const h = Math.floor(value / 60);
        const m = Math.round(value - h * 60);
        return m > 0 ? `${h} h ${m} min` : `${h} h`;
    }

    fmtDate(value) {
        return value ? moment(value).format('DD/MM HH:mm') : '—';
    }

    actionLabel(action) {
        if (action === 'FINISH_SEQUENCE') return 'Finir séquence';
        if (action === 'PREPARE_BOXES') return 'Préparer boxes';
        return 'Surveiller';
    }

    stateLabel(state) {
        if (state === 'OPEN') return 'Ouverte';
        if (state === 'JUST_ADDED') return 'Ajoutée';
        if (state === 'READY_TO_START') return 'À préparer';
        return 'Backlog';
    }

    materialLabel(status) {
        if (status === 'OK') return 'Matière OK';
        if (status === 'OUT_OF_ZONE') return 'Hors zone';
        if (status === 'SHORTAGE') return 'Manque';
        if (status === 'NONE') return 'Aucun stock';
        if (status === 'RETURN_TO_STOCK') return 'Retour stock';
        return 'Matière —';
    }

    filterZones(focus) {
        const zones = focus?.zones || [];
        const zoneFilter = this.props.zoneFilter || 'All';
        if (zoneFilter === 'All') return zones;
        return zones.filter(z => z.zone === zoneFilter);
    }

    renderSummary(focus, zones) {
        const totals = focus?.totals || {};
        const balancing = focus && focus.dispatchBalanced === false;
        return (
            <div className="wb-focus-summary">
                <div className="wb-focus-title">
                    <FontAwesomeIcon icon={faBullseye} />
                    <span>Focus Séquence</span>
                    {focus?.generatedAt && (
                        <small>{moment(focus.generatedAt).format('HH:mm:ss')}</small>
                    )}
                </div>
                <div className="wb-focus-stat">
                    <span>Zones</span>
                    <strong>{zones.length}</strong>
                </div>
                <div className="wb-focus-stat">
                    <span>Ouvertes</span>
                    <strong>{totals.openSequenceCount || 0}</strong>
                </div>
                <div className="wb-focus-stat">
                    <span>À préparer</span>
                    <strong>{totals.aboutToStartCount || 0}</strong>
                </div>
                <div className={`wb-focus-stat ${(totals.occupiedBoxes || 0) > (totals.boxCapacity || 0) ? 'wb-focus-stat-warn' : ''}`}>
                    <span>Boxes</span>
                    <strong>{totals.occupiedBoxes || 0}/{totals.boxCapacity || 0}</strong>
                </div>
                <div className={`wb-focus-stat ${totals.materialIssueCount > 0 ? 'wb-focus-stat-warn' : ''}`}>
                    <span>Matière</span>
                    <strong>{totals.materialIssueCount || 0}</strong>
                </div>
                <div className={`wb-focus-balance ${balancing ? 'wb-focus-balance-wait' : 'wb-focus-balance-ok'}`}>
                    <FontAwesomeIcon icon={balancing ? faSpinner : faCheckCircle} spin={balancing} />
                    {balancing ? 'Engine en équilibrage' : 'Plan équilibré'}
                </div>
            </div>
        );
    }

    renderSequence(seq, compact = false) {
        const statusClass = String(seq.materialStatus || 'OK').toLowerCase();
        return (
            <div key={`${seq.sequence}-${seq.state}-${compact ? 'c' : 'f'}`} className={`wb-focus-seq wb-focus-seq-${String(seq.state).toLowerCase()}`}>
                <div className="wb-focus-seq-main">
                    <div className="wb-focus-seq-id">
                        <FontAwesomeIcon icon={seq.state === 'OPEN' ? faPlayCircle : faLayerGroup} />
                        <strong>{seq.sequence}</strong>
                        <span className={`wb-focus-state wb-focus-state-${String(seq.state).toLowerCase()}`}>
                            {this.stateLabel(seq.state)}
                        </span>
                    </div>
                    <span className="wb-focus-action">{this.actionLabel(seq.action)}</span>
                </div>
                <div className="wb-focus-seq-meta">
                    <span>{seq.boxCount || 0} box</span>
                    <span>{seq.remainingSeries || 0}/{seq.totalSeries || 0} séries</span>
                    <span>{this.fmtMin(seq.predictedCloseMinutes)}</span>
                    {seq.firstStart && <span>Début {this.fmtDate(seq.firstStart)}</span>}
                    {seq.minutesToStart != null && seq.minutesToStart >= 0 && (
                        <span>dans {this.fmtMin(seq.minutesToStart)}</span>
                    )}
                    <span className={`wb-focus-material wb-focus-material-${statusClass}`}>
                        {this.materialLabel(seq.materialStatus)}
                    </span>
                </div>
                {!compact && seq.machines && seq.machines.length > 0 && (
                    <div className="wb-focus-machines">
                        {seq.machines.slice(0, 4).map(m => <span key={m}>{m}</span>)}
                    </div>
                )}
            </div>
        );
    }

    renderBoxOccupancy(zone) {
        const occupancy = zone.boxOccupancy || {};
        const sequences = occupancy.occupiedSequences || [];
        return (
            <div className="wb-focus-occupancy">
                <div className="wb-focus-occupancy-head">
                    <span>Occupation boxes</span>
                    <strong>{occupancy.occupiedBoxes || 0}/{occupancy.maxBoxes || 0}</strong>
                </div>
                <div className="wb-focus-occupancy-bar">
                    <span style={{ width: `${Math.min(100, occupancy.occupancyPct || 0)}%` }} />
                </div>
                {sequences.length > 0 ? (
                    <div className="wb-focus-occupied-list">
                        {sequences.map(seq => (
                            <span key={`occ-${seq.sequence}`}>
                                {seq.sequence} · {seq.boxCount || 0} box
                            </span>
                        ))}
                    </div>
                ) : (
                    <div className="wb-focus-occupied-empty">Aucune séquence ouverte.</div>
                )}
            </div>
        );
    }

    renderChefPanel(zone) {
        const focusSequences = zone.focusSequences || [];
        const alerts = zone.chefAlerts || [];
        return (
            <div className="wb-focus-panel">
                <div className="wb-focus-panel-head">
                    <FontAwesomeIcon icon={faSitemap} />
                    <span>Chef de zone</span>
                </div>
                {this.renderBoxOccupancy(zone)}
                {alerts.length > 0 && (
                    <div className="wb-focus-alert-list">
                        {alerts.map(seq => this.renderSequence(seq, true))}
                    </div>
                )}
                <div className="wb-focus-list">
                    {focusSequences.length > 0
                        ? focusSequences.map(seq => this.renderSequence(seq))
                        : <div className="wb-focus-empty">Aucune séquence à préparer dans ce créneau.</div>}
                </div>
            </div>
        );
    }

    renderMaterialRow(row, mode) {
        const statusClass = String(row.status || '').toLowerCase();
        const options = row.transferOptions || [];
        return (
            <tr key={`${mode}-${row.material}`}>
                <td className="wb-focus-material-ref">{row.material}</td>
                <td>{Number(row.needed || 0).toFixed(0)} m</td>
                <td>{Number(row.availableInZone || 0).toFixed(0)} m</td>
                <td>{Number(row.zoneGap || row.deficit || 0).toFixed(0)} m</td>
                <td>
                    <span className={`wb-focus-mat-status wb-focus-mat-${statusClass}`}>
                        {this.materialLabel(row.status)}
                    </span>
                </td>
                <td>
                    {options.length > 0 ? (
                        <span className="wb-focus-transfer">
                            {options[0].zone} <FontAwesomeIcon icon={faArrowRight} /> {Number(options[0].available || 0).toFixed(0)} m
                        </span>
                    ) : row.status === 'RETURN_TO_STOCK' ? (
                        <span className="wb-focus-transfer">
                            <FontAwesomeIcon icon={faRecycle} /> retirer
                        </span>
                    ) : '—'}
                </td>
            </tr>
        );
    }

    renderLogisticsPanel(zone) {
        const logistics = zone.logistics || {};
        const shortages = logistics.shortages || [];
        const transfers = logistics.transferSuggestions || [];
        const returns = logistics.returnCandidates || [];
        const rows = [...shortages, ...transfers];
        return (
            <div className="wb-focus-panel">
                <div className="wb-focus-panel-head">
                    <FontAwesomeIcon icon={faTruck} />
                    <span>Logistique 2h</span>
                    <span className={`wb-focus-log-status wb-focus-log-${String(logistics.status || 'OK').toLowerCase()}`}>
                        {logistics.status || 'OK'}
                    </span>
                </div>
                {rows.length > 0 ? (
                    <div className="wb-focus-table-wrap">
                        <table className="wb-focus-table">
                            <thead>
                                <tr>
                                    <th>Matière</th>
                                    <th>Besoin</th>
                                    <th>Rack</th>
                                    <th>Gap</th>
                                    <th>Status</th>
                                    <th>Action</th>
                                </tr>
                            </thead>
                            <tbody>
                                {rows.map(row => this.renderMaterialRow(row, 'need'))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div className="wb-focus-ok">
                        <FontAwesomeIcon icon={faCheckCircle} /> Matière OK pour les 2h
                    </div>
                )}
                {returns.length > 0 && (
                    <div className="wb-focus-return">
                        <div className="wb-focus-return-title">
                            <FontAwesomeIcon icon={faBoxesPacking} /> À retirer du rack
                        </div>
                        <div className="wb-focus-return-list">
                            {returns.slice(0, 6).map(r => (
                                <span key={r.material}>{r.material} · {Number(r.availableInZone || 0).toFixed(0)} m</span>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        );
    }

    renderZone(zone) {
        const occupancy = zone.boxOccupancy || {};
        return (
            <div key={zone.zone} className="wb-focus-zone">
                <div className="wb-focus-zone-head">
                    <div>
                        <FontAwesomeIcon icon={faSitemap} />
                        <strong>{zone.zone}</strong>
                        {zone.category && <span>{zone.category}</span>}
                    </div>
                    <div className="wb-focus-zone-kpis">
                        <span>{zone.openSequenceCount || 0} ouvertes</span>
                        <span>{zone.aboutToStartCount || 0} à préparer</span>
                        <span className={(occupancy.occupiedBoxes || 0) > (occupancy.maxBoxes || 0) ? 'wb-focus-kpi-warn' : ''}>
                            {occupancy.occupiedBoxes || 0}/{occupancy.maxBoxes || 0} boxes
                        </span>
                        <span className={zone.materialIssueCount > 0 ? 'wb-focus-kpi-warn' : ''}>
                            {zone.materialIssueCount || 0} matière
                        </span>
                    </div>
                </div>
                <div className="wb-focus-zone-grid">
                    {this.renderChefPanel(zone)}
                    {this.renderLogisticsPanel(zone)}
                </div>
            </div>
        );
    }

    render() {
        const focus = this.props.data?.sequenceFocus;
        if (!focus) {
            return <div className="wb-focus-empty">Focus séquence indisponible.</div>;
        }
        const zones = this.filterZones(focus);
        return (
            <div className="wb-focus-view">
                {this.renderSummary(focus, zones)}
                {zones.length > 0
                    ? zones.map(zone => this.renderZone(zone))
                    : <div className="wb-focus-empty">Aucune zone dans le filtre courant.</div>}
            </div>
        );
    }
}

export default SequenceFocusView;
