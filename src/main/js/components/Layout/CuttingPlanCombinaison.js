import { faCheck, faDownload, faFilter, faFilterCircleXmark, faGear, faMagnifyingGlass, faPenAlt, faPlus, faRotate, faSearch, faTimes } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import axios from 'axios';
import React, { Component } from 'react'
import AdvancedPagination from "../utils/AdvancedPagination";
import SortIcon from "../utils/SortIcon";
import { filterOptions, metadata } from '../../metadata';
import { Modal } from 'react-bootstrap';
import CuttingPlanForm from './CuttingPlanForm';
import Select from "react-select";
import "../../styles/CuttingPlan.scss"
import CuttingPlanCombinaisonForm from './CuttingPlanCombinaisonForm';

export default class CuttingPlanCombinaison extends Component {
    constructor() {
		super()
		this.state = {
			entriesPage: {
				totalElements: 0,
				totalPages: 1,
				numberOfElements: 0
			},
			sortProp: "createdAt",
			sortDirection: "desc",
			page: 0,
			size: 20,
			entriesList: [],
			optionsList: {},
			error: null,
			modalObj: null,
			modalFilter: {},
			showModalFilter: false,
			filter: {},
			showFilter: false,
			filterArr: [{ type: filterOptions[0].value }],
			filterFields: [],
			showAdjusterModal: false,
			adjusterModalArr: [],
		}
		this.inputArr = []
	}

	componentDidMount() {
		if (this.props.match.params.entityId != null) {
			axios.get(`/api/cuttingPlanCombination/${this.props.match.params.entityId}`)
				.then(res => {
					this.setState({ modalObj: res.data })
				})
		} else if (this.props.history.location.pathname === "/cuttingPlanCombination/new") {
			this.setState({ modalObj: {} })
		} else {
			this.searchSubEntriesPage();
		}
		this.setState({
			filterFields: metadata["cuttingPlanCombination"].fieldsFilter.map(e => {
				if (e.formDisplayProperty) {
					return { label: e.displayName, value: e.name + "." + e.formDisplayProperty }
				}
				return { label: e.displayName, value: e.name }
			})
		})
	}

	componentDidUpdate(prevProps, prevState, snapshot) {
		if ((prevState.sortProp !== this.state.sortProp)
			|| (prevState.sortDirection !== this.state.sortDirection)
			|| (prevState.page !== this.state.page)
			|| (prevState.size !== this.state.size)
			|| (prevProps.match.params.entityId !== this.props.match.params.entityId)
		) {
			if (this.props.match.params.entityId != null) {
				axios.get(`/api/cuttingPlanCombination/${this.props.match.params.entityId}`)
					.then(res => {
						this.setState({ modalObj: res.data })
					})
			} else {
				this.searchSubEntriesPage();
			}
		}
	}

	searchSubEntriesPage = () => {
		try {
			this.setState({
				entriesPage: {
					totalElements: 0,
					totalPages: 1,
					numberOfElements: 0
				},
				entriesList: null,
				modalObj: null
			})
			let dependencies = "";
			// for (let k in this.state.modalFilter) {
			// 	if (this.state.modalFilter.hasOwnProperty(k) && this.state.modalFilter[k] && this.state.modalFilter[k] !== "") {
			// 		dependencies += k + "=" + this.state.modalFilter[k] + "&"
			// 	}
			// }
			dependencies = this.state.filterArr.filter(e => e.type && e.fieldName && e.value).map(e => e.type + "." + e.fieldName + "=" + e.value).join("&")
			if (dependencies.length > 0) {
				dependencies += "&"
			}
			axios.get(`/api/cuttingPlanCombination/all?${dependencies}page=${this.state.page}&size=${this.state.size}&sort=${this.state.sortProp || metadata["cuttingPlanCombination"].fields[0].name},${this.state.sortDirection}`)
				.then((res) => {
					this.setState({
						entriesList: res.data.content ? res.data.content : res.data.sort((a, b) => b.id - a.id),
						entriesPage: {
							totalElements: res.data.totalElements,
							totalPages: res.data.totalPages,
							numberOfElements: res.data.numberOfElements
						}
					})
				})
				.catch(err => {
					if (err.response.data != null && err.response.data.username === "Invalid Username") {
						window.location.pathname = "/login";
					}
				})
		} catch (error) {
			console.log({ error })
		}
	}

