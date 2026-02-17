import React from 'react';

interface LoadingStateProps {
    serverColor: string;
    bgColor: string;
    textColor: string;
    cardBg: string;
    borderColor: string;
}

const LoadingState: React.FC<LoadingStateProps> = ({
                                                       serverColor,
                                                       bgColor,
                                                       textColor,
                                                       cardBg,
                                                       borderColor
                                                   }) => {
    return (
        <div className={`min-h-screen ${bgColor} ${textColor} flex items-center justify-center`}>
            <div className={`p-10 rounded-xl text-center shadow-xl ${cardBg} border ${borderColor}`}>
                <div className="flex flex-col items-center gap-4">
                    <div
                        className="animate-spin rounded-full h-10 w-10 border-b-2"
                        style={{ borderColor: serverColor }}
                    />
                    <p className="text-xl font-semibold">Loading Profile...</p>
                </div>
            </div>
        </div>
    );
};

export default LoadingState;