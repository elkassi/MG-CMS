import React, { Component } from 'react';
import moment from 'moment';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faLock, faLockOpen, faChevronDown, faChevronRight,
    faExclamationTriangle, faClock, faIndustry, faSitemap,
    faCheckCircle, faPlayCircle, faPauseCircle, faQuestionCircle,
    faTimes,
} from '@fortawesome/free-solid-svg-icons';
import './styles/LiveChargeView.scss';

/**
 * Status-aware live snapshot for /processDispatcher.
 *
 * Renders the full backend math: capacity formula per (zone, machineType),
 * remaining-time breakdown per serie (Waiting / In progress / Complete),
 * and lock metadata (EXPLICIT_ACCEPTED vs IMPLICIT_TABLE_STRICT).
 *
 * Drilling down: zone card click opens modal -> locked/pending sections -> per-sequence row -> per-serie table.
 *
 * Props:
 *   data                    — LiveChargeDto (see LiveChargeDto.java)
 *   loading                 — bool, show spinner
 *   onRefresh               — callback to reload
 *   renderSequenceActions   — optional (seq, zone) => ReactNode. Action toolbar
 *                             rendered under each sequence row's zoneInfo.
 *                             Used by ChefDeZonePage to inject Accept/Reject/Pin/Pull.
 *   renderHeaderExtras      — optional () => ReactNode. Extra controls in the header card.
 */
class LiveChargeView extends Component {

    constructor(props) {
        super(props);
        this.state = {
            expandedLocked: {},     // zoneNom -> bool
            expandedPending: {},    // zoneNom -> bool
            expandedSequences: {},  // sequence -> bool
            unassignedExpanded: false,
            selectedZoneModal: null,
        };
    }

    toggle(stateKey, key) {
        this.setState(prev => ({
            [stateKey]: { ...prev[stateKey], [key]: !prev[stateKey][key] },
        }));
    }

    fmtMin = (m) => {
        if (m == null || isNaN(m)) return '—';
        if (m === 0) return '0 min';
        if (m < 60) return `${m.toFixed(1)} min`;
        const h = Math.floor(m / 60);
        const rem = m - h * 60;
        return rem < 0.5 ? `${h} h` : `${h} h ${rem.toFixed(0)} min`;
    };

    fmtPct = (p) => p == null ? '—' : `${p.toFixed(1)}%`;

    fmtDate = (d) => d ? moment(d).format('DD/MM HH:mm') : '—';

    statusBadge = (status, kind) => {
        if (!status) return <span className="lc-status-badge lc-status-unknown">—</span>;
        const s = status.trim();
        let cls = 'lc-status-unknown';
        let icon = faQuestionCircle;
        if (s === 'Complete') { cls = 'lc-status-complete'; icon = faCheckCircle; }
        else if (s === 'In progress') { cls = 'lc-status-progress'; icon = faPlayCircle; }
        else if (s === 'Waiting') { cls = 'lc-status-waiting'; icon = faPauseCircle; }
        else if (s === 'Incomplete') { cls = 'lc-status-incomplete'; icon = faExclamationTriangle; }
        return (
            <span className={`lc-status-badge ${cls}`} title={`${kind}: ${s}`}>
                <FontAwesomeIcon icon={icon} /> {s}
            </span>
        );
    };

    loadBar = (loadPct, label) => {
        const clamped = Math.min(100, Math.max(0, loadPct || 0));
        let cls = 'lc-bar-ok';
        if (loadPct > 100) cls = 'lc-bar-over';
        else if (loadPct > 90) cls = 'lc-bar-warn';
        return (
            <div className="lc-load-bar">
                <div className={`lc-load-bar-fill ${cls}`} style={{ width: `${clamped}%` }} />
                {loadPct > 100 && (
                    <div className="lc-load-bar-overflow"
                         style={{ width: `${Math.min(100, loadPct - 100)}%` }} />
                )}
                {label && <div className="lc-load-bar-label">{label}</div>}
            </div>
        );
    };

    // ------------------------------------------------------------------ totals

