import React, { Component } from 'react';
import axios from 'axios';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faExchangeAlt, faSync, faSearch, faCheck, faTimes, faExclamationTriangle, faList, faCheckCircle, faChevronDown, faChevronRight, faChevronLeft } from '@fortawesome/free-solid-svg-icons';
import { Modal } from 'react-bootstrap';

export default class MachineTypeSwap extends Component {
    constructor(props) {
        super(props);
        this.state = {
            // Search/Filter
            partNumberMaterial: '',
            cuttingPlanId: '',
            placementFilter: '',
            projectFilter: '',
            machineTypeFilter: '',
            // Search Results
            placements: [],
            loading: false,
            searchPerformed: false,
            page: 0,
            size: 20,
            totalPages: 0,
            totalPlans: 0,
            totalPlacements: 0,
            // Machine types
            availableMachines: [],
            targetMachine: '',
            // Swap process
            swapping: false,
            swapResults: [],
            showResultsModal: false,
            // Collapsed groups
            collapsedCuttingPlans: {},
            collapsedGroups: {}
        };
    }

    componentDidMount() {
        window.scrollTo(0, 0);
        this.loadMachineTypes();
    }

    loadMachineTypes = () => {
        axios.get('/api/machineType/list')
            .then(response => {
                const machines = (response.data.content || response.data || [])
                    .slice()
                    .sort((a, b) => this.getMachineName(a).localeCompare(this.getMachineName(b)));
                this.setState({ availableMachines: machines });
            })
            .catch(error => {
                console.error('Error loading machine types:', error);
            });
    }

    getMachineName = (machine) => {
        return (machine && (machine.name || machine.value || machine)) || '';
    }

    searchPlacements = (page = 0) => {
        const { partNumberMaterial, cuttingPlanId, placementFilter, projectFilter, machineTypeFilter, size } = this.state;

        if (!partNumberMaterial || !partNumberMaterial.trim()) {
            alert('Veuillez entrer la matière à charger');
            return;
        }

        this.setState({ loading: true, searchPerformed: true, page });

        const params = new URLSearchParams();
        params.append('page', page.toString());
        params.append('size', size.toString());
        params.append('partNumberMaterial', partNumberMaterial.trim());

        if (cuttingPlanId && cuttingPlanId.trim()) {
            params.append('cuttingPlan', cuttingPlanId.trim());
        }
        if (placementFilter && placementFilter.trim()) {
            params.append('placement', placementFilter.trim());
        }
        if (projectFilter && projectFilter.trim()) {
            params.append('project', projectFilter.trim());
        }
        if (machineTypeFilter) {
            params.append('machine', machineTypeFilter);
        }

        axios.get(`/api/cuttingPlanMaterialPlacementData/machineTypeSwap?${params.toString()}`)
            .then(response => {
                let data = response.data.content || [];
                // Sort: cuttingPlan asc, groupPlacement asc, activated desc
                data.sort((a, b) => {
                    if (a.cuttingPlan !== b.cuttingPlan) return a.cuttingPlan - b.cuttingPlan;
                    if ((a.groupPlacement || 0) !== (b.groupPlacement || 0)) return (a.groupPlacement || 0) - (b.groupPlacement || 0);
                    if (a.activated === b.activated) return 0;
                    return a.activated ? -1 : 1;
                });
                this.setState({
                    placements: data,
                    loading: false,
                    page: response.data.number || 0,
                    size: response.data.size || size,
                    totalPages: response.data.totalPages || 0,
                    totalPlans: response.data.totalElements || 0,
                    totalPlacements: response.data.totalPlacements || 0,
                    collapsedCuttingPlans: {},
                    collapsedGroups: {}
                });
            })
            .catch(error => {
                console.error('Error searching placements:', error);
                this.setState({ loading: false });
                alert('Erreur lors de la recherche: ' + (error.response?.data || error.message));
            });
    }

    getFilteredPlacements = () => {
        return this.state.placements;
    }

