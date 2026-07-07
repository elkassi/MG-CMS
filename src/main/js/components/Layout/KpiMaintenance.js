import React, { Component } from 'react';
import axios from 'axios';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSync, faFileCsv, faChartBar, faFilter, faChevronDown, faChevronUp, faWrench, faClock, faExclamationTriangle, faCheckCircle, faTimesCircle } from '@fortawesome/free-solid-svg-icons';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

class KpiMaintenance extends Component {
    constructor(props) {
        super(props);
        const today = new Date();
        const thirtyDaysAgo = new Date(today);
        thirtyDaysAgo.setDate(today.getDate() - 30);

        this.state = {
            startDate: thirtyDaysAgo.toISOString().split('T')[0],
            endDate: today.toISOString().split('T')[0],
            machine: '',
            data: [],
            summary: null,
            byMachine: [],
            byCodeArret: [],
            byShift: [],
            byDay: [],
            byWeek: [],
            loading: false,
            viewMode: 'dashboard',
            collapsedSections: {},
            selectedRow: null
        };
    }

    componentDidMount() {
        this.loadData();
    }

    loadData = () => {
        this.setState({ loading: true });
        const { startDate, endDate, machine } = this.state;
        
        const params = new URLSearchParams();
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        if (machine) params.append('machine', machine);

        axios.get(`/api/intervention/kpi-maintenance?${params.toString()}`)
            .then(res => {
                const data = res.data || [];
                this.processData(data);
            })
            .catch(error => {
                console.error('Error loading maintenance data:', error);
                this.setState({ loading: false });
            });
    }

    processData = (data) => {
        // Calculate KPI metrics
        const summary = this.calculateSummary(data);
        const byMachine = this.groupByMachine(data);
        const byCodeArret = this.groupByCodeArret(data);
        const byShift = this.groupByShift(data);
        const byDay = this.groupByDay(data);
        const byWeek = this.groupByWeek(data);

        this.setState({
            data,
            summary,
            byMachine,
            byCodeArret,
            byShift,
            byDay,
            byWeek,
            loading: false
        });
    }

    calculateSummary = (data) => {
        if (!data || data.length === 0) return null;

        let totalInterventions = data.length;
        let totalDowntime = 0; // Minutes machine stopped (debutArret to finIntervention)
        let totalReactionTime = 0; // Minutes from debutArret to debutIntervention
        let totalRepairTime = 0; // Minutes from debutIntervention to finIntervention
        let resolvedCount = 0;
        let unresolvedCount = 0;
        let validatedCount = 0;

        data.forEach(item => {
            const debutArret = item.debutArret ? new Date(item.debutArret) : null;
            const debutIntervention = item.debutIntervention ? new Date(item.debutIntervention) : null;
            const finIntervention = item.finIntervention ? new Date(item.finIntervention) : null;

            // Total downtime
            if (debutArret && finIntervention) {
                totalDowntime += (finIntervention - debutArret) / 60000;
            }

            // Reaction time (time until maintenance starts)
            if (debutArret && debutIntervention) {
                totalReactionTime += (debutIntervention - debutArret) / 60000;
            }

            // Repair time (time to fix)
            if (debutIntervention && finIntervention) {
                totalRepairTime += (finIntervention - debutIntervention) / 60000;
            }

            // Problem resolved status
            if (item.problemeResolu === 'Oui') {
                resolvedCount++;
            } else if (item.problemeResolu === 'Non') {
                unresolvedCount++;
            }

            // Validated count
            if (item.dateValidation) {
                validatedCount++;
            }
        });

        const avgReactionTime = totalInterventions > 0 ? totalReactionTime / totalInterventions : 0;
        const avgRepairTime = totalInterventions > 0 ? totalRepairTime / totalInterventions : 0;
        const avgDowntime = totalInterventions > 0 ? totalDowntime / totalInterventions : 0;
        const resolutionRate = totalInterventions > 0 ? (resolvedCount / totalInterventions) * 100 : 0;

        return {
            totalInterventions,
            totalDowntime: Math.round(totalDowntime),
            totalReactionTime: Math.round(totalReactionTime),
            totalRepairTime: Math.round(totalRepairTime),
            avgReactionTime: Math.round(avgReactionTime * 10) / 10,
            avgRepairTime: Math.round(avgRepairTime * 10) / 10,
            avgDowntime: Math.round(avgDowntime * 10) / 10,
            resolvedCount,
            unresolvedCount,
            validatedCount,
            pendingValidation: totalInterventions - validatedCount,
            resolutionRate: Math.round(resolutionRate * 10) / 10
        };
    }

