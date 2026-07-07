import React, { Component } from 'react';
import axios from 'axios';
import moment from 'moment';
import ReactToPrint from 'react-to-print';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faTruck, faRotate, faSpinner, faCheckCircle,
    faExclamationTriangle, faArrowRight, faSitemap,
    faPrint, faClipboardCheck, faXmark, faBoxesStacked,
    faWarehouse, faArrowRightArrowLeft, faRotateLeft, faClock,
    faCircle, faLayerGroup, faScaleBalanced,
} from '@fortawesome/free-solid-svg-icons';
import '../../styles/LogisticsRelease.scss';

/** Current plant shift, matching the backend ShiftClock boundaries. */
function computeCurrentShift() {
    const minutes = moment().hour() * 60 + moment().minute();
    if (minutes >= 1310) return 1;
    if (minutes < 350) return 1;
    if (minutes < 830) return 2;
    return 3;
}

const MATERIAL_LABEL = {
    OK: 'Matière OK',
    OUT_OF_ZONE: 'Hors zone',
    SHORTAGE: 'Manque',
    NONE: 'Aucun stock',
};

const RECAP_LABEL = {
    OK: 'OK',
    COVERED: 'Couvert magasin',
    SHORTAGE: 'Insuffisant',
};

/** The staged page load — one real backend call per step, run in order. */
const LOAD_STEPS = [
    { key: 'sequences', label: 'Séquences à sortir' },
    { key: 'racks', label: 'Rouleaux en rack & affectations' },
    { key: 'magasin', label: 'Stock magasin (matières manquantes)' },
];

class LogisticsRelease extends Component {
    constructor(props) {
        super(props);
        this.state = {
            date: moment().format('YYYY-MM-DD'),
            shift: computeCurrentShift(),
            data: null,
            sequencesPreview: null,
            magasinByMaterial: {},
            steps: {},
            loading: false,
            error: null,
            selected: {},
            modalSeq: null,
            recap: null,
            recapLoading: false,
            committing: false,
            commitResult: null,
            cellDetail: null,
        };
        this.loadToken = 0;
        this.printRef = React.createRef();
    }

    componentDidMount() {
        this.load();
    }

    load = () => {
        const token = ++this.loadToken;
        this.setState({
            loading: true,
            error: null,
            data: null,
            sequencesPreview: null,
            magasinByMaterial: {},
            selected: {},
            recap: null,
            commitResult: null,
            steps: { sequences: 'active', racks: 'pending', magasin: 'pending' },
        });
        this.runLoad(token);
    };

    /**
     * Staged load: one real call per step, in order, so the user sees progress.
     * Stage 1 (sequences) and stage 3 (magasin) are best-effort — only stage 2
     * (the full advice) is authoritative and surfaces an error if it fails.
     */
    runLoad = async (token) => {
        const params = { date: this.state.date, shift: this.state.shift };
        const fresh = () => this.loadToken === token;

        // Stage 1 — sequences (instant feedback while the advice computes).
        try {
            const res = await axios.get('/api/logistics/release/candidates/sequences', { params });
            if (!fresh()) return;
            this.setState({ sequencesPreview: res.data });
        } catch (e) { /* non-fatal: skip the preview */ }
        if (!fresh()) return;
        this.setState(prev => ({ steps: { ...prev.steps, sequences: 'done', racks: 'active' } }));

        // Stage 2 — full advice (sequences + rack stock + zone assignments + overview).
        let data;
        try {
            const res = await axios.get('/api/logistics/release/candidates', { params });
            if (!fresh()) return;
            data = res.data;
            this.setState(prev => ({ data, steps: { ...prev.steps, racks: 'done', magasin: 'active' } }));
        } catch (err) {
            if (!fresh()) return;
            this.setState(prev => ({
                loading: false,
                error: err.response?.data?.message || err.message || 'Erreur de chargement',
                steps: { ...prev.steps, racks: 'error' },
            }));
            return;
        }

        // Stage 3 — magasin (R100) for the materials the overview flags as short.
        const short = (data.materials || [])
            .filter(m => m.status && m.status !== 'OK')
            .map(m => m.refTissus);
        if (short.length > 0) {
            try {
                const res = await axios.get('/api/logistics/release/candidates/magasin', {
                    params: { materials: short.join(',') },
                });
                if (!fresh()) return;
                this.setState({ magasinByMaterial: res.data || {} });
            } catch (e) { /* non-fatal: overview shows without magasin */ }
        }
        if (!fresh()) return;
        this.setState(prev => ({ loading: false, steps: { ...prev.steps, magasin: 'done' } }));
    };

    setDate = e => this.setState({ date: e.target.value }, this.load);
    setShift = e => this.setState({ shift: Number(e.target.value) }, this.load);

    selectedSequences() {
        return Object.keys(this.state.selected).filter(seq => this.state.selected[seq]);
    }

    afterSelectionChange = () => {
        const sequences = this.selectedSequences();
        if (sequences.length === 0) {
            this.setState({ recap: null, recapLoading: false });
            return;
        }
        this.setState({ recapLoading: true });
        axios.post('/api/logistics/release/recap', { sequences })
            .then(res => this.setState({ recap: res.data, recapLoading: false }))
            .catch(err => this.setState({
                recapLoading: false,
                error: err.response?.data?.error || err.response?.data?.message || err.message || 'Erreur du récapitulatif',
            }));
    };

    toggleSequence = sequence => {
        this.setState(prev => ({
            selected: { ...prev.selected, [sequence]: !prev.selected[sequence] },
            commitResult: null,
        }), this.afterSelectionChange);
    };

    selectZone = zone => {
        const sequences = this.state.data?.sequences || [];
        this.setState(prev => {
            const selected = { ...prev.selected };
            sequences.forEach(seq => {
                if (seq.recommendation === 'ASSIGN' && seq.recommendedZone === zone && !seq.materialMissingSuggested) {
                    selected[seq.sequence] = true;
                }
            });
            return { selected, commitResult: null };
        }, this.afterSelectionChange);
    };

