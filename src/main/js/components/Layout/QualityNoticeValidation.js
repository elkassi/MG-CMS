import axios from 'axios';
import React, { Component } from 'react'
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import Select from "react-select";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEye, faS, faSpinner, faTimes } from '@fortawesome/free-solid-svg-icons';
import GammePnForQN from './GammePnForQN';
import { Modal } from 'react-bootstrap'
import moment from 'moment';
import Switch from "react-switch";

class QualityNoticeValidation extends Component {

    constructor(props) {
        super(props);
        this.state = {
            modalObj: {},
            user: null,
            listCtcFiles: [],
            codeDefauts: [],
            showModalGamme: false,
            box: null,
            message: null,
            error: null,
            loading: false,
            showDetail: false,
            rouleauDetails: [],
            showCustomSecurisation: false,
        }
    }

    componentDidMount() {
        if (this.props.security) {
            axios.get(`/api/user/${this.props.security.user.matricule}`)
                .then(res => {
                    this.setState({
                        user: { ...res.data },
                    })
                })
                .catch(err => {
                    if (err.response.data != null && err.response.data.username === "Invalid Username") {
                        window.location.pathname = "/login";
                    }
                })
        }
        const urlParams = new URLSearchParams(window.location.search);
        const numeroQn = urlParams.get('numeroQn');

        if (numeroQn) {
            this.setState({ showDetail: null })
            axios.get(`/api/qualityNotice/${numeroQn}`)
                .then(res => {
                    let obj = res.data
                    if (obj.correctDefaut == null) {
                        obj.correctDefaut = obj.codeDefaut
                    }
                    this.setState({ modalObj: { ...obj }, showDetail: true })
                    this.loadFromWO()
                    // this.loadFournisseur()
                    if (obj.site === "TRIM1" && obj.typeDefaut === "Défaut fournisseur") {
                        if (obj.wo && obj.wo.length > 0) {
                            this.loadRouleauDetails()
                        }
                        this.loadFournisseur()
                    }

                })
                .catch(err => {
                    console.log(err)
                })
        }
        axios.get("/api/codeDefaut/listC")
            .then(res => {
                this.setState({ codeDefauts: res.data })
            })
            .catch(err => {
                console.log(err)
            })
    }

    handleChanges = (obj) => {
        this.setState({ modalObj: { ...this.state.modalObj, ...obj }, error: null, message: null })
    }

    loadRouleauDetails = () => {
        this.setState({ rouleauDetails: null })
        axios.get("/api/cuttingRequestSerieData/getSeries/" + this.state.modalObj.sequence + "/" + this.state.modalObj.reftissu)
            .then(res => {
                let arrSerie = res.data
                axios.get("/api/cuttingRequestSerieRouleauData/bySeries/" + arrSerie.join(","))
                    .then(res2 => {
                        if (res2.data.length > 0 && this.state.modalObj.idRouleau === null) {
                            this.setState({
                                rouleauDetails: res2.data,
                                modalObj: {
                                    ...this.state.modalObj,
                                    idRouleau: res2.data[0].idRouleau,
                                    lotFrs: res2.data[0].lotFrs,
                                    dateCoupe: res2.data[0].createdAt
                                }
                            })
                        } else {
                            this.setState({ rouleauDetails: res2.data })
                        }
                    })
                    .catch(err => {
                        this.setState({ rouleauDetails: [] })
                        console.log(err)
                    })
            })
            .catch(err => {
                this.setState({ rouleauDetails: [] })
                console.log(err)
            })
    }

    loadFournisseur = () => {
        axios.get("/api/query/refDetails?reftissu=" + this.state.modalObj.reftissu)
            .then(res => {
                this.handleChanges({ nomFournisseur: res.data.fournisseur, reftissuDescription: res.data.description })
            })
            .catch(err => {
                console.log(err)
            })
    }

    renderExtraEmailsReponse = () => {
        let arr = this.state.modalObj.extraEmailsReponse ? this.state.modalObj.extraEmailsReponse.split(";") : [""]
        return <div className='row py-2'>
            <label className='col-4 col-form-label text-right'>Extra Emails :</label>
            <div className='col-4 p-0'>
                {arr.map((email, index) => {
                    return <div className='d-flex mb-1'><input type='text' className='form-control form-control-sm mr-1' ref={ref => this.woRef = ref} key={index}
                        value={email}
                        style={{ width: '300px' }}
                        onChange={(e) => {
                            this.handleChanges({ extraEmailsReponse: arr.map((elem, i) => i === index ? e.target.value.trim().toLowerCase() : elem).join(";") })
                        }}
                    />
                        <button className='btn btn-sm btn-danger' onClick={() => {
                            arr.splice(index, 1)
                            this.handleChanges({ extraEmailsReponse: arr.join(";") })
                        }}>-</button>
                    </div>
                })}
                <button className='btn btn-sm btn-primary' onClick={() => {
                    arr.push("")
                    this.handleChanges({ extraEmailsReponse: arr.join(";") })
                }}>+</button>
            </div>
        </div>
    }


