import axios from 'axios';
import moment from 'moment';
import React, { Component } from 'react'
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { optionsShift } from '../../metadata';
import Select from "react-select";
import { faMagnifyingGlass, faPenAlt, faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { Modal } from 'react-bootstrap'
import FormCoupe from './FormCoupe';

export default class Matelassage extends Component {

	constructor() {
		super();
		this.state = {
			entriesList: [],
			date: moment().add(2, 'hours').format('YYYY-MM-DD'),
			shift: this.getShift(moment().add(2, 'hours')),
			type: "coupe",
			machine: null,
			productionTableList: [],
			filter: {}
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
		// this.getData(moment().add(2, 'hours').format('YYYY-MM-DD'), this.getShift(moment().add(2, 'hours')), "coupe", null)
		axios.get("/api/productionTable/list")
		.then(res => {
			this.setState({ productionTableList: res.data })
		})
		.catch(err => {
			console.log(err)
		})
	}

	getData = (date, shift, type, machine) => {
		let filter = "date=" + date
		this.setState({ entriesList: null })
		if (shift) {
			filter += "&shift=" + shift
		}

		if (type) {
			filter += "&type=" + type
		}

		if (machine) {
			filter += "&machine=" + machine
		}

		axios.get(`/api/cuttingRequestSerieInfo/filtre?${filter}`)
			.then(res => {
				this.setState({ entriesList: res.data })
			})
	}

	renderHeader = () => {
		return <thead className='entity-table-header'>
			<tr>
				<th className='table-elem-sm'>Serie</th>
				<th className='table-elem-sm'>Séquence</th>
				<th className='table-elem-sm'>Part Number Material</th>
				<th className='table-elem-sm'>Description</th>
				<th className='table-elem-sm'>Placement</th>
				<th className='table-elem-sm'>Nombre de couche</th>
				<th className='table-elem-sm'>Longueur</th>
				<th className='table-elem-sm'>Date planning</th>
				<th className='table-elem-sm'>Shift</th>
				<th className='table-elem-sm'>debut</th>
				<th className='table-elem-sm'>fin</th>
				<th className='table-elem-sm'>Matelassage</th>
				<th className='table-elem-sm'>debut</th>
				<th className='table-elem-sm'>fin</th>
				<th className='table-elem-sm'>Coupe</th>
				{/* <th className='table-elem-sm'></th> */}
			</tr>
			<tr>
				<th className='table-elem-sm'>
					<input value={this.state.filter.serie} onChange={event => this.setState({ filter: { ...this.state.filter, serie: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.sequence} onChange={event => this.setState({ filter: { ...this.state.filter, sequence: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.partNumberMaterial} onChange={event => this.setState({ filter: { ...this.state.filter, partNumberMaterial: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.description} onChange={event => this.setState({ filter: { ...this.state.filter, description: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.placement} onChange={event => this.setState({ filter: { ...this.state.filter, placement: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.nbrCouche} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, nbrCouche: event.target.value === "" ? null : parseInt(event.target.value) } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.longueur} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, longueur: event.target.value === "" ? null : parseInt(event.target.value) } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.planningDate} onChange={event => this.setState({ filter: { ...this.state.filter, planningDate: event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.shift} onChange={event => this.setState({ filter: { ...this.state.filter, shift: event.target.value } })} />
				</th>
				<th className='table-elem-sm'></th>
				<th className='table-elem-sm'></th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.statusMatelassage} onChange={event => this.setState({ filter: { ...this.state.filter, statusMatelassage: event.target.value } })} />
				</th>
				<th className='table-elem-sm'></th>
				<th className='table-elem-sm'></th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.statusCoupe} onChange={event => this.setState({ filter: { ...this.state.filter, statusCoupe: event.target.value } })} />
				</th>
				{/* <th></th> */}
			</tr>
		</thead>
	}

	returnColorStatus = (status) => {
		switch (status) {
			case "Waiting":
				return "#ffafaf"
			case "In progress":
				return "#f6ff6b"
			case "Complete":
				return "#7bff6b"
			case "Incomplete":
				return "#ffc46b"
			default:
				return ""
		}
	}

	renderRow(item, ind) {
		return <tr key={"row-" + ind} className={"clickable-element"} onDoubleClick={() => {
			this.setState({ showSerieForm: item.serie })
		}}>
			<td className='table-elem-sm'>{item.serie}</td>
			<td className='table-elem-sm'>{item.cuttingRequest && item.cuttingRequest.sequence}</td>
			<td className='table-elem-sm'>{item.partNumberMaterial}</td>
			<td className='table-elem-sm' style={{ whiteSpace: "nowrap" }}>{item.description}</td>
			<td className='table-elem-sm'>{item.placement}</td>
			<td className='table-elem-sm'>{item.nbrCouche}</td>
			<td className='table-elem-sm'>{item.longueur}</td>
			<td className='table-elem-sm'>{item.planningDate}</td>
			<td className='table-elem-sm'>{item.shift}</td>
			<td className='table-elem-sm'>{item.dateDebutMatelassage && moment(item.dateDebutMatelassage).format("YYYY-MM-DD HH:mm")}</td>
			<td className='table-elem-sm'>{item.dateFinMatelassage && moment(item.dateFinMatelassage).format("YYYY-MM-DD HH:mm")}</td>
			<td className='table-elem-sm'
				style={{ backgroundColor: this.returnColorStatus(item.statusMatelassage) }}
			>{item.statusMatelassage}</td>
			<td className='table-elem-sm'>{item.dateDebutCoupe && moment(item.dateDebutCoupe).format("YYYY-MM-DD HH:mm")}</td>
			<td className='table-elem-sm'>{item.dateFinCoupe && moment(item.dateFinCoupe).format("YYYY-MM-DD HH:mm")}</td>
			<td className='table-elem-sm'
				style={{ backgroundColor: this.returnColorStatus(item.statusCoupe) }}
			>{item.statusCoupe}</td>
			{/* <td className='table-elem-sm' style={{ padding: "8", width: 50 }}>
				<div className='d-flex' style={{ margin: "auto 0" }}>
					<button type="button" className='btn btn btn-outline-dark btn-sm mr-1'
						onClick={() => {
							this.setState({ showSerieForm: item.serie })
						}} style={{ fontSize: 12, padding: "3 6" }}>
						<FontAwesomeIcon icon={faPenAlt} />
					</button>
					<button type="button" className='btn btn btn-outline-dark btn-sm' style={{ fontSize: 12, padding: "3 6" }}>
						<FontAwesomeIcon icon={faTimes} />
					</button>
				</div>

			</td> */}
		</tr>
	}

	serieModal = () => {
		return <Modal
			show={this.state.showSerieForm !== null}
			onHide={() => this.setState({ showSerieForm: null })}
			dialogClassName="modal-90w"
			centered
		>
			{this.state.showSerieForm && <div style={{ height: "75vh", overflowY: 'auto' }}>
				<FormCoupe serie={this.state.showSerieForm} />
			</div>}
		</Modal>
	}

	render() {
		return (
			<div>
				<h1 className='text-center' style={{ margin: "10 0 8" }}>Matelassage / Coupe</h1>
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
					{this.state.date && <div style={{ width: 100, margin: "0 8" }}>
						<Select classNamePrefix="rs"
							placeholder={"Shift..."} style={{ width: 100 }}
							isClearable={true}
							value={this.state.shift ? { label: this.state.shift, value: this.state.shift } : null}
							options={optionsShift}
							onChange={(option) => {
								this.setState({ shift: option ? option.value : null })
							}}
						/>
					</div>}
					{this.state.date && <div style={{ width: 250, margin: "0 8" }}>
						<Select classNamePrefix="rs"
							placeholder={"type..."} style={{ width: 250 }}
							isClearable={true}
							value={this.state.type ? { label: this.state.type, value: this.state.type } : null}
							options={[{ label: "matelassage", value: "matelassage" }, { label: "coupe", value: "coupe" }]}
							onChange={(option) => {
								this.setState({ type: option ? option.value : null })
							}}
						/>
					</div>}
					{this.state.date && <div style={{ width: 150, margin: "0 8" }}>
						<Select classNamePrefix="rs"
							placeholder={"Machine..."} style={{ width: 150 }}
							isClearable={true}
							value={this.state.machine ? { label: this.state.machine, value: this.state.machine } : null}
							options={this.state.productionTableList.map((item) => { return { label: item.nom, value: item.nom } })}
							onChange={(option) => {
								this.setState({ machine: option ? option.value : null })
							}}
						/>
					</div>}
					<button className='btn btn-danger' onClick={() => {
						this.getData(this.state.date, this.state.shift, this.state.type, this.state.machine)
					}}>
						<FontAwesomeIcon icon={faMagnifyingGlass} />
					</button>
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
												(this.state.filter.serie == null || elem.serie.toUpperCase().startsWith(this.state.filter.serie.toUpperCase())) &&
												(this.state.filter.sequence == null || elem.cuttingRequest.sequence.toUpperCase().startsWith(this.state.filter.sequence.toUpperCase())) &&
												(this.state.filter.partNumberMaterial == null || elem.partNumberMaterial.toString().startsWith(this.state.filter.partNumberMaterial)) &&
												(this.state.filter.nbrCouche == null || elem.nbrCouche.toString().includes(this.state.filter.nbrCouche)) &&
												(this.state.filter.planningDate == null || elem.planningDate.toString().startsWith(this.state.filter.planningDate)) &&
												(this.state.filter.shift == null || elem.shift.toString().startsWith(this.state.filter.shift)) &&
												(this.state.filter.nbrCouche == null || elem.nbrCouche === (this.state.filter.nbrCouche)) &&
												(this.state.filter.longueur == null || elem.longueur === (this.state.filter.longueur)) &&
												(this.state.filter.statusMatelassage == null || elem.statusMatelassage.toUpperCase().toString().startsWith(this.state.filter.statusMatelassage.toUpperCase())) &&
												(this.state.filter.statusCoupe == null || elem.statusCoupe.toUpperCase().toString().startsWith(this.state.filter.statusCoupe.toUpperCase()))
											))
											.sort((a,b) => {
												if(this.state.type === "matelassage") {
													// sort by date debut matelassage asc
													return a.dateDebutMatelassage > b.dateDebutMatelassage ? 1 : -1
												} else {
													// sort by date debut coupe asc
													return a.dateDebutCoupe > b.dateDebutCoupe ? 1 : -1
												}
												
											})
											.map((item , ind) => {
												return this.renderRow(item, ind)
											})}
							</tbody>
						</table>
					</div>
				</div>
				{this.state.showSerieForm && this.serieModal()}
			</div>
		)
	}
}
