import { combineReducers } from "redux";
import securityReducer from "./securityReducer";
import errorReducer from './errorReducer';
import userReducer from './userReducer';

export default combineReducers({
  security: securityReducer,
  errors: errorReducer,
  userInfo: userReducer
})