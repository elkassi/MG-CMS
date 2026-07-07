import React, { Component } from 'react'
import "../../styles/PltViewer.scss"
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faGear, faMagnifyingGlassMinus, faMagnifyingGlassPlus } from '@fortawesome/free-solid-svg-icons';
import { min } from 'moment';
// import { Modal } from 'react-bootstrap'

export default class Placement extends Component {

    constructor(props) {
        super(props)
        this.state = {
            zoom: 140,
            file: null,
            fileText: [],
            elem: {},
            viewBox: { x: 0, y: 0, width: window.innerWidth, height: window.innerHeight }, // Initial viewBox state
            showCalibrageModal: false,
            calibrageNumber: null,
            margeLeft: 20,
            margeRight: 20,
            extraOverlap: 10,
            overlapDistance: null,
            zoomLevel: 100, // Default zoom level
            defaultZoom: null,
        };
    }

    componentDidMount() {
        this.setTheDragAndDrop();
        this.checkZoom();
        window.addEventListener('resize', this.checkZoom); // Listen for resize events
        // if (localStorage.getItem("calibrageZoomPlt")) {
        this.setState({ zoom: 140 })
        // } else {
        //     this.setState({ showCalibrageModal: true })
        // }
    }

    componentWillUnmount() {
        window.removeEventListener('resize', this.checkZoom); // Cleanup on unmount
    }

    checkZoom = () => {
        const innerWidth = window.innerWidth;
        const outerWidth = window.outerWidth;

        // Subtract a fixed value for Edge to account for UI elements
        const edgeAdjustment = navigator.userAgent.indexOf("Edg/") !== -1 ? 2 : 0; // Adjust as needed

        // Calculate zoom level, adjusting outerWidth for Edge
        const adjustedOuterWidth = outerWidth - edgeAdjustment;
        const zoomLevel = Math.round((adjustedOuterWidth / innerWidth) * 100);

        // Set the zoom level, ensuring it's at least 100%
        this.setState({ zoomLevel: zoomLevel }, () => {
            console.log(`Current zoom level: ${this.state.zoomLevel}% nagigator userAgent: ${navigator.userAgent.indexOf("Edg/")}`);
        });

    }

    convertFloat = (float, num = 4) => {
        //check if the float is a number
        if (!float || isNaN(float)) {
            return float
        }
        return parseFloat(parseFloat(float).toFixed(num))
    }


    renderCalibrageForm = () => {
        return <div className='calibrage-container' style={this.state.showCalibrageModal ? { height: "150" } : { height: "0" }}>
            <div className='calibrage-content'>
                <h3 className='text-center'>CALIBRAGE PLT : mesurer la distance en centimètre</h3>
                {this.renderSVGLineToMesure()}
                <div className='d-flex justify-content-center'>
                    <input className='form-control' style={{ width: "200px" }}
                        ref={input => this.calibrageNumberInput = input}
                        type="number" value={this.state.calibrageNumber}
                        onChange={(e) => this.setState({ calibrageNumber: e.target.value })}
                    />
                    <button className='btn btn-primary ml-2' onClick={() => {
                        localStorage.setItem("calibrageZoomPlt", this.convertFloat(this.state.calibrageNumber))
                        this.setState({ zoom: this.convertFloat(this.state.calibrageNumber), showCalibrageModal: false })
                    }}>Valider en centimètre</button>
                </div>
            </div>
        </div>
    }

