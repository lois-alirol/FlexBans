import React, { type ReactNode } from 'react';

import { useTitle } from '@hooks/useTitle';

interface AuthLayoutProps {
    title: string;
    children: ReactNode;
    pageTitle: string;
}

export const AuthLayout: React.FC<AuthLayoutProps> = ({ title, children, pageTitle }) => {
    useTitle(pageTitle);

    const containerClasses = 'bg-background text-text-primary'
    const cardClasses = 'bg-surface text-text-primary border border-surface-border'

    return (
        <div className={`grid place-items-center min-h-screen p-4 transition-colors duration-500 ${containerClasses}`}>
            <div
                className={`relative w-full max-w-md rounded-2xl overflow-hidden flex flex-col px-8 py-10 ${cardClasses}`}
            >
                <div className="mb-8">
                    <h1 className="text-3xl font-bold tracking-tight text-center">
                        {title}
                    </h1>
                    <div
                        className="h-1 w-12 mx-auto mt-2 rounded-full bg-server-color"
                    />
                </div>

                {children}
            </div>
        </div>
    );
};