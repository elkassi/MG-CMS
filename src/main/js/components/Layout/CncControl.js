import React, { Component } from 'react';
import axios from 'axios';
import { connect } from 'react-redux';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSpinner, faCheckCircle, faTrash, faCogs, faPlus, faExclamationTriangle, faSearch, faLock, faArrowRight, faPen, faImage } from '@fortawesome/free-solid-svg-icons';
import CncPatternImages from './CncPatternImages';

const STAGE_LABELS = { CNC: 'Contrôle CNC', PRESS: 'Contrôle Press', BLIND: 'Contrôle Blind' };
const SCRAP_STATUSES = [
    { code: 'EN_ATTENTE_VALIDATION', label: 'En attente de validation' },
    { code: 'EN_ATTENTE_MATIERE', label: 'En attente de la matière' },
    { code: 'REMPLACE', label: 'Remplacé' },
    { code: 'NON_REMPLACABLE', label: 'Non remplaçable' },
];
const scrapStatusLabel = (code) => {
    const s = SCRAP_STATUSES.find(x => x.code === code);
    return s ? s.label : (code || '-');
};
const emptyForm = () => ({ quantite: '', result: 'OK', codeDefaut: '', codeScrap: '', matricule: '', numBonScrap: '', scrapStatus: '', machineId: '' });

class CncControl extends Component {
    constructor(props) {
        super(props);
        this.state = {
            boxId: '',
            session: null,
            boxLoading: false,

            machines: [],          // all machines, each carries a `type` (CNC/PRESS/BLIND)
            codesDefautCNC: [],
            codesScrapCNC: [],

            controls: [],          // each control carries a `stage` and its programme (programNumber/panelNumber/pattern)
            patterns: [],          // associated CNC programmes (one is controlled at a time)
            selectedProgIdx: null, // index of the programme currently being controlled
            expandedIdx: null,     // pattern row whose image detail is open

            // per-stage add-control form
            forms: { CNC: emptyForm(), PRESS: emptyForm(), BLIND: emptyForm() },
            savingStage: null,     // stage whose control is currently being saved

            editingScrap: null,    // { id, numBonScrap, scrapStatus } of the scrap row being rectified
            savingScrap: false,

            message: null,
            messageType: null,
        };
    }

    componentDidMount() {
        this.loadMachines();
        this.loadCodes();
        // Embedded (e.g. inside the /cncQualite modal): auto-load the given box.
        if (this.props.boxId) {
            this.setState({ boxId: this.props.boxId }, this.loadBox);
        }
    }

    loadMachines = () => {
        axios.get('/api/cncPs/machines')
            .then(res => this.setState({ machines: res.data }))
            .catch(() => {});
    }

    loadCodes = () => {
        axios.get('/api/cncPs/codesDefautCNC')
            .then(res => this.setState({ codesDefautCNC: res.data }))
            .catch(() => {});
        axios.get('/api/cncPs/codesScrapCNC')
            .then(res => this.setState({ codesScrapCNC: res.data }))
            .catch(() => {});
    }

    handleBoxScan = (e) => {
        if (e.key === 'Enter' || e.type === 'click') this.loadBox();
    }

    loadBox = () => {
        const { boxId } = this.state;
        if (!boxId.trim()) return;
        if (!boxId.trim().toUpperCase().startsWith('S')) {
            this.showMessage("L'ID Boîte doit commencer par S", 'error');
            return;
        }
        this.setState({ boxLoading: true, session: null, controls: [], patterns: [], selectedProgIdx: null, expandedIdx: null });
        axios.get(`/api/cncPs/boxDetails/${boxId.trim()}`)
            .then(res => {
                const data = res.data;
                if (data.existingSession) {
                    const patterns = data.patterns || [];
                    this.setState({
                        session: data.existingSession,
                        patterns,
                        selectedProgIdx: patterns.length === 1 ? 0 : null,
                        boxLoading: false,
                    });
                    this.loadControls(data.existingSession.id);
                    this.showMessage('Session trouvée', 'success');
                } else {
                    this.setState({ boxLoading: false });
                    this.showMessage("Aucune session CNC PS pour cette boîte. Créez-la d'abord dans CNC PS.", 'error');
                }
            })
            .catch(err => {
                this.setState({ boxLoading: false });
                this.showMessage(err.response?.data || 'Boîte non trouvée', 'error');
            });
    }

    loadControls = (sessionId) => {
        axios.get(`/api/cncPs/session/${sessionId}/controls`)
            .then(res => this.setState({ controls: res.data }))
            .catch(() => {});
    }

