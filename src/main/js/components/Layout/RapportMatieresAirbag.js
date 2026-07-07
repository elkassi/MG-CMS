import React, { Component } from 'react';
import axios from 'axios';
import moment from 'moment';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFileExcel, faMagnifyingGlass, faEraser } from '@fortawesome/free-solid-svg-icons';
import ExcelJS from 'exceljs';
import { saveAs } from 'file-saver';
import SortIcon from '../utils/SortIcon';
import '../styles/RapportMatieresAirbag.scss';

export default class RapportMatieresAirbag extends Component {

    constructor() {
        super();
        const currentYear = moment().year();
        this.state = {
            list: [],
            loading: false,
            startDate: `${currentYear}-01-01`,
            endDate: `${currentYear + 1}-01-01`,
            partNumberMaterial: '',
            filter: {},
            sortProp: 'createdAt',
            sortDirec: 'desc',
        };
    }

    componentDidMount() {
        this.search();
    }

    search = async () => {
        this.setState({ list: [], loading: true });
        try {
            let params = new URLSearchParams();
            if (this.state.startDate) {
                params.append('startDate', this.state.startDate);
            }
            if (this.state.endDate) {
                params.append('endDate', this.state.endDate);
            }
            if (this.state.partNumberMaterial) {
                params.append('partNumberMaterial', this.state.partNumberMaterial);
            }
            const res = await axios.get(`/api/query/rapportMatieresAirbag?${params.toString()}`);
            this.setState({ list: res.data, loading: false });
        } catch (err) {
            console.error(err);
            this.setState({ list: [], loading: false });
        }
    };

    resetFilters = () => {
        const currentYear = moment().year();
        this.setState({
            startDate: `${currentYear}-01-01`,
            endDate: `${currentYear + 1}-01-01`,
            partNumberMaterial: '',
            filter: {},
        });
    };

    exportExcel = () => {
        if (!this.state.list || this.state.list.length === 0) return;

        const data = this.getFilteredAndSortedList().map(e => ({
            'Demande ID': e.demandeId,
            'Sequence': e.sequence,
            'Part Number Material': e.partNumberMaterial,
            'N° Lot FRS': e.nlotfrs,
            'Type Défaut': e.typeDefaut,
            'Défaut Code': e.defautCode,
            'Site': e.siteNom,
            'Projet': e.projetNom,
            'Description': e.description,
            'Init Quantity': e.initQuantity,
            'Label ID': e.labelId,
            'Lot Nr': e.lotNr,
            'Quantity': e.quantity,
            'Ref Tissu': e.reftissu,
            'Table Name': e.tableName,
            'Quantité PLS': e.quantitePLS,
            'Prix Total': e.prixTotal,
            'Prix Unit': e.prixUnit,
            'Created At': e.createdAt,
        }));

        const workbook = new ExcelJS.Workbook();
        const worksheet = workbook.addWorksheet('Rapport Matières Airbag');

        const xlsHeader = Object.keys(data[0]);
        const headerRow = worksheet.addRow(xlsHeader);

        headerRow.eachCell((cell) => {
            cell.fill = {
                type: 'pattern',
                pattern: 'solid',
                fgColor: { argb: 'FFBF3030' },
            };
            cell.font = {
                bold: true,
                color: { argb: 'FFFFFFFF' },
            };
            cell.alignment = { horizontal: 'center' };
        });

        data.forEach(item => {
            worksheet.addRow(Object.values(item));
        });

        worksheet.columns = xlsHeader.map((header) => {
            const colLength = Math.max(
                header.length,
                ...data.map(item => (item[header]?.toString().length || 10))
            );
            return { width: colLength + 2 };
        });

        workbook.xlsx.writeBuffer().then((buffer) => {
            const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
            saveAs(blob, `Rapport_Matieres_Airbag_${moment().format('YYYY-MM-DD')}.xlsx`);
        });
    };

    sortChanged = (field) => {
        const sortProp = field;
        const propChanged = this.state.sortProp !== sortProp;
        const sortDirec = propChanged ? 'asc' : this.state.sortDirec === 'asc' ? 'desc' : 'asc';
        this.setState({ sortProp, sortDirec });
    };