    renderModalGamme = (modelItemsId) => {
        return <Modal
            show={this.state.showModalGamme}
            onHide={() => {
                this.setState({ showModalGamme: false })
                if (this.state.modalObj.reftissu) {
                    this.loadFournisseur()
                }
            }}
            dialogClassName="modal-90w"
            centered
        >
            {this.state.showModalGamme && <div style={{ maxHeight: "80vh", overflowY: 'auto' }}>
                <div style={{ position: "sticky", top: 0, left: 0, backgroundColor: "lightgray", padding: "5 15", fontSize: 20, zIndex: 2 }}>
                    {modelItemsId.length} modèles sélectionnés: {modelItemsId.map(e => e[0]).join(" / ")}
                </div>
                {this.state.modalObj.partnumber && <GammePnForQN
                    box={this.state.box}
                    selectedIds={modelItemsId.map(e => e[0]) || []}
                // updateSelectedIds={(selectedIds) => {
                // 	let reftissu = null, description = null

                // 	if (selectedIds.length > 0) {
                // 		let objFirst = this.state.listCtcFiles.find(e => e.panelNumber === selectedIds[0])
                // 		reftissu = objFirst ? objFirst.partNumberMaterial : null
                // 		description = objFirst ? objFirst.partNumberMaterialDescription : null
                // 	}

                // 	if (selectedIds.length > 0 && this.state.listCtcFiles.length > 0) {
                // 		this.state.listCtcFiles
                // 			.filter(e => selectedIds.includes(e.panelNumber))
                // 			.map(option => {
                // 				if (reftissu != null && reftissu != option.partNumberMaterial) {
                // 					selectedIds = selectedIds.filter(e => e !== option.panelNumber)
                // 				} else {
                // 					reftissu = option.partNumberMaterial
                // 				}
                // 			})
                // 	}
                // 	this.handleChanges({ numEmp: selectedIds.map(e => e + " : 1").join(" / "), reftissu: reftissu, quantite: selectedIds.length, reftissuDescription: description })
                // 	return selectedIds
                // }}
                />}
            </div>}
        </Modal>
    }

    renderConfirme = () => {
        return (this.state.message && Object.keys(this.state.message).length !== 0)
            && !(Object.keys(this.state.message).length == 1) && <div className="alert alert-success alert-error text-center m-4" role="alert">
                <ul>
                    <button type='btn' className="btn btn-toclose" onClick={() => { this.setState({ message: null }) }}>
                        <FontAwesomeIcon icon={faTimes} size="2x" />
                    </button>
                    {this.renderErrorsAlert(this.state.message)}
                </ul>
            </div>
    }


    handleSave = () => {
        let error = {}
        if (!this.state.modalObj.numeroQn || this.state.modalObj.numeroQn.length === 0) {
            error.numeroQn = "Numero QN est obligatoire"
        }
        if (!this.state.modalObj.correctDefaut && this.state.modalObj.reponse !== "Refusé") {
            error.correctDefaut = "Correct Défaut est obligatoire"
        }

        if (this.state.modalObj.reponse !== "Refusé" && (!this.state.modalObj.qteRecu || this.state.modalObj.qteRecu == 0) 
            && (!this.state.modalObj.qteRecuCoiffe || this.state.modalObj.qteRecuCoiffe == 0) 
        && (!this.state.modalObj.qteRecuMetrage || this.state.modalObj.qteRecuMetrage == 0)) {
            error.qteRecu = "Quantité (reçu ou coiffe ou métrage) est obligatoire"
        }
        if (!this.state.modalObj.reponse) {
            error.reponse = "Réponse est obligatoire"
        }
        if (!this.state.modalObj.decision && this.state.modalObj.reponse === "Accepté") {
            error.decision = "Décision est obligatoire"
        }
        if (!this.state.modalObj.securisation && this.state.modalObj.reponse === "Accepté") {
            error.securisation = "Sécurisation est obligatoire"
        }
        if (Object.keys(error).length > 0) {
            this.setState({ error: error })
            return
        }
        this.setState({ loading: true })
        axios.post("/api/qualityNotice/valider", this.state.modalObj)
            .then(res => {
                this.setState({
                    modalObj: {
                        numeroQn: res.data.numeroQn,
                    },
                    listCtcFiles: [],
                    box: null,
                    message: "Enregistrement effectué avec succès QN: " + res.data.numeroQn,
                    loading: false,
                    showDetail: false
                })
                this.numeroQnRef.focus()
                this.numeroQnRef.select()
            })
            .catch(err => {
                this.setState({ error: err.response.data, loading: false })
                console.log(err)
            })

    }

    renderError = () => {
        return (this.state.error && Object.keys(this.state.error).length !== 0)
            && <div className="alert alert-danger alert-error text-center m-4" role="alert">
                <ul>
                    <button type='btn' className="btn btn-toclose" onClick={() => { this.setState({ error: null }) }}>
                        <FontAwesomeIcon icon={faTimes} size="2x" />
                    </button>
                    {this.renderErrorsAlert(this.state.error)}
                </ul>
            </div>
    }

    renderErrorsAlert(errors) {
        let arr = [];
        if (typeof errors === 'string') {
            arr.push(<li>{errors}</li>)
        } else {
            for (let prop in errors) {
                if (typeof errors[prop] === 'string') {
                    if (typeof prop === 'number') {
                        arr.push(<li>{parseInt(prop) + 1}: {errors[prop]}</li>)
                    } else {
                        arr.push(<li>{prop}: {errors[prop]}</li>)
                    }
                } else if (typeof errors[prop] === "object") {
                    if (Object.keys(errors[prop]).length > 0) {
                        arr.push(<li>{prop}: <ul>{this.renderErrorsAlert(errors[prop])}</ul></li>)
                    }

                }
            }
        }
        return arr
    }

