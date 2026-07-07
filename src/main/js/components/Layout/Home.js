import React, { Component } from 'react'
import { connect } from 'react-redux';
import logo from '../../assets/images/lear_logo.png'


class Home extends Component {

  render() {
    const user = this.props.security.user || {};
    const roles = (user.roles || []).map(r => r.authority.replace("ROLE_", "").replace(/_/g, " "));
    return (
      <div style={{ display: "flex", alignItems: "center", justifyContent: "center", minHeight: "80vh" }}>
        <div style={{ textAlign: "center", maxWidth: 560, padding: "32px 24px" }}>
          <img src={logo} alt="LEAR Logo" height="90" />
          <h2 style={{ margin: "24px 0 4px 0" }}>
            Bienvenue{user.firstName ? `, ${user.firstName} ${user.lastName || ""}` : ""}
          </h2>
          <p style={{ color: "#666", margin: "0 0 16px 0" }}>
            Système de gestion et traçabilité des processus de découpe
          </p>
          {roles.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              {roles.map(r => (
                <span key={r} style={{
                  display: "inline-block", margin: 3, padding: "3px 10px",
                  borderRadius: 12, backgroundColor: "#f1f1f1", color: "#444", fontSize: 13
                }}>{r}</span>
              ))}
            </div>
          )}
          <p style={{ color: "#888" }}>
            Utilisez le menu à gauche pour accéder à vos écrans.
          </p>
        </div>
      </div>
    )
  }
}

const mapStateToProps = state => ({
  security: state.security
})

export default connect(mapStateToProps, null)(Home);
