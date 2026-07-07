import axios from 'axios';
import moment from 'moment';
import React, { Component } from 'react'
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { metadata, optionsShift } from '../../metadata';
import Select from "react-select";
import { faMagnifyingGlass, faPenAlt, faPlus, faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import DemandeDeCoupeForm from './DemandeDeCoupeForm';
import { Modal } from 'react-bootstrap'
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

class DemandeDeCoupe extends Component {

	constructor() {
		super();
		this.state = {
			entriesList: [],
			date: moment().add(2, 'hours').format('YYYY-MM-DD'),
			shift: this.getShift(moment().add(2, 'hours')),
			filter: {},
			zoneList: []
		}
	}

	getShift(date) {
		let hour = date.hour()
		if (hour >= 0 && hour < 8) {
			return 1
		} else if (hour >= 0 && hour < 8) {
			return 2
		} else {
			return 3
		}
	}


	componentDidMount() {
		this.getData(moment().add(2, 'hours').format('YYYY-MM-DD'), this.getShift(moment().add(2, 'hours')))
		axios.get(`/api/zone/list`)
			.then(res => {
				this.setState({ zoneList: res.data })
			})
	}

	searchSubEntriesPage = () => {
		this.getData(this.state.date, this.state.shift)
	}

	getData = (date, shift, zone) => {
		let filter = "date=" + date
		this.setState({ entriesList: null })
		if (shift) {
			filter += "&shift=" + shift
		}
		if (zone) {
			filter += "&zone=" + zone
		}
		axios.get(`/api/cuttingRequest/stat?${filter}`)
			.then(res => {
				this.setState({ entriesList: res.data })
			})
	}

	renderHeader = () => {
		/*
private String sequence;
	private Long cuttingPlanId;
	private String modele;
	private LocalDateTime planningDate;
	private String shift;
	private String version;
	private String zone_nom;
	private String createdBy_matricule;
	private LocalDateTime dateDebutMatelassage;
	private LocalDateTime dateFinMatelassage;
	private int waitingMatelassage;
	private int inProgressMatelassage;
	private int completeMatelassage;
	private int incompleteMatelassage;
	private LocalDateTime dateDebutCoupe;
	private LocalDateTime dateFinCoupe;
	private int waitingCoupe;
	private int inProgressCoupe;
	private int completeCoupe;
	private int incompleteCoupe;
		*/
		return <thead className='entity-table-header'>
			<tr>
				<th className='table-elem-sm'>Sequence</th>
				<th className='table-elem-sm'>zone</th>
				<th className='table-elem-sm' style={{ minWidth: 141 }}>Début Matelassage</th>
				<th className='table-elem-sm' style={{ minWidth: 141 }}>Fin Matelassage</th>
				<th className='table-elem-sm' style={{ minWidth: 141 }}>Matelassage</th>
				<th className='table-elem-sm' style={{ minWidth: 141 }}>Début Coupe</th>
				<th className='table-elem-sm' style={{ minWidth: 141 }}>Fin Coupe</th>
				<th className='table-elem-sm' style={{ minWidth: 141 }}>Coupe</th>
				<th className='table-elem-sm'>Modele</th>
				<th className='table-elem-sm'>Planning Date</th>
				<th className='table-elem-sm'>shift</th>

				<th className='table-elem-sm'></th>
			</tr>
			<tr>
				<th className='table-elem-sm'>
					<input value={this.state.filter.sequence} onChange={event => this.setState({ filter: { ...this.state.filter, sequence: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.zone_nom} onChange={event => this.setState({ filter: { ...this.state.filter, zone_nom: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.dateDebutMatelassage} onChange={event => this.setState({ filter: { ...this.state.filter, dateDebutMatelassage: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.dateFinMatelassage} onChange={event => this.setState({ filter: { ...this.state.filter, dateFinMatelassage: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.dateDebutCoupe} onChange={event => this.setState({ filter: { ...this.state.filter, dateDebutCoupe: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.dateFinCoupe} onChange={event => this.setState({ filter: { ...this.state.filter, dateFinCoupe: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.modele} onChange={event => this.setState({ filter: { ...this.state.filter, modele: event.target.value } })} />
				</th>

				<th className='table-elem-sm'>
					<input value={this.state.filter.planningDate} onChange={event => this.setState({ filter: { ...this.state.filter, planningDate: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.shift} onChange={event => this.setState({ filter: { ...this.state.filter, shift: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
				</th>
			</tr>
		</thead>
	}

	renderStatus = (arr) => {
		return <span style={{ whiteSpace: "nowrap" }}>
			<span style={{ color: "rgb(255 0 0)" }}>{arr.filter(e => e === "Waiting").length}</span> / <span style={{ color: "rgb(189 201 5)" }}>{arr.filter(e => e === "In progress").length}</span> / <span style={{ color: "rgb(21 219 8)" }}>{arr.filter(e => e === "Complete").length}</span> / <span style={{ color: "rgb(255 156 5)" }}>{arr.filter(e => e === "Incomplete").length}</span>
		</span>
	}

	renderRow(item, ind) {
		let arrMatelassage = [], arrCoupe = [];
		let style = { flex: 1, border: "1px grey solid" }
		for (let i = 0; i < item.waitingMatelassage; i++) arrMatelassage.push(<div style={{ backgroundColor: "red", ...style }}></div>)
		for (let i = 0; i < item.inProgressMatelassage; i++) arrMatelassage.push(<div style={{ backgroundColor: "yellow", ...style }}></div>)
		for (let i = 0; i < item.completeMatelassage; i++) arrMatelassage.push(<div style={{ backgroundColor: "green", ...style }}></div>)
		for (let i = 0; i < item.incompleteMatelassage; i++) arrMatelassage.push(<div style={{ backgroundColor: "orange", ...style }}></div>)
		for (let i = 0; i < item.waitingCoupe; i++) arrCoupe.push(<div style={{ backgroundColor: "red", ...style }}></div>)
		for (let i = 0; i < item.inProgressCoupe; i++) arrCoupe.push(<div style={{ backgroundColor: "yellow", ...style }}></div>)
		for (let i = 0; i < item.completeCoupe; i++) arrCoupe.push(<div style={{ backgroundColor: "green", ...style }}></div>)
		for (let i = 0; i < item.incompleteCoupe; i++) arrCoupe.push(<div style={{ backgroundColor: "orange", ...style }}></div>)
		return <tr key={"row-" + ind} className={"table-row-selective"}
			onDoubleClick={() => 
				// this.props.history.push("/demande-de-coupe/" + item.sequence)
				window.open(`/demande-de-coupe/${item.sequence}`, '_blank')
			}
		>
			<td className='table-elem-sm elem-no-wrap'>{item.sequence}</td>
			<td className='table-elem-sm elem-no-wrap'>{item.zone_nom}</td>
			<td className='table-elem-sm elem-no-wrap'>{item.dateDebutMatelassage}</td>
			<td className='table-elem-sm elem-no-wrap'>{item.dateFinMatelassage}</td>
			<td style={{ padding: 0, height: "100%" }}>
				<div style={{ display: "flex", width: "100%", height: "100% !important", minHeight: 30.5 }}>
					{arrMatelassage}
				</div>
			</td>
			<td className='table-elem-sm elem-no-wrap'>{item.dateDebutCoupe}</td>
			<td className='table-elem-sm elem-no-wrap'>{item.dateFinCoupe}</td>
			<td style={{ padding: 0, height: "100%" }}>
				<div style={{ display: "flex", width: "100%", height: "100% !important", minHeight: 30.5 }}>
					{arrCoupe}
				</div>
			</td>
			<td className='table-elem-sm elem-no-wrap'>{item.modele}</td>

			<td className='table-elem-sm elem-no-wrap'>{item.planningDate}</td>
			<td className='table-elem-sm elem-no-wrap'>{item.shift}</td>
			<td className='table-elem-sm elem-no-wrap' style={{ padding: "8", width: 50 }}>
				<div className='d-flex' style={{ margin: "auto 0" }}>
					<button type="button" className='btn btn btn-outline-dark btn-sm mr-1'
						onClick={() => { 
							this.props.history.push(`/demande-de-coupe/${item.sequence}`) 
							window.open(`/demande-de-coupe/${item.sequence}`, '_blank')
						}} style={{ fontSize: 12, padding: "3 6" }}>
						<FontAwesomeIcon icon={faPenAlt} />
					</button>
				</div>
			</td>
		</tr>
	}

	render() {
		let entity = "demande-de-coupe"
		let { entityId } = this.props.match.params
		const { user } = this.props.security;

		if (entity) {
			if (entityId) {
				return <DemandeDeCoupeForm entityId={entityId} user={user} goBack={() => {
					this.searchSubEntriesPage()
					this.props.history.push(`/${entity}`)
				}} />

			} else if (this.state.modalObj != null) {
				return <DemandeDeCoupeForm user={user} goBack={() => { this.searchSubEntriesPage(); this.setState({ modalObj: null }) }} />
			}
		}
		return (
			<div>
				<h1 className='text-center' style={{ margin: "10 0 8" }}>Statut Process Coupe</h1>
				<div className='d-flex align-items-center mb-1 mx-2'>

					<DatePicker
						id={"date"}
						name={"date"}
						placeholderText={"date"}
						className="form-control"
						autoComplete="off"
						selected={this.state.date ? moment(this.state.date, 'YYYY-MM-DD').toDate() : null}
						onChange={date => this.setState({ date: (date ? moment(date).format('YYYY-MM-DD') : null) })}
						isClearable={false}
						dateFormat={'yyyy-MM-dd'}
					/>
					<div style={{ width: 100, margin: "0 8" }}>
						<Select classNamePrefix="rs"
							placeholder={"Shift..."} style={{ width: 100 }}
							isClearable={true}
							value={this.state.shift ? { label: this.state.shift, value: this.state.shift } : null}
							options={optionsShift}
							onChange={(option) => {
								this.setState({ shift: option ? option.value : null })
							}}
						/>
					</div>
					<div style={{ width: 200, margin: "0 8" }}>
						<Select classNamePrefix="rs"
							placeholder={"Zone..."} style={{ width: 100 }}
							isClearable={true}
							value={this.state.zone ? { label: this.state.zone, value: this.state.zone } : null}
							options={this.state.zoneList.map(item => ({ label: item.nom, value: item.nom }))}
							onChange={(option) => {
								this.setState({ zone: option ? option.value : null })
							}}
						/>
					</div>

					<button className='btn btn-danger' onClick={() => {
						this.getData(this.state.date, this.state.shift, this.state.zone)
					}}>
						<FontAwesomeIcon icon={faMagnifyingGlass} />
					</button>
					<span className='ml-2'>Redirect to</span><input
						type="text"
						className="form-control ml-2" style={{ width: 250 }}
						placeholder="Sequence..."
						value={this.state.sequence}
						onChange={(e) => this.setState({ sequence: e.target.value })}
						onKeyPress={(e) => {
							if (e.key === 'Enter') {
								window.open(`/demande-de-coupe/${this.state.sequence}`, '_blank')
							}
						}}
					/>
					<input
						type="text"
						className="form-control ml-2" style={{ width: 150 }}
						placeholder="Serie..."
						value={this.state.serie}
						onChange={(e) => this.setState({ serie: e.target.value })}
						onKeyPress={(e) => {
							if (e.key === 'Enter') {
								axios.get("/api/cuttingRequestSerieData/" + this.state.serie)
									.then(respondSerie => {
										//open a new tab /demande-de-coupe/${respondSerie.data.sequence}
										window.open(`/demande-de-coupe/${respondSerie.data.sequence}`, '_blank')
									})
									.catch(err => {
										console.log(err)
									})
							}
						}}
					/>
					<input
						type="text"
						className="form-control ml-2" style={{ width: 150 }}
						placeholder="WO..."
						value={this.state.wo}
						onChange={(e) => this.setState({ wo: e.target.value })}
						onKeyPress={(e) => {
							if (e.key === 'Enter') {
								axios.get(`/api/cuttingRequestBoxData/all?equal.wo=${this.state.wo}&page=0&size=1&sort=id,desc`)
									.then(respondSerie => {
										//open a new tab /demande-de-coupe/${respondSerie.data.sequence}
										if(respondSerie.data && respondSerie.data.content && respondSerie.data.content.length > 0) {
											window.open(`/demande-de-coupe/${respondSerie.data.content[0].sequence}`, '_blank')
										}
									})
									.catch(err => {
										console.log(err)
									})
							}
						}}
					/>
					<input
						type="text"
						className="form-control ml-2" style={{ width: 150 }}
						placeholder="ID Box..."
						value={this.state.idBox}
						onChange={(e) => this.setState({ idBox: e.target.value })}
						onKeyPress={(e) => {
							if (e.key === 'Enter') {
								axios.get(`/api/cuttingRequestBoxData/all?equal.id=${this.state.idBox}&page=0&size=1&sort=id,desc`)
									.then(respondSerie => {
										//open a new tab /demande-de-coupe/${respondSerie.data.sequence}
										if(respondSerie.data && respondSerie.data.content && respondSerie.data.content.length > 0) {
											window.open(`/demande-de-coupe/${respondSerie.data.content[0].sequence}`, '_blank')
										}
									})
									.catch(err => {
										console.log(err)
									})
							}
						}}
					/>
					

					{/* <button type="button" className='btn btn-outline-danger ml-2' onClick={() => { this.setState({ modalObj: {} }) }} style={{ padding: 8 }}>
						<span style={{ fontSize: 12 }}><FontAwesomeIcon icon={faPlus} /></span>
					</button> */}
				</div>
				<div className='px-2'>
					<div className='table-responsive entity-table mb-2 slider-elem'>
						<table className='table table-bordered m-0'>
							{this.renderHeader()}
							<tbody>
								{this.state.entriesList == null ? <tr ><td colSpan={100}>Chargement...</td></tr> :
									this.state.entriesList.length === 0
										? <tr><td colSpan={100}>Aucune données disponibles</td> </tr>
										: this.state.entriesList
											.filter(elem => (
												(this.state.filter.sequence == null || elem.sequence.toUpperCase().startsWith(this.state.filter.sequence.toUpperCase())) &&
												(this.state.filter.modele == null || elem.modele?.toString().includes(this.state.filter.modele)) &&
												(this.state.filter.zone_nom == null || elem.zone_nom?.toString().startsWith(this.state.filter.zone_nom)) &&
												(this.state.filter.dateDebutMatelassage == null || elem.dateDebutMatelassage?.toString().startsWith(this.state.filter.dateDebutMatelassage)) &&
												(this.state.filter.dateFinMatelassage == null || elem.dateFinMatelassage?.toString().startsWith(this.state.filter.dateFinMatelassage)) &&
												(this.state.filter.dateDebutCoupe == null || elem.dateDebutCoupe?.toString().startsWith(this.state.filter.dateDebutCoupe)) &&
												(this.state.filter.dateFinCoupe == null || elem.dateFinCoupe?.toString().startsWith(this.state.filter.dateFinCoupe)) &&
												(this.state.filter.planningDate == null || elem.planningDate?.toString().startsWith(this.state.filter.planningDate)) &&
												(this.state.filter.shift == null || elem.shift?.toString().startsWith(this.state.filter.shift))
											))
											.map((item, ind) => {
												return this.renderRow(item, ind)
											})}
							</tbody>
						</table>
					</div>
				</div>
			</div>
		)
	}
}

DemandeDeCoupe.propTypes = {
	security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
	security: state.security
})

export default connect(mapStateToProps, {})(DemandeDeCoupe);

