import React, { Component } from 'react';
import axios from 'axios';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSave, faPlay, faArrowLeft, faPlus, faTrash, faSync } from '@fortawesome/free-solid-svg-icons';
import Select from 'react-select';
import Switch from 'react-switch';

export default class CtcToleranceRuleForm extends Component {
    constructor(props) {
        super(props);
        this.state = {
            rules: [],
            loading: true,
            saving: false,
            applying: false,
            applyingAll: false,
            projets: [],
            message: null,
            messageType: 'success'
        };
    }

    componentDidMount() {
        this.loadRules();
        this.loadProjets();
    }

    loadProjets = () => {
        axios.get('/api/projet/list')
            .then(res => {
                this.setState({ 
                    projets: res.data.map(p => ({ label: p.nom, value: p.nom }))
                });
            })
            .catch(err => console.error('Error loading projets:', err));
    }

    loadRules = () => {
        this.setState({ loading: true });
        axios.get('/api/ctcToleranceRule')
            .then(res => {
                // Sort by priority descending
                const sortedRules = res.data.sort((a, b) => (b.priority || 0) - (a.priority || 0));
                this.setState({ rules: sortedRules, loading: false });
            })
            .catch(err => {
                console.error('Error loading rules:', err);
                this.setState({ loading: false });
            });
    }

    addNewRule = () => {
        const newRule = {
            id: null,
            projet: '',
            type: '',
            laminateFilter: 'all',
            applyOn: 'max',
            heightMin: 0,
            heightMax: null,
            toleranceMin1: null,
            toleranceMax1: null,
            toleranceMin2: null,
            toleranceMax2: null,
            toleranceDrill: null,
            priority: 0,
            active: true,
            isNew: true
        };
        this.setState({ rules: [newRule, ...this.state.rules] });
    }

    updateRule = (index, field, value) => {
        const rules = [...this.state.rules];
        rules[index] = { ...rules[index], [field]: value, modified: true };
        this.setState({ rules });
    }

    saveRule = (index) => {
        const rule = this.state.rules[index];
        this.setState({ saving: true });

        const savePromise = rule.id
            ? axios.put(`/api/ctcToleranceRule/${rule.id}`, rule)
            : axios.post('/api/ctcToleranceRule', rule);

        savePromise
            .then(res => {
                const rules = [...this.state.rules];
                rules[index] = { ...res.data, modified: false, isNew: false };
                this.setState({ 
                    rules, 
                    saving: false,
                    message: 'Règle enregistrée avec succès',
                    messageType: 'success'
                });
                setTimeout(() => this.setState({ message: null }), 3000);
            })
            .catch(err => {
                console.error('Error saving rule:', err);
                this.setState({ 
                    saving: false,
                    message: 'Erreur lors de l\'enregistrement',
                    messageType: 'danger'
                });
            });
    }

    deleteRule = (index) => {
        const rule = this.state.rules[index];
        if (!rule.id) {
            // New rule not yet saved
            const rules = [...this.state.rules];
            rules.splice(index, 1);
            this.setState({ rules });
            return;
        }

        if (!window.confirm('Êtes-vous sûr de vouloir supprimer cette règle ?')) {
            return;
        }

        axios.delete(`/api/ctcToleranceRule/${rule.id}`)
            .then(() => {
                const rules = [...this.state.rules];
                rules.splice(index, 1);
                this.setState({ 
                    rules,
                    message: 'Règle supprimée',
                    messageType: 'success'
                });
                setTimeout(() => this.setState({ message: null }), 3000);
            })
            .catch(err => {
                console.error('Error deleting rule:', err);
                this.setState({ 
                    message: 'Erreur lors de la suppression',
                    messageType: 'danger'
                });
            });
    }

