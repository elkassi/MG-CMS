import { faEye, faEyeSlash, faFloppyDisk, faPrint, faTimes, faTrashCan } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import axios from 'axios';
import React, { Component } from 'react'
import ReactToPrint from "react-to-print";
import Barcode from 'react-barcode'
import logo from '../../assets/images/lear_logo.png'
import "../../styles/GammePn.scss"
import Switch from "react-switch";
import moment from 'moment';
import Select from "react-select";
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { buildGammeTextResolveParams, getGammeTextsForPiece, normalizeGammeTexts } from './gammeTextUtils';

const optionTypes = [
	{ label: "fabric", value: "fabric" },
	{ label: "supplier kit leather", value: "supplier kit leather" },
	{ label: "supplier kit fabric", value: "supplier kit fabric" },
    { label: "CNC", value: "CNC" }
	
]

class GammePn extends Component {

	constructor() {
		super();
		this.state = {
			type: "fabric",
			pn: "",
			data: [],
			nbrLigne: 3,
			modalRotate: false,
			pnInfo: {},
			heightRow: 209,
			image: null,
			imageHeight: 258,
			arrPltFound: [],
			arrPn: [],
			patternActive: false,
			gammeTechniqueTexts: [],
		}
	}

	componentDidMount() {
		if (this.props.box && this.props.box.partNumber) {
			this.searchPN(this.props.box.partNumber, null)
		}
	}

	loadGammeTechniqueTexts = (pn, pieces) => {
		return axios.get("/api/gammeTechniqueText/resolve", {
			params: buildGammeTextResolveParams(pn, pieces)
		})
			.then(res => normalizeGammeTexts(res.data || []))
			.catch(() => [])
	}

	getTextsForPiece = (piece) => {
		return getGammeTextsForPiece(this.state.gammeTechniqueTexts, piece)
	}

	renderGammeTextElements = (arr, zoom) => {
		let elements = []
		arr.forEach(elem => {
			this.getTextsForPiece(elem).forEach(text => {
				const x = parseInt((elem.x + Number(text.labelX || 0)) / zoom)
				const y = parseInt((elem.y + Number(text.labelY || 0)) / zoom)
				const fontSize = Number(text.labelSize || 24)
				const lines = String(text.content || "").split("\n")
				elements.push(
					<text
						key={`${text.clientId}-${elem.file.panelNumber}`}
						x={x}
						y={y}
						fill={text.fillColor || "#ff0000"}
						transform={text.rotation ? `rotate(${text.rotation} ${x} ${y})` : undefined}
						style={{
							fontSize,
							fontFamily: text.fontFamily || "Arial",
							fontWeight: text.fontWeight || "900",
							fontStyle: text.fontStyle || "normal",
							paintOrder: "stroke"
						}}
					>
						{lines.map((line, index) => (
							<tspan key={`${text.clientId}-line-${index}`} x={x} dy={index === 0 ? 0 : fontSize * 1.15}>
								{line}
							</tspan>
						))}
					</text>
				)
			})
		})
		return elements
	}

