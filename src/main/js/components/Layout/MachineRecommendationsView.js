import React, { Component } from 'react';
import moment from 'moment';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faCheckCircle, faClock, faExclamationTriangle,
    faLayerGroup, faSitemap,
} from '@fortawesome/free-solid-svg-icons';

class MachineRecommendationsView extends Component {
    fmtMin(value) {
        if (value == null || isNaN(value)) return '-';
        return `${Number(value).toFixed(0)} min`;
    }

    fmtLength(value) {
        if (value == null || isNaN(value)) return null;
        return `${Number(value).toFixed(0)} m`;
    }

    filterZones(tableFeed) {
        const zones = tableFeed?.zones || [];
        const zoneFilter = this.props.zoneFilter || 'All';
        if (zoneFilter === 'All') return zones;
        return zones.filter(z => z.zoneNom === zoneFilter);
    }

    allTables(zones) {
        const tables = [];
        zones.forEach(zone => (zone.tables || []).forEach(table => {
            tables.push({ ...table, zoneNom: zone.zoneNom, zoneCategory: zone.category });
        }));
        return tables.sort((a, b) => {
            const idle = Number(a.timeToIdleMinutes || 0) - Number(b.timeToIdleMinutes || 0);
            if (idle !== 0) return idle;
            return String(a.tableNom || '').localeCompare(String(b.tableNom || ''));
        });
    }

    idleLabel(table) {
        if (table.idleNow) return 'Libre';
        return `Libre dans ${this.fmtMin(table.timeToIdleMinutes)}`;
    }

    idleTone(table) {
        if (table.idleNow || Number(table.timeToIdleMinutes || 0) <= 0) return 'now';
        if (Number(table.timeToIdleMinutes || 0) <= 20) return 'soon';
        return 'ok';
    }

    renderCandidate(candidate, idx) {
        const reasons = candidate.reasons || [];
        return (
            <div key={`${candidate.serie}-${idx}`} className={`wb-rec-card ${idx === 0 ? 'wb-rec-card-top' : ''}`}>
                <div className="wb-rec-rank">{idx + 1}</div>
                <div className="wb-rec-main">
                    <div className="wb-rec-title">
                        <strong>{candidate.serie || '-'}</strong>
                        <span>{candidate.sequence || '-'}</span>
                        <em>{candidate.sequenceStatus || '-'}</em>
                    </div>
                    <div className="wb-rec-meta">
                        {candidate.refTissus && <span>{candidate.refTissus}</span>}
                        {candidate.longueur != null && <span>L {this.fmtLength(candidate.longueur)}</span>}
                        {candidate.nbrCouche != null && <span>C {candidate.nbrCouche}</span>}
                        {candidate.validatedMinutes != null && <span>~{this.fmtMin(candidate.validatedMinutes)}</span>}
                        <span>Coupe Waiting</span>
                        <span>Matelassage Waiting</span>
                    </div>
                    <div className="wb-rec-signals">
                        <span className={`wb-rec-pill ${candidate.materialInZone ? 'wb-rec-pill-ok' : 'wb-rec-pill-warn'}`}>
                            {candidate.materialInZone ? 'Matière prête' : 'Vérifier matière'}
                        </span>
                        {candidate.sameRefTissuMounted && <span className="wb-rec-pill wb-rec-pill-info">Même rouleau</span>}
                        {candidate.fitsTableLength === false && <span className="wb-rec-pill wb-rec-pill-warn">Ne tient pas</span>}
                    </div>
                    {reasons.length > 0 && (
                        <div className="wb-rec-reasons">
                            {reasons.slice(0, 3).map((reason, i) => (
                                <span key={i}>{reason}</span>
                            ))}
                        </div>
                    )}
                </div>
                <div className="wb-rec-score">{Number(candidate.score || 0).toFixed(0)}</div>
            </div>
        );
    }

    renderTable(table) {
        const candidates = table.candidates || [];
        const idleTone = this.idleTone(table);
        // A table about to go idle with no ready serie = the "idle zone" risk
        // (operators with nothing to run). Flag it so the chef feeds it first.
        const starved = candidates.length === 0
            && (table.idleNow || Number(table.timeToIdleMinutes || 0) <= 20);
        return (
            <div key={table.tableNom} className={`wb-machine-card${starved ? ' wb-machine-card-starved' : ''}`}>
                <div className="wb-machine-head">
                    <div className="wb-machine-name">
                        <FontAwesomeIcon icon={faLayerGroup} />
                        <strong>{table.tableNom}</strong>
                        {table.machineType && <span>{table.machineType}</span>}
                    </div>
                    <div className="wb-machine-zone">
                        <FontAwesomeIcon icon={faSitemap} />
                        {table.zoneNom}
                    </div>
                    <div className={`wb-machine-idle wb-machine-idle-${idleTone}`}>
                        <FontAwesomeIcon icon={faClock} />
                        {this.idleLabel(table)}
                    </div>
                </div>
                {table.mountedRefTissu && (
                    <div className="wb-machine-mounted">
                        Matière montée: <strong>{table.mountedRefTissu}</strong>
                    </div>
                )}
                <div className="wb-rec-list">
                    {candidates.length > 0
                        ? candidates.map((candidate, idx) => this.renderCandidate(candidate, idx))
                        : (
                            <div className={`wb-rec-empty${starved ? ' wb-rec-empty-starved' : ''}`}>
                                <FontAwesomeIcon icon={faExclamationTriangle} />
                                {starved
                                    ? ' Bientôt libre — aucune série prête. Alimenter cette machine en priorité.'
                                    : ' Aucune série Waiting/Waiting RELEASED ou STARTED pour cette machine.'}
                            </div>
                        )}
                </div>
            </div>
        );
    }

    renderSummary(tableFeed, tables) {
        const candidateCount = tables.reduce((sum, table) => sum + (table.candidates || []).length, 0);
        const blocked = tables.filter(table => (table.candidates || []).length === 0).length;
        return (
            <div className="wb-rec-summary">
                <div className="wb-rec-summary-title">
                    <FontAwesomeIcon icon={faCheckCircle} />
                    <span>Top 3 séries par machine</span>
                    {tableFeed?.asOf && <small>{moment(tableFeed.asOf).format('HH:mm:ss')}</small>}
                </div>
                <div className="wb-rec-summary-stat">
                    <span>Machines</span>
                    <strong>{tables.length}</strong>
                </div>
                <div className="wb-rec-summary-stat">
                    <span>Séries proposées</span>
                    <strong>{candidateCount}</strong>
                </div>
                <div className={`wb-rec-summary-stat ${blocked > 0 ? 'wb-rec-summary-warn' : ''}`}>
                    <span>Sans candidat</span>
                    <strong>{blocked}</strong>
                </div>
            </div>
        );
    }

    render() {
        const tableFeed = this.props.data?.tableFeed;
        if (!tableFeed) {
            return <div className="wb-rec-empty">Recommandations machines indisponibles.</div>;
        }
        const zones = this.filterZones(tableFeed);
        const tables = this.allTables(zones);
        return (
            <div className="wb-rec-view">
                {this.renderSummary(tableFeed, tables)}
                {tables.length > 0
                    ? <div className="wb-machine-grid">{tables.map(table => this.renderTable(table))}</div>
                    : <div className="wb-rec-empty">Aucune machine active dans le filtre courant.</div>}
            </div>
        );
    }
}

export default MachineRecommendationsView;
