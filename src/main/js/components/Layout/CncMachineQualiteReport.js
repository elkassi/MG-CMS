import React, { Component } from 'react';
import axios from 'axios';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSpinner, faSearch, faIndustry, faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';

// Report columns. Each is sortable (click header) and filterable (header input).
//   text():    value for the per-column filter and default string sort.
//   sortVal(): optional comparable for correct numeric sort.
//   render():  optional JSX for display (defaults to text()).
const COLUMNS = [
    { key: 'createdAt', label: 'Date contrôle', text: r => r.createdAt || '', cls: 'small' },
    { key: 'machine', label: 'Machine', text: r => r.machine || '', render: r => r.machine || '-' },
    { key: 'stage', label: 'Étape', text: r => r.stage || '' },
    { key: 'result', label: 'Résultat', text: r => r.result || '',
      render: r => r.result ? <span className={`badge badge-${r.result === 'OK' ? 'success' : 'danger'}`}>{r.result}</span> : '-' },
    { key: 'quantite', label: 'Qté', text: r => String(r.quantite ?? ''), sortVal: r => Number(r.quantite) || 0 },
    { key: 'codeDefaut', label: 'Défaut', text: r => r.codeDefaut || '', render: r => r.codeDefaut || '-' },
    { key: 'codeScrap', label: 'Scrap', text: r => r.codeScrap || '', render: r => r.codeScrap || '-' },
    { key: 'matricule', label: 'Matricule', text: r => r.matricule || '', render: r => r.matricule || '-' },
    { key: 'boxId', label: 'Box ID', text: r => r.boxId || '', render: r => <strong>{r.boxId}</strong> },
    { key: 'partNumberImp', label: 'PN', text: r => r.partNumberImp || '' },
    { key: 'boxQuantite', label: 'Qté Boîte', text: r => r.boxQuantite || '' },
    { key: 'code1Imp', label: 'Cuir', text: r => r.code1Imp || '', render: r => r.code1Imp || '-' },
    { key: 'operator', label: 'Opérateur', text: r => r.operator || '', render: r => r.operator || '-' },
    { key: 'programNumber', label: 'Programme', text: r => r.programNumber || '', render: r => r.programNumber || '-' },
    { key: 'panelNumber', label: 'Panel', text: r => r.panelNumber || '', render: r => r.panelNumber || '-' },
    { key: 'pattern', label: 'Pattern', text: r => r.pattern || '', render: r => r.pattern || '-' },
    { key: 'numBonScrap', label: 'Bon Scrap', text: r => r.numBonScrap || '', render: r => r.numBonScrap || '-' },
    { key: 'scrapStatus', label: 'Statut Scrap', text: r => r.scrapStatus || '', render: r => r.scrapStatus || '-' },
    { key: 'qualiteStatus', label: 'Statut Qualité', text: r => r.qualiteStatus || '', render: r => r.qualiteStatus || '-' },
    { key: 'userQualite', label: 'User Qualité', text: r => r.userQualite || '', render: r => r.userQualite || '-' },
];

class CncMachineQualiteReport extends Component {
    constructor(props) {
        super(props);
        this.state = {
            // Filters
            machines: [],
            machineId: '',
            date: new Date().toISOString().slice(0, 10),
            shift: '',

            // Report
            rows: [],
            summary: null,
            loading: false,
            message: null,

            // Table header filters + click-to-sort
            columnFilters: {},
            sortKey: '',
            sortDir: 'asc',
        };
    }

    componentDidMount() {
        axios.get('/api/cncPs/machines')
            .then(res => this.setState({ machines: res.data || [] }))
            .catch(() => {});
    }

