import { faSpinner } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import axios from 'axios';
import React, { Component } from 'react';
import Select from "react-select";

const optionTypes = [
	{ label: "fabric", value: "fabric" },
	{ label: "supplier kit leather", value: "supplier kit leather" },
	{ label: "supplier kit fabric", value: "supplier kit fabric" },
    { label: "CNC", value: "CNC" }
];

export default class ToleranceForm extends Component {
	constructor() {
		super();
		this.state = {
			projet: "",
			type: null,
			marge: "",
			max1: "",
			min1: "",
			max2: "",
			min2: "",

			t2marge: "",
			t2max1: "",
			t2min1: "",
			t2max2: "",
			t2min2: "",
			loading: false
		};
	}

	handleSubmit = () => {
		let dep = []
		if (this.state.min1.length > 0 && this.state.max1.length > 0) {
			dep.push("min1=" + this.state.min1 + "&max1=" + this.state.max1)
		}
		if (this.state.t2min1.length > 0 && this.state.t2max1.length > 0) {
			dep.push("t2min1=" + this.state.t2min1 + "&t2max1=" + this.state.t2max1)
		}
		if(this.state.projet && this.state.projet.length > 0) {
			dep.push("projet="+ this.state.projet) 
		}
		if(this.state.type && this.state.type.length > 0) {
			dep.push("type="+ this.state.type) 
		}

		if (dep.length > 0) {
			this.setState({loading: true})
			axios.post(`/api/query/updateTolerence?${dep.join("&")}`)
			.finally(() => {
				this.setState({loading: false})

			})
		}
	}

	handleInputChange = (field, value, isFloat) => {
    if (isFloat) {
        const floatPattern = /^-?\d*\.?\d*$/;
        if (!floatPattern.test(value)) {
            return; // Do not update state if the value does not match the float pattern
        }
    }
    this.setState({ [field]: value });
};



	render() {
		return (
			<div className="container mt-4">
				<h1 className="text-center mb-4">Tolerance Form</h1>
				<div className="row mb-3">
					<div className="col-md-6">
						<input
							type="text"
							className="form-control"
							placeholder="Projet..."
							value={this.state.projet}
							onChange={e => this.handleInputChange('projet', e.target.value, false)}
						/>
					</div>
					<div className="col-md-6" style={{fontSize: 18}}>
						<Select
							classNamePrefix="rs"
							placeholder="Type..."
							isClearable
							value={this.state.type ? optionTypes.find(e => e.value === this.state.type) : null}
							options={optionTypes}
							onChange={option => this.handleInputChange('type', option ? option.value : null, false)}
						/>
					</div>
				</div>
				<h3 className="text-center mb-4">Tolerance 1 (non laminé)</h3>
				<div className="table-responsive">
					<table className="table table-bordered">
						<thead className="thead-light">
							<tr>
								<th>Tolerance inf</th>
								<th>Seuil</th>
								<th>Tolerance sup</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<td>
									<input
										type="text"
										className="form-control"
										placeholder="min1..."
										value={this.state.min1}
										onChange={e => this.handleInputChange('min1', e.target.value, true)}
									/>
								</td>
								<td rowSpan={2} className="align-middle text-center">
									<input
										type="text" disabled
										className="form-control"
										placeholder="marge..."
										value={this.state.marge}
										onChange={e => this.handleInputChange('marge', e.target.value, true)}
									/>
								</td>
								<td>
									<input
										type="text" disabled
										className="form-control"
										placeholder="min2..."
										value={this.state.min2}
										onChange={e => this.handleInputChange('min2', e.target.value, true)}
									/>
								</td>
							</tr>
							<tr>
								<td>
									<input
										type="text"
										className="form-control"
										placeholder="max1..."
										value={this.state.max1}
										onChange={e => this.handleInputChange('max1', e.target.value, true)}
									/>
								</td>
								<td>
									<input
										type="text" disabled
										className="form-control"
										placeholder="max2..."
										value={this.state.max2}
										onChange={e => this.handleInputChange('max2', e.target.value, true)}
									/>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
				<h3 className="text-center mb-4">Tolerance 2 (Lamination)</h3>
				<div className="table-responsive">
					<table className="table table-bordered">
						<thead className="thead-light">
							<tr>
								<th>Tolerance inf</th>
								<th>Seuil</th>
								<th>Tolerance sup</th>
							</tr>
						</thead>
						<tbody>
							<tr>
								<td>
									<input
										type="text"
										className="form-control"
										placeholder="t2min1..."
										value={this.state.t2min1}
										onChange={e => this.handleInputChange('t2min1', e.target.value, true)}
									/>
								</td>
								<td rowSpan={2} className="align-middle text-center">
									<input
										type="text" disabled
										className="form-control"
										placeholder="t2marge..."
										value={this.state.t2marge}
										onChange={e => this.handleInputChange('t2marge', e.target.value, true)}
									/>
								</td>
								<td>
									<input
										type="text" disabled
										className="form-control"
										placeholder="t2min2..."
										value={this.state.t2min2}
										onChange={e => this.handleInputChange('t2min2', e.target.value, true)}
									/>
								</td>
							</tr>
							<tr>
								<td>
									<input
										type="text"
										className="form-control"
										placeholder="t2max1..."
										value={this.state.t2max1}
										onChange={e => this.handleInputChange('t2max1', e.target.value, true)}
									/>
								</td>
								<td>
									<input
										type="text" disabled
										className="form-control"
										placeholder="t2max2..."
										value={this.state.t2max2}
										onChange={e => this.handleInputChange('t2max2', e.target.value, true)}
									/>
								</td>
							</tr>
						</tbody>
					</table>
				</div>
				<div className="text-center mt-3">
					<button className="btn btn-primary" onClick={this.handleSubmit} disabled={this.state.loading} >
						{this.state.loading && <FontAwesomeIcon icon={faSpinner} spin />}Apply Changes
					</button>
				</div>
			</div>
		);
	}
}
