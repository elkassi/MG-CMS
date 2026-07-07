import {createStore, applyMiddleware, compose} from 'redux';
import thunk from 'redux-thunk';
import rootReducer from './reducers';

const initialState = {};
const middleware = [thunk];

let store;

const ReactReduxDexTools = window.__REDUX_DEVTOOLS_EXTENSION__ && 
window.__REDUX_DEVTOOLS_EXTENSION__()

if(window.navigator.userAgent.includes("Chrome") && ReactReduxDexTools) {
    store = createStore(
        rootReducer, 
        initialState, 
        compose(
            applyMiddleware(...middleware),
            ReactReduxDexTools
        )
    )
}else {
    store = createStore(
        rootReducer, 
        initialState, 
        compose(
            applyMiddleware(...middleware)
        )
    )
}

export default store;