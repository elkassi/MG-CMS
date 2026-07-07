import React, { useState } from 'react'
import {Route, Redirect} from 'react-router-dom'
import {connect} from 'react-redux';
import PropTypes from 'prop-types';
import Dashboard from '../components/Dashboard';
import TopBar from '../components/Layout/TopBar';
import '../styles/dashboard.scss'

const SecuredRoute = ({ component: Component, security, allowedRoles, ...otherProps }) => {
    const [showMenu, setShowMenu] = useState(() => {
        // Initialize from localStorage, default to true
        const saved = localStorage.getItem('showMenu');
        return saved !== null ? JSON.parse(saved) : true;
    });

    // Function to handle menu toggle and save to localStorage
    const toggleMenu = () => {
        const newShowMenu = !showMenu;
        setShowMenu(newShowMenu);
        localStorage.setItem('showMenu', JSON.stringify(newShowMenu));
    };

    // ROLE_ADMIN is implicitly allowed on every guarded route.
    const userAuthorities = (security.user && security.user.roles)
        ? security.user.roles.map(r => r.authority)
        : [];
    const hasAccess = !allowedRoles || allowedRoles.length === 0
        || userAuthorities.includes("ROLE_ADMIN")
        || allowedRoles.some(r => userAuthorities.includes(r));

    return <Route
        {...otherProps}
        render={props => {
            // Force showMenu to true if on root path "/"
            const currentShowMenu = props.location.pathname === "/" ? true : showMenu;

            if (security.validToken !== true) {
                return <Redirect to="/login" />;
            }
            if (!hasAccess) {
                return <Redirect to="/" />;
            }

            return (
                <div className='page-container'>
                    <Dashboard history={props.history} showMenu={currentShowMenu} toggleMenu={toggleMenu} />
                    <div className="content-container" style={currentShowMenu ? {width: "calc(100vw - 300px)"} : {width: "100vw"}}>
                        <TopBar location={props.location} onToggleMenu={toggleMenu} />
                        <div className="content-body">
                            <Component {...props} />
                        </div>
                    </div>

                </div>
            );
        }}
    />
};
SecuredRoute.propTypes = {
    security: PropTypes.object.isRequired,
    allowedRoles: PropTypes.arrayOf(PropTypes.string)
};

const mapStateToProps = state => ({
    security: state.security
});
  
export default connect(mapStateToProps)(SecuredRoute);