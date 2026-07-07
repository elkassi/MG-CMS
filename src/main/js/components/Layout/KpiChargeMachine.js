import React, { Component } from 'react';
import { connect } from 'react-redux';
import axios from 'axios';
import { Bar } from 'react-chartjs-2';
import { Chart, BarElement, BarController, CategoryScale, LinearScale, Title, Tooltip, Legend } from 'chart.js';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSearch, faSync, faChartBar, faTable, faInfoCircle, faSpinner } from '@fortawesome/free-solid-svg-icons';

Chart.register(BarElement, BarController, CategoryScale, LinearScale, Title, Tooltip, Legend);

// Machine type colors (same as PlanDeCharge)
const MACHINE_TYPE_COLORS = {
    'Lectra': '#a9d08e',
    'Lectra IP6': '#92d050',
    'Gerber': '#e2efda',
    'LASER-DXF': '#ffc000',
    'DIE': '#0077ff'
};

const SHIFT_DURATION_MINUTES = 460;

// Local-timezone yyyy-MM-dd. toISOString() is UTC and flags the WRONG current
// shift during the first hour(s) of the night shift (Morocco is UTC+1 in DST).
const localDateStr = (d) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

// chart.js v3 inline plugin: dashed red reference line at 100%. The previous
// `annotation` options block needed chartjs-plugin-annotation, which is not
// installed — the line silently never rendered.
const LINE_100_PLUGIN = {
    id: 'line100',
    afterDatasetsDraw(chart) {
        const { ctx, chartArea, scales } = chart;
        if (!scales.y || !chartArea) return;
        const y = scales.y.getPixelForValue(100);
        if (y < chartArea.top || y > chartArea.bottom) return;
        ctx.save();
        ctx.strokeStyle = '#e74c3c';
        ctx.setLineDash([6, 4]);
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(chartArea.left, y);
        ctx.lineTo(chartArea.right, y);
        ctx.stroke();
        ctx.restore();
    }
};

class KpiChargeMachine extends Component {
    constructor(props) {
        super(props);

        const today = new Date();
        const startDate = new Date(today);
        startDate.setDate(startDate.getDate() - 7);
        const endDate = new Date(today);
        endDate.setDate(endDate.getDate() + 3);

        this.state = {
            dateDebut: localDateStr(startDate),
            dateFin: localDateStr(endDate),
            loading: false,
            error: null,
            machinesByZone: {},
            statusGrid: {},
            aggregatedCuttingTime: {},
            aggregatedCuttingTimeWithStatus: {},
            capaciteInstalleeData: [],
            currentShift: null,
            currentDate: localDateStr(today),
            viewMode: 'chart', // 'chart' or 'table'
            selectedMachineType: 'ALL' // 'ALL' or specific type
        };
    }

    componentDidMount() {
        this.loadData();
    }

    loadData = async () => {
        const { dateDebut, dateFin } = this.state;
        if (!dateDebut || !dateFin) {
            this.setState({ error: 'Veuillez sélectionner les dates.' });
            return;
        }

        this.setState({ loading: true, error: null });

        try {
            const [machinesRes, currentShiftRes, statusRes, cuttingTimeRes, cuttingTimeStatusRes, capaciteRes] = await Promise.all([
                axios.get('/api/planDeCharge/machines'),
                axios.get('/api/planDeCharge/currentShift'),
                axios.get('/api/planDeCharge/search', { params: { startDate: dateDebut, endDate: dateFin } }),
                axios.get('/api/planDeCharge/aggregatedCuttingTime', { params: { startDate: dateDebut, endDate: dateFin } }),
                axios.get('/api/planDeCharge/aggregatedCuttingTimeWithStatus', { params: { startDate: dateDebut, endDate: dateFin } }),
                axios.get('/api/capaciteInstallee/effectiveByDateRange', { params: { startDate: dateDebut, endDate: dateFin } }).catch(() => ({ data: [] }))
            ]);

            this.setState({
                machinesByZone: machinesRes.data || {},
                currentShift: currentShiftRes.data,
                statusGrid: statusRes.data.statusGrid || {},
                aggregatedCuttingTime: cuttingTimeRes.data || {},
                aggregatedCuttingTimeWithStatus: cuttingTimeStatusRes.data || {},
                capaciteInstalleeData: capaciteRes.data || [],
                loading: false
            });
        } catch (error) {
            console.error('Error loading KPI data:', error);
            this.setState({
                error: 'Erreur lors du chargement: ' + (error.response?.data?.message || error.message),
                loading: false
            });
        }
    };

