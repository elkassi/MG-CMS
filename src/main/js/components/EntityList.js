import { faCheck, faDownload, faFilter, faFilterCircleXmark, faFloppyDisk, faGear, faSearch, faMagnifyingGlass, faPenAlt, faPlus, faRotate, faTimes, faUpload } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import axios from 'axios';
import React, { Component } from 'react'
import { Modal } from 'react-bootstrap';
import "../styles/EntityList.scss"
import AdvancedPagination from "./utils/AdvancedPagination";
import SortIcon from "./utils/SortIcon";
import Switch from "react-switch";
import Select from "react-select";
import { filterOptions, metadata } from '../metadata';
import EntityForm from './EntityForm';
import { Link } from 'react-router-dom';
//import for reading excel with readXlsxFile
import readXlsxFile from 'read-excel-file'
import { connect } from 'react-redux';
import PropTypes from 'prop-types';


class EntityList extends Component {

	constructor() {
		super()
		this.state = {
			entriesPage: {
				totalElements: 0,
				totalPages: 1,
				numberOfElements: 0
			},
			sortProp: null,
			sortDirection: "desc",
			page: 0,
			size: null,
			entriesList: [],
			optionsList: {},
			error: null,
			modalObj: null,
			showModalFilter: null,
			modalFilter: {},
			importModal: null,
			showFilter: false,
			filterArr: [{ type: filterOptions[0].value }],
			filterFields: [],
			blockNextLoad: false,
			showAdjusterModal: false,
			adjusterModalArr: [],
			stats: [],
			statsByTypeDefaut: [],
		}
		this.inputArr = []
	}

	componentDidMount() {
		if (this.props.match.params.entityId != null) {
			axios.get(`/api/${this.props.match.params.entity}/${this.props.match.params.entityId}`)
				.then(res => {
					this.setState({ modalObj: res.data })
				})
		} else {
			this.searchSubEntriesPage();
		}
		if (this.props.match.params.entity && metadata[this.props.match.params.entity]) {
			this.setState({
				filterFields: (metadata[this.props.match.params.entity].fieldsFilter || metadata[this.props.match.params.entity].fields).map(e => {
					if (e.formDisplayProperty) {
						return { label: e.displayName, value: e.name + "." + e.formDisplayProperty, item: e }
					}
					return { label: e.displayName, value: e.name, item: e }
				})
			})
		}

	}

	componentDidUpdate(prevProps, prevState, snapshot) {
		if ((prevState.sortProp !== this.state.sortProp)
			|| (prevState.sortDirection !== this.state.sortDirection)
			|| (prevState.page !== this.state.page)
			|| (prevState.size != null && prevState.size !== this.state.size)
			|| (prevProps.match.params.entity !== this.props.match.params.entity)
			|| (prevProps.match.params.entityId !== this.props.match.params.entityId)
		) {

			if (prevProps.match.params.entity !== this.props.match.params.entity) {
				if (this.props.match.params.entity && metadata[this.props.match.params.entity]) {
					this.setState({
						entriesPage: {
							totalElements: 0,
							totalPages: 1,
							numberOfElements: 0
						},
						sortProp: null,
						sortDirection: "desc",
						// page: 0,
						// size: null,
						entriesList: [],
						optionsList: {},
						error: null,
						modalObj: null,
						showModalFilter: null,
						modalFilter: {},
						importModal: null,
						showFilter: false,
						showAdjusterModal: false,
						adjusterModalArr: [],
						filterArr: [{ type: filterOptions[0].value }],
						filterFields: (metadata[this.props.match.params.entity].fieldsFilter || metadata[this.props.match.params.entity].fields).map(e => {
							if (e.formDisplayProperty) {
								return { label: e.displayName, value: e.name + "." + e.formDisplayProperty, item: e }
							}
							return { label: e.displayName, value: e.name, item: e }
						}),
						blockNextLoad: this.state.page !== 0 || (metadata[this.props.match.params.entity].tableSize || 20) !== this.state.size
					})
					setTimeout(() => {
						this.searchSubEntriesPage({ page: 0, size: metadata[this.props.match.params.entity].tableSize || 20 });
					}, 100)
				}
			} else {
				if (this.props.match.params.entityId != null) {
					axios.get(`/api/${this.props.match.params.entity}/${this.props.match.params.entityId}`)
						.then(res => {
							this.setState({ modalObj: res.data })
						})
				} else if (this.state.blockNextLoad === false) {
					this.searchSubEntriesPage();
				} else {
					this.setState({ blockNextLoad: false })
				}
			}
		}
	}

