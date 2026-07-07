import React, { Component } from 'react';
import axios from 'axios';
import moment from 'moment';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faRotate, faSpinner, faMagnifyingGlass, faCheckCircle,
    faTriangleExclamation, faBoxesStacked, faBan, faWandMagicSparkles,
} from '@fortawesome/free-solid-svg-icons';
import '../../styles/SequenceRectification.scss';

const STATUSES = [
    { value: 'IMPORTED', label: 'Importée', cls: 'imported' },
    { value: 'RELEASED', label: 'Released', cls: 'released' },
    { value: 'STARTED', label: 'En cours', cls: 'started' },
    { value: 'COMPLETED', label: 'Terminée', cls: 'completed' },
    { value: 'MATERIAL_MISSING', label: 'Matière manquante', cls: 'missing' },
    { value: 'INCOMPLETE', label: 'Incomplète', cls: 'incomplete' },
];

const STATUS_META = STATUSES.reduce((acc, s) => { acc[s.value] = s; return acc; }, {});

function statusMeta(status) {
    return STATUS_META[status] || { value: status, label: status || '—', cls: 'other' };
}

/** The cleanup case: every serie is cut but the sequence is still En cours. */
function isStuckStarted(row) {
    return row.status === 'STARTED' && row.seriesTotal > 0 && row.seriesDone >= row.seriesTotal;
}

/** Who wrote the zone — log = picklist /logisticsRelease (locked), chef = manual (locked), auto = inferred from the cutting table. */
const ZONE_SOURCE_META = {
    LOGISTICS: { tag: 'log', title: 'Zone fixée par la release logistique (verrouillée)' },
    CHEF: { tag: 'chef', title: 'Zone fixée manuellement par un chef (verrouillée)' },
    AUTO: { tag: 'auto', title: 'Zone déduite de la table de coupe de la dernière série (zone stricte)' },
};

class SequenceRectification extends Component {
    state = {
        days: 1,
        data: null,
        loading: false,
        error: null,
        filterZone: '',
        filterStatus: '',
        q: '',
        selected: {},
        busy: null,
    };

    componentDidMount() {
        this.load();
    }

    load = () => {
        this.setState({ loading: true, error: null });
        axios.get('/api/sequence/rectification', { params: { days: this.state.days } })
            .then(res => this.setState({ data: res.data, loading: false, selected: {} }))
            .catch(err => this.setState({
                loading: false,
                error: err.response?.data?.message || err.message || 'Erreur de chargement',
            }));
    };

    setDays = e => this.setState({ days: Number(e.target.value) }, this.load);

    /* ---------- filtering ---------- */

    filteredRows() {
        const rows = this.state.data?.rows || [];
        const { filterZone, filterStatus, q } = this.state;
        const needle = q.trim().toLowerCase();
        return rows.filter(r => {
            if (filterZone === 'NONE') {
                if (r.zone) return false;
            } else if (filterZone && r.zone !== filterZone) {
                return false;
            }
            if (filterStatus === 'STUCK') {
                if (!isStuckStarted(r)) return false;
            } else if (filterStatus && r.status !== filterStatus) {
                return false;
            }
            if (needle
                && !(r.sequence || '').toLowerCase().includes(needle)
                && !(r.projet || '').toLowerCase().includes(needle)) {
                return false;
            }
            return true;
        });
    }

    /* ---------- actions ---------- */

    rectify = (sequence, payload, label) => {
        if (!window.confirm(`Séquence ${sequence} : ${label} ?`)) return;
        this.setState({ busy: sequence });
        axios.post(`/api/sequence/${encodeURIComponent(sequence)}/rectify`, payload)
            .then(() => { this.setState({ busy: null }); this.load(); })
            .catch(err => {
                this.setState({ busy: null });
                alert(err.response?.data?.error || err.message || 'Action refusée');
            });
    };

    onStatusPick = (row, status) => {
        if (!status || status === row.status) return;
        const meta = statusMeta(status);
        this.rectify(row.sequence, { status }, `changer le statut en ${meta.label}`);
    };

    onZonePick = (row, zone) => {
        if (!zone || zone === row.zone) return;
        this.rectify(row.sequence, { zone }, `déplacer vers la zone ${zone}`);
    };

    toggleSelect = sequence => {
        this.setState(prev => {
            const selected = { ...prev.selected };
            if (selected[sequence]) delete selected[sequence];
            else selected[sequence] = true;
            return { selected };
        });
    };

    toggleSelectAll = rows => {
        const all = rows.length > 0 && rows.every(r => this.state.selected[r.sequence]);
        const selected = {};
        if (!all) rows.forEach(r => { selected[r.sequence] = true; });
        this.setState({ selected });
    };