	searchPN = async (pn, hr) => {
		let arrPn = [], arr = [], arrPltFound = [], cncPanels = [];

		let res = await axios.get(`/api/cuttingPlan/ctc-info/${pn}/${this.state.type}`)
		let pndesc, item, itemcode5, leatherKit, supplierKit, ecnNumber, projet, image, imageHeight, pnOther, site;
		let hasCNCPanel = false; // Flag to track if this is a CNC gamme
		if (res.data && res.data.length > 0) {
			if (this.state.type !== "fabric") {
				pnOther = res.data[0].file.partNumberCover
			}
			// pn
			pndesc = res.data[0].file.partNumberCoverDesciption
			let arr1 = res.data.filter(elem => elem.file.type.toLowerCase().trim() === "fabric" && elem.file.semiFinishedGoodPartNumber != null)
			let arr2 = res.data.filter(elem => elem.file.type.toLowerCase().trim() === "supplier kit leather" && elem.file.semiFinishedGoodPartNumber != null)
			let arr3 = res.data.filter(elem => elem.file.type.toLowerCase().trim() === "supplier kit fabric" && elem.file.semiFinishedGoodPartNumber != null)
			let arrProjet = res.data.filter(elem => elem.file.projet != null)
			let arr4 = res.data.filter(elem => elem.file.ecnNumber != null)

			// Handle CNC panels for fabric type
			if (this.state.type === "fabric") {
				// Check if arr1 contains any panel ending with "CNC"
				hasCNCPanel = arr1.some(elem => elem.file.panelNumber && elem.file.panelNumber.toUpperCase().endsWith("CNC"));
				
				if (hasCNCPanel) {
					// Merge arr2 into arr1
					arr1 = [...arr1, ...arr2];
					
					// Collect all panel numbers ending with CNC from arr1 and arr2 (without the "CNC" suffix)
					const cncPanelsFromArr1 = arr1.filter(elem => elem.file.panelNumber && elem.file.panelNumber.toUpperCase().endsWith("CNC"))
						.map(elem => elem.file.panelNumber.replace(/CNC$/i, '').trim());
					const cncPanelsFromArr2 = arr2.filter(elem => elem.file.panelNumber && elem.file.panelNumber.toUpperCase().endsWith("CNC"))
						.map(elem => elem.file.panelNumber.replace(/CNC$/i, '').trim());
					
					// Combine and remove duplicates
					cncPanels = [...new Set([...cncPanelsFromArr1, ...cncPanelsFromArr2])];
				}
			}

			console.log({ arr1, arr2, arr3 })
			if (arr1.length > 0) {
				item = (this.props.box && this.props.box.item) ? this.props.box.item : arr1[0].file.semiFinishedGoodPartNumber
				itemcode5 = (this.props.box && this.props.box.item5) ? this.props.box.item5 : null
				if (itemcode5 == null) {
					if (item.startsWith("WL")) {
						itemcode5 = item.replace("WL", "YL")
					}
					if (item.startsWith("30")) {
						itemcode5 = item.replace("30", "50")
					}
					if (item.startsWith("33")) {
						itemcode5 = item.replace("33", "55")
					}
				}
			}
			if (arr2.length > 0) {
				leatherKit = arr2[0].file.semiFinishedGoodPartNumber
			}
			if (arr3.length > 0) {
				supplierKit = arr3[0].file.semiFinishedGoodPartNumber
			}
			if (arr4.length > 0) {
				ecnNumber = arr4[0].file.ecnNumber
			}
			if (arrProjet.length > 0) {
				projet = arrProjet[0].file.projet
			}
		}
		// Filter arr based on type and CNC gamme status
		// For CNC gammes (fabric type with CNC panels), include supplier kit leather pieces
		let typesToInclude = this.state.type === "fabric" 
			? (hasCNCPanel ? ["fabric", "supplier kit fabric", "supplier kit leather", "cnc"] : ["fabric", "supplier kit fabric", "cnc"])
			: [this.state.type];
		
		arr = res.data.filter(elem => typesToInclude.includes(elem.file.type.toLowerCase().trim()) && (this.state.type === "fabric" || pnOther === elem.file.partNumberCover))
			.map(elem => {
				let arrPointsXY = []
				let pointXY = ""
				if (!arrPn.includes(elem.file.partNumberMaterial)) {
					arrPn.push(elem.file.partNumberMaterial)
				}
				if (elem.graphContent === null || elem.graphContent.length === 0) {
					arrPltFound.push(elem.file.panelNumber)
				}
				elem.graphContent.map(e => {
					if (!e.includes("SI")) {
						let arrString = e.split(";")
						let puPassed = false
						arrString.map(contentElem => {
							if (contentElem.startsWith("PU")) {
								if(pointXY.length > 0) {
									arrPointsXY.push(pointXY)
									pointXY = ""
								}
								pointXY = contentElem.replace("PU", "")
							}
							if (contentElem.startsWith("PD")) {
								if (pointXY.length > 0) {
									pointXY += "," + contentElem.replace("PD", "")
								}
							}
						})
					}
				})
				if(pointXY.length > 0) {
					arrPointsXY.push(pointXY)
				}
				let minX = 9999999999, minY = 9999999999, maxX = 0, maxY = 0
				arrPointsXY.map(elemXY => {
					let arrCoords = elemXY.split(",").filter(e => !isNaN(e)).map(e => parseInt(e))

					for (let i = 0; i < (arrCoords.length - 1) / 2; i++) {
						if (arrCoords[2 * i + 1] < minX) {
							minX = arrCoords[2 * i + 1]
						}
						if (arrCoords[2 * i + 1] > maxX) {
							maxX = arrCoords[2 * i + 1]
						}
						if (arrCoords[2 * i] < minY) {
							minY = arrCoords[2 * i]
						}
						if (arrCoords[2 * i] > maxY) {
							maxY = arrCoords[2 * i]
						}
					}
				})


				elem.minX = 0
				elem.minY = 0
				elem.maxX = maxX - minX
				elem.maxY = maxY - minY
				elem.pointsXY = arrPointsXY.map(e => {
					let pointsFliped = []
					let points = e.split(",").filter(e => !isNaN(e)).map(num => parseInt(num))
					for (let i = 0; i < (points.length - 1) / 2; i++) {
						pointsFliped.push((maxX - minX) - (points[2 * i + 1] - minX))
						pointsFliped.push((maxY - minY) - (points[2 * i] - minY))
					}
					return pointsFliped.join(",")
				})

				return elem
			})
		let finalArr = [], pnInfo = {}, height = this.state.type !== "fabric" ? this.state.heightRow : 209

		if(this.props.box && this.props.box.site && this.props.box.site.length > 0) {
			site = this.props.box.site
		} else {
			let resProjet = await axios.get(`/api/projet/search?nom=${projet}`)
			if (resProjet.data != null && resProjet.data.site) {
				site = resProjet.data.site.nom
			}	
		}
			
		let resGt = await axios.get(`/api/gammeTechnique/${pn}`)
		if (resGt.data != null && resGt.data != "") {
			if(this.state.type !== "fabric") {
				if (hr != null) {
					height = hr
				} else {
					height = resGt.data.heightRow ? resGt.data.heightRow : this.state.heightRow
				}	
			} else {
				height = 209
			}
			// item = resGt.data.item ? resGt.data.item : this.state.item
			// itemcode5 = resGt.data.itemcode5 ? resGt.data.itemcode5 : this.state.itemcode5
			// leatherKit = resGt.data.leatherKit ? resGt.data.leatherKit : this.state.leatherKit
			// supplierKit = resGt.data.supplierKit ? resGt.data.supplierKit : this.state.supplierKit
			// ecnNumber = this.state.ecnNumber
			image = resGt.data.image ? resGt.data.image : this.state.image
			imageHeight = resGt.data.imageHeight ? resGt.data.imageHeight : this.state.imageHeight
		}

		arr.map(elem => {
			let oldEmpObj = (resGt.data != null && resGt.data != "") ? resGt.data.gammeTechniqueEmps.find(empObj => empObj.panelNumber === elem.file.panelNumber) : null
			elem.labelX = (oldEmpObj && oldEmpObj.labelX) ? oldEmpObj.labelX : (elem.maxX / 2) - 8
			elem.labelY = (oldEmpObj && oldEmpObj.labelY) ? oldEmpObj.labelY : elem.maxY / 2
			elem.labelSize = (oldEmpObj && oldEmpObj.labelSize) ? oldEmpObj.labelSize : 20
			elem.rotation = (oldEmpObj && oldEmpObj.rotation) ? oldEmpObj.rotation : 0
			elem.inverse = (oldEmpObj && oldEmpObj.inverse) ? oldEmpObj.inverse : false

			//update  xy positions
			if (elem.rotation != 0 || elem.inverse === true) {
				let { maxX, maxY, minY, minX } = elem
				elem.pointsXY = [...elem.pointsXY.map(pointsString => {
					let pointsFliped = []
					let points = pointsString.split(",").filter(e => !isNaN(e)).map(num => parseInt(num))

					for (let i = 0; i < (points.length - 1) / 2; i++) {
						if (elem.inverse) {
							if ((elem.rotation + 360) % 360 == 0) {
								pointsFliped.push((maxX + minX) - points[2 * i])
								pointsFliped.push(points[2 * i + 1])
							}
							if ((elem.rotation + 360) % 360 == 180) {
								pointsFliped.push(points[2 * i])
								pointsFliped.push((maxY + minY) - points[2 * i + 1])
							}
							if ((elem.rotation + 360) % 360 == 90) {
								pointsFliped.push((maxY + minY) - points[2 * i + 1])
								pointsFliped.push((maxX + minX) - points[2 * i])
							}
							if ((elem.rotation + 360) % 360 == 270) {
								pointsFliped.push(points[2 * i + 1])
								pointsFliped.push(points[2 * i])
							}
						} else {
							if ((elem.rotation + 360) % 360 == 180) {
								pointsFliped.push((maxX + minX) - points[2 * i])
								pointsFliped.push((maxY + minY) - points[2 * i + 1])
							}
							if ((elem.rotation + 360) % 360 == 90) {
								pointsFliped.push((maxY + minY) - points[2 * i + 1])
								pointsFliped.push(points[2 * i])
							}
							if ((elem.rotation + 360) % 360 == 270) {
								pointsFliped.push(points[2 * i + 1])
								pointsFliped.push((maxX + minX) - points[2 * i])
							}
						}

					}
					console.log({ points, pointsFliped })
					return pointsFliped.join(",")
				})]

				if (elem.rotation % 180 == 90) {
					elem.maxX = maxY
					elem.maxY = maxX
				}
			}
		})

		arrPn.map(pn => {
			pnInfo[pn] = { width: 0 }
			let subarr = arr.filter(elem => elem.file.partNumberMaterial === pn).sort((a, b) => b.maxY - a.maxY), newSubArr = []
			let oldPnInfo = (resGt.data != null && resGt.data != "") ? resGt.data.gammeTechniquePartNumberMaterials.find(pnObj => pnObj.partNumberMaterial.toUpperCase() === pn.toUpperCase()) : null
			pnInfo[pn].zoom = (oldPnInfo && oldPnInfo.zoom) ? oldPnInfo.zoom : Math.max(20, subarr[0].maxY / height)
			pnInfo[pn].height = height * pnInfo[pn].zoom
			let bars = [{ x: 0, y1: 0, y2: height * pnInfo[pn].zoom }]
			subarr.map(elem => {
				for (let i = 0; i < bars.length; i++) {
					if (parseInt(elem.maxY) <= parseInt(bars[i].y2 - bars[i].y1) + 1) {
						elem.x = bars[i].x
						elem.y = bars[i].y1
						break;
					}
				}

				if (pnInfo[pn].width < elem.x + elem.maxX) {
					pnInfo[pn].width = elem.x + elem.maxX
				}

				let newBars = []

				for (let k = 0; k < bars.length; k++) {
					if (bars[k].x < (elem.x + elem.maxX) && !(bars[k].y1 >= (elem.y + elem.maxY) || bars[k].y2 <= elem.y)) {
						if (bars[k].y1 < elem.y && (bars[k].y2 >= elem.y && bars[k].y2 <= (elem.y + elem.maxY))) {
							if (elem.y > bars[k].y1) { newBars.push({ x: bars[k].x, y1: bars[k].y1, y2: elem.y - 5 * pnInfo[pn].zoom }) }
						} else if (bars[k].y2 > (elem.y + elem.maxY) && (bars[k].y1 >= elem.y && bars[k].y1 <= (elem.y + elem.maxY))) {
							if ((elem.y + elem.maxY) < bars[k].y2) { newBars.push({ x: bars[k].x, y1: (elem.y + elem.maxY) + 5 * pnInfo[pn].zoom, y2: bars[k].y2 }) }
						} else if (bars[k].y2 >= (elem.y + elem.maxY) && bars[k].y1 <= elem.y) {
							if (elem.y > bars[k].y1) { newBars.push({ x: bars[k].x, y1: bars[k].y1, y2: elem.y - 5 * pnInfo[pn].zoom }) }
							if ((elem.y + elem.maxY) < bars[k].y2) { newBars.push({ x: bars[k].x, y1: (elem.y + elem.maxY) + 5 * pnInfo[pn].zoom, y2: bars[k].y2 }) }
						}
					} else {
						newBars.push(bars[k])
					}
				}
				let objBar = { x: (elem.x + elem.maxX) + 5 * pnInfo[pn].zoom, y1: 0, y2: height * pnInfo[pn].zoom }
				for (let j = 0; j < newSubArr.length; j++) {
					if (newSubArr[j].x <= (elem.x + elem.maxX) && (newSubArr[j].maxX + newSubArr[j].x) >= (elem.x + elem.maxX)) {
						if (newSubArr[j].y > (elem.y + elem.maxY)) {
							objBar.y2 = Math.min(newSubArr[j].y - 5 * pnInfo[pn].zoom, objBar.y2)
						} else if ((newSubArr[j].y + newSubArr[j].maxY) <= elem.y) {
							objBar.y1 = Math.max((newSubArr[j].y + newSubArr[j].maxY) + 5 * pnInfo[pn].zoom, objBar.y1)
						}
					}
				}
				newBars.push(objBar)
				bars = [...newBars.sort((a, b) => (a.x - b.x) || (a.y1 - b.y1))]
				newSubArr.push(elem)
			})
			finalArr = [...finalArr, ...newSubArr]
		})

		this.setState({ arrPltFound, error: null, message: null, data: finalArr, arrPn: arrPn, pnInfo, pn, pndesc, item, itemcode5, leatherKit, supplierKit, ecnNumber, site, projet, heightRow: height, image, imageHeight, pnOther, cncPanels }, () => {
			this.updatePnOrder()
			if(this.props.endLoading) {
				this.props.endLoading()
			}
		})
		// Load the extra pattern texts WITHOUT blocking print readiness: endLoading()
		// enables the "Imprimer Gammes" button, so it must not wait on /gammeTechniqueText/resolve.
		this.loadGammeTechniqueTexts(pn, finalArr).then(gammeTechniqueTexts => this.setState({ gammeTechniqueTexts }))
	
		
			
	}

