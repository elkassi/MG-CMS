import axios from 'axios';
import moment from 'moment';
import React, { Component } from 'react'
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { optionsShift } from '../../metadata';
import Select from "react-select";
import { faFileCsv, faMagnifyingGlass, faPenAlt, faTimes } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { Modal } from 'react-bootstrap'
import FormCoupe from './FormCoupe';

export default class SuiviQulite extends Component {

    constructor() {
        super();
        this.state = {
            entriesList: [],
            date: moment().add(2, 'hours').format('YYYY-MM-DD'),
            date2: moment().add(2, 'hours').format('YYYY-MM-DD'),
            shift: this.getShift(moment().add(2, 'hours')),
            type: "coupe",
            machine: null,
            productionTableList: [],
            filter: {}
        }
    }

    getShift(date) {
        let hour = date.hour()
        if (hour >= 0 && hour < 8) {
            return 1
        } else if (hour >= 0 && hour < 8) {
            return 2
        } else {
            return 3
        }
    }


    componentDidMount() {
        // this.getData(moment().add(2, 'hours').format('YYYY-MM-DD'), this.getShift(moment().add(2, 'hours')), "coupe", null)
        axios.get("/api/productionTable/list")
            .then(res => {
                this.setState({ productionTableList: res.data })
            })
            .catch(err => {
                console.log(err)
            })
    }

    getData = (date, date2, valide, machine) => {
        let filter = "date=" + date + "&date2=" + date2;
        this.setState({ entriesList: null })


        if (valide) {
            if (valide === "validé") {
                filter += "&valide=true"
            } else if (valide === "non validé") {
                filter += "&valide=false"
            }
        }

        if (machine) {
            filter += "&machine=" + machine
        }

        axios.get(`/api/cuttingRequestSerieInfo/statusQualite?${filter}`)
            .then(res => {
                this.setState({ entriesList: res.data })
            })
    }