    renderSVGLineToMesure = () => {
        let pltLineText = "SP1;PU0,3937,100,3937,100,0,0,0;PD0,3937;";
        let arrPointsXY = [];
        let pointXY = "";
        if (!pltLineText.includes("SI")) {
            let arrString = pltLineText.split(";");
            arrString.map(contentElem => {
                if (contentElem.startsWith("PU")) {
                    if (pointXY.length > 0) {
                        arrPointsXY.push(pointXY);
                        pointXY = "";
                    }
                    pointXY = contentElem.replace("PU", "");
                }
                if (contentElem.startsWith("PD")) {
                    if (pointXY.length > 0) {
                        pointXY += "," + contentElem.replace("PD", "");
                    }
                }
            });
        }
        if (pointXY.length > 0) {
            arrPointsXY.push(pointXY);
        }

        let minX = 9999999999, minY = 9999999999, maxX = 0, maxY = 0;
        arrPointsXY.map(elemXY => {
            let arrCoords = elemXY.split(",").filter(e => !isNaN(e)).map(e => parseInt(e));
            for (let i = 0; i < (arrCoords.length - 1) / 2; i++) {
                if (arrCoords[2 * i + 1] < minX) {
                    minX = arrCoords[2 * i + 1];
                }
                if (arrCoords[2 * i + 1] > maxX) {
                    maxX = arrCoords[2 * i + 1];
                }
                if (arrCoords[2 * i] < minY) {
                    minY = arrCoords[2 * i];
                }
                if (arrCoords[2 * i] > maxY) {
                    maxY = arrCoords[2 * i];
                }
            }
        });

        let elem = {};
        elem.minX = 0;
        elem.minY = 0;
        elem.maxX = maxX - minX;
        elem.maxY = maxY - minY;
        elem.pointsXY = arrPointsXY.map(e => {
            let pointsFliped = [];
            let points = e.split(",").filter(e => !isNaN(e)).map(num => parseInt(num));
            for (let i = 0; i < (points.length - 1) / 2; i++) {
                pointsFliped.push((maxX - minX) - (points[2 * i + 1] - minX));
                pointsFliped.push((maxY - minY) - (points[2 * i] - minY));
            }
            return pointsFliped.join(",");
        });

        // SVG dimensions
        const svgWidth = window.innerWidth; // or any width you desire
        const svgHeight = 30; // or any height you desire

        // Polyline width and height
        const polylineWidth = maxX - minX;
        const polylineHeight = maxY - minY;

        // Calculate offsets to center the polyline
        const offsetX = (svgWidth - (polylineWidth / 10)) / 2;  // scale the polyline by 10
        const offsetY = (svgHeight - (polylineHeight / 10)) / 2;

        return (
            <svg
                style={{ height: svgHeight, width: svgWidth, }}
                viewBox={`0 0 ${svgWidth} ${svgHeight}`}
            >
                <polyline
                    points={elem.pointsXY[0]
                        .split(",")
                        .filter(e => !isNaN(e))
                        .map(e => (parseInt(e) / 10))
                        .join(",")}
                    fill="black"
                    stroke="black"
                    transform={`translate(${offsetX}, ${offsetY})`}
                />
            </svg>
        );
    }


