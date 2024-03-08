import React from 'react';
import {render, screen} from '@testing-library/react';
import App from './App';


describe('App.test', () => {
    test('renders learn react link', () => {
        render(<App/>);
        const linkElement = screen.getByText(/learn react/i);
        expect(linkElement).toBeInTheDocument();
    });

    const greeting = {"id":1,"content":"Hello, World!"};
    beforeEach(() => {
        jest.spyOn(global, 'fetch').mockResolvedValue({
            json: jest.fn().mockResolvedValue(greeting)
        })
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });
    test('renders greeting header',async () => {

        render(<App/>);

        const greetingNode = await screen.findByText("Hello, World!");

        expect(greetingNode).toBeInTheDocument();

    });
});
