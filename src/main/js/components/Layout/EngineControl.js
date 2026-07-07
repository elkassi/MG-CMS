import axios from 'axios';
import moment from 'moment';
import React, { Component } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faPlay, faStop, faRotate, faSpinner, faGears, faToggleOn, faToggleOff,
    faInfoCircle, faExclamationTriangle, faCheckCircle, faCirclePlay,
} from '@fortawesome/free-solid-svg-icons';
import './styles/EngineControl.scss';

/**
 * EngineControl — runtime control panel for the Phase 7 ordonnancement
 * engine.
 *
 * GET  /api/engine/status
 * POST /api/engine/start
 * POST /api/engine/stop
 * POST /api/engine/runOnce?date&shift
 *
 * The auto-tick scheduler is gated by three flags. Only autoTick.enabled
 * is mutable from the UI; the other two (dispatcher.enabled, zoneAware)
 * require an application.properties change + restart and are shown here
 * as read-only state so ops can see why Start may refuse with 409.
 */
export default class EngineControl extends Component {

    constructor(props) {
        super(props);
        this.state = {
            status: null,
            loading: false,
            mutating: false,
            running: false,
            error: null,
            info: null,
            success: null,
            runDate: moment().format('YYYY-MM-DD'),
            runShift: 1,
        };
    }

    componentDidMount() {
        this.refreshStatus();
    }

    refreshStatus = () => {
        this.setState({ loading: true, error: null });
        axios.get('/api/engine/status')
            .then(res => this.setState({ status: res.data, loading: false }))
            .catch(err => this.setState({
                error: this.formatError(err) || 'Lecture du statut impossible.',
                loading: false,
            }));
    };

    formatError(err) {
        if (!err.response) return err.message;
        const data = err.response.data;
        if (data && data.message) return data.message;
        if (typeof data === 'string') return data;
        return JSON.stringify(data);
    }

    start = () => {
        this.setState({ mutating: true, error: null, success: null, info: null });
        axios.post('/api/engine/start')
            .then(res => this.setState({
                success: res.data.message || 'Auto-tick démarré.',
                mutating: false,
            }, this.refreshStatus))
            .catch(err => this.setState({
                error: this.formatError(err),
                mutating: false,
            }));
    };

    stop = () => {
        this.setState({ mutating: true, error: null, success: null, info: null });
        axios.post('/api/engine/stop')
            .then(res => this.setState({
                success: res.data.message || 'Auto-tick arrêté.',
                mutating: false,
            }, this.refreshStatus))
            .catch(err => this.setState({
                error: this.formatError(err),
                mutating: false,
            }));
    };

    runOnce = () => {
        const { runDate, runShift } = this.state;
        this.setState({ running: true, error: null, success: null, info: null });
        axios.post('/api/engine/runOnce', null, {
            params: { date: runDate, shift: runShift },
        })
            .then(res => this.setState({
                success: res.data.message || `Run-Once terminé pour ${runDate} shift ${runShift}.`,
                running: false,
            }))
            .catch(err => this.setState({
                error: this.formatError(err),
                running: false,
            }));
    };

    flagPill(active) {
        return (
            <span className={`eng-pill ${active ? 'eng-pill-on' : 'eng-pill-off'}`}>
                <FontAwesomeIcon icon={active ? faToggleOn : faToggleOff} />
                {active ? 'ACTIF' : 'INACTIF'}
            </span>
        );
    }

    renderStatusCard() {
        const { status, loading } = this.state;
        if (loading && !status) {
            return <div className="eng-card"><div style={{ padding: 20, textAlign: 'center', color: '#888' }}>
                <FontAwesomeIcon icon={faSpinner} spin /> Chargement…
            </div></div>;
        }
        if (!status) return null;
        return (
            <div className="eng-card">
                <h5>
                    <FontAwesomeIcon icon={faGears} style={{ color: '#EE3124' }} />
                    État courant
                </h5>
                <div className="eng-flag-row">
                    <div>
                        <div className="eng-flag-name">mgcms.dispatcher.enabled</div>
                        <div className="eng-flag-desc">Master switch — Phase 4 endpoints (preview / publish)</div>
                    </div>
                    {this.flagPill(status.dispatcherEnabled)}
                </div>
                <div className="eng-flag-row">
                    <div>
                        <div className="eng-flag-name">mgcms.engine.zone-aware</div>
                        <div className="eng-flag-desc">Phase 7 — engine respecte dispatched_zone + pinnedByChef</div>
                    </div>
                    {this.flagPill(status.zoneAware)}
                </div>
                <div className="eng-flag-row">
                    <div>
                        <div className="eng-flag-name">mgcms.engine.auto-tick.enabled</div>
                        <div className="eng-flag-desc">
                            Cron auto-dispatch (mutable via Start/Stop)
                            <br />
                            <span className="eng-cron">cron: {status.autoTickCron}</span>
                        </div>
                    </div>
                    {this.flagPill(status.autoTickEnabled)}
                </div>
            </div>
        );
    }

