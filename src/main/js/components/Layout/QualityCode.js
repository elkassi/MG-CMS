import { faSave, faSpinner, faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import axios from 'axios';
import React, { Component } from 'react'

export default class QualityCode extends Component {

	constructor() {
		super();
		this.state = {
			loading: false,
			code: null,
			reftissuList: [],
			reftissu: "",
			description: "",
			filter: {}
		}
	}

	componentDidMount() {
		this.setState({ loading: true });
		axios.get("/api/config/code")
			.then(res => {
				this.setState({ loading: false, code: res.data });
			})
		axios.get("/api/qualityReftissuBlock/list")
			.then(res => {
				this.setState({ reftissuList: res.data.sort((a, b) => b.date.localeCompare(a.date)) });
			})
	}

	render() {
		return (
			<div>
				<h1 className='entityform-title text-center'>Quality Code : {this.state.loading
					? <FontAwesomeIcon icon={faSpinner} spin />
					: this.state.code
				}</h1>
				<div style={{ display: "flex" }}>
					<div style={{ flex: 1, padding: 10 }}>
						<div className='d-flex mb-1'>
							<input type="text" className="form-control mr-1 " placeholder="Reftissu...." value={this.state.reftissu}
								style={{ width: 220 }}
								onChange={(e) => this.setState({ reftissu: e.target.value })}
							/>
							<input type="text" className="form-control mr-1 " placeholder="Description..." value={this.state.description}
								onChange={(e) => this.setState({ description: e.target.value })}
							/>
							<button className="btn btn-primary"
								disabled={this.state.reftissu === ""}
								onClick={() => {
									axios.post(`/api/qualityReftissuBlock`, { reftissu: this.state.reftissu, description: this.state.description })
										.then(res => {
											this.setState({ reftissu: "", description: "", reftissuList: [res.data, ...this.state.reftissuList] });
										})
								}}><FontAwesomeIcon icon={faSave} /></button>
						</div>
						<div className='table-responsive'>
							<table className="table table-striped table-cells-sm">
								<thead style={{backgroundColor: "#ca0c0c", color: "white"}}>
									<tr>
										<th>Reftissu</th>
										<th>Date</th>
										<th>Créé par</th>
										<th>Description</th>
										<th></th>
									</tr>
									<tr>
										<th className='table-elem-sm'><input type="text" placeholder="Reftissu..." value={this.state.filter.reftissu}
											onChange={(e) => this.setState({ filter: { ...this.state.filter, reftissu: e.target.value } })} /></th>
										<th className='table-elem-sm'><input type="text" placeholder="Date..." value={this.state.filter.date}
											onChange={(e) => this.setState({ filter: { ...this.state.filter, date: e.target.value } })} /></th>
										<th className='table-elem-sm'><input type="text" placeholder="Créé par..." value={this.state.filter.createdBy}
											onChange={(e) => this.setState({ filter: { ...this.state.filter, createdBy: e.target.value } })} /></th>
										<th className='table-elem-sm'></th>
										<th className='table-elem-sm'></th>
									</tr>
								</thead>
								<tbody>
									{this.state.reftissuList
									.filter(elem => (
										(this.state.filter.reftissu == null || (elem.reftissu && elem.reftissu.toUpperCase().startsWith(this.state.filter.reftissu.toUpperCase()))) &&
										(this.state.filter.date == null || (elem.date && elem.date.toUpperCase().startsWith(this.state.filter.date.toUpperCase()))) &&
										(this.state.filter.createdBy == null || (elem.createdBy && elem.createdBy.toUpperCase().startsWith(this.state.filter.createdBy.toUpperCase())))
									))									
									.map((obj, index) => (
										<tr key={index}>
											<td>{obj.reftissu}</td>
											<td>{obj.date}</td>
											<td>{obj.createdBy}</td>
											<td>{obj.description}</td>
											<td>
												<button className="btn" onClick={() => {
													axios.post(`/api/qualityReftissuBlock/delete`, obj)
														.then(res => {
															this.setState({ reftissuList: this.state.reftissuList.filter((item) => item.reftissu !== obj.reftissu) });
														})
												}}><FontAwesomeIcon icon={faTimes} color='red' /></button>
											</td>
										</tr>
									))}
								</tbody>
							</table>
						</div>
					</div>
					{/* <div style={{ flex: 1 }}>

					</div> */}
				</div>
			</div>
		)
	}
}
