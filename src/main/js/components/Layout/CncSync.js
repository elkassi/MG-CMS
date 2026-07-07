import React, { Component } from 'react';
import axios from 'axios';

class CncSync extends Component {
    constructor(props) {
        super(props);
        this.state = {
            file: null,
            importing: false,
            exporting: false,
            message: '',
            messageType: 'info',
            importResult: null,
        };
    }

    handleExport = () => {
        this.setState({ exporting: true, message: '', importResult: null });
        axios.get('/api/cncSync/export', { responseType: 'blob' })
            .then(res => {
                const url = window.URL.createObjectURL(new Blob([res.data]));
                const link = document.createElement('a');
                link.href = url;
                link.setAttribute('download', 'mgcms-cnc-export.json');
                document.body.appendChild(link);
                link.click();
                link.remove();
                window.URL.revokeObjectURL(url);
                this.setState({ exporting: false, message: 'Export téléchargé avec succès', messageType: 'success' });
            })
            .catch(err => {
                this.setState({ exporting: false, message: 'Erreur export: ' + (err.response?.data || err.message), messageType: 'danger' });
            });
    };

    handleImport = () => {
        const { file } = this.state;
        if (!file) {
            this.setState({ message: 'Veuillez sélectionner un fichier JSON', messageType: 'warning' });
            return;
        }
        this.setState({ importing: true, message: '', importResult: null });
        const formData = new FormData();
        formData.append('file', file);
        axios.post('/api/cncSync/import', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
            .then(res => {
                this.setState({
                    importing: false,
                    importResult: res.data,
                    message: 'Import réussi!',
                    messageType: 'success',
                    file: null,
                });
            })
            .catch(err => {
                this.setState({
                    importing: false,
                    message: 'Erreur import: ' + (err.response?.data || err.message),
                    messageType: 'danger',
                });
            });
    };

    render() {
        const { importing, exporting, message, messageType, importResult } = this.state;

        return (
            <div className="container mt-4">
                <h3 className="mb-4" style={{ color: '#1a237e' }}>
                    <i className="fa fa-exchange-alt" style={{ marginRight: 10 }}></i>
                    Import / Export CNC
                </h3>

                {message && <div className={`alert alert-${messageType}`}>{message}</div>}

                <div className="row">
                    {/* Export Section - MG-CMS → CMS-CNC */}
                    <div className="col-md-6 mb-4">
                        <div className="card shadow" style={{ borderRadius: '12px' }}>
                            <div className="card-body">
                                <h5>📤 Export vers CMS-CNC</h5>
                                <p className="text-muted">
                                    Exporte les données de référence (machines, programmes, distributions)
                                    vers un fichier JSON à importer dans CMS-CNC.
                                </p>
                                <button className="btn btn-success mt-2" onClick={this.handleExport} disabled={exporting}>
                                    {exporting ? 'Export en cours...' : 'Exporter les Données'}
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* Import Section - CMS-CNC → MG-CMS */}
                    <div className="col-md-6 mb-4">
                        <div className="card shadow" style={{ borderRadius: '12px' }}>
                            <div className="card-body">
                                <h5>📥 Import depuis CMS-CNC</h5>
                                <p className="text-muted">
                                    Importez les rapports de sessions CNC (travail par machine) depuis un
                                    fichier JSON exporté de CMS-CNC.
                                </p>
                                <div className="form-group">
                                    <input
                                        type="file"
                                        className="form-control-file"
                                        accept=".json"
                                        onChange={e => this.setState({ file: e.target.files[0] })}
                                    />
                                </div>
                                <button className="btn btn-primary mt-2" onClick={this.handleImport} disabled={importing}>
                                    {importing ? 'Import en cours...' : 'Importer'}
                                </button>

                                {importResult && (
                                    <div className="mt-3">
                                        <h6>Résultat:</h6>
                                        <ul className="list-group">
                                            <li className="list-group-item d-flex justify-content-between">
                                                Sessions importées
                                                <span className="badge badge-primary badge-pill">{importResult.imported}</span>
                                            </li>
                                            <li className="list-group-item d-flex justify-content-between">
                                                Déjà existantes (ignorées)
                                                <span className="badge badge-secondary badge-pill">{importResult.skipped}</span>
                                            </li>
                                            <li className="list-group-item d-flex justify-content-between">
                                                Pièces totales
                                                <span className="badge badge-info badge-pill">{importResult.totalPieces}</span>
                                            </li>
                                        </ul>
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

export default CncSync;
