import axios from 'axios';
import moment from 'moment';
import React, { Component } from 'react'
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { optionsShift } from '../../metadata';
import Select from "react-select";
import { faArrowLeft, faArrowRight, faArrowUp, faCut, faEnvelope, faMagnifyingGlass, faPenAlt, faPlus, faPrint, faSave, faSpinner, faSync, faTimes, faTrash } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { Button, ButtonGroup, Modal } from 'react-bootstrap'
import ReactToPrint from "react-to-print";
import logo from '../../assets/images/lear_logo.png'
import GammePn from './GammePn';
import GammeCMS from './GammeCMS';


class ImportationNew extends Component {
	constructor() {
		super();
		this._isMounted = false;
		this.state = {
			entriesList: [],
			cuttingPlanList: [],
			optionsList: {
				zone: [],
				projet: [],
			},
			date: moment().add(10, 'hours').format('YYYY-MM-DD'),
			shift: this.getShift(moment().add(10, 'hours')),
			filter: {},
			filterPC: {},
			viewedTable: "workOrder",
			suggestions: [],
			importModalInd: null,
			selectedBoxs: [],
			specialElems: [],
			savingSequence: false,
			gammeCMSSequenceModal: null,
			gammeTechniqueArr: [],
			productionTableArr: [],
			scheduleMachineArr: [],
			timingPlacementObj: [],
			loading: false,
			materialConfigCache: {},
			validationWarnings: [],
			partNumberBoomMismatch: false,
			partNumberBoomWarnings: [],
			showDuplicateDialog: false,
			duplicateGroups: [],
			fusingWos: false,
			hoveredRemarkInfo: null,
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
		this._isMounted = true
		// this.getData(moment().add(2, 'hours').format('YYYY-MM-DD'), this.getShift(moment().add(2, 'hours')))
		axios.get("/api/productionTable/list")
			.then(res => {
				if (!this._isMounted) return
				this.setState({productionTableArr : res.data})
			})
		axios.get(`/api/zone/list`)
			.then((res) => {
				if (!this._isMounted) return
				this.setState({
					optionsList: {
						...this.state.optionsList,
						zone: res.data.map((elem) => ({ value: elem.nom, label: elem.nom, item: elem }))
					}
				})
			})

		axios.get(`/api/projet/list`)
			.then((res) => {
				if (!this._isMounted) return
				this.setState({
					optionsList: {
						...this.state.optionsList,
						projet: res.data.map((elem) => ({ value: elem.nom, label: elem.nom, item: elem }))
					}
				})
			})


	}

	componentWillUnmount() {
		this._isMounted = false
	}

	getData = async (date, shift) => {
		let filter = "date=" + date
		this.setState({ entriesList: [], showDuplicateDialog: false, duplicateGroups: [] })
		if (shift) {
			filter += "&shift=" + shift
		}

		const resWO = await axios.get(`/api/workOrder/filter?${filter}`)
		//load alos OrderSchedule of this shift and date with /api/orderSchedule/findByDateAndShift?dateDemande=${date}&shiftDemande=${shift}
		const resOrderSchedule = await axios.get(`/api/orderSchedule/findByDateAndShift?dateDemande=${date}&shiftDemande=${shift}`)

		// Map idDemande in resOrderSchedule.data to wo in resWO.data and update statusDemande
		if (Array.isArray(resOrderSchedule.data) && Array.isArray(resWO.data)) {
			const orderScheduleMap = {};
			resOrderSchedule.data.forEach(os => {
				orderScheduleMap[os.idDemande] = os;
			});
			resWO.data.forEach(wo => {
				if (orderScheduleMap[wo.wo] !== undefined) {
					wo.statusDemande = orderScheduleMap[wo.wo].statusDemande;
					wo.remarqueDemande = orderScheduleMap[wo.wo].remarqueDemande;
				}
			});
		}
		// resWO.data has wo prop is the same as the prop idDemande in the resOrderSchedule.data
		// i want all resOrderSchedule.data to be  the same as resWO.data but with the prop idDemande replaced by wo
		// take statusDemande  in resOrderSchedule and put it in resWO.data statusDemande
		// first we will pass the list of work orders  and get the sequences of it then we will load the series, the partnumbers, the boxes of those sequences
		const resSequences = await axios.post(`/api/cuttingRequestPartNumberData/getSequencesByWos`, resWO.data.map(e => e.wo))
		let sequences = resSequences.data
		// let sequences = []
		/*
		now we will do this post axios with the list of sequence in the body :
		/api/cuttingRequestData/bySequences
		/api/cuttingRequestPartNumberData/bySequences
		/api/cuttingRequestSerieData/bySequences
		/api/cuttingRequestBoxData/bySequences
		*/
		const resCuttingRequestData = await axios.post(`/api/cuttingRequestData/bySequences`, sequences)
		const resCuttingRequestPartNumberData = await axios.post(`/api/cuttingRequestPartNumberData/bySequences`, sequences)
		const resCuttingRequestSerieData = await axios.post(`/api/cuttingRequestSerieData/bySequences`, sequences)
		const resCuttingRequestBoxData = await axios.post(`/api/cuttingRequestBoxData/bySequences`, sequences)
		// as you can see below, we need to update the work orders with the cutting request part numbers  and increase thier counter and imported properties
		let obj = {};
		if (resWO.data && resWO.data.length > 0) {
			resWO.data.forEach(e => {
				obj[e.wo] = {
					...e,
					total: e.qtyOpen + e.qtyRejeter + e.qtyCompleted,
					counter: 0,
					imported: 0,
				};
			});
		}
		if (resCuttingRequestPartNumberData.data && resCuttingRequestPartNumberData.data.length > 0) {
			resCuttingRequestPartNumberData.data.forEach(e => {
				if (obj[e.wo]) {
					obj[e.wo].counter += e.quantity;
					obj[e.wo].imported += e.quantity;
				}
			});
		}

		let cuttingPlanList = resCuttingRequestData.data.map(e => {
			return {
				...e,
				cuttingRequestPartNumbers: resCuttingRequestPartNumberData.data.filter(pn => pn.cuttingRequest === e.sequence),
				cuttingRequestSeries: resCuttingRequestSerieData.data.filter(serie => serie.sequence === e.sequence),
				cuttingRequestBoxs: resCuttingRequestBoxData.data.filter(box => box.sequence === e.sequence),
			}
		}).filter(e => e.cuttingRequestPartNumbers.length > 0 || e.cuttingRequestSeries.length > 0 || e.cuttingRequestBoxs.length > 0)
		// now we will set the state with the entriesList and cuttingPlanList
		const entriesList = Object.values(obj)
		if (!this._isMounted) return
		this.setState({
			cuttingPlanList: cuttingPlanList,
			entriesList: entriesList,
			date: date,
			shift: shift,
			filter: {
				wo: null,
				woid: null,
				item: null,
				partNumber: null,
				description: null,
				groupName: null,
				designGroup: null,
				coverGroup: null,
				partNumberStatus: null,
				qtyOpen: null,
				qtyRejeter: null,
				qtyCompleted: null,
				status: null,
				total: null,
				counter: null,
				imported: null,
				remarqueDemande: null,
			},
			filterPC: {
				sequence: null,
				cuttingPlanId: null,
				cmsId: null,
				projet: null,
				version: null,
				modele: null,
				definition: null,
				date: date || moment().format('YYYY-MM-DD'),
			}
		})
		this.loadPlacement(resCuttingRequestSerieData.data.map(e => e.placement))

		// now we will get the suggestions
		this.getSuggestions2(entriesList, cuttingPlanList)
		const resGammeTechnique = await axios.post(`/api/gammeTechniqueCMS/getByPartNumber`, resWO.data.map(e => e.partNumber))
		if (!this._isMounted) return
		this.setState({ gammeTechniqueArr: resGammeTechnique.data.map(e => { return { partNumber: e.str1, site: e.str2, packaging: e.num1 } }) })
		const resScheduleMachine = await axios.get(`/api/query/scheduleMachine?date=${date}&shift=${shift}`)
		if (!this._isMounted) return
		this.setState({scheduleMachineArr : resScheduleMachine.data})

		// Check for duplicate partNumbers after loading work orders
		try {
			const resDuplicates = await axios.get(`/api/workOrder/duplicates?date=${date}&shift=${shift}`)
			if (resDuplicates.data.hasDuplicates) {
				this.setState({
					showDuplicateDialog: true,
					duplicateGroups: resDuplicates.data.duplicates
				})
			}
		} catch (err) {
			console.log('Duplicate check error:', err)
		}

	}

	refreshData = async (date, shift) => {
		// Set loading state to true and disable the button
		this.setState({ loading: true })

		try {
			// Send refresh request to the API
			await axios.get(`/api/workOrder/refresh?date=${date}&shift=${shift}`)
			
			// After refresh is complete, get the updated data
			await this.getData(date, shift)
		} catch (error) {
			console.error('Error refreshing data:', error)
		} finally {
			// Reset loading state
			this.setState({ loading: false })
		}
	}

	reloadStatuses = async () => {
		try {
			const resOrderSchedule = await axios.get(`/api/orderSchedule/findByDateAndShift?dateDemande=${this.state.date}&shiftDemande=${this.state.shift}`);
			if (Array.isArray(resOrderSchedule.data) && Array.isArray(this.state.entriesList)) {
				const orderScheduleMap = {};
				resOrderSchedule.data.forEach(os => {
					orderScheduleMap[os.idDemande] = os;
				});
				const updatedEntriesList = this.state.entriesList.map(wo => {
					if (orderScheduleMap[wo.wo] !== undefined) {
						return {
							...wo,
							statusDemande: orderScheduleMap[wo.wo].statusDemande,
							remarqueDemande: orderScheduleMap[wo.wo].remarqueDemande,
						};
					}
					return wo;
				});
				this.setState({ entriesList: updatedEntriesList });
			}
		} catch (error) {
			console.error('Error reloading statuses:', error);
		}
	}

	getDuplicateGroupForWorkOrder = (wo) => {
		const duplicateGroups = Array.isArray(this.state.duplicateGroups) ? this.state.duplicateGroups : []
		return duplicateGroups.find(group => Array.isArray(group.workOrders) && group.workOrders.some(item => item.wo === wo)) || null
	}

	getDuplicateGroupsForCuttingRequest = (cuttingRequest) => {
		if (!cuttingRequest || !Array.isArray(cuttingRequest.cuttingRequestPartNumbers)) {
			return []
		}

		const seenPartNumbers = new Set()
		return cuttingRequest.cuttingRequestPartNumbers.reduce((acc, partNumberLine) => {
			const duplicateGroup = this.getDuplicateGroupForWorkOrder(partNumberLine.wo)
			if (duplicateGroup && !seenPartNumbers.has(duplicateGroup.partNumber)) {
				seenPartNumbers.add(duplicateGroup.partNumber)
				acc.push(duplicateGroup)
			}
			return acc
		}, [])
	}

	getSplitCandidateForPartNumber = (partNumberLine, entriesListArg = this.state.entriesList) => {
		const entriesList = Array.isArray(entriesListArg) ? entriesListArg : []
		if (!partNumberLine || !partNumberLine.wo) {
			return null
		}

		const workOrder = entriesList.find(entry => entry.wo === partNumberLine.wo)
		const totalQty = Number(workOrder?.total)
		const importedQty = Number(partNumberLine.quantity)

		if (!workOrder || Number.isNaN(totalQty) || Number.isNaN(importedQty) || importedQty >= totalQty) {
			return null
		}

		return {
			wo: partNumberLine.wo,
			woid: partNumberLine.woid,
			partNumber: partNumberLine.partNumber,
			importedQty,
			totalQty,
			remainingQty: totalQty - importedQty,
		}
	}

	getSplitCandidatesForCuttingRequest = (cuttingRequest, entriesListArg = this.state.entriesList) => {
		const entriesList = Array.isArray(entriesListArg) ? entriesListArg : []
		if (!cuttingRequest || !Array.isArray(cuttingRequest.cuttingRequestPartNumbers) || entriesList.length === 0) {
			return []
		}

		const entriesByWo = entriesList.reduce((acc, entry) => {
			if (entry && entry.wo) {
				acc[entry.wo] = entry
			}
			return acc
		}, {})

		const splitByWo = {}
		cuttingRequest.cuttingRequestPartNumbers.forEach(partNumberLine => {
			if (!partNumberLine || !partNumberLine.wo || partNumberLine.quantity === null || partNumberLine.quantity === undefined) {
				return
			}

			const workOrder = entriesByWo[partNumberLine.wo]
			const totalQty = Number(workOrder?.total)
			const importedQty = Number(partNumberLine.quantity)
			if (!workOrder || Number.isNaN(totalQty) || Number.isNaN(importedQty)) {
				return
			}

			if (!splitByWo[partNumberLine.wo]) {
				splitByWo[partNumberLine.wo] = {
					wo: partNumberLine.wo,
					woid: partNumberLine.woid,
					totalQty,
					importedQty: 0,
					remainingQty: 0,
					partNumbers: [],
				}
			}

			splitByWo[partNumberLine.wo].importedQty += importedQty
			if (partNumberLine.partNumber) {
				splitByWo[partNumberLine.wo].partNumbers.push(partNumberLine.partNumber)
			}
		})

		return Object.values(splitByWo)
			.map(candidate => ({
				...candidate,
				remainingQty: candidate.totalQty - candidate.importedQty,
			}))
			.filter(candidate => candidate.importedQty > 0 && candidate.importedQty < candidate.totalQty)
	}

	getRemarkParts = (remarqueDemande) => {
		if (!remarqueDemande) {
			return []
		}

		return remarqueDemande
			.split('|')
			.map(part => part.trim())
			.filter(part => part.length > 0)
	}

	showRemarkTooltip = (event, item) => {
		const remarks = this.getRemarkParts(item.remarqueDemande)
		if (remarks.length === 0) {
			return
		}

		const viewportWidth = window.innerWidth || 1280
		const viewportHeight = window.innerHeight || 720
		const tooltipWidth = 360
		const tooltipHeight = Math.min(remarks.length * 54 + 28, 260)
		const left = Math.min(event.clientX + 16, viewportWidth - tooltipWidth - 16)
		const top = Math.min(event.clientY + 16, viewportHeight - tooltipHeight - 16)

		this.setState({
			hoveredRemarkInfo: {
				wo: item.wo,
				remarks,
				left,
				top,
			},
		})
	}

	hideRemarkTooltip = () => {
		if (this.state.hoveredRemarkInfo !== null) {
			this.setState({ hoveredRemarkInfo: null })
		}
	}

	renderRemarkTooltip = () => {
		const info = this.state.hoveredRemarkInfo
		if (!info) {
			return null
		}

		return (
			<div
				style={{
					position: 'fixed',
					left: info.left,
					top: info.top,
					zIndex: 10001,
					width: 360,
					maxWidth: 'calc(100vw - 32px)',
					maxHeight: 260,
					overflowY: 'auto',
					backgroundColor: '#fffdf4',
					border: '1px solid #e5c96b',
					borderRadius: 12,
					boxShadow: '0 14px 28px rgba(0,0,0,0.18)',
					padding: '12px 14px',
					pointerEvents: 'none',
				}}
			>
				<div style={{ fontWeight: 700, color: '#8a6d00', marginBottom: 8 }}>WO {info.wo}</div>
				{info.remarks.map((remark, index) => (
					<p key={`${info.wo}-${index}`} style={{ margin: 0, whiteSpace: 'normal', lineHeight: 1.45, color: '#4d3b00' }}>
						{remark}
					</p>
				))}
			</div>
		)
	}
	/*
private String wo;
	private String woid;
	private String item;
	private String partNumber; // to continue
	private String description;
	private String groupName;
	private String designGroup;
	private String coverGroup;
	private String partNumberStatus;
	private Double qtyOpen;
	private Double qtyRejeter;
	private Double qtyCompleted;
	private LocalDate dueDate;
	private String shift;
	private String status;
	*/

	renderHeader = () => {
		const { user } = this.props.security;
		return <thead className='entity-table-header'>
			<tr>
				<th className='table-elem-sm'>WO</th>
				<th className='table-elem-sm'>WO ID</th>
				<th className='table-elem-sm'>Item</th>
				<th className='table-elem-sm'>Part Number</th>
				<th className='table-elem-sm'>Description</th>
				<th className='table-elem-sm'>Projet</th>
				<th className='table-elem-sm'>Version</th>
				<th className='table-elem-sm'>Cover</th>
				<th className='table-elem-sm'>PN Status</th>
				{/* <th className='table-elem-sm'>Open</th>
				<th className='table-elem-sm'>Rejeter</th>
				<th className='table-elem-sm'>Completed</th> */}
				<th className='table-elem-sm'>Status</th>
				<th className='table-elem-sm'>Pk</th>
				<th className='table-elem-sm'>Total</th>
				<th className='table-elem-sm'>Compteur</th>
				<th className='table-elem-sm'>Importés</th>
				{/* Deactivation header for ROLE_CAD */}
				{/* {user && user.roles.map(e => e.authority).includes("ROLE_CAD") && <th className='table-elem-sm'>Actions</th>} */}
			</tr>
			<tr>
				<th className='table-elem-sm'>
					<input style={{ width: 90 }}
						value={this.state.filter.wo}
						onChange={event => this.setState({ filter: { ...this.state.filter, wo: event.target.value === "" ? null : event.target.value } })}
						onKeyUp={e => {
							if (e.key === 'Enter') {
								axios.get(`/api/workOrder/all?startWith.wo=${this.state.filter.wo}&page=0&size=20&sort=createdAt,desc`)
									.then(res => {
										if (res.data.content.length > 0) {
											this.setState({ date: res.data.content[0].dueDate, shift: res.data.content[0].shift })
											this.getData(res.data.content[0].dueDate, res.data.content[0].shift)
										}
									})
							}
						}}
					/>
					<button className='btn btn-primary btn-sm'
						onClick={() => {
							axios.get(`/api/workOrder/all?startWith.wo=${this.state.filter.wo}&page=0&size=20&sort=createdAt,desc`)
								.then(res => {
									if (res.data.content.length > 0) {
										this.setState({ date: res.data.content[0].dueDate, shift: res.data.content[0].shift })
										this.getData(res.data.content[0].dueDate, res.data.content[0].shift)
									}
								})
						}}
					>
						<FontAwesomeIcon icon={faMagnifyingGlass} />
					</button>
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.woid} onChange={event => this.setState({ filter: { ...this.state.filter, woid: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.item} onChange={event => this.setState({ filter: { ...this.state.filter, item: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.partNumber} onChange={event => this.setState({ filter: { ...this.state.filter, partNumber: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.description} onChange={event => this.setState({ filter: { ...this.state.filter, description: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.groupName} onChange={event => this.setState({ filter: { ...this.state.filter, groupName: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.designGroup} onChange={event => this.setState({ filter: { ...this.state.filter, designGroup: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.coverGroup} onChange={event => this.setState({ filter: { ...this.state.filter, coverGroup: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.partNumberStatus} onChange={event => this.setState({ filter: { ...this.state.filter, partNumberStatus: event.target.value === "" ? null : event.target.value } })} />
				</th>
				{/* <th className='table-elem-sm'>
					<input value={this.state.filter.qtyOpen} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, qtyOpen: event.target.value === "" ? null : parseInt(event.target.value) } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.qtyRejeter} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, qtyRejeter: event.target.value === "" ? null : parseInt(event.target.value) } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.qtyCompleted} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, qtyCompleted: event.target.value === "" ? null : parseInt(event.target.value) } })} />
				</th> */}
				<th className='table-elem-sm'>
					<input value={this.state.filter.statusDemande} onChange={event => this.setState({ filter: { ...this.state.filter, statusDemande: event.target.value === "" ? null : event.target.value } })} />
				</th>
				<th className='table-elem-sm'></th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.total} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, total: event.target.value === "" ? null : parseInt(event.target.value) } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.counter} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, counter: event.target.value === "" ? null : parseInt(event.target.value) } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filter.imported} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, imported: event.target.value === "" ? null : parseInt(event.target.value) } })} />
				</th>
				{/* Deactivation filter placeholder for ROLE_CAD */}
				{/* {user && user.roles.map(e => e.authority).includes("ROLE_CAD") && <th className='table-elem-sm'></th>} */}
			</tr>
		</thead>
	}

	returnColorStatus = (status) => {
		switch (status) {
			case "Waiting":
				return "#ffafaf"
			case "In progress":
				return "#f6ff6b"
			case "Complete":
				return "#7bff6b"
			case "Incomplete":
				return "#ffc46b"
			default:
				return ""
		}
	}

	renderRow(item, ind) {
		const { user } = this.props.security;
		let totalPlans = this.state.cuttingPlanList.filter(elem => elem.cuttingRequestPartNumbers && elem.cuttingRequestPartNumbers.map(elem => elem.wo).includes(item.wo)).length
		let gt = this.state.gammeTechniqueArr.find(e => e.partNumber == item.partNumber)
		const isDeactivated = item.deactivated === true
		const duplicateGroup = this.getDuplicateGroupForWorkOrder(item.wo)
		const isFuseCandidate = duplicateGroup !== null
		const remarks = this.getRemarkParts(item.remarqueDemande)
		return <tr key={"row-" + ind} className={"clickable-element"}
			style={
				isDeactivated ? { backgroundColor: '#ffdddd', textDecoration: 'line-through', opacity: 0.6 } :
				this.state.specialElems.map(e => e.wo).includes(item.wo) ? { backgroundColor: 'lightcyan' } : {}
			}
			onClick={() => {
				if(isDeactivated) return; // Prevent selection if deactivated
				if (!this.state.specialElems.map(e => e.wo).includes(item.wo)) {
					this.setState({ specialElems: [...this.state.specialElems, { ...item }] })
				} else {
					this.setState({ specialElems: [...this.state.specialElems.filter(e => e.wo !== item.wo)] })
				}
			}}
			onDoubleClick={() => {
				let cpInd = this.state.cuttingPlanList.findIndex(elem => elem.cuttingRequestPartNumbers.map(elem => elem.wo).includes(item.wo))
				if (cpInd !== -1) {
					this.setState({ importModalInd: cpInd })
					if (this.state.cuttingPlanList[cpInd].sequence) {
						this.setState({ cuttingRequest: this.state.cuttingPlanList[cpInd] })
					} else {
						axios.get(`/api/cuttingPlan/${this.state.cuttingPlanList[cpInd].cuttingPlanId}`)
							.then(res => {
								this.setState({ cuttingRequest: this.getCR(res.data, cpInd) })
							})
					}
				}
			}}

		>
				<td className='table-elem-sm'>
					<div style={{ display: 'flex', alignItems: 'center', justifyContent: "center" , gap: 8 }}>
						<span>{item.wo}</span>
						{remarks.length > 0 && (
							<span
								style={{
									display: 'inline-flex',
									alignItems: 'center',
									justifyContent: 'center',
									minWidth: 28,
									height: 28,
									padding: '0 8px',
									borderRadius: 999,
									backgroundColor: '#ffe082',
									color: '#6d5200',
									fontWeight: 700,
									fontSize: 12,
									boxShadow: 'inset 0 0 0 1px rgba(109,82,0,0.15)',
									cursor: 'default',
								}}
								onMouseEnter={(event) => this.showRemarkTooltip(event, item)}
								onMouseMove={(event) => this.showRemarkTooltip(event, item)}
								onMouseLeave={this.hideRemarkTooltip}
							>
								R{remarks.length}
							</span>
						)}
					</div>
				</td>
			<td className='table-elem-sm'>{item.woid}</td>
			<td className='table-elem-sm'>{item.item}</td>
			<td className='table-elem-sm' style={isFuseCandidate ? { backgroundColor: '#fff3a3' } : {}}>
				<div>{item.partNumber}</div>
				{isFuseCandidate && <small style={{ color: '#8a6d00', fontWeight: 600 }}>Fusion possible ({duplicateGroup.count})</small>}
			</td>
			<td className='table-elem-sm'>{item.description}</td>
			<td className='table-elem-sm'>{item.groupName}</td>
			<td className='table-elem-sm'>{item.designGroup}</td>
			<td className='table-elem-sm'>{item.coverGroup}</td>
			<td className='table-elem-sm'>{item.partNumberStatus}</td>
			{/* <td className='table-elem-sm'>{item.qtyOpen}</td>
			<td className='table-elem-sm'>{item.qtyRejeter}</td>
			<td className='table-elem-sm'>{item.qtyCompleted}</td> */}
			<td
				className='table-elem-sm'
				style={
					item.statusDemande === "O" ? { backgroundColor: "orange" } :
						item.statusDemande === "F" ? { backgroundColor: "#b3e0ff" } :
							item.statusDemande === "S" ? { backgroundColor: "yellow" } :
								item.statusDemande === "E" ? { backgroundColor: "#b3ffb3" } :
									item.statusDemande === "C" ? { backgroundColor: "#228B22", color: "white" } :
										{}
				}
			>
				{item.statusDemande}
			</td>
			<td  style={gt ? {backgroundColor: "#B8FDB6"} : {backgroundColor: "#FD3232"}}>
				{gt && gt.packaging}
			</td>
			<td className='table-elem-sm'>{item.total}</td>
			<td className='table-elem-sm' style={item.counter === item.total ? { backgroundColor: "#7bff6b" } : item.counter > 0 ? { backgroundColor: "#f6ff6b" } : { backgroundColor: "#ffafaf" }}>
				{item.counter}{totalPlans > 1 && ` (${totalPlans})`}
			</td>
			<td className='table-elem-sm' style={item.imported === item.total ? { backgroundColor: "#7bff6b" } : item.imported > 0 ? { backgroundColor: "#f6ff6b" } : { backgroundColor: "#ffafaf" }}>
				{item.imported}
			</td>
			{/* Deactivation toggle for ROLE_CAD */}
			{/* {user && user.roles.map(e => e.authority).includes("ROLE_CAD") && <td className='table-elem-sm'>
				<button 
					className={`btn btn-sm ${isDeactivated ? 'btn-success' : 'btn-warning'}`}
					onClick={(e) => {
						e.stopPropagation();
						axios.post(`/api/workOrder/${item.wo}/toggleDeactivation`)
							.then(res => {
								// Update the item in entriesList
								let updatedList = this.state.entriesList.map(entry => 
									entry.wo === item.wo ? { ...entry, deactivated: res.data.deactivated } : entry
								);
								this.setState({ entriesList: updatedList });
							})
							.catch(err => {
								console.error("Error toggling deactivation:", err);
							});
					}}
				>
					{isDeactivated ? 'Réactiver' : 'Désactiver'}
				</button>
			</td>} */}
		</tr>
	}

	/*
{ name: 'id', displayName: 'Id', type: "text", required: true, reg: /^[A-Za-z0-9-\u0020]*$/, hideForm: true },
			{ name: 'projet', displayName: 'Projet', type: "text" },
			{ name: 'version', displayName: 'Version', type: "text" },
			{ name: 'description', displayName: 'Description', type: "text" },
			{ name: 'version2', displayName: 'Version2', type: "text", hideTable: true },
			{ name: 'definition', displayName: 'Definition', type: "text" },
	*/

	renderCuttingPlanHeader = () => {
		return <thead className='entity-table-header' style={{ backgroundColor: "green" }}>
			<tr>
				<th className='table-elem-sm'>sequence</th>
				<th className='table-elem-sm'>Cutting Plan ID</th>
				<th className='table-elem-sm'>CMS ID</th>
				<th className='table-elem-sm'>Projet</th>
				<th className='table-elem-sm'>Version</th>
				<th className='table-elem-sm'>modele</th>
				<th className='table-elem-sm'>definition</th>
				<th className='table-elem-sm'>date</th>
				<th className='table-elem-sm'>createdBy</th>
			</tr>
			<tr>
				<th className='table-elem-sm'>
					<input style={{ width: 115 }}
						value={this.state.filterPC.sequence} type="text"
						onChange={event => this.setState({ filterPC: { ...this.state.filterPC, sequence: event.target.value != "" ? event.target.value : null } })}
						onKeyUp={event => {
							if (event.key === "Enter") {
								axios.get(`/api/cuttingRequestData/all?startWith.sequence=${event.target.value}&page=0&size=20&sort=createdAt,desc`)
									.then(res => {
										if (res.data.content.length > 0) {
											this.setState({ date: res.data.content[0].planningDate, shift: res.data.content[0].shift })
											this.getData(res.data.content[0].planningDate, res.data.content[0].shift)
										}
									})
							}
						}}
					/>
					<button className='btn btn-primary btn-sm'
						onClick={() => {
							axios.get(`/api/cuttingRequestData/all?startWith.sequence=${this.state.filterPC.sequence}&page=0&size=20&sort=createdAt,desc`)
								.then(res => {
									if (res.data.content.length > 0) {
										this.setState({ date: res.data.content[0].planningDate, shift: res.data.content[0].shift })
										this.getData(res.data.content[0].planningDate, res.data.content[0].shift)
									}
								})

						}}
					>
						<FontAwesomeIcon icon={faMagnifyingGlass} />
					</button>
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filterPC.cuttingPlanId} type="text" onChange={event => this.setState({ filterPC: { ...this.state.filterPC, cuttingPlanId: event.target.value != "" ? event.target.value : null } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filterPC.cmsId} type="text" onChange={event => this.setState({ filterPC: { ...this.state.filterPC, cmsId: event.target.value != "" ? event.target.value : null } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filterPC.projet} type="text" onChange={event => this.setState({ filterPC: { ...this.state.filterPC, projet: event.target.value != "" ? event.target.value : null } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filterPC.version} type="text" onChange={event => this.setState({ filterPC: { ...this.state.filterPC, version: event.target.value != "" ? event.target.value : null } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filterPC.modele} type="text" onChange={event => this.setState({ filterPC: { ...this.state.filterPC, modele: event.target.value != "" ? event.target.value : null } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filterPC.definition} type="text" onChange={event => this.setState({ filterPC: { ...this.state.filterPC, definition: event.target.value != "" ? event.target.value : null } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filterPC.date} type="text" onChange={event => this.setState({ filterPC: { ...this.state.filterPC, date: event.target.value != "" ? event.target.value : null } })} />
				</th>
				<th className='table-elem-sm'>
					<input value={this.state.filterPC.createdBy} type="text" onChange={event => this.setState({ filterPC: { ...this.state.filterPC, createdBy: event.target.value != "" ? event.target.value : null } })} />
				</th>
			</tr>
		</thead>

	}
	renderCuttingPlanRow = (item, ind) => {
		return <tr key={"row-" + ind} className={"clickable-element"}
			onDoubleClick={() => {
				this.setState({ importModalInd: ind })
				if (this.state.cuttingPlanList[ind].sequence) {
					this.setState({ cuttingRequest: this.state.cuttingPlanList[ind] })
				} else {
					axios.get(`/api/cuttingPlan/${this.state.cuttingPlanList[ind].cuttingPlanId}`)
						.then(res => {
							this.setState({ cuttingRequest: this.getCR(res.data, ind) })
						})
				}
			}}
		>
			<td className='table-elem-sm'>{item.sequence}</td>
			<td className='table-elem-sm' style={(this.state.cuttingPlanList && this.state.cuttingPlanList.filter(e => e.cuttingPlanId === item.cuttingPlanId).map(e => e.cuttingPlanId).length > 1) ? { backgroundColor: "yellow" } : {}}>
				{item.cuttingPlanId}
			</td>
			<td className='table-elem-sm' style={(this.state.cuttingPlanList && this.state.cuttingPlanList.filter(e => e.cuttingPlanId === item.cuttingPlanId).map(e => e.cuttingPlanId).length > 1) ? { backgroundColor: "yellow" } : {}}>
				{item.cmsId}
			</td>
			<td className='table-elem-sm'>{item.projet}</td>
			<td className='table-elem-sm'>{item.version}</td>
			<td className='table-elem-sm'>{item.modele}</td>
			<td className='table-elem-sm'>{item.definition}</td>
			<td className='table-elem-sm'>{item.planningDate}</td>
			<td className='table-elem-sm'>{item.createdBy ? `${item.createdBy.lastName} ${item.createdBy.firstName}` : ""}</td>
		</tr>
	}

	getSuggestions = () => {
		this.setState({ suggestions: null, toWorkLoading: true })
		const entriesList = Array.isArray(this.state.entriesList) ? this.state.entriesList : []
		console.log(entriesList)
		if (entriesList.length === 0) {
			this.setState({ toWorkLoading: false, suggestions: [] })
			return null;
		}

		const partNumbers = entriesList
			.filter(e => e.total > 0 && (e.imported == 0 || e.imported < e.total) && e.deactivated !== true)
			.map(e => e.partNumber)

		if (partNumbers.length === 0) {
			this.setState({ toWorkLoading: false, suggestions: [] })
			return null;
		}
		
		// Create AbortController for 60s timeout
		const controller = new AbortController()
		const timeoutId = setTimeout(() => {
			controller.abort()
		}, 60000) // 60 seconds
		
		axios.post(`/api/cuttingPlanPartNumberInfo/to-work`, partNumbers, {
			signal: controller.signal
		})
			.then(res => {
				clearTimeout(timeoutId)
				if (!this._isMounted) return
				this.setState({ toWorkLoading: false })
				// orgonize the suggestions by cutting plan
				let obj = {}
				res.data.map(elem => {
					if (obj[elem.cuttingPlan]) {
						obj[elem.cuttingPlan].push(elem)
					} else {
						obj[elem.cuttingPlan] = [elem]
					}
				})
				let arr = []
				for (let key in obj) {
					arr.push({ cuttingPlan: key, cmsId: res.data.find(e => e.cuttingPlan == key)?.cmsId, parts: obj[key] })
				}
				arr = arr.sort((a, b) => (
					Math.max(...b.parts.map(e => e.quantity)) - Math.max(...a.parts.map(e => e.quantity))
					|| b.parts.length - a.parts.length
					|| (b.parts.map(e => e.quantity || 0).reduce((a, b) => a + b)) - (a.parts.map(e => e.quantity).reduce((a, b) => a + b))
				))
				// indexing by partnumber 

				let entriesObj = {}
				let pnMap = {}
				entriesList.filter(e => e.deactivated !== true).map(e => {
					// if(!pnMap[e.partNumber] && (e.imported == 0 || e.imported < e.total)) {
					pnMap[e.partNumber] = e.wo
					// }
					entriesObj[e.wo] = e
					entriesObj[e.wo].counter = 0
					entriesObj[e.wo].imported = 0
				})
				//initializing the counter property
				let cuttingPlanList = (Array.isArray(this.state.cuttingPlanList) ? this.state.cuttingPlanList : []).filter(e => e.sequence != undefined && e.sequence != null)

				cuttingPlanList.map(cuttingPlan => {
					cuttingPlan.cuttingRequestPartNumbers.map(part => {
						if (entriesObj[part.wo]) {
							entriesObj[part.wo].imported += part.quantity
							entriesObj[part.wo].counter += part.quantity
						}
					})
				})

				// now we iterate through the list arr and we take the first suggestion and we add it to the entriesList
				// we also add the quantity we need to the counter property
				let goodCuttinPlan = []
				arr = arr.filter(suggestion => (
					suggestion.parts
						.map(part => part.partNumber)
						.filter(pn => !pnMap[pn] || !entriesObj[pnMap[pn]] || entriesObj[pnMap[pn]].total - entriesObj[pnMap[pn]].counter < pn.quantity).length == 0
				))
				arr.map(suggestion => {
					try {
						let good = true
						let numList = []
						suggestion.parts.map(part => {

							if (!pnMap[part.partNumber] || !entriesObj[pnMap[part.partNumber]] || entriesObj[pnMap[part.partNumber]].total - entriesObj[pnMap[part.partNumber]].counter < part.quantity) {
								good = false
							} else {
								numList.push(parseInt((entriesObj[pnMap[part.partNumber]].total - entriesObj[pnMap[part.partNumber]].counter) / part.quantity))
							}
						})
						if (good) {
							let num = Math.min(...numList, 2)
							suggestion.projet = entriesObj[pnMap[suggestion.parts[0].partNumber]].groupName
							suggestion.parts.map(part => {
								entriesObj[pnMap[part.partNumber]].counter += part.quantity * num
							})
							for (let i = 0; i < num; i++) {
								goodCuttinPlan.push(suggestion)
							}
						}
					} catch (err) {
						console.log(err)
					}
				})
				this.setState({
					cuttingPlanList: [...goodCuttinPlan.map(e => {
						return {
							cuttingPlanId: e.cuttingPlan,
							cmsId: e.cmsId,
							cuttingRequestPartNumbers: e.parts.map(pn => { return { ...pn, wo: entriesObj[pnMap[pn.partNumber]].wo, woid: e.cmsId } }),
							modele: e.parts.map(p => p.partNumber + `(${p.quantity})`).join("_"),
							projet: e.projet,
						}
					}), ...cuttingPlanList],
					entriesList: Object.values(entriesObj),
					suggestions: arr
				})
				// setTimeout(() => {
				// 	axios.post(`/api/cuttingPlan`)
				// })

			})
			.catch(err => {
				clearTimeout(timeoutId)
				if (!this._isMounted) return
				this.setState({ toWorkLoading: false })
				if (axios.isCancel(err) || err.name === 'AbortError' || err.code === 'ERR_CANCELED') {
					alert("La requête a pris plus de 60 secondes. Veuillez sélectionner un ensemble plus petit de partNumbers.")
				} else {
					console.error("Error in getSuggestions:", err)
				}
			})
			.finally(() => {
				this.getStat()
			})
	}

	loadPlacement = async (arr) => {
		const resTimingPlacement = await axios.post(`/api/query/timingPlacement`, arr)
		if (!this._isMounted) return
		let timingPlacementObj = this.state.timingPlacementObj
		resTimingPlacement.data.map(e => {
			timingPlacementObj[e.placementTimingModel] = e.validatedCuttingTimeTimingModel || e.cuttingTimeTimingModel
		})
		this.setState({timingPlacementObj })
	}

	getSuggestions2 = (entriesListArg = this.state.entriesList, cuttingPlanListArg = this.state.cuttingPlanList) => {
		this.setState({ suggestions: null, toWorkLoading: true })
		const entriesList = Array.isArray(entriesListArg) ? entriesListArg : []
		const cuttingPlanListInput = Array.isArray(cuttingPlanListArg) ? cuttingPlanListArg : []
		console.log(entriesList)
		if (entriesList.length === 0) {
			this.setState({ toWorkLoading: false, suggestions: [] })
			return
		}

		const partNumbers = entriesList
			.filter(e => e.total > 0 && (e.imported == 0 || e.imported < e.total) && e.deactivated !== true)
			.map(e => e.partNumber)

		if (partNumbers.length === 0) {
			this.setState({ toWorkLoading: false, suggestions: [] })
			return
		}
		
		// Create AbortController for 60s timeout
		const controller = new AbortController()
		const timeoutId = setTimeout(() => {
			controller.abort()
		}, 60000) // 60 seconds
		
		axios.post(`/api/cuttingPlanPartNumberInfo/to-work`, partNumbers, {
			signal: controller.signal
		})
			.then(res => {
				clearTimeout(timeoutId)
				if (!this._isMounted) return
				this.setState({ toWorkLoading: false })
				// orgonize the suggestions by cutting plan
				let obj = {}
				res.data.map(elem => {
					if (obj[elem.cuttingPlan]) {
						obj[elem.cuttingPlan].push(elem)
					} else {
						obj[elem.cuttingPlan] = [elem]
					}
				})
				let arr = []
				for (let key in obj) {
					arr.push({ cuttingPlan: key, cmsId: res.data.find(e => e.cuttingPlan == key)?.cmsId, parts: obj[key] })
				}
				arr = arr.sort((a, b) => (
					b.parts.length - a.parts.length
					|| Math.max(...b.parts.map(e => e.quantity)) - Math.max(...a.parts.map(e => e.quantity))
					|| (b.parts.map(e => e.quantity || 0).reduce((a, b) => a + b)) - (a.parts.map(e => e.quantity).reduce((a, b) => a + b))
				))
				// indexing by partnumber 

				let entriesObj = {}
				let pnMap = {}
				entriesList.filter(e => e.deactivated !== true).map(e => {
					entriesObj[e.wo] = e
					entriesObj[e.wo].counter = 0
					entriesObj[e.wo].imported = 0
					pnMap[e.partNumber] = e.wo
				})
				//initializing the counter property
				let cuttingPlanList = cuttingPlanListInput.filter(e => e.sequence != undefined && e.sequence != null)

				cuttingPlanList.map(cuttingPlan => {
					if (cuttingPlan.cuttingRequestPartNumbers) {
						cuttingPlan.cuttingRequestPartNumbers.map(part => {
							if (entriesObj[part.wo]) {
								entriesObj[part.wo].imported += part.quantity
								entriesObj[part.wo].counter += part.quantity
							}
						})
					}
				})

				// now we iterate through the list arr and we take the first suggestion and we add it to the entriesList
				// we also add the quantity we need to the counter property
				let goodCuttinPlan = []
				arr = arr.filter(suggestion => (
					suggestion.parts
						.map(part => part.partNumber)
						.filter(pn => !pnMap[pn] || !entriesObj[pnMap[pn]] || entriesObj[pnMap[pn]].total - entriesObj[pnMap[pn]].counter < pn.quantity).length == 0
				))
				arr.map(suggestion => {
					try {
						let good = true
						let numList = []
						suggestion.parts.map(part => {
							if (!pnMap[part.partNumber] || !entriesObj[pnMap[part.partNumber]] || entriesObj[pnMap[part.partNumber]].total - entriesObj[pnMap[part.partNumber]].counter < part.quantity) {
								good = false
							} else {
								numList.push(parseInt((entriesObj[pnMap[part.partNumber]].total - entriesObj[pnMap[part.partNumber]].counter) / part.quantity))
							}
						})
						if (good) {
							let num = Math.min(...numList, 2)
							suggestion.projet = entriesObj[pnMap[suggestion.parts[0].partNumber]].groupName
							suggestion.parts.map(part => {
								entriesObj[pnMap[part.partNumber]].counter += part.quantity * num
							})
							for (let i = 0; i < num; i++) {
								goodCuttinPlan.push(suggestion)
							}
						}
					} catch (err) {
						console.log(err)
					}
				})
				this.setState({
					cuttingPlanList: [...goodCuttinPlan.map(e => {
						return {
							cuttingPlanId: e.cuttingPlan,
							cmsId: e.cmsId,
							cuttingRequestPartNumbers: e.parts.map(pn => { return { ...pn, wo: entriesObj[pnMap[pn.partNumber]].wo, woid: e.cmsId } }),
							modele: e.parts.map(p => p.partNumber + `(${p.quantity})`).join("_"),
							projet: e.projet,
						}
					}), ...cuttingPlanList],
					entriesList: Object.values(entriesObj),
					suggestions: arr
				})
				// setTimeout(() => {
				// 	axios.post(`/api/cuttingPlan`)
				// })

			})
			.catch(err => {
				clearTimeout(timeoutId)
				if (!this._isMounted) return
				this.setState({ toWorkLoading: false })
				if (axios.isCancel(err) || err.name === 'AbortError' || err.code === 'ERR_CANCELED') {
					alert("La requête a pris plus de 60 secondes. Veuillez sélectionner un ensemble plus petit de partNumbers.")
				} else {
					console.error("Error in getSuggestions2:", err)
				}
			})
			.finally(() => {
				// this.getStat()
			})
	}

	getSuggestionSpec = () => {
		this.setState({ suggestions: null, toWorkLoading: true })
		const entriesList = Array.isArray(this.state.entriesList) ? this.state.entriesList : []
		console.log(entriesList)
		if (entriesList.length === 0) {
			this.setState({ toWorkLoading: false, suggestions: [] })
			return
		}
		
		// Create AbortController for 60s timeout
		const controller = new AbortController()
		const timeoutId = setTimeout(() => {
			controller.abort()
		}, 60000) // 60 seconds
		
		axios.post(`/api/cuttingPlanPartNumberInfo/to-work`, this.state.specialElems.map(e => e.partNumber), {
			signal: controller.signal
		})
			.then(res => {
				clearTimeout(timeoutId)
				if (!this._isMounted) return
				this.setState({ toWorkLoading: false })
				// orgonize the suggestions by cutting plan
				let obj = {}
				res.data.map(elem => {
					if (obj[elem.cuttingPlan]) {
						obj[elem.cuttingPlan].push(elem)
					} else {
						obj[elem.cuttingPlan] = [elem]
					}
				})
				let arr = []
				for (let key in obj) {
					arr.push({ cuttingPlan: key, cmsId: res.data.find(e => e.cuttingPlan == key)?.cmsId, parts: obj[key] })
				}
				arr = arr.sort((a, b) => (
					b.parts.length - a.parts.length
					|| Math.max(...b.parts.map(e => e.quantity)) - Math.max(...a.parts.map(e => e.quantity))
					|| (b.parts.map(e => e.quantity || 0).reduce((a, b) => a + b)) - (a.parts.map(e => e.quantity).reduce((a, b) => a + b))
				))
				// indexing by partnumber 

				let entriesObj = {}
				let pnMap = {}
				entriesList.filter(e => e.deactivated !== true).map(e => {
					entriesObj[e.wo] = e
					entriesObj[e.wo].counter = 0
					entriesObj[e.wo].imported = 0
					if (this.state.specialElems.map(elem => elem.wo).includes(e.wo)) {
						pnMap[e.partNumber] = e.wo
					}
				})
				//initializing the counter property
				let cuttingPlanList = (Array.isArray(this.state.cuttingPlanList) ? this.state.cuttingPlanList : []).filter(e => e.sequence != undefined && e.sequence != null)

				cuttingPlanList.map(cuttingPlan => {
					cuttingPlan.cuttingRequestPartNumbers.map(part => {
						entriesObj[part.wo].imported += part.quantity
						entriesObj[part.wo].counter += part.quantity
					})
				})

				// now we iterate through the list arr and we take the first suggestion and we add it to the entriesList
				// we also add the quantity we need to the counter property
				let goodCuttinPlan = []
				arr = arr.filter(suggestion => (
					suggestion.parts
						.map(part => part.partNumber)
						.filter(pn => !pnMap[pn] || !entriesObj[pnMap[pn]] || entriesObj[pnMap[pn]].total - entriesObj[pnMap[pn]].counter < pn.quantity).length == 0
				))
				arr.map(suggestion => {
					try {
						let good = true
						let numList = []
						suggestion.parts.map(part => {
							if (!pnMap[part.partNumber] || !entriesObj[pnMap[part.partNumber]] || entriesObj[pnMap[part.partNumber]].total - entriesObj[pnMap[part.partNumber]].counter < part.quantity) {
								good = false
							} else {
								numList.push(parseInt((entriesObj[pnMap[part.partNumber]].total - entriesObj[pnMap[part.partNumber]].counter) / part.quantity))
							}
						})
						if (good) {
							let num = Math.min(...numList, 2)
							suggestion.projet = entriesObj[pnMap[suggestion.parts[0].partNumber]].groupName
							suggestion.parts.map(part => {
								entriesObj[pnMap[part.partNumber]].counter += part.quantity * num
							})
							for (let i = 0; i < num; i++) {
								goodCuttinPlan.push(suggestion)
							}
						}
					} catch (err) {
						console.log(err)
					}
				})
				this.setState({
					cuttingPlanList: [...goodCuttinPlan.map(e => {
						return {
							cuttingPlanId: e.cuttingPlan,
							cmsId: e.cmsId,
							cuttingRequestPartNumbers: e.parts.map(pn => { return { ...pn, wo: entriesObj[pnMap[pn.partNumber]].wo, woid: e.cmsId } }),
							modele: e.parts.map(p => p.partNumber + `(${p.quantity})`).join("_"),
							projet: e.projet,
						}
					}), ...cuttingPlanList],
					entriesList: Object.values(entriesObj),
					suggestions: arr
				})
				// setTimeout(() => {
				// 	axios.post(`/api/cuttingPlan`)
				// })

			})
			.catch(err => {
				clearTimeout(timeoutId)
				this.setState({ toWorkLoading: false })
				if (axios.isCancel(err) || err.name === 'AbortError' || err.code === 'ERR_CANCELED') {
					alert("La requête a pris plus de 60 secondes. Veuillez sélectionner un ensemble plus petit de partNumbers.")
				} else {
					console.error("Error in getSuggestionSpec:", err)
				}
			})
	}

	getStat = () => {
		// this
	}

	getMarge = (cpmp, nbrCouche) => {
		let marge = 0
		let arr = cpmp.pliesConfigMarge.split("|").map(e => e.split(";").map(numb => parseFloat(numb))).sort((a, b) => a[0] - b[0])
		for (let i = 0; i < arr.length; i++) {
			if (nbrCouche >= arr[i][0]) {
				marge = arr[i][1]
			}
		}
		return marge
	}

	validatePartNumberMaterials = async (cp) => {
		let warnings = []
		if (cp && cp.cuttingPlanMaterials && cp.cuttingPlanMaterials.length > 0) {
			let partNumberMaterials = cp.cuttingPlanMaterials.map(cpm => cpm.partNumberMaterial).filter(p => p)
			
			try {
				// Fetch PartNumberMaterialConfig for all materials in one call
				const res = await axios.get(`/api/partNumberMaterialConfig/pns/${partNumberMaterials.join(",")}?projet=${cp.projet}`)
				let configMap = {}
				res.data.forEach(cfg => {
					configMap[cfg.partNumberMaterial] = cfg
				})
				
				// Check each material has a config
				partNumberMaterials.forEach(pnm => {
					if (!configMap[pnm]) {
						warnings.push(`PartNumberMaterialConfig not found for: ${pnm}`)
					}
				})
				
				// Cache the configs for later use
				this.setState({ materialConfigCache: configMap })
			} catch (err) {
				console.error("Error validating partNumberMaterials:", err)
				warnings.push("Error loading PartNumberMaterialConfig data")
			}
		}
		this.setState({ validationWarnings: warnings })
		return warnings
	}

	validatePartNumberBoom = async (cuttingRequestPartNumbers, partNumberMaterialListPlan) => {
		// Validate that partNumberMaterials from BOM match with cutting plan materials
		if (!cuttingRequestPartNumbers || cuttingRequestPartNumbers.length === 0) {
			this.setState({ partNumberBoomMismatch: false, partNumberBoomWarnings: [] })
			return true
		}

		try {
			// Build payload with partNumber and item pairs
			const payload = cuttingRequestPartNumbers
				.filter(pn => pn.partNumber)
				.map(pn => ({ partNumber: pn.partNumber, item: pn.item || '' }))
			
			if (payload.length === 0) {
				this.setState({ partNumberBoomMismatch: false, partNumberBoomWarnings: [] })
				return true
			}

			// Fetch partNumberMaterial for all partNumber/item pairs from BOM
			const res = await axios.post(`/api/partNumberBoom/byPartNumbersAndItems`, payload)
			
			// Response is a list of partNumberMaterial strings from BOM
			const partNumberMaterialsFromBom = res.data || []
			const partNumberMaterialSetFromBom = new Set(partNumberMaterialsFromBom.filter(m => m))
			
			// Convert plan materials to set for comparison
			const partNumberMaterialSetFromPlan = new Set((partNumberMaterialListPlan || []).filter(m => m))
			
			let warnings = []
			let hasMismatch = false
			
			// Check for materials in BOM but not in Plan (excess in BOM)
			const excessInBom = []
			partNumberMaterialSetFromBom.forEach(material => {
				if (!partNumberMaterialSetFromPlan.has(material)) {
					excessInBom.push(material)
				}
			})
			
			// Check for materials in Plan but not in BOM (missing in BOM)
			const missingInBom = []
			partNumberMaterialSetFromPlan.forEach(material => {
				if (!partNumberMaterialSetFromBom.has(material)) {
					missingInBom.push(material)
				}
			})
			
			// Check for missing partNumbers in BOM response (null or empty)
			const missingPartNumbers = []
			payload.forEach((p, idx) => {
				if (!partNumberMaterialsFromBom[idx]) {
					missingPartNumbers.push(`${p.partNumber} (item: ${p.item})`)
				}
			})
			
			// If there are any mismatches
			if (excessInBom.length > 0 || missingInBom.length > 0 || missingPartNumbers.length > 0) {
				hasMismatch = true
				
				if (missingPartNumbers.length > 0) {
					warnings.push(`PartNumberBoom non trouvé pour: ${missingPartNumbers.join(", ")}`)
				}
				
				if (excessInBom.length > 0) {
					warnings.push(`Matériaux dans BOM mais pas dans le plan: ${excessInBom.join(", ")}`)
				}
				
				if (missingInBom.length > 0) {
					warnings.push(`Matériaux dans le plan mais pas dans BOM: ${missingInBom.join(", ")}`)
				}
				
				// Show details of BOM response
				warnings.push(`Matériaux BOM: ${Array.from(partNumberMaterialSetFromBom).join(", ") || "(aucun)"}`)
				warnings.push(`Matériaux Plan: ${Array.from(partNumberMaterialSetFromPlan).join(", ") || "(aucun)"}`)
			}

			this.setState({ 
				partNumberBoomMismatch: hasMismatch, 
				partNumberBoomWarnings: warnings 
			})
			return !hasMismatch
		} catch (err) {
			console.error("Error validating partNumberBoom:", err)
			this.setState({ 
				partNumberBoomMismatch: true, 
				partNumberBoomWarnings: ["Erreur lors de la validation partNumberBoom"] 
			})
			return false
		}
	}

	sendNotificationToCAD = async () => {
		try {
			const cuttingRequest = this.state.cuttingRequest
			if (!cuttingRequest || !cuttingRequest.cuttingRequestPartNumbers) {
				alert("Aucune donnée à envoyer")
				return
			}

			const payload = {
				cuttingPlanId: cuttingRequest.cuttingPlanId,
				cmsId: cuttingRequest.cmsId,
				projet: cuttingRequest.projet,
				partNumbers: cuttingRequest.cuttingRequestPartNumbers.map(pn => pn.partNumber),
				warnings: this.state.partNumberBoomWarnings || []
			}

			await axios.post(`/api/notification/cad-material-mismatch`, payload)
			alert("Notification envoyée au CAD avec succès")
		} catch (err) {
			console.error("Error sending notification to CAD:", err)
			alert("Erreur lors de l'envoi de la notification au CAD")
		}
	}

	getCR = (cp, ind) => {
		// Validate partNumberMaterials asynchronously
		this.validatePartNumberMaterials(cp)
		
		
		
		let objCR = {
			cuttingPlanId: cp.id,
			cmsId: cp.cmsId,
			projet: cp.projet,
			version: cp.version,
			modele: cp.description,
			definition: cp.definition,
			shift: this.state.shift,
			planningDate: this.state.date,
			cuttingRequestPartNumbers: this.state.cuttingPlanList[ind].cuttingRequestPartNumbers,
			cuttingRequestSeries: [],
			planningList: [],

		}

		let arrPlacement = []

		if (cp && cp.cuttingPlanMaterials) {
			// try {
			cp.cuttingPlanMaterials.map(cpm => {
				cpm.cuttingPlanMaterialPlacement.sort((a, b) => a.partNumberMaterial - b.partNumberMaterial || Number(b.activated) - Number(a.activated)).map(cpmp => {
					let arrDrill = (cpmp.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
					let maxPlie = arrDrill[0] == null && arrDrill[1] == null ? cpmp.maxPlie : cpmp.maxPlieDrill
					let nbrCouche = cpmp.nbrCouche
					while (nbrCouche > 0) {
						let couche = Math.min(maxPlie, nbrCouche)
						let config = null;
						if (cpmp.pliesConfig != null) {
							let configArr = cpmp.pliesConfig.split("|").map(e => { return [parseInt(e.split(";")[0]), e.split(";")[1]] }).sort((a, b) => a[0] - b[0])
							for (let i = 0; i < configArr.length; i++) {
								if (configArr[i][0] <= cpmp.nbrCouche) {
									config = configArr[i][1]
								}
							}
						}

						// Calculate longueur: if material has plaque value, use it; otherwise use placement longueur + marge
						let longueurValue = cpm.plaque ? parseFloat(cpm.plaque.toFixed(3)) : parseFloat((cpmp.longueur + this.getMarge(cpmp, couche)).toFixed(3))

						// Get matelassageEndroit from partNumberMaterialConfig cache if available, otherwise from cuttingPlanMaterial
						let cachedConfig = this.state.materialConfigCache[cpm.partNumberMaterial]
						let matelassageEndroitValue = cachedConfig?.matelassageEndroit || cpm.matelassageEndroit

						let obj = {
							partNumberMaterial: cpm.partNumberMaterial,
							description: cpm.description,
							matelassageEndroit: matelassageEndroitValue,
							partNumbers: cpmp.partNumbers,
							longueur: longueurValue,
							nbrCouche: couche,
							laize: cpmp.laize,// en attent
							maxPlie: cpmp.maxPlie,
							maxPlieDrill: cpmp.maxPlieDrill,
							maxDrill: cpmp.maxDrill,
							placement: cpmp.placement,
							config: config,
							drill: arrDrill.join(","),
							machine: cpmp.machine,
							groupPlacement: cpmp.groupPlacement,
							activated: cpmp.activated,
							perimetre: cpmp.perimetre,
							tempsDeCoupe: this.state.timingPlacementObj[cpmp.placement] || cpmp.tempsDeCoupe,
							isPlaque: cpm.plaque ? true : false,
						}
						arrPlacement.push(obj)
						nbrCouche -= couche
					}
				})
			})
		}

		objCR.cuttingRequestSeries = arrPlacement

		if (this.state.optionsList && this.state.optionsList.projet != null && this.state.optionsList.projet.length > 0) {
			let projet = this.state.optionsList.projet.find(elem => elem.item.nom === objCR.projet)
			if (projet != null) {
				objCR.zone = { ...projet.item.zone }
			}
		}

		// Validate partNumberBoom for the cuttingRequestPartNumbers
		const cuttingRequestPartNumbers = this.state.cuttingPlanList[ind].cuttingRequestPartNumbers
		this.validatePartNumberBoom(cuttingRequestPartNumbers, cp.cuttingPlanMaterials.map(e=> e.partNumberMaterial));

		return objCR
	}

	renderGammeCMSModal = () => {
		// Modal for GammeCMSSequence with improved design and close button
		if (this.state.gammeCMSSequenceModal === null) {
			return <div></div>
		}
		return (
			<Modal
				show={this.state.gammeCMSSequenceModal !== null}
				onHide={() => this.setState({ gammeCMSSequenceModal: null })}
				dialogClassName="modal-90w"
				centered
			>
				<div style={{ position: "relative", height: "90vh", overflowY: 'auto', padding: 24 }}>
					<button
						type="button"
						className="close"
						aria-label="Close"
						style={{
							position: "fixed",
							top: 32,
							right: 20,
							zIndex: 1051,
							width: 44,
							height: 44,
							borderRadius: "50%",
							background: "#fff",
							boxShadow: "0 2px 8px rgba(0,0,0,0.18)",
							border: "none",
							color: "#333",
							cursor: "pointer",
							display: "flex",
							alignItems: "center",
							justifyContent: "center",
							fontSize: 28,
						}}
						onClick={() => this.setState({ gammeCMSSequenceModal: null })}
					>
						<span aria-hidden="true">&times;</span>
					</button>
					{this.state.gammeCMSSequenceModal && (
						<GammeCMS sequence={this.state.gammeCMSSequenceModal} />
					)}
				</div>
			</Modal>
		);
	}


	renderImportModal = () => {
		if (this.state.importModalInd === null) {
			return <div></div>
		}
		const { user } = this.props.security;
		let cuttingRequest = this.state.cuttingRequest
		const optionsList = this.state.optionsList || { zone: [], projet: [] }
		const entriesList = Array.isArray(this.state.entriesList) ? this.state.entriesList : []
		const fuseWarnings = this.getDuplicateGroupsForCuttingRequest(cuttingRequest)
		const splitCandidates = this.getSplitCandidatesForCuttingRequest(cuttingRequest, entriesList)
		const hasSplitCandidates = splitCandidates.length > 0
		console.log({ cuttingRequest })
		let counterPerPartnumber = {};
		if (cuttingRequest && cuttingRequest.cuttingRequestBoxs) {

			cuttingRequest.cuttingRequestBoxs = cuttingRequest.cuttingRequestBoxs.map(e => {
				if (counterPerPartnumber[e.partNumber]) {
					counterPerPartnumber[e.partNumber] = counterPerPartnumber[e.partNumber] + 1;
				} else {
					counterPerPartnumber[e.partNumber] = 1;
				}
				return {
					...e,
					counter: counterPerPartnumber[e.partNumber],
					total: cuttingRequest.cuttingRequestBoxs.filter(elem => elem.partNumber == e.partNumber && elem.nbrImpression && elem.nbrImpression.trim() === "1").length
				}
			})

		}
		return <Modal
			show={this.state.importModalInd !== null}
			onHide={() => this.setState({ importModalInd: null, selectedBoxs: [], cuttingRequest: null })}
			dialogClassName="modal-90w"
			centered
		>
			{this.state.importModalInd !== null && cuttingRequest && <div style={{ height: "75vh", overflowY: 'auto' }}>
				<h4 className='text-center my-2'>Importation du plan de coupe {this.state.importModalInd + 1}</h4>
				<div style={{ display: "flex" }}>
					{<button className='btn btn-link' disabled={this.state.importModalInd <= 0} onClick={() => {

						if (this.state.cuttingPlanList[this.state.importModalInd - 1].sequence) {
							this.setState({ cuttingRequest: this.state.cuttingPlanList[this.state.importModalInd - 1], selectedBoxs: [], importModalInd: this.state.importModalInd - 1 })
						} else {
							axios.get(`/api/cuttingPlan/${this.state.cuttingPlanList[this.state.importModalInd - 1].cuttingPlanId}`)
								.then(res => {
									this.setState({ cuttingRequest: this.getCR(res.data, this.state.importModalInd - 1), selectedBoxs: [], importModalInd: this.state.importModalInd - 1 })
								})
						}
					}}>
						<FontAwesomeIcon icon={faArrowLeft} />
					</button>}
					{<button className='btn btn-link' disabled={this.state.importModalInd >= this.state.cuttingPlanList.length - 1} onClick={() => {

						if (this.state.cuttingPlanList[this.state.importModalInd + 1].sequence) {
							this.setState({ cuttingRequest: this.state.cuttingPlanList[this.state.importModalInd + 1], selectedBoxs: [], importModalInd: this.state.importModalInd + 1 })
						} else {
							axios.get(`/api/cuttingPlan/${this.state.cuttingPlanList[this.state.importModalInd + 1].cuttingPlanId}`)
								.then(res => {
									this.setState({ cuttingRequest: this.getCR(res.data, this.state.importModalInd + 1), selectedBoxs: [], importModalInd: this.state.importModalInd + 1 })
								})
						}
					}}>
						<FontAwesomeIcon icon={faArrowRight} />
					</button>}


				</div>

				<div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Sequence : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.sequence}
							{cuttingRequest.sequence && <button className='btn btn-danger' onClick={() => {
								if (window.confirm("Voulez-vous supprimer cette sequence ?")) {
									axios.delete(`/api/cuttingRequest/${cuttingRequest.sequence}`)
										.then(res => {
											this.setState({ cuttingRequest: null, importModalInd: null, selectedBoxs: [], cuttingPlanList: this.state.cuttingPlanList.filter(elem => elem.cuttingPlanId !== cuttingRequest.cuttingPlanId) })
											setTimeout(() => {
												this.getSuggestions2()
											}, 200)
										})
								}
							}}><FontAwesomeIcon icon={faTrash} /></button>}
						</div>
					</div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Projet : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.projet}</div>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Version : </strong></div>
						<div className='' style={{ width: "35%" }}>{cuttingRequest.version}</div>
					</div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>modele : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.modele}</div>
					</div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>definition : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.definition}</div>
					</div>
					<div className='d-flex'>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Cutting Plan Id : </strong></div>
						<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.cuttingPlanId}</div>
						<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>CMS ID : </strong></div>
						<div className='' style={{ width: "35%" }}>{cuttingRequest.cmsId}</div>
					</div>
					<div className='d-flex'>
						<label className='col-form-label text-no-wrap text-right' style={{ width: "15%" }}><strong>Zone : </strong></label>
						<div className='text-no-wrap' style={{ width: "200px" }}>
							<Select classNamePrefix="rs"
								placeholder={"Zone..."} className='p-0'
								isClearable={true}
								value={(optionsList.zone && optionsList.zone.length > 0 && cuttingRequest.zone)
									? { label: cuttingRequest.zone.nom, value: cuttingRequest.zone.nom, item: cuttingRequest.zone }
									: null
								}
								options={optionsList.zone}
								onChange={(option) => {
									this.setState({ cuttingRequest: { ...cuttingRequest, zone: (option ? option.item : null) } })
								}}
							/>
						</div>
					</div>
					{/* Validation Warnings */}
					{this.state.validationWarnings && this.state.validationWarnings.length > 0 && (
						<div className='alert alert-warning mt-2 mb-0'>
							<strong>Validation Warnings:</strong>
							<ul className='mb-0 pl-3'>
								{this.state.validationWarnings.map((warning, idx) => (
									<li key={idx}>{warning}</li>
								))}
							</ul>
						</div>
					)}
					{/* PartNumberBoom Validation Warnings */}
					{this.state.partNumberBoomMismatch && this.state.partNumberBoomWarnings && this.state.partNumberBoomWarnings.length > 0 && (
						<div className='alert alert-danger mt-2 mb-0'>
							<strong>Erreur PartNumberBoom:</strong>
							<ul className='mb-0 pl-3'>
								{this.state.partNumberBoomWarnings.map((warning, idx) => (
									<li key={idx}>{warning}</li>
								))}
							</ul>
						</div>
					)}
					{fuseWarnings.length > 0 && !cuttingRequest.sequence && (
						<div className='alert alert-warning mt-2 mb-0'>
							<div><strong>Fusion possible avant importation.</strong></div>
							<div style={{ marginTop: 6 }}>
								{fuseWarnings.map(group => (
									<div key={group.partNumber}>
										{group.partNumber} : {group.count} WOs libres, total {group.totalQty}
									</div>
								))}
							</div>
							<button className='btn btn-warning btn-sm mt-2'
								onClick={() => this.setState({ showDuplicateDialog: true })}
							>
								Voir {fuseWarnings.length} fusion{fuseWarnings.length > 1 ? 's' : ''} possible{fuseWarnings.length > 1 ? 's' : ''}
							</button>
						</div>
					)}
					{hasSplitCandidates && !cuttingRequest.sequence && (
						<div className='alert mt-2 mb-0' style={{ backgroundColor: '#fff6cc', borderColor: '#f0d44c', color: '#6f5a00' }}>
							<div><strong>Division de WO détectée avant importation.</strong></div>
							<div style={{ marginTop: 6 }}>
								{splitCandidates.map(candidate => (
									<div key={candidate.wo}>
										<FontAwesomeIcon icon={faCut} style={{ marginRight: 6 }} />
										WO {candidate.wo} : {candidate.importedQty} / {candidate.totalQty}, reste {candidate.remainingQty}
									</div>
								))}
							</div>
						</div>
					)}
				</div>
				<div>
					<table className='table m-0 table table-grey-border'>
						<thead style={{ backgroundColor: "#ca0c0c", color: "white" }}>
							<tr>
								<th className='table-elem-sm'>Part Number <button style={{ padding: "0 12" }} className='btn btn-sm btn-primary'
									onClick={(event) => {
										if (cuttingRequest.cuttingRequestPartNumbers.length > 0 && this.state.selectedBoxs.length < cuttingRequest.cuttingRequestBoxs.length) {
											this.setState({ selectedBoxs: [...cuttingRequest.cuttingRequestBoxs.filter(e => e.nbrImpression == null || e.nbrImpression === "1").map(e => { return { ...e, sequence: (cuttingRequest.sequence || "") } })] })
										} else {
											this.setState({ selectedBoxs: [] })
										}
									}}
								>ALL</button></th>
								<th className='table-elem-sm'>Description</th>
								<th className='table-elem-sm'>Kit textil</th>
								<th className='table-elem-sm'>Quantité</th>
								<th className='table-elem-sm'>wo</th>
								<th className='table-elem-sm'>woid</th>
								<th className='table-elem-sm' style={{width : 100}}>Package Qty</th>
							</tr>
						</thead>
						<tbody>
							{cuttingRequest.cuttingRequestPartNumbers && cuttingRequest.cuttingRequestPartNumbers.map(elemPn => {
								let gt = this.state.gammeTechniqueArr.find(e => e.partNumber == elemPn.partNumber)
								const duplicateGroup = this.getDuplicateGroupForWorkOrder(elemPn.wo)
								const workOrderEntry = entriesList.find(e => e.wo === elemPn.wo)
								const splitLineCandidate = this.getSplitCandidateForPartNumber(elemPn, entriesList)

								return <tr
									className='clickable-element'
									onClick={() => this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber)
										? this.setState({ selectedBoxs: this.state.selectedBoxs.filter(e => e.partNumber != elemPn.partNumber) })
										: this.setState({ selectedBoxs: [...this.state.selectedBoxs, ...cuttingRequest.cuttingRequestBoxs.filter(e => e.partNumber == elemPn.partNumber)].sort((a, b) => a.id.localeCompare(b.id)).map(e => { return { ...e, sequence: (cuttingRequest.sequence || "") } }) })
									}
									style={this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber) ? { backgroundColor: "rgb(207, 207, 207)" } : {}}
								>
									<td className='table-elem-sm' style={duplicateGroup ? { backgroundColor: '#fff3a3' } : {}}>
										<div>{elemPn.partNumber}</div>
										{duplicateGroup && <small style={{ color: '#8a6d00', fontWeight: 600 }}>Fusion possible</small>}
									</td>
									<td className='table-elem-sm'>{elemPn.description}</td>
									<td className='table-elem-sm'>{elemPn.item}</td>
										<td className='table-elem-sm' style={splitLineCandidate ? { backgroundColor: '#fff3a3' } : {}}>
											<span>{elemPn.quantity}</span>
											{splitLineCandidate && (
												<small style={{ display: 'block', color: '#8a6d00', fontWeight: 600 }}>
													<FontAwesomeIcon icon={faCut} style={{ marginRight: 4 }} />
													Division possible
												</small>
											)}
										</td>
										<td className='table-elem-sm'>{elemPn.wo} ({workOrderEntry?.total})</td>
									<td className='table-elem-sm'>{elemPn.woid}</td>
									<td className='table-elem-sm' style={gt ? {backgroundColor: "#B8FDB6"} : {backgroundColor: "#FD3232"}}><strong>{gt && gt.packaging}</strong></td>
									{/* <td className='table-elem-sm' style={{ padding: "0 !important" }}>
										{cuttingRequest.sequence && elemPn.partNumber && <button className='btn btn-primary btn-sm'
											style={{ fontSize: "12", width: "100%", padding: "5 !important", margin: 0 }}
											onClick={(event) => {
												if (window.confirm("Voulez vous vraiment ajouter un box " + elemPn.partNumber + "?")) {
													axios.post(`/api/cuttingRequestV2/add-box?sequence=${cuttingRequest.sequence}&pn=${elemPn.partNumber}`)
														.then(res => {
															let cuttingPlanList = this.state.cuttingPlanList
															cuttingPlanList[this.state.importModalInd] = res.data
															this.setState({ cuttingRequest: res.data, cuttingPlanList: cuttingPlanList })
														})

												}
												event.stopPropagation();
											}}>
											<FontAwesomeIcon icon={faPlus} />
										</button>}</td> */}
								</tr>
							})}
						</tbody>
					</table>
				</div>
				<div>
					<table className='table m-0 table table-grey-border'>
						<thead style={{ backgroundColor: "#ca0c0c", color: "white" }}>
							<tr>
								<th className='table-elem-sm'>serie</th>
								<th className='table-elem-sm'>partNumberMaterial</th>
								<th className='table-elem-sm'>Description</th>
								<th className='table-elem-sm'>matelassageEndroit</th>
								<th className='table-elem-sm'>longueur</th>
								<th className='table-elem-sm'>nbrCouche</th>
								<th className='table-elem-sm'>laize</th>
								<th className='table-elem-sm'>placement</th>
								<th className='table-elem-sm'>config</th>
								<th className='table-elem-sm'>drill1</th>
								<th className='table-elem-sm'>drill2</th>
								<th className='table-elem-sm'>machine</th>
								<th className='table-elem-sm'>Temps De Coupe</th>
								<th className='table-elem-sm'>
									{cuttingRequest.cuttingRequestSeries && cuttingRequest.cuttingRequestSeries.filter(e => e.serie != null).length > 0 && <button className='btn btn-outline-light'
										onClick={() => {
											axios.post(`/api/cuttingRequest/printSerie`,
												[...cuttingRequest.cuttingRequestSeries.map(elemCRS => { return { ...elemCRS, cuttingRequest: cuttingRequest } })]
											)
										}}
										style={{
											padding: "2 5",
											fontSize: 12,
											marginLeft: 5
										}}
									>
										<FontAwesomeIcon icon={faPrint} />
									</button>}
								</th>
							</tr>
						</thead>
						<tbody>
							{cuttingRequest.cuttingRequestSeries && cuttingRequest.cuttingRequestSeries
								.sort((a, b) => a.partNumberMaterial.localeCompare(b.partNumberMaterial) || a.groupPlacement - b.groupPlacement || Number(b.activated) - Number(a.activated))
								.map((elemPn, indPn) => {
									let arrDrill = (elemPn.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
									return <tr style={elemPn.activated === false ? { backgroundColor: "#ffffc0" } : { backgroundColor: "" }}>
										<td className='table-elem-sm'>{elemPn.serie}</td>
										<td className='table-elem-sm'>{elemPn.partNumberMaterial}</td>
										<td className='table-elem-sm'>{elemPn.description}</td>
										<td className='table-elem-sm'>{elemPn.matelassageEndroit}</td>
										<td className='table-elem-sm'>{elemPn.longueur && elemPn.longueur.toFixed(3)}</td>
										<td className='table-elem-sm'>{elemPn.nbrCouche}</td>
										<td className='table-elem-sm'>{elemPn.laize}</td>
										<td className='table-elem-sm'>{elemPn.placement}</td>
										<td className='table-elem-sm'>{elemPn.config}</td>
										<td className='table-elem-sm'>{arrDrill[0]}</td>
										<td className='table-elem-sm'>{arrDrill[1]}</td>
										<td className='table-elem-sm'>{elemPn.machine}</td>
										<td className='table-elem-sm'>{this.state.timingPlacementObj[elemPn.placement] && <span>{this.state.timingPlacementObj[elemPn.placement] - this.state.timingPlacementObj[elemPn.placement] % 1} min {((this.state.timingPlacementObj[elemPn.placement] % 1) * 60).toFixed(0)} s</span>}</td>


										<td className='table-elem-sm'>
											{elemPn.serie && <button className='btn btn-outline-dark'
												onClick={() => {
													axios.post(`/api/cuttingRequest/printSerie`, [{ ...elemPn, cuttingRequest: cuttingRequest }])
												}}
												style={{
													padding: "2 5",
													fontSize: 12,
													marginLeft: 5
												}}
											>
												<FontAwesomeIcon icon={faPrint} />
											</button>}
											{!elemPn.activated && elemPn.serie == null && <button className='btn btn-sm btn-outline-dark' onClick={() => {
												let series = [...cuttingRequest.cuttingRequestSeries]
												let objInd = series.findIndex(e => (e.partNumberMaterial == elemPn.partNumberMaterial && e.groupPlacement == elemPn.groupPlacement && e.activated === true))
												if (objInd >= 0) {
													series[objInd].activated = false
												}
												series[indPn].activated = true
												cuttingRequest.cuttingRequestSeries = [...series]
												this.setState({ cuttingRequest: { ...cuttingRequest } })
											}}>
												<FontAwesomeIcon icon={faArrowUp} />
											</button>}
										</td>
									</tr>
								})}
						</tbody>
					</table>
				</div>
				<div>
					<table className='table m-0 table table-grey-border'>
						<thead style={{ backgroundColor: "#ca0c0c", color: "white" }}>
							<tr>
								<th className='table-elem-sm'>ID</th>
								<th className='table-elem-sm'>Part Number</th>
								<th className='table-elem-sm'>Description</th>
								<th className='table-elem-sm'>Kit textil</th>
								<th className='table-elem-sm'>Quantité</th>
								<th className='table-elem-sm'>wo</th>
								<th className='table-elem-sm'>woid</th>
							</tr>
						</thead>
						<tbody>
							{cuttingRequest.cuttingRequestBoxs && cuttingRequest.cuttingRequestBoxs.map(elemBox => <tr
								className='clickable-element'
								onClick={() => this.state.selectedBoxs.map(e => e.id).includes(elemBox.id)
									? this.setState({ selectedBoxs: this.state.selectedBoxs.filter(e => e.id != elemBox.id) })
									: this.setState({ selectedBoxs: [...this.state.selectedBoxs, { ...elemBox, sequence: (cuttingRequest.sequence || "") }].sort((a, b) => a.id.localeCompare(b.id)) })
								}
								style={this.state.selectedBoxs.map(e => e.id).includes(elemBox.id) ? { backgroundColor: "rgb(207, 207, 207)" } : {}}
							>
								<td className='table-elem-sm' style={(elemBox.nbrImpression && elemBox.nbrImpression.trim() !== "1") ? { backgroundColor: "#ff7979" } : {}}>{elemBox.id}</td>
								<td className='table-elem-sm'>{elemBox.partNumber}</td>
								<td className='table-elem-sm'>{elemBox.description}</td>
								<td className='table-elem-sm'>{elemBox.item}</td>
								<td className='table-elem-sm'>{elemBox.qtyBox}</td>
								<td className='table-elem-sm'>{elemBox.wo}</td>
								<td className='table-elem-sm'>{elemBox.woid}</td>
							</tr>)}
						</tbody>
					</table>
				</div>
				<div style={{ overflow: "hidden", height: 0 }}>
					<div className='' ref={elem => this.componentRef = elem}>
						{this.state.selectedBoxs.length > 0 && this.state.selectedBoxs.map(box => {
							let wd = 1565, hg = 1100
							return [<div style={{ height: wd }} key={"gamme-" + box.wo}><GammePn box={box} /></div>, <div className="page-break" />]
						})}
					</div>
				</div>
				<div style={{ overflow: "hidden", height: 0 }}>
					{cuttingRequest && this.renderPlanCoupe(cuttingRequest, user)}
				</div>
			</div>}
			<Modal.Footer>
				<div className='d-flex pb-2 ' style={{ justifyContent: "end" }}>
					<button className='btn btn-link' onClick={() => { this.setState({ importModalInd: null, selectedBoxs: [] }) }}> <FontAwesomeIcon icon={faArrowLeft} /> Retour</button>
					{/* <ReactToPrint
						onBeforeGetContent={() => {
							return new Promise((resolve, reject) => {
								this.setState({}, () => resolve())
							});
						}}
						onAfterPrint={() => { this.setState({ modalRotate: false }) }}
						onPrintError={() => { this.setState({ modalRotate: false }) }}
						trigger={() => <button
							type="button"
							className="btn btn-outline-success ml-1"

						><FontAwesomeIcon icon={faPrint} /> Imprimer Plan</button>}
						content={() => this.planPrintPage}
					/>
					<ReactToPrint
						onBeforeGetContent={() => {
							return new Promise((resolve, reject) => {
								this.setState({ modalRotate: true, selectedEmp: null }, () => resolve())
							});
						}}
						onAfterPrint={() => { this.setState({ modalRotate: false }) }}
						onPrintError={() => { this.setState({ modalRotate: false }) }}
						trigger={() => <button
							type="button"
							className="btn btn-outline-success ml-1"

						><FontAwesomeIcon icon={faPrint} /> Imprimer Gammes</button>}
						content={() => this.componentRef}
					/> */}
					{this.state.cuttingRequest && this.state.cuttingRequest.sequence && <button className='btn btn-outline-success ml-1'
						onClick={() => {
							this.setState({ gammeCMSSequenceModal: cuttingRequest.sequence })
						}}
					>
						<FontAwesomeIcon icon={faPrint} /> Interface d'impression
					</button>}
					{/* PartNumberBoom Mismatch Warning and CAD Notification Button */}
					{this.state.partNumberBoomMismatch && this.state.cuttingRequest && !this.state.cuttingRequest.sequence && (
						<button className='btn btn-warning float-right ml-2'
							onClick={() => this.sendNotificationToCAD()}
						>
							<FontAwesomeIcon icon={faEnvelope} /> Envoyer notification au CAD
						</button>
					)}
					{this.state.cuttingRequest && this.state.cuttingRequest.cuttingRequestSeries && !this.state.cuttingRequest.sequence && <button className='btn btn-success float-right ml-2'
						title={hasSplitCandidates ? `Cette action va diviser ${splitCandidates.length} WO puis enregistrer la séquence` : 'Enregistrer la séquence'}
						disabled={
							this.state.savingSequence
							 || this.state.cuttingRequest.cuttingRequestSeries.filter(elem => elem.activated === true).length === 0
							//   || this.state.partNumberBoomMismatch
							}
						onClick={() => {
							let objCR = { ...this.state.cuttingRequest }
							objCR.cuttingRequestSeries = [...objCR.cuttingRequestSeries.filter(elem => elem.activated === true)]
							const saveSplitCandidates = this.getSplitCandidatesForCuttingRequest(objCR, entriesList)
							if (saveSplitCandidates.length > 0) {
								const splitConfirmationMessage = saveSplitCandidates
									.map(candidate => `WO ${candidate.wo} : ${candidate.importedQty}/${candidate.totalQty} -> reste ${candidate.remainingQty}`)
									.join('\n')
								if (!window.confirm(`Les work orders suivantes vont être divisées avant l'enregistrement :\n${splitConfirmationMessage}\n\nVoulez-vous continuer ?`)) {
									return
								}
							}
							this.setState({ savingSequence: true })
							axios.post(`/api/cuttingRequest`, {
								...objCR,
								dueDate: this.state.date,
								dueShift: this.state.shift,
							})
								.then(res => {
									const splitInfos = Array.isArray(res.data.splitInfos) && res.data.splitInfos.length > 0
										? res.data.splitInfos
										: (res.data.splitInfo && res.data.splitInfo.split ? [res.data.splitInfo] : [])
									let cuttingPlanList = this.state.cuttingPlanList.map((elem, index) => {
										if (index === this.state.importModalInd) return { ...res.data, cmsId: elem.cmsId }
										return elem
									})
									let entriesObj = {}
									this.state.entriesList.map(e => {
										entriesObj[e.wo] = e
										entriesObj[e.wo].imported = 0
										entriesObj[e.wo].statusDemande = "O"
									})
									cuttingPlanList.map(e => {
										if (e.sequence) {
											e.cuttingRequestPartNumbers.map(elem => {
												if (entriesObj[elem.wo]) {
													entriesObj[elem.wo].imported += elem.quantity
												}
											})
										}
									})
									this.setState({
										cuttingRequest: res.data,
										selectedBoxs: [],
										importModalInd: splitInfos.length > 0 ? null : this.state.importModalInd,
										cuttingPlanList: cuttingPlanList,
										entriesList: Object.values(entriesObj),
										savingSequence: false,
									}, () => {
										if (splitInfos.length > 0) {
											const splitMessage = splitInfos
												.map(info => `WO ${info.newWo} créé avec ${info.remainingQty} unités restantes`)
												.join('\n')
											alert(`Division de work order effectuée:\n${splitMessage}`)
											this.getData(this.state.date, this.state.shift)
											return
										}
										// Reload statuses for all work orders after saving
										this.reloadStatuses();
									})
								})
								.catch(err => {
									console.log(err)
									this.setState({ savingSequence: false })
								})
						}}

					>
						{this.state.savingSequence
							? <span><FontAwesomeIcon icon={faSpinner} spin /> Enregistrement...</span>
							: <span><FontAwesomeIcon icon={hasSplitCandidates ? faCut : faSave} /> {hasSplitCandidates ? ' Diviser et enregistrer la séquence' : ' Enregistrer la séquence'}</span>
						}
					</button>}
				</div>
			</Modal.Footer>
		</Modal>
	}

	renderPlanCoupe = (cuttingRequest, user) => {
		let arrTable = [], reftissu = null, desc = null;
		cuttingRequest.cuttingRequestSeries.sort((a, b) => a.partNumberMaterial.localeCompare(b.partNumberMaterial)).map((elem, index) => {
			if (reftissu != null && elem.partNumberMaterial != reftissu) {
				arrTable.push(<tr>
					<td style={{ borderLeft: "0", borderBottom: "0" }}>Group By</td>
					<td colSpan={2} style={{ fontWeight: 'bold' }}>{reftissu}</td>
					<td colSpan={4}>{desc}</td>
					<td colSpan={3}>{cuttingRequest.cuttingRequestSeries.filter(e => e.partNumberMaterial === reftissu).map(e => (e.longueur ?? 0) * (e.nbrCouche ?? 0)).reduce((a, b) => a + b).toFixed(2)}</td>
				</tr>)
				arrTable.push(<br />)
			}
			let arrDrill = (elem.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
			arrTable.push(<tr>
				<td >{elem.serie}</td>
				<td>{elem.matelassageEndroit}</td>
				<td>{elem.longueur && elem.longueur.toFixed(3)}</td>
				<td>{elem.nbrCouche}</td>
				<td>{elem.laize}</td>
				<td className='ml-1' style={{ whiteSpace: "nowrap" }}>{elem.placement}</td>
				<td>{elem.config}</td>
				<td>{arrDrill[0]}</td>
				<td>{arrDrill[1]}</td>
				<td>{elem.machine}</td>
			</tr>)
			reftissu = elem.partNumberMaterial
			desc = elem.description
		})
		if (reftissu != null) {
			arrTable.push(<tr>
				<td style={{ borderLeft: "0", borderBottom: "0" }}>Group By</td>
				<td colSpan={2} style={{ fontWeight: 'bold' }}>{reftissu}</td>
				<td colSpan={4}>{desc}</td>
				<td colSpan={3}>{cuttingRequest.cuttingRequestSeries.filter(e => e.partNumberMaterial === reftissu).map(e => (e.longueur ?? 0) * (e.nbrCouche ?? 0)).reduce((a, b) => a + b)}</td>
			</tr>)
		}
		return <div className='' ref={elem => this.planPrintPage = elem} style={{ padding: 15 }}>
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
			<div style={{ fontSize: 16 }}>
				<div className='d-flex'>
					<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Sequence : </strong></div>
					<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.sequence}</div>
				</div>
				<div className='d-flex'>
					<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Projet : </strong></div>
					<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.projet}</div>
					<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Version : </strong></div>
					<div className='' style={{ width: "35%" }}>{cuttingRequest.version}</div>
				</div>
				<div className='d-flex'>
					<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Modele : </strong></div>
					<div className='' style={{ width: "85%" }}>{cuttingRequest.modele && cuttingRequest.modele.replaceAll("_", " ")}</div>
				</div>
				<div className='d-flex'>
					<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Definition : </strong></div>
					<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.definition}</div>
				</div>
				<div className='d-flex'>
					<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Cutting Plan Id : </strong></div>
					<div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.cuttingPlanId}</div>
					<div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>CMS ID : </strong></div>
					<div className='' style={{ width: "35%" }}>{cuttingRequest.cmsId}</div>
				</div>
				<div className='d-flex'>
					<label className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Zone : </strong></label>
					<div className='' style={{ width: "35%" }}>{cuttingRequest.zone?.nom}</div>
				</div>

			</div>
			<div className='mb-2'>
				<table className='table m-0 table table-grey-border entity-table-sm print-background'>
					<thead>
						<tr>
							<th className=''>Part Number</th>
							<th className=''>Description</th>
							<th className=''>Kit textil</th>
							<th className=''>Quantité</th>
							<th className=''>wo</th>
							<th className=''>woid</th>
							<th className=''>packageQty</th>

						</tr>
					</thead>
					<tbody>
						{cuttingRequest.cuttingRequestPartNumbers && cuttingRequest.cuttingRequestPartNumbers.map(elemPn => <tr
							className='clickable-element'
							onClick={() => this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber)
								? this.setState({ selectedBoxs: this.state.selectedBoxs.filter(e => e.partNumber != elemPn.partNumber) })
								: this.setState({ selectedBoxs: [...this.state.selectedBoxs, ...cuttingRequest.cuttingRequestBoxs.filter(e => e.partNumber == elemPn.partNumber)].sort((a, b) => a.id.localeCompare(b.id)) })
							}
							style={this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber) ? { backgroundColor: "rgb(207, 207, 207)" } : {}}
						>
							<td className=''>{elemPn.partNumber}</td>
							<td className=''>{elemPn.description}</td>
							<td className=''>{elemPn.item}</td>
							<td className=''>{elemPn.quantity}</td>
							<td className=''>{elemPn.wo}</td>
							<td className=''>{elemPn.woid}</td>
							<td className=''><strong>{elemPn.packageQty}</strong></td>

						</tr>)}
					</tbody>
				</table>
			</div>
			<div>
				<table className='table m-0 table table-grey-border entity-table-sm'>
					<thead>
						<tr style={{ backgroundColor: "black" }}>
							<th style={{ fontWeight: "bold", fontSize: 20 }} className='' colSpan={5}>Matelassage</th>
							<th style={{ fontWeight: "bold", fontSize: 20 }} className=' ml-1' colSpan={5} >Coupe</th>
						</tr>
						<tr>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Serie</th>
							{/* <th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Part Number Material</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Description</th> */}
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Sens</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Longueur</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Nbr Couche</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Laize</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=' ml-1'>Placement</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Config</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Drill1</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Drill2</th>
							<th style={{ fontWeight: "bold", fontSize: 16 }} className=''>Machine</th>
						</tr>
					</thead>
					<tbody>
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

	renderStats = () => {
		if (!this.state.cuttingPlanList || this.state.cuttingPlanList.length == 0) return null
		let arrRemovedTables = this.state.scheduleMachineArr.map(e => e.ligne)
		let totalMachineByType = {}
		this.state.productionTableArr
		.filter(e => e.pcCoupe !== 'NA')
		.map(obj => {
			if(!arrRemovedTables.includes(obj.nom)) {
				if(totalMachineByType[obj.machineType.name]) {
					totalMachineByType[obj.machineType.name] = totalMachineByType[obj.machineType.name] + 1;
				} else {
					totalMachineByType[obj.machineType.name] = 1
				}
			}
		})

		let counterPerMachineType = {}
		this.state.cuttingPlanList.filter(cp => cp.sequence != null).forEach(cp => {
			cp.cuttingRequestSeries.forEach(crs => {
				if (crs.tempsDeCoupe && crs.machine) {
					if (counterPerMachineType[crs.machine]) {
						counterPerMachineType[crs.machine] += this.state.timingPlacementObj[crs.placement]
					} else {
						counterPerMachineType[crs.machine] = this.state.timingPlacementObj[crs.placement]
					}
				}
			})
		})

		let str = []

		for(const type in counterPerMachineType) {
			str.push(`${type} (${totalMachineByType[type]}) : ${((counterPerMachineType[type] * 100) / (totalMachineByType[type]* 60 * 7.33)).toFixed(1)}%`)
		}


		// let num1 = 0, num2 = 0, num3 = 0
		// if (counterPerMachineType["Lectra"]) {
		// 	num1 += counterPerMachineType["Lectra"]
		// }
		// if (counterPerMachineType["Lectra IP6"]) {
		// 	num1 += counterPerMachineType["Lectra IP6"]
		// }
		// if (counterPerMachineType["Gerber"]) {
		// 	num1 += counterPerMachineType["Gerber"]
		// }
		// // if (num1 > 0) {
		// // 	str.push(`Lectra : ${((num1 * 100) / (22 * 60 * 7.33)).toFixed(1)}%`)
		// // }
		// if (counterPerMachineType["LASER-DXF"] > 0) {
		// 	str.push(`LASER-DXF : ${((counterPerMachineType["LASER-DXF"] * 100) / (60 * 7.33)).toFixed(1)}%`)
		// }
		// if (counterPerMachineType["LASER-LSR"] > 0) {
		// 	str.push(`LASER-LSR : ${((counterPerMachineType["LASER-LSR"] * 100) / (60 * 7.33)).toFixed(1)}%`)
		// }

		// if (counterPerMachineType["DIE"] > 0) {
		// 	num3 += counterPerMachineType["LASER-DXF"]
		// 	// str.push(`DIE : ${((counterPerMachineType["DIE"] * 100) / (60 * 7.33)).toFixed(1)}%`)
		// }

		//return a joined text with each machine next to its counter
		return str.join(" | ")
	}





	handleFuse = (woIds) => {
		if (window.confirm(`Fusionner ${woIds.length} Work Orders?\nLa quantité totale sera dans le dernier WO (${woIds[woIds.length-1]}).\nLes autres seront mis à quantité 0.`)) {
			this.setState({ fusingWos: true })
			axios.post('/api/workOrder/fuse', woIds)
				.then(res => {
					alert(`Fusionné avec succès!\nWO ${res.data.targetWo} = ${res.data.totalQty} unités\nWOs mis à 0: ${res.data.zeroedWos.join(', ')}`)
					this.getData(this.state.date, this.state.shift)
					this.setState({ showDuplicateDialog: false, fusingWos: false })
				})
				.catch(err => {
					alert('Erreur fusion: ' + (err.response?.data?.message || err.message))
					this.setState({ fusingWos: false })
				})
		}
	}

	renderDuplicateDialog = () => {
		if (!this.state.showDuplicateDialog) return null
		const duplicateCount = Array.isArray(this.state.duplicateGroups) ? this.state.duplicateGroups.length : 0
		return (
			<div className="modal show d-block" style={{backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 9999}}>
				<div className="modal-dialog modal-lg" style={{ maxWidth: 980 }}>
					<div className="modal-content" style={{ maxHeight: '88vh', display: 'flex', flexDirection: 'column' }}>
						<div className="modal-header" style={{backgroundColor: '#ffc107'}}>
							<h5 className="modal-title">Work Orders en Doublon Détectés ({duplicateCount} fusion{duplicateCount > 1 ? 's' : ''} possible{duplicateCount > 1 ? 's' : ''})</h5>
							<button type="button" className="close" onClick={() => this.setState({showDuplicateDialog: false})}>
								<span>&times;</span>
							</button>
						</div>
						<div className="modal-body" style={{ overflowY: 'auto' }}>
							<p>Les Part Numbers suivants apparaissent dans plusieurs Work Orders. Voulez-vous les fusionner?</p>
							{this.state.duplicateGroups.map((group, gi) => (
								<div key={gi} className="card mb-2">
									<div className="card-body">
										<h6>{group.partNumber} — {group.count} WOs, Total: {group.totalQty}</h6>
										<table className="table table-sm">
											<thead><tr><th>WO</th><th>Qty</th></tr></thead>
											<tbody>
												{group.workOrders.map(wo => (
													<tr key={wo.wo}>
														<td>{wo.wo}</td>
														<td>{wo.qtyOpen}</td>
													</tr>
												))}
											</tbody>
										</table>
										<button className="btn btn-warning btn-sm"
											disabled={this.state.fusingWos}
											onClick={() => this.handleFuse(group.workOrders.map(wo => wo.wo))}
										>
											Fusionner → dernière WO ({group.workOrders[group.workOrders.length-1].wo})
										</button>
									</div>
								</div>
							))}
						</div>
						<div className="modal-footer">
							<button className="btn btn-secondary" onClick={() => this.setState({showDuplicateDialog: false})}>
								Ignorer
							</button>
						</div>
					</div>
				</div>
			</div>
		)
	}

	render() {
		const { user } = this.props.security;
		const duplicateCount = Array.isArray(this.state.duplicateGroups) ? this.state.duplicateGroups.length : 0
		return (
			<div>
				<h1 className=''
					style={{ marginTop: 10, display: "flex", justifyContent: "center" }}>
					<span>Préparation Ordre de Fabrication</span>
					<button className='btn btn-outline-danger ml-2'
						disabled={this.state.viewedTable === "workOrder"}
						onClick={() => {
							this.setState({ viewedTable: "workOrder" })
						}}
					>
						WO
					</button>
					<button className='btn btn-outline-danger ml-2'
						disabled={this.state.viewedTable === "cuttingPlan"}
						onClick={() => {
							this.setState({ viewedTable: "cuttingPlan" })
						}}
					>
						Plan de coupe
					</button>
				</h1>
				<div className='d-flex align-items-center mb-1 mx-2'>
					<DatePicker
						id={"date"}
						name={"date"}
						placeholderText={"date"}
						className="form-control"
						autoComplete="off"
						selected={this.state.date ? moment(this.state.date, 'YYYY-MM-DD').toDate() : null}
						onChange={date => this.setState({ date: (date ? moment(date).format('YYYY-MM-DD') : null) })}
						isClearable={false}
						dateFormat={'yyyy-MM-dd'}
					/>
					{this.state.date && <div style={{ width: 100, margin: "0 8" }}>
						<Select classNamePrefix="rs"
							placeholder={"Shift..."} style={{ width: 100 }}
							isClearable={false}
							value={this.state.shift ? { label: this.state.shift, value: this.state.shift } : null}
							options={optionsShift}
							onChange={(option) => {
								this.setState({ shift: option ? option.value : null })
							}}
						/>
					</div>}
					<button className='btn btn-danger' onClick={() => {
						this.getData(this.state.date, this.state.shift)
					}}>
						<FontAwesomeIcon icon={faMagnifyingGlass} />
					</button>
					{/* add a refresh button and and when clicked it send a get request to /api/workOrder/refresh?date=${date}&shift=${shift}, and make the button disable and loading until it finished the it get data again */}
					<button 
						className='btn btn-warning ml-1' 
						disabled={this.state.loading}
						onClick={() => {
							this.refreshData(this.state.date, this.state.shift)
						}}
					>
						<FontAwesomeIcon icon={faSync} spin={this.state.loading} />
					</button>
					{/* <button className='btn btn-danger ml-1' style={{ padding: "2 10" }} onClick={() => {
						this.getSuggestions()
					}}>
						Combinaison 1
					</button> */}
					<button className='btn btn-danger ml-1 mr-1' style={{ padding: "2 10" }} onClick={() => {
						this.getSuggestions2()
					}}>
						Combinaison 2
					</button>
					{this.state.specialElems.length > 0 && <ButtonGroup variant="text" aria-label="text button group">
						<Button className='btn btn-primary btn-sm'
							onClick={() => {
								this.getSuggestionSpec()
							}}
						>
							{this.state.specialElems.length} éléments
						</Button>
						<Button className='btn btn-danger btn-sm'
							onClick={() => {
								this.setState({ specialElems: [] })
							}}
						>
							<FontAwesomeIcon icon={faTimes} />
						</Button>
					</ButtonGroup>}
					{user && (user.roles.map(e => e.authority).includes("ROLE_IMPORTER") || user.roles.map(e => e.authority).includes("ROLE_IMPORTATEUR")) && <button className='btn btn-success mr-1' style={{ padding: "2 10" }}
						onClick={() => {
							this.setState({ importModalInd: 0 })
							if (this.state.cuttingPlanList[0].sequence) {
								this.setState({ cuttingRequest: this.state.cuttingPlanList[0] })
							} else {
								axios.get(`/api/cuttingPlan/${this.state.cuttingPlanList[0].cuttingPlanId}`)
									.then(res => {
										this.setState({ cuttingRequest: this.getCR(res.data, 0) })
									})
							}
						}}
					>
						Importer
					</button>}
					{duplicateCount > 0 && <button className='btn btn-warning mr-1' style={{ padding: "2 10" }}
						onClick={() => this.setState({ showDuplicateDialog: true })}
					>
						{duplicateCount} fusion{duplicateCount > 1 ? 's' : ''} possible{duplicateCount > 1 ? 's' : ''}
					</button>}
					{this.renderStats()}
				</div>
				{this.state.viewedTable === "workOrder" && <div className='px-2'>
					<div className='table-responsive entity-table mb-2 slider-elem'>
						<table className='table table-bordered m-0'>
							{this.renderHeader()}
							<tbody>
								{this.state.entriesList == null ? <tr ><td colSpan={100}>Chargement...</td></tr> :
									this.state.entriesList.length === 0
										? <tr><td colSpan={100}>Aucune données disponibles</td> </tr>
										: this.state.entriesList
											.filter(elem => (
												(this.state.filter.wo == null || (elem.wo && elem.wo.toUpperCase().startsWith(this.state.filter.wo.toUpperCase()))) &&
												(this.state.filter.woid == null || (elem.woid && elem.woid.toUpperCase().startsWith(this.state.filter.woid.toUpperCase()))) &&
												(this.state.filter.item == null || (elem.item && elem.item.toUpperCase().startsWith(this.state.filter.item.toUpperCase()))) &&
												(this.state.filter.partNumber == null || (elem.partNumber && elem.partNumber.toUpperCase().startsWith(this.state.filter.partNumber.toUpperCase()))) &&
												(this.state.filter.description == null || (elem.description && elem.description.toUpperCase().includes(this.state.filter.description.toUpperCase()))) &&
												(this.state.filter.groupName == null || (elem.groupName && elem.groupName.toUpperCase().startsWith(this.state.filter.groupName.toUpperCase()))) &&
												(this.state.filter.designGroup == null || (elem.designGroup && elem.designGroup.toUpperCase().startsWith(this.state.filter.designGroup.toUpperCase()))) &&
												(this.state.filter.coverGroup == null || (elem.coverGroup && elem.coverGroup.toUpperCase().startsWith(this.state.filter.coverGroup.toUpperCase()))) &&
												(this.state.filter.partNumberStatus == null || (elem.partNumberStatus && elem.partNumberStatus.toUpperCase().startsWith(this.state.filter.partNumberStatus.toUpperCase()))) &&
												// (this.state.filter.qtyOpen == null || elem.qtyOpen === (this.state.filter.qtyOpen)) &&
												// (this.state.filter.qtyRejeter == null || elem.qtyRejeter === (this.state.filter.qtyRejeter)) &&
												// (this.state.filter.qtyCompleted == null || elem.qtyCompleted === (this.state.filter.qtyCompleted)) &&
												(this.state.filter.statusDemande == null || (elem.statusDemande && elem.statusDemande.toUpperCase().startsWith(this.state.filter.statusDemande.toUpperCase()))) &&
												(this.state.filter.counter == null || elem.counter === (this.state.filter.counter)) &&
												(this.state.filter.imported == null || elem.imported === (this.state.filter.imported))
											))
											//sort by groupName designGroup coverGroup
											.sort((a, b) => (a.groupName || "").localeCompare((b.groupName || ""))
												|| (a.designGroup || "").localeCompare((b.designGroup || ""))
												|| (a.coverGroup || "").localeCompare((b.coverGroup || "")))
											.map((item, ind) => {
												return this.renderRow(item, ind)
											})}
							</tbody>
						</table>
					</div>
				</div>}
				{this.state.viewedTable === "cuttingPlan" && <div className='px-2'>
					<div className='table-responsive entity-table mb-2 slider-elem'>
						<table className='table table-bordered m-0'>
							{this.renderCuttingPlanHeader()}
							<tbody>
								{this.state.cuttingPlanList == null ? <tr ><td colSpan={100}>Chargement...</td></tr> :
									this.state.cuttingPlanList.length === 0
										? <tr><td colSpan={100}>Aucune données disponibles</td> </tr>
										: this.state.cuttingPlanList
											.map((item, ind) => {
												if (
													(this.state.filterPC.sequence == null || (item.sequence && item.sequence.toString().toUpperCase().startsWith(this.state.filterPC.sequence.toUpperCase()))) &&
													(this.state.filterPC.cuttingPlanId == null || (item.cuttingPlanId && item.cuttingPlanId.toString().toUpperCase().startsWith(this.state.filterPC.cuttingPlanId.toUpperCase()))) &&
													(this.state.filterPC.cmsId == null || (item.cmsId && item.cmsId.toString().toUpperCase().startsWith(this.state.filterPC.cmsId.toUpperCase()))) &&
													(this.state.filterPC.projet == null || (item.projet && item.projet.toUpperCase().startsWith(this.state.filterPC.projet.toUpperCase()))) &&
													(this.state.filterPC.version == null || (item.version && item.version.toUpperCase().startsWith(this.state.filterPC.version.toUpperCase()))) &&
													(this.state.filterPC.modele == null || (item.modele && item.modele.toUpperCase().startsWith(this.state.filterPC.modele.toUpperCase()))) &&
													(this.state.filterPC.definition == null || (item.definition && item.definition.toUpperCase().startsWith(this.state.filterPC.definition.toUpperCase()))) &&
													(this.state.filterPC.planningDate == null || (item.planningDate && item.planningDate.toUpperCase().startsWith(this.state.filterPC.planningDate.toUpperCase()))) &&
													(this.state.filterPC.createdBy == null || (item.createdBy && item.createdBy.lastName.toUpperCase().startsWith(this.state.filterPC.createdBy.toUpperCase())))
												) {
													return this.renderCuttingPlanRow(item, ind)
												}
												return;
											})}
							</tbody>
						</table>
					</div>
				</div>}
				{this.renderImportModal()}
				{this.renderGammeCMSModal()}
				{this.renderDuplicateDialog()}
				{this.renderRemarkTooltip()}
			</div>
		)
	}
}

ImportationNew.propTypes = {
	security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
	security: state.security
})

export default connect(mapStateToProps, {})(ImportationNew);
