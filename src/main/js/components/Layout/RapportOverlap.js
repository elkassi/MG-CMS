import { faFileCsv, faSearch } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import axios from 'axios';
import moment from 'moment';
import React, { Component } from 'react'

export default class RapportOverlap extends Component {

	constructor(props) {
		super(props);
		this.state = {
			rapportOverlap: [],
			date1: moment().subtract(1, 'days').format('YYYY-MM-DD'),
			date2: moment().format('YYYY-MM-DD'),
			filter: {},
		}
	}

	componentDidMount() {
		this.getRapportOverlap();
	}

	getRapportOverlap = () => {
		this.setState({ rapportOverlap: null });
		axios.get(`/api/cuttingRequestSerieInfo/rapportOverlap?date1=${this.state.date1}&date2=${this.state.date2}`)
			.then(response => {
				this.setState({ rapportOverlap: response.data });
			})
			.catch(error => {
				console.log(error);
				this.setState({ rapportOverlap: [] });
			});
	}

	downloadCSV = () => {
		// download csv that is fill with this.state.rapportOverlap
		const replacer = (key, value) => value === null ? '' : value;
		// user the hearder that i have in my table
		const header = ["cuttingRequest_sequence", "cuttingRequestSerie_serie", "quantite", "partNumbers", "placement", "longueurPremierCouche", "nbrCouche", "laizeMesure", "laize", "confirmReftissu", "description", "createdAt", "tableMatelassage", "tableCoupe", "drill1", "drill2", "matelassageEndroit", "matelasseur", "nbrCoucheTotal", "overlap1", "overlap2", "overlap3", "overlap4", "overlap5", "overlap6", "overlap7", "overlap8", "excess", "retour", "totalUsage", "cuttingPlanId", "cmsId"];
		let csv = this.state.rapportOverlap.map(row => header.map(fieldName => JSON.stringify(row[fieldName], replacer)).join(','));
		csv.unshift(header.join(','));
		csv = csv.join('\r\n');
		const blob = new Blob([csv], { type: 'text/csv' });
		const url = window.URL.createObjectURL(blob);
		const a = document.createElement('a');
		a.href = url;
		a.download = 'rapportOverlap.csv';
		a.click();
		window.URL.revokeObjectURL(url);
	}



