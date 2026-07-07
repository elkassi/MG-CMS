import { faArrowAltCircleLeft, faCopy, faFloppyDisk } from '@fortawesome/free-regular-svg-icons'
import { faMinus, faPlus, faArrowRightFromBracket, faTimes, faPrint, faCheck, faTriangleExclamation, faRotateLeft, faArrowRotateRight, faScrewdriverWrench, faKeyboard, faTable, faCircleNotch, faExclamationCircle, faExclamationTriangle } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import axios from 'axios'
import { Modal } from 'react-bootstrap'
import moment, { min } from 'moment'
import React, { Component } from 'react'
import logo from '../../assets/images/lear_logo.png'
import "../../styles/Form.scss"
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { defautRouleauOption, departementOption, problemeResoluOption } from '../../metadata'
import Select from "react-select";
import reactSelect from 'react-select'
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

class Form extends Component {

	constructor() {
		super()
		this.state = {
			obj: {},
			rouleauObj: {},
			error: null,
			qn: null,
			showQn: false,
			intervention: null,
			interventionList: [],
			codeErreurList: [],
			codeArretList: [],
			mode: null,
			startloop: false,
			markerLog: [],
			firstCheckConfigList: [],
			firstCheckList: [],
			showFirstCheckModal: false,
			date: moment().add(2, 'hours').add(10, 'minutes').format('YYYY-MM-DD'),
			shift: this.getShift(moment().add(2, 'hours').add(10, 'minutes')),
			showTable: false,
			serieList: [],
			// overlapTotal: 1
			restRouleauMatiere: null,
			restRouleauArr: [],
			loadingSave: false,
			serieRouleauTemp: [],
			productionTable: null,
			defautRouleauList: [],
			listIntervenant: [],
			reftissuProperties: [],
			plsVerification: null,
			laminationPls: null,
			listReferenceAirbag: [],
			usingSplice: false,
			machineSchedule: null,
			machineScheduleLoading: false
		}
	}

	componentDidMount() {

		axios.get("/api/query/reftissu-airbag")
			.then(res => {
				this.setState({ listReferenceAirbag: res.data })
			})
		this.serieInput.focus()
		axios.get("/api/codeArret/list")
			.then(res => {
				this.setState({ codeArretList: res.data })
			})

		axios.get("/api/defautRouleau/list")
			.then(res => {
				this.setState({ defautRouleauList: res.data })
			})
			.catch(err => {
				this.setState({ error: err.response.data })
			})

	}

	componentDidUpdate(prevProps, prevState) {
		if (this.state.startloop == true && prevState.startloop == false) {
			this.startLoop();
		} else if (this.state.startloop == false && prevState.startloop == true) {
			this.stopLoop();
		}
	}

	startLoop() {
		if (this.state.obj.placement) {

			axios.get(`/api/spliceMarkerLog/serie/${this.state.obj.serie}`)
				.then(res => {
					if (res.data != null) {
						this.setState({ usingSplice: true })
					}
					if (res.data.fabricTypeSpliceLength > 0) {
						this.setState({
							markerLog: [res.data]
						})
					}
				})
		}
		this.loopInterval = setInterval(() => {
			// Your loop code goes here
			if (this.state.obj.placement) {
				axios.get(`/api/spliceMarkerLog/serie/${this.state.obj.serie}`)
					.then(res => {
						if (this.state.usingSplice !== true && res.data != null) {
							this.setState({ usingSplice: true })
						}
						if (res.data.fabricTypeSpliceLength > 0) {
							let mesArr = this.state.markerLog.map(e => e.fabricTypeSpliceLength)
							if (!mesArr.includes(res.data.fabricTypeSpliceLength)) {
								if (this.state.rouleauObj.id == null) {
									let lg = this.convertFloat((res.data.fabricTypeSpliceLength) / 1000)
									if (this.state.markerLog.length > 0) {
										lg = this.convertFloat((res.data.fabricTypeSpliceLength - this.state.markerLog[this.state.markerLog.length - 1].fabricTypeSpliceLength) / 1000)
									}
									if (this.state.rouleauObj.overlap1 == null) {
										this.setState({ markerLog: [...this.state.markerLog, res.data], rouleauObj: { ...this.state.rouleauObj, overlap1: lg } })
									} else if (this.state.rouleauObj.overlap2 == null) {
										this.setState({ markerLog: [...this.state.markerLog, res.data], rouleauObj: { ...this.state.rouleauObj, overlap2: lg } })
									} else if (this.state.rouleauObj.overlap3 == null) {
										this.setState({ markerLog: [...this.state.markerLog, res.data], rouleauObj: { ...this.state.rouleauObj, overlap3: lg } })
									} else if (this.state.rouleauObj.overlap4 == null) {
										this.setState({ markerLog: [...this.state.markerLog, res.data], rouleauObj: { ...this.state.rouleauObj, overlap4: lg } })
									} else if (this.state.rouleauObj.overlap5 == null) {
										this.setState({ markerLog: [...this.state.markerLog, res.data], rouleauObj: { ...this.state.rouleauObj, overlap5: lg } })
									} else if (this.state.rouleauObj.overlap6 == null) {
										this.setState({ markerLog: [...this.state.markerLog, res.data], rouleauObj: { ...this.state.rouleauObj, overlap6: lg } })
									} else if (this.state.rouleauObj.overlap7 == null) {
										this.setState({ markerLog: [...this.state.markerLog, res.data], rouleauObj: { ...this.state.rouleauObj, overlap7: lg } })
									} else if (this.state.rouleauObj.overlap8 == null) {
										this.setState({ markerLog: [...this.state.markerLog, res.data], rouleauObj: { ...this.state.rouleauObj, overlap8: lg } })
									}
								} else {
									this.setState({
										markerLog: [...this.state.markerLog, res.data],
									})
								}
							}
						}
					})
			}
		}, 5000);
	}

	stopLoop() {
		clearInterval(this.loopInterval);
	}




	searchInterventions(serie) {
		axios.get(`/api/intervention/${serie}/matelassage`)
			.then(res => {
				this.setState({ interventionList: res.data })
			})
	}

