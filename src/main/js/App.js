import React, { Component } from 'react'
import { Route, BrowserRouter, Switch, Redirect } from 'react-router-dom'
import axios from 'axios'
import Landing from './components/Layout/Landing'
import SecuredRoute from './securityUtils/SecuredRoute'
import 'bootstrap/dist/css/bootstrap.min.css'
import "react-bootstrap";
import { Provider } from 'react-redux'

import ChatWidget from './components/LlmChat/ChatWidget'

import store from './store'
import Home from './components/Layout/Home'
import { logout } from './actions/securityAction'
import { SET_CURRENT_USER } from './actions/types'
import setJWTToken from './securityUtils/setJWTToken'
import jwt_decode from 'jwt-decode';
import history from './history'
import Profile from './components/Layout/Profile'
import Users from './components/Layout/Users'
import EntityList from './components/EntityList'
import NoMatchRoute from './components/Layout/NoMatchRoute'
import PartNumberMaterialConfig from './components/Layout/PartNumberMaterialConfig'
import CuttingPlan from './components/Layout/CuttingPlan'
import GammesTechnique from './components/Layout/GammesTechnique'
import GraphEmp from './components/Layout/GraphEmp'
import GammePn from './components/Layout/GammePn'
import Importation from './components/Layout/Importation'
import DemandeDeCoupe from './components/Layout/DemandeDeCoupe'
import Matelassage from './components/Layout/Matelassage'
import Boxes from './components/Layout/Boxes'
import StatutPlanning from './components/Layout/StatutPlanning'
import RapportLectra from './components/Layout/RapportLectra'
import ChangementReftissu from './components/Layout/ChangementReftissu' 
import ChangementOptional from './components/Layout/ChangementOptional'
import ImportationNew from './components/Layout/ImportationNew'
import CuttingPlanCombinaison from './components/Layout/CuttingPlanCombinaison'
import GammeCMS from './components/Layout/GammeCMS'
import VerificationQualite from './components/Layout/VerificationQualite'
import GammePnPage from './components/Layout/GammePnPage'
import PatternSearch from './components/Layout/PatternSearch'
import ValidationIntervention from './components/Layout/ValidationIntervention'
import StatusMaintenance from './components/Layout/StatusMaintenance'
import RapportOverlap from './components/Layout/RapportOverlap'
import StatusMaintPerNiveau from './components/Layout/StatusMaintPerNiveau'
import ValidationDefautRouleau from './components/Layout/ValidationDefautRouleau'
import RapportRestRouleau from './components/Layout/RapportRestRouleau'
import RouleauSummary from './components/Layout/RouleauSummary'
import SuiviQulite from './components/Layout/SuiviQulite'
import ConsumableStat from './components/Layout/ConsumableStat'
import RapportLectraV2 from './components/Layout/RapportLectraV2'
import Form from './components/Layout/Form'
import QualityNoticeForm from './components/Layout/QualityNoticeForm'
import qualityNoticeValidation from './components/Layout/qualityNoticeValidation'
import QualityCode from './components/Layout/QualityCode'
import RapportUsage from './components/Layout/RapportUsage'
import Placement from './components/Layout/Placement'
import RapportShortage from './components/Layout/RapportShortage'
import ChangementSerie from './components/Layout/ChangementSerie'
import DemandeChangementSerieValidation from './components/Layout/DemandeChangementSerieValidation'
import ToleranceForm from './components/Layout/ToleranceForm'
//StockReportVerification
import StockReportVerification from './components/Layout/StockReportVerification'
import RollQlaize from './components/Layout/RollQlaize'
import CuttingPlanFoam from './components/Layout/CuttingPlanFoam'
import MachineLog from './components/Layout/MachineLog'
import ConsumableStatWeek from './components/Layout/ConsumableStatWeek'
import IppmReport from './components/Layout/IppmReport'
import KpiMaintenance from './components/Layout/KpiMaintenance'
import CtcToleranceRuleForm from './components/Layout/CtcToleranceRuleForm'
import BoxWeightFilling from './components/Layout/BoxWeightFilling'
import BoxWeightVerifying from './components/Layout/BoxWeightVerifying'
import PieceDetailImport from './components/Layout/PieceDetailImport'
import WeightCalculation from './components/Layout/WeightCalculation'
import CuttingTimePerPartNumber from './components/Layout/CuttingTimePerPartNumber'
import CncPs from './components/Layout/CncPs'
import CncControl from './components/Layout/CncControl'
import CncQualite from './components/Layout/CncQualite'
import CncMachineQualiteReport from './components/Layout/CncMachineQualiteReport'
import CncShiftStatus from './components/Layout/CncShiftStatus'
import CncKpi from './components/Layout/CncKpi'
import CncSync from './components/Layout/CncSync'
import ProcessDispatcher from './components/Layout/ProcessDispatcher'
import EngineControl from './components/Layout/EngineControl'
import ChefDeZoneConfirm from './components/Layout/ChefDeZoneConfirm'
import MachineTypeSwap from './components/Layout/MachineTypeSwap'
import PerforationDistanceAnalyzer from './components/Layout/PerforationDistanceAnalyzer'
import RapportMatieresAirbag from './components/Layout/RapportMatieresAirbag'
import PlanDeCharge from './components/Layout/PlanDeCharge'
import KpiChargeMachine from './components/Layout/KpiChargeMachine'
import Workbench from './components/Layout/Workbench'
import LogisticsRelease from './components/Layout/LogisticsRelease'
import TableFeed from './components/Layout/TableFeed'
import ProductionFloor from './components/Layout/ProductionFloor'
import SequenceRectification from './components/Layout/SequenceRectification'
import SystemHealth from './components/Layout/SystemHealth'
import Archiving from './components/Layout/Archiving'
// import ImportSM from './components/GPF/ImportSM'


