import React, { Component } from 'react';
import axios from 'axios';
import { connect } from 'react-redux';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSpinner, faCheckCircle, faTimesCircle, faTrash, faUndo, faSearch, faExclamationTriangle, faShieldAlt, faEye } from '@fortawesome/free-solid-svg-icons';
import CncControl from './CncControl';

const fmt = (dt) => (dt ? new Date(dt).toLocaleString('fr-FR') : '-');

// Every data column is sortable (click the header) and filterable (header input).
//   text():    value used for the per-column filter and the default string sort.
//   sortVal(): optional comparable for correct numeric / chronological sort.
//   render():  optional JSX for display (defaults to text()).
//   cls:       optional <td> className.
const COLUMNS = [
    { key: 'id', label: 'ID', text: s => String(s.id ?? ''), sortVal: s => s.id ?? 0 },
    { key: 'boxId', label: 'Box ID', text: s => s.boxId || '', render: s => <strong>{s.boxId}</strong> },
    { key: 'partNumberImp', label: 'PN', text: s => s.partNumberImp || '' },
    { key: 'quantiteImp', label: 'Qté', text: s => String(s.quantiteImp ?? ''), sortVal: s => Number(s.quantiteImp) || 0 },
    { key: 'operator', label: 'Opérateur PS', text: s => s.operator || '', render: s => s.operator || '-' },
    { key: 'code1Imp', label: 'Cuir', text: s => s.code1Imp || '', render: s => s.code1Imp || <span className="text-muted">-</span> },
    { key: 'completed', label: 'Statut PS', text: s => (s.completed ? 'Complétée' : 'En cours'),
      render: s => <span className={`badge badge-${s.completed ? 'success' : 'warning'}`}>{s.completed ? 'Complétée' : 'En cours'}</span> },
    { key: 'qualiteStatus', label: 'Statut Qualité', text: s => s.qualiteStatus || 'Non démarré',
      render: s => <span className={`badge badge-${(s.qualiteStatus || '').startsWith('Terminé') ? 'success' : s.qualiteStatus === 'En cours' ? 'warning' : 'secondary'}`}>{s.qualiteStatus || 'Non démarré'}</span> },
    { key: 'userQualite', label: 'User Qualité', text: s => s.userQualite || '', render: s => s.userQualite || '-' },
    { key: 'startDateControl', label: 'Début contrôle', text: s => fmt(s.startDateControl), sortVal: s => s.startDateControl || '', cls: 'small' },
    { key: 'endDateControl', label: 'Fin contrôle', text: s => fmt(s.endDateControl), sortVal: s => s.endDateControl || '', cls: 'small' },
    { key: 'createdAt', label: 'Créé le', text: s => fmt(s.createdAt), sortVal: s => s.createdAt || '', cls: 'small' },
];

class CncQualite extends Component {
    constructor(props) {
        super(props);
        this.state = {
            // Filters
            searchBoxId: '',
            filterStatus: '',
            filterDate: new Date().toISOString().slice(0, 10),
            dateMode: 'ps', // 'ps' = date création PS, 'qualite' = date contrôle qualité

            // Column header filters + click-to-sort
            columnFilters: {},
            sortKey: '',
            sortDir: 'asc',

            // Data
            sessions: [],
            loading: false,

            // Messages
            message: null,
            messageType: null,

            // Detail modal
            detailSession: null,
        };
    }

    componentDidMount() {
        this.loadSessions();
    }

    loadSessions = () => {
        this.setState({ loading: true });
        axios.get('/api/cncPs/all?size=500')
            .then(res => this.setState({ sessions: res.data.content || [], loading: false }))
            .catch(() => this.setState({ loading: false }));
    }

    handleSearchBox = () => {
        const { searchBoxId } = this.state;
        if (!searchBoxId.trim()) {
            this.loadSessions();
            return;
        }
        this.setState({ loading: true });
        axios.get(`/api/cncPs/boxDetails/${searchBoxId.trim()}`)
            .then(res => {
                const data = res.data;
                if (data.existingSession) {
                    // Search is independent of the date filter: move the date picker to the
                    // found box's date so the client-side filter doesn't hide it.
                    this.setState({
                        sessions: [data.existingSession],
                        dateMode: 'ps',
                        filterDate: (data.existingSession.createdAt || '').substring(0, 10),
                        columnFilters: {},
                        loading: false,
                    });
                } else {
                    this.setState({ sessions: [], loading: false });
                    this.showMessage('Aucune session trouvée', 'error');
                }
            })
            .catch(err => {
                this.setState({ loading: false });
                this.showMessage(err.response?.data || 'Erreur recherche', 'error');
            });
    }

    handleReopen = (sessionId) => {
        if (!window.confirm('Réouvrir cette session ?')) return;
        axios.post(`/api/cncPs/session/${sessionId}/reopen`)
            .then(res => {
                this.showMessage('Session réouverte', 'success');
                this.loadSessions();
            })
            .catch(err => this.showMessage(err.response?.data || 'Erreur réouverture', 'error'));
    }

