import axios from 'axios'
import React, { Component } from 'react'
import "../../styles/RapportRestRouleau.scss"
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { 
	faPrint, 
	faChevronLeft, 
	faChevronRight,
	faFileExcel,
	faTable,
	faFilter,
	faCog,
	faSpinner,
	faSort,
	faSortUp,
	faSortDown
} from '@fortawesome/free-solid-svg-icons'
import "../../styles/RapportRestRouleau.scss"
import ReactToPrint from "react-to-print";

export default class RapportRestRouleau extends Component {

	constructor(props) {
		super(props)
		this.state = {
			rouleauList: [],
			rouleauEncoursList: [],
			filter: {},
			filterEncours: {},
			loading: false,
			// Pagination for first table
			currentPage: 1,
			itemsPerPage: 10,
			// UI state
			showFilters: true,
			showFiltersEncours: true,
			// Sorting for first table
			sortProp: null,
			sortDirection: 'asc',
			// Sorting for second table
			sortPropEncours: null,
			sortDirectionEncours: 'asc'
		}
		// Refs for print components
		this.printRouleauRef = React.createRef();
		this.printEncoursRef = React.createRef();
	}

	componentDidMount() {
		this.loadRouleauList()
	}

	async loadRouleauList() {
		this.setState({ loading: true })
		
		try {
			const rouleauResponse = await axios.get("/api/cuttingRequestSerieRouleauData/rouleauRapport");
			const encoursResponse = await axios.get("/api/serieRouleauTemp/all");
			this.setState({ 
				rouleauList: rouleauResponse.data,
				rouleauEncoursList: encoursResponse.data,
				loading: false
			});
		} catch (error) {
			console.log(error);
			this.setState({ loading: false });
		}
	}

	// Pagination methods
	handlePageChange = (newPage) => {
		this.setState({ currentPage: newPage })
	}

	handleItemsPerPageChange = (newItemsPerPage) => {
		this.setState({ 
			itemsPerPage: newItemsPerPage,
			currentPage: 1 
		})
	}

	// Sorting methods
	sortChanged = (field) => {
		let sortProp = field;
		let propChanged = this.state.sortProp !== sortProp;
		let sortDirection = propChanged ? 'asc' : this.state.sortDirection === 'asc' ? 'desc' : 'asc';

		this.setState({ sortProp, sortDirection, currentPage: 1 });
	}

	sortChangedEncours = (field) => {
		let sortPropEncours = field;
		let propChanged = this.state.sortPropEncours !== sortPropEncours;
		let sortDirectionEncours = propChanged ? 'asc' : this.state.sortDirectionEncours === 'asc' ? 'desc' : 'asc';

		this.setState({ sortPropEncours, sortDirectionEncours });
	}

	// Sort Icon component
	renderSortIcon = (field, isEncours = false) => {
		const currentSort = isEncours ? this.state.sortPropEncours : this.state.sortProp;
		const sortDirection = isEncours ? this.state.sortDirectionEncours : this.state.sortDirection;

		if (currentSort === field) {
			return <FontAwesomeIcon icon={sortDirection === 'asc' ? faSortUp : faSortDown} className="sort-icon active" />;
		}
		return <FontAwesomeIcon icon={faSort} className="sort-icon" />;
	}

	// Sorting helper function
	sortData = (data, sortProp, sortDirection) => {
		if (!sortProp) return data;

		return [...data].sort((a, b) => {
			let aVal = a[sortProp];
			let bVal = b[sortProp];

			// Handle null/undefined values
			if (aVal == null && bVal == null) return 0;
			if (aVal == null) return 1;
			if (bVal == null) return -1;

			// Convert to string for comparison
			aVal = aVal.toString().toLowerCase();
			bVal = bVal.toString().toLowerCase();

			// Check if values are numeric
			const aNum = parseFloat(aVal);
			const bNum = parseFloat(bVal);
			
			if (!isNaN(aNum) && !isNaN(bNum)) {
				// Numeric comparison
				return sortDirection === 'asc' ? aNum - bNum : bNum - aNum;
			} else {
				// String comparison
				if (aVal < bVal) return sortDirection === 'asc' ? -1 : 1;
				if (aVal > bVal) return sortDirection === 'asc' ? 1 : -1;
				return 0;
			}
		});
	}

