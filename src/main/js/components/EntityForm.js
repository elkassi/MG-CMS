import { faFloppyDisk } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import axios from 'axios'
import React, { Component } from 'react'
import { metadata } from '../metadata'
import "../styles/EntityForm.scss"
import Select from "react-select";
import Switch from "react-switch";

export default class EntityForm extends Component {

	constructor() {
		super()
		this.state = {
			modalObj: {},
			optionsList: {}
		}
		this.inputArr = []
	}

	componentDidMount() {
		const { entity, entityId } = this.props
		if (entity && entityId) {
			axios.get(`/api/${entity}/${entityId}`)
				.then(res => {
					// we need to change all null properties that equals to null to "NULL"
					let modalObj = res.data
					Object.keys(modalObj).map((key) => {
						if (modalObj[key] == null) {
							modalObj[key] = "NULL"
						}
					})
					this.setState({ modalObj })
				})
		}
		this.searchObjectOptions(entity)
	}

	searchObjectOptions(entity) {
		metadata[entity].fields
			.filter((field) => (field.type === 'object' || field.type === 'list' || (field.type === 'option' && typeof field.optionsList === 'string')) && field.hideForm !== true)
			.map((field) => {
				if (field.type === 'object') {
					
					let objectEntity = field.formObject ? field.formObject : field.name
					let objectDisplayEntity = field.formDisplayProperty ? field.formDisplayProperty : "nom"
					let url = ""
					if (field.optionUrl) {
						url = field.optionUrl
					} else {
						url = `/api/${objectEntity}/list`
					}
					axios.get(url)
						.then((res) => {
							this.setState({
								optionsList: {
									...this.state.optionsList,
									[objectEntity]: res.data.map((elem) => ({ value: elem[objectDisplayEntity], label: elem[objectDisplayEntity], item: elem }))
								}
							})
						})
				}
				if (field.type === 'option' && typeof field.optionsList === 'string') {
					let entityName = field.optionsList
					let displayProperty = field.formDisplayProperty || "code"
					axios.get(`/api/${entityName}/list`)
						.then((res) => {
							let data = res.data.content ? res.data.content : res.data
							this.setState({
								optionsList: {
									...this.state.optionsList,
									[field.name]: data.map((elem) => ({ value: elem[displayProperty], label: elem[displayProperty] }))
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
		const { entity, entityId } = this.props
		switch (field.type) {
			case "text":
				return <input className='form-control col-8 entityform-input' value={this.state.modalObj[field.name]} disabled={field.disabled === true}
				style={this.state.modalObj[field.name] === "NULL" ? {fontStyle: "italic"} : {}}
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
			case "textarea":
				return <textarea rows={3} className='form-control col-8 entityform-input' value={this.state.modalObj[field.name]} disabled={field.disabled === true}
				style={this.state.modalObj[field.name] === "NULL" ? {fontStyle: "italic"} : {}}
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
			case "boolean":
				return <Switch id={field.name} name={field.name} checked={this.state.modalObj[field.name] === true}
					className="react-switch mt-1" offColor="#F00"
					onChange={(checked) => this.setState({ modalObj: { ...this.state.modalObj, [field.name]: checked } })}
				/>
			case "option":
				// Support both static array and dynamic string (entity name to load from API)
				let optionsList = typeof field.optionsList === 'string' 
					? (this.state.optionsList[field.name] || [])
					: field.optionsList
				return <Select classNamePrefix="rs" className='col-8 p-0' id={field.name} name={field.name}
					placeholder={field.displayName}
					isClearable={!field.isRequired}
					value={this.state.modalObj[field.name] ? { label: this.state.modalObj[field.name], value: this.state.modalObj[field.name] } : null}
					options={optionsList}
					onChange={(option) => {
						this.setState({ modalObj: { ...this.state.modalObj, [field.name]: option ? option.value : null } })
					}}
				/>
			case "file":
				return <div className='d-flex flex-wrap col-8 mt-1'>
					<input type="file"
						onChange={(e) => {
							let formData = new FormData()
							formData.append('file', e.target.files[0])
							axios.post(`/api/file/store`, formData, {
								headers: {
									"Content-type": "application/json",
									"Content-Type": "multipart/form-data"
								}
							})
								.then((res) => {
									this.setState({ modalObj: { ...this.state.modalObj, [field.name]: res.data } })
								})
								.catch((err) => {
									console.log({ err })
								})
							console.log({ files2: e.target.files[0] })
						}}
					/>
					{this.state.modalObj[field.name] &&
						<span style={{ fontSize: 12 }}
							className="btn btn-link"
							onClick={() => {
								axios(
									{
										url: `/api/file/` + this.state.modalObj[field.name], //your url
										method: 'GET',
										responseType: 'blob', // important
									}
								).then((response) => {
									const url = window.URL.createObjectURL(new Blob([response.data]));
									const link = document.createElement('a');
									link.href = url;
									link.setAttribute('download', this.state.modalObj[field.name]); //or any other extension
									document.body.appendChild(link);
									link.click();
								})
							}}
						>
							{this.state.modalObj[field.name]}
							{/* <img src={`/api/file/${this.state.modalObj[field.name]}`} height="200px" /> */}
						</span>
					}
				</div>
			case "number":
				return <input 
					type="number"
					step="any"
					className='form-control col-8 entityform-input' 
					value={this.state.modalObj[field.name] === "NULL" ? "" : this.state.modalObj[field.name]} 
					disabled={field.disabled === true}
					ref={input => this.inputArr[ind] = input}
					onChange={(event) => {
						const value = event.target.value === "" ? null : parseFloat(event.target.value)
						this.setState({ modalObj: { ...this.state.modalObj, [field.name]: value } })
					}}
					onKeyUp={(e) => {
						if (e.key === "Enter") {
							this.inputArr[ind + 1].focus()
						}
					}}
				/>
			case "image":
				return [
					<input type="file"
						onChange={(e) => {
							let formData = new FormData()
							formData.append('file', e.target.files[0])
							axios.post(`/api/file/store`, formData, {
								headers: {
									"Content-type": "application/json",
									"Content-Type": "multipart/form-data"
								}
							})
								.then((res) => {
									this.setState({ modalObj: { ...this.state.modalObj, [field.name]: res.data } })
								})
								.catch((err) => {
									console.log({ err })
								})
							console.log({ files2: e.target.files[0] })
						}}
					/>,
					this.state.modalObj[field.name] &&
					<span style={{ fontSize: 12 }}
						className="btn btn-link"
						onClick={() => {
							axios(
								{
									url: `/api/file/` + this.state.modalObj[field.name], //your url
									method: 'GET',
									responseType: 'blob', // important
								}
							).then((response) => {
								const url = window.URL.createObjectURL(new Blob([response.data]));
								const link = document.createElement('a');
								link.href = url;
								link.setAttribute('download', this.state.modalObj[field.name]); //or any other extension
								document.body.appendChild(link);
								link.click();
							})
						}}
					>
						{/* {this.state.modalObj[field.name]} */}
						<img src={`/api/file/${this.state.modalObj[field.name]}`} height="500px" />
					</span>
				]
			default: return <div></div>
		}
	}



	render() {
		const { entity, entityId } = this.props
		return (
			<div>
				<div className='entityform-header'>
					<h2 className='entityform-title'>Formulaire {metadata[entity].displayName} {entityId}</h2>
					<div className='entityform-buttons'>
						<button type="button" className='btn btn-danger ml-2' ref={btn => this.inputArr[metadata[entity].fields.filter(field => field.hideForm !== true).length] = btn}
							onClick={() => {
								this.setState({ loading: false })
								// we need to change all values of properties of this.state.modalObj that equal to "NULL" to null
								let newObj = {}
								Object.keys(this.state.modalObj).forEach(key => {
									if (this.state.modalObj[key] === "NULL") {
										newObj[key] = null
									} else {
										newObj[key] = this.state.modalObj[key]
									}
								})
								this.setState({ modalObj: newObj })
								axios.post(`/api/${entity}`, newObj)
									.then(res => {
										this.props.goBack()
									})
							}}
						>
							<FontAwesomeIcon icon={faFloppyDisk} /> Enregistrer
						</button>
						<button className='btn btn-link' onClick={() => { this.props.goBack() }}>Annuler</button>
					</div>
				</div>
				<div className='entityform-container'>
					<div>
						<button className='btn btn-link' onClick={() => { this.props.goBack() }}>Retour au tableau</button>
					</div>
					<div className='row entityform-field'>
						{metadata[entity].fields.filter(field => field.hideForm !== true).map((field, ind) => {
							return <div className='row entityform-field col-6'>
								<label className='col-4 col-form-label text-right'>{field.displayName} :</label>
								{this.renderField(field, ind)}
							</div>
						})}
					</div>
				</div>

			</div>
		)
	}
}
