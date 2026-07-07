import React, { Component } from 'react';
import axios from 'axios';
import { connect } from 'react-redux';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faWeight, faTrash, faPaperPlane, faCheckCircle, faTimesCircle, faSpinner } from '@fortawesome/free-solid-svg-icons';
import Select from 'react-select';
import '../../styles/BoxWeight.scss';

class BoxWeightFilling extends Component {
    constructor(props) {
        super(props);
        this.state = {
            boxType: null,
            boxId: '',
            sentWeight: '',
            partnumber: '',
            quantity: '',
            estimatedWeight: null,
            partNumberWeight: null,
            gammeLoading: false,
            gammeInfo: null,
            myEntries: [],
            loading: false,
            submitting: false,
            message: null,
            messageType: null,
        };
    }

    componentDidMount() {
        this.loadMyEntries();
    }

    loadMyEntries = () => {
        this.setState({ loading: true });
        axios.get('/api/boxWeight/mySent')
            .then(res => {
                this.setState({ myEntries: res.data, loading: false });
            })
            .catch(err => {
                console.error(err);
                this.setState({ loading: false });
            });
    }

    handleBoxIdLookup = (boxId) => {
        if (!boxId || boxId.trim() === '') return;
        this.setState({ gammeLoading: true, gammeInfo: null });
        axios.get(`/api/gammeTechniqueImprimer/serie/${boxId.trim()}`)
            .then(res => {
                const gamme = res.data;
                this.setState({
                    gammeLoading: false,
                    gammeInfo: gamme,
                    partnumber: gamme.partNumberImp || '',
                    quantity: gamme.quantiteImp ? gamme.quantiteImp.toString().trim() : ''
                });
                this.showMessage(`Gamme trouvée: PN=${gamme.partNumberImp}, Qté=${gamme.quantiteImp}`, 'success');
            })
            .catch(err => {
                console.error(err);
                this.setState({ gammeLoading: false, gammeInfo: null });
                this.showMessage('ID Boîte non trouvé dans les gammes CMS', 'error');
            });
    }

    getEmptyWeight = () => {
        const { boxType } = this.state;
        if (!boxType) return 0;
        const emptyWeights = { gray: 0.5, black: 0.8 };
        return emptyWeights[boxType.value] || 0;
    }

    calculatePartNumberWeight = () => {
        const { sentWeight, quantity } = this.state;
        const emptyWeight = this.getEmptyWeight();
        const sw = parseFloat(sentWeight);
        const qt = parseInt(quantity);

        if (isNaN(sw) || isNaN(qt) || qt <= 0) return null;
        return (sw - emptyWeight) / qt;
    }

    handleSubmit = (e) => {
        e.preventDefault();
        const { boxType, boxId, sentWeight, partnumber, quantity } = this.state;

        if (!boxType || !boxId || !sentWeight) {
            this.showMessage('Veuillez remplir tous les champs obligatoires', 'error');
            return;
        }

        this.setState({ submitting: true });

        const payload = {
            boxType: boxType.value,
            boxId: boxId.trim(),
            sentWeight: parseFloat(sentWeight)
        };

        if (partnumber && partnumber.trim()) {
            payload.partnumber = partnumber.trim();
        }

        if (quantity && quantity.trim()) {
            payload.quantity = parseInt(quantity);
        }

        const pnWeight = this.calculatePartNumberWeight();
        if (pnWeight !== null) {
            payload.partNumberWeight = parseFloat(pnWeight.toFixed(4));
        }

        axios.post('/api/boxWeight/fill', payload)
            .then(res => {
                this.showMessage('Poids enregistré avec succès!', 'success');
                this.setState({
                    boxId: '',
                    sentWeight: '',
                    partnumber: '',
                    quantity: '',
                    estimatedWeight: null,
                    submitting: false
                });
                this.loadMyEntries();
            })
            .catch(err => {
                console.error(err);
                this.showMessage('Erreur lors de l\'enregistrement', 'error');
                this.setState({ submitting: false });
            });
    }