    // Re-fetch the session to reflect server-computed quality status after a control changes.
    refreshSession = (sessionId) => {
        axios.get(`/api/cncPs/session/${sessionId}`)
            .then(res => this.setState({ session: res.data }))
            .catch(() => {});
    }

    // ===================== programme helpers =====================

    // Stable identity of a programme / control row: programNumber + panelNumber + pattern.
    progKey = (x) => ['programNumber', 'panelNumber', 'pattern']
        .map(k => (x && x[k] != null) ? String(x[k]).trim() : '').join('||');

    // A programme has a blind step only when its fil blind is present and at least 4 chars long.
    progHasBlind = (p) => {
        const b = (p && p.blindStitch != null) ? String(p.blindStitch).trim() : '';
        return b.length >= 4;
    }

    selectedProg = () => {
        const { patterns, selectedProgIdx } = this.state;
        return (selectedProgIdx != null && patterns[selectedProgIdx]) ? patterns[selectedProgIdx] : null;
    }

    selectProg = (i) => this.setState({ selectedProgIdx: i });

    controlMatchesProg = (c, p) => this.progKey(c) === this.progKey(p || {});

    progControls = (stage, p) => this.state.controls
        .filter(c => (c.stage || 'CNC') === stage && this.controlMatchesProg(c, p));

    // ===================== stage helpers (all per selected programme) =====================

    stagesFor = (p) => this.progHasBlind(p) ? ['CNC', 'PRESS', 'BLIND'] : ['CNC', 'PRESS'];

    boxQty = () => {
        const { session } = this.state;
        if (!session) return 0;
        const n = parseInt(session.quantiteImp, 10);
        return isNaN(n) ? 0 : n;
    }

    sumQty = (arr) => arr.reduce((s, c) => s + (c.quantite || 0), 0);
    controlledQty = (stage, p) => this.sumQty(this.progControls(stage, p));
    scrappedQty = (stage, p) => this.sumQty(this.progControls(stage, p)
        .filter(c => c.codeScrap && String(c.codeScrap).trim() !== ''));
    repriseQty = (stage, p) => this.sumQty(this.progControls(stage, p)
        .filter(c => c.result === 'NOK' && c.codeDefaut && String(c.codeDefaut).trim() !== ''
            && !(c.codeScrap && String(c.codeScrap).trim() !== '')));
    okQty = (stage, p) => this.controlledQty(stage, p) - this.scrappedQty(stage, p) - this.repriseQty(stage, p);
    // Scrapped pieces marked "Remplacé" — their replacements re-enter the next stage.
    remplaceQty = (stage, p) => this.sumQty(this.progControls(stage, p)
        .filter(c => c.codeScrap && String(c.codeScrap).trim() !== '' && c.scrapStatus === 'REMPLACE'));
    // Pieces passing to the next stage = controlled - scrapped + replaced.
    passingQty = (stage, p) => this.controlledQty(stage, p) - this.scrappedQty(stage, p) + this.remplaceQty(stage, p);

    stageTarget = (stage, p) => {
        if (stage === 'CNC') return this.boxQty();
        if (stage === 'PRESS') return this.passingQty('CNC', p);
        return this.passingQty('PRESS', p); // BLIND
    }

    machinesForStage = (stage) => this.state.machines.filter(m => (m.type || 'CNC') === stage);

    // Machine of the most recent control for this stage+programme (so the select keeps its value after reload).
    lastMachineId = (stage, p) => {
        const rows = this.progControls(stage, p).filter(c => c.machine);
        if (rows.length === 0) return '';
        const last = rows.reduce((a, b) => ((b.id || 0) > (a.id || 0) ? b : a));
        return last.machine.id;
    }

    // Effective machine for a section = the form choice, else the last control's machine.
    effectiveMachineId = (stage, p) => this.state.forms[stage].machineId || this.lastMachineId(stage, p);

    // a stage is reachable once the previous stage controlled its full target (for this programme)
    prevStageDone = (stage, p) => {
        if (stage === 'CNC') return true;
        const cncTarget = this.boxQty();
        if (cncTarget <= 0) return false;
        if (stage === 'PRESS') return this.controlledQty('CNC', p) >= cncTarget;
        // BLIND
        return this.controlledQty('CNC', p) >= cncTarget
            && this.controlledQty('PRESS', p) >= this.stageTarget('PRESS', p);
    }

    stageDone = (stage, p) => this.controlledQty(stage, p) >= this.stageTarget(stage, p);

    setForm = (stage, patch) => {
        this.setState({ forms: { ...this.state.forms, [stage]: { ...this.state.forms[stage], ...patch } } });
    }

