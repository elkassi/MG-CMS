import axios from 'axios'
import React, { Component } from 'react'
import Select from "react-select";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { optionsShift } from '../../metadata';
import moment from 'moment';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faArrowRight, faArrowUp, faCheck, faMagnifyingGlass, faPrint, faTimes } from '@fortawesome/free-solid-svg-icons';
import "../../styles/Importation.scss"
import { Modal } from 'react-bootstrap'
import GammePn from './GammePn';
import ReactToPrint from "react-to-print";
import logo from '../../assets/images/lear_logo.png'
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

class Importation extends Component {

	constructor() {
		super();
		this.state = {
			entriesList: [],
			filter: {},
			optionsList: {},
			date: moment().add(10, 'hours').format('YYYY-MM-DD'),
			shift: this.getShift(moment().add(10, 'hours')),
			left: "0%",
			importModalInd: null,
			arrImport: [],
			cuttingPlanSelected: [],
			selectedBoxs: []
		}
	}

	getShift(date) {
		let hour = date.hour()
		if (hour >= 0 && hour < 8) {
			return 1
		} else if (hour >= 8 && hour < 16) {
			return 2
		} else {
			return 3
		}
	}

	componentDidMount() {

		this.setState({ entriesList: null })
		axios.get(`/api/planning/${this.state.date}/${this.state.shift}`)
			.then(res => {
				let arr = res.data.sort((a, b) => (a.rowId - (b.rowId)))
				this.setState({ entriesList: arr })
			})
		axios.get(`/api/zone/list`)
			.then((res) => {
				this.setState({
					optionsList: {
						...this.state.optionsList,
						zone: res.data.map((elem) => ({ value: elem.nom, label: elem.nom, item: elem }))
					}
				})
			})
		axios.get(`/api/projet/list`)
			.then((res) => {
				this.setState({
					optionsList: {
						...this.state.optionsList,
						projet: res.data.map((elem) => ({ value: elem.nom, label: elem.nom, item: elem }))
					}
				})
			})
	}

	getMarge = (cpmp, nbrCouche) => {
		let marge = 0
		let arr = cpmp.pliesConfigMarge.split("|").map(e => e.split(";").map(numb => parseFloat(numb))).sort((a, b) => a[0] - b[0])
		for (let i = 0; i < arr.length; i++) {
			if (nbrCouche >= arr[i][0]) {
				marge = arr[i][1]
			}
		}
		return marge
	}

	renderRow(item, ind) {
		return <tr key={"row-" + ind}>
			<td className='table-elem-sm'>{item.rowId}</td>
			<td className='table-elem-sm'>{item.partNumber}</td>
			<td className='table-elem-sm'>{item.description}</td>
			<td className='table-elem-sm'>{item.item}</td>
			<td className='table-elem-sm'>{item.groupName}</td>
			<td className='table-elem-sm'>{item.designGroup}</td>
			<td className='table-elem-sm'>{item.coverGroup}</td>
			<td className='table-elem-sm'>{item.status}</td>
			<td
				className='table-elem-sm'
				style={(item.color && item.color.length > 2) ? { backgroundColor: ("#" + item.color.slice(2)) } : {}}
			>{item.quantity}</td>
			<td className='table-elem-sm'>{item.commentaire}</td>

		</tr>
	}

