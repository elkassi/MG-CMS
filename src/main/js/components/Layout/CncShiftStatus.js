import React, { Component } from 'react';
import axios from 'axios';
import { connect } from 'react-redux';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSpinner, faClock, faCheckCircle, faTimesCircle, faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';

class CncShiftStatus extends Component {
    constructor(props) {
        super(props);
        const now = new Date();
        this.state = {
            date: now.toISOString().slice(0, 10),
            shift: this.detectCurrentShift(now),
            data: null,
            loading: false,
            message: null,
        };
    }

    componentDidMount() {
        this.loadShiftStatus();
    }

    detectCurrentShift = (now) => {
        const h = now.getHours();
        const m = now.getMinutes();
        const totalMins = h * 60 + m;
        // shift1: 21:50 → 05:50, shift2: 05:50 → 13:50, shift3: 13:50 → 21:50
        if (totalMins >= 350 && totalMins < 830) return 2;       // 05:50 → 13:50
        if (totalMins >= 830 && totalMins < 1310) return 3;      // 13:50 → 21:50
        return 1;                                                  // 21:50 → 05:50
    }

    loadShiftStatus = () => {
        const { date, shift } = this.state;
        this.setState({ loading: true });
        axios.get(`/api/cncPs/shiftStatus?date=${date}&shift=${shift}`)
            .then(res => this.setState({ data: res.data, loading: false }))
            .catch(err => {
                this.setState({ loading: false, data: null });
                this.setState({ message: err.response?.data || 'Erreur chargement' });
                setTimeout(() => this.setState({ message: null }), 4000);
            });
    }

    getShiftLabel = (s) => {
        if (s === 1) return 'Shift 1 (21:50 → 05:50)';
        if (s === 2) return 'Shift 2 (05:50 → 13:50)';
        return 'Shift 3 (13:50 → 21:50)';
    }

    formatDateTime = (dt) => {
        if (!dt) return '-';
        return new Date(dt).toLocaleString('fr-FR');
    }

