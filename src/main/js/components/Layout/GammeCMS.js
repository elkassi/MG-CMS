import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faArrowRight, faArrowRotateBack, faArrowUp, faCheck, faEye, faPlus, faPrint, faSave, faSpinner, faTimes } from '@fortawesome/free-solid-svg-icons';

import axios from 'axios';
import React, { Component } from 'react'
import { Modal } from 'react-bootstrap';
import ReactToPrint from "react-to-print";
import GammePn from './GammePn';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import logo from '../../assets/images/lear_logo.png'
import moment from 'moment';
import Select from "react-select";

class GammeCMS extends Component {

	constructor(props) {
		super(props);
		this.state = {
			sequence: '',
			cmsData: [],
			data: [],
			selectedBoxs: [],
			errors: {},
			selectedPartnumber: null,
			modalDetail: null,
			cuttingRequest: null,
			loadingPrintSeries: [],
			loadingMessage: null,
			modificationNeeded: [],
			arrGoodExtraSeries: [],
			loadingExtraSeries: false,
			diviseLoading: false,
			optionnals: {},
			planning: null,
			serieValidationResults: [],
			showAddSerieModal: false,
			newSerieToAdd: null
		}
	}

	componentDidMount() {
		//check if there is param called sequence in url like /gammeCMS?sequence=123
		const urlParams = new URLSearchParams(window.location.search);
		let sequence = urlParams.get('sequence');
		// check also of in props there is a sequence then replace the state with that sequence
		if (this.props.sequence) {
			sequence = this.props.sequence;
		}
		if (sequence) {
			this.setState({ sequence, data: [], selectedBoxs: [] })
			// axios.get(`/api/gammeTechniqueImprimer/sequnce/${sequence}`)
			// 	.then(res => {
			// 		let counterPerPartnumber = {};

			// 		this.setState({
			// 			cmsData: res.data,
			// 			data: res.data.map(e => {
			// 				if (counterPerPartnumber[e.partNumberImp]) {
			// 					counterPerPartnumber[e.partNumberImp] = counterPerPartnumber[e.partNumberImp] + 1;
			// 				} else {
			// 					counterPerPartnumber[e.partNumberImp] = 1;
			// 				}
			// 				return {
			// 					id: e.nSerieGammeImp,
			// 					partNumber: e.partNumberImp,
			// 					description: e.descriptionImp,
			// 					item: e.code3Imp,
			// 					item5: e.code5Imp,
			// 					qtyBox: e.quantiteImp,
			// 					wo: e.nofImp,
			// 					woid: e.woidImp,
			// 					sequence: e.nSequenceImp,
			// 					nbrImprissionImp: e.nbrImprissionImp,
			// 					counter: counterPerPartnumber[e.partNumberImp],
			// 					total: res.data.filter(elem => elem.partNumberImp == e.partNumberImp && elem.nbrImprissionImp && elem.nbrImprissionImp.trim() === "1").length,
			// 				}
			// 			})
			// 		})
			// 	})
			this.loadSequence(sequence)
		}

		axios.get(`/api/site/list`)
			.then((res) => {
				this.setState({
					optionsList: {
						...this.state.optionsList,
						site: res.data.map((elem) => ({ value: elem.nom, label: elem.nom, item: elem }))
					}
				})
			})

	}

	truncateString = (s, maxLength = 20) => {
		if (s.length <= maxLength) {
			return s;
		} else {
			return s.slice(0, maxLength);
		}
	}


