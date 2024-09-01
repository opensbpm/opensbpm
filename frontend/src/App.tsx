import React, {useEffect, useRef, useState} from 'react';
import logo from './logo.svg';
import './App.css';
import keycloak from './keycloak';

function App() {
    const wasCalled = useRef(false);

    useEffect(() => {
        if(wasCalled.current) return;
        wasCalled.current = true;

        keycloak.init({
            onLoad: 'login-required',
            enableLogging: true
        })
            .then(authenticated => {
                console.log(`User is ${authenticated ? 'authenticated' : 'not authenticated'}`);
            })
            .catch(error =>
                console.error('Failed to initialize adapter:', error)
            );
    }, []);

    function login() {
        keycloak.login();
    }

    function logout() {
        keycloak.logout();
    }

    function showUserInfo() {
        keycloak.loadUserProfile()
            .then(result => console.log(result));
    }

    function fetchResource() {
        fetch("http://localhost:8090/",{
          headers:{
              authorization: `Bearer ${keycloak.token}`
          },
        })
            .then(res => {
              console.log(res);
            });
    }

    return (
        <div className="App">
            <header className="App-header">
                <img src={logo} className="App-logo" alt="logo"/>
                <p>
                    Edit <code>src/App.tsx</code> and save to reload.
                </p>
                <div>{`User is ${!keycloak.authenticated ? 'not ' : ''}authenticated`}</div>
                <button onClick={login}>Login</button>
                <button onClick={logout}>Logout</button>
                <button onClick={showUserInfo}>UserInfo</button>
                <button onClick={fetchResource}>Resource</button>
                <a
                    className="App-link"
                    href="https://reactjs.org"
                    target="_blank"
                    rel="noopener noreferrer"
                >
                    Learn React
                </a>
            </header>
        </div>
    );
}

export default App;