    groupByMachine = (data) => {
        const grouped = {};
        data.forEach(item => {
            const machine = item.machine || 'Non spécifié';
            if (!grouped[machine]) {
                grouped[machine] = { 
                    machine, 
                    count: 0, 
                    totalDowntime: 0, 
                    totalReactionTime: 0,
                    totalRepairTime: 0,
                    resolved: 0 
                };
            }
            grouped[machine].count++;
            
            const debutArret = item.debutArret ? new Date(item.debutArret) : null;
            const debutIntervention = item.debutIntervention ? new Date(item.debutIntervention) : null;
            const finIntervention = item.finIntervention ? new Date(item.finIntervention) : null;

            if (debutArret && finIntervention) {
                grouped[machine].totalDowntime += (finIntervention - debutArret) / 60000;
            }
            if (debutArret && debutIntervention) {
                grouped[machine].totalReactionTime += (debutIntervention - debutArret) / 60000;
            }
            if (debutIntervention && finIntervention) {
                grouped[machine].totalRepairTime += (finIntervention - debutIntervention) / 60000;
            }
            if (item.problemeResolu === 'Oui') {
                grouped[machine].resolved++;
            }
        });

        return Object.values(grouped)
            .map(g => ({
                ...g,
                avgDowntime: g.count > 0 ? Math.round(g.totalDowntime / g.count * 10) / 10 : 0,
                avgReactionTime: g.count > 0 ? Math.round(g.totalReactionTime / g.count * 10) / 10 : 0,
                avgRepairTime: g.count > 0 ? Math.round(g.totalRepairTime / g.count * 10) / 10 : 0,
                resolutionRate: g.count > 0 ? Math.round(g.resolved / g.count * 100 * 10) / 10 : 0,
                totalDowntime: Math.round(g.totalDowntime)
            }))
            .sort((a, b) => b.count - a.count);
    }

    groupByCodeArret = (data) => {
        const grouped = {};
        data.forEach(item => {
            const code = item.codeArret?.code || 'Non spécifié';
            const motif = item.codeArret?.motifArret || '';
            if (!grouped[code]) {
                grouped[code] = { code, motif, count: 0, totalDowntime: 0, resolved: 0 };
            }
            grouped[code].count++;
            
            const debutArret = item.debutArret ? new Date(item.debutArret) : null;
            const finIntervention = item.finIntervention ? new Date(item.finIntervention) : null;
            if (debutArret && finIntervention) {
                grouped[code].totalDowntime += (finIntervention - debutArret) / 60000;
            }
            if (item.problemeResolu === 'Oui') {
                grouped[code].resolved++;
            }
        });

        return Object.values(grouped)
            .map(g => ({
                ...g,
                avgDowntime: g.count > 0 ? Math.round(g.totalDowntime / g.count * 10) / 10 : 0,
                resolutionRate: g.count > 0 ? Math.round(g.resolved / g.count * 100 * 10) / 10 : 0,
                totalDowntime: Math.round(g.totalDowntime)
            }))
            .sort((a, b) => b.count - a.count);
    }

