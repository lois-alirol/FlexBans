import React, { type ReactNode } from 'react';
import { useServerConfig } from '../../hooks/useServerConfig';
import { Link } from 'react-router-dom';

interface ErrorPageLayoutProps {
    statusCode: number;
    title: string;
    description: string;
    actionText: string;
    iconClass: string;
    details?: ReactNode;
}

const ErrorPageLayout: React.FC<ErrorPageLayoutProps> = ({
    statusCode,
    title,
    description,
    actionText,
    iconClass,
    details
}) => {
    const { serverConfig } = useServerConfig();

    const serverName = serverConfig.serverName;
    const serverColor = serverConfig.serverColor;
    const serverColorHover = serverConfig.serverColorHover;

    const buttonStyle = {
        '--server-color': serverColor,
        '--server-color-hover': serverColorHover,
        backgroundColor: 'var(--server-color)',
    } as React.CSSProperties;

    return (
        <div className="bg-[#2e2e2e] text-[#e4e4e7] flex items-center justify-center min-h-screen font-sans relative overflow-hidden">
            <title>{statusCode} - {serverName}</title>

            <div className="bg-[#1f1f1f] p-8 md:p-12 rounded-2xl shadow-2xl w-full max-w-xl text-center z-10 border border-[#3b3b3b]">
                <p 
                    className="text-8xl font-extrabold mt-2 mb-4"
                    style={{color: serverColor}}
                >
                    {statusCode}
                </p>
                <h1 className="text-3xl md:text-4xl mb-6 font-bold tracking-tight text-white">
                    {title}
                </h1>
                
                <p className={`mb-6 text-base md:text-lg text-gray-400 ${details ? 'mb-4' : ''}`}>
                    {description}
                </p>
                
                {details && (
                    <div className="mt-4 mb-6 p-4 bg-[#2e2e2e] rounded-lg text-left text-sm border border-[#3b3b3b]">
                        {details}
                    </div>
                )}

                <Link
                    to="/"
                    className={`
                        text-white font-bold py-3 px-8 rounded-xl inline-flex items-center justify-center
                        transition duration-300 transform hover:scale-[1.02] active:scale-[0.98]
                        shadow-lg
                        ${details ? 'mt-4' : 'mt-0'}
                    `}
                    style={buttonStyle}
                    >
                    <i className={`${iconClass} mr-2`}></i>
                    {actionText}
                </Link>
            </div>
        </div>
    );
};

export default ErrorPageLayout;