    /**
     * Get all dates in range.
     */
    getDates = () => {
        const { dateDebut, dateFin } = this.state;
        const dates = [];
        const start = new Date(dateDebut);
        const end = new Date(dateFin);
        for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
            dates.push(d.toISOString().split('T')[0]);
        }
        return dates;
    };

    /**
     * Get unique machine types.
     */
    getMachineTypes = () => {
        const { machinesByZone } = this.state;
        const types = new Set();
        Object.values(machinesByZone).forEach(machines => {
            machines.forEach(m => {
                if (m.machineType?.name) types.add(m.machineType.name);
            });
        });
        return Array.from(types).sort();
    };

    /**
     * Get machines of a specific type.
     */
    getMachinesOfType = (machineType) => {
        const { machinesByZone } = this.state;
        const machineNames = [];
        Object.values(machinesByZone).forEach(machines => {
            machines.forEach(m => {
                if (machineType === 'ALL' || m.machineType?.name === machineType) {
                    machineNames.push(m);
                }
            });
        });
        return machineNames;
    };

    /**
     * Map a machine type to its CapaciteInstallee groupe (Coupe / Laser).
     */
    getGroupeForMachineType = (type) => {
        const coupeTypes = ['Lectra', 'Lectra IP6', 'Gerber'];
        if (coupeTypes.includes(type)) return 'Coupe';
        if (type === 'LASER-DXF') return 'Laser';
        return type;
    };

    /**
     * Effective CapaciteInstallee entry for a date/shift/groupe (or null).
     */
    getCapaciteInstalleeEntry = (date, shift, groupe) => {
        const { capaciteInstalleeData } = this.state;
        if (!capaciteInstalleeData || capaciteInstalleeData.length === 0) return null;
        return capaciteInstalleeData.find(e =>
            e.dateProduction === date && e.shiftNumber === shift && e.groupe === groupe
        ) || null;
    };

    /**
     * Configured shift minutes per machine from CapaciteInstallee (temps total par
     * machine), falling back to SHIFT_DURATION_MINUTES when none is configured.
     */
    getConfiguredShiftMinutes = (date, shift, groupe) => {
        const entry = this.getCapaciteInstalleeEntry(date, shift, groupe);
        const minutes = Number(entry?.tempsTotalParMachine);
        return Number.isFinite(minutes) && minutes > 0 ? minutes : SHIFT_DURATION_MINUTES;
    };

    /**
     * Compute charge data for a given date/shift/machineType.
     * Returns: { retard, charge, sum, capacite, totalMachines }
     */
    computeShiftData = (date, shift, machineType) => {
        const { aggregatedCuttingTime, aggregatedCuttingTimeWithStatus, statusGrid } = this.state;
        const machines = this.getMachinesOfType(machineType);

        let machinesAvailable = 0;
        let totalMachines = machines.length;
        const machineNames = machines.map(m => m.nom);

        // Available machines (status M/MS/MD/R — intentionally broader than the
        // Plan de Charge grid, which counts M only). Available time uses each
        // available machine's configured shift minutes from CapaciteInstallee
        // (the same effective-capacity source the backend uses), instead of a
        // hardcoded 460.
        let availableTime = 0;
        machines.forEach(m => {
            const status = statusGrid[m.nom]?.[date]?.[shift] || 'M';
            if (status === 'M' || status === 'MS' || status === 'MD' || status === 'R') {
                machinesAvailable++;
                const groupe = this.getGroupeForMachineType(m.machineType?.name);
                availableTime += this.getConfiguredShiftMinutes(date, shift, groupe);
            }
        });

        // Sum cutting times
        let totalCuttingTime = 0;
        machineNames.forEach(name => {
            const machineData = aggregatedCuttingTime[name];
            if (machineData && machineData[date]) {
                const shiftTime = machineData[date][shift];
                if (shiftTime > 0) totalCuttingTime += shiftTime;
            }
        });

        const charge = availableTime > 0 ? (totalCuttingTime / availableTime) * 100 : 0;

        // Retard from previous shift
        let retard = 0;
        let prevShift = shift - 1;
        let prevDate = date;
        if (shift === 1) {
            prevShift = 3;
            const d = new Date(date);
            d.setDate(d.getDate() - 1);
            prevDate = d.toISOString().split('T')[0];
        }

        if (aggregatedCuttingTimeWithStatus && Object.keys(aggregatedCuttingTimeWithStatus).length > 0) {
            machineNames.forEach(name => {
                const machineData = aggregatedCuttingTimeWithStatus[name];
                if (machineData && machineData[prevDate] && machineData[prevDate][prevShift]) {
                    const statusData = machineData[prevDate][prevShift];
                    if (statusData.notCut && statusData.notCut > 0) {
                        retard += statusData.notCut;
                    }
                }
            });
        }

        const retardPercentage = availableTime > 0 ? (retard / availableTime * 100) : 0;
        const sum = retardPercentage + charge;

        return {
            retard: retardPercentage,
            charge: charge,
            sum: sum,
            capacite: machinesAvailable,
            totalMachines: totalMachines,
            totalCuttingTime: totalCuttingTime,
            retardMinutes: retard,
            availableTime: availableTime
        };
    };

    /**
     * Format date for display dd/MM.
     */
    formatDateShort = (dateStr) => {
        const parts = dateStr.split('-');
        return `${parts[2]}/${parts[1]}`;
    };

    /**
     * Build chart data for the stacked bar chart.
     * Each bar = one date/shift combination.
     * Stack segments: Retard (red), Charge (green), Remaining capacity (light gray).
     * Overload beyond 100% is shown with the sum color being red if > 100%.
     */
    buildChartData = () => {
        const dates = this.getDates();
        const { selectedMachineType, currentShift, currentDate } = this.state;

        const labels = [];
        const retardData = [];
        const chargeData = [];
        const remainingData = [];
        const bgColors = { retard: [], charge: [], remaining: [] };

        dates.forEach(date => {
            [1, 2, 3].forEach(shift => {
                const label = `${this.formatDateShort(date)} S${shift}`;
                labels.push(label);

                const data = this.computeShiftData(date, shift, selectedMachineType);

                const isCurrentShift = currentShift && date === currentDate && shift === currentShift.shift;
                const isPast = this.isShiftPast(date, shift);
                const isFuture = this.isShiftFuture(date, shift);

                retardData.push(parseFloat(data.retard.toFixed(1)));
                chargeData.push(parseFloat(data.charge.toFixed(1)));

                // Remaining capacity = max(0, 100 - sum)
                const remaining = Math.max(0, 100 - data.sum);
                remainingData.push(parseFloat(remaining.toFixed(1)));

                // Colors based on past/current/future
                if (isPast) {
                    bgColors.retard.push('#e74c3c');      // Red for past retard
                    bgColors.charge.push('#27ae60');       // Green for past charge (actual)
                    bgColors.remaining.push('#ecf0f1');    // Light gray
                } else if (isCurrentShift) {
                    bgColors.retard.push('#e67e22');       // Orange for current retard
                    bgColors.charge.push('#f39c12');       // Yellow/amber for current (mixed)
                    bgColors.remaining.push('#ecf0f1');
                } else {
                    bgColors.retard.push('#e74c3c80');     // Faded red for future retard
                    bgColors.charge.push('#3498db');       // Blue for future (estimated)
                    bgColors.remaining.push('#ecf0f1');
                }
            });
        });

        return {
            labels,
            datasets: [
                {
                    label: 'Retard (%)',
                    data: retardData,
                    backgroundColor: bgColors.retard,
                    stack: 'stack1',
                    borderWidth: 0.5,
                    borderColor: '#c0392b'
                },
                {
                    label: 'Charge (%)',
                    data: chargeData,
                    backgroundColor: bgColors.charge,
                    stack: 'stack1',
                    borderWidth: 0.5,
                    borderColor: '#2ecc71'
                },
                {
                    label: 'Capacité restante (%)',
                    data: remainingData,
                    backgroundColor: bgColors.remaining,
                    stack: 'stack1',
                    borderWidth: 0.5,
                    borderColor: '#bdc3c7'
                }
            ]
        };
    };

    /**
     * Build the chart options.
     */
    buildChartOptions = () => {
        return {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: `KPI Charge Machines - ${this.state.selectedMachineType === 'ALL' ? 'Toutes machines' : this.state.selectedMachineType}`,
                    font: { size: 16, weight: 'bold' }
                },
                legend: {
                    position: 'top',
                    labels: { font: { size: 12 } }
                },
                tooltip: {
                    callbacks: {
                        afterBody: (context) => {
                            const idx = context[0].dataIndex;
                            const dates = this.getDates();
                            const allSlots = [];
                            dates.forEach(date => [1, 2, 3].forEach(shift => allSlots.push({ date, shift })));
                            if (allSlots[idx]) {
                                const { date, shift } = allSlots[idx];
                                const data = this.computeShiftData(date, shift, this.state.selectedMachineType);
                                return [
                                    `───────────────`,
                                    `Total: ${data.sum.toFixed(1)}%`,
                                    `Capacité: ${data.capacite}/${data.totalMachines} machines`,
                                    `Temps coupe: ${data.totalCuttingTime.toFixed(0)} min`,
                                    `Temps dispo: ${data.availableTime.toFixed(0)} min`,
                                    `Retard: ${data.retardMinutes.toFixed(0)} min`
                                ];
                            }
                            return [];
                        }
                    }
                }
            },
            scales: {
                x: {
                    stacked: true,
                    ticks: { font: { size: 10 }, maxRotation: 45 }
                },
                y: {
                    stacked: true,
                    min: 0,
                    suggestedMax: 120,
                    ticks: {
                        callback: (val) => val + '%',
                        font: { size: 11 }
                    },
                    title: {
                        display: true,
                        text: 'Charge (%)',
                        font: { size: 13 }
                    }
                }
            }
        };
    };

    /**
     * Check if a shift is in the past relative to current shift.
     */
    isShiftPast = (date, shift) => {
        const { currentShift, currentDate } = this.state;
        if (!currentShift) return false;
        if (date < currentDate) return true;
        if (date === currentDate && shift < currentShift.shift) return true;
        return false;
    };

    /**
     * Check if a shift is in the future relative to current shift.
     */
    isShiftFuture = (date, shift) => {
        const { currentShift, currentDate } = this.state;
        if (!currentShift) return true;
        if (date > currentDate) return true;
        if (date === currentDate && shift > currentShift.shift) return true;
        return false;
    };

    /**
     * Render the data table view.
     */
    renderTable = () => {
        const dates = this.getDates();
        const { selectedMachineType, currentShift, currentDate } = this.state;

        return (
            <div className="table-responsive" style={{ maxHeight: '600px', overflowY: 'auto' }}>
                <table className="table table-sm table-bordered table-hover" style={{ fontSize: '12px' }}>
                    <thead className="thead-dark">
                        <tr>
                            <th>Date</th>
                            <th>Shift</th>
                            <th>Type</th>
                            <th>Retard (%)</th>
                            <th>Charge (%)</th>
                            <th>Sum (%)</th>
                            <th>Capacité</th>
                            <th>Temps coupe (min)</th>
                            <th>Temps dispo (min)</th>
                            <th>Retard (min)</th>
                        </tr>
                    </thead>
                    <tbody>
                        {dates.map(date => (
                            [1, 2, 3].map(shift => {
                                const data = this.computeShiftData(date, shift, selectedMachineType);
                                const isCurrentShift = currentShift && date === currentDate && shift === currentShift.shift;
                                const isPast = this.isShiftPast(date, shift);

                                return (
                                    <tr
                                        key={`${date}-${shift}`}
                                        style={{
                                            backgroundColor: isCurrentShift ? '#fff3cd' : isPast ? '#f8f9fa' : '#ffffff',
                                            fontWeight: isCurrentShift ? 'bold' : 'normal'
                                        }}
                                    >
                                        {shift === 1 && <td rowSpan={3}>{this.formatDateShort(date)}</td>}
                                        <td>{shift}</td>
                                        <td>{selectedMachineType === 'ALL' ? 'Global' : selectedMachineType}</td>
                                        <td style={{ color: data.retard > 0 ? '#e74c3c' : '#333' }}>
                                            {data.retard.toFixed(1)}%
                                        </td>
                                        <td style={{
                                            color: data.charge > 100 ? '#e74c3c' : data.charge > 80 ? '#e67e22' : '#27ae60'
                                        }}>
                                            {data.charge.toFixed(1)}%
                                        </td>
                                        <td style={{
                                            color: data.sum > 100 ? '#e74c3c' : data.sum > 80 ? '#e67e22' : '#27ae60',
                                            fontWeight: 'bold'
                                        }}>
                                            {data.sum.toFixed(1)}%
                                        </td>
                                        <td>{data.capacite}/{data.totalMachines}</td>
                                        <td>{data.totalCuttingTime.toFixed(0)}</td>
                                        <td>{data.availableTime.toFixed(0)}</td>
                                        <td style={{ color: data.retardMinutes > 0 ? '#e74c3c' : '#333' }}>
                                            {data.retardMinutes.toFixed(0)}
                                        </td>
                                    </tr>
                                );
                            })
                        ))}
                    </tbody>
                </table>
            </div>
        );
    };

    render() {
        const { dateDebut, dateFin, loading, error, viewMode, selectedMachineType } = this.state;
        const machineTypes = this.getMachineTypes();

        return (
            <div className="container-fluid" style={{ padding: '15px' }}>
                {/* Header */}
                <div className="d-flex align-items-center justify-content-between mb-3">
                    <h4 className="mb-0">
                        <FontAwesomeIcon icon={faChartBar} className="mr-2" />
                        KPI Charge Machines
                    </h4>
                    <div className="d-flex align-items-center">
                        <FontAwesomeIcon icon={faInfoCircle} className="mr-2 text-muted" />
                        <small className="text-muted">
                            <span style={{ display: 'inline-block', width: 12, height: 12, backgroundColor: '#27ae60', marginRight: 4, borderRadius: 2 }}></span> Passé (réel)
                            <span style={{ display: 'inline-block', width: 12, height: 12, backgroundColor: '#f39c12', marginRight: 4, marginLeft: 10, borderRadius: 2 }}></span> Actuel (mixte)
                            <span style={{ display: 'inline-block', width: 12, height: 12, backgroundColor: '#3498db', marginRight: 4, marginLeft: 10, borderRadius: 2 }}></span> Futur (estimé)
                            <span style={{ display: 'inline-block', width: 12, height: 12, backgroundColor: '#e74c3c', marginRight: 4, marginLeft: 10, borderRadius: 2 }}></span> Retard
                        </small>
                    </div>
                </div>

                {/* Controls */}
                <div className="card mb-3">
                    <div className="card-body d-flex flex-wrap align-items-center" style={{ gap: '10px', padding: '10px 15px' }}>
                        <div className="form-group mb-0">
                            <label className="small mb-0 mr-2">Date début</label>
                            <input
                                type="date"
                                className="form-control form-control-sm"
                                value={dateDebut}
                                onChange={(e) => this.setState({ dateDebut: e.target.value })}
                                style={{ width: '150px', display: 'inline-block' }}
                            />
                        </div>
                        <div className="form-group mb-0">
                            <label className="small mb-0 mr-2">Date fin</label>
                            <input
                                type="date"
                                className="form-control form-control-sm"
                                value={dateFin}
                                onChange={(e) => this.setState({ dateFin: e.target.value })}
                                style={{ width: '150px', display: 'inline-block' }}
                            />
                        </div>
                        <div className="form-group mb-0">
                            <label className="small mb-0 mr-2">Type machine</label>
                            <select
                                className="form-control form-control-sm"
                                value={selectedMachineType}
                                onChange={(e) => this.setState({ selectedMachineType: e.target.value })}
                                style={{ width: '160px', display: 'inline-block' }}
                            >
                                <option value="ALL">Toutes les machines</option>
                                {machineTypes.map(type => (
                                    <option key={type} value={type}>{type}</option>
                                ))}
                            </select>
                        </div>
                        <button
                            className="btn btn-primary btn-sm"
                            onClick={this.loadData}
                            disabled={loading}
                        >
                            <FontAwesomeIcon icon={loading ? faSpinner : faSearch} spin={loading} className="mr-1" />
                            Rechercher
                        </button>
                        <button
                            className="btn btn-outline-secondary btn-sm"
                            onClick={this.loadData}
                            disabled={loading}
                        >
                            <FontAwesomeIcon icon={faSync} className="mr-1" />
                        </button>

                        <div className="ml-auto btn-group btn-group-sm">
                            <button
                                className={`btn ${viewMode === 'chart' ? 'btn-dark' : 'btn-outline-dark'}`}
                                onClick={() => this.setState({ viewMode: 'chart' })}
                            >
                                <FontAwesomeIcon icon={faChartBar} className="mr-1" /> Graphique
                            </button>
                            <button
                                className={`btn ${viewMode === 'table' ? 'btn-dark' : 'btn-outline-dark'}`}
                                onClick={() => this.setState({ viewMode: 'table' })}
                            >
                                <FontAwesomeIcon icon={faTable} className="mr-1" /> Tableau
                            </button>
                        </div>
                    </div>
                </div>

                {/* Error */}
                {error && (
                    <div className="alert alert-danger">{error}</div>
                )}

                {/* Loading */}
                {loading && (
                    <div className="text-center py-5">
                        <FontAwesomeIcon icon={faSpinner} spin size="2x" className="text-primary" />
                        <p className="mt-2">Chargement des données...</p>
                    </div>
                )}

                {/* Content */}
                {!loading && (
                    <div className="card">
                        <div className="card-body" style={{ padding: '10px' }}>
                            {viewMode === 'chart' ? (
                                <div style={{ height: '500px', position: 'relative' }}>
                                    <Bar data={this.buildChartData()} options={this.buildChartOptions()} plugins={[LINE_100_PLUGIN]} />
                                </div>
                            ) : (
                                this.renderTable()
                            )}
                        </div>
                    </div>
                )}

                {/* Per-type breakdown (only when ALL selected) */}
                {!loading && selectedMachineType === 'ALL' && machineTypes.length > 0 && viewMode === 'chart' && (
                    <div className="row mt-3">
                        {machineTypes.map(type => (
                            <div key={type} className="col-md-6 mb-3">
                                <div className="card">
                                    <div className="card-header py-1" style={{ backgroundColor: MACHINE_TYPE_COLORS[type] || '#ddd', fontSize: '13px', fontWeight: 'bold' }}>
                                        {type}
                                    </div>
                                    <div className="card-body" style={{ padding: '5px', height: '280px' }}>
                                        <Bar
                                            data={this.buildChartDataForType(type)}
                                            options={{
                                                responsive: true,
                                                maintainAspectRatio: false,
                                                plugins: {
                                                    title: { display: false },
                                                    legend: { display: false }
                                                },
                                                scales: {
                                                    x: { stacked: true, ticks: { font: { size: 9 }, maxRotation: 45 } },
                                                    y: {
                                                        stacked: true,
                                                        min: 0,
                                                        suggestedMax: 120,
                                                        ticks: { callback: (v) => v + '%', font: { size: 9 } }
                                                    }
                                                }
                                            }}
                                            plugins={[LINE_100_PLUGIN]}
                                        />
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        );
    }

    /**
     * Build chart data for a specific machine type (used in per-type breakdown).
     */
    buildChartDataForType = (machineType) => {
        const dates = this.getDates();
        const { currentShift, currentDate } = this.state;

        const labels = [];
        const retardData = [];
        const chargeData = [];
        const remainingData = [];
        const bgRetard = [];
        const bgCharge = [];

        dates.forEach(date => {
            [1, 2, 3].forEach(shift => {
                labels.push(`${this.formatDateShort(date)} S${shift}`);
                const data = this.computeShiftData(date, shift, machineType);
                const isCurrentShift = currentShift && date === currentDate && shift === currentShift.shift;
                const isPast = this.isShiftPast(date, shift);

                retardData.push(parseFloat(data.retard.toFixed(1)));
                chargeData.push(parseFloat(data.charge.toFixed(1)));
                remainingData.push(parseFloat(Math.max(0, 100 - data.sum).toFixed(1)));

                if (isPast) {
                    bgRetard.push('#e74c3c');
                    bgCharge.push('#27ae60');
                } else if (isCurrentShift) {
                    bgRetard.push('#e67e22');
                    bgCharge.push('#f39c12');
                } else {
                    bgRetard.push('#e74c3c80');
                    bgCharge.push('#3498db');
                }
            });
        });

        return {
            labels,
            datasets: [
                { label: 'Retard', data: retardData, backgroundColor: bgRetard, stack: 'stack1', borderWidth: 0 },
                { label: 'Charge', data: chargeData, backgroundColor: bgCharge, stack: 'stack1', borderWidth: 0 },
                { label: 'Restante', data: remainingData, backgroundColor: '#ecf0f1', stack: 'stack1', borderWidth: 0 }
            ]
        };
    };
}

const mapStateToProps = (state) => ({
    security: state.security
});

export default connect(mapStateToProps)(KpiChargeMachine);
