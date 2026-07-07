import axios from 'axios';
import React, { Component } from 'react'
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import Select from "react-select";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEye, faSpinner, faTimes } from '@fortawesome/free-solid-svg-icons';
import GammePnForQN from './GammePnForQN';
import { Modal } from 'react-bootstrap'
import moment from 'moment';
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";

class QualityNoticeForm extends Component {

	constructor(props) {
		super(props);
		this.state = {
			modalObj: {},
			user: null,
			listCtcFiles: [],
			codeDefauts: [],
			showModalGamme: false,
			box: null,
			message: null,
			error: null,
			loading: false,
			rouleauDetails: [],
			loadingImage1: false,
			loadingImage2: false,
		}
	}

	componentDidMount() {
		if (this.props.security) {
			axios.get(`/api/user/${this.props.security.user.matricule}`)
				.then(res => {
					this.setState({
						user: { ...res.data },
						modalObj: {
							createdBy: res.data.firstName + " " + res.data.lastName + " : " + res.data.matricule,
							site: res.data.site ? res.data.site.nom.toUpperCase().replaceAll(" ", "") : null,
							coordinateur: res.data.chefDirect
						}
					})
				})
				.catch(err => {
					if (err.response.data != null && err.response.data.username === "Invalid Username") {
						window.location.pathname = "/login";
					}
				})
		}
		axios.get("/api/codeDefaut/listC")
			.then(res => {
				this.setState({ codeDefauts: res.data })
			})
			.catch(err => {
				console.log(err)
			})
	}

	handleChanges = (obj) => {
		this.setState({ modalObj: { ...this.state.modalObj, ...obj }, error: null, message: null })
	}

	renderModalGamme = (modelItemsId) => {
		return <Modal
			show={this.state.showModalGamme}
			onHide={() => {
				this.setState({ showModalGamme: false })
				if (this.state.modalObj.reftissu) {
					this.loadFournisseur()
				}
			}}
			dialogClassName="modal-90w"
			centered
		>
			{this.state.showModalGamme && <div style={{ maxHeight: "80vh", overflowY: 'auto' }}>
				<div style={{ position: "sticky", top: 0, left: 0, backgroundColor: "lightgray", padding: "5 15", fontSize: 20, zIndex: 2 }}>
					{modelItemsId.length} modèles sélectionnés: {modelItemsId.map(e => e[0]).join(" / ")}
				</div>
				{this.state.modalObj.partnumber && <GammePnForQN
					box={this.state.box}
					selectedIds={modelItemsId.map(e => e[0]) || []}
					updateSelectedIds={(selectedIds) => {
						let reftissu = null, description = null

						if (selectedIds.length > 0) {
							let objFirst = this.state.listCtcFiles.find(e => e.panelNumber === selectedIds[0])
							reftissu = objFirst ? objFirst.partNumberMaterial : null
							description = objFirst ? objFirst.partNumberMaterialDescription : null
						}

						if (selectedIds.length > 0 && this.state.listCtcFiles.length > 0) {
							this.state.listCtcFiles
								.filter(e => selectedIds.includes(e.panelNumber))
								.map(option => {
									if (reftissu != null && reftissu != option.partNumberMaterial) {
										selectedIds = selectedIds.filter(e => e !== option.panelNumber)
									} else {
										reftissu = option.partNumberMaterial
									}
								})
						}
						this.handleChanges({ numEmp: selectedIds.map(e => e + " : 1").join(" / "), reftissu: reftissu, quantite: selectedIds.length, reftissuDescription: description })
						return selectedIds
					}}
				/>}
			</div>}
		</Modal>
	}

	renderConfirme = () => {
		return (this.state.message && Object.keys(this.state.message).length !== 0)
			&& !(Object.keys(this.state.message).length == 1) && <div className="alert alert-success alert-error text-center m-4" role="alert">
				<ul>
					<button type='btn' className="btn btn-toclose" onClick={() => { this.setState({ message: null }) }}>
						<FontAwesomeIcon icon={faTimes} size="2x" />
					</button>
					{this.renderErrorsAlert(this.state.message)}
				</ul>
			</div>
	}