    groupByShift = (data) => {
        const grouped = {};
        data.forEach(item => {
            const shift = item.shift || 'Non spécifié';
            if (!grouped[shift]) {
                grouped[shift] = { shift, count: 0, totalDowntime: 0, resolved: 0 };
            }
            grouped[shift].count++;
            
            const debutArret = item.debutArret ? new Date(item.debutArret) : null;
            const finIntervention = item.finIntervention ? new Date(item.finIntervention) : null;
            if (debutArret && finIntervention) {
                grouped[shift].totalDowntime += (finIntervention - debutArret) / 60000;
            }
            if (item.problemeResolu === 'Oui') {
                grouped[shift].resolved++;
            }
        });

        return Object.values(grouped)
            .map(g => ({
                ...g,
                avgDowntime: g.count > 0 ? Math.round(g.totalDowntime / g.count * 10) / 10 : 0,
                resolutionRate: g.count > 0 ? Math.round(g.resolved / g.count * 100 * 10) / 10 : 0,
                totalDowntime: Math.round(g.totalDowntime)
            }))
            .sort((a, b) => a.shift.localeCompare(b.shift));
    }

    groupByDay = (data) => {
        const grouped = {};
        data.forEach(item => {
            const date = item.date || (item.debutArret ? item.debutArret.split('T')[0] : 'Non spécifié');
            if (!grouped[date]) {
                grouped[date] = { date, count: 0, totalDowntime: 0, resolved: 0 };
            }
            grouped[date].count++;
            
            const debutArret = item.debutArret ? new Date(item.debutArret) : null;
            const finIntervention = item.finIntervention ? new Date(item.finIntervention) : null;
            if (debutArret && finIntervention) {
                grouped[date].totalDowntime += (finIntervention - debutArret) / 60000;
            }
            if (item.problemeResolu === 'Oui') {
                grouped[date].resolved++;
            }
        });

        return Object.values(grouped)
            .map(g => ({
                ...g,
                avgDowntime: g.count > 0 ? Math.round(g.totalDowntime / g.count * 10) / 10 : 0,
                totalDowntime: Math.round(g.totalDowntime)
            }))
            .sort((a, b) => a.date.localeCompare(b.date));
    }

    groupByWeek = (data) => {
        const grouped = {};
        data.forEach(item => {
            const dateStr = item.date || (item.debutArret ? item.debutArret.split('T')[0] : null);
            if (!dateStr) return;
            
            const date = new Date(dateStr);
            const week = this.getWeekNumber(date);
            const weekKey = `${date.getFullYear()}-W${week.toString().padStart(2, '0')}`;
            
            if (!grouped[weekKey]) {
                grouped[weekKey] = { week: weekKey, count: 0, totalDowntime: 0, resolved: 0 };
            }
            grouped[weekKey].count++;
            
            const debutArret = item.debutArret ? new Date(item.debutArret) : null;
            const finIntervention = item.finIntervention ? new Date(item.finIntervention) : null;
            if (debutArret && finIntervention) {
                grouped[weekKey].totalDowntime += (finIntervention - debutArret) / 60000;
            }
            if (item.problemeResolu === 'Oui') {
                grouped[weekKey].resolved++;
            }
        });

        return Object.values(grouped)
            .map(g => ({
                ...g,
                avgDowntime: g.count > 0 ? Math.round(g.totalDowntime / g.count * 10) / 10 : 0,
                totalDowntime: Math.round(g.totalDowntime)
            }))
            .sort((a, b) => a.week.localeCompare(b.week));
    }

    getWeekNumber = (date) => {
        const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
        const dayNum = d.getUTCDay() || 7;
        d.setUTCDate(d.getUTCDate() + 4 - dayNum);
        const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
        return Math.ceil((((d - yearStart) / 86400000) + 1) / 7);
    }

    formatMinutes = (minutes) => {
        if (minutes < 60) {
            return `${Math.round(minutes)} min`;
        }
        const hours = Math.floor(minutes / 60);
        const mins = Math.round(minutes % 60);
        return `${hours}h ${mins}min`;
    }

    formatNumber = (num) => {
        if (num === null || num === undefined) return '-';
        return Number(num).toLocaleString('fr-FR', { maximumFractionDigits: 1 });
    }

    toggleSection = (section) => {
        this.setState(prev => ({
            collapsedSections: {
                ...prev.collapsedSections,
                [section]: !prev.collapsedSections[section]
            }
        }));
    }

