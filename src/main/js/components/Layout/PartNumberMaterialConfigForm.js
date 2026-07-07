import { faArrowLeft, faArrowRight, faFloppyDisk, faPlus, faTrashAlt, faUpload } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import axios from 'axios'
import React, { Component } from 'react'
import { connect } from 'react-redux'
import { jsonStyle, metadata, optionsMatelassageEndroit } from '../../metadata'
import Select from "react-select";
import Switch from "react-switch";

class PartNumberMaterialConfigForm extends Component {

	constructor() {
		super()
		this.state = {
			modalObj: { vitesse: 300, tauxScrap: 3 },
			optionsList: {},
			machineRow: null,
			marginRow: null,
			reftissuCopy: "",
			arrHistory: null,
			savingToCms: false
		}
		this.inputArr = []

	}

	getRoles = () => {
		const roles = (this.props.security && this.props.security.user && this.props.security.user.roles) || []
		return roles.map(r => r.authority)
	}

	isCadFoamOnly = () => {
		const auths = this.getRoles()
		return auths.includes("ROLE_CAD_FOAM") && !auths.includes("ROLE_CAD")
	}

	isWriteBlocked = () => {
		const { entityId } = this.props
		return entityId != null && this.isCadFoamOnly() && this.state.modalObj.fipDev !== true
	}

	componentDidMount() {
		const { entityId } = this.props
		if (entityId) {
			axios.get(`/api/partNumberMaterialConfig/${entityId}`)
				.then(res => {
					this.setState({ modalObj: res.data })
				})
		}
		axios.get(`/api/machineType/list`)
			.then((res) => {
				this.setState({ optionsList: { ...this.state.optionsList, machineType: res.data.map(elem => { return { label: elem.name, value: elem.name } }) } })
			})
		this.searchObjectOptions("partNumberMaterialConfig")


	}

	searchObjectOptions(entity) {
		metadata[entity].fields
			.filter((field) => (field.type === 'object' || field.type === 'list') && field.hideForm !== true)
			.map((field) => {
				if (field.type === 'object') {
					let objectEntity = field.formObject ? field.formObject : field.name
					let objectDisplayEntity = field.formDisplayProperty ? field.formDisplayProperty : "nom"
					axios.get(`/api/${objectEntity}/list`)
						.then((res) => {
							this.setState({
								optionsList: {
									...this.state.optionsList,
									[objectEntity]: res.data.map((elem) => ({ value: elem[objectDisplayEntity], label: elem[objectDisplayEntity], item: elem }))
								}
							})
						})
				}
				if (field.type === 'list') {

					let formDisplayProperty = field.formDisplayProperty || "nom"
					let link = field.link ? field.link : `/api/${field.entityName}/list`
					axios.get(link)
						.then((res) => {
							if (typeof res.data.pageable === "object") {
								this.setState({
									optionsList: {
										...this.state.optionsList,
										[field.entityName]: res.data.content.map((elem) => ({ value: elem[formDisplayProperty], label: elem[formDisplayProperty], item: elem }))
									}
								})
							} else {
								this.setState({
									optionsList: {
										...this.state.optionsList,
										[field.entityName]: res.data.map((elem) => ({ value: elem[formDisplayProperty], label: elem[formDisplayProperty], item: elem }))
									}
								})
							}

						})

				}

			})
	}