    render() {
        const { date, shift, data, loading, message } = this.state;

        return (
            <div className="container-fluid mt-3">
                <div className="d-flex justify-content-between align-items-center mb-3">
                    <h2><FontAwesomeIcon icon={faClock} /> Statut Shift CNC</h2>
                </div>

                {message && <div className="alert alert-danger">{message}</div>}

                {/* Filter bar */}
                <div className="card mb-3">
                    <div className="card-body py-2">
                        <div className="row align-items-end">
                            <div className="col-md-3 form-group mb-1">
                                <label className="small">Date</label>
                                <input type="date" className="form-control form-control-sm" value={date}
                                       onChange={e => this.setState({ date: e.target.value })} />
                            </div>
                            <div className="col-md-3 form-group mb-1">
                                <label className="small">Shift</label>
                                <select className="form-control form-control-sm" value={shift}
                                        onChange={e => this.setState({ shift: parseInt(e.target.value) })}>
                                    <option value={1}>Shift 1 (21:50 → 05:50)</option>
                                    <option value={2}>Shift 2 (05:50 → 13:50)</option>
                                    <option value={3}>Shift 3 (13:50 → 21:50)</option>
                                </select>
                            </div>
                            <div className="col-md-2 form-group mb-1">
                                <button className="btn btn-primary btn-sm w-100" onClick={this.loadShiftStatus}
                                        disabled={loading}>
                                    {loading ? <FontAwesomeIcon icon={faSpinner} spin /> : 'Charger'}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                {loading && <div className="text-center p-4"><FontAwesomeIcon icon={faSpinner} spin size="2x" /></div>}

                {data && !loading && (
                    <>
                        <div className="mb-2 text-muted">{this.getShiftLabel(shift)} — {date}</div>

                        {/* KPI cards */}
                        <div className="row mb-3">
                            <div className="col-md-2">
                                <div className="card text-center">
                                    <div className="card-body py-2">
                                        <h3 className="mb-0 text-primary">{data.totalSessions || 0}</h3>
                                        <small>Sessions</small>
                                    </div>
                                </div>
                            </div>
                            <div className="col-md-2">
                                <div className="card text-center">
                                    <div className="card-body py-2">
                                        <h3 className="mb-0 text-success">{data.completedSessions || 0}</h3>
                                        <small>Complétées</small>
                                    </div>
                                </div>
                            </div>
                            <div className="col-md-2">
                                <div className="card text-center">
                                    <div className="card-body py-2">
                                        <h3 className="mb-0 text-warning">{data.inProgressSessions || 0}</h3>
                                        <small>En cours</small>
                                    </div>
                                </div>
                            </div>
                            <div className="col-md-2">
                                <div className="card text-center">
                                    <div className="card-body py-2">
                                        <h3 className="mb-0 text-secondary">{data.waitingSessions || 0}</h3>
                                        <small>En attente</small>
                                    </div>
                                </div>
                            </div>
                            <div className="col-md-2">
                                <div className="card text-center">
                                    <div className="card-body py-2">
                                        <h3 className="mb-0">{data.totalBoxQuantity || 0}</h3>
                                        <small>Qté Boîtes</small>
                                    </div>
                                </div>
                            </div>
                            <div className="col-md-2">
                                <div className="card text-center">
                                    <div className="card-body py-2">
                                        <h3 className="mb-0">
                                            <span className="text-success">{data.totalControlOK || 0}</span>{' / '}
                                            <span className="text-danger">{data.totalControlNOK || 0}</span>
                                        </h3>
                                        <small>OK / NOK</small>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* By Operator */}
                        {data.byOperator && Object.keys(data.byOperator).length > 0 && (
                            <div className="card mb-3">
                                <div className="card-header bg-info text-white"><strong>Par Opérateur</strong></div>
                                <div className="card-body p-0">
                                    <table className="table table-sm table-bordered mb-0">
                                        <thead className="thead-light">
                                            <tr><th>Opérateur</th><th>Sessions</th></tr>
                                        </thead>
                                        <tbody>
                                            {Object.entries(data.byOperator).map(([op, count]) => (
                                                <tr key={op}>
                                                    <td>{op}</td>
                                                    <td>{count}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}

                        {/* By PN */}
                        {data.byPartNumber && Object.keys(data.byPartNumber).length > 0 && (
                            <div className="card mb-3">
                                <div className="card-header bg-secondary text-white"><strong>Par Part Number</strong></div>
                                <div className="card-body p-0">
                                    <table className="table table-sm table-bordered mb-0">
                                        <thead className="thead-light">
                                            <tr><th>Part Number</th><th>Sessions</th></tr>
                                        </thead>
                                        <tbody>
                                            {Object.entries(data.byPartNumber).map(([pn, count]) => (
                                                <tr key={pn}>
                                                    <td>{pn}</td>
                                                    <td>{count}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}

                        {/* Sessions list */}
                        {data.sessions && data.sessions.length > 0 && (
                            <div className="card">
                                <div className="card-header bg-dark text-white"><strong>Sessions du Shift</strong></div>
                                <div className="card-body p-0">
                                    <div className="table-responsive">
                                        <table className="table table-sm table-hover table-bordered mb-0">
                                            <thead className="thead-light">
                                                <tr>
                                                    <th>ID</th>
                                                    <th>Box ID</th>
                                                    <th>PN</th>
                                                    <th>Qté</th>
                                                    <th>Op. PS</th>
                                                    <th>Statut PS</th>
                                                    <th>Statut Prod.</th>
                                                    <th>Op. Prod.</th>
                                                    <th>Machine</th>
                                                    <th>Créé le</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {data.sessions.map(s => (
                                                    <tr key={s.id}>
                                                        <td>{s.id}</td>
                                                        <td><strong>{s.boxId}</strong></td>
                                                        <td>{s.partNumberImp}</td>
                                                        <td>{s.quantiteImp}</td>
                                                        <td>{s.operator || '-'}</td>
                                                        <td>
                                                            <span className={`badge badge-${s.completed ? 'success' : 'warning'}`}>
                                                                {s.completed ? 'OK' : '...'}
                                                            </span>
                                                        </td>
                                                        <td>
                                                            <span className={`badge badge-${s.productionStatus === 'Complete' ? 'success' : s.productionStatus === 'In progress' ? 'info' : 'secondary'}`}>
                                                                {s.productionStatus || 'Waiting'}
                                                            </span>
                                                        </td>
                                                        <td>{s.productionOperator || '-'}</td>
                                                        <td>{s.machineCnc ? s.machineCnc.name : '-'}</td>
                                                        <td className="small">{this.formatDateTime(s.createdAt)}</td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                        )}
                    </>
                )}
            </div>
        );
    }
}

const mapStateToProps = state => ({ security: state.security });
export default connect(mapStateToProps)(CncShiftStatus);
