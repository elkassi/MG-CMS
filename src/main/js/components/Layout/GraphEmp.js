import React, { Component } from 'react'

export default class GraphEmp extends Component {

	constructor() {
		super()
		this.state = {
			text: "",
			zoom: 1
		}
	}

  render() {
    return (
      <div>
        <h1>Graph EMP</h1>
				<input type="number" value={this.state.zoom} onChange={e=>{this.setState({zoom: e.target.value})}} />
        <textarea 
         	value = {this.state.text}
					onChange = {(e) => {
						this.setState({text : e.target.value})
					}} 
					rows={6}
					style={{width: "100%"}}
        />
				<div style={{ height: "calc(100vh - 200px)", overflow: "auto" }}>
				<svg
					// style={{ transform: "rotate(180deg) scaleX(-1)" }} 
					height={"10000"}
					width={"10000"}
				>
					<polyline points={this.state.text.split(",").map(e=>(parseInt(e)/this.state.zoom))} fill="none" stroke="black"/>
				</svg>
				</div>
      </div>
    )
  }
}