const jwtToken = localStorage.jwtToken;

if(jwtToken){
  setJWTToken(jwtToken);
  const decode_jwtToekn = jwt_decode(jwtToken);
  store.dispatch({
      type: SET_CURRENT_USER,
      payload: decode_jwtToekn
  });
  const currentTime = Date.now() / 1000;
  if(decode_jwtToekn.exp < currentTime){
    store.dispatch(logout())
    window.location.pathname= "/login";
  }
} else {
  if(window.location.pathname !== "/login") {
    window.location.pathname= "/login";
  }
}

// When the JWT expires mid-session every API call starts failing with 401 and
// each screen breaks in its own way; send the user back to the login page
// instead. The login call itself is excluded so the form shows its own error.
axios.interceptors.response.use(
  response => response,
  error => {
    const status = error.response && error.response.status;
    const url = (error.config && error.config.url) || "";
    if (status === 401 && url.indexOf("/api/user/login") < 0
        && window.location.pathname !== "/login") {
      store.dispatch(logout());
      window.location.pathname = "/login";
    }
    return Promise.reject(error);
  }
);

class App extends Component {

  // componentDidMount() {
  //   const jwtToken = localStorage.jwtToken;
  //   if(jwtToken){ 
  //     setJWTToken(jwtToken);
  //     const decode_jwtToekn = jwt_decode(jwtToken);
  //     store.dispatch({
  //         type: SET_CURRENT_USER,
  //         payload: decode_jwtToekn
  //     });  
  //     const currentTime = Date.now() / 1000;
  //     if(decode_jwtToekn.exp < currentTime){
  //       store.dispatch(logout())
  //       history.push("/login");
  //     }
  //   } else {
  //     if(window.location.pathname !== "/login") {
  //       history.push("/login");
  //     }
  //   }
  // }

