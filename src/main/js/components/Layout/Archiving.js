import axios from 'axios';
import React, { Component } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSync, faBoxArchive, faEye, faTriangleExclamation } from '@fortawesome/free-solid-svg-icons';
import '../../styles/Archiving.scss';

/** ROLE_ADMIN: move aged rows from hot tables into same-DB <table>_archive copies. */
export default class Archiving extends Component {
    state = { inv: null, loading: false, error: null, rows: {}, busy: {} };

    componentDidMount() {
        const d = new Date();
        d.setMonth(d.getMonth() - 13);
        this.defaultDate = d.toISOString().slice(0, 10); // ~13 months back
        this.load();
    }

    load = () => {
        this.setState({ loading: true, error: null });
        axios.get('/api/admin/archiving/inventory')
            .then(r => this.setState({ inv: r.data, loading: false }))
            .catch(err => this.setState({ loading: false, error: err.response?.data?.message || 'Échec du chargement de l\'inventaire.' }));
    };

    row = (t) => this.state.rows[t] || { beforeDate: this.defaultDate, preview: null, result: null, error: null };
    setRow = (t, patch) => this.setState(s => ({ rows: { ...s.rows, [t]: { ...this.row(t), ...patch } } }));
    setBusy = (t, v) => this.setState(s => ({ busy: { ...s.busy, [t]: v } }));

    preview = (t) => {
        const { beforeDate } = this.row(t);
        this.setBusy(t, true); this.setRow(t, { error: null, result: null });
        axios.post('/api/admin/archiving/preview', null, { params: { table: t, beforeDate } })
            .then(r => { this.setRow(t, { preview: r.data.rowsToArchive }); this.setBusy(t, false); })
            .catch(err => { this.setRow(t, { error: err.response?.data?.message || 'Échec de l\'aperçu.' }); this.setBusy(t, false); });
    };

    run = (t) => {
        const { beforeDate, preview } = this.row(t);
        if (!window.confirm(`Archiver ${preview != null ? preview : '?'} lignes de ${t} antérieures à ${beforeDate} vers ${t}_archive, puis les supprimer de ${t} ?`)) return;
        this.setBusy(t, true); this.setRow(t, { error: null });
        axios.post('/api/admin/archiving/run', null, { params: { table: t, beforeDate } })
            .then(r => { this.setRow(t, { result: r.data.archivedRows, preview: null }); this.setBusy(t, false); this.load(); })
            .catch(err => { this.setRow(t, { error: err.response?.data?.message || 'Échec de l\'archivage.' }); this.setBusy(t, false); });
    };

    render() {
        const { inv, loading, error } = this.state;
        return (
            <div className="arc-page">
                <div className="arc-head">
                    <h2><FontAwesomeIcon icon={faBoxArchive} /> Archivage</h2>
                    <button onClick={this.load} disabled={loading}><FontAwesomeIcon icon={faSync} spin={loading} /> Rafraîchir</button>
                </div>
                <p className="arc-note"><FontAwesomeIcon icon={faTriangleExclamation} /> Ces 9 tables sont des journaux indépendants (aucune autre table n'en dépend) — l'archivage ne casse aucune relation. Le groupe cycle-de-vie <code>CuttingRequest*</code> n'est volontairement pas listé (phase 2, cascade par séquence COMPLETED).</p>
                {error && <div className="arc-alert">{error}</div>}

                {inv && (
                    <table className="arc-table">
                        <thead><tr>
                            <th>Table</th><th>Colonne date</th><th>Lignes</th><th>Taille</th><th>Période</th>
                            <th>Antérieur à</th><th>Aperçu</th><th>Action</th>
                        </tr></thead>
                        <tbody>
                            {(inv.candidates || []).map(c => {
                                const r = this.row(c.table); const busy = this.state.busy[c.table];
                                return (
                                    <tr key={c.table}>
                                        <td><b>{c.table}</b><div className="arc-sub">→ {c.archiveTable}{c.archiveExists ? ' (existe)' : ''}</div></td>
                                        <td>{c.dateColumn}</td>
                                        <td>{c.rows != null ? c.rows.toLocaleString() : '—'}</td>
                                        <td>{c.sizeMb != null ? c.sizeMb + ' Mo' : '—'}</td>
                                        <td className="arc-sub">{(c.minDate || '').slice(0, 10)} → {(c.maxDate || '').slice(0, 10)}</td>
                                        <td><input type="date" value={r.beforeDate} onChange={e => this.setRow(c.table, { beforeDate: e.target.value, preview: null })} /></td>
                                        <td>
                                            <button className="arc-ghost" disabled={busy} onClick={() => this.preview(c.table)}><FontAwesomeIcon icon={faEye} /></button>
                                            {r.preview != null && <span className="arc-count">{r.preview.toLocaleString()} lignes</span>}
                                            {r.result != null && <span className="arc-ok">{r.result.toLocaleString()} archivées ✓</span>}
                                            {r.error && <span className="arc-err">{r.error}</span>}
                                        </td>
                                        <td><button className="arc-danger" disabled={busy} onClick={() => this.run(c.table)}>
                                            <FontAwesomeIcon icon={faBoxArchive} /> Archiver</button></td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                )}

                {inv && inv.alreadyArchived && inv.alreadyArchived.length > 0 && (
                    <div className="arc-cold">
                        <h3>Tables déjà froides (copies / archives existantes)</h3>
                        <p className="arc-sub">Ce sont déjà des archives (suffixes <code>2024 / old / _Archive</code>). À exporter puis supprimer hors-ligne plutôt que ré-archiver.</p>
                        <table className="arc-table">
                            <thead><tr><th>Table</th><th>Lignes</th><th>Taille</th></tr></thead>
                            <tbody>{inv.alreadyArchived.map(c => (
                                <tr key={c.table}><td>{c.table}</td><td>{c.rows != null ? c.rows.toLocaleString() : '—'}</td><td>{c.sizeMb != null ? c.sizeMb + ' Mo' : '—'}</td></tr>
                            ))}</tbody>
                        </table>
                    </div>
                )}
            </div>
        );
    }
}