    commitSelected = () => {
        const sequences = this.selectedSequences();
        if (sequences.length === 0 || this.state.committing) return;
        if (!this.state.recap?.canConfirm) return;
        this.setState({ committing: true, error: null, commitResult: null });
        axios.post('/api/logistics/release/commit', {
            date: this.state.date,
            shift: this.state.shift,
            sequences,
        })
            .then(res => {
                if (res.data && res.data.success === false) {
                    this.setState({
                        committing: false,
                        error: res.data.error || 'Erreur de confirmation',
                    });
                    return;
                }
                this.setState({ committing: false, commitResult: res.data });
            })
            .catch(err => this.setState({
                committing: false,
                error: err.response?.data?.error || err.response?.data?.message || err.message || 'Erreur de confirmation',
            }));
    };

    /**
     * Load the last persisted picklist for the selected date/shift back into
     * commitResult (the same state the printable picklist reads) so an operator
     * interrupted before printing can still reprint it. The refresh-safe trail
     * lives server-side; commitResult is wiped by load()/date-click.
     */
    reprintLast = () => {
        const params = { date: this.state.date, shift: this.state.shift };
        axios.get('/api/logistics/release/picklist/last', { params })
            .then(res => {
                if (!res.data || res.data.success === false) {
                    this.setState({ error: res.data?.error || 'Aucune picklist enregistrée pour ce créneau' });
                    return;
                }
                this.setState({ commitResult: res.data, error: null });
            })
            .catch(err => this.setState({
                error: err.response?.data?.error || err.response?.data?.message || err.message || 'Erreur de réimpression',
            }));
    };

    openModal = seq => this.setState({ modalSeq: seq });
    closeModal = () => this.setState({ modalSeq: null });

    /**
     * Open the "how is this number calculated" modal for one recap cell.
     * Nouveau / Engagé / Dispo / Restant read the breakdown embedded in the recap
     * row; Magasin is loaded lazily so R100.prn is read only when opened.
     */
    openCellDetail = (m, metric, label) => {
        const material = m.refTissus;
        if (metric === 'magasinMeters') {
            // Prefer the stock preloaded at page load (stage 3); only hit R100 if absent.
            const cached = this.state.magasinByMaterial[material];
            if (cached) {
                this.setState({ cellDetail: { refTissus: material, metric, label, loading: false, rows: cached.rolls || [], total: cached.totalMeters || 0 } });
                return;
            }
            this.setState({ cellDetail: { refTissus: material, metric, label, loading: true, rows: [], total: 0 } });
            axios.get('/api/logistics/release/recap/magasin', { params: { material } })
                .then(res => this.setState(prev => (
                    prev.cellDetail && prev.cellDetail.refTissus === material && prev.cellDetail.metric === metric
                        ? { cellDetail: { ...prev.cellDetail, loading: false, rows: res.data.rolls || [], total: res.data.totalMeters || 0 } }
                        : null
                )))
                .catch(err => this.setState(prev => (
                    prev.cellDetail && prev.cellDetail.refTissus === material && prev.cellDetail.metric === metric
                        ? { cellDetail: { ...prev.cellDetail, loading: false, error: err.response?.data?.message || err.message || 'Erreur magasin' } }
                        : null
                )));
            return;
        }
        if (metric === 'overviewAvailable') {
            this.setState({ cellDetail: { refTissus: material, metric: 'availableRolls', label, loading: false, rows: m.rolls || [] } });
            return;
        }
        if (metric === 'overviewNeeded') {
            this.setState({ cellDetail: { refTissus: material, metric, label, loading: false, data: this.neededRowsFor(material) } });
            return;
        }
        this.setState({ cellDetail: { refTissus: material, metric, label, loading: false, data: (m.breakdown || {})[metric] } });
    };

    /** Series demanding a material, drawn from the cached sequence data (preview or full advice). */
    neededRowsFor(material) {
        const seqs = (this.state.sequencesPreview && this.state.sequencesPreview.sequences)
            || (this.state.data && this.state.data.sequences) || [];
        const rows = [];
        seqs.forEach(seq => {
            (seq.series || []).forEach(s => {
                if (s.refTissus === material) {
                    rows.push({ sequence: seq.sequence, serie: s.serie, neededMeters: s.neededMeters });
                }
            });
        });
        return rows;
    }

    closeCellDetail = () => this.setState({ cellDetail: null });

    materialLabel(status) {
        return MATERIAL_LABEL[status] || 'Matière —';
    }

    fmtMeters(v) {
        return `${Number(v || 0).toFixed(0)} m`;
    }

    /* ---------- Staged loading ---------- */

    renderLoadStepper() {
        if (!this.state.loading) return null;
        const { steps } = this.state;
        return (
            <div className="lr-stepper">
                {LOAD_STEPS.map(step => {
                    const st = steps[step.key] || 'pending';
                    const icon = st === 'done' ? faCheckCircle
                        : st === 'active' ? faSpinner
                            : st === 'error' ? faExclamationTriangle
                                : faCircle;
                    return (
                        <div key={step.key} className={`lr-step lr-step-${st}`}>
                            <FontAwesomeIcon icon={icon} spin={st === 'active'} />
                            <span>{step.label}</span>
                        </div>
                    );
                })}
            </div>
        );
    }

    renderSummary() {
        const t = this.state.data && this.state.data.totals;
        if (!t) return null;
        const short = Number(t.materialShort || 0);
        const overloaded = Number(t.zonesOverloaded || 0);
        const cards = [
            { label: 'Séquences', value: t.candidateCount || 0 },
            { label: 'Prêtes (matière OK)', value: t.materialReady || 0, ok: true },
            { label: 'Matières manquantes', value: short, warn: short > 0 },
            { label: 'Zones surchargées', value: overloaded, warn: overloaded > 0 },
            { label: 'Matières', value: t.materialCount || 0 },
        ];
        return (
            <div className="lr-summary">
                {cards.map(c => (
                    <div key={c.label} className={`lr-stat ${c.ok ? 'lr-stat-ok' : ''} ${c.warn ? 'lr-stat-warn' : ''}`}>
                        <span>{c.label}</span>
                        <strong>{c.value}</strong>
                    </div>
                ))}
            </div>
        );
    }