	render() {
		return (
			<div>
				<h2 className='text-center' style={{display: "flex", justifyContent: "center", margin: "8 0" }}>
					Rapport Overlap
					<input type='date' value={this.state.date1} onChange={e => this.setState({ date1: e.target.value })} className="ml-1" />
					<input type='date' value={this.state.date2} onChange={e => this.setState({ date2: e.target.value })} className="ml-1" />
					<button onClick={this.getRapportOverlap} className='btn btn-primary ml-1'><FontAwesomeIcon icon={faSearch} /> Chercher</button>
					<button onClick={this.downloadCSV} className='btn btn-success ml-1'><FontAwesomeIcon icon={faFileCsv} /> Télecharger</button>
				</h2>
				<div className='table-responsive entity-table  mb-2'
					style={{ maxHeight: `calc(100vh - 69px)` }}
				>
					<table className='table table-striped table-bordered table-elements-sm m-0'>
						<thead className='entity-table-header'>
							<tr>
								<th>Sequence</th>
								<th>Serie</th>
								<th>Quantité</th>
								<th>Part Numbers</th>
								<th>Placement</th>
								<th>Longueur 1ere couche</th>
								<th>Nbr couche</th>
								<th>Laize Mesurée</th>
								<th>Laize</th>
								<th>Confirm ref tissu</th>
								<th>Description</th>
								<th>Created at</th>
								<th>Table matelassage</th>
								<th>Table coupe</th>
								<th>Drill1</th>
								<th>Drill2</th>
								<th>Sens</th>
								<th>Matelasseur</th>
								<th>Nbr couche total</th>
								<th>Overlap 1</th>
								<th>Overlap 2</th>
								<th>Overlap 3</th>
								<th>Overlap 4</th>
								<th>Overlap 5</th>
								<th>Overlap 6</th>
								<th>Overlap 7</th>
								<th>Overlap 8</th>
								<th>Excess</th>
								<th>Retour</th>
								<th>Total usage</th>
								<th>Cutting Plan ID</th>
								<th>CMS ID</th>
							</tr>
							<tr>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.cuttingRequest_sequence} onChange={e => this.setState({ filter: { ...this.state.filter, cuttingRequest_sequence: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.cuttingRequestSerie_serie} onChange={e => this.setState({ filter: { ...this.state.filter, cuttingRequestSerie_serie: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.quantite} onChange={e => this.setState({ filter: { ...this.state.filter, quantite: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.partNumbers} onChange={e => this.setState({ filter: { ...this.state.filter, partNumbers: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.placement} onChange={e => this.setState({ filter: { ...this.state.filter, placement: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.longueurPremierCouche} onChange={e => this.setState({ filter: { ...this.state.filter, longueurPremierCouche: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.nbrCouche} onChange={e => this.setState({ filter: { ...this.state.filter, nbrCouche: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.laize} onChange={e => this.setState({ filter: { ...this.state.filter, laize: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.laizeMesuree} onChange={e => this.setState({ filter: { ...this.state.filter, laizeMesuree: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.confirmRefTissu} onChange={e => this.setState({ filter: { ...this.state.filter, confirmRefTissu: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.description} onChange={e => this.setState({ filter: { ...this.state.filter, description: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.createdAt} onChange={e => this.setState({ filter: { ...this.state.filter, createdAt: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.tableMatelassage} onChange={e => this.setState({ filter: { ...this.state.filter, tableMatelassage: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.tableCoupe} onChange={e => this.setState({ filter: { ...this.state.filter, tableCoupe: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.drill1} onChange={e => this.setState({ filter: { ...this.state.filter, drill1: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.drill2} onChange={e => this.setState({ filter: { ...this.state.filter, drill2: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.matelassageEndroit} onChange={e => this.setState({ filter: { ...this.state.filter, matelassageEndroit: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.matelasseur} onChange={e => this.setState({ filter: { ...this.state.filter, matelasseur: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.nbrCoucheTotal} onChange={e => this.setState({ filter: { ...this.state.filter, nbrCoucheTotal: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.overlap1} onChange={e => this.setState({ filter: { ...this.state.filter, overlap1: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.overlap2} onChange={e => this.setState({ filter: { ...this.state.filter, overlap2: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.overlap3} onChange={e => this.setState({ filter: { ...this.state.filter, overlap3: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.overlap4} onChange={e => this.setState({ filter: { ...this.state.filter, overlap4: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.overlap5} onChange={e => this.setState({ filter: { ...this.state.filter, overlap5: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.overlap6} onChange={e => this.setState({ filter: { ...this.state.filter, overlap6: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.overlap7} onChange={e => this.setState({ filter: { ...this.state.filter, overlap7: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.overlap8} onChange={e => this.setState({ filter: { ...this.state.filter, overlap8: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.excess} onChange={e => this.setState({ filter: { ...this.state.filter, excess: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.retour} onChange={e => this.setState({ filter: { ...this.state.filter, retour: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.totalUsage} onChange={e => this.setState({ filter: { ...this.state.filter, totalUsage: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.cuttingPlanId} onChange={e => this.setState({ filter: { ...this.state.filter, cuttingPlanId: e.target.value != "" ? e.target.value : null } })} /></th>
								<th><input type='text' style={{width: "100%"}} value={this.state.filter.cmsId} onChange={e => this.setState({ filter: { ...this.state.filter, cmsId: e.target.value != "" ? e.target.value : null } })} /></th>
							</tr>
						</thead>
						<tbody>
							{this.state.rapportOverlap ===null ? <tr><td colSpan='26' className='text-center'>Loading...</td></tr> :
							this.state.rapportOverlap.length === 0 ? <tr><td colSpan='26' className='text-center'>No data</td></tr> :
							this.state.rapportOverlap
							// show only the ones that rescpect the filter
							.filter(item => (this.state.filter.cuttingRequest_sequence == null || (item.cuttingRequest_sequence && item.cuttingRequest_sequence.toString().toLowerCase().includes(this.state.filter.cuttingRequest_sequence.toLowerCase())))
							&& (this.state.filter.cuttingRequestSerie_serie == null || (item.cuttingRequestSerie_serie && item.cuttingRequestSerie_serie.toString().toLowerCase().includes(this.state.filter.cuttingRequestSerie_serie.toLowerCase())))
							&& (this.state.filter.quantite == null || (item.quantite && item.quantite.toString().toLowerCase().includes(this.state.filter.quantite.toLowerCase())))
							&& (this.state.filter.partNumbers == null || (item.partNumbers && item.partNumbers.toString().toLowerCase().includes(this.state.filter.partNumbers.toLowerCase())))
							&& (this.state.filter.placement == null || (item.placement && item.placement.toString().toLowerCase().includes(this.state.filter.placement.toLowerCase())))
							&& (this.state.filter.longueurPremierCouche == null || (item.longueurPremierCouche && item.longueurPremierCouche.toString().toLowerCase().includes(this.state.filter.longueurPremierCouche.toLowerCase())))
							&& (this.state.filter.nbrCouche == null || (item.nbrCouche && item.nbrCouche.toString().toLowerCase().includes(this.state.filter.nbrCouche.toLowerCase())))
							&& (this.state.filter.laizeMesure == null || (item.laizeMesure && item.laizeMesure.toString().toLowerCase().includes(this.state.filter.laizeMesure.toLowerCase())))
							&& (this.state.filter.laize == null || (item.laize && item.laize.toString().toLowerCase().includes(this.state.filter.laize.toLowerCase())))
							&& (this.state.filter.confirmReftissu == null || (item.confirmReftissu && item.confirmReftissu.toString().toLowerCase().includes(this.state.filter.confirmReftissu.toLowerCase())))
							&& (this.state.filter.description == null || (item.description && item.description.toString().toLowerCase().includes(this.state.filter.description.toLowerCase())))
							&& (this.state.filter.createdAt == null || (item.createdAt && item.createdAt.toString().toLowerCase().includes(this.state.filter.createdAt.toLowerCase())))
							&& (this.state.filter.tableMatelassage == null || (item.tableMatelassage && item.tableMatelassage.toString().toLowerCase().includes(this.state.filter.tableMatelassage.toLowerCase())))
							&& (this.state.filter.tableCoupe == null || (item.tableCoupe && item.tableCoupe.toString().toLowerCase().includes(this.state.filter.tableCoupe.toLowerCase())))
							&& (this.state.filter.drill1 == null || (item.drill1 && item.drill1.toString().toLowerCase().includes(this.state.filter.drill1.toLowerCase())))
							&& (this.state.filter.drill2 == null || (item.drill2 && item.drill2.toString().toLowerCase().includes(this.state.filter.drill2.toLowerCase())))
							&& (this.state.filter.matelassageEndroit == null || (item.matelassageEndroit && item.matelassageEndroit.toString().toLowerCase().includes(this.state.filter.matelassageEndroit.toLowerCase())))
							&& (this.state.filter.matelasseur == null || (item.matelasseur && item.matelasseur.toString().toLowerCase().includes(this.state.filter.matelasseur.toLowerCase())))
							&& (this.state.filter.nbrCoucheTotal == null || (item.nbrCoucheTotal && item.nbrCoucheTotal.toString().toLowerCase().includes(this.state.filter.nbrCoucheTotal.toLowerCase())))
							&& (this.state.filter.overlap1 == null || (item.overlap1 && item.overlap1.toString().toLowerCase().includes(this.state.filter.overlap1.toLowerCase())))
							&& (this.state.filter.overlap2 == null || (item.overlap2 && item.overlap2.toString().toLowerCase().includes(this.state.filter.overlap2.toLowerCase())))
							&& (this.state.filter.overlap3 == null || (item.overlap3 && item.overlap3.toString().toLowerCase().includes(this.state.filter.overlap3.toLowerCase())))
							&& (this.state.filter.overlap4 == null || (item.overlap4 && item.overlap4.toString().toLowerCase().includes(this.state.filter.overlap4.toLowerCase())))
							&& (this.state.filter.overlap5 == null || (item.overlap5 && item.overlap5.toString().toLowerCase().includes(this.state.filter.overlap5.toLowerCase())))
							&& (this.state.filter.overlap6 == null || (item.overlap6 && item.overlap6.toString().toLowerCase().includes(this.state.filter.overlap6.toLowerCase())))
							&& (this.state.filter.overlap7 == null || (item.overlap7 && item.overlap7.toString().toLowerCase().includes(this.state.filter.overlap7.toLowerCase())))
							&& (this.state.filter.overlap8 == null || (item.overlap8 && item.overlap8.toString().toLowerCase().includes(this.state.filter.overlap8.toLowerCase())))
							&& (this.state.filter.excess == null || (item.excess && item.excess.toString().toLowerCase().includes(this.state.filter.excess.toLowerCase())))
							&& (this.state.filter.retour == null || (item.retour && item.retour.toString().toLowerCase().includes(this.state.filter.retour.toLowerCase())))
							&& (this.state.filter.totalUsage == null || (item.totalUsage && item.totalUsage.toString().toLowerCase().includes(this.state.filter.totalUsage.toLowerCase())))
							&& (this.state.filter.cuttingPlanId == null || (item.cuttingPlanId && item.cuttingPlanId.toString().toLowerCase().includes(this.state.filter.cuttingPlanId.toLowerCase())))
							&& (this.state.filter.cmsId == null || (item.cmsId && item.cmsId.toString().toLowerCase().includes(this.state.filter.cmsId.toLowerCase())))
							)
							.slice(0, 1000)
							.map((item, index) => (
								<tr key={index}>
									<td>{item.cuttingRequest_sequence}</td>
									<td>{item.cuttingRequestSerie_serie}</td>
									<td>{item.quantite}</td>
									<td>{item.partNumbers}</td>
									<td>{item.placement}</td>
									<td>{item.longueurPremierCouche}</td>
									<td>{item.nbrCouche}</td>
									<td>{item.laizeMesure}</td>
									<td>{item.laize}</td>
									<td>{item.confirmReftissu}</td>
									<td>{item.description}</td>
									<td>{item.createdAt}</td>
									<td>{item.tableMatelassage}</td>
									<td>{item.tableCoupe}</td>
									<td>{item.drill1}</td>
									<td>{item.drill2}</td>
									<td>{item.matelassageEndroit}</td>
									<td>{item.matelasseur}</td>
									<td>{item.nbrCoucheTotal}</td>
									<td>{item.overlap1}</td>
									<td>{item.overlap2}</td>
									<td>{item.overlap3}</td>
									<td>{item.overlap4}</td>
									<td>{item.overlap5}</td>
									<td>{item.overlap6}</td>
									<td>{item.overlap7}</td>
									<td>{item.overlap8}</td>
									<td>{item.excess}</td>
									<td>{item.retour}</td>
									<td>{item.totalUsage}</td>
									<td>{item.cuttingPlanId}</td>
									<td>{item.cmsId}</td>
								</tr>
							))}
						</tbody>
					</table>
				</div>
			</div>
		)
	}
}