    zoneAutofix = () => {
        if (!window.confirm('Recalculer maintenant les zones des séquences En cours / Terminées '
            + 'd\'après la table de coupe de leur dernière série ? '
            + '(les zones fixées par la logistique ou par un chef ne sont pas touchées)')) return;
        this.setState({ busy: 'AUTOFIX' });
        axios.post('/api/sequence/zone-autofix')
            .then(res => {
                this.setState({ busy: null });
                const d = res.data || {};
                alert(`Zones corrigées: ${d.corrected} (examinées: ${d.examined}, `
                    + `verrouillées: ${d.locked}, sans signal: ${d.noSignal})`);
                this.load();
            })
            .catch(err => {
                this.setState({ busy: null });
                alert(err.response?.data?.error || err.message || 'Action refusée');
            });
    };

    bulkComplete = () => {
        const sequences = Object.keys(this.state.selected);
        if (sequences.length === 0) return;
        if (!window.confirm(`Marquer ${sequences.length} séquence(s) TERMINÉE(S) ?`)) return;
        this.setState({ busy: 'BULK' });
        axios.post('/api/sequence/rectify-bulk', { sequences, status: 'COMPLETED' })
            .then(res => {
                this.setState({ busy: null });
                const d = res.data || {};
                if (d.failed > 0) {
                    const errs = (d.results || []).filter(r => !r.success)
                        .map(r => `${r.sequence}: ${r.error}`).join('\n');
                    alert(`Mises à jour: ${d.updated} — Échecs: ${d.failed}\n${errs}`);
                }
                this.load();
            })
            .catch(err => {
                this.setState({ busy: null });
                alert(err.response?.data?.error || err.message || 'Action refusée');
            });
    };

    /* ---------- rendering ---------- */

    renderStatusChips(rows) {
        const counts = {};
        rows.forEach(r => { counts[r.status] = (counts[r.status] || 0) + 1; });
        const stuck = rows.filter(isStuckStarted).length;
        const { filterStatus } = this.state;
        return (
            <div className="sr-chips">
                {STATUSES.map(s => (
                    <button
                        key={s.value}
                        className={`sr-chip sr-chip-${s.cls} ${filterStatus === s.value ? 'sr-chip-active' : ''}`}
                        onClick={() => this.setState({ filterStatus: filterStatus === s.value ? '' : s.value })}
                    >
                        {s.label} <b>{counts[s.value] || 0}</b>
                    </button>
                ))}
                <button
                    className={`sr-chip sr-chip-stuck ${filterStatus === 'STUCK' ? 'sr-chip-active' : ''}`}
                    title="Toutes les séries sont coupées mais la séquence est restée En cours"
                    onClick={() => this.setState({ filterStatus: filterStatus === 'STUCK' ? '' : 'STUCK' })}
                >
                    <FontAwesomeIcon icon={faTriangleExclamation} /> À clôturer <b>{stuck}</b>
                </button>
            </div>
        );
    }

    renderRow(row, zones, canEdit) {
        const meta = statusMeta(row.status);
        const busy = this.state.busy;
        const stuck = isStuckStarted(row);
        const seriesCls = row.seriesTotal > 0 && row.seriesDone >= row.seriesTotal ? 'sr-series-done' : 'sr-series-open';
        return (
            <tr key={row.sequence} className={stuck ? 'sr-row-stuck' : ''}>
                {canEdit && (
                    <td className="sr-td-check">
                        <input
                            type="checkbox"
                            checked={!!this.state.selected[row.sequence]}
                            onChange={() => this.toggleSelect(row.sequence)}
                        />
                    </td>
                )}
                <td className="sr-td-seq">{row.sequence}</td>
                <td>{row.projet || '—'}</td>
                <td>
                    <span className={`sr-badge sr-badge-${meta.cls}`}>{meta.label}</span>
                    {stuck && (
                        <span className="sr-stuck-hint" title="Toutes les séries sont coupées — à marquer Terminée">
                            <FontAwesomeIcon icon={faTriangleExclamation} />
                        </span>
                    )}
                </td>
                <td>
                    {row.zone || '—'}
                    {row.zone && ZONE_SOURCE_META[row.zoneSource] && (
                        <span className={`sr-zone-src sr-zone-src-${ZONE_SOURCE_META[row.zoneSource].tag}`}
                              title={ZONE_SOURCE_META[row.zoneSource].title}>
                            {ZONE_SOURCE_META[row.zoneSource].tag}
                        </span>
                    )}
                </td>
                <td>{row.planningDate ? moment(row.planningDate).format('DD/MM') : '—'}{row.shift ? ` (${row.shift})` : ''}</td>
                <td>{row.dueDate ? moment(row.dueDate).format('DD/MM') : '—'}{row.dueShift ? ` (${row.dueShift})` : ''}</td>
                <td className={seriesCls}>{row.seriesDone}/{row.seriesTotal}</td>
                <td><FontAwesomeIcon icon={faBoxesStacked} /> {row.boxes}</td>
                {canEdit && (
                    <td className="sr-td-actions">
                        <select
                            value=""
                            disabled={busy === row.sequence || busy === 'BULK'}
                            onChange={e => this.onStatusPick(row, e.target.value)}
                        >
                            <option value="">Statut…</option>
                            {STATUSES.filter(s => s.value !== row.status).map(s => (
                                <option key={s.value} value={s.value}>{s.label}</option>
                            ))}
                        </select>
                        <select
                            value=""
                            disabled={busy === row.sequence || busy === 'BULK'}
                            onChange={e => this.onZonePick(row, e.target.value)}
                        >
                            <option value="">Zone…</option>
                            {zones.filter(z => z !== row.zone).map(z => (
                                <option key={z} value={z}>{z}</option>
                            ))}
                        </select>
                        {busy === row.sequence && <FontAwesomeIcon icon={faSpinner} spin />}
                    </td>
                )}
            </tr>
        );
    }

