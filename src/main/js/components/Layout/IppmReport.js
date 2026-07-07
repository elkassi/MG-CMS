import React, { Component } from 'react';
import axios from 'axios';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSync, faFileCsv, faChartBar, faFilter, faChevronDown, faChevronUp, faSortUp, faSortDown, faSort, faTimes } from '@fortawesome/free-solid-svg-icons';
import { Modal } from 'react-bootstrap';

export default class IppmReport extends Component {
    constructor(props) {
        super(props);
        const today = new Date();
        const thirtyDaysAgo = new Date(today);
        thirtyDaysAgo.setDate(today.getDate() - 30);

        this.state = {
            startDate: thirtyDaysAgo.toISOString().split('T')[0],
            endDate: today.toISOString().split('T')[0],
            machine: '',
            placement: '',
            summary: null,
            byMachine: [],
            byCodeDefaut: [],
            byLieuDetection: [],
            byTableCoupe: [],
            byPartNumberMaterial: [],
            byCoupeur1: [],
            detailedReport: [],
            loading: false,
            viewMode: 'oneView', // 'oneView' or 'tabs'
            activeTab: 'summary',
            collapsedSections: {},
            // Sorting state for each table
            sortConfig: {},
            // Search/filter state for each table
            searchFilters: {},
            // Selected row for detail modal
            selectedRow: null,
            showDetailModal: false
        };
    }

    componentDidMount() {
        this.loadData();
    }

    loadData = () => {
        this.setState({ loading: true });
        const { startDate, endDate, machine, placement } = this.state;
        
        const params = new URLSearchParams();
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        if (machine) params.append('machine', machine);
        if (placement) params.append('placement', placement);

        Promise.all([
            axios.get(`/api/ippm/summary?${params.toString()}`),
            axios.get(`/api/ippm/byMachine?${params.toString()}`),
            axios.get(`/api/ippm/byCodeDefaut?${params.toString()}`),
            axios.get(`/api/ippm/byLieuDetection?${params.toString()}`),
            axios.get(`/api/ippm/byTableCoupe?${params.toString()}`),
            axios.get(`/api/ippm/byPartNumberMaterial?${params.toString()}`),
            axios.get(`/api/ippm/byCoupeur1?${params.toString()}`),
            axios.get(`/api/ippm/report?${params.toString()}`)
        ])
        .then(([summaryRes, machineRes, defautRes, lieuRes, tableCoupeRes, pnmRes, coupeurRes, reportRes]) => {
            this.setState({
                summary: summaryRes.data,
                byMachine: machineRes.data,
                byCodeDefaut: defautRes.data,
                byLieuDetection: lieuRes.data,
                byTableCoupe: tableCoupeRes.data,
                byPartNumberMaterial: pnmRes.data,
                byCoupeur1: coupeurRes.data,
                detailedReport: reportRes.data,
                loading: false
            });
        })
        .catch(error => {
            console.error('Error loading IPPM data:', error);
            this.setState({ loading: false });
        });
    }

    formatNumber = (num) => {
        if (num === null || num === undefined) return '-';
        return Number(num).toLocaleString('fr-FR', { maximumFractionDigits: 2 });
    }

    // Sorting functionality
    handleSort = (tableKey, field) => {
        this.setState(prev => {
            const currentSort = prev.sortConfig[tableKey] || {};
            let direction = 'asc';
            if (currentSort.field === field && currentSort.direction === 'asc') {
                direction = 'desc';
            }
            return {
                sortConfig: {
                    ...prev.sortConfig,
                    [tableKey]: { field, direction }
                }
            };
        });
    }