	renderHeader = () => {
		return <thead className='entity-table-header'>
			<tr>
				<th className='table-elem-sm'>Row Id</th>
				<th className='table-elem-sm'>Part Number</th>
				<th className='table-elem-sm'>Description</th>
				<th className='table-elem-sm'>Item</th>
				<th className='table-elem-sm'>Group</th>
				<th className='table-elem-sm'>Design Group</th>
				<th className='table-elem-sm'>Cover Group</th>
				<th className='table-elem-sm'>Status</th>
				<th className='table-elem-sm'>Quantité</th>
				<th className='table-elem-sm'>Commentaire</th>
			</tr>
			<tr>
				<th className='table-elem-sm'>
					<input value={this.state.filter.rowId} onChange={event => this.setState({ filter: { ...this.state.filter, rowId: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.partNumber} onChange={event => this.setState({ filter: { ...this.state.filter, partNumber: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.description} onChange={event => this.setState({ filter: { ...this.state.filter, description: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.item} onChange={event => this.setState({ filter: { ...this.state.filter, item: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.groupName} onChange={event => this.setState({ filter: { ...this.state.filter, groupName: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.designGroup} onChange={event => this.setState({ filter: { ...this.state.filter, designGroup: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.coverGroup} onChange={event => this.setState({ filter: { ...this.state.filter, coverGroup: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.status} onChange={event => this.setState({ filter: { ...this.state.filter, status: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.quantity} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, quantity: event.target.value === "" ? null : parseInt(event.target.value) } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.commentaire} onChange={event => this.setState({ filter: { ...this.state.filter, commentaire: event.target.value === "" ? null : event.target.value } })} />
				</th>
			</tr>
		</thead>
	}





	refreshPlanning = () => {
		axios.get(`/api/planning/${this.state.date}/${this.state.shift}`)
			.then(res => {
				let arr = res.data.sort((a, b) => (a.rowId - (b.rowId)))
				this.setState({ entriesList: arr })
			})
	}

	renderPlanningTable = () => {
		return <div className='table-responsive entity-table mb-2 slider-elem'>
			<table className='table table-bordered m-0'>
				{this.renderHeader()}
				<tbody>
					{this.state.entriesList == null ? <tr ><td colSpan={100}>Chargement...</td></tr> :
						this.state.entriesList.length === 0
							? <tr><td colSpan={100}>Aucune données disponibles</td> </tr>
							: this.state.entriesList
								.filter(elem => (
									(this.state.filter.rowId == null || (elem.rowId && elem.rowId.toString().toUpperCase().startsWith(this.state.filter.rowId.toString().toUpperCase()))) &&
									(this.state.filter.partNumber == null || (elem.partNumber && elem.partNumber.toUpperCase().startsWith(this.state.filter.partNumber.toUpperCase()))) &&
									(this.state.filter.description == null || (elem.description && elem.description.toUpperCase().startsWith(this.state.filter.description.toUpperCase()))) &&
									(this.state.filter.item == null || (elem.item && elem.item.toUpperCase().startsWith(this.state.filter.item.toUpperCase()))) &&
									(this.state.filter.groupName == null || (elem.groupName && elem.groupName.toUpperCase().startsWith(this.state.filter.groupName.toUpperCase()))) &&
									(this.state.filter.designGroup == null || (elem.designGroup && elem.designGroup.toUpperCase().startsWith(this.state.filter.designGroup.toUpperCase()))) &&
									(this.state.filter.coverGroup == null || (elem.coverGroup && elem.coverGroup.toUpperCase().startsWith(this.state.filter.coverGroup.toUpperCase()))) &&
									(this.state.filter.status == null || (elem.status && elem.status.toUpperCase().startsWith(this.state.filter.status.toUpperCase()))) &&
									(this.state.filter.quantity == null || (elem.quantity && elem.quantity.toString().toUpperCase().startsWith(this.state.filter.quantity.toString().toUpperCase()))) &&
									(this.state.filter.commentaire == null || (elem.commentaire && elem.commentaire.toUpperCase().startsWith(this.state.filter.commentaire.toUpperCase())))
								))
								.map((item, ind) => {
									return this.renderRow(item, ind)
								})}
				</tbody>
			</table>
		</div>
	}


	render() {
		console.log(this.state)
		return (
			<div>
				<h1 className=''
					style={{ marginTop: 10, display: "flex", justifyContent: "center" }}>
					<span>Planning Excel</span>
				</h1>
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
							placeholder={"Date"} style={{ width: 100 }}
							isClearable={false}
							value={this.state.shift ? { label: this.state.shift, value: this.state.shift } : null}
							options={optionsShift}
							onChange={(option) => {
								this.setState({ shift: option.value })
							}}
						/>
					</div>}

					{this.state.date && <button className='btn btn-danger' onClick={() => {
						this.setState({ entriesList: null })
						axios.get(`/api/planning/${this.state.date}/${this.state.shift}`)
							.then(res => {
								let arr = res.data.sort((a, b) => (a.rowId - (b.rowId)))
								this.setState({ entriesList: arr })
							})
					}}>
						<FontAwesomeIcon icon={faMagnifyingGlass} />
					</button>}
				</div>
				<div className='px-2 slider-container'>
					<div className='slider-list' style={{ left: this.state.left }}>
						{this.renderPlanningTable()}
					</div>
				</div>
			</div>
		)
	}
}

Importation.propTypes = {
	security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
	security: state.security
})


export default connect(mapStateToProps, {})(Importation);