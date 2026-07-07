import axios from 'axios';
import React, { Component } from 'react'
import Select from "react-select";

export default class GammesTechnique extends Component {

	constructor() {
		super();
		this.state = {
			orderId: null,
			orderObj: {},
			partNumber: null,
			empList: [],
			showInd: 0,
			minX: null,
			minY: null,
			zoom: 2,
		}
	}

	renderGraph = (text, x, y, zoom) => {
		let points = []
		let pointsXY = []
		let normalPoint = []
		let specialPoint = []
		if (text.length > 0) {
			points = text.split("*")
			let pointerType = ""
			let drillType = ""
			let objPoly = {}
			let lastElem = ""
			points.map(elem => {
				if (elem.startsWith("M")) {
					pointerType = elem
					drillType = ""
					if (objPoly.listXY && objPoly.listXY.length > 0) {
						normalPoint.push({ ...objPoly })
					}
					objPoly.pointerType = elem
					objPoly.listXY = []
				} else if (elem.startsWith("D")) {
					drillType = elem
				} else if (elem.startsWith("X")) {
					if (drillType === "" || !elem.includes("M")) {
						pointsXY.push(elem.replace("X", "").split("Y").map(e => parseInt(e)))
						objPoly.listXY = [...objPoly.listXY, { x: elem.replace("X", "").split("Y")[0], y: elem.replace("X", "").split("Y")[1] }]
					} else {
						let obj = { x: elem.replace("X", "").split("Y")[0], y: elem.split("Y")[1].split("M")[0], r: elem.split("M")[1], drill: drillType }
						specialPoint.push(obj)
					}
				}
			})
			console.log({ pointsXY, normalPoint, specialPoint, x, y, zoom })
			if (pointsXY != null && pointsXY.length > 0) {

				for (let i = 0; i < pointsXY.length; i++) {
					pointsXY[i][0] = Math.round((pointsXY[i][0] - x) / zoom)
					pointsXY[i][1] = Math.round((pointsXY[i][1] - y) / zoom)
				}
			}

		}
		return [<polyline
			points={pointsXY.map(e => e.join(",")).join(",")}
			//points={pointsXY.map(e=>{return Math.round((e[0] - minX) / zoom) + ","+ Math.round((e[1] - minY) / zoom)}).join(",")}
			//renderPoint={this.myRenderPoint} 
			fill="none" stroke="black"
		/>,
		...specialPoint.map((elem, ind) => {
			if (elem.r === "43") {
				return <circle key={"circle-" + ind} cx={(parseInt(elem.x) - x) / zoom} cy={(parseInt(elem.y) - y) / zoom} r={(25) / (zoom * 2)} stroke="black" fill="none" />
			}
			return <circle key={"circle-" + ind} cx={(parseInt(elem.x) - x) / zoom} cy={(parseInt(elem.y) - y) / zoom} r={(40) / (zoom * 2)} stroke="black" fill="none" />
		})
		]
	}

	renderEmp = (text, x, y, zoom) => {
		return <svg
			// style={{ transform: "rotate(180deg) scaleX(-1)" }} 
			height={"10000"}
			width={"10000"}
		>
			{text.split("*M15*").map(elem => {
				if (elem.includes("*")) {
					return this.renderGraph("*M15*" + elem + "*M15*", x, y, zoom)
				}
			})}
		</svg>
	}