    handleRemoveLast = () => {
        if (!window.confirm('Êtes-vous sûr de vouloir supprimer votre dernière entrée?')) {
            return;
        }

        axios.delete('/api/boxWeight/removeLast')
            .then(res => {
                this.showMessage('Dernière entrée supprimée', 'success');
                this.loadMyEntries();
            })
            .catch(err => {
                console.error(err);
                this.showMessage(err.response?.data?.message || 'Erreur lors de la suppression', 'error');
            });
    }

    showMessage = (message, type) => {
        this.setState({ message, messageType: type });
        setTimeout(() => {
            this.setState({ message: null, messageType: null });
        }, 3000);
    }

    handleEstimateWeight = () => {
        const { partnumber, quantity, boxType } = this.state;

        if (!partnumber || !quantity || !boxType) {
            this.showMessage('Veuillez sélectionner le type de boîte et remplir part number et quantité', 'error');
            return;
        }

        axios.get('/api/boxWeight/estimateWeight', {
            params: {
                partnumber: partnumber.trim(),
                quantity: parseInt(quantity),
                boxType: boxType.value
            }
        })
            .then(res => {
                this.setState({ estimatedWeight: res.data.estimatedWeight });
                if (res.data.estimatedWeight) {
                    this.showMessage(`Poids estimé: ${res.data.estimatedWeight.toFixed(2)} kg`, 'success');
                } else {
                    this.showMessage('Aucune donnée disponible pour l\'estimation', 'error');
                }
            })
            .catch(err => {
                console.error(err);
                this.showMessage('Erreur lors de l\'estimation', 'error');
            });
    }

    formatDateTime = (dateStr) => {
        if (!dateStr) return '-';
        const date = new Date(dateStr);
        return date.toLocaleString('fr-FR');
    }