	updateZoom = (zoom, pn) => {
		let finalArr = this.state.data.filter(elem => elem.file.partNumberMaterial !== pn), pnInfo = { ...this.state.pnInfo }, height = this.state.heightRow
		pnInfo[pn] = {}
		let subarr = this.state.data.filter(elem => elem.file.partNumberMaterial === pn).sort((a, b) => b.maxY - a.maxY), newSubArr = []
		pnInfo[pn].zoom = Math.max(20, zoom, subarr[0].maxY / this.state.heightRow)
		pnInfo[pn].height = this.state.heightRow * pnInfo[pn].zoom
		pnInfo[pn].width = 0
		let bars = [{ x: 0, y1: 0, y2: height * pnInfo[pn].zoom }]
		subarr.map(elem => {
			for (let i = 0; i < bars.length; i++) {
				if (parseInt(elem.maxY) <= parseInt(bars[i].y2 - bars[i].y1) + 1) {
					elem.x = bars[i].x
					elem.y = bars[i].y1
					break;
				}
			}

			if (pnInfo[pn].width < elem.x + elem.maxX) {
				pnInfo[pn].width = elem.x + elem.maxX + 5
			}

			let newBars = []

			for (let k = 0; k < bars.length; k++) {
				if (bars[k].x < (elem.x + elem.maxX) && !(bars[k].y1 >= (elem.y + elem.maxY) || bars[k].y2 <= elem.y)) {
					if (bars[k].y1 < elem.y && (bars[k].y2 >= elem.y && bars[k].y2 <= (elem.y + elem.maxY))) {
						if (elem.y > bars[k].y1) { newBars.push({ x: bars[k].x, y1: bars[k].y1, y2: elem.y - 5 * pnInfo[pn].zoom }) }
					} else if (bars[k].y2 > (elem.y + elem.maxY) && (bars[k].y1 >= elem.y && bars[k].y1 <= (elem.y + elem.maxY))) {
						if ((elem.y + elem.maxY) < bars[k].y2) { newBars.push({ x: bars[k].x, y1: (elem.y + elem.maxY) + 5 * pnInfo[pn].zoom, y2: bars[k].y2 }) }
					} else if (bars[k].y2 >= (elem.y + elem.maxY) && bars[k].y1 <= elem.y) {
						if (elem.y > bars[k].y1) { newBars.push({ x: bars[k].x, y1: bars[k].y1, y2: elem.y - 5 * pnInfo[pn].zoom }) }
						if ((elem.y + elem.maxY) < bars[k].y2) { newBars.push({ x: bars[k].x, y1: (elem.y + elem.maxY) + 5 * pnInfo[pn].zoom, y2: bars[k].y2 }) }
					}
				} else {
					newBars.push(bars[k])
				}
			}

			//the new bar
			let objBar = { x: (elem.x + elem.maxX) + 5 * pnInfo[pn].zoom, y1: 0, y2: height * pnInfo[pn].zoom }
			for (let j = 0; j < newSubArr.length; j++) {
				if (newSubArr[j].x <= (elem.x + elem.maxX) && (newSubArr[j].maxX + newSubArr[j].x) >= (elem.x + elem.maxX)) {
					if (newSubArr[j].y > (elem.y + elem.maxY)) {
						objBar.y2 = Math.min(newSubArr[j].y - 5 * pnInfo[pn].zoom, objBar.y2)
					} else if ((newSubArr[j].y + newSubArr[j].maxY) <= elem.y) {
						objBar.y1 = Math.max((newSubArr[j].y + newSubArr[j].maxY) + 5 * pnInfo[pn].zoom, objBar.y1)
					}
				}
			}
			newBars.push(objBar)
			bars = [...newBars.sort((a, b) => a.x - b.x)]
			newSubArr.push(elem)
		})
		finalArr = [...finalArr, ...newSubArr]
		this.updatePnOrder()

		this.setState({ data: finalArr, pnInfo: pnInfo, error: null, message: null })
	}

	updatePnOrder = () => {
		let widthPage = 1552;
		if (!this.state.arrPn || !this.state.pnInfo) {
			return;
		}
		let arrNewPn = []
		let counter = 0
		for (let i = 0; i < this.state.arrPn.length; i++) {
			let objArr = this.state.arrPn.filter(e => !arrNewPn.includes(e) && ((this.state.pnInfo[e].width / this.state.pnInfo[e].zoom) + 10 <= widthPage - counter))
			if (objArr.length > 0) {
				arrNewPn.push(objArr[0])
				counter += Math.max((this.state.pnInfo[objArr[0]].width / this.state.pnInfo[objArr[0]].zoom) + 10, 200)
			} else {
				objArr = this.state.arrPn.filter(e => !arrNewPn.includes(e))
				arrNewPn.push(objArr[0])
				counter = (this.state.pnInfo[objArr[0]].width / this.state.pnInfo[objArr[0]].zoom) + 10
			}
		}
		this.setState({ arrPn: arrNewPn, error: null, message: null })
	}