    getGroupedData = () => {
        const filtered = this.getFilteredPlacements();
        const grouped = {};
        filtered.forEach(p => {
            const cpKey = p.cuttingPlan;
            if (!grouped[cpKey]) grouped[cpKey] = {};
            const gpKey = p.groupPlacement || 'N/A';
            if (!grouped[cpKey][gpKey]) grouped[cpKey][gpKey] = [];
            grouped[cpKey][gpKey].push(p);
        });
        return grouped;
    }

    getDistinctMachinesFromResults = () => {
        const machines = new Set();
        this.getFilteredPlacements().forEach(p => {
            if (p.machine) machines.add(p.machine);
        });
        return Array.from(machines);
    }

    /**
     * Execute the machine swap logic per group:
     * - If group already has target machine with activated=true → yellow (no change)
     * - If group has no row with target machine → red (cannot change)
     * - If group has target machine with activated=false → swap activated flags
     */
    executeChangerMachine = async () => {
        const { targetMachine, placements } = this.state;
        if (!targetMachine) {
            alert('Veuillez sélectionner un type de machine cible');
            return;
        }

        this.setState({ swapping: true });
        const grouped = this.getGroupedData();
        const results = [];

        for (const cpKey of Object.keys(grouped)) {
            for (const gpKey of Object.keys(grouped[cpKey])) {
                const groupRows = grouped[cpKey][gpKey];
                
                const activatedWithTarget = groupRows.find(r => r.machine === targetMachine && r.activated === true);
                if (activatedWithTarget) {
                    results.push({ cuttingPlan: cpKey, groupPlacement: gpKey, status: 'already', message: 'Déjà activé avec cette machine' });
                    continue;
                }

                const inactiveWithTarget = groupRows.find(r => r.machine === targetMachine && r.activated === false);
                if (!inactiveWithTarget) {
                    results.push({ cuttingPlan: cpKey, groupPlacement: gpKey, status: 'notfound', message: 'Machine non trouvée dans ce groupe' });
                    continue;
                }

                const currentlyActivated = groupRows.find(r => r.activated === true);

                try {
                    if (currentlyActivated && currentlyActivated.machine !== targetMachine) {
                        await axios.post('/api/cuttingPlanMaterialPlacementData/toggleActivation', {
                            cuttingPlan: parseInt(cpKey),
                            placement: currentlyActivated.placement
                        });
                    }

                    await axios.post('/api/cuttingPlanMaterialPlacementData/toggleActivation', {
                        cuttingPlan: parseInt(cpKey),
                        placement: inactiveWithTarget.placement
                    });

                    results.push({ cuttingPlan: cpKey, groupPlacement: gpKey, status: 'changed', message: `Changé de ${currentlyActivated ? currentlyActivated.machine : 'N/A'} → ${targetMachine}` });
                } catch (error) {
                    results.push({ cuttingPlan: cpKey, groupPlacement: gpKey, status: 'error', message: error.response?.data || error.message });
                }
            }
        }

        this.setState({ swapping: false, swapResults: results, showResultsModal: true });
        this.searchPlacements(this.state.page);
    }

