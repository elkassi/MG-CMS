import React, { Component } from 'react';
import axios from 'axios';
import moment from 'moment';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faLayerGroup, faRotate, faSpinner, faSitemap,
    faClock, faCheckCircle,
} from '@fortawesome/free-solid-svg-icons';
import '../../styles/TableFeed.scss';

/** Current plant shift, matching the backend ShiftClock boundaries. */
function computeCurrentShift() {
    const minutes = moment().hour() * 60 + moment().minute();
    if (minutes >= 1310) return 1;
    if (minutes < 350) return 1;
    if (minutes < 830) return 2;
    return 3;
}

class TableFeed extends Component {
    constructor(props) {
        super(props);
        this.state = {
            date: moment().format('YYYY-MM-DD'),
            shift: computeCurrentShift(),
            // Until the user explicitly picks a date/shift, let the backend
            // ShiftClock be the single source of truth (it rolls the date for
            // the late-night night shift, which the front end does not).
            userOverride: false,
            data: null,
            loading: false,
            error: null,
        };
        this.inFlight = false;
    }

    componentDidMount() {
        this.load();
        this.refreshInterval = setInterval(this.load, 45000);
    }

    componentWillUnmount() {
        if (this.refreshInterval) clearInterval(this.refreshInterval);
    }

    load = () => {
        if (this.inFlight) return;
        this.inFlight = true;
        this.setState({ loading: true, error: null });
        const params = this.state.userOverride
            ? { date: this.state.date, shift: this.state.shift }
            : {};
        axios.get('/api/dispatcher/tableFeed', { params })
            .then(res => this.setState({ data: res.data, loading: false }))
            .catch(err => this.setState({
                loading: false,
                error: err.response?.data?.message || err.message || 'Erreur de chargement',
            }))
            .finally(() => { this.inFlight = false; });
    };

    setDate = e => this.setState({ date: e.target.value, userOverride: true }, this.load);
    setShift = e => this.setState({ shift: Number(e.target.value), userOverride: true }, this.load);

    idleClass(minutes, idleNow) {
        if (idleNow || minutes <= 0) return 'tf-idle-now';
        if (minutes <= 20) return 'tf-idle-soon';
        return 'tf-idle-ok';
    }

    fmtIdle(minutes, idleNow) {
        if (idleNow) return 'Libre maintenant';
        return `${Number(minutes || 0).toFixed(0)} min`;
    }

    renderCandidate(cand, idx) {
        return (
            <div key={cand.serie + '-' + idx} className={`tf-cand ${idx === 0 ? 'tf-cand-top' : ''}`}>
                <div className="tf-cand-head">
                    <span className="tf-cand-rank">#{idx + 1}</span>
                    <strong className="tf-cand-serie">{cand.serie}</strong>
                    <span className="tf-cand-seq">{cand.sequence}</span>
                    {cand.refTissus && <span className="tf-cand-ref">{cand.refTissus}</span>}
                    <span className="tf-status tf-status-ready">
                        <FontAwesomeIcon icon={faCheckCircle} />
                        Waiting / Waiting
                    </span>
                    {cand.sequenceStatus && <span className="tf-status tf-status-wait">{cand.sequenceStatus}</span>}
                    <span className="tf-cand-score">{Number(cand.score || 0).toFixed(0)} pts</span>
                </div>
                <div className="tf-cand-reasons">
                    {(cand.reasons || []).map((r, i) => (
                        <span key={i} className="tf-reason">{r}</span>
                    ))}
                </div>
            </div>
        );
    }

    renderTable(table) {
        const idleCls = this.idleClass(table.timeToIdleMinutes, table.idleNow);
        return (
            <div key={table.tableNom} className="tf-table">
                <div className="tf-table-head">
                    <div className="tf-table-id">
                        <FontAwesomeIcon icon={faLayerGroup} />
                        <strong>{table.tableNom}</strong>
                        {table.machineType && <span className="tf-table-type">{table.machineType}</span>}
                        {table.mountedRefTissu && (
                            <span className="tf-table-mounted" title="Réf. tissu montée">
                                {table.mountedRefTissu}
                            </span>
                        )}
                    </div>
                    <div className={`tf-idle ${idleCls}`}>
                        <FontAwesomeIcon icon={faClock} />
                        <span>Libre dans {this.fmtIdle(table.timeToIdleMinutes, table.idleNow)}</span>
                    </div>
                </div>
                <div className="tf-cand-list">
                    {(table.candidates || []).length > 0
                        ? table.candidates.map((c, i) => this.renderCandidate(c, i))
                        : <div className="tf-cand-empty">Aucune série Waiting/Waiting RELEASED ou STARTED pour cette table.</div>}
                </div>
            </div>
        );
    }

    renderZone(zone) {
        return (
            <div key={zone.zoneNom} className="tf-zone">
                <div className="tf-zone-head">
                    <FontAwesomeIcon icon={faSitemap} />
                    <strong>{zone.zoneNom}</strong>
                    {zone.category && <span className="tf-zone-cat">{zone.category}</span>}
                    <span className="tf-zone-count">{(zone.tables || []).length} table(s)</span>
                </div>
                <div className="tf-table-list">
                    {(zone.tables || []).map(t => this.renderTable(t))}
                </div>
            </div>
        );
    }

    render() {
        const { data, loading, error, date, shift } = this.state;
        const zones = data?.zones || [];
        return (
            <div className="tf-container">
                <div className="tf-header">
                    <h2 className="tf-title">
                        <FontAwesomeIcon icon={faLayerGroup} style={{ marginRight: 8 }} />
                        Top 3 séries par machine
                    </h2>
                    <div className="tf-controls">
                        <div className="tf-control">
                            <label>Date</label>
                            <input type="date" value={date} onChange={this.setDate} className="tf-input" />
                        </div>
                        <div className="tf-control">
                            <label>Shift</label>
                            <select value={shift} onChange={this.setShift} className="tf-select">
                                <option value={1}>1 (Nuit)</option>
                                <option value={2}>2 (Matin)</option>
                                <option value={3}>3 (Après-midi)</option>
                            </select>
                        </div>
                        <button className="tf-refresh" onClick={this.load} disabled={loading} title="Rafraîchir">
                            <FontAwesomeIcon icon={loading ? faSpinner : faRotate} spin={loading} />
                        </button>
                    </div>
                </div>

                {data?.asOf && (
                    <div className="tf-generated">
                        <FontAwesomeIcon icon={faClock} style={{ marginRight: 6 }} />
                        Mis à jour {moment(data.asOf).format('DD/MM/YYYY HH:mm:ss')} · actualisation auto 45s
                    </div>
                )}

                {error && <div className="tf-error">{error}</div>}

                <div className="tf-zone-list">
                    {zones.length > 0
                        ? zones.map(z => this.renderZone(z))
                        : !loading && <div className="tf-empty">Aucune table active pour ce créneau.</div>}
                </div>
            </div>
        );
    }
}

export default TableFeed;
