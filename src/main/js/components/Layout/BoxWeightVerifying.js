import React, { Component } from 'react';
import axios from 'axios';
import { connect } from 'react-redux';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faBalanceScale, faSearch, faCheckCircle, faTimesCircle, faSpinner, faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';
import '../../styles/BoxWeight.scss';

class BoxWeightVerifying extends Component {
    constructor(props) {
        super(props);
        this.state = {
            searchBoxId: '',
            pendingEntries: [],
            allEntries: [],
            selectedEntry: null,
            receivedWeight: '',
            loading: false,
            verifying: false,
            message: null,
            messageType: null,
            showAll: false,
            verificationResult: null,
        };
    }

    componentDidMount() {
        this.loadPendingEntries();
    }

    loadPendingEntries = () => {
        this.setState({ loading: true });
        axios.get('/api/boxWeight/notVerified')
            .then(res => {
                this.setState({ pendingEntries: res.data, loading: false });
            })
            .catch(err => {
                console.error(err);
                this.setState({ loading: false });
            });
    }

    loadAllEntries = () => {
        this.setState({ loading: true });
        axios.get('/api/boxWeight/list')
            .then(res => {
                this.setState({ allEntries: res.data, loading: false, showAll: true });
            })
            .catch(err => {
                console.error(err);
                this.setState({ loading: false });
            });
    }

    handleSearch = () => {
        const { searchBoxId } = this.state;
        if (!searchBoxId.trim()) {
            this.showMessage('Veuillez entrer un ID de boîte', 'error');
            return;
        }

        this.setState({ loading: true });
        axios.get(`/api/boxWeight/byBoxIdNotVerified/${searchBoxId.trim()}`)
            .then(res => {
                if (res.data.length === 0) {
                    this.showMessage('Aucune entrée non vérifiée trouvée pour cet ID', 'warning');
                    this.setState({ loading: false });
                } else if (res.data.length === 1) {
                    this.setState({ selectedEntry: res.data[0], loading: false, verificationResult: null });
                } else {
                    this.setState({ pendingEntries: res.data, loading: false });
                    this.showMessage(`${res.data.length} entrées trouvées pour cet ID`, 'info');
                }
            })
            .catch(err => {
                console.error(err);
                this.showMessage('Erreur lors de la recherche', 'error');
                this.setState({ loading: false });
            });
    }

    handleVerify = () => {
        const { selectedEntry, receivedWeight } = this.state;

        if (!receivedWeight) {
            this.showMessage('Veuillez entrer le poids reçu', 'error');
            return;
        }

        this.setState({ verifying: true });

        axios.post(`/api/boxWeight/verify/${selectedEntry.id}`, {
            receivedWeight: parseFloat(receivedWeight)
        })
            .then(res => {
                const result = res.data;
                this.setState({
                    verifying: false,
                    verificationResult: {
                        validated: result.validated,
                        difference: result.difference,
                        boxWeight: result.boxWeight
                    }
                });

                if (result.validated) {
                    this.showMessage('Poids validé avec succès!', 'success');
                } else {
                    this.showMessage(`Différence trop élevée: ${result.difference.toFixed(2)} kg`, 'error');
                }

                // Reload after 2 seconds
                setTimeout(() => {
                    this.setState({ 
                        selectedEntry: null, 
                        receivedWeight: '', 
                        verificationResult: null,
                        searchBoxId: '' 
                    });
                    this.loadPendingEntries();
                }, 2000);
            })
            .catch(err => {
                console.error(err);
                this.showMessage('Erreur lors de la vérification', 'error');
                this.setState({ verifying: false });
            });
    }

    selectEntry = (entry) => {
        this.setState({ 
            selectedEntry: entry, 
            receivedWeight: '', 
            verificationResult: null 
        });
    }

    showMessage = (message, type) => {
        this.setState({ message, messageType: type });
        setTimeout(() => {
            this.setState({ message: null, messageType: null });
        }, 4000);
    }

    formatDateTime = (dateStr) => {
        if (!dateStr) return '-';
        const date = new Date(dateStr);
        return date.toLocaleString('fr-FR');
    }