    loadingFile = (e) => {
        this.setState({
            file: null,
            defaultZoom: null,
            fileText: [],
            elem: {},
            viewBox: { x: 0, y: 0, width: window.innerWidth, height: window.innerHeight }, // Initial viewBox state
        })
        let reader = new FileReader();
        let file = e.target.files[0];

        reader.onload = (event) => {
            const text = event.target.result;
            const lines = text.split(/\r?\n/); // Split by line breaks (both Unix and Windows)

            let objEmp = {}, numberEmp = 0

            /*
            line contains inside this : 
            ...
L,30
P,37706,53570,0,0
D,1,HCB-N-LAT1-00-10-02-2022-A
D,2,X1
D,3,L1
D,4,1
D,5,I
D,7,L
L,29 
...
and we need to extract the data from it
            */
            lines.map(line => {
                //line starts with L and contains a number
                if (line.startsWith("L") && !isNaN(line.split(",")[1])) {
                    objEmp[line.split(",")[1]] = {}
                    numberEmp = line.split(",")[1]
                }
                //line starts with P and contains 4 numbers
                if (line.startsWith("P") && line.split(",").length >= 3) {
                    let arrPosition = line.split(",")
                    objEmp[numberEmp].position = { x: parseInt(arrPosition[1]), y: parseInt(arrPosition[2]) }
                }
                //line starts with D and contains 2 numbers
                if (line.startsWith("D") && line.split(",").length === 3) {
                    objEmp[numberEmp]["info-" + line.split(",")[1]] = line.split(",")[2]
                }

            })

            let perimetre = 0
            let pointsData = lines[0].substring(lines[0].indexOf("*N1") + 2, lines[0].indexOf("*Q"))
            let longestEmp = 0, longueur = 0, largeur = 0, nbrPiece = 0, detailEmps = []
            pointsData.split("*N").map(points => {
                let empNumber = points.split("*")[0]
                nbrPiece++;
                let minX = Infinity, minY = Infinity, maxX = 0, maxY = 0;
                let pointsXY = ""
                let arrPointsXYMini = []
                points.split("*M15*").map(segments => {
                    let pointerType = "", drillType = "";
                    let lastX = null, lastY = null;
                    segments.split("*").map(elem => {
                        if (elem.startsWith("M")) {
                            pointerType = elem;
                            drillType = "";
                        }
                        if (elem.startsWith("D")) {
                            drillType = elem;
                        }
                        if (elem.startsWith("X")) {
                            if (drillType === "" || !elem.includes("M")) {
                                if (lastX !== null && lastY !== null) {
                                    perimetre += Math.hypot(parseInt(elem.replace("X", "").split("Y")[0]) - lastX, parseInt(elem.replace("X", "").split("Y")[1]) - lastY);
                                }
                                lastX = parseInt(elem.replace("X", "").split("Y")[0]);
                                lastY = parseInt(elem.replace("X", "").split("Y")[1]);
                                if (pointsXY.length > 0) {
                                    pointsXY += ","
                                }
                                pointsXY += elem.replace("X", "").split("Y")[0] + "," + elem.replace("X", "").split("Y")[1]
                            }
                            let x = parseInt(elem.substring(1).split("Y")[0]);
                            if (x < minX) {
                                minX = x;
                            }
                            if (x > maxX) {
                                maxX = x;
                            }
                            let y = parseInt(elem.substring(1).split("Y")[1]);
                            if (y < minY) {
                                minY = y;
                            }
                            if (y > maxY) {
                                maxY = y;
                            }
                        }
                    })
                    if (pointsXY.length > 0) {
                        // arrPointsXY.push({
                        //     pointsXY, minX, minY, maxX, maxY
                        // })
                        arrPointsXYMini.push(pointsXY)
                    }
                })
                if (maxX - minX > longestEmp) {
                    longestEmp = maxX - minX
                }
                longueur = Math.max(longueur, maxX)
                largeur = Math.max(largeur, maxY)
                detailEmps.push({ empNumber, minX, minY, maxX, maxY, arrPointsXYMini, objEmp: objEmp[parseInt(empNumber)] })
            })
            let elem = {}
            elem.minX = 0
            elem.minY = 0
            elem.maxX = longueur
            elem.maxY = largeur
            elem.longueur = longueur
            elem.largeur = largeur
            elem.perimetre = perimetre
            elem.longestEmp = longestEmp
            elem.nbrPiece = nbrPiece
            elem.detailEmps = detailEmps
            //we need to change zoom so that the svg can be displayed  the maximum possible and also to be able to see the whole svg
            let screenWidth = window.innerWidth - 50
            let screenHeight = window.innerHeight - 282 // 232 is the height of the header

            let zoom = this.convertFloat(Math.max(longueur / screenWidth, largeur / screenHeight), 1)

            this.setState({
                defaultZoom: zoom,
                zoom,
                file,
                fileText: lines,
                elem,
                viewBox: { x: 0, y: 0, width: window.innerWidth, height: window.innerHeight }, // Reset viewBox after file load
            });


            setTimeout(() => {
                // this.moveToPosition(-18, -75)
                this.centerSVG()
            }, 50)
        };

        if (file) {
            reader.readAsText(file); // Start reading the file
        }

    }

    // Function to move SVG to a specific position
    moveToPosition = (x, y) => {
        // Update viewBox in state
        this.setState((prevState) => ({
            viewBox: { ...prevState.viewBox, x, y }
        }), () => {
            const svg = document.getElementById('my-svg');
            svg.setAttribute("viewBox", `${this.state.viewBox.x} ${this.state.viewBox.y} ${this.state.viewBox.width} ${this.state.viewBox.height}`);
        });
    }


    // Original drag-and-drop function (unchanged)
    setTheDragAndDrop = () => {
        const svg = document.getElementById('my-svg');
        let isDragging = false;
        let startX, startY;

        // Set the initial viewBox
        svg.setAttribute("viewBox", `${this.state.viewBox.x} ${this.state.viewBox.y} ${this.state.viewBox.width} ${this.state.viewBox.height}`);

        svg.addEventListener('mousedown', (e) => {
            isDragging = true;
            startX = e.clientX;
            startY = e.clientY;
        });

        window.addEventListener('mousemove', (e) => {
            if (!isDragging) return;

            const dx = (startX - e.clientX); // Calculate horizontal movement
            const dy = (startY - e.clientY); // Calculate vertical movement

            // Update viewBox based on movement
            this.setState((prevState) => {
                const newViewBox = {
                    x: prevState.viewBox.x + dx,
                    y: prevState.viewBox.y + dy,
                    width: prevState.viewBox.width,
                    height: prevState.viewBox.height,
                };
                // Set the new viewBox on the SVG
                svg.setAttribute("viewBox", `${newViewBox.x} ${newViewBox.y} ${newViewBox.width} ${newViewBox.height}`);
                return { viewBox: newViewBox };
            });

            startX = e.clientX;
            startY = e.clientY;
        });

        window.addEventListener('mouseup', () => {
            isDragging = false;
        });
    }

