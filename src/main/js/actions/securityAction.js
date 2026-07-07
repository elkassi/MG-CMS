
import axios from "axios";
import jwt_decode from "jwt-decode";
import { GET_ERRORS, SET_CURRENT_USER } from "./types";
import setJWTToken from "../securityUtils/setJWTToken";

export const login = (loginRequest) => async dispatch => {
    try{
        const res = await axios.post("/api/user/login", loginRequest);
        const {token} = res.data;
        if (loginRequest.remembreMe) {
            localStorage.setItem("username", loginRequest.username)
            localStorage.setItem("password", loginRequest.password)
            localStorage.setItem("remembreMe", loginRequest.remembreMe)
        } else {
            if (localStorage.username) localStorage.removeItem("username")
            if(localStorage.password) localStorage.removeItem("password")
            localStorage.setItem("remembreMe", false)

        }
        
        localStorage.setItem("jwtToken", token);
        setJWTToken(token);
        const decoded = jwt_decode(token);
        // window.location.pathname= "/"

        
        
        
        dispatch({
            type: SET_CURRENT_USER,
            payload: decoded
        })
    }catch(err){
        // err.response is undefined when the server is unreachable; without this
        // fallback the login button stays stuck on "Connexion en cours..."
        dispatch({
            type: GET_ERRORS,
            payload: (err.response && err.response.data)
                ? err.response.data
                : { username: "Serveur injoignable — vérifiez la connexion et réessayez", password: "" }
        });
    }
}

export const logout = () => dispatch => {
    localStorage.removeItem("jwtToken");
    setJWTToken(false);
    dispatch({
        type: SET_CURRENT_USER,
        payload: {}
    })
}