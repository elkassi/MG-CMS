import React, { Component } from 'react';
import axios from 'axios';

class CuttingTimePerPartNumber extends Component {
  constructor(props) {
    super(props);
    this.state = {
      project: '',
      startDate: '',
      endDate: '',
      projects: [],
      loading: false,
      results: null,
      error: null,
    };
  }

  componentDidMount() {
    // Load available projects
    axios
      .get('/api/projet/list')
      .then((res) => {
        this.setState({ projects: res.data || [] });
      })
      .catch(() => {});
  }

  handleCalculate = () => {
    const { project, startDate, endDate } = this.state;
    if (!project) {
      this.setState({ error: 'Veuillez sélectionner un projet' });
      return;
    }

    this.setState({ loading: true, error: null });
    axios
      .post('/api/partNumberCuttingTime/calculate', {
        project,
        startDate: startDate || null,
        endDate: endDate || null,
      })
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

  render() {
    const { project, startDate, endDate, projects, loading, results, error } = this.state;

    return (
      <div className="container mt-4">
        <h3>Temps de Coupe par Part Number</h3>
        <p className="text-muted">
          Calculez le temps de coupe attribué à chaque Part Number basé sur la proportion de périmètre dans les plans de coupe actifs.
        </p>

        <div className="card p-3 mb-3">
          <div className="row">
            <div className="col-md-4">
              <div className="form-group">
                <label>Projet</label>
                <select
                  className="form-control"
                  value={project}
                  onChange={(e) => this.setState({ project: e.target.value })}
                >
                  <option value="">-- Sélectionner --</option>
                  {projects.map((p, i) => (
                    <option key={i} value={typeof p === 'string' ? p : p.nom || p.projet}>
                      {typeof p === 'string' ? p : p.nom || p.projet}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="col-md-3">
              <div className="form-group">
                <label>Date début</label>
                <input
                  type="date"
                  className="form-control"
                  value={startDate}
                  onChange={(e) => this.setState({ startDate: e.target.value })}
                />
              </div>
            </div>
            <div className="col-md-3">
              <div className="form-group">
                <label>Date fin</label>
                <input
                  type="date"
                  className="form-control"
                  value={endDate}
                  onChange={(e) => this.setState({ endDate: e.target.value })}
                />
              </div>
            </div>
            <div className="col-md-2 d-flex align-items-end">
              <button
                className="btn btn-primary"
                onClick={this.handleCalculate}
                disabled={loading}
              >
                {loading ? 'Calcul...' : 'Calculer'}
              </button>
            </div>
          </div>
        </div>

        {error && <div className="alert alert-danger">{error}</div>}

        {results &&
          results.map((planResult, pi) => (
            <div key={pi} className="card p-3 mb-3">
              {planResult.error ? (
                <div className="alert alert-warning">{planResult.error}</div>
              ) : (
                <>
                  <h5>
                    Plan de Coupe #{planResult.cuttingPlanId} — {planResult.cuttingPlanDescription || ''}
                  </h5>
                  <div className="row mb-2">
                    <div className="col-md-4">
                      <strong>Total Périmètre:</strong> {planResult.totalPerimetre?.toFixed(2) || 0} cm
                    </div>
                    <div className="col-md-4">
                      <strong>Total Temps de Coupe:</strong> {planResult.totalTempsDeCoupe?.toFixed(2) || 0} min
                    </div>
                  </div>

                  {planResult.errors && planResult.errors.length > 0 && (
                    <div className="alert alert-warning">
                      {planResult.errors.map((e, i) => (
                        <div key={i}>{e}</div>
                      ))}
                    </div>
                  )}

                  <table className="table table-bordered table-striped table-sm">
                    <thead>
                      <tr>
                        <th>Part Number</th>
                        <th>Périmètre (cm)</th>
                        <th>% du Plan</th>
                        <th>Temps de Coupe (min)</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {planResult.partNumbers &&
                        planResult.partNumbers.map((pn, i) => (
                          <tr key={i}>
                            <td>{pn.partNumber}</td>
                            <td>{pn.perimetrePN?.toFixed(2)}</td>
                            <td>{pn.percentagePerimetre?.toFixed(2)}%</td>
                            <td>{pn.tempsCoupePN?.toFixed(2)}</td>
                            <td>
                              {pn.saved ? (
                                <span className="text-success">✅ Sauvegardé</span>
                              ) : (
                                <span className="text-muted">—</span>
                              )}
                            </td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </>
              )}
            </div>
          ))}
      </div>
    );
  }
}

export default CuttingTimePerPartNumber;
