/**
 * INSPIRED FROM: https://github.com/AdeleD/react-paginate/blob/master/react_components/PaginationBoxView.js
 */

 'use strict';

 import React, {Component} from 'react';
 import PropTypes from 'prop-types';
 import {Pagination} from "react-bootstrap";
 
 export default class AdvancedPagination extends Component {
 
     static propTypes = {
         pageCount: PropTypes.number.isRequired,
         onPageChange: PropTypes.func,
     };
 
     constructor(props) {
         super(props);
 
         this.pageRangeDisplayed = 2;
         this.marginPagesDisplayed = 3;
 
         this.state = {
             selected: 0,
         };
     }
 
     setPage = (selected) => {
         this.setState({selected})
     };
 
     handleFirstPage = evt => {
         const {selected} = this.state;
         evt.preventDefault ? evt.preventDefault() : (evt.returnValue = false);
         if (selected > 0) {
             this.handlePageSelected(0, evt);
         }
     };
 
     handleLastPage = evt => {
         const {selected} = this.state;
         const {pageCount} = this.props;
 
         evt.preventDefault ? evt.preventDefault() : (evt.returnValue = false);
         if (selected < pageCount - 1) {
             this.handlePageSelected(pageCount - 1, evt);
         }
     };
 
     handlePreviousPage = evt => {
         const {selected} = this.state;
         evt.preventDefault ? evt.preventDefault() : (evt.returnValue = false);
         if (selected > 0) {
             this.handlePageSelected(selected - 1, evt);
         }
     };
 
     handleNextPage = evt => {
         const {selected} = this.state;
         const {pageCount} = this.props;
 
         evt.preventDefault ? evt.preventDefault() : (evt.returnValue = false);
         if (selected < pageCount - 1) {
             this.handlePageSelected(selected + 1, evt);
         }
     };
 
     handlePageSelected = (selected, evt) => {
         evt.preventDefault ? evt.preventDefault() : (evt.returnValue = false);
 
         if (this.state.selected === selected) return;
 
         this.setState({selected: selected});
 
         // Call the callback with the new selected item:
         this.callCallback(selected);
     };
 
     getForwardJump() {
         const {selected} = this.state;
         const {pageCount} = this.props;
 
         const forwardJump = selected + this.pageRangeDisplayed;
         return forwardJump >= pageCount ? pageCount - 1 : forwardJump;
     }
 
     getBackwardJump() {
         const {selected} = this.state;
 
         const backwardJump = selected - this.pageRangeDisplayed;
         return backwardJump < 0 ? 0 : backwardJump;
     }
 
     handleBreakClick = (index, evt) => {
         evt.preventDefault ? evt.preventDefault() : (evt.returnValue = false);
 
         const {selected} = this.state;
 
         this.handlePageSelected(
             selected < index ? this.getForwardJump() : this.getBackwardJump(),
             evt
         );
     };
 
     callCallback = selectedItem => {
         if (
             typeof this.props.onPageChange !== 'undefined' &&
             typeof this.props.onPageChange === 'function'
         ) {
             this.props.onPageChange(selectedItem);
         }
     };
 
     getPageElement(index) {
         const {selected} = this.state;
         return (
             <Pagination.Item
                 key={index}
                 onClick={this.handlePageSelected.bind(null, index)}
                 active={selected === index}>
                 {index + 1}
             </Pagination.Item>
         );
     }
 
     pagination = () => {
         const items = [];
         const {pageCount} = this.props;
 
         const {selected} = this.state;
 
         if (pageCount <= this.pageRangeDisplayed) {
             for (let index = 0; index < pageCount; index++) {
                 items.push(this.getPageElement(index));
             }
         } else {
             let leftSide = this.pageRangeDisplayed / 2;
             let rightSide = this.pageRangeDisplayed - leftSide;
 
             // If the selected page index is on the default right side of the pagination,
             // we consider that the new right side is made up of it (= only one break element).
             // If the selected page index is on the default left side of the pagination,
             // we consider that the new left side is made up of it (= only one break element).
             if (selected > pageCount - this.pageRangeDisplayed / 2) {
                 rightSide = pageCount - selected;
                 leftSide = this.pageRangeDisplayed - rightSide;
             } else if (selected < this.pageRangeDisplayed / 2) {
                 leftSide = selected;
                 rightSide = this.pageRangeDisplayed - leftSide;
             }
 
             let index;
             let page;
             let breakView = null;
             let createPageView = index => this.getPageElement(index);
 
             for (index = 0; index < pageCount; index++) {
                 page = index + 1;
 
                 // If the page index is lower than the margin defined,
                 // the page has to be displayed on the left side of
                 // the pagination.
                 if (page <= this.marginPagesDisplayed) {
                     items.push(createPageView(index));
                     continue;
                 }
 
                 // If the page index is greater than the page count
                 // minus the margin defined, the page has to be
                 // displayed on the right side of the pagination.
                 if (page > pageCount - this.marginPagesDisplayed) {
                     items.push(createPageView(index));
                     continue;
                 }
 
                 // If the page index is near the selected page index
                 // and inside the defined range (this.pageRangeDisplayed)
                 // we have to display it (it will create the center
                 // part of the pagination).
                 if (index >= selected - leftSide && index <= selected + rightSide) {
                     items.push(createPageView(index));
                     continue;
                 }
 
                 // If the page index doesn't meet any of the conditions above,
                 // we check if the last item of the current "items" array
                 // is a break element. If not, we add a break element, else,
                 // we do nothing (because we don't want to display the page).
                 if (items[items.length - 1] !== breakView) {
                     breakView = (
                         <Pagination.Ellipsis onClick={this.handleBreakClick.bind(null, index)}/>
                     );
                     items.push(breakView);
                 }
             }
         }
 
         return items;
     };
 
     render() {
         return (
             <Pagination size="sm" color='#123123'>
 
                 <Pagination.First onClick={this.handleFirstPage}/>
                 <Pagination.Prev onClick={this.handlePreviousPage}/>
                 {this.pagination()}
                 <Pagination.Next onClick={this.handleNextPage}/>
                 <Pagination.Last onClick={this.handleLastPage}/>
 
             </Pagination>
         );
     }
 }
 