    // Example button click handler
    onButtonClick = () => {
        const newX = 100; // Set your desired x position
        const newY = 50;  // Set your desired y position
        this.moveToPosition(newX, newY); // Call the function to move the SVG
    }

    centerSVG = () => {
        const svg = document.getElementById('my-svg');
        const polylineElements = svg.getElementsByTagName('polyline');

        if (polylineElements.length === 0) return; // No polyline to center

        let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;

        // Loop through each polyline to find the bounding box
        Array.from(polylineElements).forEach(polyline => {
            const points = polyline.getAttribute('points').trim().split(" ");
            points.forEach(point => {
                const [x, y] = point.split(",").map(Number);
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            });
        });

        // Calculate the center position based on the bounding box
        const centerX = (minX + maxX) / 2;
        const centerY = (minY + maxY) / 2;

        // Calculate new viewBox positions
        const width = maxX - minX;
        const height = maxY - minY;

        // Adjust the viewBox to center the polylines
        // this.moveToPosition((width - this.state.viewBox.width / 2), (height - this.state.viewBox.height / 2));
        // this.moveToPosition(Math.min((width - this.state.viewBox.width) / 2, -18), Math.min(((height - this.state.viewBox.height) / 2), -75));
        this.moveToPosition(Math.min((width - this.state.viewBox.width) / 2, -18), -25);

    }

    calculateOverlapNeeded = (overlapDistance) => {
        let empArr = []
        let smallestX = 9999999999
        let axisOverlap = (overlapDistance - this.state.margeLeft) * 3937 / 1000
        this.state.elem.detailEmps.map(e => {
            e.arrPointsXYMini.map((elem, index) => {
                let points = elem.split(",").filter(e => !isNaN(e)).map(num => parseInt(num))
                for (let i = 0; i < (points.length - 1) / 2; i++) {
                    let currentX = points[2 * i]
                    let nextX = points[2 * i + 2]
                    if (2 * i === points.length - 2) {
                        nextX = points[0]
                    }
                    //check if axisOverlap is between currentX and nextX
                    if ((currentX <= axisOverlap && axisOverlap <= nextX) || (nextX <= axisOverlap && axisOverlap <= currentX)) {
                        empArr.push(e)
                        break;
                    }
                }
            })
        })
        empArr.map(e => {
            if (e.minX < smallestX) {
                smallestX = e.minX
            }
        })
        this.setState({ overlapNeeded: axisOverlap - smallestX, smallestX, axisOverlap })
    }

