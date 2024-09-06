import React from 'react';
import {render, screen} from '@testing-library/react';
import App from './App';
import {AuthProvider} from "react-oidc-context";

describe('App.test', () => {
    test('renders learn react link', () => {
        render(
            <AuthProvider>
            <App/>
            </AuthProvider>
        );
        const linkElement = screen.getByText(/loading/i);
        expect(linkElement).toBeInTheDocument();
    });
});