	// Filter methods
	getFilteredRouleauList = () => {
		const filtered = this.state.rouleauList.filter(item => 
			(this.state.filter.reftissu == null || (item.reftissu && item.reftissu.toString().toLowerCase().includes(this.state.filter.reftissu.toLowerCase())))
			&& (this.state.filter.lotFrs == null || (item.lotFrs && item.lotFrs.toString().toLowerCase().includes(this.state.filter.lotFrs.toLowerCase())))
			&& (this.state.filter.idRouleau == null || (item.idRouleau && item.idRouleau.toString().toLowerCase().includes(this.state.filter.idRouleau.toLowerCase())))
			&& (this.state.filter.laize == null || (item.laize && item.laize.toString().toLowerCase().includes(this.state.filter.laize.toLowerCase())))
			&& (this.state.filter.retour == null || (item.retour && item.retour.toString().toLowerCase().includes(this.state.filter.retour.toLowerCase())))
			&& (this.state.filter.serie == null || (item.serie && item.serie.toString().toLowerCase().includes(this.state.filter.serie.toLowerCase())))
			&& (this.state.filter.createdAt == null || (item.createdAt && item.createdAt.toString().toLowerCase().includes(this.state.filter.createdAt.toLowerCase())))
			&& (this.state.filter.tableMatelassage == null || (item.tableMatelassage && item.tableMatelassage.toString().toLowerCase().includes(this.state.filter.tableMatelassage.toLowerCase())))
			&& (this.state.filter.locationMP == null || (item.locationMP && item.locationMP.toString().toLowerCase().includes(this.state.filter.locationMP.toLowerCase())))
			&& (this.state.filter.quantityMP == null || (item.quantityMP && item.quantityMP.toString().toLowerCase().includes(this.state.filter.quantityMP.toLowerCase())))
			&& (this.state.filter.statut == null || this.state.filter.statut === '' || (item.statut && item.statut === this.state.filter.statut))
		);

		// Apply sorting
		return this.sortData(filtered, this.state.sortProp, this.state.sortDirection);
	}

	// Get unique statut options for dropdown
	getStatutOptions = () => {
		const statuts = this.state.rouleauList
			.map(item => item.statut)
			.filter(statut => statut != null && statut !== '')
			.filter((statut, index, self) => self.indexOf(statut) === index)
			.sort();
		return statuts;
	}

	getFilteredEncoursList = () => {
		const filtered = this.state.rouleauEncoursList.filter(item => 
			(this.state.filterEncours.tableMatelassage == null || (item.tableMatelassage && item.tableMatelassage.toString().toLowerCase().includes(this.state.filterEncours.tableMatelassage.toLowerCase())))
			&& (this.state.filterEncours.idRouleau == null || (item.idRouleau && item.idRouleau.toString().toLowerCase().includes(this.state.filterEncours.idRouleau.toLowerCase())))
			&& (this.state.filterEncours.lot == null || (item.lot && item.lot.toString().toLowerCase().includes(this.state.filterEncours.lot.toLowerCase())))
			&& (this.state.filterEncours.reftissu == null || (item.reftissu && item.reftissu.toString().toLowerCase().includes(this.state.filterEncours.reftissu.toLowerCase())))
			&& (this.state.filterEncours.date == null || (item.date && item.date.toString().toLowerCase().includes(this.state.filterEncours.date.toLowerCase())))
			&& (this.state.filterEncours.quantiteInitiale == null || (item.quantiteInitiale && item.quantiteInitiale.toString().toLowerCase().includes(this.state.filterEncours.quantiteInitiale.toLowerCase())))
			&& (this.state.filterEncours.estimationRest == null || (item.estimationRest && item.estimationRest.toString().toLowerCase().includes(this.state.filterEncours.estimationRest.toLowerCase())))
		);

		// Apply sorting
		return this.sortData(filtered, this.state.sortPropEncours, this.state.sortDirectionEncours);
	}

	// Export and Print methods
	exportToCSV = (data, filename, headers) => {
		const csvContent = [
			headers.join(','),
			...data.map(row => headers.map(header => {
				const key = header.toLowerCase().replace(/\s+/g, '');
				const value = row[key] || row[header] || '';
				return `"${value.toString().replace(/"/g, '""')}"`;
			}).join(','))
		].join('\n');

		const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
		const link = document.createElement('a');
		const url = URL.createObjectURL(blob);
		link.setAttribute('href', url);
		link.setAttribute('download', `${filename}.csv`);
		link.style.visibility = 'hidden';
		document.body.appendChild(link);
		link.click();
		document.body.removeChild(link);
	}

