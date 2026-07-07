import { faPlayCircle, faPlay, faPause, faCommentsDollar, faStop, faLocationCrosshairs, faArrowRight, faArrowLeft, faEarthAsia, faEye, faFileCsv } from '@fortawesome/free-solid-svg-icons';
import axios from 'axios';
import moment from 'moment';
import React, { Component } from 'react'
import Select from "react-select";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import "../../styles/RapportLectra.scss";
import { Modal } from 'react-bootstrap'
import { departementOption, problemeResoluOption } from '../../metadata';
import FormCoupe from './FormCoupe';

export default class RapportLectraV2 extends Component {

	constructor() {
		super();
		this.cancelTokenSource = axios.CancelToken.source();
		this.state = {
			productionTableArr: [],
			machineObj: {},
			date: moment().add(2, 'hours').format('YYYY-MM-DD'),
			shift: this.getShift(moment().add(2, 'hours')),
			zoneArr: [],
			machineArr: [],
			data: {},
			date1: null, 
			date2: null,
			selectedZone: null,
			selectedMachine: [],
			placements: {},
			series: {},
			interruptions: {},
			xplSeries: {}, // XPL scanned series lookup
			codeErreurList: [],
			codeArretList: [],
			codeDefautList: [],
			intervention: null,
		}
	}