    handleAddControl = (stage) => {
        const { session, forms, patterns } = this.state;
        const f = forms[stage];
        if (!session) return;
        const prog = this.selectedProg();
        if (patterns.length > 0 && !prog) { this.showMessage('Sélectionnez un programme à contrôler', 'error'); return; }
        const machineId = this.effectiveMachineId(stage, prog);
        if (!machineId) { this.showMessage(`Sélectionnez la machine ${stage} d'abord`, 'error'); return; }
        if (!f.quantite) { this.showMessage('Quantité obligatoire', 'error'); return; }
        if (f.result === 'NOK') {
            if (!f.codeDefaut) { this.showMessage('Pour NOK, le code défaut est obligatoire', 'error'); return; }
            if (!f.matricule || !f.matricule.trim()) { this.showMessage('Pour NOK, le matricule est obligatoire', 'error'); return; }
        }
        this.setState({ savingStage: stage });
        axios.post(`/api/cncPs/session/${session.id}/control`, {
            stage,
            quantite: parseInt(f.quantite, 10),
            result: f.result,
            codeDefaut: f.result === 'NOK' ? f.codeDefaut : null,
            codeScrap: f.result === 'NOK' ? f.codeScrap : null,
            matricule: f.matricule || null,
            programNumber: prog ? prog.programNumber : null,
            panelNumber: prog ? prog.panelNumber : null,
            pattern: prog ? prog.pattern : null,
            numBonScrap: f.result === 'NOK' && f.codeScrap ? (f.numBonScrap || null) : null,
            scrapStatus: f.result === 'NOK' && f.codeScrap ? (f.scrapStatus || null) : null,
            machineId: parseInt(machineId, 10),
        })
            .then(() => {
                this.showMessage('Contrôle ajouté', 'success');
                this.setForm(stage, { quantite: '', codeDefaut: '', codeScrap: '', numBonScrap: '', scrapStatus: '' });
                this.setState({ savingStage: null });
                this.loadControls(session.id);
                this.refreshSession(session.id);
            })
            .catch(err => { this.setState({ savingStage: null }); this.showMessage(err.response?.data || 'Erreur ajout contrôle', 'error'); });
    }

    handleDeleteControl = (controlId) => {
        if (!window.confirm('Supprimer ce contrôle ?')) return;
        axios.delete(`/api/cncPs/control/${controlId}`)
            .then(() => { this.showMessage('Contrôle supprimé', 'success'); this.loadControls(this.state.session.id); this.refreshSession(this.state.session.id); })
            .catch(err => this.showMessage(err.response?.data || 'Erreur suppression', 'error'));
    }

    openScrapEdit = (c) => this.setState({ editingScrap: { id: c.id, numBonScrap: c.numBonScrap || '', scrapStatus: c.scrapStatus || '' } });
    closeScrapEdit = () => this.setState({ editingScrap: null });

    saveScrapEdit = () => {
        const { editingScrap, session } = this.state;
        if (!editingScrap) return;
        this.setState({ savingScrap: true });
        axios.put(`/api/cncPs/control/${editingScrap.id}/scrap`, {
            numBonScrap: editingScrap.numBonScrap || null,
            scrapStatus: editingScrap.scrapStatus || null,
        })
            .then(() => {
                this.showMessage('Scrap mis à jour', 'success');
                this.setState({ savingScrap: false, editingScrap: null });
                this.loadControls(session.id);
                this.refreshSession(session.id);
            })
            .catch(err => { this.setState({ savingScrap: false }); this.showMessage(err.response?.data || 'Erreur mise à jour scrap', 'error'); });
    }

    showMessage = (message, type) => {
        this.setState({ message, messageType: type });
        setTimeout(() => this.setState({ message: null, messageType: null }), 5000);
    }

    formatDateTime = (dt) => dt ? new Date(dt).toLocaleString('fr-FR') : '-';

    newBox = () => this.setState({
        boxId: '', session: null, controls: [], patterns: [], selectedProgIdx: null, expandedIdx: null,
        forms: { CNC: emptyForm(), PRESS: emptyForm(), BLIND: emptyForm() },
    });

    // ===================== programme progress bar (table column) =====================

    renderProgBar(stage, p) {
        if (stage === 'BLIND' && !this.progHasBlind(p)) {
            return (
                <div className="progress" style={{ height: 18, backgroundColor: '#e9ecef' }} title="Pas de fil blind">
                    <div className="progress-bar" style={{ width: '100%', backgroundColor: '#adb5bd', color: '#fff' }}>N/A</div>
                </div>
            );
        }
        const target = this.stageTarget(stage, p);
        const ok = this.okQty(stage, p);
        const rep = this.repriseQty(stage, p);
        const scr = this.scrappedQty(stage, p);
        const controlled = ok + rep + scr;
        const pct = v => target > 0 ? Math.min((v / target) * 100, 100) : 0;
        return (
            <div className="progress" style={{ height: 18 }} title={`${controlled}/${target} contrôlé`}>
                {ok > 0 && <div className="progress-bar bg-success" style={{ width: `${pct(ok)}%` }}>{ok}</div>}
                {rep > 0 && <div className="progress-bar bg-warning text-dark" style={{ width: `${pct(rep)}%` }}>{rep}</div>}
                {scr > 0 && <div className="progress-bar bg-danger" style={{ width: `${pct(scr)}%` }}>{scr}</div>}
            </div>
        );
    }

