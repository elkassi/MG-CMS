import { faCheck, faPenAlt, faPlus, faRotate, faTimes } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import axios from 'axios';
import React, { Component } from 'react'
import { Modal } from 'react-bootstrap';
import "../../styles/User.scss"
import AdvancedPagination from "../utils/AdvancedPagination";
import SortIcon from "../utils/SortIcon";
import Switch from "react-switch";
import Select from "react-select";
import { departementOption } from '../../metadata';

export default class Users extends Component {

	constructor() {
		super()
		this.state = {
			entriesPage: {
				totalElements: 0,
				totalPages: 1,
				numberOfElements: 0
			},
			sortProp: "created_At",
			sortDirection: "desc",
			page: 0,
			size: 20,
			entriesList: [],
			optionsList: {},
			error: null,
			modalObj: null,

		}
	}

	componentDidMount() {
		this.searchSubEntriesPage()
		axios.get(`/api/role/all`)
			.then(res => {
				this.setState({
					optionsList: {
						...this.state.optionsList,
						role: res.data.map(e => { return { label: e.description, value: e } })
					}
				})
			})
		axios.get(`/api/site/all`)
			.then(res => {
				this.setState({
					optionsList: {
						...this.state.optionsList,
						site: res.data.map(e => { return { label: e.nom, value: e } })
					}
				})
			})
	}
	componentDidUpdate(prevProps, prevState, snapshot) {
		if ((prevState.sortProp !== this.state.sortProp)
			|| (prevState.sortDirection !== this.state.sortDirection)
			|| (prevState.page !== this.state.page)
			|| (prevState.size !== this.state.size)
		) {
			this.searchSubEntriesPage();
		}
	}

	searchSubEntriesPage = () => {
		this.setState({
			entriesPage: {
				totalElements: 0,
				totalPages: 1,
				numberOfElements: 0
			},
			entriesList: null,
			modalObj: null
		})
		axios.get(`/api/user/all?page=${this.state.page}&size=${this.state.size}&sort=${this.state.sortProp},${this.state.sortDirection}`)
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
	}

	sortChanged(field) {
		let sortProp = field;
		let propChanged = this.state.sortProp !== sortProp;
		let sortDirection = propChanged ? 'asc' : this.state.sortDirection === 'asc' ? 'desc' : 'asc';

		this.setState({ sortProp, sortDirection });
	}

	renderErrorsAlert(errors) {
		let arr = []
		for (let prop in errors) {
			arr.push(<li>{prop}: {errors[prop]}</li>)
		}
		return arr
	}

