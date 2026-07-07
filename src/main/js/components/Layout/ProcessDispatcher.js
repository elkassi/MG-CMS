import axios from 'axios';
import moment from 'moment';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faPaperPlane, faSpinner, faExclamationTriangle,
    faInfoCircle, faLayerGroup, faGears, faRotate,
} from '@fortawesome/free-solid-svg-icons';
import DispatchEngineControl from './DispatchEngineControl';
import DispatchHeatmap from './DispatchHeatmap';
import DispatchEngineRunsPanel from './DispatchEngineRunsPanel';
import './styles/ProcessDispatcher.scss';

/**
 * DispatchingView — presentational component for the dispatching section.
 * Props: liveCharge, engineState, engineMode, engineDuration, engineBusy,
 *        engineIteration, engineSpread, engineStdDev, engineMedian,
 *        engineLastImprovement, engineRunId, chartData, matrix,
 *        loading, publishing, error, info, date, shift,
 *        canConfigureEngine, onRefresh, onPublish,
 *        onEngineStart, onEnginePause, onEngineResume, onEngineStop,
 *        onModeChange, onDurationChange
 */
export class DispatchingView extends Component {

    formatMinutes(minutes) {
        const value = Number(minutes || 0);
        const h = Math.floor(value / 60);
        const m = Math.round(value % 60);
        return h > 0 ? `${h}h ${String(m).padStart(2, '0')}min` : `${m}min`;
    }

    buildMatrixFromLiveCharge(lc, baseThresholds) {
        if (!lc || !lc.zones) return null;
        const typeSet = new Set();
        lc.zones.forEach(z => z.byMachineType.forEach(mt => typeSet.add(mt.machineType)));
        const machineTypes = Array.from(typeSet).sort((a, b) => a.localeCompare(b));

        const cells = [];
        const rows = [];
        const strictRows = [];
        let maxLoad = -Infinity;
        let minLoad = Infinity;
        let hottest = null;
        let coolest = null;

        lc.zones.forEach(zone => {
            const seqCount = (zone.lockedSequences?.length || 0) + (zone.pendingSequences?.length || 0);
            zone.byMachineType.forEach(mt => {
                cells.push({
                    zoneNom: zone.zoneNom,
                    zoneCategory: zone.category,
                    machineType: mt.machineType,
                    plannedMinutes: mt.totalRemainingMinutes,
                    baselineMinutes: mt.lockedRemainingMinutes,
                    pendingMinutes: mt.pendingRemainingMinutes,
                    capacityMinutes: mt.capacityMinutes,
                    loadPct: mt.loadPct,
                    activeMachines: mt.activeMachines,
                    sequencesCount: seqCount,
                    machinePresent: mt.activeMachines > 0 || mt.totalRemainingMinutes > 0,
                });
            });
            const totalActiveMachines = zone.byMachineType.reduce((a, mt) => a + mt.activeMachines, 0);
            const rowData = {
                zoneNom: zone.zoneNom,
                zoneCategory: zone.category,
                plannedMinutes: zone.totalRemainingMinutes,
                capacityMinutes: zone.totalCapacityMinutes,
                loadPct: zone.overallLoadPct,
                activeMachines: totalActiveMachines,
                sequencesCount: seqCount,
                intraZoneSpreadPct: 0,
            };
            rows.push(rowData);
            if (zone.category === 'STRICT' && totalActiveMachines > 0) {
                strictRows.push(rowData);
                if (rowData.loadPct > maxLoad) { maxLoad = rowData.loadPct; hottest = zone.zoneNom; }
                if (rowData.loadPct < minLoad) { minLoad = rowData.loadPct; coolest = zone.zoneNom; }
            }
        });

        const interSpread = strictRows.length >= 2 && maxLoad !== -Infinity
            ? Math.max(0, maxLoad - minLoad) : 0;
        const target = baseThresholds?.interZoneSpreadTargetPct ?? 15;
        const warning = baseThresholds?.interZoneSpreadWarningPct ?? 30;
        const interStatus = interSpread <= target ? 'GREEN'
            : interSpread <= warning ? 'AMBER' : 'RED';

        return {
            machineTypes,
            rows,
            cells,
            thresholds: baseThresholds,
            equilibre: {
                interZoneSpreadPct: interSpread,
                interStatus,
                worstIntraSpreadPct: 0,
                worstIntraZone: null,
                avgIntraSpreadPct: 0,
                intraStatus: 'GREEN',
                hottestZone: hottest,
                coolestZone: coolest,
            },
        };
    }

