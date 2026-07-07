import React, { Component } from 'react';
import axios from 'axios';
import { connect } from 'react-redux';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSpinner, faCheckCircle, faTimesCircle, faPrint, faTrash, faBoxArchive, faPlus, faWifi, faExclamationTriangle, faImage } from '@fortawesome/free-solid-svg-icons';
import CncPatternImages from './CncPatternImages';

class CncPs extends Component {
    constructor(props) {
        super(props);
        this.state = {
            // Box scanning
            boxId: '',
            boxDetails: null,
            boxLoading: false,

            // Active session (auto-created on scan — no manual session step)
            session: null,

            // Leather consumption form
            leatherPartNumber: '',
            serial: '',
            lot: '',
            quantiteInitial: '',
            consumeLoading: false,

            // CNC Patterns
            patterns: [],
            expandedIdx: null, // pattern row whose image detail is open

            // Recent history (last 8h)
            recentSessions: [],
            historyLoading: false,

            // Printer status
            printerConnected: null,
            printerIp: '',
            printerChecking: false,

            // Messages
            message: null,
            messageType: null,
        };
    }

    componentDidMount() {
        this.loadRecentHistory();
        this.checkPrinterStatus();
    }

    checkPrinterStatus = () => {
        this.setState({ printerChecking: true });
        axios.get('/api/cncPs/printerStatus')
            .then(res => {
                this.setState({
                    printerConnected: res.data.connected,
                    printerIp: res.data.ip || '',
                    printerNetworkConnected: res.data.networkConnected,
                    printerLocalZebraConnected: res.data.localZebraConnected,
                    printerLocalZebraName: res.data.localZebraName || '',
                    printerMessage: res.data.message || '',
                    printerChecking: false,
                });
            })
            .catch(() => this.setState({ printerConnected: false, printerChecking: false }));
    }

    loadRecentHistory = () => {
        this.setState({ historyLoading: true });
        axios.get('/api/cncPs/myRecent')
            .then(res => this.setState({ recentSessions: res.data, historyLoading: false }))
            .catch(() => this.setState({ historyLoading: false }));
    }

    // Scan box ID -> show leather consumption directly (no manual session step).
    // If the box has no session yet, one is auto-created. If it is unknown, show an error.
    handleBoxIdScan = (e) => {
        if (e.key === 'Enter' || e.type === 'click') {
            const { boxId } = this.state;
            if (!boxId.trim()) return;
            if (!boxId.trim().toUpperCase().startsWith('S')) {
                this.showMessage("L'ID Boîte doit commencer par S", 'error');
                return;
            }
            this.setState({ boxLoading: true, boxDetails: null, session: null, patterns: [], expandedIdx: null });
            axios.get(`/api/cncPs/boxDetails/${boxId.trim()}`)
                .then(res => {
                    const data = res.data;
                    if (data.existingSession) {
                        // Box already has consumption data — resume it directly
                        this.setState({
                            boxDetails: data,
                            session: data.existingSession,
                            patterns: data.patterns || [],
                            boxLoading: false,
                        });
                    } else {
                        // No session step: auto-create so the consumption form shows immediately
                        axios.post('/api/cncPs/session', { boxId: boxId.trim() })
                            .then(sres => {
                                this.setState({
                                    boxDetails: data,
                                    session: sres.data,
                                    patterns: data.patterns || [],
                                    boxLoading: false,
                                });
                                this.loadRecentHistory();
                            })
                            .catch(err => {
                                if (err.response?.status === 409) {
                                    // A session was created meanwhile (concurrent scan) — resume it
                                    axios.get(`/api/cncPs/boxDetails/${boxId.trim()}`)
                                        .then(r => this.setState({
                                            boxDetails: r.data,
                                            session: r.data.existingSession || null,
                                            patterns: r.data.patterns || [],
                                            boxLoading: false,
                                        }))
                                        .catch(() => {
                                            this.setState({ boxLoading: false });
                                            this.showMessage('Boîte déjà enregistrée — veuillez rescanner', 'warning');
                                        });
                                    return;
                                }
                                this.setState({ boxLoading: false });
                                this.showMessage(err.response?.data || 'Erreur de création session', 'error');
                            });
                    }
                })
                .catch(err => {
                    this.setState({ boxLoading: false });
                    this.showMessage(err.response?.data || 'Boîte non trouvée', 'error');
                });
        }
    }

