import { faCheck, faFilter, faFilterCircleXmark, faMagnifyingGlass, faPenAlt, faPlus, faRotate, faTimes } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import axios from 'axios';
import React, { Component } from 'react'
import { connect } from 'react-redux'
import AdvancedPagination from "../utils/AdvancedPagination";
import SortIcon from "../utils/SortIcon";
import { metadata } from '../../metadata';
import PartNumberMaterialConfigForm from './PartNumberMaterialConfigForm';
import { Modal } from 'react-bootstrap';

class PartNumberMaterialConfig extends Component {

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
		}
		this.inputArr = []
	}

	componentDidMount() {
		if (this.props.match.params.entityId != null) {
			axios.get(`/api/partNumberMaterialConfig/${this.props.match.params.entityId}`)
				.then(res => {
					this.setState({ modalObj: res.data })
				})
		} else if(this.props.history.location.pathname === "/partNumberMaterialConfig/new") {
			this.setState({ modalObj: {} })
		} else {
			this.searchSubEntriesPage();
		}
	}

	componentDidUpdate(prevProps, prevState, snapshot) {
		if ((prevState.sortProp !== this.state.sortProp)
			|| (prevState.sortDirection !== this.state.sortDirection)
			|| (prevState.page !== this.state.page)
			|| (prevState.size !== this.state.size)
			|| (prevProps.match.params.entityId !== this.props.match.params.entityId)
		) {
			if (this.props.match.params.entityId != null) {
				axios.get(`/api/partNumberMaterialConfig/${this.props.match.params.entityId}`)
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
			for (let k in this.state.modalFilter) {
				if (this.state.modalFilter.hasOwnProperty(k) && this.state.modalFilter[k] && this.state.modalFilter[k] !== "") {
					dependencies += k + "=" + this.state.modalFilter[k] + "&"  
				}
			}
			axios.get(`/api/partNumberMaterialConfig/all?${dependencies}page=${this.state.page}&size=${this.state.size}&sort=${this.state.sortProp || metadata["partNumberMaterialConfig"].fields[0].name},${this.state.sortDirection}`)
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

	getRoles = () => {
		const roles = (this.props.security && this.props.security.user && this.props.security.user.roles) || []
		return roles.map(r => r.authority)
	}

	isCadFoamOnly = () => {
		const auths = this.getRoles()
		return auths.includes("ROLE_CAD_FOAM") && !auths.includes("ROLE_CAD")
	}

	isDeleteBlocked = (item) => {
		return this.isCadFoamOnly() && item && item.fipDev !== true
	}

	renderHeader = (entity) => {
		return <thead className='entity-table-header'>
			<tr>
				{metadata[entity].fields.map(field => {
					return <th onClick={() => this.sortChanged(field.name)}>{field.displayName}<SortIcon currentSort={field.name} sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
				})}
				<th style={{ padding: 5 , display:"flex"}}>
					{metadata[entity].fieldsFilter && metadata[entity].fieldsFilter.length > 0 && <button
						type="button" className='btn btn-outline-light mr-1'
						onClick={() => { this.setState({ showModalFilter: true }) }}
					>
						<FontAwesomeIcon icon={faFilter} size="sm" />
					</button>}
					{Object.keys(this.state.modalFilter).length > 0 && <button type="button" className='btn btn-outline-light'
						onClick={() => { 
							this.setState({ modalFilter: {} }) 
							setTimeout(() => {this.searchSubEntriesPage()},10);
						}}
					>
						<FontAwesomeIcon icon={faFilterCircleXmark}/>
					</button>}
				</th>
			</tr>
		</thead>
	}

	renderRow = (entity, item, ind) => {
		return <tr key={"row-" + ind}>
			{metadata[entity].fields.map(field => {
				switch (field.type) {
					case "object":
						return <td>{item[field.name] ? item[field.name][field.formDisplayProperty] : ""}</td>
					default:
						return <td>{item[field.name]}</td>
				}
			})}
			<td style={{ padding: "8", width: 50 }}>
				<div className='d-flex' style={{ margin: "auto 0" }}>
					<button type="button" className='btn btn btn-outline-dark btn-sm mr-1'
						onClick={() => {
							this.props.history.push(`/${entity}/${item[metadata[entity].fields[0].name]}`)
							window.open(`/${entity}/${item[metadata[entity].fields[0].name]}`, '_blank').focus();
						}} style={{ fontSize: 12, padding: "3 6" }}>
						<FontAwesomeIcon icon={faPenAlt} />
					</button>
					<button type="button" className='btn btn btn-outline-dark btn-sm' style={{ fontSize: 12, padding: "3 6" }}
						disabled={this.isDeleteBlocked(item)}
						title={this.isDeleteBlocked(item) ? "FIP dev non activé : suppression interdite pour CAD FOAM" : ""}
						onClick={() => {
							if (this.isDeleteBlocked(item)) return
							if (window.confirm("voulez vous supprimer cette ligne ?")) {
								axios.post(`/api/${entity}/delete`, item)
									.then((res) => {
										this.searchSubEntriesPage()
									})
									.catch(err => {
										alert('Erreur: ' + (err.response?.data || err.message))
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


	renderFilter = (entity) => {
		return <Modal
			show={this.state.showModalFilter}
			onHide={() => this.setState({ showModalFilter: false })}
			// className=""
			dialogClassName="modal-75w"
			centered
		>
			<div style={{
				maxHeight: "80vh",
				overflow: "auto"
			}}>
				<h2 className='text-center'>{metadata[entity].displayName} filter</h2>
				{metadata[entity].fieldsFilter.map((field, ind) => {
					return <div className='d-flex px-2 mb-2'>
						<label className='text-right col-form-label pr-2' style={{ whiteSpace: "nowrap", width: "20%" }}>{field.displayName} :</label>
						{this.renderField(field, ind)}
					</div>
				})}
				
				<div style={{ display: "flex", flexDirection: 'row-reverse' }} className="px-2 mb-2">
					<button type="button" className='btn btn-primary ml-2' ref={btn => this.inputArr[metadata[entity].fieldsFilter.length] = btn}
						onClick={() => {
							this.setState({showModalFilter: false})
							this.searchSubEntriesPage()
						}}
					>
						<FontAwesomeIcon icon={faMagnifyingGlass} /> Chercher
					</button>
					<button type="button" className='btn btn-danger ml-2' ref={btn => this.inputArr[metadata[entity].fieldsFilter.length] = btn}
						onClick={() => {
							this.setState({showModalFilter: false, modalFilter: {}})
							setTimeout(() => {this.searchSubEntriesPage()},10);
						}}
					>
						<FontAwesomeIcon icon={faTimes} /> Annuler
					</button>
					<button className='btn btn-link' onClick={() => { this.setState({ showModalFilter: false }) }}>Retour au tableau</button>
				</div>
			</div>
		</Modal>
	}

	render() {
		console.log(this.props)
		let entity = "partNumberMaterialConfig"
		let { entityId } = this.props.match.params
		if (metadata[entity] == null) {
			return <div>
				<h1 >Entity {entity} not found</h1>
			</div>
		}
		if (entity) {
			if (entityId) {
				return <PartNumberMaterialConfigForm entityId={entityId} goBack={() => {
					this.props.history.push(`/partNumberMaterialConfig`)
					this.searchSubEntriesPage()
				}} />

			} else if (this.state.modalObj != null) {
				return <PartNumberMaterialConfigForm goBack={() => { this.setState({ modalObj: null }); this.searchSubEntriesPage() }} />
			}
		}

		return (
			<div>
				<h1 className='text-center' style={{ marginTop: 10 }}>{metadata[entity].displayName}</h1>
				<div className='d-flex align-items-center mb-1 mr-2'>
					{metadata[entity].operation.includes("Add") && <button type="button" className='btn btn-outline-danger mx-2' onClick={() => { 
						// this.setState({ modalObj: {} }) 
						window.open(`/${entity}/new`, '_blank').focus();
						}}>
						<FontAwesomeIcon icon={faPlus} />
					</button>}
					<div style={{ flex: 1 }}></div>
				</div>
				<div className='px-2'>
					<div className='table-responsive entity-table'>
						<table className='table table-bordered'>
							{this.renderHeader(entity)}
							<tbody>
								{this.state.entriesList == null ? <tr><td colSpan={100}>Chargement...</td></tr> :
									this.state.entriesList.length === 0
										? <tr><td colSpan={100}>Aucune données disponibles</td> </tr>
										: this.state.entriesList.map((item, ind) => {
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
				{metadata[entity].fieldsFilter && metadata[entity].fieldsFilter.length > 0 && this.renderFilter(entity)}
			</div>
		)
	}
}

const mapStateToProps = state => ({
	security: state.security
})

export default connect(mapStateToProps, {})(PartNumberMaterialConfig)
