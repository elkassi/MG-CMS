import { faAngleDown, faAngleLeft, faAngleRight, faAngleUp, faArrowDown, faArrowLeft, faArrowRight, faArrowUp, faCheck, faClock, faEye, faFloppyDisk, faInfo, faPlus, faPrint, faRefresh, faRightLeft, faSearch, faTimes, faTrashAlt } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import axios from 'axios'
import React, { Component } from 'react'
import { metadata, optionsMatelassageEndroit } from '../../metadata'
import Select from "react-select";
import Switch from "react-switch";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import moment from 'moment'
import AsyncSelect from 'react-select/async';
import "../../styles/CuttingPlanForm.scss"
import { FormControl, InputGroup, Button, Dropdown, Modal } from "react-bootstrap";
import ReactToPrint from "react-to-print";
import GammePn from './GammePn';
import logo from '../../assets/images/lear_logo.png'

export default class CuttingPlanCombinaisonForm extends Component {
	constructor() {
		super()
		this.state = {
			modalObj: {},
			optionsList: {},
			machineRow: null,
			marginRow: null,
			reftissuCopy: "",
			partNumberMaterialConfigs: {},
			hideColumn: false,
			loading: false,
			loadingTest: false,
			selectedBoxs: [],
			arrPlieConfig: [],
			ctcData: {},
		}
		this.inputArr = []


	}