    // Validate PN Cuir against session code1Imp
    validatePnCuir = (leatherPn) => {
        const { session } = this.state;
        if (!session || !session.code1Imp) return true;
        const code1 = session.code1Imp.trim().toUpperCase();
        const pn = leatherPn.trim().toUpperCase();
        // leatherPartNumber must equal code1Imp OR be "P" + code1Imp
        return pn === code1 || pn === 'P' + code1;
    }

    handleAddConsumption = () => {
        const { session, leatherPartNumber, serial, lot, quantiteInitial } = this.state;
        if (!session) return;
        if (!leatherPartNumber.trim() || !serial.trim() || !lot.trim() || !quantiteInitial) {
            this.showMessage('Veuillez remplir tous les champs de consommation', 'error');
            return;
        }
        // Validate PN Cuir
        if (!this.validatePnCuir(leatherPartNumber)) {
            this.showMessage(`Le PN Cuir doit correspondre au Code1 (${session.code1Imp}) ou être P${session.code1Imp}`, 'error');
            return;
        }
        // Lot: auto-prefix 'H' when it starts with a digit (safety net for the click path)
        let lotVal = lot.trim();
        if (/^[0-9]/.test(lotVal)) lotVal = 'H' + lotVal;
        if (!lotVal.toUpperCase().startsWith('H')) {
            this.showMessage("Le lot doit commencer par 'H'", 'error');
            return;
        }
        this.setState({ consumeLoading: true, lot: lotVal });
        axios.post(`/api/cncPs/session/${session.id}/consume`, {
            leatherPartNumber: leatherPartNumber.trim(),
            serial: serial.trim(),
            lot: lotVal,
            quantiteInitial: parseInt(quantiteInitial, 10),
        })
            .then(() => {
                this.showMessage('Consommation ajoutée', 'success');
                this.setState({ leatherPartNumber: '', serial: '', lot: '', quantiteInitial: '', consumeLoading: false });
                this.refreshSession(session.id);
            })
            .catch(err => {
                this.setState({ consumeLoading: false });
                this.showMessage(err.response?.data || 'Erreur ajout consommation', 'error');
            });
    }

    refreshSession = (sessionId) => {
        axios.get(`/api/cncPs/session/${sessionId}`)
            .then(res => this.setState({ session: res.data }))
            .catch(() => {});
    }

    handleCompleteSession = () => {
        const { session } = this.state;
        if (!session) return;
        axios.post(`/api/cncPs/session/${session.id}/complete`)
            .then(res => {
                this.setState({ session: res.data });
                this.showMessage('Session terminée', 'success');
                this.loadRecentHistory();
            })
            .catch(err => this.showMessage(err.response?.data || 'Erreur', 'error'));
    }

    handlePrintLeatherReturn = () => {
        const { session } = this.state;
        if (!session) return;
        axios.post(`/api/cncPs/session/${session.id}/printLeatherReturn`)
            .then(res => {
                this.showMessage(res.data || 'Étiquette retour cuir imprimée', 'success');
                this.refreshSession(session.id);
            })
            .catch(err => this.showMessage(err.response?.data || 'Erreur impression', 'error'));
    }

    handlePrintBoxLabel = () => {
        const { session } = this.state;
        if (!session) return;
        axios.post(`/api/cncPs/session/${session.id}/printBoxLabel`)
            .then(res => this.showMessage(res.data || 'Étiquette boîte imprimée', 'success'))
            .catch(err => this.showMessage(err.response?.data || 'Erreur impression', 'error'));
    }

    handleDeleteConsumption = (consumptionId) => {
        if (!window.confirm('Supprimer cette consommation ?')) return;
        axios.delete(`/api/cncPs/consumption/${consumptionId}`)
            .then(() => {
                this.showMessage('Consommation supprimée', 'success');
                this.refreshSession(this.state.session.id);
            })
            .catch(err => this.showMessage(err.response?.data || 'Erreur suppression', 'error'));
    }

    handleNewSession = () => {
        this.setState({
            boxId: '',
            boxDetails: null,
            session: null,
            leatherPartNumber: '',
            serial: '',
            lot: '',
            quantiteInitial: '',
            patterns: [],
            expandedIdx: null,
        });
    }

