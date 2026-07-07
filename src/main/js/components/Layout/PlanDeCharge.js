import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import axios from 'axios';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSearch, faSync, faTimes, faSave, faEdit, faPlus, faInfoCircle, faSpinner, faDownload, faTrash, faList, faEye, faEyeSlash, faCalculator, faChartBar, faPen, faCog } from '@fortawesome/free-solid-svg-icons';
import './styles/PlanDeCharge.scss';

// Status codes configuration
const STATUS_CONFIG = {
    M: { name: 'Marche', color: '#00b050', textColor: '#fff' },
    A: { name: 'Arrêt', color: '#a5a5a5', textColor: '#fff' },
    P: { name: 'PM', color: '#ffff00', textColor: '#000' },
    O: { name: 'OFF', color: '#000000', textColor: '#fff' },
    R: { name: 'Récupération', color: '#e2efda', textColor: '#000' },
    MS: { name: 'Machine Spéciale', color: '#00da63', textColor: '#fff' },
    AD: { name: 'Arrêt Prod', color: '#ff0000', textColor: '#fff' },
    ADM: { name: 'Arrêt Maintenance', color: '#ed7d31', textColor: '#fff' },
    MD: { name: 'Démarrée sur demande', color: '#c00000', textColor: '#fff' },
    PN: { name: 'En Panne', color: '#c00000', textColor: '#fff' }
};

// Machine type colors
const MACHINE_TYPE_COLORS = {
    'Lectra': '#a9d08e',
    'Lectra IP6': '#92d050',
    'Gerber': '#e2efda',
    'LASER-DXF': '#ffc000',
    'DIE': '#0077ff'
};

class PlanDeCharge extends Component {
    constructor(props) {
        super(props);
        
        const today = new Date();
        const startDate = new Date(today);
        startDate.setDate(startDate.getDate() - 1);
        const endDate = new Date(today);
        endDate.setDate(endDate.getDate() + 1);
        const embeddedDate = props.embeddedDate;
        const embeddedShift = props.embeddedShift;
        this.state = {
            dateDebut: embeddedDate || startDate.toISOString().split('T')[0],
            dateFin: embeddedDate || endDate.toISOString().split('T')[0],
            loading: false,
            loadingPhase: '', // For progressive loading indication
            error: null,
            machinesByZone: {},
            statusGrid: {},
            loadCalculations: [],
            seriesData: [], // Raw series for the date range (from /seriesForDateRange)
            aggregatedCuttingTime: {}, // Computed on frontend from seriesData
            aggregatedCuttingTimeWithStatus: {}, // Computed on frontend from seriesData
            currentShift: embeddedShift != null ? embeddedShift : null,
            currentDate: embeddedDate || today.toISOString().split('T')[0], // Today's date for highlighting
            showModal: false,
            modalData: null,
            selectedCell: null,
            statusHistory: [],
            allHistoryList: [], // All EtatMachineHistorique records
            showAllHistory: false, // Toggle for showing all history
            editingItem: null, // Item being edited
            machineHistory: [], // History for the selected machine in modal
            showLegend: true,
            showLoadIndicators: false, // Toggle for load indicator columns
            calculating: false,
            // Charge calculation states
            selectedShiftForCharge: null, // { date, shift }
            chargeSummary: null, // Detailed charge summary for selected shift
            detailedSeries: [], // Series list for selected shift
            showChargeDetails: false, // Toggle for showing charge details panel
            loadingCharge: false,
            chargeDetailsMachineTypeFilter: null, // Filter series by machine type in charge details
            // Next shift series states
            nextShiftSeries: [],
            nextShiftLoadingSeries: false,
            showNextShiftSeries: false,
            // Capacité installée data
            capaciteInstalleeData: [],
            // Capacité installée editing
            editingCapacite: null, // { id, groupe, capaciteInstallee, efficienceTarget }
            savingCapacite: false,
            // Non-imported charge (Order_Schedule status='F')
            nonImportedCharge: null, // { totalMinutes, count, details }
            loadingNonImportedCharge: false,
            showNonImportedChargeModal: false,
            // Capacité installée rules (interval-based defaults / overrides)
            capaciteRules: [],
            showRulesModal: false,
            editingRule: null,
            savingRule: false,
            // Part-number cutting-time report (in Détails de charge)
            partNumberReport: [],
            pnReportFilters: {},
            pnReportSort: { col: null, dir: 'asc' },
            // Shift snapshot caching: old shifts are served from a persisted snapshot
            servedFromSnapshot: false,
            snapshotAt: null
        };
    }

    componentDidMount() {
        this.loadInitialData();
        this.loadRules();
    }

    /**
     * Load initial data that doesn't change with date range: machines list and current shift.
     * These are loaded once on mount, not on every search/refresh.
     */
    loadInitialData = async () => {
        try {
            const [machinesResponse, currentShiftResponse] = await Promise.all([
                axios.get('/api/productionTable/list'),
                axios.get('/api/planDeCharge/currentShift')
            ]);

            // Group machines by zone (same logic as backend getMachinesGroupedByZone)
            const machines = machinesResponse.data || [];
            const machinesByZone = {};
            machines.forEach(m => {
                if (m.zone) {
                    const zoneName = m.zone.nom || 'Unknown';
                    if (!machinesByZone[zoneName]) machinesByZone[zoneName] = [];
                    machinesByZone[zoneName].push(m);
                }
            });
            // Sort zones by zone.orderInd then by name, and machines within zones by name
            const sortedByZone = {};
            Object.keys(machinesByZone)
                .sort((a, b) => {
                    const za = machinesByZone[a][0]?.zone;
                    const zb = machinesByZone[b][0]?.zone;
                    const orderA = za?.orderInd ?? 999;
                    const orderB = zb?.orderInd ?? 999;
                    if (orderA !== orderB) return orderA - orderB;
                    return a.localeCompare(b);
                })
                .forEach(zone => {
                    sortedByZone[zone] = machinesByZone[zone].sort((a, b) => (a.nom || '').localeCompare(b.nom || ''));
                });

            this.setState({
                machinesByZone: sortedByZone,
                currentShift: this.props.embeddedShift != null ? this.props.embeddedShift : currentShiftResponse.data
            }, () => {
                this.loadData();
            });
        } catch (error) {
            console.error('Error loading initial data:', error);
            this.setState({ error: 'Erreur lors du chargement initial: ' + (error.response?.data?.message || error.message) });
        }
    };

