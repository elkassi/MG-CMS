import axios from 'axios';
import moment from 'moment';
import React, { Component } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faCheckCircle, faTimesCircle, faSync, faSave, faExclamationTriangle,
    faSitemap, faIndustry, faEye, faUndo, faMagic,
} from '@fortawesome/free-solid-svg-icons';
import './styles/ChefDeZoneConfirm.scss';

/**
 * Chef-de-zone shift confirmation page.
 *
 * Pre-fills the machine list from {@code EtatMachineHistorique} (status M
 * or null = up). Chef sees all machines in their zone with current status
 * code, adjusts the checkboxes, confirms once.
 *
 * PROCESS / ADMIN can open the page in read-only mode (canWrite = false
 * from the server). The Confirmer button is disabled and any zone in
 * /api/zone/list is selectable.
 */

const STATUS_CONFIG = {
    M:   { name: 'Marche',         color: '#198754', text: '#fff' },
    A:   { name: 'Arrêt',          color: '#a5a5a5', text: '#fff' },
    P:   { name: 'PM',             color: '#ffc107', text: '#000' },
    O:   { name: 'OFF',            color: '#212529', text: '#fff' },
    R:   { name: 'Récupération',   color: '#e2efda', text: '#000' },
    AD:  { name: 'Arrêt Prod',     color: '#dc3545', text: '#fff' },
    ADM: { name: 'Arrêt Maint',    color: '#fd7e14', text: '#fff' },
    PN:  { name: 'En panne',       color: '#c00000', text: '#fff' },
};

export default class ChefDeZoneConfirm extends Component {

    constructor(props) {
        super(props);
        this.state = {
            date: moment().format('YYYY-MM-DD'),
            shift: this.guessShift(),
            zoneNom: '',
            myZones: [],          // chef's owned zones
            allZones: [],         // fallback for PROCESS/ADMIN
            machines: [],         // [{machineNom, machineType, isUp, codeEtat, currentStatusUp}]
            preview: true,        // is this a synthesised pre-fill (no confirmation yet)?
            canWrite: false,      // server-derived (CHEF_DE_ZONE | CHEF_EQUIPE)
            confirmedAt: null,
            confirmedByMatricule: null,
            loading: false,
            saving: false,
            info: null,
            error: null,
        };
    }

    componentDidMount() {
        this.bootstrap();
    }

    /** Best-guess current shift from local time (1=night, 2=morning, 3=afternoon). */
    guessShift = () => {
        const h = moment().hour();
        if (h >= 6  && h < 14) return 2;
        if (h >= 14 && h < 22) return 3;
        return 1;
    };

    bootstrap = async () => {
        try {
            const meRes = await axios.get('/api/userZone/me');
            const my = (meRes.data && meRes.data.zones) || [];
            const def = (meRes.data && meRes.data.defaultZone)
                || (my.length > 0 ? my[0].nom : '');
            // PROCESS/ADMIN often have no userZone — load the global zone list as fallback.
            let all = [];
            if (my.length === 0) {
                try {
                    const allRes = await axios.get('/api/zone/list');
                    all = (allRes.data || []).filter(z => z && z.active !== false);
                } catch (_) { /* tolerate missing endpoint */ }
            }
            this.setState({
                myZones: my,
                allZones: all,
                zoneNom: def || (all.length > 0 ? all[0].nom : ''),
            }, this.reload);
        } catch (err) {
            if (err.response && err.response.status === 404) {
                this.setState({ info: 'Dispatcher désactivé.' });
            } else {
                this.setState({ error: this.errMsg(err, 'Échec chargement.') });
            }
        }
    };

    reload = () => {
        const { date, shift, zoneNom } = this.state;
        if (!zoneNom) {
            this.setState({ machines: [], preview: true, confirmedAt: null });
            return;
        }
        this.setState({ loading: true, error: null });
        axios.get(`/api/zone/confirmation/${encodeURIComponent(zoneNom)}/orPreview`,
                  { params: { date, shift } })
            .then(res => {
                const d = res.data || {};
                this.setState({
                    machines: d.machines || [],
                    preview: !!d.preview,
                    canWrite: !!d.canWrite,
                    confirmedAt: d.confirmedAt || null,
                    confirmedByMatricule: d.confirmedByMatricule || null,
                    loading: false,
                });
            })
            .catch(err => {
                if (err.response && err.response.status === 404) {
                    this.setState({ loading: false, info: 'Dispatcher désactivé.' });
                } else {
                    this.setState({ loading: false, error: this.errMsg(err, 'Échec /orPreview.') });
                }
            });
    };