	renderModal() {
		return <Modal
			show={this.state.modalObj != null}
			onHide={() => this.setState({ modalObj: null })}
			// className=""
			// dialogClassName="modal-75w"
			centered
		>
			{this.state.modalObj && <Modal.Body>
				<div className='row my-2'>
					<label className='col-lg-3 col-form-label text-right'>matricule</label>
					<input className='form-control input-sm col-8' value={this.state.modalObj.matricule} disabled={this.state.modalObj.edit === true}
						onChange={(event) => { this.setState({ modalObj: { ...this.state.modalObj, matricule: event.target.value } }) }}
					/>
				</div>
				<div className='row my-2'>
					<label className='col-lg-3 col-form-label text-right'>username</label>
					<input className='form-control input-sm col-8' value={this.state.modalObj.username}
						onChange={(event) => { this.setState({ modalObj: { ...this.state.modalObj, username: event.target.value } }) }}
					/>
				</div>
				<div className='row my-2'>
					<label className='col-lg-3 col-form-label text-right'>firstName</label>
					<input className='form-control input-sm col-8' value={this.state.modalObj.firstName}
						onChange={(event) => { this.setState({ modalObj: { ...this.state.modalObj, firstName: event.target.value } }) }}
					/>
				</div>
				<div className='row my-2'>
					<label className='col-lg-3 col-form-label text-right'>lastName</label>
					<input className='form-control input-sm col-8' value={this.state.modalObj.lastName}
						onChange={(event) => { this.setState({ modalObj: { ...this.state.modalObj, lastName: event.target.value } }) }}
					/>
				</div>
				<div className='row my-2'>
					<label className='col-lg-3 col-form-label text-right'>email</label>
					<input className='form-control input-sm col-8' value={this.state.modalObj.email} type="email"
						onChange={(event) => { this.setState({ modalObj: { ...this.state.modalObj, email: event.target.value } }) }}
					/>
				</div>
				<div className='row my-2'>
					<label className='col-lg-3 col-form-label text-right'>roles</label>
					<div className='col-8 p-0'>
						{this.state.optionsList.role && <Select classNamePrefix="rs"
							isClearable={true} isMulti
							value={this.state.modalObj.roles ? this.state.optionsList.role.filter((elem) => this.state.modalObj.roles.map(e => e.id).includes(elem.value.id)) : []}
							options={this.state.optionsList.role}
							onChange={(options) => this.setState({ modalObj: { ...this.state.modalObj, roles: options.map(option => option.value) } })}
						/>}
					</div>
				</div>
				<div className='row my-2'>
					<label className='col-lg-3 col-form-label text-right'>Site</label>
					<div className='col-8 p-0'>
						<Select id={"site"} name={"site"} classNamePrefix="rs"
							placeholder={"Site"} isClearable={false}
							value={(this.state.optionsList.site && this.state.optionsList.site.length > 0 && this.state.modalObj.site)
								? { label: this.state.modalObj.site.nom, value: this.state.modalObj.site }
								: null
							}
							options={this.state.optionsList.site}
							onChange={(option) => {
								this.setState({ modalObj: { ...this.state.modalObj, site: (option ? option.value : null) } })
							}}
						/>
					</div>
				</div>

				<div className='row my-2'>
					<label className='col-lg-3 col-form-label text-right'>Departement</label>
					<div className='col-8 p-0'>
						<Select classNamePrefix="rs" id={"departement"} name={"departement"}
							placeholder={"Département..."}
							isClearable={false}
							value={this.state.modalObj["departement"] ? { label: this.state.modalObj["departement"], value: this.state.modalObj["departement"] } : null}
							options={departementOption}
							onChange={(option) => {
								this.setState({ modalObj: { ...this.state.modalObj, ["departement"]: option.value } })
							}}
						/>
					</div>
				</div>
				<div className='row my-2'>
					<label className='col-lg-3 col-form-label text-right'>Chef Direct</label>
					<input className='form-control input-sm col-8' value={this.state.modalObj.chefDirect} type="chefDirect"
						onChange={(event) => { this.setState({ modalObj: { ...this.state.modalObj, chefDirect: event.target.value, } }) }}
					/>
				</div>

				<div className='row my-2'>
					<label className='col-lg-3 col-form-label text-right'>fonction</label>
					<input className='form-control input-sm col-8' value={this.state.modalObj.fonction}
						onChange={(event) => { this.setState({ modalObj: { ...this.state.modalObj, fonction: event.target.value } }) }}
					/>
				</div>

				<div className='row my-2'>
					<label className='col-lg-3 col-form-label text-right'>IP Imprimante</label>
					<input className='form-control input-sm col-8' value={this.state.modalObj.ipPrinter}
						onChange={(event) => { this.setState({ modalObj: { ...this.state.modalObj, ipPrinter: event.target.value } }) }}
					/>
				</div>

				<div className='row my-2'>
					<label className='col-lg-3 col-form-label text-right'>active</label>
					<Switch checked={this.state.modalObj.active != null ? this.state.modalObj.active : true}
						className="react-switch mt-1" offColor="#F00"
						onChange={(checked) => {
							console.log({ checked })
							this.setState({ modalObj: { ...this.state.modalObj, active: checked } })
						}}
					/>
				</div>
				{(this.state.error && Object.keys(this.state.error).length !== 0) && <div className="alert alert-danger text-center m-4" role="alert">
					<ul>{this.renderErrorsAlert(this.state.error)}</ul>
				</div>}
			</Modal.Body>}
			<Modal.Footer>
				<button className='btn btn-link' onClick={() => { this.setState({ modalObj: null }) }}>annuler</button>
				<button className='btn btn-success' onClick={() => {
					axios.post(`/api/user`, this.state.modalObj)
						.then((res) => {
							if (this.state.modalObj.matricule == null) {
								alert("Mot de passe : " + res.data)
							}
							this.setState({ modalObj: null })
							this.searchSubEntriesPage();
						})
						.catch(err => {
							if (typeof err.response.data === 'string') {
								this.setState({
									error: {
										...this.state.error,
										errorMessage: err.response.data
									}
								})
							} else {
								this.setState({
									error: err.response.data
								})
							}
						})
				}}>Enregistrer</button>
			</Modal.Footer>
		</Modal>
	}