	renderModalQn = () => {
		return <Modal
			show={this.state.showQn}
			onHide={() => this.setState({ showQn: false })}
			// className=""
			backdropStyle={{ backgroundColor: "rgba(255,255,255,1)" }}
			style={{ width: "100%" }}
			dialogClassName="modal-75w"
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

	printDefaut = () => {
		this.state({ loading: true })
		this.handleSubmit()
		axios.post(`/api/cuttingRequestSerieRouleauInfo/printDefaut`, this.state.rouleauObj)
			.then(res => {
				this.setState({ loadingSave: false, rouleauObj: {} })
			})
			.catch(err => {
				console.log(err)
			})

	}

	handleSubmit = () => {
		let error = []
		let rouleauObj = { ...this.state.rouleauObj }
		this.setState({ loadingSave: true })

		let arr = [
			...this.state.obj.cuttingRequestSerieRouleaus.filter(e => rouleauObj.id == null || e.id != rouleauObj.id),
			{ ...rouleauObj }
		]
		if (rouleauObj.confirmReftissu == null || rouleauObj.confirmReftissu.trim().length == 0) {
			error.push("il faut remplir la Référence tissu pour la confirmation")
		} else if (rouleauObj.confirmReftissu !== this.state.obj.partNumberMaterial && rouleauObj.confirmReftissu.toUpperCase().trim() !== "P" + this.state.obj.partNumberMaterial.toUpperCase().trim()) {
			error.push("Reftissu doit être " + this.state.obj.partNumberMaterial)
		}
		if (rouleauObj.idRouleau == null || rouleauObj.idRouleau.trim().length == 0) {
			error.push("il faut remplir l'id du rouleau")
		} else if (!rouleauObj.idRouleau.startsWith("S")) {
			error.push("l'id du rouleau doit commencer par S")
		} else if (rouleauObj.idRouleau.length > 20) {
			error.push("l'id du rouleau doit être inférieur à 20 caractères")
		} else {
			if (!rouleauObj.id) {
				let idRouleau = arr.filter(e => e.idRouleau === rouleauObj.idRouleau)
				if (idRouleau.length > 1) {
					error.push("l'id du rouleau " + rouleauObj.idRouleau + " est déjà utilisé")
				}
			}
		}
		let notSameLotAirbag = false
		if (this.state.obj.partNumberMaterial && this.state.listReferenceAirbag.includes(this.state.obj.partNumberMaterial) && this.state.rouleauObj && this.state.obj.cuttingRequestSerieRouleaus && this.state.obj.cuttingRequestSerieRouleaus.length > 0) {
			let lastLot = this.state.obj.cuttingRequestSerieRouleaus[this.state.obj.cuttingRequestSerieRouleaus.length - 1].lotFrs
			if (lastLot && this.state.rouleauObj.lotFrs && this.state.rouleauObj.lotFrs.length > 0 && lastLot !== this.state.rouleauObj.lotFrs && "H" + lastLot !== this.state.rouleauObj.lotFrs) {
				notSameLotAirbag = true
			}
		}
		if (rouleauObj.lotFrs == null || rouleauObj.lotFrs.trim().length == 0) {
			error.push("il faut remplir lot frs")
		} else if (!rouleauObj.lotFrs.startsWith("H")) {
			error.push("lot frs doit commencer par H")
		} else if (notSameLotAirbag) {
			error.push("le lot frs doit être le même pour tous les rouleaux airbag")
		}
		if (rouleauObj.metrage == null || rouleauObj.metrage == 0) {
			error.push("il faut remplir le métrage du rouleau")
		} else if (rouleauObj.metrage > 200) {
			error.push("le métrage doit être inférieur à 200m")
		}
		if (rouleauObj.laize == null || rouleauObj.laize == 0) {
			error.push("il faut remplir la laize")
		} else if (rouleauObj.laize < this.state.obj.laize) {
			error.push("laize doit être supérieur à la laize demandé")
		} else if (rouleauObj.laize > 2) {
			error.push("laize doit être inférieur à 2m")
		}

		if (rouleauObj.nbrCouche == null || rouleauObj.nbrCouche == 0) {
			if (rouleauObj.longueurCoucheOverlap != null && rouleauObj.longueurCoucheOverlap != 0) {
				rouleauObj.nbrCouche = 0
			} else {
				error.push("il faut remplir le nombre de couche")
			}
		} else if (rouleauObj.nbrCouche > this.state.obj.nbrCouche) {
			error.push("le nombre de couche ne doit pas dépasser le nombre de couche demandé")
		}




		if (error.length == 0) {
			const { user } = this.props.security;

			let totalOverlap = this.convertFloat(arr.map(e => (e.longueurCoucheOverlap ? this.convertFloat(e.longueurCoucheOverlap) : 0)).reduce((a, b) => (a || 0) + (b || 0)).toFixed(3))
			let overlapCouchs = Math.floor(totalOverlap / this.state.obj.longueur)
			let totalUsage = 0;
			if (rouleauObj) {
				if (this.state.obj.longueur != null && rouleauObj.nbrCouche != null) {
					totalUsage += this.convertFloat(this.state.obj.longueur) * rouleauObj.nbrCouche
				}
				if (rouleauObj.longueurCoucheOverlap != null) {
					totalUsage += this.convertFloat(rouleauObj.longueurCoucheOverlap)
				}
				if (rouleauObj.nonUtitlse != null) {
					totalUsage += this.convertFloat(rouleauObj.nonUtitlse)
				}
				if (rouleauObj.defaut != null) {
					totalUsage += this.convertFloat(rouleauObj.defaut)
				}
				for (let i = 1; i <= 8; i++) {
					if (rouleauObj["overlap" + i] != null) {
						totalUsage += this.convertFloat(rouleauObj["overlap" + i])
					}
				}
			}
			rouleauObj.totalUsage = this.convertFloat(totalUsage)
			let retourMagasin = null
			if (this.state.obj.cuttingRequestSerieRouleaus) {
				if (this.state.obj.cuttingRequestSerieRouleaus.length > 0) {
					retourMagasin = arr.map(e => e.retour).reduce((a, b) => (a || 0) + (b || 0))
				} else {
					retourMagasin = 0
				}
			}


			if (rouleauObj.confirmRetour === true) {
				rouleauObj.retour = this.convertFloat(rouleauObj.metrage - totalUsage)
				rouleauObj.excess = 0
			} else {
				rouleauObj.retour = 0
				rouleauObj.excess = this.convertFloat(totalUsage - rouleauObj.metrage)
			}
			let nbrCoucheTotal = null
			if (rouleauObj.nbrCouche > 0) {
				if (this.state.obj.cuttingRequestSerieRouleaus.length > 0) {
					nbrCoucheTotal = arr.map(e => e.nbrCouche).reduce((a, b) => (a || 0) + (b || 0))
				} else {
					nbrCoucheTotal = 0
				}
			}


			let arrRetour = this.state.obj.cuttingRequestSerieRouleaus.filter(e => e.confirmRetour === true)

			if (arrRetour.length > 0 && rouleauObj.confirmRetour && (rouleauObj.id == null || rouleauObj.id != arrRetour[0].id)) {
				error.push("vous avez déjà un retour dans le rouleau " + arrRetour[0].idRouleau)
				this.setState({ error, loading: false, loadingSave: false })
			} else if ((nbrCoucheTotal + overlapCouchs) > this.state.obj.nbrCouche) {
				error.push("vous avez dépasser le nbr de couche total " + this.state.obj.nbrCouche)
				this.setState({ error, loading: false, loadingSave: false })
			} else {
				axios.post("/api/cuttingRequestSerieInfo", {
					...this.state.obj,
					tableMatelassage: this.state.obj.tableMatelassage,
					cuttingRequestSerieRouleaus: [
						...this.state.obj.cuttingRequestSerieRouleaus.filter(e => rouleauObj.id == null || e.id != rouleauObj.id),
						{ ...rouleauObj }
					],
					statusMatelassage: 'In progress',
				})
					.then(res => {
						try {
							// if (this.state.rouleauObj.defaut > 0 && this.state.rouleauObj.defautCode && this.state.rouleauObj.defautCode.length > 0 && this.state.rouleauObj.confirmRetour === false) {
							// 	let indRouleau = res.data.cuttingRequestSerieRouleaus.findIndex(e => e.id == this.state.rouleauObj.id)
							// 	axios.post("/api/cuttingRequestSerieInfo/printDefaut/" + res.data.cuttingRequestSerieRouleaus[indRouleau].id,
							// 		res.data)
							// 		.then(res => {
							// 			console.log(res)
							// 		})
							// 		.catch(err => {
							// 			console.log(err)
							// 		})
							// }
							let idRouleau = this.state.rouleauObj.idRouleau.replace("S", "").replace("s", "")
							let changedRouleau = res.data.cuttingRequestSerieRouleaus.find(e => e.idRouleau == idRouleau)
							if (changedRouleau != null) {
								axios.post("/api/cuttingRequestSerieRouleauHistory/saveSerieRouleau?serie=" + this.state.obj.serie + "&matricule=" + user.firstName+ " " + user.lastName + " : " + user.matricule , { ...changedRouleau })
							}
						} catch (e) {
							console.log(e)
						}
						let rest = this.convertFloat(totalOverlap % this.state.obj.longueur)
						if (rest === this.state.obj.longueur) {
							rest = 0
						}
						console.log(rest)
						if (rest > 0 && rest < this.state.obj.longueur) {
							this.setState({ obj: res.data, loading: false, loadingSave: false, rouleauObj: { longueurCoucheOverlap: this.convertFloat((this.state.obj.longueur - rest).toFixed(3)) }, error: null })
						} else {
							this.setState({ obj: res.data, loading: false, loadingSave: false, rouleauObj: {}, error: null })
						}
						this.confirmReftissuInput.focus()
					})
					.catch(err => {
						this.setState({ error: err.response.data.errors, loadingSave: false, loading: false })
					})

			}


		} else {
			this.setState({ error, loading: false, loadingSave: false })
		}

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

	renderModalIntervention = () => {
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
					<div className='row my-2'>
						<label className='col-2 col-form-label text-right'>Début d'arrêt</label>
						<div className='col-3 p-0'>
							<input type="datetime-local" name="debutArret" step="1" min="2023-01-01T00:00:00" max="2100-12-31T23:59:59" style={{ width: 220, color: "black" }}
								value={this.state.intervention.debutArret ? this.state.intervention.debutArret : undefined}
								disabled={this.state.intervention.autoCoupe}
								onChange={e => {
									if (e.target.value === null || e.target.value === "") {
										this.setState({ intervention: { ...this.state.intervention, debutArret: e.target.value } })
									} else {
										this.setState({ intervention: { ...this.state.intervention, debutArret: e.target.value } })
									}
								}}
							/>
						</div>
					</div>
					<div className='row my-2'>
						<label className='col-2 col-form-label text-right' style={{ whiteSpace: "nowrap" }}>Début d'intervention</label>
						<div className='col-4 p-0'>
							<input type="datetime-local" name="debutIntervention" step="1" min="2023-01-01T00:00:00" max="2100-12-31T23:59:59" style={{ width: 220, color: "black" }}
								value={this.state.intervention.debutIntervention ? this.state.intervention.debutIntervention : undefined}
								disabled={this.state.intervention.autoCoupe}
								onChange={e => {
									if (e.target.value === null || e.target.value === "") {
										this.setState({ intervention: { ...this.state.intervention, debutIntervention: e.target.value } })
									} else {
										this.setState({ intervention: { ...this.state.intervention, debutIntervention: e.target.value } })
									}
								}}
							/>
						</div>
						<label className='col-1 col-form-label text-right pl-0' style={{ whiteSpace: "nowrap" }}>Fin d'intervention</label>
						<div className='col-4 p-0'>
							<input type="datetime-local" name="finIntervention" step="1" min="2023-01-01T00:00:00" max="2100-12-31T23:59:59" style={{ width: 220, color: "black" }}
								value={this.state.intervention.finIntervention ? this.state.intervention.finIntervention : undefined}
								disabled={this.state.intervention.autoCoupe}
								onChange={e => {
									if (e.target.value === null || e.target.value === "") {
										this.setState({ intervention: { ...this.state.intervention, finIntervention: e.target.value } })
									} else {
										this.setState({ intervention: { ...this.state.intervention, finIntervention: e.target.value } })
									}
								}}
							/>
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


									axios.get("/api/optitime/listNames?sec=" + option.num)
										.then((response) => {
											this.setState({
												listIntervenant: response.data,
												intervention: {
													...this.state.intervention,
													departement: option.value,
												}
											})
										})
										.catch((error) => {
											this.setState({
												listIntervenant: [],
												intervention: {
													...this.state.intervention,
													departement: option.value,
												}
											})

											console.log(error);
										});
								} else {
									this.setState({
										intervention: {
											...this.state.intervention,
											departement: null,
										},
										listIntervenant: []
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
							options={this.state.intervention.departement ? this.state.codeArretList.filter(e => e.departement === this.state.intervention.departement).map(codeArret => ({ label: codeArret.code + " " + codeArret.motifArret, value: codeArret })) : []} // departementOption
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
						{/* <label className='col-2 col-form-label text-right'>Motif d'arrêt</label>
						<div className='col-3 ' style={{ padding: "7 0 0" }}>
							{this.state.intervention.codeArret && this.state.intervention.codeArret.motifArret}
						</div> */}

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


					<div className='row my-2'>
						<label className='col-2 col-form-label text-right'>Matricule d'émetteur</label>
						{/* <input className={`form-control input-sm`}
								value={this.state.intervention.matriculeEmetteur || ''}
								onChange={event => this.setState({ intervention: { ...this.state.intervention, matriculeEmetteur: event.target.value } })}
							/> */}
						<Select classNamePrefix="rs" className='p-0 input-sm col-3'
							isClearable={true} placeholder={"matriculeEmetteur..."}
							value={this.state.intervention.matriculeEmetteur ? { label: this.state.intervention.matriculeEmetteur, value: this.state.intervention.matriculeEmetteur } : null}
							options={[this.state.obj.matelasseur1, this.state.obj.matelasseur2].map(e => { return { label: e, value: e } })}
							// menuPosition={'absolute'} // 'fixed', 'absolute'
							menuPlacement={"top"} // auto, bottom, top
							onChange={(option) => {
								if (option) {
									this.setState({
										intervention: {
											...this.state.intervention,
											matriculeEmetteur: option.value,
										}
									})
								} else {
									this.setState({
										intervention: {
											...this.state.intervention,
											matriculeEmetteur: null,
										}
									})
								}
							}}
						/>
						<label className='col-2 col-form-label text-right'>Matricule de responsable</label>
						<Select classNamePrefix="rs" className='p-0 input-sm col-3'
							isClearable={true} placeholder={"matriculeResponsable..."}
							value={this.state.intervention.matriculeResponsable ? { label: this.state.intervention.matriculeResponsable, value: this.state.intervention.matriculeResponsable } : null}
							options={this.state.listIntervenant.map(e => { return { label: e, value: e } })}
							// menuPosition={'absolute'} // 'fixed', 'absolute'
							menuPlacement={"top"} // auto, bottom, top
							onChange={(option) => {
								if (option) {
									this.setState({
										intervention: {
											...this.state.intervention,
											matriculeResponsable: option.value,
										}
									})
								} else {
									this.setState({
										intervention: {
											...this.state.intervention,
											matriculeResponsable: null,
										}
									})
								}
							}}
						/>
					</div>
					<div>
						<button
							className='btn btn-sm btn-outline-dark ml-2 mb-2' style={{ padding: "4 40" }}
							onClick={() => {
								this.searchInterventions(this.state.obj.serie)
							}}
						><FontAwesomeIcon icon={faArrowRotateRight} /> Refresh</button>
					</div>
					<div className='table-responsive'>
						<table className='table table-bordered table-cells-sm mb-0' style={{ fontSize: 12 }}>
							<thead className='header-table-black'>
								<tr>
									<th>Date de creation</th>
									<th>Début d'arrêt</th>
									<th>Début d'intervention</th>
									<th>Fin d'intervention</th>
									<th>Code d'arrêt</th>
									<th>Motif d'arrêt</th>
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
													machine: this.state.obj.tableMatelassage
												}
											})
										} else {
											this.setState({ intervention: { ...elem } })
										}
									}}
									style={this.state.intervention.id === elem.id ? { backgroundColor: "lightgray" } : {}}
								>
									<td>{elem.createdAt}</td>
									<td>{elem.debutArret?.replace("T", " ")}</td>
									<td>{elem.debutIntervention?.replace("T", " ")}</td>
									<td>{elem.finIntervention?.replace("T", " ")}</td>
									<td>{elem.codeArret?.code}</td>
									<td>{elem.codeArret?.motifArret}</td>
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
					<button className='btn btn-danger' onClick={() => {
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
										machine: this.state.obj.tableMatelassage,
										type: "matelassage"
									}
								})
								this.searchInterventions(this.state.obj.serie)
							})
					}}>{this.state.intervention.id ? "Modification " + this.state.intervention.id : "Enregistrer"}</button>
					<button className='btn btn-link' onClick={() => { this.setState({ intervention: null }) }}>Retour au formulaire</button>
				</div>
			</div>}
		</Modal>
	}

	renderRestRouleauModal = () => {
		//render table of rest rouleau matiere
		/*
			private String lotFrs;
			private String idRouleau;
			private Double laize;
			private Double retour;
			private String serie;
			private LocalDateTime createdAt;
			private String tableMatessage;

		*/
		return <Modal
			show={this.state.restRouleauMatiere !== null}
			onHide={() => this.setState({ restRouleauMatiere: null })}
			// className=""
			dialogClassName="modal-75w"
			centered
		>
			{this.state.restRouleauMatiere && <div style={{
				maxHeight: "calc(95vh - 30px)",
				overflow: "auto",
				padding: "8 15 0",
				display: 'flex', flexDirection: "column"
			}}>
				<div className="row">
					<div className="col-12">
						<h2 className='text-center'>Rest rouleau matiere : {this.state.restRouleauMatiere}</h2>
					</div>
				</div>
				<div>
					<table className="table table-bordered">
						<thead>
							<tr>
								<th>Lot fournisseur</th>
								<th>Id rouleau</th>
								<th>Laize</th>
								<th>Retour</th>
								<th>Serie</th>
								<th>Date</th>
								<th>Table matelassage</th>
							</tr>
						</thead>
						<tbody>
							{this.state.restRouleauArr === null ? <tr><td colSpan="7">Chargement...</td></tr> :
								this.state.restRouleauArr.length === 0 ? <tr><td colSpan="7">Aucun résultat</td></tr> :
									this.state.restRouleauArr
										.filter(elem => elem.retour >= 1)
										.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
										.map((elem, index) => <tr key={index}>
											<td className='not-selectable'>{elem.lotFrs}</td>
											<td className='not-selectable'>{elem.idRouleau}</td>
											<td className='not-selectable'>{elem.laize}</td>
											<td className='not-selectable'>{elem.retour}</td>
											<td className='not-selectable'>{elem.serie}</td>
											<td className='not-selectable'>{elem.createdAt}</td>
											<td className='not-selectable'>{elem.tableMatelassage}</td>
										</tr>)}
						</tbody>
					</table>
					<div className="row">
						<div className="col-12">
							<h2 className='text-center'>En cours de matelassage : {this.state.restRouleauMatiere}</h2>
						</div>
					</div>
					{
						/*
						private String tableMatelassage;
		private String idRouleau;
		private String lot;
		private String reftissu;
		private LocalDateTime date;
		private Double quantiteInitiale;
		private Double estimationRest;
						*/
					}
					<table className="table table-bordered">
						<thead>
							<tr>
								<th>Table matelassage</th>
								<th>Id rouleau</th>
								<th>Lot</th>
								<th>Ref tissu</th>
								<th>Date</th>
								<th>Quantité initiale</th>
								<th>Estimation restante</th>
							</tr>
						</thead>
						<tbody>
							{this.state.serieRouleauTemp === null ? <tr><td colSpan="7">Chargement...</td></tr> :
								this.state.serieRouleauTemp.length === 0 ? <tr><td colSpan="7">Aucun résultat</td></tr> :
									this.state.serieRouleauTemp
										.sort((a, b) => new Date(b.date) - new Date(a.date))
										.map((elem, index) => <tr key={index}>
											<td className='not-selectable'>{elem.tableMatelassage}</td>
											<td className='not-selectable'>{elem.idRouleau}</td>
											<td className='not-selectable'>{elem.lot}</td>
											<td className='not-selectable'>{elem.reftissu}</td>
											<td className='not-selectable'>{elem.date}</td>
											<td className='not-selectable'>{elem.quantiteInitiale}</td>
											<td className='not-selectable'>{elem.estimationRest}</td>
										</tr>)}
						</tbody>
					</table>
				</div>
				<hr />
				<div style={{ display: "flex", flexDirection: 'row-reverse', backgroundColor: "white", position: "sticky", bottom: 0 }} className="p-2">
					<button className='btn btn-link' onClick={() => { this.setState({ restRouleauMatiere: null }) }}>Retour au formulaire</button>
				</div>
			</div>}
		</Modal>
	}

	convertFloat = (float) => {
		//check if the float is a number
		if (!float || isNaN(float)) {
			return float
		}
		return parseFloat(parseFloat(float).toFixed(3))
	}

	getShift(date) {
		let hour = date.hour()
		if (hour >= 0 && hour < 8) {
			return 1
		} else if (hour >= 8 && hour < 16) {
			return 2
		} else {
			return 3
		}
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
				<h4 className="text-center mt-2">FICHE DE MATELASSAGE / COUPE</h4>
			</div>
			<div className="col-3 border border-dark">
				<p className="text-center mt-2">FR PR 02</p>
			</div>
		</div>
	}


	fetchMachineSchedule(machineNom) {
		if (!machineNom) return;
		this.setState({ machineScheduleLoading: true });
		axios.get(`/api/ordonnancement/machineSchedule/${machineNom}`)
			.then(res => {
				this.setState({ machineSchedule: res.data, machineScheduleLoading: false });
			})
			.catch(() => {
				this.setState({ machineScheduleLoading: false });
			});
	}

	renderMachineSchedule() {
		const { machineSchedule, machineScheduleLoading } = this.state;
		if (machineScheduleLoading) {
			return (
				<div style={{ padding: '15px', textAlign: 'center', color: '#888', fontSize: '0.9rem' }}>
					Chargement du planning...
				</div>
			);
		}
		if (!machineSchedule) return null;

		const { lastFinish, lastFinishIsEstimated, nextWaiting, waitingCount, machineNom } = machineSchedule;

		return (
			<div style={{
				background: '#f8f9fa',
				borderTop: '2px solid #dee2e6',
				padding: '15px 20px',
				fontSize: '0.9rem',
			}}>
				<div style={{ display: 'flex', gap: 20, flexWrap: 'wrap' }}>
					{/* Last finish time */}
					<div style={{ flex: '1 1 250px' }}>
						<h6 style={{ margin: '0 0 8px', fontWeight: 700, color: '#495057', fontSize: '0.95rem' }}>
							🕐 Disponibilité machine {machineNom}
						</h6>
						{lastFinish ? (
							<div style={{
								background: lastFinishIsEstimated ? '#fff3cd' : '#d4edda',
								border: `1px solid ${lastFinishIsEstimated ? '#ffc107' : '#28a745'}`,
								borderRadius: 6,
								padding: '10px 14px',
							}}>
								<div style={{ fontWeight: 600, color: lastFinishIsEstimated ? '#856404' : '#155724' }}>
									{lastFinishIsEstimated ? '⏳ Fin estimée' : '✅ Fin confirmée'}
								</div>
								<div style={{ fontSize: '1.1rem', fontWeight: 700, marginTop: 4 }}>
									{moment(lastFinish).format('DD/MM HH:mm')}
								</div>
								<div style={{ fontSize: '0.8rem', color: '#6c757d', marginTop: 2 }}>
									{moment(lastFinish).fromNow()}
								</div>
							</div>
						) : (
							<div style={{
								background: '#e9ecef',
								border: '1px solid #ced4da',
								borderRadius: 6,
								padding: '10px 14px',
								color: '#6c757d',
							}}>
								Aucune série en cours sur cette machine
							</div>
						)}
					</div>

					{/* Next waiting series */}
					<div style={{ flex: '2 1 400px' }}>
						<h6 style={{ margin: '0 0 8px', fontWeight: 700, color: '#495057', fontSize: '0.95rem' }}>
							📋 Prochaines séries en attente ({waitingCount || 0})
						</h6>
						{nextWaiting && nextWaiting.length > 0 ? (
							<div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
								{nextWaiting.map((s, idx) => (
									<div key={s.serie} style={{
										display: 'flex',
										alignItems: 'center',
										gap: 12,
										background: '#fff',
										border: '1px solid #dee2e6',
										borderRadius: 6,
										padding: '8px 12px',
									}}>
										<span style={{
											background: idx === 0 ? '#dc3545' : idx === 1 ? '#fd7e14' : '#ffc107',
											color: '#fff',
											borderRadius: '50%',
											width: 24,
											height: 24,
											display: 'flex',
											alignItems: 'center',
											justifyContent: 'center',
											fontWeight: 700,
											fontSize: '0.8rem',
										}}>{idx + 1}</span>
										<div style={{ flex: 1 }}>
											<div style={{ fontWeight: 600 }}>Série {s.serie} — {s.sequence}</div>
											<div style={{ fontSize: '0.8rem', color: '#6c757d' }}>
												{s.description || s.partNumberMaterial || '—'}
											</div>
										</div>
										{s.dueDate && (
											<span style={{
												fontSize: '0.78rem',
												padding: '2px 8px',
												borderRadius: 10,
												background: '#e9ecef',
												color: '#495057',
												fontWeight: 600,
												whiteSpace: 'nowrap',
											}}>
												📅 {s.dueDate} {s.dueShift ? `(Équipe ${s.dueShift})` : ''}
											</span>
										)}
									</div>
								))}
							</div>
						) : (
							<div style={{
								background: '#e9ecef',
								border: '1px solid #ced4da',
								borderRadius: 6,
								padding: '10px 14px',
								color: '#6c757d',
							}}>
								Aucune série en attente sur cette machine
							</div>
						)}
					</div>
				</div>
			</div>
		);
	}

	render() {
		let arr = []
		for (let i = 1; i <= 10; i++) {
			arr.push(<input key={"overlap-" + i} type="number"
				style={{ width: 120, padding: "2 8" }}
				ref={input => { this["overlap" + i + "Input"] = input }}
				value={this.state.rouleauObj["overlap" + i] || ""}
				onChange={(event) => {
					if (/^\d*\.?\d*$/.test(event.target.value)) {
						this.setState({ rouleauObj: { ...this.state.rouleauObj, ["overlap" + i]: event.target.value.trim() !== "" ? event.target.value : null } })
					}
				}}
				onKeyUp={(e) => {
					if (e.key === "Enter" && this["overlap" + (i + 1) + "Input"]) {
						this["overlap" + (i + 1) + "Input"].focus()
					}
				}}
			/>)
		}

		let totalUsage = 0;
		if (this.state.rouleauObj) {
			if (this.state.obj.longueur != null && this.state.rouleauObj.nbrCouche != null) {
				totalUsage += this.convertFloat(this.state.obj.longueur) * this.state.rouleauObj.nbrCouche
			}
			if (this.state.rouleauObj.longueurCoucheOverlap != null) {
				totalUsage += this.convertFloat(this.state.rouleauObj.longueurCoucheOverlap)
			}
			if (this.state.rouleauObj.nonUtitlse != null) {
				totalUsage += this.convertFloat(this.state.rouleauObj.nonUtitlse)
			}
			if (this.state.rouleauObj.defaut != null) {
				totalUsage += this.convertFloat(this.state.rouleauObj.defaut)
			}
			for (let i = 1; i <= 8; i++) {
				if (this.state.rouleauObj["overlap" + i] != null) {
					totalUsage += this.convertFloat(this.state.rouleauObj["overlap" + i])
				}
			}
		}
		let nbrCoucheTotal = null, totalOverlap = null, overlapCouchs = null, rest = null
		if (this.state.obj.cuttingRequestSerieRouleaus) {
			if (this.state.obj.cuttingRequestSerieRouleaus.length > 0) {
				nbrCoucheTotal = this.state.obj.cuttingRequestSerieRouleaus.map(e => e.nbrCouche).reduce((a, b) => (a || 0) + (b || 0))
				totalOverlap = this.convertFloat(this.state.obj.cuttingRequestSerieRouleaus.map(e => (e.longueurCoucheOverlap ? this.convertFloat(e.longueurCoucheOverlap) : 0)).reduce((a, b) => (a || 0) + (b || 0)).toFixed(3))
				overlapCouchs = Math.floor(totalOverlap / this.state.obj.longueur)
				console.log({ overlapCouchs, totalOverlap, longueur: this.state.obj.longueur })
				nbrCoucheTotal += overlapCouchs;
				rest = this.convertFloat(totalOverlap % this.state.obj.longueur)
				if (rest === this.state.obj.longueur) {
					rest = 0
				}

			} else {
				nbrCoucheTotal = 0
			}
		}

		let retourMagasin = null
		if (this.state.obj.cuttingRequestSerieRouleaus) {
			if (this.state.obj.cuttingRequestSerieRouleaus.length > 0) {
				retourMagasin = this.state.obj.cuttingRequestSerieRouleaus.map(e => e.retour).reduce((a, b) => (a || 0) + (b || 0))
			} else {
				retourMagasin = 0
			}
		}

		let badReftissu = false
		if (this.state.rouleauObj) {
			badReftissu = this.state.rouleauObj.confirmReftissu == null || this.state.rouleauObj.confirmReftissu.length === 0 || (
				// this.state.rouleauObj.confirmReftissu != null && 
				//this.state.rouleauObj.confirmReftissu.length > 0 && 
				this.state.obj.partNumberMaterial &&
				this.state.rouleauObj.confirmReftissu.toUpperCase() !== this.state.obj.partNumberMaterial.toUpperCase()
				&& this.state.rouleauObj.confirmReftissu.toUpperCase() !== ("P" + this.state.obj.partNumberMaterial.toUpperCase())
			) || this.state.loadingIdRouleau
		}

		let notSameLotAirbag = false
		if (this.state.obj.partNumberMaterial && this.state.listReferenceAirbag.includes(this.state.obj.partNumberMaterial) && this.state.rouleauObj && this.state.obj.cuttingRequestSerieRouleaus && this.state.obj.cuttingRequestSerieRouleaus.length > 0) {
			let lastLot = this.state.obj.cuttingRequestSerieRouleaus[this.state.obj.cuttingRequestSerieRouleaus.length - 1].lotFrs
			if (lastLot && this.state.rouleauObj.lotFrs && this.state.rouleauObj.lotFrs.length > 0 && lastLot !== this.state.rouleauObj.lotFrs && "H" + lastLot !== this.state.rouleauObj.lotFrs) {
				notSameLotAirbag = true
			}
		}

		let markerLog = this.state.markerLog && this.state.markerLog.length > 0 && this.state.markerLog[this.state.markerLog.length - 1]

		return (
			<div style={{ minHeight: "100%", display: "flex", flexDirection: "column" }}>
				<div className='p-2'>
					{this.renderHeader()}
					{<div style={{ fontSize: 14 }}>
							<div>
								<div className='d-flex'>
									<div style={{ width: "20%", textAlign: "end" }}>
										Matelassaur 1 :
									</div>
									<div style={{ width: "30%", paddingLeft: "20" }}>
										{this.state.obj.matelasseur1}
									</div>
									<div style={{ width: "20%", textAlign: "end" }}>
										Zone :
									</div>
									<div style={{ width: "30%", paddingLeft: "20" }}>
										{this.state.obj.zoneMatelassage}
									</div>
								</div>
								<div className='d-flex'>
									<div style={{ width: "20%", textAlign: "end" }}>
										Matelassaur 2 :
									</div>
									<div style={{ width: "30%", paddingLeft: "20" }}>
										{this.state.obj.matelasseur2}
									</div>
									<div style={{ width: "20%", textAlign: "end" }}>
										Table :
									</div>
									<div style={{ width: "30%", paddingLeft: "20" }}>
										{this.state.obj.tableMatelassage} ({this.state.obj.zoneMatelassage})
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
													this.setState({ obj: { serie: e.target.value, qn: null, showQn: false, rouleauObj: {} }, startloop: false, markerLog: [] })
												}
											}}
											onKeyUp={(e) => {
												if (e.key === "Enter") {
													this.setState({ obj: { serie: this.state.obj.serie }, qn: null, showQn: false, rouleauObj: {}, reftissuProperties: [], laminationPls: null, plsVerification: null })
													axios.get(`/api/cuttingRequestSerieInfo/${this.state.obj.serie}`)
														.then((res) => {
															if (res.data && this.state.listReferenceAirbag.includes(res.data.partNumberMaterial)) {
																if (this.state.productionTable.autorisationAirbag !== true) {
																	this.setState({ error: "Airbag non autorisé sur cette table" })
																	return
																}
															}
															this.setState({ obj: { ...this.state.obj, ...res.data }, startloop: true })
															this.searchInterventions(res.data.serie)
															axios.get(`/api/qn/reftissu/${this.state.obj.partNumberMaterial}`)
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

															axios.get("/api/reftissuProperty/list?reftissu=" + this.state.obj.partNumberMaterial)
																.then(res => {
																	this.setState({ reftissuProperties: res.data })
																})
																.catch(err => {
																	this.setState({ error: err.response.data })
																})

															axios.get("/api/lamination-pls/filter?sequence=" + this.state.obj.cuttingRequest.sequence + "&partNumberMaterial=" + this.state.obj.partNumberMaterial)
																.then(res => {
																	this.setState({ laminationPls: res.data })
																	axios.get("/api/query/pls-verification?sequence=" + this.state.obj.cuttingRequest.sequence + "&partNumberMaterial=" + this.state.obj.partNumberMaterial)
																		.then(res2 => {
																			this.setState({ plsVerification: res2.data })
																		})
																		.catch(err => {
																			this.setState({ error: err.response.data, plsVerification: null })
																		})

																})
																.catch(err => {
																	axios.get("/api/query/pls-verification?sequence=" + this.state.obj.cuttingRequest.sequence + "&partNumberMaterial=" + this.state.obj.partNumberMaterial)
																		.then(res2 => {
																			this.setState({ plsVerification: res2.data })
																		})
																		.catch(err => {
																			this.setState({ error: err.response.data, plsVerification: null })
																		})
																})


														})
														.catch(err => {
															console.log(err)
															this.setState({ error: err.response.data })
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
									<div style={{ width: "50%" }}>
										<div className='d-flex'>
											<div style={{ width: "40%", textAlign: "end" }}>
												Status Matelassage :
											</div>
											<div style={this.state.obj.statusMatelassage === "Waiting" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#ffafaf" } :
												this.state.obj.statusMatelassage === "In progress" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#f6ff6b" } :
													this.state.obj.statusMatelassage === "Complete" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#7bff6b" } :
														this.state.obj.statusMatelassage === "Incomplete" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#ffc46b" } :
															{ width: "60%", paddingLeft: "70", marginLeft: 20 }}>
												{this.state.obj.statusMatelassage}
											</div>
										</div>
										<div className='d-flex'>
											<div style={{ width: "40%", textAlign: "end" }}>
												Status Coupe :
											</div>
											<div style={this.state.obj.statusCoupe === "Waiting" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#ffafaf" } :
												this.state.obj.statusCoupe === "In progress" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#f6ff6b" } :
													this.state.obj.statusCoupe === "Complete" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#7bff6b" } :
														this.state.obj.statusCoupe === "Incomplete" ? { padding: "0 70", marginLeft: 20, backgroundColor: "#ffc46b" } :
															{ width: "60%", paddingLeft: "70", marginLeft: 20 }}>
												{this.state.obj.statusCoupe}
											</div>
										</div>
									</div>
									<div style={{ width: "50%", textAlign: "center", color: "blue", fontWeight: "bold" }}>
										{this.state.reftissuProperties.map((e, i) => {
											return <div key={i} className='d-flex'>
												<div style={{ width: "50%", textAlign: "end", fontWeight: "bold" }}>
													{e.property} :
												</div>
												<div style={{ width: "50%", paddingLeft: "20", textAlign: "left", fontWeight: "bold" }}>
													{e.value}
												</div>
											</div>
										})}
									</div>
								</div>
							</div>
							{this.state.obj.placement && <div>
								<h4>Information indiquée sur le plan de coupe :</h4>
								<div className='table-responsive'>
									<table className='table table-bordered table-cells-sm' style={{ fontSize: 14 }}>
										<thead className='header-table-black'>
											<tr>
												<th>Placement</th>
												<th>Longueur</th>
												<th>Nbr de Couche</th>
												<th>Laize</th>
												<th>Reftissu</th>
												<th>Description</th>
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
															: {}}>
													{this.state.obj.placement} {markerLog && markerLog.marker && markerLog.marker.toUpperCase() !== this.state.obj.placement.toUpperCase() && <span style={{ color: "red" }}><FontAwesomeIcon icon={faExclamationTriangle} /> {markerLog.marker}</span>}
												</td>
												<td>{this.state.obj.longueur && (this.state.obj.longueur).toFixed(3)}
													{markerLog && markerLog.longueur !== this.state.obj.markerLengthBrut && <span style={{ color: "red" }}><FontAwesomeIcon icon={faExclamationTriangle} /> {(markerLog.markerLengthBrut * 0.001).toFixed(3)}</span>}
												</td>
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
															: {}}>
													{this.state.obj.partNumberMaterial}
													<button
														className='btn btn-sm btn-primary'
														onClick={() => {
															this.setState({ restRouleauMatiere: this.state.obj.partNumberMaterial, restRouleauArr: null, serieRouleauTemp: [] })
															axios.get("/api/cuttingRequestSerieRouleauInfo/rouleauRapport?reftissu=" + this.state.obj.partNumberMaterial)
																.then(res => {
																	let arr = res.data
																	axios.get("/api/serieRouleauTemp/all")
																		.then(resTemp => {

																			axios.get("/api/query/plsRest/" + this.state.obj.partNumberMaterial)
																				.then(res2 => {
																					res2.data.map(e => {
																						let obj = {
																							//remove h or H from the beginning of the string e.lotNr
																							lotFrs: (e.lotNr && e.lotNr.toUpperCase().startsWith("H")) ? e.lotNr.substring(1) : e.lotNr,
																							//remove s or S from the beginning of the string e.labelId
																							idRouleau: (e.labelId && e.labelId.toUpperCase().startsWith("S")) ? e.labelId.substring(1) : e.labelId,
																							retour: e.quantity,
																							serie: e.plsId,
																							createdAt: e.createdAt,
																							tableMatelassage: e.tableName
																						}
																						// that obj.idRouleau exsit in arr and retour of obj is less that of in arr then replace the obj in arr with the new obj
																						// and if obj.idRouleau doesnt exsit in arr then add it to arr
																						let index = arr.findIndex(elem => elem.idRouleau === obj.idRouleau)
																						if (index !== -1) {
																							if (obj.retour < arr[index].retour) {
																								arr[index] = obj
																							}
																						} else {
																							arr.push({ ...obj })
																						}
																					})
																					this.setState({
																						serieRouleauTemp: resTemp.data.filter(elem => elem.reftissu === this.state.obj.partNumberMaterial),
																						restRouleauArr: arr.filter(elem => resTemp.data == null || !resTemp.data.map(e => e.idRouleau).includes(elem.idRouleau))
																					})
																				})
																				.catch(err => {
																					console.log(err)
																					this.setState({
																						serieRouleauTemp: resTemp.data.filter(elem => elem.reftissu === this.state.obj.partNumberMaterial),
																						restRouleauArr: arr.filter(elem => resTemp.data == null || !resTemp.data.map(e => e.idRouleau).includes(elem.idRouleau))
																					})
																				})
																		})
																})
														}}
														style={{ marginLeft: 10 }}
													>
														<FontAwesomeIcon icon={faTable} /> Rest
													</button>
												</td>
												<td>{this.state.obj.description}</td>
											</tr>
										</tbody>
									</table>
								</div>
								{this.state.laminationPls && !this.state.plsVerification && <div style={{ display: "flex", justifyContent: "center", }}>
									<button className='btn btn-sm btn-outline-danger' style={{ fontSize: 20 }} onClick={() => {

										axios.get("/api/query/duplicatePls/" + this.state.laminationPls.plsId + "?sequence=" + this.state.obj.cuttingRequest.sequence + "&partNumberMaterial=" + this.state.obj.partNumberMaterial + "&serie=" + this.state.obj.serie)
											.then(res => {
												this.setState({ plsVerification: res.data })
											})
									}}>
										<FontAwesomeIcon icon={faCopy} /> Dupliquer la demande {this.state.laminationPls.plsId} pour la lamination
									</button>
								</div>}
								{this.state.plsVerification && <div style={{ fontSize: 20, textAlign: "center" }}>
									Lamination PLS : {this.state.plsVerification}
								</div>}
								<h4>OK Démarrage Matlassage :</h4>
								{(this.state.obj.longueur <= 0.7 || this.state.usingSplice)
									? <div style={this.state.rouleauObj.id ? { padding: "10", backgroundColor: "#c0c6f9", borderRadius: "5" } : { padding: "10", backgroundColor: "#f3f3f3", borderRadius: "5" }}>
										<div className='d-flex flex-wrap'>
											<div style={{ width: 140, padding: "0 10" }}>
												Reftissu<br />
												<input style={(
													this.state.rouleauObj.confirmReftissu != null
													&& this.state.rouleauObj.confirmReftissu.length > 0
													&& this.state.rouleauObj.confirmReftissu.toUpperCase() !== this.state.obj.partNumberMaterial.toUpperCase()
													&& this.state.rouleauObj.confirmReftissu.toUpperCase() !== ("P" + this.state.obj.partNumberMaterial.toUpperCase())
												)
													? { width: "100%", padding: "2 8", backgroundColor: "#ff9c9c" }
													: { width: "100%", padding: "2 8" }}
													ref={input => { this.confirmReftissuInput = input }}
													value={this.state.rouleauObj.confirmReftissu || ""}
													onChange={(event) => {
														if (/^[A-Za-z0-9-]*$/.test(event.target.value)) {
															this.setState({ rouleauObj: { ...this.state.rouleauObj, confirmReftissu: event.target.value.toUpperCase().trim() } })
														}
													}}
													onKeyUp={(e) => {
														if (e.key === "Enter") {
															this.idRouleauInput.focus()
														}
													}}
												/>
											</div>
											<div style={{ width: 140, padding: "0 10" }}>
												N° Rouleau<br />
												<input style={{ width: "100%", padding: "2 8" }} disabled={badReftissu}
													ref={input => { this.idRouleauInput = input }}
													value={this.state.rouleauObj.idRouleau || ""}
													onChange={(event) => {
														if (/^[A-Za-z0-9/]*$/.test(event.target.value)) {
															this.setState({ rouleauObj: { ...this.state.rouleauObj, idRouleau: event.target.value.toUpperCase().trim() } })
														}
													}}
													onKeyUp={(e) => {
														if (e.key === "Enter") {
															let idRouleau = this.state.rouleauObj.idRouleau.replace("S", "")
															if (this.state.obj.cuttingRequestSerieRouleaus.map(e => e.idRouleau).includes(idRouleau)) {
																this.setState({ error: ["Id Rouleau " + idRouleau + "est déjà utilisé dans cette serie"] })
																this.idRouleauInput.select()
															} else {
																this.setState({ loadingIdRouleau: true })
																axios.get(`/api/cuttingRequestSerieRouleauInfo/idRouleau?idRouleau=${idRouleau}`)
																	.then(res => {
																		if (res.data && res.data.lotFrs) {
																			this.setState({
																				rouleauObj: {
																					...this.state.rouleauObj,
																					idRouleau: "S" + idRouleau,
																					lotFrs: "H" + res.data.lotFrs,
																					metrage: res.data.retour,
																					laize: res.data.laize
																				},
																				loadingIdRouleau: false
																			})
																			this.laizeInput.focus()
																			this.laizeInput.select()

																		} else {
																			this.setState({
																				loadingIdRouleau: false,
																			})
																			this.lotFrsInput.focus()
																			this.lotFrsInput.select()
																		}
																		axios.post(`/api/serieRouleauTemp`, {
																			tableMatelassage: this.state.obj.tableMatelassage,
																			idRouleau: idRouleau,
																			lot: res.data ? res.data.lotFrs : null,
																			reftissu: this.state.obj.partNumberMaterial,
																			quantiteInitiale: res.data.retour,
																			estimationRest: nbrCoucheTotal != null
																				? (res.data.retour
																					? this.convertFloat(res.data.retour + ((nbrCoucheTotal - this.state.obj.nbrCouche) * this.state.obj.longueur))
																					: this.convertFloat((nbrCoucheTotal - this.state.obj.nbrCouche) * this.state.obj.longueur)
																				) : null
																		})


																	})
																	.catch(err => {
																		this.setState({
																			error: err.response.data,
																			loadingIdRouleau: false,
																			rouleauObj: {
																				...this.state.rouleauObj,
																				idRouleau: "",
																			},
																		})
																		this.idRouleauInput.focus()
																	})

															}
														}
													}}
												/>
											</div>
											<div style={{ width: 140, padding: "0 10" }}>
												Lot<br />
												<input style={notSameLotAirbag ? { width: "100%", padding: "2 8", backgroundColor: "ff9c9c" } : { width: "100%", padding: "2 8" }} disabled={badReftissu}
													ref={input => { this.lotFrsInput = input }}
													value={this.state.rouleauObj.lotFrs || ""}
													onChange={(event) => {
														if (/^[A-Za-z0-9/]*$/.test(event.target.value)) {
															this.setState({ rouleauObj: { ...this.state.rouleauObj, lotFrs: event.target.value.toUpperCase().trim() } })
														}
													}}
													onKeyUp={(e) => {
														if (e.key === "Enter") {
															this.metrageInput.focus()
														}
													}}
												/>
											</div>
											<div style={{ width: 140, padding: "0 10" }}>
												Metrage<br />
												<input style={{ width: "100%", padding: "2 8" }} disabled={badReftissu}
													value={this.state.rouleauObj.metrage || ""}
													onChange={(event) => {
														if (/^\d*\.?\d*$/.test(event.target.value)) {
															this.setState({ rouleauObj: { ...this.state.rouleauObj, metrage: event.target.value.trim() !== "" ? event.target.value : null } })
														}
													}}
													onKeyUp={(e) => {
														if (e.key === "Enter") {
															this.laizeInput.focus()
														}
													}}
													ref={input => { this.metrageInput = input }}
												/>
											</div>
											<div style={{ width: 140, padding: "0 10" }}>
												Laize(+2/0 cm)<br />
												<input style={(this.state.obj.laize > this.state.rouleauObj.laize || this.state.obj.laize + 0.02 < this.state.rouleauObj.laize) ? { width: "100%", padding: "2 8", backgroundColor: "#ff9c9c" } : { width: "100%", padding: "2 8" }}
													value={this.state.rouleauObj.laize || ""} type="number" disabled={badReftissu}
													onChange={(event) => {
														if (/^\d*\.?\d*$/.test(event.target.value)) {
															this.setState({ rouleauObj: { ...this.state.rouleauObj, laize: event.target.value.trim() !== "" ? event.target.value : null } })
														}
													}}
													onKeyUp={(e) => {
														if (e.key === "Enter") {
															this.nbrCoucheInput.focus()
														}
													}}
													ref={input => { this.laizeInput = input }}
												/>
											</div>
											<div style={{ width: 140, padding: "0 10" }}>
												Nbr Couche<br />
												<input style={(this.state.rouleauObj.id == null && this.state.rouleauObj.nbrCouche > Math.min(this.state.obj.nbrCouche - nbrCoucheTotal)) ? { width: "100%", padding: "2 8", backgroundColor: "#ff9c9c" } : { width: "100%", padding: "2 8" }}
													disabled={(this.state.rouleauObj.id == null && this.state.obj.nbrCouche <= nbrCoucheTotal) || badReftissu}
													value={this.state.rouleauObj.nbrCouche || ""} type="number"
													onChange={(event) => {
														if (/^\d*$/.test(event.target.value)) {
															this.setState({ rouleauObj: { ...this.state.rouleauObj, nbrCouche: (event.target.value.trim() !== "" && parseInt(event.target.value) > 0) ? parseInt(event.target.value) : null } })
														}
													}}
													onKeyUp={(e) => {
														if (e.key === "Enter") {
															this.longueurCoucheOverlapInput.focus()
														}
													}}
													ref={input => { this.nbrCoucheInput = input }}
												/>
											</div>
											<div style={{ width: 150, padding: "0 10" }}>
												Longueur Overlap<br />
												<input style={{ width: "100%", padding: "2 8" }} type="number" disabled={badReftissu}
													value={this.state.rouleauObj.longueurCoucheOverlap || ""}
													onChange={(event) => {
														if (/^\d*\.?\d*$/.test(event.target.value)) {
															this.setState({ rouleauObj: { ...this.state.rouleauObj, longueurCoucheOverlap: event.target.value.trim() !== "" ? event.target.value : null } })
														}
													}}
													onKeyUp={(e) => {
														if (e.key === "Enter") {
															this.defautInput.focus()
														}
													}}
													ref={input => { this.longueurCoucheOverlapInput = input }}
												/>
											</div>
											<div style={{ width: 140, padding: "0 10" }}>
												Défaut<br />
												<input style={{ width: "100%", padding: "2 8" }}
													value={this.state.rouleauObj.defaut || ""} type="number" disabled={badReftissu}
													onChange={(event) => {
														if (/^\d*\.?\d*$/.test(event.target.value)) {
															this.setState({ rouleauObj: { ...this.state.rouleauObj, defaut: event.target.value.trim() !== "" ? event.target.value : null } })
														}
													}}
													onKeyUp={(e) => {
														if (e.key === "Enter") {
															this.nonUtitlseInput.focus()
														}
													}}
													ref={input => { this.defautInput = input }}
												/>
											</div>
											<div style={{ width: 140, padding: "0 10" }}>
												Non Utilisé<br />
												<input style={{ width: "100%", padding: "2 8" }} type="number" disabled={badReftissu}
													value={this.state.rouleauObj.nonUtitlse || ""}
													onChange={(event) => {
														if (/^\d*\.?\d*$/.test(event.target.value)) {
															this.setState({ rouleauObj: { ...this.state.rouleauObj, nonUtitlse: event.target.value.trim() !== "" ? event.target.value : null } })
														}
													}}
													onKeyUp={(e) => {
														if (e.key === "Enter") {
															this.excessInput.focus()
														}
													}}
													ref={input => { this.nonUtitlseInput = input }}
												/>
											</div>

											<div style={{ width: 174, padding: "0 10" }}>
												Retour {this.state.rouleauObj.confirmRetour ? <FontAwesomeIcon icon={faCheck} color="green" /> : <FontAwesomeIcon icon={faTimes} color="red" />}
												<div className='d-flex'>
													<button className='btn btn-success mr-1' style={{ width: 50 }} onClick={() => {
														this.setState({ rouleauObj: { ...this.state.rouleauObj, confirmRetour: true } })
													}}>
														<FontAwesomeIcon icon={faCheck} />
													</button>
													<button className='btn btn-danger' style={{ width: 50, marginLeft: 40 }} onClick={() => {
														this.setState({ rouleauObj: { ...this.state.rouleauObj, confirmRetour: false } })
													}}>
														<FontAwesomeIcon icon={faTimes} />
													</button>
												</div>
											</div>
											{/* <div style={{ width: 140, padding: "0 10" }}>
										Excess <br /><strong>{this.state.rouleauObj.metrage && (this.state.rouleauObj.metrage - totalUsage).toFixed(3)}</strong>
									</div> */}
											<div style={{ width: 250, padding: "0 10" }}>
												Total Usage<br />
												<strong>{totalUsage.toFixed(3)} {this.state.rouleauObj.metrage && `(Rest:${(this.state.rouleauObj.metrage - totalUsage).toFixed(3)})`}</strong>
											</div>
										</div>
										<div className='mb-2'>
											<div style={{ padding: "0 10" }}>
												Overlap
											</div>
											<div className='d-flex flex-wrap' style={{ padding: "0 10" }}>
												{arr}
											</div>
											{this.state.markerLog && this.state.markerLog.length > 0 && this.state.markerLog[this.state.markerLog.length - 1].fabricTypeSpliceLength > 0 && <div className='d-flex flex-wrap' style={{ padding: "0 10" }}>
												{this.state.markerLog.map((item, index) => {
													let len = 0
													if (index === 0) {
														len = this.convertFloat(this.state.markerLog[0].fabricTypeSpliceLength / 1000)
													} else {
														len = this.convertFloat((this.state.markerLog[index].fabricTypeSpliceLength - this.state.markerLog[index - 1].fabricTypeSpliceLength) / 1000)
													}
													return <div style={{ width: ((len * 1000 * 100 / this.state.markerLog[this.state.markerLog.length - 1].fabricTypeSpliceLength) + "%"), textAlign: "center", border: "1px black solid" }}>
														{len}
													</div>
												})}
											</div>}
											{this.state.markerLog && this.state.markerLog.length > 0 && this.state.markerLog[this.state.markerLog.length - 1].fabricTypeSpliceLength > 0 && <div className='d-flex flex-wrap' style={{ padding: "0 10" }}>
												{this.state.obj.cuttingRequestSerieRouleaus.map((item, index) => {
													let arrOverlaps = []
													if (item.overlap1 && item.overlap1 > 0) { arrOverlaps.push(item.overlap1) }
													if (item.overlap2 && item.overlap2 > 0) { arrOverlaps.push(item.overlap2) }
													if (item.overlap3 && item.overlap3 > 0) { arrOverlaps.push(item.overlap3) }
													if (item.overlap4 && item.overlap4 > 0) { arrOverlaps.push(item.overlap4) }
													if (item.overlap5 && item.overlap5 > 0) { arrOverlaps.push(item.overlap5) }
													if (item.overlap6 && item.overlap6 > 0) { arrOverlaps.push(item.overlap6) }
													if (item.overlap7 && item.overlap7 > 0) { arrOverlaps.push(item.overlap7) }
													if (item.overlap8 && item.overlap8 > 0) { arrOverlaps.push(item.overlap8) }
													return arrOverlaps.map((overlap, index) => {
														return <div style={{ width: ((overlap * 100 * 1000 / this.state.markerLog[this.state.markerLog.length - 1].fabricTypeSpliceLength) + "%"), textAlign: "center", border: "1px black solid" }}>
															{overlap}
														</div>
													})
												})}
											</div>}
										</div>
										<div style={{ display: "flex", justifyContent: "end", paddingBottom: 10 }}>
											<span className='mr-2' style={{ fontSize: 20 }}>Nombre de couche total: {nbrCoucheTotal}</span>
											<button className='btn btn-sm btn-outline-dark ml-2' style={{ padding: "4 40" }}
												onClick={() => { this.setState({ rouleauObj: {} }); this.confirmReftissuInput.focus() }}
											>Annuler</button>
											{this.state.rouleauObj && this.state.rouleauObj.defaut > 0 && this.state.rouleauObj.confirmRetour === false && <Select classNamePrefix="rs" className='p-0 input-sm col-3 ml-2'
												isClearable={true} placeholder={"Défaut Rouleau ..."}
												value={this.state.rouleauObj.defautCode ? { label: this.state.rouleauObj.defautCode, value: this.state.rouleauObj.defautCode } : null}
												options={this.state.defautRouleauList.map((item) => { return { label: item.titre, value: item.titre } })}
												// menuPosition={'absolute'} // 'fixed', 'absolute'
												menuPlacement={"top"} // auto, bottom, top
												onChange={(option) => {
													if (option) {
														this.setState({
															rouleauObj: {
																...this.state.rouleauObj,
																defautCode: option.value,
															}
														})
													} else {
														this.setState({
															rouleauObj: {
																...this.state.rouleauObj,
																defautCode: null,
															}
														})
													}
												}}
											/>}
											{/* <button className='btn btn-sm btn-dark ml-2' style={{ padding: "4 40" }} disabled={this.state.loadingSave}
												onClick={() => { this.handleSubmit() }}
											>{this.state.loadingSave ?
												<FontAwesomeIcon icon={faCircleNotch} spin />
												: <span><FontAwesomeIcon icon={faFloppyDisk} /> {this.state.rouleauObj.id ? "Modifier" : "Ajouter"}</span>
												}</button> */}
										</div>
										{this.state.obj.cuttingRequestSerieRouleaus && <div className='table-responsive bg-white'>

											<table className='table table-bordered table-cells-bg m-0' style={{ fontSize: 14 }}>
												<thead className='header-table-black'>
													<tr>
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
																	this.confirmReftissuInput.focus()
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
									: <div style={this.state.rouleauObj.id ? { padding: "10", backgroundColor: "#c0c6f9", borderRadius: "5" } : { padding: "10", backgroundColor: "#f3f3f3", borderRadius: "5" }}>
										<h1>{this.state.obj.longueur}m {"> 0.7m"} : En Attente d'utilisation du splice ...</h1>
									</div>}

							</div>}
						</div>}
				</div>
				{this.state.obj.placement && <span style={{ color: "green", padding: "0 10 15" }}>
					Dans le cas des points NOK, aviser le Aid chef / chef d'équipe / Coordenateur qualité / Contrôle volant / Agent maitenance / Agent CAD
				</span>}
				<div style={{ flex: 1 }}></div>
				{this.state.obj.tableMatelassage && this.renderMachineSchedule()}
				{this.state.error != null && <div className="alert alert-danger alert-error text-center m-4" role="alert">
					<ul>
						<button type='btn' className="btn btn-toclose" onClick={() => { this.setState({ error: null }) }}>
							<FontAwesomeIcon icon={faTimes} size="2x" />
						</button>
						{this.renderErrorsAlert(this.state.error)}
					</ul>
				</div>}
				{this.state.obj.placement && <div style={{ display: "flex", justifyContent: "center", backgroundColor: "#f9f9f9", position: "sticky", bottom: 0, width: "100%", padding: 10, boxShadow: "0px 0px 5px 1px #d5d5d5" }}>
					{/* <button className='btn btn-outline-dark mr-2' style={{ padding: "10 16" }}
						onClick={() => {
							if (window.confirm("Voulez-vous vraiment quitter l'application ?")) {
								try {
									axios.get("/api/stop-app")
								} catch {

								}
								setTimeout(() => {
									let newWindow = window.open('', '_self');
									window.close();
									newWindow.close();
								}, 100)
							}
						}}
					>
						<FontAwesomeIcon icon={faTimes} />
					</button> */}
					{/* <button className='btn btn-outline-dark mr-2' style={{ padding: "10 16" }}
						onClick={() => {
							this.setState({ showFirstCheckModal: !this.state.showFirstCheckModal })
						}}
					>
						<span><FontAwesomeIcon icon={faScrewdriverWrench} /> Maint. 1er niveau</span>
					</button>
					<button className='btn btn-outline-dark mr-2' style={{ padding: "10 16" }}
						onClick={() => {
							this.setState({ showTable: !this.state.showTable })
						}}
					>
						{this.state.showTable ? <span><FontAwesomeIcon icon={faKeyboard} /> Formulaire</span> : <span><FontAwesomeIcon icon={faTable} /> Tableau</span>}
					</button> */}

					{/* <button className='btn btn-outline-dark mr-2' style={{ padding: "10 16" }}
						onClick={() => {
							localStorage.removeItem("matricule1");
							localStorage.removeItem("matricule2");
							this.props.history.push("/")
						}}
					>
						<FontAwesomeIcon icon={faArrowRightFromBracket} />
					</button> */}
					{/* <button className='btn btn-outline-dark mr-2' style={{ padding: "10 16" }}
						onClick={() => {
							this.setState({ obj: { serie: "" }, qn: null, showQn: false, rouleauObj: {}, interventionList: [], intervention: null })
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
									machine: this.state.obj.tableMatelassage,
									type: "matelassage"
								}
							})
						}}
					>
						<FontAwesomeIcon icon={faTriangleExclamation} /> Intervention
					</button>}
					{
						this.state.obj.placement
						&& nbrCoucheTotal !== null
						&& rest !== null && rest >= 0 && rest <= 0.02
						&& nbrCoucheTotal == this.state.obj.nbrCouche
						&& this.state.obj.statusMatelassage !== 'Complete'
						&& <button
							className='btn btn-outline-success mr-2'
							onClick={() => {
								axios.post("/api/cuttingRequestSerieInfo", {
									...this.state.obj,
									cuttingRequestSerieRouleaus: [
										...this.state.obj.cuttingRequestSerieRouleaus
									],
									statusMatelassage: 'Complete',
									retourMagasin
								})
									.then(res => {
										if (res.data.cuttingRequestSerieRouleaus && res.data.cuttingRequestSerieRouleaus.filter(e => e.confirmRetour).length === 0) {
											this.setState({ obj: { serie: "" }, qn: null, showQn: false, rouleauObj: {}, interventionList: [], intervention: null })
										} else {
											this.setState({ obj: { ...res.data }, qn: null, showQn: false, rouleauObj: {}, interventionList: [], intervention: null })
										}
									})
							}}
						>
							Complet
						</button>}
					{this.state.obj.placement && nbrCoucheTotal !== null && nbrCoucheTotal < this.state.obj.nbrCouche && this.state.obj.statusMatelassage !== 'Incomplete' && <button className='btn btn-outline-danger mr-2'
						onClick={() => {
							axios.post("/api/cuttingRequestSerieInfo", {
								...this.state.obj,
								cuttingRequestSerieRouleaus: [
									...this.state.obj.cuttingRequestSerieRouleaus
								],
								statusMatelassage: 'Incomplete',
								retourMagasin
							})
								.then(res => {
									this.setState({ obj: { serie: "" }, rouleauObj: {} })
								})
						}}
					>
						Incomplet {nbrCoucheTotal !== null && `(${(nbrCoucheTotal - this.state.obj.nbrCouche)})`}
					</button>}
					{/* {this.state.obj.cuttingRequestSerieRouleaus
						&& this.state.obj.cuttingRequestSerieRouleaus.filter(e => e.confirmRetour).length > 0
						&& ['Complete', 'Incomplete'].includes(this.state.obj.statusMatelassage)
						&& <button className='btn btn-outline-success'
							onClick={() => {
								axios.post("/api/cuttingRequestSerieInfo/print", this.state.obj)
									.then(res => {
										this.setState({ obj: { serie: "" }, rouleauObj: {} })
										this.serieInput.focus()
									})
							}}
						>
							<FontAwesomeIcon icon={faPrint} /> Imprimer
						</button>} */}
					{retourMagasin !== null && <span style={{ whiteSpace: "nowrap", padding: "6 12", fontWeight: "bold" }}>Retour au Magasin : {retourMagasin.toFixed(3)}</span>}
					{this.state.obj.longueur && rest !== null && rest > 0 && rest < this.state.obj.longueur && <span style={{ whiteSpace: "nowrap", padding: "6 12" }}>(Manque Couche overlap : {(this.state.obj.longueur - rest).toFixed(3)})</span>}
				</div>}
				{this.renderModalQn()}
				{this.renderModalIntervention()}
				{this.renderRestRouleauModal()}
			</div>
		)
	}
}

Form.propTypes = {
	security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
	security: state.security
})

export default connect(mapStateToProps, { })(Form);