    executeActivatePlacement = async () => {
        const { placementFilter } = this.state;
        const f = (placementFilter || '').trim().toUpperCase();
        if (!f) {
            alert('Veuillez entrer un filtre de placement (ex: PROG-59)');
            return;
        }

        this.setState({ swapping: true });
        const grouped = this.getGroupedData();
        const results = [];

        for (const cpKey of Object.keys(grouped)) {
            for (const gpKey of Object.keys(grouped[cpKey])) {
                const groupRows = grouped[cpKey][gpKey];
                const matching = groupRows.filter(r => r.placement && r.placement.toUpperCase().includes(f));

                if (matching.length === 0) {
                    continue;
                }
                if (matching.length > 1) {
                    results.push({ cuttingPlan: cpKey, groupPlacement: gpKey, status: 'error', message: `Plusieurs placements matchent "${f}" dans ce groupe` });
                    continue;
                }

                const target = matching[0];
                if (target.activated === true) {
                    results.push({ cuttingPlan: cpKey, groupPlacement: gpKey, status: 'already', message: `${target.placement} déjà activé` });
                    continue;
                }

                const currentlyActivated = groupRows.find(r => r.activated === true);

                try {
                    if (currentlyActivated) {
                        await axios.post('/api/cuttingPlanMaterialPlacementData/toggleActivation', {
                            cuttingPlan: parseInt(cpKey),
                            placement: currentlyActivated.placement
                        });
                    }
                    await axios.post('/api/cuttingPlanMaterialPlacementData/toggleActivation', {
                        cuttingPlan: parseInt(cpKey),
                        placement: target.placement
                    });
                    results.push({ cuttingPlan: cpKey, groupPlacement: gpKey, status: 'changed', message: `Activé ${target.placement}${currentlyActivated ? ` (désactivé ${currentlyActivated.placement})` : ''}` });
                } catch (error) {
                    results.push({ cuttingPlan: cpKey, groupPlacement: gpKey, status: 'error', message: error.response?.data || error.message });
                }
            }
        }

        this.setState({ swapping: false, swapResults: results, showResultsModal: true });
        this.searchPlacements(this.state.page);
    }

    toggleRowActivation = async (row) => {
        try {
            await axios.post('/api/cuttingPlanMaterialPlacementData/toggleActivation', {
                cuttingPlan: parseInt(row.cuttingPlan),
                placement: row.placement
            });
            this.searchPlacements(this.state.page);
        } catch (error) {
            console.error('Error toggling activation:', error);
            alert('Erreur: ' + (error.response?.data || error.message));
        }
    }

    toggleCuttingPlanCollapse = (cpKey) => {
        this.setState(prev => ({
            collapsedCuttingPlans: { ...prev.collapsedCuttingPlans, [cpKey]: !prev.collapsedCuttingPlans[cpKey] }
        }));
    }

    toggleGroupCollapse = (cpKey, gpKey) => {
        const key = `${cpKey}-${gpKey}`;
        this.setState(prev => ({
            collapsedGroups: { ...prev.collapsedGroups, [key]: !prev.collapsedGroups[key] }
        }));
    }

    changePage = (page) => {
        const { totalPages, loading } = this.state;
        if (loading) return;
        const lastPage = Math.max((totalPages || 1) - 1, 0);
        const nextPage = Math.min(Math.max(page, 0), lastPage);
        this.searchPlacements(nextPage);
    }

    changePageSize = (event) => {
        const size = parseInt(event.target.value, 10) || 20;
        this.setState({ size }, () => this.searchPlacements(0));
    }

