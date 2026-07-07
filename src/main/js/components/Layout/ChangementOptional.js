import React, { Component } from 'react'
import axios from 'axios'
import Select from "react-select"
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faExchange, faMagnifyingGlass, faSave, faSpinner } from '@fortawesome/free-solid-svg-icons'
import { Modal } from 'react-bootstrap'

export default class ChangementOptional extends Component {

	constructor(props) {
		super(props)
		this.state = {
			sequence: '',
			cuttingPlanId: null,
			placements: [],
			selectedPlacement: null,
			optionalsList: [],
			loading: false,
			error: null,
			success: null,
			showConfirmModal: false,
			selectedOption: null
		}
	}

	searchSequence = async () => {
		if (!this.state.sequence) return
		
		this.setState({ loading: true, error: null, placements: [], selectedPlacement: null, optionalsList: [] })
		
		try {
			// Search for cutting plan by sequence
			const res = await axios.get(`/api/cuttingRequestData/all?startWith.sequence=${this.state.sequence}&page=0&size=20&sort=createdAt,desc`)
			
			if (res.data.content.length === 0) {
				this.setState({ error: "Aucun plan trouvé pour cette séquence", loading: false })
				return
			}

			const cuttingPlan = res.data.content[0]
			this.setState({ cuttingPlanId: cuttingPlan.cuttingPlanId })

			// Get placements with optionals
			const resOptionnal = await axios.get(`/api/cuttingPlanMaterialPlacementData/cuttingPlan/${cuttingPlan.cuttingPlanId}`)
			
			// Group placements that have optionals (same groupPlacement, different machines)
			const placementGroups = {}
			resOptionnal.data.forEach(cpmp => {
				const key = `${cpmp.partNumberMaterial}-${cpmp.groupPlacement}`
				if (!placementGroups[key]) {
					placementGroups[key] = []
				}
				placementGroups[key].push(cpmp)
			})

			// Filter to only include placements with optionals (more than 1 option)
			const placementsWithOptionals = Object.entries(placementGroups)
				.filter(([key, items]) => items.length > 1)
				.map(([key, items]) => ({
					key,
					partNumberMaterial: items[0].partNumberMaterial,
					groupPlacement: items[0].groupPlacement,
					options: items.sort((a, b) => (a.machine || '').localeCompare(b.machine || ''))
				}))

			this.setState({ 
				placements: placementsWithOptionals, 
				loading: false 
			})

		} catch (err) {
			console.error("Error:", err)
			this.setState({ error: "Erreur lors de la recherche", loading: false })
		}
	}

	selectPlacement = (placement) => {
		this.setState({
			selectedPlacement: placement,
			optionalsList: placement.options,
			selectedOption: null
		})
		
		// Scroll to the form section with a slight delay to ensure rendering
		setTimeout(() => {
			const formSection = document.getElementById('optional-form-section')
			if (formSection) {
				formSection.scrollIntoView({ behavior: 'smooth', block: 'start' })
			}
		}, 100)
	}

	handleSwitch = async () => {
		if (!this.state.selectedOption || !this.state.selectedPlacement) return
		
		this.setState({ loading: true, error: null, success: null })
		
		try {
			// Call API to switch the optional
			await axios.post(`/api/cuttingPlanMaterialPlacement/switchOptional`, {
				cuttingPlanId: this.state.cuttingPlanId,
				groupPlacement: this.state.selectedPlacement.groupPlacement,
				partNumberMaterial: this.state.selectedPlacement.partNumberMaterial,
				newPlacementId: this.state.selectedOption.id
			})
			
			this.setState({ 
				success: "Changement effectué avec succès", 
				loading: false,
				showConfirmModal: false 
			})
			
			// Refresh the data
			this.searchSequence()
			
		} catch (err) {
			console.error("Error switching:", err)
			this.setState({ 
				error: err.response?.data || "Erreur lors du changement", 
				loading: false,
				showConfirmModal: false 
			})
		}
	}

	renderConfirmModal = () => {
		return <Modal
			show={this.state.showConfirmModal}
			onHide={() => this.setState({ showConfirmModal: false })}
			centered
		>
			<Modal.Header closeButton>
				<Modal.Title>Confirmer le changement</Modal.Title>
			</Modal.Header>
			<Modal.Body>
				<p>Êtes-vous sûr de vouloir changer vers:</p>
				{this.state.selectedOption && (
					<div className="alert alert-info">
						<strong>Placement:</strong> {this.state.selectedOption.placement}<br/>
						<strong>Machine:</strong> {this.state.selectedOption.machine || 'DIE'}<br/>
						<strong>Longueur:</strong> {this.state.selectedOption.longueur}
					</div>
				)}
			</Modal.Body>
			<Modal.Footer>
				<button 
					className="btn btn-secondary" 
					onClick={() => this.setState({ showConfirmModal: false })}
				>
					Annuler
				</button>
				<button 
					className="btn btn-primary" 
					onClick={this.handleSwitch}
					disabled={this.state.loading}
				>
					{this.state.loading ? <FontAwesomeIcon icon={faSpinner} spin /> : 'Confirmer'}
				</button>
			</Modal.Footer>
		</Modal>
	}