    renderHeader = () => {
        return <div style={{ margin: 0, backgroundColor: "grey", zIndex: 2, position: 'absolute', bottom: 0, width: "100%" }}>
            <div className='d-flex' style={{ justifyContent: "center", alignItems: "center", padding: 5 }}>
                <span className='mr-2' style={{ fontSize: 28 }}>Placement Viewer</span>
                {/* <button className='btn btn-danger' style={{ fontSize: 20, marginLeft: "10px" }} onClick={() => {
                    this.setState({ showCalibrageModal: true, calibrageNumber: localStorage.getItem("calibrageZoomPlt") })
                    this.calibrageNumberInput.focus()
                    this.calibrageNumberInput.select()
                }}>
                    <FontAwesomeIcon icon={faGear} /> Calibrer
                </button> */}
                {/* <div style={{ width: "150px", marginRight: "10px" }}>
                    <input className='form-control' style={{ fontSize: 20 }}
                        type="number" value={this.state.zoom}
                        onChange={(e) => this.setState({ zoom: e.target.value })}
                    />
                </div> */}
                {this.state.elem.detailEmps && this.state.elem.detailEmps.length > 0 && <button className='btn btn-dark' style={{ fontSize: 20, marginLeft: "10px" }} onClick={() => {
                    this.setState({ zoom: this.state.zoom + 10 })
                }}>
                    <FontAwesomeIcon icon={faMagnifyingGlassMinus} />
                </button>}
                {this.state.elem.detailEmps && this.state.elem.detailEmps.length > 0 && <button className='btn btn-dark' style={{ fontSize: 20, marginLeft: "10px" }} onClick={() => {
                    this.setState({ zoom: this.state.zoom - 10 })
                }}>
                    <FontAwesomeIcon icon={faMagnifyingGlassPlus} />
                </button>}

                {this.state.elem.detailEmps && this.state.elem.detailEmps.length > 0 && <button className='btn btn-dark' style={{ fontSize: 20, marginLeft: "10px" }} onClick={() => {
                    this.setState({ zoom: this.state.defaultZoom })
                    setTimeout(() => {
                        this.centerSVG()
                    }, 50)
                }}
                    disabled={this.state.defaultZoom === this.state.zoom}
                >
                    Voir tout
                </button>}
                {this.state.elem.detailEmps && this.state.elem.detailEmps.length > 0 && <button className='btn btn-dark' style={{ fontSize: 20, marginLeft: "10px" }} onClick={() => {
                    this.centerSVG()
                    // this.moveToPosition(-18, -75)
                }}>
                    Centrer
                </button>}
                <input type="file" style={{ width: "40%", fontSize: 20, marginLeft: "10px" }} onChange={(e) => this.loadingFile(e)} />
            </div>
            {this.state.elem.maxX > 0 && this.state.elem.maxY > 0
                && <div className='d-flex' style={{ justifyContent: "center", alignItems: "center", padding: 5 }}>
                    <span className='header-info-bubble'>Longueur: {this.convertFloat((this.state.elem.longueur * 1000 / 3937) + (this.state.margeLeft || 0) + (this.state.margeRight || 0), 0)} mm</span>
                    <span className='header-info-bubble'>Largeur: {this.convertFloat(this.state.elem.largeur * 1000 / 3937, 0)} mm</span>
                    <span className='header-info-bubble'>Perimetre: {this.convertFloat(this.state.elem.perimetre * 1000 / 3937, 0)} mm</span>
                    <span className='header-info-bubble'>Emp la plus longue: {this.convertFloat(this.state.elem.longestEmp * 1000 / 3937, 1)} mm</span>
                    <span className='header-info-bubble'>Nombre de pièces: {this.state.elem.nbrPiece}</span>
                </div>}
            {this.state.elem.maxX > 0 && this.state.elem.maxY > 0
                //we put here there input for margeLeft and margeRight and overlapDistance to caluculate the value of overlapNeeded
                && <div>
                    <div className='d-flex' style={{ justifyContent: "center", alignItems: "center", padding: 5, backgroundColor: "#00129c" }}>
                        <div className='d-flex' style={{ justifyContent: "center", alignItems: "center" }}>
                            <span className='mr-2' style={{ fontSize: 20 }}>Marge Gauche</span>
                            <div style={{ width: "150px", marginRight: "10px" }}>
                                <input className='form-control' style={{ fontSize: 20 }}
                                    type="number" value={this.state.margeLeft}
                                    onChange={(e) => this.setState({ margeLeft: e.target.value ? parseInt(e.target.value) : 0 })}
                                />
                            </div>
                        </div>
                        <div className='d-flex' style={{ justifyContent: "center", alignItems: "center" }}>
                            <span className='mr-2' style={{ fontSize: 20 }}>Marge Droite</span>
                            <div style={{ width: "150px", marginRight: "10px" }}>
                                <input className='form-control' style={{ fontSize: 20 }}
                                    type="number" value={this.state.margeRight}
                                    onChange={(e) => this.setState({ margeRight: e.target.value ? parseInt(e.target.value) : 0 })}
                                />
                            </div>
                        </div>
                        <div className='d-flex' style={{ justifyContent: "center", alignItems: "center" }}>
                            <span className='mr-2' style={{ fontSize: 20 }}>Extra overlap</span>
                            <div style={{ width: "150px", marginRight: "10px" }}>
                                <input className='form-control' style={{ fontSize: 20 }}
                                    type="number" value={this.state.extraOverlap}
                                    onChange={(e) => this.setState({ extraOverlap: e.target.value ? parseInt(e.target.value) : 0 })}
                                />
                            </div>
                        </div>
                        <div className='d-flex' style={{ justifyContent: "center", alignItems: "center" }}>
                            <span className='mr-2' style={{ fontSize: 20 }}>Overlap Distance</span>
                            <div style={{ width: "150px", marginRight: "10px" }}>
                                <input className='form-control' style={{ fontSize: 20 }}
                                    type="number" value={this.state.overlapDistance}
                                    onChange={(e) => {
                                        this.setState({ overlapDistance: e.target.value ? parseInt(e.target.value) : 0 })
                                        if (e.target.value) {
                                            this.calculateOverlapNeeded(e.target.value ? parseInt(e.target.value) : 0)
                                        }
                                    }}
                                />
                            </div>
                        </div>
                    </div>
                    {this.state.overlapNeeded && <div className='d-flex' style={{ justifyContent: "center", alignItems: "center", padding: 5, backgroundColor: "#00129c" }}>
                        <span className='header-info-bubble'>
                            Overlap Needed: {this.convertFloat((this.state.overlapNeeded * 1000 / 3937) + ((this.state.extraOverlap) || 0), 1)} mm
                        </span>
                    </div>}
                </div>
            }
        </div>
    }



