import React, { Component } from 'react'
import '../../styles/landing.scss'
import logo from '../../assets/images/lear_logo.png'
import { connect } from 'react-redux'
import PropTypes from 'prop-types';
import { login } from '../../actions/securityAction';
import classnames from 'classnames';
import axios from 'axios';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSpinner, faEye, faEyeSlash, faUser, faLock, faEnvelope, faCut, faClipboardCheck } from '@fortawesome/free-solid-svg-icons'

class Landing extends Component {

  constructor() {
    super();
    this.state = {
      username: localStorage.username || "",
      password: localStorage.password || "",
      remembreMe: (localStorage.remembreMe === "true") || false,
      errors: {},
      showPassword: false,
      isLoading: false
    }
    this.onChange = this.onChange.bind(this);
    this.onSubmit = this.onSubmit.bind(this);
  }

  componentDidMount() {
    if (this.props.security.validToken) {
      this.props.history.push('/');
    }

  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.security.validToken) {
      this.props.history.push("/");
    }

    if (nextProps.errors) {
      this.setState({ errors: nextProps.errors, isLoading: false });
    }
  }

  onSubmit(e) {
    e.preventDefault();
    this.setState({ isLoading: true });
    const LoginRequest = {
      username: this.state.username,
      password: this.state.password,
      remembreMe: this.state.remembreMe
    };
    this.props.login(LoginRequest);
  }

  onChange(e) {
    this.setState({ [e.target.name]: e.target.value })
  }


  render() {
    const { errors } = this.state;
    // The backend answers a failed login with a plain string (401) or a Spring
    // error object (bad credentials) — neither carries username/password keys,
    // so without this banner the form gave no feedback at all.
    const generalError = (typeof errors === "string" && errors.length > 0)
      || (errors && errors.status && !errors.username && !errors.password)
      ? "Identifiants incorrects ou compte inactif"
      : null;
    return (
      <div className="landing-container">
        <div className="landing-background">
          <div className="landing-overlay">
            <div className="landing-content">
              {/* Left Side - Branding */}
              <div className="landing-branding">
                <div className="landing-brand-content">
                  <img
                    src={logo}
                    alt="LEAR Logo"
                    className="landing-main-logo"
                  />
                  <p className="landing-description">
                    Système de gestion et traçabilité des processus de découpe
                  </p>
                  <div className="landing-features">
                    <div className="feature-item">
                      <FontAwesomeIcon icon={faClipboardCheck} className="feature-icon" />
                      <span>Contrôle qualité avancé</span>
                    </div>
                    <div className="feature-item">
                      <FontAwesomeIcon icon={faCut} className="feature-icon" />
                      <span>Traçabilité complète</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Right Side - Login Form */}
              <div className="landing-login-section">
                <div className="login-card">
                  <div className="login-header">
                    <h3>Bienvenue</h3>
                    <p>Veuillez vous connecter à votre compte</p>
                  </div>

                  <form noValidate onSubmit={this.onSubmit} className="login-form">
                    {/* Username Field */}
                    <div className="form-group">
                      <label htmlFor="username" className="form-label">
                        <FontAwesomeIcon icon={faUser} className="input-icon" />
                        Nom d'utilisateur
                      </label>
                      <input
                        type="text"
                        className={classnames("form-input", {
                          "form-input-error": errors.username
                        })}
                        id="username"
                        name="username"
                        placeholder="Entrez votre nom d'utilisateur"
                        value={this.state.username}
                        onChange={this.onChange}
                        autoComplete="off"
                      />
                      {errors.username && (
                        <div className="error-message">
                          <FontAwesomeIcon icon={faEnvelope} className="error-icon" />
                          {errors.username}
                        </div>
                      )}
                    </div>

                    <div className="form-group">
                      <label htmlFor="password" className="form-label">
                        <FontAwesomeIcon icon={faLock} className="input-icon" />
                        Mot de passe
                      </label>
                      <div className="password-input-container">
                        <input
                          type={this.state.showPassword ? "text" : "password"}
                          className={classnames("form-input", {
                            "form-input-error": errors.password
                          })}
                          id="password"
                          name="password"
                          value={this.state.password}
                          placeholder="Entrez votre mot de passe"
                          onChange={this.onChange}
                          autoComplete="off"
                        />
                        <button
                          type="button"
                          className="password-toggle"
                          onClick={() => this.setState({ showPassword: !this.state.showPassword })}
                        >
                          <FontAwesomeIcon icon={this.state.showPassword ? faEyeSlash : faEye} />
                        </button>
                      </div>
                      {errors.password && (
                        <div className="error-message">
                          <FontAwesomeIcon icon={faLock} className="error-icon" />
                          {errors.password}
                        </div>
                      )}
                    </div>

                    {/* Remember Me */}
                    <div className="form-options">
                      <label className="checkbox-container">
                        <input
                          type="checkbox"
                          onChange={(e) => { this.setState({ [e.target.name]: e.target.checked }) }}
                          name="remembreMe"
                          checked={this.state.remembreMe}
                          className="checkbox-input"
                        />
                        <span className="checkbox-checkmark"></span>
                        Se souvenir de moi
                      </label>
                    </div>

                    {generalError && (
                      <div className="error-message" style={{ marginBottom: 12 }}>
                        <FontAwesomeIcon icon={faLock} className="error-icon" />
                        {generalError}
                      </div>
                    )}

                    {/* Submit Button */}
                    <button
                      type="submit"
                      className="login-button"
                      disabled={this.state.isLoading}
                    >
                      {this.state.isLoading ? (
                        <>
                          <FontAwesomeIcon icon={faSpinner} spin className="button-icon" />
                          Connexion en cours...
                        </>
                      ) : (
                        <>
                          <FontAwesomeIcon icon={faLock} className="button-icon" />
                          Se connecter
                        </>
                      )}
                    </button>

                    {/* Forgot Password */}
                    {this.state.username && this.state.username.length > 0 && (
                      <div className="forgot-password">
                        <button 
                          className='forgot-password-link' 
                          type='button'
                          onClick={() => {
                            this.setState({ sendingEmailConfirmation: true })
                            axios.get(`/api/user/emailConfirmation?username=${this.state.username}`)
                              .then(res => {
                                this.setState({ sendingEmailConfirmation: false })
                                alert("Un email de confirmation a été envoyé à votre adresse email")
                              })
                              .catch(err => {
                                this.setState({ sendingEmailConfirmation: false })
                                if (err.response && err.response.status === 404) {
                                  alert("Utilisateur \"" + this.state.username + "\" introuvable")
                                } else {
                                  alert("Une erreur est survenue lors de l'envoi de l'email de confirmation!")
                                }
                              })
                          }}
                        >
                          {this.state.sendingEmailConfirmation && (
                            <FontAwesomeIcon icon={faSpinner} spin className="link-icon" />
                          )}
                          Mot de passe oublié ?
                        </button>
                      </div>
                    )}
                  </form>

                  <div className="login-footer">
                    <p>© 2026 Lear Corporation — Trim Tangier. All rights reserved.</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }
}

Landing.propTypes = {
  login: PropTypes.func.isRequired,
  errors: PropTypes.object.isRequired,
  security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
  security: state.security,
  errors: state.errors
})

export default connect(mapStateToProps, { login })(Landing);
