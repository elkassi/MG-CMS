import React, { Component } from 'react';
import axios from 'axios';

class WeightCalculation extends Component {
  constructor(props) {
    super(props);
    this.state = {
      partNumberCovers: '',
      loading: false,
      results: null,
      error: null,
      expandedRows: {},
    };
  }

  handleCalculate = () => {
    const { partNumberCovers } = this.state;
    if (!partNumberCovers.trim()) {
      this.setState({ error: 'Veuillez entrer au moins un Part Number Cover' });
      return;
    }

    const pnList = partNumberCovers
      .split(/[\n,;]+/)
      .map((pn) => pn.trim())
      .filter((pn) => pn.length > 0);

    this.setState({ loading: true, error: null });
    axios
      .post('/api/pieceDetail/calculateWeight', { partNumberCovers: pnList })
      .then((res) => {
        this.setState({ results: res.data, loading: false });
      })
      .catch((err) => {
        this.setState({
          error: err.response?.data?.message || err.message,
          loading: false,
        });
      });
  };

  toggleRow = (index) => {
    this.setState((prev) => ({
      expandedRows: {
        ...prev.expandedRows,
        [index]: !prev.expandedRows[index],
      },
    }));
  };

  render() {
    const { partNumberCovers, loading, results, error, expandedRows } = this.state;

    return (
      <div className="container mt-4">
        <h3>Calcul de Poids par Part Number Cover</h3>
        <p className="text-muted">
          Entrez les Part Number Covers (un par ligne ou séparés par virgule) pour calculer le poids réel.
        </p>

        <div className="card p-3 mb-3">
          <div className="form-group">
            <label>Part Number Covers</label>
            <textarea
              className="form-control"
              rows={4}
              value={partNumberCovers}
              onChange={(e) => this.setState({ partNumberCovers: e.target.value })}
              placeholder="PN-001&#10;PN-002&#10;PN-003"
            />
          </div>
          <button
            className="btn btn-primary mt-2"
            onClick={this.handleCalculate}
            disabled={loading}
          >
            {loading ? 'Calcul en cours...' : 'Calculer'}
          </button>
        </div>

        {error && <div className="alert alert-danger">{error}</div>}

        {results && (
          <div className="card p-3">
            <h5>Résultats</h5>
            <table className="table table-bordered table-striped">
              <thead>
                <tr>
                  <th></th>
                  <th>Part Number Cover</th>
                  <th>Poids Total (kg)</th>
                  <th>Périmètre Total (cm)</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {results.map((r, i) => (
                  <React.Fragment key={i}>
                    <tr>
                      <td>
                        <button
                          className="btn btn-sm btn-outline-secondary"
                          onClick={() => this.toggleRow(i)}
                        >
                          {expandedRows[i] ? '−' : '+'}
                        </button>
                      </td>
                      <td>{r.partNumberCover}</td>
                      <td>
                        {r.totalWeight != null
                          ? r.totalWeight.toFixed(4)
                          : '—'}
                      </td>
                      <td>
                        {r.totalPerimeter != null
                          ? r.totalPerimeter.toFixed(2)
                          : '—'}
                      </td>
                      <td>
                        {r.errors && r.errors.length > 0 ? (
                          <span className="text-danger">
                            ❌ {r.errors[0]}
                          </span>
                        ) : (
                          <span className="text-success">✅ OK{r.saved ? ' (Sauvegardé)' : ''}</span>
                        )}
                      </td>
                    </tr>
                    {expandedRows[i] && r.patterns && (
                      <tr>
                        <td colSpan={5}>
                          <table className="table table-sm table-bordered mb-0">
                            <thead className="thead-light">
                              <tr>
                                <th>Pattern</th>
                                <th>Matériau</th>
                                <th>Surface (cm²)</th>
                                <th>Quantité</th>
                                <th>Poids/m² (kg)</th>
                                <th>Contribution (kg)</th>
                              </tr>
                            </thead>
                            <tbody>
                              {r.patterns.map((p, j) => (
                                <tr key={j}>
                                  <td>{p.pattern}</td>
                                  <td>{p.partNumberMaterial}</td>
                                  <td>{p.area != null ? p.area.toFixed(2) : '—'}</td>
                                  <td>{p.quantity}</td>
                                  <td>{p.weightUnit != null ? p.weightUnit.toFixed(4) : '—'}</td>
                                  <td>
                                    {p.weightContribution != null
                                      ? p.weightContribution.toFixed(4)
                                      : '—'}
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
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
    );
  }
}

export default WeightCalculation;