    showElem = () => {
        let { elem, zoom } = this.state
        return (<svg
            id="my-svg"
            style={{
                height: "100vh",
                width: "100vw",
                position: "absolute",
                top: 0, left: 0, //x - (elem.maxX / zoom)
                backgroundColor: "white",
                // make it 
            }}
            className="gamme-emp"
            onClick={() => {
            }}
            //right click event
            onContextMenu={(e) => {
                console.log("right click")
                e.preventDefault()
            }}

        >
            {elem.maxX > 0 && elem.maxY > 0 &&
                <polyline
                    points={"0,0,0," + elem.maxY / zoom + "," + elem.maxX / zoom + "," + elem.maxY / zoom + "," + elem.maxX / zoom + ",0,0,0"}
                    fill="none"
                    stroke="red"
                    style={{}}
                />}

            {elem.detailEmps && elem.detailEmps.length > 0 && elem.detailEmps.map(elem => {
                return elem.arrPointsXYMini.map((points, index) => {
                    return <polyline
                        key={index}  // Add a unique key for each polyline
                        points={points.split(",").filter(e => !isNaN(e)).map(e => (parseInt(e) / zoom)).join(",")}
                        fill="none"
                        stroke="black"
                        // style={{ cursor: 'pointer' }}  
                        // onMouseEnter={(e) => e.target.setAttribute("stroke", "blue")}  // Change stroke to blue on hover
                        // onMouseLeave={(e) => e.target.setAttribute("stroke", "black")} // Revert stroke to black when mouse leaves
                        // onClick={() => window.location.href = '/your-target-page'}  // Redirect on click
                    />
                })
            })}


            {elem.detailEmps && elem.detailEmps.length > 0 && elem.detailEmps.map(elem => {
                // return the number of the emp in the center of the emp using the average of the x and y
                let x = (elem.maxX + elem.minX) / 2
                let y = (elem.maxY + elem.minY) / 2
                return <text x={x / zoom} y={y / zoom} fill="black" fontSize="14" textAnchor="middle">{elem.empNumber}</text>
            })}


            {// this is just a vertical axis
                this.state.axisOverlap &&
                <polyline
                    points={this.state.axisOverlap / zoom + ",-10," + this.state.axisOverlap / zoom + "," + (((elem.maxY) / zoom) + 10)}
                    fill="none"
                    stroke="blue"
                    style={{}}
                    stroke-width="2"
                />
            }
            {this.state.smallestX &&
                <polyline
                    points={this.state.smallestX / zoom + ",-10," + this.state.smallestX / zoom + "," + (((elem.maxY) / zoom) + 10)}
                    fill="none"
                    stroke="green"
                    style={{}}
                    stroke-width="2"
                />
            }
        </svg>)

    }

    // renderModal = () => {
    //     return <Modal
    //         show={this.state.showEmpDetail}
    //         onHide={() => this.setState({ showEmpDetail: false })}
    //         dialogClassName="modal-90w"
    //         centered
    //     >
    //         {this.state.showEmpDetail && <div style={{ height: "90vh", overflowY: 'auto' }}>
    //             <table>
                    
    //             </table>
    //         </div>}
    //     </Modal>
    // }


    render() {
        if (this.state.zoomLevel <= 99 || this.state.zoomLevel >= 101) {
            return <div style={{
                backgroundColor: "black",
                display: "flex",
                justifyContent: "center",
                alignItems: "center",
                height: "100%",
                width: "100%",
            }}>
                <h1 className='text-center' style={{ color: "white" }}>Zoom Level: {this.state.zoomLevel}%</h1>
            </div>
        }
        return (
            <div style={{
                height: "100vh",
                color: "white"
            }}>
                {this.renderHeader()}
                {this.showElem()}
                {this.renderCalibrageForm()}
            </div>
        )
    }
}