	updateEmpSens = (panelNumber, angle, inverse) => {
		let pn;
		let arr = this.state.data.map(elem => {
			if (elem.file.panelNumber === panelNumber) {
				pn = elem.file.partNumberMaterial
				let oldRotation = elem.rotation ? elem.rotation : 0
				let oldInverse = elem.inverse === true ? elem.inverse : false
				let { maxX, minX, maxY, minY } = elem
				elem.pointsXY = elem.pointsXY.map(e => {
					let pointsFliped = []
					let points = e.split(",").map(num => parseInt(num)).filter(num => !isNaN(num))
					for (let i = 0; i < (points.length - 1) / 2; i++) {
						if ((angle - oldRotation + 360) % 360 == 180) {
							pointsFliped.push((maxX + minX) - points[2 * i])
							pointsFliped.push((maxY + minY) - points[2 * i + 1])
						}
						if ((angle - oldRotation + 360) % 360 == 90) {
							pointsFliped.push((maxY + minY) - points[2 * i + 1])
							pointsFliped.push(points[2 * i])
						}
						if ((angle - oldRotation + 360) % 360 == 270) {
							pointsFliped.push(points[2 * i + 1])
							pointsFliped.push((maxX + minX) - points[2 * i])
						}
						if ((angle - oldRotation + 360) % 360 == 0 && inverse !== oldInverse) {
							if ((angle + 360) % 360 == 90 || (angle + 360) % 360 == 270) {
								pointsFliped.push(points[2 * i])
								pointsFliped.push((maxY + minY) - points[2 * i + 1])
							} else {
								pointsFliped.push((maxX + minX) - points[2 * i])
								pointsFliped.push(points[2 * i + 1])
							}
						}
					}
					return pointsFliped.join(",")
				})
				if ((angle - oldRotation + 360) % 180 == 90) {
					elem.maxX = maxY
					elem.minX = 0
					elem.maxY = maxX
					elem.minY = 0
					let { labelX, labelY } = elem
					elem.labelX = labelY
					elem.labelY = labelX
				}
				elem.rotation = angle
				elem.inverse = inverse
			}
			return elem
		})

		this.setState({ data: arr, error: null, message: null })
		if (pn) {
			setTimeout(this.updateZoom(this.state.pnInfo[pn].zoom, pn), 30)
		}
	}

