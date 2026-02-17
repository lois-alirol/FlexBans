import React from 'react';

type Theme = 'dark' | 'light';

interface LoadingSpinnerProps {
    currentTheme: Theme;
    serverColor: string;
}

const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ currentTheme, serverColor }) => {
    return (
        <div className={`flex items-center justify-center min-h-screen ${currentTheme === 'dark' ? 'bg-[#1c1c1c] text-gray-100' : 'bg-gray-50 text-gray-800'}`}>
            <div className={`flex items-center space-x-3 p-8 rounded-xl ${currentTheme === 'dark' ? 'bg-[#2c2c2c]' : 'bg-white'} shadow-xl`}>
                <div className="animate-spin rounded-full h-8 w-8 border-b-2" style={{ borderColor: serverColor }}></div>
                <p className="text-xl font-semibold">Loading Punishment Details...</p>
            </div>
        </div>
    );
};

export default LoadingSpinner;