    /**
     * Format date to dd/MM/yyyy HH:mm format (24h).
     */
    formatDateTimeFR = (dateStr) => {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${day}/${month}/${year} ${hours}:${minutes}`;
    };

    /**
     * Convert datetime-local value to dd/MM/yyyy HH:mm for display.
     */
    toDateTimeLocal = (date) => {
        if (!date) return '';
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        return `${year}-${month}-${day}T${hours}:${minutes}`;
    };

    /**
     * Format datetime-local value to dd/MM/yyyy HH:mm for text input display (24h format).
     */
    formatDateTimeForInput = (dateTimeLocalValue) => {
        if (!dateTimeLocalValue) return '';
        // dateTimeLocalValue is in format yyyy-MM-ddTHH:mm
        const parts = dateTimeLocalValue.split('T');
        if (parts.length !== 2) return dateTimeLocalValue;
        const datePart = parts[0]; // yyyy-MM-dd
        const timePart = parts[1]; // HH:mm
        const dateParts = datePart.split('-');
        if (dateParts.length !== 3) return dateTimeLocalValue;
        return `${dateParts[2]}/${dateParts[1]}/${dateParts[0]} ${timePart}`;
    };

    /**
     * Parse dd/MM/yyyy HH:mm input to datetime-local format yyyy-MM-ddTHH:mm.
     */
    handleDateTimeInputChange = (field, value) => {
        // Expected format: dd/MM/yyyy HH:mm
        const regex = /^(\d{2})\/(\d{2})\/(\d{4})\s+(\d{2}):(\d{2})$/;
        const match = value.match(regex);
        if (match) {
            const [_, day, month, year, hours, minutes] = match;
            const dateTimeLocal = `${year}-${month}-${day}T${hours}:${minutes}`;
            this.handleModalChange(field, dateTimeLocal);
        }
        // For partial input, we don't update - user must complete the format
    };

    /**
     * Get unique machine types from machinesByZone.
     */
    getUniqueMachineTypes = () => {
        const { machinesByZone } = this.state;
        const types = new Set();
        Object.values(machinesByZone).forEach(machines => {
            machines.forEach(machine => {
                if (machine.machineType?.name) {
                    types.add(machine.machineType.name);
                }
            });
        });
        return Array.from(types).sort();
    };

    /**
     * Get load indicator data for a specific date/shift/machineType.
     * Calculates: Ret (retard from prev shift), Chg (charge based on estimated cutting time),
     * SR (surpassing: series planned here but already worked in a previous shift),
     * Sum = Ret + Chg - SR, Cap (available machines), Rec (recommended machines).
     * 
     * Chg is based purely on estimated temps de coupe per serie (no dateDebut/dateFin).
     * Ret uses dateDebutCoupe/dateFinCoupe to check overlap from previous shift.
     * SR uses dateDebutCoupe/dateFinCoupe to detect series that were worked in advance.
     * Gerber machines: estimated time is multiplied by 2.
     */
    getLoadIndicator = (date, shift, machineType) => {
        const { statusGrid, machinesByZone, aggregatedByMachineType } = this.state;

        // Count machines of this type and available ones
        let machinesAvailable = 0;
        let totalMachines = 0;

        Object.values(machinesByZone).forEach(machines => {
            machines.forEach(machine => {
                if (machine.machineType?.name === machineType) {
                    totalMachines++;
                    const status = statusGrid[machine.nom]?.[date]?.[shift] || 'M';
                    if (status === 'M') {
                        machinesAvailable++;
                    }
                }
            });
        });

        // Apply efficience (and per-shift configured minutes) so the grid % matches the modal
        // "verifier formule de charge multiple par efficience"
        const groupe = this.getGroupeForMachineType(machineType);
        const capEntry = this.getCapaciteInstalleeEntry(date, shift, groupe);
        const availableTime = this.getShiftProductiveCapacityMinutes(machinesAvailable, capEntry);
        const perMachineMinutes = this.getConfiguredShiftMinutes(capEntry);

        // Get cutting time data from machineType-level aggregation (same source as renderChargeDetails)
        const typeData = aggregatedByMachineType?.[machineType]?.[date]?.[shift];
        const totalCuttingTime = typeData?.total || 0;
        const srTime = typeData?.sr || 0;

        const charge = availableTime > 0 ? (totalCuttingTime / availableTime) * 100 : 0;

        // Retard from previous shift
        let prevShift = shift - 1;
        let prevDate = date;
        if (shift === 1) {
            prevShift = 3;
            const d = new Date(date);
            d.setDate(d.getDate() - 1);
            prevDate = d.toISOString().split('T')[0];
        }
        const retard = this.getAdjustedRetardCarryoverForTypes([machineType], prevDate, prevShift, groupe);

        const retardPercentage = availableTime > 0 ? (retard / availableTime * 100) : 0;
        const srPercentage = availableTime > 0 ? (srTime / availableTime * 100) : 0;
        const sum = retardPercentage + charge - srPercentage;

        // Recommended machines — use productive minutes per machine (configured × efficience)
        // so the recommendation aligns with the efficiency-aware % above.
        const productiveMinutesPerMachine = perMachineMinutes
            * (Number.isFinite(Number(capEntry?.efficienceTarget)) ? Number(capEntry.efficienceTarget) / 100 : 0.9);
        let recommended = machinesAvailable;
        const effectiveWork = totalCuttingTime + retard - srTime;
        if (effectiveWork > 0 && productiveMinutesPerMachine > 0) {
            recommended = Math.ceil(effectiveWork / productiveMinutesPerMachine);
            recommended = Math.min(recommended, totalMachines);
            recommended = Math.max(recommended, 1);
        }

        return {
            retard: retardPercentage.toFixed(1),
            charge: charge.toFixed(1),
            sr: srPercentage.toFixed(1),
            sum: sum.toFixed(1),
            capacite: machinesAvailable,
            totalMachines,
            recommended,
            hasData: totalCuttingTime > 0 || retard > 0 || srTime > 0
        };
    };

    /**
     * Get general cutting charge (GLOBAL) for a specific date/shift.
     */
    getGlobalCharge = (date, shift) => {
        const { aggregatedCuttingTime, machinesByZone, statusGrid } = this.state;

        if (aggregatedCuttingTime && Object.keys(aggregatedCuttingTime).length > 0) {
            let totalCuttingTime = 0;

            Object.entries(aggregatedCuttingTime).forEach(([machine, dateData]) => {
                if (machine && dateData[date] && dateData[date][shift]) {
                    totalCuttingTime += dateData[date][shift];
                }
            });

            // Per-type available time so each group (Coupe vs Laser) uses its own efficience target.
            // Sum across types for the global denominator.
            const machineTypeMap = {};
            Object.values(machinesByZone).forEach(machines => {
                machines.forEach(m => {
                    if (m.machineType?.name) machineTypeMap[m.nom] = m.machineType.name;
                });
            });
            const availableByType = {};
            Object.values(machinesByZone).forEach(machines => {
                machines.forEach(machine => {
                    const status = statusGrid[machine.nom]?.[date]?.[shift] || 'M';
                    if (status === 'M') {
                        const type = machineTypeMap[machine.nom] || 'Unknown';
                        availableByType[type] = (availableByType[type] || 0) + 1;
                    }
                });
            });
            let availableTime = 0;
            Object.entries(availableByType).forEach(([type, count]) => {
                const capEntry = this.getCapaciteInstalleeEntry(date, shift, this.getGroupeForMachineType(type));
                availableTime += this.getShiftProductiveCapacityMinutes(count, capEntry);
            });

            const loadPercentage = availableTime > 0 ? (totalCuttingTime / availableTime) * 100 : 0;

            return loadPercentage.toFixed(1);
        }

        return '-';
    };

    /**
     * Toggle load indicator columns visibility.
     */
    toggleLoadIndicators = () => {
        this.setState(prev => ({ showLoadIndicators: !prev.showLoadIndicators }));
    };

    /**
     * Get Coupe group indicator (Lectra + Lectra IP6 combined).
     */
    getCoupeGroupIndicator = (date, shift) => {
        const coupeTypes = ['Lectra', 'Lectra IP6', 'Gerber'];
        const { statusGrid, machinesByZone, aggregatedByMachineType } = this.state;

        let machinesAvailable = 0;
        let totalMachines = 0;

        Object.values(machinesByZone).forEach(machines => {
            machines.forEach(machine => {
                if (coupeTypes.includes(machine.machineType?.name)) {
                    totalMachines++;
                    const status = statusGrid[machine.nom]?.[date]?.[shift] || 'M';
                    if (status === 'M') {
                        machinesAvailable++;
                    }
                }
            });
        });

        // Sum data from all coupe types using machineType-level aggregation
        let totalCuttingTime = 0;
        let srTime = 0;
        let retard = 0;

        let prevShift = shift - 1;
        let prevDate = date;
        if (shift === 1) {
            prevShift = 3;
            const d = new Date(date);
            d.setDate(d.getDate() - 1);
            prevDate = d.toISOString().split('T')[0];
        }

        coupeTypes.forEach(ctype => {
            const typeData = aggregatedByMachineType?.[ctype]?.[date]?.[shift];
            if (typeData) {
                totalCuttingTime += typeData.total || 0;
                srTime += typeData.sr || 0;
            }
        });
        retard = this.getAdjustedRetardCarryoverForTypes(coupeTypes, prevDate, prevShift, 'Coupe');

        // Coupe group shares a single capaciteInstallee entry (groupe = 'Coupe').
        const capEntry = this.getCapaciteInstalleeEntry(date, shift, 'Coupe');
        const availableTime = this.getShiftProductiveCapacityMinutes(machinesAvailable, capEntry);
        const productiveMinutesPerMachine = this.getConfiguredShiftMinutes(capEntry)
            * (Number.isFinite(Number(capEntry?.efficienceTarget)) ? Number(capEntry.efficienceTarget) / 100 : 0.9);
        const charge = availableTime > 0 ? (totalCuttingTime / availableTime) * 100 : 0;

        const retardPercentage = availableTime > 0 ? (retard / availableTime * 100) : 0;
        const srPercentage = availableTime > 0 ? (srTime / availableTime * 100) : 0;
        const sum = retardPercentage + charge - srPercentage;

        let recommended = machinesAvailable;
        const effectiveWork = totalCuttingTime + retard - srTime;
        if (effectiveWork > 0 && productiveMinutesPerMachine > 0) {
            recommended = Math.ceil(effectiveWork / productiveMinutesPerMachine);
            recommended = Math.min(recommended, totalMachines);
            recommended = Math.max(recommended, 1);
        }

        return {
            retard: retardPercentage.toFixed(1),
            charge: charge.toFixed(1),
            sr: srPercentage.toFixed(1),
            sum: sum.toFixed(1),
            capacite: machinesAvailable,
            totalMachines,
            recommended,
            hasData: totalCuttingTime > 0 || retard > 0 || srTime > 0
        };
    };

    /**
     * A shift is "old" when it is strictly before the current shift (earlier date,
     * or an earlier shift of the current date). Old shifts are served from a
     * persisted snapshot; the current, next and future shifts are always live.
     * Returns false in embedded mode / when the current shift is unknown.
     */
    isOldShift = (date, shift) => {
        const cs = this.state.currentShift;
        if (!cs || typeof cs !== 'object') return false;
        const curDate = cs.date;
        const curShift = cs.shift;
        if (!curDate || curShift == null) return false;
        const s = parseInt(shift, 10);
        if (date < curDate) return true;
        if (date === curDate && s < curShift) return true;
        return false;
    };

    /**
     * Load detailed charge information for a specific shift.
     * Derives data from already-loaded seriesData instead of calling backend.
     * Old shifts are served from a persisted snapshot unless {@code forceRefresh}
     * is set (the "Actualiser" button), which recomputes live and overwrites it.
     */
    loadChargeDetails = async (date, shift, forceRefresh = false) => {
        const { seriesData, machinesByZone, statusGrid, currentShift, currentDate } = this.state;
        const shiftStr = String(shift);

        // Old shift served from a persisted snapshot (unless the user forced a
        // refresh): skip recomputing series + reading cut files entirely.
        const isOld = this.isOldShift(date, shiftStr);
        if (isOld && !forceRefresh) {
            try {
                const snapResp = await axios.get('/api/planDeCharge/snapshot', {
                    params: { date, shift: shiftStr }
                });
                if (snapResp.status === 200 && snapResp.data && snapResp.data.snapshotJson) {
                    const payload = JSON.parse(snapResp.data.snapshotJson);
                    this.setState({
                        selectedShiftForCharge: { date, shift },
                        showChargeDetails: true,
                        loadingCharge: false,
                        chargeSummary: payload.chargeSummary || null,
                        detailedSeries: payload.detailedSeries || [],
                        partNumberReport: payload.partNumberReport || [],
                        nonImportedCharge: payload.nonImportedCharge || null,
                        pnReportFilters: {},
                        pnReportSort: { col: null, dir: 'asc' },
                        servedFromSnapshot: true,
                        snapshotAt: snapResp.data.updatedAt || snapResp.data.createdAt || null
                    });
                    return;
                }
            } catch (err) {
                // No snapshot yet (204) or read error — fall through to live compute.
            }
        }

        // Fetch non-imported charge in parallel
        this.setState({ loadingNonImportedCharge: true });
        let nonImportedCharge = null;
        try {
            const nicResp = await axios.get('/api/planDeCharge/nonImportedCharge', {
                params: { date, shift: shiftStr }
            });
            nonImportedCharge = nicResp.data;
        } catch (err) {
            console.error('Error fetching non-imported charge:', err);
            nonImportedCharge = { totalMinutes: 0, count: 0, details: [] };
        }
        this.setState({ nonImportedCharge, loadingNonImportedCharge: false });

        // Fetch the part-number cutting-time report (imported + non-imported)
        let partNumberReport = [];
        try {
            const pnResp = await axios.get('/api/planDeCharge/partNumberReport', {
                params: { date, shift: shiftStr }
            });
            partNumberReport = pnResp.data || [];
        } catch (err) {
            console.error('Error fetching part-number report:', err);
        }

        // Filter series for this date/shift
        const shiftSeries = seriesData.filter(s => s.dueDate === date && String(s.dueShift) === shiftStr);

        // Build machine type map
        const machineTypeMap = {};
        Object.values(machinesByZone).forEach(machines => {
            machines.forEach(m => {
                if (m.machineType?.name) machineTypeMap[m.nom] = m.machineType.name;
            });
        });

        const shiftNum = parseInt(shiftStr, 10) || 1;
        const shiftTimes = this.getShiftTimes(date, shiftNum);
        const shiftStart = shiftTimes.start;
        const shiftEnd = shiftTimes.end;

        // Build detailed series with status
        const detailedSeries = shiftSeries.map(serie => {
            const machine = serie.machine || 'Unknown';
            const ect = serie.effectiveCuttingTime || 0;
            const dateDebutCoupe = serie.dateDebutCoupe ? new Date(serie.dateDebutCoupe) : null;
            const dateFinCoupe = serie.dateFinCoupe ? new Date(serie.dateFinCoupe) : null;

            const isPrepared = true;
            const isCutting = dateDebutCoupe !== null && dateFinCoupe === null;
            const isCut = dateFinCoupe !== null;

            let isPartialRetard = false;
            let retardMinutes = 0;

            // SR: minutes worked BEFORE the shift started
            let srMinutes = 0;
            if (dateDebutCoupe && dateDebutCoupe < shiftStart) {
                const srEnd = (dateFinCoupe && dateFinCoupe < shiftStart) ? dateFinCoupe : shiftStart;
                srMinutes = Math.max(0, (srEnd - dateDebutCoupe) / 60000);
                if (srMinutes > ect) srMinutes = ect;
            }

            // Retard: series not completed within the shift
            if (!dateDebutCoupe) {
                retardMinutes = ect;
            } else if (dateDebutCoupe > shiftEnd) {
                retardMinutes = ect;
            } else if (!dateFinCoupe && new Date() > shiftEnd) {
                retardMinutes = ect;
            } else if (dateFinCoupe && dateFinCoupe > shiftEnd) {
                isPartialRetard = true;
                retardMinutes = Math.min((dateFinCoupe - shiftEnd) / 60000, ect);
            }

            return {
                serie: serie.serie || '',
                sequence: serie.sequence || '',
                cuttingPlanId: serie.cuttingPlanId || null,
                cmsId: serie.cmsId || null,
                placement: serie.placement || '',
                machine,
                tableCoupe: serie.tableCoupe || '',
                nbrCouche: serie.nbrCouche || null,
                tempsDeCoupe: serie.tempsDeCoupe || null,
                effectiveCuttingTime: ect,
                // Provenance of effectiveCuttingTime: 'Validated' | 'Real' | 'TempsDeCoupe' | ''
                // Now passed through from the server (see PlanDeChargeService.getSeriesForDateRange).
                cuttingTimeSource: serie.cuttingTimeSource || '',
                isPrepared,
                dateDebutCoupe: serie.dateDebutCoupe,
                dateFinCoupe: serie.dateFinCoupe,
                isCut,
                isCutting,
                isCarryover: retardMinutes > 0,
                isPartialRetard,
                retardMinutes,
                srMinutes
            };
        });

        // --- Compute previous shift retard (retard that comes INTO this shift) ---
        let prevShift = shiftNum - 1;
        let prevDate = date;
        if (shiftNum === 1) {
            prevShift = 3;
            const d = new Date(date);
            d.setDate(d.getDate() - 1);
            prevDate = d.toISOString().split('T')[0];
        }
        const retardPrevByType = {};
        let totalRetardPrev = 0;
        let totalRetardPrevRaw = 0;

        // --- Aggregate per machine type for THIS shift ---
        const machineTypeSummaries = {};
        let totalChgMin = 0, totalSrMin = 0, totalRetardThisShift = 0;

        detailedSeries.forEach(s => {
            const machineType = machineTypeMap[s.tableCoupe] || machineTypeMap[s.machine] || s.tableCoupe || s.machine;
            if (!machineTypeSummaries[machineType]) {
                machineTypeSummaries[machineType] = {
                    seriesCount: 0, chargeMin: 0, srMin: 0, retardThisShift: 0
                };
            }
            const mt = machineTypeSummaries[machineType];
            mt.seriesCount++;
            mt.chargeMin += s.effectiveCuttingTime || 0;
            mt.srMin += s.srMinutes || 0;
            mt.retardThisShift += s.retardMinutes || 0;
        });

        // Calculate percentages per machine type
        const machineTypeIndicators = {};

        // Determine if this is current shift or next shift (for time-left calculation)
        const currentShiftNum = currentShift ? currentShift.shift : null;
        const nextShiftInfo = this.getNextShiftInfo();
        const isCurrentShift = currentShift && currentDate === date && currentShiftNum === shiftNum;
        const isNextShift = nextShiftInfo && nextShiftInfo.date === date && nextShiftInfo.shift === shiftNum;
        const showTimeLeft = isCurrentShift || isNextShift;

        // Helper: lookup capacité installée for a date/shift/groupe
        const getCapaciteForGroupe = (groupe) => this.getCapaciteInstalleeEntry(date, shiftNum, groupe);

        // Accumulate installed-capacity minutes across types so we can produce a global %.
        let globalInstalledCapacityMinutes = 0;

        Object.entries(machineTypeSummaries).forEach(([type, data]) => {
            // Count available machines of this type
            let machinesAvailable = 0, totalMachines = 0;
            Object.values(machinesByZone).forEach(machines => {
                machines.forEach(m => {
                    if ((machineTypeMap[m.nom] || '') === type) {
                        totalMachines++;
                        const status = statusGrid[m.nom]?.[date]?.[shiftNum] || 'M';
                        if (status === 'M') {
                            machinesAvailable++;
                        }
                    }
                });
            });

            const groupe = this.getGroupeForMachineType(type);
            const capEntry = getCapaciteForGroupe(groupe);

            // Available time (currently-available machines × productive minutes per machine).
            // Using the same formula as getShiftProductiveCapacityMinutes so efficiency applies
            // everywhere — this is the "verifier formule de charge multiple par efficience" fix.
            const availableTime = this.getShiftProductiveCapacityMinutes(machinesAvailable, capEntry);

            // Installed-capacity-based charge%:
            // uses capEntry.capaciteInstallee (the installed machine count, regardless of current
            // availability) × configured shift minutes × efficiency ratio.
            const installedMachines = Number(capEntry?.capaciteInstallee);
            const installedCapacityMinutes = Number.isFinite(installedMachines) && installedMachines > 0
                ? this.getShiftProductiveCapacityMinutes(installedMachines, capEntry)
                : 0;
            const chgPctInstalled = installedCapacityMinutes > 0
                ? (data.chargeMin / installedCapacityMinutes) * 100
                : 0;
            globalInstalledCapacityMinutes += installedCapacityMinutes;

            const retPrevRaw = this.getRawRetardCarryoverForTypes([type], prevDate, prevShift);
            const retPrev = this.getAdjustedRetardCarryoverForTypes([type], prevDate, prevShift, groupe);
            retardPrevByType[type] = retPrev;
            totalRetardPrev += retPrev;
            totalRetardPrevRaw += retPrevRaw;
            const chgPct = availableTime > 0 ? (data.chargeMin / availableTime) * 100 : 0;
            const srPct = availableTime > 0 ? (data.srMin / availableTime) * 100 : 0;
            const retPrevPct = availableTime > 0 ? (retPrev / availableTime) * 100 : 0;
            const retPrevRawPct = availableTime > 0 ? (retPrevRaw / availableTime) * 100 : 0;
            const sumPct = chgPct - srPct + retPrevPct;
            const retThisPct = availableTime > 0 ? (data.retardThisShift / availableTime) * 100 : 0;

            // Time left calculation (current shift and next shift only)
            let tempsRestantMin = 0;
            let retardAdjustedMin = data.retardThisShift;
            if (showTimeLeft && machinesAvailable > 0) {
                if (isCurrentShift) {
                    tempsRestantMin = this.getCurrentShiftProductiveMinutesLeft(date, shiftNum, machinesAvailable, capEntry);
                } else if (isNextShift) {
                    tempsRestantMin = this.getShiftProductiveCapacityMinutes(machinesAvailable, capEntry);
                }
                retardAdjustedMin = Math.max(0, data.retardThisShift - tempsRestantMin);
            }
            const retardAdjustedPct = availableTime > 0 ? (retardAdjustedMin / availableTime) * 100 : 0;

            machineTypeIndicators[type] = {
                seriesCount: data.seriesCount,
                chargeMin: data.chargeMin,
                chgPct,
                chgPctInstalled,
                installedMachines: Number.isFinite(installedMachines) ? installedMachines : 0,
                installedCapacityMinutes,
                srMin: data.srMin,
                srPct,
                retardPrevRawMin: retPrevRaw,
                retardPrevRawPct: retPrevRawPct,
                retardPrevMin: retPrev,
                retardPrevPct: retPrevPct,
                sumPct,
                retardThisShiftMin: data.retardThisShift,
                retardThisShiftPct: retThisPct,
                tempsRestantMin: showTimeLeft ? tempsRestantMin : null,
                retardAdjustedMin,
                retardAdjustedPct,
                machinesAvailable,
                totalMachines,
                availableTime
            };

            totalChgMin += data.chargeMin;
            totalSrMin += data.srMin;
            totalRetardThisShift += data.retardThisShift;
        });

        // Global totals — sum the per-type productive minutes so the global % uses the same
        // efficiency-applied denominator as the per-type rows.
        let globalMachinesAvailable = 0, globalTotalMachines = 0;
        Object.values(machinesByZone).forEach(machines => {
            machines.forEach(m => {
                globalTotalMachines++;
                const status = statusGrid[m.nom]?.[date]?.[shiftNum] || 'M';
                if (status === 'M') {
                    globalMachinesAvailable++;
                }
            });
        });
        const globalAvailableTime = Object.values(machineTypeIndicators)
            .reduce((acc, ind) => acc + (ind.availableTime || 0), 0);
        const globalChgPct = globalAvailableTime > 0 ? (totalChgMin / globalAvailableTime) * 100 : 0;
        const globalChgPctInstalled = globalInstalledCapacityMinutes > 0
            ? (totalChgMin / globalInstalledCapacityMinutes) * 100
            : 0;
        const globalSrPct = globalAvailableTime > 0 ? (totalSrMin / globalAvailableTime) * 100 : 0;
        const globalRetPrevPct = globalAvailableTime > 0 ? (totalRetardPrev / globalAvailableTime) * 100 : 0;
        const globalRetPrevRawPct = globalAvailableTime > 0 ? (totalRetardPrevRaw / globalAvailableTime) * 100 : 0;
        const globalSumPct = globalChgPct - globalSrPct + globalRetPrevPct;
        const globalRetThisPct = globalAvailableTime > 0 ? (totalRetardThisShift / globalAvailableTime) * 100 : 0;

        // Compute global time-left totals
        let globalTempsRestantMin = null;
        let globalRetardAdjustedMin = null;
        let globalRetardAdjustedPct = null;
        if (showTimeLeft) {
            globalTempsRestantMin = 0;
            globalRetardAdjustedMin = 0;
            Object.values(machineTypeIndicators).forEach(ind => {
                globalTempsRestantMin += ind.tempsRestantMin || 0;
                globalRetardAdjustedMin += ind.retardAdjustedMin || 0;
            });
            globalRetardAdjustedPct = globalAvailableTime > 0 ? (globalRetardAdjustedMin / globalAvailableTime) * 100 : 0;
        }

        const chargeSummary = {
            totalSeries: detailedSeries.length,
            totalChgMin: totalChgMin,
            totalSrMin: totalSrMin,
            totalRetardPrevRaw,
            totalRetardPrev: totalRetardPrev,
            totalRetardThisShift: totalRetardThisShift,
            globalChgPct,
            globalChgPctInstalled,
            globalInstalledCapacityMinutes,
            globalSrPct,
            globalRetPrevRawPct,
            globalRetPrevPct,
            globalSumPct,
            globalRetThisPct,
            globalMachinesAvailable,
            globalTotalMachines,
            globalAvailableTime,
            machineTypeIndicators,
            retardPrevByType,
            showTimeLeft,
            globalTempsRestantMin,
            globalRetardAdjustedMin,
            globalRetardAdjustedPct
        };

        this.setState({
            selectedShiftForCharge: { date, shift },
            showChargeDetails: true,
            detailedSeries,
            chargeSummary,
            loadingCharge: false,
            nonImportedCharge,
            partNumberReport,
            pnReportFilters: {},
            pnReportSort: { col: null, dir: 'asc' },
            servedFromSnapshot: false,
            snapshotAt: isOld ? new Date().toISOString() : null
        });

        // Persist / overwrite the snapshot for old shifts so subsequent views load
        // instantly from it (the "Actualiser" refresh writes through here too).
        if (isOld) {
            const payload = { chargeSummary, detailedSeries, partNumberReport, nonImportedCharge };
            axios.post('/api/planDeCharge/snapshot', JSON.stringify(payload), {
                params: { date, shift: shiftStr },
                headers: { 'Content-Type': 'text/plain' }
            }).catch(err => console.error('Error saving plan de charge snapshot:', err));
        }
    };

    /**
     * Close the charge details panel.
     */
    closeChargeDetails = () => {
        this.setState({
            showChargeDetails: false,
            selectedShiftForCharge: null,
            chargeSummary: null,
            detailedSeries: [],
            nonImportedCharge: null,
            showNonImportedChargeModal: false,
            servedFromSnapshot: false,
            snapshotAt: null
        });
    };

    /**
     * Save capacité installée changes (capaciteInstallee count and efficienceTarget).
     */
    saveCapaciteInstallee = async (entry) => {
        this.setState({ savingCapacite: true });
        try {
            if (entry.id) {
                await axios.put(`/api/capaciteInstallee/${entry.id}`, entry);
            } else {
                await axios.post('/api/capaciteInstallee/', entry);
            }
            // Reload capacité data
            const capResp = await axios.get('/api/capaciteInstallee/');
            this.setState({ 
                capaciteInstalleeData: capResp.data,
                editingCapacite: null,
                savingCapacite: false
            });
            // Reload charge details if still open
            const { selectedShiftForCharge } = this.state;
            if (selectedShiftForCharge) {
                this.loadChargeDetails(selectedShiftForCharge.date, selectedShiftForCharge.shift);
            }
        } catch (err) {
            console.error('Error saving capacité installée:', err);
            this.setState({ savingCapacite: false });
            alert('Erreur lors de la sauvegarde de la capacité installée.');
        }
    };

    /**
     * Load capacité-installée rules (interval-based defaults / overrides).
     */
    loadRules = async () => {
        try {
            const resp = await axios.get('/api/capaciteInstallee/rules');
            this.setState({ capaciteRules: resp.data || [] });
        } catch (err) {
            console.error('Error loading capacité rules:', err);
        }
    };

    /** Blank rule template for the "add" form (empty strings = unset). */
    blankRule = () => ({
        id: null, dateDebut: '', dateFin: '', dayOfWeek: '', shiftNumber: '',
        groupe: '', capaciteInstallee: '', tempsTotalParMachine: '', efficienceTarget: ''
    });

    /** Persist a rule (create or update), normalising empty strings to null. */
    saveRule = async () => {
        const r = this.state.editingRule;
        if (!r) return;
        const toNum = v => (v === '' || v === null || v === undefined ? null : Number(v));
        const payload = {
            id: r.id || null,
            dateDebut: r.dateDebut || null,
            dateFin: r.dateFin || null,
            dayOfWeek: toNum(r.dayOfWeek),
            shiftNumber: toNum(r.shiftNumber),
            groupe: r.groupe || null,
            capaciteInstallee: toNum(r.capaciteInstallee),
            tempsTotalParMachine: toNum(r.tempsTotalParMachine),
            efficienceTarget: toNum(r.efficienceTarget)
        };
        this.setState({ savingRule: true });
        try {
            if (payload.id) {
                await axios.put(`/api/capaciteInstallee/rules/${payload.id}`, payload);
            } else {
                await axios.post('/api/capaciteInstallee/rules', payload);
            }
            await this.loadRules();
            this.setState({ editingRule: null, savingRule: false });
            // Rules change effective capacity — refresh the displayed data.
            this.loadData();
        } catch (err) {
            console.error('Error saving rule:', err);
            this.setState({ savingRule: false });
            alert('Erreur lors de la sauvegarde de la règle.');
        }
    };

    deleteRule = async (id) => {
        if (!id || !window.confirm('Supprimer cette règle ?')) return;
        try {
            await axios.delete(`/api/capaciteInstallee/rules/${id}`);
            await this.loadRules();
            this.loadData();
        } catch (err) {
            console.error('Error deleting rule:', err);
            alert('Erreur lors de la suppression de la règle.');
        }
    };

    /**
     * Modal to manage capacité-installée rules (ROLE_PROCESS/ADMIN).
     */
    renderRulesModal = () => {
        if (!this.state.showRulesModal) return null;
        const { capaciteRules, editingRule, savingRule } = this.state;
        const dayNames = { 1: 'Lun', 2: 'Mar', 3: 'Mer', 4: 'Jeu', 5: 'Ven', 6: 'Sam', 7: 'Dim' };
        const fmt = v => (v === null || v === undefined || v === '' ? '—' : v);
        const close = () => this.setState({ showRulesModal: false, editingRule: null });
        const r = editingRule || this.blankRule();
        const setF = (f, v) => this.setState({ editingRule: { ...r, [f]: v } });
        return (
            <div className="pdc-modal-overlay" onClick={close}>
                <div className="pdc-modal pdc-modal-large" onClick={e => e.stopPropagation()}>
                    <div className="pdc-modal-header">
                        <h3><FontAwesomeIcon icon={faCog} style={{ marginRight: '8px' }} />Règles de Capacité Installée</h3>
                        <button className="pdc-modal-close" onClick={close}><FontAwesomeIcon icon={faTimes} /></button>
                    </div>
                    <div className="pdc-modal-content">
                        <p style={{ fontSize: '0.82rem', color: '#666', marginTop: 0 }}>
                            Les valeurs explicites (date + shift saisies dans le résumé) priment. Sinon la règle la
                            plus spécifique (intervalle + jour + shift + groupe) s'applique, puis la valeur par défaut.
                            Champ vide = « tous » pour une condition, ou « hériter » pour une valeur.
                        </p>
                        <table className="pdc-summary-table" style={{ fontSize: '0.8rem' }}>
                            <thead>
                                <tr>
                                    <th>Date début</th><th>Date fin</th><th>Jour</th><th>Shift</th><th>Groupe</th>
                                    <th>Cap.</th><th>Temps</th><th>Eff.%</th><th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {capaciteRules.map(rule => (
                                    <tr key={rule.id}>
                                        <td>{fmt(rule.dateDebut)}</td>
                                        <td>{fmt(rule.dateFin)}</td>
                                        <td>{rule.dayOfWeek ? (dayNames[rule.dayOfWeek] || rule.dayOfWeek) : '—'}</td>
                                        <td>{fmt(rule.shiftNumber)}</td>
                                        <td>{fmt(rule.groupe)}</td>
                                        <td>{fmt(rule.capaciteInstallee)}</td>
                                        <td>{fmt(rule.tempsTotalParMachine)}</td>
                                        <td>{fmt(rule.efficienceTarget)}</td>
                                        <td style={{ whiteSpace: 'nowrap' }}>
                                            <button className="pdc-btn-search" style={{ padding: '2px 8px', marginRight: 4 }}
                                                onClick={() => this.setState({ editingRule: {
                                                    id: rule.id,
                                                    dateDebut: rule.dateDebut || '', dateFin: rule.dateFin || '',
                                                    dayOfWeek: rule.dayOfWeek ?? '', shiftNumber: rule.shiftNumber ?? '',
                                                    groupe: rule.groupe || '', capaciteInstallee: rule.capaciteInstallee ?? '',
                                                    tempsTotalParMachine: rule.tempsTotalParMachine ?? '',
                                                    efficienceTarget: rule.efficienceTarget ?? ''
                                                } })}>
                                                <FontAwesomeIcon icon={faPen} />
                                            </button>
                                            <button className="pdc-btn-export" style={{ padding: '2px 8px', background: '#c0392b' }}
                                                onClick={() => this.deleteRule(rule.id)}>
                                                <FontAwesomeIcon icon={faTrash} />
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                {capaciteRules.length === 0 && (
                                    <tr><td colSpan="9" style={{ textAlign: 'center', color: '#999' }}>Aucune règle</td></tr>
                                )}
                            </tbody>
                        </table>

                        <div style={{ marginTop: 16, padding: 12, background: '#f3e5f5', borderRadius: 8, border: '1px solid #ce93d8' }}>
                            <h4 style={{ margin: '0 0 10px 0', color: '#6a1b9a' }}>
                                {editingRule && editingRule.id ? 'Modifier la règle' : 'Nouvelle règle'}
                            </h4>
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'flex-end' }}>
                                <label>Date début<br /><input type="date" value={r.dateDebut} onChange={e => setF('dateDebut', e.target.value)} /></label>
                                <label>Date fin<br /><input type="date" value={r.dateFin} onChange={e => setF('dateFin', e.target.value)} /></label>
                                <label>Jour<br />
                                    <select value={r.dayOfWeek} onChange={e => setF('dayOfWeek', e.target.value)}>
                                        <option value="">Tous</option>
                                        {[1, 2, 3, 4, 5, 6, 7].map(d => <option key={d} value={d}>{dayNames[d]}</option>)}
                                    </select>
                                </label>
                                <label>Shift<br />
                                    <select value={r.shiftNumber} onChange={e => setF('shiftNumber', e.target.value)}>
                                        <option value="">Tous</option><option value="1">1</option><option value="2">2</option><option value="3">3</option>
                                    </select>
                                </label>
                                <label>Groupe<br />
                                    <select value={r.groupe} onChange={e => setF('groupe', e.target.value)}>
                                        <option value="">Tous</option><option value="Coupe">Coupe</option><option value="Laser">Laser</option>
                                    </select>
                                </label>
                                <label>Capacité<br /><input type="number" min="0" style={{ width: 70 }} value={r.capaciteInstallee} onChange={e => setF('capaciteInstallee', e.target.value)} /></label>
                                <label>Temps (min)<br /><input type="number" min="0" style={{ width: 85 }} value={r.tempsTotalParMachine} onChange={e => setF('tempsTotalParMachine', e.target.value)} /></label>
                                <label>Eff. %<br /><input type="number" min="0" max="100" step="0.1" style={{ width: 70 }} value={r.efficienceTarget} onChange={e => setF('efficienceTarget', e.target.value)} /></label>
                                <button className="pdc-btn-search" disabled={savingRule} onClick={this.saveRule}>
                                    {savingRule ? <FontAwesomeIcon icon={faSpinner} spin /> : <FontAwesomeIcon icon={faSave} />} Enregistrer
                                </button>
                                {editingRule && (
                                    <button className="pdc-btn-export" style={{ background: '#999' }} onClick={() => this.setState({ editingRule: null })}>Annuler</button>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    };

    /**
     * Load date-range-dependent data: history (for statusGrid) and series (for indicators).
     * Machines and currentShift are already loaded in componentDidMount.
     * StatusGrid is derived from allHistoryList on the frontend.
     */
    loadData = async () => {
        const { dateDebut, dateFin, machinesByZone } = this.state;
        
        if (!dateDebut || !dateFin) {
            this.setState({ error: 'Veuillez sélectionner les dates de début et de fin.' });
            return;
        }

        this.setState({ loading: true, error: null, loadingPhase: 'Chargement des données...' });

        try {
            // Build shift-aware range for history: shift 1 of dateDebut starts at 21:55 of previous day
            const rangeStart = new Date(dateDebut);
            rangeStart.setDate(rangeStart.getDate() - 1);
            rangeStart.setHours(21, 55, 0, 0);
            const rangeEnd = new Date(dateFin);
            rangeEnd.setHours(21, 45, 0, 0);

            // Load history + series + capacité installée in parallel
            const [historyResponse, seriesResponse, capaciteResponse] = await Promise.all([
                axios.get('/api/etatMachineHistorique/listBetweenDate', {
                    params: {
                        dateDebut: rangeStart.toISOString(),
                        dateFin: rangeEnd.toISOString()
                    }
                }),
                axios.get('/api/planDeCharge/seriesForDateRange', {
                    params: { startDate: dateDebut, endDate: dateFin }
                }),
                axios.get('/api/capaciteInstallee/effectiveByDateRange', {
                    params: { startDate: dateDebut, endDate: dateFin }
                }).catch(() => ({ data: [] }))
            ]);

            const allHistoryList = historyResponse.data || [];
            const seriesData = seriesResponse.data || [];
            const capaciteInstalleeData = capaciteResponse.data || [];

            // Build statusGrid from allHistoryList on the frontend
            const statusGrid = this.buildStatusGridFromHistory(allHistoryList, machinesByZone, dateDebut, dateFin);

            // Compute aggregated cutting time and status from seriesData
            const { aggregatedCuttingTime, aggregatedCuttingTimeWithStatus, aggregatedByMachineType } = this.computeAggregationsFromSeries(seriesData, dateDebut, dateFin);

            // Pre-fetch non-imported charge for all date/shift combinations
            const nonImportedChargeByShift = {};
            const startD = new Date(dateDebut);
            const endD = new Date(dateFin);
            const shiftPromises = [];
            const shiftKeys = [];
            for (let d = new Date(startD); d <= endD; d.setDate(d.getDate() + 1)) {
                const dateStr = d.toISOString().split('T')[0];
                for (let s = 1; s <= 3; s++) {
                    shiftKeys.push(`${dateStr}_${s}`);
                    shiftPromises.push(
                        axios.get('/api/planDeCharge/nonImportedCharge', {
                            params: { date: dateStr, shift: String(s) }
                        }).catch(() => ({ data: { totalMinutes: 0, count: 0, details: [] } }))
                    );
                }
            }
            const shiftResults = await Promise.all(shiftPromises);
            shiftResults.forEach((res, i) => {
                nonImportedChargeByShift[shiftKeys[i]] = res.data;
            });

            this.setState({
                allHistoryList,
                seriesData,
                statusGrid,
                aggregatedCuttingTime,
                aggregatedCuttingTimeWithStatus,
                aggregatedByMachineType,
                capaciteInstalleeData,
                nonImportedChargeByShift,
                loading: false,
                loadingPhase: ''
            });
        } catch (error) {
            console.error('Error loading plan de charge:', error);
            this.setState({ 
                error: 'Erreur lors du chargement des données: ' + (error.response?.data?.message || error.message),
                loading: false,
                loadingPhase: ''
            });
        }
    };

    /**
     * Build statusGrid from allHistoryList on the frontend.
     * Logic: for each machine, for each date/shift, find if any history record
     * covers the shift midpoint — if so, use its codeEtat; otherwise default to 'M'.
     */
    buildStatusGridFromHistory = (allHistoryList, machinesByZone, dateDebut, dateFin) => {
        const grid = {};
        const SHIFT_DURATION_MINUTES = 460;

        // Build a map: machine -> list of history records
        const historyByMachine = {};
        allHistoryList.forEach(item => {
            if (!historyByMachine[item.machine]) historyByMachine[item.machine] = [];
            historyByMachine[item.machine].push(item);
        });

        // Iterate all machines
        Object.values(machinesByZone).forEach(machines => {
            machines.forEach(machine => {
                const machineName = machine.nom;
                grid[machineName] = {};

                const current = new Date(dateDebut);
                const end = new Date(dateFin);

                while (current <= end) {
                    const dateStr = current.toISOString().split('T')[0];
                    grid[machineName][dateStr] = {};

                    for (let shift = 1; shift <= 3; shift++) {
                        const shiftTimes = this.getShiftTimes(dateStr, shift);
                        // Midpoint of the shift
                        const midTime = new Date(shiftTimes.start.getTime() + (SHIFT_DURATION_MINUTES / 2) * 60000);

                        let statusCode = 'M'; // Default

                        const machineRecords = historyByMachine[machineName];
                        if (machineRecords) {
                            for (const record of machineRecords) {
                                const recordStart = new Date(record.startDate);
                                const recordEnd = record.endDate ? new Date(record.endDate) : null;

                                if (recordStart <= midTime && (recordEnd === null || recordEnd >= midTime)) {
                                    statusCode = record.codeEtat;
                                    break;
                                }
                            }
                        }

                        grid[machineName][dateStr][shift] = statusCode;
                    }

                    current.setDate(current.getDate() + 1);
                }
            });
        });

        return grid;
    };

    /**
     * Compute aggregatedCuttingTime and aggregatedCuttingTimeWithStatus from seriesData on the frontend.
     * seriesData: array of { machine, placement, dueDate, dueShift, effectiveCuttingTime, dateDebutCoupe, dateFinCoupe }
     */
    computeAggregationsFromSeries = (seriesData, dateDebut, dateFin) => {
        const aggregatedCuttingTime = {};
        const aggregatedCuttingTimeWithStatus = {};

        // Build machine type map: ProductionTable.nom -> machineType.name
        const machineTypeMap = {};
        const { machinesByZone } = this.state;
        Object.values(machinesByZone || {}).forEach(machines => {
            machines.forEach(m => {
                if (m.machineType?.name) machineTypeMap[m.nom] = m.machineType.name;
            });
        });

        // Also build aggregation by machineType directly (same logic as loadChargeDetails)
        // machineType -> date -> shift -> { total, cut, notCut, sr }
        const aggregatedByMachineType = {};

        seriesData.forEach(serie => {
            // Use tableCoupe as key to match ProductionTable.nom (used by getLoadIndicator/renderNextShiftPreparation)
            const machine = serie.tableCoupe || serie.machine;
            const dueDate = serie.dueDate;
            const dueShift = serie.dueShift;
            const ect = serie.effectiveCuttingTime;
            if (!machine || !dueDate || !dueShift || !ect || ect <= 0) return;

            // aggregatedCuttingTime: machine -> date -> shift -> totalCuttingTime
            if (!aggregatedCuttingTime[machine]) aggregatedCuttingTime[machine] = {};
            if (!aggregatedCuttingTime[machine][dueDate]) aggregatedCuttingTime[machine][dueDate] = {};
            aggregatedCuttingTime[machine][dueDate][dueShift] = (aggregatedCuttingTime[machine][dueDate][dueShift] || 0) + ect;

            // aggregatedCuttingTimeWithStatus: machine -> date -> shift -> { total, cut, notCut, sr }
            if (!aggregatedCuttingTimeWithStatus[machine]) aggregatedCuttingTimeWithStatus[machine] = {};
            if (!aggregatedCuttingTimeWithStatus[machine][dueDate]) aggregatedCuttingTimeWithStatus[machine][dueDate] = {};
            if (!aggregatedCuttingTimeWithStatus[machine][dueDate][dueShift]) {
                aggregatedCuttingTimeWithStatus[machine][dueDate][dueShift] = { total: 0, cut: 0, notCut: 0, sr: 0 };
            }
            const statusMap = aggregatedCuttingTimeWithStatus[machine][dueDate][dueShift];
            statusMap.total += ect;

            // Parse shift number for shift times
            const shiftNum = parseInt(dueShift, 10) || 1;
            const shiftTimes = this.getShiftTimes(dueDate, shiftNum);
            const shiftStart = shiftTimes.start;
            const shiftEnd = shiftTimes.end;

            const dateDebutCoupe = serie.dateDebutCoupe ? new Date(serie.dateDebutCoupe) : null;
            const dateFinCoupe = serie.dateFinCoupe ? new Date(serie.dateFinCoupe) : null;

            // SR: minutes worked BEFORE the shift started
            let srTime = 0;
            if (dateDebutCoupe && dateDebutCoupe < shiftStart) {
                const srEnd = (dateFinCoupe && dateFinCoupe < shiftStart) ? dateFinCoupe : shiftStart;
                srTime = Math.max(0, (srEnd - dateDebutCoupe) / 60000);
                if (srTime > ect) srTime = ect;
            }
            statusMap.sr += srTime;

            // Retard calculation (3 cases)
            // For future shifts (not yet started), "Non coupé" is NOT retard
            const now = new Date();
            const isShiftInFuture = now < shiftStart;
            let retardTime = 0;
            let cutTime = 0;
            if (!dateDebutCoupe) {
                if (isShiftInFuture) {
                    // Shift hasn't started: this is planned work, not retard
                    cutTime = ect;
                } else {
                    retardTime = ect;
                }
            } else if (dateDebutCoupe > shiftEnd) {
                retardTime = ect;
            } else if (!dateFinCoupe) {
                if (new Date() > shiftEnd) {
                    retardTime = ect;
                } else {
                    cutTime = ect;
                }
            } else if (dateFinCoupe > shiftEnd) {
                const partialRetardMs = dateFinCoupe - shiftEnd;
                retardTime = Math.min(partialRetardMs / 60000, ect);
                cutTime = ect - retardTime;
            } else {
                cutTime = ect;
            }
            statusMap.cut += cutTime;
            statusMap.notCut += retardTime;

            // Aggregate by machineType (same resolution as loadChargeDetails)
            const mtype = machineTypeMap[serie.tableCoupe] || machineTypeMap[serie.machine] || serie.tableCoupe || serie.machine || 'Unknown';
            if (!aggregatedByMachineType[mtype]) aggregatedByMachineType[mtype] = {};
            if (!aggregatedByMachineType[mtype][dueDate]) aggregatedByMachineType[mtype][dueDate] = {};
            if (!aggregatedByMachineType[mtype][dueDate][dueShift]) {
                aggregatedByMachineType[mtype][dueDate][dueShift] = { total: 0, cut: 0, notCut: 0, sr: 0 };
            }
            const typeMap = aggregatedByMachineType[mtype][dueDate][dueShift];
            typeMap.total += ect;
            typeMap.cut += cutTime;
            typeMap.notCut += retardTime;
            typeMap.sr += srTime;
        });

        return { aggregatedCuttingTime, aggregatedCuttingTimeWithStatus, aggregatedByMachineType };
    };

    handleDateChange = (field, value) => {
        this.setState({ [field]: value });
    };

    handleCellClick = async (machine, date, shift) => {
        try {
            // Get status history for this machine and date range
            const shiftTimes = this.getShiftTimes(date, shift);
            const response = await axios.get(`/api/etatMachineHistorique/machine/${machine}/byDateRange`, {
                params: {
                    startDate: shiftTimes.start.toISOString(),
                    endDate: shiftTimes.end.toISOString()
                }
            });

            // Load all history for this specific machine to show existing intervals
            const machineHistoryResponse = await axios.get(`/api/etatMachineHistorique/machine/${machine}`);

            // Auto-fill start and end dates based on shift times
            this.setState({
                showModal: true,
                selectedCell: { machine, date, shift },
                statusHistory: response.data || [],
                machineHistory: machineHistoryResponse.data || [],
                editingItem: null,
                modalData: {
                    machine,
                    startDate: this.toDateTimeLocal(shiftTimes.start),
                    endDate: this.toDateTimeLocal(shiftTimes.end),
                    codeEtat: 'M',
                    cause: '',
                    action: ''
                }
            });
        } catch (error) {
            console.error('Error loading status history:', error);
        }
    };

    getShiftTimes = (dateStr, shift) => {
        const date = new Date(dateStr);
        let start, end;

        switch (shift) {
            case 1:
                start = new Date(date);
                start.setDate(start.getDate() - 1);
                start.setHours(21, 55, 0, 0);
                end = new Date(date);
                end.setHours(5, 45, 0, 0);
                break;
            case 2:
                start = new Date(date);
                start.setHours(5, 55, 0, 0);
                end = new Date(date);
                end.setHours(13, 45, 0, 0);
                break;
            case 3:
                start = new Date(date);
                start.setHours(13, 55, 0, 0);
                end = new Date(date);
                end.setHours(21, 45, 0, 0);
                break;
            default:
                start = new Date(date);
                end = new Date(date);
        }

        return { start, end };
    };

    getConfiguredShiftMinutes = (capEntry) => {
        const configuredMinutes = Number(capEntry?.tempsTotalParMachine);
        return Number.isFinite(configuredMinutes) && configuredMinutes > 0 ? configuredMinutes : 460;
    };

    getShiftProductiveCapacityMinutes = (machineCount, capEntry) => {
        if (!machineCount || machineCount <= 0) return 0;

        // Raw capacity: efficiency now lives per-machine in the cutting time (numerator),
        // so the denominator is machines × configured opening time, no efficiency factor.
        const configuredMinutes = this.getConfiguredShiftMinutes(capEntry);

        return configuredMinutes * machineCount;
    };

    getCurrentShiftProductiveMinutesLeft = (dateStr, shift, machineCount, capEntry, now = new Date()) => {
        if (!machineCount || machineCount <= 0) return 0;

        const { start, end } = this.getShiftTimes(dateStr, shift);
        if (now >= end) return 0;

        const configuredMinutes = this.getConfiguredShiftMinutes(capEntry);
        const elapsedMinutes = now <= start ? 0 : Math.max(0, (now - start) / 60000);
        const elapsedRoundedMinutes = Math.min(configuredMinutes, Math.ceil(elapsedMinutes / 5) * 5);
        const remainingMinutesPerMachine = Math.max(0, configuredMinutes - elapsedRoundedMinutes);

        // Raw capacity (no efficiency factor — efficiency is per-machine in the cutting time).
        return remainingMinutesPerMachine * machineCount;
    };

    getGroupeForMachineType = (type) => {
        const coupeTypes = ['Lectra', 'Lectra IP6', 'Gerber'];
        const laserTypes = ['LASER-DXF'];

        if (coupeTypes.includes(type)) return 'Coupe';
        if (laserTypes.includes(type)) return 'Laser';

        return type;
    };

    getCapaciteInstalleeEntry = (dateStr, shift, groupe) => {
        const { capaciteInstalleeData } = this.state;

        if (!capaciteInstalleeData || capaciteInstalleeData.length === 0) return null;

        return capaciteInstalleeData.find(entry =>
            entry.dateProduction === dateStr && entry.shiftNumber === shift && entry.groupe === groupe
        ) || null;
    };

    countAvailableMachinesForTypes = (machineTypes, dateStr, shift) => {
        const { machinesByZone, statusGrid } = this.state;
        const typeSet = new Set(machineTypes);
        let machinesAvailable = 0;

        Object.values(machinesByZone).forEach(machines => {
            machines.forEach(machine => {
                if (!typeSet.has(machine.machineType?.name)) return;

                const status = statusGrid[machine.nom]?.[dateStr]?.[shift] || 'M';
                if (status === 'M') {
                    machinesAvailable++;
                }
            });
        });

        return machinesAvailable;
    };

    isCurrentShiftActive = (dateStr, shift, now = new Date()) => {
        const { currentShift, currentDate } = this.state;

        if (!currentShift || currentDate !== dateStr || currentShift.shift !== shift) {
            return false;
        }

        const { start, end } = this.getShiftTimes(dateStr, shift);
        return now >= start && now <= end;
    };

    getAdjustedRetardCarryoverForTypes = (machineTypes, sourceDate, sourceShift, groupeName = null, now = new Date()) => {
        const rawRetard = this.getRawRetardCarryoverForTypes(machineTypes, sourceDate, sourceShift);

        if (rawRetard <= 0) return 0;
        if (!this.isCurrentShiftActive(sourceDate, sourceShift, now)) return rawRetard;

        const machinesAvailable = this.countAvailableMachinesForTypes(machineTypes, sourceDate, sourceShift);
        const resolvedGroupe = groupeName || this.getGroupeForMachineType(machineTypes[0]);
        const capEntry = this.getCapaciteInstalleeEntry(sourceDate, sourceShift, resolvedGroupe);
        const remainingCapacity = this.getCurrentShiftProductiveMinutesLeft(sourceDate, sourceShift, machinesAvailable, capEntry, now);

        return Math.max(0, rawRetard - remainingCapacity);
    };

    getRawRetardCarryoverForTypes = (machineTypes, sourceDate, sourceShift) => {
        const { aggregatedByMachineType } = this.state;

        return machineTypes.reduce((total, machineType) => {
            return total + (aggregatedByMachineType?.[machineType]?.[sourceDate]?.[sourceShift]?.notCut || 0);
        }, 0);
    };

    handleModalChange = (field, value) => {
        this.setState(prevState => ({
            modalData: {
                ...prevState.modalData,
                [field]: value
            }
        }));
    };

    handleSaveStatus = async () => {
        const { modalData, editingItem, statusHistory, allHistoryList } = this.state;
        
        try {
            const payload = {
                machine: modalData.machine,
                startDate: modalData.startDate,
                endDate: modalData.endDate || null,
                codeEtat: modalData.codeEtat,
                cause: modalData.cause,
                action: modalData.action
            };

            // Check for overlapping intervals
            const newStart = new Date(modalData.startDate);
            const newEnd = modalData.endDate ? new Date(modalData.endDate) : null;
            
            // Get all records for this machine to check overlaps
            const machineRecords = allHistoryList.filter(item => 
                item.machine === modalData.machine && 
                (editingItem ? item.id !== editingItem.id : true)
            );
            
            for (const record of machineRecords) {
                const recordStart = new Date(record.startDate);
                const recordEnd = record.endDate ? new Date(record.endDate) : null;
                
                // Check for overlap
                const overlaps = this.checkOverlap(newStart, newEnd, recordStart, recordEnd);
                if (overlaps) {
                    alert(`Erreur: L'intervalle chevauche avec un enregistrement existant.\n` +
                          `Conflit avec: ${this.formatDateTimeFR(record.startDate)} - ${record.endDate ? this.formatDateTimeFR(record.endDate) : 'En cours'}`);
                    return;
                }
            }

            if (editingItem) {
                // Update existing
                await axios.put(`/api/etatMachineHistorique/${editingItem.id}`, payload);
            } else {
                // Create new
                await axios.post('/api/etatMachineHistorique', payload);
            }
            
            this.setState({ showModal: false, modalData: null, editingItem: null });
            this.loadData();
        } catch (error) {
            console.error('Error saving status:', error);
            alert('Erreur lors de l\'enregistrement: ' + (error.response?.data?.message || error.message));
        }
    };

    /**
     * Check if two date intervals overlap.
     */
    checkOverlap = (start1, end1, start2, end2) => {
        // If either interval has no end date (ongoing), treat it as extending to infinity
        const effectiveEnd1 = end1 || new Date('9999-12-31');
        const effectiveEnd2 = end2 || new Date('9999-12-31');
        
        // Intervals overlap if start1 < end2 AND start2 < end1
        return start1 < effectiveEnd2 && start2 < effectiveEnd1;
    };

    handleEditStatus = (item) => {
        this.setState({
            editingItem: item,
            modalData: {
                machine: item.machine,
                startDate: this.toDateTimeLocal(item.startDate),
                endDate: item.endDate ? this.toDateTimeLocal(item.endDate) : '',
                codeEtat: item.codeEtat,
                cause: item.cause || '',
                action: item.action || ''
            }
        });
    };

    handleDeleteStatus = async (id) => {
        if (!window.confirm('Êtes-vous sûr de vouloir supprimer cet enregistrement?')) {
            return;
        }
        
        try {
            await axios.delete(`/api/etatMachineHistorique/${id}`);
            this.loadData();
            
            // Refresh modal data if open
            if (this.state.selectedCell) {
                this.handleCellClick(
                    this.state.selectedCell.machine,
                    this.state.selectedCell.date,
                    this.state.selectedCell.shift
                );
            }
        } catch (error) {
            console.error('Error deleting status:', error);
            alert('Erreur lors de la suppression: ' + (error.response?.data?.message || error.message));
        }
    };

    handleCancelEdit = () => {
        const { selectedCell } = this.state;
        if (selectedCell) {
            const shiftTimes = this.getShiftTimes(selectedCell.date, selectedCell.shift);
            this.setState({
                editingItem: null,
                modalData: {
                    machine: selectedCell.machine,
                    startDate: this.toDateTimeLocal(shiftTimes.start),
                    endDate: this.toDateTimeLocal(shiftTimes.end),
                    codeEtat: 'M',
                    cause: '',
                    action: ''
                }
            });
        } else {
            this.setState({ editingItem: null });
        }
    };

    handleCloseStatus = async (id) => {
        try {
            await axios.put(`/api/etatMachineHistorique/${id}/close`);
            this.loadData();
            
            // Refresh modal data
            if (this.state.selectedCell) {
                this.handleCellClick(
                    this.state.selectedCell.machine,
                    this.state.selectedCell.date,
                    this.state.selectedCell.shift
                );
            }
        } catch (error) {
            console.error('Error closing status:', error);
        }
    };

    handleRecalculate = async (date, shift) => {
        this.setState({ calculating: true });
        
        try {
            await axios.post('/api/planDeCharge/calculate', null, {
                params: { date, shiftNumber: shift }
            });
            
            this.loadData();
        } catch (error) {
            console.error('Error recalculating:', error);
            alert('Erreur lors du recalcul: ' + (error.response?.data?.message || error.message));
        } finally {
            this.setState({ calculating: false });
        }
    };

    getDateRange = () => {
        const { dateDebut, dateFin } = this.state;
        const dates = [];
        let current = new Date(dateDebut);
        const end = new Date(dateFin);

        while (current <= end) {
            dates.push(current.toISOString().split('T')[0]);
            current.setDate(current.getDate() + 1);
        }

        return dates;
    };

    getMachineTypeColor = (machineType) => {
        if (!machineType) return '#ffffff';
        return MACHINE_TYPE_COLORS[machineType] || '#ffffff';
    };

    formatDate = (dateStr) => {
        const date = new Date(dateStr);
        return date.toLocaleDateString('fr-FR', { 
            weekday: 'short', 
            day: '2-digit', 
            month: '2-digit' 
        });
    };

    exportCSV = () => {
        const { machinesByZone, statusGrid } = this.state;
        const dates = this.getDateRange();
        
        let csv = 'Zone,Machine,';
        dates.forEach(date => {
            csv += `${date} S1,${date} S2,${date} S3,`;
        });
        csv += '\n';

        Object.entries(machinesByZone).forEach(([zone, machines]) => {
            machines.forEach(machine => {
                csv += `${zone},${machine.nom},`;
                dates.forEach(date => {
                    for (let shift = 1; shift <= 3; shift++) {
                        const status = statusGrid[machine.nom]?.[date]?.[shift] || 'M';
                        csv += `${status},`;
                    }
                });
                csv += '\n';
            });
        });

        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `plan_de_charge_${this.state.dateDebut}_${this.state.dateFin}.csv`;
        link.click();
    };

    /**
     * Export the detailed series table to CSV.
     */
    exportSeriesCSV = (series, shiftInfo) => {
        if (!series || series.length === 0) return;

        const headers = ['Série', 'Séquence', 'CMS Id', 'Plan ID', 'Placement', 'Type Machine', 'Table Coupe', 'Temps (min)', 'Source',
            'Statut Matelassage', 'Début Coupe', 'Fin Coupe', 'Statut', 'Retard (min)', 'SR (min)', 'Temps Changement Série'];
        let csv = '\uFEFF' + headers.join(';') + '\n';

        series.forEach(serie => {
            const status = serie.isPartialRetard ? 'Retard Partiel' :
                serie.isCut ? 'Coupé' :
                serie.isCutting ? 'En Cours' : 'Non Coupé';
            const row = [
                serie.serie || '',
                serie.sequence || '',
                serie.cmsId || '',
                serie.cuttingPlanId || '',
                serie.placement || '',
                serie.machine || 'Unknown',
                serie.tableCoupe || '',
                serie.effectiveCuttingTime?.toFixed(2) || '',
                serie.cuttingTimeSource || '',
                serie.isPrepared ? 'Prêt' : 'En attente',
                serie.dateDebutCoupe ? this.formatDateTimeFR(serie.dateDebutCoupe) : '',
                serie.dateFinCoupe ? this.formatDateTimeFR(serie.dateFinCoupe) : '',
                status,
                serie.retardMinutes > 0 ? serie.retardMinutes.toFixed(0) : '',
                serie.srMinutes > 0 ? Math.round(serie.srMinutes) : '',
                serie.tempsChangementSerie?.toFixed(0) || ''
            ];
            csv += row.join(';') + '\n';
        });

        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `series_${shiftInfo?.date || 'export'}_shift${shiftInfo?.shift || ''}.csv`;
        link.click();
        URL.revokeObjectURL(link.href);
    };

    /**
     * Export the detailed series table to Excel (XLSX-like via HTML table).
     */
    exportSeriesExcel = (series, shiftInfo) => {
        if (!series || series.length === 0) return;

        let html = '<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:spreadsheet" xmlns="http://www.w3.org/TR/REC-html40">';
        html += '<head><meta charset="UTF-8"></head><body>';
        html += '<table border="1">';
        html += '<tr><th>Série</th><th>Séquence</th><th>CMS Id</th><th>Plan ID</th><th>Placement</th><th>Type Machine</th><th>Table Coupe</th>';
        html += '<th>Temps (min)</th><th>Source</th><th>Statut Matelassage</th>';
        html += '<th>Début Coupe</th><th>Fin Coupe</th><th>Statut</th><th>Retard (min)</th><th>SR (min)</th><th>Temps Changement Série</th></tr>';

        series.forEach(serie => {
            const status = serie.isPartialRetard ? 'Retard Partiel' :
                serie.isCut ? 'Coupé' :
                serie.isCutting ? 'En Cours' : 'Non Coupé';
            html += '<tr>';
            html += `<td>${serie.serie || ''}</td>`;
            html += `<td>${serie.sequence || ''}</td>`;
            html += `<td>${serie.cmsId || ''}</td>`;
            html += `<td>${serie.cuttingPlanId || ''}</td>`;
            html += `<td>${serie.placement || ''}</td>`;
            html += `<td>${serie.machine || 'Unknown'}</td>`;
            html += `<td>${serie.tableCoupe || ''}</td>`;
            html += `<td>${serie.effectiveCuttingTime?.toFixed(2) || ''}</td>`;
            html += `<td>${serie.cuttingTimeSource || ''}</td>`;
            html += `<td>${serie.isPrepared ? 'Prêt' : 'En attente'}</td>`;
            html += `<td>${serie.dateDebutCoupe ? this.formatDateTimeFR(serie.dateDebutCoupe) : ''}</td>`;
            html += `<td>${serie.dateFinCoupe ? this.formatDateTimeFR(serie.dateFinCoupe) : ''}</td>`;
            html += `<td>${status}</td>`;
            html += `<td>${serie.retardMinutes > 0 ? serie.retardMinutes.toFixed(0) : ''}</td>`;
            html += `<td>${serie.srMinutes > 0 ? Math.round(serie.srMinutes) : ''}</td>`;
            html += `<td>${serie.tempsChangementSerie?.toFixed(0) || ''}</td>`;
            html += '</tr>';
        });

        html += '</table></body></html>';

        const blob = new Blob([html], { type: 'application/vnd.ms-excel;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `series_${shiftInfo?.date || 'export'}_shift${shiftInfo?.shift || ''}.xls`;
        link.click();
        URL.revokeObjectURL(link.href);
    };

    /**
     * Export non-imported charge details to CSV.
     */
    exportNonImportedCSV = (nonImportedCharge, shiftInfo) => {
        if (!nonImportedCharge || !nonImportedCharge.details || nonImportedCharge.details.length === 0) return;

        const headers = ['WorkOrder', 'Item', 'Description', 'Quantite', 'Unit_Time_min', 'Total_Time_min', 'Source'];
        let csv = '\uFEFF' + headers.join(';') + '\n';

        nonImportedCharge.details.forEach(d => {
            const row = [
                d.idDemande || '',
                d.partNumber || '',
                d.description || '',
                d.quantity || '',
                d.minutesPerPiece?.toFixed(2) || '',
                d.estimatedMinutes?.toFixed(0) || '',
                d.source || ''
            ];
            csv += row.join(';') + '\n';
        });

        csv += `TOTAL;;;;;${nonImportedCharge.totalMinutes?.toFixed(0) || 0};${nonImportedCharge.count || 0} OF\n`;

        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `work_orders_non_importes_${shiftInfo?.date || 'export'}_shift${shiftInfo?.shift || ''}.csv`;
        link.click();
        URL.revokeObjectURL(link.href);
    };

    /**
     * Export non-imported charge details to Excel (XLSX-like via HTML table).
     */
    exportNonImportedExcel = (nonImportedCharge, shiftInfo) => {
        if (!nonImportedCharge || !nonImportedCharge.details || nonImportedCharge.details.length === 0) return;

        let html = '<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:spreadsheet" xmlns="http://www.w3.org/TR/REC-html40">';
        html += '<head><meta charset="UTF-8"></head><body>';
        html += '<table border="1">';
        html += '<tr><th>WorkOrder</th><th>Item</th><th>Description</th><th>Quantite</th><th>Unit Time (min)</th><th>Total Time (min)</th><th>Source</th></tr>';

        nonImportedCharge.details.forEach(d => {
            html += '<tr>';
            html += `<td>${d.idDemande || ''}</td>`;
            html += `<td>${d.partNumber || ''}</td>`;
            html += `<td>${d.description || ''}</td>`;
            html += `<td>${d.quantity || ''}</td>`;
            html += `<td>${d.minutesPerPiece?.toFixed(2) || ''}</td>`;
            html += `<td>${d.estimatedMinutes?.toFixed(0) || ''}</td>`;
            html += `<td>${d.source || ''}</td>`;
            html += '</tr>';
        });

        html += `<tr style="font-weight:bold"><td colspan="5" align="right">Total Charge Non Importee</td><td>${nonImportedCharge.totalMinutes?.toFixed(0) || 0}</td><td>${nonImportedCharge.count || 0} OF</td></tr>`;
        html += '</table></body></html>';

        const blob = new Blob([html], { type: 'application/vnd.ms-excel;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `work_orders_non_importes_${shiftInfo?.date || 'export'}_shift${shiftInfo?.shift || ''}.xls`;
        link.click();
        URL.revokeObjectURL(link.href);
    };

    renderLegend = () => {
        const { showLegend } = this.state;

        return (
            <div className="pdc-legend-container">
                <div className="pdc-legend-header" onClick={() => this.setState({ showLegend: !showLegend })}>
                    <FontAwesomeIcon icon={faInfoCircle} style={{ marginRight: '8px' }} />
                    Légende
                </div>
                
                {showLegend && (
                    <div className="pdc-legend-content">
                        <div className="pdc-legend-section">
                            <h4>Codes Status</h4>
                            <div className="pdc-legend-grid">
                                {Object.entries(STATUS_CONFIG).map(([code, config]) => (
                                    <div key={code} className="pdc-legend-item">
                                        <span 
                                            className="pdc-legend-color"
                                            style={{ 
                                                backgroundColor: config.color,
                                                color: config.textColor
                                            }}
                                        >
                                            {code}
                                        </span>
                                        <span className="pdc-legend-name">{config.name}</span>
                                    </div>
                                ))}
                            </div>
                        </div>

                        <div className="pdc-legend-section">
                            <h4>Types de Machine</h4>
                            <div className="pdc-legend-grid">
                                {Object.entries(MACHINE_TYPE_COLORS).map(([type, color]) => (
                                    <div key={type} className="pdc-legend-item">
                                        <span 
                                            className="pdc-legend-color"
                                            style={{ backgroundColor: color }}
                                        >
                                            &nbsp;
                                        </span>
                                        <span className="pdc-legend-name">{type}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                )}
            </div>
        );
    };

    renderLoadSummary = () => {
        const { aggregatedByMachineType, machinesByZone, statusGrid, capaciteInstalleeData, currentShift, currentDate } = this.state;
        if (!aggregatedByMachineType || Object.keys(aggregatedByMachineType).length === 0) {
            return null;
        }

        const COUPE_TYPES = ['Lectra', 'Lectra IP6', 'Gerber'];
        const LASER_TYPES = ['LASER-DXF'];

        // Helper: lookup capacité installée for a date/shift/groupe
        const getCapaciteInstallee = (date, shift, groupe) => {
            if (!capaciteInstalleeData || capaciteInstalleeData.length === 0) return null;
            return capaciteInstalleeData.find(c =>
                c.dateProduction === date && c.shiftNumber === shift && c.groupe === groupe
            ) || null;
        };

        // Build machine type map for each physical machine
        const machineTypeMap = {};
        Object.values(machinesByZone || {}).forEach(machines => {
            machines.forEach(m => {
                if (m.machineType?.name) machineTypeMap[m.nom] = m.machineType.name;
            });
        });

        // Collect all unique date/shift combinations from aggregatedByMachineType
        const dateShiftSet = new Set();
        Object.values(aggregatedByMachineType).forEach(dateData => {
            Object.entries(dateData).forEach(([date, shiftData]) => {
                Object.keys(shiftData).forEach(shift => {
                    dateShiftSet.add(`${date}_${shift}`);
                });
            });
        });

        // Helper: count available machines for a group in a given date/shift
        const countAvailableMachines = (typeList, date, shift) => {
            let count = 0;
            Object.values(machinesByZone || {}).forEach(machines => {
                machines.forEach(machine => {
                    if (typeList.includes(machine.machineType?.name)) {
                        const status = statusGrid[machine.nom]?.[date]?.[shift] || 'M';
                        if (status === 'M') {
                            count++;
                        }
                    }
                });
            });
            return count;
        };

        // Helper: compute indicators for a group (typeList) at a given date/shift
        const computeGroupIndicators = (typeList, groupeName, date, shift) => {
            let totalCuttingTime = 0;
            let srTime = 0;
            let retThis = 0;

            typeList.forEach(ctype => {
                const typeData = aggregatedByMachineType?.[ctype]?.[date]?.[shift];
                if (typeData) {
                    totalCuttingTime += typeData.total || 0;
                    srTime += typeData.sr || 0;
                    retThis += typeData.notCut || 0;
                }
            });

            // Non-imported charge from Order_Schedule (only for Coupe group)
            let nonImportedMinutes = 0;
            if (groupeName === 'Coupe') {
                const nicKey = `${date}_${shift}`;
                const nicData = this.state.nonImportedChargeByShift?.[nicKey];
                nonImportedMinutes = nicData?.totalMinutes || 0;
            }
            const seriesCuttingTime = totalCuttingTime;
            totalCuttingTime += nonImportedMinutes;

            // Previous shift for retard prev
            let prevShift = shift - 1;
            let prevDate = date;
            if (shift === 1) {
                prevShift = 3;
                const d = new Date(date);
                d.setDate(d.getDate() - 1);
                prevDate = d.toISOString().split('T')[0];
            }

            const retPrevRaw = this.getRawRetardCarryoverForTypes(typeList, prevDate, prevShift);
            const retPrev = this.getAdjustedRetardCarryoverForTypes(typeList, prevDate, prevShift, groupeName);

            const machinesCount = countAvailableMachines(typeList, date, shift);
            const capEntryForGroup = getCapaciteInstallee(date, shift, groupeName);
            const availableTime = this.getShiftProductiveCapacityMinutes(machinesCount, capEntryForGroup);
            const installedCount = capEntryForGroup ? (capEntryForGroup.capaciteInstallee || 0) : 0;
            const installedCapacityMinutes = installedCount > 0
                ? this.getShiftProductiveCapacityMinutes(installedCount, capEntryForGroup)
                : 0;

            // Determine if this is current shift or next shift
            const nextShiftInfo = this.getNextShiftInfo();
            const isCurrentShift = currentShift && currentDate === date && currentShift.shift === shift;
            const isNextShift = nextShiftInfo && nextShiftInfo.date === date && nextShiftInfo.shift === shift;
            const showTimeLeft = isCurrentShift || isNextShift;

            // Keep raw retard before adjustment
            const retThisRaw = retThis;
            let tempsRestantMin = 0;
            let retThisAdjusted = retThisRaw;

            if (showTimeLeft && machinesCount > 0) {
                if (isCurrentShift) {
                    tempsRestantMin = this.getCurrentShiftProductiveMinutesLeft(date, shift, machinesCount, capEntryForGroup);
                } else if (isNextShift) {
                    tempsRestantMin = this.getShiftProductiveCapacityMinutes(machinesCount, capEntryForGroup);
                }
                retThisAdjusted = Math.max(0, retThisRaw - tempsRestantMin);
            }

            const chgPct = availableTime > 0 ? (totalCuttingTime / availableTime) * 100 : 0;
            const chgPctInstalled = installedCapacityMinutes > 0
                ? (totalCuttingTime / installedCapacityMinutes) * 100
                : 0;
            const srPct = availableTime > 0 ? (srTime / availableTime) * 100 : 0;
            const retPrevRawPct = availableTime > 0 ? (retPrevRaw / availableTime) * 100 : 0;
            const retPrevPct = availableTime > 0 ? (retPrev / availableTime) * 100 : 0;
            const sumPct = chgPct - srPct + retPrevPct;
            const retThisRawPct = availableTime > 0 ? (retThisRaw / availableTime) * 100 : 0;
            const retThisPct = availableTime > 0 ? (retThisAdjusted / availableTime) * 100 : 0;

            return {
                machinesCount, availableTime,
                installedCount, installedCapacityMinutes, chgPctInstalled,
                totalCuttingTime, chgPct,
                seriesCuttingTime, nonImportedMinutes,
                srTime, srPct,
                retPrevRaw, retPrevRawPct,
                retPrev, retPrevPct,
                sumPct,
                retThis: retThisAdjusted, retThisPct,
                retThisRaw,
                retThisRawPct,
                tempsRestantMin: showTimeLeft ? tempsRestantMin : null,
                showTimeLeft,
                hasData: totalCuttingTime > 0 || retPrevRaw > 0 || retPrev > 0 || srTime > 0 || retThisAdjusted > 0 || retThisRaw > 0 || nonImportedMinutes > 0
            };
        };

        // Build sorted list of date/shift entries
        const sortedDateShifts = Array.from(dateShiftSet)
            .map(key => {
                const [date, shift] = key.split('_');
                return { date, shift: parseInt(shift) };
            })
            .sort((a, b) => {
                const dateCompare = a.date.localeCompare(b.date);
                return dateCompare !== 0 ? dateCompare : a.shift - b.shift;
            });

        if (sortedDateShifts.length === 0) {
            return null;
        }

        // Render helper for one sub-row
        const renderSubRow = (item, groupLabel, ind, bgColor) => (
            <tr
                key={`${item.date}_${item.shift}_${groupLabel}`}
                className={`${ind.sumPct > 100 ? 'overload' : ''} pdc-summary-row-clickable`}
                onClick={() => this.loadChargeDetails(item.date, item.shift.toString())}
                style={{ cursor: 'pointer', backgroundColor: bgColor }}
            >
                <td style={{ fontWeight: 'bold' }}>{groupLabel}</td>
                <td style={{ fontWeight: 'bold' }}>{ind.machinesCount}</td>
                <td style={{ fontWeight: 'bold' }}>{ind.totalCuttingTime.toFixed(0)}</td>
                <td style={{
                    color: ind.chgPct > 100 ? '#ff0000' : ind.chgPct > 80 ? '#ffc000' : '#00b050',
                    fontWeight: 'bold'
                }}>
                    {ind.chgPct.toFixed(1)}%
                </td>
                <td style={{ color: ind.srTime > 0 ? '#2196f3' : 'inherit' }}>
                    {ind.srTime > 0 ? ind.srTime.toFixed(0) : '-'}
                </td>
                <td style={{ color: ind.srPct > 0 ? '#2196f3' : 'inherit' }}>
                    {ind.srPct > 0 ? ind.srPct.toFixed(1) + '%' : '-'}
                </td>
                <td style={{ color: ind.retPrev > 0 ? '#d32f2f' : 'inherit' }}>
                    {ind.retPrev > 0 ? ind.retPrev.toFixed(0) : '-'}
                </td>
                <td style={{ color: ind.retPrevPct > 0 ? '#d32f2f' : 'inherit' }}>
                    {ind.retPrevPct > 0 ? ind.retPrevPct.toFixed(1) + '%' : '-'}
                </td>
                <td style={{
                    color: ind.sumPct > 100 ? '#ff0000' : ind.sumPct > 80 ? '#ffc000' : '#00b050',
                    fontWeight: 'bold'
                }}>
                    {ind.sumPct.toFixed(1)}%
                </td>
                <td style={{ color: ind.retThis > 0 ? '#ff6600' : 'inherit' }}>
                    {ind.retThis > 0 ? ind.retThis.toFixed(0) : '-'}
                </td>
                <td style={{ color: ind.retThisPct > 0 ? '#ff6600' : 'inherit' }}>
                    {ind.retThisPct > 0 ? ind.retThisPct.toFixed(1) + '%' : '-'}
                </td>
                <td onClick={e => e.stopPropagation()}>
                    <button
                        className="pdc-btn-details"
                        onClick={() => this.loadChargeDetails(item.date, item.shift.toString())}
                        title="Voir les détails des séries"
                    >
                        <FontAwesomeIcon icon={faList} />
                    </button>
                </td>
            </tr>
        );

        return (
            <div className="pdc-load-summary">
                <h3>
                    <FontAwesomeIcon icon={faChartBar} style={{ marginRight: '8px' }} />
                    Résumé de Charge (basé sur les temps de coupe)
                </h3>
                <table className="pdc-summary-table">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Shift</th>
                            <th>Groupe</th>
                            <th title="Capacité installée (machines cibles)">Cap. Inst.</th>
                            <th>Cap</th>
                            <th title="Charge non importée (Order_Schedule status='F')" style={{ color: '#9c27b0' }}>Chg NP (min)</th>
                            <th>Chg (min)</th>
                            <th>Chg %</th>
                            <th title="Chg % basé sur la capacité installée × efficience (référence théorique)" style={{ color: '#6a1b9a' }}>Chg % cap.inst.</th>
                            <th title="SR: séries travaillées avant le shift">SR (min)</th>
                            <th>SR %</th>
                            <th title="Retard du shift précédent">Ret prev (min)</th>
                            <th>Ret prev %</th>
                            <th title="Charge réelle = Chg - SR + Ret prev">Sum %</th>
                            <th title="Retard brut de ce shift avant déduction du temps restant">Ret→next brut (min)</th>
                            <th>Ret→next brut %</th>
                            <th title="Temps productif restant (shift actuel/suivant)" style={{ color: '#00796b' }}>T. Rest (min)</th>
                            <th title="Retard ajusté = Ret→next - Temps Restant" style={{ color: '#e65100' }}>Ret adj (min)</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {sortedDateShifts.map(item => {
                            const coupeInd = computeGroupIndicators(COUPE_TYPES, 'Coupe', item.date, item.shift);
                            const laserInd = computeGroupIndicators(LASER_TYPES, 'Laser', item.date, item.shift);
                            const coupeCap = getCapaciteInstallee(item.date, item.shift, 'Coupe');
                            const laserCap = getCapaciteInstallee(item.date, item.shift, 'Laser');
                            const isSummaryCurrentShift = currentShift && currentDate === item.date && currentShift.shift === item.shift;
                            return (
                                <React.Fragment key={`${item.date}_${item.shift}`}>
                                    {/* Coupe sub-row with rowSpan on Date and Shift */}
                                    <tr
                                        className={`${coupeInd.sumPct > 100 ? 'overload' : ''} pdc-summary-row-clickable${isSummaryCurrentShift ? ' pdc-summary-current-shift-top' : ''}`}
                                        onClick={() => this.loadChargeDetails(item.date, item.shift.toString())}
                                        style={{ cursor: 'pointer', backgroundColor: '#f0f8f0' }}
                                    >
                                        <td
                                            rowSpan={2}
                                            className={isSummaryCurrentShift ? 'pdc-summary-current-shift-spanned pdc-summary-current-shift-left' : ''}
                                            style={{ verticalAlign: 'middle', fontWeight: 'bold' }}
                                        >
                                            {item.date}
                                        </td>
                                        <td
                                            rowSpan={2}
                                            className={isSummaryCurrentShift ? 'pdc-summary-current-shift-spanned' : ''}
                                            style={{ verticalAlign: 'middle' }}
                                        >
                                            Shift {item.shift}
                                        </td>
                                        <td style={{ fontWeight: 'bold', color: '#2e7d32' }}>Coupe</td>
                                        <td style={{ fontWeight: 'bold', color: '#6a1b9a' }}>
                                            {coupeCap ? coupeCap.capaciteInstallee : '-'}
                                        </td>
                                        <td style={{ fontWeight: 'bold' }}>{coupeInd.machinesCount}</td>
                                        <td style={{ fontWeight: 'bold', color: '#9c27b0' }}>
                                            {coupeInd.nonImportedMinutes > 0 ? coupeInd.nonImportedMinutes.toFixed(0) : '-'}
                                        </td>
                                        <td style={{ fontWeight: 'bold' }}>{coupeInd.totalCuttingTime.toFixed(0)}</td>
                                        <td style={{
                                            color: coupeInd.chgPct > 100 ? '#ff0000' : coupeInd.chgPct > 80 ? '#ffc000' : '#00b050',
                                            fontWeight: 'bold'
                                        }}>
                                            {coupeInd.chgPct.toFixed(1)}%
                                        </td>
                                        <td
                                            title={coupeInd.installedCapacityMinutes > 0
                                                ? `Temps charge / (cap.installée ${coupeInd.installedCount} × ${coupeInd.installedCapacityMinutes.toFixed(0)} min)`
                                                : 'Capacité installée non définie'}
                                            style={{
                                                color: coupeInd.installedCapacityMinutes === 0
                                                    ? '#9e9e9e'
                                                    : coupeInd.chgPctInstalled > 100 ? '#ff0000'
                                                        : coupeInd.chgPctInstalled > 80 ? '#ffc000'
                                                        : '#6a1b9a',
                                                fontWeight: 'bold'
                                            }}
                                        >
                                            {coupeInd.installedCapacityMinutes > 0 ? coupeInd.chgPctInstalled.toFixed(1) + '%' : '-'}
                                        </td>
                                        <td style={{ color: coupeInd.srTime > 0 ? '#2196f3' : 'inherit' }}>
                                            {coupeInd.srTime > 0 ? coupeInd.srTime.toFixed(0) : '-'}
                                        </td>
                                        <td style={{ color: coupeInd.srPct > 0 ? '#2196f3' : 'inherit' }}>
                                            {coupeInd.srPct > 0 ? coupeInd.srPct.toFixed(1) + '%' : '-'}
                                        </td>
                                        <td style={{ color: coupeInd.retPrevRaw > 0 ? '#d32f2f' : 'inherit' }}>
                                            {coupeInd.retPrevRaw > 0 ? coupeInd.retPrev.toFixed(0) : '-'}
                                        </td>
                                        <td style={{ color: coupeInd.retPrevRaw > 0 ? '#d32f2f' : 'inherit' }}>
                                            {coupeInd.retPrevRaw > 0 ? coupeInd.retPrevPct.toFixed(1) + '%' : '-'}
                                        </td>
                                        <td style={{
                                            color: coupeInd.sumPct > 100 ? '#ff0000' : coupeInd.sumPct > 80 ? '#ffc000' : '#00b050',
                                            fontWeight: 'bold'
                                        }}>
                                            {coupeInd.sumPct.toFixed(1)}%
                                        </td>
                                        <td style={{ color: coupeInd.retThisRaw > 0 ? '#ff6600' : 'inherit' }}>
                                            {coupeInd.retThisRaw > 0 ? coupeInd.retThisRaw.toFixed(0) : '-'}
                                        </td>
                                        <td style={{ color: coupeInd.retThisRaw > 0 ? '#ff6600' : 'inherit' }}>
                                            {coupeInd.retThisRaw > 0 ? coupeInd.retThisRawPct.toFixed(1) + '%' : '-'}
                                        </td>
                                        <td style={{ color: '#009688', fontWeight: 'bold' }}>
                                            {coupeInd.showTimeLeft ? coupeInd.tempsRestantMin.toFixed(0) : '-'}
                                        </td>
                                        <td style={{ color: coupeInd.retThisRaw > 0 ? '#ff6600' : 'inherit', fontWeight: 'bold' }}>
                                            {coupeInd.retThisRaw > 0 ? coupeInd.retThis.toFixed(0) : '-'}
                                        </td>
                                        <td
                                            rowSpan={2}
                                            className={isSummaryCurrentShift ? 'pdc-summary-current-shift-spanned pdc-summary-current-shift-right' : ''}
                                            style={{ verticalAlign: 'middle' }}
                                            onClick={e => e.stopPropagation()}
                                        >
                                            <button
                                                className="pdc-btn-details"
                                                onClick={() => this.loadChargeDetails(item.date, item.shift.toString())}
                                                title="Voir les détails des séries"
                                            >
                                                <FontAwesomeIcon icon={faList} />
                                            </button>
                                        </td>
                                    </tr>
                                    {/* Laser sub-row */}
                                    <tr
                                        className={`${laserInd.sumPct > 100 ? 'overload' : ''} pdc-summary-row-clickable${isSummaryCurrentShift ? ' pdc-summary-current-shift-bottom' : ''}`}
                                        onClick={() => this.loadChargeDetails(item.date, item.shift.toString())}
                                        style={{ cursor: 'pointer', backgroundColor: '#fff8e1' }}
                                    >
                                        <td style={{ fontWeight: 'bold', color: '#e65100' }}>Laser</td>
                                        <td style={{ fontWeight: 'bold', color: '#6a1b9a' }}>
                                            {laserCap ? laserCap.capaciteInstallee : '-'}
                                        </td>
                                        <td style={{ fontWeight: 'bold' }}>{laserInd.machinesCount}</td>
                                        <td style={{ fontWeight: 'bold', color: '#9c27b0' }}>
                                            {laserInd.nonImportedMinutes > 0 ? laserInd.nonImportedMinutes.toFixed(0) : '-'}
                                        </td>
                                        <td style={{ fontWeight: 'bold' }}>{laserInd.totalCuttingTime.toFixed(0)}</td>
                                        <td style={{
                                            color: laserInd.chgPct > 100 ? '#ff0000' : laserInd.chgPct > 80 ? '#ffc000' : '#00b050',
                                            fontWeight: 'bold'
                                        }}>
                                            {laserInd.chgPct.toFixed(1)}%
                                        </td>
                                        <td
                                            title={laserInd.installedCapacityMinutes > 0
                                                ? `Temps charge / (cap.installée ${laserInd.installedCount} × ${laserInd.installedCapacityMinutes.toFixed(0)} min)`
                                                : 'Capacité installée non définie'}
                                            style={{
                                                color: laserInd.installedCapacityMinutes === 0
                                                    ? '#9e9e9e'
                                                    : laserInd.chgPctInstalled > 100 ? '#ff0000'
                                                        : laserInd.chgPctInstalled > 80 ? '#ffc000'
                                                        : '#6a1b9a',
                                                fontWeight: 'bold'
                                            }}
                                        >
                                            {laserInd.installedCapacityMinutes > 0 ? laserInd.chgPctInstalled.toFixed(1) + '%' : '-'}
                                        </td>
                                        <td style={{ color: laserInd.srTime > 0 ? '#2196f3' : 'inherit' }}>
                                            {laserInd.srTime > 0 ? laserInd.srTime.toFixed(0) : '-'}
                                        </td>
                                        <td style={{ color: laserInd.srPct > 0 ? '#2196f3' : 'inherit' }}>
                                            {laserInd.srPct > 0 ? laserInd.srPct.toFixed(1) + '%' : '-'}
                                        </td>
                                        <td style={{ color: laserInd.retPrevRaw > 0 ? '#d32f2f' : 'inherit' }}>
                                            {laserInd.retPrevRaw > 0 ? laserInd.retPrev.toFixed(0) : '-'}
                                        </td>
                                        <td style={{ color: laserInd.retPrevRaw > 0 ? '#d32f2f' : 'inherit' }}>
                                            {laserInd.retPrevRaw > 0 ? laserInd.retPrevPct.toFixed(1) + '%' : '-'}
                                        </td>
                                        <td style={{
                                            color: laserInd.sumPct > 100 ? '#ff0000' : laserInd.sumPct > 80 ? '#ffc000' : '#00b050',
                                            fontWeight: 'bold'
                                        }}>
                                            {laserInd.sumPct.toFixed(1)}%
                                        </td>
                                        <td style={{ color: laserInd.retThisRaw > 0 ? '#ff6600' : 'inherit' }}>
                                            {laserInd.retThisRaw > 0 ? laserInd.retThisRaw.toFixed(0) : '-'}
                                        </td>
                                        <td style={{ color: laserInd.retThisRaw > 0 ? '#ff6600' : 'inherit' }}>
                                            {laserInd.retThisRaw > 0 ? laserInd.retThisRawPct.toFixed(1) + '%' : '-'}
                                        </td>
                                        <td style={{ color: '#009688', fontWeight: 'bold' }}>
                                            {laserInd.showTimeLeft ? laserInd.tempsRestantMin.toFixed(0) : '-'}
                                        </td>
                                        <td style={{ color: laserInd.retThisRaw > 0 ? '#ff6600' : 'inherit', fontWeight: 'bold' }}>
                                            {laserInd.retThisRaw > 0 ? laserInd.retThis.toFixed(0) : '-'}
                                        </td>
                                    </tr>
                                </React.Fragment>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        );
    };

    /**
     * Render the charge details panel showing all series for the selected shift.
     */
    /** Apply the part-number report's column filters + active sort. */
    getFilteredSortedPnReport = () => {
        const { partNumberReport, pnReportFilters, pnReportSort } = this.state;
        let rows = Array.isArray(partNumberReport) ? [...partNumberReport] : [];
        Object.entries(pnReportFilters || {}).forEach(([col, val]) => {
            if (val == null || val === '') return;
            const needle = String(val).toLowerCase();
            rows = rows.filter(r => r[col] != null && String(r[col]).toLowerCase().includes(needle));
        });
        if (pnReportSort && pnReportSort.col) {
            const { col, dir } = pnReportSort;
            const numeric = ['quantity', 'cmsId', 'perimetre', 'percentageOnPlan', 'tempsDeCoupe'].includes(col);
            rows.sort((a, b) => {
                const av = a[col], bv = b[col];
                if (av == null && bv == null) return 0;
                if (av == null) return 1;
                if (bv == null) return -1;
                const cmp = numeric ? (Number(av) - Number(bv)) : String(av).localeCompare(String(bv));
                return dir === 'desc' ? -cmp : cmp;
            });
        }
        return rows;
    };

    togglePnReportSort = (col) => {
        this.setState(prev => {
            const cur = prev.pnReportSort || {};
            return { pnReportSort: cur.col === col ? { col, dir: cur.dir === 'asc' ? 'desc' : 'asc' } : { col, dir: 'asc' } };
        });
    };

    setPnReportFilter = (col, val) => {
        this.setState(prev => ({ pnReportFilters: { ...(prev.pnReportFilters || {}), [col]: val } }));
    };

    pnReportColumns = () => ([
        ['projet', 'Projet'], ['version', 'Version'], ['partNumber', 'Part Number'],
        ['quantity', 'Quantity'], ['cmsId', 'ID Plan CMS'], ['sequence', 'Sequence'],
        ['perimetre', 'Perimetre'], ['percentageOnPlan', '% sur Plan'], ['tempsDeCoupe', 'Temps de coupe (min)']
    ]);

    exportPnReportCSV = () => {
        const rows = this.getFilteredSortedPnReport();
        if (!rows.length) return;
        const cols = this.pnReportColumns();
        const dec = new Set(['perimetre', 'percentageOnPlan', 'tempsDeCoupe']);
        let csv = '﻿' + cols.map(c => c[1]).join(';') + '\n';
        rows.forEach(r => {
            csv += cols.map(([k]) => {
                const v = r[k];
                if (v == null) return '';
                if (dec.has(k)) return Number(v).toFixed(2).replace('.', ',');
                return String(v).replace(/;/g, ',');
            }).join(';') + '\n';
        });
        const { selectedShiftForCharge } = this.state;
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `rapport_partnumber_${selectedShiftForCharge?.date || ''}_S${selectedShiftForCharge?.shift || ''}.csv`;
        link.click();
    };

    exportPnReportExcel = () => {
        const rows = this.getFilteredSortedPnReport();
        if (!rows.length) return;
        const cols = this.pnReportColumns();
        const dec = new Set(['perimetre', 'percentageOnPlan', 'tempsDeCoupe']);
        let html = '<table border="1"><thead><tr>' + cols.map(c => `<th>${c[1]}</th>`).join('') + '</tr></thead><tbody>';
        rows.forEach(r => {
            html += '<tr>' + cols.map(([k]) => {
                const v = r[k];
                if (v == null) return '<td></td>';
                return `<td>${dec.has(k) ? Number(v).toFixed(2) : v}</td>`;
            }).join('') + '</tr>';
        });
        html += '</tbody></table>';
        const { selectedShiftForCharge } = this.state;
        const blob = new Blob(['﻿' + html], { type: 'application/vnd.ms-excel;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `rapport_partnumber_${selectedShiftForCharge?.date || ''}_S${selectedShiftForCharge?.shift || ''}.xls`;
        link.click();
    };

    /** Part-number cutting-time report table (imported + non-imported), sortable + filterable. */
    renderPartNumberReport = () => {
        const rows = this.getFilteredSortedPnReport();
        const { pnReportSort, pnReportFilters } = this.state;
        const cols = this.pnReportColumns();
        const fmt2 = v => (v == null ? '' : Number(v).toFixed(2));
        const totalTemps = rows.reduce((acc, r) => acc + (Number(r.tempsDeCoupe) || 0), 0);
        const sortIcon = (col) => (pnReportSort && pnReportSort.col === col ? (pnReportSort.dir === 'asc' ? ' ▲' : ' ▼') : '');
        return (
            <div className="pdc-pn-report" style={{ marginTop: '20px' }}>
                <h4 style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span><FontAwesomeIcon icon={faChartBar} style={{ marginRight: '8px' }} />Temps de coupe par Part Number ({rows.length})</span>
                    <span>
                        <button className="pdc-btn-export" style={{ marginRight: 6 }} onClick={this.exportPnReportCSV}><FontAwesomeIcon icon={faDownload} /> CSV</button>
                        <button className="pdc-btn-export" onClick={this.exportPnReportExcel}><FontAwesomeIcon icon={faDownload} /> Excel</button>
                    </span>
                </h4>
                <div style={{ overflowX: 'auto' }}>
                    <table className="pdc-summary-table" style={{ fontSize: '0.82rem' }}>
                        <thead>
                            <tr>
                                {cols.map(([k, label]) => (
                                    <th key={k} style={{ cursor: 'pointer', whiteSpace: 'nowrap' }} onClick={() => this.togglePnReportSort(k)}>
                                        {label}{sortIcon(k)}
                                    </th>
                                ))}
                            </tr>
                            <tr>
                                {cols.map(([k]) => (
                                    <th key={k} style={{ padding: '2px' }}>
                                        <input
                                            type="text"
                                            value={(pnReportFilters && pnReportFilters[k]) || ''}
                                            onChange={e => this.setPnReportFilter(k, e.target.value)}
                                            style={{ width: '95%', fontSize: '0.75rem', padding: '1px 3px' }}
                                            placeholder="filtrer"
                                        />
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {rows.map((r, i) => (
                                <tr key={i} style={{ background: r.imported === false ? '#fff3e0' : 'inherit' }}>
                                    <td>{r.projet || ''}</td>
                                    <td>{r.version || ''}</td>
                                    <td>{r.partNumber || ''}</td>
                                    <td style={{ textAlign: 'center' }}>{r.quantity != null ? r.quantity : ''}</td>
                                    <td style={{ textAlign: 'center' }}>{r.cmsId != null ? r.cmsId : ''}</td>
                                    <td>{r.sequence || ''}</td>
                                    <td style={{ textAlign: 'right' }}>{fmt2(r.perimetre)}</td>
                                    <td style={{ textAlign: 'right' }}>{r.percentageOnPlan != null ? fmt2(r.percentageOnPlan) + '%' : ''}</td>
                                    <td style={{ textAlign: 'right', fontWeight: 'bold' }}>{fmt2(r.tempsDeCoupe)}</td>
                                </tr>
                            ))}
                            {rows.length === 0 && (
                                <tr><td colSpan="9" style={{ textAlign: 'center', color: '#999' }}>Aucune donnée</td></tr>
                            )}
                        </tbody>
                        <tfoot>
                            <tr style={{ fontWeight: 'bold' }}>
                                <td colSpan="8" style={{ textAlign: 'right' }}>Total Temps de coupe :</td>
                                <td style={{ textAlign: 'right' }}>{totalTemps.toFixed(2)} min</td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
                <p style={{ fontSize: '0.75rem', color: '#888', margin: '4px 0 0' }}>
                    Lignes oranges = charge non importée (temps direct, sans plan/perimetre). Importé : temps = (temps du plan dans le shift) × (perimetre du PN / perimetre total du plan).
                </p>
            </div>
        );
    };

    renderChargeDetails = () => {
        const { showChargeDetails, selectedShiftForCharge, chargeSummary, detailedSeries, loadingCharge, chargeDetailsMachineTypeFilter } = this.state;

        if (!showChargeDetails) return null;

        // Filter series by selected machine type
        const filteredSeries = chargeDetailsMachineTypeFilter 
            ? detailedSeries.filter(s => (s.machine || 'Unknown') === chargeDetailsMachineTypeFilter)
            : detailedSeries;

        return (
            <div className="pdc-charge-details-overlay" onClick={this.closeChargeDetails}>
                <div className="pdc-charge-details-panel" onClick={e => e.stopPropagation()}>
                    <div className="pdc-charge-details-header">
                        <h3>
                            <FontAwesomeIcon icon={faChartBar} style={{ marginRight: '8px' }} />
                            Détails de Charge - {selectedShiftForCharge?.date} Shift {selectedShiftForCharge?.shift}
                            {this.state.servedFromSnapshot && (
                                <span style={{ fontSize: '0.65em', fontWeight: 'normal', color: '#888', marginLeft: '12px' }}>
                                    <FontAwesomeIcon icon={faInfoCircle} style={{ marginRight: '4px' }} />
                                    données archivées{this.state.snapshotAt ? ` — ${String(this.state.snapshotAt).replace('T', ' ').slice(0, 16)}` : ''}
                                </span>
                            )}
                        </h3>
                        <span>
                            {selectedShiftForCharge && this.isOldShift(selectedShiftForCharge.date, String(selectedShiftForCharge.shift)) && (
                                <button className="pdc-btn-export" style={{ marginRight: '8px' }} disabled={loadingCharge}
                                    title="Recalculer ce shift et remplacer les données archivées"
                                    onClick={() => this.loadChargeDetails(selectedShiftForCharge.date, selectedShiftForCharge.shift, true)}>
                                    <FontAwesomeIcon icon={faSync} spin={loadingCharge} /> Actualiser
                                </button>
                            )}
                            <button className="pdc-modal-close" onClick={this.closeChargeDetails}>
                                <FontAwesomeIcon icon={faTimes} />
                            </button>
                        </span>
                    </div>

                    {loadingCharge ? (
                        <div className="pdc-loading">
                            <FontAwesomeIcon icon={faSpinner} spin size="2x" />
                            <p>Chargement des détails...</p>
                        </div>
                    ) : (
                        <div className="pdc-charge-details-content">
                            {/* Global Indicators Summary */}
                            {chargeSummary && (
                                <div className="pdc-charge-summary-section">
                                    <h4>Indicateurs Globaux</h4>
                                    <div className="pdc-charge-summary-grid">
                                        <div className="pdc-charge-stat">
                                            <span className="pdc-stat-label">Séries</span>
                                            <span className="pdc-stat-value">{chargeSummary.totalSeries}</span>
                                        </div>
                                        <div className="pdc-charge-stat">
                                            <span className="pdc-stat-label">Charge (Chg)</span>
                                            <span className="pdc-stat-value">
                                                {chargeSummary.totalChgMin?.toFixed(0)} min — {chargeSummary.globalChgPct?.toFixed(1)}%
                                            </span>
                                        </div>
                                        <div className="pdc-charge-stat" style={{ borderLeft: '3px solid #2196f3' }}>
                                            <span className="pdc-stat-label">SR (travaillé avant shift)</span>
                                            <span className="pdc-stat-value" style={{ color: '#2196f3' }}>
                                                {chargeSummary.totalSrMin?.toFixed(0)} min — {chargeSummary.globalSrPct?.toFixed(1)}%
                                            </span>
                                        </div>
                                        <div className="pdc-charge-stat" style={{ borderLeft: '3px solid #d32f2f' }}>
                                            <span className="pdc-stat-label">Retard shift précédent</span>
                                            <span className="pdc-stat-value" style={{ color: '#d32f2f' }}>
                                                {chargeSummary.totalRetardPrev?.toFixed(0)} min — {chargeSummary.globalRetPrevPct?.toFixed(1)}%
                                            </span>
                                        </div>
                                        <div className="pdc-charge-stat" style={{ borderLeft: '3px solid #388e3c', fontWeight: 'bold' }}>
                                            <span className="pdc-stat-label">Charge réelle (Sum = Chg - SR + Ret prev)</span>
                                            <span className="pdc-stat-value" style={{ 
                                                color: chargeSummary.globalSumPct > 100 ? '#ff0000' : chargeSummary.globalSumPct > 80 ? '#ffc000' : '#00b050'
                                            }}>
                                                {chargeSummary.globalSumPct?.toFixed(1)}%
                                            </span>
                                        </div>
                                        <div className="pdc-charge-stat" style={{ borderLeft: '3px solid #ff6600' }}>
                                            <span className="pdc-stat-label">Retard brut du shift</span>
                                            <span className="pdc-stat-value" style={{ color: '#ff6600' }}>
                                                {chargeSummary.totalRetardThisShift?.toFixed(0)} min — {chargeSummary.globalRetThisPct?.toFixed(1)}%
                                            </span>
                                        </div>
                                        {/* Non-imported charge stat */}
                                        <div 
                                            className="pdc-charge-stat" 
                                            style={{ borderLeft: '3px solid #9c27b0', cursor: this.state.nonImportedCharge?.count > 0 ? 'pointer' : 'default' }}
                                            onClick={() => this.state.nonImportedCharge?.count > 0 && this.setState({ showNonImportedChargeModal: true })}
                                            title={this.state.nonImportedCharge?.count > 0 ? "Cliquer pour voir le détail" : ""}
                                        >
                                            <span className="pdc-stat-label">
                                                Charge non importée
                                                {this.state.loadingNonImportedCharge && <FontAwesomeIcon icon={faSpinner} spin style={{ marginLeft: 6, fontSize: '0.7rem' }} />}
                                            </span>
                                            <span className="pdc-stat-value" style={{ color: '#9c27b0' }}>
                                                {this.state.nonImportedCharge
                                                    ? `${this.state.nonImportedCharge.totalMinutes?.toFixed(0)} min — ${this.state.nonImportedCharge.count} OF`
                                                    : '-'
                                                }
                                            </span>
                                        </div>
                                        {chargeSummary.showTimeLeft && (
                                            <div className="pdc-charge-stat" style={{ borderLeft: '3px solid #00796b' }}>
                                                <span className="pdc-stat-label">Temps restant sur le shift</span>
                                                <span className="pdc-stat-value" style={{ color: '#00796b' }}>
                                                    {chargeSummary.globalTempsRestantMin?.toFixed(0)} min
                                                </span>
                                            </div>
                                        )}
                                        {chargeSummary.showTimeLeft && (
                                            <div className="pdc-charge-stat" style={{ borderLeft: '3px solid #e65100' }}>
                                                <span className="pdc-stat-label">Ret→next ajusté (Ret - Temps restant)</span>
                                                <span className="pdc-stat-value" style={{ color: '#e65100' }}>
                                                    {chargeSummary.globalRetardAdjustedMin?.toFixed(0)} min — {chargeSummary.globalRetardAdjustedPct?.toFixed(1)}%
                                                </span>
                                            </div>
                                        )}
                                        <div className="pdc-charge-stat">
                                            <span className="pdc-stat-label">Capacité</span>
                                            <span className="pdc-stat-value">
                                                {chargeSummary.globalMachinesAvailable}/{chargeSummary.globalTotalMachines} machines — {chargeSummary.globalAvailableTime?.toFixed(0)} min
                                            </span>
                                        </div>
                                    </div>

                                    {/* Per Machine Type Indicators Table */}
                                    <h4 style={{ marginTop: '20px' }}>
                                        Indicateurs par Type de Machine
                                        {chargeDetailsMachineTypeFilter && (
                                            <button 
                                                className="btn btn-sm btn-outline-secondary ml-3"
                                                onClick={() => this.setState({ chargeDetailsMachineTypeFilter: null })}
                                            >
                                                <FontAwesomeIcon icon={faTimes} className="mr-1" />
                                                Effacer filtre: {chargeDetailsMachineTypeFilter}
                                            </button>
                                        )}
                                    </h4>
                                    <table className="pdc-charge-breakdown-table">
                                        <thead>
                                            <tr>
                                                <th>Type Machine</th>
                                                <th>Séries</th>
                                                <th>Cap</th>
                                                <th title="Capacité installée (machines configurées pour ce shift)">Cap. Inst.</th>
                                                <th>Chg (min)</th>
                                                <th title="Chg % basé sur les machines actuellement disponibles × efficience">Chg %</th>
                                                <th title="Chg % basé sur la capacité installée × efficience (référence théorique)" style={{ color: '#6a1b9a' }}>Chg % cap.inst.</th>
                                                <th>SR (min)</th>
                                                <th>SR %</th>
                                                <th>Ret prev (min)</th>
                                                <th>Ret prev %</th>
                                                <th title="Charge réelle = Chg - SR + Ret prev">Sum %</th>
                                                <th>Ret→next brut (min)</th>
                                                <th>Ret→next brut %</th>
                                                {chargeSummary.showTimeLeft && <th title="Temps productif restant sur le shift" style={{ color: '#00796b' }}>Temps Restant (min)</th>}
                                                {chargeSummary.showTimeLeft && <th title="Retard ajusté = Ret→next - Temps Restant" style={{ color: '#e65100' }}>Ret ajusté (min)</th>}
                                                {chargeSummary.showTimeLeft && <th style={{ color: '#e65100' }}>Ret ajusté %</th>}
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {chargeSummary.machineTypeIndicators && Object.entries(chargeSummary.machineTypeIndicators).map(([type, ind]) => (
                                                <tr 
                                                    key={type} 
                                                    style={{ 
                                                        cursor: 'pointer',
                                                        backgroundColor: chargeDetailsMachineTypeFilter === type ? '#e3f2fd' : 'inherit'
                                                    }}
                                                    onClick={() => this.setState({ 
                                                        chargeDetailsMachineTypeFilter: chargeDetailsMachineTypeFilter === type ? null : type 
                                                    })}
                                                    title="Cliquer pour filtrer les séries par ce type"
                                                >
                                                    <td>
                                                        <span 
                                                            className="pdc-machine-type-badge"
                                                            style={{ backgroundColor: this.getMachineTypeColor(type) }}
                                                        >
                                                            {type}
                                                        </span>
                                                    </td>
                                                    <td>{ind.seriesCount}</td>
                                                    <td>{ind.machinesAvailable}/{ind.totalMachines}</td>
                                                    <td title="Machines installées configurées (capaciteInstallee)">
                                                        {ind.installedMachines > 0 ? ind.installedMachines : '-'}
                                                    </td>
                                                    <td style={{ fontWeight: 'bold' }}>{ind.chargeMin?.toFixed(0)}</td>
                                                    <td style={{ color: ind.chgPct > 100 ? '#ff0000' : ind.chgPct > 80 ? '#ffc000' : '#00b050', fontWeight: 'bold' }}>
                                                        {ind.chgPct?.toFixed(1)}%
                                                    </td>
                                                    <td
                                                        title="Chg min / (capaciteInstallee × temps configuré × efficience)"
                                                        style={{
                                                            color: ind.installedCapacityMinutes > 0
                                                                ? (ind.chgPctInstalled > 100 ? '#ff0000' : ind.chgPctInstalled > 80 ? '#ffc000' : '#6a1b9a')
                                                                : '#999',
                                                            fontWeight: 'bold'
                                                        }}
                                                    >
                                                        {ind.installedCapacityMinutes > 0 ? ind.chgPctInstalled.toFixed(1) + '%' : '-'}
                                                    </td>
                                                    <td style={{ color: ind.srMin > 0 ? '#2196f3' : 'inherit' }}>
                                                        {ind.srMin > 0 ? ind.srMin.toFixed(0) : '-'}
                                                    </td>
                                                    <td style={{ color: ind.srPct > 0 ? '#2196f3' : 'inherit' }}>
                                                        {ind.srPct > 0 ? ind.srPct.toFixed(1) + '%' : '-'}
                                                    </td>
                                                    <td style={{ color: ind.retardPrevRawMin > 0 ? '#d32f2f' : 'inherit' }}>
                                                        {ind.retardPrevRawMin > 0 ? ind.retardPrevMin.toFixed(0) : '-'}
                                                    </td>
                                                    <td style={{ color: ind.retardPrevRawMin > 0 ? '#d32f2f' : 'inherit' }}>
                                                        {ind.retardPrevRawMin > 0 ? ind.retardPrevPct.toFixed(1) + '%' : '-'}
                                                    </td>
                                                    <td style={{ 
                                                        color: ind.sumPct > 100 ? '#ff0000' : ind.sumPct > 80 ? '#ffc000' : '#00b050',
                                                        fontWeight: 'bold'
                                                    }}>
                                                        {ind.sumPct?.toFixed(1)}%
                                                    </td>
                                                    <td style={{ color: ind.retardThisShiftMin > 0 ? '#ff6600' : 'inherit' }}>
                                                        {ind.retardThisShiftMin > 0 ? ind.retardThisShiftMin.toFixed(0) : '-'}
                                                    </td>
                                                    <td style={{ color: ind.retardThisShiftPct > 0 ? '#ff6600' : 'inherit' }}>
                                                        {ind.retardThisShiftPct > 0 ? ind.retardThisShiftPct.toFixed(1) + '%' : '-'}
                                                    </td>
                                                    {chargeSummary.showTimeLeft && (
                                                        <td style={{ color: '#00796b', fontWeight: 'bold' }}>
                                                            {ind.tempsRestantMin != null ? ind.tempsRestantMin.toFixed(0) : '-'}
                                                        </td>
                                                    )}
                                                    {chargeSummary.showTimeLeft && (
                                                        <td style={{ color: ind.retardAdjustedMin > 0 ? '#e65100' : 'inherit', fontWeight: 'bold' }}>
                                                            {ind.retardAdjustedMin != null ? (ind.retardAdjustedMin > 0 ? ind.retardAdjustedMin.toFixed(0) : '0') : '-'}
                                                        </td>
                                                    )}
                                                    {chargeSummary.showTimeLeft && (
                                                        <td style={{ color: ind.retardAdjustedPct > 0 ? '#e65100' : 'inherit', fontWeight: 'bold' }}>
                                                            {ind.retardAdjustedPct != null ? (ind.retardAdjustedPct > 0 ? ind.retardAdjustedPct.toFixed(1) + '%' : '0%') : '-'}
                                                        </td>
                                                    )}
                                                </tr>
                                            ))}
                                            {/* Non-imported charge row */}
                                            {this.state.nonImportedCharge?.totalMinutes > 0 && (
                                                <tr 
                                                    style={{ 
                                                        backgroundColor: '#f3e5f5',
                                                        cursor: 'pointer'
                                                    }}
                                                    onClick={() => this.setState({ showNonImportedChargeModal: true })}
                                                    title="Cliquer pour voir le détail"
                                                >
                                                    <td>
                                                        <span 
                                                            className="pdc-machine-type-badge"
                                                            style={{ backgroundColor: '#9c27b0', color: '#fff' }}
                                                        >
                                                            Non importé
                                                        </span>
                                                    </td>
                                                    <td>{this.state.nonImportedCharge.count}</td>
                                                    <td colSpan="2">—</td>
                                                    <td style={{ fontWeight: 'bold', color: '#9c27b0' }}>
                                                        {this.state.nonImportedCharge.totalMinutes.toFixed(0)}
                                                    </td>
                                                    <td colSpan={chargeSummary.showTimeLeft ? 11 : 8}>—</td>
                                                </tr>
                                            )}
                                        </tbody>
                                        <tfoot>
                                            <tr style={{ fontWeight: 'bold', borderTop: '2px solid #333' }}>
                                                <td>GLOBAL</td>
                                                <td>{chargeSummary.totalSeries}</td>
                                                <td>{chargeSummary.globalMachinesAvailable}/{chargeSummary.globalTotalMachines}</td>
                                                <td>
                                                    {Object.values(chargeSummary.machineTypeIndicators || {})
                                                        .reduce((acc, ind) => acc + (ind.installedMachines || 0), 0) || '-'}
                                                </td>
                                                <td>{chargeSummary.totalChgMin?.toFixed(0)}</td>
                                                <td style={{ color: chargeSummary.globalChgPct > 100 ? '#ff0000' : chargeSummary.globalChgPct > 80 ? '#ffc000' : '#00b050' }}>
                                                    {chargeSummary.globalChgPct?.toFixed(1)}%
                                                </td>
                                                <td
                                                    title="Chg min / (Σ capaciteInstallee × temps configuré × efficience)"
                                                    style={{
                                                        color: chargeSummary.globalInstalledCapacityMinutes > 0
                                                            ? (chargeSummary.globalChgPctInstalled > 100 ? '#ff0000' : chargeSummary.globalChgPctInstalled > 80 ? '#ffc000' : '#6a1b9a')
                                                            : '#999'
                                                    }}
                                                >
                                                    {chargeSummary.globalInstalledCapacityMinutes > 0
                                                        ? chargeSummary.globalChgPctInstalled.toFixed(1) + '%'
                                                        : '-'}
                                                </td>
                                                <td style={{ color: chargeSummary.totalSrMin > 0 ? '#2196f3' : 'inherit' }}>
                                                    {chargeSummary.totalSrMin > 0 ? chargeSummary.totalSrMin.toFixed(0) : '-'}
                                                </td>
                                                <td style={{ color: chargeSummary.globalSrPct > 0 ? '#2196f3' : 'inherit' }}>
                                                    {chargeSummary.globalSrPct > 0 ? chargeSummary.globalSrPct.toFixed(1) + '%' : '-'}
                                                </td>
                                                <td style={{ color: chargeSummary.totalRetardPrevRaw > 0 ? '#d32f2f' : 'inherit' }}>
                                                    {chargeSummary.totalRetardPrevRaw > 0 ? chargeSummary.totalRetardPrev.toFixed(0) : '-'}
                                                </td>
                                                <td style={{ color: chargeSummary.totalRetardPrevRaw > 0 ? '#d32f2f' : 'inherit' }}>
                                                    {chargeSummary.totalRetardPrevRaw > 0 ? chargeSummary.globalRetPrevPct.toFixed(1) + '%' : '-'}
                                                </td>
                                                <td style={{ 
                                                    color: chargeSummary.globalSumPct > 100 ? '#ff0000' : chargeSummary.globalSumPct > 80 ? '#ffc000' : '#00b050'
                                                }}>
                                                    {chargeSummary.globalSumPct?.toFixed(1)}%
                                                </td>
                                                <td style={{ color: chargeSummary.totalRetardThisShift > 0 ? '#ff6600' : 'inherit' }}>
                                                    {chargeSummary.totalRetardThisShift > 0 ? chargeSummary.totalRetardThisShift.toFixed(0) : '-'}
                                                </td>
                                                <td style={{ color: chargeSummary.globalRetThisPct > 0 ? '#ff6600' : 'inherit' }}>
                                                    {chargeSummary.globalRetThisPct > 0 ? chargeSummary.globalRetThisPct.toFixed(1) + '%' : '-'}
                                                </td>
                                                {chargeSummary.showTimeLeft && (
                                                    <td style={{ color: '#00796b', fontWeight: 'bold' }}>
                                                        {chargeSummary.globalTempsRestantMin != null ? chargeSummary.globalTempsRestantMin.toFixed(0) : '-'}
                                                    </td>
                                                )}
                                                {chargeSummary.showTimeLeft && (
                                                    <td style={{ color: chargeSummary.globalRetardAdjustedMin > 0 ? '#e65100' : 'inherit', fontWeight: 'bold' }}>
                                                        {chargeSummary.globalRetardAdjustedMin != null ? (chargeSummary.globalRetardAdjustedMin > 0 ? chargeSummary.globalRetardAdjustedMin.toFixed(0) : '0') : '-'}
                                                    </td>
                                                )}
                                                {chargeSummary.showTimeLeft && (
                                                    <td style={{ color: chargeSummary.globalRetardAdjustedPct > 0 ? '#e65100' : 'inherit', fontWeight: 'bold' }}>
                                                        {chargeSummary.globalRetardAdjustedPct != null ? (chargeSummary.globalRetardAdjustedPct > 0 ? chargeSummary.globalRetardAdjustedPct.toFixed(1) + '%' : '0%') : '-'}
                                                    </td>
                                                )}
                                            </tr>
                                        </tfoot>
                                    </table>
                                </div>
                            )}

                            {/* Capacité Installée Editing - ROLE_PROCESS only */}
                            {chargeSummary && selectedShiftForCharge && 
                             this.props.security?.user?.roles?.some(r => 
                                ['ROLE_PROCESS', 'ROLE_ADMIN'].includes(r.authority)
                             ) && (() => {
                                const capGroups = ['Coupe', 'Laser'];
                                const capEntries = capGroups.map(g => {
                                    const existing = (this.state.capaciteInstalleeData || []).find(c =>
                                        c.dateProduction === selectedShiftForCharge.date &&
                                        c.shiftNumber === parseInt(selectedShiftForCharge.shift) &&
                                        c.groupe === g
                                    );
                                    return {
                                        groupe: g,
                                        id: existing?.id || null,
                                        dateProduction: selectedShiftForCharge.date,
                                        shiftNumber: parseInt(selectedShiftForCharge.shift),
                                        capaciteInstallee: existing?.capaciteInstallee || 0,
                                        efficienceTarget: existing?.efficienceTarget || 90,
                                        tempsTotalParMachine: existing?.tempsTotalParMachine || 460
                                    };
                                });
                                const { editingCapacite, savingCapacite } = this.state;
                                return (
                                    <div style={{ marginTop: '15px', padding: '12px', background: '#f3e5f5', borderRadius: '8px', border: '1px solid #ce93d8' }}>
                                        <h4 style={{ margin: '0 0 10px 0', color: '#6a1b9a' }}>
                                            <FontAwesomeIcon icon={faCog} style={{ marginRight: '6px' }} />
                                            Capacité Installée — {selectedShiftForCharge.date} Shift {selectedShiftForCharge.shift}
                                        </h4>
                                        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                                            <thead>
                                                <tr style={{ borderBottom: '2px solid #ce93d8' }}>
                                                    <th style={{ textAlign: 'left', padding: '4px 8px' }}>Groupe</th>
                                                    <th style={{ textAlign: 'center', padding: '4px 8px' }}>Capacité Installée</th>
                                                    <th style={{ textAlign: 'center', padding: '4px 8px' }}>Efficience Target %</th>
                                                    <th style={{ textAlign: 'center', padding: '4px 8px' }}>Actions</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {capEntries.map(cap => {
                                                    const isEditing = editingCapacite && editingCapacite.groupe === cap.groupe;
                                                    return (
                                                        <tr key={cap.groupe} style={{ borderBottom: '1px solid #e1bee7' }}>
                                                            <td style={{ padding: '6px 8px', fontWeight: 'bold' }}>{cap.groupe}</td>
                                                            <td style={{ textAlign: 'center', padding: '6px 8px' }}>
                                                                {isEditing ? (
                                                                    <input
                                                                        type="number"
                                                                        min="0"
                                                                        value={editingCapacite.capaciteInstallee}
                                                                        onChange={e => this.setState({
                                                                            editingCapacite: { ...editingCapacite, capaciteInstallee: parseInt(e.target.value) || 0 }
                                                                        })}
                                                                        style={{ width: '70px', textAlign: 'center', padding: '2px 4px' }}
                                                                    />
                                                                ) : cap.capaciteInstallee}
                                                            </td>
                                                            <td style={{ textAlign: 'center', padding: '6px 8px' }}>
                                                                {isEditing ? (
                                                                    <input
                                                                        type="number"
                                                                        min="0"
                                                                        max="100"
                                                                        step="0.1"
                                                                        value={editingCapacite.efficienceTarget}
                                                                        onChange={e => this.setState({
                                                                            editingCapacite: { ...editingCapacite, efficienceTarget: parseFloat(e.target.value) || 0 }
                                                                        })}
                                                                        style={{ width: '70px', textAlign: 'center', padding: '2px 4px' }}
                                                                    />
                                                                ) : cap.efficienceTarget + '%'}
                                                            </td>
                                                            <td style={{ textAlign: 'center', padding: '6px 8px' }}>
                                                                {isEditing ? (
                                                                    <>
                                                                        <button
                                                                            className="pdc-btn-search"
                                                                            style={{ padding: '2px 10px', fontSize: '0.8rem', marginRight: '4px' }}
                                                                            disabled={savingCapacite}
                                                                            onClick={() => this.saveCapaciteInstallee(editingCapacite)}
                                                                        >
                                                                            {savingCapacite ? <FontAwesomeIcon icon={faSpinner} spin /> : '✓'}
                                                                        </button>
                                                                        <button
                                                                            className="pdc-btn-export"
                                                                            style={{ padding: '2px 10px', fontSize: '0.8rem', background: '#999' }}
                                                                            onClick={() => this.setState({ editingCapacite: null })}
                                                                        >
                                                                            ✕
                                                                        </button>
                                                                    </>
                                                                ) : (
                                                                    <button
                                                                        className="pdc-btn-search"
                                                                        style={{ padding: '2px 10px', fontSize: '0.8rem' }}
                                                                        onClick={() => this.setState({ editingCapacite: { ...cap } })}
                                                                    >
                                                                        <FontAwesomeIcon icon={faPen} />
                                                                    </button>
                                                                )}
                                                            </td>
                                                        </tr>
                                                    );
                                                })}
                                            </tbody>
                                        </table>
                                    </div>
                                );
                            })()}

                            {/* Part Number Cutting-Time Report (imported + non-imported) */}
                            {this.renderPartNumberReport()}

                            {/* Detailed Series List */}
                            <div className="pdc-series-list-section">
                                <h4>
                                    Liste des Séries ({filteredSeries.length}{chargeDetailsMachineTypeFilter ? ` / ${detailedSeries.length} total` : ''})
                                    <span className="pdc-legend-inline">
                                        <span className="pdc-legend-cut">✓ Coupé</span>
                                        <span className="pdc-legend-cutting">⏳ En Cours</span>
                                        <span className="pdc-legend-carryover">⚠ Non Coupé</span>
                                        <span style={{ color: '#ff6600', fontWeight: 'bold', marginLeft: '10px' }}>◐ Retard Partiel</span>
                                    </span>
                                    <span style={{ marginLeft: 'auto', display: 'flex', gap: '8px' }}>
                                        <button
                                            className="pdc-btn-export"
                                            style={{ padding: '4px 10px', fontSize: '0.78rem' }}
                                            onClick={() => this.exportSeriesCSV(filteredSeries, selectedShiftForCharge)}
                                            title="Exporter en CSV"
                                        >
                                            <FontAwesomeIcon icon={faDownload} /> CSV
                                        </button>
                                        <button
                                            className="pdc-btn-export"
                                            style={{ padding: '4px 10px', fontSize: '0.78rem', background: '#1976d2' }}
                                            onClick={() => this.exportSeriesExcel(filteredSeries, selectedShiftForCharge)}
                                            title="Exporter en Excel"
                                        >
                                            <FontAwesomeIcon icon={faDownload} /> Excel
                                        </button>
                                    </span>
                                </h4>
                                <div className="pdc-series-table-scroll">
                                    <table className="pdc-series-table">
                                        <thead>
                                            <tr>
                                                <th>Série</th>
                                                <th title="Séquence (CuttingRequest.sequence)">Séquence</th>
                                                <th title="CuttingPlan.id (clé interne)">Plan ID</th>
                                                <th title="CuttingPlan.cmsId (référence CMS)">CMS Id</th>
                                                <th>Placement</th>
                                                <th>Machine</th>
                                                <th>Table Coupe</th>
                                                <th>Temps (min)</th>
                                                <th title="Source du temps de coupe estimé : Validated > Real > TempsDeCoupe">Source</th>
                                                <th>Début Coupe</th>
                                                <th>Fin Coupe</th>
                                                <th>Statut</th>
                                                <th>Retard (min)</th>
                                                <th>SR (min)</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {filteredSeries.map((serie, idx) => {
                                                // Colour-code the cutting-time source so operators spot when we fall back
                                                // to a raw tempsDeCoupe (least reliable) vs a validated TimingModel value.
                                                const sourceColor = serie.cuttingTimeSource === 'Validated' ? '#2e7d32'
                                                    : serie.cuttingTimeSource === 'Real' ? '#1565c0'
                                                    : serie.cuttingTimeSource === 'TempsDeCoupe' ? '#e65100'
                                                    : '#999';
                                                const sourceLabel = serie.cuttingTimeSource || '-';
                                                return (
                                                    <tr
                                                        key={`${serie.serie}-${idx}`}
                                                        className={
                                                            serie.isPartialRetard ? 'pdc-row-partial-retard' :
                                                            serie.isCut ? 'pdc-row-cut' :
                                                            serie.isCutting ? 'pdc-row-cutting' :
                                                            'pdc-row-carryover'
                                                        }
                                                    >
                                                        <td>{serie.serie}</td>
                                                        <td>{serie.sequence || '-'}</td>
                                                        <td>{serie.cuttingPlanId || '-'}</td>
                                                        <td>{serie.cmsId || '-'}</td>
                                                        <td className="pdc-cell-placement">{serie.placement || '-'}</td>
                                                        <td>
                                                            <span
                                                                className="pdc-machine-type-badge"
                                                                style={{ backgroundColor: this.getMachineTypeColor(serie.machine || 'Unknown'), fontSize: '11px' }}
                                                            >
                                                                {serie.machine || 'Unknown'}
                                                            </span>
                                                        </td>
                                                        <td>{serie.tableCoupe || '-'}</td>
                                                        <td className="pdc-cell-time">
                                                            {serie.effectiveCuttingTime?.toFixed(2) || '-'}
                                                        </td>
                                                        <td style={{ color: sourceColor, fontWeight: 'bold', fontSize: '0.85em' }}>
                                                            {sourceLabel}
                                                        </td>
                                                        <td>{serie.dateDebutCoupe ? this.formatDateTimeFR(serie.dateDebutCoupe) : '-'}</td>
                                                        <td>{serie.dateFinCoupe ? this.formatDateTimeFR(serie.dateFinCoupe) : '-'}</td>
                                                        <td>
                                                            {serie.isPartialRetard ? (
                                                                <span className="pdc-status-partial">◐ Retard Partiel</span>
                                                            ) : serie.isCut ? (
                                                                <span className="pdc-status-cut">✓ Coupé</span>
                                                            ) : serie.isCutting ? (
                                                                <span className="pdc-status-cutting">⏳ En Cours</span>
                                                            ) : (
                                                                <span className="pdc-status-carryover">⚠ Non Coupé</span>
                                                            )}
                                                        </td>
                                                        <td style={{ fontWeight: serie.retardMinutes > 0 ? 'bold' : 'normal', color: serie.retardMinutes > 0 ? '#d32f2f' : 'inherit' }}>
                                                            {serie.retardMinutes > 0 ? Math.round(serie.retardMinutes) : '-'}
                                                        </td>
                                                        <td style={{ fontWeight: serie.srMinutes > 0 ? 'bold' : 'normal', color: serie.srMinutes > 0 ? '#2196f3' : 'inherit' }}>
                                                            {serie.srMinutes > 0 ? Math.round(serie.srMinutes) : '-'}
                                                        </td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            {/* Non-imported Work Orders List */}
                            {this.state.nonImportedCharge && this.state.nonImportedCharge.count > 0 && (
                                <div className="pdc-series-list-section" style={{ marginTop: '20px' }}>
                                    <h4>
                                        <span style={{ color: '#9c27b0' }}>
                                            <FontAwesomeIcon icon={faInfoCircle} style={{ marginRight: '6px' }} />
                                            Liste des Work Orders non importés ({this.state.nonImportedCharge.count} — {this.state.nonImportedCharge.totalMinutes?.toFixed(2)} min)
                                        </span>
                                        <span style={{ marginLeft: 'auto', display: 'flex', gap: '8px' }}>
                                            <button
                                                className="pdc-btn-export"
                                                style={{ padding: '4px 10px', fontSize: '0.78rem', background: '#9c27b0' }}
                                                onClick={() => this.exportNonImportedCSV(this.state.nonImportedCharge, selectedShiftForCharge)}
                                                title="Exporter en CSV"
                                            >
                                                <FontAwesomeIcon icon={faDownload} /> CSV
                                            </button>
                                            <button
                                                className="pdc-btn-export"
                                                style={{ padding: '4px 10px', fontSize: '0.78rem', background: '#7b1fa2' }}
                                                onClick={() => this.exportNonImportedExcel(this.state.nonImportedCharge, selectedShiftForCharge)}
                                                title="Exporter en Excel"
                                            >
                                                <FontAwesomeIcon icon={faDownload} /> Excel
                                            </button>
                                        </span>
                                    </h4>
                                    <div className="pdc-series-table-scroll">
                                        <table className="pdc-series-table">
                                            <thead>
                                                <tr>
                                                    <th>WorkOrder (ID_demande)</th>
                                                    <th>Item (PartNumber)</th>
                                                    <th>Description</th>
                                                    <th>Quantité</th>
                                                    <th>Unit Time (min/pièce)</th>
                                                    <th>Total Time (min)</th>
                                                    <th>Source</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {(this.state.nonImportedCharge.details || []).map((d, i) => (
                                                    <tr key={i}>
                                                        <td>{d.idDemande}</td>
                                                        <td style={{ fontWeight: 600 }}>{d.partNumber}</td>
                                                        <td>{d.description || '-'}</td>
                                                        <td>{d.quantity}</td>
                                                        <td>{d.minutesPerPiece?.toFixed(2)}</td>
                                                        <td style={{ fontWeight: 600, color: '#9c27b0' }}>{d.estimatedMinutes?.toFixed(0)}</td>
                                                        <td>
                                                            <span style={{
                                                                fontSize: '0.7rem',
                                                                padding: '2px 6px',
                                                                borderRadius: 4,
                                                                backgroundColor: d.source === 'Fallback' ? '#ffebee' : '#e8f5e9',
                                                                color: d.source === 'Fallback' ? '#c62828' : '#2e7d32'
                                                            }}>
                                                                {d.source}
                                                            </span>
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                            <tfoot>
                                                <tr style={{ fontWeight: 'bold', borderTop: '2px solid #9c27b0' }}>
                                                    <td colSpan="5" style={{ textAlign: 'right' }}>Total Charge Non Importée:</td>
                                                    <td style={{ color: '#9c27b0' }}>{this.state.nonImportedCharge.totalMinutes?.toFixed(0)} min</td>
                                                    <td>{this.state.nonImportedCharge.count} OF</td>
                                                </tr>
                                            </tfoot>
                                        </table>
                                    </div>
                                </div>
                            )}

                            {/* Calculation Explanation */}
                            <div className="pdc-calculation-explanation">
                                <h4>
                                    <FontAwesomeIcon icon={faInfoCircle} style={{ marginRight: '8px' }} />
                                    Formules
                                </h4>
                                <ul>
                                    <li><strong>Chg %:</strong> Somme temps de coupe planifié (ajusté par l'efficience machine: temps × 1/efficience%) / (machines actives × temps configuré)</li>
                                    <li><strong>Chg % cap.inst.:</strong> Somme temps de coupe planifié / (capacité installée × temps configuré) — indicateur théorique, indépendant des pannes du jour</li>
                                    <li><strong>Source (temps de coupe):</strong> Validated (TimingModel validé) &gt; Real (TimingModel réel) &gt; TempsDeCoupe (estimé brut × nbrCouche pour LASER-DXF)</li>
                                    <li><strong>SR:</strong> Somme des durées des séries travaillées avant le début du shift</li>
                                    <li><strong>Ret prev:</strong> Somme du retard du shift précédent (séries non coupées)</li>
                                    <li><strong>Sum (Charge réelle):</strong> Chg - SR + Ret prev</li>
                                    <li><strong>Ret→next:</strong> Retard de ce shift passé au shift suivant</li>
                                    <li><strong>Temps Restant:</strong> (Fin shift - maintenant) × machines actives (shift actuel/suivant)</li>
                                    <li><strong>Ret ajusté:</strong> max(0, Ret→next brut - Temps Restant)</li>
                                </ul>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        );
    };

    renderAllHistoryList = () => {
        const { allHistoryList, showAllHistory } = this.state;

        return (
            <div className="pdc-all-history-container">
                <div 
                    className="pdc-all-history-header" 
                    onClick={() => this.setState({ showAllHistory: !showAllHistory })}
                >
                    <FontAwesomeIcon icon={faList} style={{ marginRight: '8px' }} />
                    Historique des États Machines ({allHistoryList.length})
                </div>
                
                {showAllHistory && (
                    <div className="pdc-all-history-content">
                        {allHistoryList.length === 0 ? (
                            <p className="pdc-no-history">Aucun historique disponible.</p>
                        ) : (
                            <table className="pdc-all-history-table">
                                <thead>
                                    <tr>
                                        <th>Machine</th>
                                        <th>Code</th>
                                        <th>Date Début</th>
                                        <th>Date Fin</th>
                                        <th>Cause</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {allHistoryList.map(item => (
                                        <tr key={item.id}>
                                            <td>{item.machine}</td>
                                            <td>
                                                <span 
                                                    className="pdc-status-badge"
                                                    style={{ 
                                                        backgroundColor: STATUS_CONFIG[item.codeEtat]?.color,
                                                        color: STATUS_CONFIG[item.codeEtat]?.textColor
                                                    }}
                                                >
                                                    {item.codeEtat}
                                                </span>
                                            </td>
                                            <td>{this.formatDateTimeFR(item.startDate)}</td>
                                            <td>
                                                {item.endDate 
                                                    ? this.formatDateTimeFR(item.endDate)
                                                    : <span className="pdc-active">En cours</span>
                                                }
                                            </td>
                                            <td>{item.cause || '-'}</td>
                                            <td>
                                                <div className="pdc-action-btns">
                                                    <button 
                                                        className="pdc-btn-edit"
                                                        onClick={() => this.handleEditFromList(item)}
                                                        title="Modifier"
                                                    >
                                                        <FontAwesomeIcon icon={faEdit} />
                                                    </button>
                                                    <button 
                                                        className="pdc-btn-delete"
                                                        onClick={() => this.handleDeleteStatus(item.id)}
                                                        title="Supprimer"
                                                    >
                                                        <FontAwesomeIcon icon={faTrash} />
                                                    </button>
                                                    {!item.endDate && (
                                                        <button 
                                                            className="pdc-btn-close-status"
                                                            onClick={() => this.handleCloseStatus(item.id)}
                                                            title="Clôturer"
                                                        >
                                                            Clôturer
                                                        </button>
                                                    )}
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}
            </div>
        );
    };

    handleEditFromList = async (item) => {
        // Load all history for this machine
        try {
            const machineHistoryResponse = await axios.get(`/api/etatMachineHistorique/machine/${item.machine}`);
            
            this.setState({
                showModal: true,
                selectedCell: null,
                statusHistory: [],
                machineHistory: machineHistoryResponse.data || [],
                editingItem: item,
                modalData: {
                    machine: item.machine,
                    startDate: this.toDateTimeLocal(item.startDate),
                    endDate: item.endDate ? this.toDateTimeLocal(item.endDate) : '',
                    codeEtat: item.codeEtat,
                    cause: item.cause || '',
                    action: item.action || ''
                }
            });
        } catch (error) {
            console.error('Error loading machine history:', error);
            this.setState({
                showModal: true,
                selectedCell: null,
                statusHistory: [],
                machineHistory: [],
                editingItem: item,
                modalData: {
                    machine: item.machine,
                    startDate: this.toDateTimeLocal(item.startDate),
                    endDate: item.endDate ? this.toDateTimeLocal(item.endDate) : '',
                    codeEtat: item.codeEtat,
                    cause: item.cause || '',
                    action: item.action || ''
                }
            });
        }
    };

    renderStatusGrid = () => {
        const { machinesByZone, statusGrid, loading, loadingPhase, showLoadIndicators } = this.state;
        const dates = this.getDateRange();
        const machineTypes = this.getUniqueMachineTypes();

        if (loading) {
            return (
                <div className="pdc-loading">
                    <FontAwesomeIcon icon={faSpinner} spin size="2x" />
                    <p>{loadingPhase || 'Chargement des données...'}</p>
                </div>
            );
        }

        if (!machinesByZone || Object.keys(machinesByZone).length === 0) {
            return (
                <div className="pdc-no-data">
                    <p>Aucune donnée disponible. Veuillez effectuer une recherche.</p>
                </div>
            );
        }

        // Calculate indicator columns span: 6 columns per machineType + 2 for Coupe + Global
        const indicatorColsPerType = 6; // Retard, Charge, SR, Sum, Capacité, Recommandé
        const totalIndicatorCols = showLoadIndicators ? (machineTypes.length * indicatorColsPerType + 2) : 0; // +1 for Coupe group +1 for Global

        return (
            <div className="pdc-grid-container">
                <table className="pdc-grid-table">
                    <thead>
                        {/* Zone header row */}
                        <tr className="pdc-zone-row">
                            <th className="pdc-date-header" rowSpan="2">Date</th>
                            <th className="pdc-shift-header" rowSpan="2">Shift</th>
                            
                            {/* Indicator columns headers */}
                            {showLoadIndicators && (
                                <>
                                    {machineTypes.map(type => (
                                        <th 
                                            key={`ind-${type}`}
                                            colSpan={indicatorColsPerType}
                                            className="pdc-indicator-header"
                                            style={{ backgroundColor: this.getMachineTypeColor(type) }}
                                        >
                                            {type}
                                        </th>
                                    ))}
                                    <th 
                                        className="pdc-indicator-header"
                                        colSpan={indicatorColsPerType}
                                        style={{ backgroundColor: '#7cb342', color: '#fff' }}
                                    >
                                        Coupe
                                    </th>
                                    <th className="pdc-indicator-header pdc-global-header" rowSpan="2">
                                        <FontAwesomeIcon icon={faChartBar} style={{ marginRight: '4px' }} />
                                        Global
                                    </th>
                                </>
                            )}

                            {Object.entries(machinesByZone).map(([zone, machines]) => (
                                <th 
                                    key={zone} 
                                    colSpan={machines.length}
                                    className="pdc-zone-header"
                                >
                                    {zone}
                                </th>
                            ))}
                        </tr>
                        
                        {/* Machine/Indicator sub-header row */}
                        <tr className="pdc-machine-row">
                            {/* Indicator sub-headers */}
                            {showLoadIndicators && machineTypes.map(type => (
                                <React.Fragment key={`sub-${type}`}>
                                    <th className="pdc-indicator-subheader" title="Retard du shift précédent">Ret</th>
                                    <th className="pdc-indicator-subheader" title="Charge actuelle (%)">Chg</th>
                                    <th className="pdc-indicator-subheader" title="Surpassant: séries planifiées ici mais déjà travaillées avant">SR</th>
                                    <th className="pdc-indicator-subheader" title="Somme Ret + Chg - SR">Sum</th>
                                    <th className="pdc-indicator-subheader" title="Machines disponibles (M uniquement)">Cap</th>
                                    <th className="pdc-indicator-subheader" title="Machines recommandées">Rec</th>
                                </React.Fragment>
                            ))}
                            {showLoadIndicators && (
                                <React.Fragment>
                                    <th className="pdc-indicator-subheader" title="Retard Coupe (Lectra + Lectra IP6)">Ret</th>
                                    <th className="pdc-indicator-subheader" title="Charge Coupe">Chg</th>
                                    <th className="pdc-indicator-subheader" title="Surpassant Coupe">SR</th>
                                    <th className="pdc-indicator-subheader" title="Somme Coupe">Sum</th>
                                    <th className="pdc-indicator-subheader" title="Capacité Coupe">Cap</th>
                                    <th className="pdc-indicator-subheader" title="Recommandé Coupe">Rec</th>
                                </React.Fragment>
                            )}

                            {Object.entries(machinesByZone).map(([zone, machines]) => (
                                machines.map(machine => (
                                    <th 
                                        key={machine.nom}
                                        className="pdc-machine-header"
                                        style={{ 
                                            backgroundColor: this.getMachineTypeColor(machine.machineType?.name)
                                        }}
                                        title={`${machine.nom} - ${machine.machineType?.name || 'N/A'}`}
                                    >
                                        {machine.nom}
                                    </th>
                                ))
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {dates.map(date => (
                            [1, 2, 3].map(shift => {
                                const { currentShift, currentDate } = this.state;
                                const isCurrentShift = currentShift && 
                                    date === currentDate && 
                                    shift === currentShift.shift;
                                
                                return (
                                <tr 
                                    key={`${date}-${shift}`} 
                                    className={`pdc-data-row${isCurrentShift ? ' pdc-current-shift-row' : ''}`}
                                >
                                    {shift === 1 && (
                                        <td className="pdc-date-cell" rowSpan="3">
                                            {this.formatDate(date)}
                                        </td>
                                    )}
                                    <td className="pdc-shift-cell">
                                        {shift}
                                    </td>

                                    {/* Indicator data cells */}
                                    {showLoadIndicators && (
                                        <>
                                            {machineTypes.map(type => {
                                                const ind = this.getLoadIndicator(date, shift, type);
                                                const isOverloaded = parseFloat(ind.sum) > 100;
                                                return (
                                                    <React.Fragment key={`ind-data-${type}-${date}-${shift}`}>
                                                        <td className={`pdc-indicator-cell ${ind.retard > 0 ? 'pdc-retard' : ''}`}>
                                                            {ind.retard}
                                                        </td>
                                                        <td className={`pdc-indicator-cell ${parseFloat(ind.charge) > 100 ? 'pdc-overload' : ''}`}>
                                                            {ind.charge}%
                                                        </td>
                                                        <td className={`pdc-indicator-cell ${parseFloat(ind.sr) > 0 ? 'pdc-sr' : ''}`}>
                                                            {ind.sr}%
                                                        </td>
                                                        <td className={`pdc-indicator-cell ${isOverloaded ? 'pdc-overload' : ''}`}>
                                                            {ind.sum}
                                                        </td>
                                                        <td className="pdc-indicator-cell pdc-capacite">
                                                            {ind.capacite}/{ind.totalMachines}
                                                        </td>
                                                        <td className={`pdc-indicator-cell ${ind.recommended > ind.capacite ? 'pdc-need-more' : 'pdc-ok'}`}>
                                                            {ind.recommended}
                                                        </td>
                                                    </React.Fragment>
                                                );
                                            })}
                                            {/* Coupe group (Lectra + Lectra IP6) */}
                                            {(() => {
                                                const coupeInd = this.getCoupeGroupIndicator(date, shift);
                                                const coupeOverloaded = parseFloat(coupeInd.sum) > 100;
                                                return (
                                                    <React.Fragment>
                                                        <td className={`pdc-indicator-cell ${coupeInd.retard > 0 ? 'pdc-retard' : ''}`}>
                                                            {coupeInd.retard}
                                                        </td>
                                                        <td className={`pdc-indicator-cell ${parseFloat(coupeInd.charge) > 100 ? 'pdc-overload' : ''}`}>
                                                            {coupeInd.charge}%
                                                        </td>
                                                        <td className={`pdc-indicator-cell ${parseFloat(coupeInd.sr) > 0 ? 'pdc-sr' : ''}`}>
                                                            {coupeInd.sr}%
                                                        </td>
                                                        <td className={`pdc-indicator-cell ${coupeOverloaded ? 'pdc-overload' : ''}`}>
                                                            {coupeInd.sum}
                                                        </td>
                                                        <td className="pdc-indicator-cell pdc-capacite">
                                                            {coupeInd.capacite}/{coupeInd.totalMachines}
                                                        </td>
                                                        <td className={`pdc-indicator-cell ${coupeInd.recommended > coupeInd.capacite ? 'pdc-need-more' : 'pdc-ok'}`}>
                                                            {coupeInd.recommended}
                                                        </td>
                                                    </React.Fragment>
                                                );
                                            })()}
                                            <td className="pdc-indicator-cell pdc-global">
                                                {this.getGlobalCharge(date, shift)}%
                                            </td>
                                        </>
                                    )}

                                    {Object.entries(machinesByZone).map(([zone, machines]) => (
                                        machines.map(machine => {
                                            const status = statusGrid[machine.nom]?.[date]?.[shift] || 'M';
                                            const config = STATUS_CONFIG[status] || STATUS_CONFIG.M;
                                            
                                            return (
                                                <td
                                                    key={`${machine.nom}-${date}-${shift}`}
                                                    className="pdc-status-cell"
                                                    style={{
                                                        backgroundColor: config.color,
                                                        color: config.textColor,
                                                        cursor: 'pointer'
                                                    }}
                                                    onClick={() => this.handleCellClick(machine.nom, date, shift)}
                                                    title={`${machine.nom} - ${date} Shift ${shift}: ${config.name}`}
                                                >
                                                    {status}
                                                </td>
                                            );
                                        })
                                    ))}
                                </tr>)
                            })
                        ))}
                    </tbody>
                </table>
            </div>
        );
    };

    /**
     * Get next shift info relative to current shift.
     */
    getNextShiftInfo = () => {
        const { currentShift, currentDate } = this.state;
        if (!currentShift) return null;
        
        const currentShiftNum = currentShift.shift;
        let nextDate = currentDate;
        let nextShift;
        
        if (currentShiftNum === 3) {
            // Next is shift 1 of next day
            const d = new Date(currentDate);
            d.setDate(d.getDate() + 1);
            nextDate = d.toISOString().split('T')[0];
            nextShift = 1;
        } else {
            nextShift = currentShiftNum + 1;
        }
        
        return { date: nextDate, shift: nextShift };
    };

    /**
     * Render the next shift preparation section.
     * Shows per machine type: total cutting time needed, machines recommended,
     * current machines at M status, surplus/deficit, and retard from current shift.
     * Charge is based on estimated cutting times (no dateDebutCoupe/dateFinCoupe needed).
     * Backend already applies LASER-DXF nbrCouche and Gerber x2 multipliers.
     */
    renderNextShiftPreparation = () => {
        const { currentShift, currentDate, aggregatedByMachineType, statusGrid, machinesByZone,
            nextShiftSeries, nextShiftLoadingSeries, showNextShiftSeries } = this.state;

        if (!currentShift || !aggregatedByMachineType || Object.keys(aggregatedByMachineType).length === 0) {
            return null;
        }

        const nextInfo = this.getNextShiftInfo();
        if (!nextInfo) return null;

        // Compute time-left on the CURRENT shift to adjust retard passed to next
        const currentShiftTimes = this.getShiftTimes(currentDate, currentShift.shift);
        const now = new Date();
        const isCurrentShiftActive = now >= currentShiftTimes.start && now <= currentShiftTimes.end;

        const machineTypes = this.getUniqueMachineTypes();
        const preparations = [];

        let globalTotalCutting = 0;
        let globalRetard = 0;
        let globalRetardAdjusted = 0;
        let globalTempsRestant = 0;
        let globalSR = 0;
        let globalMachinesAvailable = 0;
        let globalTotalMachines = 0;
        let globalRecommended = 0;
        let globalAvailableTime = 0;

        machineTypes.forEach(machineType => {
            let totalMachines = 0;
            let machinesAvailableNextShift = 0;
            
            Object.values(machinesByZone).forEach(machines => {
                machines.forEach(machine => {
                    if (machine.machineType?.name === machineType) {
                        totalMachines++;
                        const status = statusGrid[machine.nom]?.[nextInfo.date]?.[nextInfo.shift] || 'M';
                        if (status === 'M') {
                            machinesAvailableNextShift++;
                        }
                    }
                });
            });

            const indicator = this.getLoadIndicator(nextInfo.date, nextInfo.shift, machineType);

            // Get next shift data from machineType-level aggregation
            const nextTypeData = aggregatedByMachineType?.[machineType]?.[nextInfo.date]?.[nextInfo.shift];
            const nextShiftCutting = nextTypeData?.total || 0;
            const nextShiftSR = nextTypeData?.sr || 0;

            let prevShift = nextInfo.shift - 1;
            let prevDate = nextInfo.date;
            if (nextInfo.shift === 1) {
                prevShift = 3;
                const d = new Date(nextInfo.date);
                d.setDate(d.getDate() - 1);
                prevDate = d.toISOString().split('T')[0];
            }
            const prevTypeData = aggregatedByMachineType?.[machineType]?.[prevDate]?.[prevShift];
            const rawCurrentShiftRetard = prevTypeData?.notCut || 0;

            // Compute time-left for the current shift on this machine type
            // to produce an adjusted retard that will truly pass to the next shift
            const groupeKey = (machineType === 'Lectra' || machineType === 'Lectra IP6' || machineType === 'Gerber') ? 'Coupe' : 'Laser';
            const capEntry = this.getCapaciteInstalleeEntry(prevDate, prevShift, groupeKey);

            // Count machines available on the CURRENT (prev) shift for this type
            let machinesCurrentShift = 0;
            Object.values(machinesByZone).forEach(machines => {
                machines.forEach(machine => {
                    if (machine.machineType?.name === machineType) {
                        const status = statusGrid[machine.nom]?.[prevDate]?.[prevShift] || 'M';
                        if (status === 'M') {
                            machinesCurrentShift++;
                        }
                    }
                });
            });

            const tempsRestantCurrentShift = isCurrentShiftActive
                ? this.getCurrentShiftProductiveMinutesLeft(prevDate, prevShift, machinesCurrentShift, capEntry, now)
                : 0;
            const currentShiftRetard = this.getAdjustedRetardCarryoverForTypes([machineType], prevDate, prevShift, groupeKey, now);
            const retardAdjusted = Math.max(0, rawCurrentShiftRetard - tempsRestantCurrentShift);

            const totalWork = nextShiftCutting + currentShiftRetard;
            const nextCapEntry = this.getCapaciteInstalleeEntry(nextInfo.date, nextInfo.shift, groupeKey);
            const availableTime = this.getShiftProductiveCapacityMinutes(machinesAvailableNextShift, nextCapEntry);

            // Percentages must match renderStatusGrid for the same next shift.
            const retPercentage = parseFloat(indicator.retard);
            const chgPercentage = parseFloat(indicator.charge);
            const srPercentage = parseFloat(indicator.sr);
            const sumPercentage = parseFloat(indicator.sum);
            const loadPercentage = availableTime > 0 ? (totalWork / availableTime) * 100 : 0;

            const recommended = indicator.recommended;

            const surplus = machinesAvailableNextShift - recommended;

            preparations.push({
                machineType,
                nextShiftCutting,
                currentShiftRetard,
                tempsRestantCurrentShift,
                retardAdjusted,
                nextShiftSR,
                totalWork,
                machinesAvailableNextShift,
                totalMachines,
                availableTime,
                retPercentage,
                chgPercentage,
                srPercentage,
                sumPercentage,
                loadPercentage,
                recommended,
                surplus
            });

            globalTotalCutting += nextShiftCutting;
            globalRetard += currentShiftRetard;
            globalRetardAdjusted += retardAdjusted;
            globalTempsRestant += tempsRestantCurrentShift;
            globalSR += nextShiftSR;
            globalMachinesAvailable += machinesAvailableNextShift;
            globalTotalMachines += totalMachines;
            globalRecommended += recommended;
            globalAvailableTime += availableTime;
        });

        const globalTotalWork = globalTotalCutting + globalRetard;
        const globalRetPercentage = globalAvailableTime > 0 ? (globalRetard / globalAvailableTime) * 100 : 0;
        const globalChgPercentage = globalAvailableTime > 0 ? (globalTotalCutting / globalAvailableTime) * 100 : 0;
        const globalSRPercentage = globalAvailableTime > 0 ? (globalSR / globalAvailableTime) * 100 : 0;
        const globalSumPercentage = globalRetPercentage + globalChgPercentage - globalSRPercentage;
        const globalSurplus = globalMachinesAvailable - globalRecommended;

        return (
            <div className="pdc-next-shift-preparation">
                <h3>
                    <FontAwesomeIcon icon={faCalculator} style={{ marginRight: '8px' }} />
                    Préparation Shift Suivant — {nextInfo.date} Shift {nextInfo.shift}
                </h3>
                <p className="pdc-next-shift-subtitle">
                    Les pourcentages affichés ici utilisent la même logique que renderStatusGrid pour le shift {nextInfo.shift} du {nextInfo.date}.
                </p>
                <table className="pdc-next-shift-table">
                    <thead>
                        <tr>
                            <th>Type Machine</th>
                            <th title="Retard du shift précédent / capacité du shift affiché">Ret %</th>
                            <th title="Charge planifiée du shift affiché / capacité du shift affiché">Chg %</th>
                            <th title="SR du shift affiché / capacité du shift affiché">SR %</th>
                            <th title="Sum = Ret + Chg - SR">Sum %</th>
                            <th title="Machines en statut M pour le prochain shift">Cap</th>
                            <th title="Machines recommandées avec la même formule que renderStatusGrid">Rec</th>
                            <th title="Excédent (+) ou déficit (-) de machines">Surplus/Déficit</th>
                            <th title="Temps restant sur le shift courant (min) = timeLeft × machines × efficience" style={{ color: '#009688' }}>T. Rest (min)</th>
                            <th title="Retard ajusté = max(0, Retard - Temps Restant)" style={{ color: '#ff6600' }}>Ret adj (min)</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        {preparations.map(prep => {
                            const surplusClass = prep.surplus > 0 ? 'pdc-surplus-positive' : 
                                                 prep.surplus < 0 ? 'pdc-surplus-negative' : 'pdc-surplus-neutral';
                            const sumClass = prep.sumPercentage > 100 ? 'pdc-load-danger' :
                                              prep.sumPercentage > 80 ? 'pdc-load-warning' : 'pdc-load-ok';
                            return (
                                <tr key={prep.machineType}>
                                    <td>
                                        <span 
                                            className="pdc-machine-type-badge"
                                            style={{ backgroundColor: this.getMachineTypeColor(prep.machineType) }}
                                        >
                                            {prep.machineType}
                                        </span>
                                    </td>
                                    <td className={`pdc-indicator-cell ${prep.retPercentage > 0 ? 'pdc-retard' : ''}`}>
                                        {prep.retPercentage.toFixed(1)}%
                                    </td>
                                    <td className={`pdc-indicator-cell ${prep.chgPercentage > 100 ? 'pdc-overload' : ''}`}>
                                        {prep.chgPercentage.toFixed(1)}%
                                    </td>
                                    <td className={`pdc-indicator-cell ${prep.srPercentage > 0 ? 'pdc-sr' : ''}`}>
                                        {prep.srPercentage.toFixed(1)}%
                                    </td>
                                    <td className={`pdc-indicator-cell ${sumClass}`} style={{ fontWeight: 'bold' }}>
                                        {prep.sumPercentage.toFixed(1)}%
                                    </td>
                                    <td className="pdc-indicator-cell pdc-capacite">
                                        {prep.machinesAvailableNextShift}/{prep.totalMachines}
                                    </td>
                                    <td className="pdc-cell-number" style={{ fontWeight: 'bold' }}>{prep.recommended}</td>
                                    <td className={`pdc-cell-number ${surplusClass}`}>
                                        {prep.surplus > 0 ? `+${prep.surplus}` : prep.surplus}
                                        {prep.surplus > 0 && ' ▼'}
                                        {prep.surplus < 0 && ' ▲'}
                                    </td>
                                    <td style={{ color: '#009688', fontWeight: 'bold' }}>
                                        {isCurrentShiftActive ? prep.tempsRestantCurrentShift.toFixed(0) : '-'}
                                    </td>
                                    <td style={{ color: prep.retardAdjusted > 0 ? '#ff6600' : 'inherit', fontWeight: 'bold' }}>
                                        {isCurrentShiftActive ? prep.retardAdjusted.toFixed(0) : '-'}
                                    </td>
                                    <td>
                                        {prep.surplus > 0 && (
                                            <span className="pdc-action-hint" title="Des machines peuvent être mises en arrêt">
                                                Arrêter {prep.surplus} machine(s)
                                            </span>
                                        )}
                                        {prep.surplus < 0 && (
                                            <span className="pdc-action-hint pdc-action-urgent" title="Machines supplémentaires nécessaires">
                                                Démarrer {Math.abs(prep.surplus)} machine(s)
                                            </span>
                                        )}
                                        {prep.surplus === 0 && (
                                            <span className="pdc-action-hint pdc-action-ok">✓ OK</span>
                                        )}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                    <tfoot>
                        <tr className="pdc-next-shift-total">
                            <td style={{ fontWeight: 'bold' }}>GLOBAL</td>
                            <td className={`pdc-indicator-cell ${globalRetPercentage > 0 ? 'pdc-retard' : ''}`} style={{ fontWeight: 'bold' }}>
                                {globalRetPercentage.toFixed(1)}%
                            </td>
                            <td className={`pdc-indicator-cell ${globalChgPercentage > 100 ? 'pdc-overload' : ''}`} style={{ fontWeight: 'bold' }}>
                                {globalChgPercentage.toFixed(1)}%
                            </td>
                            <td className={`pdc-indicator-cell ${globalSRPercentage > 0 ? 'pdc-sr' : ''}`} style={{ fontWeight: 'bold' }}>
                                {globalSRPercentage.toFixed(1)}%
                            </td>
                            <td className={`pdc-indicator-cell ${globalSumPercentage > 100 ? 'pdc-load-danger' : globalSumPercentage > 80 ? 'pdc-load-warning' : 'pdc-load-ok'}`} style={{ fontWeight: 'bold' }}>
                                {globalSumPercentage.toFixed(1)}%
                            </td>
                            <td className="pdc-indicator-cell pdc-capacite" style={{ fontWeight: 'bold' }}>{globalMachinesAvailable}/{globalTotalMachines}</td>
                            <td className="pdc-cell-number" style={{ fontWeight: 'bold' }}>{globalRecommended}</td>
                            <td className={`pdc-cell-number ${globalSurplus > 0 ? 'pdc-surplus-positive' : globalSurplus < 0 ? 'pdc-surplus-negative' : 'pdc-surplus-neutral'}`} style={{ fontWeight: 'bold' }}>
                                {globalSurplus > 0 ? `+${globalSurplus}` : globalSurplus}
                            </td>
                            <td style={{ color: '#009688', fontWeight: 'bold' }}>
                                {isCurrentShiftActive ? globalTempsRestant.toFixed(0) : '-'}
                            </td>
                            <td style={{ color: globalRetardAdjusted > 0 ? '#ff6600' : 'inherit', fontWeight: 'bold' }}>
                                {isCurrentShiftActive ? globalRetardAdjusted.toFixed(0) : '-'}
                            </td>
                            <td></td>
                        </tr>
                    </tfoot>
                </table>

                {/* Load series button + series list for next shift */}
                <div style={{ marginTop: '15px', display: 'flex', gap: '10px', alignItems: 'center' }}>
                    <button
                        className="pdc-btn-search"
                        onClick={() => this.loadNextShiftSeries(nextInfo.date, nextInfo.shift)}
                        disabled={nextShiftLoadingSeries}
                        style={{ padding: '6px 14px', fontSize: '0.85rem' }}
                    >
                        <FontAwesomeIcon icon={nextShiftLoadingSeries ? faSpinner : faList} spin={nextShiftLoadingSeries} />
                        {showNextShiftSeries ? 'Recharger Séries' : 'Charger Séries'}
                    </button>
                    {showNextShiftSeries && nextShiftSeries && nextShiftSeries.length > 0 && (
                        <>
                            <button
                                className="pdc-btn-export"
                                style={{ padding: '4px 10px', fontSize: '0.78rem' }}
                                onClick={() => this.exportSeriesCSV(nextShiftSeries, nextInfo)}
                            >
                                <FontAwesomeIcon icon={faDownload} /> CSV
                            </button>
                            <button
                                className="pdc-btn-export"
                                style={{ padding: '4px 10px', fontSize: '0.78rem', background: '#1976d2' }}
                                onClick={() => this.exportSeriesExcel(nextShiftSeries, nextInfo)}
                            >
                                <FontAwesomeIcon icon={faDownload} /> Excel
                            </button>
                        </>
                    )}
                </div>

                {showNextShiftSeries && nextShiftSeries && (
                    <div style={{ marginTop: '15px' }}>
                        <h4 style={{ color: '#EE3124', borderBottom: '1px solid #dee2e6', paddingBottom: '8px' }}>
                            Séries Shift Suivant ({nextShiftSeries.length})
                        </h4>
                        {nextShiftSeries.length === 0 ? (
                            <p style={{ color: '#888' }}>Aucune série planifiée pour le shift suivant.</p>
                        ) : (
                            <div className="pdc-series-table-scroll">
                                <table className="pdc-series-table">
                                    <thead>
                                        <tr>
                                            <th>Série</th>
                                            <th title="Séquence (CuttingRequest.sequence)">Séquence</th>
                                            <th title="CuttingPlan.id (clé interne)">Plan ID</th>
                                            <th title="CuttingPlan.cmsId (référence CMS)">CMS Id</th>
                                            <th>Placement</th>
                                            <th>Type Machine</th>
                                            <th>Table Coupe</th>
                                            <th>Temps (min)</th>
                                            <th>Source</th>
                                            <th>Temps Changement Série</th>
                                            <th>Statut</th>
                                            <th>Retard (min)</th>
                                            <th>SR (min)</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {nextShiftSeries.map(serie => (
                                            <tr key={serie.serie}>
                                                <td>{serie.serie}</td>
                                                <td>{serie.sequence || '-'}</td>
                                                <td>{serie.cuttingPlanId || '-'}</td>
                                                <td>{serie.cmsId || '-'}</td>
                                                <td className="pdc-cell-placement">{serie.placement || '-'}</td>
                                                <td>
                                                    <span 
                                                        className="pdc-machine-type-badge"
                                                        style={{ backgroundColor: this.getMachineTypeColor(serie.machine || 'Unknown'), fontSize: '11px' }}
                                                    >
                                                        {serie.machine || 'Unknown'}
                                                    </span>
                                                </td>
                                                <td>{serie.tableCoupe || '-'}</td>
                                                <td className="pdc-cell-time">
                                                    {serie.effectiveCuttingTime?.toFixed(2) || '-'}
                                                </td>
                                                <td>
                                                    <span className={`pdc-source-badge pdc-source-${serie.cuttingTimeSource?.toLowerCase()}`}>
                                                        {serie.cuttingTimeSource || '-'}
                                                    </span>
                                                </td>
                                                <td className="pdc-cell-time">
                                                    {serie.tempsChangementSerie?.toFixed(0) || '-'}
                                                </td>
                                                <td>
                                                    {serie.isCut ? (
                                                        <span className="pdc-status-cut">✓ Coupé</span>
                                                    ) : serie.isCutting ? (
                                                        <span className="pdc-status-cutting">⏳ En Cours</span>
                                                    ) : (
                                                        <span className="pdc-status-waiting">Planifié</span>
                                                    )}
                                                </td>
                                                <td style={{ fontWeight: serie.retardMinutes > 0 ? 'bold' : 'normal', color: serie.retardMinutes > 0 ? '#d32f2f' : 'inherit' }}>
                                                    {serie.retardMinutes > 0 ? Math.round(serie.retardMinutes) : '-'}
                                                </td>
                                                <td style={{ fontWeight: serie.srMinutes > 0 ? 'bold' : 'normal', color: serie.srMinutes > 0 ? '#2196f3' : 'inherit' }}>
                                                    {serie.srMinutes > 0 ? Math.round(serie.srMinutes) : '-'}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                )}
            </div>
        );
    };

    /**
     * Load series for the next shift preparation section.
     * Derives from already-loaded seriesData.
     */
    loadNextShiftSeries = (date, shift) => {
        const { seriesData, machinesByZone } = this.state;
        const shiftStr = String(shift);

        // Filter series for this date/shift from already-loaded data
        const shiftSeries = seriesData.filter(s => s.dueDate === date && String(s.dueShift) === shiftStr);

        const shiftNum = parseInt(shiftStr, 10) || 1;
        const shiftTimes = this.getShiftTimes(date, shiftNum);
        const shiftStart = shiftTimes.start;
        const shiftEnd = shiftTimes.end;

        const nextShiftSeries = shiftSeries.map(serie => {
            const ect = serie.effectiveCuttingTime || 0;
            const dateDebutCoupe = serie.dateDebutCoupe ? new Date(serie.dateDebutCoupe) : null;
            const dateFinCoupe = serie.dateFinCoupe ? new Date(serie.dateFinCoupe) : null;

            const isCutting = dateDebutCoupe !== null && dateFinCoupe === null;
            const isCut = dateFinCoupe !== null;

            let retardMinutes = 0;
            let srMinutes = 0;
            if (dateDebutCoupe && dateDebutCoupe < shiftStart) {
                const srEnd = (dateFinCoupe && dateFinCoupe < shiftStart) ? dateFinCoupe : shiftStart;
                srMinutes = Math.max(0, (srEnd - dateDebutCoupe) / 60000);
                if (srMinutes > ect) srMinutes = ect;
            }
            if (!dateDebutCoupe) {
                retardMinutes = ect;
            } else if (dateDebutCoupe > shiftEnd) {
                retardMinutes = ect;
            } else if (!dateFinCoupe && new Date() > shiftEnd) {
                retardMinutes = ect;
            } else if (dateFinCoupe && dateFinCoupe > shiftEnd) {
                retardMinutes = Math.min((dateFinCoupe - shiftEnd) / 60000, ect);
            }

            return {
                serie: serie.serie || '',
                sequence: serie.sequence || '',
                cuttingPlanId: serie.cuttingPlanId || null,
                cmsId: serie.cmsId || null,
                placement: serie.placement || '',
                machine: serie.machine || 'Unknown',
                tableCoupe: serie.tableCoupe || '',
                effectiveCuttingTime: ect,
                // Provenance (passed through from the server now that getSeriesForDateRange surfaces it).
                cuttingTimeSource: serie.cuttingTimeSource || '',
                isCut,
                isCutting,
                retardMinutes,
                srMinutes
            };
        });

        this.setState({
            nextShiftSeries,
            showNextShiftSeries: true,
            nextShiftLoadingSeries: false
        });
    };

    renderNonImportedChargeModal = () => {
        const { showNonImportedChargeModal, nonImportedCharge, selectedShiftForCharge } = this.state;
        if (!showNonImportedChargeModal || !nonImportedCharge) return null;

        const details = nonImportedCharge.details || [];

        return (
            <div className="pdc-modal-overlay" onClick={() => this.setState({ showNonImportedChargeModal: false })}>
                <div className="pdc-modal pdc-modal-large" onClick={e => e.stopPropagation()}>
                    <div className="pdc-modal-header">
                        <h3>
                            <FontAwesomeIcon icon={faInfoCircle} style={{ marginRight: '8px', color: '#9c27b0' }} />
                            Charge Non Importée — {selectedShiftForCharge?.date} Shift {selectedShiftForCharge?.shift}
                        </h3>
                        <button className="pdc-modal-close" onClick={() => this.setState({ showNonImportedChargeModal: false })}>
                            <FontAwesomeIcon icon={faTimes} />
                        </button>
                    </div>
                    <div className="pdc-modal-content">
                        <div style={{ marginBottom: 16, padding: 12, background: '#f3e5f5', borderRadius: 8 }}>
                            <strong>Total: {nonImportedCharge.totalMinutes?.toFixed(0)} min</strong>
                            {' | '}
                            <span>{nonImportedCharge.count} ordres de fabrication</span>
                        </div>
                        {details.length === 0 ? (
                            <p>Aucun détail disponible.</p>
                        ) : (
                            <div style={{ maxHeight: '60vh', overflowY: 'auto' }}>
                                <table className="pdc-history-table" style={{ fontSize: '0.85rem' }}>
                                    <thead>
                                        <tr>
                                            <th>ID Demande</th>
                                            <th>Part Number</th>
                                            <th>Description</th>
                                            <th>Qté</th>
                                            <th>Min/pièce</th>
                                            <th>Min estimés</th>
                                            <th>Source</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {details.map((d, i) => (
                                            <tr key={i}>
                                                <td>{d.idDemande}</td>
                                                <td style={{ fontWeight: 600 }}>{d.partNumber}</td>
                                                <td>{d.description || '-'}</td>
                                                <td>{d.quantity}</td>
                                                <td>{d.minutesPerPiece?.toFixed(2)}</td>
                                                <td style={{ fontWeight: 600, color: '#9c27b0' }}>{d.estimatedMinutes?.toFixed(0)}</td>
                                                <td>
                                                    <span style={{
                                                        fontSize: '0.7rem',
                                                        padding: '2px 6px',
                                                        borderRadius: 4,
                                                        backgroundColor: d.source === 'Fallback' ? '#ffebee' : '#e8f5e9',
                                                        color: d.source === 'Fallback' ? '#c62828' : '#2e7d32'
                                                    }}>
                                                        {d.source}
                                                    </span>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                    <div className="pdc-modal-footer">
                        <button className="pdc-btn-cancel" onClick={() => this.setState({ showNonImportedChargeModal: false })}>
                            Fermer
                        </button>
                    </div>
                </div>
            </div>
        );
    };

    renderModal = () => {
        const { showModal, modalData, statusHistory, selectedCell, editingItem, machineHistory } = this.state;

        if (!showModal || !modalData) return null;

        const modalTitle = editingItem 
            ? `Modifier: ${modalData.machine}` 
            : (selectedCell 
                ? `${selectedCell.machine} - ${selectedCell.date} Shift ${selectedCell.shift}` 
                : `Nouveau Status - ${modalData.machine}`);

        return (
            <div className="pdc-modal-overlay" onClick={() => this.setState({ showModal: false, editingItem: null })}>
                <div className="pdc-modal pdc-modal-large" onClick={e => e.stopPropagation()}>
                    <div className="pdc-modal-header">
                        <h3>
                            <FontAwesomeIcon icon={faEdit} style={{ marginRight: '8px' }} />
                            {modalTitle}
                        </h3>
                        <button 
                            className="pdc-modal-close"
                            onClick={() => this.setState({ showModal: false, editingItem: null })}
                        >
                            <FontAwesomeIcon icon={faTimes} />
                        </button>
                    </div>

                    <div className="pdc-modal-content">
                        {/* All history for this machine - to see existing intervals */}
                        {machineHistory.length > 0 && (
                            <div className="pdc-history-section">
                                <h4>
                                    <FontAwesomeIcon icon={faList} style={{ marginRight: '8px' }} />
                                    Tous les intervalles de {modalData.machine} ({machineHistory.length})
                                </h4>
                                <div className="pdc-machine-history-scroll">
                                    <table className="pdc-history-table">
                                        <thead>
                                            <tr>
                                                <th>Code</th>
                                                <th>Début</th>
                                                <th>Fin</th>
                                                <th>Cause</th>
                                                <th>Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {machineHistory.map(item => (
                                                <tr key={item.id} className={editingItem?.id === item.id ? 'pdc-editing-row' : ''}>
                                                    <td>
                                                        <span 
                                                            className="pdc-status-badge"
                                                            style={{ 
                                                                backgroundColor: STATUS_CONFIG[item.codeEtat]?.color,
                                                                color: STATUS_CONFIG[item.codeEtat]?.textColor
                                                            }}
                                                        >
                                                            {item.codeEtat}
                                                        </span>
                                                    </td>
                                                    <td>{this.formatDateTimeFR(item.startDate)}</td>
                                                    <td>
                                                        {item.endDate 
                                                            ? this.formatDateTimeFR(item.endDate)
                                                            : <span className="pdc-active">En cours</span>
                                                        }
                                                    </td>
                                                    <td>{item.cause || '-'}</td>
                                                    <td>
                                                        <div className="pdc-action-btns">
                                                            <button 
                                                                className="pdc-btn-edit"
                                                                onClick={() => this.handleEditStatus(item)}
                                                                title="Modifier"
                                                            >
                                                                <FontAwesomeIcon icon={faEdit} />
                                                            </button>
                                                            <button 
                                                                className="pdc-btn-delete"
                                                                onClick={() => this.handleDeleteStatus(item.id)}
                                                                title="Supprimer"
                                                            >
                                                                <FontAwesomeIcon icon={faTrash} />
                                                            </button>
                                                            {!item.endDate && (
                                                                <button 
                                                                    className="pdc-btn-close-status"
                                                                    onClick={() => this.handleCloseStatus(item.id)}
                                                                >
                                                                    Clôturer
                                                                </button>
                                                            )}
                                                        </div>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}

                        {/* New/Edit status form */}
                        <div className="pdc-form-section">
                            <h4>
                                <FontAwesomeIcon icon={editingItem ? faEdit : faPlus} style={{ marginRight: '8px' }} />
                                {editingItem ? 'Modifier Status' : 'Nouveau Status'}
                            </h4>
                            <div className="pdc-form-grid">
                                <div className="pdc-form-group">
                                    <label>Machine</label>
                                    <input
                                        type="text"
                                        value={modalData.machine}
                                        onChange={e => this.handleModalChange('machine', e.target.value)}
                                        readOnly={!!selectedCell}
                                    />
                                </div>
                                <div className="pdc-form-group">
                                    <label>Code Status</label>
                                    <select
                                        value={modalData.codeEtat}
                                        onChange={e => this.handleModalChange('codeEtat', e.target.value)}
                                    >
                                        {Object.entries(STATUS_CONFIG).map(([code, config]) => (
                                            <option key={code} value={code}>
                                                {code} - {config.name}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div className="pdc-form-group">
                                    <label>Date Début <span style={{ color: 'red' }}>*</span></label>
                                    <input
                                        type="datetime-local"
                                        className="pdc-datetime-picker"
                                        value={modalData.startDate}
                                        onChange={e => this.handleModalChange('startDate', e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="pdc-form-group" style={{ position: 'relative' }}>
                                    <label>Date Fin</label>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                                        <input
                                            type="datetime-local"
                                            className="pdc-datetime-picker"
                                            value={modalData.endDate || ''}
                                            onChange={e => this.handleModalChange('endDate', e.target.value)}
                                            style={{ flex: 1 }}
                                        />
                                        {modalData.endDate && (
                                            <button
                                                type="button"
                                                className="pdc-btn-clear-date"
                                                onClick={() => this.handleModalChange('endDate', '')}
                                                title="Effacer la date de fin"
                                                style={{
                                                    background: '#dc3545',
                                                    color: 'white',
                                                    border: 'none',
                                                    borderRadius: '4px',
                                                    padding: '8px 12px',
                                                    cursor: 'pointer',
                                                    fontSize: '16px',
                                                    fontWeight: 'bold'
                                                }}
                                            >
                                                ✕
                                            </button>
                                        )}
                                    </div>
                                </div>
                                <div className="pdc-form-group full-width">
                                    <label>Cause</label>
                                    <textarea
                                        value={modalData.cause}
                                        onChange={e => this.handleModalChange('cause', e.target.value)}
                                        placeholder="Description de la cause..."
                                    />
                                </div>
                                <div className="pdc-form-group full-width">
                                    <label>Action</label>
                                    <textarea
                                        value={modalData.action}
                                        onChange={e => this.handleModalChange('action', e.target.value)}
                                        placeholder="Action entreprise..."
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="pdc-modal-footer">
                        {editingItem && (
                            <button 
                                className="pdc-btn-cancel"
                                onClick={this.handleCancelEdit}
                                style={{ marginRight: 'auto' }}
                            >
                                Annuler Modification
                            </button>
                        )}
                        <button 
                            className="pdc-btn-cancel"
                            onClick={() => this.setState({ showModal: false, editingItem: null })}
                        >
                            Fermer
                        </button>
                        <button 
                            className="pdc-btn-save"
                            onClick={this.handleSaveStatus}
                        >
                            <FontAwesomeIcon icon={faSave} style={{ marginRight: '8px' }} />
                            {editingItem ? 'Mettre à jour' : 'Enregistrer'}
                        </button>
                    </div>
                </div>
            </div>
        );
    };

    render() {
        const { dateDebut, dateFin, error, loading, showLoadIndicators } = this.state;

        return (
            <div className="pdc-container">
                <div className="pdc-header">
                    <h1>Plan de Charge - Machines de Coupe</h1>
                </div>

                {/* Search Controls */}
                <div className="pdc-controls">
                    <div className="pdc-date-controls">
                        {!this.props.embeddedDate && (
                            <>
                                <div className="pdc-form-group">
                                    <label>Date Début</label>
                                    <input
                                        type="date"
                                        value={dateDebut}
                                        onChange={e => this.handleDateChange('dateDebut', e.target.value)}
                                    />
                                </div>
                                <div className="pdc-form-group">
                                    <label>Date Fin</label>
                                    <input
                                        type="date"
                                        value={dateFin}
                                        onChange={e => this.handleDateChange('dateFin', e.target.value)}
                                    />
                                </div>
                            </>
                        )}
                        <button 
                            className="pdc-btn-search"
                            onClick={this.loadData}
                            disabled={loading}
                        >
                            <FontAwesomeIcon icon={loading ? faSpinner : faSearch} spin={loading} />
                            Rechercher
                        </button>
                        <button 
                            className="pdc-btn-export"
                            onClick={this.exportCSV}
                            disabled={loading}
                        >
                            <FontAwesomeIcon icon={faDownload} />
                            Export CSV
                        </button>
                        <button 
                            className={`pdc-btn-toggle ${showLoadIndicators ? 'active' : ''}`}
                            onClick={this.toggleLoadIndicators}
                            title={showLoadIndicators ? 'Masquer indicateurs de charge' : 'Afficher indicateurs de charge'}
                        >
                            <FontAwesomeIcon icon={showLoadIndicators ? faEye : faEyeSlash} />
                            {showLoadIndicators ? 'Masquer Charge' : 'Afficher Charge'}
                        </button>
                        {this.props.security?.user?.roles?.some(r => ['ROLE_PROCESS', 'ROLE_ADMIN'].includes(r.authority)) && (
                            <button
                                className="pdc-btn-toggle"
                                onClick={() => this.setState({ showRulesModal: true })}
                                title="Gérer les règles de capacité installée (intervalle, jour, shift, groupe)"
                            >
                                <FontAwesomeIcon icon={faCog} />
                                Règles Capacité
                            </button>
                        )}
                    </div>
                </div>

                {/* Error Message */}
                {error && (
                    <div className="pdc-error">
                        <p>{error}</p>
                    </div>
                )}

                {/* All History List - before export */}
                {this.renderAllHistoryList()}

                {/* Legend */}
                {this.renderLegend()}

                {/* Load Summary */}
                {this.renderLoadSummary()}

                {/* Status Grid */}
                {this.renderStatusGrid()}

                {/* Next Shift Preparation */}
                {this.renderNextShiftPreparation()}

                {/* Modal */}
                {this.renderModal()}

                {/* Charge Details Panel */}
                {this.renderChargeDetails()}

                {/* Non-imported Charge Modal */}
                {this.renderNonImportedChargeModal()}

                {/* Capacité Rules Modal */}
                {this.renderRulesModal()}
            </div>
        );
    }
}

PlanDeCharge.propTypes = {
    security: PropTypes.object.isRequired
};

const mapStateToProps = state => ({
    security: state.security
});

export default connect(mapStateToProps)(PlanDeCharge);

/**
 * PlanDeChargeView — presentational subcomponent for zone-load matrix.
 * Renders a simplified heatmap/table when embedded in the Workbench.
 * When accessed directly, use PlanDeCharge (default export) which preserves
 * full self-fetching behaviour.
 */
export class PlanDeChargeView extends Component {
    render() {
        const { data } = this.props;
        if (!data) {
            return <div className="pdc-empty">Aucune donnée de charge disponible.</div>;
        }

        // zoneLoad matrix shape: { machineTypes, rows, cells, thresholds, equilibre }
        const rows = data.rows || [];
        const cells = data.cells || [];
        const equilibre = data.equilibre || {};

        return (
            <div className="pdc-container">
                <div className="pdc-header">
                    <h1>Plan de Charge — Vue Matricielle</h1>
                </div>

                {equilibre.interZoneSpreadPct != null && (
                    <div className="pdc-info" style={{ marginBottom: 12, fontSize: '0.85rem' }}>
                        Spread inter-zone: <strong>{equilibre.interZoneSpreadPct.toFixed(1)}%</strong>
                        {' | '}
                        Status: <strong>{equilibre.interStatus}</strong>
                        {equilibre.hottestZone && (
                            <span> | Hottest: <strong>{equilibre.hottestZone}</strong></span>
                        )}
                        {equilibre.coolestZone && (
                            <span> | Coolest: <strong>{equilibre.coolestZone}</strong></span>
                        )}
                    </div>
                )}

                <div className="pdc-grid" style={{ overflowX: 'auto' }}>
                    <table className="pdc-table" style={{ minWidth: 600 }}>
                        <thead>
                            <tr>
                                <th>Zone</th>
                                <th>Catégorie</th>
                                <th>Machines actives</th>
                                <th>Minutes planifiées</th>
                                <th>Capacité</th>
                                <th>Charge %</th>
                            </tr>
                        </thead>
                        <tbody>
                            {rows.map(r => (
                                <tr key={r.zoneNom}>
                                    <td>{r.zoneNom}</td>
                                    <td>{r.zoneCategory}</td>
                                    <td>{r.activeMachines}</td>
                                    <td>{r.plannedMinutes?.toFixed(0) || 0}</td>
                                    <td>{r.capacityMinutes?.toFixed(0) || 0}</td>
                                    <td style={{
                                        color: r.loadPct > 100 ? '#dc3545' : r.loadPct > 90 ? '#ffc107' : '#198754',
                                        fontWeight: 600
                                    }}>
                                        {r.loadPct != null ? r.loadPct.toFixed(1) : 0}%
                                    </td>
                                </tr>
                            ))}
                            {rows.length === 0 && (
                                <tr><td colSpan="6" style={{ textAlign: 'center', color: '#888' }}>Aucune donnée</td></tr>
                            )}
                        </tbody>
                    </table>
                </div>

                {cells.length > 0 && (
                    <div className="pdc-grid" style={{ marginTop: 16, overflowX: 'auto' }}>
                        <table className="pdc-table" style={{ minWidth: 600 }}>
                            <thead>
                                <tr>
                                    <th>Zone</th>
                                    <th>Type Machine</th>
                                    <th>Actives</th>
                                    <th>Minutes</th>
                                    <th>Capacité</th>
                                    <th>Charge %</th>
                                </tr>
                            </thead>
                            <tbody>
                                {cells.map((c, i) => (
                                    <tr key={i}>
                                        <td>{c.zoneNom}</td>
                                        <td>{c.machineType}</td>
                                        <td>{c.activeMachines}</td>
                                        <td>{c.plannedMinutes?.toFixed(0) || 0}</td>
                                        <td>{c.capacityMinutes?.toFixed(0) || 0}</td>
                                        <td style={{
                                            color: c.loadPct > 100 ? '#dc3545' : c.loadPct > 90 ? '#ffc107' : '#198754',
                                            fontWeight: 600
                                        }}>
                                            {c.loadPct != null ? c.loadPct.toFixed(1) : 0}%
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        );
    }
}

PlanDeChargeView.propTypes = {
    data: PropTypes.object
};