    renderHeader = () => {
        /*
        private String cuttingRequestSequence;
    private String serie;
    private String partNumberMaterial;
    private String placement;
    private String tableMatelassage;
    private String tableCoupe;
    private String tableQualite;
              premierPaquet,
  milieuPaquet,
  dernierPaquet,
  verificationDrill,

    private int nbrPiece;
    private int nbrCouche;
    private int nbrPieceTotal;

    private int qteNonConforme;
    private String codeDefautCode;
    private String codeDefautDescription;
    private String lieuDetection;
    private int qteScrap;
    private String codeScrapCode;
    private String codeScrapDescription;
    private LocalDateTime dateDebutCoupe;
    private LocalDateTime dateValidationQualite;

        */
        return <thead className='entity-table-header'>
            <tr>
                <th className='table-elem-sm'>Séquence</th>
                <th className='table-elem-sm'>Serie</th>
                <th className='table-elem-sm'>Part Number Material</th>
                <th className='table-elem-sm'>Placement</th>
                <th className='table-elem-sm'>Table Matelassage</th>
                <th className='table-elem-sm'>Table Coupe</th>
                <th className='table-elem-sm'>Table Qualite</th>
                <th className='table-elem-sm'>Premier Paquet</th>
                <th className='table-elem-sm'>Milieu Paquet</th>
                <th className='table-elem-sm'>Dernier Paquet</th>
                <th className='table-elem-sm'>Verification Drill</th>
                <th className='table-elem-sm'>Verification Drill 2</th>
                <th className='table-elem-sm'>Nbr Piece</th>
                <th className='table-elem-sm'>Nbr Couche</th>
                <th className='table-elem-sm'>Nbr Piece Total</th>
                <th className='table-elem-sm'>Qte Non Conforme</th>
                <th className='table-elem-sm'>Code Defaut Code</th>
                <th className='table-elem-sm'>Code Defaut Description</th>
                <th className='table-elem-sm'>Lieu Detection</th>
                <th className='table-elem-sm'>Qte Scrap</th>
                <th className='table-elem-sm'>Code Scrap Code</th>
                <th className='table-elem-sm'>Code Scrap Description</th>
                <th className='table-elem-sm'>Date Debut Coupe</th>
                <th className='table-elem-sm'>Date Validation Qualite</th>
            </tr>
            {/* <tr>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.sequence} onChange={event => this.setState({ filter: { ...this.state.filter, sequence: event.target.value } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.serie} onChange={event => this.setState({ filter: { ...this.state.filter, serie: event.target.value } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.partNumberMaterial} onChange={event => this.setState({ filter: { ...this.state.filter, partNumberMaterial: event.target.value } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.placement} onChange={event => this.setState({ filter: { ...this.state.filter, placement: event.target.value } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.tableMatelassage} onChange={event => this.setState({ filter: { ...this.state.filter, tableMatelassage: event.target.value } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.tableCoupe} onChange={event => this.setState({ filter: { ...this.state.filter, tableCoupe: event.target.value } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.tableQualite} onChange={event => this.setState({ filter: { ...this.state.filter, tableQualite: event.target.value } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.nbrPiece} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, nbrPiece: event.target.value === "" ? null : parseInt(event.target.value) } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.nbrCouche} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, nbrCouche: event.target.value === "" ? null : parseInt(event.target.value) } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.nbrPieceTotal} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, nbrPieceTotal: event.target.value === "" ? null : parseInt(event.target.value) } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.qteNonConforme} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, qteNonConforme: event.target.value === "" ? null : parseInt(event.target.value) } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.codeDefautCode} onChange={event => this.setState({ filter: { ...this.state.filter, codeDefautCode: event.target.value } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.codeDefautDescription} onChange={event => this.setState({ filter: { ...this.state.filter, codeDefautDescription: event.target.value } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.lieuDetection} onChange={event => this.setState({ filter: { ...this.state.filter, lieuDetection: event.target.value } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.qteScrap} type="number" onChange={event => this.setState({ filter: { ...this.state.filter, qteScrap: event.target.value === "" ? null : parseInt(event.target.value) } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.codeScrapCode} onChange={event => this.setState({ filter: { ...this.state.filter, codeScrapCode: event.target.value } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.codeScrapDescription} onChange={event => this.setState({ filter: { ...this.state.filter, codeScrapDescription: event.target.value } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.dateDebutCoupe} type="date" onChange={event => this.setState({ filter: { ...this.state.filter, dateDebutCoupe: event.target.value } })} />
                </th>
                <th className='table-elem-sm'>
                    <input value={this.state.filter.dateValidationQualite} type="date" onChange={event => this.setState({ filter: { ...this.state.filter, dateValidationQualite: event.target.value } })} />
                </th>
            </tr> */}
        </thead>
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

    renderRow(item, ind) {
        /*
        private String cuttingRequestSequence;
    private String serie;
    private String partNumberMaterial;
    private String placement;
    private String tableMatelassage;
    private String tableCoupe;
    private String tableQualite;
    premierPaquet,
  milieuPaquet,
  dernierPaquet,
  verificationDrill,

    private int nbrPiece;
    private int nbrCouche;
    private int nbrPieceTotal;
    private int qteNonConforme;
    private String codeDefautCode;
    private String codeDefautDescription;
    private String lieuDetection;
    private int qteScrap;
    private String codeScrapCode;
    private String codeScrapDescription;
    private LocalDateTime dateDebutCoupe;
    private LocalDateTime dateValidationQualite;
        */
        return <tr key={"row-" + ind} className={"clickable-element"} onDoubleClick={() => {
            this.setState({ showSerieForm: item.serie })
        }}>
            <td className='table-elem-sm'>{item.cuttingRequestSequence}</td>
            <td className='table-elem-sm'>{item.serie}</td>
            <td className='table-elem-sm'>{item.partNumberMaterial}</td>
            <td className='table-elem-sm'>{item.placement}</td>
            <td className='table-elem-sm'>{item.tableMatelassage}</td>
            <td className='table-elem-sm'>{item.tableCoupe}</td>
            <td className='table-elem-sm'>{item.tableQualite}</td>
            <td className='table-elem-sm'>{item.premierPaquet}</td>
            <td className='table-elem-sm'>{item.milieuPaquet}</td>
            <td className='table-elem-sm'>{item.dernierPaquet}</td>
            <td className='table-elem-sm'>{item.verificationDrill}</td>
            <td className='table-elem-sm'>{item.verificationDrill2}</td>

            <td className='table-elem-sm'>{item.nbrPiece}</td>
            <td className='table-elem-sm'>{item.nbrCouche}</td>
            <td className='table-elem-sm'>{item.nbrPieceTotal}</td>
            <td className='table-elem-sm'>{item.qteNonConforme}</td>
            <td className='table-elem-sm'>{item.codeDefautCode}</td>
            <td className='table-elem-sm'>{item.codeDefautDescription}</td>
            <td className='table-elem-sm'>{item.lieuDetection}</td>
            <td className='table-elem-sm'>{item.qteScrap}</td>
            <td className='table-elem-sm'>{item.codeScrapCode}</td>
            <td className='table-elem-sm'>{item.codeScrapDescription}</td>
            <td className='table-elem-sm'>{item.dateDebutCoupe}</td>
            <td className='table-elem-sm'>{item.dateValidationQualite}</td>
        </tr>
    }

    serieModal = () => {
        return <Modal
            show={this.state.showSerieForm !== null}
            onHide={() => this.setState({ showSerieForm: null })}
            dialogClassName="modal-90w"
            centered
        >
            {this.state.showSerieForm && <div style={{ height: "75vh", overflowY: 'auto' }}>
                <FormCoupe serie={this.state.showSerieForm} />
            </div>}
        </Modal>
    }

    downloadCSV = () => {
        const replacer = (key, value) => value === null ? '' : value;
        // user the hearder that i have in my table
        const header = ["cuttingRequestSequence", "serie", "partNumberMaterial", "placement", "tableMatelassage", "tableCoupe", "tableQualite", "premierPaquet", "milieuPaquet", "dernierPaquet", "verificationDrill","verificationDrill2", "nbrPiece", "nbrCouche", "nbrPieceTotal", "qteNonConforme", "codeDefautCode", "codeDefautDescription", "lieuDetection", "qteScrap", "codeScrapCode", "codeScrapDescription", "dateDebutCoupe", "dateValidationQualite"];
        /*
                let csv = this.state.rapportOverlap.map(row => header.map(fieldName => JSON.stringify(row[fieldName], replacer)).join(','));
        csv.unshift(header.join(','));
        csv = csv.join('\r\n');
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'rapportOverlap.csv';
        a.click();
        window.URL.revokeObjectURL(url);
*/
        let csv = this.state.entriesList.map(row => header.map(fieldName => JSON.stringify(row[fieldName], replacer)).join(','));
        csv.unshift(header.join(','));
        csv = csv.join('\r\n');
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'SuiviQualite.csv';
        a.click();
        window.URL.revokeObjectURL(url);
    }

    render() {
        return (
            <div>
                <h1 className='text-center' style={{ margin: "10 0 8" }}>Suivi Qualité</h1>
                <div className='d-flex align-items-center mb-1 mx-2'>
                    <div style={{ width: 120, marginLeft: "0 12" }}>
                        <DatePicker
                            id={"date"}
                            name={"date"}
                            placeholderText={"date"}
                            className="form-control"
                            autoComplete="off"
                            selected={this.state.date ? moment(this.state.date, 'YYYY-MM-DD').toDate() : null}
                            onChange={date => this.setState({ date: (date ? moment(date).format('YYYY-MM-DD') : null) })}
                            isClearable={false}
                            dateFormat={'yyyy-MM-dd'}
                        />
                    </div>
                    <div style={{ width: 120, marginLeft: "0 12" }}>
                        <DatePicker
                            id={"date2"}
                            name={"date2"}
                            placeholderText={"date2"}
                            className="form-control mx-2"
                            autoComplete="off"
                            selected={this.state.date2 ? moment(this.state.date2, 'YYYY-MM-DD').toDate() : null}
                            onChange={date2 => this.setState({ date2: (date2 ? moment(date2).format('YYYY-MM-DD') : null) })}
                            isClearable={false}
                            dateFormat={'yyyy-MM-dd'}
                        />
                    </div>
                    {this.state.date && <div style={{ width: 100, marginLeft: 15 }}>
                        <Select classNamePrefix="rs"
                            placeholder={"Status..."} style={{ width: 100 }}
                            isClearable={true}
                            value={this.state.valide ? { label: this.state.valide, value: this.state.valide } : null}
                            options={[
                                { label: "validé", value: "validé" },
                                { label: "non validé", value: "non validé" },
                            ]}
                            onChange={(option) => {
                                this.setState({ valide: option ? option.value : null })
                            }}
                        />
                    </div>}
                    {this.state.date && <div style={{ width: 150, margin: "0 8" }}>
                        <Select classNamePrefix="rs"
                            placeholder={"Machine..."} style={{ width: 150 }}
                            isClearable={true}
                            value={this.state.machine ? { label: this.state.machine, value: this.state.machine } : null}
                            options={this.state.productionTableList.map((item) => { return { label: item.nom, value: item.nom } })}
                            onChange={(option) => {
                                this.setState({ machine: option ? option.value : null })
                            }}
                        />
                    </div>}
                    <button className='btn btn-danger' onClick={() => {
                        this.getData(this.state.date, this.state.date2, this.state.valide, this.state.machine)
                    }}>
                        <FontAwesomeIcon icon={faMagnifyingGlass} />
                    </button>
                    <button onClick={this.downloadCSV} className='btn btn-outline-danger btn-sm ml-1'><FontAwesomeIcon icon={faFileCsv} /> Télecharger</button>
                </div>
                <div className='px-2'>
                    <div className='table-responsive entity-table mb-2 slider-elem'>
                        <table className='table table-bordered m-0'>
                            {this.renderHeader()}
                            <tbody>
                                {this.state.entriesList == null ? <tr ><td colSpan={100}>Chargement...</td></tr> :
                                    this.state.entriesList.length === 0
                                        ? <tr><td colSpan={100}>Aucune données disponibles</td> </tr>
                                        : this.state.entriesList
                                            .filter(elem => (
                                                (this.state.filter.serie == null || elem.serie.toUpperCase().startsWith(this.state.filter.serie.toUpperCase())) &&
                                                (this.state.filter.sequence == null || elem.cuttingRequest.sequence.toUpperCase().startsWith(this.state.filter.sequence.toUpperCase())) &&
                                                (this.state.filter.partNumberMaterial == null || elem.partNumberMaterial.toString().startsWith(this.state.filter.partNumberMaterial)) &&
                                                (this.state.filter.nbrCouche == null || elem.nbrCouche.toString().includes(this.state.filter.nbrCouche)) &&
                                                (this.state.filter.planningDate == null || elem.planningDate.toString().startsWith(this.state.filter.planningDate)) &&
                                                (this.state.filter.shift == null || elem.shift.toString().startsWith(this.state.filter.shift)) &&
                                                (this.state.filter.nbrCouche == null || elem.nbrCouche === (this.state.filter.nbrCouche)) &&
                                                (this.state.filter.longueur == null || elem.longueur === (this.state.filter.longueur)) &&
                                                (this.state.filter.statusMatelassage == null || elem.statusMatelassage.toUpperCase().toString().startsWith(this.state.filter.statusMatelassage.toUpperCase())) &&
                                                (this.state.filter.statusCoupe == null || elem.statusCoupe.toUpperCase().toString().startsWith(this.state.filter.statusCoupe.toUpperCase()))
                                            ))
                                            .map((item, ind) => {
                                                return this.renderRow(item, ind)
                                            })}
                            </tbody>
                        </table>
                    </div>
                </div>
                {this.state.showSerieForm && this.serieModal()}
            </div>
        )
    }
}