    buildActionItems(liveCharge, matrix) {
        if (!liveCharge || !liveCharge.zones) return [];
        const strictZones = liveCharge.zones
            .filter(z => z.category !== 'SHARED')
            .sort((a, b) => (b.overallLoadPct || 0) - (a.overallLoadPct || 0));
        const hottest = strictZones[0];
        const coolest = [...strictZones].sort((a, b) => (a.overallLoadPct || 0) - (b.overallLoadPct || 0))[0];
        const actions = [];

        if (hottest && hottest.overallLoadPct >= 90) {
            actions.push({
                tone: hottest.overallLoadPct >= 110 ? 'danger' : 'warn',
                title: 'Zone à délester',
                value: `${hottest.zoneNom} · ${Math.round(hottest.overallLoadPct)}%`,
                detail: `${this.formatMinutes(hottest.totalRemainingMinutes)} restants, ${hottest.pendingSequences?.length || 0} séquence(s) mobiles`,
            });
        }

        if (hottest && coolest && hottest.zoneNom !== coolest.zoneNom && (hottest.pendingSequences?.length || 0) > 0) {
            const seq = [...hottest.pendingSequences]
                .sort((a, b) => (b.totalRemainingMinutes || 0) - (a.totalRemainingMinutes || 0))[0];
            actions.push({
                tone: 'info',
                title: 'Mouvement à vérifier',
                value: `${seq?.sequence || '-'} vers ${coolest.zoneNom}`,
                detail: `${this.formatMinutes(seq?.totalRemainingMinutes)} à déplacer si matière et machines sont OK`,
            });
        }

        const materialBlocked = [];
        liveCharge.zones.forEach(z => {
            [...(z.pendingSequences || []), ...(z.lockedSequences || [])].forEach(seq => {
                if (seq.materialStatus && seq.materialStatus !== 'AVAILABLE_IN_ZONE') {
                    materialBlocked.push(seq);
                }
            });
        });
        if (materialBlocked.length > 0) {
            actions.push({
                tone: 'warn',
                title: 'Bloqué matière',
                value: `${materialBlocked.length} séquence(s)`,
                detail: `${materialBlocked.slice(0, 3).map(s => s.sequence).join(', ')}${materialBlocked.length > 3 ? '…' : ''}`,
            });
        }

        const spread = matrix?.equilibre?.interZoneSpreadPct;
        if (spread != null) {
            actions.push({
                tone: spread > 30 ? 'danger' : spread > 15 ? 'warn' : 'ok',
                title: 'Risque fin de shift',
                value: `${Math.round(spread)} pts d'écart`,
                detail: spread > 15 ? 'Rééquilibrage conseillé avant lancement série suivante' : 'Équilibre inter-zone acceptable',
            });
        }

        return actions;
    }

    renderActionPanel(liveCharge, matrix) {
        const actions = this.buildActionItems(liveCharge, matrix);
        if (actions.length === 0) return null;
        return (
            <div className="disp-action-panel">
                {actions.map((a, idx) => (
                    <div key={`${a.title}-${idx}`} className={`disp-action-card disp-action-${a.tone}`}>
                        <div className="disp-action-title">{a.title}</div>
                        <div className="disp-action-value">{a.value}</div>
                        <div className="disp-action-detail">{a.detail}</div>
                    </div>
                ))}
            </div>
        );
    }