    renderHeader() {
        const { data, renderHeaderExtras } = this.props;
        if (!data) return null;
        const t = data.totals;
        const overallLoad = t.totalCapacityMinutes > 0
            ? (t.totalRemainingMinutes / t.totalCapacityMinutes) * 100
            : 0;
        return (
            <div className="lc-header-card">
                <div className="lc-header-row">
                    <div className="lc-header-title">
                        <FontAwesomeIcon icon={faClock} /> Charge en temps réel
                    </div>
                    <div className="lc-header-meta">
                        {data.date} · shift {data.shift} · à {moment(data.asOf).format('HH:mm:ss')}
                    </div>
                    {renderHeaderExtras && (
                        <div className="lc-header-extras">{renderHeaderExtras()}</div>
                    )}
                </div>
                <div className="lc-stat-grid">
                    <div className="lc-stat">
                        <div className="lc-stat-label">Séquences totales</div>
                        <div className="lc-stat-value">{t.totalSequences}</div>
                    </div>
                    <div className="lc-stat lc-stat-locked">
                        <div className="lc-stat-label">
                            <FontAwesomeIcon icon={faLock} /> Verrouillées
                        </div>
                        <div className="lc-stat-value">{t.lockedSequences}</div>
                    </div>
                    <div className="lc-stat lc-stat-pending">
                        <div className="lc-stat-label">
                            <FontAwesomeIcon icon={faLockOpen} /> Mobiles
                        </div>
                        <div className="lc-stat-value">{t.pendingSequences}</div>
                    </div>
                    <div className="lc-stat lc-stat-unassigned">
                        <div className="lc-stat-label">Sans zone</div>
                        <div className="lc-stat-value">{t.unassignedSequences}</div>
                    </div>
                </div>
                <div className="lc-totals-line">
                    <span><b>Reste à couper:</b> {this.fmtMin(t.totalRemainingMinutes)}</span>
                    <span><b>Capacité totale:</b> {this.fmtMin(t.totalCapacityMinutes)}</span>
                    <span>
                        <b>Charge globale:</b> {this.fmtPct(overallLoad)}
                        <span className="lc-formula-note">
                            &nbsp;= {this.fmtMin(t.totalRemainingMinutes)} / {this.fmtMin(t.totalCapacityMinutes)}
                        </span>
                    </span>
                    <span className="lc-formula-note">
                        Shift = {data.shiftMinutes} min
                    </span>
                </div>
            </div>
        );
    }

    // ------------------------------------------------------------------ zone

    renderMachineTypeRow(mt) {
        const formula = `${mt.activeMachines} machine(s) × ${mt.shiftMinutes} min × ${this.fmtPct(mt.efficiencePct)}`;
        return (
            <div key={mt.machineType} className="lc-mt-row">
                <div className="lc-mt-head">
                    <span className="lc-mt-name">
                        <FontAwesomeIcon icon={faIndustry} /> {mt.machineType}
                        <span className="lc-mt-groupe">[{mt.groupe}]</span>
                    </span>
                    <span className="lc-mt-load">
                        {this.fmtMin(mt.totalRemainingMinutes)} / {this.fmtMin(mt.capacityMinutes)}
                        <span className="lc-mt-pct"> ({this.fmtPct(mt.loadPct)})</span>
                    </span>
                </div>
                <div className="lc-mt-formula">
                    Capacité = {formula} = <b>{this.fmtMin(mt.capacityMinutes)}</b>
                    {mt.activeMachines === 0 && (
                        <span className="lc-mt-noactive">
                            &nbsp;⚠️ Aucune machine active — capacité = 0
                        </span>
                    )}
                </div>
                {this.loadBar(mt.loadPct)}
                <div className="lc-mt-split">
                    <span className="lc-split-locked">
                        <FontAwesomeIcon icon={faLock} /> Verrouillé: <b>{this.fmtMin(mt.lockedRemainingMinutes)}</b>
                    </span>
                    <span className="lc-split-pending">
                        <FontAwesomeIcon icon={faLockOpen} /> Mobile: <b>{this.fmtMin(mt.pendingRemainingMinutes)}</b>
                    </span>
                </div>
            </div>
        );
    }