	renderGamme = () => {
		let wd = 1545, hg = 1100
		// Fit onto ONE A4 page (794x1123 @96dpi) with a 20px margin. On this setup the print dialog's
		// "Par défaut" scale renders the gamme at ~0.67x, so pre-scale the page by PRINT_SCALE = 1.5
		// (= the "150%" that fills it manually) to fill the sheet at the DEFAULT scale. If the print
		// comes out small raise PRINT_SCALE; if it's too big / spills to 2 pages, lower it. Keep the
		// browser at 100% zoom (Ctrl+0) and the dialog scale on "Par défaut".
		const PRINT_SCALE = 1.49
		const PAGE_W = 794 * PRINT_SCALE, PAGE_H = 1123 * PRINT_SCALE, MARGIN = 20 * PRINT_SCALE
		const fitScale = Math.min((PAGE_W - 2 * MARGIN) / hg, (PAGE_H - 2 * MARGIN) / wd)
		const rotated = this.props.box ? true : this.state.modalRotate
		let centerText = {
			display: "flex",
			alignItems: "center",
			justifyContent: "center"
		}
		let { pn, pndesc, item, itemcode5, leatherKit, supplierKit, ecnNumber, projet, pnOther, site } = this.state;
		const { user } = this.props.security;

		return <div ref={input => this.componentRef = input} style={rotated ? { position: "relative", width: PAGE_W, height: PAGE_H } : { position: "relative" }}>
			<div style={rotated
				? {
					// Scale the wd x hg gamme, rotated 90deg CW, to fit one A4 page with MARGIN px all round.
					transformOrigin: "top left",
					transform: "translate(" + (MARGIN + hg * fitScale) + "px, " + MARGIN + "px) rotate(90deg) scale(" + fitScale + ")",
					border: "1px black solid",
					height: hg,
					width: wd,
					fontSize: "14px",
					padding: "15px",
					boxSizing: "border-box",
					position: "absolute", top: 0, left: 0
				}
				: {
					border: "1px black solid",
					width: wd,
					height: hg,
					fontSize: "14px",
					padding: "15px",
					position: "relative"
				}}

			>

				<div style={{ display: "flex", flexWrap: "wrap" }}>
					{this.state.arrPn && this.state.arrPn.map(pn => {
						let arr = []
						if (this.state.data) {
							arr = this.state.data.filter(elem => elem.file.partNumberMaterial === pn)
						}
						let zoom = this.state.pnInfo[pn].zoom
						return <div style={{
							border: "1px black solid",
							background: (pn.startsWith("Leather") || pn.startsWith("CNC")) ? "radial-gradient(#9e9e9e 1.5px, #d7d7d7 1.5px) 0 0 / 8px 8px #d7d7d7" : "white",
							WebkitPrintColorAdjust: "exact",
							// display: "inline-block",
							width: Math.max(190, this.state.pnInfo[pn].width / zoom) + 10
						}}>
							<span style={{ whiteSpace: "nowrap", paddingLeft: 5, fontWeight: 'bold' }}>{pn} : <input type="number" value={this.state.pnInfo[pn].zoom}
								style={{ width: 54, padding: 0, textAlign: "center", fontSize: 12 }}
								onChange={e => {
									this.setState({ pnInfo: { ...this.state.pnInfo, [pn]: { ...this.state.pnInfo[pn], zoom: e.target.value } } })
									this.updateZoom(e.target.value, pn)
								}}
								onKeyUp={(e) => {
									if (e.key === "Enter") {
										this.updateZoom(e.target.value, pn)
									}
								}}
							/>{(pn.startsWith("Leather") || pn.startsWith("CNC")) && <span style={{ paddingLeft: 6, fontWeight: 'bold' }}>type = CNC</span>}</span>
							<p style={{ margin: 0, paddingLeft: 5, fontWeight: 'bold', whiteSpace: 'nowrap', width: "100%", overflowX: "clip", fontSize: 10 }}>{arr[0].file.type && arr[0].file.type.toLowerCase().trim() === "supplier kit fabric" && "(Supplier) "} {arr[0].file.partNumberMaterialDescription}</p>
							<div style={{
								position: "relative",
								height: (this.state.pnInfo[pn].height / zoom) + 10,
								width: Math.max(190, this.state.pnInfo[pn].width / zoom) + 10,
								margin: 5
							}}>


								<svg
									style={{
										// height: (elem.maxY / zoom),width: (elem.maxX / zoom),
										height: (this.state.pnInfo[pn].height / zoom) + 10, width: Math.max(190, this.state.pnInfo[pn].width / zoom) + 10,
										position: "absolute", top: 0, left: 0 //x - (elem.maxX / zoom)
									}}
								>
									{zoom && arr.sort((a, b) => b.maxY - a.maxY).map((elem, ind) => {
										if (isNaN(parseInt((elem.x + elem.labelX) / zoom))) {
											console.log({ elem })
										}
										return <text
											x={parseInt((elem.x + elem.labelX) / zoom)}
											y={parseInt((elem.y + elem.labelY) / zoom)}
											fill="red"
											style={{ fontSize: elem.labelSize, fontWeight: 'bold' }}
										>
											{elem.file.panelNumber}
											{(elem.file.quantity != null && elem.file.quantity != "1") ? `X${elem.file.quantity}` : ""}
											{(elem.file.pattern != null && this.state.type !== "fabric" && this.state.patternActive) ? `:${elem.file.pattern}` : ""}

										</text>
									})}
									{this.renderGammeTextElements(arr, zoom)}
								</svg>

								{arr.sort((a, b) => b.maxY - a.maxY).map((elem, ind) => {
									return (elem.pointsXY.length > 0 && <svg
										style={{
											height: (elem.maxY / zoom)+1,
											width: (elem.maxX / zoom)+1,
											position: "absolute", top: elem.y / zoom, left: elem.x / zoom //x - (elem.maxX / zoom)
										}}
										className="gamme-emp"
										onClick={() => {
											if (this.state.selectedEmp !== elem.file.panelNumber) {
												this.setState({ selectedEmp: elem.file.panelNumber })
											} else {
												this.setState({ selectedEmp: null })
											}
										}}
										//right click event
										onContextMenu={(e) => {
											console.log("right click")
											e.preventDefault()
											this.setState({ selectedEmp: elem.file.panelNumber })
											setTimeout(() => {
												let selectedObj;
												if (this.state.selectedEmp) {
													selectedObj = this.state.data.find(e => e.file != null && e.file.panelNumber == this.state.selectedEmp)
												}					
												this.updateEmpSens(this.state.selectedEmp, (parseInt(selectedObj.rotation) + 90) % 360, selectedObj.inverse)	
											}, 100)
				
										}}

									>
										{elem.pointsXY.map(points => <polyline
											points={points.split(",").filter(e => !isNaN(e)).map(e => (parseInt(e) / zoom)).join(",")}
											fill="none"
											stroke="black"
											style={this.state.selectedEmp === elem.file.panelNumber ? { stroke: "blue", strokeWidth: 2 } : {}}
										/>)}
									</svg>)
								})}
							</div>
						</div>
					})}
					{this.state.image && <div style={{
						border: "1px black solid",
						// display: "inline-block",
					}}>
						<img src={`/api/file/${this.state.image}`} height={this.state.imageHeight ? Math.min(this.state.heightRow + 58, this.state.imageHeight) : this.state.heightRow} />
					</div>}
				</div>
				{(this.state.type === "fabric" || this.props.box) && <div style={{ position: "absolute", width: "calc(100% - 30px)", bottom: 0, backgroundColor: "white" }}>
					<div>
						<div style={{ display: "flex" }}>
							<div style={{ width: "33%" }}>
								{/* <div style={{ display: "flex", height: 48.39 }}>
									<div style={{ flex: 1, textAlign: "center", border: "1px black solid" }}>
										<img
											src={logo}
											alt="lear logo"
											height="40"
											onClick={() => { this.props.history.push("/"); this.setState({ menuElem: null }) }}
										/>
									</div>
									<div style={{ flex: 3, textAlign: "center", border: "1px black solid", fontWeight: "bold", fontSize: 23, lineHeight: 1 }}>Gamme Technique<br />FR PE 04</div>
								</div> */}
								<div style={{ display: "flex", height: 48.39 }}>
									<div style={{ textAlign: "center", border: "1px black solid", width: "24.4%", ...centerText, fontWeight: "bold" }}>Parte Number :</div>
									<div style={{ textAlign: "center", border: "1px black solid", width: "75.6%", padding: "1px 10px" }}>
										{this.state.type === "fabric"
											? (pn && <Barcode
												width={1.5}
												height={20}
												value={pn}
												fontSize={12}
												fontWeight="bold"
												marginBottom={0.1}
												marginTop={0.1}
												marginLeft={5}
												marginRight={5}
												format="CODE128B"
												displayValue={false}
											/>)
											: (pnOther && <Barcode
												width={1.5}
												height={20}
												value={pnOther}
												fontSize={12}
												fontWeight="bold"
												marginBottom={0.1}
												marginTop={0.1}
												marginLeft={5}
												marginRight={5}
												format="CODE128B"
												displayValue={false}
											/>)}
										<br /><span style={{ fontSize: 20, fontWeight: "900" }}>{pn ? pn : pnOther}</span>
									</div>
								</div>
								<div style={{ display: "flex", height: 53.67 }}>
									<div style={{ textAlign: "center", border: "1px black solid", width: "24.4%", ...centerText, fontWeight: "bold", fontSize: 16 }}>Description :</div>
									<div style={{ textAlign: "center", border: "1px black solid", width: "75.6%", ...centerText, fontWeight: "900", fontSize: 20, lineHeight: 1 }}>{pndesc}{this.state.arrPltFound.length > 0 && ` (ERR PLT : ${this.state.arrPltFound.join(" / ")})`}</div>
								</div>

							</div>
							<div style={{ width: "16.5%" }}>
							{(this.state.cncPanels && this.state.cncPanels.length > 0) 
								?<div style={{ textAlign: "center", height: 102, border: "1px black solid", ...centerText, fontWeight: "900", fontSize: 50, whiteSpace: "nowrap", lineHeight: 1 }}>CNC<br/>{site}</div>
								:<div style={{ textAlign: "center", height: 102, border: "1px black solid", ...centerText, fontWeight: "900", fontSize: 70, whiteSpace: "nowrap" }}>{site}</div>
							}
							</div>
							<div style={{ width: "16.5%" }}>
								<div style={{ textAlign: "center", height: 102, border: "1px black solid", ...centerText, fontWeight: "900", lineHeight: 1, fontSize: (projet && projet.length < 5) ? 80 : 55 }}>{projet === "P33BMY24" ? "P33B MY24" : projet}</div>
							</div>
							<div style={{ width: "34%" }}>
								<div style={{ display: "flex", height: 24.13 }}>
									<div className='gamme-header-elem' style={{ width: "20%" }}>ECN</div>
									<div className='gamme-header-elem' style={{ width: "80%" }}>Gamme Technique FR PE 04</div>
								</div>
								<div style={{ display: "flex", height: 24.13 }}>
									<div className='gamme-header-elem' style={{ width: "20%" }}>{ecnNumber}</div>
									{/* <div className='gamme-header-elem' style={{ flex: 1 }}>Printed By</div>
									<div className='gamme-header-elem' style={{ flex: 3 }}>{user.lastName} {user.firstName}</div> */}
									<div className='gamme-header-elem' style={{ width: "20%" }}>Date</div>
									<div className='gamme-header-elem' style={{ width: "60%" }}>{moment().format("YYYY-MM-DD,HH:mm")} {this.props.box ? `(${this.props.box.counter}/${this.props.box.total})` : ""}</div>

								</div>
								<div style={{ display: "flex", height: 53.67 }}>
									<div style={{ textAlign: "center", border: "1px black solid", width: "23.4%", ...centerText, fontWeight: "bold" }}>Cut Kit 2 (P) :</div>
									<div style={{ textAlign: "center", border: "1px black solid", width: "76.6%", padding: "1px 10px", fontWeight: "bold" }}>
										{item && <Barcode
											width={1.5}
											height={25}
											value={"P"+itemcode5}
											fontSize={12}
											marginBottom={0.1}
											marginTop={0.1}
											marginLeft={5}
											marginRight={5}
											format="CODE128B"
											displayValue={false}
										/>}
										<br /><span style={{ fontSize: 20, fontWeight: "900" }}>{itemcode5}</span>
									</div>
								</div>
							</div>
						</div>
						<div style={{ display: "flex", height: 53.67 }}>
							<div style={{ textAlign: "center", border: "1px black solid", width: "8.05%", ...centerText, fontWeight: "bold", fontSize: 16 }}>Séquence :</div>
							<div style={{ textAlign: "center", border: "1px black solid", width: "24.95%", padding: "1px 10px", fontWeight: "bold" }}>
								{itemcode5 && <Barcode
									width={1.5}
									height={25}
									value={this.props.box ? this.props.box.sequence : "123456"}
									fontSize={12}
									marginBottom={0.1}
									marginTop={0.1}
									marginLeft={5}
									marginRight={5}
									format="CODE128B"
									displayValue={false}
								/>}
								<br /><span style={{ fontSize: 20, fontWeight: "900" }}>{ this.props.box ? this.props.box.sequence : "123456"}</span>
							</div>
							{(this.state.cncPanels && this.state.cncPanels.length > 0) 
							? null
							: <div style={{ textAlign: "center", border: "1px black solid", width: "8%", ...centerText, fontWeight: "bold"}}>
								Supplier Kit :
							</div>}
							{(this.state.cncPanels && this.state.cncPanels.length > 0) ? <div style={{ textAlign: "center", border: "1px black solid", width: "33%", padding: "1px 10px", fontWeight: "bold" }}>
								<div style={{ textAlign: "center", height: 54, ...centerText, fontWeight: "900", fontSize: 35, whiteSpace: "nowrap" }}>
								{`${this.state.cncPanels.join("/")}`}
								</div>
							</div> : <div style={{ textAlign: "center", border: "1px black solid", width: "25%", padding: "1px 10px", fontWeight: "bold" }}>
								{supplierKit && <Barcode
									width={1.5}
									height={25}
									value={supplierKit}
									fontSize={12}
									marginBottom={0.1}
									marginTop={0.1}
									marginLeft={5}
									marginRight={5}
									format="CODE128B"
									displayValue={false}
								/>}
								<br /><span style={{ fontSize: 20, fontWeight: "900" }}>{supplierKit}</span>
							</div>}
							<div style={{ textAlign: "center", border: "1px black solid", width: "7.95%", ...centerText, fontWeight: "bold" }}>Leather Kit :</div>
							<div style={{ textAlign: "center", border: "1px black solid", width: "26.05%", padding: "1px 10px", fontWeight: "bold" }}>
								{leatherKit && <Barcode
									width={1.5}
									height={25}
									value={leatherKit}
									fontSize={12}
									marginBottom={0.1}
									marginTop={0.1}
									marginLeft={5}
									marginRight={5}
									format="CODE128B"
									displayValue={false}
								/>}
								<br /><span style={{ fontSize: 20, fontWeight: "900" }}>{leatherKit}</span>
							</div>

						</div>
					</div>
					<div style={{ display: "flex" }}>
						<div style={{ textAlign: "center", border: "1px black solid", width: "40%", fontWeight: "bold" }}>Cut Kit (P) :</div>
						<div style={{ textAlign: "center", border: "1px black solid", width: "20%", fontWeight: "bold" }}>Quantity (Q) :</div>
						<div style={{ textAlign: "center", border: "1px black solid", width: "20%", fontWeight: "bold" }}>Label ID (S) :</div>
						<div style={{ textAlign: "center", border: "1px black solid", width: "20%", fontWeight: "bold" }}>Work Order (H) WOID {this.props.box?.woid}</div>
					</div>
					<div style={{ display: "flex", height: 62 }}>
						<div style={{ textAlign: "center", border: "1px black solid", width: "40%", padding: "0px 10px" }}>
							{<Barcode
								width={2}
								height={30}
								value={"P" + item}
								fontSize={12}
								marginBottom={0.1}
								marginTop={1}
								marginLeft={5}
								marginRight={5}
								format="CODE128B"
								displayValue={false}
							/>}
							<br /><span style={{ fontSize: 20, fontWeight: "900" }}>{item}</span>
						</div>
						<div style={{ textAlign: "center", border: "1px black solid", width: "20%", padding: "0px 10px" }}>
							{<Barcode
								width={3}
								height={30}
								value={this.props.box ? "Q" + this.props.box.qtyBox : "Q0"}
								fontSize={12}
								marginBottom={0.1}
								marginTop={1}
								marginLeft={5}
								marginRight={5}
								format="CODE128B"
								displayValue={false}
							/>}
							<br /><span style={{ fontSize: 20, fontWeight: "900" }}>{this.props.box ? this.props.box.qtyBox : "0"}</span>
						</div>
						<div style={{ textAlign: "center", border: "1px black solid", width: "20%", padding: "0px 10px" }}>
							{<Barcode
								width={2}
								height={30}
								value={this.props.box ? "S" + this.props.box.id : "S72135648"}
								fontSize={12}
								marginBottom={0.1}
								marginTop={1}
								marginLeft={5}
								marginRight={5}
								format="CODE128B"
								displayValue={false}
							/>}
							<br /><span style={{ fontSize: 20, fontWeight: "900" }}>{this.props.box ? this.props.box.id : "72135648"}</span>
						</div>
						<div style={{ textAlign: "center", border: "1px black solid", width: "20%", padding: "0px 10px" }}>
							{<Barcode
								width={1.7}
								height={30}
								value={this.props.box ? "H" + this.props.box.wo : "H1123123"}
								fontSize={12}
								marginBottom={0.1}
								marginTop={1}
								marginLeft={5}
								marginRight={5}
								format="CODE128B"
								displayValue={false}
							/>}
							<br /><span style={{ fontSize: 20, fontWeight: "900" }}>{this.props.box ? this.props.box.wo : "1123123"}</span>
						</div>
					</div>
				</div>}
				{this.state.type !== "fabric" && <div style={{ position: "absolute", width: "calc(100% - 30px)", bottom: 0, backgroundColor: "white" }}>
					<div>
						<div style={{ display: "flex" }}>
							<div style={{ width: "33%" }}>
								{/* <div style={{ display: "flex", height: 48.39 }}>
									<div style={{ flex: 1, textAlign: "center", border: "1px black solid" }}>
										<img
											src={logo}
											alt="lear logo"
											height="40"
											onClick={() => { this.props.history.push("/"); this.setState({ menuElem: null }) }}
										/>
									</div>
									<div style={{ flex: 3, textAlign: "center", border: "1px black solid", fontWeight: "bold", fontSize: 23, lineHeight: 1 }}>Gamme Technique<br />FR PE 04</div>
								</div> */}
								<div style={{ display: "flex", height: 48.39 }}>
									<div style={{ textAlign: "center", border: "1px black solid", width: "24.4%", ...centerText, fontWeight: "bold" }}>Parte Number :</div>
									<div style={{ textAlign: "center", border: "1px black solid", width: "75.6%", padding: "1px 10px" }}>
										{this.state.type === "fabric"
											? (pn && <Barcode
												width={1.5}
												height={20}
												value={pn}
												fontSize={12}
												fontWeight="bold"
												marginBottom={0.1}
												marginTop={0.1}
												marginLeft={5}
												marginRight={5}
												format="CODE128B"
												displayValue={false}
											/>)
											: (pnOther && <Barcode
												width={1.5}
												height={20}
												value={pnOther}
												fontSize={12}
												fontWeight="bold"
												marginBottom={0.1}
												marginTop={0.1}
												marginLeft={5}
												marginRight={5}
												format="CODE128B"
												displayValue={false}
											/>)}
										<br /><span style={{ fontSize: 20, fontWeight: "900" }}>{pn ? pn : pnOther}</span>
									</div>
								</div>
								<div style={{ display: "flex", height: 53.67 }}>
									<div style={{ textAlign: "center", border: "1px black solid", width: "24.4%", ...centerText, fontWeight: "bold", fontSize: 16 }}>{this.state.type === "fabric" && "Description :"}</div>
									<div style={{ textAlign: "center", border: "1px black solid", width: "75.6%", ...centerText, fontWeight: "900", fontSize: 20, lineHeight: 1 }}>{this.state.type === "fabric" && pndesc}{this.state.arrPltFound.length > 0 && ` (ERR PLT : ${this.state.arrPltFound.join(" / ")})`}</div>
								</div>

							</div>
							<div style={{ width: "16.5%" }}>
							{(this.state.cncPanels && this.state.cncPanels.length > 0) 
								?<div style={{ textAlign: "center", height: 102, border: "1px black solid", ...centerText, fontWeight: "900", fontSize: 50, whiteSpace: "nowrap", lineHeight: 1 }}>CNC<br/>{site}</div>
								:<div style={{ textAlign: "center", height: 102, border: "1px black solid", ...centerText, fontWeight: "900", fontSize: 70, whiteSpace: "nowrap" }}>{site}</div>
							}
							</div>
							<div style={{ width: "16.5%" }}>
								<div style={{ textAlign: "center", height: 102, border: "1px black solid", ...centerText, fontWeight: "900", lineHeight: 1, fontSize: (projet && projet.length < 5) ? 80 : 55 }}>{projet === "P33BMY24" ? "P33B MY24" : projet}</div>
							</div>
							<div style={{ width: "34%" }}>
								<div style={{ display: "flex", height: 24.13 }}>
									<div className='gamme-header-elem' style={{ width: "20%" }}>ECN</div>
									<div className='gamme-header-elem' style={{ width: "80%" }}>Gamme Technique FR PE 04</div>
								</div>
								<div style={{ display: "flex", height: 24.13 }}>
									<div className='gamme-header-elem' style={{ width: "20%" }}>{ecnNumber}</div>
									{/* <div className='gamme-header-elem' style={{ flex: 1 }}>Printed By</div>
									<div className='gamme-header-elem' style={{ flex: 3 }}>{user.lastName} {user.firstName}</div> */}
									<div className='gamme-header-elem' style={{ width: "20%" }}>Date</div>
									<div className='gamme-header-elem' style={{ width: "60%" }}>{moment().format("YYYY-MM-DD,HH:mm")} {this.props.box ? `(${this.props.box.counter}/${this.props.box.total})` : ""}</div>

								</div>
								<div style={{ display: "flex", height: 53.67 }}>
									<div style={{ textAlign: "center", border: "1px black solid", width: "23.4%", ...centerText, fontWeight: "bold" }}>Séquence :</div>
									<div style={{ textAlign: "center", border: "1px black solid", width: "76.6%", padding: "1px 10px", fontWeight: "bold" }}>
										{this.props.box && this.props.box.sequence && <Barcode
											width={1.5}
											height={25}
											value={this.props.box ? this.props.box.sequence : ""}
											fontSize={12}
											marginBottom={0.1}
											marginTop={0.1}
											marginLeft={5}
											marginRight={5}
											format="CODE128B"
											displayValue={false}
										/>}
										<br /><span style={{ fontSize: 20, fontWeight: "900" }}>{this.props.box ? this.props.box.sequence : ""}</span>
									</div>
								</div>
							</div>
						</div>
						<div style={{ display: "flex", height: 53.67 }}>
							<div style={{ textAlign: "center", border: "1px black solid", width: "8.05%", ...centerText, fontWeight: "bold", fontSize: 16 }}>Cut Kit 2 :</div>
							<div style={{ textAlign: "center", border: "1px black solid", width: "24.95%", padding: "1px 10px", fontWeight: "bold" }}>
								{itemcode5 && <Barcode
									width={1.5}
									height={25}
									value={itemcode5}
									fontSize={12}
									marginBottom={0.1}
									marginTop={0.1}
									marginLeft={5}
									marginRight={5}
									format="CODE128B"
									displayValue={false}
								/>}
								<br 
								/><span style={{ fontSize: 20, fontWeight: "900" }}>{itemcode5}</span>
							</div>
							{(this.state.cncPanels && this.state.cncPanels.length > 0) 
							? null 
							: <div style={{ textAlign: "center", border: "1px black solid", width: "8%", ...centerText, fontWeight: "bold"}}>
								Supplier Kit :
							</div>}
							{(this.state.cncPanels && this.state.cncPanels.length > 0) ? <div style={{ textAlign: "center", border: "1px black solid", width: "33%", padding: "1px 10px", fontWeight: "bold" }}>
								<div style={{ textAlign: "center", height: 54, ...centerText, fontWeight: "900", fontSize: 35, whiteSpace: "nowrap" }}>
								{`${this.state.cncPanels.join("/")}`}
								</div>
							</div> : <div style={{ textAlign: "center", border: "1px black solid", width: "25%", padding: "1px 10px", fontWeight: "bold" }}>
								{supplierKit && <Barcode
									width={1.5}
									height={25}
									value={supplierKit}
									fontSize={12}
									marginBottom={0.1}
									marginTop={0.1}
									marginLeft={5}
									marginRight={5}
									format="CODE128B"
									displayValue={false}
								/>}
								<br /><span style={{ fontSize: 20, fontWeight: "900" }}>{supplierKit}</span>
							</div>}
							<div style={{ textAlign: "center", border: "1px black solid", width: "7.95%", ...centerText, fontWeight: "bold" }}>Leather Kit :</div>
							<div style={{ textAlign: "center", border: "1px black solid", width: "26.05%", padding: "1px 10px", fontWeight: "bold" }}>
								{leatherKit && <Barcode
									width={1.5}
									height={25}
									value={leatherKit}
									fontSize={12}
									marginBottom={0.1}
									marginTop={0.1}
									marginLeft={5}
									marginRight={5}
									format="CODE128B"
									displayValue={false}
								/>}
								<br /><span style={{ fontSize: 20, fontWeight: "900" }}>{leatherKit}</span>
							</div>

						</div>
					</div>
				</div>}
			</div>
		</div>
	}