	searchSubEntriesPage = (config) => {
		if (this.props.match.params.entity === "qualityNotice" && this.props.match.params.entityId == null) {
			axios.get(`/api/qualityNotice/stats`)
				.then(res => {
					this.setState({ stats: res.data })
				})
			axios.get(`/api/qualityNotice/statsByTypeDefaut`)
				.then(res => {
					this.setState({ statsByTypeDefaut: res.data })
				})

		}

		try {
			let page = this.state.page;
			if (config && config.page !== null && config.page !== undefined) {
				page = config.page;
			}
			let size = this.state.size || metadata[this.props.match.params.entity].tableSize || 20;
			if (config && config.size !== null && config.size !== undefined) {
				size = config.size;
			}
			this.setState({
				entriesPage: {
					totalElements: 0,
					totalPages: 1,
					numberOfElements: 0
				},
				entriesList: null,
				modalObj: null,

			})

			// for (let k in this.state.modalFilter) {
			// 	if (this.state.modalFilter.hasOwnProperty(k) && this.state.modalFilter[k] && this.state.modalFilter[k] !== "") {		
			// 		dependencies += k + "=" + this.state.modalFilter[k] + "&"
			// 	}
			// }
			let dependencies = "";
			dependencies = this.state.filterArr.filter(e => e.type && e.fieldName && e.value).map(e => e.type + "." + e.fieldName + "=" + e.value).join("&")
			// dependencies = this.state.filterArr.filter(e => e.type && e.fieldName && e.value).map((e,ind) => e.type + "." + e.fieldName+"."+ind + "=" + e.value).join("&")
			if (dependencies.length > 0) {
				dependencies += "&"
			}
			axios.get(`/api/${this.props.match.params.entity}/all?${dependencies}page=${page}&size=${size}&sort=${this.state.sortProp || (metadata[this.props.match.params.entity].firstOrderProperty || metadata[this.props.match.params.entity].fields[0].name)},${this.state.sortDirection}`)
				.then((res) => {
					this.setState({
						entriesList: res.data.content ? res.data.content : [],
						entriesPage: {
							totalElements: res.data.totalElements,
							totalPages: res.data.totalPages,
							numberOfElements: res.data.numberOfElements
						},
						page: page,
						size: size
					})
				})
				.catch(err => {
					if (err.response.data != null && err.response.data.username === "Invalid Username") {
						window.location.pathname = "/login";
					}
				})
		} catch (error) {
			console.log({ error })
		}
	}

	renderErrorsAlert(errors) {
		let arr = []
		for (let prop in errors) {
			arr.push(<li>{prop}: {errors[prop]}</li>)
		}
		return arr
	}

	sortChanged(field) {
		let sortProp = field;
		let propChanged = this.state.sortProp !== sortProp;
		let sortDirection = propChanged ? 'asc' : this.state.sortDirection === 'asc' ? 'desc' : 'asc';

		this.setState({ sortProp, sortDirection });
	} 

	renderHeader = (entity) => {
		return <thead className='entity-table-header'>
			<tr>
				{metadata[entity].fields
					.filter(elem => elem.hideTable !== true && !this.state.adjusterModalArr.includes(elem.name))
					.map(field => {
						return <th onClick={() => this.sortChanged(field.name)}>{field.displayName}<SortIcon currentSort={field.name} sortProp={this.state.sortProp} sortDirection={this.state.sortDirection} className="ml-1" /></th>
					})}
				<th >
					{/* {metadata[entity].fieldsFilter && metadata[entity].fieldsFilter.length > 0 && <button
						type="button" className='btn btn-outline-light mr-1'
						onClick={() => { this.setState({ showModalFilter: true }) }}
					>
						<FontAwesomeIcon icon={faFilter} size="sm" />
					</button>}
					{Object.keys(this.state.modalFilter).length > 0 && <button type="button" className='btn btn-outline-light'
						onClick={() => {
							this.setState({ modalFilter: {} })
							setTimeout(() => { this.searchSubEntriesPage() }, 10);
						}}
					>
						<FontAwesomeIcon icon={faFilterCircleXmark} />
					</button>} */}
				</th>
			</tr>
		</thead>
	}

