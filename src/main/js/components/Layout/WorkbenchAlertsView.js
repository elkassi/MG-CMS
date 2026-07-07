import React, { Component } from 'react';
import moment from 'moment';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faTriangleExclamation, faBoxesStacked, faLock, faTruckRampBox,
    faCheckCircle, faSitemap,
} from '@fortawesome/free-solid-svg-icons';

const MATERIAL_MISSING = new Set(['SHORTAGE', 'NONE', 'OUT_OF_ZONE']);

function materialLabel(status) {
    switch (status) {
        case 'OK': return 'Matière OK';
        case 'OUT_OF_ZONE': return 'Hors zone';
        case 'SHORTAGE': return 'Manque';
        case 'NONE': return 'Aucun stock';
        case 'RETURN_TO_STOCK': return 'Retour stock';
        default: return 'Matière —';
    }
}

/**
 * Action alerts for the workbench, derived from {@code data.liveCharge}:
 *  - Material-missing: sequences whose material is short / out-of-zone / absent.
 *  - To close: sequences whose every serie is cut (Complete) but the sequence is
 *    still open — these freeze their boxes (rectify to Terminée).
 *  - Finish first: locked sequences (a serie is physically running on a STRICT
 *    table) that should be completed before opening new work in the zone.
 */
class WorkbenchAlertsView extends Component {
    /** All sequences across zones (locked + pending) + unassigned, deduped. */
    collectSequences() {
        const lc = this.props.data?.liveCharge;
        if (!lc) return [];
        const zoneFilter = this.props.zoneFilter || 'All';
        const out = new Map();
        const push = seq => {
            if (!seq || !seq.sequence) return;
            if (zoneFilter !== 'All' && seq.effectiveZone !== zoneFilter) return;
            if (!out.has(seq.sequence)) out.set(seq.sequence, seq);
        };
        (lc.zones || []).forEach(z => {
            (z.lockedSequences || []).forEach(push);
            (z.pendingSequences || []).forEach(push);
        });
        (lc.unassigned || []).forEach(push);
        return [...out.values()];
    }

    isStuck(seq) {
        const series = seq.series || [];
        return series.length > 0 && series.every(s => String(s.statusCoupe) === 'Complete');
    }

    fmtDue(value) {
        return value ? moment(value).format('DD/MM') : '—';
    }

    renderSeqRow(seq, kind) {
        const stuck = this.isStuck(seq);
        return (
            <div key={`${kind}-${seq.sequence}`} className="wb-al-row">
                <div className="wb-al-row-main">
                    <strong>{seq.sequence}</strong>
                    <span className="wb-al-zone">
                        <FontAwesomeIcon icon={faSitemap} /> {seq.effectiveZone || '—'}
                    </span>
                    {seq.locked && (
                        <span className="wb-al-tag wb-al-tag-lock" title={seq.lockReason || 'Verrouillée'}>
                            <FontAwesomeIcon icon={faLock} /> Verrou
                        </span>
                    )}
                </div>
                <div className="wb-al-row-meta">
                    <span>{(seq.series || []).length} séries</span>
                    <span>Due {this.fmtDue(seq.dueDate)}</span>
                    {kind === 'material' && (
                        <span className={`wb-al-mat wb-al-mat-${String(seq.materialStatus || '').toLowerCase()}`}>
                            {materialLabel(seq.materialStatus)}
                        </span>
                    )}
                    {kind === 'close' && stuck && (
                        <span className="wb-al-mat wb-al-mat-stuck">
                            <FontAwesomeIcon icon={faBoxesStacked} /> Toutes séries coupées — à clôturer
                        </span>
                    )}
                </div>
            </div>
        );
    }

    renderPanel(title, icon, cls, rows, kind, empty) {
        return (
            <div className={`wb-al-panel ${cls}`}>
                <div className="wb-al-panel-head">
                    <FontAwesomeIcon icon={icon} />
                    <span>{title}</span>
                    <strong>{rows.length}</strong>
                </div>
                <div className="wb-al-list">
                    {rows.length > 0
                        ? rows.map(seq => this.renderSeqRow(seq, kind))
                        : <div className="wb-al-empty"><FontAwesomeIcon icon={faCheckCircle} /> {empty}</div>}
                </div>
            </div>
        );
    }

    render() {
        const sequences = this.collectSequences();
        if (!this.props.data?.liveCharge) {
            return <div className="wb-al-empty">Alertes indisponibles pour ce créneau.</div>;
        }

        const material = sequences.filter(s => MATERIAL_MISSING.has(String(s.materialStatus)));
        const toClose = sequences.filter(s => this.isStuck(s));
        const finishFirst = sequences.filter(s => s.locked && !this.isStuck(s));

        return (
            <div className="wb-al-view">
                {this.renderPanel('Matière manquante', faTruckRampBox, 'wb-al-panel-danger',
                    material, 'material', 'Aucun manque matière signalé')}
                {this.renderPanel('À clôturer (séries coupées)', faBoxesStacked, 'wb-al-panel-warn',
                    toClose, 'close', 'Aucune séquence à clôturer')}
                {this.renderPanel('Prioriser / finir (verrouillées)', faLock, 'wb-al-panel-info',
                    finishFirst, 'finish', 'Aucune séquence verrouillée à finir')}
            </div>
        );
    }
}

export default WorkbenchAlertsView;