    showMessage = (message, type) => {
        this.setState({ message, messageType: type });
        setTimeout(() => this.setState({ message: null, messageType: null }), 5000);
    }

    formatDateTime = (dt) => {
        if (!dt) return '-';
        return new Date(dt).toLocaleString('fr-FR');
    }

    getBoxQuantity = () => {
        const { boxDetails, session } = this.state;
        const qStr = session ? session.quantiteImp : (boxDetails ? boxDetails.quantiteImp : null);
        if (!qStr) return 0;
        try { return parseFloat(qStr); } catch (e) { return 0; }
    }

    getTotalConsumed = () => {
        const { session } = this.state;
        if (!session || !session.consumptions) return 0;
        return session.consumptions.reduce((sum, c) => sum + (c.quantiteConsumed || 0), 0);
    }

    getRemainingQuantity = () => {
        return this.getBoxQuantity() - this.getTotalConsumed();
    }

    hasRetourConsumptions = () => {
        const { session } = this.state;
        if (!session || !session.consumptions) return false;
        return session.consumptions.some(c => c.quantiteRetour && c.quantiteRetour > 0);
    }

    // Alphanumeric validation: only allows a-z, A-Z, 0-9
    handleAlphanumericChange = (field) => (e) => {
        const val = e.target.value.replace(/[^a-zA-Z0-9]/g, '');
        this.setState({ [field]: val });
    }

    // onKeyUp Enter for PN Cuir: validate then move to next
    handlePnCuirEnter = (e) => {
        if (e.key === 'Enter') {
            const val = e.target.value.trim();
            if (!val) return;
            if (!this.validatePnCuir(val)) {
                const { session } = this.state;
                this.showMessage(`Le PN Cuir doit correspondre au Code1 (${session.code1Imp}) ou être P${session.code1Imp}`, 'error');
                return;
            }
            if (this.serialRef && this.serialRef.current) {
                this.serialRef.current.focus();
            }
        }
    }

    // onKeyUp Enter for Serial: search if already used
    handleSerialEnter = (e) => {
        if (e.key === 'Enter') {
            const val = e.target.value.trim();
            if (!val) return;
            const { leatherPartNumber } = this.state;
            // Search for previous consumptions with this serial
            axios.get(`/api/cncPs/serialCheck/${val}`)
                .then(res => {
                    const data = res.data;
                    if (data.found) {
                        if (data.lastRetour <= 0) {
                            // Serial fully consumed
                            this.showMessage(`Le serial ${val} a déjà été entièrement consommé (retour = 0)`, 'error');
                            return;
                        }
                        // Check if PN Cuir matches
                        if (leatherPartNumber.trim() && data.lastLeatherPartNumber &&
                            leatherPartNumber.trim().toUpperCase() !== data.lastLeatherPartNumber.toUpperCase()) {
                            this.showMessage(`Le PN Cuir saisi (${leatherPartNumber}) ne correspond pas au dernier PN Cuir utilisé (${data.lastLeatherPartNumber})`, 'error');
                            return;
                        }
                        // Auto-fill lot and quantiteInitial from last retour
                        this.setState({
                            lot: data.lastLot || '',
                            quantiteInitial: data.lastRetour != null ? Math.trunc(data.lastRetour).toString() : '',
                        });
                        this.showMessage(`Serial trouvé - Lot: ${data.lastLot}, Qté Retour précédente: ${data.lastRetour}`, 'success');
                        if (this.lotRef && this.lotRef.current) {
                            this.lotRef.current.focus();
                        }
                    } else {
                        // Serial not found - new serial, proceed normally
                        if (this.lotRef && this.lotRef.current) {
                            this.lotRef.current.focus();
                        }
                    }
                })
                .catch(() => {
                    // If API not available, just move to next
                    if (this.lotRef && this.lotRef.current) {
                        this.lotRef.current.focus();
                    }
                });
        }
    }

    // onKeyUp Enter for Lot: auto-prefix 'H' when it starts with a digit, then move to qty
    handleLotEnter = (e) => {
        if (e.key === 'Enter') {
            let val = this.state.lot.trim();
            if (!val) return;
            if (/^[0-9]/.test(val)) {
                val = 'H' + val;
                this.setState({ lot: val });
            }
            if (this.qteInitRef && this.qteInitRef.current) {
                this.qteInitRef.current.focus();
            }
        }
    }

