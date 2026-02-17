import React from 'react';

interface StatusMessageProps {
    isRemoved: boolean;
}

const StatusMessage: React.FC<StatusMessageProps> = ({ isRemoved }) => {
    return (
        <div className={`p-4 rounded-xl ${isRemoved ? 'bg-red-500/20 text-red-400 border-red-500/40' : 'bg-yellow-500/20 text-yellow-400 border-yellow-500/40'} font-bold text-center border shadow-md`}>
            {isRemoved ? 'This punishment has already been removed.' : 'This punishment has expired.'}
        </div>
    );
};

export default StatusMessage;