	renderEmpOperation = () => {

	}

	handlePrint = async (count) => {
		for (let i = 0; i < count; i++) {
			await new Promise((resolve) => {
				console.log(this.printComponent)
				this.printComponent.print();
				setTimeout(resolve, 500); // wait for print to finish
			});
		}
	};

	renderErrorsAlert(errors) {
		let arr = [];
		if (typeof errors === 'string') {
			arr.push(<li>{errors}</li>)
		} else {
			for (let prop in errors) {
				if (typeof errors[prop] === 'string') {
					arr.push(<li>{parseInt(prop) + 1}: {errors[prop]}</li>)
				} else if (typeof errors[prop] === "object") {
					if (Object.keys(errors[prop]).length > 0) {
						arr.push(<li>{prop}: <ul>{this.renderErrorsAlert(errors[prop])}</ul></li>)
					}

				}
			}
		}
		return arr
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

	render() {
		console.log(this.state)

		let selectedObj;
		if (this.state.selectedEmp) {
			selectedObj = this.state.data.find(e => e.file != null && e.file.panelNumber == this.state.selectedEmp)
		}
		return (
			<div className='' style={this.props.box ? {} : { padding: "20 50" }}>
				{this.props.box == null && <div className='d-flex'>
					<div style={{ width: 200, margin: "0 8" }}>
						<Select
							placeholder={"Type..."}
							isClearable={false}
							value={this.state.type ? { label: this.state.type, value: this.state.type } : null}
							options={optionTypes}
							onChange={(option) => {
								if(option && option.value == "fabric") {
									this.setState({ heightRow: 209, type: option ? option.value : null, error: null, message: null })
								} else {
									this.setState({ type: option ? option.value : null, error: null, message: null })
								}
							}}
						/>
					</div>
					<input value={this.state.pn} 
						onChange={e => { 
							this.setState({ pn: e.target.value, error: null, message: null }) 
						}}
						onKeyUp={(e) => {
							if (e.key === "Enter") {
								this.searchPN(e.target.value, null)
							}
						}}
					/>
					{/* <input style={{ width: 70 }} value={this.state.heightRow} onChange={e => { this.setState({ heightRow: e.target.value, error: null, message: null }) }}
						onKeyUp={(e) => {
							if (e.key === "Enter") {
								this.searchPN(this.state.pn, this.state.heightRow)
							}
						}}
					/>

					<input style={{ width: 70 }}
						type="number" value={this.state.nbrLigne} onChange={e => { this.setState({ nbrLigne: e.target.value }) }}
					/> */}
					{ this.state.type === "fabric" ? <div style={{ fontSize: 12, padding: 8 }}>{this.state.heightRow}</div>
						: <input style={{ width: 70 }} value={this.state.heightRow} onChange={e => { this.setState({ heightRow: e.target.value, error: null, message: null }) }}
							onKeyUp={(e) => {
								if (e.key === "Enter") {
									this.searchPN(this.state.pn, this.state.heightRow)
								}
							}}
						/>}

					{this.state.type !== "fabric" && <button
						className="btn btn-outline-primary"
						onClick={() => {
							this.setState({patternActive: !this.state.patternActive})
						}}
					>{this.state.patternActive ? <FontAwesomeIcon icon={faEye} /> : <FontAwesomeIcon icon={faEyeSlash} />} Pattern</button>}
					<button type="button" className="btn btn-success" onClick={() => {
						this.setState({ loading: true, error: null, message: null })
						axios.post("/api/gammeTechnique", {
							partNumber: this.state.pn,
							description: this.state.pndesc,
							item: this.state.item,
							itemcode5: this.state.itemcode5,
							leatherKit: this.state.leatherKit,
							supplierKit: this.state.supplierKit,
							heightRow: this.state.heightRow,
							image: this.state.image,
							imageHeight: this.state.imageHeight,
							gammeTechniquePartNumberMaterials: this.state.arrPn.map(pn => {
								return {
									partNumberMaterial: pn,
									zoom: this.state.pnInfo[pn].zoom
								}
							}),
							gammeTechniqueEmps: this.state.data.map(emp => {
								return {
									panelNumber: emp.file.panelNumber,
									labelX: emp.labelX,
									labelY: emp.labelY,
									labelSize: emp.labelSize,
									rotation: emp.rotation,
									inverse: emp.inverse
								}
							})
						})
							.then(res => {
								this.setState({ loading: false, message: this.state.pn + " is saved" })
							})
							.catch(err => {
								this.setState({ loading: false, error: err.response.data })
							})
					}}><FontAwesomeIcon icon={faFloppyDisk} /></button>
					<button type="button" className="btn btn-danger" onClick={() => {
						axios.delete("/api/gammeTechnique/" + this.state.pn)
					}}><FontAwesomeIcon icon={faTrashCan} /></button>
					<ReactToPrint ref={(component) => (this.printComponent = component)}
						onBeforeGetContent={() => {
							return new Promise((resolve, reject) => {
								this.setState({ modalRotate: true, selectedEmp: null }, () => resolve())
							});
						}}
						onAfterPrint={() => { this.setState({ modalRotate: false }) }}
						onPrintError={() => { this.setState({ modalRotate: false }) }}
						trigger={() => <button
							type="button"
							className="btn btn-success ml-1"
							onClick={() => this.handlePrint(3)}
						>Imprimer <FontAwesomeIcon icon={faPrint} /></button>}
						content={() => this.componentRef}
					// pageStyle={`{ size: 10cm 22cm }`}
					// pageStyle="@page { size: 1754px 1240px; margin: 0px } @media print {html, body {height: 99%}}"
					/>
					<button onClick={() => { this.setState({ modalRotate: !this.state.modalRotate }) }}>sw</button>

					<input type="file" style={{ marginTop: 5 }}
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
									this.setState({ image: res.data })
								})
								.catch((err) => {
									console.log({ err })
								})
						}}
					/>