    applyRule = (index) => {
        const rule = this.state.rules[index];
        if (!rule.id) {
            this.setState({
                message: 'Veuillez d\'abord enregistrer la règle',
                messageType: 'warning'
            });
            return;
        }

        this.setState({ applying: index });
        axios.post(`/api/ctcToleranceRule/${rule.id}/apply`)
            .then(res => {
                this.setState({ 
                    applying: false,
                    message: res.data.message || 'Règle appliquée',
                    messageType: res.data.success ? 'success' : 'warning'
                });
                setTimeout(() => this.setState({ message: null }), 5000);
            })
            .catch(err => {
                console.error('Error applying rule:', err);
                this.setState({ 
                    applying: false,
                    message: 'Erreur lors de l\'application',
                    messageType: 'danger'
                });
            });
    }

    applyAllRules = () => {
        if (!window.confirm('Appliquer toutes les règles actives ? Cette opération peut prendre du temps.')) {
            return;
        }

        this.setState({ applyingAll: true });
        axios.post('/api/ctcToleranceRule/apply-all')
            .then(res => {
                this.setState({ 
                    applyingAll: false,
                    message: `${res.data.totalRulesApplied} règles appliquées, ${res.data.totalPatternsUpdated} patterns mis à jour`,
                    messageType: 'success'
                });
                setTimeout(() => this.setState({ message: null }), 5000);
            })
            .catch(err => {
                console.error('Error applying all rules:', err);
                this.setState({ 
                    applyingAll: false,
                    message: 'Erreur lors de l\'application des règles',
                    messageType: 'danger'
                });
            });
    }

