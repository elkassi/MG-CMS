import React, { Component } from 'react';
import axios from 'axios';
import moment from 'moment';
import { connect } from 'react-redux';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faIndustry, faRotate, faSpinner, faPlay, faStop, faWrench,
    faPause, faSitemap, faBoxesStacked, faLayerGroup, faWarehouse,
    faClock, faScissors, faLocationCrosshairs, faBan,
    faCheckCircle, faExclamationTriangle, faExchangeAlt, faLock,
} from '@fortawesome/free-solid-svg-icons';
import '../../styles/ProductionFloor.scss';

const CHEF_ROLES = ['ROLE_CHEF_DE_ZONE', 'ROLE_CHEF_EQUIPE', 'ROLE_ADMIN'];

const SEQ_STATUS_META = {
    RELEASED: { cls: 'released', label: 'Released' },
    STARTED: { cls: 'started', label: 'En cours' },
    MATERIAL_MISSING: { cls: 'missing', label: 'Matière manquante' },
};

/** Plant shift, hour-based, matching RapportLectraV2.getShift. */
function computeCurrentShift(m) {
    const hour = m.hour();
    if (hour >= 0 && hour < 8) return 1;
    if (hour >= 8 && hour < 16) return 2;
    return 3;
}

/** Machine status -> { cls, icon, label } for the Lectra-style tile. */
const STATUS_META = {
    CUTTING: { cls: 'cutting', icon: faPlay, label: 'En coupe' },
    STOPPED: { cls: 'stopped', icon: faStop, label: 'Arrêt' },
    REPRAGE: { cls: 'reprage', icon: faWrench, label: 'Repérage' },
    IDLE: { cls: 'idle', icon: faPause, label: 'Inactif' },
    DOWN: { cls: 'down', icon: faBan, label: 'Hors service' },
};

function statusMeta(status) {
    return STATUS_META[status] || STATUS_META.IDLE;
}

class ProductionFloor extends Component {
    constructor(props) {
        super(props);
        const now = moment();
        this.state = {
            date: now.format('YYYY-MM-DD'),
            shift: computeCurrentShift(now),
            // Until the user explicitly picks a date/shift, let the backend
            // ShiftClock be the single source of truth (it rolls the date for
            // the late-night night shift, which the front end does not).
            userOverride: false,
            data: null,
            loading: false,
            error: null,
            jumpZone: '',
            expandedRacks: {},
            actionBusy: null,
        };
        this.boardRef = React.createRef();
        this.zoneRefs = {};
        this.inFlight = false;
    }

    componentDidMount() {
        this.load();
        this.refreshInterval = setInterval(this.load, 60000);
        document.addEventListener('visibilitychange', this.onVisibilityChange);
    }

    componentWillUnmount() {
        if (this.refreshInterval) clearInterval(this.refreshInterval);
        document.removeEventListener('visibilitychange', this.onVisibilityChange);
    }

    // Skip background polling in a hidden tab; reload immediately on return so
    // the operator sees fresh data without waiting for the next tick.
    onVisibilityChange = () => {
        if (!document.hidden) this.load();
    };

    load = () => {
        if (this.inFlight || document.hidden) return;
        this.inFlight = true;
        this.setState({ loading: true, error: null });
        const params = this.state.userOverride
            ? { date: this.state.date, shift: this.state.shift }
            : {};
        axios.get('/api/production/floor', { params })
            .then(res => this.setState({ data: res.data, loading: false }))
            .catch(err => this.setState({
                loading: false,
                error: err.response?.data?.message || err.message || 'Erreur de chargement',
            }))
            .finally(() => { this.inFlight = false; });
    };

    setDate = e => this.setState({ date: e.target.value, userOverride: true }, this.load);
    setShift = e => this.setState({ shift: Number(e.target.value), userOverride: true }, this.load);

