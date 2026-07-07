import React, { Component } from 'react'
import "../../styles/StatutPlanning.scss"
import axios from 'axios'
import { BarChart, Bar, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import moment from 'moment';
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import Select from "react-select";
import { optionsShift } from '../../metadata';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faArrowRight } from '@fortawesome/free-solid-svg-icons';

export default class StatutPlanning extends Component {

    constructor(props) {
        super(props)
        this.state = {
            date: moment().add(2, 'hours').format('YYYY-MM-DD'),
			shift: this.getShift(moment().add(2, 'hours')),
            data1: [],
            data2: [],
            data3: [],
            dataRecap1: [],
            dataRecap2: [],
            dataRecap3: [],

        }
    }

    getShift(date) {
        console.log(date)
        let hour = date.hour()
        if (hour >= 0 && hour < 8) {
            return 1
        } else if (hour >= 8 && hour < 16) {
            return 2
        } else {
            return 3
        }
    }

    getData1 = () => {
        let date = null, shift = null;
        if(this.state.shift === 1) {
            date = moment(this.state.date).add(-1, 'days').format('YYYY-MM-DD')
            shift = 3
        } else {
            date = this.state.date
            shift = this.state.shift - 1
        }
        axios.get(`/api/planning/stats/${date}/${shift}`)
            .then(response => {
                let obj = {info: date + " " + shift}
                response.data.map(elem => {
                    if(elem.info === null) {
                        obj["#fcfcfa"] = elem.value1
                    } else {
                        obj[elem.info] = elem.value1
                    }
                })
                this.setState({ dataRecap1: [obj] })
            })
            .catch(error => {
                console.log(error.response.data)
            })

        axios.get(`/api/planning/stats/imported/${date}/${shift}`)
            .then(response => {
                let arr = [...response.data]
                axios.get(`/api/planning/stats/not-imported/${date}/${shift}`)
                    .then(response2 => {
                        // response.data and response2.data are arrays of objects that have properties info and value1 and value2
                        // we want to merge them into one array of objects that have properties info, value1 and value2 and value3 and value4
                        for (let i = 0; i < response2.data.length; i++) {
                            let index = arr.findIndex(elem => elem.info === response2.data[i].info)
                            if (index === -1) {
                                arr.push({
                                    info: response2.data[i].info,
                                    value1: 0,
                                    value2: 0,
                                    value3: response2.data[i].value1,
                                    value4: response2.data[i].value2
                                })
                            } else {
                                arr[index].value3 = response2.data[i].value1
                                arr[index].value4 = response2.data[i].value2
                            }
                        }
                        // we want to sort the array by date
                        arr.sort((a, b) => {
                            if (a.info < b.info) {
                                return -1
                            } else if (a.info > b.info) {
                                return 1
                            } else {
                                return 0
                            }
                        })

                        this.setState({ data1: arr })
                    })
                    .catch(error => {
                        console.log(error.response.data)
                    })

            })
            .catch(error => {
                console.log(error.response.data)
            })
    }

    getData2 = () => {
        axios.get(`/api/planning/stats/${this.state.date}/${this.state.shift}`)
            .then(response => {
                let obj = {info: this.state.date + " " + this.state.shift}
                response.data.map(elem => {
                    obj[elem.info] = elem.value1
                })
                this.setState({ dataRecap2: [obj] })
            })
            .catch(error => {
                console.log(error.response.data)
            })
        axios.get(`/api/planning/stats/imported/${this.state.date}/${this.state.shift}`)
            .then(response => {
                let arr = [...response.data]
                axios.get(`/api/planning/stats/not-imported/${this.state.date}/${this.state.shift}`)
                    .then(response2 => {
                        // response.data and response2.data are arrays of objects that have properties info and value1 and value2
                        // we want to merge them into one array of objects that have properties info, value1 and value2 and value3 and value4
                        for (let i = 0; i < response2.data.length; i++) {
                            let index = arr.findIndex(elem => elem.info === response2.data[i].info)
                            if (index === -1) {
                                arr.push({
                                    info: response2.data[i].info,
                                    value1: 0,
                                    value2: 0,
                                    value3: response2.data[i].value1,
                                    value4: response2.data[i].value2
                                })
                            } else {
                                arr[index].value3 = response2.data[i].value1
                                arr[index].value4 = response2.data[i].value2
                            }
                        }
                        // we want to sort the array by date
                        arr.sort((a, b) => {
                            if (a.info < b.info) {
                                return -1
                            } else if (a.info > b.info) {
                                return 1
                            } else {
                                return 0
                            }
                        })

                        this.setState({ data2: arr })
                    })
                    .catch(error => {
                        console.log(error.response.data)
                    })

            })
            .catch(error => {
                console.log(error.response.data)
            })
    }