    getSortedData = (data, tableKey) => {
        const sortConfig = this.state.sortConfig[tableKey];
        if (!sortConfig || !sortConfig.field) return data;

        return [...data].sort((a, b) => {
            let aVal = a[sortConfig.field];
            let bVal = b[sortConfig.field];

            // Handle null/undefined
            if (aVal === null || aVal === undefined) aVal = '';
            if (bVal === null || bVal === undefined) bVal = '';

            // Check if numeric
            const aNum = typeof aVal === 'number' ? aVal : parseFloat(aVal);
            const bNum = typeof bVal === 'number' ? bVal : parseFloat(bVal);

            if (!isNaN(aNum) && !isNaN(bNum)) {
                return sortConfig.direction === 'asc' ? aNum - bNum : bNum - aNum;
            }

            // String comparison
            const comparison = String(aVal).localeCompare(String(bVal));
            return sortConfig.direction === 'asc' ? comparison : -comparison;
        });
    }

    // Filter functionality
    handleSearchChange = (tableKey, field, value) => {
        this.setState(prev => ({
            searchFilters: {
                ...prev.searchFilters,
                [tableKey]: {
                    ...prev.searchFilters[tableKey],
                    [field]: value
                }
            }
        }));
    }

    getFilteredData = (data, tableKey) => {
        const filters = this.state.searchFilters[tableKey] || {};
        
        return data.filter(row => {
            return Object.entries(filters).every(([field, filterValue]) => {
                if (!filterValue || filterValue === '') return true;
                
                const cellValue = row[field];
                if (cellValue === null || cellValue === undefined) return false;

                // Check if it's a number field
                if (typeof cellValue === 'number') {
                    const numFilter = parseFloat(filterValue);
                    if (!isNaN(numFilter)) {
                        return cellValue === numFilter;
                    }
                    return String(cellValue).includes(filterValue);
                }

                // String contains check
                return String(cellValue).toLowerCase().includes(String(filterValue).toLowerCase());
            });
        });
    }

    getSortIcon = (tableKey, field) => {
        const sortConfig = this.state.sortConfig[tableKey];
        if (!sortConfig || sortConfig.field !== field) {
            return <FontAwesomeIcon icon={faSort} className="ml-1 text-muted" style={{ opacity: 0.5 }} />;
        }
        return sortConfig.direction === 'asc' 
            ? <FontAwesomeIcon icon={faSortUp} className="ml-1" />
            : <FontAwesomeIcon icon={faSortDown} className="ml-1" />;
    }

    // Row double-click to show details
    handleRowDoubleClick = (row) => {
        this.setState({
            selectedRow: row,
            showDetailModal: true
        });
    }