    onField = (field) => (e) => {
        const value = field === 'shift' ? Number(e.target.value) : e.target.value;
        this.setState({ [field]: value }, () => {
            if (['date', 'shift', 'zoneNom'].includes(field)) this.reload();
        });
    };

    toggleLocal = (machineNom) => {
        if (!this.state.canWrite) return;
        this.setState({
            machines: this.state.machines.map(m =>
                m.machineNom === machineNom ? { ...m, isUp: !m.isUp } : m),
        });
    };

    bulkAll = (value) => {
        if (!this.state.canWrite) return;
        this.setState({
            machines: this.state.machines.map(m => ({ ...m, isUp: value })),
        });
    };

    resetToCurrentStatus = () => {
        if (!this.state.canWrite) return;
        this.setState({
            machines: this.state.machines.map(m => ({ ...m, isUp: m.currentStatusUp })),
        });
    };

    confirm = () => {
        const { date, shift, zoneNom, machines, canWrite } = this.state;
        if (!canWrite || !zoneNom) return;
        const upMachineNoms = machines.filter(m => m.isUp).map(m => m.machineNom);
        this.setState({ saving: true, info: null, error: null });
        axios.post('/api/zone/confirm', { date, shift, zoneNom, upMachineNoms })
            .then(() => {
                this.setState({ saving: false, info: 'Zone confirmée.' });
                this.reload();
            })
            .catch(err => this.setState({
                saving: false,
                error: this.errMsg(err, 'Échec de la confirmation.'),
            }));
    };

    // --------------------------------------------------------------- render

    renderStatusBadge = (codeEtat) => {
        if (!codeEtat) {
            return <span className="cz-status cz-status-none" title="Aucun statut — considéré actif">—</span>;
        }
        const conf = STATUS_CONFIG[codeEtat] || { name: codeEtat, color: '#6c757d', text: '#fff' };
        return (
            <span className="cz-status"
                style={{ background: conf.color, color: conf.text }}
                title={conf.name}>
                {codeEtat}
            </span>
        );
    };

    renderHeader = () => {
        const { date, shift, zoneNom, myZones, allZones, preview,
                confirmedAt, confirmedByMatricule, canWrite } = this.state;
        const zonesForSelect = myZones.length > 0 ? myZones : allZones;
        return (
            <>
                <div className="cz-title-row">
                    <h3>
                        <FontAwesomeIcon icon={faSitemap} /> Confirmer la zone pour le shift
                    </h3>
                    {!canWrite && (
                        <span className="cz-readonly-badge" title="Vous n'êtes pas chef de zone — lecture seule">
                            <FontAwesomeIcon icon={faEye} /> Lecture seule
                        </span>
                    )}
                </div>

                <div className="cz-controls">
                    <div className="cz-control">
                        <label htmlFor="czc-date">Date</label>
                        <input id="czc-date" type="date" className="form-control"
                            value={date} onChange={this.onField('date')} />
                    </div>
                    <div className="cz-control">
                        <label htmlFor="czc-shift">Shift</label>
                        <select id="czc-shift" className="form-control"
                            value={shift} onChange={this.onField('shift')}>
                            <option value={1}>1 — Nuit</option>
                            <option value={2}>2 — Matin</option>
                            <option value={3}>3 — Après-midi</option>
                        </select>
                    </div>
                    <div className="cz-control cz-zone-control">
                        <label htmlFor="czc-zone">Zone</label>
                        <select id="czc-zone" className="form-control"
                            value={zoneNom} onChange={this.onField('zoneNom')}>
                            <option value="">—</option>
                            {zonesForSelect.map(z => (
                                <option key={z.nom} value={z.nom}>
                                    {z.nom} {z.category ? `[${z.category}]` : ''}
                                </option>
                            ))}
                        </select>
                    </div>
                    <button className="btn btn-outline-secondary"
                        onClick={this.reload}>
                        <FontAwesomeIcon icon={faSync} /> Rafraîchir
                    </button>
                </div>

                <div className={`cz-state-banner ${preview ? 'cz-state-preview' : 'cz-state-confirmed'}`}>
                    {preview ? (
                        <>
                            <FontAwesomeIcon icon={faExclamationTriangle} />
                            <b>Aperçu</b> — aucune confirmation enregistrée pour ce shift.
                            Les coches sont pré-remplies à partir de l'état machines (M ou sans statut = actif).
                        </>
                    ) : (
                        <>
                            <FontAwesomeIcon icon={faCheckCircle} />
                            <b>Confirmée</b> par <i>{confirmedByMatricule || '—'}</i>{' '}
                            le {confirmedAt ? moment(confirmedAt).format('DD/MM/YYYY HH:mm') : '—'}.
                            Modifiez et appuyez sur <em>Confirmer</em> pour mettre à jour.
                        </>
                    )}
                </div>
            </>
        );
    };

