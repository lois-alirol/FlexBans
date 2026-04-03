import React from 'react';
import { FaInfoCircle } from 'react-icons/fa';

interface DetailCardProps {
    title: string;
    children: React.ReactNode;
}

const DetailCard: React.FC<DetailCardProps> = ({ title, children }) => {
    return (
        <div className={`bg-surface-elevated p-5 rounded-2xl border border-surface-border`}>
            <h2 className={`text-lg font-bold mb-3 border-b pb-2 flex items-center border-surface-border text-server-color`}>
                <FaInfoCircle className='mr-2 text-base' /> {title}
            </h2>
            {children}
        </div>
    )
}

export default DetailCard;