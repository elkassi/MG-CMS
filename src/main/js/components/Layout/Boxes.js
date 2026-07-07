import axios from 'axios';
import moment from 'moment';
import React, { Component } from 'react'
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { optionsShift } from '../../metadata';
import Select from "react-select";
import { faMagnifyingGlass, faPenAlt, faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

export default class Boxes extends Component {

	constructor() {
		super();
		this.state = {
			entriesList: [],
			date: moment().add(2, 'hours').format('YYYY-MM-DD'),
			shift: this.getShift(moment().add(2, 'hours')),
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
		this.getData(moment().add(2, 'hours').format('YYYY-MM-DD'), this.getShift(moment().add(2, 'hours')))
	}

	getData = (date, shift) => {
		let filter = "date="+date
		this.setState({ entriesList: null })
		if(shift) {
			filter+= "&shift="+shift
		}
		axios.get(`/api/cuttingRequestBoxInfo?${filter}`)
			.then(res => {
				this.setState({ entriesList: res.data })
			})
	}

	renderHeader = () => {
		return <thead className='entity-table-header'>
			<tr>
				<th className='table-elem-sm'>ref</th>
				<th className='table-elem-sm'>Sequence</th>
				<th className='table-elem-sm'>partNumber</th>
				<th className='table-elem-sm'>description</th>
				<th className='table-elem-sm'>item</th>
				<th className='table-elem-sm'>wo</th>
				<th className='table-elem-sm'>woid</th>
				<th className='table-elem-sm'>qtyBox</th>
                <th></th>
			</tr>
            <tr>
				<th className='table-elem-sm'>
                    <input value={this.state.filter.id} onChange={event => this.setState({filter: {...this.state.filter, id: event.target.value}})} />
                </th>
				<th className='table-elem-sm'>
                    <input value={this.state.filter.sequence} onChange={event => this.setState({filter: {...this.state.filter, sequence: event.target.value}})} />
                </th>
				<th className='table-elem-sm'>
                    <input value={this.state.filter.partNumber} onChange={event => this.setState({filter: {...this.state.filter, partNumber: event.target.value}})} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.description} onChange={event => this.setState({filter: {...this.state.filter, description: event.target.value}})} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.item} onChange={event => this.setState({filter: {...this.state.filter, item: event.target.value}})} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.wo} onChange={event => this.setState({filter: {...this.state.filter, wo: event.target.value}})} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.woid} onChange={event => this.setState({filter: {...this.state.filter, woid: event.target.value}})} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.qtyBox} type="number" onChange={event => this.setState({filter: {...this.state.filter, qtyBox: event.target.value === "" ? null : parseInt(event.target.value)}})} />
                </th>
                <th></th>
			</tr>
		</thead>
	}

	renderRow(item, ind) {
		return <tr key={"row-" + ind} className={""}>
			<td className='table-elem-sm'>{item.id}</td>
			<td className='table-elem-sm'>{item.cuttingRequest.sequence}</td>
			<td className='table-elem-sm'>{item.partNumber}</td>
			<td className='table-elem-sm'>{item.description}</td>
			<td className='table-elem-sm'>{item.item}</td>
			<td className='table-elem-sm'>{item.wo}</td>
			<td className='table-elem-sm'>{item.woid}</td>
            <td className='table-elem-sm'>{item.qtyBox}</td>
			<td className='table-elem-sm'  style={{ padding: "8", width: 50 }}>
				<div className='d-flex' style={{ margin: "auto 0" }}>
					<button type="button" className='btn btn btn-outline-dark btn-sm mr-1'
						onClick={() => {}} style={{ fontSize: 12, padding: "3 6" }}>
						<FontAwesomeIcon icon={faPenAlt} />
					</button>
					<button type="button" className='btn btn btn-outline-dark btn-sm' style={{ fontSize: 12, padding: "3 6" }}>
						<FontAwesomeIcon icon={faTimes} />
					</button>
				</div>
			</td>
		</tr>
	}

	render() {
		return (
			<div>
				<h1 className='text-center' style={{margin: "10 0 8"}}>Demande de coupe</h1>
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
					<button className='btn btn-danger' onClick={() => {
						this.getData(this.state.date, this.state.shift)
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
											(this.state.filter.id == null || elem.id.toUpperCase().startsWith(this.state.filter.id.toUpperCase())) &&
											(this.state.filter.sequence == null || elem.cuttingRequest.sequence.toUpperCase().startsWith(this.state.filter.sequence.toUpperCase())) &&
											(this.state.filter.qtyBox == null || elem.qtyBox === (this.state.filter.qtyBox)) &&
											(this.state.filter.partNumber == null || elem.partNumber.toString().startsWith(this.state.filter.partNumber)) && 
											(this.state.filter.description == null || elem.description.toString().includes(this.state.filter.description)) && 
											(this.state.filter.item == null || elem.item.toString().startsWith(this.state.filter.item)) && 
											(this.state.filter.wo == null || elem.wo.toString().startsWith(this.state.filter.wo)) && 
											(this.state.filter.woid == null || elem.woid.toString().startsWith(this.state.filter.woid))
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