	renderField(field, ind) {
		// const { entity, entityId } = this.props
		try {
			switch (field.type) {
				case "text":
					return <input className='form-control entityform-input' value={this.state.modalFilter[field.name]}
						ref={input => this.inputArr[ind] = input}
						onChange={(event) => {
							if (field.reg == null || field.reg.test(event.target.value)) {
								this.setState({ modalFilter: { ...this.state.modalFilter, [field.name]: event.target.value } })
							}
						}}
						onKeyUp={(e) => {
							if (e.key === "Enter") {
								this.inputArr[ind + 1].focus()
							}
						}}
					/>
				default: return <div></div>
			}

		} catch (e) {
			console.log(e)
		}
	}

	handleFileChange = async (event, entity) => {
		let file = event.target.files[0];
		try {
			// Read all sheets from the Excel file
			const sheets = await readXlsxFile(file, { getSheets: true });

			// Use Promise.all to read and convert each sheet into JSON
			const promises = sheets.map((sheet, index) =>
				readXlsxFile(file, { sheet: index + 1 }).then((rows) => {
					const arr = [];
					rows.forEach((row, rowInd) => {
						if (rowInd > 0) {
							const elem = {};
							row.forEach((cell, cellInd) => {
								elem[metadata[entity].fields[cellInd].name] = cell;
							});
							elem.id = null;
							elem.createdAt = null;
							elem.addedBy = null;
							elem.updatedAt = null;
							elem.updatedBy = null;
							arr.push(elem);
						}
					});
					return arr;
				})
			);

			// Wait for all promises to resolve and set the importModal state with the results
			const results = await Promise.all(promises);
			const allSheetsData = [].concat(...results);
			this.setState({ importModal: allSheetsData });
		} catch (error) {
			console.error('Error reading Excel file:', error);
		}
	};

