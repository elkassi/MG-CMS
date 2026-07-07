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
import readXlsxFile from 'read-excel-file'
import logo from '../../assets/images/lear_logo.png'
import * as XLSX from 'xlsx';
import Switch from "react-switch";


export default class CuttingPlan extends Component {

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
			filterArr: [{ type: filterOptions[0].value }],
			filterFields: [],
			showAdjusterModal: false,
			adjusterModalArr: [],
			importModal: null,
		}
		this.inputArr = []
	}

	componentDidMount() {
		if (this.props.match.params.entityId != null) {
			axios.get(`/api/cuttingPlan/${this.props.match.params.entityId}`)
				.then(res => {
					this.setState({ modalObj: res.data })
				})
		} else if (this.props.history.location.pathname === "/cuttingPlan/new") {
			this.setState({ modalObj: {} })
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
			dependencies = this.state.filterArr.filter(e => e.type && e.fieldName && e.value).map((e, ind) => e.type + "." + e.fieldName + "." + ind + "=" + e.value).join("&")
			if (dependencies.length > 0) {
				dependencies += "&"
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
					if (err.response.data != null && err.response.data.username === "Invalid Username") {
						window.location.pathname = "/login";
					}
				})
		} catch (error) {
			console.log({ error })
		}
	}

	// renderErrorsAlert(errors) {
	// 	let arr = []
	// 	for (let prop in errors) {
	// 		arr.push(<li>{prop}: {errors[prop]}</li>)
	// 	}
	// 	return arr
	// }

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
			// this.props.history.push(`/${entity}/${item[metadata[entity].fields[0].name]}`)
			window.open(`/${entity}/${item[metadata[entity].fields[0].name]}`, '_blank').focus();
		}}>
			<td className='switch-form-td'>
				{/* <Switch id="enabled" name="enabled" 
					// style={{ zIndex: -1 }}
					checked={item.enabled || false}
					className="react-switch" offColor="#F00" width={40} height={20}
					onChange={(checked) => {
						if(checked) {
							axios.post(`/api/${entity}/enable/${item.id}`)
								.then((res) => {
									this.setState({ entriesList: this.state.entriesList.map(e => e.id === item.id ? { ...e, enabled: true } : e) })
								})
								.catch(err => {
		
								})
						} else {
							axios.post(`/api/${entity}/disable/${item.id}`)
								.then((res) => {
									this.setState({ entriesList: this.state.entriesList.map(e => e.id === item.id ? { ...e, enabled: false } : e) })
								})
								.catch(err => {
		
								})
						}
					}}
				/> */}
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
			arr.push(<div style={{ width: "33.33%", paddingLeft: "20" }} key={"ajuster-" + i}>
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
				<div style={{ display: "flex", flexWrap: "wrap", position: "relative" }}>{arr}</div>
			</div>}
		</Modal>
	}

	renderImportModal = (entity) => {
		if (this.state.importModal === null) { return <div></div> }
		let { importModal } = this.state
		return <Modal
			show={this.state.importModal !== null}
			onHide={() => this.setState({ importModal: null })}
			dialogClassName="modal-75w"
			centered
		>
			{this.state.importModal !== null && <div style={{ height: "95vh", overflowY: 'auto' }}>
				<h4 className='text-center'>Importer</h4>
				<div className='d-flex justify-content-center'>
					<input type='file' onChange={(e) => {
						const file = e.target.files[0];

						if (file) {
							const reader = new FileReader();
							let obj = {}
							reader.onload = (e) => {
								const data = e.target.result;
								const workbook = XLSX.read(data, { type: 'binary' });

								// Process the workbook, for example, log the first sheet's data
								const firstSheetName = workbook.SheetNames[0];
								const worksheet = workbook.Sheets[firstSheetName];
								const sheetData = XLSX.utils.sheet_to_json(worksheet, { header: 1 });

								obj.description = sheetData[2][2]
								obj.definition = sheetData[3][2]
								obj.cuttingPlanPartNumbers = []
								obj.enabled = true
								let i = 2
								while (sheetData[i][5] != null && sheetData[i][5].length > 6) {
									obj.cuttingPlanPartNumbers.push({
										partNumber: sheetData[i][5],
										description: sheetData[i][9],
										item: sheetData[i][15],
										// take only the number in sheetData[i][17]    .split(' ')[0]
										quantity: parseInt((sheetData[i][17] + "").split(' ')[0]),
									})
									i++;
								}
								let j = 12
								let materialPlacement = {}, materialInfo = []

								for (let j = 12; j < sheetData.length; j++) {
									if (sheetData[j][1] != null && sheetData[j][1].length > 0 && sheetData[j][1].length < 10 && sheetData[j][20] != null && sheetData[j][20] > 0) {
										if (materialPlacement[sheetData[j][2]] === undefined) {
											materialInfo.push({
												partNumberMaterial: sheetData[j][2],
												description: sheetData[j][3],
												matelassageEndroit: sheetData[j][9],
											})
											materialPlacement[sheetData[j][2]] = [{

												machine: sheetData[j][1],
												longueur: this.convertFloat(sheetData[j][4]),
												maxPlie: sheetData[j][5],//maxPlieDrill,maxDrill
												maxPlieDrill: sheetData[j][5],
												maxDrill: 30,
												laize: sheetData[j][6],
												longueurMatelas: this.convertFloat(sheetData[j][8]),
												activated: true,
												groupPlacement: 1,
												placement: sheetData[j][12],
												config: sheetData[j][13],
												drill: sheetData[j][14] + "," + sheetData[j][15],
												nbrCouche: sheetData[j][20],
											}]
										} else {
											if (!materialPlacement[sheetData[j][2]].map(e => e.placement).includes(sheetData[j][12])) {
												materialPlacement[sheetData[j][2]].push({
													machine: sheetData[j][1],
													longueur: this.convertFloat(sheetData[j][4]),
													maxPlie: sheetData[j][5],//maxPlieDrill,maxDrill
													maxPlieDrill: sheetData[j][5],
													maxDrill: 30,
													laize: sheetData[j][6],
													longueurMatelas: this.convertFloat(sheetData[j][8]),
													activated: true,
													groupPlacement: materialPlacement[sheetData[j][2]].length + 1,
													placement: sheetData[j][12],
													config: sheetData[j][13],
													drill: sheetData[j][14] + "," + sheetData[j][15],
													nbrCouche: sheetData[j][20],
												})
											}
										}
									}
								}

								materialInfo = materialInfo.map(elem => {
									elem.cuttingPlanMaterialPlacement = materialPlacement[elem.partNumberMaterial]
									return elem
								})

								obj.cuttingPlanMaterials = materialInfo

								this.setState({ importModal: { ...obj } })
								console.log('Sheet Data:', sheetData);
							};

							reader.readAsBinaryString(file);
						}
					}} />
				</div>
				{<div>
					<div className="row"
						style={{
							margin: "0",
							// width: "31.5cm", 
							marginBottom: "5px"
						}}
					>
						<div className="col-3 border border-dark" style={{ paddingLeft: "30px", paddingTop: "6px" }}>
							<img
								src={logo}
								alt="lear logo"
								height="40"
							/>
						</div>
						<div className="col-6 border border-dark">
							<h3 className="text-center mt-2">PLAN DE COUPE / MATELASSAGE</h3>
						</div>
						<div className="col-3 border border-dark">
							<p className="text-center mt-2">FR PE 47</p>
						</div>
					</div>
					<div style={{ fontSize: 16 }}>
						{/* <div className='d-flex'>
							<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Projet : </strong></div>
							<div className='text-no-wrap' style={{ width: "35%" }}>{importModal.projet}</div>
							<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Version : </strong></div>
							<div className='' style={{ width: "35%" }}>{importModal.version}</div>
						</div> */}
						<div className='row entityform-field'>
							<div className='row entityform-field col-6'>
								<label className='col-4 col-form-label text-right'><strong>Projet :</strong></label>
								{this.state.optionsList.projet && <Select id={"projet"} name={"projet"} classNamePrefix="rs"
									placeholder={"Projet..."} className='col-8 p-0'
									isClearable={false}
									value={(this.state.optionsList.projet && this.state.optionsList.projet.length > 0 && this.state.importModal.projet)
										? { label: this.state.importModal.projet, value: this.state.importModal.projet, item: this.state.importModal.projet }
										: null
									}
									options={this.state.optionsList.projet}
									onChange={(option) => {
										this.setState({ importModal: { ...this.state.importModal, projet: (option ? option.value : null) } })
										axios.get(`/api/projetVersion/projet/${option.value}`)
											.then(res => {
												this.setState({
													optionsList: { ...this.state.optionsList, version: res.data.map(e => { return { label: e.version, value: e.version, item: e } }) }

												})
											})
									}}
								/>}
							</div>
							<div className='row entityform-field col-6'>
								<label className='col-4 col-form-label text-right'><strong>Version :</strong></label>
								<Select id={"version"} name={"version"} classNamePrefix="rs"
									placeholder={"version..."} className='col-8 p-0'
									isClearable={true}
									value={(this.state.optionsList.version && this.state.optionsList.version.length > 0 && this.state.importModal.version)
										? { label: this.state.importModal.version, value: this.state.importModal.version, item: this.state.importModal.version }
										: null
									}
									options={this.state.optionsList.version}
									onChange={(option) => {
										this.setState({ importModal: { ...this.state.importModal, version: (option ? option.value : null) } })
									}}
								/>
							</div>
						</div>
						<div className='d-flex'>
							<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Modele : </strong></div>
							<div className='' style={{ width: "85%" }}>{importModal.description && importModal.description.replaceAll("_", " ")}</div>
						</div>
						<div className='d-flex'>
							<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Definition : </strong></div>
							<div className='text-no-wrap' style={{ width: "35%" }}>{importModal.definition}</div>
						</div>
					</div>
					<div className='mb-2'>
						<table className='table m-0 table table-grey-border entity-table-sm print-background'>
							<thead>
								<tr>
									<th style={{ fontWeight: "bold" }} className=''>Part Number</th>
									<th style={{ fontWeight: "bold" }} className=''>Description</th>
									<th style={{ fontWeight: "bold" }} className=''>Kit textil</th>
									<th style={{ fontWeight: "bold" }} className=''>Quantité</th>
								</tr>
							</thead>
							<tbody>
								{importModal.cuttingPlanPartNumbers && importModal.cuttingPlanPartNumbers.map(elemPn => <tr>
									<td className=''>{elemPn.partNumber}</td>
									<td className=''>{elemPn.description}</td>
									<td className=''>{elemPn.item}</td>
									<td className=''>{elemPn.quantity}</td>
								</tr>)}
							</tbody>
						</table>
					</div>
					<div className='mb-2'>
						<table className='table m-0 table table-grey-border entity-table-sm'>
							<thead>
								{/* <tr style={{ backgroundColor: "black" }}>
									<th style={{ fontWeight: "bold", fontSize: 20 }} className='' colSpan={5}>Matelassage</th>
									<th style={{ fontWeight: "bold", fontSize: 20 }} className=' ml-1' colSpan={5} >Coupe</th>
								</tr> */}
								<tr>
									<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Machine</th>
									<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Reference</th>
									<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Designation</th>
									<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Long Matelas</th>
									<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Max Plie</th>
									<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Nbr de couche</th>
									<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>La laize</th>
									<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Qt Tissu</th>
									<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Matelassage Endroit</th>
									<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Placement</th>
									<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>config</th>
									<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Drill 1</th>
									<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Drill 2</th>
								</tr>
							</thead>
							<tbody>
								{importModal.cuttingPlanMaterials && importModal.cuttingPlanMaterials.map((cpm, index) => {
									if (cpm == undefined || cpm.cuttingPlanMaterialPlacement == undefined || cpm.cuttingPlanMaterialPlacement.length == 0) return null
									return cpm.cuttingPlanMaterialPlacement.map((cpmp, index2) => {
										let arrDrill = (cpmp.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
										return <tr key={index + "-" + index2}>
											<td className=''>{cpmp.machine}</td>
											<td className=''>{cpm.partNumberMaterial}</td>
											<td className=''>{cpm.description}</td>
											<td className=''>{(cpmp.longueur)}</td>
											<td className=''>{cpmp.maxPlie}</td>
											<td className=''>{cpmp.nbrCouche}</td>
											<td className=''>{cpmp.laize}</td>
											<td className=''>{(cpmp.longueurMatelas)}</td>
											<td className=''>{cpm.matelassageEndroit}</td>
											<td className=''>{cpmp.placement}</td>
											<td className=''>{cpmp.config}</td>
											<td className=''>{arrDrill[0]}</td>
											<td className=''>{arrDrill[1]}</td>
										</tr>
									})
								})}
							</tbody>
						</table>
					</div>

				</div>}
				<div>
					<button className='btn btn-success' onClick={() => {
						axios.post("/api/cuttingPlan/save-bulk", importModal)
					}}>
						Enregistrer
					</button>
				</div>
			</div>}

		</Modal>
	}

	convertFloat = (float) => {
		if (!float || isNaN(float)) {
			return float
		}
		return parseFloat(parseFloat(float).toFixed(3))
	}

	renderErrorsAlert(errors) {
		let arr = [];
		if (typeof errors === 'string') {
			arr.push(<li>{errors}</li>)
		} else {
			for (let prop in errors) {
				if (typeof errors[prop] === 'string') {
					arr.push(<li>{parseInt(prop) + 1}: {errors[prop]}</li>)
				} else if (typeof errors[prop] === "object") {
					if (Object.keys(errors[prop]).length > 0) {
						arr.push(<li>{prop}: <ul>{this.renderErrorsAlert(errors[prop])}</ul></li>)
					}

				}
			}
		}
		return arr
	}



	render() {
		console.log(this.props)
		let entity = "cuttingPlan"
		let { entityId } = this.props.match.params
		if (metadata[entity] == null) {
			return <div>
				<h1 >Entity {entity} not found</h1>
			</div>
		}
		if (entity) {
			if (entityId) {
				return <CuttingPlanForm entityId={entityId} goBack={() => {
					this.props.history.push(`/cuttingPlan`)
					this.searchSubEntriesPage()
				}} />

			} else if (this.state.modalObj != null) {
				return <CuttingPlanForm goBack={() => {
					this.props.history.push(`/cuttingPlan`)
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
					<button type="button" className='btn btn-outline-danger btn-sm ml-2' onClick={() => {
						this.setState({ importModal: {} })
					}}>
						<FontAwesomeIcon icon={faDownload} /> Importer
					</button>
					<button type="button" className='btn btn-outline-danger btn-sm  ml-2'
						disabled={this.state.refreshLoading}
						onClick={() => {
							this.setState({ refreshLoading: true })
							axios.post(`/api/${entity}/refresh-cms`)
								.then(res => {
									this.setState({ refreshLoading: false })
									this.searchSubEntriesPage()
								})
								.catch(err => {
									this.setState({ refreshLoading: false })
								})
						}}>
						{this.state.refreshLoading ?
							<span><FontAwesomeIcon icon={faSpinner} spin /> Loading...</span>
							: <span><FontAwesomeIcon icon={faRefresh} /> Refresh CMS</span>}
					</button>
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
					{(this.state.error && Object.keys(this.state.error).length !== 0)
						&& !(this.state.error.subDemandes && this.state.error.subDemandes.length == 0) && <div className="alert alert-danger alert-error text-center m-4" role="alert">
							<ul>
								<button type='btn' className="btn btn-toclose" onClick={() => { this.setState({ error: null }) }}>
									<FontAwesomeIcon icon={faTimes} size="2x" />
								</button>
								{this.renderErrorsAlert(this.state.error)}
							</ul>
						</div>}

				</div>
				{this.renderAdjuterModal(entity)}
				{this.renderImportModal(entity)}
			</div>
		)
	}
}

