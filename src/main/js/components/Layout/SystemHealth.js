import axios from 'axios';
import React, { Component } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSync, faDownload, faMicrochip, faMemory, faHardDrive, faNetworkWired, faDatabase } from '@fortawesome/free-solid-svg-icons';
import '../../styles/SystemHealth.scss';

/** ROLE_ADMIN diagnostics: DB health, expensive queries (+ login), host CPU/RAM/disk/network. */
export default class SystemHealth extends Component {
    state = { data: null, loading: false, error: null, downloading: false };

    componentDidMount() {
        this.load();
        this.timer = setInterval(this.load, 30000); // light auto-refresh
    }
    componentWillUnmount() { clearInterval(this.timer); }

    load = () => {
        this.setState({ loading: true, error: null });
        axios.get('/api/admin/systemHealth/status')
            .then(r => this.setState({ data: r.data, loading: false }))
            .catch(err => this.setState({ loading: false, error: err.response?.data?.message || 'Échec du chargement de la santé système.' }));
    };

    downloadReport = () => {
        this.setState({ downloading: true });
        axios.get('/api/admin/systemHealth/report?minutes=60', { responseType: 'blob' })
            .then(r => {
                const cd = r.headers['content-disposition'] || '';
                const name = (cd.match(/filename="?([^"]+)"?/) || [])[1] || 'mgcms-health-report.md';
                const url = window.URL.createObjectURL(new Blob([r.data]));
                const a = document.createElement('a');
                a.href = url; a.download = name; a.click();
                window.URL.revokeObjectURL(url);
                this.setState({ downloading: false });
            })
            .catch(() => this.setState({ downloading: false, error: 'Échec du téléchargement du rapport.' }));
    };

    bar = (pct) => {
        const p = Math.min(100, Math.max(0, pct || 0));
        const cls = p >= 90 ? 'sh-bar-crit' : p >= 75 ? 'sh-bar-warn' : 'sh-bar-ok';
        return <div className="sh-bar"><div className={`sh-bar-fill ${cls}`} style={{ width: p + '%' }} /><span>{p}%</span></div>;
    };