    render() {
        const {
            liveCharge, engineState, engineMode, engineDuration, engineBusy,
            engineStarting, engineIteration, engineSpread, engineStdDev, engineMedian,
            engineLastImprovement, engineRunId, chartData, matrix,
            loading, publishing, error, info,
            canConfigureEngine, onRefresh, onPublish,
            onEngineStart, onEnginePause, onEngineResume, onEngineStop,
            onModeChange, onDurationChange,
        } = this.props;

        const thresholds = matrix?.thresholds;
        const hasData = liveCharge && liveCharge.zones && liveCharge.zones.length > 0;
        const heatmapMatrix = this.buildMatrixFromLiveCharge(liveCharge, thresholds) || matrix;

        return (
            <div className="disp-container">
                <div className="disp-header">
                    <h1>
                        <FontAwesomeIcon icon={faGears} style={{ marginRight: 10, color: '#EE3124' }} />
                        Dispatching de Séquence
                    </h1>
                    <div className="disp-subtitle">
                        Vue temps-réel — verrous physiques, charges par zone × type de machine, détail séries.
                    </div>
                </div>

                <div className="disp-controls" style={{ justifyContent: 'flex-end' }}>
                    <button
                        type="button"
                        className="disp-btn disp-btn-preview"
                        onClick={onRefresh}
                        disabled={loading}>
                        <FontAwesomeIcon icon={loading ? faSpinner : faRotate} spin={loading} />
                        Rafraîchir
                    </button>
                    {onPublish && (
                        <button
                            type="button"
                            className="disp-btn disp-btn-publish"
                            onClick={onPublish}
                            disabled={!hasData || loading || publishing || engineBusy}>
                            <FontAwesomeIcon icon={publishing ? faSpinner : faPaperPlane} spin={publishing} />
                            {publishing ? 'Publication…' : 'Publier'}
                        </button>
                    )}
                </div>

                {info && (
                    <div className="disp-alert disp-alert-info">
                        <FontAwesomeIcon icon={faInfoCircle} /> {info}
                    </div>
                )}
                {error && (
                    <div className="disp-alert disp-alert-error">
                        <FontAwesomeIcon icon={faExclamationTriangle} /> {String(error)}
                    </div>
                )}

                {!liveCharge && !loading && (
                    <div className="disp-empty">
                        <div className="disp-empty-icon"><FontAwesomeIcon icon={faLayerGroup} /></div>
                        <div className="disp-empty-text">
                            Aucune donnée à afficher.
                        </div>
                    </div>
                )}

                {/* Engine Control */}
                {liveCharge && (
                    <DispatchEngineControl
                        canConfigure={canConfigureEngine}
                        engineState={engineState}
                        engineMode={engineMode}
                        enginePhase={this.props.enginePhase}
                        engineDuration={engineDuration}
                        engineBusy={engineBusy}
                        engineStarting={engineStarting}
                        iteration={engineIteration}
                        currentSpread={engineSpread}
                        stdDev={engineStdDev}
                        median={engineMedian}
                        lastImprovement={engineLastImprovement}
                        onModeChange={onModeChange}
                        onDurationChange={onDurationChange}
                        onStart={onEngineStart}
                        onPause={onEnginePause}
                        onResume={onEngineResume}
                        onStop={onEngineStop}
                    />
                )}

                {liveCharge && this.renderActionPanel(liveCharge, heatmapMatrix)}

                {/* Heatmap */}
                {liveCharge && (
                    <DispatchHeatmap matrix={heatmapMatrix} loading={loading} liveCharge={liveCharge} />
                )}

                {/* Active-run suggestions + saved-runs history */}
                {liveCharge && (
                    <DispatchEngineRunsPanel
                        currentRunId={engineRunId}
                        engineState={engineState}
                        canPublish={canConfigureEngine}
                        liveCharge={liveCharge}
                    />
                )}


            </div>
        );
    }
}

/**
 * ProcessDispatcher — status-aware live charge view + engine control.
 * Thin wrapper around DispatchingView that self-fetches data.
 */
class ProcessDispatcher extends Component {

    constructor(props) {
        super(props);
        this.state = {
            date: moment().format('YYYY-MM-DD'),
            shift: 1,
            loading: false,
            publishing: false,
            liveCharge: null,
            matrix: null,
            error: null,
            info: null,

            // Engine state
            engineState: 'IDLE',
            engineMode: 'ALTERNATING',
            enginePhase: null,
            engineDuration: 300,
            engineBusy: false,
            engineIteration: 0,
            engineSpread: null,
            engineRawSpread: null,
            engineStdDev: null,
            engineMedian: null,
            engineInitialSpread: null,
            engineLastImprovement: null,
            engineRunId: null,
            chartData: [],
            engineStarting: false,
        };
        this.pollInterval = null;
        this.matrixPollInterval = null;
        this.liveChargePollInterval = null;
    }