	render() {
		return (
			<div style={{ padding: 20 }}>
				<h1 className='text-center mb-4'>Changement des Optionnels (DIE/Lectra)</h1>
				
				{/* Search Section */}
				<div className='row mb-4'>
					<div className='col-12'>
						<div className='card'>
							<div className='card-body'>
								<div className='row align-items-center'>
									<label className='col-2 col-form-label text-right'>Séquence :</label>
									<div className='col-4'>
										<input 
											type='text' 
											className='form-control' 
											value={this.state.sequence} 
											onChange={(e) => this.setState({ sequence: e.target.value })}
											onKeyPress={(e) => e.key === 'Enter' && this.searchSequence()}
											placeholder="Entrer la séquence..."
										/>
									</div>
									<div className='col-2'>
										<button 
											className='btn btn-primary' 
											onClick={this.searchSequence}
											disabled={this.state.loading}
										>
											{this.state.loading 
												? <FontAwesomeIcon icon={faSpinner} spin /> 
												: <><FontAwesomeIcon icon={faMagnifyingGlass} /> Rechercher</>
											}
										</button>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>

				{/* Error/Success Messages */}
				{this.state.error && (
					<div className="alert alert-danger" role="alert">
						{this.state.error}
					</div>
				)}
				{this.state.success && (
					<div className="alert alert-success" role="alert">
						{this.state.success}
					</div>
				)}

				{/* Placements with optionals */}
				{this.state.placements.length > 0 && (
					<div className='row mb-4'>
						<div className='col-12'>
							<div className='card'>
								<div className='card-header'>
									<h5>Placements avec optionnels</h5>
								</div>
								<div className='card-body'>
									<table className='table table-bordered table-hover'>
										<thead className='thead-light'>
											<tr>
												<th>Matière</th>
												<th>Groupe Placement</th>
												<th>Nombre d'options</th>
												<th>Machines disponibles</th>
												<th>Action</th>
											</tr>
										</thead>
										<tbody>
											{this.state.placements.map((p, idx) => (
												<tr 
													key={idx} 
													className={this.state.selectedPlacement?.key === p.key ? 'table-active' : ''}
													style={{ cursor: 'pointer' }}
													onClick={() => this.selectPlacement(p)}
												>
													<td>{p.partNumberMaterial}</td>
													<td>{p.groupPlacement}</td>
													<td>{p.options.length}</td>
													<td>{p.options.map(o => o.machine || 'DIE').join(', ')}</td>
													<td>
														<button 
															className='btn btn-sm btn-outline-primary'
															onClick={(e) => {
																e.stopPropagation()
																this.selectPlacement(p)
															}}
														>
															<FontAwesomeIcon icon={faExchange} /> Sélectionner
														</button>
													</td>
												</tr>
											))}
										</tbody>
									</table>
								</div>
							</div>
						</div>
					</div>
				)}

				{/* Optional selection form - Fixed scroll issue by using id for scrollIntoView */}
				{this.state.selectedPlacement && (
					<div id="optional-form-section" className='row mb-4' style={{ scrollMarginTop: '20px' }}>
						<div className='col-12'>
							<div className='card border-primary'>
								<div className='card-header bg-primary text-white'>
									<h5>Sélection de l'optionnel - {this.state.selectedPlacement.partNumberMaterial}</h5>
								</div>
								<div className='card-body'>
									<table className='table table-bordered'>
										<thead className='thead-light'>
											<tr>
												<th>Sélection</th>
												<th>Placement</th>
												<th>Machine</th>
												<th>Longueur</th>
												<th>Laize</th>
												<th>Nbr Couche</th>
												<th>Config</th>
											</tr>
										</thead>
										<tbody>
											{this.state.optionalsList.map((opt, idx) => (
												<tr 
													key={idx}
													className={this.state.selectedOption?.id === opt.id ? 'table-success' : ''}
												>
													<td>
														<input 
															type="radio" 
															name="optionalSelect"
															checked={this.state.selectedOption?.id === opt.id}
															onChange={() => this.setState({ selectedOption: opt })}
														/>
													</td>
													<td>{opt.placement}</td>
													<td>
														<span className={`badge ${opt.machine?.includes('Lectra') ? 'badge-info' : 'badge-secondary'}`}>
															{opt.machine || 'DIE'}
														</span>
													</td>
													<td>{opt.longueur}</td>
													<td>{opt.laize}</td>
													<td>{opt.nbrCouche}</td>
													<td>{opt.config}</td>
												</tr>
											))}
										</tbody>
									</table>
									
									<div className='text-center mt-3'>
										<button 
											className='btn btn-success btn-lg'
											disabled={!this.state.selectedOption || this.state.loading}
											onClick={() => this.setState({ showConfirmModal: true })}
										>
											<FontAwesomeIcon icon={faSave} /> Appliquer le changement
										</button>
									</div>
								</div>
							</div>
						</div>
					</div>
				)}

				{this.renderConfirmModal()}
			</div>
		)
	}
}