	renderErrorsAlert(errors) {
		let arr = []
		for (let prop in errors) {
			arr.push(<li>{prop}: {errors[prop]}</li>)
		}
		return arr
	}

	sortChanged(field) {
		let sortProp = field;
		let propChanged = this.state.sortProp !== sortProp;
		let sortDirection = propChanged ? 'asc' : this.state.sortDirection === 'asc' ? 'desc' : 'asc';

		this.setState({ sortProp, sortDirection });
	}

	renderHeader = (entity) => {
		return <thead className='entity-table-header'>
			<tr>
				{metadata[entity].fields
					.filter(elem => elem.hideTable !== true && !this.state.adjusterModalArr.includes(elem.name))
					.map(field => {
						return <th onClick={() => this.sortChanged(field.name)}>{field.displayName}<SortIcon currentSort={field.name} sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
					})}
				<th></th>
			</tr>
		</thead>
	}

	renderRow = (entity, item, ind) => {
		return <tr key={"row-" + ind} className='table-row-selective' onDoubleClick={() => {
			// this.props.history.push(`/${entity}/${item[metadata[entity].fields[0].name]}`)
			window.open(`/${entity}/${item[metadata[entity].fields[0].name]}`, '_blank').focus();
		}}>
			{metadata[entity].fields
				.filter(elem => elem.hideTable !== true && !this.state.adjusterModalArr.includes(elem.name))
				.map(field => {
					switch (field.type) {
						case "boolean":
							return (item[field.name] === true ? <td><FontAwesomeIcon icon={faCheck} color="green" /></td> : item[field.name] === false ? <td><FontAwesomeIcon icon={faTimes} color="red" /></td> : <td></td>)
						case "object":
							return <td>{item[field.name] ? item[field.name][field.formDisplayProperty] : ""}</td>
						default:
							return <td style={{ whiteSpace: "nowrap" }}>{item[field.name]}</td>
					}
				})}
			<td style={{ padding: "8", width: 50 }}>
				<div className='d-flex' style={{ margin: "auto 0" }}>
					<button type="button" className='btn btn btn-outline-dark btn-sm mr-1'
						onClick={() => { this.props.history.push(`/${entity}/${item[metadata[entity].fields[0].name]}`) }} style={{ fontSize: 12, padding: "3 6" }}>
						<FontAwesomeIcon icon={faPenAlt} />
					</button>
					<button type="button" className='btn btn btn-outline-dark btn-sm' style={{ fontSize: 12, padding: "3 6" }}
						onClick={() => {
							if (window.confirm("voulez vous supprimer cette ligne ?")) {
								axios.post(`/api/${entity}/delete`, item)
									.then((res) => {
										this.searchSubEntriesPage()
									})
									.catch(err => {

									})
							}
						}}
					>
						<FontAwesomeIcon icon={faTimes} />
					</button>
				</div>
			</td>
		</tr>
	}

	renderOptionPopup = () => {
		return <div>
			hi
		</div>
	}