    /* ---------- Zone summary strip ---------- */

    renderZones() {
        const zones = this.state.data?.zones || [];
        if (zones.length === 0) return null;
        return (
            <div className="lr-zones">
                {zones.map(z => {
                    const boxFull = Number(z.boxOccupancyPct || 0) >= 100;
                    const danger = z.overloaded || boxFull;
                    return (
                        <div key={z.zone} className={`lr-zone-chip ${danger ? 'lr-zone-over' : ''}`}>
                            <div className="lr-zone-chip-head">
                                <FontAwesomeIcon icon={faSitemap} />
                                <strong>{z.zone}</strong>
                                {z.category && <span className="lr-zone-cat">{z.category}</span>}
                            </div>
                            <div className="lr-zone-chip-meta">
                                <span>Charge {Number(z.loadPct || 0).toFixed(0)}%</span>
                                <span className={boxFull ? 'lr-zone-box-full' : ''}>
                                    <FontAwesomeIcon icon={faBoxesStacked} /> {z.occupiedBoxes || 0}/{z.boxCapacity || 0}
                                </span>
                                <span>Marge {Number(z.headroomMinutes || 0).toFixed(0)} min</span>
                            </div>
                        </div>
                    );
                })}
            </div>
        );
    }

    /* ---------- Per-machine-type charge balance table (strict zones) ---------- */

