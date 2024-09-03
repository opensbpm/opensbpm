import React from "react";
import { useAuth } from "react-oidc-context";

function App() {
    const auth = useAuth();

    function fetchResource() {
        fetch("http://localhost:8090/",{
            headers:{
                authorization: `Bearer ${auth.user?.access_token}`
            },
        })
            .then(res => {
                console.log(res);
            });
    }


    switch (auth.activeNavigator) {
        case "signinSilent":
            return <div>Signing you in...</div>;
        case "signoutRedirect":
            return <div>Signing you out...</div>;
    }

    if (auth.isLoading) {
        return <div>Loading...</div>;
    }

    if (auth.error) {
        return <div>Oops... {auth.error.message}</div>;
    }

    if (auth.isAuthenticated) {
        return (
            <div>
                Hello {auth.user?.profile.sub}{" "}
                <button onClick={() => void auth.removeUser()}>Log out</button>
                <button onClick={fetchResource}>Resource</button>
            </div>

        );
    }

    return <button onClick={() => void auth.signinRedirect()}>Log in</button>;
}

export default App;