    handleDelete = (sessionId) => {
        if (!window.confirm('Supprimer cette session et toutes ses données (consommations et contrôles) ?')) return;
        axios.delete(`/api/cncPs/session/${sessionId}`)
            .then(() => {
                this.showMessage('Session supprimée', 'success');
                this.loadSessions();
            })
            .catch(err => this.showMessage(err.response?.data || 'Erreur suppression', 'error'));
    }

    showMessage = (message, type) => {
        this.setState({ message, messageType: type });
        setTimeout(() => this.setState({ message: null, messageType: null }), 5000);
    }

    handleSort = (key) => {
        this.setState(prev => ({
            sortKey: key,
            sortDir: prev.sortKey === key && prev.sortDir === 'asc' ? 'desc' : 'asc',
        }));
    }

    setColumnFilter = (key, value) => {
        this.setState(prev => ({ columnFilters: { ...prev.columnFilters, [key]: value } }));
    }

    openDetail = (session) => this.setState({ detailSession: session });
    // Reload on close so any changes made in the embedded CncControl are reflected in the list.
    closeDetail = () => this.setState({ detailSession: null }, this.loadSessions);

    renderDetailModal() {
        const s = this.state.detailSession;
        if (!s) return null;
        return (
            <>
                <div className="modal fade show" style={{ display: 'block' }} role="dialog" onClick={this.closeDetail}>
                    <div className="modal-dialog modal-xl modal-dialog-scrollable" role="document" onClick={e => e.stopPropagation()}>
                        <div className="modal-content">
                            <div className="modal-header">
                                <h5 className="modal-title"><FontAwesomeIcon icon={faShieldAlt} /> Détail contrôle — Boîte {s.boxId}</h5>
                                <button type="button" className="close" onClick={this.closeDetail}><span>&times;</span></button>
                            </div>
                            <div className="modal-body" style={{ background: '#f5f6f8' }}>
                                {/* Reuse the exact /cncControl view, loaded by box id. */}
                                <CncControl boxId={s.boxId} embedded />
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn btn-secondary" onClick={this.closeDetail}>Fermer</button>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="modal-backdrop fade show"></div>
            </>
        );
    }

    getFilteredSessions = () => {
        const { sessions, filterStatus, filterDate, dateMode, columnFilters, sortKey, sortDir } = this.state;
        let filtered = sessions;

        if (filterStatus === 'completed') filtered = filtered.filter(s => s.completed);
        else if (filterStatus === 'active') filtered = filtered.filter(s => !s.completed);

        if (filterDate) {
            filtered = filtered.filter(s => {
                if (dateMode === 'qualite') {
                    return (s.startDateControl || '').substring(0, 10) === filterDate
                        || (s.endDateControl || '').substring(0, 10) === filterDate;
                }
                return (s.createdAt || '').substring(0, 10) === filterDate;
            });
        }

        // Per-column header filters (case-insensitive substring on the displayed text).
        COLUMNS.forEach(col => {
            const q = (columnFilters[col.key] || '').trim().toLowerCase();
            if (q) filtered = filtered.filter(s => col.text(s).toLowerCase().includes(q));
        });

        // Click-to-sort by a single column (asc/desc toggled in handleSort).
        if (sortKey) {
            const col = COLUMNS.find(c => c.key === sortKey);
            if (col) {
                const dir = sortDir === 'asc' ? 1 : -1;
                const val = s => (col.sortVal ? col.sortVal(s) : col.text(s));
                filtered = [...filtered].sort((a, b) => {
                    const av = val(a), bv = val(b);
                    if (typeof av === 'number' && typeof bv === 'number') return (av - bv) * dir;
                    return String(av).localeCompare(String(bv)) * dir;
                });
            }
        }

        return filtered;
    }

