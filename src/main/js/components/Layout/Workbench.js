import React, { Component } from 'react';
import { connect } from 'react-redux';
import { withRouter } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faBullseye, faLayerGroup, faScaleBalanced, faTriangleExclamation,
} from '@fortawesome/free-solid-svg-icons';
import { WorkbenchProvider, WorkbenchContext } from './WorkbenchContext';
import WorkbenchHeader from './WorkbenchHeader';
import MachineRecommendationsView from './MachineRecommendationsView';
import SequenceFocusView from './SequenceFocusView';
import WorkbenchZoneChargeView from './WorkbenchZoneChargeView';
import WorkbenchAlertsView from './WorkbenchAlertsView';
import '../../styles/Workbench.scss';

const TABS = [
    { id: 'nextSeries', label: 'Top 3 par machine', icon: faLayerGroup },
    { id: 'zoneCharge', label: 'Charge par zone', icon: faScaleBalanced },
    { id: 'alerts', label: 'Alertes & à clôturer', icon: faTriangleExclamation },
    { id: 'sequenceFocus', label: 'Séquences & matière', icon: faBullseye },
];

class Workbench extends Component {
    constructor(props) {
        super(props);
        this.state = {
            activeTab: 'nextSeries',
            zoneFilter: 'All',
        };
    }

    componentDidMount() {
        // Read ?section= from URL and map to tab
        const params = new URLSearchParams(this.props.location.search);
        const section = params.get('section');
        const tabMap = {
            focus: 'sequenceFocus',
            sequenceFocus: 'sequenceFocus',
            nextSeries: 'nextSeries',
            tableFeed: 'nextSeries',
            zoneCharge: 'zoneCharge',
            charge: 'zoneCharge',
            alerts: 'alerts',
        };
        if (section && tabMap[section]) {
            this.setState({ activeTab: tabMap[section] });
        }
    }

    renderTabContent(data) {
        const { activeTab } = this.state;
        switch (activeTab) {
            case 'nextSeries':
                return (
                    <MachineRecommendationsView
                        data={data}
                        zoneFilter={this.state.zoneFilter}
                    />
                );
            case 'zoneCharge':
                return (
                    <WorkbenchZoneChargeView
                        data={data}
                        zoneFilter={this.state.zoneFilter}
                    />
                );
            case 'alerts':
                return (
                    <WorkbenchAlertsView
                        data={data}
                        zoneFilter={this.state.zoneFilter}
                    />
                );
            case 'sequenceFocus':
                return (
                    <SequenceFocusView
                        data={data}
                        zoneFilter={this.state.zoneFilter}
                    />
                );
            default:
                return null;
        }
    }

    render() {
        const { activeTab } = this.state;

        return (
            <WorkbenchProvider>
                <WorkbenchContext.Consumer>
                    {({ data }) => (
                        <div className="wb-container">
                            <WorkbenchHeader
                                zoneFilter={this.state.zoneFilter}
                                onZoneFilterChange={(zoneFilter) => this.setState({ zoneFilter })}
                            />

                            {/* ── Horizontal Tab Menu ── */}
                            <div className="wb-tab-bar">
                                {TABS.map(tab => (
                                    <button
                                        key={tab.id}
                                        className={`wb-tab ${activeTab === tab.id ? 'wb-tab--active' : ''}`}
                                        onClick={() => this.setState({ activeTab: tab.id })}
                                    >
                                        <FontAwesomeIcon icon={tab.icon} className="wb-tab-icon" />
                                        <span>{tab.label}</span>
                                    </button>
                                ))}
                            </div>

                            {/* ── Tab Content ── */}
                            <div className="wb-tab-content">
                                {this.renderTabContent(data)}
                            </div>
                        </div>
                    )}
                </WorkbenchContext.Consumer>
            </WorkbenchProvider>
        );
    }
}

const mapStateToProps = state => ({
    security: state.security,
});

export default withRouter(connect(mapStateToProps)(Workbench));
