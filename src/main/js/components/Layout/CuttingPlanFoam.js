import { faCheck, faDownload, faFilter, faFilterCircleXmark, faGear, faMagnifyingGlass, faPenAlt, faPlus, faRefresh, faRotate, faSearch, faSpinner, faTimes, faUpload } from '@fortawesome/free-solid-svg-icons'
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
import Switch from "react-switch";
import { connect } from 'react-redux';
import PropTypes from 'prop-types';


class CuttingPlanFoam extends Component {

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
			size: 100,
			entriesList: [],
			optionsList: {},
			error: null,
			modalObj: null,
			modalFilter: {},
			showModalFilter: false,
			filter: {},
			showFilter: false,
			filterArr: [{ type: filterOptions[0].value, fieldName: 'foam', value: 'true' }],
			filterFields: [],
			showAdjusterModal: false,
			adjusterModalArr: [],
		}
		this.inputArr = []
	}

	componentDidMount() {
		const { user } = this.props.security;
		// Check if user has ROLE_CAD_FOAM role
		if (!user.roles.map(e => e.authority).includes("ROLE_CAD_FOAM")) {
			this.props.history.push("/");
			return;
		}
		
		if (this.props.match.params.entityId != null) {
			axios.get(`/api/cuttingPlan/${this.props.match.params.entityId}`)
				.then(res => {
					// Only allow editing foam plans
					if (res.data.foam !== true) {
						alert("Vous n'avez pas la permission de modifier ce plan de coupe.");
						this.props.history.push("/cuttingPlanFoam");
						return;
					}
					this.setState({ modalObj: res.data })
				})
		} else if (this.props.history.location.pathname === "/cuttingPlanFoam/new") {
			this.setState({ modalObj: { foam: true } })
		} else {
			this.searchSubEntriesPage();
		}
		this.setState({
			filterFields: metadata["cuttingPlan"].fieldsFilter.map(e => {
				if (e.formDisplayProperty) {
					return { label: e.displayName, value: e.name + "." + e.formDisplayProperty }
				}
				return { label: e.displayName, value: e.name }
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

	componentDidUpdate(prevProps, prevState, snapshot) {
		if ((prevState.sortProp !== this.state.sortProp)
			|| (prevState.sortDirection !== this.state.sortDirection)
			|| (prevState.page !== this.state.page)
			|| (prevState.size !== this.state.size)
			|| (prevProps.match.params.entityId !== this.props.match.params.entityId)
		) {
			if (this.props.match.params.entityId != null) {
				axios.get(`/api/cuttingPlan/${this.props.match.params.entityId}`)
					.then(res => {
						if (res.data.foam !== true) {
							alert("Vous n'avez pas la permission de modifier ce plan de coupe.");
							this.props.history.push("/cuttingPlanFoam");
							return;
						}
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
			// Always filter by foam=true for CAD FOAM role
			let dependencies = "equal.foam.0=true&";
			let additionalFilters = this.state.filterArr
				.filter(e => e.type && e.fieldName && e.value && e.fieldName !== 'foam')
				.map((e, ind) => e.type + "." + e.fieldName + "." + (ind + 1) + "=" + e.value).join("&");
			if (additionalFilters.length > 0) {
				dependencies += additionalFilters + "&";
			}
			
			axios.get(`/api/cuttingPlan/all?${dependencies}page=${this.state.page}&size=${this.state.size}&sort=${this.state.sortProp || metadata["cuttingPlan"].fields[0].name},${this.state.sortDirection}`)
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
					if (err.response && err.response.data != null && err.response.data.username === "Invalid Username") {
						window.location.pathname = "/login";
					}
				})
		} catch (error) {
			console.log({ error })
		}
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
				<th></th>
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
		return <tr key={"row-" + ind} className='table-row-selective' 
		style={(item.alertMessages && item.alertMessages.length > 0) ? {backgroundColor : "#ffff84"} : {}}
		onDoubleClick={() => {
			window.open(`/cuttingPlanFoam/${item[metadata[entity].fields[0].name]}`, '_blank').focus();
		}}>
			<td className='switch-form-td'>
				{item.loadingEnabled ?
					<FontAwesomeIcon icon={faSpinner} spin style={{ fontSize: 20 }} />
					: <input
						type="checkbox"
						style={{
							width: 20,
							height: 20,
						}}
						checked={item.enabled || false}
						onChange={(e) => {
							if (window.confirm("Voulez-vous " + (e.target.checked ? "activer" : "désactiver") + " le plan de coupe " + item.description + " ?")) {
								this.setState({ entriesList: this.state.entriesList.map(e => e.id === item.id ? { ...e, loadingEnabled: true } : e) })
								if (e.target.checked) {
									axios.post(`/api/${entity}/enable/${item.id}`)
										.then((res) => {
											this.setState({ entriesList: this.state.entriesList.map(e => e.id === item.id ? { ...e, enabled: true, loadingEnabled: false } : e) })
										})
										.catch(err => {
											this.setState({
												entriesList: this.state.entriesList.map(e => e.id === item.id ? { ...e, loadingEnabled: false } : e),
												error: err.response.data
											})
										})
								} else {
									axios.post(`/api/${entity}/disable/${item.id}`)
										.then((res) => {
											this.setState({ entriesList: this.state.entriesList.map(e => e.id === item.id ? { ...e, enabled: false, loadingEnabled: false } : e) })
										})
										.catch(err => {
											this.setState({ entriesList: this.state.entriesList.map(e => e.id === item.id ? { ...e, loadingEnabled: false } : e) })
										})
								}
							}
						}}
					/>}
			</td>
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
			<td style={{ width: 50 }} >
				<div className='d-flex' style={{ margin: "auto 0" }}>
					<button type="button" className='btn btn btn-outline-dark btn-sm mr-1'
						onClick={() => { this.props.history.push(`/cuttingPlanFoam/${item[metadata[entity].fields[0].name]}`) }} style={{ fontSize: 12, padding: "3 6" }}>
						<FontAwesomeIcon icon={faPenAlt} />
					</button>
				</div>
			</td>
		</tr>
	}

	renderFilter = () => {
		let arr = this.state.filterArr.filter(e => e.fieldName !== 'foam');

		return <div className='p-2 border' style={{ backgroundColor: "#fbfbfb" }}>
			{arr.map((filter, ind) => (
				<div className='d-flex mb-1' key={ind}>
					<Select classNamePrefix="rs"
						placeholder={"Type..."} style={{ width: 100 }}
						isClearable={true}
						value={filter.type ? { label: filter.type, value: filter.type } : null}
						options={filterOptions}
						onChange={(option) => {
							let filterArr = [...this.state.filterArr];
							let realInd = this.state.filterArr.findIndex(e => e === filter);
							filterArr[realInd].type = option ? option.value : null;
							this.setState({ filterArr });
						}}
					/>
					<Select classNamePrefix="rs" className='ml-1'
						placeholder={"Champs..."} style={{ width: 100 }}
						isClearable={true}
						value={filter.fieldName ? { label: filter.fieldName, value: filter.fieldName } : null}
						options={this.state.filterFields}
						onChange={(option) => {
							let filterArr = [...this.state.filterArr];
							let realInd = this.state.filterArr.findIndex(e => e === filter);
							filterArr[realInd].fieldName = option ? option.value : null;
							this.setState({ filterArr });
						}}
					/>
					<input className='form-control ml-1' placeholder='Valeur...'
						value={filter.value}
						onChange={(e) => {
							let filterArr = [...this.state.filterArr];
							let realInd = this.state.filterArr.findIndex(f => f === filter);
							filterArr[realInd].value = e.target.value;
							this.setState({ filterArr });
						}}
						style={{ maxWidth: 200 }}
						onKeyUp={(e) => {
							if (e.key === 'Enter') {
								this.searchSubEntriesPage();
							}
						}}
					/>
					<button className='btn btn-outline-danger ml-1' onClick={() => {
						let filterArr = [...this.state.filterArr];
						let realInd = this.state.filterArr.findIndex(f => f === filter);
						filterArr.splice(realInd, 1);
						this.setState({ filterArr });
					}}>
						<FontAwesomeIcon icon={faTimes} />
					</button>
				</div>
			))}
			<button className='btn btn-outline-primary' onClick={() => {
				let filterArr = [...this.state.filterArr, { type: filterOptions[0].value }];
				this.setState({ filterArr });
			}}>
				<FontAwesomeIcon icon={faPlus} />
			</button>
			<button className='btn btn-primary ml-1' onClick={() => this.searchSubEntriesPage()}>
				<FontAwesomeIcon icon={faSearch} />
			</button>
		</div>
	}

	render() {
		let entity = "cuttingPlan";
		return (
			<div className='container-fluid'>
				<div className='d-flex align-items-center my-2'>
					<h3 className='mr-auto'>Plan de coupe (Foam)</h3>
					<button className='btn btn-outline-primary mr-1' onClick={() => this.setState({ showFilter: !this.state.showFilter })}>
						<FontAwesomeIcon icon={this.state.showFilter ? faFilterCircleXmark : faFilter} />
					</button>
					<button className='btn btn-primary mr-1' onClick={() => this.searchSubEntriesPage()}>
						<FontAwesomeIcon icon={faRefresh} />
					</button>
					<button className='btn btn-success' onClick={() => {
						this.props.history.push("/cuttingPlanFoam/new")
					}}>
						<FontAwesomeIcon icon={faPlus} /> Nouveau
					</button>
				</div>
				{this.state.showFilter && this.renderFilter()}
				{this.state.error && <div className='alert alert-danger'>{JSON.stringify(this.state.error)}</div>}
				<div className='table-responsive entity-table mb-2' style={{ maxHeight: `calc(100vh - 170px)` }}>
					<table className='table table-striped table-bordered table-elements-sm m-0'>
						{this.renderHeader(entity)}
						<tbody>
							{this.state.entriesList === null ? <tr><td colSpan={100}>Chargement...</td></tr> :
								this.state.entriesList.length === 0 ? <tr><td colSpan={100}>Aucun plan de coupe foam trouvé</td></tr> :
									this.state.entriesList.map((item, ind) => this.renderRow(entity, item, ind))}
						</tbody>
					</table>
				</div>
				<AdvancedPagination
					page={this.state.page}
					totalPages={this.state.entriesPage.totalPages}
					onPageChange={(page) => this.setState({ page })}
				/>
			</div>
		)
	}
}

CuttingPlanFoam.propTypes = {
	security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
	security: state.security
})

export default connect(mapStateToProps, {})(CuttingPlanFoam);