    downloadCSV = () => {
        const { data } = this.state;
        if (data.length === 0) return;

        const headers = ['ID', 'Machine', 'Date', 'Shift', 'Début Arrêt', 'Début Intervention', 
            'Fin Intervention', 'Code Arrêt', 'Problème Résolu', 'Matricule Émetteur', 
            'Matricule Responsable', 'Date Validation', 'Validé Par'];
        
        const rows = data.map(row => [
            row.id || '',
            row.machine || '',
            row.date || '',
            row.shift || '',
            row.debutArret || '',
            row.debutIntervention || '',
            row.finIntervention || '',
            row.codeArret?.code || '',
            row.problemeResolu || '',
            row.matriculeEmetteur || '',
            row.matriculeResponsable || '',
            row.dateValidation || '',
            row.validerPar || ''
        ]);

        let csv = '\uFEFF' + [headers.join(';'), ...rows.map(r => r.join(';'))].join('\n');
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `kpi-maintenance-${this.state.startDate}-${this.state.endDate}.csv`;
        a.click();
        window.URL.revokeObjectURL(url);
    }

    renderCollapsibleSection = (title, sectionKey, content, bgColor = 'dark', icon = null) => {
        const { collapsedSections } = this.state;
        const isCollapsed = collapsedSections[sectionKey];
        
        return (
            <div className="card mb-3">
                <div 
                    className={`card-header bg-${bgColor} text-white d-flex justify-content-between align-items-center`}
                    style={{ cursor: 'pointer' }}
                    onClick={() => this.toggleSection(sectionKey)}
                >
                    <strong>
                        {icon && <FontAwesomeIcon icon={icon} className="mr-2" />}
                        {title}
                    </strong>
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

    renderBarChart = (data, labelKey, valueKey, maxValue, color = '#007bff') => {
        if (!data || data.length === 0) {
            return <div className="text-center text-muted p-3">Aucune donnée</div>;
        }

        const max = maxValue || Math.max(...data.map(d => d[valueKey]));

        return (
            <div style={{ padding: '10px' }}>
                {data.slice(0, 10).map((item, index) => (
                    <div key={index} style={{ marginBottom: '8px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2px' }}>
                            <span style={{ fontSize: '12px', fontWeight: 'bold' }}>{item[labelKey]}</span>
                            <span style={{ fontSize: '12px' }}>{this.formatNumber(item[valueKey])}</span>
                        </div>
                        <div style={{ 
                            height: '20px', 
                            backgroundColor: '#e9ecef', 
                            borderRadius: '3px',
                            overflow: 'hidden'
                        }}>
                            <div style={{
                                width: `${(item[valueKey] / max) * 100}%`,
                                height: '100%',
                                backgroundColor: color,
                                transition: 'width 0.3s ease'
                            }}></div>
                        </div>
                    </div>
                ))}
            </div>
        );
    }

    renderMiniTable = (data, columns, keyField) => {
        if (!data || data.length === 0) {
            return <div className="text-center text-muted p-3">Aucune donnée</div>;
        }
        return (
            <div className="table-responsive" style={{ maxHeight: '300px', overflowY: 'auto' }}>
                <table className="table table-striped table-bordered table-hover table-sm mb-0">
                    <thead className="thead-light" style={{ position: 'sticky', top: 0 }}>
                        <tr>
                            {columns.map((col, i) => (
                                <th key={i}>{col.header}</th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {data.map((row, index) => (
                            <tr key={row[keyField] || index}>
                                {columns.map((col, i) => (
                                    <td key={i} className={col.className || ''}>
                                        {col.format ? col.format(row[col.field], row) : row[col.field]}
                                    </td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        );
    }

    renderPieChart = (data, labelKey, valueKey, colors) => {
        if (!data || data.length === 0) {
            return <div className="text-center text-muted p-3">Aucune donnée</div>;
        }

        const total = data.reduce((sum, d) => sum + d[valueKey], 0);
        let cumulativePercent = 0;

        const defaultColors = ['#007bff', '#28a745', '#ffc107', '#dc3545', '#17a2b8', '#6c757d', '#6f42c1', '#e83e8c'];

        return (
            <div style={{ display: 'flex', alignItems: 'center', padding: '10px' }}>
                <svg viewBox="0 0 36 36" style={{ width: '120px', height: '120px', marginRight: '20px' }}>
                    {data.map((item, index) => {
                        const percent = (item[valueKey] / total) * 100;
                        const startAngle = cumulativePercent * 3.6;
                        cumulativePercent += percent;
                        const endAngle = cumulativePercent * 3.6;
                        
                        const x1 = 18 + 16 * Math.cos((startAngle - 90) * Math.PI / 180);
                        const y1 = 18 + 16 * Math.sin((startAngle - 90) * Math.PI / 180);
                        const x2 = 18 + 16 * Math.cos((endAngle - 90) * Math.PI / 180);
                        const y2 = 18 + 16 * Math.sin((endAngle - 90) * Math.PI / 180);
                        
                        const largeArc = percent > 50 ? 1 : 0;
                        
                        return (
                            <path
                                key={index}
                                d={`M 18 18 L ${x1} ${y1} A 16 16 0 ${largeArc} 1 ${x2} ${y2} Z`}
                                fill={(colors && colors[index]) || defaultColors[index % defaultColors.length]}
                            />
                        );
                    })}
                </svg>
                <div>
                    {data.map((item, index) => (
                        <div key={index} style={{ display: 'flex', alignItems: 'center', marginBottom: '4px' }}>
                            <div style={{
                                width: '12px',
                                height: '12px',
                                backgroundColor: (colors && colors[index]) || defaultColors[index % defaultColors.length],
                                marginRight: '8px',
                                borderRadius: '2px'
                            }}></div>
                            <span style={{ fontSize: '11px' }}>
                                {item[labelKey]}: {item[valueKey]} ({Math.round((item[valueKey] / total) * 100)}%)
                            </span>
                        </div>
                    ))}
                </div>
            </div>
        );
    }

    render() {
        const { summary, byMachine, byCodeArret, byShift, byDay, byWeek, data,
                loading, startDate, endDate, machine, viewMode } = this.state;

        return (
            <div className="container-fluid" style={{ padding: '20px' }}>
                <div className="d-flex justify-content-between align-items-center mb-4">
                    <h2>
                        <FontAwesomeIcon icon={faWrench} className="mr-2" />
                        KPI Réactivité Maintenance
                    </h2>
                    <div>
                        <div className="btn-group mr-2">
                            <button 
                                className={`btn ${viewMode === 'dashboard' ? 'btn-primary' : 'btn-outline-primary'}`}
                                onClick={() => this.setState({ viewMode: 'dashboard' })}
                            >
                                Dashboard
                            </button>
                            <button 
                                className={`btn ${viewMode === 'table' ? 'btn-primary' : 'btn-outline-primary'}`}
                                onClick={() => this.setState({ viewMode: 'table' })}
                            >
                                Détails
                            </button>
                        </div>
                        <button 
                            className="btn btn-success mr-2"
                            onClick={this.downloadCSV}
                            disabled={loading || data.length === 0}
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
                            <div className="col-md-3">
                                <label>Machine:</label>
                                <input 
                                    type="text" 
                                    className="form-control"
                                    placeholder="Ex: AA1"
                                    value={machine}
                                    onChange={(e) => this.setState({ machine: e.target.value })}
                                />
                            </div>
                            <div className="col-md-3 d-flex align-items-end">
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

                {loading ? (
                    <div className="text-center p-5">
                        <FontAwesomeIcon icon={faSync} spin size="3x" />
                    </div>
                ) : viewMode === 'dashboard' ? (
                    <>
                        {/* Summary Cards */}
                        {summary && (
                            <div className="row mb-4">
                                <div className="col-md-2">
                                    <div className="card bg-primary text-white">
                                        <div className="card-body text-center p-2">
                                            <h6>Total Interventions</h6>
                                            <h3>{summary.totalInterventions}</h3>
                                        </div>
                                    </div>
                                </div>
                                <div className="col-md-2">
                                    <div className="card bg-warning text-dark">
                                        <div className="card-body text-center p-2">
                                            <FontAwesomeIcon icon={faClock} className="mb-1" />
                                            <h6>Temps Réaction Moy.</h6>
                                            <h4>{this.formatMinutes(summary.avgReactionTime)}</h4>
                                        </div>
                                    </div>
                                </div>
                                <div className="col-md-2">
                                    <div className="card bg-info text-white">
                                        <div className="card-body text-center p-2">
                                            <FontAwesomeIcon icon={faWrench} className="mb-1" />
                                            <h6>Temps Réparation Moy.</h6>
                                            <h4>{this.formatMinutes(summary.avgRepairTime)}</h4>
                                        </div>
                                    </div>
                                </div>
                                <div className="col-md-2">
                                    <div className="card bg-danger text-white">
                                        <div className="card-body text-center p-2">
                                            <FontAwesomeIcon icon={faExclamationTriangle} className="mb-1" />
                                            <h6>Temps Arrêt Total</h6>
                                            <h4>{this.formatMinutes(summary.totalDowntime)}</h4>
                                        </div>
                                    </div>
                                </div>
                                <div className="col-md-2">
                                    <div className="card bg-success text-white">
                                        <div className="card-body text-center p-2">
                                            <FontAwesomeIcon icon={faCheckCircle} className="mb-1" />
                                            <h6>Taux Résolution</h6>
                                            <h4>{summary.resolutionRate}%</h4>
                                        </div>
                                    </div>
                                </div>
                                <div className="col-md-2">
                                    <div className="card bg-secondary text-white">
                                        <div className="card-body text-center p-2">
                                            <h6>Validées / En attente</h6>
                                            <h4>{summary.validatedCount} / {summary.pendingValidation}</h4>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* Charts and Tables */}
                        <div className="row">
                            <div className="col-lg-6">
                                {this.renderCollapsibleSection(
                                    'Interventions par Machine',
                                    'byMachine',
                                    <div>
                                        {this.renderBarChart(byMachine, 'machine', 'count', null, '#007bff')}
                                        {this.renderMiniTable(byMachine, [
                                            { field: 'machine', header: 'Machine', format: (v) => <strong>{v}</strong> },
                                            { field: 'count', header: 'Interventions' },
                                            { field: 'avgReactionTime', header: 'Réaction Moy. (min)' },
                                            { field: 'avgRepairTime', header: 'Réparation Moy. (min)' },
                                            { field: 'resolutionRate', header: 'Résolution %' }
                                        ], 'machine')}
                                    </div>,
                                    'primary'
                                )}

                                {this.renderCollapsibleSection(
                                    'Par Code Arrêt',
                                    'byCodeArret',
                                    <div>
                                        {this.renderPieChart(byCodeArret.slice(0, 6), 'code', 'count')}
                                        {this.renderMiniTable(byCodeArret, [
                                            { field: 'code', header: 'Code', format: (v) => <strong>{v}</strong> },
                                            { field: 'motif', header: 'Motif' },
                                            { field: 'count', header: 'Interventions' },
                                            { field: 'avgDowntime', header: 'Arrêt Moy. (min)' },
                                            { field: 'resolutionRate', header: 'Résolution %' }
                                        ], 'code')}
                                    </div>,
                                    'danger'
                                )}
                            </div>

                            <div className="col-lg-6">
                                {this.renderCollapsibleSection(
                                    'Par Shift',
                                    'byShift',
                                    <div>
                                        {this.renderPieChart(byShift, 'shift', 'count', ['#28a745', '#ffc107', '#17a2b8'])}
                                        {this.renderMiniTable(byShift, [
                                            { field: 'shift', header: 'Shift', format: (v) => <strong>Shift {v}</strong> },
                                            { field: 'count', header: 'Interventions' },
                                            { field: 'totalDowntime', header: 'Arrêt Total (min)' },
                                            { field: 'avgDowntime', header: 'Arrêt Moy. (min)' },
                                            { field: 'resolutionRate', header: 'Résolution %' }
                                        ], 'shift')}
                                    </div>,
                                    'success'
                                )}

                                {this.renderCollapsibleSection(
                                    'Évolution par Semaine',
                                    'byWeek',
                                    <div>
                                        {this.renderBarChart(byWeek, 'week', 'count', null, '#6f42c1')}
                                        {this.renderMiniTable(byWeek, [
                                            { field: 'week', header: 'Semaine', format: (v) => <strong>{v}</strong> },
                                            { field: 'count', header: 'Interventions' },
                                            { field: 'totalDowntime', header: 'Arrêt Total (min)' },
                                            { field: 'avgDowntime', header: 'Arrêt Moy. (min)' }
                                        ], 'week')}
                                    </div>,
                                    'dark'
                                )}
                            </div>
                        </div>

                        {/* Daily evolution */}
                        {this.renderCollapsibleSection(
                            'Évolution Quotidienne',
                            'byDay',
                            <div style={{ padding: '10px' }}>
                                <div style={{ overflowX: 'auto' }}>
                                    <div style={{ display: 'flex', alignItems: 'flex-end', height: '150px', minWidth: byDay.length * 30 + 'px' }}>
                                        {byDay.map((day, index) => (
                                            <div key={index} style={{ 
                                                flex: '0 0 25px', 
                                                display: 'flex', 
                                                flexDirection: 'column', 
                                                alignItems: 'center',
                                                marginRight: '5px'
                                            }}>
                                                <div style={{
                                                    height: `${Math.max(5, (day.count / Math.max(...byDay.map(d => d.count))) * 100)}px`,
                                                    width: '20px',
                                                    backgroundColor: '#17a2b8',
                                                    borderRadius: '3px 3px 0 0'
                                                }} title={`${day.date}: ${day.count} interventions`}></div>
                                                <span style={{ fontSize: '8px', transform: 'rotate(-45deg)', transformOrigin: 'center', marginTop: '5px' }}>
                                                    {day.date.substring(5)}
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>,
                            'info'
                        )}
                    </>
                ) : (
                    /* Table View */
                    <div className="table-responsive">
                        <table className="table table-striped table-bordered table-hover table-sm">
                            <thead className="thead-dark">
                                <tr>
                                    <th>ID</th>
                                    <th>Machine</th>
                                    <th>Date</th>
                                    <th>Shift</th>
                                    <th>Début Arrêt</th>
                                    <th>Début Intervention</th>
                                    <th>Fin Intervention</th>
                                    <th>Code Arrêt</th>
                                    <th>Résolu</th>
                                    <th>Émetteur</th>
                                    <th>Responsable</th>
                                    <th>Validé</th>
                                </tr>
                            </thead>
                            <tbody>
                                {data.map((row, index) => (
                                    <tr key={index} 
                                        onClick={() => this.setState({ selectedRow: row })}
                                        style={{ cursor: 'pointer' }}
                                    >
                                        <td>{row.id}</td>
                                        <td><strong>{row.machine}</strong></td>
                                        <td>{row.date}</td>
                                        <td>{row.shift}</td>
                                        <td>{row.debutArret?.replace('T', ' ')}</td>
                                        <td>{row.debutIntervention?.replace('T', ' ')}</td>
                                        <td>{row.finIntervention?.replace('T', ' ')}</td>
                                        <td>{row.codeArret?.code}</td>
                                        <td>
                                            {row.problemeResolu === 'Oui' 
                                                ? <FontAwesomeIcon icon={faCheckCircle} className="text-success" />
                                                : row.problemeResolu === 'Non'
                                                ? <FontAwesomeIcon icon={faTimesCircle} className="text-danger" />
                                                : '-'
                                            }
                                        </td>
                                        <td>{row.matriculeEmetteur}</td>
                                        <td>{row.matriculeResponsable}</td>
                                        <td>{row.dateValidation ? <FontAwesomeIcon icon={faCheckCircle} className="text-success" /> : '-'}</td>
                                    </tr>
                                ))}
                                {data.length === 0 && (
                                    <tr>
                                        <td colSpan="12" className="text-center text-muted">Aucune donnée</td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        );
    }
}

KpiMaintenance.propTypes = {
    security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
    security: state.security
})

export default connect(mapStateToProps, {})(KpiMaintenance);
