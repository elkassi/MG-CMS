import { faCheck, faDownload, faFilter, faGear, faSearch, faMagnifyingGlass, faPenAlt, faPlus, faRotate, faTimes, faUpload, faTrash, faEdit } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import axios from 'axios';
import React, { Component } from 'react'
import { Modal } from 'react-bootstrap';
import "../../styles/EntityList.scss"
import AdvancedPagination from "../utils/AdvancedPagination";
import SortIcon from "../utils/SortIcon";
import Switch from "react-switch";
import Select from "react-select";
import { filterOptions, metadata } from '../../metadata';
import { Link } from 'react-router-dom';
import { departementOption, problemeResoluOption } from '../../metadata'
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

//import for reading excel with readXlsxFile
import readXlsxFile from 'read-excel-file'


class ValidationIntervention extends Component {

	constructor() {
		super()
		this.state = {
			entriesPage: {
				totalElements: 0,
				totalPages: 1,
				numberOfElements: 0
			},
			sortProp: null,
			sortDirection: "desc",
			page: 0,
			size: 50,
			entriesList: [],
			codeErreurList: [],
			codeArretList: [],
			codeDefautList: [],
			optionsList: {},
			error: null,
			modalObj: null,
			showModalFilter: null,
			modalFilter: {},
			importModal: null,
			showFilter: false,
			filterArr: [{ type: filterOptions[0].value }],
			filterFields: [],
			showAdjusterModal: false,
			adjusterModalArr: [],
			editMode: false, // For admin edit mode
			deleteConfirmId: null, // For delete confirmation
		}
		this.inputArr = []
	}

	isAdmin = () => {
		const { user } = this.props.security;
		return user && user.roles && user.roles.some(role => role.authority === 'ROLE_ADMIN');
	}