    renderSearchForm = () => {
        const {
            partNumberMaterial,
            cuttingPlanId,
            placementFilter,
            projectFilter,
            machineTypeFilter,
            availableMachines,
            loading
        } = this.state;
        const machineOptions = availableMachines
            .map(this.getMachineName)
            .filter(Boolean);

        return (
            <div className="card mb-4">
                <div className="card-header bg-primary text-white">
                    <FontAwesomeIcon icon={faSearch} className="mr-2" />
                    Charger la matière
                </div>
                <div className="card-body">
                    <div className="row align-items-end">
                        <div className="col-lg-2 col-md-4">
                            <div className="form-group">
                                <label><strong>Matière *</strong></label>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder="Part Number Material"
                                    value={partNumberMaterial}
                                    onChange={(e) => this.setState({ partNumberMaterial: e.target.value })}
                                    onKeyUp={(e) => e.key === 'Enter' && this.searchPlacements(0)}
                                />
                            </div>
                        </div>
                        <div className="col-lg-2 col-md-4">
                            <div className="form-group">
                                <label>Placement contient</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder="Ex: 1BC"
                                    value={placementFilter}
                                    onChange={(e) => this.setState({ placementFilter: e.target.value })}
                                    onKeyUp={(e) => e.key === 'Enter' && this.searchPlacements(0)}
                                />
                            </div>
                        </div>
                        <div className="col-lg-2 col-md-4">
                            <div className="form-group">
                                <label>Projet contient</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder="Ex: JLR"
                                    value={projectFilter}
                                    onChange={(e) => this.setState({ projectFilter: e.target.value })}
                                    onKeyUp={(e) => e.key === 'Enter' && this.searchPlacements(0)}
                                />
                            </div>
                        </div>
                        <div className="col-lg-2 col-md-4">
                            <div className="form-group">
                                <label>Machine dans le plan</label>
                                <select
                                    className="form-control"
                                    value={machineTypeFilter}
                                    onChange={(e) => this.setState({ machineTypeFilter: e.target.value })}
                                >
                                    <option value="">Toutes</option>
                                    {machineOptions.map((machine) => (
                                        <option key={machine} value={machine}>{machine}</option>
                                    ))}
                                </select>
                            </div>
                        </div>
                        <div className="col-lg-2 col-md-4">
                            <div className="form-group">
                                <label>Cutting Plan ID</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder="Ex: 1234"
                                    value={cuttingPlanId}
                                    onChange={(e) => this.setState({ cuttingPlanId: e.target.value })}
                                    onKeyUp={(e) => e.key === 'Enter' && this.searchPlacements(0)}
                                />
                            </div>
                        </div>
                        <div className="col-lg-2 col-md-4 mb-3">
                            <button
                                className="btn btn-primary btn-block"
                                onClick={() => this.searchPlacements(0)}
                                disabled={loading}
                            >
                                <FontAwesomeIcon icon={loading ? faSync : faSearch} spin={loading} className="mr-2" />
                                Charger
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    renderMachineSelector = () => {
        const { availableMachines, targetMachine, swapping, placements, placementFilter } = this.state;
        const distinctMachines = this.getDistinctMachinesFromResults();
        const machineOptions = availableMachines
            .map(this.getMachineName)
            .filter(Boolean);

        if (placements.length === 0) return null;

        return (
            <div className="card mb-3">
                <div className="card-body d-flex align-items-center flex-wrap">
                    <label className="mr-3 mb-0"><strong>Machine Cible:</strong></label>
                    <select
                        className="form-control mr-3"
                        style={{ maxWidth: 250 }}
                        value={targetMachine}
                        onChange={(e) => this.setState({ targetMachine: e.target.value })}
                    >
                        <option value="">-- Sélectionner --</option>
                        {machineOptions.map((m) => (
                            <option key={m} value={m}>{m}</option>
                        ))}
                    </select>
                    <button
                        className="btn btn-success mr-2"
                        onClick={this.executeChangerMachine}
                        disabled={swapping || !targetMachine}
                    >
                        {swapping ? (
                            <><FontAwesomeIcon icon={faSync} spin className="mr-2" />En cours...</>
                        ) : (
                            <><FontAwesomeIcon icon={faExchangeAlt} className="mr-2" />Changer Machine</>
                        )}
                    </button>
                    <button
                        className="btn btn-info"
                        onClick={this.executeActivatePlacement}
                        disabled={swapping || !(placementFilter || '').trim()}
                        title="Active le placement matchant le filtre, désactive l'autre du groupe"
                    >
                        {swapping ? (
                            <><FontAwesomeIcon icon={faSync} spin className="mr-2" />En cours...</>
                        ) : (
                            <><FontAwesomeIcon icon={faCheck} className="mr-2" />Activer ce placement</>
                        )}
                    </button>
                    <span className="ml-3 text-muted">
                        Machines visibles sur cette page: {distinctMachines.join(', ') || 'Aucune'}
                    </span>
                </div>
            </div>
        );
    }

    renderPaginationControls = () => {
        const { searchPerformed, loading, page, size, totalPages, totalPlans, placements } = this.state;

        if (!searchPerformed || totalPlans === 0) return null;

        const pageCount = totalPages || 1;
        const currentPage = page + 1;
        const canPrevious = page > 0 && !loading;
        const canNext = page < pageCount - 1 && !loading;

        return (
            <div className="d-flex align-items-center justify-content-between flex-wrap mb-2">
                <div className="text-muted mb-2 mb-md-0">
                    {totalPlans} plan(s) correspondant(s) · {placements.length} placement(s) affiché(s) · Page {currentPage} / {pageCount}
                </div>
                <div className="d-flex align-items-center">
                    <label className="mb-0 mr-2 text-muted">Plans/page</label>
                    <select
                        className="form-control form-control-sm mr-2"
                        style={{ width: 80 }}
                        value={size}
                        onChange={this.changePageSize}
                        disabled={loading}
                    >
                        <option value="10">10</option>
                        <option value="20">20</option>
                        <option value="50">50</option>
                        <option value="100">100</option>
                    </select>
                    <div className="btn-group btn-group-sm" role="group" aria-label="Pagination des plans">
                        <button
                            className="btn btn-outline-secondary"
                            onClick={() => this.changePage(0)}
                            disabled={!canPrevious}
                            title="Première page"
                        >
                            <FontAwesomeIcon icon={faChevronLeft} />
                            <FontAwesomeIcon icon={faChevronLeft} />
                        </button>
                        <button
                            className="btn btn-outline-secondary"
                            onClick={() => this.changePage(page - 1)}
                            disabled={!canPrevious}
                            title="Page précédente"
                        >
                            <FontAwesomeIcon icon={faChevronLeft} />
                        </button>
                        <button
                            className="btn btn-outline-secondary"
                            onClick={() => this.changePage(page + 1)}
                            disabled={!canNext}
                            title="Page suivante"
                        >
                            <FontAwesomeIcon icon={faChevronRight} />
                        </button>
                        <button
                            className="btn btn-outline-secondary"
                            onClick={() => this.changePage(pageCount - 1)}
                            disabled={!canNext}
                            title="Dernière page"
                        >
                            <FontAwesomeIcon icon={faChevronRight} />
                            <FontAwesomeIcon icon={faChevronRight} />
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    renderResults = () => {
        const { placements, loading, searchPerformed, collapsedCuttingPlans, collapsedGroups, totalPlans } = this.state;

        if (!searchPerformed) {
            return (
                <div className="card">
                    <div className="card-body text-center text-muted p-5">
                        <FontAwesomeIcon icon={faList} size="3x" className="mb-3" />
                        <p>Entrez la matière pour charger les plans et placements</p>
                    </div>
                </div>
            );
        }

        if (loading) {
            return (
                <div className="card">
                    <div className="card-body text-center p-5">
                        <FontAwesomeIcon icon={faSync} spin size="2x" />
                    </div>
                </div>
            );
        }

        if (placements.length === 0) {
            return (
                <div className="card">
                    <div className="card-body text-center text-muted p-5">
                        Aucun placement trouvé
                    </div>
                </div>
            );
        }

        const grouped = this.getGroupedData();

        return (
            <div className="card">
                <div className="card-header bg-dark text-white d-flex align-items-center justify-content-between flex-wrap">
                    <div>
                        <FontAwesomeIcon icon={faList} className="mr-2" />
                        Résultats ({totalPlans} plans)
                    </div>
                    <div className="small">
                        {placements.length} placement(s) sur cette page
                    </div>
                </div>
                <div className="card-body p-2">
                    {this.renderPaginationControls()}
                    <div style={{ maxHeight: '65vh', overflowY: 'auto' }}>
                    {Object.keys(grouped).sort((a, b) => parseInt(a) - parseInt(b)).map(cpKey => {
                        const cpCollapsed = collapsedCuttingPlans[cpKey];
                        const groupKeys = Object.keys(grouped[cpKey]).sort((a, b) => {
                            if (a === 'N/A') return 1;
                            if (b === 'N/A') return -1;
                            return parseInt(a) - parseInt(b);
                        });
                        const totalInCp = groupKeys.reduce((sum, gk) => sum + grouped[cpKey][gk].length, 0);
                        const firstGroup = groupKeys.length > 0 ? grouped[cpKey][groupKeys[0]] : [];
                        const project = firstGroup.length > 0 ? firstGroup[0].projet : null;

                        return (
                            <div key={cpKey} className="mb-3 border rounded">
                                <div 
                                    className="p-2 bg-light d-flex align-items-center"
                                    style={{ cursor: 'pointer', borderBottom: '2px solid #dee2e6' }}
                                    onClick={() => this.toggleCuttingPlanCollapse(cpKey)}
                                >
                                    <FontAwesomeIcon icon={cpCollapsed ? faChevronRight : faChevronDown} className="mr-2" />
                                    <strong style={{ fontSize: '1.1em' }}>Cutting Plan: {cpKey}</strong>
                                    <span className="badge badge-light ml-2">Projet: {project || '-'}</span>
                                    <span className="badge badge-info ml-2">{totalInCp} placements</span>
                                </div>

                                {!cpCollapsed && groupKeys.map(gpKey => {
                                    const groupCollapsed = collapsedGroups[`${cpKey}-${gpKey}`];
                                    const groupRows = grouped[cpKey][gpKey];

                                    return (
                                        <div key={gpKey} className="ml-3 mb-1">
                                            <div 
                                                className="p-2 d-flex align-items-center"
                                                style={{ cursor: 'pointer', backgroundColor: '#f0f7ff', borderBottom: '1px solid #e0e0e0' }}
                                                onClick={() => this.toggleGroupCollapse(cpKey, gpKey)}
                                            >
                                                <FontAwesomeIcon icon={groupCollapsed ? faChevronRight : faChevronDown} className="mr-2" />
                                                <strong>Group Placement: {gpKey}</strong>
                                                <span className="badge badge-secondary ml-2">{groupRows.length}</span>
                                            </div>

                                            {!groupCollapsed && (
                                                <table className="table table-sm table-bordered mb-0 ml-2" style={{ fontSize: '0.85em' }}>
                                                    <thead className="thead-light">
                                                        <tr>
                                                            <th>Placement</th>
                                                            <th>Machine</th>
                                                            <th>Activé</th>
                                                            <th>Category</th>
                                                            <th>Laize</th>
                                                            <th>Longueur</th>
                                                            <th>Actions</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        {groupRows.map((row, ri) => (
                                                            <tr key={ri} style={{
                                                                backgroundColor: row.activated ? '#d4edda' : '#f8f9fa',
                                                                fontWeight: row.activated ? 'bold' : 'normal'
                                                            }}>
                                                                <td>{row.placement}</td>
                                                                <td>
                                                                    <span className={`badge ${row.machine ? 'badge-primary' : 'badge-secondary'}`}>
                                                                        {row.machine || 'Non défini'}
                                                                    </span>
                                                                </td>
                                                                <td className="text-center">
                                                                    {row.activated 
                                                                        ? <FontAwesomeIcon icon={faCheck} className="text-success" />
                                                                        : <FontAwesomeIcon icon={faTimes} className="text-danger" />
                                                                    }
                                                                </td>
                                                                <td>{row.category || '-'}</td>
                                                                <td>{row.laize || '-'}</td>
                                                                <td>{row.longueur || '-'}</td>
                                                                <td>
                                                                    <button
                                                                        className={`btn btn-sm ${row.activated ? 'btn-outline-danger' : 'btn-outline-success'}`}
                                                                        title={row.activated ? 'Désactiver' : 'Activer'}
                                                                        onClick={() => this.toggleRowActivation(row)}
                                                                    >
                                                                        <FontAwesomeIcon icon={row.activated ? faTimes : faCheck} />
                                                                    </button>
                                                                </td>
                                                            </tr>
                                                        ))}
                                                    </tbody>
                                                </table>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        );
                    })}
                    </div>
                    <div className="mt-2">
                        {this.renderPaginationControls()}
                    </div>
                </div>
            </div>
        );
    }

    renderResultsModal = () => {
        const { showResultsModal, swapResults } = this.state;
        const changedCount = swapResults.filter(r => r.status === 'changed').length;
        const alreadyCount = swapResults.filter(r => r.status === 'already').length;
        const notFoundCount = swapResults.filter(r => r.status === 'notfound').length;
        const errorCount = swapResults.filter(r => r.status === 'error').length;

        return (
            <Modal
                show={showResultsModal}
                onHide={() => this.setState({ showResultsModal: false })}
                size="lg"
            >
                <Modal.Header className={errorCount > 0 || notFoundCount > 0 ? 'bg-warning' : 'bg-success text-white'}>
                    <Modal.Title>
                        <FontAwesomeIcon icon={faCheckCircle} className="mr-2" />
                        Résultats du Changement de Machine
                    </Modal.Title>
                    <button type="button" className="close" onClick={() => this.setState({ showResultsModal: false })}>
                        <FontAwesomeIcon icon={faTimes} />
                    </button>
                </Modal.Header>
                <Modal.Body>
                    <div className="row mb-3">
                        <div className="col-md-3">
                            <div className="alert alert-success py-2 text-center"><strong>Changé:</strong> {changedCount}</div>
                        </div>
                        <div className="col-md-3">
                            <div className="alert alert-warning py-2 text-center"><strong>Déjà OK:</strong> {alreadyCount}</div>
                        </div>
                        <div className="col-md-3">
                            <div className="alert alert-danger py-2 text-center"><strong>Non trouvé:</strong> {notFoundCount}</div>
                        </div>
                        <div className="col-md-3">
                            <div className="alert alert-dark py-2 text-center"><strong>Erreur:</strong> {errorCount}</div>
                        </div>
                    </div>
                    <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
                        <table className="table table-sm table-bordered">
                            <thead className="thead-light">
                                <tr>
                                    <th>Cutting Plan</th>
                                    <th>Group Placement</th>
                                    <th>Status</th>
                                    <th>Message</th>
                                </tr>
                            </thead>
                            <tbody>
                                {swapResults.map((r, index) => (
                                    <tr key={index} style={{
                                        backgroundColor: r.status === 'changed' ? '#d4edda' :
                                            r.status === 'already' ? '#fff3cd' :
                                            r.status === 'notfound' ? '#f8d7da' : '#e2e3e5'
                                    }}>
                                        <td>{r.cuttingPlan}</td>
                                        <td>{r.groupPlacement}</td>
                                        <td>
                                            {r.status === 'changed' && <FontAwesomeIcon icon={faCheck} className="text-success" />}
                                            {r.status === 'already' && <FontAwesomeIcon icon={faExclamationTriangle} className="text-warning" />}
                                            {r.status === 'notfound' && <FontAwesomeIcon icon={faTimes} className="text-danger" />}
                                            {r.status === 'error' && <FontAwesomeIcon icon={faTimes} className="text-dark" />}
                                        </td>
                                        <td>{r.message}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <button className="btn btn-primary" onClick={() => this.setState({ showResultsModal: false })}>
                        Fermer
                    </button>
                </Modal.Footer>
            </Modal>
        );
    }

    render() {
        return (
            <div style={{ padding: '20px' }}>
                <h2 className="mb-4">
                    <FontAwesomeIcon icon={faExchangeAlt} className="mr-2" />
                    Changement de Type Machine
                </h2>

                {this.renderSearchForm()}
                {this.renderMachineSelector()}
                {this.renderResults()}
                {this.renderResultsModal()}
            </div>
        );
    }
}