    componentDidMount() {
        this.startPolling();
        this.loadLiveCharge();
    }

    componentWillUnmount() {
        this.stopPolling();
    }

    // ------------------------------------------------------------------ roles

    hasRole(role) {
        const user = this.props.security?.user || this.props.user;
        if (!user || !user.roles) return false;
        return user.roles.some(r => r.authority === role);
    }

    canConfigureEngine() {
        return this.hasRole('ROLE_PROCESS') || this.hasRole('ROLE_ADMIN');
    }

    // ------------------------------------------------------------------ polling

    startPolling() {
        if (this.pollInterval) clearInterval(this.pollInterval);
        const interval = ['WARMING', 'IMPROVING', 'PAUSED'].includes(this.state.engineState) ? 1000 : 3000;
        this.pollInterval = setInterval(() => this.pollEngineState(), interval);
    }

    stopPolling() {
        [this.pollInterval, this.matrixPollInterval, this.liveChargePollInterval]
            .forEach(h => h && clearInterval(h));
        this.pollInterval = null;
        this.matrixPollInterval = null;
        this.liveChargePollInterval = null;
    }

    pollEngineState() {
        axios.get('/api/dispatcher/engine/state')
            .then(res => {
                const d = res.data;
                const newState = d.state || 'IDLE';
                const wasRunning = ['WARMING', 'IMPROVING', 'PAUSED'].includes(this.state.engineState);
                const isRunning = ['WARMING', 'IMPROVING', 'PAUSED'].includes(newState);
                this.setState(prev => {
                    const newChart = [...prev.chartData];
                    if (d.iteration != null && d.iteration > 0 && d.currentSpread != null) {
                        if (newChart.length === 0 || newChart[newChart.length - 1].iteration !== d.iteration) {
                            newChart.push({
                                iteration: d.iteration,
                                spread: d.currentSpread,
                            });
                            if (newChart.length > 100) newChart.shift();
                        }
                    }
                    const becameRunning = ['WARMING', 'IMPROVING'].includes(newState);
                    return {
                        engineState: newState,
                        engineMode: d.mode || prev.engineMode,
                        enginePhase: d.phase || prev.enginePhase,
                        engineIteration: d.iteration != null ? d.iteration : prev.engineIteration,
                        engineSpread: d.rawSpread != null ? d.rawSpread : prev.engineSpread,
                        engineRawSpread: d.rawSpread != null ? d.rawSpread : prev.engineRawSpread,
                        engineStdDev: d.stdDev != null ? d.stdDev : prev.engineStdDev,
                        engineMedian: d.median != null ? d.median : prev.engineMedian,
                        engineInitialSpread: d.initialSpread != null ? d.initialSpread : prev.engineInitialSpread,
                        engineLastImprovement: d.lastImprovement != null ? d.lastImprovement : prev.engineLastImprovement,
                        engineRunId: d.runId || null,
                        chartData: newChart,
                        engineStarting: becameRunning ? false : prev.engineStarting,
                    };
                });
                if (isRunning && !wasRunning) {
                    this.startPolling();
                }
                if (isRunning || (wasRunning && !isRunning)) {
                    this.loadLiveCharge(true);
                }
            })
            .catch(() => {
                // 404 when feature disabled — silently ignore
            });
    }

    startMatrixPolling() {
        if (this.matrixPollInterval) clearInterval(this.matrixPollInterval);
        this.matrixPollInterval = setInterval(() => this.loadMatrix(), 5000);
    }

    startLiveChargePolling() {
        if (this.liveChargePollInterval) clearInterval(this.liveChargePollInterval);
        this.liveChargePollInterval = setInterval(() => this.loadLiveCharge(true), 10000);
    }

    // ------------------------------------------------------------------ data loading

    loadLiveCharge = (silent = false) => {
        if (!silent) {
            this.setState({ loading: true, error: null, info: 'Chargement en cours…' });
        }
        axios.get('/api/dispatcher/liveCharge')
            .then(res => {
                const data = res.data;
                this.setState({
                    liveCharge: data,
                    loading: false,
                    info: silent ? null : `${data.totals.totalSequences} séquence(s) chargée(s) — ${data.totals.lockedSequences} verrouillée(s), ${data.totals.pendingSequences} mobile(s).`,
                });
                if (!silent) {
                    this.startMatrixPolling();
                    this.startLiveChargePolling();
                    this.loadMatrix();
                }
            })
            .catch(err => {
                this.setState({ loading: false });
                this.handleError(err);
            });
    };