    renderStage(stage, idx, p) {
        const { forms, savingStage, codesDefautCNC, codesScrapCNC, session } = this.state;
        const hasLeather = session && session.code1Imp && session.code1Imp.trim();
        const leatherOk = !hasLeather || session.completed;

        const prevDone = this.prevStageDone(stage, p);
        const open = prevDone && leatherOk;
        const lockReason = !leatherOk ? 'Complétez la consommation cuir dans CNC PS pour débloquer.'
            : !prevDone ? "Terminez l'étape précédente pour débloquer." : null;
        const target = this.stageTarget(stage, p);
        const controlled = this.controlledQty(stage, p);
        const scrapped = this.scrappedQty(stage, p);
        const passing = this.passingQty(stage, p);
        const remaining = target - controlled;
        const machines = this.machinesForStage(stage);
        const machineId = this.effectiveMachineId(stage, p);
        const done = prevDone && this.stageDone(stage, p);
        const isLast = idx === this.stagesFor(p).length - 1;
        const f = forms[stage];

        const headerClass = !open ? 'bg-secondary' : done ? 'bg-success' : 'bg-primary';

        return (
            <div className="card mb-3 shadow-sm" key={stage}>
                <div className={`card-header ${headerClass} text-white d-flex justify-content-between align-items-center`}>
                    <strong>{idx + 1}. {STAGE_LABELS[stage]} {!open && <FontAwesomeIcon icon={faLock} />}</strong>
                    <span className="badge badge-light">{!open ? 'Verrouillé' : done ? 'Terminé ✓' : 'En cours'}</span>
                </div>
                <div className="card-body">
                    {!open ? (
                        <div className="text-muted"><FontAwesomeIcon icon={faLock} /> {lockReason}</div>
                    ) : (
                        <>
                            {/* Progress summary */}
                            <div className="d-flex justify-content-between flex-wrap mb-1" style={{ fontSize: '0.95rem' }}>
                                <span>À contrôler: <strong>{target}</strong></span>
                                <span>Contrôlé: <strong>{controlled}</strong></span>
                                <span className="text-success">OK → suivant: <strong>{passing}</strong></span>
                                <span className="text-danger">Scrap: <strong>{scrapped}</strong></span>
                                <span>Restant: <strong className={remaining > 0 ? 'text-warning' : 'text-success'}>{remaining}</strong></span>
                            </div>
                            <div className="progress mb-3" style={{ height: '20px' }}>
                                <div className={`progress-bar ${remaining <= 0 ? 'bg-success' : 'bg-info'}`}
                                     style={{ width: `${target > 0 ? Math.min((controlled / target) * 100, 100) : 100}%` }}>
                                    {target > 0 ? `${Math.round((controlled / target) * 100)}%` : '—'}
                                </div>
                            </div>

                            {/* Machine selector for this section (recorded on each control) */}
                            <div className="form-group row mb-3">
                                <label className="col-sm-3 col-form-label font-weight-bold">Machine {stage} <span className="text-danger">*</span></label>
                                <div className="col-sm-6">
                                    <select className="form-control" value={machineId || ''}
                                            onChange={e => this.setForm(stage, { machineId: e.target.value })}>
                                        <option value="">-- Sélectionner --</option>
                                        {machines.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
                                    </select>
                                    {machines.length === 0 && (
                                        <small className="text-warning">Aucune machine de type {stage}. Ajoutez-en dans Admin → Machines CNC.</small>
                                    )}
                                </div>
                            </div>

                            {/* Add-control form */}
                            {machineId && target > 0 && remaining > 0 && (
                                <div className="border rounded p-3 bg-light">
                                    <div className="row">
                                        <div className="col-md-3 form-group mb-2">
                                            <label className="font-weight-bold">Quantité <span className="text-danger">*</span></label>
                                            <input type="number" min="1" className="form-control form-control-lg"
                                                   value={f.quantite} onChange={e => this.setForm(stage, { quantite: e.target.value })}
                                                   style={{ fontWeight: 'bold' }} />
                                        </div>
                                        <div className="col-md-3 form-group mb-2">
                                            <label className="font-weight-bold">Résultat <span className="text-danger">*</span></label>
                                            <select className="form-control form-control-lg" value={f.result}
                                                    onChange={e => this.setForm(stage, { result: e.target.value, codeDefaut: '', codeScrap: '' })}
                                                    style={{ fontWeight: 'bold', color: f.result === 'OK' ? '#28a745' : '#dc3545' }}>
                                                <option value="OK">✓ OK</option>
                                                <option value="NOK">✗ NOK</option>
                                            </select>
                                        </div>
                                        {f.result === 'NOK' && (
                                            <>
                                                <div className="col-md-3 form-group mb-2">
                                                    <label className="font-weight-bold">Code Défaut <span className="text-danger">*</span></label>
                                                    <select className="form-control" value={f.codeDefaut}
                                                            onChange={e => this.setForm(stage, { codeDefaut: e.target.value })}>
                                                        <option value="">-- Aucun --</option>
                                                        {codesDefautCNC.map(cd => <option key={cd.code} value={cd.code}>{cd.label}</option>)}
                                                    </select>
                                                </div>
                                                <div className="col-md-3 form-group mb-2">
                                                    <label className="font-weight-bold">Code Scrap</label>
                                                    <select className="form-control" value={f.codeScrap}
                                                            onChange={e => this.setForm(stage, { codeScrap: e.target.value })}>
                                                        <option value="">-- Aucun --</option>
                                                        {codesScrapCNC.map(cs => <option key={cs.code} value={cs.code}>{cs.label}</option>)}
                                                    </select>
                                                </div>
                                                {f.codeScrap && (
                                                    <>
                                                        <div className="col-md-3 form-group mb-2">
                                                            <label className="font-weight-bold">N° Bon Scrap</label>
                                                            <input type="text" className="form-control" value={f.numBonScrap}
                                                                   onChange={e => this.setForm(stage, { numBonScrap: e.target.value })}
                                                                   placeholder="Optionnel" />
                                                        </div>
                                                        <div className="col-md-3 form-group mb-2">
                                                            <label className="font-weight-bold">Statut Scrap</label>
                                                            <select className="form-control" value={f.scrapStatus}
                                                                    onChange={e => this.setForm(stage, { scrapStatus: e.target.value })}>
                                                                <option value="">-- Non défini --</option>
                                                                {SCRAP_STATUSES.map(s => <option key={s.code} value={s.code}>{s.label}</option>)}
                                                            </select>
                                                        </div>
                                                    </>
                                                )}
                                            </>
                                        )}
                                    </div>
                                    <div className="row">
                                        <div className="col-md-3 form-group mb-2">
                                            <label className="font-weight-bold">Matricule {f.result === 'NOK' && <span className="text-danger">*</span>}</label>
                                            <input type="text" className="form-control" value={f.matricule}
                                                   onChange={e => this.setForm(stage, { matricule: e.target.value })} placeholder="Matricule" />
                                        </div>
                                    </div>
                                    {f.result === 'NOK' && (
                                        <small className="text-muted d-block mb-2">
                                            NOK + code défaut sans scrap = pièce reprise OK (passe à l'étape suivante). NOK avec code scrap = pièce rebutée.
                                        </small>
                                    )}
                                    <button className="btn btn-primary btn-lg" onClick={() => this.handleAddControl(stage)}
                                            disabled={savingStage === stage || !f.quantite}>
                                        {savingStage === stage ? <FontAwesomeIcon icon={faSpinner} spin /> : <><FontAwesomeIcon icon={faPlus} /> Ajouter le contrôle</>}
                                    </button>
                                </div>
                            )}

                            {!machineId && target > 0 && (
                                <div className="alert alert-info mb-0">Sélectionnez la machine {stage} pour commencer.</div>
                            )}
                            {machineId && target > 0 && remaining <= 0 && (
                                <div className="alert alert-success mb-0">
                                    <FontAwesomeIcon icon={faCheckCircle} /> Étape terminée — {passing} pièce(s) {isLast
                                        ? 'conformes.'
                                        : <>à transmettre <FontAwesomeIcon icon={faArrowRight} /> étape suivante.</>}
                                </div>
                            )}
                            {target === 0 && (
                                <div className="alert alert-warning mb-0">Aucune pièce à contrôler à cette étape (toutes rebutées avant).</div>
                            )}

                            {/* Controls list for this stage / this programme */}
                            {this.progControls(stage, p).length > 0 && (
                                <div className="table-responsive mt-3">
                                    <table className="table table-sm table-bordered table-hover mb-0">
                                        <thead className="thead-dark">
                                            <tr><th>#</th><th>Qté</th><th>Résultat</th><th>Machine</th><th>Code Défaut</th><th>Code Scrap</th><th>N° Bon Scrap</th><th>Statut Scrap</th><th>Matricule</th><th>Date</th><th style={{ width: 80 }}></th></tr>
                                        </thead>
                                        <tbody>
                                            {this.progControls(stage, p).map((c, i) => {
                                                const isScrap = c.codeScrap && String(c.codeScrap).trim() !== '';
                                                const stCode = c.scrapStatus || '';
                                                const stClass = stCode === 'REMPLACE' ? 'badge-success'
                                                    : stCode === 'NON_REMPLACABLE' ? 'badge-dark'
                                                    : stCode ? 'badge-warning' : 'badge-light';
                                                return (
                                                    <tr key={c.id} className={isScrap ? 'table-danger' : (c.result === 'NOK' ? 'table-warning' : '')}>
                                                        <td>{i + 1}</td>
                                                        <td><strong>{c.quantite}</strong></td>
                                                        <td>
                                                            <span className={`badge badge-${c.result === 'OK' ? 'success' : 'danger'}`}>{c.result}</span>
                                                            {isScrap && <span className="badge badge-dark ml-1">SCRAP</span>}
                                                        </td>
                                                        <td>{c.machine ? c.machine.name : '-'}</td>
                                                        <td>{c.codeDefaut || '-'}</td>
                                                        <td>{c.codeScrap || '-'}</td>
                                                        <td>{isScrap ? (c.numBonScrap || <span className="text-muted">-</span>) : '-'}</td>
                                                        <td>{isScrap ? <span className={`badge ${stClass}`}>{stCode ? scrapStatusLabel(stCode) : 'Non défini'}</span> : '-'}</td>
                                                        <td>{c.matricule || '-'}</td>
                                                        <td className="small">{this.formatDateTime(c.createdAt)}</td>
                                                        <td className="text-nowrap">
                                                            {isScrap && (
                                                                <button className="btn btn-outline-primary btn-sm py-0 mr-1" title="Rectifier le scrap"
                                                                        onClick={() => this.openScrapEdit(c)}><FontAwesomeIcon icon={faPen} /></button>
                                                            )}
                                                            <button className="btn btn-outline-danger btn-sm py-0" onClick={() => this.handleDeleteControl(c.id)}><FontAwesomeIcon icon={faTrash} /></button>
                                                        </td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
        );
    }

    renderScrapEditModal() {
        const { editingScrap, savingScrap } = this.state;
        if (!editingScrap) return null;
        const set = (patch) => this.setState({ editingScrap: { ...editingScrap, ...patch } });
        return (
            <div className="modal d-block" tabIndex="-1" style={{ background: 'rgba(0,0,0,0.5)' }} onClick={this.closeScrapEdit}>
                <div className="modal-dialog modal-dialog-centered" onClick={e => e.stopPropagation()}>
                    <div className="modal-content">
                        <div className="modal-header bg-danger text-white">
                            <h5 className="modal-title">Rectifier le scrap</h5>
                            <button type="button" className="close text-white" onClick={this.closeScrapEdit}><span>&times;</span></button>
                        </div>
                        <div className="modal-body">
                            <div className="form-group">
                                <label className="font-weight-bold">N° Bon Scrap</label>
                                <input type="text" className="form-control" value={editingScrap.numBonScrap}
                                       onChange={e => set({ numBonScrap: e.target.value })} placeholder="Optionnel" autoFocus />
                            </div>
                            <div className="form-group mb-0">
                                <label className="font-weight-bold">Statut Scrap</label>
                                <select className="form-control" value={editingScrap.scrapStatus}
                                        onChange={e => set({ scrapStatus: e.target.value })}>
                                    <option value="">-- Non défini --</option>
                                    {SCRAP_STATUSES.map(s => <option key={s.code} value={s.code}>{s.label}</option>)}
                                </select>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-secondary" onClick={this.closeScrapEdit} disabled={savingScrap}>Annuler</button>
                            <button className="btn btn-primary" onClick={this.saveScrapEdit} disabled={savingScrap}>
                                {savingScrap ? <FontAwesomeIcon icon={faSpinner} spin /> : 'Enregistrer'}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    render() {
        const { boxId, session, boxLoading, patterns, selectedProgIdx, message, messageType } = this.state;
        const embedded = !!this.props.embedded;
        const hasLeather = session && session.code1Imp && session.code1Imp.trim();
        const leatherOk = !hasLeather || session.completed;
        const selProg = this.selectedProg();

        return (
            <div className={embedded ? '' : 'container-fluid mt-3'} style={embedded ? {} : { maxWidth: 1200 }}>
                {!embedded && (
                    <div className="d-flex justify-content-between align-items-center mb-3">
                        <h2><FontAwesomeIcon icon={faCogs} /> Contrôle CNC</h2>
                        {session && <button className="btn btn-outline-secondary" onClick={this.newBox}>Nouvelle boîte</button>}
                    </div>
                )}

                {message && (
                    <div className={`alert alert-${messageType === 'success' ? 'success' : messageType === 'warning' ? 'warning' : 'danger'} d-flex align-items-center`}>
                        <FontAwesomeIcon icon={messageType === 'success' ? faCheckCircle : faExclamationTriangle} className="mr-2" />
                        {' '}{message}
                    </div>
                )}

                {this.renderScrapEditModal()}

                {/* Scan box */}
                {!session && !embedded && (
                    <div className="card mb-3 shadow-sm">
                        <div className="card-header bg-primary text-white"><strong>Scanner la boîte</strong></div>
                        <div className="card-body p-4">
                            <div className="row justify-content-center">
                                <div className="col-md-8 col-lg-6">
                                    <div className="form-group">
                                        <label className="font-weight-bold" style={{ fontSize: '1.1rem' }}>ID Boîte</label>
                                        <input type="text" className="form-control form-control-lg" value={boxId} autoFocus
                                               onChange={e => this.setState({ boxId: e.target.value })}
                                               onKeyPress={this.handleBoxScan}
                                               placeholder="Scannez ou saisissez l'ID de la boîte" style={{ fontSize: '1.2rem' }} />
                                    </div>
                                    <button className="btn btn-primary btn-lg w-100 mt-2" onClick={this.handleBoxScan} disabled={boxLoading || !boxId.trim()}>
                                        {boxLoading ? <FontAwesomeIcon icon={faSpinner} spin /> : <><FontAwesomeIcon icon={faSearch} /> Rechercher</>}
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {embedded && !session && boxLoading && (
                    <div className="text-center p-4"><FontAwesomeIcon icon={faSpinner} spin size="2x" /></div>
                )}

                {session && (
                    <div>
                        {/* 1. Session info */}
                        <div className="card mb-3 shadow-sm">
                            <div className="card-header bg-info text-white d-flex justify-content-between align-items-center">
                                <strong>Session #{session.id} — {session.boxId}</strong>
                                <span className={`badge badge-${(session.qualiteStatus || '').startsWith('Terminé') ? 'success' : session.qualiteStatus === 'En cours' ? 'warning' : 'light'}`}>
                                    Qualité: {session.qualiteStatus || 'Non démarré'}
                                </span>
                            </div>
                            <div className="card-body">
                                <div className="row">
                                    <div className="col-md-3"><strong>PN:</strong> {session.partNumberImp}</div>
                                    <div className="col-md-2"><strong>Qté Boîte:</strong> <span className="text-primary font-weight-bold">{session.quantiteImp}</span></div>
                                    <div className="col-md-2"><strong>Code3:</strong> {session.code3Imp}</div>
                                    <div className="col-md-2"><strong>Cuir:</strong> {session.code1Imp || <span className="text-muted">N/A</span>}</div>
                                    <div className="col-md-3"><strong>Cuir OK:</strong> {hasLeather
                                        ? (leatherOk ? <span className="text-success font-weight-bold">Oui ✓</span> : <span className="text-danger font-weight-bold">Non ✗</span>)
                                        : <span className="text-muted">N/A</span>}</div>
                                </div>
                            </div>
                        </div>

                        {/* 2. Leather consumption */}
                        {session.consumptions && (
                            <div className="card mb-3 shadow-sm">
                                <div className="card-header bg-light"><strong>Consommation Cuir</strong></div>
                                <div className="card-body p-0">
                                    {session.consumptions.length === 0 ? (
                                        <div className="p-3 text-muted">Aucune consommation cuir enregistrée.</div>
                                    ) : (
                                        <table className="table table-sm table-bordered mb-0">
                                            <thead className="thead-light">
                                                <tr><th>PN Cuir</th><th>Serial</th><th>Lot</th><th>Qté Initiale</th><th>Qté Consommée</th><th>Qté Retour</th><th>Date</th></tr>
                                            </thead>
                                            <tbody>
                                                {session.consumptions.map((c, i) => (
                                                    <tr key={i}>
                                                        <td>{c.leatherPartNumber || '-'}</td>
                                                        <td>{c.serial || '-'}</td>
                                                        <td>{c.lot || '-'}</td>
                                                        <td>{c.quantiteInitial != null ? c.quantiteInitial : '-'}</td>
                                                        <td>{c.quantiteConsumed != null ? c.quantiteConsumed : '-'}</td>
                                                        <td>{c.quantiteRetour != null ? c.quantiteRetour : '-'}</td>
                                                        <td className="small">{this.formatDateTime(c.createdAt)}</td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* 3. Associated CNC programmes — select one, with per-stage progress */}
                        {patterns.length > 0 && (
                            <div className="card mb-3 shadow-sm">
                                <div className="card-header bg-light d-flex justify-content-between align-items-center">
                                    <strong>Programmes CNC associés</strong>
                                    <small className="text-muted">Cliquez un programme pour le contrôler — <span className="text-success">vert OK</span> · <span className="text-warning">jaune reprise</span> · <span className="text-danger">rouge scrap</span></small>
                                </div>
                                <div className="card-body p-0">
                                    <div className="table-responsive">
                                        <table className="table table-sm table-bordered mb-0 align-middle">
                                            <thead className="thead-light">
                                                <tr>
                                                    <th style={{ width: 28 }}></th>
                                                    <th style={{ width: 40 }}></th>
                                                    <th>Panel</th><th>Pattern</th><th>N° Programme</th><th>Fil Couture CNC</th><th>Cavité Press</th><th>Fil blind</th><th>Profil</th><th>Type</th>
                                                    <th style={{ minWidth: 110 }}>CNC</th><th style={{ minWidth: 110 }}>Press</th><th style={{ minWidth: 110 }}>Blind</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {patterns.map((p, i) => {
                                                    const sel = i === selectedProgIdx;
                                                    return (
                                                        <React.Fragment key={i}>
                                                            <tr onClick={() => this.selectProg(i)} style={{ cursor: 'pointer' }} className={sel ? 'table-primary' : ''}>
                                                                <td className="text-center">
                                                                    <button className="btn btn-link btn-sm p-0" title="Voir images (cuir / fil couture)"
                                                                            onClick={(e) => { e.stopPropagation(); this.setState({ expandedIdx: this.state.expandedIdx === i ? null : i }); }}>
                                                                        <FontAwesomeIcon icon={faImage} />
                                                                    </button>
                                                                </td>
                                                                <td className="text-center"><input type="radio" checked={sel} readOnly /></td>
                                                                <td>{p.panelNumber || '-'}</td>
                                                                <td>{p.pattern}</td>
                                                                <td>{p.programNumber || '-'}</td>
                                                                <td>{p.coutureDecorativeCnc || '-'}</td>
                                                                <td>{p.cavitePress || '-'}</td>
                                                                <td>{p.blindStitch || '-'}</td>
                                                                <td>{p.profil || '-'}</td>
                                                                <td>{p.type || '-'}</td>
                                                                <td>{this.renderProgBar('CNC', p)}</td>
                                                                <td>{this.renderProgBar('PRESS', p)}</td>
                                                                <td>{this.renderProgBar('BLIND', p)}</td>
                                                            </tr>
                                                            {this.state.expandedIdx === i && (
                                                                <tr>
                                                                    <td colSpan={13} className="bg-light">
                                                                        <CncPatternImages leatherPn={session.code1Imp} filCouture={p.coutureDecorativeCnc} />
                                                                    </td>
                                                                </tr>
                                                            )}
                                                        </React.Fragment>
                                                    );
                                                })}
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* Leather gate */}
                        {hasLeather && !leatherOk && (
                            <div className="alert alert-warning shadow-sm">
                                <FontAwesomeIcon icon={faExclamationTriangle} className="mr-1" /> La consommation cuir doit être complétée avant le contrôle.
                                Allez dans <strong>CNC PS</strong> pour la compléter.
                            </div>
                        )}

                        {/* Stages for the selected programme: CNC → Press → (Blind) */}
                        {patterns.length > 0 && !selProg ? (
                            <div className="alert alert-info shadow-sm">
                                <FontAwesomeIcon icon={faArrowRight} className="mr-1" /> Sélectionnez un programme ci-dessus pour le contrôler.
                            </div>
                        ) : (
                            <>
                                {selProg && (
                                    <h5 className="mb-3">
                                        Contrôle du programme : <span className="text-primary">{selProg.programNumber || selProg.pattern || '—'}</span>
                                        {selProg.panelNumber ? <span className="text-muted"> — Panel {selProg.panelNumber}</span> : null}
                                    </h5>
                                )}
                                {this.stagesFor(selProg).map((stage, idx) => this.renderStage(stage, idx, selProg))}
                            </>
                        )}
                    </div>
                )}
            </div>
        );
    }
}

const mapStateToProps = state => ({ security: state.security });
export default connect(mapStateToProps)(CncControl);