    generate = () => {
        const { machineId, date, shift } = this.state;
        if (!date) {
            this.setState({ message: 'La date est obligatoire' });
            return;
        }
        this.setState({ loading: true, message: null });
        const params = { date };
        if (machineId) params.machineId = machineId;
        if (shift) params.shift = shift;
        axios.get('/api/cncPs/machineQualiteReport', { params })
            .then(res => this.setState({
                rows: res.data.rows || [],
                summary: res.data,
                loading: false,
            }))
            .catch(err => this.setState({ loading: false, message: (err.response && err.response.data) || 'Erreur lors de la génération du rapport' }));
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

    getVisibleRows = () => {
        const { rows, columnFilters, sortKey, sortDir } = this.state;
        let filtered = rows;

        COLUMNS.forEach(col => {
            const q = (columnFilters[col.key] || '').trim().toLowerCase();
            if (q) filtered = filtered.filter(r => col.text(r).toLowerCase().includes(q));
        });

        if (sortKey) {
            const col = COLUMNS.find(c => c.key === sortKey);
            if (col) {
                const dir = sortDir === 'asc' ? 1 : -1;
                const val = r => (col.sortVal ? col.sortVal(r) : col.text(r));
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
        const { machines, machineId, date, shift, summary, loading, message, columnFilters, sortKey, sortDir } = this.state;
        const rows = this.getVisibleRows();

        return (
            <div className="container-fluid mt-3">
                <h2><FontAwesomeIcon icon={faIndustry} /> Rapport Qualité par Machine</h2>

                {message && (
                    <div className="alert alert-danger">
                        <FontAwesomeIcon icon={faExclamationTriangle} className="mr-2" />{message}
                    </div>
                )}

                {/* Filters */}
                <div className="card mb-3">
                    <div className="card-body py-2">
                        <div className="row align-items-end">
                            <div className="col-md-3 form-group mb-1">
                                <label className="small">Machine (optionnel)</label>
                                <select className="form-control form-control-sm" value={machineId}
                                        onChange={e => this.setState({ machineId: e.target.value })}>
                                    <option value="">Toutes les machines</option>
                                    {machines.map(m => (
                                        <option key={m.id} value={m.id}>{m.name}{m.type ? ` (${m.type})` : ''}</option>
                                    ))}
                                </select>
                            </div>
                            <div className="col-md-3 form-group mb-1">
                                <label className="small">Date *</label>
                                <input type="date" className="form-control form-control-sm" value={date}
                                       onChange={e => this.setState({ date: e.target.value })} />
                            </div>
                            <div className="col-md-3 form-group mb-1">
                                <label className="small">Shift (optionnel)</label>
                                <select className="form-control form-control-sm" value={shift}
                                        onChange={e => this.setState({ shift: e.target.value })}>
                                    <option value="">Toute la journée</option>
                                    <option value="1">Shift 1 (21:50 → 05:50)</option>
                                    <option value="2">Shift 2 (05:50 → 13:50)</option>
                                    <option value="3">Shift 3 (13:50 → 21:50)</option>
                                </select>
                            </div>
                            <div className="col-md-3 form-group mb-1">
                                <button className="btn btn-primary btn-sm w-100" onClick={this.generate} disabled={loading}>
                                    <FontAwesomeIcon icon={loading ? faSpinner : faSearch} spin={loading} /> Générer le rapport
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                {summary && !loading && (
                    <div className="text-muted small mb-2">
                        Période : {summary.shiftStart} → {summary.shiftEnd} — {summary.count} contrôle(s)
                        {' '}| <span className="text-success">OK : {summary.totalOK}</span>
                        {' '}| <span className="text-danger">NOK : {summary.totalNOK}</span>
                    </div>
                )}

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
                                </tr>
                            </thead>
                            <tbody>
                                {rows.length === 0 ? (
                                    <tr><td colSpan={COLUMNS.length} className="text-center text-muted p-4">
                                        {summary ? 'Aucun contrôle trouvé' : 'Choisissez une date et générez le rapport'}
                                    </td></tr>
                                ) : rows.map(r => (
                                    <tr key={r.controlId}>
                                        {COLUMNS.map(col => (
                                            <td key={col.key} className={col.cls}>
                                                {col.render ? col.render(r) : col.text(r)}
                                            </td>
                                        ))}
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                <div className="text-muted small mt-2">{rows.length} ligne(s)</div>
            </div>
        );
    }
}

export default CncMachineQualiteReport;