    renderDetailModal = () => {
        const { selectedRow, showDetailModal } = this.state;
        if (!selectedRow) return null;

        return (
            <Modal
                show={showDetailModal}
                onHide={() => this.setState({ showDetailModal: false, selectedRow: null })}
                size="lg"
                centered
            >
                <Modal.Header className="bg-dark text-white">
                    <Modal.Title>Détails de l'entrée</Modal.Title>
                    <button 
                        type="button" 
                        className="close text-white" 
                        onClick={() => this.setState({ showDetailModal: false, selectedRow: null })}
                    >
                        <FontAwesomeIcon icon={faTimes} />
                    </button>
                </Modal.Header>
                <Modal.Body>
                    <div className="row">
                        {Object.entries(selectedRow).map(([key, value]) => (
                            <div className="col-md-6 mb-2" key={key}>
                                <div className="card">
                                    <div className="card-body p-2">
                                        <strong>{this.formatFieldName(key)}:</strong>
                                        <div className="text-primary">
                                            {value !== null && value !== undefined 
                                                ? (typeof value === 'number' ? this.formatNumber(value) : String(value))
                                                : '-'}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <button 
                        className="btn btn-secondary" 
                        onClick={() => this.setState({ showDetailModal: false, selectedRow: null })}
                    >
                        Fermer
                    </button>
                </Modal.Footer>
            </Modal>
        );
    }

    formatFieldName = (field) => {
        // Convert camelCase to readable format
        return field
            .replace(/([A-Z])/g, ' $1')
            .replace(/^./, str => str.toUpperCase())
            .trim();
    }

    downloadCSV = () => {
        const { detailedReport } = this.state;
        if (detailedReport.length === 0) return;

        const headers = ['Machine', 'Placement', 'Table Qualité', 'Lieu Détection', 
            'Code Défaut', 'Code Scrap', 'Non Conforme', 'Scrap', 'Total Pièces', 
            'IPPM Non Conforme', 'IPPM Scrap'];
        
        const rows = detailedReport.map(row => [
            row.machine || '',
            row.placement || '',
            row.tableCoupe || '',
            row.lieuDetection || '',
            row.codeDefaut || '',
            row.codeScrap || '',
            row.totalNonConforme || 0,
            row.totalScrap || 0,
            row.totalPieces || 0,
            row.ippmNonConforme?.toFixed(2) || 0,
            row.ippmScrap?.toFixed(2) || 0
        ]);

        let csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `ippm-report-${this.state.startDate}-${this.state.endDate}.csv`;
        a.click();
        window.URL.revokeObjectURL(url);
    }

    toggleSection = (section) => {
        this.setState(prev => ({
            collapsedSections: {
                ...prev.collapsedSections,
                [section]: !prev.collapsedSections[section]
            }
        }));
    }

    renderCollapsibleSection = (title, sectionKey, content, bgColor = 'dark') => {
        const { collapsedSections } = this.state;
        const isCollapsed = collapsedSections[sectionKey];
        
        return (
            <div className="card mb-3">
                <div 
                    className={`card-header bg-${bgColor} text-white d-flex justify-content-between align-items-center`}
                    style={{ cursor: 'pointer' }}
                    onClick={() => this.toggleSection(sectionKey)}
                >
                    <strong>{title}</strong>
                    <FontAwesomeIcon icon={isCollapsed ? faChevronDown : faChevronUp} />
                </div>
                {!isCollapsed && (
                    <div className="card-body p-0">
                        {content}
                    </div>
                )}
            </div>
        );
    }

    renderSortableTable = (data, columns, tableKey, keyField) => {
        if (!data || data.length === 0) {
            return <div className="text-center text-muted p-3">Aucune donnée</div>;
        }

        // Apply filtering then sorting
        let filteredData = this.getFilteredData(data, tableKey);
        let sortedData = this.getSortedData(filteredData, tableKey);
        const filters = this.state.searchFilters[tableKey] || {};

        return (
            <div className="table-responsive" style={{ maxHeight: '400px', overflowY: 'auto' }}>
                <table className="table table-striped table-bordered table-hover table-sm mb-0">
                    <thead className="thead-light" style={{ position: 'sticky', top: 0, zIndex: 1 }}>
                        {/* Header row with sorting */}
                        <tr>
                            {columns.map((col, i) => (
                                <th 
                                    key={i} 
                                    onClick={() => this.handleSort(tableKey, col.field)}
                                    style={{ cursor: 'pointer', whiteSpace: 'nowrap' }}
                                >
                                    {col.header}
                                    {this.getSortIcon(tableKey, col.field)}
                                </th>
                            ))}
                        </tr>
                        {/* Filter row */}
                        <tr>
                            {columns.map((col, i) => (
                                <th key={`filter-${i}`} style={{ padding: '2px' }}>
                                    <input
                                        type="text"
                                        className="form-control form-control-sm"
                                        placeholder="Filtrer..."
                                        value={filters[col.field] || ''}
                                        onChange={(e) => this.handleSearchChange(tableKey, col.field, e.target.value)}
                                        onClick={(e) => e.stopPropagation()}
                                        style={{ fontSize: '11px' }}
                                    />
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {sortedData.map((row, index) => (
                            <tr 
                                key={row[keyField] || index}
                                onDoubleClick={() => this.handleRowDoubleClick(row)}
                                style={{ cursor: 'pointer' }}
                                title="Double-cliquez pour voir les détails"
                            >
                                {columns.map((col, i) => (
                                    <td key={i} className={col.className || ''}>
                                        {col.format ? col.format(row[col.field]) : row[col.field]}
                                    </td>
                                ))}
                            </tr>
                        ))}
                        {sortedData.length === 0 && (
                            <tr>
                                <td colSpan={columns.length} className="text-center text-muted">
                                    Aucun résultat pour les filtres appliqués
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        );
    }

    renderMiniTable = (data, columns, keyField) => {
        // Keep the old renderMiniTable for backward compatibility, now with sorting
        return this.renderSortableTable(data, columns, keyField, keyField);
    }

    render() {
        const { summary, byMachine, byCodeDefaut, byLieuDetection, byTableCoupe, 
                byPartNumberMaterial, byCoupeur1, detailedReport, 
                loading, viewMode, activeTab, startDate, endDate, machine, placement } = this.state;

        return (
            <div className="container-fluid" style={{ padding: '20px' }}>
                {this.renderDetailModal()}
                
                <div className="d-flex justify-content-between align-items-center mb-4">
                    <h2>
                        <FontAwesomeIcon icon={faChartBar} className="mr-2" />
                        IPPM Report (Internal Parts Per Million)
                    </h2>
                    <div>
                        <div className="btn-group mr-2">
                            <button 
                                className={`btn ${viewMode === 'oneView' ? 'btn-primary' : 'btn-outline-primary'}`}
                                onClick={() => this.setState({ viewMode: 'oneView' })}
                            >
                                Vue Globale
                            </button>
                            <button 
                                className={`btn ${viewMode === 'tabs' ? 'btn-primary' : 'btn-outline-primary'}`}
                                onClick={() => this.setState({ viewMode: 'tabs' })}
                            >
                                Onglets
                            </button>
                        </div>
                        <button 
                            className="btn btn-success mr-2"
                            onClick={this.downloadCSV}
                            disabled={loading || detailedReport.length === 0}
                        >
                            <FontAwesomeIcon icon={faFileCsv} className="mr-2" />
                            Exporter CSV
                        </button>
                        <button 
                            className="btn btn-secondary"
                            onClick={this.loadData}
                            disabled={loading}
                        >
                            <FontAwesomeIcon icon={faSync} spin={loading} className="mr-2" />
                            Rafraîchir
                        </button>
                    </div>
                </div>

                {/* Filters */}
                <div className="card mb-4">
                    <div className="card-header bg-dark text-white">
                        <FontAwesomeIcon icon={faFilter} className="mr-2" />
                        Filtres
                    </div>
                    <div className="card-body">
                        <div className="row">
                            <div className="col-md-3">
                                <label>Date début:</label>
                                <input 
                                    type="date" 
                                    className="form-control"
                                    value={startDate}
                                    onChange={(e) => this.setState({ startDate: e.target.value })}
                                />
                            </div>
                            <div className="col-md-3">
                                <label>Date fin:</label>
                                <input 
                                    type="date" 
                                    className="form-control"
                                    value={endDate}
                                    onChange={(e) => this.setState({ endDate: e.target.value })}
                                />
                            </div>
                            <div className="col-md-2">
                                <label>Machine:</label>
                                <input 
                                    type="text" 
                                    className="form-control"
                                    placeholder="Ex: AA1"
                                    value={machine}
                                    onChange={(e) => this.setState({ machine: e.target.value })}
                                />
                            </div>
                            <div className="col-md-2">
                                <label>Placement:</label>
                                <input 
                                    type="text" 
                                    className="form-control"
                                    placeholder="Placement..."
                                    value={placement}
                                    onChange={(e) => this.setState({ placement: e.target.value })}
                                />
                            </div>
                            <div className="col-md-2 d-flex align-items-end">
                                <button 
                                    className="btn btn-primary btn-block"
                                    onClick={this.loadData}
                                    disabled={loading}
                                >
                                    Rechercher
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Summary Cards */}
                {summary && (
                    <div className="row mb-4">
                        <div className="col-md-3">
                            <div className="card bg-primary text-white">
                                <div className="card-body text-center">
                                    <h5>Total Pièces</h5>
                                    <h2>{this.formatNumber(summary.totalPieces)}</h2>
                                </div>
                            </div>
                        </div>
                        <div className="col-md-3">
                            <div className="card bg-warning text-dark">
                                <div className="card-body text-center">
                                    <h5>Non Conforme</h5>
                                    <h2>{this.formatNumber(summary.totalNonConforme)}</h2>
                                    <small>IPPM: {this.formatNumber(summary.ippmNonConforme)}</small>
                                </div>
                            </div>
                        </div>
                        <div className="col-md-3">
                            <div className="card bg-danger text-white">
                                <div className="card-body text-center">
                                    <h5>Scrap</h5>
                                    <h2>{this.formatNumber(summary.totalScrap)}</h2>
                                    <small>IPPM: {this.formatNumber(summary.ippmScrap)}</small>
                                </div>
                            </div>
                        </div>
                        <div className="col-md-3">
                            <div className="card bg-info text-white">
                                <div className="card-body text-center">
                                    <h5>IPPM Total</h5>
                                    <h2>{this.formatNumber((summary.ippmNonConforme || 0) + (summary.ippmScrap || 0))}</h2>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {/* Tabs */}
                {viewMode === 'tabs' && (
                <ul className="nav nav-tabs mb-3">
                    <li className="nav-item">
                        <button 
                            className={`nav-link ${activeTab === 'summary' ? 'active' : ''}`}
                            onClick={() => this.setState({ activeTab: 'summary' })}
                        >
                            Par Machine
                        </button>
                    </li>
                    <li className="nav-item">
                        <button 
                            className={`nav-link ${activeTab === 'defaut' ? 'active' : ''}`}
                            onClick={() => this.setState({ activeTab: 'defaut' })}
                        >
                            Par Code Défaut
                        </button>
                    </li>
                    <li className="nav-item">
                        <button 
                            className={`nav-link ${activeTab === 'tableCoupe' ? 'active' : ''}`}
                            onClick={() => this.setState({ activeTab: 'tableCoupe' })}
                        >
                            Par Table Coupe
                        </button>
                    </li>
                    <li className="nav-item">
                        <button 
                            className={`nav-link ${activeTab === 'partNumber' ? 'active' : ''}`}
                            onClick={() => this.setState({ activeTab: 'partNumber' })}
                        >
                            Par Part Number
                        </button>
                    </li>
                    <li className="nav-item">
                        <button 
                            className={`nav-link ${activeTab === 'coupeur' ? 'active' : ''}`}
                            onClick={() => this.setState({ activeTab: 'coupeur' })}
                        >
                            Par Coupeur
                        </button>
                    </li>
                    <li className="nav-item">
                        <button 
                            className={`nav-link ${activeTab === 'lieu' ? 'active' : ''}`}
                            onClick={() => this.setState({ activeTab: 'lieu' })}
                        >
                            Par Lieu Détection
                        </button>
                    </li>
                    <li className="nav-item">
                        <button 
                            className={`nav-link ${activeTab === 'detail' ? 'active' : ''}`}
                            onClick={() => this.setState({ activeTab: 'detail' })}
                        >
                            Détails
                        </button>
                    </li>
                </ul>
                )}

                {loading ? (
                    <div className="text-center p-5">
                        <FontAwesomeIcon icon={faSync} spin size="3x" />
                    </div>
                ) : viewMode === 'oneView' ? (
                    /* One View Mode - All sections visible */
                    <div className="row">
                        {/* Left Column */}
                        <div className="col-lg-6">
                            {this.renderCollapsibleSection('Par Code Défaut', 'defaut',
                                this.renderMiniTable(byCodeDefaut, [
                                    { field: 'codeDefaut', header: 'Code', format: (v) => <strong>{v}</strong> },
                                    { field: 'description', header: 'Description' },
                                    { field: 'occurrences', header: 'Occ.' },
                                    { field: 'totalNonConforme', header: 'Non Conf.', className: 'text-warning', format: this.formatNumber }
                                ], 'codeDefaut'),
                                'danger'
                            )}

                            {this.renderCollapsibleSection('Par Table Coupe', 'tableCoupe',
                                this.renderMiniTable(byTableCoupe, [
                                    { field: 'tableCoupe', header: 'Table Coupe', format: (v) => <strong>{v || '-'}</strong> },
                                    { field: 'totalPieces', header: 'Pièces', format: this.formatNumber },
                                    { field: 'totalNonConforme', header: 'Non Conf.', className: 'text-warning', format: this.formatNumber },
                                    { field: 'totalScrap', header: 'Scrap', className: 'text-danger', format: this.formatNumber },
                                    { field: 'ippmNonConforme', header: 'IPPM NC', format: this.formatNumber }
                                ], 'tableCoupe'),
                                'info'
                            )}

                            {this.renderCollapsibleSection('Par Coupeur', 'coupeur',
                                this.renderMiniTable(byCoupeur1, [
                                    { field: 'coupeur', header: 'Coupeur', format: (v) => <strong>{v || '-'}</strong> },
                                    { field: 'totalPieces', header: 'Pièces', format: this.formatNumber },
                                    { field: 'totalNonConforme', header: 'Non Conf.', className: 'text-warning', format: this.formatNumber },
                                    { field: 'totalScrap', header: 'Scrap', className: 'text-danger', format: this.formatNumber },
                                    { field: 'ippmNonConforme', header: 'IPPM NC', format: this.formatNumber }
                                ], 'coupeur'),
                                'secondary'
                            )}
                        </div>

                        {/* Right Column */}
                        <div className="col-lg-6">
                            {this.renderCollapsibleSection('Par Machine', 'machine',
                                this.renderMiniTable(byMachine, [
                                    { field: 'machine', header: 'Machine', format: (v) => <strong>{v}</strong> },
                                    { field: 'totalPieces', header: 'Pièces', format: this.formatNumber },
                                    { field: 'totalNonConforme', header: 'Non Conf.', className: 'text-warning', format: this.formatNumber },
                                    { field: 'ippmNonConforme', header: 'IPPM NC', format: this.formatNumber },
                                    { field: 'totalScrap', header: 'Scrap', className: 'text-danger', format: this.formatNumber },
                                    { field: 'ippmScrap', header: 'IPPM S', format: this.formatNumber }
                                ], 'machine'),
                                'primary'
                            )}

                            {this.renderCollapsibleSection('Par Part Number Material', 'partNumber',
                                this.renderMiniTable(byPartNumberMaterial, [
                                    { field: 'partNumberMaterial', header: 'PN Material', format: (v) => <strong>{v || '-'}</strong> },
                                    { field: 'totalPieces', header: 'Pièces', format: this.formatNumber },
                                    { field: 'totalNonConforme', header: 'Non Conf.', className: 'text-warning', format: this.formatNumber },
                                    { field: 'totalScrap', header: 'Scrap', className: 'text-danger', format: this.formatNumber },
                                    { field: 'ippmNonConforme', header: 'IPPM NC', format: this.formatNumber }
                                ], 'partNumberMaterial'),
                                'warning'
                            )}

                            {this.renderCollapsibleSection('Par Lieu Détection', 'lieu',
                                this.renderMiniTable(byLieuDetection, [
                                    { field: 'lieuDetection', header: 'Lieu', format: (v) => <strong>{v}</strong> },
                                    { field: 'occurrences', header: 'Occ.' },
                                    { field: 'totalNonConforme', header: 'Non Conf.', className: 'text-warning', format: this.formatNumber },
                                    { field: 'totalScrap', header: 'Scrap', className: 'text-danger', format: this.formatNumber }
                                ], 'lieuDetection'),
                                'dark'
                            )}
                        </div>
                    </div>
                ) : (
                    /* Tabs Mode */
                    <>
                        {/* By Machine */}
                        {activeTab === 'summary' && (
                            <div className="table-responsive">
                                <table className="table table-striped table-bordered table-hover">
                                    <thead className="thead-dark">
                                        <tr>
                                            <th>Machine</th>
                                            <th>Total Pièces</th>
                                            <th>Non Conforme</th>
                                            <th>IPPM Non Conforme</th>
                                            <th>Scrap</th>
                                            <th>IPPM Scrap</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {byMachine.map((row, index) => (
                                            <tr key={index}>
                                                <td><strong>{row.machine}</strong></td>
                                                <td>{this.formatNumber(row.totalPieces)}</td>
                                                <td className="text-warning">{this.formatNumber(row.totalNonConforme)}</td>
                                                <td>{this.formatNumber(row.ippmNonConforme)}</td>
                                                <td className="text-danger">{this.formatNumber(row.totalScrap)}</td>
                                                <td>{this.formatNumber(row.ippmScrap)}</td>
                                            </tr>
                                        ))}
                                        {byMachine.length === 0 && (
                                            <tr>
                                                <td colSpan="6" className="text-center text-muted">Aucune donnée</td>
                                            </tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        )}

                        {/* By Code Defaut */}
                        {activeTab === 'defaut' && (
                            <div className="table-responsive">
                                <table className="table table-striped table-bordered table-hover">
                                    <thead className="thead-dark">
                                        <tr>
                                            <th>Code Défaut</th>
                                            <th>Description</th>
                                            <th>Occurrences</th>
                                            <th>Total Non Conforme</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {byCodeDefaut.map((row, index) => (
                                            <tr key={index}>
                                                <td><strong>{row.codeDefaut}</strong></td>
                                                <td>{row.description}</td>
                                                <td>{row.occurrences}</td>
                                                <td className="text-warning">{this.formatNumber(row.totalNonConforme)}</td>
                                            </tr>
                                        ))}
                                        {byCodeDefaut.length === 0 && (
                                            <tr>
                                                <td colSpan="4" className="text-center text-muted">Aucune donnée</td>
                                            </tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        )}

                        {/* By Lieu Detection */}
                        {activeTab === 'lieu' && (
                            <div className="table-responsive">
                                <table className="table table-striped table-bordered table-hover">
                                    <thead className="thead-dark">
                                        <tr>
                                            <th>Lieu Détection</th>
                                            <th>Occurrences</th>
                                            <th>Non Conforme</th>
                                            <th>Scrap</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {byLieuDetection.map((row, index) => (
                                            <tr key={index}>
                                                <td><strong>{row.lieuDetection}</strong></td>
                                                <td>{row.occurrences}</td>
                                                <td className="text-warning">{this.formatNumber(row.totalNonConforme)}</td>
                                                <td className="text-danger">{this.formatNumber(row.totalScrap)}</td>
                                            </tr>
                                        ))}
                                        {byLieuDetection.length === 0 && (
                                            <tr>
                                                <td colSpan="4" className="text-center text-muted">Aucune donnée</td>
                                            </tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        )}

                        {/* By Table Coupe */}
                        {activeTab === 'tableCoupe' && (
                            <div className="table-responsive">
                                <table className="table table-striped table-bordered table-hover">
                                    <thead className="thead-dark">
                                        <tr>
                                            <th>Table Coupe</th>
                                            <th>Total Pièces</th>
                                            <th>Non Conforme</th>
                                            <th>IPPM Non Conforme</th>
                                            <th>Scrap</th>
                                            <th>IPPM Scrap</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {byTableCoupe.map((row, index) => (
                                            <tr key={index}>
                                                <td><strong>{row.tableCoupe || '-'}</strong></td>
                                                <td>{this.formatNumber(row.totalPieces)}</td>
                                                <td className="text-warning">{this.formatNumber(row.totalNonConforme)}</td>
                                                <td>{this.formatNumber(row.ippmNonConforme)}</td>
                                                <td className="text-danger">{this.formatNumber(row.totalScrap)}</td>
                                                <td>{this.formatNumber(row.ippmScrap)}</td>
                                            </tr>
                                        ))}
                                        {byTableCoupe.length === 0 && (
                                            <tr>
                                                <td colSpan="6" className="text-center text-muted">Aucune donnée</td>
                                            </tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        )}

                        {/* By Part Number Material */}
                        {activeTab === 'partNumber' && (
                            <div className="table-responsive">
                                <table className="table table-striped table-bordered table-hover">
                                    <thead className="thead-dark">
                                        <tr>
                                            <th>Part Number Material</th>
                                            <th>Total Pièces</th>
                                            <th>Non Conforme</th>
                                            <th>IPPM Non Conforme</th>
                                            <th>Scrap</th>
                                            <th>IPPM Scrap</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {byPartNumberMaterial.map((row, index) => (
                                            <tr key={index}>
                                                <td><strong>{row.partNumberMaterial || '-'}</strong></td>
                                                <td>{this.formatNumber(row.totalPieces)}</td>
                                                <td className="text-warning">{this.formatNumber(row.totalNonConforme)}</td>
                                                <td>{this.formatNumber(row.ippmNonConforme)}</td>
                                                <td className="text-danger">{this.formatNumber(row.totalScrap)}</td>
                                                <td>{this.formatNumber(row.ippmScrap)}</td>
                                            </tr>
                                        ))}
                                        {byPartNumberMaterial.length === 0 && (
                                            <tr>
                                                <td colSpan="6" className="text-center text-muted">Aucune donnée</td>
                                            </tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        )}

                        {/* By Coupeur */}
                        {activeTab === 'coupeur' && (
                            <div className="table-responsive">
                                <table className="table table-striped table-bordered table-hover">
                                    <thead className="thead-dark">
                                        <tr>
                                            <th>Coupeur</th>
                                            <th>Total Pièces</th>
                                            <th>Non Conforme</th>
                                            <th>IPPM Non Conforme</th>
                                            <th>Scrap</th>
                                            <th>IPPM Scrap</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {byCoupeur1.map((row, index) => (
                                            <tr key={index}>
                                                <td><strong>{row.coupeur || '-'}</strong></td>
                                                <td>{this.formatNumber(row.totalPieces)}</td>
                                                <td className="text-warning">{this.formatNumber(row.totalNonConforme)}</td>
                                                <td>{this.formatNumber(row.ippmNonConforme)}</td>
                                                <td className="text-danger">{this.formatNumber(row.totalScrap)}</td>
                                                <td>{this.formatNumber(row.ippmScrap)}</td>
                                            </tr>
                                        ))}
                                        {byCoupeur1.length === 0 && (
                                            <tr>
                                                <td colSpan="6" className="text-center text-muted">Aucune donnée</td>
                                            </tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        )}

                        {/* Detailed Report */}
                        {activeTab === 'detail' && (
                            <div className="table-responsive">
                                <table className="table table-striped table-bordered table-hover table-sm">
                                    <thead className="thead-dark">
                                        <tr>
                                            <th>Machine</th>
                                            <th>Placement</th>
                                            <th>Table Qualité</th>
                                            <th>Lieu Détection</th>
                                            <th>Code Défaut</th>
                                            <th>Code Scrap</th>
                                            <th>Non Conforme</th>
                                            <th>Scrap</th>
                                            <th>Total Pièces</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {detailedReport.map((row, index) => (
                                            <tr key={index}>
                                                <td>{row.machine}</td>
                                                <td>{row.placement}</td>
                                                <td>{row.tableCoupe}</td>
                                                <td>{row.lieuDetection}</td>
                                                <td>{row.codeDefaut}</td>
                                                <td>{row.codeScrap}</td>
                                                <td className="text-warning">{this.formatNumber(row.totalNonConforme)}</td>
                                                <td className="text-danger">{this.formatNumber(row.totalScrap)}</td>
                                                <td>{this.formatNumber(row.totalPieces)}</td>
                                            </tr>
                                        ))}
                                        {detailedReport.length === 0 && (
                                            <tr>
                                                <td colSpan="9" className="text-center text-muted">Aucune donnée</td>
                                            </tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </>
                )}
            </div>
        );
    }
}
