import { faEye, faPrint, faTimes, faSync, faSpinner, faMessage, faEnvelope, faStar, faCog } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import axios from 'axios'
import React, { Component } from 'react'
import Select from "react-select";
import ReactToPrint from "react-to-print";
import GammePn from './GammePn';
import { Modal } from 'react-bootstrap'
import moment from 'moment';
import { optionsShift, optionTypeDemandeChangementSerie } from '../../metadata';
import logo from '../../assets/images/lear_logo.png'
import FormCoupe from './FormCoupe';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import "../../styles/DemandeDeCoupeForm.scss"
class DemandeDeCoupeForm extends Component {

    constructor() {
        super()
        this.state = {
            modalObj: {},
            optionsList: {},
            machineRow: null,
            marginRow: null,
            reftissuCopy: "",
            partNumberMaterialConfigs: {},
            hideColumn: false,
            loading: false,
            selectedBoxs: [],
            showSerieForm: null,
            loadingRefresh: false,
            qnList: [],
            optionnals: {},
            optionnalsQuality: {},
            modalSerieForm: null,
            modalSerieFormLoading: false,
            modalSerieQuality: null,
            modalSerieQualityLoading: false,
        }
        this.inputArr = []
    }

    convertFloat = (float, digit) => {
		//check if the float is a number
		if (!float || isNaN(float)) {
			return float
		}
		return parseFloat(parseFloat(float).toFixed(digit))
	}

    componentDidMount() {
        const { entityId } = this.props
        if (entityId) {
            this.loadSequence(entityId)
        }
        axios.get(`/api/machineType/list`)
            .then((res) => {
                this.setState({ optionsList: { ...this.state.optionsList, machineType: res.data.map(elem => { return { label: elem.name, value: elem.name } }) } })
            })

    }

    loadSequence = async (sequence) => {
        let res = await axios.get(`/api/cuttingRequestData/${sequence}`)
        if (res.data) {
            this.setState({ modalObj: res.data })
            let resSerie = await axios.get(`/api/cuttingRequestSerieData/bySequence/${sequence}`)
            if (resSerie.data) {
                this.setState({ modalObj: { ...this.state.modalObj, cuttingRequestSeries: resSerie.data } })
            }
            let arrReftissu = this.state.modalObj.cuttingRequestSeries.map((elem) => elem.partNumberMaterial.toUpperCase())
            let resQNs = await axios.get(`/api/qn/reftissu?reftissus=${arrReftissu.join(",")}`)
            this.setState({ qnList: resQNs.data })
            let resPartNumbers = await axios.get(`/api/cuttingRequestPartNumberData/bySequence/${sequence}`)
            if (resPartNumbers.data) {
                this.setState({ modalObj: { ...this.state.modalObj, cuttingRequestPartNumbers: resPartNumbers.data } })
            }
            let resBoxs = await axios.get(`/api/cuttingRequestBoxData/bySequence/${sequence}`)
            if (resBoxs.data) {
                let obj = { ...this.state.modalObj, cuttingRequestBoxs: resBoxs.data }
                let counterPerPartnumber = {};
                obj.cuttingRequestBoxs = obj.cuttingRequestBoxs.map((e) => {
                    if (counterPerPartnumber[e.partNumber]) {
                        counterPerPartnumber[e.partNumber] = counterPerPartnumber[e.partNumber] + 1;
                    } else {
                        counterPerPartnumber[e.partNumber] = 1;
                    }
                    return {
                        ...e,
                        counter: counterPerPartnumber[e.partNumber],
                        total: obj.cuttingRequestBoxs.filter(elem => elem.partNumber == e.partNumber).length,
                        sequence: sequence,
                    }
                })
                this.setState({ modalObj: obj })
            }
            let resOptionnal = await axios.get("/api/cuttingPlanMaterialPlacementData/cuttingPlan/" + res.data.cuttingPlanId)
            let obj = {}
            let objQuality = {}
            resSerie.data.map(serie => {
                let cpmp = resOptionnal.data
                    .find(e => e.placement.toUpperCase().trim() === serie.placement.toUpperCase().trim())
                if (cpmp) {
                    let arrOpt = resOptionnal.data.filter(e => (
                        e.partNumberMaterial == cpmp.partNumberMaterial &&
                        e.groupPlacement == cpmp.groupPlacement &&
                        e.maxPlie == cpmp.maxPlie &&
                        e.maxPlieDrill == cpmp.maxPlieDrill &&
                        e.nbrCouche == cpmp.nbrCouche &&
                        e.placement.toUpperCase().trim() !== serie.placement.toUpperCase().trim()
                        // e.laize == cpmp.laize &&
                        // && e.partNumbers == cpmp.partNumbers
                        // e.machine.startsWith("Lectra")
                    ))
                    obj[serie.placement.toUpperCase().trim()] = arrOpt
                }
            })

            resSerie.data.map(serie => {
                let arrOpt = resOptionnal.data.filter(e => (
                    e.partNumberMaterial == serie.partNumberMaterial &&
                    // e.maxPlie == serie.maxPlie &&
                    // e.maxPlieDrill == serie.maxPlieDrill &&
                    // e.nbrCouche == serie.nbrCouche &&
                    e.placement.toUpperCase().trim() !== serie.placement.toUpperCase().trim()
                ))
                objQuality[serie.placement.toUpperCase().trim()] = arrOpt

            })
            // resOptionnal.data.map(e => {
            //     if (e.placement && e.placement.replaceAll("-", "").replaceAll("_", "").substring(0, 4) && e.partNumberMaterial) {
            //         if (!obj[e.placement.replaceAll("-", "").replaceAll("_", "").substring(0, 4)]) {
            //             obj[e.placement.replaceAll("-", "").replaceAll("_", "").substring(0, 4)] = []
            //         }
            //         obj[e.placement.replaceAll("-", "").replaceAll("_", "").substring(0, 4)] = [...obj[e.placement.replaceAll("-", "").replaceAll("_", "").substring(0, 4)], e]
            //     }
            // })

            this.setState({ optionnals: obj, optionnalsQuality: objQuality })
        } else {
            this.setState({ error: "Sequence not found" })
        }
    }

    returnColorStatus = (status) => {
        switch (status) {
            case "Waiting":
                return "#ffafaf"
            case "In progress":
                return "#f6ff6b"
            case "Complete":
                return "#7bff6b"
            case "Incomplete":
                return "#ffc46b"
            default:
                return ""
        }
    }

    getStatusClass = (status) => {
        switch (status) {
            case "Waiting":
                return "status-waiting"
            case "In progress":
                return "status-progress"
            case "Complete":
                return "status-complete"
            case "Incomplete":
                return "status-incomplete"
            default:
                return ""
        }
    }




