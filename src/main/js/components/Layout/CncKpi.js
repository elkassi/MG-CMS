import React, { Component } from 'react';
import axios from 'axios';
import { connect } from 'react-redux';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSpinner, faChartBar, faCheckCircle, faTimesCircle } from '@fortawesome/free-solid-svg-icons';

class CncKpi extends Component {
    constructor(props) {
        super(props);
        const today = new Date();
        const weekAgo = new Date(today);
        weekAgo.setDate(weekAgo.getDate() - 7);
        this.state = {
            startDate: weekAgo.toISOString().slice(0, 10),
            endDate: today.toISOString().slice(0, 10),
            data: null,
            loading: false,
            message: null,
        };
    }

    componentDidMount() {
        this.loadKpi();
    }

    loadKpi = () => {
        const { startDate, endDate } = this.state;
        this.setState({ loading: true });
        axios.get(`/api/cncPs/kpi?startDate=${startDate}&endDate=${endDate}`)
            .then(res => this.setState({ data: res.data, loading: false }))
            .catch(err => {
                this.setState({ loading: false, data: null });
                this.setState({ message: err.response?.data || 'Erreur chargement KPI' });
                setTimeout(() => this.setState({ message: null }), 4000);
            });
    }

    render() {
        const { startDate, endDate, data, loading, message } = this.state;

        return (
            <div className="container-fluid mt-3">
                <div className="d-flex justify-content-between align-items-center mb-3">
                    <h2><FontAwesomeIcon icon={faChartBar} /> KPI CNC</h2>
                </div>

                {message && <div className="alert alert-danger">{message}</div>}

                {/* Date Range */}
                <div className="card mb-3">
                    <div className="card-body py-2">
                        <div className="row align-items-end">
                            <div className="col-md-3 form-group mb-1">
                                <label className="small">Date début</label>
                                <input type="date" className="form-control form-control-sm" value={startDate}
                                       onChange={e => this.setState({ startDate: e.target.value })} />
                            </div>
                            <div className="col-md-3 form-group mb-1">
                                <label className="small">Date fin</label>
                                <input type="date" className="form-control form-control-sm" value={endDate}
                                       onChange={e => this.setState({ endDate: e.target.value })} />
                            </div>
                            <div className="col-md-2 form-group mb-1">
                                <button className="btn btn-primary btn-sm w-100" onClick={this.loadKpi} disabled={loading}>
                                    {loading ? <FontAwesomeIcon icon={faSpinner} spin /> : 'Charger'}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                {loading && <div className="text-center p-4"><FontAwesomeIcon icon={faSpinner} spin size="2x" /></div>}

                {data && !loading && (
                    <>
                        {/* Top KPI cards */}
                        <div className="row mb-3">
                            <div className="col-md-3">
                                <div className="card text-center border-primary">
                                    <div className="card-body py-2">
                                        <h2 className={`mb-0 ${(data.qualityRate || 0) >= 95 ? 'text-success' : (data.qualityRate || 0) >= 85 ? 'text-warning' : 'text-danger'}`}>
                                            {data.qualityRate != null ? `${data.qualityRate.toFixed(1)}%` : '-'}
                                        </h2>
                                        <small className="text-muted">Taux Qualité</small>
                                    </div>
                                </div>
                            </div>
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
                                        <h3 className="mb-0">{data.totalBoxQuantity || 0}</h3>
                                        <small>Qté Boîtes</small>
                                    </div>
                                </div>
                            </div>
                            <div className="col-md-2">
                                <div className="card text-center">
                                    <div className="card-body py-2">
                                        <h3 className="mb-0 text-success">{data.totalControlOK || 0}</h3>
                                        <small>Total OK</small>
                                    </div>
                                </div>
                            </div>
                            <div className="col-md-2">
                                <div className="card text-center">
                                    <div className="card-body py-2">
                                        <h3 className="mb-0 text-danger">{data.totalControlNOK || 0}</h3>
                                        <small>Total NOK</small>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="row">
                            {/* By Day */}
                            {data.byDay && Object.keys(data.byDay).length > 0 && (
                                <div className="col-md-6 mb-3">
                                    <div className="card h-100">
                                        <div className="card-header bg-primary text-white"><strong>Par Jour</strong></div>
                                        <div className="card-body p-0">
                                            <table className="table table-sm table-bordered mb-0">
                                                <thead className="thead-light">
                                                    <tr><th>Date</th><th>Sessions</th></tr>
                                                </thead>
                                                <tbody>
                                                    {Object.entries(data.byDay).sort(([a],[b]) => a.localeCompare(b)).map(([day, count]) => (
                                                        <tr key={day}>
                                                            <td>{day}</td>
                                                            <td>{count}</td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* By PN */}
                            {data.byPartNumber && Object.keys(data.byPartNumber).length > 0 && (
                                <div className="col-md-6 mb-3">
                                    <div className="card h-100">
                                        <div className="card-header bg-secondary text-white"><strong>Par Part Number</strong></div>
                                        <div className="card-body p-0">
                                            <table className="table table-sm table-bordered mb-0">
                                                <thead className="thead-light">
                                                    <tr><th>PN</th><th>Sessions</th></tr>
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
                                </div>
                            )}

                            {/* By Operator */}
                            {data.byOperator && Object.keys(data.byOperator).length > 0 && (
                                <div className="col-md-6 mb-3">
                                    <div className="card h-100">
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
                                </div>
                            )}

                            {/* By Machine */}
                            {data.byMachine && Object.keys(data.byMachine).length > 0 && (
                                <div className="col-md-6 mb-3">
                                    <div className="card h-100">
                                        <div className="card-header bg-dark text-white"><strong>Par Machine</strong></div>
                                        <div className="card-body p-0">
                                            <table className="table table-sm table-bordered mb-0">
                                                <thead className="thead-light">
                                                    <tr><th>Machine</th><th>Sessions</th></tr>
                                                </thead>
                                                <tbody>
                                                    {Object.entries(data.byMachine).map(([m, count]) => (
                                                        <tr key={m}>
                                                            <td>{m}</td>
                                                            <td>{count}</td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* By Defaut */}
                            {data.defautBreakdown && Object.keys(data.defautBreakdown).length > 0 && (
                                <div className="col-md-6 mb-3">
                                    <div className="card h-100">
                                        <div className="card-header bg-warning"><strong>Par Code Défaut</strong></div>
                                        <div className="card-body p-0">
                                            <table className="table table-sm table-bordered mb-0">
                                                <thead className="thead-light">
                                                    <tr><th>Code Défaut</th><th>Count</th></tr>
                                                </thead>
                                                <tbody>
                                                    {Object.entries(data.defautBreakdown).sort(([,a],[,b]) => b - a).map(([code, count]) => (
                                                        <tr key={code}>
                                                            <td>{code}</td>
                                                            <td><strong>{count}</strong></td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* By Scrap */}
                            {data.scrapBreakdown && Object.keys(data.scrapBreakdown).length > 0 && (
                                <div className="col-md-6 mb-3">
                                    <div className="card h-100">
                                        <div className="card-header bg-danger text-white"><strong>Par Code Scrap</strong></div>
                                        <div className="card-body p-0">
                                            <table className="table table-sm table-bordered mb-0">
                                                <thead className="thead-light">
                                                    <tr><th>Code Scrap</th><th>Count</th></tr>
                                                </thead>
                                                <tbody>
                                                    {Object.entries(data.scrapBreakdown).sort(([,a],[,b]) => b - a).map(([code, count]) => (
                                                        <tr key={code}>
                                                            <td>{code}</td>
                                                            <td><strong>{count}</strong></td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    </>
                )}
            </div>
        );
    }
}

const mapStateToProps = state => ({ security: state.security });
export default connect(mapStateToProps)(CncKpi);
