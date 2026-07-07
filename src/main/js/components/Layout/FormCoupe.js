import { faArrowAltCircleLeft, faFloppyDisk } from '@fortawesome/free-regular-svg-icons'
import { faMinus, faPlus, faArrowRightFromBracket, faTimes, faPrint, faCheck, faTriangleExclamation, faRotateLeft, faCommentsDollar, faAngleUp, faAngleDoubleDown, faAngleDown, faArrowsRotate, faArrowRotateRight } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import axios from 'axios'
import { Button, ButtonGroup, Modal } from 'react-bootstrap'
import moment, { min } from 'moment'
import React, { Component } from 'react'
import logo from '../../assets/images/lear_logo.png'
import "../../styles/Form.scss"
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { departementOption, lieuDetectionOptions, problemeResoluOption } from '../../metadata'
import Select from "react-select";
import Switch from "react-switch";

export default class FormCoupe extends Component {

	constructor() {
		super()
		this.state = {
			obj: {},
			postObj: {
				coupeur1: localStorage.getItem("matricule1") || undefined,
				// coupeur2: localStorage.getItem("matricule2") || undefined,
			},
			error: null,
			qn: null,
			showQn: false,
			intervention: null,
			interventionList: [],
			mode: null,
			showLog: false,
			codeErreurList: [],
			codeArretList: [],
			codeDefautList: [],
			codeScrapList: [],

			// overlapTotal: 1
		}
	}

	componentDidMount() {
		// axios.get("/api/productionTable/pc-mine")
		// 	.then(res => {
		// 		let mode = ""
		// 		if (res.data.pcMine.trim().toUpperCase() === res.data.pcMatelassage.trim().toUpperCase()) {
		// 			mode = "matelassage"
		// 		}
		// 		if (res.data.pcMine.trim().toUpperCase() === res.data.pcCoupe.trim().toUpperCase()) {
		// 			mode = "coupe"
		// 		}
		// 		this.setState({ mode, postObj: { ...this.state.postObj, zoneCoupe: res.data.zone.nom, tableCoupe: res.data.nom, machineType: res.data.machineType } })
		// 		this.serieInput.focus()
		// 	})
		// 	.catch(err => {
		// 		this.setState({ error: err.response.data })
		// 	})

		axios.get("/api/codeErreur/list")
			.then(res => {
				this.setState({ codeErreurList: res.data })
			})
		axios.get("/api/codeArret/list")
			.then(res => {
				this.setState({ codeArretList: res.data })
			})
		axios.get("/api/codeScrap/list")
			.then(res => {
				this.setState({ codeScrapList: res.data })
			})
		axios.get("/api/codeDefaut/list")
			.then(res => {
				this.setState({ codeDefautList: res.data })
			})

		if (this.props.serie) {
			this.setState({ obj: { serie: this.props.serie }, qn: null, showQn: false, rouleauObj: {} })
			axios.get(`/api/cuttingRequestSerieInfo/${this.props.serie}`)
				.then((res) => {
					this.setState({ obj: { ...this.state.obj, ...res.data } })
					this.searchInterventions(res.data.serie)
					axios.get(`/api/optitime/matricule?matricule=${res.data.matelasseur1}`)
					.then(res => {
						this.setState({ matelasseur1: res.data })
					})
					axios.get(`/api/optitime/matricule?matricule=${res.data.matelasseur2}`)
					.then(res => {
						this.setState({ matelasseur2: res.data })
					})
					axios.get(`/api/optitime/matricule?matricule=${res.data.coupeur1}`)
					.then(res => {
						this.setState({ coupeur1: res.data })
					})
						
					axios.get(`/api/qn/reftissu?reftissus=${this.state.obj.partNumberMaterial}`)
						.then(res2 => {
							let arrQn = res2.data.filter(e => e.resultat === "Non ok" && e.placement && (e.placement.toUpperCase().trim() === "ALL" || e.placement.split(";").map(e => e.trim().toUpperCase()).includes(this.state.obj.placement.trim().toUpperCase())))
							if (arrQn.length > 0) {
								this.setState({ qn: arrQn[0], showQn: true })
							} else {
								arrQn = res2.data.filter(e => e.resultat === "Formation" && e.placement && (e.placement.toUpperCase().trim() === "ALL" || e.placement.split(";").map(e => e.trim().toUpperCase()).includes(this.state.obj.placement.trim().toUpperCase())))
								if (arrQn.length > 0) {
									this.setState({ qn: arrQn[0], showQn: true })
								}
							}
						})
				})
				.catch(err => {
					this.setState({ error: err.response.data, obj: {} })
				})
		}
	}

	componentDidUpdate(prevProps, prevState) {
		// if (this.state.obj && this.state.obj.autoCoupe === true && !prevState.obj.autoCoupe) {
		// 	this.startLoop();
		// } else if (!this.state.obj.autoCoupe && prevState.obj.autoCoupe) {
		// 	this.stopLoop();
		// }
	}

	searchInterventions(serie, intervention) {
		axios.get(`/api/intervention/serie/${serie}`)
			.then(res => {
				if (intervention != undefined && intervention && intervention.id) {
					this.setState({ interventionList: res.data, intervention: res.data.find(elem => elem.id == intervention.id) })
				} else {
					this.setState({ interventionList: res.data })
				}
			})
			.catch(err => {
				this.setState({ error: err.response.data })
			})
	}

	renderErrorsAlert(errors) {
		let arr = [];
		if (typeof errors === 'string') {
			arr.push(<li>{errors}</li>)
		} else {
			for (let prop in errors) {
				if (typeof errors[prop] === 'string') {
					if (!isNaN(prop)) {
						arr.push(<li key={prop}>{parseInt(prop) + 1}: {errors[prop]}</li>)
					} else {
						arr.push(<li key={prop}>{prop}: {errors[prop]}</li>)
					}
				} else if (typeof errors[prop] === "object") {
					console.log(typeof prop)
					if (!isNaN(prop)) {
						if (Object.keys(errors[prop]).length > 0) {
							arr.push(<li key={prop}>{parseInt(prop) + 1}: <ul>{this.renderErrorsAlert(errors[prop])}</ul></li>)
						}
					} else {
						if (Object.keys(errors[prop]).length > 0) {
							arr.push(<li key={prop}>{prop}: <ul>{this.renderErrorsAlert(errors[prop])}</ul></li>)
						}
					}
				}
			}
		}

		return arr
	}