    getFilteredAndSortedList = () => {
        const { list, filter, sortProp, sortDirec } = this.state;
        if (!list) return [];

        return list
            .filter(elem => {
                const f = filter;
                return (
                    (!f.demandeId || elem.demandeId?.toString().includes(f.demandeId)) &&
                    (!f.sequence || elem.sequence?.toUpperCase().includes(f.sequence.toUpperCase())) &&
                    (!f.partNumberMaterial || elem.partNumberMaterial?.toUpperCase().includes(f.partNumberMaterial.toUpperCase())) &&
                    (!f.nlotfrs || elem.nlotfrs?.toUpperCase().includes(f.nlotfrs.toUpperCase())) &&
                    (!f.typeDefaut || elem.typeDefaut?.toUpperCase().includes(f.typeDefaut.toUpperCase())) &&
                    (!f.defautCode || elem.defautCode?.toUpperCase().includes(f.defautCode.toUpperCase())) &&
                    (!f.siteNom || elem.siteNom?.toUpperCase().includes(f.siteNom.toUpperCase())) &&
                    (!f.projetNom || elem.projetNom?.toUpperCase().includes(f.projetNom.toUpperCase())) &&
                    (!f.description || elem.description?.toUpperCase().includes(f.description.toUpperCase())) &&
                    (!f.initQuantity || elem.initQuantity?.toString().includes(f.initQuantity)) &&
                    (!f.labelId || elem.labelId?.toUpperCase().includes(f.labelId.toUpperCase())) &&
                    (!f.lotNr || elem.lotNr?.toUpperCase().includes(f.lotNr.toUpperCase())) &&
                    (!f.quantity || elem.quantity?.toString().includes(f.quantity)) &&
                    (!f.reftissu || elem.reftissu?.toUpperCase().includes(f.reftissu.toUpperCase())) &&
                    (!f.tableName || elem.tableName?.toUpperCase().includes(f.tableName.toUpperCase())) &&
                    (!f.quantitePLS || elem.quantitePLS?.toString().includes(f.quantitePLS)) &&
                    (!f.prixTotal || elem.prixTotal?.toString().includes(f.prixTotal)) &&
                    (!f.prixUnit || elem.prixUnit?.toString().includes(f.prixUnit)) &&
                    (!f.createdAt || elem.createdAt?.toUpperCase().includes(f.createdAt.toUpperCase()))
                );
            })
            .sort((a, b) => {
                if (sortProp && sortDirec) {
                    const aVal = a[sortProp];
                    const bVal = b[sortProp];
                    
                    // Handle null/undefined
                    if (aVal == null && bVal == null) return 0;
                    if (aVal == null) return sortDirec === 'asc' ? -1 : 1;
                    if (bVal == null) return sortDirec === 'asc' ? 1 : -1;

                    // String comparison for text fields
                    if (typeof aVal === 'string' && typeof bVal === 'string') {
                        return sortDirec === 'asc' 
                            ? aVal.localeCompare(bVal) 
                            : bVal.localeCompare(aVal);
                    }
                    
                    // Numeric comparison
                    return sortDirec === 'asc' ? aVal - bVal : bVal - aVal;
                }
                return 0;
            });
    };

    renderHeader = () => {
        return (
            <div className="rapport-airbag-filters d-flex align-items-center mb-3 mx-2 flex-wrap gap-2">
                <div className="filter-group">
                    <label>Date Début</label>
                    <input
                        type="date"
                        className="form-control"
                        value={this.state.startDate}
                        onChange={(e) => this.setState({ startDate: e.target.value })}
                    />
                </div>
                <div className="filter-group">
                    <label>Date Fin</label>
                    <input
                        type="date"
                        className="form-control"
                        value={this.state.endDate}
                        onChange={(e) => this.setState({ endDate: e.target.value })}
                    />
                </div>
                <div className="filter-group">
                    <label>Part Number Material</label>
                    <input
                        type="text"
                        className="form-control"
                        placeholder="Part Number Material"
                        value={this.state.partNumberMaterial}
                        onChange={(e) => this.setState({ partNumberMaterial: e.target.value })}
                    />
                </div>
                <div className="filter-buttons">
                    <button className="btn btn-danger" onClick={this.search} title="Rechercher">
                        <FontAwesomeIcon icon={faMagnifyingGlass} />
                    </button>
                    <button className="btn btn-secondary" onClick={this.resetFilters} title="Réinitialiser">
                        <FontAwesomeIcon icon={faEraser} />
                    </button>
                    <button 
                        className="btn btn-success" 
                        onClick={this.exportExcel} 
                        title="Exporter Excel"
                        disabled={!this.state.list || this.state.list.length === 0}
                    >
                        <FontAwesomeIcon icon={faFileExcel} />
                    </button>
                </div>
            </div>
        );
    };