    render() {
        const { data, loading, error, downloading } = this.state;
        const s = data?.server || {};
        return (
            <div className="sh-page">
                <div className="sh-head">
                    <h2>Santé Système</h2>
                    <div className="sh-actions">
                        <span className="sh-ts">{data ? data.timestamp : ''}</span>
                        <button onClick={this.load} disabled={loading}>
                            <FontAwesomeIcon icon={faSync} spin={loading} /> Rafraîchir
                        </button>
                        <button className="sh-btn-primary" onClick={this.downloadReport} disabled={downloading}>
                            <FontAwesomeIcon icon={faDownload} /> Rapport 1h ({data ? data.historyPoints : 0} pts)
                        </button>
                    </div>
                </div>
                {error && <div className="sh-alert">{error}</div>}
                {!data && loading && <p>Chargement…</p>}

                {data && (
                    <>
                        {/* ---- Server host ---- */}
                        <div className="sh-grid">
                            <div className="sh-card">
                                <h3><FontAwesomeIcon icon={faMicrochip} /> CPU</h3>
                                {s.cpu?.error ? <span className="sh-err">{s.cpu.error}</span> : <>
                                    {this.bar(s.cpu?.usedPct)}
                                    <div className="sh-sub">{s.cpu?.name}</div>
                                    <div className="sh-sub">{s.cpu?.physicalCores} cœurs / {s.cpu?.logicalCores} threads</div>
                                </>}
                            </div>
                            <div className="sh-card">
                                <h3><FontAwesomeIcon icon={faMemory} /> Mémoire</h3>
                                {s.memory?.error ? <span className="sh-err">{s.memory.error}</span> : <>
                                    {this.bar(s.memory?.usedPct)}
                                    <div className="sh-sub">{s.memory?.usedMb} / {s.memory?.totalMb} Mo</div>
                                    <div className="sh-sub">JVM heap : {s.jvm?.heapUsedMb} / {s.jvm?.heapMaxMb} Mo</div>
                                </>}
                            </div>
                            <div className="sh-card">
                                <h3><FontAwesomeIcon icon={faNetworkWired} /> Réseau</h3>
                                <div className="sh-sub">↓ {s.network?.rxKbps} Kbps</div>
                                <div className="sh-sub">↑ {s.network?.txKbps} Kbps</div>
                            </div>
                            <div className="sh-card">
                                <h3><FontAwesomeIcon icon={faHardDrive} /> Disques</h3>
                                {Array.isArray(s.disks) ? s.disks.map((d, i) => (
                                    <div key={i} className="sh-disk">
                                        <div className="sh-disk-label">{d.mount} — {d.freeGb} Go libres / {d.totalGb} Go</div>
                                        {this.bar(d.usedPct)}
                                    </div>
                                )) : <span className="sh-err">{s.disks?.error}</span>}
                            </div>
                        </div>

                        {/* ---- Top processes ---- */}
                        {Array.isArray(s.topProcesses) && (
                            <div className="sh-card sh-wide">
                                <h3>Processus (mémoire)</h3>
                                <table className="sh-table"><thead><tr><th>PID</th><th>Nom</th><th>Mémoire (Mo)</th></tr></thead>
                                    <tbody>{s.topProcesses.map((p, i) => (
                                        <tr key={i} className={p.isMgcms ? 'sh-self' : ''}>
                                            <td>{p.pid}</td><td>{p.name}{p.isMgcms ? ' (MG-CMS)' : ''}</td><td>{p.memMb}</td>
                                        </tr>))}</tbody>
                                </table>
                            </div>
                        )}

                        {/* ---- Databases ---- */}
                        <div className="sh-grid">
                            {(data.databases || []).map((d, i) => (
                                <div key={i} className={`sh-card sh-db ${d.online ? 'sh-up' : 'sh-down'}`}>
                                    <h3><FontAwesomeIcon icon={faDatabase} /> {d.name}</h3>
                                    {d.online ? <>
                                        <div className="sh-sub">{d.sizeMb} Mo</div>
                                        <table className="sh-table sh-mini"><tbody>
                                            {(d.biggestTables || []).map((t, j) => (
                                                <tr key={j}><td>{t.tbl}</td><td>{t.mb} Mo</td></tr>))}
                                        </tbody></table>
                                    </> : <span className="sh-err">HORS LIGNE — {d.error}</span>}
                                </div>
                            ))}
                        </div>

                        {/* ---- Active queries (with login) ---- */}
                        <div className="sh-card sh-wide">
                            <h3>Requêtes en cours (login)</h3>
                            <table className="sh-table"><thead><tr>
                                <th>Session</th><th>Login</th><th>Hôte</th><th>DB</th><th>CPU ms</th><th>Durée ms</th><th>Attente</th><th>Requête</th>
                            </tr></thead><tbody>
                                {(data.activeQueries || []).map((q, i) => (
                                    <tr key={i}>
                                        <td>{q.sessionId}</td><td><b>{q.login}</b></td><td>{q.host}</td><td>{q.db}</td>
                                        <td>{q.cpuMs}</td><td>{q.elapsedMs}</td><td>{q.waitType}</td>
                                        <td className="sh-sql">{q.queryText}</td>
                                    </tr>))}
                                {(!data.activeQueries || data.activeQueries.length === 0) && <tr><td colSpan="8">Aucune requête active.</td></tr>}
                            </tbody></table>
                        </div>

                        {/* ---- Expensive queries (historical) ---- */}
                        <div className="sh-card sh-wide">
                            <h3>Requêtes coûteuses (depuis le dernier redémarrage SQL)</h3>
                            <table className="sh-table"><thead><tr>
                                <th>DB</th><th>Exéc.</th><th>CPU moy. ms</th><th>Lectures moy.</th><th>Durée moy. ms</th><th>Requête</th>
                            </tr></thead><tbody>
                                {(data.expensiveQueries || []).map((q, i) => (
                                    <tr key={i}>
                                        <td>{q.db}</td><td>{q.execCount}</td><td>{q.avgCpuMs}</td>
                                        <td>{q.avgReads}</td><td>{q.avgElapsedMs}</td>
                                        <td className="sh-sql">{q.queryText}</td>
                                    </tr>))}
                            </tbody></table>
                        </div>
                    </>
                )}
            </div>
        );
    }
}
