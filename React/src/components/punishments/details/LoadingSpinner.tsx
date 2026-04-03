import React from 'react';

const LoadingSpinner: React.FC = () => {
    return (
        <div className={`flex items-center justify-center min-h-screen bg-background text-text-primary`}>
            <div className={`flex items-center space-x-3 p-8 rounded-xl bg-surface border border-surface-border`}>
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-server-color"></div>
                <p className="text-xl font-semibold">Loading Punishment Details...</p>
            </div>
        </div>
    );
};

export default LoadingSpinner;