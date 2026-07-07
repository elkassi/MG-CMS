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

export default class RapportShortage extends Component {

	constructor() {
		super();
		this.state = {
			list: [],
			reftissu: "",
			filter: {},
			sortProp: "excess",
			sortDirec: "asc",
		}
	}

	componentDidMount() {

	}

	exportExcel = () => {
		let data = this.state.list.map(e => {
            		/*
    private String lotFrs;
    private Double excess;
    private LocalDateTime minDate;
    private LocalDateTime maxDate;

		*/

			return {
				// put the keys as labels of the header
				"lotFrs": e.lotFrs,
				"excess": e.excess,
				"Date min": e.minDate,
				"Date max": e.maxDate,
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
        let dep = ""
		if(this.state.reftissu){
			dep += "&reftissu=" + this.state.reftissu;
		}
		try{
			let res = await axios.get(`/api/query/rapportExcess?${dep}`)
			this.setState({ list: res.data })
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
    private String lotFrs;
    private Double excess;
    private LocalDateTime minDate;
    private LocalDateTime maxDate;

		*/
		return <div className='table-responsive' style={{ maxHeight: "calc(100% - 200px)" }}>
			<table className='table table-bordered m-0 table-cells-sm' style={{ fontSize: 12 }}>
				<thead style={{ position: "sticky", top: -1, backgroundColor: "#bf3030", color: "white" }}>
					<tr>
                        <th onClick={() => this.sortChanged("lotFrs")}>lotFrs <SortIcon currentSort="lotFrs" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
                        <th onClick={() => this.sortChanged("excess")}>excess <SortIcon currentSort="excess" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
                        <th onClick={() => this.sortChanged("minDate")}>Date Min <SortIcon currentSort="minDate" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
						<th onClick={() => this.sortChanged("maxDate")}>Date Max <SortIcon currentSort="maxDate" sortProp={this.state.sortProp} sortDirection={this.state.sortDirec} /></th>
					</tr>
					<tr className=''>
						<th className='table-elem-sm'><input value={this.state.filter.lotFrs} onChange={(e) => { this.setState({ filter: { ...this.state.filter, lotFrs: e.target.value } }) }} /></th>
                        <th className='table-elem-sm'><input value={this.state.filter.excess} type="number" onChange={(e) => { this.setState({ filter: { ...this.state.filter, excess: e.target.value === "" ? null : parseFloat(e.target.value) } }) }} /></th>
                        <th className='table-elem-sm'><input value={this.state.filter.minDate} onChange={(e) => { this.setState({ filter: { ...this.state.filter, minDate: e.target.value } }) }} /></th>
						<th className='table-elem-sm'><input value={this.state.filter.maxDate} onChange={(e) => { this.setState({ filter: { ...this.state.filter, maxDate: e.target.value } }) }} /></th>
					</tr>
				</thead>
				<tbody>
					{this.state.list == null ? <tr ><td colSpan={100}>Chargement...</td></tr> :
						this.state.list.length === 0
							? <tr><td colSpan={100}>Aucune données disponibles</td> </tr>
							: this.state.list
								.filter(elem => (
									(this.state.filter.cuttingRequest_sequence == null || elem.cuttingRequest_sequence?.toUpperCase().startsWith(this.state.filter.cuttingRequest_sequence.toUpperCase())) &&
                                    (this.state.filter.defaut == null || elem.defaut === (this.state.filter.defaut)) &&
                                    (this.state.filter.minDate == null || elem.minDate?.toUpperCase().startsWith(this.state.filter.minDate.toUpperCase())) &&
									(this.state.filter.maxDate == null || elem.maxDate?.toUpperCase().startsWith(this.state.filter.maxDate.toUpperCase()))
								))
								.sort((a, b) => {
									if (this.state.sortProp && this.state.sortDirec) {
										if (["lotFrs", "minDate", "maxDate"].includes(this.state.sortProp)) {
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
        		/*
    private String lotFrs;
    private Double excess;
    private LocalDateTime minDate;
    private LocalDateTime maxDate;

		*/

		return <tr key={"row-" + ind}>
			<td style={{whiteSpace: "nowrap"}}>{item.lotFrs}</td>
			<td style={{whiteSpace: "nowrap"}} >{item.excess}</td>
			<td style={{whiteSpace: "nowrap"}} >{item.minDate}</td>
			<td style={{whiteSpace: "nowrap"}} >{item.maxDate}</td>
		</tr>
	}

	render() {
		return (
			<div>
				<h2 className='text-center' style={{ margin: "10px 0px 10px" }}>Rapport Shortage</h2>
				{this.renderHeader()}
				{this.renderTable()}
			</div>
		)
	}
}
