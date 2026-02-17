import React from 'react';
import { FaCheckCircle, FaTimesCircle, FaHistory } from 'react-icons/fa';

interface StatusBadgeProps {
    status: 'Active' | 'Expired' | 'Removed' | string | null; // Support null if status can be null
    className?: string; // Optional className for additional external styling
}

const StatusBadge: React.FC<StatusBadgeProps> = ({ status, className = '' }) => {
    const statusStyles = () => {
        switch (status) {
            case 'Active':
                return {
                    color: 'text-green-500',
                    bg: 'bg-green-500/10',
                    icon: <FaCheckCircle className="mr-2" />,
                    label: 'ACTIVE',
                };
            case 'Expired':
                return {
                    color: 'text-yellow-500',
                    bg: 'bg-yellow-500/10',
                    icon: <FaHistory className="mr-2" />,
                    label: 'EXPIRED',
                };
            case 'Removed':
                return {
                    color: 'text-red-500',
                    bg: 'bg-red-500/10',
                    icon: <FaTimesCircle className="mr-2" />,
                    label: 'REMOVED',
                };
            default:
                return {
                    color: 'text-gray-500',
                    bg: 'bg-gray-300',
                    icon: null,
                    label: status || 'UNKNOWN',
                };
        }
    };

    const { color, bg, icon, label } = statusStyles();

    return (
        <span
            className={`inline-flex items-center py-1 rounded-full tracking-wide ${color} ${bg} ${className}`}
        >
      {icon}
            {label}
    </span>
    );
};

export default StatusBadge;