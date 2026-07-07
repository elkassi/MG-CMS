import { faAngleDown, faAngleLeft, faAngleRight, faAngleUp, faArrowLeft, faArrowRight, faArrowUp, faCheck, faClock, faEye, faFloppyDisk, faInfo, faPlus, faPrint, faRefresh, faRightLeft, faSearch, faSpinner, faTimes, faTrashAlt } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import axios from 'axios'
import React, { Component } from 'react'
import ReactDOM from 'react-dom'
import { allColors, metadata, optionsMatelassageEndroit } from '../../metadata'
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
import { BarChart, Bar, Rectangle, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

import logo from '../../assets/images/lear_logo.png'

export default class CuttingPlanForm extends Component {

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
			loadingConfirmer: false,
			selectedBoxs: [],
			ctcData: {},
			digits: [],
			drillEmpArr: {},
			searchText: null,
			longueurInf: 0.5,
			nbrCoucheSup: 20,
			allowedToEdit: false,
			loadingConsommation: false,
			showDrillModal: false,
			drillModalData: {},
			partNumberCorrespondances: [],
			viewMode: false,
			placementsInfoOptional: null,
			placementMeta: {},

		}
		this.inputArr = []
	}

	componentDidMount() {
		const { entityId } = this.props
		if (entityId) {
			this.setState({ viewMode: true })
			axios.get(`/api/cuttingPlan/${entityId}`)
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
							})
						let arrPn = this.state.modalObj.cuttingPlanPartNumbers || []
						let arrBoom = []
						let cuttingPlanMaterialsList = this.state.modalObj.cuttingPlanMaterials.map(e => {
							e.qadUsage = 0
							return e
						}) || []
						this.loadSimilarPlan(arrPn.map(pn => pn.partNumber).filter(e => e != null && e.length > 0))
						Promise.all(arrPn.map((elemPn, indPn) => {
							let dep = ["project=" + this.state.modalObj.projet]
							if (this.state.modalObj.version) { dep.push("version=" + this.state.modalObj.version) }
							dep.push("partNumber=" + elemPn.partNumber)
							dep.push("item=" + elemPn.item)
							return axios.get(`/api/partNumberBoom/list?${dep.join("&")}`)
						}))
							.then(res => {
								arrPn.map((elemPn, indPn) => {
									arrBoom = [...arrBoom, ...res[indPn].data]
									let arrReftissuOption = res[indPn].data.filter(e => e.partNumber === elemPn.partNumber)
									arrReftissuOption.map((option) => {
										let i = cuttingPlanMaterialsList.findIndex(e => e.partNumberMaterial === option.partNumberMaterial)
										if (i >= 0) {

											if (cuttingPlanMaterialsList[i].partNumbers != null && !cuttingPlanMaterialsList[i].partNumbers.split(",").includes(elemPn.partNumber)) {
												cuttingPlanMaterialsList[i].partNumbers += "," + elemPn.partNumber
											}
											if (cuttingPlanMaterialsList[i].partNumbers == null) {
												cuttingPlanMaterialsList[i].partNumbers = elemPn.partNumber
											}
											if (cuttingPlanMaterialsList[i].qadUsage == null) {
												cuttingPlanMaterialsList[i].qadUsage = elemPn.quantity && option.quantityPer ? option.quantityPer * elemPn.quantity : (elemPn.qadUsage || 0)
											} else {
												cuttingPlanMaterialsList[i].qadUsage += elemPn.quantity && option.quantityPer ? option.quantityPer * elemPn.quantity : (elemPn.qadUsage || 0)
											}
										} else {
											cuttingPlanMaterialsList.push({
												partNumberMaterial: option.partNumberMaterial,
												description: option.partNumberMaterialDescription,
												partNumbers: option.partNumber,
												qadUsage: elemPn.quantity && option.quantityPer ? option.quantityPer * elemPn.quantity : (elemPn.qadUsage || 0),

												cuttingPlanMaterialPlacement: [{ groupPlacement: 1, activated: true }]
											})
										}
									})
								})
							})
							.then(() => {
								let obj = {}
								axios.get(`/api/partNumberMaterialConfig/pns/${cuttingPlanMaterialsList.map(e => e.partNumberMaterial).join(",")}?projet=${this.state.modalObj.projet}`)
									.then(res => {
										res.data.map(e => {
											obj[e.partNumberMaterial] = { ...e }
										})
										// if (!arrPn.map(pn=>pn.partNumber).includes(elem.partNumber)) {
										// 	arrPn.push(elem)
										// }
										// if (this.state.modalObj.cuttingPlanPartNumbers.map(e => e.partNumber).includes(elem.partNumber)
										// 	&& !cuttingPlanMaterials.map(e => e.partNumberMaterial).includes(elem.partNumberMaterial)) {

										// }
										cuttingPlanMaterialsList = [...cuttingPlanMaterialsList].map(e => {
											if (obj[e.partNumberMaterial]) {
												e.tauxScrap = obj[e.partNumberMaterial].tauxScrap
												e.rotation = obj[e.partNumberMaterial].rotation
												e.plaque = obj[e.partNumberMaterial].plaque
												e.vitesse = obj[e.partNumberMaterial].vitesse
												e.matelassageEndroit = obj[e.partNumberMaterial].matelassageEndroit
												e.description = obj[e.partNumberMaterial].description

												// Update maxPlie, maxPlieDrill, and maxDrill for ALL placements based on their machine
												e.cuttingPlanMaterialPlacement.map((placement, pIdx) => {
													let machine = null
													if (placement.machine) {
														machine = obj[e.partNumberMaterial].reftissuMachines.find(cm => cm.machineType === placement.machine)
													}
													if (!machine) {
														machine = obj[e.partNumberMaterial].reftissuMachines.find(cm => cm.defaultValue === true)
													}
													if (machine != null) {
														if (placement.machine == null) {
															placement.machine = machine.machineType
														}
														placement.maxPlie = machine.convertedMaxPlie || machine.maxPlie
														placement.maxPlieDrill = machine.convertedMaxPlieDrill || machine.maxPlieDrill
														placement.maxDrill = machine.maxDrill
														if (placement.pliesConfig == null) {
															placement.pliesConfig = machine.pliesConfig
														}
													}
													let category = obj[e.partNumberMaterial].reftissuCategories.find(cm => cm.defaultValue === true)
													if (category != null && placement.category == null) {
														placement.category = category.category
														placement.laize = category.borneMin
													}
													return placement
												})
											}
											return e;
										})

									})
									.finally(() => {
										this.setState({
											modalObj: { ...this.state.modalObj, cuttingPlanPartNumbers: [...arrPn], cuttingPlanMaterials: cuttingPlanMaterialsList },
											partNumberMaterialConfigs: { ...obj },
											optionsList: { ...this.state.optionsList, partNumberBoom: arrBoom.map(e => { return { label: e.partNumber, value: e.partNumber, item: e } }) }
										})
										// Load part number correspondances
										let allPartNumbers = arrPn.map(pn => pn.partNumber).filter(e => e != null && e.length > 0)
										if (allPartNumbers.length > 0) {
											axios.get(`/api/partNumberCorrespendance/byPartNumbers?partNumbers=${allPartNumbers.join(",")}`)
												.then(res => {
													this.setState({ partNumberCorrespondances: res.data })
												})
										}
									})
							})
					}

				})
		}

		axios.get("/api/reftissuProperty/list?reftissu=CAD")
			.then((res) => {
				res.data.map(e => {
					if (e.property === "longueurInf") {
						this.setState({ longueurInf: e.value })
					}
					if (e.property === "nbrCoucheSup") {
						this.setState({ nbrCoucheSup: e.value })
					}
				})
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
		axios.get(`/api/cuttingSpeed/list`)
			.then((res) => {
				this.setState({
					optionsList: {
						...this.state.optionsList,
						cuttingSpeed: res.data.map((elem) => ({ value: elem.config, label: elem.config, item: elem }))
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
		axios.get(`/api/partNumberMaterialConfig/pns/${this.state.modalObj.cuttingPlanMaterials.map(e => e.partNumberMaterial).join(",")}?projet=${this.state.modalObj.projet}`)
			.then(res => {
				let obj = {}
				res.data.map(e => {
					obj[e.partNumberMaterial] = { ...e }
				})
				let cuttingPlanMaterials = [...this.state.modalObj.cuttingPlanMaterials].map(e => {
					if (obj[e.partNumberMaterial]) {
						e.vitesse = obj[e.partNumberMaterial].vitesse
						e.tauxScrap = obj[e.partNumberMaterial].tauxScrap
						e.rotation = obj[e.partNumberMaterial].rotation
						e.plaque = obj[e.partNumberMaterial].plaque
						e.matelassageEndroit = obj[e.partNumberMaterial].matelassageEndroit
						e.description = obj[e.partNumberMaterial].description

						// Update maxPlie, maxPlieDrill, and maxDrill with converted values for all placements
						e.cuttingPlanMaterialPlacement.map(placement => {
							let machine = obj[e.partNumberMaterial].reftissuMachines.find(cm => cm.defaultValue === true)
							if (machine != null) {
								// Update with converted values from backend
								placement.maxPlie = machine.convertedMaxPlie || machine.maxPlie
								placement.maxPlieDrill = machine.convertedMaxPlieDrill || machine.maxPlieDrill
								placement.maxDrill = machine.maxDrill
							}
							return placement
						})
					}
					return e
				})
				this.setState({
					modalObj: { ...this.state.modalObj, cuttingPlanMaterials: cuttingPlanMaterials },
					partNumberMaterialConfigs: { ...obj },
				})
			})
	}

	renderReftissuModal = () => {
		let configObj = {}
		if (this.state.reftissuConfig) {
			configObj = { ...this.state.partNumberMaterialConfigs[this.state.reftissuConfig] }
		}
		return <Modal
			show={this.state.reftissuConfig != null}
			onHide={() => this.setState({ reftissuConfig: null })}
			dialogClassName="modal-75w"
			centered
		>
			{this.state.reftissuConfig && <div style={{ maxHeight: "80vh", overflowY: 'auto' }}>
				<h4 className='text-center my-2'>{this.state.reftissuConfig}</h4>
				<div className='row mx-2'>
					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>PN Material : </strong></div>
					<div className='col-4'>{configObj.partNumberMaterial}</div>
					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>Description : </strong></div>
					<div className='col-4'>{configObj.description}</div>

					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>Vitesse : </strong></div>
					<div className='col-4'>{configObj.vitesse}</div>
					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>Rotation : </strong></div>
					<div className='col-4'>{configObj.rotation}</div>

					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>Plaque : </strong></div>
					<div className='col-4'>{configObj.plaque}</div>
					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>Taux Scrap : </strong></div>
					<div className='col-4'>{configObj.tauxScrap}</div>

					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>Matelassage Endroit : </strong></div>
					<div className='col-4'>{configObj.matelassageEndroit}</div>
					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>Commentaire : </strong></div>
					<div className='col-4'>{configObj.commentaire}</div>

					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>Marge Laize Min (cm) : </strong></div>
					<div className='col-4'>{configObj.margeLaizeMin}</div>
					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>Marge Laize Max (cm) : </strong></div>
					<div className='col-4'>{configObj.margeLaizeMax}</div>
					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>Validated 0BF : </strong></div>
					<div className='col-4'>{configObj.validated0BF === true ? <FontAwesomeIcon icon={faCheck} color="green" /> : <FontAwesomeIcon icon={faTimes} color="red" />}</div>
					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>Validated IP6 : </strong></div>
					<div className='col-4'>{configObj.validatedIP6 === true ? <FontAwesomeIcon icon={faCheck} color="green" /> : <FontAwesomeIcon icon={faTimes} color="red" />}</div>

					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>Buffer 1 IP6 : </strong></div>
					<div className='col-4'>{configObj.buffer1IP6}</div>
					<div className='col-2 text-right' style={{ whiteSpace: "nowrap" }}><strong>Buffer 2 IP6 : </strong></div>
					<div className='col-4'>{configObj.buffer2IP6}</div>

				</div>
				<table className='table table-bordered'>
					<thead>
						<tr>
							<th className='table-elem-sm'>Type Machine</th>
							<th className='table-elem-sm'>Max Plie</th>
							<th className='table-elem-sm'>Max Plie Drill</th>
							<th className='table-elem-sm'>Max Drill</th>
							<th className='table-elem-sm'>Defaut</th>
							<th className='table-elem-sm'>Min Plie</th>
							<th className='table-elem-sm'>Config</th>
						</tr>
					</thead>
					<tbody>
						{configObj.reftissuMachines.map(elem => {
							let arrConfigs = elem.pliesConfig ? elem.pliesConfig.split("|").map(e => e.split(";")) : []
							return arrConfigs.map((elemConfig, indConfig) => <tr>
								{indConfig === 0 && <td className='table-elem-sm' rowSpan={arrConfigs.length}>{elem.machineType}</td>}
								{indConfig === 0 && <td className='table-elem-sm' rowSpan={arrConfigs.length}>{elem.maxPlie}</td>}
								{indConfig === 0 && <td className='table-elem-sm' rowSpan={arrConfigs.length}>{elem.maxPlieDrill}</td>}
								{indConfig === 0 && <td className='table-elem-sm' rowSpan={arrConfigs.length}>{elem.maxDrill}</td>}
								{indConfig === 0 && <td className='table-elem-sm' rowSpan={arrConfigs.length}>
									{elem.defaultValue ? <FontAwesomeIcon icon={faCheck} color="green" /> : ""}
								</td>}
								<td className='table-elem-sm'>{elemConfig[0]}</td>
								<td className='table-elem-sm'>{elemConfig[1]}</td>
							</tr>)
						})}
					</tbody>
				</table>
				<table className='table table-bordered'>
					<thead>
						<tr>
							<th className='table-elem-sm'>Category</th>
							<th className='table-elem-sm'>Description</th>
							<th className='table-elem-sm'>borneMin</th>
							<th className='table-elem-sm'>borneMax</th>
							<th className='table-elem-sm'>Defaut</th>
						</tr>
					</thead>
					<tbody>
						{configObj.reftissuCategories.map(elem => {
							return <tr>
								<td className='table-elem-sm'>{elem.category}</td>
								<td className='table-elem-sm'>{elem.description}</td>
								<td className='table-elem-sm'>{elem.borneMin}</td>
								<td className='table-elem-sm'>{elem.borneMax}</td>
								<td className='table-elem-sm'>{elem.defaultValue ? <FontAwesomeIcon icon={faCheck} color="green" /> : ""}</td>
							</tr>
						})}
					</tbody>
				</table>
				<table className='table table-bordered'>
					<thead>
						<tr>
							<th className='table-elem-sm'>Machine</th>
							<th className='table-elem-sm'>Longueur Min	</th>
							<th className='table-elem-sm'>Longueur Max</th>

							<th className='table-elem-sm'>Min Plie</th>
							<th className='table-elem-sm'>Marge</th>
						</tr>
					</thead>
					<tbody>
						{configObj.reftissuMargins.map((elem, indLg) => {
							let arrMarges = elem.pliesConfig.split("|").map(e => e.split(";"))
							console.log({ arrMarges, length: arrMarges.length, })
							return arrMarges.map((e, indMarge) => <tr>
								{indMarge === 0 && <td className='table-elem-sm' rowSpan={arrMarges.length}>{elem.machine || '-'}</td>}
								{indMarge === 0 && <td className='table-elem-sm' rowSpan={arrMarges.length}>{elem.longueurMin}</td>}
								{indMarge === 0 && <td className='table-elem-sm' rowSpan={arrMarges.length}>{elem.longueurMax}</td>}
								<td className='table-elem-sm'>{e[0]}</td>
								<td className='table-elem-sm'>{e[1]}</td>
							</tr>)
						})}
					</tbody>
				</table>
			</div>}
		</Modal>
	}

	renderConfirmModal = () => {
		let arrTable = [], arrPn = {}, arrDrillPlacement = {}, erreurDrills = [], alertMessages = [], responsableList = []
		let badPlan = false
		if (this.state.placementsInfo) {
			this.state.modalObj.cuttingPlanPartNumbers.map(elem => {
				arrPn[elem.partNumber] = elem.quantity
			})
			this.state.modalObj.cuttingPlanMaterials.map(elem => {
				elem.cuttingPlanMaterialPlacement.map(elemPc => {
					let arrDrill = (elemPc.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
					arrDrillPlacement[elemPc.placement] = arrDrill
				})
			})
			const sortedKeys = Object.keys(this.state.placementsInfo).sort((a, b) => a.localeCompare(b));
			sortedKeys.map(partNumber => {
				if (this.state.placementsInfo[partNumber] != null) {
					let arrPanelNumber = []
					let i = 0
					const sortedDigitKeys = Object.keys(this.state.placementsInfo[partNumber]).sort((a, b) => a.localeCompare(b));

					sortedDigitKeys.map(digit => {
						// put in pettern the content of digit without the last 4 letters
						let pattern = digit.slice(0, digit.length - 4);
						if (pattern.endsWith("-LSR")) {
							pattern = pattern.slice(0, pattern.length - 4);
						}
						pattern = pattern.trim().toUpperCase(); // Normalize to uppercase for consistent comparison
						let obj = null
						if (this.state.ctcData[partNumber]) {
							obj = this.state.ctcData[partNumber].find(elem => elem.pattern && elem.pattern.trim().toUpperCase() === pattern && arrPanelNumber.includes(elem.panelNumber) === false)
							if (obj) {
								arrPanelNumber.push(obj.panelNumber)
							}
						}
						let digitObj = this.state.digits.find(elem => elem.pattern && elem.pattern.trim().toUpperCase() === pattern)
						let erreurDrill1 = false, erreurDrill2 = false
						this.state.placementsInfo[partNumber][digit].placements.map(placement => {
							if (this.state.drillEmpArr[pattern]) {
								let toleranceDrill = 1
								if (obj && obj.toleranceDrill != null) {
									toleranceDrill = obj.toleranceDrill
								}
								if (this.state.drillEmpArr[pattern].drill1 > 0 &&
									(arrDrillPlacement[placement][0] == null
										|| this.state.drillEmpArr[pattern].drill1 + toleranceDrill < (arrDrillPlacement[placement][0])
										|| this.state.drillEmpArr[pattern].drill1 - toleranceDrill > parseInt(arrDrillPlacement[placement][0]))
								) {
									erreurDrill1 = true
									erreurDrills.push({ pn: partNumber, pattern: pattern, drill: 1 })
								}
								if (this.state.drillEmpArr[pattern].drill2 > 0 && (arrDrillPlacement[placement][1] == null || this.state.drillEmpArr[pattern].drill2 + toleranceDrill < (arrDrillPlacement[placement][1]) || this.state.drillEmpArr[pattern].drill2 - toleranceDrill > parseInt(arrDrillPlacement[placement][1]))) {
									erreurDrill2 = true
									erreurDrills.push({ pn: partNumber, pattern: pattern, drill: 2 })
								}
							}
						})

						if ((this.state.placementsInfo[partNumber][digit].counterDrill1 === 0 && this.state.drillEmpArr[pattern] && this.state.drillEmpArr[pattern].drill1 > 0)
							|| (this.state.placementsInfo[partNumber][digit].counterDrill1 > 0 && !(this.state.drillEmpArr[pattern] && this.state.drillEmpArr[pattern].drill1 > 0))) {
							erreurDrill1 = true
							erreurDrills.push({ pn: partNumber, pattern: pattern, drill: 1 })
						}
						if ((this.state.placementsInfo[partNumber][digit].counterDrill2 === 0 && this.state.drillEmpArr[pattern] && this.state.drillEmpArr[pattern].drill2 > 0)
							|| (this.state.placementsInfo[partNumber][digit].counterDrill2 > 0 && !(this.state.drillEmpArr[pattern] && this.state.drillEmpArr[pattern].drill2 > 0))) {
							erreurDrill2 = true
							erreurDrills.push({ pn: partNumber, pattern: pattern, drill: 2 })
						}
						if (obj && obj.partNumberMaterial && this.state.placementsInfo[partNumber][digit].reftissu
							&& obj.partNumberMaterial.trim().toUpperCase() !== this.state.placementsInfo[partNumber][digit].reftissu.join("/").trim().toUpperCase()) {
							// Check if a correspondance exists that maps between the materials
							let hasCorrespondance = this.state.partNumberCorrespondances.some(corr =>
								corr.partNumber === partNumber
								&& corr.pattern && pattern
								&& (corr.pattern.trim().toUpperCase() === pattern
									|| (corr.patternCorrespondance && corr.patternCorrespondance.trim().toUpperCase() === pattern))
							)
							if (!hasCorrespondance) {
								badPlan = true
								alertMessages.push("Part Number " + partNumber + " : " + digit + " : " + obj.partNumberMaterial.trim().toUpperCase() + " != " + this.state.placementsInfo[partNumber][digit].reftissu.join("/").trim().toUpperCase())
								if (!responsableList.includes(obj.updatedBy)) {
									responsableList.push(obj.updatedBy)
								}
							}
						}
						if (!(obj && obj.quantity) && !this.state.ctcData[partNumber]?.find(elem => elem.pattern && elem.pattern.trim().toUpperCase() === pattern)) {
							alertMessages.push("ERR CTC " + partNumber + " : " + pattern)
						}
						// Compute material mismatch flag for row highlighting
						let materialMismatch = false
						if (obj && obj.partNumberMaterial && this.state.placementsInfo[partNumber][digit].reftissu
							&& obj.partNumberMaterial.trim().toUpperCase() !== this.state.placementsInfo[partNumber][digit].reftissu.join("/").trim().toUpperCase()) {
							let hasCorr = this.state.partNumberCorrespondances.some(corr =>
								corr.partNumber === partNumber
								&& corr.pattern && pattern
								&& (corr.pattern.trim().toUpperCase() === pattern
									|| (corr.patternCorrespondance && corr.patternCorrespondance.trim().toUpperCase() === pattern))
							)
							if (!hasCorr) {
								materialMismatch = true
							}
						}
						arrTable.push(
							<tr>
								{i === 0 && <td className='table-elem-sm' rowSpan={Object.keys(this.state.placementsInfo[partNumber]).length}>{partNumber} ({arrPn[partNumber]})</td>}
								<td className='table-elem-sm'
									style={materialMismatch
										? { backgroundColor: "rgb(255, 163, 163)" } : {}}
								>{this.state.placementsInfo[partNumber][digit].reftissu.join("/")}</td>
								<td className='table-elem-sm clickable-element'
									onDoubleClick={() => {
										let arrPlct = this.state.placementsInfo[partNumber][digit].placements
										if (arrPlct.length > 0) {
											window.open("/cutfileviewer/" + arrPlct[0] + "/view", "_blank")
										}
									}}
									style={this.state.placementsInfo[partNumber][digit].placements.length > 1 ? { backgroundColor: "yellow" } : {}}
								><ul className='m-0 p-0' style={{ listStyleType: "none" }}>{this.state.placementsInfo[partNumber][digit].placements && this.state.placementsInfo[partNumber][digit].placements.map(e => {
									if (arrDrillPlacement[e] && !(arrDrillPlacement[e][0] == null && arrDrillPlacement[e][1] == null)) {
										return <li>{e + " (" + arrDrillPlacement[e].join(":") + ")"}</li>
									}
									return <li>{e}</li>
								})}
									</ul></td>
								<td className='table-elem-sm clickable-element'
									onDoubleClick={() => {
										// redirect to /search/358466/view
										if (obj) {
											window.open("/search/" + obj.id + "/view", "_blank")
										}
									}}
								>{digit}</td>
								<td className='table-elem-sm' style={erreurDrill1 ? { backgroundColor: "rgb(255, 163, 163)" } : {}}>
									{this.state.drillEmpArr[pattern] && this.state.drillEmpArr[pattern].drill1}
								</td>
								<td className='table-elem-sm' style={erreurDrill2 ? { backgroundColor: "rgb(255, 163, 163)" } : {}}>
									{this.state.drillEmpArr[pattern] && this.state.drillEmpArr[pattern].drill2}
								</td>
								<td className='table-elem-sm'
									style={this.state.placementsInfo[partNumber][digit].graphNumber > 1 ? { backgroundColor: "yellow" } : {}}
								>{this.state.placementsInfo[partNumber][digit].graphNumber > 0 && this.state.placementsInfo[partNumber][digit].graphNumber - 1}</td>

								<td className='table-elem-sm'
									style={this.state.placementsInfo[partNumber][digit].quantity % arrPn[partNumber] === 0 ? { backgroundColor: "rgb(157, 255, 140)" } : { backgroundColor: "rgb(255, 163, 163)" }}
								>{this.state.placementsInfo[partNumber][digit].quantity}</td>
								<td className='table-elem-sm'
									style={(obj && parseInt(this.state.placementsInfo[partNumber][digit].quantity / arrPn[partNumber]) !== obj.quantity) ? { backgroundColor: "rgb(255, 163, 163)" } : parseInt(this.state.placementsInfo[partNumber][digit].quantity / arrPn[partNumber]) !== 1 ? { backgroundColor: "yellow" } : {}}
								>{parseInt(this.state.placementsInfo[partNumber][digit].quantity / arrPn[partNumber])}</td>
								<td className='table-elem-sm'
									style={(obj && parseInt(this.state.placementsInfo[partNumber][digit].quantity / arrPn[partNumber]) !== obj.quantity) ? { backgroundColor: "rgb(255, 163, 163)" } : {}}
								>{(obj && obj.quantity)
									? obj.quantity
									: this.state.ctcData[partNumber]?.find(elem => elem.pattern && elem.pattern.trim().toUpperCase() === pattern)
										? <i style={{ color: "red" }}>DUPLICATED</i>
										: <i style={{ color: "red" }}>ERR CTC</i>
									}
								</td>
								<td className='table-elem-sm'>{(obj && obj.panelNumber) ? obj.panelNumber : this.state.ctcData[partNumber]?.find(elem => elem.pattern && elem.pattern.trim().toUpperCase() === pattern) ? <i style={{ color: "red" }}>DUPLICATED</i> : <i style={{ color: "red" }}>ERR CTC</i>}</td>
								<td className='table-elem-sm'
									style={materialMismatch ? { backgroundColor: "rgb(255, 163, 163)" } : {}}
								>{(obj && obj.partNumberMaterial) ? obj.partNumberMaterial : this.state.ctcData[partNumber]?.find(elem => elem.pattern && elem.pattern.trim().toUpperCase() === pattern) ? <i style={{ color: "red" }}>DUPLICATED</i> : <i style={{ color: "red" }}>ERR CTC</i>}</td>
								<td className='table-elem-sm'>
									{((digitObj && digitObj.exist) ? <FontAwesomeIcon icon={faCheck} color="green" /> : <FontAwesomeIcon icon={faTimes} color="red" />)}
								</td>
							</tr>
						)
						i++;
					})
					if (i === Object.keys(this.state.placementsInfo[partNumber]).length) {
						let arrNotFound = this.state.ctcData[partNumber] ? this.state.ctcData[partNumber].filter(elem => !arrPanelNumber.includes(elem.panelNumber)) : []
						if (arrNotFound.length > 0) {
							badPlan = true
							arrNotFound.map((elemNf, ind) => {
								alertMessages.push("Part Number " + partNumber + " : " + elemNf.pattern + " : " + elemNf.panelNumber)
								if (!responsableList.includes(elemNf.updatedBy)) {
									responsableList.push(elemNf.updatedBy)
								}
								let digitObj = this.state.digits.find(elem => elem.pattern && elemNf.pattern && elem.pattern.trim().toUpperCase() === elemNf.pattern.trim().toUpperCase())
								arrTable.push([
									<tr style={{ backgroundColor: "#ff8686" }}>
										{ind === 0 && <td className='table-elem-sm' rowSpan={arrNotFound.length}>{partNumber}</td>}
										<td className='table-elem-sm'></td>
										<td className='table-elem-sm'></td>
										<td className='table-elem-sm'>{elemNf.pattern}</td>
										<td className='table-elem-sm'></td>
										<td className='table-elem-sm'></td>
										<td className='table-elem-sm'></td>
										<td className='table-elem-sm'></td>
										<td className='table-elem-sm'></td>
										<td className='table-elem-sm'>{elemNf.quantity}</td>
										<td className='table-elem-sm'>{elemNf.panelNumber}</td>
										<td className='table-elem-sm'>{elemNf.partNumberMaterial}</td>
										<td className='table-elem-sm'>
											{((digitObj && digitObj.exist)
												? <FontAwesomeIcon icon={faCheck} color="green" />
												: <FontAwesomeIcon icon={faTimes} color="red" />)}
										</td>
									</tr>
								])
							})
						}

					}
				}
			})
		}
		return <Modal
			show={this.state.loading || this.state.loadingTest}
			onHide={() => this.setState({ loading: false, loadingTest: false })}
			dialogClassName="modal-75w"
			centered
		>
			{(this.state.loading || this.state.loadingTest) && <div style={{ maxHeight: "80vh", overflowY: 'auto' }}>
				<h4 className='text-center my-2'>Vérification du plan de coupe</h4>
				<table className='table table-bordered'>
					<thead>
						<tr>
							<th className='table-elem-sm'>PN</th>
							<th className='table-elem-sm'>Material</th>
							<th className='table-elem-sm'>Placement</th>
							<th className='table-elem-sm'>Digit</th>
							<th className='table-elem-sm'>Drill1</th>
							<th className='table-elem-sm'>Drill2</th>
							<th className='table-elem-sm'>Coupe Interne</th>
							<th className='table-elem-sm'>Quantité</th>
							<th className='table-elem-sm'>Quantité/PN</th>
							<th className='table-elem-sm'>CTC</th>
							<th className='table-elem-sm'>Number GT</th>
							<th className='table-elem-sm'>Material GT</th>
							<th className='table-elem-sm'>PLT</th>
						</tr>
					</thead>
					<tbody>
						{this.state.placementsInfo ? arrTable : <tr><td colSpan={30}>Loading ...</td></tr>}
					</tbody>
				</table>

				{/* Optional placement verification: activated=false vs activated=true digits comparison */}
				{this.state.placementsInfoOptional && Object.keys(this.state.placementsInfoOptional).length > 0 && (() => {
					let optionalErrors = []
					let optionalRows = []
					// Build mapping: { "partNumberMaterial|groupPlacement": { activated: Set<digit>, optional: Set<digit> } }
					let groupDigits = {}
					// From activated placements
					if (this.state.placementsInfo && this.state.placementMeta) {
						Object.keys(this.state.placementsInfo).map(pn => {
							Object.keys(this.state.placementsInfo[pn]).map(digitKey => {
								this.state.placementsInfo[pn][digitKey].placements.map(plcmt => {
									let meta = this.state.placementMeta[plcmt]
									if (meta && meta.groupPlacement != null) {
										let key = (meta.partNumberMaterial || "") + "|" + meta.groupPlacement
										if (!groupDigits[key]) groupDigits[key] = { activated: new Set(), optional: new Set(), material: meta.partNumberMaterial, group: meta.groupPlacement }
										let pattern = digitKey.slice(0, digitKey.length - 4)
										if (pattern.endsWith("-LSR")) pattern = pattern.slice(0, pattern.length - 4)
										groupDigits[key].activated.add(pattern.trim().toUpperCase())
									}
								})
							})
						})
					}
					// From optional placements
					if (this.state.placementsInfoOptional && this.state.placementMeta) {
						Object.keys(this.state.placementsInfoOptional).map(pn => {
							Object.keys(this.state.placementsInfoOptional[pn]).map(digitKey => {
								this.state.placementsInfoOptional[pn][digitKey].placements.map(plcmt => {
									let meta = this.state.placementMeta[plcmt]
									if (meta && meta.groupPlacement != null) {
										let key = (meta.partNumberMaterial || "") + "|" + meta.groupPlacement
										if (!groupDigits[key]) groupDigits[key] = { activated: new Set(), optional: new Set(), material: meta.partNumberMaterial, group: meta.groupPlacement }
										let pattern = digitKey.slice(0, digitKey.length - 4)
										if (pattern.endsWith("-LSR")) pattern = pattern.slice(0, pattern.length - 4)
										groupDigits[key].optional.add(pattern.trim().toUpperCase())
									}
								})
							})
						})
					}
					Object.keys(groupDigits).map(key => {
						let g = groupDigits[key]
						if (g.optional.size > 0) {
							let activatedArr = [...g.activated].sort()
							let optionalArr = [...g.optional].sort()
							let missing = activatedArr.filter(d => !g.optional.has(d))
							let surplus = optionalArr.filter(d => !g.activated.has(d))
							let status = (missing.length === 0 && surplus.length === 0) ? "OK" : "ERR"
							if (status === "ERR") {
								badPlan = true
							}
							optionalRows.push(
								<tr key={key} style={status === "ERR" ? { backgroundColor: "#ffdddd" } : { backgroundColor: "#e6ffe6" }}>
									<td className='table-elem-sm'>{g.material}</td>
									<td className='table-elem-sm'>{g.group}</td>
									<td className='table-elem-sm'>{activatedArr.join(", ")}</td>
									<td className='table-elem-sm'>{optionalArr.join(", ")}</td>
									<td className='table-elem-sm' style={missing.length > 0 ? { color: "red", fontWeight: "bold" } : {}}>{missing.length > 0 ? missing.join(", ") : "-"}</td>
									<td className='table-elem-sm' style={surplus.length > 0 ? { color: "red", fontWeight: "bold" } : {}}>{surplus.length > 0 ? surplus.join(", ") : "-"}</td>
									<td className='table-elem-sm'>{status}</td>
								</tr>
							)
							if (missing.length > 0) {
								optionalErrors.push(`${g.material} (Groupe ${g.group}) : digits manquants dans optionnel: ${missing.join(", ")}`)
							}
							if (surplus.length > 0) {
								optionalErrors.push(`${g.material} (Groupe ${g.group}) : digits en surplus dans optionnel: ${surplus.join(", ")}`)
							}
						}
					})
					return optionalRows.length > 0 ? <div>
						<h5 className='mt-3'>Vérification Placements Optionnels (activated=false vs activated=true)</h5>
						<table className='table table-bordered table-sm'>
							<thead><tr>
								<th className='table-elem-sm'>Matière</th>
								<th className='table-elem-sm'>Groupe</th>
								<th className='table-elem-sm'>Digits Activés</th>
								<th className='table-elem-sm'>Digits Optionnels</th>
								<th className='table-elem-sm'>Manquants</th>
								<th className='table-elem-sm'>Surplus</th>
								<th className='table-elem-sm'>Status</th>
							</tr></thead>
							<tbody>{optionalRows}</tbody>
						</table>
						{optionalErrors.length > 0 && <div className='alert alert-danger m-2'>
							{optionalErrors.map((e, i) => <div key={i}>{e}</div>)}
						</div>}
					</div> : null
				})()}

				{alertMessages.length > 0 && <div className='alert alert-warning m-4'>
					{alertMessages.map((elem, ind) => <div key={ind}>{elem}</div>)}
					Reponsable : {responsableList.join(", ")}
				</div>}
				{badPlan && <div className='alert alert-danger m-4'>
					Le plan de coupe contient des erreurs, veuillez vérifier les messages en rouge.
				</div>}
				{erreurDrills.length > 0
					? <div className='alert alert-danger m-4'>
						{erreurDrills.map((elem, ind) => <div key={ind}>Erreur de drill sur le PN {elem.pn} pour le pattern {elem.pattern} et le drill {elem.drill}</div>)}
					</div>
					: <div style={{ height: "55px" }}>
						{this.state.placementsInfo && !this.state.loadingConfirmer && !this.state.loadingTest
							&& <button className='btn btn-success float-right'
								style={{ margin: "0 15px 20px 0" }}
								onClick={() => {
									this.submitPlanCMS(badPlan, alertMessages, arrTable, responsableList)
								}}>
								Confirmer
							</button>}
						{this.state.loadingConfirmer && <div className="float-right" style={{ margin: "0 15px 20px 0" }}>
							<FontAwesomeIcon icon={faSpinner} spin size='2x' />
						</div>}
						{/* {this.state.placementsInfo && !this.state.loadingConfirmer ? (!this.state.loadingTest &&
							<button className='btn btn-success float-right'
								style={{ margin: "0 15px 20px 0" }}
								onClick={() => {
									this.setState({ loadingConfirmer: true })
									axios.post(`/api/cuttingPlan`, { ...this.state.modalObj, changes: JSON.stringify(this.state.modalObj, null, 2) })
										.then(res => {
											this.setState({ loadingConfirmer: false })
											this.props.goBack()
										})
										.catch(err => {
											this.setState({ error: err.response.data, loadingConfirmer: false })
										})

								}}>
								Confirmer
							</button>) : <FontAwesomeIcon icon={faSpinner} spin size='2x' />} */}
					</div>}
			</div>}
		</Modal>
	}

	submitPlanCMS = async (badPlan, alertMessages, arrTable, responsableList) => {
		try {
			this.setState({ loadingConfirmer: true });

			let res = await axios.post(`/api/cuttingPlan/cms`, {
				...this.state.modalObj,
				changes: JSON.stringify(this.state.modalObj, null, 2),
				// enabled: badPlan ? false : this.state.modalObj.enabled,
				alertMessages: (alertMessages && alertMessages.length > 0)
					? (alertMessages.join(", ").length > 254
						? alertMessages.join(", ").substring(0, 254)
						: alertMessages.join(", "))
					: null,
			});
			if (badPlan) {
				let content = <div>
					<h4 className='text-center my-2'>Vérification du plan de coupe</h4>
					<table className='table table-bordered'>
						<thead>
							<tr>
								<th className='table-elem-sm'>PN</th>
								<th className='table-elem-sm'>Material</th>
								<th className='table-elem-sm'>Placement</th>
								<th className='table-elem-sm'>Digit</th>
								<th className='table-elem-sm'>Drill1</th>
								<th className='table-elem-sm'>Drill2</th>
								<th className='table-elem-sm'>Coupe Interne</th>
								<th className='table-elem-sm'>Quantité</th>
								<th className='table-elem-sm'>Quantité/PN</th>
								<th className='table-elem-sm'>CTC</th>
								<th className='table-elem-sm'>Number GT</th>
								<th className='table-elem-sm'>Material GT</th>
								<th className='table-elem-sm'>PLT</th>
							</tr>
						</thead>
						<tbody>
							{this.state.placementsInfo ? arrTable : <tr><td colSpan={30}>Loading ...</td></tr>}
						</tbody>
					</table>
				</div>
				// send alert messages to the user
				const tempDiv = document.createElement('div');
				ReactDOM.render(content, tempDiv);
				const contentHTML = tempDiv.innerHTML;
				await axios.post(`/api/cuttingPlan/alert`, {
					alertMessages,
					content: contentHTML,
					responsableList,
					cuttingPlan: res.data
				});
			}
			this.setState({ loadingConfirmer: false });
			this.props.goBack();
		} catch (err) {
			this.setState({ error: err.response.data, loadingConfirmer: false });
		}
	}

	renderForm = (entityId) => {
		return <div className='row entityform-field'>
			<div className='row entityform-field col-6' ref={ref => this.refForm = ref}>
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
				</div>
				<div className='col-8 p-0'>
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

			<div className='row col-6 py-2'>
				<div className='col-4 text-right'>
					Activation par :
				</div>
				<div className='col-8 p-0'>
					{this.state.modalObj.enabledBy && <span>
						{this.state.modalObj.enabledBy.lastName} {this.state.modalObj.enabledBy.firstName} ({this.state.modalObj.enabledBy.matricule}) {this.state.modalObj.enabledAt}
					</span>}
				</div>
			</div>

			<div className='row col-6 py-2'>
				<div className='col-4 text-right'>
					Désactivation par :
				</div>
				<div className='col-8 p-0'>
					{this.state.modalObj.disabledBy && <span>
						{this.state.modalObj.disabledBy.lastName} {this.state.modalObj.disabledBy.firstName} ({this.state.modalObj.disabledBy.matricule}) {this.state.modalObj.disabledAt}
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
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>Type :</label>
				<Select id={"type"} name={"type"} classNamePrefix="rs"
					placeholder={"type..."} className='col-8 p-0'
					isClearable={true}
					value={this.state.modalObj.type
						? { label: this.state.modalObj.type, value: this.state.modalObj.type, item: this.state.modalObj.type }
						: null
					}
					options={[
						{ label: "Normal Plan", value: "Normal Plan" },
						{ label: "Special Plan", value: "Special Plan" }
					]}

					onChange={(option) => {
						this.setState({ modalObj: { ...this.state.modalObj, type: (option ? option.value : null) } })
					}}
				/>
			</div>
		</div>
	}

	renderPnTable = () => {
		let arrPn = this.state.modalObj.cuttingPlanPartNumbers || []
		let partnumbersObject = {}
		arrPn.map(cppn => {
			partnumbersObject[cppn.partNumber] = cppn
		})
		return <div className='mb-2'>
			<table className='table m-0 table table-grey-border'>
				<thead style={{ backgroundColor: "black", color: "white" }}>
					<tr>
						<th className='table-elem-sm' style={{ width: 220 }}>Part Number</th>
						<th className='table-elem-sm'>Description</th>
						<th className='table-elem-sm' style={{ width: 220 }}>Kit textil</th>
						<th className='table-elem-sm' style={{ width: 100 }}>Quantité</th>
						<th className='table-elem-sm'></th>
					</tr>
				</thead>
				<tbody>
					{arrPn.map((elem, ind) => {
						return <tr key={"pn-row-" + ind}>
							<td className='table-elem-sm' style={(elem.partNumber != null && this.state.modalObj.cuttingPlanPartNumbers.filter(e => e.partNumber == elem.partNumber).length > 1) ? { backgroundColor: "red" } : {}}>

								<AsyncSelect classNamePrefix="rs" cacheOptions defaultOptions
									placeholder={"Part Number ..."}
									isClearable={true}
									value={arrPn[ind].partNumber
										? { label: arrPn[ind].partNumber, value: arrPn[ind].partNumber }
										: null
									}
									// options={this.state.optionsList.partNumber 
									// 	? this.state.optionsList.partNumber.filter(e=>!this.state.modalObj.cuttingPlanPartNumbers.map(elemPn => elemPn.partNumber).includes(e.value))
									// 	: []}
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
												callback(arr.filter(e => !this.state.modalObj.cuttingPlanPartNumbers.map(elemPn => elemPn.partNumber).includes(e.partNumber)).map((elem) => ({ value: elem.partNumber, label: elem.partNumber + " (" + elem.description + ")", item: elem }))
												)
											})
									}}
									onChange={(option) => {
										arrPn[ind].partNumber = option ? option.value : null
										arrPn[ind].description = option ? option.item.description : null
										arrPn[ind].item = option ? option.item.item : null
										arrPn[ind].quantityPer = option ? option.item.quantityPer : null

										let arrBoom = []
										let cuttingPlanMaterialsList = []

										let objPlacements = {}
										if (this.state.modalObj.cuttingPlanMaterials) {
											this.state.modalObj.cuttingPlanMaterials.map(elem => {
												objPlacements[elem.partNumberMaterial] = elem.cuttingPlanMaterialPlacement
											})
										}

										Promise.all(arrPn.map((elemPn, indPn) => {
											let dep = ["project=" + this.state.modalObj.projet]
											if (this.state.modalObj.version) { dep.push("version=" + this.state.modalObj.version) }
											dep.push("partNumber=" + elemPn.partNumber)
											dep.push("item=" + elemPn.item)
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
																// description: option.partNumberMaterialDescription,
																partNumbers: option.partNumber,
																cuttingPlanMaterialPlacement: objPlacements[option.partNumberMaterial] || [{ groupPlacement: 1, activated: true }]
															})
														}
													})
												})
											})
											.then(() => {
												let obj = {}
												axios.get(`/api/partNumberMaterialConfig/pns/${cuttingPlanMaterialsList.map(e => e.partNumberMaterial).join(",")}?projet=${this.state.modalObj.projet}`)
													.then(res => {
														res.data.map(e => {
															obj[e.partNumberMaterial] = { ...e }
														})
														cuttingPlanMaterialsList = [...cuttingPlanMaterialsList].map(e => {
															if (obj[e.partNumberMaterial]) {
																e.tauxScrap = obj[e.partNumberMaterial].tauxScrap
																e.rotation = obj[e.partNumberMaterial].rotation
																e.plaque = obj[e.partNumberMaterial].plaque
																e.vitesse = obj[e.partNumberMaterial].vitesse
																e.matelassageEndroit = obj[e.partNumberMaterial].matelassageEndroit
																e.description = obj[e.partNumberMaterial].description
																if (objPlacements[e.partNumberMaterial]) {
																	e.cuttingPlanMaterialPlacement = objPlacements[e.partNumberMaterial]
																	// Update existing placements with converted values
																	e.cuttingPlanMaterialPlacement.map(placement => {
																		let machine = obj[e.partNumberMaterial].reftissuMachines.find(cm => cm.defaultValue === true)
																		if (machine != null) {
																			// Update with converted values from backend
																			placement.maxPlie = machine.convertedMaxPlie || machine.maxPlie
																			placement.maxPlieDrill = machine.convertedMaxPlieDrill || machine.maxPlieDrill
																			placement.maxDrill = machine.maxDrill
																		}
																		return placement
																	})
																} else {
																	let machine = obj[e.partNumberMaterial].reftissuMachines.find(cm => cm.defaultValue === true)
																	if (machine != null) {
																		e.cuttingPlanMaterialPlacement[0].machine = machine.machineType
																		// Use converted values from backend for new placements
																		e.cuttingPlanMaterialPlacement[0].maxPlie = machine.convertedMaxPlie || machine.maxPlie
																		e.cuttingPlanMaterialPlacement[0].maxPlieDrill = machine.convertedMaxPlieDrill || machine.maxPlieDrill
																		e.cuttingPlanMaterialPlacement[0].maxDrill = machine.maxDrill
																		e.cuttingPlanMaterialPlacement[0].pliesConfig = machine.pliesConfig
																	}
																	let category = obj[e.partNumberMaterial].reftissuCategories.find(cm => cm.defaultValue === true)
																	if (category != null) {
																		e.cuttingPlanMaterialPlacement[0].category = category.category
																		e.cuttingPlanMaterialPlacement[0].laize = category.borneMin
																	}
																}
															}
															return e;
														})

													})
													.finally(() => {
														this.setState({
															modalObj: { ...this.state.modalObj, cuttingPlanPartNumbers: [...arrPn], cuttingPlanMaterials: cuttingPlanMaterialsList },
															partNumberMaterialConfigs: { ...obj },
															optionsList: { ...this.state.optionsList, partNumberBoom: arrBoom.map(e => { return { label: e.partNumber, value: e.partNumber, item: e } }) }
														})
													})
											})



									}}
								/>
							</td>
							<td className='table-elem-sm'>{elem.description}</td>
							<td className='table-elem-sm'
								style={(elem.item && elem.item.startsWith("WL") && elem.item.toUpperCase().trim() !== "W" + elem.partNumber.toUpperCase().trim()) ? { backgroundColor: "red", color: "white" } : {}}
							>{elem.item}</td>
							<td className='table-elem-sm'>
								<input value={elem.quantity || ""} onChange={(e) => {
									if (/^\d*$/.test(e.target.value)) {
										arrPn[ind].quantity = e.target.value != "" ? parseInt(e.target.value) : null
										this.setState({
											modalObj: {
												...this.state.modalObj,
												cuttingPlanPartNumbers: [...arrPn],
												cuttingPlanMaterials: [...this.state.modalObj.cuttingPlanMaterials].map(cpmElem => {
													let total = 0;
													cpmElem.partNumbers.split(",").map(pnElem => {
														this.state.optionsList.partNumberBoom.filter(e => (e.item.partNumber === pnElem && cpmElem.partNumberMaterial === e.item.partNumberMaterial)).map(pnObj => {
															total += pnObj.item.quantityPer * partnumbersObject[pnElem].quantity
														})
													})
													// Apply 1.03 multiplier only if not a plaque material (consistent with planUsage calculation)
													cpmElem.qadUsage = !cpmElem.plaque ? total * 1.03 : total
													return cpmElem
												})
											}
										})
									}
								}} />
							</td>
							<td className='table-elem-sm d-flex justify-content-center'>
								<button className='btn btn-outline-dark' onClick={() => {
									arrPn.splice(ind, 1);
									let cuttingPlanMaterialsArr = []
									this.state.modalObj.cuttingPlanMaterials.filter(e => e.partNumbers !== elem.partNumber).map(cpm => {
										let arrPns = cpm.partNumbers.split(",")
										if (!arrPns.includes(elem.partNumber)) {
											cuttingPlanMaterialsArr.push(cpm)
										} else {
											arrPns = arrPns.filter(e => e != elem.partNumber)
											cpm.partNumbers = arrPns.join(",")
											cuttingPlanMaterialsArr.push(cpm)
										}
									})
									this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanPartNumbers: [...arrPn], cuttingPlanMaterials: cuttingPlanMaterialsArr } })
								}}><FontAwesomeIcon icon={faTrashAlt} /></button>
								{/* <button className='btn btn-outline-dark' disabled={arrPn[ind].updating === true} onClick={() => {
									arrPn[ind].updating = true;
									this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanPartNumbers: [...arrPn] } })
									axios.post(`/api/partNumberBoom/update/${arrPn[ind].partNumber}`)
										.then(res => {
											arrPn[ind].updating = false;
											this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanPartNumbers: [...arrPn] } })
											arrPn[ind].partNumber = option ? option.value : null
											arrPn[ind].description = option ? option.item.description : null
											arrPn[ind].item = option ? option.item.item : null
											arrPn[ind].quantityPer = option ? option.item.quantityPer : null

											let arrBoom = []
											let cuttingPlanMaterialsList = []

											Promise.all(arrPn.map((elemPn, indPn) => {
												let dep = ["project=" + this.state.modalObj.projet]
												if (this.state.modalObj.version) { dep.push("version=" + this.state.modalObj.version) }
												dep.push("partNumber=" + elemPn.partNumber)
												dep.push("item=" + elemPn.item)

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
													axios.get(`/api/partNumberMaterialConfig/pns/${cuttingPlanMaterialsList.map(e => e.partNumberMaterial).join(",")}?projet=${this.state.modalObj.projet}`)
														.then(res => {
															res.data.map(e => {
																obj[e.partNumberMaterial] = { ...e }
															})
															cuttingPlanMaterialsList = [...cuttingPlanMaterialsList].map(e => {
																if (obj[e.partNumberMaterial]) {
																	e.tauxScrap = obj[e.partNumberMaterial].tauxScrap
																	e.rotation = obj[e.partNumberMaterial].rotation
																	e.plaque = obj[e.partNumberMaterial].plaque
																	e.vitesse = obj[e.partNumberMaterial].vitesse
																	e.matelassageEndroit = obj[e.partNumberMaterial].matelassageEndroit
																	e.description = obj[e.partNumberMaterial].description

																	let machine = obj[e.partNumberMaterial].reftissuMachines.find(cm => cm.defaultValue === true)
																	if (machine != null) {
																		e.cuttingPlanMaterialPlacement[0].machine = machine.machineType
																		e.cuttingPlanMaterialPlacement[0].maxPlie = machine.maxPlie
																		e.cuttingPlanMaterialPlacement[0].maxPlieDrill = machine.maxPlieDrill
																		e.cuttingPlanMaterialPlacement[0].maxDrill = machine.maxDrill
																		e.cuttingPlanMaterialPlacement[0].pliesConfig = machine.pliesConfig
																	}
																	let category = obj[e.partNumberMaterial].reftissuCategories.find(cm => cm.defaultValue === true)
																	if (category != null) {
																		e.cuttingPlanMaterialPlacement[0].category = category.category
																		e.cuttingPlanMaterialPlacement[0].laize = category.borneMin
																	}
																}
																return e;
															})

														})
														.finally(() => {
															this.setState({
																modalObj: { ...this.state.modalObj, cuttingPlanPartNumbers: [...arrPn], cuttingPlanMaterials: cuttingPlanMaterialsList },
																partNumberMaterialConfigs: { ...obj },
																optionsList: { ...this.state.optionsList, partNumberBoom: arrBoom.map(e => { return { label: e.partNumber, value: e.partNumber, item: e } }) }
															})
														})
												})
										})
								}}><FontAwesomeIcon icon={faRefresh} /></button> */}
							</td>
						</tr>
					})}
				</tbody>
			</table>
			<div className='d-flex'>
				<button
					className='btn btn-outline-danger'
					onClick={() => {
						arrPn.push({})
						this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanPartNumbers: [...arrPn] } })
					}}
				><FontAwesomeIcon icon={faPlus} /></button>
				<button
					className='btn btn-outline-danger'
					onClick={() => {
						this.drillVerification()
					}}
				>Drill overview</button>
			</div>

		</div>
	}

	renderReftissus = () => {
		const { entityId } = this.props
		let arr = this.state.modalObj.cuttingPlanMaterials || []
		let totalQuantite = 0
		if (this.state.modalObj.cuttingPlanPartNumbers) {
			this.state.modalObj.cuttingPlanPartNumbers.map(cppn => {
				if (cppn.quantity) {
					totalQuantite += cppn.quantity
				}
			})
		}
		return <div className='mb-2 table-responsive'>
			<table className='table table-grey-border m-0'>
				<thead style={{ backgroundColor: "black", color: "white" }}>
					<tr>
						<th className='table-elem-sm' style={{ whiteSpace: "nowrap" }}>PN Material
							<button className='btn btn-outline-light'
								onClick={() => { this.setState({ hideColumn: !this.state.hideColumn }) }}
								style={{
									padding: "2 5",
									fontSize: 12,
									marginLeft: 5
								}}
							>
								<FontAwesomeIcon icon={this.state.hideColumn ? faAngleRight : faAngleLeft} />
							</button><br />
							<button className='btn btn-outline-light' disabled={this.state.refreshMaterial}
								onClick={() => {
									this.setState({ refreshMaterial: true })
									axios.post(`/api/partNumberMaterialConfig/refresh`, arr.map(e => e.partNumberMaterial))
										.then(res => {
											// this.setState({ refreshMaterial: false, partNumberMaterialConfigs: res.data })
											axios.get(`/api/partNumberMaterialConfig/pns/${arr.map(e => e.partNumberMaterial).join(",")}?projet=${this.state.modalObj.projet}`)
												.then(res => {
													let obj = {}
													res.data.map(e => {
														obj[e.partNumberMaterial] = { ...e }
													})
													arr = [...arr].map(e => {
														if (obj[e.partNumberMaterial]) {
															e.tauxScrap = obj[e.partNumberMaterial].tauxScrap
															e.rotation = obj[e.partNumberMaterial].rotation
															e.plaque = obj[e.partNumberMaterial].plaque
															e.vitesse = obj[e.partNumberMaterial].vitesse
															e.matelassageEndroit = obj[e.partNumberMaterial].matelassageEndroit
															e.description = obj[e.partNumberMaterial].description

															// Update maxPlie, maxPlieDrill, and maxDrill with converted values for all placements
															e.cuttingPlanMaterialPlacement.map(placement => {
																let machine = obj[e.partNumberMaterial].reftissuMachines.find(cm => cm.defaultValue === true)
																if (machine != null) {
																	// Update with converted values from backend
																	placement.maxPlie = machine.convertedMaxPlie || machine.maxPlie
																	placement.maxPlieDrill = machine.convertedMaxPlieDrill || machine.maxPlieDrill
																	placement.maxDrill = machine.maxDrill
																}
																return placement
															})
														}
														return e;
													})
													this.setState({ refreshMaterial: false, partNumberMaterialConfigs: { ...obj }, modalObj: { ...this.state.modalObj, cuttingPlanMaterials: arr } })
												})
												.catch(err => {
													this.setState({ error: err.response.data, refreshMaterial: false })
												})
										})
										.finally(() => {
											this.setState({ refreshMaterial: false })
										})
								}}
								style={{
									padding: "2 5",
									fontSize: 12,
									marginLeft: 5
								}}
							>
								{this.state.refreshMaterial ? <FontAwesomeIcon icon={faSpinner} spin /> : <FontAwesomeIcon icon={faRefresh} />}
							</button>
						</th>
						{!this.state.hideColumn && [<th className='table-elem-sm' style={{ minWidth: 250 }}>Description</th>,
						<th className='table-elem-sm'>
							Part Numbers
						</th>,
						<th className='table-elem-sm'>Plan Usage</th>,
						<th className='table-elem-sm'>QAD Usage</th>,
						<th className='table-elem-sm'>Gap</th>,
						<th className='table-elem-sm'>% Gap</th>
						]}
						<th className='table-elem-sm'>Matelassage Endroit</th>

						<th className='table-elem-sm' style={{ minWidth: 200 }}>
							Placement: {this.state.optionsList.projet && (this.state.optionsList.projet.find(e => this.state.modalObj.projet === e.value)?.item.code || "")}
							{this.state.optionsList.version && (this.state.optionsList.version.find(e => this.state.modalObj.version === e.value)?.item.code || "")}
						</th>
						<th className='table-elem-sm' style={{ minWidth: 120 }}>Machine</th>
						<th className='table-elem-sm' style={{ minWidth: 40 }}>Drill</th>
						<th className='table-elem-sm' style={{ minWidth: 120 }}>Category</th>
						<th className='table-elem-sm'>Part Numbers</th>
						<th className='table-elem-sm'>Nbr de Couche</th>
						<th className='table-elem-sm'>Config</th>
						<th className='table-elem-sm' style={{ minWidth: 60 }}>Laize</th>

						<th className='table-elem-sm' style={{ minWidth: 60 }}>Longueur</th>
						<th className='table-elem-sm' style={{ minWidth: 60 }}>LM total</th>
						<th className='table-elem-sm' style={{ minWidth: 60 }}>Max Plie</th>
						<th className='table-elem-sm' style={{ minWidth: 60 }}>Perimetre</th>
						{!this.state.hideColumn && <th className='table-elem-sm'>Temps de coupe</th>}
						<th className='table-elem-sm' style={{ minWidth: 60 }}>Espace Relarge</th>
						<th className='table-elem-sm' style={{ minWidth: 60 }}>Rotation</th>
						<th className='table-elem-sm' style={{ minWidth: 60 }}>En bas</th>

						<th></th>

					</tr>
				</thead>
				<tbody>
					{arr.sort((a, b) => a.partNumberMaterial.localeCompare(b.partNumberMaterial)).map((elem, ind) => {
						let arrPlacement = elem.cuttingPlanMaterialPlacement.length > 0 ? elem.cuttingPlanMaterialPlacement : [{}]

						let planUsage = arrPlacement.filter(e => e.activated).map(a => (a.longueurMatelas) || 0).reduce((a, b) => a + b, 0)
						if (!elem.plaque) {
							planUsage = planUsage * 1.03
						}
						return arrPlacement.sort((a, b) => a.groupPlacement - b.groupPlacement || Number(b.activated) - Number(a.activated)).map((elemPc, indPc) => {
							let arrDrill = (elemPc.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
							let maxPlie = arrDrill[0] == null && arrDrill[1] == null ? arrPlacement[indPc].maxPlie : arrPlacement[indPc].maxPlieDrill

							if (elemPc.activated === false && elemPc.hideRow === true) {
								return;
							}

							let rotaErreur = false
							if (this.state.partNumberMaterialConfigs[elem.partNumberMaterial] != null && this.state.partNumberMaterialConfigs[elem.partNumberMaterial].rotation && elemPc.rotation) {
								if (elemPc.rotation.includes("90") && !this.state.partNumberMaterialConfigs[elem.partNumberMaterial].rotation.includes("90")) {
									rotaErreur = true
								}
								if (elemPc.rotation.includes("180") && !this.state.partNumberMaterialConfigs[elem.partNumberMaterial].rotation.includes("180")) {
									rotaErreur = true
								}
								if (elemPc.rotation.toUpperCase().includes("FIX") && !this.state.partNumberMaterialConfigs[elem.partNumberMaterial].rotation.includes("FIX")) {
									rotaErreur = true
								}
							}

							let verifEndroitErreur = false
							if (elem.matelassageEndroit != null && elem.matelassageEndroit.toUpperCase().endsWith("EN BAS")) {
								//endroit is like this 10/10 and we to check like here that 10 = 10
								if (elemPc.verifEndroit == null) {
									verifEndroitErreur = true
								} else {
									let endroit = elemPc.verifEndroit.split("/").map(e => e.trim())
									if (endroit[0] != endroit[1]) {
										verifEndroitErreur = true
									}
								}
							} else {
								if (elemPc.verifEndroit && elemPc.verifEndroit.length > 0) {
									verifEndroitErreur = true
								}
							}

							let errorEspaceRelarge = false
							try {
								if (elemPc.machine === "Lectra IP6") {
									let arrConditionBuffer = []
									if (this.state.partNumberMaterialConfigs[elem.partNumberMaterial] != null &&
										(this.state.partNumberMaterialConfigs[elem.partNumberMaterial].buffer1IP6
											|| this.state.partNumberMaterialConfigs[elem.partNumberMaterial].buffer2IP6)) {
										if (this.state.partNumberMaterialConfigs[elem.partNumberMaterial].buffer1IP6 && this.state.partNumberMaterialConfigs[elem.partNumberMaterial].buffer1IP6.length > 0) {
											arrConditionBuffer.push(this.state.partNumberMaterialConfigs[elem.partNumberMaterial].buffer1IP6)
										}
										if (this.state.partNumberMaterialConfigs[elem.partNumberMaterial].buffer2IP6 && this.state.partNumberMaterialConfigs[elem.partNumberMaterial].buffer2IP6.length > 0) {
											arrConditionBuffer.push(this.state.partNumberMaterialConfigs[elem.partNumberMaterial].buffer2IP6)
										}
									}
									if (arrConditionBuffer.length === 0) {
										arrConditionBuffer.push("ESP00")
									}
									if (!arrConditionBuffer.includes(elemPc.espaceRelarge)) {
										errorEspaceRelarge = true
									}
								}
								if (elemPc.activated && totalQuantite >= 20 && elemPc.longueur > 0.3 && elemPc.nbrCouche >= 5 && (entityId === null || entityId === undefined)) {
									let configObj = this.state.partNumberMaterialConfigs[elem.partNumberMaterial]
									if (configObj.validated0BF === true
										&& elemPc.espaceRelarge && elemPc.espaceRelarge.trim().toUpperCase() !== 'ESP00' && elemPc.machine === "Lectra") {
										errorEspaceRelarge = true
									}
								}
							} catch (e) {
							}
							return <tr style={elemPc.activated === false ? { backgroundColor: "#ffffc0" } : ind % 2 === 1 ? { backgroundColor: "#f2f2f2" } : { backgroundColor: "" }}>
								{indPc === 0 && <td className='table-elem-sm' rowSpan={arrPlacement.filter(e => (e.activated === true || e.hideRow !== true)).length}>
									{elem.partNumberMaterial}
									<div style={{ display: "flex", justifyContent: "center", alignItems: "center" }}>
										<button className='btn btn-outline-danger'
											style={{
												fontSize: 10,
												padding: 2,
												borderRadius: "50%"
											}}
											onClick={() => {

												let obj = {}
												let machine = this.state.partNumberMaterialConfigs[arr[ind].partNumberMaterial].reftissuMachines.find(cm => cm.defaultValue === true)
												if (machine != null) {
													obj.machine = machine.machineType
													obj.maxPlie = machine.maxPlie
													obj.maxPlieDrill = machine.maxPlieDrill
													obj.maxDrill = machine.maxDrill
													obj.pliesConfig = machine.pliesConfig
												}
												let category = this.state.partNumberMaterialConfigs[arr[ind].partNumberMaterial].reftissuCategories.find(cm => cm.defaultValue === true)
												if (category != null) {
													obj.category = category.category
													obj.laize = category.borneMin
												}
												obj.activated = true
												obj.groupPlacement = arrPlacement[arrPlacement.length - 1].groupPlacement + 1
												arr[ind].cuttingPlanMaterialPlacement.push(obj)
												this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
											}}><FontAwesomeIcon icon={faPlus} /></button>
										{this.state.partNumberMaterialConfigs[elem.partNumberMaterial] && <button
											className='btn btn-outline-primary px-1 ml-1'
											style={{
												fontSize: 10,
												padding: 2,
												borderRadius: "50%"
											}}
											onClick={() => {
												this.setState({ reftissuConfig: elem.partNumberMaterial })
											}}>
											<FontAwesomeIcon icon={faInfo} />
										</button>}
										<button className='btn btn-outline-danger ml-1'
											style={{
												fontSize: 10,
												padding: 2,
												borderRadius: "50%"
											}}
											onClick={() => {
												if (window.confirm("est-ce-que vous êtes s^re de supprimer la refissu : " + elem.partNumberMaterial + " ?")) {
													arr.splice(ind, 1)
													this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
												}
											}}>
											<FontAwesomeIcon icon={faTimes} />
										</button>
										{this.state.partNumberMaterialConfigs[elem.partNumberMaterial]
											&& this.state.partNumberMaterialConfigs[elem.partNumberMaterial].validated0BF === true
											&& <span style={{
												backgroundColor: "red",
												padding: "2 5",
												color: "white",
												borderRadius: "50%",
												marginLeft: 4
											}}>0BF</span>}
										{this.state.partNumberMaterialConfigs[elem.partNumberMaterial]
											&& this.state.partNumberMaterialConfigs[elem.partNumberMaterial].validatedIP6 === true
											&& <span style={{
												backgroundColor: "blue",
												padding: "2 5",
												color: "white",
												borderRadius: "50%",
												marginLeft: 4
											}}>IP6</span>}
									</div>
								</td>}
								{indPc === 0 && !this.state.hideColumn && [
									<td className='table-elem-sm' rowSpan={arrPlacement.filter(e => (e.activated === true || e.hideRow !== true)).length}>{elem.description}</td>,
									<td className='table-elem-sm' rowSpan={arrPlacement.filter(e => (e.activated === true || e.hideRow !== true)).length}><ul style={{ padding: 0, margin: 0, listStyleType: "none" }}>
										{elem.partNumbers && elem.partNumbers.split(",").map(e => <li style={{ whiteSpace: "nowrap" }}>{e}</li>)}
									</ul></td>,
									<td className='table-elem-sm' rowSpan={arrPlacement.filter(e => (e.activated === true || e.hideRow !== true)).length}>
										{planUsage && planUsage.toFixed(3)}
									</td>,
									<td
										className='table-elem-sm' rowSpan={arrPlacement.filter(e => (e.activated === true || e.hideRow !== true)).length}
									>{elem.qadUsage && elem.qadUsage.toFixed(3)}</td>,
									<td
										className='table-elem-sm' rowSpan={arrPlacement.filter(e => (e.activated === true || e.hideRow !== true)).length}
									>{elem.qadUsage && planUsage && (planUsage - elem.qadUsage).toFixed(3)}</td>,
									<td
										className='table-elem-sm' rowSpan={arrPlacement.filter(e => (e.activated === true || e.hideRow !== true)).length}
										style={Math.abs((planUsage - elem.qadUsage) * 100 / planUsage) > 30 ? { whiteSpace: "nowrap", backgroundColor: "red", color: "white" } : { whiteSpace: "nowrap" }}
									>{elem.qadUsage && planUsage && ((planUsage - elem.qadUsage) * 100 / planUsage).toFixed(3)} %</td>
								]}

								{indPc === 0 && <td className='table-elem-sm' rowSpan={arrPlacement.filter(e => (e.activated === true || e.hideRow !== true)).length}
									style={(elem.matelassageEndroit && elem.matelassageEndroit.includes("En Bas")) ? { color: "red" } : {}}
								>{elem.matelassageEndroit}</td>}
								<td className='table-elem-sm '>
									<div className='d-flex'>
										<button className='btn btn-outline-primary'
											style={{
												fontSize: 10,
												padding: 2,
												// borderRadius: "50%"
											}}
											onClick={() => {
												// open a new page /cutfileviewer/1DEM365/view
												window.open("/cutfileviewer/" + elemPc.placement + "/view", "_blank")
											}}
										>
											<FontAwesomeIcon icon={faEye} />
										</button>
										<input value={elemPc.placement || ""} onChange={(e) => {
											arr[ind].cuttingPlanMaterialPlacement[indPc].placement = e.target.value
											this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
										}} />
										{elemPc.activated && <button className='btn btn-outline-warning'
											style={{
												fontSize: 10,
												padding: 2,
											}}
											onClick={() => {
												let obj = {}
												let machine = this.state.partNumberMaterialConfigs[arr[ind].partNumberMaterial].reftissuMachines.find(cm => cm.defaultValue === true)
												if (machine != null) {
													obj.machine = machine.machineType
													obj.maxPlie = machine.maxPlie
													obj.maxPlieDrill = machine.maxPlieDrill
													obj.maxDrill = machine.maxDrill
													obj.pliesConfig = machine.pliesConfig
												}
												let category = this.state.partNumberMaterialConfigs[arr[ind].partNumberMaterial].reftissuCategories.find(cm => cm.defaultValue === true)
												if (category != null) {
													obj.category = category.category
													obj.laize = category.borneMin
												}
												obj.groupPlacement = elemPc.groupPlacement
												obj.drill = elemPc.drill
												obj.activated = false
												arrPlacement.push(obj)
												arr[ind].cuttingPlanMaterialPlacement = [...arrPlacement]
												this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
											}}
										>
											<FontAwesomeIcon icon={faPlus} />
										</button>}
									</div>

								</td>

								<td className='table-elem-sm'>
									<Select classNamePrefix="rs"
										placeholder={"Machine Type"}
										isClearable={false}
										value={(this.state.optionsList.machineType && this.state.optionsList.machineType.length > 0 && elemPc.machine)
											? { label: elemPc.machine, value: elemPc.machine }
											: null
										}
										options={this.state.partNumberMaterialConfigs[elem.partNumberMaterial]
											? this.state.partNumberMaterialConfigs[elem.partNumberMaterial].reftissuMachines.map(e => { return { label: e.machineType, value: e.machineType } })
											: []
										}
										onChange={(option) => {
											let configObj = this.state.partNumberMaterialConfigs[elem.partNumberMaterial].reftissuMachines.find(e => e.machineType === option.value)
											if (configObj != null) {
												arrPlacement[indPc].machine = configObj.machineType
												arrPlacement[indPc].maxPlie = configObj.maxPlie
												arrPlacement[indPc].maxPlieDrill = configObj.maxPlieDrill
												arrPlacement[indPc].maxDrill = configObj.maxDrill
												arrPlacement[indPc].pliesConfig = configObj.pliesConfig
												arr[ind].cuttingPlanMaterialPlacement = arrPlacement
												this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
											}
										}}
									/>
								</td>
								<td className='table-elem-sm' style={{}}>
									<div className='d-flex'>
										{arrDrill.map((elemDrill, indDrill) => {
											return <input key={"input-drill-" + indDrill + "-" + indPc + "-" + ind}
												style={(elemDrill != null && elemDrill > arrPlacement[indPc].maxDrill) ? { width: 40, backgroundColor: "pink" } : { width: 40 }}

												value={elemDrill || ""}
												onChange={(e) => {
													if (/^\d*$/.test(e.target.value) && e.target.value != "0") {
														arrDrill[indDrill] = e.target.value != "" ? parseInt(e.target.value) : null
														for (let j = 0; j < arrPlacement.length; j++) {
															if (arrPlacement[indPc].groupPlacement === arrPlacement[j].groupPlacement) {
																arrPlacement[j].drill = arrDrill.join(",")
															}
														}
														arr[ind].cuttingPlanMaterialPlacement = arrPlacement
														this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
													}
												}} />
										})}
									</div>

								</td>
								<td className='table-elem-sm'>
									<Select classNamePrefix="rs"
										placeholder={"Machine Type"}
										isClearable={false}
										value={(elemPc.category)
											? { label: elemPc.category, value: elemPc.category }
											: null
										}
										options={this.state.partNumberMaterialConfigs[elem.partNumberMaterial]
											? this.state.partNumberMaterialConfigs[elem.partNumberMaterial].reftissuCategories.map(e => { return { label: e.category + "(" + e.borneMin + "=>" + e.borneMax + ")", value: e.category, item: e } })
											: []
										}
										onChange={(option) => {
											arrPlacement[indPc].category = option ? option.value : null
											arrPlacement[indPc].laize = option ? option.item.borneMin : null
											arr[ind].cuttingPlanMaterialPlacement = arrPlacement
											this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })

										}}
									/>
								</td>
								<td className='table-elem-sm'>
									<ul style={{ padding: 0, margin: 0, listStyleType: "none" }}>
										{elemPc.partNumbers && elemPc.partNumbers.length > 0 && elemPc.partNumbers.split(", ").map(e => {
											if (e == null || e.length === 0) return null
											let splitPn = e.split(":")
											let pnObj = this.state.modalObj.cuttingPlanPartNumbers
												.find(pn => pn.partNumber && pn.partNumber.toUpperCase().trim() == splitPn[0].toUpperCase().trim())
											return <li style={{ whiteSpace: "nowrap" }}>
												{e}
												{pnObj && ((parseInt(splitPn[1]) * elemPc.nbrCouche) === pnObj.quantity
													? ""
													: <span style={{ color: "red", fontWeight: 'bold' }}>({(parseInt(splitPn[1]) * elemPc.nbrCouche) - pnObj.quantity > 0 && "+"}{(parseInt(splitPn[1]) * elemPc.nbrCouche) - pnObj.quantity})</span>
												)}
											</li>
										})}
									</ul>
								</td>
								<td
									className='table-elem-sm'
									style={(this.state.modalObj.id && elemPc.nbrCoucheOld) ? { backgroundColor: "#ff8787" } : {}}
								>
									{elemPc.nbrCouche}
									{this.state.modalObj.id && elemPc.nbrCoucheOld && ` (${elemPc.nbrCoucheOld}) `}
									{(elemPc.nbrCouche / maxPlie) > 1 && <br />}
									{(elemPc.nbrCouche / maxPlie) > 1 && <span style={{ whiteSpace: "nowrap" }}>{(elemPc.nbrCouche / maxPlie) >= 2 && parseInt(elemPc.nbrCouche / maxPlie)} {(elemPc.nbrCouche / maxPlie) >= 2 && "X"} {(elemPc.nbrCouche / maxPlie) >= 1 && maxPlie} {elemPc.nbrCouche % maxPlie != 0 && "+"} {elemPc.nbrCouche % maxPlie != 0 && elemPc.nbrCouche % maxPlie}</span>}
									{/* <input value={elemPc.nbrCouche || ""}
										style={arrPlacement[indPc].nbrCouche && ((arrDrill.join(",").trim() === "," && arrPlacement[indPc].maxPlie && arrPlacement[indPc].nbrCouche > arrPlacement[indPc].maxPlie)
											|| (arrDrill.join(",").trim() !== "," && arrPlacement[indPc].maxPlieDrill && arrPlacement[indPc].nbrCouche > arrPlacement[indPc].maxPlieDrill))
											? { backgroundColor: "red" }
											: {}
										}
										onChange={(e) => {
											if (/^\d*$/.test(e.target.value)) {
												arrPlacement[indPc].nbrCouche = e.target.value != "" ? parseInt(e.target.value) : null
												let config = null;
												if (this.state.partNumberMaterialConfigs[elem.partNumberMaterial] && elemPc.machine) {
													let configObj = this.state.partNumberMaterialConfigs[elem.partNumberMaterial].reftissuMachines.find(e => e.machineType == elemPc.machine)
													if (configObj != null) {
														let configArr = configObj.pliesConfig.split("|").map(e => { return [parseInt(e.split(";")[0]), e.split(";")[1]] }).sort((a, b) => a[0] - b[0])
														for (let i = 0; i < configArr.length; i++) {
															if (configArr[i][0] <= elemPc.nbrCouche) {
																config = configArr[i][1]
															}
														}

													}
												}
												arrPlacement[indPc].config = config
												if (e.target.value !== "" && arrPlacement[indPc].longueur) {
													arrPlacement[indPc].longueurMatelas = parseFloat(arrPlacement[indPc].longueur) + this.getMarge(parseFloat(arrPlacement[indPc].longueur), parseInt(e.target.value), arr[ind].partNumberMaterial)
													// arrPlacement[indPc].tempsDeCoupe = (arrPlacement[indPc].longueurMatelas*100 / (arr[ind].vitesse)).toFixed(3)
												} else {
													arrPlacement[indPc].longueurMatelas = null
													// arrPlacement[indPc].tempsDeCoupe = null
												}
												arr[ind].cuttingPlanMaterialPlacement = arrPlacement
												this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
											}
										}} /> */}
								</td>
								<td className='table-elem-sm'>
									{elemPc.config}
								</td>



								<td className='table-elem-sm'>
									{elemPc.laize}
									{/* <input value={elemPc.laize || ""} onChange={(e) => {
										if (/^\d*\.?\d*$/.test(e.target.value)) {
											arrPlacement[indPc].category = null
											if ((e.target.value) !== "") {
												let a = parseFloat(e.target.value)
												this.state.partNumberMaterialConfigs[elem.partNumberMaterial].reftissuCategories.map(cf => {
													if (a >= cf.borneMin && a < cf.borneMax) {
														arrPlacement[indPc].category = cf.category
													}
												})
											}
											arrPlacement[indPc].laize = e.target.value !== "" ? e.target.value : null
											arr[ind].cuttingPlanMaterialPlacement = arrPlacement
											this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
										}

									}} /> */}

								</td>

								<td className='table-elem-sm'>
									{elem.plaque != null && [<span style={{ whiteSpace: "nowrap" }}>Plaque : {elem.plaque}</span>, <br />]}
									{elemPc.longueur}
									{/* <input value={elemPc.longueur || ""} onChange={(e) => {
										if (/^\d*\.?\d*$/.test(e.target.value)) {
											arrPlacement[indPc].longueur = e.target.value !== "" ? (e.target.value) : null
											if (e.target.value !== "") {
												let lm = parseFloat(e.target.value) + this.getMarge(parseFloat(e.target.value), arrPlacement[indPc].nbrCouche, arr[ind].partNumberMaterial)
												arrPlacement[indPc].longueurMatelas = lm
												// arrPlacement[indPc].tempsDeCoupe = (lm *100 / (arr[ind].vitesse)).toFixed(3)
											} else {
												arrPlacement[indPc].longueurMatelas = null
												// arrPlacement[indPc].tempsDeCoupe = null
											}
											arr[ind].cuttingPlanMaterialPlacement = arrPlacement
											this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
										}
									}} /> */}
								</td>
								<td className='table-elem-sm'>
									{elemPc.longueurMatelas && elemPc.longueurMatelas.toFixed(3)}
								</td>
								<td className='table-elem-sm'>{(arrDrill[0] == null && arrDrill[1] == null ? arrPlacement[indPc].maxPlie : arrPlacement[indPc].maxPlieDrill)}</td>
								<td className='table-elem-sm'>
									{elemPc.perimetre && elemPc.perimetre.toFixed(3)}
								</td>
								{!this.state.hideColumn && <td className='table-elem-sm' style={{ whiteSpace: "nowrap" }}>
									{elemPc.tempsDeCoupe && <span>{elemPc.tempsDeCoupe - elemPc.tempsDeCoupe % 1} min {((elemPc.tempsDeCoupe % 1) * 60).toFixed(0)} s</span>}
								</td>}
								<td className='table-elem-sm'
									style={errorEspaceRelarge
										? { backgroundColor: "red" }
										: {}} >
									{elemPc.espaceRelarge}
								</td>
								<td className='table-elem-sm'
									style={rotaErreur ? { backgroundColor: "#ff8787" } : {}} >
									{elemPc.rotation}
								</td>
								<td className='table-elem-sm'
									style={verifEndroitErreur ? { backgroundColor: "#ff8787" } : {}}
								>
									{elemPc.verifEndroit}
								</td>
								<td className='table-elem-sm'>
									<div className='d-flex'>
										{!elemPc.activated && <button className='btn btn-outline-dark' onClick={() => {
											let objInd = arrPlacement.findIndex(e => (e.groupPlacement == elemPc.groupPlacement && e.activated === true))
											if (objInd >= 0) {
												arrPlacement[objInd].activated = false
											}
											arrPlacement[indPc].activated = true
											arr[ind].cuttingPlanMaterialPlacement = arrPlacement
											this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
										}}>
											<FontAwesomeIcon icon={faArrowUp} />
										</button>}
										{elemPc.activated && arrPlacement.findIndex(e => e.groupPlacement === elemPc.groupPlacement && e.activated === false) >= 0 && (elemPc.hideRow === true
											? <button className='btn btn-outline-dark' onClick={() => {
												arr[ind].cuttingPlanMaterialPlacement = arrPlacement.map(e => {
													if (e.groupPlacement == elemPc.groupPlacement) {
														e.hideRow = false
													}
													return e
												})
												this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
											}}>
												<FontAwesomeIcon icon={faAngleDown} />
											</button>
											: <button className='btn btn-outline-dark' onClick={() => {
												arr[ind].cuttingPlanMaterialPlacement = arrPlacement.map(e => {
													if (e.groupPlacement == elemPc.groupPlacement) {
														e.hideRow = true
													}
													return e
												})
												this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
											}}>
												<FontAwesomeIcon icon={faAngleUp} />
											</button>)}
										{arrPlacement.length > 1 && <button className='btn btn-outline-dark' onClick={() => {
											if (elemPc.activated) {
												let objInd = arrPlacement.findIndex(e => (e.groupPlacement == elemPc.groupPlacement && e.activated === false))
												if (objInd >= 0) {
													arrPlacement[objInd].activated = true
												}
												arrPlacement.splice(indPc, 1)

												arr[ind].cuttingPlanMaterialPlacement = arrPlacement
												this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
											} else {
												arrPlacement.splice(indPc, 1)
												arr[ind].cuttingPlanMaterialPlacement = arrPlacement
												this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanMaterials: [...arr] } })
											}
										}}>
											<FontAwesomeIcon icon={faTrashAlt} />
										</button>}
									</div>

								</td>

							</tr>
						})

					})}
				</tbody>
			</table>
		</div>
	}


	getMarginConfig = (longueur, partNumberMaterial, machine = null) => {
		let materialConfig = this.state.partNumberMaterialConfigs[partNumberMaterial]
		if (!materialConfig || !materialConfig.reftissuMargins || longueur === null || longueur === undefined) {
			return null
		}

		let marge = null

		let arr = materialConfig.reftissuMargins.filter(e => e.machine === machine)
		if(arr.length === 0) {
			arr = materialConfig.reftissuMargins.filter(e => e.machine == null)
		}
		
		arr.forEach(cf => {
			if (longueur >= cf.longueurMin && longueur <= cf.longueurMax) {
				marge = cf
			}
		})

		return marge
	}

	getMarge = (longueur, nbrCouche, partNumberMaterial, machine = null) => {
		let marginConfig = this.getMarginConfig(longueur, partNumberMaterial, machine)
		if (!marginConfig || !marginConfig.pliesConfig || nbrCouche === null || nbrCouche === undefined) {
			return null
		}

		let marge = null
		let arr = marginConfig.pliesConfig.split("|").map(e => e.split(";").map(numb => parseFloat(numb))).sort((a, b) => a[0] - b[0])
		for (let i = 0; i < arr.length; i++) {
			if (nbrCouche >= arr[i][0]) {
				marge = arr[i][1]
			}
		}

		return marge
	}



	checkObj = () => {
		let placements = [], placementNbrCouche = {}, error = [], partNumbers = [], placementActivated = [], placementOptional = []
		let placementMeta = {}
		this.state.modalObj.cuttingPlanPartNumbers.map((cppn, ind) => {
			if (cppn.partNumber == null) {
				error.push("Un part number est vide")
			} else if (partNumbers.includes(cppn.partNumber)) {
				error.push("Le Part Number " + cppn.partNumber + " est dupliqué")
			} else if (cppn.quantity == null || cppn.quantity == 0) {
				error.push("Le Part Number " + cppn.partNumber + " n'a aucun quantité")
			} else {
				partNumbers.push(cppn.partNumber)
			}
		})
		let codePV = this.state.optionsList.projet && (this.state.optionsList.projet.find(e => this.state.modalObj.projet === e.value)?.item.code || "") +
			(this.state.optionsList.version && (this.state.optionsList.version.find(e => this.state.modalObj.version === e.value)?.item.code || ""))
		let arrPlctDrill1 = this.state.modalObj.cuttingPlanRapportDrills != null
			? this.state.modalObj.cuttingPlanRapportDrills.filter(e => ["D"].includes(e.labelInterne.trim().toUpperCase())).map(e => e.nomPlacement.toUpperCase().trim())
			: []
		let arrPlctDrill2 = this.state.modalObj.cuttingPlanRapportDrills != null
			? this.state.modalObj.cuttingPlanRapportDrills.filter(e => ["E"].includes(e.labelInterne.trim().toUpperCase())).map(e => e.nomPlacement.toUpperCase().trim())
			: []

		this.state.modalObj.cuttingPlanMaterials.map(cpm => {
			cpm.cuttingPlanMaterialPlacement.map(cpmp => {
				let arrDrill = (cpmp.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
				if (cpmp.placement == null) {
					error.push("un placement dans " + cpm.partNumberMaterial + " est vide")
				} else if (placements.includes(cpmp.placement)) {
					error.push("répétition du placement " + cpmp.placement)
					// } else if (!cpmp.placement.startsWith(codePV) && !cpmp.placement.startsWith("__" + codePV)) {
					// 	error.push("le préfixe du placement " + cpmp.placement + " doit être " + codePV + " ou __" + codePV)
				} else if (cpmp.nbrCouche == null || cpmp.nbrCouche == 0) {
					error.push("Nombre de couche du placement " + cpmp.placement + " est vide")
					// } else if ((arrDrill.join(",").trim() === "," && cpmp.maxPlie && cpmp.nbrCouche > cpmp.maxPlie)
					// 	|| (arrDrill.join(",").trim() !== "," && cpmp.maxPlieDrill && cpmp.nbrCouche > cpmp.maxPlieDrill)) {
					// 	error.push("Nombre de couche du placement " + cpmp.placement + " supérieure au max")
				} else if (cpmp.config == null) {
					error.push("Config du placement " + cpmp.placement + " est vide")
				} else if (cpmp.category == null) {
					error.push("Category du placement " + cpmp.placement + " est vide")
				} else if (cpmp.laize == null) {
					error.push("La laize du placement " + cpmp.placement + " est vide")
				} else if (cpmp.longueur == null) {
					error.push("Longueur du placement " + cpmp.placement + " est vide")
				} else if (cpmp.longueurMatelas == null) {
					error.push("Longueur Matelas du placement " + cpmp.placement + " est vide")
				} else {
					if (cpmp.activated === true) {
						placementActivated.push(cpmp.placement)
					} else if (cpmp.activated === false) {
						placementOptional.push(cpmp.placement)
					}
					placementMeta[cpmp.placement] = { partNumberMaterial: cpm.partNumberMaterial, groupPlacement: cpmp.groupPlacement, activated: cpmp.activated }
					placements.push(cpmp.placement)
					placementNbrCouche[cpmp.placement] = cpmp.nbrCouche
				}

				if (arrPlctDrill1.includes(cpmp.placement.toUpperCase().trim()) && arrDrill[0] == null) {
					error.push("R3 drill : le placement " + cpmp.placement + " a besoin de  le 1er drill")
				}
				if (arrPlctDrill2.includes(cpmp.placement.toUpperCase().trim()) && arrDrill[1] == null) {
					error.push("R3 drill : le placement " + cpmp.placement + " a besoin de  le 2eme drill")
				}

			})
		})
		if (error.length == 0) {
			let reftissuObj = {}
			this.state.modalObj.cuttingPlanMaterials.map(cpm => {
				cpm.cuttingPlanMaterialPlacement.map(cpmp => {
					if (cpmp.placement != null && cpmp.placement != "" && cpm.partNumberMaterial != null && cpm.partNumberMaterial != "") {
						reftissuObj[cpmp.placement.toUpperCase().trim()] = cpm.partNumberMaterial.toUpperCase().trim()
					}
				})
			})
			this.state.modalObj.cuttingPlanPartNumbers.map((cppn, ind) => {
				if (cppn.partNumber != null) {
					axios.get(`/api/ctcFiles/pn/${cppn.partNumber}`)
						.then(res => {
							// Filter out materials containing Bubble, Rectangle, Papier, or Bandle (case insensitive)
							const filteredData = res.data.filter(e => {
								const isValidType = e.type.trim().toLowerCase() === "fabric";
								const materialName = (e.partNumberMaterial || "").toLowerCase();
								const containsExcludedKeywords = materialName.includes("bubble") || 
																materialName.includes("rectangle") || 
																materialName.includes("papier") || 
																materialName.includes("bandle");
								return isValidType && !containsExcludedKeywords;
							});
							this.setState({ ctcData: { ...this.state.ctcData, [cppn.partNumber]: filteredData } })
						})
						.catch(err => {
							this.setState({ error: error })
						})
				}
			})
			this.setState({ loading: true, placementsInfo: null, placementsInfoOptional: null, digits: [], placementMeta: placementMeta })
			let allPlacements = [...placementActivated, ...placementOptional]
			axios.get(`/api/placementData/info-list?placements=${allPlacements.join(",")}&partNumbers=${this.state.modalObj.cuttingPlanPartNumbers.map(e => e.partNumber).join(",")}`)
				.then(res => {
					let resObj = {} // activated=true placements info
					let resObjOptional = {} // activated=false placements info
					let digits = []
					res.data.map(elem => {
						if (digits.includes(elem.digit) === false) {
							digits.push(elem.digit)
						}
						let isOptional = placementOptional.includes(elem.placement)
						let targetObj = isOptional ? resObjOptional : resObj
						if (targetObj[elem.partNumber] == null) {
							targetObj[elem.partNumber] = { [elem.digit + " - " + elem.sens]: { quantity: placementNbrCouche[elem.placement], reftissu: [reftissuObj[elem.placement.toUpperCase().trim()]], placements: [elem.placement], counterDrill1: elem.counterDrill1, counterDrill2: elem.counterDrill2, graphNumber: elem.graphNumber } }
						} else {
							if (targetObj[elem.partNumber][elem.digit + " - " + elem.sens] == null) {
								targetObj[elem.partNumber][elem.digit + " - " + elem.sens] = {
									quantity: placementNbrCouche[elem.placement],
									reftissu: [reftissuObj[elem.placement.toUpperCase().trim()]],
									placements: [elem.placement],
									counterDrill1: elem.counterDrill1,
									counterDrill2: elem.counterDrill2,
									graphNumber: elem.graphNumber
								}
							} else {
								targetObj[elem.partNumber][elem.digit + " - " + elem.sens].quantity += placementNbrCouche[elem.placement]
								if (!targetObj[elem.partNumber][elem.digit + " - " + elem.sens].reftissu.includes(reftissuObj[elem.placement.toUpperCase().trim()])) {
									targetObj[elem.partNumber][elem.digit + " - " + elem.sens].reftissu.push(reftissuObj[elem.placement.toUpperCase().trim()])
								}
								if (!targetObj[elem.partNumber][elem.digit + " - " + elem.sens].placements.includes(elem.placement)) {
									targetObj[elem.partNumber][elem.digit + " - " + elem.sens].placements.push(elem.placement)
								}
							}
						}
					})
					this.setState({ placementsInfo: resObj, placementsInfoOptional: resObjOptional })
					axios.post(`/api/placementData/plt-check`, digits.map(e => {
						if (e.endsWith("-LSR")) {
							return e.slice(0, e.length - 4)
						}
						return e;
					}))
						.then(resDg => {
							this.setState({ digits: resDg.data })
						})
					axios.post(`/api/drillEmp/list`, digits.map(e => {
						if (e.endsWith("-LSR")) {
							return e.slice(0, e.length - 4)
						}
						return e;
					}))
						.then(resDrill => {
							let objDrill = {}
							resDrill.data.map(elemDrill => {
								objDrill[elemDrill.pattern.trim().toUpperCase()] = elemDrill
							})

							this.setState({ drillEmpArr: objDrill })
						})

				})
				.catch(err => {
					this.setState({ error: err.response.data })
				})
		} else {
			this.setState({ error: error })
		}

	}

	drillVerification = async () => {
		this.setState({ showDrillModal: true, drillModalData: {} })

		let arrPn = this.state.modalObj.cuttingPlanPartNumbers || []
		let ctcDataAll = {}
		let digits = []
		let drillEmpAll = {}

		// Load CTC data for each part number
		for (let cppn of arrPn) {
			if (cppn.partNumber) {
				try {
					const res = await axios.get(`/api/ctcFiles/pn/${cppn.partNumber}`)
					// Filter out materials containing Bubble, Rectangle, Papier, or Bandle (case insensitive)
					const filteredData = res.data.filter(e => {
						const isValidType = e.type.trim().toLowerCase() === "fabric";
						const materialName = (e.partNumberMaterial || "").toLowerCase();
						const containsExcludedKeywords = materialName.includes("bubble") || 
														materialName.includes("rectangle") || 
														materialName.includes("papier") || 
														materialName.includes("bandle");
						return isValidType && !containsExcludedKeywords;
					});
					ctcDataAll[cppn.partNumber] = filteredData

					// Collect all unique digits/patterns
					filteredData.forEach(elem => {
						if (elem.pattern && !digits.includes(elem.pattern)) {
							digits.push(elem.pattern)
						}
					})
				} catch (err) {
					console.error(`Error loading CTC data for ${cppn.partNumber}:`, err)
				}
			}
		}

		// Load drill data for all patterns
		if (digits.length > 0) {
			try {
				const resDrill = await axios.post(`/api/drillEmp/list`, digits.map(e => {
					if (e.endsWith("-LSR")) {
						return e.slice(0, e.length - 4)
					}
					return e;
				}))

				resDrill.data.forEach(elemDrill => {
					drillEmpAll[elemDrill.pattern.trim().toUpperCase()] = elemDrill
				})
			} catch (err) {
				console.error("Error loading drill data:", err)
			}
		}

		this.setState({
			drillModalData: {
				ctcData: ctcDataAll,
				drillEmpData: drillEmpAll,
				partNumbers: arrPn.filter(p => p.partNumber)
			}
		})
	}

	renderDrillModal = () => {
		const { ctcData, drillEmpData, partNumbers } = this.state.drillModalData

		// Group materials by partNumberMaterial for the second table
		let materialGroups = {}
		if (ctcData && partNumbers) {
			partNumbers.forEach(pn => {
				if (ctcData[pn.partNumber]) {
					ctcData[pn.partNumber].forEach(item => {
						if (item.partNumberMaterial) {
							if (!materialGroups[item.partNumberMaterial]) {
								materialGroups[item.partNumberMaterial] = {
									drills: new Set(),
									description: item.partNumberMaterialDescription || 'N/A'
								}
							}

							// Get drill info for this pattern
							const pattern = item.pattern?.trim().toUpperCase()
							const drillInfo = drillEmpData && pattern ? drillEmpData[pattern] : null

							if (drillInfo) {
								if (drillInfo.drill1) materialGroups[item.partNumberMaterial].drills.add(`D1: ${drillInfo.drill1}`)
								if (drillInfo.drill2) materialGroups[item.partNumberMaterial].drills.add(`D2: ${drillInfo.drill2}`)
							}
						}
					})
				}
			})
		}

		return <Modal
			show={this.state.showDrillModal}
			onHide={() => this.setState({ showDrillModal: false })}
			dialogClassName="modal-90w"
			centered
		>
			<Modal.Header closeButton>
				<Modal.Title>Drill Verification Overview</Modal.Title>
			</Modal.Header>
			<Modal.Body style={{ maxHeight: "80vh", overflowY: 'auto' }}>

				{/* First Table: Part Numbers with Patterns and Materials */}
				<h5>Part Numbers with Patterns and Materials</h5>
				<div className='table-responsive mb-4'>
					<table className='table table-grey-border m-0 table-elements-sm'>
						<thead style={{ backgroundColor: "black", color: "white" }}>
							<tr>
								<th className='table-elem-sm'>Part Number</th>
								<th className='table-elem-sm'>Pattern</th>
								<th className='table-elem-sm'>Panel Number</th>
								<th className='table-elem-sm'>Material</th>
								<th className='table-elem-sm'>Drill 1</th>
								<th className='table-elem-sm'>Drill 2</th>
								<th className='table-elem-sm'>Description</th>
							</tr>
						</thead>
						<tbody>
							{partNumbers && partNumbers.map((pn, pnIndex) => {
								const patterns = ctcData && ctcData[pn.partNumber] ? ctcData[pn.partNumber] : []

								if (patterns.length === 0) {
									return (
										<tr key={`${pn.partNumber}-${pnIndex}`}>
											<td className='table-elem-sm'>{pn.partNumber}</td>
											<td className='table-elem-sm' colSpan="6" style={{ color: 'red', fontStyle: 'italic' }}>
												No CTC data found
											</td>
										</tr>
									)
								}

								return patterns.map((pattern, patternIndex) => {
									const drillInfo = drillEmpData && pattern.pattern ?
										drillEmpData[pattern.pattern.trim().toUpperCase()] : null

									return (
										<tr key={`${pn.partNumber}-${patternIndex}`}>
											{patternIndex === 0 && (
												<td className='table-elem-sm' rowSpan={patterns.length}>
													{pn.partNumber}
												</td>
											)}
											<td className='table-elem-sm'>{pattern.pattern || 'N/A'}</td>
											<td className='table-elem-sm'>{pattern.panelNumber || 'N/A'}</td>
											<td className='table-elem-sm'>{pattern.partNumberMaterial || 'N/A'}</td>
											<td className='table-elem-sm' style={{
												backgroundColor: drillInfo?.drill1 ? '#d4edda' : '#f8d7da',
												color: drillInfo?.drill1 ? '#155724' : '#721c24'
											}}>
												{drillInfo?.drill1 || 'Not found'}
											</td>
											<td className='table-elem-sm' style={{
												backgroundColor: drillInfo?.drill2 ? '#d4edda' : '#f8d7da',
												color: drillInfo?.drill2 ? '#155724' : '#721c24'
											}}>
												{drillInfo?.drill2 || 'Not found'}
											</td>
											<td className='table-elem-sm'>{pattern.partNumberMaterialDescription || 'N/A'}</td>
										</tr>
									)
								})
							})}
						</tbody>
					</table>
				</div>

				{/* Second Table: Materials with Required Drills */}
				<h5>Materials with Required Drills</h5>
				<div className='table-responsive'>
					<table className='table table-grey-border m-0 table-elements-sm'>
						<thead style={{ backgroundColor: "#4a4a4a", color: "white" }}>
							<tr>
								<th className='table-elem-sm'>Part Number Material</th>
								<th className='table-elem-sm'>Description</th>
								<th className='table-elem-sm'>Required Drills</th>
							</tr>
						</thead>
						<tbody>
							{Object.keys(materialGroups).sort().map((material, index) => (
								<tr key={material} style={index % 2 === 1 ? { backgroundColor: "#f2f2f2" } : {}}>
									<td className='table-elem-sm'>{material}</td>
									<td className='table-elem-sm'>{materialGroups[material].description}</td>
									<td className='table-elem-sm'>
										{Array.from(materialGroups[material].drills).sort().join(', ') || 'No drills required'}
									</td>
								</tr>
							))}
							{Object.keys(materialGroups).length === 0 && (
								<tr>
									<td className='table-elem-sm' colSpan="3" style={{ textAlign: 'center', fontStyle: 'italic' }}>
										No material data available
									</td>
								</tr>
							)}
						</tbody>
					</table>
				</div>
			</Modal.Body>
		</Modal>
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

	renderRapportPlacement = () => {
		return <div>
			<h2>Rapport Placement</h2>
			<div className='d-flex'>
				<textarea rows={1} value="" onChange={event => {
					let arr = this.state.modalObj.cuttingPlanRapportPlacements ? [...this.state.modalObj.cuttingPlanRapportPlacements] : []
					let error = []
					console.log(this.state.modalObj)
					let pnObj = {}
					this.state.modalObj.cuttingPlanPartNumbers.map(pn => {
						pnObj[pn.partNumber] = pn.quantity
					})
					let arrMaterials = [...this.state.modalObj.cuttingPlanMaterials]
					console.log(event.target.value)

					event.target.value.split("\n").map(e => {
						if (e.length > 0) {
							let arrInfo = e.split("\t"), elem = {}
							elem.nomPlct = arrInfo[0]
							elem.nomDefPlct = arrInfo[1]
							elem.longueur = arrInfo[2]
							elem.largeur = arrInfo[3]
							elem.perimetreTotal = arrInfo[4]
							elem.numeroDefPlct = arrInfo[5]
							elem.description = arrInfo[6]
							elem.modeles = arrInfo[7]
							elem.contraintes = arrInfo[8]
							elem.na = arrInfo[9]
							elem.surfaceTotal = arrInfo[10]
							elem.efficience = arrInfo[11]
							elem.pertePercentage = arrInfo[12]
							elem.annotation = arrInfo[13]
							elem.crans = arrInfo[14]
							elem.nbrDeModeles = arrInfo[15]
							elem.placeTailleQt = arrInfo[16]
							elem.totalTailles = arrInfo[17]
							elem.piecesRestantes = arrInfo[18]
							elem.ajoutPieces = arrInfo[19]
							arr.push(elem)
							try {
								let indMat = arrMaterials.findIndex(mat => mat.cuttingPlanMaterialPlacement.map(plct => plct.placement).includes(elem.nomDefPlct))
								let reftissu = arrMaterials[indMat].partNumberMaterial
								if (elem.numeroDefPlct.trim().toUpperCase() !== arrMaterials[indMat].partNumberMaterial) {
									error.push(elem.nomDefPlct + " n'utilise pas le bon reftissu (" + arrMaterials[indMat].partNumberMaterial + "=/=" + elem.numeroDefPlct.trim().toUpperCase() + ")")
								}
								let indPc = arrMaterials[indMat].cuttingPlanMaterialPlacement.findIndex(plct => plct.placement.trim().toUpperCase() === elem.nomDefPlct.trim().toUpperCase())
								let lg = this.convertFloat(elem.longueur.replace("M", ""), 3);
								arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].longueur = lg

								let machine = arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].machine
								let margesConfig = this.getMarginConfig(lg, reftissu, machine)
								if (margesConfig) {
									arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].pliesConfigMarge = margesConfig.pliesConfig
								} else {
									error.push("Aucune marge trouvée pour la longueur " + lg + " du placement " + elem.nomDefPlct)
								}
								// arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].perimetre = this.convertFloat(elem.perimetreTotal.replace("M", ""), 3)
								// arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].tempsDeCoupe = this.convertFloat(arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].perimetre / (arrMaterials[indMat].vitesse), 3)

								let qteList = elem.placeTailleQt.replace(")", "").split("|").map(qteElem => qteElem.split("("))
								let partNumbers = []
								qteList.map((qte, qteInd) => {
									partNumbers.push([elem.modeles.split(", ")[qteInd], parseInt(qte[0]) * parseInt(qte[1])])
								})
								arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].partNumbers = partNumbers.map(pn => pn.join(":")).join(", ")
								let maxNbrCouche = 0
								partNumbers.map(pn => {
									let couchePn = Math.ceil(pnObj[pn[0]] / parseInt(pn[1]))
									console.log(pn[0] + " " + pn[1] + " : " + couchePn)
									if (couchePn > maxNbrCouche) {
										maxNbrCouche = couchePn;
									}
								})
								arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].nbrCouche = maxNbrCouche
								let config = null;
								if (this.state.partNumberMaterialConfigs[reftissu] && arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].machine) {
									let configObj = this.state.partNumberMaterialConfigs[reftissu].reftissuMachines.find(e => e.machineType == arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].machine)
									if (configObj != null) {
										let configArr = configObj.pliesConfig.split("|").map(e => { return [parseInt(e.split(";")[0]), e.split(";")[1]] }).sort((a, b) => a[0] - b[0])
										for (let i = 0; i < configArr.length; i++) {
											if (configArr[i][0] <= arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].nbrCouche) {
												config = configArr[i][1]
											}
										}
									}
								}
								arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].config = config

								if (elem.perimetreTotal) {
									arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].perimetre = this.convertFloat(elem.perimetreTotal.replace("M", ""), 3)
									let vitesseObj = this.state.optionsList.cuttingSpeed.find(e => e.value === config)
									if (vitesseObj) {
										arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].tempsDeCoupe = this.convertFloat(arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].perimetre / (vitesseObj.item.vitesse * 100), 3)
									} else {
										arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].tempsDeCoupe = this.convertFloat(arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].perimetre / (arrMaterials[indMat].vitesse), 3)
									}
								} else {
									arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].perimetre = null
									arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].tempsDeCoupe = null
								}

								if (lg) {
									let lgMatelasTotal = 0
									let arrDrill = (arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
									let maxPlie = arrDrill[0] == null && arrDrill[1] == null ? arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].maxPlie : arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].maxPlieDrill
									let nbrCouche = arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].nbrCouche
									if (this.state.partNumberMaterialConfigs[reftissu].plaque > 0) {
										lgMatelasTotal += this.state.partNumberMaterialConfigs[reftissu].plaque * nbrCouche
									} else {
										// if (nbrCouche % maxPlie > 0) {
										// 	let marge = this.getMarge(lg, (nbrCouche % maxPlie), reftissu)
										// 	if (marge == null) {
										// 		error.push("la longueur " + lg + " dépasse l'interval pour " + reftissu)
										// 	} else {
										// 		lgMatelasTotal += (lg + this.getMarge(lg, (nbrCouche % maxPlie), reftissu)) * (nbrCouche % maxPlie)
										// 	}
										// }
										// if (nbrCouche / maxPlie >= 1) {
										// 	let marge = this.getMarge(lg, maxPlie, reftissu)
										// 	if (marge == null) {
										// 		error.push("la longueur " + lg + " dépasse l'interval pour " + reftissu)
										// 		lgMatelasTotal += (lg + this.getMarge(lg, maxPlie, reftissu)) * (parseInt(nbrCouche / maxPlie)) * maxPlie
										// 	}
										// }
										let marge = this.getMarge(lg, maxPlie, reftissu, machine)
										if (marge === null || marge === undefined) {
											error.push("Aucune marge trouvée pour la longueur " + lg + " du placement " + elem.nomDefPlct)
										} else {
											lgMatelasTotal += (lg + this.getMarge(lg, Math.min(maxPlie, nbrCouche), reftissu, machine)) * nbrCouche

										}
									}
									arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].longueurMatelas = lgMatelasTotal
								}

							} catch (err) {
								console.log(err)
							}
						}
					})

					this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportPlacements: arr, cuttingPlanMaterials: arrMaterials }, error: error.length > 0 ? error : null })
				}} />
				{this.state.modalObj.cuttingPlanRapportPlacements && this.state.modalObj.cuttingPlanRapportPlacements.length > 0 && <button className='btn btn-outline-danger' style={{ fontSize: 12 }}
					onClick={() => {
						if (this.state.modalObj.id) {
							axios.post(`/api/cuttingPlanRapportFunctions/rapport-placement/cp/${this.state.modalObj.id}`)
								.then(() => {
									this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportPlacements: [] } })
								})
						} else {
							this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportPlacements: [] } })
						}
					}}
				>Supprimer tous</button>}
				{
					// this.state.modalObj.cuttingPlanRapportPlacements &&  this.state.modalObj.cuttingPlanRapportPlacements.length == 0 &&
					<button
						className='btn btn-primary' style={{ fontSize: 12 }}
						onClick={() => this.generateByPlacement()}
					>
						Générer par placement
					</button>}
			</div>

			<div className='mb-2 table-responsive'>
				<table className='table table-grey-border m-0 entity-table-sm-12'>
					<thead style={{ backgroundColor: "#ababab", color: "white" }}>
						<tr>
							<th className='table-elem-sm '>Nom plct</th>
							<th className='table-elem-sm '>Nom def plct</th>
							<th className='table-elem-sm '>Long</th>
							<th className='table-elem-sm '>Largeur</th>
							<th className='table-elem-sm '>Perimetre total</th>
							<th className='table-elem-sm '>Numero def plct</th>
							<th className='table-elem-sm '>Description</th>
							<th className='table-elem-sm '>Modeles</th>
							<th className='table-elem-sm '>Contraintes</th>
							<th className='table-elem-sm '>n/a</th>
							<th className='table-elem-sm '>Surface total</th>
							<th className='table-elem-sm '>Efficience</th>
							<th className='table-elem-sm '>Perte %</th>
							<th className='table-elem-sm '>Annotation</th>
							<th className='table-elem-sm '>Crans</th>
							<th className='table-elem-sm '>Nb de Modeles</th>
							<th className='table-elem-sm '>PlacetailleQt</th>
							<th className='table-elem-sm '>Total tailles</th>
							<th className='table-elem-sm '>Pieces restantes</th>
							<th className='table-elem-sm '>Ajout pieces</th>
							<th className='table-elem-sm '>D1</th>
							<th className='table-elem-sm '>D2</th>
							<th></th>
						</tr>
					</thead>
					<tbody>
						{this.state.modalObj.cuttingPlanRapportPlacements && this.state.modalObj.cuttingPlanRapportPlacements.map((elem, ind) => {
							return <tr>
								<td className='table-elem-sm'>{elem.nomPlct}</td>
								<td className='table-elem-sm'>{elem.nomDefPlct}</td>
								<td className='table-elem-sm'>{elem.longueur}</td>
								<td className='table-elem-sm'>{elem.largeur}</td>
								<td className='table-elem-sm'>{elem.perimetreTotal}</td>
								<td className='table-elem-sm'>{elem.numeroDefPlct}</td>
								<td className='table-elem-sm'>{elem.description}</td>
								<td className='table-elem-sm'>{elem.modeles}</td>
								<td className='table-elem-sm'>{elem.contraintes}</td>
								<td className='table-elem-sm'>{elem.na}</td>
								<td className='table-elem-sm'>{elem.surfaceTotal}</td>
								<td className='table-elem-sm'>{elem.efficience}</td>
								<td className='table-elem-sm'>{elem.pertePercentage ? elem.pertePercentage.split(" ").map((e, i) => {
									if (elem.pertePercentage.split(" ").length == i + 1) return <span key={"pert-" + i} style={{ whiteSpace: "nowrap" }} >{e}</span>
									return [<span key={"pert-" + i} style={{ whiteSpace: "nowrap" }} >{e}</span>, <br />]
								}) : ""}</td>
								<td className='table-elem-sm'>{elem.annotation}</td>
								<td className='table-elem-sm'>{elem.crans}</td>
								<td className='table-elem-sm'>{elem.nbrDeModeles}</td>
								<td className='table-elem-sm'>{elem.placeTailleQt}</td>
								<td className='table-elem-sm'>{elem.totalTailles}</td>
								<td className='table-elem-sm'>{elem.piecesRestantes}</td>
								<td className='table-elem-sm'>{elem.ajoutPieces}</td>
								<td className='table-elem-sm'>{elem.drill1 ? <FontAwesomeIcon icon={faCheck} color="green" /> : <FontAwesomeIcon icon={faTimes} color="red" />}</td>
								<td className='table-elem-sm'>{elem.drill2 ? <FontAwesomeIcon icon={faCheck} color="green" /> : <FontAwesomeIcon icon={faTimes} color="red" />}</td>

								<td className='table-elem-sm'><button className='btn btn-outline-dark' style={{ padding: "2 12" }} onClick={() => {
									if (elem.id) {
										axios.post(`/api/cuttingPlan/rapport-placement/${elem.id}`)
											.then(() => {
												let arr = [...this.state.modalObj.cuttingPlanRapportPlacements]
												arr.splice(ind, 1)
												this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportPlacements: arr } })
											})
									} else {
										let arr = [...this.state.modalObj.cuttingPlanRapportPlacements]
										arr.splice(ind, 1)
										this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportPlacements: arr } })
									}
								}}><FontAwesomeIcon icon={faTrashAlt} /></button></td>
							</tr>
						})}
					</tbody>
				</table>
			</div>
		</div>
	}

	renderRapportModel = () => {
		return <div>
			<h2>Rapport Modéle</h2>
			<div className='d-flex'>
				<textarea rows={1} value="" onChange={event => {
					let arr = this.state.modalObj.cuttingPlanRapportModels || []
					let arrMaterials = [...this.state.modalObj.cuttingPlanMaterials]


					event.target.value.split("\n").map(e => {
						if (e.length > 0) {
							let arrInfo = e.split("\t"), elem = {}
							elem.nomPlct = arrInfo[0]
							elem.nomModele = arrInfo[1]
							elem.tissu = arrInfo[2]
							elem.quantite = arrInfo[3]
							elem.optionModele = arrInfo[4]
							elem.codeTaille = arrInfo[5]
							elem.alteration = arrInfo[6]
							elem.dynamique = arrInfo[7]
							elem.nbrModele = arrInfo[8]
							elem.piecesParPaquets = arrInfo[9]
							elem.nbrDeTailles = arrInfo[10]
							elem.ajoutPiece = arrInfo[11]
							elem.taille = arrInfo[12]
							elem.direction = arrInfo[13]
							elem.longueurPlct = arrInfo[14]
							elem.longueurPlctPerc = arrInfo[15]
							elem.dateHeure = arrInfo[16]
							elem.dernModifUtili = arrInfo[17]
							elem.heureDeCreation = arrInfo[18]
							elem.creationUtili = arrInfo[19]
							elem.heureModelePrec = arrInfo[20]
							elem.modifUtilPrecedente = arrInfo[21]
							arr.push(elem)

						}
					})
					this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportModels: arr } })
				}} />
				{this.state.modalObj.cuttingPlanRapportModels && this.state.modalObj.cuttingPlanRapportModels.length > 0 && <button className='btn btn-outline-danger' style={{ fontSize: 12 }}
					onClick={() => {
						if (this.state.modalObj.id) {
							axios.post(`/api/cuttingPlanRapportFunctions/rapport-modele/cp/${this.state.modalObj.id}`)
								.then(() => {
									this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportModels: [] } })
								})
						} else {
							this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportModels: [] } })
						}
					}}
				>Supprimer tous</button>}
			</div>

			<div className='mb-2 table-responsive'>
				<table className='table table-grey-border m-0 entity-table-sm-12'>
					<thead style={{ backgroundColor: "#ababab", color: "white" }}>
						<tr>
							<th className='table-elem-sm '>Nom plct</th>
							<th className='table-elem-sm '>Nom modele</th>
							<th className='table-elem-sm '>Tissu</th>
							<th className='table-elem-sm '>Quantite</th>
							<th className='table-elem-sm '>Option modele</th>
							<th className='table-elem-sm '>Code taille</th>
							<th className='table-elem-sm '>Alteration</th>
							<th className='table-elem-sm '>Dynamique</th>
							<th className='table-elem-sm '>Nb modele</th>
							<th className='table-elem-sm '>Pieces Par paquets</th>
							<th className='table-elem-sm '>Nb de tailles</th>
							<th className='table-elem-sm '>Ajout piece/paquets</th>
							<th className='table-elem-sm '>Taille</th>
							<th className='table-elem-sm '>Direction</th>
							<th className='table-elem-sm '>Longueur plct</th>
							<th className='table-elem-sm '>Longueur plct %</th>
							<th className='table-elem-sm '>Date/Heure</th>
							<th className='table-elem-sm '>Dern modif utili</th>
							<th className='table-elem-sm '>Heure de creation</th>
							<th className='table-elem-sm '>Creation utili</th>
							<th className='table-elem-sm '>Heure modele prec</th>
							<th className='table-elem-sm '>Modif util precedente</th>
							<th className='table-elem-sm '></th>
						</tr>
					</thead>
					<tbody>
						{this.state.modalObj.cuttingPlanRapportModels && this.state.modalObj.cuttingPlanRapportModels.map((elem, ind) => {
							return <tr>
								<td className='table-elem-sm'>{elem.nomPlct}</td>
								<td className='table-elem-sm'>{elem.nomModele}</td>
								<td className='table-elem-sm'>{elem.tissu}</td>
								<td className='table-elem-sm'>{elem.quantite}</td>
								<td className='table-elem-sm'>{elem.optionModele}</td>
								<td className='table-elem-sm'>{elem.codeTaille}</td>
								<td className='table-elem-sm'>{elem.alteration}</td>
								<td className='table-elem-sm'>{elem.dynamique}</td>
								<td className='table-elem-sm'>{elem.nbrModele}</td>
								<td className='table-elem-sm'>{elem.piecesParPaquets}</td>
								<td className='table-elem-sm'>{elem.nbrDeTailles}</td>
								<td className='table-elem-sm'>{elem.ajoutPiece}</td>
								<td className='table-elem-sm'>{elem.taille}</td>
								<td className='table-elem-sm'>{elem.direction}</td>
								<td className='table-elem-sm'>{elem.longueurPlct}</td>
								<td className='table-elem-sm'>{elem.longueurPlctPerc}</td>
								<td className='table-elem-sm'>{elem.dateHeure}</td>
								<td className='table-elem-sm'>{elem.dernModifUtili}</td>
								<td className='table-elem-sm'>{elem.heureDeCreation}</td>
								<td className='table-elem-sm'>{elem.creationUtili}</td>
								<td className='table-elem-sm'>{elem.heureModelePrec}</td>
								<td className='table-elem-sm'>{elem.modifUtilPrecedente}</td>
								<td className='table-elem-sm'><button className='btn btn-outline-dark' style={{ padding: "2 12" }}
									onClick={() => {
										if (elem.id) {
											axios.post(`/api/cuttingPlanRapportFunctions/rapport-modele/cp/${elem.id}`)
												.then(() => {
													let arr = [...this.state.modalObj.cuttingPlanRapportModels]
													arr.splice(ind, 1)
													this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportModels: arr } })
												})
										} else {
											let arr = [...this.state.modalObj.cuttingPlanRapportModels]
											arr.splice(ind, 1)
											this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportModels: arr } })
										}
									}}><FontAwesomeIcon icon={faTrashAlt} /></button></td>
							</tr>
						})}
					</tbody>
				</table>
			</div>
		</div>
	}

	renderRapportDrill = () => {
		return <div>
			<h2>Rapport Drills</h2>
			<div className='d-flex' >
				<textarea rows={1} value="" onChange={event => {
					let arr = this.state.modalObj.cuttingPlanRapportDrills || []
					console.log(event.target.value)
					event.target.value.split("\n").map(e => {
						if (e.length > 0) {
							let arrInfo = e.split("\t"), elem = {}

							elem.nomPlacement = arrInfo[0]
							elem.pointsAtributs = arrInfo[1]
							elem.pointAttrbQt = arrInfo[2]
							elem.typeDeCrans = arrInfo[3]
							elem.qtCrans = arrInfo[4]
							elem.labelInterne = arrInfo[5]
							elem.qtInterne = arrInfo[6]

							arr.push(elem)
						}
					})

					this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportDrills: arr } })
				}} />
				{this.state.modalObj.cuttingPlanRapportDrills && this.state.modalObj.cuttingPlanRapportDrills.length > 0 && <button className='btn btn-outline-danger' style={{ fontSize: 12 }}
					onClick={() => {
						if (this.state.modalObj.id) {
							axios.post(`/api/cuttingPlanRapportFunctions/rapport-drill/cp/${this.state.modalObj.id}`)
								.then(() => {
									this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportDrills: [] } })
								})

						} else {
							this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportDrills: [] } })

						}
					}}
				>Supprimer tous</button>}
			</div>

			<div className='mb-2 table-responsive'>
				<table className='table table-grey-border m-0 entity-table-sm-12'>
					<thead style={{ backgroundColor: "#ababab", color: "white" }}>
						<tr>
							<th className='table-elem-sm '>Nom placement</th>
							<th className='table-elem-sm '>Points attributs</th>
							<th className='table-elem-sm '>Point Attrib Qt</th>
							<th className='table-elem-sm '>Type de crans</th>
							<th className='table-elem-sm '>Qt crans</th>
							<th className='table-elem-sm '>Label internes</th>
							<th className='table-elem-sm '>Qt interne</th>
							<th className='table-elem-sm '></th>
						</tr>
					</thead>
					<tbody>
						{this.state.modalObj.cuttingPlanRapportDrills && this.state.modalObj.cuttingPlanRapportDrills.map((elem, ind) => {
							return <tr>
								<td className='table-elem-sm'>{elem.nomPlacement}</td>
								<td className='table-elem-sm'>{elem.pointsAtributs}</td>
								<td className='table-elem-sm'>{elem.pointAttrbQt}</td>
								<td className='table-elem-sm'>{elem.typeDeCrans}</td>
								<td className='table-elem-sm'>{elem.qtCrans}</td>
								<td className='table-elem-sm'>{elem.labelInterne}</td>
								<td className='table-elem-sm'>{elem.qtInterne}</td>
								<td className='table-elem-sm'><button className='btn btn-outline-dark' style={{ padding: "2 12" }}
									onClick={() => {
										if (elem.id) {
											axios.post(`/api/cuttingPlan/rapport-drill/${elem.id}`)
												.then(() => {
													let arr = [...this.state.modalObj.cuttingPlanRapportModels]
													arr.splice(ind, 1)
													this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportModels: arr } })
												})
												.catch(err => {
													this.setState({ error: err.response.data })
												})
										} else {
											let arr = [...this.state.modalObj.cuttingPlanRapportModels]
											arr.splice(ind, 1)
											this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportModels: arr } })
										}

									}}><FontAwesomeIcon icon={faTrashAlt} /></button></td>
							</tr>
						})}
					</tbody>
				</table>
			</div>
		</div>
	}

	renderConsommation = () => {
		try {
			if (this.state.modalObj.cuttingPlanRapportModels == null || this.state.modalObj.cuttingPlanRapportModels.length === 0) return
			let arr = []
			let valuesObj = {}
			this.state.modalObj.cuttingPlanMaterials.map(cpm => {
				let placementList = cpm.cuttingPlanMaterialPlacement.map(e => e.placement.trim().toUpperCase())
				let objPlacements = {}
				cpm.cuttingPlanMaterialPlacement.map(cpmp => {
					if (cpmp.activated) {
						objPlacements[cpmp.placement.trim().toUpperCase()] = this.convertFloat(cpmp.longueurMatelas / cpmp.nbrCouche, 3)
					}
				})

				let obj = {}
				this.state.modalObj.cuttingPlanPartNumbers.map(cppn => {
					let somme = 0
					this.state.modalObj.cuttingPlanRapportModels
						.filter(e => placementList.includes(e.nomPlct.trim().toUpperCase()) && cppn.partNumber.trim().toUpperCase() == e.nomModele.trim().toUpperCase())
						.map(e => {
							if (objPlacements[e.nomPlct.trim().toUpperCase()] != null) {
								somme += this.convertFloat((objPlacements[e.nomPlct.trim().toUpperCase()] * parseFloat(e.longueurPlctPerc) * 0.01) / parseFloat(e.quantite), 3)
							}
						})
					obj[cppn.partNumber] = this.convertFloat(somme, 3)
				})
				arr.push({
					name: cpm.partNumberMaterial,
					...obj
				})
				valuesObj[cpm.partNumberMaterial] = { ...obj }
			})
			return <div>
				<h2 style={{ display: "flex" }}>
					<span>Somme des consommations</span>
					<span>
						<Switch id="consommation" name="consommation" checked={this.state.modalObj.consommation || false}
							className="react-switch mt-1 ml-2" offColor="#F00" disabled={this.state.loadingConsommation}
							onChange={(checked) => {
								if (this.state.modalObj.id) {
									if (window.confirm("Voulez-vous " + (checked ? "activer" : "désactiver") + " la consommation le plan de coupe " + this.state.modalObj.description + " ?")) {
										this.setState({ loadingConsommation: true })
										if (checked) {
											axios.post(`/api/cuttingPlan/enable-consommation/${this.state.modalObj.id}`)
												.then((res) => {
													this.setState({ loadingConsommation: false, modalObj: { ...this.state.modalObj, consommation: checked } })
												})
												.catch(err => {
													this.setState({ loadingConsommation: false })
												})
										} else {
											axios.post(`/api/cuttingPlan/disable-consommation/${this.state.modalObj.id}`)
												.then((res) => {
													this.setState({ loadingConsommation: false, modalObj: { ...this.state.modalObj, consommation: checked } })
												})
												.catch(err => {
													this.setState({ loadingConsommation: false })
												})
										}
									}
								} else {
									this.setState({ modalObj: { ...this.state.modalObj, consommation: checked } })
								}

							}}
						/>
					</span>
				</h2>
				<div>
					<div className='mb-2 table-responsive'>
						<table className='table table-grey-border m-0 entity-table-sm-12'>
							<thead style={{ backgroundColor: "#ababab", color: "white" }}>
								<tr>
									<th className='table-elem-sm '>Row Labels</th>
									{this.state.modalObj.cuttingPlanPartNumbers.map(cppn => {
										return <th className='table-elem-sm '>{cppn.partNumber}</th>
									})}
								</tr>
							</thead>
							<tbody>
								{this.state.modalObj.cuttingPlanMaterials.map(cpm => {
									// let placementList = cpm.cuttingPlanMaterialPlacement.map(e => e.placement.trim().toUpperCase())
									// let objPlacements = {}
									// cpm.cuttingPlanMaterialPlacement.map(cpmp => {
									// 	objPlacements[cpmp.placement] = this.convertFloat(cpmp.longueurMatelas / cpmp.nbrCouche, 3)
									// })
									return <tr>
										<td>{cpm.partNumberMaterial}</td>
										{this.state.modalObj.cuttingPlanPartNumbers.map(cppn => {
											// let somme = 0
											// this.state.modalObj.cuttingPlanRapportModels
											// 	.filter(e => placementList.includes(e.nomPlct.trim().toUpperCase()) && cppn.partNumber.trim().toUpperCase() == e.nomModele.trim().toUpperCase())
											// 	.map(e => {
											// 		somme += this.convertFloat((objPlacements[e.nomPlct.trim().toUpperCase()] * parseFloat(e.longueurPlctPerc) * 0.01) / parseFloat(e.quantite), 3)
											// 	})
											let somme = valuesObj[cpm.partNumberMaterial][cppn.partNumber.trim().toUpperCase()] || 0
											return <th className='table-elem-sm' style={somme == 0 ? { backgroundColor: "#c9c9c9" } : {}} >{somme > 0 && this.convertFloat(somme, 3)}</th>
										})}
									</tr>
								})}
							</tbody>
						</table>
					</div>
					<div style={{ height: 400 }}>
						<ResponsiveContainer width="100%" height="100%">
							<BarChart width={730} height={250} data={arr}>
								<CartesianGrid strokeDasharray="3 3" />
								<XAxis dataKey="name" />
								<YAxis />
								<Tooltip />
								<Legend />
								{this.state.modalObj.cuttingPlanPartNumbers.map((cppn, i) => {
									return <Bar dataKey={cppn.partNumber} fill={allColors[i]} />
								})}
							</BarChart>
						</ResponsiveContainer>
					</div>

				</div>
			</div>
		} catch {

		}
	}

	renderDetailModal = () => {
		let arrPlacement = []
		if (this.state.modalDetail && this.state.modalDetail.cuttingPlanMaterials) {
			try {
				this.state.modalDetail.cuttingPlanMaterials.map(cpm => {
					cpm.cuttingPlanMaterialPlacement.sort((a, b) => a.groupPlacement - b.groupPlacement || Number(b.activated) - Number(a.activated)).map(cpmp => {
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
							let marge = this.getMarge(cpmp.longueur, couche, cpm.partNumberMaterial, cpmp.machine)

							let obj = {
								partNumberMaterial: cpm.partNumberMaterial,
								description: cpm.description,
								sens: cpm.matelassageEndroit,
								longueur: marge ? (cpm.plaque ? cpm.plaque : this.convertFloat((cpmp.longueur + marge), 3)) : null,
								nbrCouche: couche,
								laize: cpmp.laize,
								placement: cpmp.placement,
								config: config,
								drill1: arrDrill[0],
								drill2: arrDrill[1],
								machine: cpmp.machine,
								groupPlacement: cpmp.groupPlacement,
								activated: cpmp.activated
							}
							arrPlacement.push(obj)
							nbrCouche -= couche
						}
					})
				})
				console.log({ arrPlacement })
			} catch {
			}
		}

		return <Modal
			show={this.state.modalDetail != null}
			onHide={() => this.setState({ modalDetail: null })}
			dialogClassName="modal-90w"
			centered
		>
			{this.state.modalDetail && <div style={{ maxHeight: "80vh", overflowY: 'auto' }}>
				<h4 className='text-center my-2'>Détails du plan de coupe {this.state.modalDetail.id}</h4>
				<hr />

				<div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Projet : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%" }}>{this.state.modalDetail.projet}</div>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Version : </strong></div>
						<div className='' style={{ width: "35%" }}>{this.state.modalDetail.version}</div>
					</div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>description : </strong></div>
						<div className='text-no-wrap' style={{ width: "85%" }}>{this.state.modalDetail.description}</div>
						{/* <div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>version2 : </strong></div>
						<div className='' style={{ width: "35%" }}>{this.state.modalDetail.version2}</div> */}
					</div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Definition : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%" }}>{this.state.modalDetail.definition}</div>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>commentaire : </strong></div>
						<div className='' style={{ width: "35%" }}>{this.state.modalDetail.commentaire}</div>
					</div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Active : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%", marginTop: "5" }}> {this.state.modalDetail.enabled ? <FontAwesomeIcon icon={faCheck} color="green" /> : <FontAwesomeIcon icon={faTimes} color="red" />}</div>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong></strong></div>
						<div className='' style={{ width: "35%" }}></div>
					</div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Créer par : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%" }}>{this.state.modalDetail.createdBy && <span>{this.state.modalDetail.createdBy.lastName} {this.state.modalDetail.createdBy.firstName} ({this.state.modalDetail.createdBy.matricule}) </span>}{this.state.modalDetail.createdAt}</div>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Dernière modification par : </strong></div>
						<div className='' style={{ width: "35%" }}>{this.state.modalDetail.updatedBy && <span>{this.state.modalDetail.updatedBy.lastName} {this.state.modalDetail.updatedBy.firstName} ({this.state.modalDetail.updatedBy.matricule})</span>} {this.state.modalDetail.updatedAt}</div>
					</div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Start Date : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%" }}>{this.state.modalDetail.startDate}</div>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>End Date : </strong></div>
						<div className='' style={{ width: "35%" }}>{this.state.modalDetail.endDate}</div>
					</div>
				</div>
				<div>
					<table className='table m-0 table table-grey-border'>
						<thead style={{ backgroundColor: "black", color: "white" }}>
							<tr>
								<th className='table-elem-sm'>Part Number</th>
								<th className='table-elem-sm'>Description</th>
								<th className='table-elem-sm'>Kit textil</th>
								<th className='table-elem-sm'>Quantité</th>
							</tr>
						</thead>
						<tbody>
							{this.state.modalDetail.cuttingPlanPartNumbers && this.state.modalDetail.cuttingPlanPartNumbers.map(elemPn => <tr>
								<td className='table-elem-sm'>{elemPn.partNumber}</td>
								<td className='table-elem-sm'>{elemPn.description}</td>
								<td className='table-elem-sm'>{elemPn.item}</td>
								<td className='table-elem-sm'>{elemPn.quantity}</td>
							</tr>)}
						</tbody>
					</table>
				</div>
				<div>
					<table className='table m-0 table table-grey-border'>
						<thead style={{ backgroundColor: "black", color: "white" }}>
							<tr>
								<th className='table-elem-sm'>partNumberMaterial</th>
								<th className='table-elem-sm'>Description</th>
								<th className='table-elem-sm'>sens</th>
								<th className='table-elem-sm'>longueur</th>
								<th className='table-elem-sm'>nbrCouche</th>
								<th className='table-elem-sm'>laize</th>
								<th className='table-elem-sm'>placement</th>
								<th className='table-elem-sm'>config</th>
								<th className='table-elem-sm'>drill1</th>
								<th className='table-elem-sm'>drill2</th>
								<th className='table-elem-sm'>machine</th>
								{/* <th className='table-elem-sm'>groupPlacement</th> */}
								{/* <th className='table-elem-sm'>activated</th> */}
							</tr>
						</thead>
						<tbody>
							{arrPlacement && arrPlacement.map(elemPn => <tr style={elemPn.activated === false ? { backgroundColor: "#ffffc0" } : { backgroundColor: "" }}>
								<td className='table-elem-sm'>{elemPn.partNumberMaterial}</td>
								<td className='table-elem-sm'>{elemPn.description}</td>
								<td className='table-elem-sm'>{elemPn.sens}</td>
								<td className='table-elem-sm'>{elemPn.longueur && elemPn.longueur.toFixed(3)}</td>
								<td className='table-elem-sm'>{elemPn.nbrCouche}</td>
								<td className='table-elem-sm'>{elemPn.laize}</td>
								<td className='table-elem-sm'>{elemPn.placement}</td>
								<td className='table-elem-sm'>{elemPn.config}</td>
								<td className='table-elem-sm'>{elemPn.drill1}</td>
								<td className='table-elem-sm'>{elemPn.drill2}</td>
								<td className='table-elem-sm'>{elemPn.machine}</td>
								{/* <td className='table-elem-sm'>{elemPn.groupPlacement}</td> */}
								{/* <td className='table-elem-sm'>{elemPn.activated ? "true": "false"}</td> */}
							</tr>)}
						</tbody>
					</table>
				</div>
				<hr />
				<div className='d-flex pb-2'>
					<button className='btn btn-link' onClick={() => { this.setState({ modalDetail: null }) }}> <FontAwesomeIcon icon={faArrowLeft} /> Retour</button>
				</div>
			</div>}
		</Modal>
	}

	renderHistorique = () => {
		if (this.state.modalObj.id == null) {
			return ""
		}
		return <div className="">
			<button className='btn btn-outline-danger' onClick={() => {
				axios.get(`/api/cuttingPlanHistory/cuttingPlan/${this.state.modalObj.id}`)
					.then(res => {
						this.setState({ arrHistory: res.data })
					})
					.catch(err => {
						this.setState({ error: err.response.data })
					})
			}}>
				Historique
			</button>
			{this.state.arrHistory && <div className='d-flex'>
				<div style={{ flex: 1 }}>
					<table className='table' >
						<thead>
							<tr>
								<th>User</th>
								<th>Date</th>
								<th></th>
							</tr>
						</thead>
						<tbody>
							{this.state.arrHistory.map((elem, ind) => {
								return <tr key={"row-" + ind} className="clickable-element" onClick={() => { this.setState({ selectedHistory: elem }) }}>
									<td>{elem.updatedBy ? elem.updatedBy.matricule : ""}</td>
									<td>{elem.createdAt}</td>
									<td className='p-0'>
										{
											elem.changes === "Enabled" ? <FontAwesomeIcon icon={faCheck} color="green" style={{ fontSize: 38 }} />
												: elem.changes === "Disabled" ? <FontAwesomeIcon icon={faTimes} color="red" style={{ fontSize: 38 }} />
													: <button className='btn btn-outline-dark btn-sm' onClick={() => {
														// i did use JSON.stringify(this.state.modalObj, null, 2) , know i want the reverse
														const parsedObj = JSON.parse(elem.changes)
														this.setState({ modalObj: { ...parsedObj, id: this.state.modalObj.id, cmsId: this.state.modalObj.cmsId } }, () => {
															// Load partNumberMaterialConfigs if they don't exist
															if (parsedObj.cuttingPlanMaterials && parsedObj.cuttingPlanMaterials.length > 0) {
																const missingConfigs = parsedObj.cuttingPlanMaterials.some(cpm => 
																	!this.state.partNumberMaterialConfigs[cpm.partNumberMaterial]
																)
																if (missingConfigs) {
																	// Load configs using the same method as chargerReftissuConfig
																	axios.get(`/api/partNumberMaterialConfig/pns/${parsedObj.cuttingPlanMaterials.map(e => e.partNumberMaterial).join(",")}?projet=${parsedObj.projet}`)
																		.then(res => {
																			let obj = { ...this.state.partNumberMaterialConfigs }
																			res.data.map(e => {
																				obj[e.partNumberMaterial] = { ...e }
																			})
																			this.setState({
																				partNumberMaterialConfigs: obj,
																			})
																		})
																}
															}
														})
														this.refForm.scrollIntoView({ behavior: 'smooth' })
													}}>Voir <FontAwesomeIcon icon={faArrowUp} /></button>}
									</td>
								</tr>
							})}
						</tbody>
					</table>
				</div>
				<div style={{ flex: 1 }}>
					{this.state.selectedHistory && <pre>{this.state.selectedHistory.changes}</pre>}
				</div>
			</div>}

		</div>
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
		cp.cuttingPlanPartNumbers.map(elem => {
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
							let marge = this.getMarge(cpmp.longueur, couche, cpm.partNumberMaterial, cpmp.machine)
							let obj = {
								partNumberMaterial: cpm.partNumberMaterial,
								description: cpm.description,
								matelassageEndroit: cpm.matelassageEndroit,
								partNumbers: cpmp.partNumbers,
								longueur: marge ? (cpm.plaque ? cpm.plaque : this.convertFloat((cpmp.longueur + marge), 3)) : null,
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

	renderImportModal = () => {
		let cuttingRequest = this.state.importModal
		if (cuttingRequest == null) return null
		return <Modal
			show={this.state.importModal !== null}
			onHide={() => this.setState({ importModal: null, selectedBoxs: [] })}
			dialogClassName="modal-90w"
			centered
		>
			{this.state.importModal !== null && <div style={{ maxHeight: "75vh", overflowY: 'auto' }}>
				<h4 className='text-center my-2'>Importation du plan de coupe</h4>
				<div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Sequence : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.sequence}</div>
					</div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Projet : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.projet}</div>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Version : </strong></div>
						<div className='' style={{ width: "35%" }}>{cuttingRequest.version}</div>
					</div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>modele : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.modele}</div>
					</div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>definition : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.definition}</div>
					</div>
					<div className='d-flex'>
						<label className='col-form-label text-no-wrap text-right' style={{ width: "15%" }}><strong>Zone : </strong></label>
						<div className='text-no-wrap' style={{ width: "200px" }}>
							<Select classNamePrefix="rs"
								placeholder={"Zone..."} className='p-0'
								isClearable={true}
								value={(this.state.optionsList.zone && this.state.optionsList.zone.length > 0 && cuttingRequest.zone)
									? { label: cuttingRequest.zone.nom, value: cuttingRequest.zone.nom, item: cuttingRequest.zone }
									: null
								}
								options={this.state.optionsList.zone}
								onChange={(option) => {
									this.setState({ importModal: { ...cuttingRequest, zone: (option ? option.item : null) } })
								}}
							/>
						</div>
					</div>
				</div>
				<div>
					<table className='table m-0 table table-grey-border'>
						<thead style={{ backgroundColor: "#ca0c0c", color: "white" }}>
							<tr>
								<th className='table-elem-sm'>Part Number</th>
								<th className='table-elem-sm'>Description</th>
								<th className='table-elem-sm'>Kit textil</th>
								<th className='table-elem-sm'>Quantité</th>
								<th className='table-elem-sm'>wo</th>
								<th className='table-elem-sm'>woid</th>
								<th className='table-elem-sm'>packageQty</th>

							</tr>
						</thead>
						<tbody>
							{cuttingRequest.cuttingRequestPartNumbers && cuttingRequest.cuttingRequestPartNumbers.map(elemPn => <tr
								className='clickable-element'
								onClick={() => this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber)
									? this.setState({ selectedBoxs: this.state.selectedBoxs.filter(e => e.partNumber != elemPn.partNumber) })
									: this.setState({ selectedBoxs: [...this.state.selectedBoxs, ...cuttingRequest.cuttingRequestBoxs.filter(e => e.partNumber == elemPn.partNumber)].sort((a, b) => a.id.localeCompare(b.id)) })
								}
								style={this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber) ? { backgroundColor: "rgb(207, 207, 207)" } : {}}
							>
								<td className='table-elem-sm'>{elemPn.partNumber}</td>
								<td className='table-elem-sm'>{elemPn.description}</td>
								<td className='table-elem-sm'>{elemPn.item}</td>
								<td className='table-elem-sm'>{elemPn.quantity}</td>
								<td className='table-elem-sm'>
									<Select classNamePrefix="rs"
										placeholder={"WO"} style={{ width: 100 }}
										isClearable={false}
										value={elemPn.wo ? { label: elemPn.wo, value: elemPn.wo } : null}
										options={this.state.optionsList.wo.filter(e => e.item.item.toUpperCase().trim() == elemPn.item.toUpperCase().trim())}
										onChange={(option) => {
											this.setState({ wo: option.item.wo, woid: option.item.woid })
										}}
									/>
								</td>
								<td className='table-elem-sm'>{elemPn.woid}</td>
								<td className='table-elem-sm'><strong>{elemPn.packageQty}</strong></td>

							</tr>)}
						</tbody>
					</table>
				</div>
				<div>
					<table className='table m-0 table table-grey-border'>
						<thead style={{ backgroundColor: "#ca0c0c", color: "white" }}>
							<tr>
								<th className='table-elem-sm'>serie</th>
								<th className='table-elem-sm'>partNumberMaterial</th>
								<th className='table-elem-sm'>Description</th>
								<th className='table-elem-sm'>matelassageEndroit</th>
								<th className='table-elem-sm'>longueur</th>
								<th className='table-elem-sm'>nbrCouche</th>
								<th className='table-elem-sm'>laize</th>
								<th className='table-elem-sm'>placement</th>
								<th className='table-elem-sm'>config</th>
								<th className='table-elem-sm'>drill1</th>
								<th className='table-elem-sm'>drill2</th>
								<th className='table-elem-sm'>machine</th>
								<th className='table-elem-sm'>
									{cuttingRequest.cuttingRequestSeries && cuttingRequest.cuttingRequestSeries.filter(e => e.serie != null).length > 0 && <button className='btn btn-outline-light'
										onClick={() => {
											axios.post(`/api/cuttingRequest/printSerie`,
												[...cuttingRequest.cuttingRequestSeries.map(elemCRS => { return { ...elemCRS, cuttingRequest: cuttingRequest } })]
											)
										}}
										style={{
											padding: "2 5",
											fontSize: 12,
											marginLeft: 5
										}}
									>
										<FontAwesomeIcon icon={faPrint} />
									</button>}
								</th>
							</tr>
						</thead>
						<tbody>
							{cuttingRequest.cuttingRequestSeries && cuttingRequest.cuttingRequestSeries
								.sort((a, b) => a.partNumberMaterial.localeCompare(b.partNumberMaterial) || a.groupPlacement - b.groupPlacement || Number(b.activated) - Number(a.activated))
								.map((elemPn, indPn) => {
									let arrDrill = (elemPn.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
									return <tr style={elemPn.activated === false ? { backgroundColor: "#ffffc0" } : { backgroundColor: "" }}>
										<td className='table-elem-sm'>{elemPn.serie}</td>
										<td className='table-elem-sm'>{elemPn.partNumberMaterial}</td>
										<td className='table-elem-sm'>{elemPn.description}</td>
										<td className='table-elem-sm'>{elemPn.matelassageEndroit}</td>
										<td className='table-elem-sm'>{elemPn.longueur && elemPn.longueur.toFixed(3)}</td>
										<td className='table-elem-sm'>{elemPn.nbrCouche}</td>
										<td className='table-elem-sm'>{elemPn.laize}</td>
										<td className='table-elem-sm'>{elemPn.placement}</td>
										<td className='table-elem-sm'>{elemPn.config}</td>
										<td className='table-elem-sm'>{arrDrill[0]}</td>
										<td className='table-elem-sm'>{arrDrill[1]}</td>
										<td className='table-elem-sm'>{elemPn.machine}</td>
										<td className='table-elem-sm'>
											{elemPn.serie && <button className='btn btn-outline-dark'
												onClick={() => {
													axios.post(`/api/cuttingRequest/printSerie`, [{ ...elemPn, cuttingRequest: cuttingRequest }])
												}}
												style={{
													padding: "2 5",
													fontSize: 12,
													marginLeft: 5
												}}
											>
												<FontAwesomeIcon icon={faPrint} />
											</button>}
											{!elemPn.activated && elemPn.serie == null && <button className='btn btn-sm btn-outline-dark' onClick={() => {
												let series = [...cuttingRequest.cuttingRequestSeries]
												let objInd = series.findIndex(e => (e.partNumberMaterial == elemPn.partNumberMaterial && e.groupPlacement == elemPn.groupPlacement && e.activated === true))
												if (objInd >= 0) {
													series[objInd].activated = false
												}
												series[indPn].activated = true
												cuttingRequest.cuttingRequestSeries = [...series]
												this.setState({ cuttingRequest: { ...cuttingRequest } })
											}}>
												<FontAwesomeIcon icon={faArrowUp} />
											</button>}
										</td>
									</tr>
								})}
						</tbody>
					</table>
				</div>
				<div>
					<table className='table m-0 table table-grey-border'>
						<thead style={{ backgroundColor: "#ca0c0c", color: "white" }}>
							<tr>
								<th className='table-elem-sm'>ID</th>
								<th className='table-elem-sm'>Part Number</th>
								<th className='table-elem-sm'>Description</th>
								<th className='table-elem-sm'>Kit textil</th>
								<th className='table-elem-sm'>Quantité</th>
								<th className='table-elem-sm'>wo</th>
								<th className='table-elem-sm'>woid</th>
							</tr>
						</thead>
						<tbody>
							{cuttingRequest.cuttingRequestBoxs && cuttingRequest.cuttingRequestBoxs.map(elemBox => <tr
								className='clickable-element'
								onClick={() => this.state.selectedBoxs.map(e => e.id).includes(elemBox.id)
									? this.setState({ selectedBoxs: this.state.selectedBoxs.filter(e => e.id != elemBox.id) })
									: this.setState({ selectedBoxs: [...this.state.selectedBoxs, elemBox].sort((a, b) => a.id.localeCompare(b.id)) })
								}
								style={this.state.selectedBoxs.map(e => e.id).includes(elemBox.id) ? { backgroundColor: "rgb(207, 207, 207)" } : {}}
							>
								<td className='table-elem-sm'>{elemBox.id}</td>
								<td className='table-elem-sm'>{elemBox.partNumber}</td>
								<td className='table-elem-sm'>{elemBox.description}</td>
								<td className='table-elem-sm'>{elemBox.item}</td>
								<td className='table-elem-sm'>{elemBox.qtyBox}</td>
								<td className='table-elem-sm'>{elemBox.wo}</td>
								<td className='table-elem-sm'>{elemBox.woid}</td>
							</tr>)}
						</tbody>
					</table>
				</div>
				<div style={{ overflow: "hidden", height: 0 }}>
					<div className='' ref={elem => this.componentRef = elem}>
						{this.state.selectedBoxs.length > 0 && this.state.selectedBoxs.map(box => {
							let wd = 1565, hg = 1100
							return [<div style={{ height: wd }}><GammePn box={box} /></div>, <div className="page-break" />]
						})}
					</div>
				</div>
				<div style={{ overflow: "hidden", height: 0 }}>
					<div className='' ref={elem => this.planPrintPage = elem} style={{ padding: 15 }}>
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
						<div>
							<div className='d-flex'>
								<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Sequence : </strong></div>
								<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.sequence}</div>
							</div>
							<div className='d-flex'>
								<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Projet : </strong></div>
								<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.projet}</div>
								<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Version : </strong></div>
								<div className='' style={{ width: "35%" }}>{cuttingRequest.version}</div>
							</div>
							<div className='d-flex'>
								<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>modele : </strong></div>
								<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.modele}</div>
							</div>
							<div className='d-flex'>
								<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>definition : </strong></div>
								<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.definition}</div>
							</div>
						</div>
						<div className='mb-2'>
							<table className='table m-0 table table-grey-border'>
								<thead>
									<tr>
										<th className='table-elem-sm'>Part Number</th>
										<th className='table-elem-sm'>Description</th>
										<th className='table-elem-sm'>Kit textil</th>
										<th className='table-elem-sm'>Quantité</th>
										<th className='table-elem-sm'>wo</th>
										<th className='table-elem-sm'>woid</th>
										<th className='table-elem-sm'>packageQty</th>

									</tr>
								</thead>
								<tbody>
									{cuttingRequest.cuttingRequestPartNumbers && cuttingRequest.cuttingRequestPartNumbers.map(elemPn => <tr
										className='clickable-element'
										onClick={() => this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber)
											? this.setState({ selectedBoxs: this.state.selectedBoxs.filter(e => e.partNumber != elemPn.partNumber) })
											: this.setState({ selectedBoxs: [...this.state.selectedBoxs, ...cuttingRequest.cuttingRequestBoxs.filter(e => e.partNumber == elemPn.partNumber)].sort((a, b) => a.id.localeCompare(b.id)) })
										}
										style={this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber) ? { backgroundColor: "rgb(207, 207, 207)" } : {}}
									>
										<td className='table-elem-sm'>{elemPn.partNumber}</td>
										<td className='table-elem-sm'>{elemPn.description}</td>
										<td className='table-elem-sm'>{elemPn.item}</td>
										<td className='table-elem-sm'>{elemPn.quantity}</td>
										<td className='table-elem-sm'>{elemPn.wo}</td>
										<td className='table-elem-sm'>{elemPn.woid}</td>
										<td className='table-elem-sm'><strong>{elemPn.packageQty}</strong></td>

									</tr>)}
								</tbody>
							</table>
						</div>
						<div>
							<table className='table m-0 table table-grey-border'>
								<thead>
									<tr >
										<td className='table-elem-sm' colSpan={7} >Matelassage</td>
										<td className='table-elem-sm ml-1' colSpan={5}>Coupe</td>
									</tr>
									<tr>
										<th className='table-elem-sm'>serie</th>
										<th className='table-elem-sm'>Part Number Material</th>
										<th className='table-elem-sm'>Description</th>
										<th className='table-elem-sm'>Sens</th>
										<th className='table-elem-sm'>longueur</th>
										<th className='table-elem-sm'>nbrCouche</th>
										<th className='table-elem-sm'>laize</th>
										<th className='table-elem-sm ml-1'>placement</th>
										<th className='table-elem-sm'>config</th>
										<th className='table-elem-sm'>drill1</th>
										<th className='table-elem-sm'>drill2</th>
										<th className='table-elem-sm'>machine</th>
									</tr>
								</thead>
								<tbody>
									{cuttingRequest.cuttingRequestSeries && cuttingRequest.cuttingRequestSeries.map(elemPn => {
										let arrDrill = (elemPn.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
										return <tr style={elemPn.activated === false ? { backgroundColor: "#ffffc0" } : { backgroundColor: "" }}>
											<td className='table-elem-sm'>{elemPn.serie}</td>
											<td className='table-elem-sm'>{elemPn.partNumberMaterial}</td>
											<td className='table-elem-sm'>{elemPn.description}</td>
											<td className='table-elem-sm'>{elemPn.matelassageEndroit}</td>
											<td className='table-elem-sm'>{elemPn.longueur && elemPn.longueur.toFixed(3)}</td>
											<td className='table-elem-sm'>{elemPn.nbrCouche}</td>
											<td className='table-elem-sm'>{elemPn.laize}</td>
											<td className='table-elem-sm ml-1'>{elemPn.placement}</td>
											<td className='table-elem-sm'>{elemPn.config}</td>
											<td className='table-elem-sm'>{arrDrill[0]}</td>
											<td className='table-elem-sm'>{arrDrill[1]}</td>
											<td className='table-elem-sm'>{elemPn.machine}</td>
										</tr>
									})}
								</tbody>
							</table>
						</div>
					</div>
				</div>
			</div>}
			<Modal.Footer>
				<div className='d-flex pb-2 '>
					<button className='btn btn-link' onClick={() => { this.setState({ importModalInd: null, selectedBoxs: [] }) }}> <FontAwesomeIcon icon={faArrowLeft} /> Retour</button>
					<ReactToPrint
						onBeforeGetContent={() => {
							return new Promise((resolve, reject) => {
								this.setState({}, () => resolve())
							});
						}}
						onAfterPrint={() => { this.setState({ modalRotate: false }) }}
						onPrintError={() => { this.setState({ modalRotate: false }) }}
						trigger={() => <button
							type="button"
							className="btn btn-outline-success ml-1"

						><FontAwesomeIcon icon={faPrint} /> Imprimer Plan</button>}
						content={() => this.planPrintPage}
					/>
					<ReactToPrint
						onBeforeGetContent={() => {
							return new Promise((resolve, reject) => {
								this.setState({ modalRotate: true, selectedEmp: null }, () => resolve())
							});
						}}
						onAfterPrint={() => { this.setState({ modalRotate: false }) }}
						onPrintError={() => { this.setState({ modalRotate: false }) }}
						trigger={() => <button
							type="button"
							className="btn btn-outline-success ml-1"

						><FontAwesomeIcon icon={faPrint} /> Imprimer Gammes</button>}
						content={() => this.componentRef}
					/>
					<button className='btn btn-success float-right ml-2' onClick={() => {
						let objCR = { ...this.state.importModal }
						objCR.cuttingRequestSeries = [...objCR.cuttingRequestSeries.filter(elem => elem.activated === true)]
						axios.post(`/api/cuttingRequest`, objCR)
							.then(res => {
								this.setState({ importModal: res.data })
							})
							.catch(err => {
								this.setState({ error: err.response.data })
							})
					}}>
						Confirmer
					</button>
				</div>
			</Modal.Footer>
		</Modal>
	}

	renderVerification = (str) => {
		if (str) {
			if (str === "Loading") {
				return <FontAwesomeIcon icon={faClock} />
			}
			if (str === "Good") {
				// return <FontAwesomeIcon icon={faCheck} color="green" />
			}
			if (str.startsWith("Bad : ")) {
				return <span>
					{/* <FontAwesomeIcon icon={faTimes} color="red"/>  */}
					<pre style={this.styleVerification(str)}>{str.slice(6)}</pre>
				</span>
			}
		}
	}

	verification1 = () => {
		this.setState({ modalObj: { ...this.state.modalObj, verification1: "Loading" } })
		let error = []
		if (this.state.modalObj.cuttingPlanMaterials && this.state.modalObj.cuttingPlanMaterials.length > 0) {
			this.state.modalObj.cuttingPlanMaterials.map(cpm => {
				let arr = cpm.partNumbers.split(",")
				let arrModeles = []
				cpm.cuttingPlanMaterialPlacement.map(cpmp => {
					if (cpmp.partNumbers && cpmp.partNumbers.length > 0) {
						cpmp.partNumbers.split(", ").map(elem => {
							let modele = elem.split(":")[0]
							if (arr.includes(modele)) {
								arrModeles.push(modele)
							} else {
								error.push(cpmp.placement + " : Modèle " + modele + " n'est pas trouvé")
							}
						})
					} else {
						error.push(cpmp.placement + " with no partnumbers")
					}
				})
				if (arr.length > arrModeles.length) {
					error.push(cpm.partNumberMaterial + " : PNs non inclus " + arr.filter(e => !arrModeles.includes(e)).join("/"));
				}
			})
			if (error.length > 0) {
				this.setState({ modalObj: { ...this.state.modalObj, verification1: "Bad : " + error.join("\n") } })
			} else {
				this.setState({ modalObj: { ...this.state.modalObj, verification1: "Good" } })
			}
		} else {
			this.setState({ modalObj: { ...this.state.modalObj, verification1: "Bad : aucun reftissu" } })
		}
	}

	verification2 = () => {
		this.setState({ modalObj: { ...this.state.modalObj, verification2: "Loading" } })
		let error = []
		if (this.state.modalObj.cuttingPlanMaterials && this.state.modalObj.cuttingPlanMaterials.length > 0) {
			this.state.modalObj.cuttingPlanMaterials.map(cpm => {
				cpm.cuttingPlanMaterialPlacement.map(cpmp => {
					if (cpmp.placement && cpmp.placement.length > 0) {
						let rapportPlt = this.state.modalObj.cuttingPlanRapportPlacements.find(elemRapport => {
							return elemRapport.nomPlct.trim().toUpperCase() === cpmp.placement.trim().toUpperCase()
						})
						if (rapportPlt) {
							if (rapportPlt.largeur && !isNaN(rapportPlt.largeur)) {
								let configObj = this.state.partNumberMaterialConfigs[cpm.partNumberMaterial]
								let margeLaizeMin = (configObj && configObj.margeLaizeMin) ? configObj.margeLaizeMin : 1
								let margeLaizeMax = (configObj && configObj.margeLaizeMax) ? configObj.margeLaizeMax : 1.3
								if (this.convertFloat(rapportPlt.largeur, 1) !== this.convertFloat(cpmp.laize * 100 - margeLaizeMin, 1) && this.convertFloat(rapportPlt.largeur, 1) !== this.convertFloat(cpmp.laize * 100 - margeLaizeMax, 1)) {
									error.push(cpmp.placement + " n'est pas " + this.convertFloat(cpmp.laize * 100 - margeLaizeMin, 1) + "cm ou " + this.convertFloat(cpmp.laize * 100 - margeLaizeMax, 1) + "cm")
								}
							} else {
								error.push("Vérifier la largeur " + cpmp.placement + " dans le rapport")
							}
						} else {
							error.push(cpmp.placement + " n'est trouvé dans le rapport")
						}
					}
				})
			})
			if (error.length > 0) {
				this.setState({ modalObj: { ...this.state.modalObj, verification2: "Bad : " + error.join("\n") } })
			} else {
				this.setState({ modalObj: { ...this.state.modalObj, verification2: "Good" } })
			}
		} else {
			this.setState({ modalObj: { ...this.state.modalObj, verification2: "Bad : aucun reftissu" } })
		}
	}

	verification3 = () => {
		this.setState({ modalObj: { ...this.state.modalObj, verification3: "Loading" } })
		let error = []
		// if (this.state.modalObj.cuttingPlanRapportDrills == null || this.state.modalObj.cuttingPlanRapportDrills.length == 0) {
		// 	this.setState({ modalObj: { ...this.state.modalObj, verification3: "Bad : rapport drill vide" } })
		// } else 
		if (this.state.modalObj.cuttingPlanMaterials && this.state.modalObj.cuttingPlanMaterials.length > 0) {
			this.state.modalObj.cuttingPlanMaterials.map(cpm => {
				cpm.cuttingPlanMaterialPlacement.map(cpmp => {
					let arrDrill = (cpmp.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
					if (cpmp.placement && cpmp.placement.length > 0) {
						let arrPlctDrill1 = this.state.modalObj.cuttingPlanRapportDrills != null
							? this.state.modalObj.cuttingPlanRapportDrills.filter(e => ["D"].includes(e.labelInterne.trim().toUpperCase())).map(e => e.nomPlacement.toUpperCase().trim())
							: []
						let arrPlctDrill2 = this.state.modalObj.cuttingPlanRapportDrills != null
							? this.state.modalObj.cuttingPlanRapportDrills.filter(e => ["E"].includes(e.labelInterne.trim().toUpperCase())).map(e => e.nomPlacement.toUpperCase().trim())
							: []
						if (arrPlctDrill1.includes(cpmp.placement.toUpperCase().trim()) && (arrDrill[0] == null || arrDrill[0] == 0)) {
							error.push("R3 : " + cpmp.placement + " a besoin de 1er drill")
						}
						if (arrPlctDrill2.includes(cpmp.placement.toUpperCase().trim()) && (arrDrill[1] == null || arrDrill[1] == 0)) {
							error.push("R3 : " + cpmp.placement + " a besoin de 2eme drill")
						}

						// we need to search fro the placement in this.state.modalObj.cuttingPlanRapportPlacements and verify drill1 and drill2
						let elemRapport = this.state.modalObj.cuttingPlanRapportPlacements
							? this.state.modalObj.cuttingPlanRapportPlacements.find(elem => elem.nomPlct.trim().toUpperCase() === cpmp.placement.trim().toUpperCase())
							: null
						if (elemRapport) {
							if (elemRapport.drill1 === true && (arrDrill[0] == null || arrDrill[0] == 0)) {
								error.push("R1 : " + cpmp.placement + " a besoin de 1er drill")
							}
							if (elemRapport.drill2 === true && (arrDrill[1] == null || arrDrill[1] == 0)) {
								error.push("R1 : " + cpmp.placement + " a besoin de 2eme drill")
							}
							if (elemRapport.drill1 === false && (arrDrill[0] != null && arrDrill[0] != 0)) {
								error.push("R1 : " + cpmp.placement + " ne doit pas avoir 1er drill")
							}
							if (elemRapport.drill2 === false && (arrDrill[1] != null && arrDrill[1] != 0)) {
								error.push("R1 : " + cpmp.placement + " ne doit pas avoir 2eme drill")
							}
						}

					}
				})
			})
			if (error.length > 0) {
				this.setState({ modalObj: { ...this.state.modalObj, verification3: "Bad : " + error.join("\n") } })
			} else {
				this.setState({ modalObj: { ...this.state.modalObj, verification3: "Good" } })
			}
		} else {
			this.setState({ modalObj: { ...this.state.modalObj, verification3: "Bad : aucun reftissu" } })
		}
	}

	generateByPlacement = () => {
		return new Promise((resolve) => {
			let objPlct = {}
			this.state.modalObj.cuttingPlanMaterials.map(mat => {
				mat.cuttingPlanMaterialPlacement.map(cpmp => {
					objPlct[cpmp.placement.trim().toUpperCase()] = mat.partNumberMaterial
				})
			})

			axios.get(`/api/placementData/rapport?placements=${this.state.modalObj.cuttingPlanMaterials.map(e => e.cuttingPlanMaterialPlacement.map(cpmp => cpmp.placement).join(",")).join(",")}&partNumbers=${this.state.modalObj.cuttingPlanPartNumbers.map(e => e.partNumber).join(",")}`)
				.then((res) => {

					let error = []
					console.log(this.state.modalObj)
					let pnObj = {}
					this.state.modalObj.cuttingPlanPartNumbers.map(pn => {
						pnObj[pn.partNumber] = pn.quantity
					})
					let arrMaterials = [...this.state.modalObj.cuttingPlanMaterials]
					let arr = res.data.map(elem => {
						elem.numeroDefPlct = elem.numeroDefPlct ? elem.numeroDefPlct.trim().toUpperCase() : objPlct[elem.nomDefPlct.trim().toUpperCase()]
						return elem
					})
					arr.map(elem => {
						try {
							let indMat = arrMaterials.findIndex(mat => mat.cuttingPlanMaterialPlacement.map(plct => plct.placement).includes(elem.nomDefPlct))
							let reftissu = arrMaterials[indMat].partNumberMaterial
							elem.nomDefPlct = elem.nomDefPlct ? elem.nomDefPlct.trim().toUpperCase() : objPlct[elem.nomDefPlct.trim().toUpperCase()]
							if (elem.numeroDefPlct.trim().toUpperCase() !== arrMaterials[indMat].partNumberMaterial) {
								error.push(elem.nomDefPlct + " n'utilise pas le bon reftissu (" + arrMaterials[indMat].partNumberMaterial + "=/=" + elem.numeroDefPlct.trim().toUpperCase() + ")")
							}
							let indPc = arrMaterials[indMat].cuttingPlanMaterialPlacement.findIndex(plct => plct.placement.trim().toUpperCase() === elem.nomDefPlct.trim().toUpperCase())
							let lg = this.convertFloat((elem.longueur.replace("M", "")), 3);
							arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].longueur = lg

							let machine = arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].machine
							let margesConfig = this.getMarginConfig(lg, reftissu, machine)
							if (margesConfig) {
								arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].pliesConfigMarge = margesConfig.pliesConfig
							} else {
								error.push("Aucune marge trouvée pour la longueur " + lg + " et le tissu " + reftissu + " avec la machine " + machine)
							}


							let qteList = elem.placeTailleQt.replace(")", "").split("|").map(qteElem => qteElem.split("("))
							let partNumbers = []
							qteList.map((qte, qteInd) => {
								partNumbers.push([elem.modeles.split(", ")[qteInd], parseInt(qte[0]) * parseInt(qte[1])])
							})
							arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].partNumbers = partNumbers.map(pn => pn.join(":")).join(", ")
							let maxNbrCouche = 0
							partNumbers.map(pn => {
								let couchePn = Math.ceil(pnObj[pn[0]] / parseInt(pn[1]))
								console.log(pn[0] + " " + pn[1] + " : " + couchePn)
								if (couchePn > maxNbrCouche) {
									maxNbrCouche = couchePn;
								}
							})
							if (arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].nbrCouche && arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].nbrCouche > 0 && maxNbrCouche !== arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].nbrCouche) {
								arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].nbrCoucheOld = arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].nbrCouche
							}
							arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].nbrCouche = maxNbrCouche
							let config = null;
							if (this.state.partNumberMaterialConfigs[reftissu] && arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].machine) {
								let configObj = this.state.partNumberMaterialConfigs[reftissu].reftissuMachines.find(e => e.machineType == arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].machine)
								if (configObj != null) {
									let configArr = configObj.pliesConfig.split("|").map(e => { return [parseInt(e.split(";")[0]), e.split(";")[1]] }).sort((a, b) => a[0] - b[0])
									for (let i = 0; i < configArr.length; i++) {
										if (configArr[i][0] <= arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].nbrCouche) {
											config = configArr[i][1]
										}
									}
								}
							}
							arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].config = config

							if (elem.perimetreTotal) {
								arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].perimetre = this.convertFloat(elem.perimetreTotal.replace("M", ""), 3)
								let vitesseObj = this.state.optionsList.cuttingSpeed.find(e => e.value === config)
								if (vitesseObj) {
									arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].tempsDeCoupe = this.convertFloat(arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].perimetre / (vitesseObj.item.vitesse * 100), 3)
								} else {
									arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].tempsDeCoupe = this.convertFloat(arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].perimetre / (arrMaterials[indMat].vitesse), 3)
								}
							} else {
								arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].perimetre = null
								arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].tempsDeCoupe = null
							}

							if (lg) {
								let lgMatelasTotal = 0
								let arrDrill = (arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
								let maxPlie = arrDrill[0] == null && arrDrill[1] == null ? arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].maxPlie : arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].maxPlieDrill
								let nbrCouche = arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].nbrCouche
								if (this.state.partNumberMaterialConfigs[reftissu].plaque > 0) {
									lgMatelasTotal += this.state.partNumberMaterialConfigs[reftissu].plaque * nbrCouche
								} else {
									let marge = this.getMarge(lg, maxPlie, reftissu, machine)
									if (marge === null || marge === undefined) {
										error.push("la longueur " + lg + " dépasse l'interval pour " + reftissu)
									} else {
										lgMatelasTotal += (lg + this.getMarge(lg, Math.min(maxPlie, nbrCouche), reftissu, machine)) * nbrCouche
									}

								}
								arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].longueurMatelas = lgMatelasTotal
							}
							let arrDrill = (arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
							if (elem.drill1 === true) {
								if (arrDrill[0] == null || arrDrill[0] === 0) {
									error.push("R1 drill : le placement " + elem.nomDefPlct + " a besoin de 1er drill")
								}
							}
							if (elem.drill1 === false) {
								if (arrDrill[0] != null && arrDrill[0] !== 0) {
									error.push("R1 drill : le placement " + elem.nomDefPlct + " n'a pas besoin de 1er drill (" + arrDrill[0] + ")")
									arrDrill[0] = null
									arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].drill = arrDrill.join(",")
								}
							}
							if (elem.drill2 === true) {
								if (arrDrill[1] == null || arrDrill[1] === 0) {
									error.push("R1 drill : le placement " + elem.nomDefPlct + " a besoin de 2eme drill")
								}
							}
							if (elem.drill2 === false) {
								if (arrDrill[1] != null && arrDrill[1] !== 0) {
									error.push("R1 drill : le placement " + elem.nomDefPlct + " n'a pas besoin de 2eme drill (" + arrDrill[1] + ")")
									arrDrill[1] = null
									arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].drill = arrDrill.join(",")
								}
							}

							arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].espaceRelarge = elem.contraintes
							arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].rotation = elem.description
							arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc].verifEndroit = elem.na
							let cpmp = arrMaterials[indMat].cuttingPlanMaterialPlacement[indPc]
							let cpm = arrMaterials[indMat]
							if (cpmp.machine === "Lectra IP6") {
								let arrConditionBuffer = []
								if (this.state.partNumberMaterialConfigs[cpm.partNumberMaterial] != null &&
									(this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer1IP6
										|| this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer2IP6)) {
									if (this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer1IP6 && this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer1IP6.length > 0) {
										arrConditionBuffer.push(this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer1IP6)
									}
									if (this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer2IP6 && this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer2IP6.length > 0) {
										arrConditionBuffer.push(this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer2IP6)
									}
								}
								if (arrConditionBuffer.length === 0) {
									arrConditionBuffer.push("ESP00")
								}
								if (!arrConditionBuffer.includes(elem.contraintes)) {
									error.push(elem.nomDefPlct + " : le placement doit être avec l'espace relarge " + arrConditionBuffer.join(", "))
								}
							}


						} catch (err) {
							console.log(err)
						}
					})

					this.setState({ modalObj: { ...this.state.modalObj, cuttingPlanRapportPlacements: arr, cuttingPlanMaterials: arrMaterials }, error: error.length > 0 ? error : null }, () => resolve())
				})
				.catch((err) => { console.log(err); resolve() })
		})
	}

	verification4 = () => {
		this.setState({ modalObj: { ...this.state.modalObj, verification4: "Loading" } })
		let error = []
		const { entityId } = this.props
		if (this.state.modalObj.cuttingPlanMaterials && this.state.modalObj.cuttingPlanMaterials.length > 0) {

			let totalQuantite = 0
			this.state.modalObj.cuttingPlanPartNumbers.map(cppn => {
				if (cppn.quantity) {
					totalQuantite += cppn.quantity
				}
			})
			let arrCombinations = []
			this.state.modalObj.cuttingPlanMaterials.map(cpm => {
				let cpmConfig = this.state.partNumberMaterialConfigs[cpm.partNumberMaterial]
				if (cpmConfig && cpmConfig.validated0BF === true) {
					let optionalCpmp = cpm.cuttingPlanMaterialPlacement.find(e => e.activated === false)
					if (optionalCpmp) {
						error.push(cpm.partNumberMaterial + " : validated 0BF : aucun placement optionnel (activated=false) autorisé")
					}
				}
				cpm.cuttingPlanMaterialPlacement.map(cpmp => {
					if (arrCombinations.includes(cpm.partNumberMaterial + "-" + cpmp.machine + "-" + cpmp.category + "-" + cpmp.groupPlacement)) {
						error.push(cpmp.placement + " : le placement doit être unique pour la machine " + cpmp.machine + " et la catégorie " + cpmp.category)
					} else {
						arrCombinations.push(cpm.partNumberMaterial + "-" + cpmp.machine + "-" + cpmp.category + "-" + cpmp.groupPlacement)
					}
					let configObj = this.state.partNumberMaterialConfigs[cpm.partNumberMaterial]
					let isLectraFamily = cpmp.machine === "Lectra" || cpmp.machine === "Lectra IP6"
					if (cpmp.activated && totalQuantite >= 20 && cpmp.longueur > 0.3 && cpmp.nbrCouche >= 5 && (entityId === null || entityId === undefined)) {
						if (configObj.validated0BF === true
							&& cpmp.espaceRelarge && cpmp.espaceRelarge.trim().toUpperCase() !== 'ESP00' && isLectraFamily) {
							error.push(cpmp.placement + " : validated 0BF : l'espace relarge doit être ESP00")
						}
						if (configObj.validated0BF === true && isLectraFamily
							&& !cpmp.placement.includes("-0BF") && !cpmp.placement.includes("-MBF")) {
							error.push(cpmp.placement + " : validated 0BF : le nom du placement doit contenir -0BF ou -MBF")
						}
					}
					if (configObj.validated0BF !== true
							&& cpmp.espaceRelarge && cpmp.espaceRelarge.trim().toUpperCase() === 'ESP00' && cpmp.machine === "Lectra") {
							error.push(cpmp.placement + " : l'espace relarge ne doit pas être ESP00")
					}
					if (configObj && configObj.validatedIP6 === true && cpmp.activated === true) {
						if (!cpmp.placement || !cpmp.placement.toUpperCase().includes("-IP6")) {
							error.push(cpmp.placement + " : validated IP6 : le nom du placement doit contenir -IP6")
						}
						if (cpmp.machine !== "Lectra IP6") {
							error.push(cpmp.placement + " : validated IP6 : la machine doit être Lectra IP6")
						}
						if (!cpmp.espaceRelarge || cpmp.espaceRelarge.trim().toUpperCase() !== 'ESP00') {
							error.push(cpmp.placement + " : validated IP6 : l'espace relarge doit être ESP00")
						}
					}

					if (this.state.partNumberMaterialConfigs[cpm.partNumberMaterial] != null && this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].rotation && cpmp.rotation) {
						if (
							(cpmp.rotation.includes("90") && !this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].rotation.includes("90"))
							|| (cpmp.rotation.includes("180") && !this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].rotation.includes("180"))
							|| (cpmp.rotation.toUpperCase().includes("FIX") && !this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].rotation.includes("FIX"))
						) {
							error.push(cpmp.placement + " : le placement doit être avec la rotation " + this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].rotation)
						}
					}
					if (cpmp.machine === "Lectra IP6") {
						let arrConditionBuffer = []
						if (this.state.partNumberMaterialConfigs[cpm.partNumberMaterial] != null &&
							(this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer1IP6
								|| this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer2IP6)) {
							if (this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer1IP6 && this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer1IP6.length > 0) {
								arrConditionBuffer.push(this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer1IP6)
							}
							if (this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer2IP6 && this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer2IP6.length > 0) {
								arrConditionBuffer.push(this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].buffer2IP6)
							}
						}
						if (arrConditionBuffer.length === 0) {
							arrConditionBuffer.push("ESP00")
						}
						if (!arrConditionBuffer.includes(cpmp.espaceRelarge)) {
							error.push(cpmp.placement + " : le placement doit être avec l'espace relarge " + arrConditionBuffer.join(", "))
						}
					}
					// if (cpmp.espaceRelarge && cpmp.espaceRelarge.trim().toUpperCase() !== "ESP00") {
					// 	error.push("Le placement " + cpmp.placement + " doit être avec l'espace relarge ESP00")
					// }
					if (cpmp.machine === "Lectra IP6" && !cpmp.placement.toUpperCase().includes("-IP6") && !cpmp.placement.toUpperCase().includes("-0BF")) {
						error.push(cpmp.placement + " :  le nom du placement doit contenir IP6 ou 0BF")
					}
					if ((cpmp.machine === "LASER-DXF" && !cpmp.placement.toUpperCase().includes("-DXF")) || (cpmp.placement.toUpperCase().includes("-DXF") && cpmp.machine !== "LASER-DXF")) {
						error.push(cpmp.placement + " : le nom du placement ou type de machine incorrect")
					}
					if ((cpmp.machine === "LASER-LSR" && !cpmp.placement.toUpperCase().includes("-LSR")) || (cpmp.placement.toUpperCase().includes("-LSR") && cpmp.machine !== "LASER-LSR")) {
						error.push(cpmp.placement + " : le nom du placement ou type de machine incorrect")
					}
					if (cpmp.placement.toUpperCase().includes("-IP6") && cpmp.machine !== "Lectra IP6") {
						error.push(cpmp.placement + " : le nom du placement ou type de machine incorrect. IP6")
					}


					if (cpmp.placement && cpmp.placement.length > 0 && cpmp.machine && cpmp.machine !== "DIE") {
						let marge = this.getMarge(cpmp.longueur, cpmp.nbrCouche, cpm.partNumberMaterial, cpmp.machine)
						if (marge === null || marge === undefined) {
							error.push(`Aucune marge trouvée pour la longueur ${cpmp.longueur} du placement ${cpmp.placement}`)
						}
						if (this.state.longueurInf && this.state.nbrCoucheSup && cpmp.longueur < this.state.longueurInf && cpmp.nbrCouche > this.state.nbrCoucheSup) {
							error.push(cpmp.placement + " : Longueur < " + this.state.longueurInf + " et NbrCouche > " + this.state.nbrCoucheSup)
						}

						// LASER-DXF: if consumption (longueurMatelas) < 1.1, nbrCouche must be 1
						if (cpmp.machine === "LASER-DXF" && cpmp.longueurMatelas && cpmp.longueurMatelas < 1.1 && cpmp.nbrCouche !== 1) {
							error.push(cpmp.placement + " : LASER-DXF avec consommation < 1.1, le nombre de couche doit être 1")
						}

						let elemRapport = this.state.modalObj.cuttingPlanRapportPlacements
							? this.state.modalObj.cuttingPlanRapportPlacements.find(elem => elem.nomPlct.trim().toUpperCase() === cpmp.placement.trim().toUpperCase())
							: null
						if (elemRapport && elemRapport.longueur && elemRapport.longueur.length > 0) {
							let lg = elemRapport.longueur.replace("M", "")
							if (!isNaN(lg) && this.convertFloat(lg, 3) === cpmp.longueur) {

							} else {
								error.push(cpmp.placement + " : le placement n'a pas la même longueur que le rapport placement")
							}
						}
					}
				})
			})

			// this.state.modalObj.cuttingPlanRapportPlacements.map(elem => {
			// 	if (elem.pertePercentage && elem.pertePercentage.trim().length > 0) {
			// 		error.push(elem.nomPlct + " : Perte " + elem.pertePercentage)
			// 	}
			// })



			if (error.length > 0) {
				this.setState({ modalObj: { ...this.state.modalObj, verification4: "Bad : " + error.join("\n") } })
			} else {
				this.setState({ modalObj: { ...this.state.modalObj, verification4: "Good" } })
			}
		} else {
			this.setState({ modalObj: { ...this.state.modalObj, verification4: "Bad : aucun reftissu" } })
		}
	}

	verification5 = () => {
		this.setState({ modalObj: { ...this.state.modalObj, verification5: "Loading" } })
		let error = []
		if (this.state.modalObj.cuttingPlanMaterials && this.state.modalObj.cuttingPlanMaterials.length > 0) {
			this.state.modalObj.cuttingPlanMaterials.map(cpm => {
				cpm.cuttingPlanMaterialPlacement.map(cpmp => {
					if (cpmp.placement && cpmp.placement.length > 0) {
						let elemRapport = this.state.modalObj.cuttingPlanRapportPlacements
							? this.state.modalObj.cuttingPlanRapportPlacements.find(elem => elem.nomPlct.trim().toUpperCase() === cpmp.placement.trim().toUpperCase())
							: null
						if (elemRapport && elemRapport.numeroDefPlct && elemRapport.numeroDefPlct.length > 0) {
							if (elemRapport.numeroDefPlct.trim().toUpperCase() === cpm.partNumberMaterial.trim().toUpperCase()) {

							} else {
								error.push(cpmp.placement + " est avec " + elemRapport.numeroDefPlct.trim().toUpperCase())
							}
						}
					}
				})
			})
			if (error.length > 0) {
				this.setState({ modalObj: { ...this.state.modalObj, verification5: "Bad : " + error.join("\n") } })
			} else {
				this.setState({ modalObj: { ...this.state.modalObj, verification5: "Good" } })
			}
		} else {
			this.setState({ modalObj: { ...this.state.modalObj, verification5: "Bad : aucun reftissu" } })
		}
	}

	verification6 = () => {
		this.setState({ modalObj: { ...this.state.modalObj, verification6: "Loading" } })
		let error = []
		let projetCode = this.state.optionsList.projet && (this.state.optionsList.projet.find(e => this.state.modalObj.projet === e.value)?.item.code || "").toUpperCase().trim()
		let versionCode = this.state.optionsList.version && (this.state.optionsList.version.find(e => this.state.modalObj.version === e.value)?.item.code || "").toUpperCase().trim()
		if (this.state.modalObj.cuttingPlanMaterials && this.state.modalObj.cuttingPlanMaterials.length > 0) {
			this.state.modalObj.cuttingPlanMaterials.map(cpm => {
				cpm.cuttingPlanMaterialPlacement.map(cpmp => {
					if (cpmp.placement && cpmp.placement.length > 0 && cpmp.machine !== "DIE") {
						if (projetCode && projetCode.length > 0) {
							if (versionCode && !cpmp.placement.toUpperCase().trim().startsWith(projetCode + versionCode) && !cpmp.placement.toUpperCase().trim().startsWith("__" + projetCode + versionCode)) {
								error.push(cpmp.placement + " ne commence pas avec " + projetCode + versionCode)
							} else if (!cpmp.placement.toUpperCase().trim().startsWith(projetCode) && !cpmp.placement.toUpperCase().trim().startsWith("__" + projetCode)) {
								error.push(cpmp.placement + " ne commence pas avec " + projetCode)
							}
						}
					}
				})
			})
			if (error.length > 0) {
				this.setState({ modalObj: { ...this.state.modalObj, verification6: "Bad : " + error.join("\n") } })
			} else {
				this.setState({ modalObj: { ...this.state.modalObj, verification6: "Good" } })
			}
		} else {
			this.setState({ modalObj: { ...this.state.modalObj, verification6: "Bad : aucun reftissu" } })
		}
	}

	verification7Plaque = () => {
		this.setState({ modalObj: { ...this.state.modalObj, verification7Plaque: "Loading" } })
		let error = []
		let warnings = []
		const tolerance = 0.05 // 5% tolerance for plaque longueur difference
		
		if (this.state.modalObj.cuttingPlanMaterials && this.state.modalObj.cuttingPlanMaterials.length > 0) {
			this.state.modalObj.cuttingPlanMaterials.map(cpm => {
				// Check if this material has a plaque value
				const configObj = this.state.partNumberMaterialConfigs[cpm.partNumberMaterial]
				if (configObj && configObj.plaque) {
					const plaqueValue = parseFloat(configObj.plaque)
					
					cpm.cuttingPlanMaterialPlacement.map(cpmp => {
						if (cpmp.placement && cpmp.placement.length > 0 && cpmp.machine && cpmp.machine !== "DIE") {
							// Get the longueur from placement rapport if available
							let elemRapport = this.state.modalObj.cuttingPlanRapportPlacements
								? this.state.modalObj.cuttingPlanRapportPlacements.find(elem => elem.nomPlct.trim().toUpperCase() === cpmp.placement.trim().toUpperCase())
								: null
							
							if (elemRapport && elemRapport.longueur && elemRapport.longueur.length > 0) {
								let rapportLongueur = parseFloat(elemRapport.longueur.replace("M", ""))
								
								if (!isNaN(rapportLongueur) && !isNaN(plaqueValue) && plaqueValue > 0) {
									const difference = Math.abs(rapportLongueur - plaqueValue)
									const differencePercent = (difference / plaqueValue) * 100
									
									if (differencePercent > (tolerance * 100)) {
										warnings.push(`${cpmp.placement} : Longueur rapport (${rapportLongueur.toFixed(3)}) vs Plaque (${plaqueValue.toFixed(3)}) - Diff: ${difference.toFixed(3)}m (${differencePercent.toFixed(1)}%)`)
									}
								}
							}
							
							// Also compare with the current longueur stored in placement
							if (cpmp.longueur && !isNaN(cpmp.longueur) && !isNaN(plaqueValue) && plaqueValue > 0) {
								const currentDifference = Math.abs(cpmp.longueur - plaqueValue)
								const currentDifferencePercent = (currentDifference / plaqueValue) * 100
								
								if (currentDifferencePercent > (tolerance * 100)) {
									error.push(`${cpmp.placement} : Longueur (${cpmp.longueur.toFixed(3)}) vs Plaque (${plaqueValue.toFixed(3)}) - Diff: ${currentDifference.toFixed(3)}m (${currentDifferencePercent.toFixed(1)}%)`)
								}
							}
						}
					})
				}
			})
			
			if (error.length > 0 || warnings.length > 0) {
				let message = ""
				if (error.length > 0) {
					message = "Erreurs: " + error.join("\n")
				}
				if (warnings.length > 0) {
					if (message.length > 0) message += "\n\nAvertissements: "
					else message = "Avertissements: "
					message += warnings.join("\n")
				}
				this.setState({ modalObj: { ...this.state.modalObj, verification7Plaque: "Bad : " + message } })
			} else {
				this.setState({ modalObj: { ...this.state.modalObj, verification7Plaque: "Good" } })
			}
		} else {
			this.setState({ modalObj: { ...this.state.modalObj, verification7Plaque: "Bad : aucun reftissu" } })
		}
	}

	verification7 = () => {
		let placements = [], placementNbrCouche = {}, error = [], partNumbers = [], placementActivated = [], placementOptional = []
		let placementMeta = {} // { placementName: { partNumberMaterial, groupPlacement, activated } }
		this.state.modalObj.cuttingPlanPartNumbers.map((cppn, ind) => {
			if (cppn.partNumber == null) {
				error.push("Un part number est vide")
			} else if (partNumbers.includes(cppn.partNumber)) {
				error.push("Le Part Number " + cppn.partNumber + " est dupliqué")
			} else if (cppn.quantity == null || cppn.quantity == 0) {
				error.push("Le Part Number " + cppn.partNumber + " n'a aucun quantité")
			} else {
				partNumbers.push(cppn.partNumber)
			}
		})
		let codePV = this.state.optionsList.projet && (this.state.optionsList.projet.find(e => this.state.modalObj.projet === e.value)?.item.code || "") +
			(this.state.optionsList.version && (this.state.optionsList.version.find(e => this.state.modalObj.version === e.value)?.item.code || ""))
		let arrPlctDrill1 = this.state.modalObj.cuttingPlanRapportDrills != null
			? this.state.modalObj.cuttingPlanRapportDrills.filter(e => ["D"].includes(e.labelInterne.trim().toUpperCase())).map(e => e.nomPlacement.toUpperCase().trim())
			: []
		let arrPlctDrill2 = this.state.modalObj.cuttingPlanRapportDrills != null
			? this.state.modalObj.cuttingPlanRapportDrills.filter(e => ["E"].includes(e.labelInterne.trim().toUpperCase())).map(e => e.nomPlacement.toUpperCase().trim())
			: []

		this.state.modalObj.cuttingPlanMaterials.map(cpm => {
			cpm.cuttingPlanMaterialPlacement.map(cpmp => {
				let arrDrill = (cpmp.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
				if (cpmp.placement == null) {
					error.push("un placement dans " + cpm.partNumberMaterial + " est vide")
				} else if (placements.includes(cpmp.placement)) {
					error.push("répétition du placement " + cpmp.placement)
					// } else if (!cpmp.placement.startsWith(codePV) && !cpmp.placement.startsWith("__" + codePV)) {
					// 	error.push("le préfixe du placement " + cpmp.placement + " doit être " + codePV + " ou __" + codePV)
				} else if (cpmp.nbrCouche == null || cpmp.nbrCouche == 0) {
					error.push("Nombre de couche du placement " + cpmp.placement + " est vide")
					// } else if ((arrDrill.join(",").trim() === "," && cpmp.maxPlie && cpmp.nbrCouche > cpmp.maxPlie)
					// 	|| (arrDrill.join(",").trim() !== "," && cpmp.maxPlieDrill && cpmp.nbrCouche > cpmp.maxPlieDrill)) {
					// 	error.push("Nombre de couche du placement " + cpmp.placement + " supérieure au max")
				} else if (cpmp.config == null) {
					error.push("Config du placement " + cpmp.placement + " est vide")
				} else if (cpmp.category == null) {
					error.push("Category du placement " + cpmp.placement + " est vide")
				} else if (cpmp.laize == null) {
					error.push("La laize du placement " + cpmp.placement + " est vide")
				} else if (cpmp.longueur == null) {
					error.push("Longueur du placement " + cpmp.placement + " est vide")
				} else if (cpmp.longueurMatelas == null) {
					error.push("Longueur Matelas du placement " + cpmp.placement + " est vide")
				} else {
					if (cpmp.activated === true) {
						placementActivated.push(cpmp.placement)
					} else if (cpmp.activated === false) {
						placementOptional.push(cpmp.placement)
					}
					placementMeta[cpmp.placement] = { partNumberMaterial: cpm.partNumberMaterial, groupPlacement: cpmp.groupPlacement, activated: cpmp.activated }
					placements.push(cpmp.placement)
					placementNbrCouche[cpmp.placement] = cpmp.nbrCouche
				}

				if (arrPlctDrill1.includes(cpmp.placement.toUpperCase().trim()) && arrDrill[0] == null) {
					error.push("R3 drill : le placement " + cpmp.placement + " a besoin de  le 1er drill")
				}
				if (arrPlctDrill2.includes(cpmp.placement.toUpperCase().trim()) && arrDrill[1] == null) {
					error.push("R3 drill : le placement " + cpmp.placement + " a besoin de  le 2eme drill")
				}

			})
		})
		if (error.length == 0) {
			let reftissuObj = {}
			this.state.modalObj.cuttingPlanMaterials.map(cpm => {
				cpm.cuttingPlanMaterialPlacement.map(cpmp => {
					if (cpmp.placement != null && cpmp.placement != "" && cpm.partNumberMaterial != null && cpm.partNumberMaterial != "") {
						reftissuObj[cpmp.placement.toUpperCase().trim()] = cpm.partNumberMaterial.toUpperCase().trim()
					}
				})
			})
			this.state.modalObj.cuttingPlanPartNumbers.map((cppn, ind) => {
				if (cppn.partNumber != null) {
					axios.get(`/api/ctcFiles/pn/${cppn.partNumber}`)
						.then(res => {
							// Filter out materials containing Bubble, Rectangle, Papier, or Bandle (case insensitive)
							const filteredData = res.data.filter(e => {
								const isValidType = e.type.trim().toLowerCase() === "fabric";
								const materialName = (e.partNumberMaterial || "").toLowerCase();
								const containsExcludedKeywords = materialName.includes("bubble") || 
																materialName.includes("rectangle") || 
																materialName.includes("papier") || 
																materialName.includes("bandle");
								return isValidType && !containsExcludedKeywords;
							});
							this.setState({ ctcData: { ...this.state.ctcData, [cppn.partNumber]: filteredData } })
						})
						.catch(err => {
							this.setState({ error: error })
						})
				}
			})

			// Load ALL placements (activated + optional) from info-list
			let allPlacements = [...placementActivated, ...placementOptional]
			this.setState({ loadingTest: true, placementsInfo: null, placementsInfoOptional: null, digits: [], placementMeta: placementMeta })
			axios.get(`/api/placementData/info-list?placements=${allPlacements.join(",")}&partNumbers=${this.state.modalObj.cuttingPlanPartNumbers.map(e => e.partNumber).join(",")}`)
				.then(res => {
					let resObj = {} // activated=true placements info
					let resObjOptional = {} // activated=false placements info
					let digits = []
					res.data.map(elem => {
						if (digits.includes(elem.digit) === false) {
							digits.push(elem.digit)
						}
						// Determine if this placement is activated or optional
						let isOptional = placementOptional.includes(elem.placement)
						let targetObj = isOptional ? resObjOptional : resObj
						if (targetObj[elem.partNumber] == null) {
							targetObj[elem.partNumber] = { [elem.digit + " - " + elem.sens]: { quantity: placementNbrCouche[elem.placement], reftissu: [reftissuObj[elem.placement.toUpperCase().trim()]], placements: [elem.placement], counterDrill1: elem.counterDrill1, counterDrill2: elem.counterDrill2, graphNumber: elem.graphNumber } }
						} else {
							if (targetObj[elem.partNumber][elem.digit + " - " + elem.sens] == null) {
								targetObj[elem.partNumber][elem.digit + " - " + elem.sens] = { quantity: placementNbrCouche[elem.placement], reftissu: [reftissuObj[elem.placement.toUpperCase().trim()]], placements: [elem.placement], counterDrill1: elem.counterDrill1, counterDrill2: elem.counterDrill2, graphNumber: elem.graphNumber }
							} else {
								targetObj[elem.partNumber][elem.digit + " - " + elem.sens].quantity += placementNbrCouche[elem.placement]
								if (!targetObj[elem.partNumber][elem.digit + " - " + elem.sens].reftissu.includes(reftissuObj[elem.placement.toUpperCase().trim()])) {
									targetObj[elem.partNumber][elem.digit + " - " + elem.sens].reftissu.push(reftissuObj[elem.placement.toUpperCase().trim()])
								}
								if (!targetObj[elem.partNumber][elem.digit + " - " + elem.sens].placements.includes(elem.placement)) {
									targetObj[elem.partNumber][elem.digit + " - " + elem.sens].placements.push(elem.placement)
								}
							}
						}
					})
					this.setState({ placementsInfo: resObj, placementsInfoOptional: resObjOptional })
					axios.post(`/api/placementData/plt-check`, digits.map(e => {
						if (e.endsWith("-LSR")) {
							return e.slice(0, e.length - 4)
						}
						return e;
					}))
						.then(resDg => {
							this.setState({ digits: resDg.data })
						})
					axios.post(`/api/drillEmp/list`, digits.map(e => {
						if (e.endsWith("-LSR")) {
							return e.slice(0, e.length - 4)
						}
						return e;
					}))
						.then(resDrill => {
							let objDrill = {}
							resDrill.data.map(elemDrill => {
								objDrill[elemDrill.pattern.trim().toUpperCase()] = elemDrill
							})

							this.setState({ drillEmpArr: objDrill })
						})

				})
				.catch(err => {
					this.setState({ error: err.response.data })
				})
		} else {
			this.setState({ error: error })
		}
	}

	convertFloat = (float, digit) => {
		//check if the float is a number
		if (!float || isNaN(float)) {
			return float
		}
		return parseFloat(parseFloat(float).toFixed(digit))
	}

	styleVerification = (str) => {
		let obj = { padding: "8", margin: 0 }
		if (str) {
			if (str === "Loading") {
				return { backgroundColor: "grey", ...obj }
			}
			if (str === "Good") {
				return { backgroundColor: "rgb(206 255 179)", ...obj }
			}
			if (str.startsWith("Bad : ")) {
				return { backgroundColor: "#ffdddd", ...obj }
			}
		}
	}

	styleVerificationHeader = (str) => {
		let obj = { padding: "8", borderRadius: "19px", margin: 5 }
		if (str) {
			if (str === "Loading") {
				return { backgroundColor: "grey", ...obj }
			}
			if (str === "Good") {
				return { backgroundColor: "rgb(206 255 179)", ...obj }
			}
			if (str.startsWith("Bad : ")) {
				return { backgroundColor: "rgb(255 181 181)", ...obj }
			}
		}
		return obj
	}

	renderTest = () => {
		return <div style={{ fontSize: 16 }}>
			<h2>Vérification</h2>
			<div className='clickable-elem'
				onClick={() => {
					this.verification1()
				}}
			>
				<div style={this.styleVerificationHeader(this.state.modalObj.verification1)}>1. PNs demandés vs Modèles placements </div>
				{this.renderVerification(this.state.modalObj.verification1)}
			</div>
			<div className='clickable-elem'
				onClick={() => {
					this.verification2()
				}}
			>
				<div style={this.styleVerificationHeader(this.state.modalObj.verification2)}>2. Laize Réf vs Placements </div>
				{this.renderVerification(this.state.modalObj.verification2)}
			</div>
			<div className='clickable-elem'
				onClick={() => {
					this.verification3()
				}}
			>
				<div style={this.styleVerificationHeader(this.state.modalObj.verification3)}>3. DRILLs </div>
				{this.renderVerification(this.state.modalObj.verification3)}
			</div>
			<div className='clickable-elem'
				onClick={() => {
					this.verification4()
				}}
			>
				<div style={this.styleVerificationHeader(this.state.modalObj.verification4)}>4. Placement : Longueur + Chevauchemant + Rotation</div>
				{this.renderVerification(this.state.modalObj.verification4)}
			</div>
			<div className='clickable-elem'
				onClick={() => {
					this.verification5()
				}}
			>
				<div style={this.styleVerificationHeader(this.state.modalObj.verification5)}>5. Matiere QAD = Numéro déf placement </div>
				{this.renderVerification(this.state.modalObj.verification5)}
			</div>
			<div className='clickable-elem'
				onClick={() => {
					this.verification6()
				}}
			>
				<div style={this.styleVerificationHeader(this.state.modalObj.verification6)}>6. Code placement vs Code matiere projet </div>
				{this.renderVerification(this.state.modalObj.verification6)}
			</div>
			<div className='clickable-elem'
				onClick={() => {
					this.verification7Plaque()
				}}
			>
				<div style={this.styleVerificationHeader(this.state.modalObj.verification7Plaque)}>7. Plaque Longueur vs Placement Longueur </div>
				{this.renderVerification(this.state.modalObj.verification7Plaque)}
			</div>

			<div className='clickable-elem'
				onClick={() => {
					this.verification7()
				}}
			>
				<div style={this.styleVerificationHeader(this.state.modalObj.verification7)}>+ Quantité par Modèle </div>
				{this.renderVerification(this.state.modalObj.verification7)}
			</div>
			{this.state.modalObj.alertMessages && (
				<div className='alert alert-warning'>
					<h3>Alert Messages</h3>
					<ul>
						{this.state.modalObj.alertMessages.split(", ").map((message, index) => (
							<li key={index}>{message}</li>
						))}
					</ul>
				</div>
			)}
		</div>
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

	loadSimilarPlan = (pnList) => {
		this.setState({ searchSimilarCuttingPlan: true })
		axios.get(`/api/query/listCuttingPlan?pnList=${pnList.join(",")}`)
			.then(res => {
				let cpList = res.data
				if (cpList.length > 0) {
					axios.get(`/api/cuttingPlanData/list?listId=${cpList.join(",")}`)
						.then(res => {
							const { entityId } = this.props
							if (entityId) {
								this.setState({ cpSimilarList: res.data.filter(e => e.id != entityId), searchSimilarCuttingPlan: false })
							} else {
								this.setState({ cpSimilarList: res.data, searchSimilarCuttingPlan: false })
							}
						})
						.catch(err => {
							this.setState({ cpSimilarList: [], searchSimilarCuttingPlan: false })
						})
				} else {
					this.setState({ cpSimilarList: [], searchSimilarCuttingPlan: false })
				}
			})
			.catch(err => {
				this.setState({ cpSimilarList: [], searchSimilarCuttingPlan: false })
			})

	}

	renderSimilarCuttingPlan = () => {
		return <div>
			<h2 style={{ whiteSpace: "nowrap" }}>
				Plan de coupe similaire
				<button className='btn btn-outline-dark'
					onClick={() => {
						this.loadSimilarPlan(this.state.modalObj.cuttingPlanPartNumbers.map(pn => pn.partNumber).filter(e => e != null && e.length > 0))
					}}
				>
					{this.state.searchSimilarCuttingPlan ? <FontAwesomeIcon icon={faSpinner} spin /> : <FontAwesomeIcon icon={faRefresh} />}
				</button>
			</h2>
			<div className='mb-2'>
				<table className='table m-0 table table-grey-border'>
					<thead style={{ backgroundColor: "black", color: "white" }}>
						<tr>
							<th className='table-elem-sm' >Enabled</th>
							<th className='table-elem-sm' style={{ width: 220 }}>Cutting Plan</th>
							<th className='table-elem-sm' style={{ width: 220 }}>CMS ID</th>
							<th className='table-elem-sm' style={{ width: 220 }}>Copy ID</th>
							<th className='table-elem-sm'>Description</th>
							<th className='table-elem-sm'>Date de creation</th>
							<th className='table-elem-sm'>Commentaire</th>
						</tr>
					</thead>
					<tbody>
						{this.state.cpSimilarList && this.state.cpSimilarList.map((elem, ind) => {
							return <tr key={"similar-" + ind}>
								<td className='table-elem-sm'>
									{/* {elem.enabled ? <FontAwesomeIcon icon={faCheck} color="green" /> : <FontAwesomeIcon icon={faTimes} color="red" />} */}
									{elem.loadingEnabled
										? <FontAwesomeIcon icon={faSpinner} spin style={{ fontSize: 20 }} />
										: <input
											type="checkbox"
											style={{
												width: 20,
												height: 20,
											}}
											checked={elem.enabled || false}
											onChange={(e) => {
												if (window.confirm("Voulez-vous " + (e.target.checked ? "activer" : "désactiver") + " le plan de coupe " + elem.description + " ?")) {

													this.setState({ cpSimilarList: this.state.cpSimilarList.map(e => e.id === elem.id ? { ...e, loadingEnabled: true } : e) })
													if (e.target.checked) {
														axios.post(`/api/cuttingPlan/enable/${elem.id}`)
															.then((res) => {
																this.setState({ cpSimilarList: this.state.cpSimilarList.map(e => e.id === elem.id ? { ...e, enabled: true, loadingEnabled: false } : e) })
															})
															.catch(err => {
																this.setState({ cpSimilarList: this.state.cpSimilarList.map(e => e.id === elem.id ? { ...e, loadingEnabled: false } : e) })
															})
													} else {
														axios.post(`/api/cuttingPlan/disable/${elem.id}`)
															.then((res) => {
																this.setState({ cpSimilarList: this.state.cpSimilarList.map(e => e.id === elem.id ? { ...e, enabled: false, loadingEnabled: false } : e) })
															})
															.catch(err => {
																this.setState({ cpSimilarList: this.state.cpSimilarList.map(e => e.id === elem.id ? { ...e, loadingEnabled: false } : e) })
															})
													}
												}
											}}
										/>
									}
								</td>
								<td className='table-elem-sm table-row-selective'
									onDoubleClick={() => {
										window.open(`/cuttingPlan/${elem.id}`, '_blank').focus();
									}}
								>{elem.id}</td>
								<td className='table-elem-sm table-row-selective'
									onDoubleClick={() => {
										window.open(`/cuttingPlan/${elem.id}`, '_blank').focus();
									}}
								>{elem.cmsId}</td>
								<td className='table-elem-sm table-row-selective'
									onDoubleClick={() => {
										if (elem.cmsId) {
											window.open(`/cuttingPlan/${elem.cmsId}`, '_blank').focus();
										}
									}}
								>{elem.copyId}</td>
								<td className='table-elem-sm'>{elem.description}</td>
								<td className='table-elem-sm'>{elem.createdAt}</td>
								<td className='table-elem-sm'>{elem.commentaire}</td>
							</tr>
						})}
					</tbody>
				</table>
			</div>
		</div>
	}

	render() {
		const entity = "cuttingPlan"
		const { entityId } = this.props

		if (this.state.viewMode && entityId) {
			return (
				<div>
					<div className='entityform-header'
						style={this.state.modalObj.type === "Special Plan" ? { backgroundColor: "#fff0cc" } : {}}
					>
						<h1 className='entityform-title'
						>{metadata[entity].displayName} {this.state.modalObj.id} {this.state.modalObj.copyId && `(Copie de ${this.state.modalObj.copyId})`}</h1>
						<div className='entityform-buttons'>
							<button className='btn btn-primary ml-2' onClick={() => { this.setState({ viewMode: false }) }}>
								<FontAwesomeIcon icon={faArrowRight} /> Modifier
							</button>
							<button className='btn btn-link' onClick={() => { this.props.goBack() }}>Retour</button>
						</div>
					</div>
					<div className='entityform-container'>
						<div className='row entityform-field'>
							<div className='row col-6 py-2'>
								<div className='col-4 text-right'><strong>Projet :</strong></div>
								<div className='col-8'>{this.state.modalObj.projet}</div>
							</div>
							<div className='row col-6 py-2'>
								<div className='col-4 text-right'><strong>Version :</strong></div>
								<div className='col-8'>{this.state.modalObj.version}</div>
							</div>
							<div className='row col-6 py-2'>
								<div className='col-4 text-right'><strong>Définition :</strong></div>
								<div className='col-8'>{this.state.modalObj.definition}</div>
							</div>
							<div className='row col-6 py-2'>
								<div className='col-4 text-right'><strong>CMS ID :</strong></div>
								<div className='col-8'>{this.state.modalObj.cmsId}</div>
							</div>
							<div className='row col-6 py-2'>
								<div className='col-4 text-right'><strong>Type :</strong></div>
								<div className='col-8'>{this.state.modalObj.type || '-'}</div>
							</div>
							<div className='row col-6 py-2'>
								<div className='col-4 text-right'><strong>Status :</strong></div>
								<div className='col-8'>{this.state.modalObj.enabled === true ? <span style={{ color: "green" }}>Activé</span> : <span style={{ color: "red" }}>Désactivé</span>}</div>
							</div>
							<div className='row col-6 py-2'>
								<div className='col-4 text-right'><strong>Commentaire :</strong></div>
								<div className='col-8'>{this.state.modalObj.commentaire}</div>
							</div>
							<div className='row col-6 py-2'>
								<div className='col-4 text-right'><strong>Start Date :</strong></div>
								<div className='col-8'>{this.state.modalObj.startDate || '-'}</div>
							</div>
							<div className='row col-6 py-2'>
								<div className='col-4 text-right'><strong>End Date :</strong></div>
								<div className='col-8'>{this.state.modalObj.endDate || '-'}</div>
							</div>
							{this.state.modalObj.createdBy && <div className='row col-6 py-2'>
								<div className='col-4 text-right'><strong>Créer par :</strong></div>
								<div className='col-8'>{this.state.modalObj.createdBy.lastName} {this.state.modalObj.createdBy.firstName} ({this.state.modalObj.createdBy.matricule}) {this.state.modalObj.createdAt}</div>
							</div>}
							{this.state.modalObj.updatedBy && <div className='row col-6 py-2'>
								<div className='col-4 text-right'><strong>Dernière modification :</strong></div>
								<div className='col-8'>{this.state.modalObj.updatedBy.lastName} {this.state.modalObj.updatedBy.firstName} ({this.state.modalObj.updatedBy.matricule}) {this.state.modalObj.updatedAt}</div>
							</div>}
						</div>

						{this.state.modalObj.cuttingPlanPartNumbers && this.state.modalObj.cuttingPlanPartNumbers.length > 0 && <div>
							<h5 className='mt-3'>Part Numbers</h5>
							<table className='table table-bordered table-sm'>
								<thead><tr>
									<th className='table-elem-sm'>Part Number</th>
									<th className='table-elem-sm'>Item</th>
									<th className='table-elem-sm'>Description</th>
									<th className='table-elem-sm'>Quantité</th>
								</tr></thead>
								<tbody>
									{this.state.modalObj.cuttingPlanPartNumbers.map((pn, i) => <tr key={i}>
										<td className='table-elem-sm'>{pn.partNumber}</td>
										<td className='table-elem-sm'>{pn.item}</td>
										<td className='table-elem-sm'>{pn.description}</td>
										<td className='table-elem-sm'>{pn.quantity}</td>
									</tr>)}
								</tbody>
							</table>
						</div>}

						{this.state.modalObj.cuttingPlanMaterials && this.state.modalObj.cuttingPlanMaterials.length > 0 && <div>
							<h5 className='mt-3'>Matières & Placements</h5>
							<table className='table table-bordered table-sm'>
								<thead><tr>
									<th className='table-elem-sm'>Reftissu</th>
									<th className='table-elem-sm'>Grp</th>
									<th className='table-elem-sm'>Placement</th>
									<th className='table-elem-sm'>Machine</th>
									<th className='table-elem-sm'>Category</th>
									<th className='table-elem-sm'>Nbr Couche</th>
									<th className='table-elem-sm'>MaxPlie</th>
									<th className='table-elem-sm'>Config</th>
									<th className='table-elem-sm'>Laize</th>
									<th className='table-elem-sm'>Longueur</th>
									<th className='table-elem-sm'>LM</th>
									<th className='table-elem-sm'>Drill</th>
									<th className='table-elem-sm'>Rotation</th>
									<th className='table-elem-sm'>Esp. Relarge</th>
									<th className='table-elem-sm'>Activé</th>
								</tr></thead>
								<tbody>
									{this.state.modalObj.cuttingPlanMaterials.sort((a, b) => a.partNumberMaterial.localeCompare(b.partNumberMaterial)).map((cpm, mi) =>
										cpm.cuttingPlanMaterialPlacement.sort((a, b) => a.groupPlacement - b.groupPlacement || Number(b.activated) - Number(a.activated)).map((cpmp, pi) =>
											<tr key={mi + '-' + pi} style={cpmp.activated === false ? { backgroundColor: "#ffffc0" } : {}}>
												{pi === 0 && <td className='table-elem-sm' rowSpan={cpm.cuttingPlanMaterialPlacement.length}>{cpm.partNumberMaterial}</td>}
												<td className='table-elem-sm'>{cpmp.groupPlacement}</td>
												<td className='table-elem-sm'>{cpmp.placement}</td>
												<td className='table-elem-sm'>{cpmp.machine}</td>
												<td className='table-elem-sm'>{cpmp.category}</td>
												<td className='table-elem-sm'>{cpmp.nbrCouche}</td>
												<td className='table-elem-sm'>{cpmp.maxPlie || '-'}</td>
												<td className='table-elem-sm'>{cpmp.config}</td>
												<td className='table-elem-sm'>{cpmp.laize}</td>
												<td className='table-elem-sm'>{cpmp.longueur}</td>
												<td className='table-elem-sm'>{cpmp.longueurMatelas && cpmp.longueurMatelas.toFixed(3)}</td>
												<td className='table-elem-sm'>{cpmp.drill || '-'}</td>
												<td className='table-elem-sm'>{cpmp.rotation || '-'}</td>
												<td className='table-elem-sm'>{cpmp.espaceRelarge || '-'}</td>
												<td className='table-elem-sm'>{cpmp.activated ? <FontAwesomeIcon icon={faCheck} color="green" /> : <FontAwesomeIcon icon={faTimes} color="red" />}</td>
											</tr>
										)
									)}
								</tbody>
							</table>
						</div>}

						{this.renderTest()}

						{this.state.modalObj.alertMessages && <div className='alert alert-warning mt-3'>
							<h5>Alert Messages</h5>
							<ul>
								{this.state.modalObj.alertMessages.split(", ").map((msg, i) => <li key={i}>{msg}</li>)}
							</ul>
						</div>}
					</div>
					{this.renderReftissuModal()}
					{this.renderConfirmModal()}
				</div>
			)
		}

		return (
			<div>
				<div className='entityform-header'
					style={this.state.modalObj.type === "Special Plan" ? { backgroundColor: "#fff0cc" } : {}}
				>
					<h1 className='entityform-title'
					>{metadata[entity].displayName} {this.state.modalObj.id} {this.state.modalObj.copyId && `(Copie de ${this.state.modalObj.copyId})`}</h1>
					<div className='entityform-buttons'>
						<button type="button" className='btn btn-danger ml-2' disabled={this.state.loading}
							onClick={async () => {
								await this.generateByPlacement()
								await this.verification1()
								await this.verification2()
								await this.verification3()
								await this.verification4()
								await this.verification5()
								await this.verification6()
								await this.verification7Plaque()
								console.log({ modalObj: this.state.modalObj })
								if (this.state.modalObj.verification1 === "Good" &&
									this.state.modalObj.verification2 === "Good" &&
									this.state.modalObj.verification3 === "Good" &&
									this.state.modalObj.verification4 === "Good") {
									this.checkObj()
								}
							}}
						>
							<FontAwesomeIcon icon={faFloppyDisk} /> Enregistrer
						</button>
						{Object.keys(this.state.partNumberMaterialConfigs).length > 0 &&
							<button className='btn btn-outline-dark ml-2' onClick={() => {
								if (this.props.entityId != null) {
									let cpmArr = []
									this.state.modalObj.cuttingPlanMaterials.map(cpm => {
										let cpmpArr = []

										let machine = this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].reftissuMachines.find(cm => cm.defaultValue === true) ||
													  this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].reftissuMachines[0]
										let category = this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].reftissuCategories.find(cm => cm.defaultValue === true) ||
													   this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].reftissuCategories[0]

										// Skip this material if no machine or category is available
										if (!machine || !category) {
											console.warn(`No machine or category found for material: ${cpm.partNumberMaterial}`)
											return
										}

										let categoryObj = {}
										this.state.partNumberMaterialConfigs[cpm.partNumberMaterial].reftissuCategories.map(cat => {
											categoryObj[cat.category] = cat.borneMin
										})

										let groupsArr = []
										cpm.cuttingPlanMaterialPlacement.map(cpmp => {
											if (!groupsArr.includes(cpmp.groupPlacement)) {
												groupsArr.push(cpmp.groupPlacement)
											}
										})

										groupsArr.map(group => {
											let cpmpGood = cpm.cuttingPlanMaterialPlacement.filter(e => e.machine === machine.machineType && e.category === category.category && e.groupPlacement === group)
											if (cpmpGood.length > 0) {
												if (cpmpGood[0].activated === false) {
													cpmpArr.push({ ...cpmpGood[0], activated: true })
													// we make it activated and desactivate the others in the same groupPlacement
													cpm.cuttingPlanMaterialPlacement
														.filter(e => e.placement !== cpmpGood[0].placement && e.groupPlacement === group)
														.map(cpmp => {
															cpmpArr.push({ ...cpmp, activated: false })
														})
												} else {
													cpm.cuttingPlanMaterialPlacement
														.filter(e => e.groupPlacement === group)
														.map(cpmp => {
															cpmpArr.push({ ...cpmp })
														})
												}

												return;
											} else {
												let obj = {}
												obj.machine = machine.machineType
												obj.maxPlie = machine.maxPlie
												obj.maxPlieDrill = machine.maxPlieDrill
												obj.maxDrill = machine.maxDrill
												obj.pliesConfig = machine.pliesConfig
												obj.category = category.category
												obj.laize = category.borneMin
												obj.groupPlacement = group
												obj.drill = cpm.cuttingPlanMaterialPlacement.filter(e => e.groupPlacement === group)[0].drill
												obj.activated = true
												cpmpArr.push(obj)
												cpm.cuttingPlanMaterialPlacement
													.filter(e => e.groupPlacement === group)
													.map(cpmp => {
														if (cpmp.groupPlacement === group) {
															cpmpArr.push({ ...cpmp, activated: false })
														}
													})
												return;
											}
										})

										// update category borneMin
										cpmpArr.map(cpmp => {
											if (cpmp.category && categoryObj[cpmp.category]) {
												cpmp.laize = categoryObj[cpmp.category]
											}
										})

										cpmArr.push({ ...cpm, cuttingPlanMaterialPlacement: cpmpArr })
									})
									this.setState({
										modalObj: {
											projet: this.state.modalObj.projet,
											version: this.state.modalObj.version,
											// version2: this.state.modalObj.version2,
											definition: this.state.modalObj.definition,
											commentaire: this.state.modalObj.commentaire,
											cuttingPlanPartNumbers: this.state.modalObj.cuttingPlanPartNumbers,
											cuttingPlanMaterials: cpmArr,
											copyId: this.state.modalObj.id,
											cmsId: "",
											type: this.state.modalObj.type
										},
										searchText: ""
									})
								}
							}}><FontAwesomeIcon icon={faPlus} /> Copy</button>}
						<button className='btn btn-outline-primary ml-2' onClick={() => { this.setState({ modalDetail: { ...this.state.modalObj } }) }}>
							<FontAwesomeIcon icon={faEye} /> Détails
						</button>
						<button className='btn btn-link' onClick={() => { this.props.goBack() }}>Annuler</button>
					</div>
				</div>
				<div className='entityform-container'>
					{this.renderForm(entityId)}

					{this.renderTest()}
					{this.renderPnTable()}
					{this.renderSimilarCuttingPlan()}
					<div style={{ width: 500 }}>
						<InputGroup className="crud-table-search-input">
							<FormControl id="cms-input" value={this.state.searchText ? this.state.searchText : this.state.modalObj.cmsId}
								style={{ width: 100 }}
								onChange={(e) => {
									this.setState({ searchText: e.target.value })
								}}
								onKeyUp={(e) => {
									if (e.key === "Enter") {
										axios.get(`/api/cms/planCoupe/${this.state.searchText ? this.state.searchText : this.state.modalObj.cmsId}${this.state.modalObj.id ? "/" + this.state.modalObj.id : ""}`)
											.then(resCMS => {
												let objPlan = {
													projet: resCMS.data.groupPlanCoupe,
													version: resCMS.data.versionPlanCoupe,
													definition: resCMS.data.definitionPlanCoupe,
													enabled: resCMS.data.statusPlanCoupe,
													startDate: resCMS.data.startDateFromPlanCoupe ? resCMS.data.startDateFromPlanCoupe.substring(0, 16).replace("T", ", ") : null,
													endDate: resCMS.data.endDateToPlanCoupe ? resCMS.data.endDateToPlanCoupe.substring(0, 16).replace("T", ", ") : null,
													commentaire: resCMS.data.commentplanCoupe,
													id: this.state.modalObj.id,
													cmsId: this.state.searchText ? this.state.searchText : this.state.modalObj.cmsId
												}
												objPlan.cuttingPlanPartNumbers = []
												resCMS.data.partNumberPlanCoupes.map(pn => {
													objPlan.cuttingPlanPartNumbers.push({
														partNumber: pn.partNumberPlanCoupe,
														item: pn.kitTextilPlanCoupe,
														description: pn.descriptionPartNumberPlanCoupe,
														quantity: pn.quantityPartNumberPlanCoupe
													})
												})


												let arrBoom = []
												let cuttingPlanMaterialsList = []

												Promise.all(objPlan.cuttingPlanPartNumbers.map((elemPn, indPn) => {
													// let dep = ["project=" + this.state.modalObj.projet]
													// if (this.state.modalObj.version) { dep.push("version=" + this.state.modalObj.version) }
													// dep.push("partNumber=" + elemPn.partNumber)
													return axios.get(`/api/partNumberBoom/list?${"partNumber=" + elemPn.partNumber}`)
												}))
													.then(res => {
														objPlan.cuttingPlanPartNumbers.map((elemPn, indPn) => {
															arrBoom = [...arrBoom, ...res[indPn].data]
															let arrReftissuOption = res[indPn].data.filter(e => e.partNumber === elemPn.partNumber)
															arrReftissuOption.map((option) => {
																let i = cuttingPlanMaterialsList.findIndex(e => e.partNumberMaterial === option.partNumberMaterial)
																if (i >= 0) {
																	if (!cuttingPlanMaterialsList[i].partNumbers.split(",").includes(elemPn.partNumber)) {
																		cuttingPlanMaterialsList[i].partNumbers += "," + elemPn.partNumber
																	}
																} else {
																	let i = 0
																	let arrSpreading = resCMS.data.spreadingCuttingPlanCoupes
																		.filter(elemCMS => elemCMS.itemNumberPlanCoupe === option.partNumberMaterial && (elemCMS.placementPlanCoupe && elemCMS.placementPlanCoupe.trim().length > 0))
																		.sort((a, b) => (a.idSpreadingCuttingParentPlanCoupe - b.idSpreadingCuttingParentPlanCoupe) || (Number(b.defaultSpreadingCuttingPlanCoupe) - Number(a.defaultSpreadingCuttingPlanCoupe)))
																		.map((elemCMS, ind) => {
																			let arrDrill = ["", ""]
																			if (ind === 0) {
																				i = 0
																			}
																			if (elemCMS.defaultSpreadingCuttingPlanCoupe === true) {
																				i++
																			}
																			if (elemCMS.drillPlanCoupes && elemCMS.drillPlanCoupes.length === 1) {
																				arrDrill[0] = elemCMS.drillPlanCoupes[0].drillPlan
																			} else {
																				arrDrill = elemCMS.drillPlanCoupes.map(e => e.drillPlan)
																			}
																			console.log({ arrDrill })
																			return {
																				machine: this.convertToMachine(elemCMS.machinePlanCoupe),
																				category: elemCMS.categoryPlanCoupe,
																				placement: elemCMS.placementPlanCoupe,
																				groupPlacement: i,
																				activated: elemCMS.defaultSpreadingCuttingPlanCoupe,
																				drill: arrDrill.join(",")
																			}
																		})
																	cuttingPlanMaterialsList.push({
																		partNumberMaterial: option.partNumberMaterial,
																		description: option.partNumberMaterialDescription,
																		partNumbers: option.partNumber,
																		cuttingPlanMaterialPlacement: arrSpreading
																	})
																}
															})
														})
													})
													.then(() => {
														let obj = {}
														axios.get(`/api/partNumberMaterialConfig/pns/${cuttingPlanMaterialsList.map(e => e.partNumberMaterial).join(",")}?projet=${this.state.modalObj.projet}`)
															.then(res => {
																res.data.map(e => {
																	obj[e.partNumberMaterial] = { ...e }
																})
																cuttingPlanMaterialsList = [...cuttingPlanMaterialsList].map(e => {
																	if (obj[e.partNumberMaterial]) {
																		for (let j = 0; j < e.cuttingPlanMaterialPlacement.length; j++) {
																			e.tauxScrap = obj[e.partNumberMaterial].tauxScrap
																			e.rotation = obj[e.partNumberMaterial].rotation
																			e.plaque = obj[e.partNumberMaterial].plaque
																			e.vitesse = obj[e.partNumberMaterial].vitesse
																			e.matelassageEndroit = obj[e.partNumberMaterial].matelassageEndroit
																			e.description = obj[e.partNumberMaterial].description

																			let machine = obj[e.partNumberMaterial].reftissuMachines.find(cm => (cm.machineType === e.cuttingPlanMaterialPlacement[j].machine))
																			if (machine != null) {

																				e.cuttingPlanMaterialPlacement[j].machine = machine.machineType
																				e.cuttingPlanMaterialPlacement[j].maxPlie = machine.maxPlie
																				e.cuttingPlanMaterialPlacement[j].maxPlieDrill = machine.maxPlieDrill
																				e.cuttingPlanMaterialPlacement[j].maxDrill = machine.maxDrill
																				e.cuttingPlanMaterialPlacement[j].pliesConfig = machine.pliesConfig
																			}
																			let category = obj[e.partNumberMaterial].reftissuCategories.find(cm => cm.category === e.cuttingPlanMaterialPlacement[j].category)
																			if (category != null) {
																				e.cuttingPlanMaterialPlacement[j].category = category.category
																				e.cuttingPlanMaterialPlacement[j].laize = category.borneMin
																			}
																		}
																	}
																	return e;
																})

															})
															.finally(() => {
																let partnumbersObject = {}
																objPlan.cuttingPlanPartNumbers.map(cppn => {
																	partnumbersObject[cppn.partNumber] = cppn
																})
																this.setState({
																	modalObj: {
																		...objPlan,
																		cuttingPlanPartNumbers: [...objPlan.cuttingPlanPartNumbers],
																		cuttingPlanMaterials: cuttingPlanMaterialsList.map(cpmElem => {
																			let total = 0;
																			cpmElem.partNumbers.split(",").map(pnElem => {
																				arrBoom.filter(e => (e.partNumber === pnElem && cpmElem.partNumberMaterial === e.partNumberMaterial)).map(pnObj => {
																					total += pnObj.quantityPer * partnumbersObject[pnElem].quantity
																				})
																			})
																			// Apply 1.03 multiplier only if not a plaque material (consistent with planUsage calculation)
																			cpmElem.qadUsage = !cpmElem.plaque ? total * 1.03 : total
																			return cpmElem
																		})
																	},
																	partNumberMaterialConfigs: { ...obj },
																	optionsList: {
																		...this.state.optionsList,
																		partNumberBoom: arrBoom.map(e => { return { label: e.partNumber, value: e.partNumber, item: e } })
																	}
																})
															})
													})

											})
											.catch(err => {
												this.setState({ error: err.response.data })
											})
									}
								}}
								placeholder="Chercher..." size="sm" />
							<InputGroup.Append>
								<Button id="cms-button" variant="primary" size="sm" onClick={() => {
									axios.get(`/api/cms/planCoupe/${this.state.searchText ? this.state.searchText : this.state.modalObj.cmsId}${this.state.modalObj.id ? "/" + this.state.modalObj.id : ""}`)
										.then(resCMS => {
											let objPlan = {
												projet: resCMS.data.groupPlanCoupe,
												version: resCMS.data.versionPlanCoupe,
												definition: resCMS.data.definitionPlanCoupe,
												enabled: resCMS.data.statusPlanCoupe,
												startDate: resCMS.data.startDateFromPlanCoupe ? resCMS.data.startDateFromPlanCoupe.substring(0, 16).replace("T", ", ") : null,
												endDate: resCMS.data.endDateToPlanCoupe ? resCMS.data.endDateToPlanCoupe.substring(0, 16).replace("T", ", ") : null,
												commentaire: resCMS.data.commentplanCoupe,
												type: resCMS.data.typePlanCoupe,
												id: this.state.modalObj.id,
												cmsId: this.state.searchText ? this.state.searchText : this.state.modalObj.cmsId
											}
											objPlan.cuttingPlanPartNumbers = []
											resCMS.data.partNumberPlanCoupes.map(pn => {
												objPlan.cuttingPlanPartNumbers.push({
													partNumber: pn.partNumberPlanCoupe,
													item: pn.kitTextilPlanCoupe,
													description: pn.descriptionPartNumberPlanCoupe,
													quantity: pn.quantityPartNumberPlanCoupe
												})
											})

											let arrBoom = []
											let cuttingPlanMaterialsList = []

											Promise.all(objPlan.cuttingPlanPartNumbers.map((elemPn, indPn) => {
												// let dep = ["project=" + this.state.modalObj.projet]
												// if (this.state.modalObj.version) { dep.push("version=" + this.state.modalObj.version) }
												// dep.push("partNumber=" + elemPn.partNumber)
												return axios.get(`/api/partNumberBoom/list?${"partNumber=" + elemPn.partNumber}`)
											}))
												.then(res => {
													objPlan.cuttingPlanPartNumbers.map((elemPn, indPn) => {
														arrBoom = [...arrBoom, ...res[indPn].data]
														let arrReftissuOption = res[indPn].data.filter(e => e.partNumber === elemPn.partNumber)
														arrReftissuOption.map((option) => {
															let i = cuttingPlanMaterialsList.findIndex(e => e.partNumberMaterial === option.partNumberMaterial)
															if (i >= 0) {
																if (!cuttingPlanMaterialsList[i].partNumbers.split(",").includes(elemPn.partNumber)) {
																	cuttingPlanMaterialsList[i].partNumbers += "," + elemPn.partNumber
																}
															} else {
																let i = 0
																let arrSpreading = resCMS.data.spreadingCuttingPlanCoupes
																	.filter(elemCMS => elemCMS.itemNumberPlanCoupe === option.partNumberMaterial && (elemCMS.placementPlanCoupe && elemCMS.placementPlanCoupe.trim().length > 0))
																	.sort((a, b) => (a.idSpreadingCuttingParentPlanCoupe - b.idSpreadingCuttingParentPlanCoupe) || (Number(b.defaultSpreadingCuttingPlanCoupe) - Number(a.defaultSpreadingCuttingPlanCoupe)))
																	.map((elemCMS, ind) => {
																		let arrDrill = ["", ""]
																		if (ind === 0) {
																			i = 0
																		}
																		if (elemCMS.defaultSpreadingCuttingPlanCoupe === true) {
																			i++
																		}
																		if (elemCMS.drillPlanCoupes && elemCMS.drillPlanCoupes.length === 1) {
																			arrDrill[0] = elemCMS.drillPlanCoupes[0].drillPlan
																		} else {
																			arrDrill = elemCMS.drillPlanCoupes.map(e => e.drillPlan)
																		}
																		console.log({ arrDrill })
																		return {
																			machine: this.convertToMachine(elemCMS.machinePlanCoupe),
																			category: elemCMS.categoryPlanCoupe,
																			placement: elemCMS.placementPlanCoupe,
																			groupPlacement: i,
																			activated: elemCMS.defaultSpreadingCuttingPlanCoupe,
																			drill: arrDrill.join(",")
																		}
																	})
																cuttingPlanMaterialsList.push({
																	partNumberMaterial: option.partNumberMaterial,
																	description: option.partNumberMaterialDescription,
																	partNumbers: option.partNumber,
																	cuttingPlanMaterialPlacement: arrSpreading
																})
															}
														})
													})
												})
												.then(() => {
													let obj = {}
													axios.get(`/api/partNumberMaterialConfig/pns/${cuttingPlanMaterialsList.map(e => e.partNumberMaterial).join(",")}?projet=${this.state.modalObj.projet}`)
														.then(res => {
															res.data.map(e => {
																obj[e.partNumberMaterial] = { ...e }
															})
															cuttingPlanMaterialsList = [...cuttingPlanMaterialsList].map(e => {
																if (obj[e.partNumberMaterial]) {
																	for (let j = 0; j < e.cuttingPlanMaterialPlacement.length; j++) {
																		e.tauxScrap = obj[e.partNumberMaterial].tauxScrap
																		e.rotation = obj[e.partNumberMaterial].rotation
																		e.plaque = obj[e.partNumberMaterial].plaque
																		e.vitesse = obj[e.partNumberMaterial].vitesse
																		e.matelassageEndroit = obj[e.partNumberMaterial].matelassageEndroit
																		e.description = obj[e.partNumberMaterial].description
																		let machine = obj[e.partNumberMaterial].reftissuMachines.find(cm => (cm.machineType === e.cuttingPlanMaterialPlacement[j].machine))
																		if (machine != null) {

																			e.cuttingPlanMaterialPlacement[j].machine = machine.machineType
																			e.cuttingPlanMaterialPlacement[j].maxPlie = machine.maxPlie
																			e.cuttingPlanMaterialPlacement[j].maxPlieDrill = machine.maxPlieDrill
																			e.cuttingPlanMaterialPlacement[j].maxDrill = machine.maxDrill
																			e.cuttingPlanMaterialPlacement[j].pliesConfig = machine.pliesConfig
																		}
																		let category = obj[e.partNumberMaterial].reftissuCategories.find(cm => cm.category === e.cuttingPlanMaterialPlacement[j].category)
																		if (category != null) {
																			e.cuttingPlanMaterialPlacement[j].category = category.category
																			e.cuttingPlanMaterialPlacement[j].laize = category.borneMin
																		}
																	}
																}
																return e;
															})

														})
														.finally(() => {
															let partnumbersObject = {}
															objPlan.cuttingPlanPartNumbers.map(cppn => {
																partnumbersObject[cppn.partNumber] = cppn
															})
															this.setState({
																modalObj: {
																	...objPlan,
																	cuttingPlanPartNumbers: [...objPlan.cuttingPlanPartNumbers],
																	cuttingPlanMaterials: cuttingPlanMaterialsList.map(cpmElem => {
																		let total = 0;
																		cpmElem.partNumbers.split(",").map(pnElem => {
																			arrBoom.filter(e => (e.partNumber === pnElem && cpmElem.partNumberMaterial === e.partNumberMaterial)).map(pnObj => {
																				total += pnObj.quantityPer * partnumbersObject[pnElem].quantity
																			})
																		})
																		// Apply 1.03 multiplier only if not a plaque material (consistent with planUsage calculation)
																		cpmElem.qadUsage = !cpmElem.plaque ? total * 1.03 : total
																		return cpmElem
																	})
																},
																partNumberMaterialConfigs: { ...obj },
																optionsList: {
																	...this.state.optionsList,
																	partNumberBoom: arrBoom.map(e => { return { label: e.partNumber, value: e.partNumber, item: e } })
																}
															})
														})
												})

										})
										.catch(err => {
											this.setState({ error: err.response.data })
										})
								}}>
									<span><FontAwesomeIcon icon={faSearch} />Search CMS</span>
								</Button>


							</InputGroup.Append>
						</InputGroup>
					</div>
					{this.renderReftissus()}
					{this.renderRapportPlacement()}
					{this.renderRapportModel()}
					{this.renderRapportDrill()}
					{this.renderConsommation()}
					<h2>Historique</h2>
					{this.renderHistorique()}

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
				{this.renderReftissuModal()}
				{this.renderConfirmModal()}
				{this.renderDetailModal()}
				{this.renderImportModal()}
				{this.renderDrillModal()}

			</div>
		)
	}
}