    renderSequenceRow(seq, zoneCtx) {
        const expanded = !!this.state.expandedSequences[seq.sequence];
        const { renderSequenceActions } = this.props;
        const lockBadge = seq.locked ? (
            seq.lockReason === 'EXPLICIT_ACCEPTED' ? (
                <span className="lc-lock-badge lc-lock-explicit"
                      title="Chef-de-zone a accepté la séquence">
                    <FontAwesomeIcon icon={faLock} /> Acceptée
                </span>
            ) : (
                <span className="lc-lock-badge lc-lock-implicit"
                      title={`Coupe en cours sur la table ${seq.lockingTableNom} (statut: ${seq.lockingStatusCoupe})`}>
                    <FontAwesomeIcon icon={faLock} /> Table {seq.lockingTableNom}
                </span>
            )
        ) : (
            <span className="lc-lock-badge lc-lock-none"
                  title="Séquence mobile — l'engine peut la déplacer">
                <FontAwesomeIcon icon={faLockOpen} /> Mobile
            </span>
        );
        const zoneSourceLabels = {
            LOCKED: { txt: 'Verrouillée', cls: 'lc-zsrc-locked' },
            ENGINE_PROPOSED: { txt: 'Proposée par engine', cls: 'lc-zsrc-engine' },
            DISPATCHED: { txt: 'Dispatchée (DB)', cls: 'lc-zsrc-dispatched' },
            PREFERRED: { txt: 'Zone préférée (par défaut)', cls: 'lc-zsrc-preferred' },
            NONE: { txt: 'Sans zone', cls: 'lc-zsrc-none' },
        };
        const zsrc = zoneSourceLabels[seq.zoneSource] || zoneSourceLabels.NONE;
        const zoneInfo = (
            <div className="lc-seq-zones">
                <span title="Zone préférée (CuttingRequest.zone)">
                    Fix: <b>{seq.zoneFix || '—'}</b>
                </span>
                <span title="dispatchedZone en DB">
                    Dispatched: <b>{seq.dispatchedZone || '—'}</b>
                </span>
                <span className={`lc-zsrc-badge ${zsrc.cls}`}
                      title={`Zone effective vient de: ${seq.zoneSource}`}>
                    Effective: <b>{seq.effectiveZone || '—'}</b> [{zsrc.txt}]
                </span>
                {seq.zoneMismatch && (
                    <span className="lc-zone-mismatch"
                          title="effectiveZone diffère de dispatchedZone — overlay actif">
                        <FontAwesomeIcon icon={faExclamationTriangle} />
                        &nbsp;Override
                    </span>
                )}
                {seq.pinnedByChef && (
                    <span className="lc-pinned" title="Chef a épinglé">📌 Épinglée</span>
                )}
                {seq.zoneAcceptanceStatus && (
                    <span className={`lc-acc-status lc-acc-${seq.zoneAcceptanceStatus.toLowerCase()}`}>
                        {seq.zoneAcceptanceStatus}
                    </span>
                )}
            </div>
        );

        // Phase 8 stubs — only render when fields are present
        const phase8Info = (
            <>
                {seq.boxCycleTimeMinutes != null && (
                    <span className="lc-phase8-box-cycle" title="Temps de cycle de la boîte">
                        Box cycle: {this.fmtMin(seq.boxCycleTimeMinutes)}
                    </span>
                )}
                {seq.dueDate && (
                    <span className="lc-phase8-due" title="Date d'échéance">
                        Due: {seq.dueDate}
                    </span>
                )}
                {seq.materialStatus && (
                    <span className={`lc-phase8-material lc-material-${(seq.materialStatus || '').toLowerCase()}`}
                        title={`Matière: ${seq.materialStatus}`}>
                        {seq.materialStatus === 'AVAILABLE_IN_ZONE' ? 'OK'
                            : seq.materialStatus === 'NOT_IN_ZONE' ? 'HORS ZONE'
                            : '—'}
                    </span>
                )}
            </>
        );

        return (
            <div key={seq.sequence} className={`lc-seq-row ${expanded ? 'lc-seq-row-open' : ''}`}>
                <div className="lc-seq-head" onClick={() => this.toggle('expandedSequences', seq.sequence)}>
                    <FontAwesomeIcon icon={expanded ? faChevronDown : faChevronRight} className="lc-seq-chev" />
                    <span className="lc-seq-id">{seq.sequence}</span>
                    {lockBadge}
                    <span className="lc-seq-remaining">
                        {this.fmtMin(seq.totalRemainingMinutes)} restant
                    </span>
                    <span className="lc-seq-series-count">
                        {seq.series.length} série{seq.series.length > 1 ? 's' : ''}
                    </span>
                    <span className="lc-seq-phase8">
                        {phase8Info}
                    </span>
                </div>
                {zoneInfo}
                {renderSequenceActions && (
                    <div className="lc-seq-actions" onClick={(e) => e.stopPropagation()}>
                        {renderSequenceActions(seq, zoneCtx)}
                    </div>
                )}
                {expanded && this.renderSerieTable(seq.series, seq.effectiveZone)}
            </div>
        );
    }