	renderField(field, ind) {
		// const { entity, entityId } = this.props
		switch (field.type) {
			case "text":
				return <input className='form-control entityform-input' value={this.state.modalFilter[field.name]}
					ref={input => this.inputArr[ind] = input}
					onChange={(event) => {
						if (field.reg == null || field.reg.test(event.target.value)) {
							this.setState({ modalFilter: { ...this.state.modalFilter, [field.name]: event.target.value } })
						}
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							this.inputArr[ind + 1].focus()
						}
					}}
				/>
			default: return <div></div>
		}
	}

	renderFilter = () => {
		let arr = this.state.filterArr;

		return <div className='filter-container' style={this.state.showFilter ? { maxHeight: (33 * arr.length + 40) } : { maxHeight: 0, overflow: "hidden" }}>
			<div className='filter-list'>
				{arr.map((filter, ind) => <div className='d-flex align-items-center mb-1' key={"filter-" + ind}>
					<Select classNamePrefix="rs"
						placeholder={"Colonne..."} className='col-2 p-0  ml-2'
						isClearable={false}
						value={(filter.fieldName)
							? this.state.filterFields.find(e => e.name === filter.fieldName)
							: null
						}
						options={this.state.filterFields}
						onChange={(option) => {
							arr[ind].fieldName = option.value
							this.setState({ filterArr: [...arr] })
						}}
					/>
					<Select classNamePrefix="rs"
						placeholder={""} className='col-2 p-0 ml-2 '
						isClearable={false}
						value={(filter.type)
							? filterOptions.find(e => e.value === filter.type)
							: null
						}
						options={filterOptions}
						onChange={(option) => {
							arr[ind].type = option.value
							this.setState({ filterArr: [...arr] })
						}}
					/>
					<input className='ml-2 filter-input'
						value={filter.value}
						onChange={e => {
							arr[ind].value = e.target.value
							this.setState({ filterArr: [...arr] })
						}}
					/>
					<button className='btn btn-outline-primary ml-2' onClick={() => {
						// add an empty object in arr in position ind+1
						arr.splice(ind + 1, 0, { type: filterOptions[0].value })
						this.setState({ filterArr: [...arr] })
					}}><FontAwesomeIcon icon={faPlus} /></button>
					<button className='btn btn-outline-danger ml-2' disabled={arr.length < 2} onClick={() => {
						// remove object in arr in position ind
						arr.splice(ind, 1)
						this.setState({ filterArr: [...arr] })
					}}><FontAwesomeIcon icon={faTimes} /></button>
					{ind === 0 && <button className='btn btn-outline-success ml-2 btn-sm' onClick={() => {
						this.searchSubEntriesPage()
					}}><FontAwesomeIcon icon={faSearch} /> Chercher</button>}
					{ind === 0 && <button className='btn btn-outline-secondary ml-2 btn-sm' onClick={() => {
						this.setState({ filterArr: [{ type: filterOptions[0].value }] })
						this.searchSubEntriesPage()
					}}><FontAwesomeIcon icon={faTimes} /> Reset</button>}
				</div>)}
			</div>
		</div>
	}

	renderAdjuterModal = (entity) => {
		let arr = []
		let fields = metadata[entity].fields.filter(elem => elem.hideTable !== true)
		for (let i = 0; i < fields.length; i++) {
			arr.push(<div style={{width: "33.33%", paddingLeft: "20"}} key={"ajuster-" + i}>
				<input id={fields[i].name} type='checkbox' checked={!this.state.adjusterModalArr.includes(fields[i].name)}
					onChange={(e) => {
						if (!e.target.checked) {
							this.setState({ adjusterModalArr: [...this.state.adjusterModalArr, fields[i].name] })
						} else {
							this.setState({ adjusterModalArr: this.state.adjusterModalArr.filter(e => e !== fields[i].name) })
						}
					}}
				/>
				<label className='mb-0 ml-2' for={fields[i].name}>{fields[i].displayName}</label>
			</div>)
		}
		return <Modal
			show={this.state.showAdjusterModal}
			onHide={() => this.setState({ showAdjusterModal: false })}
			dialogClassName="modal-75w"
			centered
		>
			{this.state.showAdjusterModal && <div style={{ maxHeight: "80vh", overflowY: 'auto' }}>
				<h4 className='text-center'>Ajuster les colonnes</h4>
				<div style={{display: "flex", flexWrap: "wrap", position: "relative"}}>{arr}</div>
			</div>}
		</Modal>
	}


	render() {
		console.log(this.props)
		let entity = "cuttingPlanCombination"
		let { entityId } = this.props.match.params
		if (metadata[entity] == null) {
			return <div>
				<h1 >Entity {entity} not found</h1>
			</div>
		}
		if (entity) {
			if (entityId) {
				return <CuttingPlanCombinaisonForm entityId={entityId} goBack={() => {
					this.props.history.push(`/cuttingPlanCombination`)
					this.searchSubEntriesPage()
				}} />

			} else if (this.state.modalObj != null) {
				return <CuttingPlanCombinaisonForm goBack={() => {
					this.props.history.push(`/cuttingPlanCombination`)
					this.setState({ modalObj: null }); this.searchSubEntriesPage()
				}} />
			}
		}

		return (
			<div className='cuttingPlan-container'>
				<h1 className='text-center' style={{ marginTop: 10 }}>{metadata[entity].displayName}</h1>
				<div className='d-flex align-items-center mb-2 mr-2'>
					{metadata[entity].operation.includes("Add") && <button type="button" className='btn btn-outline-danger btn-sm ml-2' onClick={() => {
						//  this.setState({ modalObj: {} }) 
						window.open(`/${entity}/new`, '_blank').focus();
					}}>
						<FontAwesomeIcon icon={faPlus} /> Ajouter
					</button>}
					{metadata[entity].operation.includes("Export") && <button type="button" className='btn btn-outline-danger btn-sm ml-2' onClick={() => {
						//  this.setState({ modalObj: {} }) 
						window.open(`/${entity}/new`, '_blank').focus();
					}}>
						<FontAwesomeIcon icon={faDownload} /> Export
					</button>}
					<div style={{ flex: 1 }}></div>
					<button type="button" className='btn btn-outline-danger btn-sm mr-2' onClick={() => {
						this.setState({ showAdjusterModal: !this.state.showAdjusterModal })
					}}>
						<FontAwesomeIcon icon={faGear} /> Ajuster
					</button>
					<button type="button" className='btn btn-outline-danger btn-sm' onClick={() => {
						this.setState({ showFilter: !this.state.showFilter })
					}}>
						<FontAwesomeIcon icon={faFilter} /> Filtre
					</button>
				</div>
				{this.renderFilter()}

				<div className='px-2 mb-2'>
					<div className='table-responsive entity-table mb-2'
						style={this.state.showFilter ? { maxHeight: `calc(100vh - ${(33 * Math.min(this.state.filterArr.length, 10) + 250)}px)` } : { maxHeight: `calc(100vh - 213px)` }}
					>
						<table className='table table-bordered m-0'>
							{this.renderHeader(entity)}
							<tbody>
								{this.state.entriesList == null ? <tr ><td colSpan={100}>Chargement...</td></tr> :
									this.state.entriesList.length === 0
										? <tr><td colSpan={100}>Aucune données disponibles</td> </tr>
										: this.state.entriesList
											.map((item, ind) => {
												return this.renderRow(entity, item, ind)
											})}
							</tbody>
						</table>

					</div>
					<div className='d-flex'>
						<div className='mr-2'>
							<small>{this.state.entriesPage.totalElements} entrées trouvées</small>
						</div>
						<div style={{ flex: 1 }}></div>
						<select
							onChange={(option) => this.setState({ size: parseInt(option.target.value) })}
							value={this.state.size}
							className="mr-2"
							style={{ fontSize: 14, padding: 4 }}
						>
							<option value="10">10</option>
							<option value="20">20</option>
							<option value="50">50</option>
							<option value="100">100</option>
						</select>
						<div className='m-0'>
							<AdvancedPagination
								ref={this.paginationRef}
								pageCount={this.state.entriesPage.totalPages}
								onPageChange={(page) => this.setState({ page })}
							/>
						</div>
					</div>
				</div>
				{this.renderAdjuterModal(entity)}
			</div>
		)
	}
}
