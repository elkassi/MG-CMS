import React, { Component } from 'react'
import '../styles/dashboard.scss'
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import logo from '../assets/images/lear_logo.png'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faAngleDown, faAngleRight, faAngleUp, faMagnifyingGlass, faBars, faBoxArchive, faBoxesStacked, faClipboard, faDrawPolygon, faEnvelope, faFolder, faFolderOpen, faGear, faGears, faHelmetSafety, faIndustry, faRightFromBracket, faUser, faUserAlt, faUsers, faHome, faDatabase, faChartLine, faScrewdriverWrench, faTableCells, faTable, faKeyboard, faPenToSquare, faEye, faCalendarAlt, faCut, faLayerGroup, faTruck, faTachometerAlt, faCog } from '@fortawesome/free-solid-svg-icons';
import { logout } from '../actions/securityAction';
import { Link } from 'react-router-dom';



class Dashboard extends Component {

  constructor() {
    super();
    this.state = {
      path: window.location.pathname,
      menuElem: null,
      isCollapsing: false,
    }
  }

  componentDidMount() {
    const { user } = this.props.security;
    if (user.roles.map(e => e.authority).includes("ROLE_ADMIN")) {
      this.setMenuElem("Database Production")
    } else if (user.roles.map(e => e.authority).includes("ROLE_IMPORTER")) {
      this.setMenuElem("Production")
    } else if (user.roles.map(e => e.authority).includes("ROLE_CAD")) {
      this.setMenuElem("CAD")
    }
  }

  setMenuElem = (elem) => {
    if (this.state.menuElem === elem) {
      this.setState({ isCollapsing: true });
      setTimeout(() => {
        this.setState({ menuElem: null, isCollapsing: false });
      }, 100);
    } else {
      this.setState({ menuElem: elem, isCollapsing: false });
    }
  }

  logout() {
    this.props.logout();
    window.location.href = "/login";
  }

  renderMenuSection = (condition, menuKey, icon, title, children, itemCount = 1) => {
    if (!condition) return null;

    const isExpanded = this.state.menuElem === menuKey;
    const hasSubItems = Array.isArray(children);

    return (
      <div className='dashboard-section'>
        {hasSubItems ? (
          <>
            <div
              className='dashboard-elem'
              onClick={() => this.setMenuElem(menuKey)}
              style={isExpanded ? { color: "#ca0c0c" } : {}}
            >
              <FontAwesomeIcon icon={icon} />
              <span>{title}</span>
              <FontAwesomeIcon
                icon={isExpanded ? faAngleUp : faAngleDown}
                size="sm"
              />
            </div>
            <ul
              className='dashboard-subelem-list'
              style={isExpanded ? { maxHeight: 39 * itemCount } : { maxHeight: "0" }}
            >
              {children}
            </ul>
          </>
        ) : (
          children
        )}
      </div>
    );
  }