    render() {
        const { searchBoxId, pendingEntries, allEntries, selectedEntry, receivedWeight, 
                loading, verifying, message, messageType, showAll, verificationResult } = this.state;

        const displayEntries = showAll ? allEntries : pendingEntries;

        return (
            <div className="box-weight-container">
                <h2 className="page-title">
                    <FontAwesomeIcon icon={faBalanceScale} /> Vérification Poids Boîte
                </h2>

                {message && (
                    <div className={`alert alert-${
                        messageType === 'success' ? 'success' : 
                        messageType === 'warning' ? 'warning' : 
                        messageType === 'info' ? 'info' : 'danger'
                    }`}>
                        <FontAwesomeIcon icon={
                            messageType === 'success' ? faCheckCircle : 
                            messageType === 'warning' ? faExclamationTriangle : faTimesCircle
                        } />
                        {' '}{message}
                    </div>
                )}

                <div className="row">
                    {/* Search & Verify Section */}
                    <div className="col-md-4">
                        {/* Search Card */}
                        <div className="card mb-3">
                            <div className="card-header bg-info text-white">
                                <h5 className="mb-0"><FontAwesomeIcon icon={faSearch} /> Rechercher Boîte</h5>
                            </div>
                            <div className="card-body">
                                <div className="input-group">
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={searchBoxId}
                                        onChange={(e) => this.setState({ searchBoxId: e.target.value })}
                                        placeholder="ID de la boîte..."
                                        onKeyPress={(e) => e.key === 'Enter' && this.handleSearch()}
                                    />
                                    <div className="input-group-append">
                                        <button className="btn btn-info" onClick={this.handleSearch}>
                                            <FontAwesomeIcon icon={faSearch} />
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Verification Card */}
                        {selectedEntry && (
                            <div className="card">
                                <div className="card-header bg-primary text-white">
                                    <h5 className="mb-0">Vérification</h5>
                                </div>
                                <div className="card-body">
                                    <div className="entry-details mb-3">
                                        <p><strong>ID Boîte:</strong> {selectedEntry.boxId}</p>
                                        <p><strong>Type:</strong> {selectedEntry.boxType === 'gray' ? 'Grise' : 'Noire'}</p>
                                        <p><strong>Poids Envoyé:</strong> <span className="text-primary font-weight-bold">{selectedEntry.sentWeight} kg</span></p>
                                        <p><strong>Envoyé par:</strong> {selectedEntry.sentBy}</p>
                                        <p><strong>Date Envoi:</strong> {this.formatDateTime(selectedEntry.sentAt)}</p>
                                    </div>

                                    <div className="form-group mb-3">
                                        <label><strong>Poids Reçu (kg) *</strong></label>
                                        <input
                                            type="number"
                                            step="0.01"
                                            className="form-control form-control-lg"
                                            value={receivedWeight}
                                            onChange={(e) => this.setState({ receivedWeight: e.target.value })}
                                            placeholder="Entrer le poids reçu"
                                            autoFocus
                                        />
                                    </div>

                                    {receivedWeight && !verificationResult && (
                                        <div className="preview-difference mb-3">
                                            <span>Différence estimée: </span>
                                            <strong className={Math.abs(selectedEntry.sentWeight - parseFloat(receivedWeight)) > 1 ? 'text-danger' : 'text-success'}>
                                                {Math.abs(selectedEntry.sentWeight - parseFloat(receivedWeight)).toFixed(2)} kg
                                            </strong>
                                            {Math.abs(selectedEntry.sentWeight - parseFloat(receivedWeight)) > 1 && (
                                                <span className="text-danger ml-2">
                                                    <FontAwesomeIcon icon={faExclamationTriangle} /> &gt; 1 kg
                                                </span>
                                            )}
                                        </div>
                                    )}

                                    {verificationResult && (
                                        <div className={`verification-result p-3 mb-3 rounded ${verificationResult.validated ? 'bg-success' : 'bg-danger'} text-white`}>
                                            <h4 className="mb-2">
                                                <FontAwesomeIcon icon={verificationResult.validated ? faCheckCircle : faTimesCircle} />
                                                {' '}{verificationResult.validated ? 'VALIDÉ' : 'NON VALIDÉ'}
                                            </h4>
                                            <p className="mb-0">Différence: {verificationResult.difference.toFixed(2)} kg</p>
                                        </div>
                                    )}

                                    {!verificationResult && (
                                        <button
                                            className="btn btn-success btn-lg w-100"
                                            onClick={this.handleVerify}
                                            disabled={verifying || !receivedWeight}
                                        >
                                            {verifying ? (
                                                <><FontAwesomeIcon icon={faSpinner} spin /> Vérification...</>
                                            ) : (
                                                <><FontAwesomeIcon icon={faCheckCircle} /> Vérifier</>
                                            )}
                                        </button>
                                    )}

                                    <button
                                        className="btn btn-outline-secondary w-100 mt-2"
                                        onClick={() => this.setState({ selectedEntry: null, receivedWeight: '', verificationResult: null })}
                                    >
                                        Annuler
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Table Section */}
                    <div className="col-md-8">
                        <div className="card">
                            <div className="card-header bg-secondary text-white d-flex justify-content-between align-items-center">
                                <h5 className="mb-0">
                                    {showAll ? 'Tous les Enregistrements' : 'En Attente de Vérification'}
                                </h5>
                                <div>
                                    <button 
                                        className={`btn btn-sm ${!showAll ? 'btn-light' : 'btn-outline-light'} mr-2`}
                                        onClick={() => { this.setState({ showAll: false }); this.loadPendingEntries(); }}
                                    >
                                        En Attente
                                    </button>
                                    <button 
                                        className={`btn btn-sm ${showAll ? 'btn-light' : 'btn-outline-light'}`}
                                        onClick={this.loadAllEntries}
                                    >
                                        Tout
                                    </button>
                                </div>
                            </div>
                            <div className="card-body p-0">
                                {loading ? (
                                    <div className="text-center p-4">
                                        <FontAwesomeIcon icon={faSpinner} spin size="2x" />
                                    </div>
                                ) : (
                                    <div className="table-responsive" style={{ maxHeight: '600px', overflowY: 'auto' }}>
                                        <table className="table table-striped table-hover mb-0">
                                            <thead className="thead-dark" style={{ position: 'sticky', top: 0 }}>
                                                <tr>
                                                    <th>Type</th>
                                                    <th>ID Boîte</th>
                                                    <th>Poids Envoyé</th>
                                                    <th>Envoyé par</th>
                                                    <th>Date</th>
                                                    <th>Statut</th>
                                                    <th>Action</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {displayEntries.length === 0 ? (
                                                    <tr>
                                                        <td colSpan="7" className="text-center text-muted py-4">
                                                            {showAll ? 'Aucun enregistrement' : 'Aucune entrée en attente'}
                                                        </td>
                                                    </tr>
                                                ) : (
                                                    displayEntries.map((entry) => (
                                                        <tr key={entry.id} className={selectedEntry?.id === entry.id ? 'table-active' : ''}>
                                                            <td>
                                                                <span className={`badge badge-${entry.boxType === 'gray' ? 'secondary' : 'dark'}`}>
                                                                    {entry.boxType === 'gray' ? 'Grise' : 'Noire'}
                                                                </span>
                                                            </td>
                                                            <td><strong>{entry.boxId}</strong></td>
                                                            <td>{entry.sentWeight} kg</td>
                                                            <td>{entry.sentBy}</td>
                                                            <td>{this.formatDateTime(entry.sentAt)}</td>
                                                            <td>
                                                                {entry.receivedBy === null ? (
                                                                    <span className="badge badge-warning">En attente</span>
                                                                ) : entry.validated ? (
                                                                    <span className="badge badge-success">
                                                                        <FontAwesomeIcon icon={faCheckCircle} /> Validé
                                                                    </span>
                                                                ) : (
                                                                    <span className="badge badge-danger">
                                                                        <FontAwesomeIcon icon={faTimesCircle} /> Non validé
                                                                    </span>
                                                                )}
                                                            </td>
                                                            <td>
                                                                {entry.receivedBy === null && (
                                                                    <button
                                                                        className="btn btn-sm btn-primary"
                                                                        onClick={() => this.selectEntry(entry)}
                                                                    >
                                                                        Vérifier
                                                                    </button>
                                                                )}
                                                                {entry.receivedBy !== null && (
                                                                    <small className="text-muted">
                                                                        {entry.receivedWeight} kg par {entry.receivedBy}
                                                                    </small>
                                                                )}
                                                            </td>
                                                        </tr>
                                                    ))
                                                )}
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

const mapStateToProps = (state) => ({
    security: state.security
});

export default connect(mapStateToProps)(BoxWeightVerifying);
