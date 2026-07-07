import { faMagnifyingGlass, faFileCsv, faExclamationTriangle, faTable, faChartBar } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { Component } from 'react'
import Select from "react-select";
import axios from 'axios';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ReferenceLine, Cell } from 'recharts';

const darkColors = [
	'#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd',
	'#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf',
	'#115f9a', '#1984c5', '#22a7f0', '#48b5c4', '#76c68f'
];

const types = [
	'Blade',
	'Drill',
	'SharpeningBelt'
];

const yAxisOptions = [
	{ value: 'value', label: 'Avg %', max: null },
	{ value: 'value1', label: 'Avg Perimeter per week', max: 4500 },
	{ value: 'value2', label: 'Avg Blade Sharpenings', max: 4500 }
];

export default class ConsumableStatWeek extends Component {

	constructor(props) {
		super(props)
		this.state = {
			data: [],
			machineOptions: [],
			machines: [],
			type: "Blade",
			loading: false,
			alerts: [],
			mode: "table",
			yAxisColumn: "value1"
		}
	}

	componentDidMount() {
		this.getData()
	}

	getCurrentWeek = () => {
		const now = new Date();
		const startOfYear = new Date(now.getFullYear(), 0, 1);
		const pastDaysOfYear = (now - startOfYear) / 86400000;
		return Math.ceil((pastDaysOfYear + startOfYear.getDay() + 1) / 7);
	}

	getCurrentYear = () => {
		return new Date().getFullYear();
	}

	getData() {
		this.setState({ data: [], loading: true, alerts: [] })
		axios.get(`/api/consumable/stat?type=${this.state.type}`)
			.then(response => {
				const currentWeek = this.getCurrentWeek();
				const currentYear = this.getCurrentYear();
				
				// Filter only current week data
				const weekData = response.data.filter(e => 
					e.weekNumber === currentWeek && e.year === currentYear
				);

				let arr = []
				weekData.forEach(e => {
					if (!arr.includes(e.machine)) {
						arr.push(e.machine)
					}
				})

				// Check for alerts (avg perim > 4500 or avg sharpenings > 4500)
				const alerts = weekData.filter(e => e.value1 > 4500 || e.value2 > 4500).map(e => {
					let messages = [];
					if (e.value1 > 4500) {
						messages.push(`Avg Perimeter: ${e.value1?.toFixed(2)}`);
					}
					if (e.value2 > 4500) {
						messages.push(`Avg Sharpenings: ${e.value2?.toFixed(2)}`);
					}
					return {
						machine: e.machine,
						message: `Machine ${e.machine} a dépassé le seuil de 4500! (${messages.join(', ')})`
					};
				});

				this.setState({ 
					data: weekData, 
					machineOptions: arr, 
					loading: false,
					alerts 
				})
			})
			.catch(error => {
				console.log(error)
				this.setState({ loading: false })
			})
	}

	downloadCSV = () => {
		const items = this.state.data
		.filter(e => {
			return this.state.machines.length === 0 || this.state.machines.includes(e.machine)
		})
		.map(e => {
			return {
				"Machine": e.machine,
				"Year": e.year,
				"Week": e.weekNumber,
				"Total": e.total,
				"Avg %": e.value,
				"Avg Perimeter per week": e.value1,
				"Avg Blade Sharpenings": e.value2,
				"BladeDimension": e.value4,
			}
		})
		if (items.length === 0) return;
		
		const replacer = (key, value) => value === null ? '' : value
		const header = Object.keys(items[0])
		let csv = items.map(row => header.map(fieldName => JSON.stringify(row[fieldName], replacer)).join(','))
		csv.unshift(header.join(','))
		csv = csv.join('\r\n')
		const blob = new Blob([csv], { type: 'text/csv' });
		const url = window.URL.createObjectURL(blob);
		const a = document.createElement('a');
		a.href = url;
		a.download = `consumable-stat-week${this.getCurrentWeek()}.csv`;
		a.click();
		window.URL.revokeObjectURL(url);
	}

	getYAxisConfig = () => {
		const selected = yAxisOptions.find(o => o.value === this.state.yAxisColumn);
		return selected || yAxisOptions[1];
	}

