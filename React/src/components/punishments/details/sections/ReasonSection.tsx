import React from 'react';
import { FaInfoCircle } from 'react-icons/fa';

type Theme = 'dark' | 'light';

interface ReasonSectionProps {
    reason: string;
    serverColor: string;
    currentTheme: Theme;
    cardBg: string;
}

const ReasonSection: React.FC<ReasonSectionProps> = ({ reason, serverColor, currentTheme, cardBg }) => {
    return (
        <div className={`mb-8 p-6 rounded-xl ${cardBg} border ${currentTheme === 'dark' ? 'border-gray-700' : 'border-gray-300'} shadow-md`}>
            <h2 className={`text-lg font-bold mb-3 border-b pb-2 flex items-center ${currentTheme === 'dark' ? 'border-gray-700' : 'border-gray-300'}`} style={{ color: serverColor }}>
                <FaInfoCircle className='mr-2' /> Reason for Punishment
            </h2>
            <p className="whitespace-pre-wrap text-base opacity-90">{reason}</p>
        </div>
    );
};

export default ReasonSection;