	exportRouleauToCSV = () => {
		const filteredData = this.getFilteredRouleauList();
		const headers = ['reftissu', 'lotFrs', 'idRouleau', 'laize', 'retour', 'serie', 'createdAt', 'tableMatelassage', 'locationMP', 'quantityMP', 'statut'];
		this.exportToCSV(filteredData, 'rapport_rest_rouleaux', headers);
	}

	exportEncoursToCSV = () => {
		const filteredData = this.getFilteredEncoursList();
		const headers = ['tableMatelassage', 'idRouleau', 'lot', 'reftissu', 'date', 'quantiteInitiale', 'estimationRest'];
		this.exportToCSV(filteredData, 'en_cours_matelassage', headers);
	}

	// Print component for Rouleau table (hidden)
	renderPrintableRouleauTable = () => {
		const filteredData = this.getFilteredRouleauList();
		
		return (
			<div className="print-container">
				<div className="print-header">
					<div className="print-logo">LEAR Corporation</div>
					<h2 className="print-title">Rapport Rest Rouleaux</h2>
					<div className="print-info">
						<p>Date d'impression: {new Date().toLocaleDateString('fr-FR')}</p>
						<p>Total d'enregistrements: {filteredData.length}</p>
					</div>
				</div>
				<table className="print-table">
					<thead>
						<tr>
							<th>Réf Tissu</th>
							<th>Lot Frs</th>
							<th>ID Rouleau</th>
							<th>Laize</th>
							<th>Retour</th>
							<th>Série</th>
							<th>Créé le</th>
							<th>Table Matelassage</th>
							<th>Location MP</th>
							<th>Quantity MP</th>
							<th>Statut</th>
						</tr>
					</thead>
					<tbody>
						{filteredData.map((rouleau, index) => (
							<tr key={index}>
								<td>{rouleau.reftissu || '-'}</td>
								<td>{rouleau.lotFrs || '-'}</td>
								<td>{rouleau.idRouleau || '-'}</td>
								<td>{rouleau.laize || '-'}</td>
								<td>{rouleau.retour || '-'}</td>
								<td>{rouleau.serie || '-'}</td>
								<td>{rouleau.createdAt || '-'}</td>
								<td>{rouleau.tableMatelassage || '-'}</td>
								<td>{rouleau.locationMP || '-'}</td>
								<td>{rouleau.quantityMP || '-'}</td>
								<td>{rouleau.statut || '-'}</td>
							</tr>
						))}
					</tbody>
				</table>
			</div>
		);
	}

	// Print component for Encours table (hidden)
	renderPrintableEncoursTable = () => {
		const filteredData = this.getFilteredEncoursList();
		
		return (
			<div className="print-container">
				<div className="print-header">
					<div className="print-logo">LEAR Corporation</div>
					<h2 className="print-title">En cours de matelassage</h2>
					<div className="print-info">
						<p>Date d'impression: {new Date().toLocaleDateString('fr-FR')}</p>
						<p>Total d'enregistrements: {filteredData.length}</p>
					</div>
				</div>
				<table className="print-table">
					<thead>
						<tr>
							<th>Table Matelassage</th>
							<th>ID Rouleau</th>
							<th>Lot</th>
							<th>Réf Tissu</th>
							<th>Date</th>
							<th>Quantité Initiale</th>
							<th>Estimation Rest</th>
						</tr>
					</thead>
					<tbody>
						{filteredData.map((rouleau, index) => (
							<tr key={index}>
								<td>{rouleau.tableMatelassage || '-'}</td>
								<td>{rouleau.idRouleau || '-'}</td>
								<td>{rouleau.lot || '-'}</td>
								<td>{rouleau.reftissu || '-'}</td>
								<td>{rouleau.date || '-'}</td>
								<td>{rouleau.quantiteInitiale || '-'}</td>
								<td>{rouleau.estimationRest || '-'}</td>
							</tr>
						))}
					</tbody>
				</table>
			</div>
		);
	}