    jumpToZone = e => {
        const zone = e.target.value;
        this.setState({ jumpZone: zone });
        const node = this.zoneRefs[zone];
        if (node && this.boardRef.current) {
            this.boardRef.current.scrollTo({
                left: node.offsetLeft - 16,
                top: node.offsetTop - 16,
                behavior: 'smooth',
            });
        }
    };

    toggleRack = key => {
        this.setState(prev => ({
            expandedRacks: { ...prev.expandedRacks, [key]: !prev.expandedRacks[key] },
        }));
    };

    fmtMeters(v) {
        return `${Number(v || 0).toFixed(0)} m`;
    }

    /* ---------- Totals strip ---------- */

    renderTotals() {
        const t = this.state.data?.totals;
        if (!t) return null;
        const cells = [
            { label: 'Zones', value: t.zoneCount || 0 },
            { label: 'Machines', value: t.machineCount || 0 },
            { label: 'En coupe', value: t.cuttingCount || 0, cls: 'pf-stat-ok' },
            { label: 'À l\'arrêt', value: t.stoppedCount || 0, cls: (t.stoppedCount > 0 ? 'pf-stat-warn' : '') },
            { label: 'Matelassage', value: t.spreadingCount || 0 },
        ];
        return (
            <div className="pf-summary">
                {cells.map(c => (
                    <div key={c.label} className={`pf-stat ${c.cls || ''}`}>
                        <strong>{c.value}</strong>
                        <span>{c.label}</span>
                    </div>
                ))}
            </div>
        );
    }

    /* ---------- Machine tile (Lectra-style status card) ---------- */