	renderPlanCoupe = (cuttingRequest, user) => {
		let arrTable = [], reftissu = null, desc = null;
		[...cuttingRequest.cuttingRequestSeries].sort((a, b) => a.partNumberMaterial.localeCompare(b.partNumberMaterial)).map((elem, index) => {
			if (reftissu != null && elem.partNumberMaterial != reftissu) {
				arrTable.push(<tr style={{ borderBottom: "5px black solid" }}>
					<td style={{ borderLeft: "0", borderBottom: "0", fontWeight: 'bold' }}><span style={{ textDecoration: "underline", fontWeight: 'bold' }}>Group By</span></td>
					<td style={{ fontWeight: 'bold', backgroundColor: "#bfe1ff" }}>{reftissu}</td>
					<td colSpan={3} style={{ fontWeight: 'bold' }}><span style={{ textDecoration: "underline", fontWeight: 'bold' }}>Consomation :</span></td>
					<td style={{ fontWeight: 'bold', backgroundColor: "#bfe1ff" }}>{cuttingRequest.cuttingRequestSeries.filter(e => e.partNumberMaterial === reftissu).map(e => (e.longueur ?? 0) * (e.nbrCouche ?? 0)).reduce((a, b) => a + b).toFixed(2)}</td>
				</tr>)
				arrTable.push(<br />)
			}
			let arrDrill = (elem.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
			arrTable.push(<tr>
				<td >{elem.partNumberMaterial}</td>
				<td style={{ whiteSpace: "nowrap" }}>{this.truncateString(elem.description, 17)}</td>
				<td>{elem.longueur && elem.longueur.toFixed(3)}</td>
				<td>{elem.nbrCouche}</td>
				<td>{elem.laize}</td>
				<td style={{ whiteSpace: "nowrap" }}>{elem.matelassageEndroit}</td>
				<td style={{ width: 10, borderTop: 0, borderBottom: 0 }}></td>

				<td className='ml-1' style={{ whiteSpace: "nowrap", fontWeight: 'bold' }}>{elem.placement}</td>
				<td>{elem.config}</td>
				<td>{arrDrill[0]}</td>
				<td>{arrDrill[1]}</td>
			</tr>)
			reftissu = elem.partNumberMaterial
			desc = elem.description
		})
		if (reftissu != null) {
			arrTable.push(<tr style={{ borderBottom: "5px black solid" }}>
				<td style={{ borderLeft: "0", borderBottom: "0", fontWeight: 'bold' }}><span style={{ textDecoration: "underline", fontWeight: 'bold' }}>Group By</span></td>
				<td style={{ fontWeight: 'bold', backgroundColor: "#bfe1ff" }}>{reftissu}</td>
				<td colSpan={3} style={{ fontWeight: 'bold' }}><span style={{ textDecoration: "underline", fontWeight: 'bold' }}>Consomation :</span></td>
				<td style={{ fontWeight: 'bold', backgroundColor: "#bfe1ff" }}>{cuttingRequest.cuttingRequestSeries.filter(e => e.partNumberMaterial === reftissu).map(e => (e.longueur ?? 0) * (e.nbrCouche ?? 0)).reduce((a, b) => a + b)}</td>
			</tr>)
		}
		return <div className='' ref={elem => this.planPrintPage = elem} style={{ padding: 30 }}>
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
			<div className='d-flex'>
				<div style={{ fontSize: 20, width: "30%" }}>
					<div className='d-flex'>
						<div className=' text-right' style={{ width: "50%" }}><strong>N° Séquence : </strong></div>
						<div className='' style={{ width: "50%", fontWeight: "bold" }}>{cuttingRequest.sequence}</div>
					</div>
					<div className='d-flex'>
						<div className=' text-right' style={{ width: "50%" }}><strong>Projet : </strong></div>
						<div className='' style={{ width: "50%" }}>{cuttingRequest.projet}</div>
					</div>
					<div className='d-flex'>
						<div className=' text-right' style={{ width: "50%" }}><strong>Version : </strong></div>
						<div className='' style={{ width: "50%" }}>{cuttingRequest.version}</div>
					</div>

					<div className='d-flex'>
						<div className=' text-right' style={{ width: "50%" }}><strong>Définition : </strong></div>
						<div className='' style={{ width: "50%" }}>{cuttingRequest.definition}</div>
					</div>
					<div className='d-flex'>
						<div className=' text-right' style={{ width: "50%" }}><strong>CMS ID : </strong></div>
						<div className='' style={{ width: "50%" }}>{cuttingRequest.cmsId}</div>
					</div>
				</div>
				<div className='mb-2' style={{ width: "70%" }}>
					<table className='m-0 table-black-border entity-table-sm-14 '>
						<thead style={{ backgroundColor: "#bfe1ff" }}>
							<tr style={{}}>
								<th style={{ fontWeight: "bold" }}>wo</th>
								<th style={{ fontWeight: "bold" }}>woid</th>
								<th style={{ fontWeight: "bold" }}>Part Number</th>
								<th style={{ fontWeight: "bold" }}>Description</th>
								<th style={{ fontWeight: "bold" }}>Kit textil</th>
								<th style={{ fontWeight: "bold" }}>Kit</th>
							</tr>
						</thead>
						<tbody>
							{cuttingRequest.cuttingRequestPartNumbers && cuttingRequest.cuttingRequestPartNumbers.map(elemPn => <tr
								className='clickable-element'
								onClick={() => this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber)
									? this.setState({ selectedBoxs: this.state.selectedBoxs.filter(e => e.partNumber != elemPn.partNumber) })
									: this.setState({ selectedBoxs: [...this.state.selectedBoxs, ...cuttingRequest.cuttingRequestBoxs.filter(e => e.partNumber == elemPn.partNumber)].sort((a, b) => a.id.localeCompare(b.id)) })
								}
							>
								<td className=''>{elemPn.wo}</td>
								<td className=''>{elemPn.woid}</td>
								<td className=''>{elemPn.partNumber}</td>
								<td className=''>{elemPn.description}</td>
								<td className=''>{elemPn.item}</td>
								<td className=''><strong>{elemPn.quantity}</strong></td>

							</tr>)}
						</tbody>
					</table>
				</div>
			</div>
			<div className='d-flex' style={{ marginBottom: 70, fontSize: 20 }}>
				<div className=' text-right' style={{ width: "15%" }}><strong>modele : </strong></div>
				<div className='' style={{ width: "85%", wordWrap: "break-word" }}>{cuttingRequest.modele}</div>
			</div>
			<div>
				<table className='m-0 table-black-border entity-table-sm' style={{ fontSize: 5 }}>
					<thead style={{ color: "white", backgroundColor: "#33acd3" }}>
						<tr style={{}}>
							<th style={{ fontWeight: "bold", fontSize: 22, borderColor: "white" }} className='' colSpan={6}>Matelassage</th>
							<td style={{ width: 10, borderTop: 0, borderBottom: 0, borderColor: "white", backgroundColor: "white" }}></td>
							<th style={{ fontWeight: "bold", fontSize: 22, borderColor: "white" }} className=' ml-1' colSpan={5} >Coupe</th>
						</tr>
						<tr>
							<th style={{ fontWeight: "bold", borderColor: "white" }} className=''>Ref Tissus</th>
							<th style={{ fontWeight: "bold", borderColor: "white" }} className=''>Description</th>
							<th style={{ fontWeight: "bold", borderColor: "white" }} className=''>Longueur</th>
							<th style={{ fontWeight: "bold", borderColor: "white" }} className=''>Plie</th>
							<th style={{ fontWeight: "bold", borderColor: "white" }} className=''>Laize</th>
							<th style={{ fontWeight: "bold", borderColor: "white" }} className=''>Sens</th>
							<td style={{ width: 10, borderTop: 0, borderBottom: 0, borderColor: "white", backgroundColor: "white" }}></td>
							<th style={{ fontWeight: "bold", borderColor: "white" }} className=' ml-1'>Placement</th>
							<th style={{ fontWeight: "bold", borderColor: "white" }} className=''>Config</th>
							<th style={{ fontWeight: "bold", borderColor: "white" }} className=''>Drill1</th>
							<th style={{ fontWeight: "bold", borderColor: "white" }} className=''>Drill2</th>
						</tr>
					</thead>
					<tbody>
						<br />
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

	//modalSerie

	// renderModifModalSerie = () => {
	// 	return <Modal
	// 		show={this.state.modalSerie != null}
	// 		onHide={() => this.setState({ modalSerie: null })}
	// 		dialogClassName="modal-90w"
	// 		centered
	// 	>
	// 		{this.state.modalSerie && <div style={{ maxHeight: "80vh", overflowY: 'auto' }}>
	// 			<h4 className='text-center my-2'>Modification du Serie {this.state.modalSerie.serie}</h4>
	// 			<hr />
	// 			<div className='container'>
	// 				<div className='row'>


	// 					<div className='col-6'>
	// 						<div className='form-group'>
	// 							<label>Part Number Material</label>
	// 							<input type='text' className='form-control'
	// 								value={this.state.modalSerie.partNumberMaterial}
	// 								onChange={(e) => {
	// 									let modalSerie = this.state.modalSerie;
	// 									modalSerie.partNumberMaterial = e.target.value;
	// 									this.setState({ modalSerie })
	// 								}}
	// 							/>
	// 						</div>
	// 					</div>

	// 					<div className='col-6'>
	// 						<div className='form-group'>
	// 							<label>description</label>
	// 							<input type='text' className='form-control'
	// 								value={this.state.modalSerie.description}
	// 								onChange={(e) => {
	// 									let modalSerie = this.state.modalSerie;
	// 									modalSerie.description = e.target.value;
	// 									this.setState({ modalSerie })
	// 								}}
	// 							/>
	// 						</div>
	// 					</div>

	// 					<div className='col-6'>
	// 						<div className='form-group'>
	// 							<label>Sens</label>
	// 							<input type='text' className='form-control'
	// 								value={this.state.modalSerie.matelassageEndroit}
	// 								onChange={(e) => {
	// 									let modalSerie = this.state.modalSerie;
	// 									modalSerie.matelassageEndroit = e.target.value;
	// 									this.setState({ modalSerie })
	// 								}}
	// 							/>
	// 						</div>
	// 					</div>

	// 					<div className='col-6'>
	// 						<div className='form-group'>
	// 							<label>longueur</label>
	// 							<input type='text' className='form-control'
	// 								value={this.state.modalSerie.longueur}
	// 								onChange={(e) => {
	// 									let modalSerie = this.state.modalSerie;
	// 									modalSerie.longueur = e.target.value;
	// 									this.setState({ modalSerie })
	// 								}}
	// 							/>
	// 						</div>
	// 					</div>

	// 					<div className='col-6'>
	// 						<div className='form-group'>
	// 							<label>nbrCouche</label>
	// 							<input type='text' className='form-control'
	// 								value={this.state.modalSerie.nbrCouche}
	// 								onChange={(e) => {
	// 									let modalSerie = this.state.modalSerie;
	// 									modalSerie.nbrCouche = e.target.value;
	// 									this.setState({ modalSerie })
	// 								}}
	// 							/>
	// 						</div>
	// 					</div>

	// 					<div className='col-6'>
	// 						<div className='form-group'>
	// 							<label>laize</label>
	// 							<input type='text' className='form-control'
	// 								value={this.state.modalSerie.laize}
	// 								onChange={(e) => {
	// 									let modalSerie = this.state.modalSerie;
	// 									modalSerie.laize = e.target.value;
	// 									this.setState({ modalSerie })
	// 								}}
	// 							/>
	// 						</div>
	// 					</div>

	// 					<div className='col-6'>
	// 						<div className='form-group'>
	// 							<label>Placement</label>
	// 							<input type='text' className='form-control'
	// 								value={this.state.modalSerie.placement}
	// 								onChange={(e) => {
	// 									let modalSerie = this.state.modalSerie;
	// 									modalSerie.placement = e.target.value;
	// 									this.setState({ modalSerie })
	// 								}}
	// 							/>
	// 						</div>
	// 					</div>


	// 					<div className='col-6'>
	// 						<div className='form-group'>
	// 							<label>Config</label>
	// 							<input type='text' className='form-control'
	// 								value={this.state.modalSerie.config}
	// 								onChange={(e) => {
	// 									let modalSerie = this.state.modalSerie;
	// 									modalSerie.config = e.target.value;
	// 									this.setState({ modalSerie })
	// 								}}
	// 							/>
	// 						</div>
	// 					</div>

	// 					<div className='col-6'>
	// 						<div className='form-group'>
	// 							<label>drill</label>
	// 							<input type='text' className='form-control'
	// 								value={this.state.modalSerie.drill}
	// 								onChange={(e) => {
	// 									let modalSerie = this.state.modalSerie;
	// 									modalSerie.drill = e.target.value;
	// 									this.setState({ modalSerie })
	// 								}}
	// 							/>
	// 						</div>
	// 					</div>

	// 					<div className='col-6'>
	// 						<div className='form-group'>
	// 							<label>machine</label>
	// 							<input type='text' className='form-control'
	// 								value={this.state.modalSerie.machine}
	// 								onChange={(e) => {
	// 									let modalSerie = this.state.modalSerie;
	// 									modalSerie.machine = e.target.value;
	// 									this.setState({ modalSerie })
	// 								}}
	// 							/>
	// 						</div>
	// 					</div>




	// 				</div>
	// 			</div>
	// 			<hr />
	// 			<div className='d-flex justify-content-center mb-2'>
	// 				<button className='btn btn-primary mx-2' onClick={() => {

	// 				}}>
	// 					Modifier
	// 				</button>
	// 			</div>
	// 		</div>}
	// 	</Modal>
	// }

	renderModifModalSerie = () => {
		return <Modal
			show={this.state.modalSerie != null}
			onHide={() => this.setState({ modalSerie: null })}
			dialogClassName="modal-90w"
			centered
		>
			{this.state.modalSerie && <div style={{ maxHeight: "80vh", overflowY: 'auto' }}>
				<h4 className='text-center my-2'>Modification du Serie {this.state.modalSerie.serie} : {this.state.modalSerie.placement}</h4>
				<hr />
				<div className='container'>
					<div className='row'>
						<div className='col-6'>
							<div className='form-group'>
								<label>Nbr Couche</label>
								<input type='text' className='form-control mb-2'
									value={this.state.modalSerie.nbrCoucheSerie}
									onChange={(e) => {

										let modalSerie = this.state.modalSerie;
										modalSerie.nbrCoucheSerie = e.target.value.length > 0 ? parseInt(e.target.value) : null;
										this.setState({ modalSerie })

									}}
								/>
								<button className='btn btn-primary '
									disabled={this.state.diviseLoading}
									onClick={() => {
										if (this.state.modalSerie.nbrCoucheSerie > 0) {
											if (window.confirm("Êtes-vous sûr de modifier le nombre de couche de cette serie " + this.state.modalSerie.serie + this.state.modalSerie.placement)) {
												this.setState({ diviseLoading: true })
												axios.post(`/api/cuttingRequestSerieInfo/modifierNbrCouche?serie=${this.state.modalSerie.serie}&nbrCouche=${this.state.modalSerie.nbrCoucheSerie}`)
													.then(res => {
														const { user } = this.props.security;

														axios.post("/api/cuttingRequestSerieRouleauHistory", {
															content: "Modifier nombre de couche serie " + this.state.modalSerie.serie + " : nbrCouche " + this.state.modalSerie.nbrCoucheSerie + " : " + this.state.cuttingRequest.modele,
															serie: this.state.serie,
															changedAt: moment(),
															changedBy: user.lastName + " " + user.firstName
														})
															.then(res => {
																this.setState({ modalSerie: null, diviseLoading: false })
																this.loadSequence(this.state.sequence)
															})
															.catch(() => {
																this.setState({ diviseLoading: false })
															})

													})
													.catch(() => {
														this.setState({ diviseLoading: false })
													})
											}
										}
									}}>
									Modifier nombre de couche
								</button>
							</div>
						</div>
					</div>
				</div>
				<hr />
				<div className='container mb-4'>
					<div className='row'>

						<div className='col-6'>
							<div className='form-group'>
								<label>Nbr Couche</label>
								<input type='text' className='form-control'
									value={this.state.modalSerie.nbrCouche} disabled={true}
									onChange={(e) => {
										let modalSerie = this.state.modalSerie;
										modalSerie.nbrCouche = e.target.value;
										this.setState({ modalSerie })
									}}
								/>
							</div>
						</div>
						<div className='col-6'>
						</div>
						<div className='col-6'>
							<div className='form-group'>
								<label>Nbr de couche 1</label>
								<input type='text' className='form-control mb-2'
									value={this.state.modalSerie.nbrCoucheNew}
									onChange={(e) => {
										let modalSerie = this.state.modalSerie;
										modalSerie.nbrCoucheNew = e.target.value.length > 0 ? parseInt(e.target.value) : null;
										modalSerie.nbrCoucheNew2 = this.state.modalSerie.nbrCouche - (e.target.value.length > 0 ? parseInt(e.target.value) : 0)
										this.setState({ modalSerie })
									}}
								/>
								<button className='btn btn-primary'
									disabled={this.state.diviseLoading}
									onClick={() => {
										if (this.state.modalSerie.nbrCoucheNew > 0 && this.state.modalSerie.nbrCoucheNew2 > 0) {
											if (window.confirm("Êtes-vous sûr de diviser cette serie " + this.state.modalSerie.serie + this.state.modalSerie.placement)) {
												this.setState({ diviseLoading: true })
												axios.post(`/api/cuttingRequestSerieInfo/diviser?serie=${this.state.modalSerie.serie}&nbrCouche=${this.state.modalSerie.nbrCoucheNew}`)
													.then(res => {
														const { user } = this.props.security;
														axios.post("/api/cuttingRequestSerieRouleauHistory", {
															content: "Diviser serie " + this.state.modalSerie.serie + " : nbrCouche " + this.state.modalSerie.nbrCoucheNew + " : " + this.state.cuttingRequest.modele,
															serie: this.state.serie,
															changedAt: moment(),
															changedBy: user.lastName + " " + user.firstName
														})
															.then(res => {
																this.setState({ modalSerie: null, diviseLoading: false })
																this.loadSequence(this.state.sequence)
															})
															.catch(() => {
																this.setState({ diviseLoading: false })
															})

													})
													.catch(() => {
														this.setState({ diviseLoading: false })
													})
											}
										}
									}}>
									Diviser
								</button>
							</div>
						</div>

						<div className='col-6'>
							<div className='form-group'>
								<label>Nbr de couche 2</label>
								<input type='text' className='form-control'
									value={this.state.modalSerie.nbrCoucheNew2} disabled={true}
									onChange={(e) => {
										let modalSerie = this.state.modalSerie;
										modalSerie.nbrCoucheNew2 = e.target.value.length > 0 ? parseInt(e.target.value) : null;
										this.setState({ modalSerie })
									}}
								/>
							</div>
						</div>

					</div>
				</div>
			</div>}
		</Modal>
	}

	renderModifModal = () => {
		return <Modal
			show={this.state.modalDetail != null}
			onHide={() => this.setState({ modalDetail: null })}
			dialogClassName="modal-90w"
			centered
		>
			{this.state.modalDetail && <div style={{ maxHeight: "80vh", overflowY: 'auto' }}>
				<h4 className='text-center my-2'>Modification du box {this.state.modalDetail.nSerieGammeImp}</h4>
				<hr />
				<div className='container'>
					<div className='row'>
						<div className='col-6'>
							<div className='form-group'>
								<label>Part Number</label>
								<input type='text' className='form-control'
									value={this.state.modalDetail.partNumberImp}
									onChange={(e) => {
										let modalDetail = this.state.modalDetail;
										modalDetail.partNumberImp = e.target.value;
										this.setState({ modalDetail })
									}}
								/>
							</div>
						</div>
						<div className='col-6'>
							<div className='form-group'>
								<label>Description</label>
								<input type='text' className='form-control'
									value={this.state.modalDetail.descriptionImp}
									onChange={(e) => {
										let modalDetail = this.state.modalDetail;
										modalDetail.descriptionImp = e.target.value;
										this.setState({ modalDetail })
									}}
								/>
							</div>
						</div>
						<div className='col-6'>
							<div className='form-group'>
								<label>Kit textil</label>
								<input type='text' className='form-control'
									value={this.state.modalDetail.code3Imp}
									onChange={(e) => {
										let modalDetail = this.state.modalDetail;
										modalDetail.code3Imp = e.target.value;
										this.setState({ modalDetail })
									}}
								/>
							</div>
						</div>
						<div className='col-6'>
							<div className='form-group'>
								<label>Quantité</label>
								<input type='text' className='form-control'
									value={this.state.modalDetail.quantiteImp} disabled
									onChange={(e) => {
										let modalDetail = this.state.modalDetail;
										modalDetail.quantiteImp = e.target.value;
										this.setState({ modalDetail })
									}}
								/>
							</div>
						</div>
						<div className='col-6'>
							<div className='form-group'>
								<label>wo</label>
								<input type='text' className='form-control'
									value={this.state.modalDetail.nofImp} disabled
									onChange={(e) => {
										let modalDetail = this.state.modalDetail;
										modalDetail.nofImp = e.target.value;
										this.setState({ modalDetail })
									}}
								/>
							</div>
						</div>
					</div>
				</div>
				<hr />
				<div className='d-flex justify-content-center mb-2'>
					<button className='btn btn-primary mx-2' onClick={() => {
						// send this.state.modalDetail to backend
						axios.post('/api/gammeTechniqueImprimer/update', this.state.modalDetail)
							.then(res => {
								this.setState({ data: [], selectedBoxs: [], modalDetail: null })
								axios.get(`/api/gammeTechniqueImprimer/sequnce/${this.state.sequence}`)
									.then(res => {
										let counterPerPartnumber = {};

										this.setState({
											cmsData: res.data,
											data: res.data.map(e => {
												if (counterPerPartnumber[e.partNumberImp]) {
													counterPerPartnumber[e.partNumberImp] = counterPerPartnumber[e.partNumberImp] + 1;
												} else {
													counterPerPartnumber[e.partNumberImp] = 1;
												}
												return {
													id: e.nSerieGammeImp,
													partNumber: e.partNumberImp,
													description: e.descriptionImp,
													item: e.code3Imp,
													item5: e.code5Imp,
													qtyBox: e.quantiteImp,
													wo: e.nofImp,
													woid: e.woidImp,
													sequence: e.nSequenceImp,
													nbrImprissionImp: e.nbrImprissionImp,
													counter: counterPerPartnumber[e.partNumberImp],
													total: res.data.filter(elem => elem.partNumberImp == e.partNumberImp && elem.nbrImprissionImp && elem.nbrImprissionImp.trim() === "1").length,
												}
											})
										})
									})
							})
					}}>
						Modifier
					</button>
				</div>
			</div>}
		</Modal>
	}

	loadSequence = async (sequence) => {
		this.setState({ data: [], selectedBoxs: [], loadingMessage: "Chargement des Gammes CMS..." })
		let res = await axios.get(`/api/gammeTechniqueImprimer/sequnce/${sequence}`)

		let counterPerPartnumber = {};
		this.setState({
			cmsData: res.data,
			data: res.data.map(e => {
				if (counterPerPartnumber[e.partNumberImp]) {
					counterPerPartnumber[e.partNumberImp] = counterPerPartnumber[e.partNumberImp] + 1;
				} else {
					counterPerPartnumber[e.partNumberImp] = 1;
				}
				return {
					id: e.nSerieGammeImp,
					partNumber: e.partNumberImp,
					description: e.descriptionImp,
					item: e.code3Imp,
					item5: e.code5Imp,
					qtyBox: e.quantiteImp,
					wo: e.nofImp,
					woid: e.woidImp,
					sequence: e.nSequenceImp,
					nbrImprissionImp: e.nbrImprissionImp,
					counter: counterPerPartnumber[e.partNumberImp],
					total: res.data.filter(elem => elem.partNumberImp == e.partNumberImp && elem.nbrImprissionImp && elem.nbrImprissionImp.trim() === "1").length,

				}
			}),
			loadingMessage: "Chagement des produits finits..."
		})

		// try{
		// 	this.setState({ loadingMessage: "Chargement des données de plan CMS WEB..." })
		// 	let resCRData = await axios.get(`/api/cuttingRequestData/${sequence}`)
		// } catch (e) {
		// 	this.setState({ loadingMessage: "Nouvelle sequence, creation des données de plan CMS WEB..." })
		// 	let resRefreshSequence = await axios.post(`/api/cuttingRequestV2/refresh/${sequence}`)
		// }

		this.setState({ loadingMessage: "Chargement des données de produit finit...", planning: null })
		let resProduitFinit = await axios.get(`/api/cms/produitFinit/sequence/${sequence}`)
		if (resProduitFinit.data && resProduitFinit.data.length > 0) {
			let idPlanCoupe = resProduitFinit.data[0].idPlanProduiFinit
			this.setState({ loadingMessage: "Chargement des données de plan coupe CMS " + idPlanCoupe + " ..." })
			let resPlanCoupe = await axios.get(`/api/cms/planCoupe/light/${idPlanCoupe}`)
			this.setState({ loadingMessage: "Chargement des données de matlassage..." })
			let resMatlassage = await axios.get(`/api/cms/matlassage/sequence/${sequence}`)
			this.setState({ loadingMessage: "Chargement des données de suivi matlassage..." })
			let resSuiviMatlassage = await axios.get(`/api/cms/suiviMatelassage/sequence/${sequence}`)
			this.setState({ loadingMessage: "Chargement des données de coupe..." })
			let resCoupe = await axios.get(`/api/cms/coupe/sequence/${sequence}`)
			this.setState({ loadingMessage: "Chargement des données de suivi coupe..." })
			let resSuiviCoupe = await axios.get(`/api/cms/suiviCoupe/sequence/${sequence}`)
			//suiviplanning
			this.setState({ loadingMessage: "Chargement du Suivi Planning..." })
			let resSuiviplanning = await axios.get(`/api/cms/suiviPlanning/${sequence}`)

			this.setState({ loadingMessage: "Chargement des données de plan coupe CMS WEB...", planning: resSuiviplanning.data })
			let resCuttingPlan = await axios.get(`/api/cuttingPlanData/all?equal.cmsId=${idPlanCoupe}&page=0&size=1&sort=id,desc`)
			let cuttingPlanId = null
			if (resCuttingPlan.data && resCuttingPlan.data.content.length > 0) {
				cuttingPlanId = resCuttingPlan.data.content[0].id
			}
			// this.setState({ loadingMessage: "Verification des kits du placements..." })
			// let modificationNeeded = []
			// if (resCuttingPlan.data.content.length > 0) {
			// 	let resCuttingPlanPlacement = await axios.get(`/api/cuttingPlanMaterialPlacementData/all?equal.cuttingPlan=${resCuttingPlan.data.content[0].id}&page=0&size=100&sort=partNumberMaterial,asc`)

			// 	let totalNbrCouchePerMaterial = {}
			// 	resCuttingPlanPlacement.data.content.map(e => {
			// 		if (totalNbrCouchePerMaterial[e.partNumberMaterial]) {
			// 			totalNbrCouchePerMaterial[e.partNumberMaterial] = totalNbrCouchePerMaterial[e.partNumberMaterial] + e.nbrCouche
			// 		} else {
			// 			totalNbrCouchePerMaterial[e.partNumberMaterial] = e.nbrCouche
			// 		}
			// 	})

			// 	let totalNbrCouchePerMaterialMatlassage = {}
			// 	resMatlassage.data.map(e => {
			// 		if (totalNbrCouchePerMaterialMatlassage[e.reftissu]) {
			// 			totalNbrCouchePerMaterialMatlassage[e.reftissu] = totalNbrCouchePerMaterialMatlassage[e.reftissu] + e.nCouches
			// 		} else {
			// 			totalNbrCouchePerMaterialMatlassage[e.reftissu] = e.nCouches
			// 		}
			// 	})
			// 	Object.keys(totalNbrCouchePerMaterial).map(e => {
			// 		if (totalNbrCouchePerMaterial[e] != totalNbrCouchePerMaterialMatlassage[e]) {
			// 			modificationNeeded.push({
			// 				partNumberMaterial: e,
			// 				totalNbrCouchePerMaterial: totalNbrCouchePerMaterial[e],
			// 				totalNbrCouchePerMaterialMatlassage: totalNbrCouchePerMaterialMatlassage[e]
			// 			})
			// 		}
			// 	})
			// }

			///////////////////////////////////////////
			this.setState({ loadingMessage: "Chargement des Optionnels..." })
			let resOptionnal = await axios.get("/api/cuttingPlanMaterialPlacementData/cuttingPlan/" + cuttingPlanId)
			let obj = {}
			this.setState({ loadingMessage: "Chargement des config serie plus..." })
			/////////////////////////////////////
			// let resConfigSeriePlus = await axios.get(`/api/configSeriePlus/byPartNumberMaterialArr?arr=${resMatlassage.data.map(e => e.reftissu).join(",")}`)
			let resConfigSeriePlus = await axios.get(`/api/configSeriePlus/list`)
			let resPatterns = await axios.get(`/api/ctcFiles/getPattern?partNumberCoverArray=${resProduitFinit.data.map(e => e.refProdFinit).join(",")}`)

			let arrGoodExtraSeries = []
			let arrPlacementMat = resMatlassage.data.map(e => e.placement)
			resConfigSeriePlus.data.map(e => {
				if (
					resPatterns.data.includes(e.partNumberMaterial.toUpperCase().trim() + " : " + e.pattern.toUpperCase().trim())
					&& !arrPlacementMat.includes(e.placement.toUpperCase().trim())
				) {
					arrGoodExtraSeries.push(e)
				}
			})


			let cuttingRequest = {
				sequence: sequence,
				projet: resPlanCoupe.data.groupPlanCoupe,
				version: resPlanCoupe.data.versionPlanCoupe,
				modele: resPlanCoupe.data.indexPlanCoupe,
				definition: resPlanCoupe.data.definitionPlanCoupe,
				cuttingPlanId: cuttingPlanId,
				cmsId: idPlanCoupe,
				cuttingRequestPartNumbers: [],
				cuttingRequestSeries: [],
				cuttingRequestBoxs: []
			}
			cuttingRequest.cuttingRequestPartNumbers = resProduitFinit.data.map(pf => {
				let boxList = res.data.filter(e => e.partNumberImp == pf.refProdFinit)
				return {
					partNumber: pf.refProdFinit,
					description: pf.desiProdFinit,
					item: pf.refProdSemi,
					quantity: pf.nbrKit,
					wo: pf.noff,
					woid: pf.woid,
					packageQty: boxList.length > 0 ? boxList[0].quantiteImp : null,
					statusPlan: pf.statusPlan,
				}
			})
			cuttingRequest.cuttingRequestSeries = resMatlassage.data.map((mt, ind) => {
				let coupeArr = resCoupe.data.filter(e => mt.nserie == e.nserie)
				let coupe = null
				if (coupeArr.length > 0) coupe = coupeArr[0]
				let suivicoupeArr = resSuiviCoupe.data.filter(e => mt.nserie == e.nserie)
				let suivicoupe = null
				if (suivicoupeArr.length > 0) suivicoupe = suivicoupeArr[0]
				let suiviMatlassageArr = resSuiviMatlassage.data.filter(e => mt.nserie == e.nserie)
				let suiviMatlassage = null
				if (suiviMatlassageArr.length > 0) suiviMatlassage = suiviMatlassageArr[0]

				let cpmp = resOptionnal.data.find(e => e.placement.replaceAll("-", "").substring(0, 4) == mt.placement.replaceAll("-", "").substring(0, 4))
				if (cpmp) {
					let arrOpt = resOptionnal.data.filter(e => (
						e.partNumberMaterial == cpmp.partNumberMaterial &&
						e.groupPlacement == cpmp.groupPlacement &&
						e.maxPlie == cpmp.maxPlie &&
						e.nbrCouche == cpmp.nbrCouche
						// e.laize == cpmp.laize &&
						// && e.partNumbers == cpmp.partNumbers
						// e.machine.startsWith("Lectra")
					))
					obj[mt.placement.replaceAll("-", "").substring(0, 4)] = arrOpt
				}


				return {
					serie: mt.nserie,
					numberOfOptions: obj[mt.placement.replaceAll("-", "").substring(0, 4)] ? (obj[mt.placement.replaceAll("-", "").substring(0, 4)].length - 1) : null,
					partNumberMaterial: mt.reftissu,
					description: mt.description,
					matelassageEndroit: mt.sens,
					longueur: parseFloat(mt.longueur),
					nbrCouche: mt.nCouches,
					placement: mt.placement,
					config: coupe?.configuration,
					drill: coupe?.drill1 + "," + coupe?.drill2,
					machine: suivicoupe.machine,
					modele: mt.modele,
					laize: mt.laLaizeDemande,
					partNumbers: mt.quantite,
					ind: ind + 1,
					total: resMatlassage.data.length,
					statutMatelassage: suiviMatlassage?.statu,
					statutCoupe: suivicoupe?.statu,
				}
			}).sort((a, b) => a.ind - b.ind)

			this.setState({
				cuttingRequest, idPlanCoupe,
				// modificationNeeded, 
				arrGoodExtraSeries,
				resProduitFinit, resPlanCoupe, resMatlassage, resSuiviMatlassage, resCoupe, resSuiviCoupe, optionnals: obj,
				// loadingMessage: "Verification des optionnel des series ..."
				loadingMessage: null
			}, () => {
				// Validate series after loading
				this.validateSeries()
			})


		} else {
			this.setState({ cuttingRequest: null, idPlanCoupe: null, resProduitFinit: null, resPlanCoupe: null, resMatlassage: null, resSuiviMatlassage: null, resCoupe: null, resSuiviCoupe: null, loadingMessage: null, serieValidationResults: [] })
		}

	}

	validateSeries = async () => {
		if (!this.state.cuttingRequest || !this.state.cuttingRequest.cuttingRequestSeries) {
			this.setState({ serieValidationResults: [] })
			return
		}

		let validationResults = []
		const series = this.state.cuttingRequest.cuttingRequestSeries

		// Get unique partNumberMaterials for validation
		const uniqueMaterials = [...new Set(series.map(s => s.partNumberMaterial).filter(p => p))]
		
		if (uniqueMaterials.length > 0) {
			try {
				// Fetch PartNumberMaterialConfig for all materials
				const res = await axios.get(`/api/partNumberMaterialConfig/pns/${uniqueMaterials.join(",")}?projet=${this.state.cuttingRequest.projet}`)
				const configMap = {}
				res.data.forEach(cfg => {
					configMap[cfg.partNumberMaterial] = cfg
				})

				// Validate each serie
				series.forEach(serie => {
					if (serie.partNumberMaterial && !configMap[serie.partNumberMaterial]) {
						validationResults.push({
							serie: serie.serie,
							partNumberMaterial: serie.partNumberMaterial,
							placement: serie.placement,
							issue: 'PartNumberMaterialConfig not found',
							canAdd: true
						})
					}
				})
			} catch (err) {
				console.error("Error validating series:", err)
			}
		}

		this.setState({ serieValidationResults: validationResults })
	}

	handleAddSerieConfig = (validationItem) => {
		// Open modal to add new PartNumberMaterialConfig
		this.setState({ 
			showAddSerieModal: true, 
			newSerieToAdd: {
				partNumberMaterial: validationItem.partNumberMaterial,
				projet: this.state.cuttingRequest?.projet || ''
			}
		})
	}

	saveNewSerieConfig = async () => {
		if (!this.state.newSerieToAdd) return
		
		try {
			await axios.post('/api/partNumberMaterialConfig', this.state.newSerieToAdd)
			this.setState({ showAddSerieModal: false, newSerieToAdd: null })
			// Re-validate after adding
			this.validateSeries()
		} catch (err) {
			console.error("Error saving new serie config:", err)
			alert("Erreur lors de l'ajout de la configuration: " + (err.response?.data?.message || err.message))
		}
	}

	renderAddSerieModal = () => {
		return <Modal
			show={this.state.showAddSerieModal}
			onHide={() => this.setState({ showAddSerieModal: false, newSerieToAdd: null })}
			centered
		>
			<Modal.Header closeButton>
				<Modal.Title>Ajouter Configuration Matière</Modal.Title>
			</Modal.Header>
			<Modal.Body>
				{this.state.newSerieToAdd && (
					<div>
						<div className='form-group'>
							<label><strong>Part Number Material</strong></label>
							<input 
								type='text' 
								className='form-control' 
								value={this.state.newSerieToAdd.partNumberMaterial || ''} 
								onChange={(e) => this.setState({ newSerieToAdd: { ...this.state.newSerieToAdd, partNumberMaterial: e.target.value }})}
							/>
						</div>
						<div className='form-group'>
							<label><strong>Projet</strong></label>
							<input 
								type='text' 
								className='form-control' 
								value={this.state.newSerieToAdd.projet || ''} 
								onChange={(e) => this.setState({ newSerieToAdd: { ...this.state.newSerieToAdd, projet: e.target.value }})}
							/>
						</div>
						<div className='form-group'>
							<label><strong>Vitesse</strong></label>
							<input 
								type='number' 
								className='form-control' 
								value={this.state.newSerieToAdd.vitesse || ''} 
								onChange={(e) => this.setState({ newSerieToAdd: { ...this.state.newSerieToAdd, vitesse: e.target.value ? parseInt(e.target.value) : null }})}
							/>
						</div>
						<div className='form-group'>
							<label><strong>Description</strong></label>
							<input 
								type='text' 
								className='form-control' 
								value={this.state.newSerieToAdd.description || ''} 
								onChange={(e) => this.setState({ newSerieToAdd: { ...this.state.newSerieToAdd, description: e.target.value }})}
							/>
						</div>
					</div>
				)}
			</Modal.Body>
			<Modal.Footer>
				<button className='btn btn-secondary' onClick={() => this.setState({ showAddSerieModal: false, newSerieToAdd: null })}>
					Annuler
				</button>
				<button className='btn btn-primary' onClick={() => this.saveNewSerieConfig()}>
					<FontAwesomeIcon icon={faSave} /> Enregistrer
				</button>
			</Modal.Footer>
		</Modal>
	}

	renderSerieValidationWarnings = () => {
		if (!this.state.serieValidationResults || this.state.serieValidationResults.length === 0) {
			return null
		}

		return (
			<div className='alert alert-warning mt-2'>
				<strong>Validation des Séries:</strong>
				<ul className='mb-0 pl-3'>
					{this.state.serieValidationResults.map((item, idx) => (
						<li key={idx}>
							Serie {item.serie} - {item.partNumberMaterial}: {item.issue}
							{item.canAdd && (
								<button 
									className='btn btn-sm btn-outline-primary ml-2'
									onClick={() => this.handleAddSerieConfig(item)}
								>
									<FontAwesomeIcon icon={faPlus} /> Ajouter
								</button>
							)}
						</li>
					))}
				</ul>
			</div>
		)
	}

	renderModelGoodExtraSeries = () => {
		/*
		table of  this : 
			{ name: "pattern", displayName: "Pattern", type: "text" },
	  { name: "partNumberMaterial", displayName: "Part Number Material", type: "text" },
	  { name: "description", displayName: "Description", type: "text" },
	  { name: "matelassageEndroit", displayName: "Matelassage Endroit", type: "text" },
	  { name: "longueur", displayName: "Longueur", type: "text" },
	  { name: "machine", displayName: "Machine", type: "text" },
	  { name: "nbrCouche", displayName: "Nbr Couche", type: "text" },
	  { name: "kits", displayName: "Kits", type: "text" },
	  { name: "maxPlie", displayName: "Max Plie", type: "text" },
	  { name: "placement", displayName: "Placement", type: "text" },
	  { name: "laize", displayName: "Laize", type: "text" },
	  { name: "config", displayName: "Config", type: "text" },
	  { name: "drill", displayName: "Drill", type: "text" },

		*/
		return <Modal
			show={this.state.arrGoodExtraSeries.length > 0}
			onHide={() => {
				this.setState({ arrGoodExtraSeries: [] })
				// this.loadSequence()
			}}
			dialogClassName="modal-90w"
			centered
		>
			<div style={{ maxHeight: "80vh", overflowY: 'auto' }}>
				<h2 className='text-center my-2'>
					Ajouter des series plus (TEST Lamination)
				</h2>
				<table className='table table-bordered'>
					<thead className='entity-table-header'>
						<tr>
							<th>Pattern</th>
							<th>Part Number Material</th>
							<th>Description</th>
							<th>Matelassage Endroit</th>
							<th>Longueur</th>
							<th>Machine</th>
							<th>Nbr Couche</th>
							<th>Kits</th>
							<th>Max Plie</th>
							<th>Placement</th>
							<th>Laize</th>
							<th>Config</th>
							<th>Drill</th>
							<th></th>
						</tr>
					</thead>
					<tbody>
						{this.state.arrGoodExtraSeries.map((e, ind) => <tr>
							<td>{e.pattern}</td>
							<td>{e.partNumberMaterial}</td>
							<td>{e.description}</td>
							<td>{e.matelassageEndroit}</td>
							<td>{e.longueur}</td>
							<td>{e.machine}</td>
							<td>{e.nbrCouche}</td>
							<td>{e.kits}</td>
							<td>{e.maxPlie}</td>
							<td>{e.placement}</td>
							<td>{e.laize}</td>
							<td>{e.config}</td>
							<td>{e.drill}</td>
							<td style={{ padding: 3 }}>
								<button className='btn btn-primary' style={{ padding: 3 }}
									disabled={this.state.loadingExtraSeries}
									onClick={() => {
										this.setState({ loadingExtraSeries: true })
										axios.post(`/api/cuttingRequestSerieInfo/add`, {
											sequence: this.state.sequence,
											config: e.config,
											description: e.description,
											drill: e.drill,
											laize: e.laize,
											longueur: e.longueur,
											machine: e.machine,
											matelassageEndroit: e.matelassageEndroit,
											nbrCouche: e.nbrCouche,
											partNumberMaterial: e.partNumberMaterial,
											partNumberMaterialDescription: e.description,
											placement: e.placement,
											quantite: this.state.resProduitFinit.data[0].nbrKit,
											activated: true,
											groupPlacement: 1,
										})
											.then(res => {
												let arr = [...this.state.arrGoodExtraSeries.filter((e, i) => i != ind)]
												this.setState({
													loadingExtraSeries: false,
													//remove the one in position ind
													arrGoodExtraSeries: [...this.state.arrGoodExtraSeries.filter((e, i) => i != ind)]
												})
												if (arr.length == 0) {
													this.loadSequence(this.state.sequence)
												}
											})
									}}><FontAwesomeIcon icon={faPlus} /> Ajouter</button>
							</td>
						</tr>)}
					</tbody>
				</table>
			</div>
		</Modal>
	}

	renderSeriesTable = () => {
		const { user } = this.props.security;

		return <div className='responsive-table'>
			<table className='table'>
				<thead>
					<tr>
						<th>Ind</th>
						<th>Serie</th>
						<th>Material</th>
						<th>description</th>
						<th>Sens</th>
						<th >Longueur</th>
						<th >Nbr Couche</th>
						<th >Laize</th>
						<th>Placement</th>
						<th >Config</th>
						<th >Drill1</th>
						<th >Drill2</th>
						<th >Machine</th>
						<th >Matelassage</th>
						<th >Coupe</th>
					</tr>
				</thead>
				<tbody>
					{this.state.cuttingRequest.cuttingRequestSeries && this.state.cuttingRequest.cuttingRequestSeries
						.sort((a, b) => a.ind - b.ind)
						.map(elemPn => {
							let arrDrill = (elemPn.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
							return <tr
								style={elemPn.activated === false ? { backgroundColor: "#ffffc0" } : { backgroundColor: "" }}
								onDoubleClick={() => {
									if (user && user.roles && user.roles.some(role => ["ROLE_ADMIN", "ROLE_QUALITE"].includes(role.authority))) {
										this.setState({ modalSerie: { ...elemPn } })
									}
								}}
							>
								<td className=''>{elemPn.ind}</td>
								<td className='' style={{ whiteSpace: "nowrap", padding: 8 }}>
									{elemPn.serie} <button style={{ padding: "2 12" }}
										className='btn btn-outline-primary' disabled={this.state.loadingPrintSeries.includes(elemPn.serie)}
										onClick={() => {
											this.setState({ loadingPrintSeries: [...this.state.loadingPrintSeries, elemPn.serie] })
											axios.post(`/api/cuttingRequest/printSerie`, [{ ...elemPn, cuttingRequest: { ...this.state.cuttingRequest, cuttingRequestSeries: null, cuttingRequestBoxs: null, cuttingRequestPartNumbers: null } }])
												// .then(res => {
												// 	this.setState({loadingPrintSeries : [...this.state.loadingPrintSeries.filter(e => e != elemPn.serie)]})
												// })
												.finally(res => {
													this.setState({
														loadingPrintSeries: [...this.state.loadingPrintSeries.filter(e => e != elemPn.serie)],
														cuttingRequest: { ...this.state.cuttingRequest, cuttingRequestSeries: this.state.cuttingRequest.cuttingRequestSeries.sort((a, b) => (a.ind - b.ind)) }
													})
												})
										}}
									>{this.state.loadingPrintSeries.includes(elemPn.serie) ? <FontAwesomeIcon icon={faSpinner} spin /> : <FontAwesomeIcon icon={faPrint} />}</button>
								</td>
								<td className=''>{elemPn.partNumberMaterial}</td>
								<td className=''>{elemPn.description}</td>
								<td className=''>{elemPn.matelassageEndroit}</td>
								<td className=''>{elemPn.longueur && elemPn.longueur.toFixed(3)}</td>
								<td className=''>{elemPn.nbrCouche}</td>
								<td className=''>{elemPn.laize}</td>
								<td className=''>{elemPn.placement}{elemPn.numberOfOptions != null && elemPn.numberOfOptions > 0 && ` (${elemPn.numberOfOptions})`}</td>
								<td className=''>{elemPn.config}</td>
								<td className=''>{arrDrill[0]}</td>
								<td className=''>{arrDrill[1]}</td>
								<td className=''>{elemPn.machine}</td>
								<td style={elemPn.statutMatelassage === "Non demarre" ? { backgroundColor: "#ffafaf" } :
									elemPn.statutMatelassage === "En cours" ? { backgroundColor: "#f6ff6b" } :
										elemPn.statutMatelassage === "Complet" ? { backgroundColor: "#7bff6b" } :
											elemPn.statutMatelassage === "Incomplet" ? { backgroundColor: "#ffc46b" } :
												{}}>{elemPn.statutMatelassage}</td>
								<td style={elemPn.statutCoupe === "Non demarre" ? { backgroundColor: "#ffafaf" } :
									elemPn.statutCoupe === "En cours" ? { backgroundColor: "#f6ff6b" } :
										elemPn.statutCoupe === "Complet" ? { backgroundColor: "#7bff6b" } :
											elemPn.statutCoupe === "Incomplet" ? { backgroundColor: "#ffc46b" } :
												{}}>{elemPn.statutCoupe}</td>
							</tr>
						})}
				</tbody>
			</table>
		</div>
	}

	renderModifNbrCoucheModal = () => {
		return <Modal
			show={this.state.modificationNeeded.length > 0}
			onHide={() => this.setState({ modificationNeeded: [] })}
			dialogClassName="modal-90w"
			centered
		>
			<div style={{ maxHeight: "80vh", overflowY: 'auto' }}>
				<h2 className='text-center my-2'>
					Problème de nombre de couche
				</h2>
				<table className='table table-bordered'>
					<thead className='entity-table-header'>
						<tr>
							<th>Part Number</th>
							<th>Total Nbr Couche</th>
							<th>Total Nbr Couche Matlassage</th>
							<th>Series</th>
							<th>Modifier</th>
						</tr>
					</thead>
					<tbody>
						{this.state.modificationNeeded.map(e => <tr>
							<td>{e.partNumberMaterial}</td>
							<td>{e.totalNbrCouchePerMaterial}</td>
							<td>{e.totalNbrCouchePerMaterialMatlassage}</td>
							<td>
								<ul>
									{this.state.cuttingRequest.cuttingRequestSeries
										.filter(elem => elem.partNumberMaterial === e.partNumberMaterial)
										.map(elem => elem.serie + " : " + elem.nbrCouche).join(", ")}
								</ul>
							</td>
							<td><button className='btn btn-primary btn-sm' onClick={() => {
							}}>Modifier</button></td>
						</tr>)}
					</tbody>
				</table>
			</div>
		</Modal>
	}

	deleteSequence = (user) => {
		axios.delete("/api/query/deleteSequenceCMS?sequence=" + this.state.sequence)
			.then(res => {
				axios.post("/api/cuttingRequestSerieRouleauHistory", {
					content: "Delete sequence " + this.state.sequence + " : Series " + this.state.cuttingRequest.cuttingRequestSeries.map(e => e.serie).join(",") + " : " + this.state.cuttingRequest.modele,
					serie: this.state.sequence,
					changedAt: moment(),
					changedBy: user.lastName + " " + user.firstName
				})
					.then(() => {
						this.setState({
							sequence: "",
							cmsData: [],
							data: [],
							selectedBoxs: [],
							errors: {},
							selectedPartnumber: null,
							modalDetail: null,
							cuttingRequest: null,
							loadingPrintSeries: [],
							planning: null
						})
					})
			})

	}

	initialiserSequence = (user) => {
		axios.post("/api/query/initialiserSequenceCMS?sequence=" + this.state.sequence)
			.then(res => {
				axios.post("/api/cuttingRequestSerieRouleauHistory", {
					content: "Initialiser sequence " + this.state.sequence + " : Series " + this.state.cuttingRequest.cuttingRequestSeries.map(e => e.serie).join(",") + " : " + this.state.cuttingRequest.modele,
					serie: this.state.sequence,
					changedAt: moment(),
					changedBy: user.lastName + " " + user.firstName
				})
					.then(() => {
						this.setState({
							sequence: "",
							cmsData: [],
							data: [],
							selectedBoxs: [],
							errors: {},
							selectedPartnumber: null,
							modalDetail: null,
							cuttingRequest: null,
							loadingPrintSeries: [],
							planning: null
						})
					})
			})

	}

	render() {
		//gamme prop : ID	Part Number	Description	Kit textil	Quantité	wo	woid
		const { user } = this.props.security;

		let partNumbers = []
		if (this.state.data && this.state.data.length > 0) {
			this.state.data.map(e => {
				if (!partNumbers.includes(e.partNumber)) {
					partNumbers.push(e.partNumber)
				}
			})
		}

		let countTotal = this.state.selectedBoxs.length
		let countLoaded = this.state.selectedBoxs.filter(e => e.loaded == true).length
		let canPrintGammes = countTotal > 0 && countLoaded === countTotal

		return (
			<div>
				<h1 className='text-center'>Gammes CMS</h1>
				<div className='d-flex' style={{ flexWrap: "wrap", justifyContent: "center", alignItems: "center" }}>
					<input type='text' className='form-control' placeholder='Sequence CMS...'
						style={{ width: 200 }}
						name='sequence' value={this.state.sequence}
						onChange={(e) => {
							this.setState({
								sequence: e.target.value,
								cmsData: [],
								data: [],
								selectedBoxs: [],
								errors: {},
								selectedPartnumber: null,
								modalDetail: null,
								cuttingRequest: null,
								loadingPrintSeries: []
							})
						}}
						onKeyUp={(e) => {
							if (e.key === 'Enter') {
								this.loadSequence(this.state.sequence)
							}
						}}
					/>
					<button
						type="button" style={{ whiteSpace: "nowrap" }}
						className="btn btn-outline-primary ml-1" disabled={this.state.cuttingRequest == null || this.state.printingSeries}
						onClick={() => {
							this.setState({ printingSeries: true })
							axios.post(`/api/cuttingRequest/printSerie`,
								[...this.state.cuttingRequest.cuttingRequestSeries.map(elemCRS => {
									return {
										...elemCRS,
										cuttingRequest: { ...this.state.cuttingRequest, cuttingRequestSeries: null, cuttingRequestBoxs: null, cuttingRequestPartNumbers: null }
									}
								})]
							)
								.then(res => {
									this.setState({ printingSeries: false })
								})
								.catch((err) => {
									this.setState({ printingSeries: false })
								})
						}}
					>
						{this.state.printingSeries ? <FontAwesomeIcon icon={faSpinner} spin /> : <FontAwesomeIcon icon={faPrint} />} Imprimer Serie {this.state.cuttingRequest ? `(${this.state.cuttingRequest.cuttingRequestSeries.length})` : ""}

					</button>

					<ReactToPrint
						onBeforeGetContent={() => {
							return new Promise((resolve, reject) => {
								this.setState({ modalRotate: true, selectedEmp: null }, () => resolve())
							});
						}}
						onAfterPrint={() => { this.setState({ modalRotate: false }) }}
						onPrintError={() => { this.setState({ modalRotate: false }) }}
						trigger={() => <button style={{ whiteSpace: "nowrap" }}
							type="button"
							className="btn btn-outline-success ml-1"
							disabled={!canPrintGammes}

						><FontAwesomeIcon icon={faPrint} /> Imprimer Gammes {countTotal > 0 && `(${countLoaded}/${countTotal})`}</button>}
						content={() => this.componentRef}
						pageStyle={`@page { size: A4 portrait; margin: 0 } @media print { body { -webkit-print-color-adjust: exact } }`}
					/>
					<ReactToPrint
						onBeforeGetContent={() => {
							return new Promise((resolve, reject) => {
								this.setState({}, () => resolve())
							});
						}}
						onAfterPrint={() => { }}
						onPrintError={() => { }}
						trigger={() => <button style={{ whiteSpace: "nowrap" }} disabled={this.state.cuttingRequest == null}
							type="button"
							className="btn btn-outline-danger ml-1"

						><FontAwesomeIcon icon={faPrint} /> Imprimer Plan</button>}
						content={() => this.planPrintPage}
					/>
					{this.state.sequence && this.state.sequence.length > 10 && this.state.cuttingRequest && this.state.cuttingRequest.cuttingRequestSeries && this.state.cuttingRequest.cuttingRequestSeries.filter(e => e.statutMatelassage != "Non demarre").length === 0 && <button style={{ whiteSpace: "nowrap" }} disabled={this.state.cuttingRequest == null}
						type="button"
						className="btn btn-danger ml-1"
						onClick={() => {
							if (window.confirm('Êtes-vous sûr de supprimer ce plan?' + this.state.sequence)) {
								this.deleteSequence(user)
							}
						}}
					>
						<FontAwesomeIcon icon={faTimes} /> Supprimer le plan
					</button>}
					{this.state.planning && this.state.planning.statu === "Released" && this.state.planning.statuC === "Non demarre" &&
						<button style={{ whiteSpace: "nowrap" }}
							disabled={this.state.cuttingRequest == null}
							type="button"
							className="btn btn-warning ml-1"
							onClick={() => {
								if (window.confirm("Êtes-vous sûr d'initialiser ce plan? " + this.state.sequence)) {
									this.initialiserSequence(user)
								}
							}}
						>
							<FontAwesomeIcon icon={faArrowRotateBack} /> Initialiser le plan
						</button>
					}
				</div>
				{/* Serie Validation Warnings */}
				{this.renderSerieValidationWarnings()}
				{!this.state.loadingMessage && this.state.cuttingRequest && this.state.cuttingRequest.cuttingRequestPartNumbers && <div className='entity-table'><table className='table table-bordered '>
					<thead className='' style={{ backgroundColor: "#0087ff", color: "white" }}>
						<tr style={{}}>
							<th style={{ fontWeight: "bold" }}>wo</th>
							<th style={{ fontWeight: "bold" }}>woid</th>
							<th style={{ fontWeight: "bold" }}>Part Number</th>
							<th style={{ fontWeight: "bold" }}>Description</th>
							<th style={{ fontWeight: "bold" }}>Kit textil</th>
							<th style={{ fontWeight: "bold" }}>Kit</th>
							<th style={{ fontWeight: "bold", minWidth: 70 }}>Site</th>
						</tr>
					</thead>
					<tbody>
						{this.state.cuttingRequest.cuttingRequestPartNumbers && this.state.cuttingRequest.cuttingRequestPartNumbers.map((elemPn, indPn) => <tr
						// className='clickable-element'
						// onClick={() => this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber)
						// 	? this.setState({ selectedBoxs: this.state.selectedBoxs.filter(e => e.partNumber != elemPn.partNumber) })
						// 	: this.setState({ selectedBoxs: [...this.state.selectedBoxs, ...this.state.cuttingRequest.cuttingRequestBoxs.filter(e => e.partNumber == elemPn.partNumber)].sort((a, b) => a.id.localeCompare(b.id)) })
						// }
						>
							<td>{elemPn.wo}</td>
							<td>{elemPn.woid}</td>
							<td>{elemPn.partNumber}</td>
							<td>{elemPn.description}</td>
							<td>{elemPn.item}</td>
							<td><strong>{elemPn.quantity}</strong></td>
							<td>
								{this.state.optionsList.site && <Select id={"site"} name={"site"} classNamePrefix="rs"
									placeholder={"site..."} className='col-12 p-0'
									isClearable={false}
									value={(this.state.optionsList.site && this.state.optionsList.site.length > 0 && elemPn.site)
										? { label: elemPn.site, value: elemPn.site, item: elemPn.site }
										: null
									}
									options={this.state.optionsList.site}
									onChange={(option) => {
										let arr = [...this.state.cuttingRequest.cuttingRequestPartNumbers]
										arr[indPn].site = option ? option.value : null
										this.setState({
											cuttingRequest: { ...this.state.cuttingRequest, cuttingRequestPartNumbers: [...arr] },
											data: this.state.data ? this.state.data.map(e => {
												if (e.partNumber === elemPn.partNumber) {
													return { ...e, site: (option ? option.value : null) }
												}
												return e
											}) : null
										})
									}}
								/>}
							</td>
						</tr>)}
					</tbody>
				</table>
				</div>}
				{this.state.loadingMessage ?
					<div className='text-center'>
						<FontAwesomeIcon icon={faSpinner} spin /> {this.state.loadingMessage}
					</div>
					: <div>
						<div>
							<div className='table-responsive entity-table'>
								<table className='table table-bordered'>
									<thead className='entity-table-header'>
										<tr>
											<th>ID</th>
											<th>Part Number</th>
											<th>Description</th>
											<th>Kit textil</th>
											<th>Quantité</th>
											<th>wo</th>
											<th>woid</th>
											{/* <th>Site</th> */}
											<th>counter</th>
											<th style={{ padding: 3, width: 258 }}>
												<button className='btn btn-primary btn-sm'
													onClick={() => {
														if (this.state.selectedBoxs.length == this.state.data.length) {
															this.setState({ selectedBoxs: [] })
														} else {
															this.setState({ selectedBoxs: this.state.data.filter(e => e.nbrImprissionImp == null || e.nbrImprissionImp === "1") })
														}
													}}
												>All</button>
											</th>
										</tr>
									</thead>
									<tbody>
										{this.state.data.map((gamme, index) => {
											let selectedBox = this.state.selectedBoxs.find(e => e.id === gamme.id)
											return <tr className='clickable-element'
												onClick={() => this.state.selectedBoxs.map(e => e.id).includes(gamme.id)
													? this.setState({ selectedBoxs: this.state.selectedBoxs.filter(e => e.id != gamme.id) })
													: this.setState({ selectedBoxs: [...this.state.selectedBoxs, gamme].sort((a, b) => a.id.toString().localeCompare(b.id.toString())) })
												}
												style={this.state.selectedBoxs.map(e => e.id).includes(gamme.id) ? { backgroundColor: "rgb(221 221 221)" } : {}}
											>
												<td style={(gamme.nbrImprissionImp && gamme.nbrImprissionImp.trim() !== "1") ? { backgroundColor: "#ff7979" } : {}}>{gamme.id}
													{selectedBox && selectedBox.loaded !== true && <FontAwesomeIcon icon={faSpinner} spin />}
												</td>
												<td>{gamme.partNumber}</td>
												<td>{gamme.description}</td>
												<td>{gamme.item}</td>
												<td>{gamme.qtyBox}</td>
												<td>{gamme.wo}</td>
												<td>{gamme.woid}</td>
												{/* <td>{gamme.site}</td> */}
												<td>{gamme.counter}</td>
												<td>
													<div className='d-flex' style={{ alignItems: "center" }}>
														<input
															type="number" style={{ maxWidth: 100, minWidth: 50 }}
															className="form-control form-control-sm mr-1"
															value={gamme.splitQuantity}
															onClick={(e) => {
																e.stopPropagation();
															}}
															onChange={(e) => {
																this.setState({
																	data: this.state.data.map(elem => {
																		if (elem.id == gamme.id) {
																			return { ...elem, splitQuantity: e.target.value }
																		} else {
																			return elem;
																		}
																	})
																})
															}}
														/>
														<button className='btn btn-outline-primary btn-sm mt-1'
															onClick={(e) => {
																if (gamme.splitQuantity == 0) return alert("Veuillez saisir une quantité");
																axios.post(`/api/cuttingRequestV2/split?serie=${gamme.id}&qty=${gamme.splitQuantity}`)
																	.then(res => {
																		this.setState({ data: [], selectedBoxs: [] })
																		axios.get(`/api/gammeTechniqueImprimer/sequnce/${this.state.sequence}`)
																			.then(res => {
																				let counterPerPartnumber = {};

																				this.setState({
																					data: res.data.map(e => {
																						if (counterPerPartnumber[e.partNumberImp]) {
																							counterPerPartnumber[e.partNumberImp] = counterPerPartnumber[e.partNumberImp] + 1;
																						} else {
																							counterPerPartnumber[e.partNumberImp] = 1;
																						}
																						return {
																							id: e.nSerieGammeImp,
																							partNumber: e.partNumberImp,
																							description: e.descriptionImp,
																							item: e.code3Imp,
																							item5: e.code5Imp,
																							qtyBox: e.quantiteImp,
																							wo: e.nofImp,
																							woid: e.woidImp,
																							sequence: e.nSequenceImp,
																							nbrImprissionImp: e.nbrImprissionImp,
																							counter: counterPerPartnumber[e.partNumberImp],
																							total: res.data.filter(elem => elem.partNumberImp == e.partNumberImp && elem.nbrImprissionImp && elem.nbrImprissionImp.trim() === "1").length,

																						}
																					})
																				})
																			})
																	})
																e.stopPropagation();

															}}
														>
															Diviser
														</button>
														<button className='btn btn-outline-primary btn-sm mt-1'
															onClick={(e) => {
																this.setState({ modalDetail: { ...this.state.cmsData.find(elem => elem.nSerieGammeImp == gamme.id) } })
																e.stopPropagation();
															}}
														>
															Modifier
														</button>
													</div>
												</td>
											</tr>
										})}
									</tbody>
								</table>
							</div>
						</div>
						<div className='container'>
							<div className='d-flex'>
								<select
									className='form-control'
									value={this.state.selectedPartnumber}
									onChange={(e) => {
										this.setState({ selectedPartnumber: e.target.value })
									}}
								>
									<option value={""}></option>
									{partNumbers.map((partnumber, index) => {
										return <option key={index} value={partnumber}>{partnumber}</option>
									})}
								</select>
								<button
									className='btn btn-outline-primary ml-1'
									disabled={this.state.selectedPartnumber == null || this.state.sequence == null || this.state.selectedPartnumber.trim() === "" || this.state.sequence.trim() === ""}
									onClick={() => {
										axios.post(`/api/cuttingRequestV2/add-box2?sequence=${this.state.sequence}&pn=${this.state.selectedPartnumber}`)
											.then(res => {
												this.setState({ data: [], selectedBoxs: [] })
												axios.get(`/api/gammeTechniqueImprimer/sequnce/${this.state.sequence}`)
													.then(res => {
														let counterPerPartnumber = {};

														this.setState({
															data: res.data.map(e => {
																if (counterPerPartnumber[e.partNumberImp]) {
																	counterPerPartnumber[e.partNumberImp] = counterPerPartnumber[e.partNumberImp] + 1;
																} else {
																	counterPerPartnumber[e.partNumberImp] = 1;
																}
																return {
																	id: e.nSerieGammeImp,
																	partNumber: e.partNumberImp,
																	description: e.descriptionImp,
																	item: e.code3Imp,
																	item5: e.code5Imp,
																	qtyBox: e.quantiteImp,
																	wo: e.nofImp,
																	woid: e.woidImp,
																	sequence: e.nSequenceImp,
																	nbrImprissionImp: e.nbrImprissionImp,
																	counter: counterPerPartnumber[e.partNumberImp],
																	total: res.data.filter(elem => elem.partNumberImp == e.partNumberImp && elem.nbrImprissionImp && elem.nbrImprissionImp.trim() === "1").length,

																}
															})
														})
													})
											})
									}}
								>Ajouter</button>
							</div>
							<div className='d-flex pb-2' style={{ justifyContent: "end" }}>
								<button
									type="button" style={{ whiteSpace: "nowrap" }}
									className="btn btn-outline-primary ml-1"
									disabled={this.state.cuttingRequest == null}
									onClick={() => {
										axios.post(`/api/cuttingRequest/printSerie`,
											[...this.state.cuttingRequest.cuttingRequestSeries.map(elemCRS => { return { ...elemCRS, cuttingRequest: { ...this.state.cuttingRequest, cuttingRequestSeries: null, cuttingRequestBoxs: null, cuttingRequestPartNumbers: null } } })]
										)
											.then(res => {
												this.setState({ printingSeries: false })
											})
											.catch((err) => {
												this.setState({ printingSeries: false })
											})

									}}
								>
									{this.state.printingSeries ? <FontAwesomeIcon icon={faSpinner} spin /> : <FontAwesomeIcon icon={faPrint} />} Imprimer Serie {this.state.cuttingRequest ? `(${this.state.cuttingRequest.cuttingRequestSeries.length})` : ""}

								</button>

								<ReactToPrint
									onBeforeGetContent={() => {
										return new Promise((resolve, reject) => {
											this.setState({ modalRotate: true, selectedEmp: null }, () => resolve())
										});
									}}
									onAfterPrint={() => { this.setState({ modalRotate: false }) }}
									onPrintError={() => { this.setState({ modalRotate: false }) }}
									trigger={() => <button style={{ whiteSpace: "nowrap" }}
										type="button"
										className="btn btn-outline-success ml-1"
										disabled={!canPrintGammes}

									><FontAwesomeIcon icon={faPrint} /> Imprimer Gammes {countTotal > 0 && `(${countLoaded}/${countTotal})`}</button>}
									content={() => this.componentRef}
									pageStyle={`@page { size: A4 portrait; margin: 0 } @media print { body { -webkit-print-color-adjust: exact } }`}
								/>
								<ReactToPrint
									onBeforeGetContent={() => {
										return new Promise((resolve, reject) => {
											this.setState({}, () => resolve())
										});
									}}
									onAfterPrint={() => { }}
									onPrintError={() => { }}
									trigger={() => <button style={{ whiteSpace: "nowrap" }}
										disabled={this.state.cuttingRequest == null}
										type="button"
										className="btn btn-outline-danger ml-1"

									><FontAwesomeIcon icon={faPrint} /> Imprimer Plan</button>}
									content={() => this.planPrintPage}
								/>

							</div>
						</div>

						{this.state.cuttingRequest && this.renderSeriesTable()}
					</div>}
				<div style={{ overflow: "hidden", height: 0 }}>
					{this.state.cuttingRequest && this.renderPlanCoupe(this.state.cuttingRequest, user)}
				</div>

				<div style={{ overflow: "hidden", height: 0 }}>
					<div className='' ref={elem => this.componentRef = elem}>
						{this.state.selectedBoxs.length > 0 && this.state.selectedBoxs.map((box, idx) => {
							// GammePn renders each gamme as a 794x1123 A4 page; page-break BETWEEN gammes
							// (not after the last) avoids an extra trailing blank page.
							return [idx > 0 ? <div className="page-break" key={"pb-" + box.wo} /> : null,
							<div key={"gamme-" + box.wo}>
								<GammePn
									box={box}
									endLoading={() => {
										let selectedBoxs = this.state.selectedBoxs
										let index = selectedBoxs.findIndex(e => e.id == box.id)
										selectedBoxs[index].loaded = true
										this.setState({
											selectedBoxs
										})
									}}

								/>
							</div>]
						})}
					</div>
				</div>

				{this.renderModifModal()}
				{this.renderModifNbrCoucheModal()}
				{this.renderModelGoodExtraSeries()}
				{this.renderModifModalSerie()}
				{this.renderAddSerieModal()}
			</div>
		)
	}
}

GammeCMS.propTypes = {
	security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
	security: state.security
})

export default connect(mapStateToProps, {})(GammeCMS);