    engineStartActive = () => {
        this.setState({ engineBusy: true, engineStarting: true, error: null, chartData: [] });
        axios.post('/api/dispatcher/engine/startActive', {
            mode: this.state.engineMode,
            durationSec: null,
        })
        .then(() => this.setState({ engineBusy: false }))
        .catch(err => {
            this.setState({ engineBusy: false, engineStarting: false });
            this.handleError(err);
        });
    };

    loadMatrix = () => {
        axios.get('/api/zoneLoad/matrix', {
            params: { date: this.state.date, shift: this.state.shift },
        })
        .then(res => this.setState({ matrix: res.data }))
        .catch(() => { /* silently ignore 404 */ });
    };

    publish = () => {
        if (!window.confirm('Publier ce dispatch ? Les séquences proposées seront marquées PENDING chez chaque chef-de-zone.')) return;
        this.setState({ publishing: true, error: null });
        axios.post('/api/dispatcher/publish', null, {
            params: { date: this.state.date, shift: this.state.shift },
        })
        .then(() => {
            this.setState({ publishing: false, info: 'Dispatch publié — les chefs-de-zone vont voir les séquences PENDING.' });
            this.loadLiveCharge(true);
        })
        .catch(err => this.handleError(err, true));
    };

    handleError = (err, isPublish = false) => {
        if (err.response && err.response.status === 404) {
            this.setState({
                info: 'Dispatcher désactivé côté serveur.',
                [isPublish ? 'publishing' : 'loading']: false,
            });
        } else {
            this.setState({
                error: (err.response && err.response.data && (err.response.data.message || JSON.stringify(err.response.data))) || err.message,
                [isPublish ? 'publishing' : 'loading']: false,
            });
        }
    };

    // ------------------------------------------------------------------ engine actions

    enginePause = () => {
        this.setState({ engineBusy: true });
        axios.post('/api/dispatcher/engine/pause')
            .then(() => this.setState({ engineBusy: false }))
            .catch(() => this.setState({ engineBusy: false }));
    };

    engineResume = () => {
        this.setState({ engineBusy: true });
        axios.post('/api/dispatcher/engine/resume')
            .then(() => this.setState({ engineBusy: false }))
            .catch(() => this.setState({ engineBusy: false }));
    };

    engineStop = () => {
        this.setState({ engineBusy: true });
        axios.post('/api/dispatcher/engine/stop')
            .then(() => this.setState({ engineBusy: false }))
            .catch(() => this.setState({ engineBusy: false }));
    };

    render() {
        return (
            <DispatchingView
                liveCharge={this.state.liveCharge}
                engineState={this.state.engineState}
                engineMode={this.state.engineMode}
                enginePhase={this.state.enginePhase}
                engineDuration={this.state.engineDuration}
                engineBusy={this.state.engineBusy}
                engineStarting={this.state.engineStarting}
                engineIteration={this.state.engineIteration}
                engineSpread={this.state.engineSpread}
                engineStdDev={this.state.engineStdDev}
                engineMedian={this.state.engineMedian}
                engineLastImprovement={this.state.engineLastImprovement}
                engineRunId={this.state.engineRunId}
                chartData={this.state.chartData}
                matrix={this.state.matrix}
                loading={this.state.loading}
                publishing={this.state.publishing}
                error={this.state.error}
                info={this.state.info}
                canConfigureEngine={this.canConfigureEngine()}
                onRefresh={() => this.loadLiveCharge(false)}
                onPublish={this.publish}
                onEngineStart={this.engineStartActive}
                onEnginePause={this.enginePause}
                onEngineResume={this.engineResume}
                onEngineStop={this.engineStop}
                onModeChange={(m) => this.setState({ engineMode: m })}
                onDurationChange={(d) => this.setState({ engineDuration: d })}
            />
        );
    }
}

const mapStateToProps = state => ({
    security: state.security,
});

export default connect(mapStateToProps)(ProcessDispatcher);