	handlePnChange = (option) => {
		this.setState({ partNumber: option ? option.value : null })
		if (option.value) {
			let placementsList = []
			this.state.orderObj.cuttingPlanMaterials.filter(elem => elem.partNumbers.split(",").includes(option.value))
				.map(elemMat => {
					elemMat.cuttingPlanMaterialPlacement.map(elemPlac => {
						placementsList.push(elemPlac.placement)
					})
				})
			axios.get(`/api/placementData/info-content/${option.value}/${placementsList.join(",")}`)
				.then(res => {
					this.setState({
						empList: res.data.map(elemEmp => {
							let points = [], pointsXY = [], normalPoint = [], specialPoint = [], maxX = null, maxY = null;
							let { zoom } = this.state
							if (elemEmp.content.length > 0) {
								points = elemEmp.content.split("*")
								let pointerType = ""
								let drillType = ""
								let objPoly = {}
								points.map(elem => {
									if (elem.startsWith("M")) {
										pointerType = elem
										drillType = ""
										if (objPoly.listXY && objPoly.listXY.length > 0) {
											normalPoint.push({ ...objPoly })
										}
										objPoly.pointerType = elem
										objPoly.listXY = []
									} else if (elem.startsWith("D")) {
										drillType = elem
									} else if (elem.startsWith("X")) {
										if (drillType === "" || !elem.includes("M")) {
											pointsXY.push(elem.replace("X", "").split("Y").map(e => parseInt(e)))
											objPoly.listXY = [...objPoly.listXY, { x: elem.replace("X", "").split("Y")[0], y: elem.replace("X", "").split("Y")[1] }]
										}
									}
								})

								if (pointsXY != null && pointsXY.length > 0) {
									let minvarX = pointsXY[0][0], minvarY = pointsXY[0][1], maxvarX = pointsXY[0][0], maxvarY = pointsXY[0][1];
									for (let i = 0; i < pointsXY.length; i++) {
										if (pointsXY[i][0] < minvarX) { minvarX = parseInt(pointsXY[i][0]); }
										if (pointsXY[i][1] < minvarY) { minvarY = parseInt(pointsXY[i][1]); }
										if (pointsXY[i][0] > maxvarX) { maxvarX = parseInt(pointsXY[i][0]); }
										if (pointsXY[i][1] > maxvarY) { maxvarY = parseInt(pointsXY[i][1]); }
									}
									elemEmp.info = {
										minX: minvarX,
										minY: minvarY,
										maxX: maxvarX,
										maxY: maxvarY,
									}
								}
								
								console.log({ pointsXY, normalPoint, specialPoint, elemEmp })
							}
							return elemEmp
						})
					})
				})
		}
	}

	render() {
		return (
			<div>
				<h1 className='text-center' style={{ marginTop: 10 }}>Gammes Technique</h1>
				<div className='d-flex'>
					<input type={"number"} className="ml-2 p-2"
						value={this.state.orderId}
						onChange={(e) => { this.setState({ orderId: e.target.value }) }}
						onKeyUp={e => {
							if (e.key === "Enter") {
								axios.get(`/api/cuttingPlan/${e.target.value}`)
									.then(res => {
										this.setState({ orderObj: res.data })
									})
							}
						}}
					/>
					{this.state.orderObj && this.state.orderObj.id && <Select id={"partNumber"} name={"partNumber"} classNamePrefix="rs"
						placeholder={"Part Number ..."} className='col-3 p-0 ml-4'
						isClearable={true}
						value={this.state.orderObj.cuttingPlanPartNumbers.map(e => { return { label: e.partNumber, value: e.partNumber, item: e } }).find(e => e.value === this.state.partNumber)}
						options={this.state.orderObj.cuttingPlanPartNumbers.map(e => { return { label: e.partNumber, value: e.partNumber, item: e } })}
						onChange={(option) => {
							this.handlePnChange(option)
						}}
					/>}
					<button type="button" className='btn btn-primary' onClick={() => { this.setState({ showInd: this.state.showInd - 1 }) }}>Prev</button>
					<button type="button" className='btn btn-primary' onClick={() => { this.setState({ showInd: this.state.showInd + 1 }) }}>Next</button>
					{this.state.showInd !== null && this.state.empList[this.state.showInd] && <input value={this.state.showInd} type="number"
						onChange={e => {
							this.setState({ showInd: parseInt(e.target.value) })
						}}
					/>}
					{this.state.showInd !== null && this.state.empList[this.state.showInd] && <input value={this.state.empList[this.state.showInd].info.minX} type="number"
						onChange={e => {
							let arr = [...this.state.empList]
							arr[this.state.showInd].info.minX = parseInt(e.target.value)
							this.setState({ empList: arr })
						}}
					/>}
					{this.state.showInd !== null && this.state.empList[this.state.showInd] && <input value={this.state.empList[this.state.showInd].info.minY} type="number"
						onChange={e => {
							let arr = [...this.state.empList]
							arr[this.state.showInd].info.minY = parseInt(e.target.value)
							this.setState({ empList: arr })
						}}
					/>}
				</div>

				<div style={{ height: "calc(100vh - 105px)", overflow: "auto" }}>
					{
						this.state.empList[this.state.showInd] && this.state.empList[this.state.showInd].content
						&& this.renderEmp(
							this.state.empList[this.state.showInd].content
							, this.state.empList[this.state.showInd].info.minX
							, this.state.empList[this.state.showInd].info.minY
							, 3
						)
					}
				</div>
			</div>
		)
	}
}
