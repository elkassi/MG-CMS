import { SignalCellularNull } from '@mui/icons-material'
import React, { Component } from 'react'
import axios from 'axios';

export default class PatternSearch extends Component {

    constructor(props) {
        super(props)
        this.state = {
            pattern: '',
            placementFormat: '',
            data: []
        }
    }

    render() {
        return (
            <div>
                <h1 className='text-center' style={{ marginTop: 10, marginBottom: 20 }}>Recherche des empiècements</h1>
                <div className='d-flex justify-content-center mb-3'>
                    <input
                        className='form-control' style={{ width: 300 }}
                        type='text'
                        placeholder="Pattern d'empiecement"
                        value={this.state.pattern}
                        onChange={e => this.setState({ pattern: e.target.value })}
                        onKeyUp={e => {
                            if (e.key === 'Enter' && this.state.pattern !== '' && this.state.placementFormat !== '') {
                                this.setState({ data: null })
                                axios.get(`/api/placementDetail/empV2?pattern=${this.state.pattern}&placementFormat=${this.state.placementFormat}`)
                                    .then(res => {
                                        this.setState({ data: res.data })
                                    })
                                    .catch(err => {
                                        console.log(err)
                                        this.setState({ data: [] })
                                    })
                            }
                        }}
                    />
                    <input
                        className='form-control' style={{ width: 300, marginLeft: 10 }}
                        type='text'
                        placeholder="Code placement"
                        value={this.state.placementFormat}
                        onChange={e => this.setState({ placementFormat: e.target.value })}
                        onKeyUp={e => {
                            if (e.key === 'Enter' && this.state.pattern !== '' && this.state.placementFormat !== '') {
                                this.setState({ data: null })
                                axios.get(`/api/placementDetail/empV2?pattern=${this.state.pattern}&placementFormat=${this.state.placementFormat}`)
                                    .then(res => {
                                        this.setState({ data: res.data })
                                    })
                                    .catch(err => {
                                        console.log(err)
                                        this.setState({ data: [] })
                                    })
                            }
                        }}
                    />
                </div>

                <div>
                    <table className='table table-striped table-bordered '>
                        <thead>
                            <tr>
                                <th>Placement</th>
                                <th>Matière</th>
                                <th>partnumber</th>
                                <th>Date</th>
                                {/* <th>cuttingPlanId</th>
                                <th>Drill 1</th>
                                <th>Drill 2</th> */}
                            </tr>
                        </thead>
                        <tbody>
                            {this.state.data === null ? <tr><td colSpan='4' className='text-center'>Chargement...</td></tr>
                                : this.state.data.length === 0 ? <tr><td colSpan='4' className='text-center'>Aucun résultat</td></tr>
                                    : this.state.data.map((item, index) => (
                                        <tr key={index}>
                                            <td>{item.placement}</td>
                                            <td>{item.reftissu}</td>
                                            <td>{item.partnumber}</td>
                                            <td>{item.date}</td>
                                            {/* <td>{item.cuttingPlanId}</td>
                                            <td>{item.drill1}</td>
                                            <td>{item.drill2}</td> */}
                                        </tr>
                                    ))}
                        </tbody>
                    </table>
                </div>
            </div>
        )
    }
}