    renderSerieTable(series, ownerZoneNom) {
        return (
            <div className="lc-serie-table-wrap">
                <table className="lc-serie-table">
                    <thead>
                        <tr>
                            <th>Série</th>
                            <th>Machine</th>
                            <th title="Zone qui exécute cette série (peut différer de la zone de la séquence si SHARED)">Zone exécution</th>
                            <th>Estim. raw</th>
                            <th title="Validé via TimingModel ou tempsDeCoupe">Estim. validé</th>
                            <th title="Statut côté coupe">Coupe</th>
                            <th title="Statut côté matelassage">Mat.</th>
                            <th>Table coupe</th>
                            <th>Date début coupe</th>
                            <th>Date fin coupe</th>
                            <th title="Min écoulés depuis début (si In progress)">Écoulé</th>
                            <th title="Min restants après calcul">Reste</th>
                            <th>Calcul</th>
                            {/* Phase 8 columns — rendered when any serie has the fields */}
                            {series.some(s => s.refTissus || s.materialStatus) && <th>Réf. Tissu</th>}
                        </tr>
                    </thead>
                    <tbody>
                        {series.map(s => {
                            const status = s.statusCoupe ? s.statusCoupe.trim() : '';
                            let calcCell;
                            if (status === 'Complete') {
                                calcCell = <span className="lc-calc-zero">Complete → 0</span>;
                            } else if (status === 'In progress') {
                                calcCell = (
                                    <span className="lc-calc-progress">
                                        max(0, {s.validatedMinutes.toFixed(1)} − {s.elapsedMinutes.toFixed(1)})
                                        &nbsp;= <b>{s.remainingMinutes.toFixed(1)}</b>
                                    </span>
                                );
                            } else {
                                calcCell = (
                                    <span className="lc-calc-waiting">
                                        full = <b>{s.validatedMinutes.toFixed(1)}</b>
                                    </span>
                                );
                            }
                            // Highlight when serie executes in a different zone
                            // than the sequence's owner — e.g. LASER-DXF on SHARED
                            // while the sequence is owned by FirstArticle.
                            const targetZ = s.targetZoneNom;
                            const targetDiffers = targetZ && ownerZoneNom && targetZ !== ownerZoneNom;
                            const isShared = s.targetZoneCategory === 'SHARED';
                            const targetCellClass = !targetZ ? 'lc-target-none'
                                : isShared ? 'lc-target-shared'
                                : targetDiffers ? 'lc-target-foreign'
                                : 'lc-target-owner';
                            return (
                                <tr key={s.serie}>
                                    <td className="lc-serie-id">{s.serie}</td>
                                    <td>{s.machine || '—'}</td>
                                    <td className={`lc-target-cell ${targetCellClass}`}
                                        title={!targetZ ? 'Aucune zone n\'héberge ce type de machine'
                                            : isShared ? 'Hébergé en SHARED (DIE / Gerber / LASER-DXF type)'
                                            : targetDiffers ? `Exécute hors de la zone de la séquence (${ownerZoneNom})`
                                            : 'Exécute dans la zone de la séquence'}>
                                        {targetZ || '—'}
                                        {targetZ && (
                                            <span className="lc-target-cat">[{s.targetZoneCategory}]</span>
                                        )}
                                    </td>
                                    <td>{s.tempsDeCoupe != null ? s.tempsDeCoupe.toFixed(1) : '—'}</td>
                                    <td title={`Source: ${s.timeSource}`}>
                                        {s.validatedMinutes.toFixed(1)}
                                        <span className="lc-time-source">[{s.timeSource}]</span>
                                    </td>
                                    <td>{this.statusBadge(s.statusCoupe, 'Coupe')}</td>
                                    <td>{this.statusBadge(s.statusMatelassage, 'Mat.')}</td>
                                    <td>{s.tableCoupe || '—'}</td>
                                    <td>{this.fmtDate(s.dateDebutCoupe)}</td>
                                    <td>{this.fmtDate(s.dateFinCoupe)}</td>
                                    <td>{s.elapsedMinutes > 0 ? s.elapsedMinutes.toFixed(1) : '—'}</td>
                                    <td className="lc-serie-remaining">{s.remainingMinutes.toFixed(1)}</td>
                                    <td className="lc-serie-calc">{calcCell}</td>
                                    {series.some(s => s.refTissus || s.materialStatus) && (
                                        <td>
                                            {s.refTissus && <span className="lc-ref-tissus">{s.refTissus}</span>}
                                            {s.materialStatus && (
                                                <span className={`lc-material-icon lc-material-${(s.materialStatus || '').toLowerCase()}`}
                                                    title={`Matière: ${s.materialStatus}`}>
                                                    {s.materialStatus === 'AVAILABLE_IN_ZONE' ? ' OK'
                                                        : s.materialStatus === 'NOT_IN_ZONE' ? ' HORS ZONE'
                                                        : ''}
                                                </span>
                                            )}
                                        </td>
                                    )}
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        );
    }

    renderZoneCard(zone) {
        const sharedClass = zone.category === 'SHARED' ? 'lc-zone-shared' : 'lc-zone-strict';
        const { renderZoneActions } = this.props;
        return (
            <div
                key={zone.zoneNom}
                className={`lc-zone-card ${sharedClass}`}
                onClick={() => this.setState({ selectedZoneModal: zone })}
                title="Cliquer pour voir le détail"
            >
                <div className="lc-zone-card-head">
                    <span className="lc-zone-card-name">
                        <FontAwesomeIcon icon={faSitemap} /> {zone.zoneNom}
                    </span>
                    <span className={`lc-zone-cat lc-zone-cat-${zone.category.toLowerCase()}`}>
                        {zone.category}
                    </span>
                </div>
                <div className="lc-zone-card-meta">
                    <span className="lc-zone-card-minutes">
                        {this.fmtMin(zone.totalRemainingMinutes)} / {this.fmtMin(zone.totalCapacityMinutes)}
                    </span>
                    <span className="lc-zone-card-pct">{this.fmtPct(zone.overallLoadPct)}</span>
                </div>
                <div className="lc-zone-card-bar">
                    {this.loadBar(zone.overallLoadPct, this.fmtPct(zone.overallLoadPct))}
                </div>
                <div className="lc-zone-card-chips">
                    <span className="lc-chip lc-chip-locked" title="Séquences verrouillées">
                        <FontAwesomeIcon icon={faLock} /> {zone.lockedSequences.length}
                    </span>
                    <span className="lc-chip lc-chip-pending" title="Séquences mobiles">
                        <FontAwesomeIcon icon={faLockOpen} /> {zone.pendingSequences.length}
                    </span>
                    <span className="lc-chip lc-chip-mt" title="Types de machine">
                        <FontAwesomeIcon icon={faIndustry} /> {zone.byMachineType.length}
                    </span>
                    {renderZoneActions && (
                        <span className="lc-zone-card-actions" onClick={(e) => e.stopPropagation()}>
                            {renderZoneActions(zone)}
                        </span>
                    )}
                </div>
            </div>
        );
    }

    renderZoneModal() {
        const { selectedZoneModal } = this.state;
        if (!selectedZoneModal) return null;
        const zone = selectedZoneModal;
        const sharedClass = zone.category === 'SHARED' ? 'lc-zone-shared' : 'lc-zone-strict';
        const lockedOpen = this.state.expandedLocked[zone.zoneNom] !== false;
        const pendingOpen = this.state.expandedPending[zone.zoneNom] !== false;
        return (
            <div className="lc-modal-overlay" onClick={() => this.setState({ selectedZoneModal: null })}>
                <div className="lc-modal" onClick={(e) => e.stopPropagation()}>
                    <div className={`lc-modal-header ${sharedClass}`}>
                        <div className="lc-modal-header-main">
                            <span className="lc-modal-zone-name">
                                <FontAwesomeIcon icon={faSitemap} /> {zone.zoneNom}
                            </span>
                            <span className={`lc-zone-cat lc-zone-cat-${zone.category.toLowerCase()}`}>
                                {zone.category}
                            </span>
                        </div>
                        <div className="lc-modal-header-meta">
                            <span>Charge: <b>{this.fmtPct(zone.overallLoadPct)}</b></span>
                            <span>{this.fmtMin(zone.totalRemainingMinutes)} / {this.fmtMin(zone.totalCapacityMinutes)}</span>
                        </div>
                        <button
                            className="lc-modal-close"
                            onClick={() => this.setState({ selectedZoneModal: null })}
                            title="Fermer"
                        >
                            <FontAwesomeIcon icon={faTimes} />
                        </button>
                    </div>
                    <div className="lc-modal-body">
                        <div className="lc-modal-section">
                            <h6 className="lc-modal-section-title">
                                <FontAwesomeIcon icon={faIndustry} /> Types de machine
                            </h6>
                            {zone.byMachineType.length === 0 ? (
                                <div className="lc-empty-mt">Aucun type de machine actif dans cette zone.</div>
                            ) : (
                                <div className="lc-mt-table-wrap">
                                    <table className="lc-mt-table">
                                        <thead>
                                            <tr>
                                                <th>Type</th>
                                                <th>Groupe</th>
                                                <th>Actives</th>
                                                <th>Formule capacité</th>
                                                <th>Verrouillé</th>
                                                <th>Mobile</th>
                                                <th>Charge</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {zone.byMachineType.map(mt => {
                                                const formula = `${mt.activeMachines} × ${mt.shiftMinutes} min × ${this.fmtPct(mt.efficiencePct)}`;
                                                return (
                                                    <tr key={mt.machineType}>
                                                        <td className="lc-mt-table-name">{mt.machineType}</td>
                                                        <td>{mt.groupe || '—'}</td>
                                                        <td>{mt.activeMachines}</td>
                                                        <td className="lc-mt-table-formula">{formula}</td>
                                                        <td className="lc-mt-table-locked">{this.fmtMin(mt.lockedRemainingMinutes)}</td>
                                                        <td className="lc-mt-table-pending">{this.fmtMin(mt.pendingRemainingMinutes)}</td>
                                                        <td className="lc-mt-table-load">
                                                            {this.loadBar(mt.loadPct, this.fmtPct(mt.loadPct))}
                                                        </td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>

                        <div className="lc-modal-section">
                            <h6
                                className="lc-modal-section-title lc-modal-title-clickable"
                                onClick={() => this.toggle('expandedLocked', zone.zoneNom)}
                            >
                                <FontAwesomeIcon icon={lockedOpen ? faChevronDown : faChevronRight} />
                                <FontAwesomeIcon icon={faLock} className="lc-icon-locked" />
                                &nbsp;Verrouillées ({zone.lockedSequences.length})
                            </h6>
                            {lockedOpen && (zone.lockedSequences.length === 0 ? (
                                <div className="lc-empty-seq">Aucune séquence verrouillée.</div>
                            ) : zone.lockedSequences.map(s => this.renderSequenceRow(s, zone)))}
                        </div>

                        <div className="lc-modal-section">
                            <h6
                                className="lc-modal-section-title lc-modal-title-clickable"
                                onClick={() => this.toggle('expandedPending', zone.zoneNom)}
                            >
                                <FontAwesomeIcon icon={pendingOpen ? faChevronDown : faChevronRight} />
                                <FontAwesomeIcon icon={faLockOpen} className="lc-icon-pending" />
                                &nbsp;Mobiles ({zone.pendingSequences.length})
                            </h6>
                            {pendingOpen && (zone.pendingSequences.length === 0 ? (
                                <div className="lc-empty-seq">Aucune séquence mobile.</div>
                            ) : zone.pendingSequences.map(s => this.renderSequenceRow(s, zone)))}
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    renderUnassigned() {
        const { data } = this.props;
        const list = data.unassigned || [];
        if (list.length === 0) return null;
        const open = this.state.unassignedExpanded;
        return (
            <div className="lc-unassigned-card">
                <div className="lc-unassigned-head"
                     onClick={() => this.setState({ unassignedExpanded: !open })}>
                    <FontAwesomeIcon icon={open ? faChevronDown : faChevronRight} />
                    <FontAwesomeIcon icon={faExclamationTriangle} className="lc-icon-warn" />
                    &nbsp;Sans zone effective ({list.length})
                </div>
                {open && (
                    <div className="lc-unassigned-body">
                        {list.map(s => this.renderSequenceRow(s, null))}
                    </div>
                )}
            </div>
        );
    }

    render() {
        const { data, loading } = this.props;
        if (loading && !data) {
            return <div className="lc-loading">Chargement…</div>;
        }
        if (!data) return null;
        return (
            <div className="lc-container">
                {this.renderHeader()}
                <div className="lc-zones">
                    {data.zones.map(z => this.renderZoneCard(z))}
                </div>
                {this.renderUnassigned()}
                {this.renderZoneModal()}
            </div>
        );
    }
}

export default LiveChargeView;
