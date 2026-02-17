import React from 'react';

interface EmptyStateProps {
    type: 'no-punishments' | 'config-error' | 'loading' | 'error' | 'checking-access';
    currentTheme: 'dark' | 'light';
    serverColor?: string;
    errorMessage?: string;
    punishmentTypeDisplay?: string;
}

const EmptyState: React.FC<EmptyStateProps> = ({
                                                   type,
                                                   currentTheme,
                                                   serverColor,
                                                   errorMessage,
                                                   punishmentTypeDisplay
                                               }) => {
    const baseClasses = `p-10 rounded-xl text-center border border-dashed ${
        currentTheme === 'dark' ? 'border-gray-700 bg-[#242424]' : 'border-gray-300 bg-white'
    }`;

    const errorClasses = `p-10 rounded-xl text-center border border-dashed ${
        currentTheme === 'dark' ? 'border-red-700 bg-[#242424]' : 'border-red-300 bg-white'
    }`;

    const renderSpinner = () => (
        <div className="mt-4 flex justify-center">
            <div
                className="animate-spin rounded-full h-8 w-8 border-b-2"
                style={{ borderColor: serverColor }}
            />
        </div>
    );

    switch (type) {
        case 'checking-access':
            return (
                <div className="absolute inset-0 flex items-center justify-center backdrop-blur-sm z-50">
                    <div className={baseClasses}>
                        <h3 className="text-xl font-semibold mb-2">Checking Access...</h3>
                        <p className="text-gray-500">Determining server security requirements.</p>
                        {renderSpinner()}
                    </div>
                </div>
            );

        case 'loading':
            return (
                <div className={baseClasses}>
                    <h3 className="text-xl font-semibold mb-2">Loading Punishments...</h3>
                    <p className="text-gray-500">Please wait while we fetch the latest data.</p>
                    {renderSpinner()}
                </div>
            );

        case 'error':
            return (
                <div className={errorClasses}>
                    <h3 className="text-xl font-semibold mb-2 text-red-500">Error Fetching Data</h3>
                    <p className="text-gray-500">Could not load punishments from the API: {errorMessage}</p>
                </div>
            );

        case 'config-error':
            return (
                <div className={baseClasses}>
                    <h3 className="text-xl font-semibold mb-2">Seems like a config issue :/</h3>
                    <p className="text-gray-500">No punishment types are enabled in the server configuration.</p>
                </div>
            );

        case 'no-punishments':
            return (
                <div className={baseClasses}>
                    <h3 className="text-xl font-semibold mb-2">No {punishmentTypeDisplay} Found</h3>
                    <p className="text-gray-500">Try adjusting your search or filter settings.</p>
                </div>
            );

        default:
            return null;
    }
};

export default EmptyState;