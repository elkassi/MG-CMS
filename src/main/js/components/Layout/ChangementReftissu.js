import { faCheck, faInfo, faMagnifyingGlass, faPlus, faTimes, faTrashAlt } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import axios from 'axios';
import React, { Component } from 'react'
import Select from "react-select";
import { FormControl, InputGroup, Button, Dropdown, Modal } from "react-bootstrap";

export default class ChangementReftissu extends Component {

	constructor() {
		super();
		this.state = {
			optionsList: {},
			modalObj: {},
			data: [],
			partNumberMaterialConfigs: {},
			error: [],
			goodReftissu: []
		}
	}

	componentDidMount() {
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

	renderTissuTable = () => {
		let arr = this.state.modalObj.reftissuArr ? this.state.modalObj.reftissuArr.split(";").map(elem => elem.split(":")) : [["", ""]]
		return <div>
			<table className='table m-0 table table-grey-border'>
				<thead>
					<tr>
						<th className='table-elem-sm'>OLD</th>
						<th className='table-elem-sm'>NEW</th>
						<th className='table-elem-sm'>Status</th>
						<th className='table-elem-sm'></th>
					</tr>
				</thead>
				<tbody>
					{arr.map((elem, ind) => <tr>
						<td className='table-elem-sm'>
							<div className='d-flex'>
								<input className='form-control entityform-input' value={elem[0] || ""} style={{ height: 30 }}
									onChange={(event) => {
										arr[ind][0] = event.target.value
										this.setState({
											modalObj: { ...this.state.modalObj, reftissuArr: arr.map(e => e.join(":")).join(";") },
										})
									}}
								/>
								{this.state.partNumberMaterialConfigs[elem[0]] && <button
									className='btn btn-outline-primary px-1 ml-1'
									style={{
										fontSize: 10,
										width: 25,
										borderRadius: "50%"
									}}
									onClick={() => {
										this.setState({ reftissuConfig: elem[0] })
									}}>
									<FontAwesomeIcon icon={faInfo} />
								</button>}
							</div>
						</td>
						<td className='table-elem-sm'>
							<div className='d-flex'>
								<input className='form-control entityform-input' value={elem[1] || ""} style={{ height: 30 }}
									onChange={(event) => {
										arr[ind][1] = event.target.value
										this.setState({ modalObj: { ...this.state.modalObj, reftissuArr: arr.map(e => e.join(":")).join(";") } })
									}}
								/>
								{this.state.partNumberMaterialConfigs[elem[1]] && <button
									className='btn btn-outline-primary px-1 ml-1'
									style={{
										fontSize: 10,
										width: 25,
										borderRadius: "50%"
									}}
									onClick={() => {
										this.setState({ reftissuConfig: elem[1] })
									}}>
									<FontAwesomeIcon icon={faInfo} />
								</button>}
							</div>
						</td>
						<td>{this.state.goodReftissu.includes(arr[ind][0]) && <FontAwesomeIcon icon={faCheck} color="green" />}</td>
						<td className='table-elem-sm'>
							<button className='btn btn-outline-dark' onClick={() => {
								arr.splice(ind, 1);
								this.setState({ modalObj: { ...this.state.modalObj, reftissuArr: arr.map(e => e.join(":")).join(";") } })
							}}><FontAwesomeIcon icon={faTrashAlt} /></button>
						</td>
					</tr>)}
				</tbody>
			</table>
			<div className='d-flex'>
				<button
					className='btn btn-outline-danger'
					onClick={() => {
						arr.push(["", ""])
						this.setState({ modalObj: { ...this.state.modalObj, reftissuArr: arr.map(e => e.join(":")).join(";") } })
					}}
				><FontAwesomeIcon icon={faPlus} /></button>
				<button
					className='btn btn-outline-danger'
					onClick={() => {
						axios.get(`/api/cuttingPlanMaterialPlacementInfo/reftissus/${arr.map(e => e[0]).join(",")}${this.state.modalObj.projet ? `?projet=${this.state.modalObj.projet}` : ""}`)
							.then(res => {
								this.setState({ data: res.data })
							})
						let obj = {}
						axios.get(`/api/partNumberMaterialConfig/pns/${arr.map(e => e.join(",")).join(",")}`)
							.then(res => {
								res.data.map(e => {
									obj[e.partNumberMaterial] = { ...e }
								})
								this.setState({ partNumberMaterialConfigs: { ...obj } })
							})
					}}
				><FontAwesomeIcon icon={faMagnifyingGlass} /></button>
				<button
					className='btn btn-outline-danger'
					onClick={() => {
						let error = []
						let arrGoodReftissu =[]
						arr.map((elem, ind) => {
							let errorExist = false
							let config1 = this.state.partNumberMaterialConfigs[elem[0]]
							let config2 = this.state.partNumberMaterialConfigs[elem[1]]
							if (config1 && config2) {
								if (!(config1.matelassageEndroit && config2.matelassageEndroit && config1.matelassageEndroit === config2.matelassageEndroit)) {
									error.push("Vérifiez Matelassage Endroit des matière " + elem[0] + " et " + elem[1]); errorExist=true;
								}
								config1.reftissuMachines.map(machineObj => {
									let machineObj2 = config2.reftissuMachines.find(e => e.machineType === machineObj.machineType)
									if (machineObj2) {
										if (machineObj.maxPlie !== machineObj2.maxPlie) {
											error.push("Vérifiez maxPlie machine " + machineObj.machineType + " dans la configuration de la matière  " + elem[1]); errorExist=true;
										}
										if (machineObj.maxDrill !== machineObj2.maxDrill) {
											error.push("Vérifiez maxDrill machine " + machineObj.machineType + " dans la configuration de la matière  " + elem[1]); errorExist=true;
										}
										if (machineObj.maxPlieDrill !== machineObj2.maxPlieDrill) {
											error.push("Vérifiez maxPlieDrill machine " + machineObj.machineType + " dans la configuration de la matière  " + elem[1]); errorExist=true;
										}
										if (machineObj.pliesConfig !== machineObj2.pliesConfig) {
											error.push("Vérifiez pliesConfig machine " + machineObj.machineType + " dans la configuration de la matière  " + elem[1]); errorExist=true;
										}
									} else {
										error.push("Vérifiez la machine " + machineObj.machineType + " non trouvé dans la configuration de la matière  " + elem[1]); errorExist=true;
									}
								})
								config1.reftissuCategories.map(obj => {
									let obj2 = config2.reftissuCategories.find(e => e.category === obj.category)
									if (obj2) {
										if (obj.borneMin !== obj2.borneMin) {
											error.push("Vérifiez borneMin category " + obj.category + " dans la configuration de la matière  " + elem[1]); errorExist=true;
										}
										if (obj.borneMax !== obj2.borneMax) {
											error.push("Vérifiez borneMax category " + obj.category + " dans la configuration de la matière  " + elem[1]); errorExist=true;
										}
									} else {
										error.push("Vérifiez la category " + obj.category + " non trouvé dans la configuration de la matière  " + elem[1]); errorExist=true;
									}
								})
							} else {
								error.push("Vérifiez configuration des matière " + elem[0] + " et " + elem[1]); errorExist=true;
							}
							if(!errorExist) {
								arrGoodReftissu.push(elem)
							}
						})
						this.setState({ error: error, reftissuArr : arr })
						if(arrGoodReftissu.length > 0) {
							let arrCuttingPlanId = []
							let arrUpdate = this.state.data.map(elem2 => {
								if(arrGoodReftissu.map(e => e[0]).includes(elem2.cuttingPlanMaterial)) {
									elem2.status = "loading"
								}
								return elem2
							})
							this.setState({data: arrUpdate , goodReftissu : arrGoodReftissu.map(e=>e[0])})
							arrUpdate.map((elem,ind) => {
								if(arrGoodReftissu.map(e => e[0]).includes(elem.cuttingPlanMaterial)) {
									if(!arrCuttingPlanId.includes(elem.cuttingPlan)) {
										arrCuttingPlanId.push(elem.cuttingPlan)
										axios.post(`/api/cuttingPlan/${elem.cuttingPlan}/convertReftissu/${arrGoodReftissu.filter(e => arrUpdate.filter(data => data.cuttingPlan == elem.cuttingPlan).map(data => data.cuttingPlanMaterial).includes(e[0])).map(e => e.join(":")).join(",")}`)	
										.then(res=> {
											let arrUpdate = this.state.data.map(elem2 => {
												if(arrGoodReftissu.map(e => e[0]).includes(elem2.cuttingPlanMaterial) && elem.cuttingPlan === elem2.cuttingPlan) {
													elem2.status = "good"
												}
												return elem2
											})
											this.setState({data: arrUpdate})
										})
										.catch(err => {
											let arrUpdate = this.state.data.map(elem2 => {
												if(arrGoodReftissu.map(e => e[0]).includes(elem2.cuttingPlanMaterial) && elem.cuttingPlan === elem2.cuttingPlan) {
													elem2.status = "bad"
												}
												return elem2
											})
											this.setState({data: arrUpdate})
										})
									}
								}
							})
						}
					}}
				>Convert</button>
			</div>
		</div>
	}

	renderErrorsAlert(errors) {
		let arr = [];
		for (let prop in errors) {
			if (typeof errors[prop] === 'string') {
				arr.push(<li>{parseInt(prop) + 1}: {errors[prop]}</li>)
			} else if (typeof errors[prop] === "object") {
				if (Object.keys(errors[prop]).length > 0) {
					arr.push(<li>{prop}: <ul>{this.renderErrorsAlert(errors[prop])}</ul></li>)
				}

			}
		}
		return arr
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

	renderData = () => {
		return <div className='row entityform-field col-12'>
			<table className='table m-0 table table-grey-border'>
				<thead>
					<tr>
						<th className='table-elem-sm'>Cutting Plan</th>
						<th className='table-elem-sm'>Reftissu</th>
						<th className='table-elem-sm'>Placement</th>
						<th className='table-elem-sm'>Machine</th>
						<th className='table-elem-sm'>Category</th>
						<th className='table-elem-sm'>Laize</th>
						<th className='table-elem-sm'>maxPlie</th>
						<th className='table-elem-sm'>maxPlieDrill</th>
						<th className='table-elem-sm'>maxDrill</th>
						<th className='table-elem-sm'>Action</th>
					</tr>
				</thead>
				<tbody>
					{this.state.data.map((elem, ind) => {
						return <tr key={"data-" + ind} style={
							elem.status === "loading" ? {backgroundColor: "grey"} : 
							elem.status === "good" ? { backgroundColor: "rgb(206 255 179)"} : 
							elem.status === "bad" ? {backgroundColor: "#ffdddd"} : 
							{}}>
							<td className='table-elem-sm clickable-elem' onDoubleClick={() => {
								window.open(`/cuttingPlan/${elem.cuttingPlan}`, '_blank').focus();
							}}>{elem.cuttingPlan}</td>
							<td className='table-elem-sm'>{elem.cuttingPlanMaterial}</td>
							<td className='table-elem-sm'>{elem.placement}</td>
							<td className='table-elem-sm'>{elem.machine}</td>
							<td className='table-elem-sm'>{elem.category}</td>
							<td className='table-elem-sm'>{elem.laize}</td>
							<td className='table-elem-sm'>{elem.maxPlie}</td>
							<td className='table-elem-sm'>{elem.maxPlieDrill}</td>
							<td className='table-elem-sm'>{elem.maxDrill}</td>
							<td className='table-elem-sm'>
								<button className='btn btn-sm btn-outline-success' disabled={elem.status === "loading" || elem.status === "good"}
									onClick={() => {
										let arr = this.state.modalObj.reftissuArr ? this.state.modalObj.reftissuArr.split(";").map(e => e.split(":")) : []
										let matchingEntry = arr.find(e => e[0] === elem.cuttingPlanMaterial)
										if (!matchingEntry) {
											alert("Aucune correspondance OLD trouvée pour " + elem.cuttingPlanMaterial)
											return
										}
										// Mark this row as loading
										let arrUpdate = this.state.data.map((elem2, ind2) => {
											if (ind2 === ind) elem2.status = "loading"
											return elem2
										})
										this.setState({ data: arrUpdate })
										// Do the convert for this single cutting plan
										let reftissuPair = matchingEntry.join(":")
										axios.post(`/api/cuttingPlan/${elem.cuttingPlan}/convertReftissu/${reftissuPair}`)
											.then(res => {
												let arrUpdate = this.state.data.map((elem2, ind2) => {
													if (ind2 === ind) elem2.status = "good"
													return elem2
												})
												this.setState({ data: arrUpdate })
											})
											.catch(err => {
												let arrUpdate = this.state.data.map((elem2, ind2) => {
													if (ind2 === ind) elem2.status = "bad"
													return elem2
												})
												this.setState({ data: arrUpdate })
											})
									}}>
									Convert
								</button>
							</td>
						</tr>
					})}
				</tbody>
			</table>
		</div>
	}

	render() {
		return (
			<div>
				<h1 className='text-center'>Changement Reftissu</h1>
				<div className='row entityform-field' style={{ margin: 15 }}>
					<div className='row entityform-field col-12'>
						<label className='col-2 col-form-label text-right'>projet :</label>
						{this.state.optionsList.projet && <Select id={"projet"} name={"projet"} classNamePrefix="rs"
							placeholder={"Projet..."} className='col-8 p-0'
							isClearable={true}
							value={(this.state.optionsList.projet && this.state.optionsList.projet.length > 0 && this.state.modalObj.projet)
								? { label: this.state.modalObj.projet, value: this.state.modalObj.projet, item: this.state.modalObj.projet }
								: null
							}
							options={this.state.optionsList.projet}
							onChange={(option) => {
								this.setState({ modalObj: { ...this.state.modalObj, projet: (option ? option.value : null) } })
							}}
						/>}
					</div>
					{/* <div className='row entityform-field col-6'>
						<label className='col-4 col-form-label text-right'>Ref New :</label>
						<input className='form-control col-8 entityform-input' value={this.state.modalObj.refNew || ""} style={{height: 30}}
							onChange={(event) => {
								this.setState({
									modalObj: { ...this.state.modalObj, refNew: event.target.value },
								})
							}}
						/>
					</div> */}

					<div className='row entityform-field col-12'>
						<label className='col-2 col-form-label text-right'></label>
						<div className='col-8 p-0'>{this.renderTissuTable()}</div>
					</div>
					{this.renderData()}

				</div>
				{(this.state.error && Object.keys(this.state.error).length !== 0)
					&& !(this.state.error.subDemandes && this.state.error.subDemandes.length == 0) && <div className="alert alert-danger alert-error text-center m-4" role="alert">
						<ul>
							<button type='btn' className="btn btn-toclose" onClick={() => { this.setState({ error: null }) }}>
								<FontAwesomeIcon icon={faTimes} size="2x" />
							</button>
							{this.renderErrorsAlert(this.state.error)}
						</ul>
					</div>}
				{this.renderReftissuModal()}
			</div>
		)
	}
}
