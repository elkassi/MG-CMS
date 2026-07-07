import axios from 'axios'
import React, { Component } from 'react'
import "../../styles/StatusMaintenance.scss"
import { Modal } from 'react-bootstrap'
import moment from 'moment'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faArrowRight } from '@fortawesome/free-solid-svg-icons'

export default class StatusMaintenance extends Component {

  constructor(props) {
    super(props)
    this.state = {
      machineList: [],
      maintenanceInterventionConfigList: [],
      maintenanceInterventionList: [],
      selectedMachine: null
    }
  }

  componentDidMount() {
    axios.get(`/api/maintenanceInterventionConfig/list`)
      .then(res => {
        const maintenanceInterventionConfigList = res.data;
        this.setState({ maintenanceInterventionConfigList });
        axios.get(`/api/productionTable/list`)
          .then(res2 => {
            const machineList = res2.data.filter(machine => machine.vibrationTime && machine.vibrationTime > 0);
            axios.get(`/api/maintenanceIntervention/listByMachine?machines=${machineList.map(machine => machine.nom).join(',')}`)
              .then(res3 => {
                const maintenanceInterventionList = res3.data;
                this.setState({ maintenanceInterventionList });
                machineList.forEach(machine => {
                  // view metadata sutructure to not make mistakes
                  const maintenanceInterventionArr = maintenanceInterventionList.filter(maintenanceIntervention => maintenanceIntervention.machine === machine.nom);
                  const configList = res.data.filter(config => config.machineType === machine.machineType.name);
                  configList.forEach(config => {
                    let lastCounterIntervention = 0;
                    // find the last maintenance intervention with the higher date and service type is the same as config and take the counter, if not found take 0 
                    const filteredList = maintenanceInterventionArr.filter(maintenanceIntervention => maintenanceIntervention.serviceType === config.serviceType && !(maintenanceIntervention.serviceType === "Greasing" && maintenanceIntervention.service === "M125"));
                    if (filteredList.length > 0) {
                      const lastMaintenanceIntervention = filteredList.reduce((a, b) => a.date > b.date ? a : b);
                      lastCounterIntervention = lastMaintenanceIntervention ? lastMaintenanceIntervention.counter || 0 : 0;
                    } else {
                      lastCounterIntervention = 0;
                    }
                    machine[config.serviceType] = lastCounterIntervention;
                  });
                });
                this.setState({ machineList });
              })
          })
      })
  }

  convertFloat = (float) => {
    if (!float || isNaN(float)) {
      return null;
    }
    return parseFloat(parseFloat(float).toFixed(2));
  }