    // Refs for input navigation
    leatherPnRef = React.createRef();
    serialRef = React.createRef();
    lotRef = React.createRef();
    qteInitRef = React.createRef();

    render() {
        const {
            boxId, boxLoading,
            session,
            leatherPartNumber, serial, lot, quantiteInitial, consumeLoading,
            patterns,
            recentSessions, historyLoading,
            printerConnected, printerIp, printerChecking,
            message, messageType,
        } = this.state;

        const consumptions = session ? (session.consumptions || []) : [];
        const boxQty = this.getBoxQuantity();
        const totalConsumed = this.getTotalConsumed();
        const remaining = this.getRemainingQuantity();
        const hasLeather = session && session.code1Imp && session.code1Imp.trim();
        const canAddConsumption = session && !session.completed && remaining > 0 && hasLeather;
        const canComplete = session && !session.completed && (
            hasLeather
                ? (consumptions.length > 0 && Math.abs(totalConsumed - boxQty) < 0.01)
                : true
        );

        return (
            <div className="container-fluid mt-3">
                <div className="d-flex justify-content-between align-items-center mb-3">
                    <h2><FontAwesomeIcon icon={faBoxArchive} /> CNC PS - Consommation Cuir</h2>
                    <div className="d-flex align-items-center">
                        <span className={`badge badge-${printerConnected ? 'success' : 'danger'} mr-2 p-2`}
                              style={{ cursor: 'pointer' }} onClick={this.checkPrinterStatus}
                              title={this.state.printerMessage || ''}>
                            {printerChecking ? <FontAwesomeIcon icon={faSpinner} spin /> : <FontAwesomeIcon icon={faWifi} />}
                            {' '}{printerConnected
                                ? (this.state.printerNetworkConnected
                                    ? `Réseau (${printerIp})`
                                    : `Local ZEBRA (${this.state.printerLocalZebraName || ''})`)
                                : 'Imprimante Déconnectée'}
                        </span>
                    </div>
                </div>

                {message && (
                    <div className={`alert alert-${messageType === 'success' ? 'success' : messageType === 'warning' ? 'warning' : 'danger'} d-flex align-items-center`}>
                        <FontAwesomeIcon icon={messageType === 'success' ? faCheckCircle : messageType === 'warning' ? faExclamationTriangle : faTimesCircle} className="mr-2" />
                        {' '}{message}
                    </div>
                )}

                {/* FORM SECTION - Full width */}
                <div className="row">
                    <div className="col-12">
                        {/* Scan box → consumption shows directly (no session step) */}
                        {!session && (
                            <div className="card mb-3">
                                <div className="card-header bg-primary text-white">
                                    <strong>Scanner la boîte</strong>
                                </div>
                                <div className="card-body">
                                    <div className="form-group mb-2">
                                        <label>ID Boîte (scanner ou saisir)</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={boxId}
                                            onChange={e => this.setState({ boxId: e.target.value, boxDetails: null, patterns: [] })}
                                            onKeyPress={this.handleBoxIdScan}
                                            placeholder="Ex: S12345 ou 12345"
                                            autoFocus
                                        />
                                        <small className="text-muted">Appuyez sur Entrée pour afficher la consommation cuir</small>
                                    </div>
                                    <button
                                        className="btn btn-outline-primary btn-sm"
                                        onClick={this.handleBoxIdScan}
                                        disabled={boxLoading || !boxId.trim()}
                                    >
                                        {boxLoading ? <FontAwesomeIcon icon={faSpinner} spin /> : 'Rechercher'}
                                    </button>
                                </div>
                            </div>
                        )}

                        {/* Box scanned: leather consumption */}
                        {session && (
                            <div className="card mb-3">
                                <div className={`card-header ${session.completed ? 'bg-success' : 'bg-warning'} text-white d-flex justify-content-between`}>
                                    <strong><FontAwesomeIcon icon={faBoxArchive} /> Boîte {session.boxId}</strong>
                                    <span>{session.completed ? 'Terminée ✓' : 'En cours'}</span>
                                </div>
                                <div className="card-body">
                                    <div className="row">
                                        {/* Box details */}
                                        <div className="col-md-4">
                                            <h6>Détails Boîte</h6>
                                            <table className="table table-sm table-bordered mb-2">
                                                <tbody>
                                                    <tr><td><strong>Boîte ID</strong></td><td>{session.boxId}</td></tr>
                                                    <tr><td><strong>Séquence</strong></td><td>{session.nSequenceImp}</td></tr>
                                                    <tr><td><strong>Part Number</strong></td><td>{session.partNumberImp}</td></tr>
                                                    <tr><td><strong>Code3</strong></td><td>{session.code3Imp}</td></tr>
                                                    <tr><td><strong>Quantité Boîte</strong></td><td><strong>{session.quantiteImp}</strong></td></tr>
                                                </tbody>
                                            </table>

                                            {/* Quantity progress */}
                                            <div className="mb-2">
                                                <div className="d-flex justify-content-between">
                                                    <small>Consommé: <strong>{totalConsumed.toFixed(2)}</strong></small>
                                                    <small>Restant: <strong className={remaining > 0 ? 'text-warning' : 'text-success'}>{remaining.toFixed(2)}</strong></small>
                                                </div>
                                                <div className="progress" style={{ height: '20px' }}>
                                                    <div className={`progress-bar ${Math.abs(remaining) < 0.01 ? 'bg-success' : 'bg-warning'}`}
                                                         style={{ width: `${boxQty > 0 ? Math.min((totalConsumed / boxQty) * 100, 100) : 0}%` }}>
                                                        {boxQty > 0 ? `${((totalConsumed / boxQty) * 100).toFixed(0)}%` : '0%'}
                                                    </div>
                                                </div>
                                            </div>

                                            {/* Patterns */}
                                            {patterns.length > 0 && (
                                                <div className="mb-2">
                                                    <h6>Patterns CNC</h6>
                                                    <table className="table table-sm table-bordered">
                                                        <thead className="thead-light">
                                                            <tr><th style={{ width: 28 }}></th><th>Panel</th><th>Pattern</th><th>Prog</th><th>Cas.</th><th>Ver.</th><th>Row</th><th>Set</th><th>Fil Couture CNC</th><th>Cavité Press</th><th>Fil blind</th><th>Profil</th><th>Type</th></tr>
                                                        </thead>
                                                        <tbody>
                                                            {patterns.map((p, i) => (
                                                                <React.Fragment key={i}>
                                                                    <tr>
                                                                        <td className="text-center">
                                                                            <button className="btn btn-link btn-sm p-0" title="Voir images (cuir / fil couture)"
                                                                                    onClick={() => this.setState({ expandedIdx: this.state.expandedIdx === i ? null : i })}>
                                                                                <FontAwesomeIcon icon={faImage} />
                                                                            </button>
                                                                        </td>
                                                                        <td>{p.panelNumber || '-'}</td>
                                                                        <td>{p.pattern}</td>
                                                                        <td>{p.programNumber || '-'}</td>
                                                                        <td>{p.casette || '-'}</td>
                                                                        <td>{p.version || '-'}</td>
                                                                        <td>{p.row || '-'}</td>
                                                                        <td>{p.set || '-'}</td>
                                                                        <td>{p.coutureDecorativeCnc || '-'}</td>
                                                                        <td>{p.cavitePress || '-'}</td>
                                                                        <td>{p.blindStitch || '-'}</td>
                                                                        <td>{p.profil || '-'}</td>
                                                                        <td>{p.type || '-'}</td>
                                                                    </tr>
                                                                    {this.state.expandedIdx === i && (
                                                                        <tr>
                                                                            <td colSpan={13} className="bg-light">
                                                                                <CncPatternImages leatherPn={session.code1Imp} filCouture={p.coutureDecorativeCnc} />
                                                                            </td>
                                                                        </tr>
                                                                    )}
                                                                </React.Fragment>
                                                            ))}
                                                        </tbody>
                                                    </table>
                                                </div>
                                            )}
                                        </div>

                                        {/* Consumption form + list */}
                                        <div className="col-md-8">
                                            {/* Add consumption */}
                                            {canAddConsumption && (
                                                <div className="border rounded p-3 mb-3 bg-light">
                                                    <h6>Ajouter Consommation Cuir</h6>
                                                    <div className="row">
                                                        <div className="col-md-3 form-group mb-2">
                                                            <label className="small">PN Cuir</label>
                                                            <input
                                                                type="text"
                                                                className="form-control form-control-sm"
                                                                ref={this.leatherPnRef}
                                                                value={leatherPartNumber}
                                                                onChange={this.handleAlphanumericChange('leatherPartNumber')}
                                                                onKeyUp={this.handlePnCuirEnter}
                                                                placeholder="Part Number Cuir"
                                                            />
                                                        </div>
                                                        <div className="col-md-2 form-group mb-2">
                                                            <label className="small">Serial</label>
                                                            <input
                                                                type="text"
                                                                className="form-control form-control-sm"
                                                                ref={this.serialRef}
                                                                value={serial}
                                                                onChange={this.handleAlphanumericChange('serial')}
                                                                onKeyUp={this.handleSerialEnter}
                                                                placeholder="Serial"
                                                            />
                                                        </div>
                                                        <div className="col-md-2 form-group mb-2">
                                                            <label className="small">Lot (H...)</label>
                                                            <input
                                                                type="text"
                                                                className="form-control form-control-sm"
                                                                ref={this.lotRef}
                                                                value={lot}
                                                                onChange={this.handleAlphanumericChange('lot')}
                                                                onKeyUp={this.handleLotEnter}
                                                                placeholder="Ex: H20240101"
                                                            />
                                                        </div>
                                                        <div className="col-md-2 form-group mb-2">
                                                            <label className="small">Qté Initiale</label>
                                                            <input
                                                                type="text"
                                                                inputMode="numeric"
                                                                className="form-control form-control-sm"
                                                                ref={this.qteInitRef}
                                                                value={quantiteInitial}
                                                                onChange={e => this.setState({ quantiteInitial: e.target.value.replace(/[^0-9]/g, '') })}
                                                                onKeyUp={(e) => { if (e.key === 'Enter' && e.target.value.trim()) this.handleAddConsumption(); }}
                                                                placeholder="Qté initiale"
                                                            />
                                                        </div>
                                                        <div className="col-md-3 form-group mb-2 d-flex align-items-end">
                                                            <button
                                                                className="btn btn-primary btn-sm w-100"
                                                                onClick={this.handleAddConsumption}
                                                                disabled={consumeLoading}
                                                            >
                                                                {consumeLoading
                                                                    ? <FontAwesomeIcon icon={faSpinner} spin />
                                                                    : <><FontAwesomeIcon icon={faPlus} /> Ajouter</>}
                                                            </button>
                                                        </div>
                                                    </div>
                                                    {quantiteInitial && (
                                                        <div className="small text-muted mt-1">
                                                            Qté Consommée (auto): <strong>{Math.min(remaining, parseInt(quantiteInitial, 10) || 0).toFixed(2)}</strong>
                                                            {' | '}Qté Retour (auto): <strong>{Math.max(0, (parseInt(quantiteInitial, 10) || 0) - Math.min(remaining, parseInt(quantiteInitial, 10) || 0)).toFixed(2)}</strong>
                                                        </div>
                                                    )}
                                                </div>
                                            )}

                                            {/* Consumption list */}
                                            {consumptions.length > 0 && (
                                                <div className="mb-3">
                                                    <h6>Cuirs consommés ({consumptions.length}):</h6>
                                                    <div className="table-responsive">
                                                        <table className="table table-sm table-hover table-bordered">
                                                            <thead className="thead-dark">
                                                                <tr>
                                                                    <th>PN Cuir</th>
                                                                    <th>Serial</th>
                                                                    <th>Lot</th>
                                                                    <th>Qté Init.</th>
                                                                    <th>Qté Cons.</th>
                                                                    <th>Qté Retour</th>
                                                                    <th></th>
                                                                </tr>
                                                            </thead>
                                                            <tbody>
                                                                {consumptions.map(c => (
                                                                    <tr key={c.id} className={c.quantiteRetour > 0 ? 'table-warning' : ''}>
                                                                        <td>{c.leatherPartNumber}</td>
                                                                        <td>{c.serial}</td>
                                                                        <td>{c.lot}</td>
                                                                        <td>{c.quantiteInitial}</td>
                                                                        <td><strong>{c.quantiteConsumed}</strong></td>
                                                                        <td>{c.quantiteRetour > 0 ? <span className="text-danger font-weight-bold">{c.quantiteRetour}</span> : '0'}</td>
                                                                        <td>
                                                                            {!session.completed && (
                                                                                <button
                                                                                    className="btn btn-danger btn-sm py-0"
                                                                                    onClick={() => this.handleDeleteConsumption(c.id)}
                                                                                >
                                                                                    <FontAwesomeIcon icon={faTrash} />
                                                                                </button>
                                                                            )}
                                                                        </td>
                                                                    </tr>
                                                                ))}
                                                            </tbody>
                                                        </table>
                                                    </div>
                                                </div>
                                            )}

                                            {/* Warning if total consumed != box qty */}
                                            {consumptions.length > 0 && remaining > 0.01 && !session.completed && (
                                                <div className="alert alert-warning py-2">
                                                    <FontAwesomeIcon icon={faExclamationTriangle} /> Il reste <strong>{remaining.toFixed(2)}</strong> à consommer
                                                    pour pouvoir terminer.
                                                </div>
                                            )}

                                            {/* Actions */}
                                            <div className="d-flex flex-wrap gap-2">
                                                {canComplete && (
                                                    <button className="btn btn-success btn-sm mr-2 mb-1" onClick={this.handleCompleteSession}>
                                                        <FontAwesomeIcon icon={faCheckCircle} /> Terminer
                                                    </button>
                                                )}
                                                {this.hasRetourConsumptions() && (
                                                    <button className="btn btn-warning btn-sm mr-2 mb-1" onClick={this.handlePrintLeatherReturn}
                                                            disabled={!printerConnected}>
                                                        <FontAwesomeIcon icon={faPrint} /> Imprimer retour cuir
                                                    </button>
                                                )}
                                                {session.completed && (
                                                    <button className="btn btn-info btn-sm mr-2 mb-1" onClick={this.handlePrintBoxLabel}
                                                            disabled={!printerConnected}>
                                                        <FontAwesomeIcon icon={faPrint} /> Imprimer étiquette boîte
                                                    </button>
                                                )}
                                                <button className="btn btn-outline-secondary btn-sm mb-1" onClick={this.handleNewSession}>
                                                    Scanner une autre boîte
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                {/* HISTORY TABLE - Full width under form */}
                <div className="row">
                    <div className="col-12">
                        <div className="card">
                            <div className="card-header bg-secondary text-white d-flex justify-content-between align-items-center">
                                <strong>Mes boîtes (8 dernières heures)</strong>
                                <button className="btn btn-sm btn-light" onClick={this.loadRecentHistory}>↻</button>
                            </div>
                            <div className="card-body p-0">
                                {historyLoading ? (
                                    <div className="text-center p-3"><FontAwesomeIcon icon={faSpinner} spin /></div>
                                ) : (
                                    <div className="table-responsive">
                                        <table className="table table-sm table-striped mb-0">
                                            <thead>
                                                <tr>
                                                    <th>#</th>
                                                    <th>Boîte</th>
                                                    <th>Séquence</th>
                                                    <th>PN</th>
                                                    <th>Cuirs</th>
                                                    <th>Date</th>
                                                    <th>Statut</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {recentSessions.length === 0 ? (
                                                    <tr><td colSpan="7" className="text-center text-muted py-3">Aucune boîte dans les 8 dernières heures</td></tr>
                                                ) : recentSessions.map(s => (
                                                    <tr key={s.id} style={{ cursor: 'pointer' }}
                                                        onClick={() => {
                                                            this.setState({ session: s, boxId: s.boxId, expandedIdx: null });
                                                            // Reload patterns
                                                            if (s.boxId) {
                                                                axios.get(`/api/cncPs/boxDetails/${s.boxId}`)
                                                                    .then(res => this.setState({ patterns: res.data.patterns || [] }))
                                                                    .catch(() => {});
                                                            }
                                                        }}>
                                                        <td>{s.id}</td>
                                                        <td><strong>{s.boxId}</strong></td>
                                                        <td>{s.nSequenceImp}</td>
                                                        <td>{s.partNumberImp}</td>
                                                        <td>{s.consumptions ? s.consumptions.length : '-'}</td>
                                                        <td>{this.formatDateTime(s.createdAt)}</td>
                                                        <td>
                                                            <span className={`badge badge-${s.completed ? 'success' : 'warning'}`}>
                                                                {s.completed ? 'Terminée' : 'En cours'}
                                                            </span>
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

const mapStateToProps = state => ({ security: state.security });
export default connect(mapStateToProps)(CncPs);
