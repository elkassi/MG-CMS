import axios from 'axios'
import React, { Component } from 'react'

export default class ValidationDefautRouleau extends Component {

	constructor(props) {
		super(props)
		this.state = {
			rouleauList: [],
			filter: {}
		}
	}

	componentDidMount() {
		this.loadRouleauList()
	}

	loadRouleauList() {
		axios.get("/api/cuttingRequestSerieRouleauData/defaut")
			.then(response => {
				this.setState({ rouleauList: response.data })
			})
			.catch(error => {
				console.log(error)
			})
	}

	renderTable() {
		return <div className='table-responsive entity-table  mb-2'
			style={{ maxHeight: `calc(100vh - 69px)` }}
		>
			<table className='table table-bordered table-elements-sm m-0'>
				<thead className='entity-table-header'>
					<tr>
						<th>Valider</th>
						<th>Defaut</th>
						<th>Serie</th>
						<th>Reftissu</th>
						<th>Id Rouleau</th>
						<th>Lot Frs</th>
						<th>Metrage</th>
						<th>Laize</th>
						<th>Nbr Couche</th>
						<th>Longueur 1ere Couche</th>
						<th>Longueur Couche Overlap</th>
						<th>Defaut</th>
						<th>Non Utilise</th>
						<th>Retour</th>
						<th>Excess</th>
						<th>Overlap 1</th>
						<th>Overlap 2</th>
						<th>Overlap 3</th>
						<th>Overlap 4</th>
						<th>Overlap 5</th>
						<th>Overlap 6</th>
						<th>Overlap 7</th>
						<th>Overlap 8</th>
						<th>Total Usage</th>
						<th></th>
					</tr>
					<tr>
						<th></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.defautCode} onChange={e => this.setState({ filter: { ...this.state.filter, defautCode: e.target.value != "" ? e.target.value : null } })} /></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.serie} onChange={e => this.setState({ filter: { ...this.state.filter, serie: e.target.value != "" ? e.target.value : null } })} /></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.confirmReftissu} onChange={e => this.setState({ filter: { ...this.state.filter, confirmReftissu: e.target.value != "" ? e.target.value : null } })} /></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.idRouleau} onChange={e => this.setState({ filter: { ...this.state.filter, idRouleau: e.target.value != "" ? e.target.value : null } })} /></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.lotFrs} onChange={e => this.setState({ filter: { ...this.state.filter, lotFrs: e.target.value != "" ? e.target.value : null } })} /></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.metrage} onChange={e => this.setState({ filter: { ...this.state.filter, metrage: e.target.value != "" ? e.target.value : null } })} /></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.laize} onChange={e => this.setState({ filter: { ...this.state.filter, laize: e.target.value != "" ? e.target.value : null } })} /></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.nbrCouche} onChange={e => this.setState({ filter: { ...this.state.filter, nbrCouche: e.target.value != "" ? e.target.value : null } })} /></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.longueurPremierCouche} onChange={e => this.setState({ filter: { ...this.state.filter, longueurPremierCouche: e.target.value != "" ? e.target.value : null } })} /></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.longueurCoucheOverlap} onChange={e => this.setState({ filter: { ...this.state.filter, longueurCoucheOverlap: e.target.value != "" ? e.target.value : null } })} /></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.defaut} onChange={e => this.setState({ filter: { ...this.state.filter, defaut: e.target.value != "" ? e.target.value : null } })} /></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.nonUtitlse} onChange={e => this.setState({ filter: { ...this.state.filter, nonUtitlse: e.target.value != "" ? e.target.value : null } })} /></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.retour} onChange={e => this.setState({ filter: { ...this.state.filter, retour: e.target.value != "" ? e.target.value : null } })} /></th>
						<th><input type='text' style={{ width: "100%" }} value={this.state.filter.excess} onChange={e => this.setState({ filter: { ...this.state.filter, excess: e.target.value != "" ? e.target.value : null } })} /></th>
						<th></th>
						<th></th>
						<th></th>
						<th></th>
						<th></th>
						<th></th>
						<th></th>
						<th></th>
						<th></th>
						<th></th>
					</tr>
				</thead>
				<tbody>
					{this.state.rouleauList
						.filter(item => (this.state.filter.serie == null || (item.serie && item.serie.toString().toLowerCase().includes(this.state.filter.serie.toLowerCase())))
						//defautCode
						&& (this.state.filter.defautCode == null || (item.defautCode && item.defautCode.toString().toLowerCase().includes(this.state.filter.defautCode.toLowerCase())))
							&& (this.state.filter.confirmReftissu == null || (item.confirmReftissu && item.confirmReftissu.toString().toLowerCase().includes(this.state.filter.confirmReftissu.toLowerCase())))
							&& (this.state.filter.idRouleau == null || (item.idRouleau && item.idRouleau.toString().toLowerCase().includes(this.state.filter.idRouleau.toLowerCase())))
							&& (this.state.filter.lotFrs == null || (item.lotFrs && item.lotFrs.toString().toLowerCase().includes(this.state.filter.lotFrs.toLowerCase())))
							&& (this.state.filter.metrage == null || (item.metrage && item.metrage.toString().toLowerCase().includes(this.state.filter.metrage.toLowerCase())))
							&& (this.state.filter.laize == null || (item.laize && item.laize.toString().toLowerCase().includes(this.state.filter.laize.toLowerCase())))
							&& (this.state.filter.nbrCouche == null || (item.nbrCouche && item.nbrCouche.toString().toLowerCase().includes(this.state.filter.nbrCouche.toLowerCase())))
							&& (this.state.filter.longueurPremierCouche == null || (item.longueurPremierCouche && item.longueurPremierCouche.toString().toLowerCase().includes(this.state.filter.longueurPremierCouche.toLowerCase())))
							&& (this.state.filter.longueurCoucheOverlap == null || (item.longueurCoucheOverlap && item.longueurCoucheOverlap.toString().toLowerCase().includes(this.state.filter.longueurCoucheOverlap.toLowerCase())))
							&& (this.state.filter.defaut == null || (item.defaut && item.defaut.toString().toLowerCase().includes(this.state.filter.defaut.toLowerCase())))
							&& (this.state.filter.nonUtitlse == null || (item.nonUtitlse && item.nonUtitlse.toString().toLowerCase().includes(this.state.filter.nonUtitlse.toLowerCase())))
							&& (this.state.filter.retour == null || (item.retour && item.retour.toString().toLowerCase().includes(this.state.filter.retour.toLowerCase())))
							&& (this.state.filter.excess == null || (item.excess && item.excess.toString().toLowerCase().includes(this.state.filter.excess.toLowerCase()))))
						.map((rouleau, index) => (
							<tr key={index}>
								<td style={{ padding: 3 }}>
									<button className="btn btn-outline-success btn-sm" onClick={() => {
										axios.post("/api/cuttingRequestSerieRouleauData/valider/" + rouleau.id)
											.then(response => {
												this.loadRouleauList();
											})
											.catch(error => {
												console.log(error);
											});
									}}>Valider</button>
								</td>
								<td>{rouleau.defautCode}</td>
								<td>{rouleau.serie}</td>
								<td>{rouleau.confirmReftissu}</td>
								<td>{rouleau.idRouleau}</td>
								<td>{rouleau.lotFrs}</td>
								<td>{rouleau.metrage}</td>
								<td>{rouleau.laize}</td>
								<td>{rouleau.nbrCouche}</td>
								<td>{rouleau.longueurPremierCouche}</td>
								<td>{rouleau.longueurCoucheOverlap}</td>
								<td>{rouleau.defaut}</td>
								<td>{rouleau.nonUtitlse}</td>
								<td>{rouleau.retour}</td>
								<td>{rouleau.excess}</td>
								<td>{rouleau.overlap1}</td>
								<td>{rouleau.overlap2}</td>
								<td>{rouleau.overlap3}</td>
								<td>{rouleau.overlap4}</td>
								<td>{rouleau.overlap5}</td>
								<td>{rouleau.overlap6}</td>
								<td>{rouleau.overlap7}</td>
								<td>{rouleau.overlap8}</td>
								<td>{rouleau.totalUsage}</td>
								<td style={{ padding: 3 }}>
									<button className="btn btn-outline-success btn-sm" onClick={() => {
										axios.post("/api/cuttingRequestSerieRouleauData/valider/" + rouleau.id)
											.then(response => {
												this.loadRouleauList();
											})
											.catch(error => {
												console.log(error);
											});
									}}>Valider</button>
								</td>
							</tr>
						))}
				</tbody>
			</table>
		</div>

	}

	render() {
		return (
			<div>
				<h2 className="text-center my-3">Validation des défauts du rouleau</h2>
				{this.renderTable()}
			</div>
		)
	}
}