	renderModalQn = () => {
		return <Modal
			show={this.state.showQn}
			onHide={() => this.setState({ showQn: false })}
			// className=""
			dialogClassName="modal-95vw"
			backdropStyle={{ backgroundColor: "rgba(255,255,255,1)" }}
			style={{ width: "100%" }}
			centered
		>
			{this.state.qn && <div style={{
				maxHeight: "95vh",
				overflow: "auto"
			}}>
				<div style={{ display: "flex", justifyContent: "center" }}>
					<img src={`/api/file/${this.state.qn.image}`} height="500px" />
				</div>
				<div style={{ display: "flex", flexDirection: 'row-reverse' }} className="px-2 mb-2">
					<button className='btn btn-link' onClick={() => { this.setState({ showQn: false }) }}>Retour au formulaire</button>
				</div>
			</div>}
		</Modal>
	}

	renderModalIntervention = () => {
		let codeErreur = (this.state.intervention && this.state.intervention.codeErreur) ? this.state.codeErreurList.find(err => err.code.toUpperCase().trim() === this.state.intervention.codeErreur.toUpperCase().trim()) : null
		return <Modal
			show={this.state.intervention !== null}
			onHide={() => this.setState({ intervention: null })}
			// className=""
			dialogClassName="modal-75w"
			centered
		>
			{this.state.intervention && <div style={{
				height: "calc(95vh - 30px)",
				overflow: "auto",
				padding: "8 15 0",
				display: 'flex', flexDirection: "column"
			}}>
				<div style={{ flex: 1 }}>
					<h2 className='text-center'>Formulaire d'intervention</h2>
					<div style={{}}>
						<div className='row my-2 '>
							<div className='col-2 text-right'>
								Serie
							</div>
							<div className='col-3'>
								{this.state.intervention.serie}
							</div>
							<div className='col-2 text-right'>
								Séquence
							</div>
							<div className='col-3'>
								{this.state.intervention.sequence}
							</div>
						</div>
						<div className='row my-2 '>
							<div className='col-2 text-right'>
								date
							</div>
							<div className='col-3'>
								{this.state.intervention.date}
							</div>
							<div className='col-2 text-right'>
								shift
							</div>
							<div className='col-3'>
								{this.state.intervention.shift}
							</div>
						</div>
						<div className='row my-2 '>
							<div className='col-2 text-right'>
								Référence tissu
							</div>
							<div className='col-3'>
								{this.state.intervention.partNumberMaterial}
							</div>
							<div className='col-2 text-right'>
								Description
							</div>
							<div className='col-3'>
								{this.state.intervention.partNumberMaterialDescription}
							</div>
						</div>
						<div className='row my-2 '>
							<div className='col-2 text-right'>
								Machine
							</div>
							<div className='col-3'>
								{this.state.intervention.machine}
							</div>
						</div>
						<div className='row my-2 '>
							<div className='col-2 text-right'>
								Code d'erreur
							</div>
							<div className='col-3'>
								{this.state.intervention.codeErreur}
							</div>
							<div className='col-2 text-right'>
								Début d'arrêt
							</div>
							<div className='col-3'>
								{this.state.intervention.debutArret?.replace("T", " ")}
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
								{this.state.intervention.debutIntervention?.replace("T", " ")}
							</div>
							<div className='col-2 text-right'>
								Fin d'intervention
							</div>
							<div className='col-3'>
								{this.state.intervention.finIntervention?.replace("T", " ")}
							</div>
						</div>
						<div className='row my-2'>
							<label className='col-2 col-form-label text-right'>Département</label>
							<Select classNamePrefix="rs" className='p-0 input-sm col-3'
								isClearable={true} placeholder={"Département..."}
								value={this.state.intervention.departement ? { label: this.state.intervention.departement, value: this.state.intervention.departement } : null}
								options={departementOption}
								// menuPosition={'absolute'} // 'fixed', 'absolute'
								menuPlacement={"top"} // auto, bottom, top
								onChange={(option) => {
									if (option) {
										this.setState({
											intervention: {
												...this.state.intervention,
												departement: option.value,
												codeArret: null,
											}
										})
									} else {
										this.setState({
											intervention: {
												...this.state.intervention,
												departement: null,
												codeArret: null,
											}
										})
									}
								}}
							/>
							<label className='col-2 col-form-label text-right'>Problème résolu ?</label>
							<Select classNamePrefix="rs" className='p-0 input-sm col-3'
								isClearable={true} placeholder={"Problème Resolu..."}
								value={this.state.intervention.problemeResolu ? { label: this.state.intervention.problemeResolu, value: this.state.intervention.problemeResolu } : null}
								options={problemeResoluOption}
								menuPlacement={"top"}
								onChange={(option) => {
									if (option) {
										this.setState({
											intervention: {
												...this.state.intervention,
												problemeResolu: option.value,
											}
										})
									} else {
										this.setState({
											intervention: {
												...this.state.intervention,
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
								value={this.state.intervention.codeArret ? { label: this.state.intervention.codeArret.code, value: this.state.intervention.codeArret } : null}
								options={this.state.intervention.departement ? this.state.codeArretList.filter(e => e.departement === this.state.intervention.departement).map(codeArret => ({ label: codeArret.code, value: codeArret })) : []} // departementOption
								// menuPosition={'absolute'} // 'fixed', 'absolute'
								menuPlacement={"top"} // auto, bottom, top
								onChange={(option) => {
									if (option) {
										this.setState({
											intervention: {
												...this.state.intervention,
												codeArret: option.value,
											}
										})
									} else {
										this.setState({
											intervention: {
												...this.state.intervention,
												codeArret: null,
											}
										})
									}
								}}
							/>
							<label className='col-2 col-form-label text-right'>Motif d'arrêt</label>
							<div className='col-3 ' style={{ padding: "7 0 0" }}>
								{this.state.intervention.codeArret && this.state.intervention.codeArret.motifArret}
							</div>

						</div>
						<div className='row my-2'>
							<label className='col-2 col-form-label text-right'>Cause</label>
							<div className='col-3 p-0'>
								<input className={`form-control input-sm`}
									value={this.state.intervention.cause || ''}
									onChange={event => this.setState({ intervention: { ...this.state.intervention, cause: event.target.value } })}
								/>
							</div>
							<label className='col-2 col-form-label text-right'>Action</label>
							<div className='col-3 p-0'>
								<input className={`form-control input-sm`}
									value={this.state.intervention.action || ''}
									onChange={event => this.setState({ intervention: { ...this.state.intervention, action: event.target.value } })}
								/>
							</div>
						</div>
					</div>

					<div className='row my-2'>
						<label className='col-2 col-form-label text-right'>Matricule d'émetteur</label>
						<div className='col-3 ' style={{ padding: "7 0 0" }}>
							{this.state.intervention.matriculeEmetteur}
							{/* <input className={`form-control input-sm`}
								value={this.state.intervention.matriculeEmetteur || ''}
								onChange={event => this.setState({ intervention: { ...this.state.intervention, matriculeEmetteur: event.target.value } })}
							/> */}
						</div>
						<label className='col-2 col-form-label text-right'>Matricule de responsable</label>
						<div className='col-3 p-0'>
							<input className={`form-control input-sm`}
								value={this.state.intervention.matriculeResponsable || ''}
								onChange={event => this.setState({ intervention: { ...this.state.intervention, matriculeResponsable: event.target.value } })}
							/>
						</div>
					</div>
					<div>
						<button
							className='btn btn-sm btn-outline-dark ml-2 mb-2' style={{ padding: "4 40" }}
							onClick={() => {
								this.searchInterventions(this.state.intervention.serie)
							}}
						><FontAwesomeIcon icon={faArrowRotateRight} /> Refresh</button>
					</div>
					<div className='table-responsive'>
						<table className='table table-bordered table-cells-sm mb-0' style={{ fontSize: 12 }}>
							<thead className='header-table-black'>
								<tr>
									<th>Id</th>
									<th>type</th>
									<th>Sous type</th>
									<th>Date de creation</th>
									<th>Début d'arrêt</th>
									<th>Début d'intervention</th>
									<th>Fin d'intervention</th>
									<th>Code d'erreur</th>

									<th>Code d'arrêt</th>
									<th>Motif d'arrêt</th>
									<th>Type d'arrêt</th>
									<th>Cause</th>
									<th>Action</th>
									<th>Département</th>
									<th>Problème résolu</th>
									<th>Matricule d'émetteur</th>
									<th>Matricule de responsable</th>
									<th>Machine</th>
								</tr>
							</thead>
							<tbody>
								{this.state.interventionList && this.state.interventionList.map((elem, ind) => <tr
									key={"row-" + ind}
									onClick={() => {
										if (this.state.intervention.id === elem.id) {
											this.setState({
												intervention: {
													serie: this.state.obj.serie,
													sequence: this.state.obj.cuttingRequest.sequence,
													date: this.state.obj.planningDate,
													shift: this.state.obj.shift,
													partNumberMaterial: this.state.obj.partNumberMaterial,
													partNumberMaterialDescription: this.state.obj.description,
													debutArret: moment().format("YYYY-MM-DD,HH:mm:ss").replace(",", "T"),
													debutIntervention: moment().format("YYYY-MM-DD,HH:mm:ss").replace(",", "T"),
													finIntervention: moment().format("YYYY-MM-DD,HH:mm:ss").replace(",", "T"),
													machine: this.state.postObj ? this.state.postObj.tableCoupe : null
												}
											})
										} else {
											this.setState({ intervention: { ...elem } })
										}
									}}
									style={this.state.intervention.id === elem.id ? { backgroundColor: "lightgray" } : {}}
								>
									<td>{elem.id}</td>
									<td>{elem.type}</td>
									<td>{elem.sousType}</td>
									<td>{elem.createdAt}</td>
									<td>{elem.debutArret?.replace("T", " ")}</td>
									<td>{elem.debutIntervention?.replace("T", " ")}</td>
									<td>{elem.finIntervention?.replace("T", " ")}</td>
									<td>{elem.codeErreur}</td>
									<td>{elem.codeArret?.code}</td>
									<td>{elem.codeArret?.motifArret}</td>
									<td>{elem.codeArret?.typeArret}</td>
									<td>{elem.cause}</td>
									<td>{elem.action}</td>
									<td>{elem.departement}</td>
									<td>{elem.problemeResolu}</td>
									<td>{elem.matriculeEmetteur}</td>
									<td>{elem.matriculeResponsable}</td>
									<td>{elem.machine}</td>
								</tr>)}
							</tbody>
						</table>
					</div>
				</div>
				<hr />
				<div style={{ display: "flex", flexDirection: 'row-reverse', backgroundColor: "white", position: "sticky", bottom: 0 }} className="p-2">
					{/* {this.state.intervention.id && <button className='btn btn-danger' onClick={() => {
						axios.post("/api/intervention", this.state.intervention)
							.then(res => {
								this.setState({
									intervention: {
										serie: this.state.obj.serie,
										sequence: this.state.obj.cuttingRequest.sequence,
										date: this.state.obj.planningDate,
										shift: this.state.obj.shift,
										partNumberMaterial: this.state.obj.partNumberMaterial,
										partNumberMaterialDescription: this.state.obj.description,
										debutArret: moment().format("YYYY-MM-DD,HH:mm:ss").replace(",", "T"),
										debutIntervention: moment().format("YYYY-MM-DD,HH:mm:ss").replace(",", "T"),
										finIntervention: moment().format("YYYY-MM-DD,HH:mm:ss").replace(",", "T"),
										machine: this.state.postObj ? this.state.postObj.tableCoupe : null,
										type: this.state.mode
									}
								})
								this.searchInterventions(this.state.obj.serie)
							})
					}}>{"Modification " + this.state.intervention.id}</button>} */}
					<button className='btn btn-link' onClick={() => { this.setState({ intervention: null }) }}>Retour au formulaire</button>
				</div>
			</div>}
		</Modal>
	}

	renderHeader = () => {
		return <div className="row"
			style={{
				margin: "0",
				// width: "31.5cm", 
				marginBottom: "5px"
			}}
		>
			<div className="col-3 border border-dark" style={{ paddingLeft: "43px", paddingTop: "3px" }}>
				<img
					src={logo}
					alt="lear logo"
					height="40"
				/>
			</div>
			<div className="col-6 border border-dark">
				<h4 className="text-center mt-2">FICHE DE COUPE</h4>
			</div>
			<div className="col-3 border border-dark">
				<p className="text-center mt-2">FR PR 47</p>
			</div>
		</div>
	}

	renderInfo = () => {
		return <div>
			<div className='d-flex'>
				<div style={{ width: "20%", textAlign: "end" }}>
					matelasseur :
				</div>
				<div style={{ width: "30%", paddingLeft: "20" }}>
					{this.state.matelasseur1 ? this.state.matelasseur1 : this.state.obj.matelasseur1} / {this.state.matelasseur2 ? this.state.matelasseur2 : this.state.obj.matelasseur2}
				</div>
				<div style={{ width: "20%", textAlign: "end" }}>
					Table :
				</div>
				<div style={{ width: "30%", paddingLeft: "20" }}>
					{this.state.obj.tableMatelassage} {this.state.obj.zoneMatelassage}
				</div>
			</div>
			<div className='d-flex'>
				<div style={{ width: "20%", textAlign: "end" }}>
					coupeur1 :
				</div>
				<div style={{ width: "30%", paddingLeft: "20" }}>
					{this.state.coupeur1 ? this.state.coupeur1 : this.state.obj.coupeur1}
				</div>
				<div style={{ width: "20%", textAlign: "end" }}>
					Machine :
				</div>
				<div style={{ width: "30%", paddingLeft: "20" }}>
					{this.state.obj.tableCoupe} ({this.state.obj.machine})
				</div>
			</div>
			<div className='d-flex'>
				<div style={{ width: "20%", textAlign: "end" }}>
					Serie :
				</div>
				<div style={{ width: "30%", paddingLeft: "20" }}>
					<input style={{ width: "200", padding: "0 5", fontSize: 28, fontWeight: "bold" }} ref={input => this.serieInput = input}
						value={this.state.obj.serie}
						onChange={e => {
							if (/^\d*$/.test(e.target.value)) {
								this.setState({ obj: { serie: e.target.value, qn: null, showQn: false, rouleauObj: {} }, error: null })
							}
						}}
						onKeyUp={(e) => {
							if (e.key === "Enter") {
								this.setState({ obj: { serie: this.state.obj.serie }, qn: null, showQn: false, rouleauObj: {} })
								axios.get(`/api/cuttingRequestSerieInfo/${this.state.obj.serie}`)
									.then((res) => {
										this.setState({ obj: { ...this.state.obj, ...res.data } })
										this.searchInterventions(res.data.serie)
										axios.get(`/api/qn/reftissu?reftissus=${this.state.obj.partNumberMaterial}`)
											.then(res2 => {
												let arrQn = res2.data.filter(e => e.resultat === "Non ok" && e.placement && (e.placement.toUpperCase().trim() === "ALL" || e.placement.split(";").map(e => e.trim().toUpperCase()).includes(this.state.obj.placement.trim().toUpperCase())))
												if (arrQn.length > 0) {
													this.setState({ qn: arrQn[0], showQn: true })
												} else {
													arrQn = res2.data.filter(e => e.resultat === "Formation" && e.placement && (e.placement.toUpperCase().trim() === "ALL" || e.placement.split(";").map(e => e.trim().toUpperCase()).includes(this.state.obj.placement.trim().toUpperCase())))
													if (arrQn.length > 0) {
														this.setState({ qn: arrQn[0], showQn: true })
													}
												}
											})
											.finally(() => {
												axios.get(`/api/placement/nbrPiece/${this.state.obj.placement}`)
													.then(res => {
														this.setState({ obj: { ...this.state.obj, nbrPiece: res.data } })
													})
											})
									})
									.catch(err => {
										this.setState({ error: err.response.data, obj: {} })
									})
							}
						}}
					/>
				</div>
				<div style={{ width: "20%", textAlign: "end" }}>
					Séquence :
				</div>
				<div style={{ width: "30%", paddingLeft: "20", fontSize: 28, fontWeight: "bold" }}>
					{this.state.obj.cuttingRequest && this.state.obj.cuttingRequest.sequence}
				</div>

			</div>
			<div className='d-flex'>
				{/* <div style={{ width: "20%", textAlign: "end" }}>
					Definition :
				</div>
				<div style={{ width: "30%", paddingLeft: "20" }}>
					{this.state.obj.cuttingRequest && this.state.obj.cuttingRequest.definition}
				</div> */}
				<div style={{ width: "20%", textAlign: "end" }}>
					Date de creation :
				</div>
				<div style={{ width: "30%", paddingLeft: "20" }}>
					{this.state.obj.cuttingRequest && this.state.obj.cuttingRequest.createdAt}
				</div>
			</div>
			<div className='d-flex'>
				<div style={{ width: "20%", textAlign: "end" }}>
					Modele :
				</div>
				<div style={{ width: "80%", paddingLeft: "20" }}>
					{this.state.obj.cuttingRequest && this.state.obj.cuttingRequest.modele}
				</div>
			</div>
			<div className='d-flex'>
				<div style={{ width: "20%", textAlign: "end" }}>
					Part Numbers :
				</div>
				<div style={{ width: "80%", paddingLeft: "20" }}>
					{this.state.obj.partNumbers}
				</div>
			</div>
			<div className='d-flex'>
				<div style={{ width: "20%", textAlign: "end" }}>
					Status Matelassage :
				</div>
				<div style={this.state.obj.statusMatelassage === "Waiting" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#ffafaf" } :
					this.state.obj.statusMatelassage === "In progress" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#f6ff6b" } :
						this.state.obj.statusMatelassage === "Complete" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#7bff6b" } :
							this.state.obj.statusMatelassage === "Incomplete" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#ffc46b" } :
								{ width: "80%", paddingLeft: "70", marginLeft: 20 }}>
					{this.state.obj.statusMatelassage}
				</div>
			</div>
			<div className='d-flex'>
				<div style={{ width: "20%", textAlign: "end" }}>
					Status Coupe :
				</div>
				<div style={this.state.obj.statusCoupe === "Waiting" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#ffafaf" } :
					this.state.obj.statusCoupe === "In progress" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#f6ff6b" } :
						this.state.obj.statusCoupe === "Complete" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#7bff6b" } :
							this.state.obj.statusCoupe === "Incomplete" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#ffc46b" } :
								{ width: "80%", paddingLeft: "70", marginLeft: 20 }}>
					{this.state.obj.statusCoupe}
				</div>
			</div>
		</div>
	}

	renderPlanInfo = () => {
		let arrDrill = (this.state.obj.drill || ",").split(",").map(e => e != "" ? parseInt(e) : 0)
		return <div>
			<h4>Information indiquée sur le plan de coupe :</h4>
			<div className='table-responsive'>
				<table className='table table-bordered table-cells-sm' style={{ fontSize: 14 }}>
					<thead className='header-table-black'>
						<tr>
							<th>Placement</th>
							<th>Longueur</th>
							<th>Nbr de Couche</th>
							<th>Laize</th>
							<th>Ref tissu</th>
							<th>Description</th>
							<th>Drill 1</th>
							<th>Drill 2</th>
							<th>Config</th>
							<th>Nbr piece</th>
						</tr>
					</thead>
					<tbody>
						<tr>
							<td
								onDoubleClick={() => {
									if (this.state.qn) {
										this.setState({ showQn: true })
									}
								}}
								style={(this.state.qn && this.state.qn.placement && this.state.qn.placement.split(";").map(e => e.trim().toUpperCase()).includes(this.state.obj.placement.trim().toUpperCase()) && this.state.qn.resultat === "Non ok") ? { backgroundColor: "red" }
									: (this.state.qn && this.state.qn.placement && this.state.qn.placement.split(";").map(e => e.trim().toUpperCase()).includes(this.state.obj.placement.trim().toUpperCase()) && this.state.qn.resultat === "Formation") ? { backgroundColor: "yellow" }
										: {}}
							>{this.state.obj.placement}</td>
							<td>{this.state.obj.longueur && (this.state.obj.longueur).toFixed(3)}</td>
							<td>{this.state.obj.nbrCouche}</td>
							<td>{this.state.obj.laize}</td>
							<td
								onDoubleClick={() => {
									if (this.state.qn) {
										this.setState({ showQn: true })
									}
								}}
								style={(this.state.qn && this.state.qn.resultat === "Non ok") ? { backgroundColor: "red" }
									: (this.state.qn && this.state.qn.resultat === "Formation") ? { backgroundColor: "yellow" }
										: {}}
							>
								{this.state.obj.partNumberMaterial}
							</td>
							<td>{this.state.obj.description}</td>
							<td>{arrDrill[0]}</td>
							<td>{arrDrill[1]}</td>
							<td>{this.state.obj.config}</td>
							<td>{this.state.obj.nbrPiece}</td>
						</tr>
					</tbody>
				</table>
			</div>
		</div>
	}

	startLoop() {
		if (this.state.obj.placement) {
			axios.post(`/api/cuttingRequestSerieInfo/placement`, {
				serie: this.state.obj.serie,
				coupeur: this.state.postObj.coupeur1,
				machine: this.state.postObj.tableCoupe
			})
				.then(res => {
					if (res.data.showIntervention) {
						this.searchInterventions(this.state.obj.serie, res.data.showIntervention)
					}
					this.setState({
						obj: {
							...this.state.obj,
							dateDebutCoupe: res.data.dateDebutCoupe,
							dateFinCoupe: res.data.dateFinCoupe,
							logs: res.data.content.join("\n")
						}
					})
				})
		}
		this.loopInterval = setInterval(() => {
			// Your loop code goes here
			if (this.state.obj.placement) {
				axios.post(`/api/cuttingRequestSerieInfo/placement`, {
					serie: this.state.obj.serie,
					coupeur: this.state.postObj.coupeur1,
					machine: this.state.postObj.tableCoupe
				})
					.then(res => {
						if (res.data.showIntervention) {
							this.searchInterventions(this.state.obj.serie, res.data.showIntervention)
						}
						if (this.state.obj.statusCoupe === "Waiting" && res.data.dateDebutCoupe != null && res.data.dateFinCoupe == null) {
							axios.post("/api/cuttingRequestSerieInfo", {
								...this.state.obj,
								...this.state.postObj,
								statusCoupe: "In progress",
								dateDebutCoupe: res.data.dateDebutCoupe,
								dateFinCoupe: res.data.dateFinCoupe,
								logs: res.data.content.join("\n")
							})
								.then(res => {
									this.setState({ obj: { ...res.data } })
								})
						} else if (this.state.obj.statusCoupe === "In progress" && res.data.dateDebutCoupe != null && res.data.dateFinCoupe != null) {
							axios.post("/api/cuttingRequestSerieInfo", {
								...this.state.obj,
								...this.state.postObj,
								statusCoupe: "Complete",
								dateDebutCoupe: res.data.dateDebutCoupe,
								dateFinCoupe: res.data.dateFinCoupe,
								logs: res.data.content.join("\n")
							})
								.then(res => {
									this.setState({ obj: { ...res.data } })
								})
								.catch(err => {
									this.setState({ error: err.response.data })
								})
						} else {
							this.setState({
								obj: {
									...this.state.obj,
									dateDebutCoupe: res.data.dateDebutCoupe,
									dateFinCoupe: res.data.dateFinCoupe,
									logs: res.data.content.join("\n")
								}
							})

						}
					})
					.catch(err => {
						this.setState({ error: err.response.data })
					})
			}
		}, 5000);
	}

	stopLoop() {
		clearInterval(this.loopInterval);
	}



	renderCoupeForm = () => {
		let arrDrill = (this.state.obj.drill || ",").split(",").map(e => e != "" ? parseInt(e) : 0)
		return <div>
			<h4>OK Démarrage Coupe - auto contrôle :</h4>
			<div style={{ padding: "10", backgroundColor: "#f3f3f3", borderRadius: "5" }}>
				<div className='d-flex flex-wrap'>
					{/* <div style={{ width: 80, padding: "0 10" }}>
						Auto fill<br />
						<Switch id="envoyerCAD" name="envoyerCAD" checked={this.state.obj.autoCoupe || false}
							handleDiameter={15} height={20}
							className="react-switch mt-1" offColor="#F00"
							onChange={(checked) => {
								this.setState({ obj: { ...this.state.obj, autoCoupe: checked } })
							}}
						/>
					</div> */}
					<div style={{ width: 260, padding: "0 10" }}>
						Date début<br />
						<input type="datetime-local" name="dateDebutCoupe" step="1" min="2023-01-01T00:00:00" max="2100-12-31T23:59:59" style={{ width: 240, color: "black" }}
							value={this.state.obj.dateDebutCoupe ? this.state.obj.dateDebutCoupe : undefined}
							disabled={this.state.obj.autoCoupe}
							onChange={e => {
								if (e.target.value === null || e.target.value === "") {
									this.setState({ obj: { ...this.state.obj, dateDebutCoupe: e.target.value } })
								} else {
									this.setState({ obj: { ...this.state.obj, dateDebutCoupe: e.target.value } })
								}
							}}
						/>
					</div>
					<div style={{ width: 260, padding: "0 10" }}>
						Date fin<br />
						<input type="datetime-local" name="dateFinCoupe" step="1" min="2023-01-01T00:00:00" max="2100-12-31T23:59:59" style={{ width: 240, color: "black" }}
							value={this.state.obj.dateFinCoupe ? this.state.obj.dateFinCoupe : undefined} disabled={this.state.obj.autoCoupe}
							onChange={e => {
								if (e.target.value === null || e.target.value === "") {
									this.setState({ obj: { ...this.state.obj, dateFinCoupe: null } })
								} else {
									this.setState({ obj: { ...this.state.obj, dateFinCoupe: e.target.value } })
								}
							}}

						/>
					</div>

				</div>
				<div className='d-flex flex-wrap mt-2'>
					<div style={{ width: 170, padding: "0 10" }}>
						<label>Début matelas</label><br />{/* show two button that set value OK or NOK in this field */}
						<button className={"btn btn-sm " + (this.state.obj.premierPaquet === 'OK' ? "btn-success" : "btn-outline-success")}
							onClick={() => this.setState({ obj: { ...this.state.obj, premierPaquet: 'OK' } })}style={{ marginRight: 30 }}
							>OK</button>
						<button className={"btn btn-sm " + (this.state.obj.premierPaquet === 'NOK' ? "btn-danger" : "btn-outline-danger")}
							onClick={() => this.setState({ obj: { ...this.state.obj, premierPaquet: 'NOK' } })}>NOK</button>
						{this.state.obj.premierPaquetDate && [<br />, <i>{this.state.obj.premierPaquetDate}</i>]}
					</div>
					{this.state.obj.longueur >= 3 && <div style={{ width: 170, padding: "0 10" }}>
						<label>Milieu matelas</label><br />{/* show two button that set value OK or NOK in this field */}
						<button className={"btn btn-sm " + (this.state.obj.milieuPaquet === 'OK' ? "btn-success" : "btn-outline-success")}
							onClick={() => this.setState({ obj: { ...this.state.obj, milieuPaquet: 'OK' } })}style={{ marginRight: 30 }}
							>OK</button>
						<button className={"btn btn-sm " + (this.state.obj.milieuPaquet === 'NOK' ? "btn-danger" : "btn-outline-danger")}
							onClick={() => this.setState({ obj: { ...this.state.obj, milieuPaquet: 'NOK' } })}>NOK</button>
							{this.state.obj.milieuPaquetDate && [<br />, <i>{this.state.obj.milieuPaquetDate}</i>]}
					</div>}
					{this.state.obj.longueur >= 0.4 && <div style={{ width: 170, padding: "0 10" }}>
						<label>Fin matelas</label><br />{/* show two button that set value OK or NOK in this field */}
						<button className={"btn btn-sm " + (this.state.obj.dernierPaquet === "OK" ? "btn-success" : "btn-outline-success")}
							onClick={() => this.setState({ obj: { ...this.state.obj, dernierPaquet: 'OK' } })}style={{ marginRight: 30 }}
							>OK</button>
						<button className={"btn btn-sm " + (this.state.obj.dernierPaquet === "NOK" ? "btn-danger" : "btn-outline-danger")}
							onClick={() => this.setState({ obj: { ...this.state.obj, dernierPaquet: 'NOK' } })}>NOK</button>
							{this.state.obj.dernierPaquetDate && [<br />, <i>{this.state.obj.dernierPaquetDate}</i>]}
					</div>}
					{(arrDrill[0] !== 0) && <div style={{ width: 170, padding: "0 10" }}>
						<label>Drill 1</label><br />{/* show two button that set value OK or NOK or NA in this field */}
						{<button className={"btn btn-sm " + (this.state.obj.verificationDrill === "OK" ? "btn-success" : "btn-outline-success")}
							onClick={() => this.setState({ obj: { ...this.state.obj, verificationDrill: "OK" } })}
							disabled={arrDrill[0] === 0 && arrDrill[1] === 0}
							style={{ marginRight: 30 }}
							>OK</button>}
						{<button className={"btn btn-sm " + (this.state.obj.verificationDrill === "NOK" ? "btn-danger" : "btn-outline-danger")}
							onClick={() => this.setState({ obj: { ...this.state.obj, verificationDrill: "NOK" } })}
							disabled={arrDrill[0] === 0 && arrDrill[1] === 0}
						>NOK</button>}
						{this.state.obj.verificationDrillDate && [<br />, <i>{this.state.obj.verificationDrillDate}</i>]}
					</div>}
					{arrDrill[1] !== 0 && <div style={{ width: 170, padding: "0 10" }}>
						<label>Drill 2</label><br />{/* show two button that set value OK or NOK or NA in this field */}
						{<button className={"btn btn-sm " + (this.state.obj.verificationDrill2 === "OK" ? "btn-success" : "btn-outline-success")}
							onClick={() => this.setState({ obj: { ...this.state.obj, verificationDrill2: "OK" } })}
							disabled={arrDrill[0] === 0 && arrDrill[1] === 0}
							style={{ marginRight: 30 }}
							>OK</button>}
						{<button className={"btn btn-sm " + (this.state.obj.verificationDrill2 === "NOK" ? "btn-danger" : "btn-outline-danger")}
							onClick={() => this.setState({ obj: { ...this.state.obj, verificationDrill2: "NOK" } })}
							disabled={arrDrill[0] === 0 && arrDrill[1] === 0}
						>NOK</button>}
						{this.state.obj.verificationDrill2Date && [<br />, <i>{this.state.obj.verificationDrill2Date}</i>]}
					</div>}
				</div>
				<hr />
				<h4>Information à noter par la qualité</h4>
				<div className='d-flex flex-wrap'>

					<div style={{ width: 170, padding: "0 10" }}>
						<label>Qte non conforme</label><br />
						<input type="number" name="qteNonConforme" style={{ width: 150, color: "black", padding: "3 8" }}
							value={this.state.obj.qteNonConforme || ""}
							onChange={e => {
								if (e.target.value === null || e.target.value === "") {
									this.setState({ obj: { ...this.state.obj, qteNonConforme: null, codeDefaut: null, lieuDetection: null } })
								} else {
									this.setState({ obj: { ...this.state.obj, qteNonConforme: parseInt(e.target.value), codeDefaut: null, lieuDetection: null } })
								}
							}}
						/>
					</div>



					<div style={{ width: 500, padding: "0 10" }}>
						<label>Code defaut</label><br />
						<Select id={"codeDefaut"} name={"codeDefaut"} classNamePrefix="rs"
							placeholder={"Code Defaut..."} className='col-12 p-0' isDisabled={this.state.obj.qteNonConforme == null || this.state.obj.qteNonConforme === "" || this.state.obj.qteNonConforme == 0}
							isClearable={true} menuPlacement={"top"}
							value={(this.state.codeDefautList && this.state.codeDefautList.length > 0 && this.state.obj.codeDefaut)
								? { label: this.state.obj.codeDefaut.code + ' (' + this.state.obj.codeDefaut.description + ')', value: this.state.obj.codeDefaut, item: this.state.obj.codeDefaut }
								: null
							}
							options={this.state.codeDefautList.filter(e => e.code != null && e.code.toUpperCase().startsWith("C")).map(e => { return { label: e.code + ' (' + e.description + ')', value: e } })}
							onChange={(option) => {
								this.setState({ obj: { ...this.state.obj, codeDefaut: (option ? option.value : null) } })
							}}
						/>
					</div>
					<div style={{ width: 250, padding: "0 10" }}>
						<label>Lieu de detection</label><br />
						<Select id={"lieuDetection"} name={"lieuDetection"} classNamePrefix="rs"
							placeholder={"Lieu de detection..."} className='col-12 p-0'
							isClearable={true} menuPlacement={"top"} isDisabled={this.state.obj.qteNonConforme == null || this.state.obj.qteNonConforme === "" || this.state.obj.qteNonConforme == 0}
							value={(this.state.obj.lieuDetection)
								? { label: this.state.obj.lieuDetection, value: this.state.obj.lieuDetection, item: this.state.obj.lieuDetection }
								: null
							}
							options={lieuDetectionOptions}
							onChange={(e) => {
								if (e) {
									this.setState({ obj: { ...this.state.obj, lieuDetection: e.value } })
								} else {
									this.setState({ obj: { ...this.state.obj, lieuDetection: null } })
								}
							}}
						/>
					</div>
					{/* <div style={{ width: 500, padding: "0 10" }}>
						<label>Code defaut additionnel</label><br />
						<Select id={"codeDefautAdditionnel"} name={"codeDefautAdditionnel"} classNamePrefix="rs"
							placeholder={"Code Defaut Additionnel..."} className='col-12 p-0' isDisabled={this.state.obj.qteNonConforme == null || this.state.obj.qteNonConforme === "" || this.state.obj.qteNonConforme == 0}
							isClearable={true} menuPlacement={"top"}
							value={(this.state.codeDefautList && this.state.codeDefautList.length > 0 && this.state.obj.codeDefautAdditionnel)
								? { label: this.state.obj.codeDefautAdditionnel.code + ' (' + this.state.obj.codeDefautAdditionnel.description + ')', value: this.state.obj.codeDefautAdditionnel, item: this.state.obj.codeDefautAdditionnel }
								: null
							}
							options={this.state.codeDefautList.map(e => { return { label: e.code + ' (' + e.description + ')', value: e } })}
							onChange={(option) => {
								this.setState({ obj: { ...this.state.obj, codeDefautAdditionnel: (option ? option.value : null) } })
							}}
						/>
					</div> */}


				</div>
				<div className='d-flex flex-wrap mt-2'>
					<div style={{ width: 170, padding: "0 10" }}>
						<label>Qte scrap</label><br />
						<input type="number" name="qteScrap" style={{ width: 150, color: "black", padding: "3 8" }}
							value={this.state.obj.qteScrap || ""}
							onChange={e => {
								if (e.target.value === null || e.target.value === "") {
									this.setState({ obj: { ...this.state.obj, qteScrap: null, codeScrap: null } })
								} else {
									this.setState({ obj: { ...this.state.obj, qteScrap: parseInt(e.target.value), codeScrap: null } })
								}
							}}
						/>
					</div>
					<div style={{ width: 500, padding: "0 10" }}>
						<label>Code d'erreur qualité</label><br />
						<Select id={"codeScrap"} name={"codeScrap"} classNamePrefix="rs"
							placeholder={"Code Scrap..."} className='col-12 p-0' isDisabled={this.state.obj.qteScrap === null || this.state.obj.qteScrap === undefined || this.state.obj.qteScrap === 0}
							isClearable={true} menuPlacement={"top"}
							value={(this.state.codeScrapList && this.state.codeScrapList.length > 0 && this.state.obj.codeScrap)
								? { label: this.state.obj.codeScrap.code + ' (' + this.state.obj.codeScrap.description + ')', value: this.state.obj.codeScrap, item: this.state.obj.codeScrap }
								: null
							}
							options={this.state.codeScrapList.map(e => { return { label: e.code + ' (' + e.description + ')', value: e } })}
							onChange={(option) => {
								this.setState({ obj: { ...this.state.obj, codeScrap: (option ? option.value : null) } })
							}}
						/>
					</div>
				</div>

			</div>
		</div>
	}

	renderMatelassage = () => {
		return <div>
			<h4>Matlassage :</h4>
			<div style={{ padding: "10", backgroundColor: "#f3f3f3", borderRadius: "5" }}>
				{this.state.obj.cuttingRequestSerieRouleaus && <div className='table-responsive bg-white'>
					<table className='table table-bordered table-cells-sm m-0' style={{ fontSize: 14 }}>
						<thead className='header-table-black'>
							<tr>
								{/* <th>confirmReftissu</th> */}
								<th>Rouleau</th>
								<th>Lot</th>
								<th>Metrage</th>
								<th>Laize</th>
								<th>NbrCouche</th>
								<th>longueur 1er Couche</th>
								<th>longueur Couche Overlap</th>
								<th>Defaut</th>
								<th>Non Utitlse</th>
								<th>Retour</th>
								<th>Excès</th>
								<th>Total Usage</th>
								<th>Overlap 1</th>
								<th>Overlap 2</th>
								<th>Overlap 3</th>
								<th>Overlap 4</th>
								<th>Overlap 5</th>
								<th>Overlap 6</th>
								<th>Overlap 7</th>
								<th>Overlap 8</th>
							</tr>
						</thead>
						<tbody>
							{this.state.obj.cuttingRequestSerieRouleaus
								.sort((a, b) => a.id - b.id)
								.map((elem, ind) => {
									return <tr key={"row-" + ind}
										style={this.state.rouleauObj.id == elem.id ? { backgroundColor: "lightgray" } : {}}
										onClick={() => {
											if (this.state.rouleauObj.id != elem.id) {
												this.setState({ rouleauObj: { ...elem, idRouleau: "S" + elem.idRouleau, lotFrs: "H" + elem.lotFrs, confirmReftissu: "" } })
											} else {
												this.setState({ rouleauObj: {} })
											}
										}}
									>
										{/* <td>{elem.confirmReftissu}</td> */}
										<td>{elem.idRouleau}</td>
										<td>{elem.lotFrs}</td>
										<td>{elem.metrage}</td>
										<td>{elem.laize}</td>
										<td>{elem.nbrCouche}</td>
										<td>{elem.longueurPremierCouche}</td>
										<td>{elem.longueurCoucheOverlap}</td>
										<td>{elem.defaut}</td>
										<td>{elem.nonUtitlse}</td>
										<td style={elem.confirmRetour ? { backgroundColor: "lightgreen" } : { backgroundColor: "pink" }}>{elem.retour && elem.retour.toFixed(3)}</td>
										<td>{elem.excess && elem.excess.toFixed(3)}</td>
										<td>{elem.totalUsage && elem.totalUsage.toFixed(3)}</td>
										<td>{elem.overlap1}</td>
										<td>{elem.overlap2}</td>
										<td>{elem.overlap3}</td>
										<td>{elem.overlap4}</td>
										<td>{elem.overlap5}</td>
										<td>{elem.overlap6}</td>
										<td>{elem.overlap7}</td>
										<td>{elem.overlap8}</td>
									</tr>
								})}

						</tbody>
					</table>
				</div>}
			</div>
		</div>
	}

	render() {
		return (<div style={{ minHeight: "100%", display: "flex", flexDirection: "column" }}>
			<div className='p-2'>
				{this.renderHeader()}
				<div style={{ fontSize: 14 }}>
					{this.renderInfo()}
				</div>
				{this.state.obj.placement && this.renderPlanInfo()}
				{this.state.obj.placement && this.renderMatelassage()}
				{this.state.obj.placement && this.renderCoupeForm()}
				{this.state.obj.placement && <span style={{ color: "green", padding: "0 10 15" }}>
					Dans le cas des points NOK, aviser le Aid chef / chef d'équipe / Coordenateur qualité / Contrôle volant / Agent maitenance / Agent CAD
				</span>}

				{this.state.error != null && <div className="alert alert-danger alert-error text-center m-4" role="alert">
					<ul>
						<button type='btn' className="btn btn-toclose" onClick={() => { this.setState({ error: null }) }}>
							<FontAwesomeIcon icon={faTimes} size="2x" />
						</button>
						{this.renderErrorsAlert(this.state.error)}
					</ul>
				</div>}

				{this.renderModalQn()}
				{this.renderModalIntervention()}
			</div>
			<div style={{ flex: 1 }}></div>
			<div style={{ display: "flex", justifyContent: "center", backgroundColor: "#f9f9f9", position: "sticky", bottom: 0, width: "100%", padding: 10, boxShadow: "0px 0px 5px 1px #d5d5d5" }}>
				{/* <button className='btn btn-outline-dark mr-2' style={{ padding: "10 16" }}
					onClick={() => {
						try {
							axios.get("/api/stop-app")
						} catch {

						}
						setTimeout(() => {
							let newWindow = window.open('', '_self');
							window.close();
							newWindow.close();
						}, 100)
					}}
				>
					<FontAwesomeIcon icon={faTimes} />
				</button>
				<button className='btn btn-outline-dark mr-2' style={{ padding: "10 16" }}
					onClick={() => {
						localStorage.removeItem("matricule1");
						localStorage.removeItem("matricule2");
						this.props.history.push("/")
					}}
				>
					<FontAwesomeIcon icon={faArrowRightFromBracket} />
				</button>
				<button className='btn btn-outline-dark mr-2' style={{ padding: "10 16" }}
					onClick={() => {
						window.location.reload()
					}}
				>
					<FontAwesomeIcon icon={faRotateLeft} />
				</button> */}
				{this.state.obj && this.state.obj.placement && <button className='btn btn-outline-danger mr-2'
					onClick={() => {
						this.setState({
							intervention: {
								serie: this.state.obj.serie,
								sequence: this.state.obj.cuttingRequest.sequence,
								date: this.state.obj.planningDate,
								shift: this.state.obj.shift,
								partNumberMaterial: this.state.obj.partNumberMaterial,
								partNumberMaterialDescription: this.state.obj.description,
								debutArret: moment().format("YYYY-MM-DD,HH:mm:ss").replace(",", "T"),
								debutIntervention: moment().format("YYYY-MM-DD,HH:mm:ss").replace(",", "T"),
								finIntervention: moment().format("YYYY-MM-DD,HH:mm:ss").replace(",", "T"),
								machine: this.state.postObj ? this.state.postObj.tableCoupe : null
							}
						})
					}}
				>
					<FontAwesomeIcon icon={faTriangleExclamation} /> Intervention
				</button>}

				{/* {this.state.obj.placement && <ButtonGroup variant="text" aria-label="text button group">
					{["Waiting", "In progress", "Complete", "Incomplete"].map(status => {
						return <Button disabled={this.state.obj.statusCoupe === status}
							onClick={() => {
								axios.post("/api/cuttingRequestSerieInfo", {
									...this.state.obj,
									...this.state.postObj,
									statusCoupe: status
								})
									.then(res => {
										this.setState({ obj: { ...res.data } })
									})
									.catch(err => {
										this.setState({ error: err.response.data })
									})
							}}
						>
							{status}
						</Button>
					})}
				</ButtonGroup>} */}

			</div>
		</div>
		)
	}
}
