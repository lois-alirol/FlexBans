import React from 'react';
import MyAccount from '../common/MyAccount';

interface PageHeaderProps {
    serverName: string;
    typeDisplay: string;
    isAuthenticated: boolean;
    isSecured: boolean;
    currentTheme: 'dark' | 'light';
}

const PageHeader: React.FC<PageHeaderProps> = ({
                                                   serverName,
                                                   typeDisplay,
                                                   isAuthenticated,
                                                   isSecured,
                                                   currentTheme
                                               }) => {
    return (
        <>
            <div className="flex justify-between items-start">
                <div>
                    <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
                        {serverName}'s Punishments
                    </h1>
                </div>

                {isSecured && isAuthenticated && (
                    <div className="hidden lg:block mt-1">
                        <MyAccount currentTheme={currentTheme} />
                    </div>
                )}
            </div>

            <p className="text-sm sm:text-base mb-6 text-gray-500 dark:text-gray-400">
                Viewing <span className={`font-semibold ${currentTheme === 'dark' ? 'text-white' : 'text-black'}`}>{typeDisplay}</span>. Select a different type from the menu.
            </p>
        </>
    );
};

export default PageHeader;