    render() {
        const { data, loading, error, days, filterZone, q, selected, busy } = this.state;
        const zones = data?.zones || [];
        const canEdit = !!data?.rectifyEnabled;
        const rows = this.filteredRows();
        const zoneFiltered = (data?.rows || []).filter(r => {
            if (filterZone === 'NONE') return !r.zone;
            return !filterZone || r.zone === filterZone;
        });
        const selectedCount = Object.keys(selected).length;
        const allChecked = rows.length > 0 && rows.every(r => selected[r.sequence]);
        return (
            <div className="sr-container">
                <div className="sr-header">
                    <h2 className="sr-title">Rectification Séquences</h2>
                    <div className="sr-controls">
                        <div className="sr-control">
                            <label>Fenêtre</label>
                            <select className="sr-select" value={days} onChange={this.setDays}>
                                <option value={1}>1 jour</option>
                                <option value={2}>2 jours</option>
                                <option value={3}>3 jours</option>
                                <option value={7}>7 jours</option>
                                <option value={14}>14 jours</option>
                                <option value={30}>30 jours</option>
                            </select>
                        </div>
                        <div className="sr-control">
                            <label>Zone</label>
                            <select className="sr-select" value={filterZone} onChange={e => this.setState({ filterZone: e.target.value })}>
                                <option value="">Toutes</option>
                                <option value="NONE">Sans zone</option>
                                {zones.map(z => <option key={z} value={z}>{z}</option>)}
                            </select>
                        </div>
                        <div className="sr-control">
                            <label>Recherche</label>
                            <div className="sr-search">
                                <FontAwesomeIcon icon={faMagnifyingGlass} />
                                <input
                                    className="sr-input"
                                    placeholder="Séquence / projet"
                                    value={q}
                                    onChange={e => this.setState({ q: e.target.value })}
                                />
                            </div>
                        </div>
                        <button className="sr-refresh" onClick={this.load} disabled={loading}>
                            <FontAwesomeIcon icon={loading ? faSpinner : faRotate} spin={loading} /> Actualiser
                        </button>
                        {canEdit && (
                            <button
                                className="sr-refresh sr-autofix"
                                disabled={busy === 'AUTOFIX'}
                                title="Déduire les zones des séquences En cours / Terminées depuis la table de coupe de leur dernière série (zones strictes uniquement)"
                                onClick={this.zoneAutofix}
                            >
                                <FontAwesomeIcon icon={busy === 'AUTOFIX' ? faSpinner : faWandMagicSparkles} spin={busy === 'AUTOFIX'} /> Auto-zones
                            </button>
                        )}
                    </div>
                </div>

                {!canEdit && data && (
                    <div className="sr-banner">
                        <FontAwesomeIcon icon={faBan} /> La rectification est désactivée — affichage en lecture seule.
                    </div>
                )}
                {error && <div className="sr-error">{error}</div>}

                {data && this.renderStatusChips(zoneFiltered)}

                {canEdit && selectedCount > 0 && (
                    <div className="sr-bulkbar">
                        <span>{selectedCount} séquence(s) sélectionnée(s)</span>
                        <button className="sr-bulk-btn" disabled={busy === 'BULK'} onClick={this.bulkComplete}>
                            <FontAwesomeIcon icon={busy === 'BULK' ? faSpinner : faCheckCircle} spin={busy === 'BULK'} />
                            {' '}Marquer Terminée(s)
                        </button>
                    </div>
                )}

                <div className="sr-table-wrap">
                    <table className="sr-table">
                        <thead>
                            <tr>
                                {canEdit && (
                                    <th className="sr-td-check">
                                        <input type="checkbox" checked={allChecked} onChange={() => this.toggleSelectAll(rows)} />
                                    </th>
                                )}
                                <th>Séquence</th>
                                <th>Projet</th>
                                <th>Statut</th>
                                <th>Zone</th>
                                <th>Planif.</th>
                                <th>Due</th>
                                <th title="Séries coupées / total">Séries</th>
                                <th>Boîtes</th>
                                {canEdit && <th>Rectifier</th>}
                            </tr>
                        </thead>
                        <tbody>
                            {rows.map(r => this.renderRow(r, zones, canEdit))}
                            {!loading && rows.length === 0 && (
                                <tr><td colSpan={canEdit ? 10 : 8} className="sr-empty">Aucune séquence</td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }
}

export default SequenceRectification;