    getData3 = () => {
        let date = null, shift = null;
        if(this.state.shift === 3) {
            date = moment(this.state.date).add(1, 'days').format('YYYY-MM-DD')
            shift = 1
        } else {
            date = this.state.date
            shift = this.state.shift + 1
        }

        axios.get(`/api/planning/stats/${date}/${shift}`)
            .then(response => {
                let obj = {info: date + " " + shift}
                response.data.map(elem => {
                    obj[elem.info] = elem.value1
                })
                this.setState({ dataRecap3: [obj] })
            })
            .catch(error => {
                console.log(error.response.data)
            })
        axios.get(`/api/planning/stats/imported/${date}/${shift}`)
            .then(response => {
                let arr = [...response.data]
                axios.get(`/api/planning/stats/not-imported/${date}/${shift}`)
                    .then(response2 => {
                        // response.data and response2.data are arrays of objects that have properties info and value1 and value2
                        // we want to merge them into one array of objects that have properties info, value1 and value2 and value3 and value4
                        for (let i = 0; i < response2.data.length; i++) {
                            let index = arr.findIndex(elem => elem.info === response2.data[i].info)
                            if (index === -1) {
                                arr.push({
                                    info: response2.data[i].info,
                                    value1: 0,
                                    value2: 0,
                                    value3: response2.data[i].value1,
                                    value4: response2.data[i].value2
                                })
                            } else {
                                arr[index].value3 = response2.data[i].value1
                                arr[index].value4 = response2.data[i].value2
                            }
                        }
                        // we want to sort the array by date
                        arr.sort((a, b) => {
                            if (a.info < b.info) {
                                return -1
                            } else if (a.info > b.info) {
                                return 1
                            } else {
                                return 0
                            }
                        })

                        this.setState({ data3: arr })
                    })
                    .catch(error => {
                        console.log(error.response.data)
                    })

            })
            .catch(error => {
                console.log(error.response.data)
            })
    }


    componentDidMount() {
        this.getData1()
        this.getData2()
        this.getData3()
    }

    renderBar(data) {
        if (data && data.length > 0) {
            let obj = data[0]
            let arr =[]
            for (let prop in obj) {
                if(prop !== "info") {
                    arr.push({info: prop, value1: obj[prop]})
                }
            }
            return <ResponsiveContainer width="100%" height={50}>
                <BarChart 
                    layout="vertical"
                    width={500}
                    height={300}
                    data={data || []}
                    margin={{
                        top: 0,
                        right: 0,
                        left: 0,
                        bottom: 0,
                    }}
                >
                    {/* <CartesianGrid strokeDasharray="3 3" /> */}
                    <XAxis type="number" />
                    <YAxis dataKey="info" type="category" scale="band"/>
                {arr.map((elem, index) => {
                    return <Bar key={index} name={elem.info} dataKey={elem.info} stackId="a" fill={elem.info.replace("FF", "#")} />
                })}
                </BarChart>
            </ResponsiveContainer>

        }
        return;
    }

