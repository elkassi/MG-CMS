import React, { Component } from 'react'
import '../../styles/StockReportVerification.scss'
import axios from 'axios'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faSearch, faCalendarAlt, faExclamationTriangle, faDownload, faSpinner } from '@fortawesome/free-solid-svg-icons'

export default class StockReportVerification extends Component {
  constructor() {
    super();
    this.state = {
      startDate: '',
      endDate: '',
      reportData: [],
      loading: false,
      error: null,
      hasSearched: false
    }
  }

  componentDidMount() {
    // Set default dates (last 30 days)
    const today = new Date();
    const thirtyDaysAgo = new Date(today.getTime() - (30 * 24 * 60 * 60 * 1000));
    
    this.setState({
      endDate: today.toISOString().split('T')[0],
      startDate: thirtyDaysAgo.toISOString().split('T')[0]
    });
  }

  handleInputChange = (e) => {
    this.setState({ [e.target.name]: e.target.value });
  }

  launchVerification = async () => {
    const { startDate, endDate } = this.state;
    
    if (!startDate || !endDate) {
      this.setState({ error: 'Veuillez sélectionner les dates de début et de fin' });
      return;
    }

    if (new Date(startDate) > new Date(endDate)) {
      this.setState({ error: 'La date de début doit être antérieure à la date de fin' });
      return;
    }

    this.setState({ loading: true, error: null, hasSearched: false });

    try {
      const startDateTime = new Date(startDate + 'T00:00:00').toISOString();
      const endDateTime = new Date(endDate + 'T23:59:59').toISOString();
      
      const response = await axios.get('/api/cuttingRequestSerieRouleauData/badLinesInReport', {
        params: {
          startDate: startDateTime,
          endDate: endDateTime
        }
      });
      
      this.setState({ 
        reportData: response.data,
        loading: false,
        hasSearched: true
      });
    } catch (error) {
      console.error('Error fetching verification data:', error);
      this.setState({ 
        error: 'Erreur lors de la récupération des données: ' + (error.response?.data?.message || error.message),
        loading: false,
        hasSearched: true
      });
    }
  }

  exportToCSV = () => {
    const { reportData } = this.state;
    if (reportData.length === 0) return;

    const headers = [
      'Item Number', 'UM', 'ABC', 'Site', 'Last Count', 
      'Location', 'Ref', 'Qty On Hand', 'Status', 'Last Updated', 'Serie'
    ];
    
    const csvContent = [
      headers.join(','),
      ...reportData.map(row => [
        row.itemNumber || '',
        row.um || '',
        row.abc || '',
        row.site || '',
        row.lastCnt || '',
        row.location || '',
        row.ref || '',
        row.qtyOnHand || '',
        row.status || '',
        row.lastUpdated || '',
        row.serie || ''
      ].join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `stock_verification_${this.state.startDate}_${this.state.endDate}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  formatDate = (dateString) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('fr-FR');
  }

  render() {
    const { startDate, endDate, reportData, loading, error, hasSearched } = this.state;

    return (
      <div className="stock-verification-container">
        <div className="stock-verification-header">
          <h2>
            <FontAwesomeIcon icon={faExclamationTriangle} className="header-icon" />
            Vérification du Rapport de Stock
          </h2>
          <p className="header-description">
            Rechercher les lignes problématiques dans les rapports de stock pour une période donnée
          </p>
        </div>

        <div className="search-section">
          <div className="date-inputs">
            <div className="input-group">
              <label htmlFor="startDate">
                <FontAwesomeIcon icon={faCalendarAlt} />
                Date de début
              </label>
              <input
                type="date"
                id="startDate"
                name="startDate"
                value={startDate}
                onChange={this.handleInputChange}
                className="date-input"
              />
            </div>
            
            <div className="input-group">
              <label htmlFor="endDate">
                <FontAwesomeIcon icon={faCalendarAlt} />
                Date de fin
              </label>
              <input
                type="date"
                id="endDate"
                name="endDate"
                value={endDate}
                onChange={this.handleInputChange}
                className="date-input"
              />
            </div>
          </div>

          <button 
            onClick={this.launchVerification}
            disabled={loading}
            className="verification-button"
          >
            {loading ? (
              <>
                <FontAwesomeIcon icon={faSpinner} spin />
                Vérification en cours...
              </>
            ) : (
              <>
                <FontAwesomeIcon icon={faSearch} />
                Lancer la vérification
              </>
            )}
          </button>
        </div>

        {error && (
          <div className="error-message">
            <FontAwesomeIcon icon={faExclamationTriangle} />
            {error}
          </div>
        )}

        {hasSearched && (
          <div className="results-section">
            <div className="results-header">
              <h3>
                Résultats de la vérification
                {reportData.length > 0 && (
                  <span className="result-count">
                    ({reportData.length} ligne{reportData.length > 1 ? 's' : ''} problématique{reportData.length > 1 ? 's' : ''})
                  </span>
                )}
              </h3>
              
              {reportData.length > 0 && (
                <button onClick={this.exportToCSV} className="export-button">
                  <FontAwesomeIcon icon={faDownload} />
                  Exporter CSV
                </button>
              )}
            </div>

            {reportData.length === 0 ? (
              <div className="no-results">
                <FontAwesomeIcon icon={faSearch} className="no-results-icon" />
                <h4>Aucune ligne problématique trouvée</h4>
                <p>Tous les rapports de stock sont conformes pour la période sélectionnée.</p>
              </div>
            ) : (
              <div className="table-container">
                <table className="verification-table">
                  <thead>
                    <tr>
                      <th>Item Number</th>
                      <th>UM</th>
                      <th>ABC</th>
                      <th>Site</th>
                      <th>Last Count</th>
                      <th>Location</th>
                      <th>Ref</th>
                      <th>Qty On Hand</th>
                      <th>Status</th>
                      <th>Last Updated</th>
                      <th>Serie</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reportData.map((item, index) => (
                      <tr key={`${item.itemNumber}-${item.location}-${item.ref}-${index}`}>
                        <td>{item.itemNumber || '-'}</td>
                        <td>{item.um || '-'}</td>
                        <td>{item.abc || '-'}</td>
                        <td>{item.site || '-'}</td>
                        <td>{item.lastCnt || '-'}</td>
                        <td>{item.location || '-'}</td>
                        <td>{item.ref || '-'}</td>
                        <td className="qty-cell">{item.qtyOnHand !== null ? item.qtyOnHand : '-'}</td>
                        <td>
                          <span className={`status-badge ${item.status?.toLowerCase()}`}>
                            {item.status || '-'}
                          </span>
                        </td>
                        <td>{this.formatDate(item.lastUpdated)}</td>
                        <td>{item.serie || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </div>
    )
  }
}