  renderModalDetailMachine = () => {
    let arrHistorique = [];
    if (this.state.selectedMachine === null) {
      return null;
    }
    arrHistorique = this.state.maintenanceInterventionList.filter(maintenanceIntervention => maintenanceIntervention.machine === this.state.selectedMachine.nom).sort((a, b) => b.date.localeCompare(a.date));
    const configList = this.state.maintenanceInterventionConfigList.filter(config => config.machineType === this.state.selectedMachine.machineType.name);
    let alert = false
    let arr = configList.map((config, index) => {
      const lastCounterIntervention = this.state.selectedMachine[config.serviceType] || 0;
      const lastValidationNumber = parseInt((lastCounterIntervention + config.frequency * (1 - (config.notificationPercentage/100))) / config.frequency);
      
      let percentageToNextValidation = (this.state.selectedMachine.vibrationTime - lastValidationNumber * config.frequency) * 100 / config.frequency;
      if(config.serviceType === "Greasing") {
        percentageToNextValidation = (this.state.selectedMachine.vacuumTime - lastValidationNumber * config.frequency) * 100 / config.frequency;
      }

      if (this.convertFloat(percentageToNextValidation) > 90) {
        alert = true;
      }
      return <div key={index}>
        <h6 className='card-subtitle mb-2 text-muted'>
          {config.serviceType} {config.frequency}: {this.convertFloat(lastCounterIntervention)} / <span style={this.convertFloat(percentageToNextValidation) > 90 ? { backgroundColor: "red", color: "white", padding: "0 5" } : { padding: "0 5" }}>{this.convertFloat(percentageToNextValidation)}%</span>
          
        </h6>
      </div>
    })

    // calculate the somme of time in arrHistorique
    let somme = 0;
    arrHistorique.forEach(maintenanceIntervention => {
      somme += maintenanceIntervention.time;
    })

    return <Modal
      show={this.state.selectedMachine !== null}
      onHide={() => this.setState({ selectedMachine: null })}
      dialogClassName="modal-90w"
      centered
    >
      {this.state.selectedMachine && <div style={{ maxHeight: "75vh", overflowY: 'auto' }}>
        <h2 className='text-center my-2'>Machine {this.state.selectedMachine.nom} {this.state.selectedMachine.machineType && ` : ${this.state.selectedMachine.machineType.name}`}</h2>
        <div style={{ display: "flex" }}>
          <div className='card m-2' style={{ width: "50%" }}>
            <div className='card-body'>
              <h5 className='card-title'>Compteurs:</h5>
              <h6 className='card-subtitle mb-2 text-muted'>Vibration Time: {this.state.selectedMachine.vibrationTime}</h6>
              <h6 className='card-subtitle mb-2 text-muted'>Vacuum Time: {this.state.selectedMachine.vacuumTime}</h6>
            </div>
          </div>
          <div className='card m-2' style={{ width: "50%" }}>
            <div className='card-body'>
              <h5 className='card-title'>Général</h5>
              <h6 className='card-subtitle mb-2 text-muted'>Date d'installation: {this.state.selectedMachine.installationDate}</h6>
              <h6 className='card-subtitle mb-2 text-muted'>Nombre d'interventions: {arrHistorique.length}</h6>
              <h6 className='card-subtitle mb-2 text-muted'>Temps Total d'intervention: {this.convertSecToHeur(somme)}</h6>
            </div>
          </div>
        </div>
        <div className='card m-2'>
          <div className='card-body' style={alert ? { borderColor: "#fdeded" } : {}}>
            {arr}
          </div>
        </div>
        <div className='card m-2'>
          <div className='card-body'>
            <h5 className='card-title'>Historique</h5>
            <div style={{ overflow: "auto", maxHeight: 300 }}>
              <table className="table table-striped">
                <thead style={{ position: "sticky", top: -1, backgroundColor: "white" }}>
                  <tr>
                    <th scope="col">Date</th>
                    <th scope="col">Service Type</th>
                    <th scope="col">Service</th>
                    <th scope="col">Compteur</th>
                    <th scope="col">Temps</th>
                    <th scope="col">comment</th>
                  </tr>
                </thead>
                <tbody>
                  {arrHistorique.map((maintenanceIntervention, index) => {
                    return <tr key={index}>
                      <td>{maintenanceIntervention.date && maintenanceIntervention.date.replace("T", " ")}</td>
                      <td>{maintenanceIntervention.serviceType}</td>
                      <td>{maintenanceIntervention.service}</td>
                      <td>{maintenanceIntervention.counter}</td>
                      <td>{this.convertSecToHeur(maintenanceIntervention.time)}</td>
                      <td>{maintenanceIntervention.comment}</td>
                    </tr>
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>

      </div>}
    </Modal>
  }

  convertSecToHeur(sec) {
    //convert it to the string format hh:mm:ss
    let hours = Math.floor(sec / 3600);
    let minutes = Math.floor((sec - hours * 3600) / 60);
    let seconds = sec - hours * 3600 - minutes * 60;
    // format hh:mm:ss
    return (
      ("0" + hours).slice(-2) +
      ":" +
      ("0" + minutes).slice(-2) +
      ":" +
      ("0" + seconds).slice(-2)
    );
  }

  render() {
    return (
      <div>
        <h1 className='text-center my-2'>Status Maintenance</h1>
        <div style={{ display: "flex", flexWrap: "wrap", justifyContent: "center" }}>
          {this.state.machineList
          .sort((a, b) => a.nom.localeCompare(b.nom))
          .map((machine, index) => {
            const configList = this.state.maintenanceInterventionConfigList.filter(config => config.machineType === machine.machineType.name);
            let alert = false
            let arr = configList.map((config, index) => {
              const lastCounterIntervention = machine[config.serviceType] || 0;
              const lastValidationNumber = parseInt((lastCounterIntervention + config.frequency * (1 - (config.notificationPercentage/100))) / config.frequency);
              let percentageToNextValidation = (machine.vibrationTime - lastValidationNumber * config.frequency) * 100 / config.frequency;
              if(config.serviceType === "Greasing") {
                percentageToNextValidation = (machine.vacuumTime - lastValidationNumber * config.frequency) * 100 / config.frequency;
              }
              if (this.convertFloat(percentageToNextValidation) > 90) {
                alert = true;
              }
              return <div key={index}>
                <h6 className='card-subtitle mb-2 text-muted'>
                  {config.serviceType} {config.frequency}: {this.convertFloat(lastCounterIntervention)} / <span style={this.convertFloat(percentageToNextValidation) > 100 ? { backgroundColor: "red", color: "white", padding: "0 5" } : this.convertFloat(percentageToNextValidation) > 90 ? { backgroundColor: "#f9c000", color: "white", padding: "0 5" } : { padding: "0 5" }}>{this.convertFloat(percentageToNextValidation)}%</span>
                  <br/>
                  {lastValidationNumber * config.frequency} <FontAwesomeIcon icon={faArrowRight} /> {config.serviceType === "Greasing" ? this.convertFloat(machine.vacuumTime) : this.convertFloat(machine.vibrationTime)} 
                  </h6>
              </div>
            })
            return <div key={index} className='card m-2 clickable-element' style={{ width: "380px" }} onDoubleClick={() => {
              this.setState({ selectedMachine: machine })
            }}>
              <div className='card-body' style={alert ? { borderColor: "#fdeded" } : {}}>
                <h5 className='card-title'>{machine.nom} {machine.pcCoupe} {machine.machineType && ` : ${machine.machineType.name}`}</h5>
                <h6 className='card-subtitle mb-2 text-muted'>Vibration Time: {machine.vibrationTime}</h6>
                <h6 className='card-subtitle mb-2 text-muted'>Vacuum Time: {machine.vacuumTime}</h6>
                {arr}
              </div>
            </div>
          })}
        </div>
        {this.renderModalDetailMachine()}
      </div>
    )
  }

}
