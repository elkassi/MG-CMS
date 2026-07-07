import { faArrowLeft, faArrowRight, faArrowUp, faCheck, faEye, faPrint, faSpinner, faSync, faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import axios from 'axios';
import React, { Component } from 'react'
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import GammePn from './GammePn';
import ReactToPrint from "react-to-print";
import logo from '../../assets/images/lear_logo.png'
import { Button, ButtonGroup, Modal } from 'react-bootstrap'
import Select from "react-select";
import moment from 'moment';
import FormCoupe from './FormCoupe';

class VerificationQualite extends Component {

	constructor(props) {
		super(props);
		this.state = {
			sequence: '',
			cuttingRequest: null,
			selectedBoxs: [],
			drillEmpArr: {},
			modalRotate: false,
			qnList: [],
			showSerieForm: null,
			loadingRefresh: false,
		}
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

	serieModal = () => {
		return <Modal
			show={this.state.showSerieForm !== null}
			onHide={() => this.setState({ showSerieForm: null })}
			dialogClassName="modal-90w"
			centered
		>
			{this.state.showSerieForm && <div style={{ height: "90vh", overflowY: 'auto' }}>
				<FormCoupe serie={this.state.showSerieForm} />
			</div>}
		</Modal>
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
										this.setState({ showSerieForm: elemPn.serie })
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

	verification = () => {
		let placements = [], placementNbrCouche = {}, error = [], partNumbers = [], placementActivated = []
		if (this.state.cuttingRequest.sequence == null) return
		if (this.state.cuttingRequest.cuttingRequestPartNumbers) {
			this.state.cuttingRequest.cuttingRequestPartNumbers.map((cppn, ind) => {
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
		}
		let arrDrillPlacement = {}
		if (this.state.cuttingRequest.cuttingRequestSeries) {
			this.state.cuttingRequest.cuttingRequestSeries.map(cpmp => {
				let arrDrill = (cpmp.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
				if (cpmp.placement == null) {
					error.push("un placement dans " + cpmp.partNumberMaterial + " est vide")
				} else if (cpmp.nbrCouche == null || cpmp.nbrCouche == 0) {
					error.push("Nombre de couche du placement " + cpmp.placement + " est vide")
				} else if (cpmp.config == null) {
					error.push("Config du placement " + cpmp.placement + " est vide")
				} else if (cpmp.laize == null) {
					error.push("La laize du placement " + cpmp.placement + " est vide")
				} else if (cpmp.longueur == null) {
					error.push("Longueur du placement " + cpmp.placement + " est vide")
				} else {
					if (cpmp.activated === true && !placements.includes(cpmp.placement)) {
						placementActivated.push(cpmp.placement)
					}
					if (!placements.includes(cpmp.placement)) {
						placements.push(cpmp.placement)
					}
					if (placementNbrCouche[cpmp.placement]) {
						placementNbrCouche[cpmp.placement] += cpmp.nbrCouche
					} else {
						placementNbrCouche[cpmp.placement] = cpmp.nbrCouche
					}
				}

				arrDrillPlacement[cpmp.placement] = arrDrill
			})
		}
		if (error.length == 0) {

			let reftissuObj = {}
			if (this.state.cuttingRequest.cuttingRequestSeries) {
				this.state.cuttingRequest.cuttingRequestSeries.map(crs => {
					if (crs.placement != null && crs.placement != "" && crs.partNumberMaterial != null && crs.partNumberMaterial != "") {
						reftissuObj[crs.placement.toUpperCase().trim()] = crs.partNumberMaterial.toUpperCase().trim()
					}
				})
			}
			if (this.state.cuttingRequest.cuttingRequestPartNumbers) {
				this.state.cuttingRequest.cuttingRequestPartNumbers.map((cppn, ind) => {
					if (cppn.partNumber != null) {
						axios.get(`/api/ctcFiles/pn/${cppn.partNumber}`)
							.then(res => {
								this.setState({ ctcData: { ...this.state.ctcData, [cppn.partNumber]: res.data.filter(e => e.type.trim().toLowerCase() === "fabric") } })
							})
							.catch(err => {
								this.setState({ error: error })
							})
					}
				})
			}


			this.setState({ loadingTest: true, placementsInfo: null, digits: [] })
			axios.get(`/api/placementData/info-list?placements=${placementActivated.join(",")}&partNumbers=${partNumbers.join(",")}`)
				.then(res => {
					let resObj = {}
					let digits = []
					res.data.map(elem => {
						if (digits.includes(elem.digit) === false) {
							digits.push(elem.digit)
						}
						if (resObj[elem.partNumber] == null) {
							resObj[elem.partNumber] = { [elem.digit + " - " + elem.sens]: { quantity: placementNbrCouche[elem.placement], reftissu: [reftissuObj[elem.placement.toUpperCase().trim()]], placements: [elem.placement], counterDrill1: elem.counterDrill1, counterDrill2: elem.counterDrill2, graphNumber: elem.graphNumber } }
						} else {
							if (resObj[elem.partNumber][elem.digit + " - " + elem.sens] == null) {
								resObj[elem.partNumber][elem.digit + " - " + elem.sens] = { quantity: placementNbrCouche[elem.placement], reftissu: [reftissuObj[elem.placement.toUpperCase().trim()]], placements: [elem.placement], counterDrill1: elem.counterDrill1, counterDrill2: elem.counterDrill2, graphNumber: elem.graphNumber }
							} else {
								resObj[elem.partNumber][elem.digit + " - " + elem.sens].quantity += placementNbrCouche[elem.placement]
								if (!resObj[elem.partNumber][elem.digit + " - " + elem.sens].reftissu.includes(reftissuObj[elem.placement.toUpperCase().trim()])) {
									resObj[elem.partNumber][elem.digit + " - " + elem.sens].reftissu.push(reftissuObj[elem.placement.toUpperCase().trim()])
								}
								if (!resObj[elem.partNumber][elem.digit + " - " + elem.sens].placements.includes(elem.placement)) {
									resObj[elem.partNumber][elem.digit + " - " + elem.sens].placements.push(elem.placement)
								}
							}
						}
					})
					this.setState({ placementsInfo: resObj })
					axios.post(`/api/placementData/plt-check`, digits.map(e => {
						if (e.endsWith("-LSR")) {
							return e.slice(0, e.length - 4)
						}
						return e;
					}))
						.then(resDg => {
							this.setState({ digits: resDg.data })
						})
					axios.post(`/api/drillEmp/list`, digits)
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

	loadSequence = async (sequence) => {
		this.setState({
			cuttingRequest: null,
			selectedBoxs: [],
			drillEmpArr: {},
			modalRotate: false,
			qnList: [],
			showSerieForm: null,
			loadingRefresh: false,
		})


		let res = await axios.get(`/api/cuttingRequestData/${sequence}`)
		if (res.data && res.data.sequence) {
			this.setState({ cuttingRequest: res.data })
			let resSerie = await axios.get(`/api/cuttingRequestSerieData/bySequence/${sequence}`)
			if (resSerie.data) {
				this.setState({ cuttingRequest: { ...this.state.cuttingRequest, cuttingRequestSeries: resSerie.data } })
			}
			let resPartNumbers = await axios.get(`/api/cuttingRequestPartNumberData/bySequence/${sequence}`)
			if (resPartNumbers.data) {
				this.setState({ cuttingRequest: { ...this.state.cuttingRequest, cuttingRequestPartNumbers: resPartNumbers.data } })
			}
			let resBoxs = await axios.get(`/api/cuttingRequestBoxData/bySequence/${sequence}`)
			if (resBoxs.data) {
				this.setState({ cuttingRequest: { ...this.state.cuttingRequest, cuttingRequestBoxs: resBoxs.data } })
			}
			this.verification()
			this.chargeQns(this.state.cuttingRequest)
		} else {
			this.setState({ error: "Sequence not found" })
		}
	}


	render() {
		return (
			<div>
				<h1 className='text-center' style={{ marginTop: 10, marginBottom: 20 }}>Vérification Qualité</h1>
				<div style={{ display: "flex", justifyContent: "center", alignItems: "center" }} >
					<input
						className='form-control' style={{ width: 150 }}
						type='text'
						placeholder='Séquence...'
						value={this.state.sequence}
						onChange={e => this.setState({ sequence: e.target.value })}
						onKeyUp={e => {
							if (e.key === 'Enter') {
								this.loadSequence(this.state.sequence)
							}
						}}
					/>
					<input
						className='form-control ml-2' style={{ width: 150 }}
						type='text'
						placeholder='Serie...'
						value={this.state.serie}
						onChange={e => this.setState({ serie: e.target.value })}
						onKeyUp={e => {
							if (e.key === 'Enter') {
								if (this.state.serie && this.state.serie.trim().length > 0) {
									axios.get("/api/cuttingRequestSerieData/" + this.state.serie)
										.then(respondSerie => {
											this.setState({ sequence: respondSerie.data.sequence })
											this.loadSequence(respondSerie.data.sequence)
										})
								}
							}
						}}
					/>
					<input
						type="text"
						className="form-control ml-2" style={{ width: 150 }}
						placeholder="WO..."
						value={this.state.wo}
						onChange={(e) => this.setState({ wo: e.target.value })}
						onKeyPress={(e) => {
							if (e.key === 'Enter') {
								axios.get(`/api/cuttingRequestBoxData/all?equal.wo=${this.state.wo}&page=0&size=1&sort=id,desc`)
									.then(respondBox => {
										this.setState({ sequence: respondBox.data.content[0].sequence })
										this.loadSequence(respondBox.data.content[0].sequence)
									})
									.catch(err => {
										console.log(err)
									})
							}
						}}
					/>
					<input
						type="text"
						className="form-control ml-2" style={{ width: 150 }}
						placeholder="ID Box..."
						value={this.state.idBox}
						onChange={(e) => this.setState({ idBox: e.target.value })}
						onKeyPress={(e) => {
							if (e.key === 'Enter') {
								axios.get(`/api/cuttingRequestBoxData/all?equal.id=${this.state.idBox}&page=0&size=1&sort=id,desc`)
									.then(respondBox => {
										this.setState({ sequence: respondBox.data.content[0].sequence })
										this.loadSequence(respondBox.data.content[0].sequence)
									})
									.catch(err => {
										console.log(err)
									})
							}
						}}
					/>

					{<button
						className='btn btn-sm btn-primary ml-2' disabled={!(this.state.sequence && this.state.sequence.trim().length > 0)}
						onClick={() => {
							this.setState({ loadingRefresh: true })
							axios.post(`/api/cuttingRequestV2/refresh/${this.state.sequence}`).then(res => {
								this.setState({ cuttingRequest: res.data, loadingRefresh: false })
							})
								.catch(err => {
									this.setState({ error: err.response.data, loadingRefresh: false })
								})
						}}
					>
						{this.state.loadingRefresh
							? <span><FontAwesomeIcon icon={faSpinner} spin /> Loading ...</span>
							: <span><FontAwesomeIcon icon={faSync} /> Refresh from CMS</span>
						}

					</button>}
				</div>
				{this.state.cuttingRequest && this.state.cuttingRequest.sequence && this.renderSequenceDetails(this.state.cuttingRequest)}
				{this.state.cuttingRequest && this.state.cuttingRequest.sequence && <div>{this.renderConfirmModal()}</div>}
				{this.serieModal()}
			</div>
		)
	}

}

VerificationQualite.propTypes = {
	security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
	security: state.security
})

export default connect(mapStateToProps, {})(VerificationQualite);