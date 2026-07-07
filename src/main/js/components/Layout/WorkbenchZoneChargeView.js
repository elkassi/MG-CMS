import React, { Component } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faScaleBalanced, faSitemap } from '@fortawesome/free-solid-svg-icons';

/**
 * Live per-zone, per-machine-type charge balance for the workbench. Reads
 * {@code data.liveCharge} (already in the /api/workbench/data payload) and shows,
 * for each zone, the overall load and a row per machine type (Lectra / Lectra
 * IP6 …) with capacity, remaining minutes (locked vs pending) and load%.
 */
class WorkbenchZoneChargeView extends Component {
    fmtMin(value) {
        if (value == null || isNaN(value)) return '—';
        if (value < 60) return `${Number(value).toFixed(0)} min`;
        const h = Math.floor(value / 60);
        const m = Math.round(value - h * 60);
        return m > 0 ? `${h} h ${m}` : `${h} h`;
    }

    loadClass(pct) {
        const v = Number(pct || 0);
        if (v >= 100) return 'wb-zc-load-over';
        if (v >= 85) return 'wb-zc-load-high';
        return 'wb-zc-load-ok';
    }

    filteredZones() {
        const zones = this.props.data?.liveCharge?.zones || [];
        const zoneFilter = this.props.zoneFilter || 'All';
        const list = zoneFilter === 'All' ? zones : zones.filter(z => z.zoneNom === zoneFilter);
        // STRICT zones first, then by name — same order as the rest of the app.
        return [...list].sort((a, b) => {
            const as = a.category === 'SHARED', bs = b.category === 'SHARED';
            if (as !== bs) return as ? 1 : -1;
            return String(a.zoneNom).localeCompare(String(b.zoneNom));
        });
    }

    render() {
        const zones = this.filteredZones();
        if (zones.length === 0) {
            return <div className="wb-zc-empty">Charge par zone indisponible pour ce créneau.</div>;
        }
        return (
            <div className="wb-zc-view">
                <div className="wb-zc-head">
                    <FontAwesomeIcon icon={faScaleBalanced} />
                    <span>Charge par zone &amp; type de machine</span>
                    <small>Minutes restantes (verrouillées + en attente) vs capacité du créneau</small>
                </div>
                {zones.map(z => {
                    const types = z.byMachineType || [];
                    return (
                        <div key={z.zoneNom} className="wb-zc-zone">
                            <div className="wb-zc-zone-head">
                                <div className="wb-zc-zone-id">
                                    <FontAwesomeIcon icon={faSitemap} />
                                    <strong>{z.zoneNom}</strong>
                                    {z.category && <span className="wb-zc-cat">{z.category}</span>}
                                </div>
                                <div className={`wb-zc-overall ${this.loadClass(z.overallLoadPct)}`}>
                                    {Number(z.overallLoadPct || 0).toFixed(0)}%
                                </div>
                            </div>
                            <table className="wb-zc-table">
                                <thead>
                                    <tr>
                                        <th>Type machine</th>
                                        <th>Machines</th>
                                        <th>Charge</th>
                                        <th>Capacité</th>
                                        <th>Verrou.</th>
                                        <th>Attente</th>
                                        <th>Charge %</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {types.length === 0 && (
                                        <tr><td colSpan={7} className="wb-zc-na">Aucune machine active</td></tr>
                                    )}
                                    {types.map(t => (
                                        <tr key={t.machineType}>
                                            <td className="wb-zc-type">{t.machineType}</td>
                                            <td>{t.activeMachines}</td>
                                            <td>{this.fmtMin(t.totalRemainingMinutes)}</td>
                                            <td>{this.fmtMin(t.capacityMinutes)}</td>
                                            <td>{this.fmtMin(t.lockedRemainingMinutes)}</td>
                                            <td>{this.fmtMin(t.pendingRemainingMinutes)}</td>
                                            <td>
                                                <div className="wb-zc-bar">
                                                    <span
                                                        className={this.loadClass(t.loadPct)}
                                                        style={{ width: `${Math.min(100, Number(t.loadPct || 0))}%` }}
                                                    />
                                                    <em>{Number(t.loadPct || 0).toFixed(0)}%</em>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    );
                })}
            </div>
        );
    }
}

export default WorkbenchZoneChargeView;