    render() {
        let datebefore = null, shiftbefore = null, dateafter = null, shiftafter = null;
        if(this.state.shift === 1) {
            datebefore = moment(this.state.date).add(-1, 'days').format('YYYY-MM-DD')
            shiftbefore = 3
        } else {
            datebefore = this.state.date
            shiftbefore = this.state.shift - 1
        }
        if(this.state.shift === 3) {
            dateafter = moment(this.state.date).add(1, 'days').format('YYYY-MM-DD')
            shiftafter = 1
        } else {
            dateafter = this.state.date
            shiftafter = this.state.shift + 1
        }
        return (
            <div>
                <h1 className='text-center' style={{ margin: "10 0 8" }}>Statut Planning </h1>
                <div className='d-flex align-items-center mb-1 mx-2' style={{justifyContent: "center"}}>
                    <button className='btn btn-link btn-sm' onClick={() => {
                        let date = null, shift = null;
                        if(this.state.shift === 1) {
                            date = moment(this.state.date).add(-1, 'days').format('YYYY-MM-DD')
                            shift = 3
                        } else {
                            date = this.state.date
                            shift = this.state.shift - 1
                        }
                        this.setState({date, shift}, () => {
                            this.getData1()
                            this.getData2()
                            this.getData3()
                        })
                    }}><FontAwesomeIcon icon={faArrowLeft} /></button>
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
					{this.state.date && <div style={{ width: 100, margin: "0 8" }}>
						<Select classNamePrefix="rs"
							placeholder={"Date"} style={{ width: 100 }}
							isClearable={false}
							value={this.state.shift ? { label: this.state.shift, value: this.state.shift } : null}
							options={optionsShift}
							onChange={(option) => {
								this.setState({ shift: option.value })
							}}
						/>
					</div>}
                    <button className='btn btn-primary btn-sm' onClick={() => {
                        this.getData1()
                        this.getData2()
                        this.getData3()
                    }}>Rechercher</button>
                    <button className='btn btn-link btn-sm' onClick={() => {
                        let date = null, shift = null;
                        if(this.state.shift === 3) {
                            date = moment(this.state.date).add(1, 'days').format('YYYY-MM-DD')
                            shift = 1
                        } else {
                            date = this.state.date
                            shift = this.state.shift + 1
                        }
                        this.setState({date, shift}, () => {
                            this.getData1()
                            this.getData2()
                            this.getData3()
                        })
                    }}><FontAwesomeIcon icon={faArrowRight} /></button>
                </div>
                <div className='graph-header'>
                    <div className='graph-header-elem'>
                        {datebefore} {shiftbefore}
                        {this.renderBar(this.state.dataRecap1)}
                    </div>
                    <div className='graph-header-elem'>
                        {this.state.date} {this.state.shift}
                        {this.renderBar(this.state.dataRecap2)}
                    </div>
                    <div className='graph-header-elem'>
                        {dateafter} {shiftafter}
                        {this.renderBar(this.state.dataRecap3)}
                    </div>
                </div>
                <div className='graph-container'>
                    {/* <div className='graph-list'>
                        <div className='graph-elem'>
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart
                                    width={500}
                                    height={300}
                                    data={this.state.data1 || []}
                                    margin={{
                                        top: 20,
                                        right: 30,
                                        left: 20,
                                        bottom: 5,
                                    }}
                                >
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="info"/>
                                    <YAxis />
                                    <Tooltip />
                                    <Legend />
                                    <Bar name="Imported" dataKey="value1" stackId="a" fill="#8884d8" />
                                    <Bar name="Not yet" dataKey="value3" stackId="a" fill="#82ca9d" />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                        <div className='graph-elem'>
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart
                                    width={500}
                                    height={300}
                                    data={this.state.data2 || []}
                                    margin={{
                                        top: 20,
                                        right: 30,
                                        left: 20,
                                        bottom: 5,
                                    }}
                                >
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="info" />
                                    <YAxis />
                                    <Tooltip />
                                    <Legend />
                                    <Bar name="Imported" dataKey="value1" stackId="a" fill="#8884d8" />
                                    <Bar name="Not yet" dataKey="value3" stackId="a" fill="#82ca9d" />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                        <div className='graph-elem'>
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart
                                    width={500}
                                    height={300}
                                    data={this.state.data3 || []}
                                    margin={{
                                        top: 20,
                                        right: 30,
                                        left: 20,
                                        bottom: 5,
                                    }}
                                >
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="info" />
                                    <YAxis />
                                    <Tooltip />
                                    <Legend />
                                    <Bar name="Imported" dataKey="value1" stackId="a" fill="#8884d8" />
                                    <Bar name="Not yet" dataKey="value3" stackId="a" fill="#82ca9d" />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </div> */}
                    <div className='graph-list'>
                        <div className='graph-elem'>
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart 
                                    layout="vertical"
                                    width={500}
                                    height={300}
                                    data={this.state.data1 || []}
                                    margin={{
                                        top: 20,
                                        right: 30,
                                        left: 20,
                                        bottom: 5,
                                    }}
                                >
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis type="number" />
                                    <YAxis type="category" dataKey="info" />
                                    <Tooltip />
                                    <Legend />
                                    <Bar name="Imported" dataKey="value2" stackId="a" fill="#8884d8" />
                                    <Bar name="Not yet" dataKey="value4" stackId="a" fill="#82ca9d" />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                        <div className='graph-elem'>
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart
                                layout="vertical"
                                    width={500}
                                    height={300}
                                    data={this.state.data2 || []}
                                    margin={{
                                        top: 20,
                                        right: 30,
                                        left: 20,
                                        bottom: 5,
                                    }}
                                >
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis type="number" />
                                    <YAxis type="category" dataKey="info" />
                                    <Tooltip />
                                    <Legend />
                                    <Bar name="Imported" dataKey="value2" stackId="a" fill="#8884d8" />
                                    <Bar name="Not yet" dataKey="value4" stackId="a" fill="#82ca9d" />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                        <div className='graph-elem'>
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart
                                layout="vertical"
                                    width={500}
                                    height={300}
                                    data={this.state.data3 || []}
                                    margin={{
                                        top: 20,
                                        right: 30,
                                        left: 20,
                                        bottom: 5,
                                    }}
                                >
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis type="number" />
                                    <YAxis type="category" dataKey="info" />
                                    <Tooltip />
                                    <Legend />
                                    <Bar name="Imported" dataKey="value2" stackId="a" fill="#8884d8" />
                                    <Bar name="Not yet" dataKey="value4" stackId="a" fill="#82ca9d" />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </div>

                </div>
                <div className='my-2 table-data-container'>
                    <table className='table table-striped table-bordered table-data-elem'>
                        <thead className='table-data-header'>
                            <tr>
                                <th scope='col' rowSpan={3} style={{ verticalAlign: "middle" }}>Projet</th>
                                <th scope='col' colSpan={4} style={{ textAlign: "center" }}>{datebefore} {shiftbefore}</th>
                            </tr>
                            <tr>
                                <th scope='col' colSpan={2} style={{ textAlign: "center" }}>Imported</th>
                                <th scope='col' colSpan={2} style={{ textAlign: "center" }}>Not yet</th>
                            </tr>
                            <tr>
                                <th scope='col'>Ligne</th>
                                <th scope='col'>Kit</th>
                                <th scope='col'>Ligne</th>
                                <th scope='col'>Kit</th>
                            </tr>
                        </thead>
                        <tbody className='table-data-body'>
                            {this.state.data1.map((item, index) => {
                                return (
                                    <tr key={index}>
                                        <td>{item.info}</td>
                                        <td>{item.value1}</td>
                                        <td>{item.value2}</td>
                                        <td>{item.value3}</td>
                                        <td>{item.value4}</td>
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>
                    <table className='table table-striped table-bordered table-data-elem'>
                        <thead className='table-data-header'>
                            <tr>
                                <th scope='col' rowSpan={3} style={{ verticalAlign: "middle" }}>Projet</th>
                                <th scope='col' colSpan={4} style={{ textAlign: "center" }}>{this.state.date} {this.state.shift}</th>
                            </tr>
                            <tr>
                                <th scope='col' colSpan={2} style={{ textAlign: "center" }}>Imported</th>
                                <th scope='col' colSpan={2} style={{ textAlign: "center" }}>Not yet</th>
                            </tr>
                            <tr>
                                <th scope='col'>Ligne</th>
                                <th scope='col'>Kit</th>
                                <th scope='col'>Ligne</th>
                                <th scope='col'>Kit</th>
                            </tr>
                        </thead>
                        <tbody className='table-data-body'>
                            {this.state.data2.map((item, index) => {
                                return (
                                    <tr key={index}>
                                        <td>{item.info}</td>
                                        <td>{item.value1}</td>
                                        <td>{item.value2}</td>
                                        <td>{item.value3}</td>
                                        <td>{item.value4}</td>
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>
                    <table className='table table-striped table-bordered table-data-elem'>
                        <thead className='table-data-header'>
                            <tr>
                                <th scope='col' rowSpan={3} style={{ verticalAlign: "middle" }}>Projet</th>
                                <th scope='col' colSpan={4} style={{ textAlign: "center" }}>{dateafter} {shiftafter}</th>
                            </tr>
                            <tr>
                                <th scope='col' colSpan={2} style={{ textAlign: "center" }}>Imported</th>
                                <th scope='col' colSpan={2} style={{ textAlign: "center" }}>Not yet</th>
                            </tr>
                            <tr>
                                <th scope='col'>Ligne</th>
                                <th scope='col'>Kit</th>
                                <th scope='col'>Ligne</th>
                                <th scope='col'>Kit</th>
                            </tr>
                        </thead>
                        <tbody className='table-data-body'>
                            {this.state.data3.map((item, index) => {
                                return (
                                    <tr key={index}>
                                        <td>{item.info}</td>
                                        <td>{item.value1}</td>
                                        <td>{item.value2}</td>
                                        <td>{item.value3}</td>
                                        <td>{item.value4}</td>
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>

                </div>
            </div>
        )
    }
}