  render() {
    const { user } = this.props.security;
    return (
      <div className='dashboard-container' style={this.props.showMenu ? { width: '300px' } : { width: '0px' }}>
        <div className="dashboard-scrollable-content">
          <div className='dashboard-logo'>
            <img
              src={logo}
              alt="LEAR Logo"
              height="100"
              onClick={() => {
                this.props.history.push("/");
                this.setState({ menuElem: null });
              }}
            />
          </div>



          {user.roles
            && (user.roles.some(role => ["ROLE_ADMIN", "ROLE_INDICATEUR"].includes(role.authority)))
            && <div className='dashboard-section'>
              <div className='dashboard-elem' onClick={() => { this.setMenuElem("KPI") }}
                style={this.state.menuElem === "KPI" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faChartLine} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>KPI</span>
                {this.state.menuElem === "KPI"
                  ? <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />
                  : <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />}
              </div>
              <ul className='dashboard-subelem-list' style={this.state.menuElem === "KPI" ? { maxHeight: 39 * 10 } : { maxHeight: "0" }}>
                <Link to="/rapportLectra" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>KPI machines</span>
                </Link>
                <Link to="/rapportLectraV2" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>KPI machines par hisotrique</span>
                </Link>
                <Link to="/statusMaintPerNiveau" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Maintenance 1er niveau</span>
                </Link>

                <Link to="/statusMaintenance" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Status Maintenance Préventive</span>
                </Link>
                <Link to="/consumableStat" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Consumable Stat</span>
                </Link>
                <Link to="/consumableStatWeek" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Consumable Stat (Semaine)</span>
                </Link>
                <Link to="/ippmReport" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>IPPM</span>
                </Link>
                <Link to="/kpiMaintenance" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>KPI Réactivité Maintenance</span>
                </Link>
                <Link to="/kpiChargeMachine" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>KPI Charge Machines</span>
                </Link>
              </ul>
            </div>}



          {/* {user.roles
            // && (user.roles.map(e => e.authority).includes("ROLE_IMPORTER") && !user.roles.map(e => e.authority).includes("ROLE_ADMIN"))
            && (user.roles.some(role => ["ROLE_IMPORTER"].includes(role.authority)))
            && !user.roles.map(e => e.authority).includes("ROLE_ADMIN")
            && <div className='dashboard-section'>
              <div className='dashboard-elem' onClick={() => { this.setMenuElem("Production Importateur") }}
                style={this.state.menuElem === "Production Importateur" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faIndustry} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Production Importateur</span>
                {this.state.menuElem === "Production Importateur"
                  ? <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />
                  : <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />}
              </div>
              <ul className='dashboard-subelem-list' style={this.state.menuElem === "Production Importateur" ? { maxHeight: 39 * 2 } : { maxHeight: "0" }}>
                
                <Link to="/preparation" className='dashboard-subelem-listelem' >
                  <span style={{ flex: 1 }}>Préparation</span>
                </Link> 
                <Link to="/gammeCMS" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Gamme CMS Importateur</span>
                </Link>
              </ul>
            </div>} */}

          {user.roles
            && (user.roles.some(role => ["ROLE_QUALITE", "ROLE_CHEF_EQUIPE", "ROLE_PROCESS",
              "ROLE_ADMIN", "ROLE_CHEF_DE_ZONE", "ROLE_VARIANCE", "ROLE_CAD","ROLE_IMPORTER","ROLE_GT_READER"].includes(role.authority)))
            // && !user.roles.map(e => e.authority).includes("ROLE_IMPORTER")
            && <div className='dashboard-section'>
              <div className='dashboard-elem' onClick={() => { this.setMenuElem("Production") }}
                style={this.state.menuElem === "Production" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faIndustry} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Production</span>
                {this.state.menuElem === "Production"
                  ? <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />
                  : <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />}
              </div>
              <ul className='dashboard-subelem-list' style={this.state.menuElem === "Production" ? { maxHeight: 39 * 14 } : { maxHeight: "0" }}>

                <Link to="/preparation" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Préparation</span>
                </Link>
                <Link to="/gammeCMS" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Gamme CMS Importateur</span>
                </Link>
                {/* <Link to="/workOrder" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Work Order</span>
                </Link> */}
                {/* <Link to="/planning" className='dashboard-subelem-listelem' >
                  <span style={{ flex: 1 }}>Planning Excel</span>
                </Link> */}
                {/* <Link to="/statutPlanning" className='dashboard-subelem-listelem' >
                  <span style={{ flex: 1 }}>Statut Planning</span>
                </Link> */}
                <Link to="/demande-de-coupe" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Statut Process Coupe</span>
                </Link>
                <Link to="/matelassage" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Matelassage / Coupe</span>
                </Link>
                <Link to="/form" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Fiche de matelassage</span>
                </Link>
                <Link to="/cuttingRequestSerieRouleauData" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Consommation Rouleaux</span>
                </Link>
                <Link to="/rapportRestRouleau" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Rapport Reste Rouleau</span>
                </Link>
                <Link to="/rouleauSummary" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Global Roll Summary</span>
                </Link>
                {!user.roles.map(e => e.authority).includes("ROLE_VARIANCE") && <Link to="/firstCheck" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Maintenance 1er niveau</span>
                </Link>}
                <Link to="/rapportOverlap" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Rapport Overlap</span>
                </Link>
                <Link to="/stockStatusReport" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Stock Status Report - QAD</span>
                </Link>
                <Link to="/cuttingRequestSerieRouleauHistory" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Historique des Rouleaux/Series</span>
                </Link>
                <Link to="/scanRouleau" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Scan Rouleau</span>
                </Link>
                <Link to="/scanRouleauHistorique" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Scan Rouleau Historique</span>
                </Link>
                <Link to="/markersOnlyCode" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Marker Only Code</span>
                </Link>

              </ul>
            </div>}

          {/* Process Workbench */}
          {user.roles
            && (user.roles.some(role => ["ROLE_PROCESS", "ROLE_CHEF_DE_ZONE", "ROLE_CHEF_EQUIPE", "ROLE_ADMIN", "ROLE_VALID_QN_LOGISTIQUE"].includes(role.authority)))
            && <div className='dashboard-section'>
              <Link to="/processWorkbench" className='dashboard-elem' onClick={() => this.props.toggleMenu()}
                style={{ textDecoration: 'none', color: this.state.path === '/processWorkbench' ? '#ca0c0c' : 'inherit' }}
              >
                <FontAwesomeIcon icon={faGears} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Process Workbench</span>
              </Link>
            </div>}

          {/* Rectification Séquences (chef) */}
          {user.roles
            && (user.roles.some(role => ["ROLE_CHEF_DE_ZONE", "ROLE_CHEF_EQUIPE", "ROLE_ADMIN"].includes(role.authority)))
            && <div className='dashboard-section'>
              <Link to="/sequenceRectification" className='dashboard-elem' onClick={() => this.props.toggleMenu()}
                style={{ textDecoration: 'none', color: this.state.path === '/sequenceRectification' ? '#ca0c0c' : 'inherit' }}
              >
                <FontAwesomeIcon icon={faGears} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Rectification Séquences</span>
              </Link>
            </div>}

          {/* Box Weight Control Section */}
          {user.roles
            && (user.roles.some(role => ["ROLE_ADMIN", "ROLE_FILLING_WEIGHT", "ROLE_VERIFYING_WEIGHT"].includes(role.authority)))
            && <div className='dashboard-section'>
              <div className='dashboard-elem' onClick={() => { this.setMenuElem("BoxWeight") }}
                style={this.state.menuElem === "BoxWeight" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faBoxesStacked} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Contrôle Poids Boîte</span>
                {this.state.menuElem === "BoxWeight"
                  ? <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />
                  : <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />}
              </div>
              <ul className='dashboard-subelem-list' style={this.state.menuElem === "BoxWeight" ? { maxHeight: 39 * 3 } : { maxHeight: "0" }}>
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_FILLING_WEIGHT"].includes(role.authority)) && (
                  <Link to="/boxWeightFilling" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Enregistrement Poids</span>
                  </Link>
                )}
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_VERIFYING_WEIGHT"].includes(role.authority)) && (
                  <Link to="/boxWeightVerifying" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Vérification Poids</span>
                  </Link>
                )}
                {user.roles.some(role => ["ROLE_ADMIN"].includes(role.authority)) && (
                  <Link to="/boxWeight" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Historique Poids</span>
                  </Link>
                )}
              </ul>
            </div>}
            

          {/* CNC PS Section */}
          {user.roles
            && (user.roles.some(role => ["ROLE_ADMIN", "ROLE_CNC_PS", "ROLE_CNC_CONTROL", "ROLE_QUALITE", "ROLE_PROCESS", "ROLE_ENGINEERING"].includes(role.authority)))
            && <div className='dashboard-section'>
              <div className='dashboard-elem' onClick={() => { this.setMenuElem("CNC PS") }}
                style={this.state.menuElem === "CNC PS" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faKeyboard} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>CNC PS</span>
                {this.state.menuElem === "CNC PS"
                  ? <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />
                  : <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />}
              </div>
              <ul className='dashboard-subelem-list' style={this.state.menuElem === "CNC PS" ? { maxHeight: 39 * 14 } : { maxHeight: "0" }}>
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_CNC_PS"].includes(role.authority)) && (
                  <Link to="/cncPs" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Consommation Cuir</span>
                  </Link>
                )}
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_CNC_CONTROL", "ROLE_QUALITE"].includes(role.authority)) && (
                  <Link to="/cncControl" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Contrôle CNC</span>
                  </Link>
                )}
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_CNC_CONTROL", "ROLE_QUALITE"].includes(role.authority)) && (
                  <Link to="/cncQualite" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Gestion Qualité</span>
                  </Link>
                )}
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_CNC_CONTROL", "ROLE_QUALITE"].includes(role.authority)) && (
                  <Link to="/cncQualiteMachine" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Rapport Qualité Machine</span>
                  </Link>
                )}
                <Link to="/cncShiftStatus" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Statut Shift</span>
                </Link>
                <Link to="/cncKpi" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>KPI CNC</span>
                </Link>
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_PROCESS", "ROLE_QUALITE", "ROLE_ENGINEERING"].includes(role.authority)) && (
                  <Link to="/cncPsSession" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Historique Sessions</span>
                  </Link>
                )}
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_PROCESS", "ROLE_QUALITE", "ROLE_ENGINEERING"].includes(role.authority)) && (
                  <Link to="/cncPsLeatherConsumption" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Historique Consommations</span>
                  </Link>
                )}
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_PROCESS", "ROLE_QUALITE", "ROLE_ENGINEERING"].includes(role.authority)) && (
                  <Link to="/programCNC" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Programmes CNC</span>
                  </Link>
                )}
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_PROCESS", "ROLE_QUALITE", "ROLE_ENGINEERING"].includes(role.authority)) && (
                  <Link to="/machineCnc" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Machines CNC</span>
                  </Link>
                )}
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_PROCESS", "ROLE_QUALITE", "ROLE_ENGINEERING"].includes(role.authority)) && (
                  <Link to="/programmeDistribution" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Programme Distribution</span>
                  </Link>
                )}
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_PROCESS", "ROLE_ENGINEERING"].includes(role.authority)) && (
                  <Link to="/cncSync" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Import / Export CNC</span>
                  </Link>
                )}
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_PROCESS", "ROLE_ENGINEERING"].includes(role.authority)) && (
                  <Link to="/cncMachineReport" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Rapports Machine CNC</span>
                  </Link>
                )}
                {user.roles.some(role => ["ROLE_ADMIN", "ROLE_PROCESS", "ROLE_ENGINEERING"].includes(role.authority)) && (
                  <Link to="/cncMachineReportPiece" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                    <span style={{ flex: 1 }}>Pièces Rapport CNC</span>
                  </Link>
                )}
              </ul>
            </div>}


          {user.roles
            && user.roles.map(e => e.authority).some(r => ["ROLE_ADMIN", "ROLE_VARIANCE", "ROLE_PROCESS", "ROLE_CHEF_DE_ZONE", "ROLE_CHEF_EQUIPE", "ROLE_VALID_QN_LOGISTIQUE"].includes(r))
            && <div className='dashboard-section'>
              <div className='dashboard-elem' onClick={() => { this.setMenuElem("Variance") }}
                style={this.state.menuElem === "Variance" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faTable} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Logistique</span>
                {this.state.menuElem === "Variance"
                  ? <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />
                  : <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />}
              </div>
              <ul className='dashboard-subelem-list' style={this.state.menuElem === "Variance" ? { maxHeight: 39 * 11 } : { maxHeight: "0" }}>
                <Link to="/cuttingRequestSerieRouleauData" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Consommation Rouleaux</span>
                </Link>
                <Link to="/plsProdTicket" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Rapport PLS</span>
                </Link>
                <Link to="/rapportUsage" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Rapport usage / BOM</span>
                </Link>
                <Link to="/rapportUsageReport" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Rapport usage / BOM (historique)</span>
                </Link>
                <Link to="/rapportRestRouleau" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Rapport Reste Rouleau</span>
                </Link>
                <Link to="/rouleauSummary" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Global Roll Summary</span>
                </Link>
                <Link to="/stockStatusReport" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Stock Status Report - QAD</span>
                </Link>
                <Link to="/rapportShortage" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Rapport Shortage</span>
                </Link>
                <Link to="/stockReportVerification" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Stock Report Verification</span>
                </Link>
                <Link to="/rollQlaize" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Roll Qlaize</span>
                </Link>
                <Link to="/logisticsRelease" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Préparation / Release</span>
                </Link>
                <Link to="/tableFeed" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Alimentation Tables</span>
                </Link>
                <Link to="/productionFloor" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Vue Production (Floor)</span>
                </Link>
              </ul>
            </div>}

          {user.roles && (user.roles.map(e => e.authority).includes("ROLE_ENGINEERING") 
          || user.roles.map(e => e.authority).includes("ROLE_PROCESS") 
          || user.roles.map(e => e.authority).includes("ROLE_CAD")) && <div className='dashboard-section'>
            <div className='dashboard-elem' onClick={() => { this.setMenuElem("Ingénierie") }}
              style={this.state.menuElem === "Ingénierie" ? { color: "#ca0c0c" } : {}}
            >
              <FontAwesomeIcon icon={faHelmetSafety} style={{ margin: "0 10px" }} />
              <span style={{ flex: 1 }}>Ingénierie</span>
              {this.state.menuElem === "Ingénierie"
                ? <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />
                : <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />}
            </div>
            <ul className='dashboard-subelem-list' style={this.state.menuElem === "Ingénierie" ? { maxHeight: 39 * 6 } : { maxHeight: "0" }}>
              <Link to="/gammesTechnique" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Gammes technique</span>
              </Link>
              <Link to="/gammeTechnique" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Gammes technique Hisotrique</span>
              </Link>
              <Link to="/ctcFiles" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>CTC Files</span>
              </Link>
              <Link to="/filesHistory" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Files History</span>
              </Link>

              <Link to="/toleranceForm" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Tolerance Form</span>
              </Link>
              <Link to="/ctcToleranceRuleForm" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Règles de Tolérance CTC</span>
              </Link>
            </ul>
          </div>}

          {user.roles && (user.roles.map(e => e.authority).includes("ROLE_CAD") 
        || user.roles.map(e => e.authority).includes("ROLE_CAD_READER")) && <div className='dashboard-section'>
            <div className='dashboard-elem' onClick={() => { this.setMenuElem("CAD") }}
              style={this.state.menuElem === "CAD" ? { color: "#ca0c0c" } : {}}
            >
              <FontAwesomeIcon icon={faDrawPolygon} style={{ margin: "0 10px" }} />
              <span style={{ flex: 1 }}>CAD</span>
              {this.state.menuElem === "CAD"
                ? <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />
                : <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />}
            </div>
            <ul className='dashboard-subelem-list' style={this.state.menuElem === "CAD" ? { maxHeight: 39 * 22 } : { maxHeight: "0" }}>
              <Link to="/cuttingPlan" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Plan de coupe</span>
              </Link>
              <Link to="/preparation" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Préparation</span>
              </Link>
              <Link to="/ChangementSerie" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Verification/Changement Serie</span>
              </Link>
              <Link to="/cuttingRequestSerieRouleauHistory" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Historique des Rouleaux/Series</span>
                </Link>
              <Link to="/drillEmp" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Drill Emp</span>
              </Link>
              <Link to="/cuttingPlanCombination" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Combinaison des plans de coupe</span>
              </Link>
              <Link to="/changementReftissu" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Changement Reftissu</span>
              </Link>
              <Link to="/changementOptional" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Changement Optionnel (DIE/Lectra)</span>
              </Link>
              <Link to="/partNumberMaterialConfig" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Part Number Material</span>
              </Link>
              <Link to="/partNumberMaterialConfigData" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Part Number Material Data</span>
              </Link>
              <Link to="/plieConfig" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Pli Config</span>
              </Link>
              <Link to="/partNumberBoom" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Parte Number</span>
              </Link>
              <Link to="/partNumberCorrespendance" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Part Number Correspendance</span>
              </Link>
              <Link to="/projetVersion" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Codes des versions</span>
              </Link>
              <Link to="/projetReftissu" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Codes des reftissus</span>
              </Link>
              <Link to="/placement" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Placement</span>
              </Link>
              <Link to="/placementDetail" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Placement Detail</span>
              </Link>
              <Link to="/patternSearch" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Recherche des empiècements</span>
              </Link>
              <Link to="/cuttingSpeed" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Vitesse de coupe</span>
              </Link>
              <Link to="/rapportOverlap" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Rapport Overlap</span>
              </Link>
              <Link to="/machineTypeSwap" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Changement Type Machine</span>
              </Link>
              <Link to="/pieceDetailImport" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Import Pièces CSV</span>
              </Link>
              <Link to="/weightCalculation" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Calcul Poids</span>
              </Link>
              <Link to="/pieceDetail" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Détails Pièces</span>
              </Link>
              <Link to="/cuttingTimePerPartNumber" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Temps de Coupe / PN</span>
              </Link>

            </ul>
          </div>}

          {user.roles && user.roles.map(e => e.authority).includes("ROLE_CAD_FOAM") 
            && !user.roles.map(e => e.authority).includes("ROLE_CAD") && <div className='dashboard-section'>
            <div className='dashboard-elem' onClick={() => { this.setMenuElem("CAD FOAM") }}
              style={this.state.menuElem === "CAD FOAM" ? { color: "#ca0c0c" } : {}}
            >
              <FontAwesomeIcon icon={faDrawPolygon} style={{ margin: "0 10px" }} />
              <span style={{ flex: 1 }}>CAD FOAM</span>
              {this.state.menuElem === "CAD FOAM"
                ? <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />
                : <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />}
            </div>
            <ul className='dashboard-subelem-list' style={this.state.menuElem === "CAD FOAM" ? { maxHeight: 39 * 4 } : { maxHeight: "0" }}>
              <Link to="/cuttingPlan" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Plan de coupe (Foam)</span>
              </Link>
              <Link to="/drillEmp" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Drill Emp</span>
              </Link>
              <Link to="/partNumberMaterialConfig" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Part Number Material</span>
              </Link>
              <Link to="/partNumberMaterialConfigData" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Part Number Material Data</span>
              </Link>
            </ul>
          </div>}

          {user.roles && (user.roles.map(e => e.authority).includes("ROLE_QUALITE") || user.roles.map(e => e.authority).includes("ROLE_QUALITE_READER")) && <div className='dashboard-section'>
            <div className='dashboard-elem' onClick={() => { this.setMenuElem("Qualité") }}
              style={this.state.menuElem === "Qualité" ? { color: "#ca0c0c" } : {}}
            >
              <FontAwesomeIcon icon={faMagnifyingGlass} style={{ margin: "0 10px" }} />
              <span style={{ flex: 1 }}>Qualité</span>
              {this.state.menuElem === "Qualité"
                ? <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />
                : <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />}
            </div>
            <ul className='dashboard-subelem-list' style={this.state.menuElem === "Qualité" ? { maxHeight: 39 * 13 } : { maxHeight: "0" }}>
              <Link to="/qn" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Flash qualité</span>
              </Link>
              <Link to="/verificationQualite" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Vérification Qualité</span>
              </Link>
              <Link to="/auditQualite" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Audit Qualité</span>
              </Link>
              <Link to="/validationDefautRouleau" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Validation défaut rouleau</span>
              </Link>
              <Link to="/reftissuProperty" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Propriété Réf Tissu</span>
              </Link>
              <Link to="/suiviQulite" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Suivi Qualité</span>
              </Link>
              <Link to="/qualityNotice" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Lear Quality Notice</span>
              </Link>
              <Link to="/qualityNoticeValidation" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Validation des QN</span>
              </Link>
              <Link to="/qualityCode" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Code Qualité</span>
              </Link>
              <Link to="/qualityValidationHistory" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Historique des validations</span>
              </Link>
              <Link to="/qualityValidationPattern" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Validation Empiècement</span>
              </Link>
              <Link to="/qualityPatternValidationHistory" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Historique Validation Empiècement</span>
              </Link>
              <Link to="/perforationAnalyzer" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Analyse Perforation</span>
              </Link>
            </ul>
          </div>}


          {user.roles
            && (user.roles.map(e => e.authority).includes("ROLE_MAINTENANCE"))
            && <div className='dashboard-section'>
              <div className='dashboard-elem' onClick={() => { this.setMenuElem("Maintenance") }}
                style={this.state.menuElem === "Maintenance" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faScrewdriverWrench} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Maintenance</span>
                {this.state.menuElem === "Maintenance"
                  ? <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />
                  : <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />}
              </div>
              <ul className='dashboard-subelem-list' style={this.state.menuElem === "Maintenance" ? { maxHeight: 39 * 4 } : { maxHeight: "0" }}>
                <Link to="/maintenanceIntervention" className='dashboard-subelem-listelem' >
                  <span style={{ flex: 1 }}>Maintenance Machine</span>
                </Link>
                <Link to="/maintenanceInterventionConfig" className='dashboard-subelem-listelem' >
                  <span style={{ flex: 1 }}>Maintenance Machine Config</span>
                </Link>
                <Link to="/productionTable" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Machine de Coupe</span>
                </Link>
                <Link to="/firstCheckConfig" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Maitenance 1er niveau Config</span>
                </Link>
              </ul>
            </div>}

          {user.roles
            && (user.roles.map(e => e.authority).includes("ROLE_ADMIN")
              || user.roles.map(e => e.authority).includes("ROLE_QUALITE")
              || user.roles.map(e => e.authority).includes("ROLE_DB_PROD_READER")
              || user.roles.map(e => e.authority).includes("ROLE_PROCESS"))
            && <div className='dashboard-section'>
              <div className='dashboard-elem' onClick={() => { this.setMenuElem("Database Production") }}
                style={this.state.menuElem === "Database Production" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faDatabase} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Database Production</span>
                {this.state.menuElem === "Database Production"
                  ? <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />
                  : <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />}
              </div>
              <ul className='dashboard-subelem-list' style={this.state.menuElem === "Database Production" ? { maxHeight: 39 * 15 } : { maxHeight: "0" }}>
                <Link to="/cuttingRequestData" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>DB Demande de coupe</span>
                </Link>
                <Link to="/cuttingRequestSerieData" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>DB Serie</span>
                </Link>
                <Link to="/cuttingRequestSerieRouleauData" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>DB Consommation Rouleaux</span>
                </Link>
                <Link to="/cuttingRequestSerieRouleauHistory" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Historique des Rouleaux/Series</span>
                </Link>
                <Link to="/cuttingRequestBoxData" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>DB Box</span>
                </Link>
                <Link to="/scanXPL" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Scan XPL</span>
                </Link>
                <Link to="/intervention" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Intervention</span>
                </Link>
                <Link to="/consumable" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Consumable</span>
                </Link>

                <Link to="/pointage" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Pointage Coupe</span>
                </Link>
                <Link to="/coupeMachineHistory" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Coupe Machine Hisotrique</span>
                </Link>
                <Link to="/coupePerformance" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Performance Coupe</span>
                </Link>
                <Link to="/machineDxfRapport" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>DXF Rapport</span>
                </Link>
                <Link to="/machineLsrRapport" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>LSR Rapport</span>
                </Link>
                <Link to="/machineLog" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Machine Log</span>
                </Link>
                <Link to="/partNumberInfo" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Part Number Info</span>
                </Link>


              </ul>
            </div>}

          {user.roles
            && (user.roles.map(e => e.authority).includes("ROLE_PLS_READER") || user.roles.map(e => e.authority).includes("ROLE_PLS_ADMIN"))
            && <div className='dashboard-section'>
              <div className='dashboard-elem' onClick={() => { this.setMenuElem("Database PLS") }}
                style={this.state.menuElem === "Database PLS" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faDatabase} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Database PLS</span>
                {this.state.menuElem === "Database PLS"
                  ? <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />
                  : <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />}
              </div>
              <ul className='dashboard-subelem-list' style={this.state.menuElem === "Database PLS" ? { maxHeight: 39 * 18 } : { maxHeight: "0" }}>
                <Link to="/plsDemande" className  ='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Demande PLS</span>
                </Link>
                <Link to="/plsSubDemande" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Sub Demande PLS</span>
                </Link>
                <Link to="/plsProdTicket" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Rapport PLS</span>
                </Link>
                <Link to="/plsDemandeHistory" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Demande History</span>
                </Link>
                <Link to="/plsSite" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Site PLS</span>
                </Link>
                <Link to="/plsLieuDetection" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Lieu Detection PLS</span>
                </Link>
                <Link to="/plsChaine" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Chaine PLS</span>
                </Link>
                <Link to="/plsDefaut" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Defaut PLS</span>
                </Link>
                <Link to="/plsScrap" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Scrap PLS</span>
                </Link>
                <Link to="/plsSubScrap" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Sub Scrap PLS</span>
                </Link>
                <Link to="/plsScrapRapport" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Scrap Rapport PLS</span>
                </Link>
                <Link to="/plsCauseScrap" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Cause Scrap PLS</span>
                </Link>
                <Link to="/plsMachine" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Machine PLS</span>
                </Link>
                <Link to="/plsRapportRestRouleau" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Rapport Rest Rouleau PLS</span>
                </Link>
                {/* <Link to="/plsReftissu" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Ref Tissu PLS</span>
                </Link>
                <Link to="/plsReftissuAlert" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Ref Tissu Alert PLS</span>
                </Link> */}
                <Link to="/plsAirbagDetail" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Airbag Detail PLS</span>
                </Link>

              </ul>
            </div>}

          {user.roles
            && (user.roles.map(e => e.authority).includes("ROLE_SPLICE_READER") || user.roles.map(e => e.authority).includes("ROLE_PROCESS"))
            && <div className='dashboard-section'>
              <div className='dashboard-elem' onClick={() => { this.setMenuElem("Database Splice") }}
                style={this.state.menuElem === "Database Splice" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faDatabase} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Database Splice</span>
                {this.state.menuElem === "Database Splice"
                  ? <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />
                  : <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />}
              </div>
              <ul className='dashboard-subelem-list' style={this.state.menuElem === "Database Splice" ? { maxHeight: 39 * 4 } : { maxHeight: "0" }}>
                <Link to="/spliceMarker" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Marker</span>
                </Link>
                <Link to="/spliceMarkerLog" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Marker Log</span>
                </Link>
                <Link to="/markersOnlyCode" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Marker Only Code</span>
                </Link>
                <Link to="/calibrationLog" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Calibration Log</span>
                </Link>
              </ul>
            </div>}

          {user.roles && !user.roles.map(e => e.authority).includes("ROLE_ENGINEERING") && (user.roles.map(e => e.authority).includes("ROLE_CUTTING_CUIR") || user.roles.map(e => e.authority).includes("ROLE_QUALITE")) && <div className='dashboard-section'>
            <div className='dashboard-elem' onClick={() => { this.setMenuElem("Cutting Cuir") }}
              style={this.state.menuElem === "Cutting Cuir" ? { color: "#ca0c0c" } : {}}
            >
              <FontAwesomeIcon icon={faHelmetSafety} style={{ margin: "0 10px" }} />
              <span style={{ flex: 1 }}>Cutting Cuir</span>
              {this.state.menuElem === "Cutting Cuir"
                ? <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />
                : <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />}
            </div>
            <ul className='dashboard-subelem-list' style={this.state.menuElem === "Cutting Cuir" ? { maxHeight: 39 * 3 } : { maxHeight: "0" }}>
              <Link to="/gammesTechnique" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Gammes technique</span>
              </Link>
            </ul>
          </div>}

          {user.roles && (user.roles.map(e => e.authority).includes("ROLE_GT_READER")) 
           && <div className='dashboard-section'>
              <Link to="/gammesTechnique" className='dashboard-elem' onClick={() => this.props.toggleMenu()}
              //style={this.state.menuElem === "Production" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faEye} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Gammes technique</span>
              </Link>

            </div>}


          {user.roles
            && (user.roles.map(e => e.authority).includes("ROLE_CAD") || user.roles.map(e => e.authority).includes("ROLE_CAD_READER"))
            && <div className='dashboard-section'>
              <div className='dashboard-elem' onClick={() => { this.setMenuElem("Database CAD") }}
                style={this.state.menuElem === "Database CAD" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faDatabase} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Database Plan De Coupe</span>
                {this.state.menuElem === "Database CAD"
                  ? <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />
                  : <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />}
              </div>
              <ul className='dashboard-subelem-list' style={this.state.menuElem === "Database CAD" ? { maxHeight: 39 * 4 } : { maxHeight: "0" }}>
                <Link to="/cuttingPlanData" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>DB Plan de coupe</span>
                </Link>
                <Link to="/cuttingPlanPartNumberData" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>DB Plan de coupe PartNumber</span>
                </Link>
                <Link to="/cuttingPlanMaterialData" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>DB Plan de coupe Matériel</span>
                </Link>
                <Link to="/cuttingPlanMaterialPlacementData" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>DB Plan de coupe Placement</span>
                </Link>
              </ul>
            </div>}
          {/* {user.roles && (!user.roles.map(e => e.authority).includes("ROLE_CAD") && user.roles.map(e => e.authority).includes("ROLE_QUALITE")) && <div className='dashboard-section'>
            <div className='dashboard-elem' onClick={() => { this.setMenuElem("CAD") }}
              style={this.state.menuElem === "CAD" ? { color: "#ca0c0c" } : {}}
            >
              <FontAwesomeIcon icon={faDrawPolygon} style={{ margin: "0 10px" }} />
              <span style={{ flex: 1 }}>CAD</span>
              {this.state.menuElem === "CAD"
                ? <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />
                : <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />}
            </div>
            <ul className='dashboard-subelem-list' style={this.state.menuElem === "CAD" ? { maxHeight: 39 * 1 } : { maxHeight: "0" }}>
              <Link to="/cuttingPlan" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Plan de coupe</span>
              </Link>
            </ul>
          </div>} */}


          {user.roles
            && (user.roles.some(role => ["ROLE_QN"].includes(role.authority)))
            && <div className='dashboard-section'>
              <div className='dashboard-elem' onClick={() => { this.setMenuElem("QN") }}
                style={this.state.menuElem === "QN" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faKeyboard} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Quality Notice</span>
                {this.state.menuElem === "QN"
                  ? <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />
                  : <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />}
              </div>
              <ul className='dashboard-subelem-list' style={this.state.menuElem === "QN" ? { maxHeight: 39 * 3 } : { maxHeight: "0" }}>
                <Link to="/qualityNoticeForm" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Lear Quality Notice Form</span>
                </Link>
                <Link to="/qualityNotice" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Lear Quality Notice</span>
                </Link>
                <Link to="/qualityNoticeValidation" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Validation des QN</span>
                </Link>
              </ul>
            </div>}

            

          {user.roles.map(e => e.authority).length > 0 && (user.roles.map(e => e.authority).length > 1 || user.roles.map(e => e.authority)[0] !== "ROLE_QN") && <div className='dashboard-section'>
            <Link to="/validationIntervention" className='dashboard-elem' onClick={() => this.props.toggleMenu()}
            //style={this.state.menuElem === "Production" ? { color: "#ca0c0c" } : {}}
            >
              <FontAwesomeIcon icon={faClipboard} style={{ margin: "0 10px" }} />
              <span style={{ flex: 1 }}>Validation d'intervention coupe</span>
            </Link>
          </div>}


          {user.roles
            // && (user.roles.some(role => ["ROLE_ADMIN", "ROLE_PROCESS", "ROLE_MAINTENANCE"].includes(role.authority)))
            && <div className='dashboard-section'>
              <Link to="/demandeChangementSerie" className='dashboard-elem' onClick={() => this.props.toggleMenu()}
              //style={this.state.menuElem === "Production" ? { color: "#ca0c0c" } : {}}
              >
                <FontAwesomeIcon icon={faPenToSquare} style={{ margin: "0 10px" }} />
                <span style={{ flex: 1 }}>Demande Changement Serie</span>
              </Link>
            </div>}

          {user.roles && (user.roles.some(role => ["ROLE_PROCESS", "ROLE_ADMIN"].includes(role.authority))) && <div className='dashboard-section'>
            <div className='dashboard-elem' onClick={() => { this.setMenuElem("Config") }}
              style={this.state.menuElem === "Config" ? { color: "#ca0c0c" } : {}}
            >
              <FontAwesomeIcon icon={faGear} style={{ margin: "0 10px" }} />
              <span style={{ flex: 1 }}>Config</span>
              {this.state.menuElem === "Config"
                ? <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />
                : <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />}
            </div>
            {user.roles.some(role => ["ROLE_ADMIN"].includes(role.authority))
              ? <ul className='dashboard-subelem-list' style={this.state.menuElem === "Config" ? { maxHeight: 39 * 15 } : { maxHeight: "0" }}>
                <Link to="/projet" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Projets</span>
                </Link>
                <Link to="/zone" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Zones</span>
                </Link>
                <Link to="/productionTable" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Machine de Coupe</span>
                </Link>
                <Link to="/firstCheckConfig" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Maitenance 1er niveau Config</span>
                </Link>
                <Link to="/auditQualiteConfig" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Audit Qualité Config</span>
                </Link>
                <Link to="/maintenanceInterventionConfig" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Maitenance Machine Config</span>
                </Link>
                <Link to="/codeErreur" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Code d'erreur</span>
                </Link>
                <Link to="/codeArret" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Code d'arrêt</span>
                </Link>
                <Link to="/codeDefaut" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Code defaut</span>
                </Link>
                <Link to="/codeScrap" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Code scrap</span>
                </Link>
                <Link to="/configSeriePlus" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Config Serie Plus</span>
                </Link>
                <Link to="/user" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Utilisateurs</span>
                </Link>
                {/*hardwareConfig */}
                <Link to="/hardwareConfig" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Configuration Hardware</span>
                </Link>
                <Link to="/systemHealth" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Santé Système</span>
                </Link>
                <Link to="/archiving" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Archivage</span>
                </Link>
              </ul>
              : <ul className='dashboard-subelem-list' style={this.state.menuElem === "Config" ? { maxHeight: 39 * 5 } : { maxHeight: "0" }}>
                <Link to="/projet" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Projets</span>
                </Link>
                <Link to="/zone" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Zones</span>
                </Link>
                <Link to="/productionTable" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Machine de Coupe</span>
                </Link>
                <Link to="/firstCheckConfig" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Maitenance 1er niveau Config</span>
                </Link>
                <Link to="/codeErreur" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Code d'erreur</span>
                </Link>
                <Link to="/hardwareConfig" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                  <span style={{ flex: 1 }}>Configuration Hardware</span>
                </Link>

              </ul>}
          </div>}

          {user.roles && user.roles.map(e => e.authority).includes("ROLE_PROCESS") && <div className='dashboard-section'>
            <div className='dashboard-elem' onClick={() => { this.setMenuElem("Process") }}
              style={this.state.menuElem === "Process" ? { color: "#ca0c0c" } : {}}
            >
              <FontAwesomeIcon icon={faCog} style={{ margin: "0 10px" }} />
              <span style={{ flex: 1 }}>Process</span>
              {this.state.menuElem === "Process"
                ? <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />
                : <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />}
            </div>
            <ul className='dashboard-subelem-list' style={this.state.menuElem === "Process" ? { maxHeight: 39 * 7 } : { maxHeight: "0" }}>
              <Link to="/planDeCharge" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Plan de Charge</span>
              </Link>
              <Link to="/processDispatcher" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Dispatching de Séquence</span>
              </Link>
              <Link to="/processWorkbench" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Process Workbench</span>
              </Link>
              <Link to="/engineControl" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Contrôle Moteur</span>
              </Link>
              <Link to="/partNumberMaterialConfig" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Part Number Material Config</span>
              </Link>
              <Link to="/partNumberMaterialConfigData" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Part Number Material Config Data</span>
              </Link>
              <Link to="/capaciteInstallee" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Capacité Installée</span>
              </Link>
            </ul>
          </div>}

          {user.roles && user.roles.some(e => ["ROLE_ADMIN", "ROLE_PROCESS", "ROLE_CHEF_DE_ZONE", "ROLE_CHEF_EQUIPE"].includes(e.authority)) && <div className='dashboard-section'>
            <div className='dashboard-elem' onClick={() => { this.setMenuElem("ChefDeZone") }}
              style={this.state.menuElem === "ChefDeZone" ? { color: "#ca0c0c" } : {}}
            >
              <FontAwesomeIcon icon={faHelmetSafety} style={{ margin: "0 10px" }} />
              <span style={{ flex: 1 }}>Chef de Zone</span>
              {this.state.menuElem === "ChefDeZone"
                ? <FontAwesomeIcon icon={faAngleUp} size="lg" style={{ margin: "0 10px" }} />
                : <FontAwesomeIcon icon={faAngleDown} size="lg" style={{ margin: "0 10px" }} />}
            </div>
            <ul className='dashboard-subelem-list' style={this.state.menuElem === "ChefDeZone" ? { maxHeight: 39 * 2 } : { maxHeight: "0" }}>
              <Link to="/processWorkbench" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Process Workbench</span>
              </Link>
              <Link to="/chefDeZoneConfirm" className='dashboard-subelem-listelem' onClick={() => this.props.toggleMenu()}>
                <span style={{ flex: 1 }}>Confirmation Machines</span>
              </Link>
            </ul>
          </div>}
        </div>

        <div className='dashboard-bottom'>
          <div className='dashboard-bottom-elem' onClick={() => { 
            this.props.history.push("/profile");
            this.props.toggleMenu();
          }}>
            <FontAwesomeIcon icon={faUserAlt} />
            <span className='ml-2'>{user.firstName} {user.lastName}</span>
          </div>
          <div className='dashboard-bottom-elem' onClick={() => this.logout()}>
            <FontAwesomeIcon icon={faRightFromBracket} />
            <span className='ml-2'>Se déconnecter</span>
          </div>
        </div>
      </div>
    )
  }
}

Dashboard.propTypes = {
  logout: PropTypes.func.isRequired,
  security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
  security: state.security
})

export default connect(mapStateToProps, { logout })(Dashboard);