    render() {
        const { rules, loading, saving, applying, applyingAll, projets, message, messageType } = this.state;

        const typeOptions = [
            { label: 'Tous', value: '' },
            { label: 'Fabric', value: 'fabric' },
            { label: 'Supplier Kit Leather', value: 'supplier kit leather' },
            { label: 'Supplier Kit Fabric', value: 'supplier kit fabric' },
            { label: "CNC", value: "CNC" }
        ];

        const laminateOptions = [
            { label: 'Tous', value: 'all' },
            { label: 'Laminate uniquement (L)', value: 'laminate_only' },
            { label: 'Non-laminate uniquement', value: 'non_laminate_only' }
        ];

        const applyOnOptions = [
            { label: 'Max (H/L)', value: 'max' },
            { label: 'Hauteur', value: 'height' },
            { label: 'Largeur', value: 'width' }
        ];

        return (
            <div className="container-fluid" style={{ padding: '20px' }}>
                <div className="d-flex justify-content-between align-items-center mb-4">
                    <h2>Configuration des Règles de Tolérance CTC</h2>
                    <div>
                        <button 
                            className="btn btn-success mr-2"
                            onClick={this.addNewRule}
                        >
                            <FontAwesomeIcon icon={faPlus} className="mr-2" />
                            Nouvelle Règle
                        </button>
                        <button 
                            className="btn btn-primary mr-2"
                            onClick={this.loadRules}
                            disabled={loading}
                        >
                            <FontAwesomeIcon icon={faSync} spin={loading} className="mr-2" />
                            Rafraîchir
                        </button>
                        <button 
                            className="btn btn-warning"
                            onClick={this.applyAllRules}
                            disabled={applyingAll}
                        >
                            <FontAwesomeIcon icon={faPlay} spin={applyingAll} className="mr-2" />
                            Appliquer Toutes les Règles
                        </button>
                    </div>
                </div>

                {message && (
                    <div className={`alert alert-${messageType} alert-dismissible`}>
                        {message}
                        <button type="button" className="close" onClick={() => this.setState({ message: null })}>
                            <span>&times;</span>
                        </button>
                    </div>
                )}

                <div className="card mb-4">
                    <div className="card-header bg-dark text-white">
                        <strong>Instructions</strong>
                    </div>
                    <div className="card-body">
                        <p><strong>Intervalle de dimension:</strong> Définissez la plage de dimensions (min-max en mm). Laissez "Dimension Max" vide pour l'infini.</p>
                        <p><strong>Filtre Laminate:</strong> Appliquer uniquement aux pièces laminées (patterns finissant par "L") ou non.</p>
                        <p><strong>Appliquer sur:</strong> Choisissez si la règle s'applique sur la largeur, la hauteur, ou le max des deux.</p>
                        <p><strong>Priorité:</strong> Les règles avec une priorité plus haute sont appliquées en premier.</p>
                    </div>
                </div>

                {loading ? (
                    <div className="text-center p-5">
                        <FontAwesomeIcon icon={faSync} spin size="3x" />
                    </div>
                ) : (
                    <div className="table-responsive">
                        <table className="table table-bordered table-hover">
                            <thead className="thead-dark">
                                <tr>
                                    <th style={{ width: '80px' }}>Actif</th>
                                    <th style={{ width: '120px' }}>Projet</th>
                                    <th style={{ width: '150px' }}>Type</th>
                                    <th style={{ width: '140px' }}>Laminate</th>
                                    <th style={{ width: '100px' }}>Appliquer</th>
                                    <th style={{ width: '100px' }}>Dim Min</th>
                                    <th style={{ width: '100px' }}>Dim Max</th>
                                    <th style={{ width: '80px' }}>Tol Min1</th>
                                    <th style={{ width: '80px' }}>Tol Max1</th>
                                    <th style={{ width: '80px' }}>Tol Min2</th>
                                    <th style={{ width: '80px' }}>Tol Max2</th>
                                    <th style={{ width: '60px' }}>Priorité</th>
                                    <th style={{ width: '180px' }}>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {rules.map((rule, index) => (
                                    <tr key={rule.id || `new-${index}`} className={rule.isNew ? 'table-info' : (rule.modified ? 'table-warning' : '')}>
                                        <td className="text-center">
                                            <Switch
                                                checked={rule.active === true}
                                                onChange={(checked) => this.updateRule(index, 'active', checked)}
                                                onColor="#28a745"
                                                offColor="#dc3545"
                                                height={20}
                                                width={40}
                                            />
                                        </td>
                                        <td>
                                            <Select
                                                classNamePrefix="rs"
                                                value={projets.find(p => p.value === rule.projet) || { label: rule.projet, value: rule.projet }}
                                                options={projets}
                                                onChange={(opt) => this.updateRule(index, 'projet', opt ? opt.value : '')}
                                                isClearable
                                                placeholder="Projet"
                                            />
                                        </td>
                                        <td>
                                            <Select
                                                classNamePrefix="rs"
                                                value={typeOptions.find(t => t.value === rule.type)}
                                                options={typeOptions}
                                                onChange={(opt) => this.updateRule(index, 'type', opt ? opt.value : '')}
                                                placeholder="Type"
                                            />
                                        </td>
                                        <td>
                                            <Select
                                                classNamePrefix="rs"
                                                value={laminateOptions.find(l => l.value === rule.laminateFilter)}
                                                options={laminateOptions}
                                                onChange={(opt) => this.updateRule(index, 'laminateFilter', opt ? opt.value : 'all')}
                                            />
                                        </td>
                                        <td>
                                            <Select
                                                classNamePrefix="rs"
                                                value={applyOnOptions.find(a => a.value === rule.applyOn)}
                                                options={applyOnOptions}
                                                onChange={(opt) => this.updateRule(index, 'applyOn', opt ? opt.value : 'max')}
                                            />
                                        </td>
                                        <td>
                                            <input
                                                type="number"
                                                className="form-control form-control-sm"
                                                value={rule.heightMin || ''}
                                                onChange={(e) => this.updateRule(index, 'heightMin', e.target.value ? parseFloat(e.target.value) : 0)}
                                                placeholder="0"
                                            />
                                        </td>
                                        <td>
                                            <input
                                                type="number"
                                                className="form-control form-control-sm"
                                                value={rule.heightMax || ''}
                                                onChange={(e) => this.updateRule(index, 'heightMax', e.target.value ? parseFloat(e.target.value) : null)}
                                                placeholder="∞"
                                            />
                                        </td>
                                        <td>
                                            <input
                                                type="number"
                                                step="0.1"
                                                className="form-control form-control-sm"
                                                value={rule.toleranceMin1 ?? ''}
                                                onChange={(e) => this.updateRule(index, 'toleranceMin1', e.target.value ? parseFloat(e.target.value) : null)}
                                                placeholder="-1"
                                            />
                                        </td>
                                        <td>
                                            <input
                                                type="number"
                                                step="0.1"
                                                className="form-control form-control-sm"
                                                value={rule.toleranceMax1 ?? ''}
                                                onChange={(e) => this.updateRule(index, 'toleranceMax1', e.target.value ? parseFloat(e.target.value) : null)}
                                                placeholder="1"
                                            />
                                        </td>
                                        <td>
                                            <input
                                                type="number"
                                                step="0.1"
                                                className="form-control form-control-sm"
                                                value={rule.toleranceMin2 ?? ''}
                                                onChange={(e) => this.updateRule(index, 'toleranceMin2', e.target.value ? parseFloat(e.target.value) : null)}
                                                placeholder="-1"
                                            />
                                        </td>
                                        <td>
                                            <input
                                                type="number"
                                                step="0.1"
                                                className="form-control form-control-sm"
                                                value={rule.toleranceMax2 ?? ''}
                                                onChange={(e) => this.updateRule(index, 'toleranceMax2', e.target.value ? parseFloat(e.target.value) : null)}
                                                placeholder="1"
                                            />
                                        </td>
                                        <td>
                                            <input
                                                type="number"
                                                className="form-control form-control-sm"
                                                value={rule.priority ?? ''}
                                                onChange={(e) => this.updateRule(index, 'priority', e.target.value ? parseInt(e.target.value) : 0)}
                                                placeholder="0"
                                            />
                                        </td>
                                        <td>
                                            <button
                                                className="btn btn-sm btn-primary mr-1"
                                                onClick={() => this.saveRule(index)}
                                                disabled={saving}
                                                title="Enregistrer"
                                            >
                                                <FontAwesomeIcon icon={faSave} />
                                            </button>
                                            <button
                                                className="btn btn-sm btn-success mr-1"
                                                onClick={() => this.applyRule(index)}
                                                disabled={applying === index || !rule.id}
                                                title="Appliquer cette règle"
                                            >
                                                <FontAwesomeIcon icon={faPlay} spin={applying === index} />
                                            </button>
                                            <button
                                                className="btn btn-sm btn-danger"
                                                onClick={() => this.deleteRule(index)}
                                                title="Supprimer"
                                            >
                                                <FontAwesomeIcon icon={faTrash} />
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                {rules.length === 0 && (
                                    <tr>
                                        <td colSpan="13" className="text-center text-muted">
                                            Aucune règle configurée. Cliquez sur "Nouvelle Règle" pour en ajouter une.
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                )}

                <div className="card mt-4">
                    <div className="card-header bg-info text-white">
                        <strong>Exemple de Configuration</strong>
                    </div>
                    <div className="card-body">
                        <p>Pour configurer les intervalles de tolérance suivants pour le projet AU546 avec type "supplier kit leather":</p>
                        <ul>
                            <li>0 à 150mm: Tolérance ±1mm</li>
                            <li>150 à 300mm: Tolérance ±1.5mm</li>
                            <li>300 à 600mm: Tolérance ±2mm</li>
                            <li>600mm et plus: Tolérance ±3mm</li>
                        </ul>
                        <p>Créez 4 règles avec les mêmes projet/type mais des intervalles de dimension différents et les tolérances correspondantes.</p>
                    </div>
                </div>
            </div>
        );
    }
}