    loadFournisseur = () => {
        axios.get("/api/query/refDetails?reftissu=" + this.state.modalObj.reftissu)
            .then(res => {
                this.handleChanges({ nomFournisseur: res.data.fournisseur, reftissuDescription: res.data.description })
            })
            .catch(err => {
                console.log(err)
            })

    }

    loadFromWO = () => {
        if (this.state.modalObj.wo === null || this.state.modalObj.wo === "") return
        axios.get(`/api/cuttingRequestBoxData/wo/${this.state.modalObj.wo}`)
            .then(respondWo => {
                this.setState({
                    modalObj: {
                        ...this.state.modalObj,
                        partnumber: respondWo.data.partNumber,
                        sequence: respondWo.data.sequence,
                    },
                    box: respondWo.data
                })

                axios.get(`/api/ctcFiles/pn/${respondWo.data.partNumber}`)
                    .then(res => {
                        this.setState({
                            modalObj: {
                                ...this.state.modalObj,
                                projet: res.data[0].projet
                            },
                            listCtcFiles: res.data
                        })
                        axios.get(`/api/cuttingRequestSerieInfo/machines?sequence=${respondWo.data.sequence}&reftissuList=${this.state.modalObj.reftissu.replaceAll(" / ", ",")}`)
                            .then(resMachines => {
                                this.setState({
                                    modalObj: {
                                        ...this.state.modalObj,
                                        machine: resMachines.data.join(" / "),
                                    }
                                })
                            })
                            .catch(err => {
                                console.log(err)
                            })
                    })
            })
            .catch(err => {
                console.log(err)
            })
    }

    renderTableRouleauDetails = () => {
        /*
        sh
        */
        return <div className='table-responsive entity-table'>
            <table className='table table-bordered m-0'>
                <thead>
                    <tr>
                        <th>Serie</th>
                        <th>idRouleau</th>
                        <th>lotFrs</th>
                        <th>totalUsage</th>
                        <th>createdAt</th>
                    </tr>
                </thead>
                <tbody>
                    {this.state.rouleauDetails === null ? <tr><td colSpan="6">loading...</td></tr> :
                        this.state.rouleauDetails.length === 0 ? <tr><td colSpan="6">No data</td></tr> :
                            this.state.rouleauDetails.map((rouleau, index) => {
                                return <tr key={index}
                                    onClick={() => {
                                        this.handleChanges({ idRouleau: rouleau.idRouleau, lotFrs: rouleau.lotFrs, dateCoupe: rouleau.createdAt })
                                    }}
                                >
                                    <td>{rouleau.serie}</td>
                                    <td>{rouleau.idRouleau}</td>
                                    <td>{rouleau.lotFrs}</td>
                                    <td>{rouleau.totalUsage}</td>
                                    <td>{rouleau.createdAt}</td>
                                </tr>
                            })}
                </tbody>
            </table>
        </div>
    }