					<input style={{ width: 70 }}
						type="number" value={this.state.imageHeight} onChange={e => { this.setState({ imageHeight: e.target.value }) }}
					/>
					{this.state.image && <span style={{ fontSize: 12 }}
						className="btn btn-link"
						onClick={() => {
							axios(
								{
									url: `/api/file/` + this.state.image, //your url
									method: 'GET',
									responseType: 'blob', // important
								}
							).then((response) => {
								const url = window.URL.createObjectURL(new Blob([response.data]));
								const link = document.createElement('a');
								link.href = url;
								link.setAttribute('download', this.state.image); //or any other extension
								document.body.appendChild(link);
								link.click();
							})
						}}
					>
						{this.state.image}

					</span>}

					{selectedObj && <Switch id="envoyerCAD" name="envoyerCAD" checked={selectedObj.inverse || false}
						className="react-switch mt-1" offColor="#F00"
						onChange={(checked) => {
							this.updateEmpSens(this.state.selectedEmp, selectedObj.rotation || 0, checked)
						}}
					/>}
					{selectedObj && <select
						onChange={(option) => {
							this.updateEmpSens(this.state.selectedEmp, parseInt(option.target.value), selectedObj.inverse)
						}}
						value={selectedObj.rotation || 0}
						className="mr-2"
						style={{ fontSize: 14, padding: 4 }}
					>
						<option value="0">0</option>
						<option value="90">90</option>
						<option value="180">180</option>
						<option value="270">270</option>
					</select>}
					{selectedObj && [
						<input style={{ width: 70 }}
							value={selectedObj.labelX} type="number"
							onChange={(e) => {
								this.setState({
									data: this.state.data.map(elem => {
										if (elem.file.panelNumber === this.state.selectedEmp) {
											elem.labelX = e.target.value !== "" ? parseInt(e.target.value) : 0
										}
										return elem
									})
								})
							}}
						/>,
						<input style={{ width: 70 }}
							value={selectedObj.labelY} type="number"
							onChange={(e) => {
								this.setState({
									data: this.state.data.map(elem => {
										if (elem.file.panelNumber === this.state.selectedEmp) {
											elem.labelY = e.target.value !== "" ? parseInt(e.target.value) : 0
										}
										return elem
									})
								})
							}}
						/>
						,
						<input style={{ width: 70 }}
							value={selectedObj.labelSize} type="number"
							onChange={(e) => {
								this.setState({
									data: this.state.data.map(elem => {
										if (elem.file.panelNumber === this.state.selectedEmp) {
											elem.labelSize = e.target.value !== "" ? parseInt(e.target.value) : 0
										}
										return elem
									})
								})
							}}
						/>
					]}
				</div>}
				{this.renderGamme()}
				{this.renderConfirme()}
				{this.renderError()}

				{/* {this.state.arrPn && this.state.arrPn.map(pn => {
					return <div style={{ display: "flex", backgroundColor: "#efefef", margin: 5, padding: 10, fontSize: 16 }}>
						{pn} <input type="number" value={this.state.pnInfo[pn].zoom}
							style={{ width: 54, padding: 0, textAlign: "center", fontSize: 12 }}
							onChange={e => {
								this.setState({ pnInfo: { ...this.state.pnInfo, [pn]: { ...this.state.pnInfo[pn], zoom: e.target.value } } })
							}}
							onKeyUp={(e) => {
								if (e.key === "Enter") {
									this.updateZoom(e.target.value, pn)
								}
							}}
						/>
					</div>
				})} */}

			</div>
		)
	}
}

GammePn.propTypes = {
	security: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
	security: state.security
})


export default connect(mapStateToProps, {})(GammePn);