	renderBarChart = () => {
		const yConfig = this.getYAxisConfig();
		const filteredData = this.state.data
			.filter(e => this.state.machines.length === 0 || this.state.machines.includes(e.machine))
			.map(e => ({
				machine: e.machine,
				value: e[this.state.yAxisColumn],
				isOverLimit: yConfig.max && e[this.state.yAxisColumn] > yConfig.max
			}));

		return (
			<div style={{ height: '500px', width: '100%' }}>
				<ResponsiveContainer width="100%" height="100%">
					<BarChart
						data={filteredData}
						margin={{ top: 20, right: 30, left: 20, bottom: 60 }}
					>
						<CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
						<XAxis 
							dataKey="machine" 
							angle={-45} 
							textAnchor="end" 
							height={80}
							tick={{ fontSize: 11 }}
						/>
						<YAxis 
							domain={yConfig.max ? [0, yConfig.max] : ['auto', 'auto']}
							tick={{ fontSize: 11 }}
						/>
						<Tooltip 
							contentStyle={{ backgroundColor: '#fff', border: '1px solid #ccc', borderRadius: '4px' }}
							formatter={(value) => [value?.toFixed(2), yConfig.label]}
						/>
						<Legend wrapperStyle={{ paddingTop: '20px' }} />
						{yConfig.max && (
							<ReferenceLine 
								y={yConfig.max} 
								stroke="red" 
								strokeWidth={2}
								strokeDasharray="5 5" 
								label={{ value: `Max: ${yConfig.max}`, fill: 'red', fontSize: 12, position: 'right' }} 
							/>
						)}
						<Bar 
							dataKey="value" 
							name={yConfig.label}
							radius={[4, 4, 0, 0]}
						>
							{filteredData.map((entry, index) => (
								<Cell 
									key={`cell-${index}`} 
									fill={entry.isOverLimit ? '#dc3545' : darkColors[index % darkColors.length]} 
								/>
							))}
						</Bar>
					</BarChart>
				</ResponsiveContainer>
			</div>
		);
	}