    renderBalanceTable() {
        const zones = (this.state.data?.zones || []).filter(
            z => (z.category === 'STRICT' || z.category == null) && (z.byMachineType || []).length > 0);
        if (zones.length === 0) return null;

        // Stable union of machine types present across the strict zones.
        const types = [];
        zones.forEach(z => (z.byMachineType || []).forEach(t => {
            if (t.machineType && !types.includes(t.machineType)) types.push(t.machineType);
        }));
        if (types.length === 0) return null;

        const cellOf = (z, type) => (z.byMachineType || []).find(t => t.machineType === type);
        const pct = v => `${Number(v || 0).toFixed(0)}%`;
        const dmin = v => { const n = Number(v || 0); return `${n > 0 ? '+' : ''}${n.toFixed(0)} min`; };

        // Per machine type, the projected load spread across strict zones — the
        // chef's at-a-glance verdict on whether the recommended release balances it.
        const BALANCE_THRESHOLD = 15; // load-% points
        const balance = types.map(t => {
            const vals = zones.map(z => cellOf(z, t)).filter(Boolean)
                .map(c => Number(c.projectedLoadPct || 0));
            if (vals.length < 2) return { type: t, spread: 0, ok: true };
            const spread = Math.max(...vals) - Math.min(...vals);
            return { type: t, spread, ok: spread <= BALANCE_THRESHOLD };
        });

        return (
            <div className="lr-balance">
                <div className="lr-balance-head">
                    <FontAwesomeIcon icon={faScaleBalanced} />
                    <strong>Équilibrage des charges — zones strictes</strong>
                    <span className="lr-balance-sub">
                        Charge actuelle → projetée si la séquence recommandée est respectée
                    </span>
                </div>
                <div className="lr-balance-summary">
                    {balance.map(b => (
                        <span key={b.type} className={`lr-balance-chip ${b.ok ? 'lr-balance-chip--ok' : 'lr-balance-chip--warn'}`}>
                            <FontAwesomeIcon icon={faScaleBalanced} /> {b.type} : {b.ok ? 'équilibré' : 'déséquilibré'} (écart {b.spread.toFixed(0)}%)
                        </span>
                    ))}
                </div>
                <div className="lr-balance-wrap">
                    <table className="lr-balance-table">
                        <thead>
                            <tr>
                                <th rowSpan={2}>Zone</th>
                                <th rowSpan={2}>Bacs</th>
                                {types.map(t => <th key={t} colSpan={3}>{t}</th>)}
                            </tr>
                            <tr>
                                {types.map(t => ([
                                    <th key={`${t}-a`}>Actuelle</th>,
                                    <th key={`${t}-p`}>Projetée</th>,
                                    <th key={`${t}-d`}>Δ</th>,
                                ]))}
                            </tr>
                        </thead>
                        <tbody>
                            {zones.map(z => (
                                <tr key={z.zone}>
                                    <td className="lr-balance-zone">{z.zone}</td>
                                    <td className={Number(z.boxOccupancyPct || 0) >= 100 ? 'lr-zone-box-full' : ''}>
                                        {z.occupiedBoxes || 0}/{z.boxCapacity || 0}
                                    </td>
                                    {types.map(t => {
                                        const c = cellOf(z, t);
                                        if (!c) return ([
                                            <td key={`${t}-a`} className="lr-balance-na">—</td>,
                                            <td key={`${t}-p`} className="lr-balance-na">—</td>,
                                            <td key={`${t}-d`} className="lr-balance-na">—</td>,
                                        ]);
                                        const over = Number(c.projectedLoadPct || 0) > 100;
                                        const delta = Number(c.deltaMinutes || 0);
                                        return ([
                                            <td key={`${t}-a`}>{pct(c.currentLoadPct)}</td>,
                                            <td key={`${t}-p`} className={over ? 'lr-balance-over' : ''}>{pct(c.projectedLoadPct)}</td>,
                                            <td key={`${t}-d`} className={delta > 0 ? 'lr-balance-up' : ''}>{delta !== 0 ? dmin(delta) : '—'}</td>,
                                        ]);
                                    })}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }

    /* ---------- Quick zone selection actions ---------- */

    renderQuickActions() {
        const sequences = this.state.data?.sequences || [];
        const zones = [];
        sequences.forEach(seq => {
            if (seq.recommendation === 'ASSIGN' && seq.recommendedZone && !zones.includes(seq.recommendedZone)) {
                zones.push(seq.recommendedZone);
            }
        });
        if (zones.length === 0) return null;
        return (
            <div className="lr-quick">
                <span className="lr-quick-label">Sélection rapide:</span>
                {zones.map(z => (
                    <button key={z} className="lr-quick-btn" onClick={() => this.selectZone(z)}>
                        Tout pour zone {z}
                    </button>
                ))}
            </div>
        );
    }

    /* ---------- Compact sequence table ---------- */

    renderRecoChip(seq) {
        if (seq.recommendation === 'ASSIGN') {
            return (
                <span className="lr-reco lr-reco-assign">
                    <FontAwesomeIcon icon={faArrowRight} /> Zone {seq.recommendedZone || seq.suggestedZone || '—'}
                </span>
            );
        }
        return (
            <span className="lr-reco lr-reco-wait">
                <FontAwesomeIcon icon={faClock} /> Attendre
            </span>
        );
    }

    renderSequenceRow(seq) {
        const statusClass = String(seq.materialStatus || 'OK').toLowerCase();
        const checked = !!this.state.selected[seq.sequence];
        const isWait = seq.recommendation === 'WAIT';
        const disabled = !!seq.materialMissingSuggested || isWait;
        const disabledTitle = seq.materialMissingSuggested
            ? 'Matière insuffisante: à passer MATERIAL_MISSING'
            : (seq.recommendationReason || 'Recommandation: attendre');
        return (
            <tr
                key={seq.sequence}
                className={`lr-row lr-row-${statusClass} ${checked ? 'lr-row-selected' : ''}`}
                onClick={() => this.openModal(seq)}
            >
                <td className="lr-row-check" onClick={e => e.stopPropagation()}>
                    <input
                        type="checkbox"
                        checked={checked}
                        disabled={disabled}
                        onChange={() => this.toggleSequence(seq.sequence)}
                        title={disabled ? disabledTitle : 'Sélectionner pour la picklist'}
                    />
                </td>
                <td className="lr-row-seq">
                    {seq.materialMissingSuggested && <span className="lr-warn-dot" title="Matière manquante" />}
                    <strong>{seq.sequence}</strong>
                </td>
                <td>{this.renderRecoChip(seq)}</td>
                <td className="lr-row-due">
                    {seq.dueDate ? moment(seq.dueDate).format('DD/MM') : '—'}
                    {seq.dueShift ? <small> · S{seq.dueShift}</small> : null}
                </td>
                <td>
                    <span className={`lr-status lr-status-${statusClass}`}>
                        <FontAwesomeIcon icon={seq.materialStatus === 'OK' ? faCheckCircle : faExclamationTriangle} />
                        {this.materialLabel(seq.materialStatus)}
                    </span>
                </td>
                <td className="lr-row-reason">{seq.recommendationReason || '—'}</td>
            </tr>
        );
    }

    renderSequenceTable() {
        const sequences = this.state.data?.sequences || [];
        if (sequences.length === 0) return null;
        return (
            <div className="lr-card">
                <div className="lr-card-head">
                    <FontAwesomeIcon icon={faTruck} />
                    <span>Séquences à sortir</span>
                </div>
                <div className="lr-table-wrap">
                    <table className="lr-table lr-seq-table">
                        <thead>
                            <tr>
                                <th />
                                <th>Séquence</th>
                                <th>Zone conseillée</th>
                                <th>Échéance</th>
                                <th>Matière</th>
                                <th>Raison</th>
                            </tr>
                        </thead>
                        <tbody>
                            {sequences.map(seq => this.renderSequenceRow(seq))}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }

    /* ---------- Stage-1 preview & materials overview ---------- */

    renderSequencesPreview() {
        const preview = this.state.sequencesPreview;
        if (!preview || this.state.data) return null;
        const seqs = preview.sequences || [];
        if (seqs.length === 0) return null;
        return (
            <div className="lr-card">
                <div className="lr-card-head">
                    <FontAwesomeIcon icon={faTruck} />
                    <span>Séquences à sortir ({preview.count != null ? preview.count : seqs.length})</span>
                    <span className="lr-card-hint"><FontAwesomeIcon icon={faSpinner} spin /> calcul des affectations…</span>
                </div>
                <div className="lr-table-wrap">
                    <table className="lr-table">
                        <thead>
                            <tr><th>Séquence</th><th>Échéance</th><th>Machines</th><th>Séries</th><th>Besoin</th></tr>
                        </thead>
                        <tbody>
                            {seqs.map(s => (
                                <tr key={s.sequence}>
                                    <td className="lr-mat-ref">{s.sequence}</td>
                                    <td>
                                        {s.dueDate ? moment(s.dueDate).format('DD/MM') : '—'}
                                        {s.dueShift ? <small> · S{s.dueShift}</small> : null}
                                    </td>
                                    <td>{(s.machines || []).join(', ') || '—'}</td>
                                    <td>{s.serieCount}</td>
                                    <td>{this.fmtMeters(s.neededMeters)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }

    renderMaterialsOverview() {
        const materials = (this.state.data && this.state.data.materials) || [];
        if (materials.length === 0) return null;
        return (
            <div className="lr-card">
                <div className="lr-card-head">
                    <FontAwesomeIcon icon={faLayerGroup} />
                    <span>Matières — vue d'ensemble ({materials.length})</span>
                </div>
                <div className="lr-table-wrap">
                    <table className="lr-table">
                        <thead>
                            <tr>
                                <th>Matière</th>
                                <th>Besoin</th>
                                <th>Dispo rack</th>
                                <th>Déficit</th>
                                <th>Statut</th>
                                <th>Séq.</th>
                                <th>Zones</th>
                                <th>Magasin</th>
                            </tr>
                        </thead>
                        <tbody>
                            {materials.map(m => this.renderOverviewRow(m))}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }

    renderOverviewRow(m) {
        const sc = String(m.status || '').toLowerCase();
        const isShort = m.status && m.status !== 'OK';
        const mag = this.state.magasinByMaterial[m.refTissus];
        const statusLabel = m.status === 'NONE' ? 'Aucun stock' : (RECAP_LABEL[m.status] || m.status || '—');
        return (
            <tr key={m.refTissus}>
                <td className="lr-mat-ref">{m.refTissus}</td>
                <td>{this.renderCell(m, 'overviewNeeded', m.neededMeters, `Besoin · ${m.refTissus}`)}</td>
                <td>{this.renderCell(m, 'overviewAvailable', m.availableTotal, `Dispo rack · ${m.refTissus}`)}</td>
                <td>{this.fmtMeters(m.deficit)}</td>
                <td><span className={`lr-status lr-status-${sc}`}>{statusLabel}</span></td>
                <td>{m.sequenceCount}</td>
                <td className="lr-overview-zones">{(m.zones || []).join(', ') || '—'}</td>
                <td className="lr-recap-mag">
                    {isShort
                        ? (
                            <button
                                type="button"
                                className="lr-cell-btn"
                                onClick={() => this.openCellDetail(m, 'magasinMeters', `Magasin · ${m.refTissus}`)}
                                title="Stock magasin (R100)"
                            >
                                {mag ? this.fmtMeters(mag.totalMeters) : <><FontAwesomeIcon icon={faWarehouse} /> Voir</>}
                            </button>
                        )
                        : '—'}
                </td>
            </tr>
        );
    }

    /* ---------- Series modal ---------- */

    renderRollPlacements(placements) {
        if (!placements || placements.length === 0) {
            return <span className="lr-roll-none">Aucun rouleau disponible</span>;
        }
        return (
            <div className="lr-rolls">
                {placements.map(r => (
                    <span key={r.serialId} className={`lr-roll ${r.inTargetZone ? '' : 'lr-roll-transfer'}`}>
                        {r.serialId} · {r.rack}
                        {!r.inTargetZone && r.sourceZone && (
                            <em> <FontAwesomeIcon icon={faArrowRight} /> {r.sourceZone}</em>
                        )}
                        {r.transferAfterUse && (
                            <em> puis <FontAwesomeIcon icon={faArrowRight} /> {r.transferAfterUse}</em>
                        )}
                        <small>{this.fmtMeters(r.meters)}</small>
                    </span>
                ))}
            </div>
        );
    }

    renderModal() {
        const seq = this.state.modalSeq;
        if (!seq) return null;
        return (
            <div className="lr-modal-overlay" onClick={this.closeModal}>
                <div className="lr-modal" onClick={e => e.stopPropagation()}>
                    <div className="lr-modal-head">
                        <div className="lr-modal-title">
                            <strong>{seq.sequence}</strong>
                            <span className="lr-seq-zone">
                                <FontAwesomeIcon icon={faSitemap} /> {seq.recommendedZone || seq.suggestedZone || '—'}
                            </span>
                            {this.renderRecoChip(seq)}
                        </div>
                        <button className="lr-modal-close" onClick={this.closeModal} title="Fermer">
                            <FontAwesomeIcon icon={faXmark} />
                        </button>
                    </div>
                    {seq.recommendationReason && (
                        <div className="lr-modal-reason">{seq.recommendationReason}</div>
                    )}
                    <div className="lr-modal-body">
                        {(seq.series || []).map(s => (
                            <div key={s.serie} className="lr-serie">
                                <div className="lr-serie-head">
                                    <span className="lr-mat-ref">{s.serie}</span>
                                    <span>{s.machine}</span>
                                    <span className="lr-mat-ref">{s.refTissus}</span>
                                    <span>{this.fmtMeters(s.neededMeters)}</span>
                                    <span className="lr-serie-zone">{s.targetZone}</span>
                                    <span className={`lr-status lr-status-${String(s.materialStatus || '').toLowerCase()}`}>
                                        {this.materialLabel(s.materialStatus)}
                                    </span>
                                </div>
                                {this.renderRollPlacements(s.rollPlacements)}
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        );
    }

    /* ---------- Recap cell drill-down ---------- */

    renderCell(m, metric, value, label) {
        return (
            <button
                type="button"
                className="lr-cell-btn"
                onClick={() => this.openCellDetail(m, metric, label)}
                title="Voir le détail du calcul"
            >
                {this.fmtMeters(value)}
            </button>
        );
    }

    renderCellDetailModal() {
        const cd = this.state.cellDetail;
        if (!cd) return null;
        return (
            <div className="lr-modal-overlay" onClick={this.closeCellDetail}>
                <div className="lr-modal lr-detail-modal" onClick={e => e.stopPropagation()}>
                    <div className="lr-modal-head">
                        <div className="lr-modal-title">
                            <strong>{cd.label}</strong>
                            <span className="lr-seq-zone">
                                <span className="lr-mat-ref">{cd.refTissus}</span>
                            </span>
                        </div>
                        <button className="lr-modal-close" onClick={this.closeCellDetail} title="Fermer">
                            <FontAwesomeIcon icon={faXmark} />
                        </button>
                    </div>
                    <div className="lr-modal-body">
                        {this.renderCellDetailBody(cd)}
                    </div>
                </div>
            </div>
        );
    }

    renderCellDetailBody(cd) {
        if (cd.loading) {
            return (
                <div className="lr-detail-loading">
                    <FontAwesomeIcon icon={faSpinner} spin /> Lecture du stock magasin (R100)…
                </div>
            );
        }
        if (cd.error) return <div className="lr-error">{cd.error}</div>;
        if (cd.metric === 'remainingMeters') return this.renderRemainingDetail(cd.data);
        if (cd.metric === 'magasinMeters') return this.renderRollDetail(cd.rows, cd.total, 'magasin');
        if (cd.metric === 'availableMeters') return this.renderRollDetail(cd.data, null, 'available');
        if (cd.metric === 'availableRolls') return this.renderRollDetail(cd.rows, null, 'available');
        if (cd.metric === 'overviewNeeded') return this.renderNeededDetail(cd.data);
        return this.renderDemandDetail(cd.data);
    }

    renderNeededDetail(rows) {
        rows = rows || [];
        if (rows.length === 0) return <div className="lr-detail-empty">Aucune série ne demande cette matière.</div>;
        const total = rows.reduce((s, r) => s + Number(r.neededMeters || 0), 0);
        return (
            <table className="lr-table lr-detail-table">
                <thead>
                    <tr><th>Séquence</th><th>Série</th><th>Besoin</th></tr>
                </thead>
                <tbody>
                    {rows.map((r, i) => (
                        <tr key={`${r.serie}-${i}`}>
                            <td>{r.sequence}</td>
                            <td className="lr-mat-ref">{r.serie}</td>
                            <td>{this.fmtMeters(r.neededMeters)}</td>
                        </tr>
                    ))}
                </tbody>
                <tfoot><tr><td colSpan={2}>Total</td><td>{this.fmtMeters(total)}</td></tr></tfoot>
            </table>
        );
    }

    renderDemandDetail(rows) {
        rows = rows || [];
        if (rows.length === 0) return <div className="lr-detail-empty">Aucune série ne contribue à ce total.</div>;
        const total = rows.reduce((s, r) => s + Number(r.meters || 0), 0);
        return (
            <table className="lr-table lr-detail-table">
                <thead>
                    <tr><th>Séquence</th><th>Série</th><th>Longueur</th><th>Couches</th><th>Mètres</th></tr>
                </thead>
                <tbody>
                    {rows.map((r, i) => (
                        <tr key={`${r.serie}-${i}`}>
                            <td>{r.sequence}</td>
                            <td className="lr-mat-ref">{r.serie}</td>
                            <td>{Number(r.longueur || 0).toFixed(2)} m</td>
                            <td>× {r.nbrCouche}</td>
                            <td>{this.fmtMeters(r.meters)}</td>
                        </tr>
                    ))}
                </tbody>
                <tfoot><tr><td colSpan={4}>Total</td><td>{this.fmtMeters(total)}</td></tr></tfoot>
            </table>
        );
    }

    renderRollDetail(rows, total, kind) {
        rows = rows || [];
        if (rows.length === 0) {
            return (
                <div className="lr-detail-empty">
                    {kind === 'magasin'
                        ? 'Aucun rouleau AVAIL2 en magasin (R100) pour cette matière.'
                        : 'Aucun rouleau disponible pour cette matière.'}
                </div>
            );
        }
        const sum = total != null ? total : rows.reduce((s, r) => s + Number(r.meters || 0), 0);
        return (
            <table className="lr-table lr-detail-table">
                <thead>
                    <tr>
                        <th>Id Rouleau</th>
                        <th>Emplacement</th>
                        <th>{kind === 'magasin' ? 'Statut' : 'Source'}</th>
                        <th>{kind === 'magasin' ? 'Métrage' : 'Mètres'}</th>
                    </tr>
                </thead>
                <tbody>
                    {rows.map((r, i) => (
                        <tr key={`${r.serialId}-${i}`}>
                            <td className="lr-mat-ref">{r.serialId}</td>
                            <td>{r.location}</td>
                            <td>{kind === 'magasin' ? r.status : (r.source === 'ON_TABLE' ? 'Table' : 'Rack')}</td>
                            <td>{this.fmtMeters(r.meters)}</td>
                        </tr>
                    ))}
                </tbody>
                <tfoot><tr><td colSpan={3}>Total</td><td>{this.fmtMeters(sum)}</td></tr></tfoot>
            </table>
        );
    }

    renderRemainingDetail(data) {
        data = data || {};
        return (
            <div className="lr-detail-formula">
                <div className="lr-detail-formula-row"><span>Dispo</span><strong>{this.fmtMeters(data.availableMeters)}</strong></div>
                <div className="lr-detail-formula-op">−</div>
                <div className="lr-detail-formula-row"><span>Engagé</span><strong>{this.fmtMeters(data.committedMeters)}</strong></div>
                <div className="lr-detail-formula-op">−</div>
                <div className="lr-detail-formula-row"><span>Nouveau</span><strong>{this.fmtMeters(data.newMeters)}</strong></div>
                <div className="lr-detail-formula-op">=</div>
                <div className="lr-detail-formula-row lr-detail-formula-result"><span>Restant</span><strong>{this.fmtMeters(data.remainingMeters)}</strong></div>
            </div>
        );
    }

    /* ---------- Recap panel ---------- */

    renderMagasinPull(pull) {
        if (!pull || pull.length === 0) return null;
        return (
            <div className="lr-pull">
                {pull.map((p, i) => (
                    <span key={`${p.location}-${i}`} className="lr-pull-item">
                        <FontAwesomeIcon icon={faWarehouse} /> {p.location} · {this.fmtMeters(p.qty)}
                    </span>
                ))}
            </div>
        );
    }

    renderRecap() {
        const { recap, recapLoading } = this.state;
        if (recapLoading) {
            return (
                <div className="lr-card lr-recap">
                    <div className="lr-card-head">
                        <FontAwesomeIcon icon={faSpinner} spin />
                        <span>Calcul de la matière nécessaire…</span>
                    </div>
                </div>
            );
        }
        if (!recap) return null;
        const materials = recap.materials || [];
        const totals = recap.totals || {};
        return (
            <div className="lr-card lr-recap">
                <div className="lr-card-head">
                    <FontAwesomeIcon icon={faTruck} />
                    <span>Matière nécessaire ({recap.selectedCount || 0} séquence(s))</span>
                </div>
                <div className={`lr-recap-banner ${recap.canConfirm ? 'lr-recap-ok' : 'lr-recap-bad'}`}>
                    <FontAwesomeIcon icon={recap.canConfirm ? faCheckCircle : faExclamationTriangle} />
                    <span>{recap.canConfirm ? 'Confirmable' : 'Matière insuffisante en magasin'}</span>
                    <em>
                        {totals.materialsOk || 0} OK · {totals.materialsCovered || 0} couvert(s) · {totals.materialsShort || 0} insuffisant(s)
                    </em>
                </div>
                <div className="lr-table-wrap">
                    <table className="lr-table">
                        <thead>
                            <tr>
                                <th>Matière</th>
                                <th>Nouveau</th>
                                <th>Engagé</th>
                                <th>Dispo</th>
                                <th>Restant</th>
                                <th>Status</th>
                                <th>Magasin</th>
                            </tr>
                        </thead>
                        <tbody>
                            {materials.map(m => {
                                const sc = String(m.status || '').toLowerCase();
                                return (
                                    <tr key={m.refTissus}>
                                        <td className="lr-mat-ref">{m.refTissus}</td>
                                        <td>{this.renderCell(m, 'newMeters', m.newMeters, 'Nouveau')}</td>
                                        <td>{this.renderCell(m, 'committedMeters', m.committedMeters, 'Engagé')}</td>
                                        <td>{this.renderCell(m, 'availableMeters', m.availableMeters, 'Dispo')}</td>
                                        <td>{this.renderCell(m, 'remainingMeters', m.remainingMeters, 'Restant')}</td>
                                        <td>
                                            <span className={`lr-status lr-status-${sc}`}>
                                                {RECAP_LABEL[m.status] || m.status || '—'}
                                            </span>
                                        </td>
                                        <td className="lr-recap-mag">
                                            <button
                                                type="button"
                                                className="lr-cell-btn"
                                                onClick={() => this.openCellDetail(m, 'magasinMeters', 'Magasin')}
                                                title="Voir le stock magasin (R100)"
                                            >
                                                {m.status === 'OK'
                                                    ? <><FontAwesomeIcon icon={faWarehouse} /> Voir</>
                                                    : this.fmtMeters(m.magasinMeters)}
                                            </button>
                                            {m.status !== 'OK' && this.renderMagasinPull(m.magasinPull)}
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }

    /* ---------- Action bar ---------- */

    renderActions() {
        const count = this.selectedSequences().length;
        const { committing, commitResult, recap } = this.state;
        const canConfirm = count > 0 && !!recap?.canConfirm;
        return (
            <div className="lr-actions">
                <div className="lr-actions-meta">
                    <strong>{count}</strong>
                    <span>séquence{count > 1 ? 's' : ''} sélectionnée{count > 1 ? 's' : ''}</span>
                    {commitResult && (
                        <em>Picklist {commitResult.picklistId} confirmée</em>
                    )}
                </div>
                <div className="lr-actions-buttons">
                    <button
                        className="lr-primary"
                        onClick={this.commitSelected}
                        disabled={!canConfirm || committing}
                        title={!canConfirm ? 'Sélection vide ou matière insuffisante' : 'Confirmer la sortie'}
                    >
                        <FontAwesomeIcon icon={committing ? faSpinner : faClipboardCheck} spin={committing} />
                        <span>Confirmer la sortie</span>
                    </button>
                    <button
                        className="lr-secondary"
                        onClick={this.reprintLast}
                        title="Charger la dernière picklist enregistrée pour ce créneau, puis l'imprimer"
                    >
                        <FontAwesomeIcon icon={faRotateLeft} />
                        <span>Réimprimer dernière picklist</span>
                    </button>
                    <ReactToPrint
                        trigger={() => (
                            <button className="lr-secondary" disabled={!commitResult}>
                                <FontAwesomeIcon icon={faPrint} />
                                <span>Imprimer</span>
                            </button>
                        )}
                        content={() => this.printRef.current}
                        bodyClass="lr-printing"
                    />
                </div>
            </div>
        );
    }

    renderCommitNotice() {
        const result = this.state.commitResult;
        if (!result) return null;
        return (
            <div className="lr-commit-ok">
                <FontAwesomeIcon icon={faCheckCircle} />
                <span>
                    Picklist {result.picklistId} confirmée: {result.sequences?.length || 0} séquence(s),
                    {` ${result.suiviUpdatedRows || 0}`} suivi mis à jour,
                    {` ${result.allocationCount || 0}`} allocation(s) matière.
                </span>
            </div>
        );
    }

    /* ---------- Printable picklist ---------- */

    renderPrintPicklist() {
        const result = this.state.commitResult;
        const sequences = result?.sequences || [];
        const magasinPull = result?.magasinPull || [];
        const returns = result?.returnToMagasin || [];
        const transfers = result?.transferAfterUse || [];
        const fillPlan = result?.fillPlan || [];
        return (
            <div className="lr-print" ref={this.printRef}>
                <div className="lr-print-head">
                    <strong>LEAR Corporation</strong>
                    <h1>Picklist Logistique</h1>
                    <div>
                        <span>ID: {result?.picklistId || '—'}</span>
                        <span>Date: {this.state.date}</span>
                        <span>Shift: {this.state.shift}</span>
                        <span>Imprimé: {moment().format('DD/MM/YYYY HH:mm')}</span>
                    </div>
                </div>

                {sequences.map(seq => (
                    <div key={seq.sequence} className="lr-print-seq">
                        <h2>{seq.sequence} · Zone {seq.recommendedZone || seq.suggestedZone || '—'}</h2>
                        <table>
                            <thead>
                                <tr>
                                    <th>Série</th>
                                    <th>Matière</th>
                                    <th>Besoin</th>
                                    <th>Rouleaux à sortir</th>
                                </tr>
                            </thead>
                            <tbody>
                                {(seq.series || []).map(serie => (
                                    <tr key={serie.serie}>
                                        <td>{serie.serie}</td>
                                        <td>{serie.refTissus}</td>
                                        <td>{this.fmtMeters(serie.neededMeters)}</td>
                                        <td>
                                            {(serie.rollPlacements || []).map(r => (
                                                <div key={r.serialId}>
                                                    {r.serialId} · {r.rack} · {this.fmtMeters(r.meters)}
                                                    {!r.inTargetZone && r.sourceZone ? ` · transfert ${r.sourceZone} → ${serie.targetZone}` : ''}
                                                    {r.transferAfterUse ? ` · puis ${r.transferAfterUse}` : ''}
                                                </div>
                                            ))}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                ))}

                {magasinPull.length > 0 && (
                    <div className="lr-print-block">
                        <h2>Sortie magasin (FIFO)</h2>
                        <table>
                            <thead>
                                <tr>
                                    <th>Matière</th>
                                    <th>Emplacement</th>
                                    <th>Quantité</th>
                                </tr>
                            </thead>
                            <tbody>
                                {magasinPull.map((p, i) => (
                                    <tr key={`pull-${i}`}>
                                        <td>{p.ref || p.refTissus || '—'}</td>
                                        <td>{p.location}</td>
                                        <td>{this.fmtMeters(p.qty)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {fillPlan.length > 0 && (
                    <div className="lr-print-block">
                        <h2>Plan de remplissage</h2>
                        <table>
                            <thead>
                                <tr>
                                    <th>Matière</th>
                                    <th>Rouleau</th>
                                    <th>Emplacement</th>
                                    <th>Zone</th>
                                    <th>Quantité</th>
                                </tr>
                            </thead>
                            <tbody>
                                {fillPlan.map((f, i) => (
                                    <tr key={`fill-${i}`}>
                                        <td>{f.refTissus || f.ref || '—'}</td>
                                        <td>{f.serialId || '—'}</td>
                                        <td>{f.rack || f.location || '—'}</td>
                                        <td>{f.zone || f.targetZone || '—'}</td>
                                        <td>{this.fmtMeters(f.meters || f.qty)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {transfers.length > 0 && (
                    <div className="lr-print-block">
                        <h2>Transferts après usage</h2>
                        <table>
                            <thead>
                                <tr>
                                    <th>Matière</th>
                                    <th>Rouleau</th>
                                    <th>Emplacement</th>
                                    <th>Utiliser en</th>
                                    <th>Transférer vers</th>
                                    <th>Quantité</th>
                                </tr>
                            </thead>
                            <tbody>
                                {transfers.map((t, i) => (
                                    <tr key={`xfer-${i}`}>
                                        <td>{t.refTissus}</td>
                                        <td>{t.serialId}</td>
                                        <td>{t.rack}</td>
                                        <td>{t.useInZone || t.currentZone || '—'}</td>
                                        <td>{t.transferAfterUseTo || '—'}</td>
                                        <td>{this.fmtMeters(t.meters)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {returns.length > 0 && (
                    <div className="lr-print-block">
                        <h2>Retours magasin</h2>
                        <table>
                            <thead>
                                <tr>
                                    <th>Matière</th>
                                    <th>Rouleau</th>
                                    <th>Emplacement</th>
                                    <th>Zone</th>
                                    <th>Quantité</th>
                                </tr>
                            </thead>
                            <tbody>
                                {returns.map((r, i) => (
                                    <tr key={`ret-${i}`}>
                                        <td>{r.refTissus}</td>
                                        <td>{r.serialId}</td>
                                        <td>{r.rack}</td>
                                        <td>{r.zone || '—'}</td>
                                        <td>{this.fmtMeters(r.meters)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                <div className="lr-print-footer">
                    <span>Logistique: ____________________</span>
                    <span>Production: ____________________</span>
                    <span>Heure: __________</span>
                </div>
            </div>
        );
    }

    render() {
        const { data, loading, error, date, shift } = this.state;
        const sequences = data?.sequences || [];
        return (
            <div className="lr-container">
                <div className="lr-header">
                    <h2 className="lr-title">
                        <FontAwesomeIcon icon={faTruck} style={{ marginRight: 8 }} />
                        Préparation / Release Logistique
                    </h2>
                    <div className="lr-controls">
                        <div className="lr-control">
                            <label>Date</label>
                            <input type="date" value={date} onChange={this.setDate} className="lr-input" />
                        </div>
                        <div className="lr-control">
                            <label>Shift</label>
                            <select value={shift} onChange={this.setShift} className="lr-select">
                                <option value={1}>1 (Nuit)</option>
                                <option value={2}>2 (Matin)</option>
                                <option value={3}>3 (Après-midi)</option>
                            </select>
                        </div>
                        <button className="lr-refresh" onClick={this.load} disabled={loading} title="Rafraîchir">
                            <FontAwesomeIcon icon={loading ? faSpinner : faRotate} spin={loading} />
                        </button>
                    </div>
                </div>

                {error && <div className="lr-error">{error}</div>}
                {this.renderCommitNotice()}
                {this.renderLoadStepper()}

                {this.renderSummary()}
                {this.renderZones()}
                {this.renderBalanceTable()}
                {this.renderActions()}
                {this.renderQuickActions()}
                {this.renderSequencesPreview()}
                {this.renderSequenceTable()}
                {this.renderMaterialsOverview()}

                {!loading && data && sequences.length === 0 && (
                    <div className="lr-empty">Aucune séquence à sortir pour ce créneau.</div>
                )}

                {this.renderRecap()}

                {this.renderModal()}
                {this.renderCellDetailModal()}
                {this.renderPrintPicklist()}
            </div>
        );
    }
}

export default LogisticsRelease;
