import React, { Component } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faRightFromBracket, faChevronRight, faBars } from '@fortawesome/free-solid-svg-icons';
import logo from '../../assets/images/lear_logo.png';
import { logout } from '../../actions/securityAction';
import '../../styles/TopBar.scss';

/** Human label for a route segment (camelCase / kebab → spaced, capitalised). */
function prettifySegment(segment) {
    if (!segment) return '';
    const decoded = decodeURIComponent(segment)
        .replace(/[-_]+/g, ' ')
        .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
        .trim();
    return decoded.charAt(0).toUpperCase() + decoded.slice(1);
}

/**
 * Branded application top bar: Lear lockup + product title + breadcrumb of the
 * current route + the signed-in user and logout. Rendered once by SecuredRoute
 * above the routed screen, so every page gains consistent chrome with no
 * per-screen change.
 */
class TopBar extends Component {
    render() {
        const { location, security } = this.props;
        const path = location ? location.pathname : '/';
        const segments = path.split('/').filter(Boolean);
        const crumb = segments.length ? prettifySegment(segments[0]) : 'Accueil';
        const user = security && security.user ? security.user : null;
        const userName = user ? (user.nom || user.matricule || user.username || '') : '';

        return (
            <header className="appbar">
                <button className="appbar-burger" onClick={() => this.props.onToggleMenu && this.props.onToggleMenu()} title="Menu">
                    <FontAwesomeIcon icon={faBars} />
                </button>
                <div className="appbar-brand">
                    <img className="appbar-logo" src={logo} alt="Lear" />
                    <div className="appbar-title">
                        <span className="appbar-product">Trim Tangier</span>
                        <span className="appbar-sub">Cutting Management</span>
                    </div>
                </div>

                <nav className="appbar-crumbs" aria-label="breadcrumb">
                    <span className="appbar-crumb-root">Lear</span>
                    <FontAwesomeIcon icon={faChevronRight} className="appbar-crumb-sep" />
                    <span className="appbar-crumb-current">{crumb}</span>
                </nav>

                <div className="appbar-right">
                    {userName && <span className="appbar-user">{userName}</span>}
                    <button className="appbar-logout" onClick={() => this.props.logout()} title="Se déconnecter">
                        <FontAwesomeIcon icon={faRightFromBracket} />
                    </button>
                </div>
            </header>
        );
    }
}

TopBar.propTypes = {
    location: PropTypes.object,
    security: PropTypes.object.isRequired,
    logout: PropTypes.func.isRequired,
    onToggleMenu: PropTypes.func,
};

const mapStateToProps = state => ({ security: state.security });

export default connect(mapStateToProps, { logout })(TopBar);