    renderTable = () => {
        const columns = [
            { key: 'demandeId', label: 'Demande ID', type: 'number' },
            { key: 'sequence', label: 'Sequence', type: 'text' },
            { key: 'partNumberMaterial', label: 'Part Number Material', type: 'text' },
            { key: 'nlotfrs', label: 'N° Lot FRS', type: 'text' },
            { key: 'typeDefaut', label: 'Type Défaut', type: 'text' },
            { key: 'defautCode', label: 'Défaut Code', type: 'text' },
            { key: 'siteNom', label: 'Site', type: 'text' },
            { key: 'projetNom', label: 'Projet', type: 'text' },
            { key: 'description', label: 'Description', type: 'text' },
            { key: 'initQuantity', label: 'Init Quantity', type: 'number' },
            { key: 'labelId', label: 'Label ID', type: 'text' },
            { key: 'lotNr', label: 'Lot Nr', type: 'text' },
            { key: 'quantity', label: 'Quantity', type: 'number' },
            { key: 'reftissu', label: 'Ref Tissu', type: 'text' },
            { key: 'tableName', label: 'Table Name', type: 'text' },
            { key: 'quantitePLS', label: 'Quantité PLS', type: 'number' },
            { key: 'prixTotal', label: 'Prix Total', type: 'number' },
            { key: 'prixUnit', label: 'Prix Unit', type: 'number' },
            { key: 'createdAt', label: 'Created At', type: 'text' },
        ];

        const filteredList = this.getFilteredAndSortedList();

        return (
            <div className="table-responsive rapport-airbag-table">
                <table className="table table-bordered table-hover m-0">
                    <thead className="sticky-header">
                        <tr className="header-row">
                            {columns.map(col => (
                                <th 
                                    key={col.key} 
                                    onClick={() => this.sortChanged(col.key)}
                                    className="sortable-header"
                                >
                                    {col.label}
                                    <SortIcon 
                                        currentSort={col.key} 
                                        sortProp={this.state.sortProp} 
                                        sortDirection={this.state.sortDirec} 
                                    />
                                </th>
                            ))}
                        </tr>
                        <tr className="filter-row">
                            {columns.map(col => (
                                <th key={`filter-${col.key}`} className="filter-cell">
                                    <input
                                        type={col.type === 'number' ? 'text' : 'text'}
                                        placeholder="..."
                                        value={this.state.filter[col.key] || ''}
                                        onChange={(e) => 
                                            this.setState({ 
                                                filter: { 
                                                    ...this.state.filter, 
                                                    [col.key]: e.target.value || null 
                                                } 
                                            })
                                        }
                                    />
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {this.state.loading ? (
                            <tr>
                                <td colSpan={columns.length} className="text-center py-4">
                                    <div className="spinner-border text-danger" role="status">
                                        <span className="visually-hidden">Chargement...</span>
                                    </div>
                                    <p className="mt-2 mb-0">Chargement en cours...</p>
                                </td>
                            </tr>
                        ) : filteredList.length === 0 ? (
                            <tr>
                                <td colSpan={columns.length} className="text-center py-4">
                                    Aucune donnée disponible
                                </td>
                            </tr>
                        ) : (
                            filteredList.map((item, idx) => (
                                <tr key={`row-${idx}`}>
                                    <td>{item.demandeId}</td>
                                    <td>{item.sequence}</td>
                                    <td>{item.partNumberMaterial}</td>
                                    <td>{item.nlotfrs}</td>
                                    <td>{item.typeDefaut}</td>
                                    <td>{item.defautCode}</td>
                                    <td>{item.siteNom}</td>
                                    <td>{item.projetNom}</td>
                                    <td className="description-cell">{item.description}</td>
                                    <td className="text-end">{item.initQuantity}</td>
                                    <td>{item.labelId}</td>
                                    <td>{item.lotNr}</td>
                                    <td className="text-end">{item.quantity}</td>
                                    <td>{item.reftissu}</td>
                                    <td>{item.tableName}</td>
                                    <td className="text-end">{item.quantitePLS}</td>
                                    <td className="text-end">{item.prixTotal}</td>
                                    <td className="text-end">{item.prixUnit}</td>
                                    <td className="nowrap">{item.createdAt}</td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        );
    };

    render() {
        return (
            <div className="rapport-matieres-airbag">
                <h2 className="page-title">Rapport des matières Airbag</h2>
                {this.renderHeader()}
                <div className="results-count mb-2 mx-2">
                    {this.state.list && this.state.list.length > 0 && (
                        <span className="badge bg-secondary">
                            {this.getFilteredAndSortedList().length} / {this.state.list.length} résultats
                        </span>
                    )}
                </div>
                {this.renderTable()}
            </div>
        );
    }
}
