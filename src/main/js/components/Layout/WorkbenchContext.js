import React, { Component, createContext } from 'react';
import axios from 'axios';
import moment from 'moment';

/**
 * Compute current plant shift matching the backend ShiftClock logic.
 * Shift 1 (night):  21:50 → 05:50
 * Shift 2 (morning): 05:50 → 13:50
 * Shift 3 (afternoon): 13:50 → 21:50
 */
function computeCurrentShift() {
    const now = moment();
    const minutes = now.hour() * 60 + now.minute();
    // 21:50 = 1310, 05:50 = 350, 13:50 = 830
    if (minutes >= 1310) return 1; // late evening — shift 1 of next day
    if (minutes < 350) return 1;   // early morning — still shift 1
    if (minutes < 830) return 2;   // morning
    return 3;                       // afternoon
}

export const WorkbenchContext = createContext({
    data: null,
    loading: false,
    error: null,
    date: '',
    shift: 1,
    setDate: () => {},
    setShift: () => {},
    refresh: () => {},
    reloadAll: () => {},
});

export class WorkbenchProvider extends Component {
    constructor(props) {
        super(props);
        const today = moment().format('YYYY-MM-DD');
        this.state = {
            data: null,
            loading: false,
            error: null,
            date: today,
            shift: computeCurrentShift(),
            pollInterval: null,
        };
    }

    componentDidMount() {
        this.loadData();
    }

    componentWillUnmount() {
        this.stopPolling();
    }

    componentDidUpdate(prevProps, prevState) {
        if (prevState.date !== this.state.date || prevState.shift !== this.state.shift) {
            this.loadData();
        }
    }

    startPolling = (engineState) => {
        this.stopPolling();
        const newState = engineState?.state;
        const isRunning = newState && ['WARMING', 'IMPROVING', 'PAUSED'].includes(newState);
        const intervalMs = isRunning ? 5000 : 15000;

        // Immediate reload on transition from not-running to running
        const wasRunning = this.lastEngineState && ['WARMING', 'IMPROVING', 'PAUSED'].includes(this.lastEngineState);
        if (isRunning && !wasRunning) {
            this.loadData(true);
        }
        this.lastEngineState = newState;

        const interval = setInterval(() => this.loadData(true), intervalMs);
        this.setState({ pollInterval: interval });
    };

    stopPolling = () => {
        if (this.state.pollInterval) {
            clearInterval(this.state.pollInterval);
        }
    };

    loadData = (silent = false) => {
        if (this.pendingRequest) {
            return; // Skip if a request is already in flight
        }
        if (!silent) {
            this.setState({ loading: true, error: null });
        }
        this.pendingRequest = true;
        axios.get('/api/workbench/data', {
            params: { date: this.state.date, shift: this.state.shift }
        })
            .then(res => {
                this.pendingRequest = false;
                const data = res.data;

                // Deduplication: skip setState if cache timestamp AND engine iteration are unchanged
                const prevData = this.state.data;
                if (prevData && data && data._cachedAt === prevData._cachedAt) {
                    const prevEng = prevData.engineState || {};
                    const newEng = data.engineState || {};
                    if (prevEng.iteration === newEng.iteration && prevEng.state === newEng.state) {
                        this.startPolling(newEng);
                        return;
                    }
                }

                // Merge engineState with previous values so metrics survive IDLE/STOPPED
                const newEngineState = data?.engineState || {};
                const prevEngineState = this.state.data?.engineState || {};
                const mergedEngineState = { ...prevEngineState };
                for (const [key, value] of Object.entries(newEngineState)) {
                    if (value != null && value !== 0) {
                        mergedEngineState[key] = value;
                    }
                }
                // Preserve best metrics if new state doesn't have them
                ['bestSpread', 'bestStdDev', 'bestMedian', 'lastImprovement'].forEach(k => {
                    if ((mergedEngineState[k] == null || mergedEngineState[k] === 0) && prevEngineState[k] != null) {
                        mergedEngineState[k] = prevEngineState[k];
                    }
                });
                data.engineState = mergedEngineState;
                this.setState({ data, loading: false });
                this.startPolling(data?.engineState);
            })
            .catch(err => {
                this.pendingRequest = false;
                this.setState({
                    loading: false,
                    error: (err.response?.data?.message) || err.message || 'Erreur de chargement'
                });
                this.startPolling(null);
            });
    };

    setDate = (date) => this.setState({ date });
    setShift = (shift) => this.setState({ shift: Number(shift) });
    refresh = () => this.loadData(false);

    reloadAll = () => {
        if (this.pendingRequest) {
            return;
        }
        this.setState({ loading: true, error: null });
        this.pendingRequest = true;
        axios.post('/api/workbench/reload', null, {
            params: { date: this.state.date, shift: this.state.shift }
        })
            .then(res => {
                this.pendingRequest = false;
                const data = res.data;
                const newEngineState = data?.engineState || {};
                const prevEngineState = this.state.data?.engineState || {};
                const mergedEngineState = { ...prevEngineState };
                for (const [key, value] of Object.entries(newEngineState)) {
                    if (value != null && value !== 0) {
                        mergedEngineState[key] = value;
                    }
                }
                ['bestSpread', 'bestStdDev', 'bestMedian', 'lastImprovement'].forEach(k => {
                    if ((mergedEngineState[k] == null || mergedEngineState[k] === 0) && prevEngineState[k] != null) {
                        mergedEngineState[k] = prevEngineState[k];
                    }
                });
                data.engineState = mergedEngineState;
                this.setState({ data, loading: false });
                this.startPolling(data?.engineState);
            })
            .catch(err => {
                this.pendingRequest = false;
                this.setState({
                    loading: false,
                    error: (err.response?.data?.message) || err.message || 'Erreur de rechargement'
                });
                this.startPolling(null);
            });
    };

    render() {
        const value = {
            data: this.state.data,
            loading: this.state.loading,
            error: this.state.error,
            date: this.state.date,
            shift: this.state.shift,
            setDate: this.setDate,
            setShift: this.setShift,
            refresh: this.refresh,
            reloadAll: this.reloadAll,
        };
        return (
            <WorkbenchContext.Provider value={value}>
                {this.props.children}
            </WorkbenchContext.Provider>
        );
    }
}