	render() {
		const currentWeek = this.getCurrentWeek();
		const currentYear = this.getCurrentYear();
		const yConfig = this.getYAxisConfig();

		return (
			<div className="container-fluid" style={{ padding: '20px', backgroundColor: '#f8f9fa', minHeight: '100vh' }}>
				{/* Header */}
				<div className="card mb-4 shadow-sm">
					<div className="card-header bg-info text-white d-flex justify-content-between align-items-center">
						<h4 className="mb-0">
							<FontAwesomeIcon icon={faChartBar} className="mr-2" />
							Consumable Stats - Semaine {currentWeek} ({currentYear})
						</h4>
						<div className="btn-group">
							<button 
								className={`btn btn-sm ${this.state.mode === 'table' ? 'btn-light' : 'btn-outline-light'}`}
								onClick={() => this.setState({ mode: 'table' })}
							>
								<FontAwesomeIcon icon={faTable} className="mr-1" /> Table
							</button>
							<button 
								className={`btn btn-sm ${this.state.mode === 'chart' ? 'btn-light' : 'btn-outline-light'}`}
								onClick={() => this.setState({ mode: 'chart' })}
							>
								<FontAwesomeIcon icon={faChartBar} className="mr-1" /> Graph
							</button>
						</div>
					</div>
					<div className="card-body">
						<div className="row align-items-end">
							<div className="col-md-2">
								<label className="small text-muted mb-1">Type</label>
								<Select 
									classNamePrefix="rs"
									placeholder={"Type..."} 
									isClearable={false}
									value={this.state.type ? { label: this.state.type, value: this.state.type } : null}
									options={types.map(type => ({ label: type, value: type }))}
									onChange={(option) => {
										if (option) {
											this.setState({ type: option.value, data: [] }, () => this.getData())
										}
									}}
								/>
							</div>
							<div className="col-md-3">
								<label className="small text-muted mb-1">Machines</label>
								<Select 
									classNamePrefix="rs"
									isClearable={false} 
									isMulti
									placeholder="All machines..."
									value={this.state.machines.map(e => ({ label: e, value: e }))}
									options={this.state.machineOptions.map(e => ({ label: e, value: e }))}
									onChange={(options) => this.setState({ machines: options ? options.map(option => option.value) : [] })}
								/>
							</div>
							{this.state.mode === 'chart' && (
								<div className="col-md-3">
									<label className="small text-muted mb-1">Y-Axis Column</label>
									<Select 
										classNamePrefix="rs"
										isClearable={false}
										value={yAxisOptions.find(o => o.value === this.state.yAxisColumn)}
										options={yAxisOptions}
										onChange={(option) => this.setState({ yAxisColumn: option.value })}
									/>
								</div>
							)}
							<div className="col-md-2">
								<button 
									onClick={() => this.getData()} 
									className='btn btn-primary mr-1'
									disabled={this.state.loading}
								>
									<FontAwesomeIcon icon={faMagnifyingGlass} spin={this.state.loading} />
								</button>
								<button 
									onClick={() => this.downloadCSV()} 
									className='btn btn-success'
									disabled={this.state.data.length === 0}
								>
									<FontAwesomeIcon icon={faFileCsv} />
								</button>
							</div>
						</div>
					</div>
				</div>

				{/* Alerts Section */}
				{this.state.alerts.length > 0 && (
					<div className="mb-4">
						{this.state.alerts.map((alert, index) => (
							<div key={index} className="alert alert-warning d-flex align-items-center shadow-sm" role="alert">
								<FontAwesomeIcon icon={faExclamationTriangle} className="mr-2" style={{ color: '#856404' }} />
								<strong>{alert.message}</strong>
							</div>
						))}
					</div>
				)}

				{/* Content */}
				{this.state.loading ? (
					<div className="text-center p-5">
						<div className="spinner-border text-primary" role="status">
							<span className="sr-only">Loading...</span>
						</div>
					</div>
				) : (
					<div className="card shadow-sm">
						<div className="card-body">
							{this.state.mode === "table" && (
								<div className='table-responsive'>
									<table className='table table-striped table-bordered table-hover table-sm'>
										<thead className="thead-dark">
											<tr>
												<th>Machine</th>
												<th>Year</th>
												<th>Week</th>
												<th>Total</th>
												<th>Avg %</th>
												<th>
													Avg Perimeter/Week
													<span className="badge badge-warning ml-1" title="Alert if > 4500">!</span>
												</th>
												<th>
													Avg Blade Sharpenings
													<span className="badge badge-warning ml-1" title="Alert if > 4500">!</span>
												</th>
												<th>BladeDimension</th>
											</tr>
										</thead>
										<tbody>
											{this.state.data
											.filter(e => this.state.machines.length === 0 || this.state.machines.includes(e.machine))
											.map((item, index) => {
												const isHighPerim = item.value1 > 4500;
												const isHighSharp = item.value2 > 4500;
												return (
													<tr key={index}>
														<td><strong>{item.machine}</strong></td>
														<td>{item.year}</td>
														<td>{item.weekNumber}</td>
														<td>{item.total}</td>
														<td>{item.value?.toFixed(2)}</td>
														<td className={isHighPerim ? 'table-danger' : ''}>
															{item.value1?.toFixed(2)}
															{isHighPerim && <FontAwesomeIcon icon={faExclamationTriangle} className="ml-1 text-danger" />}
														</td>
														<td className={isHighSharp ? 'table-danger' : ''}>
															{item.value2?.toFixed(2)}
															{isHighSharp && <FontAwesomeIcon icon={faExclamationTriangle} className="ml-1 text-danger" />}
														</td>
														<td>{item.value4}</td>
													</tr>
												)
											})}
											{this.state.data.length === 0 && (
												<tr>
													<td colSpan="8" className="text-center text-muted">
														Aucune donnée pour la semaine actuelle
													</td>
												</tr>
											)}
										</tbody>
									</table>
								</div>
							)}
							{this.state.mode === "chart" && (
								<div>
									<h5 className='text-center mb-3'>
										{yConfig.label} by Machine - Week {currentWeek}
										{yConfig.max && <span className="badge badge-danger ml-2">Max: {yConfig.max}</span>}
									</h5>
									{this.renderBarChart()}
								</div>
							)}
						</div>
					</div>
				)}
			</div>
		)
	}
}