	componentWillUnmount() {
		this.stopLoop(); // Stop the loop when the component is unmounted or navigating away
		this.cancelTokenSource.cancel('Component unmounted');
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

	componentDidMount() {

		let shift = this.state.shift
		let date1;
		let date2;
		let date = this.state.date
		if (shift === 1) {
			date1 = moment(date).subtract(1, 'day').format('YYYY-MM-DD 21:55');
			date2 = moment(date).format('YYYY-MM-DD 05:45');
		} else if (shift === 2) {
			date1 = moment(date).format('YYYY-MM-DD 05:55');
			if (moment(date).day() === 5) {
				date2 = moment(date).format('YYYY-MM-DD 13:30');
			} else {
				date2 = moment(date).format('YYYY-MM-DD 13:45');
			}
		} else if (shift === 3) {
			if (moment(date).day() === 5) {
				date1 = moment(date).format('YYYY-MM-DD 14:05');
			} else {
				date1 = moment(date).format('YYYY-MM-DD 13:55');
			}
			date2 = moment(date).format('YYYY-MM-DD 21:45');
		}

		this.setState({ date1, date2 })
		axios.get(`/api/productionTable/list`)
			.then(res => {
				let zoneArr = [];
				res.data.forEach(element => {
					if (!zoneArr.includes(element.zone.nom)) {
						zoneArr.push(element.zone.nom);
					}
				});
				this.setState({
					zoneArr,
					productionTableArr: res.data
				});
			})
			.catch(error => this.setState({ error, productionTableArr: [] }));

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

	startLoop() {
		if (this.loopInterval) {
			clearInterval(this.loopInterval);
		}
		this.getData()
		this.loopInterval = setInterval(() => {
			// Your loop code goes here
			this.getData()
		}, 60000);
	}

	stopLoop() {
		clearInterval(this.loopInterval);
	}

	getData = () => {
		this.cancelTokenSource.cancel('New request initiated');
		this.cancelTokenSource = axios.CancelToken.source();


		axios.get(`/api/coupePerformance/filter`, {
			params: {
				date: this.state.date,
				shift: this.state.shift,
				machines: this.state.machineArr.map((e) => e.nom).join(','),
			},
			cancelToken: this.cancelTokenSource.token, // Pass the cancel token
		})
			.then(res => {
				// let resultObj = this.getIndicatorsFromMachineData(res.data)
				let obj = {}
				this.state.machineArr.forEach(machine => {
					obj[machine.nom] = res.data.filter(element => element.machine === machine.nom)
				})
				this.setState({
					data: obj,
				})
				let placementArr = []
				let seriesArr = []
				let interruptionArr = []

				res.data.map(e => {
					if (!placementArr.includes(e.placement)) {
						placementArr.push(e.placement)
					}
					if (!seriesArr.includes(e.serie)) {
						seriesArr.push(e.serie)
					}
				})

				if (seriesArr.length > 0) {
					axios.get(`/api/cuttingRequestSerieData/seriesArr?series=${seriesArr.join(",")}`)
						.then(res => {
							let series = this.state.series
							res.data.forEach(e => {
								series[e.serie] = e
							})
							this.setState({ series })
						})
					// Load XPL scan data for the current series
					axios.get(`/api/scanXPL/bySerieIn?series=${seriesArr.join(",")}`)
						.then(res => {
							let xplSeries = this.state.xplSeries
							res.data.forEach(e => {
								xplSeries[e.serie] = e
							})
							this.setState({ xplSeries })
						})
						.catch(err => {
							console.log("Error loading XPL data:", err)
						})
				}
				// if (placementArr.length > 0) {
				// 	axios.get(`/api/placementData/nbrPiece/${placementArr.join(",")}`)
				// 		.then(res => {
				// 			let placements = this.state.placements
				// 			//sdf
				// 			res.data.forEach(e => {
				// 				if (e.nbrPiece != null) {
				// 					let tempReperage = 0;
				// 					if (e.longueur > 0 && e.longueur < 2) {
				// 						tempReperage += 90
				// 					} else if (e.longueur >= 2 && e.longueur <= 3.5) {
				// 						tempReperage += 130
				// 					} else if (e.longueur > 3.5) {
				// 						tempReperage += 180
				// 					}
				// 					if (e.drill1 || e.drill2) {
				// 						tempReperage += 40
				// 					}
				// 					placements[e.placement] = { ...e, tempReperage }
				// 				}
				// 			})
				// 			this.setState({ placements })
				// 		})
				// }
			})
	}

	convertMinutesToTimeString = (minutes) => {
		const hours = Math.floor(minutes / 60);
		const remainingMinutes = minutes % 60;
		let timeString = hours.toString().padStart(2, '0') + " H ";
		if (remainingMinutes !== 0) {
			timeString += remainingMinutes.toString().padStart(2, '0') + " min";
		}
		return timeString;
	};

	convertMillisecondsToTimeString = (milliseconds) => {
		const seconds = Math.floor(milliseconds / 1000);
		const minutes = Math.floor(seconds / 60);
		const remainingSeconds = seconds % 60;
		const hours = Math.floor(minutes / 60);
		const remainingMinutes = minutes % 60;

		let timeString = '';

		if (hours > 0) {
			timeString += hours.toString() + 'h';
		}

		if (remainingMinutes > 0) {
			timeString += ' ' + remainingMinutes.toString() + 'min';
		}

		return timeString.trim();
	};

	convertMillisecondsToTimeStringV2 = (milliseconds) => {
		let timeString = '';
		if (milliseconds < 0) {
			timeString += '-';
			milliseconds = Math.abs(milliseconds);
		}

		const seconds = Math.floor(milliseconds / 1000);
		const minutes = Math.floor(seconds / 60);
		const remainingSeconds = seconds % 60;
		const hours = Math.floor(minutes / 60);
		const remainingMinutes = minutes % 60;



		if (hours > 0) {
			timeString += hours.toString() + 'h';
		}

		if (remainingMinutes > 0) {
			timeString += ' ' + remainingMinutes.toString() + 'min';
		}

		if (remainingSeconds > 0) {
			timeString += ' ' + remainingSeconds.toString() + 's';
		}

		return timeString.trim();
	};

	convertFloat = (float) => {
		//check if the float is a number
		if (!float || isNaN(float)) {
			return float
		}
		return parseFloat(parseFloat(float).toFixed(1))
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
				maxHeight: "calc(95vh - 30px)",
				overflow: "auto",
				padding: "8 15 0",
				display: 'flex', flexDirection: "column"
			}}>
				<div style={{ flex: 1 }}>
					<h2 className='text-center m-2'>Formulaire d'intervention</h2>
					<div style={{ paddingTop: 35 }}>
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
							<label className='col-2 text-right'>Département</label>
							<div className='col-3'>
								{this.state.intervention.departement}
							</div>
							<label className='col-2 text-right'>Problème résolu ?</label>
							<div className='col-3'>
								{this.state.intervention.problemeResolu}
							</div>

						</div>
						<div className='row my-2'>
							<label className='col-2 text-right'>Code d'arrêt</label>
							<div className='col-3'>
								{this.state.intervention.codeArret.code + " " + this.state.intervention.codeArret.motifArret}
							</div>
						</div>
						<div className='row my-2'>
							{this.state.intervention.codeArret && this.state.intervention.codeArret.code === "D_2" && [<label className='col-2  text-right'>Code Coupe</label>,
							<div className='col-3'>
								{this.state.intervention.codeCoupe.code + " " + this.state.intervention.codeCoupe.description}
							</div>]}
							{this.state.intervention.departement === "Maintenance" && [<label className='col-2  text-right'>Solution</label>,
							<div className='col-3'>
								{this.state.intervention.solution && (this.state.intervention.solution.code + " " + this.state.intervention.solution.description)}
							</div>]}
						</div>
						<div className='row my-2'>
							<label className='col-2  text-right'>Cause</label>
							<div className='col-3 p-0'>
								<div className='col-3'>
									{this.state.intervention.cause}
								</div>
							</div>
							<label className='col-2  text-right'>Action</label>
							<div className='col-3 p-0'>
								<div className='col-3'>
									{this.state.intervention.action}
								</div>

							</div>
						</div>

					</div>

					<div className='row my-2'>
						<label className='col-2 text-right'>Matricule d'émetteur</label>
						<div className='col-3' >
							{this.state.intervention.matriculeEmetteur}
						</div>
						<label className='col-2 text-right'>Matricule de responsable</label>
						<div className='col-3'>
							{this.state.intervention.matriculeResponsable}
						</div>
					</div>
					{/* <div>
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
					</div> */}
				</div>
				<hr />
				<div style={{ display: "flex", flexDirection: 'row-reverse', backgroundColor: "white", position: "sticky", bottom: 0 }} className="p-2">
					<button className='btn btn-link' onClick={() => { this.setState({ intervention: null }) }}>Retour</button>
				</div>
			</div>}
		</Modal>
	}

	renderModalSerie() {
		if (!this.state.modalObj) return null
		let arrInteruptions = []
		let arrReperage = []
		let lastInteruptionDate = null, lastReperageDate = null, errorCode = null
		// if (this.state.modalObj && this.state.modalObj.events) {
		// 	this.state.modalObj.events.forEach((element, index) => {
		// 		if (element.type === "StartPositioning") {
		// 			lastReperageDate = element.lineDate
		// 		} else if (element.type === "StopPositioning") {
		// 			if (arrReperage.length === 0 && lastReperageDate === null) {
		// 				arrReperage.push({
		// 					start: this.state.modalObj.start,
		// 					end: element.lineDate,
		// 					extra: element.extra,
		// 					element
		// 				})
		// 			} else {
		// 				arrReperage.push({
		// 					start: lastReperageDate,
		// 					end: element.lineDate,
		// 					extra: element.extra,
		// 					element
		// 				})
		// 				lastReperageDate = null
		// 			}
		// 		}

		// 		if (element.type === "InterruptionCoupeIn") {
		// 			lastInteruptionDate = element.lineDate
		// 			errorCode = element.errorCode
		// 		} else if (element.type === "InterruptionCoupeOut") {
		// 			if (arrInteruptions.length === 0 && lastInteruptionDate === null) {
		// 				arrInteruptions.push({
		// 					start: this.state.modalObj.start,
		// 					end: element.lineDate,
		// 					errorCode: errorCode ? errorCode : element.errorCode,
		// 					extra: element.extra,
		// 					element
		// 				})
		// 				errorCode = null
		// 			} else {
		// 				arrInteruptions.push({
		// 					start: lastInteruptionDate,
		// 					end: element.lineDate,
		// 					errorCode: errorCode ? errorCode : element.errorCode,
		// 					extra: element.extra,
		// 					element
		// 				})
		// 				lastInteruptionDate = null,
		// 					errorCode = null
		// 			}
		// 		}
		// 	});
		// 	let newArrReperage = []

		// 	if (this.state.modalObj.placement && this.state.placements[this.state.modalObj.placement]) {
		// 		let counterMsRepearage = this.state.placements[this.state.modalObj.placement].tempReperage * 1000
		// 		if (this.state.placements[this.state.modalObj.placement].tempReperage)
		// 			arrReperage.map(rep => {
		// 				// counterMsRepearage is the tatal time of reperage theorique and we to consume it and leave in arrReperege only the time that is not consumed
		// 				let intervalReperage = moment(rep.end).diff(moment(rep.start))
		// 				if (intervalReperage > counterMsRepearage) {
		// 					newArrReperage.push({ ...rep, start: moment(rep.start).add(counterMsRepearage, "ms").format("YYYY-MM-DDTHH:mm:ss.SSS") })
		// 					counterMsRepearage = 0
		// 				} else {
		// 					counterMsRepearage -= intervalReperage
		// 				}
		// 			})
		// 		// arrReperage = arrReperage
		// 		// .filter(rep => this.state.obj.placement &&  this.state.placements[this.state.obj.placement] && moment(rep.end).diff(moment(rep.start)) > this.state.placements[this.state.obj.placement].tempReperage*1000)
		// 		// .map(repElem => { return { ...repElem, start: moment(repElem.start).add(this.state.placements[this.state.obj.placement].tempReperage*1000, "ms").format("YYYY-MM-DDTHH:mm:ss.SSS") } })
		// 		arrReperage = newArrReperage
		// 	}

		// }

		// arrInteruptions = arrInteruptions.filter(element => moment(element.end).diff(moment(element.start)) > 30000)
		return <Modal
			show={this.state.modalObj}
			onHide={() => this.setState({ modalObj: null })}
			dialogClassName="modal-75w"
			centered
		>
			<div style={{
				maxHeight: "calc(95vh - 30px)",
				overflow: "auto",
				padding: "8 15 0",
				display: 'flex', flexDirection: "column"
			}}>
				<div>
					<FormCoupe serie={this.state.modalObj.serie} />
				</div>
				{this.state.modalObj.lastEnd && moment(this.state.modalObj.events[0].lineDate).diff(moment(this.state.modalObj.lastEnd)) > 30000 && <div className=''>
					<table className='table table-bordered table-cells-sm  font-size-30' >
						<thead className='header-table-black'>
							<tr style={{ backgroundColor: "#000000" }}>
								<th colSpan={10} >Hors Cycle</th>
							</tr>
							<tr style={{ backgroundColor: "#767676" }}>
								<th>Date Fin precendente</th>
								<th>Debut de reperage</th>
								<th>Diff</th>
								<th style={{ width: 248 }}>N° Bon</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<td>{this.state.modalObj.lastEnd && moment(this.state.modalObj.lastEnd).format("HH:mm:ss")}</td>
								<td>{this.state.modalObj.events[0].lineDate && moment(this.state.modalObj.events[0].lineDate).format("HH:mm:ss")}</td>
								<td>{this.state.modalObj.events[0].lineDate && this.state.modalObj.lastEnd && this.convertMillisecondsToTimeStringV2(moment(this.state.modalObj.events[0].lineDate).diff(moment(this.state.modalObj.lastEnd)))}</td>
								<td>{this.state.modalObj.events[0].extra
									? <div className='d-flex' style={{ justifyContent: "center", alignItems: "center" }}>
										<span>{this.state.modalObj.events[0].extra}</span>
										{this.state.interruptions[this.state.modalObj.events[0].extra] && <button className='btn btn-sm btn-outline-primary'
											onClick={() => {
												this.setState({ intervention: this.state.interruptions[this.state.modalObj.events[0].extra] })
											}}><FontAwesomeIcon icon={faEye} /></button>}
									</div>
									: <div className='d-flex'>
										{/* <div style={{ width: 200, fontSize: 16 }}>
											<button className='btn btn-sm btn-outline-primary ml-2' onClick={() => {
												this.setState({
													intervention: {
														debutArret: this.state.modalObj.lastEnd && moment(this.state.modalObj.lastEnd).format("yyyy-MM-DD HH:mm:ss").replace(" ", "T"),
														debutIntervention: this.state.modalObj.lastEnd && moment(this.state.modalObj.lastEnd).format("yyyy-MM-DD HH:mm:ss").replace(" ", "T"),
														finIntervention: this.state.modalObj.events[0].lineDate && moment(this.state.modalObj.events[0].lineDate).format("yyyy-MM-DD HH:mm:ss").replace(" ", "T"),
														type: "coupe",
														sousType: "Hors cycle",
														date: this.state.date,
														shift: this.state.shift,
														serie: this.state.modalObj.serie,
														sequence: this.state.series[this.state.modalObj.serie] ? this.state.series[this.state.modalObj.serie].cuttingRequest.sequence : null,
														partNumberMaterial: this.state.series[this.state.modalObj.serie] ? this.state.series[this.state.modalObj.serie].partNumberMaterial : null,
														partNumberMaterialDescription: this.state.series[this.state.modalObj.serie] ? this.state.series[this.state.modalObj.serie].description : null,
														matriculeEmetteur: this.state.postObj.coupeur1,
														machine: this.state.postObj.tableCoupe,
														codeErreur: this.state.modalObj.events[0].errorCode,
													},
													quickElement: {
														...this.state.modalObj.events[0],
														extra: null,
													}
												})
											}}
											>Créer un bon</button>
										</div> */}
									</div>
								}</td>
							</tr>
						</tbody>
					</table>
				</div>}
				{arrReperage.length > 0 && <div className=''>
					<table className='table table-bordered table-cells-sm mt-2  font-size-30'>
						<thead className='header-table-black' >
							<tr style={{ backgroundColor: "#034472" }}>
								<th colSpan={10} >Les repérages</th>
							</tr>
							<tr style={{ backgroundColor: "#0070c0" }}>
								<th>Date debut</th>
								<th>Date fin</th>
								<th>Diff</th>
								<th style={{ width: 248 }}>N° Bon</th>
							</tr>
						</thead>
						<tbody>
							{arrReperage.map(objRep => {
								let diff = 0
								if (objRep.end && objRep.start) {
									diff = moment(objRep.end).diff(moment(objRep.start))
								}
								return <tr key={objRep.id}>
									<td>{objRep.start && moment(objRep.start).format("HH:mm:ss")}</td>
									<td>{objRep.end && moment(objRep.end).format("HH:mm:ss")}</td>
									<td>
										{this.convertMillisecondsToTimeStringV2(diff)}
									</td>
									<td>{objRep.extra
										? <div className='d-flex' style={{ justifyContent: "center", alignItems: "center" }}>
											<span>{objRep.extra}</span>
											{this.state.interruptions[objRep.extra] && <button className='btn btn-sm btn-outline-primary'
												onClick={() => {
													this.setState({ intervention: this.state.interruptions[objRep.extra] })
												}}><FontAwesomeIcon icon={faEye} /></button>}
										</div>
										: <div>
											{/* <button className='btn btn-sm btn-outline-primary ml-2' onClick={() => {
												this.setState({
													intervention: {
														debutArret: objRep.start && moment(objRep.start).format("yyyy-MM-DD HH:mm:ss").replace(" ", "T"),
														debutIntervention: objRep.start && moment(objRep.start).format("yyyy-MM-DD HH:mm:ss").replace(" ", "T"),
														finIntervention: objRep.end && moment(objRep.end).format("yyyy-MM-DD HH:mm:ss").replace(" ", "T"),
														// codeArret: e.value,
														// departement: e.value.departement,
														type: "coupe",
														sousType: "reperage",
														date: this.state.date,
														shift: this.state.shift,
														serie: this.state.modalObj.serie,
														sequence: this.state.series[this.state.modalObj.serie] ? this.state.series[this.state.modalObj.serie].cuttingRequest.sequence : null,
														partNumberMaterial: this.state.series[this.state.modalObj.serie] ? this.state.series[this.state.modalObj.serie].partNumberMaterial : null,
														partNumberMaterialDescription: this.state.series[this.state.modalObj.serie] ? this.state.series[this.state.modalObj.serie].description : null,
														matriculeEmetteur: this.state.postObj.coupeur1,
														machine: this.state.postObj.tableCoupe,
														codeErreur: objRep.errorCode,
													},
													quickElement: {
														...objRep.element,
													}
												})
											}}>Créer un bon</button> */}
										</div>
									}</td>
								</tr>
							})}
						</tbody>
					</table>
				</div>}
				{arrInteruptions.length > 0 && <div className=''>
					<table className='table table-bordered table-cells-sm font-size-30'>
						<thead className='header-table-black' >
							<tr style={{ backgroundColor: "#c59505" }}>
								<th colSpan={10} >Les interruptions</th>
							</tr>
							<tr style={{ backgroundColor: "#ffc000" }}>
								<th>Date debut</th>
								<th>Date fin</th>
								<th>Diff</th>
								<th>Code</th>
								<th>Designation</th>
								<th style={{ width: 248 }}>N° Bon</th>
							</tr>
						</thead>
						<tbody>
							{arrInteruptions.map(objInt => {
								let diff = 0
								if (objInt.end && objInt.start) {
									diff = moment(objInt.end).diff(moment(objInt.start))
								}
								let errorObj = null
								if (objInt.errorCode) {
									errorObj = this.state.codeErreurList.find(obj => obj.code === objInt.errorCode)
								}
								return <tr>
									<td>{objInt.start && moment(objInt.start).format("HH:mm:ss")}</td>
									<td>{objInt.end && moment(objInt.end).format("HH:mm:ss")}</td>
									<td>{this.convertMillisecondsToTimeStringV2(diff)}</td>
									<td>{objInt.errorCode}</td>
									<td>{errorObj && errorObj.designation}</td>
									<td>{objInt.extra
										&& <div className='d-flex' style={{ justifyContent: "center", alignItems: "center" }}>
											<span>{objInt.extra}</span>
											{this.state.interruptions[objInt.extra] && <button className='btn btn-sm btn-outline-primary'
												onClick={() => {
													this.setState({ intervention: this.state.interruptions[objInt.extra] })
												}}><FontAwesomeIcon icon={faEye} /></button>}
										</div>
									}</td>
								</tr>
							})}
						</tbody>
					</table>
				</div>}
				<div className='d-flex justify-content-between mb-2'>
					<button className='btn btn-primary ml-auto' onClick={() => this.setState({ modalObj: null })}>Fermer</button>
				</div>
			</div>

		</Modal>
	}

	renderDetailsMachine = (machine, key) => {
		let runningTotal = 0
		let interruptionTotal = 0
		let reperageTotal = 0
		let horsCycleTotal = 0
		let placementCount = 0
		let horsCycleTheorique = 0
		let reperageTheorique = 0
		if (this.state.data[machine.nom] && this.state.data[machine.nom].length > 0) {
			this.state.data[machine.nom].map(obj => {
				// Check if placement ends with "-NS" and exclude from efficiency calculations
				if (obj.placement && obj.placement.endsWith("-NS")) {
					// Move all time to horsCycleTotal (non-productive time)
					horsCycleTotal += (obj.running || 0) + (obj.interruption || 0) + (obj.reperage || 0) + (obj.horsCycle || 0);
				} else {
					// Normal calculation for productive placements
					if (obj.running) {
						runningTotal += obj.running
					}
					if (obj.interruption) {
						interruptionTotal += obj.interruption
					}
					if (obj.reperage && obj.reperage != 0) {
						reperageTotal += obj.reperage
						reperageTheorique += obj.reperagePlus
					}
					if (obj.horsCycle) {
						horsCycleTotal += obj.horsCycle
					}
					horsCycleTheorique += (30000 - obj.horsCycle)
				}
			})

			placementCount = this.state.data[machine.nom].filter(e => e.compteur > 0 && e.placement != null && !e.placement.toLowerCase().startsWith("test") && !e.placement.endsWith("-NS")).length

		}
		return <div key={key}>
			<div className='machine-card-container' onClick={() => {
				if (this.state.selectedMachine.includes(machine.nom)) {
					this.setState({ selectedMachine: this.state.selectedMachine.filter(e => e !== machine.nom) })
				} else {
					this.setState({ selectedMachine: [...this.state.selectedMachine, machine.nom] })
				}
			}}>
				<div className='machine-info' style={{ width: 175 }}>
					{machine.nom}<br />{machine.pcCoupe && `${machine.pcCoupe}`}
				</div>
				{this.state.data[machine.nom] && this.state.data[machine.nom].length > 0 && [
					<div className='machine-info' style={{ flex: 1 }}>
					</div>,
					<div className='machine-info-center'>
						{(this.state.data[machine.nom].length > 0) ? placementCount : 0}
					</div>,
					<div className='machine-info-center'>
						{this.convertMillisecondsToTimeString(runningTotal)} ({this.convertFloat(100 * runningTotal / moment.min(moment(this.state.date2), moment()).diff(moment(this.state.date1)))} %)
					</div>,
					<div className='machine-info-center'>
						{this.convertMillisecondsToTimeString(interruptionTotal)} ({this.convertFloat(100 * interruptionTotal / moment.min(moment(this.state.date2), moment()).diff(moment(this.state.date1)))} %)
					</div>,
					<div className='machine-info-center'>
						{this.convertMillisecondsToTimeString(reperageTotal)} ({this.convertFloat(100 * reperageTotal / moment.min(moment(this.state.date2), moment()).diff(moment(this.state.date1)))} %)
						<br /><span style={reperageTheorique >= 0 ? { color: "#00ad00" } : { color: "#ff0000" }}>{this.convertMillisecondsToTimeStringV2(reperageTheorique)}</span>
					</div>,
					<div className='machine-info-center'>
						{this.convertMillisecondsToTimeString(horsCycleTotal)} ({this.convertFloat(100 * horsCycleTotal / moment.min(moment(this.state.date2), moment()).diff(moment(this.state.date1)))} %)
						<br /><span style={horsCycleTheorique >= 0 ? { color: "#00ad00" } : { color: "#ff0000" }}>{this.convertMillisecondsToTimeStringV2(horsCycleTheorique)}</span>
					</div>
				]}
			</div>

			{this.state.selectedMachine.includes(machine.nom) && <div style={{ margin: "5 20" }}>
				<table className='table table-bordered table-cells-sm table-font-size'>
					<thead style={{ color: "white", backgroundColor: "#cb0000" }}>
						<tr>
							<th style={{ width: 100 }}>Serie</th>
							<th style={{ width: 100 }}>Auto Contrôle</th>
							<th style={{ width: 100 }}>Placement</th>
							<th>Début</th>
							<th>Fin</th>
							<th style={{ width: 200 }}>Compteur Pièce</th>
							<th style={{ width: 235 }}>Running</th>
							<th style={{ width: 223 }}>Interruption</th>
							<th style={{ width: 223 }}>Repérage</th>
							<th style={{ width: 210 }}>Hors cycle</th>
						</tr>
					</thead>
					<tbody>
						{this.state.data && this.state.data[machine.nom] && this.state.data[machine.nom].map((obj, key) => {
							let arrAudit = []
							if (this.state.series[obj.serie]) {
								let style = { flex: 1, border: "1px grey solid", color: "black", fontWeight: "bold", paddingTop: 12 }
								arrAudit.push(<div alt={"Premier Paquet"}
									style={{
										backgroundColor: (this.state.series[obj.serie].premierPaquet === "OK" ? "#2fdb2f"
											: this.state.series[obj.serie].premierPaquet === "NOK" ? "red"
												: this.state.series[obj.serie].premierPaquet === "NA" ? "#5b5beb"
													: "white"), ...style
									}}></div>)
								if (this.state.series[obj.serie].longueur > 3) {
									arrAudit.push(<div
										alt={"Milieu Paquet"}
										style={{
											backgroundColor: (this.state.series[obj.serie].milieuPaquet === "OK" ? "#2fdb2f"
												: this.state.series[obj.serie].milieuPaquet === "NOK" ? "red"
													: this.state.series[obj.serie].milieuPaquet === "NA" ? "#5b5beb"
														: "white"), ...style
										}}></div>)
								}
								if (this.state.series[obj.serie].longueur > 0.4) {
									arrAudit.push(<div
										alt={"Dernier Paquet"}
										style={{
											backgroundColor: (this.state.series[obj.serie].dernierPaquet === "OK" ? "#2fdb2f"
												: this.state.series[obj.serie].dernierPaquet === "NOK" ? "red"
													: this.state.series[obj.serie].dernierPaquet === "NA" ? "#5b5beb"
														: "white"), ...style
										}}></div>)
								}


								let arrDrill = (this.state.series[obj.serie].drill || ",").split(",").map(e => e != "" ? parseInt(e) : 0)

								if (arrDrill[0] > 0) {
									arrAudit.push(<div
										alt={"Verification Drill"}
										style={{
											backgroundColor: (this.state.series[obj.serie].verificationDrill === "OK" ? "#2fdb2f"
												: this.state.series[obj.serie].verificationDrill === "NOK" ? "red"
													: this.state.series[obj.serie].verificationDrill === "NA" ? "#5b5beb"
														: "white"), ...style
										}}>D1</div>)
								}
								if (arrDrill[1] > 0) {
									arrAudit.push(<div
										alt={"Verification Drill"}
										style={{
											backgroundColor: (this.state.series[obj.serie].verificationDrill2 === "OK" ? "#2fdb2f"
												: this.state.series[obj.serie].verificationDrill2 === "NOK" ? "red"
													: this.state.series[obj.serie].verificationDrill2 === "NA" ? "#5b5beb"
														: "white"), ...style
										}}>D2</div>)
								}

								// XPL Scan verification - show if serie was scanned in XPL
								arrAudit.push(<div
									alt={"Scan XPL"}
									title={this.state.xplSeries[obj.serie] ? 
										`Scanné par: ${this.state.xplSeries[obj.serie].operator || 'N/A'} le ${this.state.xplSeries[obj.serie].scanDate ? moment(this.state.xplSeries[obj.serie].scanDate).format('HH:mm:ss') : 'N/A'}` 
										: "Non scanné XPL"}
									style={{
										backgroundColor: this.state.xplSeries[obj.serie] ? "#2fdb2f" : "white",
										...style
									}}>XPL</div>)
							}
							return <tr key={key}
								onDoubleClick={() => {
									this.setState({ modalObj: obj })
								}}
								style={obj.placement && obj.placement.endsWith("-NS") ? { backgroundColor: "#f0f0f0", opacity: 0.7 } : {}}
							>
								<td
									style={this.state.series[obj.serie] ? (this.state.series[obj.serie].statusCoupe === "Waiting" ? { backgroundColor: "#ffafaf" } :
										this.state.series[obj.serie].statusCoupe === "In progress" ? { backgroundColor: "#f6ff6b" } :
											this.state.series[obj.serie].statusCoupe === "Complete" ? { backgroundColor: "#7bff6b" } :
												this.state.series[obj.serie].statusCoupe === "Incomplete" ? { backgroundColor: "#ffc46b" } :
													{}) : {}}
								>{obj.serie}</td>
								<td style={{ padding: 0, height: "100%" }}>
									<div style={{ display: "flex", width: "100%", height: "100% !important", minHeight: 51.5 }}>
										{arrAudit}
									</div>
								</td>
								<td>{obj.placement}{obj.placement && obj.placement.endsWith("-NS") && <span style={{color: "red", fontWeight: "bold", marginLeft: "5px"}}>[NS]</span>}</td>
								<td>{obj.dateDebut && moment(obj.dateDebut).format("HH:mm:ss")}</td>
								<td>{obj.dateFin && moment(obj.dateFin).format("HH:mm:ss")}</td>
								<td
									style={{ backgroundColor: obj.nbrPieces === undefined ? "#ff0000" : obj.compteur < obj.nbrPieces ? "#ffff00" : "#00ff00" }}
								>{obj.compteur}/{obj.nbrPieces}</td>
								<td>{this.convertMillisecondsToTimeStringV2(obj.running)}</td>
								<td>{this.convertMillisecondsToTimeStringV2(obj.interruption)}</td>
								<td>{obj.reperage != 0 && this.convertMillisecondsToTimeStringV2(obj.reperage)} {obj.reperage != 0 && <span>({<span style={obj.reperagePlus >= 0 ? { color: "#00ad00" } : { color: "#ff0000" }}>
									{this.convertMillisecondsToTimeStringV2(obj.reperagePlus)}
								</span>})</span>}</td>
								<td>{this.convertMillisecondsToTimeStringV2(obj.horsCycle)}</td>
							</tr>
						})}
					</tbody>
				</table>
			</div>}
		</div>
	}

	downloadCSV = () => {
		// download csv that is fill with this.state.rapportOverlap
		const replacer = (key, value) => value === null ? '' : value;
		// user the hearder that i have in my table
		const header = ["Zone", "Machine", "Placements", "Efficience", "Interruption Total", "Reperage", "Hors Cycle", "Efficience Minute", "Interruption Total Minute", "Reperage Minute", "Reperage +/-",
			"Hors Cycle Minute", "Hors Cycle +/-"]
		let arr = []
		this.state.machineArr.sort((a, b) => (a.zone.nom.localeCompare(b.zone.nom) || a.nom.localeCompare(b.nom)))
			.map((machine, key) => {
				if (this.state.data[machine.nom]) {
					let runningTotal = 0
					let interruptionTotal = 0
					let reperageTotal = 0
					let horsCycleTotal = 0
					let placementCount = 0
					let horsCycleTheorique = 0
					let reperageTheorique = 0
					if (this.state.data[machine.nom] && this.state.data[machine.nom].length > 0) {
						this.state.data[machine.nom].map(obj => {
							// Check if placement ends with "-NS" and exclude from efficiency calculations
							if (obj.placement && obj.placement.endsWith("-NS")) {
								// Move all time to horsCycleTotal (non-productive time)
								horsCycleTotal += (obj.running || 0) + (obj.interruption || 0) + (obj.reperage || 0) + (obj.horsCycle || 0);
							} else {
								// Normal calculation for productive placements
								if (obj.running) {
									runningTotal += obj.running
								}
								if (obj.interruption) {
									interruptionTotal += obj.interruption
								}
								if (obj.reperage && obj.reperage != 0) {
									reperageTotal += obj.reperage
									reperageTheorique += obj.reperagePlus
								}
								if (obj.horsCycle) {
									horsCycleTotal += obj.horsCycle
								}
								horsCycleTheorique += (30000 - obj.horsCycle)
							}
						})
						placementCount = this.state.data[machine.nom].filter(e => e.compteur > 0 && e.placement != null && !e.placement.toLowerCase().startsWith("test") && !e.placement.endsWith("-NS")).length
					}

					arr.push({
						"Zone": machine.zone.nom,
						"Machine": machine.nom,
						"Placements": placementCount,
						"Efficience": this.convertFloat(100 * runningTotal / moment.min(moment(this.state.date2), moment()).diff(moment(this.state.date1))),
						"Interruption Total": this.convertFloat(100 * interruptionTotal / moment.min(moment(this.state.date2), moment()).diff(moment(this.state.date1))),
						"Reperage": this.convertFloat(100 * reperageTotal / moment.min(moment(this.state.date2), moment()).diff(moment(this.state.date1))),
						"Hors Cycle": this.convertFloat(100 * horsCycleTotal / moment.min(moment(this.state.date2), moment()).diff(moment(this.state.date1))),
						"Efficience Minute": this.convertMillisecondsToMinutes(runningTotal),
						"Interruption Total Minute": this.convertMillisecondsToMinutes(interruptionTotal),
						"Reperage Minute": this.convertMillisecondsToMinutes(reperageTotal),
						"Reperage +/-": this.convertMillisecondsToMinutes(reperageTheorique),
						"Hors Cycle Minute": this.convertMillisecondsToMinutes(horsCycleTotal),
						"Hors Cycle +/-": this.convertMillisecondsToMinutes(horsCycleTheorique)
					})
				}
			})
		let csv = arr.map(row => header.map(fieldName => JSON.stringify(row[fieldName], replacer)).join(','));
		csv.unshift(header.join(','));
		csv = csv.join('\r\n');
		const blob = new Blob([csv], { type: 'text/csv' });
		const url = window.URL.createObjectURL(blob);
		const a = document.createElement('a');
		a.href = url;
		a.download = 'kpi ' + this.state.date + ' shift' + this.state.shift + ' ' + this.state.selectedZone + '.csv';
		a.click();
		window.URL.revokeObjectURL(url);
	}

	convertMillisecondsToMinutes = (ms) => {
		return this.convertFloat(ms / 60000)
	}


	render() {
		let zone = null
		return (
			<div id='kpi' ref={ref => this.bodyRef = ref}>
				<div style={{ display: "flex", justifyContent: "center", margin: "4 0" }}>
					<h1>KPI Machines</h1>
					<div className='col-3' style={{ fontSize: 29 }}>
						<Select classNamePrefix="rs"
							placeholder={"Zone..."} className='p-0'
							isClearable={true}
							value={(this.state.selectedZone)
								? { label: this.state.selectedZone, value: this.state.selectedZone }
								: null
							}
							options={[{ label: "All", value: "All" }, ...this.state.zoneArr.map(zone => { return { label: zone, value: zone } })]}
							onChange={(option) => {
								if (option) {
									this.stopLoop()
									if (option.value === "All") {
										this.setState({ selectedZone: option.value, machineArr: this.state.productionTableArr })
									} else {
										this.setState({ selectedZone: option.value, machineArr: this.state.productionTableArr.filter(element => element.zone.nom === option.value) })
									}
									setTimeout(() => {
										this.startLoop()
									}, 300)
								} else {
									this.setState({ selectedZone: null, machineArr: [] })
									this.stopLoop()
								}
							}}
						/>

					</div>
					<button onClick={() => { this.downloadCSV() }} className='btn btn-success ml-1'><FontAwesomeIcon icon={faFileCsv} /> Télécharger</button>

				</div>
				<div className='machine-card-container' style={{ backgroundColor: "white" }}>
					<div className='machine-info'>
						<div style={{ display: "flex", alignItems: "center", fontSize: 20, fontWeight: "bold" }}>
							Shift <select
								className='form-control' style={{ width: 50, height: 30, fontSize: 20, fontWeight: "bold", padding: "0 5" }}
								value={this.state.shift}
								onChange={(e) => {
									let shift = parseInt(e.target.value)
									let date = this.state.date
									let date1, date2;
									if (shift === 1) {
										date1 = moment(date).subtract(1, 'day').format('YYYY-MM-DD 21:55');
										date2 = moment(date).format('YYYY-MM-DD 05:45');
									} else if (shift === 2) {
										date1 = moment(date).format('YYYY-MM-DD 05:55');
										//if the day is friday, make the end at 13:30
										if (moment(date).day() === 5) {
											date2 = moment(date).format('YYYY-MM-DD 13:30');
										} else {
											date2 = moment(date).format('YYYY-MM-DD 13:45');
										}
									} else if (shift === 3) {
										if (moment(date).day() === 5) {
											date1 = moment(date).format('YYYY-MM-DD 14:05');
										} else {
											date1 = moment(date).format('YYYY-MM-DD 13:55');
										}
										date2 = moment(date).format('YYYY-MM-DD 21:45');
									}



									this.setState({ shift, date1, date2 }, () => {
										this.stopLoop()
										this.startLoop()
									})
								}}
							>
								<option value={1}>1</option>
								<option value={2}>2</option>
								<option value={3}>3</option>
							</select>
							- <input
								type="date" className='form-control' style={{ width: 170, height: 30, fontSize: 20, fontWeight: "bold" }}
								value={this.state.date}
								onChange={(e) => {
									let date = e.target.value
									let shift = this.state.shift
									let date1, date2;
									if (shift === 1) {
										date1 = moment(date).subtract(1, 'day').format('YYYY-MM-DD 21:55');
										date2 = moment(date).format('YYYY-MM-DD 05:45');
									} else if (shift === 2) {
										date1 = moment(date).format('YYYY-MM-DD 05:55');
										//if the day is friday, make the end at 13:30
										if (moment(date).day() === 5) {
											date2 = moment(date).format('YYYY-MM-DD 13:30');
										} else {
											date2 = moment(date).format('YYYY-MM-DD 13:45');
										}
									} else if (shift === 3) {
										if (moment(date).day() === 5) {
											date1 = moment(date).format('YYYY-MM-DD 14:05');
										} else {
											date1 = moment(date).format('YYYY-MM-DD 13:55');
										}
										date2 = moment(date).format('YYYY-MM-DD 21:45');
									}

									this.setState({ date, date1, date2 }, () => {
										this.stopLoop()
										this.startLoop()
									})
								}}
							/> </div>
						<div style={{ whiteSpace: "nowrap", textAlign: "center" }}>{this.state.date1 && this.state.date1.substring(11)} <FontAwesomeIcon icon={faArrowRight} /> {this.state.date2 && moment.min(moment(this.state.date2), moment()).format("YYYY-MM-DD HH:mm").substring(11)}</div>
					</div>
					<div style={{ flex: 1 }}></div>
					<div className='machine-info-center'>
						Placements
					</div>
					<div className='machine-info-center'>
						Efficience
					</div>
					<div className='machine-info-center'>
						Interruption total
					</div>
					<div className='machine-info-center'>
						Repérage
					</div>
					<div className='machine-info-center' >
						Hors cycle
					</div>
				</div>
				{this.state.machineArr
					.sort((a, b) => (a.zone.nom.localeCompare(b.zone.nom) || a.nom.localeCompare(b.nom)))
					.map((element, index) => {
						// render each machine in a card horizontally with other information
						if ((zone == null || element.zone.nom !== zone) && this.state.selectedZone === "All") {
							zone = element.zone.nom
							return <>
								<h2 style={{ textAlign: "center", backgroundColor: "grey", color: "white", padding: 5 }}>{zone}</h2>
								{this.renderDetailsMachine(element, index)}
							</>
						}
						return this.renderDetailsMachine(element, index)
					})
				}
				{this.renderModalSerie()}
				{this.renderModalIntervention()}

			</div>
		)

	}

}