    render() {
        const modelItemsId = this.state.modalObj.numEmp ? this.state.modalObj.numEmp.split(" / ").map(e => {
            let arr = e.split(" : ")
            if (arr.length === 1) arr.push("1")
            return [arr[0], arr[1]]
        }) : []
        const options = this.state.listCtcFiles.map(e => { return { label: e.panelNumber, value: e.panelNumber, item: e } }) || []
        const { user } = this.props.security;
        return (
            <div>
                <h1 className='text-center p-2'>
                    Lear Quality Notice
                </h1>
                {this.state.showDetail && <div className='mx-3'>
                    <div className='row py-2'>
                        <label className='col-2 col-form-label text-right'>Numero QN:</label>
                        <div className='col-4 p-0'>
                            <input className='form-control form-control-sm' ref={ref => this.numeroQnRef = ref}
                                value={this.state.modalObj.numeroQn}
                                // style={{ width: '100px' }}
                                onChange={(e) => {
                                    this.setState({
                                        modalObj: {
                                            numeroQn: e.target.value
                                        },
                                        listCtcFiles: [],
                                        box: null,
                                        showDetail: false
                                    })
                                }}
                                onKeyUp={(e) => {
                                    if (e.key === "Enter") {
                                        this.setState({ showDetail: null })
                                        axios.get(`/api/qualityNotice/${this.state.modalObj.numeroQn}`)
                                            .then(res => {
                                                this.setState({ modalObj: res.data, showDetail: true })
                                                this.loadFromWO()
                                                // this.loadFournisseur()
                                                if (res.data.site === "TRIM1" && res.data.typeDefaut === "Défaut fournisseur") {
                                                    if (res.data.wo && res.data.wo.length > 0) {
                                                        this.loadRouleauDetails()
                                                    }
                                                    this.loadFournisseur()
                                                }
                                            })
                                            .catch(err => {
                                                console.log(err)
                                            })
                                    }
                                }}
                            />
                        </div>
                    </div>
                    <div className='row py-2'>
                        <label className='col-2 text-right'>
                            Matricule controleur:
                        </label>
                        <div className='col-4 p-0'>
                            <span>
                                {this.state.modalObj.createdBy} {this.state.modalObj.createdAt}
                            </span>
                        </div>
                        <label className='col-2 text-right'>
                            Chef direct:
                        </label>
                        <div className='col-4 p-0'>
                            <span>
                                {this.state.modalObj.coordinateur}
                            </span>
                        </div>

                        <label className='col-2 text-right'>
                            Site:
                        </label>
                        <div className='col-4 p-0'>
                            <span>
                                {this.state.modalObj.site}
                            </span>
                        </div>
                        {this.state.modalObj.wo && this.state.modalObj.wo.length > 0 &&
                            [
                                <label className='col-2  text-right'>WO :</label>,
                                <div className='col-4 p-0'>
                                    <span>
                                        {this.state.modalObj.wo}
                                    </span>
                                </div>,
                                <label className='col-2 text-right'>
                                    Projet:
                                </label>,
                                <div className='col-4 p-0'>
                                    <span>
                                        {this.state.modalObj.projet}
                                    </span>
                                </div>,

                                <label className='col-2 text-right'>Quantité Défaut :</label>,
                                <div className='col-4 p-0'>
                                    <span>
                                        {this.state.modalObj.quantite}
                                    </span>
                                </div>,
                                <label className='col-2 text-right'>
                                    Sequence:
                                </label>,
                                <div className='col-4 p-0'>
                                    <span>
                                        {this.state.modalObj.sequence}
                                    </span>
                                </div>,
                                <label className='col-2 text-right'>
                                    Partnumber:
                                </label>,
                                <div className='col-4 p-0'>
                                    <span>
                                        {this.state.modalObj.partnumber}
                                    </span>
                                    {this.state.box && <button className='btn btn-outline-primary'
                                        style={{ padding: "0 10", height: 22, marginLeft: 10, fontSize: 12 }}
                                        onClick={() => {
                                            this.setState({ showModalGamme: true })
                                        }}
                                    >
                                        <FontAwesomeIcon icon={faEye} /> Gamme
                                    </button>}
                                </div>,
                                <label className='col-2 text-right'>Num Emp :</label>,

                                <div className='col-4 p-0  z-index-11 table-responsive entity-table'>
                                    <table className='table table-bordered m-0'>
                                        <thead>
                                            <tr>
                                                <th>Num Emp</th>
                                                <th>Quantité</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {modelItemsId.map((emp, index) => {
                                                return <tr key={index}>
                                                    <td className='table-elem-sm'>
                                                        {emp[0]}
                                                    </td>
                                                    <td className='table-elem-sm'>
                                                        {emp[1]}
                                                    </td>
                                                </tr>
                                            })}
                                            <tr style={{ backgroundColor: "#f5f5f5" }}>
                                                <td style={{ fontWeight: "bold" }}>Total</td>
                                                <td style={{ fontWeight: "bold" }}>{this.state.modalObj.quantite}</td>
                                            </tr>
                                        </tbody>
                                    </table>

                                </div>

                            ]}

                        <label className='col-2 text-right'>
                            Reftissu:
                        </label>
                        <div className='col-4 p-0'>
                            <span>
                                {this.state.modalObj.reftissu}
                            </span>
                        </div>
                        <label className='col-2 text-right'>
                            Reftissu Description :
                        </label>
                        <div className='col-4 p-0'>
                            <span>
                                {this.state.modalObj.reftissuDescription}
                            </span>
                        </div>
                        <label className='col-2 text-right'>
                            Fournisseur :
                        </label>
                        <div className='col-4 p-0'>
                            <span>
                                {this.state.modalObj.nomFournisseur}
                            </span>
                        </div>
                        <label className='col-2 text-right'>
                            Type Défaut :
                        </label>
                        <div className='col-4 p-0'>
                            <span>
                                {this.state.modalObj.typeDefaut}
                            </span>
                        </div>
                        <label className='col-2 text-right'>Code Défaut :</label>
                        <div className='col-4 p-0'>
                            {this.state.modalObj.codeDefaut && <span>
                                {this.state.modalObj.codeDefaut.code + " : " + this.state.modalObj.codeDefaut.description}
                            </span>}
                        </div>

                        {this.state.modalObj.typeDefaut === "Défaut Fournisseur" &&
                            [<label className='col-2 text-right'>
                                N° Rouleau :
                            </label>,
                            <div className='col-4 p-0'>
                                <span>
                                    {this.state.modalObj.idRouleau}
                                </span>
                            </div>,
                            <label className='col-2 text-right'>lotFrs :</label>,
                            <div className='col-4 p-0'>
                                <span>
                                    {this.state.modalObj.lotFrs}
                                </span>
                            </div>]}

                        <label className='col-2 text-right'>
                            Date Coupe  :
                        </label>
                        <div className='col-4 p-0'>
                            <span>
                                {this.state.modalObj.dateCoupe}
                            </span>
                        </div>
                        <label className='col-2 text-right'>Métrage écarté :</label>
                        <div className='col-4 p-0'>
                            <span>
                                {this.state.modalObj.metrageEcarte}
                            </span>
                        </div>

                        {this.state.modalObj.wo && this.state.modalObj.wo.length > 0 && [<label className='col-2 col-form-label text-right'>Machine :</label>,
                        <div className='col-4 col-form-label'>
                            <span>
                                {this.state.modalObj.machine}
                            </span>
                        </div>]}
                        <label className='col-2 col-form-label text-right'>Description :</label>
                        <div className='col-4 p-0'>
                            <textarea type='text' className='form-control form-control-sm' ref={ref => this.descriptionRef = ref}
                                value={this.state.modalObj.description} rows={5}
                            />
                        </div>
                    </div>
                    <div className='row py-2'>
                        <label className='col-2 col-form-label text-right'>image1:</label>
                        <div className='col-4 p-0'>
                            {this.state.modalObj.image1 &&
                                <span style={{ fontSize: 12 }}
                                    className="btn btn-link"
                                    onClick={() => {
                                        axios(
                                            {
                                                url: `/api/file/` + this.state.modalObj.image1, //your url
                                                method: 'GET',
                                                responseType: 'blob', // important
                                            }
                                        ).then((response) => {
                                            const url = window.URL.createObjectURL(new Blob([response.data]));
                                            const link = document.createElement('a');
                                            link.href = url;
                                            link.setAttribute('download', this.state.modalObj.image1); //or any other extension
                                            document.body.appendChild(link);
                                            link.click();
                                        })
                                    }}
                                >
                                    <img src={`/api/file/${this.state.modalObj.image1}`} style={{ maxWidth: "100%", maxHeight: "400px" }} />
                                </span>}
                        </div>
                        <label className='col-2 col-form-label text-right'>image2:</label>
                        <div className='col-4 p-0'>
                            {this.state.modalObj.image2 &&
                                <span style={{ fontSize: 12 }}
                                    className="btn btn-link"
                                    onClick={() => {
                                        axios(
                                            {
                                                url: `/api/file/` + this.state.modalObj.image2, //your url
                                                method: 'GET',
                                                responseType: 'blob', // important
                                            }
                                        ).then((response) => {
                                            const url = window.URL.createObjectURL(new Blob([response.data]));
                                            const link = document.createElement('a');
                                            link.href = url;
                                            link.setAttribute('download', this.state.modalObj.image2); //or any other extension
                                            document.body.appendChild(link);
                                            link.click();
                                        })
                                    }}
                                >
                                    <img src={`/api/file/${this.state.modalObj.image2}`} style={{ maxWidth: "100%", maxHeight: "400px" }} />
                                </span>}
                        </div>
                    </div>
                    <hr />
                    {this.state.modalObj.site === "TRIM1" && <>
                        {this.state.modalObj.typeDefaut === "Défaut fournisseur" && [
                            (this.state.modalObj.wo && this.state.modalObj.wo.length > 0 && <div className='row py-2'>
                                <div className='col-4 text-right'>
                                    Rouleau Details :
                                </div>
                                <div className='col-6 p-0'>
                                    {this.renderTableRouleauDetails()}
                                </div>
                                <div className='col-2 p-0'>
                                    <button className='btn btn-primary' onClick={() => {
                                        this.loadRouleauDetails()
                                        this.loadFournisseur()
                                    }}>Refresh</button>
                                </div>
                            </div>),
                            <div className='row py-2'>
                                <label className='col-4 col-form-label text-right'>Reftissu Description :</label>
                                <div className='col-4 p-0'>
                                    <input className='form-control form-control-sm'
                                        value={this.state.modalObj.reftissuDescription || ""}
                                        // style={{ width: '100px' }}
                                        onChange={(e) => {
                                            this.handleChanges({ reftissuDescription: e.target.value })
                                        }}
                                    />
                                </div>
                            </div>,
                            <div className='row py-2'>
                                <label className='col-4 col-form-label text-right'>Nom Fournisseur :</label>
                                <div className='col-4 p-0'>
                                    <input className='form-control form-control-sm'
                                        value={this.state.modalObj.nomFournisseur || ""}
                                        onChange={(e) => {
                                            this.handleChanges({ nomFournisseur: e.target.value })
                                        }}
                                    />
                                </div>
                            </div>,


                            <div className='row py-2'>
                                <label className='col-4 col-form-label text-right'>ID Rouleau :</label>
                                <div className='col-4 p-0'>
                                    <input className='form-control form-control-sm'
                                        value={this.state.modalObj.idRouleau || ""}
                                        style={{ maxWidth: '250px' }}
                                        onChange={(e) => {
                                            this.handleChanges({ idRouleau: e.target.value })
                                        }}
                                    />
                                </div>
                            </div>,

                            <div className='row py-2'>
                                <label className='col-4 col-form-label text-right'>Lot Frs :</label>
                                <div className='col-4 p-0'>
                                    <input className='form-control form-control-sm'
                                        value={this.state.modalObj.lotFrs || ""}
                                        style={{ maxWidth: '250px' }}
                                        onChange={(e) => {
                                            this.handleChanges({ lotFrs: e.target.value })
                                        }}
                                    />
                                </div>
                            </div>,

                            <div className='row py-2'>
                                <label className='col-4 col-form-label text-right'>dateCoupe :</label>
                                <div className='col-4 p-0'>
                                    <input
                                        type="datetime-local"
                                        id="dateCoupe"
                                        name="dateCoupe"
                                        placeholder="Date Coupe"
                                        className="form-control"
                                        autoComplete="off"
                                        value={this.state.modalObj.dateCoupe ? moment(this.state.modalObj.dateCoupe.replace(", ", " ")).format('YYYY-MM-DDTHH:mm') : ''}
                                        onChange={event => {
                                            if (event.target.value) {
                                                this.handleChanges({ dateCoupe: moment(event.target.value).format('YYYY-MM-DDTHH:mm').replace("T", ", ") })
                                            } else {
                                                this.handleChanges({ dateCoupe: null })
                                            }
                                        }}

                                    />
                                </div>
                            </div>,


                        ]}
                    </>}

                    <div className='row py-2'>
                        <label className='col-4 col-form-label text-right'>Correct Défaut :</label>
                        <Select classNamePrefix="rs" className='col-4 p-0 z-index-11' id={"correctDefaut"} name={"correctDefaut"}
                            placeholder={"Correct Défaut"}
                            isClearable={false}
                            value={this.state.modalObj.correctDefaut ? { label: this.state.modalObj.correctDefaut.code + " : " + this.state.modalObj.correctDefaut.description, value: this.state.modalObj.correctDefaut } : null}
                            options={this.state.codeDefauts.filter(e => {
                                return (this.state.modalObj.typeDefaut === "Défaut coupe" && e.code.startsWith("C"))
                                    || (this.state.modalObj.typeDefaut === "Défaut fournisseur" && e.code.startsWith("CF"))
                                    || (this.state.modalObj.typeDefaut === "Défaut logistique" && e.code.startsWith("CL"))
                                    || (this.state.modalObj.typeDefaut === "Défaut CNC" && e.code.startsWith("CNC"))
                                    || (!this.state.modalObj.typeDefaut)
                            }).map(codeDefaut => ({ label: codeDefaut.code + " : " + codeDefaut.description, value: codeDefaut }))}
                            onChange={(option) => {
                                this.handleChanges({ ...this.state.modalObj, correctDefaut: option ? option.value : null })
                            }}
                        />
                    </div>

                    <div className='row py-2'>
                        <label className='col-4 col-form-label text-right'>Quantité reçu :</label>
                        <div className='col-4 p-0'>
                            <input type='text' className='form-control form-control-sm' ref={ref => this.qteRecuRef = ref}
                                value={this.state.modalObj.qteRecu}
                                onChange={(e) => {
                                    this.handleChanges({ qteRecu: e.target.value })
                                }}
                            />
                        </div>
                    </div>

                    <div className='row py-2'>
                        <label className='col-4 col-form-label text-right'>Quantité reçu coiffes :</label>
                        <div className='col-4 p-0'>
                            <input type='text' className='form-control form-control-sm' ref={ref => this.qteRecuCoiffeRef = ref}
                                value={this.state.modalObj.qteRecuCoiffe}
                                onChange={(e) => {
                                    this.handleChanges({ qteRecuCoiffe: e.target.value })
                                }}
                            />
                        </div>
                    </div>
                    {/* add qteRecuMetrage  */}
                    <div className='row py-2'>
                        <label className='col-4 col-form-label text-right'>Quantité reçu métrage :</label>
                        <div className='col-4 p-0'>
                            <input type='text' className='form-control form-control-sm' ref={ref => this.qteRecuMetrageRef = ref}
                                value={this.state.modalObj.qteRecuMetrage}
                                onChange={(e) => {
                                    this.handleChanges({ qteRecuMetrage: e.target.value })
                                }}
                            />
                        </div>
                    </div>
                    <div className='row py-2'>
                        <label className='col-4 col-form-label text-right'>Réponse :</label>
                        <Select classNamePrefix="rs" className='col-4 p-0 z-index-11' id={"reponse"} name={"reponse"}
                            placeholder={"Réponse..."}
                            isClearable={false}
                            value={this.state.modalObj.reponse ? { label: this.state.modalObj.reponse, value: this.state.modalObj.reponse } : null}
                            options={[
                                { label: "Accepté", value: "Accepté" }, 
                                { label: "Refusé", value: "Refusé" },
                                { label: "Validé", value: "Validé" }
                            ]}
                            onChange={(option) => {
                                this.setState({ showCustomSecurisation: false });
                                this.handleChanges({
                                    ...this.state.modalObj,
                                    reponse: option ? option.value : null,
                                    decision: null,
                                    securisation: null,
                                })
                            }}
                        />
                    </div>
                    {(this.state.modalObj.reponse === "Accepté") && <div className='row py-2'>
                        <label className='col-4 col-form-label text-right'>Décision :</label>
                        <Select classNamePrefix="rs" className='col-4 p-0 z-index-11' id={"decision"} name={"decision"}
                            placeholder={"Décision..."}
                            isClearable={false}
                            value={this.state.modalObj.decision ? { label: this.state.modalObj.decision, value: this.state.modalObj.decision } : null}
                            options={[{ label: "Scrap", value: "Scrap" }, { label: "Récupération", value: "Récupération" }, { label: "NA", value: "NA" }]}
                            onChange={(option) => {
                                this.handleChanges({ ...this.state.modalObj, decision: option ? option.value : null })
                            }}
                        />
                    </div>}
                    {(this.state.modalObj.reponse === "Accepté") && <div className='row py-2'>
                        <label className='col-4 col-form-label text-right'>Sécurisation :</label>
                        <div className='col-4 p-0'>
                            <Select classNamePrefix="rs" className='z-index-11' id={"securisation"} name={"securisation"}
                                placeholder={"Sécurisation..."}
                                isClearable={false}
                                value={this.state.modalObj.securisation ? { label: this.state.modalObj.securisation, value: this.state.modalObj.securisation } : null}
                                options={[
                                    { label: "NA", value: "NA" }, 
                                    { label: "15 jours", value: "15 jours" },
                                    { label: "Autre (personnalisé)", value: "custom" }
                                ]}
                                onChange={(option) => {
                                    if (option && option.value === "custom") {
                                        this.setState({ showCustomSecurisation: true });
                                    } else {
                                        this.setState({ showCustomSecurisation: false });
                                        this.handleChanges({ ...this.state.modalObj, securisation: option ? option.value : null });
                                    }
                                }}
                            />
                            {this.state.showCustomSecurisation && (
                                <div className='mt-2 d-flex align-items-center'>
                                    <input 
                                        type='number' 
                                        className='form-control form-control-sm' 
                                        placeholder='Nombre de jours'
                                        style={{ width: '120px', marginRight: '8px' }}
                                        min="1"
                                        onChange={(e) => {
                                            if (e.target.value && !isNaN(e.target.value) && parseInt(e.target.value) > 0) {
                                                this.handleChanges({ 
                                                    ...this.state.modalObj, 
                                                    securisation: `${e.target.value} jours` 
                                                });
                                            }
                                        }}
                                    />
                                    <span>jours</span>
                                </div>
                            )}
                        </div>
                    </div>}

                    {/* Switch component after Sécurisation */}
                    <div className='row py-2'>
                        <label className='col-4 col-form-label text-right'>QRQC :</label>
                        <div className='col-4 p-0 d-flex align-items-center'>
                            <Switch
                                checked={this.state.modalObj.qrqc || false}
                                onChange={(checked) => {
                                    this.handleChanges({ ...this.state.modalObj, qrqc: checked })
                                }}
                                onColor="#28a745"
                                offColor="#dc3545"
                                checkedIcon={false}
                                uncheckedIcon={false}
                                height={20}
                                width={48}
                            />
                            <span className='ml-2'>
                                {this.state.modalObj.qrqc ? 'Activé' : 'Désactivé'}
                            </span>
                        </div>
                    </div>

                    <div className='row py-2'>
                        <label className='col-4 col-form-label text-right'>Remarque / CAUSE POTENTIEL:</label>
                        <div className='col-4 p-0'>
                            <textarea type='text' className='form-control form-control-sm' ref={ref => this.remarqueRef = ref}
                                value={this.state.modalObj.remarque} rows={5}
                                onChange={(e) => {
                                    this.handleChanges({ ...this.state.modalObj, remarque: e.target.value })
                                }}
                            />
                        </div>
                    </div>
                    <div className='row py-2'>
                        <label className='col-4 col-form-label text-right'>Fichier :</label>
                        <div className='d-flex flex-wrap col-8 mt-1'>
                            <input type="file"
                                onChange={(e) => {
                                    let formData = new FormData()
                                    formData.append('file', e.target.files[0])
                                    axios.post(`/api/file/store`, formData, {
                                        headers: {
                                            "Content-type": "application/json",
                                            "Content-Type": "multipart/form-data"
                                        }
                                    })
                                        .then((res) => {
                                            this.setState({ modalObj: { ...this.state.modalObj, fichier: res.data } })
                                        })
                                        .catch((err) => {
                                            console.log({ err })
                                        })
                                    console.log({ files2: e.target.files[0] })
                                }}
                            />
                            {this.state.modalObj.fichier &&
                                <span style={{ fontSize: 12, padding: "0 4" }}
                                    className="btn btn-link"
                                    onClick={() => {
                                        axios(
                                            {
                                                url: `/api/file/` + this.state.modalObj.fichier, //your url
                                                method: 'GET',
                                                responseType: 'blob', // important
                                            }
                                        ).then((response) => {
                                            const url = window.URL.createObjectURL(new Blob([response.data]));
                                            const link = document.createElement('a');
                                            link.href = url;
                                            link.setAttribute('download', this.state.modalObj.fichier); //or any other extension
                                            document.body.appendChild(link);
                                            link.click();
                                        })
                                    }}
                                >
                                    {this.state.modalObj.fichier}
                                    {/* <img src={`/api/file/${this.state.modalObj[field.name]}`} height="200px" /> */}
                                </span>
                            }
                        </div>
                    </div>
                    {this.state.modalObj.sendNotificationDate && <div className='row py-2'>
                        <label className='col-4 text-right'>
                            Notification envoyée:
                        </label>
                        <div className='col-4 p-0'>
                            <span>
                                {this.state.modalObj.notificationBy} {this.state.modalObj.sendNotificationDate}
                            </span>
                        </div>
                    </div>}
                    {this.state.modalObj.coupeValidationDate && <div className='row py-2'>
                        <label className='col-4 text-right'>
                            Validation Coupe:
                        </label>
                        <div className='col-4 p-0'>
                            <span>
                                {this.state.modalObj.coupeValidationBY} {this.state.modalObj.coupeValidationDate}
                            </span>
                        </div>
                    </div>}
                    {this.renderExtraEmailsReponse()}
                    {(this.state.modalObj.traiterPar && this.state.modalObj.traiterPar.length > 0)
                        ? <div className='row py-2'>
                            <label className='col-4 text-right'>
                                Traité Par:
                            </label>
                            <div className='col-4 p-0'>
                                <span>
                                    {this.state.modalObj.traiterPar} {this.state.modalObj.dateTraitement}
                                </span>
                            </div>
                        </div>
                        : <div style={{ display: "flex", justifyContent: "center", alignItems: "center", marginBottom: 20 }} >
                            <button className='btn btn-sm btn-warning'
                                style={{ width: "20%" }}
                                onClick={() => {
                                    this.setState({
                                        modalObj: {
                                            numeroQn: this.state.modalObj.numeroQn,
                                        },
                                        listCtcFiles: [],
                                        box: null,
                                        showDetail: false,
                                    })
                                    this.numeroQnRef.focus()
                                    this.numeroQnRef.select()
                                }}>Reset</button>
                            {user && user.roles && (user.roles.some(role => ["ROLE_QUALITE"].includes(role.authority))) &&
                                <button className='btn btn-sm btn-success ml-2' style={{ width: "20%" }} onClick={() => {
                                    axios.get("/api/qualityNotice/sendNotification/" + this.state.modalObj.numeroQn)
                                        .then(res => {
                                            this.setState({
                                                modalObj: {
                                                    ...res.data
                                                },
                                                message: "Notification Pièces non reçues est envoyée"
                                            })
                                        })
                                        .catch(err => {
                                            console.log(err)
                                        })
                                }}>Pièces non reçues</button>}
                            {user && user.roles && (user.roles.some(role => ["ROLE_QUALITE"].includes(role.authority))) && !this.state.modalObj.coupeValidationDate &&
                                <button className='btn btn-sm btn-primary ml-2' style={{ width: "20%" }} onClick={() => {
                                    axios.get("/api/qualityNotice/transfertQR/" + this.state.modalObj.numeroQn)
                                        .then(res => {
                                            this.setState({
                                                modalObj: {
                                                    ...res.data
                                                },
                                                message: "QN est validé et envoyé au qualité récéption"
                                            })
                                        })
                                        .catch(err => {
                                            console.log(err)
                                        })
                                }}>Transfert à la qualité reception</button>}
                            {user && user.roles && (user.roles.some(role => ["ROLE_QUALITE"].includes(role.authority))) && !this.state.modalObj.coupeValidationDate &&
                                <button className='btn btn-sm btn-primary ml-2' style={{ width: "20%" }} onClick={() => {
                                    axios.get("/api/qualityNotice/transfertLogistique/" + this.state.modalObj.numeroQn)
                                        .then(res => {
                                            this.setState({
                                                modalObj: {
                                                    ...res.data
                                                },
                                                message: "QN est validé et envoyé au logistique"
                                            })
                                        })
                                        .catch(err => {
                                            console.log(err)
                                        })
                                }}>Transfert aux logistiques</button>}

                            <button
                                className='btn btn-sm btn-success ml-2'
                                onClick={() => this.handleSave()} style={{ width: "20%" }}
                                disabled={this.state.loading}
                            >
                                {this.state.loading ? <FontAwesomeIcon icon={faSpinner} spin /> : "Valider"}
                            </button>
                        </div>}
                    {(this.state.modalObj.traiterPar && this.state.modalObj.traiterPar.length > 0) && (
                        (this.state.modalObj.superviseurConfirmationBy && this.state.modalObj.superviseurConfirmationBy.length > 0)
                            ? <div className='row py-2'>
                                <label className='col-4 text-right'>
                                    Confirmation Superviseur Par:
                                </label>
                                <div className='col-4 p-0'>
                                    <span>
                                        {this.state.modalObj.superviseurConfirmationBy} {this.state.modalObj.superviseurConfirmationDate}
                                    </span>
                                </div>
                            </div>
                            : <div style={{ display: "flex", justifyContent: "center", alignItems: "center", marginBottom: 20 }} >
                                {user && user.roles && (user.roles.some(role => ["ROLE_QN_SUPERVISOR"].includes(role.authority))) &&
                                    <button className='btn btn-sm btn-primary ml-2' style={{ width: "40%" }} onClick={() => {
                                        axios.get("/api/qualityNotice/confirmationSuperviseur/" + this.state.modalObj.numeroQn)
                                            .then(res => {
                                                this.setState({
                                                    modalObj: {
                                                        ...res.data
                                                    },
                                                    message: "Demande confirmée : " + this.state.modalObj.numeroQn
                                                })
                                            })
                                            .catch(err => {
                                                console.log(err)
                                            })
                                    }}>
                                        Confirmation de superviseur
                                    </button>}
                            </div>
                    )}
                </div>}
                {this.state.showDetail === null && <div className='text-center py-5'>
                    <FontAwesomeIcon icon={faSpinner} spin size="2x" />
                </div>}
                {this.renderModalGamme(modelItemsId)}
                {this.renderError()}
                {this.renderConfirme()}

            </div>
        )
    }
}

QualityNoticeValidation.propTypes = {
    security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
    security: state.security
})

export default connect(mapStateToProps, {})(QualityNoticeValidation);

