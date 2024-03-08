import React, {useEffect, useState} from 'react';
import logo from './logo.svg';
import './App.css';

function App() {
  const [greeting, setGreeting] = useState();
  useEffect(() => {
    const API_BASEURL = process.env.REACT_APP_API_URL ? process.env.REACT_APP_API_URL : '';
    fetch(`${API_BASEURL}/api/greeting`, { method: 'GET' })
        .then(response => response.json())
        .then(data => setGreeting(data.content))
        .catch((error) => console.log('ERROR in getGameCards: ' + error));
  }, []);

    return (
        <div className="App">
          <h1 data-testid="greeting">{greeting}</h1>
          <header className="App-header">
            <img src={logo} className="App-logo" alt="logo"/>
            <p>
              Edit <code>src/App.tsx</code> and save to reload.
            </p>
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
