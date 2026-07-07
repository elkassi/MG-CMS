import React from "react";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import PropTypes from 'prop-types';
import { faSort, faSortUp, faSortDown } from '@fortawesome/free-solid-svg-icons'

class SortIcon extends React.PureComponent {

    constructor(props) {
        super(props);
    }

    render() {
        return (
            <span className={this.props.className}>
                <FontAwesomeIcon icon={
                    (this.props.sortProp !== this.props.currentSort && faSort)
                    || (this.props.sortProp === this.props.currentSort && this.props.sortDirection === 'asc' && faSortUp)
                    || (this.props.sortProp === this.props.currentSort && this.props.sortDirection === 'desc' && faSortDown)
                }/>
            </span>
        );
    }

}

export default SortIcon;

SortIcon.prototypes = {
    className: PropTypes.string,
    currentSort: PropTypes.string,
    sortProp: PropTypes.string,
    sortDirection: PropTypes.oneOf(['asc', 'desc'])
};