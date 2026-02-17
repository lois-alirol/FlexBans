import React from 'react';
import { FaInfoCircle } from 'react-icons/fa';

type Theme = 'dark' | 'light';

interface DetailCardProps {
    title: string;
    children: React.ReactNode;
    accentColor: string;
    currentTheme: Theme;
}

const DetailCard: React.FC<DetailCardProps> = ({ title, children, accentColor, currentTheme }) => (
    <div className={`p-5 rounded-xl border ${currentTheme === 'dark' ? 'border-gray-700' : 'border-gray-300'} shadow-md`}>
        <h2 className={`text-lg font-bold mb-3 border-b pb-2 flex items-center ${currentTheme === 'dark' ? 'border-gray-700' : 'border-gray-300'}`} style={{ color: accentColor }}>
            <FaInfoCircle className='mr-2 text-base' /> {title}
        </h2>
        {children}
    </div>
);

export default DetailCard;