	render() {
		return (
			<div>
				<h1 className='text-center' style={{ marginTop: 10 }}>Users</h1>
				<div className='d-flex align-items-center mb-1 mr-2'>
					<button type="button" className='btn btn-outline-danger mx-2' onClick={() => { this.setState({ modalObj: {} }) }}>
						<FontAwesomeIcon icon={faPlus} />
					</button>
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
						<option value="1000">1000</option>
						<option value="5000">5000</option>
					</select>


				</div>
				<div className='px-2'>
					<div className='table-responsive user-table'>
						<table className='table table-bordered'>
							<thead className='user-table-header'>
								<tr>

									<th onClick={() => this.sortChanged("matricule")}>matricule  <SortIcon currentSort="matricule" sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
									<th onClick={() => this.sortChanged("username")}>username  <SortIcon currentSort="username" sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
									<th onClick={() => this.sortChanged("firstName")}>firstName  <SortIcon currentSort="firstName" sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
									<th onClick={() => this.sortChanged("lastName")}>lastName  <SortIcon currentSort="lastName" sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
									<th onClick={() => this.sortChanged("email")}>email  <SortIcon currentSort="email" sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
									<th onClick={() => this.sortChanged("roles")}>roles  <SortIcon currentSort="roles" sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
									<th onClick={() => this.sortChanged("departement")}>departement  <SortIcon currentSort="departement" sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
									<th onClick={() => this.sortChanged("chefDirect")}>chefDirect  <SortIcon currentSort="chefDirect" sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
									<th onClick={() => this.sortChanged("fonction")}>fonction  <SortIcon currentSort="fonction" sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
									<th onClick={() => this.sortChanged("ipPrinter")}>IP Imprimante  <SortIcon currentSort="ipPrinter" sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
									<th onClick={() => this.sortChanged("active")}>active  <SortIcon currentSort="active" sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
									<th onClick={() => this.sortChanged("created_At")}>created_At  <SortIcon currentSort="created_At" sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
									<th onClick={() => this.sortChanged("updated_At")}>updated_At  <SortIcon currentSort="updated_At" sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="float-right" /></th>
									<th></th>
								</tr>
							</thead>
							<tbody>
								{this.state.entriesList == null ? <tr><td colSpan={100}>Chargement...</td></tr> :
									this.state.entriesList.length === 0
										? <tr><td colSpan={100}>Aucune données disponibles</td> </tr>
										: this.state.entriesList.map((item, ind) => {
											return <tr key={"row-" + ind}>
												<td>{item.matricule}</td>
												<td>{item.username}</td>
												<td>{item.firstName}</td>
												<td>{item.lastName}</td>
												<td>{item.email}</td>
												<td><pre style={{ margin: 0 }}>{item.roles.map(role => role.description).join("\n")}</pre></td>
												<td>{item.departement}</td>
												<td>{item.chefDirect}</td>
												<td>{item.fonction}</td>
												<td>{item.ipPrinter}</td>
												<td>{item.active ? <FontAwesomeIcon icon={faCheck} color="green" /> : <FontAwesomeIcon icon={faTimes} color="red" />}</td>
												<td>{item.created_At}</td>
												<td>{item.updated_At}</td>
												<td style={{ padding: "8", width: 50 }}>
													<div className='d-flex' style={{ margin: "auto 0" }}>
														<button type="button" className='btn btn btn-outline-dark btn-sm mr-1' onClick={() => { this.setState({ modalObj: { ...item, edit: true }, }) }} style={{ fontSize: 12, padding: "3 6" }}>
															<FontAwesomeIcon icon={faPenAlt} />
														</button>
														<button type="button" className='btn btn btn-outline-dark btn-sm' style={{ fontSize: 12, padding: "3 6" }}
															onClick={() => {
																if (window.confirm("voulez vous réinitialiser le mots de passe de : " + item.firstName + " " + item.lastName)) {
																	axios.post(`/api/user/reset/${item.matricule}`)
																		.then((res) => {
																			alert("Nouveau mot de passe : " + res.data)
																		})
																		.catch(err => {
																			alert("Prolème dans la réinitialisation de " + item.firstName + " " + item.lastName)
																		})
																}
															}}
														>
															<FontAwesomeIcon icon={faRotate} />
														</button>
													</div>
												</td>
											</tr>
										})}
							</tbody>
						</table>
					</div>
					<div className='d-flex'>
						<div className='mr-2'>
							<small>{this.state.entriesPage.totalElements} entrées trouvées</small>
						</div>
						<div style={{ flex: 1 }}></div>
						<div className='m-0'>
							<AdvancedPagination
								ref={this.paginationRef}
								pageCount={this.state.entriesPage.totalPages}
								onPageChange={(page) => this.setState({ page })}
							/>
						</div>
					</div>
				</div>
				{this.renderModal()}
			</div>
		)
	}
}