	handleSave = () => {
		let error= []
		if (!this.state.modalObj.codeDefaut) {
			error.push("Veuillez remplir le codeDefaut")
		}
		if (!this.state.modalObj.reftissu) {
			error.push("Veuillez remplir le reftissu")
		}
		if (!this.state.modalObj.wo) {
			error.push("Veuillez remplir le WO")
		}
		if(error.length > 0){
			this.setState({ error: error })
			return
		}
		this.setState({ loading: true })
		axios.post("/api/qualityNotice/qnSave", this.state.modalObj)
			.then(res => {
				this.setState({
					modalObj: {
						coordinateur: this.state.modalObj.coordinateur,
						createdBy: this.state.modalObj.createdBy,
						site: this.state.modalObj.site,
						wo: "",
						quantite: "",
						description: ""
					},
					listCtcFiles: [],
					box: null,
					message: "Enregistrement effectué avec succès QN: " + res.data.numeroQn,
					loading: false
				})
			})
			.catch(err => {
				this.setState({ error: err.response.data, loading: false })
				console.log(err)
			})
	}

	renderError = () => {
		return (this.state.error && Object.keys(this.state.error).length !== 0)
			&& <div className="alert alert-danger alert-error text-center m-4" role="alert">
				<ul>
					<button type='btn' className="btn btn-toclose" onClick={() => { this.setState({ error: null }) }}>
						<FontAwesomeIcon icon={faTimes} size="2x" />
					</button>
					{this.renderErrorsAlert(this.state.error)}
				</ul>
			</div>
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

	renderExtraEmails = () => {
		let arr = this.state.modalObj.extraEmails ? this.state.modalObj.extraEmails.split(";") : [""]
		return <div className='row py-2'>
			<label className='col-4 col-form-label text-right'>Extra Emails :</label>
			<div className='col-4 p-0'>
				{arr.map((email, index) => {
					return <div className='d-flex mb-1'><input type='text' className='form-control form-control-sm mr-1' ref={ref => this.woRef = ref} key={index}
						value={email}
						style={{ width: '300px' }}
						onChange={(e) => {
							this.handleChanges({ extraEmails: arr.map((elem, i) => i === index ? e.target.value.trim().toLowerCase() : elem).join(";") })
						}}
					/>
						<button className='btn btn-sm btn-danger' onClick={() => {
							arr.splice(index, 1)
							this.handleChanges({ extraEmails: arr.join(";") })
						}}>-</button>
					</div>
				})}
				<button className='btn btn-sm btn-primary' onClick={() => {
					arr.push("")
					this.handleChanges({ extraEmails: arr.join(";") })
				}}>+</button>
			</div>
		</div>
	}

	loadRouleauDetails = () => {
		this.setState({ rouleauDetails: null })
		axios.get("/api/cuttingRequestSerieData/getSeries/" + this.state.modalObj.sequence + "/" + this.state.modalObj.reftissu)
			.then(res => {
				let arrSerie = res.data
				axios.get("/api/cuttingRequestSerieRouleauData/bySeries/" + arrSerie.join(","))
					.then(res2 => {
						if (res2.data.length > 0) {
							this.setState({
								rouleauDetails: res2.data,
								modalObj: {
									...this.state.modalObj,
									idRouleau: res2.data[0].idRouleau,
									lotFrs: res2.data[0].lotFrs,
									dateCoupe: res2.data[0].createdAt
								}
							})
						} else {
							this.setState({ rouleauDetails: res2.data })
						}
					})
					.catch(err => {
						this.setState({ rouleauDetails: [] })
						console.log(err)
					})
			})
			.catch(err => {
				this.setState({ rouleauDetails: [] })
				console.log(err)
			})
	}

	loadFournisseur = () => {
		axios.get("/api/query/refDetails?reftissu=" + this.state.modalObj.reftissu)
			.then(res => {
				this.handleChanges({ nomFournisseur: res.data.fournisseur, reftissuDescription: res.data.description })
			})
			.catch(err => {
				console.log(err)
			})
	}

	renderTableRouleauDetails = () => {
		/*
		sh
		*/
		return <div className='table-responsive entity-table'>
			<table className='table table-bordered m-0'>
				<thead>
					<tr>
						<th>Serie</th>
						<th>idRouleau</th>
						<th>lotFrs</th>
						<th>totalUsage</th>
						<th>createdAt</th>
					</tr>
				</thead>
				<tbody>
					{this.state.rouleauDetails === null ? <tr><td colSpan="6">loading...</td></tr> :
						this.state.rouleauDetails.length === 0 ? <tr><td colSpan="6">No data</td></tr> :
							this.state.rouleauDetails.map((rouleau, index) => {
								return <tr key={index}
									onClick={() => {
										this.handleChanges({ idRouleau: rouleau.idRouleau, lotFrs: rouleau.lotFrs, dateCoupe: rouleau.createdAt })
									}}
								>
									<td>{rouleau.serie}</td>
									<td>{rouleau.idRouleau}</td>
									<td>{rouleau.lotFrs}</td>
									<td>{rouleau.totalUsage}</td>
									<td>{rouleau.createdAt}</td>
								</tr>
							})}
				</tbody>
			</table>
		</div>
	}

	render() {
		const modelItemsId = this.state.modalObj.numEmp ? this.state.modalObj.numEmp.split(" / ").map(e => {
			let arr = e.split(" : ")
			if (arr.length === 1) arr.push("1")
			return [arr[0], arr[1]]
		}) : []
		const options = this.state.listCtcFiles.map(e => { return { label: e.panelNumber, value: e.panelNumber, item: e } }) || []

		return (
			<div>
				<h1 className='text-center p-2'>
					Lear Quality Notice
				</h1>
				<div className='m-3'>
					<div className='row py-2'>
						<div className='col-4 text-right'>
							Matricule controleur:
						</div>
						<div className='col-2 p-0'>
							<span>
								{this.state.modalObj.createdBy}
							</span>
						</div>
						<div className='col-2 text-right'>
							Site:
						</div>
						<div className='col-4 p-0'>
							<span>
								{this.state.modalObj.site}
							</span>
						</div>
					</div>

					<div className='row py-2'>
						<div className='col-4 text-right'>
							Chef direct:
						</div>
						<div className='col-2 p-0'>
							<span>
								{this.state.modalObj.coordinateur}
							</span>
						</div>
					</div>
					{/* <div className='row py-2'>
						<label className='col-4 col-form-label text-right'>Quantité Défaut :</label>
						<div className='col-4 p-0'>
							<input type='number' className='form-control form-control-sm' ref={ref => this.quantiteRef = ref}
								value={this.state.modalObj.quantite}
								style={{ width: '100px' }}
								onChange={(e) => {
									this.handleChanges({ quantite: e.target.value })
								}}
							/>
						</div>
					</div> */}
					<div className='row py-2'>
						<label className='col-4 col-form-label text-right'>WO :</label>
						<div className='col-4 p-0'>
							<input type='text' className='form-control form-control-sm' ref={ref => this.woRef = ref}
								value={this.state.modalObj.wo}
								style={{ width: '100px' }}
								onChange={(e) => {
									this.setState({
										modalObj: {
											...this.state.modalObj,
											wo: e.target.value,
											sequence: null,
											partnumber: null,
											projet: null,
											numEmp: null,
											quantite: null,
											reftissu: null,
										},
										listCtcFiles: [],
										box: null,
										message: null,
										error: null
									})

								}}
								onKeyUp={(e) => {
									if (e.key === "Enter") {
										e.target.blur()
									}
								}}
								onBlur={(e) => {
									if (this.state.modalObj.wo && this.state.modalObj.wo.length > 0) {
										axios.get(`/api/cuttingRequestBoxData/wo/${this.state.modalObj.wo}`)
											.then(respondWo => {
												this.setState({
													modalObj: {
														...this.state.modalObj,
														partnumber: respondWo.data.partNumber,
														sequence: respondWo.data.sequence,
													},
													box: respondWo.data
												})

												axios.get(`/api/ctcFiles/pn/${respondWo.data.partNumber}`)
													.then(res => {
														this.setState({
															modalObj: {
																...this.state.modalObj,
																projet: res.data[0].projet
															},
															listCtcFiles: res.data

														})
													})
											})
											.catch(err => {
												this.setState({ modalObj: { ...this.state.modalObj, wo: "", sequence: null, partnumber: null, projet: null } })
												console.log(err)
											})
									}
								}}
							/>
						</div>
					</div>

					{this.state.modalObj.wo && this.state.modalObj.wo.length > 0 && [<div className='row py-2'>
						<div className='col-4 text-right'>
							Sequence:
						</div>
						<div className='col-2 p-0'>
							<span>
								{this.state.modalObj.sequence}
							</span>
						</div>
					</div>,
					<div className='row py-2'>
						<div className='col-4 text-right'>
							Partnumber:
						</div>
						<div className='col-2 p-0'>
							<span>
								{this.state.modalObj.partnumber}
							</span>
						</div>
					</div>,
					<div className='row py-2'>
						<div className='col-4 text-right'>
							Projet:
						</div>
						<div className='col-2 p-0'>
							<span>
								{this.state.modalObj.projet}
							</span>
						</div>
					</div>,
					<div className='row py-2'>
						<label className='col-4 col-form-label text-right'>Num Emp :</label>
						{/* <Select id={"numEmp"} name={"numEmp"} classNamePrefix="rs" className='col-4 p-0 z-index-11'
							isClearable={true} isMulti placeholder={"Num Emp ..."}
							value={options.filter((elem) => modelItemsId.includes(elem.value))}
							options={options}
							onChange={(options) => {
								let arr = []
								options.map(option => {
									if (option.item && !arr.includes(option.item.partNumberMaterial)) {
										arr.push(option.item.partNumberMaterial)
									}
								})
								this.handleChanges({ numEmp: options.map(option => option.value).join(" / "), reftissu: arr.join(" / ") })
							}}
						/> */}
						<div className='col-4 p-0  z-index-11 table-responsive entity-table'>
							<table className='table table-bordered m-0'>
								<thead>
									<tr>
										<th>Num Emp</th>
										<th>Quantité</th>
										<th></th>
									</tr>
								</thead>
								<tbody>
									{modelItemsId.map((emp, index) => {
										return <tr key={index}>
											<td>
												{emp[0]}
											</td>
											<td className='table-elem-sm'>
												<input type='number' className='form-control form-control-sm' ref={ref => this.quantiteRef = ref}
													value={emp[1]}
													// style={{ width: '100px' }}
													onChange={(e) => {
														let arr = modelItemsId.map((elem, i) => i === index ? [elem[0], e.target.value] : elem)
														this.handleChanges({ numEmp: arr.map(e => e.join(" : ")).join(" / "), quantite: arr.reduce((acc, curr) => acc + parseInt(curr[1]), 0) })
													}}
												/>
											</td>
											<td>
												<button className='btn btn-sm btn-danger' onClick={() => {
													let arr = modelItemsId.filter((elem, i) => i !== index)
													this.handleChanges({ numEmp: arr.map(e => e.join(" : ")).join(" / "), quantite: arr.reduce((acc, curr) => acc + parseInt(curr[1]), 0) })
												}}>-</button>
											</td>
										</tr>
									})}
									<tr style={{ backgroundColor: "#f5f5f5" }}>
										<td style={{ fontWeight: "bold" }}>Total</td>
										<td style={{ fontWeight: "bold" }}>{this.state.modalObj.quantite}</td>
										<td></td>
									</tr>
								</tbody>
							</table>
						</div>

						{this.state.box && <button className='btn btn-outline-primary'
							style={{ padding: "0 10", height: 30, marginLeft: 10 }}
							onClick={() => {
								this.setState({ showModalGamme: true })
							}}
						>
							<FontAwesomeIcon icon={faEye} /> Gamme
						</button>}

					</div>]}
					{/* {!(this.state.modalObj.wo && this.state.modalObj.wo.length > 0)
						?  */}
						<div className='row py-2'>
							<label className='col-4 col-form-label text-right'>Reftissu :</label>
							<div className='col-4 p-0'>
								<input className='form-control form-control-sm'
									value={this.state.modalObj.reftissu || ""}
									// style={{ width: '100px' }}
									onChange={(e) => {
										this.handleChanges({ reftissu: e.target.value })
									}}
									onKeyUp={(e) => {
										if (e.key === "Enter") {
											e.target.blur()
										}
									}}
									onBlur={(e) => {
										if (this.state.modalObj.reftissu && this.state.modalObj.reftissu.length > 0) {
											this.loadFournisseur()
										}
									}}
								/>
							</div>
						</div>

						{/* : <div className='row py-2'>
							<div className='col-4 text-right'>
								Reftissu:
							</div>
							<div className='col-4 p-0'>
								<span>
									{this.state.modalObj.reftissu}
								</span>
							</div>
						</div>} */}

					<div className='row py-2'>
						<label className='col-4 col-form-label text-right'>Type Défaut :</label>
						<Select classNamePrefix="rs" className='col-4 p-0 z-index-11' id={"typeDefaut"} name={"typeDefaut"}
							placeholder={"code Defaut"}
							isClearable={false}
							value={this.state.modalObj.typeDefaut ? { label: this.state.modalObj.typeDefaut, value: this.state.modalObj.typeDefaut } : null}
							options={[
								{ label: "Défaut coupe", value: "Défaut coupe" }, 
								{ label: "Défaut fournisseur", value: "Défaut fournisseur" }, 
								{ label: "Défaut logistique", value: "Défaut logistique" },
								{ label: "Défaut CNC", value: "Défaut CNC" }
							]}
							onChange={(option) => {
								this.handleChanges({ ...this.state.modalObj, typeDefaut: option ? option.value : null, codeDefaut: null })
								if (this.state.modalObj.site === "TRIM1" && (option.value === "Défaut fournisseur" || option.value === "Défaut logistique")) {
									if (this.state.modalObj.wo && this.state.modalObj.wo.length > 0) {
										this.loadRouleauDetails()
									}
									this.loadFournisseur()
								}
							}}
						/>
					</div>
					<div className='row py-2'>
						<label className='col-4 col-form-label text-right'>Code Defaut :</label>

						<Select classNamePrefix="rs" className='col-4 p-0 z-index-11' id={"codeDefaut"} name={"codeDefaut"}
							placeholder={"code Defaut"}
							isClearable={false}
							value={this.state.modalObj.codeDefaut ? { label: this.state.modalObj.codeDefaut.code + " : " + this.state.modalObj.codeDefaut.description, value: this.state.modalObj.codeDefaut } : null}
							options={this.state.codeDefauts.filter(e => {
								//if Défaut coupe then codeDefauts.code starts with 'C' else if Défaut fournisseur then codeDefauts.code starts with 'F' else all
								return (this.state.modalObj.typeDefaut === "Défaut coupe" && e.code.startsWith("C")) 
								|| (this.state.modalObj.typeDefaut === "Défaut fournisseur" && e.code.startsWith("CF")) 
                                || (this.state.modalObj.typeDefaut === "Défaut logistique" && e.code.startsWith("CL")) 
								|| (this.state.modalObj.typeDefaut === "Défaut CNC" && e.code.startsWith("CNC")) 
								|| (!this.state.modalObj.typeDefaut)
							}).map(codeDefaut => ({ label: codeDefaut.code + " : " + codeDefaut.description, value: codeDefaut }))}
							onChange={(option) => {
								this.handleChanges({ ...this.state.modalObj, codeDefaut: option ? option.value : null })
							}}
						/>
					</div>
					{this.state.modalObj.site === "TRIM1" && <>
						{(this.state.modalObj.typeDefaut === "Défaut fournisseur"|| this.state.modalObj.typeDefaut === "Défaut logistique") && [
							(this.state.modalObj.wo && this.state.modalObj.wo.length > 0 && <div className='row py-2'>
								<div className='col-4 text-right'>
									Rouleau Details :
								</div>
								<div className='col-6 p-0'>
									{this.renderTableRouleauDetails()}
								</div>
							</div>),
							<div className='row py-2'>
								<label className='col-4 col-form-label text-right'>reftissuDescription :</label>
								<div className='col-4 p-0'>
									<input className='form-control form-control-sm'
										value={this.state.modalObj.reftissuDescription || ""}
										// style={{ width: '100px' }}
										onChange={(e) => {
											this.handleChanges({ reftissuDescription: e.target.value })
										}}
									/>
								</div>
							</div>,
							<div className='row py-2'>
								<label className='col-4 col-form-label text-right'>nomFournisseur :</label>
								<div className='col-4 p-0'>
									<input className='form-control form-control-sm'
										value={this.state.modalObj.nomFournisseur || ""}
										onChange={(e) => {
											this.handleChanges({ nomFournisseur: e.target.value })
										}}
									/>
								</div>
							</div>,


							<div className='row py-2'>
								<label className='col-4 col-form-label text-right'>idRouleau :</label>
								<div className='col-4 p-0'>
									<input className='form-control form-control-sm'
										value={this.state.modalObj.idRouleau || ""}
										style={{ maxWidth: '250px' }}
										onChange={(e) => {
											this.handleChanges({ idRouleau: e.target.value })
										}}
									/>
								</div>
							</div>,

							<div className='row py-2'>
								<label className='col-4 col-form-label text-right'>lotFrs :</label>
								<div className='col-4 p-0'>
									<input className='form-control form-control-sm'
										value={this.state.modalObj.lotFrs || ""}
										style={{ maxWidth: '250px' }}
										onChange={(e) => {
											this.handleChanges({ lotFrs: e.target.value })
										}}
									/>
								</div>
							</div>,

							<div className='row py-2'>
								<label className='col-4 col-form-label text-right'>dateCoupe :</label>
								<div className='col-4 p-0'>
									<input
										type="datetime-local"
										id="dateCoupe"
										name="dateCoupe"
										placeholder="Date Coupe"
										className="form-control"
										autoComplete="off"
										value={this.state.modalObj.dateCoupe ? moment(this.state.modalObj.dateCoupe.replace(", ", " ")).format('YYYY-MM-DDTHH:mm') : ''}
										onChange={event => {
											if (event.target.value) {
												this.handleChanges({ dateCoupe: moment(event.target.value).format('YYYY-MM-DDTHH:mm').replace("T", ", ") })
											} else {
												this.handleChanges({ dateCoupe: null })
											}
										}}

									/>
								</div>
							</div>,


						]}
					</>}
					<div className='row py-2'>
						<label className='col-4 col-form-label text-right'>Métrage écarté :</label>
						<div className='col-4 p-0'>
							<input type='number' className='form-control form-control-sm'
								value={this.state.modalObj.metrageEcarte || ""}
								style={{ width: '100px' }}
								onChange={(e) => {
									this.handleChanges({ metrageEcarte: e.target.value })
								}}
							/>
						</div>
					</div>


					<div className='row py-2'>
						<label className='col-4 col-form-label text-right'>Description :</label>
						<div className='col-4 p-0'>
							<textarea type='text' className='form-control form-control-sm' ref={ref => this.descriptionRef = ref}
								value={this.state.modalObj.description} rows={5}
								onChange={(e) => {
									this.handleChanges({ ...this.state.modalObj, description: e.target.value })
								}}
							/>
						</div>
					</div>
					<div className='row py-2'>
						<label className='col-2 col-form-label text-right'>image1:</label>
						<div className='col-4 p-0'>
							<input type="file"
								onChange={(e) => {
									let formData = new FormData()
									formData.append('file', e.target.files[0])
									this.setState({ loadingImage1: true })

									axios.post(`/api/file/store`, formData, {
										headers: {
											"Content-type": "application/json",
											"Content-Type": "multipart/form-data"
										}
									})
										.then((res) => {
											this.setState({ modalObj: { ...this.state.modalObj, image1: res.data }, loadingImage1: false })
										})
										.catch((err) => {
											this.setState({ loadingImage1: false })
											console.log({ err })
										})
								}}
							/>
							{this.state.loadingImage1 && <span><FontAwesomeIcon icon={faSpinner} spin /></span>}
							{this.state.modalObj.image1 &&
								<span style={{ fontSize: 12 }}
									className="btn btn-link"
									onClick={() => {
										axios(
											{
												url: `/api/file/` + this.state.modalObj.image1, //your url
												method: 'GET',
												responseType: 'blob', // important
											}
										).then((response) => {
											const url = window.URL.createObjectURL(new Blob([response.data]));
											const link = document.createElement('a');
											link.href = url;
											link.setAttribute('download', this.state.modalObj.image1); //or any other extension
											document.body.appendChild(link);
											link.click();
										})
									}}
								>
									<img src={`/api/file/${this.state.modalObj.image1}`} style={{ maxWidth: "100%", maxHeight: "400px" }} />
								</span>}
						</div>
						<label className='col-2 col-form-label text-right'>image2:</label>
						<div className='col-4 p-0'>
							<input type="file"
								onChange={(e) => {
									let formData = new FormData()
									formData.append('file', e.target.files[0])
									this.setState({ loadingImage2: true })
									axios.post(`/api/file/store`, formData, {
										headers: {
											"Content-type": "application/json",
											"Content-Type": "multipart/form-data"
										}
									})
										.then((res) => {
											this.setState({ modalObj: { ...this.state.modalObj, image2: res.data }, loadingImage2: false })
										})
										.catch((err) => {
											this.setState({ loadingImage2: false })
											console.log({ err })
										})
								}}
							/>
							{this.state.loadingImage2 && <span><FontAwesomeIcon icon={faSpinner} spin /></span>}
							{this.state.modalObj.image2 &&
								<span style={{ fontSize: 12 }}
									className="btn btn-link"
									onClick={() => {
										axios(
											{
												url: `/api/file/` + this.state.modalObj.image2, //your url
												method: 'GET',
												responseType: 'blob', // important
											}
										).then((response) => {
											const url = window.URL.createObjectURL(new Blob([response.data]));
											const link = document.createElement('a');
											link.href = url;
											link.setAttribute('download', this.state.modalObj.image2); //or any other extension
											document.body.appendChild(link);
											link.click();
										})
									}}
								>
									<img src={`/api/file/${this.state.modalObj.image2}`} style={{ maxWidth: "100%", maxHeight: "400px" }} />
								</span>}
						</div>
					</div>
					{this.renderExtraEmails()}
					<div className='row py-2'>
						<div className='col-4 col-form-label text-right'></div>
						<button className='btn btn-sm btn-warning col-2' onClick={() => {
							this.setState({
								modalObj: {
									coordinateur: this.state.modalObj.coordinateur,
									createdBy: this.state.modalObj.createdBy,
									site: this.state.modalObj.site,
									quantite: "",
									wo: "",
									description: ""
								},
								listCtcFiles: [],
								box: null
							})
						}}>Reset</button>
						<button className='btn btn-sm btn-primary col-2 ml-2'
							onClick={() => this.handleSave()}
							disabled={this.state.loading}
						>Enregistrer</button>
					</div>

				</div>
				{this.renderModalGamme(modelItemsId)}
				{this.renderError()}
				{this.renderConfirme()}

			</div>
		)
	}
}

QualityNoticeForm.propTypes = {
	security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
	security: state.security
})

export default connect(mapStateToProps, {})(QualityNoticeForm);

