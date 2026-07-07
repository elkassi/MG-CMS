
import * as React from 'react';
import Paper from '@mui/material/Paper';
import { GroupingState, IntegratedGrouping, ViewState } from '@devexpress/dx-react-scheduler';
import { green, lightBlue } from '@mui/material/colors';

import {
	Scheduler,
	DayView,
	Toolbar,
	DateNavigator,
	Appointments,
	TodayButton,
	Resources,
	AppointmentTooltip,
	GroupingPanel,
	CurrentTimeIndicator,
} from '@devexpress/dx-react-scheduler-material-ui';
import moment from 'moment';
import axios from 'axios';
import { ContactSupportOutlined } from '@mui/icons-material';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowRight } from '@fortawesome/free-solid-svg-icons';
import { faClock } from '@fortawesome/free-regular-svg-icons';
import { LineChart, Line, XAxis, YAxis, ReferenceLine, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';




const formatTimeScaleDate = date => moment(date).format('HH:mm:ss');

const TimeScaleLabel = (
	{ formatDate, ...restProps },
) => <DayView.TimeScaleLabel {...restProps} formatDate={formatTimeScaleDate} />;

const colors = ["#b30000", "#7c1158", "#4421af", "#1a53ff", "#0d88e6", "#00b7c7", "#5ad45a", "#ebdc78", "#e60049", "#0bb4ff", "#50e991", "#e6d800", "#8be04e", "#9b19f5", "#ffa300", "#dc0ab4", "#b3d4ff", "#00bfa0"]

const Appointment = ({
	children, style, ...restProps
}) => (
	<Appointments.Appointment
		{...restProps}
		style={{
			...style,
			backgroundColor: restProps.data.color,
			borderRadius: '8px',
		}}
	>
		{/* {children} */}
		<div style={{ color: "white", padding: 10, lineHeight: "1", fontWeight: "bold" }}>
			{restProps.data.obj.serie} <FontAwesomeIcon icon={faClock} /> {restProps.data.obj.dateDebutCoupe && moment(restProps.data.obj.dateDebutCoupe).format('HH:mm:ss')}<FontAwesomeIcon icon={faArrowRight} /> {restProps.data.obj.dateFinCoupe && moment(restProps.data.obj.dateFinCoupe).format('HH:mm:ss')}<br />
			Placement : {restProps.data.obj.placement} {restProps.data.obj.drill}<br />
			Reftissu : {restProps.data.obj.partNumberMaterial} <br />
			Sequence : {restProps.data.obj.cuttingRequest.sequence} <br />
		</div>
	</Appointments.Appointment>
);

// const AppointmentTooltipContent = ({ appointmentData, ...restProps }) => {
// 	console.log({ appointmentData, ...restProps })
// 	//test
// 	return (
//   <AppointmentTooltip.Content {...restProps}>
//     <div>
//       <div>{appointmentData && appointmentData.startDate && moment(appointmentData.startDate).format('HH:mm:ss')}</div>
//       <div>{appointmentData && appointmentData.endDate && moment(appointmentData.endDate).format('HH:mm:ss')}</div>
//     </div>
//   </AppointmentTooltip.Content>
// )}

export default class Demo extends React.PureComponent {
	constructor(props) {
		super(props);
		this.cancelTokenSource = axios.CancelToken.source();

		this.state = {
			date: moment().add(2, 'hours').format('YYYY-MM-DD'),
			data: [],
			sequenceStatus: [],
			series: [],
			colorSeqArr: {},
			chartData: []
		};
	}

	componentWillUnmount() {
		this.stopLoop(); // Stop the loop when the component is unmounted or navigating away
		this.cancelTokenSource.cancel('Component unmounted');
	}

	componentDidUpdate(prevProps) {
		if (prevProps.selectedZone !== this.props.selectedZone) {
			this.getData()
		}
	}

	startLoop() {
		if (this.loopInterval) {
			clearInterval(this.loopInterval);
		}
		this.getData()
		this.loopInterval = setInterval(() => {
			// Your loop code goes here
			this.getData()
		}, 10000);
	}

	stopLoop() {
		clearInterval(this.loopInterval);
	}

	getData = () => {
		this.cancelTokenSource.cancel('New request initiated');
		this.cancelTokenSource = axios.CancelToken.source();

		axios.get(`/api/cuttingRequestSerieLight/historique`, {
			params: {
				date: this.state.date,
				machines: this.props.machineArr.map(e => e.nom).join(",")
			},
			cancelToken: this.cancelTokenSource.token, // Pass the cancel token
		})
			.then(res => {
				let colorSeqArr = {}, colorSeq = 0; let arrSequence = [];
				let dataArr = res.data.map((serie) => {
					if (!colorSeqArr[serie.cuttingRequest.sequence]) {
						colorSeqArr[serie.cuttingRequest.sequence] = colors[colorSeq % colors.length];
						arrSequence.push(serie.cuttingRequest.sequence)
						colorSeq++;
					}
					return {
						title: serie.serie,
						startDate: serie.dateDebutCoupe,
						endDate: serie.dateFinCoupe ? serie.dateFinCoupe : moment(serie.dateDebutCoupe).add(serie.tempsDeCoupe, 'minutes').format('YYYY-MM-DDTHH:mm:ss'),
						priorityId: serie.tableCoupe,
						obj: serie,
						color: colorSeqArr[serie.cuttingRequest.sequence]
					}
				})
				this.setState({
					series: res.data,
					data: dataArr,
					colorSeqArr
				});
				this.setState({ sequenceStatus: null })
				axios.post("/api/cuttingRequestSerieLight/getSequences")
					.then(res2 => {
						this.setState({ sequenceStatus: res2.data })
						let chartData = []
						res2.data.forEach((seq) => {
							if (arrSequence.includes(seq.sequence)) {
								if (seq.dateDebutCoupe) {
									chartData.push({
										date: seq.dateDebutCoupe,
										diff: seq.totalBoxes
									})
								}
								if (seq.dateFinCoupe) {
									chartData.push({
										date: seq.dateFinCoupe,
										diff: -seq.totalBoxes
									})
								}
							}
						})
						chartData = chartData.sort((a, b) => moment(a.date).diff(moment(b.date)))
						let newChartData = []
						let total = 0
						chartData.map((e) => {
							total += e.diff

							newChartData.push({
								date: e.date,
								total: total
							})
						})
						this.setState({ chartData: newChartData })
					})
			})
	}




	render() {
		const { data } = this.state;
		if (data.length === 0) return <div>No data</div>;
		if (this.props.showChart) {
			return (
				<div style={{ height: 'calc(100% - 120px)', width: '100%' }}>
					<ResponsiveContainer width="100%" height="100%">
						<LineChart width={500} height={300} data={this.state.chartData}>
							<CartesianGrid strokeDasharray="3 3" />
							<XAxis dataKey="date" type="category" allowDuplicatedCategory={false} />
							<YAxis dataKey="total" />
							<Tooltip />
							<Legend />
							<Line dataKey="total" name="Total Boxes" />
							<ReferenceLine y={80} stroke="red" label="Max" />
						</LineChart>
					</ResponsiveContainer>
				</div>
			);
		}
		return (
			<Paper style={{ height: 'calc(100% - 120px)' }}>
				<Scheduler data={data.filter(e => (this.props.selectedZone == null || this.props.machineArr.map(e => e.nom).includes(e.priorityId)))} locale={'fr-FR'}>
					<ViewState defaultCurrentDate={this.state.date} />
					<GroupingState
						grouping={[
							{
								resourceName: 'priorityId',
							},
						]}
						groupByDate={viewName => viewName === 'Day'}
					/>

					<DayView cellDuration={5} timeScaleLabelComponent={TimeScaleLabel} />
					<Appointments
						appointmentComponent={Appointment}
					/>
					{this.props.machineArr.length > 0 && (
						<Resources
							data={[
								{
									fieldName: 'priorityId',
									title: 'Priority',
									instances: this.props.machineArr
										? this.props.machineArr.map((machine, index) => {

											return {
												text: machine.nom + ' : ' + machine.pcCoupe,
												id: machine.nom,
												// color: lightBlue,
											};
										})
										: [],
								},
							]}
							mainResourceName="priorityId"
						/>
					)}
					<IntegratedGrouping />
					<AppointmentTooltip
					// contentComponent={AppointmentTooltipContent} 
					/>
					<Toolbar />
					<DateNavigator />
					<GroupingPanel />
					<TodayButton />
					<CurrentTimeIndicator shadePreviousCells={false} />
				</Scheduler>
			</Paper>
		);
	}
}