	renderImportModal = (entity) => {
		//let give a random colorfull background color to the <td> of partNumberCover field in the table for each diffent value
		//the color should go well with a clack font color
		let partNumberCoverColor = {}, counter = {}
		//also some light colors
		let partNumberCoverColorArr = [
			"#ccccff", "#ffcccc", "#ccffcc", "#cccccc", "#ffffcc", "#ffccff", "#ccffff", "#ffffff",
			"#ff9999", "#99ff99", "#9999ff", "#ffff99", "#ff99ff", "#99ffff",
			"#ff6666", "#66ff66", "#6666ff", "#ffff66", "#ff66ff", "#66ffff",
			"#ff0000", "#00ff00", "#ffff00", "#ff00ff", "#00ffff",
			"#ff3333", "#33ff33", "#3333ff", "#ffff33", "#ff33ff", "#33ffff",
		]
		if (this.state.importModal != null) {
			let ind = 0
			this.state.importModal.map(elem => {
				try {
					let number = this.state.importModal.filter(e => (e.partNumberCover && e.partNumberCover.trim().toUpperCase() === elem.partNumberCover.trim().toUpperCase()) && (e.panelNumber && e.panelNumber.toString().trim().toUpperCase() === elem.panelNumber.toString().trim().toUpperCase())).length
					if (number > 1) {
						counter[elem.partNumberCover + ' - ' + elem.panelNumber] = number
					}
				} catch (e) {

				}

				if (elem.partNumberCover != null && partNumberCoverColor[elem.partNumberCover] == null) {
					partNumberCoverColor[elem.partNumberCover] = partNumberCoverColorArr[ind % partNumberCoverColorArr.length] // Math.floor(Math.random() * partNumberCoverColorArr.length)
					ind++;
				}
			})
		}

		return <Modal
			show={this.state.importModal !== null}
			onHide={() => this.setState({ importModal: null })}
			dialogClassName="modal-90w"
			centered
		>
			{this.state.importModal && <div style={{
				maxHeight: "80vh",
				overflow: "auto"
			}}>
				<h2 className='text-center'>{metadata[entity].displayName} Import</h2>

				<div className='d-flex' >
					<textarea rows={1} value="" onChange={event => {
						let arr = []
						event.target.value.split("\n").map(e => {
							if (e.length > 0) {
								let arrInfo = e.split("\t"), elem = {}
								elem.nomPlct = arrInfo[0]
								elem.nomDefPlct = arrInfo[1]
								metadata[entity].fields.filter(e => e.hideForm !== true).map((field, ind) => {
									elem[field.name] = arrInfo[ind]
								})
								arr.push(elem)
							}
						})

						this.setState({ importModal: arr })
					}} />
					<input type="file" onChange={e => this.handleFileChange(e, entity)} />

				</div>
				<div>
					{Object.keys(counter).length > 0 && <div className="alert alert-danger alert-error m-2" role="alert">
						<ul className='m-0'>
							{Object.keys(counter).map(e => {
								return <li>{e} : {counter[e]} répétition </li>
							})}
						</ul>
					</div>}

				</div>
				<div className='table-responsive mb-2'>
					<table className='table table-bordered m-0'>
						<thead>
							<tr>
								{metadata[entity].fields.filter(e => e.hideForm !== true).map(field => {
									return <th className='table-elem-sm'>{field.displayName}</th>
								})}
								<th className='table-elem-sm'><button type="button" className='btn btn-outline-primary' style={{ fontSize: 12 }} onClick={() => {
									this.state.importModal
										.map((rowElem, rowInd) => {
											if ((rowElem.saved !== true && !(counter[rowElem.partNumberCover + " - " + rowElem.panelNumber] && counter[rowElem.partNumberCover + " - " + rowElem.panelNumber] > 1))) {
												axios.post(`/api/${entity}`, rowElem)
													.then(res => {
														let arr = this.state.importModal
														arr[rowInd].saved = true
														this.setState({ importModal: arr })
													})
													.catch(err => {
														let arr = this.state.importModal
														arr[rowInd].saved = false
														if (err.response && typeof err.response.data === "object") {
															arr[rowInd].error = err.response.data.error
														}
														this.setState({ importModal: arr })
													})
											}
										})
								}}>All</button></th>
							</tr>
						</thead>
						<tbody>
							{this.state.importModal.map((elem, ind) => {
								return <tr style={(counter[elem.partNumberCover + " - " + elem.panelNumber] && counter[elem.partNumberCover + " - " + elem.panelNumber] > 1) ? { backgroundColor: "#ffbaba" } : {}}>
									{metadata[entity].fields.filter(e => e.hideForm !== true).map((field) => {
										if (field.name === "partNumberCover") {
											return <td className='table-elem-sm' style={{ backgroundColor: partNumberCoverColor[elem.partNumberCover] }}>
												{elem[field.name]}
											</td>
										}
										return <td className='table-elem-sm' style={(elem.error && elem.error[field.name]) ? { backgroundColor: "#ff8484" } : {}}>
											{elem[field.name]}
										</td>
									})}
									{(counter[elem.partNumberCover + " - " + elem.panelNumber] && counter[elem.partNumberCover + " - " + elem.panelNumber] > 1) ? <td></td> : <td className='table-elem-sm' style={elem.saved === false ? { backgroundColor: "#ff8484" } : {}}>
										{elem.saved ? <span className='text-success' >Saved</span> : <button type="button" className='btn btn-outline-primary' style={{ fontSize: 12 }} onClick={() => {
											axios.post(`/api/${entity}`, elem)
												.then(res => {
													let arr = [...this.state.importModal]
													arr[ind].saved = true
													this.setState({ importModal: arr })
												})
												.catch(err => {
													let arr = [...this.state.importModal]
													arr[ind].saved = false
													if (err.response && typeof err.response.data === "object") {
														arr[ind].error = err.response?.data
													}

													this.setState({ importModal: arr })
												})
										}}><FontAwesomeIcon icon={faFloppyDisk} /></button>}
									</td>}
								</tr>
							})}
						</tbody>
					</table>
				</div>
			</div>}
		</Modal>
	}

