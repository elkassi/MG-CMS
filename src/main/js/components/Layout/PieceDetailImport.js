import React, { Component } from 'react';
import axios from 'axios';

class PieceDetailImport extends Component {
  constructor(props) {
    super(props);
    this.state = {
      file: null,
      loading: false,
      result: null,
      error: null,
    };
  }

  handleFileChange = (e) => {
    this.setState({ file: e.target.files[0], result: null, error: null });
  };

  handleUpload = () => {
    const { file } = this.state;
    if (!file) {
      this.setState({ error: 'Veuillez sélectionner un fichier CSV' });
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    this.setState({ loading: true, error: null });
    axios
      .post('/api/pieceDetail/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((res) => {
        this.setState({ result: res.data, loading: false });
      })
      .catch((err) => {
        this.setState({
          error: err.response?.data?.message || err.message,
          loading: false,
        });
      });
  };

  render() {
    const { loading, result, error } = this.state;

    return (
      <div className="container mt-4">
        <h3>Import Pièces CSV (CAD)</h3>
        <p className="text-muted">
          Importez un fichier CSV exporté depuis le logiciel CAD contenant les détails des pièces.
        </p>

        <div className="card p-3 mb-3">
          <div className="form-group">
            <label>Fichier CSV</label>
            <input
              type="file"
              className="form-control"
              accept=".csv"
              onChange={this.handleFileChange}
            />
          </div>
          <button
            className="btn btn-primary mt-2"
            onClick={this.handleUpload}
            disabled={loading}
          >
            {loading ? 'Importation en cours...' : 'Importer'}
          </button>
        </div>

        {error && (
          <div className="alert alert-danger">{error}</div>
        )}

        {result && (
          <div className="card p-3">
            <h5>Résultat de l'importation</h5>
            <p>
              <strong>{result.imported}</strong> pièces importées avec succès.
            </p>
            {result.errors && result.errors.length > 0 && (
              <div>
                <h6 className="text-danger">Erreurs ({result.errors.length}):</h6>
                <ul className="list-group">
                  {result.errors.map((err, i) => (
                    <li key={i} className="list-group-item list-group-item-danger">
                      {err}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </div>
    );
  }
}

export default PieceDetailImport;
