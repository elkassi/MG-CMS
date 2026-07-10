import axios from 'axios'
import React, { Component } from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { 
	faSearch, 
	faSpinner,
	faSync,
	faCheck,
	faTimes
} from '@fortawesome/free-solid-svg-icons'

export default class RouleauSummary extends Component {

	constructor(props) {
		super(props)
		this.state = {
			rouleauList: [],
			loading: false,
			currentPage: 0,
			itemsPerPage: 100,
			totalPages: 0,
			totalElements: 0,
			
			searchRollId: '',
			searchItemNumber: '',
			statusFilter: 'ALL'
		}
	}

	componentDidMount() {
		this.loadRouleauList()
	}

	loadRouleauList = async () => {
		this.setState({ loading: true })
		
		try {
			const { currentPage, itemsPerPage, searchRollId, searchItemNumber, statusFilter } = this.state;
			
			let url = `/api/rouleau-summary/search?page=${currentPage}&size=${itemsPerPage}`;
			if (searchRollId) {
				url += `&rollId=${searchRollId}`;
			}
			if (searchItemNumber) {
				url += `&itemNumber=${searchItemNumber}`;
			}
			if (statusFilter && statusFilter !== 'ALL') {
				url += `&status=${statusFilter}`;
			}

			const response = await axios.get(url);
			
			this.setState({ 
				rouleauList: response.data.content,
				totalPages: response.data.totalPages,
				totalElements: response.data.totalElements,
				loading: false
			});
		} catch (error) {
			console.error("Error fetching rouleau summary", error);
			this.setState({ loading: false });
		}
	}

	handleSearch = (e) => {
		e.preventDefault();
		this.setState({ currentPage: 0 }, this.loadRouleauList);
	}

	handleReset = () => {
		this.setState({
			searchRollId: '',
			searchItemNumber: '',
			statusFilter: 'ALL',
			currentPage: 0
		}, this.loadRouleauList);
	}

	handlePageChange = (newPage) => {
		if (newPage >= 0 && newPage < this.state.totalPages) {
			this.setState({ currentPage: newPage }, this.loadRouleauList);
		}
	}

	render() {
		const { rouleauList, loading, currentPage, totalPages, totalElements, searchRollId, searchItemNumber, statusFilter } = this.state;
		
		return (
			<div className="container-fluid mt-3">
				<div className="card shadow mb-4">
					<div className="card-header py-3 d-flex flex-row align-items-center justify-content-between">
						<h6 className="m-0 font-weight-bold text-primary">Global Roll Summary (Rouleau)</h6>
					</div>
					
					<div className="card-body">
						{/* Search Bar */}
						<form onSubmit={this.handleSearch} className="mb-4">
							<div className="form-row align-items-center">
								<div className="col-sm-3 my-1">
									<label className="sr-only">Roll ID (Reference)</label>
									<input 
										type="text" 
										className="form-control" 
										placeholder="Roll ID (Reference)" 
										value={searchRollId}
										onChange={(e) => this.setState({ searchRollId: e.target.value })}
									/>
								</div>
								<div className="col-sm-3 my-1">
									<label className="sr-only">Item Number</label>
									<input 
										type="text" 
										className="form-control" 
										placeholder="Item Number (Reftissu)" 
										value={searchItemNumber}
										onChange={(e) => this.setState({ searchItemNumber: e.target.value })}
									/>
								</div>
								<div className="col-sm-3 my-1">
									<label className="sr-only">Status Filter</label>
									<select 
										className="form-control" 
										value={statusFilter}
										onChange={(e) => this.setState({ statusFilter: e.target.value })}
									>
										<option value="ALL">All Statuses</option>
										<option value="In stock">In stock</option>
										<option value="In production">In production</option>
										<option value="Consommé">Consommé</option>
										<option value="Blocked">Blocked</option>
									</select>
								</div>
								<div className="col-auto my-1">
									<button type="submit" className="btn btn-primary mr-2" disabled={loading}>
										<FontAwesomeIcon icon={loading ? faSpinner : faSearch} spin={loading} className="mr-1" />
										Search
									</button>
									<button type="button" className="btn btn-secondary" onClick={this.handleReset} disabled={loading}>
										<FontAwesomeIcon icon={faSync} className="mr-1" />
										Reset
									</button>
								</div>
							</div>
						</form>

						{/* Table */}
						<div className="table-responsive">
							<table className="table table-bordered table-hover table-sm">
								<thead className="thead-light">
									<tr>
										<th>Roll ID</th>
										<th>Serial ID</th>
										<th>Item Ref</th>
										<th>Quantity</th>
										<th>Status</th>
										<th>Emplacement</th>
										<th>R100 Location</th>
									</tr>
								</thead>
								<tbody>
									{loading && rouleauList.length === 0 ? (
										<tr><td colSpan="7" className="text-center">Loading...</td></tr>
									) : rouleauList.length === 0 ? (
										<tr><td colSpan="7" className="text-center">No rolls found</td></tr>
									) : (
										rouleauList.map((roll, idx) => {
											let rowClass = "";
											if (roll.status === "Consommé") rowClass = "table-danger";
											if (roll.status === "Blocked") rowClass = "table-secondary";
											
											return (
												<tr key={idx} className={rowClass}>
												<td><strong>{roll.rollId}</strong></td>
												<td>{roll.serialId || '-'}</td>
												<td>{roll.itemNumber}</td>
												<td>{roll.quantity}</td>
												<td>
													{roll.status === 'In stock' ? (
														<span className="badge badge-info">In stock</span>
													) : roll.status === 'In production' ? (
														<span className="badge badge-warning">In production</span>
													) : roll.status === 'Consommé' ? (
														<span className="badge badge-danger">Consommé</span>
													) : (
														<span className="badge badge-dark">Blocked</span>
													)}
												</td>
												<td>{roll.emplacement || '-'}</td>
												<td>{roll.r100Location || '-'}</td>
												</tr>
											);
										})
									)}
								</tbody>
							</table>
						</div>

						{/* Pagination */}
						{!loading && totalElements > 0 && (
							<div className="d-flex justify-content-between align-items-center mt-3">
								<div>
									Showing page {currentPage + 1} of {totalPages} ({totalElements} total rolls)
								</div>
								<ul className="pagination mb-0">
									<li className={`page-item ${currentPage === 0 ? 'disabled' : ''}`}>
										<button className="page-link" onClick={() => this.handlePageChange(currentPage - 1)}>Previous</button>
									</li>
									<li className={`page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}`}>
										<button className="page-link" onClick={() => this.handlePageChange(currentPage + 1)}>Next</button>
									</li>
								</ul>
							</div>
						)}
					</div>
				</div>
			</div>
		)
	}
}
