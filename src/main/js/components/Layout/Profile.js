import axios from 'axios';
import React, { Component } from 'react'
import { connect } from 'react-redux';
import "../../styles/Profile.scss"
import PropTypes from 'prop-types';
import {logout} from '../../actions/securityAction';


class Profile extends Component {

	constructor() {
		super();
		this.state = {
			modal:{}
		}
	}

	componentDidMount() {
		if(this.props.security) {
			axios.get(`/api/user/${this.props.security.user.matricule}`)
			.then(res=> {
				this.setState({modal: {...res.data, password: null}})
			})
			.catch(err => {
				if(err.response && err.response.data != null && err.response.data.username === "Invalid Username") {
					window.location.pathname= "/login";
				}
			})
		}
	}

	renderErrorsAlert(errors) {
    let arr = []
    for (let prop in errors) {
      arr.push(<li>{prop}: {errors[prop]}</li>)
    }
    return arr
  }

	logout(){
		this.props.logout();
		window.location.href = "/login"; 
	}

	submitForm = () => {
		axios.post(`/api/user/changePsw`, this.state.modal)
			.then(res=> {
				this.logout()
			})
			.catch(err=> {
				if (err.response && typeof err.response.data === 'string') {
					this.setState({
						error: {
							...this.state.error,
							errorMessage: err.response.data
						}
					})
				} else {
					this.setState({
						error: (err.response && err.response.data) || { errorMessage: "Serveur injoignable — réessayez" }
					})
				}
			})
	}

  render() {
    return (
      <div className='p-4' style={{fontSize: 16, maxHeight: "100vh", overflowY: "auto"}}>
        <h1 className='text-center'>Mon profile</h1>
				<div className='row'>
					<div className='text-right col-lg-6 '>username :</div>
					<div className='col-lg-6'>{this.state.modal.username}</div>
				</div>
				<div className='row'>
					<div className='text-right col-lg-6 '>firstName :</div>
					<div className='col-lg-6'>{this.state.modal.firstName}</div>
				</div>
				<div className='row'>
					<div className='text-right col-lg-6 '>lastName :</div>
					<div className='col-lg-6'>{this.state.modal.lastName}</div>
				</div>
				<div className='row'>
					<div className='text-right col-lg-6 '>email :</div>
					<div className='col-lg-6'>{this.state.modal.email}</div>
				</div>
				{this.state.modal.roles && <div className='row'>
					<div className='text-right col-lg-6'>roles :</div>
					<ul className='col-lg-6 m-0'>
						{this.state.modal.roles.map(e=><li>{e.description}</li>)}
					</ul>
				</div>}
				<div className='row'>
					<div className='text-right col-lg-6 '>matricule :</div>
					<div className='col-lg-6'>{this.state.modal.matricule}</div>
				</div>
				<div className='row'>
					<div className='text-right col-lg-6 '>fonction :</div>
					<div className='col-lg-6'>{this.state.modal.fonction}</div>
				</div>
				<div className='row'>
					<div className='text-right col-lg-6 '>created_At :</div>
					<div className='col-lg-6'>{this.state.modal.created_At}</div>
				</div>
				<div className='row'>
					<div className='text-right col-lg-6 '>updated_At :</div>
					<div className='col-lg-6'>{this.state.modal.updated_At}</div>
				</div>
				
				<hr/>
        <div className='row mb-2'>
					<label className='col-lg-6 col-form-label text-right'>password</label>
					<input className='form-control input-sm col-lg-3 profile_input_psw' type="password" autoComplete="new-password"
					value={this.state.modal.password || ""}
						onChange={(event) => { this.setState({ modal: { ...this.state.modal, password: event.target.value } }) }}
						onKeyUp={(e) => {
							if (e.key === "Enter") {
								this.confirmPasswordInput.focus()
							}
						}}
						/>
        </div>
				<div className='row mb-2'>
					<label className='col-lg-6 col-form-label text-right'>confirmPassword</label>
					<input className='form-control input-sm col-lg-3 profile_input_psw' type="password" autoComplete="new-password"
						value={this.state.modal.confirmPassword || ""}  ref={input => this.confirmPasswordInput = input}
						onChange={(event) => { this.setState({ modal: { ...this.state.modal, confirmPassword: event.target.value } }) }}
						onKeyUp={(e) => {
							if (e.key === "Enter") {
								this.submitForm()
							}
						}}
						/>
        </div>
				{(this.state.error && Object.keys(this.state.error).length !== 0) && <div className="alert alert-danger text-center m-4" role="alert">
					<ul>{this.renderErrorsAlert(this.state.error)}</ul>
				</div>}
				<div className='row'>
					<div className='col-6'></div>
					<button
						type="button" className='btn btn-success col-3' 
						onClick={() => {
							this.submitForm()
						}}
					>Enregistrer</button>
				</div>
      </div>
    )
  }
}

Profile.propTypes = { 
	logout: PropTypes.func.isRequired,
  security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
  security: state.security
})

export default connect(mapStateToProps, {logout}) (Profile);