    serieFormModal = () => {
        const { user } = this.props;
        let arr = []
        if (!this.state.modalSerieForm) {
            return null
        }
        if (this.state.modalSerieForm && this.state.modalSerieForm.partNumberMaterial && this.state.modalSerieForm.placement && this.state.optionnals[this.state.modalSerieForm.placement.toUpperCase().trim()]) {
            arr = this.state.optionnals[this.state.modalSerieForm.placement.toUpperCase().trim()]
                .filter(e => (
                    e.placement != this.state.modalSerieForm.placement &&
                    e.partNumberMaterial == this.state.modalSerieForm.partNumberMaterial
                ))
        }
        let serieObj = this.state.modalObj.cuttingRequestSeries.find(e => e.serie === this.state.modalSerieForm.serie)
        let laizeContractuel = null
        if(this.state.dataIMS && this.state.dataIMS.laize) {
            laizeContractuel = this.convertFloat(parseFloat(this.state.dataIMS.laize) / 1000, 3)
        }
        return <Modal
            show={this.state.modalSerieForm !== null}
            onHide={() => this.setState({ modalSerieForm: null })}
            dialogClassName="modal-90w"
            centered
        >
            {this.state.modalSerieForm && <div className="modal-serie-form-container">
                <h4 className='text-center my-2'>Modification du Serie {this.state.modalSerieForm.serie}</h4>
                <hr />
                <div className='container mb-3'>
                    <div className='row'>
                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Type Demande</label>
                                <div className="form-section-title">
                                    <Select
                                        classNamePrefix="rs"
                                        placeholder={"Type Demande"}
                                        isClearable={false}
                                        value={this.state.modalSerieForm.typeDemande
                                            ? { label: this.state.modalSerieForm.typeDemande, value: this.state.modalSerieForm.typeDemande }
                                            : null
                                        }
                                        options={optionTypeDemandeChangementSerie || []}
                                        onChange={(option) => {
                                            if (option) {
                                                this.setState({
                                                    modalSerieForm: { ...this.state.modalSerieForm, typeDemande: option.value, machine: null, laize: null, config: null },
                                                    allowSaving: false
                                                })
                                            }
                                        }}
                                    />
                                </div>
                            </div>
                        </div>

                        {this.state.modalSerieForm.typeDemande && this.state.modalSerieForm.typeDemande === "Machine"
                            ? <div className='col-6'>
                                <div className='form-group'>
                                    <label>Machine</label>
                                    <div className="form-section-title">
                                        <Select
                                            classNamePrefix="rs"
                                            placeholder={"machine"}
                                            isClearable={false}
                                            value={this.state.modalSerieForm.machine
                                                ? { label: this.state.modalSerieForm.machine.name, value: this.state.modalSerieForm.machine.name }
                                                : null
                                            }
                                            options={this.state.optionsList.machineType || []}
                                            onChange={(option) => {
                                                if (option) {
                                                    this.setState({ modalSerieForm: { ...this.state.modalSerieForm, machine: { name: option.value } }, allowSaving: false })
                                                }
                                            }}
                                        />
                                    </div>

                                </div>
                            </div>
                            : (this.state.modalSerieForm.typeDemande && (this.state.modalSerieForm.typeDemande.startsWith("QLaize") || this.state.modalSerieForm.typeDemande.startsWith("Overlaize")))
                                ? <div className='col-6'>
                                    <div className='form-group'>
                                        <label>Laize</label>
                                        <input type='text' className='form-control'
                                            value={this.state.modalSerieForm.laize}
                                            onChange={(e) => {
                                                let modalSerieForm = this.state.modalSerieForm;
                                                modalSerieForm.laize = e.target.value;
                                                this.setState({ modalSerieForm })
                                            }}
                                        />
                                    </div>
                                </div>
                                : (this.state.modalSerieForm.typeDemande && this.state.modalSerieForm.typeDemande === "Changement de config")
                                ? <div className='col-6'>
                                    <div className='form-group'>
                                        <label>Config</label>
                                        <input type='text' className='form-control'
                                            value={this.state.modalSerieForm.config || ''}
                                            onChange={(e) => {
                                                let modalSerieForm = this.state.modalSerieForm;
                                                modalSerieForm.config = e.target.value;
                                                this.setState({ modalSerieForm })
                                            }}
                                            placeholder="Entrez la configuration"
                                        />
                                    </div>
                                </div>
                                : (this.state.modalSerieForm.typeDemande && this.state.modalSerieForm.typeDemande === "Erreur métrage")
                                ? <div className='col-6'>
                                    <div className='form-group'>
                                        <label>Longueur NOK (m)</label>
                                        <input type='text' className='form-control'
                                            value={this.state.modalSerieForm.laize || ''}
                                            onChange={(e) => {
                                                let modalSerieForm = this.state.modalSerieForm;
                                                modalSerieForm.laize = e.target.value;
                                                this.setState({ modalSerieForm })
                                            }}
                                            placeholder="Longueur non conforme mesurée"
                                        />
                                    </div>
                                </div>
                                : <div className='col-6'></div>}

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Autre Changement</label>
                                <textarea type='text' className='form-control'
                                    value={this.state.modalSerieForm.autreChangement}
                                    onChange={(e) => {
                                        let modalSerieForm = this.state.modalSerieForm;
                                        modalSerieForm.autreChangement = e.target.value;
                                        this.setState({ modalSerieForm })
                                    }}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>description</label>
                                <textarea type='text' className='form-control'
                                    value={this.state.modalSerieForm.description}
                                    onChange={(e) => {
                                        let modalSerieForm = this.state.modalSerieForm;
                                        modalSerieForm.description = e.target.value;
                                        this.setState({ modalSerieForm })
                                    }}
                                />
                            </div>
                        </div>
                        {this.state.dataIMS &&
                            <div className='col-12'>
                                <small className='text-muted'>
                                    {this.state.dataIMS.laize && `(La laize contractuelle : ${laizeContractuel} m)`}
                                </small>
                            </div>
                        }
                    </div>
                </div>
                <hr />

                <div className='d-flex justify-content-center mb-2'>
                    <button className='btn btn-primary mx-2'
                        disabled={
                            this.state.modalSerieFormLoading
                            || this.state.modalSerieForm.typeDemande === null
                            || (this.state.modalSerieForm.typeDemande === "Machine" && !this.state.modalSerieForm.machine)
                            || (this.state.modalSerieForm.typeDemande && (this.state.modalSerieForm.typeDemande.startsWith("QLaize")  || this.state.modalSerieForm.typeDemande.startsWith("Overlaize")) && !this.state.modalSerieForm.laize)
                            || (this.state.modalSerieForm.typeDemande === "Changement de config" && (!this.state.modalSerieForm.config || this.state.modalSerieForm.config.trim() === ""))
                            || (this.state.modalSerieForm.typeDemande === "Erreur métrage" && !this.state.modalSerieForm.laize)
                        }
                        onClick={() => {
                            // Le plafond 3 m ne vaut que pour une LAIZE (QLaize/Overlaize);
                            // une longueur NOK (Erreur métrage) dépasse légitimement 3 m.
                            const isLaizeDemande = this.state.modalSerieForm.typeDemande
                                && (this.state.modalSerieForm.typeDemande.startsWith("QLaize") || this.state.modalSerieForm.typeDemande.startsWith("Overlaize"));
                            if(isLaizeDemande && this.convertFloat(this.state.modalSerieForm.laize, 3) > 3) {
                                alert("Laize "+ this.convertFloat(this.state.modalSerieForm.laize, 3) + " > 3")
                                return;
                            }
                            if(this.state.dataIMS 
                                && this.state.modalSerieForm.typeDemande.startsWith("Overlaize") 
                                && this.state.dataIMS.laize && laizeContractuel > this.convertFloat(this.state.modalSerieForm.laize, 3)
                            ) {
                                alert("laizeContractuel " + laizeContractuel +  " > " + this.convertFloat(this.state.modalSerieForm.laize, 3))
                                return;
                            }
                            this.setState({ modalSerieFormLoading: true })
                            axios.post(`/api/demandeChangementSerie`, this.state.modalSerieForm)
                                .then(res => {
                                    this.setState({ modalSerieForm: null, modalSerieFormLoading: false })
                                })
                                .catch(() => {
                                    this.setState({ modalSerieFormLoading: false })
                                })

                        }}>
                        {this.state.modalSerieFormLoading && <FontAwesomeIcon icon={faSpinner} spin />} Envoyer la demande
                    </button>
                </div>

                <hr />

                <div className='container'>
                    <div className='row'>
                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Part Number Material</label>
                                <input type='text' className='form-control'
                                    value={serieObj ? serieObj.partNumberMaterial : ''}
                                    readOnly
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>description</label>
                                <input type='text' className='form-control'
                                    value={serieObj ? serieObj.description : ''}
                                    readOnly
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Sens</label>
                                <input type='text' className='form-control'
                                    value={serieObj ? serieObj.matelassageEndroit : ''}
                                    readOnly
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>longueur</label>
                                <input type='text' className='form-control'
                                    value={serieObj ? serieObj.longueur : ''}
                                    readOnly
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>nbrCouche</label>
                                <input type='text' className='form-control'
                                    value={serieObj ? serieObj.nbrCouche : ''}
                                    readOnly
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>laize</label>
                                <input type='text' className='form-control'
                                    value={serieObj ? serieObj.laize : ''}
                                    readOnly
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Placement</label>
                                <input type='text' className='form-control'
                                    value={serieObj ? serieObj.placement : ''}
                                    readOnly
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Config</label>
                                <input type='text' className='form-control'
                                    value={serieObj ? serieObj.config : ''}
                                    readOnly
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>drill</label>
                                <input type='text' className='form-control'
                                    value={serieObj ? serieObj.drill : ''}
                                    readOnly
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>machine</label>
                                <input type='text' className='form-control'
                                    value={serieObj ? serieObj.machine : ''}
                                    readOnly
                                />
                            </div>
                        </div>
                    </div>
                </div>
            </div>}
        </Modal>
    }