	componentDidMount() {
		const { entityId } = this.props
		if (entityId) {
			axios.get(`/api/cuttingPlanCombination/${entityId}`)
				.then(res => {
					this.setState({ modalObj: res.data })

					if (this.state.modalObj.projet) {
						axios.get(`/api/projetVersion/projet/${this.state.modalObj.projet}`)
							.then((res) => {
								this.setState({
									optionsList: {
										...this.state.optionsList,
										version: res.data.map((elem) => ({ value: elem.version, label: elem.version, item: elem }))
									}
								})
								axios.get(`/api/plieConfig/projet/${this.state.modalObj.projet}`)
									.then(res2 => {
										this.setState({ arrPlieConfig: res2.data })
									})
							})
						let arrPn = this.state.modalObj.cuttingPlanCombinationPartNumbers || []
						let arrBoom = []
						let cuttingPlanMaterialsList = this.state.modalObj.cuttingPlanMaterials || []
						Promise.all(arrPn.map((elemPn, indPn) => {
							let dep = ["project=" + this.state.modalObj.projet]
							if (this.state.modalObj.version) { dep.push("version=" + this.state.modalObj.version) }
							dep.push("partNumber=" + elemPn.partNumber)
							return axios.get(`/api/partNumberBoom/list?${dep.join("&")}`)
						}))
							.then(res => {
								arrPn.map((elemPn, indPn) => {
									arrBoom = [...arrBoom, ...res[indPn].data]
									let arrReftissuOption = res[indPn].data.filter(e => e.partNumber === elemPn.partNumber)
									arrReftissuOption.map((option) => {
										let i = cuttingPlanMaterialsList.findIndex(e => e.partNumberMaterial === option.partNumberMaterial)
										if (i >= 0) {
											if (!cuttingPlanMaterialsList[i].partNumbers.split(",").includes(elemPn.partNumber)) {
												cuttingPlanMaterialsList[i].partNumbers += "," + elemPn.partNumber
											}
										} else {
											cuttingPlanMaterialsList.push({
												partNumberMaterial: option.partNumberMaterial,
												description: option.partNumberMaterialDescription,
												partNumbers: option.partNumber,
												cuttingPlanMaterialPlacement: [{ groupPlacement: 1, activated: true }]
											})
										}
									})
								})
							})
							.then(() => {
								let obj = {}
								axios.get(`/api/partNumberMaterialConfig/pns/${cuttingPlanMaterialsList.map(e => e.partNumberMaterial).join(",")}`)
									.then(res => {
										res.data.map(e => {
											obj[e.partNumberMaterial] = { ...e }
										})
										// if (!arrPn.map(pn=>pn.partNumber).includes(elem.partNumber)) {
										// 	arrPn.push(elem)
										// }
										// if (this.state.modalObj.cuttingPlanCombinationPartNumbers.map(e => e.partNumber).includes(elem.partNumber)
										// 	&& !cuttingPlanMaterials.map(e => e.partNumberMaterial).includes(elem.partNumberMaterial)) {

										// }
										cuttingPlanMaterialsList = [...cuttingPlanMaterialsList].map(e => {
											if (obj[e.partNumberMaterial]) {
												e.tauxScrap = obj[e.partNumberMaterial].tauxScrap
												e.rotation = obj[e.partNumberMaterial].rotation
												e.plaque = obj[e.partNumberMaterial].plaque
												e.vitesse = obj[e.partNumberMaterial].vitesse
												e.matelassageEndroit = obj[e.partNumberMaterial].matelassageEndroit
												let machine = obj[e.partNumberMaterial].reftissuMachines.find(cm => cm.defaultValue === true)
												if (machine != null && e.cuttingPlanMaterialPlacement[0].machine == null) {
													e.cuttingPlanMaterialPlacement[0].machine = machine.machineType
													e.cuttingPlanMaterialPlacement[0].maxPlie = this.converter(machine.maxPlie, this.state.arrPlieConfig)
													e.cuttingPlanMaterialPlacement[0].maxPlieDrill = this.converter(machine.maxPlieDrill, this.state.arrPlieConfig)
													e.cuttingPlanMaterialPlacement[0].maxDrill = machine.maxDrill
													e.cuttingPlanMaterialPlacement[0].pliesConfig = machine.pliesConfig
												}
												let category = obj[e.partNumberMaterial].reftissuCategories.find(cm => cm.defaultValue === true)
												if (category != null && e.cuttingPlanMaterialPlacement[0].category == null) {
													e.cuttingPlanMaterialPlacement[0].category = category.category
													e.cuttingPlanMaterialPlacement[0].laize = category.borneMin
												}
											}
											return e;
										})

									})
									.finally(() => {
										this.setState({
											modalObj: { ...this.state.modalObj, cuttingPlanCombinationPartNumbers: [...arrPn], cuttingPlanMaterials: cuttingPlanMaterialsList },
											partNumberMaterialConfigs: { ...obj },
											optionsList: { ...this.state.optionsList, partNumberBoom: arrBoom.map(e => { return { label: e.partNumber, value: e.partNumber, item: e } }) }
										})
									})
							})

						// let dep = ["project=" + this.state.modalObj.projet]
						// if (this.state.modalObj.version) {
						// 	dep.push("version=" + this.state.modalObj.version)
						// }
						// axios.get(`/api/partNumberBoom/list?${dep.join("&")}`)
						// 	.then((res) => {
						// 		let arrPn = [], arr = [], cuttingPlanMaterials = [...this.state.modalObj.cuttingPlanMaterials]
						// 		res.data.map(elem => {
						// 			if (!arrPn.includes(elem.partNumber)) {
						// 				arrPn.push(elem.partNumber)
						// 				arr.push(elem)
						// 			}
						// 			if (this.state.modalObj.cuttingPlanCombinationPartNumbers.map(e => e.partNumber).includes(elem.partNumber)
						// 				&& !cuttingPlanMaterials.map(e => e.partNumberMaterial).includes(elem.partNumberMaterial)) {
						// 				cuttingPlanMaterials.push({
						// 					partNumberMaterial: elem.partNumberMaterial,
						// 					description: elem.partNumberMaterialDescription,
						// 					pnList: res.data.filter(e => e.partNumberMaterial === elem.partNumberMaterial).map(e => e.partNumber).join(","),
						// 					cuttingPlanMaterialPlacement: [{ groupPlacement: 1, activated: true }]
						// 				})
						// 			}
						// 		})
						// 		this.setState({
						// 			modalObj: { ...this.state.modalObj, cuttingPlanMaterials: cuttingPlanMaterials },
						// 			optionsList: {
						// 				...this.state.optionsList,
						// 				partNumber: [...arr].map((elem) => ({ value: elem.partNumber, label: elem.partNumber + " (" + elem.description + ")", item: elem })),
						// 				partNumberBoom: res.data.map((elem) => ({ value: elem.partNumber, label: elem.partNumber + " (" + elem.description + ")", item: elem }))
						// 			}
						// 		})
						// 	})
						// 	.finally(() => {
						// 		this.chargerReftissuConfig()
						// 	})
					}

				})
		}
		axios.get(`/api/zone/list`)
			.then((res) => {
				this.setState({
					optionsList: {
						...this.state.optionsList,
						zone: res.data.map((elem) => ({ value: elem.nom, label: elem.nom, item: elem }))
					}
				})
			})

		axios.get(`/api/machineType/list`)
			.then((res) => {
				this.setState({ optionsList: { ...this.state.optionsList, machineType: res.data.map(elem => { return { label: elem.name, value: elem.name } }) } })
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

	chargerReftissuConfig = () => {
		axios.get(`/api/partNumberMaterialConfig/pns/${this.state.modalObj.cuttingPlanMaterials.map(e => e.partNumberMaterial).join(",")}`)
			.then(res => {
				let obj = {}
				res.data.map(e => {
					obj[e.partNumberMaterial] = { ...e }
				})
				let cuttingPlanMaterials = [...this.state.modalObj.cuttingPlanMaterials].map(e => {
					e.vitesse = obj[e.partNumberMaterial].vitesse
					e.tauxScrap = obj[e.partNumberMaterial].tauxScrap
					e.rotation = obj[e.partNumberMaterial].rotation
					e.plaque = obj[e.partNumberMaterial].plaque
					e.matelassageEndroit = obj[e.partNumberMaterial].matelassageEndroit
				})
				this.setState({
					partNumberMaterialConfigs: { ...obj },
					// cuttingPlanMaterials: cuttingPlanMaterials
				})
			})
	}

	renderForm = (entityId) => {
		// let arrMultiplications = this.state.modalObj.multiplications ? this.state.modalObj.multiplications.split(";") : [""]

		return <div className='row entityform-field'>
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>projet :</label>
				{this.state.optionsList.projet && <Select id={"projet"} name={"projet"} classNamePrefix="rs"
					placeholder={"Projet..."} className='col-8 p-0'
					isClearable={false}
					value={(this.state.optionsList.projet && this.state.optionsList.projet.length > 0 && this.state.modalObj.projet)
						? { label: this.state.modalObj.projet, value: this.state.modalObj.projet, item: this.state.modalObj.projet }
						: null
					}
					options={this.state.optionsList.projet}
					onChange={(option) => {
						this.setState({ modalObj: { ...this.state.modalObj, projet: (option ? option.value : null) } })
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
				<label className='col-4 col-form-label text-right'>version :</label>
				<Select id={"version"} name={"version"} classNamePrefix="rs"
					placeholder={"version..."} className='col-8 p-0'
					isClearable={true}
					value={(this.state.optionsList.version && this.state.optionsList.version.length > 0 && this.state.modalObj.version)
						? { label: this.state.modalObj.version, value: this.state.modalObj.version, item: this.state.modalObj.version }
						: null
					}
					options={this.state.optionsList.version}
					onChange={(option) => {
						this.setState({ modalObj: { ...this.state.modalObj, version: (option ? option.value : null) } })
					}}
				/>
			</div>
			<div className='row col-12 py-2'>
				<div className='col-2 text-right'>
					Description :
				</div>
				<div className='col-10 p-0'>
					<span>
						{this.state.modalObj.description}
					</span>
				</div>
			</div>
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>definition :</label>
				<input className='form-control col-8 entityform-input' value={this.state.modalObj.definition || ""}
					onChange={(event) => {
						this.setState({
							modalObj: { ...this.state.modalObj, definition: event.target.value },
						})
					}}
				/>
			</div>
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>commentaire :</label>
				<textarea className='form-control col-8 entityform-input' value={this.state.modalObj.commentaire || ""}
					rows={3}
					onChange={(event) => {
						this.setState({
							modalObj: { ...this.state.modalObj, commentaire: event.target.value }
						})
					}}
				/>
			</div>
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>active</label>
				<div className='col-2'>
					<Switch id="envoyerCAD" name="envoyerCAD" checked={this.state.modalObj.enabled || false}
						className="react-switch mt-1" offColor="#F00"
						onChange={(checked) => {
							this.setState({ modalObj: { ...this.state.modalObj, enabled: checked } })
						}}
					/>
				</div>
			</div>
			<div className='row col-6 py-2'>
				<div className='col-4 text-right'>
					Changement d'activation par :
				</div>
				<div className='col-8 p-0'>
					{this.state.modalObj.enabledBy && <span>
						{this.state.modalObj.enabledBy.lastName} {this.state.modalObj.enabledBy.firstName} ({this.state.modalObj.enabledBy.matricule}) {this.state.modalObj.enabledAt}
					</span>}
				</div>
			</div>
			<div className='row col-6 py-2'>
				<div className='col-4 text-right'>
					Créer par :
				</div>
				<div className='col-8 p-0'>
					{this.state.modalObj.createdBy && <span>
						{this.state.modalObj.createdBy.lastName} {this.state.modalObj.createdBy.firstName} ({this.state.modalObj.createdBy.matricule}) {this.state.modalObj.createdAt}
					</span>}
				</div>
			</div>

			<div className='row col-6 py-2'>
				<div className='col-4 text-right'>
					Dernière modification par :
				</div>
				<div className='col-8 p-0'>
					{this.state.modalObj.updatedBy && <span>
						{this.state.modalObj.updatedBy.lastName} {this.state.modalObj.updatedBy.firstName} ({this.state.modalObj.updatedBy.matricule}) {this.state.modalObj.updatedAt}
					</span>}
				</div>
			</div>

			<div className='row entityform-field col-6 d-flex'>
				<label className='col-4 col-form-label text-right'>Start Date :</label>
				<DatePicker
					id={"startDate"}
					name={"startDate"}
					placeholderText={"startDate"}
					className="form-control"
					autoComplete="off"
					selected={this.state.modalObj.startDate ? moment(this.state.modalObj.startDate, 'YYYY-MM-DD, HH:mm').toDate() : null}
					onChange={date => this.setState({ modalObj: { ...this.state.modalObj, startDate: date ? moment(date).format('YYYY-MM-DD, HH:mm') : null } })}
					showTimeSelect={true}
					timeFormat="HH:mm"
					timeIntervals={30}
					timeCaption=""
					isClearable={true}
					dateFormat={'yyyy-MM-dd, HH:mm'}
				/>
			</div>
			<div className='row entityform-field col-6 d-flex'>
				<label className='col-4 col-form-label text-right'>End Date :</label>
				<DatePicker
					id={"endDate"} style={{ flex: 1 }}
					name={"endDate"}
					placeholderText={"endDate"}
					className="form-control"
					autoComplete="off"
					selected={this.state.modalObj.endDate ? moment(this.state.modalObj.endDate, 'YYYY-MM-DD, HH:mm').toDate() : null}
					onChange={date => this.setState({ modalObj: { ...this.state.modalObj, endDate: date ? moment(date).format('YYYY-MM-DD, HH:mm') : null } })}
					showTimeSelect={true}
					timeFormat="HH:mm"
					timeIntervals={30}
					timeCaption=""
					isClearable={true}
					dateFormat={'yyyy-MM-dd, HH:mm'}
				/>
			</div>
			{/* <div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>Multiplications :</label>
				<div className='col-8'>
					<div>
						{arrMultiplications.map((multiplication, index) => {
							return <div key={index} className='d-flex mb-1'>
								<input value={multiplication} style={{ width: 50 }}  className='form-control entityform-input'
									onChange={(event) => {
										arrMultiplications[index] = event.target.value
										this.setState({ modalObj: { ...this.state.modalObj, multiplications: arrMultiplications.join(";") } })
									}} />
								<button
									className='btn btn-link'
									onClick={() => {
										arrMultiplications.splice(index, 1)
										this.setState({ modalObj: { ...this.state.modalObj, multiplications: arrMultiplications.join(";") } })
									}}
								>
									<FontAwesomeIcon icon={faTrashAlt} />
								</button>
							</div>
						})}
						<button
							className='btn btn-outline-danger'
							onClick={() => {
								arrMultiplications.push("")
								this.setState({ modalObj: { ...this.state.modalObj, multiplications: arrMultiplications.join(";") } })
							}}
						>
							<FontAwesomeIcon icon={faPlus} />
						</button>
					</div>
				</div>
			</div> */}
		</div>
	}

	renderPnTable = () => {
		let arrPn = this.state.modalObj.cuttingPlanCombinationPartNumbers ? this.state.modalObj.cuttingPlanCombinationPartNumbers.sort((a,b) => (a.num - b.num)) : []
		let partnumbersObject = {}
		arrPn.map(cppn => {
			partnumbersObject[cppn.partNumber] = cppn
		})
		let numberCombination = (arrPn[0] && arrPn[0].combination) ? arrPn[0].combination.split(";").length : 2
		return <div className='mb-2'>
			<table className='table m-0 table table-grey-border'>
				<thead style={{ backgroundColor: "black", color: "white" }}>
					<tr>
						<th className='table-elem-sm' style={{ width: 10 }}>N°</th>
						<th className='table-elem-sm' style={{ width: 220 }}>Part Number</th>
						<th className='table-elem-sm' style={{ width: 130 }}>Modèle</th>
						<th className='table-elem-sm'>Description</th>
						<th className='table-elem-sm' style={{ width: 220 }}>Kit textil</th>
						{[...Array(numberCombination)].map((e, i) => {
							return <th key={"th-" + i} className='table-elem-sm' style={{ width: 50 }}>{i + 1}</th>
						})}
						<th className='table-elem-sm' style={{ width: 50 }}></th>
					</tr>
				</thead>
				<tbody>
					{arrPn.map((elem, ind) => {
						let arrComb = elem.combination ? elem.combination.split(";") : ["", ""]
						return <tr key={"pn-row-" + ind}>
							<td className='table-elem-sm'>{elem.num}</td>
							<td className='table-elem-sm' style={(elem.partNumber != null && this.state.modalObj.cuttingPlanCombinationPartNumbers.filter(e => e.partNumber == elem.partNumber).length > 1) ? { backgroundColor: "red" } : {}}>

								<AsyncSelect classNamePrefix="rs" cacheOptions defaultOptions
									placeholder={"Part Number ..."}
									isClearable={true}
									value={arrPn[ind].partNumber
										? { label: arrPn[ind].partNumber, value: arrPn[ind].partNumber }
										: null
									}
									loadOptions={(text, callback) => {
										let dep = ["project=" + this.state.modalObj.projet]
										if (this.state.modalObj.version) {
											dep.push("version=" + this.state.modalObj.version)
										}
										dep.push("partNumber=" + text)
										axios.get(`/api/partNumberBoom/list?${dep.join("&")}`)
											.then(res => {
												let arrPn = [], arr = []
												res.data.map(elem => {
													if (!arrPn.includes(elem.partNumber)) {
														arrPn.push(elem.partNumber)
														arr.push(elem)
													}
												})
												callback(arr.filter(e => !this.state.modalObj.cuttingPlanCombinationPartNumbers.map(elemPn => elemPn.partNumber).includes(e.partNumber)).map((elem) => ({ value: elem.partNumber, label: elem.partNumber + " (" + elem.description + ")", item: elem }))
												)
											})
									}}
									onChange={(option) => {
										arrPn[ind].partNumber = option ? option.value : null
										arrPn[ind].modele = option ? option.value.substring(0, 10) : null
										arrPn[ind].description = option ? option.item.description : null
										arrPn[ind].item = option ? option.item.item : null
										arrPn[ind].quantityPer = option ? option.item.quantityPer : null
										this.setState({
											modalObj: { ...this.state.modalObj, cuttingPlanCombinationPartNumbers: [...arrPn], },
										})
									}}
								/>
							</td>
							<td className='table-elem-sm '>
								<input style={{width: "100%"}} 
								// imput need to be smaller
								className='form-control form-control-sm'
									value={elem.modele}
									onChange={(event) => {
										arrPn[ind].modele = event.target.value
										this.setState({
											modalObj: { ...this.state.modalObj, cuttingPlanCombinationPartNumbers: [...arrPn], },
										})
									}}
								/>
							</td>
							<td className='table-elem-sm'>{elem.description}</td>
							<td className='table-elem-sm'>{elem.item}</td>
							{[...Array(numberCombination)].map((e, i) => {
								return <td key={"td-" + i} className='table-elem-sm'
									style={{backgroundColor: arrComb[i] == "1" ? "green" : "white"}}
									onClick={() => {
										let arrComb = elem.combination ? elem.combination.split(";") : ["", ""]
										arrComb[i] = arrComb[i] == "1" ? "0" : "1"
										arrPn[ind].combination = arrComb.join(";")
										this.setState({
											modalObj: {
												...this.state.modalObj,
												cuttingPlanCombinationPartNumbers: [...arrPn]
											}
										})
									}}
								>
									{/* <input value={arrComb[i] || ""} onChange={(e) => {
										// change the total value which is the sum of all combinations
										let arrComb = elem.combination ? elem.combination.split(";") : ["", ""]
										arrComb[i] = e.target.value
										arrPn[ind].combination = arrComb.join(";")
										this.setState({
											modalObj: {
												...this.state.modalObj,
												cuttingPlanCombinationPartNumbers: [...arrPn]
											}
										})
									}} /> */}
								</td>
							})}

							<td className='table-elem-sm '>
								<div className='d-flex'>
									<button className='btn btn-outline-dark' onClick={() => {
										if(window.confirm("Are you sure you want to delete this row ?")) {
											arrPn.splice(ind, 1);
											arrPn = arrPn.map((e,index) => {
												e.num = index+1
												return e
											})
											this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanCombinationPartNumbers: [...arrPn] } })
										}
									}}><FontAwesomeIcon icon={faTrashAlt} /></button>
									<div>
										<button className='btn btn-outline-dark' 
										style={{fontSize: 10, padding: "2 12"}}
										onClick={() => {
											//change the num property between this row and the previous one
											let arrPn = [...this.state.modalObj.cuttingPlanCombinationPartNumbers]
											let temp = arrPn[ind].num
											arrPn[ind].num = arrPn[ind - 1].num
											arrPn[ind - 1].num = temp
											this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanCombinationPartNumbers: [...arrPn] } })
										}}><FontAwesomeIcon icon={faArrowUp} /></button>
										<button className='btn btn-outline-dark' 
										style={{fontSize: 10, padding: "2 12"}}
										onClick={() => {
											//change the num property between this row and the next one
											let arrPn = [...this.state.modalObj.cuttingPlanCombinationPartNumbers]
											let temp = arrPn[ind].num
											arrPn[ind].num = arrPn[ind + 1].num
											arrPn[ind + 1].num = temp
											this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanCombinationPartNumbers: [...arrPn] } })
										}}><FontAwesomeIcon icon={faArrowDown} /></button>
									</div>
									
								</div>
							</td>
						</tr>
					})}
					{/*  */}
				</tbody>
			</table>
			<div className='d-flex'>
				<button
					className='btn btn-outline-danger'
					onClick={() => {
						arrPn.push({})
						arrPn = arrPn.map((e,index) => {
							e.num = index+1
							return e
						})
						this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanCombinationPartNumbers: [...arrPn] } })
					}}
				>
					<FontAwesomeIcon icon={faPlus} />
				</button>
				<input type='number' style={{ width: 50 }}
					value={numberCombination}
					onChange={(e) => {
						if (/^\d*$/.test(e.target.value)) {
							if (e.target.value == "") e.target.value = 2
							e.target.value = Math.min(Math.max(1, e.target.value), 9)
							let initArr = []
							for (let i = 0; i < parseInt(e.target.value); i++) {
								initArr.push("")
							}
							for (let j = 0; j < arrPn.length; j++) {
								let arrComb = arrPn[j].combination ? arrPn[j].combination.split(";") : null
								if (arrComb) {
									// i want to change arrComb to have parseInt(e.target.value) elements in it, fill with empty stringif higher or delete some if lower
									if (arrComb.length < parseInt(e.target.value)) {
										for (let i = 0; i < (parseInt(e.target.value) - arrComb.length); i++) {
											arrComb.push("")
										}
									} else if (arrComb.length > parseInt(e.target.value)) {
										arrComb = arrComb.slice(0, parseInt(e.target.value))

									}
									arrPn[j].combination = arrComb.join(";")
									//update the total value
									let total = 0
									arrComb.map(num => {
										if (num.trim().length > 0 && /^\d*$/.test(num)) {
											total += parseInt(num)
										}
									})
									arrPn[j].total = total
								} else {
									arrPn[j].combination = initArr.join(";")
									arrPn[j].total = 0
								}


							}
							this.setState({ numberCombination: parseInt(e.target.value) })
						}
					}}
				/>
			</div>
		</div>
	}

	getMarge = (longueur, nbrCouche, partNumberMaterial) => {
		let marge = 0
		this.state.partNumberMaterialConfigs[partNumberMaterial].reftissuMargins.map(cf => {
			if (longueur >= cf.longueurMin && longueur <= cf.longueurMax) {
				let arr = cf.pliesConfig.split("|").map(e => e.split(";").map(numb => parseFloat(numb))).sort((a, b) => a[0] - b[0])
				for (let i = 0; i < arr.length; i++) {
					if (nbrCouche >= arr[i][0]) {
						marge = arr[i][1]
					}
				}
			}
		})
		return marge
	}



	checkObj = () => {
		let placements = [], placementNbrCouche = {}, error = [], partNumbers = [], placementActivated = []
		this.state.modalObj.cuttingPlanCombinationPartNumbers.map((cppn, ind) => {
			if (cppn.partNumber == null) {
				error.push("Un part number est vide")
			} else if (partNumbers.includes(cppn.partNumber)) {
				error.push("Le Part Number " + cppn.partNumber + " est dupliqué")
			} else {
				partNumbers.push(cppn.partNumber)
			}
		})



		if (error.length == 0) {
			this.setState({ loading: true })
			axios.post(`/api/cuttingPlanCombination`, this.state.modalObj)
				.then(res => {
					this.props.goBack()
				})
		} else {
			this.setState({ error: error })
		}

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

	convertToMachine = (machineId) => {
		switch (machineId) {
			case "1":
				return "Lectra"
			case "2":
				return "Gerber"
			case "3":
				return "DIE"
			case "4":
				return "LASER-LSR"
			case "5":
				return "Lectra IP6"
			case "6":
				return "LASER-DXF"
			default:
				return null
		}
	}

	getCR = (cp, arrWO) => {
		let objCR = {
			cuttingPlanId: cp.id,
			projet: cp.projet,
			version: cp.version,
			modele: cp.description,
			definition: cp.definition,
			shift: null,
			planningDate: null,
			cuttingRequestPartNumbers: [],
			cuttingRequestSeries: [],
			planningList: []
		}
		cp.cuttingPlanCombinationPartNumbers.map(elem => {
			let woObj = arrWO.find(e => e.item == elem.item)
			objCR.cuttingRequestPartNumbers.push({
				partNumber: elem.partNumber,
				description: elem.description,
				item: elem.item,
				quantity: elem.quantity,
				wo: woObj ? woObj.wo : null,
				woid: woObj ? woObj.woid : null,
			})
		})

		let arrPlacement = []

		if (cp && cp.cuttingPlanMaterials) {
			try {
				cp.cuttingPlanMaterials.map(cpm => {
					cpm.cuttingPlanMaterialPlacement.sort((a, b) => a.partNumberMaterial - b.partNumberMaterial || Number(b.activated) - Number(a.activated)).map(cpmp => {
						let arrDrill = (cpmp.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
						let maxPlie = arrDrill[0] == null && arrDrill[1] == null ? cpmp.maxPlie : cpmp.maxPlieDrill
						let nbrCouche = cpmp.nbrCouche
						while (nbrCouche > 0) {
							let couche = Math.min(maxPlie, nbrCouche)
							let config = null;
							if (cpmp.pliesConfig != null) {
								let configArr = cpmp.pliesConfig.split("|").map(e => { return [parseInt(e.split(";")[0]), e.split(";")[1]] }).sort((a, b) => a[0] - b[0])
								for (let i = 0; i < configArr.length; i++) {
									if (configArr[i][0] <= cpmp.nbrCouche) {
										config = configArr[i][1]
									}
								}
							}

							let obj = {
								partNumberMaterial: cpm.partNumberMaterial,
								description: cpm.description,
								matelassageEndroit: cpm.matelassageEndroit,
								partNumbers: cpmp.partNumbers,
								longueur: this.convertFloat((cpmp.longueur + this.getMarge(cpmp.longueur, couche, cpm.partNumberMaterial)), 3),
								nbrCouche: couche,
								laize: cpmp.laize,// en attent
								maxPlie: cpmp.maxPlie,
								maxPlieDrill: cpmp.maxPlieDrill,
								maxDrill: cpmp.maxDrill,
								placement: cpmp.placement,
								config: config,
								drill: arrDrill.join(","),
								machine: cpmp.machine,
								groupPlacement: cpmp.groupPlacement,
								activated: cpmp.activated
							}
							arrPlacement.push(obj)
							nbrCouche -= couche
						}
					})
				})
			} catch (err) {
				console.log({ err })
			}
		}

		objCR.cuttingRequestSeries = arrPlacement

		if (this.state.optionsList.projet != null && this.state.optionsList.projet.length > 0) {
			let projet = this.state.optionsList.projet.find(elem => elem.item.nom === objCR.projet)
			if (projet != null) {
				objCR.zone = { ...projet.item.zone }
			}
		}

		return objCR
	}




	convertFloat = (float, digit) => {
		//check if the float is a number
		if (!float || isNaN(float)) {
			return float
		}
		return parseFloat(parseFloat(float).toFixed(digit))
	}

	converter = (number, arr) => {
		let newNumber = number;
		arr.map(e => {
			if (e.plieOld === number) {
				newNumber = e.plieNew
			}
		})
		return newNumber
	}

	render() {
		const entity = "cuttingPlanCombination"
		const { entityId } = this.props
		return (
			<div>
				<div className='entityform-header'
					style={this.state.modalObj.type === "Special Plan" ? { backgroundColor: "#fff0cc" } : {}}
				>
					<h1 className='entityform-title'
					>{metadata[entity].displayName} {this.state.modalObj.id} {this.state.modalObj.copyId && `(Copie de ${this.state.modalObj.copyId})`}</h1>
					<div className='entityform-buttons'>
						<button type="button" className='btn btn-danger ml-2' disabled={this.state.loading}
							onClick={() => {
								console.log({ modalObj: this.state.modalObj })
								this.checkObj()
							}}
						>
							<FontAwesomeIcon icon={faFloppyDisk} /> Enregistrer
						</button>
						<button className='btn btn-link' onClick={() => { this.props.goBack() }}>Annuler</button>
					</div>
				</div>
				<div className='entityform-container'>
					{this.renderForm(entityId)}

					{this.renderPnTable()}

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

			</div>
		)
	}
}