	componentDidMount() {
		this.searchSubEntriesPage();
		this.setState({
			filterFields: (metadata["intervention"].fieldsFilter || metadata["intervention"].fields).map(e => {
				if (e.formDisplayProperty) {
					return { label: e.displayName, value: e.name + "." + e.formDisplayProperty, item: e }
				}
				return { label: e.displayName, value: e.name, item: e }
			})
		})
		axios.get("/api/codeErreur/list")
			.then(res => {
				this.setState({ codeErreurList: res.data })
			})
		axios.get("/api/codeArret/list")
			.then(res => {
				this.setState({ codeArretList: res.data })
			})
		axios.get("/api/codeDefaut/list")
			.then(res => {
				this.setState({ codeDefautList: res.data })
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

	searchSubEntriesPage = (config) => {
		try {
			let page = this.state.page;
			if (config && config.page !== null && config.page !== undefined) {
				page = config.page;
			}
			this.setState({
				entriesPage: {
					totalElements: 0,
					totalPages: 1,
					numberOfElements: 0
				},
				entriesList: null,
				modalObj: null,

			})

			// for (let k in this.state.modalFilter) {
			// 	if (this.state.modalFilter.hasOwnProperty(k) && this.state.modalFilter[k] && this.state.modalFilter[k] !== "") {		
			// 		dependencies += k + "=" + this.state.modalFilter[k] + "&"
			// 	}
			// }
			let dependencies = "";
			dependencies = this.state.filterArr.filter(e => e.type && e.fieldName && e.value).map(e => e.type + "." + e.fieldName + "=" + e.value).join("&")
			// dependencies = this.state.filterArr.filter(e => e.type && e.fieldName && e.value).map((e,ind) => e.type + "." + e.fieldName+"."+ind + "=" + e.value).join("&")
			if (dependencies.length > 0) {
				dependencies += "&"
			}
			axios.get(`/api/intervention/all?${dependencies}page=${page}&size=${this.state.size}&sort=${this.state.sortProp || (metadata["intervention"].firstOrderProperty || metadata["intervention"].fields[0].name)},${this.state.sortDirection}`)
				.then((res) => {
					this.setState({
						entriesList: res.data.content ? res.data.content : res.data.sort((a, b) => b.id - a.id),
						entriesPage: {
							totalElements: res.data.totalElements,
							totalPages: res.data.totalPages,
							numberOfElements: res.data.numberOfElements
						},
						page: page
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
						return <th onClick={() => this.sortChanged(field.name)}>{field.displayName}<SortIcon currentSort={field.name} sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="ml-1" /></th>
					})}
				<th >
					{/* {metadata[entity].fieldsFilter && metadata[entity].fieldsFilter.length > 0 && <button
						type="button" className='btn btn-outline-light mr-1'
						onClick={() => { this.setState({ showModalFilter: true }) }}
					>
						<FontAwesomeIcon icon={faFilter} size="sm" />
					</button>}
					{Object.keys(this.state.modalFilter).length > 0 && <button type="button" className='btn btn-outline-light'
						onClick={() => {
							this.setState({ modalFilter: {} })
							setTimeout(() => { this.searchSubEntriesPage() }, 10);
						}}
					>
						<FontAwesomeIcon icon={faFilterCircleXmark} />
					</button>} */}
				</th>
			</tr>
		</thead>
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

	handleFileChange = async (event, entity) => {
		let file = event.target.files[0];
		try {
			// Read all sheets from the Excel file
			const sheets = await readXlsxFile(file, { getSheets: true });

			// Use Promise.all to read and convert each sheet into JSON
			const promises = sheets.map((sheet, index) =>
				readXlsxFile(file, { sheet: index + 1 }).then((rows) => {
					const arr = [];
					rows.forEach((row, rowInd) => {
						if (rowInd > 0) {
							const elem = {};
							row.forEach((cell, cellInd) => {
								elem[metadata[entity].fields[cellInd].name] = cell;
							});
							elem.id = null;
							elem.createdAt = null;
							elem.addedBy = null;
							elem.updatedAt = null;
							elem.updatedBy = null;
							arr.push(elem);
						}
					});
					return arr;
				})
			);

			// Wait for all promises to resolve and set the importModal state with the results
			const results = await Promise.all(promises);
			const allSheetsData = [].concat(...results);
			this.setState({ importModal: allSheetsData });
		} catch (error) {
			console.error('Error reading Excel file:', error);
		}
	};

	renderModal = (entity) => {
		let codeErreur = (this.state.modalObj && this.state.modalObj.codeErreur) ? this.state.codeErreurList.find(err => err.code.toUpperCase().trim() === this.state.modalObj.codeErreur.toUpperCase().trim()) : null

		return <Modal
			show={this.state.modalObj != null}
			onHide={() => this.setState({ modalObj: null })}
			dialogClassName="modal-75w"
			centered
		>
			{this.state.modalObj && <div style={{
				maxHeight: "calc(95vh - 30px)",
				overflow: "auto",
				padding: "8 15 0",
				display: 'flex', flexDirection: "column"
			}}>
				<div style={{ flex: 1 }}>
					<h2 className='text-center m-2'>Formulaire d'intervention : {this.state.modalObj.id}</h2>
					<div style={{ paddingTop: 35 }}>
						<div className='row my-2 '>
							<div className='col-2 text-right'>
								Serie
							</div>
							<div className='col-3'>
								{this.state.modalObj.serie}
							</div>
							<div className='col-2 text-right'>
								Séquence
							</div>
							<div className='col-3'>
								{this.state.modalObj.sequence}
							</div>
						</div>
						<div className='row my-2 '>
							<div className='col-2 text-right'>
								date
							</div>
							<div className='col-3'>
								{this.state.modalObj.date}
							</div>
							<div className='col-2 text-right'>
								shift
							</div>
							<div className='col-3'>
								{this.state.modalObj.shift}
							</div>
						</div>
						<div className='row my-2 '>
							<div className='col-2 text-right'>
								Référence tissu
							</div>
							<div className='col-3'>
								{this.state.modalObj.partNumberMaterial}
							</div>
							<div className='col-2 text-right'>
								Description
							</div>
							<div className='col-3'>
								{this.state.modalObj.partNumberMaterialDescription}
							</div>
						</div>
						<div className='row my-2 '>
							<div className='col-2 text-right'>
								Machine
							</div>
							<div className='col-3'>
								{this.state.modalObj.machine}
							</div>
						</div>
						<div className='row my-2 '>
							<div className='col-2 text-right'>
								Code d'erreur
							</div>
							<div className='col-3'>
								{this.state.modalObj.codeErreur}
							</div>
							<div className='col-2 text-right'>
								Début d'arrêt
							</div>
							<div className='col-3'>
								{this.state.modalObj.debutArret?.replace("T", " ")}
							</div>

						</div>
						{codeErreur && <div className='row my-2 '>
							<div className='col-2 text-right'>
								Détails code d'erreur
							</div>
							<div className='col-10'>
								<ul>
									<li>designation : {codeErreur.designation}</li>
									<li>Cause : {codeErreur.rootCause}</li>
									<li>Action Possible : {codeErreur.actionPossible}</li>
								</ul>
							</div>
						</div>}

						<div className='row my-2 '>
							<div className='col-2 text-right'>
								Début d'intervention
							</div>
							<div className='col-3'>
								{this.state.modalObj.debutIntervention?.replace("T", " ")}
							</div>
							<div className='col-2 text-right'>
								Fin d'intervention
							</div>
							<div className='col-3'>
								{this.state.modalObj.finIntervention?.replace("T", " ")}
							</div>
						</div>
						<div className='row my-2'>
							<label className='col-2 text-right my-2'>Département</label>
							<label className='col-3 my-2'>{this.state.modalObj.departement}</label>

							<label className='col-2 col-form-label text-right'>Problème résolu ?</label>
							<Select classNamePrefix="rs" className='p-0 input-sm col-3'
								isClearable={true} placeholder={"Problème Resolu..."}
								value={this.state.modalObj.problemeResolu ? { label: this.state.modalObj.problemeResolu, value: this.state.modalObj.problemeResolu } : null}
								options={problemeResoluOption}
								menuPlacement={"top"}
								onChange={(option) => {
									if (option) {
										this.setState({
											modalObj: {
												...this.state.modalObj,
												problemeResolu: option.value,
											}
										})
									} else {
										this.setState({
											modalObj: {
												...this.state.modalObj,
												problemeResolu: null,
											}
										})
									}
								}}
							/>
						</div>
						<div className='row my-2'>
							<label className='col-2 col-form-label text-right'>Code d'arrêt</label>
							<Select classNamePrefix="rs" className='p-0 input-sm col-3'
								isClearable={true} placeholder={"Code d'arrêt..."}
								value={this.state.modalObj.codeArret ? { label: this.state.modalObj.codeArret.code + " " + this.state.modalObj.codeArret.motifArret, value: this.state.modalObj.codeArret } : null}
								options={this.state.modalObj.departement
									? this.state.codeArretList.filter(e => e.departement === this.state.modalObj.departement).map(codeArret => ({ label: codeArret.code + " " + codeArret.motifArret, value: codeArret }))
									: []} // departementOption
								// menuPosition={'absolute'} // 'fixed', 'absolute'
								menuPlacement={"top"} // auto, bottom, top
								onChange={(option) => {
									if (option) {
										this.setState({
											modalObj: {
												...this.state.modalObj,
												codeArret: option.value,
											}
										})
									} else {
										this.setState({
											modalObj: {
												...this.state.modalObj,
												codeArret: null,
											}
										})
									}
								}}
							/>
						</div>

						<div className='row my-2'>
							{this.state.modalObj.codeArret && this.state.modalObj.codeArret.code === "D_2" && [<label className='col-2 col-form-label text-right'>Code Coupe</label>,
							<Select classNamePrefix="rs" className='p-0 input-sm col-3'
								isClearable={true} placeholder={"Code d'arrêt..."}
								value={this.state.modalObj.codeCoupe ? { label: this.state.modalObj.codeCoupe.code, value: this.state.modalObj.codeCoupe } : null}
								options={this.state.modalObj.departement
									? this.state.codeDefautList.filter(e => e.departement === "Qualite").map(codeElem => ({ label: codeElem.code + " " + codeElem.description, value: codeElem }))
									: []} // departementOption
								// menuPosition={'absolute'} // 'fixed', 'absolute'
								menuPlacement={"top"} // auto, bottom, top
								onChange={(option) => {
									if (option) {
										this.setState({
											modalObj: {
												...this.state.modalObj,
												codeCoupe: option.value,
											}
										})
									} else {
										this.setState({
											modalObj: {
												...this.state.modalObj,
												codeCoupe: null,
											}
										})
									}
								}}
							/>]}
							{this.state.modalObj.departement === "Maintenance" && [<label className='col-2 col-form-label text-right'>Solution</label>,
							<Select classNamePrefix="rs" className='p-0 input-sm col-3'
								isClearable={true} placeholder={"Code d'arrêt..."}
								value={this.state.modalObj.solution ? { label: this.state.modalObj.solution.code, value: this.state.modalObj.solution } : null}
								options={this.state.modalObj.departement
									? this.state.codeDefautList.filter(e => e.departement === this.state.modalObj.departement && e.type == "Solution").map(codeArret => ({ label: codeArret.code + " " + codeArret.description, value: codeArret }))
									: []} // departementOption
								// menuPosition={'absolute'} // 'fixed', 'absolute'
								menuPlacement={"top"} // auto, bottom, top
								onChange={(option) => {
									if (option) {
										this.setState({
											modalObj: {
												...this.state.modalObj,
												solution: option.value,
											}
										})
									} else {
										this.setState({
											modalObj: {
												...this.state.modalObj,
												solution: null,
											}
										})
									}
								}}
							/>]}

						</div>
					</div>
					<div className='row my-2'>
						<label className='col-2 col-form-label text-right'>Matricule d'émetteur</label>
						<div className='col-3 ' style={{ padding: "7 0 0" }}>
							{this.state.modalObj.matriculeEmetteur}
						</div>
						<label className='col-2 col-form-label text-right'>Matricule de responsable</label>
						<div className='col-3 p-0'>
							<input className={`form-control input-sm`}
								value={this.state.modalObj.matriculeResponsable || ''}
								onChange={event => this.setState({ modalObj: { ...this.state.modalObj, matriculeResponsable: event.target.value } })}
							/>
						</div>
					</div>
				</div>
				<div style={{
					borderTop: "1px solid #ddd",
					margin: "10px 0",
				}} ></div>
				<div className='row my-2'>
					<label className='col-2 col-form-label text-right'>Cause</label>
					<div className='col-3 p-0'>
						<input className={`form-control input-sm`}
							value={this.state.modalObj.cause || ''}
							onChange={event => this.setState({ modalObj: { ...this.state.modalObj, cause: event.target.value } })}
						/>
					</div>
					<label className='col-2 col-form-label text-right'>Action</label>
					<div className='col-3 p-0'>
						<textarea className={`form-control input-sm`} rows="3" 
							value={this.state.modalObj.action || ''}
							onChange={event => this.setState({ modalObj: { ...this.state.modalObj, action: event.target.value } })}
						/>
					</div>
				</div>
				<div style={{}}>
					<div className='row my-2'>
						<label className='col-2 col-form-label text-right'>Date de validation</label>
						<label className='col-3 col-form-label'>
							{this.state.modalObj.dateValidation}
						</label>
						<label className='col-2 col-form-label text-right'>Validé par</label>
						<label className='col-3 col-form-label'>
							{this.state.modalObj.validerPar}
						</label>
					</div>
				</div>

				<div style={{
					borderTop: "1px solid #ddd",
					margin: "10px 0",
				}} ></div>
				<div style={{ display: "flex", flexDirection: 'row-reverse', backgroundColor: "white", position: "sticky", bottom: 0 }} className="p-2">
					<button className='btn btn-danger' onClick={() => {
						axios.post("/api/intervention/valider", this.state.modalObj)
							.then(res => {
								this.setState({ modalObj: null })
								this.searchSubEntriesPage()
							})
					}}>{"Valider"}</button>
					{this.isAdmin() && <button className='btn btn-primary mr-2' onClick={() => {
						axios.post("/api/intervention", this.state.modalObj)
							.then(res => {
								this.setState({ modalObj: null })
								this.searchSubEntriesPage()
							})
							.catch(err => {
								alert(err.response?.data || "Erreur lors de la sauvegarde")
							})
					}}><FontAwesomeIcon icon={faEdit} /> Modifier</button>}
					{this.isAdmin() && <button className='btn btn-outline-danger mr-2' onClick={() => {
						if (window.confirm(`Êtes-vous sûr de vouloir supprimer l'intervention ${this.state.modalObj.id}?`)) {
							axios.post("/api/intervention/delete", this.state.modalObj)
								.then(res => {
									this.setState({ modalObj: null })
									this.searchSubEntriesPage()
								})
								.catch(err => {
									alert(err.response?.data || "Erreur lors de la suppression")
								})
						}
					}}><FontAwesomeIcon icon={faTrash} /> Supprimer</button>}
					<button className='btn btn-link' onClick={() => { this.setState({ modalObj: null }) }}>Retour</button>
				</div>
			</div>}
		</Modal>
	}

	renderFilterField = (arr, ind) => {
		if (arr[ind].item
			// && arr[ind].item.type === "text"
		) {
			return [
				<Select classNamePrefix="rs"
					placeholder={""} className='col-2 p-0 ml-2 '
					isClearable={false}
					value={(arr[ind].type)
						? filterOptions.find(e => e.value === arr[ind].item.type)
						: null
					}
					options={filterOptions}
					onChange={(option) => {
						arr[ind].type = option.value
						this.setState({ filterArr: [...arr] })
					}}
				/>,

				<input className='ml-2 filter-input'
					value={arr[ind].value}
					onChange={e => {
						arr[ind].value = e.target.value
						this.setState({ filterArr: [...arr] })
					}}
				/>]
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

					{/* {this.renderFilterField(arr, ind)} */}

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

						this.searchSubEntriesPage({ page: 0 })
					}}><FontAwesomeIcon icon={faSearch} /> Chercher</button>}
					{ind === 0 && <button className='btn btn-outline-secondary ml-2 btn-sm' onClick={() => {
						this.setState({ filterArr: [{ type: filterOptions[0].value }] })
						this.searchSubEntriesPage()
					}}><FontAwesomeIcon icon={faTimes} /> Reset</button>}
				</div>)}
			</div>
		</div>
	}

	renderRow = (entity, item, ind) => {
		try {
			return <tr key={"row-" + ind} className={metadata[entity].operation.includes("Edit") ? "table-row-selective" : ""}
				onDoubleClick={() => {
					if (metadata[entity].operation.includes("Edit")) {
						this.setState({ modalObj: { ...item } })
					}
				}}
			>
				{metadata[entity].fields
					.filter(elem => elem.hideTable !== true && !this.state.adjusterModalArr.includes(elem.name))
					.map(field => {
						switch (field.type) {
							case "boolean":
								return (item[field.name] === true ? <td><FontAwesomeIcon icon={faCheck} color="green" /></td> : item[field.name] === false ? <td><FontAwesomeIcon icon={faTimes} color="red" /></td> : <td></td>)
							case "object":
								return <td>{item[field.name] && item[field.name][field.formDisplayProperty]}</td>
							default:
								return <td>{item[field.name]}</td>
						}
					})}
				{<td style={{ padding: "5", width: 50 }}>
					<div className='d-flex' style={{ margin: "auto 0" }}>
						{metadata[entity].operation.includes("Edit") && <button type="button" className='btn btn btn-outline-dark btn-sm mr-1'
							onClick={() => {
								this.setState({ modalObj: { ...item } })
							}}
							style={{ fontSize: 12, padding: "3 6" }}>
							<FontAwesomeIcon icon={faCheck} />
						</button>}
					</div>
				</td>}
			</tr>
		} catch (err) {
			return;
		}

	}




	render() {
		console.log(this.props)
		let entity = "intervention"
		if (metadata[entity] == null) {
			return <div>
				<h1 >Entity {entity} not found</h1>
			</div>
		}


		return (
			<div>
				<h1 className='text-center' style={{ marginTop: 10 }}>{metadata[entity].displayName}</h1>
				<div className='d-flex align-items-center mb-1 mr-2'>
					{/* {metadata[entity].operation.includes("Add") && <button type="button" className='btn btn-outline-danger ml-2' onClick={() => { this.setState({ modalObj: {} }) }} style={{ padding: 8 }}>
						<span style={{ fontSize: 12 }}><FontAwesomeIcon icon={faPlus} /></span>
					</button>} */}
					{metadata[entity].operation.includes("Export") && <button type="button" className='btn btn-outline-danger ml-2'
						onClick={() => {
							// this.searchSubEntriesPage()
							if (window.confirm(`est-ce-que vous êtes sûre d'exporter ${this.state.entriesPage.totalElements} ligne?`)) {
								let dependencies = "";
								dependencies = this.state.filterArr.filter(e => e.type && e.fieldName && e.value).map(e => e.type + "." + e.fieldName + "=" + e.value).join("&")
								axios(
									{
										url: `/api/${entity}/download/${entity}.xlsx?${dependencies}`, //your url
										method: 'GET',
										responseType: 'blob', // important
									}
								).then((response) => {
									const url = window.URL.createObjectURL(new Blob([response.data]));
									const link = document.createElement('a');
									link.href = url;
									link.setAttribute('download', `${entity}.xlsx`); //or any other extension
									document.body.appendChild(link);
									link.click();
								});
							}
						}}
					>
						<span style={{ fontSize: 12 }}><FontAwesomeIcon icon={faDownload} /> Export</span>
					</button>}
					{metadata[entity].operation.includes("Import") && <button type="button" className='btn btn-outline-danger ml-2' onClick={() => { this.setState({ importModal: [] }) }}>
						<span style={{ fontSize: 12 }}><FontAwesomeIcon icon={faUpload} /> Import</span>
					</button>}
					{metadata[entity].operation.includes("Supprimer") && <button type="button" className='btn btn-outline-danger ml-2' disabled={this.state.filterArr.filter(e => e.type && e.fieldName && e.value).length === 0}
						onClick={() => {
							if (window.confirm(`est-ce-que vous êtes sûre de supprimer ${this.state.entriesPage.totalElements} ligne?`)) {
								let dependencies = "";
								dependencies = this.state.filterArr.filter(e => e.type && e.fieldName && e.value).map(e => e.type + "." + e.fieldName + "=" + e.value).join("&")
								axios.post(`/api/${entity}/supprimer?${dependencies}`).then((response) => {
									this.searchSubEntriesPage()
								});
							}
						}}
					>
						<span style={{ fontSize: 12 }}><FontAwesomeIcon icon={faTimes} /> Supprimer</span>
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
				<div className='px-2'>
					<div className='table-responsive entity-table mb-2' style={this.state.showFilter ? { maxHeight: `calc(100vh - ${(33 * Math.min(this.state.filterArr.length, 10) + 200)}px)` } : { maxHeight: `calc(100vh - 163px)` }}>
						<table className='table table-bordered m-0'>
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
					<div className='d-flex mb-2'>
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
				{this.renderModal(entity)}
			</div>
		)
	}
}

ValidationIntervention.propTypes = {
	security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
	security: state.security
})

export default connect(mapStateToProps, {})(ValidationIntervention);