    renderMachine(machine) {
        const meta = statusMeta(machine.status);
        const queue = machine.queue || [];
        return (
            <div key={machine.nom} className={`pf-machine pf-machine-${meta.cls}`}>
                <div className="pf-machine-head">
                    <FontAwesomeIcon icon={meta.icon} />
                    <span className="pf-machine-name">{machine.nom}</span>
                    {machine.groupe && <span className="pf-machine-grp">{machine.groupe}</span>}
                </div>
                <div className="pf-machine-status">{meta.label}</div>
                {machine.currentSerie ? (
                    <div className="pf-machine-cur">
                        <div className="pf-cur-serie">
                            <FontAwesomeIcon icon={faScissors} /> {machine.currentSerie}
                            {machine.currentSequence && <small> · {machine.currentSequence}</small>}
                        </div>
                        {machine.currentMaterial && <div className="pf-cur-mat">{machine.currentMaterial}</div>}
                        {(machine.finishEtaClock || machine.finishEtaMinutes != null) && (
                            <div className="pf-cur-eta">
                                <FontAwesomeIcon icon={faClock} />
                                {machine.finishEtaClock
                                    ? ` ${machine.finishEtaClock}`
                                    : ` +${machine.finishEtaMinutes} min`}
                            </div>
                        )}
                    </div>
                ) : (
                    <div className="pf-machine-cur pf-machine-empty">—</div>
                )}
                {queue.length > 0 && (
                    <div className="pf-queue">
                        <div className="pf-queue-title">File ({queue.length})</div>
                        {queue.map((q, i) => (
                            <div key={`${q.serie}-${i}`} className="pf-queue-item">
                                <span className="pf-queue-pos">{q.position != null ? q.position : i + 1}</span>
                                <span className="pf-queue-serie">{q.serie}</span>
                                {q.material && <span className="pf-queue-mat">{q.material}</span>}
                                {q.longueur != null && <span className="pf-queue-len">{this.fmtMeters(q.longueur)}</span>}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        );
    }

    /* ---------- Spreading table tile ---------- */

    renderSpreadingTable(t, idx) {
        return (
            <div key={t.table || idx} className="pf-spread">
                <div className="pf-spread-head">
                    <FontAwesomeIcon icon={faLayerGroup} />
                    <span>{t.table}</span>
                    <em>matelassage en cours</em>
                </div>
                <div className="pf-spread-body">
                    {t.idRouleau && (
                        <div className="pf-spread-roll">
                            {t.idRouleau}
                            {t.reftissu && <small> · {t.reftissu}</small>}
                        </div>
                    )}
                    <div className="pf-spread-meta">
                        {t.lot && <span>Lot {t.lot}</span>}
                        {t.serie && <span><FontAwesomeIcon icon={faScissors} /> {t.serie}</span>}
                        {t.estimationRest != null && <span>{this.fmtMeters(t.estimationRest)} restant</span>}
                        {t.since && <span><FontAwesomeIcon icon={faClock} /> {moment(t.since).format('HH:mm')}</span>}
                    </div>
                </div>
            </div>
        );
    }

    /* ---------- Rack tile (expandable) ---------- */

    renderRack(zoneName, rack, idx) {
        const key = `${zoneName}::${rack.rack || idx}`;
        const open = !!this.state.expandedRacks[key];
        const rolls = rack.rolls || [];
        return (
            <div
                key={key}
                className={`pf-rack ${open ? 'pf-rack-open' : ''}`}
                onClick={() => this.toggleRack(key)}
                title="Cliquer pour voir les rouleaux"
            >
                <div className="pf-rack-head">
                    <FontAwesomeIcon icon={faWarehouse} />
                    <span className="pf-rack-name">{rack.rack}</span>
                </div>
                <div className="pf-rack-meta">
                    <span>{rack.rollCount || rolls.length} rlx</span>
                    <span>{this.fmtMeters(rack.meters)}</span>
                </div>
                {open && rolls.length > 0 && (
                    <div className="pf-rack-rolls">
                        {rolls.map((r, i) => (
                            <div key={`${r.serialId}-${i}`} className="pf-rack-roll">
                                <span className="pf-roll-id">{r.serialId}</span>
                                {r.reftissu && <span className="pf-roll-ref">{r.reftissu}</span>}
                                {r.metrage != null && <span className="pf-roll-m">{this.fmtMeters(r.metrage)}</span>}
                                {r.lot && <span className="pf-roll-lot">{r.lot}</span>}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        );
    }

    /* ---------- Sequences (chef rectification) ---------- */

    isChef() {
        const user = this.props.security && this.props.security.user;
        if (!user || !user.roles) return false;
        return user.roles.some(r => CHEF_ROLES.includes(r.authority));
    }

    /**
     * Chef corrections go through /rectify (not the plain lifecycle endpoints)
     * so the fix is also written to suiviplanning — otherwise the 20-min status
     * sync would revert it as soon as suiviplanning disagrees.
     */
    seqAction = (sequence, status, label) => {
        if (!window.confirm(`Séquence ${sequence} : ${label} ?`)) return;
        this.setState({ actionBusy: sequence });
        axios.post(`/api/sequence/${encodeURIComponent(sequence)}/rectify`, { status })
            .then(() => { this.setState({ actionBusy: null }); this.load(); })
            .catch(err => {
                this.setState({ actionBusy: null });
                alert(err.response?.data?.error || err.message || 'Action refusée');
            });
    };

    seqReassign = (sequence, zone, fromZone) => {
        if (!zone || zone === fromZone) return;
        if (!window.confirm(`Déplacer la séquence ${sequence} vers la zone ${zone} ?`)) return;
        this.setState({ actionBusy: sequence });
        axios.post(`/api/sequence/${encodeURIComponent(sequence)}/zone`, { zone })
            .then(() => { this.setState({ actionBusy: null }); this.load(); })
            .catch(err => {
                this.setState({ actionBusy: null });
                alert(err.response?.data?.error || err.message || 'Action refusée');
            });
    };

    renderSequences(zone) {
        const seqs = zone.sequences || [];
        if (seqs.length === 0) return null;
        const chef = this.isChef();
        const zones = (this.state.data?.zones || []).map(z => z.zone);
        const busy = this.state.actionBusy;
        return (
            <div className="pf-zone-area">
                <div className="pf-area-label">
                    <FontAwesomeIcon icon={faBoxesStacked} /> Séquences ({seqs.length})
                </div>
                <div className="pf-seqs">
                    {seqs.map(s => {
                        const meta = SEQ_STATUS_META[s.status] || { cls: 'other', label: s.status || '—' };
                        return (
                            <div key={s.sequence} className={`pf-seq pf-seq-${meta.cls}`}>
                                <div className="pf-seq-main">
                                    <span className="pf-seq-id">
                                        {s.locked && <FontAwesomeIcon icon={faLock} title={`Démarrée sur ${s.lockingTable || 'table'}`} />}
                                        {' '}{s.sequence}
                                    </span>
                                    <span className={`pf-seq-status pf-seq-status-${meta.cls}`}>{meta.label}</span>
                                    <span className="pf-seq-boxes" title="Boîtes occupées">
                                        <FontAwesomeIcon icon={faBoxesStacked} /> {s.boxCount}
                                    </span>
                                    {s.dueDate && <span className="pf-seq-due">{moment(s.dueDate).format('DD/MM')}</span>}
                                </div>
                                {chef && (
                                    <div className="pf-seq-actions">
                                        <button
                                            className="pf-seq-btn pf-seq-btn-ok"
                                            disabled={busy === s.sequence}
                                            title="Marquer terminée (libère les boîtes)"
                                            onClick={() => this.seqAction(s.sequence, 'COMPLETED', 'marquer TERMINÉE')}
                                        >
                                            <FontAwesomeIcon icon={faCheckCircle} /> Terminée
                                        </button>
                                        <button
                                            className="pf-seq-btn pf-seq-btn-warn"
                                            disabled={busy === s.sequence}
                                            title="Marquer incomplète (sort de la production)"
                                            onClick={() => this.seqAction(s.sequence, 'INCOMPLETE', 'marquer INCOMPLÈTE')}
                                        >
                                            <FontAwesomeIcon icon={faExclamationTriangle} /> Incomplète
                                        </button>
                                        <span className="pf-seq-move" title="Pas ma zone — déplacer vers…">
                                            <FontAwesomeIcon icon={faExchangeAlt} />
                                            <select
                                                value=""
                                                disabled={busy === s.sequence}
                                                onChange={e => this.seqReassign(s.sequence, e.target.value, zone.zone)}
                                            >
                                                <option value="">Zone…</option>
                                                {zones.filter(z => z !== zone.zone).map(z => (
                                                    <option key={z} value={z}>{z}</option>
                                                ))}
                                            </select>
                                        </span>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>
        );
    }

    /* ---------- Boxes occupancy bar ---------- */

    renderBoxes(boxes) {
        if (!boxes) return null;
        const pct = Math.max(0, Math.min(100, Number(boxes.pct || 0)));
        const danger = pct >= 90;
        return (
            <div className="pf-boxes">
                <div className="pf-boxes-label">
                    <FontAwesomeIcon icon={faBoxesStacked} />
                    <span>Boîtes</span>
                    <strong className={danger ? 'pf-boxes-full' : ''}>
                        {boxes.occupied || 0}/{boxes.capacity || 0}
                    </strong>
                </div>
                <div className="pf-boxes-track">
                    <div
                        className={`pf-boxes-fill ${danger ? 'pf-boxes-fill-full' : ''}`}
                        style={{ width: `${pct}%` }}
                    />
                </div>
            </div>
        );
    }

    /* ---------- Zone panel ---------- */

    renderZone(zone) {
        const machines = zone.machines || [];
        const tables = zone.spreadingTables || [];
        const racks = zone.racks || [];
        return (
            <section
                key={zone.zone}
                className="pf-zone"
                ref={node => { this.zoneRefs[zone.zone] = node; }}
            >
                <header className="pf-zone-head">
                    <FontAwesomeIcon icon={faSitemap} />
                    <h3>{zone.zone}</h3>
                    {zone.category && <span className="pf-zone-cat">{zone.category}</span>}
                </header>

                <div className="pf-zone-area">
                    <div className="pf-area-label"><FontAwesomeIcon icon={faIndustry} /> Machines</div>
                    <div className="pf-machines">
                        {machines.length > 0
                            ? machines.map(m => this.renderMachine(m))
                            : <div className="pf-area-empty">Aucune machine</div>}
                    </div>
                </div>

                <div className="pf-zone-area">
                    <div className="pf-area-label"><FontAwesomeIcon icon={faLayerGroup} /> Matelassage</div>
                    <div className="pf-spreads">
                        {tables.length > 0
                            ? tables.map((t, i) => this.renderSpreadingTable(t, i))
                            : <div className="pf-area-empty">Aucun matelassage</div>}
                    </div>
                </div>

                <div className="pf-zone-area">
                    <div className="pf-area-label"><FontAwesomeIcon icon={faWarehouse} /> Racks</div>
                    <div className="pf-racks">
                        {racks.length > 0
                            ? racks.map((r, i) => this.renderRack(zone.zone, r, i))
                            : <div className="pf-area-empty">Aucun rack</div>}
                    </div>
                </div>

                {this.renderSequences(zone)}
                {this.renderBoxes(zone.boxes)}
            </section>
        );
    }

    render() {
        const { data, loading, error, date, shift, jumpZone } = this.state;
        const zones = data?.zones || [];
        return (
            <div className="pf-container">
                <div className="pf-header">
                    <h2 className="pf-title">
                        <FontAwesomeIcon icon={faLocationCrosshairs} style={{ marginRight: 8 }} />
                        Vue Production — Plan d'usine
                    </h2>
                    <div className="pf-controls">
                        <div className="pf-control">
                            <label>Date</label>
                            <input type="date" value={date} onChange={this.setDate} className="pf-input" />
                        </div>
                        <div className="pf-control">
                            <label>Shift</label>
                            <select value={shift} onChange={this.setShift} className="pf-select">
                                <option value={1}>1 (Nuit)</option>
                                <option value={2}>2 (Matin)</option>
                                <option value={3}>3 (Après-midi)</option>
                            </select>
                        </div>
                        {zones.length > 0 && (
                            <div className="pf-control">
                                <label>Aller à la zone</label>
                                <select value={jumpZone} onChange={this.jumpToZone} className="pf-select">
                                    <option value="">— Zone —</option>
                                    {zones.map(z => (
                                        <option key={z.zone} value={z.zone}>{z.zone}</option>
                                    ))}
                                </select>
                            </div>
                        )}
                        <button className="pf-refresh" onClick={this.load} disabled={loading} title="Rafraîchir">
                            <FontAwesomeIcon icon={loading ? faSpinner : faRotate} spin={loading} />
                        </button>
                    </div>
                </div>

                {data?.generatedAt && (
                    <div className="pf-generated">
                        Mis à jour {moment(data.generatedAt).format('DD/MM/YYYY HH:mm:ss')} · actualisation auto 60s
                    </div>
                )}

                {error && <div className="pf-error">{error}</div>}
                {this.renderTotals()}

                {loading && !data && (
                    <div className="pf-empty">
                        <FontAwesomeIcon icon={faSpinner} spin /> Chargement du plan d'usine…
                    </div>
                )}

                {!loading && zones.length === 0 && !error && (
                    <div className="pf-empty">Aucune zone à afficher pour ce créneau.</div>
                )}

                {zones.length > 0 && (
                    <div className="pf-board" ref={this.boardRef}>
                        {zones.map(z => this.renderZone(z))}
                    </div>
                )}
            </div>
        );
    }
}

const mapStateToProps = state => ({
    security: state.security,
});

export default connect(mapStateToProps)(ProductionFloor);