    renderControlsCard() {
        const { status, mutating } = this.state;
        const canStart = status && status.dispatcherEnabled && status.zoneAware && !status.autoTickEnabled;
        const canStop = status && status.autoTickEnabled;
        return (
            <div className="eng-card">
                <h5>
                    <FontAwesomeIcon icon={faCirclePlay} style={{ color: '#EE3124' }} />
                    Contrôle de l'auto-tick
                </h5>
                <p style={{ fontSize: '0.85rem', color: '#666', marginBottom: 12 }}>
                    Démarre / arrête le scheduler qui re-dispatche automatiquement chaque shift selon le cron ci-dessus.
                    Le réglage se perd au redémarrage du serveur.
                </p>
                <div className="eng-actions">
                    <button
                        className="eng-btn eng-btn-start"
                        onClick={this.start}
                        disabled={mutating || !canStart}
                        title={!status?.dispatcherEnabled || !status?.zoneAware
                            ? 'Activer mgcms.dispatcher.enabled et mgcms.engine.zone-aware d\'abord'
                            : (canStart ? 'Démarrer' : 'Déjà démarré')}>
                        <FontAwesomeIcon icon={mutating ? faSpinner : faPlay} spin={mutating} />
                        Démarrer
                    </button>
                    <button
                        className="eng-btn eng-btn-stop"
                        onClick={this.stop}
                        disabled={mutating || !canStop}
                        title={canStop ? 'Arrêter' : 'Déjà arrêté'}>
                        <FontAwesomeIcon icon={mutating ? faSpinner : faStop} spin={mutating} />
                        Arrêter
                    </button>
                    <button
                        className="eng-btn"
                        style={{ background: '#6c757d', color: '#fff' }}
                        onClick={this.refreshStatus}
                        disabled={mutating}>
                        <FontAwesomeIcon icon={faRotate} /> Rafraîchir
                    </button>
                </div>
            </div>
        );
    }

    renderRunOnceCard() {
        const { runDate, runShift, running, status } = this.state;
        const blocked = !status || !status.dispatcherEnabled || !status.zoneAware;
        return (
            <div className="eng-card" style={{ gridColumn: '1 / -1' }}>
                <h5>
                    <FontAwesomeIcon icon={faRotate} style={{ color: '#EE3124' }} />
                    Run-Once (forcer un cycle)
                </h5>
                <p style={{ fontSize: '0.85rem', color: '#666', marginBottom: 12 }}>
                    Déclenche une seule itération de dispatch + version-bump pour la (date, shift) choisie. Utile après une nouvelle
                    confirmation chef-de-zone : pas besoin d'attendre le prochain cron.
                </p>
                <div className="eng-runonce-form">
                    <div className="eng-form-group">
                        <label>Date</label>
                        <input
                            type="date"
                            value={runDate}
                            onChange={e => this.setState({ runDate: e.target.value })}
                            disabled={running} />
                    </div>
                    <div className="eng-form-group">
                        <label>Shift</label>
                        <select
                            value={runShift}
                            onChange={e => this.setState({ runShift: parseInt(e.target.value, 10) })}
                            disabled={running}>
                            <option value={1}>Shift 1</option>
                            <option value={2}>Shift 2</option>
                            <option value={3}>Shift 3</option>
                        </select>
                    </div>
                    <button
                        className="eng-btn eng-btn-runonce"
                        onClick={this.runOnce}
                        disabled={running || blocked}
                        title={blocked ? 'Activez dispatcher + zone-aware d\'abord' : 'Lancer maintenant'}>
                        <FontAwesomeIcon icon={running ? faSpinner : faRotate} spin={running} />
                        {running ? 'Exécution…' : 'Lancer maintenant'}
                    </button>
                </div>
            </div>
        );
    }

    render() {
        const { error, info, success } = this.state;
        return (
            <div className="eng-container">
                <div className="eng-header">
                    <h1>
                        <FontAwesomeIcon icon={faGears} style={{ marginRight: 10, color: '#EE3124' }} />
                        Contrôle Moteur
                    </h1>
                    <div className="eng-subtitle">
                        Phase 7 — démarrer / arrêter l'auto-dispatch et forcer des cycles ponctuels.
                    </div>
                </div>

                {info && (
                    <div className="eng-banner eng-banner-info">
                        <FontAwesomeIcon icon={faInfoCircle} /> {info}
                    </div>
                )}
                {success && (
                    <div className="eng-banner eng-banner-success">
                        <FontAwesomeIcon icon={faCheckCircle} /> {success}
                    </div>
                )}
                {error && (
                    <div className="eng-banner eng-banner-error">
                        <FontAwesomeIcon icon={faExclamationTriangle} /> {String(error)}
                    </div>
                )}

                <div className="eng-grid">
                    {this.renderStatusCard()}
                    {this.renderControlsCard()}
                    {this.renderRunOnceCard()}
                </div>
            </div>
        );
    }
}
