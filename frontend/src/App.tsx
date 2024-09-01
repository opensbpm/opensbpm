import React from 'react';
import Keycloak from 'keycloak-js';
import logo from './logo.svg';
import './App.css';

function App() {

  const keycloak = new Keycloak({
    url: 'http://localhost:9000',
    realm: 'quickstart',
    clientId: 'authz-servlet'
  });

  function login() {
    try {
      keycloak.init({onLoad: 'login-required'})
          .then((authenticated) => {
        if (authenticated) {
          console.log('User is authenticated');
        } else {
          console.log('User is not authenticated');
        }
      });
    } catch (error) {
      console.error('Failed to initialize adapter:', error);
    }

    //await keycloak.login();
  }

  function showUserInfo() {
    // keycloak.loadUserInfo()
    //     .then(ui =>{
    //       console.log(ui);
    //     })

    keycloak.loadUserProfile()
        .then(ui =>{
          console.log(ui);
        })
  }

  function fetchResource() {

    fetch("http://localhost:8090/",{
      headers:{
        'Authorization': "Bearer "+keycloak.token
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
        <button onClick={login}>Login</button>
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