    render() {
        const {
            searchBoxId, filterStatus, filterDate, dateMode,
            columnFilters, sortKey, sortDir,
            loading,
            message, messageType,
        } = this.state;

        const sessions = this.getFilteredSessions();

        return (
            <div className="container-fluid mt-3">
                <div className="d-flex justify-content-between align-items-center mb-3">
                    <h2><FontAwesomeIcon icon={faShieldAlt} /> Gestion Qualité CNC</h2>
                    <button className="btn btn-outline-secondary btn-sm" onClick={this.loadSessions}>
                        <FontAwesomeIcon icon={faSpinner} spin={loading} /> Actualiser
                    </button>
                </div>

                {message && (
                    <div className={`alert alert-${messageType === 'success' ? 'success' : 'danger'}`}>
                        <FontAwesomeIcon icon={messageType === 'success' ? faCheckCircle : faExclamationTriangle} className="mr-2" />
                        {message}
                    </div>
                )}

                {/* Filters */}
                <div className="card mb-3">
                    <div className="card-body py-2">
                        <div className="row align-items-end">
                            <div className="col-md-3 form-group mb-1">
                                <label className="small">Recherche Box ID</label>
                                <div className="input-group input-group-sm">
                                    <input type="text" className="form-control" value={searchBoxId}
                                           onChange={e => this.setState({ searchBoxId: e.target.value })}
                                           onKeyPress={e => e.key === 'Enter' && this.handleSearchBox()}
                                           placeholder="Ex: S12345" />
                                    <div className="input-group-append">
                                        <button className="btn btn-outline-primary" onClick={this.handleSearchBox}>
                                            <FontAwesomeIcon icon={faSearch} />
                                        </button>
                                    </div>
                                </div>
                            </div>
                            <div className="col-md-3 form-group mb-1">
                                <label className="small">Statut</label>
                                <select className="form-control form-control-sm" value={filterStatus}
                                        onChange={e => this.setState({ filterStatus: e.target.value })}>
                                    <option value="">Tous</option>
                                    <option value="completed">Complétées</option>
                                    <option value="active">En cours</option>
                                </select>
                            </div>
                            <div className="col-md-3 form-group mb-1">
                                <label className="small">Date ({dateMode === 'qualite' ? 'contrôle qualité' : 'PS'})</label>
                                <div className="input-group input-group-sm">
                                    <select className="form-control" style={{ maxWidth: '95px' }} value={dateMode}
                                            onChange={e => this.setState({ dateMode: e.target.value })}>
                                        <option value="ps">PS</option>
                                        <option value="qualite">Qualité</option>
                                    </select>
                                    <input type="date" className="form-control" value={filterDate}
                                           onChange={e => this.setState({ filterDate: e.target.value })} />
                                </div>
                            </div>
                            <div className="col-md-3 form-group mb-1">
                                <button className="btn btn-outline-secondary btn-sm w-100"
                                        onClick={() => this.setState({ filterStatus: '', filterDate: '', searchBoxId: '', dateMode: 'ps', columnFilters: {}, sortKey: '', sortDir: 'asc' }, this.loadSessions)}>
                                    Réinitialiser filtres
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Sessions table */}
                {loading ? (
                    <div className="text-center p-4"><FontAwesomeIcon icon={faSpinner} spin size="2x" /></div>
                ) : (
                    <div className="table-responsive">
                        <table className="table table-sm table-hover table-bordered">
                            <thead className="thead-dark">
                                <tr>
                                    {COLUMNS.map(col => (
                                        <th key={col.key} style={{ cursor: 'pointer', whiteSpace: 'nowrap' }}
                                            onClick={() => this.handleSort(col.key)}>
                                            {col.label}
                                            {sortKey === col.key && <span className="ml-1">{sortDir === 'asc' ? '▲' : '▼'}</span>}
                                        </th>
                                    ))}
                                    <th>Actions</th>
                                </tr>
                                <tr>
                                    {COLUMNS.map(col => (
                                        <th key={col.key} className="p-1" style={{ fontWeight: 'normal' }}>
                                            <input type="text" className="form-control form-control-sm"
                                                   value={columnFilters[col.key] || ''}
                                                   onChange={e => this.setColumnFilter(col.key, e.target.value)}
                                                   placeholder="filtrer" />
                                        </th>
                                    ))}
                                    <th className="p-1"></th>
                                </tr>
                            </thead>
                            <tbody>
                                {sessions.length === 0 ? (
                                    <tr><td colSpan={COLUMNS.length + 1} className="text-center text-muted p-4">Aucune session trouvée</td></tr>
                                ) : sessions.map(s => (
                                    <tr key={s.id} className={s.completed ? '' : 'table-warning'} style={{ cursor: 'pointer' }}
                                        onDoubleClick={() => this.openDetail(s)}>
                                        {COLUMNS.map(col => (
                                            <td key={col.key} className={col.cls}>
                                                {col.render ? col.render(s) : col.text(s)}
                                            </td>
                                        ))}
                                        <td>
                                            <button className="btn btn-info btn-sm py-0 mr-1" title="Détail contrôle"
                                                    onClick={() => this.openDetail(s)}>
                                                <FontAwesomeIcon icon={faEye} />
                                            </button>
                                            {s.completed && (
                                                <button className="btn btn-warning btn-sm py-0 mr-1" title="Réouvrir"
                                                        onClick={() => this.handleReopen(s.id)}>
                                                    <FontAwesomeIcon icon={faUndo} />
                                                </button>
                                            )}
                                            <button className="btn btn-danger btn-sm py-0" title="Supprimer"
                                                    onClick={() => this.handleDelete(s.id)}>
                                                <FontAwesomeIcon icon={faTrash} />
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                <div className="text-muted small mt-2">{sessions.length} session(s)</div>

                {this.renderDetailModal()}
            </div>
        );
    }
}

const mapStateToProps = state => ({ security: state.security });
export default connect(mapStateToProps)(CncQualite);