  render() {
    return (
      <Provider store={store}>  
        <BrowserRouter history={history}>
          <div className="App">
            
            <Route exact path="/login" component={Landing} />     
            
            <Switch>
              <SecuredRoute exact path="/" component={Home} />
              <SecuredRoute exact path="/stockReportVerification" component={StockReportVerification} />
              <SecuredRoute exact path="/placement-view" component={Placement} />
              <SecuredRoute exact path="/profile" component={Profile} />
              <SecuredRoute exact path="/suiviQulite" component={SuiviQulite} />
              <SecuredRoute exact path="/user" component={Users} allowedRoles={["ROLE_ADMIN"]} />
              <SecuredRoute exact path="/rapportLectra" component={RapportLectra} /> 
              <SecuredRoute exact path="/rapportUsage" component={RapportUsage} />
              <SecuredRoute exact path="/rapportLectraV2" component={RapportLectraV2} /> 
              <SecuredRoute exact path="/rapportOverlap" component={RapportOverlap} />
              <SecuredRoute exact path="/rapportRestRouleau" component={RapportRestRouleau} />
              <SecuredRoute exact path="/rouleauSummary" component={RouleauSummary} />
              <SecuredRoute exact path="/statusMaintPerNiveau" component={StatusMaintPerNiveau} />
              <SecuredRoute exact path="/form" component={Form} />
              <SecuredRoute exact path="/demandeChangementSerieValidation" component={DemandeChangementSerieValidation} />

              <SecuredRoute exact path="/qualityNoticeValidation" component={qualityNoticeValidation} />
              <SecuredRoute exact path="/qualityNoticeForm" component={QualityNoticeForm} />
              <SecuredRoute exact path="/qualityCode" component={QualityCode} />
              <SecuredRoute exact path="/consumableStat" component={ConsumableStat} />
              <SecuredRoute exact path="/gammeCMS" component={GammeCMS} /> 
              <SecuredRoute exact path="/validationDefautRouleau" component={ValidationDefautRouleau} />
              <SecuredRoute exact path="/graphEmp" component={GraphEmp} />
              <SecuredRoute exact path="/statusMaintenance" component={StatusMaintenance} />
              <SecuredRoute exact path="/gammesTechnique" component={GammePnPage} />
              <SecuredRoute exact path="/statutPlanning" component={StatutPlanning} />
              <SecuredRoute exact path="/planning" component={Importation} />
              <SecuredRoute exact path="/preparation" component={ImportationNew} />
              <SecuredRoute exact path="/changementReftissu" component={ChangementReftissu} />
              <SecuredRoute exact path="/changementOptional" component={ChangementOptional} />
              <SecuredRoute exact path="/demande-de-coupe/:entityId?" component={DemandeDeCoupe} />
              <SecuredRoute exact path="/matelassage/:entityId?" component={Matelassage} />
              <SecuredRoute exact path="/boxes/:entityId?" component={Boxes} />
              <SecuredRoute exact path="/verificationQualite" component={VerificationQualite} />
              <SecuredRoute exact path="/processWorkbench" component={Workbench} allowedRoles={["ROLE_PROCESS", "ROLE_CHEF_DE_ZONE", "ROLE_CHEF_EQUIPE", "ROLE_ADMIN", "ROLE_VALID_QN_LOGISTIQUE"]} />
              <SecuredRoute exact path="/logisticsRelease" component={LogisticsRelease} allowedRoles={["ROLE_PROCESS", "ROLE_CHEF_DE_ZONE", "ROLE_CHEF_EQUIPE", "ROLE_ADMIN", "ROLE_VALID_QN_LOGISTIQUE", "ROLE_VARIANCE"]} />
              <SecuredRoute exact path="/tableFeed" component={TableFeed} allowedRoles={["ROLE_PROCESS", "ROLE_CHEF_DE_ZONE", "ROLE_CHEF_EQUIPE", "ROLE_ADMIN", "ROLE_VALID_QN_LOGISTIQUE", "ROLE_VARIANCE"]} />
              <SecuredRoute exact path="/productionFloor" component={ProductionFloor} allowedRoles={["ROLE_PROCESS", "ROLE_CHEF_DE_ZONE", "ROLE_CHEF_EQUIPE", "ROLE_ADMIN", "ROLE_VALID_QN_LOGISTIQUE", "ROLE_VARIANCE"]} />
              <SecuredRoute exact path="/sequenceRectification" component={SequenceRectification} allowedRoles={["ROLE_CHEF_DE_ZONE", "ROLE_CHEF_EQUIPE", "ROLE_ADMIN"]} />
              <SecuredRoute exact path="/processDispatcher" component={ProcessDispatcher} allowedRoles={["ROLE_PROCESS"]} />
              <SecuredRoute exact path="/engineControl" component={EngineControl} allowedRoles={["ROLE_PROCESS", "ROLE_CHEF_EQUIPE", "ROLE_ADMIN"]} />
              <SecuredRoute exact path="/chefDeZoneConfirm" component={ChefDeZoneConfirm} allowedRoles={["ROLE_PROCESS", "ROLE_CHEF_DE_ZONE", "ROLE_CHEF_EQUIPE", "ROLE_ADMIN"]} />
              <SecuredRoute exact path="/systemHealth" component={SystemHealth} allowedRoles={["ROLE_ADMIN"]} />
              <SecuredRoute exact path="/archiving" component={Archiving} allowedRoles={["ROLE_ADMIN"]} />
              <SecuredRoute exact path="/validationIntervention" component={ValidationIntervention} />
              <SecuredRoute exact path="/partNumberMaterialConfig/new" component={PartNumberMaterialConfig} />
              <SecuredRoute exact path="/partNumberMaterialConfig/:entityId?" component={PartNumberMaterialConfig} />
              <SecuredRoute exact path="/patternSearch" component={PatternSearch} />
              
              <SecuredRoute exact path="/changementSerie" component={ChangementSerie} />
              <SecuredRoute exact path="/rapportShortage" component={RapportShortage} />

              <SecuredRoute exact path="/rollQlaize" component={RollQlaize} />

              <SecuredRoute exact path="/toleranceForm" component={ToleranceForm} />

              <SecuredRoute exact path="/machineLog" component={MachineLog} />
              <SecuredRoute exact path="/consumableStatWeek" component={ConsumableStatWeek} />
              <SecuredRoute exact path="/ippmReport" component={IppmReport} />
              <SecuredRoute exact path="/kpiMaintenance" component={KpiMaintenance} />
              <SecuredRoute exact path="/ctcToleranceRuleForm" component={CtcToleranceRuleForm} />

              {/* Box Weight Management */}
              <SecuredRoute exact path="/boxWeightFilling" component={BoxWeightFilling} />
              <SecuredRoute exact path="/boxWeightVerifying" component={BoxWeightVerifying} />

              {/* CAD Piece Detail & Weight Calculation */}
              <SecuredRoute exact path="/pieceDetailImport" component={PieceDetailImport} />
              <SecuredRoute exact path="/weightCalculation" component={WeightCalculation} />
              <SecuredRoute exact path="/cuttingTimePerPartNumber" component={CuttingTimePerPartNumber} />

              {/* CNC PS */}
              <SecuredRoute exact path="/cncPs" component={CncPs} />
              <SecuredRoute exact path="/cncControl" component={CncControl} />
              <SecuredRoute exact path="/cncQualite" component={CncQualite} />
              <SecuredRoute exact path="/cncQualiteMachine" component={CncMachineQualiteReport} />
              <SecuredRoute exact path="/cncShiftStatus" component={CncShiftStatus} />
              <SecuredRoute exact path="/cncKpi" component={CncKpi} />
              <SecuredRoute exact path="/cncSync" component={CncSync} />
              <SecuredRoute exact path="/machineTypeSwap" component={MachineTypeSwap} />
              <SecuredRoute exact path="/perforationAnalyzer" component={PerforationDistanceAnalyzer} />
              <SecuredRoute exact path="/rapportMatieresAirbag" component={RapportMatieresAirbag} />
              <SecuredRoute exact path="/planDeCharge" component={PlanDeCharge} />
              <SecuredRoute exact path="/kpiChargeMachine" component={KpiChargeMachine} />
              <SecuredRoute exact path="/projetPLS" component={EntityList} />

              <SecuredRoute exact path="/cuttingPlan/new" component={CuttingPlan} />
              <SecuredRoute exact path="/cuttingPlan/:entityId?" component={CuttingPlan} />
              <SecuredRoute exact path="/cuttingPlanFoam/new" component={CuttingPlanFoam} />
              <SecuredRoute exact path="/cuttingPlanFoam/:entityId?" component={CuttingPlanFoam} />
              <SecuredRoute exact path="/cuttingPlanCombination/new" component={CuttingPlanCombinaison} />
              <SecuredRoute exact path="/cuttingPlanCombination/:entityId?" component={CuttingPlanCombinaison} />
              <SecuredRoute exact path="/:entity/:entityId?" component={EntityList} />
              <SecuredRoute path="*" component={NoMatchRoute} />
            </Switch>
            <ChatWidget />
          </div> 
        </BrowserRouter>
      </Provider>
    )
  }
}

export default App