    serieModal = () => {
        return <Modal
            show={this.state.showSerieForm !== null}
            onHide={() => this.setState({ showSerieForm: null })}
            dialogClassName="modal-90w"
            centered
        >
            {this.state.showSerieForm && <div className="serie-form-container">
                <FormCoupe serie={this.state.showSerieForm} />
            </div>}
        </Modal>
    }
    convertFloat = (float, digit) => {
        //check if the float is a number
        if (!float || isNaN(float)) {
            return float
        }
        return parseFloat(parseFloat(float).toFixed(digit))
    }

    // 
    serieFormQualityModal = () => {
        const { user } = this.props;

        let arr1 = [], arr2 = []
        if (this.state.modalSerieQuality && this.state.modalSerieQuality.partNumberMaterial) {
            const qualityOptions = this.state.optionnalsQuality[this.state.modalSerieQuality.placement.toUpperCase().trim()];
            if (qualityOptions) {
                arr1 = qualityOptions
                    .filter(e => (
                        e.placement !== this.state.modalSerieQuality.placement
                        && e.partNumberMaterial === this.state.modalSerieQuality.partNumberMaterial
                        && e.laize === this.state.modalSerieQuality.laize
                    ))
                arr2 = qualityOptions
                    .filter(e => (
                        e.placement !== this.state.modalSerieQuality.placement
                        && e.partNumberMaterial === this.state.modalSerieQuality.partNumberMaterial
                        && e.laize !== this.state.modalSerieQuality.laize
                    ))
            }
        }
        return <Modal
            show={this.state.modalSerieQuality != null}
            onHide={() => this.setState({ modalSerieQuality: null })}
            dialogClassName="modal-90w"
            centered
        >
            {this.state.modalSerieQuality && <div className="modal-serie-quality-container">
                <h4 className='text-center my-2'>Options Qualité du Serie {this.state.modalSerieQuality.serie}</h4>
                <hr />
                <div className='container'>
                    <div className='row'>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Part Number Material</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerieQuality.partNumberMaterial}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>description</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerieQuality.description}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Sens</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerieQuality.matelassageEndroit}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>longueur</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerieQuality.longueur}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>nbrCouche</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerieQuality.nbrCouche}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>laize</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerieQuality.laize}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Placement</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerieQuality.placement}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Config</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerieQuality.config}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>drill</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerieQuality.drill}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>machine</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerieQuality.machine}
                                />
                            </div>
                        </div>
                    </div>
                </div>
                <hr />
                {arr1.length > 0 && <div>
                    <h2 className='text-center my-2'>Options Qualité machine {this.state.modalSerieQuality.serie}</h2>
                    <div>
                        <table className='table table table-grey-border'>
                            <thead style={{ backgroundColor: "#aaaa00", color: "white" }}>
                                <tr>
                                    <th>longueur</th>
                                    <th>nbrCouche</th>
                                    <th>laize</th>
                                    <th>Placement</th>
                                    <th>Config</th>
                                    <th>drill</th>
                                    <th>machine</th>
                                    <th style={{ width: 10 }}></th>
                                </tr>
                            </thead>
                            <tbody>
                                {arr1.map(e => {
                                    return <tr>
                                        <td>{this.convertFloat(e.longueurMatelas / e.nbrCouche, 3)}</td>
                                        <td>{e.nbrCouche}
                                        </td>
                                        <td>{e.laize}</td>
                                        <td>{e.placement}</td>
                                        <td>{e.config}</td>
                                        <td>{e.drill}</td>
                                        <td>{e.machine}</td>
                                        <td style={{ padding: "3px" }}>
                                            <button className='btn btn-primary mx-2'
                                                disabled={this.state.changeLoading}
                                                onClick={() => {
                                                    this.setState({ changeLoading: true })
                                                    axios.post(`/api/cuttingRequestSerieInfo/modify`, {
                                                        ...this.state.modalSerieQuality,

                                                        // description: this.state.modalSerieQuality.description,
                                                        // matelassageEndroit: this.state.modalSerieQuality.matelassageEndroit,
                                                        longueur: this.convertFloat(e.longueurMatelas / e.nbrCouche, 3),
                                                        // nbrCouche: e.nbrCouche,
                                                        laize: e.laize,
                                                        placement: e.placement,
                                                        config: e.config,
                                                        drill: e.drill,
                                                        machine: e.machine
                                                    })
                                                        .then(res => {
                                                            axios.post("/api/cuttingRequestSerieRouleauHistory", {
                                                                content: "Modification QUALITÉ serie " + this.state.modalSerieQuality.serie + " :"
                                                                    + " longueur " + e.longueur
                                                                    + " placement " + e.placement
                                                                    + " Config " + e.config
                                                                    + " machine " + e.machine
                                                                    + " drill " + e.drill
                                                                    + " maxPlie " + e.maxPlie
                                                                    + " laize " + e.laize
                                                                ,
                                                                serie: this.state.modalSerieQuality.serie,
                                                                changedAt: moment(),
                                                                changedBy: user.lastName + " " + user.firstName
                                                            })
                                                                .then(res => {
                                                                    this.setState({ modalSerieQuality: null, changeLoading: false })
                                                                    this.loadSequence(this.state.modalObj.sequence)
                                                                })
                                                                .catch(() => {
                                                                    this.setState({ changeLoading: false })
                                                                })

                                                        })
                                                        .finally(() => {
                                                            this.setState({ modalSerieQuality: null, changeLoading: false })
                                                        })
                                                }}>
                                                Modifier
                                            </button>
                                        </td>
                                    </tr>
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>}
                {arr2.length > 0 && <div>
                    <h2 className='text-center my-2'>Options Qualité laize et autre {this.state.modalSerieQuality.serie}</h2>
                    <div>
                        <table className='table table table-grey-border'>
                            <thead style={{ backgroundColor: "#ff0000", color: "white" }}>
                                <tr>
                                    <th>longueur</th>
                                    <th>nbrCouche</th>
                                    <th>laize</th>
                                    <th>Placement</th>
                                    <th>Config</th>
                                    <th>drill</th>
                                    <th>machine</th>
                                    <th style={{ width: 10 }}></th>
                                </tr>
                            </thead>
                            <tbody>
                                {arr2.map(e => {
                                    return <tr>
                                        <td>{this.convertFloat(e.longueurMatelas / e.nbrCouche, 3)}</td>
                                        <td>{e.nbrCouche}{e.maxPlie && ` / Max Plie : ${e.maxPlie} `}{e.maxPlieDrill && ` / Max Plie Drill : ${e.maxPlieDrill} `}</td>
                                        <td>{e.laize}</td>
                                        <td>{e.placement}</td>
                                        <td>{e.config}</td>
                                        <td>{e.drill}</td>
                                        <td>{e.machine}</td>
                                        <td style={{ padding: "3px" }}>
                                            <button className='btn btn-primary mx-2'
                                                disabled={this.state.changeLoading}
                                                onClick={() => {
                                                    this.setState({ changeLoading: true })
                                                    axios.post(`/api/cuttingRequestSerieInfo/modify`, {
                                                        ...this.state.modalSerieQuality,
                                                        placement: e.placement,
                                                        laize: e.laize,
                                                        drill: e.drill,
                                                        longueur: this.convertFloat(e.longueurMatelas / e.nbrCouche, 3),
                                                        config: e.config,
                                                        machine: e.machine
                                                    })
                                                        .then(res => {
                                                            axios.post("/api/cuttingRequestSerieRouleauHistory", {
                                                                content: "Modification QUALITÉ serie " + this.state.modalSerieQuality.serie + " :"
                                                                    + " longueur " + e.longueur
                                                                    + " placement " + e.placement
                                                                    + " Config " + e.config
                                                                    + " machine " + e.machine
                                                                    + " drill " + e.drill
                                                                    + " nbrCouche " + e.nbrCouche
                                                                    + " laize " + e.laize
                                                                ,
                                                                serie: this.state.modalSerieQuality.serie,
                                                                changedAt: moment(),
                                                                changedBy: user.lastName + " " + user.firstName
                                                            })
                                                                .then(res => {
                                                                    this.setState({ modalSerieQuality: null, changeLoading: false })
                                                                    this.loadSequence(this.state.modalObj.sequence)
                                                                })
                                                                .catch(() => {
                                                                    this.setState({ changeLoading: false })
                                                                })

                                                        })
                                                        .finally(() => {
                                                            this.setState({ modalSerieQuality: null, changeLoading: false })
                                                        })
                                                }}>
                                                Modifier
                                            </button>
                                        </td>
                                    </tr>
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>}
            </div>}
        </Modal>
    }

    serieModifModal = () => {
        const { user } = this.props;


        let arr1 = [], arr2 = []
        if (this.state.modalSerie && this.state.modalSerie.partNumberMaterial) {
            arr1 = this.state.optionnals[this.state.modalSerie.placement.toUpperCase().trim()]
                .filter(e => (
                    e.placement !== this.state.modalSerie.placement
                    && e.partNumberMaterial === this.state.modalSerie.partNumberMaterial
                    && e.laize === this.state.modalSerie.laize
                ))
            arr2 = this.state.optionnals[this.state.modalSerie.placement.toUpperCase().trim()]
                .filter(e => (
                    e.placement !== this.state.modalSerie.placement
                    && e.partNumberMaterial === this.state.modalSerie.partNumberMaterial
                    && e.laize !== this.state.modalSerie.laize
                ))
        }
        return <Modal
            show={this.state.modalSerie != null}
            onHide={() => this.setState({ modalSerie: null })}
            dialogClassName="modal-90w"
            centered
        >
            {this.state.modalSerie && <div className="modal-serie-container">
                <h4 className='text-center my-2'>Modification du Serie {this.state.modalSerie.serie}</h4>
                <hr />
                <div className='container'>
                    <div className='row'>


                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Part Number Material</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerie.partNumberMaterial}
                                // onChange={(e) => {
                                // 	let modalSerie = this.state.modalSerie;
                                // 	modalSerie.partNumberMaterial = e.target.value;
                                // 	this.setState({ modalSerie })
                                // }}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>description</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerie.description}
                                // onChange={(e) => {
                                // 	let modalSerie = this.state.modalSerie;
                                // 	modalSerie.description = e.target.value;
                                // 	this.setState({ modalSerie })
                                // }}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Sens</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerie.matelassageEndroit}
                                // onChange={(e) => {
                                // 	let modalSerie = this.state.modalSerie;
                                // 	modalSerie.matelassageEndroit = e.target.value;
                                // 	this.setState({ modalSerie })
                                // }}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>longueur</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerie.longueur}
                                // onChange={(e) => {
                                // 	let modalSerie = this.state.modalSerie;
                                // 	modalSerie.longueur = e.target.value;
                                // 	this.setState({ modalSerie })
                                // }}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>nbrCouche</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerie.nbrCouche}
                                // onChange={(e) => {
                                // 	let modalSerie = this.state.modalSerie;
                                // 	modalSerie.nbrCouche = e.target.value;
                                // 	this.setState({ modalSerie })
                                // }}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>laize</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerie.laize}
                                // onChange={(e) => {
                                // 	let modalSerie = this.state.modalSerie;
                                // 	modalSerie.laize = e.target.value;
                                // 	this.setState({ modalSerie })
                                // }}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Placement</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerie.placement}
                                // onChange={(e) => {
                                // 	let modalSerie = this.state.modalSerie;
                                // 	modalSerie.placement = e.target.value;
                                // 	this.setState({ modalSerie })
                                // }}
                                />
                            </div>
                        </div>


                        <div className='col-6'>
                            <div className='form-group'>
                                <label>Config</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerie.config}
                                // onChange={(e) => {
                                // 	let modalSerie = this.state.modalSerie;
                                // 	modalSerie.config = e.target.value;
                                // 	this.setState({ modalSerie })
                                // }}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>drill</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerie.drill}
                                // onChange={(e) => {
                                // 	let modalSerie = this.state.modalSerie;
                                // 	modalSerie.drill = e.target.value;
                                // 	this.setState({ modalSerie })
                                // }}
                                />
                            </div>
                        </div>

                        <div className='col-6'>
                            <div className='form-group'>
                                <label>machine</label>
                                <input type='text' className='form-control'
                                    value={this.state.modalSerie.machine}
                                // onChange={(e) => {
                                // 	let modalSerie = this.state.modalSerie;
                                // 	modalSerie.machine = e.target.value;
                                // 	this.setState({ modalSerie })
                                // }}
                                />
                            </div>
                        </div>
                    </div>
                </div>
                <hr />
                {arr1.length > 0 && <div>
                    <h2 className='text-center my-2'>Modification de machine {this.state.modalSerie.serie}</h2>
                    <div>
                        <table className='table table table-grey-border'>
                            <thead style={{ backgroundColor: "#aaaa00", color: "white" }}>
                                <tr>
                                    <th>longueur</th>
                                    <th>nbrCouche</th>
                                    <th>laize</th>
                                    <th>Placement</th>
                                    <th>Config</th>
                                    <th>drill</th>
                                    <th>machine</th>
                                    <th style={{ width: 10 }}></th>
                                </tr>
                            </thead>
                            <tbody>
                                {arr1.map(e => {
                                    // let arrDrill = (e.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
                                    // let maxPlie = arrDrill[0] == null && arrDrill[1] == null ? e.maxPlie : e.maxPlieDrill
                                    return <tr>
                                        <td>{this.convertFloat(e.longueurMatelas / e.nbrCouche, 3)}</td>
                                        <td>{e.nbrCouche}
                                            {/* {e.maxPlie && ` / Max Plie : ${e.maxPlie} `}{e.maxPlieDrill && ` / Max Plie Drill : ${e.maxPlieDrill} `} */}
                                        </td>
                                        <td>{e.laize}</td>
                                        <td>{e.placement}</td>
                                        <td>{e.config}</td>
                                        <td>{e.drill}</td>
                                        <td>{e.machine}</td>
                                        <td style={{ padding: "3px" }}>
                                            <button className='btn btn-primary mx-2'
                                                disabled={this.state.changeLoading}
                                                onClick={() => {
                                                    this.setState({ changeLoading: true })
                                                    axios.post(`/api/cuttingRequestSerieInfo/modify`, {
                                                        ...this.state.modalSerie,
                                                        placement: e.placement,
                                                        laize: e.laize,
                                                        // nbrCouche: maxPlie,
                                                        drill: e.drill,
                                                        longueur: this.convertFloat(e.longueurMatelas / e.nbrCouche, 3),
                                                        config: e.config,
                                                        machine: e.machine
                                                    })
                                                        .then(res => {
                                                            axios.post("/api/cuttingRequestSerieRouleauHistory", {
                                                                content: "Modification PROD serie " + this.state.modalSerie.serie + " :"
                                                                    + " longueur " + e.longueur
                                                                    + " placement " + e.placement
                                                                    + " Config " + e.config
                                                                    + " machine " + e.machine
                                                                    + " drill " + e.drill
                                                                    + " nbrCouche " + e.maxPlie
                                                                    + " laize " + e.laize
                                                                ,
                                                                serie: this.state.modalSerie.serie,
                                                                changedAt: moment(),
                                                                changedBy: user.lastName + " " + user.firstName
                                                            })
                                                                .then(res => {
                                                                    this.setState({ modalSerie: null, changeLoading: false })
                                                                    this.loadSequence(this.state.modalObj.sequence)
                                                                })
                                                                .catch(() => {
                                                                    this.setState({ changeLoading: false })
                                                                })

                                                        })
                                                        .finally(() => {
                                                            this.setState({ modalSerie: null, changeLoading: false })
                                                        })
                                                }}>
                                                Modifier
                                            </button>
                                        </td>
                                    </tr>
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>}
                {arr2.length > 0 && <div>
                    <h2 className='text-center my-2'>Modification de laize et autre {this.state.modalSerie.serie}</h2>
                    <div>
                        <table className='table table table-grey-border'>
                            <thead style={{ backgroundColor: "#ff0000", color: "white" }}>
                                <tr>
                                    <th>longueur</th>
                                    <th>nbrCouche</th>
                                    <th>laize</th>
                                    <th>Placement</th>
                                    <th>Config</th>
                                    <th>drill</th>
                                    <th>machine</th>
                                    <th style={{ width: 10 }}></th>
                                </tr>
                            </thead>
                            <tbody>
                                {arr2.map(e => {
                                    return <tr>
                                        <td>{this.convertFloat(e.longueurMatelas / e.nbrCouche, 3)}</td>
                                        <td>{e.nbrCouche}{e.maxPlie && ` / Max Plie : ${e.maxPlie} `}{e.maxPlieDrill && ` / Max Plie Drill : ${e.maxPlieDrill} `}</td>
                                        <td>{e.laize}</td>
                                        <td>{e.placement}</td>
                                        <td>{e.config}</td>
                                        <td>{e.drill}</td>
                                        <td>{e.machine}</td>
                                        <td style={{ padding: "3px" }}>
                                            <button className='btn btn-primary mx-2'
                                                disabled={this.state.changeLoading}
                                                onClick={() => {
                                                    this.setState({ changeLoading: true })
                                                    axios.post(`/api/cuttingRequestSerieInfo/modify`, {
                                                        ...this.state.modalSerie,
                                                        placement: e.placement,
                                                        laize: e.laize,
                                                        // nbrCouche: e.nbrCouche,
                                                        drill: e.drill,
                                                        longueur: this.convertFloat(e.longueurMatelas / e.nbrCouche, 3),
                                                        config: e.config,
                                                        machine: e.machine
                                                    })
                                                        .then(res => {
                                                            axios.post("/api/cuttingRequestSerieRouleauHistory", {
                                                                content: "Modification PROD serie " + this.state.modalSerie.serie + " :"
                                                                    + " longueur " + e.longueur
                                                                    + " placement " + e.placement
                                                                    + " Config " + e.config
                                                                    + " machine " + e.machine
                                                                    + " drill " + e.drill
                                                                    + " nbrCouche " + e.nbrCouche
                                                                    + " laize " + e.laize
                                                                ,
                                                                serie: this.state.modalSerie.serie,
                                                                changedAt: moment(),
                                                                changedBy: user.lastName + " " + user.firstName
                                                            })
                                                                .then(res => {
                                                                    this.setState({ modalSerie: null, changeLoading: false })
                                                                    this.loadSequence(this.state.modalObj.sequence)
                                                                })
                                                                .catch(() => {
                                                                    this.setState({ changeLoading: false })
                                                                })

                                                        })
                                                        .finally(() => {
                                                            this.setState({ modalSerie: null, changeLoading: false })
                                                        })
                                                }}>
                                                Modifier
                                            </button>
                                        </td>
                                    </tr>
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>}
            </div>}
        </Modal>
    }

    showSerieModalForm = async (elemPn) => {
        this.setState({ modalSerieForm: null, dataIMS: null })
        try {
            let resIMS = await axios.get(`/api/query/refDetails?reftissu=${elemPn.partNumberMaterial}`)
            this.setState({ dataIMS: resIMS.data })
        } catch (err) {
            this.setState({ dataIMS: null })
        }

        if (this.state.partNumberMaterialConfigs[elemPn.partNumberMaterial]) {
            this.setState({
                modalSerieForm: { serie: elemPn.serie, partNumberMaterial: elemPn.partNumberMaterial },
            })
        } else {
            let resConfig = await axios.get(`/api/partNumberMaterialConfig/pns/${elemPn.partNumberMaterial}`)
            this.setState({
                modalSerieForm: { serie: elemPn.serie, partNumberMaterial: elemPn.partNumberMaterial, placement: elemPn.placement },
                partNumberMaterialConfigs: { ...this.state.partNumberMaterialConfigs, [elemPn.partNumberMaterial]: resConfig.data.length > 0 ? resConfig.data[0] : null },
            })
        }
    }

    renderForm = (entityId) => {
        const { user } = this.props.security;
        let cuttingRequest = this.state.modalObj
        return <div className="demande-coupe-container fade-in">
            <div className="sequence-header">
                <div className='d-flex'>
                    <div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Sequence : </strong></div>
                    <div className='text-no-wrap ml-2' style={{ width: "35%" }}>{cuttingRequest.sequence}</div>
                </div>
                <div className='d-flex'>
                    <div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Projet : </strong></div>
                    <div className='text-no-wrap ml-2' style={{ width: "35%" }}>
                        {cuttingRequest.projet}
                    </div>
                    <div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Version : </strong></div>
                    <div className='' style={{ width: "35%" }}>{cuttingRequest.version}</div>
                </div>
                <div className='d-flex'>
                    <div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>modele : </strong></div>
                    <div className='text-no-wrap ml-2' style={{ width: "35%" }}>{cuttingRequest.modele}</div>
                </div>
                <div className='d-flex'>
                    <div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>definition : </strong></div>
                    <div className='text-no-wrap ml-2' style={{ width: "35%" }}>{cuttingRequest.definition}</div>
                </div>
                <div className='d-flex'>
                    <div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Zone : </strong></div>
                    <div className='text-no-wrap ml-2' style={{ width: "35%" }}>
                        {cuttingRequest.zone ? cuttingRequest.zone.nom : ""}
                    </div>
                </div>
            </div>
            <div className="enhanced-table">
                <table className='table table table-grey-border'>
                    <thead style={{ backgroundColor: "#ca0c0c", color: "white" }}>
                        <tr>
                            <th className='table-elem-sm'>Part Number</th>
                            <th className='table-elem-sm'>Description</th>
                            <th className='table-elem-sm'>Kit textil</th>
                            <th className='table-elem-sm'>Quantité</th>
                            <th className='table-elem-sm'>wo</th>
                            <th className='table-elem-sm'>woid</th>
                            <th className='table-elem-sm'>packageQty</th>

                        </tr>
                    </thead>
                    <tbody>
                        {cuttingRequest.cuttingRequestPartNumbers && cuttingRequest.cuttingRequestPartNumbers.map(elemPn => <tr
                            className='clickable-element'
                            onClick={() => this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber)
                                ? this.setState({ selectedBoxs: this.state.selectedBoxs.filter(e => e.partNumber != elemPn.partNumber) })
                                : this.setState({ selectedBoxs: [...this.state.selectedBoxs, ...cuttingRequest.cuttingRequestBoxs.filter(e => e.partNumber == elemPn.partNumber)].sort((a, b) => a.id.localeCompare(b.id)) })
                            }
                            style={this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber) ? { backgroundColor: "rgb(207, 207, 207)" } : {}}
                        >
                            <td className='table-elem-sm'>{elemPn.partNumber}</td>
                            <td className='table-elem-sm'>{elemPn.description}</td>
                            <td className='table-elem-sm'>{elemPn.item}</td>
                            <td className='table-elem-sm'>{elemPn.quantity}</td>
                            <td className='table-elem-sm'>{elemPn.wo}</td>
                            <td className='table-elem-sm'>{elemPn.woid}</td>
                            <td className='table-elem-sm'><strong>{elemPn.packageQty}</strong></td>

                        </tr>)}
                    </tbody>
                </table>
            </div>
            <div className="enhanced-table">
                <table className='table table table-grey-border'>
                    <thead style={{ backgroundColor: "#ca0c0c", color: "white" }}>
                        <tr>
                            <th className='table-elem-sm'>serie</th>
                            <th className='table-elem-sm'>placement</th>
                            <th className='table-elem-sm'>partNumberMaterial</th>
                            <th className='table-elem-sm'>Description</th>
                            {/* <th className='table-elem-sm'>matelassageEndroit</th>
                            <th className='table-elem-sm'>longueur</th>
                            <th className='table-elem-sm'>nbrCouche</th>
                            <th className='table-elem-sm'>laize</th>
                           
                            <th className='table-elem-sm'>config</th>
                            <th className='table-elem-sm'>drill1</th>
                            <th className='table-elem-sm'>drill2</th>
                            <th className='table-elem-sm'>machine</th> */}
                            <th className='table-elem-sm'>Table</th>
                            <th className='table-elem-sm'>Debut Matelassage</th>
                            <th className='table-elem-sm'>Fin Matelassage</th>
                            <th className='table-elem-sm'>Temps Matelassage</th>
                            <th className='table-elem-sm'>Status Matelassage</th>
                            <th className='table-elem-sm'>Machine</th>
                            <th className='table-elem-sm'>Debut Coupe</th>
                            <th className='table-elem-sm'>Fin Coupe</th>
                            <th className='table-elem-sm'>Temps Coupe</th>
                            <th className='table-elem-sm'>Status Coupe</th>
                            <th className='table-elem-sm'>
                                {/* {cuttingRequest.cuttingRequestSeries && cuttingRequest.cuttingRequestSeries.filter(e => e.serie != null).length > 0 && <button className='btn btn-outline-light'
                                    onClick={() => {
                                        axios.post(`/api/cuttingRequest/printSerie`,
                                            [...cuttingRequest.cuttingRequestSeries.map(elemCRS => { return { ...elemCRS, cuttingRequest: cuttingRequest } })]
                                        )
                                    }}
                                    style={{
                                        padding: "2 5",
                                        fontSize: 12,
                                        marginLeft: 5
                                    }}
                                >
                                    <FontAwesomeIcon icon={faPrint} />
                                </button>} */}

                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        {cuttingRequest.cuttingRequestSeries && cuttingRequest.cuttingRequestSeries
                            .sort((a, b) => a.partNumberMaterial.localeCompare(b.partNumberMaterial) || a.groupPlacement - b.groupPlacement || Number(b.activated) - Number(a.activated))
                            .map((elemPn, indPn) => {
                                let arrDrill = (elemPn.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
                                // use moment.js to create the diff in minite between two dates
                                let diff = (elemPn.dateDebutMatelassage && elemPn.dateFinMatelassage) ? moment(elemPn.dateFinMatelassage, "yyyy-MM-dd HH:mm:ss").diff(moment(elemPn.dateDebutMatelassage, "yyyy-MM-dd HH:mm:ss"), 'minutes') : null
                                let diff2 = (elemPn.dateDebutCoupe && elemPn.dateFinCoupe) ? moment(elemPn.dateFinCoupe, "yyyy-MM-dd HH:mm:ss").diff(moment(elemPn.dateDebutCoupe, "yyyy-MM-dd HH:mm:ss"), 'minutes') : null

                                let qn = null
                                let arrQn = this.state.qnList.filter(e => e.reftissu.toUpperCase() === elemPn.partNumberMaterial.toUpperCase() && e.resultat === "Non ok" && e.placement && (e.placement.toUpperCase().trim() === "ALL" || e.placement.split(";").map(e => e.trim().toUpperCase()).includes(elemPn.placement.trim().toUpperCase())))
                                if (arrQn.length > 0) {
                                    qn = arrQn[0]
                                } else {
                                    arrQn = this.state.qnList.filter(e => e.reftissu.toUpperCase() === elemPn.partNumberMaterial.toUpperCase() && e.resultat === "Formation" && e.placement && (e.placement.toUpperCase().trim() === "ALL" || e.placement.split(";").map(e => e.trim().toUpperCase()).includes(elemPn.placement.trim().toUpperCase())))
                                    if (arrQn.length > 0) {
                                        qn = arrQn[0]
                                    }
                                }

                                return <tr style={elemPn.activated === false ? { backgroundColor: "#ffffc0" } : { backgroundColor: "" }} className="clickable-element"
                                    onDoubleClick={() => {
                                        this.setState({ showSerieForm: elemPn.serie })
                                    }}
                                >
                                    <td className='table-elem-sm enhanced-buttons'>{elemPn.serie}
                                        <div className='d-flex flex-wrap mt-1'>
                                            {
                                                elemPn.statusMatelassage === "Waiting" &&
                                                this.state.optionnals[elemPn.placement.toUpperCase().trim()] &&
                                                this.state.optionnals[elemPn.placement.toUpperCase().trim()]
                                                    .filter(e => e.placement != elemPn.placement && e.partNumberMaterial === elemPn.partNumberMaterial).length >= 1 &&
                                                <button className='btn btn-sm btn-outline-primary mr-1'
                                                    style={{
                                                        fontSize: '10px',
                                                        padding: '2px 8px',
                                                        borderRadius: '12px',
                                                        fontWeight: '500'
                                                    }}
                                                    onClick={() => { this.setState({ modalSerie: { ...elemPn } }) }}
                                                >
                                                    <FontAwesomeIcon icon={faCog} className='mr-1' />
                                                    Options
                                                </button>
                                            }
                                            {user && user.roles && (user.roles.some(role => ["ROLE_ADMIN", "ROLE_QUALITE"].includes(role.authority))) &&
                                                elemPn.statusMatelassage === "Waiting" &&
                                                this.state.optionnalsQuality[elemPn.placement.toUpperCase().trim()] &&
                                                this.state.optionnalsQuality[elemPn.placement.toUpperCase().trim()]
                                                    .filter(e => e.placement != elemPn.placement && e.partNumberMaterial === elemPn.partNumberMaterial).length >= 1 &&
                                                <button className='btn btn-sm btn-outline-warning'
                                                    style={{
                                                        fontSize: '10px',
                                                        padding: '2px 8px',
                                                        borderRadius: '12px',
                                                        fontWeight: '500',
                                                        color: '#856404',
                                                        borderColor: '#856404'
                                                    }}
                                                    onClick={() => { this.setState({ modalSerieQuality: { ...elemPn } }) }}
                                                >
                                                    <FontAwesomeIcon icon={faStar} className='mr-1' /> Qualité
                                                </button>
                                            }
                                        </div>
                                    </td>
                                    <td className={(qn && qn.placement && qn.placement.split(";").map(e => e.trim().toUpperCase()).includes(elemPn.placement.trim().toUpperCase()) && qn.resultat === "Non ok") ? "table-elem-sm quality-non-ok"
                                        : (qn && qn.placement && qn.placement.split(";").map(e => e.trim().toUpperCase()).includes(elemPn.placement.trim().toUpperCase()) && qn.resultat === "Formation") ? "table-elem-sm quality-formation"
                                            : "table-elem-sm"}
                                    >{elemPn.placement}
                                        <button className='btn btn-outline-primary'
                                            style={{
                                                fontSize: 12,
                                                padding: 2,
                                                marginLeft: 3,
                                                width: 28,
                                            }}
                                            onClick={() => {
                                                // open a new page /cutfileviewer/1DEM365/view
                                                window.open("/cutfileviewer/" + elemPn.placement + "/view", "_blank")
                                            }}
                                        >
                                            <FontAwesomeIcon icon={faEye} />
                                        </button>
                                    </td>
                                    <td className={(qn && qn.resultat === "Non ok") ? "table-elem-sm quality-non-ok"
                                        : (qn && qn.resultat === "Formation") ? "table-elem-sm quality-formation"
                                            : "table-elem-sm"}
                                    >{elemPn.partNumberMaterial}</td>
                                    <td className='table-elem-sm'>{elemPn.description}</td>
                                    {/* <td className='table-elem-sm'>{elemPn.matelassageEndroit}</td>
                                    <td className='table-elem-sm'>{elemPn.longueur && elemPn.longueur.toFixed(3)}</td>
                                    <td className='table-elem-sm'>{elemPn.nbrCouche}</td>
                                    <td className='table-elem-sm'>{elemPn.laize}</td>
                                    <td className='table-elem-sm'>{elemPn.placement}</td>
                                    <td className='table-elem-sm'>{elemPn.config}</td>
                                    <td className='table-elem-sm'>{arrDrill[0]}</td>
                                    <td className='table-elem-sm'>{arrDrill[1]}</td>
                                    <td className='table-elem-sm'>{elemPn.machine}</td> */}

                                    <td className='table-elem-sm'>{elemPn.tableMatelassage}</td>
                                    <td className='table-elem-sm'>{elemPn.dateDebutMatelassage}</td>
                                    <td className='table-elem-sm'>{elemPn.dateFinMatelassage}</td>
                                    <td className='table-elem-sm'>{diff}</td>
                                    <td className={`table-elem-sm ${this.getStatusClass(elemPn.statusMatelassage)}`}>{elemPn.statusMatelassage}</td>

                                    <td className='table-elem-sm'>{elemPn.tableCoupe}</td>
                                    <td className='table-elem-sm'>{elemPn.dateDebutCoupe}</td>
                                    <td className='table-elem-sm'>{elemPn.dateFinCoupe}</td>
                                    <td className='table-elem-sm'>{diff2}</td>
                                    <td className={`table-elem-sm ${this.getStatusClass(elemPn.statusCoupe)}`}>{elemPn.statusCoupe}</td>

                                    <td className='table-elem-sm'>
                                        {/* {elemPn.serie && <button className='btn btn-outline-dark'
                                            onClick={() => {
                                                axios.post(`/api/cuttingRequest/printSerie`, [{ ...elemPn, cuttingRequest: cuttingRequest }])
                                            }}
                                            style={{
                                                padding: "2 5",
                                                fontSize: 12,
                                                marginLeft: 5
                                            }}
                                        >
                                            <FontAwesomeIcon icon={faPrint} />
                                        </button>}
                                        {!elemPn.activated && elemPn.serie == null && <button className='btn btn-sm btn-outline-dark' onClick={() => {
                                            let series = [...cuttingRequest.cuttingRequestSeries]
                                            let objInd = series.findIndex(e => (e.partNumberMaterial == elemPn.partNumberMaterial && e.groupPlacement == elemPn.groupPlacement && e.activated === true))
                                            if (objInd >= 0) {
                                                series[objInd].activated = false
                                            }
                                            series[indPn].activated = true
                                            cuttingRequest.cuttingRequestSeries = [...series]
                                            this.setState({ cuttingRequest: { ...cuttingRequest } })
                                        }}>
                                            <FontAwesomeIcon icon={faArrowUp} />
                                        </button>} */}
                                        <button className='btn btn-outline-dark'
                                            onClick={() => { this.showSerieModalForm(elemPn) }}
                                            style={{
                                                padding: "2 5",
                                                fontSize: 12,
                                                marginLeft: 5
                                            }}
                                        >
                                            <FontAwesomeIcon icon={faEnvelope} />
                                        </button>
                                    </td>
                                </tr>
                            })}
                    </tbody>
                </table>
            </div>
            <div className="enhanced-table">
                <table className='table table table-grey-border'>
                    <thead style={{ backgroundColor: "#ca0c0c", color: "white" }}>
                        <tr>
                            <th className='table-elem-sm'>ID</th>
                            <th className='table-elem-sm'>Part Number</th>
                            <th className='table-elem-sm'>Description</th>
                            <th className='table-elem-sm'>Kit textil</th>
                            <th className='table-elem-sm'>Quantité</th>
                            <th className='table-elem-sm'>wo</th>
                            <th className='table-elem-sm'>woid</th>
                        </tr>
                    </thead>
                    <tbody>
                        {cuttingRequest.cuttingRequestBoxs && cuttingRequest.cuttingRequestBoxs.map(elemBox => <tr
                            className='clickable-element'
                            onClick={() => this.state.selectedBoxs.map(e => e.id).includes(elemBox.id)
                                ? this.setState({ selectedBoxs: this.state.selectedBoxs.filter(e => e.id != elemBox.id) })
                                : this.setState({ selectedBoxs: [...this.state.selectedBoxs, elemBox].sort((a, b) => a.id.localeCompare(b.id)) })
                            }
                            style={this.state.selectedBoxs.map(e => e.id).includes(elemBox.id) ? { backgroundColor: "rgb(207, 207, 207)" } : {}}
                        >
                            <td className='table-elem-sm'>{elemBox.id}</td>
                            <td className='table-elem-sm'>{elemBox.partNumber}</td>
                            <td className='table-elem-sm'>{elemBox.description}</td>
                            <td className='table-elem-sm'>{elemBox.item}</td>
                            <td className='table-elem-sm'>{elemBox.qtyBox}</td>
                            <td className='table-elem-sm'>{elemBox.wo}</td>
                            <td className='table-elem-sm'>{elemBox.woid}</td>
                        </tr>)}
                    </tbody>
                </table>
            </div>
            <div style={{ overflow: "hidden", height: 0 }}>
                <div className='' ref={elem => this.componentRef = elem}>
                    {this.state.selectedBoxs.length > 0 && this.state.selectedBoxs.map(box => {
                        let wd = 1565, hg = 1100
                        return [<div style={{ height: wd }} key={"gamme-" + box.wo}><GammePn box={box} /></div>, <div className="page-break" />]
                    })}
                </div>
            </div>
            <div style={{ overflow: "hidden", height: 0 }}>
                <div className='' ref={elem => this.planPrintPage = elem} style={{ padding: 15 }}>
                    <div className="row"
                        style={{
                            margin: "0",
                            // width: "31.5cm", 
                            marginBottom: "5px"
                        }}
                    >
                        <div className="col-3 border border-dark" style={{ paddingLeft: "30px", paddingTop: "6px" }}>
                            <img
                                src={logo}
                                alt="lear logo"
                                height="40"
                            />
                        </div>
                        <div className="col-6 border border-dark">
                            <h3 className="text-center mt-2">PLAN DE COUPE / MATELASSAGE</h3>
                        </div>
                        <div className="col-3 border border-dark">
                            <p className="text-center mt-2">FR PE 47</p>
                        </div>
                    </div>
                    <div>
                        <div className='d-flex'>
                            <div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Sequence : </strong></div>
                            <div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.sequence}</div>
                        </div>
                        <div className='d-flex'>
                            <div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Projet : </strong></div>
                            <div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.projet}</div>
                            <div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>Version : </strong></div>
                            <div className='' style={{ width: "35%" }}>{cuttingRequest.version}</div>
                        </div>
                        <div className='d-flex'>
                            <div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>modele : </strong></div>
                            <div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.modele}</div>
                        </div>
                        <div className='d-flex'>
                            <div className='text-no-wrap text-right' style={{ width: "15%" }}><strong>definition : </strong></div>
                            <div className='text-no-wrap' style={{ width: "35%" }}>{cuttingRequest.definition}</div>
                        </div>
                    </div>
                    <div className='mb-2'>
                        <table className='table m-0 table table-grey-border'>
                            <thead>
                                <tr>
                                    <th className='table-elem-sm'>Part Number</th>
                                    <th className='table-elem-sm'>Description</th>
                                    <th className='table-elem-sm'>Kit textil</th>
                                    <th className='table-elem-sm'>Quantité</th>
                                    <th className='table-elem-sm'>wo</th>
                                    <th className='table-elem-sm'>woid</th>
                                    <th className='table-elem-sm'>packageQty</th>

                                </tr>
                            </thead>
                            <tbody>
                                {cuttingRequest.cuttingRequestPartNumbers && cuttingRequest.cuttingRequestPartNumbers.map(elemPn => <tr
                                    className='clickable-element'
                                    onClick={() => this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber)
                                        ? this.setState({ selectedBoxs: this.state.selectedBoxs.filter(e => e.partNumber != elemPn.partNumber) })
                                        : this.setState({ selectedBoxs: [...this.state.selectedBoxs, ...cuttingRequest.cuttingRequestBoxs.filter(e => e.partNumber == elemPn.partNumber)].sort((a, b) => a.id.localeCompare(b.id)) })
                                    }
                                    style={this.state.selectedBoxs.map(e => e.partNumber).includes(elemPn.partNumber) ? { backgroundColor: "rgb(207, 207, 207)" } : {}}
                                >
                                    <td className='table-elem-sm'>{elemPn.partNumber}</td>
                                    <td className='table-elem-sm'>{elemPn.description}</td>
                                    <td className='table-elem-sm'>{elemPn.item}</td>
                                    <td className='table-elem-sm'>{elemPn.quantity}</td>
                                    <td className='table-elem-sm'>{elemPn.wo}</td>
                                    <td className='table-elem-sm'>{elemPn.woid}</td>
                                    <td className='table-elem-sm'><strong>{elemPn.packageQty}</strong></td>

                                </tr>)}
                            </tbody>
                        </table>
                    </div>
                    <div>
                        <table className='table m-0 table table-grey-border'>
                            <thead>
                                <tr >
                                    <td className='table-elem-sm' colSpan={7} >Matelassage</td>
                                    <td className='table-elem-sm ml-1' colSpan={5}>Coupe</td>
                                </tr>
                                <tr>
                                    <th className='table-elem-sm'>serie</th>
                                    <th className='table-elem-sm'>Part Number Material</th>
                                    <th className='table-elem-sm'>Description</th>
                                    <th className='table-elem-sm'>Sens</th>
                                    <th className='table-elem-sm'>longueur</th>
                                    <th className='table-elem-sm'>nbrCouche</th>
                                    <th className='table-elem-sm'>laize</th>
                                    <th className='table-elem-sm ml-1'>placement</th>
                                    <th className='table-elem-sm'>config</th>
                                    <th className='table-elem-sm'>drill1</th>
                                    <th className='table-elem-sm'>drill2</th>
                                    <th className='table-elem-sm'>machine</th>
                                </tr>
                            </thead>
                            <tbody>
                                {cuttingRequest.cuttingRequestSeries && cuttingRequest.cuttingRequestSeries.map(elemPn => {
                                    let arrDrill = (elemPn.drill || ",").split(",").map(e => e != "" ? parseInt(e) : null)
                                    return <tr style={elemPn.activated === false ? { backgroundColor: "#ffffc0" } : { backgroundColor: "" }}>
                                        <td className='table-elem-sm'>{elemPn.serie}</td>
                                        <td className='table-elem-sm'>{elemPn.partNumberMaterial}</td>
                                        <td className='table-elem-sm'>{elemPn.description}</td>
                                        <td className='table-elem-sm'>{elemPn.matelassageEndroit}</td>
                                        <td className='table-elem-sm'>{elemPn.longueur && elemPn.longueur.toFixed(3)}</td>
                                        <td className='table-elem-sm'>{elemPn.nbrCouche}</td>
                                        <td className='table-elem-sm'>{elemPn.laize}</td>
                                        <td className='table-elem-sm ml-1'>{elemPn.placement}</td>
                                        <td className='table-elem-sm'>{elemPn.config}</td>
                                        <td className='table-elem-sm'>{arrDrill[0]}</td>
                                        <td className='table-elem-sm'>{arrDrill[1]}</td>
                                        <td className='table-elem-sm'>{elemPn.machine}</td>
                                    </tr>
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    }

    checkObj = () => {
        axios.post(`/api/cuttingRequest`, this.state.modalObj)
            .then(res => {
                this.setState({ modalObj: { ...res.data } })
            })
    }

    render() {
        // let { entityId } = this.props.match.params
        let { entityId } = this.props
        return (
            <div>
                <div className='entityform-header'>
                    <h1 className='entityform-title'>Statut sequence {this.state.modalObj.sequence}</h1>
                    <div className='entityform-buttons'>
                        <ReactToPrint
                            onBeforeGetContent={() => {
                                return new Promise((resolve, reject) => {
                                    this.setState({ modalRotate: true, selectedEmp: null }, () => resolve())
                                });
                            }}
                            onAfterPrint={() => { this.setState({ modalRotate: false }) }}
                            onPrintError={() => { this.setState({ modalRotate: false }) }}
                            trigger={() => <button
                                type="button"
                                className="btn btn-outline-success ml-1"

                            ><FontAwesomeIcon icon={faPrint} /> Gammes</button>}
                            content={() => this.componentRef}
                        />
                        <ReactToPrint
                            onBeforeGetContent={() => {
                                return new Promise((resolve, reject) => {
                                    this.setState({}, () => resolve())
                                });
                            }}
                            onAfterPrint={() => { this.setState({ modalRotate: false }) }}
                            onPrintError={() => { this.setState({ modalRotate: false }) }}
                            trigger={() => <button
                                type="button"
                                className="btn btn-outline-success ml-1"

                            ><FontAwesomeIcon icon={faPrint} /> Plan</button>}
                            content={() => this.planPrintPage}
                        />

                        <button
                            className='btn btn-sm btn-outline-primary ml-2' disabled={this.state.loadingRefresh}
                            onClick={() => {
                                const { entityId } = this.props
                                this.setState({ loadingRefresh: true })
                                axios.post(`/api/cuttingRequestV2/refresh/${entityId}`).then(res => {
                                    this.setState({ modalObj: res.data, loadingRefresh: false })
                                })
                                    .catch(err => {
                                        this.setState({ loadingRefresh: false })
                                    })
                            }}
                        >
                            {this.state.loadingRefresh
                                ? <span><FontAwesomeIcon icon={faSpinner} spin /> Loading ...</span>
                                : <span><FontAwesomeIcon icon={faSync} /> Refresh from CMS</span>}
                        </button>

                        <button className='btn btn-link' onClick={() => { this.props.goBack() }}>Annuler</button>
                    </div>
                </div>
                <div className='entityform-container'>
                    {this.renderForm(entityId)}
                </div>
                {this.serieModal()}
                {this.serieModifModal()}
                {this.serieFormModal()}
                {this.serieFormQualityModal()}
            </div>
        )
    }
}
DemandeDeCoupeForm.propTypes = {
    security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
    security: state.security
})


export default connect(mapStateToProps, {})(DemandeDeCoupeForm);