	renderAdjuterModal = (entity) => {
		let arr = []
		let fields = metadata[entity].fields.filter(elem => elem.hideTable !== true)
		for (let i = 0; i < fields.length; i++) {
			arr.push(<div style={{ width: "33.33%", paddingLeft: "20" }} key={"ajuster-" + i}>
				<input id={fields[i].name} type='checkbox' checked={!this.state.adjusterModalArr.includes(fields[i].name)}
					onChange={(e) => {
						if (!e.target.checked) {
							this.setState({ adjusterModalArr: [...this.state.adjusterModalArr, fields[i].name] })
						} else {
							this.setState({ adjusterModalArr: this.state.adjusterModalArr.filter(e => e !== fields[i].name) })
						}
					}}
				/>
				<label className='mb-0 ml-2' for={fields[i].name}>{fields[i].displayName}</label>
			</div>)
		}
		return <Modal
			show={this.state.showAdjusterModal}
			onHide={() => this.setState({ showAdjusterModal: false })}
			dialogClassName="modal-75w"
			centered
		>
			{this.state.showAdjusterModal && <div style={{ maxHeight: "80vh", overflowY: 'auto' }}>
				<h4 className='text-center'>Ajuster les colonnes</h4>
				<div style={{ display: "flex", flexWrap: "wrap", position: "relative" }}>{arr}</div>
			</div>}
		</Modal>
	}

	renderFilterField = (arr, ind) => {
		if (arr[ind].item
			// && arr[ind].item.type === "text"
		) {
			return [
				<Select classNamePrefix="rs"
					placeholder={""} className='col-2 p-0 ml-2 '
					isClearable={false}
					value={(arr[ind].type)
						? filterOptions.find(e => e.value === arr[ind].item.type)
						: null
					}
					options={filterOptions}
					onChange={(option) => {
						arr[ind].type = option.value
						this.setState({ filterArr: [...arr] })
					}}
				/>,

				<input className='ml-2 filter-input'
					value={arr[ind].value}
					onChange={e => {
						arr[ind].value = e.target.value
						this.setState({ filterArr: [...arr] })
					}}
				/>]
		}
	}

	renderFilter = () => {
		let arr = this.state.filterArr;

		return <div className='filter-container' style={this.state.showFilter ? { maxHeight: (33 * arr.length + 40) } : { maxHeight: 0, overflow: "hidden" }}>
			<div className='filter-list'>
				{arr.map((filter, ind) => <div className='d-flex align-items-center mb-1' key={"filter-" + ind}>
					<Select classNamePrefix="rs"
						placeholder={"Colonne..."} className='col-2 p-0  ml-2'
						isClearable={false}
						value={(filter.fieldName)
							? this.state.filterFields.find(e => e.name === filter.fieldName)
							: null
						}
						options={this.state.filterFields}
						onChange={(option) => {
							arr[ind].fieldName = option.value
							this.setState({ filterArr: [...arr] })
						}}
					/>
					<Select classNamePrefix="rs"
						placeholder={""} className='col-2 p-0 ml-2 '
						isClearable={false}
						value={(filter.type)
							? filterOptions.find(e => e.value === filter.type)
							: null
						}
						options={filterOptions}
						onChange={(option) => {
							arr[ind].type = option.value
							this.setState({ filterArr: [...arr] })
						}}
					/>
					<input className='ml-2 filter-input'
						value={filter.value}
						onChange={e => {
							arr[ind].value = e.target.value
							this.setState({ filterArr: [...arr] })
						}}
					/>

					{/* {this.renderFilterField(arr, ind)} */}

					<button className='btn btn-outline-primary ml-2' onClick={() => {
						// add an empty object in arr in position ind+1
						arr.splice(ind + 1, 0, { type: filterOptions[0].value })
						this.setState({ filterArr: [...arr] })
					}}><FontAwesomeIcon icon={faPlus} /></button>
					<button className='btn btn-outline-danger ml-2' disabled={arr.length < 2} onClick={() => {
						// remove object in arr in position ind
						arr.splice(ind, 1)
						this.setState({ filterArr: [...arr] })
					}}><FontAwesomeIcon icon={faTimes} /></button>
					{ind === 0 && <button className='btn btn-outline-success ml-2 btn-sm' onClick={() => {

						this.searchSubEntriesPage({ page: 0 })
					}}><FontAwesomeIcon icon={faSearch} /> Chercher</button>}
					{ind === 0 && <button className='btn btn-outline-secondary ml-2 btn-sm' onClick={() => {
						this.setState({ filterArr: [{ type: filterOptions[0].value }] })
						this.searchSubEntriesPage()
					}}><FontAwesomeIcon icon={faTimes} /> Reset</button>}
				</div>)}
			</div>
		</div>
	}