	renderField(field, ind) {
		const { entityId } = this.props
		switch (field.type) {
			case "text":
				return <input className='form-control col-8 entityform-input' value={this.state.modalObj[field.name] || ""} disabled={entityId != null && ind === 0}
					ref={input => this.inputArr[ind] = input}
					onChange={(event) => {
						if (field.reg == null || field.reg.test(event.target.value)) {
							this.setState({ modalObj: { ...this.state.modalObj, [field.name]: event.target.value } })
						}
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							this.inputArr[ind + 1].focus()
						}
					}}
				/>
			case "option":
				return <Select classNamePrefix="rs" className='col-8 p-0'
					placeholder={field.displayName}
					isClearable={false}
					value={field.optionsList.find(option => option.value === this.state.modalObj[field.name])}
					options={field.optionsList}
					onChange={(option) => {
						this.setState({ modalObj: { ...this.state.modalObj, [field.name]: option.value } })
					}}
				/>
			case "object":
				let objectEntity = field.formObject ? field.formObject : field.name
				// let entityId = metadata[objectEntity].fields[0].name
				return (
					<Select id={field.name} name={field.name} classNamePrefix="rs"
						placeholder={field.displayName} className='col-8 p-0'
						isClearable={!field.isRequired}
						value={(this.state.optionsList[objectEntity] && this.state.optionsList[objectEntity].length > 0 && this.state.modalObj[field.name])
							? { label: this.state.modalObj[field.name][field.formDisplayProperty], value: this.state.modalObj[field.name][field.formDisplayProperty], item: this.state.modalObj[field.name] }
							: null
						}
						options={this.state.optionsList[objectEntity]}
						onChange={(option) => {
							this.setState({ modalObj: { ...this.state.modalObj, [field.name]: (option ? option.item : null) } })
						}}
					/>
				)
			default: return <div></div>
		}
	}

	renderMachineForm = () => {
		let arrMachine = this.state.modalObj.reftissuMachines ? this.state.modalObj.reftissuMachines : [{ defaultValue: true }]
		let arrPlie = null
		if (this.state.machineRow !== null && arrMachine[this.state.machineRow]) {
			arrPlie = arrMachine[this.state.machineRow].pliesConfig ? arrMachine[this.state.machineRow].pliesConfig.split("|").map(e => e.split(";")) : [["1", ""]]
		}

		return <div className='row mb-2'>
			<div className=' col-8 px-2' >
				<table className='table table-bordered m-0'>
					<thead>
						<tr>
							<th className='table-elem-sm' style={{ width: 150 }}>Type Machine</th>
							<th className='table-elem-sm'>Max Plie</th>
							<th className='table-elem-sm'>Max Plie Drill</th>
							<th className='table-elem-sm'>Max Drill</th>
							<th className='table-elem-sm'>Defaut</th>
							<th className='table-elem-sm'></th>
						</tr>
					</thead>
					<tbody>
						{arrMachine.map((elem, ind) => {
							return <tr key={"arrMachine-" + ind}>
								<td className='table-elem-sm'>
									<Select classNamePrefix="rs"
										placeholder={"Machine Type"}
										isClearable={false}
										value={(this.state.optionsList.machineType && this.state.optionsList.machineType.length > 0 && arrMachine[ind].machineType)
											? { label: arrMachine[ind].machineType, value: arrMachine[ind].machineType }
											: null
										}
										options={this.state.optionsList.machineType}
										onChange={(option) => {
											arrMachine[ind].machineType = option ? option.value : null
											this.setState({ modalObj: { ...this.state.modalObj, reftissuMachines: [...arrMachine] } })
										}}
									/>
								</td>
								<td className='table-elem-sm'>
									<input value={elem.maxPlie || ""} onChange={(e) => {
										if (/^\d*$/.test(e.target.value)) {
											arrMachine[ind].maxPlie = e.target.value != "" ? parseInt(e.target.value) : null
											this.setState({ modalObj: { ...this.state.modalObj, reftissuMachines: [...arrMachine] } })
										}
									}} />
								</td>
								<td className='table-elem-sm'>
									<input value={elem.maxPlieDrill || ""} onChange={(e) => {
										if (/^\d*$/.test(e.target.value)) {
											arrMachine[ind].maxPlieDrill = e.target.value != "" ? parseInt(e.target.value) : null
											this.setState({ modalObj: { ...this.state.modalObj, reftissuMachines: [...arrMachine] } })
										}
									}} />
								</td>
								<td className='table-elem-sm'>
									<input value={elem.maxDrill || ""} onChange={(e) => {
										if (/^\d*$/.test(e.target.value)) {
											arrMachine[ind].maxDrill = e.target.value != "" ? parseInt(e.target.value) : null
											this.setState({ modalObj: { ...this.state.modalObj, reftissuMachines: [...arrMachine] } })
										}
									}} />
								</td>
								<td className='table-elem-sm'>
									<input type='radio' name="machine-radio" value={ind} checked={arrMachine[ind].defaultValue === true} onChange={(e) => {
										console.log({ e: e.target.value, ind })
										let arr = [...arrMachine]
										for (let i = 0; i < arr.length; i++) {
											arr[i].defaultValue = false
										}
										arr[e.target.value].defaultValue = true
										this.setState({ modalObj: { ...this.state.modalObj, reftissuMachines: [...arr] } })
									}} />
								</td>
								<td className='table-elem-sm d-flex justify-content-center'>
									<button className='btn btn-danger' onClick={() => {
										arrMachine.splice(ind, 1);
										this.setState({ modalObj: { ...this.state.modalObj, reftissuMachines: [...arrMachine] } })
									}}><FontAwesomeIcon icon={faTrashAlt} /></button>
									<button className='btn btn-primary ml-2' onClick={() => {
										this.setState({ machineRow: ind })
									}}><FontAwesomeIcon icon={faArrowRight} /></button>
								</td>
							</tr>
						})}

					</tbody>
				</table>
				<div className='d-flex'>
					<button className="btn btn-outline-danger" onClick={() => {
						arrMachine.push({})
						this.setState({ modalObj: { ...this.state.modalObj, reftissuMachines: [...arrMachine] } })
					}}><FontAwesomeIcon icon={faPlus} /></button>
				</div>
			</div>
			{arrPlie && <div className='col-4 px-2'>
				<table className='table table-bordered m-0'>
					<thead>
						<tr>
							<th className='table-elem-sm' colSpan={4}>{arrMachine[this.state.machineRow].machineType}</th>
						</tr>
						<tr>
							<th className='table-elem-sm'>Min Plie</th>
							<th className='table-elem-sm'>Config</th>
							<th className='table-elem-sm' style={{ width: 150 }}>Sens</th>
							<th className='table-elem-sm'></th>
						</tr>
					</thead>
					<tbody>
						{arrPlie.map((plieElem, plieInd) => <tr key={"arrPlie-" + plieInd}>
							<td className='table-elem-sm'><input value={plieElem[0] || ""} onChange={(event) => {
								if (/^\d*$/.test(event.target.value)) {
									arrPlie[plieInd][0] = event.target.value != "" ? parseInt(event.target.value) : null
									arrMachine[this.state.machineRow].pliesConfig = arrPlie.map(e => e.join(";")).join("|")
									this.setState({ modalObj: { ...this.state.modalObj, reftissuMachines: [...arrMachine] } })
								}
							}} /></td>
							<td className='table-elem-sm'><input value={plieElem[1] || ""} onChange={(event) => {
								arrPlie[plieInd][1] = event.target.value
								arrMachine[this.state.machineRow].pliesConfig = arrPlie.map(e => e.join(";")).join("|")
								this.setState({ modalObj: { ...this.state.modalObj, reftissuMachines: [...arrMachine] } })
							}} /></td>
							<td className='table-elem-sm'>
								<button className='btn btn-danger' onClick={() => {
									arrPlie.splice(plieInd, 1);
									arrMachine[this.state.machineRow].pliesConfig = arrPlie.map(e => e.join(";")).join("|")
									this.setState({ modalObj: { ...this.state.modalObj, reftissuMachines: [...arrMachine] } })
								}}><FontAwesomeIcon icon={faTrashAlt} /></button>
							</td>
						</tr>)}
					</tbody>
				</table>
				<div className='d-flex'>
					<button className="btn btn-outline-danger" onClick={() => {
						arrPlie.push(["", ""])
						arrMachine[this.state.machineRow].pliesConfig = arrPlie.map(e => e.join(";")).join("|")
						this.setState({ modalObj: { ...this.state.modalObj, reftissuMachines: [...arrMachine] } })
					}}><FontAwesomeIcon icon={faPlus} /></button>
				</div>
			</div>}

		</div>
	}

	renderCategoryForm = () => {
		let arrCategory = this.state.modalObj.reftissuCategories ? this.state.modalObj.reftissuCategories : [{ defaultValue: true }]
		return <div className='row mb-2 px-2' >
			<table className='table table-bordered m-0'>
				<thead>
					<tr>
						<th className='table-elem-sm' style={{ width: 150 }}>Category</th>
						<th className='table-elem-sm'>Description</th>
						<th className='table-elem-sm'>borneMin</th>
						<th className='table-elem-sm'>borneMax</th>
						<th className='table-elem-sm'>Defaut</th>
						<th className='table-elem-sm'></th>
					</tr>
				</thead>
				<tbody>
					{arrCategory.map((elem, ind) => {
						return <tr key={"arrCategory-" + ind}>
							<td className='table-elem-sm'>
								<input value={elem.category} onChange={(e) => {
									arrCategory[ind].category = e.target.value.toUpperCase()
									if (e.target.value === "A") {
										arrCategory[ind].description = 'Contactuel'
									} else if (e.target.value === "B") {
										arrCategory[ind].description = 'Physique'
									} else {
										arrCategory[ind].description = ''
									}
									this.setState({ modalObj: { ...this.state.modalObj, reftissuCategories: [...arrCategory] } })

								}} />
							</td>
							<td className='table-elem-sm'>
								<input value={elem.description} onChange={(e) => {
									arrCategory[ind].description = e.target.value
									this.setState({ modalObj: { ...this.state.modalObj, reftissuCategories: [...arrCategory] } })

								}} />
							</td>
							<td className='table-elem-sm'>
								<input value={elem.borneMin || ""} onChange={(e) => {
									if (/^\d*\.?\d*$/.test(e.target.value)) {
										arrCategory[ind].borneMin = e.target.value === "" ? null : e.target.value
										this.setState({ modalObj: { ...this.state.modalObj, reftissuCategories: [...arrCategory] } })
									}
								}} />
							</td>
							<td className='table-elem-sm'>
								<input value={elem.borneMax || ""} onChange={(e) => {
									if (/^\d*\.?\d*$/.test(e.target.value)) {
										arrCategory[ind].borneMax = e.target.value === "" ? null : e.target.value
										this.setState({ modalObj: { ...this.state.modalObj, reftissuCategories: [...arrCategory] } })
									}
								}} />
							</td>
							<td className='table-elem-sm'>
								<input type='radio' name="category-defaultValue" value={ind} checked={arrCategory[ind].defaultValue === true} onChange={(e) => {
									let arr = [...arrCategory]
									for (let i = 0; i < arr.length; i++) {
										arr[i].defaultValue = false
									}
									arr[e.target.value].defaultValue = true
									this.setState({ modalObj: { ...this.state.modalObj, reftissuCategories: [...arr] } })
								}} />
							</td>
							<td className='table-elem-sm'>
								<button className='btn btn-danger' onClick={() => {
									arrCategory.splice(ind, 1);
									this.setState({ modalObj: { ...this.state.modalObj, reftissuCategories: [...arrCategory] } })
								}}><FontAwesomeIcon icon={faTrashAlt} /></button>
							</td>
						</tr>
					})}

				</tbody>
			</table>
			<div className='d-flex'>
				<button className="btn btn-outline-danger" onClick={() => {
					if (arrCategory.length == 0) {
						arrCategory.push({ defaultValue: true })
					} else {
						arrCategory.push({})
					}

					this.setState({ modalObj: { ...this.state.modalObj, reftissuCategories: [...arrCategory] } })
				}}><FontAwesomeIcon icon={faPlus} /></button>
			</div>
		</div>
	}
	renderSeuil() {
		let arrMargin = this.state.modalObj.reftissuMargins ? this.state.modalObj.reftissuMargins : [{}]
		let arrPlie = null
		if (this.state.marginRow !== null) {
			arrPlie = arrMargin[this.state.marginRow].pliesConfig ? arrMargin[this.state.marginRow].pliesConfig.split("|").map(e => e.split(";")) : [["1", ""]]
		}

		return <div className='row mb-2'>
			<div className=' col-8 px-2' >
				<table className='table table-bordered m-0'>
					<thead>
						<tr>
							<th className='table-elem-sm' style={{ width: 150 }}>Type Machine</th>
							<th className='table-elem-sm'>Longueur Min</th>
							<th className='table-elem-sm'>Longueur Max</th>
							<th className='table-elem-sm'></th>
						</tr>
					</thead>
					<tbody>
						{arrMargin.map((elem, ind) => {
							return <tr key={"arrMargin-" + ind}>
								<td className='table-elem-sm'>
									<Select classNamePrefix="rs"
										placeholder={"Machine"}
										isClearable={true}
										value={(this.state.optionsList.machineType && this.state.optionsList.machineType.length > 0 && arrMargin[ind].machine)
											? { label: arrMargin[ind].machine, value: arrMargin[ind].machine }
											: null
										}
										options={this.state.optionsList.machineType}
										onChange={(option) => {
											arrMargin[ind].machine = option ? option.value : null
											this.setState({ modalObj: { ...this.state.modalObj, reftissuMargins: [...arrMargin] } })
										}}
									/>
								</td>
								<td className='table-elem-sm'>
									<input value={elem.longueurMin || ""} onChange={(e) => {
										console.log(e.target.value)
										if (/^\d*\.?\d*$/.test(e.target.value)) {
											arrMargin[ind].longueurMin = e.target.value
											this.setState({ modalObj: { ...this.state.modalObj, reftissuMargins: [...arrMargin] } })
										}
									}} />
								</td>
								<td className='table-elem-sm'>
									<input value={elem.longueurMax || ""} onChange={(e) => {
										if (/^\d*\.?\d*$/.test(e.target.value)) {
											arrMargin[ind].longueurMax = e.target.value
											this.setState({ modalObj: { ...this.state.modalObj, reftissuMargins: [...arrMargin] } })
										}
									}} />
								</td>
								<td className='table-elem-sm d-flex justify-content-center'>
									<button className='btn btn-danger ' onClick={() => {
										arrMargin.splice(ind, 1);
										this.setState({ modalObj: { ...this.state.modalObj, reftissuMargins: [...arrMargin] } })
									}}><FontAwesomeIcon icon={faTrashAlt} /></button>
									<button className='btn btn-primary  ml-2' onClick={() => {
										this.setState({ marginRow: ind })
									}}><FontAwesomeIcon icon={faArrowRight} /></button>
								</td>
							</tr>
						})}

					</tbody>
				</table>
				<div className='d-flex'>
					<button className="btn btn-outline-danger" onClick={() => {
						arrMargin.push({})
						this.setState({ modalObj: { ...this.state.modalObj, reftissuMargins: [...arrMargin] } })
					}}><FontAwesomeIcon icon={faPlus} /></button>
				</div>
			</div>
			{arrPlie && <div className='col-4 px-2'>
				<table className='table table-bordered m-0'>
					<thead>
						<tr>
							<th className='table-elem-sm' colSpan={4}>{arrMargin[this.state.marginRow].longueurMin} <FontAwesomeIcon icon={faArrowRight} /> {arrMargin[this.state.marginRow].longueurMax}</th>
						</tr>
						<tr>
							<th className='table-elem-sm'>Min Plie</th>
							<th className='table-elem-sm'>Marge</th>
							<th className='table-elem-sm'></th>
						</tr>
					</thead>
					<tbody>
						{arrPlie.map((plieElem, plieInd) => <tr key={"arrPlie-" + plieInd}>
							<td className='table-elem-sm'><input value={plieElem[0] || ""} onChange={(event) => {
								if (/^\d*$/.test(event.target.value)) {
									arrPlie[plieInd][0] = event.target.value != "" ? parseInt(event.target.value) : null
									arrMargin[this.state.marginRow].pliesConfig = arrPlie.map(e => e.join(";")).join("|")
									this.setState({ modalObj: { ...this.state.modalObj, reftissuMargins: [...arrMargin] } })
								}
							}} /></td>
							<td className='table-elem-sm'><input value={plieElem[1] || ""} onChange={(event) => {
								if (/^\d*\.?\d*$/.test(event.target.value)) {
									arrPlie[plieInd][1] = event.target.value
									arrMargin[this.state.marginRow].pliesConfig = arrPlie.map(e => e.join(";")).join("|")
									this.setState({ modalObj: { ...this.state.modalObj, reftissuMargins: [...arrMargin] } })
								}
							}} /></td>
							<td className='table-elem-sm'>
								<button className='btn btn-danger' onClick={() => {
									arrPlie.splice(plieInd, 1);
									arrMargin[this.state.marginRow].pliesConfig = arrPlie.map(e => e.join(";")).join("|")
									this.setState({ modalObj: { ...this.state.modalObj, reftissuMargins: [...arrMargin] } })
								}}><FontAwesomeIcon icon={faTrashAlt} /></button>
							</td>
						</tr>)}
					</tbody>
				</table>
				<div className='d-flex'>
					<button className="btn btn-outline-danger" onClick={() => {
						arrPlie.push(["", "", ""])
						arrMargin[this.state.marginRow].pliesConfig = arrPlie.map(e => e.join(";")).join("|")
						this.setState({ modalObj: { ...this.state.modalObj, reftissuMargins: [...arrMargin] } })
					}}><FontAwesomeIcon icon={faPlus} /></button>
				</div>
			</div>}

		</div>
	}

	renderInitForm = () => {
		const { entityId } = this.props
		return <div className='row entityform-field'>
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>Part Number Material :</label>
				<input className='form-control col-8 entityform-input' value={this.state.modalObj.partNumberMaterial || ""} disabled={entityId != null}
					ref={input => this.inputArr[0] = input}
					onChange={(event) => {
						if (/^[A-Za-z0-9-\u0020]*$/.test(event.target.value)) {
							this.setState({ modalObj: { ...this.state.modalObj, partNumberMaterial: event.target.value } })
						}
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							axios.get(`/api/partNumberMaterialConfig/${e.target.value}`)
								.then(res => {
									window.location.pathname = `/partNumberMaterialConfig/${e.target.value}`
								})
								.catch(err => {
									axios.get(`/api/partNumberBoom/pnMaterial/${e.target.value}`)
										.then(res => {
											this.setState({ modalObj: { ...this.state.modalObj, description: res.data.partNumberMaterialDescription }, })
											this.inputArr[2].focus()
										})
										.catch(err => {
											this.inputArr[1].focus()
										})
								})

						}
					}}
				/>
			</div>
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>description :</label>
				<input className='form-control col-8 entityform-input' value={this.state.modalObj.description || ""}
					ref={input => this.inputArr[1] = input}
					onChange={(event) => {
						this.setState({ modalObj: { ...this.state.modalObj, description: event.target.value } })
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							this.inputArr[2].focus()
						}
					}}
				/>
			</div>

			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>vitesse :</label>
				<input className='form-control col-8 entityform-input' value={this.state.modalObj.vitesse || ""}
					ref={input => this.inputArr[2] = input}
					onChange={(event) => {
						if (/^\d*\.?\d*$/.test(event.target.value)) {
							this.setState({ modalObj: { ...this.state.modalObj, vitesse: event.target.value } })
						}
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							this.inputArr[3].focus()
						}
					}}
				/>
			</div>
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>rotation :</label>
				<input className='form-control col-8 entityform-input' value={this.state.modalObj.rotation || ""}
					ref={input => this.inputArr[3] = input}
					onChange={(event) => {
						// if (/^\d*\.?\d*$/.test(event.target.value)) {
							this.setState({ modalObj: { ...this.state.modalObj, rotation: event.target.value } })
						// }
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							this.inputArr[4].focus()
						}
					}}
				/>
			</div>

			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>plaque :</label>
				<input className='form-control col-8 entityform-input' value={this.state.modalObj.plaque || ""}
					ref={input => this.inputArr[4] = input}
					onChange={(event) => {
						if (/^\d*\.?\d*$/.test(event.target.value)) {
							this.setState({ modalObj: { ...this.state.modalObj, plaque: event.target.value } })
						}
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							this.inputArr[5].focus()
						}
					}}
				/>
			</div>
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>tauxScrap :</label>
				<input className='form-control col-8 entityform-input' value={this.state.modalObj.tauxScrap || ""}
					ref={input => this.inputArr[5] = input}
					onChange={(event) => {
						if (/^\d*\.?\d*$/.test(event.target.value)) {
							this.setState({ modalObj: { ...this.state.modalObj, tauxScrap: event.target.value } })
						}
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							this.inputArr[6].focus()
						}
					}}
				/>
			</div>

			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>Matelassage Endroit :</label>
				<Select classNamePrefix="rs" className='col-8 p-0'
					placeholder={"Matelassage Endroit"} ref={input => this.inputArr[6] = input}
					isClearable={false}
					value={this.state.modalObj.matelassageEndroit ? { label: this.state.modalObj.matelassageEndroit, value: this.state.modalObj.matelassageEndroit } : null}
					options={optionsMatelassageEndroit}
					onChange={(option) => {
						this.setState({ modalObj: { ...this.state.modalObj, matelassageEndroit: option.value } })
						this.inputArr[7].focus()
					}}
				/>
			</div>
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>commentaire :</label>
				<input className='form-control col-8 entityform-input' value={this.state.modalObj.commentaire || ""}
					ref={input => this.inputArr[7] = input}
					onChange={(event) => {
						this.setState({ modalObj: { ...this.state.modalObj, commentaire: event.target.value } })
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							this.inputArr[8].focus()
						}
					}}
				/>
			</div>

			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>Marge Laize Min (cm) :</label>
				<input className='form-control col-8 entityform-input' value={this.state.modalObj.margeLaizeMin || ""}
					ref={input => this.inputArr[8] = input}
					onChange={(event) => {
						if (/^\d*\.?\d*$/.test(event.target.value)) {
							this.setState({ modalObj: { ...this.state.modalObj, margeLaizeMin: event.target.value } })
						}
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							this.inputArr[9].focus()
						}
					}}
				/>
			</div>
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>Marge Laize Max (cm) :</label>
				<input className='form-control col-8 entityform-input' value={this.state.modalObj.margeLaizeMax || ""}
					ref={input => this.inputArr[9] = input}
					onChange={(event) => {
						if (/^\d*\.?\d*$/.test(event.target.value)) {
							this.setState({ modalObj: { ...this.state.modalObj, margeLaizeMax: event.target.value } })
						}
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							this.inputArr[12].focus()
						}
					}}
				/>
			</div>

			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>Validated 0BF :</label>
				<div className='col-8 d-flex align-items-center'>
					<Switch checked={this.state.modalObj.validated0BF === true}
						ref={input => this.inputArr[10] = input}
						onChange={(checked) => {
							this.setState({ modalObj: { ...this.state.modalObj, validated0BF: checked } })
						}}
						onColor="#28a745" offColor="#dc3545"
						height={22} width={44} handleDiameter={18}
					/>
				</div>
			</div>
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>Validated IP6 :</label>
				<div className='col-8 d-flex align-items-center'>
					<Switch checked={this.state.modalObj.validatedIP6 === true}
						ref={input => this.inputArr[11] = input}
						onChange={(checked) => {
							this.setState({ modalObj: { ...this.state.modalObj, validatedIP6: checked } })
						}}
						onColor="#28a745" offColor="#dc3545"
						height={22} width={44} handleDiameter={18}
					/>
				</div>
			</div>
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>FIP dev :</label>
				<div className='col-8 d-flex align-items-center'>
					<Switch checked={this.state.modalObj.fipDev === true}
						disabled={this.isCadFoamOnly()}
						onChange={(checked) => {
							if (this.isCadFoamOnly()) return
							this.setState({ modalObj: { ...this.state.modalObj, fipDev: checked } })
						}}
						onColor="#28a745" offColor="#dc3545"
						height={22} width={44} handleDiameter={18}
					/>
				</div>
			</div>

			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>Buffer 1 IP6 :</label>
				<input className='form-control col-8 entityform-input' value={this.state.modalObj.buffer1IP6 || ""}
					ref={input => this.inputArr[12] = input}
					onChange={(event) => {
						this.setState({ modalObj: { ...this.state.modalObj, buffer1IP6: event.target.value } })
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							this.inputArr[13].focus()
						}
					}}
				/>
			</div>
			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>Buffer 2 IP6 :</label>
				<input className='form-control col-8 entityform-input' value={this.state.modalObj.buffer2IP6 || ""}
					ref={input => this.inputArr[13] = input}
					onChange={(event) => {
						this.setState({ modalObj: { ...this.state.modalObj, buffer2IP6: event.target.value } })
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							this.inputArr[14].focus()
						}
					}}
				/>
			</div>

			<div className='row entityform-field col-6'>
				<label className='col-4 col-form-label text-right'>Poids/m² (kg) :</label>
				<input className='form-control col-8 entityform-input' value={this.state.modalObj.weightUnit || ""}
					ref={input => this.inputArr[14] = input}
					onChange={(event) => {
						if (/^\d*\.?\d*$/.test(event.target.value)) {
							this.setState({ modalObj: { ...this.state.modalObj, weightUnit: event.target.value } })
						}
					}}
				/>
			</div>
		</div>
	}

	renderHistorique = () => {
		return <div className="">
			<button className='btn btn-outline-danger' onClick={() => {
				axios.get(`/api/partNumberMaterialConfigHistory/material/${this.state.modalObj.partNumberMaterial}`)
					.then(res => {
						this.setState({ arrHistory: res.data })
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
							</tr>
						</thead>
						<tbody>
							{this.state.arrHistory.map((elem, ind) => {
								return <tr key={"row-" + ind} className="clickable-element" onClick={() => { this.setState({ selectedHistory: elem }) }}>
									<td>{elem.updatedBy ? elem.updatedBy.matricule : ""}</td>
									<td>{elem.createdAt}</td>
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

	render() {
		const entity = "partNumberMaterialConfig"
		const { entityId } = this.props
		return (
			<div>
				<div className='entityform-header'>
					<h1 className='entityform-title'>{metadata[entity].displayName} {entityId}</h1>
					<div className='entityform-buttons'>
						{this.isWriteBlocked() && <div className='alert alert-warning ml-2 mb-2'>
							FIP dev non activé pour cette référence : modification interdite pour les utilisateurs CAD FOAM.
						</div>}
						<button type="button" className='btn btn-danger ml-2' ref={btn => this.inputArr[metadata[entity].fields.filter(field => field.hideForm !== true).length] = btn}
							disabled={this.isWriteBlocked()}
							onClick={() => {
								this.setState({ loading: false })
								if (entityId) {
									axios.post(`/api/${entity}`, { ...this.state.modalObj, changes: JSON.stringify(this.state.modalObj, null, 2) })
										.then(res => {
											this.props.goBack()
										})
										.catch(err => {
											alert('Erreur: ' + (err.response?.data || err.message))
										})
								} else {
									axios.get(`/api/partNumberMaterialConfig/${this.state.modalObj.partNumberMaterial}`)
										.then(res => {
											window.location.pathname = `/partNumberMaterialConfig/${this.state.modalObj.partNumberMaterial}`
										})
										.catch(err => {
											axios.post(`/api/${entity}`, { ...this.state.modalObj, changes: JSON.stringify(this.state.modalObj, null, 2) })
												.then(res => {
													this.props.goBack()
												})
												.catch(err2 => {
													alert('Erreur: ' + (err2.response?.data || err2.message))
												})
										})
								}

							}}
						>
							<FontAwesomeIcon icon={faFloppyDisk} /> Enregistrer
						</button>
						{entityId && <button type="button" className='btn btn-warning ml-2' disabled={this.state.savingToCms || this.isWriteBlocked()}
							onClick={async () => {
								this.setState({ savingToCms: true })
								try {
									// Always save first before sending to CMS
									await axios.post(`/api/${entity}`, { ...this.state.modalObj, changes: JSON.stringify(this.state.modalObj, null, 2) })
									// Then send to CMS
									await axios.post(`/api/partNumberMaterialConfig/saveToCms/${entityId}`)
									this.setState({ savingToCms: false })
									alert('Sauvegardé puis envoyé vers CMS avec succès')
								} catch (err) {
									this.setState({ savingToCms: false })
									alert('Erreur: ' + (err.response?.data || err.message))
								}
							}}
						>
							<FontAwesomeIcon icon={faUpload} /> {this.state.savingToCms ? 'Sauvegarde...' : 'Sauvegarder vers CMS'}
						</button>}
						{!entityId && <button type="button" className='btn btn-warning ml-2' disabled={this.state.savingToCms}
							onClick={async () => {
								this.setState({ savingToCms: true })
								try {
									// New creation: save first to get the new ID
									let saveRes = await axios.post(`/api/${entity}`, { ...this.state.modalObj, changes: JSON.stringify(this.state.modalObj, null, 2) })
									let newId = saveRes.data.partNumberMaterial || saveRes.data.id
									// Then send to CMS using the new ID
									await axios.post(`/api/partNumberMaterialConfig/saveToCms/${newId}`)
									this.setState({ savingToCms: false })
									// alert('Sauvegardé puis envoyé vers CMS avec succès')
									// window.location.pathname = `/partNumberMaterialConfig/${newId}`
									this.props.goBack()
								} catch (err) {
									this.setState({ savingToCms: false })
									alert('Erreur: ' + (err.response?.data || err.message))
								}
							}}
						>
							<FontAwesomeIcon icon={faUpload} /> {this.state.savingToCms ? 'Sauvegarde...' : 'Sauvegarder vers CMS'}
						</button>}
						<button className='btn btn-link' onClick={() => { this.props.goBack() }}>Annuler</button>
					</div>
				</div>
				<div className='entityform-container'>
					<div>
						<button className='btn btn-link' onClick={() => { this.props.goBack() }}>Retour au tableau</button>
					</div>
					<h2>1. Information</h2>
					{this.renderInitForm()}
					<div className='row entityform-field'>
						{metadata[entity].fields.filter(field => field.hideForm !== true).map((field, ind) => {
							return
						})}

						<span style={{ marginLeft: 20 }}>Appliquer la même config que : </span><input
							value={this.state.reftissuCopy || ""}
							onChange={(e) => {
								this.setState({ reftissuCopy: e.target.value })
							}}
							onKeyUp={(e) => {
								if (e.key === "Enter") {
									axios.get(`/api/partNumberMaterialConfig/${this.state.reftissuCopy}`)
										.then(res => {
											this.setState({
												modalObj: {
													...res.data,
													partNumberMaterial: this.state.modalObj.partNumberMaterial,
													description: this.state.modalObj.description
												}
											})
										})
								}
							}}
						/>
					</div>
					<h2>2. Machines</h2>
					{this.renderMachineForm()}
					<h2>3. Catégories</h2>
					{this.renderCategoryForm()}
					<h2>4. Seuil</h2>
					{this.renderSeuil()}
					<h2>5. Historique</h2>
					{this.renderHistorique()}
				</div>

			</div>
		)
	}
}

const mapStateToProps = state => ({
	security: state.security
})

export default connect(mapStateToProps, {})(PartNumberMaterialConfigForm)
