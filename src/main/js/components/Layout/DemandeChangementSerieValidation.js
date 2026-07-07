import axios from 'axios';
import React, { Component } from 'react'
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Button, ButtonGroup, Modal } from 'react-bootstrap'

import { faArrowLeft, faArrowRight, faArrowUp, faBan, faCheck, faEye, faInfo, faPrint, faSpinner, faSync, faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import GammePn from './GammePn';
import ReactToPrint from "react-to-print";
import logo from '../../assets/images/lear_logo.png'
import moment from 'moment';
import Select from "react-select";
import { optionTypeDemandeChangementSerie } from '../../metadata';


class DemandeChangementSerieValidation extends Component {

	constructor(props) {
		super(props);
		this.state = {
			modalObj: {},
			user: null,
			showDetail: false,
			message: null,

			sequence: '',
			cuttingRequest: null,
			selectedBoxs: [],
			drillEmpArr: {},
			modalRotate: false,
			qnList: [],
			showSerieForm: null,
			loadingRefresh: false,
			modalSerie: null,
			modalSerieForm: null,
			changeLoading: false,
			partNumberMaterialConfigs: {},

			margeInitial: null,
			partNumbersInitial: null,
			targetQuantityPN: null,
			error: {},
			allowSaving: false,
			loadingPlacement: false,
			dataIMS: null,
			optionsList: {},
			reftissuMachineOld: null,
		}
	}

	componentDidMount() {
		if (this.props.security) {
			axios.get(`/api/user/${this.props.security.user.matricule}`)
				.then(res => {
					this.setState({
						user: { ...res.data },
					})
				})
				.catch(err => {
					if (err.response.data != null && err.response.data.username === "Invalid Username") {
						window.location.pathname = "/login";
					}
				})
		}
		const urlParams = new URLSearchParams(window.location.search);
		const id = urlParams.get('id');
		if (id) {
			this.loadSequence(id)
		}
        axios.get(`/api/machineType/list`)
            .then((res) => {
                this.setState({ optionsList: { ...this.state.optionsList, machineType: res.data.map(elem => { return { label: elem.name, value: elem.name } }) } })
            })

		
	}

	loadSequence = async (id) => {
		this.setState({ showDetail: null })
		let resDemande
		try {
			resDemande = await axios.get(`/api/demandeChangementSerie/${id}`)
		} catch (err) {
			return;
		}

		let resConfig = await axios.get(`/api/partNumberMaterialConfig/pns/${resDemande.data.partNumberMaterial}`)
		this.setState({
			partNumberMaterialConfigs: { ...this.state.partNumberMaterialConfigs, [resDemande.data.partNumberMaterial]: resConfig.data.length > 0 ? resConfig.data[0] : null },
			modalObj: { ...resDemande.data },
			cuttingRequest: null,
			selectedBoxs: [],
			drillEmpArr: {},
			modalRotate: false,
			qnList: [],
			showSerieForm: null,
			loadingRefresh: false,
		})
		try {
			let resIMS = await axios.get(`/api/query/refDetails?reftissu=${resDemande.data.partNumberMaterial}`)
			this.setState({ dataIMS: resIMS.data })
		} catch (err) {
			this.setState({ dataIMS: null })
		}

		let sequence = resDemande.data.sequence
		let res = await axios.get(`/api/cuttingRequestData/${sequence}`)
		if (res.data && res.data.sequence) {
			this.setState({ cuttingRequest: res.data })
			let resSerie = await axios.get(`/api/cuttingRequestSerieData/bySequence/${sequence}`)
			let obj;
			if (resSerie.data) {
				obj = resSerie.data.find(e => e.serie == resDemande.data.serie)
				this.setState({
					cuttingRequest: {
						...this.state.cuttingRequest,
						cuttingRequestSeries: resSerie.data,
					},
					modalSerieForm: { ...obj }
				})

			}
			let resPartNumbers = await axios.get(`/api/cuttingRequestPartNumberData/bySequence/${sequence}`)
			if (resPartNumbers.data) {
				this.setState({ cuttingRequest: { ...this.state.cuttingRequest, cuttingRequestPartNumbers: resPartNumbers.data } })
			}
			let resBoxs = await axios.get(`/api/cuttingRequestBoxData/bySequence/${sequence}`)
			if (resBoxs.data) {
				this.setState({ cuttingRequest: { ...this.state.cuttingRequest, cuttingRequestBoxs: resBoxs.data } })
			}
			if (obj) {
				this.loadPlacement(obj.placement, true)
			}
			this.chargeQns(this.state.cuttingRequest)
		} else {
			this.setState({ error: "Sequence not found" })
		}

	}

	renderSequenceDetails = () => {
		const { user } = this.props.security;
		const { cuttingRequest } = this.state;
		return <div>
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
					<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Cutting Plan Id : </strong></div>
					<div className='text-no-wrap' style={{ width: "35%", display: "flex", alignItems: "baseline" }}>{cuttingRequest.cuttingPlanId} <button
						className='btn btn-sm btn-primary' style={{ padding: "0 12" }}
						onClick={() => {
							window.open(`/cuttingPlan/${cuttingRequest.cuttingPlanId}`, '_blank').focus();
						}}><FontAwesomeIcon icon={faArrowRight} /></button></div>
					<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>CMS ID : </strong></div>
					<div className='' style={{ width: "35%" }}>{cuttingRequest.cmsId}</div>
				</div>
				<div className='d-flex'>
					<label className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Zone : </strong></label>
					<div className='text-no-wrap' style={{ width: "200px" }}>
						{cuttingRequest.zone?.nom}
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
								: this.setState({ selectedBoxs: [...this.state.selectedBoxs, ...cuttingRequest.cuttingRequestBoxs.filter(e => e.partNumber == elemPn.partNumber)].sort((a, b) => a.id.localeCompare(b.id)).map(e => { return { ...e, sequence: (cuttingRequest.sequence || "") } }) })
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
							<th className='table-elem-sm'>Temps De Coupe</th>
							<th className='table-elem-sm' colSpan={2}>Matelassage</th>
							<th className='table-elem-sm' colSpan={2}>Coupe</th>
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
								let qn = null
								let arrQn = this.state.qnList.filter(e => e.reftissu.toUpperCase() === elemPn.partNumberMaterial.toUpperCase() && e.resultat === "Non ok" && e.placement && (e.placement.toUpperCase().trim() === "ALL" || e.placement.split(";").map(e => e.trim().toUpperCase()).includes(elemPn.placement.trim().toUpperCase())))
								if (arrQn.length > 0) {
									qn = arrQn[0]
								} else {
									arrQn = this.state.qnList.filter(e => e.reftissu.toUpperCase() === elemPn.partNumberMaterial.toUpperCase() && e.resultat === "Formation" && e.placement && (e.placement.toUpperCase().trim() === "ALL" || e.placement.split(";").map(e => e.trim().toUpperCase()).includes(elemPn.placement.trim().toUpperCase())))
									if (arrQn.length > 0) {
										qn = arrQn[0]
									}
								}

								return <tr style={elemPn.activated === false ? { backgroundColor: "#ffffc0" } : { backgroundColor: "" }} className="clickable-element"
									onDoubleClick={() => {
										this.setState({ showSerieForm: elemPn.serie, modalSerie: { ...elemPn } })
									}}

								>
									<td className='table-elem-sm'>{elemPn.serie}</td>
									<td className='table-elem-sm'
										style={(qn && qn.resultat === "Non ok") ? { backgroundColor: "red" }
											: (qn && qn.resultat === "Formation") ? { backgroundColor: "yellow" }
												: {}}
									>{elemPn.partNumberMaterial}</td>
									<td className='table-elem-sm'>{elemPn.description}</td>
									<td className='table-elem-sm'>{elemPn.matelassageEndroit}</td>
									<td className='table-elem-sm'>{elemPn.longueur && elemPn.longueur.toFixed(3)}</td>
									<td className='table-elem-sm'>{elemPn.nbrCouche}</td>
									<td className='table-elem-sm'>{elemPn.laize}</td>
									<td className='table-elem-sm'
										style={(qn && qn.placement && qn.placement.split(";").map(e => e.trim().toUpperCase()).includes(elemPn.placement.trim().toUpperCase()) && qn.resultat === "Non ok") ? { backgroundColor: "red" }
											: (qn && qn.placement && qn.placement.split(";").map(e => e.trim().toUpperCase()).includes(elemPn.placement.trim().toUpperCase()) && qn.resultat === "Formation") ? { backgroundColor: "yellow" }
												: {}}
									>{elemPn.placement}
										<button className='btn btn-outline-primary'
											style={{
												fontSize: 12,
												padding: 2,
												marginLeft: 3,
												width: 28,
											}}
											onClick={() => {
												// open a new page /cutfileviewer/1DEM365/view
												window.open("/cutfileviewer/" + elemPn.placement + "/view", "_blank")
											}}
										>
											<FontAwesomeIcon icon={faEye} />
										</button>
									</td>
									<td className='table-elem-sm'>{elemPn.config}</td>
									<td className='table-elem-sm'>{arrDrill[0]}</td>
									<td className='table-elem-sm'>{arrDrill[1]}</td>
									<td className='table-elem-sm'>{elemPn.machine}</td>
									<td className='table-elem-sm'>{elemPn.tempsDeCoupe && <span>{elemPn.tempsDeCoupe - elemPn.tempsDeCoupe % 1} min {((elemPn.tempsDeCoupe % 1) * 60).toFixed(0)} s</span>}</td>
									<td className='table-elem-sm'>
										{elemPn.tableMatelassage}
									</td>
									<td className='table-elem-sm' style={{ backgroundColor: this.returnColorStatus(elemPn.statusMatelassage) }}>
										{elemPn.dateDebutMatelassage ? <span>{elemPn.dateDebutMatelassage}</span> : "..."}<br />
										{elemPn.dateFinMatelassage ? <span>{elemPn.dateFinMatelassage}</span> : "..."}
									</td>
									<td className='table-elem-sm'>
										{elemPn.tableCoupe}
									</td>
									<td className='table-elem-sm' style={{ backgroundColor: this.returnColorStatus(elemPn.statusCoupe) }}>
										{elemPn.dateDebutCoupe ? <span>{elemPn.dateDebutCoupe}</span> : "..."}<br />
										{elemPn.dateFinCoupe ? <span>{elemPn.dateFinCoupe}</span> : "..."}
									</td>

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
								: this.setState({ selectedBoxs: [...this.state.selectedBoxs, { ...elemBox, sequence: (cuttingRequest.sequence || "") }].sort((a, b) => a.id.localeCompare(b.id)) })
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
			<div className='d-flex pb-2' style={{ justifyContent: "end" }}>
				<ReactToPrint
					onBeforeGetContent={() => {
						return new Promise((resolve, reject) => {
							this.setState({}, () => resolve())
						});
					}}
					onAfterPrint={() => { }}
					onPrintError={() => { }}
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
			</div>
			{ }
			<div style={{ overflow: "hidden", height: 0 }}>
				<div className='' ref={elem => this.componentRef = elem}>
					{this.state.selectedBoxs.length > 0 && this.state.selectedBoxs.map(box => {
						let wd = 1565, hg = 1100
						return [<div style={{ height: wd }} key={"gamme-" + box.wo}><GammePn box={box} /></div>, <div className="page-break" />]
					})}
				</div>
			</div>
			<div style={{ overflow: "hidden", height: 0 }}>
				{cuttingRequest && this.renderPlanCoupe(cuttingRequest, user)}
			</div>
		</div>
	}

	renderConfirmModal = () => {
		let arrTable = [], arrPn = {}, arrDrillPlacement = {}
		if (this.state.placementsInfo && this.state.cuttingRequest && this.state.cuttingRequest.cuttingRequestPartNumbers) {
			this.state.cuttingRequest.cuttingRequestPartNumbers.map(elem => {
				arrPn[elem.partNumber] = elem.quantity
			})
			this.state.cuttingRequest.cuttingRequestSeries.map((crs) => {
				let arrDrill = (crs.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
				arrDrillPlacement[crs.placement] = arrDrill
			})

			const sortedKeys = Object.keys(this.state.placementsInfo).sort((a, b) => a.localeCompare(b));
			sortedKeys.map((partNumber, ind1) => {
				if (this.state.placementsInfo[partNumber] != null) {
					let arrPanelNumber = []
					let i = 0
					const sortedDigitKeys = Object.keys(this.state.placementsInfo[partNumber]).sort((a, b) => a.localeCompare(b));

					sortedDigitKeys.map((digit, ind2) => {
						// put in pettern the content of digit without the last 4 letters
						let pattern = digit.slice(0, digit.length - 4);
						if (pattern.endsWith("-LSR")) {
							pattern = pattern.slice(0, pattern.length - 4);
						}
						let obj = null
						if (this.state.ctcData[partNumber]) {
							obj = this.state.ctcData[partNumber].find(elem => elem.pattern === pattern && arrPanelNumber.includes(elem.panelNumber) === false)
							if (obj) {
								arrPanelNumber.push(obj.panelNumber)
							}
						}
						let digitObj = this.state.digits.find(elem => elem.pattern === pattern)
						let erreurDrill1 = false, erreurDrill2 = false
						this.state.placementsInfo[partNumber][digit].placements.map(placement => {
							if (this.state.drillEmpArr[pattern.trim().toUpperCase()]) {
								if (this.state.drillEmpArr[pattern.trim().toUpperCase()].drill1 > 0 && (arrDrillPlacement[placement][0] === null || this.state.drillEmpArr[pattern.trim().toUpperCase()].drill1 + 1 < (arrDrillPlacement[placement][0]) || this.state.drillEmpArr[pattern.trim().toUpperCase()].drill1 - 1 > parseInt(arrDrillPlacement[placement][0]))) {
									erreurDrill1 = true
								}
								if (this.state.drillEmpArr[pattern.trim().toUpperCase()].drill2 > 0 && (arrDrillPlacement[placement][1] === null || this.state.drillEmpArr[pattern.trim().toUpperCase()].drill2 + 1 < (arrDrillPlacement[placement][1]) || this.state.drillEmpArr[pattern.trim().toUpperCase()].drill2 - 1 > parseInt(arrDrillPlacement[placement][1]))) {
									erreurDrill2 = true
								}
							}
						})
						if ((this.state.placementsInfo[partNumber][digit].counterDrill2 == 0 && this.state.drillEmpArr[pattern.trim().toUpperCase()] && this.state.drillEmpArr[pattern.trim().toUpperCase()].drill2 > 0)
							|| (this.state.placementsInfo[partNumber][digit].counterDrill2 > 0 && !(this.state.drillEmpArr[pattern.trim().toUpperCase()] && this.state.drillEmpArr[pattern.trim().toUpperCase()].drill2 > 0))) {
							erreurDrill2 = true
						}
						if ((this.state.placementsInfo[partNumber][digit].counterDrill1 == 0 && this.state.drillEmpArr[pattern.trim().toUpperCase()] && this.state.drillEmpArr[pattern.trim().toUpperCase()].drill1 > 0)
							|| (this.state.placementsInfo[partNumber][digit].counterDrill1 > 0 && !(this.state.drillEmpArr[pattern.trim().toUpperCase()] && this.state.drillEmpArr[pattern.trim().toUpperCase()].drill1 > 0))) {
							erreurDrill1 = true
						}
						arrTable.push(
							<tr key={ind1 + "-" + ind2}>
								{i === 0 && <td className='table-elem-sm' rowSpan={Object.keys(this.state.placementsInfo[partNumber]).length}>{partNumber} ({arrPn[partNumber]})</td>}
								<td className='table-elem-sm'
									style={(obj && obj.partNumberMaterial && this.state.placementsInfo[partNumber][digit].reftissu && obj.partNumberMaterial.trim().toUpperCase() !== this.state.placementsInfo[partNumber][digit].reftissu.join("/").trim().toUpperCase()) ? { backgroundColor: "rgb(255, 163, 163)" } : {}}
								>{this.state.placementsInfo[partNumber][digit].reftissu.join("/")}</td>
								<td className='table-elem-sm clickable-element'
									onDoubleClick={() => {
										let arrPlct = this.state.placementsInfo[partNumber][digit].placements
										if (arrPlct.length > 0) {
											window.open("/cutfileviewer/" + arrPlct[0] + "/view", "_blank")
										}
									}}
								>
									{this.state.placementsInfo[partNumber][digit].placements && this.state.placementsInfo[partNumber][digit].placements.map(e => {
										if (arrDrillPlacement[e] && !(arrDrillPlacement[e][0] == null && arrDrillPlacement[e][1] == null)) {
											return e + " (" + arrDrillPlacement[e].join(":") + ")"
										}
										return e
									}).join("/")}
								</td>
								<td className='table-elem-sm clickable-element'
									onDoubleClick={() => {
										// redirect to /search/358466/view
										if (obj) {
											window.open("/search/" + obj.id + "/view", "_blank")
										}
									}}
								>{digit}</td>
								<td className='table-elem-sm' style={erreurDrill1 ? { backgroundColor: "rgb(255, 163, 163)" } : {}}>
									{this.state.drillEmpArr[pattern.trim().toUpperCase()] && this.state.drillEmpArr[pattern.trim().toUpperCase()].drill1}{this.state.placementsInfo[partNumber][digit].counterDrill1 > 0 ? " X " + this.state.placementsInfo[partNumber][digit].counterDrill1 : null}
								</td>
								<td className='table-elem-sm' style={erreurDrill2 ? { backgroundColor: "rgb(255, 163, 163)" } : {}}>
									{this.state.drillEmpArr[pattern.trim().toUpperCase()] && this.state.drillEmpArr[pattern.trim().toUpperCase()].drill2}{this.state.placementsInfo[partNumber][digit].counterDrill2 > 0 ? " X " + this.state.placementsInfo[partNumber][digit].counterDrill2 : null}
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
								>{(obj && obj.quantity) ? obj.quantity : this.state.ctcData[partNumber]?.find(elem => elem.pattern === pattern) ? <i style={{ color: "red" }}>DUPLICATED</i> : <i style={{ color: "red" }}>ERR CTC</i>}</td>
								<td className='table-elem-sm'>{(obj && obj.panelNumber) ? obj.panelNumber : this.state.ctcData[partNumber]?.find(elem => elem.pattern === pattern) ? <i style={{ color: "red" }}>DUPLICATED</i> : <i style={{ color: "red" }}>ERR CTC</i>}</td>
								<td className='table-elem-sm'
									style={(obj && obj.partNumberMaterial && this.state.placementsInfo[partNumber][digit].reftissu && obj.partNumberMaterial.trim().toUpperCase() !== this.state.placementsInfo[partNumber][digit].reftissu.join("/").trim().toUpperCase()) ? { backgroundColor: "rgb(255, 163, 163)" } : {}}
								>{(obj && obj.partNumberMaterial) ? obj.partNumberMaterial : this.state.ctcData[partNumber]?.find(elem => elem.pattern === pattern) ? <i style={{ color: "red" }}>DUPLICATED</i> : <i style={{ color: "red" }}>ERR CTC</i>}</td>
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

							arrNotFound.map((elemNf, ind) => {
								let digitObj = this.state.digits.find(elem => elem.pattern === elemNf.pattern)
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
										<td className='table-elem-sm'>{elemNf.quantity}</td>
										<td className='table-elem-sm'>{elemNf.panelNumber}</td>
										<td className='table-elem-sm'>{elemNf.partNumberMaterial}</td>
										<td className='table-elem-sm'>
											{((digitObj && digitObj.exist) ? <FontAwesomeIcon icon={faCheck} color="green" /> : <FontAwesomeIcon icon={faTimes} color="red" />)}
										</td>
									</tr>
								])
							})
						}

					}
				}
			})
		}
		return (this.state.loading || this.state.loadingTest) && <div>
			<h4 className='text-center my-2'>Vérification du plan de coupe</h4>
			<table className='table table-bordered'>
				<thead>
					<tr>
						<th className='table-elem-sm'>PN</th>
						<th className='table-elem-sm'>Material</th>
						<th className='table-elem-sm'>Placement</th>
						<th className='table-elem-sm'>Digit</th>
						<th className='table-elem-sm'>Drill 1</th>
						<th className='table-elem-sm'>Drill 2</th>
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
	}

	serieModal = () => {
		const { user } = this.props.security;

		return <Modal
			show={this.state.modalSerie != null}
			onHide={() => this.setState({ modalSerie: null })}
			dialogClassName="modal-90w"
			centered
		>
			{this.state.modalSerie && <div style={{ maxHeight: "80vh", overflowY: 'auto' }}>
				<h4 className='text-center my-2'>Modification du Serie {this.state.modalSerie.serie}</h4>
				<hr />
				<div className='container'>
					<div className='row'>


						<div className='col-6'>
							<div className='form-group'>
								<label>Part Number Material</label>
								<input type='text' className='form-control'
									value={this.state.modalSerie.partNumberMaterial}
									onChange={(e) => {
										let modalSerie = this.state.modalSerie;
										modalSerie.partNumberMaterial = e.target.value;
										this.setState({ modalSerie })
									}}
								/>
							</div>
						</div>

						<div className='col-6'>
							<div className='form-group'>
								<label>description</label>
								<input type='text' className='form-control'
									value={this.state.modalSerie.description}
									onChange={(e) => {
										let modalSerie = this.state.modalSerie;
										modalSerie.description = e.target.value;
										this.setState({ modalSerie })
									}}
								/>
							</div>
						</div>

						<div className='col-6'>
							<div className='form-group'>
								<label>Sens</label>
								<input type='text' className='form-control'
									value={this.state.modalSerie.matelassageEndroit}
									onChange={(e) => {
										let modalSerie = this.state.modalSerie;
										modalSerie.matelassageEndroit = e.target.value;
										this.setState({ modalSerie })
									}}
								/>
							</div>
						</div>

						<div className='col-6'>
							<div className='form-group'>
								<label>longueur</label>
								<input type='text' className='form-control'
									value={this.state.modalSerie.longueur}
									onChange={(e) => {
										let modalSerie = this.state.modalSerie;
										modalSerie.longueur = e.target.value;
										this.setState({ modalSerie })
									}}
								/>
							</div>
						</div>

						<div className='col-6'>
							<div className='form-group'>
								<label>nbrCouche</label>
								<input type='text' className='form-control'
									value={this.state.modalSerie.nbrCouche}
									onChange={(e) => {
										let modalSerie = this.state.modalSerie;
										modalSerie.nbrCouche = e.target.value;
										this.setState({ modalSerie })
									}}
								/>
							</div>
						</div>

						<div className='col-6'>
							<div className='form-group'>
								<label>laize</label>
								<input type='text' className='form-control'
									value={this.state.modalSerie.laize}
									onChange={(e) => {
										let modalSerie = this.state.modalSerie;
										modalSerie.laize = e.target.value;
										this.setState({ modalSerie })
									}}
								/>
							</div>
						</div>

						<div className='col-6'>
							<div className='form-group'>
								<label>Placement</label>
								<input type='text' className='form-control'
									value={this.state.modalSerie.placement}
									onChange={(e) => {
										let modalSerie = this.state.modalSerie;
										modalSerie.placement = e.target.value;
										this.setState({ modalSerie })
									}}
								/>
							</div>
						</div>


						<div className='col-6'>
							<div className='form-group'>
								<label>Config</label>
								<input type='text' className='form-control'
									value={this.state.modalSerie.config}
									onChange={(e) => {
										let modalSerie = this.state.modalSerie;
										modalSerie.config = e.target.value;
										this.setState({ modalSerie })
									}}
								/>
							</div>
						</div>

						<div className='col-6'>
							<div className='form-group'>
								<label>drill</label>
								<input type='text' className='form-control'
									value={this.state.modalSerie.drill}
									onChange={(e) => {
										let modalSerie = this.state.modalSerie;
										modalSerie.drill = e.target.value;
										this.setState({ modalSerie })
									}}
								/>
							</div>
						</div>

						<div className='col-6'>
							<div className='form-group'>
								<label>machine</label>
								<input type='text' className='form-control'
									value={this.state.modalSerie.machine}
									onChange={(e) => {
										let modalSerie = this.state.modalSerie;
										modalSerie.machine = e.target.value;
										this.setState({ modalSerie })
									}}
								/>
							</div>
						</div>




					</div>
				</div>
				<hr />
				<div className='d-flex justify-content-center mb-2'>
					<button className='btn btn-primary mx-2'
						disabled={this.state.changeLoading}
						onClick={() => {
							this.setState({ changeLoading: true })
							axios.post(`/api/cuttingRequestSerieInfo/modify`, this.state.modalSerie)
								.then(res => {
									axios.post("/api/cuttingRequestSerieRouleauHistory", {
										content: "Modification serie " + this.state.modalSerie.serie + " :"
											+ " partNumberMaterial " + this.state.modalSerie.partNumberMaterial
											+ " description " + this.state.modalSerie.description
											+ " matelassageEndroit " + this.state.modalSerie.matelassageEndroit
											+ " longueur " + this.state.modalSerie.longueur
											+ " nbrCouche " + this.state.modalSerie.nbrCouche
											+ " Config " + this.state.modalSerie.config
											+ " drill " + this.state.modalSerie.drill
											+ " machine " + this.state.modalSerie.machine
										,
										serie: this.state.serie,
										changedAt: moment(),
										changedBy: user.lastName + " " + user.firstName
									})
										.then(res => {
											this.setState({ modalSerie: null, changeLoading: false })
											this.loadSequence(this.state.sequence)
										})
										.catch(() => {
											this.setState({ changeLoading: false })
										})

								})
								.finally(() => {
									this.setState({ modalSerie: null, changeLoading: false })
								})
						}}>
						{<FontAwesomeIcon icon={faSpinner} spin />} Modifier
					</button>
				</div>
			</div>}
		</Modal>
	}

	renderPlanCoupe = (cuttingRequest, user) => {
		let arrTable = [], reftissu = null, desc = null;
		if (cuttingRequest.cuttingRequestSeries && cuttingRequest.cuttingRequestSeries.length > 0) {
			cuttingRequest.cuttingRequestSeries.sort((a, b) => a.partNumberMaterial.localeCompare(b.partNumberMaterial)).map((elem, index) => {
				if (reftissu != null && elem.partNumberMaterial != reftissu) {
					arrTable.push(<tr>
						<td style={{ borderLeft: "0", borderBottom: "0" }}>Group By</td>
						<td colSpan={2} style={{ fontWeight: 'bold' }}>{reftissu}</td>
						<td colSpan={4}>{desc}</td>
						<td colSpan={3}>{cuttingRequest.cuttingRequestSeries.filter(e => e.partNumberMaterial === reftissu).map(e => (e.longueur ?? 0) * (e.nbrCouche ?? 0)).reduce((a, b) => a + b).toFixed(2)}</td>
					</tr>)
					arrTable.push(<br />)
				}
				let arrDrill = (elem.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
				arrTable.push(<tr>
					<td >{elem.serie}</td>
					<td>{elem.matelassageEndroit}</td>
					<td>{elem.longueur && elem.longueur.toFixed(3)}</td>
					<td>{elem.nbrCouche}</td>
					<td>{elem.laize}</td>
					<td className='ml-1' style={{ whiteSpace: "nowrap" }}>{elem.placement}
						<button className='btn btn-outline-primary'
							style={{
								fontSize: 12,
								padding: 2,
								marginLeft: 3,
								width: 28,
							}}
							onClick={() => {
								// open a new page /cutfileviewer/1DEM365/view
								window.open("/cutfileviewer/" + elem.placement + "/view", "_blank")
							}}
						>
							<FontAwesomeIcon icon={faEye} />
						</button>
					</td>
					<td>{elem.config}</td>
					<td>{arrDrill[0]}</td>
					<td>{arrDrill[1]}</td>
					<td>{elem.machine}</td>
				</tr>)
				reftissu = elem.partNumberMaterial
				desc = elem.description
			})

			if (reftissu != null) {
				arrTable.push(<tr>
					<td style={{ borderLeft: "0", borderBottom: "0" }}>Group By</td>
					<td colSpan={2} style={{ fontWeight: 'bold' }}>{reftissu}</td>
					<td colSpan={4}>{desc}</td>
					<td colSpan={3}>{cuttingRequest.cuttingRequestSeries.filter(e => e.partNumberMaterial === reftissu).map(e => (e.longueur ?? 0) * (e.nbrCouche ?? 0)).reduce((a, b) => a + b)}</td>
				</tr>)
			}
		}
		return <div className='' ref={elem => this.planPrintPage = elem} style={{ padding: 15 }}>
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
					<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Cutting Plan Id : </strong></div>
					<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.cuttingPlanId}</div>
					<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>CMS ID : </strong></div>
					<div className='' style={{ width: "35%" }}>{cuttingRequest.cmsId}</div>
				</div>
				<div className='d-flex'>
					<label className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Zone : </strong></label>
					<div className='' style={{ width: "35%" }}>{cuttingRequest.zone?.nom}</div>
				</div>

			</div>
			<div className='mb-2'>
				<table className='table m-0 table table-grey-border entity-table-sm print-background'>
					<thead>
						<tr>
							<th className=''>Part Number</th>
							<th className=''>Description</th>
							<th className=''>Kit textil</th>
							<th className=''>Quantité</th>
							<th className=''>wo</th>
							<th className=''>woid</th>
							<th className=''>packageQty</th>

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
							<td className=''>{elemPn.partNumber}</td>
							<td className=''>{elemPn.description}</td>
							<td className=''>{elemPn.item}</td>
							<td className=''>{elemPn.quantity}</td>
							<td className=''>{elemPn.wo}</td>
							<td className=''>{elemPn.woid}</td>
							<td className=''><strong>{elemPn.packageQty}</strong></td>

						</tr>)}
					</tbody>
				</table>
			</div>
			<div>
				<table className='table m-0 table table-grey-border entity-table-sm'>
					<thead>
						<tr style={{ backgroundColor: "black" }}>
							<th style={{ fontWeight: "bold", fontSize: 20 }} className='' colSpan={5}>Matelassage</th>
							<th style={{ fontWeight: "bold", fontSize: 20 }} className=' ml-1' colSpan={5} >Coupe</th>
						</tr>
						<tr>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Serie</th>
							{/* <th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Part Number Material</th>
				<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Description</th> */}
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Sens</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Longueur</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Nbr Couche</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Laize</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=' ml-1'>Placement</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Config</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Drill1</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Drill2</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Machine</th>
						</tr>
					</thead>
					<tbody>
						{/* {cuttingRequest.cuttingRequestSeries && cuttingRequest.cuttingRequestSeries.map(elemPn => {
				let arrDrill = (elemPn.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
				return <tr style={elemPn.activated === false ? { backgroundColor: "#ffffc0" } : { backgroundColor: "" }}>
					<td className=''>{elemPn.serie}</td>
					<td className=''>{elemPn.partNumberMaterial}</td>
					<td className=''>{elemPn.description}</td>
					<td className=''>{elemPn.matelassageEndroit}</td>
					<td className=''>{elemPn.longueur && elemPn.longueur.toFixed(3)}</td>
					<td className=''>{elemPn.nbrCouche}</td>
					<td className=''>{elemPn.laize}</td>
					<td className=' ml-1' style={{ whiteSpace: "nowrap" }}>{elemPn.placement}</td>
					<td className=''>{elemPn.config}</td>
					<td className=''>{arrDrill[0]}</td>
					<td className=''>{arrDrill[1]}</td>
					<td className=''>{elemPn.machine}</td>
				</tr>
			})} */}
						{arrTable}
					</tbody>
				</table>
			</div>
			{user && <div className='float-right'>
				{user.lastName} {user.firstName} le {moment().format("DD/MM/YYYY HH:mm")}
			</div>}
		</div>
	}


	chargeQns = (obj) => {
		if (!obj || obj.cuttingRequestSeries.length === 0) return
		let arrReftissu = obj.cuttingRequestSeries.map((elem) => elem.partNumberMaterial.toUpperCase())
		axios.get(`/api/qn/reftissu?reftissus=${arrReftissu.join(",")}`)
			.then((res) => {
				this.setState({ qnList: res.data })
			})


	}


	returnColorStatus = (status) => {
		switch (status) {
			case "Waiting":
				return "#ffafaf"
			case "In progress":
				return "#f6ff6b"
			case "Complete":
				return "#7bff6b"
			case "Incomplete":
				return "#ffc46b"
			default:
				return ""
		}
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

	getMarge = (longueur, nbrCouche, partNumberMaterial) => {
		let marge = null
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



	loadPlacement = async (placement, isFirst) => {
		this.setState({ loadingPlacement: true })
		let { modalSerieForm, margeInitial, partNumbersInitial, targetQuantityPN } = this.state;

		let pnObj = {}, error = []
		let totalQuantite = 0

		this.state.cuttingRequest.cuttingRequestPartNumbers.map(pn => {
			pnObj[pn.partNumber] = pn.quantity
			totalQuantite += pn.quantity
		})

		let res = await axios.get(`/api/placementData/rapport?placements=${placement}&partNumbers=${this.state.cuttingRequest.cuttingRequestBoxs.map(e => e.partNumber)}`)
		if (res.data.length == 0) {
			error.push(placement + " n'est pas trouvé dans le dossier cutfile")
			this.setState({ loadingPlacement: false, error })
			return;
		}
		let elem = res.data[0]

		// if (elem.numeroDefPlct != modalSerieForm.partNumberMaterial) {
		// 	error.push("Part Number Material : " + elem.numeroDefPlct + " =/= " + modalSerieForm.partNumberMaterial);
		// }

		if (totalQuantite >= 20  && modalSerieForm.longueur > 0.3 && modalSerieForm.nbrCouche >= 5) {
			let configObj = this.state.partNumberMaterialConfigs[modalSerieForm.partNumberMaterial]
			if (configObj.validated0BF === true && elem.contraintes && elem.contraintes.trim().toUpperCase() !== 'ESP00' && modalSerieForm.machine === "Lectra") {
				error.push(modalSerieForm.placement + " :  l'espace relarge doit être ESP00")
			}
		}

		// if (elem.contraintes && elem.contraintes.trim().toUpperCase() !== "ESP00" && (modalSerieForm.placement.toUpperCase().includes("-IP6") || modalSerieForm.placement.toUpperCase().includes("-0BF"))) {
		// 	error.push("Le placement " + modalSerieForm.placement + " doit être avec l'espace relarge ESP00")
		// }
		if (modalSerieForm.machine === "Lectra IP6" && !modalSerieForm.placement.toUpperCase().includes("-IP6") && !modalSerieForm.placement.toUpperCase().includes("-0BF")) {
			error.push(modalSerieForm.placement + " :  le nom du placement doit contenir IP6 ou 0BF")
		}
		if ((modalSerieForm.machine === "LASER-DXF" && !modalSerieForm.placement.toUpperCase().includes("-DXF")) || (modalSerieForm.placement.toUpperCase().includes("-DXF") && modalSerieForm.machine !== "LASER-DXF")) {
			error.push(modalSerieForm.placement + " : le nom du placement ou type de machine incorrect")
		}
		if ((modalSerieForm.machine === "LASER-LSR" && !modalSerieForm.placement.toUpperCase().includes("-LSR")) || (modalSerieForm.placement.toUpperCase().includes("-LSR") && modalSerieForm.machine !== "LASER-LSR")) {
			error.push(modalSerieForm.placement + " : le nom du placement ou type de machine incorrect")
		}
		if (modalSerieForm.placement.toUpperCase().includes("-IP6") && modalSerieForm.machine !== "Lectra IP6") {
			error.push(modalSerieForm.placement + " : le nom du placement ou type de machine incorrect")
		}


		let qteList = elem.placeTailleQt.replace(")", "").split("|").map(qteElem => qteElem.split("("))
		let partNumbers = []
		qteList.map((qte, qteInd) => {
			partNumbers.push([elem.modeles.split(", ")[qteInd], parseInt(qte[0]) * parseInt(qte[1])])
		})
		let maxNbrCouche = 0
		if (isFirst) {
			targetQuantityPN = {}
			partNumbers.map(e => {
				targetQuantityPN[e[0]] = e[1] * modalSerieForm.nbrCouche
			})
			maxNbrCouche = modalSerieForm.nbrCouche
		} else {
			partNumbers.map(pn => {
				let couchePn = Math.ceil(targetQuantityPN[pn[0]] / parseInt(pn[1]))
				if (couchePn > maxNbrCouche) {
					maxNbrCouche = couchePn;
				}
			})
		}

		// partNumbers.map(pn => {
		// 	let couchePn = Math.ceil(pnObj[pn[0]] / parseInt(pn[1]))
		// 	console.log(pn[0] + " " + pn[1] + " : " + couchePn)
		// 	if (couchePn > maxNbrCouche) {
		// 		maxNbrCouche = couchePn;
		// 	}
		// })
		modalSerieForm.nbrCoucheTotal = maxNbrCouche
		let arrDrill = (modalSerieForm.drill || ",").split(",").map(e => (e != "") ? parseInt(e) : null)
		
		let machineConfig = this.state.partNumberMaterialConfigs[modalSerieForm.partNumberMaterial].reftissuMachines.find(e => e.machineType == modalSerieForm.machine)
		if(machineConfig == null) {
			machineConfig = this.state.reftissuMachineOld
		}
		modalSerieForm.maxPlie = machineConfig ? 
			((arrDrill[0] == null || arrDrill[0] === 0) && (arrDrill[1] == null || arrDrill[1] === 0) ? machineConfig.maxPlie : machineConfig.maxPlieDrill) 
			: 100; // Default value if machineConfig is null
		modalSerieForm.nbrCouche = Math.min(modalSerieForm.maxPlie, maxNbrCouche)
		let config = null;
		if (machineConfig != null) {
			let configArr = machineConfig.pliesConfig.split("|").map(e => { return [parseInt(e.split(";")[0]), e.split(";")[1]] }).sort((a, b) => a[0] - b[0])
			for (let i = 0; i < configArr.length; i++) {
				if (configArr[i][0] <= modalSerieForm.nbrCouche) {
					config = configArr[i][1]
				}
			}
		}


		modalSerieForm.config = config
		modalSerieForm.partNumbers = partNumbers.map(pn => pn[0] + " : " + pn[1]).join(" / ")
		modalSerieForm.espaceRelarge = elem.contraintes
		// if (elem.contraintes && elem.contraintes.trim().toUpperCase() !== "ESP00" && (elem.nomDefPlct.toUpperCase().includes("-IP6") || elem.nomDefPlct.toUpperCase().includes("-0BF"))) {
		// 	error.push("Le placement " + elem.nomDefPlct + " doit être avec l'espace relarge ESP00")
		// }

		if (modalSerieForm.machine === "Lectra IP6") {
			let arrConditionBuffer = []
			if (this.state.partNumberMaterialConfigs[modalSerieForm.partNumberMaterial] != null &&
				(this.state.partNumberMaterialConfigs[modalSerieForm.partNumberMaterial].buffer1IP6
					|| this.state.partNumberMaterialConfigs[modalSerieForm.partNumberMaterial].buffer2IP6)) {
				if (this.state.partNumberMaterialConfigs[modalSerieForm.partNumberMaterial].buffer1IP6 && this.state.partNumberMaterialConfigs[modalSerieForm.partNumberMaterial].buffer1IP6.length > 0) {
					arrConditionBuffer.push(this.state.partNumberMaterialConfigs[modalSerieForm.partNumberMaterial].buffer1IP6)
				}
				if (this.state.partNumberMaterialConfigs[modalSerieForm.partNumberMaterial].buffer2IP6 && this.state.partNumberMaterialConfigs[modalSerieForm.partNumberMaterial].buffer2IP6.length > 0) {
					arrConditionBuffer.push(this.state.partNumberMaterialConfigs[modalSerieForm.partNumberMaterial].buffer2IP6)
				}
			}
			if (arrConditionBuffer.length === 0) {
				arrConditionBuffer.push("ESP00")
			}
			if (!arrConditionBuffer.includes(modalSerieForm.espaceRelarge)) {
				error.push(modalSerieForm.placement + " : le placement doit être avec l'espace relarge " + arrConditionBuffer.join(", "))
			}
		}

		if (elem.drill1 === true) {
			if (arrDrill[0] == null || arrDrill[0] === 0) {
				error.push("R1 drill : le placement " + elem.nomDefPlct + " a besoin de 1er drill")
			}
		}
		if (elem.drill1 === false) {
			if (arrDrill[0] != null && arrDrill[0] !== 0) {
				error.push("R1 drill : le placement " + elem.nomDefPlct + " n'a pas besoin de 1er drill (" + arrDrill[0] + ")")
				arrDrill[0] = "0"
				modalSerieForm.drill = arrDrill.join(",")
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
				arrDrill[1] = "0"
				modalSerieForm.drill = arrDrill.join(",")
			}
		}
		let longueurPlacement = this.convertFloat((elem.longueur.replace("M", "")), 3);
		let largeurPlacement = this.convertFloat((elem.largeur.replace("M", "")), 3);
		let marge = this.getMarge(longueurPlacement, this.state.modalSerieForm.nbrCouche, this.state.modalSerieForm.partNumberMaterial)
		modalSerieForm.longueurPlacement = longueurPlacement
		modalSerieForm.marge = marge
		modalSerieForm.largeurPlacement = this.convertFloat(largeurPlacement / 100, 3)
		modalSerieForm.longueur = this.convertFloat(longueurPlacement + marge, 3)

		//	margeInitial: null,
		//	partNumbers: null
		if (isFirst) {
			modalSerieForm.laizeMarge = this.convertFloat(this.state.modalSerieForm.laize - modalSerieForm.largeurPlacement, 3)
		} else {
			let allowedMarge = [0.01, 0.03]
			let { margeLaizeMin, margeLaizeMax } = this.state.partNumberMaterialConfigs[modalSerieForm.partNumberMaterial]
			if (margeLaizeMin
				&& this.convertFloat(modalSerieForm.largeurPlacement + (margeLaizeMin * 0.01), 3) === this.state.modalObj.laize) {
				modalSerieForm.laizeMarge = (margeLaizeMin * 0.01)
				modalSerieForm.laize = this.convertFloat(modalSerieForm.largeurPlacement + (margeLaizeMin * 0.01), 3)
			} else if (margeLaizeMax
				&& this.convertFloat(modalSerieForm.largeurPlacement + (margeLaizeMax * 0.01), 3) === this.state.modalObj.laize) {
				modalSerieForm.laizeMarge = (margeLaizeMax * 0.01)
				modalSerieForm.laize = this.convertFloat(modalSerieForm.largeurPlacement + (margeLaizeMax * 0.01), 3)
			} else if (this.convertFloat(modalSerieForm.largeurPlacement + 0.01, 3) === this.state.modalObj.laize) {
				modalSerieForm.laizeMarge = 0.01
				modalSerieForm.laize = this.convertFloat(modalSerieForm.largeurPlacement + 0.01, 3)
			} else if (this.convertFloat(modalSerieForm.largeurPlacement + 0.013, 3) === this.state.modalObj.laize) {
				modalSerieForm.laizeMarge = 0.013
				modalSerieForm.laize = this.convertFloat(modalSerieForm.largeurPlacement + 0.013, 3)
			} if (this.convertFloat(modalSerieForm.largeurPlacement + 0.01, 3) === modalSerieForm.laize) {
				modalSerieForm.laizeMarge = 0.01
				modalSerieForm.laize = this.convertFloat(modalSerieForm.largeurPlacement + 0.01, 3)
			} else if (this.convertFloat(modalSerieForm.largeurPlacement + 0.013, 3) === modalSerieForm.laize) {
				modalSerieForm.laizeMarge = 0.013
				modalSerieForm.laize = this.convertFloat(modalSerieForm.largeurPlacement + 0.013, 3)
			} else {
				modalSerieForm.laizeMarge = margeInitial
				modalSerieForm.laize = this.convertFloat(modalSerieForm.largeurPlacement + margeInitial, 3)
			}
			if (partNumbersInitial !== partNumbers.map(e => e[0]).sort().join(" / ")) {
				error.push("partNumbers ne contient pas les même partnumber que l'original")
			}
		}

		if (!isFirst && this.state.modalObj.laize && this.state.modalObj.laize != modalSerieForm.laize) {
			error.push("La laize demandée du placement doit être " + this.state.modalObj.laize)
		}

		if (!isFirst && this.state.modalObj.machine && this.state.modalObj.machine.name && this.state.modalObj.machine.name != modalSerieForm.machine) {
			error.push("La machine demandée du placement doit être " + this.state.modalObj.machine.name)
		}


		// if(error && Object.keys(error).length > 0) {
		// 	this.setState({error})
		// }

		if (isFirst) {
			this.setState({ 
				targetQuantityPN, loadingPlacement: false, error, modalSerieForm, margeInitial: modalSerieForm.laizeMarge, 
				partNumbersInitial: partNumbers.map(e => e[0]).sort().join(" / "),
				reftissuMachineOld : this.state.reftissuMachineOld || machineConfig
			})
		} else {
			this.setState({ targetQuantityPN, loadingPlacement: false, error, modalSerieForm, allowSaving: error.length == 0,
				reftissuMachineOld : this.state.reftissuMachineOld || machineConfig
			})
		}

	}

	renderErrorsAlert(errors) {
		let arr = [];
		if (typeof errors === 'string') {
			arr.push(<li>{errors}</li>)
		} else {
			for (let prop in errors) {
				if (typeof errors[prop] === 'string') {
					arr.push(<li>{prop}: {errors[prop]}</li>)
				} else if (!isNaN(prop)) {
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


	convertFloat = (float, digit) => {
		//check if the float is a number
		if (!float || isNaN(float)) {
			return float
		}
		return parseFloat(parseFloat(float).toFixed(digit))
	}



	render() {
		const urlParams = new URLSearchParams(window.location.search);
		const id = urlParams.get('id');
		let arrDrill = ["", ""]
		if (this.state.modalSerieForm) {
			arrDrill = (this.state.modalSerieForm.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
		}
		const { user } = this.props.security;

		return (
			<div>
				<h2 className='text-center my-3'>Demande Changement Serie {id}</h2>
				{this.state.modalObj && <div className='mx-3'>
					<div className=''>
						<div className='row py-2'>
							<label className='col-2 text-right'>
								Serie:
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.serie}
								</span>
							</div>
							<label className='col-2 text-right'>
								Sequence:
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.sequence}
								</span>
							</div>
							<label className='col-2 text-right'>
								Placement:
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.placement}
								</span>
							</div>
							<label className='col-2 text-right'>
								Part Number Material:
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.partNumberMaterial} {this.state.partNumberMaterialConfigs[this.state.modalObj.partNumberMaterial] && <button
										className='btn btn-outline-primary px-1 ml-1'
										style={{
											fontSize: 10,
											padding: 2,
											borderRadius: "50%"
										}}
										onClick={() => {
											this.setState({ reftissuConfig: this.state.modalObj.partNumberMaterial })
										}}>
										<FontAwesomeIcon icon={faInfo} />
									</button>}
								</span>
							</div>
							<label className='col-2 text-right'>
								Part Numbers:
							</label>
							<div className='col-10 p-0'>
								<span>
									{this.state.modalObj.partNumbers}
								</span>
							</div>
							<label className='col-2 text-right'>
								{this.state.modalObj.typeDemande === "Erreur métrage" ? "Longueur NOK:" : "Laize:"}
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.laize} {this.state.modalObj.typeDemande !== "Erreur métrage" && this.state.dataIMS && `(La laize contractuelle : ${this.state.dataIMS.laize})`}
								</span>
							</div>
							<label className='col-2 text-right'>
								Machine:
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.machine && this.state.modalObj.machine.name}
								</span>
							</div>
							<label className='col-2 text-right'>
								Config demandée:
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.config || '-'}
								</span>
							</div>
							<label className='col-2 text-right'>
								Autre Changement:
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.autreChangement}
								</span>
							</div>
							<label className='col-2 text-right'>
								Description:
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.description}
								</span>
							</div>
							{/* add typeDemande and departement departementValidation reponseDepartement confirmeParDepartement dateConfirmationDepartement */}
							<label className='col-2 text-right'>
								Type Demande:
							</label>
							<div className='col-4 p-0'>

								<span>
									{this.state.modalObj.typeDemande}
								</span>
							</div>
							<label className='col-2 text-right'>
								Département Validation:
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.departementValidation}
								</span>
							</div>
							<label className='col-2 text-right'>
								Réponse Département:
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.reponseDepartement}
								</span>
							</div>
							<label className='col-2 text-right'>
								Confirmé par Département:
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.confirmeParDepartement} {this.state.modalObj.dateConfirmationDepartement}
								</span>
							</div>
							<label className='col-2 text-right'>
								Réponse CAD:
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.reponse}
								</span>
							</div>
							<label className='col-2 text-right'>
								Confirmé par CAD:
							</label>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.confirmePar} {this.state.modalObj.dateConfirmation}
								</span>
							</div>
						</div>
					</div>
					<hr />
					{this.state.modalSerieForm && <div className=''>
						<div className='row'>
							<div className='col-3'>
								<div className='form-group'>
									<label>Machine :</label>
									{/* <input type='text' className='form-control'
										value={this.state.modalSerieForm.machine}
										onChange={(e) => {
											let modalSerieForm = this.state.modalSerieForm;
											modalSerieForm.machine = e.target.value;
											this.setState({ modalSerieForm })
										}}
									/> */}
									<div style={{ fontSize: 18 }}>
										<Select
											classNamePrefix="rs"
											placeholder={"machine"}
											isClearable={false}
											value={this.state.modalSerieForm.machine
												? { label: this.state.modalSerieForm.machine, value: this.state.modalSerieForm.machine }
												: null
											}
											// options={this.state.partNumberMaterialConfigs[this.state.modalSerieForm.partNumberMaterial]
											// 	? this.state.partNumberMaterialConfigs[this.state.modalSerieForm.partNumberMaterial].reftissuMachines.map(e => { return { label: e.machineType, value: e.machineType } })
											// 	: []
											// }
											options={this.state.optionsList.machineType || []}
											onChange={(option) => {
												if (option) {
													this.setState({ modalSerieForm: { ...this.state.modalSerieForm, machine: option.value }, allowSaving: false })
													setTimeout(() => {
														this.loadPlacement(this.state.modalSerieForm.placement, false)
													}, 100)
												}
											}}
										/>
									</div>
								</div>
							</div>
							<div className='col-3'>
								<div className='form-group'>
									<label>Placement</label>
									<input type='text' className='form-control' disabled={this.state.loadingPlacement}
										value={this.state.modalSerieForm.placement}
										onChange={(e) => {
											let modalSerieForm = this.state.modalSerieForm;
											modalSerieForm.placement = e.target.value;
											this.setState({ modalSerieForm, allowSaving: false })
										}}
										onKeyUp={e => {
											if (e.key === 'Enter') {
												this.loadPlacement(this.state.modalSerieForm.placement, false)
											}
										}}
										onBlur={e => {
										}}
									/>
								</div>
							</div>
							<div className='col-3'>
								<div className='form-group'>
									<label>Drill 1:</label>
									<input type='text' className='form-control' style={{ border: "none" }}
										value={arrDrill[0]} disabled={true}
									// onChange={(e) => {
									// 	if (/^\d*$/.test(e.target.value) && e.target.value != "0") {
									// 		arrDrill[0] = e.target.value != "" ? parseInt(e.target.value) : null
									// 		this.setState({ modalSerieForm: { ...this.state.modalSerieForm, drill: arrDrill.join(",") } })
									// 	}
									// }}
									/>
								</div>
							</div>
							<div className='col-3'>
								<div className='form-group'>
									<label>Drill 2 :</label>
									<input type='text' className='form-control' style={{ border: "none" }}
										value={arrDrill[1]} disabled={true}
									// onChange={(e) => {
									// 	if (/^\d*$/.test(e.target.value) && e.target.value != "0") {
									// 		arrDrill[1] = e.target.value != "" ? parseInt(e.target.value) : null
									// 		this.setState({ modalSerieForm: { ...this.state.modalSerieForm, drill: arrDrill.join(",") } })
									// 	}
									// }}
									/>
								</div>
							</div>

							<div className='col-12'>
								<span className='mb-1'>Partnumbers : {this.state.modalSerieForm.partNumbers}</span><br />
								<span className='mb-1'>Nombre de couche total : {this.state.modalSerieForm.nbrCoucheTotal}</span><br />
								<span className='mb-1'>Max Plie : {this.state.modalSerieForm.maxPlie}</span>
							</div>

							<div className='col-3 mb-3'>
								Longueur : {this.state.modalSerieForm.longueurPlacement} + {this.state.modalSerieForm.marge} = {this.state.modalSerieForm.longueur}
							</div>
							<div className='col-3 mb-3'>
								Laize : {this.state.modalSerieForm.largeurPlacement} + {this.state.modalSerieForm.laizeMarge} = {this.state.modalSerieForm.laize}
							</div>
							<div className='col-3 mb-3'>
								Nombre de Couche : {this.state.modalSerieForm.nbrCouche}
							</div>
							<div className='col-3 mb-3'>
								Config : {this.state.modalSerieForm.config}
							</div>
							{/* <div className='col-6'>
								<div className='form-group'>
									<label>Config</label>
									<input type='text' className='form-control'
										value={this.state.modalSerieForm.config} style={{ border: "none" }}
									/>
								</div>
							</div> */}


							<hr />

							<div className='col-3'>
								<div className='form-group'>
									<label>Part Number Material</label>
									<input type='text' className='form-control' disabled={true}
										value={this.state.modalSerieForm.partNumberMaterial}
										style={{ border: "none" }}
									// onChange={(e) => {
									// 	let modalSerieForm = this.state.modalSerieForm;
									// 	modalSerieForm.partNumberMaterial = e.target.value;
									// 	this.setState({ modalSerieForm })
									// }}
									/>
								</div>
							</div>
							<div className='col-9'>
								<div className='form-group'>
									<label>description</label>
									<input type='text' className='form-control' disabled={true}
										value={this.state.modalSerieForm.description} style={{ border: "none" }}
									/>
								</div>
							</div>

							<div className='col-6'>
								<div className='form-group'>
									<label>Sens</label>
									<input type='text' className='form-control' disabled={true}
										value={this.state.modalSerieForm.matelassageEndroit} style={{ border: "none" }}
									/>
								</div>
							</div>
						</div>
					</div>}
					<hr />
					<div className='d-flex justify-content-center mb-2'>
							{user && user.roles && 
						(
							(user.roles.some(role => ["ROLE_PROCESS"].includes(role.authority)) && this.state.modalObj?.typeDemande === "Machine") ||
							(user.roles.some(role => ["ROLE_QUALITE"].includes(role.authority)) && this.state.modalObj?.typeDemande?.startsWith("Diviser Matelas")) ||
							(user.roles.some(role => ["ROLE_VARIANCE"].includes(role.authority)) && (this.state.modalObj?.typeDemande?.startsWith("QLaize") || this.state.modalObj?.typeDemande === "Erreur métrage"))
						) &&
							this.state.modalObj.reponseDepartement === "En attente" && 
							(() => {
								const selectedTypeOption = optionTypeDemandeChangementSerie.find(option => option.value === this.state.modalObj?.typeDemande);
								return selectedTypeOption && selectedTypeOption.causes ? (
									<div className='col-4'>
										<Select
											classNamePrefix="rs"
											placeholder="Sélectionnez la cause..."
											isClearable={true}
											value={this.state.modalObj.cause 
												? { label: this.state.modalObj.cause, value: this.state.modalObj.cause }
												: null
											}
											options={selectedTypeOption.causes.map(cause => ({ label: cause, value: cause }))}
											onChange={(option) => {
												this.setState({ 
													modalObj: { 
														...this.state.modalObj, 
														cause: option ? option.value : null
													} 
												})
											}}
										/>
									</div>
								) : null;
							})()}

						{user && user.roles && 
						(
							(user.roles.some(role => ["ROLE_PROCESS"].includes(role.authority)) && this.state.modalObj?.typeDemande === "Machine") ||
							(user.roles.some(role => ["ROLE_QUALITE"].includes(role.authority)) && this.state.modalObj?.typeDemande?.startsWith("Diviser Matelas")) ||
							(user.roles.some(role => ["ROLE_VARIANCE"].includes(role.authority)) && (this.state.modalObj?.typeDemande?.startsWith("QLaize") || this.state.modalObj?.typeDemande === "Erreur métrage"))
						) &&
							this.state.modalObj.reponseDepartement === "En attente" &&
							<button className='btn btn-sm btn-danger ml-2' style={{ width: "20%" }}
								disabled={this.state.loadingValidation}
								onClick={() => {
									this.setState({ loadingValidation: true })
									axios.post("/api/demandeChangementSerie/departementRefuser", this.state.modalObj)
										.then(res => {
											this.props.history.push(`/demandeChangementSerie`)
											this.setState({ loadingValidation: false })
										})
										.catch(err => {
											this.setState({ error: err.response.data, loadingValidation: false })
										})
								}}><FontAwesomeIcon icon={faTimes} /> Refuser</button>}
						{user && user.roles && 
						(
							(user.roles.some(role => ["ROLE_PROCESS"].includes(role.authority)) && this.state.modalObj?.typeDemande === "Machine") ||
							(user.roles.some(role => ["ROLE_QUALITE"].includes(role.authority)) && this.state.modalObj?.typeDemande?.startsWith("Diviser Matelas")) ||
							(user.roles.some(role => ["ROLE_VARIANCE"].includes(role.authority)) && (this.state.modalObj?.typeDemande?.startsWith("QLaize") || this.state.modalObj?.typeDemande === "Erreur métrage"))
						) &&
							this.state.modalObj.reponseDepartement === "En attente" &&
							<button className='btn btn-sm btn-success ml-2' style={{ width: "20%" }}
								disabled={this.state.loadingValidation}
								onClick={() => {
									this.setState({ loadingValidation: true })
									axios.post("/api/demandeChangementSerie/departementValider", {...this.state.modalObj, cause: this.state.modalObj.cause || ""})
										.then(res => {
											this.props.history.push(`/demandeChangementSerie`)
											this.setState({ loadingValidation: false })
										})
										.catch(err => {
											this.setState({ error: err.response.data, loadingValidation: false })
										})
								}}>
								<FontAwesomeIcon icon={faCheck} /> Valider
							</button>}

						{user && user.roles && (user.roles.some(role => ["ROLE_CAD"].includes(role.authority))) &&
							this.state.modalObj.reponse === "En attente" &&
							<button className='btn btn-sm btn-danger ml-2' style={{ width: "20%" }}
								disabled={this.state.changeLoading}
								onClick={() => {
									this.setState({ changeLoading: true })
									axios.post("/api/demandeChangementSerie/cadRefuser", {...this.state.modalObj, cause: this.state.modalObj.cause || ""})
										.then(res => {
											this.props.history.push(`/demandeChangementSerie`)
											this.setState({ changeLoading: false })
										})
										.catch(err => {
											this.setState({ error: err.response.data, changeLoading: false })
										})
								}}><FontAwesomeIcon icon={faTimes} /> Refuser par CAD</button>}
						{user && user.roles && (user.roles.some(role => ["ROLE_CAD"].includes(role.authority))) &&
							this.state.modalObj.reponse === "En attente" &&
							<button className='btn btn-success mx-2'
								disabled={this.state.changeLoading || !this.state.allowSaving} style={{ width: "20%" }}
								onClick={() => {
									this.setState({ changeLoading: true })
									let param = ""
									if (this.state.modalSerieForm.maxNbrCouche > this.state.modalSerieForm.maxPlie) {
										param = `?maxNbrCouche=${this.state.modalSerieForm.maxNbrCouche}&maxPlie=${this.state.modalSerieForm.maxPlie}`
									}
									axios.post(`/api/cuttingRequestSerieInfo/modify${param}`, this.state.modalSerieForm)
										.then(res => {
											axios.post("/api/demandeChangementSerie/cadValider", { ...this.state.modalObj, newPlacement: this.state.modalSerieForm.placement })
												.then(res => {
													// axios.post("/api/cuttingRequestSerieRouleauHistory", {
													// 	content: "Modification serie " + this.state.modalSerieForm.serie + " :"
													// 		+ " partNumberMaterial " + this.state.modalSerieForm.partNumberMaterial
													// 		+ " description " + this.state.modalSerieForm.description
													// 		+ " matelassageEndroit " + this.state.modalSerieForm.matelassageEndroit
													// 		+ " longueur " + this.state.modalSerieForm.longueur
													// 		+ " nbrCouche " + this.state.modalSerieForm.nbrCouche
													// 		+ " Config " + this.state.modalSerieForm.config
													// 		+ " drill " + this.state.modalSerieForm.drill
													// 		+ " machine " + this.state.modalSerieForm.machine
													// 	,
													// 	serie: this.state.modalSerieForm.serie,
													// 	changedAt: moment(),
													// 	changedBy: user.lastName + " " + user.firstName
													// })
													// .then(res => {
													this.props.history.push(`/demandeChangementSerie`)
													this.setState({ changeLoading: false })
													// })
													// .catch(() => {
													// 	this.props.history.push(`/demandeChangementSerie`)
													// 	this.setState({ changeLoading: false })
													// })
												})
												.catch(err => {
													this.setState({ error: err.response.data, changeLoading: false })
												})
										})
										.catch(err => {
											this.setState({ modalSerie: null, changeLoading: false, error: err.response.data })
										})
								}}>
								{this.state.changeLoading ? <FontAwesomeIcon icon={faSpinner} spin /> : !this.state.allowSaving ? <FontAwesomeIcon icon={faBan} /> : null} Traiter
							</button>}

					</div>
				</div>}
				<hr />
				{this.state.cuttingRequest && this.state.cuttingRequest.sequence && this.renderSequenceDetails(this.state.cuttingRequest)}
				{this.state.cuttingRequest && this.state.cuttingRequest.sequence && <div>{this.renderConfirmModal()}</div>}
				{this.serieModal()}
				{this.renderReftissuModal()}
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
		)
	}
}

DemandeChangementSerieValidation.propTypes = {
	security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
	security: state.security
})

export default connect(mapStateToProps, {})(DemandeChangementSerieValidation);