    render() {
        const { boxType, boxId, sentWeight, partnumber, quantity, estimatedWeight, myEntries, loading, submitting, message, messageType, gammeLoading } = this.state;

        const boxTypeOptions = [
            { label: 'Boîte Grise (0.5 kg)', value: 'gray', emptyWeight: 0.5 },
            { label: 'Boîte Noire (0.8 kg)', value: 'black', emptyWeight: 0.8 }
        ];

        const partNumberWeight = this.calculatePartNumberWeight();

        return (
            <div className="box-weight-container">
                <h2 className="page-title">
                    <FontAwesomeIcon icon={faWeight} /> Enregistrement Poids Boîte
                </h2>

                {message && (
                    <div className={`alert alert-${messageType === 'success' ? 'success' : 'danger'}`}>
                        <FontAwesomeIcon icon={messageType === 'success' ? faCheckCircle : faTimesCircle} />
                        {' '}{message}
                    </div>
                )}

                <div className="row">
                    {/* Form Section */}
                    <div className="col-md-4">
                        <div className="card">
                            <div className="card-header bg-primary text-white">
                                <h5 className="mb-0">Nouveau Enregistrement</h5>
                            </div>
                            <div className="card-body">
                                <form onSubmit={this.handleSubmit}>
                                    <div className="form-group mb-3">
                                        <label>Type de Boîte *</label>
                                        <Select
                                            value={boxType}
                                            onChange={(option) => this.setState({ boxType: option })}
                                            options={boxTypeOptions}
                                            placeholder="Sélectionner..."
                                            classNamePrefix="rs"
                                        />
                                    </div>

                                    <div className="form-group mb-3">
                                        <label>ID Boîte *</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={boxId}
                                            onChange={(e) => this.setState({ boxId: e.target.value })}
                                            onKeyUp={(e) => {
                                                if (e.key === 'Enter') {
                                                    this.handleBoxIdLookup(boxId);
                                                }
                                            }}
                                            placeholder="Scanner/Entrer l'ID de la boîte"
                                        />
                                        {gammeLoading && <small className="text-info"><FontAwesomeIcon icon={faSpinner} spin /> Recherche gamme...</small>}
                                    </div>

                                    <div className="form-group mb-3">
                                        <label>Poids (kg) *</label>
                                        <input
                                            type="number"
                                            step="0.01"
                                            className="form-control"
                                            value={sentWeight}
                                            onChange={(e) => this.setState({ sentWeight: e.target.value })}
                                            placeholder="Entrer le poids en kg"
                                        />
                                    </div>

                                    <div className="form-group mb-3">
                                        <label>Part Number (optionnel)</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={partnumber}
                                            onChange={(e) => this.setState({ partnumber: e.target.value })}
                                            placeholder="Entrer le part number"
                                        />
                                    </div>

                                    <div className="form-group mb-3">
                                        <label>Quantité (optionnel)</label>
                                        <input
                                            type="number"
                                            className="form-control"
                                            value={quantity}
                                            onChange={(e) => this.setState({ quantity: e.target.value })}
                                            placeholder="Entrer la quantité"
                                        />
                                    </div>

                                    {estimatedWeight && (
                                        <div className="alert alert-info mb-3">
                                            <strong>Poids estimé:</strong> {estimatedWeight.toFixed(2)} kg
                                        </div>
                                    )}

                                    {partNumberWeight !== null && (
                                        <div className="alert alert-success mb-3">
                                            <strong>Poids par pièce:</strong> {partNumberWeight.toFixed(4)} kg
                                            <br/>
                                            <small className="text-muted">
                                                ({sentWeight} - {this.getEmptyWeight()} kg boîte) / {quantity} pcs
                                            </small>
                                        </div>
                                    )}

                                    <button
                                        type="button"
                                        className="btn btn-secondary w-100 mb-2"
                                        onClick={this.handleEstimateWeight}
                                        disabled={!partnumber || !quantity || !boxType}
                                    >
                                        Estimer le poids
                                    </button>

                                    <button
                                        type="submit"
                                        className="btn btn-primary w-100"
                                        disabled={submitting}
                                    >
                                        {submitting ? (
                                            <><FontAwesomeIcon icon={faSpinner} spin /> Enregistrement...</>
                                        ) : (
                                            <><FontAwesomeIcon icon={faPaperPlane} /> Enregistrer</>
                                        )}
                                    </button>
                                </form>

                                {myEntries.length > 0 && myEntries[0].receivedBy === null && (
                                    <button
                                        className="btn btn-outline-danger w-100 mt-3"
                                        onClick={this.handleRemoveLast}
                                    >
                                        <FontAwesomeIcon icon={faTrash} /> Supprimer ma dernière entrée
                                    </button>
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Table Section */}
                    <div className="col-md-8">
                        <div className="card">
                            <div className="card-header bg-secondary text-white">
                                <h5 className="mb-0">Mes Enregistrements</h5>
                            </div>
                            <div className="card-body p-0">
                                {loading ? (
                                    <div className="text-center p-4">
                                        <FontAwesomeIcon icon={faSpinner} spin size="2x" />
                                    </div>
                                ) : (
                                    <div className="table-responsive">
                                        <table className="table table-striped table-hover mb-0">
                                            <thead className="thead-dark">
                                                <tr>
                                                    <th>Type</th>
                                                    <th>ID Boîte</th>
                                                    <th>Poids Envoyé</th>
                                                    <th>Date Envoi</th>
                                                    <th>Statut</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {myEntries.length === 0 ? (
                                                    <tr>
                                                        <td colSpan="5" className="text-center text-muted py-4">
                                                            Aucun enregistrement trouvé
                                                        </td>
                                                    </tr>
                                                ) : (
                                                    myEntries.map((entry, index) => (
                                                        <tr key={entry.id}>
                                                            <td>
                                                                <span className={`badge badge-${entry.boxType === 'gray' ? 'secondary' : 'dark'}`}>
                                                                    {entry.boxType === 'gray' ? 'Grise' : 'Noire'}
                                                                </span>
                                                            </td>
                                                            <td><strong>{entry.boxId}</strong></td>
                                                            <td>{entry.sentWeight} kg</td>
                                                            <td>{this.formatDateTime(entry.sentAt)}</td>
                                                            <td>
                                                                {entry.receivedBy !== null ? (
                                                                    entry.validated ? (
                                                                        <span className="badge badge-success">
                                                                            <FontAwesomeIcon icon={faCheckCircle} /> Validé
                                                                        </span>
                                                                    ) : (
                                                                        <span className="badge badge-danger">
                                                                            <FontAwesomeIcon icon={faTimesCircle} /> Non validé
                                                                        </span>
                                                                    )
                                                                ) : (
                                                                    <span className="badge badge-secondary">Non vérifié</span>
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

export default connect(mapStateToProps)(BoxWeightFilling);
