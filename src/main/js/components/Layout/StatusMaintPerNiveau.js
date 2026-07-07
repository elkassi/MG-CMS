import axios from 'axios';
import moment from 'moment';
import React, { Component } from 'react'
import Select from "react-select";
import "../../styles/StatusMaintPerNiveau.scss";
import { Modal } from 'react-bootstrap'
import Switch from "react-switch";
import { faCheck, faExclamationTriangle, faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { spliceTestMarker } from '../../metadata';
import logo from '../../assets/images/lear_logo.png'



export default class StatusMaintPerNiveau extends Component {

	constructor(props) {
		super(props);
		this.state = {
			data: [],
			date: moment().add(2, 'hours').add(10, 'minutes').format("YYYY-MM-DD"),
			shift: this.getShift(moment().add(2, 'hours').add(10, 'minutes')),
			category: null,
			configList: [],
			categoryList: [],
			arr: [],
			totalTask: 0,
			productionTableList: [],
			zoneList: [],
			selectedMachine: null,
			dataMachineStopped: [],
			scheduleMachineList: [],
			spliceMarkerLog: [],
			calibrationLog: [],
			controlTableList: [],
		}
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
		axios.get("/api/productionTable/list")
			.then(res => {
				let zoneList = []
				res.data.map(e => {
					if (!zoneList.includes(e.zone.nom)) {
						zoneList.push(e.zone.nom)
					}
				})
				this.setState({ productionTableList: res.data, zoneList })
			})
			.catch(err => {
				console.log(err)
			})
		axios.get("/api/controlTable/list")
			.then(res => {
				this.setState({ controlTableList: res.data })
			})
		axios.get("/api/firstCheckConfig/list")
			.then(response => {
				let arr = [];
				response.data.forEach(element => {
					if (!arr.includes(element.category)) {
						arr.push(element.category);
					}
				});
				axios.get("/api/auditQualiteConfig/list")
					.then(responseQualite => {
						this.setState({ auditQualiteConfigList: responseQualite.data, configList: [...response.data, ...responseQualite.data.map(e => { return { ...e, category: "Qualité" } })], categoryList: arr, category: arr.length > 0 ? arr[0] : null });
						this.getData()
					})
					.catch(error => {
						console.log(error);
						this.setState({ configList: response.data, categoryList: arr, category: arr.length > 0 ? arr[0] : null });
						this.getData()
					})

			})
			.catch(error => {
				console.log(error);
				this.setState({ configList: [] });
			})
	}

	getData = () => {
		this.setState({ data: null });
		if (this.state.category === "Qualité") {
			axios.get(`/api/auditQualite/filtre?date=${this.state.date}&shift=${this.state.shift}`)
				.then(response => {
					this.setState({ data: response.data.map(e => { return { ...e, machine: e.tableControle, category: this.state.category } }) });
					//firstCheckMachineStopped
					axios.get(`/api/firstCheckMachineStopped/filtre?date=${this.state.date}&category=${this.state.category}&shift=${this.state.shift}`)
						.then(response2 => {
							this.setState({ dataMachineStopped: response2.data });
						})
					axios.get(`/api/query/scheduleMachine?date=${this.state.date}&shift=${this.state.shift}`)
						.then(response3 => {
							this.setState({ scheduleMachineList: response3.data });
						})

				})
				.catch(error => {
					console.log(error);
					this.setState({ data: [] });
				});

		} else {
			axios.get(`/api/firstCheck/filtre?date=${this.state.date}&category=${this.state.category}&shift=${this.state.shift}`)
				.then(response => {
					this.setState({ data: response.data });
					//firstCheckMachineStopped
					axios.get(`/api/firstCheckMachineStopped/filtre?date=${this.state.date}&category=${this.state.category}&shift=${this.state.shift}`)
						.then(response2 => {
							this.setState({ dataMachineStopped: response2.data });
						})
					axios.get(`/api/query/scheduleMachine?date=${this.state.date}&shift=${this.state.shift}`)
						.then(response3 => {
							this.setState({ scheduleMachineList: response3.data });
						})

					if (this.state.category === "Matelassage") {
						axios.get(`/api/calibrationLog/findByDate?date=${this.state.date}&shift=${this.state.shift}`)
							.then(response3 => {
								this.setState({
									calibrationLog: response3.data.map(e => {
										if (e.stationNumber.startsWith("A")) {
											return { ...e, machine: "A" + e.stationNumber }
										} else if (e.stationNumber.startsWith("B")) {
											return { ...e, machine: "B" + e.stationNumber }
										}
										 else {
											return { ...e, machine: e.stationNumber }
										 }
									})
								})
							})
						axios.get(`/api/spliceMarkerLog/findTestsInShift?date=${this.state.date}&shift=${this.state.shift}`)
							.then(response4 => {
								this.setState({
									spliceMarkerLog: response4.data.map(e => {
										if (e.stationNumber.startsWith("A")) {
											return { ...e, machine: "A" + e.stationNumber }
										} else if (e.stationNumber.startsWith("B")) {
											return { ...e, machine: "B" + e.stationNumber }
										} else {
											return { ...e, machine: e.stationNumber }
										}
									})
								});
							})
					}
				})
				.catch(error => {
					console.log(error);
					this.setState({ data: [] });
				});
		}
	}

	renderTable = () => {
		if (this.state.category === "Qualité") {
			return <div className="table-responsive">
				<table className="table table-bordered">
					<thead>
						<tr>
							<th style={{ width: 210, fontSize: 25 }}>Zone</th>
							<th style={{ fontSize: 25 }}>Machines</th>
						</tr>
					</thead>
					<tbody>
						<tr>
							<td style={{ fontSize: 25 }}>Zone A</td>
							<td style={{ padding: 0 }}>
								<div className='d-flex flex-wrap'>
									{this.state.controlTableList.filter(ct => ct.poste.startsWith("TV A")).map(ct => <div className='hover-elem'
										key={"pd-" + ct.poste}
										style={{ margin: "6 0 6 10", padding: "5 10", border: this.getBorderStyle(ct.poste), borderRadius: 5, fontSize: 25, backgroundColor: this.getColor(ct.poste) }}
										onClick={() => { this.setState({ selectedMachine: ct.poste }) }}
									>
										{ct.poste}
									</div>)}
								</div>
							</td>
						</tr>
						<tr>
							<td style={{ fontSize: 25 }}>Zone B</td>
							<td style={{ padding: 0 }}>
								<div className='d-flex flex-wrap'>
									{this.state.controlTableList.filter(ct => ct.poste.startsWith("TV B")).map(ct => <div className='hover-elem'
										key={"pd-" + ct.poste}
										style={{ margin: "6 0 6 10", padding: "5 10", border: this.getBorderStyle(ct.poste), borderRadius: 5, fontSize: 25, backgroundColor: this.getColor(ct.poste) }}
										onClick={() => { this.setState({ selectedMachine: ct.poste }) }}
									>
										{ct.poste}
									</div>)}
								</div>
							</td>
						</tr>
						<tr>
							<td style={{ fontSize: 25 }}>Zone C</td>
							<td style={{ padding: 0 }}>
								<div className='d-flex flex-wrap'>
									{this.state.controlTableList.filter(ct => ct.poste.startsWith("TV C")).map(ct => <div className='hover-elem'
										key={"pd-" + ct.poste}
										style={{ margin: "6 0 6 10", padding: "5 10", border: this.getBorderStyle(ct.poste), borderRadius: 5, fontSize: 25, backgroundColor: this.getColor(ct.poste) }}
										onClick={() => { this.setState({ selectedMachine: ct.poste }) }}
									>
										{ct.poste}
									</div>)}
								</div>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		}

		return <div className="table-responsive">
			<table className="table table-bordered">
				<thead>
					<tr>
						<th style={{ width: 210, fontSize: 25 }}>Zone</th>
						<th style={{ fontSize: 25 }}>Machines</th>
					</tr>
				</thead>
				<tbody>
					{this.state.zoneList.map(zone => {
						let arrPd = this.state.productionTableList.filter(pd => pd.zone.nom == zone && (
							(this.state.category === "Matelassage" && pd.pcMatelassage != "NA") // && pd.machineType.name != "DIE"
							|| (this.state.category === "Coupe" && pd.pcCoupe != "NA" && ["Lectra", "Lectra IP6", "Gerber", "DIE"].includes(pd.machineType.name))
							|| (this.state.category === "Picking" && pd.pcCoupe != "NA" && ["Lectra", "Lectra IP6", "Gerber"].includes(pd.machineType.name))
						))
						if (arrPd.length === 0) return null
						return <tr key={"zone-" + zone}>
							<td style={{ fontSize: 25 }}>{zone}</td>
							<td style={{ padding: 0 }}>
								<div style={{ display: "flex", flexWrap: "wrap" }}>
									{arrPd.sort((a, b) => a.nom.localeCompare(b.nom))
										.map(pd => {
											let objMachineStopped = this.state.dataMachineStopped.find(e => e.machine === pd.nom)

											return <div className='hover-elem'
												key={"pd-" + pd.nom}
												style={{ margin: "6 0 6 10", padding: "5 10", border: this.getBorderStyle(pd.nom), borderRadius: 5, fontSize: 25, backgroundColor: this.getColor(pd.nom) }}
												onClick={() => { this.setState({ selectedMachine: pd.nom }) }}
											>
												<span style={(objMachineStopped && objMachineStopped.forPls && this.state.category === "Matelassage" && ["Lectra", "Lectra IP6"].includes(pd.machineType.name)) ? { position: "relative", marginRight: 10 } : { position: "relative" }}>{pd.nom}{((objMachineStopped && objMachineStopped.forPls) || pd.forPls === true) && <span class="circle-div">PLS</span>}</span> {this.state.category === "Matelassage" && ["Lectra", "Lectra IP6"].includes(pd.machineType.name) && this.renderAlertIcon(pd.nom)}
												{pd.autorisationAirbag && <div class="up">
													<p>LGK-CC09</p>
												</div>}
											</div>
										})}
								</div>
							</td>
						</tr>
					})}
				</tbody>
			</table>
		</div>
	}

	renderAlertIcon = (machine) => {
		let arrSplice = this.state.spliceMarkerLog.filter(e => e.machine === machine && (e.numberOfLayersDone === 0 || e.numberOfLayersDone == null || ((e.fabricTypeLength / e.numberOfLayersDone) - e.markerLengthBrut) > 0 || ((e.fabricTypeLength / e.numberOfLayersDone) - e.markerLengthBrut) < -5))
		let arrCalibrage = this.state.calibrationLog.filter(e => e.machine === machine)
		let errorArr = []
		spliceTestMarker.map(marker => {
			let arr = this.state.spliceMarkerLog.filter(e => e.machine === machine && e.marker.toUpperCase().endsWith(marker))
			if (arr.length > 0) {
				let arrGood = arr.filter(e => e.numberOfLayersDone !== 0 && e.numberOfLayersDone != null && (e.fabricTypeLength / e.numberOfLayersDone) - e.markerLengthBrut <= 0 && (e.fabricTypeLength / e.numberOfLayersDone) - e.markerLengthBrut >= -5)
				if (arrGood.length === 0) {
					arr.map(e => {
						errorArr.push(marker + " : " + (e.numberOfLayersDone === 0 || e.numberOfLayersDone == null ? "non testé" : (this.convertFloat((e.fabricTypeLength / e.numberOfLayersDone) - e.markerLengthBrut))))
					})
				}
			} else {
				errorArr.push(marker + " n'est pas testé")
			}
		})
		if (errorArr.length > 0) {
			if (arrCalibrage.length > 0) {
				return <span className='crud-tooltip'>
					<FontAwesomeIcon icon={faExclamationTriangle} color="#0035ff" style={{ paddingTop: "0px", zIndex: 7 }} />
					<span className="crud-tooltiptext" style={{ backgroundColor: "#0035ff", whiteSpace: "pre-line" }}>{errorArr.map((e, ind) => <><span style={{ whiteSpace: "nowrap" }}>{e}</span>{ind != errorArr.length - 1 && <br />}</>)}</span>
				</span>

			} else {
				return <span className='crud-tooltip'>
					<FontAwesomeIcon icon={faExclamationTriangle} color="#c20000" style={{ paddingTop: "0px", zIndex: 7 }} />
					<span className="crud-tooltiptext" style={{ backgroundColor: "#c20000", whiteSpace: "pre-line" }}>{errorArr.map((e, ind) => <><span style={{ whiteSpace: "nowrap" }}>{e}</span>{ind != errorArr.length - 1 && <br />}</>)}</span>
				</span>
			}
		}
		return;

	}

	getBorderStyle = (machine) => {
		if (this.state.data) {
			let arr = this.state.data.filter(e => e.machine === machine).map(e => e.decision)
			if (arr.length === 0) {
				return "2px #F00 solid"
			}
		}
		return "2px #000 solid"
	}

	getColor = (machine) => {
		if (this.state.data && this.state.configList) {
			let productionTableObj = this.state.productionTableList.find(e => e.nom === machine)
			let category = this.state.category
			if (this.state.category == "Matelassage") {
				switch (productionTableObj.machineType.name) {
					case "Gerber":
						category = "Gerber Matelassage"
						break;
					case "LASER-DXF":
						category = "DXF"
						break;
					case "LASER-LSR":
						category = "LSR"
						break;
					case "DIE":
						category = "DIE Matelassage"
						break;
					default:
						category = "Matelassage"
						break;
				}
			}
			if (this.state.category === "Coupe") {
				switch (productionTableObj.machineType.name) {
					case "Gerber":
						category = "Gerber Coupe"
						break;
					case "DIE":
						category = "DIE"
						break;
					default:
						category = "Coupe"
						break;
				}
			}
			// if (this.state.category === "Matelassage") {
			// 	let arrSplice = this.state.spliceMarkerLog.filter(e => e.machine === machine && (e.numberOfLayersDone === 0 || e.numberOfLayersDone == null || ((e.fabricTypeLength / e.numberOfLayersDone) - e.markerLengthBrut) > 0 || ((e.fabricTypeLength / e.numberOfLayersDone) - e.markerLengthBrut) < -5))
			// 	let arrCalibrage = this.state.calibrationLog.filter(e => e.machine === machine)
			// 	if (arrSplice.length > 0) {
			// 		if (arrCalibrage.length > 0) {
			// 			return "#57acff"
			// 		} else {
			// 			return "#ff9c9c"
			// 		}
			// 	}
			// }


			let numberTasks = this.state.configList.filter(e => e.category === category).length
			let arr = this.state.data.filter(e => e.machine === machine).map(e => e.decision)
			let arrNonValide = this.state.data.filter(e => e.machine === machine && e.actionBy == null && e.decision === "NOK")
			let objMachineStopped = this.state.dataMachineStopped.find(e => e.machine === machine)

			if (objMachineStopped && objMachineStopped.forPls !== true) {
				return "#cbcbcb"
			}
			if (this.state.scheduleMachineList && !(objMachineStopped && objMachineStopped.forPls === true)) {
				let objScheduleMachine = this.state.scheduleMachineList.find(e => e.ligne === machine)
				if (objScheduleMachine) {
					return "#cbcbcb"
				}
			}
			if (arr.length === 0) {
				return "white"
			}
			if (arr.length < numberTasks) {
				return "#ffff4d";
			}

			if (arr.includes("NOK")) {
				if (arrNonValide.length > 0) {
					return "#ff9c9c"
				} else {
					return "#57acff"
				}
			}
			return "#00ff00"
		}
	}

	convertFloat = (float) => {
		//check if the float is a number
		if (!float || isNaN(float)) {
			return float
		}
		return parseFloat(parseFloat(float).toFixed(2))
	}

	renderHeaderForm = (category) => {
		let title = ""
		let formId = ""
		let ind = ""
		switch (category) {
			case "Matelassage":
				title = "Maintenance 1er niveau & TPM - Matelassage"
				formId = "FR MT 62"
				ind = "O"
				break;
			case "Gerber Matelassage":
				title = "Maintenance 1er niveau & TPM - Gerber - Matelassage"
				// formId = "FR MT 63"
				// ind = "O"
				break;
			case "DXF":
				title = "Maintenance 1er niveau & TPM - Machine de coupe laser"
				formId = "FR MT 65"
				ind = "E"
				break;
			case "LSR":
				title = "Maintenance 1er niveau & TPM - Machine de coupe laser"
				formId = "FR MT 65"
				ind = "E"
				break;
			case "DIE Matelassage":
				title = "Maintenance préventive 1er niveau-Matelassage Automatique-Presse"
				formId = "FR MT 33/34"
				ind = "C"
				break;
			case "DIE":
				title = "Maintenance préventive 1er niveau-COUPE Automatique-Presse"
				formId = "FR MT 35"
				ind = "C"
				break;
			case "Gerber Coupe":
				title = "Maintenance 1er niveau & TPM - Coupe - Gerber "
				formId = "FR MT 95"
				ind = "I"
				break;
			case "Coupe":
				title = "Maintenance 1er niveau & TPM - Lectra"
				formId = "FR MT 116"
				ind = "O"
				break;
			case "Qualité":
				title = "Audit Poste contrôle Qualité"
				formId = "FR QS 94-1"
				ind = ""
				break;
			default:
				title = "Maintenance 1er niveau & TPM - " + this.state.category
				break;
		}
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
				<h4 className="text-center mt-2">{title}
					{["LSR", "DXF"].includes(category) && <span>
						{<div class="up">
							<p>LGK-CC09</p>
						</div>}
					</span>}
				</h4>
			</div>
			<div className="col-3 border border-dark">
				<p className="text-center"  style={{margin: 0}}>{formId}
					<br/><span className="text-center">{ind.length > 0 && `Ind:${ind}`}</span>
				</p>
				
			</div>
		</div>
	}


	renderDetails = () => {
		if (this.state.selectedMachine === null) {
			return null
		}

		let productionTableObj = this.state.productionTableList.find(e => e.nom === this.state.selectedMachine)
		let category = this.state.category
		if (this.state.category == "Matelassage") {
			switch (productionTableObj.machineType.name) {
				case "Gerber":
					category = "Gerber Matelassage"
					break;
				case "LASER-DXF":
					category = "DXF"
					break;
				case "LASER-LSR":
					category = "LSR"
					break;
				case "DIE":
					category = "DIE Matelassage"
					break;
				default:
					category = "Matelassage"
					break;
			}
		}
		if (this.state.category === "Coupe") {
			switch (productionTableObj.machineType.name) {
				case "Gerber":
					category = "Gerber Coupe"
					break;
				case "DIE":
					category = "DIE"
					break;
				default:
					category = "Coupe"
					break;
			}
		}

		let arr = this.state.data.filter(e => e.machine === this.state.selectedMachine).sort((a, b) => a.taskNumber - b.taskNumber)
		let arrTasks = arr.filter(e => e.category === category).map(e => e.task)
		let configList = this.state.configList.filter(e => e.category === category && !arrTasks.includes(e.task))
		console.log({ arr, arrTasks, configList, category, productionTableObj })
		let objMachineStopped = this.state.dataMachineStopped.find(e => e.machine === this.state.selectedMachine)

		let arrSplice = this.state.spliceMarkerLog.filter(e => e.machine === this.state.selectedMachine)
		let arrCalibrage = this.state.calibrationLog.filter(e => e.machine === this.state.selectedMachine)
		let objScheduleMachine = this.state.scheduleMachineList.find(e => e.ligne === this.state.selectedMachine)

		return <Modal
			show={this.state.selectedMachine !== null}
			onHide={() => this.setState({ selectedMachine: null })}
			// className=""
			dialogClassName="modal-75w"
			centered
		>
			{this.state.selectedMachine && <div style={{
				maxHeight: "calc(95vh - 30px)",
				overflow: "auto",
				padding: "8 15 0",
				display: 'flex', flexDirection: "column"
			}}>

				{this.renderHeaderForm(category)}
				<h3 className='text-center'>
					{this.state.selectedMachine} <Switch checked={objMachineStopped == null}
						className="react-switch mt-1" offColor="#F00" height={25} width={70}
						onChange={(checked) => {
							//remove objMachineStopped from dataMachineStopped after delete axios
							if (checked) {
								axios.delete("/api/firstCheckMachineStopped/" + objMachineStopped.id)
									.then(res => {
										this.setState({ dataMachineStopped: this.state.dataMachineStopped.filter(e => e.id !== objMachineStopped.id) })
									})
							} else {
								axios.post("/api/firstCheckMachineStopped", {
									machine: this.state.selectedMachine,
									date: this.state.date,
									shift: this.state.shift,
									category: this.state.category
								})
									.then(res => {
										this.setState({ dataMachineStopped: [...this.state.dataMachineStopped, res.data] })
									})
							}
						}}
					/>
					{objMachineStopped && <button className={objMachineStopped.forPls ? "btn btn-danger btn-sm" : "btn btn-outline-danger btn-sm"}
						style={{ marginBottom: 8, marginLeft: 8, padding: "3 12" }}
						onClick={() => {
							axios.post(`/api/firstCheckMachineStopped`, {
								...objMachineStopped,
								forPls: objMachineStopped.forPls === true ? false : true,
							})
								.then(res => {
									this.setState({ dataMachineStopped: [...this.state.dataMachineStopped.filter(e => e.id !== objMachineStopped.id), res.data] })
								})

						}}>
						{objMachineStopped.forPls ? <FontAwesomeIcon icon={faCheck} /> : <FontAwesomeIcon icon={faTimes} />} PLS
					</button>}
				</h3>
				{objMachineStopped != null && (objMachineStopped.forPls
					? <i style={{ textAlign: "center", fontSize: 16 }} >CMS WEB: {"Machine PLS par " + objMachineStopped.createdBy + " : " + objMachineStopped.createdAt}</i>
					: <i style={{ textAlign: "center", fontSize: 16 }} >CMS WEB: {"Machine arrêtée par " + objMachineStopped.createdBy + " : " + objMachineStopped.createdAt}</i>
				)}
				{objScheduleMachine != null && <i style={{ textAlign: "center", fontSize: 16 }} >CMS: {"Machine arrêtée par " + objScheduleMachine.userName_Schedule_Machine + " : " + objScheduleMachine.createdDate_Schedule_Machine + " " + objScheduleMachine.createdHour_Schedule_Machine}</i>}
				<hr />
				<div style={{ display: "flex", justifyContent: "space-between" }}>
					{this.state.category == "Matelassage" && ["Lectra", "Lectra IP6"].includes(productionTableObj.machineType.name) && <div style={{ width: "49%" }}>
						<h3 className="text-center">Tests du système splice</h3>
						<table className='table table-splice'>
							<thead>
								<tr>
									<th>Placement</th>
									<th>Longueur théorique</th>
									{/* <th>Nombre de couches</th> */}
									<th>Longueur</th>
									<th>Diff</th>
									<th>Date</th>
								</tr>
							</thead>
							<tbody>
								{arrSplice.length === 0 ? <tr><td colSpan="4">No Splice Marker Log</td></tr> :
									arrSplice
										.sort((a, b) => a.createdDate - b.createdDate)
										.map((e, i) => {
											return <tr key={i}
												style={(e.numberOfLayersDone === 0 || e.numberOfLayersDone == null || ((e.fabricTypeLength / e.numberOfLayersDone) - e.markerLengthBrut) > 0 || ((e.fabricTypeLength / e.numberOfLayersDone) - e.markerLengthBrut) < -5) ? { backgroundColor: "#ffc4c4" }
													: { backgroundColor: "#9dff9d" }
												}
											>
												<td>{e.marker}</td>
												<td>{e.markerLengthBrut}</td>
												{/* <td>{e.numberOfLayersDone}</td> */}
												<td>{this.convertFloat(e.fabricTypeLength / e.numberOfLayersDone)}</td>
												<td style={{}}>{e.numberOfLayersDone > 0 ? this.convertFloat((e.fabricTypeLength / e.numberOfLayersDone) - e.markerLengthBrut) : 0}</td>
												<td>{e.createdAt && moment(e.createdAt).format("YYYY-MM-DD HH:mm")}</td>
											</tr>
										})}
							</tbody>
						</table>
					</div>}
					{this.state.category == "Matelassage" && ["Lectra", "Lectra IP6"].includes(productionTableObj.machineType.name) && <div style={{ width: "49%" }}>
						<h3 className="text-center">
							Calibrages
						</h3>
						<table className='table table-calibrage'>
							<thead>
								<tr>
									<th>UserName</th>
									<th>Date</th>
								</tr>
							</thead>
							<tbody>
								{arrCalibrage.length === 0 ? <tr><td colSpan="2" style={{ textAlign: "center" }}><i>Aucune Calibration.</i></td></tr> :
									arrCalibrage.map((e, i) => {
										return <tr key={i}>
											<td>{e.userName}</td>
											<td>{e.createdAt}</td>
										</tr>
									})}
							</tbody>
						</table>
					</div>}
				</div>
				<hr />
				<div>
					<table className=' table table-maint'>
						<thead>
							<tr>
								<th>N°</th>
								<th>Tâche</th>
								{this.state.category !== "Qualité" && <th>Image</th>}
								<th>Commentaire</th>
								<th>Decision</th>
								<th style={{ width: 450 }}>Action</th>
							</tr>
						</thead>
						<tbody>
							{arr.map((e, i) => {
								return <tr key={i}>
									<td>{e.taskNumber}</td>
									<td>{e.task}
										{e.taskDescription && [<br />, <i>{e.taskDescription}</i>]}
										{e.matricule && [<br />, <i style={{fontSize:18}}>Matricule : {e.matricule}</i>]}
										{e.createdBy && [<br />,  <i style={{fontSize:18}}>Matricule : {e.createdBy}</i>]}
									</td>
									{this.state.category !== "Qualité" && <td><img src={`/api/file/${e.taskImage}`} height="170px" /></td>}
									<td>
										<pre>{e.comment}</pre>
									</td>
									<td style={{ margin: 0, backgroundColor: this.getColorByDecision(e.decision, e.type), textAlign: "center", verticalAlign: "middle", fontWeight: "bold", height: 100 }}>
										{e.decision}
									</td>
									<td>
										{e.actionBy
											?
											<>
												<pre>{e.action}</pre>
												<>{e.actionBy}<br />{e.actionDate}</>
											</>
											: <>
												{e.decision === "NOK" && <textarea class="form-control input-sm" value={e.action} rows="5" onChange={(event) => {
													let newArr = [...this.state.data]
													let ind = newArr.findIndex(el => el.id === e.id)
													newArr[ind].action = event.target.value
													this.setState({ data: newArr })

												}}
												></textarea>}
												{e.decision === "NOK" && <button className="btn btn-primary" onClick={() => {
													if (e.action && e.action.trim() !== "") {
														if (this.state.category === "Qualité") {
															axios.post("/api/auditQualite/validateAction", e)
																.then(res => {
																	let newArr = [...this.state.data]
																	let ind = newArr.findIndex(el => el.id === e.id)
																	newArr[ind] = res.data
																	this.setState({ data: newArr })
																})
														} else {
															axios.post("/api/firstCheck/validateAction", e)
																.then(res => {
																	let newArr = [...this.state.data]
																	let ind = newArr.findIndex(el => el.id === e.id)
																	newArr[ind] = res.data
																	this.setState({ data: newArr })
																})
														}
													}
												}}>Valider</button>}
											</>}
									</td>
								</tr>
							})}
						</tbody>
					</table>
				</div>
				{configList && configList.length > 0 && <>
					<hr />
					<h3 className='text-center'>{configList.length} tâches restantes : </h3>
					<div>
						<table className=' table table-maint'>
							<thead>
								<tr>
									<th>N°</th>
									<th>Tâche</th>
									{this.state.category !== "Qualité" && <th>Image</th>}
								</tr>
							</thead>
							<tbody>
								{configList.map((e, i) => {
									return <tr key={i}>
										<td>{e.taskNumber}</td>
										<td>{e.task}</td>
										{this.state.category !== "Qualité" && <td><img src={`/api/file/${e.taskImage}`} height="200px" /></td>}
									</tr>
								})}
							</tbody>
						</table>
					</div>
				</>}
			</div>}
		</Modal>
	}

	getColorByDecision = (decision, type) => {
		if (type === "nombre" || type === "text") {
			return "#ebebeb"
		}
		if (decision === "NOK") {
			return "#ff9c9c"
		}
		if (decision === "OK") {
			return "#00ff00"
		}
		if (decision === "NA") {
			//blue 
			return "#6f6fff"
		}
		return "#ffff4d"
	}

	renderColorQuide = () => {
		return <>
			<div style={{ display: "flex", justifyContent: "center", margin: "12 0", fontSize: 20 }}>
				<div style={{ display: "flex", alignItems: "center", margin: "0 8" }}>
					<div style={{ width: 30, height: 30, backgroundColor: "#00ff00", border: "2px solid #000", borderRadius: "5px" }}></div>
					<span style={{ margin: "0 8" }}>OK</span>
				</div>
				<div style={{ display: "flex", alignItems: "center", margin: "0 8" }}>
					<div style={{ width: 30, height: 30, backgroundColor: "#ff9c9c", border: "2px solid #000", borderRadius: "5px" }}></div>
					<span style={{ margin: "0 8" }}>NOK</span>
				</div>
				<div style={{ display: "flex", alignItems: "center", margin: "0 8" }}>

					<div style={{ width: 30, height: 30, backgroundColor: "#57acff", border: "2px solid #000", borderRadius: "5px" }}></div>
					<span style={{ margin: "0 8" }}>Corrigé</span>
				</div>
				<div style={{ display: "flex", alignItems: "center", margin: "0 8" }}>
					<div style={{ width: 30, height: 30, backgroundColor: "#ffff4d", border: "2px solid #000", borderRadius: "5px" }}></div>
					<span style={{ margin: "0 8" }}>Incomplet </span>
				</div>
				<div style={{ display: "flex", alignItems: "center", margin: "0 8" }}>
					<div style={{ width: 30, height: 30, backgroundColor: "#cbcbcb", border: "2px solid #000", borderRadius: "5px" }}></div>
					<span style={{ margin: "0 8" }}>Non planifié</span>
				</div>
				<div style={{ display: "flex", alignItems: "center", margin: "0 8" }}>
					<div style={{ width: 30, height: 30, backgroundColor: "#fff", border: "2px solid #F00", borderRadius: "5px" }}></div>
					<span style={{ margin: "0 8" }}>Non fait</span>
				</div>
			</div>
			<div style={{ display: "flex", justifyContent: "center", margin: "12 0", fontSize: 20 }}>
				<div style={{ display: "flex", alignItems: "center", margin: "0 8" }}>
					<FontAwesomeIcon icon={faExclamationTriangle} style={{ color: "#c20000", fontSize: 22 }} />
					<span style={{ margin: "0 8" }}>Problème Splice</span>
				</div>
				<div style={{ display: "flex", alignItems: "center", margin: "0 8" }}>
					<FontAwesomeIcon icon={faExclamationTriangle} style={{ color: "#0035ff", fontSize: 22 }} />
					<span style={{ margin: "0 8" }}>Splice calibré</span>
				</div>
			</div>
		</>
	}


	render() {
		return (
			<div>
				<h2 className="text-center" style={{ display: "flex", justifyContent: "center", margin: "12 0" }}>
					<span style={{ margin: "5 0" }}>Status Maint. 1er Niveau</span>
					<input
						style={{ width: 240, padding: "0 5" }}
						type="date"
						value={this.state.date} onChange={(e) => this.setState({ date: e.target.value, data: [], dataMachineStopped: [], calibrationLog: [], spliceMarkerLog: [], scheduleMachineList: [] })}
					/>
					<select
						className='form-control' style={{ width: 70, height: 48, fontSize: 32, fontWeight: "bold", padding: "0 5", marginLeft: 8 }}
						value={this.state.shift}
						onChange={(e) => {
							let shift = parseInt(e.target.value)
							let date = this.state.date
							let date1, date2;
							if (shift === 1) {
								date1 = moment(date).subtract(1, 'day').hour(22).format('YYYY-MM-DD 21:55');
								date2 = moment(date).hour(6).format('YYYY-MM-DD 05:45');
							} else if (shift === 2) {
								date1 = moment(date).hour(6).format('YYYY-MM-DD 05:55');
								date2 = moment(date).hour(14).format('YYYY-MM-DD 13:45');
							} else if (shift === 3) {
								date1 = moment(date).hour(14).format('YYYY-MM-DD 13:55');
								date2 = moment(date).hour(22).format('YYYY-MM-DD 21:45');
							}
							this.setState({ shift, date1, date2, data: [], dataMachineStopped: [], calibrationLog: [], spliceMarkerLog: [], scheduleMachineList: [] })
						}}
					>
						<option value={1}>1</option>
						<option value={2}>2</option>
						<option value={3}>3</option>
					</select>
					<Select classNamePrefix="rs"
						placeholder={""} className='col-2 p-0 ml-2 '
						isClearable={false}
						value={this.state.category ? { value: this.state.category, label: this.state.category } : null}
						onChange={(e) => this.setState({ category: e.value, data: [], dataMachineStopped: [], calibrationLog: [], spliceMarkerLog: [], scheduleMachineList: [] })}
						options={[
							{ value: "Matelassage", label: "Matelassage" },
							{ value: "Coupe", label: "Coupe" },
							{ value: "Picking", label: "Picking" },
							{ value: "Qualité", label: "Qualité" },
						]}
					/>
					<button className="btn btn-primary ml-2" onClick={this.getData}>Rechercher</button>
				</h2>
				{this.renderColorQuide()}
				{this.renderTable()}
				{this.renderDetails()}

			</div >
		)
	}
}