	renderTable() {
		const filteredData = this.getFilteredRouleauList();
		const { currentPage, itemsPerPage } = this.state;
		const totalItems = filteredData.length;
		const totalPages = Math.ceil(totalItems / itemsPerPage);
		const startIndex = (currentPage - 1) * itemsPerPage;
		const endIndex = startIndex + itemsPerPage;
		const paginatedData = filteredData.slice(startIndex, endIndex);
		const statutOptions = this.getStatutOptions();

		return (
			<div className="table-section">
				<div className="table-header">
					<div className="table-title">
						<FontAwesomeIcon icon={faTable} className="table-icon" />
						<h3>Rapport Rest Rouleaux</h3>
						<span className="record-count">({totalItems} enregistrements)</span>
					</div>
					<div className="table-actions">
						<button 
							className="action-btn filter-btn"
							onClick={() => this.setState({ showFilters: !this.state.showFilters })}
						>
							<FontAwesomeIcon icon={faFilter} />
							Filtres
						</button>
						<button 
							className="action-btn export-btn"
							onClick={this.exportRouleauToCSV}
						>
							<FontAwesomeIcon icon={faFileExcel} />
							CSV
						</button>
						<ReactToPrint
							trigger={() => (
								<button className="action-btn print-btn">
									<FontAwesomeIcon icon={faPrint} />
									Imprimer
								</button>
							)}
							content={() => this.printRouleauRef.current}
						/>
					</div>
				</div>

				<div className="table-container">
					<div className="table-responsive">
						<table id="rouleau-table" className="modern-table">
							<thead>
								<tr>
									<th onClick={() => this.sortChanged('reftissu')} className="sortable-header">
										Réf Tissu {this.renderSortIcon('reftissu')}
									</th>
									<th onClick={() => this.sortChanged('lotFrs')} className="sortable-header">
										Lot Frs {this.renderSortIcon('lotFrs')}
									</th>
									<th onClick={() => this.sortChanged('idRouleau')} className="sortable-header">
										ID Rouleau {this.renderSortIcon('idRouleau')}
									</th>
									<th onClick={() => this.sortChanged('laize')} className="sortable-header">
										Laize {this.renderSortIcon('laize')}
									</th>
									<th onClick={() => this.sortChanged('retour')} className="sortable-header">
										Retour {this.renderSortIcon('retour')}
									</th>
									<th onClick={() => this.sortChanged('serie')} className="sortable-header">
										Série {this.renderSortIcon('serie')}
									</th>
									<th onClick={() => this.sortChanged('createdAt')} className="sortable-header">
										Créé le {this.renderSortIcon('createdAt')}
									</th>
									<th onClick={() => this.sortChanged('tableMatelassage')} className="sortable-header">
										Table Matelassage {this.renderSortIcon('tableMatelassage')}
									</th>
									<th onClick={() => this.sortChanged('locationMP')} className="sortable-header">
										Location MP {this.renderSortIcon('locationMP')}
									</th>
									<th onClick={() => this.sortChanged('quantityMP')} className="sortable-header">
										Quantity MP {this.renderSortIcon('quantityMP')}
									</th>
									<th onClick={() => this.sortChanged('statut')} className="sortable-header">
										Statut {this.renderSortIcon('statut')}
									</th>
								</tr>
								{this.state.showFilters && (
									<tr className="filter-row">
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filter.reftissu || ''} 
												onChange={e => this.setState({ 
													filter: { ...this.state.filter, reftissu: e.target.value },
													currentPage: 1
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filter.lotFrs || ''} 
												onChange={e => this.setState({ 
													filter: { ...this.state.filter, lotFrs: e.target.value },
													currentPage: 1
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filter.idRouleau || ''} 
												onChange={e => this.setState({ 
													filter: { ...this.state.filter, idRouleau: e.target.value },
													currentPage: 1
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filter.laize || ''} 
												onChange={e => this.setState({ 
													filter: { ...this.state.filter, laize: e.target.value },
													currentPage: 1
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filter.retour || ''} 
												onChange={e => this.setState({ 
													filter: { ...this.state.filter, retour: e.target.value },
													currentPage: 1
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filter.serie || ''} 
												onChange={e => this.setState({ 
													filter: { ...this.state.filter, serie: e.target.value },
													currentPage: 1
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filter.createdAt || ''} 
												onChange={e => this.setState({ 
													filter: { ...this.state.filter, createdAt: e.target.value },
													currentPage: 1
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filter.tableMatelassage || ''} 
												onChange={e => this.setState({ 
													filter: { ...this.state.filter, tableMatelassage: e.target.value },
													currentPage: 1
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filter.locationMP || ''} 
												onChange={e => this.setState({ 
													filter: { ...this.state.filter, locationMP: e.target.value },
													currentPage: 1
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filter.quantityMP || ''} 
												onChange={e => this.setState({ 
													filter: { ...this.state.filter, quantityMP: e.target.value },
													currentPage: 1
												})} 
											/>
										</th>
										<th>
											<select 
												className="filter-input filter-select"
												value={this.state.filter.statut || ''} 
												onChange={e => this.setState({ 
													filter: { ...this.state.filter, statut: e.target.value },
													currentPage: 1
												})} 
											>
												<option value="">Tous</option>
												{statutOptions.map((statut, index) => (
													<option key={index} value={statut}>{statut}</option>
												))}
											</select>
										</th>
									</tr>
								)}
							</thead>
							<tbody>
								{paginatedData.map((rouleau, index) => (
									<tr key={index}>
										<td>{rouleau.reftissu || '-'}</td>
										<td>{rouleau.lotFrs || '-'}</td>
										<td>{rouleau.idRouleau || '-'}</td>
										<td className="numeric-cell">{rouleau.laize || '-'}</td>
										<td className="numeric-cell">{rouleau.retour || '-'}</td>
										<td>{rouleau.serie || '-'}</td>
										<td>{rouleau.createdAt || '-'}</td>
										<td>{rouleau.tableMatelassage || '-'}</td>
										<td>{rouleau.locationMP || '-'}</td>
										<td className="numeric-cell">{rouleau.quantityMP || '-'}</td>
										<td>
											<span className={`status-badge ${rouleau.statut ? rouleau.statut.toLowerCase().replace(/\s+/g, '-') : 'no-status'}`}>
												{rouleau.statut || '-'}
											</span>
										</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>

					{totalPages > 1 && (
						<div className="pagination-container">
							<div className="pagination-info">
								<span>
									Affichage {startIndex + 1} à {Math.min(endIndex, totalItems)} sur {totalItems} entrées
								</span>
								<select 
									value={itemsPerPage} 
									onChange={e => this.handleItemsPerPageChange(parseInt(e.target.value))}
									className="items-per-page"
								>
									<option value={5}>5 par page</option>
									<option value={10}>10 par page</option>
									<option value={25}>25 par page</option>
									<option value={50}>50 par page</option>
								</select>
							</div>
							<div className="pagination-controls">
								<button 
									onClick={() => this.handlePageChange(currentPage - 1)}
									disabled={currentPage === 1}
									className="pagination-btn"
								>
									<FontAwesomeIcon icon={faChevronLeft} />
								</button>
								<div className="page-numbers">
									{Array.from({ length: totalPages }, (_, i) => i + 1)
										.filter(page => 
											page === 1 || 
											page === totalPages || 
											Math.abs(page - currentPage) <= 2
										)
										.map((page, index, array) => (
											<React.Fragment key={page}>
												{index > 0 && array[index - 1] !== page - 1 && (
													<span className="page-ellipsis">...</span>
												)}
												<button
													onClick={() => this.handlePageChange(page)}
													className={`page-number ${currentPage === page ? 'active' : ''}`}
												>
													{page}
												</button>
											</React.Fragment>
										))
									}
								</div>
								<button 
									onClick={() => this.handlePageChange(currentPage + 1)}
									disabled={currentPage === totalPages}
									className="pagination-btn"
								>
									<FontAwesomeIcon icon={faChevronRight} />
								</button>
							</div>
						</div>
					)}
				</div>
			</div>
		)
	}

	renderTableEncours() {
		const filteredData = this.getFilteredEncoursList();

		return (
			<div className="table-section">
				<div className="table-header">
					<div className="table-title">
						<FontAwesomeIcon icon={faCog} className="table-icon spinning" />
						<h3>En cours de matelassage</h3>
						<span className="record-count">({filteredData.length} enregistrements)</span>
					</div>
					<div className="table-actions">
						<button 
							className="action-btn filter-btn"
							onClick={() => this.setState({ showFiltersEncours: !this.state.showFiltersEncours })}
						>
							<FontAwesomeIcon icon={faFilter} />
							Filtres
						</button>
						<button 
							className="action-btn export-btn"
							onClick={this.exportEncoursToCSV}
						>
							<FontAwesomeIcon icon={faFileExcel} />
							CSV
						</button>
						<ReactToPrint
							trigger={() => (
								<button className="action-btn print-btn">
									<FontAwesomeIcon icon={faPrint} />
									Imprimer
								</button>
							)}
							content={() => this.printEncoursRef.current}
						/>
					</div>
				</div>

				<div className="table-container">
					<div className="table-responsive">
						<table id="encours-table" className="modern-table">
							<thead>
								<tr>
									<th onClick={() => this.sortChangedEncours('tableMatelassage')} className="sortable-header">
										Table Matelassage {this.renderSortIcon('tableMatelassage', true)}
									</th>
									<th onClick={() => this.sortChangedEncours('idRouleau')} className="sortable-header">
										ID Rouleau {this.renderSortIcon('idRouleau', true)}
									</th>
									<th onClick={() => this.sortChangedEncours('lot')} className="sortable-header">
										Lot {this.renderSortIcon('lot', true)}
									</th>
									<th onClick={() => this.sortChangedEncours('reftissu')} className="sortable-header">
										Réf Tissu {this.renderSortIcon('reftissu', true)}
									</th>
									<th onClick={() => this.sortChangedEncours('date')} className="sortable-header">
										Date {this.renderSortIcon('date', true)}
									</th>
									<th onClick={() => this.sortChangedEncours('quantiteInitiale')} className="sortable-header">
										Quantité Initiale {this.renderSortIcon('quantiteInitiale', true)}
									</th>
									<th onClick={() => this.sortChangedEncours('estimationRest')} className="sortable-header">
										Estimation Rest {this.renderSortIcon('estimationRest', true)}
									</th>
								</tr>
								{this.state.showFiltersEncours && (
									<tr className="filter-row">
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filterEncours.tableMatelassage || ''} 
												onChange={e => this.setState({ 
													filterEncours: { ...this.state.filterEncours, tableMatelassage: e.target.value }
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filterEncours.idRouleau || ''} 
												onChange={e => this.setState({ 
													filterEncours: { ...this.state.filterEncours, idRouleau: e.target.value }
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filterEncours.lot || ''} 
												onChange={e => this.setState({ 
													filterEncours: { ...this.state.filterEncours, lot: e.target.value }
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filterEncours.reftissu || ''} 
												onChange={e => this.setState({ 
													filterEncours: { ...this.state.filterEncours, reftissu: e.target.value }
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filterEncours.date || ''} 
												onChange={e => this.setState({ 
													filterEncours: { ...this.state.filterEncours, date: e.target.value }
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filterEncours.quantiteInitiale || ''} 
												onChange={e => this.setState({ 
													filterEncours: { ...this.state.filterEncours, quantiteInitiale: e.target.value }
												})} 
											/>
										</th>
										<th>
											<input 
												type="text" 
												className="filter-input"
												placeholder="Filtrer..."
												value={this.state.filterEncours.estimationRest || ''} 
												onChange={e => this.setState({ 
													filterEncours: { ...this.state.filterEncours, estimationRest: e.target.value }
												})} 
											/>
										</th>
									</tr>
								)}
							</thead>
							<tbody>
								{filteredData.map((rouleau, index) => (
									<tr key={index}>
										<td>{rouleau.tableMatelassage || '-'}</td>
										<td>{rouleau.idRouleau || '-'}</td>
										<td>{rouleau.lot || '-'}</td>
										<td>{rouleau.reftissu || '-'}</td>
										<td>{rouleau.date || '-'}</td>
										<td className="numeric-cell">{rouleau.quantiteInitiale || '-'}</td>
										<td className="numeric-cell">{rouleau.estimationRest || '-'}</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				</div>
			</div>
		)
	}


	render() {
		return (
			<div className="rapport-container">
				<div className="page-header">
					<div className="header-content">
						<h1 className="page-title">
							<FontAwesomeIcon icon={faTable} className="title-icon" />
							Rapport Rest Rouleaux
						</h1>
						<p className="page-description">
							Gestion et suivi des rouleaux de tissu et des processus de matelassage
						</p>
					</div>
					{this.state.loading && (
						<div className="loading-indicator">
							<FontAwesomeIcon icon={faSpinner} spin />
							Chargement des données...
						</div>
					)}
				</div>

				{this.renderTable()}
				{this.renderTableEncours()}
				
				{/* Hidden print components */}
				<div style={{ display: 'none' }}>
					<div ref={this.printRouleauRef}>
						{this.renderPrintableRouleauTable()}
					</div>
					<div ref={this.printEncoursRef}>
						{this.renderPrintableEncoursTable()}
					</div>
				</div>
			</div>
		)
	}
}