    renderTable = () => {
        const { machines, loading, canWrite } = this.state;
        if (loading) return <p className="cz-loading">Chargement…</p>;
        if (machines.length === 0) {
            return <p className="cz-empty">Aucune machine dans cette zone.</p>;
        }
        const upCount = machines.filter(m => m.isUp).length;
        return (
            <>
                <div className="cz-bulk-row">
                    <span className="cz-count">
                        <b>{upCount}</b> / {machines.length} machine(s) cochées
                    </span>
                    {canWrite && (
                        <>
                            <button className="btn btn-sm btn-outline-success"
                                onClick={() => this.bulkAll(true)}>
                                <FontAwesomeIcon icon={faCheckCircle} /> Tout cocher
                            </button>
                            <button className="btn btn-sm btn-outline-danger"
                                onClick={() => this.bulkAll(false)}>
                                <FontAwesomeIcon icon={faTimesCircle} /> Tout décocher
                            </button>
                            <button className="btn btn-sm btn-outline-info"
                                onClick={this.resetToCurrentStatus}
                                title="Restaurer le pré-remplissage à partir du statut machine actuel">
                                <FontAwesomeIcon icon={faMagic} /> Réinitialiser au statut courant
                            </button>
                        </>
                    )}
                </div>

                <table className="table table-bordered cz-machines-table">
                    <thead>
                        <tr>
                            <th scope="col">Machine</th>
                            <th scope="col">Type</th>
                            <th scope="col" title="Statut courant en EtatMachineHistorique">Statut</th>
                            <th scope="col" title="Aurait été pré-cochée d'après le statut">Pré-rempli</th>
                            <th scope="col">Confirmé Up</th>
                        </tr>
                    </thead>
                    <tbody>
                        {machines.map(m => {
                            const drift = m.isUp !== m.currentStatusUp;
                            return (
                                <tr key={m.machineNom}
                                    className={drift ? 'cz-row-drift' : ''}>
                                    <td className="cz-mch-name">
                                        <FontAwesomeIcon icon={faIndustry} /> {m.machineNom}
                                    </td>
                                    <td className="cz-mch-type">{m.machineType || '—'}</td>
                                    <td>{this.renderStatusBadge(m.codeEtat)}</td>
                                    <td className="cz-prefill">
                                        {m.currentStatusUp
                                            ? <FontAwesomeIcon icon={faCheckCircle} className="cz-icon-good" />
                                            : <FontAwesomeIcon icon={faTimesCircle} className="cz-icon-bad" />}
                                    </td>
                                    <td className="cz-checkbox-cell">
                                        <input type="checkbox"
                                            checked={!!m.isUp}
                                            disabled={!canWrite}
                                            onChange={() => this.toggleLocal(m.machineNom)} />
                                        {drift && (
                                            <span className="cz-drift-badge"
                                                title="Confirmation diffère du statut machine courant">
                                                ⚠
                                            </span>
                                        )}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </>
        );
    };

    render() {
        const { zoneNom, saving, loading, info, error, canWrite } = this.state;
        return (
            <div className="container cz-confirm-page" style={{ padding: 16 }}>
                {this.renderHeader()}

                {info  && <div className="alert alert-info">{info}</div>}
                {error && (
                    <div className="alert alert-danger">
                        <FontAwesomeIcon icon={faExclamationTriangle} /> {String(error)}
                    </div>
                )}

                {this.renderTable()}

                <div className="cz-actions">
                    <button className="btn btn-primary"
                        onClick={this.confirm}
                        disabled={!zoneNom || !canWrite || saving || loading}
                        title={!canWrite ? 'Vous devez être chef de zone ou chef d\'équipe pour confirmer' : ''}>
                        <FontAwesomeIcon icon={faSave} /> {saving ? 'Enregistrement…' : 'Confirmer'}
                    </button>
                    <button className="btn btn-outline-secondary"
                        onClick={this.reload}>
                        <FontAwesomeIcon icon={faUndo} /> Annuler les modifications
                    </button>
                </div>
            </div>
        );
    }

    errMsg = (err, fallback) => {
        if (!err) return fallback;
        if (err.response && err.response.data) {
            const d = err.response.data;
            return typeof d === 'string' ? d : (d.error || d.message || fallback);
        }
        return err.message || fallback;
    };
}
