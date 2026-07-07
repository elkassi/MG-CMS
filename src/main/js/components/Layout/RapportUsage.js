import React, { Component } from 'react'
import axios from 'axios';
import Select from "react-select";
import moment from 'moment';
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCheck, faFileExcel, faMagnifyingGlass, faTimes } from '@fortawesome/free-solid-svg-icons';
import { Modal } from 'react-bootstrap';
import ExcelJS from 'exceljs';
import { saveAs } from 'file-saver';
import SortIcon from '../utils/SortIcon';


const shiftOptions = [
	{ value: 1, label: "shift 1" },
	{ value: 2, label: "shift 2" },
	{ value: 3, label: "shift 3" },
]

export default class RapportUsage extends Component {

	constructor() {
		super();
		this.state = {
			list: [],
			date: moment().add(2, 'hours').add(10, 'minutes').format("YYYY-MM-DD"),
			shift: this.getShift(moment().add(2, 'hours').add(10, 'minutes')),
			reftissu: "",
			filter: {},
			sortProp: "date",
			sortDirec: "asc",
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

	}

	exportExcel = () => {
		let data = this.state.list.map(e => {
			return {
				// put the keys as labels of the header
				"Cutting Plan Id": e.cuttingPlanId,
				"Sequence": e.cuttingRequest_sequence,
				"Debut Matelassage": e.dateDebutMatelassage,
				"Fin Matelassage": e.dateFinMatelassage,
				"Debut Coupe": e.dateDebutCoupe,
				"Fin Coupe": e.dateFinCoupe,
				"Reftissu": e.confirmReftissu,
				"Description": e.description,
				"Consommation Plan": e.totalConsommationPlan,
				"Overlap": e.overlap,
				"Non Utitlse": e.nonUtitlse,
				"Defaut": e.defaut,
				"Total Usage": e.totalUsage,
				"Excess": e.excess,
				"Final Usage": e.finalUsage,
				"Qad Usage": e.qadUsage,
				"Variance": e.variance,
			}
		})
		// Create workbook and worksheet
		let workbook = new ExcelJS.Workbook();
		let worksheet = workbook.addWorksheet("Table");

		// Add header row
		let xlsHeader = Object.keys(data[0]);
		let headerRow = worksheet.addRow(xlsHeader);

		// Style the header row
		headerRow.eachCell((cell) => {
			cell.fill = {
				type: 'pattern',
				pattern: 'solid',
				fgColor: { argb: 'FFBF3030' }, // Dark blue background
			};
			cell.font = {
				bold: true,
				color: { argb: 'FFFFFFFF' }, // White text color
			};
			cell.alignment = { horizontal: 'center' };
		});

		// Add data rows
		data.forEach(item => {
			worksheet.addRow(Object.values(item));
		});

		worksheet.columns = xlsHeader.map((header, i) => {
			const colLength = Math.max(
				header.length, // Header length
				...data.map(item => item[header]?.toString().length || 10) // Maximum data length in the column
			);
			return { width: colLength + 2 }; // Add some padding to the calculated length
		});

		workbook.xlsx.writeBuffer().then((buffer) => {
			const blob = new Blob([buffer], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
			saveAs(blob, "Table.xlsx");
		});


	}

	search = async () => {
		this.setState({ list: null })
		let dep  = "date=" + this.state.date ;
		if(this.state.shift){
			dep += "&shift=" + this.state.shift;
		}
		if(this.state.reftissu){
			dep += "&reftissu=" + this.state.reftissu;
		}
		try{
			let res = await axios.get(`/api/query/rapportUsage?${dep}`)
			this.setState({ list: res.data })
			let arrSequence = [], arrItem = []
			let itemCache = {}
			res.data.map(e => {
				if(arrSequence.includes(e.cuttingRequest_sequence)) return
				arrSequence.push(e.cuttingRequest_sequence)
			})
			let resCrpn = await axios.get(`/api/cuttingRequestPartNumberData/bySequences/${arrSequence.join(",")}`)
			let arrCrpn = resCrpn.data
			arrCrpn.map(e => {
				if(arrItem.includes(e.item)) return
				arrItem.push(e.item)
			})
			let resBOM = await axios.get(`/api/partNumberBoom/items/${arrItem.join(",")}`)
			let arrBOM = resBOM.data
			res.data = res.data.map(line => {
				let qadUsage = 0 
				let objKits = {}
				let partnumbresList = arrCrpn.filter(e => e.cuttingRequest === line.cuttingRequest_sequence)
				partnumbresList.map(e => {
					objKits[e.item] = e.quantity
				})
				let bomList = arrBOM.filter(e => partnumbresList.map(e => e.item).includes(e.item) && line.confirmReftissu === e.partNumberMaterial)
				bomList.map(e => {
					if(objKits[e.item] != null) {
						qadUsage += e.quantityPer * objKits[e.item] * 1.03
					}
				})
				line.qadUsage = this.convertTwoDigit(qadUsage, 3)
				line.variance = this.convertTwoDigit(line.finalUsage - qadUsage, 3)
				return line
			})

			this.setState({ list: res.data })

			// Persist this date/shift's comparison so it accumulates in the
			// /rapportUsageReport entity page (incremental - no heavy all-dates rebuild).
			axios.post('/api/rapportUsageReport/save', res.data)
				.catch(saveErr => console.log('rapportUsageReport save failed', saveErr))

		} catch (err) {
			this.setState({ list: [] })
			console.log(err)
		}
	}

	convertTwoDigit = (num, i) => {
		return parseFloat(num.toFixed(i));
	}
	

	renderHeader = () => {
		return <div className='d-flex align-items-center mb-1 mx-2 justify-content-center'>
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

			<div style={{ width: 150, margin: "0 8" }}>
				<Select classNamePrefix="rs"
					placeholder={"shift"} style={{ width: 150 }}
					isClearable={true}
					value={this.state.shift ? shiftOptions.find(e => e.value === this.state.shift) : null}
					options={shiftOptions}
					onChange={(option) => {
						if (option) {
							this.setState({ shift: option.value })
						} else {
							this.setState({ shift: null })
						}
					}}
				/>
			</div>
			<div style={{ width: 180, margin: "0 8" }}>
				<input type="text" className="form-control" placeholder="Reftissu" style={{ height: 30 }}
					value={this.state.reftissu} onChange={e => this.setState({ reftissu: e.target.value })} />
			</div>
			<button className='btn btn-danger' onClick={() => { this.search() }} style={{ padding: "9 16", margin: "0 8" }}>
				<FontAwesomeIcon icon={faMagnifyingGlass} />
			</button>
			<button className='btn btn-success' onClick={() => {
				this.exportExcel()
			}} style={{ padding: "9 16", margin: "0 8" }}>
				<FontAwesomeIcon icon={faFileExcel} />
			</button>
		</div>
	}

	renderTable = () => {
		/*
				private Long cuttingPlanId;
		private String cuttingRequest_sequence;
		private String dateDebutMatelassage;
		private String dateFinMatelassage;
		private String dateDebutCoupe;
		private String dateFinCoupe;
		private String confirmReftissu;
		private String description;
		private Double totalConsommationPlan;
		private Double overlap;
		private Double nonUtitlse;
		private Double defaut;
		private Double totalUsage;
		private Double excess;
		private Double finalUsage;
		private Double qadUsage;
		private Double variance;

		*/
		return <div className='table-responsive' style={{ maxHeight: "calc(100% - 200px)" }}>
			<table className='table table-bordered m-0 table-cells-sm' style={{ fontSize: 12 }}>
				<thead style={{ position: "sticky", top: -1, backgroundColor: "#bf3030", color: "white" }}>
					<tr>
						<th onClick={() => this.sortChanged("cuttingPlanId")}>Cutting Plan Id <SortIcon currentSort="cuttingPlanId" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("cuttingRequest_sequence")}>Sequence <SortIcon currentSort="cuttingRequest_sequence" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("dateDebutMatelassage")}>Debut Matelassage <SortIcon currentSort="dateDebutMatelassage" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("dateFinMatelassage")}>Fin Matelassage <SortIcon currentSort="dateFinMatelassage" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("dateDebutCoupe")}>Debut Coupe <SortIcon currentSort="dateDebutCoupe" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("dateFinCoupe")}>Fin Coupe <SortIcon currentSort="dateFinCoupe" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th style={{minWidth: 175}} onClick={() => this.sortChanged("confirmReftissu")}>Reftissu <SortIcon currentSort="confirmReftissu" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th style={{minWidth: 350}} onClick={() => this.sortChanged("description")}>Description <SortIcon currentSort="description" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("totalConsommationPlan")}>Consommation Plan <SortIcon currentSort="totalConsommationPlan" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("overlap")}>Overlap <SortIcon currentSort="overlap" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("nonUtitlse")}>Non Utitlse <SortIcon currentSort="nonUtitlse" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("defaut")}>Defaut <SortIcon currentSort="defaut" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("totalUsage")}>Total Usage <SortIcon currentSort="totalUsage" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("excess")}>Excess <SortIcon currentSort="excess" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("finalUsage")}>Final Usage <SortIcon currentSort="finalUsage" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("qadUsage")}>Qad Usage <SortIcon currentSort="qadUsage" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("variance")}>Variance <SortIcon currentSort="variance" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
					</tr>
					<tr className=''>
						<th className='table-elem-sm'><input value={this.state.filter.cuttingPlanId} type="number" onChange={(e) => { this.setState({ filter: { ...this.state.filter, cuttingPlanId: e.target.value === "" ? null : parseInt(e.target.value) } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.cuttingRequest_sequence} onChange={(e) => { this.setState({ filter: { ...this.state.filter, cuttingRequest_sequence: e.target.value } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.dateDebutMatelassage} onChange={(e) => { this.setState({ filter: { ...this.state.filter, dateDebutMatelassage: e.target.value } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.dateFinMatelassage} onChange={(e) => { this.setState({ filter: { ...this.state.filter, dateFinMatelassage: e.target.value } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.dateDebutCoupe} onChange={(e) => { this.setState({ filter: { ...this.state.filter, dateDebutCoupe: e.target.value } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.dateFinCoupe} onChange={(e) => { this.setState({ filter: { ...this.state.filter, dateFinCoupe: e.target.value } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.confirmReftissu} onChange={(e) => { this.setState({ filter: { ...this.state.filter, confirmReftissu: e.target.value } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.description} onChange={(e) => { this.setState({ filter: { ...this.state.filter, description: e.target.value } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.totalConsommationPlan} type="number" onChange={(e) => { this.setState({ filter: { ...this.state.filter, totalConsommationPlan: e.target.value === "" ? null : parseFloat(e.target.value) } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.overlap} type="number" onChange={(e) => { this.setState({ filter: { ...this.state.filter, overlap: e.target.value === "" ? null : parseFloat(e.target.value) } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.nonUtitlse} type="number" onChange={(e) => { this.setState({ filter: { ...this.state.filter, nonUtitlse: e.target.value === "" ? null : parseFloat(e.target.value) } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.defaut} type="number" onChange={(e) => { this.setState({ filter: { ...this.state.filter, defaut: e.target.value === "" ? null : parseFloat(e.target.value) } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.totalUsage} type="number" onChange={(e) => { this.setState({ filter: { ...this.state.filter, totalUsage: e.target.value === "" ? null : parseFloat(e.target.value) } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.excess} type="number" onChange={(e) => { this.setState({ filter: { ...this.state.filter, excess: e.target.value === "" ? null : parseFloat(e.target.value) } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.finalUsage} type="number" onChange={(e) => { this.setState({ filter: { ...this.state.filter, finalUsage: e.target.value === "" ? null : parseFloat(e.target.value) } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.qadUsage} type="number" onChange={(e) => { this.setState({ filter: { ...this.state.filter, qadUsage: e.target.value === "" ? null : parseFloat(e.target.value) } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.variance} type="number" onChange={(e) => { this.setState({ filter: { ...this.state.filter, variance: e.target.value === "" ? null : parseFloat(e.target.value) } }) }} /></th>
					</tr>
				</thead>
				<tbody>
					{this.state.list == null ? <tr ><td colSpan={100}>Chargement...</td></tr> :
						this.state.list.length === 0
							? <tr><td colSpan={100}>Aucune données disponibles</td> </tr>
							: this.state.list
								.filter(elem => (
									(this.state.filter.cuttingPlanId == null || elem.cuttingPlanId === (this.state.filter.cuttingPlanId)) &&
									(this.state.filter.cuttingRequest_sequence == null || elem.cuttingRequest_sequence?.toUpperCase().startsWith(this.state.filter.cuttingRequest_sequence.toUpperCase())) &&
									(this.state.filter.dateDebutMatelassage == null || elem.dateDebutMatelassage?.toUpperCase().startsWith(this.state.filter.dateDebutMatelassage.toUpperCase())) &&
									(this.state.filter.dateFinMatelassage == null || elem.dateFinMatelassage?.toUpperCase().startsWith(this.state.filter.dateFinMatelassage.toUpperCase())) &&
									(this.state.filter.dateDebutCoupe == null || elem.dateDebutCoupe?.toUpperCase().startsWith(this.state.filter.dateDebutCoupe.toUpperCase())) &&
									(this.state.filter.dateFinCoupe == null || elem.dateFinCoupe?.toUpperCase().startsWith(this.state.filter.dateFinCoupe.toUpperCase())) &&
									(this.state.filter.confirmReftissu == null || elem.confirmReftissu?.toUpperCase().startsWith(this.state.filter.confirmReftissu.toUpperCase())) &&
									(this.state.filter.description == null || elem.description?.toUpperCase().startsWith(this.state.filter.description.toUpperCase())) &&
									(this.state.filter.totalConsommationPlan == null || elem.totalConsommationPlan === (this.state.filter.totalConsommationPlan)) &&
									(this.state.filter.overlap == null || elem.overlap === (this.state.filter.overlap)) &&
									(this.state.filter.nonUtitlse == null || elem.nonUtitlse === (this.state.filter.nonUtitlse)) &&
									(this.state.filter.defaut == null || elem.defaut === (this.state.filter.defaut)) &&
									(this.state.filter.totalUsage == null || elem.totalUsage === (this.state.filter.totalUsage)) &&
									(this.state.filter.excess == null || elem.excess === (this.state.filter.excess)) &&
									(this.state.filter.finalUsage == null || elem.finalUsage === (this.state.filter.finalUsage)) &&
									(this.state.filter.qadUsage == null || elem.qadUsage === (this.state.filter.qadUsage)) &&
									(this.state.filter.variance == null || elem.variance === (this.state.filter.variance))
								))
								.sort((a, b) => {
									if (this.state.sortProp && this.state.sortDirec) {
										if (["cuttingRequest_sequence", "dateDebutMatelassage", "dateFinMatelassage", "dateDebutCoupe", "dateFinCoupe", "confirmReftissu", "description"].includes(this.state.sortProp)) {
											if (this.state.sortDirec == "asc") {
												return (a[this.state.sortProp] || "").localeCompare((b[this.state.sortProp] || ""))
											} else {
												return (b[this.state.sortProp] || "").localeCompare((a[this.state.sortProp] || ""))
											}
										} else {
											if (this.state.sortDirec == "asc") {
												return a[this.state.sortProp] - b[this.state.sortProp]
											} else {
												return b[this.state.sortProp] - a[this.state.sortProp]
											}
										}
									}
									return a.cuttingRequest_sequence.localeCompare(b.cuttingRequest_sequence) || a.confirmReftissu.localeCompare(b.confirmReftissu)
								})
								.map((item, ind) => {
									return this.renderRow(item, ind)
								})
					}
				</tbody>
			</table>
		</div>
	}

	sortChanged(field) {
		let sortProp = field;
		let propChanged = this.state.sortProp !== sortProp;
		let sortDirec = propChanged ? 'asc' : this.state.sortDirec === 'asc' ? 'desc' : 'asc';

		this.setState({ sortProp, sortDirec });
	}



	renderRow(item, ind) {
		return <tr key={"row-" + ind}
		   style={item.statusMatelassage.startsWith("Incom") ? { backgroundColor : "orange"} : {}}
		>
			<td style={{whiteSpace: "nowrap"}}>{item.cuttingPlanId}</td>
			<td style={{whiteSpace: "nowrap"}}>{item.cuttingRequest_sequence}</td>
			<td style={{whiteSpace: "nowrap"}} >{item.dateDebutMatelassage}</td>
			<td style={{whiteSpace: "nowrap"}} >{item.dateFinMatelassage}</td>
			<td style={{whiteSpace: "nowrap"}} >{item.dateDebutCoupe}</td>
			<td style={{whiteSpace: "nowrap"}} >{item.dateFinCoupe}</td>
						{/* <td dangerouslySetInnerHTML={{ __html: item.dateDebutMatelassage?.replace(" ", "<br>") }}></td>
			<td dangerouslySetInnerHTML={{ __html: item.dateFinMatelassage?.replace(" ", "<br>") }}></td>
			<td dangerouslySetInnerHTML={{ __html: item.dateDebutCoupe?.replace(" ", "<br>") }}></td>
			<td dangerouslySetInnerHTML={{ __html: item.dateFinCoupe?.replace(" ", "<br>") }}></td> */}

			<td style={{whiteSpace: "nowrap"}}>{item.confirmReftissu}</td>
			<td style={{whiteSpace: "nowrap"}}>{item.description}</td>
			<td style={{whiteSpace: "nowrap"}}>{item.totalConsommationPlan}</td>
			<td style={{whiteSpace: "nowrap"}}>{item.overlap}</td>
			<td style={{whiteSpace: "nowrap"}}>{item.nonUtitlse}</td>
			<td style={{whiteSpace: "nowrap"}}>{item.defaut}</td>
			<td style={{whiteSpace: "nowrap"}}>{item.totalUsage}</td>
			<td style={{whiteSpace: "nowrap"}}>{item.excess}</td>
			<td style={{whiteSpace: "nowrap"}}>{item.finalUsage}</td>
			<td style={{whiteSpace: "nowrap"}}>{item.qadUsage}</td>
			<td style={{whiteSpace: "nowrap"}}>{item.variance}</td>
		</tr>
	}

	render() {
		return (
			<div>
				<h2 className='text-center' style={{ margin: "10px 0px 10px" }}>Rapport Usage / BOM</h2>
				{this.renderHeader()}
				{this.renderTable()}
			</div>
		)
	}
}