	renderRow = (entity, item, ind) => {
		const { user } = this.props.security;

		try {
			return <tr key={"row-" + ind} className={metadata[entity].operation.includes("Edit") ? "table-row-selective" : ""}
				onDoubleClick={() => {
					if (metadata[entity].operation.includes("Edit")) {
						if (entity === "qualityNotice") {
							window.open(`/qualityNoticeValidation?numeroQn=${item.numeroQn}`, "_blank")
						} else if (entity === "demandeChangementSerie") {
							window.open(`/demandeChangementSerieValidation?id=${item.id}`, "_blank")
						} else {
							this.props.history.push(`/${entity}/${item[metadata[entity].fields[0].name]}`)
						}
					}
				}}
			>
				{metadata[entity].fields
					.filter(elem => elem.hideTable !== true && !this.state.adjusterModalArr.includes(elem.name))
					.map(field => {

						if((field.name === "cuttingPlan" && field.type === "number") || (entity === "cuttingPlanData" && field.name === "id")){
							// we will show a the number and next to it a small button that do : window.open(`/${entity}/${item[metadata[entity].fields[0].name]}`, '_blank').focus();
							return <td key={field.name}>
								<div className='d-flex justify-content-center align-items-center'>
									<div style={{ width: 50 }}>{item[field.name]}</div>
									<button type="button" className='btn btn-outline-dark btn-sm' style={{ fontSize: 8, padding: "2 6" }}
										onClick={() => {
											window.open(`/cuttingPlan/${item[field.name]}`, '_blank').focus();
										}}
									>
										<FontAwesomeIcon icon={faMagnifyingGlass} />
									</button>
								</div>
							</td>
						}

						let styleExtra = {}
						if (entity === "qualityNotice" && field.name === "numeroQn") {
							styleExtra = item.reponse === "Accepté" ? { backgroundColor: "#d4edda" }
								: item.reponse === "Validé" ? { backgroundColor: "#d4edda" }
								: item.reponse === "Refusé" ? { backgroundColor: "#f8d7da" }
									: item.reponse === "En attente de la validation qualité réception" ? { backgroundColor: "#cdd6ff" }
										: (item.reponse === "En attente de la validation logistique") ? { backgroundColor: "#f0cdff" }
											: { backgroundColor: "#fff3cd" }
						}
						// demandeChangementSerie
						if (entity === "demandeChangementSerie" && field.name === "reponseDepartement") {
							styleExtra = item[field.name] === "Validée" ? { backgroundColor: "#d4edda" }
								: item[field.name] === "Refusée" ? { backgroundColor: "#f8d7da" }
									: item[field.name] === "En attente" ? { backgroundColor: "#fff3cd" }
											: { }
						}
						if (entity === "demandeChangementSerie" && field.name === "reponse") {
							styleExtra = item[field.name] === "Traitée" ? { backgroundColor: "#d4edda" }
								: item[field.name] === "Refusée" ? { backgroundColor: "#f8d7da" }
									: item[field.name] === "En attente" ? { backgroundColor: "#fff3cd" }
											: { }
						}



						try {
							switch (field.type) {
								case "boolean":
									return (item[field.name] === true ? <td style={styleExtra}><FontAwesomeIcon icon={faCheck} color="green" /></td> : item[field.name] === false ? <td><FontAwesomeIcon icon={faTimes} color="red" /></td> : <td></td>)
								case "object":
									if (item[field.name] && typeof item[field.name][field.formDisplayProperty] === "string") {
										return <td style={styleExtra}>{item[field.name] && item[field.name][field.formDisplayProperty]}</td>
									}
									break;
								default:
									if (typeof item[field.name] === "string" || typeof item[field.name] === "number" || typeof item[field.name] === "bigint") {
										return <td style={styleExtra}>{item[field.name]}</td>
									}

							}
						} catch (e) {
							console.log(e)
						}
						return <td></td>
					})}
				<td style={{ width: 50 }}>
					<div className='d-flex' style={{ margin: "auto 0" }}>
						{metadata[entity].operation.includes("Edit") && <button type="button" className='btn btn btn-outline-dark btn-sm mr-1'
							onClick={() => { this.props.history.push(`/${entity}/${item[metadata[entity].fields[0].name]}`) }} style={{ fontSize: 12, padding: "3 6" }}>
							<FontAwesomeIcon icon={faPenAlt} />
						</button>}
						{metadata[entity].operation.includes("Delete") && <button type="button" className='btn btn btn-outline-dark btn-sm' style={{ fontSize: 12, padding: "3 6" }}
							onClick={() => {
								if (window.confirm("voulez vous supprimer cette ligne?")) {
									axios.post(`/api/${entity}/delete`, item)
										.then((res) => {
											this.searchSubEntriesPage()
										})
										.catch(err => {

										})
								}
							}}
						>
							<FontAwesomeIcon icon={faTimes} />
						</button>}
					</div>
				</td>
			</tr>
		} catch (err) {
			return;
		}

	}


	render() {
		console.log(this.props)
		let { entity, entityId } = this.props.match.params
		if (metadata[entity] == null) {
			return <div>
				<h1 >Entity {entity} not found</h1>
			</div>
		}
		if (entity) {
			if (entityId) {
				return <EntityForm entity={entity} entityId={entityId} goBack={() => {
					this.searchSubEntriesPage()
					this.props.history.push(`/${entity}`)
				}} />

			} else if (this.state.modalObj != null) {
				return <EntityForm entity={entity} goBack={() => { this.searchSubEntriesPage(); this.setState({ modalObj: null }) }} />
			}
		}

		return (
			<div>
				{entity === "qualityNotice"
					? <div className="d-flex" style={{ margin: 10 }} >
						<div>
							<div style={{ fontSize: 20, paddingLeft: "30px" }}>{this.state.stats.map(e => <span className="mr-3 ml-3"><strong className="mr-3">{e.info} :</strong>{e.value}</span>)}</div>
							<div style={{ fontSize: 20, paddingLeft: "30px" }}>{this.state.statsByTypeDefaut.map(e => <span className="mr-3 ml-3"><strong className="mr-3">{e.info} :</strong>{e.value}</span>)}</div>
						</div>
						<div className="flex-fill text-right"><h1>{metadata[entity].displayName}</h1></div>
					</div>
					: <h1 className='text-center' style={{ marginTop: 10 }}>{metadata[entity].displayName}</h1>}
				<div className='d-flex align-items-center mb-1 mr-2'>
					{metadata[entity].operation.includes("Add") && <button type="button" className='btn btn-outline-danger ml-2' onClick={() => { this.setState({ modalObj: {} }) }} style={{ padding: 8 }}>
						<span style={{ fontSize: 12 }}><FontAwesomeIcon icon={faPlus} /></span>
					</button>}
					{metadata[entity].operation.includes("Export") && <button type="button" className='btn btn-outline-danger ml-2'
						onClick={() => {
							// this.searchSubEntriesPage()
							if (window.confirm(`est-ce-que vous êtes sûre d'exporter ${this.state.entriesPage.totalElements} ligne?`)) {
								let dependencies = "";
								dependencies = this.state.filterArr.filter(e => e.type && e.fieldName && e.value).map(e => e.type + "." + e.fieldName + "=" + e.value).join("&")
								axios(
									{
										url: `/api/${entity}/download/${entity}.xlsx?${dependencies}`, //your url
										method: 'GET',
										responseType: 'blob', // important
									}
								).then((response) => {
									const url = window.URL.createObjectURL(new Blob([response.data]));
									const link = document.createElement('a');
									link.href = url;
									link.setAttribute('download', `${entity}.xlsx`); //or any other extension
									document.body.appendChild(link);
									link.click();
								});
							}
						}}
					>
						<span style={{ fontSize: 12 }}><FontAwesomeIcon icon={faDownload} /> Export</span>
					</button>}
					{metadata[entity].operation.includes("Import") && <button type="button" className='btn btn-outline-danger ml-2' onClick={() => { this.setState({ importModal: [] }) }}>
						<span style={{ fontSize: 12 }}><FontAwesomeIcon icon={faUpload} /> Import</span>
					</button>}
					{metadata[entity].operation.includes("Supprimer") && <button type="button" className='btn btn-outline-danger ml-2' disabled={this.state.filterArr.filter(e => e.type && e.fieldName && e.value).length === 0}
						onClick={() => {
							if (window.confirm(`est-ce-que vous êtes sûre de supprimer ${this.state.entriesPage.totalElements} ligne?`)) {
								let dependencies = "";
								dependencies = this.state.filterArr.filter(e => e.type && e.fieldName && e.value).map(e => e.type + "." + e.fieldName + "=" + e.value).join("&")
								axios.post(`/api/${entity}/supprimer?${dependencies}`).then((response) => {
									this.searchSubEntriesPage()
								});
							}
						}}
					>
						<span style={{ fontSize: 12 }}><FontAwesomeIcon icon={faTimes} /> Supprimer</span>
					</button>}
					<div style={{ flex: 1 }}></div>
					<button type="button" className='btn btn-outline-danger btn-sm mr-2' onClick={() => {
						this.setState({ showAdjusterModal: !this.state.showAdjusterModal })
					}}>
						<FontAwesomeIcon icon={faGear} /> Ajuster
					</button>
					<button type="button" className='btn btn-outline-danger btn-sm' onClick={() => {
						this.setState({ showFilter: !this.state.showFilter })
					}}>
						<FontAwesomeIcon icon={faFilter} /> Filtre
					</button>
				</div>
				{this.renderFilter()}
				<div className='px-2'>
					<div className='table-responsive entity-table mb-2'
						style={this.state.showFilter ? { maxHeight: `calc(100vh - ${(33 * Math.min(this.state.filterArr.length, 10) + 250)}px)` } : { maxHeight: `calc(100vh - 213px)` }}
					>
						<table className='table table-bordered m-0'>
							{this.renderHeader(entity)}
							<tbody>
								{this.state.entriesList == null ? <tr><td colSpan={100} style={{textAlign: "start"}}>Chargement...</td></tr> :
									this.state.entriesList.length === 0
										? <tr><td colSpan={100} style={{textAlign: "start"}}>Aucune données disponibles</td> </tr>
										: this.state.entriesList.map((item, ind) => {
											return this.renderRow(entity, item, ind)
										})}
							</tbody>
						</table>
					</div>
					<div className='d-flex mb-2'>
						<div className='mr-2'>
							<small>{this.state.entriesPage.totalElements} entrées trouvées</small>
						</div>
						<div style={{ flex: 1 }}></div>
						<select
							onChange={(option) => this.setState({ size: parseInt(option.target.value) })}
							value={this.state.size}
							className="mr-2"
							style={{ fontSize: 14, padding: 4 }}
						>
							<option value="10">10</option>
							<option value="20">20</option>
							<option value="50">50</option>
							<option value="100">100</option>
						</select>
						<div className='m-0'>
							<AdvancedPagination
								ref={this.paginationRef}
								pageCount={this.state.entriesPage.totalPages}
								onPageChange={(page) => this.setState({ page })}
							/>
						</div>
					</div>
				</div>
				{this.renderImportModal(entity)}
				{this.renderAdjuterModal(entity)}
			</div>
		)
	}
}


EntityList.propTypes = {
	security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
	security: state.security
})

export default connect(mapStateToProps, {})(EntityList);
